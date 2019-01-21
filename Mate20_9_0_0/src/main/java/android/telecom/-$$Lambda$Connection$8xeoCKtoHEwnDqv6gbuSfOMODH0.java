package android.telecom;

import android.telecom.Connection.Listener;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Connection$8xeoCKtoHEwnDqv6gbuSfOMODH0 implements Consumer {
    private final /* synthetic */ Connection f$0;

    public /* synthetic */ -$$Lambda$Connection$8xeoCKtoHEwnDqv6gbuSfOMODH0(Connection connection) {
        this.f$0 = connection;
    }

    public final void accept(Object obj) {
        ((Listener) obj).onRttInitiationSuccess(this.f$0);
    }
}
