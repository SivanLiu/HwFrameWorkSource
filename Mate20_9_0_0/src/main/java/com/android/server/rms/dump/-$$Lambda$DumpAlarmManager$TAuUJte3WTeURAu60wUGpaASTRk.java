package com.android.server.rms.dump;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DumpAlarmManager$TAuUJte3WTeURAu60wUGpaASTRk implements Consumer {
    public static final /* synthetic */ -$$Lambda$DumpAlarmManager$TAuUJte3WTeURAu60wUGpaASTRk INSTANCE = new -$$Lambda$DumpAlarmManager$TAuUJte3WTeURAu60wUGpaASTRk();

    private /* synthetic */ -$$Lambda$DumpAlarmManager$TAuUJte3WTeURAu60wUGpaASTRk() {
    }

    public final void accept(Object obj) {
        DumpAlarmManager.setDebugSwitch(((Params) obj).context, ((Params) obj).pw, ((Params) obj).args);
    }
}
