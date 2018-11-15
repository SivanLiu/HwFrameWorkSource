package com.android.server.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.os.Binder;
import android.os.RemoteException;
import android.pc.IHwPCManager;
import android.text.TextUtils;
import android.util.HwPCUtils;
import android.util.Log;
import java.util.Iterator;

public class HwMediaFocusControl extends MediaFocusControl {
    private static final boolean DEBUG = false;
    private static final String TAG = "HwMediaFocusControl";
    protected volatile boolean mInDestopMode = false;

    public HwMediaFocusControl(Context cntxt, PlayerFocusEnforcer pfe) {
        super(cntxt, pfe);
    }

    protected boolean isMediaForDPExternalDisplay(AudioAttributes aa, String clientId, String pkgName, int uid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isMediaForDPExternalDisplay aa = ");
        stringBuilder.append(aa);
        stringBuilder.append(", clientId = ");
        stringBuilder.append(clientId);
        stringBuilder.append(", pkgName = ");
        stringBuilder.append(pkgName);
        stringBuilder.append(", mInDestopMode = ");
        stringBuilder.append(this.mInDestopMode);
        stringBuilder.append(", uid = ");
        stringBuilder.append(uid);
        HwPCUtils.log(str, stringBuilder.toString());
        if (!this.mInDestopMode || "AudioFocus_For_Phone_Ring_And_Calls".compareTo(clientId) == 0 || (33792 & AudioSystem.getDevicesForStream(3)) == 0) {
            return false;
        }
        boolean isMedia = true;
        if (aa == null || aa.getUsage() != 1) {
            isMedia = false;
        }
        if (isMedia && !TextUtils.isEmpty(pkgName)) {
            long token;
            try {
                IHwPCManager service = HwPCUtils.getHwPCManager();
                if (service != null) {
                    token = Binder.clearCallingIdentity();
                    boolean isPackageRunningOnPCMode = service.isPackageRunningOnPCMode(pkgName, uid);
                    Binder.restoreCallingIdentity(token);
                    return isPackageRunningOnPCMode;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "isMediaForDPExternalDisplay RemoteException");
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }
        return false;
    }

    public void desktopModeChanged(boolean desktopMode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("changedToDestopMode desktopMode = ");
        stringBuilder.append(desktopMode);
        HwPCUtils.log(str, stringBuilder.toString());
        if (desktopMode != this.mInDestopMode) {
            this.mInDestopMode = desktopMode;
        }
    }

    public boolean isPkgInExternalDisplay(String pkgName) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isPkgInExternalDisplay pkgName = ");
        stringBuilder.append(pkgName);
        HwPCUtils.log(str, stringBuilder.toString());
        if (pkgName == null) {
            return false;
        }
        synchronized (mAudioFocusLock) {
            Iterator it = this.mFocusStack.iterator();
            while (it.hasNext()) {
                FocusRequester fr = (FocusRequester) it.next();
                boolean isLargeDisplayApp = false;
                long token;
                try {
                    IHwPCManager service = HwPCUtils.getHwPCManager();
                    if (service != null) {
                        token = Binder.clearCallingIdentity();
                        isLargeDisplayApp = service.isPackageRunningOnPCMode(pkgName, fr.getClientUid());
                        Binder.restoreCallingIdentity(token);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "isPkgInExternalDisplay RemoteException");
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(token);
                }
                if (fr.hasSamePackage(pkgName) && isLargeDisplayApp) {
                    return true;
                }
            }
            return false;
        }
    }

    boolean isInDesktopMode() {
        return this.mInDestopMode;
    }

    protected void travelsFocusedStack() {
        Iterator<FocusRequester> stackIterator = this.mFocusStack.iterator();
        while (stackIterator.hasNext()) {
            FocusRequester nextFr = (FocusRequester) stackIterator.next();
            boolean isInExternalDisplay = isMediaForDPExternalDisplay(nextFr.getAudioAttributes(), nextFr.getClientId(), nextFr.getPackageName(), nextFr.getClientUid());
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("travelsFocusedStack isInExternalDisplay = ");
            stringBuilder.append(isInExternalDisplay);
            HwPCUtils.log(str, stringBuilder.toString());
            nextFr.setIsInExternal(isInExternalDisplay);
        }
    }

    protected boolean isUsageAffectDesktopMedia(int usage) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" isUsageAffectDesktopMedia usage = ");
        stringBuilder.append(usage);
        HwPCUtils.log(str, stringBuilder.toString());
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        if (usage == 5 || usage == 1) {
            return true;
        }
        if (usage == 2 && audioManager.getMode() == 3) {
            return true;
        }
        return false;
    }
}
