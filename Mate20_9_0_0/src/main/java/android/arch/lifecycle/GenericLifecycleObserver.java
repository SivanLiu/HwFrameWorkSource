package android.arch.lifecycle;

import android.arch.lifecycle.Lifecycle.Event;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;

@RestrictTo({Scope.LIBRARY})
public interface GenericLifecycleObserver extends LifecycleObserver {
    void onStateChanged(LifecycleOwner lifecycleOwner, Event event);
}
