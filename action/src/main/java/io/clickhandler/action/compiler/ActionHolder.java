package io.clickhandler.action.compiler;

import com.squareup.javapoet.ClassName;
import io.clickhandler.action.*;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 *
 */
public class ActionHolder {
    RemoteAction remoteAction;
    InternalAction internalAction;
    WorkerAction workerAction;
    ScheduledAction scheduledAction;
    ActionConfig config;
    TypeElement type;
    TypeMirror actorTypeClass;
    DeclaredTypeVar actorType;
    DeclaredTypeVar inType;
    DeclaredTypeVar outType;

    String pkgName = null;

    public boolean isRemote() {
        return remoteAction != null;
    }

    public boolean isInternal() {
        return internalAction != null;
    }

    public boolean isWorker() {
        return workerAction != null;
    }

    public boolean isScheduled() {
        return scheduledAction != null;
    }

    public ClassName getProviderTypeName() {
        Class providerClass = null;
        if (config != null) {
            providerClass = config.provider();
        }

        if (providerClass != null && !providerClass.equals(ActionProvider.class)) {
            return ClassName.get(providerClass);
        }

        if (isRemote()) {
            return ClassName.get(RemoteActionProvider.class);
        } else if (isInternal()) {
            return ClassName.get(InternalActionProvider.class);
        } else {
            return ClassName.get(ActionProvider.class);
        }
    }

    public String getName() {
        return type.getQualifiedName().toString();
    }

    public String getFieldName() {
        final String name = type.getSimpleName().toString();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    public String getPackage() {
        if (pkgName != null) {
            return pkgName;
        }

        final String qname = type.getQualifiedName().toString();

        final String[] parts = qname.split("[.]");
        if (parts.length == 1) {
            return pkgName = "";
        } else {
            final String lastPart = parts[parts.length - 1];
            return pkgName = qname.substring(0, qname.length() - lastPart.length() - 1);
        }
    }
}
