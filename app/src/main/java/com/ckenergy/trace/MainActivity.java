package com.ckenergy.trace;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.ckenergy.lib.TestIgnore;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final TestBean TEST_BEAN = new TestBean(1, "s");

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
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long start = System.currentTimeMillis();
                doSome1();
                Log.e("main", "doSome time:" + (System.currentTimeMillis() - start));
            }
        });
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @TestTrace(value = {"2", "3"}, type = {TraceType.NORMAL},
            intRes = {1, 2},
            boolRes = {false, true},
            shortRes = {3, 4},
            longRes = {5, 6},
            doubleRes = {7.0, 8.0},
            floatRes = {9.0f, 10.0f},
            byteRes = {10, 11},
            chatRes = {'a', 'b'},
            byteType = 1, shortType = 2, longType = 3, charType = 'c', booleanType = true, floatType = 5.5f, doubleType = 6.6,
            testType = TestBean.class
    )
    private void doSome(long l, double d, int type, String name, float f, boolean su, List<String> list, Object... args) {
        Log.e("main", "doSome");
        TestIgnore.INSTANCE.doSomeInIgnore("info");
    }

    //    @TestList({@TestTrace(value = {"2", "3"}, type = {TraceType.NORMAL},
//            intRes = {1,2},
//            boolRes = {false, true},
//            shortRes = {3,4},
//            longRes = {5,6},
//            doubleRes = {7.0,8.0},
//            floatRes = {9.0f,10.0f},
//            byteRes = {10,11},
//            chatRes = {'a','b'},
//            byteType = 1, shortType = 2, longType = 3, charType = 'c', booleanType = true, floatType = 5.5f, doubleType = 6.6),
//
//            @TestTrace(value = {"4", "5"}, type = {TraceType.NORMAL},
//            intRes = {1,2},
//            boolRes = {false, true},
//            shortRes = {3,4},
//            longRes = {5,6},
//            doubleRes = {7.0,8.0},
//            floatRes = {9.0f,10.0f},
//            byteRes = {10,11},
//            chatRes = {'a','b'},
//            byteType = 1, shortType = 2, longType = 3, charType = 'c', booleanType = true, floatType = 5.5f, doubleType = 6.6)})
    @TestTrace(
            value = {"2", "3"}, type = {TraceType.NORMAL},
            intRes = {1, 2},
            boolRes = {false, true},
            shortRes = {3, 4},
            longRes = {5, 6},
            doubleRes = {7.0, 8.0},
            floatRes = {9.0f, 10.0f},
            byteRes = {10, 11},
            chatRes = {'a', 'b'},
            byteType = 1, shortType = 2, longType = 3, charType = 'c', booleanType = true, floatType = 5.5f, doubleType = 6.6,
            testType = TestBean.class
    )
    private void doSome1() {
        Log.e("main", "doSome1");

    }

    @Override
    public void onClick(View v) {

    }
}