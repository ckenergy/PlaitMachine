package com.hellotalk.trace;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.hellotalk.trace.ignore.TestIgnore;
import com.hellotalk.tracelog.TestTrace;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("main", "onCreate");
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            List<String> ars = new ArrayList<>();
            ars.add("position 1");
            ars.add("position 2");
            ars.add("position 3");
            ars.add("position 4");
            doSome(5, "doSome", 2.0f, false, ars);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @TestTrace(2)
    private static void doSome(int type, String name, float f, boolean su, List<String> ars) {
        Log.e("main", "doSome");
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                doSome1();
//            }
//        }).start();

        TestIgnore.INSTANCE.doSomeInIgnore();
    }

//    private void doSome1() {
//        Log.e("main", "doSome1");
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }


    @Override
    public void onClick(View v) {

    }
}