package move.action.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Resolves a single DECLARED Type Variable.
 * <p/>
 * Searches down the superclass, interface, and superinterface paths.
 *
 * @author Clay Molocznik
 */
public class DeclaredTypeVar {

  private final Map<String, ResolvedTypeVar> resolvedMap = new HashMap<>();
  private DeclaredType type;
  private TypeElement element;
  private int index;
  private String varName;
  private TypeMirror varType;
  private TypeElement varTypeElement;

  public DeclaredTypeVar(DeclaredType type, TypeElement element, int index, String varName,
      TypeMirror varType) {
    this.type = type;
    this.element = element;
    this.index = index;
    this.varName = varName;
    this.varType = varType;

    if (varType instanceof DeclaredType) {
      this.varTypeElement = (TypeElement) ((DeclaredType) varType).asElement();
    }
  }

  public DeclaredType getType() {
    return type;
  }

  public TypeElement getElement() {
    return element;
  }

  public int getIndex() {
    return index;
  }

  public String getVarName() {
    return varName;
  }

  public TypeMirror getResolvedType() {
    return varType;
  }

  public TypeElement getResolvedElement() {
    return varTypeElement;
  }

  public ResolvedTypeVar getResolved(String name) {
    return resolvedMap.get(name);
  }

  public void resolve() {
    // Find 'varName' on the SuperClass or any Interfaces.
    final TypeMirror superClass = element.getSuperclass();

    if (superClass != null) {
      search(varName, superClass);
    }
  }

  private void search(String varName, TypeMirror mirror) {
    if (mirror == null || !(mirror instanceof DeclaredType)) {
      return;
    }

    final DeclaredType type = (DeclaredType) mirror;
    final TypeElement element = (TypeElement) type.asElement();
    final List<? extends TypeMirror> typeArgs = type.getTypeArguments();
    if (typeArgs == null || typeArgs.isEmpty()) {
      return;
    }

    for (int i = 0; i < typeArgs.size(); i++) {
      final TypeMirror typeArg = typeArgs.get(i);

      if (typeArg.getKind() == TypeKind.TYPEVAR
          && typeArg.toString().equals(varName)) {
        // Found it.
        // Get TypeVariable name.
        varName = element.getTypeParameters().get(i).getSimpleName().toString();

        // Add to resolved map.
        resolvedMap.put(element.toString(), new ResolvedTypeVar(type, element, i, varName));

        // Go up to the SuperClass.
        search(varName, element.getSuperclass());

        final List<? extends TypeMirror> interfaces = element.getInterfaces();
        if (interfaces != null && !interfaces.isEmpty()) {
          for (TypeMirror iface : interfaces) {
            search(varName, iface);
          }
        }
      }
    }
  }
}
