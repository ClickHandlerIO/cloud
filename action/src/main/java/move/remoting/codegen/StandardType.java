package move.remoting.codegen;

/**
 *
 */
public interface StandardType {

  /**
   * The underlying Java Class.
   *
   * @return Java Class object
   */
  Class javaType();

  /**
   * DataType enum
   *
   * @return DataType value
   */
  DataType dataType();

  /**
   * @return true if nullable, otherwise false
   */
  boolean nullable();

  /**
   * @return true if primitive, otherwise false
   */
  boolean isPrimitive();

  /**
   * @return true if type is a List, Set, or Map. otherwise, false
   */
  boolean isCollection();

  /**
   *
   * @return
   */
  boolean isAbstract();

  /**
   *
   * @return
   */
  boolean isInterface();

  /**
   *
   * @return
   */
  String canonicalName();
}
