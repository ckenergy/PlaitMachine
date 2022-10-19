package com.ckenergy.trace

/**
 * Created by chengkai on 2021/7/15.
 */
object Log {

    @JvmStatic
    var printLog = false

    @JvmStatic
    fun d(tag: String, info: String) {
        if (printLog) println("$tag, $info")
    }

}