package com.android.server.wifi;

import android.os.IHwBinder.DeathRecipient;

final /* synthetic */ class -$Lambda$lKwQPnSIRFCOpiR3PrChpeYqVrQ implements DeathRecipient {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ void $m$0(long arg0) {
        ((HalDeviceManager) this.-$f0).lambda$-com_android_server_wifi_HalDeviceManager_20938(arg0);
    }

    private final /* synthetic */ void $m$1(long arg0) {
        ((HalDeviceManager) this.-$f0).lambda$-com_android_server_wifi_HalDeviceManager_24266(arg0);
    }

    private final /* synthetic */ void $m$2(long arg0) {
        ((SupplicantStaIfaceHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaIfaceHal_5828(arg0);
    }

    private final /* synthetic */ void $m$3(long arg0) {
        ((SupplicantStaIfaceHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaIfaceHal_6222(arg0);
    }

    public /* synthetic */ -$Lambda$lKwQPnSIRFCOpiR3PrChpeYqVrQ(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
    }

    public final void serviceDied(long j) {
        switch (this.$id) {
            case (byte) 0:
                $m$0(j);
                return;
            case (byte) 1:
                $m$1(j);
                return;
            case (byte) 2:
                $m$2(j);
                return;
            case (byte) 3:
                $m$3(j);
                return;
            default:
                throw new AssertionError();
        }
    }
}
