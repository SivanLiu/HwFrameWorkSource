package android.hardware.radio;

import android.hardware.radio.ProgramList.ListCallback;
import android.hardware.radio.ProgramSelector.Identifier;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ProgramList$fDnoTVk5UB7qTfD9S7SYPcadYn0 implements Consumer {
    private final /* synthetic */ Identifier f$0;

    public /* synthetic */ -$$Lambda$ProgramList$fDnoTVk5UB7qTfD9S7SYPcadYn0(Identifier identifier) {
        this.f$0 = identifier;
    }

    public final void accept(Object obj) {
        ((ListCallback) obj).onItemChanged(this.f$0);
    }
}
