package com.ckenergy.trace

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.ckenergy.trace.extension.PlaitExtension
import com.ckenergy.trace.utils.Log
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by chengkai on 2021/3/5.
 */
class PlaintMachineTransform : Transform() {

    companion object {
        const val TAG = "===PlaintMachineTransform==="

        fun newTransform(): PlaintMachineTransform {
            return PlaintMachineTransform()
        }

    }

    var plaintMachineExtension: PlaitExtension? = null

    override fun getName(): String = javaClass.simpleName

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    //是否支持增量更新
    override fun isIncremental() = false

    override fun transform(transformInvocation: TransformInvocation) {
        transforming(transformInvocation)
    }

    private fun transforming(invocation: TransformInvocation) {
        val outputProvider = invocation.outputProvider!!
        val isIncremental = invocation.isIncremental && this.isIncremental

        if (!isIncremental) {
            outputProvider.deleteAll()
        }

        val changedFiles = ConcurrentHashMap<File, Status>()
        val inputToOutput = ConcurrentHashMap<File, File>()
        val inputFiles = ArrayList<File>()

        var transformDirectory: File? = null

        for (input in invocation.inputs) {
            for (directoryInput in input.directoryInputs) {
                changedFiles.putAll(directoryInput.changedFiles)
                val inputDir = directoryInput.file
                inputFiles.add(inputDir)
                val outputDirectory = outputProvider.getContentLocation(
                    directoryInput.name,
                    directoryInput.contentTypes,
                    directoryInput.scopes,
                    Format.DIRECTORY
                )

                inputToOutput[inputDir] = outputDirectory
                if (transformDirectory == null) transformDirectory = outputDirectory.parentFile
            }

            for (jarInput in input.jarInputs) {
                val inputFile = jarInput.file
                changedFiles[inputFile] = jarInput.status
                inputFiles.add(inputFile)
                val outputJar = outputProvider.getContentLocation(
                    jarInput.name,
                    jarInput.contentTypes,
                    jarInput.scopes,
                    Format.JAR
                )

                inputToOutput[inputFile] = outputJar
                if (transformDirectory == null) transformDirectory = outputJar.parentFile
            }
        }

        if (inputFiles.size == 0 || transformDirectory == null) {
            Log.d(TAG, "trace do not find any input files")
            return
        }

        // Get transform root dir.
        val outputDirectory = transformDirectory

        TraceManager(plaintMachineExtension).doTransform(
            classInputs = inputFiles,
            changedFiles = changedFiles,
            isIncremental = isIncremental,
            traceClassDirectoryOutput = outputDirectory,
            inputToOutput = inputToOutput,
        )

    }

}