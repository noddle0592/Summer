package ren.yale.java.annotation;

import java.lang.annotation.*;

/**
 * http的返回状态值
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HttpStatus {
    int value() default -1;
}
