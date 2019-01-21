package libcore.reflect;

import java.lang.reflect.AbstractMethod;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

public final class TypeVariableImpl<D extends GenericDeclaration> implements TypeVariable<D> {
    private ListOfTypes bounds;
    private final GenericDeclaration declOfVarUser;
    private TypeVariableImpl<D> formalVar;
    private D genericDeclaration;
    private final String name;

    public boolean equals(Object o) {
        boolean z = false;
        if (!(o instanceof TypeVariable)) {
            return false;
        }
        TypeVariable<?> that = (TypeVariable) o;
        if (getName().equals(that.getName()) && getGenericDeclaration().equals(that.getGenericDeclaration())) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return (31 * getName().hashCode()) + getGenericDeclaration().hashCode();
    }

    TypeVariableImpl(D genericDecl, String name, ListOfTypes bounds) {
        this.genericDeclaration = genericDecl;
        this.name = name;
        this.bounds = bounds;
        this.formalVar = this;
        this.declOfVarUser = null;
    }

    TypeVariableImpl(D genericDecl, String name) {
        this.name = name;
        this.declOfVarUser = genericDecl;
    }

    static TypeVariable findFormalVar(GenericDeclaration layer, String name) {
        for (TypeVariable var : layer.getTypeParameters()) {
            if (name.equals(var.getName())) {
                return var;
            }
        }
        return null;
    }

    private static GenericDeclaration nextLayer(GenericDeclaration decl) {
        if (decl instanceof Class) {
            Class cl = (Class) decl;
            AbstractMethod m = cl.getEnclosingMethod();
            Constructor decl2 = m != null ? m : cl.getEnclosingConstructor();
            if (decl2 != null) {
                return decl2;
            }
            return cl.getEnclosingClass();
        } else if (decl instanceof Method) {
            return ((Method) decl).getDeclaringClass();
        } else {
            if (decl instanceof Constructor) {
                return ((Constructor) decl).getDeclaringClass();
            }
            throw new AssertionError();
        }
    }

    void resolve() {
        if (this.formalVar == null) {
            GenericDeclaration curLayer = this.declOfVarUser;
            while (true) {
                TypeVariable findFormalVar = findFormalVar(curLayer, this.name);
                TypeVariable var = findFormalVar;
                if (findFormalVar == null) {
                    curLayer = nextLayer(curLayer);
                    if (curLayer == null) {
                        throw new AssertionError("illegal type variable reference");
                    }
                } else {
                    this.formalVar = (TypeVariableImpl) var;
                    this.genericDeclaration = this.formalVar.genericDeclaration;
                    this.bounds = this.formalVar.bounds;
                    return;
                }
            }
        }
    }

    public Type[] getBounds() {
        resolve();
        return (Type[]) this.bounds.getResolvedTypes().clone();
    }

    public D getGenericDeclaration() {
        resolve();
        return this.genericDeclaration;
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        return this.name;
    }
}
