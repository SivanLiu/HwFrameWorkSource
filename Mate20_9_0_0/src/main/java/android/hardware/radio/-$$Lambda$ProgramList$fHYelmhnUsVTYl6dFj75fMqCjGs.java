package android.hardware.radio;

import android.hardware.radio.ProgramList.ListCallback;
import android.hardware.radio.ProgramSelector.Identifier;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ProgramList$fHYelmhnUsVTYl6dFj75fMqCjGs implements Consumer {
    private final /* synthetic */ Identifier f$0;

    public /* synthetic */ -$$Lambda$ProgramList$fHYelmhnUsVTYl6dFj75fMqCjGs(Identifier identifier) {
        this.f$0 = identifier;
    }

    public final void accept(Object obj) {
        ((ListCallback) obj).onItemRemoved(this.f$0);
    }
}
