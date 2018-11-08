package com.android.server.media;

import android.content.Context;
import android.media.AudioPlaybackConfiguration;
import android.media.IAudioService;
import android.media.IPlaybackConfigDispatcher.Stub;
import android.os.Binder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.IntArray;
import android.util.Log;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class AudioPlaybackMonitor extends Stub {
    private static boolean DEBUG = MediaSessionService.DEBUG;
    private static String TAG = "AudioPlaybackMonitor";
    private static AudioPlaybackMonitor sInstance;
    private final Set<Integer> mActiveAudioPlaybackClientUids = new HashSet();
    private final List<OnAudioPlaybackStartedListener> mAudioPlaybackStartedListeners = new ArrayList();
    private final Map<Integer, Integer> mAudioPlaybackStates = new HashMap();
    private final List<OnAudioPlayerActiveStateChangedListener> mAudioPlayerActiveStateChangedListeners = new ArrayList();
    private final Context mContext;
    private final Object mLock = new Object();
    private final IntArray mSortedAudioPlaybackClientUids = new IntArray();

    interface OnAudioPlaybackStartedListener {
        void onAudioPlaybackStarted(int i);
    }

    interface OnAudioPlayerActiveStateChangedListener {
        void onAudioPlayerActiveStateChanged(int i, boolean z);
    }

    static AudioPlaybackMonitor getInstance(Context context, IAudioService audioService) {
        if (sInstance == null) {
            sInstance = new AudioPlaybackMonitor(context, audioService);
        }
        return sInstance;
    }

    private AudioPlaybackMonitor(Context context, IAudioService audioService) {
        this.mContext = context;
        try {
            audioService.registerPlaybackCallback(this);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Failed to register playback callback", e);
        }
    }

    public void dispatchPlaybackConfigChange(List<AudioPlaybackConfiguration> configs, boolean flush) {
        if (flush) {
            Binder.flushPendingCommands();
        }
        long token = Binder.clearCallingIdentity();
        try {
            List<Integer> newActiveAudioPlaybackClientUids = new ArrayList();
            synchronized (this.mLock) {
                this.mActiveAudioPlaybackClientUids.clear();
                for (AudioPlaybackConfiguration config : configs) {
                    if (config.isActive() && config.getPlayerType() != 3) {
                        this.mActiveAudioPlaybackClientUids.add(Integer.valueOf(config.getClientUid()));
                        if (!isActiveState((Integer) this.mAudioPlaybackStates.get(Integer.valueOf(config.getPlayerInterfaceId())))) {
                            if (DEBUG) {
                                Log.d(TAG, "Found a new active media playback. " + AudioPlaybackConfiguration.toLogFriendlyString(config));
                            }
                            newActiveAudioPlaybackClientUids.add(Integer.valueOf(config.getClientUid()));
                            int index = this.mSortedAudioPlaybackClientUids.indexOf(config.getClientUid());
                            if (index != 0) {
                                if (index > 0) {
                                    this.mSortedAudioPlaybackClientUids.remove(index);
                                }
                                this.mSortedAudioPlaybackClientUids.add(0, config.getClientUid());
                            }
                        }
                    }
                }
                List<OnAudioPlayerActiveStateChangedListener> audioPlayerActiveStateChangedListeners = new ArrayList(this.mAudioPlayerActiveStateChangedListeners);
                List<OnAudioPlaybackStartedListener> audioPlaybackStartedListeners = new ArrayList(this.mAudioPlaybackStartedListeners);
            }
            for (AudioPlaybackConfiguration config2 : configs) {
                boolean wasActive = isActiveState((Integer) this.mAudioPlaybackStates.get(Integer.valueOf(config2.getPlayerInterfaceId())));
                boolean isActive = config2.isActive();
                if (wasActive != isActive) {
                    for (OnAudioPlayerActiveStateChangedListener listener : audioPlayerActiveStateChangedListeners) {
                        listener.onAudioPlayerActiveStateChanged(config2.getClientUid(), isActive);
                    }
                }
            }
            for (Integer intValue : newActiveAudioPlaybackClientUids) {
                int uid = intValue.intValue();
                for (OnAudioPlaybackStartedListener listener2 : audioPlaybackStartedListeners) {
                    listener2.onAudioPlaybackStarted(uid);
                }
            }
            this.mAudioPlaybackStates.clear();
            for (AudioPlaybackConfiguration config22 : configs) {
                this.mAudioPlaybackStates.put(Integer.valueOf(config22.getPlayerInterfaceId()), Integer.valueOf(config22.getPlayerState()));
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void registerOnAudioPlaybackStartedListener(OnAudioPlaybackStartedListener listener) {
        synchronized (this.mLock) {
            this.mAudioPlaybackStartedListeners.add(listener);
        }
    }

    public void unregisterOnAudioPlaybackStartedListener(OnAudioPlaybackStartedListener listener) {
        synchronized (this.mLock) {
            this.mAudioPlaybackStartedListeners.remove(listener);
        }
    }

    public void registerOnAudioPlayerActiveStateChangedListener(OnAudioPlayerActiveStateChangedListener listener) {
        synchronized (this.mLock) {
            this.mAudioPlayerActiveStateChangedListeners.add(listener);
        }
    }

    public void unregisterOnAudioPlayerActiveStateChangedListener(OnAudioPlayerActiveStateChangedListener listener) {
        synchronized (this.mLock) {
            this.mAudioPlayerActiveStateChangedListeners.remove(listener);
        }
    }

    public IntArray getSortedAudioPlaybackClientUids() {
        IntArray sortedAudioPlaybackClientUids = new IntArray();
        synchronized (this.mLock) {
            sortedAudioPlaybackClientUids.addAll(this.mSortedAudioPlaybackClientUids);
        }
        return sortedAudioPlaybackClientUids;
    }

    public boolean isPlaybackActive(int uid) {
        boolean contains;
        synchronized (this.mLock) {
            contains = this.mActiveAudioPlaybackClientUids.contains(Integer.valueOf(uid));
        }
        return contains;
    }

    public void cleanUpAudioPlaybackUids(int mediaButtonSessionUid) {
        synchronized (this.mLock) {
            int userId = UserHandle.getUserId(mediaButtonSessionUid);
            int i = this.mSortedAudioPlaybackClientUids.size() - 1;
            while (i >= 0 && this.mSortedAudioPlaybackClientUids.get(i) != mediaButtonSessionUid) {
                int uid = this.mSortedAudioPlaybackClientUids.get(i);
                if (userId == UserHandle.getUserId(uid) && (isPlaybackActive(uid) ^ 1) != 0) {
                    this.mSortedAudioPlaybackClientUids.remove(i);
                }
                i--;
            }
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        synchronized (this.mLock) {
            pw.println(prefix + "Audio playback (lastly played comes first)");
            String indent = prefix + "  ";
            for (int i = 0; i < this.mSortedAudioPlaybackClientUids.size(); i++) {
                int uid = this.mSortedAudioPlaybackClientUids.get(i);
                pw.print(indent + "uid=" + uid + " packages=");
                String[] packages = this.mContext.getPackageManager().getPackagesForUid(uid);
                if (packages != null && packages.length > 0) {
                    for (String str : packages) {
                        pw.print(str + " ");
                    }
                }
                pw.println();
            }
        }
    }

    private boolean isActiveState(Integer state) {
        return state != null ? state.equals(Integer.valueOf(2)) : false;
    }
}
