package com.android.server.security.securityprofile;

import android.security.keystore.KeyProtection.Builder;
import android.util.Slog;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStore.SecretKeyEntry;
import java.security.SecureRandom;
import java.util.Formatter;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

class PolicyStorage {
    private static final String HMAC_SHA_ALGORITHM = "HmacSHA256";
    private static final String KEY_PREFIX = "SecurityProfileDB";
    private static final int PIECE_SIZE = 16384;
    public static final String TAG = "SecurityProfileDB";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private String databaseName = "/data/system/securityprofile.json";
    private String defaultContent;
    private SecretKey key;
    private String keyName = null;
    private Path mFile;

    public PolicyStorage(String dbName, String content) {
        this.databaseName = dbName;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SecurityProfileDB");
        stringBuilder.append(dbName);
        this.keyName = stringBuilder.toString();
        this.defaultContent = content;
        this.mFile = Paths.get(this.databaseName, new String[0]);
        initKey();
    }

    private void initKey() {
        if (!getKey(false)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("initKey get no old key , I must delete file:");
            stringBuilder.append(this.mFile);
            Slog.e("SecurityProfileDB", stringBuilder.toString());
            try {
                if (Files.exists(this.mFile, new LinkOption[0])) {
                    Files.delete(this.mFile);
                }
            } catch (Exception e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("initKey delete file error exception: ");
                stringBuilder.append(e.getMessage());
                Slog.e("SecurityProfileDB", stringBuilder.toString());
            }
            if (getKey(true)) {
                Slog.i("SecurityProfileDB", "initKey create new key Succ!");
            } else {
                Slog.e("SecurityProfileDB", "initKey create new key failed!");
            }
        }
        Slog.i("SecurityProfileDB", "initKey end!");
    }

    private boolean getKey(boolean create) {
        boolean z = false;
        if (create) {
            try {
                KeyGenerator kg = KeyGenerator.getInstance("AES");
                kg.init(new SecureRandom());
                KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
                ks.load(null);
                ks.deleteEntry(this.keyName);
                ks.setEntry(this.keyName, new SecretKeyEntry(kg.generateKey()), new Builder(3).setBlockModes(new String[]{"GCM"}).setEncryptionPaddings(new String[]{"NoPadding"}).setCriticalToDeviceEncryption(true).build());
                this.key = (SecretKey) ks.getKey(this.keyName, null);
                Slog.i("SecurityProfileDB", "getKey create new key end!");
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getKey exception");
                stringBuilder.append(e.getMessage());
                Slog.e("SecurityProfileDB", stringBuilder.toString());
                this.key = null;
                return false;
            }
        }
        KeyStore ks2 = KeyStore.getInstance("AndroidKeyStore");
        ks2.load(null);
        this.key = (SecretKey) ks2.getKey(this.keyName, null);
        if (this.key != null) {
            z = true;
        }
        return z;
    }

    private static String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        int length = bytes.length;
        for (int i = 0; i < length; i++) {
            formatter.format("%02x", new Object[]{Byte.valueOf(bytes[i])});
        }
        return formatter.toString();
    }

    private byte[] bufferedDoFinal(Cipher cipher, byte[] data) throws IOException, BadPaddingException, IllegalBlockSizeException {
        byte[] out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int offset = 0;
        while (offset + PIECE_SIZE <= data.length) {
            out = cipher.update(data, offset, 16384);
            if (out != null) {
                output.write(out);
            }
            offset += PIECE_SIZE;
        }
        out = cipher.doFinal(data, offset, data.length - offset);
        if (out != null) {
            output.write(out);
        }
        return output.toByteArray();
    }

    public void writeDatabase(JSONObject policy) {
        try {
            String json = policy.toString(4);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("writing: ");
            stringBuilder.append(json);
            Slog.d("SecurityProfileDB", stringBuilder.toString());
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(1, this.key);
            byte[] iv = cipher.getIV();
            byte[] cipherText = bufferedDoFinal(cipher, json.getBytes(StandardCharsets.UTF_8));
            ByteBuffer byteBuffer = ByteBuffer.allocate((4 + iv.length) + cipherText.length);
            byteBuffer.putInt(iv.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            Files.write(this.mFile, byteBuffer.array(), new OpenOption[0]);
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("write database exception");
            stringBuilder2.append(e.getMessage());
            Slog.e("SecurityProfileDB", stringBuilder2.toString());
        }
    }

    private String readAndVerifyDatabase() throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.wrap(Files.readAllBytes(this.mFile));
        int ivLength = byteBuffer.getInt();
        if (ivLength < 0 || ivLength > 64) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("readAndVerifyDatabase too large IV length err,ivLength:");
            stringBuilder.append(ivLength);
            stringBuilder.append(", mFile:");
            stringBuilder.append(this.mFile);
            Slog.e("SecurityProfileDB", stringBuilder.toString());
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Too large IV length:");
            stringBuilder2.append(ivLength);
            throw new InvalidAlgorithmParameterException(stringBuilder2.toString());
        }
        byte[] iv = new byte[ivLength];
        byteBuffer.get(iv);
        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(2, this.key, new GCMParameterSpec(128, iv));
        return new String(bufferedDoFinal(cipher, cipherText), StandardCharsets.UTF_8);
    }

    public JSONObject parseJSON(String json) {
        if (json == null) {
            return null;
        }
        try {
            return new JSONObject(json);
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("parseJSON err:");
            stringBuilder.append(e.getMessage());
            Slog.e("SecurityProfileDB", stringBuilder.toString());
            return null;
        }
    }

    public JSONObject readDatabase() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("readDabase file:");
        stringBuilder.append(this.mFile);
        Slog.d("SecurityProfileDB", stringBuilder.toString());
        StringBuilder stringBuilder2;
        try {
            if (Files.exists(this.mFile, new LinkOption[0])) {
                String json = readAndVerifyDatabase();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(this.mFile);
                stringBuilder2.append(" read:\n");
                stringBuilder2.append(json);
                Slog.d("SecurityProfileDB", stringBuilder2.toString());
                return parseJSON(json);
            } else if (this.defaultContent == null) {
                return null;
            } else {
                JSONObject policy = parseJSON(this.defaultContent);
                writeDatabase(policy);
                return policy;
            }
        } catch (Exception e) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("readDabase exception: ");
            stringBuilder2.append(e.getMessage());
            Slog.e("SecurityProfileDB", stringBuilder2.toString());
            return parseJSON(this.defaultContent);
        }
    }
}
