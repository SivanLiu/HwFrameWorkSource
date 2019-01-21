package java.security;

import java.nio.ByteBuffer;
import sun.security.jca.JCAUtil;

public abstract class MessageDigestSpi {
    private byte[] tempArray;

    protected abstract byte[] engineDigest();

    protected abstract void engineReset();

    protected abstract void engineUpdate(byte b);

    protected abstract void engineUpdate(byte[] bArr, int i, int i2);

    protected int engineGetDigestLength() {
        return 0;
    }

    protected void engineUpdate(ByteBuffer input) {
        if (input.hasRemaining()) {
            int ofs;
            int pos;
            if (input.hasArray()) {
                byte[] b = input.array();
                ofs = input.arrayOffset();
                pos = input.position();
                int lim = input.limit();
                engineUpdate(b, ofs + pos, lim - pos);
                input.position(lim);
            } else {
                int len = input.remaining();
                ofs = JCAUtil.getTempArraySize(len);
                if (this.tempArray == null || ofs > this.tempArray.length) {
                    this.tempArray = new byte[ofs];
                }
                while (len > 0) {
                    pos = Math.min(len, this.tempArray.length);
                    input.get(this.tempArray, 0, pos);
                    engineUpdate(this.tempArray, 0, pos);
                    len -= pos;
                }
            }
        }
    }

    protected int engineDigest(byte[] buf, int offset, int len) throws DigestException {
        byte[] digest = engineDigest();
        if (len < digest.length) {
            throw new DigestException("partial digests not returned");
        } else if (buf.length - offset >= digest.length) {
            System.arraycopy(digest, 0, buf, offset, digest.length);
            return digest.length;
        } else {
            throw new DigestException("insufficient space in the output buffer to store the digest");
        }
    }

    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        }
        throw new CloneNotSupportedException();
    }
}
