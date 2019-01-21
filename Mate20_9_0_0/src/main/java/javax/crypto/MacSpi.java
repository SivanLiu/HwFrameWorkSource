package javax.crypto;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;

public abstract class MacSpi {
    protected abstract byte[] engineDoFinal();

    protected abstract int engineGetMacLength();

    protected abstract void engineInit(Key key, AlgorithmParameterSpec algorithmParameterSpec) throws InvalidKeyException, InvalidAlgorithmParameterException;

    protected abstract void engineReset();

    protected abstract void engineUpdate(byte b);

    protected abstract void engineUpdate(byte[] bArr, int i, int i2);

    protected void engineUpdate(ByteBuffer input) {
        if (input.hasRemaining()) {
            int pos;
            if (input.hasArray()) {
                byte[] b = input.array();
                int ofs = input.arrayOffset();
                pos = input.position();
                int lim = input.limit();
                engineUpdate(b, ofs + pos, lim - pos);
                input.position(lim);
            } else {
                int len = input.remaining();
                byte[] b2 = new byte[CipherSpi.getTempArraySize(len)];
                while (len > 0) {
                    pos = Math.min(len, b2.length);
                    input.get(b2, 0, pos);
                    engineUpdate(b2, 0, pos);
                    len -= pos;
                }
            }
        }
    }

    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        }
        throw new CloneNotSupportedException();
    }
}
