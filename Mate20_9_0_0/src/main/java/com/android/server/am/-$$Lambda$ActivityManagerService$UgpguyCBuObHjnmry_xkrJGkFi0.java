package com.android.server.am;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ActivityManagerService$UgpguyCBuObHjnmry_xkrJGkFi0 implements Runnable {
    private final /* synthetic */ ActivityManagerService f$0;
    private final /* synthetic */ ProcessRecord f$1;
    private final /* synthetic */ long f$2;
    private final /* synthetic */ String f$3;
    private final /* synthetic */ String f$4;
    private final /* synthetic */ int[] f$5;
    private final /* synthetic */ int f$6;
    private final /* synthetic */ int f$7;
    private final /* synthetic */ String f$8;
    private final /* synthetic */ String f$9;

    public /* synthetic */ -$$Lambda$ActivityManagerService$UgpguyCBuObHjnmry_xkrJGkFi0(ActivityManagerService activityManagerService, ProcessRecord processRecord, long j, String str, String str2, int[] iArr, int i, int i2, String str3, String str4) {
        this.f$0 = activityManagerService;
        this.f$1 = processRecord;
        this.f$2 = j;
        this.f$3 = str;
        this.f$4 = str2;
        this.f$5 = iArr;
        this.f$6 = i;
        this.f$7 = i2;
        this.f$8 = str3;
        this.f$9 = str4;
    }

    public final void run() {
        ActivityManagerService.lambda$startProcessLocked$0(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9);
    }
}
