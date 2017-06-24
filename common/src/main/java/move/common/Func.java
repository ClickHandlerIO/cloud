package move.common;

/**
 *
 */
public class Func {

  @FunctionalInterface
  public interface Run1<T> {

    void run(T value);
  }
}
