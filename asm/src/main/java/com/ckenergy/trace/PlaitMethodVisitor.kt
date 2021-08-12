package com.ckenergy.trace

import org.objectweb.asm.*
import org.objectweb.asm.commons.AdviceAdapter
import java.util.*

class PlaitMethodVisitor(
    val className: String,
    methodVisitor: MethodVisitor,
    access: Int,
    name: String?,
    val descriptor: String?
) : AdviceAdapter(Opcodes.ASM7, methodVisitor, access, name, descriptor) {

    val annotations = HashMap<String, Map<String, Any?>?>()

    //方法开始,此处可在方法开始插入字节码
    override fun onMethodEnter() {
        super.onMethodEnter()
        var traceName = "$className.$name"
        Log.d(TraceLogTransform.TAG, "onMethodEnter name:$traceName ,annotations:$annotations")
//        val start = (traceName.length - 126).coerceAtLeast(0)
//        traceName = traceName.subSequence(start, traceName.length)//不能超过127个字符
//        mv.visitLdcInsn(traceName)
////        mv.visitVarInsn(ALOAD, 0)
////        mv.visitVarInsn(ALOAD, 1)
//        mv.visitMethodInsn(
//            INVOKESTATIC,
//            Contants.TRACELOG_CLASS,
//            "i",
//            "(Ljava/lang/String;)V",
//            false
//        )

        val types = Type.getArgumentTypes(descriptor)
        val size = types.size
        mv.visitLdcInsn(size)
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object")
        val isStatic = AsmUtil.isStatic(access)
        val objsIndex = if (isStatic) size else size + 1
        mv.visitVarInsn(ASTORE, objsIndex)
        types.forEachIndexed { index, type ->
            mv.visitVarInsn(ALOAD, objsIndex)
            mv.visitLdcInsn(index)
            val opcode = getOpcode(type)
            mv.visitVarInsn(opcode, if (isStatic) index else index + 1)
            visitMethod(type, opcode)
            mv.visitInsn(AASTORE)
        }

        val mapIndex = objsIndex + 1
        visitMap(mapIndex)
        val traceInfoIndex = mapIndex + 2
        //构建traceinfo对象
        visiteTraceInfo(isStatic, traceName, objsIndex, mapIndex, traceInfoIndex)
        //注入方法
        mv.visitVarInsn(ALOAD, traceInfoIndex)
        mv.visitMethodInsn(
            INVOKESTATIC,
            Contants.TRACELOG_CLASS,
            "test", "(Lcom/hellotalk/tracelog/TraceInfo;)V",
            false
        )
    }

    // -------------
    private fun getOpcode(type: Type): Int {
        val t =
            when (type.sort) {
                Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> ILOAD
                Type.FLOAT -> FLOAD
                Type.LONG -> LLOAD
                Type.DOUBLE -> DLOAD
                else -> ALOAD
            }
        return t
    }

    //调用自动
    private fun visitMethod(type: Type, opcode: Int) {
        when (opcode) {
            ILOAD -> {
                when (type.sort) {
                    Type.BOOLEAN ->
                        mv.visitMethodInsn(
                            INVOKESTATIC,
                            "java/lang/Boolean",
                            "valueOf",
                            "(Z)Ljava/lang/Boolean;",
                            false
                        );
                    Type.CHAR ->
                        mv.visitMethodInsn(
                            INVOKESTATIC,
                            "java/lang/Character",
                            "valueOf",
                            "(C)Ljava/lang/Character;",
                            false
                        );
                    else ->
                        mv.visitMethodInsn(
                            INVOKESTATIC,
                            "java/lang/Integer",
                            "valueOf",
                            "(I)Ljava/lang/Integer;",
                            false
                        )
                }
            }
            FLOAD -> {
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Float",
                    "valueOf",
                    "(F)Ljava/lang/Float;",
                    false
                )
            }
            LLOAD -> {
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Long",
                    "valueOf",
                    "(J)Ljava/lang/Long;",
                    false
                )
            }
            DLOAD -> {
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Double",
                    "valueOf",
                    "(D)Ljava/lang/Double;",
                    false
                )
            }
        }
    }

    private fun visitMap(index: Int): Int {
        Log.d(TraceLogTransform.TAG, "annotations:" + annotations)
        mv.visitTypeInsn(NEW, "java/util/HashMap")
        mv.visitInsn(DUP)
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false)
        mv.visitVarInsn(ASTORE, index)

        annotations.forEach { key, value ->
            mv.visitVarInsn(ALOAD, index)
            mv.visitLdcInsn(key)
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "java/util/HashMap",
                "get",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                false
            )
            mv.visitTypeInsn(CHECKCAST, "java/util/HashMap")
            mv.visitVarInsn(ASTORE, index + 1)
            mv.visitVarInsn(ALOAD, index + 1)
            val l3 = Label()
            mv.visitJumpInsn(IFNONNULL, l3)
            mv.visitTypeInsn(NEW, "java/util/HashMap")
            mv.visitInsn(DUP)
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false)
            mv.visitVarInsn(ASTORE, index + 1)
            mv.visitLabel(l3)
            mv.visitFrame(
                F_APPEND,
                2,
                arrayOf<Any>("java/util/HashMap", "java/util/HashMap"),
                0,
                null
            )
            mv.visitVarInsn(ALOAD, index + 1)
            mv.visitLdcInsn(value?.keys.toString())
            mv.visitLdcInsn(value?.values.toString())
//            mv.visitLdcInsn("key")
//            mv.visitLdcInsn("value")
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "java/util/HashMap",
                "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                false
            )
            mv.visitInsn(POP)
            mv.visitVarInsn(ALOAD, index)
            mv.visitLdcInsn(key)
            mv.visitVarInsn(ALOAD, index + 1)
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "java/util/HashMap",
                "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                false
            )
            mv.visitInsn(POP)
        }
        return index
    }

    private fun visiteTraceInfo(isStatic: Boolean, traceName: String, objsIndex: Int, mapIndex: Int, traceInfoIndex: Int) {
        mv.visitTypeInsn(NEW, Contants.TRACEINFO_CLASS)
        mv.visitInsn(DUP)
        mv.visitLdcInsn(traceName)
        if (isStatic) {
            mv.visitInsn(ACONST_NULL);
        }else {
            mv.visitVarInsn(ALOAD, 0)
        }
        mv.visitVarInsn(ALOAD, objsIndex)
        mv.visitVarInsn(ALOAD, mapIndex)
        mv.visitMethodInsn(
            INVOKESPECIAL,
            Contants.TRACEINFO_CLASS,
            "<init>",
            "(Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;Ljava/util/HashMap;)V",
            false
        )
        mv.visitVarInsn(ASTORE, traceInfoIndex)
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
//        println("visitAnnotation descriptor:$descriptor, visible:$visible")
        return object : AnnotationVisitor(Opcodes.ASM5) {
            override fun visit(name: String, value: Any?) {
                super.visit(name, value)
                var item = annotations[descriptor] as? HashMap
                if (item == null) {
                    item = hashMapOf(name to value)
                    annotations[descriptor] = item
                } else {
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