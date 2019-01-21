package com.android.server.usb;

import android.app.ActivityManager;
import android.app.StatusBarManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.LocalSocketAddress.Namespace;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.util.Base64;
import android.util.Slog;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.internal.util.dump.DumpUtils;
import com.android.server.FgThread;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Arrays;

public class UsbDebuggingManager {
    private static final String ADBD_SOCKET = "adbd";
    private static final String ADB_DIRECTORY = "misc/adb";
    private static final String ADB_KEYS_FILE = "adb_keys";
    private static final int BUFFER_SIZE = 4096;
    private static final boolean DEBUG = false;
    private static final String TAG = "UsbDebuggingManager";
    private boolean mAdbEnabled = false;
    private final Context mContext;
    private String mFingerprints;
    private final Handler mHandler = new UsbDebuggingHandler(FgThread.get().getLooper());
    private StatusBarManager mStatusBarManager;
    private UsbDebuggingThread mThread;

    class UsbDebuggingHandler extends Handler {
        private static final int MESSAGE_ADB_ALLOW = 3;
        private static final int MESSAGE_ADB_CLEAR = 6;
        private static final int MESSAGE_ADB_CONFIRM = 5;
        private static final int MESSAGE_ADB_DENY = 4;
        private static final int MESSAGE_ADB_DISABLED = 2;
        private static final int MESSAGE_ADB_ENABLED = 1;

        public UsbDebuggingHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            String key;
            String str;
            switch (msg.what) {
                case 1:
                    if (!UsbDebuggingManager.this.mAdbEnabled) {
                        UsbDebuggingManager.this.mAdbEnabled = true;
                        UsbDebuggingManager.this.mThread = new UsbDebuggingThread();
                        UsbDebuggingManager.this.mThread.start();
                        return;
                    }
                    return;
                case 2:
                    if (UsbDebuggingManager.this.mAdbEnabled) {
                        UsbDebuggingManager.this.mAdbEnabled = false;
                        if (UsbDebuggingManager.this.mThread != null) {
                            UsbDebuggingManager.this.mThread.stopListening();
                            UsbDebuggingManager.this.mThread = null;
                            return;
                        }
                        return;
                    }
                    return;
                case 3:
                    key = (String) msg.obj;
                    String fingerprints = UsbDebuggingManager.this.getFingerprints(key);
                    if (fingerprints.equals(UsbDebuggingManager.this.mFingerprints)) {
                        if (msg.arg1 == 1) {
                            UsbDebuggingManager.this.writeKey(key);
                        }
                        if (UsbDebuggingManager.this.mThread != null) {
                            UsbDebuggingManager.this.mThread.sendResponse("OK");
                            return;
                        }
                        return;
                    }
                    str = UsbDebuggingManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Fingerprints do not match. Got ");
                    stringBuilder.append(fingerprints);
                    stringBuilder.append(", expected ");
                    stringBuilder.append(UsbDebuggingManager.this.mFingerprints);
                    Slog.e(str, stringBuilder.toString());
                    return;
                case 4:
                    if (UsbDebuggingManager.this.mThread != null) {
                        UsbDebuggingManager.this.mThread.sendResponse("NO");
                        return;
                    }
                    return;
                case 5:
                    if ("trigger_restart_min_framework".equals(SystemProperties.get("vold.decrypt"))) {
                        Slog.d(UsbDebuggingManager.TAG, "Deferring adb confirmation until after vold decrypt");
                        if (UsbDebuggingManager.this.mThread != null) {
                            UsbDebuggingManager.this.mThread.sendResponse("NO");
                            return;
                        }
                        return;
                    } else if (UsbDebuggingManager.this.mContext == null || UsbDebuggingManager.isDeviceProvisioned(UsbDebuggingManager.this.mContext)) {
                        key = msg.obj;
                        str = UsbDebuggingManager.this.getFingerprints(key);
                        if (!BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(str)) {
                            UsbDebuggingManager.this.mFingerprints = str;
                            UsbDebuggingManager.this.startConfirmation(key, UsbDebuggingManager.this.mFingerprints);
                            return;
                        } else if (UsbDebuggingManager.this.mThread != null) {
                            UsbDebuggingManager.this.mThread.sendResponse("NO");
                            return;
                        } else {
                            return;
                        }
                    } else {
                        Slog.d(UsbDebuggingManager.TAG, "Deferring adb confirmation until device is provisioned");
                        if (UsbDebuggingManager.this.mThread != null) {
                            UsbDebuggingManager.this.mThread.sendResponse("NO");
                            return;
                        }
                        return;
                    }
                case 6:
                    UsbDebuggingManager.this.deleteKeyFile();
                    return;
                default:
                    return;
            }
        }
    }

    class UsbDebuggingThread extends Thread {
        private InputStream mInputStream;
        private OutputStream mOutputStream;
        private LocalSocket mSocket;
        private boolean mStopped;

        UsbDebuggingThread() {
            super(UsbDebuggingManager.TAG);
        }

        /* JADX WARNING: Missing block: B:12:?, code skipped:
            listenToSocket();
     */
        /* JADX WARNING: Missing block: B:15:0x0012, code skipped:
            android.os.SystemClock.sleep(1000);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            while (true) {
                synchronized (this) {
                    if (this.mStopped) {
                        return;
                    }
                    try {
                        openSocketLocked();
                    } catch (Exception e) {
                    }
                }
            }
            while (true) {
            }
        }

        private void openSocketLocked() throws IOException {
            try {
                LocalSocketAddress address = new LocalSocketAddress(UsbDebuggingManager.ADBD_SOCKET, Namespace.RESERVED);
                this.mInputStream = null;
                this.mSocket = new LocalSocket();
                this.mSocket.connect(address);
                this.mOutputStream = this.mSocket.getOutputStream();
                this.mInputStream = this.mSocket.getInputStream();
            } catch (IOException ioe) {
                closeSocketLocked();
                throw ioe;
            }
        }

        private void listenToSocket() throws IOException {
            try {
                byte[] buffer = new byte[4096];
                while (true) {
                    int count = this.mInputStream.read(buffer);
                    String str;
                    StringBuilder stringBuilder;
                    if (count < 0) {
                        break;
                    } else if (buffer[0] == (byte) 80 && buffer[1] == (byte) 75) {
                        String key = new String(Arrays.copyOfRange(buffer, 2, count));
                        str = UsbDebuggingManager.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Received public key: ");
                        stringBuilder.append(key);
                        Slog.d(str, stringBuilder.toString());
                        Message msg = UsbDebuggingManager.this.mHandler.obtainMessage(5);
                        msg.obj = key;
                        UsbDebuggingManager.this.mHandler.sendMessage(msg);
                    } else {
                        str = UsbDebuggingManager.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Wrong message: ");
                        stringBuilder.append(new String(Arrays.copyOfRange(buffer, 0, 2)));
                        Slog.e(str, stringBuilder.toString());
                    }
                }
                synchronized (this) {
                    closeSocketLocked();
                }
            } catch (Throwable th) {
                synchronized (this) {
                    closeSocketLocked();
                }
            }
        }

        private void closeSocketLocked() {
            try {
                if (this.mOutputStream != null) {
                    this.mOutputStream.close();
                    this.mOutputStream = null;
                }
            } catch (IOException e) {
                String str = UsbDebuggingManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed closing output stream: ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
            try {
                if (this.mSocket != null) {
                    this.mSocket.close();
                    this.mSocket = null;
                }
            } catch (IOException ex) {
                String str2 = UsbDebuggingManager.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed closing socket: ");
                stringBuilder2.append(ex);
                Slog.e(str2, stringBuilder2.toString());
            }
        }

        void stopListening() {
            synchronized (this) {
                this.mStopped = true;
                closeSocketLocked();
            }
        }

        void sendResponse(String msg) {
            synchronized (this) {
                if (!(this.mStopped || this.mOutputStream == null)) {
                    try {
                        this.mOutputStream.write(msg.getBytes());
                    } catch (IOException ex) {
                        Slog.e(UsbDebuggingManager.TAG, "Failed to write response:", ex);
                    }
                }
            }
        }
    }

    public UsbDebuggingManager(Context context) {
        this.mContext = context;
        this.mStatusBarManager = (StatusBarManager) context.getSystemService("statusbar");
    }

    private static boolean isDeviceProvisioned(Context context) {
        return Global.getInt(context.getContentResolver(), "device_provisioned", 0) == 1;
    }

    private String getFingerprints(String key) {
        String hex = "0123456789ABCDEF";
        StringBuilder sb = new StringBuilder();
        if (key == null) {
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        try {
            int i = 0;
            try {
                byte[] digest = MessageDigest.getInstance("MD5").digest(Base64.decode(key.split("\\s+")[0].getBytes(), 0));
                while (i < digest.length) {
                    sb.append(hex.charAt((digest[i] >> 4) & 15));
                    sb.append(hex.charAt(digest[i] & 15));
                    if (i < digest.length - 1) {
                        sb.append(":");
                    }
                    i++;
                }
                return sb.toString();
            } catch (IllegalArgumentException e) {
                Slog.e(TAG, "error doing base64 decoding", e);
                return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
        } catch (Exception ex) {
            Slog.e(TAG, "Error getting digester", ex);
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
    }

    private void startConfirmation(String key, String fingerprints) {
        UserInfo userInfo = UserManager.get(this.mContext).getUserInfo(ActivityManager.getCurrentUser());
        String componentString = (userInfo.isAdmin() || userInfo.isRepairMode()) ? Resources.getSystem().getString(17039774) : Resources.getSystem().getString(17039775);
        ComponentName componentName = ComponentName.unflattenFromString(componentString);
        if (!startConfirmationActivity(componentName, userInfo.getUserHandle(), key, fingerprints) && !startConfirmationService(componentName, userInfo.getUserHandle(), key, fingerprints)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unable to start customAdbPublicKeyConfirmation[SecondaryUser]Component ");
            stringBuilder.append(componentString);
            stringBuilder.append(" as an Activity or a Service");
            Slog.e(str, stringBuilder.toString());
        }
    }

    private boolean startConfirmationActivity(ComponentName componentName, UserHandle userHandle, String key, String fingerprints) {
        PackageManager packageManager = this.mContext.getPackageManager();
        Intent intent = createConfirmationIntent(componentName, key, fingerprints);
        intent.addFlags(268435456);
        if (packageManager.resolveActivity(intent, 65536) != null) {
            try {
                if (this.mStatusBarManager != null) {
                    this.mStatusBarManager.collapsePanels();
                }
                this.mContext.startActivityAsUser(intent, userHandle);
                return true;
            } catch (ActivityNotFoundException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unable to start adb whitelist activity: ");
                stringBuilder.append(componentName);
                Slog.e(str, stringBuilder.toString(), e);
            }
        }
        return false;
    }

    private boolean startConfirmationService(ComponentName componentName, UserHandle userHandle, String key, String fingerprints) {
        try {
            if (this.mContext.startServiceAsUser(createConfirmationIntent(componentName, key, fingerprints), userHandle) != null) {
                return true;
            }
        } catch (SecurityException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unable to start adb whitelist service: ");
            stringBuilder.append(componentName);
            Slog.e(str, stringBuilder.toString(), e);
        }
        return false;
    }

    private Intent createConfirmationIntent(ComponentName componentName, String key, String fingerprints) {
        Intent intent = new Intent();
        intent.setClassName(componentName.getPackageName(), componentName.getClassName());
        intent.putExtra("key", key);
        intent.putExtra("fingerprints", fingerprints);
        return intent;
    }

    private File getUserKeyFile() {
        File adbDir = new File(Environment.getDataDirectory(), ADB_DIRECTORY);
        if (adbDir.exists()) {
            return new File(adbDir, ADB_KEYS_FILE);
        }
        Slog.e(TAG, "ADB data directory does not exist");
        return null;
    }

    private void writeKey(String key) {
        try {
            File keyFile = getUserKeyFile();
            if (keyFile != null) {
                if (!keyFile.exists()) {
                    keyFile.createNewFile();
                    FileUtils.setPermissions(keyFile.toString(), 416, -1, -1);
                }
                FileOutputStream fo = new FileOutputStream(keyFile, true);
                fo.write(key.getBytes());
                fo.write(10);
                fo.close();
            }
        } catch (IOException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error writing key:");
            stringBuilder.append(ex);
            Slog.e(str, stringBuilder.toString());
        }
    }

    private void deleteKeyFile() {
        File keyFile = getUserKeyFile();
        if (keyFile != null) {
            keyFile.delete();
        }
    }

    public void setAdbEnabled(boolean enabled) {
        int i;
        Handler handler = this.mHandler;
        if (enabled) {
            i = 1;
        } else {
            i = 2;
        }
        handler.sendEmptyMessage(i);
    }

    public void allowUsbDebugging(boolean alwaysAllow, String publicKey) {
        Message msg = this.mHandler.obtainMessage(3);
        msg.arg1 = alwaysAllow;
        msg.obj = publicKey;
        this.mHandler.sendMessage(msg);
    }

    public void denyUsbDebugging() {
        this.mHandler.sendEmptyMessage(4);
    }

    public void clearUsbDebuggingKeys() {
        this.mHandler.sendEmptyMessage(6);
    }

    public void dump(DualDumpOutputStream dump, String idName, long id) {
        long token = dump.start(idName, id);
        dump.write("connected_to_adb", 1133871366145L, this.mThread != null);
        DumpUtils.writeStringIfNotNull(dump, "last_key_received", 1138166333442L, this.mFingerprints);
        try {
            dump.write("user_keys", 1138166333443L, FileUtils.readTextFile(new File("/data/misc/adb/adb_keys"), 0, null));
        } catch (IOException e) {
            Slog.e(TAG, "Cannot read user keys", e);
        }
        try {
            dump.write("system_keys", 1138166333444L, FileUtils.readTextFile(new File("/adb_keys"), 0, null));
        } catch (IOException e2) {
            Slog.e(TAG, "Cannot read system keys", e2);
        }
        dump.end(token);
    }
}
