package android.hardware.radio;

import android.hardware.radio.ProgramList.OnCompleteListener;
import java.util.concurrent.Executor;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ProgramList$aDYMynqVdAUqeKXIxfNtN1u67zs implements OnCompleteListener {
    private final /* synthetic */ Executor f$0;
    private final /* synthetic */ OnCompleteListener f$1;

    public /* synthetic */ -$$Lambda$ProgramList$aDYMynqVdAUqeKXIxfNtN1u67zs(Executor executor, OnCompleteListener onCompleteListener) {
        this.f$0 = executor;
        this.f$1 = onCompleteListener;
    }

    public final void onComplete() {
        ProgramList.lambda$addOnCompleteListener$0(this.f$0, this.f$1);
    }
}
