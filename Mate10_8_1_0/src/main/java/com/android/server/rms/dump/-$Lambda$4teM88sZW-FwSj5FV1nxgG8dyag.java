package com.android.server.rms.dump;

import com.android.server.rms.iaware.appmng.AwareWakeUpManager;
import java.util.function.Consumer;

final /* synthetic */ class -$Lambda$4teM88sZW-FwSj5FV1nxgG8dyag implements Consumer {
    public static final /* synthetic */ -$Lambda$4teM88sZW-FwSj5FV1nxgG8dyag $INST$0 = new -$Lambda$4teM88sZW-FwSj5FV1nxgG8dyag((byte) 0);
    public static final /* synthetic */ -$Lambda$4teM88sZW-FwSj5FV1nxgG8dyag $INST$1 = new -$Lambda$4teM88sZW-FwSj5FV1nxgG8dyag((byte) 1);
    public static final /* synthetic */ -$Lambda$4teM88sZW-FwSj5FV1nxgG8dyag $INST$2 = new -$Lambda$4teM88sZW-FwSj5FV1nxgG8dyag((byte) 2);
    public static final /* synthetic */ -$Lambda$4teM88sZW-FwSj5FV1nxgG8dyag $INST$3 = new -$Lambda$4teM88sZW-FwSj5FV1nxgG8dyag((byte) 3);
    public static final /* synthetic */ -$Lambda$4teM88sZW-FwSj5FV1nxgG8dyag $INST$4 = new -$Lambda$4teM88sZW-FwSj5FV1nxgG8dyag((byte) 4);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ void $m$0(Object arg0) {
        DumpAlarmManager.delay(((Params) arg0).context, ((Params) arg0).pw, ((Params) arg0).args);
    }

    private final /* synthetic */ void $m$1(Object arg0) {
        DumpAlarmManager.dumpBigData(((Params) arg0).context, ((Params) arg0).pw, ((Params) arg0).args);
    }

    private final /* synthetic */ void $m$2(Object arg0) {
        DumpAlarmManager.dumpDebugLog(((Params) arg0).context, ((Params) arg0).pw, ((Params) arg0).args);
    }

    private final /* synthetic */ void $m$3(Object arg0) {
        DumpAlarmManager.setDebugSwitch(((Params) arg0).context, ((Params) arg0).pw, ((Params) arg0).args);
    }

    private final /* synthetic */ void $m$4(Object arg0) {
        AwareWakeUpManager.getInstance().dumpParam(((Params) arg0).pw);
    }

    private /* synthetic */ -$Lambda$4teM88sZW-FwSj5FV1nxgG8dyag(byte b) {
        this.$id = b;
    }

    public final void accept(Object obj) {
        switch (this.$id) {
            case (byte) 0:
                $m$0(obj);
                return;
            case (byte) 1:
                $m$1(obj);
                return;
            case (byte) 2:
                $m$2(obj);
                return;
            case (byte) 3:
                $m$3(obj);
                return;
            case (byte) 4:
                $m$4(obj);
                return;
            default:
                throw new AssertionError();
        }
    }
}
