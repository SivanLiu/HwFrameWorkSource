package javax.obex;

import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public final class ObexHelper {
    public static final int BASE_PACKET_LENGTH = 3;
    public static final int LOWER_LIMIT_MAX_PACKET_SIZE = 255;
    public static final int MAX_CLIENT_PACKET_SIZE = 64512;
    public static final int MAX_PACKET_SIZE_INT = 65534;
    public static final int OBEX_AUTH_REALM_CHARSET_ASCII = 0;
    public static final int OBEX_AUTH_REALM_CHARSET_ISO_8859_1 = 1;
    public static final int OBEX_AUTH_REALM_CHARSET_ISO_8859_2 = 2;
    public static final int OBEX_AUTH_REALM_CHARSET_ISO_8859_3 = 3;
    public static final int OBEX_AUTH_REALM_CHARSET_ISO_8859_4 = 4;
    public static final int OBEX_AUTH_REALM_CHARSET_ISO_8859_5 = 5;
    public static final int OBEX_AUTH_REALM_CHARSET_ISO_8859_6 = 6;
    public static final int OBEX_AUTH_REALM_CHARSET_ISO_8859_7 = 7;
    public static final int OBEX_AUTH_REALM_CHARSET_ISO_8859_8 = 8;
    public static final int OBEX_AUTH_REALM_CHARSET_ISO_8859_9 = 9;
    public static final int OBEX_AUTH_REALM_CHARSET_UNICODE = 255;
    public static final int OBEX_BYTE_SEQ_HEADER_LEN = 3;
    public static final int OBEX_OPCODE_ABORT = 255;
    public static final int OBEX_OPCODE_CONNECT = 128;
    public static final int OBEX_OPCODE_DISCONNECT = 129;
    public static final int OBEX_OPCODE_FINAL_BIT_MASK = 128;
    public static final int OBEX_OPCODE_GET = 3;
    public static final int OBEX_OPCODE_GET_FINAL = 131;
    public static final int OBEX_OPCODE_PUT = 2;
    public static final int OBEX_OPCODE_PUT_FINAL = 130;
    public static final int OBEX_OPCODE_RESERVED = 4;
    public static final int OBEX_OPCODE_RESERVED_FINAL = 132;
    public static final int OBEX_OPCODE_SETPATH = 133;
    public static final byte OBEX_SRMP_WAIT = (byte) 1;
    public static final byte OBEX_SRM_DISABLE = (byte) 0;
    public static final byte OBEX_SRM_ENABLE = (byte) 1;
    public static final byte OBEX_SRM_SUPPORT = (byte) 2;
    private static final String TAG = "ObexHelper";
    public static final boolean VDBG = false;

    private ObexHelper() {
    }

    public static byte[] updateHeaderSet(HeaderSet header, byte[] headerArray) throws IOException {
        byte[] bArr = headerArray;
        byte[] body = null;
        int length = 0;
        int index = 0;
        HeaderSet headerImpl = header;
        while (true) {
            HeaderSet headerImpl2 = headerImpl;
            try {
                if (index >= bArr.length) {
                    return body;
                }
                int headerID = 255 & bArr[index];
                int i = headerID & 192;
                byte[] value;
                if (i == 0 || i == 64) {
                    boolean trimTail = true;
                    index++;
                    length = ((bArr[index] & 255) << 8) + (255 & bArr[index + 1]);
                    index += 2;
                    if (length <= 3) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Remote sent an OBEX packet with incorrect header length = ");
                        stringBuilder.append(length);
                        Log.e(str, stringBuilder.toString());
                    } else {
                        length -= 3;
                        value = new byte[length];
                        System.arraycopy(bArr, index, value, 0, length);
                        if (length == 0 || (length > 0 && value[length - 1] != (byte) 0)) {
                            trimTail = false;
                        }
                        boolean trimTail2 = trimTail;
                        switch (headerID) {
                            case HeaderSet.TYPE /*66*/:
                                if (!trimTail2) {
                                    headerImpl2.setHeader(headerID, new String(value, 0, value.length, "ISO8859_1"));
                                    break;
                                }
                                headerImpl2.setHeader(headerID, new String(value, 0, value.length - 1, "ISO8859_1"));
                                break;
                            case HeaderSet.TIME_ISO_8601 /*68*/:
                                String dateString = new String(value, "ISO8859_1");
                                Calendar temp = Calendar.getInstance();
                                if (dateString.length() == 16 && dateString.charAt(15) == 'Z') {
                                    temp.setTimeZone(TimeZone.getTimeZone("UTC"));
                                }
                                temp.set(1, Integer.parseInt(dateString.substring(0, 4)));
                                temp.set(2, Integer.parseInt(dateString.substring(4, 6)));
                                temp.set(5, Integer.parseInt(dateString.substring(6, 8)));
                                temp.set(11, Integer.parseInt(dateString.substring(9, 11)));
                                temp.set(12, Integer.parseInt(dateString.substring(11, 13)));
                                temp.set(13, Integer.parseInt(dateString.substring(13, 15)));
                                headerImpl2.setHeader(68, temp);
                                break;
                            case HeaderSet.BODY /*72*/:
                            case HeaderSet.END_OF_BODY /*73*/:
                                body = new byte[(length + 1)];
                                body[0] = (byte) headerID;
                                System.arraycopy(bArr, index, body, 1, length);
                                break;
                            case HeaderSet.AUTH_CHALLENGE /*77*/:
                                headerImpl2.mAuthChall = new byte[length];
                                System.arraycopy(bArr, index, headerImpl2.mAuthChall, 0, length);
                                break;
                            case HeaderSet.AUTH_RESPONSE /*78*/:
                                headerImpl2.mAuthResp = new byte[length];
                                System.arraycopy(bArr, index, headerImpl2.mAuthResp, 0, length);
                                break;
                            default:
                                if ((headerID & 192) != 0) {
                                    headerImpl2.setHeader(headerID, value);
                                    break;
                                }
                                headerImpl2.setHeader(headerID, convertToUnicode(value, true));
                                break;
                        }
                        index += length;
                    }
                } else if (i == 128) {
                    index++;
                    try {
                        headerImpl2.setHeader(headerID, Byte.valueOf(bArr[index]));
                    } catch (Exception e) {
                    }
                    index++;
                } else if (i == 192) {
                    index++;
                    value = new byte[4];
                    System.arraycopy(bArr, index, value, 0, 4);
                    if (headerID == 196) {
                        Calendar temp2 = Calendar.getInstance();
                        temp2.setTime(new Date(convertToLong(value) * 1000));
                        headerImpl2.setHeader(196, temp2);
                    } else if (headerID == 203) {
                        headerImpl2.mConnectionID = new byte[4];
                        System.arraycopy(value, 0, headerImpl2.mConnectionID, 0, 4);
                    } else {
                        headerImpl2.setHeader(headerID, Long.valueOf(convertToLong(value)));
                    }
                    index += 4;
                }
                headerImpl = headerImpl2;
            } catch (UnsupportedEncodingException e2) {
                throw e2;
            } catch (UnsupportedEncodingException e22) {
                throw e22;
            } catch (Exception e3) {
                throw new IOException("Header was not formatted properly", e3);
            } catch (IOException e4) {
                throw new IOException("Header was not formatted properly", e4);
            }
        }
    }

    public static byte[] createHeader(HeaderSet head, boolean nullOut) {
        byte[] result;
        Long intHeader = null;
        String stringHeader = null;
        Calendar dateHeader = null;
        StringBuffer buffer = null;
        byte[] value = null;
        byte[] lengthArray = new byte[2];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HeaderSet headImpl = head;
        try {
            int length;
            int length2;
            int temp;
            Byte byteHeader;
            if (headImpl.mConnectionID != null && headImpl.getHeader(70) == null) {
                out.write(-53);
                out.write(headImpl.mConnectionID);
            }
            intHeader = (Long) headImpl.getHeader(192);
            if (intHeader != null) {
                out.write(-64);
                value = convertToByteArray(intHeader.longValue());
                out.write(value);
                if (nullOut) {
                    headImpl.setHeader(192, null);
                }
            }
            stringHeader = (String) headImpl.getHeader(1);
            if (stringHeader != null) {
                out.write(1);
                value = convertToUnicodeByteArray(stringHeader);
                length = value.length + 3;
                lengthArray[0] = (byte) (255 & (length >> 8));
                lengthArray[1] = (byte) (255 & length);
                out.write(lengthArray);
                out.write(value);
                if (nullOut) {
                    headImpl.setHeader(1, null);
                }
            } else if (headImpl.getEmptyNameHeader()) {
                out.write(1);
                lengthArray[0] = (byte) 0;
                lengthArray[1] = (byte) 3;
                out.write(lengthArray);
            }
            stringHeader = (String) headImpl.getHeader(66);
            if (stringHeader != null) {
                out.write(66);
                value = stringHeader.getBytes("ISO8859_1");
                length2 = value.length + 4;
                lengthArray[0] = (byte) (255 & (length2 >> 8));
                lengthArray[1] = (byte) (255 & length2);
                out.write(lengthArray);
                out.write(value);
                out.write(0);
                if (nullOut) {
                    headImpl.setHeader(66, null);
                }
            }
            intHeader = (Long) headImpl.getHeader(195);
            if (intHeader != null) {
                out.write(-61);
                value = convertToByteArray(intHeader.longValue());
                out.write(value);
                if (nullOut) {
                    headImpl.setHeader(195, null);
                }
            }
            dateHeader = (Calendar) headImpl.getHeader(68);
            if (dateHeader != null) {
                buffer = new StringBuffer();
                length2 = dateHeader.get(1);
                int temp2 = length2;
                while (length2 < 1000) {
                    buffer.append("0");
                    length2 *= 10;
                }
                buffer.append(temp2);
                temp = dateHeader.get(2);
                if (temp < 10) {
                    buffer.append("0");
                }
                buffer.append(temp);
                temp = dateHeader.get(5);
                if (temp < 10) {
                    buffer.append("0");
                }
                buffer.append(temp);
                buffer.append("T");
                temp = dateHeader.get(11);
                if (temp < 10) {
                    buffer.append("0");
                }
                buffer.append(temp);
                temp = dateHeader.get(12);
                if (temp < 10) {
                    buffer.append("0");
                }
                buffer.append(temp);
                length2 = dateHeader.get(13);
                if (length2 < 10) {
                    buffer.append("0");
                }
                buffer.append(length2);
                if (dateHeader.getTimeZone().getID().equals("UTC")) {
                    buffer.append("Z");
                }
                value = buffer.toString().getBytes("ISO8859_1");
                temp = value.length + 3;
                lengthArray[0] = (byte) ((temp >> 8) & 255);
                lengthArray[1] = (byte) (255 & temp);
                out.write(68);
                out.write(lengthArray);
                out.write(value);
                if (nullOut) {
                    headImpl.setHeader(68, null);
                }
            }
            dateHeader = (Calendar) headImpl.getHeader(196);
            if (dateHeader != null) {
                out.write(196);
                out.write(convertToByteArray(dateHeader.getTime().getTime() / 1000));
                if (nullOut) {
                    headImpl.setHeader(196, null);
                }
            }
            stringHeader = (String) headImpl.getHeader(5);
            if (stringHeader != null) {
                out.write(5);
                value = convertToUnicodeByteArray(stringHeader);
                temp = value.length + 3;
                lengthArray[0] = (byte) ((temp >> 8) & 255);
                lengthArray[1] = (byte) (255 & temp);
                out.write(lengthArray);
                out.write(value);
                if (nullOut) {
                    headImpl.setHeader(5, null);
                }
            }
            value = (byte[]) headImpl.getHeader(70);
            if (value != null) {
                out.write(70);
                temp = value.length + 3;
                lengthArray[0] = (byte) ((temp >> 8) & 255);
                lengthArray[1] = (byte) (255 & temp);
                out.write(lengthArray);
                out.write(value);
                if (nullOut) {
                    headImpl.setHeader(70, null);
                }
            }
            value = (byte[]) headImpl.getHeader(71);
            if (value != null) {
                out.write(71);
                length = value.length + 3;
                lengthArray[0] = (byte) ((length >> 8) & 255);
                lengthArray[1] = (byte) (255 & length);
                out.write(lengthArray);
                out.write(value);
                if (nullOut) {
                    headImpl.setHeader(71, null);
                }
            }
            value = (byte[]) headImpl.getHeader(74);
            if (value != null) {
                out.write(74);
                length = value.length + 3;
                lengthArray[0] = (byte) ((length >> 8) & 255);
                lengthArray[1] = (byte) (255 & length);
                out.write(lengthArray);
                out.write(value);
                if (nullOut) {
                    headImpl.setHeader(74, null);
                }
            }
            value = (byte[]) headImpl.getHeader(76);
            if (value != null) {
                out.write(76);
                length = value.length + 3;
                lengthArray[0] = (byte) ((length >> 8) & 255);
                lengthArray[1] = (byte) (255 & length);
                out.write(lengthArray);
                out.write(value);
                if (nullOut) {
                    headImpl.setHeader(76, null);
                }
            }
            value = (byte[]) headImpl.getHeader(79);
            if (value != null) {
                out.write(79);
                temp = value.length + 3;
                lengthArray[0] = (byte) ((temp >> 8) & 255);
                lengthArray[1] = (byte) (255 & temp);
                out.write(lengthArray);
                out.write(value);
                if (nullOut) {
                    headImpl.setHeader(79, null);
                }
            }
            for (temp = 0; temp < 16; temp++) {
                stringHeader = (String) headImpl.getHeader(temp + 48);
                if (stringHeader != null) {
                    out.write(((byte) temp) + 48);
                    value = convertToUnicodeByteArray(stringHeader);
                    length = value.length + 3;
                    lengthArray[0] = (byte) ((length >> 8) & 255);
                    lengthArray[1] = (byte) (255 & length);
                    out.write(lengthArray);
                    out.write(value);
                    if (nullOut) {
                        headImpl.setHeader(temp + 48, null);
                    }
                }
                value = (byte[]) headImpl.getHeader(temp + 112);
                if (value != null) {
                    out.write(((byte) temp) + 112);
                    length = value.length + 3;
                    lengthArray[0] = (byte) ((length >> 8) & 255);
                    lengthArray[1] = (byte) (255 & length);
                    out.write(lengthArray);
                    out.write(value);
                    if (nullOut) {
                        headImpl.setHeader(temp + 112, null);
                    }
                }
                byteHeader = (Byte) headImpl.getHeader(temp + ResponseCodes.OBEX_HTTP_MULT_CHOICE);
                if (byteHeader != null) {
                    out.write(((byte) temp) + ResponseCodes.OBEX_HTTP_MULT_CHOICE);
                    out.write(byteHeader.byteValue());
                    if (nullOut) {
                        headImpl.setHeader(temp + ResponseCodes.OBEX_HTTP_MULT_CHOICE, null);
                    }
                }
                intHeader = (Long) headImpl.getHeader(temp + 240);
                if (intHeader != null) {
                    out.write(((byte) temp) + 240);
                    out.write(convertToByteArray(intHeader.longValue()));
                    if (nullOut) {
                        headImpl.setHeader(temp + 240, null);
                    }
                }
            }
            if (headImpl.mAuthChall != null) {
                out.write(77);
                temp = headImpl.mAuthChall.length + 3;
                lengthArray[0] = (byte) ((temp >> 8) & 255);
                lengthArray[1] = (byte) (255 & temp);
                out.write(lengthArray);
                out.write(headImpl.mAuthChall);
                if (nullOut) {
                    headImpl.mAuthChall = null;
                }
            }
            if (headImpl.mAuthResp != null) {
                out.write(78);
                temp = headImpl.mAuthResp.length + 3;
                lengthArray[0] = (byte) ((temp >> 8) & 255);
                lengthArray[1] = (byte) (255 & temp);
                out.write(lengthArray);
                out.write(headImpl.mAuthResp);
                if (nullOut) {
                    headImpl.mAuthResp = null;
                }
            }
            byteHeader = (Byte) headImpl.getHeader(HeaderSet.SINGLE_RESPONSE_MODE);
            if (byteHeader != null) {
                out.write(-105);
                out.write(byteHeader.byteValue());
                if (nullOut) {
                    headImpl.setHeader(HeaderSet.SINGLE_RESPONSE_MODE, null);
                }
            }
            byteHeader = (Byte) headImpl.getHeader(HeaderSet.SINGLE_RESPONSE_MODE_PARAMETER);
            if (byteHeader != null) {
                out.write(-104);
                out.write(byteHeader.byteValue());
                if (nullOut) {
                    headImpl.setHeader(HeaderSet.SINGLE_RESPONSE_MODE_PARAMETER, null);
                }
            }
            result = out.toByteArray();
            try {
                out.close();
            } catch (Exception e) {
            }
        } catch (UnsupportedEncodingException e2) {
            throw e2;
        } catch (UnsupportedEncodingException e22) {
            throw e22;
        } catch (IOException e3) {
            result = out.toByteArray();
            out.close();
        } catch (Throwable th) {
            value = buffer;
            Byte buffer2 = null;
            Calendar byteHeader2 = dateHeader;
            String dateHeader2 = stringHeader;
            Long stringHeader2 = intHeader;
            Throwable intHeader2 = th;
            result = out.toByteArray();
            try {
                out.close();
            } catch (Exception e4) {
            }
        }
        return result;
    }

    public static int findHeaderEnd(byte[] headerArray, int start, int maxSize) {
        int fullLength = 0;
        int lastLength = -1;
        int index = start;
        while (fullLength < maxSize && index < headerArray.length) {
            lastLength = fullLength;
            int i = (headerArray[index] < (byte) 0 ? headerArray[index] + 256 : headerArray[index]) & 192;
            if (i == 0 || i == 64) {
                index++;
                if (headerArray[index] < (byte) 0) {
                    i = headerArray[index] + 256;
                } else {
                    i = headerArray[index];
                }
                int length = i << 8;
                index++;
                if (headerArray[index] < (byte) 0) {
                    i = headerArray[index] + 256;
                } else {
                    i = headerArray[index];
                }
                length = (length + i) - 3;
                index = (index + 1) + length;
                fullLength += length + 3;
            } else if (i == 128) {
                index = (index + 1) + 1;
                fullLength += 2;
            } else if (i == 192) {
                index += 5;
                fullLength += 5;
            }
        }
        if (lastLength != 0) {
            return lastLength + start;
        }
        if (fullLength < maxSize) {
            return headerArray.length;
        }
        return -1;
    }

    public static long convertToLong(byte[] b) {
        long result = 0;
        long power = 0;
        for (int i = b.length - 1; i >= 0; i--) {
            long value = (long) b[i];
            if (value < 0) {
                value += 256;
            }
            result |= value << ((int) power);
            power += 8;
        }
        return result;
    }

    public static byte[] convertToByteArray(long l) {
        return new byte[]{(byte) ((int) ((l >> 24) & 255)), (byte) ((int) ((l >> 16) & 255)), (byte) ((int) ((l >> 8) & 255)), (byte) ((int) (255 & l))};
    }

    public static byte[] convertToUnicodeByteArray(String s) {
        if (s == null) {
            return null;
        }
        char[] c = s.toCharArray();
        byte[] result = new byte[((c.length * 2) + 2)];
        for (int i = 0; i < c.length; i++) {
            result[i * 2] = (byte) (c[i] >> 8);
            result[(i * 2) + 1] = (byte) c[i];
        }
        result[result.length - 2] = (byte) 0;
        result[result.length - 1] = (byte) 0;
        return result;
    }

    public static byte[] getTagValue(byte tag, byte[] triplet) {
        int index = findTag(tag, triplet);
        if (index == -1) {
            return null;
        }
        index++;
        int length = triplet[index] & 255;
        byte[] result = new byte[length];
        System.arraycopy(triplet, index + 1, result, 0, length);
        return result;
    }

    public static int findTag(byte tag, byte[] value) {
        if (value == null) {
            return -1;
        }
        int index = 0;
        while (index < value.length && value[index] != tag) {
            index += (value[index + 1] & 255) + 2;
        }
        if (index >= value.length) {
            return -1;
        }
        return index;
    }

    public static String convertToUnicode(byte[] b, boolean includesNull) {
        if (b == null || b.length == 0) {
            return null;
        }
        int arrayLength = b.length;
        if (arrayLength % 2 == 0) {
            arrayLength >>= 1;
            if (includesNull) {
                arrayLength--;
            }
            char[] c = new char[arrayLength];
            for (int i = 0; i < arrayLength; i++) {
                int upper = b[2 * i];
                int lower = b[(2 * i) + 1];
                if (upper < 0) {
                    upper += 256;
                }
                if (lower < 0) {
                    lower += 256;
                }
                if (upper == 0 && lower == 0) {
                    return new String(c, 0, i);
                }
                c[i] = (char) ((upper << 8) | lower);
            }
            return new String(c);
        }
        throw new IllegalArgumentException("Byte array not of a valid form");
    }

    public static byte[] computeMd5Hash(byte[] in) {
        try {
            return MessageDigest.getInstance("MD5").digest(in);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] computeAuthenticationChallenge(byte[] nonce, String realm, boolean access, boolean userID) throws IOException {
        if (nonce.length == 16) {
            byte[] authChall;
            if (realm == null) {
                authChall = new byte[21];
            } else if (realm.length() < 255) {
                authChall = new byte[(realm.length() + 24)];
                authChall[21] = (byte) 2;
                authChall[22] = (byte) (realm.length() + 1);
                authChall[23] = (byte) 1;
                System.arraycopy(realm.getBytes("ISO8859_1"), 0, authChall, 24, realm.length());
            } else {
                throw new IllegalArgumentException("Realm must be less then 255 bytes");
            }
            authChall[0] = (byte) 0;
            authChall[1] = (byte) 16;
            System.arraycopy(nonce, 0, authChall, 2, 16);
            authChall[18] = (byte) 1;
            authChall[19] = (byte) 1;
            authChall[20] = (byte) 0;
            if (!access) {
                authChall[20] = (byte) (authChall[20] | 2);
            }
            if (userID) {
                authChall[20] = (byte) (authChall[20] | 1);
            }
            return authChall;
        }
        throw new IllegalArgumentException("Nonce must be 16 bytes long");
    }

    public static int getMaxTxPacketSize(ObexTransport transport) {
        return validateMaxPacketSize(transport.getMaxTransmitPacketSize());
    }

    public static int getMaxRxPacketSize(ObexTransport transport) {
        return validateMaxPacketSize(transport.getMaxReceivePacketSize());
    }

    private static int validateMaxPacketSize(int size) {
        if (size == -1) {
            return MAX_PACKET_SIZE_INT;
        }
        if (size >= 255) {
            return size;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(size);
        stringBuilder.append(" is less that the lower limit: ");
        stringBuilder.append(255);
        throw new IllegalArgumentException(stringBuilder.toString());
    }
}
