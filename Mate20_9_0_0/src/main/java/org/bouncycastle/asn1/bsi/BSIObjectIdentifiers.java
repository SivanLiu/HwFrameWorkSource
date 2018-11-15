package org.bouncycastle.asn1.bsi;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;

public interface BSIObjectIdentifiers {
    public static final ASN1ObjectIdentifier bsi_de = new ASN1ObjectIdentifier("0.4.0.127.0.7");
    public static final ASN1ObjectIdentifier ecdsa_plain_RIPEMD160 = ecdsa_plain_signatures.branch("6");
    public static final ASN1ObjectIdentifier ecdsa_plain_SHA1 = ecdsa_plain_signatures.branch("1");
    public static final ASN1ObjectIdentifier ecdsa_plain_SHA224 = ecdsa_plain_signatures.branch("2");
    public static final ASN1ObjectIdentifier ecdsa_plain_SHA256 = ecdsa_plain_signatures.branch("3");
    public static final ASN1ObjectIdentifier ecdsa_plain_SHA384 = ecdsa_plain_signatures.branch("4");
    public static final ASN1ObjectIdentifier ecdsa_plain_SHA512 = ecdsa_plain_signatures.branch("5");
    public static final ASN1ObjectIdentifier ecdsa_plain_signatures = id_ecc.branch("4.1");
    public static final ASN1ObjectIdentifier id_ecc = bsi_de.branch("1.1");
}
