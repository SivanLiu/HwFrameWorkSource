package android.app;

import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.content.Loader.OnLoadCanceledListener;
import android.content.Loader.OnLoadCompleteListener;
import android.os.Bundle;
import android.util.DebugUtils;
import android.util.Log;
import android.util.SparseArray;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;

/* compiled from: LoaderManager */
class LoaderManagerImpl extends LoaderManager {
    static boolean DEBUG = false;
    static final String TAG = "LoaderManager";
    boolean mCreatingLoader;
    private FragmentHostCallback mHost;
    final SparseArray<LoaderInfo> mInactiveLoaders = new SparseArray(0);
    final SparseArray<LoaderInfo> mLoaders = new SparseArray(0);
    boolean mRetaining;
    boolean mRetainingStarted;
    boolean mStarted;
    final String mWho;

    /* compiled from: LoaderManager */
    final class LoaderInfo implements OnLoadCompleteListener<Object>, OnLoadCanceledListener<Object> {
        final Bundle mArgs;
        LoaderCallbacks<Object> mCallbacks;
        Object mData;
        boolean mDeliveredData;
        boolean mDestroyed;
        boolean mHaveData;
        final int mId;
        boolean mListenerRegistered;
        Loader<Object> mLoader;
        LoaderInfo mPendingLoader;
        boolean mReportNextStart;
        boolean mRetaining;
        boolean mRetainingStarted;
        boolean mStarted;

        public LoaderInfo(int id, Bundle args, LoaderCallbacks<Object> callbacks) {
            this.mId = id;
            this.mArgs = args;
            this.mCallbacks = callbacks;
        }

        void start() {
            if (this.mRetaining && this.mRetainingStarted) {
                this.mStarted = true;
            } else if (!this.mStarted) {
                this.mStarted = true;
                if (LoaderManagerImpl.DEBUG) {
                    String str = LoaderManagerImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("  Starting: ");
                    stringBuilder.append(this);
                    Log.v(str, stringBuilder.toString());
                }
                if (this.mLoader == null && this.mCallbacks != null) {
                    this.mLoader = this.mCallbacks.onCreateLoader(this.mId, this.mArgs);
                }
                if (this.mLoader != null) {
                    if (!this.mLoader.getClass().isMemberClass() || Modifier.isStatic(this.mLoader.getClass().getModifiers())) {
                        if (!this.mListenerRegistered) {
                            this.mLoader.registerListener(this.mId, this);
                            this.mLoader.registerOnLoadCanceledListener(this);
                            this.mListenerRegistered = true;
                        }
                        this.mLoader.startLoading();
                    } else {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Object returned from onCreateLoader must not be a non-static inner member class: ");
                        stringBuilder2.append(this.mLoader);
                        throw new IllegalArgumentException(stringBuilder2.toString());
                    }
                }
            }
        }

        void retain() {
            if (LoaderManagerImpl.DEBUG) {
                String str = LoaderManagerImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("  Retaining: ");
                stringBuilder.append(this);
                Log.v(str, stringBuilder.toString());
            }
            this.mRetaining = true;
            this.mRetainingStarted = this.mStarted;
            this.mStarted = false;
            this.mCallbacks = null;
        }

        void finishRetain() {
            if (this.mRetaining) {
                if (LoaderManagerImpl.DEBUG) {
                    String str = LoaderManagerImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("  Finished Retaining: ");
                    stringBuilder.append(this);
                    Log.v(str, stringBuilder.toString());
                }
                this.mRetaining = false;
                if (!(this.mStarted == this.mRetainingStarted || this.mStarted)) {
                    stop();
                }
            }
            if (this.mStarted && this.mHaveData && !this.mReportNextStart) {
                callOnLoadFinished(this.mLoader, this.mData);
            }
        }

        void reportStart() {
            if (this.mStarted && this.mReportNextStart) {
                this.mReportNextStart = false;
                if (this.mHaveData && !this.mRetaining) {
                    callOnLoadFinished(this.mLoader, this.mData);
                }
            }
        }

        void stop() {
            if (LoaderManagerImpl.DEBUG) {
                String str = LoaderManagerImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("  Stopping: ");
                stringBuilder.append(this);
                Log.v(str, stringBuilder.toString());
            }
            this.mStarted = false;
            if (!this.mRetaining && this.mLoader != null && this.mListenerRegistered) {
                this.mListenerRegistered = false;
                this.mLoader.unregisterListener(this);
                this.mLoader.unregisterOnLoadCanceledListener(this);
                this.mLoader.stopLoading();
            }
        }

        boolean cancel() {
            if (LoaderManagerImpl.DEBUG) {
                String str = LoaderManagerImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("  Canceling: ");
                stringBuilder.append(this);
                Log.v(str, stringBuilder.toString());
            }
            if (!this.mStarted || this.mLoader == null || !this.mListenerRegistered) {
                return false;
            }
            boolean cancelLoadResult = this.mLoader.cancelLoad();
            if (!cancelLoadResult) {
                onLoadCanceled(this.mLoader);
            }
            return cancelLoadResult;
        }

        void destroy() {
            if (LoaderManagerImpl.DEBUG) {
                String str = LoaderManagerImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("  Destroying: ");
                stringBuilder.append(this);
                Log.v(str, stringBuilder.toString());
            }
            this.mDestroyed = true;
            boolean needReset = this.mDeliveredData;
            this.mDeliveredData = false;
            if (this.mCallbacks != null && this.mLoader != null && this.mHaveData && needReset) {
                String str2;
                if (LoaderManagerImpl.DEBUG) {
                    str2 = LoaderManagerImpl.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  Reseting: ");
                    stringBuilder2.append(this);
                    Log.v(str2, stringBuilder2.toString());
                }
                str2 = null;
                if (LoaderManagerImpl.this.mHost != null) {
                    str2 = LoaderManagerImpl.this.mHost.mFragmentManager.mNoTransactionsBecause;
                    LoaderManagerImpl.this.mHost.mFragmentManager.mNoTransactionsBecause = "onLoaderReset";
                }
                try {
                    this.mCallbacks.onLoaderReset(this.mLoader);
                } finally {
                    if (LoaderManagerImpl.this.mHost != null) {
                        LoaderManagerImpl.this.mHost.mFragmentManager.mNoTransactionsBecause = str2;
                    }
                }
            }
            this.mCallbacks = null;
            this.mData = null;
            this.mHaveData = false;
            if (this.mLoader != null) {
                if (this.mListenerRegistered) {
                    this.mListenerRegistered = false;
                    this.mLoader.unregisterListener(this);
                    this.mLoader.unregisterOnLoadCanceledListener(this);
                }
                this.mLoader.reset();
            }
            if (this.mPendingLoader != null) {
                this.mPendingLoader.destroy();
            }
        }

        public void onLoadCanceled(Loader<Object> loader) {
            if (LoaderManagerImpl.DEBUG) {
                String str = LoaderManagerImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onLoadCanceled: ");
                stringBuilder.append(this);
                Log.v(str, stringBuilder.toString());
            }
            if (this.mDestroyed) {
                if (LoaderManagerImpl.DEBUG) {
                    Log.v(LoaderManagerImpl.TAG, "  Ignoring load canceled -- destroyed");
                }
            } else if (LoaderManagerImpl.this.mLoaders.get(this.mId) != this) {
                if (LoaderManagerImpl.DEBUG) {
                    Log.v(LoaderManagerImpl.TAG, "  Ignoring load canceled -- not active");
                }
            } else {
                LoaderInfo pending = this.mPendingLoader;
                if (pending != null) {
                    if (LoaderManagerImpl.DEBUG) {
                        String str2 = LoaderManagerImpl.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("  Switching to pending loader: ");
                        stringBuilder2.append(pending);
                        Log.v(str2, stringBuilder2.toString());
                    }
                    this.mPendingLoader = null;
                    LoaderManagerImpl.this.mLoaders.put(this.mId, null);
                    destroy();
                    LoaderManagerImpl.this.installLoader(pending);
                }
            }
        }

        public void onLoadComplete(Loader<Object> loader, Object data) {
            if (LoaderManagerImpl.DEBUG) {
                String str = LoaderManagerImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onLoadComplete: ");
                stringBuilder.append(this);
                Log.v(str, stringBuilder.toString());
            }
            if (this.mDestroyed) {
                if (LoaderManagerImpl.DEBUG) {
                    Log.v(LoaderManagerImpl.TAG, "  Ignoring load complete -- destroyed");
                }
            } else if (LoaderManagerImpl.this.mLoaders.get(this.mId) != this) {
                if (LoaderManagerImpl.DEBUG) {
                    Log.v(LoaderManagerImpl.TAG, "  Ignoring load complete -- not active");
                }
            } else {
                LoaderInfo pending = this.mPendingLoader;
                if (pending != null) {
                    if (LoaderManagerImpl.DEBUG) {
                        String str2 = LoaderManagerImpl.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("  Switching to pending loader: ");
                        stringBuilder2.append(pending);
                        Log.v(str2, stringBuilder2.toString());
                    }
                    this.mPendingLoader = null;
                    LoaderManagerImpl.this.mLoaders.put(this.mId, null);
                    destroy();
                    LoaderManagerImpl.this.installLoader(pending);
                    return;
                }
                if (!(this.mData == data && this.mHaveData)) {
                    this.mData = data;
                    this.mHaveData = true;
                    if (this.mStarted) {
                        callOnLoadFinished(loader, data);
                    }
                }
                LoaderInfo info = (LoaderInfo) LoaderManagerImpl.this.mInactiveLoaders.get(this.mId);
                if (!(info == null || info == this)) {
                    info.mDeliveredData = false;
                    info.destroy();
                    LoaderManagerImpl.this.mInactiveLoaders.remove(this.mId);
                }
                if (!(LoaderManagerImpl.this.mHost == null || LoaderManagerImpl.this.hasRunningLoaders())) {
                    LoaderManagerImpl.this.mHost.mFragmentManager.startPendingDeferredFragments();
                }
            }
        }

        void callOnLoadFinished(Loader<Object> loader, Object data) {
            if (this.mCallbacks != null) {
                String lastBecause = null;
                if (LoaderManagerImpl.this.mHost != null) {
                    lastBecause = LoaderManagerImpl.this.mHost.mFragmentManager.mNoTransactionsBecause;
                    LoaderManagerImpl.this.mHost.mFragmentManager.mNoTransactionsBecause = "onLoadFinished";
                }
                try {
                    if (LoaderManagerImpl.DEBUG) {
                        String str = LoaderManagerImpl.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("  onLoadFinished in ");
                        stringBuilder.append(loader);
                        stringBuilder.append(": ");
                        stringBuilder.append(loader.dataToString(data));
                        Log.v(str, stringBuilder.toString());
                    }
                    this.mCallbacks.onLoadFinished(loader, data);
                    this.mDeliveredData = true;
                } finally {
                    if (LoaderManagerImpl.this.mHost != null) {
                        LoaderManagerImpl.this.mHost.mFragmentManager.mNoTransactionsBecause = lastBecause;
                    }
                }
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(64);
            sb.append("LoaderInfo{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(" #");
            sb.append(this.mId);
            sb.append(" : ");
            DebugUtils.buildShortClassTag(this.mLoader, sb);
            sb.append("}}");
            return sb.toString();
        }

        public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            writer.print(prefix);
            writer.print("mId=");
            writer.print(this.mId);
            writer.print(" mArgs=");
            writer.println(this.mArgs);
            writer.print(prefix);
            writer.print("mCallbacks=");
            writer.println(this.mCallbacks);
            writer.print(prefix);
            writer.print("mLoader=");
            writer.println(this.mLoader);
            if (this.mHaveData || this.mDeliveredData) {
                writer.print(prefix);
                writer.print("mHaveData=");
                writer.print(this.mHaveData);
                writer.print("  mDeliveredData=");
                writer.println(this.mDeliveredData);
                writer.print(prefix);
                writer.print("mData=");
                writer.println(this.mData);
            }
            writer.print(prefix);
            writer.print("mStarted=");
            writer.print(this.mStarted);
            writer.print(" mReportNextStart=");
            writer.print(this.mReportNextStart);
            writer.print(" mDestroyed=");
            writer.println(this.mDestroyed);
            writer.print(prefix);
            writer.print("mRetaining=");
            writer.print(this.mRetaining);
            writer.print(" mRetainingStarted=");
            writer.print(this.mRetainingStarted);
            writer.print(" mListenerRegistered=");
            writer.println(this.mListenerRegistered);
            if (this.mPendingLoader != null) {
                writer.print(prefix);
                writer.println("Pending Loader ");
                writer.print(this.mPendingLoader);
                writer.println(":");
                LoaderInfo loaderInfo = this.mPendingLoader;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("  ");
                loaderInfo.dump(stringBuilder.toString(), fd, writer, args);
            }
        }
    }

    LoaderManagerImpl(String who, FragmentHostCallback host, boolean started) {
        this.mWho = who;
        this.mHost = host;
        this.mStarted = started;
    }

    void updateHostController(FragmentHostCallback host) {
        this.mHost = host;
    }

    public FragmentHostCallback getFragmentHostCallback() {
        return this.mHost;
    }

    private LoaderInfo createLoader(int id, Bundle args, LoaderCallbacks<Object> callback) {
        LoaderInfo info = new LoaderInfo(id, args, callback);
        info.mLoader = callback.onCreateLoader(id, args);
        return info;
    }

    private LoaderInfo createAndInstallLoader(int id, Bundle args, LoaderCallbacks<Object> callback) {
        try {
            this.mCreatingLoader = true;
            LoaderInfo info = createLoader(id, args, callback);
            installLoader(info);
            return info;
        } finally {
            this.mCreatingLoader = false;
        }
    }

    void installLoader(LoaderInfo info) {
        this.mLoaders.put(info.mId, info);
        if (this.mStarted) {
            info.start();
        }
    }

    public <D> Loader<D> initLoader(int id, Bundle args, LoaderCallbacks<D> callback) {
        if (this.mCreatingLoader) {
            throw new IllegalStateException("Called while creating a loader");
        }
        String str;
        StringBuilder stringBuilder;
        LoaderInfo info = (LoaderInfo) this.mLoaders.get(id);
        if (DEBUG) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("initLoader in ");
            stringBuilder.append(this);
            stringBuilder.append(": args=");
            stringBuilder.append(args);
            Log.v(str, stringBuilder.toString());
        }
        if (info == null) {
            info = createAndInstallLoader(id, args, callback);
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("  Created new loader ");
                stringBuilder.append(info);
                Log.v(str, stringBuilder.toString());
            }
        } else {
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("  Re-using existing loader ");
                stringBuilder.append(info);
                Log.v(str, stringBuilder.toString());
            }
            info.mCallbacks = callback;
        }
        if (info.mHaveData && this.mStarted) {
            info.callOnLoadFinished(info.mLoader, info.mData);
        }
        return info.mLoader;
    }

    public <D> Loader<D> restartLoader(int id, Bundle args, LoaderCallbacks<D> callback) {
        if (this.mCreatingLoader) {
            throw new IllegalStateException("Called while creating a loader");
        }
        LoaderInfo info = (LoaderInfo) this.mLoaders.get(id);
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("restartLoader in ");
            stringBuilder.append(this);
            stringBuilder.append(": args=");
            stringBuilder.append(args);
            Log.v(str, stringBuilder.toString());
        }
        if (info != null) {
            LoaderInfo inactive = (LoaderInfo) this.mInactiveLoaders.get(id);
            String str2;
            StringBuilder stringBuilder2;
            if (inactive == null) {
                if (DEBUG) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  Making last loader inactive: ");
                    stringBuilder2.append(info);
                    Log.v(str2, stringBuilder2.toString());
                }
                info.mLoader.abandon();
                this.mInactiveLoaders.put(id, info);
            } else if (info.mHaveData) {
                if (DEBUG) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  Removing last inactive loader: ");
                    stringBuilder2.append(info);
                    Log.v(str2, stringBuilder2.toString());
                }
                inactive.mDeliveredData = false;
                inactive.destroy();
                info.mLoader.abandon();
                this.mInactiveLoaders.put(id, info);
            } else if (info.cancel()) {
                if (DEBUG) {
                    Log.v(TAG, "  Current loader is running; configuring pending loader");
                }
                if (info.mPendingLoader != null) {
                    if (DEBUG) {
                        str2 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("  Removing pending loader: ");
                        stringBuilder3.append(info.mPendingLoader);
                        Log.v(str2, stringBuilder3.toString());
                    }
                    info.mPendingLoader.destroy();
                    info.mPendingLoader = null;
                }
                if (DEBUG) {
                    Log.v(TAG, "  Enqueuing as new pending loader");
                }
                info.mPendingLoader = createLoader(id, args, callback);
                return info.mPendingLoader.mLoader;
            } else {
                if (DEBUG) {
                    Log.v(TAG, "  Current loader is stopped; replacing");
                }
                this.mLoaders.put(id, null);
                info.destroy();
            }
        }
        return createAndInstallLoader(id, args, callback).mLoader;
    }

    public void destroyLoader(int id) {
        if (this.mCreatingLoader) {
            throw new IllegalStateException("Called while creating a loader");
        }
        LoaderInfo info;
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("destroyLoader in ");
            stringBuilder.append(this);
            stringBuilder.append(" of ");
            stringBuilder.append(id);
            Log.v(str, stringBuilder.toString());
        }
        int idx = this.mLoaders.indexOfKey(id);
        if (idx >= 0) {
            info = (LoaderInfo) this.mLoaders.valueAt(idx);
            this.mLoaders.removeAt(idx);
            info.destroy();
        }
        idx = this.mInactiveLoaders.indexOfKey(id);
        if (idx >= 0) {
            info = (LoaderInfo) this.mInactiveLoaders.valueAt(idx);
            this.mInactiveLoaders.removeAt(idx);
            info.destroy();
        }
        if (this.mHost != null && !hasRunningLoaders()) {
            this.mHost.mFragmentManager.startPendingDeferredFragments();
        }
    }

    public <D> Loader<D> getLoader(int id) {
        if (this.mCreatingLoader) {
            throw new IllegalStateException("Called while creating a loader");
        }
        LoaderInfo loaderInfo = (LoaderInfo) this.mLoaders.get(id);
        if (loaderInfo == null) {
            return null;
        }
        if (loaderInfo.mPendingLoader != null) {
            return loaderInfo.mPendingLoader.mLoader;
        }
        return loaderInfo.mLoader;
    }

    void doStart() {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Starting in ");
            stringBuilder.append(this);
            Log.v(str, stringBuilder.toString());
        }
        if (this.mStarted) {
            RuntimeException e = new RuntimeException("here");
            e.fillInStackTrace();
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Called doStart when already started: ");
            stringBuilder2.append(this);
            Log.w(str2, stringBuilder2.toString(), e);
            return;
        }
        this.mStarted = true;
        int i = this.mLoaders.size() - 1;
        while (true) {
            int i2 = i;
            if (i2 >= 0) {
                ((LoaderInfo) this.mLoaders.valueAt(i2)).start();
                i = i2 - 1;
            } else {
                return;
            }
        }
    }

    void doStop() {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stopping in ");
            stringBuilder.append(this);
            Log.v(str, stringBuilder.toString());
        }
        if (this.mStarted) {
            for (int i = this.mLoaders.size() - 1; i >= 0; i--) {
                ((LoaderInfo) this.mLoaders.valueAt(i)).stop();
            }
            this.mStarted = false;
            return;
        }
        RuntimeException e = new RuntimeException("here");
        e.fillInStackTrace();
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Called doStop when not started: ");
        stringBuilder2.append(this);
        Log.w(str2, stringBuilder2.toString(), e);
    }

    void doRetain() {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Retaining in ");
            stringBuilder.append(this);
            Log.v(str, stringBuilder.toString());
        }
        if (this.mStarted) {
            this.mRetaining = true;
            this.mStarted = false;
            int i = this.mLoaders.size() - 1;
            while (true) {
                int i2 = i;
                if (i2 >= 0) {
                    ((LoaderInfo) this.mLoaders.valueAt(i2)).retain();
                    i = i2 - 1;
                } else {
                    return;
                }
            }
        }
        RuntimeException e = new RuntimeException("here");
        e.fillInStackTrace();
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Called doRetain when not started: ");
        stringBuilder2.append(this);
        Log.w(str2, stringBuilder2.toString(), e);
    }

    void finishRetain() {
        if (this.mRetaining) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Finished Retaining in ");
                stringBuilder.append(this);
                Log.v(str, stringBuilder.toString());
            }
            this.mRetaining = false;
            for (int i = this.mLoaders.size() - 1; i >= 0; i--) {
                ((LoaderInfo) this.mLoaders.valueAt(i)).finishRetain();
            }
        }
    }

    void doReportNextStart() {
        for (int i = this.mLoaders.size() - 1; i >= 0; i--) {
            ((LoaderInfo) this.mLoaders.valueAt(i)).mReportNextStart = true;
        }
    }

    void doReportStart() {
        for (int i = this.mLoaders.size() - 1; i >= 0; i--) {
            ((LoaderInfo) this.mLoaders.valueAt(i)).reportStart();
        }
    }

    void doDestroy() {
        String str;
        StringBuilder stringBuilder;
        int i;
        if (!this.mRetaining) {
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Destroying Active in ");
                stringBuilder.append(this);
                Log.v(str, stringBuilder.toString());
            }
            for (i = this.mLoaders.size() - 1; i >= 0; i--) {
                ((LoaderInfo) this.mLoaders.valueAt(i)).destroy();
            }
            this.mLoaders.clear();
        }
        if (DEBUG) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Destroying Inactive in ");
            stringBuilder.append(this);
            Log.v(str, stringBuilder.toString());
        }
        for (i = this.mInactiveLoaders.size() - 1; i >= 0; i--) {
            ((LoaderInfo) this.mInactiveLoaders.valueAt(i)).destroy();
        }
        this.mInactiveLoaders.clear();
        this.mHost = null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("LoaderManager{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" in ");
        DebugUtils.buildShortClassTag(this.mHost, sb);
        sb.append("}}");
        return sb.toString();
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        String innerPrefix;
        int i = 0;
        if (this.mLoaders.size() > 0) {
            writer.print(prefix);
            writer.println("Active Loaders:");
            innerPrefix = new StringBuilder();
            innerPrefix.append(prefix);
            innerPrefix.append("    ");
            innerPrefix = innerPrefix.toString();
            for (int i2 = 0; i2 < this.mLoaders.size(); i2++) {
                LoaderInfo li = (LoaderInfo) this.mLoaders.valueAt(i2);
                writer.print(prefix);
                writer.print("  #");
                writer.print(this.mLoaders.keyAt(i2));
                writer.print(": ");
                writer.println(li.toString());
                li.dump(innerPrefix, fd, writer, args);
            }
        }
        if (this.mInactiveLoaders.size() > 0) {
            writer.print(prefix);
            writer.println("Inactive Loaders:");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("    ");
            innerPrefix = stringBuilder.toString();
            while (i < this.mInactiveLoaders.size()) {
                LoaderInfo li2 = (LoaderInfo) this.mInactiveLoaders.valueAt(i);
                writer.print(prefix);
                writer.print("  #");
                writer.print(this.mInactiveLoaders.keyAt(i));
                writer.print(": ");
                writer.println(li2.toString());
                li2.dump(innerPrefix, fd, writer, args);
                i++;
            }
        }
    }

    public boolean hasRunningLoaders() {
        int count = this.mLoaders.size();
        boolean loadersRunning = false;
        for (int i = 0; i < count; i++) {
            LoaderInfo li = (LoaderInfo) this.mLoaders.valueAt(i);
            int i2 = (!li.mStarted || li.mDeliveredData) ? 0 : 1;
            loadersRunning |= i2;
        }
        return loadersRunning;
    }
}
