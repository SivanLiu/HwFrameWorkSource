package android.app.backup;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.File;
import java.io.FileDescriptor;

class FileBackupHelperBase {
    private static final String TAG = "FileBackupHelperBase";
    Context mContext;
    boolean mExceptionLogged;
    long mPtr = ctor();

    private static native long ctor();

    private static native void dtor(long j);

    private static native int performBackup_native(FileDescriptor fileDescriptor, long j, FileDescriptor fileDescriptor2, String[] strArr, String[] strArr2);

    private static native int writeFile_native(long j, String str, long j2);

    private static native int writeSnapshot_native(long j, FileDescriptor fileDescriptor);

    FileBackupHelperBase(Context context) {
        this.mContext = context;
    }

    protected void finalize() throws Throwable {
        try {
            dtor(this.mPtr);
        } finally {
            super.finalize();
        }
    }

    static void performBackup_checked(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState, String[] files, String[] keys) {
        if (files.length != 0) {
            StringBuilder stringBuilder;
            int length = files.length;
            int i = 0;
            while (i < length) {
                String f = files[i];
                if (f.charAt(0) == '/') {
                    i++;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("files must have all absolute paths: ");
                    stringBuilder.append(f);
                    throw new RuntimeException(stringBuilder.toString());
                }
            }
            if (files.length == keys.length) {
                FileDescriptor oldStateFd = oldState != null ? oldState.getFileDescriptor() : null;
                FileDescriptor newStateFd = newState.getFileDescriptor();
                if (newStateFd != null) {
                    i = performBackup_native(oldStateFd, data.mBackupWriter, newStateFd, files, keys);
                    if (i != 0) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Backup failed 0x");
                        stringBuilder2.append(Integer.toHexString(i));
                        throw new RuntimeException(stringBuilder2.toString());
                    }
                    return;
                }
                throw new NullPointerException();
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("files.length=");
            stringBuilder.append(files.length);
            stringBuilder.append(" keys.length=");
            stringBuilder.append(keys.length);
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    boolean writeFile(File f, BackupDataInputStream in) {
        f.getParentFile().mkdirs();
        int result = writeFile_native(this.mPtr, f.getAbsolutePath(), in.mData.mBackupReader);
        if (!(result == 0 || this.mExceptionLogged)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed restoring file '");
            stringBuilder.append(f);
            stringBuilder.append("' for app '");
            stringBuilder.append(this.mContext.getPackageName());
            stringBuilder.append("' result=0x");
            stringBuilder.append(Integer.toHexString(result));
            Log.e(str, stringBuilder.toString());
            this.mExceptionLogged = true;
        }
        if (result == 0) {
            return true;
        }
        return false;
    }

    public void writeNewStateDescription(ParcelFileDescriptor fd) {
        int result = writeSnapshot_native(this.mPtr, fd.getFileDescriptor());
    }

    boolean isKeyInList(String key, String[] list) {
        for (String s : list) {
            if (s.equals(key)) {
                return true;
            }
        }
        return false;
    }
}
