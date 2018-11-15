package android.support.v4.media;

import android.annotation.TargetApi;
import android.media.AudioAttributes;
import android.media.DeniedByServerException;
import android.media.MediaDataSource;
import android.media.MediaDrm.KeyRequest;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.MediaPlayer.DrmInfo;
import android.media.MediaPlayer.NoDrmSchemeException;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnDrmConfigHelper;
import android.media.MediaPlayer.OnDrmInfoListener;
import android.media.MediaPlayer.OnDrmPreparedListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnMediaTimeDiscontinuityListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnSubtitleDataListener;
import android.media.MediaPlayer.OnTimedMetaDataAvailableListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.media.MediaPlayer.ProvisioningNetworkErrorException;
import android.media.MediaPlayer.ProvisioningServerErrorException;
import android.media.MediaPlayer.TrackInfo;
import android.media.MediaTimestamp;
import android.media.PlaybackParams;
import android.media.ResourceBusyException;
import android.media.SubtitleData;
import android.media.SyncParams;
import android.media.TimedMetaData;
import android.media.UnsupportedSchemeException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.media.BaseMediaPlayer.PlayerEventCallback;
import android.support.v4.media.MediaPlayer2.DrmEventCallback;
import android.support.v4.media.MediaPlayer2.EventCallback;
import android.support.v4.media.PlaybackParams2.Builder;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Preconditions;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

@TargetApi(28)
@RestrictTo({Scope.LIBRARY_GROUP})
public final class MediaPlayer2Impl extends MediaPlayer2 {
    private static final int SOURCE_STATE_ERROR = -1;
    private static final int SOURCE_STATE_INIT = 0;
    private static final int SOURCE_STATE_PREPARED = 2;
    private static final int SOURCE_STATE_PREPARING = 1;
    private static final String TAG = "MediaPlayer2Impl";
    private static ArrayMap<Integer, Integer> sErrorEventMap = new ArrayMap();
    private static ArrayMap<Integer, Integer> sInfoEventMap = new ArrayMap();
    private static ArrayMap<Integer, Integer> sPrepareDrmStatusMap = new ArrayMap();
    private static ArrayMap<Integer, Integer> sStateMap = new ArrayMap();
    private BaseMediaPlayerImpl mBaseMediaPlayerImpl;
    @GuardedBy("mTaskLock")
    private Task mCurrentTask;
    private Pair<Executor, DrmEventCallback> mDrmEventCallbackRecord;
    private final Handler mEndPositionHandler;
    private HandlerThread mHandlerThread = new HandlerThread("MediaPlayer2TaskThread");
    private final Object mLock = new Object();
    private Pair<Executor, EventCallback> mMp2EventCallbackRecord;
    @GuardedBy("mTaskLock")
    private final ArrayDeque<Task> mPendingTasks = new ArrayDeque();
    private MediaPlayerSourceQueue mPlayer;
    private ArrayMap<PlayerEventCallback, Executor> mPlayerEventCallbackMap = new ArrayMap();
    private final Handler mTaskHandler;
    private final Object mTaskLock = new Object();

    private static class DataSourceError {
        final DataSourceDesc mDSD;
        final int mExtra;
        final int mWhat;

        DataSourceError(DataSourceDesc dsd, int what, int extra) {
            this.mDSD = dsd;
            this.mWhat = what;
            this.mExtra = extra;
        }
    }

    private interface DrmEventNotifier {
        void notify(DrmEventCallback drmEventCallback);
    }

    private class MediaPlayerSource {
        final AtomicInteger mBufferedPercentage = new AtomicInteger(0);
        int mBufferingState = 0;
        volatile DataSourceDesc mDSD;
        int mMp2State = 1001;
        boolean mPlayPending;
        final MediaPlayer mPlayer = new MediaPlayer();
        int mPlayerState = 0;
        int mSourceState = 0;

        MediaPlayerSource(DataSourceDesc dsd) {
            this.mDSD = dsd;
            MediaPlayer2Impl.this.setUpListeners(this);
        }

        DataSourceDesc getDSD() {
            return this.mDSD;
        }
    }

    private class MediaPlayerSourceQueue {
        AudioAttributesCompat mAudioAttributes;
        Integer mAudioSessionId;
        Integer mAuxEffect;
        Float mAuxEffectSendLevel;
        PlaybackParams mPlaybackParams;
        List<MediaPlayerSource> mQueue = new ArrayList();
        Surface mSurface;
        SyncParams mSyncParams;
        Float mVolume = Float.valueOf(1.0f);

        MediaPlayerSourceQueue() {
            this.mQueue.add(new MediaPlayerSource(null));
        }

        synchronized MediaPlayer getCurrentPlayer() {
            return ((MediaPlayerSource) this.mQueue.get(0)).mPlayer;
        }

        synchronized MediaPlayerSource getFirst() {
            return (MediaPlayerSource) this.mQueue.get(0);
        }

        synchronized void setFirst(DataSourceDesc dsd) throws IOException {
            if (this.mQueue.isEmpty()) {
                this.mQueue.add(0, new MediaPlayerSource(dsd));
            } else {
                ((MediaPlayerSource) this.mQueue.get(0)).mDSD = dsd;
                MediaPlayer2Impl.this.setUpListeners((MediaPlayerSource) this.mQueue.get(0));
            }
            MediaPlayer2Impl.handleDataSource((MediaPlayerSource) this.mQueue.get(0));
        }

        synchronized DataSourceError setNext(DataSourceDesc dsd) {
            MediaPlayerSource src = new MediaPlayerSource(dsd);
            if (this.mQueue.isEmpty()) {
                this.mQueue.add(src);
                return prepareAt(0);
            }
            this.mQueue.add(1, src);
            return prepareAt(1);
        }

        synchronized DataSourceError setNextMultiple(List<DataSourceDesc> descs) {
            List<MediaPlayerSource> sources = new ArrayList();
            for (DataSourceDesc dsd : descs) {
                sources.add(new MediaPlayerSource(dsd));
            }
            if (this.mQueue.isEmpty()) {
                this.mQueue.addAll(sources);
                return prepareAt(0);
            }
            this.mQueue.addAll(1, sources);
            return prepareAt(1);
        }

        synchronized void play() {
            MediaPlayerSource src = (MediaPlayerSource) this.mQueue.get(0);
            if (src.mSourceState == 2) {
                src.mPlayer.start();
                setMp2State(src.mPlayer, 1004);
            } else {
                throw new IllegalStateException();
            }
        }

        synchronized void prepare() {
            getCurrentPlayer().prepareAsync();
        }

        synchronized void release() {
            getCurrentPlayer().release();
        }

        synchronized void prepareAsync() {
            MediaPlayer mp = getCurrentPlayer();
            mp.prepareAsync();
            setBufferingState(mp, 2);
        }

        synchronized void pause() {
            MediaPlayer mp = getCurrentPlayer();
            mp.pause();
            setMp2State(mp, 1003);
        }

        synchronized long getCurrentPosition() {
            return (long) getCurrentPlayer().getCurrentPosition();
        }

        synchronized long getDuration() {
            return (long) getCurrentPlayer().getDuration();
        }

        synchronized long getBufferedPosition() {
            MediaPlayerSource src;
            src = (MediaPlayerSource) this.mQueue.get(0);
            return (((long) src.mPlayer.getDuration()) * ((long) src.mBufferedPercentage.get())) / 100;
        }

        synchronized void setAudioAttributes(AudioAttributesCompat attributes) {
            AudioAttributes attr;
            this.mAudioAttributes = attributes;
            if (this.mAudioAttributes == null) {
                attr = null;
            } else {
                attr = (AudioAttributes) this.mAudioAttributes.unwrap();
            }
            getCurrentPlayer().setAudioAttributes(attr);
        }

        synchronized AudioAttributesCompat getAudioAttributes() {
            return this.mAudioAttributes;
        }

        synchronized DataSourceError onPrepared(MediaPlayer mp) {
            for (int i = 0; i < this.mQueue.size(); i++) {
                MediaPlayerSource src = (MediaPlayerSource) this.mQueue.get(i);
                if (mp == src.mPlayer) {
                    if (i == 0) {
                        if (src.mPlayPending) {
                            src.mPlayPending = false;
                            src.mPlayer.start();
                            setMp2State(src.mPlayer, 1004);
                        } else {
                            setMp2State(src.mPlayer, 1002);
                        }
                    }
                    src.mSourceState = 2;
                    setBufferingState(src.mPlayer, 1);
                    return prepareAt(i + 1);
                }
            }
            return null;
        }

        synchronized DataSourceError onCompletion(MediaPlayer mp) {
            if (!this.mQueue.isEmpty() && mp == getCurrentPlayer()) {
                if (this.mQueue.size() == 1) {
                    setMp2State(mp, 1003);
                    final DataSourceDesc dsd = ((MediaPlayerSource) this.mQueue.get(0)).getDSD();
                    MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                        public void notify(EventCallback callback) {
                            callback.onInfo(MediaPlayer2Impl.this, dsd, 6, 0);
                        }
                    });
                    return null;
                }
                moveToNext();
            }
            return playCurrent();
        }

        synchronized void moveToNext() {
            MediaPlayerSource src1 = (MediaPlayerSource) this.mQueue.remove(0);
            src1.mPlayer.release();
            if (this.mQueue.isEmpty()) {
                throw new IllegalStateException("player/source queue emptied");
            }
            final MediaPlayerSource src2 = (MediaPlayerSource) this.mQueue.get(0);
            if (src1.mPlayerState != src2.mPlayerState) {
                MediaPlayer2Impl.this.notifyPlayerEvent(new PlayerEventNotifier() {
                    public void notify(PlayerEventCallback cb) {
                        cb.onPlayerStateChanged(MediaPlayer2Impl.this.mBaseMediaPlayerImpl, src2.mPlayerState);
                    }
                });
            }
            MediaPlayer2Impl.this.notifyPlayerEvent(new PlayerEventNotifier() {
                public void notify(PlayerEventCallback cb) {
                    cb.onCurrentDataSourceChanged(MediaPlayer2Impl.this.mBaseMediaPlayerImpl, src2.mDSD);
                }
            });
        }

        synchronized DataSourceError playCurrent() {
            DataSourceError err;
            err = null;
            final MediaPlayerSource src = (MediaPlayerSource) this.mQueue.get(0);
            if (this.mSurface != null) {
                src.mPlayer.setSurface(this.mSurface);
            }
            if (this.mVolume != null) {
                src.mPlayer.setVolume(this.mVolume.floatValue(), this.mVolume.floatValue());
            }
            if (this.mAudioAttributes != null) {
                src.mPlayer.setAudioAttributes((AudioAttributes) this.mAudioAttributes.unwrap());
            }
            if (this.mAuxEffect != null) {
                src.mPlayer.attachAuxEffect(this.mAuxEffect.intValue());
            }
            if (this.mAuxEffectSendLevel != null) {
                src.mPlayer.setAuxEffectSendLevel(this.mAuxEffectSendLevel.floatValue());
            }
            if (this.mSyncParams != null) {
                src.mPlayer.setSyncParams(this.mSyncParams);
            }
            if (this.mPlaybackParams != null) {
                src.mPlayer.setPlaybackParams(this.mPlaybackParams);
            }
            if (src.mSourceState == 2) {
                src.mPlayer.start();
                setMp2State(src.mPlayer, 1004);
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    public void notify(EventCallback callback) {
                        callback.onInfo(MediaPlayer2Impl.this, src.getDSD(), 2, 0);
                    }
                });
            } else {
                if (src.mSourceState == 0) {
                    err = prepareAt(0);
                }
                src.mPlayPending = true;
            }
            return err;
        }

        synchronized void onError(MediaPlayer mp) {
            setMp2State(mp, 1005);
            setBufferingState(mp, 0);
        }

        /* JADX WARNING: Missing block: B:24:0x0059, code:
            return null;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        synchronized DataSourceError prepareAt(int n) {
            if (n < this.mQueue.size() && ((MediaPlayerSource) this.mQueue.get(n)).mSourceState == 0 && (n == 0 || getPlayerState() != 0)) {
                MediaPlayerSource src = (MediaPlayerSource) this.mQueue.get(n);
                try {
                    if (this.mAudioSessionId != null) {
                        src.mPlayer.setAudioSessionId(this.mAudioSessionId.intValue());
                    }
                    src.mSourceState = 1;
                    MediaPlayer2Impl.handleDataSource(src);
                    src.mPlayer.prepareAsync();
                    return null;
                } catch (Exception e) {
                    DataSourceDesc dsd = src.getDSD();
                    setMp2State(src.mPlayer, 1005);
                    return new DataSourceError(dsd, 1, MediaPlayer2.MEDIA_ERROR_UNSUPPORTED);
                }
            }
        }

        synchronized void skipToNext() {
            if (this.mQueue.size() > 1) {
                MediaPlayerSource src = (MediaPlayerSource) this.mQueue.get(0);
                moveToNext();
                if (src.mPlayerState == 2 || src.mPlayPending) {
                    playCurrent();
                }
            } else {
                throw new IllegalStateException("No next source available");
            }
        }

        synchronized void setLooping(boolean loop) {
            getCurrentPlayer().setLooping(loop);
        }

        synchronized void setPlaybackParams(PlaybackParams playbackParams) {
            getCurrentPlayer().setPlaybackParams(playbackParams);
            this.mPlaybackParams = playbackParams;
        }

        synchronized float getVolume() {
            return this.mVolume.floatValue();
        }

        synchronized void setVolume(float volume) {
            this.mVolume = Float.valueOf(volume);
            getCurrentPlayer().setVolume(volume, volume);
        }

        synchronized void setSurface(Surface surface) {
            this.mSurface = surface;
            getCurrentPlayer().setSurface(surface);
        }

        synchronized int getVideoWidth() {
            return getCurrentPlayer().getVideoWidth();
        }

        synchronized int getVideoHeight() {
            return getCurrentPlayer().getVideoHeight();
        }

        synchronized PersistableBundle getMetrics() {
            return getCurrentPlayer().getMetrics();
        }

        synchronized PlaybackParams getPlaybackParams() {
            return getCurrentPlayer().getPlaybackParams();
        }

        synchronized void setSyncParams(SyncParams params) {
            getCurrentPlayer().setSyncParams(params);
            this.mSyncParams = params;
        }

        synchronized SyncParams getSyncParams() {
            return getCurrentPlayer().getSyncParams();
        }

        synchronized void seekTo(long msec, int mode) {
            getCurrentPlayer().seekTo(msec, mode);
        }

        synchronized void reset() {
            MediaPlayerSource src = (MediaPlayerSource) this.mQueue.get(0);
            src.mPlayer.reset();
            src.mBufferedPercentage.set(0);
            this.mVolume = Float.valueOf(1.0f);
            this.mSurface = null;
            this.mAuxEffect = null;
            this.mAuxEffectSendLevel = null;
            this.mAudioAttributes = null;
            this.mAudioSessionId = null;
            this.mSyncParams = null;
            this.mPlaybackParams = null;
            setMp2State(src.mPlayer, 1001);
            setBufferingState(src.mPlayer, 0);
        }

        synchronized MediaTimestamp2 getTimestamp() {
            MediaTimestamp t;
            t = getCurrentPlayer().getTimestamp();
            return t == null ? null : new MediaTimestamp2(t);
        }

        synchronized void setAudioSessionId(int sessionId) {
            getCurrentPlayer().setAudioSessionId(sessionId);
        }

        synchronized int getAudioSessionId() {
            return getCurrentPlayer().getAudioSessionId();
        }

        synchronized void attachAuxEffect(int effectId) {
            getCurrentPlayer().attachAuxEffect(effectId);
            this.mAuxEffect = Integer.valueOf(effectId);
        }

        synchronized void setAuxEffectSendLevel(float level) {
            getCurrentPlayer().setAuxEffectSendLevel(level);
            this.mAuxEffectSendLevel = Float.valueOf(level);
        }

        synchronized TrackInfo[] getTrackInfo() {
            return getCurrentPlayer().getTrackInfo();
        }

        synchronized int getSelectedTrack(int trackType) {
            return getCurrentPlayer().getSelectedTrack(trackType);
        }

        synchronized void selectTrack(int index) {
            getCurrentPlayer().selectTrack(index);
        }

        synchronized void deselectTrack(int index) {
            getCurrentPlayer().deselectTrack(index);
        }

        synchronized DrmInfo getDrmInfo() {
            return getCurrentPlayer().getDrmInfo();
        }

        synchronized void prepareDrm(UUID uuid) throws ResourceBusyException, ProvisioningServerErrorException, ProvisioningNetworkErrorException, UnsupportedSchemeException {
            getCurrentPlayer().prepareDrm(uuid);
        }

        synchronized void releaseDrm() throws NoDrmSchemeException {
            getCurrentPlayer().stop();
            getCurrentPlayer().releaseDrm();
        }

        synchronized byte[] provideKeyResponse(byte[] keySetId, byte[] response) throws DeniedByServerException, NoDrmSchemeException {
            return getCurrentPlayer().provideKeyResponse(keySetId, response);
        }

        synchronized void restoreKeys(byte[] keySetId) throws NoDrmSchemeException {
            getCurrentPlayer().restoreKeys(keySetId);
        }

        synchronized String getDrmPropertyString(String propertyName) throws NoDrmSchemeException {
            return getCurrentPlayer().getDrmPropertyString(propertyName);
        }

        synchronized void setDrmPropertyString(String propertyName, String value) throws NoDrmSchemeException {
            getCurrentPlayer().setDrmPropertyString(propertyName, value);
        }

        synchronized void setOnDrmConfigHelper(OnDrmConfigHelper onDrmConfigHelper) {
            getCurrentPlayer().setOnDrmConfigHelper(onDrmConfigHelper);
        }

        synchronized KeyRequest getKeyRequest(byte[] keySetId, byte[] initData, String mimeType, int keyType, Map<String, String> optionalParameters) throws NoDrmSchemeException {
            return getCurrentPlayer().getKeyRequest(keySetId, initData, mimeType, keyType, optionalParameters);
        }

        synchronized void setMp2State(MediaPlayer mp, int mp2State) {
            for (MediaPlayerSource src : this.mQueue) {
                if (src.mPlayer == mp) {
                    if (src.mMp2State != mp2State) {
                        src.mMp2State = mp2State;
                        final int playerState = ((Integer) MediaPlayer2Impl.sStateMap.get(Integer.valueOf(mp2State))).intValue();
                        if (src.mPlayerState != playerState) {
                            src.mPlayerState = playerState;
                            MediaPlayer2Impl.this.notifyPlayerEvent(new PlayerEventNotifier() {
                                public void notify(PlayerEventCallback cb) {
                                    cb.onPlayerStateChanged(MediaPlayer2Impl.this.mBaseMediaPlayerImpl, playerState);
                                }
                            });
                            return;
                        }
                        return;
                    }
                    return;
                }
            }
        }

        synchronized void setBufferingState(MediaPlayer mp, final int state) {
            for (final MediaPlayerSource src : this.mQueue) {
                if (src.mPlayer == mp) {
                    if (src.mBufferingState != state) {
                        src.mBufferingState = state;
                        MediaPlayer2Impl.this.notifyPlayerEvent(new PlayerEventNotifier() {
                            public void notify(PlayerEventCallback cb) {
                                cb.onBufferingStateChanged(MediaPlayer2Impl.this.mBaseMediaPlayerImpl, src.getDSD(), state);
                            }
                        });
                        return;
                    }
                    return;
                }
            }
        }

        synchronized int getMediaPlayer2State() {
            return ((MediaPlayerSource) this.mQueue.get(0)).mMp2State;
        }

        synchronized int getBufferingState() {
            return ((MediaPlayerSource) this.mQueue.get(0)).mBufferingState;
        }

        synchronized int getPlayerState() {
            return ((MediaPlayerSource) this.mQueue.get(0)).mPlayerState;
        }

        synchronized MediaPlayerSource getSourceForPlayer(MediaPlayer mp) {
            for (MediaPlayerSource src : this.mQueue) {
                if (src.mPlayer == mp) {
                    return src;
                }
            }
            return null;
        }
    }

    private interface Mp2EventNotifier {
        void notify(EventCallback eventCallback);
    }

    private interface PlayerEventNotifier {
        void notify(PlayerEventCallback playerEventCallback);
    }

    private abstract class Task implements Runnable {
        private DataSourceDesc mDSD;
        private final int mMediaCallType;
        private final boolean mNeedToWaitForEventToComplete;

        abstract void process() throws IOException, MediaPlayer2.NoDrmSchemeException;

        Task(int mediaCallType, boolean needToWaitForEventToComplete) {
            this.mMediaCallType = mediaCallType;
            this.mNeedToWaitForEventToComplete = needToWaitForEventToComplete;
        }

        public void run() {
            int status = 0;
            try {
                process();
            } catch (IllegalStateException e) {
                status = 1;
            } catch (IllegalArgumentException e2) {
                status = 2;
            } catch (SecurityException e3) {
                status = 3;
            } catch (IOException e4) {
                status = 4;
            } catch (Exception e5) {
                status = Integer.MIN_VALUE;
            }
            this.mDSD = MediaPlayer2Impl.this.getCurrentDataSource();
            if (!this.mNeedToWaitForEventToComplete || status != 0) {
                sendCompleteNotification(status);
                synchronized (MediaPlayer2Impl.this.mTaskLock) {
                    MediaPlayer2Impl.this.mCurrentTask = null;
                    MediaPlayer2Impl.this.processPendingTask_l();
                }
            }
        }

        private void sendCompleteNotification(final int status) {
            if (this.mMediaCallType != 1003) {
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    public void notify(EventCallback cb) {
                        cb.onCallCompleted(MediaPlayer2Impl.this, Task.this.mDSD, Task.this.mMediaCallType, status);
                    }
                });
            }
        }
    }

    private class BaseMediaPlayerImpl extends BaseMediaPlayer {
        private BaseMediaPlayerImpl() {
        }

        /* synthetic */ BaseMediaPlayerImpl(MediaPlayer2Impl x0, AnonymousClass1 x1) {
            this();
        }

        public void play() {
            MediaPlayer2Impl.this.play();
        }

        public void prepare() {
            MediaPlayer2Impl.this.prepare();
        }

        public void pause() {
            MediaPlayer2Impl.this.pause();
        }

        public void reset() {
            MediaPlayer2Impl.this.reset();
        }

        public void skipToNext() {
            MediaPlayer2Impl.this.skipToNext();
        }

        public void seekTo(long pos) {
            MediaPlayer2Impl.this.seekTo(pos);
        }

        public long getCurrentPosition() {
            return MediaPlayer2Impl.this.getCurrentPosition();
        }

        public long getDuration() {
            return MediaPlayer2Impl.this.getDuration();
        }

        public long getBufferedPosition() {
            return MediaPlayer2Impl.this.getBufferedPosition();
        }

        public int getPlayerState() {
            return MediaPlayer2Impl.this.getPlayerState();
        }

        public int getBufferingState() {
            return MediaPlayer2Impl.this.getBufferingState();
        }

        public void setAudioAttributes(AudioAttributesCompat attributes) {
            MediaPlayer2Impl.this.setAudioAttributes(attributes);
        }

        public AudioAttributesCompat getAudioAttributes() {
            return MediaPlayer2Impl.this.getAudioAttributes();
        }

        public void setDataSource(DataSourceDesc dsd) {
            MediaPlayer2Impl.this.setDataSource(dsd);
        }

        public void setNextDataSource(DataSourceDesc dsd) {
            MediaPlayer2Impl.this.setNextDataSource(dsd);
        }

        public void setNextDataSources(List<DataSourceDesc> dsds) {
            MediaPlayer2Impl.this.setNextDataSources(dsds);
        }

        public DataSourceDesc getCurrentDataSource() {
            return MediaPlayer2Impl.this.getCurrentDataSource();
        }

        public void loopCurrent(boolean loop) {
            MediaPlayer2Impl.this.loopCurrent(loop);
        }

        public void setPlaybackSpeed(float speed) {
            MediaPlayer2Impl.this.setPlaybackParams(new Builder(MediaPlayer2Impl.this.getPlaybackParams().getPlaybackParams()).setSpeed(speed).build());
        }

        public float getPlaybackSpeed() {
            return MediaPlayer2Impl.this.getPlaybackParams().getSpeed().floatValue();
        }

        public void setPlayerVolume(float volume) {
            MediaPlayer2Impl.this.setPlayerVolume(volume);
        }

        public float getPlayerVolume() {
            return MediaPlayer2Impl.this.getPlayerVolume();
        }

        public void registerPlayerEventCallback(Executor e, PlayerEventCallback cb) {
            MediaPlayer2Impl.this.registerPlayerEventCallback(e, cb);
        }

        public void unregisterPlayerEventCallback(PlayerEventCallback cb) {
            MediaPlayer2Impl.this.unregisterPlayerEventCallback(cb);
        }

        public void close() throws Exception {
            MediaPlayer2Impl.this.close();
        }
    }

    public static final class DrmInfoImpl extends MediaPlayer2.DrmInfo {
        private Map<UUID, byte[]> mMapPssh;
        private UUID[] mSupportedSchemes;

        /* synthetic */ DrmInfoImpl(Map x0, UUID[] x1, AnonymousClass1 x2) {
            this(x0, x1);
        }

        public Map<UUID, byte[]> getPssh() {
            return this.mMapPssh;
        }

        public List<UUID> getSupportedSchemes() {
            return Arrays.asList(this.mSupportedSchemes);
        }

        private DrmInfoImpl(Map<UUID, byte[]> pssh, UUID[] supportedSchemes) {
            this.mMapPssh = pssh;
            this.mSupportedSchemes = supportedSchemes;
        }

        private DrmInfoImpl(Parcel parcel) {
            String str = MediaPlayer2Impl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DrmInfoImpl(");
            stringBuilder.append(parcel);
            stringBuilder.append(") size ");
            stringBuilder.append(parcel.dataSize());
            Log.v(str, stringBuilder.toString());
            int psshsize = parcel.readInt();
            byte[] pssh = new byte[psshsize];
            parcel.readByteArray(pssh);
            String str2 = MediaPlayer2Impl.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("DrmInfoImpl() PSSH: ");
            stringBuilder2.append(arrToHex(pssh));
            Log.v(str2, stringBuilder2.toString());
            this.mMapPssh = parsePSSH(pssh, psshsize);
            str2 = MediaPlayer2Impl.TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("DrmInfoImpl() PSSH: ");
            stringBuilder2.append(this.mMapPssh);
            Log.v(str2, stringBuilder2.toString());
            int supportedDRMsCount = parcel.readInt();
            this.mSupportedSchemes = new UUID[supportedDRMsCount];
            for (int i = 0; i < supportedDRMsCount; i++) {
                byte[] uuid = new byte[16];
                parcel.readByteArray(uuid);
                this.mSupportedSchemes[i] = bytesToUUID(uuid);
                String str3 = MediaPlayer2Impl.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("DrmInfoImpl() supportedScheme[");
                stringBuilder3.append(i);
                stringBuilder3.append("]: ");
                stringBuilder3.append(this.mSupportedSchemes[i]);
                Log.v(str3, stringBuilder3.toString());
            }
            String str4 = MediaPlayer2Impl.TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("DrmInfoImpl() Parcel psshsize: ");
            stringBuilder4.append(psshsize);
            stringBuilder4.append(" supportedDRMsCount: ");
            stringBuilder4.append(supportedDRMsCount);
            Log.v(str4, stringBuilder4.toString());
        }

        private DrmInfoImpl makeCopy() {
            return new DrmInfoImpl(this.mMapPssh, this.mSupportedSchemes);
        }

        private String arrToHex(byte[] bytes) {
            String out = "0x";
            for (int i = 0; i < bytes.length; i++) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(out);
                stringBuilder.append(String.format("%02x", new Object[]{Byte.valueOf(bytes[i])}));
                out = stringBuilder.toString();
            }
            return out;
        }

        private UUID bytesToUUID(byte[] uuid) {
            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb |= (((long) uuid[i]) & 255) << ((7 - i) * 8);
                lsb |= (((long) uuid[i + 8]) & 255) << (8 * (7 - i));
            }
            return new UUID(msb, lsb);
        }

        private Map<UUID, byte[]> parsePSSH(byte[] pssh, int psshsize) {
            byte[] bArr = pssh;
            Map<UUID, byte[]> result = new HashMap();
            int i = 0;
            int numentries = 0;
            int len = psshsize;
            int i2 = 0;
            while (len > 0) {
                if (len < 16) {
                    Log.w(MediaPlayer2Impl.TAG, String.format("parsePSSH: len is too short to parse UUID: (%d < 16) pssh: %d", new Object[]{Integer.valueOf(len), Integer.valueOf(psshsize)}));
                    return null;
                }
                UUID uuid = bytesToUUID(Arrays.copyOfRange(bArr, i2, i2 + 16));
                i2 += 16;
                len -= 16;
                if (len < 4) {
                    Log.w(MediaPlayer2Impl.TAG, String.format("parsePSSH: len is too short to parse datalen: (%d < 4) pssh: %d", new Object[]{Integer.valueOf(len), Integer.valueOf(psshsize)}));
                    return null;
                }
                int datalen;
                int i3;
                byte[] subset = Arrays.copyOfRange(bArr, i2, i2 + 4);
                if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                    datalen = (((subset[3] & 255) << 24) | ((subset[2] & 255) << 16)) | ((subset[1] & 255) << 8);
                    i3 = subset[0];
                } else {
                    datalen = (((subset[0] & 255) << 24) | ((subset[1] & 255) << 16)) | ((subset[2] & 255) << 8);
                    i3 = subset[3];
                }
                datalen |= i3 & 255;
                i2 += 4;
                len -= 4;
                if (len < datalen) {
                    Log.w(MediaPlayer2Impl.TAG, String.format("parsePSSH: len is too short to parse data: (%d < %d) pssh: %d", new Object[]{Integer.valueOf(len), Integer.valueOf(datalen), Integer.valueOf(psshsize)}));
                    return null;
                }
                byte[] data = Arrays.copyOfRange(bArr, i2, i2 + datalen);
                i2 += datalen;
                len -= datalen;
                Log.v(MediaPlayer2Impl.TAG, String.format("parsePSSH[%d]: <%s, %s> pssh: %d", new Object[]{Integer.valueOf(numentries), uuid, arrToHex(data), Integer.valueOf(psshsize)}));
                numentries++;
                result.put(uuid, data);
                i = 0;
            }
            return result;
        }
    }

    public static final class NoDrmSchemeExceptionImpl extends MediaPlayer2.NoDrmSchemeException {
        public NoDrmSchemeExceptionImpl(String detailMessage) {
            super(detailMessage);
        }
    }

    public static final class ProvisioningNetworkErrorExceptionImpl extends MediaPlayer2.ProvisioningNetworkErrorException {
        public ProvisioningNetworkErrorExceptionImpl(String detailMessage) {
            super(detailMessage);
        }
    }

    public static final class ProvisioningServerErrorExceptionImpl extends MediaPlayer2.ProvisioningServerErrorException {
        public ProvisioningServerErrorExceptionImpl(String detailMessage) {
            super(detailMessage);
        }
    }

    public static final class TrackInfoImpl extends MediaPlayer2.TrackInfo {
        final MediaFormat mFormat;
        final int mTrackType;

        public int getTrackType() {
            return this.mTrackType;
        }

        public String getLanguage() {
            String language = this.mFormat.getString("language");
            return language == null ? "und" : language;
        }

        public MediaFormat getFormat() {
            if (this.mTrackType == 3 || this.mTrackType == 4) {
                return this.mFormat;
            }
            return null;
        }

        TrackInfoImpl(int type, MediaFormat format) {
            this.mTrackType = type;
            this.mFormat = format;
        }

        public String toString() {
            StringBuilder out = new StringBuilder(128);
            out.append(getClass().getName());
            out.append('{');
            switch (this.mTrackType) {
                case 1:
                    out.append("VIDEO");
                    break;
                case 2:
                    out.append("AUDIO");
                    break;
                case 3:
                    out.append("TIMEDTEXT");
                    break;
                case 4:
                    out.append("SUBTITLE");
                    break;
                default:
                    out.append("UNKNOWN");
                    break;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(", ");
            stringBuilder.append(this.mFormat.toString());
            out.append(stringBuilder.toString());
            out.append("}");
            return out.toString();
        }
    }

    static {
        sInfoEventMap.put(Integer.valueOf(1), Integer.valueOf(1));
        sInfoEventMap.put(Integer.valueOf(2), Integer.valueOf(2));
        sInfoEventMap.put(Integer.valueOf(3), Integer.valueOf(3));
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_VIDEO_TRACK_LAGGING), Integer.valueOf(MediaPlayer2.MEDIA_INFO_VIDEO_TRACK_LAGGING));
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_BUFFERING_START), Integer.valueOf(MediaPlayer2.MEDIA_INFO_BUFFERING_START));
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_BUFFERING_END), Integer.valueOf(MediaPlayer2.MEDIA_INFO_BUFFERING_END));
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_BAD_INTERLEAVING), Integer.valueOf(MediaPlayer2.MEDIA_INFO_BAD_INTERLEAVING));
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_NOT_SEEKABLE), Integer.valueOf(MediaPlayer2.MEDIA_INFO_NOT_SEEKABLE));
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_METADATA_UPDATE), Integer.valueOf(MediaPlayer2.MEDIA_INFO_METADATA_UPDATE));
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_AUDIO_NOT_PLAYING), Integer.valueOf(MediaPlayer2.MEDIA_INFO_AUDIO_NOT_PLAYING));
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_VIDEO_NOT_PLAYING), Integer.valueOf(MediaPlayer2.MEDIA_INFO_VIDEO_NOT_PLAYING));
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_UNSUPPORTED_SUBTITLE), Integer.valueOf(MediaPlayer2.MEDIA_INFO_UNSUPPORTED_SUBTITLE));
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_SUBTITLE_TIMED_OUT), Integer.valueOf(MediaPlayer2.MEDIA_INFO_SUBTITLE_TIMED_OUT));
        sErrorEventMap.put(Integer.valueOf(1), Integer.valueOf(1));
        sErrorEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK), Integer.valueOf(MediaPlayer2.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK));
        sErrorEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_ERROR_IO), Integer.valueOf(MediaPlayer2.MEDIA_ERROR_IO));
        sErrorEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_ERROR_MALFORMED), Integer.valueOf(MediaPlayer2.MEDIA_ERROR_MALFORMED));
        sErrorEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_ERROR_UNSUPPORTED), Integer.valueOf(MediaPlayer2.MEDIA_ERROR_UNSUPPORTED));
        sErrorEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_ERROR_TIMED_OUT), Integer.valueOf(MediaPlayer2.MEDIA_ERROR_TIMED_OUT));
        sPrepareDrmStatusMap.put(Integer.valueOf(0), Integer.valueOf(0));
        sPrepareDrmStatusMap.put(Integer.valueOf(1), Integer.valueOf(1));
        sPrepareDrmStatusMap.put(Integer.valueOf(2), Integer.valueOf(2));
        sPrepareDrmStatusMap.put(Integer.valueOf(2), Integer.valueOf(2));
        sStateMap.put(Integer.valueOf(1001), Integer.valueOf(0));
        sStateMap.put(Integer.valueOf(1002), Integer.valueOf(1));
        sStateMap.put(Integer.valueOf(1003), Integer.valueOf(1));
        sStateMap.put(Integer.valueOf(1004), Integer.valueOf(2));
        sStateMap.put(Integer.valueOf(1005), Integer.valueOf(3));
    }

    private void handleDataSourceError(final DataSourceError err) {
        if (err != null) {
            notifyMediaPlayer2Event(new Mp2EventNotifier() {
                public void notify(EventCallback callback) {
                    callback.onError(MediaPlayer2Impl.this, err.mDSD, err.mWhat, err.mExtra);
                }
            });
        }
    }

    public MediaPlayer2Impl() {
        this.mHandlerThread.start();
        Looper looper = this.mHandlerThread.getLooper();
        this.mEndPositionHandler = new Handler(looper);
        this.mTaskHandler = new Handler(looper);
        this.mPlayer = new MediaPlayerSourceQueue();
    }

    public BaseMediaPlayer getBaseMediaPlayer() {
        BaseMediaPlayer baseMediaPlayer;
        synchronized (this.mLock) {
            if (this.mBaseMediaPlayerImpl == null) {
                this.mBaseMediaPlayerImpl = new BaseMediaPlayerImpl(this, null);
            }
            baseMediaPlayer = this.mBaseMediaPlayerImpl;
        }
        return baseMediaPlayer;
    }

    public void close() {
        this.mPlayer.release();
        if (this.mHandlerThread != null) {
            this.mHandlerThread.quitSafely();
            this.mHandlerThread = null;
        }
    }

    public void play() {
        addTask(new Task(5, false) {
            void process() {
                MediaPlayer2Impl.this.mPlayer.play();
            }
        });
    }

    public void prepare() {
        addTask(new Task(6, true) {
            void process() throws IOException {
                MediaPlayer2Impl.this.mPlayer.prepareAsync();
            }
        });
    }

    public void pause() {
        addTask(new Task(4, false) {
            void process() {
                MediaPlayer2Impl.this.mPlayer.pause();
            }
        });
    }

    public void skipToNext() {
        addTask(new Task(29, false) {
            void process() {
                MediaPlayer2Impl.this.mPlayer.skipToNext();
            }
        });
    }

    public long getCurrentPosition() {
        return this.mPlayer.getCurrentPosition();
    }

    public long getDuration() {
        return this.mPlayer.getDuration();
    }

    public long getBufferedPosition() {
        return this.mPlayer.getBufferedPosition();
    }

    public int getState() {
        return this.mPlayer.getMediaPlayer2State();
    }

    private int getPlayerState() {
        return this.mPlayer.getPlayerState();
    }

    private int getBufferingState() {
        return this.mPlayer.getBufferingState();
    }

    public void setAudioAttributes(@NonNull final AudioAttributesCompat attributes) {
        addTask(new Task(16, false) {
            void process() {
                MediaPlayer2Impl.this.mPlayer.setAudioAttributes(attributes);
            }
        });
    }

    @NonNull
    public AudioAttributesCompat getAudioAttributes() {
        return this.mPlayer.getAudioAttributes();
    }

    public void setDataSource(@NonNull final DataSourceDesc dsd) {
        addTask(new Task(19, false) {
            void process() {
                Preconditions.checkNotNull(dsd, "the DataSourceDesc cannot be null");
                try {
                    MediaPlayer2Impl.this.mPlayer.setFirst(dsd);
                } catch (IOException e) {
                    Log.e(MediaPlayer2Impl.TAG, "process: setDataSource", e);
                }
            }
        });
    }

    public void setNextDataSource(@NonNull final DataSourceDesc dsd) {
        addTask(new Task(22, false) {
            void process() {
                Preconditions.checkNotNull(dsd, "the DataSourceDesc cannot be null");
                MediaPlayer2Impl.this.handleDataSourceError(MediaPlayer2Impl.this.mPlayer.setNext(dsd));
            }
        });
    }

    public void setNextDataSources(@NonNull final List<DataSourceDesc> dsds) {
        addTask(new Task(23, false) {
            void process() {
                if (dsds == null || dsds.size() == 0) {
                    throw new IllegalArgumentException("data source list cannot be null or empty.");
                }
                for (DataSourceDesc dsd : dsds) {
                    if (dsd == null) {
                        throw new IllegalArgumentException("DataSourceDesc in the source list cannot be null.");
                    }
                }
                MediaPlayer2Impl.this.handleDataSourceError(MediaPlayer2Impl.this.mPlayer.setNextMultiple(dsds));
            }
        });
    }

    @NonNull
    public DataSourceDesc getCurrentDataSource() {
        return this.mPlayer.getFirst().getDSD();
    }

    public void loopCurrent(final boolean loop) {
        addTask(new Task(3, false) {
            void process() {
                MediaPlayer2Impl.this.mPlayer.setLooping(loop);
            }
        });
    }

    public void setPlayerVolume(final float volume) {
        addTask(new Task(26, false) {
            void process() {
                MediaPlayer2Impl.this.mPlayer.setVolume(volume);
            }
        });
    }

    public float getPlayerVolume() {
        return this.mPlayer.getVolume();
    }

    public float getMaxPlayerVolume() {
        return 1.0f;
    }

    private void registerPlayerEventCallback(@NonNull Executor e, @NonNull PlayerEventCallback cb) {
        if (cb == null) {
            throw new IllegalArgumentException("Illegal null PlayerEventCallback");
        } else if (e != null) {
            synchronized (this.mLock) {
                this.mPlayerEventCallbackMap.put(cb, e);
            }
        } else {
            throw new IllegalArgumentException("Illegal null Executor for the PlayerEventCallback");
        }
    }

    private void unregisterPlayerEventCallback(@NonNull PlayerEventCallback cb) {
        if (cb != null) {
            synchronized (this.mLock) {
                this.mPlayerEventCallbackMap.remove(cb);
            }
            return;
        }
        throw new IllegalArgumentException("Illegal null PlayerEventCallback");
    }

    public void notifyWhenCommandLabelReached(final Object label) {
        addTask(new Task(1003, false) {
            void process() {
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    public void notify(EventCallback cb) {
                        cb.onCommandLabelReached(MediaPlayer2Impl.this, label);
                    }
                });
            }
        });
    }

    public void setSurface(final Surface surface) {
        addTask(new Task(27, false) {
            void process() {
                MediaPlayer2Impl.this.mPlayer.setSurface(surface);
            }
        });
    }

    public void clearPendingCommands() {
        synchronized (this.mTaskLock) {
            this.mPendingTasks.clear();
        }
    }

    private void addTask(Task task) {
        synchronized (this.mTaskLock) {
            this.mPendingTasks.add(task);
            processPendingTask_l();
        }
    }

    @GuardedBy("mTaskLock")
    private void processPendingTask_l() {
        if (this.mCurrentTask == null && !this.mPendingTasks.isEmpty()) {
            Task task = (Task) this.mPendingTasks.removeFirst();
            this.mCurrentTask = task;
            this.mTaskHandler.post(task);
        }
    }

    private static void handleDataSource(MediaPlayerSource src) throws IOException {
        final DataSourceDesc dsd = src.getDSD();
        Preconditions.checkNotNull(dsd, "the DataSourceDesc cannot be null");
        MediaPlayer player = src.mPlayer;
        switch (dsd.getType()) {
            case 1:
                player.setDataSource(new MediaDataSource() {
                    Media2DataSource mDataSource = dsd.getMedia2DataSource();

                    public int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
                        return this.mDataSource.readAt(position, buffer, offset, size);
                    }

                    public long getSize() throws IOException {
                        return this.mDataSource.getSize();
                    }

                    public void close() throws IOException {
                        this.mDataSource.close();
                    }
                });
                return;
            case 2:
                player.setDataSource(dsd.getFileDescriptor(), dsd.getFileDescriptorOffset(), dsd.getFileDescriptorLength());
                return;
            case 3:
                player.setDataSource(dsd.getUriContext(), dsd.getUri(), dsd.getUriHeaders(), dsd.getUriCookies());
                return;
            default:
                return;
        }
    }

    public int getVideoWidth() {
        return this.mPlayer.getVideoWidth();
    }

    public int getVideoHeight() {
        return this.mPlayer.getVideoHeight();
    }

    public PersistableBundle getMetrics() {
        return this.mPlayer.getMetrics();
    }

    public void setPlaybackParams(@NonNull final PlaybackParams2 params) {
        addTask(new Task(24, false) {
            void process() {
                MediaPlayer2Impl.this.setPlaybackParamsInternal(params.getPlaybackParams());
            }
        });
    }

    @NonNull
    public PlaybackParams2 getPlaybackParams() {
        return new Builder(this.mPlayer.getPlaybackParams()).build();
    }

    public void seekTo(long msec, int mode) {
        final long j = msec;
        final int i = mode;
        addTask(new Task(14, true) {
            void process() {
                MediaPlayer2Impl.this.mPlayer.seekTo(j, i);
            }
        });
    }

    @Nullable
    public MediaTimestamp2 getTimestamp() {
        return this.mPlayer.getTimestamp();
    }

    public void reset() {
        this.mPlayer.reset();
        synchronized (this.mLock) {
            this.mMp2EventCallbackRecord = null;
            this.mPlayerEventCallbackMap.clear();
            this.mDrmEventCallbackRecord = null;
        }
    }

    public void setAudioSessionId(final int sessionId) {
        addTask(new Task(17, false) {
            void process() {
                MediaPlayer2Impl.this.mPlayer.setAudioSessionId(sessionId);
            }
        });
    }

    public int getAudioSessionId() {
        return this.mPlayer.getAudioSessionId();
    }

    public void attachAuxEffect(final int effectId) {
        addTask(new Task(1, false) {
            void process() {
                MediaPlayer2Impl.this.mPlayer.attachAuxEffect(effectId);
            }
        });
    }

    public void setAuxEffectSendLevel(final float level) {
        addTask(new Task(18, false) {
            void process() {
                MediaPlayer2Impl.this.mPlayer.setAuxEffectSendLevel(level);
            }
        });
    }

    public List<MediaPlayer2.TrackInfo> getTrackInfo() {
        TrackInfo[] list = this.mPlayer.getTrackInfo();
        List<MediaPlayer2.TrackInfo> trackList = new ArrayList();
        for (TrackInfo info : list) {
            trackList.add(new TrackInfoImpl(info.getTrackType(), info.getFormat()));
        }
        return trackList;
    }

    public int getSelectedTrack(int trackType) {
        return this.mPlayer.getSelectedTrack(trackType);
    }

    public void selectTrack(final int index) {
        addTask(new Task(15, false) {
            void process() {
                MediaPlayer2Impl.this.mPlayer.selectTrack(index);
            }
        });
    }

    public void deselectTrack(final int index) {
        addTask(new Task(2, false) {
            void process() {
                MediaPlayer2Impl.this.mPlayer.deselectTrack(index);
            }
        });
    }

    public void setEventCallback(@NonNull Executor executor, @NonNull EventCallback eventCallback) {
        if (eventCallback == null) {
            throw new IllegalArgumentException("Illegal null EventCallback");
        } else if (executor != null) {
            synchronized (this.mLock) {
                this.mMp2EventCallbackRecord = new Pair(executor, eventCallback);
            }
        } else {
            throw new IllegalArgumentException("Illegal null Executor for the EventCallback");
        }
    }

    public void clearEventCallback() {
        synchronized (this.mLock) {
            this.mMp2EventCallbackRecord = null;
        }
    }

    public void setOnDrmConfigHelper(final MediaPlayer2.OnDrmConfigHelper listener) {
        this.mPlayer.setOnDrmConfigHelper(new OnDrmConfigHelper() {
            public void onDrmConfig(MediaPlayer mp) {
                MediaPlayerSource src = MediaPlayer2Impl.this.mPlayer.getSourceForPlayer(mp);
                listener.onDrmConfig(MediaPlayer2Impl.this, src == null ? null : src.getDSD());
            }
        });
    }

    public void setDrmEventCallback(@NonNull Executor executor, @NonNull DrmEventCallback eventCallback) {
        if (eventCallback == null) {
            throw new IllegalArgumentException("Illegal null EventCallback");
        } else if (executor != null) {
            synchronized (this.mLock) {
                this.mDrmEventCallbackRecord = new Pair(executor, eventCallback);
            }
        } else {
            throw new IllegalArgumentException("Illegal null Executor for the EventCallback");
        }
    }

    public void clearDrmEventCallback() {
        synchronized (this.mLock) {
            this.mDrmEventCallbackRecord = null;
        }
    }

    public MediaPlayer2.DrmInfo getDrmInfo() {
        DrmInfo info = this.mPlayer.getDrmInfo();
        return info == null ? null : new DrmInfoImpl(info.getPssh(), info.getSupportedSchemes(), null);
    }

    public void prepareDrm(@NonNull UUID uuid) throws UnsupportedSchemeException, ResourceBusyException, MediaPlayer2.ProvisioningNetworkErrorException, MediaPlayer2.ProvisioningServerErrorException {
        try {
            this.mPlayer.prepareDrm(uuid);
        } catch (ProvisioningNetworkErrorException e) {
            throw new MediaPlayer2.ProvisioningNetworkErrorException(e.getMessage());
        } catch (ProvisioningServerErrorException e2) {
            throw new MediaPlayer2.ProvisioningServerErrorException(e2.getMessage());
        }
    }

    public void releaseDrm() throws MediaPlayer2.NoDrmSchemeException {
        try {
            this.mPlayer.releaseDrm();
        } catch (NoDrmSchemeException e) {
            throw new MediaPlayer2.NoDrmSchemeException(e.getMessage());
        }
    }

    @NonNull
    public KeyRequest getDrmKeyRequest(@Nullable byte[] keySetId, @Nullable byte[] initData, @Nullable String mimeType, int keyType, @Nullable Map<String, String> optionalParameters) throws MediaPlayer2.NoDrmSchemeException {
        try {
            return this.mPlayer.getKeyRequest(keySetId, initData, mimeType, keyType, optionalParameters);
        } catch (NoDrmSchemeException e) {
            throw new MediaPlayer2.NoDrmSchemeException(e.getMessage());
        }
    }

    public byte[] provideDrmKeyResponse(@Nullable byte[] keySetId, @NonNull byte[] response) throws MediaPlayer2.NoDrmSchemeException, DeniedByServerException {
        try {
            return this.mPlayer.provideKeyResponse(keySetId, response);
        } catch (NoDrmSchemeException e) {
            throw new MediaPlayer2.NoDrmSchemeException(e.getMessage());
        }
    }

    public void restoreDrmKeys(@NonNull byte[] keySetId) throws MediaPlayer2.NoDrmSchemeException {
        try {
            this.mPlayer.restoreKeys(keySetId);
        } catch (NoDrmSchemeException e) {
            throw new MediaPlayer2.NoDrmSchemeException(e.getMessage());
        }
    }

    @NonNull
    public String getDrmPropertyString(@NonNull String propertyName) throws MediaPlayer2.NoDrmSchemeException {
        try {
            return this.mPlayer.getDrmPropertyString(propertyName);
        } catch (NoDrmSchemeException e) {
            throw new MediaPlayer2.NoDrmSchemeException(e.getMessage());
        }
    }

    public void setDrmPropertyString(@NonNull String propertyName, @NonNull String value) throws MediaPlayer2.NoDrmSchemeException {
        try {
            this.mPlayer.setDrmPropertyString(propertyName, value);
        } catch (NoDrmSchemeException e) {
            throw new MediaPlayer2.NoDrmSchemeException(e.getMessage());
        }
    }

    private void setPlaybackParamsInternal(final PlaybackParams params) {
        PlaybackParams current = this.mPlayer.getPlaybackParams();
        this.mPlayer.setPlaybackParams(params);
        if (current.getSpeed() != params.getSpeed()) {
            notifyPlayerEvent(new PlayerEventNotifier() {
                public void notify(PlayerEventCallback cb) {
                    cb.onPlaybackSpeedChanged(MediaPlayer2Impl.this.mBaseMediaPlayerImpl, params.getSpeed());
                }
            });
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code:
            if (r1 == null) goto L_?;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code:
            ((java.util.concurrent.Executor) r1.first).execute(new android.support.v4.media.MediaPlayer2Impl.AnonymousClass24(r3));
     */
    /* JADX WARNING: Missing block: B:14:?, code:
            return;
     */
    /* JADX WARNING: Missing block: B:15:?, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void notifyMediaPlayer2Event(final Mp2EventNotifier notifier) {
        Throwable th;
        synchronized (this.mLock) {
            try {
                final Pair<Executor, EventCallback> record = this.mMp2EventCallbackRecord;
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

    private void notifyPlayerEvent(final PlayerEventNotifier notifier) {
        ArrayMap<PlayerEventCallback, Executor> map;
        synchronized (this.mLock) {
            map = new ArrayMap(this.mPlayerEventCallbackMap);
        }
        int callbackCount = map.size();
        for (int i = 0; i < callbackCount; i++) {
            final PlayerEventCallback cb = (PlayerEventCallback) map.keyAt(i);
            ((Executor) map.valueAt(i)).execute(new Runnable() {
                public void run() {
                    notifier.notify(cb);
                }
            });
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code:
            if (r1 == null) goto L_?;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code:
            ((java.util.concurrent.Executor) r1.first).execute(new android.support.v4.media.MediaPlayer2Impl.AnonymousClass26(r3));
     */
    /* JADX WARNING: Missing block: B:14:?, code:
            return;
     */
    /* JADX WARNING: Missing block: B:15:?, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void notifyDrmEvent(final DrmEventNotifier notifier) {
        Throwable th;
        synchronized (this.mLock) {
            try {
                final Pair<Executor, DrmEventCallback> record = this.mDrmEventCallbackRecord;
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

    private void setEndPositionTimerIfNeeded(OnCompletionListener completionListener, MediaPlayerSource src, MediaTimestamp timedsd) {
        final OnCompletionListener onCompletionListener;
        final MediaPlayerSource mediaPlayerSource = src;
        if (mediaPlayerSource == this.mPlayer.getFirst()) {
            this.mEndPositionHandler.removeCallbacksAndMessages(null);
            DataSourceDesc dsd = src.getDSD();
            if (dsd.getEndPosition() != 576460752303423487L && timedsd.getMediaClockRate() > 0.0f) {
                long timeLeftMs = (long) (((float) (dsd.getEndPosition() - ((timedsd.getAnchorMediaTimeUs() + ((System.nanoTime() - timedsd.getAnchorSytemNanoTime()) / 1000)) / 1000))) / timedsd.getMediaClockRate());
                Handler handler = this.mEndPositionHandler;
                onCompletionListener = completionListener;
                Runnable anonymousClass27 = new Runnable() {
                    public void run() {
                        if (MediaPlayer2Impl.this.mPlayer.getFirst() == mediaPlayerSource) {
                            MediaPlayer2Impl.this.mPlayer.pause();
                            onCompletionListener.onCompletion(mediaPlayerSource.mPlayer);
                        }
                    }
                };
                long j = 0;
                if (timeLeftMs >= 0) {
                    j = timeLeftMs;
                }
                handler.postDelayed(anonymousClass27, j);
                return;
            }
        }
        onCompletionListener = completionListener;
    }

    private void setUpListeners(final MediaPlayerSource src) {
        MediaPlayer p = src.mPlayer;
        final OnPreparedListener preparedListener = new OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                MediaPlayer2Impl.this.handleDataSourceError(MediaPlayer2Impl.this.mPlayer.onPrepared(mp));
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    public void notify(EventCallback callback) {
                        callback.onInfo(MediaPlayer2Impl.this, src.getDSD(), 100, 0);
                    }
                });
                MediaPlayer2Impl.this.notifyPlayerEvent(new PlayerEventNotifier() {
                    public void notify(PlayerEventCallback cb) {
                        cb.onMediaPrepared(MediaPlayer2Impl.this.mBaseMediaPlayerImpl, src.getDSD());
                    }
                });
                synchronized (MediaPlayer2Impl.this.mTaskLock) {
                    if (MediaPlayer2Impl.this.mCurrentTask != null && MediaPlayer2Impl.this.mCurrentTask.mMediaCallType == 6 && MediaPlayer2Impl.this.mCurrentTask.mDSD == src.getDSD() && MediaPlayer2Impl.this.mCurrentTask.mNeedToWaitForEventToComplete) {
                        MediaPlayer2Impl.this.mCurrentTask.sendCompleteNotification(0);
                        MediaPlayer2Impl.this.mCurrentTask = null;
                        MediaPlayer2Impl.this.processPendingTask_l();
                    }
                }
            }
        };
        p.setOnPreparedListener(new OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                if (src.getDSD().getStartPosition() != 0) {
                    src.mPlayer.seekTo((long) ((int) src.getDSD().getStartPosition()), 3);
                } else {
                    preparedListener.onPrepared(mp);
                }
            }
        });
        p.setOnVideoSizeChangedListener(new OnVideoSizeChangedListener() {
            public void onVideoSizeChanged(MediaPlayer mp, final int width, final int height) {
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    public void notify(EventCallback cb) {
                        cb.onVideoSizeChanged(MediaPlayer2Impl.this, src.getDSD(), width, height);
                    }
                });
            }
        });
        p.setOnInfoListener(new OnInfoListener() {
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                if (what != 3) {
                    switch (what) {
                        case MediaPlayer2.MEDIA_INFO_BUFFERING_START /*701*/:
                            MediaPlayer2Impl.this.mPlayer.setBufferingState(mp, 2);
                            break;
                        case MediaPlayer2.MEDIA_INFO_BUFFERING_END /*702*/:
                            MediaPlayer2Impl.this.mPlayer.setBufferingState(mp, 1);
                            break;
                    }
                }
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    public void notify(EventCallback cb) {
                        cb.onInfo(MediaPlayer2Impl.this, src.getDSD(), 3, 0);
                    }
                });
                return false;
            }
        });
        final OnCompletionListener completionListener = new OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                MediaPlayer2Impl.this.handleDataSourceError(MediaPlayer2Impl.this.mPlayer.onCompletion(mp));
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    public void notify(EventCallback cb) {
                        cb.onInfo(MediaPlayer2Impl.this, src.getDSD(), 5, 0);
                    }
                });
            }
        };
        p.setOnCompletionListener(completionListener);
        p.setOnErrorListener(new OnErrorListener() {
            public boolean onError(MediaPlayer mp, final int what, final int extra) {
                MediaPlayer2Impl.this.mPlayer.onError(mp);
                synchronized (MediaPlayer2Impl.this.mTaskLock) {
                    if (MediaPlayer2Impl.this.mCurrentTask != null && MediaPlayer2Impl.this.mCurrentTask.mNeedToWaitForEventToComplete) {
                        MediaPlayer2Impl.this.mCurrentTask.sendCompleteNotification(Integer.MIN_VALUE);
                        MediaPlayer2Impl.this.mCurrentTask = null;
                        MediaPlayer2Impl.this.processPendingTask_l();
                    }
                }
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    public void notify(EventCallback cb) {
                        cb.onError(MediaPlayer2Impl.this, src.getDSD(), ((Integer) MediaPlayer2Impl.sErrorEventMap.getOrDefault(Integer.valueOf(what), Integer.valueOf(1))).intValue(), extra);
                    }
                });
                return true;
            }
        });
        p.setOnSeekCompleteListener(new OnSeekCompleteListener() {
            public void onSeekComplete(MediaPlayer mp) {
                if (src.mMp2State != 1001 || src.getDSD().getStartPosition() == 0) {
                    synchronized (MediaPlayer2Impl.this.mTaskLock) {
                        if (MediaPlayer2Impl.this.mCurrentTask != null && MediaPlayer2Impl.this.mCurrentTask.mMediaCallType == 14 && MediaPlayer2Impl.this.mCurrentTask.mNeedToWaitForEventToComplete) {
                            MediaPlayer2Impl.this.mCurrentTask.sendCompleteNotification(0);
                            MediaPlayer2Impl.this.mCurrentTask = null;
                            MediaPlayer2Impl.this.processPendingTask_l();
                        }
                    }
                    final long seekPos = MediaPlayer2Impl.this.getCurrentPosition();
                    MediaPlayer2Impl.this.notifyPlayerEvent(new PlayerEventNotifier() {
                        public void notify(PlayerEventCallback cb) {
                            cb.onSeekCompleted(MediaPlayer2Impl.this.mBaseMediaPlayerImpl, seekPos);
                        }
                    });
                    return;
                }
                preparedListener.onPrepared(mp);
            }
        });
        p.setOnTimedMetaDataAvailableListener(new OnTimedMetaDataAvailableListener() {
            public void onTimedMetaDataAvailable(MediaPlayer mp, final TimedMetaData data) {
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    public void notify(EventCallback cb) {
                        cb.onTimedMetaDataAvailable(MediaPlayer2Impl.this, src.getDSD(), data);
                    }
                });
            }
        });
        p.setOnInfoListener(new OnInfoListener() {
            public boolean onInfo(MediaPlayer mp, final int what, final int extra) {
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    public void notify(EventCallback cb) {
                        cb.onInfo(MediaPlayer2Impl.this, src.getDSD(), ((Integer) MediaPlayer2Impl.sInfoEventMap.getOrDefault(Integer.valueOf(what), Integer.valueOf(1))).intValue(), extra);
                    }
                });
                return true;
            }
        });
        p.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {
            public void onBufferingUpdate(MediaPlayer mp, final int percent) {
                if (percent >= 100) {
                    MediaPlayer2Impl.this.mPlayer.setBufferingState(mp, 3);
                }
                src.mBufferedPercentage.set(percent);
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    public void notify(EventCallback cb) {
                        cb.onInfo(MediaPlayer2Impl.this, src.getDSD(), MediaPlayer2.MEDIA_INFO_BUFFERING_UPDATE, percent);
                    }
                });
            }
        });
        p.setOnMediaTimeDiscontinuityListener(new OnMediaTimeDiscontinuityListener() {
            public void onMediaTimeDiscontinuity(MediaPlayer mp, final MediaTimestamp timestamp) {
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    public void notify(EventCallback cb) {
                        cb.onMediaTimeDiscontinuity(MediaPlayer2Impl.this, src.getDSD(), new MediaTimestamp2(timestamp));
                    }
                });
                MediaPlayer2Impl.this.setEndPositionTimerIfNeeded(completionListener, src, timestamp);
            }
        });
        p.setOnSubtitleDataListener(new OnSubtitleDataListener() {
            public void onSubtitleData(MediaPlayer mp, final SubtitleData data) {
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    public void notify(EventCallback cb) {
                        cb.onSubtitleData(MediaPlayer2Impl.this, src.getDSD(), new SubtitleData2(data));
                    }
                });
            }
        });
        p.setOnDrmInfoListener(new OnDrmInfoListener() {
            public void onDrmInfo(MediaPlayer mp, final DrmInfo drmInfo) {
                MediaPlayer2Impl.this.notifyDrmEvent(new DrmEventNotifier() {
                    public void notify(DrmEventCallback cb) {
                        cb.onDrmInfo(MediaPlayer2Impl.this, src.getDSD(), new DrmInfoImpl(drmInfo.getPssh(), drmInfo.getSupportedSchemes(), null));
                    }
                });
            }
        });
        p.setOnDrmPreparedListener(new OnDrmPreparedListener() {
            public void onDrmPrepared(MediaPlayer mp, final int status) {
                MediaPlayer2Impl.this.notifyDrmEvent(new DrmEventNotifier() {
                    public void notify(DrmEventCallback cb) {
                        cb.onDrmPrepared(MediaPlayer2Impl.this, src.getDSD(), ((Integer) MediaPlayer2Impl.sPrepareDrmStatusMap.getOrDefault(Integer.valueOf(status), Integer.valueOf(3))).intValue());
                    }
                });
            }
        });
    }
}
