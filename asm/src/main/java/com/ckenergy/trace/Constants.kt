package com.ckenergy.trace

import org.objectweb.asm.Opcodes

/**
 * Created by chengkai on 2021/4/26.
 */
object Constants {

    const val ALL = "all*"
    const val TRACE_INFO_CLASS = "com/ckenergy/plaintmachine/PlaitContext"
    val UN_PLAINT_CLASS = arrayOf("R.class", "R$", "Manifest", "BuildConfig")
    val DEFAULT_BLACK_PACKAGE = arrayOf(TRACE_INFO_CLASS)

    const val ASM_VERSION = Opcodes.ASM7

}