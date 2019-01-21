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
import com.android.internal.annotations.GuardedBy;
import com.android.server.HwServiceFactory;
import com.android.server.audio.AudioEventLogger.StringEvent;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import com.huawei.pgmng.log.LogPower;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
    @GuardedBy("mExtFocusChangeLock")
    private long mExtFocusChangeCounter;
    private final Object mExtFocusChangeLock = new Object();
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

    /*  JADX ERROR: JadxRuntimeException in pass: SSATransform
        jadx.core.utils.exceptions.JadxRuntimeException: Not initialized variable reg: 11, insn: 0x0025: IF  (r11 ?[int, OBJECT, ARRAY, boolean, byte, short, char]) == (0 ?[int, OBJECT, ARRAY, boolean, byte, short, char])  -> B:13:0x0049, block:B:9:0x0025, method: com.android.server.audio.MediaFocusControl.notifyExtFocusPolicyFocusRequest_syncAf(android.media.AudioFocusInfo, android.media.IAudioFocusDispatcher, android.os.IBinder):boolean
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:162)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:133)
        	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
        	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    boolean notifyExtFocusPolicyFocusRequest_syncAf(android.media.AudioFocusInfo r14, android.media.IAudioFocusDispatcher r15, android.os.IBinder r16) {
        /*
        r13 = this;
        r7 = r13;
        r8 = r14;
        r9 = r16;
        r0 = r7.mFocusPolicy;
        r10 = 0;
        if (r0 != 0) goto L_0x000a;
        return r10;
        r1 = r7.mExtFocusChangeLock;
        monitor-enter(r1);
        r2 = r7.mExtFocusChangeCounter;	 Catch:{ all -> 0x0086 }
        r4 = 1;	 Catch:{ all -> 0x0086 }
        r4 = r4 + r2;	 Catch:{ all -> 0x0086 }
        r7.mExtFocusChangeCounter = r4;	 Catch:{ all -> 0x0086 }
        r8.setGen(r2);	 Catch:{ all -> 0x0086 }
        monitor-exit(r1);	 Catch:{ all -> 0x0086 }
        r0 = r7.mFocusOwnersForFocusPolicy;
        r1 = r8.getClientId();
        r0 = r0.get(r1);
        r11 = r0;
        r11 = (com.android.server.audio.FocusRequester) r11;
        if (r11 == 0) goto L_0x0049;
        r12 = r15;
        r0 = r11.hasSameDispatcher(r12);
        if (r0 != 0) goto L_0x0061;
        r11.release();
        r4 = new com.android.server.audio.MediaFocusControl$AudioFocusDeathHandler;
        r4.<init>(r9);
        r6 = 0;
        r1 = r8;
        r2 = r12;
        r3 = r9;
        r5 = r7;
        r0 = com.android.server.HwServiceFactory.getHwFocusRequester(r1, r2, r3, r4, r5, r6);
        r1 = r7.mFocusOwnersForFocusPolicy;
        r2 = r8.getClientId();
        r1.put(r2, r0);
        goto L_0x0061;
        r12 = r15;
        r4 = new com.android.server.audio.MediaFocusControl$AudioFocusDeathHandler;
        r4.<init>(r9);
        r6 = 0;
        r1 = r8;
        r2 = r12;
        r3 = r9;
        r5 = r7;
        r0 = com.android.server.HwServiceFactory.getHwFocusRequester(r1, r2, r3, r4, r5, r6);
        r1 = r7.mFocusOwnersForFocusPolicy;
        r2 = r8.getClientId();
        r1.put(r2, r0);
        r0 = r7.mFocusPolicy;	 Catch:{ RemoteException -> 0x0068 }
        r1 = 1;	 Catch:{ RemoteException -> 0x0068 }
        r0.notifyAudioFocusRequest(r8, r1);	 Catch:{ RemoteException -> 0x0068 }
        return r1;
        r0 = move-exception;
        r1 = "MediaFocusControl";
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "Can't call notifyAudioFocusRequest() on IAudioPolicyCallback ";
        r2.append(r3);
        r3 = r7.mFocusPolicy;
        r3 = r3.asBinder();
        r2.append(r3);
        r2 = r2.toString();
        android.util.Log.e(r1, r2, r0);
        return r10;
        r0 = move-exception;
        r12 = r15;
        monitor-exit(r1);	 Catch:{ all -> 0x008a }
        throw r0;
        r0 = move-exception;
        goto L_0x0088;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.audio.MediaFocusControl.notifyExtFocusPolicyFocusRequest_syncAf(android.media.AudioFocusInfo, android.media.IAudioFocusDispatcher, android.os.IBinder):boolean");
    }

    protected MediaFocusControl(Context cntxt, PlayerFocusEnforcer pfe) {
        this.mContext = cntxt;
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mFocusEnforcer = pfe;
    }

    protected void dump(PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\nMediaFocusControl dump time: ");
        stringBuilder.append(DateFormat.getTimeInstance().format(new Date()));
        pw.println(stringBuilder.toString());
        dumpFocusStack(pw);
        pw.println("\n");
        mEventLogger.dump(pw);
    }

    public boolean duckPlayers(FocusRequester winner, FocusRequester loser, boolean forceDuck) {
        return this.mFocusEnforcer.duckPlayers(winner, loser, forceDuck);
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
                exFocusOwner.handleFocusLoss(-1, null, false);
                exFocusOwner.release();
            }
        }
    }

    private void notifyTopOfAudioFocusStack(boolean isInExternal, int topUsage) {
        if (!this.mFocusStack.empty() && canReassignAudioFocus()) {
            FocusRequester nextFr = (FocusRequester) this.mFocusStack.peek();
            int usage = nextFr.getAudioAttributes().getUsage();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("nextFr.getIsInExternal() = ");
            stringBuilder.append(nextFr.getIsInExternal());
            stringBuilder.append(", isInExternal = ");
            stringBuilder.append(isInExternal);
            stringBuilder.append(", usage = ");
            stringBuilder.append(usage);
            stringBuilder.append(", topUsage = ");
            stringBuilder.append(topUsage);
            Log.v(str, stringBuilder.toString());
            if (nextFr.getIsInExternal() == isInExternal || ((isUsageAffectDesktopMedia(topUsage) && nextFr.getIsInExternal()) || isInExternal)) {
                nextFr.handleFocusGain(1);
            }
        }
    }

    @GuardedBy("mAudioFocusLock")
    private void propagateFocusLossFromGain_syncAf(int focusGain, FocusRequester fr, boolean forceDuck) {
        List<String> clientsToRemove = new LinkedList();
        Iterator it = this.mFocusStack.iterator();
        while (it.hasNext()) {
            FocusRequester focusLoser = (FocusRequester) it.next();
            int usage = fr.getAudioAttributes().getUsage();
            if ((focusLoser.getIsInExternal() == fr.getIsInExternal() || ((isUsageAffectDesktopMedia(usage) && !fr.getIsInExternal()) || fr.getIsInExternal())) && focusLoser.handleFocusLossFromGain(focusGain, fr, forceDuck)) {
                clientsToRemove.add(focusLoser.getClientId());
            }
        }
        for (String clientToRemove : clientsToRemove) {
            removeFocusStackEntry(clientToRemove, false, true);
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
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("External focus policy: ");
                stringBuilder.append(this.mFocusPolicy);
                stringBuilder.append(", focus owners:\n");
                pw.println(stringBuilder.toString());
                dumpExtFocusPolicyFocusOwners(pw);
            }
        }
        pw.println("\n");
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" Notify on duck:  ");
        stringBuilder2.append(this.mNotifyFocusOwnerOnDuck);
        stringBuilder2.append("\n");
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" In ring or call: ");
        stringBuilder2.append(this.mRingOrCallActive);
        stringBuilder2.append("\n");
        pw.println(stringBuilder2.toString());
    }

    @GuardedBy("mAudioFocusLock")
    private void removeFocusStackEntry(String clientToRemove, boolean signal, boolean notifyFocusFollowers) {
        if (this.mFocusStack.empty() || !((FocusRequester) this.mFocusStack.peek()).hasSameClient(clientToRemove)) {
            Iterator<FocusRequester> stackIterator = this.mFocusStack.iterator();
            while (stackIterator.hasNext()) {
                FocusRequester fr = (FocusRequester) stackIterator.next();
                if (fr.hasSameClient(clientToRemove)) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("AudioFocus  removeFocusStackEntry(): removing entry for ");
                    stringBuilder.append(clientToRemove);
                    Log.i(str, stringBuilder.toString());
                    stackIterator.remove();
                    fr.release();
                }
            }
            return;
        }
        FocusRequester fr2 = (FocusRequester) this.mFocusStack.pop();
        boolean isInExternal = fr2.getIsInExternal();
        int usage = fr2.getAudioAttributes().getUsage();
        fr2.release();
        if (notifyFocusFollowers) {
            AudioFocusInfo afi = fr2.toAudioFocusInfo();
            afi.clearLossReceived();
            notifyExtPolicyFocusLoss_syncAf(afi, false);
        }
        if (signal) {
            notifyTopOfAudioFocusStack(isInExternal, usage);
        }
    }

    @GuardedBy("mAudioFocusLock")
    private void removeFocusStackEntryOnDeath(IBinder cb) {
        boolean isTopOfStackForClientToRemove = !this.mFocusStack.isEmpty() && ((FocusRequester) this.mFocusStack.peek()).hasSameBinder(cb);
        boolean isInExternal = false;
        int usage = 0;
        if (isTopOfStackForClientToRemove) {
            FocusRequester topFr = (FocusRequester) this.mFocusStack.peek();
            isInExternal = topFr.getIsInExternal();
            usage = topFr.getAudioAttributes().getUsage();
        }
        Iterator<FocusRequester> stackIterator = this.mFocusStack.iterator();
        while (stackIterator.hasNext()) {
            FocusRequester fr = (FocusRequester) stackIterator.next();
            if (fr.hasSameBinder(cb)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AudioFocus  removeFocusStackEntryOnDeath(): removing entry for ");
                stringBuilder.append(cb);
                Log.i(str, stringBuilder.toString());
                stackIterator.remove();
                fr.release();
            }
        }
        if (isTopOfStackForClientToRemove) {
            notifyTopOfAudioFocusStack(isInExternal, usage);
        }
    }

    @GuardedBy("mAudioFocusLock")
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
        return fr.hasSameClient("AudioFocus_For_Phone_Ring_And_Calls") || fr.isLockedFocusOwner();
    }

    @GuardedBy("mAudioFocusLock")
    private int pushBelowLockedFocusOwners(FocusRequester nfr) {
        int lastLockedFocusOwnerIndex = this.mFocusStack.size();
        for (int index = this.mFocusStack.size() - 1; index >= 0; index--) {
            if (isLockedFocusOwner((FocusRequester) this.mFocusStack.elementAt(index))) {
                lastLockedFocusOwnerIndex = index;
            }
        }
        if (lastLockedFocusOwnerIndex == this.mFocusStack.size()) {
            Log.e(TAG, "No exclusive focus owner found in propagateFocusLossFromGain_syncAf()", new Exception());
            propagateFocusLossFromGain_syncAf(nfr.getGainRequest(), nfr, false);
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
                Iterator it = this.mFocusFollowers.iterator();
                while (it.hasNext()) {
                    if (((IAudioPolicyCallback) it.next()).asBinder().equals(ff.asBinder())) {
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
                Iterator it = this.mFocusFollowers.iterator();
                while (it.hasNext()) {
                    IAudioPolicyCallback pcb = (IAudioPolicyCallback) it.next();
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

    void notifyExtPolicyCurrentFocusAsync(IAudioPolicyCallback pcb) {
        final IAudioPolicyCallback pcb2 = pcb;
        new Thread() {
            public void run() {
                synchronized (MediaFocusControl.mAudioFocusLock) {
                    if (MediaFocusControl.this.mFocusStack.isEmpty()) {
                        return;
                    }
                    try {
                        pcb2.notifyAudioFocusGrant(((FocusRequester) MediaFocusControl.this.mFocusStack.peek()).toAudioFocusInfo(), 1);
                    } catch (RemoteException e) {
                        String str = MediaFocusControl.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Can't call notifyAudioFocusGrant() on IAudioPolicyCallback ");
                        stringBuilder.append(pcb2.asBinder());
                        Log.e(str, stringBuilder.toString(), e);
                    }
                }
            }
        }.start();
    }

    void notifyExtPolicyFocusGrant_syncAf(AudioFocusInfo afi, int requestResult) {
        Iterator it = this.mFocusFollowers.iterator();
        while (it.hasNext()) {
            IAudioPolicyCallback pcb = (IAudioPolicyCallback) it.next();
            try {
                pcb.notifyAudioFocusGrant(afi, requestResult);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Can't call notifyAudioFocusGrant() on IAudioPolicyCallback ");
                stringBuilder.append(pcb.asBinder());
                Log.e(str, stringBuilder.toString(), e);
            }
        }
    }

    void notifyExtPolicyFocusLoss_syncAf(AudioFocusInfo afi, boolean wasDispatched) {
        Iterator it = this.mFocusFollowers.iterator();
        while (it.hasNext()) {
            IAudioPolicyCallback pcb = (IAudioPolicyCallback) it.next();
            try {
                pcb.notifyAudioFocusLoss(afi, wasDispatched);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Can't call notifyAudioFocusLoss() on IAudioPolicyCallback ");
                stringBuilder.append(pcb.asBinder());
                Log.e(str, stringBuilder.toString(), e);
            }
        }
    }

    /* JADX WARNING: Missing block: B:8:0x0010, code skipped:
            r0 = (com.android.server.audio.FocusRequester) r5.mFocusOwnersForFocusPolicy.get(r6.getClientId());
     */
    /* JADX WARNING: Missing block: B:9:0x001c, code skipped:
            if (r0 == null) goto L_0x0021;
     */
    /* JADX WARNING: Missing block: B:10:0x001e, code skipped:
            r0.dispatchFocusResultFromExtPolicy(r7);
     */
    /* JADX WARNING: Missing block: B:11:0x0021, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void setFocusRequestResultFromExtPolicy(AudioFocusInfo afi, int requestResult) {
        synchronized (this.mExtFocusChangeLock) {
            if (afi.getGen() > this.mExtFocusChangeCounter) {
            }
        }
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't call notifyAudioFocusAbandon() on IAudioPolicyCallback ");
            stringBuilder.append(this.mFocusPolicy.asBinder());
            Log.e(str, stringBuilder.toString(), e);
        }
        return true;
    }

    int dispatchFocusChange(AudioFocusInfo afi, int focusChange) {
        synchronized (mAudioFocusLock) {
            if (this.mFocusPolicy == null) {
                return 0;
            }
            FocusRequester fr;
            if (focusChange == -1) {
                fr = (FocusRequester) this.mFocusOwnersForFocusPolicy.remove(afi.getClientId());
            } else {
                fr = (FocusRequester) this.mFocusOwnersForFocusPolicy.get(afi.getClientId());
            }
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

    /* JADX WARNING: Removed duplicated region for block: B:99:0x01c7 A:{Catch:{ all -> 0x01ef, all -> 0x022b }} */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x01b8 A:{Catch:{ all -> 0x01ef, all -> 0x022b }} */
    /* JADX WARNING: Missing block: B:98:0x01c6, code skipped:
            return r1;
     */
    /* JADX WARNING: Missing block: B:109:0x01ee, code skipped:
            return r15;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected int requestAudioFocus(AudioAttributes aa, int focusChangeHint, IBinder cb, IAudioFocusDispatcher fd, String clientId, String callingPackageName, int flags, int sdk, boolean forceDuck) {
        Throwable th;
        boolean z;
        boolean z2;
        boolean isInExternalDisplay;
        IBinder iBinder;
        int i = focusChangeHint;
        IBinder iBinder2 = cb;
        String str = clientId;
        String str2 = callingPackageName;
        int i2 = flags;
        AudioEventLogger audioEventLogger = mEventLogger;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("requestAudioFocus() from uid/pid ");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(SliceAuthority.DELIMITER);
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(" clientId=");
        stringBuilder.append(str);
        stringBuilder.append(" callingPack=");
        stringBuilder.append(str2);
        stringBuilder.append(" req=");
        stringBuilder.append(i);
        stringBuilder.append(" flags=0x");
        stringBuilder.append(Integer.toHexString(flags));
        stringBuilder.append(" sdk=");
        stringBuilder.append(sdk);
        audioEventLogger.log(new StringEvent(stringBuilder.toString()).printLog(TAG));
        if (3 == AudioAttributes.toLegacyStreamType(aa)) {
            LogPower.push(147, str2, Integer.toString(AudioAttributes.toLegacyStreamType(aa)));
        }
        if (!cb.pingBinder()) {
            Log.e(TAG, " AudioFocus DOA client for requestAudioFocus(), aborting.");
            return 0;
        } else if (this.mAppOps.noteOp(32, Binder.getCallingUid(), str2) != 0) {
            return 0;
        } else {
            AudioAttributes audioAttributes = aa;
            boolean isInExternalDisplay2 = isMediaForDPExternalDisplay(audioAttributes, str, str2, Binder.getCallingUid());
            String str3 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" requestAudioFocus isInExternalDisplay = ");
            stringBuilder2.append(isInExternalDisplay2);
            HwPCUtils.log(str3, stringBuilder2.toString());
            synchronized (mAudioFocusLock) {
                try {
                    if (this.mFocusStack.size() > 100) {
                        try {
                            Log.e(TAG, "Max AudioFocus stack size reached, failing requestAudioFocus()");
                            return 0;
                        } catch (Throwable th2) {
                            th = th2;
                            z = forceDuck;
                            z2 = isInExternalDisplay2;
                            isInExternalDisplay2 = iBinder2;
                            throw th;
                        }
                    }
                    int i3;
                    AudioFocusInfo afiForExtPolicy;
                    travelsFocusedStack();
                    boolean enteringRingOrCall = (this.mRingOrCallActive ^ 1) & ("AudioFocus_For_Phone_Ring_And_Calls".compareTo(str) == 0 ? 1 : 0);
                    if (enteringRingOrCall) {
                        this.mRingOrCallActive = true;
                    }
                    if (this.mFocusPolicy != null) {
                        try {
                            AudioFocusInfo audioFocusInfo = audioFocusInfo;
                            i3 = 100;
                            isInExternalDisplay = isInExternalDisplay2;
                            try {
                                audioFocusInfo = new AudioFocusInfo(audioAttributes, Binder.getCallingUid(), str, str2, i, 0, i2, sdk);
                            } catch (Throwable th3) {
                                th = th3;
                                z = forceDuck;
                                iBinder = iBinder2;
                                z2 = isInExternalDisplay;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            z = forceDuck;
                            z2 = isInExternalDisplay2;
                            isInExternalDisplay2 = iBinder2;
                            throw th;
                        }
                    }
                    i3 = 100;
                    isInExternalDisplay = isInExternalDisplay2;
                    afiForExtPolicy = null;
                    AudioFocusInfo afiForExtPolicy2 = afiForExtPolicy;
                    boolean focusGrantDelayed = false;
                    try {
                        int i4;
                        if (canReassignAudioFocus()) {
                            i4 = 0;
                        } else if ((i2 & 1) == 0) {
                            return 0;
                        } else {
                            i4 = 0;
                            focusGrantDelayed = true;
                        }
                        boolean focusGrantDelayed2 = focusGrantDelayed;
                        IAudioFocusDispatcher iAudioFocusDispatcher = fd;
                        if (notifyExtFocusPolicyFocusRequest_syncAf(afiForExtPolicy2, iAudioFocusDispatcher, iBinder2)) {
                            return i3;
                        }
                        FocusRequester fr;
                        AudioFocusDeathHandler afdh = new AudioFocusDeathHandler(iBinder2);
                        try {
                            IBinder iBinder3;
                            iBinder2.linkToDeath(afdh, i4);
                            try {
                                int i5;
                                int i6;
                                if (!this.mFocusStack.empty()) {
                                    if (((FocusRequester) this.mFocusStack.peek()).hasSameClient(str)) {
                                        fr = (FocusRequester) this.mFocusStack.peek();
                                        if (fr.getGainRequest() == i && fr.getGrantFlags() == i2) {
                                            iBinder2.unlinkToDeath(afdh, i4);
                                            notifyExtPolicyFocusGrant_syncAf(fr.toAudioFocusInfo(), 1);
                                            return 1;
                                        }
                                        i5 = 1;
                                        if (!focusGrantDelayed2) {
                                            this.mFocusStack.pop();
                                            fr.release();
                                        }
                                        removeFocusStackEntry(str, i4, i4);
                                        iBinder3 = iBinder2;
                                        i6 = i5;
                                        fr = HwServiceFactory.getHwFocusRequester(aa, i, i2, iAudioFocusDispatcher, iBinder3, str, afdh, str2, Binder.getCallingUid(), this, sdk, isInExternalDisplay);
                                        fr.setIsInExternal(isInExternalDisplay);
                                        if (focusGrantDelayed2) {
                                            if (this.mFocusStack.empty()) {
                                                z = forceDuck;
                                            } else {
                                                propagateFocusLossFromGain_syncAf(i, fr, forceDuck);
                                            }
                                            this.mFocusStack.push(fr);
                                            fr.handleFocusGainFromRequest(i6);
                                            notifyExtPolicyFocusGrant_syncAf(fr.toAudioFocusInfo(), i6);
                                            if ((i6 & enteringRingOrCall) != 0) {
                                                runAudioCheckerForRingOrCallAsync(i6);
                                            }
                                        } else {
                                            i4 = pushBelowLockedFocusOwners(fr);
                                            if (i4 != 0) {
                                                notifyExtPolicyFocusGrant_syncAf(fr.toAudioFocusInfo(), i4);
                                            }
                                        }
                                    }
                                }
                                i5 = 1;
                                removeFocusStackEntry(str, i4, i4);
                                iBinder3 = iBinder2;
                                i6 = i5;
                            } catch (Throwable th5) {
                                th = th5;
                                z = forceDuck;
                                z2 = isInExternalDisplay;
                                iBinder = iBinder2;
                                throw th;
                            }
                            try {
                                fr = HwServiceFactory.getHwFocusRequester(aa, i, i2, iAudioFocusDispatcher, iBinder3, str, afdh, str2, Binder.getCallingUid(), this, sdk, isInExternalDisplay);
                            } catch (Throwable th6) {
                                th = th6;
                                z = forceDuck;
                                z2 = isInExternalDisplay;
                                iBinder = cb;
                                throw th;
                            }
                        } catch (RemoteException e) {
                            z = forceDuck;
                            AudioFocusDeathHandler audioFocusDeathHandler = afdh;
                            AudioFocusInfo audioFocusInfo2 = afiForExtPolicy2;
                            z2 = isInExternalDisplay;
                            RemoteException remoteException = e;
                            String str4 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("AudioFocus  requestAudioFocus() could not link to ");
                            stringBuilder3.append(cb);
                            stringBuilder3.append(" binder death");
                            Log.w(str4, stringBuilder3.toString());
                            return i4;
                        } catch (Throwable th7) {
                            th = th7;
                            throw th;
                        }
                        try {
                            fr.setIsInExternal(isInExternalDisplay);
                            if (focusGrantDelayed2) {
                            }
                        } catch (Throwable th8) {
                            th = th8;
                            iBinder = cb;
                            throw th;
                        }
                    } catch (Throwable th9) {
                        th = th9;
                        z = forceDuck;
                        iBinder = iBinder2;
                        z2 = isInExternalDisplay;
                        throw th;
                    }
                } catch (Throwable th10) {
                    th = th10;
                    z = forceDuck;
                    z2 = isInExternalDisplay2;
                    isInExternalDisplay2 = iBinder2;
                    throw th;
                }
            }
        }
    }

    protected int abandonAudioFocus(IAudioFocusDispatcher fl, String clientId, AudioAttributes aa, String callingPackageName) {
        ConcurrentModificationException cme;
        String str = clientId;
        AudioEventLogger audioEventLogger = mEventLogger;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("abandonAudioFocus() from uid/pid ");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(SliceAuthority.DELIMITER);
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(" clientId=");
        stringBuilder.append(str);
        audioEventLogger.log(new StringEvent(stringBuilder.toString()).printLog(TAG));
        AudioAttributes audioAttributes;
        String str2;
        try {
            audioAttributes = aa;
            str2 = callingPackageName;
            try {
                boolean isInExternalDisplay = isMediaForDPExternalDisplay(audioAttributes, str, str2, Binder.getCallingUid());
                String str3 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" abandonAudioFocus isInExternalDisplay = ");
                stringBuilder2.append(isInExternalDisplay);
                HwPCUtils.log(str3, stringBuilder2.toString());
                synchronized (mAudioFocusLock) {
                    travelsFocusedStack();
                    if (this.mFocusPolicy != null) {
                        if (notifyExtFocusPolicyFocusAbandon_syncAf(new AudioFocusInfo(audioAttributes, Binder.getCallingUid(), str, str2, 0, 0, 0, 0))) {
                            return 1;
                        }
                    }
                    boolean exitingRingOrCall = this.mRingOrCallActive & ("AudioFocus_For_Phone_Ring_And_Calls".compareTo(str) == 0 ? 1 : 0);
                    if (exitingRingOrCall) {
                        this.mRingOrCallActive = false;
                    }
                    removeFocusStackEntry(str, true, true);
                    if ((1 & exitingRingOrCall) != 0) {
                        runAudioCheckerForRingOrCallAsync(false);
                    }
                }
            } catch (ConcurrentModificationException e) {
                cme = e;
            }
        } catch (ConcurrentModificationException e2) {
            cme = e2;
            audioAttributes = aa;
            str2 = callingPackageName;
            String str4 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("FATAL EXCEPTION AudioFocus  abandonAudioFocus() caused ");
            stringBuilder.append(cme);
            Log.e(str4, stringBuilder.toString());
            cme.printStackTrace();
            return 1;
        }
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
