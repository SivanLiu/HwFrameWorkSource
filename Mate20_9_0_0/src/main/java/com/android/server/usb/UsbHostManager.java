package com.android.server.usb;

import android.content.ComponentName;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.internal.util.dump.DumpUtils;
import com.android.server.usb.descriptors.UsbDescriptor;
import com.android.server.usb.descriptors.UsbDescriptorParser;
import com.android.server.usb.descriptors.UsbDeviceDescriptor;
import com.android.server.usb.descriptors.report.TextReportCanvas;
import com.android.server.usb.descriptors.tree.UsbDescriptorsTree;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class UsbHostManager {
    private static final boolean DEBUG;
    private static final int HUAWEI_STORAGE_PID = 15168;
    private static final int HUAWEI_STORAGE_VID = 4817;
    private static final int LINUX_FOUNDATION_VID = 7531;
    private static final int MAX_CONNECT_RECORDS = 32;
    protected static final String SUW_FRP_STATE = "hw_suw_frp_state";
    private static final String TAG = UsbHostManager.class.getSimpleName();
    static final SimpleDateFormat sFormat = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");
    private final LinkedList<ConnectionRecord> mConnections = new LinkedList();
    private final Context mContext;
    @GuardedBy("mSettingsLock")
    private UsbProfileGroupSettingsManager mCurrentSettings;
    @GuardedBy("mLock")
    private final HashMap<String, UsbDevice> mDevices = new HashMap();
    private Object mHandlerLock = new Object();
    private final String[] mHostBlacklist;
    private ConnectionRecord mLastConnect;
    private final Object mLock = new Object();
    private int mNumConnects;
    private Object mSettingsLock = new Object();
    private final UsbSettingsManager mSettingsManager;
    private final UsbAlsaManager mUsbAlsaManager;
    @GuardedBy("mHandlerLock")
    private ComponentName mUsbDeviceConnectionHandler;

    class ConnectionRecord {
        static final int CONNECT = 0;
        static final int CONNECT_BADDEVICE = 2;
        static final int CONNECT_BADPARSE = 1;
        static final int DISCONNECT = -1;
        private static final int kDumpBytesPerLine = 16;
        final byte[] mDescriptors;
        String mDeviceAddress;
        final int mMode;
        long mTimestamp = System.currentTimeMillis();

        ConnectionRecord(String deviceAddress, int mode, byte[] descriptors) {
            this.mDeviceAddress = deviceAddress;
            this.mMode = mode;
            this.mDescriptors = descriptors;
        }

        private String formatTime() {
            return new StringBuilder(UsbHostManager.sFormat.format(new Date(this.mTimestamp))).toString();
        }

        void dump(DualDumpOutputStream dump, String idName, long id) {
            DualDumpOutputStream dualDumpOutputStream = dump;
            long token = dump.start(idName, id);
            dualDumpOutputStream.write("device_address", 1138166333441L, this.mDeviceAddress);
            dualDumpOutputStream.write("mode", 1159641169922L, this.mMode);
            dualDumpOutputStream.write(WatchlistEventKeys.TIMESTAMP, 1112396529667L, this.mTimestamp);
            if (this.mMode != -1) {
                UsbDescriptorParser parser = new UsbDescriptorParser(this.mDeviceAddress, this.mDescriptors);
                UsbDeviceDescriptor deviceDescriptor = parser.getDeviceDescriptor();
                dualDumpOutputStream.write("manufacturer", 1120986464260L, deviceDescriptor.getVendorID());
                dualDumpOutputStream.write("product", 1120986464261L, deviceDescriptor.getProductID());
                long isHeadSetToken = dualDumpOutputStream.start("is_headset", 1146756268038L);
                dualDumpOutputStream.write("in", 1133871366145L, parser.isInputHeadset());
                dualDumpOutputStream.write("out", 1133871366146L, parser.isOutputHeadset());
                dualDumpOutputStream.end(isHeadSetToken);
            }
            dualDumpOutputStream.end(token);
        }

        void dumpShort(IndentingPrintWriter pw) {
            StringBuilder stringBuilder;
            if (this.mMode != -1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(formatTime());
                stringBuilder.append(" Connect ");
                stringBuilder.append(this.mDeviceAddress);
                stringBuilder.append(" mode:");
                stringBuilder.append(this.mMode);
                pw.println(stringBuilder.toString());
                UsbDescriptorParser parser = new UsbDescriptorParser(this.mDeviceAddress, this.mDescriptors);
                UsbDeviceDescriptor deviceDescriptor = parser.getDeviceDescriptor();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("manfacturer:0x");
                stringBuilder2.append(Integer.toHexString(deviceDescriptor.getVendorID()));
                stringBuilder2.append(" product:");
                stringBuilder2.append(Integer.toHexString(deviceDescriptor.getProductID()));
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("isHeadset[in: ");
                stringBuilder2.append(parser.isInputHeadset());
                stringBuilder2.append(" , out: ");
                stringBuilder2.append(parser.isOutputHeadset());
                stringBuilder2.append("]");
                pw.println(stringBuilder2.toString());
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(formatTime());
            stringBuilder.append(" Disconnect ");
            stringBuilder.append(this.mDeviceAddress);
            pw.println(stringBuilder.toString());
        }

        void dumpTree(IndentingPrintWriter pw) {
            StringBuilder stringBuilder;
            if (this.mMode != -1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(formatTime());
                stringBuilder.append(" Connect ");
                stringBuilder.append(this.mDeviceAddress);
                stringBuilder.append(" mode:");
                stringBuilder.append(this.mMode);
                pw.println(stringBuilder.toString());
                UsbDescriptorParser parser = new UsbDescriptorParser(this.mDeviceAddress, this.mDescriptors);
                StringBuilder stringBuilder2 = new StringBuilder();
                UsbDescriptorsTree descriptorTree = new UsbDescriptorsTree();
                descriptorTree.parse(parser);
                descriptorTree.report(new TextReportCanvas(parser, stringBuilder2));
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("isHeadset[in: ");
                stringBuilder3.append(parser.isInputHeadset());
                stringBuilder3.append(" , out: ");
                stringBuilder3.append(parser.isOutputHeadset());
                stringBuilder3.append("]");
                stringBuilder2.append(stringBuilder3.toString());
                pw.println(stringBuilder2.toString());
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(formatTime());
            stringBuilder.append(" Disconnect ");
            stringBuilder.append(this.mDeviceAddress);
            pw.println(stringBuilder.toString());
        }

        void dumpList(IndentingPrintWriter pw) {
            StringBuilder stringBuilder;
            if (this.mMode != -1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(formatTime());
                stringBuilder.append(" Connect ");
                stringBuilder.append(this.mDeviceAddress);
                stringBuilder.append(" mode:");
                stringBuilder.append(this.mMode);
                pw.println(stringBuilder.toString());
                UsbDescriptorParser parser = new UsbDescriptorParser(this.mDeviceAddress, this.mDescriptors);
                StringBuilder stringBuilder2 = new StringBuilder();
                TextReportCanvas canvas = new TextReportCanvas(parser, stringBuilder2);
                Iterator it = parser.getDescriptors().iterator();
                while (it.hasNext()) {
                    ((UsbDescriptor) it.next()).report(canvas);
                }
                pw.println(stringBuilder2.toString());
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("isHeadset[in: ");
                stringBuilder3.append(parser.isInputHeadset());
                stringBuilder3.append(" , out: ");
                stringBuilder3.append(parser.isOutputHeadset());
                stringBuilder3.append("]");
                pw.println(stringBuilder3.toString());
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(formatTime());
            stringBuilder.append(" Disconnect ");
            stringBuilder.append(this.mDeviceAddress);
            pw.println(stringBuilder.toString());
        }

        void dumpRaw(IndentingPrintWriter pw) {
            StringBuilder stringBuilder;
            if (this.mMode != -1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(formatTime());
                stringBuilder.append(" Connect ");
                stringBuilder.append(this.mDeviceAddress);
                stringBuilder.append(" mode:");
                stringBuilder.append(this.mMode);
                pw.println(stringBuilder.toString());
                int length = this.mDescriptors.length;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Raw Descriptors ");
                stringBuilder2.append(length);
                stringBuilder2.append(" bytes");
                pw.println(stringBuilder2.toString());
                int dataOffset = 0;
                int line = 0;
                while (line < length / 16) {
                    StringBuilder sb = new StringBuilder();
                    int dataOffset2 = dataOffset;
                    dataOffset = 0;
                    while (dataOffset < 16) {
                        sb.append("0x");
                        Object[] objArr = new Object[1];
                        int dataOffset3 = dataOffset2 + 1;
                        objArr[0] = Byte.valueOf(this.mDescriptors[dataOffset2]);
                        sb.append(String.format("0x%02X", objArr));
                        sb.append(" ");
                        dataOffset++;
                        dataOffset2 = dataOffset3;
                    }
                    pw.println(sb.toString());
                    line++;
                    dataOffset = dataOffset2;
                }
                stringBuilder2 = new StringBuilder();
                while (dataOffset < length) {
                    stringBuilder2.append("0x");
                    Object[] objArr2 = new Object[1];
                    int dataOffset4 = dataOffset + 1;
                    objArr2[0] = Byte.valueOf(this.mDescriptors[dataOffset]);
                    stringBuilder2.append(String.format("0x%02X", objArr2));
                    stringBuilder2.append(" ");
                    dataOffset = dataOffset4;
                }
                pw.println(stringBuilder2.toString());
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(formatTime());
            stringBuilder.append(" Disconnect ");
            stringBuilder.append(this.mDeviceAddress);
            pw.println(stringBuilder.toString());
        }
    }

    private native void monitorUsbHostBus();

    private native ParcelFileDescriptor nativeOpenDevice(String str);

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
    }

    public UsbHostManager(Context context, UsbAlsaManager alsaManager, UsbSettingsManager settingsManager) {
        this.mContext = context;
        this.mHostBlacklist = context.getResources().getStringArray(17236050);
        this.mUsbAlsaManager = alsaManager;
        this.mSettingsManager = settingsManager;
        String deviceConnectionHandler = context.getResources().getString(17039764);
        if (!TextUtils.isEmpty(deviceConnectionHandler)) {
            setUsbDeviceConnectionHandler(ComponentName.unflattenFromString(deviceConnectionHandler));
        }
    }

    public void setCurrentUserSettings(UsbProfileGroupSettingsManager settings) {
        synchronized (this.mSettingsLock) {
            this.mCurrentSettings = settings;
        }
    }

    private UsbProfileGroupSettingsManager getCurrentUserSettings() {
        UsbProfileGroupSettingsManager usbProfileGroupSettingsManager;
        synchronized (this.mSettingsLock) {
            usbProfileGroupSettingsManager = this.mCurrentSettings;
        }
        return usbProfileGroupSettingsManager;
    }

    public void setUsbDeviceConnectionHandler(ComponentName usbDeviceConnectionHandler) {
        synchronized (this.mHandlerLock) {
            this.mUsbDeviceConnectionHandler = usbDeviceConnectionHandler;
        }
    }

    private ComponentName getUsbDeviceConnectionHandler() {
        ComponentName componentName;
        synchronized (this.mHandlerLock) {
            componentName = this.mUsbDeviceConnectionHandler;
        }
        return componentName;
    }

    private boolean isBlackListed(String deviceAddress) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isBlackListed(");
        stringBuilder.append(deviceAddress);
        stringBuilder.append(")");
        Slog.i(str, stringBuilder.toString());
        for (String startsWith : this.mHostBlacklist) {
            if (deviceAddress.startsWith(startsWith)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlackListed(int clazz, int subClass) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isBlackListed(");
        stringBuilder.append(clazz);
        stringBuilder.append(", ");
        stringBuilder.append(subClass);
        stringBuilder.append(")");
        Slog.i(str, stringBuilder.toString());
        boolean z = true;
        if (clazz == 9) {
            return true;
        }
        if (!(clazz == 3 && subClass == 1)) {
            z = false;
        }
        return z;
    }

    private void addConnectionRecord(String deviceAddress, int mode, byte[] rawDescriptors) {
        this.mNumConnects++;
        while (this.mConnections.size() >= 32) {
            this.mConnections.removeFirst();
        }
        ConnectionRecord rec = new ConnectionRecord(deviceAddress, mode, rawDescriptors);
        this.mConnections.add(rec);
        if (mode != -1) {
            this.mLastConnect = rec;
        }
    }

    private int logUsbDevice(UsbDescriptorParser descriptorParser) {
        UsbDescriptorParser usbDescriptorParser = descriptorParser;
        int vid = 0;
        int pid = 0;
        String mfg = "<unknown>";
        String product = "<unknown>";
        String version = "<unknown>";
        String serial = "<unknown>";
        UsbDeviceDescriptor deviceDescriptor = descriptorParser.getDeviceDescriptor();
        if (deviceDescriptor != null) {
            vid = deviceDescriptor.getVendorID();
            pid = deviceDescriptor.getProductID();
            mfg = deviceDescriptor.getMfgString(usbDescriptorParser);
            product = deviceDescriptor.getProductString(usbDescriptorParser);
            version = deviceDescriptor.getDeviceReleaseString();
            serial = deviceDescriptor.getSerialString(usbDescriptorParser);
        }
        if (mfg == null && product == null && serial == null) {
            Slog.d(TAG, "mfd:null, product:null, serial :null. return false.");
            return -1;
        } else if (vid == LINUX_FOUNDATION_VID) {
            Slog.d(TAG, "vid is LINUX_FOUNDATION_VID, return false");
            return -1;
        } else {
            boolean hasAudio = descriptorParser.hasAudioInterface();
            boolean hasHid = descriptorParser.hasHIDInterface();
            boolean hasStorage = descriptorParser.hasStorageInterface();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("USB device attached: ");
            stringBuilder.append(String.format("vidpid %04x:%04x", new Object[]{Integer.valueOf(vid), Integer.valueOf(pid)}));
            String attachedString = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(attachedString);
            stringBuilder.append(String.format(" mfg/product/ver/serial %s/%s/%s/%s", new Object[]{mfg, product, version, serial}));
            attachedString = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(attachedString);
            stringBuilder.append(String.format(" hasAudio/HID/Storage: %b/%b/%b", new Object[]{Boolean.valueOf(hasAudio), Boolean.valueOf(hasHid), Boolean.valueOf(hasStorage)}));
            Slog.d(TAG, stringBuilder.toString());
            return 0;
        }
    }

    private static boolean isDeviceProvisioned(Context context) {
        return Global.getInt(context.getContentResolver(), "device_provisioned", 0) == 1;
    }

    protected static boolean isFrpRestricted(Context context) {
        return Secure.getIntForUser(context.getContentResolver(), SUW_FRP_STATE, 0, 0) == 1;
    }

    private void updateHuaweiStorageExtraInfo(UsbDevice device) {
        if (device.getProductId() == HUAWEI_STORAGE_PID && device.getVendorId() == HUAWEI_STORAGE_VID) {
            UsbDeviceConnection connection = ((UsbManager) this.mContext.getSystemService("usb")).openDevice(device);
            if (connection == null) {
                Slog.w(TAG, "The USB Connection is NULL, return !");
                return;
            }
            int inquire;
            byte[] sStringBuffer = new byte[256];
            if (connection.controlTransfer(HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_PLUS, 7, 0, 0, sStringBuffer, 1, 0) >= 0) {
                inquire = sStringBuffer[0] & 255;
            } else {
                inquire = -1;
            }
            connection.close();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setBackupSubProductId, inquire = ");
            stringBuilder.append(inquire);
            Slog.d(str, stringBuilder.toString());
            device.setBackupSubProductId(inquire);
            return;
        }
        Slog.w(TAG, "No huawei Backup device, return !");
    }

    /* JADX WARNING: Missing block: B:51:0x0105, code skipped:
            if (DEBUG == false) goto L_0x0122;
     */
    /* JADX WARNING: Missing block: B:52:0x0107, code skipped:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("beginUsbDeviceAdded(");
            r2.append(r8);
            r2.append(") end");
            android.util.Slog.d(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:54:0x0123, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean usbDeviceAdded(String deviceAddress, int deviceClass, int deviceSubclass, byte[] descriptors) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("usbDeviceAdded(");
            stringBuilder.append(deviceAddress);
            stringBuilder.append(") - start");
            Slog.d(str, stringBuilder.toString());
        }
        if (this.mContext != null && !isDeviceProvisioned(this.mContext) && isFrpRestricted(this.mContext)) {
            Slog.d(TAG, "usbDeviceAdded return, Global.DEVICE_PROVISIONED:0");
            return false;
        } else if (isBlackListed(deviceAddress)) {
            if (DEBUG) {
                Slog.d(TAG, "device address is black listed");
            }
            return false;
        } else {
            UsbDescriptorParser parser = new UsbDescriptorParser(deviceAddress, descriptors);
            if (-1 == logUsbDevice(parser)) {
                if (DEBUG) {
                    Slog.d(TAG, "device parser failed");
                }
                return false;
            } else if (isBlackListed(deviceClass, deviceSubclass)) {
                if (DEBUG) {
                    Slog.d(TAG, "device class is black listed");
                }
                return false;
            } else {
                synchronized (this.mLock) {
                    if (this.mDevices.get(deviceAddress) != null) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("device already on mDevices list: ");
                        stringBuilder2.append(deviceAddress);
                        Slog.w(str2, stringBuilder2.toString());
                        return false;
                    }
                    UsbDevice newDevice = parser.toAndroidUsbDevice();
                    if (newDevice == null) {
                        Slog.e(TAG, "Couldn't create UsbDevice object.");
                        addConnectionRecord(deviceAddress, 2, parser.getRawDescriptors());
                    } else {
                        this.mDevices.put(deviceAddress, newDevice);
                        updateHuaweiStorageExtraInfo(newDevice);
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Added device ");
                        stringBuilder3.append(newDevice);
                        Slog.d(str3, stringBuilder3.toString());
                        ComponentName usbDeviceConnectionHandler = getUsbDeviceConnectionHandler();
                        if (usbDeviceConnectionHandler == null) {
                            getCurrentUserSettings().deviceAttached(newDevice);
                        } else {
                            getCurrentUserSettings().deviceAttachedForFixedHandler(newDevice, usbDeviceConnectionHandler);
                        }
                        if (parser.getRawDescriptors() != null) {
                            this.mUsbAlsaManager.usbDeviceAdded(deviceAddress, newDevice, parser);
                        } else {
                            Slog.e(TAG, "rawDescriptors is null, skip UsbAlsaManager.usbDeviceAdded");
                        }
                        addConnectionRecord(deviceAddress, 0, parser.getRawDescriptors());
                    }
                }
            }
        }
    }

    private void usbDeviceRemoved(String deviceAddress) {
        synchronized (this.mLock) {
            UsbDevice device = (UsbDevice) this.mDevices.remove(deviceAddress);
            String str;
            StringBuilder stringBuilder;
            if (device != null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Removed device at deviceAddress = ");
                stringBuilder.append(deviceAddress);
                stringBuilder.append(": ");
                stringBuilder.append(device.getProductName());
                Slog.d(str, stringBuilder.toString());
                this.mUsbAlsaManager.usbDeviceRemoved(deviceAddress);
                this.mSettingsManager.usbDeviceRemoved(device);
                getCurrentUserSettings().usbDeviceRemoved(device);
                addConnectionRecord(deviceAddress, -1, null);
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Removed device at deviceAddress  = ");
                stringBuilder.append(deviceAddress);
                stringBuilder.append(" was already gone");
                Slog.d(str, stringBuilder.toString());
            }
        }
    }

    public void systemReady() {
        synchronized (this.mLock) {
            new Thread(null, new -$$Lambda$UsbHostManager$XT3F5aQci4H6VWSBYBQQNSzpnvs(this), "UsbService host thread").start();
        }
    }

    public void getDeviceList(Bundle devices) {
        synchronized (this.mLock) {
            for (String name : this.mDevices.keySet()) {
                devices.putParcelable(name, (Parcelable) this.mDevices.get(name));
            }
        }
    }

    public ParcelFileDescriptor openDevice(String deviceAddress, UsbUserSettingsManager settings, String packageName, int uid) {
        ParcelFileDescriptor nativeOpenDevice;
        synchronized (this.mLock) {
            if (isBlackListed(deviceAddress)) {
                throw new SecurityException("USB device is on a restricted bus");
            }
            UsbDevice device = (UsbDevice) this.mDevices.get(deviceAddress);
            if (device != null) {
                settings.checkPermission(device, packageName, uid);
                nativeOpenDevice = nativeOpenDevice(deviceAddress);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("device ");
                stringBuilder.append(deviceAddress);
                stringBuilder.append(" does not exist or is restricted");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        return nativeOpenDevice;
    }

    public void dump(DualDumpOutputStream dump, String idName, long id) {
        long token = dump.start(idName, id);
        synchronized (this.mHandlerLock) {
            if (this.mUsbDeviceConnectionHandler != null) {
                DumpUtils.writeComponentName(dump, "default_usb_host_connection_handler", 1146756268033L, this.mUsbDeviceConnectionHandler);
            }
        }
        synchronized (this.mLock) {
            for (String name : this.mDevices.keySet()) {
                com.android.internal.usb.DumpUtils.writeDevice(dump, "devices", 2246267895810L, (UsbDevice) this.mDevices.get(name));
            }
            dump.write("num_connects", 1120986464259L, this.mNumConnects);
            Iterator it = this.mConnections.iterator();
            while (it.hasNext()) {
                ((ConnectionRecord) it.next()).dump(dump, "connections", 2246267895812L);
            }
        }
        dump.end(token);
    }

    public void dumpDescriptors(IndentingPrintWriter pw, String[] args) {
        if (this.mLastConnect != null) {
            pw.println("Last Connected USB Device:");
            if (args.length <= 1 || args[1].equals("-dump-short")) {
                this.mLastConnect.dumpShort(pw);
                return;
            } else if (args[1].equals("-dump-tree")) {
                this.mLastConnect.dumpTree(pw);
                return;
            } else if (args[1].equals("-dump-list")) {
                this.mLastConnect.dumpList(pw);
                return;
            } else if (args[1].equals("-dump-raw")) {
                this.mLastConnect.dumpRaw(pw);
                return;
            } else {
                return;
            }
        }
        pw.println("No USB Devices have been connected.");
    }
}
