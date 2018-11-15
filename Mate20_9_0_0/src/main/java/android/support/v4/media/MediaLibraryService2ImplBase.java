package android.support.v4.media;

import android.content.Intent;
import android.os.IBinder;
import android.support.v4.media.MediaLibraryService2.MediaLibrarySession;

class MediaLibraryService2ImplBase extends MediaSessionService2ImplBase {
    MediaLibraryService2ImplBase() {
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x002b  */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x003d  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0030  */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x002b  */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x003d  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0030  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public IBinder onBind(Intent intent) {
        Object obj;
        String action = intent.getAction();
        int hashCode = action.hashCode();
        if (hashCode == 901933117) {
            if (action.equals(MediaLibraryService2.SERVICE_INTERFACE)) {
                obj = null;
                switch (obj) {
                    case null:
                        break;
                    case 1:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 1665850838 && action.equals(MediaBrowserServiceCompat.SERVICE_INTERFACE)) {
            obj = 1;
            switch (obj) {
                case null:
                    return getSession().getSessionBinder();
                case 1:
                    return getSession().getImpl().getLegacySessionBinder();
                default:
                    return super.onBind(intent);
            }
        }
        obj = -1;
        switch (obj) {
            case null:
                break;
            case 1:
                break;
            default:
                break;
        }
    }

    public MediaLibrarySession getSession() {
        return (MediaLibrarySession) super.getSession();
    }

    public int getSessionType() {
        return 2;
    }
}
