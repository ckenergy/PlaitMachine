package com.ckenergy.trace.task

import com.android.build.api.transform.Status
import com.ckenergy.trace.Constants.TAG
import com.ckenergy.trace.TraceManager
import com.ckenergy.trace.extension.PlaitExtension
import com.ckenergy.trace.utils.Log
import com.ckenergy.trace.utils.TraceUtil
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.zip.ZipFile

private const val ORIGIN_NAME = "origin"

abstract class TransformClassesTask : DefaultTask() {

    private var pluginInfo: PlaitExtension? = null

    @get:InputFiles
    abstract val allDirectories: ListProperty<Directory>

    @get:InputFiles
    abstract val allJars: ListProperty<RegularFile>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun taskAction() {
        val start = System.currentTimeMillis()

        val filterAllJar = TraceUtil.filterJetifiedJar(allJars.get().map { file -> file.asFile })

        Log.w(TAG,"pluginInfo:$pluginInfo")

        if (pluginInfo?.enable == true) {

            val outputDirectory = output.get().asFile.absoluteFile
            val traceDir = File(outputDirectory.parentFile, "trace")
            val originDir = File(outputDirectory.parentFile, ORIGIN_NAME)
            if (!originDir.exists()) {
                originDir.mkdirs()
            }
            val copyJar = System.currentTimeMillis()
            allDirectories.get().forEach { directory ->
                Log.w(TAG,"directory:${directory.asFile.absolutePath}")
                directory.asFile.walk().forEach { file ->
                    if (file.isFile) {
                        val relativePath = file.toRelativeString(directory.asFile)
                        FileUtils.copyFile(file, File(originDir, relativePath))
                    }
                }
            }
            Log.w(TAG, " save jar cost time: ${System.currentTimeMillis() - copyJar}ms.")

            val allFile = filterAllJar + originDir

            TraceManager(pluginInfo).doTransform(
                classInputs = allFile,
                changedFiles = ConcurrentHashMap<File, Status>(),
                isIncremental = false,
                traceClassDirectoryOutput = traceDir,
                inputToOutput = ConcurrentHashMap(),
            )
            val saveJarTime = System.currentTimeMillis()
            saveFile(listOf(traceDir) , output.get().asFile)
            Log.w(TAG, " save jar cost time: ${System.currentTimeMillis() - saveJarTime}ms.")
        }else {
            val allFile = filterAllJar + allDirectories.get().map {
                it.asFile
            }
            saveFile(allFile, output.get().asFile)
        }

        val cost = System.currentTimeMillis() - start
        Log.w(TAG, "cost time: ${cost}ms.")
    }

    private fun saveFile(input: List<File>, out: File) {
        if (input.size == 1 && input.first().isFile) {
            FileUtils.copyFile(input.first(), out)
        }else {
            val jarOutput = JarOutputStream(
                BufferedOutputStream(
                    FileOutputStream(
                        out
                    )
                )
            )
            input.forEach {
                jarOutput.saveInJar(it)
            }
            jarOutput.close()
        }
    }

    private fun JarOutputStream.saveInJar(traceDir: File) {
        if (traceDir.isDirectory) {
            traceDir.walk().forEach {
                if (it.isFile) {
                    if (it.name.endsWith(".jar")) {
                        decodeAndSaveJar(it)
                    }else {
                        val name = it.toRelativeString(traceDir).removePrefix(ORIGIN_NAME +File.separator).ifBlank { it.name }
                        saveEntry(name, FileInputStream(it))
                    }
                }
            }
        }else if (traceDir.name.endsWith(".jar")) {
            decodeAndSaveJar(traceDir)
        }else {
            saveEntry(traceDir.name, FileInputStream(traceDir))
        }
    }

    private fun JarOutputStream.decodeAndSaveJar(source: File) {
        val zipFile = ZipFile(source)
        val enumeration = zipFile.entries()
        while (enumeration.hasMoreElements()) {
            val entry = enumeration.nextElement()
            try {
                if (!entry.isDirectory) {
                    saveEntry(entry.name, zipFile.getInputStream(entry))
                }
            } catch (e: Exception) {
                if (!entry.name.contains("META-INF") && entry.name.endsWith(".class") && entry.name != "module-info.class") {
                    error("${e.message},file:${entry.name}, jar:${source.absoluteFile}")
                }else{
//                    Log.i("TransformYApmClassesTask", "${e.message},file:${entry.name}, jar:${source.absoluteFile}")
                }
            }
        }
        try {
            zipFile.close()
        } catch (e: Exception) {
        }
    }

    private fun JarOutputStream.saveEntry(entryName: String, inputStream: InputStream) {
        this.putNextEntry(JarEntry(entryName))
        inputStream.use {
            IOUtils.copy(it, this)
//            it.copyTo(this)
        }
        this.closeEntry()
    }

    class CreationAction(
        private val pluginInfo: PlaitExtension?,
    ) : Action<TransformClassesTask> {

        override fun execute(task: TransformClassesTask) {
            task.pluginInfo = pluginInfo
        }
    }

}