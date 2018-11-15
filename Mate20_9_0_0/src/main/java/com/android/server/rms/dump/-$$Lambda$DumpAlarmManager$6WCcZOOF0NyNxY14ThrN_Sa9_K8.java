package com.android.server.rms.dump;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DumpAlarmManager$6WCcZOOF0NyNxY14ThrN_Sa9_K8 implements Consumer {
    public static final /* synthetic */ -$$Lambda$DumpAlarmManager$6WCcZOOF0NyNxY14ThrN_Sa9_K8 INSTANCE = new -$$Lambda$DumpAlarmManager$6WCcZOOF0NyNxY14ThrN_Sa9_K8();

    private /* synthetic */ -$$Lambda$DumpAlarmManager$6WCcZOOF0NyNxY14ThrN_Sa9_K8() {
    }

    public final void accept(Object obj) {
        DumpAlarmManager.dumpBigData(((Params) obj).context, ((Params) obj).pw, ((Params) obj).args);
    }
}
