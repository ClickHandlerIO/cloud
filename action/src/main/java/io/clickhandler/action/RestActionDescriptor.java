package io.clickhandler.action;


import java.lang.reflect.Method;

/**
 *
 */
public class RestActionDescriptor {
    public final Method method;
    public final String path;
    public final Class requestClass;
    public final Class responseClass;

    public RestActionDescriptor(Method method, String path, Class requestClass, Class responseClass) {
        this.method = method;
        this.path = path;
        this.requestClass = requestClass;
        this.responseClass = responseClass;
    }
}
