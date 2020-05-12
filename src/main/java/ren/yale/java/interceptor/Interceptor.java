package ren.yale.java.interceptor;


import io.vertx.ext.web.RoutingContext;

/**
 * Created by yale on 2018/2/1.
 */
public interface Interceptor {
    /**
     * 过滤器处理逻辑
     * @param routingContext 上下文对象
     * @param object 方法执行完毕之后的返回结果。仅对@After有效，@Before则此参数均为null
     * @return 放行为false，拦截则返回true
     */
    boolean handle(RoutingContext routingContext, Object object);
}
