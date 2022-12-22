package com.ckenergy.trace

import com.android.build.gradle.AppExtension
import com.ckenergy.trace.extension.PlaitExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * ckenergy 2021-04-19
 */
class PlaitPlugin : Plugin<Project> {
//    private static final String TAG = "PlaitPlugin"

    override fun apply(project: Project) {

        project.extensions.create("plaitMachine", PlaitExtension::class.java, project)
        val transform = PlaintTransform.newTransform()
        val appExtension = project.extensions.findByType(AppExtension::class.java)
        appExtension?.registerTransform(transform)

        project.afterEvaluate {
            val configuration: PlaitExtension? =
                it.extensions.getByName("plaitMachine") as? PlaitExtension
            Log.printLog = configuration?.logInfo ?: false
            Log.d(PlaintTransform.TAG, "afterEvaluate configuration:${configuration?.enable}, method:${configuration?.plaitClass?.asMap}")
            //注入PlaitMachineTransform
            transform.plaintMachineExtension = configuration

        }
//        if (project.plugins.hasPlugin(AppPlugin::class.java)) {
//        }
    }

}
