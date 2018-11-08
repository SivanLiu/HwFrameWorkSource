package com.android.internal.telephony.ims;

import com.android.internal.telephony.ims.ImsServiceController.RebindRetry;

final /* synthetic */ class -$Lambda$Dp0MKpTfGctn5WSf-VZIVicYMbM implements RebindRetry {
    public static final /* synthetic */ -$Lambda$Dp0MKpTfGctn5WSf-VZIVicYMbM $INST$0 = new -$Lambda$Dp0MKpTfGctn5WSf-VZIVicYMbM((byte) 0);
    public static final /* synthetic */ -$Lambda$Dp0MKpTfGctn5WSf-VZIVicYMbM $INST$1 = new -$Lambda$Dp0MKpTfGctn5WSf-VZIVicYMbM((byte) 1);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ long $m$0() {
        return 5000;
    }

    private final /* synthetic */ long $m$1() {
        return 5000;
    }

    private /* synthetic */ -$Lambda$Dp0MKpTfGctn5WSf-VZIVicYMbM(byte b) {
        this.$id = b;
    }

    public final long getRetryTimeout() {
        switch (this.$id) {
            case (byte) 0:
                return $m$0();
            case (byte) 1:
                return $m$1();
            default:
                throw new AssertionError();
        }
    }
}
