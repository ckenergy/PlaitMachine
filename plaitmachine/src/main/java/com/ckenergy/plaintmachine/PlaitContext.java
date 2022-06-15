package com.ckenergy.plaintmachine;

import org.jetbrains.annotations.Nullable;

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

    private static Annotation buildAnnotation(String key, Map<String, Object> annotations) {
        for (Map.Entry<String, Object> entry : annotations.entrySet()) {
            if (entry.getValue() instanceof Pair) {
                Pair<String, Map> pair = (Pair<String, Map>) entry.getValue();
                annotations.put(entry.getKey(), buildAnnotation(pair.getFirst(), (Map<String, Object>) pair.getSecond()));
            } else if (entry.getValue() instanceof Pair[]) {
                Pair[] maps = (Pair[]) entry.getValue();
                if (maps.length > 0) {
                    Annotation[] annotationList = null;
                    String className = maps[0].getFirst().toString();
                    try {
                        annotationList = (Annotation[]) Array.newInstance(Class.forName(className.replace("/", ".")), maps.length);
                        for (int i = 0; i < maps.length; i++) {
                            annotationList[i] = buildAnnotation(className, (Map<String, Object>)maps[i].getSecond());
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
            Class<Annotation> annotationClass = (Class<Annotation>) Class.forName(key.replace("/", "."));
            return TypeFactory.annotation(annotationClass, annotations);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private final String methodName;

    @Nullable
    private final Object current;

    @Nullable
    private final Object[] args;

    @Nullable
    private final List<Annotation> annotationList;

    public PlaitContext(String methodName, @Nullable Object current, @Nullable Object[] args, @Nullable HashMap<String, HashMap<String, Object>> annotations) {
        this.methodName = methodName;
        this.current = current;
        this.args = args;

        if (annotations != null && !annotations.isEmpty()) {
            annotationList = new ArrayList<>(annotations.size());

            for (Map.Entry<String, HashMap<String, Object>> entry : annotations.entrySet()) {
                try {
                    annotationList.add(buildAnnotation(entry.getKey(), entry.getValue()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            annotationList = null;
        }

    }

    @Nullable
    public List<Annotation> getAnnotationList() {
        return annotationList;
    }

    @Nullable
    public Object[] getArgs() {
        return args;
    }

    public String getMethodName() {
        return methodName;
    }

    @Nullable
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
