package android.text.util;

import android.text.Spannable;
import android.view.textclassifier.TextLinks;
import android.view.textclassifier.TextLinksParams;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Linkify$ZGgxzuK-YqBkZXo_7HE4xwOLsh0 implements Consumer {
    private final /* synthetic */ Consumer f$0;
    private final /* synthetic */ Spannable f$1;
    private final /* synthetic */ CharSequence f$2;
    private final /* synthetic */ TextLinksParams f$3;
    private final /* synthetic */ Runnable f$4;

    public /* synthetic */ -$$Lambda$Linkify$ZGgxzuK-YqBkZXo_7HE4xwOLsh0(Consumer consumer, Spannable spannable, CharSequence charSequence, TextLinksParams textLinksParams, Runnable runnable) {
        this.f$0 = consumer;
        this.f$1 = spannable;
        this.f$2 = charSequence;
        this.f$3 = textLinksParams;
        this.f$4 = runnable;
    }

    public final void accept(Object obj) {
        Linkify.lambda$addLinksAsync$2(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, (TextLinks) obj);
    }
}
