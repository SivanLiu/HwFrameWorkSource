package android.arch.lifecycle;

import android.arch.lifecycle.Lifecycle.Event;

class ReflectiveGenericLifecycleObserver implements GenericLifecycleObserver {
    private final CallbackInfo mInfo = ClassesInfoCache.sInstance.getInfo(this.mWrapped.getClass());
    private final Object mWrapped;

    ReflectiveGenericLifecycleObserver(Object wrapped) {
        this.mWrapped = wrapped;
    }

    public void onStateChanged(LifecycleOwner source, Event event) {
        this.mInfo.invokeCallbacks(source, event, this.mWrapped);
    }
}
