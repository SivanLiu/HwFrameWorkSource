package android.hardware.radio;

import android.hardware.radio.ProgramList.ListCallback;
import android.hardware.radio.ProgramSelector.Identifier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ProgramList$1$DVvry5MfhR6n8H2EZn67rvuhllI implements Runnable {
    private final /* synthetic */ ListCallback f$0;
    private final /* synthetic */ Identifier f$1;

    public /* synthetic */ -$$Lambda$ProgramList$1$DVvry5MfhR6n8H2EZn67rvuhllI(ListCallback listCallback, Identifier identifier) {
        this.f$0 = listCallback;
        this.f$1 = identifier;
    }

    public final void run() {
        this.f$0.onItemChanged(this.f$1);
    }
}
