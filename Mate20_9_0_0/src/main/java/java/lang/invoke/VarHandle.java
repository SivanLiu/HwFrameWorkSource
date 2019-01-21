package java.lang.invoke;

import java.lang.invoke.MethodHandle.PolymorphicSignature;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import sun.misc.Unsafe;

public abstract class VarHandle {
    private static final int ALL_MODES_BIT_MASK = ((((READ_ACCESS_MODES_BIT_MASK | WRITE_ACCESS_MODES_BIT_MASK) | ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK) | NUMERIC_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK) | BITWISE_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK);
    private static final int ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK = accessTypesToBitMask(EnumSet.of(AccessType.COMPARE_AND_EXCHANGE, AccessType.COMPARE_AND_SWAP, AccessType.GET_AND_UPDATE));
    private static final int BITWISE_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK = accessTypesToBitMask(EnumSet.of(AccessType.GET_AND_UPDATE_BITWISE));
    private static final int NUMERIC_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK = accessTypesToBitMask(EnumSet.of(AccessType.GET_AND_UPDATE_NUMERIC));
    private static final int READ_ACCESS_MODES_BIT_MASK = accessTypesToBitMask(EnumSet.of(AccessType.GET));
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int WRITE_ACCESS_MODES_BIT_MASK = accessTypesToBitMask(EnumSet.of(AccessType.SET));
    private final int accessModesBitMask;
    private final Class<?> coordinateType0;
    private final Class<?> coordinateType1;
    private final Class<?> varType;

    public enum AccessMode {
        GET("get", AccessType.GET),
        SET("set", AccessType.SET),
        GET_VOLATILE("getVolatile", AccessType.GET),
        SET_VOLATILE("setVolatile", AccessType.SET),
        GET_ACQUIRE("getAcquire", AccessType.GET),
        SET_RELEASE("setRelease", AccessType.SET),
        GET_OPAQUE("getOpaque", AccessType.GET),
        SET_OPAQUE("setOpaque", AccessType.SET),
        COMPARE_AND_SET("compareAndSet", AccessType.COMPARE_AND_SWAP),
        COMPARE_AND_EXCHANGE("compareAndExchange", AccessType.COMPARE_AND_EXCHANGE),
        COMPARE_AND_EXCHANGE_ACQUIRE("compareAndExchangeAcquire", AccessType.COMPARE_AND_EXCHANGE),
        COMPARE_AND_EXCHANGE_RELEASE("compareAndExchangeRelease", AccessType.COMPARE_AND_EXCHANGE),
        WEAK_COMPARE_AND_SET_PLAIN("weakCompareAndSetPlain", AccessType.COMPARE_AND_SWAP),
        WEAK_COMPARE_AND_SET("weakCompareAndSet", AccessType.COMPARE_AND_SWAP),
        WEAK_COMPARE_AND_SET_ACQUIRE("weakCompareAndSetAcquire", AccessType.COMPARE_AND_SWAP),
        WEAK_COMPARE_AND_SET_RELEASE("weakCompareAndSetRelease", AccessType.COMPARE_AND_SWAP),
        GET_AND_SET("getAndSet", AccessType.GET_AND_UPDATE),
        GET_AND_SET_ACQUIRE("getAndSetAcquire", AccessType.GET_AND_UPDATE),
        GET_AND_SET_RELEASE("getAndSetRelease", AccessType.GET_AND_UPDATE),
        GET_AND_ADD("getAndAdd", AccessType.GET_AND_UPDATE_NUMERIC),
        GET_AND_ADD_ACQUIRE("getAndAddAcquire", AccessType.GET_AND_UPDATE_NUMERIC),
        GET_AND_ADD_RELEASE("getAndAddRelease", AccessType.GET_AND_UPDATE_NUMERIC),
        GET_AND_BITWISE_OR("getAndBitwiseOr", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_OR_RELEASE("getAndBitwiseOrRelease", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_OR_ACQUIRE("getAndBitwiseOrAcquire", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_AND("getAndBitwiseAnd", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_AND_RELEASE("getAndBitwiseAndRelease", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_AND_ACQUIRE("getAndBitwiseAndAcquire", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_XOR("getAndBitwiseXor", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_XOR_RELEASE("getAndBitwiseXorRelease", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_XOR_ACQUIRE("getAndBitwiseXorAcquire", AccessType.GET_AND_UPDATE_BITWISE);
        
        static final Map<String, AccessMode> methodNameToAccessMode = null;
        final AccessType at;
        final String methodName;

        static {
            methodNameToAccessMode = new HashMap(values().length);
            AccessMode[] values = values();
            int length = values.length;
            int i;
            while (i < length) {
                AccessMode am = values[i];
                methodNameToAccessMode.put(am.methodName, am);
                i++;
            }
        }

        private AccessMode(String methodName, AccessType at) {
            this.methodName = methodName;
            this.at = at;
        }

        public String methodName() {
            return this.methodName;
        }

        public static AccessMode valueFromMethodName(String methodName) {
            AccessMode am = (AccessMode) methodNameToAccessMode.get(methodName);
            if (am != null) {
                return am;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No AccessMode value for method name ");
            stringBuilder.append(methodName);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    enum AccessType {
        GET,
        SET,
        COMPARE_AND_SWAP,
        COMPARE_AND_EXCHANGE,
        GET_AND_UPDATE,
        GET_AND_UPDATE_BITWISE,
        GET_AND_UPDATE_NUMERIC;

        MethodType accessModeType(Class<?> receiver, Class<?> value, Class<?>... intermediate) {
            Class[] ps;
            int i;
            int i2;
            switch (this) {
                case GET:
                    ps = allocateParameters(0, receiver, intermediate);
                    fillParameters(ps, receiver, intermediate);
                    return MethodType.methodType((Class) value, ps);
                case SET:
                    ps = allocateParameters(1, receiver, intermediate);
                    ps[fillParameters(ps, receiver, intermediate)] = value;
                    return MethodType.methodType(Void.TYPE, ps);
                case COMPARE_AND_SWAP:
                    ps = allocateParameters(2, receiver, intermediate);
                    i = fillParameters(ps, receiver, intermediate);
                    i2 = i + 1;
                    ps[i] = value;
                    ps[i2] = value;
                    return MethodType.methodType(Boolean.TYPE, ps);
                case COMPARE_AND_EXCHANGE:
                    ps = allocateParameters(2, receiver, intermediate);
                    i = fillParameters(ps, receiver, intermediate);
                    i2 = i + 1;
                    ps[i] = value;
                    ps[i2] = value;
                    return MethodType.methodType((Class) value, ps);
                case GET_AND_UPDATE:
                case GET_AND_UPDATE_BITWISE:
                case GET_AND_UPDATE_NUMERIC:
                    ps = allocateParameters(1, receiver, intermediate);
                    ps[fillParameters(ps, receiver, intermediate)] = value;
                    return MethodType.methodType((Class) value, ps);
                default:
                    throw new InternalError("Unknown AccessType");
            }
        }

        private static Class<?>[] allocateParameters(int values, Class<?> receiver, Class<?>... intermediate) {
            return new Class[(((receiver != null ? 1 : 0) + intermediate.length) + values)];
        }

        private static int fillParameters(Class<?>[] ps, Class<?> receiver, Class<?>... intermediate) {
            int i;
            int i2 = 0;
            if (receiver != null) {
                i = 0 + 1;
                ps[0] = receiver;
                i2 = i;
            }
            i = 0;
            while (i < intermediate.length) {
                int i3 = i2 + 1;
                ps[i2] = intermediate[i];
                i++;
                i2 = i3;
            }
            return i2;
        }
    }

    @PolymorphicSignature
    public final native Object compareAndExchange(Object... objArr);

    @PolymorphicSignature
    public final native Object compareAndExchangeAcquire(Object... objArr);

    @PolymorphicSignature
    public final native Object compareAndExchangeRelease(Object... objArr);

    @PolymorphicSignature
    public final native boolean compareAndSet(Object... objArr);

    @PolymorphicSignature
    public final native Object get(Object... objArr);

    @PolymorphicSignature
    public final native Object getAcquire(Object... objArr);

    @PolymorphicSignature
    public final native Object getAndAdd(Object... objArr);

    @PolymorphicSignature
    public final native Object getAndAddAcquire(Object... objArr);

    @PolymorphicSignature
    public final native Object getAndAddRelease(Object... objArr);

    @PolymorphicSignature
    public final native Object getAndBitwiseAnd(Object... objArr);

    @PolymorphicSignature
    public final native Object getAndBitwiseAndAcquire(Object... objArr);

    @PolymorphicSignature
    public final native Object getAndBitwiseAndRelease(Object... objArr);

    @PolymorphicSignature
    public final native Object getAndBitwiseOr(Object... objArr);

    @PolymorphicSignature
    public final native Object getAndBitwiseOrAcquire(Object... objArr);

    @PolymorphicSignature
    public final native Object getAndBitwiseOrRelease(Object... objArr);

    @PolymorphicSignature
    public final native Object getAndBitwiseXor(Object... objArr);

    @PolymorphicSignature
    public final native Object getAndBitwiseXorAcquire(Object... objArr);

    @PolymorphicSignature
    public final native Object getAndBitwiseXorRelease(Object... objArr);

    @PolymorphicSignature
    public final native Object getAndSet(Object... objArr);

    @PolymorphicSignature
    public final native Object getAndSetAcquire(Object... objArr);

    @PolymorphicSignature
    public final native Object getAndSetRelease(Object... objArr);

    @PolymorphicSignature
    public final native Object getOpaque(Object... objArr);

    @PolymorphicSignature
    public final native Object getVolatile(Object... objArr);

    @PolymorphicSignature
    public final native void set(Object... objArr);

    @PolymorphicSignature
    public final native void setOpaque(Object... objArr);

    @PolymorphicSignature
    public final native void setRelease(Object... objArr);

    @PolymorphicSignature
    public final native void setVolatile(Object... objArr);

    @PolymorphicSignature
    public final native boolean weakCompareAndSet(Object... objArr);

    @PolymorphicSignature
    public final native boolean weakCompareAndSetAcquire(Object... objArr);

    @PolymorphicSignature
    public final native boolean weakCompareAndSetPlain(Object... objArr);

    @PolymorphicSignature
    public final native boolean weakCompareAndSetRelease(Object... objArr);

    static {
        if (AccessMode.values().length <= 32) {
            return;
        }
        throw new InternalError("accessModes overflow");
    }

    public final Class<?> varType() {
        return this.varType;
    }

    public final List<Class<?>> coordinateTypes() {
        if (this.coordinateType0 == null) {
            return Collections.EMPTY_LIST;
        }
        if (this.coordinateType1 == null) {
            return Collections.singletonList(this.coordinateType0);
        }
        return Collections.unmodifiableList(Arrays.asList(this.coordinateType0, this.coordinateType1));
    }

    public final MethodType accessModeType(AccessMode accessMode) {
        if (this.coordinateType1 == null) {
            return accessMode.at.accessModeType(this.coordinateType0, this.varType, new Class[0]);
        }
        return accessMode.at.accessModeType(this.coordinateType0, this.varType, this.coordinateType1);
    }

    public final boolean isAccessModeSupported(AccessMode accessMode) {
        int testBit = 1 << accessMode.ordinal();
        if ((this.accessModesBitMask & testBit) == testBit) {
            return true;
        }
        return false;
    }

    public final MethodHandle toMethodHandle(AccessMode accessMode) {
        return MethodHandles.varHandleExactInvoker(accessMode, accessModeType(accessMode)).bindTo(this);
    }

    public static void fullFence() {
        UNSAFE.fullFence();
    }

    public static void acquireFence() {
        UNSAFE.loadFence();
    }

    public static void releaseFence() {
        UNSAFE.storeFence();
    }

    public static void loadLoadFence() {
        UNSAFE.loadFence();
    }

    public static void storeStoreFence() {
        UNSAFE.storeFence();
    }

    VarHandle(Class<?> varType, boolean isFinal) {
        this.varType = (Class) Objects.requireNonNull(varType);
        this.coordinateType0 = null;
        this.coordinateType1 = null;
        this.accessModesBitMask = alignedAccessModesBitMask(varType, isFinal);
    }

    VarHandle(Class<?> varType, boolean isFinal, Class<?> coordinateType) {
        this.varType = (Class) Objects.requireNonNull(varType);
        this.coordinateType0 = (Class) Objects.requireNonNull(coordinateType);
        this.coordinateType1 = null;
        this.accessModesBitMask = alignedAccessModesBitMask(varType, isFinal);
    }

    VarHandle(Class<?> varType, Class<?> backingArrayType, boolean isFinal, Class<?> coordinateType0, Class<?> coordinateType1) {
        this.varType = (Class) Objects.requireNonNull(varType);
        this.coordinateType0 = (Class) Objects.requireNonNull(coordinateType0);
        this.coordinateType1 = (Class) Objects.requireNonNull(coordinateType1);
        Objects.requireNonNull(backingArrayType);
        Class<?> backingArrayComponentType = backingArrayType.getComponentType();
        if (backingArrayComponentType != varType && backingArrayComponentType != Byte.TYPE) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported backingArrayType: ");
            stringBuilder.append((Object) backingArrayType);
            throw new InternalError(stringBuilder.toString());
        } else if (backingArrayType.getComponentType() == varType) {
            this.accessModesBitMask = alignedAccessModesBitMask(varType, isFinal);
        } else {
            this.accessModesBitMask = unalignedAccessModesBitMask(varType);
        }
    }

    static int accessTypesToBitMask(EnumSet<AccessType> accessTypes) {
        int m = 0;
        for (AccessMode accessMode : AccessMode.values()) {
            if (accessTypes.contains(accessMode.at)) {
                m |= 1 << accessMode.ordinal();
            }
        }
        return m;
    }

    static int alignedAccessModesBitMask(Class<?> varType, boolean isFinal) {
        int bitMask = ALL_MODES_BIT_MASK;
        if (isFinal) {
            bitMask &= READ_ACCESS_MODES_BIT_MASK;
        }
        if (!(varType == Byte.TYPE || varType == Short.TYPE || varType == Character.TYPE || varType == Integer.TYPE || varType == Long.TYPE || varType == Float.TYPE || varType == Double.TYPE)) {
            bitMask &= ~NUMERIC_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK;
        }
        if (varType == Boolean.TYPE || varType == Byte.TYPE || varType == Short.TYPE || varType == Character.TYPE || varType == Integer.TYPE || varType == Long.TYPE) {
            return bitMask;
        }
        return bitMask & (~BITWISE_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK);
    }

    static int unalignedAccessModesBitMask(Class<?> varType) {
        int bitMask = READ_ACCESS_MODES_BIT_MASK | WRITE_ACCESS_MODES_BIT_MASK;
        if (varType == Integer.TYPE || varType == Long.TYPE || varType == Float.TYPE || varType == Double.TYPE) {
            bitMask |= ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK;
        }
        if (varType == Integer.TYPE || varType == Long.TYPE) {
            bitMask |= NUMERIC_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK;
        }
        if (varType == Integer.TYPE || varType == Long.TYPE) {
            return bitMask | BITWISE_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK;
        }
        return bitMask;
    }
}
