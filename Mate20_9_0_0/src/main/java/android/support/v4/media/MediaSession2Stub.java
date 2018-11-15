package android.support.v4.media;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.mediacompat.Rating2;
import android.support.v4.media.IMediaSession2.Stub;
import android.support.v4.media.MediaController2.PlaybackInfo;
import android.support.v4.media.MediaSession2.CommandButton;
import android.support.v4.media.MediaSession2.ControllerInfo;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@TargetApi(19)
class MediaSession2Stub extends Stub {
    private static final boolean DEBUG = true;
    private static final String TAG = "MediaSession2Stub";
    private static final SparseArray<SessionCommand2> sCommandsForOnCommandRequest = new SparseArray();
    @GuardedBy("mLock")
    private final ArrayMap<ControllerInfo, SessionCommandGroup2> mAllowedCommandGroupMap = new ArrayMap();
    @GuardedBy("mLock")
    private final Set<IBinder> mConnectingControllers = new HashSet();
    final Context mContext;
    @GuardedBy("mLock")
    private final ArrayMap<IBinder, ControllerInfo> mControllers = new ArrayMap();
    private final Object mLock = new Object();
    final SupportLibraryImpl mSession;

    @FunctionalInterface
    private interface SessionRunnable {
        void run(ControllerInfo controllerInfo) throws RemoteException;
    }

    static final class Controller2Cb extends ControllerCb {
        private final IMediaController2 mIControllerCallback;

        Controller2Cb(@NonNull IMediaController2 callback) {
            this.mIControllerCallback = callback;
        }

        @NonNull
        IBinder getId() {
            return this.mIControllerCallback.asBinder();
        }

        void onCustomLayoutChanged(List<CommandButton> layout) throws RemoteException {
            this.mIControllerCallback.onCustomLayoutChanged(MediaUtils2.convertCommandButtonListToBundleList(layout));
        }

        void onPlaybackInfoChanged(PlaybackInfo info) throws RemoteException {
            this.mIControllerCallback.onPlaybackInfoChanged(info.toBundle());
        }

        void onAllowedCommandsChanged(SessionCommandGroup2 commands) throws RemoteException {
            this.mIControllerCallback.onAllowedCommandsChanged(commands.toBundle());
        }

        void onCustomCommand(SessionCommand2 command, Bundle args, ResultReceiver receiver) throws RemoteException {
            this.mIControllerCallback.onCustomCommand(command.toBundle(), args, receiver);
        }

        void onPlayerStateChanged(long eventTimeMs, long positionMs, int playerState) throws RemoteException {
            this.mIControllerCallback.onPlayerStateChanged(eventTimeMs, positionMs, playerState);
        }

        void onPlaybackSpeedChanged(long eventTimeMs, long positionMs, float speed) throws RemoteException {
            this.mIControllerCallback.onPlaybackSpeedChanged(eventTimeMs, positionMs, speed);
        }

        void onBufferingStateChanged(MediaItem2 item, int state, long bufferedPositionMs) throws RemoteException {
            Bundle bundle;
            IMediaController2 iMediaController2 = this.mIControllerCallback;
            if (item == null) {
                bundle = null;
            } else {
                bundle = item.toBundle();
            }
            iMediaController2.onBufferingStateChanged(bundle, state, bufferedPositionMs);
        }

        void onSeekCompleted(long eventTimeMs, long positionMs, long seekPositionMs) throws RemoteException {
            this.mIControllerCallback.onSeekCompleted(eventTimeMs, positionMs, seekPositionMs);
        }

        void onError(int errorCode, Bundle extras) throws RemoteException {
            this.mIControllerCallback.onError(errorCode, extras);
        }

        void onCurrentMediaItemChanged(MediaItem2 item) throws RemoteException {
            this.mIControllerCallback.onCurrentMediaItemChanged(item == null ? null : item.toBundle());
        }

        void onPlaylistChanged(List<MediaItem2> playlist, MediaMetadata2 metadata) throws RemoteException {
            Bundle bundle;
            IMediaController2 iMediaController2 = this.mIControllerCallback;
            List convertMediaItem2ListToBundleList = MediaUtils2.convertMediaItem2ListToBundleList(playlist);
            if (metadata == null) {
                bundle = null;
            } else {
                bundle = metadata.toBundle();
            }
            iMediaController2.onPlaylistChanged(convertMediaItem2ListToBundleList, bundle);
        }

        void onPlaylistMetadataChanged(MediaMetadata2 metadata) throws RemoteException {
            this.mIControllerCallback.onPlaylistMetadataChanged(metadata.toBundle());
        }

        void onShuffleModeChanged(int shuffleMode) throws RemoteException {
            this.mIControllerCallback.onShuffleModeChanged(shuffleMode);
        }

        void onRepeatModeChanged(int repeatMode) throws RemoteException {
            this.mIControllerCallback.onRepeatModeChanged(repeatMode);
        }

        void onRoutesInfoChanged(List<Bundle> routes) throws RemoteException {
            this.mIControllerCallback.onRoutesInfoChanged(routes);
        }

        void onGetLibraryRootDone(Bundle rootHints, String rootMediaId, Bundle rootExtra) throws RemoteException {
            this.mIControllerCallback.onGetLibraryRootDone(rootHints, rootMediaId, rootExtra);
        }

        void onChildrenChanged(String parentId, int itemCount, Bundle extras) throws RemoteException {
            this.mIControllerCallback.onChildrenChanged(parentId, itemCount, extras);
        }

        void onGetChildrenDone(String parentId, int page, int pageSize, List<MediaItem2> result, Bundle extras) throws RemoteException {
            this.mIControllerCallback.onGetChildrenDone(parentId, page, pageSize, MediaUtils2.convertMediaItem2ListToBundleList(result), extras);
        }

        void onGetItemDone(String mediaId, MediaItem2 result) throws RemoteException {
            this.mIControllerCallback.onGetItemDone(mediaId, result == null ? null : result.toBundle());
        }

        void onSearchResultChanged(String query, int itemCount, Bundle extras) throws RemoteException {
            this.mIControllerCallback.onSearchResultChanged(query, itemCount, extras);
        }

        void onGetSearchResultDone(String query, int page, int pageSize, List<MediaItem2> result, Bundle extras) throws RemoteException {
            this.mIControllerCallback.onGetSearchResultDone(query, page, pageSize, MediaUtils2.convertMediaItem2ListToBundleList(result), extras);
        }

        void onDisconnected() throws RemoteException {
            this.mIControllerCallback.onDisconnected();
        }
    }

    static {
        SessionCommandGroup2 group = new SessionCommandGroup2();
        group.addAllPlaybackCommands();
        group.addAllPlaylistCommands();
        group.addAllVolumeCommands();
        for (SessionCommand2 command : group.getCommands()) {
            sCommandsForOnCommandRequest.append(command.getCommandCode(), command);
        }
    }

    MediaSession2Stub(SupportLibraryImpl session) {
        this.mSession = session;
        this.mContext = this.mSession.getContext();
    }

    List<ControllerInfo> getConnectedControllers() {
        ArrayList<ControllerInfo> controllers = new ArrayList();
        synchronized (this.mLock) {
            for (int i = 0; i < this.mControllers.size(); i++) {
                controllers.add(this.mControllers.valueAt(i));
            }
        }
        return controllers;
    }

    void setAllowedCommands(ControllerInfo controller, SessionCommandGroup2 commands) {
        synchronized (this.mLock) {
            this.mAllowedCommandGroupMap.put(controller, commands);
        }
    }

    private boolean isAllowedCommand(ControllerInfo controller, SessionCommand2 command) {
        SessionCommandGroup2 allowedCommands;
        synchronized (this.mLock) {
            allowedCommands = (SessionCommandGroup2) this.mAllowedCommandGroupMap.get(controller);
        }
        return (allowedCommands == null || !allowedCommands.hasCommand(command)) ? false : DEBUG;
    }

    private boolean isAllowedCommand(ControllerInfo controller, int commandCode) {
        SessionCommandGroup2 allowedCommands;
        synchronized (this.mLock) {
            allowedCommands = (SessionCommandGroup2) this.mAllowedCommandGroupMap.get(controller);
        }
        return (allowedCommands == null || !allowedCommands.hasCommand(commandCode)) ? false : DEBUG;
    }

    private void onSessionCommand(@NonNull IMediaController2 caller, int commandCode, @NonNull SessionRunnable runnable) {
        onSessionCommandInternal(caller, null, commandCode, runnable);
    }

    private void onSessionCommand(@NonNull IMediaController2 caller, @NonNull SessionCommand2 sessionCommand, @NonNull SessionRunnable runnable) {
        onSessionCommandInternal(caller, sessionCommand, 0, runnable);
    }

    private void onSessionCommandInternal(@NonNull IMediaController2 caller, @Nullable SessionCommand2 sessionCommand, int commandCode, @NonNull SessionRunnable runnable) {
        ControllerInfo controller;
        synchronized (this.mLock) {
            controller = null;
            if (caller != null) {
                controller = (ControllerInfo) this.mControllers.get(caller.asBinder());
            }
        }
        if (!this.mSession.isClosed() && controller != null) {
            final ControllerInfo controllerInfo = controller;
            final SessionCommand2 sessionCommand2 = sessionCommand;
            final int i = commandCode;
            final SessionRunnable sessionRunnable = runnable;
            this.mSession.getCallbackExecutor().execute(new Runnable() {
                /* JADX WARNING: Missing block: B:9:0x001a, code:
            if (r5 == null) goto L_0x003a;
     */
                /* JADX WARNING: Missing block: B:11:0x0026, code:
            if (android.support.v4.media.MediaSession2Stub.access$200(r5.this$0, r4, r5) != false) goto L_0x0029;
     */
                /* JADX WARNING: Missing block: B:12:0x0028, code:
            return;
     */
                /* JADX WARNING: Missing block: B:13:0x0029, code:
            r0 = (android.support.v4.media.SessionCommand2) android.support.v4.media.MediaSession2Stub.access$300().get(r5.getCommandCode());
     */
                /* JADX WARNING: Missing block: B:15:0x0044, code:
            if (android.support.v4.media.MediaSession2Stub.access$400(r5.this$0, r4, r6) != false) goto L_0x0047;
     */
                /* JADX WARNING: Missing block: B:16:0x0046, code:
            return;
     */
                /* JADX WARNING: Missing block: B:17:0x0047, code:
            r0 = (android.support.v4.media.SessionCommand2) android.support.v4.media.MediaSession2Stub.access$300().get(r6);
     */
                /* JADX WARNING: Missing block: B:18:0x0053, code:
            if (r0 == null) goto L_0x009a;
     */
                /* JADX WARNING: Missing block: B:20:0x006b, code:
            if (r5.this$0.mSession.getCallback().onCommandRequest(r5.this$0.mSession.getInstance(), r4, r0) != false) goto L_0x009a;
     */
                /* JADX WARNING: Missing block: B:21:0x006d, code:
            r2 = android.support.v4.media.MediaSession2Stub.TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("Command (");
            r3.append(r0);
            r3.append(") from ");
            r3.append(r4);
            r3.append(" was rejected by ");
            r3.append(r5.this$0.mSession);
            android.util.Log.d(r2, r3.toString());
     */
                /* JADX WARNING: Missing block: B:22:0x0099, code:
            return;
     */
                /* JADX WARNING: Missing block: B:24:?, code:
            r7.run(r4);
     */
                /* JADX WARNING: Missing block: B:25:0x00a2, code:
            r1 = move-exception;
     */
                /* JADX WARNING: Missing block: B:26:0x00a3, code:
            r2 = android.support.v4.media.MediaSession2Stub.TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("Exception in ");
            r3.append(r4.toString());
            android.util.Log.w(r2, r3.toString(), r1);
     */
                /* Code decompiled incorrectly, please refer to instructions dump. */
                public void run() {
                    synchronized (MediaSession2Stub.this.mLock) {
                        if (!MediaSession2Stub.this.mControllers.containsValue(controllerInfo)) {
                        }
                    }
                }
            });
        }
    }

    private void onBrowserCommand(@NonNull IMediaController2 caller, int commandCode, @NonNull SessionRunnable runnable) {
        if (this.mSession instanceof SupportLibraryImpl) {
            onSessionCommandInternal(caller, null, commandCode, runnable);
            return;
        }
        throw new RuntimeException("MediaSession2 cannot handle MediaLibrarySession command");
    }

    void removeControllerInfo(ControllerInfo controller) {
        synchronized (this.mLock) {
            controller = (ControllerInfo) this.mControllers.remove(controller.getId());
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("releasing ");
            stringBuilder.append(controller);
            Log.d(str, stringBuilder.toString());
        }
    }

    private void releaseController(IMediaController2 iController) {
        final ControllerInfo controller;
        synchronized (this.mLock) {
            controller = (ControllerInfo) this.mControllers.remove(iController.asBinder());
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("releasing ");
            stringBuilder.append(controller);
            Log.d(str, stringBuilder.toString());
        }
        if (!this.mSession.isClosed() && controller != null) {
            this.mSession.getCallbackExecutor().execute(new Runnable() {
                public void run() {
                    MediaSession2Stub.this.mSession.getCallback().onDisconnected(MediaSession2Stub.this.mSession.getInstance(), controller);
                }
            });
        }
    }

    public void connect(final IMediaController2 caller, String callingPackage) throws RuntimeException {
        final ControllerInfo controllerInfo = new ControllerInfo(callingPackage, Binder.getCallingPid(), Binder.getCallingUid(), new Controller2Cb(caller));
        this.mSession.getCallbackExecutor().execute(new Runnable() {
            public void run() {
                if (!MediaSession2Stub.this.mSession.isClosed()) {
                    synchronized (MediaSession2Stub.this.mLock) {
                        MediaSession2Stub.this.mConnectingControllers.add(controllerInfo.getId());
                    }
                    SessionCommandGroup2 allowedCommands = MediaSession2Stub.this.mSession.getCallback().onConnect(MediaSession2Stub.this.mSession.getInstance(), controllerInfo);
                    boolean z = (allowedCommands != null || controllerInfo.isTrusted()) ? MediaSession2Stub.DEBUG : false;
                    String str;
                    StringBuilder stringBuilder;
                    SessionCommandGroup2 allowedCommands2;
                    if (z) {
                        str = MediaSession2Stub.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Accepting connection, controllerInfo=");
                        stringBuilder.append(controllerInfo);
                        stringBuilder.append(" allowedCommands=");
                        stringBuilder.append(allowedCommands);
                        Log.d(str, stringBuilder.toString());
                        if (allowedCommands == null) {
                            allowedCommands = new SessionCommandGroup2();
                        }
                        allowedCommands2 = allowedCommands;
                        synchronized (MediaSession2Stub.this.mLock) {
                            MediaSession2Stub.this.mConnectingControllers.remove(controllerInfo.getId());
                            MediaSession2Stub.this.mControllers.put(controllerInfo.getId(), controllerInfo);
                            MediaSession2Stub.this.mAllowedCommandGroupMap.put(controllerInfo, allowedCommands2);
                        }
                        allowedCommands = MediaSession2Stub.this.mSession.getPlayerState();
                        List<MediaItem2> list = null;
                        Bundle currentItem = MediaSession2Stub.this.mSession.getCurrentMediaItem() == null ? null : MediaSession2Stub.this.mSession.getCurrentMediaItem().toBundle();
                        long positionEventTimeMs = SystemClock.elapsedRealtime();
                        long positionMs = MediaSession2Stub.this.mSession.getCurrentPosition();
                        float playbackSpeed = MediaSession2Stub.this.mSession.getPlaybackSpeed();
                        long bufferedPositionMs = MediaSession2Stub.this.mSession.getBufferedPosition();
                        Bundle playbackInfoBundle = MediaSession2Stub.this.mSession.getPlaybackInfo().toBundle();
                        int repeatMode = MediaSession2Stub.this.mSession.getRepeatMode();
                        int shuffleMode = MediaSession2Stub.this.mSession.getShuffleMode();
                        PendingIntent sessionActivity = MediaSession2Stub.this.mSession.getSessionActivity();
                        if (allowedCommands2.hasCommand(18)) {
                            list = MediaSession2Stub.this.mSession.getPlaylist();
                        }
                        List<MediaItem2> playlist = list;
                        List<Bundle> playlistBundle = MediaUtils2.convertMediaItem2ListToBundleList(playlist);
                        if (!MediaSession2Stub.this.mSession.isClosed()) {
                            try {
                                try {
                                    caller.onConnected(MediaSession2Stub.this, allowedCommands2.toBundle(), allowedCommands, currentItem, positionEventTimeMs, positionMs, playbackSpeed, bufferedPositionMs, playbackInfoBundle, repeatMode, shuffleMode, playlistBundle, sessionActivity);
                                } catch (RemoteException e) {
                                }
                            } catch (RemoteException e2) {
                                List<MediaItem2> list2 = playlist;
                            }
                        } else {
                            return;
                        }
                    }
                    synchronized (MediaSession2Stub.this.mLock) {
                        MediaSession2Stub.this.mConnectingControllers.remove(controllerInfo.getId());
                    }
                    str = MediaSession2Stub.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Rejecting connection, controllerInfo=");
                    stringBuilder.append(controllerInfo);
                    Log.d(str, stringBuilder.toString());
                    try {
                        caller.onDisconnected();
                    } catch (RemoteException e3) {
                    }
                    allowedCommands2 = allowedCommands;
                }
            }
        });
    }

    public void release(IMediaController2 caller) throws RemoteException {
        releaseController(caller);
    }

    public void setVolumeTo(IMediaController2 caller, final int value, final int flags) throws RuntimeException {
        onSessionCommand(caller, 10, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                VolumeProviderCompat volumeProvider = MediaSession2Stub.this.mSession.getVolumeProvider();
                if (volumeProvider == null) {
                    MediaSessionCompat sessionCompat = MediaSession2Stub.this.mSession.getSessionCompat();
                    if (sessionCompat != null) {
                        sessionCompat.getController().setVolumeTo(value, flags);
                        return;
                    }
                    return;
                }
                volumeProvider.onSetVolumeTo(value);
            }
        });
    }

    public void adjustVolume(IMediaController2 caller, final int direction, final int flags) throws RuntimeException {
        onSessionCommand(caller, 11, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                VolumeProviderCompat volumeProvider = MediaSession2Stub.this.mSession.getVolumeProvider();
                if (volumeProvider == null) {
                    MediaSessionCompat sessionCompat = MediaSession2Stub.this.mSession.getSessionCompat();
                    if (sessionCompat != null) {
                        sessionCompat.getController().adjustVolume(direction, flags);
                        return;
                    }
                    return;
                }
                volumeProvider.onAdjustVolume(direction);
            }
        });
    }

    public void play(IMediaController2 caller) throws RuntimeException {
        onSessionCommand(caller, 1, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.play();
            }
        });
    }

    public void pause(IMediaController2 caller) throws RuntimeException {
        onSessionCommand(caller, 2, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.pause();
            }
        });
    }

    public void reset(IMediaController2 caller) throws RuntimeException {
        onSessionCommand(caller, 3, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.reset();
            }
        });
    }

    public void prepare(IMediaController2 caller) throws RuntimeException {
        onSessionCommand(caller, 6, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.prepare();
            }
        });
    }

    public void fastForward(IMediaController2 caller) {
        onSessionCommand(caller, 7, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.getCallback().onFastForward(MediaSession2Stub.this.mSession.getInstance(), controller);
            }
        });
    }

    public void rewind(IMediaController2 caller) {
        onSessionCommand(caller, 8, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.getCallback().onRewind(MediaSession2Stub.this.mSession.getInstance(), controller);
            }
        });
    }

    public void seekTo(IMediaController2 caller, final long pos) throws RuntimeException {
        onSessionCommand(caller, 9, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.seekTo(pos);
            }
        });
    }

    public void sendCustomCommand(IMediaController2 caller, Bundle commandBundle, final Bundle args, final ResultReceiver receiver) {
        final SessionCommand2 command = SessionCommand2.fromBundle(commandBundle);
        onSessionCommand(caller, SessionCommand2.fromBundle(commandBundle), new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.getCallback().onCustomCommand(MediaSession2Stub.this.mSession.getInstance(), controller, command, args, receiver);
            }
        });
    }

    public void prepareFromUri(IMediaController2 caller, final Uri uri, final Bundle extras) {
        onSessionCommand(caller, 26, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                if (uri == null) {
                    String str = MediaSession2Stub.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("prepareFromUri(): Ignoring null uri from ");
                    stringBuilder.append(controller);
                    Log.w(str, stringBuilder.toString());
                    return;
                }
                MediaSession2Stub.this.mSession.getCallback().onPrepareFromUri(MediaSession2Stub.this.mSession.getInstance(), controller, uri, extras);
            }
        });
    }

    public void prepareFromSearch(IMediaController2 caller, final String query, final Bundle extras) {
        onSessionCommand(caller, 27, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                if (TextUtils.isEmpty(query)) {
                    String str = MediaSession2Stub.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("prepareFromSearch(): Ignoring empty query from ");
                    stringBuilder.append(controller);
                    Log.w(str, stringBuilder.toString());
                    return;
                }
                MediaSession2Stub.this.mSession.getCallback().onPrepareFromSearch(MediaSession2Stub.this.mSession.getInstance(), controller, query, extras);
            }
        });
    }

    public void prepareFromMediaId(IMediaController2 caller, final String mediaId, final Bundle extras) {
        onSessionCommand(caller, 25, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                if (mediaId == null) {
                    String str = MediaSession2Stub.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("prepareFromMediaId(): Ignoring null mediaId from ");
                    stringBuilder.append(controller);
                    Log.w(str, stringBuilder.toString());
                    return;
                }
                MediaSession2Stub.this.mSession.getCallback().onPrepareFromMediaId(MediaSession2Stub.this.mSession.getInstance(), controller, mediaId, extras);
            }
        });
    }

    public void playFromUri(IMediaController2 caller, final Uri uri, final Bundle extras) {
        onSessionCommand(caller, 23, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                if (uri == null) {
                    String str = MediaSession2Stub.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("playFromUri(): Ignoring null uri from ");
                    stringBuilder.append(controller);
                    Log.w(str, stringBuilder.toString());
                    return;
                }
                MediaSession2Stub.this.mSession.getCallback().onPlayFromUri(MediaSession2Stub.this.mSession.getInstance(), controller, uri, extras);
            }
        });
    }

    public void playFromSearch(IMediaController2 caller, final String query, final Bundle extras) {
        onSessionCommand(caller, 24, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                if (TextUtils.isEmpty(query)) {
                    String str = MediaSession2Stub.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("playFromSearch(): Ignoring empty query from ");
                    stringBuilder.append(controller);
                    Log.w(str, stringBuilder.toString());
                    return;
                }
                MediaSession2Stub.this.mSession.getCallback().onPlayFromSearch(MediaSession2Stub.this.mSession.getInstance(), controller, query, extras);
            }
        });
    }

    public void playFromMediaId(IMediaController2 caller, final String mediaId, final Bundle extras) {
        onSessionCommand(caller, 22, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                if (mediaId == null) {
                    String str = MediaSession2Stub.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("playFromMediaId(): Ignoring null mediaId from ");
                    stringBuilder.append(controller);
                    Log.w(str, stringBuilder.toString());
                    return;
                }
                MediaSession2Stub.this.mSession.getCallback().onPlayFromMediaId(MediaSession2Stub.this.mSession.getInstance(), controller, mediaId, extras);
            }
        });
    }

    public void setRating(IMediaController2 caller, final String mediaId, final Bundle ratingBundle) {
        onSessionCommand(caller, 28, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                String str;
                StringBuilder stringBuilder;
                if (mediaId == null) {
                    str = MediaSession2Stub.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("setRating(): Ignoring null mediaId from ");
                    stringBuilder.append(controller);
                    Log.w(str, stringBuilder.toString());
                } else if (ratingBundle == null) {
                    str = MediaSession2Stub.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("setRating(): Ignoring null ratingBundle from ");
                    stringBuilder.append(controller);
                    Log.w(str, stringBuilder.toString());
                } else {
                    Rating2 rating = Rating2.fromBundle(ratingBundle);
                    if (rating != null) {
                        MediaSession2Stub.this.mSession.getCallback().onSetRating(MediaSession2Stub.this.mSession.getInstance(), controller, mediaId, rating);
                    } else if (ratingBundle == null) {
                        String str2 = MediaSession2Stub.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("setRating(): Ignoring null rating from ");
                        stringBuilder2.append(controller);
                        Log.w(str2, stringBuilder2.toString());
                    }
                }
            }
        });
    }

    public void setPlaybackSpeed(IMediaController2 caller, final float speed) {
        onSessionCommand(caller, 39, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.getInstance().setPlaybackSpeed(speed);
            }
        });
    }

    public void setPlaylist(IMediaController2 caller, final List<Bundle> playlist, final Bundle metadata) {
        onSessionCommand(caller, 19, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                if (playlist == null) {
                    String str = MediaSession2Stub.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setPlaylist(): Ignoring null playlist from ");
                    stringBuilder.append(controller);
                    Log.w(str, stringBuilder.toString());
                    return;
                }
                MediaSession2Stub.this.mSession.getInstance().setPlaylist(MediaUtils2.convertBundleListToMediaItem2List(playlist), MediaMetadata2.fromBundle(metadata));
            }
        });
    }

    public void updatePlaylistMetadata(IMediaController2 caller, final Bundle metadata) {
        onSessionCommand(caller, 21, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.getInstance().updatePlaylistMetadata(MediaMetadata2.fromBundle(metadata));
            }
        });
    }

    public void addPlaylistItem(IMediaController2 caller, final int index, final Bundle mediaItem) {
        onSessionCommand(caller, 15, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.getInstance().addPlaylistItem(index, MediaItem2.fromBundle(mediaItem, null));
            }
        });
    }

    public void removePlaylistItem(IMediaController2 caller, final Bundle mediaItem) {
        onSessionCommand(caller, 16, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.getInstance().removePlaylistItem(MediaItem2.fromBundle(mediaItem));
            }
        });
    }

    public void replacePlaylistItem(IMediaController2 caller, final int index, final Bundle mediaItem) {
        onSessionCommand(caller, 17, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.getInstance().replacePlaylistItem(index, MediaItem2.fromBundle(mediaItem, null));
            }
        });
    }

    public void skipToPlaylistItem(IMediaController2 caller, final Bundle mediaItem) {
        onSessionCommand(caller, 12, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                if (mediaItem == null) {
                    String str = MediaSession2Stub.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("skipToPlaylistItem(): Ignoring null mediaItem from ");
                    stringBuilder.append(controller);
                    Log.w(str, stringBuilder.toString());
                }
                MediaSession2Stub.this.mSession.getInstance().skipToPlaylistItem(MediaItem2.fromBundle(mediaItem));
            }
        });
    }

    public void skipToPreviousItem(IMediaController2 caller) {
        onSessionCommand(caller, 5, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.getInstance().skipToPreviousItem();
            }
        });
    }

    public void skipToNextItem(IMediaController2 caller) {
        onSessionCommand(caller, 4, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.getInstance().skipToNextItem();
            }
        });
    }

    public void setRepeatMode(IMediaController2 caller, final int repeatMode) {
        onSessionCommand(caller, 14, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.getInstance().setRepeatMode(repeatMode);
            }
        });
    }

    public void setShuffleMode(IMediaController2 caller, final int shuffleMode) {
        onSessionCommand(caller, 13, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.getInstance().setShuffleMode(shuffleMode);
            }
        });
    }

    public void subscribeRoutesInfo(IMediaController2 caller) {
        onSessionCommand(caller, 36, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.getCallback().onSubscribeRoutesInfo(MediaSession2Stub.this.mSession.getInstance(), controller);
            }
        });
    }

    public void unsubscribeRoutesInfo(IMediaController2 caller) {
        onSessionCommand(caller, 37, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.getCallback().onUnsubscribeRoutesInfo(MediaSession2Stub.this.mSession.getInstance(), controller);
            }
        });
    }

    public void selectRoute(IMediaController2 caller, final Bundle route) {
        onSessionCommand(caller, 37, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.mSession.getCallback().onSelectRoute(MediaSession2Stub.this.mSession.getInstance(), controller, route);
            }
        });
    }

    private SupportLibraryImpl getLibrarySession() {
        if (this.mSession instanceof SupportLibraryImpl) {
            return (SupportLibraryImpl) this.mSession;
        }
        throw new RuntimeException("Session cannot be casted to library session");
    }

    public void getLibraryRoot(IMediaController2 caller, final Bundle rootHints) throws RuntimeException {
        onBrowserCommand(caller, 31, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                MediaSession2Stub.this.getLibrarySession().onGetLibraryRootOnExecutor(controller, rootHints);
            }
        });
    }

    public void getItem(IMediaController2 caller, final String mediaId) throws RuntimeException {
        onBrowserCommand(caller, 30, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                if (mediaId == null) {
                    String str = MediaSession2Stub.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getItem(): Ignoring null mediaId from ");
                    stringBuilder.append(controller);
                    Log.w(str, stringBuilder.toString());
                    return;
                }
                MediaSession2Stub.this.getLibrarySession().onGetItemOnExecutor(controller, mediaId);
            }
        });
    }

    public void getChildren(IMediaController2 caller, String parentId, int page, int pageSize, Bundle extras) throws RuntimeException {
        final String str = parentId;
        final int i = page;
        final int i2 = pageSize;
        final Bundle bundle = extras;
        onBrowserCommand(caller, 29, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                String str;
                StringBuilder stringBuilder;
                if (str == null) {
                    str = MediaSession2Stub.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getChildren(): Ignoring null parentId from ");
                    stringBuilder.append(controller);
                    Log.w(str, stringBuilder.toString());
                } else if (i < 1 || i2 < 1) {
                    str = MediaSession2Stub.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getChildren(): Ignoring page nor pageSize less than 1 from ");
                    stringBuilder.append(controller);
                    Log.w(str, stringBuilder.toString());
                } else {
                    MediaSession2Stub.this.getLibrarySession().onGetChildrenOnExecutor(controller, str, i, i2, bundle);
                }
            }
        });
    }

    public void search(IMediaController2 caller, final String query, final Bundle extras) {
        onBrowserCommand(caller, 33, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                if (TextUtils.isEmpty(query)) {
                    String str = MediaSession2Stub.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("search(): Ignoring empty query from ");
                    stringBuilder.append(controller);
                    Log.w(str, stringBuilder.toString());
                    return;
                }
                MediaSession2Stub.this.getLibrarySession().onSearchOnExecutor(controller, query, extras);
            }
        });
    }

    public void getSearchResult(IMediaController2 caller, String query, int page, int pageSize, Bundle extras) {
        final String str = query;
        final int i = page;
        final int i2 = pageSize;
        final Bundle bundle = extras;
        onBrowserCommand(caller, 32, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                String str;
                StringBuilder stringBuilder;
                if (TextUtils.isEmpty(str)) {
                    str = MediaSession2Stub.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getSearchResult(): Ignoring empty query from ");
                    stringBuilder.append(controller);
                    Log.w(str, stringBuilder.toString());
                } else if (i < 1 || i2 < 1) {
                    str = MediaSession2Stub.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getSearchResult(): Ignoring page nor pageSize less than 1  from ");
                    stringBuilder.append(controller);
                    Log.w(str, stringBuilder.toString());
                } else {
                    MediaSession2Stub.this.getLibrarySession().onGetSearchResultOnExecutor(controller, str, i, i2, bundle);
                }
            }
        });
    }

    public void subscribe(IMediaController2 caller, final String parentId, final Bundle option) {
        onBrowserCommand(caller, 34, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                if (parentId == null) {
                    String str = MediaSession2Stub.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("subscribe(): Ignoring null parentId from ");
                    stringBuilder.append(controller);
                    Log.w(str, stringBuilder.toString());
                    return;
                }
                MediaSession2Stub.this.getLibrarySession().onSubscribeOnExecutor(controller, parentId, option);
            }
        });
    }

    public void unsubscribe(IMediaController2 caller, final String parentId) {
        onBrowserCommand(caller, 35, new SessionRunnable() {
            public void run(ControllerInfo controller) throws RemoteException {
                if (parentId == null) {
                    String str = MediaSession2Stub.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unsubscribe(): Ignoring null parentId from ");
                    stringBuilder.append(controller);
                    Log.w(str, stringBuilder.toString());
                    return;
                }
                MediaSession2Stub.this.getLibrarySession().onUnsubscribeOnExecutor(controller, parentId);
            }
        });
    }
}
