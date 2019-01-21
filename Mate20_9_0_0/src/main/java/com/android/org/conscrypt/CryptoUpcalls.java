package com.android.org.conscrypt;

import com.android.org.conscrypt.ct.CTConstants;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

final class CryptoUpcalls {
    private static final Logger logger = Logger.getLogger(CryptoUpcalls.class.getName());

    private CryptoUpcalls() {
    }

    private static boolean isOurProvider(Provider p) {
        return p.getClass().getPackage().equals(CryptoUpcalls.class.getPackage());
    }

    private static ArrayList<Provider> getExternalProviders(String algorithm) {
        ArrayList<Provider> providers = new ArrayList(1);
        for (Provider p : Security.getProviders(algorithm)) {
            if (!isOurProvider(p)) {
                providers.add(p);
            }
        }
        if (providers.isEmpty()) {
            Logger logger = logger;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not find external provider for algorithm: ");
            stringBuilder.append(algorithm);
            logger.warning(stringBuilder.toString());
        }
        return providers;
    }

    static byte[] rawSignDigestWithPrivateKey(PrivateKey javaKey, byte[] message) {
        String algorithm;
        Signature signature;
        Logger logger;
        String keyAlgorithm = javaKey.getAlgorithm();
        if ("RSA".equals(keyAlgorithm)) {
            algorithm = "NONEwithRSA";
        } else if ("EC".equals(keyAlgorithm)) {
            algorithm = "NONEwithECDSA";
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected key type: ");
            stringBuilder.append(javaKey.toString());
            throw new RuntimeException(stringBuilder.toString());
        }
        try {
            signature = Signature.getInstance(algorithm);
            signature.initSign(javaKey);
            if (isOurProvider(signature.getProvider())) {
                signature = null;
            }
        } catch (NoSuchAlgorithmException e) {
            Logger logger2 = logger;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unsupported signature algorithm: ");
            stringBuilder2.append(algorithm);
            logger2.warning(stringBuilder2.toString());
            return null;
        } catch (InvalidKeyException e2) {
            logger.warning("Preferred provider doesn't support key:");
            e2.printStackTrace();
            signature = null;
        }
        if (signature == null) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Signature.");
            stringBuilder3.append(algorithm);
            Iterator it = getExternalProviders(stringBuilder3.toString()).iterator();
            while (it.hasNext()) {
                try {
                    signature = Signature.getInstance(algorithm, (Provider) it.next());
                    signature.initSign(javaKey);
                    break;
                } catch (InvalidKeyException | NoSuchAlgorithmException e3) {
                    signature = null;
                }
            }
            if (signature == null) {
                logger = logger;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Could not find provider for algorithm: ");
                stringBuilder4.append(algorithm);
                logger.warning(stringBuilder4.toString());
                return null;
            }
        }
        try {
            signature.update(message);
            return signature.sign();
        } catch (Exception e4) {
            logger = logger;
            Level level = Level.WARNING;
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append("Exception while signing message with ");
            stringBuilder5.append(javaKey.getAlgorithm());
            stringBuilder5.append(" private key:");
            logger.log(level, stringBuilder5.toString(), e4);
            return null;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:34:0x00b9  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static byte[] rsaDecryptWithPrivateKey(PrivateKey javaKey, int openSSLPadding, byte[] input) {
        Logger logger;
        StringBuilder stringBuilder;
        String keyAlgorithm = javaKey.getAlgorithm();
        Logger logger2;
        StringBuilder stringBuilder2;
        if ("RSA".equals(keyAlgorithm)) {
            String jcaPadding;
            if (openSSLPadding != 1) {
                switch (openSSLPadding) {
                    case CTConstants.CERTIFICATE_LENGTH_BYTES /*3*/:
                        jcaPadding = "NoPadding";
                        break;
                    case 4:
                        jcaPadding = "OAEPPadding";
                        break;
                    default:
                        logger2 = logger;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unsupported OpenSSL/BoringSSL padding: ");
                        stringBuilder2.append(openSSLPadding);
                        logger2.warning(stringBuilder2.toString());
                        return null;
                }
            }
            jcaPadding = "PKCS1Padding";
            String transformation = new StringBuilder();
            transformation.append("RSA/ECB/");
            transformation.append(jcaPadding);
            transformation = transformation.toString();
            Cipher c = null;
            try {
                c = Cipher.getInstance(transformation);
                c.init(2, javaKey);
                if (isOurProvider(c.getProvider())) {
                    c = null;
                }
            } catch (NoSuchAlgorithmException e) {
                logger = logger;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported cipher algorithm: ");
                stringBuilder.append(transformation);
                logger.warning(stringBuilder.toString());
                return null;
            } catch (NoSuchPaddingException e2) {
                logger = logger;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported cipher algorithm: ");
                stringBuilder.append(transformation);
                logger.warning(stringBuilder.toString());
                return null;
            } catch (InvalidKeyException e3) {
                logger.log(Level.WARNING, "Preferred provider doesn't support key:", e3);
                c = null;
            }
            if (c == null) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Cipher.");
                stringBuilder3.append(transformation);
                Iterator it = getExternalProviders(stringBuilder3.toString()).iterator();
                while (it.hasNext()) {
                    try {
                        c = Cipher.getInstance(transformation, (Provider) it.next());
                        c.init(2, javaKey);
                        if (c == null) {
                            Logger logger3 = logger;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Could not find provider for algorithm: ");
                            stringBuilder.append(transformation);
                            logger3.warning(stringBuilder.toString());
                            return null;
                        }
                    } catch (NoSuchAlgorithmException e4) {
                        c = null;
                    } catch (InvalidKeyException e5) {
                        c = null;
                    } catch (NoSuchPaddingException e6) {
                        c = null;
                    }
                }
                if (c == null) {
                }
            }
            try {
                return c.doFinal(input);
            } catch (Exception e7) {
                logger = logger;
                Level level = Level.WARNING;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Exception while decrypting message with ");
                stringBuilder4.append(javaKey.getAlgorithm());
                stringBuilder4.append(" private key using ");
                stringBuilder4.append(transformation);
                stringBuilder4.append(":");
                logger.log(level, stringBuilder4.toString(), e7);
                return null;
            }
        }
        logger2 = logger;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Unexpected key type: ");
        stringBuilder2.append(keyAlgorithm);
        logger2.warning(stringBuilder2.toString());
        return null;
    }
}
