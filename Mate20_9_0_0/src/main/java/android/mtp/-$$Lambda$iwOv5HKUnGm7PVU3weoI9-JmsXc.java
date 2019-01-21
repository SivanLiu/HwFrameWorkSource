package android.mtp;

import android.mtp.MtpStorageManager.MtpObject;
import java.util.function.ToIntFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$iwOv5HKUnGm7PVU3weoI9-JmsXc implements ToIntFunction {
    public static final /* synthetic */ -$$Lambda$iwOv5HKUnGm7PVU3weoI9-JmsXc INSTANCE = new -$$Lambda$iwOv5HKUnGm7PVU3weoI9-JmsXc();

    private /* synthetic */ -$$Lambda$iwOv5HKUnGm7PVU3weoI9-JmsXc() {
    }

    public final int applyAsInt(Object obj) {
        return ((MtpObject) obj).getId();
    }
}
