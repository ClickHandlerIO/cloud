package io.clickhandler.action.compiler;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Clay Molocznik
 */
public class TypeParameterResolver {
    private final TypeElement element;
    private final List<DeclaredTypeVar> vars = new ArrayList<>();
    private boolean resolved;

    public TypeParameterResolver(TypeElement element) {
        this.element = element;
    }

    public void resolve() {
        if (resolved) {
            return;
        }

        resolveDeclaredTypeVars(element.getSuperclass());

        final List<? extends TypeMirror> interfaces = element.getInterfaces();
        if (interfaces != null && !interfaces.isEmpty()) {
            for (TypeMirror iface : interfaces) {
                resolveDeclaredTypeVars(iface);
            }
        }

        resolved = true;
    }

    public DeclaredTypeVar resolve(Class cls, int typeVarIndex) {
        if (!resolved) resolve();

        for (DeclaredTypeVar var : vars) {
            final ResolvedTypeVar actionTypeVar = var.getResolved(cls.getName());
            if (actionTypeVar != null) {
                if (actionTypeVar.getIndex() == typeVarIndex) {
                    return var;
                }
            }
        }
        return null;
    }

    private void resolveDeclaredTypeVars(TypeMirror type) {
        if (type == null) {
            return;
        }

        if (type.getKind() != TypeKind.DECLARED) {
            return;
        }

        if (type.toString().equals(Object.class.getName())) {
            return;
        }

        if (!(type instanceof DeclaredType)) {
            return;
        }

        final DeclaredType declaredType = (DeclaredType) type;
        final TypeElement element = ((TypeElement) ((DeclaredType) type).asElement());

        if (element.getQualifiedName().toString().equals(Object.class.getName())) {
            return;
        }

        final List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
        if (typeArgs != null && !typeArgs.isEmpty()) {
            for (int i = 0; i < typeArgs.size(); i++) {
                final TypeMirror typeArg = typeArgs.get(i);

                if (typeArg.getKind() == TypeKind.DECLARED) {
                    final List<? extends TypeParameterElement> typeParams =
                        ((TypeElement) ((DeclaredType) type).asElement()).getTypeParameters();
                    final String typeVarName = typeParams.get(i).getSimpleName().toString();

                    final DeclaredTypeVar declaredTypeVar =
                        new DeclaredTypeVar(declaredType, element, i, typeVarName, typeArg);
                    declaredTypeVar.resolve();
                    vars.add(declaredTypeVar);
                }
            }
        }

        resolveDeclaredTypeVars(element.getSuperclass());

        final List<? extends TypeMirror> interfaces = element.getInterfaces();
        if (interfaces != null && !interfaces.isEmpty()) {
            for (TypeMirror iface : interfaces) {
                resolveDeclaredTypeVars(iface);
            }
        }
    }
}
