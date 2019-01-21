package libcore.reflect;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class AnnotationFactory implements InvocationHandler, Serializable {
    private static final transient Map<Class<? extends Annotation>, AnnotationMember[]> cache = new WeakHashMap();
    private AnnotationMember[] elements;
    private final Class<? extends Annotation> klazz;

    /* JADX WARNING: Missing block: B:9:0x0014, code skipped:
            if (r8.isAnnotation() == false) goto L_0x004f;
     */
    /* JADX WARNING: Missing block: B:10:0x0016, code skipped:
            r0 = r8.getDeclaredMethods();
            r1 = new libcore.reflect.AnnotationMember[r0.length];
            r2 = 0;
     */
    /* JADX WARNING: Missing block: B:12:0x001f, code skipped:
            if (r2 >= r0.length) goto L_0x0042;
     */
    /* JADX WARNING: Missing block: B:13:0x0021, code skipped:
            r3 = r0[r2];
            r4 = r3.getName();
            r5 = r3.getReturnType();
     */
    /* JADX WARNING: Missing block: B:15:?, code skipped:
            r1[r2] = new libcore.reflect.AnnotationMember(r4, r3.getDefaultValue(), r5, r3);
     */
    /* JADX WARNING: Missing block: B:16:0x0037, code skipped:
            r6 = move-exception;
     */
    /* JADX WARNING: Missing block: B:17:0x0038, code skipped:
            r1[r2] = new libcore.reflect.AnnotationMember(r4, r6, r5, r3);
     */
    /* JADX WARNING: Missing block: B:19:0x0042, code skipped:
            r2 = cache;
     */
    /* JADX WARNING: Missing block: B:20:0x0044, code skipped:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:22:?, code skipped:
            cache.put(r8, r1);
     */
    /* JADX WARNING: Missing block: B:23:0x004a, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:24:0x004b, code skipped:
            return r1;
     */
    /* JADX WARNING: Missing block: B:28:0x004f, code skipped:
            r1 = new java.lang.StringBuilder();
            r1.append("Type is not annotation: ");
            r1.append(r8.getName());
     */
    /* JADX WARNING: Missing block: B:29:0x0069, code skipped:
            throw new java.lang.IllegalArgumentException(r1.toString());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static AnnotationMember[] getElementsDescription(Class<? extends Annotation> annotationType) {
        synchronized (cache) {
            AnnotationMember[] desc = (AnnotationMember[]) cache.get(annotationType);
            if (desc != null) {
                return desc;
            }
        }
        int i++;
    }

    public static <A extends Annotation> A createAnnotation(Class<? extends Annotation> annotationType, AnnotationMember[] elements) {
        AnnotationFactory factory = new AnnotationFactory(annotationType, elements);
        return (Annotation) Proxy.newProxyInstance(annotationType.getClassLoader(), new Class[]{annotationType}, factory);
    }

    private AnnotationFactory(Class<? extends Annotation> klzz, AnnotationMember[] values) {
        this.klazz = klzz;
        AnnotationMember[] defs = getElementsDescription(this.klazz);
        if (values == null) {
            this.elements = defs;
            return;
        }
        this.elements = new AnnotationMember[defs.length];
        for (int i = this.elements.length - 1; i >= 0; i--) {
            for (AnnotationMember val : values) {
                if (val.name.equals(defs[i].name)) {
                    this.elements[i] = val.setDefinition(defs[i]);
                    break;
                }
            }
            this.elements[i] = defs[i];
        }
    }

    private void readObject(ObjectInputStream os) throws IOException, ClassNotFoundException {
        os.defaultReadObject();
        AnnotationMember[] defs = getElementsDescription(this.klazz);
        AnnotationMember[] old = this.elements;
        List<AnnotationMember> merged = new ArrayList(defs.length + old.length);
        for (AnnotationMember el1 : old) {
            for (AnnotationMember el2 : defs) {
                if (el2.name.equals(el1.name)) {
                    break;
                }
            }
            merged.add(el1);
        }
        for (AnnotationMember el12 : defs) {
            for (AnnotationMember el22 : old) {
                if (el22.name.equals(el12.name)) {
                    merged.add(el22.setDefinition(el12));
                    break;
                }
            }
            merged.add(el12);
        }
        this.elements = (AnnotationMember[]) merged.toArray(new AnnotationMember[merged.size()]);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!this.klazz.isInstance(obj)) {
            return false;
        }
        int length;
        if (Proxy.isProxyClass(obj.getClass())) {
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(obj);
            InvocationHandler handler = invocationHandler;
            if (invocationHandler instanceof AnnotationFactory) {
                AnnotationFactory other = (AnnotationFactory) handler;
                if (this.elements.length != other.elements.length) {
                    return false;
                }
                AnnotationMember[] annotationMemberArr = this.elements;
                length = annotationMemberArr.length;
                int i = 0;
                while (i < length) {
                    AnnotationMember el1 = annotationMemberArr[i];
                    AnnotationMember[] annotationMemberArr2 = other.elements;
                    int length2 = annotationMemberArr2.length;
                    int i2 = 0;
                    while (i2 < length2) {
                        if (el1.equals(annotationMemberArr2[i2])) {
                            i++;
                        } else {
                            i2++;
                        }
                    }
                    return false;
                }
                return true;
            }
        }
        AnnotationMember[] annotationMemberArr3 = this.elements;
        int length3 = annotationMemberArr3.length;
        length = 0;
        while (length < length3) {
            AnnotationMember el = annotationMemberArr3[length];
            if (el.tag == '!') {
                return false;
            }
            try {
                if (!el.definingMethod.isAccessible()) {
                    el.definingMethod.setAccessible(true);
                }
                Object otherValue = el.definingMethod.invoke(obj, new Object[0]);
                if (otherValue != null) {
                    if (el.tag == '[') {
                        if (!el.equalArrayValue(otherValue)) {
                            return false;
                        }
                    } else if (!el.value.equals(otherValue)) {
                        return false;
                    }
                } else if (el.value != AnnotationMember.NO_VALUE) {
                    return false;
                }
                length++;
            } catch (Throwable th) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int hash = 0;
        for (AnnotationMember element : this.elements) {
            hash += element.hashCode();
        }
        return hash;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append('@');
        result.append(this.klazz.getName());
        result.append('(');
        for (int i = 0; i < this.elements.length; i++) {
            if (i != 0) {
                result.append(", ");
            }
            result.append(this.elements[i]);
        }
        result.append(')');
        return result.toString();
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        Class[] params = method.getParameterTypes();
        int i = 0;
        if (params.length == 0) {
            if ("annotationType".equals(name)) {
                return this.klazz;
            }
            if ("toString".equals(name)) {
                return toString();
            }
            if ("hashCode".equals(name)) {
                return Integer.valueOf(hashCode());
            }
            AnnotationMember element = null;
            AnnotationMember[] annotationMemberArr = this.elements;
            int length = annotationMemberArr.length;
            while (i < length) {
                AnnotationMember el = annotationMemberArr[i];
                if (name.equals(el.name)) {
                    element = el;
                    break;
                }
                i++;
            }
            if (element == null || !method.equals(element.definingMethod)) {
                throw new IllegalArgumentException(method.toString());
            }
            Object value = element.validateValue();
            if (value != null) {
                return value;
            }
            throw new IncompleteAnnotationException(this.klazz, name);
        } else if (params.length == 1 && params[0] == Object.class && "equals".equals(name)) {
            return Boolean.valueOf(equals(args[0]));
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid method for annotation type: ");
            stringBuilder.append(method);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }
}
