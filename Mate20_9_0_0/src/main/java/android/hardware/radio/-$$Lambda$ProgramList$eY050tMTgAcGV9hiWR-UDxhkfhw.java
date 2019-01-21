package android.hardware.radio;

import android.hardware.radio.RadioManager.ProgramInfo;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ProgramList$eY050tMTgAcGV9hiWR-UDxhkfhw implements Consumer {
    private final /* synthetic */ ProgramList f$0;

    public /* synthetic */ -$$Lambda$ProgramList$eY050tMTgAcGV9hiWR-UDxhkfhw(ProgramList programList) {
        this.f$0 = programList;
    }

    public final void accept(Object obj) {
        this.f$0.putLocked((ProgramInfo) obj);
    }
}
