package android.view.textclassifier;

import android.content.Context;
import com.android.internal.util.Preconditions;

public final class TextClassificationManager {
    private final Context mContext;
    private TextClassifier mTextClassifier;
    private final Object mTextClassifierLock = new Object();

    public TextClassificationManager(Context context) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
    }

    public TextClassifier getTextClassifier() {
        TextClassifier textClassifier;
        synchronized (this.mTextClassifierLock) {
            if (this.mTextClassifier == null) {
                this.mTextClassifier = new TextClassifierImpl(this.mContext);
            }
            textClassifier = this.mTextClassifier;
        }
        return textClassifier;
    }

    public void setTextClassifier(TextClassifier textClassifier) {
        synchronized (this.mTextClassifierLock) {
            this.mTextClassifier = textClassifier;
        }
    }
}
