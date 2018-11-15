package org.bouncycastle.cms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.cms.OtherRevocationInfoFormat;
import org.bouncycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.rosstandart.RosstandartObjectIdentifiers;
import org.bouncycastle.asn1.teletrust.TeleTrusTObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.AttributeCertificate;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.CertificateList;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.cert.X509AttributeCertificateHolder;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.pqc.jcajce.spec.McElieceCCA2KeyGenParameterSpec;
import org.bouncycastle.util.CollectionStore;
import org.bouncycastle.util.Store;

class CMSSignedHelper {
    static final CMSSignedHelper INSTANCE = new CMSSignedHelper();
    private static final Map digestAlgs = new HashMap();
    private static final Map digestAliases = new HashMap();
    private static final Map encryptionAlgs = new HashMap();

    static {
        addEntries(NISTObjectIdentifiers.dsa_with_sha224, "SHA224", "DSA");
        addEntries(NISTObjectIdentifiers.dsa_with_sha256, "SHA256", "DSA");
        addEntries(NISTObjectIdentifiers.dsa_with_sha384, "SHA384", "DSA");
        addEntries(NISTObjectIdentifiers.dsa_with_sha512, "SHA512", "DSA");
        addEntries(OIWObjectIdentifiers.dsaWithSHA1, "SHA1", "DSA");
        addEntries(OIWObjectIdentifiers.md4WithRSA, "MD4", "RSA");
        addEntries(OIWObjectIdentifiers.md4WithRSAEncryption, "MD4", "RSA");
        addEntries(OIWObjectIdentifiers.md5WithRSA, "MD5", "RSA");
        addEntries(OIWObjectIdentifiers.sha1WithRSA, "SHA1", "RSA");
        addEntries(PKCSObjectIdentifiers.md2WithRSAEncryption, "MD2", "RSA");
        addEntries(PKCSObjectIdentifiers.md4WithRSAEncryption, "MD4", "RSA");
        addEntries(PKCSObjectIdentifiers.md5WithRSAEncryption, "MD5", "RSA");
        addEntries(PKCSObjectIdentifiers.sha1WithRSAEncryption, "SHA1", "RSA");
        addEntries(PKCSObjectIdentifiers.sha224WithRSAEncryption, "SHA224", "RSA");
        addEntries(PKCSObjectIdentifiers.sha256WithRSAEncryption, "SHA256", "RSA");
        addEntries(PKCSObjectIdentifiers.sha384WithRSAEncryption, "SHA384", "RSA");
        addEntries(PKCSObjectIdentifiers.sha512WithRSAEncryption, "SHA512", "RSA");
        addEntries(X9ObjectIdentifiers.ecdsa_with_SHA1, "SHA1", "ECDSA");
        addEntries(X9ObjectIdentifiers.ecdsa_with_SHA224, "SHA224", "ECDSA");
        addEntries(X9ObjectIdentifiers.ecdsa_with_SHA256, "SHA256", "ECDSA");
        addEntries(X9ObjectIdentifiers.ecdsa_with_SHA384, "SHA384", "ECDSA");
        addEntries(X9ObjectIdentifiers.ecdsa_with_SHA512, "SHA512", "ECDSA");
        addEntries(X9ObjectIdentifiers.id_dsa_with_sha1, "SHA1", "DSA");
        addEntries(EACObjectIdentifiers.id_TA_ECDSA_SHA_1, "SHA1", "ECDSA");
        addEntries(EACObjectIdentifiers.id_TA_ECDSA_SHA_224, "SHA224", "ECDSA");
        addEntries(EACObjectIdentifiers.id_TA_ECDSA_SHA_256, "SHA256", "ECDSA");
        addEntries(EACObjectIdentifiers.id_TA_ECDSA_SHA_384, "SHA384", "ECDSA");
        addEntries(EACObjectIdentifiers.id_TA_ECDSA_SHA_512, "SHA512", "ECDSA");
        addEntries(EACObjectIdentifiers.id_TA_RSA_v1_5_SHA_1, "SHA1", "RSA");
        addEntries(EACObjectIdentifiers.id_TA_RSA_v1_5_SHA_256, "SHA256", "RSA");
        addEntries(EACObjectIdentifiers.id_TA_RSA_PSS_SHA_1, "SHA1", "RSAandMGF1");
        addEntries(EACObjectIdentifiers.id_TA_RSA_PSS_SHA_256, "SHA256", "RSAandMGF1");
        encryptionAlgs.put(X9ObjectIdentifiers.id_dsa.getId(), "DSA");
        encryptionAlgs.put(PKCSObjectIdentifiers.rsaEncryption.getId(), "RSA");
        encryptionAlgs.put(TeleTrusTObjectIdentifiers.teleTrusTRSAsignatureAlgorithm, "RSA");
        encryptionAlgs.put(X509ObjectIdentifiers.id_ea_rsa.getId(), "RSA");
        encryptionAlgs.put(CMSSignedDataGenerator.ENCRYPTION_RSA_PSS, "RSAandMGF1");
        encryptionAlgs.put(CryptoProObjectIdentifiers.gostR3410_94.getId(), "GOST3410");
        encryptionAlgs.put(CryptoProObjectIdentifiers.gostR3410_2001.getId(), "ECGOST3410");
        encryptionAlgs.put("1.3.6.1.4.1.5849.1.6.2", "ECGOST3410");
        encryptionAlgs.put("1.3.6.1.4.1.5849.1.1.5", "GOST3410");
        encryptionAlgs.put(RosstandartObjectIdentifiers.id_tc26_gost_3410_12_256, "ECGOST3410-2012-256");
        encryptionAlgs.put(RosstandartObjectIdentifiers.id_tc26_gost_3410_12_512, "ECGOST3410-2012-512");
        encryptionAlgs.put(CryptoProObjectIdentifiers.gostR3411_94_with_gostR3410_2001.getId(), "ECGOST3410");
        encryptionAlgs.put(CryptoProObjectIdentifiers.gostR3411_94_with_gostR3410_94.getId(), "GOST3410");
        encryptionAlgs.put(RosstandartObjectIdentifiers.id_tc26_signwithdigest_gost_3410_12_256, "ECGOST3410-2012-256");
        encryptionAlgs.put(RosstandartObjectIdentifiers.id_tc26_signwithdigest_gost_3410_12_512, "ECGOST3410-2012-512");
        digestAlgs.put(PKCSObjectIdentifiers.md2.getId(), "MD2");
        digestAlgs.put(PKCSObjectIdentifiers.md4.getId(), "MD4");
        digestAlgs.put(PKCSObjectIdentifiers.md5.getId(), "MD5");
        digestAlgs.put(OIWObjectIdentifiers.idSHA1.getId(), "SHA1");
        digestAlgs.put(NISTObjectIdentifiers.id_sha224.getId(), "SHA224");
        digestAlgs.put(NISTObjectIdentifiers.id_sha256.getId(), "SHA256");
        digestAlgs.put(NISTObjectIdentifiers.id_sha384.getId(), "SHA384");
        digestAlgs.put(NISTObjectIdentifiers.id_sha512.getId(), "SHA512");
        digestAlgs.put(TeleTrusTObjectIdentifiers.ripemd128.getId(), "RIPEMD128");
        digestAlgs.put(TeleTrusTObjectIdentifiers.ripemd160.getId(), "RIPEMD160");
        digestAlgs.put(TeleTrusTObjectIdentifiers.ripemd256.getId(), "RIPEMD256");
        digestAlgs.put(CryptoProObjectIdentifiers.gostR3411.getId(), "GOST3411");
        digestAlgs.put("1.3.6.1.4.1.5849.1.2.1", "GOST3411");
        digestAlgs.put(RosstandartObjectIdentifiers.id_tc26_gost_3411_12_256, "GOST3411-2012-256");
        digestAlgs.put(RosstandartObjectIdentifiers.id_tc26_gost_3411_12_512, "GOST3411-2012-512");
        digestAliases.put("SHA1", new String[]{McElieceCCA2KeyGenParameterSpec.SHA1});
        digestAliases.put("SHA224", new String[]{McElieceCCA2KeyGenParameterSpec.SHA224});
        digestAliases.put("SHA256", new String[]{McElieceCCA2KeyGenParameterSpec.SHA256});
        digestAliases.put("SHA384", new String[]{McElieceCCA2KeyGenParameterSpec.SHA384});
        digestAliases.put("SHA512", new String[]{McElieceCCA2KeyGenParameterSpec.SHA512});
    }

    CMSSignedHelper() {
    }

    private static void addEntries(ASN1ObjectIdentifier aSN1ObjectIdentifier, String str, String str2) {
        digestAlgs.put(aSN1ObjectIdentifier.getId(), str);
        encryptionAlgs.put(aSN1ObjectIdentifier.getId(), str2);
    }

    AlgorithmIdentifier fixAlgID(AlgorithmIdentifier algorithmIdentifier) {
        return algorithmIdentifier.getParameters() == null ? new AlgorithmIdentifier(algorithmIdentifier.getAlgorithm(), DERNull.INSTANCE) : algorithmIdentifier;
    }

    Store getAttributeCertificates(ASN1Set aSN1Set) {
        if (aSN1Set == null) {
            return new CollectionStore(new ArrayList());
        }
        Collection arrayList = new ArrayList(aSN1Set.size());
        Enumeration objects = aSN1Set.getObjects();
        while (objects.hasMoreElements()) {
            ASN1Primitive toASN1Primitive = ((ASN1Encodable) objects.nextElement()).toASN1Primitive();
            if (toASN1Primitive instanceof ASN1TaggedObject) {
                arrayList.add(new X509AttributeCertificateHolder(AttributeCertificate.getInstance(((ASN1TaggedObject) toASN1Primitive).getObject())));
            }
        }
        return new CollectionStore(arrayList);
    }

    Store getCRLs(ASN1Set aSN1Set) {
        if (aSN1Set == null) {
            return new CollectionStore(new ArrayList());
        }
        Collection arrayList = new ArrayList(aSN1Set.size());
        Enumeration objects = aSN1Set.getObjects();
        while (objects.hasMoreElements()) {
            ASN1Primitive toASN1Primitive = ((ASN1Encodable) objects.nextElement()).toASN1Primitive();
            if (toASN1Primitive instanceof ASN1Sequence) {
                arrayList.add(new X509CRLHolder(CertificateList.getInstance(toASN1Primitive)));
            }
        }
        return new CollectionStore(arrayList);
    }

    Store getCertificates(ASN1Set aSN1Set) {
        if (aSN1Set == null) {
            return new CollectionStore(new ArrayList());
        }
        Collection arrayList = new ArrayList(aSN1Set.size());
        Enumeration objects = aSN1Set.getObjects();
        while (objects.hasMoreElements()) {
            ASN1Primitive toASN1Primitive = ((ASN1Encodable) objects.nextElement()).toASN1Primitive();
            if (toASN1Primitive instanceof ASN1Sequence) {
                arrayList.add(new X509CertificateHolder(Certificate.getInstance(toASN1Primitive)));
            }
        }
        return new CollectionStore(arrayList);
    }

    String getEncryptionAlgName(String str) {
        String str2 = (String) encryptionAlgs.get(str);
        return str2 != null ? str2 : str;
    }

    Store getOtherRevocationInfo(ASN1ObjectIdentifier aSN1ObjectIdentifier, ASN1Set aSN1Set) {
        if (aSN1Set == null) {
            return new CollectionStore(new ArrayList());
        }
        Collection arrayList = new ArrayList(aSN1Set.size());
        Enumeration objects = aSN1Set.getObjects();
        while (objects.hasMoreElements()) {
            ASN1Primitive toASN1Primitive = ((ASN1Encodable) objects.nextElement()).toASN1Primitive();
            if (toASN1Primitive instanceof ASN1TaggedObject) {
                ASN1TaggedObject instance = ASN1TaggedObject.getInstance(toASN1Primitive);
                if (instance.getTagNo() == 1) {
                    OtherRevocationInfoFormat instance2 = OtherRevocationInfoFormat.getInstance(instance, false);
                    if (aSN1ObjectIdentifier.equals(instance2.getInfoFormat())) {
                        arrayList.add(instance2.getInfo());
                    }
                }
            }
        }
        return new CollectionStore(arrayList);
    }

    void setSigningDigestAlgorithmMapping(ASN1ObjectIdentifier aSN1ObjectIdentifier, String str) {
        digestAlgs.put(aSN1ObjectIdentifier.getId(), str);
    }

    void setSigningEncryptionAlgorithmMapping(ASN1ObjectIdentifier aSN1ObjectIdentifier, String str) {
        encryptionAlgs.put(aSN1ObjectIdentifier.getId(), str);
    }
}
