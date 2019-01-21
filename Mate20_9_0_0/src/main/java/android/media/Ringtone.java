package android.media;

import android.app.DownloadManager;
import android.app.backup.FullBackup;
import android.common.HwFrameworkFactory;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.media.AudioAttributes.Builder;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;
import android.os.RemoteException;
import android.provider.Settings.System;
import android.util.Log;
import com.android.internal.R;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Ringtone {
    private static final boolean LOGD = true;
    private static final String[] MEDIA_COLUMNS = new String[]{DownloadManager.COLUMN_ID, "_data", "title"};
    private static final String MEDIA_SELECTION = "mime_type LIKE 'audio/%' OR mime_type IN ('application/ogg', 'application/x-flac')";
    private static final String TAG = "Ringtone";
    private static final ArrayList<Ringtone> sActiveRingtones = new ArrayList();
    private final boolean mAllowRemote;
    private AudioAttributes mAudioAttributes = new Builder().setUsage(6).setContentType(4).build();
    private final AudioManager mAudioManager;
    private final MyOnCompletionListener mCompletionListener = new MyOnCompletionListener();
    private final Context mContext;
    private boolean mIsLooping = false;
    private MediaPlayer mLocalPlayer;
    private final Object mPlaybackSettingsLock = new Object();
    private final IRingtonePlayer mRemotePlayer;
    private final Binder mRemoteToken;
    private final Object mStopPlayLock = new Object();
    private String mTitle;
    private int mType = -1;
    private Uri mUri;
    private float mVolume = 1.0f;
    private boolean prepareStat;

    class MyOnCompletionListener implements OnCompletionListener {
        MyOnCompletionListener() {
        }

        public void onCompletion(MediaPlayer mp) {
            synchronized (Ringtone.sActiveRingtones) {
                Ringtone.sActiveRingtones.remove(Ringtone.this);
            }
            mp.setOnCompletionListener(null);
        }
    }

    public Ringtone(Context context, boolean allowRemote) {
        this.mContext = context;
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mAllowRemote = allowRemote;
        Binder binder = null;
        this.mRemotePlayer = allowRemote ? this.mAudioManager.getRingtonePlayer() : null;
        if (allowRemote) {
            binder = new Binder();
        }
        this.mRemoteToken = binder;
    }

    @Deprecated
    public void setStreamType(int streamType) {
        PlayerBase.deprecateStreamTypeForPlayback(streamType, TAG, "setStreamType()");
        setAudioAttributes(new Builder().setInternalLegacyStreamType(streamType).build());
        HwMediaMonitorManager.writeMediaBigData(Process.myPid(), HwMediaMonitorManager.getStreamBigDataType(streamType), TAG);
    }

    @Deprecated
    public int getStreamType() {
        return AudioAttributes.toLegacyStreamType(this.mAudioAttributes);
    }

    public void setAudioAttributes(AudioAttributes attributes) throws IllegalArgumentException {
        if (attributes != null) {
            this.mAudioAttributes = attributes;
            setUri(this.mUri);
            return;
        }
        throw new IllegalArgumentException("Invalid null AudioAttributes for Ringtone");
    }

    public AudioAttributes getAudioAttributes() {
        return this.mAudioAttributes;
    }

    public void setLooping(boolean looping) {
        synchronized (this.mPlaybackSettingsLock) {
            this.mIsLooping = looping;
            applyPlaybackProperties_sync();
        }
    }

    public boolean isLooping() {
        boolean z;
        synchronized (this.mPlaybackSettingsLock) {
            z = this.mIsLooping;
        }
        return z;
    }

    public void setVolume(float volume) {
        synchronized (this.mPlaybackSettingsLock) {
            if (volume < 0.0f) {
                volume = 0.0f;
            }
            if (volume > 1.0f) {
                volume = 1.0f;
            }
            this.mVolume = volume;
            applyPlaybackProperties_sync();
        }
    }

    public float getVolume() {
        float f;
        synchronized (this.mPlaybackSettingsLock) {
            f = this.mVolume;
        }
        return f;
    }

    private void applyPlaybackProperties_sync() {
        if (this.mLocalPlayer != null) {
            this.mLocalPlayer.setVolume(this.mVolume);
            this.mLocalPlayer.setLooping(this.mIsLooping);
        } else if (!this.mAllowRemote || this.mRemotePlayer == null) {
            Log.w(TAG, "Neither local nor remote player available when applying playback properties");
        } else {
            try {
                this.mRemotePlayer.setPlaybackProperties(this.mRemoteToken, this.mVolume, this.mIsLooping);
            } catch (RemoteException e) {
                Log.w(TAG, "Problem setting playback properties: ", e);
            }
        }
    }

    public String getTitle(Context context) {
        if (this.mTitle != null) {
            return this.mTitle;
        }
        String title = getTitle(context, this.mUri, true, this.mAllowRemote);
        this.mTitle = title;
        return title;
    }

    /* JADX WARNING: Missing block: B:22:0x006b, code skipped:
            if (r10 != null) goto L_0x006d;
     */
    /* JADX WARNING: Missing block: B:23:0x006d, code skipped:
            r10.close();
     */
    /* JADX WARNING: Missing block: B:41:0x0095, code skipped:
            if (r10 == null) goto L_0x0070;
     */
    /* JADX WARNING: Missing block: B:42:0x0098, code skipped:
            if (r7 != null) goto L_0x00a4;
     */
    /* JADX WARNING: Missing block: B:43:0x009a, code skipped:
            r7 = "";
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static String getTitle(Context context, Uri uri, boolean followSettingsUri, boolean allowRemote) {
        ContentResolver res = context.getContentResolver();
        String title = null;
        if (uri != null) {
            String authority = ContentProvider.getAuthorityWithoutUserId(uri.getAuthority());
            if (!"settings".equals(authority)) {
                String str = null;
                Cursor cursor = null;
                try {
                    if ("media".equals(authority)) {
                        if (!allowRemote) {
                            str = MEDIA_SELECTION;
                        }
                        String mediaSelection = str;
                        cursor = res.query(uri, MEDIA_COLUMNS, mediaSelection, null, null);
                        if (cursor != null && cursor.getCount() == 1) {
                            cursor.moveToFirst();
                            str = cursor.getString(2);
                            if (cursor != null) {
                                cursor.close();
                            }
                            return str;
                        }
                    }
                } catch (SecurityException e) {
                    IRingtonePlayer mRemotePlayer = null;
                    if (allowRemote) {
                        mRemotePlayer = ((AudioManager) context.getSystemService("audio")).getRingtonePlayer();
                    }
                    if (mRemotePlayer != null) {
                        try {
                            title = mRemotePlayer.getTitle(uri);
                        } catch (RemoteException e2) {
                        }
                    }
                } catch (Throwable th) {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            } else if (followSettingsUri) {
                title = context.getString(R.string.ringtone_default_with_actual, getTitle(context, RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.getDefaultType(uri)), false, allowRemote));
            }
        } else {
            title = context.getString(R.string.ringtone_silent);
        }
        if (title == null) {
            title = context.getString(R.string.ringtone_unknown);
            if (title == null) {
                title = "";
            }
        }
        return title;
    }

    private Uri handleSettingsUri(Uri uri) {
        Uri fileUri = uri;
        Cursor mediaCursor = null;
        int ringtoneType = RingtoneManager.getDefaultType(uri);
        if (ringtoneType != -1) {
            try {
                Uri mediaUri = RingtoneManager.getActualDefaultRingtoneUri(this.mContext, ringtoneType);
                if (mediaUri != null) {
                    mediaCursor = this.mContext.getContentResolver().query(mediaUri, new String[]{"_data"}, null, null, null);
                    if (mediaCursor != null && mediaCursor.getCount() > 0) {
                        mediaCursor.moveToFirst();
                        if (new File(mediaCursor.getString(null)).exists()) {
                            fileUri = mediaUri;
                        }
                    }
                }
                if (mediaCursor != null) {
                    mediaCursor.close();
                }
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ringtone uri convert failed ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                if (mediaCursor != null) {
                    mediaCursor.close();
                }
                return uri;
            } catch (Throwable th) {
                if (mediaCursor != null) {
                    mediaCursor.close();
                }
                throw th;
            }
        }
        return fileUri;
    }

    public void setType(int type) {
        this.mType = type;
    }

    private AssetFileDescriptor getDefaultFd() {
        Uri uri = RingtoneManager.getDefaultUri(this.mType);
        if (uri == null) {
            uri = System.DEFAULT_RINGTONE_URI;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Using default uri ");
        stringBuilder.append(uri);
        Log.i(str, stringBuilder.toString());
        try {
            return this.mContext.getContentResolver().openAssetFileDescriptor(uri, FullBackup.ROOT_TREE_TOKEN);
        } catch (IOException | SecurityException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Open file failed, goto fallback, uri=");
            stringBuilder2.append(uri);
            Log.w(str2, stringBuilder2.toString());
            return null;
        }
    }

    public void setUri(Uri uri) {
        destroyLocalPlayer();
        this.mType = RingtoneManager.getDefaultType(uri);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setUri uri=");
        stringBuilder.append(uri != null ? uri.toString().replaceAll("\t|\r|\n", "") : null);
        Log.i(str, stringBuilder.toString());
        this.mUri = uri;
        if (this.mUri != null) {
            Uri fileUri = handleSettingsUri(uri);
            this.mLocalPlayer = new MediaPlayer();
            this.prepareStat = false;
            try {
                if (this.mLocalPlayer != null) {
                    this.mLocalPlayer.setDataSource(this.mContext, uri);
                    this.mLocalPlayer.setAudioAttributes(this.mAudioAttributes);
                    synchronized (this.mPlaybackSettingsLock) {
                        applyPlaybackProperties_sync();
                    }
                    this.mLocalPlayer.prepare();
                }
            } catch (IOException | IllegalArgumentException | IllegalStateException | SecurityException e) {
                Log.i(TAG, "Local player open file failed.");
                this.prepareStat = true;
                destroyLocalPlayer();
                if (!this.mAllowRemote) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Remote playback not allowed: ");
                    stringBuilder2.append(e);
                    Log.w(str2, stringBuilder2.toString());
                }
            }
            if (this.mLocalPlayer != null) {
                Log.d(TAG, "Successfully created local player");
            } else {
                Log.d(TAG, "Problem opening; delegating to remote player");
            }
        }
    }

    public boolean getPrepareStat() {
        return this.prepareStat;
    }

    public Uri getUri() {
        return this.mUri;
    }

    public void play() {
        if (this.mLocalPlayer != null) {
            if (this.mAudioManager.getStreamVolume(AudioAttributes.toLegacyStreamType(this.mAudioAttributes)) != 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Ringtone is playing = ");
                stringBuilder.append(isPlaying());
                Log.d(str, stringBuilder.toString());
                if (!isPlaying()) {
                    this.mLocalPlayer.seekTo(0);
                }
                startLocalPlayer();
            }
        } else if (this.mAllowRemote && this.mRemotePlayer != null && this.mUri != null) {
            boolean looping;
            float volume;
            Uri canonicalUri = this.mUri.getCanonicalUri();
            synchronized (this.mPlaybackSettingsLock) {
                looping = this.mIsLooping;
                volume = this.mVolume;
            }
            try {
                this.mRemotePlayer.play(this.mRemoteToken, canonicalUri, this.mAudioAttributes, volume, looping);
            } catch (RemoteException | IllegalStateException e) {
                if (!playFallbackRingtone()) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Problem playing ringtone: ");
                    stringBuilder2.append(e);
                    Log.w(str2, stringBuilder2.toString());
                }
            }
        } else if (!playFallbackRingtone()) {
            Log.w(TAG, "Neither local nor remote playback available");
        }
    }

    public void stop() {
        if (this.mLocalPlayer != null) {
            destroyLocalPlayer();
        } else if (this.mAllowRemote && this.mRemotePlayer != null) {
            try {
                this.mRemotePlayer.stop(this.mRemoteToken);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Problem stopping ringtone: ");
                stringBuilder.append(e);
                Log.w(str, stringBuilder.toString());
            }
        }
    }

    private void destroyLocalPlayer() {
        synchronized (this.mStopPlayLock) {
            if (this.mLocalPlayer != null) {
                this.mLocalPlayer.setOnCompletionListener(null);
                this.mLocalPlayer.reset();
                this.mLocalPlayer.release();
                this.mLocalPlayer = null;
                synchronized (sActiveRingtones) {
                    sActiveRingtones.remove(this);
                }
            }
        }
    }

    private void startLocalPlayer() {
        if (this.mLocalPlayer != null) {
            synchronized (sActiveRingtones) {
                sActiveRingtones.add(this);
            }
            this.mLocalPlayer.setOnCompletionListener(this.mCompletionListener);
            this.mLocalPlayer.start();
        }
    }

    public boolean isPlaying() {
        if (this.mLocalPlayer != null) {
            return this.mLocalPlayer.isPlaying();
        }
        if (!this.mAllowRemote || this.mRemotePlayer == null) {
            Log.w(TAG, "Neither local nor remote playback available");
            return false;
        }
        try {
            return this.mRemotePlayer.isPlaying(this.mRemoteToken);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Problem checking ringtone: ");
            stringBuilder.append(e);
            Log.w(str, stringBuilder.toString());
            return false;
        }
    }

    private boolean playFallbackRingtone() {
        if (this.mAudioManager.getStreamVolume(AudioAttributes.toLegacyStreamType(this.mAudioAttributes)) != 0) {
            int ringtoneType = RingtoneManager.getDefaultType(this.mUri);
            if (ringtoneType == -1 || RingtoneManager.getActualDefaultRingtoneUri(this.mContext, ringtoneType) != null) {
                AssetFileDescriptor afd = null;
                try {
                    afd = getDefaultFd();
                    if (afd == null) {
                        afd = this.mContext.getResources().openRawResourceFd(R.raw.fallbackring);
                    }
                    if (afd != null) {
                        this.mLocalPlayer = new MediaPlayer();
                        if (afd.getDeclaredLength() < 0) {
                            this.mLocalPlayer.setDataSource(afd.getFileDescriptor());
                        } else {
                            this.mLocalPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getDeclaredLength());
                        }
                        this.mLocalPlayer.setAudioAttributes(this.mAudioAttributes);
                        synchronized (this.mPlaybackSettingsLock) {
                            applyPlaybackProperties_sync();
                        }
                        this.mLocalPlayer.prepare();
                        startLocalPlayer();
                        if (afd != null) {
                            try {
                                afd.close();
                            } catch (Exception e) {
                                Log.w(TAG, "close afd error.");
                            }
                        }
                        return true;
                    }
                    Log.e(TAG, "Could not load fallback ringtone");
                    if (afd != null) {
                        try {
                            afd.close();
                        } catch (Exception e2) {
                            Log.w(TAG, "close afd error.");
                        }
                    }
                } catch (IOException e3) {
                    destroyLocalPlayer();
                    Log.e(TAG, "Failed to open fallback ringtone");
                    HwFrameworkFactory.getLogException().msg("app-ringtone", 65, "ringtone", "Failed to open fallback ringtone");
                    if (afd != null) {
                        afd.close();
                    }
                } catch (NotFoundException e4) {
                    Log.e(TAG, "Fallback ringtone does not exist ");
                    if (afd != null) {
                        afd.close();
                    }
                } catch (IllegalStateException e5) {
                    try {
                        Log.e(TAG, "Illegal ringtone Exception: ");
                        if (afd != null) {
                            afd.close();
                        }
                    } catch (Throwable th) {
                        if (afd != null) {
                            try {
                                afd.close();
                            } catch (Exception e6) {
                                Log.w(TAG, "close afd error.");
                            }
                        }
                    }
                }
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("not playing fallback for ");
                stringBuilder.append(this.mUri);
                Log.w(str, stringBuilder.toString());
            }
        }
        return false;
    }

    void setTitle(String title) {
        this.mTitle = title;
    }

    protected void finalize() {
        if (this.mLocalPlayer != null) {
            this.mLocalPlayer.release();
        }
    }
}
