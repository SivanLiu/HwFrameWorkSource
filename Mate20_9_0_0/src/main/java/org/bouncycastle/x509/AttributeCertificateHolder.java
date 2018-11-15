package org.bouncycastle.x509;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.Principal;
import java.security.cert.CertSelector;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.Holder;
import org.bouncycastle.asn1.x509.IssuerSerial;
import org.bouncycastle.asn1.x509.ObjectDigestInfo;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Selector;

public class AttributeCertificateHolder implements CertSelector, Selector {
    final Holder holder;

    public AttributeCertificateHolder(int i, String str, String str2, byte[] bArr) {
        this.holder = new Holder(new ObjectDigestInfo(i, new ASN1ObjectIdentifier(str2), new AlgorithmIdentifier(new ASN1ObjectIdentifier(str)), Arrays.clone(bArr)));
    }

    public AttributeCertificateHolder(X509Certificate x509Certificate) throws CertificateParsingException {
        try {
            this.holder = new Holder(new IssuerSerial(generateGeneralNames(PrincipalUtil.getIssuerX509Principal(x509Certificate)), new ASN1Integer(x509Certificate.getSerialNumber())));
        } catch (Exception e) {
            throw new CertificateParsingException(e.getMessage());
        }
    }

    public AttributeCertificateHolder(X500Principal x500Principal) {
        this(X509Util.convertPrincipal(x500Principal));
    }

    public AttributeCertificateHolder(X500Principal x500Principal, BigInteger bigInteger) {
        this(X509Util.convertPrincipal(x500Principal), bigInteger);
    }

    AttributeCertificateHolder(ASN1Sequence aSN1Sequence) {
        this.holder = Holder.getInstance(aSN1Sequence);
    }

    public AttributeCertificateHolder(X509Principal x509Principal) {
        this.holder = new Holder(generateGeneralNames(x509Principal));
    }

    public AttributeCertificateHolder(X509Principal x509Principal, BigInteger bigInteger) {
        this.holder = new Holder(new IssuerSerial(GeneralNames.getInstance(new DERSequence(new GeneralName((X509Name) x509Principal))), new ASN1Integer(bigInteger)));
    }

    private GeneralNames generateGeneralNames(X509Principal x509Principal) {
        return GeneralNames.getInstance(new DERSequence(new GeneralName((X509Name) x509Principal)));
    }

    private Object[] getNames(GeneralName[] generalNameArr) {
        List arrayList = new ArrayList(generalNameArr.length);
        for (int i = 0; i != generalNameArr.length; i++) {
            if (generalNameArr[i].getTagNo() == 4) {
                try {
                    arrayList.add(new X500Principal(generalNameArr[i].getName().toASN1Primitive().getEncoded()));
                } catch (IOException e) {
                    throw new RuntimeException("badly formed Name object");
                }
            }
        }
        return arrayList.toArray(new Object[arrayList.size()]);
    }

    private Principal[] getPrincipals(GeneralNames generalNames) {
        Object[] names = getNames(generalNames.getNames());
        List arrayList = new ArrayList();
        for (int i = 0; i != names.length; i++) {
            if (names[i] instanceof Principal) {
                arrayList.add(names[i]);
            }
        }
        return (Principal[]) arrayList.toArray(new Principal[arrayList.size()]);
    }

    private boolean matchesDN(X509Principal x509Principal, GeneralNames generalNames) {
        GeneralName[] names = generalNames.getNames();
        for (int i = 0; i != names.length; i++) {
            GeneralName generalName = names[i];
            if (generalName.getTagNo() == 4) {
                try {
                    if (new X509Principal(generalName.getName().toASN1Primitive().getEncoded()).equals(x509Principal)) {
                        return true;
                    }
                } catch (IOException e) {
                }
            }
        }
        return false;
    }

    public Object clone() {
        return new AttributeCertificateHolder((ASN1Sequence) this.holder.toASN1Primitive());
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof AttributeCertificateHolder)) {
            return false;
        }
        return this.holder.equals(((AttributeCertificateHolder) obj).holder);
    }

    public String getDigestAlgorithm() {
        return this.holder.getObjectDigestInfo() != null ? this.holder.getObjectDigestInfo().getDigestAlgorithm().getAlgorithm().getId() : null;
    }

    public int getDigestedObjectType() {
        return this.holder.getObjectDigestInfo() != null ? this.holder.getObjectDigestInfo().getDigestedObjectType().getValue().intValue() : -1;
    }

    public Principal[] getEntityNames() {
        return this.holder.getEntityName() != null ? getPrincipals(this.holder.getEntityName()) : null;
    }

    public Principal[] getIssuer() {
        return this.holder.getBaseCertificateID() != null ? getPrincipals(this.holder.getBaseCertificateID().getIssuer()) : null;
    }

    public byte[] getObjectDigest() {
        return this.holder.getObjectDigestInfo() != null ? this.holder.getObjectDigestInfo().getObjectDigest().getBytes() : null;
    }

    public String getOtherObjectTypeID() {
        if (this.holder.getObjectDigestInfo() != null) {
            this.holder.getObjectDigestInfo().getOtherObjectTypeID().getId();
        }
        return null;
    }

    public BigInteger getSerialNumber() {
        return this.holder.getBaseCertificateID() != null ? this.holder.getBaseCertificateID().getSerial().getValue() : null;
    }

    public int hashCode() {
        return this.holder.hashCode();
    }

    public boolean match(Object obj) {
        return !(obj instanceof X509Certificate) ? false : match((Certificate) obj);
    }

    /* JADX WARNING: Missing block: B:26:0x0077, code:
            r0.update(r5);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean match(Certificate certificate) {
        boolean z = false;
        if (!(certificate instanceof X509Certificate)) {
            return false;
        }
        X509Certificate x509Certificate = (X509Certificate) certificate;
        try {
            if (this.holder.getBaseCertificateID() != null) {
                if (this.holder.getBaseCertificateID().getSerial().getValue().equals(x509Certificate.getSerialNumber()) && matchesDN(PrincipalUtil.getIssuerX509Principal(x509Certificate), this.holder.getBaseCertificateID().getIssuer())) {
                    z = true;
                }
                return z;
            } else if (this.holder.getEntityName() != null && matchesDN(PrincipalUtil.getSubjectX509Principal(x509Certificate), this.holder.getEntityName())) {
                return true;
            } else {
                if (this.holder.getObjectDigestInfo() != null) {
                    try {
                        MessageDigest instance = MessageDigest.getInstance(getDigestAlgorithm(), "BC");
                        byte[] encoded;
                        switch (getDigestedObjectType()) {
                            case 0:
                                encoded = certificate.getPublicKey().getEncoded();
                                break;
                            case 1:
                                encoded = certificate.getEncoded();
                                break;
                            default:
                                if (!Arrays.areEqual(instance.digest(), getObjectDigest())) {
                                    return false;
                                }
                                break;
                        }
                    } catch (Exception e) {
                    }
                }
                return false;
            }
        } catch (CertificateEncodingException e2) {
            return false;
        }
    }
}
