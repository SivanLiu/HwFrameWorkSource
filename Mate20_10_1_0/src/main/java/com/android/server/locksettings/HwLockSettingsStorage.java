package com.android.server.locksettings;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.locksettings.LockSettingsStorage;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import huawei.android.os.IHwAntiTheftManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Calendar;

class HwLockSettingsStorage extends LockSettingsStorage {
    private static final int ANTI_THEFT_TYPE_PIN_PARTITION = 4;
    private static final String CONFIG_FRP_PST = SystemProperties.get("ro.frp.pst", "");
    private static final String CONFIG_GMSVERSION = SystemProperties.get("ro.com.google.gmsversion", "");
    private static final String LOCK_EXTEND_PASSWORD_FILE = "gatekeeper.extendpassword.key";
    private static final String SYSTEM_DIRECTORY = "/system/";
    private static final String TABLE = "locksettings";
    private static final String TAG = "HwLockSettingsStorage";
    private FileLogger mFLogger = new FileLogger();
    private IHwAntiTheftManager mHwAntiTheftManager = null;
    private LockSettingsDbReport mLSSDbReport;

    public HwLockSettingsStorage(Context context) {
        super(context);
        this.mLSSDbReport = new LockSettingsDbReport(context);
    }

    private IHwAntiTheftManager getIHwAntiTheftManager() {
        if (this.mHwAntiTheftManager == null) {
            IBinder binder = ServiceManager.getService("hwAntiTheftService");
            if (binder == null) {
                Slog.e(TAG, "getIHwAntiTheftManager binder is null.");
                return this.mHwAntiTheftManager;
            }
            this.mHwAntiTheftManager = IHwAntiTheftManager.Stub.asInterface(binder);
        }
        return this.mHwAntiTheftManager;
    }

    public void writePersistentDataBlock(int persistentType, int userId, int qualityForUi, byte[] payload) {
        if (isHwFrpScheme()) {
            IHwAntiTheftManager iHwAntiTheftManager = getIHwAntiTheftManager();
            if (iHwAntiTheftManager == null) {
                Slog.e(TAG, "writePersistentDataBlock iHwAntiTheftManager is null");
                return;
            }
            Slog.i(TAG, "writePersistentDataBlock persistentType " + persistentType);
            if (persistentType != 0) {
                try {
                    iHwAntiTheftManager.writeAntiTheftDataWithType(PersistentData.toBytes(persistentType, userId, qualityForUi, payload), 4);
                } catch (RemoteException e) {
                    Slog.e(TAG, "writePersistentDataBlock error persistentType " + persistentType);
                }
            } else {
                iHwAntiTheftManager.wipeAntiTheftDataWithType(4);
            }
        } else {
            Slog.i(TAG, "writePersistentDataBlock not hw frp");
            HwLockSettingsStorage.super.writePersistentDataBlock(persistentType, userId, qualityForUi, payload);
        }
    }

    public PersistentData readPersistentDataBlock() {
        if (isHwFrpScheme()) {
            IHwAntiTheftManager iHwAntiTheftManager = getIHwAntiTheftManager();
            if (iHwAntiTheftManager == null) {
                Slog.e(TAG, "readPersistentDataBlock iHwAntiTheftManager is null");
                return PersistentData.NONE;
            }
            try {
                PersistentData data = PersistentData.fromBytes(iHwAntiTheftManager.readAntiTheftDataWithType(4));
                Slog.i(TAG, "readPersistentDataBlock quality " + data.qualityForUi + " type " + data.type);
                return data;
            } catch (RemoteException e) {
                Slog.e(TAG, "readPersistentDataBlock error");
                return PersistentData.NONE;
            }
        } else {
            Slog.i(TAG, "readPersistentDataBlock not hw frp");
            return HwLockSettingsStorage.super.readPersistentDataBlock();
        }
    }

    private boolean isHwFrpScheme() {
        return "".equals(CONFIG_FRP_PST) || "".equals(CONFIG_GMSVERSION);
    }

    public CredentialHash readCredentialHashEx(int userId) {
        CredentialHash passwordHash = readExPasswordHashIfExists(userId);
        if (passwordHash != null) {
            return passwordHash;
        }
        return CredentialHash.createEmptyHash();
    }

    private CredentialHash readExPasswordHashIfExists(int userId) {
        byte[] stored = readFile(getExLockPasswordFilename(userId));
        if (!ArrayUtils.isEmpty(stored)) {
            return CredentialHash.create(stored, 2);
        }
        Slog.i(TAG, "readPatternHash , cannot get any PasswordHash");
        return null;
    }

    /* access modifiers changed from: package-private */
    public void writeCredentialHashEx(CredentialHash hash, int userId) {
        writeFile(getExLockPasswordFilename(userId), hash.hash);
    }

    private String getExLockPasswordFilename(int userId) {
        String dataSystemDirectory = Environment.getDataDirectory().getAbsolutePath() + "/system/";
        if (userId != 0) {
            return new File(Environment.getUserSystemDirectory(userId), LOCK_EXTEND_PASSWORD_FILE).getAbsolutePath();
        }
        return dataSystemDirectory + LOCK_EXTEND_PASSWORD_FILE;
    }

    /* access modifiers changed from: package-private */
    public void deleteExPasswordFile(int userId) {
        File file = new File(getExLockPasswordFilename(userId));
        if (file.exists() && !file.delete()) {
            Slog.e(TAG, "Error delet file ");
        }
    }

    /* access modifiers changed from: package-private */
    public boolean hasSetPassword(int userId) {
        if (new File(getExLockPasswordFilename(userId)).exists()) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public void flog(String tag, String msg) {
        FileLogger fileLogger = this.mFLogger;
        if (fileLogger != null) {
            fileLogger.log(tag, msg);
        }
    }

    private static class FileLogger {
        private static final String CHARSET_ASCII = "US-ASCII";
        private static String FILE_LOG_DIRECTORY = "/data/log/lss_log";
        private static final int MAX_DUMP_DEPTH = 2;
        private static final int MAX_FILE = 2;
        private static final String TAG = "FileLogger";
        private static final String log_file_prefix = "lock_settings_log_0";
        private static final long max_size = 524288;
        private final Calendar mCalendar = Calendar.getInstance();

        public FileLogger() {
            File pFile = new File(FILE_LOG_DIRECTORY);
            if (!pFile.exists() && !pFile.mkdirs()) {
                Slog.i(TAG, "create log directory fail:" + FILE_LOG_DIRECTORY);
            }
        }

        private String getFullFilePath(int idx) {
            return FILE_LOG_DIRECTORY + File.separator + log_file_prefix + Integer.toString(idx + 1) + ".log";
        }

        private RandomAccessFile getTargetFile() {
            try {
                File f0 = new File(getFullFilePath(0));
                File f1 = new File(getFullFilePath(1));
                File target = f0;
                if (f0.exists() && f1.exists()) {
                    target = f0.lastModified() > f1.lastModified() ? f0 : f1;
                }
                long size = target.length();
                if (size >= max_size) {
                    target = target == f0 ? f1 : f0;
                    size = 0;
                }
                RandomAccessFile raf = new RandomAccessFile(target.getCanonicalPath(), "rw");
                if (size == 0) {
                    raf.setLength(0);
                }
                raf.seek(size);
                return raf;
            } catch (FileNotFoundException e) {
                Log.w(TAG, "FileLogerFaile FileNotFoundException");
                closeSafely(null);
                return null;
            } catch (IOException e2) {
                Log.w(TAG, "FileLogerFaile IOException");
                closeSafely(null);
                return null;
            }
        }

        private void closeSafely(RandomAccessFile raf) {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    Log.w(TAG, "Close file fail");
                }
            }
        }

        private boolean write(byte[] content) {
            synchronized (this) {
                RandomAccessFile raf = getTargetFile();
                if (raf == null) {
                    Log.e(TAG, "check raf file fail." + FILE_LOG_DIRECTORY);
                    return false;
                }
                try {
                    raf.write(content);
                    return true;
                } catch (FileNotFoundException e) {
                    Log.w(TAG, "FileLogerFaile FileNotFoundException");
                } catch (IOException e2) {
                    Log.w(TAG, "FileLogerFaile IOException");
                } finally {
                    closeSafely(raf);
                }
            }
            return false;
        }

        private boolean write(StringBuilder sb) {
            sb.append(System.lineSeparator());
            return write(getBytes(sb.toString(), CHARSET_ASCII));
        }

        private byte[] getBytes(String msg, String charSet) {
            try {
                return msg.getBytes(charSet);
            } catch (UnsupportedEncodingException e) {
                Log.w(TAG, "FileLogerFaile UnsupportedEncodingException");
                return msg.getBytes(Charset.defaultCharset());
            }
        }

        private static void intToString(StringBuilder sb, int digit, int num) {
            int i = 1;
            int base = 10;
            while (i < digit) {
                if (num < base) {
                    sb.append("0");
                }
                i++;
                base *= 10;
            }
            sb.append(num);
        }

        private StringBuilder makeBaseInfo(StringBuilder sb, String tag) {
            long millis = System.currentTimeMillis();
            synchronized (this.mCalendar) {
                this.mCalendar.setTimeInMillis(millis);
                intToString(sb, 2, this.mCalendar.get(2) + 1);
                sb.append('-');
                intToString(sb, 2, this.mCalendar.get(5));
                sb.append(' ');
                intToString(sb, 2, this.mCalendar.get(11));
                sb.append(':');
                intToString(sb, 2, this.mCalendar.get(12));
                sb.append(':');
                intToString(sb, 2, this.mCalendar.get(13));
                sb.append('.');
                intToString(sb, 3, this.mCalendar.get(14));
            }
            sb.append(" ");
            sb.append(Process.myPid());
            sb.append(AwarenessInnerConstants.DASH_KEY);
            sb.append(Thread.currentThread().getId());
            sb.append(" ");
            sb.append(tag);
            sb.append(": ");
            return sb;
        }

        public void log(String tag, String message) {
            StringBuilder makeBaseInfo = makeBaseInfo(new StringBuilder(), tag);
            makeBaseInfo.append(message);
            write(makeBaseInfo);
        }
    }

    public void writeKeyValue(SQLiteDatabase db, String key, String value, int userId) {
        HwLockSettingsStorage.super.writeKeyValue(db, key, value, userId);
        this.mLSSDbReport.writeBackItemData(key, value, userId);
        Slog.i(TAG, "writeKeyValue userid " + userId + " key " + key + " callpid " + Binder.getCallingPid() + " callUid " + Binder.getCallingUid());
    }

    public void restoreDataFromXml(SQLiteDatabase db) {
        this.mLSSDbReport.restoreDataFromXml(db);
    }

    public void syncDataToXmlFile(SQLiteDatabase db) {
        this.mLSSDbReport.syncDataToXmlFile(db);
    }

    public void removeUser(int userId) {
        HwLockSettingsStorage.super.removeUser(userId);
        this.mLSSDbReport.removeUserInfo(userId);
    }
}
