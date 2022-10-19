package com.ckenergy.trace.extension;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;


/**
 * Created by chengkai on 2021/4/19.
 */
public class PlaitExtension {
    public boolean enable = false;
    public boolean logInfo = false;

    //定义一个 NamedDomainObjectContainer 属性
    public NamedDomainObjectContainer<PlaitTraceMethodExtension> plaitClass;

    public PlaitExtension(Project project) {
        //通过 project.container(...) 方法创建 NamedDomainObjectContainer
        NamedDomainObjectContainer<PlaitTraceMethodExtension> domainObjs = project.container(PlaitTraceMethodExtension.class);
        plaitClass = domainObjs;
        plaitClass.all(new Action<PlaitTraceMethodExtension>() {
            @Override
            public void execute(PlaitTraceMethodExtension plaitTraceMethodExtension) {
                System.out.println("====PlaitExtension >>> " + plaitTraceMethodExtension);
                NamedDomainObjectContainer<TraceMethodListExtension> domainObjs1 = project.container(TraceMethodListExtension.class);
                NamedDomainObjectContainer<TraceMethodListExtension> domainObjs2 = project.container(TraceMethodListExtension.class);
                plaitTraceMethodExtension.classList = domainObjs1;
                plaitTraceMethodExtension.blackClassList = domainObjs2;
            }
        });
    }

    //让其支持 Gradle DSL 语法
    public void plaitClass(Action<NamedDomainObjectContainer<PlaitTraceMethodExtension>> action) {
        action.execute(plaitClass);
    }

    //创建内部Extension，名称为方法名 plaitMethod
//    void plaitMethod(Closure c) {
//        org.gradle.util.ConfigureUtil.configure(c, plaitMethod);
//    }

    @Override
    public String toString() {
        return "PlaitExtension{" +
                "enable=" + enable +
                ", plait_method=" + plaitClass +
                '}';
    }
}
