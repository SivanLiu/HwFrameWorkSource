package com.android.server.audio;

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
import com.huawei.pgmng.log.LogPower;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public final class PlaybackActivityMonitor implements PlayerDeathMonitor, PlayerFocusEnforcer {
    private static final boolean DEBUG = false;
    private static final Configuration DUCK_ID = new Configuration(1);
    private static final Configuration DUCK_VSHAPE = new Builder().setId(1).setCurve(new float[]{0.0f, 1.0f}, new float[]{1.0f, 0.2f}).setOptionFlags(2).setDuration((long) MediaFocusControl.getFocusRampTimeMs(3, new AudioAttributes.Builder().setUsage(5).build())).build();
    private static final Operation PLAY_CREATE_IF_NEEDED = new Operation.Builder(Operation.PLAY).createIfNeeded().build();
    private static final Operation PLAY_SKIP_RAMP = new Operation.Builder(PLAY_CREATE_IF_NEEDED).setXOffset(1.0f).build();
    public static final String TAG = "AudioService.PlaybackActivityMonitor";
    private static final int VOLUME_SHAPER_SYSTEM_DUCK_ID = 1;
    private final ArrayList<PlayMonitorClient> mClients = new ArrayList();
    private final DuckingManager mDuckingManager = new DuckingManager();
    private boolean mHasPublicClients = false;
    private MediaFocusControl mMfc;
    private final ArrayList<Integer> mMutedPlayers = new ArrayList();
    private final Object mPlayerLock = new Object();
    private final HashMap<Integer, AudioPlaybackConfiguration> mPlayers = new HashMap();

    private static final class DuckingManager {
        private final HashMap<Integer, DuckedApp> mDuckers;

        private static final class DuckedApp {
            private final ArrayList<Integer> mDuckedPlayers = new ArrayList();
            private final int mUid;

            DuckedApp(int uid) {
                this.mUid = uid;
            }

            void dump(PrintWriter pw) {
                pw.print("\t uid:" + this.mUid + " piids:");
                for (Integer intValue : this.mDuckedPlayers) {
                    pw.print(" " + intValue.intValue());
                }
                pw.println("");
            }

            void addDuck(AudioPlaybackConfiguration apc, boolean skipRamp) {
                int piid = new Integer(apc.getPlayerInterfaceId()).intValue();
                if (!this.mDuckedPlayers.contains(Integer.valueOf(piid))) {
                    try {
                        Log.v(PlaybackActivityMonitor.TAG, "ducking (skipRamp=" + skipRamp + ") player piid:" + apc.getPlayerInterfaceId() + " uid:" + this.mUid);
                        apc.getPlayerProxy().applyVolumeShaper(PlaybackActivityMonitor.DUCK_VSHAPE, skipRamp ? PlaybackActivityMonitor.PLAY_SKIP_RAMP : PlaybackActivityMonitor.PLAY_CREATE_IF_NEEDED);
                        this.mDuckedPlayers.add(Integer.valueOf(piid));
                    } catch (Exception e) {
                        Log.e(PlaybackActivityMonitor.TAG, "Error ducking player piid:" + piid + " uid:" + this.mUid, e);
                    }
                }
            }

            void removeUnduckAll(HashMap<Integer, AudioPlaybackConfiguration> players) {
                for (Integer intValue : this.mDuckedPlayers) {
                    int piid = intValue.intValue();
                    AudioPlaybackConfiguration apc = (AudioPlaybackConfiguration) players.get(Integer.valueOf(piid));
                    if (apc != null) {
                        try {
                            Log.v(PlaybackActivityMonitor.TAG, "unducking player " + piid + " uid:" + this.mUid);
                            apc.getPlayerProxy().applyVolumeShaper(PlaybackActivityMonitor.DUCK_ID, Operation.REVERSE);
                        } catch (Exception e) {
                            Log.e(PlaybackActivityMonitor.TAG, "Error unducking player piid:" + piid + " uid:" + this.mUid, e);
                        }
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
            if (!this.mDuckers.containsKey(Integer.valueOf(uid))) {
                this.mDuckers.put(Integer.valueOf(uid), new DuckedApp(uid));
            }
            DuckedApp da = (DuckedApp) this.mDuckers.get(Integer.valueOf(uid));
            for (AudioPlaybackConfiguration apc : apcsToDuck) {
                da.addDuck(apc, false);
            }
        }

        synchronized void unduckUid(int uid, HashMap<Integer, AudioPlaybackConfiguration> players) {
            DuckedApp da = (DuckedApp) this.mDuckers.remove(Integer.valueOf(uid));
            if (da != null) {
                da.removeUnduckAll(players);
            }
        }

        synchronized void checkDuck(AudioPlaybackConfiguration apc) {
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
            DuckedApp da = (DuckedApp) this.mDuckers.get(Integer.valueOf(apc.getClientUid()));
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

    PlaybackActivityMonitor() {
        PlayMonitorClient.sListenerDeathMonitor = this;
        AudioPlaybackConfiguration.sPlayerDeathMonitor = this;
    }

    public int trackPlayer(PlayerIdCard pic) {
        int newPiid = AudioSystem.newAudioPlayerId();
        AudioPlaybackConfiguration apc = new AudioPlaybackConfiguration(pic, newPiid, Binder.getCallingUid(), Binder.getCallingPid());
        apc.init();
        synchronized (this.mPlayerLock) {
            this.mPlayers.put(Integer.valueOf(newPiid), apc);
        }
        return newPiid;
    }

    public void playerAttributes(int piid, AudioAttributes attr, int binderUid) {
        boolean handleAudioAttributesEvent;
        synchronized (this.mPlayerLock) {
            AudioPlaybackConfiguration apc = (AudioPlaybackConfiguration) this.mPlayers.get(new Integer(piid));
            if (checkConfigurationCaller(piid, apc, binderUid)) {
                handleAudioAttributesEvent = apc.handleAudioAttributesEvent(attr);
            } else {
                Log.e(TAG, "Error updating audio attributes");
                handleAudioAttributesEvent = false;
            }
        }
        if (handleAudioAttributesEvent) {
            dispatchPlaybackChange();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void playerEvent(int piid, int event, int binderUid) {
        synchronized (this.mPlayerLock) {
            AudioPlaybackConfiguration apc = (AudioPlaybackConfiguration) this.mPlayers.get(new Integer(piid));
            if (apc == null) {
            } else if (apc.getPlayerType() == 3) {
            } else {
                boolean handleStateEvent;
                if (checkConfigurationCaller(piid, apc, binderUid)) {
                    handleStateEvent = apc.handleStateEvent(event);
                } else {
                    Log.e(TAG, "Error handling event " + event);
                    handleStateEvent = false;
                }
                if (handleStateEvent && event == 2) {
                    this.mDuckingManager.checkDuck(apc);
                }
            }
        }
    }

    public void releasePlayer(int piid, int binderUid) {
        synchronized (this.mPlayerLock) {
            AudioPlaybackConfiguration apc = (AudioPlaybackConfiguration) this.mPlayers.get(new Integer(piid));
            if (checkConfigurationCaller(piid, apc, binderUid)) {
                this.mPlayers.remove(new Integer(piid));
                this.mDuckingManager.removeReleased(apc);
                LogPower.push(163, String.valueOf(Binder.getCallingPid()), String.valueOf(piid), String.valueOf(Binder.getCallingUid()));
            }
        }
    }

    public void playerDeath(int piid) {
        releasePlayer(piid, 0);
    }

    protected void dump(PrintWriter pw) {
        pw.println("\nPlaybackActivityMonitor dump time: " + DateFormat.getTimeInstance().format(new Date()));
        synchronized (this.mPlayerLock) {
            for (AudioPlaybackConfiguration conf : this.mPlayers.values()) {
                conf.dump(pw);
            }
            pw.println("\n  ducked players:");
            this.mDuckingManager.dump(pw);
            pw.println("\n  muted player piids:");
            for (Integer intValue : this.mMutedPlayers) {
                pw.println(" " + intValue.intValue());
            }
        }
    }

    private static boolean checkConfigurationCaller(int piid, AudioPlaybackConfiguration apc, int binderUid) {
        if (apc == null) {
            return false;
        }
        if (binderUid == 0 || apc.getClientUid() == binderUid) {
            return true;
        }
        Log.e(TAG, "Forbidden operation from uid " + binderUid + " for player " + piid);
        return false;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void dispatchPlaybackChange() {
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

    public boolean duckPlayers(FocusRequester winner, FocusRequester loser) {
        synchronized (this.mPlayerLock) {
            if (this.mPlayers.isEmpty()) {
                return true;
            }
            ArrayList<AudioPlaybackConfiguration> apcsToDuck = new ArrayList();
            for (AudioPlaybackConfiguration apc : this.mPlayers.values()) {
                if (!winner.hasSameUid(apc.getClientUid()) && loser.hasSameUid(apc.getClientUid()) && apc.getPlayerState() == 2) {
                    if (apc.getAudioAttributes().getContentType() == 1) {
                        Log.v(TAG, "not ducking player " + apc.getPlayerInterfaceId() + " uid:" + apc.getClientUid() + " pid:" + apc.getClientPid() + " - SPEECH");
                        return false;
                    } else if (apc.getPlayerType() == 3) {
                        Log.v(TAG, "not ducking player " + apc.getPlayerInterfaceId() + " uid:" + apc.getClientUid() + " pid:" + apc.getClientPid() + " - SoundPool");
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
        synchronized (this.mPlayerLock) {
            this.mDuckingManager.unduckUid(winner.getClientUid(), this.mPlayers);
        }
    }

    public void mutePlayersForCall(int[] usagesToMute) {
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
                    int i = 0;
                    if (!(this.mMfc == null || apc.getPkgName() == null)) {
                        i = this.mMfc.isPkgInExternalStack(apc.getPkgName());
                        Log.v(TAG, "isInExternalDisplay = " + i);
                    }
                    if (mute && (r2 ^ 1) != 0) {
                        try {
                            Log.v(TAG, "call: muting player" + piid + " uid:" + apc.getClientUid());
                            apc.getPlayerProxy().setVolume(0.0f);
                            this.mMutedPlayers.add(new Integer(piid.intValue()));
                        } catch (Exception e) {
                            Log.e(TAG, "call: error muting player " + piid, e);
                        }
                    }
                }
            }
        }
    }

    public void unmutePlayersForCall() {
        synchronized (this.mPlayerLock) {
            if (this.mMutedPlayers.isEmpty()) {
                return;
            }
            for (Integer intValue : this.mMutedPlayers) {
                int piid = intValue.intValue();
                AudioPlaybackConfiguration apc = (AudioPlaybackConfiguration) this.mPlayers.get(Integer.valueOf(piid));
                if (apc != null && (this.mMfc == null || apc.getPkgName() == null || !this.mMfc.isPkgInExternalStack(apc.getPkgName()))) {
                    try {
                        Log.v(TAG, "call: unmuting player" + piid + " uid:" + apc.getClientUid());
                        apc.getPlayerProxy().setVolume(1.0f);
                    } catch (Exception e) {
                        Log.e(TAG, "call: error unmuting player " + piid + " uid:" + apc.getClientUid(), e);
                    }
                }
            }
            this.mMutedPlayers.clear();
        }
    }

    void registerPlaybackCallback(IPlaybackConfigDispatcher pcdb, boolean isPrivileged) {
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
                List arrayList = new ArrayList(this.mPlayers.values());
                return arrayList;
            }
            List<AudioPlaybackConfiguration> configsPublic;
            synchronized (this.mPlayerLock) {
                configsPublic = anonymizeForPublicConsumption(new ArrayList(this.mPlayers.values()));
            }
            return configsPublic;
        }
    }

    void setMediaFocusControl(MediaFocusControl mfc) {
        this.mMfc = mfc;
    }
}
