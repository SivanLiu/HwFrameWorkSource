package android.telecom.Logging;

import android.os.Process;
import android.telecom.Logging.SessionManager.ICurrentThreadId;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$L5F_SL2jOCUETYvgdB36aGwY50E implements ICurrentThreadId {
    public static final /* synthetic */ -$$Lambda$L5F_SL2jOCUETYvgdB36aGwY50E INSTANCE = new -$$Lambda$L5F_SL2jOCUETYvgdB36aGwY50E();

    private /* synthetic */ -$$Lambda$L5F_SL2jOCUETYvgdB36aGwY50E() {
    }

    public final int get() {
        return Process.myTid();
    }
}
