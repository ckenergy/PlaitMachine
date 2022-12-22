package com.ckenergy.trace;

import android.os.Trace;
import android.util.Log;

import com.ckenergy.plaintmachine.PlaitContext;

public class TraceTag {

    private static final String TAG = "TraceTag";

    public static void start(PlaitContext plaitContext) {
//        Log.e(TAG, "Test1:"+ plaitContext);
        Trace.beginSection(plaitContext.getClass().getName()+"."+plaitContext.getMethodName());
    }

    public static void end(PlaitContext plaitContext) {
//        Log.e(TAG, "Test "+plaitContext);
        Trace.endSection();
    }
}
