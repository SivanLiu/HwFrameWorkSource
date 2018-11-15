package com.android.server.soundtrigger;

import android.hardware.soundtrigger.SoundTrigger.GenericRecognitionEvent;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SoundTriggerService$RemoteSoundTriggerDetectionService$pFqiq_C9KJsoa_HQOdj7lmMixsI implements Runnable {
    private final /* synthetic */ RemoteSoundTriggerDetectionService f$0;
    private final /* synthetic */ GenericRecognitionEvent f$1;

    public /* synthetic */ -$$Lambda$SoundTriggerService$RemoteSoundTriggerDetectionService$pFqiq_C9KJsoa_HQOdj7lmMixsI(RemoteSoundTriggerDetectionService remoteSoundTriggerDetectionService, GenericRecognitionEvent genericRecognitionEvent) {
        this.f$0 = remoteSoundTriggerDetectionService;
        this.f$1 = genericRecognitionEvent;
    }

    public final void run() {
        RemoteSoundTriggerDetectionService.lambda$onGenericSoundTriggerDetected$2(this.f$0, this.f$1);
    }
}
