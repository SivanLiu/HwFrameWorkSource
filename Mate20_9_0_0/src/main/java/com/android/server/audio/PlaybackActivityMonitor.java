package com.android.server.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioPlaybackConfiguration.PlayerDeathMonitor;
import android.media.AudioSystem;
import android.media.IPlaybackConfigDispatcher;
import android.media.PlayerBase.PlayerIdCard;
import android.media.VolumeShaper.Configuration;
import android.media.VolumeShaper.Configuration.Builder;
import android.media.VolumeShaper.Operation;
import android.os.Binder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
import com.android.server.audio.AudioEventLogger.Event;
import com.android.server.audio.AudioEventLogger.StringEvent;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import com.huawei.pgmng.log.LogPower;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public final class PlaybackActivityMonitor implements PlayerDeathMonitor, PlayerFocusEnforcer {
    private static final boolean DEBUG = true;
    private static final Configuration DUCK_ID = new Configuration(1);
    private static final Configuration DUCK_VSHAPE = new Builder().setId(1).setCurve(new float[]{0.0f, 1.0f}, new float[]{1.0f, 0.2f}).setOptionFlags(2).setDuration((long) MediaFocusControl.getFocusRampTimeMs(3, new AudioAttributes.Builder().setUsage(5).build())).build();
    private static final int FLAGS_FOR_SILENCE_OVERRIDE = 192;
    private static final Operation PLAY_CREATE_IF_NEEDED = new Operation.Builder(Operation.PLAY).createIfNeeded().build();
    private static final Operation PLAY_SKIP_RAMP = new Operation.Builder(PLAY_CREATE_IF_NEEDED).setXOffset(1.0f).build();
    public static final String TAG = "AudioService.PlaybackActivityMonitor";
    private static final int[] UNDUCKABLE_PLAYER_TYPES = new int[]{13, 3};
    private static final int VOLUME_SHAPER_SYSTEM_DUCK_ID = 1;
    private static final AudioEventLogger sEventLogger = new AudioEventLogger(100, "playback activity as reported through PlayerBase");
    private final ArrayList<Integer> mBannedUids = new ArrayList();
    private final ArrayList<PlayMonitorClient> mClients = new ArrayList();
    private final Context mContext;
    private final DuckingManager mDuckingManager = new DuckingManager();
    private boolean mHasPublicClients = false;
    private final int mMaxAlarmVolume;
    private MediaFocusControl mMfc;
    private final ArrayList<Integer> mMutedPlayers = new ArrayList();
    private final Object mPlayerLock = new Object();
    private final HashMap<Integer, AudioPlaybackConfiguration> mPlayers = new HashMap();
    private int mPrivilegedAlarmActiveCount = 0;
    private int mSavedAlarmVolume = -1;

    private static final class DuckingManager {
        private final HashMap<Integer, DuckedApp> mDuckers;

        private static final class DuckedApp {
            private final ArrayList<Integer> mDuckedPlayers = new ArrayList();
            private final int mUid;

            DuckedApp(int uid) {
                this.mUid = uid;
            }

            void dump(PrintWriter pw) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("\t uid:");
                stringBuilder.append(this.mUid);
                stringBuilder.append(" piids:");
                pw.print(stringBuilder.toString());
                Iterator it = this.mDuckedPlayers.iterator();
                while (it.hasNext()) {
                    int piid = ((Integer) it.next()).intValue();
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" ");
                    stringBuilder2.append(piid);
                    pw.print(stringBuilder2.toString());
                }
                pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            }

            void addDuck(AudioPlaybackConfiguration apc, boolean skipRamp) {
                int piid = new Integer(apc.getPlayerInterfaceId()).intValue();
                if (this.mDuckedPlayers.contains(Integer.valueOf(piid))) {
                    String str = PlaybackActivityMonitor.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("player piid:");
                    stringBuilder.append(piid);
                    stringBuilder.append(" already ducked");
                    Log.v(str, stringBuilder.toString());
                    return;
                }
                try {
                    PlaybackActivityMonitor.sEventLogger.log(new DuckEvent(apc, skipRamp).printLog(PlaybackActivityMonitor.TAG));
                    apc.getPlayerProxy().applyVolumeShaper(PlaybackActivityMonitor.DUCK_VSHAPE, skipRamp ? PlaybackActivityMonitor.PLAY_SKIP_RAMP : PlaybackActivityMonitor.PLAY_CREATE_IF_NEEDED);
                    this.mDuckedPlayers.add(Integer.valueOf(piid));
                } catch (Exception e) {
                    String str2 = PlaybackActivityMonitor.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Error ducking player piid:");
                    stringBuilder2.append(piid);
                    stringBuilder2.append(" uid:");
                    stringBuilder2.append(this.mUid);
                    Log.e(str2, stringBuilder2.toString(), e);
                }
            }

            void removeUnduckAll(HashMap<Integer, AudioPlaybackConfiguration> players) {
                Iterator it = this.mDuckedPlayers.iterator();
                while (it.hasNext()) {
                    int piid = ((Integer) it.next()).intValue();
                    AudioPlaybackConfiguration apc = (AudioPlaybackConfiguration) players.get(Integer.valueOf(piid));
                    if (apc != null) {
                        StringBuilder stringBuilder;
                        try {
                            AudioEventLogger access$100 = PlaybackActivityMonitor.sEventLogger;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("unducking piid:");
                            stringBuilder.append(piid);
                            access$100.log(new StringEvent(stringBuilder.toString()).printLog(PlaybackActivityMonitor.TAG));
                            apc.getPlayerProxy().applyVolumeShaper(PlaybackActivityMonitor.DUCK_ID, Operation.REVERSE);
                        } catch (Exception e) {
                            String str = PlaybackActivityMonitor.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error unducking player piid:");
                            stringBuilder.append(piid);
                            stringBuilder.append(" uid:");
                            stringBuilder.append(this.mUid);
                            Log.e(str, stringBuilder.toString(), e);
                        }
                    } else {
                        String str2 = PlaybackActivityMonitor.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Error unducking player piid:");
                        stringBuilder2.append(piid);
                        stringBuilder2.append(", player not found for uid ");
                        stringBuilder2.append(this.mUid);
                        Log.v(str2, stringBuilder2.toString());
                    }
                }
                this.mDuckedPlayers.clear();
            }

            void removeReleased(AudioPlaybackConfiguration apc) {
                this.mDuckedPlayers.remove(new Integer(apc.getPlayerInterfaceId()));
            }
        }

        private DuckingManager() {
            this.mDuckers = new HashMap();
        }

        synchronized void duckUid(int uid, ArrayList<AudioPlaybackConfiguration> apcsToDuck) {
            String str = PlaybackActivityMonitor.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DuckingManager: duckUid() uid:");
            stringBuilder.append(uid);
            Log.v(str, stringBuilder.toString());
            if (!this.mDuckers.containsKey(Integer.valueOf(uid))) {
                this.mDuckers.put(Integer.valueOf(uid), new DuckedApp(uid));
            }
            DuckedApp da = (DuckedApp) this.mDuckers.get(Integer.valueOf(uid));
            Iterator it = apcsToDuck.iterator();
            while (it.hasNext()) {
                da.addDuck((AudioPlaybackConfiguration) it.next(), false);
            }
        }

        synchronized void unduckUid(int uid, HashMap<Integer, AudioPlaybackConfiguration> players) {
            String str = PlaybackActivityMonitor.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DuckingManager: unduckUid() uid:");
            stringBuilder.append(uid);
            Log.v(str, stringBuilder.toString());
            DuckedApp da = (DuckedApp) this.mDuckers.remove(Integer.valueOf(uid));
            if (da != null) {
                da.removeUnduckAll(players);
            }
        }

        synchronized void checkDuck(AudioPlaybackConfiguration apc) {
            String str = PlaybackActivityMonitor.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DuckingManager: checkDuck() player piid:");
            stringBuilder.append(apc.getPlayerInterfaceId());
            stringBuilder.append(" uid:");
            stringBuilder.append(apc.getClientUid());
            Log.v(str, stringBuilder.toString());
            DuckedApp da = (DuckedApp) this.mDuckers.get(Integer.valueOf(apc.getClientUid()));
            if (da != null) {
                da.addDuck(apc, true);
            }
        }

        synchronized void dump(PrintWriter pw) {
            for (DuckedApp da : this.mDuckers.values()) {
                da.dump(pw);
            }
        }

        synchronized void removeReleased(AudioPlaybackConfiguration apc) {
            int uid = apc.getClientUid();
            String str = PlaybackActivityMonitor.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DuckingManager: removedReleased() player piid: ");
            stringBuilder.append(apc.getPlayerInterfaceId());
            stringBuilder.append(" uid:");
            stringBuilder.append(uid);
            Log.v(str, stringBuilder.toString());
            DuckedApp da = (DuckedApp) this.mDuckers.get(Integer.valueOf(uid));
            if (da != null) {
                da.removeReleased(apc);
            }
        }
    }

    private static final class PlayMonitorClient implements DeathRecipient {
        static final int MAX_ERRORS = 5;
        static PlaybackActivityMonitor sListenerDeathMonitor;
        final IPlaybackConfigDispatcher mDispatcherCb;
        int mErrorCount = 0;
        final boolean mIsPrivileged;

        PlayMonitorClient(IPlaybackConfigDispatcher pcdb, boolean isPrivileged) {
            this.mDispatcherCb = pcdb;
            this.mIsPrivileged = isPrivileged;
        }

        public void binderDied() {
            Log.w(PlaybackActivityMonitor.TAG, "client died");
            sListenerDeathMonitor.unregisterPlaybackCallback(this.mDispatcherCb);
        }

        boolean init() {
            try {
                this.mDispatcherCb.asBinder().linkToDeath(this, 0);
                return true;
            } catch (RemoteException e) {
                Log.w(PlaybackActivityMonitor.TAG, "Could not link to client death", e);
                return false;
            }
        }

        void release() {
            this.mDispatcherCb.asBinder().unlinkToDeath(this, 0);
        }
    }

    private static final class AudioAttrEvent extends Event {
        private final AudioAttributes mPlayerAttr;
        private final int mPlayerIId;

        AudioAttrEvent(int piid, AudioAttributes attr) {
            this.mPlayerIId = piid;
            this.mPlayerAttr = attr;
        }

        public String eventToString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("player piid:");
            stringBuilder.append(this.mPlayerIId);
            stringBuilder.append(" new AudioAttributes:");
            stringBuilder.append(this.mPlayerAttr);
            return new String(stringBuilder.toString());
        }
    }

    private static final class DuckEvent extends Event {
        private final int mClientPid;
        private final int mClientUid;
        private final int mPlayerIId;
        private final boolean mSkipRamp;

        DuckEvent(AudioPlaybackConfiguration apc, boolean skipRamp) {
            this.mPlayerIId = apc.getPlayerInterfaceId();
            this.mSkipRamp = skipRamp;
            this.mClientUid = apc.getClientUid();
            this.mClientPid = apc.getClientPid();
        }

        public String eventToString() {
            StringBuilder stringBuilder = new StringBuilder("ducking player piid:");
            stringBuilder.append(this.mPlayerIId);
            stringBuilder.append(" uid/pid:");
            stringBuilder.append(this.mClientUid);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(this.mClientPid);
            stringBuilder.append(" skip ramp:");
            stringBuilder.append(this.mSkipRamp);
            return stringBuilder.toString();
        }
    }

    private static final class NewPlayerEvent extends Event {
        private final int mClientPid;
        private final int mClientUid;
        private final AudioAttributes mPlayerAttr;
        private final int mPlayerIId;
        private final int mPlayerType;

        NewPlayerEvent(AudioPlaybackConfiguration apc) {
            this.mPlayerIId = apc.getPlayerInterfaceId();
            this.mPlayerType = apc.getPlayerType();
            this.mClientUid = apc.getClientUid();
            this.mClientPid = apc.getClientPid();
            this.mPlayerAttr = apc.getAudioAttributes();
        }

        public String eventToString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("new player piid:");
            stringBuilder.append(this.mPlayerIId);
            stringBuilder.append(" uid/pid:");
            stringBuilder.append(this.mClientUid);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(this.mClientPid);
            stringBuilder.append(" type:");
            stringBuilder.append(AudioPlaybackConfiguration.toLogFriendlyPlayerType(this.mPlayerType));
            stringBuilder.append(" attr:");
            stringBuilder.append(this.mPlayerAttr);
            return new String(stringBuilder.toString());
        }
    }

    private static final class PlayerEvent extends Event {
        final int mPlayerIId;
        final int mState;

        PlayerEvent(int piid, int state) {
            this.mPlayerIId = piid;
            this.mState = state;
        }

        public String eventToString() {
            StringBuilder stringBuilder = new StringBuilder("player piid:");
            stringBuilder.append(this.mPlayerIId);
            stringBuilder.append(" state:");
            stringBuilder.append(AudioPlaybackConfiguration.toLogFriendlyPlayerState(this.mState));
            return stringBuilder.toString();
        }
    }

    private static final class PlayerOpPlayAudioEvent extends Event {
        final boolean mHasOp;
        final int mPlayerIId;
        final int mUid;

        PlayerOpPlayAudioEvent(int piid, boolean hasOp, int uid) {
            this.mPlayerIId = piid;
            this.mHasOp = hasOp;
            this.mUid = uid;
        }

        public String eventToString() {
            StringBuilder stringBuilder = new StringBuilder("player piid:");
            stringBuilder.append(this.mPlayerIId);
            stringBuilder.append(" has OP_PLAY_AUDIO:");
            stringBuilder.append(this.mHasOp);
            stringBuilder.append(" in uid:");
            stringBuilder.append(this.mUid);
            return stringBuilder.toString();
        }
    }

    PlaybackActivityMonitor(Context context, int maxAlarmVolume) {
        this.mContext = context;
        this.mMaxAlarmVolume = maxAlarmVolume;
        PlayMonitorClient.sListenerDeathMonitor = this;
        AudioPlaybackConfiguration.sPlayerDeathMonitor = this;
    }

    public void disableAudioForUid(boolean disable, int uid) {
        synchronized (this.mPlayerLock) {
            int index = this.mBannedUids.indexOf(new Integer(uid));
            AudioEventLogger audioEventLogger;
            StringBuilder stringBuilder;
            if (index >= 0) {
                if (!disable) {
                    audioEventLogger = sEventLogger;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("unbanning uid:");
                    stringBuilder.append(uid);
                    audioEventLogger.log(new StringEvent(stringBuilder.toString()));
                    this.mBannedUids.remove(index);
                }
            } else if (disable) {
                for (AudioPlaybackConfiguration apc : this.mPlayers.values()) {
                    checkBanPlayer(apc, uid);
                }
                audioEventLogger = sEventLogger;
                stringBuilder = new StringBuilder();
                stringBuilder.append("banning uid:");
                stringBuilder.append(uid);
                audioEventLogger.log(new StringEvent(stringBuilder.toString()));
                this.mBannedUids.add(new Integer(uid));
            }
        }
    }

    private boolean checkBanPlayer(AudioPlaybackConfiguration apc, int uid) {
        boolean toBan = apc.getClientUid() == uid;
        if (toBan) {
            int piid = apc.getPlayerInterfaceId();
            try {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("banning player ");
                stringBuilder.append(piid);
                stringBuilder.append(" uid:");
                stringBuilder.append(uid);
                Log.v(str, stringBuilder.toString());
                apc.getPlayerProxy().pause();
            } catch (Exception e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("error banning player ");
                stringBuilder2.append(piid);
                stringBuilder2.append(" uid:");
                stringBuilder2.append(uid);
                Log.e(str2, stringBuilder2.toString(), e);
            }
        }
        return toBan;
    }

    public int trackPlayer(PlayerIdCard pic) {
        int newPiid = AudioSystem.newAudioPlayerId();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("trackPlayer() new piid=");
        stringBuilder.append(newPiid);
        Log.v(str, stringBuilder.toString());
        AudioPlaybackConfiguration apc = new AudioPlaybackConfiguration(pic, newPiid, Binder.getCallingUid(), Binder.getCallingPid());
        apc.init();
        sEventLogger.log(new NewPlayerEvent(apc));
        synchronized (this.mPlayerLock) {
            this.mPlayers.put(Integer.valueOf(newPiid), apc);
        }
        return newPiid;
    }

    public void playerAttributes(int piid, AudioAttributes attr, int binderUid) {
        boolean change;
        synchronized (this.mPlayerLock) {
            boolean change2;
            AudioPlaybackConfiguration apc = (AudioPlaybackConfiguration) this.mPlayers.get(new Integer(piid));
            if (checkConfigurationCaller(piid, apc, binderUid)) {
                sEventLogger.log(new AudioAttrEvent(piid, attr));
                change2 = apc.handleAudioAttributesEvent(attr);
            } else {
                Log.e(TAG, "Error updating audio attributes");
                change2 = false;
            }
            change = change2;
        }
        if (change) {
            dispatchPlaybackChange(false);
        }
    }

    private void checkVolumeForPrivilegedAlarm(AudioPlaybackConfiguration apc, int event) {
        if ((event != 2 && apc.getPlayerState() != 2) || (apc.getAudioAttributes().getAllFlags() & FLAGS_FOR_SILENCE_OVERRIDE) != FLAGS_FOR_SILENCE_OVERRIDE || apc.getAudioAttributes().getUsage() != 4 || this.mContext.checkPermission("android.permission.MODIFY_PHONE_STATE", apc.getClientPid(), apc.getClientUid()) != 0) {
            return;
        }
        int i;
        if (event == 2 && apc.getPlayerState() != 2) {
            i = this.mPrivilegedAlarmActiveCount;
            this.mPrivilegedAlarmActiveCount = i + 1;
            if (i == 0) {
                this.mSavedAlarmVolume = AudioSystem.getStreamVolumeIndex(4, 2);
                AudioSystem.setStreamVolumeIndex(4, this.mMaxAlarmVolume, 2);
            }
        } else if (event != 2 && apc.getPlayerState() == 2) {
            i = this.mPrivilegedAlarmActiveCount - 1;
            this.mPrivilegedAlarmActiveCount = i;
            if (i == 0 && AudioSystem.getStreamVolumeIndex(4, 2) == this.mMaxAlarmVolume) {
                AudioSystem.setStreamVolumeIndex(4, this.mSavedAlarmVolume, 2);
            }
        }
    }

    /* JADX WARNING: Missing block: B:30:0x00b5, code skipped:
            if (r3 == false) goto L_0x00be;
     */
    /* JADX WARNING: Missing block: B:31:0x00b7, code skipped:
            if (r10 != 0) goto L_0x00bb;
     */
    /* JADX WARNING: Missing block: B:32:0x00b9, code skipped:
            r5 = true;
     */
    /* JADX WARNING: Missing block: B:33:0x00bb, code skipped:
            dispatchPlaybackChange(r5);
     */
    /* JADX WARNING: Missing block: B:34:0x00be, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void playerEvent(int piid, int event, int binderUid) {
        String str = TAG;
        r3 = new Object[2];
        boolean z = false;
        r3[0] = Integer.valueOf(piid);
        r3[1] = Integer.valueOf(event);
        Log.v(str, String.format("playerEvent(piid=%d, event=%d)", r3));
        synchronized (this.mPlayerLock) {
            AudioPlaybackConfiguration apc = (AudioPlaybackConfiguration) this.mPlayers.get(new Integer(piid));
            if (apc == null) {
                return;
            }
            sEventLogger.log(new PlayerEvent(piid, event));
            if (event == 2) {
                Iterator it = this.mBannedUids.iterator();
                while (it.hasNext()) {
                    if (checkBanPlayer(apc, ((Integer) it.next()).intValue())) {
                        AudioEventLogger audioEventLogger = sEventLogger;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("not starting piid:");
                        stringBuilder.append(piid);
                        stringBuilder.append(" ,is banned");
                        audioEventLogger.log(new StringEvent(stringBuilder.toString()));
                        return;
                    }
                }
            }
            if (apc.getPlayerType() == 3) {
                return;
            }
            boolean change;
            if (checkConfigurationCaller(piid, apc, binderUid)) {
                checkVolumeForPrivilegedAlarm(apc, event);
                change = apc.handleStateEvent(event);
            } else {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error handling event ");
                stringBuilder2.append(event);
                Log.e(str2, stringBuilder2.toString());
                change = false;
            }
            if (change && event == 2) {
                this.mDuckingManager.checkDuck(apc);
            }
        }
    }

    public void playerHasOpPlayAudio(int piid, boolean hasOpPlayAudio, int binderUid) {
        sEventLogger.log(new PlayerOpPlayAudioEvent(piid, hasOpPlayAudio, binderUid));
    }

    public void releasePlayer(int piid, int binderUid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("releasePlayer() for piid=");
        stringBuilder.append(piid);
        Log.v(str, stringBuilder.toString());
        boolean change = false;
        synchronized (this.mPlayerLock) {
            AudioPlaybackConfiguration apc = (AudioPlaybackConfiguration) this.mPlayers.get(new Integer(piid));
            if (checkConfigurationCaller(piid, apc, binderUid)) {
                AudioEventLogger audioEventLogger = sEventLogger;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("releasing player piid:");
                stringBuilder2.append(piid);
                audioEventLogger.log(new StringEvent(stringBuilder2.toString()));
                this.mPlayers.remove(new Integer(piid));
                this.mDuckingManager.removeReleased(apc);
                checkVolumeForPrivilegedAlarm(apc, 0);
                change = apc.handleStateEvent(0);
                LogPower.push(163, String.valueOf(Binder.getCallingPid()), String.valueOf(piid), String.valueOf(Binder.getCallingUid()));
            }
        }
        if (change) {
            dispatchPlaybackChange(true);
        }
    }

    public void playerDeath(int piid) {
        releasePlayer(piid, 0);
    }

    protected void dump(PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\nPlaybackActivityMonitor dump time: ");
        stringBuilder.append(DateFormat.getTimeInstance().format(new Date()));
        pw.println(stringBuilder.toString());
        synchronized (this.mPlayerLock) {
            Iterator it;
            StringBuilder stringBuilder2;
            int piid;
            pw.println("\n  playback listeners:");
            synchronized (this.mClients) {
                it = this.mClients.iterator();
                while (it.hasNext()) {
                    PlayMonitorClient pmc = (PlayMonitorClient) it.next();
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" ");
                    stringBuilder2.append(pmc.mIsPrivileged ? "(S)" : "(P)");
                    stringBuilder2.append(pmc.toString());
                    pw.print(stringBuilder2.toString());
                }
            }
            pw.println("\n");
            pw.println("\n  players:");
            List<Integer> piidIntList = new ArrayList(this.mPlayers.keySet());
            Collections.sort(piidIntList);
            for (Integer piidInt : piidIntList) {
                AudioPlaybackConfiguration apc = (AudioPlaybackConfiguration) this.mPlayers.get(piidInt);
                if (apc != null) {
                    apc.dump(pw);
                }
            }
            pw.println("\n  ducked players piids:");
            this.mDuckingManager.dump(pw);
            pw.print("\n  muted player piids:");
            it = this.mMutedPlayers.iterator();
            while (it.hasNext()) {
                piid = ((Integer) it.next()).intValue();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" ");
                stringBuilder2.append(piid);
                pw.print(stringBuilder2.toString());
            }
            pw.println();
            pw.print("\n  banned uids:");
            it = this.mBannedUids.iterator();
            while (it.hasNext()) {
                piid = ((Integer) it.next()).intValue();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" ");
                stringBuilder2.append(piid);
                pw.print(stringBuilder2.toString());
            }
            pw.println("\n");
            sEventLogger.dump(pw);
        }
    }

    private static boolean checkConfigurationCaller(int piid, AudioPlaybackConfiguration apc, int binderUid) {
        if (apc == null) {
            return false;
        }
        if (binderUid == 0 || apc.getClientUid() == binderUid) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Forbidden operation from uid ");
        stringBuilder.append(binderUid);
        stringBuilder.append(" for player ");
        stringBuilder.append(piid);
        Log.e(str, stringBuilder.toString());
        return false;
    }

    /* JADX WARNING: Missing block: B:8:0x000e, code skipped:
            r0 = TAG;
            r1 = new java.lang.StringBuilder();
            r1.append("dispatchPlaybackChange to ");
            r1.append(r9.mClients.size());
            r1.append(" clients");
            android.util.Log.v(r0, r1.toString());
            r1 = r9.mPlayerLock;
     */
    /* JADX WARNING: Missing block: B:9:0x0031, code skipped:
            monitor-enter(r1);
     */
    /* JADX WARNING: Missing block: B:12:0x0038, code skipped:
            if (r9.mPlayers.isEmpty() == false) goto L_0x003c;
     */
    /* JADX WARNING: Missing block: B:13:0x003a, code skipped:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:14:0x003b, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:15:0x003c, code skipped:
            r0 = new java.util.ArrayList(r9.mPlayers.values());
     */
    /* JADX WARNING: Missing block: B:16:0x0047, code skipped:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:17:0x0048, code skipped:
            r2 = r9.mClients;
     */
    /* JADX WARNING: Missing block: B:18:0x004a, code skipped:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:21:0x0051, code skipped:
            if (r9.mClients.isEmpty() == false) goto L_0x0055;
     */
    /* JADX WARNING: Missing block: B:22:0x0053, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:23:0x0054, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:25:0x0057, code skipped:
            if (r9.mHasPublicClients == false) goto L_0x005e;
     */
    /* JADX WARNING: Missing block: B:26:0x0059, code skipped:
            r1 = anonymizeForPublicConsumption(r0);
     */
    /* JADX WARNING: Missing block: B:27:0x005e, code skipped:
            r1 = null;
     */
    /* JADX WARNING: Missing block: B:28:0x005f, code skipped:
            r3 = r9.mClients.iterator();
     */
    /* JADX WARNING: Missing block: B:30:0x0069, code skipped:
            if (r3.hasNext() == false) goto L_0x00bd;
     */
    /* JADX WARNING: Missing block: B:31:0x006b, code skipped:
            r4 = (com.android.server.audio.PlaybackActivityMonitor.PlayMonitorClient) r3.next();
     */
    /* JADX WARNING: Missing block: B:34:0x0074, code skipped:
            if (r4.mErrorCount >= 5) goto L_0x00bc;
     */
    /* JADX WARNING: Missing block: B:36:0x0078, code skipped:
            if (r4.mIsPrivileged == false) goto L_0x0087;
     */
    /* JADX WARNING: Missing block: B:37:0x007a, code skipped:
            android.util.Log.v(TAG, "configsSystem");
            r4.mDispatcherCb.dispatchPlaybackConfigChange(r0, r10);
     */
    /* JADX WARNING: Missing block: B:38:0x0087, code skipped:
            android.util.Log.v(TAG, "configsPublic");
            r4.mDispatcherCb.dispatchPlaybackConfigChange(r1, false);
     */
    /* JADX WARNING: Missing block: B:39:0x0095, code skipped:
            r5 = move-exception;
     */
    /* JADX WARNING: Missing block: B:41:?, code skipped:
            r4.mErrorCount++;
            r6 = TAG;
            r7 = new java.lang.StringBuilder();
            r7.append("Error (");
            r7.append(r4.mErrorCount);
            r7.append(") trying to dispatch playback config change to ");
            r7.append(r4);
            android.util.Log.e(r6, r7.toString(), r5);
     */
    /* JADX WARNING: Missing block: B:43:0x00bd, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:44:0x00be, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void dispatchPlaybackChange(boolean iplayerReleased) {
        synchronized (this.mClients) {
            if (this.mClients.isEmpty()) {
            }
        }
    }

    private ArrayList<AudioPlaybackConfiguration> anonymizeForPublicConsumption(List<AudioPlaybackConfiguration> sysConfigs) {
        ArrayList<AudioPlaybackConfiguration> publicConfigs = new ArrayList();
        for (AudioPlaybackConfiguration config : sysConfigs) {
            if (config.isActive()) {
                publicConfigs.add(AudioPlaybackConfiguration.anonymizedCopy(config));
            }
        }
        return publicConfigs;
    }

    public boolean duckPlayers(FocusRequester winner, FocusRequester loser, boolean forceDuck) {
        Log.v(TAG, String.format("duckPlayers: uids winner=%d loser=%d", new Object[]{Integer.valueOf(winner.getClientUid()), Integer.valueOf(loser.getClientUid())}));
        synchronized (this.mPlayerLock) {
            if (this.mPlayers.isEmpty()) {
                return true;
            }
            ArrayList<AudioPlaybackConfiguration> apcsToDuck = new ArrayList();
            for (AudioPlaybackConfiguration apc : this.mPlayers.values()) {
                if (!winner.hasSameUid(apc.getClientUid()) && loser.hasSameUid(apc.getClientUid()) && apc.getPlayerState() == 2) {
                    String str;
                    StringBuilder stringBuilder;
                    if (!forceDuck && apc.getAudioAttributes().getContentType() == 1) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("not ducking player ");
                        stringBuilder.append(apc.getPlayerInterfaceId());
                        stringBuilder.append(" uid:");
                        stringBuilder.append(apc.getClientUid());
                        stringBuilder.append(" pid:");
                        stringBuilder.append(apc.getClientPid());
                        stringBuilder.append(" - SPEECH");
                        Log.v(str, stringBuilder.toString());
                        return false;
                    } else if (ArrayUtils.contains(UNDUCKABLE_PLAYER_TYPES, apc.getPlayerType())) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("not ducking player ");
                        stringBuilder.append(apc.getPlayerInterfaceId());
                        stringBuilder.append(" uid:");
                        stringBuilder.append(apc.getClientUid());
                        stringBuilder.append(" pid:");
                        stringBuilder.append(apc.getClientPid());
                        stringBuilder.append(" due to type:");
                        stringBuilder.append(AudioPlaybackConfiguration.toLogFriendlyPlayerType(apc.getPlayerType()));
                        Log.v(str, stringBuilder.toString());
                        return false;
                    } else {
                        apcsToDuck.add(apc);
                    }
                }
            }
            this.mDuckingManager.duckUid(loser.getClientUid(), apcsToDuck);
            return true;
        }
    }

    public void unduckPlayers(FocusRequester winner) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unduckPlayers: uids winner=");
        stringBuilder.append(winner.getClientUid());
        Log.v(str, stringBuilder.toString());
        synchronized (this.mPlayerLock) {
            this.mDuckingManager.unduckUid(winner.getClientUid(), this.mPlayers);
        }
    }

    public void mutePlayersForCall(int[] usagesToMute) {
        String log = new String("mutePlayersForCall: usages=");
        String log2 = log;
        for (int usage : usagesToMute) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(log2);
            stringBuilder.append(" ");
            stringBuilder.append(usage);
            log2 = stringBuilder.toString();
        }
        Log.v(TAG, log2);
        synchronized (this.mPlayerLock) {
            for (Integer piid : this.mPlayers.keySet()) {
                AudioPlaybackConfiguration apc = (AudioPlaybackConfiguration) this.mPlayers.get(piid);
                if (apc != null) {
                    int playerUsage = apc.getAudioAttributes().getUsage();
                    boolean mute = false;
                    for (int usageToMute : usagesToMute) {
                        if (playerUsage == usageToMute) {
                            mute = true;
                            break;
                        }
                    }
                    boolean isInExternalDisplay = false;
                    if (!(this.mMfc == null || apc.getPkgName() == null)) {
                        isInExternalDisplay = this.mMfc.isPkgInExternalDisplay(apc.getPkgName());
                        String str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("isInExternalDisplay = ");
                        stringBuilder2.append(isInExternalDisplay);
                        Log.v(str, stringBuilder2.toString());
                    }
                    if (mute && !isInExternalDisplay) {
                        StringBuilder stringBuilder3;
                        try {
                            AudioEventLogger audioEventLogger = sEventLogger;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("call: muting piid:");
                            stringBuilder3.append(piid);
                            stringBuilder3.append(" uid:");
                            stringBuilder3.append(apc.getClientUid());
                            audioEventLogger.log(new StringEvent(stringBuilder3.toString()).printLog(TAG));
                            apc.getPlayerProxy().setVolume(0.0f);
                            this.mMutedPlayers.add(new Integer(piid.intValue()));
                        } catch (Exception e) {
                            String str2 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("call: error muting player ");
                            stringBuilder3.append(piid);
                            Log.e(str2, stringBuilder3.toString(), e);
                        }
                    }
                }
            }
        }
    }

    public void unmutePlayersForCall() {
        Log.v(TAG, "unmutePlayersForCall()");
        synchronized (this.mPlayerLock) {
            if (this.mMutedPlayers.isEmpty()) {
                return;
            }
            Iterator it = this.mMutedPlayers.iterator();
            while (it.hasNext()) {
                int piid = ((Integer) it.next()).intValue();
                AudioPlaybackConfiguration apc = (AudioPlaybackConfiguration) this.mPlayers.get(Integer.valueOf(piid));
                if (apc != null) {
                    StringBuilder stringBuilder;
                    try {
                        AudioEventLogger audioEventLogger = sEventLogger;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("call: unmuting piid:");
                        stringBuilder.append(piid);
                        audioEventLogger.log(new StringEvent(stringBuilder.toString()).printLog(TAG));
                        apc.getPlayerProxy().setVolume(1.0f);
                    } catch (Exception e) {
                        String str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("call: error unmuting player ");
                        stringBuilder.append(piid);
                        stringBuilder.append(" uid:");
                        stringBuilder.append(apc.getClientUid());
                        Log.e(str, stringBuilder.toString(), e);
                    }
                }
            }
            this.mMutedPlayers.clear();
        }
    }

    void registerPlaybackCallback(IPlaybackConfigDispatcher pcdb, boolean isPrivileged) {
        Log.v(TAG, "registerPlaybackCallback");
        if (pcdb != null) {
            synchronized (this.mClients) {
                PlayMonitorClient pmc = new PlayMonitorClient(pcdb, isPrivileged);
                if (pmc.init()) {
                    if (!isPrivileged) {
                        this.mHasPublicClients = true;
                    }
                    this.mClients.add(pmc);
                }
            }
        }
    }

    void unregisterPlaybackCallback(IPlaybackConfigDispatcher pcdb) {
        Log.v(TAG, "unregisterPlaybackCallback");
        if (pcdb != null) {
            synchronized (this.mClients) {
                Iterator<PlayMonitorClient> clientIterator = this.mClients.iterator();
                boolean hasPublicClients = false;
                while (clientIterator.hasNext()) {
                    PlayMonitorClient pmc = (PlayMonitorClient) clientIterator.next();
                    if (pcdb.equals(pmc.mDispatcherCb)) {
                        pmc.release();
                        clientIterator.remove();
                    } else if (!pmc.mIsPrivileged) {
                        hasPublicClients = true;
                    }
                }
                this.mHasPublicClients = hasPublicClients;
            }
        }
    }

    List<AudioPlaybackConfiguration> getActivePlaybackConfigurations(boolean isPrivileged) {
        synchronized (this.mPlayers) {
            if (isPrivileged) {
                try {
                    ArrayList arrayList = new ArrayList(this.mPlayers.values());
                    return arrayList;
                } catch (Throwable th) {
                }
            } else {
                List<AudioPlaybackConfiguration> configsPublic;
                synchronized (this.mPlayerLock) {
                    configsPublic = anonymizeForPublicConsumption(new ArrayList(this.mPlayers.values()));
                }
                return configsPublic;
            }
        }
    }

    void setMediaFocusControl(MediaFocusControl mfc) {
        this.mMfc = mfc;
    }
}
