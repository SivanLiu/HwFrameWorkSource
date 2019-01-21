package android.view.textclassifier;

import android.content.Context;
import android.database.ContentObserver;
import android.os.ServiceManager.ServiceNotFoundException;
import android.provider.Settings.Global;
import android.service.textclassifier.TextClassifierService;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import java.lang.ref.WeakReference;

public final class TextClassificationManager {
    private static final String LOG_TAG = "TextClassificationManager";
    private final Context mContext;
    @GuardedBy("mLock")
    private TextClassifier mCustomTextClassifier;
    private final TextClassificationSessionFactory mDefaultSessionFactory = new -$$Lambda$TextClassificationManager$JIaezIJbMig_-kVzN6oArzkTsJE(this);
    @GuardedBy("mLock")
    private TextClassifier mLocalTextClassifier;
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private TextClassificationSessionFactory mSessionFactory;
    @GuardedBy("mLock")
    private TextClassificationConstants mSettings;
    private final SettingsObserver mSettingsObserver;
    @GuardedBy("mLock")
    private TextClassifier mSystemTextClassifier;

    private static final class SettingsObserver extends ContentObserver {
        private final WeakReference<TextClassificationManager> mTcm;

        SettingsObserver(TextClassificationManager tcm) {
            super(null);
            this.mTcm = new WeakReference(tcm);
            tcm.getApplicationContext().getContentResolver().registerContentObserver(Global.getUriFor(Global.TEXT_CLASSIFIER_CONSTANTS), false, this);
        }

        public void onChange(boolean selfChange) {
            TextClassificationManager tcm = (TextClassificationManager) this.mTcm.get();
            if (tcm != null) {
                tcm.invalidate();
            }
        }
    }

    public TextClassificationManager(Context context) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mSessionFactory = this.mDefaultSessionFactory;
        this.mSettingsObserver = new SettingsObserver(this);
    }

    public TextClassifier getTextClassifier() {
        synchronized (this.mLock) {
            TextClassifier textClassifier;
            if (this.mCustomTextClassifier != null) {
                textClassifier = this.mCustomTextClassifier;
                return textClassifier;
            } else if (isSystemTextClassifierEnabled()) {
                textClassifier = getSystemTextClassifier();
                return textClassifier;
            } else {
                textClassifier = getLocalTextClassifier();
                return textClassifier;
            }
        }
    }

    public void setTextClassifier(TextClassifier textClassifier) {
        synchronized (this.mLock) {
            this.mCustomTextClassifier = textClassifier;
        }
    }

    public TextClassifier getTextClassifier(int type) {
        if (type != 0) {
            return getSystemTextClassifier();
        }
        return getLocalTextClassifier();
    }

    private TextClassificationConstants getSettings() {
        TextClassificationConstants textClassificationConstants;
        synchronized (this.mLock) {
            if (this.mSettings == null) {
                this.mSettings = TextClassificationConstants.loadFromString(Global.getString(getApplicationContext().getContentResolver(), Global.TEXT_CLASSIFIER_CONSTANTS));
            }
            textClassificationConstants = this.mSettings;
        }
        return textClassificationConstants;
    }

    public TextClassifier createTextClassificationSession(TextClassificationContext classificationContext) {
        Preconditions.checkNotNull(classificationContext);
        TextClassifier textClassifier = this.mSessionFactory.createTextClassificationSession(classificationContext);
        Preconditions.checkNotNull(textClassifier, "Session Factory should never return null");
        return textClassifier;
    }

    public TextClassifier createTextClassificationSession(TextClassificationContext classificationContext, TextClassifier textClassifier) {
        Preconditions.checkNotNull(classificationContext);
        Preconditions.checkNotNull(textClassifier);
        return new TextClassificationSession(classificationContext, textClassifier);
    }

    public void setTextClassificationSessionFactory(TextClassificationSessionFactory factory) {
        synchronized (this.mLock) {
            if (factory != null) {
                try {
                    this.mSessionFactory = factory;
                } catch (Throwable th) {
                }
            } else {
                this.mSessionFactory = this.mDefaultSessionFactory;
            }
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mSettingsObserver != null) {
                getApplicationContext().getContentResolver().unregisterContentObserver(this.mSettingsObserver);
            }
            super.finalize();
        } catch (Throwable th) {
            super.finalize();
        }
    }

    private TextClassifier getSystemTextClassifier() {
        synchronized (this.mLock) {
            if (this.mSystemTextClassifier == null && isSystemTextClassifierEnabled()) {
                try {
                    this.mSystemTextClassifier = new SystemTextClassifier(this.mContext, getSettings());
                    Log.d(LOG_TAG, "Initialized SystemTextClassifier");
                } catch (ServiceNotFoundException e) {
                    Log.e(LOG_TAG, "Could not initialize SystemTextClassifier", e);
                }
            }
        }
        if (this.mSystemTextClassifier != null) {
            return this.mSystemTextClassifier;
        }
        return TextClassifier.NO_OP;
    }

    private TextClassifier getLocalTextClassifier() {
        TextClassifier textClassifier;
        synchronized (this.mLock) {
            if (this.mLocalTextClassifier == null) {
                if (getSettings().isLocalTextClassifierEnabled()) {
                    this.mLocalTextClassifier = new TextClassifierImpl(this.mContext, getSettings(), TextClassifier.NO_OP);
                } else {
                    Log.d(LOG_TAG, "Local TextClassifier disabled");
                    this.mLocalTextClassifier = TextClassifier.NO_OP;
                }
            }
            textClassifier = this.mLocalTextClassifier;
        }
        return textClassifier;
    }

    private boolean isSystemTextClassifierEnabled() {
        return getSettings().isSystemTextClassifierEnabled() && TextClassifierService.getServiceComponentName(this.mContext) != null;
    }

    private void invalidate() {
        synchronized (this.mLock) {
            this.mSettings = null;
            this.mLocalTextClassifier = null;
            this.mSystemTextClassifier = null;
        }
    }

    Context getApplicationContext() {
        if (this.mContext.getApplicationContext() != null) {
            return this.mContext.getApplicationContext();
        }
        return this.mContext;
    }

    public static TextClassificationConstants getSettings(Context context) {
        Preconditions.checkNotNull(context);
        TextClassificationManager tcm = (TextClassificationManager) context.getSystemService(TextClassificationManager.class);
        if (tcm != null) {
            return tcm.getSettings();
        }
        return TextClassificationConstants.loadFromString(Global.getString(context.getApplicationContext().getContentResolver(), Global.TEXT_CLASSIFIER_CONSTANTS));
    }
}
