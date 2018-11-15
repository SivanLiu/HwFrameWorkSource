package org.bouncycastle.crypto.tls;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.io.Streams;

public class HeartbeatMessage {
    protected int paddingLength;
    protected byte[] payload;
    protected short type;

    static class PayloadBuffer extends ByteArrayOutputStream {
        PayloadBuffer() {
        }

        byte[] toTruncatedByteArray(int i) {
            return this.count < i + 16 ? null : Arrays.copyOf(this.buf, i);
        }
    }

    public HeartbeatMessage(short s, byte[] bArr, int i) {
        if (!HeartbeatMessageType.isValid(s)) {
            throw new IllegalArgumentException("'type' is not a valid HeartbeatMessageType value");
        } else if (bArr == null || bArr.length >= PKIFailureInfo.notAuthorized) {
            throw new IllegalArgumentException("'payload' must have length < 2^16");
        } else if (i >= 16) {
            this.type = s;
            this.payload = bArr;
            this.paddingLength = i;
        } else {
            throw new IllegalArgumentException("'paddingLength' must be at least 16");
        }
    }

    public static HeartbeatMessage parse(InputStream inputStream) throws IOException {
        short readUint8 = TlsUtils.readUint8(inputStream);
        if (HeartbeatMessageType.isValid(readUint8)) {
            int readUint16 = TlsUtils.readUint16(inputStream);
            OutputStream payloadBuffer = new PayloadBuffer();
            Streams.pipeAll(inputStream, payloadBuffer);
            byte[] toTruncatedByteArray = payloadBuffer.toTruncatedByteArray(readUint16);
            return toTruncatedByteArray == null ? null : new HeartbeatMessage(readUint8, toTruncatedByteArray, payloadBuffer.size() - toTruncatedByteArray.length);
        } else {
            throw new TlsFatalAlert((short) 47);
        }
    }

    public void encode(TlsContext tlsContext, OutputStream outputStream) throws IOException {
        TlsUtils.writeUint8(this.type, outputStream);
        TlsUtils.checkUint16(this.payload.length);
        TlsUtils.writeUint16(this.payload.length, outputStream);
        outputStream.write(this.payload);
        byte[] bArr = new byte[this.paddingLength];
        tlsContext.getNonceRandomGenerator().nextBytes(bArr);
        outputStream.write(bArr);
    }
}
