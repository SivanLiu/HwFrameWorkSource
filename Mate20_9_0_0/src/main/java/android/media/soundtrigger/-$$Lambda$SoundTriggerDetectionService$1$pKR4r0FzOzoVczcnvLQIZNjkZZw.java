package android.media.soundtrigger;

import android.os.Bundle;
import com.android.internal.util.function.TriConsumer;
import java.util.UUID;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SoundTriggerDetectionService$1$pKR4r0FzOzoVczcnvLQIZNjkZZw implements TriConsumer {
    public static final /* synthetic */ -$$Lambda$SoundTriggerDetectionService$1$pKR4r0FzOzoVczcnvLQIZNjkZZw INSTANCE = new -$$Lambda$SoundTriggerDetectionService$1$pKR4r0FzOzoVczcnvLQIZNjkZZw();

    private /* synthetic */ -$$Lambda$SoundTriggerDetectionService$1$pKR4r0FzOzoVczcnvLQIZNjkZZw() {
    }

    public final void accept(Object obj, Object obj2, Object obj3) {
        ((SoundTriggerDetectionService) ((SoundTriggerDetectionService) obj)).removeClient((UUID) obj2, (Bundle) obj3);
    }
}
