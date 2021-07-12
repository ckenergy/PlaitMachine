package com.ckenergy.trace

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.ide.common.internal.WaitableExecutor
import com.android.utils.FileUtils
import org.apache.commons.io.FileUtils.forceMkdir
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils.forceMkdir
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Created by chengkai on 2021/3/5.
 */
class TraceLogTransform() : Transform() {

    companion object {
        const val TAG = "TraceLogTransform"

        fun newTransform(): TraceLogTransform {
            return TraceLogTransform()
        }

    }

    var traceLogExtension: TraceLogExtension? = null

    //创建大小为16的线程池
    private val executor = WaitableExecutor.useGlobalSharedThreadPool()

    override fun getName() = TAG

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    //是否支持增量更新
    override fun isIncremental() = true

    override fun transform(transformInvocation: TransformInvocation?) {
        val extension = traceLogExtension
        val enable = extension != null && extension.enable
        val startTime = System.currentTimeMillis()

        //是否增量编译
        val isIncremental = transformInvocation!!.isIncremental && this.isIncremental
        val outputProvider = transformInvocation.outputProvider
        val inputs = transformInvocation.inputs

        for (input in inputs) {
            println("====$TAG=======, dir: ${inputs.size}, ${input.directoryInputs.size}")
            for (directoryInput in input.directoryInputs) {
                val srcDir = directoryInput.file.absolutePath
                println("====$TAG=======, directoryInput: ${srcDir}")
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
//                    println("====$TAG=======, directoryInput:$directoryInput ，size:${directoryInput.file.length()}")
                    fileStatusMap.forEach { (file, status) ->
//                        println("====$TAG=======, changedFiles:$file,status:$status")

                        if (status == Status.ADDED || status == Status.CHANGED) {
                            val action = if (enable) plait(file, srcDir, dest) else null
                            if (action != null) {
                                executor.execute {
                                    action.run()
                                }
                            } else {
                                FileUtils.copyFileToDirectory(file, dest)
                            }
                        } else if (status == Status.REMOVED) {
//                            println("====$TAG=======, dest:${dest?.exists()},file:${file.exists()}")

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
                        if (directoryInput.file.isDirectory) {
                            FileUtils.getAllFiles(directoryInput.file).forEach { file ->
                                val action = plait(file, srcDir, dest)
                                if (action != null) {
                                    executor.execute {
                                        action.run()
                                    }
                                } else {
                                    FileUtils.copyFileToDirectory(file, dest)
                                }
                            }
                        }
                    } else {
                        FileUtils.copyDirectoryToDirectory(directoryInput.file, dest)
                    }
                }
            }
            println("====$TAG=======,  changedJars size:${input.jarInputs.size} ,isIncremental:$isIncremental")
            for (inputJar in input.jarInputs) {
                //将jar也加进来,androidx需要这个
                val dest = outputProvider.getContentLocation(
                    inputJar.name,
                    inputJar.contentTypes,
                    inputJar.scopes,
                    Format.JAR
                )
                if (isIncremental) {//增量更新，只 操作有改动的文件
                    println("====$TAG=======, isIncremental inputJar:${inputJar.name},status:${inputJar.status}")
                    val status = inputJar.status
                    if (status == Status.REMOVED) {
                        if (dest?.exists() == true) {
                            dest.delete()
                        }
                    } else if (status == Status.NOTCHANGED) {//没变化的话只是简单的复制
//                        FileUtils.copyFile(inputJar.file, dest)
                        if (!enable) {
                            FileUtils.copyFile(inputJar.file, dest)
                        }
                    } else {
                        if (enable) {
                            executor.execute {
                                PlaitJarTask(inputJar.file, dest).run()
                            }
                        } else {
                            FileUtils.copyFile(inputJar.file, dest)
                        }
                    }
                } else {
//                    println("====$TAG=======, inputJar:${inputJar.name}")
                    if (enable) {
                        executor.execute {
                            PlaitJarTask(inputJar.file, dest).run()
                        }
                    } else {
                        FileUtils.copyFile(inputJar.file, dest)
                    }
                }
            }
        }
        //等待所有任务运行完毕
        executor.waitForTasksWithQuickFail<Runnable>(true)
        println("====transform >>> timeCount${System.currentTimeMillis() - startTime}")
    }

    private fun plait(file: File, input: String, dest: File): Runnable? {
        val name = file.name
        val destName = file.absolutePath.replace(input, dest.absolutePath)
//        println(">>>>>>>>> PlaitAction classPath :$name")
        if (name.endsWith(".class") && name != "R.class"
            && !name.startsWith("R\$") && name != ("BuildConfig.class")
        ) {//过滤出需要的class,将一些基本用不到的class去掉
            println(">>>>>>>>> PlaitAction filter classPath :${file.absolutePath}")
            return PlaitAction(file, destName)
        }
        return null
    }

    private class PlaitAction(val file: File, val dest: String) :
        Runnable {

        override fun run() {
            val destFile = File(dest)
            org.apache.commons.io.FileUtils.forceMkdir(destFile.parentFile)
            val classPath = dest
//            println(">>>>>>>>> classPath :$classPath")

            val name = file.absolutePath
            if (!name.contains("TraceTag", false)) {
                val fos = FileOutputStream(classPath)
                try {
                    val cr = ClassReader(file.readBytes())
                    val cw = ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
                    //需要处理的类使用自定义的visitor来处理
                    val visitor = PlaitClassVisitor(cw)
                    cr.accept(visitor, ClassReader.EXPAND_FRAMES)

                    val bytes = cw.toByteArray()
                    fos.write(bytes)
                } catch (e: Exception) {
                    e.printStackTrace()
                    println(">>>>>>>>> PlaitAction error :${e.printStackTrace()}")
                    FileUtils.copyFileToDirectory(file, destFile)
                } finally {
                    fos.flush()
                    fos.close()
                }
            }else {
                FileUtils.copyFileToDirectory(file, destFile)
            }
        }

    }

    private class PlaitJarTask(
        var fromJar: File,
        val outJar: File,
    ) : Runnable {
        override fun run() {
            innerTraceMethodFromJar(fromJar, outJar)
        }

        private fun innerTraceMethodFromJar(input: File, output: File) {
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
                    if (!zipEntryName.contains("TraceTag", false)) {
//                        println(">>>>>>>>> innerTraceMethodFromJar classPath :$zipEntryName")

                        val cr = ClassReader(inputStream)
                        val cw = ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
                        //需要处理的类使用自定义的visitor来处理
                        val visitor = PlaitClassVisitor(cw)
                        cr.accept(visitor, ClassReader.EXPAND_FRAMES)

                        val bytes = cw.toByteArray()
                        val byteArrayInputStream: InputStream = ByteArrayInputStream(bytes)
                        val newZipEntry = ZipEntry(zipEntryName)
                        FileUtil.addZipEntry(zipOutputStream, newZipEntry, byteArrayInputStream)
                    }else {
                        FileUtil.addZipEntry(zipOutputStream, zipEntry, inputStream)
                    }
                }
            } catch (e: java.lang.Exception) {
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