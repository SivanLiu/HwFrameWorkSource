package com.android.server.audio;

import android.media.AudioAttributes;
import android.media.AudioFocusInfo;
import android.media.IAudioFocusDispatcher;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.server.audio.MediaFocusControl.AudioFocusDeathHandler;
import com.huawei.pgmng.log.LogPower;
import java.io.PrintWriter;
import java.util.NoSuchElementException;

public class FocusRequester {
    private static final boolean DEBUG = false;
    private static final String TAG = "MediaFocusControl";
    private final AudioAttributes mAttributes;
    private final int mCallingUid;
    private final String mClientId;
    private AudioFocusDeathHandler mDeathHandler;
    private final MediaFocusControl mFocusController;
    private IAudioFocusDispatcher mFocusDispatcher;
    private final int mFocusGainRequest;
    private int mFocusLossReceived;
    private boolean mFocusLossWasNotified;
    private final int mGrantFlags;
    protected boolean mIsInExternal = false;
    protected final String mPackageName;
    private final int mSdkTarget;
    private final IBinder mSourceRef;

    public boolean getIsInExternal() {
        return this.mIsInExternal;
    }

    public void setIsInExternal(boolean isInExternal) {
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public FocusRequester(AudioAttributes aa, int focusRequest, int grantFlags, IAudioFocusDispatcher afl, IBinder source, String id, AudioFocusDeathHandler hdlr, String pn, int uid, MediaFocusControl ctlr, int sdk) {
        this.mAttributes = aa;
        this.mFocusDispatcher = afl;
        this.mSourceRef = source;
        this.mClientId = id;
        this.mDeathHandler = hdlr;
        this.mPackageName = pn;
        this.mCallingUid = uid;
        this.mFocusGainRequest = focusRequest;
        this.mGrantFlags = grantFlags;
        this.mFocusLossReceived = 0;
        this.mFocusLossWasNotified = true;
        this.mFocusController = ctlr;
        this.mSdkTarget = sdk;
    }

    public FocusRequester(AudioFocusInfo afi, IAudioFocusDispatcher afl, IBinder source, AudioFocusDeathHandler hdlr, MediaFocusControl ctlr) {
        this.mAttributes = afi.getAttributes();
        this.mClientId = afi.getClientId();
        this.mPackageName = afi.getPackageName();
        this.mCallingUid = afi.getClientUid();
        this.mFocusGainRequest = afi.getGainRequest();
        this.mFocusLossReceived = 0;
        this.mFocusLossWasNotified = true;
        this.mGrantFlags = afi.getFlags();
        this.mSdkTarget = afi.getSdkTarget();
        this.mFocusDispatcher = afl;
        this.mSourceRef = source;
        this.mDeathHandler = hdlr;
        this.mFocusController = ctlr;
    }

    boolean hasSameClient(String otherClient) {
        boolean z = false;
        try {
            if (this.mClientId.compareTo(otherClient) == 0) {
                z = true;
            }
            return z;
        } catch (NullPointerException e) {
            return false;
        }
    }

    boolean isLockedFocusOwner() {
        return (this.mGrantFlags & 4) != 0;
    }

    boolean hasSameBinder(IBinder ib) {
        return this.mSourceRef != null && this.mSourceRef.equals(ib);
    }

    boolean hasSameDispatcher(IAudioFocusDispatcher fd) {
        return this.mFocusDispatcher != null && this.mFocusDispatcher.equals(fd);
    }

    boolean hasSamePackage(String pack) {
        boolean z = false;
        try {
            if (this.mPackageName.compareTo(pack) == 0) {
                z = true;
            }
            return z;
        } catch (NullPointerException e) {
            return false;
        }
    }

    boolean hasSameUid(int uid) {
        return this.mCallingUid == uid;
    }

    int getClientUid() {
        return this.mCallingUid;
    }

    String getClientId() {
        return this.mClientId;
    }

    int getGainRequest() {
        return this.mFocusGainRequest;
    }

    int getGrantFlags() {
        return this.mGrantFlags;
    }

    AudioAttributes getAudioAttributes() {
        return this.mAttributes;
    }

    int getSdkTarget() {
        return this.mSdkTarget;
    }

    private static String focusChangeToString(int focus) {
        switch (focus) {
            case -3:
                return "LOSS_TRANSIENT_CAN_DUCK";
            case -2:
                return "LOSS_TRANSIENT";
            case -1:
                return "LOSS";
            case 0:
                return "none";
            case 1:
                return "GAIN";
            case 2:
                return "GAIN_TRANSIENT";
            case 3:
                return "GAIN_TRANSIENT_MAY_DUCK";
            case 4:
                return "GAIN_TRANSIENT_EXCLUSIVE";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[invalid focus change");
                stringBuilder.append(focus);
                stringBuilder.append("]");
                return stringBuilder.toString();
        }
    }

    private String focusGainToString() {
        return focusChangeToString(this.mFocusGainRequest);
    }

    private String focusLossToString() {
        return focusChangeToString(this.mFocusLossReceived);
    }

    private static String flagsToString(int flags) {
        StringBuilder stringBuilder;
        String msg = new String();
        if ((flags & 1) != 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(msg);
            stringBuilder.append("DELAY_OK");
            msg = stringBuilder.toString();
        }
        if ((flags & 4) != 0) {
            if (!msg.isEmpty()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(msg);
                stringBuilder.append("|");
                msg = stringBuilder.toString();
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(msg);
            stringBuilder.append("LOCK");
            msg = stringBuilder.toString();
        }
        if ((flags & 2) == 0) {
            return msg;
        }
        if (!msg.isEmpty()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(msg);
            stringBuilder.append("|");
            msg = stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(msg);
        stringBuilder.append("PAUSES_ON_DUCKABLE_LOSS");
        return stringBuilder.toString();
    }

    void dump(PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  source:");
        stringBuilder.append(this.mSourceRef);
        stringBuilder.append(" -- pack: ");
        stringBuilder.append(this.mPackageName);
        stringBuilder.append(" -- client: ");
        stringBuilder.append(this.mClientId);
        stringBuilder.append(" -- gain: ");
        stringBuilder.append(focusGainToString());
        stringBuilder.append(" -- flags: ");
        stringBuilder.append(flagsToString(this.mGrantFlags));
        stringBuilder.append(" -- loss: ");
        stringBuilder.append(focusLossToString());
        stringBuilder.append(" -- notified: ");
        stringBuilder.append(this.mFocusLossWasNotified);
        stringBuilder.append(" -- uid: ");
        stringBuilder.append(this.mCallingUid);
        stringBuilder.append(" -- attr: ");
        stringBuilder.append(this.mAttributes);
        stringBuilder.append(" -- sdk:");
        stringBuilder.append(this.mSdkTarget);
        pw.println(stringBuilder.toString());
    }

    void release() {
        IBinder srcRef = this.mSourceRef;
        AudioFocusDeathHandler deathHdlr = this.mDeathHandler;
        if (!(srcRef == null || deathHdlr == null)) {
            try {
                srcRef.unlinkToDeath(deathHdlr, 0);
            } catch (NoSuchElementException e) {
            }
        }
        this.mDeathHandler = null;
        this.mFocusDispatcher = null;
    }

    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    /* JADX WARNING: Missing block: B:6:0x000f, code skipped:
            switch(r3.mFocusLossReceived) {
                case -3: goto L_0x0014;
                case -2: goto L_0x0014;
                case -1: goto L_0x0013;
                case 0: goto L_0x0014;
                default: goto L_0x0012;
            };
     */
    /* JADX WARNING: Missing block: B:7:0x0013, code skipped:
            return -1;
     */
    /* JADX WARNING: Missing block: B:8:0x0014, code skipped:
            return -2;
     */
    /* JADX WARNING: Missing block: B:10:0x0017, code skipped:
            switch(r3.mFocusLossReceived) {
                case -3: goto L_0x001d;
                case -2: goto L_0x001c;
                case -1: goto L_0x001b;
                case 0: goto L_0x001d;
                default: goto L_0x001a;
            };
     */
    /* JADX WARNING: Missing block: B:11:0x001b, code skipped:
            return -1;
     */
    /* JADX WARNING: Missing block: B:12:0x001c, code skipped:
            return -2;
     */
    /* JADX WARNING: Missing block: B:14:0x001e, code skipped:
            return -3;
     */
    /* JADX WARNING: Missing block: B:15:0x001f, code skipped:
            r0 = TAG;
            r1 = new java.lang.StringBuilder();
            r1.append("focusLossForGainRequest() for invalid focus request ");
            r1.append(r4);
            android.util.Log.e(r0, r1.toString());
     */
    /* JADX WARNING: Missing block: B:16:0x0036, code skipped:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int focusLossForGainRequest(int gainRequest) {
        switch (gainRequest) {
            case 1:
                switch (this.mFocusLossReceived) {
                    case -3:
                    case -2:
                    case -1:
                    case 0:
                        return -1;
                }
                break;
            case 2:
            case 4:
                break;
            case 3:
                break;
        }
    }

    @GuardedBy("MediaFocusControl.mAudioFocusLock")
    boolean handleFocusLossFromGain(int focusGain, FocusRequester frWinner, boolean forceDuck) {
        int focusLoss = focusLossForGainRequest(focusGain);
        handleFocusLoss(focusLoss, frWinner, forceDuck);
        return focusLoss == -1;
    }

    @GuardedBy("MediaFocusControl.mAudioFocusLock")
    void handleFocusGain(int focusGain) {
        try {
            this.mFocusLossReceived = 0;
            this.mFocusController.notifyExtPolicyFocusGrant_syncAf(toAudioFocusInfo(), 1);
            IAudioFocusDispatcher fd = this.mFocusDispatcher;
            if (fd != null && this.mFocusLossWasNotified) {
                fd.dispatchAudioFocusChange(focusGain, this.mClientId);
                LogPower.push(147, this.mPackageName);
            }
            this.mFocusController.unduckPlayers(this);
        } catch (RemoteException e) {
            Log.e(TAG, "Failure to signal gain of audio focus due to: ", e);
        }
    }

    @GuardedBy("MediaFocusControl.mAudioFocusLock")
    void handleFocusGainFromRequest(int focusRequestResult) {
        if (focusRequestResult == 1) {
            this.mFocusController.unduckPlayers(this);
        }
    }

    @GuardedBy("MediaFocusControl.mAudioFocusLock")
    void handleFocusLoss(int focusLoss, FocusRequester frWinner, boolean forceDuck) {
        try {
            if (focusLoss != this.mFocusLossReceived || this.mFocusController.isInDesktopMode()) {
                this.mFocusLossReceived = focusLoss;
                this.mFocusLossWasNotified = false;
                if (!this.mFocusController.mustNotifyFocusOwnerOnDuck() && this.mFocusLossReceived == -3 && (this.mGrantFlags & 2) == 0) {
                    this.mFocusController.notifyExtPolicyFocusLoss_syncAf(toAudioFocusInfo(), false);
                    return;
                }
                boolean handled = false;
                if (!(focusLoss != -3 || frWinner == null || frWinner.mCallingUid == this.mCallingUid)) {
                    String str;
                    StringBuilder stringBuilder;
                    if (!forceDuck && (this.mGrantFlags & 2) != 0) {
                        handled = false;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("not ducking uid ");
                        stringBuilder.append(this.mCallingUid);
                        stringBuilder.append(" - flags");
                        Log.v(str, stringBuilder.toString());
                    } else if (forceDuck || getSdkTarget() > 25) {
                        handled = this.mFocusController.duckPlayers(frWinner, this, forceDuck);
                    } else {
                        handled = false;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("not ducking uid ");
                        stringBuilder.append(this.mCallingUid);
                        stringBuilder.append(" - old SDK");
                        Log.v(str, stringBuilder.toString());
                    }
                }
                if (handled) {
                    this.mFocusController.notifyExtPolicyFocusLoss_syncAf(toAudioFocusInfo(), false);
                    return;
                }
                IAudioFocusDispatcher fd = this.mFocusDispatcher;
                if (fd != null) {
                    this.mFocusController.notifyExtPolicyFocusLoss_syncAf(toAudioFocusInfo(), true);
                    this.mFocusLossWasNotified = true;
                    fd.dispatchAudioFocusChange(this.mFocusLossReceived, this.mClientId);
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failure to signal loss of audio focus due to:", e);
        }
    }

    int dispatchFocusChange(int focusChange) {
        if (this.mFocusDispatcher == null || focusChange == 0) {
            return 0;
        }
        StringBuilder stringBuilder;
        if ((focusChange == 3 || focusChange == 4 || focusChange == 2 || focusChange == 1) && this.mFocusGainRequest != focusChange) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("focus gain was requested with ");
            stringBuilder.append(this.mFocusGainRequest);
            stringBuilder.append(", dispatching ");
            stringBuilder.append(focusChange);
            Log.w(str, stringBuilder.toString());
        } else if (focusChange == -3 || focusChange == -2 || focusChange == -1) {
            this.mFocusLossReceived = focusChange;
        }
        try {
            this.mFocusDispatcher.dispatchAudioFocusChange(focusChange, this.mClientId);
            return 1;
        } catch (RemoteException e) {
            String str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("dispatchFocusChange: error talking to focus listener ");
            stringBuilder.append(this.mClientId);
            Log.e(str2, stringBuilder.toString(), e);
            return 0;
        }
    }

    void dispatchFocusResultFromExtPolicy(int requestResult) {
        IAudioFocusDispatcher iAudioFocusDispatcher = this.mFocusDispatcher;
        try {
            this.mFocusDispatcher.dispatchFocusResultFromExtPolicy(requestResult, this.mClientId);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("dispatchFocusResultFromExtPolicy: error talking to focus listener");
            stringBuilder.append(this.mClientId);
            Log.e(str, stringBuilder.toString(), e);
        }
    }

    AudioFocusInfo toAudioFocusInfo() {
        return new AudioFocusInfo(this.mAttributes, this.mCallingUid, this.mClientId, this.mPackageName, this.mFocusGainRequest, this.mFocusLossReceived, this.mGrantFlags, this.mSdkTarget);
    }
}
