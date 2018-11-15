package org.bouncycastle.cms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.BEROctetStringGenerator;
import org.bouncycastle.asn1.BERSet;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.OtherRevocationInfoFormat;
import org.bouncycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.bouncycastle.asn1.ocsp.OCSPResponse;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.rosstandart.RosstandartObjectIdentifiers;
import org.bouncycastle.asn1.sec.SECObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.cert.X509AttributeCertificateHolder;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.io.Streams;
import org.bouncycastle.util.io.TeeInputStream;
import org.bouncycastle.util.io.TeeOutputStream;

class CMSUtils {
    private static final Set<String> des = new HashSet();
    private static final Set ecAlgs = new HashSet();
    private static final Set gostAlgs = new HashSet();
    private static final Set mqvAlgs = new HashSet();

    static {
        des.add("DES");
        des.add("DESEDE");
        des.add(OIWObjectIdentifiers.desCBC.getId());
        des.add(PKCSObjectIdentifiers.des_EDE3_CBC.getId());
        des.add(PKCSObjectIdentifiers.des_EDE3_CBC.getId());
        des.add(PKCSObjectIdentifiers.id_alg_CMS3DESwrap.getId());
        mqvAlgs.add(X9ObjectIdentifiers.mqvSinglePass_sha1kdf_scheme);
        mqvAlgs.add(SECObjectIdentifiers.mqvSinglePass_sha224kdf_scheme);
        mqvAlgs.add(SECObjectIdentifiers.mqvSinglePass_sha256kdf_scheme);
        mqvAlgs.add(SECObjectIdentifiers.mqvSinglePass_sha384kdf_scheme);
        mqvAlgs.add(SECObjectIdentifiers.mqvSinglePass_sha512kdf_scheme);
        ecAlgs.add(X9ObjectIdentifiers.dhSinglePass_cofactorDH_sha1kdf_scheme);
        ecAlgs.add(X9ObjectIdentifiers.dhSinglePass_stdDH_sha1kdf_scheme);
        ecAlgs.add(SECObjectIdentifiers.dhSinglePass_cofactorDH_sha224kdf_scheme);
        ecAlgs.add(SECObjectIdentifiers.dhSinglePass_stdDH_sha224kdf_scheme);
        ecAlgs.add(SECObjectIdentifiers.dhSinglePass_cofactorDH_sha256kdf_scheme);
        ecAlgs.add(SECObjectIdentifiers.dhSinglePass_stdDH_sha256kdf_scheme);
        ecAlgs.add(SECObjectIdentifiers.dhSinglePass_cofactorDH_sha384kdf_scheme);
        ecAlgs.add(SECObjectIdentifiers.dhSinglePass_stdDH_sha384kdf_scheme);
        ecAlgs.add(SECObjectIdentifiers.dhSinglePass_cofactorDH_sha512kdf_scheme);
        ecAlgs.add(SECObjectIdentifiers.dhSinglePass_stdDH_sha512kdf_scheme);
        gostAlgs.add(CryptoProObjectIdentifiers.gostR3410_2001_CryptoPro_ESDH);
        gostAlgs.add(RosstandartObjectIdentifiers.id_tc26_agreement_gost_3410_12_256);
        gostAlgs.add(RosstandartObjectIdentifiers.id_tc26_agreement_gost_3410_12_512);
    }

    CMSUtils() {
    }

    static InputStream attachDigestsToInputStream(Collection collection, InputStream inputStream) {
        for (DigestCalculator outputStream : collection) {
            inputStream = new TeeInputStream(inputStream, outputStream.getOutputStream());
        }
        return inputStream;
    }

    static OutputStream attachSignersToOutputStream(Collection collection, OutputStream outputStream) {
        for (SignerInfoGenerator calculatingOutputStream : collection) {
            outputStream = getSafeTeeOutputStream(outputStream, calculatingOutputStream.getCalculatingOutputStream());
        }
        return outputStream;
    }

    static OutputStream createBEROctetOutputStream(OutputStream outputStream, int i, boolean z, int i2) throws IOException {
        BEROctetStringGenerator bEROctetStringGenerator = new BEROctetStringGenerator(outputStream, i, z);
        return i2 != 0 ? bEROctetStringGenerator.getOctetOutputStream(new byte[i2]) : bEROctetStringGenerator.getOctetOutputStream();
    }

    static ASN1Set createBerSetFromList(List list) {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        for (ASN1Encodable add : list) {
            aSN1EncodableVector.add(add);
        }
        return new BERSet(aSN1EncodableVector);
    }

    static ASN1Set createDerSetFromList(List list) {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        for (ASN1Encodable add : list) {
            aSN1EncodableVector.add(add);
        }
        return new DERSet(aSN1EncodableVector);
    }

    static List getAttributeCertificatesFromStore(Store store) throws CMSException {
        List arrayList = new ArrayList();
        try {
            for (X509AttributeCertificateHolder toASN1Structure : store.getMatches(null)) {
                arrayList.add(new DERTaggedObject(false, 2, toASN1Structure.toASN1Structure()));
            }
            return arrayList;
        } catch (Exception e) {
            throw new CMSException("error processing certs", e);
        }
    }

    static List getCRLsFromStore(Store store) throws CMSException {
        List arrayList = new ArrayList();
        try {
            for (Object next : store.getMatches(null)) {
                Object next2;
                if (next2 instanceof X509CRLHolder) {
                    next2 = ((X509CRLHolder) next2).toASN1Structure();
                } else if (next2 instanceof OtherRevocationInfoFormat) {
                    ASN1Encodable instance = OtherRevocationInfoFormat.getInstance(next2);
                    validateInfoFormat(instance);
                    arrayList.add(new DERTaggedObject(false, 1, instance));
                } else if (!(next2 instanceof ASN1TaggedObject)) {
                }
                arrayList.add(next2);
            }
            return arrayList;
        } catch (Exception e) {
            throw new CMSException("error processing certs", e);
        }
    }

    static List getCertificatesFromStore(Store store) throws CMSException {
        List arrayList = new ArrayList();
        try {
            for (X509CertificateHolder toASN1Structure : store.getMatches(null)) {
                arrayList.add(toASN1Structure.toASN1Structure());
            }
            return arrayList;
        } catch (Exception e) {
            throw new CMSException("error processing certs", e);
        }
    }

    static Collection getOthersFromStore(ASN1ObjectIdentifier aSN1ObjectIdentifier, Store store) {
        Collection arrayList = new ArrayList();
        for (ASN1Encodable otherRevocationInfoFormat : store.getMatches(null)) {
            ASN1Encodable otherRevocationInfoFormat2 = new OtherRevocationInfoFormat(aSN1ObjectIdentifier, otherRevocationInfoFormat);
            validateInfoFormat(otherRevocationInfoFormat2);
            arrayList.add(new DERTaggedObject(false, 1, otherRevocationInfoFormat2));
        }
        return arrayList;
    }

    static OutputStream getSafeOutputStream(OutputStream outputStream) {
        return outputStream == null ? new NullOutputStream() : outputStream;
    }

    static OutputStream getSafeTeeOutputStream(OutputStream outputStream, OutputStream outputStream2) {
        return outputStream == null ? getSafeOutputStream(outputStream2) : outputStream2 == null ? getSafeOutputStream(outputStream) : new TeeOutputStream(outputStream, outputStream2);
    }

    static boolean isDES(String str) {
        return des.contains(Strings.toUpperCase(str));
    }

    static boolean isEC(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        return ecAlgs.contains(aSN1ObjectIdentifier);
    }

    static boolean isEquivalent(AlgorithmIdentifier algorithmIdentifier, AlgorithmIdentifier algorithmIdentifier2) {
        boolean z = false;
        if (algorithmIdentifier != null) {
            if (algorithmIdentifier2 == null || !algorithmIdentifier.getAlgorithm().equals(algorithmIdentifier2.getAlgorithm())) {
                return false;
            }
            ASN1Encodable parameters = algorithmIdentifier.getParameters();
            ASN1Encodable parameters2 = algorithmIdentifier2.getParameters();
            if (parameters != null) {
                if (parameters.equals(parameters2) || (parameters.equals(DERNull.INSTANCE) && parameters2 == null)) {
                    z = true;
                }
                return z;
            } else if (parameters2 == null || parameters2.equals(DERNull.INSTANCE)) {
                z = true;
            }
        }
        return z;
    }

    static boolean isGOST(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        return gostAlgs.contains(aSN1ObjectIdentifier);
    }

    static boolean isMQV(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        return mqvAlgs.contains(aSN1ObjectIdentifier);
    }

    static boolean isRFC2631(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        return aSN1ObjectIdentifier.equals(PKCSObjectIdentifiers.id_alg_ESDH) || aSN1ObjectIdentifier.equals(PKCSObjectIdentifiers.id_alg_SSDH);
    }

    static ContentInfo readContentInfo(InputStream inputStream) throws CMSException {
        return readContentInfo(new ASN1InputStream(inputStream));
    }

    private static ContentInfo readContentInfo(ASN1InputStream aSN1InputStream) throws CMSException {
        try {
            ContentInfo instance = ContentInfo.getInstance(aSN1InputStream.readObject());
            if (instance != null) {
                return instance;
            }
            throw new CMSException("No content found.");
        } catch (Exception e) {
            throw new CMSException("IOException reading content.", e);
        } catch (Exception e2) {
            throw new CMSException("Malformed content.", e2);
        } catch (Exception e22) {
            throw new CMSException("Malformed content.", e22);
        }
    }

    static ContentInfo readContentInfo(byte[] bArr) throws CMSException {
        return readContentInfo(new ASN1InputStream(bArr));
    }

    public static byte[] streamToByteArray(InputStream inputStream) throws IOException {
        return Streams.readAll(inputStream);
    }

    public static byte[] streamToByteArray(InputStream inputStream, int i) throws IOException {
        return Streams.readAllLimited(inputStream, i);
    }

    private static void validateInfoFormat(OtherRevocationInfoFormat otherRevocationInfoFormat) {
        if (CMSObjectIdentifiers.id_ri_ocsp_response.equals(otherRevocationInfoFormat.getInfoFormat()) && OCSPResponse.getInstance(otherRevocationInfoFormat.getInfo()).getResponseStatus().getValue().intValue() != 0) {
            throw new IllegalArgumentException("cannot add unsuccessful OCSP response to CMS SignedData");
        }
    }
}
