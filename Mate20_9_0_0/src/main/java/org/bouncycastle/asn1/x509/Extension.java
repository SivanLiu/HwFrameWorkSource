package org.bouncycastle.asn1.x509;

import java.io.IOException;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;

public class Extension extends ASN1Object {
    public static final ASN1ObjectIdentifier auditIdentity = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.1.4").intern();
    public static final ASN1ObjectIdentifier authorityInfoAccess = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.1.1").intern();
    public static final ASN1ObjectIdentifier authorityKeyIdentifier = new ASN1ObjectIdentifier("2.5.29.35").intern();
    public static final ASN1ObjectIdentifier basicConstraints = new ASN1ObjectIdentifier("2.5.29.19").intern();
    public static final ASN1ObjectIdentifier biometricInfo = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.1.2").intern();
    public static final ASN1ObjectIdentifier cRLDistributionPoints = new ASN1ObjectIdentifier("2.5.29.31").intern();
    public static final ASN1ObjectIdentifier cRLNumber = new ASN1ObjectIdentifier("2.5.29.20").intern();
    public static final ASN1ObjectIdentifier certificateIssuer = new ASN1ObjectIdentifier("2.5.29.29").intern();
    public static final ASN1ObjectIdentifier certificatePolicies = new ASN1ObjectIdentifier("2.5.29.32").intern();
    public static final ASN1ObjectIdentifier deltaCRLIndicator = new ASN1ObjectIdentifier("2.5.29.27").intern();
    public static final ASN1ObjectIdentifier expiredCertsOnCRL = new ASN1ObjectIdentifier("2.5.29.60").intern();
    public static final ASN1ObjectIdentifier extendedKeyUsage = new ASN1ObjectIdentifier("2.5.29.37").intern();
    public static final ASN1ObjectIdentifier freshestCRL = new ASN1ObjectIdentifier("2.5.29.46").intern();
    public static final ASN1ObjectIdentifier inhibitAnyPolicy = new ASN1ObjectIdentifier("2.5.29.54").intern();
    public static final ASN1ObjectIdentifier instructionCode = new ASN1ObjectIdentifier("2.5.29.23").intern();
    public static final ASN1ObjectIdentifier invalidityDate = new ASN1ObjectIdentifier("2.5.29.24").intern();
    public static final ASN1ObjectIdentifier issuerAlternativeName = new ASN1ObjectIdentifier("2.5.29.18").intern();
    public static final ASN1ObjectIdentifier issuingDistributionPoint = new ASN1ObjectIdentifier("2.5.29.28").intern();
    public static final ASN1ObjectIdentifier keyUsage = new ASN1ObjectIdentifier("2.5.29.15").intern();
    public static final ASN1ObjectIdentifier logoType = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.1.12").intern();
    public static final ASN1ObjectIdentifier nameConstraints = new ASN1ObjectIdentifier("2.5.29.30").intern();
    public static final ASN1ObjectIdentifier noRevAvail = new ASN1ObjectIdentifier("2.5.29.56").intern();
    public static final ASN1ObjectIdentifier policyConstraints = new ASN1ObjectIdentifier("2.5.29.36").intern();
    public static final ASN1ObjectIdentifier policyMappings = new ASN1ObjectIdentifier("2.5.29.33").intern();
    public static final ASN1ObjectIdentifier privateKeyUsagePeriod = new ASN1ObjectIdentifier("2.5.29.16").intern();
    public static final ASN1ObjectIdentifier qCStatements = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.1.3").intern();
    public static final ASN1ObjectIdentifier reasonCode = new ASN1ObjectIdentifier("2.5.29.21").intern();
    public static final ASN1ObjectIdentifier subjectAlternativeName = new ASN1ObjectIdentifier("2.5.29.17").intern();
    public static final ASN1ObjectIdentifier subjectDirectoryAttributes = new ASN1ObjectIdentifier("2.5.29.9").intern();
    public static final ASN1ObjectIdentifier subjectInfoAccess = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.1.11").intern();
    public static final ASN1ObjectIdentifier subjectKeyIdentifier = new ASN1ObjectIdentifier("2.5.29.14").intern();
    public static final ASN1ObjectIdentifier targetInformation = new ASN1ObjectIdentifier("2.5.29.55").intern();
    private boolean critical;
    private ASN1ObjectIdentifier extnId;
    private ASN1OctetString value;

    public Extension(ASN1ObjectIdentifier aSN1ObjectIdentifier, ASN1Boolean aSN1Boolean, ASN1OctetString aSN1OctetString) {
        this(aSN1ObjectIdentifier, aSN1Boolean.isTrue(), aSN1OctetString);
    }

    public Extension(ASN1ObjectIdentifier aSN1ObjectIdentifier, boolean z, ASN1OctetString aSN1OctetString) {
        this.extnId = aSN1ObjectIdentifier;
        this.critical = z;
        this.value = aSN1OctetString;
    }

    public Extension(ASN1ObjectIdentifier aSN1ObjectIdentifier, boolean z, byte[] bArr) {
        this(aSN1ObjectIdentifier, z, new DEROctetString(bArr));
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:8:0x0047 in {2, 4, 7, 10} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    private Extension(org.bouncycastle.asn1.ASN1Sequence r6) {
        /*
        r5 = this;
        r5.<init>();
        r0 = r6.size();
        r1 = 1;
        r2 = 2;
        r3 = 0;
        if (r0 != r2) goto L_0x0023;
    L_0x000c:
        r0 = r6.getObjectAt(r3);
        r0 = org.bouncycastle.asn1.ASN1ObjectIdentifier.getInstance(r0);
        r5.extnId = r0;
        r5.critical = r3;
        r6 = r6.getObjectAt(r1);
    L_0x001c:
        r6 = org.bouncycastle.asn1.ASN1OctetString.getInstance(r6);
        r5.value = r6;
        return;
    L_0x0023:
        r0 = r6.size();
        r4 = 3;
        if (r0 != r4) goto L_0x0048;
    L_0x002a:
        r0 = r6.getObjectAt(r3);
        r0 = org.bouncycastle.asn1.ASN1ObjectIdentifier.getInstance(r0);
        r5.extnId = r0;
        r0 = r6.getObjectAt(r1);
        r0 = org.bouncycastle.asn1.ASN1Boolean.getInstance(r0);
        r0 = r0.isTrue();
        r5.critical = r0;
        r6 = r6.getObjectAt(r2);
        goto L_0x001c;
        return;
    L_0x0048:
        r0 = new java.lang.IllegalArgumentException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "Bad sequence size: ";
        r1.append(r2);
        r6 = r6.size();
        r1.append(r6);
        r6 = r1.toString();
        r0.<init>(r6);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.x509.Extension.<init>(org.bouncycastle.asn1.ASN1Sequence):void");
    }

    private static ASN1Primitive convertValueToObject(Extension extension) throws IllegalArgumentException {
        try {
            return ASN1Primitive.fromByteArray(extension.getExtnValue().getOctets());
        } catch (IOException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("can't convert extension: ");
            stringBuilder.append(e);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public static Extension getInstance(Object obj) {
        return obj instanceof Extension ? (Extension) obj : obj != null ? new Extension(ASN1Sequence.getInstance(obj)) : null;
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!(obj instanceof Extension)) {
            return false;
        }
        Extension extension = (Extension) obj;
        if (extension.getExtnId().equals(getExtnId()) && extension.getExtnValue().equals(getExtnValue()) && extension.isCritical() == isCritical()) {
            z = true;
        }
        return z;
    }

    public ASN1ObjectIdentifier getExtnId() {
        return this.extnId;
    }

    public ASN1OctetString getExtnValue() {
        return this.value;
    }

    public ASN1Encodable getParsedValue() {
        return convertValueToObject(this);
    }

    public int hashCode() {
        return isCritical() ? getExtnValue().hashCode() ^ getExtnId().hashCode() : ~(getExtnValue().hashCode() ^ getExtnId().hashCode());
    }

    public boolean isCritical() {
        return this.critical;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.extnId);
        if (this.critical) {
            aSN1EncodableVector.add(ASN1Boolean.getInstance(true));
        }
        aSN1EncodableVector.add(this.value);
        return new DERSequence(aSN1EncodableVector);
    }
}
