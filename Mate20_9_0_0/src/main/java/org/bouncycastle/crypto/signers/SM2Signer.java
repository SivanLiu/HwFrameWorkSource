package org.bouncycastle.crypto.signers;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithID;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECConstants;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECMultiplier;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

public class SM2Signer implements Signer, ECConstants {
    private final SM3Digest digest = new SM3Digest();
    private ECKeyParameters ecKey;
    private ECDomainParameters ecParams;
    private final DSAKCalculator kCalculator = new RandomDSAKCalculator();
    private ECPoint pubPoint;
    private byte[] z;

    private void addFieldElement(Digest digest, ECFieldElement eCFieldElement) {
        byte[] encoded = eCFieldElement.getEncoded();
        digest.update(encoded, 0, encoded.length);
    }

    private void addUserID(Digest digest, byte[] bArr) {
        int length = bArr.length * 8;
        digest.update((byte) ((length >> 8) & 255));
        digest.update((byte) (length & 255));
        digest.update(bArr, 0, bArr.length);
    }

    private byte[] digestDoFinal() {
        byte[] bArr = new byte[this.digest.getDigestSize()];
        this.digest.doFinal(bArr, 0);
        reset();
        return bArr;
    }

    private byte[] getZ(byte[] bArr) {
        this.digest.reset();
        addUserID(this.digest, bArr);
        addFieldElement(this.digest, this.ecParams.getCurve().getA());
        addFieldElement(this.digest, this.ecParams.getCurve().getB());
        addFieldElement(this.digest, this.ecParams.getG().getAffineXCoord());
        addFieldElement(this.digest, this.ecParams.getG().getAffineYCoord());
        addFieldElement(this.digest, this.pubPoint.getAffineXCoord());
        addFieldElement(this.digest, this.pubPoint.getAffineYCoord());
        bArr = new byte[this.digest.getDigestSize()];
        this.digest.doFinal(bArr, 0);
        return bArr;
    }

    /* JADX WARNING: Missing block: B:18:0x0070, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean verifySignature(BigInteger bigInteger, BigInteger bigInteger2) {
        BigInteger n = this.ecParams.getN();
        if (bigInteger.compareTo(ONE) < 0 || bigInteger.compareTo(n) >= 0 || bigInteger2.compareTo(ONE) < 0 || bigInteger2.compareTo(n) >= 0) {
            return false;
        }
        BigInteger calculateE = calculateE(digestDoFinal());
        BigInteger mod = bigInteger.add(bigInteger2).mod(n);
        if (mod.equals(ZERO)) {
            return false;
        }
        ECPoint normalize = ECAlgorithms.sumOfTwoMultiplies(this.ecParams.getG(), bigInteger2, ((ECPublicKeyParameters) this.ecKey).getQ(), mod).normalize();
        return normalize.isInfinity() ? false : calculateE.add(normalize.getAffineXCoord().toBigInteger()).mod(n).equals(bigInteger);
    }

    protected BigInteger calculateE(byte[] bArr) {
        return new BigInteger(1, bArr);
    }

    protected ECMultiplier createBasePointMultiplier() {
        return new FixedPointCombMultiplier();
    }

    protected BigInteger[] derDecode(byte[] bArr) throws IOException {
        ASN1Sequence instance = ASN1Sequence.getInstance(ASN1Primitive.fromByteArray(bArr));
        if (instance.size() != 2) {
            return null;
        }
        if (!Arrays.constantTimeAreEqual(derEncode(ASN1Integer.getInstance(instance.getObjectAt(0)).getValue(), ASN1Integer.getInstance(instance.getObjectAt(1)).getValue()), bArr)) {
            return null;
        }
        return new BigInteger[]{ASN1Integer.getInstance(instance.getObjectAt(0)).getValue(), ASN1Integer.getInstance(instance.getObjectAt(1)).getValue()};
    }

    protected byte[] derEncode(BigInteger bigInteger, BigInteger bigInteger2) throws IOException {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(new ASN1Integer(bigInteger));
        aSN1EncodableVector.add(new ASN1Integer(bigInteger2));
        return new DERSequence(aSN1EncodableVector).getEncoded(ASN1Encoding.DER);
    }

    public byte[] generateSignature() throws CryptoException {
        byte[] digestDoFinal = digestDoFinal();
        BigInteger n = this.ecParams.getN();
        BigInteger calculateE = calculateE(digestDoFinal);
        BigInteger d = ((ECPrivateKeyParameters) this.ecKey).getD();
        ECMultiplier createBasePointMultiplier = createBasePointMultiplier();
        while (true) {
            BigInteger nextK = this.kCalculator.nextK();
            BigInteger mod = calculateE.add(createBasePointMultiplier.multiply(this.ecParams.getG(), nextK).normalize().getAffineXCoord().toBigInteger()).mod(n);
            if (!(mod.equals(ZERO) || mod.add(nextK).equals(n))) {
                nextK = d.add(ONE).modInverse(n).multiply(nextK.subtract(mod.multiply(d)).mod(n)).mod(n);
                if (!nextK.equals(ZERO)) {
                    try {
                        return derEncode(mod, nextK);
                    } catch (IOException e) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("unable to encode signature: ");
                        stringBuilder.append(e.getMessage());
                        throw new CryptoException(stringBuilder.toString(), e);
                    }
                }
            }
        }
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        byte[] id;
        ECPoint normalize;
        if (cipherParameters instanceof ParametersWithID) {
            ParametersWithID parametersWithID = (ParametersWithID) cipherParameters;
            CipherParameters parameters = parametersWithID.getParameters();
            id = parametersWithID.getID();
            cipherParameters = parameters;
        } else {
            id = Hex.decode("31323334353637383132333435363738");
        }
        if (z) {
            if (cipherParameters instanceof ParametersWithRandom) {
                ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
                this.ecKey = (ECKeyParameters) parametersWithRandom.getParameters();
                this.ecParams = this.ecKey.getParameters();
                this.kCalculator.init(this.ecParams.getN(), parametersWithRandom.getRandom());
            } else {
                this.ecKey = (ECKeyParameters) cipherParameters;
                this.ecParams = this.ecKey.getParameters();
                this.kCalculator.init(this.ecParams.getN(), new SecureRandom());
            }
            normalize = createBasePointMultiplier().multiply(this.ecParams.getG(), ((ECPrivateKeyParameters) this.ecKey).getD()).normalize();
        } else {
            this.ecKey = (ECKeyParameters) cipherParameters;
            this.ecParams = this.ecKey.getParameters();
            normalize = ((ECPublicKeyParameters) this.ecKey).getQ();
        }
        this.pubPoint = normalize;
        this.z = getZ(id);
        this.digest.update(this.z, 0, this.z.length);
    }

    public void reset() {
        this.digest.reset();
        if (this.z != null) {
            this.digest.update(this.z, 0, this.z.length);
        }
    }

    public void update(byte b) {
        this.digest.update(b);
    }

    public void update(byte[] bArr, int i, int i2) {
        this.digest.update(bArr, i, i2);
    }

    public boolean verifySignature(byte[] bArr) {
        try {
            BigInteger[] derDecode = derDecode(bArr);
            if (derDecode != null) {
                return verifySignature(derDecode[0], derDecode[1]);
            }
        } catch (IOException e) {
        }
        return false;
    }
}
