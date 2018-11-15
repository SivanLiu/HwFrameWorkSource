package com.android.server.rms.dump;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DumpAlarmManager$yU5hWyGyhPsIICiAzmBOdnA7mnA implements Consumer {
    public static final /* synthetic */ -$$Lambda$DumpAlarmManager$yU5hWyGyhPsIICiAzmBOdnA7mnA INSTANCE = new -$$Lambda$DumpAlarmManager$yU5hWyGyhPsIICiAzmBOdnA7mnA();

    private /* synthetic */ -$$Lambda$DumpAlarmManager$yU5hWyGyhPsIICiAzmBOdnA7mnA() {
    }

    public final void accept(Object obj) {
        DumpAlarmManager.dumpDebugLog(((Params) obj).context, ((Params) obj).pw, ((Params) obj).args);
    }
}
