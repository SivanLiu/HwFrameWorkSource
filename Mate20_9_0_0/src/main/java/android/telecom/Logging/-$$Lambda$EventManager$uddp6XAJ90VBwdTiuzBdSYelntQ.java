package android.telecom.Logging;

import android.telecom.Logging.EventManager.EventRecord;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EventManager$uddp6XAJ90VBwdTiuzBdSYelntQ implements Consumer {
    private final /* synthetic */ EventManager f$0;

    public /* synthetic */ -$$Lambda$EventManager$uddp6XAJ90VBwdTiuzBdSYelntQ(EventManager eventManager) {
        this.f$0 = eventManager;
    }

    public final void accept(Object obj) {
        EventManager.lambda$changeEventCacheSize$1(this.f$0, (EventRecord) obj);
    }
}
