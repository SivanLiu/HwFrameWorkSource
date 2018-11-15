package com.android.server.soundtrigger;

import android.media.soundtrigger.ISoundTriggerDetectionService;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SoundTriggerService$RemoteSoundTriggerDetectionService$crQZgbDmIG6q92Mrkm49T2yqrs0 implements ExecuteOp {
    private final /* synthetic */ RemoteSoundTriggerDetectionService f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$SoundTriggerService$RemoteSoundTriggerDetectionService$crQZgbDmIG6q92Mrkm49T2yqrs0(RemoteSoundTriggerDetectionService remoteSoundTriggerDetectionService, int i) {
        this.f$0 = remoteSoundTriggerDetectionService;
        this.f$1 = i;
    }

    public final void run(int i, ISoundTriggerDetectionService iSoundTriggerDetectionService) {
        iSoundTriggerDetectionService.onError(this.f$0.mPuuid, i, this.f$1);
    }
}
