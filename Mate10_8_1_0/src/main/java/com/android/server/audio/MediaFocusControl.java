package com.android.server.audio;

import android.app.AppOpsManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusInfo;
import android.media.IAudioFocusDispatcher;
import android.media.audiopolicy.IAudioPolicyCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.util.HwPCUtils;
import android.util.Log;
import com.android.server.HwServiceFactory;
import com.android.server.audio.AudioEventLogger.StringEvent;
import com.huawei.pgmng.log.LogPower;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Stack;

public class MediaFocusControl implements PlayerFocusEnforcer {
    static final boolean DEBUG = false;
    static final int DUCKING_IN_APP_SDK_LEVEL = 25;
    static final boolean ENFORCE_DUCKING = true;
    static final boolean ENFORCE_DUCKING_FOR_NEW = true;
    static final boolean ENFORCE_MUTING_FOR_RING_OR_CALL = true;
    private static final int MAX_STACK_SIZE = 100;
    private static final int RING_CALL_MUTING_ENFORCEMENT_DELAY_MS = 100;
    private static final String TAG = "MediaFocusControl";
    private static final int[] USAGES_TO_MUTE_IN_RING_OR_CALL = new int[]{1, 14};
    protected static final Object mAudioFocusLock = new Object();
    private static final AudioEventLogger mEventLogger = new AudioEventLogger(50, "focus commands as seen by MediaFocusControl");
    private final AppOpsManager mAppOps;
    protected final Context mContext;
    private PlayerFocusEnforcer mFocusEnforcer;
    private ArrayList<IAudioPolicyCallback> mFocusFollowers = new ArrayList();
    private HashMap<String, FocusRequester> mFocusOwnersForFocusPolicy = new HashMap();
    private IAudioPolicyCallback mFocusPolicy = null;
    protected final Stack<FocusRequester> mFocusStack = new Stack();
    private boolean mNotifyFocusOwnerOnDuck = true;
    private boolean mRingOrCallActive = false;

    public class AudioFocusDeathHandler implements DeathRecipient {
        private IBinder mCb;

        AudioFocusDeathHandler(IBinder cb) {
            this.mCb = cb;
        }

        public void binderDied() {
            synchronized (MediaFocusControl.mAudioFocusLock) {
                if (MediaFocusControl.this.mFocusPolicy != null) {
                    MediaFocusControl.this.removeFocusEntryForExtPolicy(this.mCb);
                } else {
                    MediaFocusControl.this.removeFocusStackEntryOnDeath(this.mCb);
                }
            }
        }
    }

    protected MediaFocusControl(Context cntxt, PlayerFocusEnforcer pfe) {
        this.mContext = cntxt;
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mFocusEnforcer = pfe;
    }

    protected void dump(PrintWriter pw) {
        pw.println("\nMediaFocusControl dump time: " + DateFormat.getTimeInstance().format(new Date()));
        dumpFocusStack(pw);
        pw.println("\n");
        mEventLogger.dump(pw);
    }

    public boolean duckPlayers(FocusRequester winner, FocusRequester loser) {
        return this.mFocusEnforcer.duckPlayers(winner, loser);
    }

    public void unduckPlayers(FocusRequester winner) {
        this.mFocusEnforcer.unduckPlayers(winner);
    }

    public void mutePlayersForCall(int[] usagesToMute) {
        this.mFocusEnforcer.mutePlayersForCall(usagesToMute);
    }

    public void unmutePlayersForCall() {
        this.mFocusEnforcer.unmutePlayersForCall();
    }

    protected void discardAudioFocusOwner() {
        synchronized (mAudioFocusLock) {
            if (!this.mFocusStack.empty()) {
                FocusRequester exFocusOwner = (FocusRequester) this.mFocusStack.pop();
                exFocusOwner.handleFocusLoss(-1, null);
                exFocusOwner.release();
            }
        }
    }

    private void notifyTopOfAudioFocusStack(boolean isInExternal, int topUsage) {
        if (!this.mFocusStack.empty() && canReassignAudioFocus()) {
            FocusRequester nextFr = (FocusRequester) this.mFocusStack.peek();
            Log.v(TAG, "nextFr.getIsInExternal() = " + nextFr.getIsInExternal() + ", isInExternal = " + isInExternal + ", usage = " + nextFr.getAudioAttributes().getUsage() + ", topUsage = " + topUsage);
            if (nextFr.getIsInExternal() == isInExternal || ((isUsageAffectDesktopMedia(topUsage) && nextFr.getIsInExternal()) || isInExternal)) {
                nextFr.handleFocusGain(1);
            }
        }
    }

    private void propagateFocusLossFromGain_syncAf(int focusGain, FocusRequester fr) {
        Iterator<FocusRequester> stackIterator = this.mFocusStack.iterator();
        while (stackIterator.hasNext()) {
            FocusRequester nextFr = (FocusRequester) stackIterator.next();
            int usage = fr.getAudioAttributes().getUsage();
            if (nextFr.getIsInExternal() == fr.getIsInExternal() || ((isUsageAffectDesktopMedia(usage) && (fr.getIsInExternal() ^ 1) != 0) || fr.getIsInExternal())) {
                nextFr.handleExternalFocusGain(focusGain, fr);
            }
        }
    }

    private void dumpFocusStack(PrintWriter pw) {
        pw.println("\nAudio Focus stack entries (last is top of stack):");
        synchronized (mAudioFocusLock) {
            pw.println("mFocusStack:\n");
            Iterator<FocusRequester> stackIterator = this.mFocusStack.iterator();
            while (stackIterator.hasNext()) {
                ((FocusRequester) stackIterator.next()).dump(pw);
            }
            if (this.mFocusPolicy == null) {
                pw.println("No external focus policy\n");
            } else {
                pw.println("External focus policy: " + this.mFocusPolicy + ", focus owners:\n");
                dumpExtFocusPolicyFocusOwners(pw);
            }
        }
        pw.println("\n");
        pw.println(" Notify on duck:  " + this.mNotifyFocusOwnerOnDuck + "\n");
        pw.println(" In ring or call: " + this.mRingOrCallActive + "\n");
    }

    private void removeFocusStackEntry(String clientToRemove, boolean signal, boolean notifyFocusFollowers) {
        FocusRequester fr;
        if (this.mFocusStack.empty() || !((FocusRequester) this.mFocusStack.peek()).hasSameClient(clientToRemove)) {
            Iterator<FocusRequester> stackIterator = this.mFocusStack.iterator();
            while (stackIterator.hasNext()) {
                fr = (FocusRequester) stackIterator.next();
                if (fr.hasSameClient(clientToRemove)) {
                    Log.i(TAG, "AudioFocus  removeFocusStackEntry(): removing entry for " + clientToRemove);
                    stackIterator.remove();
                    fr.release();
                }
            }
            return;
        }
        fr = (FocusRequester) this.mFocusStack.pop();
        boolean isInExternal = fr.getIsInExternal();
        int usage = fr.getAudioAttributes().getUsage();
        fr.release();
        if (notifyFocusFollowers) {
            AudioFocusInfo afi = fr.toAudioFocusInfo();
            afi.clearLossReceived();
            notifyExtPolicyFocusLoss_syncAf(afi, false);
        }
        if (signal) {
            notifyTopOfAudioFocusStack(isInExternal, usage);
        }
    }

    private void removeFocusStackEntryOnDeath(IBinder cb) {
        boolean isTopOfStackForClientToRemove;
        if (this.mFocusStack.isEmpty()) {
            isTopOfStackForClientToRemove = false;
        } else {
            isTopOfStackForClientToRemove = ((FocusRequester) this.mFocusStack.peek()).hasSameBinder(cb);
        }
        boolean z = false;
        int usage = 0;
        if (isTopOfStackForClientToRemove) {
            FocusRequester topFr = (FocusRequester) this.mFocusStack.peek();
            z = topFr.getIsInExternal();
            usage = topFr.getAudioAttributes().getUsage();
        }
        Iterator<FocusRequester> stackIterator = this.mFocusStack.iterator();
        while (stackIterator.hasNext()) {
            FocusRequester fr = (FocusRequester) stackIterator.next();
            if (fr.hasSameBinder(cb)) {
                Log.i(TAG, "AudioFocus  removeFocusStackEntryOnDeath(): removing entry for " + cb);
                stackIterator.remove();
                fr.release();
            }
        }
        if (isTopOfStackForClientToRemove) {
            notifyTopOfAudioFocusStack(z, usage);
        }
    }

    private void removeFocusEntryForExtPolicy(IBinder cb) {
        if (!this.mFocusOwnersForFocusPolicy.isEmpty()) {
            Iterator<Entry<String, FocusRequester>> ownerIterator = this.mFocusOwnersForFocusPolicy.entrySet().iterator();
            while (ownerIterator.hasNext()) {
                FocusRequester fr = (FocusRequester) ((Entry) ownerIterator.next()).getValue();
                if (fr.hasSameBinder(cb)) {
                    ownerIterator.remove();
                    fr.release();
                    notifyExtFocusPolicyFocusAbandon_syncAf(fr.toAudioFocusInfo());
                    break;
                }
            }
        }
    }

    private boolean canReassignAudioFocus() {
        if (this.mFocusStack.isEmpty() || !isLockedFocusOwner((FocusRequester) this.mFocusStack.peek())) {
            return true;
        }
        return false;
    }

    private boolean isLockedFocusOwner(FocusRequester fr) {
        return !fr.hasSameClient("AudioFocus_For_Phone_Ring_And_Calls") ? fr.isLockedFocusOwner() : true;
    }

    private int pushBelowLockedFocusOwners(FocusRequester nfr) {
        int lastLockedFocusOwnerIndex = this.mFocusStack.size();
        for (int index = this.mFocusStack.size() - 1; index >= 0; index--) {
            if (isLockedFocusOwner((FocusRequester) this.mFocusStack.elementAt(index))) {
                lastLockedFocusOwnerIndex = index;
            }
        }
        if (lastLockedFocusOwnerIndex == this.mFocusStack.size()) {
            Log.e(TAG, "No exclusive focus owner found in propagateFocusLossFromGain_syncAf()", new Exception());
            propagateFocusLossFromGain_syncAf(nfr.getGainRequest(), nfr);
            this.mFocusStack.push(nfr);
            return 1;
        }
        this.mFocusStack.insertElementAt(nfr, lastLockedFocusOwnerIndex);
        return 2;
    }

    protected void setDuckingInExtPolicyAvailable(boolean available) {
        this.mNotifyFocusOwnerOnDuck = available ^ 1;
    }

    boolean mustNotifyFocusOwnerOnDuck() {
        return this.mNotifyFocusOwnerOnDuck;
    }

    void addFocusFollower(IAudioPolicyCallback ff) {
        if (ff != null) {
            synchronized (mAudioFocusLock) {
                boolean found = false;
                for (IAudioPolicyCallback pcb : this.mFocusFollowers) {
                    if (pcb.asBinder().equals(ff.asBinder())) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    return;
                }
                this.mFocusFollowers.add(ff);
                notifyExtPolicyCurrentFocusAsync(ff);
            }
        }
    }

    void removeFocusFollower(IAudioPolicyCallback ff) {
        if (ff != null) {
            synchronized (mAudioFocusLock) {
                for (IAudioPolicyCallback pcb : this.mFocusFollowers) {
                    if (pcb.asBinder().equals(ff.asBinder())) {
                        this.mFocusFollowers.remove(pcb);
                        break;
                    }
                }
            }
        }
    }

    void setFocusPolicy(IAudioPolicyCallback policy) {
        if (policy != null) {
            synchronized (mAudioFocusLock) {
                this.mFocusPolicy = policy;
            }
        }
    }

    void unsetFocusPolicy(IAudioPolicyCallback policy) {
        if (policy != null) {
            synchronized (mAudioFocusLock) {
                if (this.mFocusPolicy == policy) {
                    this.mFocusPolicy = null;
                }
            }
        }
    }

    void notifyExtPolicyCurrentFocusAsync(final IAudioPolicyCallback pcb) {
        IAudioPolicyCallback pcb2 = pcb;
        new Thread() {
            public void run() {
                synchronized (MediaFocusControl.mAudioFocusLock) {
                    if (MediaFocusControl.this.mFocusStack.isEmpty()) {
                        return;
                    }
                    try {
                        pcb.notifyAudioFocusGrant(((FocusRequester) MediaFocusControl.this.mFocusStack.peek()).toAudioFocusInfo(), 1);
                    } catch (RemoteException e) {
                        Log.e(MediaFocusControl.TAG, "Can't call notifyAudioFocusGrant() on IAudioPolicyCallback " + pcb.asBinder(), e);
                    }
                }
            }
        }.start();
    }

    void notifyExtPolicyFocusGrant_syncAf(AudioFocusInfo afi, int requestResult) {
        for (IAudioPolicyCallback pcb : this.mFocusFollowers) {
            try {
                pcb.notifyAudioFocusGrant(afi, requestResult);
            } catch (RemoteException e) {
                Log.e(TAG, "Can't call notifyAudioFocusGrant() on IAudioPolicyCallback " + pcb.asBinder(), e);
            }
        }
    }

    void notifyExtPolicyFocusLoss_syncAf(AudioFocusInfo afi, boolean wasDispatched) {
        for (IAudioPolicyCallback pcb : this.mFocusFollowers) {
            try {
                pcb.notifyAudioFocusLoss(afi, wasDispatched);
            } catch (RemoteException e) {
                Log.e(TAG, "Can't call notifyAudioFocusLoss() on IAudioPolicyCallback " + pcb.asBinder(), e);
            }
        }
    }

    boolean notifyExtFocusPolicyFocusRequest_syncAf(AudioFocusInfo afi, int requestResult, IAudioFocusDispatcher fd, IBinder cb) {
        if (this.mFocusPolicy == null) {
            return false;
        }
        FocusRequester existingFr = (FocusRequester) this.mFocusOwnersForFocusPolicy.get(afi.getClientId());
        if (existingFr != null) {
            if (!existingFr.hasSameDispatcher(fd)) {
                existingFr.release();
                this.mFocusOwnersForFocusPolicy.put(afi.getClientId(), HwServiceFactory.getHwFocusRequester(afi, fd, cb, new AudioFocusDeathHandler(cb), this, false));
            }
        } else if (requestResult == 1 || requestResult == 2) {
            this.mFocusOwnersForFocusPolicy.put(afi.getClientId(), HwServiceFactory.getHwFocusRequester(afi, fd, cb, new AudioFocusDeathHandler(cb), this, false));
        }
        try {
            this.mFocusPolicy.notifyAudioFocusRequest(afi, requestResult);
        } catch (RemoteException e) {
            Log.e(TAG, "Can't call notifyAudioFocusRequest() on IAudioPolicyCallback " + this.mFocusPolicy.asBinder(), e);
        }
        return true;
    }

    boolean notifyExtFocusPolicyFocusAbandon_syncAf(AudioFocusInfo afi) {
        if (this.mFocusPolicy == null) {
            return false;
        }
        FocusRequester fr = (FocusRequester) this.mFocusOwnersForFocusPolicy.remove(afi.getClientId());
        if (fr != null) {
            fr.release();
        }
        try {
            this.mFocusPolicy.notifyAudioFocusAbandon(afi);
        } catch (RemoteException e) {
            Log.e(TAG, "Can't call notifyAudioFocusAbandon() on IAudioPolicyCallback " + this.mFocusPolicy.asBinder(), e);
        }
        return true;
    }

    int dispatchFocusChange(AudioFocusInfo afi, int focusChange) {
        synchronized (mAudioFocusLock) {
            if (this.mFocusPolicy == null) {
                return 0;
            }
            FocusRequester fr = (FocusRequester) this.mFocusOwnersForFocusPolicy.get(afi.getClientId());
            if (fr == null) {
                return 0;
            }
            int dispatchFocusChange = fr.dispatchFocusChange(focusChange);
            return dispatchFocusChange;
        }
    }

    private void dumpExtFocusPolicyFocusOwners(PrintWriter pw) {
        for (Entry<String, FocusRequester> owner : this.mFocusOwnersForFocusPolicy.entrySet()) {
            ((FocusRequester) owner.getValue()).dump(pw);
        }
    }

    protected int getCurrentAudioFocus() {
        synchronized (mAudioFocusLock) {
            if (this.mFocusStack.empty()) {
                return 0;
            }
            int gainRequest = ((FocusRequester) this.mFocusStack.peek()).getGainRequest();
            return gainRequest;
        }
    }

    protected static int getFocusRampTimeMs(int focusGain, AudioAttributes attr) {
        switch (attr.getUsage()) {
            case 1:
            case 14:
                return 1000;
            case 2:
            case 3:
            case 5:
            case 7:
            case 8:
            case 9:
            case 10:
            case 13:
                return 500;
            case 4:
            case 6:
            case 11:
            case 12:
            case 16:
                return 700;
            default:
                return 0;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected int requestAudioFocus(AudioAttributes aa, int focusChangeHint, IBinder cb, IAudioFocusDispatcher fd, String clientId, String callingPackageName, int flags, int sdk) {
        mEventLogger.log(new StringEvent("requestAudioFocus() from uid/pid " + Binder.getCallingUid() + "/" + Binder.getCallingPid() + " clientId=" + clientId + " callingPack=" + callingPackageName + " req=" + focusChangeHint + " flags=0x" + Integer.toHexString(flags) + " sdk=" + sdk).printLog(TAG));
        if (3 == AudioAttributes.toLegacyStreamType(aa)) {
            LogPower.push(147, callingPackageName, Integer.toString(AudioAttributes.toLegacyStreamType(aa)));
        }
        if (!cb.pingBinder()) {
            Log.e(TAG, " AudioFocus DOA client for requestAudioFocus(), aborting.");
            return 0;
        } else if (this.mAppOps.noteOp(32, Binder.getCallingUid(), callingPackageName) != 0) {
            return 0;
        } else {
            boolean isInExternalDisplay = isMediaForDPExternalDisplay(aa, clientId, callingPackageName, Binder.getCallingUid());
            HwPCUtils.log(TAG, " requestAudioFocus isInExternalDisplay = " + isInExternalDisplay);
            synchronized (mAudioFocusLock) {
                if (this.mFocusStack.size() > 100) {
                    Log.e(TAG, "Max AudioFocus stack size reached, failing requestAudioFocus()");
                    return 0;
                }
                AudioFocusInfo audioFocusInfo;
                travelsFocusedStack();
                boolean enteringRingOrCall = (this.mRingOrCallActive ^ 1) & ("AudioFocus_For_Phone_Ring_And_Calls".compareTo(clientId) == 0 ? 1 : 0);
                if (enteringRingOrCall) {
                    this.mRingOrCallActive = true;
                }
                if (this.mFocusPolicy != null) {
                    audioFocusInfo = new AudioFocusInfo(aa, Binder.getCallingUid(), clientId, callingPackageName, focusChangeHint, 0, flags, sdk);
                } else {
                    audioFocusInfo = null;
                }
                boolean focusGrantDelayed = false;
                if (!canReassignAudioFocus()) {
                    if ((flags & 1) == 0) {
                        notifyExtFocusPolicyFocusRequest_syncAf(audioFocusInfo, 0, fd, cb);
                        return 0;
                    }
                    focusGrantDelayed = true;
                }
                if (notifyExtFocusPolicyFocusRequest_syncAf(audioFocusInfo, 2, fd, cb)) {
                    return 2;
                }
                AudioFocusDeathHandler afdh = new AudioFocusDeathHandler(cb);
                try {
                    cb.linkToDeath(afdh, 0);
                    if (!this.mFocusStack.empty() && ((FocusRequester) this.mFocusStack.peek()).hasSameClient(clientId)) {
                        FocusRequester fr = (FocusRequester) this.mFocusStack.peek();
                        if (fr.getGainRequest() == focusChangeHint && fr.getGrantFlags() == flags) {
                            cb.unlinkToDeath(afdh, 0);
                            notifyExtPolicyFocusGrant_syncAf(fr.toAudioFocusInfo(), 1);
                            return 1;
                        } else if (!focusGrantDelayed) {
                            this.mFocusStack.pop();
                            fr.release();
                        }
                    }
                    removeFocusStackEntry(clientId, false, false);
                    FocusRequester nfr = HwServiceFactory.getHwFocusRequester(aa, focusChangeHint, flags, fd, cb, clientId, afdh, callingPackageName, Binder.getCallingUid(), this, sdk, isInExternalDisplay);
                    nfr.setIsInExternal(isInExternalDisplay);
                    if (focusGrantDelayed) {
                        int requestResult = pushBelowLockedFocusOwners(nfr);
                        if (requestResult != 0) {
                            notifyExtPolicyFocusGrant_syncAf(nfr.toAudioFocusInfo(), requestResult);
                        }
                    } else {
                        if (!this.mFocusStack.empty()) {
                            propagateFocusLossFromGain_syncAf(focusChangeHint, nfr);
                        }
                        this.mFocusStack.push(nfr);
                        nfr.handleFocusGainFromRequest(1);
                        notifyExtPolicyFocusGrant_syncAf(nfr.toAudioFocusInfo(), 1);
                        if (enteringRingOrCall) {
                            runAudioCheckerForRingOrCallAsync(true);
                        }
                    }
                } catch (RemoteException e) {
                    Log.w(TAG, "AudioFocus  requestAudioFocus() could not link to " + cb + " binder death");
                    return 0;
                }
            }
        }
    }

    protected int abandonAudioFocus(IAudioFocusDispatcher fl, String clientId, AudioAttributes aa, String callingPackageName) {
        mEventLogger.log(new StringEvent("abandonAudioFocus() from uid/pid " + Binder.getCallingUid() + "/" + Binder.getCallingPid() + " clientId=" + clientId).printLog(TAG));
        try {
            HwPCUtils.log(TAG, " abandonAudioFocus isInExternalDisplay = " + isMediaForDPExternalDisplay(aa, clientId, callingPackageName, Binder.getCallingUid()));
            synchronized (mAudioFocusLock) {
                travelsFocusedStack();
                if (this.mFocusPolicy != null) {
                    if (notifyExtFocusPolicyFocusAbandon_syncAf(new AudioFocusInfo(aa, Binder.getCallingUid(), clientId, callingPackageName, 0, 0, 0, 0))) {
                        return 1;
                    }
                }
                boolean exitingRingOrCall = this.mRingOrCallActive & ("AudioFocus_For_Phone_Ring_And_Calls".compareTo(clientId) == 0 ? 1 : 0);
                if (exitingRingOrCall) {
                    this.mRingOrCallActive = false;
                }
                removeFocusStackEntry(clientId, true, true);
                if (exitingRingOrCall) {
                    runAudioCheckerForRingOrCallAsync(false);
                }
            }
        } catch (ConcurrentModificationException cme) {
            Log.e(TAG, "FATAL EXCEPTION AudioFocus  abandonAudioFocus() caused " + cme);
            cme.printStackTrace();
        }
        return 1;
    }

    protected void unregisterAudioFocusClient(String clientId) {
        synchronized (mAudioFocusLock) {
            removeFocusStackEntry(clientId, false, true);
        }
    }

    private void runAudioCheckerForRingOrCallAsync(final boolean enteringRingOrCall) {
        new Thread() {
            public void run() {
                if (enteringRingOrCall) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                synchronized (MediaFocusControl.mAudioFocusLock) {
                    if (MediaFocusControl.this.mRingOrCallActive) {
                        MediaFocusControl.this.mFocusEnforcer.mutePlayersForCall(MediaFocusControl.USAGES_TO_MUTE_IN_RING_OR_CALL);
                    } else {
                        MediaFocusControl.this.mFocusEnforcer.unmutePlayersForCall();
                    }
                }
            }
        }.start();
    }

    public void desktopModeChanged(boolean desktopMode) {
    }

    protected boolean isMediaForDPExternalDisplay(AudioAttributes aa, String clientId, String pkgName, int uid) {
        return false;
    }

    public boolean isPkgInExternalDisplay(String pkgName) {
        return false;
    }

    boolean isInDesktopMode() {
        return false;
    }

    protected void travelsFocusedStack() {
    }

    protected boolean isUsageAffectDesktopMedia(int usage) {
        return false;
    }
}
