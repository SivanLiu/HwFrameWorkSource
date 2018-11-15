package org.bouncycastle.crypto.tls;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.bouncycastle.util.io.SimpleOutputStream;

class RecordStream {
    private static int DEFAULT_PLAINTEXT_LIMIT = 16384;
    static final int TLS_HEADER_LENGTH_OFFSET = 3;
    static final int TLS_HEADER_SIZE = 5;
    static final int TLS_HEADER_TYPE_OFFSET = 0;
    static final int TLS_HEADER_VERSION_OFFSET = 1;
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private int ciphertextLimit;
    private int compressedLimit;
    private TlsProtocol handler;
    private TlsHandshakeHash handshakeHash = null;
    private SimpleOutputStream handshakeHashUpdater = new SimpleOutputStream() {
        public void write(byte[] bArr, int i, int i2) throws IOException {
            RecordStream.this.handshakeHash.update(bArr, i, i2);
        }
    };
    private InputStream input;
    private OutputStream output;
    private TlsCipher pendingCipher = null;
    private TlsCompression pendingCompression = null;
    private int plaintextLimit;
    private TlsCipher readCipher = null;
    private TlsCompression readCompression = null;
    private SequenceNumber readSeqNo = new SequenceNumber();
    private ProtocolVersion readVersion = null;
    private boolean restrictReadVersion = true;
    private TlsCipher writeCipher = null;
    private TlsCompression writeCompression = null;
    private SequenceNumber writeSeqNo = new SequenceNumber();
    private ProtocolVersion writeVersion = null;

    private static class SequenceNumber {
        private boolean exhausted;
        private long value;

        private SequenceNumber() {
            this.value = 0;
            this.exhausted = false;
        }

        /* synthetic */ SequenceNumber(AnonymousClass1 anonymousClass1) {
            this();
        }

        synchronized long nextValue(short s) throws TlsFatalAlert {
            long j;
            if (this.exhausted) {
                throw new TlsFatalAlert(s);
            }
            j = this.value;
            long j2 = this.value + 1;
            this.value = j2;
            if (j2 == 0) {
                this.exhausted = true;
            }
            return j;
        }
    }

    RecordStream(TlsProtocol tlsProtocol, InputStream inputStream, OutputStream outputStream) {
        this.handler = tlsProtocol;
        this.input = inputStream;
        this.output = outputStream;
        this.readCompression = new TlsNullCompression();
        this.writeCompression = this.readCompression;
    }

    private static void checkLength(int i, int i2, short s) throws IOException {
        if (i > i2) {
            throw new TlsFatalAlert(s);
        }
    }

    private static void checkType(short s, short s2) throws IOException {
        switch (s) {
            case (short) 20:
            case (short) 21:
            case (short) 22:
            case (short) 23:
                return;
            default:
                throw new TlsFatalAlert(s2);
        }
    }

    private byte[] getBufferContents() {
        byte[] toByteArray = this.buffer.toByteArray();
        this.buffer.reset();
        return toByteArray;
    }

    void checkRecordHeader(byte[] bArr) throws IOException {
        checkType(TlsUtils.readUint8(bArr, 0), (short) 10);
        if (this.restrictReadVersion) {
            ProtocolVersion readVersion = TlsUtils.readVersion(bArr, 1);
            if (!(this.readVersion == null || readVersion.equals(this.readVersion))) {
                throw new TlsFatalAlert((short) 47);
            }
        } else if ((TlsUtils.readVersionRaw(bArr, 1) & -256) != 768) {
            throw new TlsFatalAlert((short) 47);
        }
        checkLength(TlsUtils.readUint16(bArr, 3), this.ciphertextLimit, (short) 22);
    }

    byte[] decodeAndVerify(short s, InputStream inputStream, int i) throws IOException {
        byte[] readFully = TlsUtils.readFully(i, inputStream);
        byte[] decodeCiphertext = this.readCipher.decodeCiphertext(this.readSeqNo.nextValue((short) 10), s, readFully, 0, readFully.length);
        checkLength(decodeCiphertext.length, this.compressedLimit, (short) 22);
        OutputStream decompress = this.readCompression.decompress(this.buffer);
        if (decompress != this.buffer) {
            decompress.write(decodeCiphertext, 0, decodeCiphertext.length);
            decompress.flush();
            decodeCiphertext = getBufferContents();
        }
        checkLength(decodeCiphertext.length, this.plaintextLimit, (short) 30);
        if (decodeCiphertext.length >= 1 || s == (short) 23) {
            return decodeCiphertext;
        }
        throw new TlsFatalAlert((short) 47);
    }

    void finaliseHandshake() throws IOException {
        if (this.readCompression == this.pendingCompression && this.writeCompression == this.pendingCompression && this.readCipher == this.pendingCipher && this.writeCipher == this.pendingCipher) {
            this.pendingCompression = null;
            this.pendingCipher = null;
            return;
        }
        throw new TlsFatalAlert((short) 40);
    }

    void flush() throws IOException {
        this.output.flush();
    }

    TlsHandshakeHash getHandshakeHash() {
        return this.handshakeHash;
    }

    OutputStream getHandshakeHashUpdater() {
        return this.handshakeHashUpdater;
    }

    int getPlaintextLimit() {
        return this.plaintextLimit;
    }

    ProtocolVersion getReadVersion() {
        return this.readVersion;
    }

    void init(TlsContext tlsContext) {
        this.readCipher = new TlsNullCipher(tlsContext);
        this.writeCipher = this.readCipher;
        this.handshakeHash = new DeferredHash();
        this.handshakeHash.init(tlsContext);
        setPlaintextLimit(DEFAULT_PLAINTEXT_LIMIT);
    }

    void notifyHelloComplete() {
        this.handshakeHash = this.handshakeHash.notifyPRFDetermined();
    }

    TlsHandshakeHash prepareToFinish() {
        TlsHandshakeHash tlsHandshakeHash = this.handshakeHash;
        this.handshakeHash = this.handshakeHash.stopTracking();
        return tlsHandshakeHash;
    }

    boolean readRecord() throws IOException {
        byte[] readAllOrNothing = TlsUtils.readAllOrNothing(5, this.input);
        if (readAllOrNothing == null) {
            return false;
        }
        short readUint8 = TlsUtils.readUint8(readAllOrNothing, 0);
        checkType(readUint8, (short) 10);
        if (this.restrictReadVersion) {
            ProtocolVersion readVersion = TlsUtils.readVersion(readAllOrNothing, 1);
            if (this.readVersion == null) {
                this.readVersion = readVersion;
            } else if (!readVersion.equals(this.readVersion)) {
                throw new TlsFatalAlert((short) 47);
            }
        } else if ((TlsUtils.readVersionRaw(readAllOrNothing, 1) & -256) != 768) {
            throw new TlsFatalAlert((short) 47);
        }
        int readUint16 = TlsUtils.readUint16(readAllOrNothing, 3);
        checkLength(readUint16, this.ciphertextLimit, (short) 22);
        readAllOrNothing = decodeAndVerify(readUint8, this.input, readUint16);
        this.handler.processRecord(readUint8, readAllOrNothing, 0, readAllOrNothing.length);
        return true;
    }

    void receivedReadCipherSpec() throws IOException {
        if (this.pendingCompression == null || this.pendingCipher == null) {
            throw new TlsFatalAlert((short) 40);
        }
        this.readCompression = this.pendingCompression;
        this.readCipher = this.pendingCipher;
        this.readSeqNo = new SequenceNumber();
    }

    void safeClose() {
        try {
            this.input.close();
        } catch (IOException e) {
        }
        try {
            this.output.close();
        } catch (IOException e2) {
        }
    }

    void sentWriteCipherSpec() throws IOException {
        if (this.pendingCompression == null || this.pendingCipher == null) {
            throw new TlsFatalAlert((short) 40);
        }
        this.writeCompression = this.pendingCompression;
        this.writeCipher = this.pendingCipher;
        this.writeSeqNo = new SequenceNumber();
    }

    void setPendingConnectionState(TlsCompression tlsCompression, TlsCipher tlsCipher) {
        this.pendingCompression = tlsCompression;
        this.pendingCipher = tlsCipher;
    }

    void setPlaintextLimit(int i) {
        this.plaintextLimit = i;
        this.compressedLimit = this.plaintextLimit + 1024;
        this.ciphertextLimit = this.compressedLimit + 1024;
    }

    void setReadVersion(ProtocolVersion protocolVersion) {
        this.readVersion = protocolVersion;
    }

    void setRestrictReadVersion(boolean z) {
        this.restrictReadVersion = z;
    }

    void setWriteVersion(ProtocolVersion protocolVersion) {
        this.writeVersion = protocolVersion;
    }

    void writeRecord(short s, byte[] bArr, int i, int i2) throws IOException {
        if (this.writeVersion != null) {
            checkType(s, (short) 80);
            checkLength(i2, this.plaintextLimit, (short) 80);
            if (i2 >= 1 || s == (short) 23) {
                TlsCipher tlsCipher;
                short s2;
                byte[] bArr2;
                int i3;
                int i4;
                OutputStream compress = this.writeCompression.compress(this.buffer);
                long nextValue = this.writeSeqNo.nextValue((short) 80);
                if (compress == this.buffer) {
                    tlsCipher = this.writeCipher;
                    s2 = s;
                    bArr2 = bArr;
                    i3 = i;
                    i4 = i2;
                } else {
                    compress.write(bArr, i, i2);
                    compress.flush();
                    bArr2 = getBufferContents();
                    checkLength(bArr2.length, i2 + 1024, (short) 80);
                    tlsCipher = this.writeCipher;
                    i3 = 0;
                    i4 = bArr2.length;
                    s2 = s;
                }
                Object encodePlaintext = tlsCipher.encodePlaintext(nextValue, s2, bArr2, i3, i4);
                checkLength(encodePlaintext.length, this.ciphertextLimit, (short) 80);
                byte[] bArr3 = new byte[(encodePlaintext.length + 5)];
                TlsUtils.writeUint8(s, bArr3, 0);
                TlsUtils.writeVersion(this.writeVersion, bArr3, 1);
                TlsUtils.writeUint16(encodePlaintext.length, bArr3, 3);
                System.arraycopy(encodePlaintext, 0, bArr3, 5, encodePlaintext.length);
                this.output.write(bArr3);
                this.output.flush();
                return;
            }
            throw new TlsFatalAlert((short) 80);
        }
    }
}
