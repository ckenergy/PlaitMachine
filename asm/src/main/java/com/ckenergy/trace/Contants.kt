package com.ckenergy.trace

import org.objectweb.asm.Opcodes

/**
 * Created by chengkai on 2021/4/26.
 */
object Contants {

    const val ALL = "all*"
    const val TRACE_INFO_CLASS = "com/ckenergy/tracelog/TraceInfo"
    val UN_TRACE_CLASS = arrayOf("R.class", "R$", "Manifest", "BuildConfig")

    const val ASM_VERSION = Opcodes.ASM7

}