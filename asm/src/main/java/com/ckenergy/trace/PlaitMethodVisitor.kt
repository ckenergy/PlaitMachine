package com.ckenergy.trace

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class PlaitMethodVisitor(
    methodVisitor: MethodVisitor,
    access: Int,
    name: String?,
    descriptor: String?
) : AdviceAdapter(Opcodes.ASM5, methodVisitor, access, name, descriptor) {

    //方法开始,此处可在方法开始插入字节码
    override fun onMethodEnter() {
        super.onMethodEnter()
        println("onMethodEnter name:$name")
        mv.visitLdcInsn(name)
//        mv.visitVarInsn(ALOAD, 0)
//        mv.visitVarInsn(ALOAD, 1)
        mv.visitMethodInsn(
            INVOKESTATIC,
            Contants.TRACELOG_CLASS,
            "i",
            "(Ljava/lang/String;)V",
            false
        )
    }

    override fun onMethodExit(opcode: Int) {
        super.onMethodExit(opcode)
        println("onMethodExit name:$name")
        mv.visitMethodInsn(
            INVOKESTATIC,
            Contants.TRACELOG_CLASS,
            "o",
            "()V",
            false
        )
    }

    //指令操作,这里可以判断拦截return,并在方法尾部插入字节码
//    override fun visitInsn(opcode: Int) {
//        if (opcode == ARETURN || opcode == RETURN) {
//            mv.visitLdcInsn("MainActivity");
//            mv.visitLdcInsn("tttInsn");
//            mv.visitMethodInsn(
//                    INVOKESTATIC,
//                    "android/util/Log",
//                    "i",
//                    "(Ljava/lang/String;Ljava/lang/String;)I",
//                    false
//            );
//            mv.visitInsn(POP);
//        }
//        super.visitInsn(opcode)
//    }
//
//    //方法栈深度
//    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
//        super.visitMaxs(maxStack, maxLocals)
//    }
//
//    //方法结束回调
//    override fun visitEnd() {
//        super.visitEnd()
//    }
}