package android.telecom.Logging;

import android.telecom.Logging.EventManager.Event;
import android.util.Pair;
import java.util.function.ToLongFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EventManager$weOtitr8e1cZeiy1aDSqzNoKaY8 implements ToLongFunction {
    public static final /* synthetic */ -$$Lambda$EventManager$weOtitr8e1cZeiy1aDSqzNoKaY8 INSTANCE = new -$$Lambda$EventManager$weOtitr8e1cZeiy1aDSqzNoKaY8();

    private /* synthetic */ -$$Lambda$EventManager$weOtitr8e1cZeiy1aDSqzNoKaY8() {
    }

    public final long applyAsLong(Object obj) {
        return ((Event) ((Pair) obj).second).time;
    }
}
