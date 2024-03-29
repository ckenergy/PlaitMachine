package com.ckenergy.trace

import com.ckenergy.trace.extension.PlaitMethodList
import com.ckenergy.trace.utils.Log
import org.objectweb.asm.*
import org.objectweb.asm.commons.AdviceAdapter

private const val TAG = "===PlaitMethodVisitor==="
class PlaitMethodVisitor @JvmOverloads constructor(
    api: Int,
    methodVisitor: MethodVisitor,
    access: Int,
    name: String?,
    private val className: String,
    private val descriptor: String?,
    private val methodList: List<PlaitMethodList>?,
    private val annotationMap: Map<String, List<PlaitMethodList>?>? = null,
    private val blackAnnotationMap: Map<String, List<PlaitMethodList>?>? = null,
) : AdviceAdapter(api, methodVisitor, access, name, descriptor) {

    private val annotations by lazy{
        HashMap<String, Map<String, Any?>?>()
    }

    private var plaitMethodList: List<PlaitMethodList>? = null

    private var contextIndex = -1

    //方法开始,此处可在方法开始插入字节码
    override fun onMethodEnter() {
        super.onMethodEnter()
        val methodName = "$className.$name"
        log( "onMethodEnter name:$methodName")

        filterPlaintList()

        if (plaitMethodList.isNullOrEmpty()) {
            log( "name:$className.$name, method is empty")
            return
        }

        //获取参数,然后构建参数数值
        val isStatic = AsmUtil.isStatic(access)
        val types = Type.getArgumentTypes(descriptor ?: "")
        val size = types.size

        val argsArrayIndex = newArgsArray(size)
        var argIndex = if (isStatic) 0 else 1
        types.forEachIndexed { index, type ->
            mv.visitVarInsn(ALOAD, argsArrayIndex)
            val opcode = getOpcode(type)
            mv.visitLdcInsn(index)
            mv.visitVarInsn(opcode, argIndex)
            visitMethod(type, opcode)
            mv.visitInsn(AASTORE)
            if (opcode == LLOAD || opcode == DLOAD) {
                argIndex += 2
            }else {
                argIndex ++
            }
        }

        val mapIndex = visitAnnotationListMap()
        //构建context对象
        contextIndex = visitPlaitContext(isStatic, methodName, argsArrayIndex, mapIndex)

        //注入方法
        plaitMethodList?.forEach {
            if (!it.isMethodExit && contextIndex >= 0) {
                log( "name:$className.$name, invoke method: ${it.plaitClass}.${it.plaitMethod}")
                mv.visitVarInsn(ALOAD, contextIndex)
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    it.plaitClass,
                    it.plaitMethod, "(L${Constants.PLAINT_CONTEXT_CLASS};)V",
                    false
                )
            }
        }
    }

    /**
     * 过滤要插入的方法
     */
    private fun filterPlaintList() {
        val temMethodList = arrayListOf<PlaitMethodList>()
        if (!annotationMap.isNullOrEmpty()) {
            annotations.forEach {
                annotationMap[it.key]?.apply {
                    temMethodList.addAll(this)
                }
            }
        }

        if (!methodList.isNullOrEmpty())
            temMethodList.addAll(methodList)

        if (temMethodList.isEmpty()) return

        log( "name:$className.$name, temMethodList:$temMethodList")

        val blackMethodList = arrayListOf<PlaitMethodList>()
        if (!blackAnnotationMap.isNullOrEmpty()) {
            annotations.forEach {
                blackAnnotationMap[it.key]?.apply {
                    blackMethodList.addAll(this)
                }
            }
        }
        log( "name:$className.$name, blackMethodList:$blackMethodList")

        //todo 优化算法
        plaitMethodList = if (temMethodList.isNotEmpty() && blackMethodList.isNotEmpty()) {
            temMethodList.filter { it1->//去除配置注解黑名单里的方法
                blackMethodList.find { it.plaitClass == it1.plaitClass && it.plaitMethod == it1.plaitMethod } == null
            }
        }else temMethodList
        log( "name:$className.$name, plaitMethodList:$plaitMethodList")
    }

    private fun newArgsArray(size: Int): Int {
        val index = newLocal(Type.getType(Array<Any>::class.java))
        mv.visitLdcInsn(size)
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object")
        mv.visitVarInsn(ASTORE, index)
        return index
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

    // -------------
    private fun putOpcode(type: Type): Int {
        val t =
            when (type.sort) {
                Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> ISTORE
                Type.FLOAT -> FSTORE
                Type.LONG -> LSTORE
                Type.DOUBLE -> DSTORE
                else -> ASTORE
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

    private fun visitAnnotationListMap(): Int {
        log( "annotations:$annotations")
        val index = newLocal(Type.getType(HashMap::class.java))
        mv.visitTypeInsn(NEW, "java/util/HashMap")
        mv.visitInsn(DUP)
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false)
        mv.visitVarInsn(ASTORE, index)

        val itemIndex = newLocal(Type.getType(HashMap::class.java))
        val arrayIndex = newLocal(Type.getType(Array<Any>::class.java))
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
            mv.visitVarInsn(ASTORE, itemIndex)
            mv.visitVarInsn(ALOAD, itemIndex)
            val l3 = Label()
            mv.visitJumpInsn(IFNONNULL, l3)
            mv.visitTypeInsn(NEW, "java/util/HashMap")
            mv.visitInsn(DUP)
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false)
            mv.visitVarInsn(ASTORE, itemIndex)
            mv.visitLabel(l3)
            value?.forEach { (t, u) ->
                mv.visitVarInsn(ALOAD, itemIndex)
                mv.visitLdcInsn(t)
                visitAnnotationValue(u, arrayIndex)
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
            mv.visitVarInsn(ALOAD, itemIndex)
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

    private fun visiteAnnotationMap(value: Map<*, *>, itemIndex: Int) {
        val arrayIndex = newLocal(Type.getType(Array<Any>::class.java))
        mv.visitTypeInsn(NEW, "java/util/HashMap")
        mv.visitInsn(DUP)
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false)
        mv.visitVarInsn(ASTORE, itemIndex)
        value.forEach { (t, u) ->
            mv.visitVarInsn(ALOAD, itemIndex)
            mv.visitLdcInsn(t.toString())
            visitAnnotationValue(u, arrayIndex)
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "java/util/HashMap",
                "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                false
            )
            mv.visitInsn(POP)
        }
    }

    private fun visitePair(value: Pair<*, Map<*,*>>, itemIndex: Int) {
        mv.visitTypeInsn(NEW, "kotlin/Pair")
        mv.visitInsn(DUP)
        val mapIndex = newLocal(Type.getType(HashMap::class.java))
        visiteAnnotationMap(value.second, mapIndex)
        mv.visitLdcInsn(value.first.toString())
        mv.visitVarInsn(ALOAD, mapIndex)
        mv.visitMethodInsn(INVOKESPECIAL, "kotlin/Pair", "<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V", false)

        mv.visitVarInsn(ASTORE, itemIndex)

    }

    private fun visitPlaitContext(
        isStatic: Boolean,
        methodName: String,
        objsIndex: Int,
        mapIndex: Int,
    ): Int {
        mv.visitTypeInsn(NEW, Constants.PLAINT_CONTEXT_CLASS)
        mv.visitInsn(DUP)
        mv.visitLdcInsn(methodName)
        if (isStatic) {
            mv.visitInsn(ACONST_NULL);
        } else {
            mv.visitVarInsn(ALOAD, 0)
        }
        mv.visitVarInsn(ALOAD, objsIndex)
        mv.visitVarInsn(ALOAD, mapIndex)
        mv.visitMethodInsn(
            INVOKESPECIAL,
            Constants.PLAINT_CONTEXT_CLASS,
            "<init>",
            "(Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;Ljava/util/HashMap;)V",
            false
        )
        val traceInfoIndex = newLocal(Type.getType("L"+Constants.PLAINT_CONTEXT_CLASS))
        mv.visitVarInsn(ASTORE, traceInfoIndex)
        return traceInfoIndex
    }

    override fun onMethodExit(opcode: Int) {
        super.onMethodExit(opcode)
        log("onMethodExit name:$name")
        //注入方法
        plaitMethodList?.forEach {
            if (it.isMethodExit && contextIndex >= 0) {
                log( "name:$className.$name, invoke method: ${it.plaitClass}.${it.plaitMethod}")
                mv.visitVarInsn(ALOAD, contextIndex)
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    it.plaitClass,
                    it.plaitMethod, "(L${Constants.PLAINT_CONTEXT_CLASS};)V",
                    false
                )
            }
        }
    }

    private fun visitAnnotationValue(value: Any?, nextIndex: Int) {
        fun eachLoad(listIndex: Int, listValue: Any) {
            mv.visitVarInsn(ALOAD, nextIndex)
            mv.visitLdcInsn(listIndex)
            visiteIntType(listValue)
        }
        when (value) {
            is ArrayList<*> -> {
                if (value.isEmpty()) {
                    log("visitAnnotationValue ArrayList is empty")
                    mv.visitInsn(ACONST_NULL)
                    return
                }
                val item = value[0]
                var klass = item.javaClass.name.replace(".","/")
                if (item is AnnotionWrap) {
                    val type = Type.getType(item.desc)
                    klass = type.className.replace(".","/")
                }
                newArray(nextIndex, value.size, klass)
                value.forEachIndexed { index1, any ->
                    eachLoad(index1, any)
                }
                mv.visitVarInsn(ALOAD, nextIndex)
            }
            is ByteArray -> {
                newIntArray(nextIndex, value.size, T_BYTE)
                value.forEachIndexed { index1, i ->
                    eachLoad(index1, i)
                }
                mv.visitVarInsn(ALOAD, nextIndex)
            }
            is IntArray -> {
                newIntArray(nextIndex, value.size, T_INT)
                value.forEachIndexed { index1, i ->
                    eachLoad(index1, i)
                }
                mv.visitVarInsn(ALOAD, nextIndex)
            }
            is LongArray -> {
                newIntArray(nextIndex, value.size, T_LONG)
                value.forEachIndexed { index1, i ->
                    eachLoad(index1, i)
                }
                mv.visitVarInsn(ALOAD, nextIndex)
            }
            is BooleanArray -> {
                newIntArray(nextIndex, value.size, T_BOOLEAN)
                value.forEachIndexed { index1, i ->
                    eachLoad(index1, i)
                }
                mv.visitVarInsn(ALOAD, nextIndex)
            }
            is FloatArray -> {
                newIntArray(nextIndex, value.size, T_FLOAT)
                value.forEachIndexed { index1, i ->
                    eachLoad(index1, i)
                }
                mv.visitVarInsn(ALOAD, nextIndex)
            }
            is DoubleArray -> {
                newIntArray(nextIndex, value.size, T_DOUBLE)
                value.forEachIndexed { index1, i ->
                    eachLoad(index1, i)
                }
                mv.visitVarInsn(ALOAD, nextIndex)
            }
            is CharArray -> {
                newIntArray(nextIndex, value.size, T_CHAR)
                value.forEachIndexed { index1, i ->
                    eachLoad(index1, i)
                }
                mv.visitVarInsn(ALOAD, nextIndex)
            }
            is ShortArray -> {
                newIntArray(nextIndex, value.size, T_SHORT)
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

    private fun newArray(index: Int, size: Int, type: String) {
        mv.visitLdcInsn(size)
        mv.visitTypeInsn(ANEWARRAY, type)
        mv.visitVarInsn(ASTORE, index)
    }

    private fun newIntArray(index: Int, size: Int, type: Int) {
        mv.visitLdcInsn(size)
        mv.visitIntInsn(NEWARRAY, type)
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
                )
            }
            is Short -> {
                mv.visitLdcInsn(value)
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Short",
                    "valueOf",
                    "(S)Ljava/lang/Short;",
                    false
                )
            }
            is Boolean -> {
                mv.visitLdcInsn(value)
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Boolean",
                    "valueOf",
                    "(Z)Ljava/lang/Boolean;",
                    false
                )
            }
            is Char -> {
                mv.visitLdcInsn(value)
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Character",
                    "valueOf",
                    "(C)Ljava/lang/Character;",
                    false
                )
            }
            is Byte -> {
                mv.visitLdcInsn(value)
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Byte",
                    "valueOf",
                    "(B)Ljava/lang/Byte;",
                    false
                )
            }
            is Float -> {
                mv.visitLdcInsn(value)
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Float",
                    "valueOf",
                    "(F)Ljava/lang/Float;",
                    false
                )

            }
            is Double -> {
                mv.visitLdcInsn(value)
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Double",
                    "valueOf",
                    "(D)Ljava/lang/Double;",
                    false
                )

            }
            is Long -> {
                mv.visitLdcInsn(value)
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Long",
                    "valueOf",
                    "(J)Ljava/lang/Long;",
                    false
                )
            }
            else ->{
                visiteObjValue(value)
            }
        }
    }

    private fun visiteIntType(value: Any?) {
        when (value) {
            is Int -> {
                mv.visitLdcInsn(value)
                mv.visitInsn(IASTORE)
            }
            is Short -> {
                mv.visitLdcInsn(value)
                mv.visitInsn(SASTORE)
            }
            is Boolean -> {
                mv.visitLdcInsn(value)
                mv.visitInsn(BASTORE)
            }
            is Char -> {
                mv.visitLdcInsn(value)
                mv.visitInsn(CASTORE)
            }
            is Byte -> {
                mv.visitLdcInsn(value)
                mv.visitInsn(BASTORE)
            }
            is Float -> {
                mv.visitLdcInsn(value)
                mv.visitInsn(FASTORE)
            }
            is Double -> {
                mv.visitLdcInsn(value)
                mv.visitInsn(DASTORE)
            }
            is Long -> {
                mv.visitLdcInsn(value)
                mv.visitInsn(LASTORE)
            }
            else -> {
                visiteObjValue(value)
                mv.visitInsn(AASTORE)
            }
        }
    }

    private fun visiteObjValue(value: Any?) {
        when(value) {
            is String -> {
                mv.visitLdcInsn(value)
            }
            is AnnotionWrap -> {
                val type = Type.getType(value.desc)
                log(
                    "visiteAnnotationValue AnnotionWrap :${type.className}, descriptor:${value.desc}"
                )
                mv.visitFieldInsn(GETSTATIC, type.className.replace(".", "/"),
                    value.value, value.desc.replace(".", "/"))
            }
            is Pair<*,*> -> {
                if (value.second !is Map<*,*> || (value.second as Map<*, *>).isEmpty()) {
                    log("visitAnnotationValue Pair second is empty")
                    mv.visitInsn(ACONST_NULL)
                    return
                }
                val itemIndex = newLocal(Type.getType(HashMap::class.java))
                visitePair(value as Pair<*, Map<*, *>>, itemIndex)
                mv.visitVarInsn(ALOAD, itemIndex)
            }
            is HashMap<*, *> -> {
                if (value.isEmpty()) {
                    log("visitAnnotationValue HashMap is empty")
                    mv.visitInsn(ACONST_NULL)
                    return
                }
                val itemIndex = newLocal(Type.getType(HashMap::class.java))
                visiteAnnotationMap(value, itemIndex)
                mv.visitVarInsn(ALOAD, itemIndex)
            }
            is Class<*> -> {
                log("visiteAnnotationValue Class $value")
                mv.visitLdcInsn(Type.getType(value))
            }
            is Type -> {
                log("visiteAnnotationValue type $value")
                mv.visitLdcInsn(value)
            }
            else ->{
                log("visiteAnnotationValue else ${value?.javaClass}")
                mv.visitLdcInsn(value?.toString())
            }
        }
    }

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
        log( "visitAnnotation descriptor:$descriptor, visible:$visible")
        val key = Type.getType(descriptor).className
        val item = annotations[key] as? HashMap ?: HashMap()
        annotations[key] = item
        val annotationVisitor = super.visitAnnotation(descriptor, visible)
        return PlaintAnnotationVisitor(api, annotationVisitor, item)
    }

    private fun log(info: String) {
            Log.d(TAG, info)
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