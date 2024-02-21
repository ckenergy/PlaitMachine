package com.ckenergy.trace

import com.android.build.api.transform.Status
import com.ckenergy.trace.extension.PlaintConfig
import com.ckenergy.trace.extension.PlaitExtension
import com.ckenergy.trace.extension.PlaitMethodList
<<<<<<<< HEAD:plaitPlugin/plait-plugin/src/main/java/com/ckenergy/trace/TraceManager.kt
import com.ckenergy.trace.utils.FileUtil
import com.ckenergy.trace.utils.Log
import org.apache.commons.io.FileUtils
========
import com.ckenergy.trace.extension.PlaintConfig
import org.apache.commons.codec.digest.DigestUtils
>>>>>>>> 8d7264b4744bfeedeffb4f670b9f50cb26d0bc8f:plaitPlugin/plait-plugin/src/main/java/com/ckenergy/trace/PlaintTransform.kt
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import com.ckenergy.trace.Constants.TAG

/**
 * @author yeahka
 * @date 2023/12/6
 * @desc
 */
<<<<<<<< HEAD:plaitPlugin/plait-plugin/src/main/java/com/ckenergy/trace/TraceManager.kt
class TraceManager(private val pluginInfo: PlaitExtension?) {
========
class PlaintTransform : Transform() {
>>>>>>>> 8d7264b4744bfeedeffb4f670b9f50cb26d0bc8f:plaitPlugin/plait-plugin/src/main/java/com/ckenergy/trace/PlaintTransform.kt

    companion object {

<<<<<<<< HEAD:plaitPlugin/plait-plugin/src/main/java/com/ckenergy/trace/TraceManager.kt
        private fun log(info: String) {
//            Log.d(TAG, info)
========
        fun newTransform(): PlaintTransform {
            return PlaintTransform()
>>>>>>>> 8d7264b4744bfeedeffb4f670b9f50cb26d0bc8f:plaitPlugin/plait-plugin/src/main/java/com/ckenergy/trace/PlaintTransform.kt
        }
        private fun transformMap(plaitExtension: PlaitExtension): PlaintConfig {
            val classMap = HashMap<String, ArrayList<PlaitMethodList>?>()
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
                        map = classMap
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
                    map[it.name] = list
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
                    blackPackages[it.name] = list1
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
            return PlaintConfig(classMap, packages, blackPackages)
        }

    }

    private var plaintConfig: PlaintConfig? = null

    private var enable = false

    private val executor: ExecutorService = Executors.newFixedThreadPool(16)

<<<<<<<< HEAD:plaitPlugin/plait-plugin/src/main/java/com/ckenergy/trace/TraceManager.kt
    fun doTransform(
        classInputs: Collection<File>,
        changedFiles: Map<File, Status>,
        inputToOutput: Map<File, File>,
        isIncremental: Boolean,
        traceClassDirectoryOutput: File,
    ) {
========
    override fun getName() = "PlaintTransform"

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
>>>>>>>> 8d7264b4744bfeedeffb4f670b9f50cb26d0bc8f:plaitPlugin/plait-plugin/src/main/java/com/ckenergy/trace/PlaintTransform.kt
        val startTime = System.currentTimeMillis()

        if (!traceClassDirectoryOutput.exists()) {
            traceClassDirectoryOutput.mkdirs()
        }

        enable = pluginInfo?.enable == true
        Log.w(TAG, "enable:$enable")
        if (enable) {
            plaintConfig = transformMap(pluginInfo!!)
            println("$TAG >>> collect timeCount：${System.currentTimeMillis() - startTime}")
            trace(classInputs, changedFiles, inputToOutput, isIncremental, traceClassDirectoryOutput)
        }else {
            val fileList =  if (isIncremental) {
                //增量更新，只 操作有改动的文件
                changedFiles.filter { (file, status) ->
                    val dest = if (inputToOutput.containsKey(file)) {
                        inputToOutput[file]!!
                    } else {
                        File(traceClassDirectoryOutput, file.name)
                    }
                    if (status == Status.REMOVED) {
                        if (dest.exists()) {
                            dest.delete()
                        }
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                    status != Status.REMOVED
                }.map { (file, status) -> file }
            }else {
                classInputs
            }
            fileList.forEach {
                val dirOutput = if (inputToOutput.containsKey(it)) {
                    inputToOutput[it]!!
                } else {
                    File(traceClassDirectoryOutput, it.name)
                }
                if (it.exists()) {
                    if (it.isDirectory) {
                        it.walk().forEach { it1 ->
                            val out = toOutputFile(dirOutput, it, it1)
                            copyFileAndMkdirsAsNeed(it1, out)
                        }
                    }else {
                        FileUtils.copyFile(it, dirOutput)
                    }
                }
            }

        }
        println("${TAG} >>> timeCount：${System.currentTimeMillis() - startTime}")
    }

    private fun toOutputFile(outputDir: File, inputDir: File, inputFile: File): File {
        return File(outputDir, inputFile.toRelativeString(inputDir))
    }

    private fun copyFileAndMkdirsAsNeed(from: File, to: File) {
        if (from.exists()) {
            to.parentFile.mkdirs()
            if (from.isDirectory) {
//                FileUtils.copyDirectoryToDirectory(from, to)
            }else {
                FileUtils.copyFile(from, to)
            }
        }
    }

    private fun trace(
        classInputs: Collection<File>,
        changedFiles: Map<File, Status>,
        inputToOutput: Map<File, File>,
        isIncremental: Boolean,
        traceClassDirectoryOutput: File,
    ) {
        val tasks = ArrayList<Future<*>>()
        if (isIncremental) {
            //增量更新，只 操作有改动的文件
            changedFiles.forEach { (file, status) ->
                val dest = if (inputToOutput.containsKey(file)) {
                    inputToOutput[file]!!
                } else {
                    File(traceClassDirectoryOutput, file.name)
                }
                if (status == Status.ADDED || status == Status.CHANGED) {
                    traceFile(tasks, file, dest)
                } else if (status == Status.REMOVED) {
                    if (dest.exists()) {
                        dest.delete()
                    }
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        }else {
            classInputs.forEach {
                val dirOutput = if (inputToOutput.containsKey(it)) {
                    inputToOutput[it]!!
                } else {
                    File(traceClassDirectoryOutput, it.name)
                }
                traceFile(tasks, it, dirOutput)
            }
        }

        //等待所有任务运行完毕
        tasks.forEach {
            it.get()
        }
    }

    private fun traceFile(tasks: MutableList<Future<*>>, srcDir: File, dest: File) {
        if (!srcDir.exists()) return
        if (srcDir.isDirectory) {
            srcDir.walk().forEach {
                val destName = it.absolutePath.replace(srcDir.absolutePath, dest.absolutePath)
                if(it.isFile) {
                    executor.submit(getTraceTask(it, File(destName))).apply { tasks.add(this) }
                }
            }
        } else {
            executor.submit(getTraceTask(srcDir, dest)).apply { tasks.add(this) }
        }
    }

    private fun getTraceTask(origin: File, dest: File): Runnable {
        return if(origin.name.endsWith(".jar")) {
            PlaitJarTask(origin, dest, plaintConfig)
        } else {
            PlaitAction(origin, dest.absolutePath, plaintConfig)
        }
    }

    private inner class PlaitAction(
        val file: File,
        val dest: String,
        val map: PlaintConfig?
    ) :
        Runnable {

        override fun run() {
            val destFile = File(dest)
            if (destFile.exists()) {
                destFile.delete()
            }
            val destDir = destFile.parentFile
            FileUtils.forceMkdir(destDir)
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
                    Log.d(TAG,">>>>>>>>> PlaitAction:$name error :${e.printStackTrace()}")
                    FileUtils.copyFileToDirectory(file, destDir)
                } finally {
                    fos.flush()
                    fos.close()
                }
            } else {
                try {
                    FileUtils.copyFileToDirectory(file, destDir)
                }catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    }

    private inner class PlaitJarTask(
        var fromJar: File,
        val outJar: File,
        val map: PlaintConfig?
    ) : Runnable {
        override fun run() {
            innerTraceMethodFromJar(fromJar, outJar, map)
        }

        private fun innerTraceMethodFromJar(
            input: File,
            output: File,
            map: PlaintConfig?
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
                        Log.d(TAG,"trace jar entry:$zipEntryName error:${e.message}")
                    }
                    if (byteArrayInputStream == null) {
                        byteArrayInputStream = ByteArrayInputStream(originBytes)
                    }
                    val newZipEntry = ZipEntry(zipEntryName)
                    FileUtil.addZipEntry(zipOutputStream, newZipEntry, byteArrayInputStream)

                    inputStream.close()
                }
            } catch (e: java.lang.Exception) {
                Log.d(TAG,"trace jar:${input.absolutePath} error:${e.message}")
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