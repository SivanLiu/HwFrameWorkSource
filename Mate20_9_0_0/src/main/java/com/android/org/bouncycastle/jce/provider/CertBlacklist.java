package com.android.org.bouncycastle.jce.provider;

import com.android.org.bouncycastle.crypto.Digest;
import com.android.org.bouncycastle.crypto.digests.AndroidDigestFactory;
import com.android.org.bouncycastle.util.encoders.Hex;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CertBlacklist {
    private static final Logger logger = Logger.getLogger(CertBlacklist.class.getName());
    public final Set<byte[]> pubkeyBlacklist;
    public final Set<BigInteger> serialBlacklist;

    public CertBlacklist() {
        String androidData = System.getenv("ANDROID_DATA");
        String blacklistRoot = new StringBuilder();
        blacklistRoot.append(androidData);
        blacklistRoot.append("/misc/keychain/");
        blacklistRoot = blacklistRoot.toString();
        String defaultPubkeyBlacklistPath = new StringBuilder();
        defaultPubkeyBlacklistPath.append(blacklistRoot);
        defaultPubkeyBlacklistPath.append("pubkey_blacklist.txt");
        defaultPubkeyBlacklistPath = defaultPubkeyBlacklistPath.toString();
        String defaultSerialBlacklistPath = new StringBuilder();
        defaultSerialBlacklistPath.append(blacklistRoot);
        defaultSerialBlacklistPath.append("serial_blacklist.txt");
        defaultSerialBlacklistPath = defaultSerialBlacklistPath.toString();
        this.pubkeyBlacklist = readPublicKeyBlackList(defaultPubkeyBlacklistPath);
        this.serialBlacklist = readSerialBlackList(defaultSerialBlacklistPath);
    }

    public CertBlacklist(String pubkeyBlacklistPath, String serialBlacklistPath) {
        this.pubkeyBlacklist = readPublicKeyBlackList(pubkeyBlacklistPath);
        this.serialBlacklist = readSerialBlackList(serialBlacklistPath);
    }

    private static boolean isHex(String value) {
        try {
            BigInteger bigInteger = new BigInteger(value, 16);
            return true;
        } catch (NumberFormatException e) {
            Logger logger = logger;
            Level level = Level.WARNING;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not parse hex value ");
            stringBuilder.append(value);
            logger.log(level, stringBuilder.toString(), e);
            return false;
        }
    }

    private static boolean isPubkeyHash(String value) {
        if (value.length() == 40) {
            return isHex(value);
        }
        Logger logger = logger;
        Level level = Level.WARNING;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid pubkey hash length: ");
        stringBuilder.append(value.length());
        logger.log(level, stringBuilder.toString());
        return false;
    }

    private static String readBlacklist(String path) {
        try {
            return readFileAsString(path);
        } catch (FileNotFoundException e) {
            return "";
        } catch (IOException e2) {
            logger.log(Level.WARNING, "Could not read blacklist", e2);
            return "";
        }
    }

    private static String readFileAsString(String path) throws IOException {
        return readFileAsBytes(path).toString("UTF-8");
    }

    private static ByteArrayOutputStream readFileAsBytes(String path) throws IOException {
        RandomAccessFile f = null;
        try {
            f = new RandomAccessFile(path, "r");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream((int) f.length());
            byte[] buffer = new byte[8192];
            while (true) {
                int byteCount = f.read(buffer);
                if (byteCount == -1) {
                    break;
                }
                bytes.write(buffer, 0, byteCount);
            }
            return bytes;
        } finally {
            closeQuietly(f);
        }
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception e) {
            }
        }
    }

    private static final Set<BigInteger> readSerialBlackList(String path) {
        r1 = new BigInteger[10];
        int i = 0;
        r1[0] = new BigInteger("077a59bcd53459601ca6907267a6dd1c", 16);
        r1[1] = new BigInteger("047ecbe9fca55f7bd09eae36e10cae1e", 16);
        r1[2] = new BigInteger("d8f35f4eb7872b2dab0692e315382fb0", 16);
        r1[3] = new BigInteger("b0b7133ed096f9b56fae91c874bd3ac0", 16);
        r1[4] = new BigInteger("9239d5348f40d1695a745470e1f23f43", 16);
        r1[5] = new BigInteger("e9028b9578e415dc1a710a2b88154447", 16);
        r1[6] = new BigInteger("d7558fdaf5f1105bb213282b707729a3", 16);
        r1[7] = new BigInteger("f5c86af36162f13a64f54f6dc9587c06", 16);
        r1[8] = new BigInteger("392a434f0e07df1f8aa305de34e0c229", 16);
        r1[9] = new BigInteger("3e75ced46b693021218830ae86a82a71", 16);
        Set<BigInteger> bl = new HashSet(Arrays.asList(r1));
        String serialBlacklist = readBlacklist(path);
        if (!serialBlacklist.equals("")) {
            String[] split = serialBlacklist.split(",");
            int length = split.length;
            while (i < length) {
                String value = split[i];
                try {
                    bl.add(new BigInteger(value, 16));
                } catch (NumberFormatException e) {
                    Logger logger = logger;
                    Level level = Level.WARNING;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Tried to blacklist invalid serial number ");
                    stringBuilder.append(value);
                    logger.log(level, stringBuilder.toString(), e);
                }
                i++;
            }
        }
        return Collections.unmodifiableSet(bl);
    }

    private static final Set<byte[]> readPublicKeyBlackList(String path) {
        r1 = new byte[9][];
        int i = 0;
        r1[0] = "410f36363258f30b347d12ce4863e433437806a8".getBytes();
        r1[1] = "ba3e7bd38cd7e1e6b9cd4c219962e59d7a2f4e37".getBytes();
        r1[2] = "e23b8d105f87710a68d9248050ebefc627be4ca6".getBytes();
        r1[3] = "7b2e16bc39bcd72b456e9f055d1de615b74945db".getBytes();
        r1[4] = "e8f91200c65cee16e039b9f883841661635f81c5".getBytes();
        r1[5] = "0129bcd5b448ae8d2496d1c3e19723919088e152".getBytes();
        r1[6] = "5f3ab33d55007054bc5e3e5553cd8d8465d77c61".getBytes();
        r1[7] = "783333c9687df63377efceddd82efa9101913e8e".getBytes();
        r1[8] = "3ecf4bbbe46096d514bb539bb913d77aa4ef31bf".getBytes();
        Set<byte[]> bl = new HashSet(Arrays.asList(r1));
        String pubkeyBlacklist = readBlacklist(path);
        if (!pubkeyBlacklist.equals("")) {
            String[] split = pubkeyBlacklist.split(",");
            int length = split.length;
            while (i < length) {
                String value = split[i].trim();
                if (isPubkeyHash(value)) {
                    bl.add(value.getBytes());
                } else {
                    Logger logger = logger;
                    Level level = Level.WARNING;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Tried to blacklist invalid pubkey ");
                    stringBuilder.append(value);
                    logger.log(level, stringBuilder.toString());
                }
                i++;
            }
        }
        return bl;
    }

    public boolean isPublicKeyBlackListed(PublicKey publicKey) {
        byte[] encoded = publicKey.getEncoded();
        Digest digest = AndroidDigestFactory.getSHA1();
        digest.update(encoded, 0, encoded.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        for (byte[] blacklisted : this.pubkeyBlacklist) {
            if (Arrays.equals(blacklisted, Hex.encode(out))) {
                return true;
            }
        }
        return false;
    }

    public boolean isSerialNumberBlackListed(BigInteger serial) {
        return this.serialBlacklist.contains(serial);
    }
}
