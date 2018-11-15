package android.widget;

import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Handler;
import android.os.LocaleList;
import android.provider.Telephony.BaseMmsColumns;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextSelection;
import android.view.textclassifier.logging.SmartSelectionEventTracker;
import android.view.textclassifier.logging.SmartSelectionEventTracker.SelectionEvent;
import android.widget.-$Lambda$tTszxdFZ0V9nXhnBpPsqeBMO0fw.AnonymousClass1;
import com.android.internal.R;
import com.android.internal.util.Preconditions;
import java.text.BreakIterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

final class SelectionActionModeHelper {
    private static final String LOG_TAG = "SelectActionModeHelper";
    private final Editor mEditor;
    private final SelectionTracker mSelectionTracker = new SelectionTracker(this.mTextView);
    private TextClassification mTextClassification;
    private AsyncTask mTextClassificationAsyncTask;
    private final TextClassificationHelper mTextClassificationHelper = new TextClassificationHelper(this.mTextView.getTextClassifier(), getText(this.mTextView), 0, 1, this.mTextView.getTextLocales());
    private final TextView mTextView = this.mEditor.getTextView();

    private static final class SelectionMetricsLogger {
        private static final String LOG_TAG = "SelectionMetricsLogger";
        private static final Pattern PATTERN_WHITESPACE = Pattern.compile("\\s+");
        private final SmartSelectionEventTracker mDelegate;
        private final boolean mEditTextLogger;
        private int mStartIndex;
        private String mText;
        private final BreakIterator mWordIterator;

        SelectionMetricsLogger(TextView textView) {
            int widgetType;
            Preconditions.checkNotNull(textView);
            if (textView.isTextEditable()) {
                widgetType = 3;
            } else {
                widgetType = 1;
            }
            this.mDelegate = new SmartSelectionEventTracker(textView.getContext(), widgetType);
            this.mEditTextLogger = textView.isTextEditable();
            this.mWordIterator = BreakIterator.getWordInstance(textView.getTextLocale());
        }

        public void logSelectionStarted(CharSequence text, int index) {
            try {
                Preconditions.checkNotNull(text);
                Preconditions.checkArgumentInRange(index, 0, text.length(), "index");
                if (this.mText == null || (this.mText.contentEquals(text) ^ 1) != 0) {
                    this.mText = text.toString();
                }
                this.mWordIterator.setText(this.mText);
                this.mStartIndex = index;
                this.mDelegate.logEvent(SelectionEvent.selectionStarted(0));
            } catch (Exception e) {
                Log.d(LOG_TAG, e.getMessage());
            }
        }

        public void logSelectionModified(int start, int end, TextClassification classification, TextSelection selection) {
            try {
                Preconditions.checkArgumentInRange(start, 0, this.mText.length(), BaseMmsColumns.START);
                Preconditions.checkArgumentInRange(end, start, this.mText.length(), "end");
                int[] wordIndices = getWordDelta(start, end);
                if (selection != null) {
                    this.mDelegate.logEvent(SelectionEvent.selectionModified(wordIndices[0], wordIndices[1], selection));
                } else if (classification != null) {
                    this.mDelegate.logEvent(SelectionEvent.selectionModified(wordIndices[0], wordIndices[1], classification));
                } else {
                    this.mDelegate.logEvent(SelectionEvent.selectionModified(wordIndices[0], wordIndices[1]));
                }
            } catch (Exception e) {
                Log.d(LOG_TAG, e.getMessage());
            }
        }

        public void logSelectionAction(int start, int end, int action, TextClassification classification) {
            try {
                Preconditions.checkArgumentInRange(start, 0, this.mText.length(), BaseMmsColumns.START);
                Preconditions.checkArgumentInRange(end, start, this.mText.length(), "end");
                int[] wordIndices = getWordDelta(start, end);
                if (classification != null) {
                    this.mDelegate.logEvent(SelectionEvent.selectionAction(wordIndices[0], wordIndices[1], action, classification));
                } else {
                    this.mDelegate.logEvent(SelectionEvent.selectionAction(wordIndices[0], wordIndices[1], action));
                }
            } catch (Exception e) {
                Log.d(LOG_TAG, e.getMessage());
            }
        }

        public boolean isEditTextLogger() {
            return this.mEditTextLogger;
        }

        private int[] getWordDelta(int start, int end) {
            int[] wordIndices = new int[2];
            if (start == this.mStartIndex) {
                wordIndices[0] = 0;
            } else if (start < this.mStartIndex) {
                wordIndices[0] = -countWordsForward(start);
            } else {
                wordIndices[0] = countWordsBackward(start);
                if (!(this.mWordIterator.isBoundary(start) || (isWhitespace(this.mWordIterator.preceding(start), this.mWordIterator.following(start)) ^ 1) == 0)) {
                    wordIndices[0] = wordIndices[0] - 1;
                }
            }
            if (end == this.mStartIndex) {
                wordIndices[1] = 0;
            } else if (end < this.mStartIndex) {
                wordIndices[1] = -countWordsForward(end);
            } else {
                wordIndices[1] = countWordsBackward(end);
            }
            return wordIndices;
        }

        private int countWordsBackward(int from) {
            Preconditions.checkArgument(from >= this.mStartIndex);
            int wordCount = 0;
            int offset = from;
            while (offset > this.mStartIndex) {
                int start = this.mWordIterator.preceding(offset);
                if (!isWhitespace(start, offset)) {
                    wordCount++;
                }
                offset = start;
            }
            return wordCount;
        }

        private int countWordsForward(int from) {
            Preconditions.checkArgument(from <= this.mStartIndex);
            int wordCount = 0;
            int offset = from;
            while (offset < this.mStartIndex) {
                int end = this.mWordIterator.following(offset);
                if (!isWhitespace(offset, end)) {
                    wordCount++;
                }
                offset = end;
            }
            return wordCount;
        }

        private boolean isWhitespace(int start, int end) {
            return PATTERN_WHITESPACE.matcher(this.mText.substring(start, end)).matches();
        }
    }

    private static final class SelectionResult {
        private final TextClassification mClassification;
        private final int mEnd;
        private final TextSelection mSelection;
        private final int mStart;

        SelectionResult(int start, int end, TextClassification classification, TextSelection selection) {
            this.mStart = start;
            this.mEnd = end;
            this.mClassification = (TextClassification) Preconditions.checkNotNull(classification);
            this.mSelection = selection;
        }
    }

    private static final class SelectionTracker {
        private boolean mAllowReset;
        private final LogAbandonRunnable mDelayedLogAbandon = new LogAbandonRunnable();
        private SelectionMetricsLogger mLogger;
        private int mOriginalEnd;
        private int mOriginalStart;
        private int mSelectionEnd;
        private int mSelectionStart;
        private final TextView mTextView;

        private final class LogAbandonRunnable implements Runnable {
            private boolean mIsPending;

            private LogAbandonRunnable() {
            }

            void schedule(int delayMillis) {
                if (this.mIsPending) {
                    Log.e(SelectionActionModeHelper.LOG_TAG, "Force flushing abandon due to new scheduling request");
                    flush();
                }
                this.mIsPending = true;
                SelectionTracker.this.mTextView.postDelayed(this, (long) delayMillis);
            }

            void flush() {
                SelectionTracker.this.mTextView.removeCallbacks(this);
                run();
            }

            public void run() {
                if (this.mIsPending) {
                    SelectionTracker.this.mLogger.logSelectionAction(SelectionTracker.this.mSelectionStart, SelectionTracker.this.mSelectionEnd, 107, null);
                    SelectionTracker.this.mSelectionStart = SelectionTracker.this.mSelectionEnd = -1;
                    this.mIsPending = false;
                }
            }
        }

        SelectionTracker(TextView textView) {
            this.mTextView = (TextView) Preconditions.checkNotNull(textView);
            this.mLogger = new SelectionMetricsLogger(textView);
        }

        public void onOriginalSelection(CharSequence text, int selectionStart, int selectionEnd) {
            this.mDelayedLogAbandon.flush();
            this.mSelectionStart = selectionStart;
            this.mOriginalStart = selectionStart;
            this.mSelectionEnd = selectionEnd;
            this.mOriginalEnd = selectionEnd;
            this.mAllowReset = false;
            maybeInvalidateLogger();
            this.mLogger.logSelectionStarted(text, selectionStart);
        }

        public void onSmartSelection(SelectionResult result) {
            boolean z = true;
            if (isSelectionStarted()) {
                this.mSelectionStart = result.mStart;
                this.mSelectionEnd = result.mEnd;
                if (this.mSelectionStart == this.mOriginalStart && this.mSelectionEnd == this.mOriginalEnd) {
                    z = false;
                }
                this.mAllowReset = z;
                this.mLogger.logSelectionModified(result.mStart, result.mEnd, result.mClassification, result.mSelection);
            }
        }

        public void onSelectionUpdated(int selectionStart, int selectionEnd, TextClassification classification) {
            if (isSelectionStarted()) {
                this.mSelectionStart = selectionStart;
                this.mSelectionEnd = selectionEnd;
                this.mAllowReset = false;
                this.mLogger.logSelectionModified(selectionStart, selectionEnd, classification, null);
            }
        }

        public void onSelectionDestroyed() {
            this.mAllowReset = false;
            this.mDelayedLogAbandon.schedule(100);
        }

        public void onSelectionAction(int selectionStart, int selectionEnd, int action, TextClassification classification) {
            if (isSelectionStarted()) {
                this.mAllowReset = false;
                this.mLogger.logSelectionAction(selectionStart, selectionEnd, action, classification);
            }
        }

        public boolean resetSelection(int textIndex, Editor editor) {
            TextView textView = editor.getTextView();
            if (!isSelectionStarted() || !this.mAllowReset || textIndex < this.mSelectionStart || textIndex > this.mSelectionEnd || !(SelectionActionModeHelper.getText(textView) instanceof Spannable)) {
                return false;
            }
            this.mAllowReset = false;
            boolean selected = editor.selectCurrentWord();
            if (selected) {
                this.mSelectionStart = editor.getTextView().getSelectionStart();
                this.mSelectionEnd = editor.getTextView().getSelectionEnd();
                this.mLogger.logSelectionAction(textView.getSelectionStart(), textView.getSelectionEnd(), 201, null);
            }
            return selected;
        }

        public void onTextChanged(int start, int end, TextClassification classification) {
            if (isSelectionStarted() && start == this.mSelectionStart && end == this.mSelectionEnd) {
                onSelectionAction(start, end, 100, classification);
            }
        }

        private void maybeInvalidateLogger() {
            if (this.mLogger.isEditTextLogger() != this.mTextView.isTextEditable()) {
                this.mLogger = new SelectionMetricsLogger(this.mTextView);
            }
        }

        private boolean isSelectionStarted() {
            return this.mSelectionStart >= 0 && this.mSelectionEnd >= 0 && this.mSelectionStart != this.mSelectionEnd;
        }
    }

    private static final class TextClassificationAsyncTask extends AsyncTask<Void, Void, SelectionResult> {
        private final String mOriginalText;
        private final Consumer<SelectionResult> mSelectionResultCallback;
        private final Supplier<SelectionResult> mSelectionResultSupplier;
        private final TextView mTextView;
        private final long mTimeOutDuration;

        TextClassificationAsyncTask(TextView textView, long timeOut, Supplier<SelectionResult> selectionResultSupplier, Consumer<SelectionResult> selectionResultCallback) {
            Handler handler = null;
            if (textView != null) {
                handler = textView.getHandler();
            }
            super(handler);
            this.mTextView = (TextView) Preconditions.checkNotNull(textView);
            this.mTimeOutDuration = timeOut;
            this.mSelectionResultSupplier = (Supplier) Preconditions.checkNotNull(selectionResultSupplier);
            this.mSelectionResultCallback = (Consumer) Preconditions.checkNotNull(selectionResultCallback);
            this.mOriginalText = SelectionActionModeHelper.getText(this.mTextView).toString();
        }

        /* synthetic */ void -android_widget_SelectionActionModeHelper$TextClassificationAsyncTask-mthref-0() {
            onTimeOut();
        }

        protected SelectionResult doInBackground(Void... params) {
            Runnable onTimeOut = new -$Lambda$2f4l12BcqlVIiuw8w0ONZMWiEpk((byte) 6, this);
            this.mTextView.postDelayed(onTimeOut, this.mTimeOutDuration);
            SelectionResult result = (SelectionResult) this.mSelectionResultSupplier.get();
            this.mTextView.removeCallbacks(onTimeOut);
            return result;
        }

        protected void onPostExecute(SelectionResult result) {
            Object result2;
            if (!TextUtils.equals(this.mOriginalText, SelectionActionModeHelper.getText(this.mTextView))) {
                result2 = null;
            }
            this.mSelectionResultCallback.accept(result2);
        }

        private void onTimeOut() {
            if (getStatus() == Status.RUNNING) {
                onPostExecute(null);
            }
            cancel(true);
        }
    }

    private static final class TextClassificationHelper {
        private static final int TRIM_DELTA = 120;
        private boolean mHot;
        private LocaleList mLastClassificationLocales;
        private SelectionResult mLastClassificationResult;
        private int mLastClassificationSelectionEnd;
        private int mLastClassificationSelectionStart;
        private CharSequence mLastClassificationText;
        private LocaleList mLocales;
        private int mRelativeEnd;
        private int mRelativeStart;
        private int mSelectionEnd;
        private int mSelectionStart;
        private String mText;
        private TextClassifier mTextClassifier;
        private int mTrimStart;
        private CharSequence mTrimmedText;

        TextClassificationHelper(TextClassifier textClassifier, CharSequence text, int selectionStart, int selectionEnd, LocaleList locales) {
            init(textClassifier, text, selectionStart, selectionEnd, locales);
        }

        public void init(TextClassifier textClassifier, CharSequence text, int selectionStart, int selectionEnd, LocaleList locales) {
            this.mTextClassifier = (TextClassifier) Preconditions.checkNotNull(textClassifier);
            this.mText = ((CharSequence) Preconditions.checkNotNull(text)).toString();
            this.mLastClassificationText = null;
            Preconditions.checkArgument(selectionEnd > selectionStart);
            this.mSelectionStart = selectionStart;
            this.mSelectionEnd = selectionEnd;
            this.mLocales = locales;
        }

        public SelectionResult classifyText() {
            this.mHot = true;
            return performClassification(null);
        }

        public SelectionResult suggestSelection() {
            this.mHot = true;
            trimText();
            TextSelection selection = this.mTextClassifier.suggestSelection(this.mTrimmedText, this.mRelativeStart, this.mRelativeEnd, this.mLocales);
            if (!this.mTextClassifier.getSettings().isDarkLaunch()) {
                this.mSelectionStart = Math.max(0, selection.getSelectionStartIndex() + this.mTrimStart);
                this.mSelectionEnd = Math.min(this.mText.length(), selection.getSelectionEndIndex() + this.mTrimStart);
            }
            return performClassification(selection);
        }

        public long getTimeoutDuration() {
            if (this.mHot) {
                return 200;
            }
            return 500;
        }

        private SelectionResult performClassification(TextSelection selection) {
            if (Objects.equals(this.mText, this.mLastClassificationText) && this.mSelectionStart == this.mLastClassificationSelectionStart && this.mSelectionEnd == this.mLastClassificationSelectionEnd) {
                if ((Objects.equals(this.mLocales, this.mLastClassificationLocales) ^ 1) != 0) {
                }
                return this.mLastClassificationResult;
            }
            this.mLastClassificationText = this.mText;
            this.mLastClassificationSelectionStart = this.mSelectionStart;
            this.mLastClassificationSelectionEnd = this.mSelectionEnd;
            this.mLastClassificationLocales = this.mLocales;
            trimText();
            this.mLastClassificationResult = new SelectionResult(this.mSelectionStart, this.mSelectionEnd, this.mTextClassifier.classifyText(this.mTrimmedText, this.mRelativeStart, this.mRelativeEnd, this.mLocales), selection);
            return this.mLastClassificationResult;
        }

        private void trimText() {
            this.mTrimStart = Math.max(0, this.mSelectionStart - 120);
            this.mTrimmedText = this.mText.subSequence(this.mTrimStart, Math.min(this.mText.length(), this.mSelectionEnd + 120));
            this.mRelativeStart = this.mSelectionStart - this.mTrimStart;
            this.mRelativeEnd = this.mSelectionEnd - this.mTrimStart;
        }
    }

    SelectionActionModeHelper(Editor editor) {
        this.mEditor = (Editor) Preconditions.checkNotNull(editor);
    }

    public void startActionModeAsync(boolean adjustSelection) {
        int isSuggestSelectionEnabledForEditableText;
        if (this.mTextView.isTextEditable()) {
            isSuggestSelectionEnabledForEditableText = this.mTextView.getTextClassifier().getSettings().isSuggestSelectionEnabledForEditableText();
        } else {
            isSuggestSelectionEnabledForEditableText = 1;
        }
        adjustSelection &= isSuggestSelectionEnabledForEditableText;
        this.mSelectionTracker.onOriginalSelection(getText(this.mTextView), this.mTextView.getSelectionStart(), this.mTextView.getSelectionEnd());
        cancelAsyncTask();
        if (skipTextClassification()) {
            startActionMode(null);
            return;
        }
        Supplier anonymousClass1;
        resetTextClassificationHelper();
        TextView textView = this.mTextView;
        long timeoutDuration = this.mTextClassificationHelper.getTimeoutDuration();
        TextClassificationHelper textClassificationHelper;
        if (adjustSelection) {
            textClassificationHelper = this.mTextClassificationHelper;
            textClassificationHelper.getClass();
            anonymousClass1 = new AnonymousClass1((byte) 1, textClassificationHelper);
        } else {
            textClassificationHelper = this.mTextClassificationHelper;
            textClassificationHelper.getClass();
            anonymousClass1 = new AnonymousClass1((byte) 2, textClassificationHelper);
        }
        this.mTextClassificationAsyncTask = new TextClassificationAsyncTask(textView, timeoutDuration, anonymousClass1, new -$Lambda$tTszxdFZ0V9nXhnBpPsqeBMO0fw((byte) 1, this)).execute(new Void[0]);
    }

    /* synthetic */ void -android_widget_SelectionActionModeHelper-mthref-2(SelectionResult selectionResult) {
        startActionMode(selectionResult);
    }

    public void startActionMode() {
        startActionMode(null);
    }

    public void invalidateActionModeAsync() {
        cancelAsyncTask();
        if (skipTextClassification()) {
            invalidateActionMode(null);
            return;
        }
        resetTextClassificationHelper();
        TextView textView = this.mTextView;
        long timeoutDuration = this.mTextClassificationHelper.getTimeoutDuration();
        TextClassificationHelper textClassificationHelper = this.mTextClassificationHelper;
        textClassificationHelper.getClass();
        this.mTextClassificationAsyncTask = new TextClassificationAsyncTask(textView, timeoutDuration, new AnonymousClass1((byte) 0, textClassificationHelper), new -$Lambda$tTszxdFZ0V9nXhnBpPsqeBMO0fw((byte) 0, this)).execute(new Void[0]);
    }

    /* synthetic */ void -android_widget_SelectionActionModeHelper-mthref-4(SelectionResult selectionResult) {
        invalidateActionMode(selectionResult);
    }

    public void onSelectionAction(int menuItemId) {
        this.mSelectionTracker.onSelectionAction(this.mTextView.getSelectionStart(), this.mTextView.getSelectionEnd(), getActionType(menuItemId), this.mTextClassification);
    }

    public void onSelectionDrag() {
        this.mSelectionTracker.onSelectionAction(this.mTextView.getSelectionStart(), this.mTextView.getSelectionEnd(), 106, this.mTextClassification);
    }

    public void onTextChanged(int start, int end) {
        this.mSelectionTracker.onTextChanged(start, end, this.mTextClassification);
    }

    public boolean resetSelection(int textIndex) {
        if (!this.mSelectionTracker.resetSelection(textIndex, this.mEditor)) {
            return false;
        }
        invalidateActionModeAsync();
        return true;
    }

    public TextClassification getTextClassification() {
        return this.mTextClassification;
    }

    public void onDestroyActionMode() {
        this.mSelectionTracker.onSelectionDestroyed();
        cancelAsyncTask();
    }

    private void cancelAsyncTask() {
        if (this.mTextClassificationAsyncTask != null) {
            this.mTextClassificationAsyncTask.cancel(true);
            this.mTextClassificationAsyncTask = null;
        }
        this.mTextClassification = null;
    }

    private boolean skipTextClassification() {
        boolean password;
        boolean noOpTextClassifier = this.mTextView.getTextClassifier() == TextClassifier.NO_OP;
        boolean noSelection = this.mTextView.getSelectionEnd() == this.mTextView.getSelectionStart();
        if (this.mTextView.hasPasswordTransformationMethod()) {
            password = true;
        } else {
            password = TextView.isPasswordInputType(this.mTextView.getInputType());
        }
        if (noOpTextClassifier || noSelection) {
            return true;
        }
        return password;
    }

    private void startActionMode(SelectionResult result) {
        CharSequence text = getText(this.mTextView);
        if (result == null || !(text instanceof Spannable)) {
            this.mTextClassification = null;
        } else {
            if (!this.mTextView.getTextClassifier().getSettings().isDarkLaunch()) {
                Selection.setSelection((Spannable) text, result.mStart, result.mEnd);
            }
            this.mTextClassification = result.mClassification;
        }
        if (this.mEditor.startSelectionActionModeInternal()) {
            SelectionModifierCursorController controller = this.mEditor.getSelectionController();
            if (controller != null) {
                controller.show();
            }
            if (result != null) {
                this.mSelectionTracker.onSmartSelection(result);
            }
        }
        this.mEditor.setRestartActionModeOnNextRefresh(false);
        this.mTextClassificationAsyncTask = null;
    }

    private void invalidateActionMode(SelectionResult result) {
        TextClassification -get0;
        if (result != null) {
            -get0 = result.mClassification;
        } else {
            -get0 = null;
        }
        this.mTextClassification = -get0;
        ActionMode actionMode = this.mEditor.getTextActionMode();
        if (actionMode != null) {
            actionMode.invalidate();
        }
        this.mSelectionTracker.onSelectionUpdated(this.mTextView.getSelectionStart(), this.mTextView.getSelectionEnd(), this.mTextClassification);
        this.mTextClassificationAsyncTask = null;
    }

    private void resetTextClassificationHelper() {
        this.mTextClassificationHelper.init(this.mTextView.getTextClassifier(), getText(this.mTextView), this.mTextView.getSelectionStart(), this.mTextView.getSelectionEnd(), this.mTextView.getTextLocales());
    }

    private static int getActionType(int menuItemId) {
        switch (menuItemId) {
            case R.id.selectAll /*16908319*/:
                return 200;
            case R.id.cut /*16908320*/:
                return 103;
            case R.id.copy /*16908321*/:
                return 101;
            case R.id.paste /*16908322*/:
            case R.id.pasteAsPlainText /*16908337*/:
                return 102;
            case R.id.shareText /*16908341*/:
                return 104;
            case R.id.textAssist /*16908353*/:
                return 105;
            default:
                return 108;
        }
    }

    private static CharSequence getText(TextView textView) {
        CharSequence text = textView.getText();
        if (text != null) {
            return text;
        }
        return "";
    }
}
