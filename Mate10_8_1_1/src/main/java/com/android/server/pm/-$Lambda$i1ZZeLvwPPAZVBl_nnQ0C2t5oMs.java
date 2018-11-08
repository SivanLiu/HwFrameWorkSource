package com.android.server.pm;

import java.io.File;
import java.util.List;

final /* synthetic */ class -$Lambda$i1ZZeLvwPPAZVBl_nnQ0C2t5oMs implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ int -$f0;
    private final /* synthetic */ Object -$f1;
    private final /* synthetic */ Object -$f2;

    private final /* synthetic */ void $m$0() {
        ((MyPackageMonitor) this.-$f1).lambda$-com_android_server_pm_LauncherAppsService$LauncherAppsImpl$MyPackageMonitor_39373((String) this.-$f2, this.-$f0);
    }

    private final /* synthetic */ void $m$1() {
        ((PackageManagerService) this.-$f1).lambda$-com_android_server_pm_PackageManagerService_164333((List) this.-$f2, this.-$f0);
    }

    private final /* synthetic */ void $m$2() {
        ((ParallelPackageParser) this.-$f1).lambda$-com_android_server_pm_ParallelPackageParser_3701((File) this.-$f2, this.-$f0);
    }

    private final /* synthetic */ void $m$3() {
        ((ShortcutService) this.-$f1).lambda$-com_android_server_pm_ShortcutService_55668(this.-$f0, (String) this.-$f2);
    }

    public /* synthetic */ -$Lambda$i1ZZeLvwPPAZVBl_nnQ0C2t5oMs(byte b, int i, Object obj, Object obj2) {
        this.$id = b;
        this.-$f0 = i;
        this.-$f1 = obj;
        this.-$f2 = obj2;
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
