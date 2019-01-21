package android.media.session;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.MediaMetadata;
import android.media.MediaMetadataEditor;
import android.media.Rating;
import android.media.session.MediaSession.Callback;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Log;
import android.view.KeyEvent;

public class MediaSessionLegacyHelper {
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String TAG = "MediaSessionHelper";
    private static MediaSessionLegacyHelper sInstance;
    private static final Object sLock = new Object();
    private Context mContext;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private MediaSessionManager mSessionManager;
    private ArrayMap<PendingIntent, SessionHolder> mSessions = new ArrayMap();

    private class SessionHolder {
        public SessionCallback mCb;
        public int mFlags;
        public MediaButtonListener mMediaButtonListener;
        public final PendingIntent mPi;
        public Callback mRccListener;
        public final MediaSession mSession;

        private class SessionCallback extends Callback {
            private SessionCallback() {
            }

            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                if (SessionHolder.this.mMediaButtonListener != null) {
                    SessionHolder.this.mMediaButtonListener.onMediaButtonEvent(mediaButtonIntent);
                }
                return true;
            }

            public void onPlay() {
                if (SessionHolder.this.mMediaButtonListener != null) {
                    SessionHolder.this.mMediaButtonListener.onPlay();
                }
            }

            public void onPause() {
                if (SessionHolder.this.mMediaButtonListener != null) {
                    SessionHolder.this.mMediaButtonListener.onPause();
                }
            }

            public void onSkipToNext() {
                if (SessionHolder.this.mMediaButtonListener != null) {
                    SessionHolder.this.mMediaButtonListener.onSkipToNext();
                }
            }

            public void onSkipToPrevious() {
                if (SessionHolder.this.mMediaButtonListener != null) {
                    SessionHolder.this.mMediaButtonListener.onSkipToPrevious();
                }
            }

            public void onFastForward() {
                if (SessionHolder.this.mMediaButtonListener != null) {
                    SessionHolder.this.mMediaButtonListener.onFastForward();
                }
            }

            public void onRewind() {
                if (SessionHolder.this.mMediaButtonListener != null) {
                    SessionHolder.this.mMediaButtonListener.onRewind();
                }
            }

            public void onStop() {
                if (SessionHolder.this.mMediaButtonListener != null) {
                    SessionHolder.this.mMediaButtonListener.onStop();
                }
            }

            public void onSeekTo(long pos) {
                if (SessionHolder.this.mRccListener != null) {
                    SessionHolder.this.mRccListener.onSeekTo(pos);
                }
            }

            public void onSetRating(Rating rating) {
                if (SessionHolder.this.mRccListener != null) {
                    SessionHolder.this.mRccListener.onSetRating(rating);
                }
            }
        }

        public SessionHolder(MediaSession session, PendingIntent pi) {
            this.mSession = session;
            this.mPi = pi;
        }

        public void update() {
            if (this.mMediaButtonListener == null && this.mRccListener == null) {
                this.mSession.setCallback(null);
                this.mSession.release();
                this.mCb = null;
                MediaSessionLegacyHelper.this.mSessions.remove(this.mPi);
            } else if (this.mCb == null) {
                this.mCb = new SessionCallback();
                this.mSession.setCallback(this.mCb, new Handler(Looper.getMainLooper()));
            }
        }
    }

    private static final class MediaButtonListener extends Callback {
        private final Context mContext;
        private final PendingIntent mPendingIntent;

        public MediaButtonListener(PendingIntent pi, Context context) {
            this.mPendingIntent = pi;
            this.mContext = context;
        }

        public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
            MediaSessionLegacyHelper.sendKeyEvent(this.mPendingIntent, this.mContext, mediaButtonIntent);
            return true;
        }

        public void onPlay() {
            sendKeyEvent(126);
        }

        public void onPause() {
            sendKeyEvent(127);
        }

        public void onSkipToNext() {
            sendKeyEvent(87);
        }

        public void onSkipToPrevious() {
            sendKeyEvent(88);
        }

        public void onFastForward() {
            sendKeyEvent(90);
        }

        public void onRewind() {
            sendKeyEvent(89);
        }

        public void onStop() {
            sendKeyEvent(86);
        }

        private void sendKeyEvent(int keyCode) {
            Parcelable ke = new KeyEvent(0, keyCode);
            Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            intent.addFlags(268435456);
            intent.putExtra(Intent.EXTRA_KEY_EVENT, ke);
            MediaSessionLegacyHelper.sendKeyEvent(this.mPendingIntent, this.mContext, intent);
            intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(1, keyCode));
            MediaSessionLegacyHelper.sendKeyEvent(this.mPendingIntent, this.mContext, intent);
            if (MediaSessionLegacyHelper.DEBUG) {
                String str = MediaSessionLegacyHelper.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Sent ");
                stringBuilder.append(keyCode);
                stringBuilder.append(" to pending intent ");
                stringBuilder.append(this.mPendingIntent);
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    private MediaSessionLegacyHelper(Context context) {
        this.mContext = context;
        this.mSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
    }

    public static MediaSessionLegacyHelper getHelper(Context context) {
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new MediaSessionLegacyHelper(context.getApplicationContext());
            }
        }
        return sInstance;
    }

    public static Bundle getOldMetadata(MediaMetadata metadata, int artworkWidth, int artworkHeight) {
        boolean includeArtwork = (artworkWidth == -1 || artworkHeight == -1) ? false : true;
        Bundle oldMetadata = new Bundle();
        if (metadata.containsKey("android.media.metadata.ALBUM")) {
            oldMetadata.putString(String.valueOf(1), metadata.getString("android.media.metadata.ALBUM"));
        }
        if (includeArtwork && metadata.containsKey("android.media.metadata.ART")) {
            oldMetadata.putParcelable(String.valueOf(100), scaleBitmapIfTooBig(metadata.getBitmap("android.media.metadata.ART"), artworkWidth, artworkHeight));
        } else if (includeArtwork && metadata.containsKey("android.media.metadata.ALBUM_ART")) {
            oldMetadata.putParcelable(String.valueOf(100), scaleBitmapIfTooBig(metadata.getBitmap("android.media.metadata.ALBUM_ART"), artworkWidth, artworkHeight));
        }
        if (metadata.containsKey("android.media.metadata.ALBUM_ARTIST")) {
            oldMetadata.putString(String.valueOf(13), metadata.getString("android.media.metadata.ALBUM_ARTIST"));
        }
        if (metadata.containsKey("android.media.metadata.ARTIST")) {
            oldMetadata.putString(String.valueOf(2), metadata.getString("android.media.metadata.ARTIST"));
        }
        if (metadata.containsKey("android.media.metadata.AUTHOR")) {
            oldMetadata.putString(String.valueOf(3), metadata.getString("android.media.metadata.AUTHOR"));
        }
        if (metadata.containsKey("android.media.metadata.COMPILATION")) {
            oldMetadata.putString(String.valueOf(15), metadata.getString("android.media.metadata.COMPILATION"));
        }
        if (metadata.containsKey("android.media.metadata.COMPOSER")) {
            oldMetadata.putString(String.valueOf(4), metadata.getString("android.media.metadata.COMPOSER"));
        }
        if (metadata.containsKey("android.media.metadata.DATE")) {
            oldMetadata.putString(String.valueOf(5), metadata.getString("android.media.metadata.DATE"));
        }
        if (metadata.containsKey("android.media.metadata.DISC_NUMBER")) {
            oldMetadata.putLong(String.valueOf(14), metadata.getLong("android.media.metadata.DISC_NUMBER"));
        }
        if (metadata.containsKey("android.media.metadata.DURATION")) {
            oldMetadata.putLong(String.valueOf(9), metadata.getLong("android.media.metadata.DURATION"));
        }
        if (metadata.containsKey("android.media.metadata.GENRE")) {
            oldMetadata.putString(String.valueOf(6), metadata.getString("android.media.metadata.GENRE"));
        }
        if (metadata.containsKey("android.media.metadata.NUM_TRACKS")) {
            oldMetadata.putLong(String.valueOf(10), metadata.getLong("android.media.metadata.NUM_TRACKS"));
        }
        if (metadata.containsKey("android.media.metadata.RATING")) {
            oldMetadata.putParcelable(String.valueOf(101), metadata.getRating("android.media.metadata.RATING"));
        }
        if (metadata.containsKey("android.media.metadata.USER_RATING")) {
            oldMetadata.putParcelable(String.valueOf(MediaMetadataEditor.RATING_KEY_BY_USER), metadata.getRating("android.media.metadata.USER_RATING"));
        }
        if (metadata.containsKey("android.media.metadata.TITLE")) {
            oldMetadata.putString(String.valueOf(7), metadata.getString("android.media.metadata.TITLE"));
        }
        if (metadata.containsKey("android.media.metadata.TRACK_NUMBER")) {
            oldMetadata.putLong(String.valueOf(0), metadata.getLong("android.media.metadata.TRACK_NUMBER"));
        }
        if (metadata.containsKey("android.media.metadata.WRITER")) {
            oldMetadata.putString(String.valueOf(11), metadata.getString("android.media.metadata.WRITER"));
        }
        if (metadata.containsKey("android.media.metadata.YEAR")) {
            oldMetadata.putLong(String.valueOf(8), metadata.getLong("android.media.metadata.YEAR"));
        }
        if (metadata.containsKey(MediaMetadata.METADATA_KEY_LYRIC)) {
            oldMetadata.putString(String.valueOf(1000), metadata.getString(MediaMetadata.METADATA_KEY_LYRIC));
        }
        return oldMetadata;
    }

    public MediaSession getSession(PendingIntent pi) {
        SessionHolder holder = (SessionHolder) this.mSessions.get(pi);
        return holder == null ? null : holder.mSession;
    }

    public void sendMediaButtonEvent(KeyEvent keyEvent, boolean needWakeLock) {
        if (keyEvent == null) {
            Log.w(TAG, "Tried to send a null key event. Ignoring.");
            return;
        }
        this.mSessionManager.dispatchMediaKeyEvent(keyEvent, needWakeLock);
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("dispatched media key ");
            stringBuilder.append(keyEvent);
            Log.d(str, stringBuilder.toString());
        }
    }

    public void sendVolumeKeyEvent(KeyEvent keyEvent, int stream, boolean musicOnly) {
        if (keyEvent == null) {
            Log.w(TAG, "Tried to send a null key event. Ignoring.");
        } else {
            this.mSessionManager.dispatchVolumeKeyEvent(keyEvent, stream, musicOnly);
        }
    }

    public void sendAdjustVolumeBy(int suggestedStream, int delta, int flags) {
        this.mSessionManager.dispatchAdjustVolume(suggestedStream, delta, flags);
        if (DEBUG) {
            Log.d(TAG, "dispatched volume adjustment");
        }
    }

    public boolean isGlobalPriorityActive() {
        return this.mSessionManager.isGlobalPriorityActive();
    }

    public void addRccListener(PendingIntent pi, Callback listener) {
        if (pi == null) {
            Log.w(TAG, "Pending intent was null, can't add rcc listener.");
            return;
        }
        SessionHolder holder = getHolder(pi, true);
        if (holder != null) {
            if (holder.mRccListener == null || holder.mRccListener != listener) {
                holder.mRccListener = listener;
                holder.mFlags |= 2;
                holder.mSession.setFlags(holder.mFlags);
                holder.update();
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Added rcc listener for ");
                    stringBuilder.append(pi);
                    stringBuilder.append(".");
                    Log.d(str, stringBuilder.toString());
                }
                return;
            }
            if (DEBUG) {
                Log.d(TAG, "addRccListener listener already added.");
            }
        }
    }

    public void removeRccListener(PendingIntent pi) {
        if (pi != null) {
            SessionHolder holder = getHolder(pi, null);
            if (!(holder == null || holder.mRccListener == null)) {
                holder.mRccListener = null;
                holder.mFlags &= -3;
                holder.mSession.setFlags(holder.mFlags);
                holder.update();
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Removed rcc listener for ");
                    stringBuilder.append(pi);
                    stringBuilder.append(".");
                    Log.d(str, stringBuilder.toString());
                }
            }
        }
    }

    public void addMediaButtonListener(PendingIntent pi, ComponentName mbrComponent, Context context) {
        if (pi == null) {
            Log.w(TAG, "Pending intent was null, can't addMediaButtonListener.");
            return;
        }
        SessionHolder holder = getHolder(pi, true);
        if (holder != null) {
            if (holder.mMediaButtonListener != null && DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("addMediaButtonListener already added ");
                stringBuilder.append(pi);
                Log.d(str, stringBuilder.toString());
            }
            holder.mMediaButtonListener = new MediaButtonListener(pi, context);
            holder.mFlags = 1 | holder.mFlags;
            holder.mSession.setFlags(holder.mFlags);
            holder.mSession.setMediaButtonReceiver(pi);
            holder.update();
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("addMediaButtonListener added ");
                stringBuilder2.append(pi);
                Log.d(str2, stringBuilder2.toString());
            }
        }
    }

    public void removeMediaButtonListener(PendingIntent pi) {
        if (pi != null) {
            SessionHolder holder = getHolder(pi, null);
            if (!(holder == null || holder.mMediaButtonListener == null)) {
                holder.mFlags &= -2;
                holder.mSession.setFlags(holder.mFlags);
                holder.mMediaButtonListener = null;
                holder.update();
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("removeMediaButtonListener removed ");
                    stringBuilder.append(pi);
                    Log.d(str, stringBuilder.toString());
                }
            }
        }
    }

    private static Bitmap scaleBitmapIfTooBig(Bitmap bitmap, int maxWidth, int maxHeight) {
        Bitmap bitmap2 = bitmap;
        int i = maxWidth;
        int i2 = maxHeight;
        if (bitmap2 == null) {
            return bitmap2;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= i && height <= i2) {
            return bitmap2;
        }
        float scale = Math.min(((float) i) / ((float) width), ((float) i2) / ((float) height));
        int newWidth = Math.round(((float) width) * scale);
        int newHeight = Math.round(((float) height) * scale);
        Config newConfig = bitmap.getConfig();
        if (newConfig == null) {
            newConfig = Config.ARGB_8888;
        }
        Bitmap outBitmap = Bitmap.createBitmap(newWidth, newHeight, newConfig);
        Canvas canvas = new Canvas(outBitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        canvas.drawBitmap(bitmap2, null, new RectF(0.0f, 0.0f, (float) outBitmap.getWidth(), (float) outBitmap.getHeight()), paint);
        return outBitmap;
    }

    private SessionHolder getHolder(PendingIntent pi, boolean createIfMissing) {
        SessionHolder holder = (SessionHolder) this.mSessions.get(pi);
        if (holder != null || !createIfMissing) {
            return holder;
        }
        Context context = this.mContext;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("MediaSessionHelper-");
        stringBuilder.append(pi.getCreatorPackage());
        MediaSession session = new MediaSession(context, stringBuilder.toString());
        session.setActive(true);
        holder = new SessionHolder(session, pi);
        this.mSessions.put(pi, holder);
        return holder;
    }

    private static void sendKeyEvent(PendingIntent pi, Context context, Intent intent) {
        try {
            pi.send(context, 0, intent);
        } catch (CanceledException e) {
            Log.e(TAG, "Error sending media key down event:", e);
        }
    }
}
