package huawei.android.app;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.LocalSocketAddress.Namespace;
import android.os.SystemProperties;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class HwCustEmergDataManagerImpl extends HwCustEmergDataManager {
    private static final String CONTACTS_PROVIDER_PATH = "/data/data/com.android.providers.contacts";
    private static final String CONTACTS_PROVIDER_PATH_BK = "/data_bk/data/com.android.providers.contacts";
    private static final String EMERGENCY_CRASH_STATE = "2";
    private static final String EMERGENCY_MOUNT_STATE = "1";
    private static final String EMERGENCY_PKG_NAME = "EmergencyData.apk";
    private static final String EMERGENCY_STATE_PROP_NAME = "sys.emergency.mountdata";
    private static boolean HWFLOW = false;
    private static final boolean HWLOGW_E = true;
    private static final String PKGXML_PATH = "/data/system/packages.xml";
    private static final String PKGXML_PATH_BK = "/data_bk/system/packages.xml";
    private static final String TAG = "HwCustEmergDataManager";
    private static final String TAG_FLOW = "HwCustEmergDataManager_FLOW";
    private static final String TAG_INIT = "HwCustEmergDataManager_INIT";
    private static final String TELEPHONY_PROVIDER_PATH = "/data/data/com.android.providers.telephony";
    private static final String TELEPHONY_PROVIDER_PATH_BK = "/data_bk/data/com.android.providers.telephony";
    private static boolean localLOGV;

    private static class FileBackup {
        private static final String COPY = "copy ";
        private static boolean HWFLOW = false;
        private static final boolean HWLOGW_E = true;
        private static final int MAXBUFLENGTH = 8448;
        private static final int MAX_LENGTH = 8192;
        private static final int ONEINTLENGTH = 4;
        private static final int STRINGLENGTH = 256;
        private static final String TAG = "FileBackup";
        private static final String TAG_FLOW = "FileBackup_FLOW";
        private static final String TAG_INIT = "FileBackup_INIT";
        private static boolean localLOGV;
        private InputStream mIn;
        private OutputStream mOut;
        private LocalSocket mSocket;

        static {
            boolean z = Log.HWINFO;
            boolean z2 = HWLOGW_E;
            z = (z || (Log.HWModuleLog && Log.isLoggable(TAG, ONEINTLENGTH))) ? HWLOGW_E : false;
            HWFLOW = z;
            if (!(Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3)))) {
                z2 = false;
            }
            localLOGV = z2;
        }

        private final boolean init() {
            if (connectSocket()) {
                if (HWFLOW) {
                    Log.i(TAG_FLOW, "connectSocket success");
                }
                return HWLOGW_E;
            }
            Log.e(TAG, "connectSocket failed");
            return false;
        }

        private final boolean connectSocket() {
            if (this.mSocket != null) {
                return HWLOGW_E;
            }
            if (HWFLOW) {
                Log.i(TAG_FLOW, "connecting...");
            }
            try {
                this.mSocket = new LocalSocket();
                this.mSocket.connect(new LocalSocketAddress("filebackup", Namespace.RESERVED));
                this.mIn = this.mSocket.getInputStream();
                this.mOut = this.mSocket.getOutputStream();
                return HWLOGW_E;
            } catch (Exception ex) {
                Log.e(TAG, "connecting error : ", ex);
                disconnectSocket();
                return false;
            }
        }

        private final void disconnectSocket() {
            if (HWFLOW) {
                Log.i(TAG_FLOW, "disconnecting...");
            }
            try {
                if (this.mSocket != null) {
                    this.mSocket.close();
                    this.mSocket = null;
                }
                if (this.mIn != null) {
                    this.mIn.close();
                    this.mIn = null;
                }
                if (this.mOut != null) {
                    this.mOut.close();
                    this.mOut = null;
                }
            } catch (Exception ex) {
                Log.e(TAG, "disconnectSocket error : ", ex);
            }
        }

        private final int writeCommand(String command, byte[] buf) {
            try {
                byte[] cmd = command.getBytes("UTF-8");
                int len = cmd.length;
                if (len < 1 || len > MAXBUFLENGTH) {
                    return 0;
                }
                buf[0] = (byte) (len & 255);
                buf[1] = (byte) ((len >> 8) & 255);
                try {
                    this.mOut.write(buf, 0, 2);
                    this.mOut.write(cmd, 0, len);
                    this.mOut.flush();
                    return readReply(buf);
                } catch (Exception ex) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("write error! command : ");
                    stringBuilder.append(command);
                    Log.e(str, stringBuilder.toString(), ex);
                    disconnectSocket();
                    return 0;
                }
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "writeCommand err : ", e);
                return 0;
            }
        }

        private int readReply(byte[] buf) {
            if (readBytes(buf, 2)) {
                int buflen = (buf[0] & 255) | ((buf[1] & 255) << 8);
                String str;
                StringBuilder stringBuilder;
                if (buflen < 1 || buflen > MAXBUFLENGTH) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("invalid reply length (");
                    stringBuilder.append(buflen);
                    stringBuilder.append(")");
                    Log.e(str, stringBuilder.toString());
                    disconnectSocket();
                    return 0;
                } else if (readBytes(buf, buflen)) {
                    return buflen;
                } else {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("!readBytes(buf,");
                    stringBuilder.append(buflen);
                    stringBuilder.append(") error");
                    Log.e(str, stringBuilder.toString());
                    return 0;
                }
            }
            Log.e(TAG, "!readBytes(buf, 2) error");
            return 0;
        }

        private boolean readBytes(byte[] buffer, int len) {
            int off = 0;
            if (len < 0) {
                return false;
            }
            while (off != len) {
                try {
                    int count = this.mIn.read(buffer, off, len - off);
                    if (count <= 0) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("read error ");
                        stringBuilder.append(count);
                        Log.e(str, stringBuilder.toString());
                        break;
                    }
                    off += count;
                } catch (IOException ex) {
                    Log.e(TAG, "read exception", ex);
                }
            }
            if (off == len) {
                return HWLOGW_E;
            }
            disconnectSocket();
            Log.e(TAG, "read bytes error, off != len");
            return false;
        }

        private int bytesToInt(byte[] intByte) {
            int fromByte = 0;
            for (int i = 0; i < ONEINTLENGTH; i++) {
                fromByte += (intByte[i] & 255) << (8 * i);
            }
            return fromByte;
        }

        private final boolean copy(String srcPath, String dstPath) {
            byte[] copyBuf = new byte[8448];
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(COPY);
            stringBuilder.append(srcPath);
            stringBuilder.append(" ");
            stringBuilder.append(dstPath);
            writeCommand(stringBuilder.toString(), copyBuf);
            byte[] returnValue = new byte[ONEINTLENGTH];
            System.arraycopy(copyBuf, 0, returnValue, 0, ONEINTLENGTH);
            String str;
            if (bytesToInt(returnValue) != 0) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("copy from ");
                stringBuilder2.append(srcPath);
                stringBuilder2.append(" to ");
                stringBuilder2.append(dstPath);
                stringBuilder2.append(" failed");
                Log.e(str, stringBuilder2.toString());
                return false;
            }
            if (HWFLOW) {
                str = TAG_FLOW;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("copy from ");
                stringBuilder3.append(srcPath);
                stringBuilder3.append(" to ");
                stringBuilder3.append(dstPath);
                stringBuilder3.append(" success");
                Log.i(str, stringBuilder3.toString());
            }
            return HWLOGW_E;
        }
    }

    static {
        boolean z = Log.HWINFO;
        boolean z2 = HWLOGW_E;
        z = (z || (Log.HWModuleLog && Log.isLoggable(TAG, 4))) ? HWLOGW_E : false;
        HWFLOW = z;
        if (!(Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3)))) {
            z2 = false;
        }
        localLOGV = z2;
    }

    public boolean isEmergencyState() {
        String emergencyState = SystemProperties.get(EMERGENCY_STATE_PROP_NAME);
        if (EMERGENCY_MOUNT_STATE.equals(emergencyState) || EMERGENCY_CRASH_STATE.equals(emergencyState)) {
            return HWLOGW_E;
        }
        return false;
    }

    public boolean isEmergencyMountState() {
        if (EMERGENCY_MOUNT_STATE.equals(SystemProperties.get(EMERGENCY_STATE_PROP_NAME))) {
            return HWLOGW_E;
        }
        return false;
    }

    public void backupEmergencyDataFile() {
        if (HWFLOW) {
            Log.i(TAG_FLOW, "begin to copy some files");
        }
        FileBackup mFileBackup = new FileBackup();
        if (mFileBackup.init()) {
            if (HWFLOW) {
                Log.i(TAG_FLOW, "FileBackup init success");
            }
            mFileBackup.copy(CONTACTS_PROVIDER_PATH_BK, CONTACTS_PROVIDER_PATH);
            mFileBackup.copy(TELEPHONY_PROVIDER_PATH_BK, TELEPHONY_PROVIDER_PATH);
            mFileBackup.copy(PKGXML_PATH_BK, PKGXML_PATH);
            mFileBackup.disconnectSocket();
        }
    }

    public String getEmergencyPkgName() {
        return EMERGENCY_PKG_NAME;
    }
}
