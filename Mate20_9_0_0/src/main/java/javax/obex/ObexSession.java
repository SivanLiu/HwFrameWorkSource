package javax.obex;

import java.io.IOException;

public class ObexSession {
    private static final String TAG = "ObexSession";
    private static final boolean V = false;
    protected Authenticator mAuthenticator;
    protected byte[] mChallengeDigest;

    public boolean handleAuthChall(HeaderSet header) throws IOException {
        HeaderSet headerSet = header;
        if (this.mAuthenticator == null) {
            return false;
        }
        byte[] challenge = ObexHelper.getTagValue((byte) 0, headerSet.mAuthChall);
        byte[] option = ObexHelper.getTagValue((byte) 1, headerSet.mAuthChall);
        byte[] description = ObexHelper.getTagValue((byte) 2, headerSet.mAuthChall);
        String realm = null;
        if (description != null) {
            byte[] realmString = new byte[(description.length - 1)];
            System.arraycopy(description, 1, realmString, 0, realmString.length);
            int i = description[0] & 255;
            if (i != 255) {
                switch (i) {
                    case ObexHelper.OBEX_AUTH_REALM_CHARSET_ASCII /*0*/:
                    case 1:
                        try {
                            realm = new String(realmString, "ISO8859_1");
                            break;
                        } catch (Exception e) {
                            throw new IOException("Unsupported Encoding Scheme");
                        }
                    default:
                        throw new IOException("Unsupported Encoding Scheme");
                }
            }
            realm = ObexHelper.convertToUnicode(realmString, false);
        }
        boolean isUserIDRequired = false;
        boolean isUserIDRequired2 = true;
        if (option != null) {
            if ((option[0] & 1) != 0) {
                isUserIDRequired = true;
            }
            if ((option[0] & 2) != 0) {
                isUserIDRequired2 = false;
            }
        }
        boolean isFullAccess = isUserIDRequired2;
        isUserIDRequired2 = isUserIDRequired;
        headerSet.mAuthChall = null;
        try {
            PasswordAuthentication result = this.mAuthenticator.onAuthenticationChallenge(realm, isUserIDRequired2, isFullAccess);
            if (result == null) {
                return false;
            }
            PasswordAuthentication result2 = result.getPassword();
            if (result2 == null) {
                return false;
            }
            byte[] userName = result.getUserName();
            if (userName != null) {
                headerSet.mAuthResp = new byte[(userName.length + 38)];
                headerSet.mAuthResp[36] = (byte) 1;
                headerSet.mAuthResp[37] = (byte) userName.length;
                System.arraycopy(userName, 0, headerSet.mAuthResp, 38, userName.length);
            } else {
                headerSet.mAuthResp = new byte[36];
            }
            byte[] digest = new byte[((challenge.length + result2.length) + 1)];
            System.arraycopy(challenge, 0, digest, 0, challenge.length);
            digest[challenge.length] = (byte) 58;
            System.arraycopy(result2, 0, digest, challenge.length + 1, result2.length);
            headerSet.mAuthResp[0] = (byte) 0;
            headerSet.mAuthResp[1] = (byte) 16;
            System.arraycopy(ObexHelper.computeMd5Hash(digest), 0, headerSet.mAuthResp, 2, 16);
            headerSet.mAuthResp[18] = (byte) 2;
            headerSet.mAuthResp[19] = (byte) 16;
            System.arraycopy(challenge, 0, headerSet.mAuthResp, 20, 16);
            return true;
        } catch (Exception e2) {
            return false;
        }
    }

    public boolean handleAuthResp(byte[] authResp) {
        if (this.mAuthenticator == null) {
            return false;
        }
        byte[] correctPassword = this.mAuthenticator.onAuthenticationResponse(ObexHelper.getTagValue((byte) 1, authResp));
        if (correctPassword == null) {
            return false;
        }
        byte[] temp = new byte[(correctPassword.length + 16)];
        System.arraycopy(this.mChallengeDigest, 0, temp, 0, 16);
        System.arraycopy(correctPassword, 0, temp, 16, correctPassword.length);
        byte[] correctResponse = ObexHelper.computeMd5Hash(temp);
        byte[] actualResponse = ObexHelper.getTagValue((byte) 0, authResp);
        for (int i = 0; i < 16; i++) {
            if (correctResponse[i] != actualResponse[i]) {
                return false;
            }
        }
        return true;
    }
}
