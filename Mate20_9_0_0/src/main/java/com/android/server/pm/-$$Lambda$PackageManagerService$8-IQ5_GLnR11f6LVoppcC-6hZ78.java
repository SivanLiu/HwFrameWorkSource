package com.android.server.pm;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PackageManagerService$8-IQ5_GLnR11f6LVoppcC-6hZ78 implements Runnable {
    private final /* synthetic */ PackageManagerService f$0;
    private final /* synthetic */ int[] f$1;
    private final /* synthetic */ String f$2;
    private final /* synthetic */ boolean f$3;

    public /* synthetic */ -$$Lambda$PackageManagerService$8-IQ5_GLnR11f6LVoppcC-6hZ78(PackageManagerService packageManagerService, int[] iArr, String str, boolean z) {
        this.f$0 = packageManagerService;
        this.f$1 = iArr;
        this.f$2 = str;
        this.f$3 = z;
    }

    public final void run() {
        PackageManagerService.lambda$sendPackageAddedForNewUsers$5(this.f$0, this.f$1, this.f$2, this.f$3);
    }
}
