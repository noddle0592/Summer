package ren.yale.java.method;

import ren.yale.java.interceptor.Interceptor;

import javax.ws.rs.core.MediaType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Yale
 *
 * create at:  2018-01-31 17:46
 **/
public class MethodInfo {

    public static final String PRODUCES_TYPE_ALL= MediaType.APPLICATION_JSON+";charset=utf-8";
    private Class httpMethod;

    private String methodPath;

    private String producesType=PRODUCES_TYPE_ALL;

    private Method method;
    private List<ArgInfo> argInfoList = new ArrayList<>();
    private Interceptor[] befores;

    private Interceptor[] afters;
    private int httpStatus;

    public Interceptor[] getAfters() {
        return afters;
    }

    public void setAfters(Interceptor[] afters) {
        this.afters = afters;
    }

    private boolean isBlocking;
    public boolean isBlocking() {
        return isBlocking;
    }
    public void setBlocking(boolean blocking) {
        isBlocking = blocking;
    }

    public Interceptor[] getBefores() {
        return befores;
    }

    public void setBefores(Interceptor[] befores) {
        this.befores = befores;
    }

    public String getProducesType() {
        return producesType;
    }

    public void setProducesType(String producesType) {
        this.producesType = producesType;
    }

    public String getMethodPath() {
        return methodPath;
    }

    public void setMethodPath(String methodPath) {
        this.methodPath = methodPath;
    }


    public Class getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(Class httpMethod) {
        this.httpMethod = httpMethod;
    }
    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public List<ArgInfo> getArgInfoList() {
        return argInfoList;
    }

    public void addArgInfo(ArgInfo argInfo) {
       argInfoList.add(argInfo);
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }
}
