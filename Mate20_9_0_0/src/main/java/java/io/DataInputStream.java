package java.io;

import java.nio.ByteOrder;
import libcore.io.Memory;

public class DataInputStream extends FilterInputStream implements DataInput {
    private byte[] bytearr = new byte[80];
    private char[] chararr = new char[80];
    private char[] lineBuffer;
    private byte[] readBuffer = new byte[8];

    public DataInputStream(InputStream in) {
        super(in);
    }

    public final int read(byte[] b) throws IOException {
        return this.in.read(b, 0, b.length);
    }

    public final int read(byte[] b, int off, int len) throws IOException {
        return this.in.read(b, off, len);
    }

    public final void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    public final void readFully(byte[] b, int off, int len) throws IOException {
        if (len >= 0) {
            int n = 0;
            while (n < len) {
                int count = this.in.read(b, off + n, len - n);
                if (count >= 0) {
                    n += count;
                } else {
                    throw new EOFException();
                }
            }
            return;
        }
        throw new IndexOutOfBoundsException();
    }

    public final int skipBytes(int n) throws IOException {
        int total = 0;
        while (total < n) {
            int skip = (int) this.in.skip((long) (n - total));
            int cur = skip;
            if (skip <= 0) {
                break;
            }
            total += cur;
        }
        return total;
    }

    public final boolean readBoolean() throws IOException {
        int ch = this.in.read();
        if (ch >= 0) {
            return ch != 0;
        } else {
            throw new EOFException();
        }
    }

    public final byte readByte() throws IOException {
        int ch = this.in.read();
        if (ch >= 0) {
            return (byte) ch;
        }
        throw new EOFException();
    }

    public final int readUnsignedByte() throws IOException {
        int ch = this.in.read();
        if (ch >= 0) {
            return ch;
        }
        throw new EOFException();
    }

    public final short readShort() throws IOException {
        readFully(this.readBuffer, 0, 2);
        return Memory.peekShort(this.readBuffer, 0, ByteOrder.BIG_ENDIAN);
    }

    public final int readUnsignedShort() throws IOException {
        readFully(this.readBuffer, 0, 2);
        return Memory.peekShort(this.readBuffer, 0, ByteOrder.BIG_ENDIAN) & 65535;
    }

    public final char readChar() throws IOException {
        readFully(this.readBuffer, 0, 2);
        return (char) Memory.peekShort(this.readBuffer, 0, ByteOrder.BIG_ENDIAN);
    }

    public final int readInt() throws IOException {
        readFully(this.readBuffer, 0, 4);
        return Memory.peekInt(this.readBuffer, 0, ByteOrder.BIG_ENDIAN);
    }

    public final long readLong() throws IOException {
        readFully(this.readBuffer, 0, 8);
        return (((((((((long) this.readBuffer[0]) << 56) + (((long) (this.readBuffer[1] & 255)) << 48)) + (((long) (this.readBuffer[2] & 255)) << 40)) + (((long) (this.readBuffer[3] & 255)) << 32)) + (((long) (this.readBuffer[4] & 255)) << 24)) + ((long) ((this.readBuffer[5] & 255) << 16))) + ((long) ((this.readBuffer[6] & 255) << 8))) + ((long) ((this.readBuffer[7] & 255) << 0));
    }

    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Deprecated
    public final String readLine() throws IOException {
        int c;
        char[] buf = this.lineBuffer;
        if (buf == null) {
            char[] cArr = new char[128];
            this.lineBuffer = cArr;
            buf = cArr;
        }
        int room = buf.length;
        char[] buf2 = buf;
        int offset = 0;
        while (true) {
            int read = this.in.read();
            c = read;
            if (read == -1 || read == 10) {
                break;
            } else if (read != 13) {
                room--;
                if (room < 0) {
                    buf2 = new char[(offset + 128)];
                    read = (buf2.length - offset) - 1;
                    System.arraycopy(this.lineBuffer, 0, (Object) buf2, 0, offset);
                    this.lineBuffer = buf2;
                    room = read;
                }
                read = offset + 1;
                buf2[offset] = (char) c;
                offset = read;
            } else {
                read = this.in.read();
                if (read != 10 && read != -1) {
                    if (!(this.in instanceof PushbackInputStream)) {
                        this.in = new PushbackInputStream(this.in);
                    }
                    ((PushbackInputStream) this.in).unread(read);
                }
            }
        }
        if (c == -1 && offset == 0) {
            return null;
        }
        return String.copyValueOf(buf2, 0, offset);
    }

    public final String readUTF() throws IOException {
        return readUTF(this);
    }

    public static final String readUTF(DataInput in) throws IOException {
        char[] chararr;
        byte[] bytearr;
        int c;
        int chararr_count;
        int utflen = in.readUnsignedShort();
        if (in instanceof DataInputStream) {
            DataInputStream dis = (DataInputStream) in;
            if (dis.bytearr.length < utflen) {
                dis.bytearr = new byte[(utflen * 2)];
                dis.chararr = new char[(utflen * 2)];
            }
            chararr = dis.chararr;
            bytearr = dis.bytearr;
        } else {
            bytearr = new byte[utflen];
            chararr = new char[utflen];
        }
        int count = 0;
        int chararr_count2 = 0;
        in.readFully(bytearr, 0, utflen);
        while (count < utflen) {
            c = bytearr[count] & 255;
            if (c > 127) {
                break;
            }
            count++;
            chararr_count = chararr_count2 + 1;
            chararr[chararr_count2] = (char) c;
            chararr_count2 = chararr_count;
        }
        while (count < utflen) {
            c = bytearr[count] & 255;
            chararr_count = c >> 4;
            switch (chararr_count) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    count++;
                    chararr_count = chararr_count2 + 1;
                    chararr[chararr_count2] = (char) c;
                    chararr_count2 = chararr_count;
                    break;
                default:
                    int chararr_count3;
                    StringBuilder stringBuilder;
                    switch (chararr_count) {
                        case 12:
                        case 13:
                            count += 2;
                            if (count <= utflen) {
                                chararr_count = bytearr[count - 1];
                                if ((chararr_count & 192) == 128) {
                                    chararr_count3 = chararr_count2 + 1;
                                    chararr[chararr_count2] = (char) (((c & 31) << 6) | (chararr_count & 63));
                                    break;
                                }
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("malformed input around byte ");
                                stringBuilder.append(count);
                                throw new UTFDataFormatException(stringBuilder.toString());
                            }
                            throw new UTFDataFormatException("malformed input: partial character at end");
                        case 14:
                            count += 3;
                            if (count <= utflen) {
                                chararr_count = bytearr[count - 2];
                                int char3 = bytearr[count - 1];
                                if ((chararr_count & 192) == 128 && (char3 & 192) == 128) {
                                    chararr_count3 = chararr_count2 + 1;
                                    chararr[chararr_count2] = (char) ((((c & 15) << 12) | ((chararr_count & 63) << 6)) | ((char3 & 63) << 0));
                                    break;
                                }
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("malformed input around byte ");
                                stringBuilder.append(count - 1);
                                throw new UTFDataFormatException(stringBuilder.toString());
                            }
                            throw new UTFDataFormatException("malformed input: partial character at end");
                            break;
                        default:
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("malformed input around byte ");
                            stringBuilder2.append(count);
                            throw new UTFDataFormatException(stringBuilder2.toString());
                    }
                    chararr_count2 = chararr_count3;
                    break;
            }
        }
        return new String(chararr, 0, chararr_count2);
    }
}
