package android.icu.impl;

import android.icu.impl.Trie2.Range;
import android.icu.text.UTF16;
import dalvik.bytecode.Opcodes;
import java.io.PrintStream;
import java.util.Iterator;

public class Trie2Writable extends Trie2 {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int UNEWTRIE2_DATA_0800_OFFSET = 2176;
    private static final int UNEWTRIE2_DATA_NULL_OFFSET = 192;
    private static final int UNEWTRIE2_DATA_START_OFFSET = 256;
    private static final int UNEWTRIE2_INDEX_2_NULL_OFFSET = 2656;
    private static final int UNEWTRIE2_INDEX_2_START_OFFSET = 2720;
    private static final int UNEWTRIE2_INITIAL_DATA_LENGTH = 16384;
    private static final int UNEWTRIE2_MEDIUM_DATA_LENGTH = 131072;
    private static final int UTRIE2_MAX_DATA_LENGTH = 262140;
    private static final int UTRIE2_MAX_INDEX_LENGTH = 65535;
    private boolean UTRIE2_DEBUG;
    private int[] data;
    private int dataCapacity;
    private int firstFreeBlock;
    private int[] index1;
    private int[] index2;
    private int index2Length;
    private int index2NullOffset;
    private boolean isCompacted;
    private int[] map;

    public Trie2Writable(int initialValueP, int errorValueP) {
        this.index1 = new int[544];
        this.index2 = new int[35488];
        this.map = new int[34852];
        this.UTRIE2_DEBUG = false;
        init(initialValueP, errorValueP);
    }

    private void init(int initialValueP, int errorValueP) {
        int i;
        this.initialValue = initialValueP;
        this.errorValue = errorValueP;
        this.highStart = 1114112;
        this.data = new int[16384];
        this.dataCapacity = 16384;
        this.initialValue = initialValueP;
        this.errorValue = errorValueP;
        this.highStart = 1114112;
        this.firstFreeBlock = 0;
        this.isCompacted = false;
        int i2 = 0;
        while (i2 < 128) {
            this.data[i2] = this.initialValue;
            i2++;
        }
        while (i2 < 192) {
            this.data[i2] = this.errorValue;
            i2++;
        }
        for (i2 = 192; i2 < 256; i2++) {
            this.data[i2] = this.initialValue;
        }
        this.dataNullOffset = 192;
        this.dataLength = 256;
        int i3 = 0;
        i2 = 0;
        while (i2 < 128) {
            this.index2[i3] = i2;
            this.map[i3] = 1;
            i3++;
            i2 += 32;
        }
        while (i2 < 192) {
            this.map[i3] = 0;
            i3++;
            i2 += 32;
        }
        int i4 = i3 + 1;
        this.map[i3] = 34845;
        for (i2 += 32; i2 < 256; i2 += 32) {
            this.map[i4] = 0;
            i4++;
        }
        for (i = 4; i < 2080; i++) {
            this.index2[i] = 192;
        }
        for (i = 0; i < 576; i++) {
            this.index2[2080 + i] = -1;
        }
        for (i = 0; i < 64; i++) {
            this.index2[UNEWTRIE2_INDEX_2_NULL_OFFSET + i] = 192;
        }
        this.index2NullOffset = UNEWTRIE2_INDEX_2_NULL_OFFSET;
        this.index2Length = UNEWTRIE2_INDEX_2_START_OFFSET;
        i = 0;
        i2 = 0;
        while (i < 32) {
            this.index1[i] = i2;
            i++;
            i2 += 64;
        }
        while (i < 544) {
            this.index1[i] = UNEWTRIE2_INDEX_2_NULL_OFFSET;
            i++;
        }
        for (i = 128; i < 2048; i += 32) {
            set(i, this.initialValue);
        }
    }

    public Trie2Writable(Trie2 source) {
        this.index1 = new int[544];
        this.index2 = new int[35488];
        this.map = new int[34852];
        this.UTRIE2_DEBUG = false;
        init(source.initialValue, source.errorValue);
        Iterator it = source.iterator();
        while (it.hasNext()) {
            setRange((Range) it.next(), true);
        }
    }

    private boolean isInNullBlock(int c, boolean forLSCP) {
        int i2;
        if (Character.isHighSurrogate((char) c) && forLSCP) {
            i2 = 320 + (c >> 5);
        } else {
            i2 = this.index1[c >> 11] + ((c >> 5) & 63);
        }
        return this.index2[i2] == this.dataNullOffset;
    }

    private int allocIndex2Block() {
        int newBlock = this.index2Length;
        int newTop = newBlock + 64;
        if (newTop <= this.index2.length) {
            this.index2Length = newTop;
            System.arraycopy(this.index2, this.index2NullOffset, this.index2, newBlock, 64);
            return newBlock;
        }
        throw new IllegalStateException("Internal error in Trie2 creation.");
    }

    private int getIndex2Block(int c, boolean forLSCP) {
        if (c >= 55296 && c < UTF16.TRAIL_SURROGATE_MIN_VALUE && forLSCP) {
            return 2048;
        }
        int i1 = c >> 11;
        int i2 = this.index1[i1];
        if (i2 == this.index2NullOffset) {
            i2 = allocIndex2Block();
            this.index1[i1] = i2;
        }
        return i2;
    }

    private int allocDataBlock(int copyBlock) {
        int newBlock;
        if (this.firstFreeBlock != 0) {
            newBlock = this.firstFreeBlock;
            this.firstFreeBlock = -this.map[newBlock >> 5];
        } else {
            newBlock = this.dataLength;
            int newTop = newBlock + 32;
            if (newTop > this.dataCapacity) {
                int capacity;
                if (this.dataCapacity < 131072) {
                    capacity = 131072;
                } else if (this.dataCapacity < 1115264) {
                    capacity = 1115264;
                } else {
                    throw new IllegalStateException("Internal error in Trie2 creation.");
                }
                int[] newData = new int[capacity];
                System.arraycopy(this.data, 0, newData, 0, this.dataLength);
                this.data = newData;
                this.dataCapacity = capacity;
            }
            this.dataLength = newTop;
        }
        System.arraycopy(this.data, copyBlock, this.data, newBlock, 32);
        this.map[newBlock >> 5] = 0;
        return newBlock;
    }

    private void releaseDataBlock(int block) {
        this.map[block >> 5] = -this.firstFreeBlock;
        this.firstFreeBlock = block;
    }

    private boolean isWritableBlock(int block) {
        return block != this.dataNullOffset && 1 == this.map[block >> 5];
    }

    private void setIndex2Entry(int i2, int block) {
        int[] iArr = this.map;
        int i = block >> 5;
        iArr[i] = iArr[i] + 1;
        int oldBlock = this.index2[i2];
        int[] iArr2 = this.map;
        int i3 = oldBlock >> 5;
        int i4 = iArr2[i3] - 1;
        iArr2[i3] = i4;
        if (i4 == 0) {
            releaseDataBlock(oldBlock);
        }
        this.index2[i2] = block;
    }

    private int getDataBlock(int c, boolean forLSCP) {
        int i2 = getIndex2Block(c, forLSCP) + ((c >> 5) & 63);
        int oldBlock = this.index2[i2];
        if (isWritableBlock(oldBlock)) {
            return oldBlock;
        }
        int newBlock = allocDataBlock(oldBlock);
        setIndex2Entry(i2, newBlock);
        return newBlock;
    }

    public Trie2Writable set(int c, int value) {
        if (c < 0 || c > 1114111) {
            throw new IllegalArgumentException("Invalid code point.");
        }
        set(c, true, value);
        this.fHash = 0;
        return this;
    }

    private Trie2Writable set(int c, boolean forLSCP, int value) {
        if (this.isCompacted) {
            uncompact();
        }
        this.data[(c & 31) + getDataBlock(c, forLSCP)] = value;
        return this;
    }

    private void uncompact() {
        Trie2Writable tempTrie = new Trie2Writable(this);
        this.index1 = tempTrie.index1;
        this.index2 = tempTrie.index2;
        this.data = tempTrie.data;
        this.index2Length = tempTrie.index2Length;
        this.dataCapacity = tempTrie.dataCapacity;
        this.isCompacted = tempTrie.isCompacted;
        this.header = tempTrie.header;
        this.index = tempTrie.index;
        this.data16 = tempTrie.data16;
        this.data32 = tempTrie.data32;
        this.indexLength = tempTrie.indexLength;
        this.dataLength = tempTrie.dataLength;
        this.index2NullOffset = tempTrie.index2NullOffset;
        this.initialValue = tempTrie.initialValue;
        this.errorValue = tempTrie.errorValue;
        this.highStart = tempTrie.highStart;
        this.highValueIndex = tempTrie.highValueIndex;
        this.dataNullOffset = tempTrie.dataNullOffset;
    }

    private void writeBlock(int block, int value) {
        int limit = block + 32;
        while (block < limit) {
            int block2 = block + 1;
            this.data[block] = value;
            block = block2;
        }
    }

    private void fillBlock(int block, int start, int limit, int value, int initialValue, boolean overwrite) {
        int pLimit = block + limit;
        int i;
        if (overwrite) {
            for (i = block + start; i < pLimit; i++) {
                this.data[i] = value;
            }
            return;
        }
        for (i = block + start; i < pLimit; i++) {
            if (this.data[i] == initialValue) {
                this.data[i] = value;
            }
        }
    }

    public Trie2Writable setRange(int start, int end, int value, boolean overwrite) {
        int i = start;
        int i2 = end;
        int i3 = value;
        if (i > 1114111 || i < 0 || i2 > 1114111 || i2 < 0 || i > i2) {
            throw new IllegalArgumentException("Invalid code point range.");
        } else if (!overwrite && i3 == this.initialValue) {
            return this;
        } else {
            int block;
            int nextStart;
            int start2;
            int repeatBlock;
            this.fHash = 0;
            if (this.isCompacted) {
                uncompact();
            }
            int limit = i2 + 1;
            boolean block2 = true;
            if ((i & 31) != 0) {
                block = getDataBlock(i, true);
                nextStart = (i + 32) & -32;
                if (nextStart <= limit) {
                    fillBlock(block, i & 31, 32, i3, this.initialValue, overwrite);
                    start2 = nextStart;
                } else {
                    fillBlock(block, i & 31, limit & 31, i3, this.initialValue, overwrite);
                    return this;
                }
            }
            start2 = i;
            i = limit & 31;
            limit &= -32;
            if (i3 == this.initialValue) {
                repeatBlock = this.dataNullOffset;
            } else {
                repeatBlock = -1;
            }
            block = start2;
            while (true) {
                nextStart = repeatBlock;
                if (block >= limit) {
                    break;
                }
                boolean setRepeatBlock = false;
                if (i3 == this.initialValue && isInNullBlock(block, block2)) {
                    block += 32;
                    repeatBlock = nextStart;
                } else {
                    int i22 = getIndex2Block(block, block2) + ((block >> 5) & 63);
                    int block3 = this.index2[i22];
                    int block4;
                    if (!isWritableBlock(block3)) {
                        block4 = block3;
                        i2 = i22;
                        if (this.data[block4] != i3 && (overwrite || block4 == this.dataNullOffset)) {
                            setRepeatBlock = true;
                        }
                    } else if (!overwrite || block3 < UNEWTRIE2_DATA_0800_OFFSET) {
                        i2 = i22;
                        fillBlock(block3, 0, 32, i3, this.initialValue, overwrite);
                    } else {
                        setRepeatBlock = true;
                        block4 = block3;
                        i2 = i22;
                    }
                    if (setRepeatBlock) {
                        if (nextStart >= 0) {
                            setIndex2Entry(i2, nextStart);
                        } else {
                            repeatBlock = getDataBlock(block, true);
                            writeBlock(repeatBlock, i3);
                            block += 32;
                            i2 = end;
                            block2 = true;
                        }
                    }
                    repeatBlock = nextStart;
                    block += 32;
                    i2 = end;
                    block2 = true;
                }
            }
            if (i > 0) {
                fillBlock(getDataBlock(block, true), 0, i, i3, this.initialValue, overwrite);
            }
            return this;
        }
    }

    public Trie2Writable setRange(Range range, boolean overwrite) {
        this.fHash = 0;
        if (range.leadSurrogate) {
            int c = range.startCodePoint;
            while (c <= range.endCodePoint) {
                if (overwrite || getFromU16SingleLead((char) c) == this.initialValue) {
                    setForLeadSurrogateCodeUnit((char) c, range.value);
                }
                c++;
            }
        } else {
            setRange(range.startCodePoint, range.endCodePoint, range.value, overwrite);
        }
        return this;
    }

    public Trie2Writable setForLeadSurrogateCodeUnit(char codeUnit, int value) {
        this.fHash = 0;
        set(codeUnit, false, value);
        return this;
    }

    public int get(int codePoint) {
        if (codePoint < 0 || codePoint > 1114111) {
            return this.errorValue;
        }
        return get(codePoint, true);
    }

    private int get(int c, boolean fromLSCP) {
        if (c >= this.highStart && (c < 55296 || c >= UTF16.TRAIL_SURROGATE_MIN_VALUE || fromLSCP)) {
            return this.data[this.dataLength - 4];
        }
        int i2;
        if (c < 55296 || c >= UTF16.TRAIL_SURROGATE_MIN_VALUE || !fromLSCP) {
            i2 = this.index1[c >> 11] + ((c >> 5) & 63);
        } else {
            i2 = 320 + (c >> 5);
        }
        return this.data[(c & 31) + this.index2[i2]];
    }

    public int getFromU16SingleLead(char c) {
        return get(c, false);
    }

    private boolean equal_int(int[] a, int s, int t, int length) {
        for (int i = 0; i < length; i++) {
            if (a[s + i] != a[t + i]) {
                return false;
            }
        }
        return true;
    }

    private int findSameIndex2Block(int index2Length, int otherBlock) {
        index2Length -= 64;
        for (int block = 0; block <= index2Length; block++) {
            if (equal_int(this.index2, block, otherBlock, 64)) {
                return block;
            }
        }
        return -1;
    }

    private int findSameDataBlock(int dataLength, int otherBlock, int blockLength) {
        dataLength -= blockLength;
        for (int block = 0; block <= dataLength; block += 4) {
            if (equal_int(this.data, block, otherBlock, blockLength)) {
                return block;
            }
        }
        return -1;
    }

    private int findHighStart(int highValue) {
        int prevI2Block;
        int prevBlock;
        if (highValue == this.initialValue) {
            prevI2Block = this.index2NullOffset;
            prevBlock = this.dataNullOffset;
        } else {
            prevI2Block = -1;
            prevBlock = -1;
        }
        int i1 = 544;
        int prevBlock2 = prevBlock;
        prevBlock = prevI2Block;
        prevI2Block = 1114112;
        while (prevI2Block > 0) {
            i1--;
            int i2Block = this.index1[i1];
            if (i2Block == prevBlock) {
                prevI2Block -= 2048;
            } else {
                prevBlock = i2Block;
                if (i2Block != this.index2NullOffset) {
                    int i2 = 64;
                    while (i2 > 0) {
                        i2--;
                        int block = this.index2[i2Block + i2];
                        if (block == prevBlock2) {
                            prevI2Block -= 32;
                        } else {
                            prevBlock2 = block;
                            if (block != this.dataNullOffset) {
                                int j = 32;
                                while (j > 0) {
                                    j--;
                                    if (this.data[block + j] != highValue) {
                                        return prevI2Block;
                                    }
                                    prevI2Block--;
                                }
                                continue;
                            } else if (highValue != this.initialValue) {
                                return prevI2Block;
                            } else {
                                prevI2Block -= 32;
                            }
                        }
                    }
                    continue;
                } else if (highValue != this.initialValue) {
                    return prevI2Block;
                } else {
                    prevI2Block -= 2048;
                }
            }
        }
        return 0;
    }

    private void compactData() {
        int mapIndex;
        int newStart = 192;
        int start = 0;
        int i = 0;
        while (start < 192) {
            this.map[i] = start;
            start += 32;
            i++;
        }
        int blockLength = 64;
        int blockCount = 64 >> 5;
        start = 192;
        while (start < this.dataLength) {
            if (start == UNEWTRIE2_DATA_0800_OFFSET) {
                blockLength = 32;
                blockCount = 1;
            }
            if (this.map[start >> 5] <= 0) {
                start += blockLength;
            } else {
                int movedStart = findSameDataBlock(newStart, start, blockLength);
                int mapIndex2;
                if (movedStart >= 0) {
                    i = blockCount;
                    mapIndex2 = start >> 5;
                    while (i > 0) {
                        int mapIndex3 = mapIndex2 + 1;
                        this.map[mapIndex2] = movedStart;
                        movedStart += 32;
                        i--;
                        mapIndex2 = mapIndex3;
                    }
                    start += blockLength;
                } else {
                    mapIndex2 = blockLength - 4;
                    while (mapIndex2 > 0 && !equal_int(this.data, newStart - mapIndex2, start, mapIndex2)) {
                        mapIndex2 -= 4;
                    }
                    int mapIndex4;
                    if (mapIndex2 > 0 || newStart < start) {
                        movedStart = newStart - mapIndex2;
                        i = blockCount;
                        mapIndex = start >> 5;
                        while (i > 0) {
                            mapIndex4 = mapIndex + 1;
                            this.map[mapIndex] = movedStart;
                            movedStart += 32;
                            i--;
                            mapIndex = mapIndex4;
                        }
                        start += mapIndex2;
                        i = blockLength - mapIndex2;
                        while (i > 0) {
                            mapIndex4 = newStart + 1;
                            int start2 = start + 1;
                            this.data[newStart] = this.data[start];
                            i--;
                            newStart = mapIndex4;
                            start = start2;
                        }
                    } else {
                        i = blockCount;
                        mapIndex = start >> 5;
                        while (i > 0) {
                            mapIndex4 = mapIndex + 1;
                            this.map[mapIndex] = start;
                            start += 32;
                            i--;
                            mapIndex = mapIndex4;
                        }
                        newStart = start;
                    }
                }
            }
        }
        i = 0;
        while (i < this.index2Length) {
            if (i == 2080) {
                i += 576;
            }
            this.index2[i] = this.map[this.index2[i] >> 5];
            i++;
        }
        this.dataNullOffset = this.map[this.dataNullOffset >> 5];
        while ((newStart & 3) != 0) {
            mapIndex = newStart + 1;
            this.data[newStart] = this.initialValue;
            newStart = mapIndex;
        }
        if (this.UTRIE2_DEBUG) {
            System.out.printf("compacting UTrie2: count of 32-bit data words %d->%d%n", new Object[]{Integer.valueOf(this.dataLength), Integer.valueOf(newStart)});
        }
        this.dataLength = newStart;
    }

    private void compactIndex2() {
        int movedStart;
        int start = 0;
        int i = 0;
        while (start < 2080) {
            this.map[i] = start;
            start += 64;
            i++;
        }
        int newStart = 2080 + (32 + ((this.highStart - 65536) >> 11));
        start = UNEWTRIE2_INDEX_2_NULL_OFFSET;
        while (start < this.index2Length) {
            int findSameIndex2Block = findSameIndex2Block(newStart, start);
            movedStart = findSameIndex2Block;
            if (findSameIndex2Block >= 0) {
                this.map[start >> 6] = movedStart;
                start += 64;
            } else {
                findSameIndex2Block = 63;
                while (findSameIndex2Block > 0 && !equal_int(this.index2, newStart - findSameIndex2Block, start, findSameIndex2Block)) {
                    findSameIndex2Block--;
                }
                if (findSameIndex2Block > 0 || newStart < start) {
                    this.map[start >> 6] = newStart - findSameIndex2Block;
                    start += findSameIndex2Block;
                    i = 64 - findSameIndex2Block;
                    while (i > 0) {
                        int newStart2 = newStart + 1;
                        int start2 = start + 1;
                        this.index2[newStart] = this.index2[start];
                        i--;
                        newStart = newStart2;
                        start = start2;
                    }
                } else {
                    this.map[start >> 6] = start;
                    start += 64;
                    newStart = start;
                }
            }
        }
        for (i = 0; i < 544; i++) {
            this.index1[i] = this.map[this.index1[i] >> 6];
        }
        this.index2NullOffset = this.map[this.index2NullOffset >> 6];
        while ((newStart & 3) != 0) {
            movedStart = newStart + 1;
            this.index2[newStart] = UTRIE2_MAX_DATA_LENGTH;
            newStart = movedStart;
        }
        if (this.UTRIE2_DEBUG) {
            System.out.printf("compacting UTrie2: count of 16-bit index-2 words %d->%d%n", new Object[]{Integer.valueOf(this.index2Length), Integer.valueOf(newStart)});
        }
        this.index2Length = newStart;
    }

    private void compactTrie() {
        int highValue = get(1114111);
        int localHighStart = (findHighStart(highValue) + Opcodes.OP_IGET_WIDE_JUMBO) & -2048;
        if (localHighStart == 1114112) {
            highValue = this.errorValue;
        }
        this.highStart = localHighStart;
        if (this.UTRIE2_DEBUG) {
            System.out.printf("UTrie2: highStart U+%04x  highValue 0x%x  initialValue 0x%x%n", new Object[]{Integer.valueOf(this.highStart), Integer.valueOf(highValue), Integer.valueOf(this.initialValue)});
        }
        if (this.highStart < 1114112) {
            setRange(this.highStart <= 65536 ? 65536 : this.highStart, 1114111, this.initialValue, true);
        }
        compactData();
        if (this.highStart > 65536) {
            compactIndex2();
        } else if (this.UTRIE2_DEBUG) {
            System.out.printf("UTrie2: highStart U+%04x  count of 16-bit index-2 words %d->%d%n", new Object[]{Integer.valueOf(this.highStart), Integer.valueOf(this.index2Length), Integer.valueOf(2112)});
        }
        int[] iArr = this.data;
        int i = this.dataLength;
        this.dataLength = i + 1;
        iArr[i] = highValue;
        while ((this.dataLength & 3) != 0) {
            iArr = this.data;
            i = this.dataLength;
            this.dataLength = i + 1;
            iArr[i] = this.initialValue;
        }
        this.isCompacted = true;
    }

    public Trie2_16 toTrie2_16() {
        Trie2_16 frozenTrie = new Trie2_16();
        freeze(frozenTrie, ValueWidth.BITS_16);
        return frozenTrie;
    }

    public Trie2_32 toTrie2_32() {
        Trie2_32 frozenTrie = new Trie2_32();
        freeze(frozenTrie, ValueWidth.BITS_32);
        return frozenTrie;
    }

    private void freeze(Trie2 dest, ValueWidth valueBits) {
        int allIndexesLength;
        int dataMove;
        if (!this.isCompacted) {
            compactTrie();
        }
        if (this.highStart <= 65536) {
            allIndexesLength = 2112;
        } else {
            allIndexesLength = this.index2Length;
        }
        if (valueBits == ValueWidth.BITS_16) {
            dataMove = allIndexesLength;
        } else {
            dataMove = 0;
        }
        if (allIndexesLength > 65535 || this.dataNullOffset + dataMove > 65535 || dataMove + UNEWTRIE2_DATA_0800_OFFSET > 65535 || this.dataLength + dataMove > UTRIE2_MAX_DATA_LENGTH) {
            throw new UnsupportedOperationException("Trie2 data is too large.");
        }
        int destIdx;
        PrintStream printStream;
        StringBuilder stringBuilder;
        int indexLength = allIndexesLength;
        if (valueBits == ValueWidth.BITS_16) {
            indexLength += this.dataLength;
        } else {
            dest.data32 = new int[this.dataLength];
        }
        dest.index = new char[indexLength];
        dest.indexLength = allIndexesLength;
        dest.dataLength = this.dataLength;
        if (this.highStart <= 65536) {
            dest.index2NullOffset = 65535;
        } else {
            dest.index2NullOffset = this.index2NullOffset + 0;
        }
        dest.initialValue = this.initialValue;
        dest.errorValue = this.errorValue;
        dest.highStart = this.highStart;
        dest.highValueIndex = (this.dataLength + dataMove) - 4;
        dest.dataNullOffset = this.dataNullOffset + dataMove;
        dest.header = new UTrie2Header();
        dest.header.signature = 1416784178;
        dest.header.options = valueBits == ValueWidth.BITS_16 ? 0 : 1;
        dest.header.indexLength = dest.indexLength;
        dest.header.shiftedDataLength = dest.dataLength >> 2;
        dest.header.index2NullOffset = dest.index2NullOffset;
        dest.header.dataNullOffset = dest.dataNullOffset;
        dest.header.shiftedHighStart = dest.highStart >> 11;
        int destIdx2 = 0;
        int i = 0;
        while (i < 2080) {
            destIdx = destIdx2 + 1;
            dest.index[destIdx2] = (char) ((this.index2[i] + dataMove) >> 2);
            i++;
            destIdx2 = destIdx;
        }
        if (this.UTRIE2_DEBUG) {
            printStream = System.out;
            stringBuilder = new StringBuilder();
            stringBuilder.append("\n\nIndex2 for BMP limit is ");
            stringBuilder.append(Integer.toHexString(destIdx2));
            printStream.println(stringBuilder.toString());
        }
        i = 0;
        while (i < 2) {
            destIdx = destIdx2 + 1;
            dest.index[destIdx2] = (char) (dataMove + 128);
            i++;
            destIdx2 = destIdx;
        }
        while (i < 32) {
            destIdx = destIdx2 + 1;
            dest.index[destIdx2] = (char) (this.index2[i << 1] + dataMove);
            i++;
            destIdx2 = destIdx;
        }
        if (this.UTRIE2_DEBUG) {
            printStream = System.out;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Index2 for UTF-8 2byte values limit is ");
            stringBuilder.append(Integer.toHexString(destIdx2));
            printStream.println(stringBuilder.toString());
        }
        if (this.highStart > 65536) {
            PrintStream printStream2;
            int index1Length = (this.highStart - 65536) >> 11;
            int index2Offset = 2112 + index1Length;
            i = 0;
            while (i < index1Length) {
                int destIdx3 = destIdx2 + 1;
                dest.index[destIdx2] = (char) (this.index1[i + 32] + 0);
                i++;
                destIdx2 = destIdx3;
            }
            if (this.UTRIE2_DEBUG) {
                printStream2 = System.out;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Index 1 for supplementals, limit is ");
                stringBuilder.append(Integer.toHexString(destIdx2));
                printStream2.println(stringBuilder.toString());
            }
            i = 0;
            while (i < this.index2Length - index2Offset) {
                destIdx = destIdx2 + 1;
                dest.index[destIdx2] = (char) ((this.index2[index2Offset + i] + dataMove) >> 2);
                i++;
                destIdx2 = destIdx;
            }
            if (this.UTRIE2_DEBUG) {
                printStream2 = System.out;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Index 2 for supplementals, limit is ");
                stringBuilder2.append(Integer.toHexString(destIdx2));
                printStream2.println(stringBuilder2.toString());
            }
        }
        switch (valueBits) {
            case BITS_16:
                dest.data16 = destIdx2;
                i = 0;
                while (i < this.dataLength) {
                    int destIdx4 = destIdx2 + 1;
                    dest.index[destIdx2] = (char) this.data[i];
                    i++;
                    destIdx2 = destIdx4;
                }
                return;
            case BITS_32:
                for (i = 0; i < this.dataLength; i++) {
                    dest.data32[i] = this.data[i];
                }
                return;
            default:
                return;
        }
    }
}
