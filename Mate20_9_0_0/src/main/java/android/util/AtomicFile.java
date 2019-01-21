package android.util;

import android.os.FileUtils;
import android.os.SystemClock;
import android.telephony.ims.ImsReasonInfo;
import com.android.internal.logging.EventLogTags;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.Consumer;
import libcore.io.IoUtils;

public class AtomicFile {
    private final File mBackupName;
    private final File mBaseName;
    private final String mCommitTag;
    private long mStartTime;

    public AtomicFile(File baseName) {
        this(baseName, null);
    }

    public AtomicFile(File baseName, String commitTag) {
        this.mBaseName = baseName;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(baseName.getPath());
        stringBuilder.append(".bak");
        this.mBackupName = new File(stringBuilder.toString());
        this.mCommitTag = commitTag;
    }

    public File getBaseFile() {
        return this.mBaseName;
    }

    public void delete() {
        this.mBaseName.delete();
        this.mBackupName.delete();
    }

    public FileOutputStream startWrite() throws IOException {
        return startWrite(this.mCommitTag != null ? SystemClock.uptimeMillis() : 0);
    }

    public FileOutputStream startWrite(long startTime) throws IOException {
        this.mStartTime = startTime;
        if (this.mBaseName.exists()) {
            if (this.mBackupName.exists()) {
                this.mBaseName.delete();
            } else if (!this.mBaseName.renameTo(this.mBackupName)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Couldn't rename file ");
                stringBuilder.append(this.mBaseName);
                stringBuilder.append(" to backup file ");
                stringBuilder.append(this.mBackupName);
                Log.w("AtomicFile", stringBuilder.toString());
            }
        }
        try {
            return new FileOutputStream(this.mBaseName);
        } catch (FileNotFoundException e) {
            File parent = this.mBaseName.getParentFile();
            if (parent.mkdirs()) {
                FileUtils.setPermissions(parent.getPath(), (int) ImsReasonInfo.CODE_LOW_BATTERY, -1, -1);
                try {
                    return new FileOutputStream(this.mBaseName);
                } catch (FileNotFoundException e2) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Couldn't create ");
                    stringBuilder2.append(this.mBaseName);
                    throw new IOException(stringBuilder2.toString());
                }
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Couldn't create directory ");
            stringBuilder3.append(this.mBaseName);
            throw new IOException(stringBuilder3.toString());
        }
    }

    public void finishWrite(FileOutputStream str) {
        if (str != null) {
            FileUtils.sync(str);
            try {
                str.close();
                this.mBackupName.delete();
            } catch (IOException e) {
                Log.w("AtomicFile", "finishWrite: Got exception:", e);
            }
            if (this.mCommitTag != null) {
                EventLogTags.writeCommitSysConfigFile(this.mCommitTag, SystemClock.uptimeMillis() - this.mStartTime);
            }
        }
    }

    public void failWrite(FileOutputStream str) {
        if (str != null) {
            FileUtils.sync(str);
            try {
                str.close();
                this.mBaseName.delete();
                this.mBackupName.renameTo(this.mBaseName);
            } catch (IOException e) {
                Log.w("AtomicFile", "failWrite: Got exception:", e);
            }
        }
    }

    @Deprecated
    public void truncate() throws IOException {
        try {
            FileOutputStream fos = new FileOutputStream(this.mBaseName);
            FileUtils.sync(fos);
            fos.close();
        } catch (FileNotFoundException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't append ");
            stringBuilder.append(this.mBaseName);
            throw new IOException(stringBuilder.toString());
        } catch (IOException e2) {
        }
    }

    @Deprecated
    public FileOutputStream openAppend() throws IOException {
        try {
            return new FileOutputStream(this.mBaseName, true);
        } catch (FileNotFoundException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't append ");
            stringBuilder.append(this.mBaseName);
            throw new IOException(stringBuilder.toString());
        }
    }

    public FileInputStream openRead() throws FileNotFoundException {
        if (this.mBackupName.exists()) {
            this.mBaseName.delete();
            this.mBackupName.renameTo(this.mBaseName);
        }
        return new FileInputStream(this.mBaseName);
    }

    public boolean exists() {
        return this.mBaseName.exists() || this.mBackupName.exists();
    }

    public long getLastModifiedTime() {
        if (this.mBackupName.exists()) {
            return this.mBackupName.lastModified();
        }
        return this.mBaseName.lastModified();
    }

    public byte[] readFully() throws IOException {
        FileInputStream stream = openRead();
        int pos = 0;
        try {
            byte[] data = new byte[stream.available()];
            while (true) {
                int amt = stream.read(data, pos, data.length - pos);
                if (amt <= 0) {
                    break;
                }
                pos += amt;
                int avail = stream.available();
                if (avail > data.length - pos) {
                    byte[] newData = new byte[(pos + avail)];
                    System.arraycopy(data, 0, newData, 0, pos);
                    data = newData;
                }
            }
            return data;
        } finally {
            stream.close();
        }
    }

    public void write(Consumer<FileOutputStream> writeContent) {
        FileOutputStream out = null;
        try {
            out = startWrite();
            writeContent.accept(out);
            finishWrite(out);
            IoUtils.closeQuietly(out);
        } catch (Throwable th) {
            IoUtils.closeQuietly(out);
        }
    }
}
