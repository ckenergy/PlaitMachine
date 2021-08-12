package com.hellotalk.tracelog;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Trace;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class TraceTag {

    private static final String TAG = "TraceTag";

    /**
     * hook method when it's called in.
     *
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void i(String name) {
        Trace.beginSection(name);
    }

    /**
     * hook method when it's called out.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void o() {
        Trace.endSection();
    }

    public static void test(Object[] parater) {
        Log.e(TAG, "Test:"+ Arrays.toString(parater));
    }

    public static void test(HashMap<String, Map<String, Object>> maps) {
        Log.e(TAG, "Test:"+ maps);
    }

    public static void test(TraceInfo traceInfo) {
        Log.e(TAG, "Test:"+ traceInfo);
    }
}
