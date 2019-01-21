package org.bouncycastle.jce.provider;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.DHParameter;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.DHPublicKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.util.KeyUtil;

public class JCEDHPublicKey implements DHPublicKey {
    static final long serialVersionUID = -216691575254424324L;
    private DHParameterSpec dhSpec;
    private SubjectPublicKeyInfo info;
    private BigInteger y;

    JCEDHPublicKey(BigInteger bigInteger, DHParameterSpec dHParameterSpec) {
        this.y = bigInteger;
        this.dhSpec = dHParameterSpec;
    }

    JCEDHPublicKey(DHPublicKey dHPublicKey) {
        this.y = dHPublicKey.getY();
        this.dhSpec = dHPublicKey.getParams();
    }

    JCEDHPublicKey(DHPublicKeySpec dHPublicKeySpec) {
        this.y = dHPublicKeySpec.getY();
        this.dhSpec = new DHParameterSpec(dHPublicKeySpec.getP(), dHPublicKeySpec.getG());
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:19:0x009d in {7, 10, 12, 14, 17, 18, 22} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:242)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    JCEDHPublicKey(org.bouncycastle.asn1.x509.SubjectPublicKeyInfo r4) {
        /*
        r3 = this;
        r3.<init>();
        r3.info = r4;
        r0 = r4.parsePublicKey();	 Catch:{ IOException -> 0x009e }
        r0 = (org.bouncycastle.asn1.ASN1Integer) r0;	 Catch:{ IOException -> 0x009e }
        r0 = r0.getValue();
        r3.y = r0;
        r0 = r4.getAlgorithmId();
        r0 = r0.getParameters();
        r0 = org.bouncycastle.asn1.ASN1Sequence.getInstance(r0);
        r4 = r4.getAlgorithmId();
        r4 = r4.getAlgorithm();
        r1 = org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.dhKeyAgreement;
        r1 = r4.equals(r1);
        if (r1 != 0) goto L_0x006f;
        r1 = r3.isPKCSParam(r0);
        if (r1 == 0) goto L_0x0034;
        goto L_0x006f;
        r1 = org.bouncycastle.asn1.x9.X9ObjectIdentifiers.dhpublicnumber;
        r1 = r4.equals(r1);
        if (r1 == 0) goto L_0x0058;
        r4 = org.bouncycastle.asn1.x9.DHDomainParameters.getInstance(r0);
        r0 = new javax.crypto.spec.DHParameterSpec;
        r1 = r4.getP();
        r1 = r1.getValue();
        r4 = r4.getG();
        r4 = r4.getValue();
        r0.<init>(r1, r4);
        r3.dhSpec = r0;
        return;
        r0 = new java.lang.IllegalArgumentException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "unknown algorithm type: ";
        r1.append(r2);
        r1.append(r4);
        r4 = r1.toString();
        r0.<init>(r4);
        throw r0;
        r4 = org.bouncycastle.asn1.pkcs.DHParameter.getInstance(r0);
        r0 = r4.getL();
        if (r0 == 0) goto L_0x008f;
        r0 = new javax.crypto.spec.DHParameterSpec;
        r1 = r4.getP();
        r2 = r4.getG();
        r4 = r4.getL();
        r4 = r4.intValue();
        r0.<init>(r1, r2, r4);
        goto L_0x0055;
        r0 = new javax.crypto.spec.DHParameterSpec;
        r1 = r4.getP();
        r4 = r4.getG();
        r0.<init>(r1, r4);
        goto L_0x0055;
        return;
        r4 = move-exception;
        r4 = new java.lang.IllegalArgumentException;
        r0 = "invalid info structure in DH public key";
        r4.<init>(r0);
        throw r4;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jce.provider.JCEDHPublicKey.<init>(org.bouncycastle.asn1.x509.SubjectPublicKeyInfo):void");
    }

    JCEDHPublicKey(DHPublicKeyParameters dHPublicKeyParameters) {
        this.y = dHPublicKeyParameters.getY();
        this.dhSpec = new DHParameterSpec(dHPublicKeyParameters.getParameters().getP(), dHPublicKeyParameters.getParameters().getG(), dHPublicKeyParameters.getParameters().getL());
    }

    private boolean isPKCSParam(ASN1Sequence aSN1Sequence) {
        if (aSN1Sequence.size() == 2) {
            return true;
        }
        if (aSN1Sequence.size() > 3) {
            return false;
        }
        return ASN1Integer.getInstance(aSN1Sequence.getObjectAt(2)).getValue().compareTo(BigInteger.valueOf((long) ASN1Integer.getInstance(aSN1Sequence.getObjectAt(0)).getValue().bitLength())) <= 0;
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        this.y = (BigInteger) objectInputStream.readObject();
        this.dhSpec = new DHParameterSpec((BigInteger) objectInputStream.readObject(), (BigInteger) objectInputStream.readObject(), objectInputStream.readInt());
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeObject(getY());
        objectOutputStream.writeObject(this.dhSpec.getP());
        objectOutputStream.writeObject(this.dhSpec.getG());
        objectOutputStream.writeInt(this.dhSpec.getL());
    }

    public String getAlgorithm() {
        return "DH";
    }

    public byte[] getEncoded() {
        return this.info != null ? KeyUtil.getEncodedSubjectPublicKeyInfo(this.info) : KeyUtil.getEncodedSubjectPublicKeyInfo(new AlgorithmIdentifier(PKCSObjectIdentifiers.dhKeyAgreement, new DHParameter(this.dhSpec.getP(), this.dhSpec.getG(), this.dhSpec.getL())), new ASN1Integer(this.y));
    }

    public String getFormat() {
        return "X.509";
    }

    public DHParameterSpec getParams() {
        return this.dhSpec;
    }

    public BigInteger getY() {
        return this.y;
    }
}
