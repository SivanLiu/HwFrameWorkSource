package com.android.server.am;

final /* synthetic */ class -$Lambda$-Oweh9FamEB8lkqsrsDtt1mh9YE implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ int -$f0;
    private final /* synthetic */ int -$f1;
    private final /* synthetic */ int -$f2;
    private final /* synthetic */ int -$f3;
    private final /* synthetic */ int -$f4;
    private final /* synthetic */ int -$f5;
    private final /* synthetic */ int -$f6;
    private final /* synthetic */ int -$f7;
    private final /* synthetic */ Object -$f8;

    private final /* synthetic */ void $m$0() {
        ((BatteryStatsService) this.-$f8).lambda$-com_android_server_am_BatteryStatsService_42096(this.-$f0, this.-$f1, this.-$f2, this.-$f3, this.-$f4, this.-$f5, this.-$f6, this.-$f7);
    }

    private final /* synthetic */ void $m$1() {
        ((BatteryStatsService) this.-$f8).lambda$-com_android_server_am_BatteryStatsService_40546(this.-$f0, this.-$f1, this.-$f2, this.-$f3, this.-$f4, this.-$f5, this.-$f6, this.-$f7);
    }

    public /* synthetic */ -$Lambda$-Oweh9FamEB8lkqsrsDtt1mh9YE(byte b, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, Object obj) {
        this.$id = b;
        this.-$f0 = i;
        this.-$f1 = i2;
        this.-$f2 = i3;
        this.-$f3 = i4;
        this.-$f4 = i5;
        this.-$f5 = i6;
        this.-$f6 = i7;
        this.-$f7 = i8;
        this.-$f8 = obj;
    }

    public final void run() {
        switch (this.$id) {
            case (byte) 0:
                $m$0();
                return;
            case (byte) 1:
                $m$1();
                return;
            default:
                throw new AssertionError();
        }
    }
}
