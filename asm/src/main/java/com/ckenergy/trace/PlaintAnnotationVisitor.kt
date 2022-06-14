package com.ckenergy.trace

import org.objectweb.asm.AnnotationVisitor

private const val TAG = "===PlaintAnnotationVisitor==="
/**
 * Created by chengkai on 2022/6/14.
 */
class PlaintAnnotationVisitor(api: Int, annotationVisitor: AnnotationVisitor?, val annoMap: MutableMap<String, Any?>) :
    AnnotationVisitor(api, annotationVisitor) {

    override fun visit(name: String?, value: Any?) {
        super.visit(name, value)
        log( "visit name:$name, value:$value")
        if (name != null)
            annoMap[name] = value
    }

    override fun visitEnum(name: String?, descriptor1: String, value: String?) {
        super.visitEnum(name, descriptor1, value)
        log("visitEnum name:$name, descriptor:$descriptor1, value:$value")
        if (name != null) annoMap[name] = value
    }

    override fun visitAnnotation(name: String?, descriptor: String?): AnnotationVisitor {
        log( "visitAnnotation name:$name, descriptor:$descriptor")
        val annotationVisitor1 = super.visitArray(name)
        return if (name != null && descriptor != null) {
            val map = hashMapOf<String, Any?>()
            annoMap["$name"] = descriptor.replace(";", "") to map
            PlaintAnnotationVisitor(api, annotationVisitor1, map)
        } else {
            annotationVisitor1
        }
    }

    override fun visitArray(name: String?): AnnotationVisitor {
        log( "visitArray name:$name")
        val annotationVisitor1 = super.visitArray(name)
        return if (name != null) {
            val list1 = ArrayList<Any?>()
            annoMap[name] = list1
            ListAnnotationVisitor(api, annotationVisitor1, list1)
        } else {
            annotationVisitor1
        }

    }

    private fun log(info: String) {
        Log.d(TAG, info)
    }

}