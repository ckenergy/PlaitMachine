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

    int value() default 1;

}
