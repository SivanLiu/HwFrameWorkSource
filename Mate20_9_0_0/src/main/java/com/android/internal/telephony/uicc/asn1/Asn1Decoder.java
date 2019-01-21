package com.android.internal.telephony.uicc.asn1;

import com.android.internal.telephony.uicc.IccUtils;

public final class Asn1Decoder {
    private final int mEnd;
    private int mPosition;
    private final byte[] mSrc;

    public Asn1Decoder(String hex) {
        this(IccUtils.hexStringToBytes(hex));
    }

    public Asn1Decoder(byte[] src) {
        this(src, 0, src.length);
    }

    public Asn1Decoder(byte[] bytes, int offset, int length) {
        if (offset < 0 || length < 0 || offset + length > bytes.length) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Out of the bounds: bytes=[");
            stringBuilder.append(bytes.length);
            stringBuilder.append("], offset=");
            stringBuilder.append(offset);
            stringBuilder.append(", length=");
            stringBuilder.append(length);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        }
        this.mSrc = bytes;
        this.mPosition = offset;
        this.mEnd = offset + length;
    }

    public int getPosition() {
        return this.mPosition;
    }

    public boolean hasNextNode() {
        return this.mPosition < this.mEnd;
    }

    public Asn1Node nextNode() throws InvalidAsn1DataException {
        if (this.mPosition < this.mEnd) {
            int offset;
            byte b = this.mPosition;
            byte tagStart = b;
            int offset2 = b + 1;
            if ((this.mSrc[b] & 31) == 31) {
                while (offset2 < this.mEnd) {
                    offset = offset2 + 1;
                    if ((this.mSrc[offset2] & 128) == 0) {
                        offset2 = offset;
                        break;
                    }
                    offset2 = offset;
                }
            }
            if (offset2 < this.mEnd) {
                StringBuilder stringBuilder;
                try {
                    int tag = IccUtils.bytesToInt(this.mSrc, tagStart, offset2 - tagStart);
                    int offset3 = offset2 + 1;
                    b = this.mSrc[offset2];
                    if ((b & 128) == 0) {
                        offset2 = b;
                    } else {
                        offset2 = b & 127;
                        if (offset3 + offset2 <= this.mEnd) {
                            try {
                                offset = IccUtils.bytesToInt(this.mSrc, offset3, offset2);
                                offset3 += offset2;
                                offset2 = offset;
                            } catch (IllegalArgumentException e) {
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Cannot parse length at position: ");
                                stringBuilder2.append(offset3);
                                throw new InvalidAsn1DataException(tag, stringBuilder2.toString(), e);
                            }
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Cannot parse length at position: ");
                        stringBuilder.append(offset3);
                        throw new InvalidAsn1DataException(tag, stringBuilder.toString());
                    }
                    if (offset3 + offset2 <= this.mEnd) {
                        Asn1Node root = new Asn1Node(tag, this.mSrc, offset3, offset2);
                        this.mPosition = offset3 + offset2;
                        return root;
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Incomplete data at position: ");
                    stringBuilder.append(offset3);
                    stringBuilder.append(", expected bytes: ");
                    stringBuilder.append(offset2);
                    stringBuilder.append(", actual bytes: ");
                    stringBuilder.append(this.mEnd - offset3);
                    throw new InvalidAsn1DataException(tag, stringBuilder.toString());
                } catch (IllegalArgumentException e2) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Cannot parse tag at position: ");
                    stringBuilder.append(tagStart);
                    throw new InvalidAsn1DataException(0, stringBuilder.toString(), e2);
                }
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Invalid length at position: ");
            stringBuilder3.append(offset2);
            throw new InvalidAsn1DataException(0, stringBuilder3.toString());
        }
        throw new IllegalStateException("No bytes to parse.");
    }
}
