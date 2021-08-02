package com.ckenergy.trace

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.tree.AbstractInsnNode
import java.util.HashMap

class PlaitMethodVisitor(
    val className: String,
    methodVisitor: MethodVisitor,
    access: Int,
    name: String?,
    descriptor: String?
) : AdviceAdapter(Opcodes.ASM5, methodVisitor, access, name, descriptor) {

    val annotations = HashMap<String, Map<String, Any?>?>()

    //方法开始,此处可在方法开始插入字节码
    override fun onMethodEnter() {
        super.onMethodEnter()
        Log.d(TraceLogTransform.TAG,"onMethodEnter name:$name")
        var traceName: CharSequence = "===$className.$name"
        val start = (traceName.length - 126).coerceAtLeast(0)
        traceName = traceName.subSequence(start, traceName.length)//不能超过127个字符
        mv.visitLdcInsn(traceName)
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
//        Log.d(TraceLogTransform.TAG,"onMethodExit name:$name")
        mv.visitMethodInsn(
            INVOKESTATIC,
            Contants.TRACELOG_CLASS,
            "o",
            "()V",
            false
        )
    }

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
        println("visitAnnotation descriptor:$descriptor, visible:$visible")
        return object : AnnotationVisitor(Opcodes.ASM5) {
            override fun visit(name: String, value: Any?) {
                super.visit(name, value)
                var item = annotations[descriptor] as? HashMap
                if (item == null) {
                    item = hashMapOf(name to value)
                    annotations[descriptor] = item
                }else {
                    item[name] = value
                    println("visitAnnotation name:$name, value:$value")
                }
            }
        }
    }

//    private fun isGetSetMethod(): Boolean {
//        // complex method
////            if (!isConstructor && instructions.size() > 20) {
////                return false;
////            }
//        var ignoreCount = 0
//        val iterator: ListIterator<AbstractInsnNode> = instructions.iterator()
//        while (iterator.hasNext()) {
//            val insnNode = iterator.next()
//            val opcode = insnNode.opcode
//            if (-1 == opcode) {
//                continue
//            }
//            if (opcode != GETFIELD && opcode != GETSTATIC && opcode != H_GETFIELD && opcode != H_GETSTATIC && opcode != RETURN && opcode != ARETURN && opcode != DRETURN && opcode != FRETURN && opcode != LRETURN && opcode != IRETURN && opcode != PUTFIELD && opcode != PUTSTATIC && opcode != H_PUTFIELD && opcode != H_PUTSTATIC && opcode > SALOAD) {
//                if (isConstructor && opcode == INVOKESPECIAL) {
//                    ignoreCount++
//                    if (ignoreCount > 1) {
////                            Log.e(TAG, "[ignore] classname %s, name %s", className, name);
//                        return false
//                    }
//                    continue
//                }
//                return false
//            }
//        }
//        return true
//    }
//
//    private fun isSingleMethod(): Boolean {
//        val iterator: ListIterator<AbstractInsnNode> = instructions.iterator()
//        while (iterator.hasNext()) {
//            val insnNode = iterator.next()
//            val opcode = insnNode.opcode
//            if (-1 == opcode) {
//                continue
//            } else if (opcode in INVOKEVIRTUAL..INVOKEDYNAMIC) {
//                return false
//            }
//        }
//        return true
//    }
//
//
//    private fun isEmptyMethod(): Boolean {
//        val iterator: ListIterator<AbstractInsnNode> = instructions.iterator()
//        while (iterator.hasNext()) {
//            val insnNode = iterator.next()
//            val opcode = insnNode.opcode
//            return if (-1 == opcode) {
//                continue
//            } else {
//                false
//            }
//        }
//        return true
//    }

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