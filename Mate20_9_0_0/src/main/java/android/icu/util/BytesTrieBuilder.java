package android.icu.util;

import android.icu.text.Bidi;
import android.icu.util.StringTrieBuilder.Option;
import dalvik.bytecode.Opcodes;
import java.nio.ByteBuffer;

public final class BytesTrieBuilder extends StringTrieBuilder {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private byte[] bytes;
    private int bytesLength;
    private final byte[] intBytes = new byte[5];

    private static final class BytesAsCharSequence implements CharSequence {
        private int len;
        private byte[] s;

        public BytesAsCharSequence(byte[] sequence, int length) {
            this.s = sequence;
            this.len = length;
        }

        public char charAt(int i) {
            return (char) (this.s[i] & 255);
        }

        public int length() {
            return this.len;
        }

        public CharSequence subSequence(int start, int end) {
            return null;
        }
    }

    public BytesTrieBuilder add(byte[] sequence, int length, int value) {
        addImpl(new BytesAsCharSequence(sequence, length), value);
        return this;
    }

    public BytesTrie build(Option buildOption) {
        buildBytes(buildOption);
        return new BytesTrie(this.bytes, this.bytes.length - this.bytesLength);
    }

    public ByteBuffer buildByteBuffer(Option buildOption) {
        buildBytes(buildOption);
        return ByteBuffer.wrap(this.bytes, this.bytes.length - this.bytesLength, this.bytesLength);
    }

    private void buildBytes(Option buildOption) {
        if (this.bytes == null) {
            this.bytes = new byte[1024];
        }
        buildImpl(buildOption);
    }

    public BytesTrieBuilder clear() {
        clearImpl();
        this.bytes = null;
        this.bytesLength = 0;
        return this;
    }

    @Deprecated
    protected boolean matchNodesCanHaveValues() {
        return false;
    }

    @Deprecated
    protected int getMaxBranchLinearSubNodeLength() {
        return 5;
    }

    @Deprecated
    protected int getMinLinearMatch() {
        return 16;
    }

    @Deprecated
    protected int getMaxLinearMatchLength() {
        return 16;
    }

    private void ensureCapacity(int length) {
        if (length > this.bytes.length) {
            int newCapacity = this.bytes.length;
            do {
                newCapacity *= 2;
            } while (newCapacity <= length);
            byte[] newBytes = new byte[newCapacity];
            System.arraycopy(this.bytes, this.bytes.length - this.bytesLength, newBytes, newBytes.length - this.bytesLength, this.bytesLength);
            this.bytes = newBytes;
        }
    }

    @Deprecated
    protected int write(int b) {
        int newLength = this.bytesLength + 1;
        ensureCapacity(newLength);
        this.bytesLength = newLength;
        this.bytes[this.bytes.length - this.bytesLength] = (byte) b;
        return this.bytesLength;
    }

    @Deprecated
    protected int write(int offset, int length) {
        int newLength = this.bytesLength + length;
        ensureCapacity(newLength);
        this.bytesLength = newLength;
        int bytesOffset = this.bytes.length - this.bytesLength;
        while (length > 0) {
            int bytesOffset2 = bytesOffset + 1;
            int offset2 = offset + 1;
            this.bytes[bytesOffset] = (byte) this.strings.charAt(offset);
            length--;
            bytesOffset = bytesOffset2;
            offset = offset2;
        }
        return this.bytesLength;
    }

    private int write(byte[] b, int length) {
        int newLength = this.bytesLength + length;
        ensureCapacity(newLength);
        this.bytesLength = newLength;
        System.arraycopy(b, 0, this.bytes, this.bytes.length - this.bytesLength, length);
        return this.bytesLength;
    }

    @Deprecated
    protected int writeValueAndFinal(int i, boolean isFinal) {
        if (i >= 0 && i <= 64) {
            return write(((16 + i) << 1) | isFinal);
        }
        int length;
        int length2 = 1;
        if (i < 0 || i > 16777215) {
            this.intBytes[0] = Bidi.LEVEL_DEFAULT_RTL;
            this.intBytes[1] = (byte) (i >> 24);
            this.intBytes[2] = (byte) (i >> 16);
            this.intBytes[3] = (byte) (i >> 8);
            this.intBytes[4] = (byte) i;
            length = 5;
        } else {
            if (i <= Opcodes.OP_SGET_SHORT_JUMBO) {
                this.intBytes[0] = (byte) (81 + (i >> 8));
            } else {
                if (i <= 1179647) {
                    this.intBytes[0] = (byte) (108 + (i >> 16));
                } else {
                    this.intBytes[0] = Bidi.LEVEL_DEFAULT_LTR;
                    this.intBytes[1] = (byte) (i >> 16);
                    length2 = 2;
                }
                length = length2 + 1;
                this.intBytes[length2] = (byte) (i >> 8);
                length2 = length;
            }
            length = length2 + 1;
            this.intBytes[length2] = (byte) i;
        }
        this.intBytes[0] = (byte) ((this.intBytes[0] << 1) | isFinal);
        return write(this.intBytes, length);
    }

    @Deprecated
    protected int writeValueAndType(boolean hasValue, int value, int node) {
        int offset = write(node);
        if (hasValue) {
            return writeValueAndFinal(value, false);
        }
        return offset;
    }

    @Deprecated
    protected int writeDeltaTo(int jumpTarget) {
        int i = this.bytesLength - jumpTarget;
        if (i <= 191) {
            return write(i);
        }
        int length;
        if (i <= 12287) {
            this.intBytes[0] = (byte) (192 + (i >> 8));
            length = 1;
        } else {
            if (i <= 917503) {
                this.intBytes[0] = (byte) (240 + (i >> 16));
                length = 2;
            } else {
                if (i <= 16777215) {
                    this.intBytes[0] = (byte) -2;
                    length = 3;
                } else {
                    this.intBytes[0] = (byte) -1;
                    this.intBytes[1] = (byte) (i >> 24);
                    length = 4;
                }
                this.intBytes[1] = (byte) (i >> 16);
            }
            this.intBytes[1] = (byte) (i >> 8);
        }
        int length2 = length + 1;
        this.intBytes[length] = (byte) i;
        return write(this.intBytes, length2);
    }
}
