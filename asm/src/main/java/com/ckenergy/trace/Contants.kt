package com.ckenergy.trace

import org.objectweb.asm.Opcodes

/**
 * Created by chengkai on 2021/4/26.
 */
object Contants {

    const val ALL = "all*"
    const val TRACELOG_CLASS = "com/hellotalk/tracelog/TraceTag"
    const val TRACEINFO_CLASS = "com/hellotalk/tracelog/TraceInfo"
    val UN_TRACE_CLASS = arrayOf("R.class", "R$", "Manifest", "BuildConfig")

    const val DEFAULT_BLACK_TRACE = ("[package]\n"
            + "-keepclass $TRACELOG_CLASS\n"
            + "-keepclass $TRACEINFO_CLASS\n")

    const val ASM_VERSION = Opcodes.ASM7

}