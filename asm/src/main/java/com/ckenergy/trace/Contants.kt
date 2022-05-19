package com.ckenergy.trace

import org.objectweb.asm.Opcodes

/**
 * Created by chengkai on 2021/4/26.
 */
object Contants {

    const val ALL = "all*"
    const val TRACE_INFO_CLASS = "com/ckenergy/plaintmachine/PlaitContext"
    val UN_TRACE_CLASS = arrayOf("R.class", "R$", "Manifest", "BuildConfig")
    val DEFAULT_BLACK_PACKAGE = arrayOf("android/*")

    const val ASM_VERSION = Opcodes.ASM5

}