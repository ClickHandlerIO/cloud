package move.action.processor;

import com.squareup.javapoet.ClassName;
import javax.lang.model.element.TypeElement;
import move.action.ActionConfig;
import move.action.ActionProvider;
import move.action.FifoWorkerActionProvider;
import move.action.InternalAction;
import move.action.InternalActionProvider;
import move.action.RemoteAction;
import move.action.RemoteActionProvider;
import move.action.ScheduledAction;
import move.action.ScheduledActionProvider;
import move.action.WorkerAction;
import move.action.WorkerActionProvider;

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
    } else if (isWorker()) {
      return workerAction.fifo() ?
          ClassName.get(FifoWorkerActionProvider.class) :
          ClassName.get(WorkerActionProvider.class);
    } else if (isScheduled()) {
      return ClassName.get(ScheduledActionProvider.class);
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
