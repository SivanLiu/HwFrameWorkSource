package android.mtp;

import android.mtp.MtpStorageManager.MtpObject;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MtpStorageManager$DVPwWM5hkC_B_cgO9AF8IKzObmQ implements Predicate {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$MtpStorageManager$DVPwWM5hkC_B_cgO9AF8IKzObmQ(int i) {
        this.f$0 = i;
    }

    public final boolean test(Object obj) {
        return MtpStorageManager.lambda$getObjects$1(this.f$0, (MtpObject) obj);
    }
}
