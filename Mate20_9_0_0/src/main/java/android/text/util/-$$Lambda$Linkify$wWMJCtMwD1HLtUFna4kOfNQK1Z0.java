package android.text.util;

import android.text.Spannable;
import android.widget.TextView;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Linkify$wWMJCtMwD1HLtUFna4kOfNQK1Z0 implements Runnable {
    private final /* synthetic */ TextView f$0;
    private final /* synthetic */ Spannable f$1;
    private final /* synthetic */ CharSequence f$2;

    public /* synthetic */ -$$Lambda$Linkify$wWMJCtMwD1HLtUFna4kOfNQK1Z0(TextView textView, Spannable spannable, CharSequence charSequence) {
        this.f$0 = textView;
        this.f$1 = spannable;
        this.f$2 = charSequence;
    }

    public final void run() {
        Linkify.lambda$addLinksAsync$0(this.f$0, this.f$1, this.f$2);
    }
}
