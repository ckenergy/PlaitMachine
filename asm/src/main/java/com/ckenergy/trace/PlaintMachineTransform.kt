package com.ckenergy.trace

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.ckenergy.trace.extension.PlaitExtension
import com.ckenergy.trace.extension.PlaitMethodList
import com.ckenergy.trace.extension.TraceConfig
import com.ckenergy.trace.extension.TraceMethodListExtension
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.NamedDomainObjectContainer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Created by chengkai on 2021/3/5.
 */
class PlaintMachineTransform : Transform() {

    companion object {
        const val TAG = "===PlaintMachineTransform==="

        fun newTransform(): PlaintMachineTransform {
            return PlaintMachineTransform()
        }

        private fun transformMap(plaitExtension: PlaitExtension): TraceConfig {
            val traceMap = HashMap<String, ArrayList<PlaitMethodList>?>()
            val packages = HashMap<String, ArrayList<PlaitMethodList>?>()
            val blackPackages = HashMap<String, ArrayList<PlaitMethodList>?>()
            /*
            这里的多层嵌套循环，用map结构优化下，从
            a[1,2,3]
            b[1,3,4]
            c[2,3,4]
            转化成
            1 -> [a,b]
            2 -> [a,c]
            1,2,3为需要hook方法的名字，a，b，c为要织入的方法
             */
            plaitExtension.plaitClass.forEach { plaitClassExtension ->
                val result = plaitClassExtension.name.split(".")//切分类名和方法名字
                plaitClassExtension.classList?.forEach {
//                    println("===$TAG >>>>> className:$classNameNew Trace:${it.name},")
                    val map: HashMap<String, ArrayList<PlaitMethodList>?>
//                    val black = if (!it.name.endsWith("*")) {暂时不处理
                    val black = if (false) {
                        map = traceMap
                        plaitClassExtension.blackClassList?.find { it1 -> //com.hh.cc  ,black: com.hh.*
                            (it1.name == it.name || it.name.contains(it1.name.replace("*", "")))
                                    && it1.methodList.find { it2 -> it2 == "all*" } == null
                        }
                    } else {
                        map = packages
                        null
                    }
                    var list = map[it.name]
                    if (list == null) {
                        list = ArrayList()
                    }
                    map.put(it.name, list)
                    if (result.size == 2) {
                        val plaitMethodList = PlaitMethodList()
                        plaitMethodList.plaitClass = result[0]
                        plaitMethodList.plaitMethod = result[1]
                        plaitMethodList.methodList = it.methodList
                        plaitMethodList.isMethodExit = plaitClassExtension.isMethodExit
                        list.add(plaitMethodList)
                    }
                }
                plaitClassExtension.blackClassList?.forEach {
//                    println("===$TAG >>>>> className:$classNameNew Trace:${it.name},")
                    val list1 = blackPackages[it.name] ?: ArrayList()
                    blackPackages.put(it.name, list1)
                    if (result.size == 2) {
                        val plaitMethodList = PlaitMethodList()
                        plaitMethodList.plaitClass = result[0]
                        plaitMethodList.plaitMethod = result[1]
                        plaitMethodList.methodList = it.methodList
                        list1.add(plaitMethodList)
                    }
                }

                //要插入的方法也加入到黑名单，避免造成循环调用
                val list2 = blackPackages[result[0]] ?: ArrayList()
                blackPackages[result[0]] = list2
                list2.add(PlaitMethodList().apply {
                    plaitClass = result[0]
                    plaitMethod = result[1]
                    methodList = listOf(result[1])
                })

                //添加默认的黑名单
                Constants.DEFAULT_BLACK_PACKAGE.forEach {
                    val list3 = blackPackages[it] ?: ArrayList()
                    blackPackages[it] = list3
                    list3.add(PlaitMethodList().apply {
                        plaitClass = result[0]
                        plaitMethod = result[1]
                        methodList = listOf(Constants.ALL)
                    })
                }
            }
            return TraceConfig(traceMap, packages, blackPackages)
        }

        private fun checkInBlack(
            className: String,
            blackList: NamedDomainObjectContainer<TraceMethodListExtension>?
        ): Boolean {
            if (blackList.isNullOrEmpty()) return false
            return blackList.firstOrNull {
                className.contains(it.name)
            } != null
        }

        private fun log(info: String) {
//            Log.d(TAG, info)
        }

    }

    var plaintMachineExtension: PlaitExtension? = null

    var traceConfig: TraceConfig? = null

    //创建大小为16的线程池
    private val executor = Executors.newFixedThreadPool(16)

    override fun getName() = "PlaintMachineTransform"

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    //是否支持增量更新
    override fun isIncremental() = true

    override fun transform(transformInvocation: TransformInvocation?) {
        val extension = plaintMachineExtension
        val startTime = System.currentTimeMillis()

        val enable = extension?.enable == true
        if (enable) {
            traceConfig = transformMap(extension!!)
        }

        log( "enable: $enable , traceMap:$traceConfig")

        //是否增量编译
        val isIncremental = transformInvocation!!.isIncremental && this.isIncremental
        val outputProvider = transformInvocation.outputProvider
        val inputs = transformInvocation.inputs
        if(!isIncremental) {
            outputProvider?.deleteAll()
        }

        val tasks = ArrayList<Future<*>>()

        for (input in inputs) {
//            log( "dir: ${input.directoryInputs.size}, isIncremental:$isIncremental")
            for (directoryInput in input.directoryInputs) {
                val srcDir = directoryInput.file.absolutePath
//                log("directoryInput: ${srcDir}")
                val dest = outputProvider.getContentLocation(
                    directoryInput.name,
                    directoryInput.contentTypes,
                    directoryInput.scopes,
                    Format.DIRECTORY
                )
                if (!dest.exists()) {
                    dest.mkdirs()
                }
                if (isIncremental) {//增量更新，只 操作有改动的文件
                    //增量更新，只 操作有改动的文件
                    val fileStatusMap = directoryInput.changedFiles
                    fileStatusMap.forEach { (file, status) ->
//                        log("changedFiles:${file.absolutePath},status:$status")

                        if (status == Status.ADDED || status == Status.CHANGED) {
                            if (enable) {
                                executor.submit(plait(file, srcDir, dest)).apply { tasks.add(this) }
                            } else {
                                FileUtils.copyFileToDirectory(file, dest)
                            }
                        } else if (status == Status.REMOVED) {
                            if (dest?.exists() == true) {
                                dest.delete()
                            }
                            if (file.exists()) {
                                file.delete()
                            }
                        } else {
                            if (!enable) {
                                FileUtils.copyFileToDirectory(file, dest)
                            }
                        }
                    }
                } else {
                    if (enable) {
                        directoryInput.file.let {
                            if (it.isDirectory) {
                                FileUtils.getAllFiles(it).forEach { file ->
                                    executor.submit(plait(file, srcDir, dest)).apply { tasks.add(this) }
                                }
                            }else {
                                executor.submit(plait(it, srcDir, dest)).apply { tasks.add(this) }
                            }
                        }
                    } else {
                        FileUtils.copyDirectoryToDirectory(directoryInput.file, dest)
                    }
                }
            }
//            log("changedJars size:${input.jarInputs.size}")
            for (inputJar in input.jarInputs) {
                var destName = inputJar.name
                // rename jar files
                val hexName = DigestUtils.md5Hex(inputJar.file.absolutePath)
                if (destName.endsWith(".jar")) {
                    destName = destName.substring(0, destName.length - 4)
                }
                //将jar也加进来,androidx需要这个
                val dest = outputProvider.getContentLocation(
                    destName+"_"+hexName,
                    inputJar.contentTypes,
                    inputJar.scopes,
                    Format.JAR
                )
//                log( "jar hexName:$hexName, dest:$dest, destName:$destName")
                if (isIncremental) {//增量更新，只 操作有改动的文件
//                    log("isIncremental inputJar:${inputJar.name},status:${inputJar.status}")
                    val status = inputJar.status
                    if (status == Status.REMOVED) {
                        if (dest?.exists() == true) {
                            dest.delete()
                        }
                    } else if (status == Status.NOTCHANGED) {//没变化的话只是简单的复制
                        if (!enable) {
                            FileUtils.copyFile(inputJar.file, dest)
                        }
                    } else {
                        if (enable) {
                            executor.submit(PlaitJarTask(inputJar.file, dest, traceConfig))
                                .apply { tasks.add(this) }
                        } else {
                            FileUtils.copyFile(inputJar.file, dest)
                        }
                    }
                } else {
//                    log("inputJar:${inputJar.name}")
                    if (enable) {
                        executor.submit(PlaitJarTask(inputJar.file, dest, traceConfig))
                            .apply { tasks.add(this) }
                    } else {
                        FileUtils.copyFile(inputJar.file, dest)
                    }
                }
            }
        }
        //等待所有任务运行完毕
        tasks.forEach {
            it.get()
        }
        println("$TAG >>> timeCount：${System.currentTimeMillis() - startTime}")
    }

    private fun plait(file: File, input: String, dest: File): Runnable {
        val destName = file.absolutePath.replace(input, dest.absolutePath)
//        log(">>>>>>>>> PlaitAction filter classPath :${file.absolutePath}")
        return PlaitAction(file, destName, traceConfig)
    }

    private inner class PlaitAction(
        val file: File,
        val dest: String,
        val map: TraceConfig?
    ) :
        Runnable {

        override fun run() {
            val destFile = File(dest)
            if (destFile.exists()) {
                destFile.delete()
            }
            val destDir = destFile.parentFile
            org.apache.commons.io.FileUtils.forceMkdir(destDir)
            val classPath = dest

            val name = file.name
//            log( ">>>>>>>>> classPath :$classPath, fileName:$name")
            if (name.endsWith(".class", true)) {
                val fos = FileOutputStream(classPath)
                try {
                    val cr = ClassReader(file.readBytes())
                    val cw = ClassWriter(cr, ClassWriter.COMPUTE_FRAMES)
                    //需要处理的类使用自定义的visitor来处理
                    val visitor = PlaitClassVisitor(cw, map)
                    cr.accept(visitor, ClassReader.EXPAND_FRAMES)

                    val bytes = cw.toByteArray()
                    fos.write(bytes)
                } catch (e: Exception) {
                    e.printStackTrace()
                    log( ">>>>>>>>> PlaitAction:$name error :${e.printStackTrace()}")
                    FileUtils.copyFileToDirectory(file, destDir)
                } finally {
                    fos.flush()
                    fos.close()
                }
            } else {
                FileUtils.copyFileToDirectory(file, destDir)
            }
        }

    }

    private inner class PlaitJarTask(
        var fromJar: File,
        val outJar: File,
        val map: TraceConfig?
    ) : Runnable {
        override fun run() {
            innerTraceMethodFromJar(fromJar, outJar, map)
        }

        private fun innerTraceMethodFromJar(
            input: File,
            output: File,
            map: TraceConfig?
        ) {
            var zipOutputStream: ZipOutputStream? = null
            var zipFile: ZipFile? = null
            try {
                zipOutputStream = ZipOutputStream(FileOutputStream(output))
                zipFile = ZipFile(input)
                val enumeration = zipFile.entries()
                while (enumeration.hasMoreElements()) {
                    val zipEntry = enumeration.nextElement()
                    val zipEntryName = zipEntry.name
                    val inputStream = zipFile.getInputStream(zipEntry)

                    val originBytes = inputStream.readBytes()
                    var byteArrayInputStream: InputStream? = null
                    try {
                        if (zipEntryName.endsWith(".class")) {
                            val cr = ClassReader(originBytes)
                            val cw = ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
                            //需要处理的类使用自定义的visitor来处理
                            val visitor = PlaitClassVisitor(cw, map)
                            cr.accept(visitor, ClassReader.EXPAND_FRAMES)

                            val bytes = cw.toByteArray()
                            if (bytes.isNotEmpty()) {
                                byteArrayInputStream = ByteArrayInputStream(bytes)
                            }
                        }
                    }catch (e: Exception) {
                        e.printStackTrace()
                        log( "trace jar entry:$zipEntryName error:${e.message}")
                    }
                    if (byteArrayInputStream == null) {
                        byteArrayInputStream = ByteArrayInputStream(originBytes)
                    }
                    val newZipEntry = ZipEntry(zipEntryName)
                    FileUtil.addZipEntry(zipOutputStream, newZipEntry, byteArrayInputStream)

                    inputStream.close()
                }
            } catch (e: java.lang.Exception) {
                log( "trace jar:${input.absolutePath} error:${e.message}")
                try {
                    Files.copy(input.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING)
                } catch (e1: java.lang.Exception) {
                    e1.printStackTrace()
                }
            } finally {
                try {
                    if (zipOutputStream != null) {
                        zipOutputStream.finish()
                        zipOutputStream.flush()
                        zipOutputStream.close()
                    }
                    zipFile?.close()
                } catch (e: java.lang.Exception) {
                }
            }
        }
    }

}