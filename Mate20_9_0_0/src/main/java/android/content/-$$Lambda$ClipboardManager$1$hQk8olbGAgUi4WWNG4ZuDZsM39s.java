package android.content;

import android.content.ClipboardManager.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ClipboardManager$1$hQk8olbGAgUi4WWNG4ZuDZsM39s implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;

    public /* synthetic */ -$$Lambda$ClipboardManager$1$hQk8olbGAgUi4WWNG4ZuDZsM39s(AnonymousClass1 anonymousClass1) {
        this.f$0 = anonymousClass1;
    }

    public final void run() {
        ClipboardManager.this.reportPrimaryClipChanged();
    }
}
