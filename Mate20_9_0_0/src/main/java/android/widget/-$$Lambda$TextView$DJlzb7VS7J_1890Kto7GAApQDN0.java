package android.widget;

import android.view.textclassifier.TextClassification.Request;
import java.util.function.Supplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TextView$DJlzb7VS7J_1890Kto7GAApQDN0 implements Supplier {
    private final /* synthetic */ TextView f$0;
    private final /* synthetic */ Request f$1;

    public /* synthetic */ -$$Lambda$TextView$DJlzb7VS7J_1890Kto7GAApQDN0(TextView textView, Request request) {
        this.f$0 = textView;
        this.f$1 = request;
    }

    public final Object get() {
        return this.f$0.getTextClassifier().classifyText(this.f$1);
    }
}
