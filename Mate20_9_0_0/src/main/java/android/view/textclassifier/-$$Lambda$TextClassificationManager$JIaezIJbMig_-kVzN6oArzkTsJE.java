package android.view.textclassifier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TextClassificationManager$JIaezIJbMig_-kVzN6oArzkTsJE implements TextClassificationSessionFactory {
    private final /* synthetic */ TextClassificationManager f$0;

    public /* synthetic */ -$$Lambda$TextClassificationManager$JIaezIJbMig_-kVzN6oArzkTsJE(TextClassificationManager textClassificationManager) {
        this.f$0 = textClassificationManager;
    }

    public final TextClassifier createTextClassificationSession(TextClassificationContext textClassificationContext) {
        return new TextClassificationSession(textClassificationContext, this.f$0.getTextClassifier());
    }
}
