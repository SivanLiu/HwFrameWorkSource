package android.support.v4.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.support.annotation.GuardedBy;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.ObjectsCompat;
import android.util.Log;

@VisibleForTesting(otherwise = 3)
@RestrictTo({Scope.LIBRARY})
public class AudioFocusHandler {
    private static final boolean DEBUG = false;
    private static final String TAG = "AudioFocusHandler";
    private final AudioFocusHandlerImpl mImpl;

    interface AudioFocusHandlerImpl {
        void close();

        boolean onPauseRequested();

        boolean onPlayRequested();

        void onPlayerStateChanged(int i);

        void sendIntent(Intent intent);
    }

    private static class AudioFocusHandlerImplBase implements AudioFocusHandlerImpl {
        private static final float VOLUME_DUCK_FACTOR = 0.2f;
        @GuardedBy("mLock")
        private AudioAttributesCompat mAudioAttributes;
        private final OnAudioFocusChangeListener mAudioFocusListener = new AudioFocusListener();
        private final AudioManager mAudioManager;
        private final BroadcastReceiver mBecomingNoisyIntentReceiver = new NoisyIntentReceiver();
        @GuardedBy("mLock")
        private boolean mHasAudioFocus;
        @GuardedBy("mLock")
        private boolean mHasRegisteredReceiver;
        private final IntentFilter mIntentFilter = new IntentFilter("android.media.AUDIO_BECOMING_NOISY");
        private final Object mLock = new Object();
        @GuardedBy("mLock")
        private boolean mResumeWhenAudioFocusGain;
        private final MediaSession2 mSession;

        private class AudioFocusListener implements OnAudioFocusChangeListener {
            private float mPlayerDuckingVolume;
            private float mPlayerVolumeBeforeDucking;

            private AudioFocusListener() {
            }

            /* JADX WARNING: Missing block: B:93:?, code skipped:
            return;
     */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void onAudioFocusChange(int focusGain) {
                BaseMediaPlayer player;
                float duckingVolume;
                if (focusGain != 1) {
                    switch (focusGain) {
                        case -3:
                            synchronized (AudioFocusHandlerImplBase.this.mLock) {
                                if (AudioFocusHandlerImplBase.this.mAudioAttributes != null) {
                                    if (AudioFocusHandlerImplBase.this.mAudioAttributes.getContentType() != 1) {
                                        player = AudioFocusHandlerImplBase.this.mSession.getPlayer();
                                        if (player != null) {
                                            float currentVolume = player.getPlayerVolume();
                                            duckingVolume = AudioFocusHandlerImplBase.VOLUME_DUCK_FACTOR * currentVolume;
                                            synchronized (AudioFocusHandlerImplBase.this.mLock) {
                                                this.mPlayerVolumeBeforeDucking = currentVolume;
                                                this.mPlayerDuckingVolume = duckingVolume;
                                            }
                                            player.setPlayerVolume(duckingVolume);
                                            break;
                                        }
                                    }
                                    AudioFocusHandlerImplBase.this.mSession.pause();
                                    break;
                                }
                                return;
                            }
                            break;
                        case -2:
                            AudioFocusHandlerImplBase.this.mSession.pause();
                            synchronized (AudioFocusHandlerImplBase.this.mLock) {
                                AudioFocusHandlerImplBase.this.mResumeWhenAudioFocusGain = true;
                            }
                            return;
                        case -1:
                            AudioFocusHandlerImplBase.this.mSession.pause();
                            synchronized (AudioFocusHandlerImplBase.this.mLock) {
                                AudioFocusHandlerImplBase.this.mResumeWhenAudioFocusGain = AudioFocusHandler.DEBUG;
                            }
                            return;
                        default:
                            return;
                    }
                } else if (AudioFocusHandlerImplBase.this.mSession.getPlayerState() == 1) {
                    synchronized (AudioFocusHandlerImplBase.this.mLock) {
                        if (AudioFocusHandlerImplBase.this.mResumeWhenAudioFocusGain) {
                            AudioFocusHandlerImplBase.this.mSession.play();
                            return;
                        }
                    }
                } else {
                    player = AudioFocusHandlerImplBase.this.mSession.getPlayer();
                    if (player != null) {
                        float currentVolume2 = player.getPlayerVolume();
                        synchronized (AudioFocusHandlerImplBase.this.mLock) {
                            if (currentVolume2 != this.mPlayerDuckingVolume) {
                                return;
                            }
                            duckingVolume = this.mPlayerVolumeBeforeDucking;
                            player.setPlayerVolume(duckingVolume);
                        }
                    }
                }
            }
        }

        private class NoisyIntentReceiver extends BroadcastReceiver {
            private NoisyIntentReceiver() {
            }

            /* JADX WARNING: Missing block: B:9:0x001c, code skipped:
            if ("android.media.AUDIO_BECOMING_NOISY".equals(r6.getAction()) == false) goto L_0x0069;
     */
            /* JADX WARNING: Missing block: B:10:0x001e, code skipped:
            r0 = android.support.v4.media.AudioFocusHandler.AudioFocusHandlerImplBase.access$200(r4.this$0);
     */
            /* JADX WARNING: Missing block: B:11:0x0024, code skipped:
            monitor-enter(r0);
     */
            /* JADX WARNING: Missing block: B:15:0x002c, code skipped:
            if (android.support.v4.media.AudioFocusHandler.AudioFocusHandlerImplBase.access$400(r4.this$0) != null) goto L_0x0030;
     */
            /* JADX WARNING: Missing block: B:16:0x002e, code skipped:
            monitor-exit(r0);
     */
            /* JADX WARNING: Missing block: B:17:0x002f, code skipped:
            return;
     */
            /* JADX WARNING: Missing block: B:18:0x0030, code skipped:
            r1 = android.support.v4.media.AudioFocusHandler.AudioFocusHandlerImplBase.access$400(r4.this$0).getUsage();
     */
            /* JADX WARNING: Missing block: B:19:0x003b, code skipped:
            monitor-exit(r0);
     */
            /* JADX WARNING: Missing block: B:21:0x003d, code skipped:
            if (r1 == 1) goto L_0x005c;
     */
            /* JADX WARNING: Missing block: B:23:0x0041, code skipped:
            if (r1 == 14) goto L_0x0044;
     */
            /* JADX WARNING: Missing block: B:24:0x0044, code skipped:
            r0 = android.support.v4.media.AudioFocusHandler.AudioFocusHandlerImplBase.access$500(r4.this$0).getPlayer();
     */
            /* JADX WARNING: Missing block: B:25:0x004e, code skipped:
            if (r0 == null) goto L_0x0069;
     */
            /* JADX WARNING: Missing block: B:26:0x0050, code skipped:
            r0.setPlayerVolume(r0.getPlayerVolume() * android.support.v4.media.AudioFocusHandler.AudioFocusHandlerImplBase.VOLUME_DUCK_FACTOR);
     */
            /* JADX WARNING: Missing block: B:27:0x005c, code skipped:
            android.support.v4.media.AudioFocusHandler.AudioFocusHandlerImplBase.access$500(r4.this$0).pause();
     */
            /* JADX WARNING: Missing block: B:32:0x0069, code skipped:
            return;
     */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void onReceive(Context context, Intent intent) {
                synchronized (AudioFocusHandlerImplBase.this.mLock) {
                    if (!AudioFocusHandlerImplBase.this.mHasRegisteredReceiver) {
                    }
                }
            }
        }

        AudioFocusHandlerImplBase(Context context, MediaSession2 session) {
            this.mSession = session;
            this.mAudioManager = (AudioManager) context.getSystemService("audio");
        }

        /* JADX WARNING: Missing block: B:21:0x003e, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void updateAudioAttributesIfNeeded() {
            AudioAttributesCompat attributes;
            if (this.mSession.getVolumeProvider() != null) {
                attributes = null;
            } else {
                BaseMediaPlayer player = this.mSession.getPlayer();
                attributes = player == null ? null : player.getAudioAttributes();
            }
            synchronized (this.mLock) {
                if (ObjectsCompat.equals(attributes, this.mAudioAttributes)) {
                    return;
                }
                this.mAudioAttributes = attributes;
                if (this.mHasAudioFocus) {
                    this.mHasAudioFocus = requestAudioFocusLocked();
                    if (!this.mHasAudioFocus) {
                        Log.w(AudioFocusHandler.TAG, "Failed to regain audio focus.");
                    }
                }
            }
        }

        public boolean onPlayRequested() {
            updateAudioAttributesIfNeeded();
            synchronized (this.mLock) {
                if (requestAudioFocusLocked()) {
                    return true;
                }
                return AudioFocusHandler.DEBUG;
            }
        }

        public boolean onPauseRequested() {
            synchronized (this.mLock) {
                this.mResumeWhenAudioFocusGain = AudioFocusHandler.DEBUG;
            }
            return true;
        }

        public void onPlayerStateChanged(int playerState) {
            synchronized (this.mLock) {
                switch (playerState) {
                    case 0:
                        abandonAudioFocusLocked();
                        break;
                    case 1:
                        updateAudioAttributesIfNeeded();
                        unregisterReceiverLocked();
                        break;
                    case 2:
                        updateAudioAttributesIfNeeded();
                        registerReceiverLocked();
                        break;
                    case 3:
                        abandonAudioFocusLocked();
                        unregisterReceiverLocked();
                        break;
                }
            }
        }

        public void close() {
            synchronized (this.mLock) {
                unregisterReceiverLocked();
                abandonAudioFocusLocked();
            }
        }

        public void sendIntent(Intent intent) {
            this.mBecomingNoisyIntentReceiver.onReceive(this.mSession.getContext(), intent);
        }

        @GuardedBy("mLock")
        private boolean requestAudioFocusLocked() {
            int focusGain = convertAudioAttributesToFocusGainLocked();
            if (focusGain == 0) {
                return true;
            }
            int audioFocusRequestResult = this.mAudioManager.requestAudioFocus(this.mAudioFocusListener, this.mAudioAttributes.getVolumeControlStream(), focusGain);
            if (audioFocusRequestResult == 1) {
                this.mHasAudioFocus = true;
            } else {
                String str = AudioFocusHandler.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("requestAudioFocus(");
                stringBuilder.append(focusGain);
                stringBuilder.append(") failed (return=");
                stringBuilder.append(audioFocusRequestResult);
                stringBuilder.append(") playback wouldn't start.");
                Log.w(str, stringBuilder.toString());
                this.mHasAudioFocus = AudioFocusHandler.DEBUG;
            }
            this.mResumeWhenAudioFocusGain = AudioFocusHandler.DEBUG;
            return this.mHasAudioFocus;
        }

        @GuardedBy("mLock")
        private void abandonAudioFocusLocked() {
            if (this.mHasAudioFocus) {
                this.mAudioManager.abandonAudioFocus(this.mAudioFocusListener);
                this.mHasAudioFocus = AudioFocusHandler.DEBUG;
                this.mResumeWhenAudioFocusGain = AudioFocusHandler.DEBUG;
            }
        }

        @GuardedBy("mLock")
        private void registerReceiverLocked() {
            if (!this.mHasRegisteredReceiver) {
                this.mSession.getContext().registerReceiver(this.mBecomingNoisyIntentReceiver, this.mIntentFilter);
                this.mHasRegisteredReceiver = true;
            }
        }

        @GuardedBy("mLock")
        private void unregisterReceiverLocked() {
            if (this.mHasRegisteredReceiver) {
                this.mSession.getContext().unregisterReceiver(this.mBecomingNoisyIntentReceiver);
                this.mHasRegisteredReceiver = AudioFocusHandler.DEBUG;
            }
        }

        @GuardedBy("mLock")
        private int convertAudioAttributesToFocusGainLocked() {
            AudioAttributesCompat audioAttributesCompat = this.mAudioAttributes;
            if (audioAttributesCompat == null) {
                return 0;
            }
            switch (audioAttributesCompat.getUsage()) {
                case 0:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 16:
                    return 3;
                case 1:
                case 14:
                    return 1;
                case 2:
                case 3:
                case 4:
                    return 2;
                default:
                    return 0;
            }
        }
    }

    AudioFocusHandler(Context context, MediaSession2 session) {
        this.mImpl = new AudioFocusHandlerImplBase(context, session);
    }

    public boolean onPlayRequested() {
        return this.mImpl.onPlayRequested();
    }

    public boolean onPauseRequested() {
        return this.mImpl.onPauseRequested();
    }

    public void onPlayerStateChanged(int playerState) {
        this.mImpl.onPlayerStateChanged(playerState);
    }

    public void close() {
        this.mImpl.close();
    }

    public void sendIntent(Intent intent) {
        this.mImpl.sendIntent(intent);
    }
}
