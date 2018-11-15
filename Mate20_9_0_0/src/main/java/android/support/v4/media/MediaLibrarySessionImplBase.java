package android.support.v4.media;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.GuardedBy;
import android.support.v4.media.MediaLibraryService2.LibraryRoot;
import android.support.v4.media.MediaLibraryService2.MediaLibrarySession;
import android.support.v4.media.MediaLibraryService2.MediaLibrarySession.MediaLibrarySessionCallback;
import android.support.v4.media.MediaSession2.ControllerInfo;
import android.support.v4.media.MediaSession2.SessionCallback;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@TargetApi(19)
class MediaLibrarySessionImplBase extends MediaSession2ImplBase implements SupportLibraryImpl {
    private final MediaBrowserServiceCompat mBrowserServiceLegacyStub = new MediaLibraryService2LegacyStub(this);
    @GuardedBy("mLock")
    private final ArrayMap<ControllerInfo, Set<String>> mSubscriptions = new ArrayMap();

    MediaLibrarySessionImplBase(MediaLibrarySession instance, Context context, String id, BaseMediaPlayer player, MediaPlaylistAgent playlistAgent, VolumeProviderCompat volumeProvider, PendingIntent sessionActivity, Executor callbackExecutor, SessionCallback callback) {
        super(instance, context, id, player, playlistAgent, volumeProvider, sessionActivity, callbackExecutor, callback);
        this.mBrowserServiceLegacyStub.attachToBaseContext(context);
        this.mBrowserServiceLegacyStub.onCreate();
    }

    public MediaLibrarySession getInstance() {
        return (MediaLibrarySession) super.getInstance();
    }

    public MediaLibrarySessionCallback getCallback() {
        return (MediaLibrarySessionCallback) super.getCallback();
    }

    public IBinder getLegacySessionBinder() {
        return this.mBrowserServiceLegacyStub.onBind(new Intent(MediaBrowserServiceCompat.SERVICE_INTERFACE));
    }

    public void notifyChildrenChanged(final String parentId, final int itemCount, final Bundle extras) {
        if (TextUtils.isEmpty(parentId)) {
            throw new IllegalArgumentException("query shouldn't be empty");
        } else if (itemCount >= 0) {
            List<ControllerInfo> controllers = getConnectedControllers();
            NotifyRunnable runnable = new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onChildrenChanged(parentId, itemCount, extras);
                }
            };
            for (int i = 0; i < controllers.size(); i++) {
                if (isSubscribed((ControllerInfo) controllers.get(i), parentId)) {
                    notifyToController((ControllerInfo) controllers.get(i), runnable);
                }
            }
        } else {
            throw new IllegalArgumentException("itemCount shouldn't be negative");
        }
    }

    public void notifyChildrenChanged(ControllerInfo controller, String parentId, int itemCount, Bundle extras) {
        if (controller == null) {
            throw new IllegalArgumentException("controller shouldn't be null");
        } else if (TextUtils.isEmpty(parentId)) {
            throw new IllegalArgumentException("query shouldn't be empty");
        } else if (itemCount >= 0) {
            final ControllerInfo controllerInfo = controller;
            final String str = parentId;
            final int i = itemCount;
            final Bundle bundle = extras;
            notifyToController(controller, new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    if (MediaLibrarySessionImplBase.this.isSubscribed(controllerInfo, str)) {
                        callback.onChildrenChanged(str, i, bundle);
                        return;
                    }
                    if (MediaSession2ImplBase.DEBUG) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Skipping notifyChildrenChanged() to ");
                        stringBuilder.append(controllerInfo);
                        stringBuilder.append(" because it hasn't subscribed");
                        Log.d("MS2ImplBase", stringBuilder.toString());
                        MediaLibrarySessionImplBase.this.dumpSubscription();
                    }
                }
            });
        } else {
            throw new IllegalArgumentException("itemCount shouldn't be negative");
        }
    }

    public void notifySearchResultChanged(ControllerInfo controller, final String query, final int itemCount, final Bundle extras) {
        if (controller == null) {
            throw new IllegalArgumentException("controller shouldn't be null");
        } else if (TextUtils.isEmpty(query)) {
            throw new IllegalArgumentException("query shouldn't be empty");
        } else {
            notifyToController(controller, new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onSearchResultChanged(query, itemCount, extras);
                }
            });
        }
    }

    public void onGetLibraryRootOnExecutor(ControllerInfo controller, final Bundle rootHints) {
        final LibraryRoot root = getCallback().onGetLibraryRoot(getInstance(), controller, rootHints);
        notifyToController(controller, new NotifyRunnable() {
            public void run(ControllerCb callback) throws RemoteException {
                Bundle bundle = rootHints;
                Bundle bundle2 = null;
                String rootId = root == null ? null : root.getRootId();
                if (root != null) {
                    bundle2 = root.getExtras();
                }
                callback.onGetLibraryRootDone(bundle, rootId, bundle2);
            }
        });
    }

    public void onGetItemOnExecutor(ControllerInfo controller, final String mediaId) {
        final MediaItem2 result = getCallback().onGetItem(getInstance(), controller, mediaId);
        notifyToController(controller, new NotifyRunnable() {
            public void run(ControllerCb callback) throws RemoteException {
                callback.onGetItemDone(mediaId, result);
            }
        });
    }

    public void onGetChildrenOnExecutor(ControllerInfo controller, String parentId, int page, int pageSize, Bundle extras) {
        List<MediaItem2> result = getCallback().onGetChildren(getInstance(), controller, parentId, page, pageSize, extras);
        if (result == null || result.size() <= pageSize) {
            final String str = parentId;
            final int i = page;
            final int i2 = pageSize;
            final List<MediaItem2> list = result;
            final Bundle bundle = extras;
            notifyToController(controller, new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onGetChildrenDone(str, i, i2, list, bundle);
                }
            });
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onGetChildren() shouldn't return media items more than pageSize. result.size()=");
        stringBuilder.append(result.size());
        stringBuilder.append(" pageSize=");
        stringBuilder.append(pageSize);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void onSubscribeOnExecutor(ControllerInfo controller, String parentId, Bundle option) {
        synchronized (this.mLock) {
            Set<String> subscription = (Set) this.mSubscriptions.get(controller);
            if (subscription == null) {
                subscription = new HashSet();
                this.mSubscriptions.put(controller, subscription);
            }
            subscription.add(parentId);
        }
        getCallback().onSubscribe(getInstance(), controller, parentId, option);
    }

    public void onUnsubscribeOnExecutor(ControllerInfo controller, String parentId) {
        getCallback().onUnsubscribe(getInstance(), controller, parentId);
        synchronized (this.mLock) {
            this.mSubscriptions.remove(controller);
        }
    }

    public void onSearchOnExecutor(ControllerInfo controller, String query, Bundle extras) {
        getCallback().onSearch(getInstance(), controller, query, extras);
    }

    public void onGetSearchResultOnExecutor(ControllerInfo controller, String query, int page, int pageSize, Bundle extras) {
        List<MediaItem2> result = getCallback().onGetSearchResult(getInstance(), controller, query, page, pageSize, extras);
        if (result == null || result.size() <= pageSize) {
            final String str = query;
            final int i = page;
            final int i2 = pageSize;
            final List<MediaItem2> list = result;
            final Bundle bundle = extras;
            notifyToController(controller, new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onGetSearchResultDone(str, i, i2, list, bundle);
                }
            });
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onGetSearchResult() shouldn't return media items more than pageSize. result.size()=");
        stringBuilder.append(result.size());
        stringBuilder.append(" pageSize=");
        stringBuilder.append(pageSize);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private boolean isSubscribed(ControllerInfo controller, String parentId) {
        synchronized (this.mLock) {
            Set<String> subscriptions = (Set) this.mSubscriptions.get(controller);
            if (subscriptions == null || !subscriptions.contains(parentId)) {
                return false;
            }
            return true;
        }
    }

    private void dumpSubscription() {
        if (DEBUG) {
            synchronized (this.mLock) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Dumping subscription, controller sz=");
                stringBuilder.append(this.mSubscriptions.size());
                Log.d("MS2ImplBase", stringBuilder.toString());
                for (int i = 0; i < this.mSubscriptions.size(); i++) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  controller ");
                    stringBuilder2.append(this.mSubscriptions.valueAt(i));
                    Log.d("MS2ImplBase", stringBuilder2.toString());
                    for (String parentId : (Set) this.mSubscriptions.valueAt(i)) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("  - ");
                        stringBuilder3.append(parentId);
                        Log.d("MS2ImplBase", stringBuilder3.toString());
                    }
                }
            }
        }
    }
}
