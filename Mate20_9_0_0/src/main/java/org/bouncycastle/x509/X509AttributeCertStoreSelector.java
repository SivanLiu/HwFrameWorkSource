package org.bouncycastle.x509;

import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.Target;
import org.bouncycastle.asn1.x509.TargetInformation;
import org.bouncycastle.asn1.x509.Targets;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.util.Selector;

public class X509AttributeCertStoreSelector implements Selector {
    private X509AttributeCertificate attributeCert;
    private Date attributeCertificateValid;
    private AttributeCertificateHolder holder;
    private AttributeCertificateIssuer issuer;
    private BigInteger serialNumber;
    private Collection targetGroups = new HashSet();
    private Collection targetNames = new HashSet();

    private Set extractGeneralNames(Collection collection) throws IOException {
        if (collection == null || collection.isEmpty()) {
            return new HashSet();
        }
        HashSet hashSet = new HashSet();
        for (Object next : collection) {
            Object next2;
            if (!(next2 instanceof GeneralName)) {
                next2 = GeneralName.getInstance(ASN1Primitive.fromByteArray((byte[]) next2));
            }
            hashSet.add(next2);
        }
        return hashSet;
    }

    public void addTargetGroup(GeneralName generalName) {
        this.targetGroups.add(generalName);
    }

    public void addTargetGroup(byte[] bArr) throws IOException {
        addTargetGroup(GeneralName.getInstance(ASN1Primitive.fromByteArray(bArr)));
    }

    public void addTargetName(GeneralName generalName) {
        this.targetNames.add(generalName);
    }

    public void addTargetName(byte[] bArr) throws IOException {
        addTargetName(GeneralName.getInstance(ASN1Primitive.fromByteArray(bArr)));
    }

    public Object clone() {
        X509AttributeCertStoreSelector x509AttributeCertStoreSelector = new X509AttributeCertStoreSelector();
        x509AttributeCertStoreSelector.attributeCert = this.attributeCert;
        x509AttributeCertStoreSelector.attributeCertificateValid = getAttributeCertificateValid();
        x509AttributeCertStoreSelector.holder = this.holder;
        x509AttributeCertStoreSelector.issuer = this.issuer;
        x509AttributeCertStoreSelector.serialNumber = this.serialNumber;
        x509AttributeCertStoreSelector.targetGroups = getTargetGroups();
        x509AttributeCertStoreSelector.targetNames = getTargetNames();
        return x509AttributeCertStoreSelector;
    }

    public X509AttributeCertificate getAttributeCert() {
        return this.attributeCert;
    }

    public Date getAttributeCertificateValid() {
        return this.attributeCertificateValid != null ? new Date(this.attributeCertificateValid.getTime()) : null;
    }

    public AttributeCertificateHolder getHolder() {
        return this.holder;
    }

    public AttributeCertificateIssuer getIssuer() {
        return this.issuer;
    }

    public BigInteger getSerialNumber() {
        return this.serialNumber;
    }

    public Collection getTargetGroups() {
        return Collections.unmodifiableCollection(this.targetGroups);
    }

    public Collection getTargetNames() {
        return Collections.unmodifiableCollection(this.targetNames);
    }

    public boolean match(Object obj) {
        if (!(obj instanceof X509AttributeCertificate)) {
            return false;
        }
        X509AttributeCertificate x509AttributeCertificate = (X509AttributeCertificate) obj;
        if (this.attributeCert != null && !this.attributeCert.equals(x509AttributeCertificate)) {
            return false;
        }
        if (this.serialNumber != null && !x509AttributeCertificate.getSerialNumber().equals(this.serialNumber)) {
            return false;
        }
        if (this.holder != null && !x509AttributeCertificate.getHolder().equals(this.holder)) {
            return false;
        }
        if (this.issuer != null && !x509AttributeCertificate.getIssuer().equals(this.issuer)) {
            return false;
        }
        if (this.attributeCertificateValid != null) {
            try {
                x509AttributeCertificate.checkValidity(this.attributeCertificateValid);
            } catch (CertificateExpiredException e) {
                return false;
            } catch (CertificateNotYetValidException e2) {
                return false;
            }
        }
        if (!(this.targetNames.isEmpty() && this.targetGroups.isEmpty())) {
            byte[] extensionValue = x509AttributeCertificate.getExtensionValue(X509Extensions.TargetInformation.getId());
            if (extensionValue != null) {
                try {
                    int i;
                    int i2;
                    Target[] targets;
                    Targets[] targetsObjects = TargetInformation.getInstance(new ASN1InputStream(((DEROctetString) ASN1Primitive.fromByteArray(extensionValue)).getOctets()).readObject()).getTargetsObjects();
                    if (!this.targetNames.isEmpty()) {
                        i = 0;
                        i2 = i;
                        while (i < targetsObjects.length) {
                            targets = targetsObjects[i].getTargets();
                            for (Target targetName : targets) {
                                if (this.targetNames.contains(GeneralName.getInstance(targetName.getTargetName()))) {
                                    i2 = true;
                                    break;
                                }
                            }
                            i++;
                        }
                        if (i2 == 0) {
                            return false;
                        }
                    }
                    if (!this.targetGroups.isEmpty()) {
                        i = 0;
                        i2 = i;
                        while (i < targetsObjects.length) {
                            targets = targetsObjects[i].getTargets();
                            for (Target targetName2 : targets) {
                                if (this.targetGroups.contains(GeneralName.getInstance(targetName2.getTargetGroup()))) {
                                    i2 = true;
                                    break;
                                }
                            }
                            i++;
                        }
                        if (i2 == 0) {
                            return false;
                        }
                    }
                } catch (IOException e3) {
                    return false;
                } catch (IllegalArgumentException e4) {
                    return false;
                }
            }
        }
        return true;
    }

    public void setAttributeCert(X509AttributeCertificate x509AttributeCertificate) {
        this.attributeCert = x509AttributeCertificate;
    }

    public void setAttributeCertificateValid(Date date) {
        if (date != null) {
            this.attributeCertificateValid = new Date(date.getTime());
        } else {
            this.attributeCertificateValid = null;
        }
    }

    public void setHolder(AttributeCertificateHolder attributeCertificateHolder) {
        this.holder = attributeCertificateHolder;
    }

    public void setIssuer(AttributeCertificateIssuer attributeCertificateIssuer) {
        this.issuer = attributeCertificateIssuer;
    }

    public void setSerialNumber(BigInteger bigInteger) {
        this.serialNumber = bigInteger;
    }

    public void setTargetGroups(Collection collection) throws IOException {
        this.targetGroups = extractGeneralNames(collection);
    }

    public void setTargetNames(Collection collection) throws IOException {
        this.targetNames = extractGeneralNames(collection);
    }
}
