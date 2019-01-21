package org.bouncycastle.asn1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bouncycastle.asn1.eac.CertificateBody;
import org.bouncycastle.util.Arrays;

public class ASN1ObjectIdentifier extends ASN1Primitive {
    private static final long LONG_LIMIT = 72057594037927808L;
    private static final ConcurrentMap<OidHandle, ASN1ObjectIdentifier> pool = new ConcurrentHashMap();
    private byte[] body;
    private final String identifier;

    private static class OidHandle {
        private final byte[] enc;
        private final int key;

        OidHandle(byte[] bArr) {
            this.key = Arrays.hashCode(bArr);
            this.enc = bArr;
        }

        public boolean equals(Object obj) {
            return obj instanceof OidHandle ? Arrays.areEqual(this.enc, ((OidHandle) obj).enc) : false;
        }

        public int hashCode() {
            return this.key;
        }
    }

    public ASN1ObjectIdentifier(String str) {
        if (str == null) {
            throw new IllegalArgumentException("'identifier' cannot be null");
        } else if (isValidIdentifier(str)) {
            this.identifier = str;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("string ");
            stringBuilder.append(str);
            stringBuilder.append(" not an OID");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    ASN1ObjectIdentifier(ASN1ObjectIdentifier aSN1ObjectIdentifier, String str) {
        StringBuilder stringBuilder;
        if (isValidBranchID(str, 0)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(aSN1ObjectIdentifier.getId());
            stringBuilder.append(".");
            stringBuilder.append(str);
            this.identifier = stringBuilder.toString();
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("string ");
        stringBuilder.append(str);
        stringBuilder.append(" not a valid OID branch");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    ASN1ObjectIdentifier(byte[] bArr) {
        byte[] bArr2 = bArr;
        StringBuffer stringBuffer = new StringBuffer();
        Object obj = 1;
        long j = 0;
        BigInteger bigInteger = null;
        for (int i = 0; i != bArr2.length; i++) {
            int i2 = bArr2[i] & 255;
            char c;
            if (j <= LONG_LIMIT) {
                j += (long) (i2 & CertificateBody.profileType);
                if ((i2 & 128) == 0) {
                    if (obj != null) {
                        if (j < 40) {
                            stringBuffer.append('0');
                        } else if (j < 80) {
                            stringBuffer.append('1');
                            j -= 40;
                        } else {
                            stringBuffer.append('2');
                            j -= 80;
                        }
                        c = '.';
                        obj = null;
                    } else {
                        c = '.';
                    }
                    stringBuffer.append(c);
                    stringBuffer.append(j);
                    j = 0;
                } else {
                    j <<= 7;
                }
            } else {
                if (bigInteger == null) {
                    bigInteger = BigInteger.valueOf(j);
                }
                Object or = bigInteger.or(BigInteger.valueOf((long) (i2 & CertificateBody.profileType)));
                if ((i2 & 128) == 0) {
                    if (obj != null) {
                        stringBuffer.append('2');
                        or = or.subtract(BigInteger.valueOf(80));
                        c = '.';
                        obj = null;
                    } else {
                        c = '.';
                    }
                    stringBuffer.append(c);
                    stringBuffer.append(or);
                    j = 0;
                    bigInteger = null;
                } else {
                    bigInteger = or.shiftLeft(7);
                }
            }
        }
        this.identifier = stringBuffer.toString();
        this.body = Arrays.clone(bArr);
    }

    private void doOutput(ByteArrayOutputStream byteArrayOutputStream) {
        OIDTokenizer oIDTokenizer = new OIDTokenizer(this.identifier);
        int parseInt = Integer.parseInt(oIDTokenizer.nextToken()) * 40;
        String nextToken = oIDTokenizer.nextToken();
        if (nextToken.length() <= 18) {
            writeField(byteArrayOutputStream, ((long) parseInt) + Long.parseLong(nextToken));
        } else {
            writeField(byteArrayOutputStream, new BigInteger(nextToken).add(BigInteger.valueOf((long) parseInt)));
        }
        while (oIDTokenizer.hasMoreTokens()) {
            String nextToken2 = oIDTokenizer.nextToken();
            if (nextToken2.length() <= 18) {
                writeField(byteArrayOutputStream, Long.parseLong(nextToken2));
            } else {
                writeField(byteArrayOutputStream, new BigInteger(nextToken2));
            }
        }
    }

    static ASN1ObjectIdentifier fromOctetString(byte[] bArr) {
        ASN1ObjectIdentifier aSN1ObjectIdentifier = (ASN1ObjectIdentifier) pool.get(new OidHandle(bArr));
        return aSN1ObjectIdentifier == null ? new ASN1ObjectIdentifier(bArr) : aSN1ObjectIdentifier;
    }

    private synchronized byte[] getBody() {
        if (this.body == null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            doOutput(byteArrayOutputStream);
            this.body = byteArrayOutputStream.toByteArray();
        }
        return this.body;
    }

    public static ASN1ObjectIdentifier getInstance(Object obj) {
        StringBuilder stringBuilder;
        if (obj == null || (obj instanceof ASN1ObjectIdentifier)) {
            return (ASN1ObjectIdentifier) obj;
        }
        if (obj instanceof ASN1Encodable) {
            ASN1Encodable aSN1Encodable = (ASN1Encodable) obj;
            if (aSN1Encodable.toASN1Primitive() instanceof ASN1ObjectIdentifier) {
                return (ASN1ObjectIdentifier) aSN1Encodable.toASN1Primitive();
            }
        }
        if (obj instanceof byte[]) {
            try {
                return (ASN1ObjectIdentifier) ASN1Primitive.fromByteArray((byte[]) obj);
            } catch (IOException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("failed to construct object identifier from byte[]: ");
                stringBuilder.append(e.getMessage());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("illegal object in getInstance: ");
        stringBuilder.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static ASN1ObjectIdentifier getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        ASN1Primitive object = aSN1TaggedObject.getObject();
        return (z || (object instanceof ASN1ObjectIdentifier)) ? getInstance(object) : fromOctetString(ASN1OctetString.getInstance(object).getOctets());
    }

    private static boolean isValidBranchID(String str, int i) {
        int length = str.length();
        boolean z;
        do {
            char charAt;
            z = false;
            while (true) {
                length--;
                if (length < i) {
                    return z;
                }
                charAt = str.charAt(length);
                if ('0' <= charAt && charAt <= '9') {
                    z = true;
                }
            }
            if (charAt != '.') {
                break;
            }
        } while (z);
        return false;
    }

    /* JADX WARNING: Missing block: B:12:0x0025, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean isValidIdentifier(String str) {
        if (str.length() < 3 || str.charAt(1) != '.') {
            return false;
        }
        char charAt = str.charAt(0);
        return (charAt < '0' || charAt > '2') ? false : isValidBranchID(str, 2);
    }

    private void writeField(ByteArrayOutputStream byteArrayOutputStream, long j) {
        byte[] bArr = new byte[9];
        int i = 8;
        bArr[8] = (byte) (((int) j) & CertificateBody.profileType);
        while (j >= 128) {
            j >>= 7;
            i--;
            bArr[i] = (byte) ((((int) j) & CertificateBody.profileType) | 128);
        }
        byteArrayOutputStream.write(bArr, i, 9 - i);
    }

    private void writeField(ByteArrayOutputStream byteArrayOutputStream, BigInteger bigInteger) {
        int bitLength = (bigInteger.bitLength() + 6) / 7;
        if (bitLength == 0) {
            byteArrayOutputStream.write(0);
            return;
        }
        byte[] bArr = new byte[bitLength];
        bitLength--;
        BigInteger bigInteger2 = bigInteger;
        for (int i = bitLength; i >= 0; i--) {
            bArr[i] = (byte) ((bigInteger2.intValue() & CertificateBody.profileType) | 128);
            bigInteger2 = bigInteger2.shiftRight(7);
        }
        bArr[bitLength] = (byte) (bArr[bitLength] & CertificateBody.profileType);
        byteArrayOutputStream.write(bArr, 0, bArr.length);
    }

    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        return aSN1Primitive == this ? true : !(aSN1Primitive instanceof ASN1ObjectIdentifier) ? false : this.identifier.equals(((ASN1ObjectIdentifier) aSN1Primitive).identifier);
    }

    public ASN1ObjectIdentifier branch(String str) {
        return new ASN1ObjectIdentifier(this, str);
    }

    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        byte[] body = getBody();
        aSN1OutputStream.write(6);
        aSN1OutputStream.writeLength(body.length);
        aSN1OutputStream.write(body);
    }

    int encodedLength() throws IOException {
        int length = getBody().length;
        return (1 + StreamUtil.calculateBodyLength(length)) + length;
    }

    public String getId() {
        return this.identifier;
    }

    public int hashCode() {
        return this.identifier.hashCode();
    }

    public ASN1ObjectIdentifier intern() {
        ASN1ObjectIdentifier aSN1ObjectIdentifier;
        OidHandle oidHandle = new OidHandle(getBody());
        ASN1ObjectIdentifier aSN1ObjectIdentifier2 = (ASN1ObjectIdentifier) pool.get(oidHandle);
        if (aSN1ObjectIdentifier2 == null) {
            aSN1ObjectIdentifier = (ASN1ObjectIdentifier) pool.putIfAbsent(oidHandle, this);
            if (aSN1ObjectIdentifier == null) {
                return this;
            }
        }
        aSN1ObjectIdentifier = aSN1ObjectIdentifier2;
        return aSN1ObjectIdentifier;
    }

    boolean isConstructed() {
        return false;
    }

    public boolean on(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        String id = getId();
        String id2 = aSN1ObjectIdentifier.getId();
        return id.length() > id2.length() && id.charAt(id2.length()) == '.' && id.startsWith(id2);
    }

    public String toString() {
        return getId();
    }
}
