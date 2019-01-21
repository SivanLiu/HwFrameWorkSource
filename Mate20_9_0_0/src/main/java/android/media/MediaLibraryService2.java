package android.media;

import android.app.PendingIntent;
import android.media.MediaSession2.ControllerInfo;
import android.media.MediaSession2.SessionCallback;
import android.media.update.ApiLoader;
import android.media.update.MediaLibraryService2Provider.LibraryRootProvider;
import android.media.update.MediaLibraryService2Provider.MediaLibrarySessionProvider;
import android.media.update.MediaSessionService2Provider;
import android.os.Bundle;
import java.util.List;
import java.util.concurrent.Executor;

public abstract class MediaLibraryService2 extends MediaSessionService2 {
    public static final String SERVICE_INTERFACE = "android.media.MediaLibraryService2";

    public static final class LibraryRoot {
        public static final String EXTRA_OFFLINE = "android.media.extra.OFFLINE";
        public static final String EXTRA_RECENT = "android.media.extra.RECENT";
        public static final String EXTRA_SUGGESTED = "android.media.extra.SUGGESTED";
        private final LibraryRootProvider mProvider;

        public LibraryRoot(String rootId, Bundle extras) {
            this.mProvider = ApiLoader.getProvider().createMediaLibraryService2LibraryRoot(this, rootId, extras);
        }

        public String getRootId() {
            return this.mProvider.getRootId_impl();
        }

        public Bundle getExtras() {
            return this.mProvider.getExtras_impl();
        }
    }

    public static final class MediaLibrarySession extends MediaSession2 {
        private final MediaLibrarySessionProvider mProvider;

        public static final class Builder extends BuilderBase<MediaLibrarySession, Builder, MediaLibrarySessionCallback> {
            public Builder(MediaLibraryService2 service, Executor callbackExecutor, MediaLibrarySessionCallback callback) {
                super(new -$$Lambda$MediaLibraryService2$MediaLibrarySession$Builder$KbvKQ6JiEvVRMpYadxywG_GUsco(service, callbackExecutor, callback));
            }

            public Builder setPlayer(MediaPlayerBase player) {
                return (Builder) super.setPlayer(player);
            }

            public Builder setPlaylistAgent(MediaPlaylistAgent playlistAgent) {
                return (Builder) super.setPlaylistAgent(playlistAgent);
            }

            public Builder setVolumeProvider(VolumeProvider2 volumeProvider) {
                return (Builder) super.setVolumeProvider(volumeProvider);
            }

            public Builder setSessionActivity(PendingIntent pi) {
                return (Builder) super.setSessionActivity(pi);
            }

            public Builder setId(String id) {
                return (Builder) super.setId(id);
            }

            public Builder setSessionCallback(Executor executor, MediaLibrarySessionCallback callback) {
                return (Builder) super.setSessionCallback(executor, callback);
            }

            public MediaLibrarySession build() {
                return (MediaLibrarySession) super.build();
            }
        }

        public static class MediaLibrarySessionCallback extends SessionCallback {
            public LibraryRoot onGetLibraryRoot(MediaLibrarySession session, ControllerInfo controllerInfo, Bundle rootHints) {
                return null;
            }

            public MediaItem2 onGetItem(MediaLibrarySession session, ControllerInfo controllerInfo, String mediaId) {
                return null;
            }

            public List<MediaItem2> onGetChildren(MediaLibrarySession session, ControllerInfo controller, String parentId, int page, int pageSize, Bundle extras) {
                return null;
            }

            public void onSubscribe(MediaLibrarySession session, ControllerInfo controller, String parentId, Bundle extras) {
            }

            public void onUnsubscribe(MediaLibrarySession session, ControllerInfo controller, String parentId) {
            }

            public void onSearch(MediaLibrarySession session, ControllerInfo controllerInfo, String query, Bundle extras) {
            }

            public List<MediaItem2> onGetSearchResult(MediaLibrarySession session, ControllerInfo controllerInfo, String query, int page, int pageSize, Bundle extras) {
                return null;
            }
        }

        public MediaLibrarySession(MediaLibrarySessionProvider provider) {
            super(provider);
            this.mProvider = provider;
        }

        public void notifyChildrenChanged(ControllerInfo controller, String parentId, int itemCount, Bundle extras) {
            this.mProvider.notifyChildrenChanged_impl(controller, parentId, itemCount, extras);
        }

        public void notifyChildrenChanged(String parentId, int itemCount, Bundle extras) {
            this.mProvider.notifyChildrenChanged_impl(parentId, itemCount, extras);
        }

        public void notifySearchResultChanged(ControllerInfo controller, String query, int itemCount, Bundle extras) {
            this.mProvider.notifySearchResultChanged_impl(controller, query, itemCount, extras);
        }
    }

    public abstract MediaLibrarySession onCreateSession(String str);

    MediaSessionService2Provider createProvider() {
        return ApiLoader.getProvider().createMediaLibraryService2(this);
    }
}
