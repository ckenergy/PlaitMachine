package com.ckenergy.trace

import org.objectweb.asm.AnnotationVisitor

private const val TAG = "===ListAnnotationVisitor==="
/**
 * 注解上内容是列表
 * Created by chengkai on 2022/6/14.
 */
class ListAnnotationVisitor(api: Int, annotationVisitor: AnnotationVisitor?, val list: MutableList<Any?>) :
    AnnotationVisitor(api, annotationVisitor) {

    override fun visit(name: String?, value: Any?) {
        super.visit(name, value)
        if (value != null) list.add(value)
        log( "visitArray name:${name}, value:$value")
    }

    override fun visitAnnotation(
        name: String?,
        descriptor: String?
    ): AnnotationVisitor {
        log( "visitArray visitAnnotation :${name}, descriptor:$descriptor")
        val annotationVisitor = super.visitAnnotation(name, descriptor)
        if (descriptor.isNullOrBlank()) {
            return annotationVisitor
        }
        val map = hashMapOf<String, Any?>()
        list.add(descriptor.replace(";", "") to map)
        return PlaintAnnotationVisitor(api, annotationVisitor, map)
    }

    override fun visitArray(name: String?): AnnotationVisitor {
        log( "visitArray in visitArray :${name}")
        val annotationVisitor1 = super.visitArray(name)
        if (name != null) {
            val list1 = ArrayList<Any?>()
            list.add(list1)
            return ListAnnotationVisitor(api, annotationVisitor1, list1)
        } else {
            return annotationVisitor1
        }
    }

    override fun visitEnum(name: String?, descriptor: String?, value: String?) {
        super.visitEnum(name, descriptor, value)
        if (value != null && descriptor != null) {
            val annotionWrap = AnnotionWrap(descriptor, value)
            list.add(annotionWrap)
        }
        log( "visitArrayEnmum name:$name, value:$value, descriptor:$descriptor， listClass:${list is ArrayList<*>}")
    }

    private fun log(info: String) {
        Log.d(TAG, info)
    }

}