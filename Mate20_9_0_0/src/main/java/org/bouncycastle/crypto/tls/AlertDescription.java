package org.bouncycastle.crypto.tls;

import org.bouncycastle.asn1.eac.EACTags;

public class AlertDescription {
    public static final short access_denied = (short) 49;
    public static final short bad_certificate = (short) 42;
    public static final short bad_certificate_hash_value = (short) 114;
    public static final short bad_certificate_status_response = (short) 113;
    public static final short bad_record_mac = (short) 20;
    public static final short certificate_expired = (short) 45;
    public static final short certificate_revoked = (short) 44;
    public static final short certificate_unknown = (short) 46;
    public static final short certificate_unobtainable = (short) 111;
    public static final short close_notify = (short) 0;
    public static final short decode_error = (short) 50;
    public static final short decompression_failure = (short) 30;
    public static final short decrypt_error = (short) 51;
    public static final short decryption_failed = (short) 21;
    public static final short export_restriction = (short) 60;
    public static final short handshake_failure = (short) 40;
    public static final short illegal_parameter = (short) 47;
    public static final short inappropriate_fallback = (short) 86;
    public static final short insufficient_security = (short) 71;
    public static final short internal_error = (short) 80;
    public static final short no_certificate = (short) 41;
    public static final short no_renegotiation = (short) 100;
    public static final short protocol_version = (short) 70;
    public static final short record_overflow = (short) 22;
    public static final short unexpected_message = (short) 10;
    public static final short unknown_ca = (short) 48;
    public static final short unknown_psk_identity = (short) 115;
    public static final short unrecognized_name = (short) 112;
    public static final short unsupported_certificate = (short) 43;
    public static final short unsupported_extension = (short) 110;
    public static final short user_canceled = (short) 90;

    public static String getName(short s) {
        switch (s) {
            case (short) 20:
                return "bad_record_mac";
            case (short) 21:
                return "decryption_failed";
            case (short) 22:
                return "record_overflow";
            default:
                switch (s) {
                    case (short) 40:
                        return "handshake_failure";
                    case EACTags.INTERCHANGE_PROFILE /*41*/:
                        return "no_certificate";
                    case EACTags.CURRENCY_CODE /*42*/:
                        return "bad_certificate";
                    case EACTags.DATE_OF_BIRTH /*43*/:
                        return "unsupported_certificate";
                    case (short) 44:
                        return "certificate_revoked";
                    case (short) 45:
                        return "certificate_expired";
                    case (short) 46:
                        return "certificate_unknown";
                    case (short) 47:
                        return "illegal_parameter";
                    case (short) 48:
                        return "unknown_ca";
                    case CipherSuite.TLS_DH_RSA_WITH_AES_128_CBC_SHA /*49*/:
                        return "access_denied";
                    case (short) 50:
                        return "decode_error";
                    case (short) 51:
                        return "decrypt_error";
                    default:
                        switch (s) {
                            case (short) 70:
                                return "protocol_version";
                            case EACTags.MESSAGE_REFERENCE /*71*/:
                                return "insufficient_security";
                            default:
                                switch (s) {
                                    case (short) 110:
                                        return "unsupported_extension";
                                    case (short) 111:
                                        return "certificate_unobtainable";
                                    case (short) 112:
                                        return "unrecognized_name";
                                    case (short) 113:
                                        return "bad_certificate_status_response";
                                    case (short) 114:
                                        return "bad_certificate_hash_value";
                                    case (short) 115:
                                        return "unknown_psk_identity";
                                    default:
                                        switch (s) {
                                            case (short) 0:
                                                return "close_notify";
                                            case (short) 10:
                                                return "unexpected_message";
                                            case (short) 30:
                                                return "decompression_failure";
                                            case (short) 60:
                                                return "export_restriction";
                                            case EACTags.UNIFORM_RESOURCE_LOCATOR /*80*/:
                                                return "internal_error";
                                            case (short) 86:
                                                return "inappropriate_fallback";
                                            case (short) 90:
                                                return "user_canceled";
                                            case (short) 100:
                                                return "no_renegotiation";
                                            default:
                                                return "UNKNOWN";
                                        }
                                }
                        }
                }
        }
    }

    public static String getText(short s) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getName(s));
        stringBuilder.append("(");
        stringBuilder.append(s);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}
