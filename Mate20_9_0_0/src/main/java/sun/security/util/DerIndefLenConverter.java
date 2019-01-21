package sun.security.util;

import java.io.IOException;
import java.util.ArrayList;

class DerIndefLenConverter {
    private static final int CLASS_MASK = 192;
    private static final int FORM_MASK = 32;
    private static final int LEN_LONG = 128;
    private static final int LEN_MASK = 127;
    private static final int SKIP_EOC_BYTES = 2;
    private static final int TAG_MASK = 31;
    private byte[] data;
    private int dataPos;
    private int dataSize;
    private int index;
    private ArrayList<Object> ndefsList = new ArrayList();
    private byte[] newData;
    private int newDataPos;
    private int numOfTotalLenBytes = 0;
    private int unresolved = 0;

    private boolean isEOC(int tag) {
        return (tag & 31) == 0 && (tag & 32) == 0 && (tag & CLASS_MASK) == 0;
    }

    static boolean isLongForm(int lengthByte) {
        return (lengthByte & 128) == 128;
    }

    DerIndefLenConverter() {
    }

    static boolean isIndefinite(int lengthByte) {
        return isLongForm(lengthByte) && (lengthByte & 127) == 0;
    }

    private void parseTag() throws IOException {
        if (this.dataPos != this.dataSize) {
            if (isEOC(this.data[this.dataPos]) && this.data[this.dataPos + 1] == (byte) 0) {
                int numOfEncapsulatedLenBytes = 0;
                Object elem = null;
                int index = this.ndefsList.size() - 1;
                while (index >= 0) {
                    elem = this.ndefsList.get(index);
                    if (elem instanceof Integer) {
                        break;
                    }
                    numOfEncapsulatedLenBytes += ((byte[]) elem).length - 3;
                    index--;
                }
                if (index >= 0) {
                    byte[] sectionLenBytes = getLengthBytes((this.dataPos - ((Integer) elem).intValue()) + numOfEncapsulatedLenBytes);
                    this.ndefsList.set(index, sectionLenBytes);
                    this.unresolved--;
                    this.numOfTotalLenBytes += sectionLenBytes.length - 3;
                } else {
                    throw new IOException("EOC does not have matching indefinite-length tag");
                }
            }
            this.dataPos++;
        }
    }

    private void writeTag() {
        if (this.dataPos != this.dataSize) {
            int tag = this.data;
            int i = this.dataPos;
            this.dataPos = i + 1;
            tag = tag[i];
            if (isEOC(tag) && this.data[this.dataPos] == (byte) 0) {
                this.dataPos++;
                writeTag();
            } else {
                byte[] bArr = this.newData;
                int i2 = this.newDataPos;
                this.newDataPos = i2 + 1;
                bArr[i2] = (byte) tag;
            }
        }
    }

    private int parseLength() throws IOException {
        int curLen = 0;
        if (this.dataPos == this.dataSize) {
            return 0;
        }
        byte[] bArr = this.data;
        int i = this.dataPos;
        this.dataPos = i + 1;
        int lenByte = bArr[i] & 255;
        if (isIndefinite(lenByte)) {
            this.ndefsList.add(new Integer(this.dataPos));
            this.unresolved++;
            return 0;
        }
        if (isLongForm(lenByte)) {
            lenByte &= 127;
            if (lenByte > 4) {
                throw new IOException("Too much data");
            } else if (this.dataSize - this.dataPos >= lenByte + 1) {
                for (i = 0; i < lenByte; i++) {
                    int i2 = curLen << 8;
                    byte[] bArr2 = this.data;
                    int i3 = this.dataPos;
                    this.dataPos = i3 + 1;
                    curLen = i2 + (bArr2[i3] & 255);
                }
                if (curLen < 0) {
                    throw new IOException("Invalid length bytes");
                }
            } else {
                throw new IOException("Too little data");
            }
        }
        curLen = lenByte & 127;
        return curLen;
    }

    private void writeLengthAndValue() throws IOException {
        if (this.dataPos != this.dataSize) {
            int curLen = 0;
            byte[] bArr = this.data;
            int i = this.dataPos;
            this.dataPos = i + 1;
            int lenByte = bArr[i] & 255;
            int i2 = 0;
            if (isIndefinite(lenByte)) {
                ArrayList arrayList = this.ndefsList;
                int i3 = this.index;
                this.index = i3 + 1;
                byte[] lenBytes = (byte[]) arrayList.get(i3);
                System.arraycopy(lenBytes, 0, this.newData, this.newDataPos, lenBytes.length);
                this.newDataPos += lenBytes.length;
                return;
            }
            if (isLongForm(lenByte)) {
                lenByte &= 127;
                while (true) {
                    i = i2;
                    if (i >= lenByte) {
                        break;
                    }
                    i2 = curLen << 8;
                    byte[] bArr2 = this.data;
                    int i4 = this.dataPos;
                    this.dataPos = i4 + 1;
                    curLen = i2 + (bArr2[i4] & 255);
                    i2 = i + 1;
                }
                if (curLen < 0) {
                    throw new IOException("Invalid length bytes");
                }
            }
            curLen = lenByte & 127;
            writeLength(curLen);
            writeValue(curLen);
        }
    }

    private void writeLength(int curLen) {
        byte[] bArr;
        int i;
        if (curLen < 128) {
            bArr = this.newData;
            i = this.newDataPos;
            this.newDataPos = i + 1;
            bArr[i] = (byte) curLen;
        } else if (curLen < 256) {
            bArr = this.newData;
            i = this.newDataPos;
            this.newDataPos = i + 1;
            bArr[i] = (byte) -127;
            bArr = this.newData;
            i = this.newDataPos;
            this.newDataPos = i + 1;
            bArr[i] = (byte) curLen;
        } else if (curLen < 65536) {
            bArr = this.newData;
            i = this.newDataPos;
            this.newDataPos = i + 1;
            bArr[i] = (byte) -126;
            bArr = this.newData;
            i = this.newDataPos;
            this.newDataPos = i + 1;
            bArr[i] = (byte) (curLen >> 8);
            bArr = this.newData;
            i = this.newDataPos;
            this.newDataPos = i + 1;
            bArr[i] = (byte) curLen;
        } else if (curLen < 16777216) {
            bArr = this.newData;
            i = this.newDataPos;
            this.newDataPos = i + 1;
            bArr[i] = (byte) -125;
            bArr = this.newData;
            i = this.newDataPos;
            this.newDataPos = i + 1;
            bArr[i] = (byte) (curLen >> 16);
            bArr = this.newData;
            i = this.newDataPos;
            this.newDataPos = i + 1;
            bArr[i] = (byte) (curLen >> 8);
            bArr = this.newData;
            i = this.newDataPos;
            this.newDataPos = i + 1;
            bArr[i] = (byte) curLen;
        } else {
            bArr = this.newData;
            i = this.newDataPos;
            this.newDataPos = i + 1;
            bArr[i] = (byte) -124;
            bArr = this.newData;
            i = this.newDataPos;
            this.newDataPos = i + 1;
            bArr[i] = (byte) (curLen >> 24);
            bArr = this.newData;
            i = this.newDataPos;
            this.newDataPos = i + 1;
            bArr[i] = (byte) (curLen >> 16);
            bArr = this.newData;
            i = this.newDataPos;
            this.newDataPos = i + 1;
            bArr[i] = (byte) (curLen >> 8);
            bArr = this.newData;
            i = this.newDataPos;
            this.newDataPos = i + 1;
            bArr[i] = (byte) curLen;
        }
    }

    private byte[] getLengthBytes(int curLen) {
        byte[] lenBytes;
        int index;
        if (curLen < 128) {
            lenBytes = new byte[1];
            index = 0 + 1;
            lenBytes[0] = (byte) curLen;
            return lenBytes;
        }
        int index2;
        if (curLen < 256) {
            lenBytes = new byte[2];
            index = 0 + 1;
            lenBytes[0] = (byte) -127;
            index2 = index + 1;
            lenBytes[index] = (byte) curLen;
        } else if (curLen < 65536) {
            lenBytes = new byte[3];
            index = 0 + 1;
            lenBytes[0] = (byte) -126;
            index2 = index + 1;
            lenBytes[index] = (byte) (curLen >> 8);
            index = index2 + 1;
            lenBytes[index2] = (byte) curLen;
            return lenBytes;
        } else if (curLen < 16777216) {
            lenBytes = new byte[4];
            index = 0 + 1;
            lenBytes[0] = (byte) -125;
            index2 = index + 1;
            lenBytes[index] = (byte) (curLen >> 16);
            index = index2 + 1;
            lenBytes[index2] = (byte) (curLen >> 8);
            index2 = index + 1;
            lenBytes[index] = (byte) curLen;
        } else {
            lenBytes = new byte[5];
            index = 0 + 1;
            lenBytes[0] = (byte) -124;
            index2 = index + 1;
            lenBytes[index] = (byte) (curLen >> 24);
            index = index2 + 1;
            lenBytes[index2] = (byte) (curLen >> 16);
            index2 = index + 1;
            lenBytes[index] = (byte) (curLen >> 8);
            index = index2 + 1;
            lenBytes[index2] = (byte) curLen;
            return lenBytes;
        }
        return lenBytes;
    }

    private int getNumOfLenBytes(int len) {
        if (len < 128) {
            return 1;
        }
        if (len < 256) {
            return 2;
        }
        if (len < 65536) {
            return 3;
        }
        if (len < 16777216) {
            return 4;
        }
        return 5;
    }

    private void parseValue(int curLen) {
        this.dataPos += curLen;
    }

    private void writeValue(int curLen) {
        for (int i = 0; i < curLen; i++) {
            byte[] bArr = this.newData;
            int i2 = this.newDataPos;
            this.newDataPos = i2 + 1;
            byte[] bArr2 = this.data;
            int i3 = this.dataPos;
            this.dataPos = i3 + 1;
            bArr[i2] = bArr2[i3];
        }
    }

    byte[] convert(byte[] indefData) throws IOException {
        this.data = indefData;
        this.dataPos = 0;
        this.index = 0;
        this.dataSize = this.data.length;
        int len = 0;
        int unused = 0;
        while (this.dataPos < this.dataSize) {
            parseTag();
            parseValue(parseLength());
            if (this.unresolved == 0) {
                unused = this.dataSize - this.dataPos;
                this.dataSize = this.dataPos;
                break;
            }
        }
        if (this.unresolved == 0) {
            this.newData = new byte[((this.dataSize + this.numOfTotalLenBytes) + unused)];
            this.dataPos = 0;
            this.newDataPos = 0;
            this.index = 0;
            while (this.dataPos < this.dataSize) {
                writeTag();
                writeLengthAndValue();
            }
            System.arraycopy(indefData, this.dataSize, this.newData, this.dataSize + this.numOfTotalLenBytes, unused);
            return this.newData;
        }
        throw new IOException("not all indef len BER resolved");
    }
}
