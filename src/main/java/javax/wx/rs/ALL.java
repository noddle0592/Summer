package javax.wx.rs;

import javax.ws.rs.HttpMethod;
import java.lang.annotation.*;

/**
 * 支持http的所有方法访问
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("ALL")
@Documented
public @interface ALL {
}