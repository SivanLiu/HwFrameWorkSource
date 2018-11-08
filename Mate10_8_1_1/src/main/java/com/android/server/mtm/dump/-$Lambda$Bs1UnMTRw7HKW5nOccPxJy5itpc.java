package com.android.server.mtm.dump;

import java.util.function.Consumer;

final /* synthetic */ class -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc implements Consumer {
    public static final /* synthetic */ -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc $INST$0 = new -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc((byte) 0);
    public static final /* synthetic */ -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc $INST$1 = new -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc((byte) 1);
    public static final /* synthetic */ -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc $INST$10 = new -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc((byte) 10);
    public static final /* synthetic */ -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc $INST$11 = new -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc(BluetoothMessage.NOTIFY_VAL_OFFS);
    public static final /* synthetic */ -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc $INST$12 = new -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc((byte) 12);
    public static final /* synthetic */ -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc $INST$13 = new -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc((byte) 13);
    public static final /* synthetic */ -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc $INST$14 = new -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc((byte) 14);
    public static final /* synthetic */ -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc $INST$15 = new -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc((byte) 15);
    public static final /* synthetic */ -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc $INST$16 = new -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc((byte) 16);
    public static final /* synthetic */ -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc $INST$17 = new -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc((byte) 17);
    public static final /* synthetic */ -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc $INST$2 = new -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc((byte) 2);
    public static final /* synthetic */ -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc $INST$3 = new -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc((byte) 3);
    public static final /* synthetic */ -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc $INST$4 = new -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc((byte) 4);
    public static final /* synthetic */ -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc $INST$5 = new -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc((byte) 5);
    public static final /* synthetic */ -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc $INST$6 = new -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc((byte) 6);
    public static final /* synthetic */ -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc $INST$7 = new -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc((byte) 7);
    public static final /* synthetic */ -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc $INST$8 = new -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc((byte) 8);
    public static final /* synthetic */ -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc $INST$9 = new -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc((byte) 9);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ void $m$0(Object arg0) {
        DumpAppMngClean.clean(((Params) arg0).context, ((Params) arg0).pw, ((Params) arg0).args);
    }

    private final /* synthetic */ void $m$1(Object arg0) {
        DumpAppMngClean.dumpPackage((Params) arg0);
    }

    private final /* synthetic */ void $m$10(Object arg0) {
        DumpAppMngClean.dumpPGClean((Params) arg0);
    }

    private final /* synthetic */ void $m$11(Object arg0) {
        DumpAppMngClean.dumpCrashClean((Params) arg0);
    }

    private final /* synthetic */ void $m$12(Object arg0) {
        DumpAppMngClean.dumpAppStatus((Params) arg0);
    }

    private final /* synthetic */ void $m$13(Object arg0) {
        DumpAppMngClean.dumpDecide((Params) arg0);
    }

    private final /* synthetic */ void $m$14(Object arg0) {
        DumpAppSrMng.dumpDumpRules((Params) arg0);
    }

    private final /* synthetic */ void $m$15(Object arg0) {
        DumpAppSrMng.dumpUpdateRules((Params) arg0);
    }

    private final /* synthetic */ void $m$16(Object arg0) {
        DumpAppSrMng.dumpAppStart((Params) arg0);
    }

    private final /* synthetic */ void $m$17(Object arg0) {
        DumpAppSrMng.dumpList((Params) arg0);
    }

    private final /* synthetic */ void $m$2(Object arg0) {
        DumpAppMngClean.dumpHistory((Params) arg0);
    }

    private final /* synthetic */ void $m$3(Object arg0) {
        DumpAppMngClean.help((Params) arg0);
    }

    private final /* synthetic */ void $m$4(Object arg0) {
        DumpAppMngClean.dumpBigData((Params) arg0);
    }

    private final /* synthetic */ void $m$5(Object arg0) {
        DumpAppMngClean.dumpAppType((Params) arg0);
    }

    private final /* synthetic */ void $m$6(Object arg0) {
        DumpAppMngClean.dumpPackageList((Params) arg0);
    }

    private final /* synthetic */ void $m$7(Object arg0) {
        DumpAppMngClean.dumpTask((Params) arg0);
    }

    private final /* synthetic */ void $m$8(Object arg0) {
        DumpAppMngClean.dumpSMClean((Params) arg0);
    }

    private final /* synthetic */ void $m$9(Object arg0) {
        DumpAppMngClean.getSMCleanList((Params) arg0);
    }

    private /* synthetic */ -$Lambda$Bs1UnMTRw7HKW5nOccPxJy5itpc(byte b) {
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
            case (byte) 5:
                $m$5(obj);
                return;
            case (byte) 6:
                $m$6(obj);
                return;
            case (byte) 7:
                $m$7(obj);
                return;
            case (byte) 8:
                $m$8(obj);
                return;
            case (byte) 9:
                $m$9(obj);
                return;
            case (byte) 10:
                $m$10(obj);
                return;
            case (byte) 11:
                $m$11(obj);
                return;
            case (byte) 12:
                $m$12(obj);
                return;
            case (byte) 13:
                $m$13(obj);
                return;
            case (byte) 14:
                $m$14(obj);
                return;
            case (byte) 15:
                $m$15(obj);
                return;
            case (byte) 16:
                $m$16(obj);
                return;
            case (byte) 17:
                $m$17(obj);
                return;
            default:
                throw new AssertionError();
        }
    }
}
