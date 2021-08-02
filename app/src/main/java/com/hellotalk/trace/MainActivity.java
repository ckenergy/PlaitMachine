package com.hellotalk.trace;

import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.os.Trace;
import android.util.Log;
import android.view.View;

import com.hellotalk.trace.ignore.TestIgnore;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("main", "onCreate");
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            doSome();
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void doSome() {
        Log.e("main", "doSome");
        new Thread(new Runnable() {
            @Override
            public void run() {
                doSome1();
            }
        }).start();

        TestIgnore.INSTANCE.doSomeInIgnore();
    }

    private void doSome1() {
        Log.e("main", "doSome1");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View v) {

    }
}