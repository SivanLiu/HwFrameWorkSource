package org.bouncycastle.crypto.engines;

import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.math.ec.ECConstants;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECMultiplier;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.Memoable;
import org.bouncycastle.util.Pack;

public class SM2Engine {
    private int curveLength;
    private final Digest digest;
    private ECKeyParameters ecKey;
    private ECDomainParameters ecParams;
    private boolean forEncryption;
    private SecureRandom random;

    public SM2Engine() {
        this(new SM3Digest());
    }

    public SM2Engine(Digest digest) {
        this.digest = digest;
    }

    private void addFieldElement(Digest digest, ECFieldElement eCFieldElement) {
        byte[] asUnsignedByteArray = BigIntegers.asUnsignedByteArray(this.curveLength, eCFieldElement.toBigInteger());
        digest.update(asUnsignedByteArray, 0, asUnsignedByteArray.length);
    }

    private byte[] decrypt(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        byte[] bArr2 = new byte[((this.curveLength * 2) + 1)];
        System.arraycopy(bArr, i, bArr2, 0, bArr2.length);
        ECPoint decodePoint = this.ecParams.getCurve().decodePoint(bArr2);
        if (decodePoint.multiply(this.ecParams.getH()).isInfinity()) {
            throw new InvalidCipherTextException("[h]C1 at infinity");
        }
        decodePoint = decodePoint.multiply(((ECPrivateKeyParameters) this.ecKey).getD()).normalize();
        byte[] bArr3 = new byte[((i2 - bArr2.length) - this.digest.getDigestSize())];
        System.arraycopy(bArr, i + bArr2.length, bArr3, 0, bArr3.length);
        kdf(this.digest, decodePoint, bArr3);
        byte[] bArr4 = new byte[this.digest.getDigestSize()];
        addFieldElement(this.digest, decodePoint.getAffineXCoord());
        this.digest.update(bArr3, 0, bArr3.length);
        addFieldElement(this.digest, decodePoint.getAffineYCoord());
        this.digest.doFinal(bArr4, 0);
        int i3 = 0;
        int i4 = i3;
        while (i3 != bArr4.length) {
            i4 |= bArr4[i3] ^ bArr[(bArr2.length + bArr3.length) + i3];
            i3++;
        }
        Arrays.fill(bArr2, (byte) 0);
        Arrays.fill(bArr4, (byte) 0);
        if (i4 == 0) {
            return bArr3;
        }
        Arrays.fill(bArr3, (byte) 0);
        throw new InvalidCipherTextException("invalid cipher text");
    }

    private byte[] encrypt(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        byte[] encoded;
        ECPoint normalize;
        Object obj = new byte[i2];
        System.arraycopy(bArr, i, obj, 0, obj.length);
        ECMultiplier createBasePointMultiplier = createBasePointMultiplier();
        do {
            BigInteger nextK = nextK();
            encoded = createBasePointMultiplier.multiply(this.ecParams.getG(), nextK).normalize().getEncoded(false);
            normalize = ((ECPublicKeyParameters) this.ecKey).getQ().multiply(nextK).normalize();
            kdf(this.digest, normalize, obj);
        } while (notEncrypted(obj, bArr, i));
        byte[] bArr2 = new byte[this.digest.getDigestSize()];
        addFieldElement(this.digest, normalize.getAffineXCoord());
        this.digest.update(bArr, i, i2);
        addFieldElement(this.digest, normalize.getAffineYCoord());
        this.digest.doFinal(bArr2, 0);
        return Arrays.concatenate(encoded, obj, bArr2);
    }

    private void kdf(Digest digest, ECPoint eCPoint, byte[] bArr) {
        Memoable copy;
        int digestSize = digest.getDigestSize();
        byte[] bArr2 = new byte[Math.max(4, digestSize)];
        Memoable memoable = null;
        if (digest instanceof Memoable) {
            addFieldElement(digest, eCPoint.getAffineXCoord());
            addFieldElement(digest, eCPoint.getAffineYCoord());
            memoable = (Memoable) digest;
            copy = memoable.copy();
        } else {
            copy = null;
        }
        int i = 0;
        int i2 = i;
        while (i < bArr.length) {
            if (memoable != null) {
                memoable.reset(copy);
            } else {
                addFieldElement(digest, eCPoint.getAffineXCoord());
                addFieldElement(digest, eCPoint.getAffineYCoord());
            }
            i2++;
            Pack.intToBigEndian(i2, bArr2, 0);
            digest.update(bArr2, 0, 4);
            digest.doFinal(bArr2, 0);
            int min = Math.min(digestSize, bArr.length - i);
            xor(bArr, bArr2, i, min);
            i += min;
        }
    }

    private BigInteger nextK() {
        int bitLength = this.ecParams.getN().bitLength();
        while (true) {
            BigInteger bigInteger = new BigInteger(bitLength, this.random);
            if (!bigInteger.equals(ECConstants.ZERO) && bigInteger.compareTo(this.ecParams.getN()) < 0) {
                return bigInteger;
            }
        }
    }

    private boolean notEncrypted(byte[] bArr, byte[] bArr2, int i) {
        for (int i2 = 0; i2 != bArr.length; i2++) {
            if (bArr[i2] != bArr2[i]) {
                return false;
            }
        }
        return true;
    }

    private void xor(byte[] bArr, byte[] bArr2, int i, int i2) {
        for (int i3 = 0; i3 != i2; i3++) {
            int i4 = i + i3;
            bArr[i4] = (byte) (bArr[i4] ^ bArr2[i3]);
        }
    }

    protected ECMultiplier createBasePointMultiplier() {
        return new FixedPointCombMultiplier();
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        this.forEncryption = z;
        if (z) {
            ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
            this.ecKey = (ECKeyParameters) parametersWithRandom.getParameters();
            this.ecParams = this.ecKey.getParameters();
            if (((ECPublicKeyParameters) this.ecKey).getQ().multiply(this.ecParams.getH()).isInfinity()) {
                throw new IllegalArgumentException("invalid key: [h]Q at infinity");
            }
            this.random = parametersWithRandom.getRandom();
        } else {
            this.ecKey = (ECKeyParameters) cipherParameters;
            this.ecParams = this.ecKey.getParameters();
        }
        this.curveLength = (this.ecParams.getCurve().getFieldSize() + 7) / 8;
    }

    public byte[] processBlock(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        return this.forEncryption ? encrypt(bArr, i, i2) : decrypt(bArr, i, i2);
    }
}
