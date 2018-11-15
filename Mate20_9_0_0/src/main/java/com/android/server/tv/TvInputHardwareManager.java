package com.android.server.tv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.HdmiHotplugEvent;
import android.hardware.hdmi.IHdmiControlService;
import android.hardware.hdmi.IHdmiDeviceEventListener;
import android.hardware.hdmi.IHdmiDeviceEventListener.Stub;
import android.hardware.hdmi.IHdmiHotplugEventListener;
import android.hardware.hdmi.IHdmiSystemAudioModeChangeListener;
import android.media.AudioDevicePort;
import android.media.AudioFormat;
import android.media.AudioGain;
import android.media.AudioGainConfig;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioPortUpdateListener;
import android.media.AudioPatch;
import android.media.AudioPort;
import android.media.AudioPortConfig;
import android.media.tv.ITvInputHardware;
import android.media.tv.ITvInputHardwareCallback;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvStreamConfig;
import android.os.Handler;
import android.os.IBinder.DeathRecipient;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.Surface;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.tv.TvInputHal.Callback;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

class TvInputHardwareManager implements Callback {
    private static final String TAG = TvInputHardwareManager.class.getSimpleName();
    private final AudioManager mAudioManager;
    private final SparseArray<Connection> mConnections = new SparseArray();
    private final Context mContext;
    private int mCurrentIndex = 0;
    private int mCurrentMaxIndex = 0;
    private final TvInputHal mHal = new TvInputHal(this);
    private final Handler mHandler = new ListenerHandler(this, null);
    private final SparseArray<String> mHardwareInputIdMap = new SparseArray();
    private final List<TvInputHardwareInfo> mHardwareList = new ArrayList();
    private final IHdmiDeviceEventListener mHdmiDeviceEventListener = new HdmiDeviceEventListener(this, null);
    private final List<HdmiDeviceInfo> mHdmiDeviceList = new LinkedList();
    private final IHdmiHotplugEventListener mHdmiHotplugEventListener = new HdmiHotplugEventListener(this, null);
    private final SparseArray<String> mHdmiInputIdMap = new SparseArray();
    private final SparseBooleanArray mHdmiStateMap = new SparseBooleanArray();
    private final IHdmiSystemAudioModeChangeListener mHdmiSystemAudioModeChangeListener = new HdmiSystemAudioModeChangeListener(this, null);
    private final Map<String, TvInputInfo> mInputMap = new ArrayMap();
    private final Listener mListener;
    private final Object mLock = new Object();
    private final List<Message> mPendingHdmiDeviceEvents = new LinkedList();
    private final BroadcastReceiver mVolumeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            TvInputHardwareManager.this.handleVolumeChange(context, intent);
        }
    };

    private class Connection implements DeathRecipient {
        private ITvInputHardwareCallback mCallback;
        private Integer mCallingUid = null;
        private TvStreamConfig[] mConfigs = null;
        private TvInputHardwareImpl mHardware = null;
        private final TvInputHardwareInfo mHardwareInfo;
        private TvInputInfo mInfo;
        private Runnable mOnFirstFrameCaptured;
        private Integer mResolvedUserId = null;

        public Connection(TvInputHardwareInfo hardwareInfo) {
            this.mHardwareInfo = hardwareInfo;
        }

        public void resetLocked(TvInputHardwareImpl hardware, ITvInputHardwareCallback callback, TvInputInfo info, Integer callingUid, Integer resolvedUserId) {
            if (this.mHardware != null) {
                try {
                    this.mCallback.onReleased();
                } catch (RemoteException e) {
                    Slog.e(TvInputHardwareManager.TAG, "error in Connection::resetLocked", e);
                }
                this.mHardware.release();
            }
            this.mHardware = hardware;
            this.mCallback = callback;
            this.mInfo = info;
            this.mCallingUid = callingUid;
            this.mResolvedUserId = resolvedUserId;
            this.mOnFirstFrameCaptured = null;
            if (this.mHardware != null && this.mCallback != null) {
                try {
                    this.mCallback.onStreamConfigChanged(getConfigsLocked());
                } catch (RemoteException e2) {
                    Slog.e(TvInputHardwareManager.TAG, "error in Connection::resetLocked", e2);
                }
            }
        }

        public void updateConfigsLocked(TvStreamConfig[] configs) {
            this.mConfigs = configs;
        }

        public TvInputHardwareInfo getHardwareInfoLocked() {
            return this.mHardwareInfo;
        }

        public TvInputInfo getInfoLocked() {
            return this.mInfo;
        }

        public ITvInputHardware getHardwareLocked() {
            return this.mHardware;
        }

        public TvInputHardwareImpl getHardwareImplLocked() {
            return this.mHardware;
        }

        public ITvInputHardwareCallback getCallbackLocked() {
            return this.mCallback;
        }

        public TvStreamConfig[] getConfigsLocked() {
            return this.mConfigs;
        }

        public Integer getCallingUidLocked() {
            return this.mCallingUid;
        }

        public Integer getResolvedUserIdLocked() {
            return this.mResolvedUserId;
        }

        public void setOnFirstFrameCapturedLocked(Runnable runnable) {
            this.mOnFirstFrameCaptured = runnable;
        }

        public Runnable getOnFirstFrameCapturedLocked() {
            return this.mOnFirstFrameCaptured;
        }

        public void binderDied() {
            synchronized (TvInputHardwareManager.this.mLock) {
                resetLocked(null, null, null, null, null);
            }
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Connection{ mHardwareInfo: ");
            stringBuilder.append(this.mHardwareInfo);
            stringBuilder.append(", mInfo: ");
            stringBuilder.append(this.mInfo);
            stringBuilder.append(", mCallback: ");
            stringBuilder.append(this.mCallback);
            stringBuilder.append(", mConfigs: ");
            stringBuilder.append(Arrays.toString(this.mConfigs));
            stringBuilder.append(", mCallingUid: ");
            stringBuilder.append(this.mCallingUid);
            stringBuilder.append(", mResolvedUserId: ");
            stringBuilder.append(this.mResolvedUserId);
            stringBuilder.append(" }");
            return stringBuilder.toString();
        }

        private int getConfigsLengthLocked() {
            return this.mConfigs == null ? 0 : this.mConfigs.length;
        }

        private int getInputStateLocked() {
            if (getConfigsLengthLocked() > 0) {
                return 0;
            }
            switch (this.mHardwareInfo.getCableConnectionStatus()) {
                case 1:
                    return 0;
                case 2:
                    return 2;
                default:
                    return 1;
            }
        }
    }

    private final class HdmiDeviceEventListener extends Stub {
        private HdmiDeviceEventListener() {
        }

        /* synthetic */ HdmiDeviceEventListener(TvInputHardwareManager x0, AnonymousClass1 x1) {
            this();
        }

        /* JADX WARNING: Missing block: B:31:0x00e4, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onStatusChanged(HdmiDeviceInfo deviceInfo, int status) {
            if (deviceInfo.isSourceType()) {
                synchronized (TvInputHardwareManager.this.mLock) {
                    int messageType = 0;
                    Object obj = null;
                    String access$900;
                    StringBuilder stringBuilder;
                    switch (status) {
                        case 1:
                            if (findHdmiDeviceInfo(deviceInfo.getId()) == null) {
                                TvInputHardwareManager.this.mHdmiDeviceList.add(deviceInfo);
                                messageType = 4;
                                obj = deviceInfo;
                                break;
                            }
                            String access$9002 = TvInputHardwareManager.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("The list already contains ");
                            stringBuilder2.append(deviceInfo);
                            stringBuilder2.append("; ignoring.");
                            Slog.w(access$9002, stringBuilder2.toString());
                            return;
                        case 2:
                            if (TvInputHardwareManager.this.mHdmiDeviceList.remove(findHdmiDeviceInfo(deviceInfo.getId()))) {
                                messageType = 5;
                                obj = deviceInfo;
                                break;
                            }
                            access$900 = TvInputHardwareManager.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("The list doesn't contain ");
                            stringBuilder.append(deviceInfo);
                            stringBuilder.append("; ignoring.");
                            Slog.w(access$900, stringBuilder.toString());
                            return;
                        case 3:
                            if (TvInputHardwareManager.this.mHdmiDeviceList.remove(findHdmiDeviceInfo(deviceInfo.getId()))) {
                                TvInputHardwareManager.this.mHdmiDeviceList.add(deviceInfo);
                                messageType = 6;
                                obj = deviceInfo;
                                break;
                            }
                            access$900 = TvInputHardwareManager.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("The list doesn't contain ");
                            stringBuilder.append(deviceInfo);
                            stringBuilder.append("; ignoring.");
                            Slog.w(access$900, stringBuilder.toString());
                            return;
                    }
                    Message msg = TvInputHardwareManager.this.mHandler.obtainMessage(messageType, 0, 0, obj);
                    if (TvInputHardwareManager.this.findHardwareInfoForHdmiPortLocked(deviceInfo.getPortId()) != null) {
                        msg.sendToTarget();
                    } else {
                        TvInputHardwareManager.this.mPendingHdmiDeviceEvents.add(msg);
                    }
                }
            }
        }

        private HdmiDeviceInfo findHdmiDeviceInfo(int id) {
            for (HdmiDeviceInfo info : TvInputHardwareManager.this.mHdmiDeviceList) {
                if (info.getId() == id) {
                    return info;
                }
            }
            return null;
        }
    }

    private final class HdmiHotplugEventListener extends IHdmiHotplugEventListener.Stub {
        private HdmiHotplugEventListener() {
        }

        /* synthetic */ HdmiHotplugEventListener(TvInputHardwareManager x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceived(HdmiHotplugEvent event) {
            synchronized (TvInputHardwareManager.this.mLock) {
                TvInputHardwareManager.this.mHdmiStateMap.put(event.getPort(), event.isConnected());
                TvInputHardwareInfo hardwareInfo = TvInputHardwareManager.this.findHardwareInfoForHdmiPortLocked(event.getPort());
                if (hardwareInfo == null) {
                    return;
                }
                String inputId = (String) TvInputHardwareManager.this.mHardwareInputIdMap.get(hardwareInfo.getDeviceId());
                if (inputId == null) {
                    return;
                }
                TvInputHardwareManager.this.mHandler.obtainMessage(1, event.isConnected() ? 0 : 1, 0, inputId).sendToTarget();
            }
        }
    }

    private final class HdmiSystemAudioModeChangeListener extends IHdmiSystemAudioModeChangeListener.Stub {
        private HdmiSystemAudioModeChangeListener() {
        }

        /* synthetic */ HdmiSystemAudioModeChangeListener(TvInputHardwareManager x0, AnonymousClass1 x1) {
            this();
        }

        public void onStatusChanged(boolean enabled) throws RemoteException {
            synchronized (TvInputHardwareManager.this.mLock) {
                for (int i = 0; i < TvInputHardwareManager.this.mConnections.size(); i++) {
                    TvInputHardwareImpl impl = ((Connection) TvInputHardwareManager.this.mConnections.valueAt(i)).getHardwareImplLocked();
                    if (impl != null) {
                        impl.handleAudioSinkUpdated();
                    }
                }
            }
        }
    }

    interface Listener {
        void onHardwareDeviceAdded(TvInputHardwareInfo tvInputHardwareInfo);

        void onHardwareDeviceRemoved(TvInputHardwareInfo tvInputHardwareInfo);

        void onHdmiDeviceAdded(HdmiDeviceInfo hdmiDeviceInfo);

        void onHdmiDeviceRemoved(HdmiDeviceInfo hdmiDeviceInfo);

        void onHdmiDeviceUpdated(String str, HdmiDeviceInfo hdmiDeviceInfo);

        void onStateChanged(String str, int i);
    }

    private class ListenerHandler extends Handler {
        private static final int HARDWARE_DEVICE_ADDED = 2;
        private static final int HARDWARE_DEVICE_REMOVED = 3;
        private static final int HDMI_DEVICE_ADDED = 4;
        private static final int HDMI_DEVICE_REMOVED = 5;
        private static final int HDMI_DEVICE_UPDATED = 6;
        private static final int STATE_CHANGED = 1;

        private ListenerHandler() {
        }

        /* synthetic */ ListenerHandler(TvInputHardwareManager x0, AnonymousClass1 x1) {
            this();
        }

        public final void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    TvInputHardwareManager.this.mListener.onStateChanged(msg.obj, msg.arg1);
                    return;
                case 2:
                    TvInputHardwareManager.this.mListener.onHardwareDeviceAdded((TvInputHardwareInfo) msg.obj);
                    return;
                case 3:
                    TvInputHardwareManager.this.mListener.onHardwareDeviceRemoved(msg.obj);
                    return;
                case 4:
                    TvInputHardwareManager.this.mListener.onHdmiDeviceAdded((HdmiDeviceInfo) msg.obj);
                    return;
                case 5:
                    TvInputHardwareManager.this.mListener.onHdmiDeviceRemoved((HdmiDeviceInfo) msg.obj);
                    return;
                case 6:
                    String inputId;
                    HdmiDeviceInfo info = msg.obj;
                    synchronized (TvInputHardwareManager.this.mLock) {
                        inputId = (String) TvInputHardwareManager.this.mHdmiInputIdMap.get(info.getId());
                    }
                    if (inputId != null) {
                        TvInputHardwareManager.this.mListener.onHdmiDeviceUpdated(inputId, info);
                        return;
                    } else {
                        Slog.w(TvInputHardwareManager.TAG, "Could not resolve input ID matching the device info; ignoring.");
                        return;
                    }
                default:
                    String access$900 = TvInputHardwareManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unhandled message: ");
                    stringBuilder.append(msg);
                    Slog.w(access$900, stringBuilder.toString());
                    return;
            }
        }
    }

    private class TvInputHardwareImpl extends ITvInputHardware.Stub {
        private TvStreamConfig mActiveConfig = null;
        private final OnAudioPortUpdateListener mAudioListener = new OnAudioPortUpdateListener() {
            public void onAudioPortListUpdate(AudioPort[] portList) {
                synchronized (TvInputHardwareImpl.this.mImplLock) {
                    TvInputHardwareImpl.this.updateAudioConfigLocked();
                }
            }

            public void onAudioPatchListUpdate(AudioPatch[] patchList) {
            }

            public void onServiceDied() {
                synchronized (TvInputHardwareImpl.this.mImplLock) {
                    TvInputHardwareImpl.this.mAudioSource = null;
                    TvInputHardwareImpl.this.mAudioSink.clear();
                    if (TvInputHardwareImpl.this.mAudioPatch != null) {
                        TvInputHardwareManager.this.mAudioManager;
                        AudioManager.releaseAudioPatch(TvInputHardwareImpl.this.mAudioPatch);
                        TvInputHardwareImpl.this.mAudioPatch = null;
                    }
                }
            }
        };
        private AudioPatch mAudioPatch = null;
        private List<AudioDevicePort> mAudioSink = new ArrayList();
        private AudioDevicePort mAudioSource;
        private float mCommittedVolume = -1.0f;
        private int mDesiredChannelMask = 1;
        private int mDesiredFormat = 1;
        private int mDesiredSamplingRate = 0;
        private final Object mImplLock = new Object();
        private final TvInputHardwareInfo mInfo;
        private String mOverrideAudioAddress = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        private int mOverrideAudioType = 0;
        private boolean mReleased = false;
        private float mSourceVolume = 0.0f;

        public TvInputHardwareImpl(TvInputHardwareInfo info) {
            this.mInfo = info;
            TvInputHardwareManager.this.mAudioManager.registerAudioPortUpdateListener(this.mAudioListener);
            if (this.mInfo.getAudioType() != 0) {
                this.mAudioSource = findAudioDevicePort(this.mInfo.getAudioType(), this.mInfo.getAudioAddress());
                findAudioSinkFromAudioPolicy(this.mAudioSink);
            }
        }

        private void findAudioSinkFromAudioPolicy(List<AudioDevicePort> sinks) {
            sinks.clear();
            ArrayList<AudioDevicePort> devicePorts = new ArrayList();
            TvInputHardwareManager.this.mAudioManager;
            if (AudioManager.listAudioDevicePorts(devicePorts) == 0) {
                int sinkDevice = TvInputHardwareManager.this.mAudioManager.getDevicesForStream(3);
                Iterator it = devicePorts.iterator();
                while (it.hasNext()) {
                    AudioDevicePort port = (AudioDevicePort) it.next();
                    if ((port.type() & sinkDevice) != 0 && (port.type() & Integer.MIN_VALUE) == 0) {
                        sinks.add(port);
                    }
                }
            }
        }

        private AudioDevicePort findAudioDevicePort(int type, String address) {
            if (type == 0) {
                return null;
            }
            ArrayList<AudioDevicePort> devicePorts = new ArrayList();
            TvInputHardwareManager.this.mAudioManager;
            if (AudioManager.listAudioDevicePorts(devicePorts) != 0) {
                return null;
            }
            Iterator it = devicePorts.iterator();
            while (it.hasNext()) {
                AudioDevicePort port = (AudioDevicePort) it.next();
                if (port.type() == type && port.address().equals(address)) {
                    return port;
                }
            }
            return null;
        }

        public void release() {
            synchronized (this.mImplLock) {
                TvInputHardwareManager.this.mAudioManager.unregisterAudioPortUpdateListener(this.mAudioListener);
                if (this.mAudioPatch != null) {
                    TvInputHardwareManager.this.mAudioManager;
                    AudioManager.releaseAudioPatch(this.mAudioPatch);
                    this.mAudioPatch = null;
                }
                this.mReleased = true;
            }
        }

        /* JADX WARNING: Missing block: B:30:0x006f, code:
            return r2;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean setSurface(Surface surface, TvStreamConfig config) throws RemoteException {
            synchronized (this.mImplLock) {
                if (this.mReleased) {
                    throw new IllegalStateException("Device already released.");
                }
                int result = 0;
                boolean z = false;
                if (surface == null) {
                    if (this.mActiveConfig != null) {
                        result = TvInputHardwareManager.this.mHal.removeStream(this.mInfo.getDeviceId(), this.mActiveConfig);
                        this.mActiveConfig = null;
                    } else {
                        return true;
                    }
                } else if (config == null) {
                    return false;
                } else {
                    if (!(this.mActiveConfig == null || config.equals(this.mActiveConfig))) {
                        result = TvInputHardwareManager.this.mHal.removeStream(this.mInfo.getDeviceId(), this.mActiveConfig);
                        if (result != 0) {
                            this.mActiveConfig = null;
                        }
                    }
                    if (result == 0) {
                        result = TvInputHardwareManager.this.mHal.addOrUpdateStream(this.mInfo.getDeviceId(), surface, config);
                        if (result == 0) {
                            this.mActiveConfig = config;
                        }
                    }
                }
                updateAudioConfigLocked();
                if (result == 0) {
                    z = true;
                }
            }
        }

        private void updateAudioConfigLocked() {
            boolean sinkUpdated = updateAudioSinkLocked();
            boolean sourceUpdated = updateAudioSourceLocked();
            boolean z;
            boolean z2;
            if (this.mAudioSource == null || this.mAudioSink.isEmpty()) {
                z = sourceUpdated;
            } else if (this.mActiveConfig == null) {
                z2 = sinkUpdated;
                z = sourceUpdated;
            } else {
                int sinkSamplingRate;
                int sinkFormat;
                TvInputHardwareManager.this.updateVolume();
                float volume = this.mSourceVolume * TvInputHardwareManager.this.getMediaStreamVolume();
                AudioGainConfig sourceGainConfig = null;
                int i = 1;
                if (this.mAudioSource.gains().length > 0 && volume != this.mCommittedVolume) {
                    int minValue;
                    AudioGain sourceGain = null;
                    for (AudioGain gain : this.mAudioSource.gains()) {
                        if ((gain.mode() & 1) != 0) {
                            sourceGain = gain;
                            break;
                        }
                    }
                    if (sourceGain != null) {
                        int steps = (sourceGain.maxValue() - sourceGain.minValue()) / sourceGain.stepValue();
                        minValue = sourceGain.minValue();
                        if (volume < 1.0f) {
                            minValue += sourceGain.stepValue() * ((int) (((double) (((float) steps) * volume)) + 0.5d));
                        } else {
                            minValue = sourceGain.maxValue();
                        }
                        sourceGainConfig = sourceGain.buildConfig(1, sourceGain.channelMask(), new int[]{minValue}, 0);
                    } else {
                        Slog.w(TvInputHardwareManager.TAG, "No audio source gain with MODE_JOINT support exists.");
                    }
                }
                AudioPortConfig sourceConfig = this.mAudioSource.activeConfig();
                List<AudioPortConfig> sinkConfigs = new ArrayList();
                AudioPatch[] audioPatchArray = new AudioPatch[]{this.mAudioPatch};
                boolean shouldRecreateAudioPatch = sourceUpdated || sinkUpdated;
                for (AudioDevicePort audioSink : this.mAudioSink) {
                    AudioPortConfig sinkConfig = audioSink.activeConfig();
                    sinkSamplingRate = this.mDesiredSamplingRate;
                    int sinkChannelMask = this.mDesiredChannelMask;
                    sinkFormat = this.mDesiredFormat;
                    if (sinkConfig != null) {
                        if (sinkSamplingRate == 0) {
                            sinkSamplingRate = sinkConfig.samplingRate();
                        }
                        if (sinkChannelMask == i) {
                            sinkChannelMask = sinkConfig.channelMask();
                        }
                        if (sinkFormat == i) {
                            sinkChannelMask = sinkConfig.format();
                        }
                    }
                    if (sinkConfig == null || sinkConfig.samplingRate() != sinkSamplingRate || sinkConfig.channelMask() != sinkChannelMask || sinkConfig.format() != sinkFormat) {
                        if (!TvInputHardwareManager.intArrayContains(audioSink.samplingRates(), sinkSamplingRate) && audioSink.samplingRates().length > 0) {
                            sinkSamplingRate = audioSink.samplingRates()[0];
                        }
                        if (!TvInputHardwareManager.intArrayContains(audioSink.channelMasks(), sinkChannelMask)) {
                            sinkChannelMask = 1;
                        }
                        if (!TvInputHardwareManager.intArrayContains(audioSink.formats(), sinkFormat)) {
                            sinkFormat = 1;
                        }
                        sinkConfig = audioSink.buildConfig(sinkSamplingRate, sinkChannelMask, sinkFormat, null);
                        shouldRecreateAudioPatch = true;
                    }
                    sinkConfigs.add(sinkConfig);
                    i = 1;
                }
                AudioPortConfig sinkConfig2 = (AudioPortConfig) sinkConfigs.get(0);
                if (sourceConfig == null || sourceGainConfig != null) {
                    i = 0;
                    if (TvInputHardwareManager.intArrayContains(this.mAudioSource.samplingRates(), sinkConfig2.samplingRate())) {
                        i = sinkConfig2.samplingRate();
                    } else if (this.mAudioSource.samplingRates().length > 0) {
                        i = this.mAudioSource.samplingRates()[0];
                    }
                    sinkFormat = 1;
                    int[] channelMasks = this.mAudioSource.channelMasks();
                    int length = channelMasks.length;
                    int i2 = 0;
                    while (i2 < length) {
                        sinkSamplingRate = channelMasks[i2];
                        z2 = sinkUpdated;
                        z = sourceUpdated;
                        if (AudioFormat.channelCountFromOutChannelMask(sinkConfig2.channelMask()) == AudioFormat.channelCountFromInChannelMask(sinkSamplingRate)) {
                            sinkFormat = sinkSamplingRate;
                            break;
                        }
                        i2++;
                        sinkUpdated = z2;
                        sourceUpdated = z;
                    }
                    z = sourceUpdated;
                    sinkUpdated = true;
                    if (TvInputHardwareManager.intArrayContains(this.mAudioSource.formats(), sinkConfig2.format())) {
                        sinkUpdated = sinkConfig2.format();
                    }
                    sourceConfig = this.mAudioSource.buildConfig(i, sinkFormat, sinkUpdated, sourceGainConfig);
                    shouldRecreateAudioPatch = true;
                } else {
                    z2 = sinkUpdated;
                    z = sourceUpdated;
                }
                if (shouldRecreateAudioPatch) {
                    this.mCommittedVolume = volume;
                    if (this.mAudioPatch) {
                        TvInputHardwareManager.this.mAudioManager;
                        AudioManager.releaseAudioPatch(this.mAudioPatch);
                    }
                    TvInputHardwareManager.this.mAudioManager;
                    AudioManager.createAudioPatch(audioPatchArray, new AudioPortConfig[]{sourceConfig}, (AudioPortConfig[]) sinkConfigs.toArray(new AudioPortConfig[sinkConfigs.size()]));
                    this.mAudioPatch = audioPatchArray[0];
                    if (sourceGainConfig != null) {
                        TvInputHardwareManager.this.mAudioManager;
                        AudioManager.setAudioPortGain(this.mAudioSource, sourceGainConfig);
                    }
                }
                return;
            }
            if (this.mAudioPatch != null) {
                TvInputHardwareManager.this.mAudioManager;
                AudioManager.releaseAudioPatch(this.mAudioPatch);
                this.mAudioPatch = null;
            }
        }

        public void setStreamVolume(float volume) throws RemoteException {
            synchronized (this.mImplLock) {
                if (this.mReleased) {
                    throw new IllegalStateException("Device already released.");
                }
                this.mSourceVolume = volume;
                updateAudioConfigLocked();
            }
        }

        /* JADX WARNING: Missing block: B:17:0x002d, code:
            return r2;
     */
        /* JADX WARNING: Missing block: B:19:0x002f, code:
            return false;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private boolean startCapture(Surface surface, TvStreamConfig config) {
            synchronized (this.mImplLock) {
                boolean z = false;
                if (this.mReleased) {
                    return false;
                } else if (surface == null || config == null) {
                } else if (config.getType() != 2) {
                    return false;
                } else if (TvInputHardwareManager.this.mHal.addOrUpdateStream(this.mInfo.getDeviceId(), surface, config) == 0) {
                    z = true;
                }
            }
        }

        /* JADX WARNING: Missing block: B:14:0x0023, code:
            return r2;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private boolean stopCapture(TvStreamConfig config) {
            synchronized (this.mImplLock) {
                boolean z = false;
                if (this.mReleased) {
                    return false;
                } else if (config == null) {
                    return false;
                } else if (TvInputHardwareManager.this.mHal.removeStream(this.mInfo.getDeviceId(), config) == 0) {
                    z = true;
                }
            }
        }

        private boolean updateAudioSourceLocked() {
            boolean z = false;
            if (this.mInfo.getAudioType() == 0) {
                return false;
            }
            AudioDevicePort previousSource = this.mAudioSource;
            this.mAudioSource = findAudioDevicePort(this.mInfo.getAudioType(), this.mInfo.getAudioAddress());
            if (this.mAudioSource != null ? this.mAudioSource.equals(previousSource) : previousSource == null) {
                z = true;
            }
            return z;
        }

        private boolean updateAudioSinkLocked() {
            if (this.mInfo.getAudioType() == 0) {
                return false;
            }
            List<AudioDevicePort> previousSink = this.mAudioSink;
            this.mAudioSink = new ArrayList();
            if (this.mOverrideAudioType == 0) {
                findAudioSinkFromAudioPolicy(this.mAudioSink);
            } else {
                AudioDevicePort audioSink = findAudioDevicePort(this.mOverrideAudioType, this.mOverrideAudioAddress);
                if (audioSink != null) {
                    this.mAudioSink.add(audioSink);
                }
            }
            if (this.mAudioSink.size() != previousSink.size()) {
                return true;
            }
            previousSink.removeAll(this.mAudioSink);
            return previousSink.isEmpty() ^ true;
        }

        private void handleAudioSinkUpdated() {
            synchronized (this.mImplLock) {
                updateAudioConfigLocked();
            }
        }

        public void overrideAudioSink(int audioType, String audioAddress, int samplingRate, int channelMask, int format) {
            synchronized (this.mImplLock) {
                this.mOverrideAudioType = audioType;
                this.mOverrideAudioAddress = audioAddress;
                this.mDesiredSamplingRate = samplingRate;
                this.mDesiredChannelMask = channelMask;
                this.mDesiredFormat = format;
                updateAudioConfigLocked();
            }
        }

        public void onMediaStreamVolumeChanged() {
            synchronized (this.mImplLock) {
                updateAudioConfigLocked();
            }
        }
    }

    public TvInputHardwareManager(Context context, Listener listener) {
        this.mContext = context;
        this.mListener = listener;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mHal.init();
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            IHdmiControlService hdmiControlService = IHdmiControlService.Stub.asInterface(ServiceManager.getService("hdmi_control"));
            if (hdmiControlService != null) {
                try {
                    hdmiControlService.addHotplugEventListener(this.mHdmiHotplugEventListener);
                    hdmiControlService.addDeviceEventListener(this.mHdmiDeviceEventListener);
                    hdmiControlService.addSystemAudioModeChangeListener(this.mHdmiSystemAudioModeChangeListener);
                    this.mHdmiDeviceList.addAll(hdmiControlService.getInputDevices());
                } catch (RemoteException e) {
                    Slog.w(TAG, "Error registering listeners to HdmiControlService:", e);
                }
            } else {
                Slog.w(TAG, "HdmiControlService is not available");
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.media.VOLUME_CHANGED_ACTION");
            filter.addAction("android.media.STREAM_MUTE_CHANGED_ACTION");
            this.mContext.registerReceiver(this.mVolumeReceiver, filter);
            updateVolume();
        }
    }

    public void onDeviceAvailable(TvInputHardwareInfo info, TvStreamConfig[] configs) {
        synchronized (this.mLock) {
            Connection connection = new Connection(info);
            connection.updateConfigsLocked(configs);
            this.mConnections.put(info.getDeviceId(), connection);
            buildHardwareListLocked();
            this.mHandler.obtainMessage(2, 0, 0, info).sendToTarget();
            if (info.getType() == 9) {
                processPendingHdmiDeviceEventsLocked();
            }
        }
    }

    private void buildHardwareListLocked() {
        this.mHardwareList.clear();
        for (int i = 0; i < this.mConnections.size(); i++) {
            this.mHardwareList.add(((Connection) this.mConnections.valueAt(i)).getHardwareInfoLocked());
        }
    }

    public void onDeviceUnavailable(int deviceId) {
        synchronized (this.mLock) {
            Connection connection = (Connection) this.mConnections.get(deviceId);
            if (connection == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onDeviceUnavailable: Cannot find a connection with ");
                stringBuilder.append(deviceId);
                Slog.e(str, stringBuilder.toString());
                return;
            }
            connection.resetLocked(null, null, null, null, null);
            this.mConnections.remove(deviceId);
            buildHardwareListLocked();
            TvInputHardwareInfo info = connection.getHardwareInfoLocked();
            if (info.getType() == 9) {
                Iterator<HdmiDeviceInfo> it = this.mHdmiDeviceList.iterator();
                while (it.hasNext()) {
                    HdmiDeviceInfo deviceInfo = (HdmiDeviceInfo) it.next();
                    if (deviceInfo.getPortId() == info.getHdmiPortId()) {
                        this.mHandler.obtainMessage(5, 0, 0, deviceInfo).sendToTarget();
                        it.remove();
                    }
                }
            }
            this.mHandler.obtainMessage(3, 0, 0, info).sendToTarget();
        }
    }

    public void onStreamConfigurationChanged(int deviceId, TvStreamConfig[] configs) {
        synchronized (this.mLock) {
            Connection connection = (Connection) this.mConnections.get(deviceId);
            if (connection == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("StreamConfigurationChanged: Cannot find a connection with ");
                stringBuilder.append(deviceId);
                Slog.e(str, stringBuilder.toString());
                return;
            }
            int previousConfigsLength = connection.getConfigsLengthLocked();
            connection.updateConfigsLocked(configs);
            String inputId = (String) this.mHardwareInputIdMap.get(deviceId);
            if (inputId != null) {
                if ((previousConfigsLength == 0 ? 1 : 0) != (connection.getConfigsLengthLocked() == 0 ? 1 : 0)) {
                    this.mHandler.obtainMessage(1, connection.getInputStateLocked(), 0, inputId).sendToTarget();
                }
            }
            ITvInputHardwareCallback callback = connection.getCallbackLocked();
            if (callback != null) {
                try {
                    callback.onStreamConfigChanged(configs);
                } catch (RemoteException e) {
                    Slog.e(TAG, "error in onStreamConfigurationChanged", e);
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0033, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onFirstFrameCaptured(int deviceId, int streamId) {
        synchronized (this.mLock) {
            Connection connection = (Connection) this.mConnections.get(deviceId);
            if (connection == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("FirstFrameCaptured: Cannot find a connection with ");
                stringBuilder.append(deviceId);
                Slog.e(str, stringBuilder.toString());
                return;
            }
            Runnable runnable = connection.getOnFirstFrameCapturedLocked();
            if (runnable != null) {
                runnable.run();
                connection.setOnFirstFrameCapturedLocked(null);
            }
        }
    }

    public List<TvInputHardwareInfo> getHardwareList() {
        List<TvInputHardwareInfo> unmodifiableList;
        synchronized (this.mLock) {
            unmodifiableList = Collections.unmodifiableList(this.mHardwareList);
        }
        return unmodifiableList;
    }

    public List<HdmiDeviceInfo> getHdmiDeviceList() {
        List<HdmiDeviceInfo> unmodifiableList;
        synchronized (this.mLock) {
            unmodifiableList = Collections.unmodifiableList(this.mHdmiDeviceList);
        }
        return unmodifiableList;
    }

    private boolean checkUidChangedLocked(Connection connection, int callingUid, int resolvedUserId) {
        Integer connectionCallingUid = connection.getCallingUidLocked();
        Integer connectionResolvedUserId = connection.getResolvedUserIdLocked();
        return connectionCallingUid == null || connectionResolvedUserId == null || connectionCallingUid.intValue() != callingUid || connectionResolvedUserId.intValue() != resolvedUserId;
    }

    /* JADX WARNING: Missing block: B:27:0x00b9, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addHardwareInput(int deviceId, TvInputInfo info) {
        synchronized (this.mLock) {
            String oldInputId = (String) this.mHardwareInputIdMap.get(deviceId);
            if (oldInputId != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Trying to override previous registration: old = ");
                stringBuilder.append(this.mInputMap.get(oldInputId));
                stringBuilder.append(":");
                stringBuilder.append(deviceId);
                stringBuilder.append(", new = ");
                stringBuilder.append(info);
                stringBuilder.append(":");
                stringBuilder.append(deviceId);
                Slog.w(str, stringBuilder.toString());
            }
            this.mHardwareInputIdMap.put(deviceId, info.getId());
            this.mInputMap.put(info.getId(), info);
            for (int i = 0; i < this.mHdmiStateMap.size(); i++) {
                TvInputHardwareInfo hardwareInfo = findHardwareInfoForHdmiPortLocked(this.mHdmiStateMap.keyAt(i));
                if (hardwareInfo != null) {
                    String inputId = (String) this.mHardwareInputIdMap.get(hardwareInfo.getDeviceId());
                    if (inputId != null && inputId.equals(info.getId())) {
                        this.mHandler.obtainMessage(1, this.mHdmiStateMap.valueAt(i) ? 0 : 1, 0, inputId).sendToTarget();
                        return;
                    }
                }
            }
            Connection connection = (Connection) this.mConnections.get(deviceId);
            if (connection != null) {
                this.mHandler.obtainMessage(1, connection.getInputStateLocked(), 0, info.getId()).sendToTarget();
            }
        }
    }

    private static <T> int indexOfEqualValue(SparseArray<T> map, T value) {
        for (int i = 0; i < map.size(); i++) {
            if (map.valueAt(i).equals(value)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean intArrayContains(int[] array, int value) {
        for (int element : array) {
            if (element == value) {
                return true;
            }
        }
        return false;
    }

    public void addHdmiInput(int id, TvInputInfo info) {
        if (info.getType() == 1007) {
            synchronized (this.mLock) {
                if (indexOfEqualValue(this.mHardwareInputIdMap, info.getParentId()) >= 0) {
                    String oldInputId = (String) this.mHdmiInputIdMap.get(id);
                    if (oldInputId != null) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Trying to override previous registration: old = ");
                        stringBuilder.append(this.mInputMap.get(oldInputId));
                        stringBuilder.append(":");
                        stringBuilder.append(id);
                        stringBuilder.append(", new = ");
                        stringBuilder.append(info);
                        stringBuilder.append(":");
                        stringBuilder.append(id);
                        Slog.w(str, stringBuilder.toString());
                    }
                    this.mHdmiInputIdMap.put(id, info.getId());
                    this.mInputMap.put(info.getId(), info);
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("info (");
                    stringBuilder2.append(info);
                    stringBuilder2.append(") has invalid parentId.");
                    throw new IllegalArgumentException(stringBuilder2.toString());
                }
            }
            return;
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("info (");
        stringBuilder3.append(info);
        stringBuilder3.append(") has non-HDMI type.");
        throw new IllegalArgumentException(stringBuilder3.toString());
    }

    public void removeHardwareInput(String inputId) {
        synchronized (this.mLock) {
            this.mInputMap.remove(inputId);
            int hardwareIndex = indexOfEqualValue(this.mHardwareInputIdMap, inputId);
            if (hardwareIndex >= 0) {
                this.mHardwareInputIdMap.removeAt(hardwareIndex);
            }
            int deviceIndex = indexOfEqualValue(this.mHdmiInputIdMap, inputId);
            if (deviceIndex >= 0) {
                this.mHdmiInputIdMap.removeAt(deviceIndex);
            }
        }
    }

    public ITvInputHardware acquireHardware(int deviceId, ITvInputHardwareCallback callback, TvInputInfo info, int callingUid, int resolvedUserId) {
        if (callback != null) {
            synchronized (this.mLock) {
                Connection connection = (Connection) this.mConnections.get(deviceId);
                if (connection == null) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid deviceId : ");
                    stringBuilder.append(deviceId);
                    Slog.e(str, stringBuilder.toString());
                    return null;
                }
                if (checkUidChangedLocked(connection, callingUid, resolvedUserId)) {
                    TvInputHardwareImpl hardware = new TvInputHardwareImpl(connection.getHardwareInfoLocked());
                    try {
                        callback.asBinder().linkToDeath(connection, 0);
                        connection.resetLocked(hardware, callback, info, Integer.valueOf(callingUid), Integer.valueOf(resolvedUserId));
                    } catch (RemoteException e) {
                        hardware.release();
                        return null;
                    }
                }
                ITvInputHardware hardwareLocked = connection.getHardwareLocked();
                return hardwareLocked;
            }
        }
        throw new NullPointerException();
    }

    /* JADX WARNING: Missing block: B:16:0x003e, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void releaseHardware(int deviceId, ITvInputHardware hardware, int callingUid, int resolvedUserId) {
        synchronized (this.mLock) {
            Connection connection = (Connection) this.mConnections.get(deviceId);
            if (connection == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid deviceId : ");
                stringBuilder.append(deviceId);
                Slog.e(str, stringBuilder.toString());
            } else if (connection.getHardwareLocked() != hardware || checkUidChangedLocked(connection, callingUid, resolvedUserId)) {
            } else {
                connection.resetLocked(null, null, null, null, null);
            }
        }
    }

    private TvInputHardwareInfo findHardwareInfoForHdmiPortLocked(int port) {
        for (TvInputHardwareInfo hardwareInfo : this.mHardwareList) {
            if (hardwareInfo.getType() == 9 && hardwareInfo.getHdmiPortId() == port) {
                return hardwareInfo;
            }
        }
        return null;
    }

    private int findDeviceIdForInputIdLocked(String inputId) {
        for (int i = 0; i < this.mConnections.size(); i++) {
            if (((Connection) this.mConnections.get(i)).getInfoLocked().getId().equals(inputId)) {
                return i;
            }
        }
        return -1;
    }

    public List<TvStreamConfig> getAvailableTvStreamConfigList(String inputId, int callingUid, int resolvedUserId) {
        List<TvStreamConfig> configsList = new ArrayList();
        synchronized (this.mLock) {
            int deviceId = findDeviceIdForInputIdLocked(inputId);
            if (deviceId < 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid inputId : ");
                stringBuilder.append(inputId);
                Slog.e(str, stringBuilder.toString());
                return configsList;
            }
            for (TvStreamConfig config : ((Connection) this.mConnections.get(deviceId)).getConfigsLocked()) {
                if (config.getType() == 2) {
                    configsList.add(config);
                }
            }
            return configsList;
        }
    }

    /* JADX WARNING: Missing block: B:17:0x004c, code:
            return r5;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean captureFrame(String inputId, Surface surface, final TvStreamConfig config, int callingUid, int resolvedUserId) {
        synchronized (this.mLock) {
            int deviceId = findDeviceIdForInputIdLocked(inputId);
            if (deviceId < 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid inputId : ");
                stringBuilder.append(inputId);
                Slog.e(str, stringBuilder.toString());
                return false;
            }
            Connection connection = (Connection) this.mConnections.get(deviceId);
            final TvInputHardwareImpl hardwareImpl = connection.getHardwareImplLocked();
            if (hardwareImpl != null) {
                Runnable runnable = connection.getOnFirstFrameCapturedLocked();
                if (runnable != null) {
                    runnable.run();
                    connection.setOnFirstFrameCapturedLocked(null);
                }
                boolean result = hardwareImpl.startCapture(surface, config);
                if (result) {
                    connection.setOnFirstFrameCapturedLocked(new Runnable() {
                        public void run() {
                            hardwareImpl.stopCapture(config);
                        }
                    });
                }
            } else {
                return false;
            }
        }
    }

    private void processPendingHdmiDeviceEventsLocked() {
        Iterator<Message> it = this.mPendingHdmiDeviceEvents.iterator();
        while (it.hasNext()) {
            Message msg = (Message) it.next();
            if (findHardwareInfoForHdmiPortLocked(msg.obj.getPortId()) != null) {
                msg.sendToTarget();
                it.remove();
            }
        }
    }

    private void updateVolume() {
        this.mCurrentMaxIndex = this.mAudioManager.getStreamMaxVolume(3);
        this.mCurrentIndex = this.mAudioManager.getStreamVolume(3);
    }

    /* JADX WARNING: Removed duplicated region for block: B:13:0x002e  */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x004e  */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x0045  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x0069 A:{LOOP_START, LOOP:0: B:27:0x0069->B:34:0x0083, PHI: r3 } */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x002e  */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x004e  */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x0045  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x0069 A:{LOOP_START, LOOP:0: B:27:0x0069->B:34:0x0083, PHI: r3 } */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleVolumeChange(Context context, Intent intent) {
        String action = intent.getAction();
        int hashCode = action.hashCode();
        int i = 0;
        if (hashCode != -1940635523) {
            if (hashCode == 1920758225 && action.equals("android.media.STREAM_MUTE_CHANGED_ACTION")) {
                int index;
                hashCode = 1;
                switch (hashCode) {
                    case 0:
                        if (intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1) == 3) {
                            index = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", 0);
                            if (index != this.mCurrentIndex) {
                                this.mCurrentIndex = index;
                                break;
                            }
                            return;
                        }
                        return;
                    case 1:
                        if (intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1) != 3) {
                            return;
                        }
                        break;
                    default:
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unrecognized intent: ");
                        stringBuilder.append(intent);
                        Slog.w(str, stringBuilder.toString());
                        return;
                }
                synchronized (this.mLock) {
                    while (true) {
                        index = i;
                        if (index < this.mConnections.size()) {
                            TvInputHardwareImpl hardwareImpl = ((Connection) this.mConnections.valueAt(index)).getHardwareImplLocked();
                            if (hardwareImpl != null) {
                                hardwareImpl.onMediaStreamVolumeChanged();
                            }
                            i = index + 1;
                        }
                    }
                }
            }
        } else if (action.equals("android.media.VOLUME_CHANGED_ACTION")) {
            hashCode = 0;
            switch (hashCode) {
                case 0:
                    break;
                case 1:
                    break;
                default:
                    break;
            }
            synchronized (this.mLock) {
            }
        }
        hashCode = -1;
        switch (hashCode) {
            case 0:
                break;
            case 1:
                break;
            default:
                break;
        }
        synchronized (this.mLock) {
        }
    }

    private float getMediaStreamVolume() {
        return ((float) this.mCurrentIndex) / ((float) this.mCurrentMaxIndex);
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            synchronized (this.mLock) {
                int i;
                int deviceId;
                StringBuilder stringBuilder;
                pw.println("TvInputHardwareManager Info:");
                pw.increaseIndent();
                pw.println("mConnections: deviceId -> Connection");
                pw.increaseIndent();
                int i2 = 0;
                for (i = 0; i < this.mConnections.size(); i++) {
                    deviceId = this.mConnections.keyAt(i);
                    Connection mConnection = (Connection) this.mConnections.valueAt(i);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(deviceId);
                    stringBuilder.append(": ");
                    stringBuilder.append(mConnection);
                    pw.println(stringBuilder.toString());
                }
                pw.decreaseIndent();
                pw.println("mHardwareList:");
                pw.increaseIndent();
                for (TvInputHardwareInfo tvInputHardwareInfo : this.mHardwareList) {
                    pw.println(tvInputHardwareInfo);
                }
                pw.decreaseIndent();
                pw.println("mHdmiDeviceList:");
                pw.increaseIndent();
                for (HdmiDeviceInfo hdmiDeviceInfo : this.mHdmiDeviceList) {
                    pw.println(hdmiDeviceInfo);
                }
                pw.decreaseIndent();
                pw.println("mHardwareInputIdMap: deviceId -> inputId");
                pw.increaseIndent();
                for (i = 0; i < this.mHardwareInputIdMap.size(); i++) {
                    deviceId = this.mHardwareInputIdMap.keyAt(i);
                    String inputId = (String) this.mHardwareInputIdMap.valueAt(i);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(deviceId);
                    stringBuilder.append(": ");
                    stringBuilder.append(inputId);
                    pw.println(stringBuilder.toString());
                }
                pw.decreaseIndent();
                pw.println("mHdmiInputIdMap: id -> inputId");
                pw.increaseIndent();
                while (i2 < this.mHdmiInputIdMap.size()) {
                    i = this.mHdmiInputIdMap.keyAt(i2);
                    String inputId2 = (String) this.mHdmiInputIdMap.valueAt(i2);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(i);
                    stringBuilder2.append(": ");
                    stringBuilder2.append(inputId2);
                    pw.println(stringBuilder2.toString());
                    i2++;
                }
                pw.decreaseIndent();
                pw.println("mInputMap: inputId -> inputInfo");
                pw.increaseIndent();
                for (Entry<String, TvInputInfo> entry : this.mInputMap.entrySet()) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append((String) entry.getKey());
                    stringBuilder3.append(": ");
                    stringBuilder3.append(entry.getValue());
                    pw.println(stringBuilder3.toString());
                }
                pw.decreaseIndent();
                pw.decreaseIndent();
            }
        }
    }
}
