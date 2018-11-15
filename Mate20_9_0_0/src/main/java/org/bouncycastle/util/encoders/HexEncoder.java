package org.bouncycastle.util.encoders;

import java.io.IOException;
import java.io.OutputStream;
import org.bouncycastle.crypto.tls.ExtensionType;

public class HexEncoder implements Encoder {
    protected final byte[] decodingTable = new byte[128];
    protected final byte[] encodingTable = new byte[]{(byte) 48, (byte) 49, (byte) 50, (byte) 51, (byte) 52, (byte) 53, (byte) 54, (byte) 55, (byte) 56, (byte) 57, (byte) 97, (byte) 98, (byte) 99, (byte) 100, (byte) 101, (byte) 102};

    public HexEncoder() {
        initialiseDecodingTable();
    }

    private static boolean ignore(char c) {
        return c == 10 || c == 13 || c == 9 || c == ' ';
    }

    public int decode(String str, OutputStream outputStream) throws IOException {
        int length = str.length();
        while (length > 0 && ignore(str.charAt(length - 1))) {
            length--;
        }
        int i = 0;
        int i2 = 0;
        while (i < length) {
            while (i < length && ignore(str.charAt(i))) {
                i++;
            }
            int i3 = i + 1;
            byte b = this.decodingTable[str.charAt(i)];
            while (i3 < length && ignore(str.charAt(i3))) {
                i3++;
            }
            int i4 = i3 + 1;
            byte b2 = this.decodingTable[str.charAt(i3)];
            if ((b | b2) >= 0) {
                outputStream.write((b << 4) | b2);
                i2++;
                i = i4;
            } else {
                throw new IOException("invalid characters encountered in Hex string");
            }
        }
        return i2;
    }

    public int decode(byte[] bArr, int i, int i2, OutputStream outputStream) throws IOException {
        i2 += i;
        while (i2 > i && ignore((char) bArr[i2 - 1])) {
            i2--;
        }
        int i3 = 0;
        while (i < i2) {
            while (i < i2 && ignore((char) bArr[i])) {
                i++;
            }
            int i4 = i + 1;
            byte b = this.decodingTable[bArr[i]];
            while (i4 < i2 && ignore((char) bArr[i4])) {
                i4++;
            }
            int i5 = i4 + 1;
            byte b2 = this.decodingTable[bArr[i4]];
            if ((b | b2) >= 0) {
                outputStream.write((b << 4) | b2);
                i3++;
                i = i5;
            } else {
                throw new IOException("invalid characters encountered in Hex data");
            }
        }
        return i3;
    }

    public int encode(byte[] bArr, int i, int i2, OutputStream outputStream) throws IOException {
        for (int i3 = i; i3 < i + i2; i3++) {
            int i4 = bArr[i3] & 255;
            outputStream.write(this.encodingTable[i4 >>> 4]);
            outputStream.write(this.encodingTable[i4 & 15]);
        }
        return i2 * 2;
    }

    protected void initialiseDecodingTable() {
        int i = 0;
        for (int i2 = 0; i2 < this.decodingTable.length; i2++) {
            this.decodingTable[i2] = (byte) -1;
        }
        while (i < this.encodingTable.length) {
            this.decodingTable[this.encodingTable[i]] = (byte) i;
            i++;
        }
        this.decodingTable[65] = this.decodingTable[97];
        this.decodingTable[66] = this.decodingTable[98];
        this.decodingTable[67] = this.decodingTable[99];
        this.decodingTable[68] = this.decodingTable[100];
        this.decodingTable[69] = this.decodingTable[ExtensionType.negotiated_ff_dhe_groups];
        this.decodingTable[70] = this.decodingTable[102];
    }
}
