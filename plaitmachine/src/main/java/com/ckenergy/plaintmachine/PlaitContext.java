package com.ckenergy.plaintmachine;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.leangen.geantyref.TypeFactory;
import kotlin.Pair;

/**
 * Created by chengkai on 2021/7/28.
 */
public class PlaitContext {

    private static Annotation buildAnnotation(String pre, String key, Map<String, Object> annotations) {
        for (Map.Entry<String, Object> entry : annotations.entrySet()) {
            System.out.println(pre+"annotations key:" + entry.getKey() + ", type:" + entry.getValue());
            if (entry.getValue() instanceof Pair) {
                Pair<String, Map> pair = (Pair<String, Map>) entry.getValue();
                System.out.println(pre+"annotations pair key:" + pair.getFirst() + ", value:" + pair.getSecond());
                annotations.put(entry.getKey(), buildAnnotation(pre+"==", pair.getFirst(), (Map<String, Object>) pair.getSecond()));
            } else if (entry.getValue() instanceof Pair[]) {
                Pair[] maps = (Pair[]) entry.getValue();
                if (maps.length > 0) {
                    Annotation[] annotationList = new Annotation[0];
                    try {
                        annotationList = (Annotation[]) Array.newInstance(Class.forName(maps[0].getFirst().toString().substring(1).replace("/", ".")), maps.length);
                        for (int i = 0; i < maps.length; i++) {
                            annotationList[i] = buildAnnotation(pre+"==", maps[i].getFirst().toString(), (Map<String, Object>)maps[i].getSecond());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    annotations.put(entry.getKey(), annotationList);
                } else {
                    annotations.put(entry.getKey(), null);
                }
            }
        }
        try {
            Class<Annotation> annotationClass = (Class<Annotation>) Class.forName(key.substring(1).replace("/", "."));
            return TypeFactory.annotation(annotationClass, annotations);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private final String methodName;

    private final Object current;

    private final Object[] args;

    private final List<Annotation> annotationList;

    public PlaitContext(String methodName, Object current, Object[] args, HashMap<String, HashMap<String, Object>> annotations) {
        this.methodName = methodName;
        this.current = current;
        this.args = args;

        long start = System.currentTimeMillis();

        if (annotations != null && !annotations.isEmpty()) {
            annotationList = new ArrayList<>(annotations.size());

            for (Map.Entry<String, HashMap<String, Object>> entry : annotations.entrySet()) {
                System.out.println("annotations key:" + entry.getKey() + ", type:" + entry.getValue());
                try {
                    annotationList.add(buildAnnotation("==", entry.getKey(), entry.getValue()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            annotationList = null;
        }

        System.out.println("build time:"+(System.currentTimeMillis() - start));

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
                "methodName=" + methodName +
                ",\n current=" + current +
                ",\n args=" + Arrays.toString(args) +
                ",\n annotationList=" + annotationList +
                '}';
    }
}
