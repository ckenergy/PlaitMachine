package com.ckenergy.trace;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Trace;
import android.util.Log;

import com.ckenergy.tracelog.TraceInfo;

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
        for(Map.Entry<String, HashMap<String, Object>> entry : traceInfo.getAnnotations().entrySet()) {
            for (Map.Entry<String, Object> entry1 : entry.getValue().entrySet()) {
                if (entry1.getValue() instanceof Object[]) {
                    Object[] list = (Object[]) entry1.getValue();
                    Log.e(TAG, entry.getKey()+", key:"+entry1.getKey()+", value"+ Arrays.toString(list));
                }
            }
        }
    }
}
