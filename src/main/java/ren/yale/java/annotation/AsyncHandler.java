package ren.yale.java.annotation;

import java.lang.annotation.*;

/**
 * 异步执行的service返回的结果处理标记
 */
@Target({ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AsyncHandler {
}
