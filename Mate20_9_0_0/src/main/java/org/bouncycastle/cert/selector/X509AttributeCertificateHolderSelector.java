package org.bouncycastle.cert.selector;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.Target;
import org.bouncycastle.asn1.x509.TargetInformation;
import org.bouncycastle.asn1.x509.Targets;
import org.bouncycastle.cert.AttributeCertificateHolder;
import org.bouncycastle.cert.AttributeCertificateIssuer;
import org.bouncycastle.cert.X509AttributeCertificateHolder;
import org.bouncycastle.util.Selector;

public class X509AttributeCertificateHolderSelector implements Selector {
    private final X509AttributeCertificateHolder attributeCert;
    private final Date attributeCertificateValid;
    private final AttributeCertificateHolder holder;
    private final AttributeCertificateIssuer issuer;
    private final BigInteger serialNumber;
    private final Collection targetGroups;
    private final Collection targetNames;

    X509AttributeCertificateHolderSelector(AttributeCertificateHolder attributeCertificateHolder, AttributeCertificateIssuer attributeCertificateIssuer, BigInteger bigInteger, Date date, X509AttributeCertificateHolder x509AttributeCertificateHolder, Collection collection, Collection collection2) {
        this.holder = attributeCertificateHolder;
        this.issuer = attributeCertificateIssuer;
        this.serialNumber = bigInteger;
        this.attributeCertificateValid = date;
        this.attributeCert = x509AttributeCertificateHolder;
        this.targetNames = collection;
        this.targetGroups = collection2;
    }

    public Object clone() {
        return new X509AttributeCertificateHolderSelector(this.holder, this.issuer, this.serialNumber, this.attributeCertificateValid, this.attributeCert, this.targetNames, this.targetGroups);
    }

    public X509AttributeCertificateHolder getAttributeCert() {
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
        return this.targetGroups;
    }

    public Collection getTargetNames() {
        return this.targetNames;
    }

    public boolean match(Object obj) {
        if (!(obj instanceof X509AttributeCertificateHolder)) {
            return false;
        }
        X509AttributeCertificateHolder x509AttributeCertificateHolder = (X509AttributeCertificateHolder) obj;
        if (this.attributeCert != null && !this.attributeCert.equals(x509AttributeCertificateHolder)) {
            return false;
        }
        if (this.serialNumber != null && !x509AttributeCertificateHolder.getSerialNumber().equals(this.serialNumber)) {
            return false;
        }
        if (this.holder != null && !x509AttributeCertificateHolder.getHolder().equals(this.holder)) {
            return false;
        }
        if (this.issuer != null && !x509AttributeCertificateHolder.getIssuer().equals(this.issuer)) {
            return false;
        }
        if (this.attributeCertificateValid != null && !x509AttributeCertificateHolder.isValidOn(this.attributeCertificateValid)) {
            return false;
        }
        if (!(this.targetNames.isEmpty() && this.targetGroups.isEmpty())) {
            Extension extension = x509AttributeCertificateHolder.getExtension(Extension.targetInformation);
            if (extension != null) {
                try {
                    int i;
                    int i2;
                    Target[] targets;
                    Targets[] targetsObjects = TargetInformation.getInstance(extension.getParsedValue()).getTargetsObjects();
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
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
        }
        return true;
    }
}
