package javax.net.ssl;

import java.util.Arrays;

public abstract class SNIServerName {
    private static final char[] HEXES = "0123456789ABCDEF".toCharArray();
    private final byte[] encoded;
    private final int type;

    protected SNIServerName(int type, byte[] encoded) {
        if (type < 0) {
            throw new IllegalArgumentException("Server name type cannot be less than zero");
        } else if (type <= 255) {
            this.type = type;
            if (encoded != null) {
                this.encoded = (byte[]) encoded.clone();
                return;
            }
            throw new NullPointerException("Server name encoded value cannot be null");
        } else {
            throw new IllegalArgumentException("Server name type cannot be greater than 255");
        }
    }

    public final int getType() {
        return this.type;
    }

    public final byte[] getEncoded() {
        return (byte[]) this.encoded.clone();
    }

    public boolean equals(Object other) {
        boolean z = true;
        if (this == other) {
            return true;
        }
        if (getClass() != other.getClass()) {
            return false;
        }
        SNIServerName that = (SNIServerName) other;
        if (!(this.type == that.type && Arrays.equals(this.encoded, that.encoded))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (31 * ((31 * 17) + this.type)) + Arrays.hashCode(this.encoded);
    }

    public String toString() {
        StringBuilder stringBuilder;
        if (this.type == 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("type=host_name (0), value=");
            stringBuilder.append(toHexString(this.encoded));
            return stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("type=(");
        stringBuilder.append(this.type);
        stringBuilder.append("), value=");
        stringBuilder.append(toHexString(this.encoded));
        return stringBuilder.toString();
    }

    private static String toHexString(byte[] bytes) {
        if (bytes.length == 0) {
            return "(empty)";
        }
        StringBuilder sb = new StringBuilder((bytes.length * 3) - 1);
        boolean isInitial = true;
        for (byte b : bytes) {
            if (isInitial) {
                isInitial = false;
            } else {
                sb.append(':');
            }
            int k = b & 255;
            sb.append(HEXES[k >>> 4]);
            sb.append(HEXES[k & 15]);
        }
        return sb.toString();
    }
}
