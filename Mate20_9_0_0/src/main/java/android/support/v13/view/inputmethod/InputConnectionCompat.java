package android.support.v13.view.inputmethod;

import android.content.ClipDescription;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.InputContentInfo;

public final class InputConnectionCompat {
    private static final String COMMIT_CONTENT_ACTION = "android.support.v13.view.inputmethod.InputConnectionCompat.COMMIT_CONTENT";
    private static final String COMMIT_CONTENT_CONTENT_URI_KEY = "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_URI";
    private static final String COMMIT_CONTENT_DESCRIPTION_KEY = "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_DESCRIPTION";
    private static final String COMMIT_CONTENT_FLAGS_KEY = "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_FLAGS";
    private static final String COMMIT_CONTENT_LINK_URI_KEY = "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_LINK_URI";
    private static final String COMMIT_CONTENT_OPTS_KEY = "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_OPTS";
    private static final String COMMIT_CONTENT_RESULT_RECEIVER = "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_RESULT_RECEIVER";
    public static final int INPUT_CONTENT_GRANT_READ_URI_PERMISSION = 1;

    public interface OnCommitContentListener {
        boolean onCommitContent(InputContentInfoCompat inputContentInfoCompat, int i, Bundle bundle);
    }

    static boolean handlePerformPrivateCommand(@Nullable String action, @NonNull Bundle data, @NonNull OnCommitContentListener onCommitContentListener) {
        int i = 0;
        if (!TextUtils.equals(COMMIT_CONTENT_ACTION, action) || data == null) {
            return false;
        }
        ResultReceiver resultReceiver = null;
        boolean result = false;
        try {
            resultReceiver = (ResultReceiver) data.getParcelable(COMMIT_CONTENT_RESULT_RECEIVER);
            Uri contentUri = (Uri) data.getParcelable(COMMIT_CONTENT_CONTENT_URI_KEY);
            ClipDescription description = (ClipDescription) data.getParcelable(COMMIT_CONTENT_DESCRIPTION_KEY);
            Uri linkUri = (Uri) data.getParcelable(COMMIT_CONTENT_LINK_URI_KEY);
            result = onCommitContentListener.onCommitContent(new InputContentInfoCompat(contentUri, description, linkUri), data.getInt(COMMIT_CONTENT_FLAGS_KEY), (Bundle) data.getParcelable(COMMIT_CONTENT_OPTS_KEY));
            return result;
        } finally {
            if (resultReceiver != null) {
                if (result) {
                    i = 1;
                }
                resultReceiver.send(i, null);
            }
        }
    }

    public static boolean commitContent(@NonNull InputConnection inputConnection, @NonNull EditorInfo editorInfo, @NonNull InputContentInfoCompat inputContentInfo, int flags, @Nullable Bundle opts) {
        ClipDescription description = inputContentInfo.getDescription();
        boolean supported = false;
        for (String mimeType : EditorInfoCompat.getContentMimeTypes(editorInfo)) {
            if (description.hasMimeType(mimeType)) {
                supported = true;
                break;
            }
        }
        if (!supported) {
            return false;
        }
        if (VERSION.SDK_INT >= 25) {
            return inputConnection.commitContent((InputContentInfo) inputContentInfo.unwrap(), flags, opts);
        }
        Bundle params = new Bundle();
        params.putParcelable(COMMIT_CONTENT_CONTENT_URI_KEY, inputContentInfo.getContentUri());
        params.putParcelable(COMMIT_CONTENT_DESCRIPTION_KEY, inputContentInfo.getDescription());
        params.putParcelable(COMMIT_CONTENT_LINK_URI_KEY, inputContentInfo.getLinkUri());
        params.putInt(COMMIT_CONTENT_FLAGS_KEY, flags);
        params.putParcelable(COMMIT_CONTENT_OPTS_KEY, opts);
        return inputConnection.performPrivateCommand(COMMIT_CONTENT_ACTION, params);
    }

    @NonNull
    public static InputConnection createWrapper(@NonNull InputConnection inputConnection, @NonNull EditorInfo editorInfo, @NonNull OnCommitContentListener onCommitContentListener) {
        if (inputConnection == null) {
            throw new IllegalArgumentException("inputConnection must be non-null");
        } else if (editorInfo == null) {
            throw new IllegalArgumentException("editorInfo must be non-null");
        } else if (onCommitContentListener == null) {
            throw new IllegalArgumentException("onCommitContentListener must be non-null");
        } else if (VERSION.SDK_INT >= 25) {
            final OnCommitContentListener listener = onCommitContentListener;
            return new InputConnectionWrapper(inputConnection, false) {
                public boolean commitContent(InputContentInfo inputContentInfo, int flags, Bundle opts) {
                    if (listener.onCommitContent(InputContentInfoCompat.wrap(inputContentInfo), flags, opts)) {
                        return true;
                    }
                    return super.commitContent(inputContentInfo, flags, opts);
                }
            };
        } else if (EditorInfoCompat.getContentMimeTypes(editorInfo).length == 0) {
            return inputConnection;
        } else {
            final OnCommitContentListener listener2 = onCommitContentListener;
            return new InputConnectionWrapper(inputConnection, false) {
                public boolean performPrivateCommand(String action, Bundle data) {
                    if (InputConnectionCompat.handlePerformPrivateCommand(action, data, listener2)) {
                        return true;
                    }
                    return super.performPrivateCommand(action, data);
                }
            };
        }
    }
}
