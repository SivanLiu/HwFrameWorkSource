package com.android.internal.util.function.pooled;

import android.os.Message;
import android.text.TextUtils;
import android.util.Pools.SynchronizedPool;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.BitUtils;
import com.android.internal.util.function.HexConsumer;
import com.android.internal.util.function.HexFunction;
import com.android.internal.util.function.HexPredicate;
import com.android.internal.util.function.QuadConsumer;
import com.android.internal.util.function.QuadFunction;
import com.android.internal.util.function.QuadPredicate;
import com.android.internal.util.function.QuintConsumer;
import com.android.internal.util.function.QuintFunction;
import com.android.internal.util.function.QuintPredicate;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.TriFunction;
import com.android.internal.util.function.TriPredicate;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class PooledLambdaImpl<R> extends OmniFunction<Object, Object, Object, Object, Object, Object, R> {
    private static final boolean DEBUG = false;
    private static final int FLAG_ACQUIRED_FROM_MESSAGE_CALLBACKS_POOL = 128;
    private static final int FLAG_RECYCLED = 32;
    private static final int FLAG_RECYCLE_ON_USE = 64;
    private static final String LOG_TAG = "PooledLambdaImpl";
    static final int MASK_EXPOSED_AS = 16128;
    static final int MASK_FUNC_TYPE = 1032192;
    private static final int MAX_ARGS = 5;
    private static final int MAX_POOL_SIZE = 50;
    static final Pool sMessageCallbacksPool = new Pool(Message.sPoolSync);
    static final Pool sPool = new Pool(new Object());
    Object[] mArgs = null;
    long mConstValue;
    int mFlags = 0;
    Object mFunc;

    static class LambdaType {
        public static final int MASK = 63;
        public static final int MASK_ARG_COUNT = 7;
        public static final int MASK_BIT_COUNT = 6;
        public static final int MASK_RETURN_TYPE = 56;

        static class ReturnType {
            public static final int BOOLEAN = 2;
            public static final int DOUBLE = 6;
            public static final int INT = 4;
            public static final int LONG = 5;
            public static final int OBJECT = 3;
            public static final int VOID = 1;

            ReturnType() {
            }

            static String toString(int returnType) {
                switch (returnType) {
                    case 1:
                        return "VOID";
                    case 2:
                        return "BOOLEAN";
                    case 3:
                        return "OBJECT";
                    case 4:
                        return "INT";
                    case 5:
                        return "LONG";
                    case 6:
                        return "DOUBLE";
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("");
                        stringBuilder.append(returnType);
                        return stringBuilder.toString();
                }
            }

            static String lambdaSuffix(int type) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(prefix(type));
                stringBuilder.append(suffix(type));
                return stringBuilder.toString();
            }

            private static String prefix(int type) {
                switch (type) {
                    case 4:
                        return "Int";
                    case 5:
                        return "Long";
                    case 6:
                        return "Double";
                    default:
                        return "";
                }
            }

            private static String suffix(int type) {
                switch (type) {
                    case 1:
                        return "Consumer";
                    case 2:
                        return "Predicate";
                    case 3:
                        return "Function";
                    default:
                        return "Supplier";
                }
            }
        }

        LambdaType() {
        }

        static int encode(int argCount, int returnType) {
            return PooledLambdaImpl.mask(7, argCount) | PooledLambdaImpl.mask(56, returnType);
        }

        static int decodeArgCount(int type) {
            return type & 7;
        }

        static int decodeReturnType(int type) {
            return PooledLambdaImpl.unmask(56, type);
        }

        static String toString(int type) {
            int argCount = decodeArgCount(type);
            int returnType = decodeReturnType(type);
            if (argCount == 0) {
                if (returnType == 1) {
                    return "Runnable";
                }
                if (returnType == 3 || returnType == 2) {
                    return "Supplier";
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(argCountPrefix(argCount));
            stringBuilder.append(ReturnType.lambdaSuffix(returnType));
            return stringBuilder.toString();
        }

        private static String argCountPrefix(int argCount) {
            switch (argCount) {
                case 1:
                    return "";
                case 2:
                    return "Bi";
                case 3:
                    return "Tri";
                case 4:
                    return "Quad";
                case 5:
                    return "Quint";
                case 6:
                    return "Hex";
                case 7:
                    return "";
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("");
                    stringBuilder.append(argCount);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
    }

    static class Pool extends SynchronizedPool<PooledLambdaImpl> {
        public Pool(Object lock) {
            super(50, lock);
        }
    }

    private PooledLambdaImpl() {
    }

    public void recycle() {
        if (!isRecycled()) {
            doRecycle();
        }
    }

    private void doRecycle() {
        Pool pool;
        if ((this.mFlags & 128) != 0) {
            pool = sMessageCallbacksPool;
        } else {
            pool = sPool;
        }
        this.mFunc = null;
        if (this.mArgs != null) {
            Arrays.fill(this.mArgs, null);
        }
        this.mFlags = 32;
        this.mConstValue = 0;
        pool.release(this);
    }

    R invoke(Object a1, Object a2, Object a3, Object a4, Object a5, Object a6) {
        checkNotRecycled();
        int i = 0;
        if (!fillInArg(a1) || !fillInArg(a2) || !fillInArg(a3) || !fillInArg(a4) || !fillInArg(a5) || !fillInArg(a6)) {
            boolean notUsed = false;
        }
        int argCount = LambdaType.decodeArgCount(getFlags(MASK_FUNC_TYPE));
        if (argCount != 7) {
            int i2 = 0;
            while (i2 < argCount) {
                if (this.mArgs[i2] != ArgumentPlaceholder.INSTANCE) {
                    i2++;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Missing argument #");
                    stringBuilder.append(i2);
                    stringBuilder.append(" among ");
                    stringBuilder.append(Arrays.toString(this.mArgs));
                    throw new IllegalStateException(stringBuilder.toString());
                }
            }
        }
        int argsSize;
        try {
            Object doInvoke = doInvoke();
            if (isRecycleOnUse()) {
                doRecycle();
            }
            if (!isRecycled()) {
                argsSize = ArrayUtils.size(this.mArgs);
                while (i < argsSize) {
                    popArg(i);
                    i++;
                }
            }
            return doInvoke;
        } catch (Throwable th) {
            if (isRecycleOnUse()) {
                doRecycle();
            }
            if (!isRecycled()) {
                argsSize = ArrayUtils.size(this.mArgs);
                while (i < argsSize) {
                    popArg(i);
                    i++;
                }
            }
        }
    }

    private boolean fillInArg(Object invocationArg) {
        int argsSize = ArrayUtils.size(this.mArgs);
        for (int i = 0; i < argsSize; i++) {
            if (this.mArgs[i] == ArgumentPlaceholder.INSTANCE) {
                this.mArgs[i] = invocationArg;
                this.mFlags = (int) (((long) this.mFlags) | BitUtils.bitAt(i));
                return true;
            }
        }
        if (invocationArg == null || invocationArg == ArgumentPlaceholder.INSTANCE) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No more arguments expected for provided arg ");
        stringBuilder.append(invocationArg);
        stringBuilder.append(" among ");
        stringBuilder.append(Arrays.toString(this.mArgs));
        throw new IllegalStateException(stringBuilder.toString());
    }

    private void checkNotRecycled() {
        if (isRecycled()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Instance is recycled: ");
            stringBuilder.append(this);
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    private R doInvoke() {
        int funcType = getFlags(MASK_FUNC_TYPE);
        int argCount = LambdaType.decodeArgCount(funcType);
        int returnType = LambdaType.decodeReturnType(funcType);
        switch (argCount) {
            case 0:
                switch (returnType) {
                    case 1:
                        ((Runnable) this.mFunc).run();
                        return null;
                    case 2:
                    case 3:
                        return ((Supplier) this.mFunc).get();
                }
                break;
            case 1:
                switch (returnType) {
                    case 1:
                        ((Consumer) this.mFunc).accept(popArg(0));
                        return null;
                    case 2:
                        return Boolean.valueOf(((Predicate) this.mFunc).test(popArg(0)));
                    case 3:
                        return ((Function) this.mFunc).apply(popArg(0));
                }
                break;
            case 2:
                switch (returnType) {
                    case 1:
                        ((BiConsumer) this.mFunc).accept(popArg(0), popArg(1));
                        return null;
                    case 2:
                        return Boolean.valueOf(((BiPredicate) this.mFunc).test(popArg(0), popArg(1)));
                    case 3:
                        return ((BiFunction) this.mFunc).apply(popArg(0), popArg(1));
                }
                break;
            case 3:
                switch (returnType) {
                    case 1:
                        ((TriConsumer) this.mFunc).accept(popArg(0), popArg(1), popArg(2));
                        return null;
                    case 2:
                        return Boolean.valueOf(((TriPredicate) this.mFunc).test(popArg(0), popArg(1), popArg(2)));
                    case 3:
                        return ((TriFunction) this.mFunc).apply(popArg(0), popArg(1), popArg(2));
                }
                break;
            case 4:
                switch (returnType) {
                    case 1:
                        ((QuadConsumer) this.mFunc).accept(popArg(0), popArg(1), popArg(2), popArg(3));
                        return null;
                    case 2:
                        return Boolean.valueOf(((QuadPredicate) this.mFunc).test(popArg(0), popArg(1), popArg(2), popArg(3)));
                    case 3:
                        return ((QuadFunction) this.mFunc).apply(popArg(0), popArg(1), popArg(2), popArg(3));
                }
                break;
            case 5:
                switch (returnType) {
                    case 1:
                        ((QuintConsumer) this.mFunc).accept(popArg(0), popArg(1), popArg(2), popArg(3), popArg(4));
                        return null;
                    case 2:
                        return Boolean.valueOf(((QuintPredicate) this.mFunc).test(popArg(0), popArg(1), popArg(2), popArg(3), popArg(4)));
                    case 3:
                        return ((QuintFunction) this.mFunc).apply(popArg(0), popArg(1), popArg(2), popArg(3), popArg(4));
                }
                break;
            case 6:
                switch (returnType) {
                    case 1:
                        ((HexConsumer) this.mFunc).accept(popArg(0), popArg(1), popArg(2), popArg(3), popArg(4), popArg(5));
                        return null;
                    case 2:
                        return Boolean.valueOf(((HexPredicate) this.mFunc).test(popArg(0), popArg(1), popArg(2), popArg(3), popArg(4), popArg(5)));
                    case 3:
                        return ((HexFunction) this.mFunc).apply(popArg(0), popArg(1), popArg(2), popArg(3), popArg(4), popArg(5));
                }
                break;
            case 7:
                switch (returnType) {
                    case 4:
                        return Integer.valueOf(getAsInt());
                    case 5:
                        return Long.valueOf(getAsLong());
                    case 6:
                        return Double.valueOf(getAsDouble());
                    default:
                        return this.mFunc;
                }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown function type: ");
        stringBuilder.append(LambdaType.toString(funcType));
        throw new IllegalStateException(stringBuilder.toString());
    }

    private boolean isConstSupplier() {
        return LambdaType.decodeArgCount(getFlags(MASK_FUNC_TYPE)) == 7;
    }

    private Object popArg(int index) {
        Object result = this.mArgs[index];
        if (isInvocationArgAtIndex(index)) {
            this.mArgs[index] = ArgumentPlaceholder.INSTANCE;
            this.mFlags = (int) (((long) this.mFlags) & (~BitUtils.bitAt(index)));
        }
        return result;
    }

    public String toString() {
        StringBuilder stringBuilder;
        if (isRecycled()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("<recycled PooledLambda@");
            stringBuilder.append(hashCodeHex(this));
            stringBuilder.append(">");
            return stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        if (isConstSupplier()) {
            stringBuilder.append(getFuncTypeAsString());
            stringBuilder.append("(");
            stringBuilder.append(doInvoke());
            stringBuilder.append(")");
        } else {
            if (this.mFunc instanceof PooledLambdaImpl) {
                stringBuilder.append(this.mFunc);
            } else {
                stringBuilder.append(getFuncTypeAsString());
                stringBuilder.append("@");
                stringBuilder.append(hashCodeHex(this.mFunc));
            }
            stringBuilder.append("(");
            stringBuilder.append(commaSeparateFirstN(this.mArgs, LambdaType.decodeArgCount(getFlags(MASK_FUNC_TYPE))));
            stringBuilder.append(")");
        }
        return stringBuilder.toString();
    }

    private String commaSeparateFirstN(Object[] arr, int n) {
        if (arr == null) {
            return "";
        }
        return TextUtils.join(",", Arrays.copyOf(arr, n));
    }

    private static String hashCodeHex(Object o) {
        return Integer.toHexString(o.hashCode());
    }

    private String getFuncTypeAsString() {
        if (isRecycled()) {
            throw new IllegalStateException();
        } else if (isConstSupplier()) {
            return "supplier";
        } else {
            String name = LambdaType.toString(getFlags(MASK_EXPOSED_AS));
            if (name.endsWith("Consumer")) {
                return "consumer";
            }
            if (name.endsWith("Function")) {
                return "function";
            }
            if (name.endsWith("Predicate")) {
                return "predicate";
            }
            if (name.endsWith("Supplier")) {
                return "supplier";
            }
            if (name.endsWith("Runnable")) {
                return "runnable";
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Don't know the string representation of ");
            stringBuilder.append(name);
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    static <E extends PooledLambda> E acquire(Pool pool, Object func, int fNumArgs, int numPlaceholders, int fReturnType, Object a, Object b, Object c, Object d, Object e, Object f) {
        PooledLambdaImpl r = acquire(pool);
        r.mFunc = func;
        r.setFlags(MASK_FUNC_TYPE, LambdaType.encode(fNumArgs, fReturnType));
        r.setFlags(MASK_EXPOSED_AS, LambdaType.encode(numPlaceholders, fReturnType));
        if (ArrayUtils.size(r.mArgs) < fNumArgs) {
            r.mArgs = new Object[fNumArgs];
        }
        setIfInBounds(r.mArgs, 0, a);
        setIfInBounds(r.mArgs, 1, b);
        setIfInBounds(r.mArgs, 2, c);
        setIfInBounds(r.mArgs, 3, d);
        setIfInBounds(r.mArgs, 4, e);
        setIfInBounds(r.mArgs, 5, f);
        return r;
    }

    static PooledLambdaImpl acquireConstSupplier(int type) {
        PooledLambdaImpl r = acquire(sPool);
        int lambdaType = LambdaType.encode(7, type);
        r.setFlags(MASK_FUNC_TYPE, lambdaType);
        r.setFlags(MASK_EXPOSED_AS, lambdaType);
        return r;
    }

    static PooledLambdaImpl acquire(Pool pool) {
        PooledLambdaImpl r = (PooledLambdaImpl) pool.acquire();
        if (r == null) {
            r = new PooledLambdaImpl();
        }
        r.mFlags &= -33;
        r.setFlags(128, pool == sMessageCallbacksPool ? 1 : 0);
        return r;
    }

    private static void setIfInBounds(Object[] array, int i, Object a) {
        if (i < ArrayUtils.size(array)) {
            array[i] = a;
        }
    }

    public OmniFunction<Object, Object, Object, Object, Object, Object, R> negate() {
        throw new UnsupportedOperationException();
    }

    public <V> OmniFunction<Object, Object, Object, Object, Object, Object, V> andThen(Function<? super R, ? extends V> function) {
        throw new UnsupportedOperationException();
    }

    public double getAsDouble() {
        return Double.longBitsToDouble(this.mConstValue);
    }

    public int getAsInt() {
        return (int) this.mConstValue;
    }

    public long getAsLong() {
        return this.mConstValue;
    }

    public OmniFunction<Object, Object, Object, Object, Object, Object, R> recycleOnUse() {
        this.mFlags |= 64;
        return this;
    }

    private boolean isRecycled() {
        return (this.mFlags & 32) != 0;
    }

    private boolean isRecycleOnUse() {
        return (this.mFlags & 64) != 0;
    }

    private boolean isInvocationArgAtIndex(int argIndex) {
        return (this.mFlags & (1 << argIndex)) != 0;
    }

    int getFlags(int mask) {
        return unmask(mask, this.mFlags);
    }

    void setFlags(int mask, int value) {
        this.mFlags &= ~mask;
        this.mFlags |= mask(mask, value);
    }

    private static int mask(int mask, int value) {
        return (value << Integer.numberOfTrailingZeros(mask)) & mask;
    }

    private static int unmask(int mask, int bits) {
        return (bits & mask) / (1 << Integer.numberOfTrailingZeros(mask));
    }
}
