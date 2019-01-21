package android.hardware.radio;

import android.os.Parcel;
import java.util.Objects;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Utils$CpgxAbBJVMfl2IUCmgGvKDeq9_U implements Consumer {
    private final /* synthetic */ Parcel f$0;

    public /* synthetic */ -$$Lambda$Utils$CpgxAbBJVMfl2IUCmgGvKDeq9_U(Parcel parcel) {
        this.f$0 = parcel;
    }

    public final void accept(Object obj) {
        this.f$0.writeInt(((Integer) Objects.requireNonNull((Integer) obj)).intValue());
    }
}
