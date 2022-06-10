package com.ckenergy.trace;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.ckenergy.lib.NoTrace;
import com.ckenergy.lib.TestIgnore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.leangen.geantyref.AnnotationFormatException;
import io.leangen.geantyref.TypeFactory;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("main", "onCreate");
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            List<String> list = new ArrayList<>();
            list.add("position 1");
            list.add("position 2");
            list.add("position 3");
            list.add("position 4");
            doSome(1L, 3.0, 5, "doSome", 2f, false, list, "ss", 1);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @TestTrace(value = {"2", "3"}, type = {TraceType.NORMAL},
            intRes = {1,2},
            boolRes = {false, true},
            shortRes = {3,4},
            longRes = {5,6},
            doubleRes = {7.0,8.0},
            floatRes = {9.0f,10.0f},
            byteRes = {10,11},
            chatRes = {'a','b'},
        byteType = 1, shortType = 2, longType = 3, charType = 'c', booleanType = true, floatType = 5.5f, doubleType = 6.6)
    private void doSome(long l, double d, int type, String name, float f, boolean su, List<String> list, Object... args) {
        Log.e("main", "doSome");
        new Thread(new Runnable() {
            @Override
            public void run() {
                doSome1();
            }
        }).start();

        TestIgnore.INSTANCE.doSomeInIgnore("info");
    }

    @NoTrace(1)
    private void doSome1() {
        Log.e("main", "doSome1");
//        Map<String, Object> map = new HashMap<>();
//        Integer value = 1;
//        map.put("value", value);
//        try {
//            NoTrace annotation = TypeFactory.annotation(NoTrace.class, map);
//            Log.e("main", "doSome1 value:"+annotation.value());
//        } catch (AnnotationFormatException e) {
//            e.printStackTrace();
//        }

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