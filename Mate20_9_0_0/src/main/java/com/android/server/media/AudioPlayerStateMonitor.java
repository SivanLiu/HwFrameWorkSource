package com.android.server.media;

import android.content.Context;
import android.media.AudioPlaybackConfiguration;
import android.media.IAudioService;
import android.media.IPlaybackConfigDispatcher.Stub;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

class AudioPlayerStateMonitor extends Stub {
    private static boolean DEBUG = MediaSessionService.DEBUG;
    private static String TAG = "AudioPlayerStateMonitor";
    private static AudioPlayerStateMonitor sInstance = new AudioPlayerStateMonitor();
    @GuardedBy("mLock")
    private final Set<Integer> mActiveAudioUids = new ArraySet();
    @GuardedBy("mLock")
    private final Map<OnAudioPlayerActiveStateChangedListener, MessageHandler> mListenerMap = new ArrayMap();
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private ArrayMap<Integer, AudioPlaybackConfiguration> mPrevActiveAudioPlaybackConfigs = new ArrayMap();
    @GuardedBy("mLock")
    private boolean mRegisteredToAudioService;
    @GuardedBy("mLock")
    private final IntArray mSortedAudioPlaybackClientUids = new IntArray();

    private static final class MessageHandler extends Handler {
        private static final int MSG_AUDIO_PLAYER_ACTIVE_STATE_CHANGED = 1;
        private final OnAudioPlayerActiveStateChangedListener mListener;

        MessageHandler(Looper looper, OnAudioPlayerActiveStateChangedListener listener) {
            super(looper);
            this.mListener = listener;
        }

        public void handleMessage(Message msg) {
            boolean z = true;
            if (msg.what == 1) {
                OnAudioPlayerActiveStateChangedListener onAudioPlayerActiveStateChangedListener = this.mListener;
                AudioPlaybackConfiguration audioPlaybackConfiguration = (AudioPlaybackConfiguration) msg.obj;
                if (msg.arg1 == 0) {
                    z = false;
                }
                onAudioPlayerActiveStateChangedListener.onAudioPlayerActiveStateChanged(audioPlaybackConfiguration, z);
            }
        }

        void sendAudioPlayerActiveStateChangedMessage(AudioPlaybackConfiguration config, boolean isRemoved) {
            obtainMessage(1, isRemoved, 0, config).sendToTarget();
        }
    }

    interface OnAudioPlayerActiveStateChangedListener {
        void onAudioPlayerActiveStateChanged(AudioPlaybackConfiguration audioPlaybackConfiguration, boolean z);
    }

    static AudioPlayerStateMonitor getInstance() {
        return sInstance;
    }

    private AudioPlayerStateMonitor() {
    }

    public void dispatchPlaybackConfigChange(List<AudioPlaybackConfiguration> configs, boolean flush) {
        if (flush) {
            Binder.flushPendingCommands();
        }
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                AudioPlaybackConfiguration config;
                this.mActiveAudioUids.clear();
                ArrayMap<Integer, AudioPlaybackConfiguration> activeAudioPlaybackConfigs = new ArrayMap();
                for (AudioPlaybackConfiguration config2 : configs) {
                    if (config2.isActive()) {
                        this.mActiveAudioUids.add(Integer.valueOf(config2.getClientUid()));
                        activeAudioPlaybackConfigs.put(Integer.valueOf(config2.getPlayerInterfaceId()), config2);
                    }
                }
                for (int i = 0; i < activeAudioPlaybackConfigs.size(); i++) {
                    config = (AudioPlaybackConfiguration) activeAudioPlaybackConfigs.valueAt(i);
                    int uid = config.getClientUid();
                    if (!this.mPrevActiveAudioPlaybackConfigs.containsKey(Integer.valueOf(config.getPlayerInterfaceId()))) {
                        if (DEBUG) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Found a new active media playback. ");
                            stringBuilder.append(AudioPlaybackConfiguration.toLogFriendlyString(config));
                            Log.d(str, stringBuilder.toString());
                        }
                        int index = this.mSortedAudioPlaybackClientUids.indexOf(uid);
                        if (index != 0) {
                            if (index > 0) {
                                this.mSortedAudioPlaybackClientUids.remove(index);
                            }
                            this.mSortedAudioPlaybackClientUids.add(0, uid);
                        }
                    }
                }
                Iterator it = configs.iterator();
                while (true) {
                    boolean wasActive = true;
                    if (!it.hasNext()) {
                        break;
                    }
                    config = (AudioPlaybackConfiguration) it.next();
                    if (this.mPrevActiveAudioPlaybackConfigs.remove(Integer.valueOf(config.getPlayerInterfaceId())) == null) {
                        wasActive = false;
                    }
                    if (wasActive != config.isActive()) {
                        sendAudioPlayerActiveStateChangedMessageLocked(config, false);
                    }
                }
                for (AudioPlaybackConfiguration config22 : this.mPrevActiveAudioPlaybackConfigs.values()) {
                    sendAudioPlayerActiveStateChangedMessageLocked(config22, true);
                }
                this.mPrevActiveAudioPlaybackConfigs = activeAudioPlaybackConfigs;
            }
            Binder.restoreCallingIdentity(token);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void registerListener(OnAudioPlayerActiveStateChangedListener listener, Handler handler) {
        synchronized (this.mLock) {
            this.mListenerMap.put(listener, new MessageHandler(handler == null ? Looper.myLooper() : handler.getLooper(), listener));
        }
    }

    public void unregisterListener(OnAudioPlayerActiveStateChangedListener listener) {
        synchronized (this.mLock) {
            this.mListenerMap.remove(listener);
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
            contains = this.mActiveAudioUids.contains(Integer.valueOf(uid));
        }
        return contains;
    }

    public void cleanUpAudioPlaybackUids(int mediaButtonSessionUid) {
        synchronized (this.mLock) {
            int userId = UserHandle.getUserId(mediaButtonSessionUid);
            for (int i = this.mSortedAudioPlaybackClientUids.size() - 1; i >= 0; i--) {
                if (this.mSortedAudioPlaybackClientUids.get(i) == mediaButtonSessionUid) {
                    break;
                }
                int uid = this.mSortedAudioPlaybackClientUids.get(i);
                if (userId == UserHandle.getUserId(uid) && !isPlaybackActive(uid)) {
                    this.mSortedAudioPlaybackClientUids.remove(i);
                }
            }
        }
    }

    public void dump(Context context, PrintWriter pw, String prefix) {
        synchronized (this.mLock) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("Audio playback (lastly played comes first)");
            pw.println(stringBuilder.toString());
            String indent = new StringBuilder();
            indent.append(prefix);
            indent.append("  ");
            indent = indent.toString();
            for (int i = 0; i < this.mSortedAudioPlaybackClientUids.size(); i++) {
                int uid = this.mSortedAudioPlaybackClientUids.get(i);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(indent);
                stringBuilder2.append("uid=");
                stringBuilder2.append(uid);
                stringBuilder2.append(" packages=");
                pw.print(stringBuilder2.toString());
                String[] packages = context.getPackageManager().getPackagesForUid(uid);
                if (packages != null && packages.length > 0) {
                    for (String append : packages) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(append);
                        stringBuilder3.append(" ");
                        pw.print(stringBuilder3.toString());
                    }
                }
                pw.println();
            }
        }
    }

    public void registerSelfIntoAudioServiceIfNeeded(IAudioService audioService) {
        synchronized (this.mLock) {
            try {
                if (!this.mRegisteredToAudioService) {
                    audioService.registerPlaybackCallback(this);
                    this.mRegisteredToAudioService = true;
                }
            } catch (RemoteException e) {
                Log.wtf(TAG, "Failed to register playback callback", e);
                this.mRegisteredToAudioService = false;
            }
        }
    }

    @GuardedBy("mLock")
    private void sendAudioPlayerActiveStateChangedMessageLocked(AudioPlaybackConfiguration config, boolean isRemoved) {
        for (MessageHandler messageHandler : this.mListenerMap.values()) {
            messageHandler.sendAudioPlayerActiveStateChangedMessage(config, isRemoved);
        }
    }
}
