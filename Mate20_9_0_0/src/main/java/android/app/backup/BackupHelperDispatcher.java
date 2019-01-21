package android.app.backup;

import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.TreeMap;

public class BackupHelperDispatcher {
    private static final String TAG = "BackupHelperDispatcher";
    TreeMap<String, BackupHelper> mHelpers = new TreeMap();

    private static class Header {
        int chunkSize;
        String keyPrefix;

        private Header() {
        }
    }

    private static native int allocateHeader_native(Header header, FileDescriptor fileDescriptor);

    private static native int readHeader_native(Header header, FileDescriptor fileDescriptor);

    private static native int skipChunk_native(FileDescriptor fileDescriptor, int i);

    private static native int writeHeader_native(Header header, FileDescriptor fileDescriptor, int i);

    public void addHelper(String keyPrefix, BackupHelper helper) {
        this.mHelpers.put(keyPrefix, helper);
    }

    public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
        String str;
        StringBuilder stringBuilder;
        Header header = new Header();
        TreeMap<String, BackupHelper> helpers = (TreeMap) this.mHelpers.clone();
        FileDescriptor oldStateFD;
        if (oldState != null) {
            oldStateFD = oldState.getFileDescriptor();
            while (true) {
                int readHeader_native = readHeader_native(header, oldStateFD);
                int err = readHeader_native;
                if (readHeader_native < 0) {
                    break;
                } else if (err == 0) {
                    BackupHelper helper = (BackupHelper) helpers.get(header.keyPrefix);
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("handling existing helper '");
                    stringBuilder.append(header.keyPrefix);
                    stringBuilder.append("' ");
                    stringBuilder.append(helper);
                    Log.d(str, stringBuilder.toString());
                    if (helper != null) {
                        doOneBackup(oldState, data, newState, header, helper);
                        helpers.remove(header.keyPrefix);
                    } else {
                        skipChunk_native(oldStateFD, header.chunkSize);
                    }
                }
            }
        } else {
            oldStateFD = null;
        }
        for (Entry<String, BackupHelper> entry : helpers.entrySet()) {
            header.keyPrefix = (String) entry.getKey();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("handling new helper '");
            stringBuilder.append(header.keyPrefix);
            stringBuilder.append("'");
            Log.d(str, stringBuilder.toString());
            doOneBackup(oldState, data, newState, header, (BackupHelper) entry.getValue());
        }
    }

    private void doOneBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState, Header header, BackupHelper helper) throws IOException {
        FileDescriptor newStateFD = newState.getFileDescriptor();
        int pos = allocateHeader_native(header, newStateFD);
        if (pos >= 0) {
            data.setKeyPrefix(header.keyPrefix);
            helper.performBackup(oldState, data, newState);
            int err = writeHeader_native(header, newStateFD, pos);
            if (err != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("writeHeader_native failed (error ");
                stringBuilder.append(err);
                stringBuilder.append(")");
                throw new IOException(stringBuilder.toString());
            }
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("allocateHeader_native failed (error ");
        stringBuilder2.append(pos);
        stringBuilder2.append(")");
        throw new IOException(stringBuilder2.toString());
    }

    public void performRestore(BackupDataInput input, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
        boolean alreadyComplained = false;
        BackupDataInputStream stream = new BackupDataInputStream(input);
        while (input.readNextHeader()) {
            String rawKey = input.getKey();
            int pos = rawKey.indexOf(58);
            if (pos > 0) {
                BackupHelper helper = (BackupHelper) this.mHelpers.get(rawKey.substring(null, pos));
                if (helper != null) {
                    stream.dataSize = input.getDataSize();
                    stream.key = rawKey.substring(pos + 1);
                    helper.restoreEntity(stream);
                } else if (!alreadyComplained) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Couldn't find helper for: '");
                    stringBuilder.append(rawKey);
                    stringBuilder.append("'");
                    Log.w(str, stringBuilder.toString());
                    alreadyComplained = true;
                }
            } else if (!alreadyComplained) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Entity with no prefix: '");
                stringBuilder2.append(rawKey);
                stringBuilder2.append("'");
                Log.w(str2, stringBuilder2.toString());
                alreadyComplained = true;
            }
            input.skipEntityData();
        }
        for (BackupHelper helper2 : this.mHelpers.values()) {
            helper2.writeNewStateDescription(newState);
        }
    }
}
