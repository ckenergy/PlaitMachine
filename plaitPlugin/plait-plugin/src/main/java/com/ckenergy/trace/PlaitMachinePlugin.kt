package com.ckenergy.trace

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.gradle.AppExtension
import com.ckenergy.trace.extension.PlaitExtension
import com.ckenergy.trace.task.TransformClassesTask
import com.ckenergy.trace.utils.Log
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * ckenergy 2021-04-19
 */
open class PlaitMachinePlugin : Plugin<Project> {

    private lateinit var project: Project

    private val agpVersion by lazy { getKotlinVersion(project) }

    override fun apply(project: Project) {
        this.project = project

        val configuration = project.extensions.create("plait", PlaitExtension::class.java)
        if (project.plugins.hasPlugin("com.android.application")) {
            Log.printLog = configuration.logInfo
            Log.d(PlaintMachineTransform.TAG, "afterEvaluate configuration:${configuration.enable}, method:${configuration.plaitClass?.asMap}")
            if (isAGP8()) {
                val extension1 = project.extensions.getByType(AndroidComponentsExtension::class.java)
                extension1.onVariants(extension1.selector().all()) { variant ->
                    val taskProviderTransformClassesTask =
                        project.tasks.register(
                            "${variant.name}TransformPlaitClassesTask",
                            TransformClassesTask::class.java,
                            TransformClassesTask.CreationAction(configuration)
                        )
                    // https://github.com/android/gradle-recipes
                    variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                        .use(taskProviderTransformClassesTask)
                        .toTransform(
                            ScopedArtifact.CLASSES,
                            TransformClassesTask::allJars,
                            TransformClassesTask::allDirectories,
                            TransformClassesTask::output
                        )
                }
            }else {
                val appExtension = project.extensions.findByType(AppExtension::class.java)
                val transform = PlaintMachineTransform.newTransform()
                appExtension?.registerTransform(transform)
                transform?.plaintMachineExtension = configuration
            }
        }

    }

    private fun isAGP8(): Boolean {
        println("apg:$agpVersion")

        var useNew = false
        if (agpVersion != null) {
            // 将版本字符串转换为数字形式，例如 "4.2.0" 转换为 420
            val agpVersionNumber = agpVersion?.replace(".", "")?.toInt() ?: 0
            if (agpVersionNumber >= 800) {
                useNew = true
            }
        }
        return useNew
    }

    private fun getKotlinVersion(project: Project): String? {
        val buildscriptDependencies =
            project.rootProject.buildscript.configurations.getByName("classpath").resolvedConfiguration.resolvedArtifacts.map { it.moduleVersion.id.toString() }
//        var kotlinVersion: String? = null
        var androidGradlePluginVersion: String? = null
        val result = buildscriptDependencies.map { Pair(it, it.split(":")) }.map {
            val group = it.second[0]
            val artifact = it.second[1]
            val version = it.second[2]

//            if (group == "org.jetbrains.kotlin" && artifact == "kotlin-gradle-plugin")
//                kotlinVersion = version
            if (group == "com.android.tools.build" && artifact == "gradle")
                androidGradlePluginVersion = version
        }

        println(
            "androidGradlePluginVersion:$androidGradlePluginVersion"
        )
        return androidGradlePluginVersion
    }

}
