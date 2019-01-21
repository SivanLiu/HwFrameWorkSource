package com.android.server.hdmi;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.HdmiHotplugEvent;
import android.hardware.hdmi.HdmiPortInfo;
import android.hardware.hdmi.IHdmiControlCallback;
import android.hardware.hdmi.IHdmiControlService.Stub;
import android.hardware.hdmi.IHdmiDeviceEventListener;
import android.hardware.hdmi.IHdmiHotplugEventListener;
import android.hardware.hdmi.IHdmiInputChangeListener;
import android.hardware.hdmi.IHdmiMhlVendorCommandListener;
import android.hardware.hdmi.IHdmiRecordListener;
import android.hardware.hdmi.IHdmiSystemAudioModeChangeListener;
import android.hardware.hdmi.IHdmiVendorCommandListener;
import android.media.AudioManager;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputManager.TvInputCallback;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.SystemService;
import com.android.server.hdmi.HdmiAnnotations.ServiceThreadOnly;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import libcore.util.EmptyArray;

public final class HdmiControlService extends SystemService {
    static final int INITIATED_BY_BOOT_UP = 1;
    static final int INITIATED_BY_ENABLE_CEC = 0;
    static final int INITIATED_BY_HOTPLUG = 4;
    static final int INITIATED_BY_SCREEN_ON = 2;
    static final int INITIATED_BY_WAKE_UP_MESSAGE = 3;
    static final String PERMISSION = "android.permission.HDMI_CEC";
    static final int STANDBY_SCREEN_OFF = 0;
    static final int STANDBY_SHUTDOWN = 1;
    private static final String TAG = "HdmiControlService";
    private final Locale HONG_KONG = new Locale("zh", "HK");
    private final Locale MACAU = new Locale("zh", "MO");
    @ServiceThreadOnly
    private int mActivePortId = -1;
    private boolean mAddressAllocated = false;
    private HdmiCecController mCecController;
    private final CecMessageBuffer mCecMessageBuffer = new CecMessageBuffer(this, null);
    @GuardedBy("mLock")
    private final ArrayList<DeviceEventListenerRecord> mDeviceEventListenerRecords = new ArrayList();
    private final Handler mHandler = new Handler();
    private final HdmiControlBroadcastReceiver mHdmiControlBroadcastReceiver = new HdmiControlBroadcastReceiver(this, null);
    @GuardedBy("mLock")
    private boolean mHdmiControlEnabled;
    @GuardedBy("mLock")
    private final ArrayList<HotplugEventListenerRecord> mHotplugEventListenerRecords = new ArrayList();
    @GuardedBy("mLock")
    private InputChangeListenerRecord mInputChangeListenerRecord;
    private final HandlerThread mIoThread = new HandlerThread("Hdmi Control Io Thread");
    @ServiceThreadOnly
    private String mLanguage = Locale.getDefault().getISO3Language();
    @ServiceThreadOnly
    private int mLastInputMhl = -1;
    private final List<Integer> mLocalDevices = getIntList(SystemProperties.get("ro.hdmi.device_type"));
    private final Object mLock = new Object();
    private HdmiCecMessageValidator mMessageValidator;
    private HdmiMhlControllerStub mMhlController;
    @GuardedBy("mLock")
    private List<HdmiDeviceInfo> mMhlDevices;
    @GuardedBy("mLock")
    private boolean mMhlInputChangeEnabled;
    @GuardedBy("mLock")
    private final ArrayList<HdmiMhlVendorCommandListenerRecord> mMhlVendorCommandListenerRecords = new ArrayList();
    private UnmodifiableSparseArray<HdmiDeviceInfo> mPortDeviceMap;
    private UnmodifiableSparseIntArray mPortIdMap;
    private List<HdmiPortInfo> mPortInfo;
    private UnmodifiableSparseArray<HdmiPortInfo> mPortInfoMap;
    private PowerManager mPowerManager;
    @ServiceThreadOnly
    private int mPowerStatus = 1;
    @GuardedBy("mLock")
    private boolean mProhibitMode;
    @GuardedBy("mLock")
    private HdmiRecordListenerRecord mRecordListenerRecord;
    private final SelectRequestBuffer mSelectRequestBuffer = new SelectRequestBuffer();
    private final SettingsObserver mSettingsObserver = new SettingsObserver(this.mHandler);
    @ServiceThreadOnly
    private boolean mStandbyMessageReceived = false;
    private final ArrayList<SystemAudioModeChangeListenerRecord> mSystemAudioModeChangeListenerRecords = new ArrayList();
    private TvInputManager mTvInputManager;
    @GuardedBy("mLock")
    private final ArrayList<VendorCommandListenerRecord> mVendorCommandListenerRecords = new ArrayList();
    @ServiceThreadOnly
    private boolean mWakeUpMessageReceived = false;

    private final class BinderService extends Stub {
        private BinderService() {
        }

        /* synthetic */ BinderService(HdmiControlService x0, AnonymousClass1 x1) {
            this();
        }

        public int[] getSupportedTypes() {
            HdmiControlService.this.enforceAccessPermission();
            int[] localDevices = new int[HdmiControlService.this.mLocalDevices.size()];
            for (int i = 0; i < localDevices.length; i++) {
                localDevices[i] = ((Integer) HdmiControlService.this.mLocalDevices.get(i)).intValue();
            }
            return localDevices;
        }

        public HdmiDeviceInfo getActiveSource() {
            HdmiControlService.this.enforceAccessPermission();
            HdmiCecLocalDeviceTv tv = HdmiControlService.this.tv();
            if (tv == null) {
                Slog.w(HdmiControlService.TAG, "Local tv device not available");
                return null;
            }
            ActiveSource activeSource = tv.getActiveSource();
            if (activeSource.isValid()) {
                return new HdmiDeviceInfo(activeSource.logicalAddress, activeSource.physicalAddress, -1, -1, 0, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            }
            int activePath = tv.getActivePath();
            if (activePath == NetworkConstants.ARP_HWTYPE_RESERVED_HI) {
                return null;
            }
            HdmiDeviceInfo info = tv.getSafeDeviceInfoByPath(activePath);
            return info != null ? info : new HdmiDeviceInfo(activePath, tv.getActivePortId());
        }

        public void deviceSelect(final int deviceId, final IHdmiControlCallback callback) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                public void run() {
                    if (callback == null) {
                        Slog.e(HdmiControlService.TAG, "Callback cannot be null");
                        return;
                    }
                    HdmiCecLocalDeviceTv tv = HdmiControlService.this.tv();
                    if (tv != null) {
                        HdmiMhlLocalDeviceStub device = HdmiControlService.this.mMhlController.getLocalDeviceById(deviceId);
                        if (device == null) {
                            tv.deviceSelect(deviceId, callback);
                        } else if (device.getPortId() == tv.getActivePortId()) {
                            HdmiControlService.this.invokeCallback(callback, 0);
                        } else {
                            device.turnOn(callback);
                            tv.doManualPortSwitching(device.getPortId(), null);
                        }
                    } else if (HdmiControlService.this.mAddressAllocated) {
                        Slog.w(HdmiControlService.TAG, "Local tv device not available");
                        HdmiControlService.this.invokeCallback(callback, 2);
                    } else {
                        HdmiControlService.this.mSelectRequestBuffer.set(SelectRequestBuffer.newDeviceSelect(HdmiControlService.this, deviceId, callback));
                    }
                }
            });
        }

        public void portSelect(final int portId, final IHdmiControlCallback callback) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                public void run() {
                    if (callback == null) {
                        Slog.e(HdmiControlService.TAG, "Callback cannot be null");
                        return;
                    }
                    HdmiCecLocalDeviceTv tv = HdmiControlService.this.tv();
                    if (tv != null) {
                        tv.doManualPortSwitching(portId, callback);
                    } else if (HdmiControlService.this.mAddressAllocated) {
                        Slog.w(HdmiControlService.TAG, "Local tv device not available");
                        HdmiControlService.this.invokeCallback(callback, 2);
                    } else {
                        HdmiControlService.this.mSelectRequestBuffer.set(SelectRequestBuffer.newPortSelect(HdmiControlService.this, portId, callback));
                    }
                }
            });
        }

        public void sendKeyEvent(final int deviceType, final int keyCode, final boolean isPressed) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                public void run() {
                    HdmiMhlLocalDeviceStub device = HdmiControlService.this.mMhlController.getLocalDevice(HdmiControlService.this.mActivePortId);
                    if (device != null) {
                        device.sendKeyEvent(keyCode, isPressed);
                        return;
                    }
                    if (HdmiControlService.this.mCecController != null) {
                        HdmiCecLocalDevice localDevice = HdmiControlService.this.mCecController.getLocalDevice(deviceType);
                        if (localDevice == null) {
                            Slog.w(HdmiControlService.TAG, "Local device not available");
                            return;
                        }
                        localDevice.sendKeyEvent(keyCode, isPressed);
                    }
                }
            });
        }

        public void oneTouchPlay(final IHdmiControlCallback callback) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                public void run() {
                    HdmiControlService.this.oneTouchPlay(callback);
                }
            });
        }

        public void queryDisplayStatus(final IHdmiControlCallback callback) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                public void run() {
                    HdmiControlService.this.queryDisplayStatus(callback);
                }
            });
        }

        public void addHotplugEventListener(IHdmiHotplugEventListener listener) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.addHotplugEventListener(listener);
        }

        public void removeHotplugEventListener(IHdmiHotplugEventListener listener) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.removeHotplugEventListener(listener);
        }

        public void addDeviceEventListener(IHdmiDeviceEventListener listener) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.addDeviceEventListener(listener);
        }

        public List<HdmiPortInfo> getPortInfo() {
            HdmiControlService.this.enforceAccessPermission();
            return HdmiControlService.this.getPortInfo();
        }

        public boolean canChangeSystemAudioMode() {
            HdmiControlService.this.enforceAccessPermission();
            HdmiCecLocalDeviceTv tv = HdmiControlService.this.tv();
            if (tv == null) {
                return false;
            }
            return tv.hasSystemAudioDevice();
        }

        public boolean getSystemAudioMode() {
            HdmiControlService.this.enforceAccessPermission();
            HdmiCecLocalDeviceTv tv = HdmiControlService.this.tv();
            if (tv == null) {
                return false;
            }
            return tv.isSystemAudioActivated();
        }

        public void setSystemAudioMode(final boolean enabled, final IHdmiControlCallback callback) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                public void run() {
                    HdmiCecLocalDeviceTv tv = HdmiControlService.this.tv();
                    if (tv == null) {
                        Slog.w(HdmiControlService.TAG, "Local tv device not available");
                        HdmiControlService.this.invokeCallback(callback, 2);
                        return;
                    }
                    tv.changeSystemAudioMode(enabled, callback);
                }
            });
        }

        public void addSystemAudioModeChangeListener(IHdmiSystemAudioModeChangeListener listener) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.addSystemAudioModeChangeListner(listener);
        }

        public void removeSystemAudioModeChangeListener(IHdmiSystemAudioModeChangeListener listener) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.removeSystemAudioModeChangeListener(listener);
        }

        public void setInputChangeListener(IHdmiInputChangeListener listener) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.setInputChangeListener(listener);
        }

        public List<HdmiDeviceInfo> getInputDevices() {
            List mergeToUnmodifiableList;
            HdmiControlService.this.enforceAccessPermission();
            HdmiCecLocalDeviceTv tv = HdmiControlService.this.tv();
            synchronized (HdmiControlService.this.mLock) {
                List<HdmiDeviceInfo> cecDevices;
                if (tv == null) {
                    try {
                        cecDevices = Collections.emptyList();
                    } catch (Throwable th) {
                    }
                } else {
                    cecDevices = tv.getSafeExternalInputsLocked();
                }
                mergeToUnmodifiableList = HdmiUtils.mergeToUnmodifiableList(cecDevices, HdmiControlService.this.getMhlDevicesLocked());
            }
            return mergeToUnmodifiableList;
        }

        public List<HdmiDeviceInfo> getDeviceList() {
            List<HdmiDeviceInfo> emptyList;
            HdmiControlService.this.enforceAccessPermission();
            HdmiCecLocalDeviceTv tv = HdmiControlService.this.tv();
            synchronized (HdmiControlService.this.mLock) {
                if (tv == null) {
                    try {
                        emptyList = Collections.emptyList();
                    } catch (Throwable th) {
                    }
                } else {
                    emptyList = tv.getSafeCecDevicesLocked();
                }
            }
            return emptyList;
        }

        public void setSystemAudioVolume(final int oldIndex, final int newIndex, final int maxIndex) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                public void run() {
                    HdmiCecLocalDeviceTv tv = HdmiControlService.this.tv();
                    if (tv == null) {
                        Slog.w(HdmiControlService.TAG, "Local tv device not available");
                    } else {
                        tv.changeVolume(oldIndex, newIndex - oldIndex, maxIndex);
                    }
                }
            });
        }

        public void setSystemAudioMute(final boolean mute) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                public void run() {
                    HdmiCecLocalDeviceTv tv = HdmiControlService.this.tv();
                    if (tv == null) {
                        Slog.w(HdmiControlService.TAG, "Local tv device not available");
                    } else {
                        tv.changeMute(mute);
                    }
                }
            });
        }

        public void setArcMode(boolean enabled) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                public void run() {
                    if (HdmiControlService.this.tv() == null) {
                        Slog.w(HdmiControlService.TAG, "Local tv device not available to change arc mode.");
                    }
                }
            });
        }

        public void setProhibitMode(boolean enabled) {
            HdmiControlService.this.enforceAccessPermission();
            if (HdmiControlService.this.isTvDevice()) {
                HdmiControlService.this.setProhibitMode(enabled);
            }
        }

        public void addVendorCommandListener(IHdmiVendorCommandListener listener, int deviceType) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.addVendorCommandListener(listener, deviceType);
        }

        public void sendVendorCommand(int deviceType, int targetAddress, byte[] params, boolean hasVendorId) {
            HdmiControlService.this.enforceAccessPermission();
            final int i = deviceType;
            final boolean z = hasVendorId;
            final int i2 = targetAddress;
            final byte[] bArr = params;
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                public void run() {
                    HdmiCecLocalDevice device = HdmiControlService.this.mCecController.getLocalDevice(i);
                    if (device == null) {
                        Slog.w(HdmiControlService.TAG, "Local device not available");
                        return;
                    }
                    if (z) {
                        HdmiControlService.this.sendCecCommand(HdmiCecMessageBuilder.buildVendorCommandWithId(device.getDeviceInfo().getLogicalAddress(), i2, HdmiControlService.this.getVendorId(), bArr));
                    } else {
                        HdmiControlService.this.sendCecCommand(HdmiCecMessageBuilder.buildVendorCommand(device.getDeviceInfo().getLogicalAddress(), i2, bArr));
                    }
                }
            });
        }

        public void sendStandby(final int deviceType, final int deviceId) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                public void run() {
                    HdmiMhlLocalDeviceStub mhlDevice = HdmiControlService.this.mMhlController.getLocalDeviceById(deviceId);
                    if (mhlDevice != null) {
                        mhlDevice.sendStandby();
                        return;
                    }
                    HdmiCecLocalDevice device = HdmiControlService.this.mCecController.getLocalDevice(deviceType);
                    if (device == null) {
                        Slog.w(HdmiControlService.TAG, "Local device not available");
                    } else {
                        device.sendStandby(deviceId);
                    }
                }
            });
        }

        public void setHdmiRecordListener(IHdmiRecordListener listener) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.setHdmiRecordListener(listener);
        }

        public void startOneTouchRecord(final int recorderAddress, final byte[] recordSource) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                public void run() {
                    if (HdmiControlService.this.isTvDeviceEnabled()) {
                        HdmiControlService.this.tv().startOneTouchRecord(recorderAddress, recordSource);
                    } else {
                        Slog.w(HdmiControlService.TAG, "TV device is not enabled.");
                    }
                }
            });
        }

        public void stopOneTouchRecord(final int recorderAddress) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                public void run() {
                    if (HdmiControlService.this.isTvDeviceEnabled()) {
                        HdmiControlService.this.tv().stopOneTouchRecord(recorderAddress);
                    } else {
                        Slog.w(HdmiControlService.TAG, "TV device is not enabled.");
                    }
                }
            });
        }

        public void startTimerRecording(final int recorderAddress, final int sourceType, final byte[] recordSource) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                public void run() {
                    if (HdmiControlService.this.isTvDeviceEnabled()) {
                        HdmiControlService.this.tv().startTimerRecording(recorderAddress, sourceType, recordSource);
                    } else {
                        Slog.w(HdmiControlService.TAG, "TV device is not enabled.");
                    }
                }
            });
        }

        public void clearTimerRecording(final int recorderAddress, final int sourceType, final byte[] recordSource) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                public void run() {
                    if (HdmiControlService.this.isTvDeviceEnabled()) {
                        HdmiControlService.this.tv().clearTimerRecording(recorderAddress, sourceType, recordSource);
                    } else {
                        Slog.w(HdmiControlService.TAG, "TV device is not enabled.");
                    }
                }
            });
        }

        public void sendMhlVendorCommand(int portId, int offset, int length, byte[] data) {
            HdmiControlService.this.enforceAccessPermission();
            final int i = portId;
            final int i2 = offset;
            final int i3 = length;
            final byte[] bArr = data;
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                public void run() {
                    if (!HdmiControlService.this.isControlEnabled()) {
                        Slog.w(HdmiControlService.TAG, "Hdmi control is disabled.");
                    } else if (HdmiControlService.this.mMhlController.getLocalDevice(i) == null) {
                        String str = HdmiControlService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Invalid port id:");
                        stringBuilder.append(i);
                        Slog.w(str, stringBuilder.toString());
                    } else {
                        HdmiControlService.this.mMhlController.sendVendorCommand(i, i2, i3, bArr);
                    }
                }
            });
        }

        public void addHdmiMhlVendorCommandListener(IHdmiMhlVendorCommandListener listener) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.addHdmiMhlVendorCommandListener(listener);
        }

        public void setStandbyMode(final boolean isStandbyModeOn) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                public void run() {
                    HdmiControlService.this.setStandbyMode(isStandbyModeOn);
                }
            });
        }

        protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
            if (DumpUtils.checkDumpPermission(HdmiControlService.this.getContext(), HdmiControlService.TAG, writer)) {
                IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mHdmiControlEnabled: ");
                stringBuilder.append(HdmiControlService.this.mHdmiControlEnabled);
                pw.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("mProhibitMode: ");
                stringBuilder.append(HdmiControlService.this.mProhibitMode);
                pw.println(stringBuilder.toString());
                if (HdmiControlService.this.mCecController != null) {
                    pw.println("mCecController: ");
                    pw.increaseIndent();
                    HdmiControlService.this.mCecController.dump(pw);
                    pw.decreaseIndent();
                }
                pw.println("mMhlController: ");
                pw.increaseIndent();
                HdmiControlService.this.mMhlController.dump(pw);
                pw.decreaseIndent();
                pw.println("mPortInfo: ");
                pw.increaseIndent();
                for (HdmiPortInfo hdmiPortInfo : HdmiControlService.this.mPortInfo) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("- ");
                    stringBuilder2.append(hdmiPortInfo);
                    pw.println(stringBuilder2.toString());
                }
                pw.decreaseIndent();
                stringBuilder = new StringBuilder();
                stringBuilder.append("mPowerStatus: ");
                stringBuilder.append(HdmiControlService.this.mPowerStatus);
                pw.println(stringBuilder.toString());
            }
        }
    }

    private final class CecMessageBuffer {
        private List<HdmiCecMessage> mBuffer;

        private CecMessageBuffer() {
            this.mBuffer = new ArrayList();
        }

        /* synthetic */ CecMessageBuffer(HdmiControlService x0, AnonymousClass1 x1) {
            this();
        }

        public void bufferMessage(HdmiCecMessage message) {
            int opcode = message.getOpcode();
            if (opcode == 4 || opcode == 13) {
                bufferImageOrTextViewOn(message);
            } else if (opcode == 130) {
                bufferActiveSource(message);
            }
        }

        public void processMessages() {
            for (final HdmiCecMessage message : this.mBuffer) {
                HdmiControlService.this.runOnServiceThread(new Runnable() {
                    public void run() {
                        HdmiControlService.this.handleCecCommand(message);
                    }
                });
            }
            this.mBuffer.clear();
        }

        private void bufferActiveSource(HdmiCecMessage message) {
            if (!replaceMessageIfBuffered(message, 130)) {
                this.mBuffer.add(message);
            }
        }

        private void bufferImageOrTextViewOn(HdmiCecMessage message) {
            if (!replaceMessageIfBuffered(message, 4) && !replaceMessageIfBuffered(message, 13)) {
                this.mBuffer.add(message);
            }
        }

        private boolean replaceMessageIfBuffered(HdmiCecMessage message, int opcode) {
            for (int i = 0; i < this.mBuffer.size(); i++) {
                if (((HdmiCecMessage) this.mBuffer.get(i)).getOpcode() == opcode) {
                    this.mBuffer.set(i, message);
                    return true;
                }
            }
            return false;
        }
    }

    private final class DeviceEventListenerRecord implements DeathRecipient {
        private final IHdmiDeviceEventListener mListener;

        public DeviceEventListenerRecord(IHdmiDeviceEventListener listener) {
            this.mListener = listener;
        }

        public void binderDied() {
            synchronized (HdmiControlService.this.mLock) {
                HdmiControlService.this.mDeviceEventListenerRecords.remove(this);
            }
        }
    }

    interface DevicePollingCallback {
        void onPollingFinished(List<Integer> list);
    }

    private class HdmiControlBroadcastReceiver extends BroadcastReceiver {
        private HdmiControlBroadcastReceiver() {
        }

        /* synthetic */ HdmiControlBroadcastReceiver(HdmiControlService x0, AnonymousClass1 x1) {
            this();
        }

        /* JADX WARNING: Removed duplicated region for block: B:42:? A:{SYNTHETIC, RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:31:0x0083  */
        /* JADX WARNING: Removed duplicated region for block: B:28:0x0075  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x005f  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x0051  */
        /* JADX WARNING: Removed duplicated region for block: B:42:? A:{SYNTHETIC, RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:31:0x0083  */
        /* JADX WARNING: Removed duplicated region for block: B:28:0x0075  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x005f  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x0051  */
        /* JADX WARNING: Removed duplicated region for block: B:42:? A:{SYNTHETIC, RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:31:0x0083  */
        /* JADX WARNING: Removed duplicated region for block: B:28:0x0075  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x005f  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x0051  */
        /* JADX WARNING: Removed duplicated region for block: B:42:? A:{SYNTHETIC, RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:31:0x0083  */
        /* JADX WARNING: Removed duplicated region for block: B:28:0x0075  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x005f  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x0051  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        @ServiceThreadOnly
        public void onReceive(Context context, Intent intent) {
            int i;
            HdmiControlService.this.assertRunOnServiceThread();
            String action = intent.getAction();
            int hashCode = action.hashCode();
            if (hashCode == -2128145023) {
                if (action.equals("android.intent.action.SCREEN_OFF")) {
                    i = 0;
                    switch (i) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            break;
                        case 3:
                            break;
                        default:
                            break;
                    }
                }
            } else if (hashCode == -1454123155) {
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    i = 1;
                    switch (i) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            break;
                        case 3:
                            break;
                        default:
                            break;
                    }
                }
            } else if (hashCode == 158859398) {
                if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                    i = 2;
                    switch (i) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            break;
                        case 3:
                            break;
                        default:
                            break;
                    }
                }
            } else if (hashCode == 1947666138 && action.equals("android.intent.action.ACTION_SHUTDOWN")) {
                i = 3;
                switch (i) {
                    case 0:
                        if (HdmiControlService.this.isPowerOnOrTransient()) {
                            HdmiControlService.this.onStandby(0);
                            return;
                        }
                        return;
                    case 1:
                        if (HdmiControlService.this.isPowerStandbyOrTransient()) {
                            HdmiControlService.this.onWakeUp();
                            return;
                        }
                        return;
                    case 2:
                        action = getMenuLanguage();
                        if (!HdmiControlService.this.mLanguage.equals(action)) {
                            HdmiControlService.this.onLanguageChanged(action);
                            return;
                        }
                        return;
                    case 3:
                        if (HdmiControlService.this.isPowerOnOrTransient()) {
                            HdmiControlService.this.onStandby(1);
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
            i = -1;
            switch (i) {
                case 0:
                    break;
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;
                default:
                    break;
            }
        }

        private String getMenuLanguage() {
            Locale locale = Locale.getDefault();
            if (locale.equals(Locale.TAIWAN) || locale.equals(HdmiControlService.this.HONG_KONG) || locale.equals(HdmiControlService.this.MACAU)) {
                return "chi";
            }
            return locale.getISO3Language();
        }
    }

    private class HdmiMhlVendorCommandListenerRecord implements DeathRecipient {
        private final IHdmiMhlVendorCommandListener mListener;

        public HdmiMhlVendorCommandListenerRecord(IHdmiMhlVendorCommandListener listener) {
            this.mListener = listener;
        }

        public void binderDied() {
            HdmiControlService.this.mMhlVendorCommandListenerRecords.remove(this);
        }
    }

    private class HdmiRecordListenerRecord implements DeathRecipient {
        private final IHdmiRecordListener mListener;

        public HdmiRecordListenerRecord(IHdmiRecordListener listener) {
            this.mListener = listener;
        }

        public void binderDied() {
            synchronized (HdmiControlService.this.mLock) {
                if (HdmiControlService.this.mRecordListenerRecord == this) {
                    HdmiControlService.this.mRecordListenerRecord = null;
                }
            }
        }
    }

    private final class HotplugEventListenerRecord implements DeathRecipient {
        private final IHdmiHotplugEventListener mListener;

        public HotplugEventListenerRecord(IHdmiHotplugEventListener listener) {
            this.mListener = listener;
        }

        public void binderDied() {
            synchronized (HdmiControlService.this.mLock) {
                HdmiControlService.this.mHotplugEventListenerRecords.remove(this);
            }
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof HotplugEventListenerRecord)) {
                return false;
            }
            boolean z = true;
            if (obj == this) {
                return true;
            }
            if (((HotplugEventListenerRecord) obj).mListener != this.mListener) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            return this.mListener.hashCode();
        }
    }

    private final class InputChangeListenerRecord implements DeathRecipient {
        private final IHdmiInputChangeListener mListener;

        public InputChangeListenerRecord(IHdmiInputChangeListener listener) {
            this.mListener = listener;
        }

        public void binderDied() {
            synchronized (HdmiControlService.this.mLock) {
                if (HdmiControlService.this.mInputChangeListenerRecord == this) {
                    HdmiControlService.this.mInputChangeListenerRecord = null;
                }
            }
        }
    }

    interface SendMessageCallback {
        void onSendCompleted(int i);
    }

    private class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onChange(boolean selfChange, Uri uri) {
            boolean z;
            String option = uri.getLastPathSegment();
            boolean enabled = HdmiControlService.this.readBooleanSetting(option, true);
            switch (option.hashCode()) {
                case -2009736264:
                    if (option.equals("hdmi_control_enabled")) {
                        z = false;
                        break;
                    }
                case -1489007315:
                    if (option.equals("hdmi_system_audio_control_enabled")) {
                        z = true;
                        break;
                    }
                case -1262529811:
                    if (option.equals("mhl_input_switching_enabled")) {
                        z = true;
                        break;
                    }
                case -885757826:
                    if (option.equals("mhl_power_charge_enabled")) {
                        z = true;
                        break;
                    }
                case 726613192:
                    if (option.equals("hdmi_control_auto_wakeup_enabled")) {
                        z = true;
                        break;
                    }
                case 1628046095:
                    if (option.equals("hdmi_control_auto_device_off_enabled")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HdmiControlService.this.setControlEnabled(enabled);
                    return;
                case true:
                    if (HdmiControlService.this.isTvDeviceEnabled()) {
                        HdmiControlService.this.tv().setAutoWakeup(enabled);
                    }
                    HdmiControlService.this.setCecOption(1, enabled);
                    return;
                case true:
                    for (Integer type : HdmiControlService.this.mLocalDevices) {
                        HdmiCecLocalDevice localDevice = HdmiControlService.this.mCecController.getLocalDevice(type.intValue());
                        if (localDevice != null) {
                            localDevice.setAutoDeviceOff(enabled);
                        }
                    }
                    return;
                case true:
                    if (HdmiControlService.this.isTvDeviceEnabled()) {
                        HdmiControlService.this.tv().setSystemAudioControlFeatureEnabled(enabled);
                        return;
                    }
                    return;
                case true:
                    HdmiControlService.this.setMhlInputChangeEnabled(enabled);
                    return;
                case true:
                    HdmiControlService.this.mMhlController.setOption(102, HdmiControlService.toInt(enabled));
                    return;
                default:
                    return;
            }
        }
    }

    private final class SystemAudioModeChangeListenerRecord implements DeathRecipient {
        private final IHdmiSystemAudioModeChangeListener mListener;

        public SystemAudioModeChangeListenerRecord(IHdmiSystemAudioModeChangeListener listener) {
            this.mListener = listener;
        }

        public void binderDied() {
            synchronized (HdmiControlService.this.mLock) {
                HdmiControlService.this.mSystemAudioModeChangeListenerRecords.remove(this);
            }
        }
    }

    class VendorCommandListenerRecord implements DeathRecipient {
        private final int mDeviceType;
        private final IHdmiVendorCommandListener mListener;

        public VendorCommandListenerRecord(IHdmiVendorCommandListener listener, int deviceType) {
            this.mListener = listener;
            this.mDeviceType = deviceType;
        }

        public void binderDied() {
            synchronized (HdmiControlService.this.mLock) {
                HdmiControlService.this.mVendorCommandListenerRecords.remove(this);
            }
        }
    }

    public HdmiControlService(Context context) {
        super(context);
    }

    private static List<Integer> getIntList(String string) {
        ArrayList<Integer> list = new ArrayList();
        SimpleStringSplitter splitter = new SimpleStringSplitter(',');
        splitter.setString(string);
        Iterator it = splitter.iterator();
        while (it.hasNext()) {
            String item = (String) it.next();
            try {
                list.add(Integer.valueOf(Integer.parseInt(item)));
            } catch (NumberFormatException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Can't parseInt: ");
                stringBuilder.append(item);
                Slog.w(str, stringBuilder.toString());
            }
        }
        return Collections.unmodifiableList(list);
    }

    public void onStart() {
        this.mIoThread.start();
        this.mPowerStatus = 2;
        this.mProhibitMode = false;
        this.mHdmiControlEnabled = readBooleanSetting("hdmi_control_enabled", true);
        this.mMhlInputChangeEnabled = readBooleanSetting("mhl_input_switching_enabled", true);
        this.mCecController = HdmiCecController.create(this);
        if (this.mCecController != null) {
            if (this.mHdmiControlEnabled) {
                initializeCec(1);
            }
            this.mMhlController = HdmiMhlControllerStub.create(this);
            if (!this.mMhlController.isReady()) {
                Slog.i(TAG, "Device does not support MHL-control.");
            }
            this.mMhlDevices = Collections.emptyList();
            initPortInfo();
            this.mMessageValidator = new HdmiCecMessageValidator(this);
            publishBinderService("hdmi_control", new BinderService(this, null));
            if (this.mCecController != null) {
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.SCREEN_OFF");
                filter.addAction("android.intent.action.SCREEN_ON");
                filter.addAction("android.intent.action.ACTION_SHUTDOWN");
                filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
                getContext().registerReceiver(this.mHdmiControlBroadcastReceiver, filter);
                registerContentObserver();
            }
            this.mMhlController.setOption(104, 1);
            return;
        }
        Slog.i(TAG, "Device does not support HDMI-CEC.");
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            this.mTvInputManager = (TvInputManager) getContext().getSystemService("tv_input");
            this.mPowerManager = (PowerManager) getContext().getSystemService("power");
        }
    }

    TvInputManager getTvInputManager() {
        return this.mTvInputManager;
    }

    void registerTvInputCallback(TvInputCallback callback) {
        if (this.mTvInputManager != null) {
            this.mTvInputManager.registerCallback(callback, this.mHandler);
        }
    }

    void unregisterTvInputCallback(TvInputCallback callback) {
        if (this.mTvInputManager != null) {
            this.mTvInputManager.unregisterCallback(callback);
        }
    }

    PowerManager getPowerManager() {
        return this.mPowerManager;
    }

    private void onInitializeCecComplete(int initiatedBy) {
        if (this.mPowerStatus == 2) {
            this.mPowerStatus = 0;
        }
        this.mWakeUpMessageReceived = false;
        if (isTvDeviceEnabled()) {
            this.mCecController.setOption(1, tv().getAutoWakeup());
        }
        int reason = -1;
        switch (initiatedBy) {
            case 0:
                reason = 1;
                break;
            case 1:
                reason = 0;
                break;
            case 2:
            case 3:
                reason = 2;
                break;
        }
        if (reason != -1) {
            invokeVendorCommandListenersOnControlStateChanged(true, reason);
        }
    }

    private void registerContentObserver() {
        ContentResolver resolver = getContext().getContentResolver();
        for (String s : new String[]{"hdmi_control_enabled", "hdmi_control_auto_wakeup_enabled", "hdmi_control_auto_device_off_enabled", "hdmi_system_audio_control_enabled", "mhl_input_switching_enabled", "mhl_power_charge_enabled"}) {
            resolver.registerContentObserver(Global.getUriFor(s), false, this.mSettingsObserver, -1);
        }
    }

    private static int toInt(boolean enabled) {
        return enabled;
    }

    boolean readBooleanSetting(String key, boolean defVal) {
        return Global.getInt(getContext().getContentResolver(), key, toInt(defVal)) == 1;
    }

    void writeBooleanSetting(String key, boolean value) {
        Global.putInt(getContext().getContentResolver(), key, toInt(value));
    }

    private void initializeCec(int initiatedBy) {
        this.mAddressAllocated = false;
        this.mCecController.setOption(3, true);
        this.mCecController.setLanguage(this.mLanguage);
        initializeLocalDevices(initiatedBy);
    }

    @ServiceThreadOnly
    private void initializeLocalDevices(int initiatedBy) {
        assertRunOnServiceThread();
        ArrayList<HdmiCecLocalDevice> localDevices = new ArrayList();
        for (Integer type : this.mLocalDevices) {
            int type2 = type.intValue();
            HdmiCecLocalDevice localDevice = this.mCecController.getLocalDevice(type2);
            if (localDevice == null) {
                localDevice = HdmiCecLocalDevice.create(this, type2);
            }
            localDevice.init();
            localDevices.add(localDevice);
        }
        clearLocalDevices();
        allocateLogicalAddress(localDevices, initiatedBy);
    }

    @ServiceThreadOnly
    private void allocateLogicalAddress(ArrayList<HdmiCecLocalDevice> allocatingDevices, int initiatedBy) {
        assertRunOnServiceThread();
        this.mCecController.clearLogicalAddress();
        final ArrayList<HdmiCecLocalDevice> allocatedDevices = new ArrayList();
        int[] finished = new int[1];
        this.mAddressAllocated = allocatingDevices.isEmpty();
        this.mSelectRequestBuffer.clear();
        Iterator it = allocatingDevices.iterator();
        while (it.hasNext()) {
            HdmiCecLocalDevice localDevice = (HdmiCecLocalDevice) it.next();
            final HdmiCecLocalDevice hdmiCecLocalDevice = localDevice;
            final ArrayList<HdmiCecLocalDevice> arrayList = allocatingDevices;
            final int[] iArr = finished;
            final int i = initiatedBy;
            this.mCecController.allocateLogicalAddress(localDevice.getType(), localDevice.getPreferredAddress(), new AllocateAddressCallback() {
                public void onAllocated(int deviceType, int logicalAddress) {
                    if (logicalAddress == 15) {
                        String str = HdmiControlService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to allocate address:[device_type:");
                        stringBuilder.append(deviceType);
                        stringBuilder.append("]");
                        Slog.e(str, stringBuilder.toString());
                    } else {
                        hdmiCecLocalDevice.setDeviceInfo(HdmiControlService.this.createDeviceInfo(logicalAddress, deviceType, 0));
                        HdmiControlService.this.mCecController.addLocalDevice(deviceType, hdmiCecLocalDevice);
                        HdmiControlService.this.mCecController.addLogicalAddress(logicalAddress);
                        allocatedDevices.add(hdmiCecLocalDevice);
                    }
                    int size = arrayList.size();
                    int[] iArr = iArr;
                    int i = iArr[0] + 1;
                    iArr[0] = i;
                    if (size == i) {
                        HdmiControlService.this.mAddressAllocated = true;
                        if (i != 4) {
                            HdmiControlService.this.onInitializeCecComplete(i);
                        }
                        HdmiControlService.this.notifyAddressAllocated(allocatedDevices, i);
                        HdmiControlService.this.mCecMessageBuffer.processMessages();
                    }
                }
            });
        }
    }

    @ServiceThreadOnly
    private void notifyAddressAllocated(ArrayList<HdmiCecLocalDevice> devices, int initiatedBy) {
        assertRunOnServiceThread();
        Iterator it = devices.iterator();
        while (it.hasNext()) {
            HdmiCecLocalDevice device = (HdmiCecLocalDevice) it.next();
            device.handleAddressAllocated(device.getDeviceInfo().getLogicalAddress(), initiatedBy);
        }
        if (isTvDeviceEnabled()) {
            tv().setSelectRequestBuffer(this.mSelectRequestBuffer);
        }
    }

    boolean isAddressAllocated() {
        return this.mAddressAllocated;
    }

    @ServiceThreadOnly
    private void initPortInfo() {
        assertRunOnServiceThread();
        HdmiPortInfo[] cecPortInfo = null;
        if (this.mCecController != null) {
            cecPortInfo = this.mCecController.getPortInfos();
        }
        if (cecPortInfo != null) {
            int length;
            HdmiPortInfo info;
            SparseArray<HdmiPortInfo> portInfoMap = new SparseArray();
            SparseIntArray portIdMap = new SparseIntArray();
            SparseArray<HdmiDeviceInfo> portDeviceMap = new SparseArray();
            int i = 0;
            for (HdmiPortInfo info2 : cecPortInfo) {
                portIdMap.put(info2.getAddress(), info2.getId());
                portInfoMap.put(info2.getId(), info2);
                portDeviceMap.put(info2.getId(), new HdmiDeviceInfo(info2.getAddress(), info2.getId()));
            }
            this.mPortIdMap = new UnmodifiableSparseIntArray(portIdMap);
            this.mPortInfoMap = new UnmodifiableSparseArray(portInfoMap);
            this.mPortDeviceMap = new UnmodifiableSparseArray(portDeviceMap);
            HdmiPortInfo[] mhlPortInfo = this.mMhlController.getPortInfos();
            ArraySet<Integer> mhlSupportedPorts = new ArraySet(mhlPortInfo.length);
            for (HdmiPortInfo info3 : mhlPortInfo) {
                if (info3.isMhlSupported()) {
                    mhlSupportedPorts.add(Integer.valueOf(info3.getId()));
                }
            }
            if (mhlSupportedPorts.isEmpty()) {
                this.mPortInfo = Collections.unmodifiableList(Arrays.asList(cecPortInfo));
                return;
            }
            ArrayList<HdmiPortInfo> result = new ArrayList(cecPortInfo.length);
            length = cecPortInfo.length;
            while (i < length) {
                info3 = cecPortInfo[i];
                if (mhlSupportedPorts.contains(Integer.valueOf(info3.getId()))) {
                    result.add(new HdmiPortInfo(info3.getId(), info3.getType(), info3.getAddress(), info3.isCecSupported(), true, info3.isArcSupported()));
                } else {
                    result.add(info3);
                }
                i++;
            }
            this.mPortInfo = Collections.unmodifiableList(result);
        }
    }

    List<HdmiPortInfo> getPortInfo() {
        return this.mPortInfo;
    }

    HdmiPortInfo getPortInfo(int portId) {
        return (HdmiPortInfo) this.mPortInfoMap.get(portId, null);
    }

    int portIdToPath(int portId) {
        HdmiPortInfo portInfo = getPortInfo(portId);
        if (portInfo != null) {
            return portInfo.getAddress();
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot find the port info: ");
        stringBuilder.append(portId);
        Slog.e(str, stringBuilder.toString());
        return NetworkConstants.ARP_HWTYPE_RESERVED_HI;
    }

    int pathToPortId(int path) {
        return this.mPortIdMap.get(61440 & path, -1);
    }

    boolean isValidPortId(int portId) {
        return getPortInfo(portId) != null;
    }

    Looper getIoLooper() {
        return this.mIoThread.getLooper();
    }

    Looper getServiceLooper() {
        return this.mHandler.getLooper();
    }

    int getPhysicalAddress() {
        return this.mCecController.getPhysicalAddress();
    }

    int getVendorId() {
        return this.mCecController.getVendorId();
    }

    @ServiceThreadOnly
    HdmiDeviceInfo getDeviceInfo(int logicalAddress) {
        assertRunOnServiceThread();
        return tv() == null ? null : tv().getCecDeviceInfo(logicalAddress);
    }

    @ServiceThreadOnly
    HdmiDeviceInfo getDeviceInfoByPort(int port) {
        assertRunOnServiceThread();
        HdmiMhlLocalDeviceStub info = this.mMhlController.getLocalDevice(port);
        if (info != null) {
            return info.getInfo();
        }
        return null;
    }

    int getCecVersion() {
        return this.mCecController.getVersion();
    }

    boolean isConnectedToArcPort(int physicalAddress) {
        int portId = pathToPortId(physicalAddress);
        if (portId != -1) {
            return ((HdmiPortInfo) this.mPortInfoMap.get(portId)).isArcSupported();
        }
        return false;
    }

    @ServiceThreadOnly
    boolean isConnected(int portId) {
        assertRunOnServiceThread();
        return this.mCecController.isConnected(portId);
    }

    void runOnServiceThread(Runnable runnable) {
        this.mHandler.post(runnable);
    }

    void runOnServiceThreadAtFrontOfQueue(Runnable runnable) {
        this.mHandler.postAtFrontOfQueue(runnable);
    }

    private void assertRunOnServiceThread() {
        if (Looper.myLooper() != this.mHandler.getLooper()) {
            throw new IllegalStateException("Should run on service thread.");
        }
    }

    @ServiceThreadOnly
    void sendCecCommand(HdmiCecMessage command, SendMessageCallback callback) {
        assertRunOnServiceThread();
        if (this.mMessageValidator.isValid(command) == 0) {
            this.mCecController.sendCommand(command, callback);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid message type:");
        stringBuilder.append(command);
        HdmiLogger.error(stringBuilder.toString(), new Object[0]);
        if (callback != null) {
            callback.onSendCompleted(3);
        }
    }

    @ServiceThreadOnly
    void sendCecCommand(HdmiCecMessage command) {
        assertRunOnServiceThread();
        sendCecCommand(command, null);
    }

    @ServiceThreadOnly
    void maySendFeatureAbortCommand(HdmiCecMessage command, int reason) {
        assertRunOnServiceThread();
        this.mCecController.maySendFeatureAbortCommand(command, reason);
    }

    @ServiceThreadOnly
    boolean handleCecCommand(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (this.mAddressAllocated) {
            int errorCode = this.mMessageValidator.isValid(message);
            if (errorCode == 0) {
                return dispatchMessageToLocalDevice(message);
            }
            if (errorCode == 3) {
                maySendFeatureAbortCommand(message, 3);
            }
            return true;
        }
        this.mCecMessageBuffer.bufferMessage(message);
        return true;
    }

    void enableAudioReturnChannel(int portId, boolean enabled) {
        this.mCecController.enableAudioReturnChannel(portId, enabled);
    }

    @ServiceThreadOnly
    private boolean dispatchMessageToLocalDevice(HdmiCecMessage message) {
        assertRunOnServiceThread();
        for (HdmiCecLocalDevice device : this.mCecController.getLocalDeviceList()) {
            if (device.dispatchMessage(message) && message.getDestination() != 15) {
                return true;
            }
        }
        if (message.getDestination() != 15) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unhandled cec command:");
            stringBuilder.append(message);
            HdmiLogger.warning(stringBuilder.toString(), new Object[0]);
        }
        return false;
    }

    @ServiceThreadOnly
    void onHotplug(int portId, boolean connected) {
        assertRunOnServiceThread();
        if (connected && !isTvDevice()) {
            ArrayList<HdmiCecLocalDevice> localDevices = new ArrayList();
            for (Integer type : this.mLocalDevices) {
                int type2 = type.intValue();
                HdmiCecLocalDevice localDevice = this.mCecController.getLocalDevice(type2);
                if (localDevice == null) {
                    localDevice = HdmiCecLocalDevice.create(this, type2);
                    localDevice.init();
                }
                localDevices.add(localDevice);
            }
            allocateLogicalAddress(localDevices, 4);
        }
        for (HdmiCecLocalDevice device : this.mCecController.getLocalDeviceList()) {
            device.onHotplug(portId, connected);
        }
        announceHotplugEvent(portId, connected);
    }

    @ServiceThreadOnly
    void pollDevices(DevicePollingCallback callback, int sourceAddress, int pickStrategy, int retryCount) {
        assertRunOnServiceThread();
        this.mCecController.pollDevices(callback, sourceAddress, checkPollStrategy(pickStrategy), retryCount);
    }

    private int checkPollStrategy(int pickStrategy) {
        int strategy = pickStrategy & 3;
        if (strategy != 0) {
            int iterationStrategy = 196608 & pickStrategy;
            if (iterationStrategy != 0) {
                return strategy | iterationStrategy;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid iteration strategy:");
            stringBuilder.append(pickStrategy);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Invalid poll strategy:");
        stringBuilder2.append(pickStrategy);
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    List<HdmiCecLocalDevice> getAllLocalDevices() {
        assertRunOnServiceThread();
        return this.mCecController.getLocalDeviceList();
    }

    Object getServiceLock() {
        return this.mLock;
    }

    void setAudioStatus(boolean mute, int volume) {
        if (isTvDeviceEnabled() && tv().isSystemAudioActivated()) {
            AudioManager audioManager = getAudioManager();
            boolean muted = audioManager.isStreamMute(3);
            if (!mute) {
                if (muted) {
                    audioManager.setStreamMute(3, false);
                }
                if (volume >= 0 && volume <= 100) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("volume: ");
                    stringBuilder.append(volume);
                    Slog.i(str, stringBuilder.toString());
                    audioManager.setStreamVolume(3, volume, 1 | 256);
                }
            } else if (!muted) {
                audioManager.setStreamMute(3, true);
            }
        }
    }

    void announceSystemAudioModeChange(boolean enabled) {
        synchronized (this.mLock) {
            Iterator it = this.mSystemAudioModeChangeListenerRecords.iterator();
            while (it.hasNext()) {
                invokeSystemAudioModeChangeLocked(((SystemAudioModeChangeListenerRecord) it.next()).mListener, enabled);
            }
        }
    }

    private HdmiDeviceInfo createDeviceInfo(int logicalAddress, int deviceType, int powerStatus) {
        return new HdmiDeviceInfo(logicalAddress, getPhysicalAddress(), pathToPortId(getPhysicalAddress()), deviceType, getVendorId(), Build.MODEL);
    }

    @ServiceThreadOnly
    void handleMhlHotplugEvent(int portId, boolean connected) {
        assertRunOnServiceThread();
        HdmiMhlLocalDeviceStub newDevice;
        if (connected) {
            newDevice = new HdmiMhlLocalDeviceStub(this, portId);
            HdmiMhlLocalDeviceStub oldDevice = this.mMhlController.addLocalDevice(newDevice);
            if (oldDevice != null) {
                oldDevice.onDeviceRemoved();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Old device of port ");
                stringBuilder.append(portId);
                stringBuilder.append(" is removed");
                Slog.i(str, stringBuilder.toString());
            }
            invokeDeviceEventListeners(newDevice.getInfo(), 1);
            updateSafeMhlInput();
        } else {
            newDevice = this.mMhlController.removeLocalDevice(portId);
            if (newDevice != null) {
                newDevice.onDeviceRemoved();
                invokeDeviceEventListeners(newDevice.getInfo(), 2);
                updateSafeMhlInput();
            } else {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("No device to remove:[portId=");
                stringBuilder2.append(portId);
                Slog.w(str2, stringBuilder2.toString());
            }
        }
        announceHotplugEvent(portId, connected);
    }

    @ServiceThreadOnly
    void handleMhlBusModeChanged(int portId, int busmode) {
        assertRunOnServiceThread();
        HdmiMhlLocalDeviceStub device = this.mMhlController.getLocalDevice(portId);
        if (device != null) {
            device.setBusMode(busmode);
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No mhl device exists for bus mode change[portId:");
        stringBuilder.append(portId);
        stringBuilder.append(", busmode:");
        stringBuilder.append(busmode);
        stringBuilder.append("]");
        Slog.w(str, stringBuilder.toString());
    }

    @ServiceThreadOnly
    void handleMhlBusOvercurrent(int portId, boolean on) {
        assertRunOnServiceThread();
        HdmiMhlLocalDeviceStub device = this.mMhlController.getLocalDevice(portId);
        if (device != null) {
            device.onBusOvercurrentDetected(on);
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No mhl device exists for bus overcurrent event[portId:");
        stringBuilder.append(portId);
        stringBuilder.append("]");
        Slog.w(str, stringBuilder.toString());
    }

    @ServiceThreadOnly
    void handleMhlDeviceStatusChanged(int portId, int adopterId, int deviceId) {
        assertRunOnServiceThread();
        HdmiMhlLocalDeviceStub device = this.mMhlController.getLocalDevice(portId);
        if (device != null) {
            device.setDeviceStatusChange(adopterId, deviceId);
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No mhl device exists for device status event[portId:");
        stringBuilder.append(portId);
        stringBuilder.append(", adopterId:");
        stringBuilder.append(adopterId);
        stringBuilder.append(", deviceId:");
        stringBuilder.append(deviceId);
        stringBuilder.append("]");
        Slog.w(str, stringBuilder.toString());
    }

    @ServiceThreadOnly
    private void updateSafeMhlInput() {
        assertRunOnServiceThread();
        List<HdmiDeviceInfo> inputs = Collections.emptyList();
        SparseArray<HdmiMhlLocalDeviceStub> devices = this.mMhlController.getAllLocalDevices();
        for (int i = 0; i < devices.size(); i++) {
            HdmiMhlLocalDeviceStub device = (HdmiMhlLocalDeviceStub) devices.valueAt(i);
            if (device.getInfo() != null) {
                if (inputs.isEmpty()) {
                    inputs = new ArrayList();
                }
                inputs.add(device.getInfo());
            }
        }
        synchronized (this.mLock) {
            this.mMhlDevices = inputs;
        }
    }

    @GuardedBy("mLock")
    private List<HdmiDeviceInfo> getMhlDevicesLocked() {
        return this.mMhlDevices;
    }

    private void enforceAccessPermission() {
        getContext().enforceCallingOrSelfPermission(PERMISSION, TAG);
    }

    @ServiceThreadOnly
    private void oneTouchPlay(IHdmiControlCallback callback) {
        assertRunOnServiceThread();
        HdmiCecLocalDevicePlayback source = playback();
        if (source == null) {
            Slog.w(TAG, "Local playback device not available");
            invokeCallback(callback, 2);
            return;
        }
        source.oneTouchPlay(callback);
    }

    @ServiceThreadOnly
    private void queryDisplayStatus(IHdmiControlCallback callback) {
        assertRunOnServiceThread();
        HdmiCecLocalDevicePlayback source = playback();
        if (source == null) {
            Slog.w(TAG, "Local playback device not available");
            invokeCallback(callback, 2);
            return;
        }
        source.queryDisplayStatus(callback);
    }

    private void addHotplugEventListener(final IHdmiHotplugEventListener listener) {
        final HotplugEventListenerRecord record = new HotplugEventListenerRecord(listener);
        try {
            listener.asBinder().linkToDeath(record, 0);
            synchronized (this.mLock) {
                this.mHotplugEventListenerRecords.add(record);
            }
            runOnServiceThread(new Runnable() {
                /* JADX WARNING: Missing block: B:8:0x0018, code skipped:
            r0 = com.android.server.hdmi.HdmiControlService.access$4500(r6.this$0).iterator();
     */
                /* JADX WARNING: Missing block: B:10:0x0026, code skipped:
            if (r0.hasNext() == false) goto L_0x0058;
     */
                /* JADX WARNING: Missing block: B:11:0x0028, code skipped:
            r1 = (android.hardware.hdmi.HdmiPortInfo) r0.next();
            r2 = new android.hardware.hdmi.HdmiHotplugEvent(r1.getId(), com.android.server.hdmi.HdmiControlService.access$1100(r6.this$0).isConnected(r1.getId()));
            r3 = com.android.server.hdmi.HdmiControlService.access$2100(r6.this$0);
     */
                /* JADX WARNING: Missing block: B:12:0x004b, code skipped:
            monitor-enter(r3);
     */
                /* JADX WARNING: Missing block: B:14:?, code skipped:
            com.android.server.hdmi.HdmiControlService.access$4700(r6.this$0, r5, r2);
     */
                /* JADX WARNING: Missing block: B:15:0x0053, code skipped:
            monitor-exit(r3);
     */
                /* JADX WARNING: Missing block: B:20:0x0058, code skipped:
            return;
     */
                /* Code decompiled incorrectly, please refer to instructions dump. */
                public void run() {
                    synchronized (HdmiControlService.this.mLock) {
                        if (!HdmiControlService.this.mHotplugEventListenerRecords.contains(record)) {
                        }
                    }
                }
            });
        } catch (RemoteException e) {
            Slog.w(TAG, "Listener already died");
        }
    }

    private void removeHotplugEventListener(IHdmiHotplugEventListener listener) {
        synchronized (this.mLock) {
            Iterator it = this.mHotplugEventListenerRecords.iterator();
            while (it.hasNext()) {
                HotplugEventListenerRecord record = (HotplugEventListenerRecord) it.next();
                if (record.mListener.asBinder() == listener.asBinder()) {
                    listener.asBinder().unlinkToDeath(record, 0);
                    this.mHotplugEventListenerRecords.remove(record);
                    break;
                }
            }
        }
    }

    private void addDeviceEventListener(IHdmiDeviceEventListener listener) {
        DeviceEventListenerRecord record = new DeviceEventListenerRecord(listener);
        try {
            listener.asBinder().linkToDeath(record, 0);
            synchronized (this.mLock) {
                this.mDeviceEventListenerRecords.add(record);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Listener already died");
        }
    }

    void invokeDeviceEventListeners(HdmiDeviceInfo device, int status) {
        synchronized (this.mLock) {
            Iterator it = this.mDeviceEventListenerRecords.iterator();
            while (it.hasNext()) {
                try {
                    ((DeviceEventListenerRecord) it.next()).mListener.onStatusChanged(device, status);
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to report device event:");
                    stringBuilder.append(e);
                    Slog.e(str, stringBuilder.toString());
                }
            }
        }
    }

    private void addSystemAudioModeChangeListner(IHdmiSystemAudioModeChangeListener listener) {
        SystemAudioModeChangeListenerRecord record = new SystemAudioModeChangeListenerRecord(listener);
        try {
            listener.asBinder().linkToDeath(record, 0);
            synchronized (this.mLock) {
                this.mSystemAudioModeChangeListenerRecords.add(record);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Listener already died");
        }
    }

    private void removeSystemAudioModeChangeListener(IHdmiSystemAudioModeChangeListener listener) {
        synchronized (this.mLock) {
            Iterator it = this.mSystemAudioModeChangeListenerRecords.iterator();
            while (it.hasNext()) {
                SystemAudioModeChangeListenerRecord record = (SystemAudioModeChangeListenerRecord) it.next();
                if (record.mListener.asBinder() == listener) {
                    listener.asBinder().unlinkToDeath(record, 0);
                    this.mSystemAudioModeChangeListenerRecords.remove(record);
                    break;
                }
            }
        }
    }

    private void setInputChangeListener(IHdmiInputChangeListener listener) {
        synchronized (this.mLock) {
            this.mInputChangeListenerRecord = new InputChangeListenerRecord(listener);
            try {
                listener.asBinder().linkToDeath(this.mInputChangeListenerRecord, 0);
            } catch (RemoteException e) {
                Slog.w(TAG, "Listener already died");
            }
        }
    }

    void invokeInputChangeListener(HdmiDeviceInfo info) {
        synchronized (this.mLock) {
            if (this.mInputChangeListenerRecord != null) {
                try {
                    this.mInputChangeListenerRecord.mListener.onChanged(info);
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception thrown by IHdmiInputChangeListener: ");
                    stringBuilder.append(e);
                    Slog.w(str, stringBuilder.toString());
                }
            }
        }
    }

    private void setHdmiRecordListener(IHdmiRecordListener listener) {
        synchronized (this.mLock) {
            this.mRecordListenerRecord = new HdmiRecordListenerRecord(listener);
            try {
                listener.asBinder().linkToDeath(this.mRecordListenerRecord, 0);
            } catch (RemoteException e) {
                Slog.w(TAG, "Listener already died.", e);
            }
        }
    }

    byte[] invokeRecordRequestListener(int recorderAddress) {
        synchronized (this.mLock) {
            byte[] oneTouchRecordSource;
            if (this.mRecordListenerRecord != null) {
                try {
                    oneTouchRecordSource = this.mRecordListenerRecord.mListener.getOneTouchRecordSource(recorderAddress);
                    return oneTouchRecordSource;
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to start record.", e);
                }
            }
            oneTouchRecordSource = EmptyArray.BYTE;
            return oneTouchRecordSource;
        }
    }

    void invokeOneTouchRecordResult(int recorderAddress, int result) {
        synchronized (this.mLock) {
            if (this.mRecordListenerRecord != null) {
                try {
                    this.mRecordListenerRecord.mListener.onOneTouchRecordResult(recorderAddress, result);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to call onOneTouchRecordResult.", e);
                }
            }
        }
    }

    void invokeTimerRecordingResult(int recorderAddress, int result) {
        synchronized (this.mLock) {
            if (this.mRecordListenerRecord != null) {
                try {
                    this.mRecordListenerRecord.mListener.onTimerRecordingResult(recorderAddress, result);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to call onTimerRecordingResult.", e);
                }
            }
        }
    }

    void invokeClearTimerRecordingResult(int recorderAddress, int result) {
        synchronized (this.mLock) {
            if (this.mRecordListenerRecord != null) {
                try {
                    this.mRecordListenerRecord.mListener.onClearTimerRecordingResult(recorderAddress, result);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to call onClearTimerRecordingResult.", e);
                }
            }
        }
    }

    private void invokeCallback(IHdmiControlCallback callback, int result) {
        try {
            callback.onComplete(result);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invoking callback failed:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        }
    }

    private void invokeSystemAudioModeChangeLocked(IHdmiSystemAudioModeChangeListener listener, boolean enabled) {
        try {
            listener.onStatusChanged(enabled);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invoking callback failed:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        }
    }

    private void announceHotplugEvent(int portId, boolean connected) {
        HdmiHotplugEvent event = new HdmiHotplugEvent(portId, connected);
        synchronized (this.mLock) {
            Iterator it = this.mHotplugEventListenerRecords.iterator();
            while (it.hasNext()) {
                invokeHotplugEventListenerLocked(((HotplugEventListenerRecord) it.next()).mListener, event);
            }
        }
    }

    private void invokeHotplugEventListenerLocked(IHdmiHotplugEventListener listener, HdmiHotplugEvent event) {
        try {
            listener.onReceived(event);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to report hotplug event:");
            stringBuilder.append(event.toString());
            Slog.e(str, stringBuilder.toString(), e);
        }
    }

    public HdmiCecLocalDeviceTv tv() {
        return (HdmiCecLocalDeviceTv) this.mCecController.getLocalDevice(0);
    }

    boolean isTvDevice() {
        return this.mLocalDevices.contains(Integer.valueOf(0));
    }

    boolean isTvDeviceEnabled() {
        return isTvDevice() && tv() != null;
    }

    private HdmiCecLocalDevicePlayback playback() {
        return (HdmiCecLocalDevicePlayback) this.mCecController.getLocalDevice(4);
    }

    AudioManager getAudioManager() {
        return (AudioManager) getContext().getSystemService("audio");
    }

    boolean isControlEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mHdmiControlEnabled;
        }
        return z;
    }

    @ServiceThreadOnly
    int getPowerStatus() {
        assertRunOnServiceThread();
        return this.mPowerStatus;
    }

    @ServiceThreadOnly
    boolean isPowerOnOrTransient() {
        assertRunOnServiceThread();
        return this.mPowerStatus == 0 || this.mPowerStatus == 2;
    }

    @ServiceThreadOnly
    boolean isPowerStandbyOrTransient() {
        assertRunOnServiceThread();
        return this.mPowerStatus == 1 || this.mPowerStatus == 3;
    }

    @ServiceThreadOnly
    boolean isPowerStandby() {
        assertRunOnServiceThread();
        return this.mPowerStatus == 1;
    }

    @ServiceThreadOnly
    void wakeUp() {
        assertRunOnServiceThread();
        this.mWakeUpMessageReceived = true;
        this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.server.hdmi:WAKE");
    }

    @ServiceThreadOnly
    void standby() {
        assertRunOnServiceThread();
        if (canGoToStandby()) {
            this.mStandbyMessageReceived = true;
            this.mPowerManager.goToSleep(SystemClock.uptimeMillis(), 5, 0);
        }
    }

    boolean isWakeUpMessageReceived() {
        return this.mWakeUpMessageReceived;
    }

    @ServiceThreadOnly
    private void onWakeUp() {
        assertRunOnServiceThread();
        this.mPowerStatus = 2;
        if (this.mCecController == null) {
            Slog.i(TAG, "Device does not support HDMI-CEC.");
        } else if (this.mHdmiControlEnabled) {
            int startReason = 2;
            if (this.mWakeUpMessageReceived) {
                startReason = 3;
            }
            initializeCec(startReason);
        }
    }

    @ServiceThreadOnly
    private void onStandby(final int standbyAction) {
        assertRunOnServiceThread();
        this.mPowerStatus = 3;
        invokeVendorCommandListenersOnControlStateChanged(false, 3);
        if (canGoToStandby()) {
            final List<HdmiCecLocalDevice> devices = getAllLocalDevices();
            disableDevices(new PendingActionClearedCallback() {
                public void onCleared(HdmiCecLocalDevice device) {
                    String str = HdmiControlService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("On standby-action cleared:");
                    stringBuilder.append(device.mDeviceType);
                    Slog.v(str, stringBuilder.toString());
                    devices.remove(device);
                    if (devices.isEmpty()) {
                        HdmiControlService.this.onStandbyCompleted(standbyAction);
                    }
                }
            });
            return;
        }
        this.mPowerStatus = 1;
    }

    private boolean canGoToStandby() {
        for (HdmiCecLocalDevice device : this.mCecController.getLocalDeviceList()) {
            if (!device.canGoToStandby()) {
                return false;
            }
        }
        return true;
    }

    @ServiceThreadOnly
    private void onLanguageChanged(String language) {
        assertRunOnServiceThread();
        this.mLanguage = language;
        if (isTvDeviceEnabled()) {
            tv().broadcastMenuLanguage(language);
            this.mCecController.setLanguage(language);
        }
    }

    @ServiceThreadOnly
    String getLanguage() {
        assertRunOnServiceThread();
        return this.mLanguage;
    }

    private void disableDevices(PendingActionClearedCallback callback) {
        if (this.mCecController != null) {
            for (HdmiCecLocalDevice device : this.mCecController.getLocalDeviceList()) {
                device.disableDevice(this.mStandbyMessageReceived, callback);
            }
        }
        this.mMhlController.clearAllLocalDevices();
    }

    @ServiceThreadOnly
    private void clearLocalDevices() {
        assertRunOnServiceThread();
        if (this.mCecController != null) {
            this.mCecController.clearLogicalAddress();
            this.mCecController.clearLocalDevices();
        }
    }

    @ServiceThreadOnly
    private void onStandbyCompleted(int standbyAction) {
        assertRunOnServiceThread();
        Slog.v(TAG, "onStandbyCompleted");
        if (this.mPowerStatus == 3) {
            this.mPowerStatus = 1;
            for (HdmiCecLocalDevice device : this.mCecController.getLocalDeviceList()) {
                device.onStandby(this.mStandbyMessageReceived, standbyAction);
            }
            this.mStandbyMessageReceived = false;
            this.mCecController.setOption(3, false);
            this.mMhlController.setOption(104, 0);
        }
    }

    private void addVendorCommandListener(IHdmiVendorCommandListener listener, int deviceType) {
        VendorCommandListenerRecord record = new VendorCommandListenerRecord(listener, deviceType);
        try {
            listener.asBinder().linkToDeath(record, 0);
            synchronized (this.mLock) {
                this.mVendorCommandListenerRecords.add(record);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Listener already died");
        }
    }

    boolean invokeVendorCommandListenersOnReceived(int deviceType, int srcAddress, int destAddress, byte[] params, boolean hasVendorId) {
        synchronized (this.mLock) {
            if (this.mVendorCommandListenerRecords.isEmpty()) {
                return false;
            }
            Iterator it = this.mVendorCommandListenerRecords.iterator();
            while (it.hasNext()) {
                VendorCommandListenerRecord record = (VendorCommandListenerRecord) it.next();
                if (record.mDeviceType == deviceType) {
                    try {
                        record.mListener.onReceived(srcAddress, destAddress, params, hasVendorId);
                    } catch (RemoteException e) {
                        Slog.e(TAG, "Failed to notify vendor command reception", e);
                    }
                }
            }
            return true;
        }
    }

    boolean invokeVendorCommandListenersOnControlStateChanged(boolean enabled, int reason) {
        synchronized (this.mLock) {
            if (this.mVendorCommandListenerRecords.isEmpty()) {
                return false;
            }
            Iterator it = this.mVendorCommandListenerRecords.iterator();
            while (it.hasNext()) {
                try {
                    ((VendorCommandListenerRecord) it.next()).mListener.onControlStateChanged(enabled, reason);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to notify control-state-changed to vendor handler", e);
                }
            }
            return true;
        }
    }

    private void addHdmiMhlVendorCommandListener(IHdmiMhlVendorCommandListener listener) {
        HdmiMhlVendorCommandListenerRecord record = new HdmiMhlVendorCommandListenerRecord(listener);
        try {
            listener.asBinder().linkToDeath(record, 0);
            synchronized (this.mLock) {
                this.mMhlVendorCommandListenerRecords.add(record);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Listener already died.");
        }
    }

    void invokeMhlVendorCommandListeners(int portId, int offest, int length, byte[] data) {
        synchronized (this.mLock) {
            Iterator it = this.mMhlVendorCommandListenerRecords.iterator();
            while (it.hasNext()) {
                try {
                    ((HdmiMhlVendorCommandListenerRecord) it.next()).mListener.onReceived(portId, offest, length, data);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to notify MHL vendor command", e);
                }
            }
        }
    }

    void setStandbyMode(boolean isStandbyModeOn) {
        assertRunOnServiceThread();
        if (isPowerOnOrTransient() && isStandbyModeOn) {
            this.mPowerManager.goToSleep(SystemClock.uptimeMillis(), 5, 0);
            if (playback() != null) {
                playback().sendStandby(0);
            }
        } else if (isPowerStandbyOrTransient() && !isStandbyModeOn) {
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.server.hdmi:WAKE");
            if (playback() != null) {
                oneTouchPlay(new IHdmiControlCallback.Stub() {
                    public void onComplete(int result) {
                        if (result != 0) {
                            String str = HdmiControlService.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Failed to complete 'one touch play'. result=");
                            stringBuilder.append(result);
                            Slog.w(str, stringBuilder.toString());
                        }
                    }
                });
            }
        }
    }

    boolean isProhibitMode() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mProhibitMode;
        }
        return z;
    }

    void setProhibitMode(boolean enabled) {
        synchronized (this.mLock) {
            this.mProhibitMode = enabled;
        }
    }

    @ServiceThreadOnly
    void setCecOption(int key, boolean value) {
        assertRunOnServiceThread();
        this.mCecController.setOption(key, value);
    }

    @ServiceThreadOnly
    void setControlEnabled(boolean enabled) {
        assertRunOnServiceThread();
        synchronized (this.mLock) {
            this.mHdmiControlEnabled = enabled;
        }
        if (enabled) {
            enableHdmiControlService();
            return;
        }
        invokeVendorCommandListenersOnControlStateChanged(false, 1);
        runOnServiceThread(new Runnable() {
            public void run() {
                HdmiControlService.this.disableHdmiControlService();
            }
        });
    }

    @ServiceThreadOnly
    private void enableHdmiControlService() {
        this.mCecController.setOption(3, true);
        this.mMhlController.setOption(103, 1);
        initializeCec(0);
    }

    @ServiceThreadOnly
    private void disableHdmiControlService() {
        disableDevices(new PendingActionClearedCallback() {
            public void onCleared(HdmiCecLocalDevice device) {
                HdmiControlService.this.assertRunOnServiceThread();
                HdmiControlService.this.mCecController.flush(new Runnable() {
                    public void run() {
                        HdmiControlService.this.mCecController.setOption(2, false);
                        HdmiControlService.this.mMhlController.setOption(103, 0);
                        HdmiControlService.this.clearLocalDevices();
                    }
                });
            }
        });
    }

    @ServiceThreadOnly
    void setActivePortId(int portId) {
        assertRunOnServiceThread();
        this.mActivePortId = portId;
        setLastInputForMhl(-1);
    }

    @ServiceThreadOnly
    void setLastInputForMhl(int portId) {
        assertRunOnServiceThread();
        this.mLastInputMhl = portId;
    }

    @ServiceThreadOnly
    int getLastInputForMhl() {
        assertRunOnServiceThread();
        return this.mLastInputMhl;
    }

    @ServiceThreadOnly
    void changeInputForMhl(int portId, boolean contentOn) {
        assertRunOnServiceThread();
        if (tv() != null) {
            HdmiDeviceInfo info;
            final int lastInput = contentOn ? tv().getActivePortId() : -1;
            if (portId != -1) {
                tv().doManualPortSwitching(portId, new IHdmiControlCallback.Stub() {
                    public void onComplete(int result) throws RemoteException {
                        HdmiControlService.this.setLastInputForMhl(lastInput);
                    }
                });
            }
            tv().setActivePortId(portId);
            HdmiMhlLocalDeviceStub device = this.mMhlController.getLocalDevice(portId);
            if (device != null) {
                info = device.getInfo();
            } else {
                info = (HdmiDeviceInfo) this.mPortDeviceMap.get(portId, HdmiDeviceInfo.INACTIVE_DEVICE);
            }
            invokeInputChangeListener(info);
        }
    }

    void setMhlInputChangeEnabled(boolean enabled) {
        this.mMhlController.setOption(101, toInt(enabled));
        synchronized (this.mLock) {
            this.mMhlInputChangeEnabled = enabled;
        }
    }

    boolean isMhlInputChangeEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mMhlInputChangeEnabled;
        }
        return z;
    }

    @ServiceThreadOnly
    void displayOsd(int messageId) {
        assertRunOnServiceThread();
        Intent intent = new Intent("android.hardware.hdmi.action.OSD_MESSAGE");
        intent.putExtra("android.hardware.hdmi.extra.MESSAGE_ID", messageId);
        getContext().sendBroadcastAsUser(intent, UserHandle.ALL, PERMISSION);
    }

    @ServiceThreadOnly
    void displayOsd(int messageId, int extra) {
        assertRunOnServiceThread();
        Intent intent = new Intent("android.hardware.hdmi.action.OSD_MESSAGE");
        intent.putExtra("android.hardware.hdmi.extra.MESSAGE_ID", messageId);
        intent.putExtra("android.hardware.hdmi.extra.MESSAGE_EXTRA_PARAM1", extra);
        getContext().sendBroadcastAsUser(intent, UserHandle.ALL, PERMISSION);
    }
}
