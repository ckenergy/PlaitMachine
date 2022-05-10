package com.ckenergy.plaintmachine

import android.util.Log

/**
 * Created by chengkai on 2021/7/15.
 */
object TestIgnore {

    fun doSomeInIgnore(info: String) {
        Log.e("TestIgnore", "doSomeInIgnore info:$info")
        Thread.sleep(2000)
    }

}