package android.bluetooth;

import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.IBluetoothStateChangeCallback.Stub;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class BluetoothA2dp implements BluetoothProfile {
    public static final int A2DP_CODEC_STATUS_LDAC_ERROR = 1001;
    public static final String ACTION_ACTIVE_DEVICE_CHANGED = "android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED";
    public static final String ACTION_AVRCP_CONNECTION_STATE_CHANGED = "android.bluetooth.a2dp.profile.action.AVRCP_CONNECTION_STATE_CHANGED";
    public static final String ACTION_CODEC_CONFIG_CHANGED = "android.bluetooth.a2dp.profile.action.CODEC_CONFIG_CHANGED";
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_PLAYING_STATE_CHANGED = "android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED";
    private static final boolean DBG = true;
    public static final int OPTIONAL_CODECS_NOT_SUPPORTED = 0;
    public static final int OPTIONAL_CODECS_PREF_DISABLED = 0;
    public static final int OPTIONAL_CODECS_PREF_ENABLED = 1;
    public static final int OPTIONAL_CODECS_PREF_UNKNOWN = -1;
    public static final int OPTIONAL_CODECS_SUPPORTED = 1;
    public static final int OPTIONAL_CODECS_SUPPORT_UNKNOWN = -1;
    public static final int STATE_NOT_PLAYING = 11;
    public static final int STATE_PLAYING = 10;
    private static final String TAG = "BluetoothA2dp";
    private static final boolean VDBG = false;
    private BluetoothAdapter mAdapter;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new Stub() {
        public void onBluetoothStateChange(boolean up) {
            String str = BluetoothA2dp.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onBluetoothStateChange: up=");
            stringBuilder.append(up);
            Log.d(str, stringBuilder.toString());
            if (up) {
                try {
                    BluetoothA2dp.this.mServiceLock.readLock().lock();
                    if (BluetoothA2dp.this.mService == null) {
                        BluetoothA2dp.this.doBind();
                    }
                } catch (Exception re) {
                    Log.e(BluetoothA2dp.TAG, "", re);
                } catch (Throwable th) {
                    BluetoothA2dp.this.mServiceLock.readLock().unlock();
                }
                BluetoothA2dp.this.mServiceLock.readLock().unlock();
                return;
            }
            try {
                BluetoothA2dp.this.mServiceLock.writeLock().lock();
                BluetoothA2dp.this.mService = null;
                BluetoothA2dp.this.mContext.unbindService(BluetoothA2dp.this.mConnection);
            } catch (Exception re2) {
                Log.e(BluetoothA2dp.TAG, "", re2);
            } catch (Throwable th2) {
                BluetoothA2dp.this.mServiceLock.writeLock().unlock();
            }
            BluetoothA2dp.this.mServiceLock.writeLock().unlock();
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(BluetoothA2dp.TAG, "Proxy object connected");
            try {
                BluetoothA2dp.this.mServiceLock.writeLock().lock();
                BluetoothA2dp.this.mService = IBluetoothA2dp.Stub.asInterface(Binder.allowBlocking(service));
                if (BluetoothA2dp.this.mServiceListener != null) {
                    BluetoothA2dp.this.mServiceListener.onServiceConnected(2, BluetoothA2dp.this);
                }
            } finally {
                BluetoothA2dp.this.mServiceLock.writeLock().unlock();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(BluetoothA2dp.TAG, "Proxy object disconnected");
            try {
                BluetoothA2dp.this.mServiceLock.writeLock().lock();
                if (BluetoothA2dp.this.mService != null) {
                    BluetoothA2dp.this.mService = null;
                    BluetoothA2dp.this.mContext.unbindService(BluetoothA2dp.this.mConnection);
                }
            } catch (Exception e) {
                Log.e(BluetoothA2dp.TAG, "", e);
            } catch (Throwable th) {
                BluetoothA2dp.this.mServiceLock.writeLock().unlock();
            }
            BluetoothA2dp.this.mServiceLock.writeLock().unlock();
            if (BluetoothA2dp.this.mServiceListener != null) {
                BluetoothA2dp.this.mServiceListener.onServiceDisconnected(2);
            }
        }
    };
    private Context mContext;
    @GuardedBy("mServiceLock")
    private IBluetoothA2dp mService;
    private ServiceListener mServiceListener;
    private final ReentrantReadWriteLock mServiceLock = new ReentrantReadWriteLock();

    BluetoothA2dp(Context context, ServiceListener l) {
        this.mContext = context;
        this.mServiceListener = l;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        IBluetoothManager mgr = this.mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.registerStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }
        doBind();
    }

    boolean doBind() {
        Intent intent = new Intent(IBluetoothA2dp.class.getName());
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp != null && this.mContext.bindServiceAsUser(intent, this.mConnection, 0, this.mContext.getUser())) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Could not bind to Bluetooth A2DP Service with ");
        stringBuilder.append(intent);
        Log.e(str, stringBuilder.toString());
        return false;
    }

    void close() {
        this.mServiceListener = null;
        IBluetoothManager mgr = this.mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.unregisterStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
        }
        try {
            this.mServiceLock.writeLock().lock();
            if (this.mService != null) {
                this.mService = null;
                this.mContext.unbindService(this.mConnection);
            }
        } catch (Exception re) {
            Log.e(TAG, "", re);
        } catch (Throwable th) {
            this.mServiceLock.writeLock().unlock();
        }
        this.mServiceLock.writeLock().unlock();
    }

    public void finalize() {
    }

    public boolean connect(BluetoothDevice device) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("connect(");
        stringBuilder.append(device);
        stringBuilder.append(")");
        log(stringBuilder.toString());
        boolean connect;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(device)) {
                connect = this.mService.connect(device);
                return connect;
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            this.mServiceLock.readLock().unlock();
            return false;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Stack:");
            stringBuilder2.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder2.toString());
            return false;
        } finally {
            connect = this.mServiceLock.readLock();
            connect.unlock();
            return false;
        }
    }

    public boolean disconnect(BluetoothDevice device) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("disconnect(");
        stringBuilder.append(device);
        stringBuilder.append(")");
        log(stringBuilder.toString());
        boolean disconnect;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(device)) {
                disconnect = this.mService.disconnect(device);
                return disconnect;
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            this.mServiceLock.readLock().unlock();
            return false;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Stack:");
            stringBuilder2.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder2.toString());
            return false;
        } finally {
            disconnect = this.mServiceLock.readLock();
            disconnect.unlock();
            return false;
        }
    }

    public List<BluetoothDevice> getConnectedDevices() {
        List<BluetoothDevice> arrayList;
        List<BluetoothDevice> e;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null || !isEnabled()) {
                if (this.mService == null) {
                    Log.w(TAG, "Proxy not attached to service");
                }
                ArrayList arrayList2 = new ArrayList();
                this.mServiceLock.readLock().unlock();
                return arrayList2;
            }
            e = this.mService.getConnectedDevices();
            return e;
        } catch (RemoteException e2) {
            e = e2;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            arrayList = new ArrayList();
            return arrayList;
        } finally {
            arrayList = this.mServiceLock.readLock();
            arrayList.unlock();
        }
    }

    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        List<BluetoothDevice> arrayList;
        List<BluetoothDevice> e;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null || !isEnabled()) {
                if (this.mService == null) {
                    Log.w(TAG, "Proxy not attached to service");
                }
                ArrayList arrayList2 = new ArrayList();
                this.mServiceLock.readLock().unlock();
                return arrayList2;
            }
            e = this.mService.getDevicesMatchingConnectionStates(states);
            return e;
        } catch (RemoteException e2) {
            e = e2;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            arrayList = new ArrayList();
            return arrayList;
        } finally {
            arrayList = this.mServiceLock.readLock();
            arrayList.unlock();
        }
    }

    public int getConnectionState(BluetoothDevice device) {
        int connectionState;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(device)) {
                connectionState = this.mService.getConnectionState(device);
                return connectionState;
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            this.mServiceLock.readLock().unlock();
            return 0;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            return 0;
        } finally {
            connectionState = this.mServiceLock.readLock();
            connectionState.unlock();
            return 0;
        }
    }

    public boolean setActiveDevice(BluetoothDevice device) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setActiveDevice(");
        stringBuilder.append(device);
        stringBuilder.append(")");
        log(stringBuilder.toString());
        boolean activeDevice;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && (device == null || isValidDevice(device))) {
                activeDevice = this.mService.setActiveDevice(device);
                return activeDevice;
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            this.mServiceLock.readLock().unlock();
            return false;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Stack:");
            stringBuilder2.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder2.toString());
            return false;
        } finally {
            activeDevice = this.mServiceLock.readLock();
            activeDevice.unlock();
            return false;
        }
    }

    public BluetoothDevice getActiveDevice() {
        BluetoothDevice activeDevice;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null || !isEnabled()) {
                if (this.mService == null) {
                    Log.w(TAG, "Proxy not attached to service");
                }
                this.mServiceLock.readLock().unlock();
                return null;
            }
            activeDevice = this.mService.getActiveDevice();
            return activeDevice;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            return null;
        } finally {
            activeDevice = this.mServiceLock.readLock();
            activeDevice.unlock();
            return null;
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:12:0x004d=Splitter:B:12:0x004d, B:16:0x005d=Splitter:B:16:0x005d} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean setPriority(BluetoothDevice device, int priority) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setPriority(");
        stringBuilder.append(device);
        stringBuilder.append(", ");
        stringBuilder.append(priority);
        stringBuilder.append(")");
        log(stringBuilder.toString());
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null || !isEnabled() || !isValidDevice(device)) {
                if (this.mService == null) {
                    Log.w(TAG, "Proxy not attached to service");
                }
                this.mServiceLock.readLock().unlock();
                return false;
            } else if (priority != 0 && priority != 100) {
                return false;
            } else {
                boolean priority2 = this.mService.setPriority(device, priority);
                this.mServiceLock.readLock().unlock();
                return priority2;
            }
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Stack:");
            stringBuilder2.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder2.toString());
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public int getPriority(BluetoothDevice device) {
        int priority;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(device)) {
                priority = this.mService.getPriority(device);
                return priority;
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            this.mServiceLock.readLock().unlock();
            return 0;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            return 0;
        } finally {
            priority = this.mServiceLock.readLock();
            priority.unlock();
            return 0;
        }
    }

    public boolean isAvrcpAbsoluteVolumeSupported() {
        Log.d(TAG, "isAvrcpAbsoluteVolumeSupported");
        boolean isAvrcpAbsoluteVolumeSupported;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null || !isEnabled()) {
                if (this.mService == null) {
                    Log.w(TAG, "Proxy not attached to service");
                }
                this.mServiceLock.readLock().unlock();
                return false;
            }
            isAvrcpAbsoluteVolumeSupported = this.mService.isAvrcpAbsoluteVolumeSupported();
            return isAvrcpAbsoluteVolumeSupported;
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in isAvrcpAbsoluteVolumeSupported()", e);
            return false;
        } finally {
            isAvrcpAbsoluteVolumeSupported = this.mServiceLock.readLock();
            isAvrcpAbsoluteVolumeSupported.unlock();
            return false;
        }
    }

    public void setAvrcpAbsoluteVolume(int volume) {
        Log.d(TAG, "setAvrcpAbsoluteVolume");
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled()) {
                this.mService.setAvrcpAbsoluteVolume(volume);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in setAvrcpAbsoluteVolume()", e);
        } catch (Throwable th) {
            this.mServiceLock.readLock().unlock();
        }
        this.mServiceLock.readLock().unlock();
    }

    public boolean isA2dpPlaying(BluetoothDevice device) {
        boolean isA2dpPlaying;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(device)) {
                isA2dpPlaying = this.mService.isA2dpPlaying(device);
                return isA2dpPlaying;
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            this.mServiceLock.readLock().unlock();
            return false;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            return false;
        } finally {
            isA2dpPlaying = this.mServiceLock.readLock();
            isA2dpPlaying.unlock();
            return false;
        }
    }

    public boolean shouldSendVolumeKeys(BluetoothDevice device) {
        if (isEnabled() && isValidDevice(device)) {
            ParcelUuid[] uuids = device.getUuids();
            if (uuids == null) {
                return false;
            }
            for (ParcelUuid uuid : uuids) {
                if (BluetoothUuid.isAvrcpTarget(uuid)) {
                    return true;
                }
            }
        }
        return false;
    }

    public BluetoothCodecStatus getCodecStatus(BluetoothDevice device) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCodecStatus(");
        stringBuilder.append(device);
        stringBuilder.append(")");
        Log.d(str, stringBuilder.toString());
        BluetoothCodecStatus codecStatus;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null || !isEnabled()) {
                if (this.mService == null) {
                    Log.w(TAG, "Proxy not attached to service");
                }
                this.mServiceLock.readLock().unlock();
                return null;
            }
            codecStatus = this.mService.getCodecStatus(device);
            return codecStatus;
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in getCodecStatus()", e);
            return null;
        } finally {
            codecStatus = this.mServiceLock.readLock();
            codecStatus.unlock();
            return null;
        }
    }

    public boolean isSupportPlayLDAC(String deviceMac) {
        Log.d(TAG, "isSupportPlayLDAC");
        boolean isSupportPlayLDAC;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null || !isEnabled()) {
                if (this.mService == null) {
                    Log.w(TAG, "Proxy not attached to service");
                }
                this.mServiceLock.readLock().unlock();
                return false;
            }
            isSupportPlayLDAC = this.mService.isSupportPlayLDAC(deviceMac);
            return isSupportPlayLDAC;
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in isSupportPlayLDAC()", e);
            return false;
        } finally {
            isSupportPlayLDAC = this.mServiceLock.readLock();
            isSupportPlayLDAC.unlock();
            return false;
        }
    }

    public boolean isSupportHighQuality(BluetoothDevice device) {
        Log.d(TAG, "isSupportHighQuality");
        boolean isSupportHighQuality;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null || !isEnabled()) {
                if (this.mService == null) {
                    Log.w(TAG, "Proxy not attached to service");
                }
                this.mServiceLock.readLock().unlock();
                return false;
            }
            isSupportHighQuality = this.mService.isSupportHighQuality(device);
            return isSupportHighQuality;
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in isSupportHighQuality()", e);
            return false;
        } finally {
            isSupportHighQuality = this.mServiceLock.readLock();
            isSupportHighQuality.unlock();
            return false;
        }
    }

    public int getLdacQualityValue() {
        Log.d(TAG, "getLdacQualityValue");
        int ldacQualityValue;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null || !isEnabled()) {
                if (this.mService == null) {
                    Log.w(TAG, "Proxy not attached to service");
                }
                this.mServiceLock.readLock().unlock();
                return 1001;
            }
            ldacQualityValue = this.mService.getLdacQualityValue();
            return ldacQualityValue;
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in getLdacQualityValue()", e);
            return 1001;
        } finally {
            ldacQualityValue = this.mServiceLock.readLock();
            ldacQualityValue.unlock();
            return 1001;
        }
    }

    public boolean getHighQualityDefaultConfigValue(BluetoothDevice device) {
        Log.d(TAG, "getHighQualityDefaultConfigValue");
        boolean highQualityDefaultConfigValue;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null || !isEnabled()) {
                if (this.mService == null) {
                    Log.w(TAG, "Proxy not attached to service");
                }
                this.mServiceLock.readLock().unlock();
                return false;
            }
            highQualityDefaultConfigValue = this.mService.getHighQualityDefaultConfigValue(device);
            return highQualityDefaultConfigValue;
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in getHighQualityDefaultConfigValue()", e);
            return false;
        } finally {
            highQualityDefaultConfigValue = this.mServiceLock.readLock();
            highQualityDefaultConfigValue.unlock();
            return false;
        }
    }

    public boolean setHighQualityDefaultConfigValue(BluetoothDevice device, boolean enable) {
        Log.d(TAG, "setHighQualityDefaultConfigValue");
        boolean highQualityDefaultConfigValue;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null || !isEnabled()) {
                if (this.mService == null) {
                    Log.w(TAG, "Proxy not attached to service");
                }
                this.mServiceLock.readLock().unlock();
                return false;
            }
            highQualityDefaultConfigValue = this.mService.setHighQualityDefaultConfigValue(device, enable);
            return highQualityDefaultConfigValue;
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in setHighQualityDefaultConfigValue()", e);
            return false;
        } finally {
            highQualityDefaultConfigValue = this.mServiceLock.readLock();
            highQualityDefaultConfigValue.unlock();
            return false;
        }
    }

    public void setCodecConfigPreference(BluetoothDevice device, BluetoothCodecConfig codecConfig) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setCodecConfigPreference(");
        stringBuilder.append(device);
        stringBuilder.append(")");
        Log.d(str, stringBuilder.toString());
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled()) {
                this.mService.setCodecConfigPreference(device, codecConfig);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            this.mServiceLock.readLock().unlock();
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in setCodecConfigPreference()", e);
            this.mServiceLock.readLock().unlock();
        } catch (Throwable th) {
            this.mServiceLock.readLock().unlock();
            throw th;
        }
    }

    public void enableOptionalCodecs(BluetoothDevice device) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enableOptionalCodecs(");
        stringBuilder.append(device);
        stringBuilder.append(")");
        Log.d(str, stringBuilder.toString());
        enableDisableOptionalCodecs(device, true);
    }

    public void disableOptionalCodecs(BluetoothDevice device) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("disableOptionalCodecs(");
        stringBuilder.append(device);
        stringBuilder.append(")");
        Log.d(str, stringBuilder.toString());
        enableDisableOptionalCodecs(device, false);
    }

    private void enableDisableOptionalCodecs(BluetoothDevice device, boolean enable) {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled()) {
                if (enable) {
                    this.mService.enableOptionalCodecs(device);
                } else {
                    this.mService.disableOptionalCodecs(device);
                }
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            this.mServiceLock.readLock().unlock();
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in enableDisableOptionalCodecs()", e);
            this.mServiceLock.readLock().unlock();
        } catch (Throwable th) {
            this.mServiceLock.readLock().unlock();
            throw th;
        }
    }

    public int supportsOptionalCodecs(BluetoothDevice device) {
        int supportsOptionalCodecs;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(device)) {
                supportsOptionalCodecs = this.mService.supportsOptionalCodecs(device);
                return supportsOptionalCodecs;
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            this.mServiceLock.readLock().unlock();
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in getSupportsOptionalCodecs()", e);
            return -1;
        } finally {
            supportsOptionalCodecs = this.mServiceLock.readLock();
            supportsOptionalCodecs.unlock();
            return -1;
        }
    }

    public int getOptionalCodecsEnabled(BluetoothDevice device) {
        int optionalCodecsEnabled;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(device)) {
                optionalCodecsEnabled = this.mService.getOptionalCodecsEnabled(device);
                return optionalCodecsEnabled;
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            this.mServiceLock.readLock().unlock();
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in getSupportsOptionalCodecs()", e);
            return -1;
        } finally {
            optionalCodecsEnabled = this.mServiceLock.readLock();
            optionalCodecsEnabled.unlock();
            return -1;
        }
    }

    public void setOptionalCodecsEnabled(BluetoothDevice device, int value) {
        if (value == -1 || value == 0 || value == 1) {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(device)) {
                this.mService.setOptionalCodecsEnabled(device, value);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            this.mServiceLock.readLock().unlock();
            return;
        }
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid value passed to setOptionalCodecsEnabled: ");
            stringBuilder.append(value);
            Log.e(str, stringBuilder.toString());
        } catch (RemoteException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Stack:");
            stringBuilder2.append(Log.getStackTraceString(new Throwable()));
            Log.e(str2, stringBuilder2.toString());
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public static String stateToString(int state) {
        switch (state) {
            case 0:
                return "disconnected";
            case 1:
                return "connecting";
            case 2:
                return UsbManager.USB_CONNECTED;
            case 3:
                return "disconnecting";
            default:
                switch (state) {
                    case 10:
                        return "playing";
                    case 11:
                        return "not playing";
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("<unknown state ");
                        stringBuilder.append(state);
                        stringBuilder.append(">");
                        return stringBuilder.toString();
                }
        }
    }

    private boolean isEnabled() {
        if (this.mAdapter.getState() == 12) {
            return true;
        }
        return false;
    }

    private boolean isValidDevice(BluetoothDevice device) {
        if (device != null && BluetoothAdapter.checkBluetoothAddress(device.getAddress())) {
            return true;
        }
        return false;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    private static void infolog(String msg) {
        Log.i(TAG, msg);
    }
}
