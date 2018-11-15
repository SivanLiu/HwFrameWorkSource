package com.android.server.net.watchlist;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestUtils {
    private static final int FILE_READ_BUFFER_SIZE = 16384;

    private DigestUtils() {
    }

    public static byte[] getSha256Hash(File apkFile) throws IOException, NoSuchAlgorithmException {
        Throwable th;
        Throwable th2;
        InputStream stream = new FileInputStream(apkFile);
        try {
            byte[] sha256Hash = getSha256Hash(stream);
            stream.close();
            return sha256Hash;
        } catch (Throwable th3) {
            th = th3;
        }
        throw th;
        if (th2 != null) {
            try {
                stream.close();
            } catch (Throwable th4) {
                th2.addSuppressed(th4);
            }
        } else {
            stream.close();
        }
        throw th;
    }

    public static byte[] getSha256Hash(InputStream stream) throws IOException, NoSuchAlgorithmException {
        MessageDigest digester = MessageDigest.getInstance("SHA256");
        byte[] buf = new byte[16384];
        while (true) {
            int read = stream.read(buf);
            int bytesRead = read;
            if (read < 0) {
                return digester.digest();
            }
            digester.update(buf, 0, bytesRead);
        }
    }
}
