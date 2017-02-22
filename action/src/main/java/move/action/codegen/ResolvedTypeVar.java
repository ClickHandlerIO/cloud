package move.action.codegen;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

/**
 * @author Clay Molocznik
 */
public class ResolvedTypeVar {
    private DeclaredType type;
    private TypeElement element;
    private int index;
    private String varName;

    public ResolvedTypeVar(DeclaredType type, TypeElement element, int index, String varName) {
        this.type = type;
        this.element = element;
        this.index = index;
        this.varName = varName;
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
}
