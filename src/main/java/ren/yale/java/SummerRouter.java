package ren.yale.java;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ren.yale.java.interceptor.Interceptor;
import ren.yale.java.method.ArgInfo;
import ren.yale.java.method.ClassInfo;
import ren.yale.java.method.MethodInfo;
import ren.yale.java.tools.PathParamConverter;
import ren.yale.java.tools.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.wx.rs.ALL;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.net.HttpURLConnection.*;

/**
 * Yale
 * <p>
 * create at:  2018-02-01 14:08
 **/
public class SummerRouter {
    private final static Logger LOGGER = LoggerFactory.getLogger(SummerRouter.class.getName());

    private List<ClassInfo> classInfos;
    private final Router router;
    private final Vertx vertx;
    private final MethodsProcessor methodsProcessor;
    private String contextPath = "";

    public SummerRouter(Router router, Vertx vertx) {
        this(router, vertx, new DefaultMethodsProcessor());
    }

    public SummerRouter(Router router, Vertx vertx, MethodsProcessor methodsProcessor) {
        this.router = router;
        this.vertx = vertx;
        this.methodsProcessor = methodsProcessor;
        this.classInfos = new ArrayList<>();
//        this.init();
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        if (!StringUtils.isEmpty(contextPath)) {
            this.contextPath = contextPath;
        }
    }

    public Router getRouter() {
        return router;
    }

    private void init() {
        router.route().handler(BodyHandler.create());
        router.route().handler(CookieHandler.create());
        SessionHandler handler = SessionHandler.create(LocalSessionStore.create(vertx));
        handler.setNagHttps(true);
        router.route().handler(handler);
    }

    private boolean isRegister(Class clazz) {
        for (ClassInfo classInfo : classInfos) {
            if (classInfo.getClazz() == clazz) {
                return true;
            }
        }
        return false;
    }

    public void registerResource(Object handler) {
        this.registerResource(handler, null);
    }

    public void registerResource(Object handler, AuthHandler authHandler) {
        if (handler != null) {
            this.registerResource(handler, handler.getClass(), authHandler);
            if (handler instanceof VertxAware) {
                ((VertxAware) handler).setVertx(vertx);
            }
        }
    }

    public void registerResource(Class clazz) {
        this.registerResource(null, clazz, null);
    }

    public void registerResource(Class clazz, AuthHandler authHandler) {
        this.registerResource(null, clazz, authHandler);
    }

    private void registerResource(Object handler, Class clazz, AuthHandler authHandler) {
        if (isRegister(clazz)) {
            return;
        }
        ClassInfo classInfo = methodsProcessor.get(handler, clazz);
        if (classInfo != null) {
            classInfos.add(classInfo);
            if (authHandler != null) {
                // 需要控制权限
                router.route(classInfo.getClassPath() + "/*").handler(authHandler);
            }
            for (MethodInfo methodInfo : classInfo.getMethodInfoList()) {
                // 没有标注httpMethod的方法一律不处理，需要所有方法都能访问，则标注为@ALL
                if (methodInfo.getHttpMethod() != null) {
                    String p = classInfo.getClassPath() + methodInfo.getMethodPath();
                    p = PathParamConverter.converter(p);
                    p = addContextPath(p);
                    Route route = null;
                    if (methodInfo.getHttpMethod() == GET.class) {
                        route = router.get(p);
                    } else if (methodInfo.getHttpMethod() == POST.class) {
                        route = router.post(p);
                    } else if (methodInfo.getHttpMethod() == PUT.class) {
                        route = router.put(p);
                    } else if (methodInfo.getHttpMethod() == DELETE.class) {
                        route = router.delete(p);
                    } else if (methodInfo.getHttpMethod() == OPTIONS.class) {
                        route = router.options(p);
                    } else if (methodInfo.getHttpMethod() == HEAD.class) {
                        route = router.head(p);
                    } else if (methodInfo.getHttpMethod() == ALL.class) {
                        route = router.route(p);
                    }
                    if (methodInfo.isBlocking()) {
                        route.blockingHandler(getHandler(classInfo, methodInfo));
                    } else {
                        route.handler(getHandler(classInfo, methodInfo));
                    }
                }
            }
        }

    }

    private String addContextPath(String path) {
        return contextPath + path;
    }

    private Object covertType(Class type, String v) throws Exception {
        String typeName = type.getTypeName();
        Object value = null;
        if (type == String.class) {
            value = v;
        } else if (type == Integer.class || typeName.equals("int")) {
            value = Integer.parseInt(v);
        } else if (type == Long.class || typeName.equals("long")) {
            value = Long.parseLong(v);
        } else if (type == Float.class || typeName.equals("float")) {
            value = Float.parseFloat(v);
        } else if (type == Double.class || typeName.equals("double")) {
            value = Double.parseDouble(v);
        } else if (type == Boolean.class || typeName.equals("boolean")) {
            value = Boolean.valueOf(v);
        } else if (type == List.class) {
            String[] array = v.split(",");
            value = Arrays.asList(array);
        }
        return value;

    }

    private Object getPathParamArg(RoutingContext routingContext, ArgInfo argInfo) {

        try {
            String path = routingContext.request().getParam(argInfo.getPathParam());
            if (!StringUtils.isEmpty(path)) {
                return covertType(argInfo.getClazz(), path);
            }
            if (!StringUtils.isEmpty(argInfo.getDefaultValue())) {
                return covertType(argInfo.getClazz(), argInfo.getDefaultValue());
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return null;

    }

    private Object getFromParamArg(RoutingContext routingContext, ArgInfo argInfo) {

        try {
            String q = routingContext.request().getParam(argInfo.getFormParam());
            if (!StringUtils.isEmpty(q)) {
                return covertType(argInfo.getClazz(), q);
            }
            if (!StringUtils.isEmpty(argInfo.getDefaultValue())) {
                return covertType(argInfo.getClazz(), argInfo.getDefaultValue());
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return null;
    }

    private Object getQueryParamArg(RoutingContext routingContext, ArgInfo argInfo) {

        try {
            String q = routingContext.request().getParam(argInfo.getQueryParam());
            if (!StringUtils.isEmpty(q)) {
                return covertType(argInfo.getClazz(), q);
            }
            if (!StringUtils.isEmpty(argInfo.getDefaultValue())) {
                return covertType(argInfo.getClazz(), argInfo.getDefaultValue());
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return null;
    }

    private Object getContext(RoutingContext routingContext, MethodInfo methodInfo, ArgInfo argInfo) {
        Class clz = argInfo.getClazz();
        if (clz == RoutingContext.class) {
            return routingContext;
        } else if (clz == Handler.class) {
            return getAsyncHandler(routingContext, methodInfo);
        } else if (clz == HttpServerRequest.class) {
            return routingContext.request();
        } else if (clz == HttpServerResponse.class) {
            return routingContext.response();
        } else if (clz == Session.class) {
            return routingContext.session();
        } else if (clz == Vertx.class) {
            return vertx;
        }
        return null;
    }

    private Object[] getArgs(RoutingContext routingContext, MethodInfo methodInfo) {

        Object[] objects = new Object[methodInfo.getArgInfoList().size()];
        int i = 0;
        for (ArgInfo argInfo : methodInfo.getArgInfoList()) {
            if (argInfo.isContext()) {
                objects[i] = getContext(routingContext, methodInfo, argInfo);
            } else if (argInfo.isQueryParam()) {
                objects[i] = getQueryParamArg(routingContext, argInfo);
            } else if (argInfo.isFormParam()) {
                objects[i] = getFromParamArg(routingContext, argInfo);
            } else if (argInfo.isPathParam()) {
                objects[i] = getPathParamArg(routingContext, argInfo);
            } else {
                objects[i] = null;
            }
            i++;
        }

        return objects;

    }

    private Handler<AsyncResult> getAsyncHandler(RoutingContext routingContext, MethodInfo methodInfo) {
        return (asyncResult -> {
            try {
                if (asyncResult.succeeded()) {
                    Object result = asyncResult.result();
                    if (result != null && result.getClass() != Void.class) {
                        this.handlerResponseResult(routingContext, methodInfo, result);
                    } else {
                        routingContext.response().setStatusCode(HTTP_NOT_FOUND).end();
                    }
                } else {
                    routingContext.fail(asyncResult.cause());
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
                e.printStackTrace();
                routingContext.response().setStatusCode(HTTP_INTERNAL_ERROR).putHeader("Content-Type", MediaType.TEXT_PLAIN + ";charset=utf-8")
                        .end(e.toString());
            }
        });
    }

    private String convert2XML(Object object) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(object.getClass());
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        marshaller.marshal(object, baos);
        return new String(baos.toByteArray());
    }

    private int handleBefores(RoutingContext routingContext, ClassInfo classInfo, MethodInfo methodInfo) {
        List<Interceptor> beforeList = new ArrayList<>();

        if (methodInfo.getBefores() != null) {
            beforeList.addAll(Arrays.asList(methodInfo.getBefores()));
        }
        if (classInfo.getBefores() != null) {
            beforeList.addAll(Arrays.asList(classInfo.getBefores()));
        }
        for (Interceptor inter : beforeList) {
            int statusCode = inter.handle(routingContext, null);
            if (statusCode != HTTP_OK) {
                return statusCode;
            }
        }
        return HTTP_OK;
    }

    private int handleAfters(RoutingContext routingContext, ClassInfo classInfo, MethodInfo methodInfo
            , Object obj) {
        List<Interceptor> list = new ArrayList<>();

        if (methodInfo.getAfters() != null) {
            list.addAll(Arrays.asList(methodInfo.getAfters()));
        }
        if (classInfo.getAfters() != null) {
            list.addAll(Arrays.asList(classInfo.getAfters()));
        }
        for (Interceptor inter : list) {
            int statusCode = inter.handle(routingContext, obj);
            if (statusCode != HTTP_OK) {
                return statusCode;
            }
        }
        return HTTP_OK;
    }

    private void handlers(ClassInfo classInfo, MethodInfo methodInfo, RoutingContext routingContext) {
        int statusCode = handleBefores(routingContext, classInfo, methodInfo);
        if (handleBefores(routingContext, classInfo, methodInfo) != HTTP_OK) {
            routingContext.response().setStatusCode(statusCode).end();
            return;
        }
        Object[] args = getArgs(routingContext, methodInfo);
        routingContext.response().putHeader("Content-Type", methodInfo.getProducesType())
                .setStatusCode(methodInfo.getHttpStatus() > 0 ? methodInfo.getHttpStatus() : HTTP_OK);

        try {
            Object result = methodInfo.getMethod().invoke(classInfo.getClazzObj(), args);
            if (result != null && result.getClass() != Void.class) {
                if (!routingContext.response().ended()) {
                    statusCode = handleAfters(routingContext, classInfo, methodInfo, result);
                    if (statusCode != HTTP_OK) {
                        routingContext.response().setStatusCode(statusCode).end();
                        return;
                    }
                    this.handlerResponseResult(routingContext, methodInfo, result);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.toString());
            e.printStackTrace();
            routingContext.response().setStatusCode(HTTP_INTERNAL_ERROR).putHeader("Content-Type", MediaType.TEXT_PLAIN + ";charset=utf-8")
                    .end(e.toString());
        }
    }

    private void handlerResponseResult(RoutingContext routingContext, MethodInfo methodInfo, Object result) throws JAXBException {
        if (!routingContext.response().ended()) {
            if (result instanceof String) {
                routingContext.response().end((String) result);
            } else if (methodInfo.getProducesType().indexOf(MediaType.APPLICATION_JSON) >= 0) {
                this.handlerResponseJson(routingContext, result);
            } else if (methodInfo.getProducesType().indexOf(MediaType.TEXT_HTML) >= 0 ||
                    methodInfo.getProducesType().indexOf(MediaType.TEXT_PLAIN) >= 0) {
                routingContext.response().end(result.toString());
            } else if (methodInfo.getProducesType().indexOf(MediaType.TEXT_XML) >= 0 ||
                    methodInfo.getProducesType().indexOf(MediaType.APPLICATION_XML) >= 0) {
                routingContext.response().end(convert2XML(result));
            } else {
                routingContext.response()
                        .putHeader("Content-Type", MediaType.APPLICATION_JSON + ";charset=utf-8");
                this.handlerResponseJson(routingContext, result);
            }
        }
    }

    private void handlerResponseJson(RoutingContext routingContext, Object result) {
        if (result instanceof List) {
            routingContext.response().end(new JsonArray((List) result).encodePrettily());
        } else {
            routingContext.response().end(JsonObject.mapFrom(result).encodePrettily());
        }
    }

    private Handler<RoutingContext> getHandler(ClassInfo classInfo, MethodInfo methodInfo) {

        return (routingContext -> {
            try {
                handlers(classInfo, methodInfo, routingContext);
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
                routingContext.response().setStatusCode(HTTP_INTERNAL_ERROR).putHeader("Content-Type", MediaType.TEXT_PLAIN + ";charset=utf-8")
                        .end(e.toString());
            }
        });
    }
}

