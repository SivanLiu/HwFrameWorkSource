package android.support.v4.media;

import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.GuardedBy;
import android.support.v4.media.MediaSessionService2.MediaNotification;
import android.util.Log;

class MediaSessionService2ImplBase implements SupportLibraryImpl {
    private static final boolean DEBUG = true;
    private static final String TAG = "MSS2ImplBase";
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private MediaSession2 mSession;

    MediaSessionService2ImplBase() {
    }

    public void onCreate(MediaSessionService2 service) {
        SessionToken2 token = new SessionToken2(service, new ComponentName(service, service.getClass().getName()));
        if (token.getType() == getSessionType()) {
            MediaSession2 session = service.onCreateSession(token.getId());
            synchronized (this.mLock) {
                this.mSession = session;
                if (this.mSession != null && token.getId().equals(this.mSession.getToken().getId()) && this.mSession.getToken().getType() == getSessionType()) {
                } else {
                    this.mSession = null;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Expected session with id ");
                    stringBuilder.append(token.getId());
                    stringBuilder.append(" and type ");
                    stringBuilder.append(token.getType());
                    stringBuilder.append(", but got ");
                    stringBuilder.append(this.mSession);
                    throw new RuntimeException(stringBuilder.toString());
                }
            }
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Expected session type ");
        stringBuilder2.append(getSessionType());
        stringBuilder2.append(" but was ");
        stringBuilder2.append(token.getType());
        throw new RuntimeException(stringBuilder2.toString());
    }

    public IBinder onBind(Intent intent) {
        if (MediaSessionService2.SERVICE_INTERFACE.equals(intent.getAction())) {
            synchronized (this.mLock) {
                if (this.mSession != null) {
                    IBinder sessionBinder = this.mSession.getSessionBinder();
                    return sessionBinder;
                }
                Log.d(TAG, "Session hasn't created");
            }
        }
        return null;
    }

    public MediaNotification onUpdateNotification() {
        return null;
    }

    public MediaSession2 getSession() {
        MediaSession2 mediaSession2;
        synchronized (this.mLock) {
            mediaSession2 = this.mSession;
        }
        return mediaSession2;
    }

    public int getSessionType() {
        return 1;
    }
}
