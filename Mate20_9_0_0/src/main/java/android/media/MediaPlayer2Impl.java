package android.media;

import android.app.ActivityThread;
import android.app.backup.FullBackup;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioRouting.OnRoutingChangedListener;
import android.media.MediaDrm.KeyRequest;
import android.media.MediaDrm.ProvisionRequest;
import android.media.MediaPlayer2.DrmEventCallback;
import android.media.MediaPlayer2.DrmInfo;
import android.media.MediaPlayer2.MediaPlayer2EventCallback;
import android.media.MediaPlayer2.NoDrmSchemeException;
import android.media.MediaPlayer2.OnDrmConfigHelper;
import android.media.MediaPlayer2.OnSubtitleDataListener;
import android.media.MediaPlayer2.ProvisioningNetworkErrorException;
import android.media.MediaPlayer2.ProvisioningServerErrorException;
import android.media.MediaPlayer2.TrackInfo;
import android.media.MediaPlayerBase.PlayerEventCallback;
import android.media.MediaTimeProvider.OnMediaTimeListener;
import android.media.SubtitleController.Anchor;
import android.media.SubtitleTrack.RenderingWidget;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;
import android.view.SurfaceHolder;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import dalvik.system.CloseGuard;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.io.IoBridge;
import libcore.io.Streams;

public final class MediaPlayer2Impl extends MediaPlayer2 {
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
    private static final int MEDIA_DRM_INFO = 210;
    private static final int MEDIA_ERROR = 100;
    private static final int MEDIA_INFO = 200;
    private static final int MEDIA_META_DATA = 202;
    private static final int MEDIA_NOP = 0;
    private static final int MEDIA_NOTIFY_TIME = 98;
    private static final int MEDIA_PAUSED = 7;
    private static final int MEDIA_PLAYBACK_COMPLETE = 2;
    private static final int MEDIA_PREPARED = 1;
    private static final int MEDIA_SEEK_COMPLETE = 4;
    private static final int MEDIA_SET_VIDEO_SIZE = 5;
    private static final int MEDIA_SKIPPED = 9;
    private static final int MEDIA_STARTED = 6;
    private static final int MEDIA_STOPPED = 8;
    private static final int MEDIA_SUBTITLE_DATA = 201;
    private static final int MEDIA_TIMED_TEXT = 99;
    private static final int NEXT_SOURCE_STATE_ERROR = -1;
    private static final int NEXT_SOURCE_STATE_INIT = 0;
    private static final int NEXT_SOURCE_STATE_PREPARED = 2;
    private static final int NEXT_SOURCE_STATE_PREPARING = 1;
    private static final String TAG = "MediaPlayer2Impl";
    private boolean mActiveDrmScheme;
    private AtomicInteger mBufferedPercentageCurrent;
    private AtomicInteger mBufferedPercentageNext;
    private DataSourceDesc mCurrentDSD;
    private long mCurrentSrcId;
    @GuardedBy("mTaskLock")
    private Task mCurrentTask;
    private boolean mDrmConfigAllowed;
    private ArrayList<Pair<Executor, DrmEventCallback>> mDrmEventCallbackRecords;
    private final Object mDrmEventCbLock;
    private DrmInfoImpl mDrmInfoImpl;
    private boolean mDrmInfoResolved;
    private final Object mDrmLock;
    private MediaDrm mDrmObj;
    private boolean mDrmProvisioningInProgress;
    private ProvisioningThread mDrmProvisioningThread;
    private byte[] mDrmSessionId;
    private UUID mDrmUUID;
    private ArrayList<Pair<Executor, MediaPlayer2EventCallback>> mEventCallbackRecords;
    private final Object mEventCbLock;
    private EventHandler mEventHandler;
    private final CloseGuard mGuard = CloseGuard.get();
    private HandlerThread mHandlerThread;
    private BitSet mInbandTrackIndices;
    private Vector<Pair<Integer, SubtitleTrack>> mIndexTrackPairs;
    private int mListenerContext;
    private long mNativeContext;
    private long mNativeSurfaceTexture;
    private List<DataSourceDesc> mNextDSDs;
    private boolean mNextSourcePlayPending;
    private int mNextSourceState;
    private long mNextSrcId;
    private OnDrmConfigHelper mOnDrmConfigHelper;
    private OnSubtitleDataListener mOnSubtitleDataListener;
    private Vector<InputStream> mOpenSubtitleSources;
    @GuardedBy("mTaskLock")
    private final List<Task> mPendingTasks;
    private AudioDeviceInfo mPreferredDevice;
    private boolean mPrepareDrmInProgress;
    @GuardedBy("mRoutingChangeListeners")
    private ArrayMap<OnRoutingChangedListener, NativeRoutingEventHandlerDelegate> mRoutingChangeListeners;
    private boolean mScreenOnWhilePlaying;
    private int mSelectedSubtitleTrackIndex;
    private long mSrcIdGenerator = 0;
    private final Object mSrcLock = new Object();
    private boolean mStayAwake;
    private int mStreamType = Integer.MIN_VALUE;
    private SubtitleController mSubtitleController;
    private OnSubtitleDataListener mSubtitleDataListener;
    private SurfaceHolder mSurfaceHolder;
    private final Handler mTaskHandler;
    private final Object mTaskLock;
    private TimeProvider mTimeProvider;
    private volatile float mVolume;
    private WakeLock mWakeLock = null;

    private class ProvisioningThread extends Thread {
        public static final int TIMEOUT_MS = 60000;
        private Object drmLock;
        private boolean finished;
        private MediaPlayer2Impl mediaPlayer;
        private int status;
        private String urlStr;
        private UUID uuid;

        private ProvisioningThread() {
        }

        /* synthetic */ ProvisioningThread(MediaPlayer2Impl x0, AnonymousClass1 x1) {
            this();
        }

        public int status() {
            return this.status;
        }

        public ProvisioningThread initialize(ProvisionRequest request, UUID uuid, MediaPlayer2Impl mediaPlayer) {
            this.drmLock = mediaPlayer.mDrmLock;
            this.mediaPlayer = mediaPlayer;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(request.getDefaultUrl());
            stringBuilder.append("&signedRequest=");
            stringBuilder.append(new String(request.getData()));
            this.urlStr = stringBuilder.toString();
            this.uuid = uuid;
            this.status = 3;
            String str = MediaPlayer2Impl.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("HandleProvisioninig: Thread is initialised url: ");
            stringBuilder2.append(this.urlStr);
            Log.v(str, stringBuilder2.toString());
            return this;
        }

        public void run() {
            String str;
            StringBuilder stringBuilder;
            boolean hasCallback;
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
                    String str2 = MediaPlayer2Impl.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("HandleProvisioninig: Thread run: response ");
                    stringBuilder2.append(response.length);
                    stringBuilder2.append(" ");
                    stringBuilder2.append(response);
                    Log.v(str2, stringBuilder2.toString());
                    connection.disconnect();
                } catch (Exception e) {
                    this.status = 1;
                    String str3 = MediaPlayer2Impl.TAG;
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
                str = MediaPlayer2Impl.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("HandleProvisioninig: Thread run: openConnection ");
                stringBuilder.append(e2);
                Log.w(str, stringBuilder.toString());
            } catch (Throwable th) {
                connection.disconnect();
            }
            if (response != null) {
                try {
                    MediaPlayer2Impl.this.mDrmObj.provideProvisionResponse(response);
                    Log.v(MediaPlayer2Impl.TAG, "HandleProvisioninig: Thread run: provideProvisionResponse SUCCEEDED!");
                    provisioningSucceeded = true;
                } catch (Exception e22) {
                    this.status = 2;
                    str = MediaPlayer2Impl.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HandleProvisioninig: Thread run: provideProvisionResponse ");
                    stringBuilder.append(e22);
                    Log.w(str, stringBuilder.toString());
                }
            }
            boolean succeeded = false;
            synchronized (MediaPlayer2Impl.this.mDrmEventCbLock) {
                hasCallback = MediaPlayer2Impl.this.mDrmEventCallbackRecords.isEmpty() ^ 1;
            }
            int i = 3;
            if (hasCallback) {
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
                        MediaPlayer2Impl.this.cleanDrmObj();
                    }
                }
                synchronized (MediaPlayer2Impl.this.mDrmEventCbLock) {
                    Iterator it = MediaPlayer2Impl.this.mDrmEventCallbackRecords.iterator();
                    while (it.hasNext()) {
                        Pair<Executor, DrmEventCallback> cb = (Pair) it.next();
                        ((Executor) cb.first).execute(new -$$Lambda$MediaPlayer2Impl$ProvisioningThread$ghq9Dd9r2O6PXBn2hv4fhVAxaTQ(this, cb));
                    }
                }
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
                    MediaPlayer2Impl.this.cleanDrmObj();
                }
            }
            this.finished = true;
        }
    }

    private abstract class Task implements Runnable {
        private DataSourceDesc mDSD;
        private final int mMediaCallType;
        private final boolean mNeedToWaitForEventToComplete;

        abstract void process() throws IOException, NoDrmSchemeException;

        public Task(int mediaCallType, boolean needToWaitForEventToComplete) {
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
            } catch (NoDrmSchemeException e5) {
                status = 5;
            } catch (Exception e6) {
                status = Integer.MIN_VALUE;
            }
            synchronized (MediaPlayer2Impl.this.mSrcLock) {
                this.mDSD = MediaPlayer2Impl.this.mCurrentDSD;
            }
            if (!this.mNeedToWaitForEventToComplete || status != 0) {
                sendCompleteNotification(status);
                synchronized (MediaPlayer2Impl.this.mTaskLock) {
                    MediaPlayer2Impl.this.mCurrentTask = null;
                    MediaPlayer2Impl.this.processPendingTask_l();
                }
            }
        }

        private void sendCompleteNotification(int status) {
            if (this.mMediaCallType != 1003) {
                synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                    Iterator it = MediaPlayer2Impl.this.mEventCallbackRecords.iterator();
                    while (it.hasNext()) {
                        Pair<Executor, MediaPlayer2EventCallback> cb = (Pair) it.next();
                        ((Executor) cb.first).execute(new -$$Lambda$MediaPlayer2Impl$Task$FRvdJ9PUPHSq0Jucj91aL6zYEJY(this, cb, status));
                    }
                }
            }
        }
    }

    public static final class DrmInfoImpl extends DrmInfo {
        private Map<UUID, byte[]> mapPssh;
        private UUID[] supportedSchemes;

        public Map<UUID, byte[]> getPssh() {
            return this.mapPssh;
        }

        public List<UUID> getSupportedSchemes() {
            return Arrays.asList(this.supportedSchemes);
        }

        private DrmInfoImpl(Map<UUID, byte[]> Pssh, UUID[] SupportedSchemes) {
            this.mapPssh = Pssh;
            this.supportedSchemes = SupportedSchemes;
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
            this.mapPssh = parsePSSH(pssh, psshsize);
            str2 = MediaPlayer2Impl.TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("DrmInfoImpl() PSSH: ");
            stringBuilder2.append(this.mapPssh);
            Log.v(str2, stringBuilder2.toString());
            int supportedDRMsCount = parcel.readInt();
            this.supportedSchemes = new UUID[supportedDRMsCount];
            for (int i = 0; i < supportedDRMsCount; i++) {
                byte[] uuid = new byte[16];
                parcel.readByteArray(uuid);
                this.supportedSchemes[i] = bytesToUUID(uuid);
                String str3 = MediaPlayer2Impl.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("DrmInfoImpl() supportedScheme[");
                stringBuilder3.append(i);
                stringBuilder3.append("]: ");
                stringBuilder3.append(this.supportedSchemes[i]);
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
            return new DrmInfoImpl(this.mapPssh, this.supportedSchemes);
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
                byte[] subset = Arrays.copyOfRange(bArr, i2, i2 + 4);
                if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                    datalen = ((((subset[3] & 255) << 24) | ((subset[2] & 255) << 16)) | ((subset[1] & 255) << 8)) | (subset[0] & 255);
                } else {
                    datalen = ((((subset[0] & 255) << 24) | ((subset[1] & 255) << 16)) | ((subset[2] & 255) << 8)) | (subset[3] & 255);
                }
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

    private class EventHandler extends Handler {
        private MediaPlayer2Impl mMediaPlayer;

        public EventHandler(MediaPlayer2Impl mp, Looper looper) {
            super(looper);
            this.mMediaPlayer = mp;
        }

        public void handleMessage(Message msg) {
            handleMessage(msg, 0);
        }

        /* JADX WARNING: Missing block: B:134:0x02a4, code skipped:
            r2 = android.media.MediaPlayer2Impl.access$3300(r11.this$0);
     */
        /* JADX WARNING: Missing block: B:135:0x02aa, code skipped:
            if (r2 == null) goto L_0x02b1;
     */
        /* JADX WARNING: Missing block: B:136:0x02ac, code skipped:
            r2.onSeekComplete(r11.mMediaPlayer);
     */
        /* JADX WARNING: Missing block: B:137:0x02b1, code skipped:
            return;
     */
        /* JADX WARNING: Missing block: B:244:0x04ea, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(Message msg, long srcId) {
            if (this.mMediaPlayer.mNativeContext == 0) {
                Log.w(MediaPlayer2Impl.TAG, "mediaplayer2 went away with unhandled events");
                return;
            }
            int what = msg.arg1;
            int extra = msg.arg2;
            int i = msg.what;
            DrmInfoImpl text = null;
            Iterator it;
            String str;
            StringBuilder stringBuilder;
            Iterator it2;
            if (i == 210) {
                if (msg.obj == null) {
                    Log.w(MediaPlayer2Impl.TAG, "MEDIA_DRM_INFO msg.obj=NULL");
                } else if (msg.obj instanceof Parcel) {
                    DrmInfoImpl drmInfo;
                    synchronized (MediaPlayer2Impl.this.mDrmLock) {
                        if (MediaPlayer2Impl.this.mDrmInfoImpl != null) {
                            text = MediaPlayer2Impl.this.mDrmInfoImpl.makeCopy();
                        }
                        drmInfo = text;
                    }
                    if (drmInfo != null) {
                        synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                            it = MediaPlayer2Impl.this.mDrmEventCallbackRecords.iterator();
                            while (it.hasNext()) {
                                Pair<Executor, DrmEventCallback> cb = (Pair) it.next();
                                ((Executor) cb.first).execute(new -$$Lambda$MediaPlayer2Impl$EventHandler$XDpOSvYSapoVyl-BYW0W8pLfp3A(this, cb, drmInfo));
                            }
                        }
                    }
                } else {
                    str = MediaPlayer2Impl.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("MEDIA_DRM_INFO msg.obj of unexpected type ");
                    stringBuilder.append(msg.obj);
                    Log.w(str, stringBuilder.toString());
                }
            } else if (i != 10000) {
                boolean z = true;
                String str2;
                Iterator it3;
                Pair<Executor, MediaPlayer2EventCallback> cb2;
                Pair<Executor, MediaPlayer2EventCallback> cb3;
                TimeProvider timeProvider;
                switch (i) {
                    case 0:
                        break;
                    case 1:
                        DataSourceDesc dsd;
                        try {
                            MediaPlayer2Impl.this.scanInternalSubtitleTracks();
                        } catch (RuntimeException e) {
                            sendMessage(obtainMessage(100, 1, -1010, null));
                        }
                        synchronized (MediaPlayer2Impl.this.mSrcLock) {
                            str2 = MediaPlayer2Impl.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("MEDIA_PREPARED: srcId=");
                            stringBuilder2.append(srcId);
                            stringBuilder2.append(", currentSrcId=");
                            stringBuilder2.append(MediaPlayer2Impl.this.mCurrentSrcId);
                            stringBuilder2.append(", nextSrcId=");
                            stringBuilder2.append(MediaPlayer2Impl.this.mNextSrcId);
                            Log.i(str2, stringBuilder2.toString());
                            if (srcId == MediaPlayer2Impl.this.mCurrentSrcId) {
                                dsd = MediaPlayer2Impl.this.mCurrentDSD;
                                MediaPlayer2Impl.this.prepareNextDataSource_l();
                            } else if (MediaPlayer2Impl.this.mNextDSDs == null || MediaPlayer2Impl.this.mNextDSDs.isEmpty() || srcId != MediaPlayer2Impl.this.mNextSrcId) {
                                dsd = null;
                            } else {
                                dsd = (DataSourceDesc) MediaPlayer2Impl.this.mNextDSDs.get(0);
                                MediaPlayer2Impl.this.mNextSourceState = 2;
                                if (MediaPlayer2Impl.this.mNextSourcePlayPending) {
                                    MediaPlayer2Impl.this.playNextDataSource_l();
                                }
                            }
                        }
                        if (dsd != null) {
                            synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                                it3 = MediaPlayer2Impl.this.mEventCallbackRecords.iterator();
                                while (it3.hasNext()) {
                                    cb2 = (Pair) it3.next();
                                    ((Executor) cb2.first).execute(new -$$Lambda$MediaPlayer2Impl$EventHandler$a55WUDW_Ad0Vmi1x4yZhQXvPqdc(this, cb2, dsd));
                                }
                            }
                        }
                        synchronized (MediaPlayer2Impl.this.mTaskLock) {
                            if (MediaPlayer2Impl.this.mCurrentTask != null && MediaPlayer2Impl.this.mCurrentTask.mMediaCallType == 6 && MediaPlayer2Impl.this.mCurrentTask.mDSD == dsd && MediaPlayer2Impl.this.mCurrentTask.mNeedToWaitForEventToComplete) {
                                MediaPlayer2Impl.this.mCurrentTask.sendCompleteNotification(0);
                                MediaPlayer2Impl.this.mCurrentTask = null;
                                MediaPlayer2Impl.this.processPendingTask_l();
                            }
                        }
                        return;
                    case 2:
                        DataSourceDesc dsd2 = MediaPlayer2Impl.this.mCurrentDSD;
                        synchronized (MediaPlayer2Impl.this.mSrcLock) {
                            if (srcId == MediaPlayer2Impl.this.mCurrentSrcId) {
                                str2 = MediaPlayer2Impl.TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("MEDIA_PLAYBACK_COMPLETE: srcId=");
                                stringBuilder3.append(srcId);
                                stringBuilder3.append(", currentSrcId=");
                                stringBuilder3.append(MediaPlayer2Impl.this.mCurrentSrcId);
                                stringBuilder3.append(", nextSrcId=");
                                stringBuilder3.append(MediaPlayer2Impl.this.mNextSrcId);
                                Log.i(str2, stringBuilder3.toString());
                                MediaPlayer2Impl.this.playNextDataSource_l();
                            }
                        }
                        synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                            it = MediaPlayer2Impl.this.mEventCallbackRecords.iterator();
                            while (it.hasNext()) {
                                cb3 = (Pair) it.next();
                                ((Executor) cb3.first).execute(new -$$Lambda$MediaPlayer2Impl$EventHandler$SRqj_-_1CH9_ez58ikKgR8GPWEc(this, cb3, dsd2));
                            }
                        }
                        MediaPlayer2Impl.this.stayAwake(false);
                        return;
                    case 3:
                        i = msg.arg1;
                        synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                            if (srcId == MediaPlayer2Impl.this.mCurrentSrcId) {
                                MediaPlayer2Impl.this.mBufferedPercentageCurrent.set(i);
                                it = MediaPlayer2Impl.this.mEventCallbackRecords.iterator();
                                while (it.hasNext()) {
                                    cb3 = (Pair) it.next();
                                    ((Executor) cb3.first).execute(new -$$Lambda$MediaPlayer2Impl$EventHandler$Dr_ImxKsZcrvP7slv6KPxdUdzXk(this, cb3, i));
                                }
                            } else if (srcId == MediaPlayer2Impl.this.mNextSrcId && !MediaPlayer2Impl.this.mNextDSDs.isEmpty()) {
                                MediaPlayer2Impl.this.mBufferedPercentageNext.set(i);
                                DataSourceDesc nextDSD = (DataSourceDesc) MediaPlayer2Impl.this.mNextDSDs.get(0);
                                it3 = MediaPlayer2Impl.this.mEventCallbackRecords.iterator();
                                while (it3.hasNext()) {
                                    cb2 = (Pair) it3.next();
                                    ((Executor) cb2.first).execute(new -$$Lambda$MediaPlayer2Impl$EventHandler$hsCyoCNpv30l9tb7sOpVC4dnMy8(this, cb2, nextDSD, i));
                                }
                            }
                        }
                        return;
                    case 4:
                        synchronized (MediaPlayer2Impl.this.mTaskLock) {
                            if (MediaPlayer2Impl.this.mCurrentTask != null && MediaPlayer2Impl.this.mCurrentTask.mMediaCallType == 14 && MediaPlayer2Impl.this.mCurrentTask.mNeedToWaitForEventToComplete) {
                                MediaPlayer2Impl.this.mCurrentTask.sendCompleteNotification(0);
                                MediaPlayer2Impl.this.mCurrentTask = null;
                                MediaPlayer2Impl.this.processPendingTask_l();
                            }
                        }
                    case 5:
                        i = msg.arg1;
                        int height = msg.arg2;
                        synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                            it = MediaPlayer2Impl.this.mEventCallbackRecords.iterator();
                            while (it.hasNext()) {
                                cb2 = (Pair) it.next();
                                ((Executor) cb2.first).execute(new -$$Lambda$MediaPlayer2Impl$EventHandler$ALpPmFUNsJxKZK0N2HhQK6ZY4XM(this, cb2, i, height));
                            }
                        }
                        return;
                    case 6:
                    case 7:
                        timeProvider = MediaPlayer2Impl.this.mTimeProvider;
                        if (timeProvider != null) {
                            if (msg.what != 7) {
                                z = false;
                            }
                            timeProvider.onPaused(z);
                            break;
                        }
                        break;
                    case 8:
                        timeProvider = MediaPlayer2Impl.this.mTimeProvider;
                        if (timeProvider != null) {
                            timeProvider.onStopped();
                            break;
                        }
                        break;
                    case 9:
                        break;
                    default:
                        Parcel parcel;
                        Pair<Executor, MediaPlayer2EventCallback> cb4;
                        switch (i) {
                            case 98:
                                timeProvider = MediaPlayer2Impl.this.mTimeProvider;
                                if (timeProvider != null) {
                                    timeProvider.onNotifyTime();
                                }
                                return;
                            case 99:
                                TimedText text2;
                                if (msg.obj instanceof Parcel) {
                                    parcel = msg.obj;
                                    text2 = new TimedText(parcel);
                                    parcel.recycle();
                                }
                                TimedText text3 = text2;
                                synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                                    it = MediaPlayer2Impl.this.mEventCallbackRecords.iterator();
                                    while (it.hasNext()) {
                                        cb3 = (Pair) it.next();
                                        ((Executor) cb3.first).execute(new -$$Lambda$MediaPlayer2Impl$EventHandler$sx24vrhw_-7V07cadDNXlQ5kv04(this, cb3, text3));
                                    }
                                }
                                return;
                            case 100:
                                str = MediaPlayer2Impl.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Error (");
                                stringBuilder.append(msg.arg1);
                                stringBuilder.append(",");
                                stringBuilder.append(msg.arg2);
                                stringBuilder.append(")");
                                Log.e(str, stringBuilder.toString());
                                synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                                    it2 = MediaPlayer2Impl.this.mEventCallbackRecords.iterator();
                                    while (it2.hasNext()) {
                                        cb4 = (Pair) it2.next();
                                        ((Executor) cb4.first).execute(new -$$Lambda$MediaPlayer2Impl$EventHandler$5fCusDxj0OAxGzH6d86WnqVt8Rw(this, cb4, what, extra));
                                        ((Executor) cb4.first).execute(new -$$Lambda$MediaPlayer2Impl$EventHandler$9rzGOSqsKQVeN_cdPvY8essrTyg(this, cb4));
                                    }
                                }
                                MediaPlayer2Impl.this.stayAwake(false);
                                return;
                            default:
                                switch (i) {
                                    case 200:
                                        i = msg.arg1;
                                        if (i != 2) {
                                            switch (i) {
                                                case 700:
                                                    str = MediaPlayer2Impl.TAG;
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("Info (");
                                                    stringBuilder.append(msg.arg1);
                                                    stringBuilder.append(",");
                                                    stringBuilder.append(msg.arg2);
                                                    stringBuilder.append(")");
                                                    Log.i(str, stringBuilder.toString());
                                                    break;
                                                case 701:
                                                case 702:
                                                    timeProvider = MediaPlayer2Impl.this.mTimeProvider;
                                                    if (timeProvider != null) {
                                                        if (msg.arg1 != 701) {
                                                            z = false;
                                                        }
                                                        timeProvider.onBuffering(z);
                                                        break;
                                                    }
                                                    break;
                                                default:
                                                    switch (i) {
                                                        case 802:
                                                            try {
                                                                MediaPlayer2Impl.this.scanInternalSubtitleTracks();
                                                                break;
                                                            } catch (RuntimeException e2) {
                                                                sendMessage(obtainMessage(100, 1, -1010, null));
                                                                break;
                                                            }
                                                        case 803:
                                                            break;
                                                    }
                                                    msg.arg1 = 802;
                                                    if (MediaPlayer2Impl.this.mSubtitleController != null) {
                                                        MediaPlayer2Impl.this.mSubtitleController.selectDefaultTrack();
                                                        break;
                                                    }
                                                    break;
                                            }
                                        } else if (srcId == MediaPlayer2Impl.this.mCurrentSrcId) {
                                            MediaPlayer2Impl.this.prepareNextDataSource_l();
                                        }
                                        synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                                            it2 = MediaPlayer2Impl.this.mEventCallbackRecords.iterator();
                                            while (it2.hasNext()) {
                                                cb4 = (Pair) it2.next();
                                                ((Executor) cb4.first).execute(new -$$Lambda$MediaPlayer2Impl$EventHandler$iPmZQ0HxMVwbBcbhgpHbun3WGTk(this, cb4, what, extra));
                                            }
                                        }
                                        return;
                                    case 201:
                                        OnSubtitleDataListener onSubtitleDataListener = MediaPlayer2Impl.this.mOnSubtitleDataListener;
                                        if (onSubtitleDataListener != null && (msg.obj instanceof Parcel)) {
                                            Parcel parcel2 = msg.obj;
                                            SubtitleData data = new SubtitleData(parcel2);
                                            parcel2.recycle();
                                            onSubtitleDataListener.onSubtitleData(this.mMediaPlayer, data);
                                        }
                                        return;
                                    case 202:
                                        TimedMetaData data2;
                                        if (msg.obj instanceof Parcel) {
                                            parcel = msg.obj;
                                            data2 = TimedMetaData.createTimedMetaDataFromParcel(parcel);
                                            parcel.recycle();
                                        }
                                        TimedMetaData data3 = data2;
                                        synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                                            it = MediaPlayer2Impl.this.mEventCallbackRecords.iterator();
                                            while (it.hasNext()) {
                                                cb3 = (Pair) it.next();
                                                ((Executor) cb3.first).execute(new -$$Lambda$MediaPlayer2Impl$EventHandler$5DmGtkuYQXExyXOBI9Qvu64NQ68(this, cb3, data3));
                                            }
                                        }
                                        return;
                                    default:
                                        str = MediaPlayer2Impl.TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Unknown message type ");
                                        stringBuilder.append(msg.what);
                                        Log.e(str, stringBuilder.toString());
                                        return;
                                }
                        }
                }
            } else {
                AudioManager.resetAudioPortGeneration();
                synchronized (MediaPlayer2Impl.this.mRoutingChangeListeners) {
                    for (NativeRoutingEventHandlerDelegate delegate : MediaPlayer2Impl.this.mRoutingChangeListeners.values()) {
                        delegate.notifyClient();
                    }
                }
            }
        }
    }

    private static class StreamEventCallback extends android.media.AudioTrack.StreamEventCallback {
        public long mJAudioTrackPtr;
        public long mNativeCallbackPtr;
        public long mUserDataPtr;

        public StreamEventCallback(long jAudioTrackPtr, long nativeCallbackPtr, long userDataPtr) {
            this.mJAudioTrackPtr = jAudioTrackPtr;
            this.mNativeCallbackPtr = nativeCallbackPtr;
            this.mUserDataPtr = userDataPtr;
        }

        public void onTearDown(AudioTrack track) {
            MediaPlayer2Impl.native_stream_event_onTearDown(this.mNativeCallbackPtr, this.mUserDataPtr);
        }

        public void onStreamPresentationEnd(AudioTrack track) {
            MediaPlayer2Impl.native_stream_event_onStreamPresentationEnd(this.mNativeCallbackPtr, this.mUserDataPtr);
        }

        public void onStreamDataRequest(AudioTrack track) {
            MediaPlayer2Impl.native_stream_event_onStreamDataRequest(this.mJAudioTrackPtr, this.mNativeCallbackPtr, this.mUserDataPtr);
        }
    }

    static class TimeProvider implements MediaTimeProvider {
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
        private EventHandler mEventHandler;
        private HandlerThread mHandlerThread;
        private long mLastReportedTime;
        private long mLastTimeUs = 0;
        private OnMediaTimeListener[] mListeners;
        private boolean mPaused = true;
        private boolean mPausing = false;
        private MediaPlayer2Impl mPlayer;
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

        public TimeProvider(MediaPlayer2Impl mp) {
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
                    this.mHandlerThread = new HandlerThread("MediaPlayer2MTPEventThread", -2);
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

        public void onSeekComplete(MediaPlayer2Impl mp) {
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

        /* JADX WARNING: Removed duplicated region for block: B:19:0x002e A:{Catch:{ IllegalStateException -> 0x007e }} */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0057 A:{SYNTHETIC, Splitter:B:25:0x0057} */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public long getCurrentTimeUs(boolean refreshTime, boolean monotonic) throws IllegalStateException {
            synchronized (this) {
                long j;
                if (!this.mPaused || refreshTime) {
                    try {
                        boolean z;
                        this.mLastTimeUs = this.mPlayer.getCurrentPosition() * MAX_EARLY_CALLBACK_US;
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

    public static final class TrackInfoImpl extends TrackInfo {
        static final Creator<TrackInfoImpl> CREATOR = new Creator<TrackInfoImpl>() {
            public TrackInfoImpl createFromParcel(Parcel in) {
                return new TrackInfoImpl(in);
            }

            public TrackInfoImpl[] newArray(int size) {
                return new TrackInfoImpl[size];
            }
        };
        final MediaFormat mFormat;
        final int mTrackType;

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

        TrackInfoImpl(Parcel in) {
            this.mTrackType = in.readInt();
            this.mFormat = MediaFormat.createSubtitleFormat(in.readString(), in.readString());
            if (this.mTrackType == 4) {
                this.mFormat.setInteger(MediaFormat.KEY_IS_AUTOSELECT, in.readInt());
                this.mFormat.setInteger(MediaFormat.KEY_IS_DEFAULT, in.readInt());
                this.mFormat.setInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE, in.readInt());
            }
        }

        TrackInfoImpl(int type, MediaFormat format) {
            this.mTrackType = type;
            this.mFormat = format;
        }

        void writeToParcel(Parcel dest, int flags) {
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

    public static final class NoDrmSchemeExceptionImpl extends NoDrmSchemeException {
        public NoDrmSchemeExceptionImpl(String detailMessage) {
            super(detailMessage);
        }
    }

    public static final class ProvisioningNetworkErrorExceptionImpl extends ProvisioningNetworkErrorException {
        public ProvisioningNetworkErrorExceptionImpl(String detailMessage) {
            super(detailMessage);
        }
    }

    public static final class ProvisioningServerErrorExceptionImpl extends ProvisioningServerErrorException {
        public ProvisioningServerErrorExceptionImpl(String detailMessage) {
            super(detailMessage);
        }
    }

    private native void _attachAuxEffect(int i);

    private native int _getAudioStreamType() throws IllegalStateException;

    private native void _notifyAt(long j);

    private native void _pause() throws IllegalStateException;

    private native void _prepareDrm(byte[] bArr, byte[] bArr2);

    private native void _release();

    private native void _releaseDrm();

    private native void _reset();

    private final native void _seekTo(long j, int i);

    private native void _setAudioSessionId(int i);

    private native void _setAuxEffectSendLevel(float f);

    private native void _setBufferingParams(BufferingParams bufferingParams);

    private native void _setPlaybackParams(PlaybackParams playbackParams);

    private native void _setSyncParams(SyncParams syncParams);

    private native void _setVideoSurface(Surface surface);

    private native void _setVolume(float f, float f2);

    private native void _start() throws IllegalStateException;

    private native void _stop() throws IllegalStateException;

    private native Parcel getParameter(int i);

    private native void nativeHandleDataSourceCallback(boolean z, long j, Media2DataSource media2DataSource);

    private native void nativeHandleDataSourceFD(boolean z, long j, FileDescriptor fileDescriptor, long j2, long j3) throws IOException;

    private native void nativeHandleDataSourceUrl(boolean z, long j, Media2HTTPService media2HTTPService, String str, String[] strArr, String[] strArr2) throws IOException;

    private native void nativePlayNextDataSource(long j);

    private final native void native_enableDeviceCallback(boolean z);

    private final native void native_finalize();

    private native int native_getMediaPlayer2State();

    private final native boolean native_getMetadata(boolean z, boolean z2, Parcel parcel);

    private native PersistableBundle native_getMetrics();

    private final native int native_getRoutedDeviceId();

    private static final native void native_init();

    private final native int native_invoke(Parcel parcel, Parcel parcel2);

    private final native int native_setMetadataFilter(Parcel parcel);

    private final native boolean native_setOutputDevice(int i);

    private final native void native_setup(Object obj);

    private static final native void native_stream_event_onStreamDataRequest(long j, long j2, long j3);

    private static final native void native_stream_event_onStreamPresentationEnd(long j, long j2);

    private static final native void native_stream_event_onTearDown(long j, long j2);

    private native void setLooping(boolean z);

    private native boolean setParameter(int i, Parcel parcel);

    public native void _prepare();

    public native int getAudioSessionId();

    public native BufferingParams getBufferingParams();

    public native long getCurrentPosition();

    public native long getDuration();

    public native PlaybackParams getPlaybackParams();

    public native SyncParams getSyncParams();

    public native int getVideoHeight();

    public native int getVideoWidth();

    public native boolean isLooping();

    public native boolean isPlaying();

    static {
        System.loadLibrary("media2_jni");
        native_init();
    }

    public MediaPlayer2Impl() {
        long j = this.mSrcIdGenerator;
        this.mSrcIdGenerator = j + 1;
        this.mCurrentSrcId = j;
        j = this.mSrcIdGenerator;
        this.mSrcIdGenerator = 1 + j;
        this.mNextSrcId = j;
        this.mNextSourceState = 0;
        this.mNextSourcePlayPending = false;
        this.mBufferedPercentageCurrent = new AtomicInteger(0);
        this.mBufferedPercentageNext = new AtomicInteger(0);
        this.mVolume = 1.0f;
        this.mDrmLock = new Object();
        this.mTaskLock = new Object();
        this.mPendingTasks = new LinkedList();
        this.mPreferredDevice = null;
        this.mRoutingChangeListeners = new ArrayMap();
        this.mIndexTrackPairs = new Vector();
        this.mInbandTrackIndices = new BitSet();
        this.mSelectedSubtitleTrackIndex = -1;
        this.mSubtitleDataListener = new OnSubtitleDataListener() {
            public void onSubtitleData(MediaPlayer2 mp, SubtitleData data) {
                int index = data.getTrackIndex();
                synchronized (MediaPlayer2Impl.this.mIndexTrackPairs) {
                    Iterator it = MediaPlayer2Impl.this.mIndexTrackPairs.iterator();
                    while (it.hasNext()) {
                        Pair<Integer, SubtitleTrack> p = (Pair) it.next();
                        if (!(p.first == null || ((Integer) p.first).intValue() != index || p.second == null)) {
                            p.second.onData(data);
                        }
                    }
                }
            }
        };
        this.mEventCbLock = new Object();
        this.mEventCallbackRecords = new ArrayList();
        this.mDrmEventCbLock = new Object();
        this.mDrmEventCallbackRecords = new ArrayList();
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
        this.mHandlerThread = new HandlerThread("MediaPlayer2TaskThread");
        this.mHandlerThread.start();
        this.mTaskHandler = new Handler(this.mHandlerThread.getLooper());
        this.mTimeProvider = new TimeProvider(this);
        this.mOpenSubtitleSources = new Vector();
        this.mGuard.open("close");
        native_setup(new WeakReference(this));
    }

    public void close() {
        synchronized (this.mGuard) {
            release();
        }
    }

    public void play() {
        addTask(new Task(5, false) {
            void process() {
                MediaPlayer2Impl.this.stayAwake(true);
                MediaPlayer2Impl.this._start();
            }
        });
    }

    public void prepare() {
        addTask(new Task(6, true) {
            void process() {
                MediaPlayer2Impl.this._prepare();
            }
        });
    }

    public void pause() {
        addTask(new Task(4, false) {
            void process() {
                MediaPlayer2Impl.this.stayAwake(false);
                MediaPlayer2Impl.this._pause();
            }
        });
    }

    public void skipToNext() {
        addTask(new Task(29, false) {
            void process() {
            }
        });
    }

    public long getBufferedPosition() {
        return (getDuration() * ((long) this.mBufferedPercentageCurrent.get())) / 100;
    }

    public int getPlayerState() {
        switch (getMediaPlayer2State()) {
            case 1:
                return 0;
            case 2:
            case 3:
                return 1;
            case 4:
                return 2;
            default:
                return 3;
        }
    }

    public int getBufferingState() {
        return 0;
    }

    public void setAudioAttributes(final AudioAttributes attributes) {
        addTask(new Task(16, false) {
            void process() {
                if (attributes != null) {
                    Parcel pattributes = Parcel.obtain();
                    attributes.writeToParcel(pattributes, 1);
                    MediaPlayer2Impl.this.setParameter(MediaPlayer2Impl.KEY_PARAMETER_AUDIO_ATTRIBUTES, pattributes);
                    pattributes.recycle();
                    return;
                }
                String msg = "Cannot set AudioAttributes to null";
                throw new IllegalArgumentException("Cannot set AudioAttributes to null");
            }
        });
    }

    public AudioAttributes getAudioAttributes() {
        Parcel pattributes = getParameter(KEY_PARAMETER_AUDIO_ATTRIBUTES);
        AudioAttributes attributes = (AudioAttributes) AudioAttributes.CREATOR.createFromParcel(pattributes);
        pattributes.recycle();
        return attributes;
    }

    public void setDataSource(final DataSourceDesc dsd) {
        addTask(new Task(19, false) {
            void process() {
                Preconditions.checkNotNull(dsd, "the DataSourceDesc cannot be null");
                synchronized (MediaPlayer2Impl.this.mSrcLock) {
                    MediaPlayer2Impl.this.mCurrentDSD = dsd;
                    MediaPlayer2Impl.this.mCurrentSrcId = MediaPlayer2Impl.this.mSrcIdGenerator = 1 + MediaPlayer2Impl.this.mSrcIdGenerator;
                    try {
                        MediaPlayer2Impl.this.handleDataSource(true, dsd, MediaPlayer2Impl.this.mCurrentSrcId);
                    } catch (IOException e) {
                    }
                }
            }
        });
    }

    public void setNextDataSource(final DataSourceDesc dsd) {
        addTask(new Task(22, false) {
            void process() {
                Preconditions.checkNotNull(dsd, "the DataSourceDesc cannot be null");
                synchronized (MediaPlayer2Impl.this.mSrcLock) {
                    MediaPlayer2Impl.this.mNextDSDs = new ArrayList(1);
                    MediaPlayer2Impl.this.mNextDSDs.add(dsd);
                    MediaPlayer2Impl.this.mNextSrcId = MediaPlayer2Impl.this.mSrcIdGenerator = 1 + MediaPlayer2Impl.this.mSrcIdGenerator;
                    MediaPlayer2Impl.this.mNextSourceState = 0;
                    MediaPlayer2Impl.this.mNextSourcePlayPending = false;
                }
                if (MediaPlayer2Impl.this.getMediaPlayer2State() != 1) {
                    synchronized (MediaPlayer2Impl.this.mSrcLock) {
                        MediaPlayer2Impl.this.prepareNextDataSource_l();
                    }
                }
            }
        });
    }

    public void setNextDataSources(final List<DataSourceDesc> dsds) {
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
                synchronized (MediaPlayer2Impl.this.mSrcLock) {
                    MediaPlayer2Impl.this.mNextDSDs = new ArrayList(dsds);
                    MediaPlayer2Impl.this.mNextSrcId = MediaPlayer2Impl.this.mSrcIdGenerator = 1 + MediaPlayer2Impl.this.mSrcIdGenerator;
                    MediaPlayer2Impl.this.mNextSourceState = 0;
                    MediaPlayer2Impl.this.mNextSourcePlayPending = false;
                }
                if (MediaPlayer2Impl.this.getMediaPlayer2State() != 1) {
                    synchronized (MediaPlayer2Impl.this.mSrcLock) {
                        MediaPlayer2Impl.this.prepareNextDataSource_l();
                    }
                }
            }
        });
    }

    public DataSourceDesc getCurrentDataSource() {
        DataSourceDesc dataSourceDesc;
        synchronized (this.mSrcLock) {
            dataSourceDesc = this.mCurrentDSD;
        }
        return dataSourceDesc;
    }

    public void loopCurrent(final boolean loop) {
        addTask(new Task(3, false) {
            void process() {
                MediaPlayer2Impl.this.setLooping(loop);
            }
        });
    }

    public void setPlaybackSpeed(final float speed) {
        addTask(new Task(25, false) {
            void process() {
                MediaPlayer2Impl.this._setPlaybackParams(MediaPlayer2Impl.this.getPlaybackParams().setSpeed(speed));
            }
        });
    }

    public float getPlaybackSpeed() {
        return getPlaybackParams().getSpeed();
    }

    public boolean isReversePlaybackSupported() {
        return false;
    }

    public void setPlayerVolume(final float volume) {
        addTask(new Task(26, false) {
            void process() {
                MediaPlayer2Impl.this.mVolume = volume;
                MediaPlayer2Impl.this._setVolume(volume, volume);
            }
        });
    }

    public float getPlayerVolume() {
        return this.mVolume;
    }

    public float getMaxPlayerVolume() {
        return 1.0f;
    }

    public void registerPlayerEventCallback(Executor e, PlayerEventCallback cb) {
    }

    public void unregisterPlayerEventCallback(PlayerEventCallback cb) {
    }

    public Parcel newRequest() {
        return Parcel.obtain();
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

    public void notifyWhenCommandLabelReached(final Object label) {
        addTask(new Task(1003, false) {
            void process() {
                synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                    Iterator it = MediaPlayer2Impl.this.mEventCallbackRecords.iterator();
                    while (it.hasNext()) {
                        Pair<Executor, MediaPlayer2EventCallback> cb = (Pair) it.next();
                        ((Executor) cb.first).execute(new -$$Lambda$MediaPlayer2Impl$12$GAwhcv62KlexkkYkbjb8-qEksjI(this, cb, label));
                    }
                }
            }
        });
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

    public void setSurface(final Surface surface) {
        addTask(new Task(27, false) {
            void process() {
                if (MediaPlayer2Impl.this.mScreenOnWhilePlaying && surface != null) {
                    Log.w(MediaPlayer2Impl.TAG, "setScreenOnWhilePlaying(true) is ineffective for Surface");
                }
                MediaPlayer2Impl.this.mSurfaceHolder = null;
                MediaPlayer2Impl.this._setVideoSurface(surface);
                MediaPlayer2Impl.this.updateSurfaceScreenOn();
            }
        });
    }

    public void setVideoScalingMode(final int mode) {
        addTask(new Task(1002, false) {
            void process() {
                if (MediaPlayer2Impl.this.isVideoScalingModeSupported(mode)) {
                    Parcel request = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    try {
                        request.writeInt(6);
                        request.writeInt(mode);
                        MediaPlayer2Impl.this.invoke(request, reply);
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
        });
    }

    public void clearPendingCommands() {
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
            Task task = (Task) this.mPendingTasks.remove(0);
            this.mCurrentTask = task;
            this.mTaskHandler.post(task);
        }
    }

    private void handleDataSource(boolean isCurrent, DataSourceDesc dsd, long srcId) throws IOException {
        Preconditions.checkNotNull(dsd, "the DataSourceDesc cannot be null");
        switch (dsd.getType()) {
            case 1:
                handleDataSource(isCurrent, srcId, dsd.getMedia2DataSource());
                return;
            case 2:
                handleDataSource(isCurrent, srcId, dsd.getFileDescriptor(), dsd.getFileDescriptorOffset(), dsd.getFileDescriptorLength());
                break;
            case 3:
                handleDataSource(isCurrent, srcId, dsd.getUriContext(), dsd.getUri(), dsd.getUriHeaders(), dsd.getUriCookies());
                return;
        }
        boolean z = isCurrent;
        long j = srcId;
    }

    private void handleDataSource(boolean isCurrent, long srcId, Context context, Uri uri, Map<String, String> headers, List<HttpCookie> cookies) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        String scheme = uri.getScheme();
        String authority = ContentProvider.getAuthorityWithoutUserId(uri.getAuthority());
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            handleDataSource(isCurrent, srcId, uri.getPath(), null, null);
            return;
        }
        if ("content".equals(scheme) && "settings".equals(authority)) {
            int type = RingtoneManager.getDefaultType(uri);
            Uri cacheUri = RingtoneManager.getCacheForType(type, context.getUserId());
            Uri actualUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
            if (!attemptDataSource(isCurrent, srcId, resolver, cacheUri) && !attemptDataSource(isCurrent, srcId, resolver, actualUri)) {
                handleDataSource(isCurrent, srcId, uri.toString(), (Map) headers, (List) cookies);
            } else {
                return;
            }
        }
        Context context2 = context;
        if (!attemptDataSource(isCurrent, srcId, resolver, uri)) {
            handleDataSource(isCurrent, srcId, uri.toString(), (Map) headers, (List) cookies);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:33:0x0063 A:{ExcHandler: IOException | NullPointerException | SecurityException (e java.lang.Throwable), Splitter:B:4:0x0007} */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x0063 A:{ExcHandler: IOException | NullPointerException | SecurityException (e java.lang.Throwable), Splitter:B:4:0x0007} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:27:0x0059, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:30:?, code skipped:
            r5.addSuppressed(r0);
     */
    /* JADX WARNING: Missing block: B:33:0x0063, code skipped:
            r0 = e;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean attemptDataSource(boolean isCurrent, long srcId, ContentResolver resolver, Uri uri) {
        Throwable th;
        Uri uri2 = uri;
        try {
            AssetFileDescriptor afd;
            try {
                afd = resolver.openAssetFileDescriptor(uri2, FullBackup.ROOT_TREE_TOKEN);
                if (afd.getDeclaredLength() < 0) {
                    handleDataSource(isCurrent, srcId, afd.getFileDescriptor(), 0, (long) DataSourceDesc.LONG_MAX);
                } else {
                    handleDataSource(isCurrent, srcId, afd.getFileDescriptor(), afd.getStartOffset(), afd.getDeclaredLength());
                }
                if (afd != null) {
                    afd.close();
                }
                return true;
            } catch (IOException | NullPointerException | SecurityException e) {
            } catch (Throwable th2) {
                Throwable th3 = th;
                th = th2;
                if (afd != null) {
                    if (th3 != null) {
                        afd.close();
                    } else {
                        afd.close();
                    }
                }
            }
        } catch (IOException | NullPointerException | SecurityException e2) {
            Exception ex = e2;
            ContentResolver contentResolver = resolver;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't open ");
            stringBuilder.append(uri2);
            stringBuilder.append(": ");
            stringBuilder.append(ex);
            Log.w(str, stringBuilder.toString());
            return false;
        }
    }

    private void handleDataSource(boolean isCurrent, long srcId, String path, Map<String, String> headers, List<HttpCookie> cookies) throws IOException {
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
        handleDataSource(isCurrent, srcId, path, keys, values, (List) cookies);
    }

    private void handleDataSource(boolean isCurrent, long srcId, String path, String[] keys, String[] values, List<HttpCookie> cookies) throws IOException {
        String path2;
        Uri uri = Uri.parse(path);
        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            path2 = uri.getPath();
        } else if (scheme != null) {
            path2 = path;
            nativeHandleDataSourceUrl(isCurrent, srcId, Media2HTTPService.createHTTPService(path2, cookies), path2, keys, values);
            return;
        } else {
            path2 = path;
        }
        List<HttpCookie> list = cookies;
        File file = new File(path2);
        if (file.exists()) {
            FileInputStream is = new FileInputStream(file);
            handleDataSource(isCurrent, srcId, is.getFD(), 0, (long) DataSourceDesc.LONG_MAX);
            is.close();
            return;
        }
        throw new IOException("handleDataSource failed.");
    }

    private void handleDataSource(boolean isCurrent, long srcId, FileDescriptor fd, long offset, long length) throws IOException {
        nativeHandleDataSourceFD(isCurrent, srcId, fd, offset, length);
    }

    private void handleDataSource(boolean isCurrent, long srcId, Media2DataSource dataSource) {
        nativeHandleDataSourceCallback(isCurrent, srcId, dataSource);
    }

    private void prepareNextDataSource_l() {
        if (this.mNextDSDs != null && !this.mNextDSDs.isEmpty() && this.mNextSourceState == 0) {
            try {
                this.mNextSourceState = 1;
                handleDataSource(false, (DataSourceDesc) this.mNextDSDs.get(0), this.mNextSrcId);
            } catch (Exception e) {
                final Message msg2 = this.mEventHandler.obtainMessage(100, 1, -1010, null);
                final long nextSrcId = this.mNextSrcId;
                this.mEventHandler.post(new Runnable() {
                    public void run() {
                        MediaPlayer2Impl.this.mEventHandler.handleMessage(msg2, nextSrcId);
                    }
                });
            }
        }
    }

    private void playNextDataSource_l() {
        if (this.mNextDSDs != null && !this.mNextDSDs.isEmpty()) {
            if (this.mNextSourceState == 2) {
                this.mCurrentDSD = (DataSourceDesc) this.mNextDSDs.get(0);
                this.mCurrentSrcId = this.mNextSrcId;
                this.mBufferedPercentageCurrent.set(this.mBufferedPercentageNext.get());
                this.mNextDSDs.remove(0);
                long j = this.mSrcIdGenerator;
                this.mSrcIdGenerator = 1 + j;
                this.mNextSrcId = j;
                this.mBufferedPercentageNext.set(0);
                this.mNextSourceState = 0;
                this.mNextSourcePlayPending = false;
                final long srcId = this.mCurrentSrcId;
                try {
                    nativePlayNextDataSource(srcId);
                } catch (Exception e) {
                    final Message msg2 = this.mEventHandler.obtainMessage(100, 1, -1010, null);
                    this.mEventHandler.post(new Runnable() {
                        public void run() {
                            MediaPlayer2Impl.this.mEventHandler.handleMessage(msg2, srcId);
                        }
                    });
                }
            } else {
                if (this.mNextSourceState == 0) {
                    prepareNextDataSource_l();
                }
                this.mNextSourcePlayPending = true;
            }
        }
    }

    private int getAudioStreamType() {
        if (this.mStreamType == Integer.MIN_VALUE) {
            this.mStreamType = _getAudioStreamType();
        }
        return this.mStreamType;
    }

    public void stop() {
        stayAwake(false);
        _stop();
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
        this.mWakeLock = ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).newWakeLock(536870912 | mode, MediaPlayer2Impl.class.getName());
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

    public int getMediaPlayer2State() {
        return native_getMediaPlayer2State();
    }

    public void setBufferingParams(final BufferingParams params) {
        addTask(new Task(1001, false) {
            void process() {
                Preconditions.checkNotNull(params, "the BufferingParams cannot be null");
                MediaPlayer2Impl.this._setBufferingParams(params);
            }
        });
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

    public void setPlaybackParams(final PlaybackParams params) {
        addTask(new Task(24, false) {
            void process() {
                Preconditions.checkNotNull(params, "the PlaybackParams cannot be null");
                MediaPlayer2Impl.this._setPlaybackParams(params);
            }
        });
    }

    public void setSyncParams(final SyncParams params) {
        addTask(new Task(28, false) {
            void process() {
                Preconditions.checkNotNull(params, "the SyncParams cannot be null");
                MediaPlayer2Impl.this._setSyncParams(params);
            }
        });
    }

    public void seekTo(long msec, int mode) {
        final int i = mode;
        final long j = msec;
        addTask(new Task(14, true) {
            void process() {
                if (i < 0 || i > 3) {
                    String msg = new StringBuilder();
                    msg.append("Illegal seek mode: ");
                    msg.append(i);
                    throw new IllegalArgumentException(msg.toString());
                }
                long posMs = j;
                String str;
                StringBuilder stringBuilder;
                if (posMs > 2147483647L) {
                    str = MediaPlayer2Impl.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("seekTo offset ");
                    stringBuilder.append(posMs);
                    stringBuilder.append(" is too large, cap to ");
                    stringBuilder.append(Integer.MAX_VALUE);
                    Log.w(str, stringBuilder.toString());
                    posMs = 2147483647L;
                } else if (posMs < -2147483648L) {
                    str = MediaPlayer2Impl.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("seekTo offset ");
                    stringBuilder.append(posMs);
                    stringBuilder.append(" is too small, cap to ");
                    stringBuilder.append(Integer.MIN_VALUE);
                    Log.w(str, stringBuilder.toString());
                    posMs = -2147483648L;
                }
                MediaPlayer2Impl.this._seekTo(posMs, i);
            }
        });
    }

    public MediaTimestamp getTimestamp() {
        try {
            return new MediaTimestamp(getCurrentPosition() * 1000, System.nanoTime(), isPlaying() ? getPlaybackParams().getSpeed() : 0.0f);
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
        synchronized (this.mEventCbLock) {
            this.mEventCallbackRecords.clear();
        }
        synchronized (this.mDrmEventCbLock) {
            this.mDrmEventCallbackRecords.clear();
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

    public void setAudioSessionId(final int sessionId) {
        addTask(new Task(17, false) {
            void process() {
                MediaPlayer2Impl.this._setAudioSessionId(sessionId);
            }
        });
    }

    public void attachAuxEffect(final int effectId) {
        addTask(new Task(1, false) {
            void process() {
                MediaPlayer2Impl.this._attachAuxEffect(effectId);
            }
        });
    }

    public void setAuxEffectSendLevel(final float level) {
        addTask(new Task(18, false) {
            void process() {
                MediaPlayer2Impl.this._setAuxEffectSendLevel(level);
            }
        });
    }

    public List<TrackInfo> getTrackInfo() {
        List asList;
        TrackInfoImpl[] trackInfo = getInbandTrackInfoImpl();
        synchronized (this.mIndexTrackPairs) {
            TrackInfoImpl[] allTrackInfo = new TrackInfoImpl[this.mIndexTrackPairs.size()];
            for (int i = 0; i < allTrackInfo.length; i++) {
                Pair<Integer, SubtitleTrack> p = (Pair) this.mIndexTrackPairs.get(i);
                if (p.first != null) {
                    allTrackInfo[i] = trackInfo[((Integer) p.first).intValue()];
                } else {
                    SubtitleTrack track = p.second;
                    allTrackInfo[i] = new TrackInfoImpl(track.getTrackType(), track.getFormat());
                }
            }
            asList = Arrays.asList(allTrackInfo);
        }
        return asList;
    }

    private TrackInfoImpl[] getInbandTrackInfoImpl() throws IllegalStateException {
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInt(1);
            invoke(request, reply);
            TrackInfoImpl[] trackInfo = (TrackInfoImpl[]) reply.createTypedArray(TrackInfoImpl.CREATOR);
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
                    MediaPlayer2Impl.this.mSubtitleController = new SubtitleController(ActivityThread.currentApplication(), MediaPlayer2Impl.this.mTimeProvider, MediaPlayer2Impl.this);
                    MediaPlayer2Impl.this.mSubtitleController.setAnchor(new Anchor() {
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
        setOnSubtitleDataListener(null);
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
                setOnSubtitleDataListener(this.mSubtitleDataListener);
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
                if (fIs == null || MediaPlayer2Impl.this.mSubtitleController == null) {
                    return 901;
                }
                SubtitleTrack track = MediaPlayer2Impl.this.mSubtitleController.addTrack(fFormat);
                if (track == null) {
                    return 901;
                }
                Scanner scanner = new Scanner(fIs, "UTF-8");
                String contents = scanner.useDelimiter("\\A").next();
                synchronized (MediaPlayer2Impl.this.mOpenSubtitleSources) {
                    MediaPlayer2Impl.this.mOpenSubtitleSources.remove(fIs);
                }
                scanner.close();
                synchronized (MediaPlayer2Impl.this.mIndexTrackPairs) {
                    MediaPlayer2Impl.this.mIndexTrackPairs.add(Pair.create(null, track));
                }
                Handler h = MediaPlayer2Impl.this.mTimeProvider.mEventHandler;
                h.sendMessage(h.obtainMessage(1, 4, null, Pair.create(track, contents.getBytes())));
                return 803;
            }

            public void run() {
                int res = addTrack();
                if (MediaPlayer2Impl.this.mEventHandler != null) {
                    MediaPlayer2Impl.this.mEventHandler.sendMessage(MediaPlayer2Impl.this.mEventHandler.obtainMessage(200, res, 0, null));
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
        TrackInfoImpl[] tracks = getInbandTrackInfoImpl();
        synchronized (this.mIndexTrackPairs) {
            for (int i = 0; i < tracks.length; i++) {
                if (!this.mInbandTrackIndices.get(i)) {
                    this.mInbandTrackIndices.set(i);
                    if (tracks[i].getTrackType() == 4) {
                        this.mIndexTrackPairs.add(Pair.create(Integer.valueOf(i), this.mSubtitleController.addTrack(tracks[i].getFormat())));
                    } else {
                        this.mIndexTrackPairs.add(Pair.create(Integer.valueOf(i), null));
                    }
                }
            }
        }
    }

    public void addTimedTextSource(String path, String mimeType) throws IOException {
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
            if (r1 == null) goto L_0x0043;
     */
    /* JADX WARNING: Missing block: B:23:0x003b, code skipped:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:25:0x0040, code skipped:
            if (r1 == null) goto L_0x0043;
     */
    /* JADX WARNING: Missing block: B:26:0x0043, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addTimedTextSource(Context context, Uri uri, String mimeType) throws IOException {
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
        } catch (Throwable th) {
            if (fd != null) {
                fd.close();
            }
        }
    }

    public void addTimedTextSource(FileDescriptor fd, String mimeType) {
        addTimedTextSource(fd, 0, DataSourceDesc.LONG_MAX, mimeType);
    }

    public void addTimedTextSource(FileDescriptor fd, long offset, long length, String mime) {
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
                            Handler h = MediaPlayer2Impl.this.mTimeProvider.mEventHandler;
                            h.sendMessage(h.obtainMessage(1, 4, 0, Pair.create(subtitleTrack, bos.toByteArray())));
                            try {
                                Os.close(dupedFd);
                            } catch (ErrnoException e) {
                                Log.e(MediaPlayer2Impl.TAG, e.getMessage(), e);
                            }
                            return 803;
                        } catch (Exception e2) {
                            Log.e(MediaPlayer2Impl.TAG, e2.getMessage(), e2);
                            try {
                                Os.close(dupedFd);
                            } catch (ErrnoException e3) {
                                Log.e(MediaPlayer2Impl.TAG, e3.getMessage(), e3);
                            }
                            return 900;
                        } catch (Throwable th) {
                            try {
                                Os.close(dupedFd);
                            } catch (ErrnoException e4) {
                                Log.e(MediaPlayer2Impl.TAG, e4.getMessage(), e4);
                            }
                            throw th;
                        }
                    }

                    public void run() {
                        int res = addTrack();
                        if (MediaPlayer2Impl.this.mEventHandler != null) {
                            MediaPlayer2Impl.this.mEventHandler.sendMessage(MediaPlayer2Impl.this.mEventHandler.obtainMessage(200, res, 0, null));
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

    public int getSelectedTrack(int trackType) {
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

    public void selectTrack(final int index) {
        addTask(new Task(15, false) {
            void process() {
                MediaPlayer2Impl.this.selectOrDeselectTrack(index, true);
            }
        });
    }

    public void deselectTrack(final int index) {
        addTask(new Task(2, false) {
            void process() {
                MediaPlayer2Impl.this.selectOrDeselectTrack(index, false);
            }
        });
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
            request.writeInt(select ? 4 : 5);
            request.writeInt(index);
            invoke(request, reply);
        } finally {
            request.recycle();
            reply.recycle();
        }
    }

    protected void finalize() throws Throwable {
        if (this.mGuard != null) {
            this.mGuard.warnIfOpen();
        }
        close();
        native_finalize();
    }

    private void release() {
        stayAwake(false);
        updateSurfaceScreenOn();
        synchronized (this.mEventCbLock) {
            this.mEventCallbackRecords.clear();
        }
        if (this.mHandlerThread != null) {
            this.mHandlerThread.quitSafely();
            this.mHandlerThread = null;
        }
        if (this.mTimeProvider != null) {
            this.mTimeProvider.close();
            this.mTimeProvider = null;
        }
        this.mOnSubtitleDataListener = null;
        this.mOnDrmConfigHelper = null;
        synchronized (this.mDrmEventCbLock) {
            this.mDrmEventCallbackRecords.clear();
        }
        resetDrmState();
        _release();
    }

    public MediaTimeProvider getMediaTimeProvider() {
        if (this.mTimeProvider == null) {
            this.mTimeProvider = new TimeProvider(this);
        }
        return this.mTimeProvider;
    }

    private static void postEventFromNative(Object mediaplayer2_ref, final long srcId, int what, int arg1, int arg2, Object obj) {
        MediaPlayer2Impl mp = (MediaPlayer2Impl) ((WeakReference) mediaplayer2_ref).get();
        if (mp != null) {
            if (what == 1) {
                synchronized (mp.mDrmLock) {
                    mp.mDrmInfoResolved = true;
                }
            } else if (what != 200) {
                if (what == 210) {
                    Log.v(TAG, "postEventFromNative MEDIA_DRM_INFO");
                    if (obj instanceof Parcel) {
                        DrmInfoImpl drmInfo = new DrmInfoImpl((Parcel) obj, null);
                        synchronized (mp.mDrmLock) {
                            mp.mDrmInfoImpl = drmInfo;
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
                        MediaPlayer2Impl.this.play();
                    }
                }).start();
                Thread.yield();
            }
            if (mp.mEventHandler != null) {
                final Message m = mp.mEventHandler.obtainMessage(what, arg1, arg2, obj);
                mp.mEventHandler.post(new Runnable() {
                    public void run() {
                        MediaPlayer2Impl.this.mEventHandler.handleMessage(m, srcId);
                    }
                });
            }
        }
    }

    public void setMediaPlayer2EventCallback(Executor executor, MediaPlayer2EventCallback eventCallback) {
        if (eventCallback == null) {
            throw new IllegalArgumentException("Illegal null MediaPlayer2EventCallback");
        } else if (executor != null) {
            synchronized (this.mEventCbLock) {
                this.mEventCallbackRecords.add(new Pair(executor, eventCallback));
            }
        } else {
            throw new IllegalArgumentException("Illegal null Executor for the MediaPlayer2EventCallback");
        }
    }

    public void clearMediaPlayer2EventCallback() {
        synchronized (this.mEventCbLock) {
            this.mEventCallbackRecords.clear();
        }
    }

    public void setOnSubtitleDataListener(OnSubtitleDataListener listener) {
        this.mOnSubtitleDataListener = listener;
    }

    public void setOnDrmConfigHelper(OnDrmConfigHelper listener) {
        synchronized (this.mDrmLock) {
            this.mOnDrmConfigHelper = listener;
        }
    }

    public void setDrmEventCallback(Executor executor, DrmEventCallback eventCallback) {
        if (eventCallback == null) {
            throw new IllegalArgumentException("Illegal null MediaPlayer2EventCallback");
        } else if (executor != null) {
            synchronized (this.mDrmEventCbLock) {
                this.mDrmEventCallbackRecords.add(new Pair(executor, eventCallback));
            }
        } else {
            throw new IllegalArgumentException("Illegal null Executor for the MediaPlayer2EventCallback");
        }
    }

    public void clearDrmEventCallback() {
        synchronized (this.mDrmEventCbLock) {
            this.mDrmEventCallbackRecords.clear();
        }
    }

    public DrmInfo getDrmInfo() {
        DrmInfoImpl drmInfo = null;
        synchronized (this.mDrmLock) {
            if (!this.mDrmInfoResolved) {
                if (this.mDrmInfoImpl == null) {
                    String msg = "The Player has not been prepared yet";
                    Log.v(TAG, "The Player has not been prepared yet");
                    throw new IllegalStateException("The Player has not been prepared yet");
                }
            }
            if (this.mDrmInfoImpl != null) {
                drmInfo = this.mDrmInfoImpl.makeCopy();
            }
        }
        return drmInfo;
    }

    /* JADX WARNING: Removed duplicated region for block: B:102:? A:{SYNTHETIC, RETURN, ORIG_RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00d0  */
    /* JADX WARNING: Missing block: B:33:0x0062, code skipped:
            if (r1 != false) goto L_0x0064;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void prepareDrm(UUID uuid) throws UnsupportedSchemeException, ResourceBusyException, ProvisioningNetworkErrorException, ProvisioningServerErrorException {
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
            if (this.mDrmInfoImpl == null) {
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
            this.mOnDrmConfigHelper.onDrmConfig(this, this.mCurrentDSD);
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
                            throw new ProvisioningNetworkErrorExceptionImpl(msg3);
                        case 2:
                            msg3 = "prepareDrm: Provisioning was required but the request was denied by the server.";
                            Log.e(TAG, msg3);
                            throw new ProvisioningServerErrorExceptionImpl(msg3);
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
        if (allDoneWithoutProvisioning) {
            synchronized (this.mDrmEventCbLock) {
                Iterator it = this.mDrmEventCallbackRecords.iterator();
                while (it.hasNext()) {
                    Pair<Executor, DrmEventCallback> cb = (Pair) it.next();
                    ((Executor) cb.first).execute(new -$$Lambda$MediaPlayer2Impl$1jR0wmXW_cOZenZs6Xt6lhAUeQ0(this, cb));
                }
            }
        }
    }

    public void releaseDrm() throws NoDrmSchemeException {
        addTask(new Task(12, false) {
            void process() throws NoDrmSchemeException {
                synchronized (MediaPlayer2Impl.this.mDrmLock) {
                    Log.v(MediaPlayer2Impl.TAG, "releaseDrm:");
                    if (MediaPlayer2Impl.this.mActiveDrmScheme) {
                        try {
                            MediaPlayer2Impl.this._releaseDrm();
                            MediaPlayer2Impl.this.cleanDrmObj();
                            MediaPlayer2Impl.this.mActiveDrmScheme = false;
                        } catch (IllegalStateException e) {
                            Log.w(MediaPlayer2Impl.TAG, "releaseDrm: Exception ", e);
                            throw new IllegalStateException("releaseDrm: The player is not in a valid state.");
                        } catch (Exception e2) {
                            Log.e(MediaPlayer2Impl.TAG, "releaseDrm: Exception ", e2);
                        }
                    } else {
                        Log.e(MediaPlayer2Impl.TAG, "releaseDrm(): No active DRM scheme to release.");
                        throw new NoDrmSchemeExceptionImpl("releaseDrm: No active DRM scheme to release.");
                    }
                }
            }
        });
    }

    public KeyRequest getDrmKeyRequest(byte[] keySetId, byte[] initData, String mimeType, int keyType, Map<String, String> optionalParameters) throws NoDrmSchemeException {
        String str;
        NotProvisionedException e;
        String str2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getDrmKeyRequest:  keySetId: ");
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
                        Log.w(TAG, "getDrmKeyRequest NotProvisionedException: Unexpected. Shouldn't have reached here.");
                        throw new IllegalStateException("getDrmKeyRequest: Unexpected provisioning error.");
                    } catch (Exception e3) {
                        str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("getDrmKeyRequest Exception ");
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
                stringBuilder3.append("getDrmKeyRequest:   --> request: ");
                stringBuilder3.append(e3);
                Log.v(str, stringBuilder3.toString());
            } else {
                Log.e(TAG, "getDrmKeyRequest NoDrmSchemeException");
                throw new NoDrmSchemeExceptionImpl("getDrmKeyRequest: Has to set a DRM scheme first.");
            }
        }
        return e3;
    }

    public byte[] provideDrmKeyResponse(byte[] keySetId, byte[] response) throws NoDrmSchemeException, DeniedByServerException {
        NotProvisionedException e;
        byte[] keySetResult;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("provideDrmKeyResponse: keySetId: ");
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
                        Log.w(TAG, "provideDrmKeyResponse NotProvisionedException: Unexpected. Shouldn't have reached here.");
                        throw new IllegalStateException("provideDrmKeyResponse: Unexpected provisioning error.");
                    } catch (Exception e3) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("provideDrmKeyResponse Exception ");
                        stringBuilder2.append(e3);
                        Log.w(str2, stringBuilder2.toString());
                        throw e3;
                    }
                }
                e3 = keySetId;
                keySetResult = this.mDrmObj.provideKeyResponse(e3, response);
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("provideDrmKeyResponse: keySetId: ");
                stringBuilder3.append(keySetId);
                stringBuilder3.append(" response: ");
                stringBuilder3.append(response);
                stringBuilder3.append(" --> ");
                stringBuilder3.append(keySetResult);
                Log.v(str3, stringBuilder3.toString());
            } else {
                Log.e(TAG, "getDrmKeyRequest NoDrmSchemeException");
                throw new NoDrmSchemeExceptionImpl("getDrmKeyRequest: Has to set a DRM scheme first.");
            }
        }
        return keySetResult;
    }

    public void restoreDrmKeys(final byte[] keySetId) throws NoDrmSchemeException {
        addTask(new Task(13, false) {
            void process() throws NoDrmSchemeException {
                String str = MediaPlayer2Impl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("restoreDrmKeys: keySetId: ");
                stringBuilder.append(keySetId);
                Log.v(str, stringBuilder.toString());
                synchronized (MediaPlayer2Impl.this.mDrmLock) {
                    if (MediaPlayer2Impl.this.mActiveDrmScheme) {
                        try {
                            MediaPlayer2Impl.this.mDrmObj.restoreKeys(MediaPlayer2Impl.this.mDrmSessionId, keySetId);
                        } catch (Exception e) {
                            String str2 = MediaPlayer2Impl.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("restoreKeys Exception ");
                            stringBuilder2.append(e);
                            Log.w(str2, stringBuilder2.toString());
                            throw e;
                        }
                    }
                    Log.w(MediaPlayer2Impl.TAG, "restoreDrmKeys NoDrmSchemeException");
                    throw new NoDrmSchemeExceptionImpl("restoreDrmKeys: Has to set a DRM scheme first.");
                }
            }
        });
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
                    throw new NoDrmSchemeExceptionImpl("getDrmPropertyString: Has to prepareDrm() first.");
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
                    throw new NoDrmSchemeExceptionImpl("setDrmPropertyString: Has to prepareDrm() first.");
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

    private static boolean setAudioOutputDeviceById(AudioTrack track, int deviceId) {
        int i = 0;
        if (track == null) {
            return false;
        }
        if (deviceId == 0) {
            track.setPreferredDevice(null);
            return true;
        }
        AudioDeviceInfo[] outputDevices = AudioManager.getDevicesStatic(2);
        boolean success = false;
        int length = outputDevices.length;
        while (i < length) {
            AudioDeviceInfo device = outputDevices[i];
            if (device.getId() == deviceId) {
                track.setPreferredDevice(device);
                success = true;
                break;
            }
            i++;
        }
        return success;
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
        boolean hasCallback;
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
        synchronized (this.mDrmEventCbLock) {
            hasCallback = 1 ^ this.mDrmEventCallbackRecords.isEmpty();
        }
        if (hasCallback) {
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
            stringBuilder.append("resetDrmState:  mDrmInfoImpl=");
            stringBuilder.append(this.mDrmInfoImpl);
            stringBuilder.append(" mDrmProvisioningThread=");
            stringBuilder.append(this.mDrmProvisioningThread);
            stringBuilder.append(" mPrepareDrmInProgress=");
            stringBuilder.append(this.mPrepareDrmInProgress);
            stringBuilder.append(" mActiveDrmScheme=");
            stringBuilder.append(this.mActiveDrmScheme);
            Log.v(str, stringBuilder.toString());
            this.mDrmInfoResolved = false;
            this.mDrmInfoImpl = null;
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
