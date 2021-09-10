package com.ckenergy.trace

import org.objectweb.asm.*
import org.objectweb.asm.commons.AdviceAdapter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class PlaitMethodVisitor(
    val className: String,
    methodVisitor: MethodVisitor,
    access: Int,
    name: String?,
    val descriptor: String?
) : AdviceAdapter(Contants.ASM_VERSION, methodVisitor, access, name, descriptor) {

    val annotations = HashMap<String, Map<String, Any?>?>()

    //方法开始,此处可在方法开始插入字节码
    override fun onMethodEnter() {
        super.onMethodEnter()
        var traceName = "$className.$name"
        Log.d(TraceLogTransform.TAG, "onMethodEnter name:$traceName")
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
        val traceInfoIndex = mapIndex + 1
        //构建traceinfo对象
        visiteTraceInfo(isStatic, traceName, objsIndex, mapIndex, traceInfoIndex)
        //注入方法
        mv.visitVarInsn(ALOAD, traceInfoIndex)
        mv.visitMethodInsn(
            INVOKESTATIC,
            Contants.TRACELOG_CLASS,
            "test", "(L${Contants.TRACEINFO_CLASS};)V",
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
        Log.d(TraceLogTransform.TAG, "annotations:$annotations")
        mv.visitTypeInsn(NEW, "java/util/HashMap")
        mv.visitInsn(DUP)
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false)
        mv.visitVarInsn(ASTORE, index)

        annotations.forEach { (key, value) ->
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
            value?.forEach { (t, u) ->
                mv.visitVarInsn(ALOAD, index + 1)
                mv.visitLdcInsn(t)
                visiteAnnotationValue(u, index + 2)
                mv.visitMethodInsn(
                    INVOKEVIRTUAL,
                    "java/util/HashMap",
                    "put",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                    false
                )
                mv.visitInsn(POP)
            }
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

    private fun visiteTraceInfo(
        isStatic: Boolean,
        traceName: String,
        objsIndex: Int,
        mapIndex: Int,
        traceInfoIndex: Int
    ) {
        mv.visitTypeInsn(NEW, Contants.TRACEINFO_CLASS)
        mv.visitInsn(DUP)
        mv.visitLdcInsn(traceName)
        if (isStatic) {
            mv.visitInsn(ACONST_NULL);
        } else {
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

    private fun visiteAnnotationValue(value: Any?, nextIndex: Int) {
        fun eachLoad(listIndex: Int, listValue: Any) {
            mv.visitVarInsn(ALOAD, nextIndex)
            mv.visitLdcInsn(listIndex)
            visiteType(listValue)
            mv.visitInsn(AASTORE)
        }
        when (value) {
            is ArrayList<*> -> {
                newObjArray(nextIndex, value.size)
                value.forEachIndexed { index1, any ->
                    eachLoad(index1, any)
                }
                mv.visitVarInsn(ALOAD, nextIndex)
            }
            is ByteArray -> {
                newObjArray(nextIndex, value.size)
                value.forEachIndexed { index1, i ->
                    eachLoad(index1, i)
                }
                mv.visitVarInsn(ALOAD, nextIndex)
            }
            is IntArray -> {
                newObjArray(nextIndex, value.size)
                value.forEachIndexed { index1, i ->
                    eachLoad(index1, i)
                }
                mv.visitVarInsn(ALOAD, nextIndex)
            }
            is LongArray -> {
                newObjArray(nextIndex, value.size)
                value.forEachIndexed { index1, i ->
                    eachLoad(index1, i)
                }
                mv.visitVarInsn(ALOAD, nextIndex)
            }
            is BooleanArray -> {
                newObjArray(nextIndex, value.size)
                value.forEachIndexed { index1, i ->
                    eachLoad(index1, i)
                }
                mv.visitVarInsn(ALOAD, nextIndex)
            }
            is FloatArray -> {
                newObjArray(nextIndex, value.size)
                value.forEachIndexed { index1, i ->
                    eachLoad(index1, i)
                }
                mv.visitVarInsn(ALOAD, nextIndex)
            }
            is DoubleArray -> {
                newObjArray(nextIndex, value.size)
                value.forEachIndexed { index1, i ->
                    eachLoad(index1, i)
                }
                mv.visitVarInsn(ALOAD, nextIndex)
            }
            is CharArray -> {
                newObjArray(nextIndex, value.size)
                value.forEachIndexed { index1, i ->
                    eachLoad(index1, i)
                }
                mv.visitVarInsn(ALOAD, nextIndex)
            }
            is ShortArray -> {
                newObjArray(nextIndex, value.size)
                value.forEachIndexed { index1, i ->
                    eachLoad(index1, i)
                }
                mv.visitVarInsn(ALOAD, nextIndex)
            }
            else -> {
                visiteType(value)
            }
        }
    }

    private fun newObjArray(index: Int, size: Int) {
        mv.visitLdcInsn(size)
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object")
        mv.visitVarInsn(ASTORE, index)
    }

    private fun visiteType(value: Any?) {
        when (value) {
            is Int -> {
                mv.visitLdcInsn(value)
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Integer",
                    "valueOf",
                    "(I)Ljava/lang/Integer;",
                    false
                );
            }
            is Short -> {
                mv.visitLdcInsn(value)
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Short",
                    "valueOf",
                    "(S)Ljava/lang/Short;",
                    false
                );
            }
            is Boolean -> {
                mv.visitLdcInsn(value)
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Boolean",
                    "valueOf",
                    "(Z)Ljava/lang/Boolean;",
                    false
                );
            }
            is Char -> {
                mv.visitLdcInsn(value)
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Character",
                    "valueOf",
                    "(C)Ljava/lang/Character;",
                    false
                );
            }
            is Byte -> {
                mv.visitLdcInsn(value)
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Byte",
                    "valueOf",
                    "(B)Ljava/lang/Byte;",
                    false
                );
            }
            is Float -> {
                mv.visitLdcInsn(value)
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Float",
                    "valueOf",
                    "(F)Ljava/lang/Float;",
                    false
                );

            }
            is Double -> {
                mv.visitLdcInsn(value)
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Double",
                    "valueOf",
                    "(D)Ljava/lang/Double;",
                    false
                );

            }
            is Long -> {
                mv.visitLdcInsn(value)
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Long",
                    "valueOf",
                    "(J)Ljava/lang/Long;",
                    false
                );
            }
            is String -> {
                mv.visitLdcInsn(value)
            }
            is AnnotionWrap -> {
                val type = Type.getType(value.desc)
                Log.d(
                    TraceLogTransform.TAG,
                    "visiteAnnotationValue class:${type.className}, descriptor:${value.desc}"
                )
                mv.visitFieldInsn(GETSTATIC, type.className.replace(".", "/"), value.value, value.desc.replace(".", "/"))
            }
            else ->{
                Log.d(
                    TraceLogTransform.TAG,
                    "visiteAnnotationValue else ${value?.javaClass}"
                )
                mv.visitLdcInsn(value?.toString())
            }
        }
    }

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
//        println("visitAnnotation descriptor:$descriptor, visible:$visible")
        val item = annotations[descriptor] as? HashMap ?: HashMap()
        annotations[descriptor] = item
        return object : AnnotationVisitor(Contants.ASM_VERSION) {
            override fun visit(name: String?, value: Any?) {
                super.visit(name, value)
                Log.d(TraceLogTransform.TAG, "visit name:$name, value:$value, class:${value is IntArray}")
                if (name != null)
                    item[name] = value
            }

            override fun visitEnum(name: String?, descriptor1: String, value: String?) {
                super.visitEnum(name, descriptor1, value)
                Log.d(
                    TraceLogTransform.TAG,
                    "visitEnum name:$name, descriptor:$descriptor1, value:$value"
                )
                if (name != null) item[name] = value
            }

            override fun visitAnnotation(name: String?, descriptor: String?): AnnotationVisitor {
                Log.d(TraceLogTransform.TAG, "visitAnnotation name:$name, descriptor:$descriptor")
                return super.visitAnnotation(name, descriptor)
            }

            override fun visitArray(name: String?): AnnotationVisitor {
                Log.d(TraceLogTransform.TAG, "visitArray name:$name")
                if (name != null) {
                    val list = ArrayList<Any>()
                    item[name] = list
                    return object : AnnotationVisitor(Contants.ASM_VERSION) {
                        override fun visit(name: String?, value: Any?) {
                            super.visit(name, value)
                            if (value != null) list.add(value)
                            Log.d(TraceLogTransform.TAG, "visitArray name:$name, value:$value")
                        }

                        override fun visitEnum(name: String?, descriptor: String?, value: String?) {
                            super.visitEnum(name, descriptor, value)
                            if (value != null && descriptor != null) {
                                val annotionWrap = AnnotionWrap(descriptor, value)
                                list.add(annotionWrap)
                            }
                            Log.d(TraceLogTransform.TAG, "visitArrayEnmum name:$name, value:$value, descriptor:$descriptor， listClass:${list is ArrayList<*>}")
                        }
                    }
                } else {
                    return super.visitArray(name)
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