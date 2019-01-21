package android.support.v4.app;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProvider.Factory;
import android.arch.lifecycle.ViewModelStore;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.content.Loader.OnLoadCompleteListener;
import android.support.v4.util.DebugUtils;
import android.support.v4.util.SparseArrayCompat;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;

class LoaderManagerImpl extends LoaderManager {
    static boolean DEBUG = false;
    static final String TAG = "LoaderManager";
    @NonNull
    private final LifecycleOwner mLifecycleOwner;
    @NonNull
    private final LoaderViewModel mLoaderViewModel;

    static class LoaderObserver<D> implements Observer<D> {
        @NonNull
        private final LoaderCallbacks<D> mCallback;
        private boolean mDeliveredData = false;
        @NonNull
        private final Loader<D> mLoader;

        LoaderObserver(@NonNull Loader<D> loader, @NonNull LoaderCallbacks<D> callback) {
            this.mLoader = loader;
            this.mCallback = callback;
        }

        public void onChanged(@Nullable D data) {
            if (LoaderManagerImpl.DEBUG) {
                String str = LoaderManagerImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("  onLoadFinished in ");
                stringBuilder.append(this.mLoader);
                stringBuilder.append(": ");
                stringBuilder.append(this.mLoader.dataToString(data));
                Log.v(str, stringBuilder.toString());
            }
            this.mCallback.onLoadFinished(this.mLoader, data);
            this.mDeliveredData = true;
        }

        boolean hasDeliveredData() {
            return this.mDeliveredData;
        }

        @MainThread
        void reset() {
            if (this.mDeliveredData) {
                if (LoaderManagerImpl.DEBUG) {
                    String str = LoaderManagerImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("  Resetting: ");
                    stringBuilder.append(this.mLoader);
                    Log.v(str, stringBuilder.toString());
                }
                this.mCallback.onLoaderReset(this.mLoader);
            }
        }

        public String toString() {
            return this.mCallback.toString();
        }

        public void dump(String prefix, PrintWriter writer) {
            writer.print(prefix);
            writer.print("mDeliveredData=");
            writer.println(this.mDeliveredData);
        }
    }

    static class LoaderViewModel extends ViewModel {
        private static final Factory FACTORY = new Factory() {
            @NonNull
            public <T extends ViewModel> T create(@NonNull Class<T> cls) {
                return new LoaderViewModel();
            }
        };
        private boolean mCreatingLoader = false;
        private SparseArrayCompat<LoaderInfo> mLoaders = new SparseArrayCompat();

        LoaderViewModel() {
        }

        @NonNull
        static LoaderViewModel getInstance(ViewModelStore viewModelStore) {
            return (LoaderViewModel) new ViewModelProvider(viewModelStore, FACTORY).get(LoaderViewModel.class);
        }

        void startCreatingLoader() {
            this.mCreatingLoader = true;
        }

        boolean isCreatingLoader() {
            return this.mCreatingLoader;
        }

        void finishCreatingLoader() {
            this.mCreatingLoader = false;
        }

        void putLoader(int id, @NonNull LoaderInfo info) {
            this.mLoaders.put(id, info);
        }

        <D> LoaderInfo<D> getLoader(int id) {
            return (LoaderInfo) this.mLoaders.get(id);
        }

        void removeLoader(int id) {
            this.mLoaders.remove(id);
        }

        boolean hasRunningLoaders() {
            int size = this.mLoaders.size();
            for (int index = 0; index < size; index++) {
                if (((LoaderInfo) this.mLoaders.valueAt(index)).isCallbackWaitingForData()) {
                    return true;
                }
            }
            return false;
        }

        void markForRedelivery() {
            int size = this.mLoaders.size();
            for (int index = 0; index < size; index++) {
                ((LoaderInfo) this.mLoaders.valueAt(index)).markForRedelivery();
            }
        }

        protected void onCleared() {
            super.onCleared();
            int size = this.mLoaders.size();
            for (int index = 0; index < size; index++) {
                ((LoaderInfo) this.mLoaders.valueAt(index)).destroy(true);
            }
            this.mLoaders.clear();
        }

        public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            if (this.mLoaders.size() > 0) {
                writer.print(prefix);
                writer.println("Loaders:");
                String innerPrefix = new StringBuilder();
                innerPrefix.append(prefix);
                innerPrefix.append("    ");
                innerPrefix = innerPrefix.toString();
                for (int i = 0; i < this.mLoaders.size(); i++) {
                    LoaderInfo info = (LoaderInfo) this.mLoaders.valueAt(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(this.mLoaders.keyAt(i));
                    writer.print(": ");
                    writer.println(info.toString());
                    info.dump(innerPrefix, fd, writer, args);
                }
            }
        }
    }

    public static class LoaderInfo<D> extends MutableLiveData<D> implements OnLoadCompleteListener<D> {
        @Nullable
        private final Bundle mArgs;
        private final int mId;
        private LifecycleOwner mLifecycleOwner;
        @NonNull
        private final Loader<D> mLoader;
        private LoaderObserver<D> mObserver;
        private Loader<D> mPriorLoader;

        LoaderInfo(int id, @Nullable Bundle args, @NonNull Loader<D> loader, @Nullable Loader<D> priorLoader) {
            this.mId = id;
            this.mArgs = args;
            this.mLoader = loader;
            this.mPriorLoader = priorLoader;
            this.mLoader.registerListener(id, this);
        }

        @NonNull
        Loader<D> getLoader() {
            return this.mLoader;
        }

        protected void onActive() {
            if (LoaderManagerImpl.DEBUG) {
                String str = LoaderManagerImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("  Starting: ");
                stringBuilder.append(this);
                Log.v(str, stringBuilder.toString());
            }
            this.mLoader.startLoading();
        }

        protected void onInactive() {
            if (LoaderManagerImpl.DEBUG) {
                String str = LoaderManagerImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("  Stopping: ");
                stringBuilder.append(this);
                Log.v(str, stringBuilder.toString());
            }
            this.mLoader.stopLoading();
        }

        @MainThread
        @NonNull
        Loader<D> setCallback(@NonNull LifecycleOwner owner, @NonNull LoaderCallbacks<D> callback) {
            LoaderObserver<D> observer = new LoaderObserver(this.mLoader, callback);
            observe(owner, observer);
            if (this.mObserver != null) {
                removeObserver(this.mObserver);
            }
            this.mLifecycleOwner = owner;
            this.mObserver = observer;
            return this.mLoader;
        }

        void markForRedelivery() {
            LifecycleOwner lifecycleOwner = this.mLifecycleOwner;
            LoaderObserver<D> observer = this.mObserver;
            if (lifecycleOwner != null && observer != null) {
                super.removeObserver(observer);
                observe(lifecycleOwner, observer);
            }
        }

        boolean isCallbackWaitingForData() {
            boolean z = false;
            if (!hasActiveObservers()) {
                return false;
            }
            if (!(this.mObserver == null || this.mObserver.hasDeliveredData())) {
                z = true;
            }
            return z;
        }

        public void removeObserver(@NonNull Observer<? super D> observer) {
            super.removeObserver(observer);
            this.mLifecycleOwner = null;
            this.mObserver = null;
        }

        @MainThread
        Loader<D> destroy(boolean reset) {
            if (LoaderManagerImpl.DEBUG) {
                String str = LoaderManagerImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("  Destroying: ");
                stringBuilder.append(this);
                Log.v(str, stringBuilder.toString());
            }
            this.mLoader.cancelLoad();
            this.mLoader.abandon();
            LoaderObserver<D> observer = this.mObserver;
            if (observer != null) {
                removeObserver(observer);
                if (reset) {
                    observer.reset();
                }
            }
            this.mLoader.unregisterListener(this);
            if ((observer == null || observer.hasDeliveredData()) && !reset) {
                return this.mLoader;
            }
            this.mLoader.reset();
            return this.mPriorLoader;
        }

        public void onLoadComplete(@NonNull Loader<D> loader, @Nullable D data) {
            if (LoaderManagerImpl.DEBUG) {
                String str = LoaderManagerImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onLoadComplete: ");
                stringBuilder.append(this);
                Log.v(str, stringBuilder.toString());
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                setValue(data);
                return;
            }
            if (LoaderManagerImpl.DEBUG) {
                Log.w(LoaderManagerImpl.TAG, "onLoadComplete was incorrectly called on a background thread");
            }
            postValue(data);
        }

        public void setValue(D value) {
            super.setValue(value);
            if (this.mPriorLoader != null) {
                this.mPriorLoader.reset();
                this.mPriorLoader = null;
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
            writer.print("mLoader=");
            writer.println(this.mLoader);
            Loader loader = this.mLoader;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  ");
            loader.dump(stringBuilder.toString(), fd, writer, args);
            if (this.mObserver != null) {
                writer.print(prefix);
                writer.print("mCallbacks=");
                writer.println(this.mObserver);
                LoaderObserver loaderObserver = this.mObserver;
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("  ");
                loaderObserver.dump(stringBuilder.toString(), writer);
            }
            writer.print(prefix);
            writer.print("mData=");
            writer.println(getLoader().dataToString(getValue()));
            writer.print(prefix);
            writer.print("mStarted=");
            writer.println(hasActiveObservers());
        }
    }

    LoaderManagerImpl(@NonNull LifecycleOwner lifecycleOwner, @NonNull ViewModelStore viewModelStore) {
        this.mLifecycleOwner = lifecycleOwner;
        this.mLoaderViewModel = LoaderViewModel.getInstance(viewModelStore);
    }

    @MainThread
    @NonNull
    private <D> Loader<D> createAndInstallLoader(int id, @Nullable Bundle args, @NonNull LoaderCallbacks<D> callback, @Nullable Loader<D> priorLoader) {
        LoaderInfo<D> info = null;
        try {
            this.mLoaderViewModel.startCreatingLoader();
            Loader<D> loader = callback.onCreateLoader(id, args);
            if (loader != null) {
                StringBuilder stringBuilder;
                if (loader.getClass().isMemberClass()) {
                    if (!Modifier.isStatic(loader.getClass().getModifiers())) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Object returned from onCreateLoader must not be a non-static inner member class: ");
                        stringBuilder.append(loader);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                }
                info = new LoaderInfo(id, args, loader, priorLoader);
                if (DEBUG) {
                    String str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("  Created new loader ");
                    stringBuilder.append(info);
                    Log.v(str, stringBuilder.toString());
                }
                this.mLoaderViewModel.putLoader(id, info);
                return info.setCallback(this.mLifecycleOwner, callback);
            }
            throw new IllegalArgumentException("Object returned from onCreateLoader must not be null");
        } finally {
            this.mLoaderViewModel.finishCreatingLoader();
        }
    }

    @MainThread
    @NonNull
    public <D> Loader<D> initLoader(int id, @Nullable Bundle args, @NonNull LoaderCallbacks<D> callback) {
        if (this.mLoaderViewModel.isCreatingLoader()) {
            throw new IllegalStateException("Called while creating a loader");
        } else if (Looper.getMainLooper() == Looper.myLooper()) {
            String str;
            StringBuilder stringBuilder;
            LoaderInfo<D> info = this.mLoaderViewModel.getLoader(id);
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
                return createAndInstallLoader(id, args, callback, null);
            }
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("  Re-using existing loader ");
                stringBuilder.append(info);
                Log.v(str, stringBuilder.toString());
            }
            return info.setCallback(this.mLifecycleOwner, callback);
        } else {
            throw new IllegalStateException("initLoader must be called on the main thread");
        }
    }

    @MainThread
    @NonNull
    public <D> Loader<D> restartLoader(int id, @Nullable Bundle args, @NonNull LoaderCallbacks<D> callback) {
        if (this.mLoaderViewModel.isCreatingLoader()) {
            throw new IllegalStateException("Called while creating a loader");
        } else if (Looper.getMainLooper() == Looper.myLooper()) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("restartLoader in ");
                stringBuilder.append(this);
                stringBuilder.append(": args=");
                stringBuilder.append(args);
                Log.v(str, stringBuilder.toString());
            }
            LoaderInfo<D> info = this.mLoaderViewModel.getLoader(id);
            Loader<D> priorLoader = null;
            if (info != null) {
                priorLoader = info.destroy(false);
            }
            return createAndInstallLoader(id, args, callback, priorLoader);
        } else {
            throw new IllegalStateException("restartLoader must be called on the main thread");
        }
    }

    @MainThread
    public void destroyLoader(int id) {
        if (this.mLoaderViewModel.isCreatingLoader()) {
            throw new IllegalStateException("Called while creating a loader");
        } else if (Looper.getMainLooper() == Looper.myLooper()) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("destroyLoader in ");
                stringBuilder.append(this);
                stringBuilder.append(" of ");
                stringBuilder.append(id);
                Log.v(str, stringBuilder.toString());
            }
            LoaderInfo info = this.mLoaderViewModel.getLoader(id);
            if (info != null) {
                info.destroy(true);
                this.mLoaderViewModel.removeLoader(id);
            }
        } else {
            throw new IllegalStateException("destroyLoader must be called on the main thread");
        }
    }

    @Nullable
    public <D> Loader<D> getLoader(int id) {
        if (this.mLoaderViewModel.isCreatingLoader()) {
            throw new IllegalStateException("Called while creating a loader");
        }
        LoaderInfo<D> info = this.mLoaderViewModel.getLoader(id);
        return info != null ? info.getLoader() : null;
    }

    public void markForRedelivery() {
        this.mLoaderViewModel.markForRedelivery();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("LoaderManager{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" in ");
        DebugUtils.buildShortClassTag(this.mLifecycleOwner, sb);
        sb.append("}}");
        return sb.toString();
    }

    @Deprecated
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        this.mLoaderViewModel.dump(prefix, fd, writer, args);
    }

    public boolean hasRunningLoaders() {
        return this.mLoaderViewModel.hasRunningLoaders();
    }
}
