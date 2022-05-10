package com.ckenergy.trace

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.ckenergy.plaintmachine.TestIgnore.doSomeInIgnore

class MainActivity : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("main", "onCreate")
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ars: MutableList<String> = ArrayList()
            ars.add("position 1")
            ars.add("position 2")
            ars.add("position 3")
            ars.add("position 4")
            doSome(5, "doSome", 2.0f, false, ars)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @TestTrace(
        value = ["2", "3"],
        type = [TraceType.NORMAL],
        intRes = [1, 2],
        boolRes = [false, true],
        shortRes = [3, 4],
        longRes = [5, 6],
        doubleRes = [7.0, 8.0],
        floatRes = [9.0f, 10.0f],
        byteRes = [10, 11],
        chatRes = ['a', 'b'],
        byteType = 1,
        shortType = 2,
        longType = 3,
        charType = 'c',
        booleanType = true,
        floatType = 5.5f,
        doubleType = 6.6
    )
    private fun doSome(type: Int, name: String, f: Float, su: Boolean, ars: List<String>) {
        Log.e("main", "doSome")
        Thread { doSome1() }.start()
        doSomeInIgnore("main")
    }

    @NoTrace
    private fun doSome1() {
        Log.e("main", "doSome1")
        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onClick(v: View) {}
}