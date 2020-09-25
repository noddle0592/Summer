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
     * @return 返回http的状态码。这里只有返回HTTP_OK，也就是200不拦截，其它都拦截
     *          一般本来所有非2XX的代码均表示拦截。反正其它也2XX也用不着，省点性能
     */
    int handle(RoutingContext routingContext, Object object);
}
