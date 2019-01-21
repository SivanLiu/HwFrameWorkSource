package android.hardware.radio;

import android.hardware.radio.ProgramSelector.Identifier;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ProgramList$F-JpTj3vYguKIUQbnLbTePTuqUE implements Consumer {
    private final /* synthetic */ ProgramList f$0;

    public /* synthetic */ -$$Lambda$ProgramList$F-JpTj3vYguKIUQbnLbTePTuqUE(ProgramList programList) {
        this.f$0 = programList;
    }

    public final void accept(Object obj) {
        this.f$0.removeLocked((Identifier) obj);
    }
}
