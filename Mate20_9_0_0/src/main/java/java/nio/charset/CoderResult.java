package java.nio.charset;

import java.lang.ref.WeakReference;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.HashMap;
import java.util.Map;

public class CoderResult {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int CR_ERROR_MIN = 2;
    private static final int CR_MALFORMED = 2;
    private static final int CR_OVERFLOW = 1;
    private static final int CR_UNDERFLOW = 0;
    private static final int CR_UNMAPPABLE = 3;
    public static final CoderResult OVERFLOW = new CoderResult(1, 0);
    public static final CoderResult UNDERFLOW = new CoderResult(0, 0);
    private static Cache malformedCache = new Cache() {
        public CoderResult create(int len) {
            return new CoderResult(2, len, null);
        }
    };
    private static final String[] names = new String[]{"UNDERFLOW", "OVERFLOW", "MALFORMED", "UNMAPPABLE"};
    private static Cache unmappableCache = new Cache() {
        public CoderResult create(int len) {
            return new CoderResult(3, len, null);
        }
    };
    private final int length;
    private final int type;

    private static abstract class Cache {
        private Map<Integer, WeakReference<CoderResult>> cache;

        protected abstract CoderResult create(int i);

        private Cache() {
            this.cache = null;
        }

        /* synthetic */ Cache(AnonymousClass1 x0) {
            this();
        }

        private synchronized CoderResult get(int len) {
            CoderResult e;
            if (len > 0) {
                Integer k = new Integer(len);
                e = null;
                if (this.cache == null) {
                    this.cache = new HashMap();
                } else {
                    WeakReference<CoderResult> weakReference = (WeakReference) this.cache.get(k);
                    WeakReference<CoderResult> w = weakReference;
                    if (weakReference != null) {
                        e = (CoderResult) w.get();
                    }
                }
                if (e == null) {
                    e = create(len);
                    this.cache.put(k, new WeakReference(e));
                }
            } else {
                throw new IllegalArgumentException("Non-positive length");
            }
            return e;
        }
    }

    /* synthetic */ CoderResult(int x0, int x1, AnonymousClass1 x2) {
        this(x0, x1);
    }

    private CoderResult(int type, int length) {
        this.type = type;
        this.length = length;
    }

    public String toString() {
        String nm = names[this.type];
        if (!isError()) {
            return nm;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(nm);
        stringBuilder.append("[");
        stringBuilder.append(this.length);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public boolean isUnderflow() {
        return this.type == 0 ? true : $assertionsDisabled;
    }

    public boolean isOverflow() {
        return this.type == 1 ? true : $assertionsDisabled;
    }

    public boolean isError() {
        return this.type >= 2 ? true : $assertionsDisabled;
    }

    public boolean isMalformed() {
        return this.type == 2 ? true : $assertionsDisabled;
    }

    public boolean isUnmappable() {
        return this.type == 3 ? true : $assertionsDisabled;
    }

    public int length() {
        if (isError()) {
            return this.length;
        }
        throw new UnsupportedOperationException();
    }

    public static CoderResult malformedForLength(int length) {
        return malformedCache.get(length);
    }

    public static CoderResult unmappableForLength(int length) {
        return unmappableCache.get(length);
    }

    public void throwException() throws CharacterCodingException {
        switch (this.type) {
            case 0:
                throw new BufferUnderflowException();
            case 1:
                throw new BufferOverflowException();
            case 2:
                throw new MalformedInputException(this.length);
            case 3:
                throw new UnmappableCharacterException(this.length);
            default:
                return;
        }
    }
}
