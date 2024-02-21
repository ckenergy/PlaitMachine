package com.ckenergy.trace.extension;

import org.gradle.api.NamedDomainObjectContainer;

import java.util.Map;

import groovy.lang.Closure;

/**
 * Created by chengkai on 2021/4/19.
 */
public class PlaitTraceMethodExtension {
    /**
     * 要织入的类名和方法名用 . 分割
     */
    public String name;
    public boolean isMethodExit = false;

    /**
     * 定义一个 NamedDomainObjectContainer 属性
     * 需要注入的类列表
     */
    public NamedDomainObjectContainer<TraceMethodListExtension> classList;

    public NamedDomainObjectContainer<TraceMethodListExtension> blackClassList;

    public PlaitTraceMethodExtension(String name) {
        this.name = name;
    }

    //让其支持 Gradle DSL 语法
//    public void classList(Action<NamedDomainObjectContainer<PlaitMethodListExtension>> action) {
//        action.execute(classList);
//    }

    //创建内部Extension，名称为方法名 plaitMethod
    public void classList(Closure c) {
        org.gradle.util.ConfigureUtil.configure(c, classList);
    }
    public void blackClassList(Closure c) {
        org.gradle.util.ConfigureUtil.configure(c, blackClassList);
    }

    @Override
    public String toString() {
        Map s = null;
        if (classList != null) {
            s = classList.getAsMap();
        }
        Map b = null;
        if (blackClassList != null) {
            b = blackClassList.getAsMap();
        }
        return "PlaitMethodExtension{" +
                "name='" + name +
                ", classList= " + s +
                ", blackClassList= " + b +
                '}';
    }
}
