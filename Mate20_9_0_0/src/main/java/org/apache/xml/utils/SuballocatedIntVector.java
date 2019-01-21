package org.apache.xml.utils;

import org.apache.xml.dtm.DTMFilter;

public class SuballocatedIntVector {
    protected static final int NUMBLOCKS_DEFAULT = 32;
    protected int m_MASK;
    protected int m_SHIFT;
    protected int m_blocksize;
    protected int[] m_buildCache;
    protected int m_buildCacheStartIndex;
    protected int m_firstFree;
    protected int[][] m_map;
    protected int[] m_map0;
    protected int m_numblocks;

    public SuballocatedIntVector() {
        this(DTMFilter.SHOW_NOTATION);
    }

    public SuballocatedIntVector(int blocksize, int numblocks) {
        this.m_numblocks = 32;
        this.m_firstFree = 0;
        this.m_SHIFT = 0;
        while (true) {
            int i = blocksize >>> 1;
            blocksize = i;
            if (i != 0) {
                this.m_SHIFT++;
            } else {
                this.m_blocksize = 1 << this.m_SHIFT;
                this.m_MASK = this.m_blocksize - 1;
                this.m_numblocks = numblocks;
                this.m_map0 = new int[this.m_blocksize];
                this.m_map = new int[numblocks][];
                this.m_map[0] = this.m_map0;
                this.m_buildCache = this.m_map0;
                this.m_buildCacheStartIndex = 0;
                return;
            }
        }
    }

    public SuballocatedIntVector(int blocksize) {
        this(blocksize, 32);
    }

    public int size() {
        return this.m_firstFree;
    }

    public void setSize(int sz) {
        if (this.m_firstFree > sz) {
            this.m_firstFree = sz;
        }
    }

    public void addElement(int value) {
        int indexRelativeToCache = this.m_firstFree - this.m_buildCacheStartIndex;
        if (indexRelativeToCache < 0 || indexRelativeToCache >= this.m_blocksize) {
            int index = this.m_firstFree >>> this.m_SHIFT;
            int offset = this.m_firstFree & this.m_MASK;
            if (index >= this.m_map.length) {
                int[][] newMap = new int[(this.m_numblocks + index)][];
                System.arraycopy(this.m_map, 0, newMap, 0, this.m_map.length);
                this.m_map = newMap;
            }
            int[] block = this.m_map[index];
            if (block == null) {
                int[] iArr = new int[this.m_blocksize];
                this.m_map[index] = iArr;
                block = iArr;
            }
            block[offset] = value;
            this.m_buildCache = block;
            this.m_buildCacheStartIndex = this.m_firstFree - offset;
            this.m_firstFree++;
            return;
        }
        this.m_buildCache[indexRelativeToCache] = value;
        this.m_firstFree++;
    }

    private void addElements(int value, int numberOfElements) {
        int i = 0;
        int i2;
        if (this.m_firstFree + numberOfElements < this.m_blocksize) {
            while (true) {
                i2 = i;
                if (i2 < numberOfElements) {
                    int[] iArr = this.m_map0;
                    i = this.m_firstFree;
                    this.m_firstFree = i + 1;
                    iArr[i] = value;
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        }
        i2 = this.m_firstFree >>> this.m_SHIFT;
        int offset = this.m_firstFree & this.m_MASK;
        this.m_firstFree += numberOfElements;
        while (numberOfElements > 0) {
            if (i2 >= this.m_map.length) {
                int[][] newMap = new int[(this.m_numblocks + i2)][];
                System.arraycopy(this.m_map, 0, newMap, 0, this.m_map.length);
                this.m_map = newMap;
            }
            int[] block = this.m_map[i2];
            if (block == null) {
                int[] iArr2 = new int[this.m_blocksize];
                this.m_map[i2] = iArr2;
                block = iArr2;
            }
            int copied = this.m_blocksize - offset < numberOfElements ? this.m_blocksize - offset : numberOfElements;
            numberOfElements -= copied;
            while (true) {
                int copied2 = copied - 1;
                if (copied <= 0) {
                    break;
                }
                copied = offset + 1;
                block[offset] = value;
                offset = copied;
                copied = copied2;
            }
            i2++;
            offset = 0;
        }
    }

    private void addElements(int numberOfElements) {
        int newlen = this.m_firstFree + numberOfElements;
        if (newlen > this.m_blocksize) {
            int newindex = (this.m_firstFree + numberOfElements) >>> this.m_SHIFT;
            for (int i = (this.m_firstFree >>> this.m_SHIFT) + 1; i <= newindex; i++) {
                this.m_map[i] = new int[this.m_blocksize];
            }
        }
        this.m_firstFree = newlen;
    }

    private void insertElementAt(int value, int at) {
        int index;
        int offset;
        if (at == this.m_firstFree) {
            addElement(value);
        } else if (at > this.m_firstFree) {
            index = at >>> this.m_SHIFT;
            if (index >= this.m_map.length) {
                int[][] newMap = new int[(this.m_numblocks + index)][];
                System.arraycopy(this.m_map, 0, newMap, 0, this.m_map.length);
                this.m_map = newMap;
            }
            int[] block = this.m_map[index];
            if (block == null) {
                int[] iArr = new int[this.m_blocksize];
                this.m_map[index] = iArr;
                block = iArr;
            }
            offset = this.m_MASK & at;
            block[offset] = value;
            this.m_firstFree = offset + 1;
        } else {
            int maxindex = this.m_firstFree >>> this.m_SHIFT;
            this.m_firstFree++;
            offset = this.m_MASK & at;
            for (index = at >>> this.m_SHIFT; index <= maxindex; index++) {
                int push;
                int copylen = (this.m_blocksize - offset) - 1;
                int[] block2 = this.m_map[index];
                if (block2 == null) {
                    push = 0;
                    int[] iArr2 = new int[this.m_blocksize];
                    this.m_map[index] = iArr2;
                    block2 = iArr2;
                } else {
                    push = block2[this.m_blocksize - 1];
                    System.arraycopy(block2, offset, block2, offset + 1, copylen);
                }
                block2[offset] = value;
                value = push;
                offset = 0;
            }
        }
    }

    public void removeAllElements() {
        this.m_firstFree = 0;
        this.m_buildCache = this.m_map0;
        this.m_buildCacheStartIndex = 0;
    }

    private boolean removeElement(int s) {
        int at = indexOf(s, 0);
        if (at < 0) {
            return false;
        }
        removeElementAt(at);
        return true;
    }

    private void removeElementAt(int at) {
        if (at < this.m_firstFree) {
            int maxindex = this.m_firstFree >>> this.m_SHIFT;
            int offset = this.m_MASK & at;
            for (int index = at >>> this.m_SHIFT; index <= maxindex; index++) {
                int[] iArr;
                int copylen = (this.m_blocksize - offset) - 1;
                int[] block = this.m_map[index];
                if (block == null) {
                    iArr = new int[this.m_blocksize];
                    this.m_map[index] = iArr;
                    block = iArr;
                } else {
                    System.arraycopy(block, offset + 1, block, offset, copylen);
                }
                int i = 0;
                if (index < maxindex) {
                    iArr = this.m_map[index + 1];
                    if (iArr != null) {
                        int i2 = this.m_blocksize - 1;
                        if (iArr != null) {
                            i = iArr[0];
                        }
                        block[i2] = i;
                    }
                } else {
                    block[this.m_blocksize - 1] = 0;
                }
                offset = 0;
            }
        }
        this.m_firstFree--;
    }

    public void setElementAt(int value, int at) {
        if (at < this.m_blocksize) {
            this.m_map0[at] = value;
        } else {
            int index = at >>> this.m_SHIFT;
            int offset = this.m_MASK & at;
            if (index >= this.m_map.length) {
                int[][] newMap = new int[(this.m_numblocks + index)][];
                System.arraycopy(this.m_map, 0, newMap, 0, this.m_map.length);
                this.m_map = newMap;
            }
            int[] block = this.m_map[index];
            if (block == null) {
                int[] iArr = new int[this.m_blocksize];
                this.m_map[index] = iArr;
                block = iArr;
            }
            block[offset] = value;
        }
        if (at >= this.m_firstFree) {
            this.m_firstFree = at + 1;
        }
    }

    public int elementAt(int i) {
        if (i < this.m_blocksize) {
            return this.m_map0[i];
        }
        return this.m_map[i >>> this.m_SHIFT][this.m_MASK & i];
    }

    private boolean contains(int s) {
        return indexOf(s, 0) >= 0;
    }

    public int indexOf(int elem, int index) {
        if (index >= this.m_firstFree) {
            return -1;
        }
        int boffset = this.m_MASK & index;
        int maxindex = this.m_firstFree >>> this.m_SHIFT;
        for (int bindex = index >>> this.m_SHIFT; bindex < maxindex; bindex++) {
            int[] block = this.m_map[bindex];
            if (block != null) {
                for (int offset = boffset; offset < this.m_blocksize; offset++) {
                    if (block[offset] == elem) {
                        return (this.m_blocksize * bindex) + offset;
                    }
                }
                continue;
            }
            boffset = 0;
        }
        int maxoffset = this.m_firstFree & this.m_MASK;
        int[] block2 = this.m_map[maxindex];
        for (int offset2 = boffset; offset2 < maxoffset; offset2++) {
            if (block2[offset2] == elem) {
                return (this.m_blocksize * maxindex) + offset2;
            }
        }
        return -1;
    }

    public int indexOf(int elem) {
        return indexOf(elem, 0);
    }

    private int lastIndexOf(int elem) {
        int boffset = this.m_firstFree & this.m_MASK;
        for (int index = this.m_firstFree >>> this.m_SHIFT; index >= 0; index--) {
            int[] block = this.m_map[index];
            if (block != null) {
                for (int offset = boffset; offset >= 0; offset--) {
                    if (block[offset] == elem) {
                        return (this.m_blocksize * index) + offset;
                    }
                }
                continue;
            }
            boffset = 0;
        }
        return -1;
    }

    public final int[] getMap0() {
        return this.m_map0;
    }

    public final int[][] getMap() {
        return this.m_map;
    }
}
