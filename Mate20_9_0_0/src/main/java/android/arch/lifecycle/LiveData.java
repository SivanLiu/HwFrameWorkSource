package android.arch.lifecycle;

import android.arch.core.executor.ArchTaskExecutor;
import android.arch.core.internal.SafeIterableMap;
import android.arch.lifecycle.Lifecycle.Event;
import android.arch.lifecycle.Lifecycle.State;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.Iterator;
import java.util.Map.Entry;

public abstract class LiveData<T> {
    private static final Object NOT_SET = new Object();
    static final int START_VERSION = -1;
    private int mActiveCount = 0;
    private volatile Object mData = NOT_SET;
    private final Object mDataLock = new Object();
    private boolean mDispatchInvalidated;
    private boolean mDispatchingValue;
    private SafeIterableMap<Observer<? super T>, ObserverWrapper> mObservers = new SafeIterableMap();
    private volatile Object mPendingData = NOT_SET;
    private final Runnable mPostValueRunnable = new Runnable() {
        public void run() {
            Object newValue;
            synchronized (LiveData.this.mDataLock) {
                newValue = LiveData.this.mPendingData;
                LiveData.this.mPendingData = LiveData.NOT_SET;
            }
            LiveData.this.setValue(newValue);
        }
    };
    private int mVersion = -1;

    private abstract class ObserverWrapper {
        boolean mActive;
        int mLastVersion = -1;
        final Observer<? super T> mObserver;

        abstract boolean shouldBeActive();

        ObserverWrapper(Observer<? super T> observer) {
            this.mObserver = observer;
        }

        boolean isAttachedTo(LifecycleOwner owner) {
            return false;
        }

        void detachObserver() {
        }

        void activeStateChanged(boolean newActive) {
            if (newActive != this.mActive) {
                this.mActive = newActive;
                int i = 1;
                boolean wasInactive = LiveData.this.mActiveCount == 0;
                LiveData liveData = LiveData.this;
                int access$300 = liveData.mActiveCount;
                if (!this.mActive) {
                    i = -1;
                }
                liveData.mActiveCount = access$300 + i;
                if (wasInactive && this.mActive) {
                    LiveData.this.onActive();
                }
                if (LiveData.this.mActiveCount == 0 && !this.mActive) {
                    LiveData.this.onInactive();
                }
                if (this.mActive) {
                    LiveData.this.dispatchingValue(this);
                }
            }
        }
    }

    private class AlwaysActiveObserver extends ObserverWrapper {
        AlwaysActiveObserver(Observer<? super T> observer) {
            super(observer);
        }

        boolean shouldBeActive() {
            return true;
        }
    }

    class LifecycleBoundObserver extends ObserverWrapper implements GenericLifecycleObserver {
        @NonNull
        final LifecycleOwner mOwner;

        LifecycleBoundObserver(@NonNull LifecycleOwner owner, Observer<? super T> observer) {
            super(observer);
            this.mOwner = owner;
        }

        boolean shouldBeActive() {
            return this.mOwner.getLifecycle().getCurrentState().isAtLeast(State.STARTED);
        }

        public void onStateChanged(LifecycleOwner source, Event event) {
            if (this.mOwner.getLifecycle().getCurrentState() == State.DESTROYED) {
                LiveData.this.removeObserver(this.mObserver);
            } else {
                activeStateChanged(shouldBeActive());
            }
        }

        boolean isAttachedTo(LifecycleOwner owner) {
            return this.mOwner == owner;
        }

        void detachObserver() {
            this.mOwner.getLifecycle().removeObserver(this);
        }
    }

    private void considerNotify(ObserverWrapper observer) {
        if (!observer.mActive) {
            return;
        }
        if (!observer.shouldBeActive()) {
            observer.activeStateChanged(false);
        } else if (observer.mLastVersion < this.mVersion) {
            observer.mLastVersion = this.mVersion;
            observer.mObserver.onChanged(this.mData);
        }
    }

    private void dispatchingValue(@Nullable ObserverWrapper initiator) {
        if (this.mDispatchingValue) {
            this.mDispatchInvalidated = true;
            return;
        }
        this.mDispatchingValue = true;
        do {
            this.mDispatchInvalidated = false;
            if (initiator == null) {
                Iterator<Entry<Observer<? super T>, ObserverWrapper>> iterator = this.mObservers.iteratorWithAdditions();
                while (iterator.hasNext()) {
                    considerNotify((ObserverWrapper) ((Entry) iterator.next()).getValue());
                    if (this.mDispatchInvalidated) {
                        break;
                    }
                }
            }
            considerNotify(initiator);
            initiator = null;
        } while (this.mDispatchInvalidated);
        this.mDispatchingValue = false;
    }

    @MainThread
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        assertMainThread("observe");
        if (owner.getLifecycle().getCurrentState() != State.DESTROYED) {
            LifecycleBoundObserver wrapper = new LifecycleBoundObserver(owner, observer);
            ObserverWrapper existing = (ObserverWrapper) this.mObservers.putIfAbsent(observer, wrapper);
            if (existing != null && !existing.isAttachedTo(owner)) {
                throw new IllegalArgumentException("Cannot add the same observer with different lifecycles");
            } else if (existing == null) {
                owner.getLifecycle().addObserver(wrapper);
            }
        }
    }

    @MainThread
    public void observeForever(@NonNull Observer<? super T> observer) {
        assertMainThread("observeForever");
        AlwaysActiveObserver wrapper = new AlwaysActiveObserver(observer);
        ObserverWrapper existing = (ObserverWrapper) this.mObservers.putIfAbsent(observer, wrapper);
        if (existing != null && (existing instanceof LifecycleBoundObserver)) {
            throw new IllegalArgumentException("Cannot add the same observer with different lifecycles");
        } else if (existing == null) {
            wrapper.activeStateChanged(true);
        }
    }

    @MainThread
    public void removeObserver(@NonNull Observer<? super T> observer) {
        assertMainThread("removeObserver");
        ObserverWrapper removed = (ObserverWrapper) this.mObservers.remove(observer);
        if (removed != null) {
            removed.detachObserver();
            removed.activeStateChanged(false);
        }
    }

    @MainThread
    public void removeObservers(@NonNull LifecycleOwner owner) {
        assertMainThread("removeObservers");
        Iterator it = this.mObservers.iterator();
        while (it.hasNext()) {
            Entry<Observer<? super T>, ObserverWrapper> entry = (Entry) it.next();
            if (((ObserverWrapper) entry.getValue()).isAttachedTo(owner)) {
                removeObserver((Observer) entry.getKey());
            }
        }
    }

    protected void postValue(T value) {
        boolean postTask;
        synchronized (this.mDataLock) {
            postTask = false;
            if (this.mPendingData == NOT_SET) {
                postTask = true;
            }
            this.mPendingData = value;
        }
        if (postTask) {
            ArchTaskExecutor.getInstance().postToMainThread(this.mPostValueRunnable);
        }
    }

    @MainThread
    protected void setValue(T value) {
        assertMainThread("setValue");
        this.mVersion++;
        this.mData = value;
        dispatchingValue(null);
    }

    @Nullable
    public T getValue() {
        Object data = this.mData;
        if (data != NOT_SET) {
            return data;
        }
        return null;
    }

    int getVersion() {
        return this.mVersion;
    }

    protected void onActive() {
    }

    protected void onInactive() {
    }

    public boolean hasObservers() {
        return this.mObservers.size() > 0;
    }

    public boolean hasActiveObservers() {
        return this.mActiveCount > 0;
    }

    private static void assertMainThread(String methodName) {
        if (!ArchTaskExecutor.getInstance().isMainThread()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot invoke ");
            stringBuilder.append(methodName);
            stringBuilder.append(" on a background");
            stringBuilder.append(" thread");
            throw new IllegalStateException(stringBuilder.toString());
        }
    }
}
