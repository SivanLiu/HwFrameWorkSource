package android.media.session;

import android.media.session.MediaSessionManager.SessionTokensChangedWrapper.AnonymousClass1;
import java.util.List;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MediaSessionManager$SessionTokensChangedWrapper$1$wkYv3P0_Sdm0wRGnCFHp-AGf3Dw implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;
    private final /* synthetic */ List f$1;

    public /* synthetic */ -$$Lambda$MediaSessionManager$SessionTokensChangedWrapper$1$wkYv3P0_Sdm0wRGnCFHp-AGf3Dw(AnonymousClass1 anonymousClass1, List list) {
        this.f$0 = anonymousClass1;
        this.f$1 = list;
    }

    public final void run() {
        AnonymousClass1.lambda$onSessionTokensChanged$0(this.f$0, this.f$1);
    }
}
