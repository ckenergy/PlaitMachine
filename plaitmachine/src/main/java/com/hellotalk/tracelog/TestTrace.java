package com.hellotalk.tracelog;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by chengkai on 2021/7/28.
 */
@Retention(RUNTIME)
@Target({METHOD, CONSTRUCTOR, TYPE, PARAMETER})
public @interface TestTrace {

    String[] value() default "";

    TraceType[] type() default TraceType.NORMAL;

    int[] intRes() default 0;
    float[] floatRes() default 0;
    byte[] byteRes() default 0;
    char[] chatRes() default 0;
    long[] longRes() default 0;
    double[] doubleRes() default 0;
    short[] shortRes() default 0;
    boolean[] boolRes() default false;

    byte byteType() default 0;
    char charType() default '0';
    long longType() default 0;
    float floatType() default 0;
    double doubleType() default 0;
    boolean booleanType() default false;
    short shortType() default 0;

}
