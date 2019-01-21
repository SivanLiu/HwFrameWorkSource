package android.app;

import android.content.res.Configuration;
import android.view.ViewRootImpl.ConfigChangedCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ActivityThread$ZXDWm3IBeFmLnFVblhB-IOZCr9o implements ConfigChangedCallback {
    private final /* synthetic */ ActivityThread f$0;

    public /* synthetic */ -$$Lambda$ActivityThread$ZXDWm3IBeFmLnFVblhB-IOZCr9o(ActivityThread activityThread) {
        this.f$0 = activityThread;
    }

    public final void onConfigurationChanged(Configuration configuration) {
        ActivityThread.lambda$attach$0(this.f$0, configuration);
    }
}
