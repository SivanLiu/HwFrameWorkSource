package java.io;

import dalvik.system.VMRuntime;
import dalvik.system.VMStack;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import sun.misc.Unsafe;
import sun.reflect.CallerSensitive;
import sun.reflect.misc.ReflectUtil;

public class ObjectStreamClass implements Serializable {
    static final int MAX_SDK_TARGET_FOR_CLINIT_UIDGEN_WORKAROUND = 23;
    public static final ObjectStreamField[] NO_FIELDS = new ObjectStreamField[0];
    private static final ObjectStreamField[] serialPersistentFields = NO_FIELDS;
    private static final long serialVersionUID = -6120832682080437368L;
    private Class<?> cl;
    private Constructor<?> cons;
    private volatile ClassDataSlot[] dataLayout;
    private ExceptionInfo defaultSerializeEx;
    private ExceptionInfo deserializeEx;
    private boolean externalizable;
    private FieldReflector fieldRefl;
    private ObjectStreamField[] fields;
    private boolean hasBlockExternalData = true;
    private boolean hasWriteObjectData;
    private boolean initialized;
    private boolean isEnum;
    private boolean isProxy;
    private ObjectStreamClass localDesc;
    private String name;
    private int numObjFields;
    private int primDataSize;
    private Method readObjectMethod;
    private Method readObjectNoDataMethod;
    private Method readResolveMethod;
    private ClassNotFoundException resolveEx;
    private boolean serializable;
    private ExceptionInfo serializeEx;
    private volatile Long suid;
    private ObjectStreamClass superDesc;
    private Method writeObjectMethod;
    private Method writeReplaceMethod;

    private static class Caches {
        static final ConcurrentMap<WeakClassKey, Reference<?>> localDescs = new ConcurrentHashMap();
        private static final ReferenceQueue<Class<?>> localDescsQueue = new ReferenceQueue();
        static final ConcurrentMap<FieldReflectorKey, Reference<?>> reflectors = new ConcurrentHashMap();
        private static final ReferenceQueue<Class<?>> reflectorsQueue = new ReferenceQueue();

        private Caches() {
        }
    }

    static class ClassDataSlot {
        final ObjectStreamClass desc;
        final boolean hasData;

        ClassDataSlot(ObjectStreamClass desc, boolean hasData) {
            this.desc = desc;
            this.hasData = hasData;
        }
    }

    private static class EntryFuture {
        private static final Object unset = new Object();
        private Object entry;
        private final Thread owner;

        private EntryFuture() {
            this.owner = Thread.currentThread();
            this.entry = unset;
        }

        /* synthetic */ EntryFuture(AnonymousClass1 x0) {
            this();
        }

        synchronized boolean set(Object entry) {
            if (this.entry != unset) {
                return false;
            }
            this.entry = entry;
            notifyAll();
            return true;
        }

        synchronized Object get() {
            boolean interrupted = false;
            while (this.entry == unset) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                });
            }
            return this.entry;
        }

        Thread getOwner() {
            return this.owner;
        }
    }

    private static class ExceptionInfo {
        private final String className;
        private final String message;

        ExceptionInfo(String cn, String msg) {
            this.className = cn;
            this.message = msg;
        }

        InvalidClassException newInvalidClassException() {
            return new InvalidClassException(this.className, this.message);
        }
    }

    private static class FieldReflector {
        private static final Unsafe unsafe = Unsafe.getUnsafe();
        private final ObjectStreamField[] fields;
        private final int numPrimFields;
        private final int[] offsets;
        private final long[] readKeys;
        private final char[] typeCodes;
        private final Class<?>[] types;
        private final long[] writeKeys;

        FieldReflector(ObjectStreamField[] fields) {
            this.fields = fields;
            int nfields = fields.length;
            this.readKeys = new long[nfields];
            this.writeKeys = new long[nfields];
            this.offsets = new int[nfields];
            this.typeCodes = new char[nfields];
            ArrayList<Class<?>> typeList = new ArrayList();
            Set<Long> usedKeys = new HashSet();
            for (int i = 0; i < nfields; i++) {
                ObjectStreamField f = fields[i];
                Field rf = f.getField();
                long j = -1;
                long key = rf != null ? unsafe.objectFieldOffset(rf) : -1;
                this.readKeys[i] = key;
                long[] jArr = this.writeKeys;
                if (usedKeys.add(Long.valueOf(key))) {
                    j = key;
                }
                jArr[i] = j;
                this.offsets[i] = f.getOffset();
                this.typeCodes[i] = f.getTypeCode();
                if (!f.isPrimitive()) {
                    typeList.add(rf != null ? rf.getType() : null);
                }
            }
            this.types = (Class[]) typeList.toArray(new Class[typeList.size()]);
            this.numPrimFields = nfields - this.types.length;
        }

        ObjectStreamField[] getFields() {
            return this.fields;
        }

        void getPrimFieldValues(Object obj, byte[] buf) {
            if (obj != null) {
                for (int i = 0; i < this.numPrimFields; i++) {
                    long key = this.readKeys[i];
                    int off = this.offsets[i];
                    char c = this.typeCodes[i];
                    if (c == 'F') {
                        Bits.putFloat(buf, off, unsafe.getFloat(obj, key));
                    } else if (c == 'S') {
                        Bits.putShort(buf, off, unsafe.getShort(obj, key));
                    } else if (c != 'Z') {
                        switch (c) {
                            case 'B':
                                buf[off] = unsafe.getByte(obj, key);
                                break;
                            case 'C':
                                Bits.putChar(buf, off, unsafe.getChar(obj, key));
                                break;
                            case 'D':
                                Bits.putDouble(buf, off, unsafe.getDouble(obj, key));
                                break;
                            default:
                                switch (c) {
                                    case 'I':
                                        Bits.putInt(buf, off, unsafe.getInt(obj, key));
                                        break;
                                    case 'J':
                                        Bits.putLong(buf, off, unsafe.getLong(obj, key));
                                        break;
                                    default:
                                        throw new InternalError();
                                }
                        }
                    } else {
                        Bits.putBoolean(buf, off, unsafe.getBoolean(obj, key));
                    }
                }
                return;
            }
            throw new NullPointerException();
        }

        void setPrimFieldValues(Object obj, byte[] buf) {
            if (obj != null) {
                for (int i = 0; i < this.numPrimFields; i++) {
                    long key = this.writeKeys[i];
                    if (key != -1) {
                        int off = this.offsets[i];
                        char c = this.typeCodes[i];
                        if (c == 'F') {
                            unsafe.putFloat(obj, key, Bits.getFloat(buf, off));
                        } else if (c == 'S') {
                            unsafe.putShort(obj, key, Bits.getShort(buf, off));
                        } else if (c != 'Z') {
                            switch (c) {
                                case 'B':
                                    unsafe.putByte(obj, key, buf[off]);
                                    break;
                                case 'C':
                                    unsafe.putChar(obj, key, Bits.getChar(buf, off));
                                    break;
                                case 'D':
                                    unsafe.putDouble(obj, key, Bits.getDouble(buf, off));
                                    break;
                                default:
                                    switch (c) {
                                        case 'I':
                                            unsafe.putInt(obj, key, Bits.getInt(buf, off));
                                            break;
                                        case 'J':
                                            unsafe.putLong(obj, key, Bits.getLong(buf, off));
                                            break;
                                        default:
                                            throw new InternalError();
                                    }
                            }
                        } else {
                            unsafe.putBoolean(obj, key, Bits.getBoolean(buf, off));
                        }
                    }
                }
                return;
            }
            throw new NullPointerException();
        }

        void getObjFieldValues(Object obj, Object[] vals) {
            if (obj != null) {
                int i = this.numPrimFields;
                while (i < this.fields.length) {
                    char c = this.typeCodes[i];
                    if (c == 'L' || c == '[') {
                        vals[this.offsets[i]] = unsafe.getObject(obj, this.readKeys[i]);
                        i++;
                    } else {
                        throw new InternalError();
                    }
                }
                return;
            }
            throw new NullPointerException();
        }

        void setObjFieldValues(Object obj, Object[] vals) {
            if (obj != null) {
                int i = this.numPrimFields;
                while (i < this.fields.length) {
                    long key = this.writeKeys[i];
                    if (key != -1) {
                        char c = this.typeCodes[i];
                        if (c == 'L' || c == '[') {
                            Object val = vals[this.offsets[i]];
                            if (val == null || this.types[i - this.numPrimFields].isInstance(val)) {
                                unsafe.putObject(obj, key, val);
                            } else {
                                Field f = this.fields[i].getField();
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("cannot assign instance of ");
                                stringBuilder.append(val.getClass().getName());
                                stringBuilder.append(" to field ");
                                stringBuilder.append(f.getDeclaringClass().getName());
                                stringBuilder.append(".");
                                stringBuilder.append(f.getName());
                                stringBuilder.append(" of type ");
                                stringBuilder.append(f.getType().getName());
                                stringBuilder.append(" in instance of ");
                                stringBuilder.append(obj.getClass().getName());
                                throw new ClassCastException(stringBuilder.toString());
                            }
                        }
                        throw new InternalError();
                    }
                    i++;
                }
                return;
            }
            throw new NullPointerException();
        }
    }

    private static class MemberSignature {
        public final Member member;
        public final String name;
        public final String signature;

        public MemberSignature(Field field) {
            this.member = field;
            this.name = field.getName();
            this.signature = ObjectStreamClass.getClassSignature(field.getType());
        }

        public MemberSignature(Constructor<?> cons) {
            this.member = cons;
            this.name = cons.getName();
            this.signature = ObjectStreamClass.getMethodSignature(cons.getParameterTypes(), Void.TYPE);
        }

        public MemberSignature(Method meth) {
            this.member = meth;
            this.name = meth.getName();
            this.signature = ObjectStreamClass.getMethodSignature(meth.getParameterTypes(), meth.getReturnType());
        }
    }

    private static class FieldReflectorKey extends WeakReference<Class<?>> {
        private final int hash;
        private final boolean nullClass;
        private final String sigs;

        FieldReflectorKey(Class<?> cl, ObjectStreamField[] fields, ReferenceQueue<Class<?>> queue) {
            super(cl, queue);
            int i = 0;
            this.nullClass = cl == null;
            StringBuilder sbuf = new StringBuilder();
            while (i < fields.length) {
                ObjectStreamField f = fields[i];
                sbuf.append(f.getName());
                sbuf.append(f.getSignature());
                i++;
            }
            this.sigs = sbuf.toString();
            this.hash = System.identityHashCode(cl) + this.sigs.hashCode();
        }

        public int hashCode() {
            return this.hash;
        }

        /* JADX WARNING: Missing block: B:8:0x0012, code skipped:
            if (r1.nullClass != false) goto L_0x0024;
     */
        /* JADX WARNING: Missing block: B:12:0x0022, code skipped:
            if (r4 == r1.get()) goto L_0x0024;
     */
        /* JADX WARNING: Missing block: B:14:0x002c, code skipped:
            if (r5.sigs.equals(r1.sigs) != false) goto L_0x0030;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean equals(Object obj) {
            boolean z = true;
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof FieldReflectorKey)) {
                return false;
            }
            FieldReflectorKey other = (FieldReflectorKey) obj;
            if (!this.nullClass) {
                Class<?> cls = (Class) get();
                Class<?> referent = cls;
                if (cls != null) {
                }
                z = false;
                return z;
            }
        }
    }

    static class WeakClassKey extends WeakReference<Class<?>> {
        private final int hash;

        WeakClassKey(Class<?> cl, ReferenceQueue<Class<?>> refQueue) {
            super(cl, refQueue);
            this.hash = System.identityHashCode(cl);
        }

        public int hashCode() {
            return this.hash;
        }

        public boolean equals(Object obj) {
            boolean z = true;
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof WeakClassKey)) {
                return false;
            }
            Object referent = get();
            if (referent == null || referent != ((WeakClassKey) obj).get()) {
                z = false;
            }
            return z;
        }
    }

    private static native boolean hasStaticInitializer(Class<?> cls, boolean z);

    public static ObjectStreamClass lookup(Class<?> cl) {
        return lookup(cl, false);
    }

    public static ObjectStreamClass lookupAny(Class<?> cl) {
        return lookup(cl, true);
    }

    public String getName() {
        return this.name;
    }

    public long getSerialVersionUID() {
        if (this.suid == null) {
            this.suid = (Long) AccessController.doPrivileged(new PrivilegedAction<Long>() {
                public Long run() {
                    return Long.valueOf(ObjectStreamClass.computeDefaultSUID(ObjectStreamClass.this.cl));
                }
            });
        }
        return this.suid.longValue();
    }

    @CallerSensitive
    public Class<?> forClass() {
        if (this.cl == null) {
            return null;
        }
        requireInitialized();
        if (System.getSecurityManager() != null && ReflectUtil.needsPackageAccessCheck(VMStack.getCallingClassLoader(), this.cl.getClassLoader())) {
            ReflectUtil.checkPackageAccess(this.cl);
        }
        return this.cl;
    }

    public ObjectStreamField[] getFields() {
        return getFields(true);
    }

    public ObjectStreamField getField(String name) {
        return getField(name, null);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.name);
        stringBuilder.append(": static final long serialVersionUID = ");
        stringBuilder.append(getSerialVersionUID());
        stringBuilder.append("L;");
        return stringBuilder.toString();
    }

    static ObjectStreamClass lookup(Class<?> cl, boolean all) {
        if (!all && !Serializable.class.isAssignableFrom(cl)) {
            return null;
        }
        processQueue(Caches.localDescsQueue, Caches.localDescs);
        WeakClassKey key = new WeakClassKey(cl, Caches.localDescsQueue);
        Reference<?> ref = (Reference) Caches.localDescs.get(key);
        Object entry = null;
        if (ref != null) {
            entry = ref.get();
        }
        EntryFuture future = null;
        if (entry == null) {
            EntryFuture newEntry = new EntryFuture();
            Reference<?> newRef = new SoftReference(newEntry);
            do {
                if (ref != null) {
                    Caches.localDescs.remove(key, ref);
                }
                ref = (Reference) Caches.localDescs.putIfAbsent(key, newRef);
                if (ref != null) {
                    entry = ref.get();
                }
                if (ref == null) {
                    break;
                }
            } while (entry == null);
            if (entry == null) {
                future = newEntry;
            }
        }
        if (entry instanceof ObjectStreamClass) {
            return (ObjectStreamClass) entry;
        }
        if (entry instanceof EntryFuture) {
            future = (EntryFuture) entry;
            if (future.getOwner() == Thread.currentThread()) {
                entry = null;
            } else {
                entry = future.get();
            }
        }
        if (entry == null) {
            Throwable th;
            try {
                th = new ObjectStreamClass(cl);
            } catch (Throwable th2) {
                th = th2;
            }
            entry = th;
            if (future.set(entry)) {
                Caches.localDescs.put(key, new SoftReference(entry));
            } else {
                entry = future.get();
            }
        }
        if (entry instanceof ObjectStreamClass) {
            return (ObjectStreamClass) entry;
        }
        if (entry instanceof RuntimeException) {
            throw ((RuntimeException) entry);
        } else if (entry instanceof Error) {
            throw ((Error) entry);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unexpected entry: ");
            stringBuilder.append(entry);
            throw new InternalError(stringBuilder.toString());
        }
    }

    private ObjectStreamClass(final Class<?> cl) {
        this.cl = cl;
        this.name = cl.getName();
        this.isProxy = Proxy.isProxyClass(cl);
        this.isEnum = Enum.class.isAssignableFrom(cl);
        this.serializable = Serializable.class.isAssignableFrom(cl);
        this.externalizable = Externalizable.class.isAssignableFrom(cl);
        Class<?> superCl = cl.getSuperclass();
        int i = 0;
        this.superDesc = superCl != null ? lookup(superCl, false) : null;
        this.localDesc = this;
        if (this.serializable) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    if (ObjectStreamClass.this.isEnum) {
                        ObjectStreamClass.this.suid = Long.valueOf(0);
                        ObjectStreamClass.this.fields = ObjectStreamClass.NO_FIELDS;
                        return null;
                    } else if (cl.isArray()) {
                        ObjectStreamClass.this.fields = ObjectStreamClass.NO_FIELDS;
                        return null;
                    } else {
                        ObjectStreamClass.this.suid = ObjectStreamClass.getDeclaredSUID(cl);
                        try {
                            ObjectStreamClass.this.fields = ObjectStreamClass.getSerialFields(cl);
                            ObjectStreamClass.this.computeFieldOffsets();
                        } catch (InvalidClassException e) {
                            ObjectStreamClass.this.serializeEx = ObjectStreamClass.this.deserializeEx = new ExceptionInfo(e.classname, e.getMessage());
                            ObjectStreamClass.this.fields = ObjectStreamClass.NO_FIELDS;
                        }
                        if (ObjectStreamClass.this.externalizable) {
                            ObjectStreamClass.this.cons = ObjectStreamClass.getExternalizableConstructor(cl);
                        } else {
                            ObjectStreamClass.this.cons = ObjectStreamClass.getSerializableConstructor(cl);
                            boolean z = true;
                            ObjectStreamClass.this.writeObjectMethod = ObjectStreamClass.getPrivateMethod(cl, "writeObject", new Class[]{ObjectOutputStream.class}, Void.TYPE);
                            ObjectStreamClass.this.readObjectMethod = ObjectStreamClass.getPrivateMethod(cl, "readObject", new Class[]{ObjectInputStream.class}, Void.TYPE);
                            ObjectStreamClass.this.readObjectNoDataMethod = ObjectStreamClass.getPrivateMethod(cl, "readObjectNoData", null, Void.TYPE);
                            ObjectStreamClass objectStreamClass = ObjectStreamClass.this;
                            if (ObjectStreamClass.this.writeObjectMethod == null) {
                                z = false;
                            }
                            objectStreamClass.hasWriteObjectData = z;
                        }
                        ObjectStreamClass.this.writeReplaceMethod = ObjectStreamClass.getInheritableMethod(cl, "writeReplace", null, Object.class);
                        ObjectStreamClass.this.readResolveMethod = ObjectStreamClass.getInheritableMethod(cl, "readResolve", null, Object.class);
                        return null;
                    }
                }
            });
        } else {
            this.suid = Long.valueOf(0);
            this.fields = NO_FIELDS;
        }
        try {
            this.fieldRefl = getReflector(this.fields, this);
            if (this.deserializeEx == null) {
                if (this.isEnum) {
                    this.deserializeEx = new ExceptionInfo(this.name, "enum type");
                } else if (this.cons == null) {
                    this.deserializeEx = new ExceptionInfo(this.name, "no valid constructor");
                }
            }
            while (i < this.fields.length) {
                if (this.fields[i].getField() == null) {
                    this.defaultSerializeEx = new ExceptionInfo(this.name, "unmatched serializable field(s) declared");
                }
                i++;
            }
            this.initialized = true;
        } catch (InvalidClassException ex) {
            throw new InternalError(ex);
        }
    }

    ObjectStreamClass() {
    }

    void initProxy(Class<?> cl, ClassNotFoundException resolveEx, ObjectStreamClass superDesc) throws InvalidClassException {
        ObjectStreamClass osc = null;
        if (cl != null) {
            osc = lookup(cl, true);
            if (!osc.isProxy) {
                throw new InvalidClassException("cannot bind proxy descriptor to a non-proxy class");
            }
        }
        this.cl = cl;
        this.resolveEx = resolveEx;
        this.superDesc = superDesc;
        this.isProxy = true;
        this.serializable = true;
        this.suid = Long.valueOf(0);
        this.fields = NO_FIELDS;
        if (osc != null) {
            this.localDesc = osc;
            this.name = this.localDesc.name;
            this.externalizable = this.localDesc.externalizable;
            this.writeReplaceMethod = this.localDesc.writeReplaceMethod;
            this.readResolveMethod = this.localDesc.readResolveMethod;
            this.deserializeEx = this.localDesc.deserializeEx;
            this.cons = this.localDesc.cons;
        }
        this.fieldRefl = getReflector(this.fields, this.localDesc);
        this.initialized = true;
    }

    void initNonProxy(ObjectStreamClass model, Class<?> cl, ClassNotFoundException resolveEx, ObjectStreamClass superDesc) throws InvalidClassException {
        long suid = Long.valueOf(model.getSerialVersionUID()).longValue();
        ObjectStreamClass osc = null;
        if (cl != null) {
            osc = lookup(cl, true);
            String str;
            StringBuilder stringBuilder;
            if (osc.isProxy) {
                throw new InvalidClassException("cannot bind non-proxy descriptor to a proxy class");
            } else if (model.isEnum != osc.isEnum) {
                throw new InvalidClassException(model.isEnum ? "cannot bind enum descriptor to a non-enum class" : "cannot bind non-enum descriptor to an enum class");
            } else if (model.serializable == osc.serializable && !cl.isArray() && suid != osc.getSerialVersionUID()) {
                str = osc.name;
                stringBuilder = new StringBuilder();
                stringBuilder.append("local class incompatible: stream classdesc serialVersionUID = ");
                stringBuilder.append(suid);
                stringBuilder.append(", local class serialVersionUID = ");
                stringBuilder.append(osc.getSerialVersionUID());
                throw new InvalidClassException(str, stringBuilder.toString());
            } else if (!classNamesEqual(model.name, osc.name)) {
                str = osc.name;
                stringBuilder = new StringBuilder();
                stringBuilder.append("local class name incompatible with stream class name \"");
                stringBuilder.append(model.name);
                stringBuilder.append("\"");
                throw new InvalidClassException(str, stringBuilder.toString());
            } else if (!model.isEnum) {
                if (model.serializable == osc.serializable && model.externalizable != osc.externalizable) {
                    throw new InvalidClassException(osc.name, "Serializable incompatible with Externalizable");
                } else if (!(model.serializable == osc.serializable && model.externalizable == osc.externalizable && (model.serializable || model.externalizable))) {
                    this.deserializeEx = new ExceptionInfo(osc.name, "class invalid for deserialization");
                }
            }
        }
        this.cl = cl;
        this.resolveEx = resolveEx;
        this.superDesc = superDesc;
        this.name = model.name;
        this.suid = Long.valueOf(suid);
        this.isProxy = false;
        this.isEnum = model.isEnum;
        this.serializable = model.serializable;
        this.externalizable = model.externalizable;
        this.hasBlockExternalData = model.hasBlockExternalData;
        this.hasWriteObjectData = model.hasWriteObjectData;
        this.fields = model.fields;
        this.primDataSize = model.primDataSize;
        this.numObjFields = model.numObjFields;
        if (osc != null) {
            this.localDesc = osc;
            this.writeObjectMethod = this.localDesc.writeObjectMethod;
            this.readObjectMethod = this.localDesc.readObjectMethod;
            this.readObjectNoDataMethod = this.localDesc.readObjectNoDataMethod;
            this.writeReplaceMethod = this.localDesc.writeReplaceMethod;
            this.readResolveMethod = this.localDesc.readResolveMethod;
            if (this.deserializeEx == null) {
                this.deserializeEx = this.localDesc.deserializeEx;
            }
            this.cons = this.localDesc.cons;
        }
        this.fieldRefl = getReflector(this.fields, this.localDesc);
        this.fields = this.fieldRefl.getFields();
        this.initialized = true;
    }

    void readNonProxy(ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.name = in.readUTF();
        this.suid = Long.valueOf(in.readLong());
        this.isProxy = false;
        byte flags = in.readByte();
        this.hasWriteObjectData = (flags & 1) != 0;
        this.hasBlockExternalData = (flags & 8) != 0;
        this.externalizable = (flags & 4) != 0;
        boolean sflag = (flags & 2) != 0;
        if (this.externalizable && sflag) {
            throw new InvalidClassException(this.name, "serializable and externalizable flags conflict");
        }
        boolean z = this.externalizable || sflag;
        this.serializable = z;
        this.isEnum = (flags & 16) != 0;
        String str;
        if (!this.isEnum || this.suid.longValue() == 0) {
            int numFields = in.readShort();
            if (!this.isEnum || numFields == 0) {
                this.fields = numFields > 0 ? new ObjectStreamField[numFields] : NO_FIELDS;
                int i = 0;
                while (i < numFields) {
                    char tcode = (char) in.readByte();
                    String fname = in.readUTF();
                    String signature = (tcode == 'L' || tcode == '[') ? in.readTypeString() : new String(new char[]{tcode});
                    try {
                        this.fields[i] = new ObjectStreamField(fname, signature, false);
                        i++;
                    } catch (RuntimeException e) {
                        String str2 = this.name;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("invalid descriptor for field ");
                        stringBuilder.append(fname);
                        throw ((IOException) new InvalidClassException(str2, stringBuilder.toString()).initCause(e));
                    }
                }
                computeFieldOffsets();
                return;
            }
            str = this.name;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("enum descriptor has non-zero field count: ");
            stringBuilder2.append(numFields);
            throw new InvalidClassException(str, stringBuilder2.toString());
        }
        str = this.name;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("enum descriptor has non-zero serialVersionUID: ");
        stringBuilder3.append(this.suid);
        throw new InvalidClassException(str, stringBuilder3.toString());
    }

    void writeNonProxy(ObjectOutputStream out) throws IOException {
        out.writeUTF(this.name);
        out.writeLong(getSerialVersionUID());
        byte flags = (byte) 0;
        if (this.externalizable) {
            flags = (byte) (0 | 4);
            if (out.getProtocolVersion() != 1) {
                flags = (byte) (flags | 8);
            }
        } else if (this.serializable) {
            flags = (byte) (0 | 2);
        }
        if (this.hasWriteObjectData) {
            flags = (byte) (flags | 1);
        }
        if (this.isEnum) {
            flags = (byte) (flags | 16);
        }
        out.writeByte(flags);
        out.writeShort(this.fields.length);
        for (ObjectStreamField f : this.fields) {
            out.writeByte(f.getTypeCode());
            out.writeUTF(f.getName());
            if (!f.isPrimitive()) {
                out.writeTypeString(f.getTypeString());
            }
        }
    }

    ClassNotFoundException getResolveException() {
        return this.resolveEx;
    }

    private final void requireInitialized() {
        if (!this.initialized) {
            throw new InternalError("Unexpected call when not initialized");
        }
    }

    void checkDeserialize() throws InvalidClassException {
        requireInitialized();
        if (this.deserializeEx != null) {
            throw this.deserializeEx.newInvalidClassException();
        }
    }

    void checkSerialize() throws InvalidClassException {
        requireInitialized();
        if (this.serializeEx != null) {
            throw this.serializeEx.newInvalidClassException();
        }
    }

    void checkDefaultSerialize() throws InvalidClassException {
        requireInitialized();
        if (this.defaultSerializeEx != null) {
            throw this.defaultSerializeEx.newInvalidClassException();
        }
    }

    ObjectStreamClass getSuperDesc() {
        requireInitialized();
        return this.superDesc;
    }

    ObjectStreamClass getLocalDesc() {
        requireInitialized();
        return this.localDesc;
    }

    ObjectStreamField[] getFields(boolean copy) {
        return copy ? (ObjectStreamField[]) this.fields.clone() : this.fields;
    }

    ObjectStreamField getField(String name, Class<?> type) {
        for (ObjectStreamField f : this.fields) {
            if (f.getName().equals(name)) {
                if (type == null || (type == Object.class && !f.isPrimitive())) {
                    return f;
                }
                Class<?> ftype = f.getType();
                if (ftype != null && type.isAssignableFrom(ftype)) {
                    return f;
                }
            }
        }
        return null;
    }

    boolean isProxy() {
        requireInitialized();
        return this.isProxy;
    }

    boolean isEnum() {
        requireInitialized();
        return this.isEnum;
    }

    boolean isExternalizable() {
        requireInitialized();
        return this.externalizable;
    }

    boolean isSerializable() {
        requireInitialized();
        return this.serializable;
    }

    boolean hasBlockExternalData() {
        requireInitialized();
        return this.hasBlockExternalData;
    }

    boolean hasWriteObjectData() {
        requireInitialized();
        return this.hasWriteObjectData;
    }

    boolean isInstantiable() {
        requireInitialized();
        return this.cons != null;
    }

    boolean hasWriteObjectMethod() {
        requireInitialized();
        return this.writeObjectMethod != null;
    }

    boolean hasReadObjectMethod() {
        requireInitialized();
        return this.readObjectMethod != null;
    }

    boolean hasReadObjectNoDataMethod() {
        requireInitialized();
        return this.readObjectNoDataMethod != null;
    }

    boolean hasWriteReplaceMethod() {
        requireInitialized();
        return this.writeReplaceMethod != null;
    }

    boolean hasReadResolveMethod() {
        requireInitialized();
        return this.readResolveMethod != null;
    }

    Object newInstance() throws InstantiationException, InvocationTargetException, UnsupportedOperationException {
        requireInitialized();
        if (this.cons != null) {
            try {
                return this.cons.newInstance(new Object[0]);
            } catch (IllegalAccessException ex) {
                throw new InternalError(ex);
            }
        }
        throw new UnsupportedOperationException();
    }

    void invokeWriteObject(Object obj, ObjectOutputStream out) throws IOException, UnsupportedOperationException {
        requireInitialized();
        if (this.writeObjectMethod != null) {
            try {
                this.writeObjectMethod.invoke(obj, out);
                return;
            } catch (InvocationTargetException ex) {
                Throwable th = ex.getTargetException();
                if (th instanceof IOException) {
                    throw ((IOException) th);
                }
                throwMiscException(th);
                return;
            } catch (IllegalAccessException ex2) {
                throw new InternalError(ex2);
            }
        }
        throw new UnsupportedOperationException();
    }

    void invokeReadObject(Object obj, ObjectInputStream in) throws ClassNotFoundException, IOException, UnsupportedOperationException {
        requireInitialized();
        if (this.readObjectMethod != null) {
            try {
                this.readObjectMethod.invoke(obj, in);
                return;
            } catch (InvocationTargetException ex) {
                Throwable th = ex.getTargetException();
                if (th instanceof ClassNotFoundException) {
                    throw ((ClassNotFoundException) th);
                } else if (th instanceof IOException) {
                    throw ((IOException) th);
                } else {
                    throwMiscException(th);
                    return;
                }
            } catch (IllegalAccessException ex2) {
                throw new InternalError(ex2);
            }
        }
        throw new UnsupportedOperationException();
    }

    void invokeReadObjectNoData(Object obj) throws IOException, UnsupportedOperationException {
        requireInitialized();
        if (this.readObjectNoDataMethod != null) {
            try {
                this.readObjectNoDataMethod.invoke(obj, (Object[]) null);
                return;
            } catch (InvocationTargetException ex) {
                Throwable th = ex.getTargetException();
                if (th instanceof ObjectStreamException) {
                    throw ((ObjectStreamException) th);
                }
                throwMiscException(th);
                return;
            } catch (IllegalAccessException ex2) {
                throw new InternalError(ex2);
            }
        }
        throw new UnsupportedOperationException();
    }

    Object invokeWriteReplace(Object obj) throws IOException, UnsupportedOperationException {
        requireInitialized();
        if (this.writeReplaceMethod != null) {
            try {
                return this.writeReplaceMethod.invoke(obj, (Object[]) null);
            } catch (InvocationTargetException ex) {
                Throwable th = ex.getTargetException();
                if (th instanceof ObjectStreamException) {
                    throw ((ObjectStreamException) th);
                }
                throwMiscException(th);
                throw new InternalError(th);
            } catch (IllegalAccessException ex2) {
                throw new InternalError(ex2);
            }
        }
        throw new UnsupportedOperationException();
    }

    Object invokeReadResolve(Object obj) throws IOException, UnsupportedOperationException {
        requireInitialized();
        if (this.readResolveMethod != null) {
            try {
                return this.readResolveMethod.invoke(obj, (Object[]) null);
            } catch (InvocationTargetException ex) {
                Throwable th = ex.getTargetException();
                if (th instanceof ObjectStreamException) {
                    throw ((ObjectStreamException) th);
                }
                throwMiscException(th);
                throw new InternalError(th);
            } catch (IllegalAccessException ex2) {
                throw new InternalError(ex2);
            }
        }
        throw new UnsupportedOperationException();
    }

    ClassDataSlot[] getClassDataLayout() throws InvalidClassException {
        if (this.dataLayout == null) {
            this.dataLayout = getClassDataLayout0();
        }
        return this.dataLayout;
    }

    private ClassDataSlot[] getClassDataLayout0() throws InvalidClassException {
        ArrayList<ClassDataSlot> slots = new ArrayList();
        Class<?> start = this.cl;
        Class<?> end = this.cl;
        while (end != null && Serializable.class.isAssignableFrom(end)) {
            end = end.getSuperclass();
        }
        HashSet<String> oscNames = new HashSet(3);
        Class<?> start2 = start;
        ObjectStreamClass d = this;
        while (d != null) {
            if (oscNames.contains(d.name)) {
                throw new InvalidClassException("Circular reference.");
            }
            Class<?> c;
            oscNames.add(d.name);
            String searchName = d.cl != null ? d.cl.getName() : d.name;
            Class<?> match = null;
            for (c = start2; c != end; c = c.getSuperclass()) {
                if (searchName.equals(c.getName())) {
                    match = c;
                    break;
                }
            }
            if (match != null) {
                for (c = start2; c != match; c = c.getSuperclass()) {
                    slots.add(new ClassDataSlot(lookup(c, true), false));
                }
                start2 = match.getSuperclass();
            }
            slots.add(new ClassDataSlot(d.getVariantFor(match), true));
            d = d.superDesc;
        }
        for (start = start2; start != end; start = start.getSuperclass()) {
            slots.add(new ClassDataSlot(lookup(start, true), false));
        }
        Collections.reverse(slots);
        return (ClassDataSlot[]) slots.toArray(new ClassDataSlot[slots.size()]);
    }

    int getPrimDataSize() {
        return this.primDataSize;
    }

    int getNumObjFields() {
        return this.numObjFields;
    }

    void getPrimFieldValues(Object obj, byte[] buf) {
        this.fieldRefl.getPrimFieldValues(obj, buf);
    }

    void setPrimFieldValues(Object obj, byte[] buf) {
        this.fieldRefl.setPrimFieldValues(obj, buf);
    }

    void getObjFieldValues(Object obj, Object[] vals) {
        this.fieldRefl.getObjFieldValues(obj, vals);
    }

    void setObjFieldValues(Object obj, Object[] vals) {
        this.fieldRefl.setObjFieldValues(obj, vals);
    }

    private void computeFieldOffsets() throws InvalidClassException {
        int i = 0;
        this.primDataSize = 0;
        this.numObjFields = 0;
        int firstObjIndex = -1;
        while (i < this.fields.length) {
            ObjectStreamField f = this.fields[i];
            switch (f.getTypeCode()) {
                case 'B':
                case 'Z':
                    int i2 = this.primDataSize;
                    this.primDataSize = i2 + 1;
                    f.setOffset(i2);
                    break;
                case 'C':
                case 'S':
                    f.setOffset(this.primDataSize);
                    this.primDataSize += 2;
                    break;
                case 'D':
                case 'J':
                    f.setOffset(this.primDataSize);
                    this.primDataSize += 8;
                    break;
                case Types.DATALINK /*70*/:
                case 'I':
                    f.setOffset(this.primDataSize);
                    this.primDataSize += 4;
                    break;
                case 'L':
                case Types.DATE /*91*/:
                    int i3 = this.numObjFields;
                    this.numObjFields = i3 + 1;
                    f.setOffset(i3);
                    if (firstObjIndex != -1) {
                        break;
                    }
                    firstObjIndex = i;
                    break;
                default:
                    throw new InternalError();
            }
            i++;
        }
        if (firstObjIndex != -1 && this.numObjFields + firstObjIndex != this.fields.length) {
            throw new InvalidClassException(this.name, "illegal field order");
        }
    }

    private ObjectStreamClass getVariantFor(Class<?> cl) throws InvalidClassException {
        if (this.cl == cl) {
            return this;
        }
        ObjectStreamClass desc = new ObjectStreamClass();
        if (this.isProxy) {
            desc.initProxy(cl, null, this.superDesc);
        } else {
            desc.initNonProxy(this, cl, null, this.superDesc);
        }
        return desc;
    }

    private static Constructor<?> getExternalizableConstructor(Class<?> cl) {
        Constructor<?> constructor = null;
        try {
            Constructor<?> cons = cl.getDeclaredConstructor((Class[]) null);
            cons.setAccessible(true);
            if ((1 & cons.getModifiers()) != 0) {
                constructor = cons;
            }
            return constructor;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Constructor<?> getSerializableConstructor(Class<?> cl) {
        Class<?> initCl = cl;
        while (Serializable.class.isAssignableFrom(initCl)) {
            Class<?> superclass = initCl.getSuperclass();
            initCl = superclass;
            if (superclass == null) {
                return null;
            }
        }
        try {
            Constructor<?> cons = initCl.getDeclaredConstructor((Class[]) null);
            int mods = cons.getModifiers();
            if ((mods & 2) == 0) {
                if ((mods & 5) != 0 || packageEquals(cl, initCl)) {
                    if (cons.getDeclaringClass() != cl) {
                        cons = cons.serializationCopy(cons.getDeclaringClass(), cl);
                    }
                    cons.setAccessible(true);
                    return cons;
                }
            }
            return null;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Method getInheritableMethod(Class<?> cl, String name, Class<?>[] argTypes, Class<?> returnType) {
        Method meth = null;
        Class<?> defCl = cl;
        while (defCl != null) {
            try {
                meth = defCl.getDeclaredMethod(name, argTypes);
                break;
            } catch (NoSuchMethodException e) {
                defCl = defCl.getSuperclass();
            }
        }
        Method method = null;
        if (meth == null || meth.getReturnType() != returnType) {
            return null;
        }
        meth.setAccessible(true);
        int mods = meth.getModifiers();
        if ((mods & 1032) != 0) {
            return null;
        }
        if ((mods & 5) != 0) {
            return meth;
        }
        if ((mods & 2) != 0) {
            if (cl == defCl) {
                method = meth;
            }
            return method;
        }
        if (packageEquals(cl, defCl)) {
            method = meth;
        }
        return method;
    }

    private static Method getPrivateMethod(Class<?> cl, String name, Class<?>[] argTypes, Class<?> returnType) {
        Method method = null;
        try {
            Method meth = cl.getDeclaredMethod(name, argTypes);
            meth.setAccessible(true);
            int mods = meth.getModifiers();
            if (meth.getReturnType() == returnType && (mods & 8) == 0 && (mods & 2) != 0) {
                method = meth;
            }
            return method;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static boolean packageEquals(Class<?> cl1, Class<?> cl2) {
        return cl1.getClassLoader() == cl2.getClassLoader() && getPackageName(cl1).equals(getPackageName(cl2));
    }

    private static String getPackageName(Class<?> cl) {
        String s = cl.getName();
        int i = s.lastIndexOf(91);
        if (i >= 0) {
            s = s.substring(i + 2);
        }
        i = s.lastIndexOf(46);
        return i >= 0 ? s.substring(0, i) : "";
    }

    private static boolean classNamesEqual(String name1, String name2) {
        return name1.substring(name1.lastIndexOf(46) + 1).equals(name2.substring(name2.lastIndexOf(46) + 1));
    }

    private static String getClassSignature(Class<?> cl) {
        StringBuilder sbuf = new StringBuilder();
        while (cl.isArray()) {
            sbuf.append('[');
            cl = cl.getComponentType();
        }
        if (!cl.isPrimitive()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append('L');
            stringBuilder.append(cl.getName().replace('.', '/'));
            stringBuilder.append(';');
            sbuf.append(stringBuilder.toString());
        } else if (cl == Integer.TYPE) {
            sbuf.append('I');
        } else if (cl == Byte.TYPE) {
            sbuf.append('B');
        } else if (cl == Long.TYPE) {
            sbuf.append('J');
        } else if (cl == Float.TYPE) {
            sbuf.append('F');
        } else if (cl == Double.TYPE) {
            sbuf.append('D');
        } else if (cl == Short.TYPE) {
            sbuf.append('S');
        } else if (cl == Character.TYPE) {
            sbuf.append('C');
        } else if (cl == Boolean.TYPE) {
            sbuf.append('Z');
        } else if (cl == Void.TYPE) {
            sbuf.append('V');
        } else {
            throw new InternalError();
        }
        return sbuf.toString();
    }

    private static String getMethodSignature(Class<?>[] paramTypes, Class<?> retType) {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append('(');
        for (Class classSignature : paramTypes) {
            sbuf.append(getClassSignature(classSignature));
        }
        sbuf.append(')');
        sbuf.append(getClassSignature(retType));
        return sbuf.toString();
    }

    private static void throwMiscException(Throwable th) throws IOException {
        if (th instanceof RuntimeException) {
            throw ((RuntimeException) th);
        } else if (th instanceof Error) {
            throw ((Error) th);
        } else {
            IOException ex = new IOException("unexpected exception type");
            ex.initCause(th);
            throw ex;
        }
    }

    private static ObjectStreamField[] getSerialFields(Class<?> cl) throws InvalidClassException {
        ObjectStreamField[] fields;
        if (!Serializable.class.isAssignableFrom(cl) || Externalizable.class.isAssignableFrom(cl) || Proxy.isProxyClass(cl) || cl.isInterface()) {
            fields = NO_FIELDS;
        } else {
            ObjectStreamField[] declaredSerialFields = getDeclaredSerialFields(cl);
            fields = declaredSerialFields;
            if (declaredSerialFields == null) {
                fields = getDefaultSerialFields(cl);
            }
            Arrays.sort((Object[]) fields);
        }
        return fields;
    }

    private static ObjectStreamField[] getDeclaredSerialFields(Class<?> cl) throws InvalidClassException {
        ObjectStreamField[] serialPersistentFields = null;
        try {
            Field f = cl.getDeclaredField("serialPersistentFields");
            if ((f.getModifiers() & 26) == 26) {
                f.setAccessible(true);
                serialPersistentFields = (ObjectStreamField[]) f.get(null);
            }
        } catch (Exception e) {
        }
        if (serialPersistentFields == null) {
            return null;
        }
        if (serialPersistentFields.length == 0) {
            return NO_FIELDS;
        }
        ObjectStreamField[] boundFields = new ObjectStreamField[serialPersistentFields.length];
        Set<String> fieldNames = new HashSet(serialPersistentFields.length);
        for (int i = 0; i < serialPersistentFields.length; i++) {
            ObjectStreamField spf = serialPersistentFields[i];
            String fname = spf.getName();
            if (fieldNames.contains(fname)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("multiple serializable fields named ");
                stringBuilder.append(fname);
                throw new InvalidClassException(stringBuilder.toString());
            }
            fieldNames.add(fname);
            try {
                Field f2 = cl.getDeclaredField(fname);
                if (f2.getType() == spf.getType() && (f2.getModifiers() & 8) == 0) {
                    boundFields[i] = new ObjectStreamField(f2, spf.isUnshared(), true);
                }
            } catch (NoSuchFieldException e2) {
            }
            if (boundFields[i] == null) {
                boundFields[i] = new ObjectStreamField(fname, spf.getType(), spf.isUnshared());
            }
        }
        return boundFields;
    }

    private static ObjectStreamField[] getDefaultSerialFields(Class<?> cl) {
        Field[] clFields = cl.getDeclaredFields();
        ArrayList<ObjectStreamField> list = new ArrayList();
        for (int i = 0; i < clFields.length; i++) {
            if ((clFields[i].getModifiers() & 136) == 0) {
                list.add(new ObjectStreamField(clFields[i], false, true));
            }
        }
        int size = list.size();
        if (size == 0) {
            return NO_FIELDS;
        }
        return (ObjectStreamField[]) list.toArray(new ObjectStreamField[size]);
    }

    private static Long getDeclaredSUID(Class<?> cl) {
        try {
            Field f = cl.getDeclaredField("serialVersionUID");
            if ((f.getModifiers() & 24) == 24) {
                f.setAccessible(true);
                return Long.valueOf(f.getLong(null));
            }
        } catch (Exception e) {
        }
        return null;
    }

    private static long computeDefaultSUID(Class<?> cl) {
        Class<?> cls = cl;
        if (!Serializable.class.isAssignableFrom(cls) || Proxy.isProxyClass(cl)) {
            return 0;
        }
        try {
            int i;
            int mods;
            int i2;
            char c;
            int i3;
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);
            dout.writeUTF(cl.getName());
            int classMods = cl.getModifiers() & 1553;
            Method[] methods = cl.getDeclaredMethods();
            if ((classMods & 512) != 0) {
                int i4;
                if (methods.length > 0) {
                    i4 = classMods | 1024;
                } else {
                    i4 = classMods & -1025;
                }
                classMods = i4;
            }
            dout.writeInt(classMods);
            if (!cl.isArray()) {
                Class<?>[] interfaces = cl.getInterfaces();
                Object[] ifaceNames = new String[interfaces.length];
                for (i = 0; i < interfaces.length; i++) {
                    ifaceNames[i] = interfaces[i].getName();
                }
                Arrays.sort(ifaceNames);
                for (String writeUTF : ifaceNames) {
                    dout.writeUTF(writeUTF);
                }
            }
            Field[] fields = cl.getDeclaredFields();
            MemberSignature[] fieldSigs = new MemberSignature[fields.length];
            for (i = 0; i < fields.length; i++) {
                fieldSigs[i] = new MemberSignature(fields[i]);
            }
            Arrays.sort(fieldSigs, new Comparator<MemberSignature>() {
                public int compare(MemberSignature ms1, MemberSignature ms2) {
                    return ms1.name.compareTo(ms2.name);
                }
            });
            for (MemberSignature sig : fieldSigs) {
                mods = sig.member.getModifiers() & 223;
                if ((mods & 2) == 0 || (mods & 136) == 0) {
                    dout.writeUTF(sig.name);
                    dout.writeInt(mods);
                    dout.writeUTF(sig.signature);
                }
            }
            if (hasStaticInitializer(cls, VMRuntime.getRuntime().getTargetSdkVersion() > MAX_SDK_TARGET_FOR_CLINIT_UIDGEN_WORKAROUND)) {
                dout.writeUTF("<clinit>");
                dout.writeInt(8);
                dout.writeUTF("()V");
            }
            Constructor<?>[] cons = cl.getDeclaredConstructors();
            MemberSignature[] consSigs = new MemberSignature[cons.length];
            for (i2 = 0; i2 < cons.length; i2++) {
                consSigs[i2] = new MemberSignature(cons[i2]);
            }
            Arrays.sort(consSigs, new Comparator<MemberSignature>() {
                public int compare(MemberSignature ms1, MemberSignature ms2) {
                    return ms1.signature.compareTo(ms2.signature);
                }
            });
            i2 = 0;
            while (true) {
                c = '/';
                if (i2 >= consSigs.length) {
                    break;
                }
                MemberSignature sig2 = consSigs[i2];
                mods = sig2.member.getModifiers() & 3391;
                if ((mods & 2) == 0) {
                    dout.writeUTF("<init>");
                    dout.writeInt(mods);
                    dout.writeUTF(sig2.signature.replace('/', '.'));
                }
                i2++;
            }
            MemberSignature[] methSigs = new MemberSignature[methods.length];
            for (i3 = 0; i3 < methods.length; i3++) {
                methSigs[i3] = new MemberSignature(methods[i3]);
            }
            Arrays.sort(methSigs, new Comparator<MemberSignature>() {
                public int compare(MemberSignature ms1, MemberSignature ms2) {
                    int comp = ms1.name.compareTo(ms2.name);
                    if (comp == 0) {
                        return ms1.signature.compareTo(ms2.signature);
                    }
                    return comp;
                }
            });
            int i5 = 0;
            while (true) {
                i3 = i5;
                if (i3 >= methSigs.length) {
                    break;
                }
                char c2;
                MemberSignature sig3 = methSigs[i3];
                int mods2 = sig3.member.getModifiers() & 3391;
                if ((mods2 & 2) == 0) {
                    dout.writeUTF(sig3.name);
                    dout.writeInt(mods2);
                    c2 = '/';
                    dout.writeUTF(sig3.signature.replace('/', '.'));
                } else {
                    c2 = c;
                }
                i5 = i3 + 1;
                c = c2;
                cls = cl;
            }
            dout.flush();
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] hashBytes = md.digest(bout.toByteArray());
            long hash = 0;
            long j = 8;
            i3 = Math.min(hashBytes.length, 8) - 1;
            while (i3 >= 0) {
                hash = (hash << j) | ((long) (hashBytes[i3] & 255));
                i3--;
                bout = bout;
                md = md;
                j = 8;
            }
            MessageDigest messageDigest = md;
            return hash;
        } catch (IOException ex) {
            throw new InternalError(ex);
        } catch (NoSuchAlgorithmException ex2) {
            throw new SecurityException(ex2.getMessage());
        }
    }

    private static FieldReflector getReflector(ObjectStreamField[] fields, ObjectStreamClass localDesc) throws InvalidClassException {
        Class<?> cl = (localDesc == null || fields.length <= 0) ? null : localDesc.cl;
        processQueue(Caches.reflectorsQueue, Caches.reflectors);
        FieldReflectorKey key = new FieldReflectorKey(cl, fields, Caches.reflectorsQueue);
        Reference<?> ref = (Reference) Caches.reflectors.get(key);
        Object entry = null;
        if (ref != null) {
            entry = ref.get();
        }
        EntryFuture future = null;
        if (entry == null) {
            EntryFuture newEntry = new EntryFuture();
            Reference<?> newRef = new SoftReference(newEntry);
            do {
                if (ref != null) {
                    Caches.reflectors.remove(key, ref);
                }
                ref = (Reference) Caches.reflectors.putIfAbsent(key, newRef);
                if (ref != null) {
                    entry = ref.get();
                }
                if (ref == null) {
                    break;
                }
            } while (entry == null);
            if (entry == null) {
                future = newEntry;
            }
        }
        if (entry instanceof FieldReflector) {
            return (FieldReflector) entry;
        }
        if (entry instanceof EntryFuture) {
            entry = ((EntryFuture) entry).get();
        } else if (entry == null) {
            Throwable th;
            try {
                th = new FieldReflector(matchFields(fields, localDesc));
            } catch (Throwable th2) {
                th = th2;
            }
            entry = th;
            future.set(entry);
            Caches.reflectors.put(key, new SoftReference(entry));
        }
        if (entry instanceof FieldReflector) {
            return (FieldReflector) entry;
        }
        if (entry instanceof InvalidClassException) {
            throw ((InvalidClassException) entry);
        } else if (entry instanceof RuntimeException) {
            throw ((RuntimeException) entry);
        } else if (entry instanceof Error) {
            throw ((Error) entry);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unexpected entry: ");
            stringBuilder.append(entry);
            throw new InternalError(stringBuilder.toString());
        }
    }

    private static ObjectStreamField[] matchFields(ObjectStreamField[] fields, ObjectStreamClass localDesc) throws InvalidClassException {
        ObjectStreamField[] localFields;
        if (localDesc != null) {
            localFields = localDesc.fields;
        } else {
            localFields = NO_FIELDS;
        }
        ObjectStreamField[] matches = new ObjectStreamField[fields.length];
        for (int i = 0; i < fields.length; i++) {
            ObjectStreamField f = fields[i];
            ObjectStreamField m = null;
            for (ObjectStreamField lf : localFields) {
                if (f.getName().equals(lf.getName()) && f.getSignature().equals(lf.getSignature())) {
                    if (lf.getField() != null) {
                        m = new ObjectStreamField(lf.getField(), lf.isUnshared(), false);
                    } else {
                        m = new ObjectStreamField(lf.getName(), lf.getSignature(), lf.isUnshared());
                    }
                }
            }
            if (m == null) {
                m = new ObjectStreamField(f.getName(), f.getSignature(), false);
            }
            m.setOffset(f.getOffset());
            matches[i] = m;
        }
        return matches;
    }

    private static long getConstructorId(Class<?> cls) {
        int targetSdkVersion = VMRuntime.getRuntime().getTargetSdkVersion();
        if (targetSdkVersion <= 0 || targetSdkVersion > 24) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ObjectStreamClass.getConstructorId(Class<?>) is not supported on SDK ");
            stringBuilder.append(targetSdkVersion);
            throw new UnsupportedOperationException(stringBuilder.toString());
        }
        System.logE("WARNING: ObjectStreamClass.getConstructorId(Class<?>) is private API andwill be removed in a future Android release.");
        return 1189998819991197253L;
    }

    private static Object newInstance(Class<?> clazz, long constructorId) {
        int targetSdkVersion = VMRuntime.getRuntime().getTargetSdkVersion();
        if (targetSdkVersion <= 0 || targetSdkVersion > 24) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ObjectStreamClass.newInstance(Class<?>, long) is not supported on SDK ");
            stringBuilder.append(targetSdkVersion);
            throw new UnsupportedOperationException(stringBuilder.toString());
        }
        System.logE("WARNING: ObjectStreamClass.newInstance(Class<?>, long) is private API andwill be removed in a future Android release.");
        return Unsafe.getUnsafe().allocateInstance(clazz);
    }

    static void processQueue(ReferenceQueue<Class<?>> queue, ConcurrentMap<? extends WeakReference<Class<?>>, ?> map) {
        while (true) {
            Reference<? extends Class<?>> poll = queue.poll();
            Reference<? extends Class<?>> ref = poll;
            if (poll != null) {
                map.remove(ref);
            } else {
                return;
            }
        }
    }
}
