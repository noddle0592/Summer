package ren.yale.java;

import io.vertx.core.Vertx;

/**
 * 用于设置vertx对象。需要使用vertx的Controller只需要实现此方法即可
 * @author zlf
 * @date 2020-05-11
 */
public interface VertxAware {
    void setVertx(Vertx vertx);
}
