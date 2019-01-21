package android.telecom;

import android.telecom.Connection.Listener;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Connection$lnfFNF0t9fPLEf01JE291g4chSk implements Consumer {
    private final /* synthetic */ Connection f$0;

    public /* synthetic */ -$$Lambda$Connection$lnfFNF0t9fPLEf01JE291g4chSk(Connection connection) {
        this.f$0 = connection;
    }

    public final void accept(Object obj) {
        ((Listener) obj).onRemoteRttRequest(this.f$0);
    }
}
