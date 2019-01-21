package android.hardware.radio;

import android.hardware.radio.ProgramSelector.Identifier;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ProgramList$pKu0Zp5jwjix619hfB_Imj8Ke_g implements Consumer {
    private final /* synthetic */ ProgramList f$0;

    public /* synthetic */ -$$Lambda$ProgramList$pKu0Zp5jwjix619hfB_Imj8Ke_g(ProgramList programList) {
        this.f$0 = programList;
    }

    public final void accept(Object obj) {
        this.f$0.removeLocked((Identifier) obj);
    }
}
