package android.support.v4.media;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.mediacompat.Rating2;
import android.support.v4.app.BundleCompat;
import android.support.v4.media.MediaController2.ControllerCallback;
import android.support.v4.media.MediaController2.PlaybackInfo;
import android.support.v4.media.MediaSession2.CommandButton;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaControllerCompat.Callback;
import android.support.v4.media.session.MediaSessionCompat.Token;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import java.util.List;
import java.util.concurrent.Executor;

@TargetApi(16)
class MediaController2ImplLegacy implements SupportLibraryImpl {
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String TAG = "MC2ImplLegacy";
    static final Bundle sDefaultRootExtras = new Bundle();
    @GuardedBy("mLock")
    private SessionCommandGroup2 mAllowedCommands;
    @GuardedBy("mLock")
    private MediaBrowserCompat mBrowserCompat;
    @GuardedBy("mLock")
    private int mBufferingState;
    private final ControllerCallback mCallback;
    private final Executor mCallbackExecutor;
    @GuardedBy("mLock")
    private volatile boolean mConnected;
    private final Context mContext;
    @GuardedBy("mLock")
    private MediaControllerCompat mControllerCompat;
    @GuardedBy("mLock")
    private ControllerCompatCallback mControllerCompatCallback;
    @GuardedBy("mLock")
    private MediaItem2 mCurrentMediaItem;
    private final Handler mHandler;
    private final HandlerThread mHandlerThread;
    private MediaController2 mInstance;
    @GuardedBy("mLock")
    private boolean mIsReleased;
    final Object mLock = new Object();
    @GuardedBy("mLock")
    private MediaMetadataCompat mMediaMetadataCompat;
    @GuardedBy("mLock")
    private PlaybackInfo mPlaybackInfo;
    @GuardedBy("mLock")
    private PlaybackStateCompat mPlaybackStateCompat;
    @GuardedBy("mLock")
    private int mPlayerState;
    @GuardedBy("mLock")
    private List<MediaItem2> mPlaylist;
    @GuardedBy("mLock")
    private MediaMetadata2 mPlaylistMetadata;
    @GuardedBy("mLock")
    private int mRepeatMode;
    @GuardedBy("mLock")
    private int mShuffleMode;
    private final SessionToken2 mToken;

    private class ConnectionCallback extends android.support.v4.media.MediaBrowserCompat.ConnectionCallback {
        private ConnectionCallback() {
        }

        /* synthetic */ ConnectionCallback(MediaController2ImplLegacy x0, AnonymousClass1 x1) {
            this();
        }

        public void onConnected() {
            MediaBrowserCompat browser = MediaController2ImplLegacy.this.getBrowserCompat();
            if (browser != null) {
                MediaController2ImplLegacy.this.connectToSession(browser.getSessionToken());
            } else if (MediaController2ImplLegacy.DEBUG) {
                Log.d(MediaController2ImplLegacy.TAG, "Controller is closed prematually", new IllegalStateException());
            }
        }

        public void onConnectionSuspended() {
            MediaController2ImplLegacy.this.close();
        }

        public void onConnectionFailed() {
            MediaController2ImplLegacy.this.close();
        }
    }

    private final class ControllerCompatCallback extends Callback {
        private ControllerCompatCallback() {
        }

        /* synthetic */ ControllerCompatCallback(MediaController2ImplLegacy x0, AnonymousClass1 x1) {
            this();
        }

        public void onSessionReady() {
            MediaController2ImplLegacy.this.sendCommand("android.support.v4.media.controller.command.CONNECT", new ResultReceiver(MediaController2ImplLegacy.this.mHandler) {
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    if (MediaController2ImplLegacy.this.mHandlerThread.isAlive()) {
                        switch (resultCode) {
                            case -1:
                                MediaController2ImplLegacy.this.mCallbackExecutor.execute(new Runnable() {
                                    public void run() {
                                        MediaController2ImplLegacy.this.mCallback.onDisconnected(MediaController2ImplLegacy.this.mInstance);
                                    }
                                });
                                MediaController2ImplLegacy.this.close();
                                break;
                            case 0:
                                MediaController2ImplLegacy.this.onConnectedNotLocked(resultData);
                                break;
                        }
                    }
                }
            });
        }

        public void onSessionDestroyed() {
            MediaController2ImplLegacy.this.close();
        }

        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            synchronized (MediaController2ImplLegacy.this.mLock) {
                MediaController2ImplLegacy.this.mPlaybackStateCompat = state;
            }
        }

        public void onMetadataChanged(MediaMetadataCompat metadata) {
            synchronized (MediaController2ImplLegacy.this.mLock) {
                MediaController2ImplLegacy.this.mMediaMetadataCompat = metadata;
            }
        }

        public void onSessionEvent(String event, Bundle extras) {
            if (extras != null) {
                extras.setClassLoader(MediaSession2.class.getClassLoader());
            }
            Object obj = -1;
            switch (event.hashCode()) {
                case -2076894204:
                    if (event.equals("android.support.v4.media.session.event.ON_BUFFERING_STATE_CHANGED")) {
                        obj = 13;
                        break;
                    }
                    break;
                case -2060536131:
                    if (event.equals("android.support.v4.media.session.event.ON_PLAYBACK_SPEED_CHANGED")) {
                        obj = 12;
                        break;
                    }
                    break;
                case -1588811870:
                    if (event.equals("android.support.v4.media.session.event.ON_PLAYBACK_INFO_CHANGED")) {
                        obj = 11;
                        break;
                    }
                    break;
                case -1471144819:
                    if (event.equals("android.support.v4.media.session.event.ON_PLAYER_STATE_CHANGED")) {
                        obj = 1;
                        break;
                    }
                    break;
                case -1021916189:
                    if (event.equals("android.support.v4.media.session.event.ON_ERROR")) {
                        obj = 3;
                        break;
                    }
                    break;
                case -617184370:
                    if (event.equals("android.support.v4.media.session.event.ON_CURRENT_MEDIA_ITEM_CHANGED")) {
                        obj = 2;
                        break;
                    }
                    break;
                case -92092013:
                    if (event.equals("android.support.v4.media.session.event.ON_ROUTES_INFO_CHANGED")) {
                        obj = 4;
                        break;
                    }
                    break;
                case -53555497:
                    if (event.equals("android.support.v4.media.session.event.ON_REPEAT_MODE_CHANGED")) {
                        obj = 7;
                        break;
                    }
                    break;
                case 229988025:
                    if (event.equals("android.support.v4.media.session.event.SEND_CUSTOM_COMMAND")) {
                        obj = 9;
                        break;
                    }
                    break;
                case 306321100:
                    if (event.equals("android.support.v4.media.session.event.ON_PLAYLIST_METADATA_CHANGED")) {
                        obj = 6;
                        break;
                    }
                    break;
                case 408969344:
                    if (event.equals("android.support.v4.media.session.event.SET_CUSTOM_LAYOUT")) {
                        obj = 10;
                        break;
                    }
                    break;
                case 806201420:
                    if (event.equals("android.support.v4.media.session.event.ON_PLAYLIST_CHANGED")) {
                        obj = 5;
                        break;
                    }
                    break;
                case 896576579:
                    if (event.equals("android.support.v4.media.session.event.ON_SHUFFLE_MODE_CHANGED")) {
                        obj = 8;
                        break;
                    }
                    break;
                case 1696119769:
                    if (event.equals("android.support.v4.media.session.event.ON_ALLOWED_COMMANDS_CHANGED")) {
                        obj = null;
                        break;
                    }
                    break;
                case 1871849865:
                    if (event.equals("android.support.v4.media.session.event.ON_SEEK_COMPLETED")) {
                        obj = 14;
                        break;
                    }
                    break;
            }
            final int playerState;
            final MediaItem2 item;
            final MediaMetadata2 playlistMetadata;
            PlaybackStateCompat state;
            switch (obj) {
                case null:
                    final SessionCommandGroup2 allowedCommands = SessionCommandGroup2.fromBundle(extras.getBundle("android.support.v4.media.argument.ALLOWED_COMMANDS"));
                    synchronized (MediaController2ImplLegacy.this.mLock) {
                        MediaController2ImplLegacy.this.mAllowedCommands = allowedCommands;
                    }
                    MediaController2ImplLegacy.this.mCallbackExecutor.execute(new Runnable() {
                        public void run() {
                            MediaController2ImplLegacy.this.mCallback.onAllowedCommandsChanged(MediaController2ImplLegacy.this.mInstance, allowedCommands);
                        }
                    });
                    break;
                case 1:
                    playerState = extras.getInt("android.support.v4.media.argument.PLAYER_STATE");
                    PlaybackStateCompat state2 = (PlaybackStateCompat) extras.getParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT");
                    if (state2 != null) {
                        synchronized (MediaController2ImplLegacy.this.mLock) {
                            MediaController2ImplLegacy.this.mPlayerState = playerState;
                            MediaController2ImplLegacy.this.mPlaybackStateCompat = state2;
                        }
                        MediaController2ImplLegacy.this.mCallbackExecutor.execute(new Runnable() {
                            public void run() {
                                MediaController2ImplLegacy.this.mCallback.onPlayerStateChanged(MediaController2ImplLegacy.this.mInstance, playerState);
                            }
                        });
                        break;
                    }
                    return;
                case 2:
                    item = MediaItem2.fromBundle(extras.getBundle("android.support.v4.media.argument.MEDIA_ITEM"));
                    synchronized (MediaController2ImplLegacy.this.mLock) {
                        MediaController2ImplLegacy.this.mCurrentMediaItem = item;
                    }
                    MediaController2ImplLegacy.this.mCallbackExecutor.execute(new Runnable() {
                        public void run() {
                            MediaController2ImplLegacy.this.mCallback.onCurrentMediaItemChanged(MediaController2ImplLegacy.this.mInstance, item);
                        }
                    });
                    break;
                case 3:
                    playerState = extras.getInt("android.support.v4.media.argument.ERROR_CODE");
                    final Bundle errorExtras = extras.getBundle("android.support.v4.media.argument.EXTRAS");
                    MediaController2ImplLegacy.this.mCallbackExecutor.execute(new Runnable() {
                        public void run() {
                            MediaController2ImplLegacy.this.mCallback.onError(MediaController2ImplLegacy.this.mInstance, playerState, errorExtras);
                        }
                    });
                    break;
                case 4:
                    final List<Bundle> routes = MediaUtils2.convertToBundleList(extras.getParcelableArray("android.support.v4.media.argument.ROUTE_BUNDLE"));
                    MediaController2ImplLegacy.this.mCallbackExecutor.execute(new Runnable() {
                        public void run() {
                            MediaController2ImplLegacy.this.mCallback.onRoutesInfoChanged(MediaController2ImplLegacy.this.mInstance, routes);
                        }
                    });
                    break;
                case 5:
                    playlistMetadata = MediaMetadata2.fromBundle(extras.getBundle("android.support.v4.media.argument.PLAYLIST_METADATA"));
                    final List<MediaItem2> playlist = MediaUtils2.convertToMediaItem2List(extras.getParcelableArray("android.support.v4.media.argument.PLAYLIST"));
                    synchronized (MediaController2ImplLegacy.this.mLock) {
                        MediaController2ImplLegacy.this.mPlaylist = playlist;
                        MediaController2ImplLegacy.this.mPlaylistMetadata = playlistMetadata;
                    }
                    MediaController2ImplLegacy.this.mCallbackExecutor.execute(new Runnable() {
                        public void run() {
                            MediaController2ImplLegacy.this.mCallback.onPlaylistChanged(MediaController2ImplLegacy.this.mInstance, playlist, playlistMetadata);
                        }
                    });
                    break;
                case 6:
                    playlistMetadata = MediaMetadata2.fromBundle(extras.getBundle("android.support.v4.media.argument.PLAYLIST_METADATA"));
                    synchronized (MediaController2ImplLegacy.this.mLock) {
                        MediaController2ImplLegacy.this.mPlaylistMetadata = playlistMetadata;
                    }
                    MediaController2ImplLegacy.this.mCallbackExecutor.execute(new Runnable() {
                        public void run() {
                            MediaController2ImplLegacy.this.mCallback.onPlaylistMetadataChanged(MediaController2ImplLegacy.this.mInstance, playlistMetadata);
                        }
                    });
                    break;
                case 7:
                    playerState = extras.getInt("android.support.v4.media.argument.REPEAT_MODE");
                    synchronized (MediaController2ImplLegacy.this.mLock) {
                        MediaController2ImplLegacy.this.mRepeatMode = playerState;
                    }
                    MediaController2ImplLegacy.this.mCallbackExecutor.execute(new Runnable() {
                        public void run() {
                            MediaController2ImplLegacy.this.mCallback.onRepeatModeChanged(MediaController2ImplLegacy.this.mInstance, playerState);
                        }
                    });
                    break;
                case 8:
                    playerState = extras.getInt("android.support.v4.media.argument.SHUFFLE_MODE");
                    synchronized (MediaController2ImplLegacy.this.mLock) {
                        MediaController2ImplLegacy.this.mShuffleMode = playerState;
                    }
                    MediaController2ImplLegacy.this.mCallbackExecutor.execute(new Runnable() {
                        public void run() {
                            MediaController2ImplLegacy.this.mCallback.onShuffleModeChanged(MediaController2ImplLegacy.this.mInstance, playerState);
                        }
                    });
                    break;
                case 9:
                    Bundle commandBundle = extras.getBundle("android.support.v4.media.argument.CUSTOM_COMMAND");
                    if (commandBundle != null) {
                        final SessionCommand2 command = SessionCommand2.fromBundle(commandBundle);
                        final Bundle args = extras.getBundle("android.support.v4.media.argument.ARGUMENTS");
                        final ResultReceiver receiver = (ResultReceiver) extras.getParcelable("android.support.v4.media.argument.RESULT_RECEIVER");
                        MediaController2ImplLegacy.this.mCallbackExecutor.execute(new Runnable() {
                            public void run() {
                                MediaController2ImplLegacy.this.mCallback.onCustomCommand(MediaController2ImplLegacy.this.mInstance, command, args, receiver);
                            }
                        });
                        break;
                    }
                    return;
                case 10:
                    final List<CommandButton> layout = MediaUtils2.convertToCommandButtonList(extras.getParcelableArray("android.support.v4.media.argument.COMMAND_BUTTONS"));
                    if (layout != null) {
                        MediaController2ImplLegacy.this.mCallbackExecutor.execute(new Runnable() {
                            public void run() {
                                MediaController2ImplLegacy.this.mCallback.onCustomLayoutChanged(MediaController2ImplLegacy.this.mInstance, layout);
                            }
                        });
                        break;
                    }
                    return;
                case 11:
                    final PlaybackInfo info = PlaybackInfo.fromBundle(extras.getBundle("android.support.v4.media.argument.PLAYBACK_INFO"));
                    if (info != null) {
                        synchronized (MediaController2ImplLegacy.this.mLock) {
                            MediaController2ImplLegacy.this.mPlaybackInfo = info;
                        }
                        MediaController2ImplLegacy.this.mCallbackExecutor.execute(new Runnable() {
                            public void run() {
                                MediaController2ImplLegacy.this.mCallback.onPlaybackInfoChanged(MediaController2ImplLegacy.this.mInstance, info);
                            }
                        });
                        break;
                    }
                    return;
                case 12:
                    final PlaybackStateCompat state3 = (PlaybackStateCompat) extras.getParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT");
                    if (state3 != null) {
                        synchronized (MediaController2ImplLegacy.this.mLock) {
                            MediaController2ImplLegacy.this.mPlaybackStateCompat = state3;
                        }
                        MediaController2ImplLegacy.this.mCallbackExecutor.execute(new Runnable() {
                            public void run() {
                                MediaController2ImplLegacy.this.mCallback.onPlaybackSpeedChanged(MediaController2ImplLegacy.this.mInstance, state3.getPlaybackSpeed());
                            }
                        });
                        break;
                    }
                    return;
                case 13:
                    item = MediaItem2.fromBundle(extras.getBundle("android.support.v4.media.argument.MEDIA_ITEM"));
                    final int bufferingState = extras.getInt("android.support.v4.media.argument.BUFFERING_STATE");
                    state = (PlaybackStateCompat) extras.getParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT");
                    if (item != null && state != null) {
                        synchronized (MediaController2ImplLegacy.this.mLock) {
                            MediaController2ImplLegacy.this.mBufferingState = bufferingState;
                            MediaController2ImplLegacy.this.mPlaybackStateCompat = state;
                        }
                        MediaController2ImplLegacy.this.mCallbackExecutor.execute(new Runnable() {
                            public void run() {
                                MediaController2ImplLegacy.this.mCallback.onBufferingStateChanged(MediaController2ImplLegacy.this.mInstance, item, bufferingState);
                            }
                        });
                        break;
                    }
                    return;
                    break;
                case 14:
                    final long position = extras.getLong("android.support.v4.media.argument.SEEK_POSITION");
                    state = (PlaybackStateCompat) extras.getParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT");
                    if (state != null) {
                        synchronized (MediaController2ImplLegacy.this.mLock) {
                            MediaController2ImplLegacy.this.mPlaybackStateCompat = state;
                        }
                        MediaController2ImplLegacy.this.mCallbackExecutor.execute(new Runnable() {
                            public void run() {
                                MediaController2ImplLegacy.this.mCallback.onSeekCompleted(MediaController2ImplLegacy.this.mInstance, position);
                            }
                        });
                        break;
                    }
                    return;
            }
        }
    }

    static {
        sDefaultRootExtras.putBoolean("android.support.v4.media.root_default_root", true);
    }

    MediaController2ImplLegacy(@NonNull Context context, @NonNull MediaController2 instance, @NonNull SessionToken2 token, @NonNull Executor executor, @NonNull ControllerCallback callback) {
        this.mContext = context;
        this.mInstance = instance;
        this.mHandlerThread = new HandlerThread("MediaController2_Thread");
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper());
        this.mToken = token;
        this.mCallback = callback;
        this.mCallbackExecutor = executor;
        if (this.mToken.getType() == 0) {
            synchronized (this.mLock) {
                this.mBrowserCompat = null;
            }
            connectToSession((Token) this.mToken.getBinder());
            return;
        }
        connectToService();
    }

    public void close() {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("release from ");
            stringBuilder.append(this.mToken);
            Log.d(str, stringBuilder.toString());
        }
        synchronized (this.mLock) {
            if (this.mIsReleased) {
                return;
            }
            this.mHandler.removeCallbacksAndMessages(null);
            if (VERSION.SDK_INT >= 18) {
                this.mHandlerThread.quitSafely();
            } else {
                this.mHandlerThread.quit();
            }
            this.mIsReleased = true;
            sendCommand("android.support.v4.media.controller.command.DISCONNECT");
            if (this.mControllerCompat != null) {
                this.mControllerCompat.unregisterCallback(this.mControllerCompatCallback);
            }
            if (this.mBrowserCompat != null) {
                this.mBrowserCompat.disconnect();
                this.mBrowserCompat = null;
            }
            if (this.mControllerCompat != null) {
                this.mControllerCompat.unregisterCallback(this.mControllerCompatCallback);
                this.mControllerCompat = null;
            }
            this.mConnected = DEBUG;
            this.mCallbackExecutor.execute(new Runnable() {
                public void run() {
                    MediaController2ImplLegacy.this.mCallback.onDisconnected(MediaController2ImplLegacy.this.mInstance);
                }
            });
        }
    }

    @NonNull
    public SessionToken2 getSessionToken() {
        return this.mToken;
    }

    public boolean isConnected() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mConnected;
        }
        return z;
    }

    public void play() {
        synchronized (this.mLock) {
            if (this.mConnected) {
                sendCommand(1);
                return;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
        }
    }

    public void pause() {
        synchronized (this.mLock) {
            if (this.mConnected) {
                sendCommand(2);
                return;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
        }
    }

    public void reset() {
        synchronized (this.mLock) {
            if (this.mConnected) {
                sendCommand(3);
                return;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
        }
    }

    public void prepare() {
        synchronized (this.mLock) {
            if (this.mConnected) {
                sendCommand(6);
                return;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
        }
    }

    public void fastForward() {
        synchronized (this.mLock) {
            if (this.mConnected) {
                sendCommand(7);
                return;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
        }
    }

    public void rewind() {
        synchronized (this.mLock) {
            if (this.mConnected) {
                sendCommand(8);
                return;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
        }
    }

    public void seekTo(long pos) {
        synchronized (this.mLock) {
            if (this.mConnected) {
                Bundle args = new Bundle();
                args.putLong("android.support.v4.media.argument.SEEK_POSITION", pos);
                sendCommand(9, args);
                return;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
        }
    }

    public void skipForward() {
    }

    public void skipBackward() {
    }

    public void playFromMediaId(@NonNull String mediaId, @Nullable Bundle extras) {
        synchronized (this.mLock) {
            if (this.mConnected) {
                Bundle args = new Bundle();
                args.putString("android.support.v4.media.argument.MEDIA_ID", mediaId);
                args.putBundle("android.support.v4.media.argument.EXTRAS", extras);
                sendCommand(22, args);
                return;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
        }
    }

    public void playFromSearch(@NonNull String query, @Nullable Bundle extras) {
        synchronized (this.mLock) {
            if (this.mConnected) {
                Bundle args = new Bundle();
                args.putString("android.support.v4.media.argument.QUERY", query);
                args.putBundle("android.support.v4.media.argument.EXTRAS", extras);
                sendCommand(24, args);
                return;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
        }
    }

    public void playFromUri(@NonNull Uri uri, @Nullable Bundle extras) {
        synchronized (this.mLock) {
            if (this.mConnected) {
                Bundle args = new Bundle();
                args.putParcelable("android.support.v4.media.argument.URI", uri);
                args.putBundle("android.support.v4.media.argument.EXTRAS", extras);
                sendCommand(23, args);
                return;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
        }
    }

    public void prepareFromMediaId(@NonNull String mediaId, @Nullable Bundle extras) {
        synchronized (this.mLock) {
            if (this.mConnected) {
                Bundle args = new Bundle();
                args.putString("android.support.v4.media.argument.MEDIA_ID", mediaId);
                args.putBundle("android.support.v4.media.argument.EXTRAS", extras);
                sendCommand(25, args);
                return;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
        }
    }

    public void prepareFromSearch(@NonNull String query, @Nullable Bundle extras) {
        synchronized (this.mLock) {
            if (this.mConnected) {
                Bundle args = new Bundle();
                args.putString("android.support.v4.media.argument.QUERY", query);
                args.putBundle("android.support.v4.media.argument.EXTRAS", extras);
                sendCommand(27, args);
                return;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
        }
    }

    public void prepareFromUri(@NonNull Uri uri, @Nullable Bundle extras) {
        synchronized (this.mLock) {
            if (this.mConnected) {
                Bundle args = new Bundle();
                args.putParcelable("android.support.v4.media.argument.URI", uri);
                args.putBundle("android.support.v4.media.argument.EXTRAS", extras);
                sendCommand(26, args);
                return;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
        }
    }

    public void setVolumeTo(int value, int flags) {
        synchronized (this.mLock) {
            if (this.mConnected) {
                Bundle args = new Bundle();
                args.putInt("android.support.v4.media.argument.VOLUME", value);
                args.putInt("android.support.v4.media.argument.VOLUME_FLAGS", flags);
                sendCommand(10, args);
                return;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
        }
    }

    public void adjustVolume(int direction, int flags) {
        synchronized (this.mLock) {
            if (this.mConnected) {
                Bundle args = new Bundle();
                args.putInt("android.support.v4.media.argument.VOLUME_DIRECTION", direction);
                args.putInt("android.support.v4.media.argument.VOLUME_FLAGS", flags);
                sendCommand(11, args);
                return;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
        }
    }

    @Nullable
    public PendingIntent getSessionActivity() {
        synchronized (this.mLock) {
            if (this.mConnected) {
                PendingIntent sessionActivity = this.mControllerCompat.getSessionActivity();
                return sessionActivity;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
            return null;
        }
    }

    public int getPlayerState() {
        int i;
        synchronized (this.mLock) {
            i = this.mPlayerState;
        }
        return i;
    }

    /* JADX WARNING: Missing block: B:12:0x001e, code:
            return -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long getDuration() {
        synchronized (this.mLock) {
            if (this.mMediaMetadataCompat == null || !this.mMediaMetadataCompat.containsKey("android.media.metadata.DURATION")) {
            } else {
                long j = this.mMediaMetadataCompat.getLong("android.media.metadata.DURATION");
                return j;
            }
        }
    }

    public long getCurrentPosition() {
        synchronized (this.mLock) {
            if (!this.mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return -1;
            } else if (this.mPlaybackStateCompat != null) {
                long timeDiff;
                if (this.mInstance.mTimeDiff != null) {
                    timeDiff = this.mInstance.mTimeDiff.longValue();
                } else {
                    timeDiff = SystemClock.elapsedRealtime() - this.mPlaybackStateCompat.getLastPositionUpdateTime();
                }
                long max = Math.max(0, this.mPlaybackStateCompat.getPosition() + ((long) (this.mPlaybackStateCompat.getPlaybackSpeed() * ((float) timeDiff))));
                return max;
            } else {
                return -1;
            }
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0022, code:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public float getPlaybackSpeed() {
        synchronized (this.mLock) {
            float f = 0.0f;
            if (!this.mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return 0.0f;
            } else if (this.mPlaybackStateCompat != null) {
                f = this.mPlaybackStateCompat.getPlaybackSpeed();
            }
        }
    }

    public void setPlaybackSpeed(float speed) {
        synchronized (this.mLock) {
            if (this.mConnected) {
                Bundle args = new Bundle();
                args.putFloat("android.support.v4.media.argument.PLAYBACK_SPEED", speed);
                sendCommand(39, args);
                return;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
        }
    }

    public int getBufferingState() {
        synchronized (this.mLock) {
            if (this.mConnected) {
                int i = this.mBufferingState;
                return i;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
            return 0;
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0023, code:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long getBufferedPosition() {
        synchronized (this.mLock) {
            long j = -1;
            if (!this.mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return -1;
            } else if (this.mPlaybackStateCompat != null) {
                j = this.mPlaybackStateCompat.getBufferedPosition();
            }
        }
    }

    @Nullable
    public PlaybackInfo getPlaybackInfo() {
        PlaybackInfo playbackInfo;
        synchronized (this.mLock) {
            playbackInfo = this.mPlaybackInfo;
        }
        return playbackInfo;
    }

    public void setRating(@NonNull String mediaId, @NonNull Rating2 rating) {
        synchronized (this.mLock) {
            if (this.mConnected) {
                Bundle args = new Bundle();
                args.putString("android.support.v4.media.argument.MEDIA_ID", mediaId);
                args.putBundle("android.support.v4.media.argument.RATING", rating.toBundle());
                sendCommand(28, args);
                return;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
        }
    }

    public void sendCustomCommand(@NonNull SessionCommand2 command, @Nullable Bundle args, @Nullable ResultReceiver cb) {
        synchronized (this.mLock) {
            if (this.mConnected) {
                Bundle bundle = new Bundle();
                bundle.putBundle("android.support.v4.media.argument.CUSTOM_COMMAND", command.toBundle());
                bundle.putBundle("android.support.v4.media.argument.ARGUMENTS", args);
                sendCommand("android.support.v4.media.controller.command.BY_CUSTOM_COMMAND", bundle, cb);
                return;
            }
            Log.w(TAG, "Session isn't active", new IllegalStateException());
        }
    }

    @Nullable
    public List<MediaItem2> getPlaylist() {
        List<MediaItem2> list;
        synchronized (this.mLock) {
            list = this.mPlaylist;
        }
        return list;
    }

    public void setPlaylist(@NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) {
        Bundle args = new Bundle();
        args.putParcelableArray("android.support.v4.media.argument.PLAYLIST", MediaUtils2.convertMediaItem2ListToParcelableArray(list));
        args.putBundle("android.support.v4.media.argument.PLAYLIST_METADATA", metadata == null ? null : metadata.toBundle());
        sendCommand(19, args);
    }

    public void updatePlaylistMetadata(@Nullable MediaMetadata2 metadata) {
        Bundle args = new Bundle();
        args.putBundle("android.support.v4.media.argument.PLAYLIST_METADATA", metadata == null ? null : metadata.toBundle());
        sendCommand(21, args);
    }

    @Nullable
    public MediaMetadata2 getPlaylistMetadata() {
        MediaMetadata2 mediaMetadata2;
        synchronized (this.mLock) {
            mediaMetadata2 = this.mPlaylistMetadata;
        }
        return mediaMetadata2;
    }

    public void addPlaylistItem(int index, @NonNull MediaItem2 item) {
        Bundle args = new Bundle();
        args.putInt("android.support.v4.media.argument.PLAYLIST_INDEX", index);
        args.putBundle("android.support.v4.media.argument.MEDIA_ITEM", item.toBundle());
        sendCommand(15, args);
    }

    public void removePlaylistItem(@NonNull MediaItem2 item) {
        Bundle args = new Bundle();
        args.putBundle("android.support.v4.media.argument.MEDIA_ITEM", item.toBundle());
        sendCommand(16, args);
    }

    public void replacePlaylistItem(int index, @NonNull MediaItem2 item) {
        Bundle args = new Bundle();
        args.putInt("android.support.v4.media.argument.PLAYLIST_INDEX", index);
        args.putBundle("android.support.v4.media.argument.MEDIA_ITEM", item.toBundle());
        sendCommand(17, args);
    }

    public MediaItem2 getCurrentMediaItem() {
        MediaItem2 mediaItem2;
        synchronized (this.mLock) {
            mediaItem2 = this.mCurrentMediaItem;
        }
        return mediaItem2;
    }

    public void skipToPreviousItem() {
        sendCommand(5);
    }

    public void skipToNextItem() {
        sendCommand(4);
    }

    public void skipToPlaylistItem(@NonNull MediaItem2 item) {
        Bundle args = new Bundle();
        args.putBundle("android.support.v4.media.argument.MEDIA_ITEM", item.toBundle());
        sendCommand(12, args);
    }

    public int getRepeatMode() {
        int i;
        synchronized (this.mLock) {
            i = this.mRepeatMode;
        }
        return i;
    }

    public void setRepeatMode(int repeatMode) {
        Bundle args = new Bundle();
        args.putInt("android.support.v4.media.argument.REPEAT_MODE", repeatMode);
        sendCommand(14, args);
    }

    public int getShuffleMode() {
        int i;
        synchronized (this.mLock) {
            i = this.mShuffleMode;
        }
        return i;
    }

    public void setShuffleMode(int shuffleMode) {
        Bundle args = new Bundle();
        args.putInt("android.support.v4.media.argument.SHUFFLE_MODE", shuffleMode);
        sendCommand(13, args);
    }

    public void subscribeRoutesInfo() {
        sendCommand(36);
    }

    public void unsubscribeRoutesInfo() {
        sendCommand(37);
    }

    public void selectRoute(@NonNull Bundle route) {
        Bundle args = new Bundle();
        args.putBundle("android.support.v4.media.argument.ROUTE_BUNDLE", route);
        sendCommand(38, args);
    }

    @NonNull
    public Context getContext() {
        return this.mContext;
    }

    @NonNull
    public ControllerCallback getCallback() {
        return this.mCallback;
    }

    @NonNull
    public Executor getCallbackExecutor() {
        return this.mCallbackExecutor;
    }

    @Nullable
    public MediaBrowserCompat getBrowserCompat() {
        MediaBrowserCompat mediaBrowserCompat;
        synchronized (this.mLock) {
            mediaBrowserCompat = this.mBrowserCompat;
        }
        return mediaBrowserCompat;
    }

    @NonNull
    public MediaController2 getInstance() {
        return this.mInstance;
    }

    /* JADX WARNING: Missing block: B:11:0x0088, code:
            if (null == null) goto L_0x008d;
     */
    /* JADX WARNING: Missing block: B:12:0x008a, code:
            close();
     */
    /* JADX WARNING: Missing block: B:13:0x008d, code:
            return;
     */
    /* JADX WARNING: Missing block: B:19:0x009b, code:
            if (true == false) goto L_0x00a0;
     */
    /* JADX WARNING: Missing block: B:20:0x009d, code:
            close();
     */
    /* JADX WARNING: Missing block: B:21:0x00a0, code:
            return;
     */
    /* JADX WARNING: Missing block: B:26:?, code:
            r14.mCallbackExecutor.execute(new android.support.v4.media.MediaController2ImplLegacy.AnonymousClass2(r14));
     */
    /* JADX WARNING: Missing block: B:27:0x00c3, code:
            if (null == null) goto L_0x00c8;
     */
    /* JADX WARNING: Missing block: B:28:0x00c5, code:
            close();
     */
    /* JADX WARNING: Missing block: B:29:0x00c8, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void onConnectedNotLocked(Bundle data) {
        data.setClassLoader(MediaSession2.class.getClassLoader());
        final SessionCommandGroup2 allowedCommands = SessionCommandGroup2.fromBundle(data.getBundle("android.support.v4.media.argument.ALLOWED_COMMANDS"));
        int playerState = data.getInt("android.support.v4.media.argument.PLAYER_STATE");
        MediaItem2 currentMediaItem = MediaItem2.fromBundle(data.getBundle("android.support.v4.media.argument.MEDIA_ITEM"));
        int bufferingState = data.getInt("android.support.v4.media.argument.BUFFERING_STATE");
        PlaybackStateCompat playbackStateCompat = (PlaybackStateCompat) data.getParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT");
        int repeatMode = data.getInt("android.support.v4.media.argument.REPEAT_MODE");
        int shuffleMode = data.getInt("android.support.v4.media.argument.SHUFFLE_MODE");
        List<MediaItem2> playlist = MediaUtils2.convertToMediaItem2List(data.getParcelableArray("android.support.v4.media.argument.PLAYLIST"));
        PlaybackInfo playbackInfo = PlaybackInfo.fromBundle(data.getBundle("android.support.v4.media.argument.PLAYBACK_INFO"));
        MediaMetadata2 metadata = MediaMetadata2.fromBundle(data.getBundle("android.support.v4.media.argument.PLAYLIST_METADATA"));
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onConnectedNotLocked token=");
            stringBuilder.append(this.mToken);
            stringBuilder.append(", allowedCommands=");
            stringBuilder.append(allowedCommands);
            Log.d(str, stringBuilder.toString());
        }
        try {
            synchronized (this.mLock) {
                if (this.mIsReleased) {
                } else if (this.mConnected) {
                    Log.e(TAG, "Cannot be notified about the connection result many times. Probably a bug or malicious app.");
                } else {
                    this.mAllowedCommands = allowedCommands;
                    this.mPlayerState = playerState;
                    this.mCurrentMediaItem = currentMediaItem;
                    this.mBufferingState = bufferingState;
                    this.mPlaybackStateCompat = playbackStateCompat;
                    this.mRepeatMode = repeatMode;
                    this.mShuffleMode = shuffleMode;
                    this.mPlaylist = playlist;
                    this.mPlaylistMetadata = metadata;
                    this.mConnected = true;
                    this.mPlaybackInfo = playbackInfo;
                }
            }
        } catch (Throwable th) {
            if (null != null) {
                close();
            }
        }
    }

    private void connectToSession(Token sessionCompatToken) {
        MediaControllerCompat controllerCompat = null;
        try {
            controllerCompat = new MediaControllerCompat(this.mContext, sessionCompatToken);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        synchronized (this.mLock) {
            this.mControllerCompat = controllerCompat;
            this.mControllerCompatCallback = new ControllerCompatCallback(this, null);
            this.mControllerCompat.registerCallback(this.mControllerCompatCallback, this.mHandler);
        }
        if (controllerCompat.isSessionReady()) {
            sendCommand("android.support.v4.media.controller.command.CONNECT", new ResultReceiver(this.mHandler) {
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    if (MediaController2ImplLegacy.this.mHandlerThread.isAlive()) {
                        switch (resultCode) {
                            case -1:
                                MediaController2ImplLegacy.this.mCallbackExecutor.execute(new Runnable() {
                                    public void run() {
                                        MediaController2ImplLegacy.this.mCallback.onDisconnected(MediaController2ImplLegacy.this.mInstance);
                                    }
                                });
                                MediaController2ImplLegacy.this.close();
                                break;
                            case 0:
                                MediaController2ImplLegacy.this.onConnectedNotLocked(resultData);
                                break;
                        }
                    }
                }
            });
        }
    }

    private void connectToService() {
        this.mCallbackExecutor.execute(new Runnable() {
            public void run() {
                synchronized (MediaController2ImplLegacy.this.mLock) {
                    MediaController2ImplLegacy.this.mBrowserCompat = new MediaBrowserCompat(MediaController2ImplLegacy.this.mContext, MediaController2ImplLegacy.this.mToken.getComponentName(), new ConnectionCallback(MediaController2ImplLegacy.this, null), MediaController2ImplLegacy.sDefaultRootExtras);
                    MediaController2ImplLegacy.this.mBrowserCompat.connect();
                }
            }
        });
    }

    private void sendCommand(int commandCode) {
        sendCommand(commandCode, null);
    }

    private void sendCommand(int commandCode, Bundle args) {
        if (args == null) {
            args = new Bundle();
        }
        args.putInt("android.support.v4.media.argument.COMMAND_CODE", commandCode);
        sendCommand("android.support.v4.media.controller.command.BY_COMMAND_CODE", args, null);
    }

    private void sendCommand(String command) {
        sendCommand(command, null, null);
    }

    private void sendCommand(String command, ResultReceiver receiver) {
        sendCommand(command, null, receiver);
    }

    private void sendCommand(String command, Bundle args, ResultReceiver receiver) {
        Throwable th;
        if (args == null) {
            args = new Bundle();
        }
        synchronized (this.mLock) {
            try {
                MediaControllerCompat controller = this.mControllerCompat;
                try {
                    ControllerCompatCallback controller2 = this.mControllerCompatCallback;
                    BundleCompat.putBinder(args, "android.support.v4.media.argument.ICONTROLLER_CALLBACK", controller2.getIControllerCallback().asBinder());
                    args.putString("android.support.v4.media.argument.PACKAGE_NAME", this.mContext.getPackageName());
                    args.putInt("android.support.v4.media.argument.UID", Process.myUid());
                    args.putInt("android.support.v4.media.argument.PID", Process.myPid());
                    controller.sendCommand(command, args, receiver);
                } catch (Throwable th2) {
                    th = th2;
                    MediaControllerCompat mediaControllerCompat = controller;
                    controller = null;
                    MediaControllerCompat controller3 = mediaControllerCompat;
                    while (true) {
                        try {
                            break;
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    }
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                ControllerCompatCallback callback = null;
                while (true) {
                    break;
                }
                throw th;
            }
        }
    }
}
