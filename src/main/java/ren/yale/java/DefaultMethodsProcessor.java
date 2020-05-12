package ren.yale.java;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ren.yale.java.annotation.Blocking;
import ren.yale.java.annotation.HttpStatus;
import ren.yale.java.aop.After;
import ren.yale.java.aop.Before;
import ren.yale.java.interceptor.Interceptor;
import ren.yale.java.method.ArgInfo;
import ren.yale.java.method.ClassInfo;
import ren.yale.java.method.MethodInfo;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.wx.rs.ALL;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

/**
 * @author zlf
 * @date 2020-05-11
 */
public class DefaultMethodsProcessor implements MethodsProcessor {
    private final Logger LOGGER = LoggerFactory.getLogger(MethodsProcessor.class.getName());

    private Object newClass(Class clazz) {
        try {
            for (Constructor<?> c : clazz.getDeclaredConstructors()) {
                c.setAccessible(true);
                if (c.getParameterCount() == 0) {
                    return c.newInstance();
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return null;

    }

    private String getPathValue(Path path) {
        if (path == null || path.value() == null) {
            return "";
        }
        return path.value();

    }

    private String getProducesValue(Produces produces) {
        if (produces == null || produces.value() == null || produces.value().length == 0) {
            return MethodInfo.PRODUCES_TYPE_ALL;
        }

        StringBuilder sb = new StringBuilder();
        for (String str : produces.value()) {
            if (sb.length() == 0) {
                sb.append(str);
            } else {
                sb.append(";");
                sb.append(str);
            }
        }

        return sb.toString();

    }

    private Interceptor[] getIntercepter(Class<? extends Interceptor>[] inter) {
        try {
            Interceptor[] interceptors = new Interceptor[inter.length];
            int i = 0;
            for (Class<? extends Interceptor> cls : inter) {
                interceptors[i] = this.intecepterInstance(cls);
                i++;
            }
            return interceptors;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 过滤器实例化
     * @param cls 过滤器的Class对象
     * @return 实例化的过滤器
     * @throws Exception
     */
    protected Interceptor intecepterInstance(Class<? extends Interceptor> cls) throws Exception {
        return cls.newInstance();
    }

    private Interceptor[] getBefores(Before before) {
        if (before != null && before.value() != null && before.value().length > 0) {
            return getIntercepter(before.value());
        }
        return null;
    }

    private Interceptor[] getAfters(After after) {
        if (after != null && after.value() != null && after.value().length > 0) {
            return getIntercepter(after.value());
        }
        return null;
    }

    public ClassInfo get(Object handler, Class clazz) {
        Path path = (Path) clazz.getAnnotation(Path.class);

        if (path == null || path.value() == null) {
            return null;
        }

        ClassInfo classInfo = new ClassInfo();
        classInfo.setClassPath(path.value());
        classInfo.setClazzObj(handler != null ? handler : newClass(clazz));
        classInfo.setClazz(clazz);

        Interceptor[] interceptorsClazz =
                getBefores((Before) clazz.getAnnotation(Before.class));
        if (interceptorsClazz != null) {
            classInfo.setBefores(interceptorsClazz);
        }
        interceptorsClazz =
                getAfters((After) clazz.getAnnotation(After.class));
        if (interceptorsClazz != null) {
            classInfo.setAfters(interceptorsClazz);
        }
        for (Method method : clazz.getMethods()) {
            Class mt = method.getDeclaringClass();
            if (mt == Object.class) {
                continue;
            }
            MethodInfo methodInfo = new MethodInfo();


            Interceptor[] interceptorsMethod =
                    getBefores((Before) method.getAnnotation(Before.class));
            if (interceptorsMethod != null) {
                methodInfo.setBefores(interceptorsMethod);
            }

            interceptorsMethod =
                    getAfters((After) method.getAnnotation(After.class));
            if (interceptorsMethod != null) {
                methodInfo.setAfters(interceptorsMethod);
            }

            Blocking blocking = method.getAnnotation(Blocking.class);
            if (blocking != null) {
                methodInfo.setBlocking(true);
            }

            Path pathMthod = (Path) method.getAnnotation(Path.class);
            Produces produces = (Produces) method.getAnnotation(Produces.class);

            methodInfo.setMethodPath(getPathValue(pathMthod));
            methodInfo.setProducesType(getProducesValue(produces));

            methodInfo.setHttpMethod(getHttpMethod(method));
            methodInfo.setMethod(method);

            HttpStatus httpStatus = (HttpStatus) method.getAnnotation(HttpStatus.class);
            if (httpStatus != null) {
                methodInfo.setHttpStatus(httpStatus.value());
            }

            Parameter[] parameters = method.getParameters();
            Class<?>[] parameterTypes = method.getParameterTypes();
            Annotation[][] annotations = method.getParameterAnnotations();


            int i = 0;
            for (Annotation[] an : annotations) {

                ArgInfo argInfo = new ArgInfo();

                argInfo.setAnnotation(an);
                argInfo.setClazz(parameterTypes[i]);
                argInfo.setParameter(parameters[i]);

                for (Annotation ant : an) {
                    if (ant instanceof Context) {
                        argInfo.setContext(true);
                    } else if (ant instanceof DefaultValue) {
                        argInfo.setDefaultValue(((DefaultValue) ant).value());
                    } else if (ant instanceof PathParam) {
                        argInfo.setPathParam(true);
                        argInfo.setPathParam(((PathParam) ant).value());
                    } else if (ant instanceof QueryParam) {
                        argInfo.setQueryParam(true);
                        argInfo.setQueryParam(((QueryParam) ant).value());
                    } else if (ant instanceof FormParam) {
                        argInfo.setFormParam(true);
                        argInfo.setFormParam(((FormParam) ant).value());
                    }
                }

                i++;
                methodInfo.addArgInfo(argInfo);
            }

            classInfo.addMethodInfo(methodInfo);

        }
        return classInfo;
    }

    private boolean isRestClass(Class cls) {

        List<Class<Path>> search = Arrays.asList(Path.class);

        for (Class<? extends Annotation> item : search) {
            if (cls.getAnnotation(item) != null) {
                return true;
            }
        }

        return false;
    }

    private Class getHttpMethod(Method method) {

        List<Class<? extends Annotation>> search = Arrays.asList(
                GET.class,
                POST.class,
                PUT.class,
                DELETE.class,
                OPTIONS.class,
                HEAD.class,
//                PATCH.class, PATCH相当于没标注，因为Vertx不支持
                ALL.class);

        for (Class<? extends Annotation> item : search) {
            if (method.getAnnotation(item) != null) {
                return item;
            }
        }

        return null;
    }
}
