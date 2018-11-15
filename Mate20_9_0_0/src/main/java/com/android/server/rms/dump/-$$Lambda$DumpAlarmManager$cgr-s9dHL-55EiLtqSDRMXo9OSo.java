package com.android.server.rms.dump;

import com.android.server.rms.iaware.appmng.AwareWakeUpManager;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DumpAlarmManager$cgr-s9dHL-55EiLtqSDRMXo9OSo implements Consumer {
    public static final /* synthetic */ -$$Lambda$DumpAlarmManager$cgr-s9dHL-55EiLtqSDRMXo9OSo INSTANCE = new -$$Lambda$DumpAlarmManager$cgr-s9dHL-55EiLtqSDRMXo9OSo();

    private /* synthetic */ -$$Lambda$DumpAlarmManager$cgr-s9dHL-55EiLtqSDRMXo9OSo() {
    }

    public final void accept(Object obj) {
        AwareWakeUpManager.getInstance().dumpParam(((Params) obj).pw);
    }
}
