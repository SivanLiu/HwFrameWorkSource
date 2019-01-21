package android.service.media;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.media.browse.MediaBrowser.MediaItem;
import android.media.browse.MediaBrowserUtils;
import android.media.session.MediaSession.Token;
import android.media.session.MediaSessionManager.RemoteUserInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.service.media.IMediaBrowserService.Stub;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public abstract class MediaBrowserService extends Service {
    private static final boolean DBG = false;
    public static final String KEY_MEDIA_ITEM = "media_item";
    private static final int RESULT_ERROR = -1;
    private static final int RESULT_FLAG_ON_LOAD_ITEM_NOT_IMPLEMENTED = 2;
    private static final int RESULT_FLAG_OPTION_NOT_HANDLED = 1;
    private static final int RESULT_OK = 0;
    public static final String SERVICE_INTERFACE = "android.media.browse.MediaBrowserService";
    private static final String TAG = "MediaBrowserService";
    private ServiceBinder mBinder;
    private final ArrayMap<IBinder, ConnectionRecord> mConnections = new ArrayMap();
    private ConnectionRecord mCurConnection;
    private final Handler mHandler = new Handler();
    Token mSession;

    public static final class BrowserRoot {
        public static final String EXTRA_OFFLINE = "android.service.media.extra.OFFLINE";
        public static final String EXTRA_RECENT = "android.service.media.extra.RECENT";
        public static final String EXTRA_SUGGESTED = "android.service.media.extra.SUGGESTED";
        private final Bundle mExtras;
        private final String mRootId;

        public BrowserRoot(String rootId, Bundle extras) {
            if (rootId != null) {
                this.mRootId = rootId;
                this.mExtras = extras;
                return;
            }
            throw new IllegalArgumentException("The root id in BrowserRoot cannot be null. Use null for BrowserRoot instead.");
        }

        public String getRootId() {
            return this.mRootId;
        }

        public Bundle getExtras() {
            return this.mExtras;
        }
    }

    public class Result<T> {
        private Object mDebug;
        private boolean mDetachCalled;
        private int mFlags;
        private boolean mSendResultCalled;

        Result(Object debug) {
            this.mDebug = debug;
        }

        public void sendResult(T result) {
            if (this.mSendResultCalled) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sendResult() called twice for: ");
                stringBuilder.append(this.mDebug);
                throw new IllegalStateException(stringBuilder.toString());
            }
            this.mSendResultCalled = true;
            onResultSent(result, this.mFlags);
        }

        public void detach() {
            StringBuilder stringBuilder;
            if (this.mDetachCalled) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("detach() called when detach() had already been called for: ");
                stringBuilder.append(this.mDebug);
                throw new IllegalStateException(stringBuilder.toString());
            } else if (this.mSendResultCalled) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("detach() called when sendResult() had already been called for: ");
                stringBuilder.append(this.mDebug);
                throw new IllegalStateException(stringBuilder.toString());
            } else {
                this.mDetachCalled = true;
            }
        }

        boolean isDone() {
            return this.mDetachCalled || this.mSendResultCalled;
        }

        void setFlags(int flags) {
            this.mFlags = flags;
        }

        void onResultSent(T t, int flags) {
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface ResultFlags {
    }

    private class ConnectionRecord implements DeathRecipient {
        IMediaBrowserServiceCallbacks callbacks;
        int pid;
        String pkg;
        BrowserRoot root;
        Bundle rootHints;
        HashMap<String, List<Pair<IBinder, Bundle>>> subscriptions;
        int uid;

        private ConnectionRecord() {
            this.subscriptions = new HashMap();
        }

        /* synthetic */ ConnectionRecord(MediaBrowserService x0, AnonymousClass1 x1) {
            this();
        }

        public void binderDied() {
            MediaBrowserService.this.mHandler.post(new Runnable() {
                public void run() {
                    MediaBrowserService.this.mConnections.remove(ConnectionRecord.this.callbacks.asBinder());
                }
            });
        }
    }

    private class ServiceBinder extends Stub {
        private ServiceBinder() {
        }

        /* synthetic */ ServiceBinder(MediaBrowserService x0, AnonymousClass1 x1) {
            this();
        }

        public void connect(String pkg, Bundle rootHints, IMediaBrowserServiceCallbacks callbacks) {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            if (MediaBrowserService.this.isValidPackage(pkg, uid)) {
                final IMediaBrowserServiceCallbacks iMediaBrowserServiceCallbacks = callbacks;
                final String str = pkg;
                final int i = pid;
                final int i2 = uid;
                final Bundle bundle = rootHints;
                MediaBrowserService.this.mHandler.post(new Runnable() {
                    public void run() {
                        String str;
                        StringBuilder stringBuilder;
                        IBinder b = iMediaBrowserServiceCallbacks.asBinder();
                        MediaBrowserService.this.mConnections.remove(b);
                        ConnectionRecord connection = new ConnectionRecord(MediaBrowserService.this, null);
                        connection.pkg = str;
                        connection.pid = i;
                        connection.uid = i2;
                        connection.rootHints = bundle;
                        connection.callbacks = iMediaBrowserServiceCallbacks;
                        MediaBrowserService.this.mCurConnection = connection;
                        connection.root = MediaBrowserService.this.onGetRoot(str, i2, bundle);
                        MediaBrowserService.this.mCurConnection = null;
                        if (connection.root == null) {
                            String str2 = MediaBrowserService.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("No root for client ");
                            stringBuilder2.append(str);
                            stringBuilder2.append(" from service ");
                            stringBuilder2.append(getClass().getName());
                            Log.i(str2, stringBuilder2.toString());
                            try {
                                iMediaBrowserServiceCallbacks.onConnectFailed();
                                return;
                            } catch (RemoteException e) {
                                str = MediaBrowserService.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Calling onConnectFailed() failed. Ignoring. pkg=");
                                stringBuilder.append(str);
                                Log.w(str, stringBuilder.toString());
                                return;
                            }
                        }
                        try {
                            MediaBrowserService.this.mConnections.put(b, connection);
                            b.linkToDeath(connection, 0);
                            if (MediaBrowserService.this.mSession != null) {
                                iMediaBrowserServiceCallbacks.onConnect(connection.root.getRootId(), MediaBrowserService.this.mSession, connection.root.getExtras());
                            }
                        } catch (RemoteException e2) {
                            str = MediaBrowserService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Calling onConnect() failed. Dropping client. pkg=");
                            stringBuilder.append(str);
                            Log.w(str, stringBuilder.toString());
                            MediaBrowserService.this.mConnections.remove(b);
                        }
                    }
                });
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Package/uid mismatch: uid=");
            stringBuilder.append(uid);
            stringBuilder.append(" package=");
            stringBuilder.append(pkg);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public void disconnect(final IMediaBrowserServiceCallbacks callbacks) {
            MediaBrowserService.this.mHandler.post(new Runnable() {
                public void run() {
                    ConnectionRecord old = (ConnectionRecord) MediaBrowserService.this.mConnections.remove(callbacks.asBinder());
                    if (old != null) {
                        old.callbacks.asBinder().unlinkToDeath(old, 0);
                    }
                }
            });
        }

        public void addSubscriptionDeprecated(String id, IMediaBrowserServiceCallbacks callbacks) {
        }

        public void addSubscription(String id, IBinder token, Bundle options, IMediaBrowserServiceCallbacks callbacks) {
            final IMediaBrowserServiceCallbacks iMediaBrowserServiceCallbacks = callbacks;
            final String str = id;
            final IBinder iBinder = token;
            final Bundle bundle = options;
            MediaBrowserService.this.mHandler.post(new Runnable() {
                public void run() {
                    ConnectionRecord connection = (ConnectionRecord) MediaBrowserService.this.mConnections.get(iMediaBrowserServiceCallbacks.asBinder());
                    if (connection == null) {
                        String str = MediaBrowserService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("addSubscription for callback that isn't registered id=");
                        stringBuilder.append(str);
                        Log.w(str, stringBuilder.toString());
                        return;
                    }
                    MediaBrowserService.this.addSubscription(str, connection, iBinder, bundle);
                }
            });
        }

        public void removeSubscriptionDeprecated(String id, IMediaBrowserServiceCallbacks callbacks) {
        }

        public void removeSubscription(final String id, final IBinder token, final IMediaBrowserServiceCallbacks callbacks) {
            MediaBrowserService.this.mHandler.post(new Runnable() {
                public void run() {
                    ConnectionRecord connection = (ConnectionRecord) MediaBrowserService.this.mConnections.get(callbacks.asBinder());
                    String str;
                    StringBuilder stringBuilder;
                    if (connection == null) {
                        str = MediaBrowserService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("removeSubscription for callback that isn't registered id=");
                        stringBuilder.append(id);
                        Log.w(str, stringBuilder.toString());
                        return;
                    }
                    if (!MediaBrowserService.this.removeSubscription(id, connection, token)) {
                        str = MediaBrowserService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("removeSubscription called for ");
                        stringBuilder.append(id);
                        stringBuilder.append(" which is not subscribed");
                        Log.w(str, stringBuilder.toString());
                    }
                }
            });
        }

        public void getMediaItem(final String mediaId, final ResultReceiver receiver, final IMediaBrowserServiceCallbacks callbacks) {
            MediaBrowserService.this.mHandler.post(new Runnable() {
                public void run() {
                    ConnectionRecord connection = (ConnectionRecord) MediaBrowserService.this.mConnections.get(callbacks.asBinder());
                    if (connection == null) {
                        String str = MediaBrowserService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("getMediaItem for callback that isn't registered id=");
                        stringBuilder.append(mediaId);
                        Log.w(str, stringBuilder.toString());
                        return;
                    }
                    MediaBrowserService.this.performLoadItem(mediaId, connection, receiver);
                }
            });
        }
    }

    public abstract BrowserRoot onGetRoot(String str, int i, Bundle bundle);

    public abstract void onLoadChildren(String str, Result<List<MediaItem>> result);

    public void onCreate() {
        super.onCreate();
        this.mBinder = new ServiceBinder(this, null);
    }

    public IBinder onBind(Intent intent) {
        if (SERVICE_INTERFACE.equals(intent.getAction())) {
            return this.mBinder;
        }
        return null;
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
    }

    public void onLoadChildren(String parentId, Result<List<MediaItem>> result, Bundle options) {
        result.setFlags(1);
        onLoadChildren(parentId, result);
    }

    public void onLoadItem(String itemId, Result<MediaItem> result) {
        result.setFlags(2);
        result.sendResult(null);
    }

    public void setSessionToken(final Token token) {
        if (token == null) {
            throw new IllegalArgumentException("Session token may not be null.");
        } else if (this.mSession == null) {
            this.mSession = token;
            this.mHandler.post(new Runnable() {
                public void run() {
                    Iterator<ConnectionRecord> iter = MediaBrowserService.this.mConnections.values().iterator();
                    while (iter.hasNext()) {
                        ConnectionRecord connection = (ConnectionRecord) iter.next();
                        try {
                            connection.callbacks.onConnect(connection.root.getRootId(), token, connection.root.getExtras());
                        } catch (RemoteException e) {
                            String str = MediaBrowserService.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Connection for ");
                            stringBuilder.append(connection.pkg);
                            stringBuilder.append(" is no longer valid.");
                            Log.w(str, stringBuilder.toString());
                            iter.remove();
                        }
                    }
                }
            });
        } else {
            throw new IllegalStateException("The session token has already been set.");
        }
    }

    public Token getSessionToken() {
        return this.mSession;
    }

    public final Bundle getBrowserRootHints() {
        if (this.mCurConnection != null) {
            return this.mCurConnection.rootHints == null ? null : new Bundle(this.mCurConnection.rootHints);
        } else {
            throw new IllegalStateException("This should be called inside of onGetRoot or onLoadChildren or onLoadItem methods");
        }
    }

    public final RemoteUserInfo getCurrentBrowserInfo() {
        if (this.mCurConnection != null) {
            return new RemoteUserInfo(this.mCurConnection.pkg, this.mCurConnection.pid, this.mCurConnection.uid, this.mCurConnection.callbacks.asBinder());
        }
        throw new IllegalStateException("This should be called inside of onGetRoot or onLoadChildren or onLoadItem methods");
    }

    public void notifyChildrenChanged(String parentId) {
        notifyChildrenChangedInternal(parentId, null);
    }

    public void notifyChildrenChanged(String parentId, Bundle options) {
        if (options != null) {
            notifyChildrenChangedInternal(parentId, options);
            return;
        }
        throw new IllegalArgumentException("options cannot be null in notifyChildrenChanged");
    }

    private void notifyChildrenChangedInternal(final String parentId, final Bundle options) {
        if (parentId != null) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    for (IBinder binder : MediaBrowserService.this.mConnections.keySet()) {
                        ConnectionRecord connection = (ConnectionRecord) MediaBrowserService.this.mConnections.get(binder);
                        List<Pair<IBinder, Bundle>> callbackList = (List) connection.subscriptions.get(parentId);
                        if (callbackList != null) {
                            for (Pair<IBinder, Bundle> callback : callbackList) {
                                if (MediaBrowserUtils.hasDuplicatedItems(options, (Bundle) callback.second)) {
                                    MediaBrowserService.this.performLoadChildren(parentId, connection, (Bundle) callback.second);
                                }
                            }
                        }
                    }
                }
            });
            return;
        }
        throw new IllegalArgumentException("parentId cannot be null in notifyChildrenChanged");
    }

    private boolean isValidPackage(String pkg, int uid) {
        if (pkg == null) {
            return false;
        }
        for (String equals : getPackageManager().getPackagesForUid(uid)) {
            if (equals.equals(pkg)) {
                return true;
            }
        }
        return false;
    }

    private void addSubscription(String id, ConnectionRecord connection, IBinder token, Bundle options) {
        List<Pair<IBinder, Bundle>> callbackList = (List) connection.subscriptions.get(id);
        if (callbackList == null) {
            callbackList = new ArrayList();
        }
        for (Pair<IBinder, Bundle> callback : callbackList) {
            if (token == callback.first && MediaBrowserUtils.areSameOptions(options, (Bundle) callback.second)) {
                return;
            }
        }
        callbackList.add(new Pair(token, options));
        connection.subscriptions.put(id, callbackList);
        performLoadChildren(id, connection, options);
    }

    private boolean removeSubscription(String id, ConnectionRecord connection, IBinder token) {
        if (token == null) {
            return connection.subscriptions.remove(id) != null;
        }
        boolean removed = false;
        List<Pair<IBinder, Bundle>> callbackList = (List) connection.subscriptions.get(id);
        if (callbackList != null) {
            Iterator<Pair<IBinder, Bundle>> iter = callbackList.iterator();
            while (iter.hasNext()) {
                if (token == ((Pair) iter.next()).first) {
                    removed = true;
                    iter.remove();
                }
            }
            if (callbackList.size() == 0) {
                connection.subscriptions.remove(id);
            }
        }
        return removed;
    }

    private void performLoadChildren(String parentId, ConnectionRecord connection, Bundle options) {
        final ConnectionRecord connectionRecord = connection;
        final String str = parentId;
        final Bundle bundle = options;
        Result result = new Result<List<MediaItem>>(parentId) {
            void onResultSent(List<MediaItem> list, int flag) {
                if (MediaBrowserService.this.mConnections.get(connectionRecord.callbacks.asBinder()) == connectionRecord) {
                    List<MediaItem> filteredList = (flag & 1) != 0 ? MediaBrowserService.this.applyOptions(list, bundle) : list;
                    try {
                        connectionRecord.callbacks.onLoadChildrenWithOptions(str, filteredList == null ? null : new ParceledListSlice(filteredList), bundle);
                    } catch (RemoteException e) {
                        String str = MediaBrowserService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Calling onLoadChildren() failed for id=");
                        stringBuilder.append(str);
                        stringBuilder.append(" package=");
                        stringBuilder.append(connectionRecord.pkg);
                        Log.w(str, stringBuilder.toString());
                    }
                }
            }
        };
        this.mCurConnection = connection;
        if (options == null) {
            onLoadChildren(parentId, result);
        } else {
            onLoadChildren(parentId, result, options);
        }
        this.mCurConnection = null;
        if (!result.isDone()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onLoadChildren must call detach() or sendResult() before returning for package=");
            stringBuilder.append(connection.pkg);
            stringBuilder.append(" id=");
            stringBuilder.append(parentId);
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    private List<MediaItem> applyOptions(List<MediaItem> list, Bundle options) {
        if (list == null) {
            return null;
        }
        int page = options.getInt("android.media.browse.extra.PAGE", -1);
        int pageSize = options.getInt("android.media.browse.extra.PAGE_SIZE", -1);
        if (page == -1 && pageSize == -1) {
            return list;
        }
        int fromIndex = pageSize * page;
        int toIndex = fromIndex + pageSize;
        if (page < 0 || pageSize < 1 || fromIndex >= list.size()) {
            return Collections.EMPTY_LIST;
        }
        if (toIndex > list.size()) {
            toIndex = list.size();
        }
        return list.subList(fromIndex, toIndex);
    }

    private void performLoadItem(String itemId, ConnectionRecord connection, ResultReceiver receiver) {
        final ConnectionRecord connectionRecord = connection;
        final String str = itemId;
        final ResultReceiver resultReceiver = receiver;
        Result result = new Result<MediaItem>(itemId) {
            void onResultSent(MediaItem item, int flag) {
                if (MediaBrowserService.this.mConnections.get(connectionRecord.callbacks.asBinder()) == connectionRecord) {
                    if ((flag & 2) != 0) {
                        resultReceiver.send(-1, null);
                        return;
                    }
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(MediaBrowserService.KEY_MEDIA_ITEM, item);
                    resultReceiver.send(0, bundle);
                }
            }
        };
        this.mCurConnection = connection;
        onLoadItem(itemId, result);
        this.mCurConnection = null;
        if (!result.isDone()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onLoadItem must call detach() or sendResult() before returning for id=");
            stringBuilder.append(itemId);
            throw new IllegalStateException(stringBuilder.toString());
        }
    }
}
