package move.remoting.codegen;

import java.time.ZonedDateTime;

/**
 *
 */
public class ZonedDateTimeType extends AbstractType {

  public ZonedDateTimeType() {
    super(ZonedDateTime.class);
  }

  @Override
  public DataType dataType() {
    return DataType.DATETIME;
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
