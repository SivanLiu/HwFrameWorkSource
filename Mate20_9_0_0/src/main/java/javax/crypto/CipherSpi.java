package javax.crypto;

import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

public abstract class CipherSpi {
    protected abstract int engineDoFinal(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException;

    protected abstract byte[] engineDoFinal(byte[] bArr, int i, int i2) throws IllegalBlockSizeException, BadPaddingException;

    protected abstract int engineGetBlockSize();

    protected abstract byte[] engineGetIV();

    protected abstract int engineGetOutputSize(int i);

    protected abstract AlgorithmParameters engineGetParameters();

    protected abstract void engineInit(int i, Key key, AlgorithmParameters algorithmParameters, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException;

    protected abstract void engineInit(int i, Key key, SecureRandom secureRandom) throws InvalidKeyException;

    protected abstract void engineInit(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException;

    protected abstract void engineSetMode(String str) throws NoSuchAlgorithmException;

    protected abstract void engineSetPadding(String str) throws NoSuchPaddingException;

    protected abstract int engineUpdate(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws ShortBufferException;

    protected abstract byte[] engineUpdate(byte[] bArr, int i, int i2);

    protected int engineUpdate(ByteBuffer input, ByteBuffer output) throws ShortBufferException {
        try {
            return bufferCrypt(input, output, true);
        } catch (IllegalBlockSizeException e) {
            throw new ProviderException("Internal error in update()");
        } catch (BadPaddingException e2) {
            throw new ProviderException("Internal error in update()");
        }
    }

    protected int engineDoFinal(ByteBuffer input, ByteBuffer output) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        return bufferCrypt(input, output, false);
    }

    static int getTempArraySize(int totalSize) {
        return Math.min(4096, totalSize);
    }

    /* JADX WARNING: Removed duplicated region for block: B:69:0x016e  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x0162  */
    /* JADX WARNING: Removed duplicated region for block: B:79:0x0194 A:{LOOP_END, LOOP:1: B:39:0x00fb->B:79:0x0194} */
    /* JADX WARNING: Removed duplicated region for block: B:87:0x018e A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00d3 A:{LOOP_END, LOOP:0: B:21:0x008a->B:34:0x00d3} */
    /* JADX WARNING: Removed duplicated region for block: B:86:0x00cd A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:87:0x018e A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:79:0x0194 A:{LOOP_END, LOOP:1: B:39:0x00fb->B:79:0x0194} */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x0198 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x017d  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int bufferCrypt(ByteBuffer input, ByteBuffer output, boolean isUpdate) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        int outSize;
        ByteBuffer byteBuffer = input;
        ByteBuffer byteBuffer2 = output;
        if (byteBuffer == null || byteBuffer2 == null) {
            throw new NullPointerException("Input and output buffers must not be null");
        }
        int inPos = input.position();
        int inLimit = input.limit();
        int inLen = inLimit - inPos;
        if (isUpdate && inLen == 0) {
            return 0;
        }
        int outLenNeeded = engineGetOutputSize(inLen);
        if (output.remaining() >= outLenNeeded) {
            boolean a1 = input.hasArray();
            boolean a2 = output.hasArray();
            int inOfs;
            byte[] outArray;
            int outPos;
            int outOfs;
            int n;
            byte[] inArray;
            int inLen2;
            int inLen3;
            byte[] inArray2;
            if (a1 && a2) {
                byte[] inArray3 = input.array();
                inOfs = input.arrayOffset() + inPos;
                outArray = output.array();
                outPos = output.position();
                outOfs = output.arrayOffset() + outPos;
                if (isUpdate) {
                    n = engineUpdate(inArray3, inOfs, inLen, outArray, outOfs);
                } else {
                    n = engineDoFinal(inArray3, inOfs, inLen, outArray, outOfs);
                }
                byteBuffer.position(inLimit);
                byteBuffer2.position(outPos + n);
                return n;
            } else if (a1 || !a2) {
                int inOfs2;
                if (a1) {
                    inOfs2 = input.arrayOffset() + inPos;
                    inArray = input.array();
                } else {
                    inArray = new byte[getTempArraySize(inLen)];
                    inOfs2 = 0;
                }
                n = inOfs2;
                byte[] outArray2 = new byte[getTempArraySize(outLenNeeded)];
                inLen2 = inLen;
                inOfs = outArray2.length;
                int total = 0;
                boolean resized = false;
                byte[] outArray3 = outArray2;
                while (true) {
                    int chunk;
                    byte[] outArray4;
                    boolean resized2 = resized;
                    int chunk2 = Math.min(inLen2, inOfs == 0 ? inArray.length : inOfs);
                    if (a1 || resized2 || chunk2 <= 0) {
                        outOfs = n;
                    } else {
                        byteBuffer.get(inArray, 0, chunk2);
                        outOfs = 0;
                    }
                    if (isUpdate) {
                        chunk = chunk2;
                        outArray4 = outArray3;
                        inLen3 = inLen2;
                        inArray2 = inArray;
                    } else if (inLen2 != chunk2) {
                        chunk = chunk2;
                        outArray4 = outArray3;
                        inLen3 = inLen2;
                        inArray2 = inArray;
                    } else {
                        chunk = chunk2;
                        outArray4 = outArray3;
                        inLen3 = inLen2;
                        inArray2 = inArray;
                        try {
                            inLen = engineDoFinal(inArray, outOfs, chunk, outArray4, null);
                            resized2 = false;
                            n = chunk;
                            outOfs += n;
                            inLen2 = inLen3 - n;
                            if (inLen <= 0) {
                                outArray2 = outArray4;
                                try {
                                    byteBuffer2.put(outArray2, 0, inLen);
                                    total += inLen;
                                } catch (ShortBufferException e) {
                                    inLen = e;
                                    inLen3 = inLen2;
                                    if (resized2) {
                                    }
                                }
                            } else {
                                outArray2 = outArray4;
                            }
                            outArray3 = outArray2;
                            resized = false;
                            n = outOfs;
                        } catch (ShortBufferException e2) {
                            inLen = e2;
                            n = chunk;
                            outArray2 = outArray4;
                        }
                        if (inLen2 <= 0) {
                            if (a1) {
                                byteBuffer.position(inLimit);
                            }
                            return total;
                        }
                        inArray = inArray2;
                    }
                    try {
                        inLen = engineUpdate(inArray2, outOfs, chunk, outArray4, 0);
                        resized2 = false;
                        n = chunk;
                        outOfs += n;
                        inLen2 = inLen3 - n;
                        if (inLen <= 0) {
                        }
                        outArray3 = outArray2;
                        resized = false;
                        n = outOfs;
                    } catch (ShortBufferException e3) {
                        inLen = e3;
                        n = chunk;
                        outArray2 = outArray4;
                        if (resized2) {
                            outSize = engineGetOutputSize(n);
                            inOfs = outSize;
                            n = outOfs;
                            inLen2 = inLen3;
                            outArray3 = new byte[outSize];
                            resized = true;
                            if (inLen2 <= 0) {
                            }
                        } else {
                            throw ((ProviderException) new ProviderException("Could not determine buffer size").initCause(inLen));
                        }
                    }
                    if (inLen2 <= 0) {
                    }
                }
            } else {
                inOfs = output.position();
                outArray = output.array();
                inArray = new byte[getTempArraySize(inLen)];
                inLen2 = inLen;
                outPos = output.arrayOffset() + inOfs;
                inLen = 0;
                while (true) {
                    outSize = Math.min(inLen2, inArray.length);
                    if (outSize > 0) {
                        byteBuffer.get(inArray, 0, outSize);
                    }
                    if (isUpdate) {
                        outOfs = outSize;
                        inLen3 = inLen2;
                        inArray2 = inArray;
                    } else if (inLen2 != outSize) {
                        outOfs = outSize;
                        inLen3 = inLen2;
                        inArray2 = inArray;
                    } else {
                        outOfs = outSize;
                        inLen3 = inLen2;
                        inArray2 = inArray;
                        n = engineDoFinal(inArray, 0, outSize, outArray, outPos);
                        inLen += n;
                        outPos += n;
                        inLen2 = inLen3 - outOfs;
                        if (inLen2 > 0) {
                            byteBuffer2.position(inOfs + inLen);
                            return inLen;
                        }
                        inArray = inArray2;
                    }
                    n = engineUpdate(inArray2, 0, outOfs, outArray, outPos);
                    inLen += n;
                    outPos += n;
                    inLen2 = inLen3 - outOfs;
                    if (inLen2 > 0) {
                    }
                }
            }
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Need at least ");
            stringBuilder.append(outLenNeeded);
            stringBuilder.append(" bytes of space in output buffer");
            throw new ShortBufferException(stringBuilder.toString());
        }
    }

    protected byte[] engineWrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        throw new UnsupportedOperationException();
    }

    protected Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm, int wrappedKeyType) throws InvalidKeyException, NoSuchAlgorithmException {
        throw new UnsupportedOperationException();
    }

    protected int engineGetKeySize(Key key) throws InvalidKeyException {
        throw new UnsupportedOperationException();
    }

    protected void engineUpdateAAD(byte[] src, int offset, int len) {
        throw new UnsupportedOperationException("The underlying Cipher implementation does not support this method");
    }

    protected void engineUpdateAAD(ByteBuffer src) {
        throw new UnsupportedOperationException("The underlying Cipher implementation does not support this method");
    }
}
