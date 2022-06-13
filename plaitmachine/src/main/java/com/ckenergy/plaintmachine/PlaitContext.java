package com.ckenergy.plaintmachine;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.leangen.geantyref.TypeFactory;
import kotlin.jvm.internal.SpreadBuilder;

/**
 * Created by chengkai on 2021/7/28.
 */
public class PlaitContext {

    private final String methodName;

    private final Object current;

    private final Object[] args;

    private final List<Annotation> annotationList;

    public PlaitContext(String methodName, Object current, Object[] args, HashMap<String, HashMap<String, Object>> annotations) {
        this.methodName = methodName;
        this.current = current;
        this.args = args;

        if (annotations != null && !annotations.isEmpty()) {
            annotationList = new ArrayList<>(annotations.size());

            for (Map.Entry<String, HashMap<String, Object>> entry : annotations.entrySet()) {
                try {
                    Class<Annotation> kclss = (Class<Annotation>) Class.forName(entry.getKey().substring(1).replace("/", "."));
                    Annotation annotation = TypeFactory.annotation(kclss, entry.getValue());
//                    val annotation = AnnotationParser.annotationForMap(
//                        Class.forName(it.key.substring(1).replace("/", ".")) as Class<Annotation>, it.value) as Annotation
                    annotationList.add(annotation);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }else {
            annotationList = null;
        }
    }

    public List<Annotation> getAnnotationList() {
        return annotationList;
    }

    public Object[] getArgs() {
        return args;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object getCurrent() {
        return current;
    }

    @Override
    public String toString() {
        return "PlaitContext{" +
                "methodName='" + methodName + '\'' +
                ", current=" + current +
                ", args=" + Arrays.toString(args) +
                ", annotationList=" + annotationList +
                '}';
    }
}
