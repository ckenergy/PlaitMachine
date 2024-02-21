package com.ckenergy.trace

import org.objectweb.asm.Opcodes

/**
 * Created by chengkai on 2021/4/26.
 */
object Constants {

    const val TAG = "plait"

    const val ALL = "all*"
    const val PLAINT_CONTEXT_CLASS = "com/ckenergy/plaintmachine/PlaitContext"

    /**
     * 默认过滤的类
     */
    val UN_PLAINT_CLASS = arrayOf("R.class", "R$", "Manifest", "BuildConfig", PLAINT_CONTEXT_CLASS,
        //部分情况toString会造成循环调用，所以先把这几个方法过滤了
        "androidx/arch/core/internal/SafeIterableMap",
        "androidx/collection/SimpleArrayMap",
        "androidx/collection/ArraySet")

    /**
     * 默认过滤的方法
     */
    val DEFAULT_BLACK_METHOD = arrayOf("<clinit>", "<init>", "toString", "hashCode")

    /**
     * 默认过滤的包名
     */
    val DEFAULT_BLACK_PACKAGE = arrayOf("io/leangen/geantyref/*", "kotlin/*")

    const val ASM_VERSION = Opcodes.ASM7

}