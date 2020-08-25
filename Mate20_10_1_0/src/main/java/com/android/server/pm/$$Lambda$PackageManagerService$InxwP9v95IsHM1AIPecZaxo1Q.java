package com.android.server.pm;

import java.io.File;
import java.io.FilenameFilter;

/* renamed from: com.android.server.pm.-$$Lambda$PackageManagerService$InxwP9v95IsHM1A-IPecZaxo-1Q  reason: invalid class name */
/* compiled from: lambda */
public final /* synthetic */ class $$Lambda$PackageManagerService$InxwP9v95IsHM1AIPecZaxo1Q implements FilenameFilter {
    public static final /* synthetic */ $$Lambda$PackageManagerService$InxwP9v95IsHM1AIPecZaxo1Q INSTANCE = new $$Lambda$PackageManagerService$InxwP9v95IsHM1AIPecZaxo1Q();

    private /* synthetic */ $$Lambda$PackageManagerService$InxwP9v95IsHM1AIPecZaxo1Q() {
    }

    public final boolean accept(File file, String str) {
        return PackageManagerService.lambda$deleteTempPackageFiles$16(file, str);
    }
}
