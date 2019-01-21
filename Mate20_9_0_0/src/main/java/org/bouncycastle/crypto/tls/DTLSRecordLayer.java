package org.bouncycastle.crypto.tls;

import java.io.IOException;
import org.bouncycastle.asn1.cmc.BodyPartID;

class DTLSRecordLayer implements DatagramTransport {
    private static final int MAX_FRAGMENT_LENGTH = 16384;
    private static final int RECORD_HEADER_LENGTH = 13;
    private static final long RETRANSMIT_TIMEOUT = 240000;
    private static final long TCP_MSL = 120000;
    private volatile boolean closed = false;
    private final TlsContext context;
    private DTLSEpoch currentEpoch;
    private volatile boolean failed = false;
    private volatile boolean inHandshake;
    private final TlsPeer peer;
    private DTLSEpoch pendingEpoch;
    private volatile int plaintextLimit;
    private DTLSEpoch readEpoch;
    private volatile ProtocolVersion readVersion = null;
    private final ByteQueue recordQueue = new ByteQueue();
    private DTLSHandshakeRetransmit retransmit = null;
    private DTLSEpoch retransmitEpoch = null;
    private long retransmitExpiry = 0;
    private final DatagramTransport transport;
    private DTLSEpoch writeEpoch;
    private volatile ProtocolVersion writeVersion = null;

    DTLSRecordLayer(DatagramTransport datagramTransport, TlsContext tlsContext, TlsPeer tlsPeer, short s) {
        this.transport = datagramTransport;
        this.context = tlsContext;
        this.peer = tlsPeer;
        this.inHandshake = true;
        this.currentEpoch = new DTLSEpoch(0, new TlsNullCipher(tlsContext));
        this.pendingEpoch = null;
        this.readEpoch = this.currentEpoch;
        this.writeEpoch = this.currentEpoch;
        setPlaintextLimit(16384);
    }

    private void closeTransport() {
        if (!this.closed) {
            try {
                if (!this.failed) {
                    warn((short) 0, null);
                }
                this.transport.close();
            } catch (Exception e) {
            }
            this.closed = true;
        }
    }

    private static long getMacSequenceNumber(int i, long j) {
        return ((((long) i) & BodyPartID.bodyIdMax) << 48) | j;
    }

    private void raiseAlert(short s, short s2, String str, Throwable th) throws IOException {
        this.peer.notifyAlertRaised(s, s2, str, th);
        sendRecord((short) 21, new byte[]{(byte) s, (byte) s2}, 0, 2);
    }

    private int receiveRecord(byte[] bArr, int i, int i2, int i3) throws IOException {
        if (this.recordQueue.available() > 0) {
            if (this.recordQueue.available() >= 13) {
                byte[] bArr2 = new byte[2];
                this.recordQueue.read(bArr2, 0, 2, 11);
                i2 = TlsUtils.readUint16(bArr2, 0);
            } else {
                i2 = 0;
            }
            i2 = Math.min(this.recordQueue.available(), 13 + i2);
            this.recordQueue.removeData(bArr, i, i2, 0);
            return i2;
        }
        i2 = this.transport.receive(bArr, i, i2, i3);
        if (i2 >= 13) {
            i3 = TlsUtils.readUint16(bArr, i + 11) + 13;
            if (i2 > i3) {
                this.recordQueue.addData(bArr, i + i3, i2 - i3);
                i2 = i3;
            }
        }
        return i2;
    }

    private void sendRecord(short s, byte[] bArr, int i, int i2) throws IOException {
        short s2 = s;
        int i3 = i2;
        if (this.writeVersion != null) {
            if (i3 > this.plaintextLimit) {
                throw new TlsFatalAlert((short) 80);
            } else if (i3 >= 1 || s2 == (short) 23) {
                int epoch = this.writeEpoch.getEpoch();
                long allocateSequenceNumber = this.writeEpoch.allocateSequenceNumber();
                byte[] encodePlaintext = this.writeEpoch.getCipher().encodePlaintext(getMacSequenceNumber(epoch, allocateSequenceNumber), s2, bArr, i, i3);
                byte[] bArr2 = new byte[(encodePlaintext.length + 13)];
                TlsUtils.writeUint8(s2, bArr2, 0);
                TlsUtils.writeVersion(this.writeVersion, bArr2, 1);
                TlsUtils.writeUint16(epoch, bArr2, 3);
                TlsUtils.writeUint48(allocateSequenceNumber, bArr2, 5);
                TlsUtils.writeUint16(encodePlaintext.length, bArr2, 11);
                System.arraycopy(encodePlaintext, 0, bArr2, 13, encodePlaintext.length);
                this.transport.send(bArr2, 0, bArr2.length);
            } else {
                throw new TlsFatalAlert((short) 80);
            }
        }
    }

    public void close() throws IOException {
        if (!this.closed) {
            if (this.inHandshake) {
                warn((short) 90, "User canceled handshake");
            }
            closeTransport();
        }
    }

    void fail(short s) {
        if (!this.closed) {
            try {
                raiseAlert((short) 2, s, null, null);
            } catch (Exception e) {
            }
            this.failed = true;
            closeTransport();
        }
    }

    void failed() {
        if (!this.closed) {
            this.failed = true;
            closeTransport();
        }
    }

    int getReadEpoch() {
        return this.readEpoch.getEpoch();
    }

    ProtocolVersion getReadVersion() {
        return this.readVersion;
    }

    public int getReceiveLimit() throws IOException {
        return Math.min(this.plaintextLimit, this.readEpoch.getCipher().getPlaintextLimit(this.transport.getReceiveLimit() - 13));
    }

    public int getSendLimit() throws IOException {
        return Math.min(this.plaintextLimit, this.writeEpoch.getCipher().getPlaintextLimit(this.transport.getSendLimit() - 13));
    }

    void handshakeSuccessful(DTLSHandshakeRetransmit dTLSHandshakeRetransmit) {
        if (this.readEpoch == this.currentEpoch || this.writeEpoch == this.currentEpoch) {
            throw new IllegalStateException();
        }
        if (dTLSHandshakeRetransmit != null) {
            this.retransmit = dTLSHandshakeRetransmit;
            this.retransmitEpoch = this.currentEpoch;
            this.retransmitExpiry = System.currentTimeMillis() + RETRANSMIT_TIMEOUT;
        }
        this.inHandshake = false;
        this.currentEpoch = this.pendingEpoch;
        this.pendingEpoch = null;
    }

    void initPendingEpoch(TlsCipher tlsCipher) {
        if (this.pendingEpoch == null) {
            this.pendingEpoch = new DTLSEpoch(this.writeEpoch.getEpoch() + 1, tlsCipher);
            return;
        }
        throw new IllegalStateException();
    }

    /* JADX WARNING: Removed duplicated region for block: B:37:0x0078 A:{Catch:{ IOException -> 0x0148 }} */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x0077 A:{Catch:{ IOException -> 0x0148 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int receive(byte[] bArr, int i, int i2, int i3) throws IOException {
        byte[] decodeCiphertext;
        DTLSHandshakeRetransmit dTLSHandshakeRetransmit = null;
        byte[] bArr2 = null;
        while (true) {
            int min = Math.min(i2, getReceiveLimit()) + 13;
            if (bArr2 == null || bArr2.length < min) {
                bArr2 = new byte[min];
            }
            try {
                if (this.retransmit != null && System.currentTimeMillis() > this.retransmitExpiry) {
                    this.retransmit = dTLSHandshakeRetransmit;
                    this.retransmitEpoch = dTLSHandshakeRetransmit;
                }
                min = receiveRecord(bArr2, 0, min, i3);
                if (min < 0) {
                    return min;
                }
                byte[] bArr3;
                int i4;
                if (min >= 13) {
                    if (min == TlsUtils.readUint16(bArr2, 11) + 13) {
                        short readUint8 = TlsUtils.readUint8(bArr2, 0);
                        switch (readUint8) {
                            case (short) 20:
                            case (short) 21:
                            case (short) 22:
                            case (short) 23:
                            case (short) 24:
                                DTLSEpoch dTLSEpoch;
                                DTLSEpoch dTLSEpoch2;
                                int readUint16 = TlsUtils.readUint16(bArr2, 3);
                                if (readUint16 == this.readEpoch.getEpoch()) {
                                    dTLSEpoch = this.readEpoch;
                                } else if (readUint8 == (short) 22 && this.retransmitEpoch != null && readUint16 == this.retransmitEpoch.getEpoch()) {
                                    dTLSEpoch = this.retransmitEpoch;
                                } else {
                                    dTLSEpoch2 = dTLSHandshakeRetransmit;
                                    if (dTLSEpoch2 == null) {
                                        long readUint48 = TlsUtils.readUint48(bArr2, 5);
                                        if (!dTLSEpoch2.getReplayWindow().shouldDiscard(readUint48)) {
                                            ProtocolVersion readVersion = TlsUtils.readVersion(bArr2, 1);
                                            if (readVersion.isDTLS()) {
                                                if (this.readVersion == null || this.readVersion.equals(readVersion)) {
                                                    ProtocolVersion protocolVersion = readVersion;
                                                    long j = readUint48;
                                                    bArr3 = bArr2;
                                                    DTLSEpoch dTLSEpoch3 = dTLSEpoch2;
                                                    decodeCiphertext = dTLSEpoch2.getCipher().decodeCiphertext(getMacSequenceNumber(dTLSEpoch2.getEpoch(), readUint48), readUint8, bArr2, 13, min - 13);
                                                    dTLSEpoch3.getReplayWindow().reportAuthenticated(j);
                                                    if (decodeCiphertext.length <= this.plaintextLimit) {
                                                        if (this.readVersion == null) {
                                                            this.readVersion = protocolVersion;
                                                        }
                                                        switch (readUint8) {
                                                            case (short) 20:
                                                                for (int i5 = 0; i5 < decodeCiphertext.length; i5++) {
                                                                    if (TlsUtils.readUint8(decodeCiphertext, i5) == (short) 1) {
                                                                        if (this.pendingEpoch != null) {
                                                                            this.readEpoch = this.pendingEpoch;
                                                                        }
                                                                    }
                                                                }
                                                                break;
                                                            case (short) 21:
                                                                if (decodeCiphertext.length == 2) {
                                                                    short s = (short) decodeCiphertext[0];
                                                                    short s2 = (short) decodeCiphertext[1];
                                                                    this.peer.notifyAlertReceived(s, s2);
                                                                    if (s != (short) 2) {
                                                                        if (s2 == (short) 0) {
                                                                            closeTransport();
                                                                            break;
                                                                        }
                                                                    }
                                                                    failed();
                                                                    throw new TlsFatalAlert(s2);
                                                                }
                                                                break;
                                                            case (short) 22:
                                                                if (!this.inHandshake) {
                                                                    if (this.retransmit != null) {
                                                                        this.retransmit.receivedHandshakeRecord(readUint16, decodeCiphertext, 0, decodeCiphertext.length);
                                                                        break;
                                                                    }
                                                                }
                                                                break;
                                                                break;
                                                            case (short) 23:
                                                                if (!this.inHandshake) {
                                                                    break;
                                                                }
                                                                break;
                                                            case (short) 24:
                                                                break;
                                                            default:
                                                                break;
                                                        }
                                                    }
                                                    bArr2 = bArr;
                                                    i4 = i;
                                                    dTLSHandshakeRetransmit = null;
                                                    continue;
                                                }
                                            }
                                        }
                                    }
                                }
                                dTLSEpoch2 = dTLSEpoch;
                                if (dTLSEpoch2 == null) {
                                }
                                break;
                            default:
                        }
                    }
                }
                i4 = i;
                bArr3 = bArr2;
                bArr2 = bArr;
                bArr2 = bArr3;
            } catch (IOException e) {
                throw e;
            }
        }
        if (!(this.inHandshake || this.retransmit == null)) {
            this.retransmit = null;
            this.retransmitEpoch = null;
        }
        System.arraycopy(decodeCiphertext, 0, bArr, i, decodeCiphertext.length);
        return decodeCiphertext.length;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x000c in {2, 4, 5} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:242)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    void resetWriteEpoch() {
        /*
        r1 = this;
        r0 = r1.retransmitEpoch;
        if (r0 == 0) goto L_0x0009;
        r0 = r1.retransmitEpoch;
        r1.writeEpoch = r0;
        return;
        r0 = r1.currentEpoch;
        goto L_0x0006;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.crypto.tls.DTLSRecordLayer.resetWriteEpoch():void");
    }

    public void send(byte[] bArr, int i, int i2) throws IOException {
        short s;
        if (this.inHandshake || this.writeEpoch == this.retransmitEpoch) {
            s = (short) 22;
            if (TlsUtils.readUint8(bArr, i) == (short) 20) {
                DTLSEpoch dTLSEpoch = null;
                if (this.inHandshake) {
                    dTLSEpoch = this.pendingEpoch;
                } else if (this.writeEpoch == this.retransmitEpoch) {
                    dTLSEpoch = this.currentEpoch;
                }
                if (dTLSEpoch != null) {
                    byte[] bArr2 = new byte[]{(byte) 1};
                    sendRecord((short) 20, bArr2, 0, bArr2.length);
                    this.writeEpoch = dTLSEpoch;
                } else {
                    throw new IllegalStateException();
                }
            }
        }
        s = (short) 23;
        sendRecord(s, bArr, i, i2);
    }

    void setPlaintextLimit(int i) {
        this.plaintextLimit = i;
    }

    void setReadVersion(ProtocolVersion protocolVersion) {
        this.readVersion = protocolVersion;
    }

    void setWriteVersion(ProtocolVersion protocolVersion) {
        this.writeVersion = protocolVersion;
    }

    void warn(short s, String str) throws IOException {
        raiseAlert((short) 1, s, str, null);
    }
}
