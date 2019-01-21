package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class BitSet implements Cloneable, Serializable {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int ADDRESS_BITS_PER_WORD = 6;
    private static final int BITS_PER_WORD = 64;
    private static final int BIT_INDEX_MASK = 63;
    private static final long WORD_MASK = -1;
    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[]{new ObjectStreamField("bits", long[].class)};
    private static final long serialVersionUID = 7997698588986878753L;
    private transient boolean sizeIsSticky = $assertionsDisabled;
    private long[] words;
    private transient int wordsInUse = 0;

    private static int wordIndex(int bitIndex) {
        return bitIndex >> 6;
    }

    private void checkInvariants() {
    }

    private void recalculateWordsInUse() {
        int i = this.wordsInUse - 1;
        while (i >= 0 && this.words[i] == 0) {
            i--;
        }
        this.wordsInUse = i + 1;
    }

    public BitSet() {
        initWords(64);
        this.sizeIsSticky = $assertionsDisabled;
    }

    public BitSet(int nbits) {
        if (nbits >= 0) {
            initWords(nbits);
            this.sizeIsSticky = true;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("nbits < 0: ");
        stringBuilder.append(nbits);
        throw new NegativeArraySizeException(stringBuilder.toString());
    }

    private void initWords(int nbits) {
        this.words = new long[(wordIndex(nbits - 1) + 1)];
    }

    private BitSet(long[] words) {
        this.words = words;
        this.wordsInUse = words.length;
        checkInvariants();
    }

    public static BitSet valueOf(long[] longs) {
        int n = longs.length;
        while (n > 0 && longs[n - 1] == 0) {
            n--;
        }
        return new BitSet(Arrays.copyOf(longs, n));
    }

    public static BitSet valueOf(LongBuffer lb) {
        lb = lb.slice();
        int n = lb.remaining();
        while (n > 0 && lb.get(n - 1) == 0) {
            n--;
        }
        long[] words = new long[n];
        lb.get(words);
        return new BitSet(words);
    }

    public static BitSet valueOf(byte[] bytes) {
        return valueOf(ByteBuffer.wrap(bytes));
    }

    public static BitSet valueOf(ByteBuffer bb) {
        int i;
        bb = bb.slice().order(ByteOrder.LITTLE_ENDIAN);
        int n = bb.remaining();
        while (n > 0 && bb.get(n - 1) == (byte) 0) {
            n--;
        }
        long[] words = new long[((n + 7) / 8)];
        bb.limit(n);
        int j = 0;
        int i2 = 0;
        while (bb.remaining() >= 8) {
            i = i2 + 1;
            words[i2] = bb.getLong();
            i2 = i;
        }
        i = bb.remaining();
        while (j < i) {
            words[i2] = words[i2] | ((((long) bb.get()) & 255) << (8 * j));
            j++;
        }
        return new BitSet(words);
    }

    public byte[] toByteArray() {
        int n = this.wordsInUse;
        int i = 0;
        if (n == 0) {
            return new byte[0];
        }
        int len = (n - 1) * 8;
        for (long x = this.words[n - 1]; x != 0; x >>>= 8) {
            len++;
        }
        byte[] bytes = new byte[len];
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        while (i < n - 1) {
            bb.putLong(this.words[i]);
            i++;
        }
        for (long x2 = this.words[n - 1]; x2 != 0; x2 >>>= 8) {
            bb.put((byte) ((int) (255 & x2)));
        }
        return bytes;
    }

    public long[] toLongArray() {
        return Arrays.copyOf(this.words, this.wordsInUse);
    }

    private void ensureCapacity(int wordsRequired) {
        if (this.words.length < wordsRequired) {
            this.words = Arrays.copyOf(this.words, Math.max(2 * this.words.length, wordsRequired));
            this.sizeIsSticky = $assertionsDisabled;
        }
    }

    private void expandTo(int wordIndex) {
        int wordsRequired = wordIndex + 1;
        if (this.wordsInUse < wordsRequired) {
            ensureCapacity(wordsRequired);
            this.wordsInUse = wordsRequired;
        }
    }

    private static void checkRange(int fromIndex, int toIndex) {
        StringBuilder stringBuilder;
        if (fromIndex < 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("fromIndex < 0: ");
            stringBuilder.append(fromIndex);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        } else if (toIndex < 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("toIndex < 0: ");
            stringBuilder.append(toIndex);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        } else if (fromIndex > toIndex) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("fromIndex: ");
            stringBuilder.append(fromIndex);
            stringBuilder.append(" > toIndex: ");
            stringBuilder.append(toIndex);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        }
    }

    public void flip(int bitIndex) {
        if (bitIndex >= 0) {
            int wordIndex = wordIndex(bitIndex);
            expandTo(wordIndex);
            long[] jArr = this.words;
            jArr[wordIndex] = jArr[wordIndex] ^ (1 << bitIndex);
            recalculateWordsInUse();
            checkInvariants();
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bitIndex < 0: ");
        stringBuilder.append(bitIndex);
        throw new IndexOutOfBoundsException(stringBuilder.toString());
    }

    public void flip(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        if (fromIndex != toIndex) {
            int startWordIndex = wordIndex(fromIndex);
            int endWordIndex = wordIndex(toIndex - 1);
            expandTo(endWordIndex);
            long firstWordMask = -1 << fromIndex;
            long lastWordMask = -1 >>> (-toIndex);
            long[] jArr;
            if (startWordIndex == endWordIndex) {
                jArr = this.words;
                jArr[startWordIndex] = jArr[startWordIndex] ^ (firstWordMask & lastWordMask);
            } else {
                jArr = this.words;
                jArr[startWordIndex] = jArr[startWordIndex] ^ firstWordMask;
                for (int i = startWordIndex + 1; i < endWordIndex; i++) {
                    long[] jArr2 = this.words;
                    jArr2[i] = ~jArr2[i];
                }
                jArr = this.words;
                jArr[endWordIndex] = jArr[endWordIndex] ^ lastWordMask;
            }
            recalculateWordsInUse();
            checkInvariants();
        }
    }

    public void set(int bitIndex) {
        if (bitIndex >= 0) {
            int wordIndex = wordIndex(bitIndex);
            expandTo(wordIndex);
            long[] jArr = this.words;
            jArr[wordIndex] = jArr[wordIndex] | (1 << bitIndex);
            checkInvariants();
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bitIndex < 0: ");
        stringBuilder.append(bitIndex);
        throw new IndexOutOfBoundsException(stringBuilder.toString());
    }

    public void set(int bitIndex, boolean value) {
        if (value) {
            set(bitIndex);
        } else {
            clear(bitIndex);
        }
    }

    public void set(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        if (fromIndex != toIndex) {
            int startWordIndex = wordIndex(fromIndex);
            int endWordIndex = wordIndex(toIndex - 1);
            expandTo(endWordIndex);
            long firstWordMask = -1 << fromIndex;
            long lastWordMask = -1 >>> (-toIndex);
            long[] jArr;
            if (startWordIndex == endWordIndex) {
                jArr = this.words;
                jArr[startWordIndex] = jArr[startWordIndex] | (firstWordMask & lastWordMask);
            } else {
                long[] jArr2 = this.words;
                jArr2[startWordIndex] = jArr2[startWordIndex] | firstWordMask;
                for (int i = startWordIndex + 1; i < endWordIndex; i++) {
                    this.words[i] = -1;
                }
                jArr = this.words;
                jArr[endWordIndex] = jArr[endWordIndex] | lastWordMask;
            }
            checkInvariants();
        }
    }

    public void set(int fromIndex, int toIndex, boolean value) {
        if (value) {
            set(fromIndex, toIndex);
        } else {
            clear(fromIndex, toIndex);
        }
    }

    public void clear(int bitIndex) {
        if (bitIndex >= 0) {
            int wordIndex = wordIndex(bitIndex);
            if (wordIndex < this.wordsInUse) {
                long[] jArr = this.words;
                jArr[wordIndex] = jArr[wordIndex] & (~(1 << bitIndex));
                recalculateWordsInUse();
                checkInvariants();
                return;
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bitIndex < 0: ");
        stringBuilder.append(bitIndex);
        throw new IndexOutOfBoundsException(stringBuilder.toString());
    }

    public void clear(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        if (fromIndex != toIndex) {
            int startWordIndex = wordIndex(fromIndex);
            if (startWordIndex < this.wordsInUse) {
                int endWordIndex = wordIndex(toIndex - 1);
                if (endWordIndex >= this.wordsInUse) {
                    toIndex = length();
                    endWordIndex = this.wordsInUse - 1;
                }
                long firstWordMask = -1 << fromIndex;
                long lastWordMask = -1 >>> (-toIndex);
                long[] jArr;
                if (startWordIndex == endWordIndex) {
                    jArr = this.words;
                    jArr[startWordIndex] = jArr[startWordIndex] & (~(firstWordMask & lastWordMask));
                } else {
                    jArr = this.words;
                    jArr[startWordIndex] = jArr[startWordIndex] & (~firstWordMask);
                    for (int i = startWordIndex + 1; i < endWordIndex; i++) {
                        this.words[i] = 0;
                    }
                    jArr = this.words;
                    jArr[endWordIndex] = jArr[endWordIndex] & (~lastWordMask);
                }
                recalculateWordsInUse();
                checkInvariants();
            }
        }
    }

    public void clear() {
        while (this.wordsInUse > 0) {
            long[] jArr = this.words;
            int i = this.wordsInUse - 1;
            this.wordsInUse = i;
            jArr[i] = 0;
        }
    }

    public boolean get(int bitIndex) {
        if (bitIndex >= 0) {
            checkInvariants();
            int wordIndex = wordIndex(bitIndex);
            return (wordIndex >= this.wordsInUse || (this.words[wordIndex] & (1 << bitIndex)) == 0) ? $assertionsDisabled : true;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bitIndex < 0: ");
            stringBuilder.append(bitIndex);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        }
    }

    public BitSet get(int fromIndex, int toIndex) {
        int i = fromIndex;
        int toIndex2 = toIndex;
        checkRange(fromIndex, toIndex);
        checkInvariants();
        int len = length();
        int i2 = 0;
        if (len <= i || i == toIndex2) {
            return new BitSet(0);
        }
        long j;
        if (toIndex2 > len) {
            toIndex2 = len;
        }
        BitSet result = new BitSet(toIndex2 - i);
        boolean wordAligned = true;
        int targetWords = wordIndex((toIndex2 - i) - 1) + 1;
        int sourceIndex = wordIndex(fromIndex);
        if ((i & BIT_INDEX_MASK) != 0) {
            wordAligned = $assertionsDisabled;
        }
        while (i2 < targetWords - 1) {
            long j2;
            long[] jArr = result.words;
            if (wordAligned) {
                j2 = this.words[sourceIndex];
            } else {
                j2 = (this.words[sourceIndex] >>> i) | (this.words[sourceIndex + 1] << (-i));
            }
            jArr[i2] = j2;
            i2++;
            sourceIndex++;
        }
        long lastWordMask = -1 >>> (-toIndex2);
        long[] jArr2 = result.words;
        int i3 = targetWords - 1;
        if (((toIndex2 - 1) & BIT_INDEX_MASK) < (i & BIT_INDEX_MASK)) {
            j = (this.words[sourceIndex] >>> i) | ((this.words[sourceIndex + 1] & lastWordMask) << (-i));
        } else {
            j = (this.words[sourceIndex] & lastWordMask) >>> i;
        }
        jArr2[i3] = j;
        result.wordsInUse = targetWords;
        result.recalculateWordsInUse();
        result.checkInvariants();
        return result;
    }

    public int nextSetBit(int fromIndex) {
        if (fromIndex >= 0) {
            checkInvariants();
            int u = wordIndex(fromIndex);
            if (u >= this.wordsInUse) {
                return -1;
            }
            long word = this.words[u] & (-1 << fromIndex);
            while (word == 0) {
                u++;
                if (u == this.wordsInUse) {
                    return -1;
                }
                word = this.words[u];
            }
            return (u * 64) + Long.numberOfTrailingZeros(word);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("fromIndex < 0: ");
        stringBuilder.append(fromIndex);
        throw new IndexOutOfBoundsException(stringBuilder.toString());
    }

    public int nextClearBit(int fromIndex) {
        if (fromIndex >= 0) {
            checkInvariants();
            int u = wordIndex(fromIndex);
            if (u >= this.wordsInUse) {
                return fromIndex;
            }
            long word = (~this.words[u]) & (-1 << fromIndex);
            while (word == 0) {
                u++;
                if (u == this.wordsInUse) {
                    return this.wordsInUse * 64;
                }
                word = ~this.words[u];
            }
            return (u * 64) + Long.numberOfTrailingZeros(word);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("fromIndex < 0: ");
        stringBuilder.append(fromIndex);
        throw new IndexOutOfBoundsException(stringBuilder.toString());
    }

    public int previousSetBit(int fromIndex) {
        if (fromIndex >= 0) {
            checkInvariants();
            int u = wordIndex(fromIndex);
            if (u >= this.wordsInUse) {
                return length() - 1;
            }
            long word = this.words[u] & (-1 >>> (-(fromIndex + 1)));
            while (word == 0) {
                int u2 = u - 1;
                if (u == 0) {
                    return -1;
                }
                word = this.words[u2];
                u = u2;
            }
            return (((u + 1) * 64) - 1) - Long.numberOfLeadingZeros(word);
        } else if (fromIndex == -1) {
            return -1;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fromIndex < -1: ");
            stringBuilder.append(fromIndex);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        }
    }

    public int previousClearBit(int fromIndex) {
        if (fromIndex >= 0) {
            checkInvariants();
            int u = wordIndex(fromIndex);
            if (u >= this.wordsInUse) {
                return fromIndex;
            }
            long word = (~this.words[u]) & (-1 >>> (-(fromIndex + 1)));
            while (word == 0) {
                int u2 = u - 1;
                if (u == 0) {
                    return -1;
                }
                word = ~this.words[u2];
                u = u2;
            }
            return (((u + 1) * 64) - 1) - Long.numberOfLeadingZeros(word);
        } else if (fromIndex == -1) {
            return -1;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fromIndex < -1: ");
            stringBuilder.append(fromIndex);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        }
    }

    public int length() {
        if (this.wordsInUse == 0) {
            return 0;
        }
        return ((this.wordsInUse - 1) * 64) + (64 - Long.numberOfLeadingZeros(this.words[this.wordsInUse - 1]));
    }

    public boolean isEmpty() {
        return this.wordsInUse == 0 ? true : $assertionsDisabled;
    }

    public boolean intersects(BitSet set) {
        for (int i = Math.min(this.wordsInUse, set.wordsInUse) - 1; i >= 0; i--) {
            if ((this.words[i] & set.words[i]) != 0) {
                return true;
            }
        }
        return $assertionsDisabled;
    }

    public int cardinality() {
        int sum = 0;
        for (int i = 0; i < this.wordsInUse; i++) {
            sum += Long.bitCount(this.words[i]);
        }
        return sum;
    }

    public void and(BitSet set) {
        if (this != set) {
            while (this.wordsInUse > set.wordsInUse) {
                long[] jArr = this.words;
                int i = this.wordsInUse - 1;
                this.wordsInUse = i;
                jArr[i] = 0;
            }
            for (int i2 = 0; i2 < this.wordsInUse; i2++) {
                long[] jArr2 = this.words;
                jArr2[i2] = jArr2[i2] & set.words[i2];
            }
            recalculateWordsInUse();
            checkInvariants();
        }
    }

    public void or(BitSet set) {
        if (this != set) {
            int wordsInCommon = Math.min(this.wordsInUse, set.wordsInUse);
            if (this.wordsInUse < set.wordsInUse) {
                ensureCapacity(set.wordsInUse);
                this.wordsInUse = set.wordsInUse;
            }
            for (int i = 0; i < wordsInCommon; i++) {
                long[] jArr = this.words;
                jArr[i] = jArr[i] | set.words[i];
            }
            if (wordsInCommon < set.wordsInUse) {
                System.arraycopy(set.words, wordsInCommon, this.words, wordsInCommon, this.wordsInUse - wordsInCommon);
            }
            checkInvariants();
        }
    }

    public void xor(BitSet set) {
        int wordsInCommon = Math.min(this.wordsInUse, set.wordsInUse);
        if (this.wordsInUse < set.wordsInUse) {
            ensureCapacity(set.wordsInUse);
            this.wordsInUse = set.wordsInUse;
        }
        for (int i = 0; i < wordsInCommon; i++) {
            long[] jArr = this.words;
            jArr[i] = jArr[i] ^ set.words[i];
        }
        if (wordsInCommon < set.wordsInUse) {
            System.arraycopy(set.words, wordsInCommon, this.words, wordsInCommon, set.wordsInUse - wordsInCommon);
        }
        recalculateWordsInUse();
        checkInvariants();
    }

    public void andNot(BitSet set) {
        for (int i = Math.min(this.wordsInUse, set.wordsInUse) - 1; i >= 0; i--) {
            long[] jArr = this.words;
            jArr[i] = jArr[i] & (~set.words[i]);
        }
        recalculateWordsInUse();
        checkInvariants();
    }

    public int hashCode() {
        long h = 1234;
        int i = this.wordsInUse;
        while (true) {
            i--;
            if (i < 0) {
                return (int) ((h >> 32) ^ h);
            }
            h ^= this.words[i] * ((long) (i + 1));
        }
    }

    public int size() {
        return this.words.length * 64;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof BitSet)) {
            return $assertionsDisabled;
        }
        if (this == obj) {
            return true;
        }
        BitSet set = (BitSet) obj;
        checkInvariants();
        set.checkInvariants();
        if (this.wordsInUse != set.wordsInUse) {
            return $assertionsDisabled;
        }
        for (int i = 0; i < this.wordsInUse; i++) {
            if (this.words[i] != set.words[i]) {
                return $assertionsDisabled;
            }
        }
        return true;
    }

    public Object clone() {
        if (!this.sizeIsSticky) {
            trimToSize();
        }
        try {
            BitSet result = (BitSet) super.clone();
            result.words = (long[]) this.words.clone();
            result.checkInvariants();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    private void trimToSize() {
        if (this.wordsInUse != this.words.length) {
            this.words = Arrays.copyOf(this.words, this.wordsInUse);
            checkInvariants();
        }
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        checkInvariants();
        if (!this.sizeIsSticky) {
            trimToSize();
        }
        s.putFields().put("bits", this.words);
        s.writeFields();
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        this.words = (long[]) s.readFields().get("bits", null);
        this.wordsInUse = this.words.length;
        recalculateWordsInUse();
        boolean z = true;
        if (this.words.length <= 0 || this.words[this.words.length - 1] != 0) {
            z = $assertionsDisabled;
        }
        this.sizeIsSticky = z;
        checkInvariants();
    }

    public String toString() {
        checkInvariants();
        StringBuilder b = new StringBuilder((6 * (this.wordsInUse > 128 ? cardinality() : this.wordsInUse * 64)) + 2);
        b.append('{');
        int i = nextSetBit(0);
        if (i != -1) {
            b.append(i);
            while (true) {
                i++;
                if (i < 0) {
                    break;
                }
                int nextSetBit = nextSetBit(i);
                i = nextSetBit;
                if (nextSetBit < 0) {
                    break;
                }
                nextSetBit = nextClearBit(i);
                do {
                    b.append(", ");
                    b.append(i);
                    i++;
                } while (i != nextSetBit);
            }
        }
        b.append('}');
        return b.toString();
    }

    public IntStream stream() {
        return StreamSupport.intStream(new -$$Lambda$BitSet$ifk7HV8-2uu42BYsPVrvRaHrugk(this), 16469, $assertionsDisabled);
    }
}
