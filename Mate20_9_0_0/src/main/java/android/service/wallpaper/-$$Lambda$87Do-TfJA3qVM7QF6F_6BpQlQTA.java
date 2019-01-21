package android.service.wallpaper;

import android.os.SystemClock;
import java.util.function.Supplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$87Do-TfJA3qVM7QF6F_6BpQlQTA implements Supplier {
    public static final /* synthetic */ -$$Lambda$87Do-TfJA3qVM7QF6F_6BpQlQTA INSTANCE = new -$$Lambda$87Do-TfJA3qVM7QF6F_6BpQlQTA();

    private /* synthetic */ -$$Lambda$87Do-TfJA3qVM7QF6F_6BpQlQTA() {
    }

    public final Object get() {
        return Long.valueOf(SystemClock.elapsedRealtime());
    }
}
