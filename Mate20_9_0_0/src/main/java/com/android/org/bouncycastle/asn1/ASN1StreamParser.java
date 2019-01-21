package com.android.org.bouncycastle.asn1;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ASN1StreamParser {
    private final InputStream _in;
    private final int _limit;
    private final byte[][] tmpBuffers;

    public ASN1StreamParser(InputStream in) {
        this(in, StreamUtil.findLimit(in));
    }

    public ASN1StreamParser(InputStream in, int limit) {
        this._in = in;
        this._limit = limit;
        this.tmpBuffers = new byte[11][];
    }

    public ASN1StreamParser(byte[] encoding) {
        this(new ByteArrayInputStream(encoding), encoding.length);
    }

    ASN1Encodable readIndef(int tagValue) throws IOException {
        if (tagValue == 4) {
            return new BEROctetStringParser(this);
        }
        if (tagValue == 8) {
            return new DERExternalParser(this);
        }
        switch (tagValue) {
            case 16:
                return new BERSequenceParser(this);
            case 17:
                return new BERSetParser(this);
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unknown BER object encountered: 0x");
                stringBuilder.append(Integer.toHexString(tagValue));
                throw new ASN1Exception(stringBuilder.toString());
        }
    }

    ASN1Encodable readImplicit(boolean constructed, int tag) throws IOException {
        if (!(this._in instanceof IndefiniteLengthInputStream)) {
            if (constructed) {
                if (tag == 4) {
                    return new BEROctetStringParser(this);
                }
                switch (tag) {
                    case 16:
                        return new DERSequenceParser(this);
                    case 17:
                        return new DERSetParser(this);
                }
            } else if (tag == 4) {
                return new DEROctetStringParser((DefiniteLengthInputStream) this._in);
            } else {
                switch (tag) {
                    case 16:
                        throw new ASN1Exception("sets must use constructed encoding (see X.690 8.11.1/8.12.1)");
                    case 17:
                        throw new ASN1Exception("sequences must use constructed encoding (see X.690 8.9.1/8.10.1)");
                }
            }
            throw new ASN1Exception("implicit tagging not implemented");
        } else if (constructed) {
            return readIndef(tag);
        } else {
            throw new IOException("indefinite-length primitive encoding encountered");
        }
    }

    ASN1Primitive readTaggedObject(boolean constructed, int tag) throws IOException {
        if (!constructed) {
            return new DERTaggedObject(false, tag, new DEROctetString(this._in.toByteArray()));
        }
        ASN1EncodableVector v = readVector();
        ASN1Primitive bERTaggedObject;
        if (this._in instanceof IndefiniteLengthInputStream) {
            if (v.size() == 1) {
                bERTaggedObject = new BERTaggedObject(true, tag, v.get(0));
            } else {
                bERTaggedObject = new BERTaggedObject(false, tag, BERFactory.createSequence(v));
            }
            return bERTaggedObject;
        }
        if (v.size() == 1) {
            bERTaggedObject = new DERTaggedObject(true, tag, v.get(0));
        } else {
            bERTaggedObject = new DERTaggedObject(false, tag, DERFactory.createSequence(v));
        }
        return bERTaggedObject;
    }

    public ASN1Encodable readObject() throws IOException {
        int tag = this._in.read();
        if (tag == -1) {
            return null;
        }
        boolean isConstructed = false;
        set00Check(false);
        int tagNo = ASN1InputStream.readTagNumber(this._in, tag);
        if ((tag & 32) != 0) {
            isConstructed = true;
        }
        int length = ASN1InputStream.readLength(this._in, this._limit);
        if (length >= 0) {
            InputStream defIn = new DefiniteLengthInputStream(this._in, length);
            if ((tag & 64) != 0) {
                return new DERApplicationSpecific(isConstructed, tagNo, defIn.toByteArray());
            }
            if ((tag & 128) != 0) {
                return new BERTaggedObjectParser(isConstructed, tagNo, new ASN1StreamParser(defIn));
            }
            if (isConstructed) {
                if (tagNo == 4) {
                    return new BEROctetStringParser(new ASN1StreamParser(defIn));
                }
                if (tagNo == 8) {
                    return new DERExternalParser(new ASN1StreamParser(defIn));
                }
                switch (tagNo) {
                    case 16:
                        return new DERSequenceParser(new ASN1StreamParser(defIn));
                    case 17:
                        return new DERSetParser(new ASN1StreamParser(defIn));
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("unknown tag ");
                        stringBuilder.append(tagNo);
                        stringBuilder.append(" encountered");
                        throw new IOException(stringBuilder.toString());
                }
            } else if (tagNo == 4) {
                return new DEROctetStringParser(defIn);
            } else {
                try {
                    return ASN1InputStream.createPrimitiveDERObject(tagNo, defIn, this.tmpBuffers);
                } catch (IllegalArgumentException e) {
                    throw new ASN1Exception("corrupted stream detected", e);
                }
            }
        } else if (isConstructed) {
            ASN1StreamParser sp = new ASN1StreamParser(new IndefiniteLengthInputStream(this._in, this._limit), this._limit);
            if ((tag & 64) != 0) {
                return new BERApplicationSpecificParser(tagNo, sp);
            }
            if ((tag & 128) != 0) {
                return new BERTaggedObjectParser(true, tagNo, sp);
            }
            return sp.readIndef(tagNo);
        } else {
            throw new IOException("indefinite-length primitive encoding encountered");
        }
    }

    private void set00Check(boolean enabled) {
        if (this._in instanceof IndefiniteLengthInputStream) {
            ((IndefiniteLengthInputStream) this._in).setEofOn00(enabled);
        }
    }

    ASN1EncodableVector readVector() throws IOException {
        ASN1EncodableVector v = new ASN1EncodableVector();
        while (true) {
            ASN1Encodable readObject = readObject();
            ASN1Encodable obj = readObject;
            if (readObject == null) {
                return v;
            }
            if (obj instanceof InMemoryRepresentable) {
                v.add(((InMemoryRepresentable) obj).getLoadedObject());
            } else {
                v.add(obj.toASN1Primitive());
            }
        }
    }
}
