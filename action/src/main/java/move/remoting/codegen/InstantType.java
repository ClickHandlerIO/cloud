package move.remoting.codegen;

import java.time.Instant;

/**
 *
 */
public class InstantType extends AbstractType {

  public InstantType() {
    super(Instant.class);
  }

  @Override
  public DataType dataType() {
    return DataType.INSTANT;
  }

  @Override
  public boolean nullable() {
    return true;
  }

  @Override
  public boolean isPrimitive() {
    return true;
  }
}
