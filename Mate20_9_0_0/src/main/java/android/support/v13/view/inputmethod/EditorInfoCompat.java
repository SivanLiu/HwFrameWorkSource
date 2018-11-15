package android.support.v13.view.inputmethod;

import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.inputmethod.EditorInfo;

public final class EditorInfoCompat {
    private static final String CONTENT_MIME_TYPES_KEY = "android.support.v13.view.inputmethod.EditorInfoCompat.CONTENT_MIME_TYPES";
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    public static final int IME_FLAG_FORCE_ASCII = Integer.MIN_VALUE;
    public static final int IME_FLAG_NO_PERSONALIZED_LEARNING = 16777216;

    public static void setContentMimeTypes(@NonNull EditorInfo editorInfo, @Nullable String[] contentMimeTypes) {
        if (VERSION.SDK_INT >= 25) {
            editorInfo.contentMimeTypes = contentMimeTypes;
            return;
        }
        if (editorInfo.extras == null) {
            editorInfo.extras = new Bundle();
        }
        editorInfo.extras.putStringArray(CONTENT_MIME_TYPES_KEY, contentMimeTypes);
    }

    @NonNull
    public static String[] getContentMimeTypes(EditorInfo editorInfo) {
        String[] result;
        if (VERSION.SDK_INT >= 25) {
            result = editorInfo.contentMimeTypes;
            return result != null ? result : EMPTY_STRING_ARRAY;
        } else if (editorInfo.extras == null) {
            return EMPTY_STRING_ARRAY;
        } else {
            result = editorInfo.extras.getStringArray(CONTENT_MIME_TYPES_KEY);
            return result != null ? result : EMPTY_STRING_ARRAY;
        }
    }
}
