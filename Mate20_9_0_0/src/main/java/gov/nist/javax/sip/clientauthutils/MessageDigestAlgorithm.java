package gov.nist.javax.sip.clientauthutils;

import gov.nist.core.Separators;
import gov.nist.core.StackLogger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MessageDigestAlgorithm {
    private static final char[] toHex = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    static String calculateResponse(String algorithm, String hashUserNameRealmPasswd, String nonce_value, String nc_value, String cnonce_value, String method, String digest_uri_value, String entity_body, String qop_value, StackLogger stackLogger) {
        if (stackLogger.isLoggingEnabled()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("trying to authenticate using : ");
            stringBuilder.append(algorithm);
            stringBuilder.append(", ");
            stringBuilder.append(hashUserNameRealmPasswd);
            stringBuilder.append(", ");
            stringBuilder.append(nonce_value);
            stringBuilder.append(", ");
            stringBuilder.append(nc_value);
            stringBuilder.append(", ");
            stringBuilder.append(cnonce_value);
            stringBuilder.append(", ");
            stringBuilder.append(method);
            stringBuilder.append(", ");
            stringBuilder.append(digest_uri_value);
            stringBuilder.append(", ");
            stringBuilder.append(entity_body);
            stringBuilder.append(", ");
            stringBuilder.append(qop_value);
            stackLogger.logDebug(stringBuilder.toString());
        }
        if (hashUserNameRealmPasswd == null || method == null || digest_uri_value == null || nonce_value == null) {
            throw new NullPointerException("Null parameter to MessageDigestAlgorithm.calculateResponse()");
        } else if (cnonce_value == null || cnonce_value.length() == 0) {
            throw new NullPointerException("cnonce_value may not be absent for MD5-Sess algorithm.");
        } else {
            String A2;
            StringBuilder stringBuilder2;
            if (qop_value == null || qop_value.trim().length() == 0 || qop_value.trim().equalsIgnoreCase("auth")) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(method);
                stringBuilder2.append(Separators.COLON);
                stringBuilder2.append(digest_uri_value);
                A2 = stringBuilder2.toString();
            } else {
                if (entity_body == null) {
                    entity_body = "";
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(method);
                stringBuilder2.append(Separators.COLON);
                stringBuilder2.append(digest_uri_value);
                stringBuilder2.append(Separators.COLON);
                stringBuilder2.append(H(entity_body));
                A2 = stringBuilder2.toString();
            }
            StringBuilder stringBuilder3;
            if (cnonce_value == null || qop_value == null || nc_value == null || !(qop_value.equalsIgnoreCase("auth") || qop_value.equalsIgnoreCase("auth-int"))) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(nonce_value);
                stringBuilder3.append(Separators.COLON);
                stringBuilder3.append(H(A2));
                return KD(hashUserNameRealmPasswd, stringBuilder3.toString());
            }
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(nonce_value);
            stringBuilder3.append(Separators.COLON);
            stringBuilder3.append(nc_value);
            stringBuilder3.append(Separators.COLON);
            stringBuilder3.append(cnonce_value);
            stringBuilder3.append(Separators.COLON);
            stringBuilder3.append(qop_value);
            stringBuilder3.append(Separators.COLON);
            stringBuilder3.append(H(A2));
            return KD(hashUserNameRealmPasswd, stringBuilder3.toString());
        }
    }

    static String calculateResponse(String algorithm, String username_value, String realm_value, String passwd, String nonce_value, String nc_value, String cnonce_value, String method, String digest_uri_value, String entity_body, String qop_value, StackLogger stackLogger) {
        String str = algorithm;
        String str2 = username_value;
        String str3 = realm_value;
        String str4 = passwd;
        String str5 = nonce_value;
        String str6 = nc_value;
        String str7 = cnonce_value;
        String str8 = method;
        String str9 = digest_uri_value;
        String entity_body2 = entity_body;
        String str10 = qop_value;
        if (stackLogger.isLoggingEnabled()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("trying to authenticate using : ");
            stringBuilder.append(str);
            stringBuilder.append(", ");
            stringBuilder.append(str2);
            stringBuilder.append(", ");
            stringBuilder.append(str3);
            stringBuilder.append(", ");
            boolean z = str4 != null && passwd.trim().length() > 0;
            stringBuilder.append(z);
            stringBuilder.append(", ");
            stringBuilder.append(str5);
            stringBuilder.append(", ");
            stringBuilder.append(str6);
            stringBuilder.append(", ");
            stringBuilder.append(str7);
            stringBuilder.append(", ");
            stringBuilder.append(str8);
            stringBuilder.append(", ");
            stringBuilder.append(str9);
            stringBuilder.append(", ");
            stringBuilder.append(entity_body2);
            stringBuilder.append(", ");
            stringBuilder.append(str10);
            stackLogger.logDebug(stringBuilder.toString());
        } else {
            StackLogger stackLogger2 = stackLogger;
        }
        if (str2 == null || str3 == null || str4 == null || str8 == null || str9 == null || str5 == null) {
            throw new NullPointerException("Null parameter to MessageDigestAlgorithm.calculateResponse()");
        }
        String A1;
        StringBuilder stringBuilder2;
        String A2;
        StringBuilder stringBuilder3;
        if (str == null || algorithm.trim().length() == 0 || algorithm.trim().equalsIgnoreCase("MD5")) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str2);
            stringBuilder3.append(Separators.COLON);
            stringBuilder3.append(str3);
            stringBuilder3.append(Separators.COLON);
            stringBuilder3.append(str4);
            A1 = stringBuilder3.toString();
        } else if (str7 == null || cnonce_value.length() == 0) {
            throw new NullPointerException("cnonce_value may not be absent for MD5-Sess algorithm.");
        } else {
            stringBuilder3 = new StringBuilder();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(str2);
            stringBuilder2.append(Separators.COLON);
            stringBuilder2.append(str3);
            stringBuilder2.append(Separators.COLON);
            stringBuilder2.append(str4);
            stringBuilder3.append(H(stringBuilder2.toString()));
            stringBuilder3.append(Separators.COLON);
            stringBuilder3.append(str5);
            stringBuilder3.append(Separators.COLON);
            stringBuilder3.append(str7);
            A1 = stringBuilder3.toString();
        }
        if (str10 == null || qop_value.trim().length() == 0 || qop_value.trim().equalsIgnoreCase("auth")) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(str8);
            stringBuilder2.append(Separators.COLON);
            stringBuilder2.append(str9);
            A2 = stringBuilder2.toString();
        } else {
            if (entity_body2 == null) {
                entity_body2 = "";
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(str8);
            stringBuilder2.append(Separators.COLON);
            stringBuilder2.append(str9);
            stringBuilder2.append(Separators.COLON);
            stringBuilder2.append(H(entity_body2));
            A2 = stringBuilder2.toString();
        }
        if (str7 == null || str10 == null || str6 == null || !(str10.equalsIgnoreCase("auth") || str10.equalsIgnoreCase("auth-int"))) {
            str = H(A1);
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append(str5);
            stringBuilder4.append(Separators.COLON);
            stringBuilder4.append(H(A2));
            return KD(str, stringBuilder4.toString());
        }
        String H = H(A1);
        StringBuilder stringBuilder5 = new StringBuilder();
        stringBuilder5.append(str5);
        stringBuilder5.append(Separators.COLON);
        stringBuilder5.append(str6);
        stringBuilder5.append(Separators.COLON);
        stringBuilder5.append(str7);
        stringBuilder5.append(Separators.COLON);
        stringBuilder5.append(str10);
        stringBuilder5.append(Separators.COLON);
        stringBuilder5.append(H(A2));
        return KD(H, stringBuilder5.toString());
    }

    private static String H(String data) {
        try {
            return toHexString(MessageDigest.getInstance("MD5").digest(data.getBytes()));
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Failed to instantiate an MD5 algorithm", ex);
        }
    }

    private static String KD(String secret, String data) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(secret);
        stringBuilder.append(Separators.COLON);
        stringBuilder.append(data);
        return H(stringBuilder.toString());
    }

    private static String toHexString(byte[] b) {
        int pos = 0;
        char[] c = new char[(b.length * 2)];
        for (int i = 0; i < b.length; i++) {
            int pos2 = pos + 1;
            c[pos] = toHex[(b[i] >> 4) & 15];
            pos = pos2 + 1;
            c[pos2] = toHex[b[i] & 15];
        }
        return new String(c);
    }
}
