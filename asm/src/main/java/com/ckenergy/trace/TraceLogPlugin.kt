package com.ckenergy.trace

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * ckenergy 2021-04-19
 */
class TraceLogPlugin : Plugin<Project> {
//    private static final String TAG = "PlaitMachine"

    override fun apply(project: Project) {

        if (project.plugins.hasPlugin(AppPlugin::class.java)) {
            project.extensions.create("TraceLog", TraceLogExtension::class.java)
            val transform = TraceLogTransform.newTransform()
            val appExtension = project.extensions.findByType(AppExtension::class.java)
            appExtension?.registerTransform(transform)

            project.afterEvaluate {
                val configuration: TraceLogExtension? =
                    it.extensions.getByName("TraceLog") as? TraceLogExtension
//                    it.extensions.getByName("plaitMachine") as? TraceLogExtension
                Log.printLog = configuration?.logInfo ?: false
                Log.d(TraceLogTransform.TAG, "configuration:${configuration?.enable}")
                //注入PlaitMachineTransform
                transform.traceLogExtension = configuration

            }
        }
    }

}
