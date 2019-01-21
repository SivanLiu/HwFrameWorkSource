package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;

public class GCMParameterSpec implements AlgorithmParameterSpec {
    private byte[] iv;
    private int tLen;

    public GCMParameterSpec(int tLen, byte[] src) {
        if (src != null) {
            init(tLen, src, 0, src.length);
            return;
        }
        throw new IllegalArgumentException("src array is null");
    }

    public GCMParameterSpec(int tLen, byte[] src, int offset, int len) {
        init(tLen, src, offset, len);
    }

    private void init(int tLen, byte[] src, int offset, int len) {
        if (tLen >= 0) {
            this.tLen = tLen;
            if (src == null || len < 0 || offset < 0 || len + offset > src.length) {
                throw new IllegalArgumentException("Invalid buffer arguments");
            }
            this.iv = new byte[len];
            System.arraycopy(src, offset, this.iv, 0, len);
            return;
        }
        throw new IllegalArgumentException("Length argument is negative");
    }

    public int getTLen() {
        return this.tLen;
    }

    public byte[] getIV() {
        return (byte[]) this.iv.clone();
    }
}
