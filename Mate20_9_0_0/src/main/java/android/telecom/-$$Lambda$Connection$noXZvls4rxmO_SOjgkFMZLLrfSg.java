package android.telecom;

import android.telecom.Connection.Listener;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Connection$noXZvls4rxmO_SOjgkFMZLLrfSg implements Consumer {
    private final /* synthetic */ Connection f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$Connection$noXZvls4rxmO_SOjgkFMZLLrfSg(Connection connection, int i) {
        this.f$0 = connection;
        this.f$1 = i;
    }

    public final void accept(Object obj) {
        ((Listener) obj).onRttInitiationFailure(this.f$0, this.f$1);
    }
}
