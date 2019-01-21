package com.android.server.updates;

import android.os.FileUtils;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Base64;
import android.util.Slog;
import com.android.internal.util.HexDump;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CertificateTransparencyLogInstallReceiver extends ConfigUpdateInstallReceiver {
    private static final String LOGDIR_PREFIX = "logs-";
    private static final String TAG = "CTLogInstallReceiver";

    public CertificateTransparencyLogInstallReceiver() {
        super("/data/misc/keychain/trusted_ct_logs/", "ct_logs", "metadata/", "version");
    }

    /* JADX WARNING: Removed duplicated region for block: B:41:0x0135 A:{ExcHandler: IOException | RuntimeException (r1_10 'e' java.lang.Exception), Splitter:B:11:0x0068} */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x0135 A:{ExcHandler: IOException | RuntimeException (r1_10 'e' java.lang.Exception), Splitter:B:11:0x0068} */
    /* JADX WARNING: Missing block: B:41:0x0135, code skipped:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:42:0x0136, code skipped:
            android.os.FileUtils.deleteContentsAndDir(r3);
     */
    /* JADX WARNING: Missing block: B:43:0x0139, code skipped:
            throw r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void install(byte[] content, int version) throws IOException {
        this.updateDir.mkdir();
        StringBuilder stringBuilder;
        if (this.updateDir.isDirectory()) {
            int i = 0;
            if (this.updateDir.setReadable(true, false)) {
                File currentSymlink = new File(this.updateDir, "current");
                File file = this.updateDir;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(LOGDIR_PREFIX);
                stringBuilder2.append(String.valueOf(version));
                File newVersion = new File(file, stringBuilder2.toString());
                if (newVersion.exists()) {
                    if (newVersion.getCanonicalPath().equals(currentSymlink.getCanonicalPath())) {
                        writeUpdate(this.updateDir, this.updateVersion, Long.toString((long) version).getBytes());
                        deleteOldLogDirectories();
                        return;
                    }
                    FileUtils.deleteContentsAndDir(newVersion);
                }
                try {
                    newVersion.mkdir();
                    StringBuilder stringBuilder3;
                    if (!newVersion.isDirectory()) {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Unable to make directory ");
                        stringBuilder3.append(newVersion.getCanonicalPath());
                        throw new IOException(stringBuilder3.toString());
                    } else if (newVersion.setReadable(true, false)) {
                        JSONArray logs = new JSONObject(new String(content, StandardCharsets.UTF_8)).getJSONArray("logs");
                        while (i < logs.length()) {
                            installLog(newVersion, logs.getJSONObject(i));
                            i++;
                        }
                        File tempSymlink = new File(this.updateDir, "new_symlink");
                        Os.symlink(newVersion.getCanonicalPath(), tempSymlink.getCanonicalPath());
                        tempSymlink.renameTo(currentSymlink.getAbsoluteFile());
                        String str = TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("CT log directory updated to ");
                        stringBuilder3.append(newVersion.getAbsolutePath());
                        Slog.i(str, stringBuilder3.toString());
                        writeUpdate(this.updateDir, this.updateVersion, Long.toString((long) version).getBytes());
                        deleteOldLogDirectories();
                        return;
                    } else {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Failed to set ");
                        stringBuilder3.append(newVersion.getCanonicalPath());
                        stringBuilder3.append(" readable");
                        throw new IOException(stringBuilder3.toString());
                    }
                } catch (JSONException e) {
                    throw new IOException("Failed to parse logs", e);
                } catch (IOException | RuntimeException e2) {
                } catch (ErrnoException e3) {
                    throw new IOException("Failed to create symlink", e3);
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to set permissions on ");
            stringBuilder.append(this.updateDir.getCanonicalPath());
            throw new IOException(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Unable to make directory ");
        stringBuilder.append(this.updateDir.getCanonicalPath());
        throw new IOException(stringBuilder.toString());
    }

    private void installLog(File directory, JSONObject logObject) throws IOException {
        OutputStreamWriter out;
        try {
            File file = new File(directory, getLogFileName(logObject.getString("key")));
            out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            writeLogEntry(out, "key", logObject.getString("key"));
            writeLogEntry(out, "url", logObject.getString("url"));
            writeLogEntry(out, "description", logObject.getString("description"));
            out.close();
            if (!file.setReadable(true, false)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to set permissions on ");
                stringBuilder.append(file.getCanonicalPath());
                throw new IOException(stringBuilder.toString());
            }
        } catch (JSONException e) {
            throw new IOException("Failed to parse log", e);
        } catch (Throwable th) {
            r3.addSuppressed(th);
        }
    }

    private String getLogFileName(String base64PublicKey) {
        try {
            return HexDump.toHexString(MessageDigest.getInstance("SHA-256").digest(Base64.decode(base64PublicKey, 0)), false);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeLogEntry(OutputStreamWriter out, String key, String value) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(key);
        stringBuilder.append(":");
        stringBuilder.append(value);
        stringBuilder.append("\n");
        out.write(stringBuilder.toString());
    }

    private void deleteOldLogDirectories() throws IOException {
        if (this.updateDir.exists()) {
            final File currentTarget = new File(this.updateDir, "current").getCanonicalFile();
            for (File f : this.updateDir.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return !currentTarget.equals(file) && file.getName().startsWith(CertificateTransparencyLogInstallReceiver.LOGDIR_PREFIX);
                }
            })) {
                FileUtils.deleteContentsAndDir(f);
            }
        }
    }
}
