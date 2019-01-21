package android.companion;

import android.companion.CompanionDeviceManager.Callback;
import android.content.IntentSender;
import java.util.function.BiConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$OThxsns9MAD5QsKURFQAFbt-3qc implements BiConsumer {
    public static final /* synthetic */ -$$Lambda$OThxsns9MAD5QsKURFQAFbt-3qc INSTANCE = new -$$Lambda$OThxsns9MAD5QsKURFQAFbt-3qc();

    private /* synthetic */ -$$Lambda$OThxsns9MAD5QsKURFQAFbt-3qc() {
    }

    public final void accept(Object obj, Object obj2) {
        ((Callback) obj).onDeviceFound((IntentSender) obj2);
    }
}
