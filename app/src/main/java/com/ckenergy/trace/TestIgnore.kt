package com.ckenergy.trace

import android.util.Log

/**
 * Created by chengkai on 2021/7/15.
 */
object TestIgnore {

    fun doSomeInIgnore() {
        Log.e("TestIgnore", "doSomeInIgnore")
        Thread.sleep(2000)
    }

}