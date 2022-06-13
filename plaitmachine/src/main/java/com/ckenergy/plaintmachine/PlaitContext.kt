package com.ckenergy.plaintmachine

import io.leangen.geantyref.TypeFactory
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by chengkai on 2021/7/28.
 */
class PlaitContext(
    val methodName: String,
    val current: Any?,
    val args: Array<Any>?,
    annotations: HashMap<String, HashMap<String, Any>>?
) {
    val annotationList: MutableList<Annotation?>?

    init {
        if (annotations != null && annotations.isNotEmpty()) {
            annotationList = ArrayList(annotations.size)
            annotations.forEach {
                try {
                    val annotation = TypeFactory.annotation(Class.forName(it.key.substring(1).replace("/", ".")) as Class<Annotation>, it.value) as Annotation
//                    val annotation = AnnotationParser.annotationForMap(
//                        Class.forName(it.key.substring(1).replace("/", ".")) as Class<Annotation>, it.value) as Annotation
                    annotationList.add(annotation)
                }catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }else {
            annotationList = null
        }
    }

    override fun toString(): String {
        return "PlaitContext(methodName='$methodName', current=$current, args=${args?.contentToString()}, annotationList=$annotationList)"
    }

}