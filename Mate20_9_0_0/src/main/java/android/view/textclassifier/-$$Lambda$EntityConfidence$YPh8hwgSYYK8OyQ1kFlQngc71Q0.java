package android.view.textclassifier;

import java.util.Comparator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EntityConfidence$YPh8hwgSYYK8OyQ1kFlQngc71Q0 implements Comparator {
    private final /* synthetic */ EntityConfidence f$0;

    public /* synthetic */ -$$Lambda$EntityConfidence$YPh8hwgSYYK8OyQ1kFlQngc71Q0(EntityConfidence entityConfidence) {
        this.f$0 = entityConfidence;
    }

    public final int compare(Object obj, Object obj2) {
        return Float.compare(((Float) this.f$0.mEntityConfidence.get((String) obj2)).floatValue(), ((Float) this.f$0.mEntityConfidence.get((String) obj)).floatValue());
    }
}
