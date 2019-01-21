package org.bouncycastle.cms;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.BERSequenceGenerator;
import org.bouncycastle.asn1.BERTaggedObject;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.asn1.cms.SignerInfo;

public class CMSSignedDataStreamGenerator extends CMSSignedGenerator {
    private int _bufferSize;

    private class CmsSignedDataOutputStream extends OutputStream {
        private ASN1ObjectIdentifier _contentOID;
        private BERSequenceGenerator _eiGen;
        private OutputStream _out;
        private BERSequenceGenerator _sGen;
        private BERSequenceGenerator _sigGen;

        public CmsSignedDataOutputStream(OutputStream outputStream, ASN1ObjectIdentifier aSN1ObjectIdentifier, BERSequenceGenerator bERSequenceGenerator, BERSequenceGenerator bERSequenceGenerator2, BERSequenceGenerator bERSequenceGenerator3) {
            this._out = outputStream;
            this._contentOID = aSN1ObjectIdentifier;
            this._sGen = bERSequenceGenerator;
            this._sigGen = bERSequenceGenerator2;
            this._eiGen = bERSequenceGenerator3;
        }

        public void close() throws IOException {
            this._out.close();
            this._eiGen.close();
            CMSSignedDataStreamGenerator.this.digests.clear();
            if (CMSSignedDataStreamGenerator.this.certs.size() != 0) {
                this._sigGen.getRawOutputStream().write(new BERTaggedObject(false, 0, CMSUtils.createBerSetFromList(CMSSignedDataStreamGenerator.this.certs)).getEncoded());
            }
            if (CMSSignedDataStreamGenerator.this.crls.size() != 0) {
                this._sigGen.getRawOutputStream().write(new BERTaggedObject(false, 1, CMSUtils.createBerSetFromList(CMSSignedDataStreamGenerator.this.crls)).getEncoded());
            }
            ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
            for (SignerInfoGenerator signerInfoGenerator : CMSSignedDataStreamGenerator.this.signerGens) {
                try {
                    aSN1EncodableVector.add(signerInfoGenerator.generate(this._contentOID));
                    CMSSignedDataStreamGenerator.this.digests.put(signerInfoGenerator.getDigestAlgorithm().getAlgorithm().getId(), signerInfoGenerator.getCalculatedDigest());
                } catch (CMSException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("exception generating signers: ");
                    stringBuilder.append(e.getMessage());
                    throw new CMSStreamException(stringBuilder.toString(), e);
                }
            }
            for (SignerInformation toASN1Structure : CMSSignedDataStreamGenerator.this._signers) {
                aSN1EncodableVector.add(toASN1Structure.toASN1Structure());
            }
            this._sigGen.getRawOutputStream().write(new DERSet(aSN1EncodableVector).getEncoded());
            this._sigGen.close();
            this._sGen.close();
        }

        public void write(int i) throws IOException {
            this._out.write(i);
        }

        public void write(byte[] bArr) throws IOException {
            this._out.write(bArr);
        }

        public void write(byte[] bArr, int i, int i2) throws IOException {
            this._out.write(bArr, i, i2);
        }
    }

    private ASN1Integer calculateVersion(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        Object obj;
        Object obj2;
        Object obj3;
        Object obj4 = null;
        if (this.certs != null) {
            obj = null;
            obj2 = obj;
            obj3 = obj2;
            for (Object next : this.certs) {
                if (next instanceof ASN1TaggedObject) {
                    ASN1TaggedObject aSN1TaggedObject = (ASN1TaggedObject) next;
                    if (aSN1TaggedObject.getTagNo() == 1) {
                        obj3 = 1;
                    } else if (aSN1TaggedObject.getTagNo() == 2) {
                        obj2 = 1;
                    } else if (aSN1TaggedObject.getTagNo() == 3) {
                        obj = 1;
                    }
                }
            }
        } else {
            obj = null;
            obj2 = obj;
            obj3 = obj2;
        }
        if (obj != null) {
            return new ASN1Integer(5);
        }
        if (this.crls != null) {
            for (Object obj5 : this.crls) {
                if (obj5 instanceof ASN1TaggedObject) {
                    obj4 = 1;
                }
            }
        }
        return obj4 != null ? new ASN1Integer(5) : obj2 != null ? new ASN1Integer(4) : obj3 != null ? new ASN1Integer(3) : checkForVersion3(this._signers, this.signerGens) ? new ASN1Integer(3) : !CMSObjectIdentifiers.data.equals(aSN1ObjectIdentifier) ? new ASN1Integer(3) : new ASN1Integer(1);
    }

    private boolean checkForVersion3(List list, List list2) {
        for (SignerInformation toASN1Structure : list) {
            if (SignerInfo.getInstance(toASN1Structure.toASN1Structure()).getVersion().getValue().intValue() == 3) {
                return true;
            }
        }
        for (SignerInfoGenerator generatedVersion : list2) {
            if (generatedVersion.getGeneratedVersion() == 3) {
                return true;
            }
        }
        return false;
    }

    public OutputStream open(OutputStream outputStream) throws IOException {
        return open(outputStream, false);
    }

    public OutputStream open(OutputStream outputStream, boolean z) throws IOException {
        return open(CMSObjectIdentifiers.data, outputStream, z);
    }

    public OutputStream open(OutputStream outputStream, boolean z, OutputStream outputStream2) throws IOException {
        return open(CMSObjectIdentifiers.data, outputStream, z, outputStream2);
    }

    public OutputStream open(ASN1ObjectIdentifier aSN1ObjectIdentifier, OutputStream outputStream, boolean z) throws IOException {
        return open(aSN1ObjectIdentifier, outputStream, z, null);
    }

    public OutputStream open(ASN1ObjectIdentifier aSN1ObjectIdentifier, OutputStream outputStream, boolean z, OutputStream outputStream2) throws IOException {
        BERSequenceGenerator bERSequenceGenerator = new BERSequenceGenerator(outputStream);
        bERSequenceGenerator.addObject(CMSObjectIdentifiers.signedData);
        BERSequenceGenerator bERSequenceGenerator2 = new BERSequenceGenerator(bERSequenceGenerator.getRawOutputStream(), 0, true);
        bERSequenceGenerator2.addObject(calculateVersion(aSN1ObjectIdentifier));
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        for (SignerInformation digestAlgorithmID : this._signers) {
            aSN1EncodableVector.add(CMSSignedHelper.INSTANCE.fixAlgID(digestAlgorithmID.getDigestAlgorithmID()));
        }
        for (SignerInfoGenerator digestAlgorithm : this.signerGens) {
            aSN1EncodableVector.add(digestAlgorithm.getDigestAlgorithm());
        }
        bERSequenceGenerator2.getRawOutputStream().write(new DERSet(aSN1EncodableVector).getEncoded());
        BERSequenceGenerator bERSequenceGenerator3 = new BERSequenceGenerator(bERSequenceGenerator2.getRawOutputStream());
        bERSequenceGenerator3.addObject(aSN1ObjectIdentifier);
        return new CmsSignedDataOutputStream(CMSUtils.attachSignersToOutputStream(this.signerGens, CMSUtils.getSafeTeeOutputStream(outputStream2, z ? CMSUtils.createBEROctetOutputStream(bERSequenceGenerator3.getRawOutputStream(), 0, true, this._bufferSize) : null)), aSN1ObjectIdentifier, bERSequenceGenerator, bERSequenceGenerator2, bERSequenceGenerator3);
    }

    public void setBufferSize(int i) {
        this._bufferSize = i;
    }
}
