package move.action;

/**
 *
 */
public class Java {

  public static void main(String[] args) {
    MainKt.getMove().AllocateInventory.singleBuilder(request -> {
      request.setId("id");
    }).subscribe(
        r -> {},
        e -> {}
    );
  }
}
