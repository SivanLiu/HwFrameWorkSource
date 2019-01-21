package android.media;

import android.app.ActivityThread;
import android.app.backup.FullBackup;
import android.common.HwFrameworkFactory;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.hsm.MediaTransactWrapper;
import android.media.AudioAttributes.Builder;
import android.media.AudioRouting.OnRoutingChangedListener;
import android.media.MediaDrm.KeyRequest;
import android.media.MediaDrm.ProvisionRequest;
import android.media.MediaTimeProvider.OnMediaTimeListener;
import android.media.SubtitleController.Anchor;
import android.media.SubtitleController.Listener;
import android.media.SubtitleTrack.RenderingWidget;
import android.media.VolumeShaper.Configuration;
import android.media.VolumeShaper.Operation;
import android.media.VolumeShaper.State;
import android.net.Uri;
import android.net.booster.IHwCommBoosterCallback;
import android.net.booster.IHwCommBoosterCallback.Stub;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;
import android.view.SurfaceHolder;
import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import libcore.io.IoBridge;
import libcore.io.Streams;

public class MediaPlayer extends PlayerBase implements Listener, VolumeAutomation, AudioRouting {
    public static final boolean APPLY_METADATA_FILTER = true;
    private static final boolean BOOSTER_SUPPORT = SystemProperties.getBoolean("ro.config.hw_booster", false);
    public static final boolean BYPASS_METADATA_FILTER = false;
    private static final boolean HISI_VIDEO_ACC_FUNC = SystemProperties.getBoolean("ro.config.hisi_video_acc", false);
    private static final String IMEDIA_PLAYER = "android.media.IMediaPlayer";
    private static final int INVOKE_ID_ADD_EXTERNAL_SOURCE = 2;
    private static final int INVOKE_ID_ADD_EXTERNAL_SOURCE_FD = 3;
    private static final int INVOKE_ID_DESELECT_TRACK = 5;
    private static final int INVOKE_ID_GET_SELECTED_TRACK = 7;
    private static final int INVOKE_ID_GET_TRACK_INFO = 1;
    private static final int INVOKE_ID_SELECT_TRACK = 4;
    private static final int INVOKE_ID_SET_VIDEO_SCALE_MODE = 6;
    private static final int KEY_PARAMETER_AUDIO_ATTRIBUTES = 1400;
    private static final int MEDIA_AUDIO_ROUTING_CHANGED = 10000;
    private static final int MEDIA_BUFFERING_UPDATE = 3;
    private static final int MEDIA_CALLBACK_DATATYPE = 1;
    private static final int MEDIA_DRM_INFO = 210;
    private static final int MEDIA_ERROR = 100;
    public static final int MEDIA_ERROR_IO = -1004;
    public static final int MEDIA_ERROR_MALFORMED = -1007;
    public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;
    public static final int MEDIA_ERROR_SERVER_DIED = 100;
    public static final int MEDIA_ERROR_SYSTEM = Integer.MIN_VALUE;
    public static final int MEDIA_ERROR_TIMED_OUT = -110;
    public static final int MEDIA_ERROR_UNKNOWN = 1;
    public static final int MEDIA_ERROR_UNSUPPORTED = -1010;
    private static final int MEDIA_INFO = 200;
    public static final int MEDIA_INFO_AUDIO_NOT_PLAYING = 804;
    public static final int MEDIA_INFO_BAD_INTERLEAVING = 800;
    public static final int MEDIA_INFO_BUFFERING_END = 702;
    public static final int MEDIA_INFO_BUFFERING_START = 701;
    public static final int MEDIA_INFO_EXTERNAL_METADATA_UPDATE = 803;
    public static final int MEDIA_INFO_METADATA_UPDATE = 802;
    public static final int MEDIA_INFO_NETWORK_BANDWIDTH = 703;
    public static final int MEDIA_INFO_NOT_SEEKABLE = 801;
    public static final int MEDIA_INFO_STARTED_AS_NEXT = 2;
    public static final int MEDIA_INFO_SUBTITLE_TIMED_OUT = 902;
    public static final int MEDIA_INFO_TIMED_TEXT_ERROR = 900;
    public static final int MEDIA_INFO_UNKNOWN = 1;
    public static final int MEDIA_INFO_UNSUPPORTED_SUBTITLE = 901;
    public static final int MEDIA_INFO_VIDEO_NOT_PLAYING = 805;
    public static final int MEDIA_INFO_VIDEO_RENDERING_START = 3;
    public static final int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;
    private static final int MEDIA_META_DATA = 202;
    public static final String MEDIA_MIMETYPE_TEXT_CEA_608 = "text/cea-608";
    public static final String MEDIA_MIMETYPE_TEXT_CEA_708 = "text/cea-708";
    public static final String MEDIA_MIMETYPE_TEXT_SUBRIP = "application/x-subrip";
    public static final String MEDIA_MIMETYPE_TEXT_VTT = "text/vtt";
    private static final int MEDIA_NOP = 0;
    private static final int MEDIA_NOTIFY_TIME = 98;
    private static final int MEDIA_PAUSED = 7;
    private static final int MEDIA_PLAYBACK_COMPLETE = 2;
    private static final int MEDIA_PREPARED = 1;
    private static final int MEDIA_REPORT_DATATYPE_SLICEINFO = 202;
    private static final int MEDIA_REPORT_DATATYPE_STATUS = 201;
    private static final String MEDIA_REPORT_PKG_NAME = "android.media";
    private static final boolean MEDIA_REPORT_PROP;
    private static final int MEDIA_REPORT_STATUS_END = 4;
    private static final int MEDIA_REPORT_STATUS_PAUSE = 2;
    private static final int MEDIA_REPORT_STATUS_PREPARE = 0;
    private static final int MEDIA_REPORT_STATUS_START = 1;
    private static final int MEDIA_REPORT_SWITCH_OFF = 0;
    private static final int MEDIA_REPORT_SWITCH_ON = 1;
    private static final int MEDIA_REPORT_VIDEO_PROTOCOL_HLS = 0;
    private static final int MEDIA_SEEK_COMPLETE = 4;
    private static final int MEDIA_SET_VIDEO_SIZE = 5;
    private static final int MEDIA_SKIPPED = 9;
    private static final int MEDIA_STARTED = 6;
    private static final int MEDIA_STOPPED = 8;
    private static final int MEDIA_SUBTITLE_DATA = 201;
    private static final int MEDIA_TIMED_TEXT = 99;
    private static final int MEDIA_TIME_DISCONTINUITY = 211;
    private static final int MEDIA_UPDATE_METADATA = 250;
    public static final boolean METADATA_ALL = false;
    public static final boolean METADATA_UPDATE_ONLY = true;
    public static final int PLAYBACK_RATE_AUDIO_MODE_DEFAULT = 0;
    public static final int PLAYBACK_RATE_AUDIO_MODE_RESAMPLE = 2;
    public static final int PLAYBACK_RATE_AUDIO_MODE_STRETCH = 1;
    public static final int PREPARE_DRM_STATUS_PREPARATION_ERROR = 3;
    public static final int PREPARE_DRM_STATUS_PROVISIONING_NETWORK_ERROR = 1;
    public static final int PREPARE_DRM_STATUS_PROVISIONING_SERVER_ERROR = 2;
    public static final int PREPARE_DRM_STATUS_SUCCESS = 0;
    public static final int SEEK_CLOSEST = 3;
    public static final int SEEK_CLOSEST_SYNC = 2;
    public static final int SEEK_NEXT_SYNC = 1;
    public static final int SEEK_PREVIOUS_SYNC = 0;
    private static final String TAG = "MediaPlayer";
    public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT = 1;
    public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING = 2;
    private boolean mActiveDrmScheme;
    private boolean mBypassInterruptionPolicy;
    private boolean mDrmConfigAllowed;
    private DrmInfo mDrmInfo;
    private boolean mDrmInfoResolved;
    private final Object mDrmLock = new Object();
    private MediaDrm mDrmObj;
    private boolean mDrmProvisioningInProgress;
    private ProvisioningThread mDrmProvisioningThread;
    private byte[] mDrmSessionId;
    private UUID mDrmUUID;
    private EventHandler mEventHandler;
    private Handler mExtSubtitleDataHandler;
    private OnSubtitleDataListener mExtSubtitleDataListener;
    private IHwCommBoosterCallback mIHwCommBoosterCallBack = null;
    private BitSet mInbandTrackIndices = new BitSet();
    private Vector<Pair<Integer, SubtitleTrack>> mIndexTrackPairs = new Vector();
    private final OnSubtitleDataListener mIntSubtitleDataListener = new OnSubtitleDataListener() {
        public void onSubtitleData(MediaPlayer mp, SubtitleData data) {
            int index = data.getTrackIndex();
            synchronized (MediaPlayer.this.mIndexTrackPairs) {
                Iterator it = MediaPlayer.this.mIndexTrackPairs.iterator();
                while (it.hasNext()) {
                    Pair<Integer, SubtitleTrack> p = (Pair) it.next();
                    if (!(p.first == null || ((Integer) p.first).intValue() != index || p.second == null)) {
                        p.second.onData(data);
                    }
                }
            }
        }
    };
    private boolean mIsHLS = false;
    private int mListenerContext;
    private boolean mMediaReportRegister = false;
    private boolean mMediaReportSwitch = false;
    private long mNativeContext;
    private long mNativeSurfaceTexture;
    private OnBufferingUpdateListener mOnBufferingUpdateListener;
    private final OnCompletionListener mOnCompletionInternalListener = new OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            MediaPlayer.this.baseStop();
        }
    };
    private OnCompletionListener mOnCompletionListener;
    private OnDrmConfigHelper mOnDrmConfigHelper;
    private OnDrmInfoHandlerDelegate mOnDrmInfoHandlerDelegate;
    private OnDrmPreparedHandlerDelegate mOnDrmPreparedHandlerDelegate;
    private OnErrorListener mOnErrorListener;
    private OnInfoListener mOnInfoListener;
    private Handler mOnMediaTimeDiscontinuityHandler;
    private OnMediaTimeDiscontinuityListener mOnMediaTimeDiscontinuityListener;
    private OnPreparedListener mOnPreparedListener;
    private OnSeekCompleteListener mOnSeekCompleteListener;
    private OnTimedMetaDataAvailableListener mOnTimedMetaDataAvailableListener;
    private OnTimedTextListener mOnTimedTextListener;
    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private Vector<InputStream> mOpenSubtitleSources;
    private AudioDeviceInfo mPreferredDevice = null;
    private boolean mPrepareDrmInProgress;
    @GuardedBy("mRoutingChangeListeners")
    private ArrayMap<OnRoutingChangedListener, NativeRoutingEventHandlerDelegate> mRoutingChangeListeners = new ArrayMap();
    private boolean mScreenOnWhilePlaying;
    private int mSelectedSubtitleTrackIndex = -1;
    private boolean mStayAwake;
    private int mStreamType = Integer.MIN_VALUE;
    private SubtitleController mSubtitleController;
    private boolean mSubtitleDataListenerDisabled;
    private SurfaceHolder mSurfaceHolder;
    private TimeProvider mTimeProvider;
    private int mUsage = -1;
    private WakeLock mWakeLock = null;

    public static final class DrmInfo {
        private Map<UUID, byte[]> mapPssh;
        private UUID[] supportedSchemes;

        public Map<UUID, byte[]> getPssh() {
            return this.mapPssh;
        }

        public UUID[] getSupportedSchemes() {
            return this.supportedSchemes;
        }

        private DrmInfo(Map<UUID, byte[]> Pssh, UUID[] SupportedSchemes) {
            this.mapPssh = Pssh;
            this.supportedSchemes = SupportedSchemes;
        }

        private DrmInfo(Parcel parcel) {
            String str = MediaPlayer.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DrmInfo(");
            stringBuilder.append(parcel);
            stringBuilder.append(") size ");
            stringBuilder.append(parcel.dataSize());
            Log.v(str, stringBuilder.toString());
            int psshsize = parcel.readInt();
            byte[] pssh = new byte[psshsize];
            parcel.readByteArray(pssh);
            String str2 = MediaPlayer.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("DrmInfo() PSSH: ");
            stringBuilder2.append(arrToHex(pssh));
            Log.v(str2, stringBuilder2.toString());
            this.mapPssh = parsePSSH(pssh, psshsize);
            str2 = MediaPlayer.TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("DrmInfo() PSSH: ");
            stringBuilder2.append(this.mapPssh);
            Log.v(str2, stringBuilder2.toString());
            int supportedDRMsCount = parcel.readInt();
            this.supportedSchemes = new UUID[supportedDRMsCount];
            for (int i = 0; i < supportedDRMsCount; i++) {
                byte[] uuid = new byte[16];
                parcel.readByteArray(uuid);
                this.supportedSchemes[i] = bytesToUUID(uuid);
                String str3 = MediaPlayer.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("DrmInfo() supportedScheme[");
                stringBuilder3.append(i);
                stringBuilder3.append("]: ");
                stringBuilder3.append(this.supportedSchemes[i]);
                Log.v(str3, stringBuilder3.toString());
            }
            String str4 = MediaPlayer.TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("DrmInfo() Parcel psshsize: ");
            stringBuilder4.append(psshsize);
            stringBuilder4.append(" supportedDRMsCount: ");
            stringBuilder4.append(supportedDRMsCount);
            Log.v(str4, stringBuilder4.toString());
        }

        private DrmInfo makeCopy() {
            return new DrmInfo(this.mapPssh, this.supportedSchemes);
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
                    Log.w(MediaPlayer.TAG, String.format("parsePSSH: len is too short to parse UUID: (%d < 16) pssh: %d", new Object[]{Integer.valueOf(len), Integer.valueOf(psshsize)}));
                    return null;
                }
                UUID uuid = bytesToUUID(Arrays.copyOfRange(bArr, i2, i2 + 16));
                i2 += 16;
                len -= 16;
                if (len < 4) {
                    Log.w(MediaPlayer.TAG, String.format("parsePSSH: len is too short to parse datalen: (%d < 4) pssh: %d", new Object[]{Integer.valueOf(len), Integer.valueOf(psshsize)}));
                    return null;
                }
                int datalen;
                byte[] subset = Arrays.copyOfRange(bArr, i2, i2 + 4);
                if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                    datalen = ((((subset[3] & 255) << 24) | ((subset[2] & 255) << 16)) | ((subset[1] & 255) << 8)) | (subset[0] & 255);
                } else {
                    datalen = ((((subset[0] & 255) << 24) | ((subset[1] & 255) << 16)) | ((subset[2] & 255) << 8)) | (subset[3] & 255);
                }
                i2 += 4;
                len -= 4;
                if (len < datalen) {
                    Log.w(MediaPlayer.TAG, String.format("parsePSSH: len is too short to parse data: (%d < %d) pssh: %d", new Object[]{Integer.valueOf(len), Integer.valueOf(datalen), Integer.valueOf(psshsize)}));
                    return null;
                }
                byte[] data = Arrays.copyOfRange(bArr, i2, i2 + datalen);
                i2 += datalen;
                len -= datalen;
                Log.v(MediaPlayer.TAG, String.format("parsePSSH[%d]: <%s, %s> pssh: %d", new Object[]{Integer.valueOf(numentries), uuid, arrToHex(data), Integer.valueOf(psshsize)}));
                numentries++;
                result.put(uuid, data);
                i = 0;
            }
            return result;
        }
    }

    public static final class MetricsConstants {
        public static final String CODEC_AUDIO = "android.media.mediaplayer.audio.codec";
        public static final String CODEC_VIDEO = "android.media.mediaplayer.video.codec";
        public static final String DURATION = "android.media.mediaplayer.durationMs";
        public static final String ERRORS = "android.media.mediaplayer.err";
        public static final String ERROR_CODE = "android.media.mediaplayer.errcode";
        public static final String FRAMES = "android.media.mediaplayer.frames";
        public static final String FRAMES_DROPPED = "android.media.mediaplayer.dropped";
        public static final String HEIGHT = "android.media.mediaplayer.height";
        public static final String MIME_TYPE_AUDIO = "android.media.mediaplayer.audio.mime";
        public static final String MIME_TYPE_VIDEO = "android.media.mediaplayer.video.mime";
        public static final String PLAYING = "android.media.mediaplayer.playingMs";
        public static final String WIDTH = "android.media.mediaplayer.width";

        private MetricsConstants() {
        }
    }

    public interface OnBufferingUpdateListener {
        void onBufferingUpdate(MediaPlayer mediaPlayer, int i);
    }

    public interface OnCompletionListener {
        void onCompletion(MediaPlayer mediaPlayer);
    }

    public interface OnDrmConfigHelper {
        void onDrmConfig(MediaPlayer mediaPlayer);
    }

    private class OnDrmInfoHandlerDelegate {
        private Handler mHandler;
        private MediaPlayer mMediaPlayer;
        private OnDrmInfoListener mOnDrmInfoListener;

        OnDrmInfoHandlerDelegate(MediaPlayer mp, OnDrmInfoListener listener, Handler handler) {
            this.mMediaPlayer = mp;
            this.mOnDrmInfoListener = listener;
            if (handler != null) {
                this.mHandler = handler;
            }
        }

        void notifyClient(final DrmInfo drmInfo) {
            if (this.mHandler != null) {
                this.mHandler.post(new Runnable() {
                    public void run() {
                        OnDrmInfoHandlerDelegate.this.mOnDrmInfoListener.onDrmInfo(OnDrmInfoHandlerDelegate.this.mMediaPlayer, drmInfo);
                    }
                });
            } else {
                this.mOnDrmInfoListener.onDrmInfo(this.mMediaPlayer, drmInfo);
            }
        }
    }

    public interface OnDrmInfoListener {
        void onDrmInfo(MediaPlayer mediaPlayer, DrmInfo drmInfo);
    }

    private class OnDrmPreparedHandlerDelegate {
        private Handler mHandler;
        private MediaPlayer mMediaPlayer;
        private OnDrmPreparedListener mOnDrmPreparedListener;

        OnDrmPreparedHandlerDelegate(MediaPlayer mp, OnDrmPreparedListener listener, Handler handler) {
            this.mMediaPlayer = mp;
            this.mOnDrmPreparedListener = listener;
            if (handler != null) {
                this.mHandler = handler;
            } else if (MediaPlayer.this.mEventHandler != null) {
                this.mHandler = MediaPlayer.this.mEventHandler;
            } else {
                Log.e(MediaPlayer.TAG, "OnDrmPreparedHandlerDelegate: Unexpected null mEventHandler");
            }
        }

        void notifyClient(final int status) {
            if (this.mHandler != null) {
                this.mHandler.post(new Runnable() {
                    public void run() {
                        OnDrmPreparedHandlerDelegate.this.mOnDrmPreparedListener.onDrmPrepared(OnDrmPreparedHandlerDelegate.this.mMediaPlayer, status);
                    }
                });
            } else {
                Log.e(MediaPlayer.TAG, "OnDrmPreparedHandlerDelegate:notifyClient: Unexpected null mHandler");
            }
        }
    }

    public interface OnDrmPreparedListener {
        void onDrmPrepared(MediaPlayer mediaPlayer, int i);
    }

    public interface OnErrorListener {
        boolean onError(MediaPlayer mediaPlayer, int i, int i2);
    }

    public interface OnInfoListener {
        boolean onInfo(MediaPlayer mediaPlayer, int i, int i2);
    }

    public interface OnMediaTimeDiscontinuityListener {
        void onMediaTimeDiscontinuity(MediaPlayer mediaPlayer, MediaTimestamp mediaTimestamp);
    }

    public interface OnPreparedListener {
        void onPrepared(MediaPlayer mediaPlayer);
    }

    public interface OnSeekCompleteListener {
        void onSeekComplete(MediaPlayer mediaPlayer);
    }

    public interface OnSubtitleDataListener {
        void onSubtitleData(MediaPlayer mediaPlayer, SubtitleData subtitleData);
    }

    public interface OnTimedMetaDataAvailableListener {
        void onTimedMetaDataAvailable(MediaPlayer mediaPlayer, TimedMetaData timedMetaData);
    }

    public interface OnTimedTextListener {
        void onTimedText(MediaPlayer mediaPlayer, TimedText timedText);
    }

    public interface OnVideoSizeChangedListener {
        void onVideoSizeChanged(MediaPlayer mediaPlayer, int i, int i2);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface PlaybackRateAudioMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface PrepareDrmStatusCode {
    }

    private class ProvisioningThread extends Thread {
        public static final int TIMEOUT_MS = 60000;
        private Object drmLock;
        private boolean finished;
        private MediaPlayer mediaPlayer;
        private OnDrmPreparedHandlerDelegate onDrmPreparedHandlerDelegate;
        private int status;
        private String urlStr;
        private UUID uuid;

        private ProvisioningThread() {
        }

        /* synthetic */ ProvisioningThread(MediaPlayer x0, AnonymousClass1 x1) {
            this();
        }

        public int status() {
            return this.status;
        }

        public ProvisioningThread initialize(ProvisionRequest request, UUID uuid, MediaPlayer mediaPlayer) {
            this.drmLock = mediaPlayer.mDrmLock;
            this.onDrmPreparedHandlerDelegate = mediaPlayer.mOnDrmPreparedHandlerDelegate;
            this.mediaPlayer = mediaPlayer;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(request.getDefaultUrl());
            stringBuilder.append("&signedRequest=");
            stringBuilder.append(new String(request.getData()));
            this.urlStr = stringBuilder.toString();
            this.uuid = uuid;
            this.status = 3;
            String str = MediaPlayer.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("HandleProvisioninig: Thread is initialised url: ");
            stringBuilder2.append(this.urlStr);
            Log.v(str, stringBuilder2.toString());
            return this;
        }

        public void run() {
            String str;
            StringBuilder stringBuilder;
            byte[] response = null;
            boolean provisioningSucceeded = false;
            HttpURLConnection connection;
            try {
                URL url = new URL(this.urlStr);
                connection = (HttpURLConnection) url.openConnection();
                try {
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(false);
                    connection.setDoInput(true);
                    connection.setConnectTimeout(60000);
                    connection.setReadTimeout(60000);
                    connection.connect();
                    response = Streams.readFully(connection.getInputStream());
                    String str2 = MediaPlayer.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("HandleProvisioninig: Thread run: response ");
                    stringBuilder2.append(response.length);
                    stringBuilder2.append(" ");
                    stringBuilder2.append(response);
                    Log.v(str2, stringBuilder2.toString());
                    connection.disconnect();
                } catch (Exception e) {
                    this.status = 1;
                    String str3 = MediaPlayer.TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("HandleProvisioninig: Thread run: connect ");
                    stringBuilder3.append(e);
                    stringBuilder3.append(" url: ");
                    stringBuilder3.append(url);
                    Log.w(str3, stringBuilder3.toString());
                    connection.disconnect();
                }
            } catch (Exception e2) {
                this.status = 1;
                str = MediaPlayer.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("HandleProvisioninig: Thread run: openConnection ");
                stringBuilder.append(e2);
                Log.w(str, stringBuilder.toString());
            } catch (Throwable th) {
                connection.disconnect();
            }
            if (response != null) {
                try {
                    MediaPlayer.this.mDrmObj.provideProvisionResponse(response);
                    Log.v(MediaPlayer.TAG, "HandleProvisioninig: Thread run: provideProvisionResponse SUCCEEDED!");
                    provisioningSucceeded = true;
                } catch (Exception e22) {
                    this.status = 2;
                    str = MediaPlayer.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HandleProvisioninig: Thread run: provideProvisionResponse ");
                    stringBuilder.append(e22);
                    Log.w(str, stringBuilder.toString());
                }
            }
            boolean succeeded = false;
            int i = 3;
            if (this.onDrmPreparedHandlerDelegate != null) {
                synchronized (this.drmLock) {
                    if (provisioningSucceeded) {
                        try {
                            succeeded = this.mediaPlayer.resumePrepareDrm(this.uuid);
                            if (succeeded) {
                                i = 0;
                            }
                            this.status = i;
                        } catch (Throwable th2) {
                            while (true) {
                            }
                        }
                    }
                    this.mediaPlayer.mDrmProvisioningInProgress = false;
                    this.mediaPlayer.mPrepareDrmInProgress = false;
                    if (!succeeded) {
                        MediaPlayer.this.cleanDrmObj();
                    }
                }
                this.onDrmPreparedHandlerDelegate.notifyClient(this.status);
            } else {
                if (provisioningSucceeded) {
                    succeeded = this.mediaPlayer.resumePrepareDrm(this.uuid);
                    if (succeeded) {
                        i = 0;
                    }
                    this.status = i;
                }
                this.mediaPlayer.mDrmProvisioningInProgress = false;
                this.mediaPlayer.mPrepareDrmInProgress = false;
                if (!succeeded) {
                    MediaPlayer.this.cleanDrmObj();
                }
            }
            this.finished = true;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SeekMode {
    }

    private class EventHandler extends Handler {
        private MediaPlayer mMediaPlayer;

        public EventHandler(MediaPlayer mp, Looper looper) {
            super(looper);
            this.mMediaPlayer = mp;
        }

        /* JADX WARNING: Missing block: B:76:0x0162, code skipped:
            if ((r2.obj instanceof android.os.Parcel) == false) goto L_0x018d;
     */
        /* JADX WARNING: Missing block: B:77:0x0164, code skipped:
            r4 = r2.obj;
            r5 = new android.media.SubtitleData(r4);
            r4.recycle();
            android.media.MediaPlayer.access$2700(r1.this$0).onSubtitleData(r1.mMediaPlayer, r5);
     */
        /* JADX WARNING: Missing block: B:78:0x017b, code skipped:
            if (r0 == null) goto L_0x018d;
     */
        /* JADX WARNING: Missing block: B:79:0x017d, code skipped:
            if (r3 != null) goto L_0x0185;
     */
        /* JADX WARNING: Missing block: B:80:0x017f, code skipped:
            r0.onSubtitleData(r1.mMediaPlayer, r5);
     */
        /* JADX WARNING: Missing block: B:81:0x0185, code skipped:
            r3.post(new android.media.MediaPlayer.EventHandler.AnonymousClass1(r1));
     */
        /* JADX WARNING: Missing block: B:82:0x018d, code skipped:
            return;
     */
        /* JADX WARNING: Missing block: B:147:0x02db, code skipped:
            r0 = android.media.MediaPlayer.access$400(r1.this$0);
     */
        /* JADX WARNING: Missing block: B:148:0x02e1, code skipped:
            if (r0 == null) goto L_0x02e8;
     */
        /* JADX WARNING: Missing block: B:149:0x02e3, code skipped:
            r0.onSeekComplete(r1.mMediaPlayer);
     */
        /* JADX WARNING: Missing block: B:150:0x02e8, code skipped:
            return;
     */
        /* JADX WARNING: Missing block: B:168:0x0334, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(Message msg) {
            Message message = msg;
            if (this.mMediaPlayer.mNativeContext == 0) {
                Log.w(MediaPlayer.TAG, "mediaplayer went away with unhandled events");
                return;
            }
            int i = message.what;
            boolean z = false;
            if (i == 250) {
                if (message.arg1 == 802 && MediaPlayer.MEDIA_REPORT_PROP && MediaPlayer.this.mMediaReportSwitch && MediaPlayer.this.mIsHLS) {
                    Parcel parcel = message.obj;
                    if (parcel != null) {
                        long videoRemainingPlayTime = parcel.readLong();
                        long segDuration = parcel.readLong();
                        int segIndex = parcel.readInt();
                        long aveCodeRate = parcel.readLong();
                        Bundle data = new Bundle();
                        data.putInt("videoProtocol", 0);
                        data.putLong("videoRemainingPlayTime", videoRemainingPlayTime);
                        data.putLong("segDuration", segDuration);
                        data.putInt("segIndex", segIndex);
                        data.putLong("aveCodeRate", aveCodeRate);
                        int res = HwFrameworkFactory.getHwCommBoosterServiceManager().reportBoosterPara(MediaPlayer.MEDIA_REPORT_PKG_NAME, 202, data);
                        if (res != 0) {
                            String str = MediaPlayer.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("report slice info return error = ");
                            stringBuilder.append(res);
                            Log.e(str, stringBuilder.toString());
                        }
                    }
                }
            } else if (i != 10000) {
                TimeProvider timeProvider;
                switch (i) {
                    case 0:
                        break;
                    case 1:
                        try {
                            MediaPlayer.this.scanInternalSubtitleTracks();
                        } catch (RuntimeException e) {
                            sendMessage(obtainMessage(100, 1, -1010, null));
                        }
                        OnPreparedListener onPreparedListener = MediaPlayer.this.mOnPreparedListener;
                        if (onPreparedListener != null) {
                            onPreparedListener.onPrepared(this.mMediaPlayer);
                        }
                        return;
                    case 2:
                        MediaPlayer.this.mOnCompletionInternalListener.onCompletion(this.mMediaPlayer);
                        OnCompletionListener onCompletionListener = MediaPlayer.this.mOnCompletionListener;
                        if (onCompletionListener != null) {
                            onCompletionListener.onCompletion(this.mMediaPlayer);
                        }
                        MediaPlayer.this.stayAwake(false);
                        return;
                    case 3:
                        OnBufferingUpdateListener onBufferingUpdateListener = MediaPlayer.this.mOnBufferingUpdateListener;
                        if (onBufferingUpdateListener != null) {
                            onBufferingUpdateListener.onBufferingUpdate(this.mMediaPlayer, message.arg1);
                        }
                        return;
                    case 4:
                        OnSeekCompleteListener onSeekCompleteListener = MediaPlayer.this.mOnSeekCompleteListener;
                        if (onSeekCompleteListener != null) {
                            onSeekCompleteListener.onSeekComplete(this.mMediaPlayer);
                            break;
                        }
                        break;
                    case 5:
                        OnVideoSizeChangedListener onVideoSizeChangedListener = MediaPlayer.this.mOnVideoSizeChangedListener;
                        if (onVideoSizeChangedListener != null) {
                            onVideoSizeChangedListener.onVideoSizeChanged(this.mMediaPlayer, message.arg1, message.arg2);
                        }
                        return;
                    case 6:
                    case 7:
                        timeProvider = MediaPlayer.this.mTimeProvider;
                        if (timeProvider != null) {
                            if (message.what == 7) {
                                z = true;
                            }
                            timeProvider.onPaused(z);
                            break;
                        }
                        break;
                    case 8:
                        timeProvider = MediaPlayer.this.mTimeProvider;
                        if (timeProvider != null) {
                            timeProvider.onStopped();
                            break;
                        }
                        break;
                    case 9:
                        break;
                    default:
                        Parcel parcel2;
                        String str2;
                        StringBuilder stringBuilder2;
                        switch (i) {
                            case 98:
                                timeProvider = MediaPlayer.this.mTimeProvider;
                                if (timeProvider != null) {
                                    timeProvider.onNotifyTime();
                                }
                                return;
                            case 99:
                                OnTimedTextListener onTimedTextListener = MediaPlayer.this.mOnTimedTextListener;
                                if (onTimedTextListener != null) {
                                    if (message.obj == null) {
                                        onTimedTextListener.onTimedText(this.mMediaPlayer, null);
                                    } else if (message.obj instanceof Parcel) {
                                        parcel2 = message.obj;
                                        TimedText text = new TimedText(parcel2);
                                        parcel2.recycle();
                                        onTimedTextListener.onTimedText(this.mMediaPlayer, text);
                                    }
                                    return;
                                }
                                return;
                            case 100:
                                str2 = MediaPlayer.TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Error (");
                                stringBuilder2.append(message.arg1);
                                stringBuilder2.append(",");
                                stringBuilder2.append(message.arg2);
                                stringBuilder2.append(")");
                                Log.e(str2, stringBuilder2.toString());
                                boolean error_was_handled = false;
                                OnErrorListener onErrorListener = MediaPlayer.this.mOnErrorListener;
                                if (onErrorListener != null) {
                                    error_was_handled = onErrorListener.onError(this.mMediaPlayer, message.arg1, message.arg2);
                                }
                                MediaPlayer.this.mOnCompletionInternalListener.onCompletion(this.mMediaPlayer);
                                OnCompletionListener onCompletionListener2 = MediaPlayer.this.mOnCompletionListener;
                                if (!(onCompletionListener2 == null || error_was_handled)) {
                                    onCompletionListener2.onCompletion(this.mMediaPlayer);
                                }
                                MediaPlayer.this.stayAwake(false);
                                return;
                            default:
                                Handler extSubtitleHandler;
                                switch (i) {
                                    case 200:
                                        i = message.arg1;
                                        switch (i) {
                                            case 700:
                                                str2 = MediaPlayer.TAG;
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("Info (");
                                                stringBuilder2.append(message.arg1);
                                                stringBuilder2.append(",");
                                                stringBuilder2.append(message.arg2);
                                                stringBuilder2.append(")");
                                                Log.i(str2, stringBuilder2.toString());
                                                break;
                                            case 701:
                                            case 702:
                                                timeProvider = MediaPlayer.this.mTimeProvider;
                                                if (timeProvider != null) {
                                                    if (message.arg1 == 701) {
                                                        z = true;
                                                    }
                                                    timeProvider.onBuffering(z);
                                                    break;
                                                }
                                                break;
                                            default:
                                                switch (i) {
                                                    case 802:
                                                        try {
                                                            MediaPlayer.this.scanInternalSubtitleTracks();
                                                            break;
                                                        } catch (RuntimeException e2) {
                                                            sendMessage(obtainMessage(100, 1, -1010, null));
                                                            break;
                                                        }
                                                    case 803:
                                                        break;
                                                }
                                                message.arg1 = 802;
                                                if (MediaPlayer.this.mSubtitleController != null) {
                                                    MediaPlayer.this.mSubtitleController.selectDefaultTrack();
                                                    break;
                                                }
                                                break;
                                        }
                                        OnInfoListener onInfoListener = MediaPlayer.this.mOnInfoListener;
                                        if (onInfoListener != null) {
                                            onInfoListener.onInfo(this.mMediaPlayer, message.arg1, message.arg2);
                                        }
                                        return;
                                    case 201:
                                        synchronized (this) {
                                            if (!MediaPlayer.this.mSubtitleDataListenerDisabled) {
                                                final OnSubtitleDataListener extSubtitleListener = MediaPlayer.this.mExtSubtitleDataListener;
                                                extSubtitleHandler = MediaPlayer.this.mExtSubtitleDataHandler;
                                                break;
                                            }
                                            return;
                                        }
                                    case 202:
                                        OnTimedMetaDataAvailableListener onTimedMetaDataAvailableListener = MediaPlayer.this.mOnTimedMetaDataAvailableListener;
                                        if (onTimedMetaDataAvailableListener != null && (message.obj instanceof Parcel)) {
                                            parcel2 = message.obj;
                                            TimedMetaData data2 = TimedMetaData.createTimedMetaDataFromParcel(parcel2);
                                            parcel2.recycle();
                                            onTimedMetaDataAvailableListener.onTimedMetaDataAvailable(this.mMediaPlayer, data2);
                                        }
                                        return;
                                    default:
                                        switch (i) {
                                            case 210:
                                                str2 = MediaPlayer.TAG;
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("MEDIA_DRM_INFO ");
                                                stringBuilder2.append(MediaPlayer.this.mOnDrmInfoHandlerDelegate);
                                                Log.v(str2, stringBuilder2.toString());
                                                if (message.obj == null) {
                                                    Log.w(MediaPlayer.TAG, "MEDIA_DRM_INFO msg.obj=NULL");
                                                } else if (message.obj instanceof Parcel) {
                                                    OnDrmInfoHandlerDelegate onDrmInfoHandlerDelegate;
                                                    DrmInfo drmInfo = null;
                                                    synchronized (MediaPlayer.this.mDrmLock) {
                                                        if (!(MediaPlayer.this.mOnDrmInfoHandlerDelegate == null || MediaPlayer.this.mDrmInfo == null)) {
                                                            drmInfo = MediaPlayer.this.mDrmInfo.makeCopy();
                                                        }
                                                        onDrmInfoHandlerDelegate = MediaPlayer.this.mOnDrmInfoHandlerDelegate;
                                                    }
                                                    if (onDrmInfoHandlerDelegate != null) {
                                                        onDrmInfoHandlerDelegate.notifyClient(drmInfo);
                                                    }
                                                } else {
                                                    str2 = MediaPlayer.TAG;
                                                    stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append("MEDIA_DRM_INFO msg.obj of unexpected type ");
                                                    stringBuilder2.append(message.obj);
                                                    Log.w(str2, stringBuilder2.toString());
                                                }
                                                return;
                                            case 211:
                                                final OnMediaTimeDiscontinuityListener mediaTimeListener;
                                                synchronized (this) {
                                                    mediaTimeListener = MediaPlayer.this.mOnMediaTimeDiscontinuityListener;
                                                    extSubtitleHandler = MediaPlayer.this.mOnMediaTimeDiscontinuityHandler;
                                                }
                                                if (mediaTimeListener != null && (message.obj instanceof Parcel)) {
                                                    MediaTimestamp timestamp;
                                                    Parcel parcel3 = message.obj;
                                                    parcel3.setDataPosition(0);
                                                    long anchorMediaUs = parcel3.readLong();
                                                    long anchorRealUs = parcel3.readLong();
                                                    float playbackRate = parcel3.readFloat();
                                                    parcel3.recycle();
                                                    if (anchorMediaUs == -1 || anchorRealUs == -1) {
                                                        timestamp = MediaTimestamp.TIMESTAMP_UNKNOWN;
                                                    } else {
                                                        timestamp = new MediaTimestamp(anchorMediaUs, anchorRealUs * 1000, playbackRate);
                                                    }
                                                    if (extSubtitleHandler == null) {
                                                        mediaTimeListener.onMediaTimeDiscontinuity(this.mMediaPlayer, timestamp);
                                                    } else {
                                                        extSubtitleHandler.post(new Runnable() {
                                                            public void run() {
                                                                mediaTimeListener.onMediaTimeDiscontinuity(EventHandler.this.mMediaPlayer, timestamp);
                                                            }
                                                        });
                                                    }
                                                }
                                                return;
                                            default:
                                                str2 = MediaPlayer.TAG;
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("Unknown message type ");
                                                stringBuilder2.append(message.what);
                                                Log.e(str2, stringBuilder2.toString());
                                                return;
                                        }
                                }
                        }
                }
            } else {
                AudioManager.resetAudioPortGeneration();
                synchronized (MediaPlayer.this.mRoutingChangeListeners) {
                    for (NativeRoutingEventHandlerDelegate delegate : MediaPlayer.this.mRoutingChangeListeners.values()) {
                        delegate.notifyClient();
                    }
                }
            }
        }
    }

    public static final class NoDrmSchemeException extends MediaDrmException {
        public NoDrmSchemeException(String detailMessage) {
            super(detailMessage);
        }
    }

    public static final class ProvisioningNetworkErrorException extends MediaDrmException {
        public ProvisioningNetworkErrorException(String detailMessage) {
            super(detailMessage);
        }
    }

    public static final class ProvisioningServerErrorException extends MediaDrmException {
        public ProvisioningServerErrorException(String detailMessage) {
            super(detailMessage);
        }
    }

    static class TimeProvider implements OnSeekCompleteListener, MediaTimeProvider {
        private static final long MAX_EARLY_CALLBACK_US = 1000;
        private static final long MAX_NS_WITHOUT_POSITION_CHECK = 5000000000L;
        private static final int NOTIFY = 1;
        private static final int NOTIFY_SEEK = 3;
        private static final int NOTIFY_STOP = 2;
        private static final int NOTIFY_TIME = 0;
        private static final int NOTIFY_TRACK_DATA = 4;
        private static final String TAG = "MTP";
        private static final long TIME_ADJUSTMENT_RATE = 2;
        public boolean DEBUG = false;
        private boolean mBuffering;
        private Handler mEventHandler;
        private HandlerThread mHandlerThread;
        private long mLastReportedTime;
        private long mLastTimeUs = 0;
        private OnMediaTimeListener[] mListeners;
        private boolean mPaused = true;
        private boolean mPausing = false;
        private MediaPlayer mPlayer;
        private boolean mRefresh = false;
        private boolean mSeeking = false;
        private boolean mStopped = true;
        private long[] mTimes;

        private class EventHandler extends Handler {
            public EventHandler(Looper looper) {
                super(looper);
            }

            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    int i = msg.arg1;
                    if (i != 0) {
                        switch (i) {
                            case 2:
                                TimeProvider.this.notifyStop();
                                return;
                            case 3:
                                TimeProvider.this.notifySeek();
                                return;
                            case 4:
                                TimeProvider.this.notifyTrackData((Pair) msg.obj);
                                return;
                            default:
                                return;
                        }
                    }
                    TimeProvider.this.notifyTimedEvent(true);
                }
            }
        }

        public TimeProvider(MediaPlayer mp) {
            this.mPlayer = mp;
            try {
                getCurrentTimeUs(true, false);
            } catch (IllegalStateException e) {
                this.mRefresh = true;
            }
            Looper myLooper = Looper.myLooper();
            Looper looper = myLooper;
            if (myLooper == null) {
                myLooper = Looper.getMainLooper();
                looper = myLooper;
                if (myLooper == null) {
                    this.mHandlerThread = new HandlerThread("MediaPlayerMTPEventThread", -2);
                    this.mHandlerThread.start();
                    looper = this.mHandlerThread.getLooper();
                }
            }
            this.mEventHandler = new EventHandler(looper);
            this.mListeners = new OnMediaTimeListener[0];
            this.mTimes = new long[0];
            this.mLastTimeUs = 0;
        }

        private void scheduleNotification(int type, long delayUs) {
            if (!this.mSeeking || type != 0) {
                if (this.DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("scheduleNotification ");
                    stringBuilder.append(type);
                    stringBuilder.append(" in ");
                    stringBuilder.append(delayUs);
                    Log.v(str, stringBuilder.toString());
                }
                this.mEventHandler.removeMessages(1);
                this.mEventHandler.sendMessageDelayed(this.mEventHandler.obtainMessage(1, type, 0), (long) ((int) (delayUs / MAX_EARLY_CALLBACK_US)));
            }
        }

        public void close() {
            this.mEventHandler.removeMessages(1);
            if (this.mHandlerThread != null) {
                this.mHandlerThread.quitSafely();
                this.mHandlerThread = null;
            }
        }

        protected void finalize() {
            if (this.mHandlerThread != null) {
                this.mHandlerThread.quitSafely();
            }
        }

        public void onNotifyTime() {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "onNotifyTime: ");
                }
                scheduleNotification(0, 0);
            }
        }

        public void onPaused(boolean paused) {
            synchronized (this) {
                if (this.DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onPaused: ");
                    stringBuilder.append(paused);
                    Log.d(str, stringBuilder.toString());
                }
                if (this.mStopped) {
                    this.mStopped = false;
                    this.mSeeking = true;
                    scheduleNotification(3, 0);
                } else {
                    this.mPausing = paused;
                    this.mSeeking = false;
                    scheduleNotification(0, 0);
                }
            }
        }

        public void onBuffering(boolean buffering) {
            synchronized (this) {
                if (this.DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onBuffering: ");
                    stringBuilder.append(buffering);
                    Log.d(str, stringBuilder.toString());
                }
                this.mBuffering = buffering;
                scheduleNotification(0, 0);
            }
        }

        public void onStopped() {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "onStopped");
                }
                this.mPaused = true;
                this.mStopped = true;
                this.mSeeking = false;
                this.mBuffering = false;
                scheduleNotification(2, 0);
            }
        }

        public void onSeekComplete(MediaPlayer mp) {
            synchronized (this) {
                this.mStopped = false;
                this.mSeeking = true;
                scheduleNotification(3, 0);
            }
        }

        public void onNewPlayer() {
            if (this.mRefresh) {
                synchronized (this) {
                    this.mStopped = false;
                    this.mSeeking = true;
                    this.mBuffering = false;
                    scheduleNotification(3, 0);
                }
            }
        }

        private synchronized void notifySeek() {
            this.mSeeking = false;
            try {
                long timeUs = getCurrentTimeUs(true, false);
                if (this.DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onSeekComplete at ");
                    stringBuilder.append(timeUs);
                    Log.d(str, stringBuilder.toString());
                }
                for (OnMediaTimeListener listener : this.mListeners) {
                    if (listener == null) {
                        break;
                    }
                    listener.onSeek(timeUs);
                }
            } catch (IllegalStateException e) {
                if (this.DEBUG) {
                    Log.d(TAG, "onSeekComplete but no player");
                }
                this.mPausing = true;
                notifyTimedEvent(false);
            }
        }

        private synchronized void notifyTrackData(Pair<SubtitleTrack, byte[]> trackData) {
            trackData.first.onData(trackData.second, true, -1);
        }

        private synchronized void notifyStop() {
            for (OnMediaTimeListener listener : this.mListeners) {
                if (listener == null) {
                    break;
                }
                listener.onStop();
            }
        }

        private int registerListener(OnMediaTimeListener listener) {
            int i = 0;
            while (i < this.mListeners.length && this.mListeners[i] != listener && this.mListeners[i] != null) {
                i++;
            }
            if (i >= this.mListeners.length) {
                OnMediaTimeListener[] newListeners = new OnMediaTimeListener[(i + 1)];
                long[] newTimes = new long[(i + 1)];
                System.arraycopy(this.mListeners, 0, newListeners, 0, this.mListeners.length);
                System.arraycopy(this.mTimes, 0, newTimes, 0, this.mTimes.length);
                this.mListeners = newListeners;
                this.mTimes = newTimes;
            }
            if (this.mListeners[i] == null) {
                this.mListeners[i] = listener;
                this.mTimes[i] = -1;
            }
            return i;
        }

        public void notifyAt(long timeUs, OnMediaTimeListener listener) {
            synchronized (this) {
                if (this.DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("notifyAt ");
                    stringBuilder.append(timeUs);
                    Log.d(str, stringBuilder.toString());
                }
                this.mTimes[registerListener(listener)] = timeUs;
                scheduleNotification(0, 0);
            }
        }

        public void scheduleUpdate(OnMediaTimeListener listener) {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "scheduleUpdate");
                }
                int i = registerListener(listener);
                if (!this.mStopped) {
                    this.mTimes[i] = 0;
                    scheduleNotification(0, 0);
                }
            }
        }

        public void cancelNotifications(OnMediaTimeListener listener) {
            synchronized (this) {
                int i = 0;
                while (i < this.mListeners.length) {
                    if (this.mListeners[i] == listener) {
                        System.arraycopy(this.mListeners, i + 1, this.mListeners, i, (this.mListeners.length - i) - 1);
                        System.arraycopy(this.mTimes, i + 1, this.mTimes, i, (this.mTimes.length - i) - 1);
                        this.mListeners[this.mListeners.length - 1] = null;
                        this.mTimes[this.mTimes.length - 1] = -1;
                        break;
                    } else if (this.mListeners[i] == null) {
                        break;
                    } else {
                        i++;
                    }
                }
                scheduleNotification(0, 0);
            }
        }

        private synchronized void notifyTimedEvent(boolean refreshTime) {
            boolean z = refreshTime;
            synchronized (this) {
                long nowUs;
                try {
                    nowUs = getCurrentTimeUs(z, true);
                } catch (IllegalStateException e) {
                    IllegalStateException illegalStateException = e;
                    this.mRefresh = true;
                    this.mPausing = true;
                    nowUs = getCurrentTimeUs(z, true);
                }
                long nextTimeUs = nowUs;
                if (this.mSeeking) {
                    return;
                }
                long nowUs2;
                long nowUs3;
                int ix = 0;
                if (this.DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("notifyTimedEvent(");
                    sb.append(this.mLastTimeUs);
                    sb.append(" -> ");
                    sb.append(nowUs);
                    sb.append(") from {");
                    long[] jArr = this.mTimes;
                    int length = jArr.length;
                    boolean first = true;
                    int first2 = 0;
                    while (first2 < length) {
                        nowUs2 = nowUs;
                        long time = jArr[first2];
                        if (time != -1) {
                            if (!first) {
                                sb.append(", ");
                            }
                            sb.append(time);
                            first = false;
                        }
                        first2++;
                        nowUs = nowUs2;
                    }
                    nowUs2 = nowUs;
                    sb.append("}");
                    Log.d(TAG, sb.toString());
                } else {
                    nowUs2 = nowUs;
                }
                Vector<OnMediaTimeListener> activatedListeners = new Vector();
                while (true) {
                    int ix2 = ix;
                    if (ix2 >= this.mTimes.length) {
                        break;
                    } else if (this.mListeners[ix2] == null) {
                        break;
                    } else {
                        if (this.mTimes[ix2] > -1) {
                            if (this.mTimes[ix2] <= nowUs2 + MAX_EARLY_CALLBACK_US) {
                                activatedListeners.add(this.mListeners[ix2]);
                                if (this.DEBUG) {
                                    Log.d(TAG, "removed");
                                }
                                this.mTimes[ix2] = -1;
                            } else if (nextTimeUs == nowUs2 || this.mTimes[ix2] < nextTimeUs) {
                                nextTimeUs = this.mTimes[ix2];
                            }
                        }
                        ix = ix2 + 1;
                    }
                }
                if (nextTimeUs <= nowUs2 || this.mPaused) {
                    nowUs3 = nowUs2;
                    this.mEventHandler.removeMessages(1);
                } else {
                    if (this.DEBUG) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("scheduling for ");
                        stringBuilder.append(nextTimeUs);
                        stringBuilder.append(" and ");
                        nowUs3 = nowUs2;
                        stringBuilder.append(nowUs3);
                        Log.d(str, stringBuilder.toString());
                    } else {
                        nowUs3 = nowUs2;
                    }
                    this.mPlayer.notifyAt(nextTimeUs);
                }
                Iterator it = activatedListeners.iterator();
                while (it.hasNext()) {
                    ((OnMediaTimeListener) it.next()).onTimedEvent(nowUs3);
                }
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:19:0x002f A:{Catch:{ IllegalStateException -> 0x007f }} */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0058 A:{SYNTHETIC, Splitter:B:25:0x0058} */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public long getCurrentTimeUs(boolean refreshTime, boolean monotonic) throws IllegalStateException {
            synchronized (this) {
                long j;
                if (!this.mPaused || refreshTime) {
                    try {
                        boolean z;
                        this.mLastTimeUs = ((long) this.mPlayer.getCurrentPosition()) * MAX_EARLY_CALLBACK_US;
                        if (this.mPlayer.isPlaying()) {
                            if (!this.mBuffering) {
                                z = false;
                                this.mPaused = z;
                                if (this.DEBUG) {
                                    String str = TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append(this.mPaused ? "paused" : "playing");
                                    stringBuilder.append(" at ");
                                    stringBuilder.append(this.mLastTimeUs);
                                    Log.v(str, stringBuilder.toString());
                                }
                                if (monotonic) {
                                    if (this.mLastTimeUs < this.mLastReportedTime) {
                                        if (this.mLastReportedTime - this.mLastTimeUs > 1000000) {
                                            this.mStopped = false;
                                            this.mSeeking = true;
                                            scheduleNotification(3, 0);
                                        }
                                        j = this.mLastReportedTime;
                                        return j;
                                    }
                                }
                                this.mLastReportedTime = this.mLastTimeUs;
                                j = this.mLastReportedTime;
                                return j;
                            }
                        }
                        z = true;
                        this.mPaused = z;
                        if (this.DEBUG) {
                        }
                        if (monotonic) {
                        }
                        this.mLastReportedTime = this.mLastTimeUs;
                        j = this.mLastReportedTime;
                        return j;
                    } catch (IllegalStateException e) {
                        if (this.mPausing) {
                            this.mPausing = false;
                            if (!monotonic || this.mLastReportedTime < this.mLastTimeUs) {
                                this.mLastReportedTime = this.mLastTimeUs;
                            }
                            this.mPaused = true;
                            if (this.DEBUG) {
                                String str2 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("illegal state, but pausing: estimating at ");
                                stringBuilder2.append(this.mLastReportedTime);
                                Log.d(str2, stringBuilder2.toString());
                            }
                            return this.mLastReportedTime;
                        }
                        throw e;
                    }
                }
                j = this.mLastReportedTime;
                return j;
            }
        }
    }

    public static class TrackInfo implements Parcelable {
        static final Creator<TrackInfo> CREATOR = new Creator<TrackInfo>() {
            public TrackInfo createFromParcel(Parcel in) {
                return new TrackInfo(in);
            }

            public TrackInfo[] newArray(int size) {
                return new TrackInfo[size];
            }
        };
        public static final int MEDIA_TRACK_TYPE_AUDIO = 2;
        public static final int MEDIA_TRACK_TYPE_METADATA = 5;
        public static final int MEDIA_TRACK_TYPE_SUBTITLE = 4;
        public static final int MEDIA_TRACK_TYPE_TIMEDTEXT = 3;
        public static final int MEDIA_TRACK_TYPE_UNKNOWN = 0;
        public static final int MEDIA_TRACK_TYPE_VIDEO = 1;
        final MediaFormat mFormat;
        final int mTrackType;

        @Retention(RetentionPolicy.SOURCE)
        public @interface TrackType {
        }

        public int getTrackType() {
            return this.mTrackType;
        }

        public String getLanguage() {
            String language = this.mFormat.getString(MediaFormat.KEY_LANGUAGE);
            return language == null ? "und" : language;
        }

        public MediaFormat getFormat() {
            if (this.mTrackType == 3 || this.mTrackType == 4) {
                return this.mFormat;
            }
            return null;
        }

        TrackInfo(Parcel in) {
            this.mTrackType = in.readInt();
            this.mFormat = MediaFormat.createSubtitleFormat(in.readString(), in.readString());
            if (this.mTrackType == 4) {
                this.mFormat.setInteger(MediaFormat.KEY_IS_AUTOSELECT, in.readInt());
                this.mFormat.setInteger(MediaFormat.KEY_IS_DEFAULT, in.readInt());
                this.mFormat.setInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE, in.readInt());
            }
        }

        TrackInfo(int type, MediaFormat format) {
            this.mTrackType = type;
            this.mFormat = format;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mTrackType);
            dest.writeString(getLanguage());
            if (this.mTrackType == 4) {
                dest.writeString(this.mFormat.getString(MediaFormat.KEY_MIME));
                dest.writeInt(this.mFormat.getInteger(MediaFormat.KEY_IS_AUTOSELECT));
                dest.writeInt(this.mFormat.getInteger(MediaFormat.KEY_IS_DEFAULT));
                dest.writeInt(this.mFormat.getInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE));
            }
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

    private native int _getAudioStreamType() throws IllegalStateException;

    private native void _notifyAt(long j);

    private native void _pause() throws IllegalStateException;

    private native void _prepare() throws IOException, IllegalStateException;

    private native void _prepareDrm(byte[] bArr, byte[] bArr2);

    private native void _release();

    private native void _releaseDrm();

    private native void _reset();

    private final native void _seekTo(long j, int i);

    private native void _setAudioStreamType(int i);

    private native void _setAuxEffectSendLevel(float f);

    private native void _setDataSource(MediaDataSource mediaDataSource) throws IllegalArgumentException, IllegalStateException;

    private native void _setDataSource(FileDescriptor fileDescriptor, long j, long j2) throws IOException, IllegalArgumentException, IllegalStateException;

    private native void _setVideoSurface(Surface surface);

    private native void _setVolume(float f, float f2);

    private native void _start() throws IllegalStateException;

    private native void _stop() throws IllegalStateException;

    private native void nativeSetDataSource(IBinder iBinder, String str, String[] strArr, String[] strArr2) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException;

    private native int native_applyVolumeShaper(Configuration configuration, Operation operation);

    private final native void native_enableDeviceCallback(boolean z);

    private final native void native_finalize();

    private final native boolean native_getMetadata(boolean z, boolean z2, Parcel parcel);

    private native PersistableBundle native_getMetrics();

    private final native int native_getRoutedDeviceId();

    private native State native_getVolumeShaperState(int i);

    private static final native void native_init();

    private final native int native_invoke(Parcel parcel, Parcel parcel2);

    public static native int native_pullBatteryData(Parcel parcel);

    private final native int native_setMetadataFilter(Parcel parcel);

    private final native boolean native_setOutputDevice(int i);

    private final native int native_setRetransmitEndpoint(String str, int i);

    private final native void native_setup(Object obj);

    private native boolean setParameter(int i, Parcel parcel);

    public native void attachAuxEffect(int i);

    public native int getAudioSessionId();

    public native BufferingParams getBufferingParams();

    public native int getCurrentPosition();

    public native int getDuration();

    public native PlaybackParams getPlaybackParams();

    public native SyncParams getSyncParams();

    public native int getVideoHeight();

    public native int getVideoWidth();

    public native boolean isLooping();

    public native boolean isPlaying();

    public native void prepareAsync() throws IllegalStateException;

    public native void setAudioSessionId(int i) throws IllegalArgumentException, IllegalStateException;

    public native void setBufferingParams(BufferingParams bufferingParams);

    public native void setLooping(boolean z);

    public native void setNextMediaPlayer(MediaPlayer mediaPlayer);

    public native void setPlaybackParams(PlaybackParams playbackParams);

    public native void setSyncParams(SyncParams syncParams);

    static {
        System.loadLibrary("media_jni");
        native_init();
        boolean z = false;
        if (BOOSTER_SUPPORT && HISI_VIDEO_ACC_FUNC) {
            z = true;
        }
        MEDIA_REPORT_PROP = z;
    }

    public MediaPlayer() {
        super(new Builder().build(), 2);
        Looper myLooper = Looper.myLooper();
        Looper looper = myLooper;
        if (myLooper != null) {
            this.mEventHandler = new EventHandler(this, looper);
        } else {
            myLooper = Looper.getMainLooper();
            looper = myLooper;
            if (myLooper != null) {
                this.mEventHandler = new EventHandler(this, looper);
            } else {
                this.mEventHandler = null;
            }
        }
        this.mTimeProvider = new TimeProvider(this);
        this.mOpenSubtitleSources = new Vector();
        native_setup(new WeakReference(this));
        baseRegisterPlayer();
    }

    public Parcel newRequest() {
        Parcel parcel = Parcel.obtain();
        parcel.writeInterfaceToken(IMEDIA_PLAYER);
        return parcel;
    }

    public void invoke(Parcel request, Parcel reply) {
        int retcode = native_invoke(request, reply);
        reply.setDataPosition(0);
        if (retcode != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("failure code: ");
            stringBuilder.append(retcode);
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    public void setDisplay(SurfaceHolder sh) {
        Surface surface;
        this.mSurfaceHolder = sh;
        if (sh != null) {
            surface = sh.getSurface();
        } else {
            surface = null;
        }
        _setVideoSurface(surface);
        updateSurfaceScreenOn();
    }

    public void setSurface(Surface surface) {
        if (this.mScreenOnWhilePlaying && surface != null) {
            Log.w(TAG, "setScreenOnWhilePlaying(true) is ineffective for Surface");
        }
        this.mSurfaceHolder = null;
        _setVideoSurface(surface);
        updateSurfaceScreenOn();
    }

    public void setVideoScalingMode(int mode) {
        if (isVideoScalingModeSupported(mode)) {
            Parcel request = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                request.writeInterfaceToken(IMEDIA_PLAYER);
                request.writeInt(6);
                request.writeInt(mode);
                invoke(request, reply);
            } finally {
                request.recycle();
                reply.recycle();
            }
        } else {
            String msg = new StringBuilder();
            msg.append("Scaling mode ");
            msg.append(mode);
            msg.append(" is not supported");
            throw new IllegalArgumentException(msg.toString());
        }
    }

    public static MediaPlayer create(Context context, Uri uri) {
        return create(context, uri, null);
    }

    public static MediaPlayer create(Context context, Uri uri, SurfaceHolder holder) {
        int s = AudioSystem.newAudioSessionId();
        return create(context, uri, holder, null, s > 0 ? s : 0);
    }

    public static MediaPlayer create(Context context, Uri uri, SurfaceHolder holder, AudioAttributes audioAttributes, int audioSessionId) {
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setAudioAttributes(audioAttributes != null ? audioAttributes : new Builder().build());
            mp.setAudioSessionId(audioSessionId);
            mp.setDataSource(context, uri);
            if (holder != null) {
                mp.setDisplay(holder);
            }
            mp.prepare();
            return mp;
        } catch (IOException ex) {
            Log.d(TAG, "create failed:", ex);
            return null;
        } catch (IllegalArgumentException ex2) {
            Log.d(TAG, "create failed:", ex2);
            return null;
        } catch (SecurityException ex3) {
            Log.d(TAG, "create failed:", ex3);
            return null;
        }
    }

    public static MediaPlayer create(Context context, int resid) {
        int s = AudioSystem.newAudioSessionId();
        return create(context, resid, null, s > 0 ? s : 0);
    }

    public static MediaPlayer create(Context context, int resid, AudioAttributes audioAttributes, int audioSessionId) {
        try {
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(resid);
            if (afd == null) {
                return null;
            }
            MediaPlayer mp = new MediaPlayer();
            mp.setAudioAttributes(audioAttributes != null ? audioAttributes : new Builder().build());
            mp.setAudioSessionId(audioSessionId);
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.prepare();
            return mp;
        } catch (IOException ex) {
            Log.d(TAG, "create failed:", ex);
            return null;
        } catch (IllegalArgumentException ex2) {
            Log.d(TAG, "create failed:", ex2);
            return null;
        } catch (SecurityException ex3) {
            Log.d(TAG, "create failed:", ex3);
            return null;
        }
    }

    public void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        setDataSource(context, uri, null, null);
    }

    public void setDataSource(Context context, Uri uri, Map<String, String> headers, List<HttpCookie> cookies) throws IOException {
        if (context == null) {
            throw new NullPointerException("context param can not be null.");
        } else if (uri != null) {
            if (cookies != null) {
                CookieHandler cookieHandler = CookieHandler.getDefault();
                if (!(cookieHandler == null || (cookieHandler instanceof CookieManager))) {
                    throw new IllegalArgumentException("The cookie handler has to be of CookieManager type when cookies are provided.");
                }
            }
            ContentResolver resolver = context.getContentResolver();
            String actualUrim = null;
            try {
                actualUrim = MediaStore.getPath(context, uri);
            } catch (SecurityException e) {
                Log.e(TAG, "MediaStore.getPath error ");
            }
            if (actualUrim == null || !actualUrim.endsWith(".isma")) {
                String scheme = uri.getScheme();
                String authority = ContentProvider.getAuthorityWithoutUserId(uri.getAuthority());
                if (ContentResolver.SCHEME_FILE.equals(scheme)) {
                    setDataSource(uri.getPath());
                    return;
                }
                if ("content".equals(scheme) && "settings".equals(authority)) {
                    int type = RingtoneManager.getDefaultType(uri);
                    Uri cacheUri = RingtoneManager.getCacheForType(type, context.getUserId());
                    Uri actualUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
                    if (!attemptDataSource(resolver, cacheUri) && !attemptDataSource(resolver, actualUri)) {
                        setDataSource(uri.toString(), (Map) headers, (List) cookies);
                    } else {
                        return;
                    }
                } else if (!attemptDataSource(resolver, uri)) {
                    setDataSource(uri.toString(), (Map) headers, (List) cookies);
                } else {
                    return;
                }
                return;
            }
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(R.raw.fallbackring);
            if (afd != null) {
                if (afd.getDeclaredLength() < 0) {
                    setDataSource(afd.getFileDescriptor());
                } else {
                    setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getDeclaredLength());
                }
                afd.close();
            }
        } else {
            throw new NullPointerException("uri param can not be null.");
        }
    }

    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        setDataSource(context, uri, (Map) headers, null);
    }

    /* JADX WARNING: Removed duplicated region for block: B:23:0x0027 A:{ExcHandler: IOException | NullPointerException | SecurityException (r0_2 'ex' java.lang.Exception), Splitter:B:0:0x0000} */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0027 A:{ExcHandler: IOException | NullPointerException | SecurityException (r0_2 'ex' java.lang.Exception), Splitter:B:0:0x0000} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:18:0x001e, code skipped:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:20:?, code skipped:
            r1.addSuppressed(r3);
     */
    /* JADX WARNING: Missing block: B:23:0x0027, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:24:0x0028, code skipped:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("Couldn't open ");
            r2.append(r6);
            r2.append(": ");
            r2.append(r0);
            android.util.Log.w(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:25:0x0047, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean attemptDataSource(ContentResolver resolver, Uri uri) {
        AssetFileDescriptor afd;
        try {
            afd = resolver.openAssetFileDescriptor(uri, FullBackup.ROOT_TREE_TOKEN);
            setDataSource(afd);
            if (afd != null) {
                afd.close();
            }
            return true;
        } catch (IOException | NullPointerException | SecurityException ex) {
        } catch (Throwable th) {
            if (afd != null) {
                if (r1 != null) {
                    afd.close();
                } else {
                    afd.close();
                }
            }
        }
    }

    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        setDataSource(path, null, null);
    }

    public void setDataSource(String path, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        setDataSource(path, (Map) headers, null);
    }

    private void setDataSource(String path, Map<String, String> headers, List<HttpCookie> cookies) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        String[] keys = null;
        String[] values = null;
        if (headers != null) {
            keys = new String[headers.size()];
            values = new String[headers.size()];
            int i = 0;
            for (Entry<String, String> entry : headers.entrySet()) {
                keys[i] = (String) entry.getKey();
                values[i] = (String) entry.getValue();
                i++;
            }
        }
        setDataSource(path, keys, values, (List) cookies);
    }

    private void setDataSource(String path, String[] keys, String[] values, List<HttpCookie> cookies) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        if (MEDIA_REPORT_PROP) {
            String tmpPath = path.toLowerCase(Locale.getDefault());
            if ((tmpPath.startsWith("http://") || tmpPath.startsWith("https://") || tmpPath.startsWith("file://")) && tmpPath.indexOf("m3u8") != -1) {
                this.mIsHLS = true;
            } else {
                this.mIsHLS = false;
            }
            if (!this.mMediaReportRegister && this.mIsHLS && this.mIHwCommBoosterCallBack == null) {
                this.mIHwCommBoosterCallBack = new Stub() {
                    public void callBack(int callBackDataType, Bundle bundle) throws RemoteException {
                        if (callBackDataType == 1) {
                            int mediareportswitch = bundle.getInt("VideoInfoReportState");
                            if (mediareportswitch == 0) {
                                MediaPlayer.this.mMediaReportSwitch = false;
                            } else if (mediareportswitch == 1) {
                                MediaPlayer.this.mMediaReportSwitch = true;
                            } else {
                                Log.e(MediaPlayer.TAG, "callback VideoInfoReportState invalid");
                            }
                        }
                    }
                };
                int res = HwFrameworkFactory.getHwCommBoosterServiceManager().registerCallBack(MEDIA_REPORT_PKG_NAME, this.mIHwCommBoosterCallBack);
                if (res == 0) {
                    this.mMediaReportRegister = true;
                } else {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("registerCallBack return other error = ");
                    stringBuilder.append(res);
                    Log.e(str, stringBuilder.toString());
                }
            }
        }
        Uri uri = Uri.parse(path);
        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            path = uri.getPath();
        } else if (scheme != null) {
            nativeSetDataSource(MediaHTTPService.createHttpServiceBinderIfNecessary(path, cookies), path, keys, values);
            return;
        }
        File file = new File(path);
        if (!file.exists() || path.toLowerCase(Locale.getDefault()).endsWith(".sdp")) {
            nativeSetDataSource(MediaHTTPService.createHttpServiceBinderIfNecessary(path), path, keys, values);
        } else {
            FileInputStream is = new FileInputStream(file);
            try {
                setDataSource(is.getFD());
                is.close();
            } catch (SecurityException ex) {
                Log.d(TAG, "setDataSource failed:", ex);
                throw ex;
            } catch (IOException ex2) {
                Log.d(TAG, "setDataSource failed:", ex2);
                throw ex2;
            } catch (IllegalArgumentException ex3) {
                Log.d(TAG, "setDataSource failed:", ex3);
                throw ex3;
            } catch (IllegalStateException ex4) {
                Log.d(TAG, "setDataSource failed:", ex4);
                throw ex4;
            } catch (Throwable th) {
                is.close();
            }
        }
    }

    public void setDataSource(AssetFileDescriptor afd) throws IOException, IllegalArgumentException, IllegalStateException {
        Preconditions.checkNotNull(afd);
        if (afd.getDeclaredLength() < 0) {
            setDataSource(afd.getFileDescriptor());
            return;
        }
        setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getDeclaredLength());
    }

    public void setDataSource(FileDescriptor fd) throws IOException, IllegalArgumentException, IllegalStateException {
        setDataSource(fd, 0, (long) DataSourceDesc.LONG_MAX);
    }

    public void setDataSource(FileDescriptor fd, long offset, long length) throws IOException, IllegalArgumentException, IllegalStateException {
        _setDataSource(fd, offset, length);
    }

    public void setDataSource(MediaDataSource dataSource) throws IllegalArgumentException, IllegalStateException {
        _setDataSource(dataSource);
    }

    private void reportMediaStatus(int status) {
        if (MEDIA_REPORT_PROP && this.mMediaReportSwitch && this.mIsHLS) {
            Bundle data = new Bundle();
            data.putInt("videoStatus", status);
            int res = HwFrameworkFactory.getHwCommBoosterServiceManager().reportBoosterPara(MEDIA_REPORT_PKG_NAME, 201, data);
            if (res != 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("media report return error = ");
                stringBuilder.append(res);
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    public void prepare() throws IOException, IllegalStateException {
        _prepare();
        scanInternalSubtitleTracks();
        synchronized (this.mDrmLock) {
            this.mDrmInfoResolved = true;
        }
        reportMediaStatus(0);
    }

    public void start() throws IllegalStateException {
        final int delay = getStartDelayMs();
        if (delay == 0) {
            try {
                startImpl();
            } catch (IllegalStateException e) {
                stayAwake(false);
                Log.w(TAG, "Start Error, Maybe the MediaPlayer have been Changed ");
            }
        } else {
            new Thread() {
                public void run() {
                    try {
                        Thread.sleep((long) delay);
                    } catch (InterruptedException e) {
                        Log.w(MediaPlayer.TAG, "InterruptedException when delay in start run");
                    }
                    MediaPlayer.this.baseSetStartDelayMs(0);
                    try {
                        MediaPlayer.this.startImpl();
                    } catch (IllegalStateException e2) {
                        MediaPlayer.this.stayAwake(false);
                        String str = MediaPlayer.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Start Error, Maybe the MediaPlayer have been Changed delay");
                        stringBuilder.append(delay);
                        Log.w(str, stringBuilder.toString());
                    }
                }
            }.start();
        }
        reportMediaStatus(1);
    }

    private void startImpl() {
        baseStart();
        stayAwake(true);
        _start();
    }

    private int getAudioStreamType() {
        if (this.mStreamType == Integer.MIN_VALUE) {
            this.mStreamType = _getAudioStreamType();
        }
        return this.mStreamType;
    }

    public void stop() throws IllegalStateException {
        stayAwake(false);
        _stop();
        baseStop();
        reportMediaStatus(4);
    }

    public void pause() throws IllegalStateException {
        stayAwake(false);
        _pause();
        basePause();
        reportMediaStatus(2);
    }

    void playerStart() {
        start();
    }

    void playerPause() {
        pause();
    }

    void playerStop() {
        stop();
    }

    int playerApplyVolumeShaper(Configuration configuration, Operation operation) {
        return native_applyVolumeShaper(configuration, operation);
    }

    State playerGetVolumeShaperState(int id) {
        return native_getVolumeShaperState(id);
    }

    public VolumeShaper createVolumeShaper(Configuration configuration) {
        return new VolumeShaper(configuration, this);
    }

    public boolean setPreferredDevice(AudioDeviceInfo deviceInfo) {
        int preferredDeviceId = 0;
        if (deviceInfo != null && !deviceInfo.isSink()) {
            return false;
        }
        if (deviceInfo != null) {
            preferredDeviceId = deviceInfo.getId();
        }
        boolean status = native_setOutputDevice(preferredDeviceId);
        if (status) {
            synchronized (this) {
                this.mPreferredDevice = deviceInfo;
            }
        }
        return status;
    }

    public AudioDeviceInfo getPreferredDevice() {
        AudioDeviceInfo audioDeviceInfo;
        synchronized (this) {
            audioDeviceInfo = this.mPreferredDevice;
        }
        return audioDeviceInfo;
    }

    public AudioDeviceInfo getRoutedDevice() {
        int deviceId = native_getRoutedDeviceId();
        if (deviceId == 0) {
            return null;
        }
        AudioDeviceInfo[] devices = AudioManager.getDevicesStatic(2);
        for (int i = 0; i < devices.length; i++) {
            if (devices[i].getId() == deviceId) {
                return devices[i];
            }
        }
        return null;
    }

    @GuardedBy("mRoutingChangeListeners")
    private void enableNativeRoutingCallbacksLocked(boolean enabled) {
        if (this.mRoutingChangeListeners.size() == 0) {
            native_enableDeviceCallback(enabled);
        }
    }

    public void addOnRoutingChangedListener(OnRoutingChangedListener listener, Handler handler) {
        synchronized (this.mRoutingChangeListeners) {
            if (listener != null) {
                try {
                    if (!this.mRoutingChangeListeners.containsKey(listener)) {
                        enableNativeRoutingCallbacksLocked(true);
                        ArrayMap arrayMap = this.mRoutingChangeListeners;
                        Handler handler2 = handler != null ? handler : this.mEventHandler;
                        arrayMap.put(listener, new NativeRoutingEventHandlerDelegate(this, listener, handler2));
                    }
                } finally {
                }
            }
        }
    }

    public void removeOnRoutingChangedListener(OnRoutingChangedListener listener) {
        synchronized (this.mRoutingChangeListeners) {
            if (this.mRoutingChangeListeners.containsKey(listener)) {
                this.mRoutingChangeListeners.remove(listener);
                enableNativeRoutingCallbacksLocked(false);
            }
        }
    }

    public void setWakeMode(Context context, int mode) {
        boolean washeld = false;
        if (SystemProperties.getBoolean("audio.offload.ignore_setawake", false)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IGNORING setWakeMode ");
            stringBuilder.append(mode);
            Log.w(str, stringBuilder.toString());
            return;
        }
        if (this.mWakeLock != null) {
            if (this.mWakeLock.isHeld()) {
                washeld = true;
                this.mWakeLock.release();
            }
            this.mWakeLock = null;
        }
        this.mWakeLock = ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).newWakeLock(536870912 | mode, MediaPlayer.class.getName());
        this.mWakeLock.setReferenceCounted(false);
        if (washeld) {
            this.mWakeLock.acquire();
        }
    }

    public void setScreenOnWhilePlaying(boolean screenOn) {
        if (this.mScreenOnWhilePlaying != screenOn) {
            if (screenOn && this.mSurfaceHolder == null) {
                Log.w(TAG, "setScreenOnWhilePlaying(true) is ineffective without a SurfaceHolder");
            }
            this.mScreenOnWhilePlaying = screenOn;
            updateSurfaceScreenOn();
        }
    }

    private void stayAwake(boolean awake) {
        if (this.mWakeLock != null) {
            if (awake && !this.mWakeLock.isHeld()) {
                this.mWakeLock.acquire();
            } else if (!awake && this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
        }
        this.mStayAwake = awake;
        String str;
        StringBuilder stringBuilder;
        if (this.mStayAwake) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("[HSM] stayAwake true uid: ");
            stringBuilder.append(Process.myUid());
            stringBuilder.append(", pid: ");
            stringBuilder.append(Process.myPid());
            Log.i(str, stringBuilder.toString());
            MediaTransactWrapper.musicPlaying(Process.myUid(), Process.myPid());
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("[HSM] stayAwake false uid: ");
            stringBuilder.append(Process.myUid());
            stringBuilder.append(", pid: ");
            stringBuilder.append(Process.myPid());
            Log.i(str, stringBuilder.toString());
            MediaTransactWrapper.musicPausedOrStopped(Process.myUid(), Process.myPid());
        }
        updateSurfaceScreenOn();
    }

    private void updateSurfaceScreenOn() {
        if (this.mSurfaceHolder != null) {
            SurfaceHolder surfaceHolder = this.mSurfaceHolder;
            boolean z = this.mScreenOnWhilePlaying && this.mStayAwake;
            surfaceHolder.setKeepScreenOn(z);
        }
    }

    public PersistableBundle getMetrics() {
        return native_getMetrics();
    }

    public PlaybackParams easyPlaybackParams(float rate, int audioMode) {
        PlaybackParams params = new PlaybackParams();
        params.allowDefaults();
        switch (audioMode) {
            case 0:
                params.setSpeed(rate).setPitch(1.0f);
                break;
            case 1:
                params.setSpeed(rate).setPitch(1.0f).setAudioFallbackMode(2);
                break;
            case 2:
                params.setSpeed(rate).setPitch(rate);
                break;
            default:
                String msg = new StringBuilder();
                msg.append("Audio playback mode ");
                msg.append(audioMode);
                msg.append(" is not supported");
                throw new IllegalArgumentException(msg.toString());
        }
        return params;
    }

    public void seekTo(long msec, int mode) {
        String msg;
        if (mode < 0 || mode > 3) {
            msg = new StringBuilder();
            msg.append("Illegal seek mode: ");
            msg.append(mode);
            throw new IllegalArgumentException(msg.toString());
        }
        StringBuilder stringBuilder;
        if (msec > 2147483647L) {
            msg = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("seekTo offset ");
            stringBuilder.append(msec);
            stringBuilder.append(" is too large, cap to ");
            stringBuilder.append(Integer.MAX_VALUE);
            Log.w(msg, stringBuilder.toString());
            msec = 2147483647L;
        } else if (msec < -2147483648L) {
            msg = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("seekTo offset ");
            stringBuilder.append(msec);
            stringBuilder.append(" is too small, cap to ");
            stringBuilder.append(Integer.MIN_VALUE);
            Log.w(msg, stringBuilder.toString());
            msec = -2147483648L;
        }
        _seekTo(msec, mode);
    }

    public void seekTo(int msec) throws IllegalStateException {
        seekTo((long) msec, 0);
    }

    public MediaTimestamp getTimestamp() {
        try {
            return new MediaTimestamp(((long) getCurrentPosition()) * 1000, System.nanoTime(), isPlaying() ? getPlaybackParams().getSpeed() : 0.0f);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    public Metadata getMetadata(boolean update_only, boolean apply_filter) {
        Parcel reply = Parcel.obtain();
        Metadata data = new Metadata();
        if (!native_getMetadata(update_only, apply_filter, reply)) {
            reply.recycle();
            return null;
        } else if (data.parse(reply)) {
            return data;
        } else {
            reply.recycle();
            return null;
        }
    }

    public int setMetadataFilter(Set<Integer> allow, Set<Integer> block) {
        Parcel request = newRequest();
        int capacity = request.dataSize() + (4 * (((allow.size() + 1) + 1) + block.size()));
        if (request.dataCapacity() < capacity) {
            request.setDataCapacity(capacity);
        }
        request.writeInt(allow.size());
        for (Integer t : allow) {
            request.writeInt(t.intValue());
        }
        request.writeInt(block.size());
        for (Integer t2 : block) {
            request.writeInt(t2.intValue());
        }
        return native_setMetadataFilter(request);
    }

    public void release() {
        baseRelease();
        stayAwake(false);
        updateSurfaceScreenOn();
        this.mOnPreparedListener = null;
        this.mOnBufferingUpdateListener = null;
        this.mOnCompletionListener = null;
        this.mOnSeekCompleteListener = null;
        this.mOnErrorListener = null;
        this.mOnInfoListener = null;
        this.mOnVideoSizeChangedListener = null;
        this.mOnTimedTextListener = null;
        if (this.mTimeProvider != null) {
            this.mTimeProvider.close();
            this.mTimeProvider = null;
        }
        synchronized (this) {
            this.mSubtitleDataListenerDisabled = false;
            this.mExtSubtitleDataListener = null;
            this.mExtSubtitleDataHandler = null;
            this.mOnMediaTimeDiscontinuityListener = null;
            this.mOnMediaTimeDiscontinuityHandler = null;
        }
        this.mOnDrmConfigHelper = null;
        this.mOnDrmInfoHandlerDelegate = null;
        this.mOnDrmPreparedHandlerDelegate = null;
        resetDrmState();
        _release();
        if (MEDIA_REPORT_PROP && this.mMediaReportRegister) {
            int res = HwFrameworkFactory.getHwCommBoosterServiceManager().unRegisterCallBack(MEDIA_REPORT_PKG_NAME, this.mIHwCommBoosterCallBack);
            if (res != 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unRegisterCallBack in release return error = ");
                stringBuilder.append(res);
                Log.e(str, stringBuilder.toString());
                return;
            }
            this.mMediaReportRegister = false;
        }
    }

    public void reset() {
        this.mSelectedSubtitleTrackIndex = -1;
        synchronized (this.mOpenSubtitleSources) {
            Iterator it = this.mOpenSubtitleSources.iterator();
            while (it.hasNext()) {
                try {
                    ((InputStream) it.next()).close();
                } catch (IOException e) {
                }
            }
            this.mOpenSubtitleSources.clear();
        }
        if (this.mSubtitleController != null) {
            this.mSubtitleController.reset();
        }
        if (this.mTimeProvider != null) {
            this.mTimeProvider.close();
            this.mTimeProvider = null;
        }
        stayAwake(false);
        _reset();
        if (this.mEventHandler != null) {
            this.mEventHandler.removeCallbacksAndMessages(null);
        }
        synchronized (this.mIndexTrackPairs) {
            this.mIndexTrackPairs.clear();
            this.mInbandTrackIndices.clear();
        }
        resetDrmState();
    }

    public void notifyAt(long mediaTimeUs) {
        _notifyAt(mediaTimeUs);
    }

    public void setAudioStreamType(int streamtype) {
        HwMediaMonitorManager.writeMediaBigData(Process.myPid(), HwMediaMonitorManager.getStreamBigDataType(streamtype), TAG);
        PlayerBase.deprecateStreamTypeForPlayback(streamtype, TAG, "setAudioStreamType()");
        baseUpdateAudioAttributes(new Builder().setInternalLegacyStreamType(streamtype).build());
        _setAudioStreamType(streamtype);
        this.mStreamType = streamtype;
    }

    public void setAudioAttributes(AudioAttributes attributes) throws IllegalArgumentException {
        if (attributes != null) {
            HwMediaMonitorManager.writeMediaBigData(Process.myPid(), HwMediaMonitorManager.getStreamBigDataType(AudioAttributes.toLegacyStreamType(attributes)), TAG);
            baseUpdateAudioAttributes(attributes);
            this.mUsage = attributes.getUsage();
            this.mBypassInterruptionPolicy = (attributes.getAllFlags() & 64) != 0;
            Parcel pattributes = Parcel.obtain();
            attributes.writeToParcel(pattributes, 1);
            setParameter(KEY_PARAMETER_AUDIO_ATTRIBUTES, pattributes);
            pattributes.recycle();
            return;
        }
        String msg = "Cannot set AudioAttributes to null";
        throw new IllegalArgumentException("Cannot set AudioAttributes to null");
    }

    public void setVolume(float leftVolume, float rightVolume) {
        baseSetVolume(leftVolume, rightVolume);
    }

    void playerSetVolume(boolean muting, float leftVolume, float rightVolume) {
        float f = 0.0f;
        float f2 = muting ? 0.0f : leftVolume;
        if (!muting) {
            f = rightVolume;
        }
        _setVolume(f2, f);
    }

    public void setVolume(float volume) {
        setVolume(volume, volume);
    }

    public void setAuxEffectSendLevel(float level) {
        baseSetAuxEffectSendLevel(level);
    }

    int playerSetAuxEffectSendLevel(boolean muting, float level) {
        _setAuxEffectSendLevel(muting ? 0.0f : level);
        return 0;
    }

    public TrackInfo[] getTrackInfo() throws IllegalStateException {
        TrackInfo[] allTrackInfo;
        TrackInfo[] trackInfo = getInbandTrackInfo();
        synchronized (this.mIndexTrackPairs) {
            allTrackInfo = new TrackInfo[this.mIndexTrackPairs.size()];
            for (int i = 0; i < allTrackInfo.length; i++) {
                Pair<Integer, SubtitleTrack> p = (Pair) this.mIndexTrackPairs.get(i);
                if (p.first != null) {
                    allTrackInfo[i] = trackInfo[((Integer) p.first).intValue()];
                } else {
                    SubtitleTrack track = p.second;
                    allTrackInfo[i] = new TrackInfo(track.getTrackType(), track.getFormat());
                }
            }
        }
        return allTrackInfo;
    }

    private TrackInfo[] getInbandTrackInfo() throws IllegalStateException {
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(1);
            invoke(request, reply);
            TrackInfo[] trackInfo = (TrackInfo[]) reply.createTypedArray(TrackInfo.CREATOR);
            return trackInfo;
        } finally {
            request.recycle();
            reply.recycle();
        }
    }

    private static boolean availableMimeTypeForExternalSource(String mimeType) {
        if ("application/x-subrip".equals(mimeType)) {
            return true;
        }
        return false;
    }

    public void setSubtitleAnchor(SubtitleController controller, Anchor anchor) {
        this.mSubtitleController = controller;
        this.mSubtitleController.setAnchor(anchor);
    }

    private synchronized void setSubtitleAnchor() {
        if (this.mSubtitleController == null && ActivityThread.currentApplication() != null) {
            final HandlerThread thread = new HandlerThread("SetSubtitleAnchorThread");
            thread.start();
            new Handler(thread.getLooper()).post(new Runnable() {
                public void run() {
                    MediaPlayer.this.mSubtitleController = new SubtitleController(ActivityThread.currentApplication(), MediaPlayer.this.mTimeProvider, MediaPlayer.this);
                    MediaPlayer.this.mSubtitleController.setAnchor(new Anchor() {
                        public void setSubtitleWidget(RenderingWidget subtitleWidget) {
                        }

                        public Looper getSubtitleLooper() {
                            return Looper.getMainLooper();
                        }
                    });
                    thread.getLooper().quitSafely();
                }
            });
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(TAG, "failed to join SetSubtitleAnchorThread");
            }
        }
        return;
    }

    public void onSubtitleTrackSelected(SubtitleTrack track) {
        if (this.mSelectedSubtitleTrackIndex >= 0) {
            try {
                selectOrDeselectInbandTrack(this.mSelectedSubtitleTrackIndex, false);
            } catch (IllegalStateException e) {
            }
            this.mSelectedSubtitleTrackIndex = -1;
        }
        synchronized (this) {
            this.mSubtitleDataListenerDisabled = true;
        }
        if (track != null) {
            synchronized (this.mIndexTrackPairs) {
                Iterator it = this.mIndexTrackPairs.iterator();
                while (it.hasNext()) {
                    Pair<Integer, SubtitleTrack> p = (Pair) it.next();
                    if (p.first != null && p.second == track) {
                        this.mSelectedSubtitleTrackIndex = ((Integer) p.first).intValue();
                        break;
                    }
                }
            }
            if (this.mSelectedSubtitleTrackIndex >= 0) {
                try {
                    selectOrDeselectInbandTrack(this.mSelectedSubtitleTrackIndex, true);
                } catch (IllegalStateException e2) {
                }
                synchronized (this) {
                    this.mSubtitleDataListenerDisabled = false;
                }
            }
        }
    }

    public void addSubtitleSource(InputStream is, MediaFormat format) throws IllegalStateException {
        final InputStream fIs = is;
        final MediaFormat fFormat = format;
        if (is != null) {
            synchronized (this.mOpenSubtitleSources) {
                this.mOpenSubtitleSources.add(is);
            }
        } else {
            Log.w(TAG, "addSubtitleSource called with null InputStream");
        }
        getMediaTimeProvider();
        final HandlerThread thread = new HandlerThread("SubtitleReadThread", 9);
        thread.start();
        new Handler(thread.getLooper()).post(new Runnable() {
            private int addTrack() {
                if (fIs == null || MediaPlayer.this.mSubtitleController == null) {
                    return 901;
                }
                SubtitleTrack track = MediaPlayer.this.mSubtitleController.addTrack(fFormat);
                if (track == null) {
                    return 901;
                }
                Scanner scanner = new Scanner(fIs, "UTF-8");
                String contents = scanner.useDelimiter("\\A").next();
                synchronized (MediaPlayer.this.mOpenSubtitleSources) {
                    MediaPlayer.this.mOpenSubtitleSources.remove(fIs);
                }
                scanner.close();
                synchronized (MediaPlayer.this.mIndexTrackPairs) {
                    MediaPlayer.this.mIndexTrackPairs.add(Pair.create(null, track));
                }
                Handler h = MediaPlayer.this.mTimeProvider.mEventHandler;
                h.sendMessage(h.obtainMessage(1, 4, null, Pair.create(track, contents.getBytes())));
                return 803;
            }

            public void run() {
                int res = addTrack();
                if (MediaPlayer.this.mEventHandler != null) {
                    MediaPlayer.this.mEventHandler.sendMessage(MediaPlayer.this.mEventHandler.obtainMessage(200, res, 0, null));
                }
                thread.getLooper().quitSafely();
            }
        });
    }

    private void scanInternalSubtitleTracks() {
        setSubtitleAnchor();
        populateInbandTracks();
        if (this.mSubtitleController != null) {
            this.mSubtitleController.selectDefaultTrack();
        }
    }

    private void populateInbandTracks() {
        TrackInfo[] tracks = getInbandTrackInfo();
        synchronized (this.mIndexTrackPairs) {
            int i = 0;
            while (i < tracks.length) {
                if (!this.mInbandTrackIndices.get(i)) {
                    this.mInbandTrackIndices.set(i);
                    if (tracks[i] == null) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("unexpected NULL track at index ");
                        stringBuilder.append(i);
                        Log.w(str, stringBuilder.toString());
                    }
                    if (tracks[i] == null || tracks[i].getTrackType() != 4) {
                        this.mIndexTrackPairs.add(Pair.create(Integer.valueOf(i), null));
                    } else {
                        this.mIndexTrackPairs.add(Pair.create(Integer.valueOf(i), this.mSubtitleController.addTrack(tracks[i].getFormat())));
                    }
                }
                i++;
            }
        }
    }

    public void addTimedTextSource(String path, String mimeType) throws IOException, IllegalArgumentException, IllegalStateException {
        if (availableMimeTypeForExternalSource(mimeType)) {
            File file = new File(path);
            if (file.exists()) {
                FileInputStream is = new FileInputStream(file);
                addTimedTextSource(is.getFD(), mimeType);
                is.close();
                return;
            }
            throw new IOException(path);
        }
        String msg = new StringBuilder();
        msg.append("Illegal mimeType for timed text source: ");
        msg.append(mimeType);
        throw new IllegalArgumentException(msg.toString());
    }

    /* JADX WARNING: Missing block: B:22:0x0039, code skipped:
            if (r1 == null) goto L_0x0047;
     */
    /* JADX WARNING: Missing block: B:23:0x003b, code skipped:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:25:0x0040, code skipped:
            if (r1 == null) goto L_0x0047;
     */
    /* JADX WARNING: Missing block: B:27:0x0044, code skipped:
            if (r1 == null) goto L_0x0047;
     */
    /* JADX WARNING: Missing block: B:28:0x0047, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addTimedTextSource(Context context, Uri uri, String mimeType) throws IOException, IllegalArgumentException, IllegalStateException {
        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals(ContentResolver.SCHEME_FILE)) {
            addTimedTextSource(uri.getPath(), mimeType);
            return;
        }
        AssetFileDescriptor fd = null;
        try {
            fd = context.getContentResolver().openAssetFileDescriptor(uri, FullBackup.ROOT_TREE_TOKEN);
            if (fd == null) {
                if (fd != null) {
                    fd.close();
                }
                return;
            }
            addTimedTextSource(fd.getFileDescriptor(), mimeType);
            if (fd != null) {
                fd.close();
            }
        } catch (SecurityException e) {
        } catch (IOException e2) {
        } catch (IllegalStateException e3) {
        } catch (Throwable th) {
            if (fd != null) {
                fd.close();
            }
        }
    }

    public void addTimedTextSource(FileDescriptor fd, String mimeType) throws IllegalArgumentException, IllegalStateException {
        addTimedTextSource(fd, 0, DataSourceDesc.LONG_MAX, mimeType);
    }

    public void addTimedTextSource(FileDescriptor fd, long offset, long length, String mime) throws IllegalArgumentException, IllegalStateException {
        String str = mime;
        if (availableMimeTypeForExternalSource(mime)) {
            try {
                final FileDescriptor dupedFd = Os.dup(fd);
                MediaFormat fFormat = new MediaFormat();
                fFormat.setString(MediaFormat.KEY_MIME, str);
                fFormat.setInteger(MediaFormat.KEY_IS_TIMED_TEXT, 1);
                if (this.mSubtitleController == null) {
                    setSubtitleAnchor();
                }
                if (!this.mSubtitleController.hasRendererFor(fFormat)) {
                    this.mSubtitleController.registerRenderer(new SRTRenderer(ActivityThread.currentApplication(), this.mEventHandler));
                }
                SubtitleTrack track = this.mSubtitleController.addTrack(fFormat);
                synchronized (this.mIndexTrackPairs) {
                    this.mIndexTrackPairs.add(Pair.create(null, track));
                }
                getMediaTimeProvider();
                final long offset2 = offset;
                final long length2 = length;
                HandlerThread thread = new HandlerThread("TimedTextReadThread", 9);
                thread.start();
                final SubtitleTrack subtitleTrack = track;
                final HandlerThread handlerThread = thread;
                new Handler(thread.getLooper()).post(new Runnable() {
                    private int addTrack() {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        try {
                            Os.lseek(dupedFd, offset2, OsConstants.SEEK_SET);
                            byte[] buffer = new byte[4096];
                            long total = 0;
                            while (total < length2) {
                                int bytes = IoBridge.read(dupedFd, buffer, 0, (int) Math.min((long) buffer.length, length2 - total));
                                if (bytes < 0) {
                                    break;
                                }
                                bos.write(buffer, 0, bytes);
                                total += (long) bytes;
                            }
                            Handler h = MediaPlayer.this.mTimeProvider.mEventHandler;
                            h.sendMessage(h.obtainMessage(1, 4, 0, Pair.create(subtitleTrack, bos.toByteArray())));
                            try {
                                Os.close(dupedFd);
                            } catch (ErrnoException e) {
                                Log.e(MediaPlayer.TAG, e.getMessage(), e);
                            }
                            return 803;
                        } catch (Exception e2) {
                            Log.e(MediaPlayer.TAG, e2.getMessage(), e2);
                            try {
                                Os.close(dupedFd);
                            } catch (ErrnoException e3) {
                                Log.e(MediaPlayer.TAG, e3.getMessage(), e3);
                            }
                            return 900;
                        } catch (Throwable th) {
                            try {
                                Os.close(dupedFd);
                            } catch (ErrnoException e4) {
                                Log.e(MediaPlayer.TAG, e4.getMessage(), e4);
                            }
                            throw th;
                        }
                    }

                    public void run() {
                        int res = addTrack();
                        if (MediaPlayer.this.mEventHandler != null) {
                            MediaPlayer.this.mEventHandler.sendMessage(MediaPlayer.this.mEventHandler.obtainMessage(200, res, 0, null));
                        }
                        handlerThread.getLooper().quitSafely();
                    }
                });
                return;
            } catch (ErrnoException ex) {
                ErrnoException errnoException = ex;
                Log.e(TAG, ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Illegal mimeType for timed text source: ");
        stringBuilder.append(str);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int getSelectedTrack(int trackType) throws IllegalStateException {
        int i;
        int i2 = 0;
        if (this.mSubtitleController != null && (trackType == 4 || trackType == 3)) {
            SubtitleTrack subtitleTrack = this.mSubtitleController.getSelectedTrack();
            if (subtitleTrack != null) {
                synchronized (this.mIndexTrackPairs) {
                    for (i = 0; i < this.mIndexTrackPairs.size(); i++) {
                        if (((Pair) this.mIndexTrackPairs.get(i)).second == subtitleTrack && subtitleTrack.getTrackType() == trackType) {
                            return i;
                        }
                    }
                }
            }
        }
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(7);
            request.writeInt(trackType);
            invoke(request, reply);
            i = reply.readInt();
            synchronized (this.mIndexTrackPairs) {
                while (i2 < this.mIndexTrackPairs.size()) {
                    Pair<Integer, SubtitleTrack> p = (Pair) this.mIndexTrackPairs.get(i2);
                    if (p.first == null || ((Integer) p.first).intValue() != i) {
                        i2++;
                    } else {
                        request.recycle();
                        reply.recycle();
                        return i2;
                    }
                }
                request.recycle();
                reply.recycle();
                return -1;
            }
        } catch (Throwable th) {
            request.recycle();
            reply.recycle();
        }
    }

    public void selectTrack(int index) throws IllegalStateException {
        selectOrDeselectTrack(index, true);
    }

    public void deselectTrack(int index) throws IllegalStateException {
        selectOrDeselectTrack(index, false);
    }

    private void selectOrDeselectTrack(int index, boolean select) throws IllegalStateException {
        populateInbandTracks();
        Pair<Integer, SubtitleTrack> p = null;
        try {
            p = (Pair) this.mIndexTrackPairs.get(index);
            SubtitleTrack track = p.second;
            if (track == null) {
                selectOrDeselectInbandTrack(((Integer) p.first).intValue(), select);
            } else if (this.mSubtitleController != null) {
                if (select) {
                    if (track.getTrackType() == 3) {
                        int ttIndex = getSelectedTrack(3);
                        synchronized (this.mIndexTrackPairs) {
                            if (ttIndex >= 0) {
                                try {
                                    if (ttIndex < this.mIndexTrackPairs.size()) {
                                        Pair<Integer, SubtitleTrack> p2 = (Pair) this.mIndexTrackPairs.get(ttIndex);
                                        if (p2.first != null && p2.second == null) {
                                            selectOrDeselectInbandTrack(((Integer) p2.first).intValue(), false);
                                        }
                                    }
                                } finally {
                                }
                            }
                        }
                    }
                    this.mSubtitleController.selectTrack(track);
                    return;
                }
                if (this.mSubtitleController.getSelectedTrack() == track) {
                    this.mSubtitleController.selectTrack(null);
                } else {
                    Log.w(TAG, "trying to deselect track that was not selected");
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    private void selectOrDeselectInbandTrack(int index, boolean select) throws IllegalStateException {
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(select ? 4 : 5);
            request.writeInt(index);
            invoke(request, reply);
        } finally {
            request.recycle();
            reply.recycle();
        }
    }

    public void setRetransmitEndpoint(InetSocketAddress endpoint) throws IllegalStateException, IllegalArgumentException {
        String addrString = null;
        int port = 0;
        if (endpoint != null) {
            addrString = endpoint.getAddress().getHostAddress();
            port = endpoint.getPort();
        }
        int ret = native_setRetransmitEndpoint(addrString, port);
        if (ret != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal re-transmit endpoint; native ret ");
            stringBuilder.append(ret);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    protected void finalize() {
        if (!this.isReleased) {
            baseRelease();
            native_finalize();
            if (MEDIA_REPORT_PROP && this.mMediaReportRegister) {
                int res = HwFrameworkFactory.getHwCommBoosterServiceManager().unRegisterCallBack(MEDIA_REPORT_PKG_NAME, this.mIHwCommBoosterCallBack);
                if (res != 0) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unRegisterCallBack in finalize return error = ");
                    stringBuilder.append(res);
                    Log.e(str, stringBuilder.toString());
                }
            }
        }
    }

    public MediaTimeProvider getMediaTimeProvider() {
        if (this.mTimeProvider == null) {
            this.mTimeProvider = new TimeProvider(this);
        }
        return this.mTimeProvider;
    }

    private static void postEventFromNative(Object mediaplayer_ref, int what, int arg1, int arg2, Object obj) {
        MediaPlayer mp = (MediaPlayer) ((WeakReference) mediaplayer_ref).get();
        if (mp != null) {
            if (what == 1) {
                synchronized (mp.mDrmLock) {
                    mp.mDrmInfoResolved = true;
                }
            } else if (what != 200) {
                if (what == 210) {
                    Log.v(TAG, "postEventFromNative MEDIA_DRM_INFO");
                    if (obj instanceof Parcel) {
                        DrmInfo drmInfo = new DrmInfo((Parcel) obj, null);
                        synchronized (mp.mDrmLock) {
                            mp.mDrmInfo = drmInfo;
                        }
                    } else {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("MEDIA_DRM_INFO msg.obj of unexpected type ");
                        stringBuilder.append(obj);
                        Log.w(str, stringBuilder.toString());
                    }
                }
            } else if (arg1 == 2) {
                new Thread(new Runnable() {
                    public void run() {
                        MediaPlayer.this.start();
                    }
                }).start();
                Thread.yield();
            }
            if (mp.mEventHandler != null) {
                mp.mEventHandler.sendMessage(mp.mEventHandler.obtainMessage(what, arg1, arg2, obj));
            }
        }
    }

    public void setOnPreparedListener(OnPreparedListener listener) {
        this.mOnPreparedListener = listener;
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        this.mOnCompletionListener = listener;
    }

    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        this.mOnBufferingUpdateListener = listener;
    }

    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        this.mOnSeekCompleteListener = listener;
    }

    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
        this.mOnVideoSizeChangedListener = listener;
    }

    public void setOnTimedTextListener(OnTimedTextListener listener) {
        this.mOnTimedTextListener = listener;
    }

    public void setOnSubtitleDataListener(OnSubtitleDataListener listener, Handler handler) {
        if (listener == null) {
            throw new IllegalArgumentException("Illegal null listener");
        } else if (handler != null) {
            setOnSubtitleDataListenerInt(listener, handler);
        } else {
            throw new IllegalArgumentException("Illegal null handler");
        }
    }

    public void setOnSubtitleDataListener(OnSubtitleDataListener listener) {
        if (listener != null) {
            setOnSubtitleDataListenerInt(listener, null);
            return;
        }
        throw new IllegalArgumentException("Illegal null listener");
    }

    public void clearOnSubtitleDataListener() {
        setOnSubtitleDataListenerInt(null, null);
    }

    private void setOnSubtitleDataListenerInt(OnSubtitleDataListener listener, Handler handler) {
        synchronized (this) {
            this.mExtSubtitleDataListener = listener;
            this.mExtSubtitleDataHandler = handler;
        }
    }

    public void setOnMediaTimeDiscontinuityListener(OnMediaTimeDiscontinuityListener listener, Handler handler) {
        if (listener == null) {
            throw new IllegalArgumentException("Illegal null listener");
        } else if (handler != null) {
            setOnMediaTimeDiscontinuityListenerInt(listener, handler);
        } else {
            throw new IllegalArgumentException("Illegal null handler");
        }
    }

    public void setOnMediaTimeDiscontinuityListener(OnMediaTimeDiscontinuityListener listener) {
        if (listener != null) {
            setOnMediaTimeDiscontinuityListenerInt(listener, null);
            return;
        }
        throw new IllegalArgumentException("Illegal null listener");
    }

    public void clearOnMediaTimeDiscontinuityListener() {
        setOnMediaTimeDiscontinuityListenerInt(null, null);
    }

    private void setOnMediaTimeDiscontinuityListenerInt(OnMediaTimeDiscontinuityListener listener, Handler handler) {
        synchronized (this) {
            this.mOnMediaTimeDiscontinuityListener = listener;
            this.mOnMediaTimeDiscontinuityHandler = handler;
        }
    }

    public void setOnTimedMetaDataAvailableListener(OnTimedMetaDataAvailableListener listener) {
        this.mOnTimedMetaDataAvailableListener = listener;
    }

    public void setOnErrorListener(OnErrorListener listener) {
        this.mOnErrorListener = listener;
    }

    public void setOnInfoListener(OnInfoListener listener) {
        this.mOnInfoListener = listener;
    }

    public void setOnDrmConfigHelper(OnDrmConfigHelper listener) {
        synchronized (this.mDrmLock) {
            this.mOnDrmConfigHelper = listener;
        }
    }

    public void setOnDrmInfoListener(OnDrmInfoListener listener) {
        setOnDrmInfoListener(listener, null);
    }

    public void setOnDrmInfoListener(OnDrmInfoListener listener, Handler handler) {
        synchronized (this.mDrmLock) {
            if (listener != null) {
                try {
                    this.mOnDrmInfoHandlerDelegate = new OnDrmInfoHandlerDelegate(this, listener, handler);
                } catch (Throwable th) {
                }
            } else {
                this.mOnDrmInfoHandlerDelegate = null;
            }
        }
    }

    public void setOnDrmPreparedListener(OnDrmPreparedListener listener) {
        setOnDrmPreparedListener(listener, null);
    }

    public void setOnDrmPreparedListener(OnDrmPreparedListener listener, Handler handler) {
        synchronized (this.mDrmLock) {
            if (listener != null) {
                try {
                    this.mOnDrmPreparedHandlerDelegate = new OnDrmPreparedHandlerDelegate(this, listener, handler);
                } catch (Throwable th) {
                }
            } else {
                this.mOnDrmPreparedHandlerDelegate = null;
            }
        }
    }

    public DrmInfo getDrmInfo() {
        DrmInfo drmInfo = null;
        synchronized (this.mDrmLock) {
            if (!this.mDrmInfoResolved) {
                if (this.mDrmInfo == null) {
                    String msg = "The Player has not been prepared yet";
                    Log.v(TAG, "The Player has not been prepared yet");
                    throw new IllegalStateException("The Player has not been prepared yet");
                }
            }
            if (this.mDrmInfo != null) {
                drmInfo = this.mDrmInfo.makeCopy();
            }
        }
        return drmInfo;
    }

    /* JADX WARNING: Missing block: B:33:0x0064, code skipped:
            if (r2 != false) goto L_0x0066;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void prepareDrm(UUID uuid) throws UnsupportedSchemeException, ResourceBusyException, ProvisioningNetworkErrorException, ProvisioningServerErrorException {
        OnDrmPreparedHandlerDelegate onDrmPreparedHandlerDelegate;
        String msg;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("prepareDrm: uuid: ");
        stringBuilder.append(uuid);
        stringBuilder.append(" mOnDrmConfigHelper: ");
        stringBuilder.append(this.mOnDrmConfigHelper);
        Log.v(str, stringBuilder.toString());
        boolean allDoneWithoutProvisioning = false;
        synchronized (this.mDrmLock) {
            String msg2;
            if (this.mDrmInfo == null) {
                msg2 = "prepareDrm(): Wrong usage: The player must be prepared and DRM info be retrieved before this call.";
                Log.e(TAG, "prepareDrm(): Wrong usage: The player must be prepared and DRM info be retrieved before this call.");
                throw new IllegalStateException("prepareDrm(): Wrong usage: The player must be prepared and DRM info be retrieved before this call.");
            } else if (this.mActiveDrmScheme) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("prepareDrm(): Wrong usage: There is already an active DRM scheme with ");
                stringBuilder2.append(this.mDrmUUID);
                msg2 = stringBuilder2.toString();
                Log.e(TAG, msg2);
                throw new IllegalStateException(msg2);
            } else if (this.mPrepareDrmInProgress) {
                msg2 = "prepareDrm(): Wrong usage: There is already a pending prepareDrm call.";
                Log.e(TAG, "prepareDrm(): Wrong usage: There is already a pending prepareDrm call.");
                throw new IllegalStateException("prepareDrm(): Wrong usage: There is already a pending prepareDrm call.");
            } else if (this.mDrmProvisioningInProgress) {
                msg2 = "prepareDrm(): Unexpectd: Provisioning is already in progress.";
                Log.e(TAG, "prepareDrm(): Unexpectd: Provisioning is already in progress.");
                throw new IllegalStateException("prepareDrm(): Unexpectd: Provisioning is already in progress.");
            } else {
                cleanDrmObj();
                this.mPrepareDrmInProgress = true;
                onDrmPreparedHandlerDelegate = this.mOnDrmPreparedHandlerDelegate;
                try {
                    prepareDrm_createDrmStep(uuid);
                    this.mDrmConfigAllowed = true;
                } catch (Exception e) {
                    Log.w(TAG, "prepareDrm(): Exception ", e);
                    this.mPrepareDrmInProgress = false;
                    throw e;
                }
            }
        }
        if (this.mOnDrmConfigHelper != null) {
            this.mOnDrmConfigHelper.onDrmConfig(this);
        }
        synchronized (this.mDrmLock) {
            this.mDrmConfigAllowed = false;
            boolean earlyExit = false;
            try {
                prepareDrm_openSessionStep(uuid);
                this.mDrmUUID = uuid;
                this.mActiveDrmScheme = true;
                allDoneWithoutProvisioning = true;
                if (!this.mDrmProvisioningInProgress) {
                    this.mPrepareDrmInProgress = false;
                }
            } catch (IllegalStateException e2) {
                msg = "prepareDrm(): Wrong usage: The player must be in the prepared state to call prepareDrm().";
                Log.e(TAG, "prepareDrm(): Wrong usage: The player must be in the prepared state to call prepareDrm().");
                throw new IllegalStateException("prepareDrm(): Wrong usage: The player must be in the prepared state to call prepareDrm().");
            } catch (NotProvisionedException e3) {
                Log.w(TAG, "prepareDrm: NotProvisionedException");
                int result = HandleProvisioninig(uuid);
                if (result != 0) {
                    String msg3;
                    switch (result) {
                        case 1:
                            msg3 = "prepareDrm: Provisioning was required but failed due to a network error.";
                            Log.e(TAG, msg3);
                            throw new ProvisioningNetworkErrorException(msg3);
                        case 2:
                            msg3 = "prepareDrm: Provisioning was required but the request was denied by the server.";
                            Log.e(TAG, msg3);
                            throw new ProvisioningServerErrorException(msg3);
                        default:
                            msg3 = "prepareDrm: Post-provisioning preparation failed.";
                            Log.e(TAG, msg3);
                            throw new IllegalStateException(msg3);
                    }
                }
                if (!this.mDrmProvisioningInProgress) {
                    this.mPrepareDrmInProgress = false;
                }
                if (earlyExit) {
                    cleanDrmObj();
                }
                if (allDoneWithoutProvisioning) {
                    return;
                }
            } catch (Exception e4) {
                msg = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("prepareDrm: Exception ");
                stringBuilder3.append(e4);
                Log.e(msg, stringBuilder3.toString());
                throw e4;
            } catch (Throwable th) {
                if (!this.mDrmProvisioningInProgress) {
                    this.mPrepareDrmInProgress = false;
                }
                if (earlyExit) {
                    cleanDrmObj();
                }
            }
        }
        if (allDoneWithoutProvisioning && onDrmPreparedHandlerDelegate != null) {
            onDrmPreparedHandlerDelegate.notifyClient(0);
        }
    }

    public void releaseDrm() throws NoDrmSchemeException {
        Log.v(TAG, "releaseDrm:");
        synchronized (this.mDrmLock) {
            if (this.mActiveDrmScheme) {
                try {
                    _releaseDrm();
                    cleanDrmObj();
                    this.mActiveDrmScheme = false;
                } catch (IllegalStateException e) {
                    Log.w(TAG, "releaseDrm: Exception ", e);
                    throw new IllegalStateException("releaseDrm: The player is not in a valid state.");
                } catch (Exception e2) {
                    Log.e(TAG, "releaseDrm: Exception ", e2);
                }
            } else {
                Log.e(TAG, "releaseDrm(): No active DRM scheme to release.");
                throw new NoDrmSchemeException("releaseDrm: No active DRM scheme to release.");
            }
        }
    }

    public KeyRequest getKeyRequest(byte[] keySetId, byte[] initData, String mimeType, int keyType, Map<String, String> optionalParameters) throws NoDrmSchemeException {
        String str;
        NotProvisionedException e;
        String str2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getKeyRequest:  keySetId: ");
        stringBuilder.append(keySetId);
        stringBuilder.append(" initData:");
        stringBuilder.append(initData);
        stringBuilder.append(" mimeType: ");
        stringBuilder.append(mimeType);
        stringBuilder.append(" keyType: ");
        stringBuilder.append(keyType);
        stringBuilder.append(" optionalParameters: ");
        stringBuilder.append(optionalParameters);
        Log.v(str2, stringBuilder.toString());
        synchronized (this.mDrmLock) {
            if (this.mActiveDrmScheme) {
                byte[] scope;
                if (keyType != 3) {
                    try {
                        scope = this.mDrmSessionId;
                    } catch (NotProvisionedException e2) {
                        Log.w(TAG, "getKeyRequest NotProvisionedException: Unexpected. Shouldn't have reached here.");
                        throw new IllegalStateException("getKeyRequest: Unexpected provisioning error.");
                    } catch (Exception e3) {
                        str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("getKeyRequest Exception ");
                        stringBuilder2.append(e3);
                        Log.w(str, stringBuilder2.toString());
                        throw e3;
                    }
                }
                scope = keySetId;
                if (optionalParameters != null) {
                    e3 = new HashMap(optionalParameters);
                } else {
                    e3 = null;
                }
                e3 = this.mDrmObj.getKeyRequest(scope, initData, mimeType, keyType, e3);
                str = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("getKeyRequest:   --> request: ");
                stringBuilder3.append(e3);
                Log.v(str, stringBuilder3.toString());
            } else {
                Log.e(TAG, "getKeyRequest NoDrmSchemeException");
                throw new NoDrmSchemeException("getKeyRequest: Has to set a DRM scheme first.");
            }
        }
        return e3;
    }

    public byte[] provideKeyResponse(byte[] keySetId, byte[] response) throws NoDrmSchemeException, DeniedByServerException {
        NotProvisionedException e;
        byte[] keySetResult;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("provideKeyResponse: keySetId: ");
        stringBuilder.append(keySetId);
        stringBuilder.append(" response: ");
        stringBuilder.append(response);
        Log.v(str, stringBuilder.toString());
        synchronized (this.mDrmLock) {
            if (this.mActiveDrmScheme) {
                if (keySetId == null) {
                    try {
                        e = this.mDrmSessionId;
                    } catch (NotProvisionedException e2) {
                        Log.w(TAG, "provideKeyResponse NotProvisionedException: Unexpected. Shouldn't have reached here.");
                        throw new IllegalStateException("provideKeyResponse: Unexpected provisioning error.");
                    } catch (Exception e3) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("provideKeyResponse Exception ");
                        stringBuilder2.append(e3);
                        Log.w(str2, stringBuilder2.toString());
                        throw e3;
                    }
                }
                e3 = keySetId;
                keySetResult = this.mDrmObj.provideKeyResponse(e3, response);
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("provideKeyResponse: keySetId: ");
                stringBuilder3.append(keySetId);
                stringBuilder3.append(" response: ");
                stringBuilder3.append(response);
                stringBuilder3.append(" --> ");
                stringBuilder3.append(keySetResult);
                Log.v(str3, stringBuilder3.toString());
            } else {
                Log.e(TAG, "getKeyRequest NoDrmSchemeException");
                throw new NoDrmSchemeException("getKeyRequest: Has to set a DRM scheme first.");
            }
        }
        return keySetResult;
    }

    public void restoreKeys(byte[] keySetId) throws NoDrmSchemeException {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("restoreKeys: keySetId: ");
        stringBuilder.append(keySetId);
        Log.v(str, stringBuilder.toString());
        synchronized (this.mDrmLock) {
            if (this.mActiveDrmScheme) {
                try {
                    this.mDrmObj.restoreKeys(this.mDrmSessionId, keySetId);
                } catch (Exception e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("restoreKeys Exception ");
                    stringBuilder2.append(e);
                    Log.w(str2, stringBuilder2.toString());
                    throw e;
                }
            }
            Log.w(TAG, "restoreKeys NoDrmSchemeException");
            throw new NoDrmSchemeException("restoreKeys: Has to set a DRM scheme first.");
        }
    }

    public String getDrmPropertyString(String propertyName) throws NoDrmSchemeException {
        String value;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getDrmPropertyString: propertyName: ");
        stringBuilder.append(propertyName);
        Log.v(str, stringBuilder.toString());
        synchronized (this.mDrmLock) {
            if (!this.mActiveDrmScheme) {
                if (!this.mDrmConfigAllowed) {
                    Log.w(TAG, "getDrmPropertyString NoDrmSchemeException");
                    throw new NoDrmSchemeException("getDrmPropertyString: Has to prepareDrm() first.");
                }
            }
            try {
                value = this.mDrmObj.getPropertyString(propertyName);
            } catch (Exception e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getDrmPropertyString Exception ");
                stringBuilder2.append(e);
                Log.w(str2, stringBuilder2.toString());
                throw e;
            }
        }
        str = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("getDrmPropertyString: propertyName: ");
        stringBuilder3.append(propertyName);
        stringBuilder3.append(" --> value: ");
        stringBuilder3.append(value);
        Log.v(str, stringBuilder3.toString());
        return value;
    }

    public void setDrmPropertyString(String propertyName, String value) throws NoDrmSchemeException {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setDrmPropertyString: propertyName: ");
        stringBuilder.append(propertyName);
        stringBuilder.append(" value: ");
        stringBuilder.append(value);
        Log.v(str, stringBuilder.toString());
        synchronized (this.mDrmLock) {
            if (!this.mActiveDrmScheme) {
                if (!this.mDrmConfigAllowed) {
                    Log.w(TAG, "setDrmPropertyString NoDrmSchemeException");
                    throw new NoDrmSchemeException("setDrmPropertyString: Has to prepareDrm() first.");
                }
            }
            try {
                this.mDrmObj.setPropertyString(propertyName, value);
            } catch (Exception e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setDrmPropertyString Exception ");
                stringBuilder2.append(e);
                Log.w(str2, stringBuilder2.toString());
                throw e;
            }
        }
    }

    private void prepareDrm_createDrmStep(UUID uuid) throws UnsupportedSchemeException {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("prepareDrm_createDrmStep: UUID: ");
        stringBuilder.append(uuid);
        Log.v(str, stringBuilder.toString());
        try {
            this.mDrmObj = new MediaDrm(uuid);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("prepareDrm_createDrmStep: Created mDrmObj=");
            stringBuilder.append(this.mDrmObj);
            Log.v(str, stringBuilder.toString());
        } catch (Exception e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("prepareDrm_createDrmStep: MediaDrm failed with ");
            stringBuilder.append(e);
            Log.e(TAG, stringBuilder.toString());
            throw e;
        }
    }

    private void prepareDrm_openSessionStep(UUID uuid) throws NotProvisionedException, ResourceBusyException {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("prepareDrm_openSessionStep: uuid: ");
        stringBuilder.append(uuid);
        Log.v(str, stringBuilder.toString());
        try {
            this.mDrmSessionId = this.mDrmObj.openSession();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("prepareDrm_openSessionStep: mDrmSessionId=");
            stringBuilder.append(this.mDrmSessionId);
            Log.v(str, stringBuilder.toString());
            _prepareDrm(getByteArrayFromUUID(uuid), this.mDrmSessionId);
            Log.v(TAG, "prepareDrm_openSessionStep: _prepareDrm/Crypto succeeded");
        } catch (Exception e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("prepareDrm_openSessionStep: open/crypto failed with ");
            stringBuilder.append(e);
            Log.e(TAG, stringBuilder.toString());
            throw e;
        }
    }

    private int HandleProvisioninig(UUID uuid) {
        if (this.mDrmProvisioningInProgress) {
            Log.e(TAG, "HandleProvisioninig: Unexpected mDrmProvisioningInProgress");
            return 3;
        }
        ProvisionRequest provReq = this.mDrmObj.getProvisionRequest();
        if (provReq == null) {
            Log.e(TAG, "HandleProvisioninig: getProvisionRequest returned null.");
            return 3;
        }
        int result;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HandleProvisioninig provReq  data: ");
        stringBuilder.append(provReq.getData());
        stringBuilder.append(" url: ");
        stringBuilder.append(provReq.getDefaultUrl());
        Log.v(str, stringBuilder.toString());
        this.mDrmProvisioningInProgress = true;
        this.mDrmProvisioningThread = new ProvisioningThread(this, null).initialize(provReq, uuid, this);
        this.mDrmProvisioningThread.start();
        if (this.mOnDrmPreparedHandlerDelegate != null) {
            result = 0;
        } else {
            try {
                this.mDrmProvisioningThread.join();
            } catch (Exception e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("HandleProvisioninig: Thread.join Exception ");
                stringBuilder2.append(e);
                Log.w(str2, stringBuilder2.toString());
            }
            result = this.mDrmProvisioningThread.status();
            this.mDrmProvisioningThread = null;
        }
        return result;
    }

    private boolean resumePrepareDrm(UUID uuid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("resumePrepareDrm: uuid: ");
        stringBuilder.append(uuid);
        Log.v(str, stringBuilder.toString());
        try {
            prepareDrm_openSessionStep(uuid);
            this.mDrmUUID = uuid;
            this.mActiveDrmScheme = true;
            return true;
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("HandleProvisioninig: Thread run _prepareDrm resume failed with ");
            stringBuilder2.append(e);
            Log.w(str2, stringBuilder2.toString());
            return false;
        }
    }

    private void resetDrmState() {
        synchronized (this.mDrmLock) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("resetDrmState:  mDrmInfo=");
            stringBuilder.append(this.mDrmInfo);
            stringBuilder.append(" mDrmProvisioningThread=");
            stringBuilder.append(this.mDrmProvisioningThread);
            stringBuilder.append(" mPrepareDrmInProgress=");
            stringBuilder.append(this.mPrepareDrmInProgress);
            stringBuilder.append(" mActiveDrmScheme=");
            stringBuilder.append(this.mActiveDrmScheme);
            Log.v(str, stringBuilder.toString());
            this.mDrmInfoResolved = false;
            this.mDrmInfo = null;
            if (this.mDrmProvisioningThread != null) {
                try {
                    this.mDrmProvisioningThread.join();
                } catch (InterruptedException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("resetDrmState: ProvThread.join Exception ");
                    stringBuilder2.append(e);
                    Log.w(str2, stringBuilder2.toString());
                }
                this.mDrmProvisioningThread = null;
            }
            this.mPrepareDrmInProgress = false;
            this.mActiveDrmScheme = false;
            cleanDrmObj();
        }
    }

    private void cleanDrmObj() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cleanDrmObj: mDrmObj=");
        stringBuilder.append(this.mDrmObj);
        stringBuilder.append(" mDrmSessionId=");
        stringBuilder.append(this.mDrmSessionId);
        Log.v(str, stringBuilder.toString());
        if (this.mDrmSessionId != null) {
            this.mDrmObj.closeSession(this.mDrmSessionId);
            this.mDrmSessionId = null;
        }
        if (this.mDrmObj != null) {
            this.mDrmObj.release();
            this.mDrmObj = null;
        }
    }

    private static final byte[] getByteArrayFromUUID(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] uuidBytes = new byte[16];
        for (int i = 0; i < 8; i++) {
            uuidBytes[i] = (byte) ((int) (msb >>> ((7 - i) * 8)));
            uuidBytes[8 + i] = (byte) ((int) (lsb >>> (8 * (7 - i))));
        }
        return uuidBytes;
    }

    private boolean isVideoScalingModeSupported(int mode) {
        return mode == 1 || mode == 2;
    }
}
