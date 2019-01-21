package android.text.util;

import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextLinks.Request;
import java.util.function.Supplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Linkify$hPjCKfcU4vqhADicCa9bWKrOoog implements Supplier {
    private final /* synthetic */ TextClassifier f$0;
    private final /* synthetic */ Request f$1;

    public /* synthetic */ -$$Lambda$Linkify$hPjCKfcU4vqhADicCa9bWKrOoog(TextClassifier textClassifier, Request request) {
        this.f$0 = textClassifier;
        this.f$1 = request;
    }

    public final Object get() {
        return this.f$0.generateLinks(this.f$1);
    }
}
