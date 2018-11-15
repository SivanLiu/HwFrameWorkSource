package android.support.v4.media;

import android.os.BadParcelableException;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompat.BrowserRoot;
import android.support.v4.media.MediaBrowserServiceCompat.Result;
import android.support.v4.media.MediaLibraryService2.LibraryRoot;
import android.support.v4.media.MediaSession2.ControllerInfo;
import android.support.v4.media.MediaSessionManager.RemoteUserInfo;
import java.util.List;

class MediaLibraryService2LegacyStub extends MediaBrowserServiceCompat {
    private final SupportLibraryImpl mLibrarySession;

    MediaLibraryService2LegacyStub(SupportLibraryImpl session) {
        this.mLibrarySession = session;
    }

    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle extras) {
        if (MediaUtils2.isDefaultLibraryRootHint(extras)) {
            return MediaUtils2.sDefaultBrowserRoot;
        }
        LibraryRoot libraryRoot = this.mLibrarySession.getCallback().onGetLibraryRoot(this.mLibrarySession.getInstance(), getController(), extras);
        if (libraryRoot == null) {
            return null;
        }
        return new BrowserRoot(libraryRoot.getRootId(), libraryRoot.getExtras());
    }

    public void onLoadChildren(String parentId, Result<List<MediaItem>> result) {
        onLoadChildren(parentId, result, null);
    }

    public void onLoadChildren(String parentId, Result<List<MediaItem>> result, Bundle options) {
        result.detach();
        final Bundle bundle = options;
        final ControllerInfo controller = getController();
        final String str = parentId;
        final Result<List<MediaItem>> result2 = result;
        this.mLibrarySession.getCallbackExecutor().execute(new Runnable() {
            public void run() {
                if (bundle != null) {
                    bundle.setClassLoader(MediaLibraryService2LegacyStub.this.mLibrarySession.getContext().getClassLoader());
                    try {
                        int page = bundle.getInt(MediaBrowserCompat.EXTRA_PAGE);
                        int pageSize = bundle.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE);
                        if (page > 0 && pageSize > 0) {
                            result2.sendResult(MediaUtils2.convertToMediaItemList(MediaLibraryService2LegacyStub.this.mLibrarySession.getCallback().onGetChildren(MediaLibraryService2LegacyStub.this.mLibrarySession.getInstance(), controller, str, page, pageSize, bundle)));
                            return;
                        }
                    } catch (BadParcelableException e) {
                    }
                }
                result2.sendResult(MediaUtils2.convertToMediaItemList(MediaLibraryService2LegacyStub.this.mLibrarySession.getCallback().onGetChildren(MediaLibraryService2LegacyStub.this.mLibrarySession.getInstance(), controller, str, 1, Integer.MAX_VALUE, null)));
            }
        });
    }

    public void onLoadItem(final String itemId, final Result<MediaItem> result) {
        result.detach();
        final ControllerInfo controller = getController();
        this.mLibrarySession.getCallbackExecutor().execute(new Runnable() {
            public void run() {
                MediaItem2 item = MediaLibraryService2LegacyStub.this.mLibrarySession.getCallback().onGetItem(MediaLibraryService2LegacyStub.this.mLibrarySession.getInstance(), controller, itemId);
                if (item == null) {
                    result.sendResult(null);
                } else {
                    result.sendResult(MediaUtils2.convertToMediaItem(item));
                }
            }
        });
    }

    public void onSearch(String query, Bundle extras, Result<List<MediaItem>> result) {
        final Bundle bundle = extras;
        result.detach();
        final ControllerInfo controller = getController();
        bundle.setClassLoader(this.mLibrarySession.getContext().getClassLoader());
        final String str;
        try {
            int page = bundle.getInt(MediaBrowserCompat.EXTRA_PAGE);
            int pageSize = bundle.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE);
            if (page <= 0 || pageSize <= 0) {
                str = query;
                try {
                    this.mLibrarySession.getCallbackExecutor().execute(new Runnable() {
                        public void run() {
                            MediaLibraryService2LegacyStub.this.mLibrarySession.getCallback().onSearch(MediaLibraryService2LegacyStub.this.mLibrarySession.getInstance(), controller, str, bundle);
                        }
                    });
                    return;
                } catch (BadParcelableException e) {
                    return;
                }
            }
            final ControllerInfo controllerInfo = controller;
            final String str2 = query;
            final int i = page;
            final int i2 = pageSize;
            final Bundle bundle2 = bundle;
            final Result<List<MediaItem>> result2 = result;
            this.mLibrarySession.getCallbackExecutor().execute(new Runnable() {
                public void run() {
                    List<MediaItem2> searchResult = MediaLibraryService2LegacyStub.this.mLibrarySession.getCallback().onGetSearchResult(MediaLibraryService2LegacyStub.this.mLibrarySession.getInstance(), controllerInfo, str2, i, i2, bundle2);
                    if (searchResult == null) {
                        result2.sendResult(null);
                    } else {
                        result2.sendResult(MediaUtils2.convertToMediaItemList(searchResult));
                    }
                }
            });
            str = query;
        } catch (BadParcelableException e2) {
            str = query;
        }
    }

    public void onCustomAction(String action, Bundle extras, Result<Bundle> result) {
    }

    private ControllerInfo getController() {
        List<ControllerInfo> controllers = this.mLibrarySession.getConnectedControllers();
        RemoteUserInfo info = getCurrentBrowserInfo();
        if (info == null) {
            return null;
        }
        for (int i = 0; i < controllers.size(); i++) {
            ControllerInfo controller = (ControllerInfo) controllers.get(i);
            if (controller.getPackageName().equals(info.getPackageName()) && controller.getUid() == info.getUid()) {
                return controller;
            }
        }
        return null;
    }
}
