package android.media.soundtrigger;

import android.hardware.soundtrigger.SoundTrigger.GenericRecognitionEvent;
import android.os.Bundle;
import com.android.internal.util.function.QuintConsumer;
import java.util.UUID;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ISQYIYPBRBIOLBUJy7rrJW-SiJg implements QuintConsumer {
    public static final /* synthetic */ -$$Lambda$ISQYIYPBRBIOLBUJy7rrJW-SiJg INSTANCE = new -$$Lambda$ISQYIYPBRBIOLBUJy7rrJW-SiJg();

    private /* synthetic */ -$$Lambda$ISQYIYPBRBIOLBUJy7rrJW-SiJg() {
    }

    public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
        ((SoundTriggerDetectionService) obj).onGenericRecognitionEvent((UUID) obj2, (Bundle) obj3, ((Integer) obj4).intValue(), (GenericRecognitionEvent) obj5);
    }
}
