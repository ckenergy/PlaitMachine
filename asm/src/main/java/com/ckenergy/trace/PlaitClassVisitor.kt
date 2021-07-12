package com.ckenergy.trace

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class PlaitClassVisitor(
    classVisitor: ClassVisitor,
) : ClassVisitor(Opcodes.ASM5, classVisitor) {

    private var className: String? = null
    private var superName: String? = null

    private var isABSClass = false

    //类入口
    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        className = name
        this.superName = superName
        //如果是接口 isABSClass =true
        if (access and Opcodes.ACC_INTERFACE > 0) {
            this.isABSClass = true
        }

        println("visit name:$name, isABSClass:$isABSClass, ")
    }

    override fun visitOuterClass(owner: String?, name: String?, descriptor: String?) {
        super.visitOuterClass(owner, name, descriptor)

    }

//    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor {
//        val result =  super.visitAnnotation(descriptor, visible)
////        println("visitMethod name:$name")
//        if (isABSClass || className.isNullOrEmpty()) {
//            return result
//        }
//        return result
//    }

    //类中方法的入口
    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val result = super.visitMethod(access, name, descriptor, signature, exceptions)
//        println("visitMethod name:$name")
        if (isABSClass || className.isNullOrEmpty()) {
            return result
        }
        if (name == "<clinit>" || "<init>" == name) {
            return result
        }
        return PlaitMethodVisitor(result, access, name, descriptor)
    }
}