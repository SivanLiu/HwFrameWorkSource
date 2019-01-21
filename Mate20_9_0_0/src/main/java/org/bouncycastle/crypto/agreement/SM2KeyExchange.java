package org.bouncycastle.crypto.agreement;

import java.math.BigInteger;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithID;
import org.bouncycastle.crypto.params.SM2KeyExchangePrivateParameters;
import org.bouncycastle.crypto.params.SM2KeyExchangePublicParameters;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Memoable;
import org.bouncycastle.util.Pack;

public class SM2KeyExchange {
    private final Digest digest;
    private ECDomainParameters ecParams;
    private ECPrivateKeyParameters ephemeralKey;
    private ECPoint ephemeralPubPoint;
    private boolean initiator;
    private ECPrivateKeyParameters staticKey;
    private ECPoint staticPubPoint;
    private byte[] userID;
    private int w;

    public SM2KeyExchange() {
        this(new SM3Digest());
    }

    public SM2KeyExchange(Digest digest) {
        this.digest = digest;
    }

    private byte[] S1(Digest digest, ECPoint eCPoint, byte[] bArr) {
        digest.update((byte) 2);
        addFieldElement(digest, eCPoint.getAffineYCoord());
        digest.update(bArr, 0, bArr.length);
        return digestDoFinal();
    }

    private byte[] S2(Digest digest, ECPoint eCPoint, byte[] bArr) {
        digest.update((byte) 3);
        addFieldElement(digest, eCPoint.getAffineYCoord());
        digest.update(bArr, 0, bArr.length);
        return digestDoFinal();
    }

    private void addFieldElement(Digest digest, ECFieldElement eCFieldElement) {
        byte[] encoded = eCFieldElement.getEncoded();
        digest.update(encoded, 0, encoded.length);
    }

    private void addUserID(Digest digest, byte[] bArr) {
        int length = bArr.length * 8;
        digest.update((byte) (length >>> 8));
        digest.update((byte) length);
        digest.update(bArr, 0, bArr.length);
    }

    private byte[] calculateInnerHash(Digest digest, ECPoint eCPoint, byte[] bArr, byte[] bArr2, ECPoint eCPoint2, ECPoint eCPoint3) {
        addFieldElement(digest, eCPoint.getAffineXCoord());
        digest.update(bArr, 0, bArr.length);
        digest.update(bArr2, 0, bArr2.length);
        addFieldElement(digest, eCPoint2.getAffineXCoord());
        addFieldElement(digest, eCPoint2.getAffineYCoord());
        addFieldElement(digest, eCPoint3.getAffineXCoord());
        addFieldElement(digest, eCPoint3.getAffineYCoord());
        return digestDoFinal();
    }

    private ECPoint calculateU(SM2KeyExchangePublicParameters sM2KeyExchangePublicParameters) {
        ECPoint q = sM2KeyExchangePublicParameters.getStaticPublicKey().getQ();
        ECPoint q2 = sM2KeyExchangePublicParameters.getEphemeralPublicKey().getQ();
        BigInteger reduce = reduce(this.ephemeralPubPoint.getAffineXCoord().toBigInteger());
        BigInteger reduce2 = reduce(q2.getAffineXCoord().toBigInteger());
        reduce = this.ecParams.getH().multiply(this.staticKey.getD().add(reduce.multiply(this.ephemeralKey.getD()))).mod(this.ecParams.getN());
        return ECAlgorithms.sumOfTwoMultiplies(q, reduce, q2, reduce.multiply(reduce2).mod(this.ecParams.getN())).normalize();
    }

    private byte[] digestDoFinal() {
        byte[] bArr = new byte[this.digest.getDigestSize()];
        this.digest.doFinal(bArr, 0);
        return bArr;
    }

    private byte[] getZ(Digest digest, byte[] bArr, ECPoint eCPoint) {
        addUserID(digest, bArr);
        addFieldElement(digest, this.ecParams.getCurve().getA());
        addFieldElement(digest, this.ecParams.getCurve().getB());
        addFieldElement(digest, this.ecParams.getG().getAffineXCoord());
        addFieldElement(digest, this.ecParams.getG().getAffineYCoord());
        addFieldElement(digest, eCPoint.getAffineXCoord());
        addFieldElement(digest, eCPoint.getAffineYCoord());
        return digestDoFinal();
    }

    private byte[] kdf(ECPoint eCPoint, byte[] bArr, byte[] bArr2, int i) {
        Memoable copy;
        int digestSize = this.digest.getDigestSize();
        byte[] bArr3 = new byte[Math.max(4, digestSize)];
        byte[] bArr4 = new byte[((i + 7) / 8)];
        Memoable memoable = null;
        if (this.digest instanceof Memoable) {
            addFieldElement(this.digest, eCPoint.getAffineXCoord());
            addFieldElement(this.digest, eCPoint.getAffineYCoord());
            this.digest.update(bArr, 0, bArr.length);
            this.digest.update(bArr2, 0, bArr2.length);
            memoable = (Memoable) this.digest;
            copy = memoable.copy();
        } else {
            copy = null;
        }
        int i2 = 0;
        int i3 = i2;
        while (i2 < bArr4.length) {
            if (memoable != null) {
                memoable.reset(copy);
            } else {
                addFieldElement(this.digest, eCPoint.getAffineXCoord());
                addFieldElement(this.digest, eCPoint.getAffineYCoord());
                this.digest.update(bArr, 0, bArr.length);
                this.digest.update(bArr2, 0, bArr2.length);
            }
            i3++;
            Pack.intToBigEndian(i3, bArr3, 0);
            this.digest.update(bArr3, 0, 4);
            this.digest.doFinal(bArr3, 0);
            int min = Math.min(digestSize, bArr4.length - i2);
            System.arraycopy(bArr3, 0, bArr4, i2, min);
            i2 += min;
        }
        return bArr4;
    }

    private BigInteger reduce(BigInteger bigInteger) {
        return bigInteger.and(BigInteger.valueOf(1).shiftLeft(this.w).subtract(BigInteger.valueOf(1))).setBit(this.w);
    }

    public byte[] calculateKey(int i, CipherParameters cipherParameters) {
        SM2KeyExchangePublicParameters sM2KeyExchangePublicParameters;
        byte[] id;
        if (cipherParameters instanceof ParametersWithID) {
            ParametersWithID parametersWithID = (ParametersWithID) cipherParameters;
            sM2KeyExchangePublicParameters = (SM2KeyExchangePublicParameters) parametersWithID.getParameters();
            id = parametersWithID.getID();
        } else {
            sM2KeyExchangePublicParameters = (SM2KeyExchangePublicParameters) cipherParameters;
            id = new byte[0];
        }
        byte[] z = getZ(this.digest, this.userID, this.staticPubPoint);
        id = getZ(this.digest, id, sM2KeyExchangePublicParameters.getStaticPublicKey().getQ());
        ECPoint calculateU = calculateU(sM2KeyExchangePublicParameters);
        return this.initiator ? kdf(calculateU, z, id, i) : kdf(calculateU, id, z, i);
    }

    public byte[][] calculateKeyWithConfirmation(int i, byte[] bArr, CipherParameters cipherParameters) {
        SM2KeyExchangePublicParameters sM2KeyExchangePublicParameters;
        byte[] id;
        int i2 = i;
        byte[] bArr2 = bArr;
        CipherParameters cipherParameters2 = cipherParameters;
        if (cipherParameters2 instanceof ParametersWithID) {
            ParametersWithID parametersWithID = (ParametersWithID) cipherParameters2;
            sM2KeyExchangePublicParameters = (SM2KeyExchangePublicParameters) parametersWithID.getParameters();
            id = parametersWithID.getID();
        } else {
            sM2KeyExchangePublicParameters = (SM2KeyExchangePublicParameters) cipherParameters2;
            id = new byte[0];
        }
        if (this.initiator && bArr2 == null) {
            throw new IllegalArgumentException("if initiating, confirmationTag must be set");
        }
        byte[] z = getZ(this.digest, this.userID, this.staticPubPoint);
        byte[] z2 = getZ(this.digest, id, sM2KeyExchangePublicParameters.getStaticPublicKey().getQ());
        ECPoint calculateU = calculateU(sM2KeyExchangePublicParameters);
        if (this.initiator) {
            byte[] kdf = kdf(calculateU, z, z2, i2);
            if (Arrays.constantTimeAreEqual(S1(this.digest, calculateU, calculateInnerHash(this.digest, calculateU, z, z2, this.ephemeralPubPoint, sM2KeyExchangePublicParameters.getEphemeralPublicKey().getQ())), bArr2)) {
                return new byte[][]{kdf, S2(this.digest, calculateU, calculateInnerHash(this.digest, calculateU, z, z2, this.ephemeralPubPoint, sM2KeyExchangePublicParameters.getEphemeralPublicKey().getQ()))};
            }
            throw new IllegalStateException("confirmation tag mismatch");
        }
        bArr2 = kdf(calculateU, z2, z, i2);
        byte[] calculateInnerHash = calculateInnerHash(this.digest, calculateU, z2, z, sM2KeyExchangePublicParameters.getEphemeralPublicKey().getQ(), this.ephemeralPubPoint);
        return new byte[][]{bArr2, S1(this.digest, calculateU, calculateInnerHash), S2(this.digest, calculateU, calculateInnerHash)};
    }

    public void init(CipherParameters cipherParameters) {
        SM2KeyExchangePrivateParameters sM2KeyExchangePrivateParameters;
        byte[] id;
        if (cipherParameters instanceof ParametersWithID) {
            ParametersWithID parametersWithID = (ParametersWithID) cipherParameters;
            sM2KeyExchangePrivateParameters = (SM2KeyExchangePrivateParameters) parametersWithID.getParameters();
            id = parametersWithID.getID();
        } else {
            sM2KeyExchangePrivateParameters = (SM2KeyExchangePrivateParameters) cipherParameters;
            id = new byte[0];
        }
        this.userID = id;
        this.initiator = sM2KeyExchangePrivateParameters.isInitiator();
        this.staticKey = sM2KeyExchangePrivateParameters.getStaticPrivateKey();
        this.ephemeralKey = sM2KeyExchangePrivateParameters.getEphemeralPrivateKey();
        this.ecParams = this.staticKey.getParameters();
        this.staticPubPoint = sM2KeyExchangePrivateParameters.getStaticPublicPoint();
        this.ephemeralPubPoint = sM2KeyExchangePrivateParameters.getEphemeralPublicPoint();
        this.w = (this.ecParams.getCurve().getFieldSize() / 2) - 1;
    }
}
