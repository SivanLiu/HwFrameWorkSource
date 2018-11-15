package android.icu.impl.coll;

import android.icu.text.DateTimePatternGenerator;

public final class CollationKeys {
    static final /* synthetic */ boolean -assertionsDisabled = (CollationKeys.class.desiredAssertionStatus() ^ 1);
    private static final int CASE_LOWER_FIRST_COMMON_HIGH = 13;
    private static final int CASE_LOWER_FIRST_COMMON_LOW = 1;
    private static final int CASE_LOWER_FIRST_COMMON_MAX_COUNT = 7;
    private static final int CASE_LOWER_FIRST_COMMON_MIDDLE = 7;
    private static final int CASE_UPPER_FIRST_COMMON_HIGH = 15;
    private static final int CASE_UPPER_FIRST_COMMON_LOW = 3;
    private static final int CASE_UPPER_FIRST_COMMON_MAX_COUNT = 13;
    private static final int QUAT_COMMON_HIGH = 252;
    private static final int QUAT_COMMON_LOW = 28;
    private static final int QUAT_COMMON_MAX_COUNT = 113;
    private static final int QUAT_COMMON_MIDDLE = 140;
    private static final int QUAT_SHIFTED_LIMIT_BYTE = 27;
    static final int SEC_COMMON_HIGH = 69;
    private static final int SEC_COMMON_LOW = 5;
    private static final int SEC_COMMON_MAX_COUNT = 33;
    private static final int SEC_COMMON_MIDDLE = 37;
    public static final LevelCallback SIMPLE_LEVEL_FALLBACK = new LevelCallback();
    private static final int TER_LOWER_FIRST_COMMON_HIGH = 69;
    private static final int TER_LOWER_FIRST_COMMON_LOW = 5;
    private static final int TER_LOWER_FIRST_COMMON_MAX_COUNT = 33;
    private static final int TER_LOWER_FIRST_COMMON_MIDDLE = 37;
    private static final int TER_ONLY_COMMON_HIGH = 197;
    private static final int TER_ONLY_COMMON_LOW = 5;
    private static final int TER_ONLY_COMMON_MAX_COUNT = 97;
    private static final int TER_ONLY_COMMON_MIDDLE = 101;
    private static final int TER_UPPER_FIRST_COMMON_HIGH = 197;
    private static final int TER_UPPER_FIRST_COMMON_LOW = 133;
    private static final int TER_UPPER_FIRST_COMMON_MAX_COUNT = 33;
    private static final int TER_UPPER_FIRST_COMMON_MIDDLE = 165;
    private static final int[] levelMasks = new int[]{2, 6, 22, 54, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 54};

    public static class LevelCallback {
        boolean needToWrite(int level) {
            return true;
        }
    }

    public static abstract class SortKeyByteSink {
        private int appended_ = 0;
        protected byte[] buffer_;

        protected abstract void AppendBeyondCapacity(byte[] bArr, int i, int i2, int i3);

        protected abstract boolean Resize(int i, int i2);

        public SortKeyByteSink(byte[] dest) {
            this.buffer_ = dest;
        }

        public void setBufferAndAppended(byte[] dest, int app) {
            this.buffer_ = dest;
            this.appended_ = app;
        }

        public void Append(byte[] bytes, int n) {
            if (n > 0 && bytes != null) {
                int length = this.appended_;
                this.appended_ += n;
                if (n <= this.buffer_.length - length) {
                    System.arraycopy(bytes, 0, this.buffer_, length, n);
                } else {
                    AppendBeyondCapacity(bytes, 0, n, length);
                }
            }
        }

        public void Append(int b) {
            if (this.appended_ < this.buffer_.length || Resize(1, this.appended_)) {
                this.buffer_[this.appended_] = (byte) b;
            }
            this.appended_++;
        }

        public int NumberOfBytesAppended() {
            return this.appended_;
        }

        public int GetRemainingCapacity() {
            return this.buffer_.length - this.appended_;
        }

        public boolean Overflowed() {
            return this.appended_ > this.buffer_.length;
        }
    }

    private static final class SortKeyLevel {
        static final /* synthetic */ boolean -assertionsDisabled = (SortKeyLevel.class.desiredAssertionStatus() ^ 1);
        private static final int INITIAL_CAPACITY = 40;
        byte[] buffer = new byte[40];
        int len = 0;

        SortKeyLevel() {
        }

        boolean isEmpty() {
            return this.len == 0;
        }

        int length() {
            return this.len;
        }

        byte getAt(int index) {
            return this.buffer[index];
        }

        byte[] data() {
            return this.buffer;
        }

        void appendByte(int b) {
            if (this.len < this.buffer.length || ensureCapacity(1)) {
                byte[] bArr = this.buffer;
                int i = this.len;
                this.len = i + 1;
                bArr[i] = (byte) b;
            }
        }

        void appendWeight16(int w) {
            if (-assertionsDisabled || (DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH & w) != 0) {
                byte b0 = (byte) (w >>> 8);
                byte b1 = (byte) w;
                int appendLength = b1 == (byte) 0 ? 1 : 2;
                if (this.len + appendLength <= this.buffer.length || ensureCapacity(appendLength)) {
                    byte[] bArr = this.buffer;
                    int i = this.len;
                    this.len = i + 1;
                    bArr[i] = b0;
                    if (b1 != (byte) 0) {
                        bArr = this.buffer;
                        i = this.len;
                        this.len = i + 1;
                        bArr[i] = b1;
                        return;
                    }
                    return;
                }
                return;
            }
            throw new AssertionError();
        }

        void appendWeight32(long w) {
            if (-assertionsDisabled || w != 0) {
                byte[] bytes = new byte[]{(byte) ((int) (w >>> 24)), (byte) ((int) (w >>> 16)), (byte) ((int) (w >>> 8)), (byte) ((int) w)};
                int appendLength = bytes[1] == (byte) 0 ? 1 : bytes[2] == (byte) 0 ? 2 : bytes[3] == (byte) 0 ? 3 : 4;
                if (this.len + appendLength <= this.buffer.length || ensureCapacity(appendLength)) {
                    byte[] bArr = this.buffer;
                    int i = this.len;
                    this.len = i + 1;
                    bArr[i] = bytes[0];
                    if (bytes[1] != (byte) 0) {
                        bArr = this.buffer;
                        i = this.len;
                        this.len = i + 1;
                        bArr[i] = bytes[1];
                        if (bytes[2] != (byte) 0) {
                            bArr = this.buffer;
                            i = this.len;
                            this.len = i + 1;
                            bArr[i] = bytes[2];
                            if (bytes[3] != (byte) 0) {
                                bArr = this.buffer;
                                i = this.len;
                                this.len = i + 1;
                                bArr[i] = bytes[3];
                                return;
                            }
                            return;
                        }
                        return;
                    }
                    return;
                }
                return;
            }
            throw new AssertionError();
        }

        void appendReverseWeight16(int w) {
            if (-assertionsDisabled || (DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH & w) != 0) {
                byte b0 = (byte) (w >>> 8);
                byte b1 = (byte) w;
                int appendLength = b1 == (byte) 0 ? 1 : 2;
                if (this.len + appendLength > this.buffer.length && !ensureCapacity(appendLength)) {
                    return;
                }
                if (b1 == (byte) 0) {
                    byte[] bArr = this.buffer;
                    int i = this.len;
                    this.len = i + 1;
                    bArr[i] = b0;
                    return;
                }
                this.buffer[this.len] = b1;
                this.buffer[this.len + 1] = b0;
                this.len += 2;
                return;
            }
            throw new AssertionError();
        }

        void appendTo(SortKeyByteSink sink) {
            if (-assertionsDisabled || (this.len > 0 && this.buffer[this.len - 1] == (byte) 1)) {
                sink.Append(this.buffer, this.len - 1);
                return;
            }
            throw new AssertionError();
        }

        private boolean ensureCapacity(int appendCapacity) {
            int newCapacity = this.buffer.length * 2;
            int altCapacity = this.len + (appendCapacity * 2);
            if (newCapacity < altCapacity) {
                newCapacity = altCapacity;
            }
            if (newCapacity < 200) {
                newCapacity = 200;
            }
            byte[] newbuf = new byte[newCapacity];
            System.arraycopy(this.buffer, 0, newbuf, 0, this.len);
            this.buffer = newbuf;
            return true;
        }
    }

    public static void writeSortKeyUpToQuaternary(android.icu.impl.coll.CollationIterator r1, boolean[] r2, android.icu.impl.coll.CollationSettings r3, android.icu.impl.coll.CollationKeys.SortKeyByteSink r4, int r5, android.icu.impl.coll.CollationKeys.LevelCallback r6, boolean r7) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.icu.impl.coll.CollationKeys.writeSortKeyUpToQuaternary(android.icu.impl.coll.CollationIterator, boolean[], android.icu.impl.coll.CollationSettings, android.icu.impl.coll.CollationKeys$SortKeyByteSink, int, android.icu.impl.coll.CollationKeys$LevelCallback, boolean):void
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: android.icu.impl.coll.CollationKeys.writeSortKeyUpToQuaternary(android.icu.impl.coll.CollationIterator, boolean[], android.icu.impl.coll.CollationSettings, android.icu.impl.coll.CollationKeys$SortKeyByteSink, int, android.icu.impl.coll.CollationKeys$LevelCallback, boolean):void");
    }

    private static SortKeyLevel getSortKeyLevel(int levels, int level) {
        return (levels & level) != 0 ? new SortKeyLevel() : null;
    }

    private CollationKeys() {
    }
}
