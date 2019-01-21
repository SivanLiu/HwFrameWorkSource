package android.support.v4.media;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.BaseMediaPlayer.PlayerEventCallback;
import android.support.v4.media.MediaController2.PlaybackInfo;
import android.support.v4.media.MediaMetadata2.Builder;
import android.support.v4.media.MediaPlaylistAgent.PlaylistEventCallback;
import android.support.v4.media.MediaSession2.CommandButton;
import android.support.v4.media.MediaSession2.ControllerInfo;
import android.support.v4.media.MediaSession2.OnDataSourceMissingHelper;
import android.support.v4.media.MediaSession2.SessionCallback;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.util.ObjectsCompat;
import android.text.TextUtils;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;

@TargetApi(19)
class MediaSession2ImplBase implements SupportLibraryImpl {
    static final boolean DEBUG = Log.isLoggable(TAG, 3);
    static final String TAG = "MS2ImplBase";
    private final AudioFocusHandler mAudioFocusHandler;
    private final AudioManager mAudioManager;
    private final SessionCallback mCallback;
    private final Executor mCallbackExecutor;
    private final Context mContext;
    @GuardedBy("mLock")
    private OnDataSourceMissingHelper mDsmHelper;
    private final Handler mHandler;
    private final HandlerThread mHandlerThread;
    private final MediaSession2 mInstance;
    final Object mLock = new Object();
    @GuardedBy("mLock")
    private PlaybackInfo mPlaybackInfo;
    @GuardedBy("mLock")
    private BaseMediaPlayer mPlayer;
    private final PlayerEventCallback mPlayerEventCallback;
    @GuardedBy("mLock")
    private MediaPlaylistAgent mPlaylistAgent;
    private final PlaylistEventCallback mPlaylistEventCallback;
    private final MediaSession2Stub mSession2Stub;
    private final PendingIntent mSessionActivity;
    private final MediaSessionCompat mSessionCompat;
    private final MediaSessionLegacyStub mSessionLegacyStub;
    @GuardedBy("mLock")
    private SessionPlaylistAgentImplBase mSessionPlaylistAgent;
    private final SessionToken2 mSessionToken;
    @GuardedBy("mLock")
    private VolumeProviderCompat mVolumeProvider;

    @FunctionalInterface
    interface NotifyRunnable {
        void run(ControllerCb controllerCb) throws RemoteException;
    }

    private static class MyPlayerEventCallback extends PlayerEventCallback {
        private final WeakReference<MediaSession2ImplBase> mSession;

        /* synthetic */ MyPlayerEventCallback(MediaSession2ImplBase x0, AnonymousClass1 x1) {
            this(x0);
        }

        private MyPlayerEventCallback(MediaSession2ImplBase session) {
            this.mSession = new WeakReference(session);
        }

        public void onCurrentDataSourceChanged(final BaseMediaPlayer player, final DataSourceDesc dsd) {
            final MediaSession2ImplBase session = getSession();
            if (session != null) {
                session.getCallbackExecutor().execute(new Runnable() {
                    public void run() {
                        MediaItem2 item;
                        if (dsd == null) {
                            item = null;
                        } else {
                            item = MyPlayerEventCallback.this.getMediaItem(session, dsd);
                            if (item == null) {
                                String str = MediaSession2ImplBase.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Cannot obtain media item from the dsd=");
                                stringBuilder.append(dsd);
                                Log.w(str, stringBuilder.toString());
                                return;
                            }
                        }
                        session.getCallback().onCurrentMediaItemChanged(session.getInstance(), player, item);
                        session.notifyToAllControllers(new NotifyRunnable() {
                            public void run(ControllerCb callback) throws RemoteException {
                                callback.onCurrentMediaItemChanged(item);
                            }
                        });
                    }
                });
            }
        }

        public void onMediaPrepared(final BaseMediaPlayer mpb, final DataSourceDesc dsd) {
            final MediaSession2ImplBase session = getSession();
            if (session != null && dsd != null) {
                session.getCallbackExecutor().execute(new Runnable() {
                    public void run() {
                        MediaItem2 item = MyPlayerEventCallback.this.getMediaItem(session, dsd);
                        if (item != null) {
                            if (item.equals(session.getCurrentMediaItem())) {
                                long duration = session.getDuration();
                                if (duration >= 0) {
                                    MediaMetadata2 metadata = item.getMetadata();
                                    if (metadata == null) {
                                        metadata = new Builder().putLong("android.media.metadata.DURATION", duration).putString("android.media.metadata.MEDIA_ID", item.getMediaId()).build();
                                    } else if (metadata.containsKey("android.media.metadata.DURATION")) {
                                        long durationFromMetadata = metadata.getLong("android.media.metadata.DURATION");
                                        if (duration != durationFromMetadata) {
                                            String str = MediaSession2ImplBase.TAG;
                                            StringBuilder stringBuilder = new StringBuilder();
                                            stringBuilder.append("duration mismatch for an item. duration from player=");
                                            stringBuilder.append(duration);
                                            stringBuilder.append(" duration from metadata=");
                                            stringBuilder.append(durationFromMetadata);
                                            stringBuilder.append(". May be a timing issue?");
                                            Log.w(str, stringBuilder.toString());
                                        }
                                        metadata = null;
                                    } else {
                                        metadata = new Builder(metadata).putLong("android.media.metadata.DURATION", duration).build();
                                    }
                                    if (metadata != null) {
                                        item.setMetadata(metadata);
                                        session.notifyToAllControllers(new NotifyRunnable() {
                                            public void run(ControllerCb callback) throws RemoteException {
                                                callback.onPlaylistChanged(session.getPlaylist(), session.getPlaylistMetadata());
                                            }
                                        });
                                    }
                                } else {
                                    return;
                                }
                            }
                            session.getCallback().onMediaPrepared(session.getInstance(), mpb, item);
                        }
                    }
                });
            }
        }

        public void onPlayerStateChanged(final BaseMediaPlayer player, final int state) {
            final MediaSession2ImplBase session = getSession();
            if (session != null) {
                session.getCallbackExecutor().execute(new Runnable() {
                    public void run() {
                        session.mAudioFocusHandler.onPlayerStateChanged(state);
                        session.getCallback().onPlayerStateChanged(session.getInstance(), player, state);
                        session.notifyToAllControllers(new NotifyRunnable() {
                            public void run(ControllerCb callback) throws RemoteException {
                                callback.onPlayerStateChanged(SystemClock.elapsedRealtime(), player.getCurrentPosition(), state);
                            }
                        });
                    }
                });
            }
        }

        public void onBufferingStateChanged(BaseMediaPlayer mpb, DataSourceDesc dsd, int state) {
            MediaSession2ImplBase session = getSession();
            if (session != null && dsd != null) {
                final MediaSession2ImplBase mediaSession2ImplBase = session;
                final DataSourceDesc dataSourceDesc = dsd;
                final BaseMediaPlayer baseMediaPlayer = mpb;
                final int i = state;
                session.getCallbackExecutor().execute(new Runnable() {
                    public void run() {
                        final MediaItem2 item = MyPlayerEventCallback.this.getMediaItem(mediaSession2ImplBase, dataSourceDesc);
                        if (item != null) {
                            mediaSession2ImplBase.getCallback().onBufferingStateChanged(mediaSession2ImplBase.getInstance(), baseMediaPlayer, item, i);
                            mediaSession2ImplBase.notifyToAllControllers(new NotifyRunnable() {
                                public void run(ControllerCb callback) throws RemoteException {
                                    callback.onBufferingStateChanged(item, i, baseMediaPlayer.getBufferedPosition());
                                }
                            });
                        }
                    }
                });
            }
        }

        public void onPlaybackSpeedChanged(final BaseMediaPlayer mpb, final float speed) {
            final MediaSession2ImplBase session = getSession();
            if (session != null) {
                session.getCallbackExecutor().execute(new Runnable() {
                    public void run() {
                        session.getCallback().onPlaybackSpeedChanged(session.getInstance(), mpb, speed);
                        session.notifyToAllControllers(new NotifyRunnable() {
                            public void run(ControllerCb callback) throws RemoteException {
                                callback.onPlaybackSpeedChanged(SystemClock.elapsedRealtime(), session.getCurrentPosition(), speed);
                            }
                        });
                    }
                });
            }
        }

        public void onSeekCompleted(BaseMediaPlayer mpb, long position) {
            MediaSession2ImplBase session = getSession();
            if (session != null) {
                final MediaSession2ImplBase mediaSession2ImplBase = session;
                final BaseMediaPlayer baseMediaPlayer = mpb;
                final long j = position;
                session.getCallbackExecutor().execute(new Runnable() {
                    public void run() {
                        mediaSession2ImplBase.getCallback().onSeekCompleted(mediaSession2ImplBase.getInstance(), baseMediaPlayer, j);
                        mediaSession2ImplBase.notifyToAllControllers(new NotifyRunnable() {
                            public void run(ControllerCb callback) throws RemoteException {
                                callback.onSeekCompleted(SystemClock.elapsedRealtime(), mediaSession2ImplBase.getCurrentPosition(), j);
                            }
                        });
                    }
                });
            }
        }

        private MediaSession2ImplBase getSession() {
            MediaSession2ImplBase session = (MediaSession2ImplBase) this.mSession.get();
            if (session == null && MediaSession2ImplBase.DEBUG) {
                Log.d(MediaSession2ImplBase.TAG, "Session is closed", new IllegalStateException());
            }
            return session;
        }

        private MediaItem2 getMediaItem(MediaSession2ImplBase session, DataSourceDesc dsd) {
            MediaPlaylistAgent agent = session.getPlaylistAgent();
            if (agent == null) {
                if (MediaSession2ImplBase.DEBUG) {
                    Log.d(MediaSession2ImplBase.TAG, "Session is closed", new IllegalStateException());
                }
                return null;
            }
            MediaItem2 item = agent.getMediaItem(dsd);
            if (item == null && MediaSession2ImplBase.DEBUG) {
                String str = MediaSession2ImplBase.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Could not find matching item for dsd=");
                stringBuilder.append(dsd);
                Log.d(str, stringBuilder.toString(), new NoSuchElementException());
            }
            return item;
        }
    }

    private static class MyPlaylistEventCallback extends PlaylistEventCallback {
        private final WeakReference<MediaSession2ImplBase> mSession;

        /* synthetic */ MyPlaylistEventCallback(MediaSession2ImplBase x0, AnonymousClass1 x1) {
            this(x0);
        }

        private MyPlaylistEventCallback(MediaSession2ImplBase session) {
            this.mSession = new WeakReference(session);
        }

        public void onPlaylistChanged(MediaPlaylistAgent playlistAgent, List<MediaItem2> list, MediaMetadata2 metadata) {
            MediaSession2ImplBase session = (MediaSession2ImplBase) this.mSession.get();
            if (session != null) {
                session.notifyPlaylistChangedOnExecutor(playlistAgent, list, metadata);
            }
        }

        public void onPlaylistMetadataChanged(MediaPlaylistAgent playlistAgent, MediaMetadata2 metadata) {
            MediaSession2ImplBase session = (MediaSession2ImplBase) this.mSession.get();
            if (session != null) {
                session.notifyPlaylistMetadataChangedOnExecutor(playlistAgent, metadata);
            }
        }

        public void onRepeatModeChanged(MediaPlaylistAgent playlistAgent, int repeatMode) {
            MediaSession2ImplBase session = (MediaSession2ImplBase) this.mSession.get();
            if (session != null) {
                session.notifyRepeatModeChangedOnExecutor(playlistAgent, repeatMode);
            }
        }

        public void onShuffleModeChanged(MediaPlaylistAgent playlistAgent, int shuffleMode) {
            MediaSession2ImplBase session = (MediaSession2ImplBase) this.mSession.get();
            if (session != null) {
                session.notifyShuffleModeChangedOnExecutor(playlistAgent, shuffleMode);
            }
        }
    }

    MediaSession2ImplBase(MediaSession2 instance, Context context, String id, BaseMediaPlayer player, MediaPlaylistAgent playlistAgent, VolumeProviderCompat volumeProvider, PendingIntent sessionActivity, Executor callbackExecutor, SessionCallback callback) {
        Context context2 = context;
        String str = id;
        PendingIntent pendingIntent = sessionActivity;
        this.mContext = context2;
        this.mInstance = instance;
        this.mHandlerThread = new HandlerThread("MediaController2_Thread");
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper());
        this.mSession2Stub = new MediaSession2Stub(this);
        this.mSessionLegacyStub = new MediaSessionLegacyStub(this);
        this.mSessionActivity = pendingIntent;
        this.mCallback = callback;
        this.mCallbackExecutor = callbackExecutor;
        this.mAudioManager = (AudioManager) context2.getSystemService("audio");
        this.mPlayerEventCallback = new MyPlayerEventCallback(this, null);
        this.mPlaylistEventCallback = new MyPlaylistEventCallback(this, null);
        this.mAudioFocusHandler = new AudioFocusHandler(context2, getInstance());
        String libraryService = getServiceName(context2, MediaLibraryService2.SERVICE_INTERFACE, str);
        String sessionService = getServiceName(context2, MediaSessionService2.SERVICE_INTERFACE, str);
        if (sessionService == null || libraryService == null) {
            SessionToken2ImplBase sessionToken2ImplBase;
            if (libraryService != null) {
                SessionToken2ImplBase sessionToken2ImplBase2 = sessionToken2ImplBase;
                sessionToken2ImplBase = new SessionToken2ImplBase(Process.myUid(), 2, context.getPackageName(), libraryService, str, this.mSession2Stub);
                this.mSessionToken = new SessionToken2(sessionToken2ImplBase2);
            } else if (sessionService != null) {
                SessionToken2ImplBase sessionToken2ImplBase3 = sessionToken2ImplBase;
                sessionToken2ImplBase = new SessionToken2ImplBase(Process.myUid(), 1, context.getPackageName(), sessionService, str, this.mSession2Stub);
                this.mSessionToken = new SessionToken2(sessionToken2ImplBase3);
            } else {
                this.mSessionToken = new SessionToken2(new SessionToken2ImplBase(Process.myUid(), 0, context.getPackageName(), null, str, this.mSession2Stub));
            }
            this.mSessionCompat = new MediaSessionCompat(context2, str, this.mSessionToken);
            this.mSessionCompat.setCallback(this.mSessionLegacyStub, this.mHandler);
            this.mSessionCompat.setSessionActivity(pendingIntent);
            updatePlayer(player, playlistAgent, volumeProvider);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Ambiguous session type. Multiple session services define the same id=");
        stringBuilder.append(str);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    /* JADX WARNING: Missing block: B:29:0x004b, code skipped:
            if (r13 != null) goto L_0x005a;
     */
    /* JADX WARNING: Missing block: B:30:0x004d, code skipped:
            r10.mSessionCompat.setPlaybackToLocal(getLegacyStreamType(r11.getAudioAttributes()));
     */
    /* JADX WARNING: Missing block: B:31:0x005a, code skipped:
            if (r11 == r5) goto L_0x006a;
     */
    /* JADX WARNING: Missing block: B:32:0x005c, code skipped:
            r11.registerPlayerEventCallback(r10.mCallbackExecutor, r10.mPlayerEventCallback);
     */
    /* JADX WARNING: Missing block: B:33:0x0063, code skipped:
            if (r5 == null) goto L_0x006a;
     */
    /* JADX WARNING: Missing block: B:34:0x0065, code skipped:
            r5.unregisterPlayerEventCallback(r10.mPlayerEventCallback);
     */
    /* JADX WARNING: Missing block: B:35:0x006a, code skipped:
            if (r12 == r2) goto L_0x007a;
     */
    /* JADX WARNING: Missing block: B:36:0x006c, code skipped:
            r12.registerPlaylistEventCallback(r10.mCallbackExecutor, r10.mPlaylistEventCallback);
     */
    /* JADX WARNING: Missing block: B:37:0x0073, code skipped:
            if (r2 == null) goto L_0x007a;
     */
    /* JADX WARNING: Missing block: B:38:0x0075, code skipped:
            r2.unregisterPlaylistEventCallback(r10.mPlaylistEventCallback);
     */
    /* JADX WARNING: Missing block: B:39:0x007a, code skipped:
            if (r5 == null) goto L_?;
     */
    /* JADX WARNING: Missing block: B:40:0x007c, code skipped:
            if (r6 == false) goto L_0x0081;
     */
    /* JADX WARNING: Missing block: B:41:0x007e, code skipped:
            notifyAgentUpdatedNotLocked(r2);
     */
    /* JADX WARNING: Missing block: B:42:0x0081, code skipped:
            if (r4 == false) goto L_0x0086;
     */
    /* JADX WARNING: Missing block: B:43:0x0083, code skipped:
            notifyPlayerUpdatedNotLocked(r5);
     */
    /* JADX WARNING: Missing block: B:44:0x0086, code skipped:
            if (r3 == null) goto L_?;
     */
    /* JADX WARNING: Missing block: B:45:0x0088, code skipped:
            notifyToAllControllers(new android.support.v4.media.MediaSession2ImplBase.AnonymousClass1(r10));
     */
    /* JADX WARNING: Missing block: B:64:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:65:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:66:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updatePlayer(@NonNull BaseMediaPlayer player, @Nullable MediaPlaylistAgent playlistAgent, @Nullable VolumeProviderCompat volumeProvider) {
        Throwable th;
        boolean hasPlaybackInfoChanged;
        MediaPlaylistAgent mediaPlaylistAgent;
        if (player != null) {
            final PlaybackInfo info = createPlaybackInfo(volumeProvider, player.getAudioAttributes());
            synchronized (this.mLock) {
                MediaPlaylistAgent oldAgent = null;
                boolean hasAgentChanged;
                try {
                    boolean hasPlayerChanged = this.mPlayer != player ? true : DEBUG;
                    try {
                        hasAgentChanged = this.mPlaylistAgent != playlistAgent ? true : DEBUG;
                    } catch (Throwable th2) {
                        th = th2;
                        hasPlaybackInfoChanged = DEBUG;
                        hasAgentChanged = hasPlaybackInfoChanged;
                        oldAgent = null;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th3) {
                                th = th3;
                            }
                        }
                        throw th;
                    }
                    try {
                        if (this.mPlaybackInfo != info) {
                            oldAgent = 1;
                        }
                        BaseMediaPlayer oldPlayer = this.mPlayer;
                    } catch (Throwable th4) {
                        th = th4;
                        mediaPlaylistAgent = null;
                        oldAgent = null;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                    try {
                        MediaPlaylistAgent oldAgent2 = this.mPlaylistAgent;
                        this.mPlayer = player;
                        if (playlistAgent == null) {
                            this.mSessionPlaylistAgent = new SessionPlaylistAgentImplBase(this, this.mPlayer);
                            if (this.mDsmHelper != null) {
                                this.mSessionPlaylistAgent.setOnDataSourceMissingHelper(this.mDsmHelper);
                            }
                            playlistAgent = this.mSessionPlaylistAgent;
                        }
                        this.mPlaylistAgent = playlistAgent;
                        this.mVolumeProvider = volumeProvider;
                        this.mPlaybackInfo = info;
                    } catch (Throwable th5) {
                        th = th5;
                        MediaPlaylistAgent mediaPlaylistAgent2 = oldAgent;
                        oldAgent = null;
                        mediaPlaylistAgent = mediaPlaylistAgent2;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                } catch (Throwable th6) {
                    th = th6;
                    hasPlaybackInfoChanged = null;
                    hasAgentChanged = hasPlaybackInfoChanged;
                    oldAgent = null;
                    while (true) {
                        break;
                    }
                    throw th;
                }
            }
        }
        throw new IllegalArgumentException("player shouldn't be null");
    }

    private PlaybackInfo createPlaybackInfo(VolumeProviderCompat volumeProvider, AudioAttributesCompat attrs) {
        if (volumeProvider != null) {
            return PlaybackInfo.createPlaybackInfo(2, attrs, volumeProvider.getVolumeControl(), volumeProvider.getMaxVolume(), volumeProvider.getCurrentVolume());
        }
        int stream = getLegacyStreamType(attrs);
        int controlType = 2;
        if (VERSION.SDK_INT >= 21 && this.mAudioManager.isVolumeFixed()) {
            controlType = 0;
        }
        return PlaybackInfo.createPlaybackInfo(1, attrs, controlType, this.mAudioManager.getStreamMaxVolume(stream), this.mAudioManager.getStreamVolume(stream));
    }

    private int getLegacyStreamType(@Nullable AudioAttributesCompat attrs) {
        if (attrs == null) {
            return 3;
        }
        int stream = attrs.getLegacyStreamType();
        if (stream == Integer.MIN_VALUE) {
            return 3;
        }
        return stream;
    }

    /* JADX WARNING: Missing block: B:14:0x0044, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void close() {
        synchronized (this.mLock) {
            if (this.mPlayer == null) {
                return;
            }
            this.mAudioFocusHandler.close();
            this.mPlayer.unregisterPlayerEventCallback(this.mPlayerEventCallback);
            this.mPlayer = null;
            this.mSessionCompat.release();
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onDisconnected();
                }
            });
            this.mHandler.removeCallbacksAndMessages(null);
            if (this.mHandlerThread.isAlive()) {
                if (VERSION.SDK_INT >= 18) {
                    this.mHandlerThread.quitSafely();
                } else {
                    this.mHandlerThread.quit();
                }
            }
        }
    }

    @NonNull
    public BaseMediaPlayer getPlayer() {
        BaseMediaPlayer baseMediaPlayer;
        synchronized (this.mLock) {
            baseMediaPlayer = this.mPlayer;
        }
        return baseMediaPlayer;
    }

    @NonNull
    public MediaPlaylistAgent getPlaylistAgent() {
        MediaPlaylistAgent mediaPlaylistAgent;
        synchronized (this.mLock) {
            mediaPlaylistAgent = this.mPlaylistAgent;
        }
        return mediaPlaylistAgent;
    }

    @Nullable
    public VolumeProviderCompat getVolumeProvider() {
        VolumeProviderCompat volumeProviderCompat;
        synchronized (this.mLock) {
            volumeProviderCompat = this.mVolumeProvider;
        }
        return volumeProviderCompat;
    }

    @NonNull
    public SessionToken2 getToken() {
        return this.mSessionToken;
    }

    @NonNull
    public List<ControllerInfo> getConnectedControllers() {
        return this.mSession2Stub.getConnectedControllers();
    }

    public void setCustomLayout(@NonNull ControllerInfo controller, @NonNull final List<CommandButton> layout) {
        if (controller == null) {
            throw new IllegalArgumentException("controller shouldn't be null");
        } else if (layout != null) {
            notifyToController(controller, new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onCustomLayoutChanged(layout);
                }
            });
        } else {
            throw new IllegalArgumentException("layout shouldn't be null");
        }
    }

    public void setAllowedCommands(@NonNull ControllerInfo controller, @NonNull final SessionCommandGroup2 commands) {
        if (controller == null) {
            throw new IllegalArgumentException("controller shouldn't be null");
        } else if (commands != null) {
            this.mSession2Stub.setAllowedCommands(controller, commands);
            notifyToController(controller, new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onAllowedCommandsChanged(commands);
                }
            });
        } else {
            throw new IllegalArgumentException("commands shouldn't be null");
        }
    }

    public void sendCustomCommand(@NonNull final SessionCommand2 command, @Nullable final Bundle args) {
        if (command != null) {
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onCustomCommand(command, args, null);
                }
            });
            return;
        }
        throw new IllegalArgumentException("command shouldn't be null");
    }

    public void sendCustomCommand(@NonNull ControllerInfo controller, @NonNull final SessionCommand2 command, @Nullable final Bundle args, @Nullable final ResultReceiver receiver) {
        if (controller == null) {
            throw new IllegalArgumentException("controller shouldn't be null");
        } else if (command != null) {
            notifyToController(controller, new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onCustomCommand(command, args, receiver);
                }
            });
        } else {
            throw new IllegalArgumentException("command shouldn't be null");
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x001c;
     */
    /* JADX WARNING: Missing block: B:8:0x000e, code skipped:
            if (r4.mAudioFocusHandler.onPlayRequested() == false) goto L_0x0014;
     */
    /* JADX WARNING: Missing block: B:9:0x0010, code skipped:
            r1.play();
     */
    /* JADX WARNING: Missing block: B:10:0x0014, code skipped:
            android.util.Log.w(TAG, "play() wouldn't be called because of the failure in audio focus");
     */
    /* JADX WARNING: Missing block: B:12:0x001e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:13:0x0020, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:20:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:21:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:22:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:23:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void play() {
        Throwable th;
        synchronized (this.mLock) {
            try {
                BaseMediaPlayer player = this.mPlayer;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x001c;
     */
    /* JADX WARNING: Missing block: B:8:0x000e, code skipped:
            if (r4.mAudioFocusHandler.onPauseRequested() == false) goto L_0x0014;
     */
    /* JADX WARNING: Missing block: B:9:0x0010, code skipped:
            r1.pause();
     */
    /* JADX WARNING: Missing block: B:10:0x0014, code skipped:
            android.util.Log.w(TAG, "pause() wouldn't be called of the failure in audio focus");
     */
    /* JADX WARNING: Missing block: B:12:0x001e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:13:0x0020, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:20:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:21:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:22:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:23:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void pause() {
        Throwable th;
        synchronized (this.mLock) {
            try {
                BaseMediaPlayer player = this.mPlayer;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000c;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            r1.reset();
     */
    /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:17:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:18:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:19:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void reset() {
        Throwable th;
        synchronized (this.mLock) {
            try {
                BaseMediaPlayer player = this.mPlayer;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000c;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            r1.prepare();
     */
    /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:17:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:18:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:19:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void prepare() {
        Throwable th;
        synchronized (this.mLock) {
            try {
                BaseMediaPlayer player = this.mPlayer;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000c;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            r1.seekTo(r5);
     */
    /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:17:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:18:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:19:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void seekTo(long pos) {
        Throwable th;
        synchronized (this.mLock) {
            try {
                BaseMediaPlayer player = this.mPlayer;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    public void skipForward() {
    }

    public void skipBackward() {
    }

    public void notifyError(final int errorCode, @Nullable final Bundle extras) {
        notifyToAllControllers(new NotifyRunnable() {
            public void run(ControllerCb callback) throws RemoteException {
                callback.onError(errorCode, extras);
            }
        });
    }

    public void notifyRoutesInfoChanged(@NonNull ControllerInfo controller, @Nullable final List<Bundle> routes) {
        notifyToController(controller, new NotifyRunnable() {
            public void run(ControllerCb callback) throws RemoteException {
                callback.onRoutesInfoChanged(routes);
            }
        });
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000d;
     */
    /* JADX WARNING: Missing block: B:8:0x000c, code skipped:
            return r1.getPlayerState();
     */
    /* JADX WARNING: Missing block: B:10:0x000f, code skipped:
            if (DEBUG == false) goto L_0x001d;
     */
    /* JADX WARNING: Missing block: B:11:0x0011, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            return 3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getPlayerState() {
        Throwable th;
        synchronized (this.mLock) {
            try {
                BaseMediaPlayer player = this.mPlayer;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000d;
     */
    /* JADX WARNING: Missing block: B:8:0x000c, code skipped:
            return r1.getCurrentPosition();
     */
    /* JADX WARNING: Missing block: B:10:0x000f, code skipped:
            if (DEBUG == false) goto L_0x001d;
     */
    /* JADX WARNING: Missing block: B:11:0x0011, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001f, code skipped:
            return -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long getCurrentPosition() {
        Throwable th;
        synchronized (this.mLock) {
            try {
                BaseMediaPlayer player = this.mPlayer;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000d;
     */
    /* JADX WARNING: Missing block: B:8:0x000c, code skipped:
            return r1.getDuration();
     */
    /* JADX WARNING: Missing block: B:10:0x000f, code skipped:
            if (DEBUG == false) goto L_0x001d;
     */
    /* JADX WARNING: Missing block: B:11:0x0011, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001f, code skipped:
            return -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long getDuration() {
        Throwable th;
        synchronized (this.mLock) {
            try {
                BaseMediaPlayer player = this.mPlayer;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000d;
     */
    /* JADX WARNING: Missing block: B:8:0x000c, code skipped:
            return r1.getBufferedPosition();
     */
    /* JADX WARNING: Missing block: B:10:0x000f, code skipped:
            if (DEBUG == false) goto L_0x001d;
     */
    /* JADX WARNING: Missing block: B:11:0x0011, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001f, code skipped:
            return -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long getBufferedPosition() {
        Throwable th;
        synchronized (this.mLock) {
            try {
                BaseMediaPlayer player = this.mPlayer;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000d;
     */
    /* JADX WARNING: Missing block: B:8:0x000c, code skipped:
            return r1.getBufferingState();
     */
    /* JADX WARNING: Missing block: B:10:0x000f, code skipped:
            if (DEBUG == false) goto L_0x001d;
     */
    /* JADX WARNING: Missing block: B:11:0x0011, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getBufferingState() {
        Throwable th;
        synchronized (this.mLock) {
            try {
                BaseMediaPlayer player = this.mPlayer;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000d;
     */
    /* JADX WARNING: Missing block: B:8:0x000c, code skipped:
            return r1.getPlaybackSpeed();
     */
    /* JADX WARNING: Missing block: B:10:0x000f, code skipped:
            if (DEBUG == false) goto L_0x001d;
     */
    /* JADX WARNING: Missing block: B:11:0x0011, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001f, code skipped:
            return 1.0f;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public float getPlaybackSpeed() {
        Throwable th;
        synchronized (this.mLock) {
            try {
                BaseMediaPlayer player = this.mPlayer;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000c;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            r1.setPlaybackSpeed(r5);
     */
    /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:17:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:18:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:19:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setPlaybackSpeed(float speed) {
        Throwable th;
        synchronized (this.mLock) {
            try {
                BaseMediaPlayer player = this.mPlayer;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    public void setOnDataSourceMissingHelper(@NonNull OnDataSourceMissingHelper helper) {
        if (helper != null) {
            synchronized (this.mLock) {
                this.mDsmHelper = helper;
                if (this.mSessionPlaylistAgent != null) {
                    this.mSessionPlaylistAgent.setOnDataSourceMissingHelper(helper);
                }
            }
            return;
        }
        throw new IllegalArgumentException("helper shouldn't be null");
    }

    public void clearOnDataSourceMissingHelper() {
        synchronized (this.mLock) {
            this.mDsmHelper = null;
            if (this.mSessionPlaylistAgent != null) {
                this.mSessionPlaylistAgent.clearOnDataSourceMissingHelper();
            }
        }
    }

    /* JADX WARNING: Missing block: B:7:0x0007, code skipped:
            if (r2 == null) goto L_0x000e;
     */
    /* JADX WARNING: Missing block: B:9:0x000d, code skipped:
            return r2.getPlaylist();
     */
    /* JADX WARNING: Missing block: B:11:0x0010, code skipped:
            if (DEBUG == false) goto L_0x001e;
     */
    /* JADX WARNING: Missing block: B:12:0x0012, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<MediaItem2> getPlaylist() {
        Throwable th;
        synchronized (this.mLock) {
            try {
                MediaPlaylistAgent agent = this.mPlaylistAgent;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                Throwable th4 = th3;
                List<MediaItem2> list = null;
                th = th4;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            if (r1 == null) goto L_0x000e;
     */
    /* JADX WARNING: Missing block: B:8:0x000a, code skipped:
            r1.setPlaylist(r5, r6);
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:11:0x0012, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:20:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:21:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:22:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setPlaylist(@NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) {
        Throwable th;
        if (list != null) {
            synchronized (this.mLock) {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    try {
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            }
        }
        throw new IllegalArgumentException("list shouldn't be null");
    }

    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            if (r1 == null) goto L_0x000e;
     */
    /* JADX WARNING: Missing block: B:8:0x000a, code skipped:
            r1.skipToPlaylistItem(r5);
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:11:0x0012, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:20:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:21:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:22:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void skipToPlaylistItem(@NonNull MediaItem2 item) {
        Throwable th;
        if (item != null) {
            synchronized (this.mLock) {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    try {
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            }
        }
        throw new IllegalArgumentException("item shouldn't be null");
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000c;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            r1.skipToPreviousItem();
     */
    /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:17:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:18:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:19:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void skipToPreviousItem() {
        Throwable th;
        synchronized (this.mLock) {
            try {
                MediaPlaylistAgent agent = this.mPlaylistAgent;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000c;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            r1.skipToNextItem();
     */
    /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:17:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:18:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:19:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void skipToNextItem() {
        Throwable th;
        synchronized (this.mLock) {
            try {
                MediaPlaylistAgent agent = this.mPlaylistAgent;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:7:0x0007, code skipped:
            if (r2 == null) goto L_0x000e;
     */
    /* JADX WARNING: Missing block: B:9:0x000d, code skipped:
            return r2.getPlaylistMetadata();
     */
    /* JADX WARNING: Missing block: B:11:0x0010, code skipped:
            if (DEBUG == false) goto L_0x001e;
     */
    /* JADX WARNING: Missing block: B:12:0x0012, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public MediaMetadata2 getPlaylistMetadata() {
        Throwable th;
        synchronized (this.mLock) {
            try {
                MediaPlaylistAgent agent = this.mPlaylistAgent;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                Throwable th4 = th3;
                MediaMetadata2 mediaMetadata2 = null;
                th = th4;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:8:0x000a, code skipped:
            if (r1 == null) goto L_0x0010;
     */
    /* JADX WARNING: Missing block: B:9:0x000c, code skipped:
            r1.addPlaylistItem(r5, r6);
     */
    /* JADX WARNING: Missing block: B:11:0x0012, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:12:0x0014, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:23:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:24:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:25:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addPlaylistItem(int index, @NonNull MediaItem2 item) {
        Throwable th;
        if (index < 0) {
            throw new IllegalArgumentException("index shouldn't be negative");
        } else if (item != null) {
            synchronized (this.mLock) {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    try {
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            }
        } else {
            throw new IllegalArgumentException("item shouldn't be null");
        }
    }

    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            if (r1 == null) goto L_0x000e;
     */
    /* JADX WARNING: Missing block: B:8:0x000a, code skipped:
            r1.removePlaylistItem(r5);
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:11:0x0012, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:20:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:21:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:22:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void removePlaylistItem(@NonNull MediaItem2 item) {
        Throwable th;
        if (item != null) {
            synchronized (this.mLock) {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    try {
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            }
        }
        throw new IllegalArgumentException("item shouldn't be null");
    }

    /* JADX WARNING: Missing block: B:8:0x000a, code skipped:
            if (r1 == null) goto L_0x0010;
     */
    /* JADX WARNING: Missing block: B:9:0x000c, code skipped:
            r1.replacePlaylistItem(r5, r6);
     */
    /* JADX WARNING: Missing block: B:11:0x0012, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:12:0x0014, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:23:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:24:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:25:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void replacePlaylistItem(int index, @NonNull MediaItem2 item) {
        Throwable th;
        if (index < 0) {
            throw new IllegalArgumentException("index shouldn't be negative");
        } else if (item != null) {
            synchronized (this.mLock) {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    try {
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            }
        } else {
            throw new IllegalArgumentException("item shouldn't be null");
        }
    }

    /* JADX WARNING: Missing block: B:7:0x0007, code skipped:
            if (r2 == null) goto L_0x000e;
     */
    /* JADX WARNING: Missing block: B:9:0x000d, code skipped:
            return r2.getCurrentMediaItem();
     */
    /* JADX WARNING: Missing block: B:11:0x0010, code skipped:
            if (DEBUG == false) goto L_0x001e;
     */
    /* JADX WARNING: Missing block: B:12:0x0012, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public MediaItem2 getCurrentMediaItem() {
        Throwable th;
        synchronized (this.mLock) {
            try {
                MediaPlaylistAgent agent = this.mPlaylistAgent;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                Throwable th4 = th3;
                MediaItem2 mediaItem2 = null;
                th = th4;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000c;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            r1.updatePlaylistMetadata(r5);
     */
    /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:17:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:18:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:19:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updatePlaylistMetadata(@Nullable MediaMetadata2 metadata) {
        Throwable th;
        synchronized (this.mLock) {
            try {
                MediaPlaylistAgent agent = this.mPlaylistAgent;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000d;
     */
    /* JADX WARNING: Missing block: B:8:0x000c, code skipped:
            return r1.getRepeatMode();
     */
    /* JADX WARNING: Missing block: B:10:0x000f, code skipped:
            if (DEBUG == false) goto L_0x001d;
     */
    /* JADX WARNING: Missing block: B:11:0x0011, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getRepeatMode() {
        Throwable th;
        synchronized (this.mLock) {
            try {
                MediaPlaylistAgent agent = this.mPlaylistAgent;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000c;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            r1.setRepeatMode(r5);
     */
    /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:17:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:18:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:19:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setRepeatMode(int repeatMode) {
        Throwable th;
        synchronized (this.mLock) {
            try {
                MediaPlaylistAgent agent = this.mPlaylistAgent;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000d;
     */
    /* JADX WARNING: Missing block: B:8:0x000c, code skipped:
            return r1.getShuffleMode();
     */
    /* JADX WARNING: Missing block: B:10:0x000f, code skipped:
            if (DEBUG == false) goto L_0x001d;
     */
    /* JADX WARNING: Missing block: B:11:0x0011, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getShuffleMode() {
        Throwable th;
        synchronized (this.mLock) {
            try {
                MediaPlaylistAgent agent = this.mPlaylistAgent;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000c;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            r1.setShuffleMode(r5);
     */
    /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:17:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:18:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:19:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setShuffleMode(int shuffleMode) {
        Throwable th;
        synchronized (this.mLock) {
            try {
                MediaPlaylistAgent agent = this.mPlaylistAgent;
                try {
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    @NonNull
    public MediaSession2 getInstance() {
        return this.mInstance;
    }

    @NonNull
    public IBinder getSessionBinder() {
        return this.mSession2Stub.asBinder();
    }

    public Context getContext() {
        return this.mContext;
    }

    public Executor getCallbackExecutor() {
        return this.mCallbackExecutor;
    }

    public SessionCallback getCallback() {
        return this.mCallback;
    }

    public MediaSessionCompat getSessionCompat() {
        return this.mSessionCompat;
    }

    public AudioFocusHandler getAudioFocusHandler() {
        return this.mAudioFocusHandler;
    }

    public boolean isClosed() {
        return this.mHandlerThread.isAlive() ^ 1;
    }

    public PlaybackStateCompat getPlaybackStateCompat() {
        PlaybackStateCompat build;
        synchronized (this.mLock) {
            build = new PlaybackStateCompat.Builder().setState(MediaUtils2.convertToPlaybackStateCompatState(getPlayerState(), getBufferingState()), getCurrentPosition(), getPlaybackSpeed()).setActions(3670015).setBufferedPosition(getBufferedPosition()).build();
        }
        return build;
    }

    public PlaybackInfo getPlaybackInfo() {
        PlaybackInfo playbackInfo;
        synchronized (this.mLock) {
            playbackInfo = this.mPlaybackInfo;
        }
        return playbackInfo;
    }

    public PendingIntent getSessionActivity() {
        return this.mSessionActivity;
    }

    private static String getServiceName(Context context, String serviceAction, String id) {
        PackageManager manager = context.getPackageManager();
        Intent serviceIntent = new Intent(serviceAction);
        serviceIntent.setPackage(context.getPackageName());
        List<ResolveInfo> services = manager.queryIntentServices(serviceIntent, 128);
        String serviceName = null;
        if (services != null) {
            int i = 0;
            while (i < services.size()) {
                String serviceId = SessionToken2.getSessionId((ResolveInfo) services.get(i));
                if (!(serviceId == null || !TextUtils.equals(id, serviceId) || ((ResolveInfo) services.get(i)).serviceInfo == null)) {
                    if (serviceName == null) {
                        serviceName = ((ResolveInfo) services.get(i)).serviceInfo.name;
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Ambiguous session type. Multiple session services define the same id=");
                        stringBuilder.append(id);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                }
                i++;
            }
        }
        return serviceName;
    }

    private void notifyAgentUpdatedNotLocked(MediaPlaylistAgent oldAgent) {
        List<MediaItem2> oldPlaylist = oldAgent.getPlaylist();
        final List<MediaItem2> newPlaylist = getPlaylist();
        if (ObjectsCompat.equals(oldPlaylist, newPlaylist)) {
            MediaMetadata2 oldMetadata = oldAgent.getPlaylistMetadata();
            final MediaMetadata2 newMetadata = getPlaylistMetadata();
            if (!ObjectsCompat.equals(oldMetadata, newMetadata)) {
                notifyToAllControllers(new NotifyRunnable() {
                    public void run(ControllerCb callback) throws RemoteException {
                        callback.onPlaylistMetadataChanged(newMetadata);
                    }
                });
            }
        } else {
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaylistChanged(newPlaylist, MediaSession2ImplBase.this.getPlaylistMetadata());
                }
            });
        }
        MediaItem2 oldCurrentItem = oldAgent.getCurrentMediaItem();
        final MediaItem2 newCurrentItem = getCurrentMediaItem();
        if (!ObjectsCompat.equals(oldCurrentItem, newCurrentItem)) {
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onCurrentMediaItemChanged(newCurrentItem);
                }
            });
        }
        final int repeatMode = getRepeatMode();
        if (oldAgent.getRepeatMode() != repeatMode) {
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onRepeatModeChanged(repeatMode);
                }
            });
        }
        final int shuffleMode = getShuffleMode();
        if (oldAgent.getShuffleMode() != shuffleMode) {
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onShuffleModeChanged(shuffleMode);
                }
            });
        }
    }

    private void notifyPlayerUpdatedNotLocked(BaseMediaPlayer oldPlayer) {
        long currentTimeMs = SystemClock.elapsedRealtime();
        long positionMs = getCurrentPosition();
        final long j = currentTimeMs;
        final long j2 = positionMs;
        final int playerState = getPlayerState();
        notifyToAllControllers(new NotifyRunnable() {
            public void run(ControllerCb callback) throws RemoteException {
                callback.onPlayerStateChanged(j, j2, playerState);
            }
        });
        MediaItem2 item = getCurrentMediaItem();
        if (item != null) {
            playerState = getBufferingState();
            final MediaItem2 mediaItem2 = item;
            final int i = playerState;
            AnonymousClass15 anonymousClass15 = r0;
            j2 = getBufferedPosition();
            AnonymousClass15 anonymousClass152 = new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onBufferingStateChanged(mediaItem2, i, j2);
                }
            };
            notifyToAllControllers(anonymousClass15);
        }
        float speed = getPlaybackSpeed();
        if (speed != oldPlayer.getPlaybackSpeed()) {
            j = currentTimeMs;
            j2 = positionMs;
            final float f = speed;
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaybackSpeedChanged(j, j2, f);
                }
            });
        }
    }

    private void notifyPlaylistChangedOnExecutor(MediaPlaylistAgent playlistAgent, final List<MediaItem2> list, final MediaMetadata2 metadata) {
        synchronized (this.mLock) {
            if (playlistAgent != this.mPlaylistAgent) {
                return;
            }
            this.mCallback.onPlaylistChanged(this.mInstance, playlistAgent, list, metadata);
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaylistChanged(list, metadata);
                }
            });
        }
    }

    private void notifyPlaylistMetadataChangedOnExecutor(MediaPlaylistAgent playlistAgent, final MediaMetadata2 metadata) {
        synchronized (this.mLock) {
            if (playlistAgent != this.mPlaylistAgent) {
                return;
            }
            this.mCallback.onPlaylistMetadataChanged(this.mInstance, playlistAgent, metadata);
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaylistMetadataChanged(metadata);
                }
            });
        }
    }

    private void notifyRepeatModeChangedOnExecutor(MediaPlaylistAgent playlistAgent, final int repeatMode) {
        synchronized (this.mLock) {
            if (playlistAgent != this.mPlaylistAgent) {
                return;
            }
            this.mCallback.onRepeatModeChanged(this.mInstance, playlistAgent, repeatMode);
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onRepeatModeChanged(repeatMode);
                }
            });
        }
    }

    private void notifyShuffleModeChangedOnExecutor(MediaPlaylistAgent playlistAgent, final int shuffleMode) {
        synchronized (this.mLock) {
            if (playlistAgent != this.mPlaylistAgent) {
                return;
            }
            this.mCallback.onShuffleModeChanged(this.mInstance, playlistAgent, shuffleMode);
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onShuffleModeChanged(shuffleMode);
                }
            });
        }
    }

    void notifyToController(@NonNull final ControllerInfo controller, @NonNull NotifyRunnable runnable) {
        String str;
        StringBuilder stringBuilder;
        if (controller != null) {
            try {
                runnable.run(controller.getControllerCb());
            } catch (DeadObjectException e) {
                if (DEBUG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(controller.toString());
                    stringBuilder.append(" is gone");
                    Log.d(str, stringBuilder.toString(), e);
                }
                this.mSession2Stub.removeControllerInfo(controller);
                this.mCallbackExecutor.execute(new Runnable() {
                    public void run() {
                        MediaSession2ImplBase.this.mCallback.onDisconnected(MediaSession2ImplBase.this.getInstance(), controller);
                    }
                });
            } catch (RemoteException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception in ");
                stringBuilder.append(controller.toString());
                Log.w(str, stringBuilder.toString(), e2);
            }
        }
    }

    void notifyToAllControllers(@NonNull NotifyRunnable runnable) {
        List<ControllerInfo> controllers = getConnectedControllers();
        for (int i = 0; i < controllers.size(); i++) {
            notifyToController((ControllerInfo) controllers.get(i), runnable);
        }
    }
}
