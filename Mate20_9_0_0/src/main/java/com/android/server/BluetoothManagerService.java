package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityManager.ProcessErrorStateInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AppGlobals;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.IBluetooth;
import android.bluetooth.IBluetoothCallback;
import android.bluetooth.IBluetoothGatt;
import android.bluetooth.IBluetoothHeadset;
import android.bluetooth.IBluetoothManager.Stub;
import android.bluetooth.IBluetoothManagerCallback;
import android.bluetooth.IBluetoothProfileServiceConnection;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.hdm.HwDeviceManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.os.UserManagerInternal.UserRestrictionsListener;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Slog;
import android.util.StatsLog;
import android.widget.Toast;
import com.android.internal.util.DumpUtils;
import com.android.server.HwServiceFactory.IHwBluetoothBigDataService;
import com.android.server.HwServiceFactory.IHwIMonitorManager;
import com.android.server.am.ActivityManagerService;
import com.android.server.display.DisplayTransformManager;
import com.android.server.pm.DumpState;
import com.android.server.pm.UserRestrictionsUtils;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.utils.PriorityDump;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class BluetoothManagerService extends Stub {
    private static final int ACTIVE_LOG_MAX_SIZE = 20;
    private static final int ADD_PROXY_DELAY_MS = 100;
    private static final int AIRPLANE_MODE_CHANGE_DELAY_MS = 1500;
    private static final String BLUETOOTH_ADMIN_PERM = "android.permission.BLUETOOTH_ADMIN";
    private static final int BLUETOOTH_OFF = 0;
    private static final int BLUETOOTH_ON_AIRPLANE = 2;
    private static final int BLUETOOTH_ON_BLUETOOTH = 1;
    private static final String BLUETOOTH_PERM = "android.permission.BLUETOOTH";
    private static final int CRASH_LOG_MAX_SIZE = 100;
    private static final boolean DBG = true;
    private static final int ENABLE_MESSAGE_REPEAT_MS = 1500;
    private static final int ERROR_RESTART_TIME_MS = 3000;
    private static final int MAX_ERROR_RESTART_RETRIES = 6;
    private static int MAX_RETRY_USER_SWITCHED_COUNT = 20;
    private static final int MESSAGE_ADD_PROXY_DELAYED = 400;
    private static final int MESSAGE_AIRPLANE_MODE_CHANGE = 600;
    private static final int MESSAGE_BIND_PROFILE_SERVICE = 401;
    private static final int MESSAGE_BLUETOOTH_SERVICE_CONNECTED = 40;
    private static final int MESSAGE_BLUETOOTH_SERVICE_DISCONNECTED = 41;
    private static final int MESSAGE_BLUETOOTH_STATE_CHANGE = 60;
    private static final int MESSAGE_DISABLE = 2;
    private static final int MESSAGE_DISABLE_RADIO = 4;
    private static final int MESSAGE_ENABLE = 1;
    private static final int MESSAGE_ENABLE_RADIO = 3;
    private static final int MESSAGE_GET_NAME_AND_ADDRESS = 200;
    private static final int MESSAGE_REGISTER_ADAPTER = 20;
    private static final int MESSAGE_REGISTER_STATE_CHANGE_CALLBACK = 30;
    private static final int MESSAGE_RESTART_BLUETOOTH_SERVICE = 42;
    private static final int MESSAGE_RESTORE_USER_SETTING = 500;
    private static final int MESSAGE_TIMEOUT_BIND = 100;
    private static final int MESSAGE_TIMEOUT_UNBIND = 101;
    private static final int MESSAGE_UNREGISTER_ADAPTER = 21;
    private static final int MESSAGE_UNREGISTER_STATE_CHANGE_CALLBACK = 31;
    private static final int MESSAGE_USER_SWITCHED = 300;
    private static final int MESSAGE_USER_UNLOCKED = 301;
    private static final int RESTORE_SETTING_TO_OFF = 0;
    private static final int RESTORE_SETTING_TO_ON = 1;
    private static final String SECURE_SETTINGS_BLUETOOTH_ADDRESS = "bluetooth_address";
    private static final String SECURE_SETTINGS_BLUETOOTH_ADDR_VALID = "bluetooth_addr_valid";
    private static final String SECURE_SETTINGS_BLUETOOTH_NAME = "bluetooth_name";
    private static final int SERVICE_IBLUETOOTH = 1;
    private static final int SERVICE_IBLUETOOTHGATT = 2;
    private static final int SERVICE_RESTART_TIME_MS = 200;
    private static final String TAG = "BluetoothManagerService";
    private static final int TIMEOUT_BIND_MS = 3000;
    private static final int USER_SWITCHED_TIME_MS = 200;
    private volatile boolean isAirplaneModeChanging = false;
    private final LinkedList<ActiveLog> mActiveLogs = new LinkedList();
    private String mAddress;
    private final ContentObserver mAirplaneModeObserver = new ContentObserver(null) {
        public void onChange(boolean unused) {
            synchronized (this) {
                if (HwDeviceManager.disallowOp(51)) {
                    Slog.w(BluetoothManagerService.TAG, "mdm force open bluetooth, not allow airplane close bluetooth");
                    return;
                }
                String str = BluetoothManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("receiver Airplane Mode change isAirplaneModeChanging: ");
                stringBuilder.append(BluetoothManagerService.this.isAirplaneModeChanging);
                Slog.d(str, stringBuilder.toString());
                if (BluetoothManagerService.this.isAirplaneModeChanging) {
                    BluetoothManagerService.this.mHandler.removeMessages(600);
                    BluetoothManagerService.this.mHandler.sendEmptyMessageDelayed(600, 1500);
                    return;
                }
                BluetoothManagerService.this.isAirplaneModeChanging = true;
                BluetoothManagerService.this.changeBluetoothStateFromAirplaneMode();
            }
        }
    };
    private boolean mBinding;
    private Map<IBinder, ClientDeathRecipient> mBleApps = new ConcurrentHashMap();
    private IBluetooth mBluetooth;
    private IBinder mBluetoothBinder;
    private final IBluetoothCallback mBluetoothCallback = new IBluetoothCallback.Stub() {
        public void onBluetoothStateChange(int prevState, int newState) throws RemoteException {
            String str = BluetoothManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mBluetoothCallback, onBluetoothStateChange prevState=");
            stringBuilder.append(prevState);
            stringBuilder.append(", newState=");
            stringBuilder.append(newState);
            HwLog.i(str, stringBuilder.toString());
            BluetoothManagerService.this.mHandler.sendMessage(BluetoothManagerService.this.mHandler.obtainMessage(60, prevState, newState));
        }
    };
    private IBluetoothGatt mBluetoothGatt;
    private final ReentrantReadWriteLock mBluetoothLock = new ReentrantReadWriteLock();
    private final BluetoothServiceStateCallback mBluetoothServiceStateCallback;
    private final RemoteCallbackList<IBluetoothManagerCallback> mCallbacks;
    private BluetoothServiceConnection mConnection = new BluetoothServiceConnection(this, null);
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private final LinkedList<Long> mCrashTimestamps = new LinkedList();
    private int mCrashes;
    private boolean mEnable;
    private boolean mEnableExternal;
    private boolean mEnableForNameAndAddress;
    private int mErrorRecoveryRetryCounter;
    private final BluetoothHandler mHandler = new BluetoothHandler(IoThread.get().getLooper());
    private long mLastEnableMessageTime;
    private long mLastEnabledTime;
    private int mLastMessage;
    private boolean mLastQuietMode;
    private String mName;
    private final boolean mPermissionReviewRequired;
    private final Map<Integer, ProfileServiceConnections> mProfileServices = new HashMap();
    private boolean mQuietEnable = false;
    private boolean mQuietEnableExternal;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String newName;
            StringBuilder stringBuilder;
            String rexpMac;
            String str;
            StringBuilder stringBuilder2;
            String str2;
            if ("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED".equals(action)) {
                newName = intent.getStringExtra("android.bluetooth.adapter.extra.LOCAL_NAME");
                String str3 = BluetoothManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Bluetooth Adapter name changed to ");
                stringBuilder.append(newName);
                Slog.d(str3, stringBuilder.toString());
                if (newName != null) {
                    BluetoothManagerService.this.storeNameAndAddress(newName, null);
                }
            } else if ("android.bluetooth.adapter.action.BLUETOOTH_ADDRESS_CHANGED".equals(action)) {
                newName = intent.getStringExtra("android.bluetooth.adapter.extra.BLUETOOTH_ADDRESS");
                if (newName != null) {
                    rexpMac = ":[A-Fa-f0-9]{2}:[A-Fa-f0-9]{2}:[A-Fa-f0-9]{2}:[A-Fa-f0-9]{2}:";
                    str = BluetoothManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Bluetooth Adapter address changed to ");
                    stringBuilder2.append(BluetoothManagerService.this.getPartMacAddress(newName));
                    Slog.d(str, stringBuilder2.toString());
                    BluetoothManagerService.this.storeNameAndAddress(null, newName);
                } else {
                    Slog.e(BluetoothManagerService.TAG, "No Bluetooth Adapter address parameter found");
                }
                str2 = BluetoothManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mEnableForNameAndAddress = ");
                stringBuilder.append(BluetoothManagerService.this.mEnableForNameAndAddress);
                HwLog.d(str2, stringBuilder.toString());
                if (BluetoothManagerService.this.mEnableForNameAndAddress && BluetoothManagerService.this.getState() == 15) {
                    str2 = BluetoothManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("get name and address, and disable bluetooth, state = ");
                    stringBuilder.append(BluetoothManagerService.this.getState());
                    HwLog.d(str2, stringBuilder.toString());
                    BluetoothManagerService.this.mEnableForNameAndAddress = false;
                    BluetoothManagerService.this.mEnable = false;
                    BluetoothManagerService.this.sendBrEdrDownCallback();
                }
            } else if ("android.os.action.SETTING_RESTORED".equals(action)) {
                if ("bluetooth_on".equals(intent.getStringExtra("setting_name"))) {
                    str2 = intent.getStringExtra("previous_value");
                    rexpMac = intent.getStringExtra("new_value");
                    str = BluetoothManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ACTION_SETTING_RESTORED with BLUETOOTH_ON, prevValue=");
                    stringBuilder2.append(str2);
                    stringBuilder2.append(", newValue=");
                    stringBuilder2.append(rexpMac);
                    Slog.d(str, stringBuilder2.toString());
                    if (rexpMac != null && str2 != null && !str2.equals(rexpMac)) {
                        BluetoothManagerService.this.mHandler.sendMessage(BluetoothManagerService.this.mHandler.obtainMessage(500, rexpMac.equals("0") ? 0 : 1, 0));
                    }
                }
            }
        }
    };
    private int mState;
    private final RemoteCallbackList<IBluetoothStateChangeCallback> mStateChangeCallbacks;
    private final int mSystemUiUid;
    private boolean mUnbinding;
    private final UserRestrictionsListener mUserRestrictionsListener = new UserRestrictionsListener() {
        public void onUserRestrictionsChanged(int userId, Bundle newRestrictions, Bundle prevRestrictions) {
            if (UserRestrictionsUtils.restrictionsChanged(prevRestrictions, newRestrictions, "no_bluetooth_sharing")) {
                BluetoothManagerService.this.updateOppLauncherComponentState(userId, newRestrictions.getBoolean("no_bluetooth_sharing"));
            }
            if (userId != 0 || !UserRestrictionsUtils.restrictionsChanged(prevRestrictions, newRestrictions, "no_bluetooth")) {
                return;
            }
            if (userId == 0 && newRestrictions.getBoolean("no_bluetooth")) {
                BluetoothManagerService.this.updateOppLauncherComponentState(userId, true);
                BluetoothManagerService.this.sendDisableMsg(3, BluetoothManagerService.this.mContext.getPackageName());
                return;
            }
            BluetoothManagerService.this.updateOppLauncherComponentState(userId, newRestrictions.getBoolean("no_bluetooth_sharing"));
        }
    };

    private class ActiveLog {
        private boolean mEnable;
        private String mPackageName;
        private int mReason;
        private long mTimestamp;

        ActiveLog(int reason, String packageName, boolean enable, long timestamp) {
            this.mReason = reason;
            this.mPackageName = packageName;
            this.mEnable = enable;
            this.mTimestamp = timestamp;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(BluetoothManagerService.timeToLog(this.mTimestamp));
            stringBuilder.append(this.mEnable ? "  Enabled " : " Disabled ");
            stringBuilder.append(" due to ");
            stringBuilder.append(BluetoothManagerService.getEnableDisableReasonString(this.mReason));
            stringBuilder.append(" by ");
            stringBuilder.append(this.mPackageName);
            return stringBuilder.toString();
        }
    }

    private class BluetoothHandler extends Handler {
        boolean mGetNameAddressOnly = false;

        BluetoothHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            Message message = msg;
            int i = 60;
            String str;
            StringBuilder stringBuilder;
            boolean added;
            String str2;
            StringBuilder stringBuilder2;
            StringBuilder stringBuilder3;
            ProfileServiceConnections psc;
            switch (message.what) {
                case 1:
                    str = BluetoothManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("MESSAGE_ENABLE(");
                    stringBuilder.append(message.arg1);
                    stringBuilder.append("): mBluetooth = ");
                    stringBuilder.append(BluetoothManagerService.this.mBluetooth);
                    Slog.d(str, stringBuilder.toString());
                    BluetoothManagerService.this.mHandler.removeMessages(42);
                    BluetoothManagerService.this.mEnable = true;
                    try {
                        BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                        if (BluetoothManagerService.this.mBluetooth != null && BluetoothManagerService.this.mBluetooth.getState() == 15) {
                            Slog.w(BluetoothManagerService.TAG, "BT Enable in BLE_ON State, going to ON");
                            BluetoothManagerService.this.mBluetooth.onLeServiceUp();
                            BluetoothManagerService.this.persistBluetoothSetting(1);
                            BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                            break;
                        }
                    } catch (RemoteException e) {
                        Slog.e(BluetoothManagerService.TAG, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, e);
                    } catch (Throwable th) {
                        BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                    }
                    BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                    BluetoothManagerService.this.mQuietEnable = message.arg1 == 1;
                    if (BluetoothManagerService.this.mBluetooth != null) {
                        BluetoothManagerService.this.waitForOnOff(false, true);
                        BluetoothManagerService.this.mHandler.sendMessageDelayed(BluetoothManagerService.this.mHandler.obtainMessage(42), 400);
                        break;
                    }
                    BluetoothManagerService.this.handleEnable(BluetoothManagerService.this.mQuietEnable);
                    break;
                case 2:
                    str = BluetoothManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("MESSAGE_DISABLE: mBluetooth = ");
                    stringBuilder.append(BluetoothManagerService.this.mBluetooth);
                    Slog.d(str, stringBuilder.toString());
                    BluetoothManagerService.this.mHandler.removeMessages(42);
                    if (BluetoothManagerService.this.mEnable && BluetoothManagerService.this.mBluetooth != null) {
                        BluetoothManagerService.this.waitForOnOff(true, false);
                        BluetoothManagerService.this.mEnable = false;
                        BluetoothManagerService.this.handleDisable();
                        BluetoothManagerService.this.waitForOnOff(false, false);
                        break;
                    }
                    BluetoothManagerService.this.mEnable = false;
                    BluetoothManagerService.this.handleDisable();
                    break;
                    break;
                case 3:
                    str = BluetoothManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("MESSAGE_ENABLE_RADIO: mBluetooth = ");
                    stringBuilder.append(BluetoothManagerService.this.mBluetooth);
                    HwLog.d(str, stringBuilder.toString());
                    BluetoothManagerService.this.handleEnableRadio();
                    break;
                case 4:
                    BluetoothManagerService.this.handleDisableRadio();
                    break;
                case 20:
                    IBluetoothManagerCallback callback = (IBluetoothManagerCallback) message.obj;
                    added = BluetoothManagerService.this.mCallbacks.register(callback, new Integer(message.arg1));
                    str2 = BluetoothManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Added callback: ");
                    stringBuilder2.append(callback == null ? "null" : callback);
                    stringBuilder2.append(":");
                    stringBuilder2.append(added);
                    stringBuilder2.append(" pid = ");
                    stringBuilder2.append(message.arg1);
                    HwLog.d(str2, stringBuilder2.toString());
                    break;
                case 21:
                    BluetoothManagerService.this.mCallbacks.unregister(message.obj);
                    break;
                case 30:
                    IBluetoothStateChangeCallback callback2 = (IBluetoothStateChangeCallback) message.obj;
                    added = BluetoothManagerService.this.mStateChangeCallbacks.register(callback2, new Integer(message.arg1));
                    str2 = BluetoothManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Added state change callback: ");
                    stringBuilder2.append(callback2);
                    stringBuilder2.append(":");
                    stringBuilder2.append(added);
                    stringBuilder2.append(" pid = ");
                    stringBuilder2.append(message.arg1);
                    HwLog.d(str2, stringBuilder2.toString());
                    break;
                case 31:
                    BluetoothManagerService.this.mStateChangeCallbacks.unregister(message.obj);
                    break;
                case 40:
                    str = BluetoothManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("MESSAGE_BLUETOOTH_SERVICE_CONNECTED: ");
                    stringBuilder.append(message.arg1);
                    Slog.d(str, stringBuilder.toString());
                    IBinder service = (IBinder) message.obj;
                    try {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                        if (message.arg1 != 2) {
                            BluetoothManagerService.this.mHandler.removeMessages(100);
                            BluetoothManagerService.this.mBinding = false;
                            BluetoothManagerService.this.mBluetoothBinder = service;
                            BluetoothManagerService.this.mBluetooth = IBluetooth.Stub.asInterface(Binder.allowBlocking(service));
                            if (!BluetoothManagerService.this.isNameAndAddressSet()) {
                                BluetoothManagerService.this.mHandler.sendMessage(BluetoothManagerService.this.mHandler.obtainMessage(DisplayTransformManager.LEVEL_COLOR_MATRIX_GRAYSCALE));
                                if (this.mGetNameAddressOnly) {
                                    BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                                    return;
                                }
                            }
                            BluetoothManagerService.this.mBluetooth.registerCallback(BluetoothManagerService.this.mBluetoothCallback);
                            BluetoothManagerService.this.sendBluetoothServiceUpCallback();
                            if (!BluetoothManagerService.this.mConnection.isTurnOnRadio()) {
                                try {
                                    if (BluetoothManagerService.this.mQuietEnable) {
                                        if (!BluetoothManagerService.this.mBluetooth.enableNoAutoConnect()) {
                                            Slog.e(BluetoothManagerService.TAG, "IBluetooth.enableNoAutoConnect() returned false");
                                        }
                                    } else if (!BluetoothManagerService.this.mBluetooth.enable()) {
                                        Slog.e(BluetoothManagerService.TAG, "IBluetooth.enable() returned false");
                                    }
                                } catch (RemoteException e2) {
                                    HwLog.e(BluetoothManagerService.TAG, "Unable to call enable()", e2);
                                }
                                BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                                if (!BluetoothManagerService.this.mEnable) {
                                    BluetoothManagerService.this.waitForOnOff(true, false);
                                    BluetoothManagerService.this.handleDisable();
                                    BluetoothManagerService.this.waitForOnOff(false, false);
                                    break;
                                }
                                HwLog.d(BluetoothManagerService.TAG, "re-getNameAndAddress when bt enabled!");
                                BluetoothManagerService.this.getNameAndAddress();
                                break;
                            }
                            try {
                                if (!BluetoothManagerService.this.mBluetooth.enableRadio()) {
                                    HwLog.e(BluetoothManagerService.TAG, "IBluetooth.enableRadio() returned false");
                                }
                                BluetoothManagerService.this.mConnection.setTurnOnRadio(false);
                            } catch (RemoteException e22) {
                                HwLog.e(BluetoothManagerService.TAG, "Unable to call enableRadio()", e22);
                            }
                        } else {
                            BluetoothManagerService.this.mBluetoothGatt = IBluetoothGatt.Stub.asInterface(Binder.allowBlocking(service));
                            BluetoothManagerService.this.continueFromBleOnState();
                        }
                    } catch (RemoteException e222) {
                        Slog.e(BluetoothManagerService.TAG, "Unable to register BluetoothCallback", e222);
                    } catch (Throwable th2) {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                    }
                    BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                    break;
                case 41:
                    str = BluetoothManagerService.TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("MESSAGE_BLUETOOTH_SERVICE_DISCONNECTED(");
                    stringBuilder3.append(message.arg1);
                    stringBuilder3.append(")");
                    Slog.e(str, stringBuilder3.toString());
                    try {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                        if (message.arg1 == 1) {
                            if (BluetoothManagerService.this.mBluetooth != null) {
                                BluetoothManagerService.this.mBluetooth = null;
                                BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                                BluetoothManagerService.this.addCrashLog();
                                BluetoothManagerService.this.addActiveLog(7, BluetoothManagerService.this.mContext.getPackageName(), false);
                                if (BluetoothManagerService.this.mEnable) {
                                    BluetoothManagerService.this.mEnable = false;
                                    BluetoothManagerService.this.mHandler.sendMessageDelayed(BluetoothManagerService.this.mHandler.obtainMessage(42), 200);
                                }
                                BluetoothManagerService.this.sendBluetoothServiceDownCallback();
                                if (BluetoothManagerService.this.mState == 11 || BluetoothManagerService.this.mState == 12) {
                                    BluetoothManagerService.this.bluetoothStateChangeHandler(12, 13);
                                    BluetoothManagerService.this.mState = 13;
                                }
                                if (BluetoothManagerService.this.mState == 13) {
                                    BluetoothManagerService.this.bluetoothStateChangeHandler(13, 10);
                                }
                                BluetoothManagerService.this.mHandler.removeMessages(60);
                                BluetoothManagerService.this.mState = 10;
                                break;
                            }
                        } else if (message.arg1 == 2) {
                            BluetoothManagerService.this.mBluetoothGatt = null;
                        } else {
                            Slog.e(BluetoothManagerService.TAG, "Unknown argument for service disconnect!");
                        }
                        BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                        break;
                    } catch (Throwable th3) {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                    }
                case 42:
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_RESTART_BLUETOOTH_SERVICE");
                    BluetoothManagerService.this.mEnable = true;
                    BluetoothManagerService.this.addActiveLog(4, BluetoothManagerService.this.mContext.getPackageName(), true);
                    BluetoothManagerService.this.handleEnable(BluetoothManagerService.this.mQuietEnable);
                    break;
                case 60:
                    int prevState = message.arg1;
                    i = message.arg2;
                    String str3;
                    StringBuilder stringBuilder4;
                    if (prevState != 14 || i != 15 || !BluetoothManagerService.this.mEnableForNameAndAddress) {
                        str3 = BluetoothManagerService.TAG;
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("MESSAGE_BLUETOOTH_STATE_CHANGE: ");
                        stringBuilder4.append(BluetoothAdapter.nameForState(prevState));
                        stringBuilder4.append(" > ");
                        stringBuilder4.append(BluetoothAdapter.nameForState(i));
                        Slog.d(str3, stringBuilder4.toString());
                        BluetoothManagerService.this.mState = i;
                        BluetoothManagerService.this.bluetoothStateChangeHandler(prevState, i);
                        if (prevState == 14 && i == 10 && BluetoothManagerService.this.mBluetooth != null && BluetoothManagerService.this.mEnable) {
                            BluetoothManagerService.this.recoverBluetoothServiceFromError(false);
                        }
                        if (prevState == 11 && i == 15 && BluetoothManagerService.this.mBluetooth != null && BluetoothManagerService.this.mEnable) {
                            BluetoothManagerService.this.recoverBluetoothServiceFromError(true);
                        }
                        if (prevState == 16 && i == 10 && BluetoothManagerService.this.mEnable) {
                            Slog.d(BluetoothManagerService.TAG, "Entering STATE_OFF but mEnabled is true; restarting.");
                            BluetoothManagerService.this.waitForOnOff(false, true);
                            BluetoothManagerService.this.mHandler.sendMessageDelayed(BluetoothManagerService.this.mHandler.obtainMessage(42), 400);
                        }
                        if ((i == 12 || i == 15) && BluetoothManagerService.this.mErrorRecoveryRetryCounter != 0) {
                            Slog.w(BluetoothManagerService.TAG, "bluetooth is recovered from error");
                            BluetoothManagerService.this.mErrorRecoveryRetryCounter = 0;
                        }
                        if (BluetoothManagerService.this.isAirplaneModeChanging && (i == 12 || i == 10)) {
                            Slog.d(BluetoothManagerService.TAG, "Entering STATE_OFF but mEnabled is true; restarting.");
                            BluetoothManagerService.this.isAirplaneModeChanging = false;
                            break;
                        }
                    }
                    str3 = BluetoothManagerService.TAG;
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("mEnableForNameAndAddress = ");
                    stringBuilder4.append(BluetoothManagerService.this.mEnableForNameAndAddress);
                    HwLog.d(str3, stringBuilder4.toString());
                    if (BluetoothManagerService.this.isNameAndAddressSet()) {
                        HwLog.d(BluetoothManagerService.TAG, "get name and address, and disable bluetooth, state is ble on.");
                        BluetoothManagerService.this.mEnableForNameAndAddress = false;
                        BluetoothManagerService.this.mEnable = false;
                        BluetoothManagerService.this.sendBrEdrDownCallback();
                    }
                    return;
                    break;
                case 100:
                    Slog.e(BluetoothManagerService.TAG, "MESSAGE_TIMEOUT_BIND");
                    BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                    BluetoothManagerService.this.mBinding = false;
                    BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                    break;
                case 101:
                    HwLog.e(BluetoothManagerService.TAG, "MESSAGE_TIMEOUT_UNBIND");
                    BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                    BluetoothManagerService.this.mUnbinding = false;
                    BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                    break;
                case DisplayTransformManager.LEVEL_COLOR_MATRIX_GRAYSCALE /*200*/:
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_GET_NAME_AND_ADDRESS");
                    try {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                        if (BluetoothManagerService.this.mBluetooth == null && !BluetoothManagerService.this.mBinding) {
                            Slog.d(BluetoothManagerService.TAG, "Binding to service to get name and address");
                            this.mGetNameAddressOnly = true;
                            BluetoothManagerService.this.mHandler.sendMessageDelayed(BluetoothManagerService.this.mHandler.obtainMessage(100), 3000);
                            if (BluetoothManagerService.this.doBind(new Intent(IBluetooth.class.getName()), BluetoothManagerService.this.mConnection, 65, UserHandle.CURRENT)) {
                                BluetoothManagerService.this.mBinding = true;
                            } else {
                                BluetoothManagerService.this.mHandler.removeMessages(100);
                            }
                        } else if (BluetoothManagerService.this.mBluetooth != null) {
                            BluetoothManagerService.this.storeNameAndAddress(BluetoothManagerService.this.mBluetooth.getName(), BluetoothManagerService.this.mBluetooth.getAddress());
                            if (this.mGetNameAddressOnly && !BluetoothManagerService.this.mEnable) {
                                BluetoothManagerService.this.unbindAndFinish();
                            }
                            this.mGetNameAddressOnly = false;
                        }
                    } catch (RemoteException e2222) {
                        Slog.e(BluetoothManagerService.TAG, "Unable to grab names", e2222);
                    } catch (Throwable th4) {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                    }
                    BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                    break;
                case 300:
                    HwLog.d(BluetoothManagerService.TAG, "MESSAGE_USER_SWITCHED");
                    BluetoothManagerService.this.mHandler.removeMessages(300);
                    if (BluetoothManagerService.this.mBluetooth == null || !BluetoothManagerService.this.isEnabled()) {
                        if (BluetoothManagerService.this.mBinding || BluetoothManagerService.this.mBluetooth != null) {
                            Message userMsg = BluetoothManagerService.this.mHandler.obtainMessage(300);
                            userMsg.arg2 = 1 + message.arg2;
                            if (userMsg.arg2 > BluetoothManagerService.MAX_RETRY_USER_SWITCHED_COUNT) {
                                try {
                                    if (BluetoothManagerService.this.mBluetooth.getState() == 15) {
                                        Slog.d(BluetoothManagerService.TAG, "STOP Retry");
                                        return;
                                    }
                                } catch (RemoteException e22222) {
                                    Slog.e(BluetoothManagerService.TAG, "Unable to call getState", e22222);
                                    return;
                                }
                            }
                            BluetoothManagerService.this.mHandler.sendMessageDelayed(userMsg, 200);
                            str = BluetoothManagerService.TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Retry MESSAGE_USER_SWITCHED ");
                            stringBuilder3.append(userMsg.arg2);
                            Slog.d(str, stringBuilder3.toString());
                            break;
                        }
                    }
                    BluetoothManagerService.this.clearBleApps();
                    try {
                        BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                        if (BluetoothManagerService.this.mBluetooth != null) {
                            BluetoothManagerService.this.mBluetooth.unregisterCallback(BluetoothManagerService.this.mBluetoothCallback);
                        }
                    } catch (RemoteException e222222) {
                        Slog.e(BluetoothManagerService.TAG, "Unable to unregister", e222222);
                    } catch (Throwable th5) {
                        BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                    }
                    BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                    if (BluetoothManagerService.this.mState == 13) {
                        BluetoothManagerService.this.bluetoothStateChangeHandler(BluetoothManagerService.this.mState, 10);
                        BluetoothManagerService.this.mState = 10;
                    }
                    if (BluetoothManagerService.this.mState == 10) {
                        BluetoothManagerService.this.bluetoothStateChangeHandler(BluetoothManagerService.this.mState, 11);
                        BluetoothManagerService.this.mState = 11;
                    }
                    BluetoothManagerService.this.waitForMonitoredOnOff(true, false);
                    if (BluetoothManagerService.this.mState == 11) {
                        BluetoothManagerService.this.bluetoothStateChangeHandler(BluetoothManagerService.this.mState, 12);
                    }
                    BluetoothManagerService.this.unbindAllBluetoothProfileServices();
                    BluetoothManagerService.this.addActiveLog(8, BluetoothManagerService.this.mContext.getPackageName(), false);
                    BluetoothManagerService.this.handleDisable();
                    BluetoothManagerService.this.bluetoothStateChangeHandler(12, 13);
                    boolean didDisableTimeout = BluetoothManagerService.this.waitForMonitoredOnOff(false, true) ^ 1;
                    BluetoothManagerService.this.bluetoothStateChangeHandler(13, 10);
                    BluetoothManagerService.this.sendBluetoothServiceDownCallback();
                    try {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                        if (BluetoothManagerService.this.mBluetooth != null) {
                            BluetoothManagerService.this.mBluetooth = null;
                            BluetoothManagerService.this.mContext.unbindService(BluetoothManagerService.this.mConnection);
                        }
                        BluetoothManagerService.this.mBluetoothGatt = null;
                        if (didDisableTimeout) {
                            SystemClock.sleep(3000);
                        } else {
                            SystemClock.sleep(100);
                        }
                        BluetoothManagerService.this.mHandler.removeMessages(i);
                        BluetoothManagerService.this.mState = 10;
                        BluetoothManagerService.this.addActiveLog(8, BluetoothManagerService.this.mContext.getPackageName(), true);
                        BluetoothManagerService.this.mEnable = true;
                        BluetoothManagerService.this.handleEnable(BluetoothManagerService.this.mQuietEnable);
                        break;
                    } finally {
                        i = BluetoothManagerService.this.mBluetoothLock.writeLock();
                        i.unlock();
                    }
                    break;
                case BluetoothManagerService.MESSAGE_USER_UNLOCKED /*301*/:
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_USER_UNLOCKED");
                    BluetoothManagerService.this.mHandler.removeMessages(300);
                    if (BluetoothManagerService.this.mEnable && !BluetoothManagerService.this.mBinding && BluetoothManagerService.this.mBluetooth == null) {
                        Slog.d(BluetoothManagerService.TAG, "Enabled but not bound; retrying after unlock");
                        BluetoothManagerService.this.handleEnable(BluetoothManagerService.this.mQuietEnable);
                        break;
                    }
                case BluetoothManagerService.MESSAGE_ADD_PROXY_DELAYED /*400*/:
                    psc = (ProfileServiceConnections) BluetoothManagerService.this.mProfileServices.get(Integer.valueOf(message.arg1));
                    if (psc != null) {
                        psc.addProxy(message.obj);
                        break;
                    }
                    break;
                case BluetoothManagerService.MESSAGE_BIND_PROFILE_SERVICE /*401*/:
                    psc = message.obj;
                    removeMessages(BluetoothManagerService.MESSAGE_BIND_PROFILE_SERVICE, message.obj);
                    if (psc != null) {
                        psc.bindService();
                        break;
                    }
                    break;
                case 500:
                    if (message.arg1 != 0 || !BluetoothManagerService.this.mEnable) {
                        if (message.arg1 == 1 && !BluetoothManagerService.this.mEnable) {
                            Slog.d(BluetoothManagerService.TAG, "Restore Bluetooth state to enabled");
                            BluetoothManagerService.this.mQuietEnableExternal = false;
                            BluetoothManagerService.this.mEnableExternal = true;
                            BluetoothManagerService.this.sendEnableMsg(false, 9, BluetoothManagerService.this.mContext.getPackageName());
                            break;
                        }
                    }
                    Slog.d(BluetoothManagerService.TAG, "Restore Bluetooth state to disabled");
                    BluetoothManagerService.this.persistBluetoothSetting(0);
                    BluetoothManagerService.this.mEnableExternal = false;
                    BluetoothManagerService.this.sendDisableMsg(9, BluetoothManagerService.this.mContext.getPackageName());
                    break;
                    break;
                case 600:
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_AIRPLANE_MODE_CHANGE");
                    BluetoothManagerService.this.mHandler.removeMessages(600);
                    BluetoothManagerService.this.changeBluetoothStateFromAirplaneMode();
                    break;
            }
        }
    }

    private class BluetoothServiceConnection implements ServiceConnection {
        private boolean mGetNameAddressOnly;
        private boolean mIsTurnOnRadio;

        private BluetoothServiceConnection() {
        }

        /* synthetic */ BluetoothServiceConnection(BluetoothManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void setTurnOnRadio(boolean isTurnOnRadio) {
            this.mIsTurnOnRadio = isTurnOnRadio;
        }

        public boolean isTurnOnRadio() {
            return this.mIsTurnOnRadio;
        }

        public void setGetNameAddressOnly(boolean getOnly) {
            this.mGetNameAddressOnly = getOnly;
        }

        public boolean isGetNameAddressOnly() {
            return this.mGetNameAddressOnly;
        }

        public void onServiceConnected(ComponentName componentName, IBinder service) {
            String name = componentName.getClassName();
            String str = BluetoothManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("BluetoothServiceConnection: ");
            stringBuilder.append(name);
            Slog.d(str, stringBuilder.toString());
            Message msg = BluetoothManagerService.this.mHandler.obtainMessage(40);
            if (name.equals("com.android.bluetooth.btservice.AdapterService")) {
                msg.arg1 = 1;
            } else if (name.equals("com.android.bluetooth.gatt.GattService")) {
                msg.arg1 = 2;
            } else {
                String str2 = BluetoothManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unknown service connected: ");
                stringBuilder2.append(name);
                Slog.e(str2, stringBuilder2.toString());
                return;
            }
            msg.obj = service;
            BluetoothManagerService.this.mHandler.sendMessage(msg);
        }

        public void onServiceDisconnected(ComponentName componentName) {
            String name = componentName.getClassName();
            String str = BluetoothManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("BluetoothServiceConnection, disconnected: ");
            stringBuilder.append(name);
            Slog.d(str, stringBuilder.toString());
            Message msg = BluetoothManagerService.this.mHandler.obtainMessage(41);
            if (name.equals("com.android.bluetooth.btservice.AdapterService")) {
                msg.arg1 = 1;
            } else if (name.equals("com.android.bluetooth.gatt.GattService")) {
                msg.arg1 = 2;
            } else {
                String str2 = BluetoothManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unknown service disconnected: ");
                stringBuilder2.append(name);
                Slog.e(str2, stringBuilder2.toString());
                return;
            }
            BluetoothManagerService.this.mHandler.sendMessage(msg);
        }
    }

    private final class BluetoothServiceStateCallback {
        private static final int AUTO_LOG_BUG_TYPE_FUNCTION_FAULT = 2;
        private static final String AUTO_UPLOAD_CATEGORY_NAME = "bluetooth";
        private static final long AUTO_UPLOAD_MIN_INTERVAL_TIME = 60000;
        private static final int BINDER_CALLBACK_TIMEOUT_MS = 20000;
        private static final int BUG_TYPE_CALLBACK_TIMEOUT = 0;
        private static final int MESSAGE_BINDER_CALLBACK_TIMEOUT = 1;
        private static final String PREFIX_AUTO_UPLOAD = "prefixautoupload";
        private static final int SERVICE_DOWN = 1;
        private static final int SERVICE_UP = 0;
        public Handler mKillPidHandler;
        private long sLastAutoUploadTime;

        private BluetoothServiceStateCallback() {
            this.sLastAutoUploadTime = 0;
            this.mKillPidHandler = new Handler() {
                public void handleMessage(Message msg) {
                    if (msg.what == 1) {
                        int[] pids = new int[]{msg.arg1};
                        ActivityManagerService ams = (ActivityManagerService) ServiceManager.getService("activity");
                        String logMsg = new StringBuilder();
                        logMsg.append("mKillPidHandler---pids = ");
                        logMsg.append(pids[0]);
                        logMsg.append(" getAppName = ");
                        logMsg.append(BluetoothServiceStateCallback.this.getAppName(pids[0]));
                        logMsg = logMsg.toString();
                        HwLog.e(BluetoothManagerService.TAG, logMsg);
                        if (Process.myPid() != pids[0]) {
                            ams.killPids(pids, "BluetoothManagerService callback timeout", true);
                            BluetoothServiceStateCallback.this.autoUpload(2, 0, logMsg);
                            if (!HwServiceFactory.getHwIMonitorManager().uploadBtRadarEvent(IHwIMonitorManager.IMONITOR_BINDER_FAILED, logMsg)) {
                                HwLog.d(BluetoothManagerService.TAG, "upload MESSAGE_BINDER_CALLBACK_TIMEOUT failed!");
                            }
                        }
                    }
                }
            };
        }

        /* synthetic */ BluetoothServiceStateCallback(BluetoothManagerService x0, AnonymousClass1 x1) {
            this();
        }

        private String getAppName(int pID) {
            List<RunningAppProcessInfo> appProcessList = ((ActivityManager) BluetoothManagerService.this.mContext.getSystemService("activity")).getRunningAppProcesses();
            if (appProcessList != null) {
                for (RunningAppProcessInfo appProcess : appProcessList) {
                    if (appProcess.pid == pID) {
                        return appProcess.processName;
                    }
                }
            }
            return null;
        }

        private boolean isActiveProcess(int pID) {
            if (getAppName(pID) != null) {
                return true;
            }
            List<ProcessErrorStateInfo> appProcessErrorList = ((ActivityManager) BluetoothManagerService.this.mContext.getSystemService("activity")).getProcessesInErrorState();
            if (appProcessErrorList != null) {
                for (ProcessErrorStateInfo appProcessError : appProcessErrorList) {
                    if (appProcessError.pid == pID && appProcessError.condition == 2) {
                        return true;
                    }
                }
            }
            String str = BluetoothManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[isActiveProcess] pID: ");
            stringBuilder.append(pID);
            stringBuilder.append(" return false");
            HwLog.d(str, stringBuilder.toString());
            return false;
        }

        public void sendBluetoothStateCallback(boolean isUp) {
            int i;
            try {
                int n = BluetoothManagerService.this.mStateChangeCallbacks.beginBroadcast();
                String str = BluetoothManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Broadcasting onBluetoothStateChange(");
                stringBuilder.append(isUp);
                stringBuilder.append(") to ");
                stringBuilder.append(n);
                stringBuilder.append(" receivers.");
                HwLog.d(str, stringBuilder.toString());
                i = 0;
                while (i < n) {
                    IBluetoothStateChangeCallback currentCallback = (IBluetoothStateChangeCallback) BluetoothManagerService.this.mStateChangeCallbacks.getBroadcastItem(i);
                    Integer currentPid = (Integer) BluetoothManagerService.this.mStateChangeCallbacks.getBroadcastCookie(i);
                    Message timeoutMsg = this.mKillPidHandler.obtainMessage(1);
                    timeoutMsg.arg1 = currentPid.intValue();
                    this.mKillPidHandler.sendMessageDelayed(timeoutMsg, 20000);
                    ((IBluetoothStateChangeCallback) BluetoothManagerService.this.mStateChangeCallbacks.getBroadcastItem(i)).onBluetoothStateChange(isUp);
                    this.mKillPidHandler.removeMessages(1);
                    i++;
                }
                BluetoothManagerService.this.mStateChangeCallbacks.finishBroadcast();
            } catch (RemoteException e) {
                String str2 = BluetoothManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unable to call onBluetoothStateChange() on callback #");
                stringBuilder2.append(i);
                HwLog.e(str2, stringBuilder2.toString(), e);
            } catch (Throwable th) {
                BluetoothManagerService.this.mStateChangeCallbacks.finishBroadcast();
            }
        }

        public void sendBluetoothServiceUpCallback() {
            sendBluetoothServiceStateCallback(0);
        }

        public void sendBluetoothServiceDownCallback() {
            sendBluetoothServiceStateCallback(1);
        }

        private void autoUpload(int bugType, int sceneDef, String msg) {
            StringBuilder sb = new StringBuilder(256);
            sb.append("Package:");
            sb.append("com.android.bluetooth");
            sb.append("\n");
            sb.append("APK version:");
            sb.append("1");
            sb.append("\n");
            sb.append("Bug type:");
            sb.append(bugType);
            sb.append("\n");
            sb.append("Scene def:");
            sb.append(sceneDef);
            sb.append("\n");
            String str = BluetoothManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("autoUpload->bugType:");
            stringBuilder.append(bugType);
            stringBuilder.append("; sceneDef:");
            stringBuilder.append(sceneDef);
            stringBuilder.append("; msg:");
            stringBuilder.append(msg);
            stringBuilder.append(";");
            stringBuilder.append(PREFIX_AUTO_UPLOAD);
            HwLog.i(str, stringBuilder.toString());
            long currentTime = System.currentTimeMillis();
            if (currentTime - this.sLastAutoUploadTime < 60000) {
                HwLog.w(BluetoothManagerService.TAG, "autoUpload->trigger auto upload frequently, return directly.");
                return;
            }
            this.sLastAutoUploadTime = currentTime;
            int level = 65;
            try {
                autoUpload(AUTO_UPLOAD_CATEGORY_NAME, 65, sb.toString(), msg);
            } catch (Exception ex) {
                HwLog.e(BluetoothManagerService.TAG, "autoUpload->LogException.msg() ex:prefixautoupload", ex);
            }
        }

        private void autoUpload(String appId, int level, String header, String msg) {
            try {
                HwLog.i(BluetoothManagerService.TAG, "autoupload");
                Class<?> clazz = Class.forName("android.util.HwLogException");
                clazz.getMethod("msg", new Class[]{String.class, Integer.TYPE, String.class, String.class}).invoke(clazz.newInstance(), new Object[]{appId, Integer.valueOf(level), header, msg});
            } catch (ClassNotFoundException ex) {
                HwLog.e(BluetoothManagerService.TAG, "autoUpload->HwLogException.msg() ClassNotFoundException, ex:prefixautoupload", ex);
            } catch (NoSuchMethodException ex2) {
                HwLog.e(BluetoothManagerService.TAG, "autoUpload->HwLogException.msg() NoSuchMethodException, ex:prefixautoupload", ex2);
            } catch (Exception ex3) {
                HwLog.e(BluetoothManagerService.TAG, "autoUpload->HwLogException.msg() Exception, ex:prefixautoupload", ex3);
            }
        }

        private void sendBluetoothServiceStateCallback(int state) {
            int n = BluetoothManagerService.this.mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                IBluetoothManagerCallback currentCallback = (IBluetoothManagerCallback) BluetoothManagerService.this.mCallbacks.getBroadcastItem(i);
                Integer currentPid = (Integer) BluetoothManagerService.this.mCallbacks.getBroadcastCookie(i);
                Message timeoutMsg = this.mKillPidHandler.obtainMessage(1);
                timeoutMsg.arg1 = currentPid.intValue();
                this.mKillPidHandler.sendMessageDelayed(timeoutMsg, 20000);
                if (state == 0) {
                    try {
                        if (isActiveProcess(currentPid.intValue())) {
                            currentCallback.onBluetoothServiceUp(BluetoothManagerService.this.mBluetooth);
                            this.mKillPidHandler.removeMessages(1);
                        }
                    } catch (RemoteException e) {
                        String str = BluetoothManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unable to call onBluetoothServiceUp() on callback #");
                        stringBuilder.append(i);
                        HwLog.e(str, stringBuilder.toString(), e);
                    }
                }
                if (state == 1 && isActiveProcess(currentPid.intValue()) != null) {
                    currentCallback.onBluetoothServiceDown();
                }
                this.mKillPidHandler.removeMessages(1);
            }
            BluetoothManagerService.this.mCallbacks.finishBroadcast();
        }
    }

    class ClientDeathRecipient implements DeathRecipient {
        private String mPackageName;

        ClientDeathRecipient(String packageName) {
            this.mPackageName = packageName;
        }

        public void binderDied() {
            String str = BluetoothManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Binder is dead - unregister ");
            stringBuilder.append(this.mPackageName);
            Slog.d(str, stringBuilder.toString());
            for (Entry<IBinder, ClientDeathRecipient> entry : BluetoothManagerService.this.mBleApps.entrySet()) {
                IBinder token = (IBinder) entry.getKey();
                if (((ClientDeathRecipient) entry.getValue()).equals(this)) {
                    BluetoothManagerService.this.updateBleAppCount(token, false, this.mPackageName);
                    return;
                }
            }
        }

        public String getPackageName() {
            return this.mPackageName;
        }
    }

    private final class ProfileServiceConnections implements ServiceConnection, DeathRecipient {
        ComponentName mClassName = null;
        Intent mIntent;
        boolean mInvokingProxyCallbacks = false;
        final RemoteCallbackList<IBluetoothProfileServiceConnection> mProxies = new RemoteCallbackList();
        IBinder mService = null;

        ProfileServiceConnections(Intent intent) {
            this.mIntent = intent;
        }

        private boolean bindService() {
            String str;
            try {
                str = BluetoothManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("before bindService, unbind service with intent: ");
                stringBuilder.append(this.mIntent);
                Slog.d(str, stringBuilder.toString());
                BluetoothManagerService.this.mContext.unbindService(this);
            } catch (IllegalArgumentException e) {
                Slog.w(BluetoothManagerService.TAG, "Unable to unbind service");
            }
            if (this.mIntent != null && this.mService == null && BluetoothManagerService.this.doBind(this.mIntent, this, 0, UserHandle.CURRENT_OR_SELF)) {
                Message msg = BluetoothManagerService.this.mHandler.obtainMessage(BluetoothManagerService.MESSAGE_BIND_PROFILE_SERVICE);
                msg.obj = this;
                BluetoothManagerService.this.mHandler.sendMessageDelayed(msg, 3000);
                return true;
            }
            str = BluetoothManagerService.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unable to bind with intent: ");
            stringBuilder2.append(this.mIntent);
            Slog.w(str, stringBuilder2.toString());
            return false;
        }

        private void addProxy(IBluetoothProfileServiceConnection proxy) {
            this.mProxies.register(proxy);
            if (this.mService != null) {
                try {
                    proxy.onServiceConnected(this.mClassName, this.mService);
                } catch (RemoteException e) {
                    Slog.e(BluetoothManagerService.TAG, "Unable to connect to proxy", e);
                }
            } else if (!BluetoothManagerService.this.mHandler.hasMessages(BluetoothManagerService.MESSAGE_BIND_PROFILE_SERVICE, this)) {
                Message msg = BluetoothManagerService.this.mHandler.obtainMessage(BluetoothManagerService.MESSAGE_BIND_PROFILE_SERVICE);
                msg.obj = this;
                BluetoothManagerService.this.mHandler.sendMessage(msg);
            }
        }

        private void removeProxy(IBluetoothProfileServiceConnection proxy) {
            if (proxy == null) {
                Slog.w(BluetoothManagerService.TAG, "Trying to remove a null proxy");
            } else if (this.mProxies.unregister(proxy)) {
                try {
                    proxy.onServiceDisconnected(this.mClassName);
                } catch (RemoteException e) {
                    Slog.e(BluetoothManagerService.TAG, "Unable to disconnect proxy", e);
                }
            }
        }

        private void removeAllProxies() {
            onServiceDisconnected(this.mClassName);
            this.mProxies.kill();
        }

        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothManagerService.this.mHandler.removeMessages(BluetoothManagerService.MESSAGE_BIND_PROFILE_SERVICE, this);
            this.mService = service;
            this.mClassName = className;
            try {
                this.mService.linkToDeath(this, 0);
            } catch (RemoteException e) {
                Slog.e(BluetoothManagerService.TAG, "Unable to linkToDeath", e);
            }
            if (this.mInvokingProxyCallbacks) {
                Slog.e(BluetoothManagerService.TAG, "Proxy callbacks already in progress.");
                return;
            }
            this.mInvokingProxyCallbacks = true;
            int n = this.mProxies.beginBroadcast();
            for (int i = 0; i < n; i++) {
                try {
                    ((IBluetoothProfileServiceConnection) this.mProxies.getBroadcastItem(i)).onServiceConnected(className, service);
                } catch (RemoteException e2) {
                    Slog.e(BluetoothManagerService.TAG, "Unable to connect to proxy", e2);
                } catch (Throwable th) {
                    this.mProxies.finishBroadcast();
                    this.mInvokingProxyCallbacks = false;
                }
            }
            this.mProxies.finishBroadcast();
            this.mInvokingProxyCallbacks = false;
        }

        public void onServiceDisconnected(ComponentName className) {
            if (this.mService != null) {
                try {
                    this.mService.unlinkToDeath(this, 0);
                } catch (NoSuchElementException e) {
                    HwLog.e(BluetoothManagerService.TAG, "onServiceDisconnected Unable to unlinkToDeath", e);
                } catch (NullPointerException e2) {
                    HwLog.e(BluetoothManagerService.TAG, "onServiceDisconnected mService is null");
                    return;
                }
                this.mService = null;
                this.mClassName = null;
                if (this.mInvokingProxyCallbacks) {
                    Slog.e(BluetoothManagerService.TAG, "Proxy callbacks already in progress.");
                    return;
                }
                this.mInvokingProxyCallbacks = true;
                int n = this.mProxies.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    try {
                        ((IBluetoothProfileServiceConnection) this.mProxies.getBroadcastItem(i)).onServiceDisconnected(className);
                    } catch (RemoteException e3) {
                        Slog.e(BluetoothManagerService.TAG, "Unable to disconnect from proxy", e3);
                    } catch (Throwable th) {
                        this.mProxies.finishBroadcast();
                        this.mInvokingProxyCallbacks = false;
                    }
                }
                this.mProxies.finishBroadcast();
                this.mInvokingProxyCallbacks = false;
                try {
                    String str = BluetoothManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onServiceDisconnected, unbind service with intent: ");
                    stringBuilder.append(this.mIntent);
                    Slog.d(str, stringBuilder.toString());
                    BluetoothManagerService.this.mContext.unbindService(this);
                } catch (IllegalArgumentException e4) {
                    String str2 = BluetoothManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unable to unbind service with intent: ");
                    stringBuilder2.append(this.mIntent);
                    Slog.e(str2, stringBuilder2.toString(), e4);
                }
            }
        }

        public void binderDied() {
            String str = BluetoothManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Profile service for profile: ");
            stringBuilder.append(this.mClassName);
            stringBuilder.append(" died.");
            HwLog.w(str, stringBuilder.toString());
            onServiceDisconnected(this.mClassName);
            Message msg = BluetoothManagerService.this.mHandler.obtainMessage(BluetoothManagerService.MESSAGE_BIND_PROFILE_SERVICE);
            msg.obj = this;
            BluetoothManagerService.this.mHandler.sendMessageDelayed(msg, 3000);
        }
    }

    private static CharSequence timeToLog(long timestamp) {
        return DateFormat.format("MM-dd HH:mm:ss", timestamp);
    }

    private void changeBluetoothStateFromAirplaneMode() {
        if (isBluetoothPersistedStateOn()) {
            if (isAirplaneModeOn()) {
                persistBluetoothSetting(2);
            } else {
                persistBluetoothSetting(1);
            }
        }
        int st = 10;
        try {
            this.mBluetoothLock.readLock().lock();
            if (this.mBluetooth != null) {
                st = this.mBluetooth.getState();
            }
            this.mBluetoothLock.readLock().unlock();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Airplane Mode change - current state:  ");
            stringBuilder.append(BluetoothAdapter.nameForState(st));
            Slog.d(str, stringBuilder.toString());
            if (isAirplaneModeOn()) {
                clearBleApps();
                if (st == 15) {
                    try {
                        this.mBluetoothLock.readLock().lock();
                        if (this.mBluetooth != null) {
                            addActiveLog(2, this.mContext.getPackageName(), false);
                            this.mBluetooth.onBrEdrDown();
                            this.mEnable = false;
                            this.mEnableExternal = false;
                        }
                    } catch (RemoteException e) {
                        Slog.e(TAG, "Unable to call onBrEdrDown", e);
                    } catch (Throwable th) {
                        this.mBluetoothLock.readLock().unlock();
                        throw th;
                    }
                    this.mBluetoothLock.readLock().unlock();
                } else if (st == 12) {
                    sendDisableMsg(2, this.mContext.getPackageName());
                }
            } else if (this.mEnableExternal) {
                sendEnableMsg(this.mQuietEnableExternal, 2, this.mContext.getPackageName());
            }
        } catch (RemoteException e2) {
            Slog.e(TAG, "Unable to call getState", e2);
            this.mBluetoothLock.readLock().unlock();
        } catch (Throwable th2) {
            this.mBluetoothLock.readLock().unlock();
            throw th2;
        }
    }

    BluetoothManagerService(Context context) {
        this.mContext = context;
        this.mPermissionReviewRequired = context.getResources().getBoolean(17957000);
        this.mCrashes = 0;
        this.mBluetooth = null;
        this.mBluetoothBinder = null;
        this.mBluetoothGatt = null;
        this.mBinding = false;
        this.mUnbinding = false;
        this.mEnable = false;
        this.mState = 10;
        this.mQuietEnableExternal = false;
        this.mEnableExternal = false;
        this.mEnableForNameAndAddress = false;
        this.mAddress = null;
        this.mName = null;
        this.mErrorRecoveryRetryCounter = 0;
        this.mContentResolver = context.getContentResolver();
        this.mLastMessage = 2;
        this.mLastEnableMessageTime = SystemClock.elapsedRealtime();
        registerForBleScanModeChange();
        this.mCallbacks = new RemoteCallbackList();
        this.mStateChangeCallbacks = new RemoteCallbackList();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED");
        filter.addAction("android.bluetooth.adapter.action.BLUETOOTH_ADDRESS_CHANGED");
        filter.addAction("android.os.action.SETTING_RESTORED");
        filter.setPriority(1000);
        this.mContext.registerReceiver(this.mReceiver, filter);
        loadStoredNameAndAddress();
        if (isBluetoothPersistedStateOn()) {
            Slog.d(TAG, "Startup: Bluetooth persisted state is ON.");
            this.mEnableExternal = true;
        }
        String airplaneModeRadios = Global.getString(this.mContentResolver, "airplane_mode_radios");
        if (airplaneModeRadios == null || airplaneModeRadios.contains("bluetooth")) {
            this.mContentResolver.registerContentObserver(Global.getUriFor("airplane_mode_on"), true, this.mAirplaneModeObserver);
        }
        int systemUiUid = -1;
        try {
            if (!this.mContext.getResources().getBoolean(17956996)) {
                systemUiUid = this.mContext.getPackageManager().getPackageUidAsUser("com.android.systemui", DumpState.DUMP_DEXOPT, 0);
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Detected SystemUiUid: ");
            stringBuilder.append(Integer.toString(systemUiUid));
            Slog.d(str, stringBuilder.toString());
        } catch (NameNotFoundException e) {
            Slog.w(TAG, "Unable to resolve SystemUI's UID.", e);
        }
        this.mSystemUiUid = systemUiUid;
        this.mBluetoothServiceStateCallback = new BluetoothServiceStateCallback(this, null);
    }

    private boolean isAirplaneModeOn() {
        return Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
    }

    private boolean supportBluetoothPersistedState() {
        return this.mContext.getResources().getBoolean(17957035);
    }

    private boolean isBluetoothPersistedStateOn() {
        boolean z = false;
        if (!supportBluetoothPersistedState()) {
            return false;
        }
        int state = Global.getInt(this.mContentResolver, "bluetooth_on", -1);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bluetooth persisted state: ");
        stringBuilder.append(state);
        Slog.d(str, stringBuilder.toString());
        if (state != 0) {
            z = true;
        }
        return z;
    }

    private boolean isBluetoothPersistedStateOnBluetooth() {
        boolean z = false;
        if (!supportBluetoothPersistedState()) {
            return false;
        }
        if (Global.getInt(this.mContentResolver, "bluetooth_on", 1) == 1) {
            z = true;
        }
        return z;
    }

    private void persistBluetoothSetting(int value) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Persisting Bluetooth Setting: ");
        stringBuilder.append(value);
        Slog.d(str, stringBuilder.toString());
        long callingIdentity = Binder.clearCallingIdentity();
        Global.putInt(this.mContext.getContentResolver(), "bluetooth_on", value);
        Binder.restoreCallingIdentity(callingIdentity);
    }

    private boolean isNameAndAddressSet() {
        return this.mName != null && this.mAddress != null && this.mName.length() > 0 && this.mAddress.length() > 0;
    }

    private void loadStoredNameAndAddress() {
        Slog.d(TAG, "Loading stored name and address");
        if (this.mContext.getResources().getBoolean(17956898) && Secure.getInt(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDR_VALID, 0) == 0) {
            Slog.d(TAG, "invalid bluetooth name and address stored");
            return;
        }
        this.mName = Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_NAME);
        this.mAddress = Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDRESS);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Stored bluetooth Name=");
        stringBuilder.append(this.mName);
        stringBuilder.append(",Address=");
        stringBuilder.append(getPartMacAddress(this.mAddress));
        HwLog.d(str, stringBuilder.toString());
    }

    private String getPartMacAddress(String address) {
        String partAddress = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        if (address == null || address.isEmpty()) {
            return partAddress;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(address.substring(0, address.length() / 2));
        stringBuilder.append(":**:**:**");
        return stringBuilder.toString();
    }

    private void storeNameAndAddress(String name, String address) {
        String str;
        StringBuilder stringBuilder;
        if (name != null) {
            Secure.putString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_NAME, name);
            this.mName = name;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Stored Bluetooth name: ");
            stringBuilder.append(Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_NAME));
            Slog.d(str, stringBuilder.toString());
        }
        if (address != null) {
            Secure.putString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDRESS, address);
            this.mAddress = address;
            str = Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDRESS);
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Stored Bluetoothaddress: ");
            stringBuilder2.append(getPartMacAddress(str));
            HwLog.d(str2, stringBuilder2.toString());
        }
        if (this.mName != null && this.mAddress != null) {
            Secure.putInt(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDR_VALID, 1);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Stored bluetooth_addr_valid to 1 for ");
            stringBuilder.append(this.mName);
            HwLog.d(str, stringBuilder.toString());
        }
    }

    public IBluetooth registerAdapter(IBluetoothManagerCallback callback) {
        if (callback == null) {
            Slog.w(TAG, "Callback is null in registerAdapter");
            return null;
        }
        Message msg = this.mHandler.obtainMessage(20);
        msg.obj = callback;
        msg.arg1 = Binder.getCallingPid();
        this.mHandler.sendMessage(msg);
        return this.mBluetooth;
    }

    public void unregisterAdapter(IBluetoothManagerCallback callback) {
        if (callback == null) {
            Slog.w(TAG, "Callback is null in unregisterAdapter");
            return;
        }
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Message msg = this.mHandler.obtainMessage(21);
        msg.obj = callback;
        this.mHandler.sendMessage(msg);
    }

    public void registerStateChangeCallback(IBluetoothStateChangeCallback callback) {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (callback == null) {
            Slog.w(TAG, "registerStateChangeCallback: Callback is null!");
            return;
        }
        Message msg = this.mHandler.obtainMessage(30);
        msg.obj = callback;
        msg.arg1 = Binder.getCallingPid();
        this.mHandler.sendMessage(msg);
    }

    public void unregisterStateChangeCallback(IBluetoothStateChangeCallback callback) {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (callback == null) {
            Slog.w(TAG, "unregisterStateChangeCallback: Callback is null!");
            return;
        }
        Message msg = this.mHandler.obtainMessage(31);
        msg.obj = callback;
        this.mHandler.sendMessage(msg);
    }

    public boolean isEnabled() {
        if (Binder.getCallingUid() == 1000 || checkIfCallerIsForegroundUser()) {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth != null) {
                    boolean isEnabled = this.mBluetooth.isEnabled();
                    this.mBluetoothLock.readLock().unlock();
                    return isEnabled;
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "isEnabled()", e);
            } catch (Throwable th) {
                this.mBluetoothLock.readLock().unlock();
            }
            this.mBluetoothLock.readLock().unlock();
            return false;
        }
        Slog.w(TAG, "isEnabled(): not allowed for non-active and non system user");
        return false;
    }

    public int getState() {
        if (Binder.getCallingUid() == 1000 || checkIfCallerIsForegroundUser()) {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth != null) {
                    int state = this.mBluetooth.getState();
                    this.mBluetoothLock.readLock().unlock();
                    return state;
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "getState()", e);
            } catch (Throwable th) {
                this.mBluetoothLock.readLock().unlock();
            }
            this.mBluetoothLock.readLock().unlock();
            return 10;
        }
        Slog.w(TAG, "getState(): report OFF for non-active and non system user");
        return 10;
    }

    public boolean isBleScanAlwaysAvailable() {
        boolean z = false;
        if (isAirplaneModeOn() && !this.mEnable) {
            return false;
        }
        try {
            if (Global.getInt(this.mContentResolver, "ble_scan_always_enabled") != 0) {
                z = true;
            }
            return z;
        } catch (SettingNotFoundException e) {
            return false;
        }
    }

    private void registerForBleScanModeChange() {
        this.mContentResolver.registerContentObserver(Global.getUriFor("ble_scan_always_enabled"), false, new ContentObserver(null) {
            public void onChange(boolean selfChange) {
                if (!BluetoothManagerService.this.isBleScanAlwaysAvailable()) {
                    BluetoothManagerService.this.disableBleScanMode();
                    BluetoothManagerService.this.clearBleApps();
                    try {
                        BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                        if (BluetoothManagerService.this.mBluetooth != null) {
                            BluetoothManagerService.this.addActiveLog(1, BluetoothManagerService.this.mContext.getPackageName(), false);
                            BluetoothManagerService.this.mBluetooth.onBrEdrDown();
                        }
                    } catch (RemoteException e) {
                        Slog.e(BluetoothManagerService.TAG, "error when disabling bluetooth", e);
                    } catch (Throwable th) {
                        BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                    }
                    BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                }
            }
        });
    }

    private void disableBleScanMode() {
        try {
            this.mBluetoothLock.writeLock().lock();
            if (!(this.mBluetooth == null || this.mBluetooth.getState() == 12)) {
                Slog.d(TAG, "Reseting the mEnable flag for clean disable");
                this.mEnable = false;
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "getState()", e);
        } catch (Throwable th) {
            this.mBluetoothLock.writeLock().unlock();
        }
        this.mBluetoothLock.writeLock().unlock();
    }

    public int updateBleAppCount(IBinder token, boolean enable, String packageName) {
        StringBuilder stringBuilder;
        ClientDeathRecipient r = (ClientDeathRecipient) this.mBleApps.get(token);
        String str;
        if (r == null && enable) {
            ClientDeathRecipient deathRec = new ClientDeathRecipient(packageName);
            try {
                token.linkToDeath(deathRec, 0);
                this.mBleApps.put(token, deathRec);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Registered for death of ");
                stringBuilder.append(packageName);
                Slog.d(str, stringBuilder.toString());
            } catch (RemoteException e) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("BLE app (");
                stringBuilder2.append(packageName);
                stringBuilder2.append(") already dead!");
                throw new IllegalArgumentException(stringBuilder2.toString());
            }
        } else if (!(enable || r == null)) {
            try {
                token.unlinkToDeath(r, 0);
            } catch (NoSuchElementException ex) {
                HwLog.e(TAG, "updateBleAppCount Unable to unlinkToDeath", ex);
            }
            this.mBleApps.remove(token);
            str = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Unregistered for death of ");
            stringBuilder3.append(packageName);
            Slog.d(str, stringBuilder3.toString());
        }
        int appCount = this.mBleApps.size();
        String str2 = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append(appCount);
        stringBuilder.append(" registered Ble Apps");
        Slog.d(str2, stringBuilder.toString());
        if (appCount == 0 && this.mEnable) {
            disableBleScanMode();
        }
        if (appCount == 0 && !this.mEnableExternal) {
            sendBrEdrDownCallback();
        }
        return appCount;
    }

    private void clearBleApps() {
        this.mBleApps.clear();
    }

    public boolean isBleAppPresent() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isBleAppPresent() count: ");
        stringBuilder.append(this.mBleApps.size());
        Slog.d(str, stringBuilder.toString());
        return this.mBleApps.size() > 0;
    }

    private void continueFromBleOnState() {
        Slog.d(TAG, "continueFromBleOnState()");
        try {
            this.mBluetoothLock.readLock().lock();
            if (this.mBluetooth == null) {
                Slog.e(TAG, "onBluetoothServiceUp: mBluetooth is null!");
                this.mBluetoothLock.readLock().unlock();
                return;
            }
            if (isBluetoothPersistedStateOnBluetooth() || !isBleAppPresent()) {
                this.mBluetooth.onLeServiceUp();
                persistBluetoothSetting(1);
            }
            this.mBluetoothLock.readLock().unlock();
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to call onServiceUp", e);
        } catch (Throwable th) {
            this.mBluetoothLock.readLock().unlock();
        }
    }

    private void sendBrEdrDownCallback() {
        Slog.d(TAG, "Calling sendBrEdrDownCallback callbacks");
        if (this.mBluetooth == null) {
            Slog.w(TAG, "Bluetooth handle is null");
            return;
        }
        if (isBleAppPresent()) {
            try {
                this.mBluetoothGatt.unregAll();
            } catch (RemoteException e) {
                Slog.e(TAG, "Unable to disconnect all apps.", e);
            }
        } else {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth != null) {
                    this.mBluetooth.onBrEdrDown();
                }
            } catch (RemoteException e2) {
                Slog.e(TAG, "Call to onBrEdrDown() failed.", e2);
            } catch (Throwable th) {
                this.mBluetoothLock.readLock().unlock();
            }
            this.mBluetoothLock.readLock().unlock();
        }
    }

    public boolean isRadioEnabled() {
        boolean z;
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        synchronized (this.mConnection) {
            z = false;
            try {
                if (this.mBluetooth != null && this.mBluetooth.isRadioEnabled()) {
                    z = true;
                }
            } catch (RemoteException e) {
                HwLog.e(TAG, "isRadioEnabled()", e);
                return false;
            } catch (Throwable th) {
            }
        }
        return z;
    }

    public void getNameAndAddress() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getNameAndAddress(): mBluetooth = ");
        stringBuilder.append(this.mBluetooth);
        stringBuilder.append(" mBinding = ");
        stringBuilder.append(this.mBinding);
        HwLog.d(str, stringBuilder.toString());
        this.mHandler.sendMessage(this.mHandler.obtainMessage(DisplayTransformManager.LEVEL_COLOR_MATRIX_GRAYSCALE));
    }

    public boolean enableNoAutoConnect(String packageName) {
        if (isBluetoothDisallowed()) {
            Slog.d(TAG, "enableNoAutoConnect(): not enabling - bluetooth disallowed");
            return false;
        }
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH ADMIN permission");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enableNoAutoConnect():  mBluetooth =");
        stringBuilder.append(this.mBluetooth);
        stringBuilder.append(" mBinding = ");
        stringBuilder.append(this.mBinding);
        Slog.d(str, stringBuilder.toString());
        if (UserHandle.getAppId(Binder.getCallingUid()) == UsbTerminalTypes.TERMINAL_BIDIR_SKRPHONE) {
            synchronized (this.mReceiver) {
                this.mQuietEnableExternal = true;
                this.mEnableExternal = true;
                sendEnableMsg(true, 1, packageName);
            }
            return true;
        }
        throw new SecurityException("no permission to enable Bluetooth quietly");
    }

    public boolean enable(String packageName) throws RemoteException {
        int callingUid = Binder.getCallingUid();
        boolean callerSystem = UserHandle.getAppId(callingUid) == 1000;
        if (isBluetoothDisallowed()) {
            Slog.d(TAG, "enable(): not enabling - bluetooth disallowed");
            return false;
        } else if (HwDeviceManager.disallowOp(8)) {
            HwLog.i(TAG, "bluetooth has been restricted.");
            return false;
        } else {
            boolean needCheck = false;
            if (!callerSystem) {
                if (checkIfCallerIsForegroundUser()) {
                    this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH ADMIN permission");
                    needCheck = checkPrecondition(callingUid);
                    if (!isEnabled() && needCheck && startConsentUiIfNeeded(packageName, callingUid, "android.bluetooth.adapter.action.REQUEST_ENABLE")) {
                        return true;
                    }
                }
                Slog.w(TAG, "enable(): not allowed for non-active and non system user");
                return false;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enable(");
            stringBuilder.append(packageName);
            stringBuilder.append("):  mBluetooth =");
            stringBuilder.append(this.mBluetooth);
            stringBuilder.append(" mBinding = ");
            stringBuilder.append(this.mBinding);
            stringBuilder.append(" mState = ");
            stringBuilder.append(BluetoothAdapter.nameForState(this.mState));
            stringBuilder.append(", needCheck = ");
            stringBuilder.append(needCheck);
            Slog.d(str, stringBuilder.toString());
            HwServiceFactory.getHwBluetoothBigDataService().sendBigDataEvent(this.mContext, IHwBluetoothBigDataService.GET_OPEN_BT_APP_NAME);
            synchronized (this.mReceiver) {
                this.mQuietEnableExternal = false;
                this.mEnableExternal = true;
                sendEnableMsg(false, 1, packageName);
            }
            HwLog.d(TAG, "enable returning");
            return true;
        }
    }

    public boolean enableRadio() {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enable():  mBluetooth =");
        stringBuilder.append(this.mBluetooth == null ? "null" : this.mBluetooth);
        stringBuilder.append(" mBinding = ");
        stringBuilder.append(this.mBinding);
        HwLog.d(str, stringBuilder.toString());
        synchronized (this.mConnection) {
            if (this.mBinding) {
                HwLog.w(TAG, "enable(): binding in progress. Returning..");
                return true;
            }
            Message msg = this.mHandler.obtainMessage(3);
            msg.arg1 = 1;
            this.mHandler.sendMessage(msg);
            return true;
        }
    }

    public boolean disable(String packageName, boolean persist) throws RemoteException {
        int callingUid = Binder.getCallingUid();
        boolean needCheck = false;
        if (!(UserHandle.getAppId(callingUid) == 1000)) {
            if (checkIfCallerIsForegroundUser()) {
                this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH ADMIN permission");
                needCheck = checkPrecondition(callingUid);
                if (isEnabled() && needCheck && startConsentUiIfNeeded(packageName, callingUid, "android.bluetooth.adapter.action.REQUEST_DISABLE")) {
                    return true;
                }
            }
            Slog.w(TAG, "disable(): not allowed for non-active and non system user");
            return false;
        }
        if (HwDeviceManager.disallowOp(51) && persist) {
            Slog.w(TAG, "mdm force open bluetooth, not allow close bluetooth");
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                public void run() {
                    Toast.makeText(BluetoothManagerService.this.mContext, BluetoothManagerService.this.mContext.getResources().getString(33686053), 0).show();
                }
            });
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("disable(");
        stringBuilder.append(packageName);
        stringBuilder.append("): mBluetooth = ");
        stringBuilder.append(this.mBluetooth);
        stringBuilder.append(" mBinding = ");
        stringBuilder.append(this.mBinding);
        stringBuilder.append(",needCheck = ");
        stringBuilder.append(needCheck);
        Slog.d(str, stringBuilder.toString());
        synchronized (this.mReceiver) {
            if (persist) {
                try {
                    persistBluetoothSetting(0);
                } catch (Throwable th) {
                }
            }
            this.mEnableExternal = false;
            sendDisableMsg(1, packageName);
        }
        return true;
    }

    public boolean disableRadio() {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("disable(): mBluetooth = ");
        stringBuilder.append(this.mBluetooth == null ? "null" : this.mBluetooth);
        stringBuilder.append(" mBinding = ");
        stringBuilder.append(this.mBinding);
        HwLog.d(str, stringBuilder.toString());
        synchronized (this.mConnection) {
            if (this.mBluetooth == null) {
                return false;
            }
            this.mHandler.sendMessage(this.mHandler.obtainMessage(4));
            return true;
        }
    }

    private boolean startConsentUiIfNeeded(String packageName, int callingUid, String intentAction) throws RemoteException {
        if (checkBluetoothPermissionWhenPermissionReviewRequired()) {
            return false;
        }
        try {
            if (this.mContext.getPackageManager().getApplicationInfoAsUser(packageName, 268435456, UserHandle.getUserId(callingUid)).uid == callingUid) {
                Intent intent = new Intent(intentAction);
                intent.putExtra("android.intent.extra.PACKAGE_NAME", packageName);
                intent.setFlags(276824064);
                try {
                    this.mContext.startActivity(intent);
                    return true;
                } catch (ActivityNotFoundException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Intent to handle action ");
                    stringBuilder.append(intentAction);
                    stringBuilder.append(" missing");
                    Slog.e(str, stringBuilder.toString());
                    return false;
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Package ");
            stringBuilder2.append(packageName);
            stringBuilder2.append(" not in uid ");
            stringBuilder2.append(callingUid);
            throw new SecurityException(stringBuilder2.toString());
        } catch (NameNotFoundException e2) {
            throw new RemoteException(e2.getMessage());
        }
    }

    private boolean checkBluetoothPermissionWhenPermissionReviewRequired() {
        boolean z = false;
        if (!this.mPermissionReviewRequired) {
            return false;
        }
        if (this.mContext.checkCallingPermission("android.permission.MANAGE_BLUETOOTH_WHEN_PERMISSION_REVIEW_REQUIRED") == 0) {
            z = true;
        }
        return z;
    }

    public void unbindAndFinish() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unbindAndFinish(): ");
        stringBuilder.append(this.mBluetooth);
        stringBuilder.append(" mBinding = ");
        stringBuilder.append(this.mBinding);
        stringBuilder.append(" mUnbinding = ");
        stringBuilder.append(this.mUnbinding);
        Slog.d(str, stringBuilder.toString());
        try {
            this.mBluetoothLock.writeLock().lock();
            if (this.mUnbinding) {
                this.mBluetoothLock.writeLock().unlock();
                return;
            }
            this.mUnbinding = true;
            this.mHandler.removeMessages(60);
            this.mHandler.removeMessages(MESSAGE_BIND_PROFILE_SERVICE);
            if (this.mBluetooth != null) {
                this.mBluetooth.unregisterCallback(this.mBluetoothCallback);
                this.mBluetoothBinder = null;
                this.mBluetooth = null;
                this.mContext.unbindService(this.mConnection);
                this.mUnbinding = false;
                this.mBinding = false;
            } else {
                this.mUnbinding = false;
            }
            this.mBluetoothGatt = null;
            this.mBluetoothLock.writeLock().unlock();
        } catch (RemoteException re) {
            Slog.e(TAG, "Unable to unregister BluetoothCallback", re);
        } catch (Throwable th) {
            this.mBluetoothLock.writeLock().unlock();
        }
    }

    public IBluetoothGatt getBluetoothGatt() {
        return this.mBluetoothGatt;
    }

    /* JADX WARNING: Missing block: B:23:0x0086, code skipped:
            r0 = r7.mHandler.obtainMessage(MESSAGE_ADD_PROXY_DELAYED);
            r0.arg1 = r8;
            r0.obj = r9;
            r7.mHandler.sendMessageDelayed(r0, 100);
     */
    /* JADX WARNING: Missing block: B:24:0x0099, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean bindBluetoothProfileService(int bluetoothProfile, IBluetoothProfileServiceConnection proxy) {
        if (this.mEnable) {
            this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
            if (proxy == null) {
                HwLog.w(TAG, "proxy is null.");
                return false;
            }
            synchronized (this.mProfileServices) {
                if (((ProfileServiceConnections) this.mProfileServices.get(new Integer(bluetoothProfile))) == null) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Creating new ProfileServiceConnections object for profile: ");
                    stringBuilder.append(bluetoothProfile);
                    HwLog.d(str, stringBuilder.toString());
                    if (bluetoothProfile != 1) {
                        return false;
                    }
                    ProfileServiceConnections psc = new ProfileServiceConnections(new Intent(IBluetoothHeadset.class.getName()));
                    if (psc.bindService()) {
                        this.mProfileServices.put(new Integer(bluetoothProfile), psc);
                    } else {
                        return false;
                    }
                }
            }
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Trying to bind to profile: ");
        stringBuilder2.append(bluetoothProfile);
        stringBuilder2.append(", while Bluetooth was disabled");
        HwLog.d(str2, stringBuilder2.toString());
        return false;
    }

    public void unbindBluetoothProfileService(int bluetoothProfile, IBluetoothProfileServiceConnection proxy) {
        synchronized (this.mProfileServices) {
            ProfileServiceConnections psc = (ProfileServiceConnections) this.mProfileServices.get(new Integer(bluetoothProfile));
            if (psc == null) {
                return;
            }
            psc.removeProxy(proxy);
        }
    }

    private void unbindAllBluetoothProfileServices() {
        synchronized (this.mProfileServices) {
            for (Integer i : this.mProfileServices.keySet()) {
                ProfileServiceConnections psc = (ProfileServiceConnections) this.mProfileServices.get(i);
                try {
                    this.mContext.unbindService(psc);
                } catch (IllegalArgumentException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to unbind service with intent: ");
                    stringBuilder.append(psc.mIntent);
                    Slog.e(str, stringBuilder.toString(), e);
                }
                psc.removeAllProxies();
            }
            this.mProfileServices.clear();
        }
    }

    public void handleOnBootPhase() {
        Slog.d(TAG, "Bluetooth boot completed");
        ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).addUserRestrictionsListener(this.mUserRestrictionsListener);
        if (!isBluetoothDisallowed()) {
            if (this.mEnableExternal && isBluetoothPersistedStateOnBluetooth()) {
                Slog.d(TAG, "Auto-enabling Bluetooth.");
                sendEnableMsg(this.mQuietEnableExternal, 6, this.mContext.getPackageName());
            } else if (!isNameAndAddressSet()) {
                if ("factory".equals(SystemProperties.get("ro.runmode", "normal"))) {
                    Slog.e(TAG, "in factory mode, don't allow to enable BT");
                } else {
                    Slog.d(TAG, "Getting adapter name and address");
                    this.mEnableForNameAndAddress = true;
                    sendEnableMsg(true, 0, "get name and address");
                }
            }
        }
    }

    public void handleOnSwitchUser(int userHandle) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("User ");
        stringBuilder.append(userHandle);
        stringBuilder.append(" switched");
        Slog.d(str, stringBuilder.toString());
        this.mHandler.obtainMessage(300, userHandle, 0).sendToTarget();
    }

    public void handleOnUnlockUser(int userHandle) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("User ");
        stringBuilder.append(userHandle);
        stringBuilder.append(" unlocked");
        Slog.d(str, stringBuilder.toString());
        this.mHandler.obtainMessage(MESSAGE_USER_UNLOCKED, userHandle, 0).sendToTarget();
    }

    private void sendBluetoothStateCallback(boolean isUp) {
        this.mBluetoothServiceStateCallback.sendBluetoothStateCallback(isUp);
    }

    private void sendBluetoothServiceUpCallback() {
        HwLog.d(TAG, "Calling onBluetoothServiceUp callbacks");
        this.mBluetoothServiceStateCallback.sendBluetoothServiceUpCallback();
    }

    private void sendBluetoothServiceDownCallback() {
        HwLog.d(TAG, "Calling onBluetoothServiceDown callbacks");
        this.mBluetoothServiceStateCallback.sendBluetoothServiceDownCallback();
    }

    public String getAddress() {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (Binder.getCallingUid() != 1000 && !checkIfCallerIsForegroundUser()) {
            Slog.w(TAG, "getAddress(): not allowed for non-active and non system user");
            return null;
        } else if (this.mContext.checkCallingOrSelfPermission("android.permission.LOCAL_MAC_ADDRESS") != 0) {
            return "02:00:00:00:00:00";
        } else {
            try {
                this.mBluetoothLock.readLock().lock();
                String addr = null;
                if (this.mBluetooth != null) {
                    addr = this.mBluetooth.getAddress();
                }
                if (addr == null) {
                    addr = this.mAddress;
                } else {
                    this.mAddress = addr;
                }
                this.mBluetoothLock.readLock().unlock();
                return addr;
            } catch (RemoteException e) {
                Slog.e(TAG, "getAddress(): Unable to retrieve address remotely. Returning cached address", e);
                this.mBluetoothLock.readLock().unlock();
                return this.mAddress;
            } catch (Throwable th) {
                this.mBluetoothLock.readLock().unlock();
                throw th;
            }
        }
    }

    public String getName() {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (Binder.getCallingUid() == 1000 || checkIfCallerIsForegroundUser()) {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth != null) {
                    String name = this.mBluetooth.getName();
                    this.mBluetoothLock.readLock().unlock();
                    return name;
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "getName(): Unable to retrieve name remotely. Returning cached name", e);
            } catch (Throwable th) {
                this.mBluetoothLock.readLock().unlock();
            }
            this.mBluetoothLock.readLock().unlock();
            return this.mName;
        }
        Slog.w(TAG, "getName(): not allowed for non-active and non system user");
        return null;
    }

    private void handleEnable(boolean quietMode) {
        this.mQuietEnable = quietMode;
        try {
            this.mBluetoothLock.writeLock().lock();
            if (this.mBluetooth == null && !this.mBinding) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(100), 3000);
                if (doBind(new Intent(IBluetooth.class.getName()), this.mConnection, 65, UserHandle.CURRENT)) {
                    this.mBinding = true;
                } else {
                    this.mHandler.removeMessages(100);
                }
            } else if (this.mBluetooth != null) {
                if (!this.mQuietEnable) {
                    HwLog.i(TAG, "BT-Enable-FW handleEnable");
                    if (!this.mBluetooth.enable()) {
                        Slog.e(TAG, "IBluetooth.enable() returned false");
                    }
                } else if (!this.mBluetooth.enableNoAutoConnect()) {
                    Slog.e(TAG, "IBluetooth.enableNoAutoConnect() returned false");
                }
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to call enable()", e);
        } catch (Throwable th) {
            this.mBluetoothLock.writeLock().unlock();
        }
        this.mBluetoothLock.writeLock().unlock();
    }

    boolean doBind(Intent intent, ServiceConnection conn, int flags, UserHandle user) {
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp != null && this.mContext.bindServiceAsUser(intent, conn, flags, user)) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Fail to bind to: ");
        stringBuilder.append(intent);
        Slog.e(str, stringBuilder.toString());
        return false;
    }

    private void handleDisable() {
        try {
            this.mBluetoothLock.readLock().lock();
            if (this.mBluetooth != null) {
                Slog.d(TAG, "Sending off request.");
                if (!this.mBluetooth.disable()) {
                    Slog.e(TAG, "IBluetooth.disable() returned false");
                }
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to call disable()", e);
        } catch (Throwable th) {
            this.mBluetoothLock.readLock().unlock();
        }
        this.mBluetoothLock.readLock().unlock();
    }

    /* JADX WARNING: Removed duplicated region for block: B:18:0x003e A:{Catch:{ all -> 0x0072 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean checkIfCallerIsForegroundUser() {
        int callingUser = UserHandle.getCallingUserId();
        int callingUid = Binder.getCallingUid();
        long callingIdentity = Binder.clearCallingIdentity();
        UserInfo ui = ((UserManager) this.mContext.getSystemService("user")).getProfileParent(callingUser);
        int parentUser = ui != null ? ui.id : -10000;
        int callingAppId = UserHandle.getAppId(callingUid);
        boolean z = false;
        boolean valid = false;
        try {
            int foregroundUser = ActivityManager.getCurrentUser();
            if (!(callingUser == foregroundUser || parentUser == foregroundUser || callingAppId == UsbTerminalTypes.TERMINAL_BIDIR_SKRPHONE)) {
                if (callingAppId != this.mSystemUiUid) {
                    valid = z;
                    if (!valid) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("checkIfCallerIsForegroundUser: valid=");
                        stringBuilder.append(valid);
                        stringBuilder.append(" callingUser=");
                        stringBuilder.append(callingUser);
                        stringBuilder.append(" parentUser=");
                        stringBuilder.append(parentUser);
                        stringBuilder.append(" foregroundUser=");
                        stringBuilder.append(foregroundUser);
                        Slog.d(str, stringBuilder.toString());
                    }
                    Binder.restoreCallingIdentity(callingIdentity);
                    return valid;
                }
            }
            z = true;
            valid = z;
            if (valid) {
            }
            Binder.restoreCallingIdentity(callingIdentity);
            return valid;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private void sendBleStateChanged(int prevState, int newState) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Sending BLE State Change: ");
        stringBuilder.append(BluetoothAdapter.nameForState(prevState));
        stringBuilder.append(" > ");
        stringBuilder.append(BluetoothAdapter.nameForState(newState));
        Slog.d(str, stringBuilder.toString());
        Intent intent = new Intent("android.bluetooth.adapter.action.BLE_STATE_CHANGED");
        intent.putExtra("android.bluetooth.adapter.extra.PREVIOUS_STATE", prevState);
        intent.putExtra("android.bluetooth.adapter.extra.STATE", newState);
        intent.addFlags(67108864);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, BLUETOOTH_PERM);
    }

    private void bluetoothStateChangeHandler(int prevState, int newState) {
        boolean isStandardBroadcast = true;
        if (prevState != newState) {
            Intent intentRadio1;
            String str;
            StringBuilder stringBuilder;
            if (prevState == 10 && newState == 18) {
                intentRadio1 = new Intent("android.bluetooth.adapter.action.RADIO_STATE_CHANGED");
                intentRadio1.putExtra("android.bluetooth.adapter.extra.PREVIOUS_STATE", prevState);
                intentRadio1.putExtra("android.bluetooth.adapter.extra.STATE", newState);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("ACTION_RADIO_STATE_CHANGED, Radio State Change Intent: ");
                stringBuilder.append(prevState);
                stringBuilder.append(" -> ");
                stringBuilder.append(newState);
                HwLog.d(str, stringBuilder.toString());
                this.mContext.sendBroadcast(intentRadio1);
                sendBluetoothServiceDownCallback();
                unbindAndFinish();
                return;
            }
            boolean isUp = false;
            if (newState == 15 || newState == 10) {
                boolean intermediate_off = prevState == 13 && newState == 15;
                if (newState == 10) {
                    Slog.d(TAG, "Bluetooth is complete send Service Down");
                    sendBluetoothStateCallback(false);
                    if (!isRadioEnabled()) {
                        sendBluetoothServiceDownCallback();
                        unbindAndFinish();
                    }
                    sendBleStateChanged(prevState, newState);
                    isStandardBroadcast = false;
                } else if (!intermediate_off) {
                    Slog.d(TAG, "Bluetooth is in LE only mode");
                    if (this.mBluetoothGatt == null && this.mContext.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le")) {
                        Slog.d(TAG, "Binding Bluetooth GATT service");
                        doBind(new Intent(IBluetoothGatt.class.getName()), this.mConnection, 65, UserHandle.CURRENT);
                    } else {
                        continueFromBleOnState();
                    }
                    sendBleStateChanged(prevState, newState);
                    isStandardBroadcast = false;
                } else if (intermediate_off) {
                    Slog.d(TAG, "Intermediate off, back to LE only mode");
                    sendBleStateChanged(prevState, newState);
                    newState = 10;
                    sendBrEdrDownCallback();
                }
            } else if (newState == 12) {
                if (newState == 12) {
                    isUp = true;
                }
                sendBluetoothStateCallback(isUp);
                sendBleStateChanged(prevState, newState);
            } else if (newState == 14 || newState == 16) {
                sendBleStateChanged(prevState, newState);
                isStandardBroadcast = false;
            } else if (newState == 11 || newState == 13) {
                sendBleStateChanged(prevState, newState);
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("isStandardBroadcast=");
            stringBuilder2.append(isStandardBroadcast);
            stringBuilder2.append(", prevState=");
            stringBuilder2.append(prevState);
            stringBuilder2.append(", newState=");
            stringBuilder2.append(newState);
            HwLog.i(str2, stringBuilder2.toString());
            if (newState == 11 && prevState == 15) {
                this.mEnable = true;
            }
            if (isStandardBroadcast) {
                if (prevState == 15) {
                    prevState = 10;
                }
                if (newState == 17 || newState == 18) {
                    intentRadio1 = new Intent("android.bluetooth.adapter.action.RADIO_STATE_CHANGED");
                    intentRadio1.putExtra("android.bluetooth.adapter.extra.PREVIOUS_STATE", prevState);
                    intentRadio1.putExtra("android.bluetooth.adapter.extra.STATE", newState);
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("send ACTION_RADIO_STATE_CHANGED, Radio State Change Intent: ");
                    stringBuilder.append(prevState);
                    stringBuilder.append(" -> ");
                    stringBuilder.append(newState);
                    HwLog.d(str, stringBuilder.toString());
                    this.mContext.sendBroadcast(intentRadio1);
                } else if (newState == 15 && prevState == 13) {
                    HwLog.e(TAG, "newState is ble on,so don't send broadcast");
                } else {
                    if (newState == 10 && prevState == 16) {
                        prevState = 13;
                    }
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("send ACTION_STATE_CHANGED, newState=");
                    stringBuilder3.append(newState);
                    stringBuilder3.append(", prevState=");
                    stringBuilder3.append(prevState);
                    HwLog.i(str3, stringBuilder3.toString());
                    intentRadio1 = new Intent("android.bluetooth.adapter.action.STATE_CHANGED");
                    intentRadio1.putExtra("android.bluetooth.adapter.extra.PREVIOUS_STATE", prevState);
                    intentRadio1.putExtra("android.bluetooth.adapter.extra.STATE", newState);
                    intentRadio1.addFlags(83886080);
                    intentRadio1.addFlags(268435456);
                    this.mContext.sendBroadcastAsUser(intentRadio1, UserHandle.ALL, BLUETOOTH_PERM);
                }
            }
        }
    }

    private boolean waitForOnOff(boolean on, boolean off) {
        int i = 0;
        while (i < 10) {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth == null) {
                    this.mBluetoothLock.readLock().unlock();
                    break;
                }
                if (on) {
                    if (this.mBluetooth.getState() == 12) {
                        this.mBluetoothLock.readLock().unlock();
                        return true;
                    }
                } else if (off) {
                    if (this.mBluetooth.getState() == 10) {
                        this.mBluetoothLock.readLock().unlock();
                        return true;
                    }
                } else if (this.mBluetooth.getState() != 12) {
                    this.mBluetoothLock.readLock().unlock();
                    return true;
                }
                this.mBluetoothLock.readLock().unlock();
                if (on || off) {
                    SystemClock.sleep(300);
                } else {
                    SystemClock.sleep(50);
                }
                i++;
            } catch (RemoteException e) {
                Slog.e(TAG, "getState()", e);
                this.mBluetoothLock.readLock().unlock();
            } catch (Throwable th) {
                this.mBluetoothLock.readLock().unlock();
                throw th;
            }
        }
        Slog.e(TAG, "waitForOnOff time out");
        return false;
    }

    /* JADX WARNING: Missing block: B:39:0x0055, code skipped:
            if (r8 != false) goto L_0x0060;
     */
    /* JADX WARNING: Missing block: B:40:0x0057, code skipped:
            if (r9 == false) goto L_0x005a;
     */
    /* JADX WARNING: Missing block: B:41:0x005a, code skipped:
            android.os.SystemClock.sleep(50);
     */
    /* JADX WARNING: Missing block: B:42:0x0060, code skipped:
            android.os.SystemClock.sleep(800);
     */
    /* JADX WARNING: Missing block: B:43:0x0065, code skipped:
            r1 = r1 + 1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean waitForMonitoredOnOff(boolean on, boolean off) {
        int i = 0;
        while (i < 10) {
            synchronized (this.mConnection) {
                try {
                    if (this.mBluetooth != null) {
                        if (on) {
                            if (this.mBluetooth.getState() == 12) {
                                return true;
                            } else if (this.mBluetooth.getState() == 15) {
                                bluetoothStateChangeHandler(14, 15);
                            }
                        } else if (off) {
                            if (this.mBluetooth.getState() == 10) {
                                return true;
                            } else if (this.mBluetooth.getState() == 15) {
                                bluetoothStateChangeHandler(13, 15);
                            }
                        } else if (this.mBluetooth.getState() != 12) {
                            return true;
                        }
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "getState()", e);
                }
            }
        }
        Log.e(TAG, "waitForOnOff time out");
        return false;
    }

    private void sendDisableMsg(int reason, String packageName) {
        this.mLastMessage = 2;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2));
        addActiveLog(reason, packageName, false);
    }

    private void sendEnableMsg(boolean quietMode, int reason, String packageName) {
        long now = SystemClock.elapsedRealtime();
        if (now - this.mLastEnableMessageTime < 1500 && this.mLastMessage == 1 && this.mLastQuietMode == quietMode) {
            HwLog.d(TAG, "MESSAGE_ENABLE message repeat in short time, return");
            this.mLastEnableMessageTime = now;
            return;
        }
        this.mLastEnableMessageTime = now;
        this.mLastMessage = 1;
        this.mLastQuietMode = this.mQuietEnable;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1, quietMode, 0));
        addActiveLog(reason, packageName, true);
        this.mLastEnabledTime = SystemClock.elapsedRealtime();
    }

    private void addActiveLog(int reason, String packageName, boolean enable) {
        int i;
        synchronized (this.mActiveLogs) {
            if (this.mActiveLogs.size() > 20) {
                this.mActiveLogs.remove();
            }
            this.mActiveLogs.add(new ActiveLog(reason, packageName, enable, System.currentTimeMillis()));
        }
        if (enable) {
            i = 1;
        } else {
            i = 2;
        }
        StatsLog.write_non_chained(67, Binder.getCallingUid(), null, i, reason, packageName);
    }

    private void addCrashLog() {
        synchronized (this.mCrashTimestamps) {
            if (this.mCrashTimestamps.size() == 100) {
                this.mCrashTimestamps.removeFirst();
            }
            this.mCrashTimestamps.add(Long.valueOf(System.currentTimeMillis()));
            this.mCrashes++;
        }
    }

    private void recoverBluetoothServiceFromError(boolean clearBle) {
        Slog.e(TAG, "recoverBluetoothServiceFromError");
        try {
            this.mBluetoothLock.readLock().lock();
            if (this.mBluetooth != null) {
                this.mBluetooth.unregisterCallback(this.mBluetoothCallback);
            }
        } catch (RemoteException re) {
            Slog.e(TAG, "Unable to unregister", re);
        } catch (Throwable th) {
            this.mBluetoothLock.readLock().unlock();
        }
        this.mBluetoothLock.readLock().unlock();
        SystemClock.sleep(500);
        addActiveLog(5, this.mContext.getPackageName(), false);
        handleDisable();
        waitForOnOff(false, true);
        sendBluetoothServiceDownCallback();
        try {
            this.mBluetoothLock.writeLock().lock();
            if (this.mBluetooth != null) {
                this.mBluetooth = null;
                this.mContext.unbindService(this.mConnection);
            }
            this.mBluetoothGatt = null;
            this.mHandler.removeMessages(60);
            this.mState = 10;
            if (clearBle) {
                clearBleApps();
            }
            this.mEnable = false;
            int i = this.mErrorRecoveryRetryCounter;
            this.mErrorRecoveryRetryCounter = i + 1;
            if (i < 6) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(42), 3000);
            }
        } finally {
            this.mBluetoothLock.writeLock().unlock();
        }
    }

    private boolean isBluetoothDisallowed() {
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            boolean hasUserRestriction = ((UserManager) this.mContext.getSystemService(UserManager.class)).hasUserRestriction("no_bluetooth", UserHandle.SYSTEM);
            return hasUserRestriction;
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private void updateOppLauncherComponentState(int userId, boolean bluetoothSharingDisallowed) {
        int newState;
        ComponentName oppLauncherComponent = new ComponentName("com.android.bluetooth", "com.android.bluetooth.opp.BluetoothOppLauncherActivity");
        if (bluetoothSharingDisallowed) {
            newState = 2;
        } else {
            newState = 0;
        }
        try {
            AppGlobals.getPackageManager().setComponentEnabledSetting(oppLauncherComponent, newState, 1, userId);
        } catch (Exception e) {
        }
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        PrintWriter printWriter = writer;
        String[] args2 = args;
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            String errorMsg = null;
            boolean protoOut = args2.length > 0 && args2[0].startsWith(PriorityDump.PROTO_ARG);
            if (!protoOut) {
                Iterator it;
                StringBuilder stringBuilder;
                printWriter.println("Bluetooth Status");
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  enabled: ");
                stringBuilder2.append(isEnabled());
                printWriter.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  state: ");
                stringBuilder2.append(BluetoothAdapter.nameForState(this.mState));
                printWriter.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  address: ");
                stringBuilder2.append(getFormatMacAddress(this.mAddress));
                printWriter.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  name: ");
                stringBuilder2.append(this.mName);
                printWriter.println(stringBuilder2.toString());
                if (this.mEnable) {
                    long onDuration = SystemClock.elapsedRealtime() - this.mLastEnabledTime;
                    String onDurationString = String.format(Locale.US, "%02d:%02d:%02d.%03d", new Object[]{Integer.valueOf((int) (onDuration / SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT)), Integer.valueOf((int) ((onDuration / 60000) % 60)), Integer.valueOf((int) ((onDuration / 1000) % 60)), Integer.valueOf((int) (onDuration % 1000))});
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("  time since enabled: ");
                    stringBuilder3.append(onDurationString);
                    printWriter.println(stringBuilder3.toString());
                }
                if (this.mActiveLogs.size() == 0) {
                    printWriter.println("\nBluetooth never enabled!");
                } else {
                    printWriter.println("\nEnable log:");
                    it = this.mActiveLogs.iterator();
                    while (it.hasNext()) {
                        ActiveLog log = (ActiveLog) it.next();
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("  ");
                        stringBuilder.append(log);
                        printWriter.println(stringBuilder.toString());
                    }
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("\nBluetooth crashed ");
                stringBuilder2.append(this.mCrashes);
                stringBuilder2.append(" time");
                stringBuilder2.append(this.mCrashes == 1 ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : "s");
                printWriter.println(stringBuilder2.toString());
                if (this.mCrashes == 100) {
                    printWriter.println("(last 100)");
                }
                it = this.mCrashTimestamps.iterator();
                while (it.hasNext()) {
                    Long time = (Long) it.next();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("  ");
                    stringBuilder.append(timeToLog(time.longValue()));
                    printWriter.println(stringBuilder.toString());
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("\n");
                stringBuilder2.append(this.mBleApps.size());
                stringBuilder2.append(" BLE app");
                stringBuilder2.append(this.mBleApps.size() == 1 ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : "s");
                stringBuilder2.append("registered");
                printWriter.println(stringBuilder2.toString());
                for (ClientDeathRecipient app : this.mBleApps.values()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("  ");
                    stringBuilder.append(app.getPackageName());
                    printWriter.println(stringBuilder.toString());
                }
                printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                writer.flush();
                if (args2.length == 0) {
                    args2 = new String[]{"--print"};
                }
            }
            String[] args3 = args2;
            FileDescriptor fileDescriptor;
            if (this.mBluetoothBinder == null) {
                errorMsg = "Bluetooth Service not connected";
                fileDescriptor = fd;
            } else {
                try {
                    try {
                        this.mBluetoothBinder.dump(fd, args3);
                    } catch (RemoteException e) {
                    }
                } catch (RemoteException e2) {
                    fileDescriptor = fd;
                    errorMsg = "RemoteException while dumping Bluetooth Service";
                    if (errorMsg == null) {
                    }
                }
            }
            if (errorMsg == null && !protoOut) {
                printWriter.println(errorMsg);
            }
        }
    }

    private static String getEnableDisableReasonString(int reason) {
        switch (reason) {
            case 1:
                return "APPLICATION_REQUEST";
            case 2:
                return "AIRPLANE_MODE";
            case 3:
                return "DISALLOWED";
            case 4:
                return "RESTARTED";
            case 5:
                return "START_ERROR";
            case 6:
                return "SYSTEM_BOOT";
            case 7:
                return "CRASH";
            case 8:
                return "USER_SWITCH";
            case 9:
                return "RESTORE_USER_SETTING";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UNKNOWN[");
                stringBuilder.append(reason);
                stringBuilder.append("]");
                return stringBuilder.toString();
        }
    }

    private String getFormatMacAddress(String address) {
        if (address == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            stringBuilder.append(null);
            return stringBuilder.toString();
        }
        int len = address.length();
        String substring = address.substring(len / 2, len);
        String result = new StringBuilder();
        result.append("******");
        result.append(substring);
        return result.toString();
    }

    private void handleEnableRadio() {
        synchronized (this.mConnection) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleEnableRadio mBluetooth = ");
            stringBuilder.append(this.mBluetooth);
            HwLog.i(str, stringBuilder.toString());
            if (this.mBluetooth == null) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(100), 3000);
                this.mConnection.setGetNameAddressOnly(false);
                this.mConnection.setTurnOnRadio(true);
                Intent i = new Intent(IBluetooth.class.getName());
                i.setComponent(i.resolveSystemService(this.mContext.getPackageManager(), 0));
                if (i.getComponent() == null && i.getPackage() == null) {
                    HwLog.e(TAG, "Illegal Argument ! Fail to open radio !");
                    return;
                } else if (!this.mContext.bindService(i, this.mConnection, 1)) {
                    this.mHandler.removeMessages(100);
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Fail to bind to: ");
                    stringBuilder2.append(IBluetooth.class.getName());
                    HwLog.e(str2, stringBuilder2.toString());
                }
            } else {
                try {
                    HwLog.d(TAG, "Getting and storing Bluetooth name and address prior to enable.");
                    storeNameAndAddress(this.mBluetooth.getName(), this.mBluetooth.getAddress());
                } catch (RemoteException e) {
                    Log.e(TAG, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, e);
                }
                try {
                    if (!this.mBluetooth.enableRadio()) {
                        HwLog.e(TAG, "IBluetooth.enableRadio() returned false");
                    }
                } catch (RemoteException e2) {
                    HwLog.e(TAG, "Unable to call enableRadio()", e2);
                }
            }
        }
        return;
    }

    private void handleDisableRadio() {
        synchronized (this.mConnection) {
            if (isRadioEnabled()) {
                try {
                    if (!this.mBluetooth.disableRadio()) {
                        HwLog.e(TAG, "IBluetooth.disableRadio() returned false");
                    }
                } catch (RemoteException e) {
                    HwLog.e(TAG, "Unable to call disableRadio()", e);
                }
            }
        }
    }

    public boolean checkPrecondition(int uid) {
        return false;
    }
}
