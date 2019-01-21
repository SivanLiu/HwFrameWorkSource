package android.icu.util;

import android.icu.text.DateTimePatternGenerator;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.NoSuchElementException;

public final class BytesTrie implements Cloneable, Iterable<Entry> {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    static final int kFiveByteDeltaLead = 255;
    static final int kFiveByteValueLead = 127;
    static final int kFourByteDeltaLead = 254;
    static final int kFourByteValueLead = 126;
    static final int kMaxBranchLinearSubNodeLength = 5;
    static final int kMaxLinearMatchLength = 16;
    static final int kMaxOneByteDelta = 191;
    static final int kMaxOneByteValue = 64;
    static final int kMaxThreeByteDelta = 917503;
    static final int kMaxThreeByteValue = 1179647;
    static final int kMaxTwoByteDelta = 12287;
    static final int kMaxTwoByteValue = 6911;
    static final int kMinLinearMatch = 16;
    static final int kMinOneByteValueLead = 16;
    static final int kMinThreeByteDeltaLead = 240;
    static final int kMinThreeByteValueLead = 108;
    static final int kMinTwoByteDeltaLead = 192;
    static final int kMinTwoByteValueLead = 81;
    static final int kMinValueLead = 32;
    private static final int kValueIsFinal = 1;
    private static Result[] valueResults_ = new Result[]{Result.INTERMEDIATE_VALUE, Result.FINAL_VALUE};
    private byte[] bytes_;
    private int pos_;
    private int remainingMatchLength_ = -1;
    private int root_;

    public static final class Entry {
        private byte[] bytes;
        private int length;
        public int value;

        private Entry(int capacity) {
            this.bytes = new byte[capacity];
        }

        public int bytesLength() {
            return this.length;
        }

        public byte byteAt(int index) {
            return this.bytes[index];
        }

        public void copyBytesTo(byte[] dest, int destOffset) {
            System.arraycopy(this.bytes, 0, dest, destOffset, this.length);
        }

        public ByteBuffer bytesAsByteBuffer() {
            return ByteBuffer.wrap(this.bytes, 0, this.length).asReadOnlyBuffer();
        }

        private void ensureCapacity(int len) {
            if (this.bytes.length < len) {
                byte[] newBytes = new byte[Math.min(this.bytes.length * 2, 2 * len)];
                System.arraycopy(this.bytes, 0, newBytes, 0, this.length);
                this.bytes = newBytes;
            }
        }

        private void append(byte b) {
            ensureCapacity(this.length + 1);
            byte[] bArr = this.bytes;
            int i = this.length;
            this.length = i + 1;
            bArr[i] = b;
        }

        private void append(byte[] b, int off, int len) {
            ensureCapacity(this.length + len);
            System.arraycopy(b, off, this.bytes, this.length, len);
            this.length += len;
        }

        private void truncateString(int newLength) {
            this.length = newLength;
        }
    }

    public static final class Iterator implements java.util.Iterator<Entry> {
        private byte[] bytes_;
        private Entry entry_;
        private int initialPos_;
        private int initialRemainingMatchLength_;
        private int maxLength_;
        private int pos_;
        private int remainingMatchLength_;
        private ArrayList<Long> stack_;

        private Iterator(byte[] trieBytes, int offset, int remainingMatchLength, int maxStringLength) {
            this.stack_ = new ArrayList();
            this.bytes_ = trieBytes;
            this.initialPos_ = offset;
            this.pos_ = offset;
            this.initialRemainingMatchLength_ = remainingMatchLength;
            this.remainingMatchLength_ = remainingMatchLength;
            this.maxLength_ = maxStringLength;
            this.entry_ = new Entry(this.maxLength_ != 0 ? this.maxLength_ : 32);
            int length = this.remainingMatchLength_;
            if (length >= 0) {
                length++;
                if (this.maxLength_ > 0 && length > this.maxLength_) {
                    length = this.maxLength_;
                }
                this.entry_.append(this.bytes_, this.pos_, length);
                this.pos_ += length;
                this.remainingMatchLength_ -= length;
            }
        }

        public Iterator reset() {
            this.pos_ = this.initialPos_;
            this.remainingMatchLength_ = this.initialRemainingMatchLength_;
            int length = this.remainingMatchLength_ + 1;
            if (this.maxLength_ > 0 && length > this.maxLength_) {
                length = this.maxLength_;
            }
            this.entry_.truncateString(length);
            this.pos_ += length;
            this.remainingMatchLength_ -= length;
            this.stack_.clear();
            return this;
        }

        public boolean hasNext() {
            return this.pos_ >= 0 || !this.stack_.isEmpty();
        }

        public Entry next() {
            int pos = this.pos_;
            boolean z = true;
            if (pos < 0) {
                if (this.stack_.isEmpty()) {
                    throw new NoSuchElementException();
                }
                long top = ((Long) this.stack_.remove(this.stack_.size() - 1)).longValue();
                int length = (int) top;
                pos = (int) (top >> 32);
                this.entry_.truncateString(DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH & length);
                length >>>= 16;
                if (length > 1) {
                    pos = branchNext(pos, length);
                    if (pos < 0) {
                        return this.entry_;
                    }
                }
                int pos2 = pos + 1;
                this.entry_.append(this.bytes_[pos]);
                pos = pos2;
            }
            if (this.remainingMatchLength_ >= 0) {
                return truncateAndStop();
            }
            while (true) {
                int pos3 = pos + 1;
                pos = this.bytes_[pos] & 255;
                if (pos >= 32) {
                    if ((pos & 1) == 0) {
                        z = false;
                    }
                    boolean isFinal = z;
                    this.entry_.value = BytesTrie.readValue(this.bytes_, pos3, pos >> 1);
                    if (isFinal || (this.maxLength_ > 0 && this.entry_.length == this.maxLength_)) {
                        this.pos_ = -1;
                    } else {
                        this.pos_ = BytesTrie.skipValue(pos3, pos);
                    }
                    return this.entry_;
                } else if (this.maxLength_ > 0 && this.entry_.length == this.maxLength_) {
                    return truncateAndStop();
                } else {
                    int pos4;
                    if (pos < 16) {
                        if (pos == 0) {
                            pos = this.bytes_[pos3] & 255;
                            pos3++;
                        }
                        pos4 = branchNext(pos3, pos + 1);
                        if (pos4 < 0) {
                            return this.entry_;
                        }
                        pos = pos4;
                    } else {
                        pos4 = (pos - 16) + 1;
                        if (this.maxLength_ <= 0 || this.entry_.length + pos4 <= this.maxLength_) {
                            this.entry_.append(this.bytes_, pos3, pos4);
                            pos = pos3 + pos4;
                        } else {
                            this.entry_.append(this.bytes_, pos3, this.maxLength_ - this.entry_.length);
                            return truncateAndStop();
                        }
                    }
                }
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private Entry truncateAndStop() {
            this.pos_ = -1;
            this.entry_.value = -1;
            return this.entry_;
        }

        private int branchNext(int trieByte, int length) {
            byte trieByte2;
            while (length > 5) {
                trieByte2 = trieByte2 + 1;
                this.stack_.add(Long.valueOf(((((long) BytesTrie.skipDelta(this.bytes_, trieByte2)) << 32) | ((long) ((length - (length >> 1)) << 16))) | ((long) this.entry_.length)));
                length >>= 1;
                trieByte2 = BytesTrie.jumpByDelta(this.bytes_, trieByte2);
            }
            int pos = trieByte2 + 1;
            trieByte2 = this.bytes_[trieByte2];
            int pos2 = pos + 1;
            int node = this.bytes_[pos] & 255;
            boolean isFinal = (node & 1) != 0;
            int value = BytesTrie.readValue(this.bytes_, pos2, node >> 1);
            pos2 = BytesTrie.skipValue(pos2, node);
            this.stack_.add(Long.valueOf(((((long) pos2) << 32) | ((long) ((length - 1) << 16))) | ((long) this.entry_.length)));
            this.entry_.append(trieByte2);
            if (!isFinal) {
                return pos2 + value;
            }
            this.pos_ = -1;
            this.entry_.value = value;
            return -1;
        }
    }

    public enum Result {
        NO_MATCH,
        NO_VALUE,
        FINAL_VALUE,
        INTERMEDIATE_VALUE;

        public boolean matches() {
            return this != NO_MATCH;
        }

        public boolean hasValue() {
            return ordinal() >= 2;
        }

        public boolean hasNext() {
            return (ordinal() & 1) != 0;
        }
    }

    public static final class State {
        private byte[] bytes;
        private int pos;
        private int remainingMatchLength;
        private int root;
    }

    public BytesTrie(byte[] trieBytes, int offset) {
        this.bytes_ = trieBytes;
        this.root_ = offset;
        this.pos_ = offset;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public BytesTrie reset() {
        this.pos_ = this.root_;
        this.remainingMatchLength_ = -1;
        return this;
    }

    public BytesTrie saveState(State state) {
        state.bytes = this.bytes_;
        state.root = this.root_;
        state.pos = this.pos_;
        state.remainingMatchLength = this.remainingMatchLength_;
        return this;
    }

    public BytesTrie resetToState(State state) {
        if (this.bytes_ == state.bytes && this.bytes_ != null && this.root_ == state.root) {
            this.pos_ = state.pos;
            this.remainingMatchLength_ = state.remainingMatchLength;
            return this;
        }
        throw new IllegalArgumentException("incompatible trie state");
    }

    public Result current() {
        int pos = this.pos_;
        if (pos < 0) {
            return Result.NO_MATCH;
        }
        Result result;
        if (this.remainingMatchLength_ < 0) {
            int i = this.bytes_[pos] & 255;
            int node = i;
            if (i >= 32) {
                result = valueResults_[node & 1];
                return result;
            }
        }
        result = Result.NO_VALUE;
        return result;
    }

    public Result first(int inByte) {
        this.remainingMatchLength_ = -1;
        if (inByte < 0) {
            inByte += 256;
        }
        return nextImpl(this.root_, inByte);
    }

    public Result next(int inByte) {
        int pos = this.pos_;
        if (pos < 0) {
            return Result.NO_MATCH;
        }
        if (inByte < 0) {
            inByte += 256;
        }
        int length = this.remainingMatchLength_;
        if (length < 0) {
            return nextImpl(pos, inByte);
        }
        int pos2 = pos + 1;
        if (inByte == (this.bytes_[pos] & 255)) {
            Result result;
            length--;
            this.remainingMatchLength_ = length;
            this.pos_ = pos2;
            if (length < 0) {
                pos = this.bytes_[pos2] & 255;
                int node = pos;
                if (pos >= 32) {
                    result = valueResults_[node & 1];
                    return result;
                }
            }
            result = Result.NO_VALUE;
            return result;
        }
        stop();
        return Result.NO_MATCH;
    }

    public Result next(byte[] s, int inByte, int sLimit) {
        if (inByte >= sLimit) {
            return current();
        }
        int pos = this.pos_;
        if (pos < 0) {
            return Result.NO_MATCH;
        }
        Result result;
        int length = this.remainingMatchLength_;
        byte inByte2;
        while (inByte2 != sLimit) {
            byte sIndex = inByte2 + 1;
            inByte2 = s[inByte2];
            if (length < 0) {
                this.remainingMatchLength_ = length;
                while (true) {
                    int pos2 = pos + 1;
                    pos = this.bytes_[pos] & 255;
                    if (pos < 16) {
                        Result result2 = branchNext(pos2, pos, inByte2 & 255);
                        if (result2 == Result.NO_MATCH) {
                            return Result.NO_MATCH;
                        }
                        if (sIndex == sLimit) {
                            return result2;
                        }
                        if (result2 == Result.FINAL_VALUE) {
                            stop();
                            return Result.NO_MATCH;
                        }
                        byte sIndex2 = sIndex + 1;
                        inByte2 = s[sIndex];
                        pos = this.pos_;
                        sIndex = sIndex2;
                    } else if (pos < 32) {
                        length = pos - 16;
                        if (inByte2 != this.bytes_[pos2]) {
                            stop();
                            return Result.NO_MATCH;
                        }
                        length--;
                        pos = pos2 + 1;
                    } else if ((pos & 1) != 0) {
                        stop();
                        return Result.NO_MATCH;
                    } else {
                        pos = skipValue(pos2, pos);
                    }
                }
            } else if (inByte2 != this.bytes_[pos]) {
                stop();
                return Result.NO_MATCH;
            } else {
                pos++;
                length--;
            }
            inByte2 = sIndex;
        }
        this.remainingMatchLength_ = length;
        this.pos_ = pos;
        if (length < 0) {
            int i = this.bytes_[pos] & 255;
            int node = i;
            if (i >= 32) {
                result = valueResults_[node & 1];
                return result;
            }
        }
        result = Result.NO_VALUE;
        return result;
    }

    public int getValue() {
        int pos = this.pos_;
        return readValue(this.bytes_, pos + 1, (this.bytes_[pos] & 255) >> 1);
    }

    public long getUniqueValue() {
        int pos = this.pos_;
        if (pos < 0) {
            return 0;
        }
        return (findUniqueValue(this.bytes_, (this.remainingMatchLength_ + pos) + 1, 0) << 31) >> 31;
    }

    public int getNextBytes(Appendable out) {
        int pos = this.pos_;
        if (pos < 0) {
            return 0;
        }
        if (this.remainingMatchLength_ >= 0) {
            append(out, this.bytes_[pos] & 255);
            return 1;
        }
        int pos2 = pos + 1;
        pos = this.bytes_[pos] & 255;
        if (pos >= 32) {
            if ((pos & 1) != 0) {
                return 0;
            }
            int pos3 = skipValue(pos2, pos);
            pos2 = pos3 + 1;
            pos = this.bytes_[pos3] & 255;
        }
        if (pos < 16) {
            int pos4;
            if (pos == 0) {
                pos4 = pos2 + 1;
                pos = this.bytes_[pos2] & 255;
            } else {
                pos4 = pos2;
            }
            pos++;
            getNextBranchBytes(this.bytes_, pos4, pos, out);
            return pos;
        }
        append(out, this.bytes_[pos2] & 255);
        return 1;
    }

    public Iterator iterator() {
        return new Iterator(this.bytes_, this.pos_, this.remainingMatchLength_, 0);
    }

    public Iterator iterator(int maxStringLength) {
        return new Iterator(this.bytes_, this.pos_, this.remainingMatchLength_, maxStringLength);
    }

    public static Iterator iterator(byte[] trieBytes, int offset, int maxStringLength) {
        return new Iterator(trieBytes, offset, -1, maxStringLength);
    }

    private void stop() {
        this.pos_ = -1;
    }

    private static int readValue(byte[] bytes, int pos, int leadByte) {
        if (leadByte < 81) {
            return leadByte - 16;
        }
        if (leadByte < 108) {
            return ((leadByte - 81) << 8) | (bytes[pos] & 255);
        }
        if (leadByte < 126) {
            return (((leadByte - 108) << 16) | ((bytes[pos] & 255) << 8)) | (bytes[pos + 1] & 255);
        }
        if (leadByte == 126) {
            return (((bytes[pos] & 255) << 16) | ((bytes[pos + 1] & 255) << 8)) | (bytes[pos + 2] & 255);
        }
        return (((bytes[pos] << 24) | ((bytes[pos + 1] & 255) << 16)) | ((bytes[pos + 2] & 255) << 8)) | (bytes[pos + 3] & 255);
    }

    private static int skipValue(int pos, int leadByte) {
        if (leadByte < 162) {
            return pos;
        }
        if (leadByte < 216) {
            return pos + 1;
        }
        if (leadByte < 252) {
            return pos + 2;
        }
        return pos + (3 + ((leadByte >> 1) & 1));
    }

    private static int skipValue(byte[] bytes, int pos) {
        return skipValue(pos + 1, bytes[pos] & 255);
    }

    private static int jumpByDelta(byte[] bytes, int pos) {
        int pos2 = pos + 1;
        pos = bytes[pos] & 255;
        if (pos >= 192) {
            if (pos < 240) {
                pos = ((pos - 192) << 8) | (bytes[pos2] & 255);
                pos2++;
            } else if (pos < 254) {
                pos = (((pos - 240) << 16) | ((bytes[pos2] & 255) << 8)) | (bytes[pos2 + 1] & 255);
                pos2 += 2;
            } else if (pos == 254) {
                pos = (((bytes[pos2] & 255) << 16) | ((bytes[pos2 + 1] & 255) << 8)) | (bytes[pos2 + 2] & 255);
                pos2 += 3;
            } else {
                pos = (((bytes[pos2] << 24) | ((bytes[pos2 + 1] & 255) << 16)) | ((bytes[pos2 + 2] & 255) << 8)) | (bytes[pos2 + 3] & 255);
                pos2 += 4;
            }
        }
        return pos2 + pos;
    }

    private static int skipDelta(byte[] bytes, int pos) {
        int pos2 = pos + 1;
        pos = bytes[pos] & 255;
        if (pos < 192) {
            return pos2;
        }
        if (pos < 240) {
            return pos2 + 1;
        }
        if (pos < 254) {
            return pos2 + 2;
        }
        return pos2 + (3 + (pos & 1));
    }

    private Result branchNext(int pos, int length, int inByte) {
        int pos2;
        if (length == 0) {
            length = this.bytes_[pos] & 255;
            pos++;
        }
        length++;
        while (length > 5) {
            pos2 = pos + 1;
            if (inByte < (this.bytes_[pos] & 255)) {
                length >>= 1;
                pos = jumpByDelta(this.bytes_, pos2);
            } else {
                length -= length >> 1;
                pos = skipDelta(this.bytes_, pos2);
            }
        }
        do {
            pos2 = pos + 1;
            if (inByte == (this.bytes_[pos] & 255)) {
                Result result;
                pos = this.bytes_[pos2] & 255;
                if ((pos & 1) != 0) {
                    result = Result.FINAL_VALUE;
                } else {
                    int delta;
                    pos2++;
                    pos >>= 1;
                    if (pos < 81) {
                        delta = pos - 16;
                    } else if (pos < 108) {
                        delta = ((pos - 81) << 8) | (this.bytes_[pos2] & 255);
                        pos2++;
                    } else if (pos < 126) {
                        delta = (((pos - 108) << 16) | ((this.bytes_[pos2] & 255) << 8)) | (this.bytes_[pos2 + 1] & 255);
                        pos2 += 2;
                    } else if (pos == 126) {
                        delta = (((this.bytes_[pos2] & 255) << 16) | ((this.bytes_[pos2 + 1] & 255) << 8)) | (this.bytes_[pos2 + 2] & 255);
                        pos2 += 3;
                    } else {
                        delta = (((this.bytes_[pos2] << 24) | ((this.bytes_[pos2 + 1] & 255) << 16)) | ((this.bytes_[pos2 + 2] & 255) << 8)) | (this.bytes_[pos2 + 3] & 255);
                        pos2 += 4;
                    }
                    pos2 += delta;
                    pos = this.bytes_[pos2] & 255;
                    result = pos >= 32 ? valueResults_[pos & 1] : Result.NO_VALUE;
                }
                this.pos_ = pos2;
                return result;
            }
            length--;
            pos = skipValue(this.bytes_, pos2);
        } while (length > 1);
        pos2 = pos + 1;
        if (inByte == (this.bytes_[pos] & 255)) {
            this.pos_ = pos2;
            pos = this.bytes_[pos2] & 255;
            return pos >= 32 ? valueResults_[pos & 1] : Result.NO_VALUE;
        }
        stop();
        return Result.NO_MATCH;
    }

    private Result nextImpl(int pos, int inByte) {
        while (true) {
            int pos2 = pos + 1;
            pos = this.bytes_[pos] & 255;
            if (pos < 16) {
                return branchNext(pos2, pos, inByte);
            }
            if (pos < 32) {
                int length = pos - 16;
                int pos3 = pos2 + 1;
                if (inByte == (this.bytes_[pos2] & 255)) {
                    Result result;
                    length--;
                    this.remainingMatchLength_ = length;
                    this.pos_ = pos3;
                    if (length < 0) {
                        pos2 = this.bytes_[pos3] & 255;
                        pos = pos2;
                        if (pos2 >= 32) {
                            result = valueResults_[pos & 1];
                            return result;
                        }
                    }
                    result = Result.NO_VALUE;
                    return result;
                }
                pos2 = pos3;
            } else if ((pos & 1) != 0) {
                break;
            } else {
                pos = skipValue(pos2, pos);
            }
        }
        stop();
        return Result.NO_MATCH;
    }

    private static long findUniqueValueFromBranch(byte[] bytes, int pos, int length, long uniqueValue) {
        while (length > 5) {
            pos++;
            uniqueValue = findUniqueValueFromBranch(bytes, jumpByDelta(bytes, pos), length >> 1, uniqueValue);
            if (uniqueValue == 0) {
                return 0;
            }
            length -= length >> 1;
            pos = skipDelta(bytes, pos);
        }
        while (true) {
            pos++;
            int pos2 = pos + 1;
            pos = bytes[pos] & 255;
            boolean isFinal = (pos & 1) != 0;
            int value = readValue(bytes, pos2, pos >> 1);
            pos2 = skipValue(pos2, pos);
            if (!isFinal) {
                uniqueValue = findUniqueValue(bytes, pos2 + value, uniqueValue);
                if (uniqueValue == 0) {
                    return 0;
                }
            } else if (uniqueValue == 0) {
                uniqueValue = (((long) value) << 1) | 1;
            } else if (value != ((int) (uniqueValue >> 1))) {
                return 0;
            }
            length--;
            if (length <= 1) {
                return (((long) (pos2 + 1)) << 33) | (8589934591L & uniqueValue);
            }
            pos = pos2;
        }
    }

    private static long findUniqueValue(byte[] bytes, int pos, long uniqueValue) {
        while (true) {
            int pos2 = pos + 1;
            pos = bytes[pos] & 255;
            if (pos < 16) {
                if (pos == 0) {
                    pos = bytes[pos2] & 255;
                    pos2++;
                }
                uniqueValue = findUniqueValueFromBranch(bytes, pos2, pos + 1, uniqueValue);
                if (uniqueValue == 0) {
                    return 0;
                }
                pos2 = (int) (uniqueValue >>> 33);
            } else if (pos < 32) {
                pos2 += (pos - 16) + 1;
            } else {
                boolean isFinal = (pos & 1) != 0;
                int value = readValue(bytes, pos2, pos >> 1);
                if (uniqueValue == 0) {
                    uniqueValue = (((long) value) << 1) | 1;
                } else if (value != ((int) (uniqueValue >> 1))) {
                    return 0;
                }
                if (isFinal) {
                    return uniqueValue;
                }
                pos = skipValue(pos2, pos);
            }
            pos = pos2;
        }
    }

    private static void getNextBranchBytes(byte[] bytes, int pos, int length, Appendable out) {
        while (length > 5) {
            pos++;
            getNextBranchBytes(bytes, jumpByDelta(bytes, pos), length >> 1, out);
            length -= length >> 1;
            pos = skipDelta(bytes, pos);
        }
        do {
            int pos2 = pos + 1;
            append(out, bytes[pos] & 255);
            pos = skipValue(bytes, pos2);
            length--;
        } while (length > 1);
        append(out, bytes[pos] & 255);
    }

    private static void append(Appendable out, int c) {
        try {
            out.append((char) c);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }
}
