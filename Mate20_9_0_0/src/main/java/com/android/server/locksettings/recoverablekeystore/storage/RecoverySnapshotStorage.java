package com.android.server.locksettings.recoverablekeystore.storage;

import android.os.Environment;
import android.security.keystore.recovery.KeyChainSnapshot;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.locksettings.recoverablekeystore.serialization.KeyChainSnapshotDeserializer;
import com.android.server.locksettings.recoverablekeystore.serialization.KeyChainSnapshotParserException;
import com.android.server.locksettings.recoverablekeystore.serialization.KeyChainSnapshotSerializer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.util.Locale;

public class RecoverySnapshotStorage {
    private static final String ROOT_PATH = "system";
    private static final String STORAGE_PATH = "recoverablekeystore/snapshots/";
    private static final String TAG = "RecoverySnapshotStorage";
    @GuardedBy("this")
    private final SparseArray<KeyChainSnapshot> mSnapshotByUid = new SparseArray();
    private final File rootDirectory;

    public static RecoverySnapshotStorage newInstance() {
        return new RecoverySnapshotStorage(new File(Environment.getDataDirectory(), ROOT_PATH));
    }

    @VisibleForTesting
    public RecoverySnapshotStorage(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    /* JADX WARNING: Removed duplicated region for block: B:5:0x000a A:{Splitter: B:3:0x0006, ExcHandler: java.io.IOException (r0_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:5:0x000a, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:7:?, code:
            android.util.Log.e(TAG, java.lang.String.format(java.util.Locale.US, "Error persisting snapshot for %d to disk", new java.lang.Object[]{java.lang.Integer.valueOf(r8)}), r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void put(int uid, KeyChainSnapshot snapshot) {
        this.mSnapshotByUid.put(uid, snapshot);
        try {
            writeToDisk(uid, snapshot);
        } catch (Exception e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:10:0x0013 A:{Splitter: B:6:0x000d, ExcHandler: java.io.IOException (r1_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:10:0x0013, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:12:?, code:
            android.util.Log.e(TAG, java.lang.String.format(java.util.Locale.US, "Error reading snapshot for %d from disk", new java.lang.Object[]{java.lang.Integer.valueOf(r9)}), r1);
     */
    /* JADX WARNING: Missing block: B:15:0x002d, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized KeyChainSnapshot get(int uid) {
        KeyChainSnapshot snapshot = (KeyChainSnapshot) this.mSnapshotByUid.get(uid);
        if (snapshot != null) {
            return snapshot;
        }
        try {
            return readFromDisk(uid);
        } catch (Exception e) {
        }
    }

    public synchronized void remove(int uid) {
        this.mSnapshotByUid.remove(uid);
        getSnapshotFile(uid).delete();
    }

    /* JADX WARNING: Removed duplicated region for block: B:16:0x001a A:{Splitter: B:1:0x0004, ExcHandler: java.io.IOException (r1_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:16:0x001a, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:17:0x001b, code:
            r0.delete();
     */
    /* JADX WARNING: Missing block: B:18:0x001e, code:
            throw r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void writeToDisk(int uid, KeyChainSnapshot snapshot) throws IOException, CertificateEncodingException {
        File snapshotFile = getSnapshotFile(uid);
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(snapshotFile);
            KeyChainSnapshotSerializer.serialize(snapshot, fileOutputStream);
            $closeResource(null, fileOutputStream);
        } catch (Exception e) {
        } catch (Throwable th) {
            $closeResource(r2, fileOutputStream);
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    /* JADX WARNING: Removed duplicated region for block: B:16:0x001a A:{Splitter: B:1:0x0004, ExcHandler: java.io.IOException (r1_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:16:0x001a, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:17:0x001b, code:
            r0.delete();
     */
    /* JADX WARNING: Missing block: B:18:0x001e, code:
            throw r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private KeyChainSnapshot readFromDisk(int uid) throws IOException, KeyChainSnapshotParserException {
        File snapshotFile = getSnapshotFile(uid);
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(snapshotFile);
            KeyChainSnapshot deserialize = KeyChainSnapshotDeserializer.deserialize(fileInputStream);
            $closeResource(null, fileInputStream);
            return deserialize;
        } catch (Exception e) {
        } catch (Throwable th) {
            $closeResource(r2, fileInputStream);
        }
    }

    private File getSnapshotFile(int uid) {
        return new File(getStorageFolder(), getSnapshotFileName(uid));
    }

    private String getSnapshotFileName(int uid) {
        return String.format(Locale.US, "%d.xml", new Object[]{Integer.valueOf(uid)});
    }

    private File getStorageFolder() {
        File folder = new File(this.rootDirectory, STORAGE_PATH);
        folder.mkdirs();
        return folder;
    }
}
