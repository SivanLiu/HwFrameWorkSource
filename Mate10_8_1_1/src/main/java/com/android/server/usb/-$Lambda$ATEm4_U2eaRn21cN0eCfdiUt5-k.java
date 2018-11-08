package com.android.server.usb;

final /* synthetic */ class -$Lambda$ATEm4_U2eaRn21cN0eCfdiUt5-k implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ void $m$0() {
        ((UsbHostManager) this.-$f0).-com_android_server_usb_UsbHostManager-mthref-0();
    }

    private final /* synthetic */ void $m$1() {
        ((UsbProfileGroupSettingsManager) this.-$f0).lambda$-com_android_server_usb_UsbProfileGroupSettingsManager_31409();
    }

    public /* synthetic */ -$Lambda$ATEm4_U2eaRn21cN0eCfdiUt5-k(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
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
