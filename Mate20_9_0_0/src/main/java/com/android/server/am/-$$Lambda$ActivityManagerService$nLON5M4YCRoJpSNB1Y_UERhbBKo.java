package com.android.server.am;

import android.app.PictureInPictureParams;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ActivityManagerService$nLON5M4YCRoJpSNB1Y_UERhbBKo implements Runnable {
    private final /* synthetic */ ActivityManagerService f$0;
    private final /* synthetic */ ActivityRecord f$1;
    private final /* synthetic */ PictureInPictureParams f$2;

    public /* synthetic */ -$$Lambda$ActivityManagerService$nLON5M4YCRoJpSNB1Y_UERhbBKo(ActivityManagerService activityManagerService, ActivityRecord activityRecord, PictureInPictureParams pictureInPictureParams) {
        this.f$0 = activityManagerService;
        this.f$1 = activityRecord;
        this.f$2 = pictureInPictureParams;
    }

    public final void run() {
        ActivityManagerService.lambda$enterPictureInPictureMode$1(this.f$0, this.f$1, this.f$2);
    }
}
