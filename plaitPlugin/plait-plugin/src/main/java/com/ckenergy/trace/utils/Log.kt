package com.ckenergy.trace.utils

/**
 * Created by chengkai on 2021/7/15.
 */
object Log {

    @JvmStatic
    var printLog = false

    var filterLog = ""

    @JvmStatic
    fun d(tag: String, info: String) {
        if (printLog && (filterLog.isBlank() || info.contains(filterLog, true))) w(tag, info)
    }

    @JvmStatic
    fun w(tag: String, info: String) {
        println("$tag, $info")
    }

}