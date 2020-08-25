package com.android.server.audio;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFocusInfo;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRecordingConfiguration;
import android.media.AudioSystem;
import android.media.IAudioFocusChangeDispatcher;
import android.media.IAudioFocusDispatcher;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.pc.IHwPCManager;
import android.text.TextUtils;
import android.util.HwPCUtils;
import android.util.Log;
import com.huawei.android.util.HwPCUtilsEx;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HwMediaFocusControl extends MediaFocusControl {
    private static final boolean DEBUG = false;
    private static final String DESKTOP_AUDIO_MODE_KEY = "desktop_audio_mode";
    private static final String PHONE_AUDIO_MODE = "1";
    private static final String TAG = "HwMediaFocusControl";
    private final boolean ABANDON = false;
    private final boolean REQUEST = true;
    private final int SYSTEMUID = 1000;
    private final ArrayList<AudioFocusChangeClient> mClients = new ArrayList<>();
    protected volatile boolean mInDestopMode = false;

    public HwMediaFocusControl(Context cntxt, PlayerFocusEnforcer pfe) {
        super(cntxt, pfe);
        AudioFocusChangeClient.sHwMediaFocusControl = this;
    }

    /* access modifiers changed from: protected */
    public boolean isMediaForDPExternalDisplay(AudioAttributes aa, String clientId, String pkgName, int uid) {
        boolean isHiCarMode = HwPCUtilsEx.isHiCarCastMode();
        HwPCUtils.log(TAG, "isMediaForDPExternalDisplay aa = " + aa + ", clientId = " + clientId + ", pkgName = " + pkgName + ", mInDestopMode = " + this.mInDestopMode + ", uid = " + uid + ", isHiCarMode = " + isHiCarMode);
        if (!this.mInDestopMode || isHiCarMode || "AudioFocus_For_Phone_Ring_And_Calls".compareTo(clientId) == 0) {
            return false;
        }
        if ((33792 & AudioSystem.getDevicesForStream(3)) == 0 && "1".equals(AudioSystem.getParameters(DESKTOP_AUDIO_MODE_KEY))) {
            return false;
        }
        boolean isMedia = true;
        if (aa == null || aa.getUsage() != 1) {
            isMedia = false;
        }
        if (isMedia && !TextUtils.isEmpty(pkgName)) {
            try {
                IHwPCManager service = HwPCUtils.getHwPCManager();
                if (service != null) {
                    long token = Binder.clearCallingIdentity();
                    try {
                        return service.isPackageRunningOnPCMode(pkgName, uid);
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, "isMediaForDPExternalDisplay RemoteException");
            }
        }
        return false;
    }

    public void desktopModeChanged(boolean desktopMode) {
        HwPCUtils.log(TAG, "changedToDestopMode desktopMode = " + desktopMode);
        if (desktopMode != this.mInDestopMode) {
            this.mInDestopMode = desktopMode;
        }
    }

    public boolean isPkgInExternalDisplay(String pkgName) {
        HwPCUtils.log(TAG, "isPkgInExternalDisplay pkgName = " + pkgName);
        if (pkgName == null) {
            return false;
        }
        synchronized (mAudioFocusLock) {
            Iterator it = this.mFocusStack.iterator();
            while (it.hasNext()) {
                FocusRequester fr = (FocusRequester) it.next();
                boolean isLargeDisplayApp = false;
                try {
                    IHwPCManager service = HwPCUtils.getHwPCManager();
                    if (service != null) {
                        long token = Binder.clearCallingIdentity();
                        try {
                            isLargeDisplayApp = service.isPackageRunningOnPCMode(pkgName, fr.getClientUid());
                        } finally {
                            Binder.restoreCallingIdentity(token);
                        }
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "isPkgInExternalDisplay RemoteException");
                }
                if (fr.hasSamePackage(pkgName) && isLargeDisplayApp) {
                    return true;
                }
            }
            return false;
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isInDesktopMode() {
        return this.mInDestopMode;
    }

    /* access modifiers changed from: protected */
    public void travelsFocusedStack() {
        Iterator<FocusRequester> stackIterator = this.mFocusStack.iterator();
        while (stackIterator.hasNext()) {
            FocusRequester nextFr = stackIterator.next();
            boolean isInExternalDisplay = isMediaForDPExternalDisplay(nextFr.getAudioAttributes(), nextFr.getClientId(), nextFr.getPackageName(), nextFr.getClientUid());
            HwPCUtils.log(TAG, "travelsFocusedStack isInExternalDisplay = " + isInExternalDisplay);
            nextFr.setIsInExternal(isInExternalDisplay);
        }
    }

    /* access modifiers changed from: protected */
    public boolean isUsageAffectDesktopMedia(int usage) {
        HwPCUtils.log(TAG, " isUsageAffectDesktopMedia usage = " + usage);
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        if (usage == 5 || usage == 1) {
            return true;
        }
        if (usage == 2 && audioManager.getMode() == 3) {
            return true;
        }
        return false;
    }

    private static final class AudioFocusChangeClient implements IBinder.DeathRecipient {
        static HwMediaFocusControl sHwMediaFocusControl;
        final String mCallback;
        final IAudioFocusChangeDispatcher mDispatcherCb;
        final String mPkgName;

        AudioFocusChangeClient(IAudioFocusChangeDispatcher afc, String callback, String pkgName) {
            this.mDispatcherCb = afc;
            this.mCallback = callback;
            this.mPkgName = pkgName;
        }

        public void binderDied() {
            Log.i(HwMediaFocusControl.TAG, "AudioFocusChangeClient died");
            sHwMediaFocusControl.unregisterAudioFocusChangeCallback(this.mDispatcherCb, this.mCallback, this.mPkgName);
        }

        /* access modifiers changed from: package-private */
        public boolean init() {
            try {
                this.mDispatcherCb.asBinder().linkToDeath(this, 0);
                return true;
            } catch (RemoteException e) {
                Log.w(HwMediaFocusControl.TAG, "Could not link to client death");
                return false;
            }
        }

        /* access modifiers changed from: package-private */
        public void release() {
            this.mDispatcherCb.asBinder().unlinkToDeath(this, 0);
        }
    }

    public boolean registerAudioFocusChangeCallback(IAudioFocusChangeDispatcher afc, String callback, String pkgName) {
        if (!isSystemApp(pkgName)) {
            Log.e(TAG, "Permission Denial to registerAudioFocusChangeCallback.");
            return false;
        } else if (afc == null) {
            return false;
        } else {
            synchronized (this.mClients) {
                AudioFocusChangeClient afcClient = new AudioFocusChangeClient(afc, callback, pkgName);
                if (afcClient.init()) {
                    this.mClients.add(afcClient);
                    Log.i(TAG, "registerAudioFocusChangeCallback successfully.");
                    return true;
                }
                Log.w(TAG, "registerAudioFocusChangeCallback false");
                return false;
            }
        }
    }

    public boolean unregisterAudioFocusChangeCallback(IAudioFocusChangeDispatcher afc, String callback, String pkgName) {
        if (!isSystemApp(pkgName)) {
            Log.e(TAG, "Permission Denial to unregisterAudioFocusChangeCallback.");
            return false;
        } else if (afc == null) {
            return false;
        } else {
            synchronized (this.mClients) {
                Iterator<AudioFocusChangeClient> clientIterator = this.mClients.iterator();
                while (clientIterator.hasNext()) {
                    AudioFocusChangeClient afcClient = clientIterator.next();
                    if (afcClient != null && afcClient.mDispatcherCb != null && callback.equals(afcClient.mCallback)) {
                        afcClient.release();
                        clientIterator.remove();
                        Log.i(TAG, "unregisterAudioFocusChangeCallback successfully.");
                        return true;
                    }
                }
                Log.w(TAG, "unregisterAudioFocusChangeCallback false");
                return false;
            }
        }
    }

    public void dispatchAudioFocusChange(AudioAttributes attributes, String clientId, int focusType, boolean action) {
        synchronized (this.mClients) {
            if (!this.mClients.isEmpty()) {
                Iterator<AudioFocusChangeClient> clientIterator = this.mClients.iterator();
                while (clientIterator.hasNext()) {
                    AudioFocusChangeClient afcClient = clientIterator.next();
                    if (afcClient != null) {
                        try {
                            if (afcClient.mDispatcherCb != null) {
                                afcClient.mDispatcherCb.dispatchAudioFocusChange(attributes, clientId, focusType, action);
                            }
                        } catch (RemoteException e) {
                            Log.e(TAG, "failed to dispatch audio focus changes");
                        }
                    }
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public int requestAudioFocus(AudioAttributes audioAttributes, int focusChangeHint, IBinder cb, IAudioFocusDispatcher fd, String clientId, String callingPackageName, int flags, int sdk, boolean forceDuck) {
        int res = HwMediaFocusControl.super.requestAudioFocus(audioAttributes, focusChangeHint, cb, fd, clientId, callingPackageName, flags, sdk, forceDuck);
        if (res == 1) {
            dispatchAudioFocusChange(audioAttributes, clientId, focusChangeHint, true);
        }
        return res;
    }

    /* access modifiers changed from: protected */
    public int abandonAudioFocus(IAudioFocusDispatcher fl, String clientId, AudioAttributes audioAttributes, String callingPackageName) {
        int res = HwMediaFocusControl.super.abandonAudioFocus(fl, clientId, audioAttributes, callingPackageName);
        dispatchAudioFocusChange(audioAttributes, clientId, 0, false);
        return res;
    }

    public AudioFocusInfo getAudioFocusInfo(String pkgName) {
        if (!isSystemApp(pkgName)) {
            Log.e(TAG, "Permission Denial to getAudioFocusInfoEx.");
            return null;
        }
        synchronized (mAudioFocusLock) {
            if (this.mFocusStack.empty()) {
                Log.i(TAG, "no audio focus");
                return null;
            }
            FocusRequester fr = (FocusRequester) this.mFocusStack.peek();
            if (fr.getGainRequest() == 1) {
                if (!checkFocusActive(fr)) {
                    return null;
                }
            }
            return fr.toAudioFocusInfo();
        }
    }

    private boolean checkFocusActive(FocusRequester fr) {
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        if (audioManager == null) {
            Log.w(TAG, "could not get AudioManager");
            return false;
        }
        List<AudioPlaybackConfiguration> playbackList = audioManager.getActivePlaybackConfigurations();
        List<AudioRecordingConfiguration> recordingList = audioManager.getActiveRecordingConfigurations();
        for (AudioPlaybackConfiguration apc : playbackList) {
            if (apc != null && fr != null && fr.getClientUid() == apc.getClientUid() && apc.isActive()) {
                return true;
            }
        }
        for (AudioRecordingConfiguration arc : recordingList) {
            if (arc != null && fr != null && fr.getClientUid() == arc.getClientUid()) {
                return true;
            }
        }
        Log.i(TAG, "no active audio focus");
        return false;
    }

    private boolean isSystemApp(String pkgName) {
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            Log.e(TAG, "could not get PackageManager");
            return false;
        }
        try {
            ApplicationInfo info = pm.getApplicationInfo(pkgName, 0);
            if (info != null && info.uid <= 1000) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.i(TAG, "AudioException not found app");
        }
        Log.i(TAG, "the caller is not system app");
        return false;
    }
}
