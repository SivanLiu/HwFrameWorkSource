package android.icu.impl;

import android.icu.impl.TrieBuilder.DataManipulate;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.UTF16;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class IntTrieBuilder extends TrieBuilder {
    protected int[] m_data_;
    protected int m_initialValue_;
    private int m_leadUnitValue_;

    public IntTrieBuilder(IntTrieBuilder table) {
        super(table);
        this.m_data_ = new int[this.m_dataCapacity_];
        System.arraycopy(table.m_data_, 0, this.m_data_, 0, this.m_dataLength_);
        this.m_initialValue_ = table.m_initialValue_;
        this.m_leadUnitValue_ = table.m_leadUnitValue_;
    }

    public IntTrieBuilder(int[] aliasdata, int maxdatalength, int initialvalue, int leadunitvalue, boolean latin1linear) {
        if (maxdatalength < 32 || (latin1linear && maxdatalength < 1024)) {
            throw new IllegalArgumentException("Argument maxdatalength is too small");
        }
        if (aliasdata != null) {
            this.m_data_ = aliasdata;
        } else {
            this.m_data_ = new int[maxdatalength];
        }
        int j = 32;
        if (latin1linear) {
            int j2 = 32;
            j = 0;
            while (true) {
                int i = j + 1;
                this.m_index_[j] = j2;
                j2 += 32;
                if (i >= 8) {
                    break;
                }
                j = i;
            }
            j = j2;
        }
        this.m_dataLength_ = j;
        Arrays.fill(this.m_data_, 0, this.m_dataLength_, initialvalue);
        this.m_initialValue_ = initialvalue;
        this.m_leadUnitValue_ = leadunitvalue;
        this.m_dataCapacity_ = maxdatalength;
        this.m_isLatin1Linear_ = latin1linear;
        this.m_isCompacted_ = false;
    }

    public int getValue(int ch) {
        if (this.m_isCompacted_ || ch > 1114111 || ch < 0) {
            return 0;
        }
        return this.m_data_[Math.abs(this.m_index_[ch >> 5]) + (ch & 31)];
    }

    public int getValue(int ch, boolean[] inBlockZero) {
        boolean z = true;
        if (this.m_isCompacted_ || ch > 1114111 || ch < 0) {
            if (inBlockZero != null) {
                inBlockZero[0] = true;
            }
            return 0;
        }
        int block = this.m_index_[ch >> 5];
        if (inBlockZero != null) {
            if (block != 0) {
                z = false;
            }
            inBlockZero[0] = z;
        }
        return this.m_data_[Math.abs(block) + (ch & 31)];
    }

    public boolean setValue(int ch, int value) {
        if (this.m_isCompacted_ || ch > 1114111 || ch < 0) {
            return false;
        }
        int block = getDataBlock(ch);
        if (block < 0) {
            return false;
        }
        this.m_data_[(ch & 31) + block] = value;
        return true;
    }

    public IntTrie serialize(DataManipulate datamanipulate, Trie.DataManipulate triedatamanipulate) {
        if (datamanipulate != null) {
            if (!this.m_isCompacted_) {
                compact(false);
                fold(datamanipulate);
                compact(true);
                this.m_isCompacted_ = true;
            }
            if (this.m_dataLength_ < 262144) {
                char[] index = new char[this.m_indexLength_];
                int[] data = new int[this.m_dataLength_];
                for (int i = 0; i < this.m_indexLength_; i++) {
                    index[i] = (char) (this.m_index_[i] >>> 2);
                }
                System.arraycopy(this.m_data_, 0, data, 0, this.m_dataLength_);
                int options = 37 | 256;
                if (this.m_isLatin1Linear_) {
                    options |= 512;
                }
                return new IntTrie(index, data, this.m_initialValue_, options, triedatamanipulate);
            }
            throw new ArrayIndexOutOfBoundsException("Data length too small");
        }
        throw new IllegalArgumentException("Parameters can not be null");
    }

    public int serialize(OutputStream os, boolean reduceTo16Bits, DataManipulate datamanipulate) throws IOException {
        if (datamanipulate != null) {
            int length;
            int i = 0;
            if (!this.m_isCompacted_) {
                compact(false);
                fold(datamanipulate);
                compact(true);
                this.m_isCompacted_ = true;
            }
            if (reduceTo16Bits) {
                length = this.m_dataLength_ + this.m_indexLength_;
            } else {
                length = this.m_dataLength_;
            }
            if (length < 262144) {
                int length2 = 16 + (this.m_indexLength_ * 2);
                if (reduceTo16Bits) {
                    length2 += this.m_dataLength_ * 2;
                } else {
                    length2 += 4 * this.m_dataLength_;
                }
                if (os == null) {
                    return length2;
                }
                length = new DataOutputStream(os);
                length.writeInt(1416784229);
                int options = 37;
                if (!reduceTo16Bits) {
                    options = 37 | 256;
                }
                if (this.m_isLatin1Linear_) {
                    options |= 512;
                }
                length.writeInt(options);
                length.writeInt(this.m_indexLength_);
                length.writeInt(this.m_dataLength_);
                int i2;
                if (reduceTo16Bits) {
                    for (i2 = 0; i2 < this.m_indexLength_; i2++) {
                        length.writeChar((this.m_index_[i2] + this.m_indexLength_) >>> 2);
                    }
                    while (i < this.m_dataLength_) {
                        length.writeChar(this.m_data_[i] & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH);
                        i++;
                    }
                } else {
                    for (i2 = 0; i2 < this.m_indexLength_; i2++) {
                        length.writeChar(this.m_index_[i2] >>> 2);
                    }
                    while (i < this.m_dataLength_) {
                        length.writeInt(this.m_data_[i]);
                        i++;
                    }
                }
                return length2;
            }
            throw new ArrayIndexOutOfBoundsException("Data length too small");
        }
        throw new IllegalArgumentException("Parameters can not be null");
    }

    public boolean setRange(int start, int limit, int value, boolean overwrite) {
        int i = start;
        int limit2 = limit;
        int i2 = value;
        if (this.m_isCompacted_ || i < 0 || i > 1114111 || limit2 < 0 || limit2 > 1114112 || i > limit2) {
            return false;
        }
        if (i == limit2) {
            return true;
        }
        int block;
        int repeatBlock;
        int start2;
        int block2;
        if ((i & 31) != 0) {
            block = getDataBlock(start);
            if (block < 0) {
                return false;
            }
            repeatBlock = (i + 32) & -32;
            if (repeatBlock <= limit2) {
                fillBlock(block, i & 31, 32, i2, overwrite);
                start2 = repeatBlock;
            } else {
                fillBlock(block, i & 31, limit2 & 31, i2, overwrite);
                return true;
            }
        }
        start2 = i;
        i = limit2 & 31;
        limit2 &= -32;
        int repeatBlock2 = 0;
        if (i2 != this.m_initialValue_) {
            repeatBlock2 = -1;
        }
        block = start2;
        repeatBlock = repeatBlock2;
        while (block < limit2) {
            block2 = this.m_index_[block >> 5];
            if (block2 > 0) {
                fillBlock(block2, 0, 32, i2, overwrite);
            } else if (this.m_data_[-block2] != i2 && (block2 == 0 || overwrite)) {
                if (repeatBlock >= 0) {
                    this.m_index_[block >> 5] = -repeatBlock;
                } else {
                    repeatBlock = getDataBlock(block);
                    if (repeatBlock < 0) {
                        return false;
                    }
                    this.m_index_[block >> 5] = -repeatBlock;
                    fillBlock(repeatBlock, 0, 32, i2, true);
                }
            }
            block += 32;
        }
        if (i > 0) {
            block2 = getDataBlock(block);
            if (block2 < 0) {
                return false;
            }
            fillBlock(block2, 0, i, i2, overwrite);
        }
        return true;
    }

    private int allocDataBlock() {
        int newBlock = this.m_dataLength_;
        int newTop = newBlock + 32;
        if (newTop > this.m_dataCapacity_) {
            return -1;
        }
        this.m_dataLength_ = newTop;
        return newBlock;
    }

    private int getDataBlock(int ch) {
        ch >>= 5;
        int indexValue = this.m_index_[ch];
        if (indexValue > 0) {
            return indexValue;
        }
        int newBlock = allocDataBlock();
        if (newBlock < 0) {
            return -1;
        }
        this.m_index_[ch] = newBlock;
        System.arraycopy(this.m_data_, Math.abs(indexValue), this.m_data_, newBlock, 128);
        return newBlock;
    }

    private void compact(boolean overlap) {
        if (!this.m_isCompacted_) {
            int i;
            findUnusedBlocks();
            int overlapStart = 32;
            if (this.m_isLatin1Linear_) {
                overlapStart = 32 + 256;
            }
            int start = 32;
            int newStart = 32;
            while (true) {
                i = 0;
                if (start >= this.m_dataLength_) {
                    break;
                } else if (this.m_map_[start >>> 5] < 0) {
                    start += 32;
                } else {
                    int i2;
                    if (start >= overlapStart) {
                        int i3;
                        i2 = this.m_data_;
                        if (overlap) {
                            i3 = 4;
                        } else {
                            i3 = 32;
                        }
                        i2 = findSameDataBlock(i2, newStart, start, i3);
                        if (i2 >= 0) {
                            this.m_map_[start >>> 5] = i2;
                            start += 32;
                        }
                    }
                    if (overlap && start >= overlapStart) {
                        i = 28;
                        while (i > 0 && !TrieBuilder.equal_int(this.m_data_, newStart - i, start, i)) {
                            i -= 4;
                        }
                    }
                    int i4;
                    int start2;
                    if (i > 0) {
                        this.m_map_[start >>> 5] = newStart - i;
                        start += i;
                        i4 = 32 - i;
                        while (i4 > 0) {
                            i2 = newStart + 1;
                            start2 = start + 1;
                            this.m_data_[newStart] = this.m_data_[start];
                            i4--;
                            newStart = i2;
                            start = start2;
                        }
                    } else if (newStart < start) {
                        this.m_map_[start >>> 5] = newStart;
                        i4 = 32;
                        while (i4 > 0) {
                            i2 = newStart + 1;
                            start2 = start + 1;
                            this.m_data_[newStart] = this.m_data_[start];
                            i4--;
                            newStart = i2;
                            start = start2;
                        }
                    } else {
                        this.m_map_[start >>> 5] = start;
                        newStart += 32;
                        start = newStart;
                    }
                }
            }
            while (true) {
                start = i;
                if (start < this.m_indexLength_) {
                    this.m_index_[start] = this.m_map_[Math.abs(this.m_index_[start]) >>> 5];
                    i = start + 1;
                } else {
                    this.m_dataLength_ = newStart;
                    return;
                }
            }
        }
    }

    private static final int findSameDataBlock(int[] data, int dataLength, int otherBlock, int step) {
        dataLength -= 32;
        int block = 0;
        while (block <= dataLength) {
            if (TrieBuilder.equal_int(data, block, otherBlock, 32)) {
                return block;
            }
            block += step;
        }
        return -1;
    }

    private final void fold(DataManipulate manipulate) {
        int[] leadIndexes = new int[32];
        int[] index = this.m_index_;
        int c = 1728;
        System.arraycopy(index, 1728, leadIndexes, 0, 32);
        int block = 0;
        if (this.m_leadUnitValue_ != this.m_initialValue_) {
            block = allocDataBlock();
            if (block >= 0) {
                fillBlock(block, 0, 32, this.m_leadUnitValue_, true);
                block = -block;
            } else {
                throw new IllegalStateException("Internal error: Out of memory space");
            }
        }
        while (c < 1760) {
            this.m_index_[c] = block;
            c++;
        }
        c = 2048;
        int c2 = 65536;
        while (c2 < 1114112) {
            if (index[c2 >> 5] != 0) {
                c2 &= -1024;
                block = TrieBuilder.findSameIndexBlock(index, c, c2 >> 5);
                int value = manipulate.getFoldedValue(c2, block + 32);
                if (value != getValue(UTF16.getLeadSurrogate(c2))) {
                    if (!setValue(UTF16.getLeadSurrogate(c2), value)) {
                        throw new ArrayIndexOutOfBoundsException("Data table overflow");
                    } else if (block == c) {
                        System.arraycopy(index, c2 >> 5, index, c, 32);
                        c += 32;
                    }
                }
                c2 += 1024;
            } else {
                c2 += 32;
            }
        }
        if (c < 34816) {
            System.arraycopy(index, 2048, index, 2080, c - 2048);
            System.arraycopy(leadIndexes, 0, index, 2048, 32);
            this.m_indexLength_ = c + 32;
            return;
        }
        throw new ArrayIndexOutOfBoundsException("Index table overflow");
    }

    private void fillBlock(int block, int start, int limit, int value, boolean overwrite) {
        limit += block;
        block += start;
        if (overwrite) {
            while (block < limit) {
                int block2 = block + 1;
                this.m_data_[block] = value;
                block = block2;
            }
            return;
        }
        while (block < limit) {
            if (this.m_data_[block] == this.m_initialValue_) {
                this.m_data_[block] = value;
            }
            block++;
        }
    }
}
