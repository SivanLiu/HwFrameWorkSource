package com.android.server.pm;

import java.util.concurrent.CountDownLatch;

final /* synthetic */ class -$Lambda$iCTRLJcHnavjRcatPDKSIvElD0U implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ void $m$0() {
        ((Installer) this.-$f0).lambda$-com_android_server_pm_Installer_4931();
    }

    private final /* synthetic */ void $m$1() {
        ((ShortcutBitmapSaver) this.-$f0).lambda$-com_android_server_pm_ShortcutBitmapSaver_7645();
    }

    private final /* synthetic */ void $m$2() {
        ((CountDownLatch) this.-$f0).countDown();
    }

    private final /* synthetic */ void $m$3() {
        ((ShortcutService) this.-$f0).-com_android_server_pm_ShortcutService-mthref-0();
    }

    public /* synthetic */ -$Lambda$iCTRLJcHnavjRcatPDKSIvElD0U(byte b, Object obj) {
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
            case (byte) 2:
                $m$2();
                return;
            case (byte) 3:
                $m$3();
                return;
            default:
                throw new AssertionError();
        }
    }
}
