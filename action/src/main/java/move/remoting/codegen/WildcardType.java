package move.remoting.codegen;

/**
 *
 */
public class WildcardType extends AbstractType {

  public WildcardType() {
    super(Object.class);
  }

  @Override
  public DataType dataType() {
    return DataType.WILDCARD;
  }

  @Override
  public boolean nullable() {
    return true;
  }
}
