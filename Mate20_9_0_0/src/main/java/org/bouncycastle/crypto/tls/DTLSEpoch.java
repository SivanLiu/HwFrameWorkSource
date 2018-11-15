package org.bouncycastle.crypto.tls;

class DTLSEpoch {
    private final TlsCipher cipher;
    private final int epoch;
    private final DTLSReplayWindow replayWindow = new DTLSReplayWindow();
    private long sequenceNumber = 0;

    DTLSEpoch(int i, TlsCipher tlsCipher) {
        if (i < 0) {
            throw new IllegalArgumentException("'epoch' must be >= 0");
        } else if (tlsCipher != null) {
            this.epoch = i;
            this.cipher = tlsCipher;
        } else {
            throw new IllegalArgumentException("'cipher' cannot be null");
        }
    }

    long allocateSequenceNumber() {
        long j = this.sequenceNumber;
        this.sequenceNumber = 1 + j;
        return j;
    }

    TlsCipher getCipher() {
        return this.cipher;
    }

    int getEpoch() {
        return this.epoch;
    }

    DTLSReplayWindow getReplayWindow() {
        return this.replayWindow;
    }

    long getSequenceNumber() {
        return this.sequenceNumber;
    }
}
