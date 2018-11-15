package com.android.server.soundtrigger;

import android.hardware.soundtrigger.SoundTrigger.GenericRecognitionEvent;
import android.media.soundtrigger.ISoundTriggerDetectionService;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SoundTriggerService$RemoteSoundTriggerDetectionService$F-iA254xzDfAHrQW86c2oSqXfwI implements ExecuteOp {
    private final /* synthetic */ RemoteSoundTriggerDetectionService f$0;
    private final /* synthetic */ GenericRecognitionEvent f$1;

    public /* synthetic */ -$$Lambda$SoundTriggerService$RemoteSoundTriggerDetectionService$F-iA254xzDfAHrQW86c2oSqXfwI(RemoteSoundTriggerDetectionService remoteSoundTriggerDetectionService, GenericRecognitionEvent genericRecognitionEvent) {
        this.f$0 = remoteSoundTriggerDetectionService;
        this.f$1 = genericRecognitionEvent;
    }

    public final void run(int i, ISoundTriggerDetectionService iSoundTriggerDetectionService) {
        iSoundTriggerDetectionService.onGenericRecognitionEvent(this.f$0.mPuuid, i, this.f$1);
    }
}
