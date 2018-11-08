package android.view.textclassifier;

import android.util.KeyValueListParser;
import android.util.Slog;

public final class TextClassifierConstants {
    static final TextClassifierConstants DEFAULT = new TextClassifierConstants();
    private static final String LOG_TAG = "TextClassifierConstants";
    private static final String SMART_SELECTION_DARK_LAUNCH = "smart_selection_dark_launch";
    private static final boolean SMART_SELECTION_DARK_LAUNCH_DEFAULT = false;
    private static final String SMART_SELECTION_ENABLED_FOR_EDIT_TEXT = "smart_selection_enabled_for_edit_text";
    private static final boolean SMART_SELECTION_ENABLED_FOR_EDIT_TEXT_DEFAULT = true;
    private final boolean mDarkLaunch;
    private final boolean mSuggestSelectionEnabledForEditableText;

    private TextClassifierConstants() {
        this.mDarkLaunch = false;
        this.mSuggestSelectionEnabledForEditableText = true;
    }

    private TextClassifierConstants(String settings) {
        KeyValueListParser parser = new KeyValueListParser(',');
        try {
            parser.setString(settings);
        } catch (IllegalArgumentException e) {
            Slog.e(LOG_TAG, "Bad TextClassifier settings: " + settings);
        }
        this.mDarkLaunch = parser.getBoolean(SMART_SELECTION_DARK_LAUNCH, false);
        this.mSuggestSelectionEnabledForEditableText = parser.getBoolean(SMART_SELECTION_ENABLED_FOR_EDIT_TEXT, true);
    }

    static TextClassifierConstants loadFromString(String settings) {
        return new TextClassifierConstants(settings);
    }

    public boolean isDarkLaunch() {
        return this.mDarkLaunch;
    }

    public boolean isSuggestSelectionEnabledForEditableText() {
        return this.mSuggestSelectionEnabledForEditableText;
    }
}
