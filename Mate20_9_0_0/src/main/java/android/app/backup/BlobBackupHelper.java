package android.app.backup;

import android.os.ParcelFileDescriptor;
import android.util.ArrayMap;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public abstract class BlobBackupHelper implements BackupHelper {
    private static final boolean DEBUG = false;
    private static final String TAG = "BlobBackupHelper";
    private final int mCurrentBlobVersion;
    private final String[] mKeys;

    protected abstract void applyRestoredPayload(String str, byte[] bArr);

    protected abstract byte[] getBackupPayload(String str);

    public BlobBackupHelper(int currentBlobVersion, String... keys) {
        this.mCurrentBlobVersion = currentBlobVersion;
        this.mKeys = keys;
    }

    private ArrayMap<String, Long> readOldState(ParcelFileDescriptor oldStateFd) {
        ArrayMap<String, Long> state = new ArrayMap();
        DataInputStream in = new DataInputStream(new FileInputStream(oldStateFd.getFileDescriptor()));
        String str;
        StringBuilder stringBuilder;
        try {
            int version = in.readInt();
            if (version <= this.mCurrentBlobVersion) {
                int numKeys = in.readInt();
                for (int i = 0; i < numKeys; i++) {
                    state.put(in.readUTF(), Long.valueOf(in.readLong()));
                }
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Prior state from unrecognized version ");
                stringBuilder.append(version);
                Log.w(str, stringBuilder.toString());
            }
        } catch (EOFException e) {
            state.clear();
        } catch (Exception e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error examining prior backup state ");
            stringBuilder.append(e2.getMessage());
            Log.e(str, stringBuilder.toString());
            state.clear();
        }
        return state;
    }

    private void writeBackupState(ArrayMap<String, Long> state, ParcelFileDescriptor stateFile) {
        try {
            DataOutputStream out = new DataOutputStream(new FileOutputStream(stateFile.getFileDescriptor()));
            out.writeInt(this.mCurrentBlobVersion);
            int i = 0;
            int N = state != null ? state.size() : 0;
            out.writeInt(N);
            while (i < N) {
                String key = (String) state.keyAt(i);
                long checksum = ((Long) state.valueAt(i)).longValue();
                out.writeUTF(key);
                out.writeLong(checksum);
                i++;
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to write updated state", e);
        }
    }

    private byte[] deflate(byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            ByteArrayOutputStream sink = new ByteArrayOutputStream();
            new DataOutputStream(sink).writeInt(this.mCurrentBlobVersion);
            DeflaterOutputStream out = new DeflaterOutputStream(sink);
            out.write(data);
            out.close();
            return sink.toByteArray();
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to process payload: ");
            stringBuilder.append(e.getMessage());
            Log.w(str, stringBuilder.toString());
            return null;
        }
    }

    private byte[] inflate(byte[] compressedData) {
        byte[] result = null;
        if (compressedData != null) {
            try {
                ByteArrayInputStream source = new ByteArrayInputStream(compressedData);
                int version = new DataInputStream(source).readInt();
                if (version > this.mCurrentBlobVersion) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Saved payload from unrecognized version ");
                    stringBuilder.append(version);
                    Log.w(str, stringBuilder.toString());
                    return null;
                }
                InflaterInputStream in = new InflaterInputStream(source);
                ByteArrayOutputStream inflated = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                while (true) {
                    int read = in.read(buffer);
                    int nRead = read;
                    if (read <= 0) {
                        break;
                    }
                    inflated.write(buffer, 0, nRead);
                }
                in.close();
                inflated.flush();
                result = inflated.toByteArray();
            } catch (IOException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unable to process restored payload: ");
                stringBuilder2.append(e.getMessage());
                Log.w(str2, stringBuilder2.toString());
            }
        }
        return result;
    }

    private long checksum(byte[] buffer) {
        if (buffer != null) {
            try {
                CRC32 crc = new CRC32();
                ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
                byte[] buf = new byte[4096];
                int nRead = 0;
                while (true) {
                    int read = bis.read(buf);
                    nRead = read;
                    if (read < 0) {
                        return crc.getValue();
                    }
                    crc.update(buf, 0, nRead);
                }
            } catch (Exception e) {
            }
        }
        return -1;
    }

    public void performBackup(ParcelFileDescriptor oldStateFd, BackupDataOutput data, ParcelFileDescriptor newStateFd) {
        ArrayMap<String, Long> oldState = readOldState(oldStateFd);
        ArrayMap<String, Long> newState = new ArrayMap();
        try {
            for (String key : this.mKeys) {
                byte[] payload = deflate(getBackupPayload(key));
                long checksum = checksum(payload);
                newState.put(key, Long.valueOf(checksum));
                Long oldChecksum = (Long) oldState.get(key);
                if (oldChecksum == null || checksum != oldChecksum.longValue()) {
                    if (payload != null) {
                        data.writeEntityHeader(key, payload.length);
                        data.writeEntityData(payload, payload.length);
                    } else {
                        data.writeEntityHeader(key, -1);
                    }
                }
            }
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to record notification state: ");
            stringBuilder.append(e.getMessage());
            Log.w(str, stringBuilder.toString());
            newState.clear();
        } catch (Throwable th) {
            writeBackupState(newState, newStateFd);
        }
        writeBackupState(newState, newStateFd);
    }

    public void restoreEntity(BackupDataInputStream data) {
        String str;
        StringBuilder stringBuilder;
        String key = data.getKey();
        int which = 0;
        while (which < this.mKeys.length) {
            try {
                if (key.equals(this.mKeys[which])) {
                    break;
                }
                which++;
            } catch (Exception e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception restoring entity ");
                stringBuilder.append(key);
                stringBuilder.append(" : ");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString());
            }
        }
        if (which >= this.mKeys.length) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unrecognized key ");
            stringBuilder.append(key);
            stringBuilder.append(", ignoring");
            Log.e(str, stringBuilder.toString());
            return;
        }
        byte[] compressed = new byte[data.size()];
        data.read(compressed);
        applyRestoredPayload(key, inflate(compressed));
    }

    public void writeNewStateDescription(ParcelFileDescriptor newState) {
        writeBackupState(null, newState);
    }
}
