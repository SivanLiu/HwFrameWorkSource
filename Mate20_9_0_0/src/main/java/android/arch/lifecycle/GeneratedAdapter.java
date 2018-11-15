package android.arch.lifecycle;

import android.arch.lifecycle.Lifecycle.Event;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;

@RestrictTo({Scope.LIBRARY_GROUP})
public interface GeneratedAdapter {
    void callMethods(LifecycleOwner lifecycleOwner, Event event, boolean z, MethodCallsLogger methodCallsLogger);
}
