package com.android.server.soundtrigger;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SoundTriggerService$RemoteSoundTriggerDetectionService$wfDlqQ7aPvu9qZCZ24jJu4tfUMY implements Consumer {
    public static final /* synthetic */ -$$Lambda$SoundTriggerService$RemoteSoundTriggerDetectionService$wfDlqQ7aPvu9qZCZ24jJu4tfUMY INSTANCE = new -$$Lambda$SoundTriggerService$RemoteSoundTriggerDetectionService$wfDlqQ7aPvu9qZCZ24jJu4tfUMY();

    private /* synthetic */ -$$Lambda$SoundTriggerService$RemoteSoundTriggerDetectionService$wfDlqQ7aPvu9qZCZ24jJu4tfUMY() {
    }

    public final void accept(Object obj) {
        ((RemoteSoundTriggerDetectionService) obj).stopAllPendingOperations();
    }
}
