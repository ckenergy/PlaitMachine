package com.ckenergy.trace.extension;

import java.util.List;

/**
 * Created by chengkai on 2021/4/19.
 */
public class TraceMethodListExtension {
    public String name; //要织入的类名
    public List<String> methodList;

    public TraceMethodListExtension(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "PlaitMethodListExtension{" +
                "name='" + name + '\'' +
                ", methodList=" + methodList +
                '}';
    }
}
