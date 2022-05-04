package com.ckenergy.trace;

import android.util.Log;

import com.ckenergy.tracelog.TraceInfo;


public class TraceTag {

    private static final String TAG = "TraceTag";

    public static void test1(TraceInfo traceInfo) {
        Log.e(TAG, "Test1:"+ traceInfo);
    }


    public static void test(TraceInfo traceInfo) {
        Log.e(TAG, "Test:"+ traceInfo);
    }
}
