package org.bouncycastle.voms;

import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.Attribute;
import org.bouncycastle.asn1.x509.IetfAttrSyntax;
import org.bouncycastle.cert.X509AttributeCertificateHolder;

public class VOMSAttribute {
    public static final String VOMS_ATTR_OID = "1.3.6.1.4.1.8005.100.100.4";
    private X509AttributeCertificateHolder myAC;
    private List myFQANs = new ArrayList();
    private String myHostPort;
    private List myStringList = new ArrayList();
    private String myVo;

    public class FQAN {
        String capability;
        String fqan;
        String group;
        String role;

        public FQAN(String str) {
            this.fqan = str;
        }

        public FQAN(String str, String str2, String str3) {
            this.group = str;
            this.role = str2;
            this.capability = str3;
        }

        public String getCapability() {
            if (this.group == null && this.fqan != null) {
                split();
            }
            return this.capability;
        }

        public String getFQAN() {
            if (this.fqan != null) {
                return this.fqan;
            }
            String stringBuilder;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(this.group);
            stringBuilder2.append("/Role=");
            stringBuilder2.append(this.role != null ? this.role : "");
            if (this.capability != null) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("/Capability=");
                stringBuilder3.append(this.capability);
                stringBuilder = stringBuilder3.toString();
            } else {
                stringBuilder = "";
            }
            stringBuilder2.append(stringBuilder);
            this.fqan = stringBuilder2.toString();
            return this.fqan;
        }

        public String getGroup() {
            if (this.group == null && this.fqan != null) {
                split();
            }
            return this.group;
        }

        public String getRole() {
            if (this.group == null && this.fqan != null) {
                split();
            }
            return this.role;
        }

        protected void split() {
            this.fqan.length();
            int indexOf = this.fqan.indexOf("/Role=");
            if (indexOf >= 0) {
                this.group = this.fqan.substring(0, indexOf);
                indexOf += 6;
                int indexOf2 = this.fqan.indexOf("/Capability=", indexOf);
                String substring = indexOf2 < 0 ? this.fqan.substring(indexOf) : this.fqan.substring(indexOf, indexOf2);
                if (substring.length() == 0) {
                    substring = null;
                }
                this.role = substring;
                substring = indexOf2 < 0 ? null : this.fqan.substring(indexOf2 + 12);
                if (substring == null || substring.length() == 0) {
                    substring = null;
                }
                this.capability = substring;
            }
        }

        public String toString() {
            return getFQAN();
        }
    }

    public VOMSAttribute(X509AttributeCertificateHolder x509AttributeCertificateHolder) {
        if (x509AttributeCertificateHolder != null) {
            this.myAC = x509AttributeCertificateHolder;
            Attribute[] attributes = x509AttributeCertificateHolder.getAttributes(new ASN1ObjectIdentifier(VOMS_ATTR_OID));
            if (attributes != null) {
                int i = 0;
                while (i != attributes.length) {
                    StringBuilder stringBuilder;
                    try {
                        IetfAttrSyntax instance = IetfAttrSyntax.getInstance(attributes[i].getAttributeValues()[0]);
                        String string = ((DERIA5String) instance.getPolicyAuthority().getNames()[0].getName()).getString();
                        int indexOf = string.indexOf("://");
                        if (indexOf < 0 || indexOf == string.length() - 1) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Bad encoding of VOMS policyAuthority : [");
                            stringBuilder.append(string);
                            stringBuilder.append("]");
                            throw new IllegalArgumentException(stringBuilder.toString());
                        }
                        this.myVo = string.substring(0, indexOf);
                        this.myHostPort = string.substring(indexOf + 3);
                        if (instance.getValueType() == 1) {
                            ASN1OctetString[] aSN1OctetStringArr = (ASN1OctetString[]) instance.getValues();
                            for (int i2 = 0; i2 != aSN1OctetStringArr.length; i2++) {
                                String str = new String(aSN1OctetStringArr[i2].getOctets());
                                FQAN fqan = new FQAN(str);
                                if (!this.myStringList.contains(str)) {
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("/");
                                    stringBuilder2.append(this.myVo);
                                    stringBuilder2.append("/");
                                    if (str.startsWith(stringBuilder2.toString())) {
                                        this.myStringList.add(str);
                                        this.myFQANs.add(fqan);
                                    }
                                }
                            }
                            i++;
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("VOMS attribute values are not encoded as octet strings, policyAuthority = ");
                            stringBuilder.append(string);
                            throw new IllegalArgumentException(stringBuilder.toString());
                        }
                    } catch (IllegalArgumentException e) {
                        throw e;
                    } catch (Exception e2) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Badly encoded VOMS extension in AC issued by ");
                        stringBuilder.append(x509AttributeCertificateHolder.getIssuer());
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                }
                return;
            }
            return;
        }
        throw new IllegalArgumentException("VOMSAttribute: AttributeCertificate is NULL");
    }

    public X509AttributeCertificateHolder getAC() {
        return this.myAC;
    }

    public List getFullyQualifiedAttributes() {
        return this.myStringList;
    }

    public String getHostPort() {
        return this.myHostPort;
    }

    public List getListOfFQAN() {
        return this.myFQANs;
    }

    public String getVO() {
        return this.myVo;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("VO      :");
        stringBuilder.append(this.myVo);
        stringBuilder.append("\nHostPort:");
        stringBuilder.append(this.myHostPort);
        stringBuilder.append("\nFQANs   :");
        stringBuilder.append(this.myFQANs);
        return stringBuilder.toString();
    }
}
