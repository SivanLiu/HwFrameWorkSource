package com.android.internal.telephony.gsm;

import android.content.Context;
import android.content.res.Resources;
import android.telephony.SmsCbLocation;
import android.telephony.SmsCbMessage;
import android.util.Pair;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.telephony.GsmAlphabet;
import java.io.UnsupportedEncodingException;

public class GsmSmsCbMessage {
    private static final char CARRIAGE_RETURN = '\r';
    private static final String[] LANGUAGE_CODES_GROUP_0 = new String[]{"de", "en", "it", "fr", "es", "nl", "sv", "da", "pt", "fi", "no", "el", "tr", "hu", "pl", null};
    private static final String[] LANGUAGE_CODES_GROUP_2 = new String[]{"cs", "he", "ar", "ru", "is", null, null, null, null, null, null, null, null, null, null, null};
    private static final int PDU_BODY_PAGE_LENGTH = 82;

    private GsmSmsCbMessage() {
    }

    private static String getEtwsPrimaryMessage(Context context, int category) {
        Resources r = context.getResources();
        switch (category) {
            case 0:
                return r.getString(17039995);
            case 1:
                return r.getString(17039999);
            case 2:
                return r.getString(17039996);
            case 3:
                return r.getString(17039998);
            case 4:
                return r.getString(17039997);
            default:
                return "";
        }
    }

    public static SmsCbMessage createSmsCbMessage(Context context, SmsCbHeader header, SmsCbLocation location, byte[][] pdus) throws IllegalArgumentException {
        byte[][] bArr = pdus;
        if (header.isEtwsPrimaryNotification()) {
            return new SmsCbMessage(1, header.getGeographicalScope(), header.getSerialNumber(), location, header.getServiceCategory(), null, getEtwsPrimaryMessage(context, header.getEtwsInfo().getWarningType()), 3, header.getEtwsInfo(), header.getCmasInfo());
        }
        Context context2 = context;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        String language = null;
        for (byte[] pdu : bArr) {
            Pair<String, String> p = parseBody(header, pdu);
            language = p.first;
            sb.append((String) p.second);
        }
        SmsCbHeader smsCbHeader = header;
        if (header.isEmergencyMessage()) {
            i = 3;
        }
        return new SmsCbMessage(1, header.getGeographicalScope(), header.getSerialNumber(), location, header.getServiceCategory(), language, sb.toString(), i, header.getEtwsInfo(), header.getCmasInfo());
    }

    /* JADX WARNING: Missing block: B:22:0x004c, code skipped:
            r10 = r1;
            r1 = r2;
     */
    /* JADX WARNING: Missing block: B:24:0x0052, code skipped:
            if (r17.isUmtsFormat() == false) goto L_0x00e1;
     */
    /* JADX WARNING: Missing block: B:25:0x0054, code skipped:
            r6 = r8[6];
            r7 = 83;
     */
    /* JADX WARNING: Missing block: B:26:0x005e, code skipped:
            if (r8.length < ((83 * r6) + 7)) goto L_0x00bc;
     */
    /* JADX WARNING: Missing block: B:27:0x0060, code skipped:
            r12 = new java.lang.StringBuilder();
            r2 = 0;
            r13 = r0;
     */
    /* JADX WARNING: Missing block: B:28:0x0068, code skipped:
            r14 = r2;
     */
    /* JADX WARNING: Missing block: B:29:0x0069, code skipped:
            if (r14 >= r6) goto L_0x00b2;
     */
    /* JADX WARNING: Missing block: B:30:0x006b, code skipped:
            r15 = 7 + (r7 * r14);
            r5 = r8[r15 + 82];
     */
    /* JADX WARNING: Missing block: B:31:0x0075, code skipped:
            if (r5 > 82) goto L_0x0092;
     */
    /* JADX WARNING: Missing block: B:32:0x0077, code skipped:
            r7 = r5;
            r0 = unpackBody(r8, r1, r15, r5, r10, r13);
            r13 = r0.first;
            r12.append((java.lang.String) r0.second);
            r2 = r14 + 1;
            r7 = 83;
     */
    /* JADX WARNING: Missing block: B:33:0x0092, code skipped:
            r7 = r5;
            r3 = new java.lang.StringBuilder();
            r3.append("Page length ");
            r3.append(r7);
            r3.append(" exceeds maximum value ");
            r3.append(82);
     */
    /* JADX WARNING: Missing block: B:34:0x00b1, code skipped:
            throw new java.lang.IllegalArgumentException(r3.toString());
     */
    /* JADX WARNING: Missing block: B:36:0x00bb, code skipped:
            return new android.util.Pair(r13, r12.toString());
     */
    /* JADX WARNING: Missing block: B:37:0x00bc, code skipped:
            r3 = new java.lang.StringBuilder();
            r3.append("Pdu length ");
            r3.append(r8.length);
            r3.append(" does not match ");
            r3.append(r6);
            r3.append(" pages");
     */
    /* JADX WARNING: Missing block: B:38:0x00e0, code skipped:
            throw new java.lang.IllegalArgumentException(r3.toString());
     */
    /* JADX WARNING: Missing block: B:40:0x00ef, code skipped:
            return unpackBody(r8, r1, 6, r8.length - 6, r10, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static Pair<String, String> parseBody(SmsCbHeader header, byte[] pdu) {
        Pair<String, String> pair = pdu;
        String language = null;
        boolean encoding = false;
        int dataCodingScheme = header.getDataCodingScheme();
        int i = (dataCodingScheme & MetricsEvent.FINGERPRINT_ENROLLING) >> 4;
        if (i != 9) {
            boolean encoding2;
            switch (i) {
                case 0:
                    encoding2 = true;
                    language = LANGUAGE_CODES_GROUP_0[dataCodingScheme & 15];
                    break;
                case 1:
                    encoding = true;
                    if ((dataCodingScheme & 15) != 1) {
                        encoding2 = true;
                        break;
                    }
                    encoding2 = true;
                    break;
                case 2:
                    encoding2 = true;
                    language = LANGUAGE_CODES_GROUP_2[dataCodingScheme & 15];
                    break;
                case 3:
                    encoding2 = true;
                    break;
                case 4:
                case 5:
                    switch ((dataCodingScheme & 12) >> 2) {
                        case 1:
                            encoding2 = true;
                            break;
                        case 2:
                            encoding2 = true;
                            break;
                        default:
                            encoding2 = true;
                            break;
                    }
                case 6:
                case 7:
                    break;
                default:
                    switch (i) {
                        case 14:
                            break;
                        case 15:
                            if (((dataCodingScheme & 4) >> 2) != 1) {
                                encoding2 = true;
                                break;
                            }
                            encoding2 = true;
                            break;
                        default:
                            boolean hasLanguageIndicator = false;
                            encoding = true;
                            break;
                    }
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported GSM dataCodingScheme ");
        stringBuilder.append(dataCodingScheme);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private static Pair<String, String> unpackBody(byte[] pdu, int encoding, int offset, int length, boolean hasLanguageIndicator, String language) {
        String body = null;
        if (encoding == 1) {
            body = GsmAlphabet.gsm7BitPackedToString(pdu, offset, (length * 8) / 7);
            if (hasLanguageIndicator && body != null && body.length() > 2) {
                language = body.substring(0, 2);
                body = body.substring(3);
            }
        } else if (encoding == 3) {
            if (hasLanguageIndicator && pdu.length >= offset + 2) {
                language = GsmAlphabet.gsm7BitPackedToString(pdu, offset, 2);
                offset += 2;
                length -= 2;
            }
            try {
                body = new String(pdu, offset, 65534 & length, "utf-16");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("Error decoding UTF-16 message", e);
            }
        }
        if (body != null) {
            for (int i = body.length() - 1; i >= 0; i--) {
                if (body.charAt(i) != CARRIAGE_RETURN) {
                    body = body.substring(0, i + 1);
                    break;
                }
            }
        } else {
            body = "";
        }
        return new Pair(language, body);
    }
}
