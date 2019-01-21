package java.lang.invoke;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import sun.invoke.util.BytecodeDescriptor;
import sun.invoke.util.Wrapper;

public final class MethodType implements Serializable {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    static final int MAX_JVM_ARITY = 255;
    static final int MAX_MH_ARITY = 254;
    static final int MAX_MH_INVOKER_ARITY = 253;
    static final Class<?>[] NO_PTYPES = new Class[0];
    static final ConcurrentWeakInternSet<MethodType> internTable = new ConcurrentWeakInternSet();
    private static final MethodType[] objectOnlyTypes = new MethodType[20];
    private static final long ptypesOffset;
    private static final long rtypeOffset;
    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[0];
    private static final long serialVersionUID = 292;
    @Stable
    private MethodTypeForm form;
    @Stable
    private String methodDescriptor;
    private final Class<?>[] ptypes;
    private final Class<?> rtype;
    @Stable
    private MethodType wrapAlt;

    private static class ConcurrentWeakInternSet<T> {
        private final ConcurrentMap<WeakEntry<T>, WeakEntry<T>> map = new ConcurrentHashMap();
        private final ReferenceQueue<T> stale = new ReferenceQueue();

        private static class WeakEntry<T> extends WeakReference<T> {
            public final int hashcode;

            public WeakEntry(T key, ReferenceQueue<T> queue) {
                super(key, queue);
                this.hashcode = key.hashCode();
            }

            public WeakEntry(T key) {
                super(key);
                this.hashcode = key.hashCode();
            }

            public boolean equals(Object obj) {
                boolean z = obj instanceof WeakEntry;
                boolean z2 = MethodType.$assertionsDisabled;
                if (!z) {
                    return MethodType.$assertionsDisabled;
                }
                Object that = ((WeakEntry) obj).get();
                Object mine = get();
                if (that != null && mine != null) {
                    z2 = mine.equals(that);
                } else if (this == obj) {
                    z2 = true;
                }
                return z2;
            }

            public int hashCode() {
                return this.hashcode;
            }
        }

        public T get(T elem) {
            if (elem != null) {
                expungeStaleElements();
                WeakEntry<T> value = (WeakEntry) this.map.get(new WeakEntry(elem));
                if (value != null) {
                    T res = value.get();
                    if (res != null) {
                        return res;
                    }
                }
                return null;
            }
            throw new NullPointerException();
        }

        public T add(T elem) {
            if (elem != null) {
                T interned;
                WeakEntry<T> e = new WeakEntry(elem, this.stale);
                do {
                    expungeStaleElements();
                    WeakEntry<T> exist = (WeakEntry) this.map.putIfAbsent(e, e);
                    interned = exist == null ? elem : exist.get();
                } while (interned == null);
                return interned;
            }
            throw new NullPointerException();
        }

        private void expungeStaleElements() {
            while (true) {
                Reference<? extends T> poll = this.stale.poll();
                Reference<? extends T> reference = poll;
                if (poll != null) {
                    this.map.remove(reference);
                } else {
                    return;
                }
            }
        }
    }

    static {
        try {
            rtypeOffset = MethodHandleStatics.UNSAFE.objectFieldOffset(MethodType.class.getDeclaredField("rtype"));
            ptypesOffset = MethodHandleStatics.UNSAFE.objectFieldOffset(MethodType.class.getDeclaredField("ptypes"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private MethodType(Class<?> rtype, Class<?>[] ptypes, boolean trusted) {
        checkRtype(rtype);
        checkPtypes(ptypes);
        this.rtype = rtype;
        this.ptypes = trusted ? ptypes : (Class[]) Arrays.copyOf((Object[]) ptypes, ptypes.length);
    }

    private MethodType(Class<?>[] ptypes, Class<?> rtype) {
        this.rtype = rtype;
        this.ptypes = ptypes;
    }

    MethodTypeForm form() {
        return this.form;
    }

    public Class<?> rtype() {
        return this.rtype;
    }

    public Class<?>[] ptypes() {
        return this.ptypes;
    }

    private static void checkRtype(Class<?> rtype) {
        Objects.requireNonNull(rtype);
    }

    private static void checkPtype(Class<?> ptype) {
        Objects.requireNonNull(ptype);
        if (ptype == Void.TYPE) {
            throw MethodHandleStatics.newIllegalArgumentException("parameter type cannot be void");
        }
    }

    private static int checkPtypes(Class<?>[] ptypes) {
        int slots = 0;
        for (Class<?> ptype : ptypes) {
            checkPtype(ptype);
            if (ptype == Double.TYPE || ptype == Long.TYPE) {
                slots++;
            }
        }
        checkSlotCount(ptypes.length + slots);
        return slots;
    }

    static void checkSlotCount(int count) {
        if ((count & MAX_JVM_ARITY) != count) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bad parameter count ");
            stringBuilder.append(count);
            throw MethodHandleStatics.newIllegalArgumentException(stringBuilder.toString());
        }
    }

    private static IndexOutOfBoundsException newIndexOutOfBoundsException(Object num) {
        if (num instanceof Integer) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bad index: ");
            stringBuilder.append(num);
            num = stringBuilder.toString();
        }
        return new IndexOutOfBoundsException(num.toString());
    }

    public static MethodType methodType(Class<?> rtype, Class<?>[] ptypes) {
        return makeImpl(rtype, ptypes, $assertionsDisabled);
    }

    public static MethodType methodType(Class<?> rtype, List<Class<?>> ptypes) {
        return makeImpl(rtype, listToArray(ptypes), $assertionsDisabled);
    }

    private static Class<?>[] listToArray(List<Class<?>> ptypes) {
        checkSlotCount(ptypes.size());
        return (Class[]) ptypes.toArray(NO_PTYPES);
    }

    public static MethodType methodType(Class<?> rtype, Class<?> ptype0, Class<?>... ptypes) {
        Object ptypes1 = new Class[(ptypes.length + 1)];
        ptypes1[0] = ptype0;
        System.arraycopy((Object) ptypes, 0, ptypes1, 1, ptypes.length);
        return makeImpl(rtype, ptypes1, true);
    }

    public static MethodType methodType(Class<?> rtype) {
        return makeImpl(rtype, NO_PTYPES, true);
    }

    public static MethodType methodType(Class<?> rtype, Class<?> ptype0) {
        return makeImpl(rtype, new Class[]{ptype0}, true);
    }

    public static MethodType methodType(Class<?> rtype, MethodType ptypes) {
        return makeImpl(rtype, ptypes.ptypes, true);
    }

    static MethodType makeImpl(Class<?> rtype, Class<?>[] ptypes, boolean trusted) {
        MethodType mt = (MethodType) internTable.get(new MethodType(ptypes, rtype));
        if (mt != null) {
            return mt;
        }
        if (ptypes.length == 0) {
            ptypes = NO_PTYPES;
            trusted = true;
        }
        mt = new MethodType(rtype, ptypes, trusted);
        mt.form = MethodTypeForm.findForm(mt);
        return (MethodType) internTable.add(mt);
    }

    public static MethodType genericMethodType(int objectArgCount, boolean finalArray) {
        checkSlotCount(objectArgCount);
        boolean ivarargs = finalArray;
        int ootIndex = (objectArgCount * 2) + ivarargs;
        if (ootIndex < objectOnlyTypes.length) {
            MethodType mt = objectOnlyTypes[ootIndex];
            if (mt != null) {
                return mt;
            }
        }
        Object[] ptypes = new Class[(objectArgCount + ivarargs)];
        Arrays.fill(ptypes, (Object) Object.class);
        if (ivarargs) {
            ptypes[objectArgCount] = Object[].class;
        }
        MethodType mt2 = makeImpl(Object.class, ptypes, true);
        if (ootIndex < objectOnlyTypes.length) {
            objectOnlyTypes[ootIndex] = mt2;
        }
        return mt2;
    }

    public static MethodType genericMethodType(int objectArgCount) {
        return genericMethodType(objectArgCount, $assertionsDisabled);
    }

    public MethodType changeParameterType(int num, Class<?> nptype) {
        if (parameterType(num) == nptype) {
            return this;
        }
        checkPtype(nptype);
        Class[] nptypes = (Class[]) this.ptypes.clone();
        nptypes[num] = nptype;
        return makeImpl(this.rtype, nptypes, true);
    }

    public MethodType insertParameterTypes(int num, Class<?>... ptypesToInsert) {
        int len = this.ptypes.length;
        if (num < 0 || num > len) {
            throw newIndexOutOfBoundsException(Integer.valueOf(num));
        }
        checkSlotCount((parameterSlotCount() + ptypesToInsert.length) + checkPtypes(ptypesToInsert));
        int ilen = ptypesToInsert.length;
        if (ilen == 0) {
            return this;
        }
        Object nptypes = (Class[]) Arrays.copyOfRange(this.ptypes, 0, len + ilen);
        System.arraycopy(nptypes, num, nptypes, num + ilen, len - num);
        System.arraycopy((Object) ptypesToInsert, 0, nptypes, num, ilen);
        return makeImpl(this.rtype, nptypes, true);
    }

    public MethodType appendParameterTypes(Class<?>... ptypesToInsert) {
        return insertParameterTypes(parameterCount(), (Class[]) ptypesToInsert);
    }

    public MethodType insertParameterTypes(int num, List<Class<?>> ptypesToInsert) {
        return insertParameterTypes(num, listToArray(ptypesToInsert));
    }

    public MethodType appendParameterTypes(List<Class<?>> ptypesToInsert) {
        return insertParameterTypes(parameterCount(), (List) ptypesToInsert);
    }

    MethodType replaceParameterTypes(int start, int end, Class<?>... ptypesToInsert) {
        if (start == end) {
            return insertParameterTypes(start, (Class[]) ptypesToInsert);
        }
        int len = this.ptypes.length;
        if (start < 0 || start > end || end > len) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("start=");
            stringBuilder.append(start);
            stringBuilder.append(" end=");
            stringBuilder.append(end);
            throw newIndexOutOfBoundsException(stringBuilder.toString());
        } else if (ptypesToInsert.length == 0) {
            return dropParameterTypes(start, end);
        } else {
            return dropParameterTypes(start, end).insertParameterTypes(start, (Class[]) ptypesToInsert);
        }
    }

    MethodType asSpreaderType(Class<?> arrayType, int arrayLength) {
        int spreadPos = this.ptypes.length - arrayLength;
        if (arrayLength == 0) {
            return this;
        }
        if (arrayType == Object[].class) {
            if (isGeneric()) {
                return this;
            }
            if (spreadPos == 0) {
                MethodType res = genericMethodType(arrayLength);
                if (this.rtype != Object.class) {
                    res = res.changeReturnType(this.rtype);
                }
                return res;
            }
        }
        Object elemType = arrayType.getComponentType();
        for (int i = spreadPos; i < this.ptypes.length; i++) {
            if (this.ptypes[i] != elemType) {
                Class[] fixedPtypes = (Class[]) this.ptypes.clone();
                Arrays.fill((Object[]) fixedPtypes, i, this.ptypes.length, elemType);
                return methodType(this.rtype, fixedPtypes);
            }
        }
        return this;
    }

    Class<?> leadingReferenceParameter() {
        if (this.ptypes.length != 0) {
            Class<?> cls = this.ptypes[0];
            Class<?> ptype = cls;
            if (!cls.isPrimitive()) {
                return ptype;
            }
        }
        throw MethodHandleStatics.newIllegalArgumentException("no leading reference parameter");
    }

    MethodType asCollectorType(Class<?> arrayType, int arrayLength) {
        MethodType res;
        if (arrayType == Object[].class) {
            res = genericMethodType(arrayLength);
            if (this.rtype != Object.class) {
                res = res.changeReturnType(this.rtype);
            }
        } else {
            res = methodType(this.rtype, Collections.nCopies(arrayLength, arrayType.getComponentType()));
        }
        if (this.ptypes.length == 1) {
            return res;
        }
        return res.insertParameterTypes(0, parameterList().subList(0, this.ptypes.length - 1));
    }

    public MethodType dropParameterTypes(int start, int end) {
        int len = this.ptypes.length;
        if (start < 0 || start > end || end > len) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("start=");
            stringBuilder.append(start);
            stringBuilder.append(" end=");
            stringBuilder.append(end);
            throw newIndexOutOfBoundsException(stringBuilder.toString());
        } else if (start == end) {
            return this;
        } else {
            Class<?>[] nptypes;
            if (start == 0) {
                if (end == len) {
                    nptypes = NO_PTYPES;
                } else {
                    nptypes = (Class[]) Arrays.copyOfRange(this.ptypes, end, len);
                }
            } else if (end == len) {
                nptypes = (Class[]) Arrays.copyOfRange(this.ptypes, 0, start);
            } else {
                int tail = len - end;
                nptypes = (Class[]) Arrays.copyOfRange(this.ptypes, 0, start + tail);
                System.arraycopy(this.ptypes, end, (Object) nptypes, start, tail);
            }
            return makeImpl(this.rtype, nptypes, true);
        }
    }

    public MethodType changeReturnType(Class<?> nrtype) {
        if (returnType() == nrtype) {
            return this;
        }
        return makeImpl(nrtype, this.ptypes, true);
    }

    public boolean hasPrimitives() {
        return this.form.hasPrimitives();
    }

    public boolean hasWrappers() {
        return unwrap() != this ? true : $assertionsDisabled;
    }

    public MethodType erase() {
        return this.form.erasedType();
    }

    MethodType basicType() {
        return this.form.basicType();
    }

    MethodType invokerType() {
        return insertParameterTypes(0, MethodHandle.class);
    }

    public MethodType generic() {
        return genericMethodType(parameterCount());
    }

    boolean isGeneric() {
        return (this != erase() || hasPrimitives()) ? $assertionsDisabled : true;
    }

    public MethodType wrap() {
        return hasPrimitives() ? wrapWithPrims(this) : this;
    }

    public MethodType unwrap() {
        return unwrapWithNoPrims(!hasPrimitives() ? this : wrapWithPrims(this));
    }

    private static MethodType wrapWithPrims(MethodType pt) {
        MethodType wt = pt.wrapAlt;
        if (wt != null) {
            return wt;
        }
        wt = MethodTypeForm.canonicalize(pt, 2, 2);
        pt.wrapAlt = wt;
        return wt;
    }

    private static MethodType unwrapWithNoPrims(MethodType wt) {
        MethodType uwt = wt.wrapAlt;
        if (uwt == null) {
            uwt = MethodTypeForm.canonicalize(wt, 3, 3);
            if (uwt == null) {
                uwt = wt;
            }
            wt.wrapAlt = uwt;
        }
        return uwt;
    }

    public Class<?> parameterType(int num) {
        return this.ptypes[num];
    }

    public int parameterCount() {
        return this.ptypes.length;
    }

    public Class<?> returnType() {
        return this.rtype;
    }

    public List<Class<?>> parameterList() {
        return Collections.unmodifiableList(Arrays.asList((Class[]) this.ptypes.clone()));
    }

    Class<?> lastParameterType() {
        int len = this.ptypes.length;
        return len == 0 ? Void.TYPE : this.ptypes[len - 1];
    }

    public Class<?>[] parameterArray() {
        return (Class[]) this.ptypes.clone();
    }

    public boolean equals(Object x) {
        return (this == x || ((x instanceof MethodType) && equals((MethodType) x))) ? true : $assertionsDisabled;
    }

    private boolean equals(MethodType that) {
        return (this.rtype == that.rtype && Arrays.equals(this.ptypes, that.ptypes)) ? true : $assertionsDisabled;
    }

    public int hashCode() {
        int hashCode = this.rtype.hashCode() + 31;
        for (Class<?> ptype : this.ptypes) {
            hashCode = (31 * hashCode) + ptype.hashCode();
        }
        return hashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < this.ptypes.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(this.ptypes[i].getSimpleName());
        }
        sb.append(")");
        sb.append(this.rtype.getSimpleName());
        return sb.toString();
    }

    boolean isConvertibleTo(MethodType newType) {
        MethodTypeForm oldForm = form();
        MethodTypeForm newForm = newType.form();
        if (oldForm == newForm) {
            return true;
        }
        if (!canConvert(returnType(), newType.returnType())) {
            return $assertionsDisabled;
        }
        Class<?>[] srcTypes = newType.ptypes;
        Class<?>[] dstTypes = this.ptypes;
        if (srcTypes == dstTypes) {
            return true;
        }
        int length = srcTypes.length;
        int argc = length;
        if (length != dstTypes.length) {
            return $assertionsDisabled;
        }
        if (argc <= 1) {
            if (argc != 1 || canConvert(srcTypes[0], dstTypes[0])) {
                return true;
            }
            return $assertionsDisabled;
        } else if ((oldForm.primitiveParameterCount() == 0 && oldForm.erasedType == this) || (newForm.primitiveParameterCount() == 0 && newForm.erasedType == newType)) {
            return true;
        } else {
            return canConvertParameters(srcTypes, dstTypes);
        }
    }

    boolean explicitCastEquivalentToAsType(MethodType newType) {
        if (this == newType) {
            return true;
        }
        if (!explicitCastEquivalentToAsType(this.rtype, newType.rtype)) {
            return $assertionsDisabled;
        }
        Class<?>[] srcTypes = newType.ptypes;
        Class<?>[] dstTypes = this.ptypes;
        if (dstTypes == srcTypes) {
            return true;
        }
        for (int i = 0; i < dstTypes.length; i++) {
            if (!explicitCastEquivalentToAsType(srcTypes[i], dstTypes[i])) {
                return $assertionsDisabled;
            }
        }
        return true;
    }

    private static boolean explicitCastEquivalentToAsType(Class<?> src, Class<?> dst) {
        boolean z = true;
        if (src == dst || dst == Object.class || dst == Void.TYPE) {
            return true;
        }
        if (src.isPrimitive() && src != Void.TYPE) {
            return canConvert(src, dst);
        }
        if (dst.isPrimitive()) {
            return $assertionsDisabled;
        }
        if (dst.isInterface() && !dst.isAssignableFrom(src)) {
            z = $assertionsDisabled;
        }
        return z;
    }

    private boolean canConvertParameters(Class<?>[] srcTypes, Class<?>[] dstTypes) {
        for (int i = 0; i < srcTypes.length; i++) {
            if (!canConvert(srcTypes[i], dstTypes[i])) {
                return $assertionsDisabled;
            }
        }
        return true;
    }

    static boolean canConvert(Class<?> src, Class<?> dst) {
        if (src == dst || src == Object.class || dst == Object.class) {
            return true;
        }
        if (src.isPrimitive()) {
            if (src == Void.TYPE) {
                return true;
            }
            Wrapper sw = Wrapper.forPrimitiveType(src);
            if (dst.isPrimitive()) {
                return Wrapper.forPrimitiveType(dst).isConvertibleFrom(sw);
            }
            return dst.isAssignableFrom(sw.wrapperType());
        } else if (!dst.isPrimitive() || dst == Void.TYPE) {
            return true;
        } else {
            Wrapper dw = Wrapper.forPrimitiveType(dst);
            if (src.isAssignableFrom(dw.wrapperType())) {
                return true;
            }
            if (Wrapper.isWrapperType(src) && dw.isConvertibleFrom(Wrapper.forWrapperType(src))) {
                return true;
            }
            return $assertionsDisabled;
        }
    }

    int parameterSlotCount() {
        return this.form.parameterSlotCount();
    }

    public static MethodType fromMethodDescriptorString(String descriptor, ClassLoader loader) throws IllegalArgumentException, TypeNotPresentException {
        if (!descriptor.startsWith("(") || descriptor.indexOf(41) < 0 || descriptor.indexOf(46) >= 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("not a method descriptor: ");
            stringBuilder.append(descriptor);
            throw MethodHandleStatics.newIllegalArgumentException(stringBuilder.toString());
        }
        List<Class<?>> types = BytecodeDescriptor.parseMethod(descriptor, loader);
        Class<?> rtype = (Class) types.remove(types.size() - 1);
        checkSlotCount(types.size());
        return makeImpl(rtype, listToArray(types), true);
    }

    public String toMethodDescriptorString() {
        String desc = this.methodDescriptor;
        if (desc != null) {
            return desc;
        }
        desc = BytecodeDescriptor.unparse(this);
        this.methodDescriptor = desc;
        return desc;
    }

    static String toFieldDescriptorString(Class<?> cls) {
        return BytecodeDescriptor.unparse((Class) cls);
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeObject(returnType());
        s.writeObject(parameterArray());
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        Class<?> returnType = (Class) s.readObject();
        Class[] parameterArray = (Class[]) s.readObject();
        checkRtype(returnType);
        checkPtypes(parameterArray);
        MethodType_init(returnType, (Class[]) parameterArray.clone());
    }

    private MethodType() {
        this.rtype = null;
        this.ptypes = null;
    }

    private void MethodType_init(Class<?> rtype, Class<?>[] ptypes) {
        checkRtype(rtype);
        checkPtypes(ptypes);
        MethodHandleStatics.UNSAFE.putObject(this, rtypeOffset, rtype);
        MethodHandleStatics.UNSAFE.putObject(this, ptypesOffset, ptypes);
    }

    private Object readResolve() {
        return methodType(this.rtype, this.ptypes);
    }
}
