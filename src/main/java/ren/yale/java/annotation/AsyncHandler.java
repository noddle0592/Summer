package ren.yale.java.annotation;

import java.lang.annotation.*;

/**
 * 异步执行的service返回的结果处理注解
 */
@Target({ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AsyncHandler {
    /**
     * define the http status
     */
    int value() default -1;
}
