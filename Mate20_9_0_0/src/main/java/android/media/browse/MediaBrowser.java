package android.media.browse;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ParceledListSlice;
import android.media.MediaDescription;
import android.media.session.MediaSession.Token;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.service.media.IMediaBrowserService;
import android.service.media.IMediaBrowserService.Stub;
import android.service.media.IMediaBrowserServiceCallbacks;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public final class MediaBrowser {
    private static final int CONNECT_STATE_CONNECTED = 3;
    private static final int CONNECT_STATE_CONNECTING = 2;
    private static final int CONNECT_STATE_DISCONNECTED = 1;
    private static final int CONNECT_STATE_DISCONNECTING = 0;
    private static final int CONNECT_STATE_SUSPENDED = 4;
    private static final boolean DBG = false;
    public static final String EXTRA_PAGE = "android.media.browse.extra.PAGE";
    public static final String EXTRA_PAGE_SIZE = "android.media.browse.extra.PAGE_SIZE";
    private static final String TAG = "MediaBrowser";
    private final ConnectionCallback mCallback;
    private final Context mContext;
    private volatile Bundle mExtras;
    private final Handler mHandler = new Handler();
    private volatile Token mMediaSessionToken;
    private final Bundle mRootHints;
    private volatile String mRootId;
    private IMediaBrowserService mServiceBinder;
    private IMediaBrowserServiceCallbacks mServiceCallbacks;
    private final ComponentName mServiceComponent;
    private MediaServiceConnection mServiceConnection;
    private volatile int mState = 1;
    private final ArrayMap<String, Subscription> mSubscriptions = new ArrayMap();

    public static class ConnectionCallback {
        public void onConnected() {
        }

        public void onConnectionSuspended() {
        }

        public void onConnectionFailed() {
        }
    }

    public static abstract class ItemCallback {
        public void onItemLoaded(MediaItem item) {
        }

        public void onError(String mediaId) {
        }
    }

    private static class Subscription {
        private final List<SubscriptionCallback> mCallbacks = new ArrayList();
        private final List<Bundle> mOptionsList = new ArrayList();

        public boolean isEmpty() {
            return this.mCallbacks.isEmpty();
        }

        public List<Bundle> getOptionsList() {
            return this.mOptionsList;
        }

        public List<SubscriptionCallback> getCallbacks() {
            return this.mCallbacks;
        }

        public SubscriptionCallback getCallback(Context context, Bundle options) {
            if (options != null) {
                options.setClassLoader(context.getClassLoader());
            }
            for (int i = 0; i < this.mOptionsList.size(); i++) {
                if (MediaBrowserUtils.areSameOptions((Bundle) this.mOptionsList.get(i), options)) {
                    return (SubscriptionCallback) this.mCallbacks.get(i);
                }
            }
            return null;
        }

        public void putCallback(Context context, Bundle options, SubscriptionCallback callback) {
            if (options != null) {
                options.setClassLoader(context.getClassLoader());
            }
            for (int i = 0; i < this.mOptionsList.size(); i++) {
                if (MediaBrowserUtils.areSameOptions((Bundle) this.mOptionsList.get(i), options)) {
                    this.mCallbacks.set(i, callback);
                    return;
                }
            }
            this.mCallbacks.add(callback);
            this.mOptionsList.add(options);
        }
    }

    public static abstract class SubscriptionCallback {
        Binder mToken = new Binder();

        public void onChildrenLoaded(String parentId, List<MediaItem> list) {
        }

        public void onChildrenLoaded(String parentId, List<MediaItem> list, Bundle options) {
        }

        public void onError(String parentId) {
        }

        public void onError(String parentId, Bundle options) {
        }
    }

    public static class MediaItem implements Parcelable {
        public static final Creator<MediaItem> CREATOR = new Creator<MediaItem>() {
            public MediaItem createFromParcel(Parcel in) {
                return new MediaItem(in, null);
            }

            public MediaItem[] newArray(int size) {
                return new MediaItem[size];
            }
        };
        public static final int FLAG_BROWSABLE = 1;
        public static final int FLAG_PLAYABLE = 2;
        private final MediaDescription mDescription;
        private final int mFlags;

        @Retention(RetentionPolicy.SOURCE)
        public @interface Flags {
        }

        public MediaItem(MediaDescription description, int flags) {
            if (description == null) {
                throw new IllegalArgumentException("description cannot be null");
            } else if (TextUtils.isEmpty(description.getMediaId())) {
                throw new IllegalArgumentException("description must have a non-empty media id");
            } else {
                this.mFlags = flags;
                this.mDescription = description;
            }
        }

        private MediaItem(Parcel in) {
            this.mFlags = in.readInt();
            this.mDescription = (MediaDescription) MediaDescription.CREATOR.createFromParcel(in);
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(this.mFlags);
            this.mDescription.writeToParcel(out, flags);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("MediaItem{");
            sb.append("mFlags=");
            sb.append(this.mFlags);
            sb.append(", mDescription=");
            sb.append(this.mDescription);
            sb.append('}');
            return sb.toString();
        }

        public int getFlags() {
            return this.mFlags;
        }

        public boolean isBrowsable() {
            return (this.mFlags & 1) != 0;
        }

        public boolean isPlayable() {
            return (this.mFlags & 2) != 0;
        }

        public MediaDescription getDescription() {
            return this.mDescription;
        }

        public String getMediaId() {
            return this.mDescription.getMediaId();
        }
    }

    private class MediaServiceConnection implements ServiceConnection {
        private MediaServiceConnection() {
        }

        /* synthetic */ MediaServiceConnection(MediaBrowser x0, AnonymousClass1 x1) {
            this();
        }

        public void onServiceConnected(final ComponentName name, final IBinder binder) {
            postOrRun(new Runnable() {
                public void run() {
                    if (MediaServiceConnection.this.isCurrent("onServiceConnected")) {
                        MediaBrowser.this.mServiceBinder = Stub.asInterface(binder);
                        MediaBrowser.this.mServiceCallbacks = MediaBrowser.this.getNewServiceCallbacks();
                        MediaBrowser.this.mState = 2;
                        try {
                            MediaBrowser.this.mServiceBinder.connect(MediaBrowser.this.mContext.getPackageName(), MediaBrowser.this.mRootHints, MediaBrowser.this.mServiceCallbacks);
                        } catch (RemoteException e) {
                            String str = MediaBrowser.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("RemoteException during connect for ");
                            stringBuilder.append(MediaBrowser.this.mServiceComponent);
                            Log.w(str, stringBuilder.toString());
                        }
                    }
                }
            });
        }

        public void onServiceDisconnected(final ComponentName name) {
            postOrRun(new Runnable() {
                public void run() {
                    if (MediaServiceConnection.this.isCurrent("onServiceDisconnected")) {
                        MediaBrowser.this.mServiceBinder = null;
                        MediaBrowser.this.mServiceCallbacks = null;
                        MediaBrowser.this.mState = 4;
                        MediaBrowser.this.mCallback.onConnectionSuspended();
                    }
                }
            });
        }

        private void postOrRun(Runnable r) {
            if (Thread.currentThread() == MediaBrowser.this.mHandler.getLooper().getThread()) {
                r.run();
            } else {
                MediaBrowser.this.mHandler.post(r);
            }
        }

        private boolean isCurrent(String funcName) {
            if (MediaBrowser.this.mServiceConnection == this && MediaBrowser.this.mState != 0 && MediaBrowser.this.mState != 1) {
                return true;
            }
            if (!(MediaBrowser.this.mState == 0 || MediaBrowser.this.mState == 1)) {
                String str = MediaBrowser.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(funcName);
                stringBuilder.append(" for ");
                stringBuilder.append(MediaBrowser.this.mServiceComponent);
                stringBuilder.append(" with mServiceConnection=");
                stringBuilder.append(MediaBrowser.this.mServiceConnection);
                stringBuilder.append(" this=");
                stringBuilder.append(this);
                Log.i(str, stringBuilder.toString());
            }
            return false;
        }
    }

    private static class ServiceCallbacks extends IMediaBrowserServiceCallbacks.Stub {
        private WeakReference<MediaBrowser> mMediaBrowser;

        public ServiceCallbacks(MediaBrowser mediaBrowser) {
            this.mMediaBrowser = new WeakReference(mediaBrowser);
        }

        public void onConnect(String root, Token session, Bundle extras) {
            MediaBrowser mediaBrowser = (MediaBrowser) this.mMediaBrowser.get();
            if (mediaBrowser != null) {
                mediaBrowser.onServiceConnected(this, root, session, extras);
            }
        }

        public void onConnectFailed() {
            MediaBrowser mediaBrowser = (MediaBrowser) this.mMediaBrowser.get();
            if (mediaBrowser != null) {
                mediaBrowser.onConnectionFailed(this);
            }
        }

        public void onLoadChildren(String parentId, ParceledListSlice list) {
            onLoadChildrenWithOptions(parentId, list, null);
        }

        public void onLoadChildrenWithOptions(String parentId, ParceledListSlice list, Bundle options) {
            MediaBrowser mediaBrowser = (MediaBrowser) this.mMediaBrowser.get();
            if (mediaBrowser != null) {
                mediaBrowser.onLoadChildren(this, parentId, list, options);
            }
        }
    }

    public MediaBrowser(Context context, ComponentName serviceComponent, ConnectionCallback callback, Bundle rootHints) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        } else if (serviceComponent == null) {
            throw new IllegalArgumentException("service component must not be null");
        } else if (callback != null) {
            this.mContext = context;
            this.mServiceComponent = serviceComponent;
            this.mCallback = callback;
            this.mRootHints = rootHints == null ? null : new Bundle(rootHints);
        } else {
            throw new IllegalArgumentException("connection callback must not be null");
        }
    }

    public void connect() {
        if (this.mState == 0 || this.mState == 1) {
            this.mState = 2;
            this.mHandler.post(new Runnable() {
                public void run() {
                    if (MediaBrowser.this.mState != 0) {
                        MediaBrowser.this.mState = 2;
                        StringBuilder stringBuilder;
                        if (MediaBrowser.this.mServiceBinder != null) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("mServiceBinder should be null. Instead it is ");
                            stringBuilder.append(MediaBrowser.this.mServiceBinder);
                            throw new RuntimeException(stringBuilder.toString());
                        } else if (MediaBrowser.this.mServiceCallbacks == null) {
                            Intent intent = new Intent("android.media.browse.MediaBrowserService");
                            intent.setComponent(MediaBrowser.this.mServiceComponent);
                            MediaBrowser.this.mServiceConnection = new MediaServiceConnection(MediaBrowser.this, null);
                            boolean bound = false;
                            try {
                                bound = MediaBrowser.this.mContext.bindService(intent, MediaBrowser.this.mServiceConnection, 1);
                            } catch (Exception e) {
                                String str = MediaBrowser.TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Failed binding to service ");
                                stringBuilder2.append(MediaBrowser.this.mServiceComponent);
                                Log.e(str, stringBuilder2.toString());
                            }
                            if (!bound) {
                                MediaBrowser.this.forceCloseConnection();
                                MediaBrowser.this.mCallback.onConnectionFailed();
                            }
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("mServiceCallbacks should be null. Instead it is ");
                            stringBuilder.append(MediaBrowser.this.mServiceCallbacks);
                            throw new RuntimeException(stringBuilder.toString());
                        }
                    }
                }
            });
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("connect() called while neither disconnecting nor disconnected (state=");
        stringBuilder.append(getStateLabel(this.mState));
        stringBuilder.append(")");
        throw new IllegalStateException(stringBuilder.toString());
    }

    public void disconnect() {
        this.mState = 0;
        this.mHandler.post(new Runnable() {
            public void run() {
                if (MediaBrowser.this.mServiceCallbacks != null) {
                    try {
                        MediaBrowser.this.mServiceBinder.disconnect(MediaBrowser.this.mServiceCallbacks);
                    } catch (RemoteException e) {
                        String str = MediaBrowser.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("RemoteException during connect for ");
                        stringBuilder.append(MediaBrowser.this.mServiceComponent);
                        Log.w(str, stringBuilder.toString());
                    }
                }
                int state = MediaBrowser.this.mState;
                MediaBrowser.this.forceCloseConnection();
                if (state != 0) {
                    MediaBrowser.this.mState = state;
                }
            }
        });
    }

    private void forceCloseConnection() {
        if (this.mServiceConnection != null) {
            try {
                this.mContext.unbindService(this.mServiceConnection);
            } catch (IllegalArgumentException e) {
            }
        }
        this.mState = 1;
        this.mServiceConnection = null;
        this.mServiceBinder = null;
        this.mServiceCallbacks = null;
        this.mRootId = null;
        this.mMediaSessionToken = null;
    }

    public boolean isConnected() {
        return this.mState == 3;
    }

    public ComponentName getServiceComponent() {
        if (isConnected()) {
            return this.mServiceComponent;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getServiceComponent() called while not connected (state=");
        stringBuilder.append(this.mState);
        stringBuilder.append(")");
        throw new IllegalStateException(stringBuilder.toString());
    }

    public String getRoot() {
        if (isConnected()) {
            return this.mRootId;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getRoot() called while not connected (state=");
        stringBuilder.append(getStateLabel(this.mState));
        stringBuilder.append(")");
        throw new IllegalStateException(stringBuilder.toString());
    }

    public Bundle getExtras() {
        if (isConnected()) {
            return this.mExtras;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getExtras() called while not connected (state=");
        stringBuilder.append(getStateLabel(this.mState));
        stringBuilder.append(")");
        throw new IllegalStateException(stringBuilder.toString());
    }

    public Token getSessionToken() {
        if (isConnected()) {
            return this.mMediaSessionToken;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getSessionToken() called while not connected (state=");
        stringBuilder.append(this.mState);
        stringBuilder.append(")");
        throw new IllegalStateException(stringBuilder.toString());
    }

    public void subscribe(String parentId, SubscriptionCallback callback) {
        subscribeInternal(parentId, null, callback);
    }

    public void subscribe(String parentId, Bundle options, SubscriptionCallback callback) {
        if (options != null) {
            subscribeInternal(parentId, new Bundle(options), callback);
            return;
        }
        throw new IllegalArgumentException("options cannot be null");
    }

    public void unsubscribe(String parentId) {
        unsubscribeInternal(parentId, null);
    }

    public void unsubscribe(String parentId, SubscriptionCallback callback) {
        if (callback != null) {
            unsubscribeInternal(parentId, callback);
            return;
        }
        throw new IllegalArgumentException("callback cannot be null");
    }

    public void getItem(final String mediaId, final ItemCallback cb) {
        if (TextUtils.isEmpty(mediaId)) {
            throw new IllegalArgumentException("mediaId cannot be empty.");
        } else if (cb == null) {
            throw new IllegalArgumentException("cb cannot be null.");
        } else if (this.mState != 3) {
            Log.i(TAG, "Not connected, unable to retrieve the MediaItem.");
            this.mHandler.post(new Runnable() {
                public void run() {
                    cb.onError(mediaId);
                }
            });
        } else {
            try {
                this.mServiceBinder.getMediaItem(mediaId, new ResultReceiver(this.mHandler) {
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        if (!MediaBrowser.this.isConnected()) {
                            return;
                        }
                        if (resultCode == 0 && resultData != null && resultData.containsKey("media_item")) {
                            Parcelable item = resultData.getParcelable("media_item");
                            if (item == null || (item instanceof MediaItem)) {
                                cb.onItemLoaded((MediaItem) item);
                                return;
                            } else {
                                cb.onError(mediaId);
                                return;
                            }
                        }
                        cb.onError(mediaId);
                    }
                }, this.mServiceCallbacks);
            } catch (RemoteException e) {
                Log.i(TAG, "Remote error getting media item.");
                this.mHandler.post(new Runnable() {
                    public void run() {
                        cb.onError(mediaId);
                    }
                });
            }
        }
    }

    private void subscribeInternal(String parentId, Bundle options, SubscriptionCallback callback) {
        if (TextUtils.isEmpty(parentId)) {
            throw new IllegalArgumentException("parentId cannot be empty.");
        } else if (callback != null) {
            Subscription sub = (Subscription) this.mSubscriptions.get(parentId);
            if (sub == null) {
                sub = new Subscription();
                this.mSubscriptions.put(parentId, sub);
            }
            sub.putCallback(this.mContext, options, callback);
            if (isConnected()) {
                if (options == null) {
                    try {
                        this.mServiceBinder.addSubscriptionDeprecated(parentId, this.mServiceCallbacks);
                    } catch (RemoteException e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("addSubscription failed with RemoteException parentId=");
                        stringBuilder.append(parentId);
                        Log.d(str, stringBuilder.toString());
                        return;
                    }
                }
                this.mServiceBinder.addSubscription(parentId, callback.mToken, options, this.mServiceCallbacks);
            }
        } else {
            throw new IllegalArgumentException("callback cannot be null");
        }
    }

    private void unsubscribeInternal(String parentId, SubscriptionCallback callback) {
        if (TextUtils.isEmpty(parentId)) {
            throw new IllegalArgumentException("parentId cannot be empty.");
        }
        Subscription sub = (Subscription) this.mSubscriptions.get(parentId);
        if (sub != null) {
            if (callback == null) {
                try {
                    if (isConnected()) {
                        this.mServiceBinder.removeSubscriptionDeprecated(parentId, this.mServiceCallbacks);
                        this.mServiceBinder.removeSubscription(parentId, null, this.mServiceCallbacks);
                    }
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("removeSubscription failed with RemoteException parentId=");
                    stringBuilder.append(parentId);
                    Log.d(str, stringBuilder.toString());
                }
            } else {
                RemoteException ex = sub.getCallbacks();
                List<Bundle> optionsList = sub.getOptionsList();
                for (int i = ex.size() - 1; i >= 0; i--) {
                    if (ex.get(i) == callback) {
                        if (isConnected()) {
                            this.mServiceBinder.removeSubscription(parentId, callback.mToken, this.mServiceCallbacks);
                        }
                        ex.remove(i);
                        optionsList.remove(i);
                    }
                }
            }
            if (sub.isEmpty() || callback == null) {
                this.mSubscriptions.remove(parentId);
            }
        }
    }

    private static String getStateLabel(int state) {
        switch (state) {
            case 0:
                return "CONNECT_STATE_DISCONNECTING";
            case 1:
                return "CONNECT_STATE_DISCONNECTED";
            case 2:
                return "CONNECT_STATE_CONNECTING";
            case 3:
                return "CONNECT_STATE_CONNECTED";
            case 4:
                return "CONNECT_STATE_SUSPENDED";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UNKNOWN/");
                stringBuilder.append(state);
                return stringBuilder.toString();
        }
    }

    private final void onServiceConnected(IMediaBrowserServiceCallbacks callback, String root, Token session, Bundle extra) {
        final IMediaBrowserServiceCallbacks iMediaBrowserServiceCallbacks = callback;
        final String str = root;
        final Token token = session;
        final Bundle bundle = extra;
        this.mHandler.post(new Runnable() {
            public void run() {
                if (!MediaBrowser.this.isCurrent(iMediaBrowserServiceCallbacks, "onConnect")) {
                    return;
                }
                if (MediaBrowser.this.mState != 2) {
                    String str = MediaBrowser.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onConnect from service while mState=");
                    stringBuilder.append(MediaBrowser.getStateLabel(MediaBrowser.this.mState));
                    stringBuilder.append("... ignoring");
                    Log.w(str, stringBuilder.toString());
                    return;
                }
                MediaBrowser.this.mRootId = str;
                MediaBrowser.this.mMediaSessionToken = token;
                MediaBrowser.this.mExtras = bundle;
                MediaBrowser.this.mState = 3;
                MediaBrowser.this.mCallback.onConnected();
                for (Entry<String, Subscription> subscriptionEntry : MediaBrowser.this.mSubscriptions.entrySet()) {
                    String id = (String) subscriptionEntry.getKey();
                    Subscription sub = (Subscription) subscriptionEntry.getValue();
                    List<SubscriptionCallback> callbackList = sub.getCallbacks();
                    List<Bundle> optionsList = sub.getOptionsList();
                    for (int i = 0; i < callbackList.size(); i++) {
                        try {
                            MediaBrowser.this.mServiceBinder.addSubscription(id, ((SubscriptionCallback) callbackList.get(i)).mToken, (Bundle) optionsList.get(i), MediaBrowser.this.mServiceCallbacks);
                        } catch (RemoteException e) {
                            String str2 = MediaBrowser.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("addSubscription failed with RemoteException parentId=");
                            stringBuilder2.append(id);
                            Log.d(str2, stringBuilder2.toString());
                        }
                    }
                }
            }
        });
    }

    private final void onConnectionFailed(final IMediaBrowserServiceCallbacks callback) {
        this.mHandler.post(new Runnable() {
            public void run() {
                String str = MediaBrowser.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onConnectFailed for ");
                stringBuilder.append(MediaBrowser.this.mServiceComponent);
                Log.e(str, stringBuilder.toString());
                if (!MediaBrowser.this.isCurrent(callback, "onConnectFailed")) {
                    return;
                }
                if (MediaBrowser.this.mState != 2) {
                    str = MediaBrowser.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("onConnect from service while mState=");
                    stringBuilder.append(MediaBrowser.getStateLabel(MediaBrowser.this.mState));
                    stringBuilder.append("... ignoring");
                    Log.w(str, stringBuilder.toString());
                    return;
                }
                MediaBrowser.this.forceCloseConnection();
                MediaBrowser.this.mCallback.onConnectionFailed();
            }
        });
    }

    private final void onLoadChildren(IMediaBrowserServiceCallbacks callback, String parentId, ParceledListSlice list, Bundle options) {
        final IMediaBrowserServiceCallbacks iMediaBrowserServiceCallbacks = callback;
        final String str = parentId;
        final Bundle bundle = options;
        final ParceledListSlice parceledListSlice = list;
        this.mHandler.post(new Runnable() {
            public void run() {
                if (MediaBrowser.this.isCurrent(iMediaBrowserServiceCallbacks, "onLoadChildren")) {
                    Subscription subscription = (Subscription) MediaBrowser.this.mSubscriptions.get(str);
                    if (subscription != null) {
                        SubscriptionCallback subscriptionCallback = subscription.getCallback(MediaBrowser.this.mContext, bundle);
                        if (subscriptionCallback != null) {
                            List<MediaItem> data = parceledListSlice == null ? null : parceledListSlice.getList();
                            if (bundle == null) {
                                if (data == null) {
                                    subscriptionCallback.onError(str);
                                } else {
                                    subscriptionCallback.onChildrenLoaded(str, data);
                                }
                            } else if (data == null) {
                                subscriptionCallback.onError(str, bundle);
                            } else {
                                subscriptionCallback.onChildrenLoaded(str, data, bundle);
                            }
                        }
                    }
                }
            }
        });
    }

    private boolean isCurrent(IMediaBrowserServiceCallbacks callback, String funcName) {
        if (this.mServiceCallbacks == callback && this.mState != 0 && this.mState != 1) {
            return true;
        }
        if (!(this.mState == 0 || this.mState == 1)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(funcName);
            stringBuilder.append(" for ");
            stringBuilder.append(this.mServiceComponent);
            stringBuilder.append(" with mServiceConnection=");
            stringBuilder.append(this.mServiceCallbacks);
            stringBuilder.append(" this=");
            stringBuilder.append(this);
            Log.i(str, stringBuilder.toString());
        }
        return false;
    }

    private ServiceCallbacks getNewServiceCallbacks() {
        return new ServiceCallbacks(this);
    }

    void dump() {
        Log.d(TAG, "MediaBrowser...");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  mServiceComponent=");
        stringBuilder.append(this.mServiceComponent);
        Log.d(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mCallback=");
        stringBuilder.append(this.mCallback);
        Log.d(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mRootHints=");
        stringBuilder.append(this.mRootHints);
        Log.d(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mState=");
        stringBuilder.append(getStateLabel(this.mState));
        Log.d(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mServiceConnection=");
        stringBuilder.append(this.mServiceConnection);
        Log.d(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mServiceBinder=");
        stringBuilder.append(this.mServiceBinder);
        Log.d(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mServiceCallbacks=");
        stringBuilder.append(this.mServiceCallbacks);
        Log.d(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mRootId=");
        stringBuilder.append(this.mRootId);
        Log.d(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mMediaSessionToken=");
        stringBuilder.append(this.mMediaSessionToken);
        Log.d(str, stringBuilder.toString());
    }
}
