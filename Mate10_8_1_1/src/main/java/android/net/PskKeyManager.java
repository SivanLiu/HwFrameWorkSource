package android.net;

import com.android.org.conscrypt.PSKKeyManager;
import java.net.Socket;
import javax.crypto.SecretKey;
import javax.net.ssl.SSLEngine;

public abstract class PskKeyManager implements PSKKeyManager {
    public static final int MAX_IDENTITY_HINT_LENGTH_BYTES = 128;
    public static final int MAX_IDENTITY_LENGTH_BYTES = 128;
    public static final int MAX_KEY_LENGTH_BYTES = 256;

    public String chooseServerKeyIdentityHint(Socket socket) {
        return null;
    }

    public String chooseServerKeyIdentityHint(SSLEngine engine) {
        return null;
    }

    public String chooseClientKeyIdentity(String identityHint, Socket socket) {
        return "";
    }

    public String chooseClientKeyIdentity(String identityHint, SSLEngine engine) {
        return "";
    }

    public SecretKey getKey(String identityHint, String identity, Socket socket) {
        return null;
    }

    public SecretKey getKey(String identityHint, String identity, SSLEngine engine) {
        return null;
    }
}
