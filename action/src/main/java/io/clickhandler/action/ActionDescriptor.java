package io.clickhandler.action;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.netflix.hystrix.*;

import javax.inject.Provider;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Clay Molocznik
 */
public class ActionDescriptor<T extends Action, IN, OUT> {
    public static final int DEFAULT_REST_TIMEOUT = 30000;
    public static final int DEFAULT_TIMEOUT = 5000;
    static final String SUCCESS_ENUM = "SUCCESS";

    public final Class<IN> requestClass;
    public final Class<OUT> responseClass;
    public final Class responseCodeClass;
    public final Class actionClassImpl;
    public final ActionConfig actionConfig;
    public final boolean sessionRequired;
    public final String threadGroupKey;
    public final Set<Object> successResponseCodes = Sets.newHashSet();
    public final Set<Object> fatalResponseCodes = Sets.newHashSet();
    public final Provider<T> handlerProvider;
    public final RestActionDescriptor rest;
    public final AtomicReference<HystrixCommand.Setter> commandSetter = new AtomicReference<>();
    public final AtomicReference<HystrixObservableCommand.Setter> observableCommandSetter = new AtomicReference<>();

    public ActionDescriptor(Class<IN> requestClass,
                            Class<OUT> responseClass,
                            Class<T> actionClassImpl,
                            ActionConfig actionConfig,
                            Provider<T> handlerProvider,
                            RestActionDescriptor rest) {
        this.requestClass = requestClass;
        if (!ActionResponse.class.isAssignableFrom(responseClass)) {
            throw new RuntimeException("Action Response Class [" + responseClass.getCanonicalName() + "] does not implement ActionResponse");
        }
        this.responseClass = responseClass;

        final TypeToken<?> codeType = TypeToken.of(responseClass).resolveType(ActionResponse.class.getTypeParameters()[0]);
        responseCodeClass = codeType.getRawType();
        if (responseCodeClass != null && responseCodeClass.isEnum()) {
            final Object[] enumConstants = responseCodeClass.getEnumConstants();
            if (enumConstants != null && enumConstants.length > 0) {
                for (Object enumConstant : enumConstants) {
                    final String name = Strings.nullToEmpty(enumConstant.toString()).trim().toUpperCase();
                    if (name.equals(SUCCESS_ENUM) ||
                        enumConstant.getClass().getAnnotation(Success.class) != null) {
                        successResponseCodes.add(enumConstant);
                    } else {
                        fatalResponseCodes.add(enumConstant);
                    }
                }
            }
        }
        this.actionClassImpl = actionClassImpl;
        this.actionConfig = actionConfig;
        this.sessionRequired = actionConfig == null || actionConfig.sessionRequired();
        this.threadGroupKey = actionConfig != null ? actionConfig.threadPoolKey() : null;
        this.handlerProvider = handlerProvider;
        this.rest = rest;

        String groupKey = Strings.nullToEmpty(actionConfig != null ? actionConfig.groupKey() : "").trim();
        if (groupKey.isEmpty()) {
            groupKey = actionClassImpl.getPackage().getName();
        }
        String commandKey = Strings.nullToEmpty(actionConfig != null ? actionConfig.commandKey() : "").trim();
        if (commandKey.isEmpty()) {
            commandKey = actionClassImpl.getSimpleName();
        }
        HystrixCommandProperties.Setter commandPropSetter = HystrixCommandProperties.Setter();

        final int timeoutInMillis = actionConfig == null
            ? rest != null
            ? DEFAULT_REST_TIMEOUT
            : DEFAULT_TIMEOUT
            : actionConfig.maxExecutionMillis() < 1
            ? DEFAULT_TIMEOUT
            : actionConfig.maxExecutionMillis();

        commandPropSetter.withExecutionTimeoutEnabled(timeoutInMillis != Integer.MAX_VALUE);
        if (commandPropSetter.getExecutionTimeoutEnabled() == Boolean.TRUE) {
            commandPropSetter.withExecutionTimeoutInMilliseconds(timeoutInMillis);
        }

        final String threadPoolKey = actionConfig == null || actionConfig.threadPoolKey() == null ? null : actionConfig.threadPoolKey();

        this.commandSetter.set(
            HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey))
                .andCommandPropertiesDefaults(commandPropSetter)
//                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(threadPoolKey))
        );

        this.observableCommandSetter.set(
            HystrixObservableCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey))
                .andCommandPropertiesDefaults(commandPropSetter)
        );
    }

    public T createAction() {
        return handlerProvider.get();
    }

    public boolean requiresToken(String token) {
        return !sessionRequired || (token != null && !token.isEmpty());
    }

    public boolean isFatal(Object value) {
        return value == null || !successResponseCodes.contains(value);
    }
}
