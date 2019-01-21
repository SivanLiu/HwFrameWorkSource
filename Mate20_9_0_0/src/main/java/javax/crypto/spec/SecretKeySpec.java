package javax.crypto.spec;

import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Locale;
import javax.crypto.SecretKey;

public class SecretKeySpec implements KeySpec, SecretKey {
    private static final long serialVersionUID = 6577238317307289933L;
    private String algorithm;
    private byte[] key;

    public SecretKeySpec(byte[] key, String algorithm) {
        if (key == null || algorithm == null) {
            throw new IllegalArgumentException("Missing argument");
        } else if (key.length != 0) {
            this.key = (byte[]) key.clone();
            this.algorithm = algorithm;
        } else {
            throw new IllegalArgumentException("Empty key");
        }
    }

    public SecretKeySpec(byte[] key, int offset, int len, String algorithm) {
        if (key == null || algorithm == null) {
            throw new IllegalArgumentException("Missing argument");
        } else if (key.length == 0) {
            throw new IllegalArgumentException("Empty key");
        } else if (key.length - offset < len) {
            throw new IllegalArgumentException("Invalid offset/length combination");
        } else if (len >= 0) {
            this.key = new byte[len];
            System.arraycopy(key, offset, this.key, 0, len);
            this.algorithm = algorithm;
        } else {
            throw new ArrayIndexOutOfBoundsException("len is negative");
        }
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    public String getFormat() {
        return "RAW";
    }

    public byte[] getEncoded() {
        return (byte[]) this.key.clone();
    }

    public int hashCode() {
        int i;
        int retval = 0;
        for (i = 1; i < this.key.length; i++) {
            retval += this.key[i] * i;
        }
        if (this.algorithm.equalsIgnoreCase("TripleDES")) {
            i = "desede".hashCode() ^ retval;
            retval = i;
            return i;
        }
        i = this.algorithm.toLowerCase(Locale.ENGLISH).hashCode() ^ retval;
        retval = i;
        return i;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SecretKey)) {
            return false;
        }
        String thatAlg = ((SecretKey) obj).getAlgorithm();
        if (!thatAlg.equalsIgnoreCase(this.algorithm) && ((!thatAlg.equalsIgnoreCase("DESede") || !this.algorithm.equalsIgnoreCase("TripleDES")) && (!thatAlg.equalsIgnoreCase("TripleDES") || !this.algorithm.equalsIgnoreCase("DESede")))) {
            return false;
        }
        return MessageDigest.isEqual(this.key, ((SecretKey) obj).getEncoded());
    }
}
