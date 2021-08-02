package com.hellotalk.tracelog;

import java.util.Map;

/**
 * Created by chengkai on 2021/7/28.
 */

public class TraceInfo {


    private Object current;

    private Object[] args;

    private Map<String, Map<String, Object>> annotations;

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Map<String, Map<String, Object>> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Map<String, Map<String, Object>> annotations) {
        this.annotations = annotations;
    }

    public TraceInfo(Object current, Object[] args, Map<String, Map<String, Object>> annotations) {
        this.current = current;
        this.args = args;
        this.annotations = annotations;
    }
}
