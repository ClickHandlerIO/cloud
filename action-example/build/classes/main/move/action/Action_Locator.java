package move.action;

import java.lang.Override;
import java.lang.String;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;

@Singleton
public final class Action_Locator extends ActionLocator {
  @NotNull
  public final InternalActionProvider<AllocateInventory, String, String> allocateInventory;

  @NotNull
  public final InternalActionProvider<AllocateInventory2, String, String> allocateInventory2;

  @NotNull
  public final InternalActionProvider<MyAsyncAction, String, String> myAsyncAction;

  @NotNull
  public final ScheduledActionProvider<MyScheduledAction> myScheduledAction;

  @NotNull
  public final FifoWorkerActionProvider<MyWorker, MyWorker.Request> myWorker;

  @Inject
  Action_Locator(final InternalActionProvider<AllocateInventory, String, String> allocateInventory,
      final InternalActionProvider<AllocateInventory2, String, String> allocateInventory2,
      final InternalActionProvider<MyAsyncAction, String, String> myAsyncAction,
      final ScheduledActionProvider<MyScheduledAction> myScheduledAction,
      final FifoWorkerActionProvider<MyWorker, MyWorker.Request> myWorker) {
    this.allocateInventory = allocateInventory;
    this.allocateInventory2 = allocateInventory2;
    this.myAsyncAction = myAsyncAction;
    this.myScheduledAction = myScheduledAction;
    this.myWorker = myWorker;
  }

  @NotNull
  public final InternalActionProvider<AllocateInventory, String, String> allocateInventory() {
    return allocateInventory;
  }

  @NotNull
  public final InternalActionProvider<AllocateInventory2, String, String> allocateInventory2() {
    return allocateInventory2;
  }

  @NotNull
  public final InternalActionProvider<MyAsyncAction, String, String> myAsyncAction() {
    return myAsyncAction;
  }

  @NotNull
  public final ScheduledActionProvider<MyScheduledAction> myScheduledAction() {
    return myScheduledAction;
  }

  @NotNull
  public final FifoWorkerActionProvider<MyWorker, MyWorker.Request> myWorker() {
    return myWorker;
  }

  @Override
  protected void initActions() {
    actionMap.put(AllocateInventory.class, allocateInventory);
    actionMap.put(AllocateInventory2.class, allocateInventory2);
    actionMap.put(MyAsyncAction.class, myAsyncAction);
    actionMap.put(MyScheduledAction.class, myScheduledAction);
    actionMap.put(MyWorker.class, myWorker);
  }

  @Override
  protected void initChildren() {
  }
}
