package org.apache.commons.codec.binary;

import org.apache.commons.codec.BinaryDecoder;
import org.apache.commons.codec.BinaryEncoder;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.http.protocol.HTTP;

@Deprecated
public class Base64 implements BinaryEncoder, BinaryDecoder {
    static final int BASELENGTH = 255;
    static final byte[] CHUNK_SEPARATOR = "\r\n".getBytes();
    static final int CHUNK_SIZE = 76;
    static final int EIGHTBIT = 8;
    static final int FOURBYTE = 4;
    static final int LOOKUPLENGTH = 64;
    static final byte PAD = (byte) 61;
    static final int SIGN = -128;
    static final int SIXTEENBIT = 16;
    static final int TWENTYFOURBITGROUP = 24;
    private static byte[] base64Alphabet = new byte[BASELENGTH];
    private static byte[] lookUpBase64Alphabet = new byte[LOOKUPLENGTH];

    static {
        int i;
        int i2;
        int j = 0;
        for (i = 0; i < BASELENGTH; i++) {
            base64Alphabet[i] = (byte) -1;
        }
        for (i2 = 90; i2 >= 65; i2--) {
            base64Alphabet[i2] = (byte) (i2 - 65);
        }
        for (i2 = 122; i2 >= 97; i2--) {
            base64Alphabet[i2] = (byte) ((i2 - 97) + 26);
        }
        for (i2 = 57; i2 >= 48; i2--) {
            base64Alphabet[i2] = (byte) ((i2 - 48) + 52);
        }
        base64Alphabet[43] = (byte) 62;
        base64Alphabet[47] = (byte) 63;
        for (i2 = 0; i2 <= 25; i2++) {
            lookUpBase64Alphabet[i2] = (byte) (65 + i2);
        }
        i = 26;
        i2 = 0;
        while (i <= 51) {
            lookUpBase64Alphabet[i] = (byte) (97 + i2);
            i++;
            i2++;
        }
        i2 = 52;
        while (i2 <= 61) {
            lookUpBase64Alphabet[i2] = (byte) (48 + j);
            i2++;
            j++;
        }
        lookUpBase64Alphabet[62] = (byte) 43;
        lookUpBase64Alphabet[63] = (byte) 47;
    }

    private static boolean isBase64(byte octect) {
        if (octect != PAD && base64Alphabet[octect] == (byte) -1) {
            return false;
        }
        return true;
    }

    public static boolean isArrayByteBase64(byte[] arrayOctect) {
        if (length == 0) {
            return true;
        }
        for (byte isBase64 : discardWhitespace(arrayOctect)) {
            if (!isBase64(isBase64)) {
                return false;
            }
        }
        return true;
    }

    public static byte[] encodeBase64(byte[] binaryData) {
        return encodeBase64(binaryData, false);
    }

    public static byte[] encodeBase64Chunked(byte[] binaryData) {
        return encodeBase64(binaryData, true);
    }

    public Object decode(Object pObject) throws DecoderException {
        if (pObject instanceof byte[]) {
            return decode((byte[]) pObject);
        }
        throw new DecoderException("Parameter supplied to Base64 decode is not a byte[]");
    }

    public byte[] decode(byte[] pArray) {
        return decodeBase64(pArray);
    }

    public static byte[] encodeBase64(byte[] binaryData, boolean isChunked) {
        int encodedDataLength;
        byte b1;
        byte b2;
        byte l;
        int numberTriplets;
        byte val3;
        byte[] bArr = binaryData;
        int lengthDataBits = bArr.length * EIGHTBIT;
        int fewerThan24bits = lengthDataBits % TWENTYFOURBITGROUP;
        int numberTriplets2 = lengthDataBits / TWENTYFOURBITGROUP;
        int nbrChunks = 0;
        if (fewerThan24bits != 0) {
            encodedDataLength = (numberTriplets2 + 1) * 4;
        } else {
            encodedDataLength = numberTriplets2 * 4;
        }
        if (isChunked) {
            nbrChunks = CHUNK_SEPARATOR.length == 0 ? 0 : (int) Math.ceil((double) (((float) encodedDataLength) / 76.0f));
            encodedDataLength += CHUNK_SEPARATOR.length * nbrChunks;
        }
        byte[] encodedData = new byte[encodedDataLength];
        int nextSeparatorIndex = CHUNK_SIZE;
        int chunksSoFar = 0;
        byte k = (byte) 0;
        int i = 0;
        int encodedIndex = 0;
        while (i < numberTriplets2) {
            int lengthDataBits2;
            int dataIndex = i * 3;
            b1 = bArr[dataIndex];
            b2 = bArr[dataIndex + 1];
            byte b3 = bArr[dataIndex + 2];
            l = (byte) (b2 & 15);
            k = (byte) (b1 & 3);
            byte val1 = (byte) ((b1 & SIGN) == 0 ? b1 >> 2 : (b1 >> 2) ^ 192);
            if ((b2 & SIGN) == 0) {
                lengthDataBits2 = lengthDataBits;
                lengthDataBits = b2 >> 4;
            } else {
                lengthDataBits2 = lengthDataBits;
                lengthDataBits = (b2 >> 4) ^ 240;
            }
            byte lengthDataBits3 = (byte) lengthDataBits;
            if ((b3 & SIGN) == 0) {
                numberTriplets = numberTriplets2;
                numberTriplets2 = b3 >> 6;
            } else {
                numberTriplets = numberTriplets2;
                numberTriplets2 = (b3 >> 6) ^ 252;
            }
            val3 = (byte) numberTriplets2;
            encodedData[encodedIndex] = lookUpBase64Alphabet[val1];
            encodedData[encodedIndex + 1] = lookUpBase64Alphabet[lengthDataBits3 | (k << 4)];
            encodedData[encodedIndex + 2] = lookUpBase64Alphabet[(l << 2) | val3];
            encodedData[encodedIndex + 3] = lookUpBase64Alphabet[b3 & 63];
            byte val2 = lengthDataBits3;
            lengthDataBits = encodedIndex + 4;
            if (isChunked && lengthDataBits == nextSeparatorIndex) {
                System.arraycopy(CHUNK_SEPARATOR, (byte) 0, encodedData, lengthDataBits, CHUNK_SEPARATOR.length);
                chunksSoFar++;
                nextSeparatorIndex = (CHUNK_SIZE * (chunksSoFar + 1)) + (CHUNK_SEPARATOR.length * chunksSoFar);
                encodedIndex = lengthDataBits + CHUNK_SEPARATOR.length;
            } else {
                encodedIndex = lengthDataBits;
                chunksSoFar = chunksSoFar;
                nextSeparatorIndex = nextSeparatorIndex;
            }
            i++;
            lengthDataBits = lengthDataBits2;
            numberTriplets2 = numberTriplets;
        }
        int i2 = nextSeparatorIndex;
        numberTriplets = numberTriplets2;
        nextSeparatorIndex = chunksSoFar;
        lengthDataBits = i * 3;
        if (fewerThan24bits == EIGHTBIT) {
            b1 = bArr[lengthDataBits];
            k = (byte) (b1 & 3);
            encodedData[encodedIndex] = lookUpBase64Alphabet[(byte) ((b1 & SIGN) == 0 ? b1 >> 2 : (b1 >> 2) ^ 192)];
            encodedData[encodedIndex + 1] = lookUpBase64Alphabet[k << 4];
            encodedData[encodedIndex + 2] = PAD;
            encodedData[encodedIndex + 3] = PAD;
        } else if (fewerThan24bits == 16) {
            b1 = bArr[lengthDataBits];
            b2 = bArr[lengthDataBits + 1];
            l = (byte) (b2 & 15);
            k = (byte) (b1 & 3);
            val3 = (byte) ((b2 & SIGN) == 0 ? b2 >> 4 : (b2 >> 4) ^ 240);
            encodedData[encodedIndex] = lookUpBase64Alphabet[(byte) ((b1 & SIGN) == 0 ? b1 >> 2 : (b1 >> 2) ^ 192)];
            encodedData[encodedIndex + 1] = lookUpBase64Alphabet[val3 | (k << 4)];
            encodedData[encodedIndex + 2] = lookUpBase64Alphabet[l << 2];
            encodedData[encodedIndex + 3] = PAD;
        }
        if (isChunked && nextSeparatorIndex < nbrChunks) {
            System.arraycopy(CHUNK_SEPARATOR, 0, encodedData, encodedDataLength - CHUNK_SEPARATOR.length, CHUNK_SEPARATOR.length);
        }
        return encodedData;
    }

    public static byte[] decodeBase64(byte[] base64Data) {
        base64Data = discardNonBase64(base64Data);
        int i = 0;
        if (base64Data.length == 0) {
            return new byte[0];
        }
        int numberQuadruple = base64Data.length / 4;
        int encodedIndex = 0;
        int lastData = base64Data.length;
        while (base64Data[lastData - 1] == PAD) {
            lastData--;
            if (lastData == 0) {
                return new byte[0];
            }
        }
        byte[] decodedData = new byte[(lastData - numberQuadruple)];
        while (i < numberQuadruple) {
            int dataIndex = i * 4;
            byte marker0 = base64Data[dataIndex + 2];
            byte marker1 = base64Data[dataIndex + 3];
            byte b1 = base64Alphabet[base64Data[dataIndex]];
            byte b2 = base64Alphabet[base64Data[dataIndex + 1]];
            byte b3;
            if (marker0 != PAD && marker1 != PAD) {
                b3 = base64Alphabet[marker0];
                byte b4 = base64Alphabet[marker1];
                decodedData[encodedIndex] = (byte) ((b1 << 2) | (b2 >> 4));
                decodedData[encodedIndex + 1] = (byte) (((b2 & 15) << 4) | ((b3 >> 2) & 15));
                decodedData[encodedIndex + 2] = (byte) ((b3 << 6) | b4);
            } else if (marker0 == PAD) {
                decodedData[encodedIndex] = (byte) ((b1 << 2) | (b2 >> 4));
            } else if (marker1 == PAD) {
                b3 = base64Alphabet[marker0];
                decodedData[encodedIndex] = (byte) ((b1 << 2) | (b2 >> 4));
                decodedData[encodedIndex + 1] = (byte) (((b2 & 15) << 4) | ((b3 >> 2) & 15));
            }
            encodedIndex += 3;
            i++;
        }
        return decodedData;
    }

    static byte[] discardWhitespace(byte[] data) {
        byte[] groomedData = new byte[data.length];
        int bytesCopied = 0;
        for (int i = 0; i < data.length; i++) {
            byte b = data[i];
            if (!(b == (byte) 13 || b == (byte) 32)) {
                switch (b) {
                    case HTTP.HT /*9*/:
                    case HTTP.LF /*10*/:
                        break;
                    default:
                        int bytesCopied2 = bytesCopied + 1;
                        groomedData[bytesCopied] = data[i];
                        bytesCopied = bytesCopied2;
                        break;
                }
            }
        }
        byte[] packedData = new byte[bytesCopied];
        System.arraycopy(groomedData, 0, packedData, 0, bytesCopied);
        return packedData;
    }

    static byte[] discardNonBase64(byte[] data) {
        byte[] groomedData = new byte[data.length];
        int bytesCopied = 0;
        for (int i = 0; i < data.length; i++) {
            if (isBase64(data[i])) {
                int bytesCopied2 = bytesCopied + 1;
                groomedData[bytesCopied] = data[i];
                bytesCopied = bytesCopied2;
            }
        }
        byte[] packedData = new byte[bytesCopied];
        System.arraycopy(groomedData, 0, packedData, 0, bytesCopied);
        return packedData;
    }

    public Object encode(Object pObject) throws EncoderException {
        if (pObject instanceof byte[]) {
            return encode((byte[]) pObject);
        }
        throw new EncoderException("Parameter supplied to Base64 encode is not a byte[]");
    }

    public byte[] encode(byte[] pArray) {
        return encodeBase64(pArray, false);
    }
}
