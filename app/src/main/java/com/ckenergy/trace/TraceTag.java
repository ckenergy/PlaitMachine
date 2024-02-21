package com.ckenergy.trace;

import android.util.Log;

import com.ckenergy.plaintmachine.PlaitContext;

public class TraceTag {

    private static final String TAG = "TraceTag";

    public static void test(PlaitContext plaitContext) {
        Log.e(TAG, "Test "+plaitContext);
    }
}
