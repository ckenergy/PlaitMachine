package com.ckenergy.trace.extension;

import java.util.List;

/**
 * Created by chengkai on 2021/4/19.
 */
public class PlaitMethodList {
    public String plaitClass; //要织入的类名
    public String plaitMethod; //要织入的方法名
    public List<String> methodList;
    public List<String> blackMethodList;

    @Override
    public String toString() {
        return ",PlaitMethodListExtension{" +
                ",plaitClass='" + plaitClass +
                ",plaitMethod='" + plaitMethod +
                ", methodList=" + methodList +
                ", blackMethodList=" + blackMethodList +
                '}';
    }
}
