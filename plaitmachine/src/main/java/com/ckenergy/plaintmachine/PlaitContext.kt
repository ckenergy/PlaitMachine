package com.ckenergy.plaintmachine;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by chengkai on 2021/7/28.
 */

public class PlaitContext {

    private String methodName;

    private Object current;

    private Object[] args;

    private HashMap<String, HashMap<String, Object>> annotations;

    public PlaitContext(String methodName, Object current, Object[] args, HashMap<String, HashMap<String, Object>> annotations) {
        this.methodName = methodName;
        this.current = current;
        this.args = args;
        this.annotations = annotations;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public HashMap<String, HashMap<String, Object>> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(HashMap<String, HashMap<String, Object>> annotations) {
        this.annotations = annotations;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Object getCurrent() {
        return current;
    }

    public void setCurrent(Object current) {
        this.current = current;
    }

    @Override
    public String toString() {
        return "TraceInfo{" +
                "methodName='" + methodName + '\'' +
                ", current=" + current +
                ", args=" + Arrays.toString(args) +
                ", annotations=" + annotations +
                '}';
    }
}
