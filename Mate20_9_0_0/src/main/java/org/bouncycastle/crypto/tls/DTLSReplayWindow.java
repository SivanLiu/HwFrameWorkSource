package org.bouncycastle.crypto.tls;

class DTLSReplayWindow {
    private static final long VALID_SEQ_MASK = 281474976710655L;
    private static final long WINDOW_SIZE = 64;
    private long bitmap = 0;
    private long latestConfirmedSeq = -1;

    DTLSReplayWindow() {
    }

    void reportAuthenticated(long j) {
        if ((VALID_SEQ_MASK & j) == j) {
            long j2;
            if (j <= this.latestConfirmedSeq) {
                j2 = this.latestConfirmedSeq - j;
                if (j2 < WINDOW_SIZE) {
                    this.bitmap |= 1 << ((int) j2);
                    return;
                }
            }
            j2 = j - this.latestConfirmedSeq;
            if (j2 >= WINDOW_SIZE) {
                this.bitmap = 1;
            } else {
                this.bitmap <<= (int) j2;
                this.bitmap |= 1;
            }
            this.latestConfirmedSeq = j;
            return;
        }
        throw new IllegalArgumentException("'seq' out of range");
    }

    void reset() {
        this.latestConfirmedSeq = -1;
        this.bitmap = 0;
    }

    boolean shouldDiscard(long j) {
        if ((VALID_SEQ_MASK & j) != j) {
            return true;
        }
        if (j <= this.latestConfirmedSeq) {
            long j2 = this.latestConfirmedSeq - j;
            if (j2 >= WINDOW_SIZE || (this.bitmap & (1 << ((int) j2))) != 0) {
                return true;
            }
        }
        return false;
    }
}
