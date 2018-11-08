package com.android.server.pm;

import android.content.pm.PackageParser.Package;
import android.content.pm.ShortcutInfo;
import java.util.Comparator;

final /* synthetic */ class -$Lambda$tZuhGcRRWSq5m9LlSrypurdt-0w implements Comparator {
    public static final /* synthetic */ -$Lambda$tZuhGcRRWSq5m9LlSrypurdt-0w $INST$0 = new -$Lambda$tZuhGcRRWSq5m9LlSrypurdt-0w((byte) 0);
    public static final /* synthetic */ -$Lambda$tZuhGcRRWSq5m9LlSrypurdt-0w $INST$1 = new -$Lambda$tZuhGcRRWSq5m9LlSrypurdt-0w((byte) 1);
    public static final /* synthetic */ -$Lambda$tZuhGcRRWSq5m9LlSrypurdt-0w $INST$2 = new -$Lambda$tZuhGcRRWSq5m9LlSrypurdt-0w((byte) 2);
    public static final /* synthetic */ -$Lambda$tZuhGcRRWSq5m9LlSrypurdt-0w $INST$3 = new -$Lambda$tZuhGcRRWSq5m9LlSrypurdt-0w((byte) 3);
    public static final /* synthetic */ -$Lambda$tZuhGcRRWSq5m9LlSrypurdt-0w $INST$4 = new -$Lambda$tZuhGcRRWSq5m9LlSrypurdt-0w((byte) 4);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ int $m$0(Object arg0, Object arg1) {
        return Long.compare(((Package) arg0).getLatestForegroundPackageUseTimeInMills(), ((Package) arg1).getLatestForegroundPackageUseTimeInMills());
    }

    private final /* synthetic */ int $m$1(Object arg0, Object arg1) {
        return Long.compare(((Package) arg1).getLatestForegroundPackageUseTimeInMills(), ((Package) arg0).getLatestForegroundPackageUseTimeInMills());
    }

    private final /* synthetic */ int $m$2(Object arg0, Object arg1) {
        return ShortcutPackage.lambda$-com_android_server_pm_ShortcutPackage_34997((ShortcutInfo) arg0, (ShortcutInfo) arg1);
    }

    private final /* synthetic */ int $m$3(Object arg0, Object arg1) {
        return ShortcutPackage.lambda$-com_android_server_pm_ShortcutPackage_42045((ShortcutInfo) arg0, (ShortcutInfo) arg1);
    }

    private final /* synthetic */ int $m$4(Object arg0, Object arg1) {
        return Integer.compare(((ShortcutInfo) arg0).getRank(), ((ShortcutInfo) arg1).getRank());
    }

    private /* synthetic */ -$Lambda$tZuhGcRRWSq5m9LlSrypurdt-0w(byte b) {
        this.$id = b;
    }

    public final int compare(Object obj, Object obj2) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(obj, obj2);
            case (byte) 1:
                return $m$1(obj, obj2);
            case (byte) 2:
                return $m$2(obj, obj2);
            case (byte) 3:
                return $m$3(obj, obj2);
            case (byte) 4:
                return $m$4(obj, obj2);
            default:
                throw new AssertionError();
        }
    }
}
