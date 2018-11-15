package org.bouncycastle.jcajce.provider.asymmetric.dsa;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAParameterSpec;
import java.security.spec.DSAPublicKeySpec;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DSAParameter;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.util.KeyUtil;
import org.bouncycastle.util.Strings;

public class BCDSAPublicKey implements DSAPublicKey {
    private static BigInteger ZERO = BigInteger.valueOf(0);
    private static final long serialVersionUID = 1752452449903495175L;
    private transient DSAParams dsaSpec;
    private transient DSAPublicKeyParameters lwKeyParams;
    private BigInteger y;

    BCDSAPublicKey(DSAPublicKey dSAPublicKey) {
        this.y = dSAPublicKey.getY();
        this.dsaSpec = dSAPublicKey.getParams();
        this.lwKeyParams = new DSAPublicKeyParameters(this.y, DSAUtil.toDSAParameters(this.dsaSpec));
    }

    BCDSAPublicKey(DSAPublicKeySpec dSAPublicKeySpec) {
        this.y = dSAPublicKeySpec.getY();
        this.dsaSpec = new DSAParameterSpec(dSAPublicKeySpec.getP(), dSAPublicKeySpec.getQ(), dSAPublicKeySpec.getG());
        this.lwKeyParams = new DSAPublicKeyParameters(this.y, DSAUtil.toDSAParameters(this.dsaSpec));
    }

    public BCDSAPublicKey(SubjectPublicKeyInfo subjectPublicKeyInfo) {
        try {
            this.y = ((ASN1Integer) subjectPublicKeyInfo.parsePublicKey()).getValue();
            if (isNotNull(subjectPublicKeyInfo.getAlgorithm().getParameters())) {
                DSAParameter instance = DSAParameter.getInstance(subjectPublicKeyInfo.getAlgorithm().getParameters());
                this.dsaSpec = new DSAParameterSpec(instance.getP(), instance.getQ(), instance.getG());
            } else {
                this.dsaSpec = null;
            }
            this.lwKeyParams = new DSAPublicKeyParameters(this.y, DSAUtil.toDSAParameters(this.dsaSpec));
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid info structure in DSA public key");
        }
    }

    BCDSAPublicKey(DSAPublicKeyParameters dSAPublicKeyParameters) {
        this.y = dSAPublicKeyParameters.getY();
        this.dsaSpec = dSAPublicKeyParameters != null ? new DSAParameterSpec(dSAPublicKeyParameters.getParameters().getP(), dSAPublicKeyParameters.getParameters().getQ(), dSAPublicKeyParameters.getParameters().getG()) : null;
        this.lwKeyParams = dSAPublicKeyParameters;
    }

    private boolean isNotNull(ASN1Encodable aSN1Encodable) {
        return (aSN1Encodable == null || DERNull.INSTANCE.equals(aSN1Encodable.toASN1Primitive())) ? false : true;
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        BigInteger bigInteger = (BigInteger) objectInputStream.readObject();
        if (bigInteger.equals(ZERO)) {
            this.dsaSpec = null;
        } else {
            this.dsaSpec = new DSAParameterSpec(bigInteger, (BigInteger) objectInputStream.readObject(), (BigInteger) objectInputStream.readObject());
        }
        this.lwKeyParams = new DSAPublicKeyParameters(this.y, DSAUtil.toDSAParameters(this.dsaSpec));
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x0026 in {2, 4, 5} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    private void writeObject(java.io.ObjectOutputStream r2) throws java.io.IOException {
        /*
        r1 = this;
        r2.defaultWriteObject();
        r0 = r1.dsaSpec;
        if (r0 != 0) goto L_0x000d;
    L_0x0007:
        r0 = ZERO;
    L_0x0009:
        r2.writeObject(r0);
        return;
    L_0x000d:
        r0 = r1.dsaSpec;
        r0 = r0.getP();
        r2.writeObject(r0);
        r0 = r1.dsaSpec;
        r0 = r0.getQ();
        r2.writeObject(r0);
        r0 = r1.dsaSpec;
        r0 = r0.getG();
        goto L_0x0009;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jcajce.provider.asymmetric.dsa.BCDSAPublicKey.writeObject(java.io.ObjectOutputStream):void");
    }

    DSAPublicKeyParameters engineGetKeyParameters() {
        return this.lwKeyParams;
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!(obj instanceof DSAPublicKey)) {
            return false;
        }
        DSAPublicKey dSAPublicKey = (DSAPublicKey) obj;
        if (this.dsaSpec != null) {
            if (getY().equals(dSAPublicKey.getY()) && dSAPublicKey.getParams() != null && getParams().getG().equals(dSAPublicKey.getParams().getG()) && getParams().getP().equals(dSAPublicKey.getParams().getP()) && getParams().getQ().equals(dSAPublicKey.getParams().getQ())) {
                z = true;
            }
            return z;
        }
        if (getY().equals(dSAPublicKey.getY()) && dSAPublicKey.getParams() == null) {
            z = true;
        }
        return z;
    }

    public String getAlgorithm() {
        return "DSA";
    }

    public byte[] getEncoded() {
        AlgorithmIdentifier algorithmIdentifier;
        ASN1Encodable aSN1Integer;
        if (this.dsaSpec == null) {
            algorithmIdentifier = new AlgorithmIdentifier(X9ObjectIdentifiers.id_dsa);
            aSN1Integer = new ASN1Integer(this.y);
        } else {
            algorithmIdentifier = new AlgorithmIdentifier(X9ObjectIdentifiers.id_dsa, new DSAParameter(this.dsaSpec.getP(), this.dsaSpec.getQ(), this.dsaSpec.getG()).toASN1Primitive());
            aSN1Integer = new ASN1Integer(this.y);
        }
        return KeyUtil.getEncodedSubjectPublicKeyInfo(algorithmIdentifier, aSN1Integer);
    }

    public String getFormat() {
        return "X.509";
    }

    public DSAParams getParams() {
        return this.dsaSpec;
    }

    public BigInteger getY() {
        return this.y;
    }

    public int hashCode() {
        return this.dsaSpec != null ? ((getY().hashCode() ^ getParams().getG().hashCode()) ^ getParams().getP().hashCode()) ^ getParams().getQ().hashCode() : getY().hashCode();
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        String lineSeparator = Strings.lineSeparator();
        stringBuffer.append("DSA Public Key [");
        stringBuffer.append(DSAUtil.generateKeyFingerprint(this.y, getParams()));
        stringBuffer.append("]");
        stringBuffer.append(lineSeparator);
        stringBuffer.append("            y: ");
        stringBuffer.append(getY().toString(16));
        stringBuffer.append(lineSeparator);
        return stringBuffer.toString();
    }
}
