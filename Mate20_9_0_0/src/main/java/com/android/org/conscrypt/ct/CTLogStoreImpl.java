package com.android.org.conscrypt.ct;

import com.android.org.conscrypt.InternalUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class CTLogStoreImpl implements CTLogStore {
    private static final char[] HEX_DIGITS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    private static volatile CTLogInfo[] defaultFallbackLogs = null;
    private static final File defaultSystemLogDir;
    private static final File defaultUserLogDir;
    private CTLogInfo[] fallbackLogs;
    private HashMap<ByteBuffer, CTLogInfo> logCache;
    private Set<ByteBuffer> missingLogCache;
    private File systemLogDir;
    private File userLogDir;

    public static class InvalidLogFileException extends Exception {
        public InvalidLogFileException(String message) {
            super(message);
        }

        public InvalidLogFileException(String message, Throwable cause) {
            super(message, cause);
        }

        public InvalidLogFileException(Throwable cause) {
            super(cause);
        }
    }

    static {
        String ANDROID_DATA = System.getenv("ANDROID_DATA");
        String ANDROID_ROOT = System.getenv("ANDROID_ROOT");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ANDROID_DATA);
        stringBuilder.append("/misc/keychain/trusted_ct_logs/current/");
        defaultUserLogDir = new File(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(ANDROID_ROOT);
        stringBuilder.append("/etc/security/ct_known_logs/");
        defaultSystemLogDir = new File(stringBuilder.toString());
    }

    public CTLogStoreImpl() {
        this(defaultUserLogDir, defaultSystemLogDir, getDefaultFallbackLogs());
    }

    public CTLogStoreImpl(File userLogDir, File systemLogDir, CTLogInfo[] fallbackLogs) {
        this.logCache = new HashMap();
        this.missingLogCache = Collections.synchronizedSet(new HashSet());
        this.userLogDir = userLogDir;
        this.systemLogDir = systemLogDir;
        this.fallbackLogs = fallbackLogs;
    }

    public CTLogInfo getKnownLog(byte[] logId) {
        ByteBuffer buf = ByteBuffer.wrap(logId);
        CTLogInfo log = (CTLogInfo) this.logCache.get(buf);
        if (log != null) {
            return log;
        }
        if (this.missingLogCache.contains(buf)) {
            return null;
        }
        log = findKnownLog(logId);
        if (log != null) {
            this.logCache.put(buf, log);
        } else {
            this.missingLogCache.add(buf);
        }
        return log;
    }

    private CTLogInfo findKnownLog(byte[] logId) {
        String filename = hexEncode(logId);
        try {
            return loadLog(new File(this.userLogDir, filename));
        } catch (InvalidLogFileException e) {
            return null;
        } catch (FileNotFoundException e2) {
            try {
                return loadLog(new File(this.systemLogDir, filename));
            } catch (InvalidLogFileException e3) {
                return null;
            } catch (FileNotFoundException e4) {
                if (!this.userLogDir.exists()) {
                    for (CTLogInfo log : this.fallbackLogs) {
                        if (Arrays.equals(logId, log.getID())) {
                            return log;
                        }
                    }
                }
                return null;
            }
        }
    }

    public static CTLogInfo[] getDefaultFallbackLogs() {
        CTLogInfo[] result = defaultFallbackLogs;
        if (result != null) {
            return result;
        }
        CTLogInfo[] createDefaultFallbackLogs = createDefaultFallbackLogs();
        result = createDefaultFallbackLogs;
        defaultFallbackLogs = createDefaultFallbackLogs;
        return result;
    }

    private static CTLogInfo[] createDefaultFallbackLogs() {
        CTLogInfo[] logs = new CTLogInfo[8];
        int i = 0;
        while (i < 8) {
            try {
                logs[i] = new CTLogInfo(InternalUtil.logKeyToPublicKey(KnownLogs.LOG_KEYS[i]), KnownLogs.LOG_DESCRIPTIONS[i], KnownLogs.LOG_URLS[i]);
                i++;
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        defaultFallbackLogs = logs;
        return logs;
    }

    public static CTLogInfo loadLog(File file) throws FileNotFoundException, InvalidLogFileException {
        return loadLog(new FileInputStream(file));
    }

    /* JADX WARNING: Removed duplicated region for block: B:30:0x0072  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x0070  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x006d  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0072  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x0070  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x006d  */
    /* JADX WARNING: Missing block: B:25:0x0065, code skipped:
            if (r7.equals("description") != false) goto L_0x0069;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static CTLogInfo loadLog(InputStream input) throws InvalidLogFileException {
        Scanner scan = new Scanner(input, "UTF-8");
        scan.useDelimiter("\n");
        String description = null;
        String url = null;
        String key = null;
        try {
            if (!scan.hasNext()) {
                return null;
            }
            while (scan.hasNext()) {
                String[] parts = scan.next().split(":", 2);
                if (parts.length >= 2) {
                    int i = 0;
                    String name = parts[0];
                    String value = parts[1];
                    int hashCode = name.hashCode();
                    if (hashCode != -1724546052) {
                        if (hashCode == 106079) {
                            if (name.equals("key")) {
                                i = 2;
                                switch (i) {
                                    case 0:
                                        break;
                                    case 1:
                                        break;
                                    case 2:
                                        break;
                                }
                            }
                        } else if (hashCode == 116079) {
                            if (name.equals("url")) {
                                i = 1;
                                switch (i) {
                                    case 0:
                                        description = value;
                                        break;
                                    case 1:
                                        url = value;
                                        break;
                                    case 2:
                                        key = value;
                                        break;
                                }
                            }
                        }
                    }
                    i = -1;
                    switch (i) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            break;
                    }
                }
            }
            scan.close();
            if (description == null || url == null || key == null) {
                throw new InvalidLogFileException("Missing one of 'description', 'url' or 'key'");
            }
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("-----BEGIN PUBLIC KEY-----\n");
                stringBuilder.append(key);
                stringBuilder.append("\n-----END PUBLIC KEY-----");
                return new CTLogInfo(InternalUtil.readPublicKeyPem(new ByteArrayInputStream(stringBuilder.toString().getBytes(US_ASCII))), description, url);
            } catch (InvalidKeyException e) {
                throw new InvalidLogFileException(e);
            } catch (NoSuchAlgorithmException e2) {
                throw new InvalidLogFileException(e2);
            }
        } finally {
            scan.close();
        }
    }

    private static String hexEncode(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(HEX_DIGITS[(b >> 4) & 15]);
            sb.append(HEX_DIGITS[b & 15]);
        }
        return sb.toString();
    }
}
