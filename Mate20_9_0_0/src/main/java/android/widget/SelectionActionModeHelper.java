package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.LocaleList;
import android.provider.Telephony.BaseMmsColumns;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.textclassifier.SelectionEvent;
import android.view.textclassifier.SelectionSessionLogger;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassification.Request;
import android.view.textclassifier.TextClassificationConstants;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextSelection;
import android.view.textclassifier.TextSelection.Request.Builder;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.annotations.VisibleForTesting.Visibility;
import com.android.internal.util.Preconditions;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@VisibleForTesting(visibility = Visibility.PACKAGE)
public final class SelectionActionModeHelper {
    private static final String LOG_TAG = "SelectActionModeHelper";
    private final Editor mEditor;
    private final SelectionTracker mSelectionTracker;
    private final SmartSelectSprite mSmartSelectSprite;
    private TextClassification mTextClassification;
    private AsyncTask mTextClassificationAsyncTask;
    private final TextClassificationHelper mTextClassificationHelper;
    private final TextView mTextView = this.mEditor.getTextView();

    private static final class SelectionMetricsLogger {
        private static final String LOG_TAG = "SelectionMetricsLogger";
        private static final Pattern PATTERN_WHITESPACE = Pattern.compile("\\s+");
        private TextClassifier mClassificationSession;
        private final boolean mEditTextLogger;
        private int mStartIndex;
        private String mText;
        private final BreakIterator mTokenIterator;

        SelectionMetricsLogger(TextView textView) {
            Preconditions.checkNotNull(textView);
            this.mEditTextLogger = textView.isTextEditable();
            this.mTokenIterator = SelectionSessionLogger.getTokenIterator(textView.getTextLocale());
        }

        private static String getWidetType(TextView textView) {
            if (textView.isTextEditable()) {
                return TextClassifier.WIDGET_TYPE_EDITTEXT;
            }
            if (textView.isTextSelectable()) {
                return TextClassifier.WIDGET_TYPE_TEXTVIEW;
            }
            return TextClassifier.WIDGET_TYPE_UNSELECTABLE_TEXTVIEW;
        }

        public void logSelectionStarted(TextClassifier classificationSession, CharSequence text, int index, int invocationMethod) {
            try {
                Preconditions.checkNotNull(text);
                Preconditions.checkArgumentInRange(index, 0, text.length(), "index");
                if (this.mText == null || !this.mText.contentEquals(text)) {
                    this.mText = text.toString();
                }
                this.mTokenIterator.setText(this.mText);
                this.mStartIndex = index;
                this.mClassificationSession = classificationSession;
                if (hasActiveClassificationSession()) {
                    this.mClassificationSession.onSelectionEvent(SelectionEvent.createSelectionStartedEvent(invocationMethod, 0));
                }
            } catch (Exception e) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString(), e);
            }
        }

        public void logSelectionModified(int start, int end, TextClassification classification, TextSelection selection) {
            try {
                if (hasActiveClassificationSession()) {
                    Preconditions.checkArgumentInRange(start, 0, this.mText.length(), BaseMmsColumns.START);
                    Preconditions.checkArgumentInRange(end, start, this.mText.length(), "end");
                    int[] wordIndices = getWordDelta(start, end);
                    if (selection != null) {
                        this.mClassificationSession.onSelectionEvent(SelectionEvent.createSelectionModifiedEvent(wordIndices[0], wordIndices[1], selection));
                    } else if (classification != null) {
                        this.mClassificationSession.onSelectionEvent(SelectionEvent.createSelectionModifiedEvent(wordIndices[0], wordIndices[1], classification));
                    } else {
                        this.mClassificationSession.onSelectionEvent(SelectionEvent.createSelectionModifiedEvent(wordIndices[0], wordIndices[1]));
                    }
                }
            } catch (Exception e) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString(), e);
            }
        }

        public void logSelectionAction(int start, int end, int action, TextClassification classification) {
            try {
                if (hasActiveClassificationSession()) {
                    Preconditions.checkArgumentInRange(start, 0, this.mText.length(), BaseMmsColumns.START);
                    Preconditions.checkArgumentInRange(end, start, this.mText.length(), "end");
                    int[] wordIndices = getWordDelta(start, end);
                    if (classification != null) {
                        this.mClassificationSession.onSelectionEvent(SelectionEvent.createSelectionActionEvent(wordIndices[0], wordIndices[1], action, classification));
                    } else {
                        this.mClassificationSession.onSelectionEvent(SelectionEvent.createSelectionActionEvent(wordIndices[0], wordIndices[1], action));
                    }
                    if (SelectionEvent.isTerminal(action)) {
                        endTextClassificationSession();
                    }
                }
            } catch (Exception e) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString(), e);
            }
        }

        public boolean isEditTextLogger() {
            return this.mEditTextLogger;
        }

        public void endTextClassificationSession() {
            if (hasActiveClassificationSession()) {
                this.mClassificationSession.destroy();
            }
        }

        private boolean hasActiveClassificationSession() {
            return (this.mClassificationSession == null || this.mClassificationSession.isDestroyed()) ? false : true;
        }

        private int[] getWordDelta(int start, int end) {
            int[] wordIndices = new int[2];
            if (start == this.mStartIndex) {
                wordIndices[0] = 0;
            } else if (start < this.mStartIndex) {
                wordIndices[0] = -countWordsForward(start);
            } else {
                wordIndices[0] = countWordsBackward(start);
                if (!(this.mTokenIterator.isBoundary(start) || isWhitespace(this.mTokenIterator.preceding(start), this.mTokenIterator.following(start)))) {
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
                int start = this.mTokenIterator.preceding(offset);
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
                int end = this.mTokenIterator.following(offset);
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
            this.mClassification = classification;
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
                    SelectionTracker.this.mLogger.endTextClassificationSession();
                    this.mIsPending = false;
                }
            }
        }

        SelectionTracker(TextView textView) {
            this.mTextView = (TextView) Preconditions.checkNotNull(textView);
            this.mLogger = new SelectionMetricsLogger(textView);
        }

        public void onOriginalSelection(CharSequence text, int selectionStart, int selectionEnd, boolean isLink) {
            this.mDelayedLogAbandon.flush();
            this.mSelectionStart = selectionStart;
            this.mOriginalStart = selectionStart;
            this.mSelectionEnd = selectionEnd;
            this.mOriginalEnd = selectionEnd;
            this.mAllowReset = false;
            maybeInvalidateLogger();
            this.mLogger.logSelectionStarted(this.mTextView.getTextClassificationSession(), text, selectionStart, isLink ? 2 : 1);
        }

        public void onSmartSelection(SelectionResult result) {
            onClassifiedSelection(result);
            this.mLogger.logSelectionModified(result.mStart, result.mEnd, result.mClassification, result.mSelection);
        }

        public void onLinkSelected(SelectionResult result) {
            onClassifiedSelection(result);
        }

        private void onClassifiedSelection(SelectionResult result) {
            if (isSelectionStarted()) {
                this.mSelectionStart = result.mStart;
                this.mSelectionEnd = result.mEnd;
                boolean z = (this.mSelectionStart == this.mOriginalStart && this.mSelectionEnd == this.mOriginalEnd) ? false : true;
                this.mAllowReset = z;
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

    private static final class TextClassificationHelper {
        private static final int TRIM_DELTA = 120;
        private final Context mContext;
        private LocaleList mDefaultLocales;
        private boolean mHot;
        private LocaleList mLastClassificationLocales;
        private SelectionResult mLastClassificationResult;
        private int mLastClassificationSelectionEnd;
        private int mLastClassificationSelectionStart;
        private CharSequence mLastClassificationText;
        private int mRelativeEnd;
        private int mRelativeStart;
        private int mSelectionEnd;
        private int mSelectionStart;
        private String mText;
        private Supplier<TextClassifier> mTextClassifier;
        private int mTrimStart;
        private CharSequence mTrimmedText;

        TextClassificationHelper(Context context, Supplier<TextClassifier> textClassifier, CharSequence text, int selectionStart, int selectionEnd, LocaleList locales) {
            init(textClassifier, text, selectionStart, selectionEnd, locales);
            this.mContext = (Context) Preconditions.checkNotNull(context);
        }

        public void init(Supplier<TextClassifier> textClassifier, CharSequence text, int selectionStart, int selectionEnd, LocaleList locales) {
            this.mTextClassifier = (Supplier) Preconditions.checkNotNull(textClassifier);
            this.mText = ((CharSequence) Preconditions.checkNotNull(text)).toString();
            this.mLastClassificationText = null;
            Preconditions.checkArgument(selectionEnd > selectionStart);
            this.mSelectionStart = selectionStart;
            this.mSelectionEnd = selectionEnd;
            this.mDefaultLocales = locales;
        }

        public SelectionResult classifyText() {
            this.mHot = true;
            return performClassification(null);
        }

        public SelectionResult suggestSelection() {
            TextSelection selection;
            this.mHot = true;
            trimText();
            if (this.mContext.getApplicationInfo().targetSdkVersion >= 28) {
                selection = ((TextClassifier) this.mTextClassifier.get()).suggestSelection(new Builder(this.mTrimmedText, this.mRelativeStart, this.mRelativeEnd).setDefaultLocales(this.mDefaultLocales).setDarkLaunchAllowed(true).build());
            } else {
                selection = ((TextClassifier) this.mTextClassifier.get()).suggestSelection(this.mTrimmedText, this.mRelativeStart, this.mRelativeEnd, this.mDefaultLocales);
            }
            if (!isDarkLaunchEnabled()) {
                this.mSelectionStart = Math.max(0, selection.getSelectionStartIndex() + this.mTrimStart);
                this.mSelectionEnd = Math.min(this.mText.length(), selection.getSelectionEndIndex() + this.mTrimStart);
            }
            return performClassification(selection);
        }

        public SelectionResult getOriginalSelection() {
            return new SelectionResult(this.mSelectionStart, this.mSelectionEnd, null, null);
        }

        public int getTimeoutDuration() {
            if (this.mHot) {
                return 200;
            }
            return 500;
        }

        private boolean isDarkLaunchEnabled() {
            return TextClassificationManager.getSettings(this.mContext).isModelDarkLaunchEnabled();
        }

        private SelectionResult performClassification(TextSelection selection) {
            if (!(Objects.equals(this.mText, this.mLastClassificationText) && this.mSelectionStart == this.mLastClassificationSelectionStart && this.mSelectionEnd == this.mLastClassificationSelectionEnd && Objects.equals(this.mDefaultLocales, this.mLastClassificationLocales))) {
                TextClassification classification;
                this.mLastClassificationText = this.mText;
                this.mLastClassificationSelectionStart = this.mSelectionStart;
                this.mLastClassificationSelectionEnd = this.mSelectionEnd;
                this.mLastClassificationLocales = this.mDefaultLocales;
                trimText();
                if (this.mContext.getApplicationInfo().targetSdkVersion >= 28) {
                    classification = ((TextClassifier) this.mTextClassifier.get()).classifyText(new Request.Builder(this.mTrimmedText, this.mRelativeStart, this.mRelativeEnd).setDefaultLocales(this.mDefaultLocales).build());
                } else {
                    classification = ((TextClassifier) this.mTextClassifier.get()).classifyText(this.mTrimmedText, this.mRelativeStart, this.mRelativeEnd, this.mDefaultLocales);
                }
                this.mLastClassificationResult = new SelectionResult(this.mSelectionStart, this.mSelectionEnd, classification, selection);
            }
            return this.mLastClassificationResult;
        }

        private void trimText() {
            this.mTrimStart = Math.max(0, this.mSelectionStart - 120);
            this.mTrimmedText = this.mText.subSequence(this.mTrimStart, Math.min(this.mText.length(), this.mSelectionEnd + 120));
            this.mRelativeStart = this.mSelectionStart - this.mTrimStart;
            this.mRelativeEnd = this.mSelectionEnd - this.mTrimStart;
        }
    }

    private static final class TextClassificationAsyncTask extends AsyncTask<Void, Void, SelectionResult> {
        private final String mOriginalText;
        private final Consumer<SelectionResult> mSelectionResultCallback;
        private final Supplier<SelectionResult> mSelectionResultSupplier;
        private final TextView mTextView;
        private final int mTimeOutDuration;
        private final Supplier<SelectionResult> mTimeOutResultSupplier;

        TextClassificationAsyncTask(TextView textView, int timeOut, Supplier<SelectionResult> selectionResultSupplier, Consumer<SelectionResult> selectionResultCallback, Supplier<SelectionResult> timeOutResultSupplier) {
            super(textView != null ? textView.getHandler() : null);
            this.mTextView = (TextView) Preconditions.checkNotNull(textView);
            this.mTimeOutDuration = timeOut;
            this.mSelectionResultSupplier = (Supplier) Preconditions.checkNotNull(selectionResultSupplier);
            this.mSelectionResultCallback = (Consumer) Preconditions.checkNotNull(selectionResultCallback);
            this.mTimeOutResultSupplier = (Supplier) Preconditions.checkNotNull(timeOutResultSupplier);
            this.mOriginalText = SelectionActionModeHelper.getText(this.mTextView).toString();
        }

        protected SelectionResult doInBackground(Void... params) {
            Runnable onTimeOut = new -$$Lambda$SelectionActionModeHelper$TextClassificationAsyncTask$D5tkmK-caFBtl9ux2L0aUfUee4E(this);
            this.mTextView.postDelayed(onTimeOut, (long) this.mTimeOutDuration);
            SelectionResult result = (SelectionResult) this.mSelectionResultSupplier.get();
            this.mTextView.removeCallbacks(onTimeOut);
            return result;
        }

        protected void onPostExecute(SelectionResult result) {
            this.mSelectionResultCallback.accept(TextUtils.equals(this.mOriginalText, SelectionActionModeHelper.getText(this.mTextView)) ? result : null);
        }

        private void onTimeOut() {
            if (getStatus() == Status.RUNNING) {
                onPostExecute((SelectionResult) this.mTimeOutResultSupplier.get());
            }
            cancel(true);
        }
    }

    SelectionActionModeHelper(Editor editor) {
        this.mEditor = (Editor) Preconditions.checkNotNull(editor);
        Context context = this.mTextView.getContext();
        TextView textView = this.mTextView;
        Objects.requireNonNull(textView);
        this.mTextClassificationHelper = new TextClassificationHelper(context, new -$$Lambda$yIdmBO6ZxaY03PGN08RySVVQXuE(textView), getText(this.mTextView), 0, 1, this.mTextView.getTextLocales());
        this.mSelectionTracker = new SelectionTracker(this.mTextView);
        if (getTextClassificationSettings().isSmartSelectionAnimationEnabled()) {
            Context context2 = this.mTextView.getContext();
            int i = editor.getTextView().mHighlightColor;
            TextView textView2 = this.mTextView;
            Objects.requireNonNull(textView2);
            this.mSmartSelectSprite = new SmartSelectSprite(context2, i, new -$$Lambda$IfzAW5fP9thoftErKAjo9SLZufw(textView2));
            return;
        }
        this.mSmartSelectSprite = null;
    }

    public void startSelectionActionModeAsync(boolean adjustSelection) {
        int isSmartSelectionEnabled = adjustSelection & getTextClassificationSettings().isSmartSelectionEnabled();
        this.mSelectionTracker.onOriginalSelection(getText(this.mTextView), this.mTextView.getSelectionStart(), this.mTextView.getSelectionEnd(), false);
        cancelAsyncTask();
        if (skipTextClassification()) {
            startSelectionActionMode(null);
            return;
        }
        TextClassificationHelper textClassificationHelper;
        -$$Lambda$E-XesXLNXm7BCuVAnjZcIGfnQJQ -__lambda_e-xesxlnxm7bcuvanjzcigfnqjq;
        -$$Lambda$SelectionActionModeHelper$l1f1_V5lw6noQxI_3u11qF753Iw -__lambda_selectionactionmodehelper_l1f1_v5lw6noqxi_3u11qf753iw;
        resetTextClassificationHelper();
        TextView textView = this.mTextView;
        int timeoutDuration = this.mTextClassificationHelper.getTimeoutDuration();
        if (isSmartSelectionEnabled != 0) {
            textClassificationHelper = this.mTextClassificationHelper;
            Objects.requireNonNull(textClassificationHelper);
            -__lambda_e-xesxlnxm7bcuvanjzcigfnqjq = new -$$Lambda$E-XesXLNXm7BCuVAnjZcIGfnQJQ(textClassificationHelper);
        } else {
            textClassificationHelper = this.mTextClassificationHelper;
            Objects.requireNonNull(textClassificationHelper);
            -__lambda_e-xesxlnxm7bcuvanjzcigfnqjq = new -$$Lambda$aOGBsMC_jnvTDjezYLRtz35nAPI(textClassificationHelper);
        }
        Supplier supplier = -__lambda_e-xesxlnxm7bcuvanjzcigfnqjq;
        if (this.mSmartSelectSprite != null) {
            -__lambda_selectionactionmodehelper_l1f1_v5lw6noqxi_3u11qf753iw = new -$$Lambda$SelectionActionModeHelper$l1f1_V5lw6noQxI_3u11qF753Iw(this);
        } else {
            -__lambda_selectionactionmodehelper_l1f1_v5lw6noqxi_3u11qf753iw = new -$$Lambda$SelectionActionModeHelper$CcJ0IF8nDFsmkuaqvOxFqYGazzY(this);
        }
        Consumer consumer = -__lambda_selectionactionmodehelper_l1f1_v5lw6noqxi_3u11qf753iw;
        textClassificationHelper = this.mTextClassificationHelper;
        Objects.requireNonNull(textClassificationHelper);
        this.mTextClassificationAsyncTask = new TextClassificationAsyncTask(textView, timeoutDuration, supplier, consumer, new -$$Lambda$etfJkiCJnT2dqM2O4M2TCm9i_oA(textClassificationHelper)).execute((Object[]) new Void[0]);
    }

    public void startLinkActionModeAsync(int start, int end) {
        this.mSelectionTracker.onOriginalSelection(getText(this.mTextView), start, end, true);
        cancelAsyncTask();
        if (skipTextClassification()) {
            startLinkActionMode(null);
            return;
        }
        resetTextClassificationHelper(start, end);
        TextView textView = this.mTextView;
        int timeoutDuration = this.mTextClassificationHelper.getTimeoutDuration();
        TextClassificationHelper textClassificationHelper = this.mTextClassificationHelper;
        Objects.requireNonNull(textClassificationHelper);
        -$$Lambda$aOGBsMC_jnvTDjezYLRtz35nAPI -__lambda_aogbsmc_jnvtdjezylrtz35napi = new -$$Lambda$aOGBsMC_jnvTDjezYLRtz35nAPI(textClassificationHelper);
        -$$Lambda$SelectionActionModeHelper$WnFw1_gP20c3ltvTN6OPqQ5XUns -__lambda_selectionactionmodehelper_wnfw1_gp20c3ltvtn6opqq5xuns = new -$$Lambda$SelectionActionModeHelper$WnFw1_gP20c3ltvTN6OPqQ5XUns(this);
        textClassificationHelper = this.mTextClassificationHelper;
        Objects.requireNonNull(textClassificationHelper);
        this.mTextClassificationAsyncTask = new TextClassificationAsyncTask(textView, timeoutDuration, -__lambda_aogbsmc_jnvtdjezylrtz35napi, -__lambda_selectionactionmodehelper_wnfw1_gp20c3ltvtn6opqq5xuns, new -$$Lambda$etfJkiCJnT2dqM2O4M2TCm9i_oA(textClassificationHelper)).execute((Object[]) new Void[0]);
    }

    public void startActionMode() {
        startActionMode(0, null);
    }

    public void invalidateActionModeAsync() {
        cancelAsyncTask();
        if (skipTextClassification()) {
            invalidateActionMode(null);
            return;
        }
        resetTextClassificationHelper();
        TextView textView = this.mTextView;
        int timeoutDuration = this.mTextClassificationHelper.getTimeoutDuration();
        TextClassificationHelper textClassificationHelper = this.mTextClassificationHelper;
        Objects.requireNonNull(textClassificationHelper);
        -$$Lambda$aOGBsMC_jnvTDjezYLRtz35nAPI -__lambda_aogbsmc_jnvtdjezylrtz35napi = new -$$Lambda$aOGBsMC_jnvTDjezYLRtz35nAPI(textClassificationHelper);
        -$$Lambda$SelectionActionModeHelper$Lwzg10CkEpNBaAXBpjnWEpIlTzQ -__lambda_selectionactionmodehelper_lwzg10ckepnbaaxbpjnwepiltzq = new -$$Lambda$SelectionActionModeHelper$Lwzg10CkEpNBaAXBpjnWEpIlTzQ(this);
        textClassificationHelper = this.mTextClassificationHelper;
        Objects.requireNonNull(textClassificationHelper);
        this.mTextClassificationAsyncTask = new TextClassificationAsyncTask(textView, timeoutDuration, -__lambda_aogbsmc_jnvtdjezylrtz35napi, -__lambda_selectionactionmodehelper_lwzg10ckepnbaaxbpjnwepiltzq, new -$$Lambda$etfJkiCJnT2dqM2O4M2TCm9i_oA(textClassificationHelper)).execute((Object[]) new Void[0]);
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
        cancelSmartSelectAnimation();
        this.mSelectionTracker.onSelectionDestroyed();
        cancelAsyncTask();
    }

    public void onDraw(Canvas canvas) {
        if (isDrawingHighlight() && this.mSmartSelectSprite != null) {
            this.mSmartSelectSprite.draw(canvas);
        }
    }

    public boolean isDrawingHighlight() {
        return this.mSmartSelectSprite != null && this.mSmartSelectSprite.isAnimationActive();
    }

    private TextClassificationConstants getTextClassificationSettings() {
        return TextClassificationManager.getSettings(this.mTextView.getContext());
    }

    private void cancelAsyncTask() {
        if (this.mTextClassificationAsyncTask != null) {
            this.mTextClassificationAsyncTask.cancel(true);
            this.mTextClassificationAsyncTask = null;
        }
        this.mTextClassification = null;
    }

    private boolean skipTextClassification() {
        boolean noOpTextClassifier = this.mTextView.usesNoOpTextClassifier();
        boolean noSelection = this.mTextView.getSelectionEnd() == this.mTextView.getSelectionStart();
        boolean password = this.mTextView.hasPasswordTransformationMethod() || TextView.isPasswordInputType(this.mTextView.getInputType());
        if (noOpTextClassifier || noSelection || password) {
            return true;
        }
        return false;
    }

    private void startLinkActionMode(SelectionResult result) {
        startActionMode(2, result);
    }

    private void startSelectionActionMode(SelectionResult result) {
        startActionMode(0, result);
    }

    private void startActionMode(@TextActionMode int actionMode, SelectionResult result) {
        CharSequence text = getText(this.mTextView);
        if (result != null && (text instanceof Spannable) && (this.mTextView.isTextSelectable() || this.mTextView.isTextEditable())) {
            if (!getTextClassificationSettings().isModelDarkLaunchEnabled()) {
                Selection.setSelection((Spannable) text, result.mStart, result.mEnd);
                this.mTextView.invalidate();
            }
            this.mTextClassification = result.mClassification;
        } else if (result == null || actionMode != 2) {
            this.mTextClassification = null;
        } else {
            this.mTextClassification = result.mClassification;
        }
        if (this.mEditor.startActionModeInternal(actionMode)) {
            SelectionModifierCursorController controller = this.mEditor.getSelectionController();
            if (controller != null && (this.mTextView.isTextSelectable() || this.mTextView.isTextEditable())) {
                controller.show();
            }
            if (result != null) {
                if (actionMode == 0) {
                    this.mSelectionTracker.onSmartSelection(result);
                } else if (actionMode == 2) {
                    this.mSelectionTracker.onLinkSelected(result);
                }
            }
        }
        this.mEditor.setRestartActionModeOnNextRefresh(false);
        this.mTextClassificationAsyncTask = null;
    }

    private void startSelectionActionModeWithSmartSelectAnimation(SelectionResult result) {
        Layout layout = this.mTextView.getLayout();
        Runnable onAnimationEndCallback = new -$$Lambda$SelectionActionModeHelper$xdBRwQcbRdz8duQr0RBo4YKAnOA(this, result);
        boolean didSelectionChange = (result == null || (this.mTextView.getSelectionStart() == result.mStart && this.mTextView.getSelectionEnd() == result.mEnd)) ? false : true;
        if (didSelectionChange) {
            List<RectangleWithTextSelectionLayout> selectionRectangles = convertSelectionToRectangles(layout, result.mStart, result.mEnd);
            this.mSmartSelectSprite.startAnimation(movePointInsideNearestRectangle(new PointF(this.mEditor.getLastUpPositionX(), this.mEditor.getLastUpPositionY()), selectionRectangles, -$$Lambda$ChL7kntlZCrPaPVdRfaSzGdk1JU.INSTANCE), selectionRectangles, onAnimationEndCallback);
            return;
        }
        onAnimationEndCallback.run();
    }

    public static /* synthetic */ void lambda$startSelectionActionModeWithSmartSelectAnimation$0(SelectionActionModeHelper selectionActionModeHelper, SelectionResult result) {
        SelectionResult startSelectionResult;
        if (result == null || result.mStart < 0 || result.mEnd > getText(selectionActionModeHelper.mTextView).length() || result.mStart > result.mEnd) {
            startSelectionResult = null;
        } else {
            startSelectionResult = result;
        }
        selectionActionModeHelper.startSelectionActionMode(startSelectionResult);
    }

    private List<RectangleWithTextSelectionLayout> convertSelectionToRectangles(Layout layout, int start, int end) {
        List<RectangleWithTextSelectionLayout> result = new ArrayList();
        layout.getSelection(start, end, new -$$Lambda$SelectionActionModeHelper$cMbIRcH-yFkksR3CQmROa0_hmgM(result));
        result.sort(Comparator.comparing(-$$Lambda$ChL7kntlZCrPaPVdRfaSzGdk1JU.INSTANCE, SmartSelectSprite.RECTANGLE_COMPARATOR));
        return result;
    }

    @VisibleForTesting
    public static <T> void mergeRectangleIntoList(List<T> list, RectF candidate, Function<T, RectF> extractor, Function<RectF, T> packer) {
        if (!candidate.isEmpty()) {
            int elementCount = list.size();
            int index = 0;
            while (index < elementCount) {
                RectF existingRectangle = (RectF) extractor.apply(list.get(index));
                if (!existingRectangle.contains(candidate)) {
                    if (candidate.contains(existingRectangle)) {
                        existingRectangle.setEmpty();
                    } else {
                        boolean canMerge = true;
                        boolean rectanglesContinueEachOther = candidate.left == existingRectangle.right || candidate.right == existingRectangle.left;
                        if (!(candidate.top == existingRectangle.top && candidate.bottom == existingRectangle.bottom && (RectF.intersects(candidate, existingRectangle) || rectanglesContinueEachOther))) {
                            canMerge = false;
                        }
                        if (canMerge) {
                            candidate.union(existingRectangle);
                            existingRectangle.setEmpty();
                        }
                    }
                    index++;
                } else {
                    return;
                }
            }
            for (int index2 = elementCount - 1; index2 >= 0; index2--) {
                if (((RectF) extractor.apply(list.get(index2))).isEmpty()) {
                    list.remove(index2);
                }
            }
            list.add(packer.apply(candidate));
        }
    }

    @VisibleForTesting
    public static <T> PointF movePointInsideNearestRectangle(PointF point, List<T> list, Function<T, RectF> extractor) {
        PointF pointF = point;
        float bestX = -1.0f;
        float bestY = -1.0f;
        double bestDistance = Double.MAX_VALUE;
        int elementCount = list.size();
        for (int index = 0; index < elementCount; index++) {
            float candidateX;
            RectF rectangle = (RectF) extractor.apply(list.get(index));
            float candidateY = rectangle.centerY();
            if (pointF.x > rectangle.right) {
                candidateX = rectangle.right;
            } else if (pointF.x < rectangle.left) {
                candidateX = rectangle.left;
            } else {
                candidateX = pointF.x;
            }
            double candidateDistance = Math.pow((double) (pointF.x - candidateX), 2.0d) + Math.pow((double) (pointF.y - candidateY), 2.0d);
            if (candidateDistance < bestDistance) {
                bestX = candidateX;
                bestY = candidateY;
                bestDistance = candidateDistance;
            }
        }
        Function<T, RectF> function = extractor;
        return new PointF(bestX, bestY);
    }

    private void invalidateActionMode(SelectionResult result) {
        cancelSmartSelectAnimation();
        this.mTextClassification = result != null ? result.mClassification : null;
        ActionMode actionMode = this.mEditor.getTextActionMode();
        if (actionMode != null) {
            actionMode.invalidate();
        }
        this.mSelectionTracker.onSelectionUpdated(this.mTextView.getSelectionStart(), this.mTextView.getSelectionEnd(), this.mTextClassification);
        this.mTextClassificationAsyncTask = null;
    }

    private void resetTextClassificationHelper(int selectionStart, int selectionEnd) {
        if (selectionStart < 0 || selectionEnd < 0) {
            selectionStart = this.mTextView.getSelectionStart();
            selectionEnd = this.mTextView.getSelectionEnd();
        }
        TextClassificationHelper textClassificationHelper = this.mTextClassificationHelper;
        TextView textView = this.mTextView;
        Objects.requireNonNull(textView);
        textClassificationHelper.init(new -$$Lambda$yIdmBO6ZxaY03PGN08RySVVQXuE(textView), getText(this.mTextView), selectionStart, selectionEnd, this.mTextView.getTextLocales());
    }

    private void resetTextClassificationHelper() {
        resetTextClassificationHelper(-1, -1);
    }

    private void cancelSmartSelectAnimation() {
        if (this.mSmartSelectSprite != null) {
            this.mSmartSelectSprite.cancelAnimation();
        }
    }

    private static int getActionType(int menuItemId) {
        if (menuItemId != 16908337) {
            if (menuItemId == 16908341) {
                return 104;
            }
            if (menuItemId == 16908353) {
                return 105;
            }
            switch (menuItemId) {
                case 16908319:
                    return 200;
                case 16908320:
                    return 103;
                case 16908321:
                    return 101;
                case 16908322:
                    break;
                default:
                    return 108;
            }
        }
        return 102;
    }

    private static CharSequence getText(TextView textView) {
        CharSequence text = textView.getText();
        if (text != null) {
            return text;
        }
        return "";
    }
}
