package sun.security.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Types;
import java.util.Date;
import sun.util.calendar.CalendarDate;
import sun.util.calendar.CalendarSystem;

class DerInputBuffer extends ByteArrayInputStream implements Cloneable {
    DerInputBuffer(byte[] buf) {
        super(buf);
    }

    DerInputBuffer(byte[] buf, int offset, int len) {
        super(buf, offset, len);
    }

    DerInputBuffer dup() {
        try {
            DerInputBuffer retval = (DerInputBuffer) clone();
            retval.mark(Integer.MAX_VALUE);
            return retval;
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    byte[] toByteArray() {
        int len = available();
        if (len <= 0) {
            return null;
        }
        byte[] retval = new byte[len];
        System.arraycopy(this.buf, this.pos, retval, 0, len);
        return retval;
    }

    int getPos() {
        return this.pos;
    }

    byte[] getSlice(int startPos, int size) {
        byte[] result = new byte[size];
        System.arraycopy(this.buf, startPos, result, 0, size);
        return result;
    }

    int peek() throws IOException {
        if (this.pos < this.count) {
            return this.buf[this.pos];
        }
        throw new IOException("out of data");
    }

    public boolean equals(Object other) {
        if (other instanceof DerInputBuffer) {
            return equals((DerInputBuffer) other);
        }
        return false;
    }

    boolean equals(DerInputBuffer other) {
        if (this == other) {
            return true;
        }
        int max = available();
        if (other.available() != max) {
            return false;
        }
        for (int i = 0; i < max; i++) {
            if (this.buf[this.pos + i] != other.buf[other.pos + i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int retval = 0;
        int len = available();
        int p = this.pos;
        for (int i = 0; i < len; i++) {
            retval += this.buf[p + i] * i;
        }
        return retval;
    }

    void truncate(int len) throws IOException {
        if (len <= available()) {
            this.count = this.pos + len;
            return;
        }
        throw new IOException("insufficient data");
    }

    BigInteger getBigInteger(int len, boolean makePositive) throws IOException {
        if (len > available()) {
            throw new IOException("short read of integer");
        } else if (len != 0) {
            byte[] bytes = new byte[len];
            System.arraycopy(this.buf, this.pos, bytes, 0, len);
            skip((long) len);
            if (len >= 2 && bytes[0] == (byte) 0 && bytes[1] >= (byte) 0) {
                throw new IOException("Invalid encoding: redundant leading 0s");
            } else if (makePositive) {
                return new BigInteger(1, bytes);
            } else {
                return new BigInteger(bytes);
            }
        } else {
            throw new IOException("Invalid encoding: zero length Int value");
        }
    }

    public int getInteger(int len) throws IOException {
        BigInteger result = getBigInteger(len, null);
        if (result.compareTo(BigInteger.valueOf(-2147483648L)) < 0) {
            throw new IOException("Integer below minimum valid value");
        } else if (result.compareTo(BigInteger.valueOf(2147483647L)) <= 0) {
            return result.intValue();
        } else {
            throw new IOException("Integer exceeds maximum valid value");
        }
    }

    public byte[] getBitString(int len) throws IOException {
        if (len > available()) {
            throw new IOException("short read of bit string");
        } else if (len != 0) {
            int numOfPadBits = this.buf[this.pos];
            if (numOfPadBits < 0 || numOfPadBits > 7) {
                throw new IOException("Invalid number of padding bits");
            }
            byte[] retval = new byte[(len - 1)];
            System.arraycopy(this.buf, this.pos + 1, retval, 0, len - 1);
            if (numOfPadBits != 0) {
                int i = len - 2;
                retval[i] = (byte) (retval[i] & (255 << numOfPadBits));
            }
            skip((long) len);
            return retval;
        } else {
            throw new IOException("Invalid encoding: zero length bit string");
        }
    }

    byte[] getBitString() throws IOException {
        return getBitString(available());
    }

    BitArray getUnalignedBitString() throws IOException {
        if (this.pos >= this.count) {
            return null;
        }
        int len = available();
        int unusedBits = this.buf[this.pos] & 255;
        if (unusedBits <= 7) {
            byte[] bits = new byte[(len - 1)];
            int length = bits.length == 0 ? 0 : (bits.length * 8) - unusedBits;
            System.arraycopy(this.buf, this.pos + 1, bits, 0, len - 1);
            BitArray bitArray = new BitArray(length, bits);
            this.pos = this.count;
            return bitArray;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid value for unused bits: ");
        stringBuilder.append(unusedBits);
        throw new IOException(stringBuilder.toString());
    }

    public Date getUTCTime(int len) throws IOException {
        if (len > available()) {
            throw new IOException("short read of DER UTC Time");
        } else if (len >= 11 && len <= 17) {
            return getTime(len, false);
        } else {
            throw new IOException("DER UTC Time length error");
        }
    }

    public Date getGeneralizedTime(int len) throws IOException {
        if (len > available()) {
            throw new IOException("short read of DER Generalized Time");
        } else if (len >= 13 && len <= 23) {
            return getTime(len, true);
        } else {
            throw new IOException("DER Generalized Time length error");
        }
    }

    private Date getTime(int len, boolean generalized) throws IOException {
        String type;
        int i;
        int year;
        int i2;
        byte[] bArr;
        int i3;
        int second;
        int i4;
        int precision;
        byte[] bArr2;
        if (generalized) {
            type = "Generalized";
            bArr2 = this.buf;
            i = this.pos;
            this.pos = i + 1;
            year = Character.digit((char) bArr2[i], 10) * 1000;
            byte[] bArr3 = this.buf;
            i2 = this.pos;
            this.pos = i2 + 1;
            year += Character.digit((char) bArr3[i2], 10) * 100;
            bArr3 = this.buf;
            i2 = this.pos;
            this.pos = i2 + 1;
            year += Character.digit((char) bArr3[i2], 10) * 10;
            bArr3 = this.buf;
            i2 = this.pos;
            this.pos = i2 + 1;
            year += Character.digit((char) bArr3[i2], 10);
            i = len - 2;
        } else {
            type = "UTC";
            bArr2 = this.buf;
            i2 = this.pos;
            this.pos = i2 + 1;
            year = Character.digit((char) bArr2[i2], 10) * 10;
            bArr = this.buf;
            i3 = this.pos;
            this.pos = i3 + 1;
            year += Character.digit((char) bArr[i3], 10);
            if (year < 50) {
                year += Types.JAVA_OBJECT;
            } else {
                year += 1900;
            }
            i = len;
        }
        bArr = this.buf;
        i3 = this.pos;
        this.pos = i3 + 1;
        i2 = Character.digit((char) bArr[i3], 10) * 10;
        byte[] bArr4 = this.buf;
        int i5 = this.pos;
        this.pos = i5 + 1;
        i2 += Character.digit((char) bArr4[i5], 10);
        bArr4 = this.buf;
        i5 = this.pos;
        this.pos = i5 + 1;
        i3 = Character.digit((char) bArr4[i5], 10) * 10;
        byte[] bArr5 = this.buf;
        int i6 = this.pos;
        this.pos = i6 + 1;
        i3 += Character.digit((char) bArr5[i6], 10);
        bArr5 = this.buf;
        i6 = this.pos;
        this.pos = i6 + 1;
        i5 = Character.digit((char) bArr5[i6], 10) * 10;
        byte[] bArr6 = this.buf;
        int i7 = this.pos;
        this.pos = i7 + 1;
        i5 += Character.digit((char) bArr6[i7], 10);
        bArr6 = this.buf;
        i7 = this.pos;
        this.pos = i7 + 1;
        i6 = Character.digit((char) bArr6[i7], 10) * 10;
        byte[] bArr7 = this.buf;
        int i8 = this.pos;
        this.pos = i8 + 1;
        i6 += Character.digit((char) bArr7[i8], 10);
        i -= 10;
        i7 = 0;
        if (i <= 2 || i >= 12) {
            second = 0;
        } else {
            byte[] bArr8 = this.buf;
            second = this.pos;
            this.pos = second + 1;
            second = Character.digit((char) bArr8[second], 10) * 10;
            bArr8 = this.buf;
            i4 = this.pos;
            this.pos = i4 + 1;
            second += Character.digit((char) bArr8[i4], 10);
            i -= 2;
            if (this.buf[this.pos] == (byte) 46 || this.buf[this.pos] == (byte) 44) {
                i--;
                this.pos++;
                precision = 0;
                i8 = this.pos;
                while (this.buf[i8] != (byte) 90 && this.buf[i8] != (byte) 43 && this.buf[i8] != (byte) 45) {
                    i8++;
                    precision++;
                }
                byte[] bArr9;
                int i9;
                switch (precision) {
                    case 1:
                        bArr9 = this.buf;
                        i9 = this.pos;
                        this.pos = i9 + 1;
                        i7 = 0 + (Character.digit((char) bArr9[i9], 10) * 100);
                        break;
                    case 2:
                        bArr9 = this.buf;
                        i9 = this.pos;
                        this.pos = i9 + 1;
                        i7 = 0 + (Character.digit((char) bArr9[i9], 10) * 100);
                        bArr9 = this.buf;
                        i4 = this.pos;
                        this.pos = i4 + 1;
                        i7 += Character.digit((char) bArr9[i4], 10) * 10;
                        break;
                    case 3:
                        byte[] bArr10 = this.buf;
                        i4 = this.pos;
                        this.pos = i4 + 1;
                        i7 = 0 + (Character.digit((char) bArr10[i4], 10) * 100);
                        bArr9 = this.buf;
                        i4 = this.pos;
                        this.pos = i4 + 1;
                        i7 += Character.digit((char) bArr9[i4], 10) * 10;
                        bArr9 = this.buf;
                        i4 = this.pos;
                        this.pos = i4 + 1;
                        i7 += Character.digit((char) bArr9[i4], 10);
                        break;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Parse ");
                        stringBuilder.append(type);
                        stringBuilder.append(" time, unsupported precision for seconds value");
                        throw new IOException(stringBuilder.toString());
                }
                i -= precision;
            }
        }
        precision = second;
        StringBuilder stringBuilder2;
        if (i2 == 0 || i3 == 0 || i2 > 12 || i3 > 31 || i5 >= 24 || i6 >= 60 || precision >= 60) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Parse ");
            stringBuilder2.append(type);
            stringBuilder2.append(" time, invalid format");
            throw new IOException(stringBuilder2.toString());
        }
        CalendarSystem gcal = CalendarSystem.getGregorianCalendar();
        CalendarDate date = gcal.newCalendarDate(null);
        date.setDate(year, i2, i3);
        date.setTimeOfDay(i5, i6, precision, i7);
        long time = gcal.getTime(date);
        StringBuilder stringBuilder3;
        if (i == 1 || i == 5) {
            byte[] bArr11 = this.buf;
            int i10 = this.pos;
            this.pos = i10 + 1;
            byte b = bArr11[i10];
            if (b == (byte) 43) {
                precision = this.buf;
                second = this.pos;
                this.pos = second + 1;
                i10 = 10 * Character.digit((char) precision[second], 10);
                precision = this.buf;
                int i11 = this.pos;
                this.pos = i11 + 1;
                i10 += Character.digit((char) precision[i11], 10);
                precision = this.buf;
                i11 = this.pos;
                this.pos = i11 + 1;
                precision = Character.digit((char) precision[i11], 10) * 10;
                byte[] bArr12 = this.buf;
                i4 = this.pos;
                this.pos = i4 + 1;
                precision += Character.digit((char) bArr12[i4], 10);
                int i12;
                if (i10 >= 24 || precision >= 60) {
                    i12 = precision;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Parse ");
                    stringBuilder2.append(type);
                    stringBuilder2.append(" time, +hhmm");
                    throw new IOException(stringBuilder2.toString());
                }
                i12 = precision;
                time -= (long) ((((i10 * 60) + precision) * 60) * 1000);
            } else if (b == (byte) 45) {
                byte[] bArr13 = this.buf;
                i10 = this.pos;
                this.pos = i10 + 1;
                second = Character.digit((char) bArr13[i10], 10) * 10;
                bArr11 = this.buf;
                i10 = this.pos;
                int i13 = precision;
                this.pos = i10 + 1;
                second += Character.digit((char) bArr11[i10], 10);
                precision = this.buf;
                i4 = this.pos;
                this.pos = i4 + 1;
                precision = Character.digit((char) precision[i4], 10) * 10;
                bArr11 = this.buf;
                i10 = this.pos;
                this.pos = i10 + 1;
                precision += Character.digit((char) bArr11[i10], 10);
                if (second >= 24 || precision >= 60) {
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Parse ");
                    stringBuilder4.append(type);
                    stringBuilder4.append(" time, -hhmm");
                    throw new IOException(stringBuilder4.toString());
                }
                time += (long) ((((second * 60) + precision) * 60) * 1000);
            } else if (b == (byte) 90) {
            } else {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Parse ");
                stringBuilder3.append(type);
                stringBuilder3.append(" time, garbage offset");
                throw new IOException(stringBuilder3.toString());
            }
            return new Date(time);
        }
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Parse ");
        stringBuilder3.append(type);
        stringBuilder3.append(" time, invalid offset");
        throw new IOException(stringBuilder3.toString());
    }
}
