package android.support.v4.media.subtitle;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.media.SubtitleData2;
import android.support.v4.media.subtitle.Cea708CCParser.CaptionEvent;
import android.support.v4.media.subtitle.Cea708CCParser.CaptionPenAttr;
import android.support.v4.media.subtitle.Cea708CCParser.CaptionPenColor;
import android.support.v4.media.subtitle.Cea708CCParser.CaptionPenLocation;
import android.support.v4.media.subtitle.Cea708CCParser.CaptionWindow;
import android.support.v4.media.subtitle.Cea708CCParser.CaptionWindowAttr;
import android.support.v4.media.subtitle.SubtitleController.Renderer;
import android.support.v4.media.subtitle.SubtitleTrack.RenderingWidget;
import android.text.Layout.Alignment;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.accessibility.CaptioningManager;
import android.view.accessibility.CaptioningManager.CaptionStyle;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

@RequiresApi(28)
@RestrictTo({Scope.LIBRARY_GROUP})
public class Cea708CaptionRenderer extends Renderer {
    private Cea708CCWidget mCCWidget;
    private final Context mContext;

    class Cea708CCWidget extends ClosedCaptionWidget implements DisplayListener {
        private final CCHandler mCCHandler;

        class CCHandler implements Callback {
            private static final int CAPTION_ALL_WINDOWS_BITMAP = 255;
            private static final long CAPTION_CLEAR_INTERVAL_MS = 60000;
            private static final int CAPTION_WINDOWS_MAX = 8;
            private static final boolean DEBUG = false;
            private static final int MSG_CAPTION_CLEAR = 2;
            private static final int MSG_DELAY_CANCEL = 1;
            private static final String TAG = "CCHandler";
            private static final int TENTHS_OF_SECOND_IN_MILLIS = 100;
            private final CCLayout mCCLayout;
            private final CCWindowLayout[] mCaptionWindowLayouts = new CCWindowLayout[8];
            private CCWindowLayout mCurrentWindowLayout;
            private final Handler mHandler;
            private boolean mIsDelayed = DEBUG;
            private final ArrayList<CaptionEvent> mPendingCaptionEvents = new ArrayList();

            CCHandler(CCLayout ccLayout) {
                this.mCCLayout = ccLayout;
                this.mHandler = new Handler(this);
            }

            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        delayCancel();
                        return true;
                    case 2:
                        clearWindows(255);
                        return true;
                    default:
                        return DEBUG;
                }
            }

            public void processCaptionEvent(CaptionEvent event) {
                if (this.mIsDelayed) {
                    this.mPendingCaptionEvents.add(event);
                    return;
                }
                switch (event.type) {
                    case 1:
                        sendBufferToCurrentWindow((String) event.obj);
                        break;
                    case 2:
                        sendControlToCurrentWindow(((Character) event.obj).charValue());
                        break;
                    case 3:
                        setCurrentWindowLayout(((Integer) event.obj).intValue());
                        break;
                    case 4:
                        clearWindows(((Integer) event.obj).intValue());
                        break;
                    case 5:
                        displayWindows(((Integer) event.obj).intValue());
                        break;
                    case 6:
                        hideWindows(((Integer) event.obj).intValue());
                        break;
                    case 7:
                        toggleWindows(((Integer) event.obj).intValue());
                        break;
                    case 8:
                        deleteWindows(((Integer) event.obj).intValue());
                        break;
                    case 9:
                        delay(((Integer) event.obj).intValue());
                        break;
                    case 10:
                        delayCancel();
                        break;
                    case 11:
                        reset();
                        break;
                    case 12:
                        setPenAttr((CaptionPenAttr) event.obj);
                        break;
                    case 13:
                        setPenColor((CaptionPenColor) event.obj);
                        break;
                    case 14:
                        setPenLocation((CaptionPenLocation) event.obj);
                        break;
                    case 15:
                        setWindowAttr((CaptionWindowAttr) event.obj);
                        break;
                    case 16:
                        defineWindow((CaptionWindow) event.obj);
                        break;
                }
            }

            private void setCurrentWindowLayout(int windowId) {
                if (windowId >= 0 && windowId < this.mCaptionWindowLayouts.length) {
                    CCWindowLayout windowLayout = this.mCaptionWindowLayouts[windowId];
                    if (windowLayout != null) {
                        this.mCurrentWindowLayout = windowLayout;
                    }
                }
            }

            private ArrayList<CCWindowLayout> getWindowsFromBitmap(int windowBitmap) {
                ArrayList<CCWindowLayout> windows = new ArrayList();
                for (int i = 0; i < 8; i++) {
                    if (((1 << i) & windowBitmap) != 0) {
                        CCWindowLayout windowLayout = this.mCaptionWindowLayouts[i];
                        if (windowLayout != null) {
                            windows.add(windowLayout);
                        }
                    }
                }
                return windows;
            }

            private void clearWindows(int windowBitmap) {
                if (windowBitmap != 0) {
                    Iterator it = getWindowsFromBitmap(windowBitmap).iterator();
                    while (it.hasNext()) {
                        ((CCWindowLayout) it.next()).clear();
                    }
                }
            }

            private void displayWindows(int windowBitmap) {
                if (windowBitmap != 0) {
                    Iterator it = getWindowsFromBitmap(windowBitmap).iterator();
                    while (it.hasNext()) {
                        ((CCWindowLayout) it.next()).show();
                    }
                }
            }

            private void hideWindows(int windowBitmap) {
                if (windowBitmap != 0) {
                    Iterator it = getWindowsFromBitmap(windowBitmap).iterator();
                    while (it.hasNext()) {
                        ((CCWindowLayout) it.next()).hide();
                    }
                }
            }

            private void toggleWindows(int windowBitmap) {
                if (windowBitmap != 0) {
                    Iterator it = getWindowsFromBitmap(windowBitmap).iterator();
                    while (it.hasNext()) {
                        CCWindowLayout windowLayout = (CCWindowLayout) it.next();
                        if (windowLayout.isShown()) {
                            windowLayout.hide();
                        } else {
                            windowLayout.show();
                        }
                    }
                }
            }

            private void deleteWindows(int windowBitmap) {
                if (windowBitmap != 0) {
                    Iterator it = getWindowsFromBitmap(windowBitmap).iterator();
                    while (it.hasNext()) {
                        CCWindowLayout windowLayout = (CCWindowLayout) it.next();
                        windowLayout.removeFromCaptionView();
                        this.mCaptionWindowLayouts[windowLayout.getCaptionWindowId()] = null;
                    }
                }
            }

            public void reset() {
                this.mCurrentWindowLayout = null;
                int i = 0;
                this.mIsDelayed = DEBUG;
                this.mPendingCaptionEvents.clear();
                while (i < 8) {
                    if (this.mCaptionWindowLayouts[i] != null) {
                        this.mCaptionWindowLayouts[i].removeFromCaptionView();
                    }
                    this.mCaptionWindowLayouts[i] = null;
                    i++;
                }
                this.mCCLayout.setVisibility(4);
                this.mHandler.removeMessages(2);
            }

            private void setWindowAttr(CaptionWindowAttr windowAttr) {
                if (this.mCurrentWindowLayout != null) {
                    this.mCurrentWindowLayout.setWindowAttr(windowAttr);
                }
            }

            private void defineWindow(CaptionWindow window) {
                if (window != null) {
                    int windowId = window.id;
                    if (windowId >= 0 && windowId < this.mCaptionWindowLayouts.length) {
                        CCWindowLayout windowLayout = this.mCaptionWindowLayouts[windowId];
                        if (windowLayout == null) {
                            windowLayout = new CCWindowLayout(Cea708CCWidget.this, this.mCCLayout.getContext());
                        }
                        windowLayout.initWindow(this.mCCLayout, window);
                        this.mCaptionWindowLayouts[windowId] = windowLayout;
                        this.mCurrentWindowLayout = windowLayout;
                    }
                }
            }

            private void delay(int tenthsOfSeconds) {
                if (tenthsOfSeconds >= 0 && tenthsOfSeconds <= 255) {
                    this.mIsDelayed = true;
                    this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), (long) (tenthsOfSeconds * 100));
                }
            }

            private void delayCancel() {
                this.mIsDelayed = DEBUG;
                processPendingBuffer();
            }

            private void processPendingBuffer() {
                Iterator it = this.mPendingCaptionEvents.iterator();
                while (it.hasNext()) {
                    processCaptionEvent((CaptionEvent) it.next());
                }
                this.mPendingCaptionEvents.clear();
            }

            private void sendControlToCurrentWindow(char control) {
                if (this.mCurrentWindowLayout != null) {
                    this.mCurrentWindowLayout.sendControl(control);
                }
            }

            private void sendBufferToCurrentWindow(String buffer) {
                if (this.mCurrentWindowLayout != null) {
                    this.mCurrentWindowLayout.sendBuffer(buffer);
                    this.mHandler.removeMessages(2);
                    this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2), CAPTION_CLEAR_INTERVAL_MS);
                }
            }

            private void setPenAttr(CaptionPenAttr attr) {
                if (this.mCurrentWindowLayout != null) {
                    this.mCurrentWindowLayout.setPenAttr(attr);
                }
            }

            private void setPenColor(CaptionPenColor color) {
                if (this.mCurrentWindowLayout != null) {
                    this.mCurrentWindowLayout.setPenColor(color);
                }
            }

            private void setPenLocation(CaptionPenLocation location) {
                if (this.mCurrentWindowLayout != null) {
                    this.mCurrentWindowLayout.setPenLocation(location.row, location.column);
                }
            }
        }

        private class CCWindowLayout extends RelativeLayout implements OnLayoutChangeListener {
            private static final int ANCHOR_HORIZONTAL_16_9_MAX = 209;
            private static final int ANCHOR_HORIZONTAL_MODE_CENTER = 1;
            private static final int ANCHOR_HORIZONTAL_MODE_LEFT = 0;
            private static final int ANCHOR_HORIZONTAL_MODE_RIGHT = 2;
            private static final int ANCHOR_MODE_DIVIDER = 3;
            private static final int ANCHOR_RELATIVE_POSITIONING_MAX = 99;
            private static final int ANCHOR_VERTICAL_MAX = 74;
            private static final int ANCHOR_VERTICAL_MODE_BOTTOM = 2;
            private static final int ANCHOR_VERTICAL_MODE_CENTER = 1;
            private static final int ANCHOR_VERTICAL_MODE_TOP = 0;
            private static final int MAX_COLUMN_COUNT_16_9 = 42;
            private static final float PROPORTION_PEN_SIZE_LARGE = 1.25f;
            private static final float PROPORTION_PEN_SIZE_SMALL = 0.75f;
            private static final String TAG = "CCWindowLayout";
            private final SpannableStringBuilder mBuilder;
            private CCLayout mCCLayout;
            private CCView mCCView;
            private CaptionStyle mCaptionStyle;
            private int mCaptionWindowId;
            private final List<CharacterStyle> mCharacterStyles;
            private float mFontScale;
            private int mLastCaptionLayoutHeight;
            private int mLastCaptionLayoutWidth;
            private int mRow;
            private int mRowLimit;
            private float mTextSize;
            private String mWidestChar;

            CCWindowLayout(Cea708CCWidget cea708CCWidget, Context context) {
                this(cea708CCWidget, context, null);
            }

            CCWindowLayout(Cea708CCWidget cea708CCWidget, Context context, AttributeSet attrs) {
                this(cea708CCWidget, context, attrs, 0);
            }

            CCWindowLayout(Cea708CCWidget cea708CCWidget, Context context, AttributeSet attrs, int defStyleAttr) {
                this(context, attrs, defStyleAttr, 0);
            }

            CCWindowLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
                super(context, attrs, defStyleAttr, defStyleRes);
                this.mRowLimit = 0;
                this.mBuilder = new SpannableStringBuilder();
                this.mCharacterStyles = new ArrayList();
                this.mRow = -1;
                this.mCCView = new CCView(Cea708CCWidget.this, context);
                addView(this.mCCView, new LayoutParams(-2, -2));
                CaptioningManager captioningManager = (CaptioningManager) context.getSystemService("captioning");
                this.mFontScale = captioningManager.getFontScale();
                setCaptionStyle(captioningManager.getUserStyle());
                this.mCCView.setText((CharSequence) "");
                updateWidestChar();
            }

            public void setCaptionStyle(CaptionStyle style) {
                this.mCaptionStyle = style;
                this.mCCView.setCaptionStyle(style);
            }

            public void setFontScale(float fontScale) {
                this.mFontScale = fontScale;
                updateTextSize();
            }

            public int getCaptionWindowId() {
                return this.mCaptionWindowId;
            }

            public void setCaptionWindowId(int captionWindowId) {
                this.mCaptionWindowId = captionWindowId;
            }

            public void clear() {
                clearText();
                hide();
            }

            public void show() {
                setVisibility(0);
                requestLayout();
            }

            public void hide() {
                setVisibility(4);
                requestLayout();
            }

            public void setPenAttr(CaptionPenAttr penAttr) {
                this.mCharacterStyles.clear();
                if (penAttr.italic) {
                    this.mCharacterStyles.add(new StyleSpan(2));
                }
                if (penAttr.underline) {
                    this.mCharacterStyles.add(new UnderlineSpan());
                }
                int i = penAttr.penSize;
                if (i == 0) {
                    this.mCharacterStyles.add(new RelativeSizeSpan(PROPORTION_PEN_SIZE_SMALL));
                } else if (i == 2) {
                    this.mCharacterStyles.add(new RelativeSizeSpan(PROPORTION_PEN_SIZE_LARGE));
                }
                i = penAttr.penOffset;
                if (i == 0) {
                    this.mCharacterStyles.add(new SubscriptSpan());
                } else if (i == 2) {
                    this.mCharacterStyles.add(new SuperscriptSpan());
                }
            }

            public void setPenColor(CaptionPenColor penColor) {
            }

            public void setPenLocation(int row, int column) {
                if (this.mRow >= 0) {
                    for (int r = this.mRow; r < row; r++) {
                        appendText("\n");
                    }
                }
                this.mRow = row;
            }

            public void setWindowAttr(CaptionWindowAttr windowAttr) {
            }

            public void sendBuffer(String buffer) {
                appendText(buffer);
            }

            public void sendControl(char control) {
            }

            public void initWindow(CCLayout ccLayout, CaptionWindow captionWindow) {
                String str;
                StringBuilder stringBuilder;
                float halfMaxWidthScale;
                CCLayout cCLayout = ccLayout;
                CaptionWindow captionWindow2 = captionWindow;
                if (this.mCCLayout != cCLayout) {
                    if (this.mCCLayout != null) {
                        this.mCCLayout.removeOnLayoutChangeListener(this);
                    }
                    this.mCCLayout = cCLayout;
                    this.mCCLayout.addOnLayoutChangeListener(this);
                    updateWidestChar();
                }
                float scaleRow = (float) captionWindow2.anchorVertical;
                boolean z = captionWindow2.relativePositioning;
                int i = ANCHOR_RELATIVE_POSITIONING_MAX;
                scaleRow /= (float) (z ? ANCHOR_RELATIVE_POSITIONING_MAX : ANCHOR_VERTICAL_MAX);
                float scaleCol = (float) captionWindow2.anchorHorizontal;
                if (!captionWindow2.relativePositioning) {
                    i = ANCHOR_HORIZONTAL_16_9_MAX;
                }
                scaleCol /= (float) i;
                if (scaleRow < 0.0f || scaleRow > 1.0f) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("The vertical position of the anchor point should be at the range of 0 and 1 but ");
                    stringBuilder.append(scaleRow);
                    Log.i(str, stringBuilder.toString());
                    scaleRow = Math.max(0.0f, Math.min(scaleRow, 1.0f));
                }
                if (scaleCol < 0.0f || scaleCol > 1.0f) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("The horizontal position of the anchor point should be at the range of 0 and 1 but ");
                    stringBuilder.append(scaleCol);
                    Log.i(str, stringBuilder.toString());
                    scaleCol = Math.max(0.0f, Math.min(scaleCol, 1.0f));
                }
                int gravity = 17;
                int verticalMode = captionWindow2.anchorId / 3;
                float scaleStartRow = 0.0f;
                float scaleEndRow = 1.0f;
                float scaleStartCol = 0.0f;
                float scaleEndCol = 1.0f;
                switch (captionWindow2.anchorId % 3) {
                    case 0:
                        gravity = 3;
                        this.mCCView.setAlignment(Alignment.ALIGN_NORMAL);
                        scaleStartCol = scaleCol;
                        break;
                    case 1:
                        float gap = Math.min(1.0f - scaleCol, scaleCol);
                        int columnCount = Math.min(getScreenColumnCount(), captionWindow2.columnCount + 1);
                        StringBuilder widestTextBuilder = new StringBuilder();
                        int i2 = 0;
                        while (true) {
                            i = i2;
                            if (i >= columnCount) {
                                Paint paint = new Paint();
                                paint.setTypeface(this.mCaptionStyle.getTypeface());
                                paint.setTextSize(this.mTextSize);
                                float maxWindowWidth = paint.measureText(widestTextBuilder.toString());
                                if (this.mCCLayout.getWidth() > 0) {
                                    maxWindowWidth = (maxWindowWidth / 2.0f) / (((float) this.mCCLayout.getWidth()) * 0.8f);
                                } else {
                                    maxWindowWidth = 0.0f;
                                }
                                halfMaxWidthScale = maxWindowWidth;
                                if (halfMaxWidthScale > 0.0f && halfMaxWidthScale < scaleCol) {
                                    int gravity2 = 3;
                                    this.mCCView.setAlignment(Alignment.ALIGN_NORMAL);
                                    scaleStartCol = scaleCol - halfMaxWidthScale;
                                    scaleEndCol = 1.0f;
                                    gravity = gravity2;
                                    break;
                                }
                                gravity = 1;
                                this.mCCView.setAlignment(Alignment.ALIGN_CENTER);
                                scaleStartCol = scaleCol - gap;
                                scaleEndCol = scaleCol + gap;
                                break;
                            }
                            widestTextBuilder.append(this.mWidestChar);
                            i2 = i + 1;
                            cCLayout = ccLayout;
                        }
                        break;
                    case 2:
                        gravity = 5;
                        scaleEndCol = scaleCol;
                        break;
                }
                switch (verticalMode) {
                    case 0:
                        gravity |= 48;
                        scaleStartRow = scaleRow;
                        break;
                    case 1:
                        gravity |= 16;
                        halfMaxWidthScale = Math.min(1.0f - scaleRow, scaleRow);
                        scaleStartRow = scaleRow - halfMaxWidthScale;
                        scaleEndRow = scaleRow + halfMaxWidthScale;
                        break;
                    case 2:
                        gravity |= 80;
                        scaleEndRow = scaleRow;
                        break;
                }
                cCLayout = this.mCCLayout;
                ScaledLayout scaledLayout = this.mCCLayout;
                scaledLayout.getClass();
                cCLayout.addOrUpdateViewToSafeTitleArea(this, new ScaledLayoutParams(scaleStartRow, scaleEndRow, scaleStartCol, scaleEndCol));
                setCaptionWindowId(captionWindow2.id);
                setRowLimit(captionWindow2.rowCount);
                setGravity(gravity);
                if (captionWindow2.visible) {
                    show();
                } else {
                    hide();
                }
            }

            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int width = right - left;
                int height = bottom - top;
                if (width != this.mLastCaptionLayoutWidth || height != this.mLastCaptionLayoutHeight) {
                    this.mLastCaptionLayoutWidth = width;
                    this.mLastCaptionLayoutHeight = height;
                    updateTextSize();
                }
            }

            private void updateWidestChar() {
                Paint paint = new Paint();
                paint.setTypeface(this.mCaptionStyle.getTypeface());
                Charset latin1 = Charset.forName("ISO-8859-1");
                float widestCharWidth = 0.0f;
                for (int i = 0; i < 256; i++) {
                    String ch = new String(new byte[]{(byte) i}, latin1);
                    float charWidth = paint.measureText(ch);
                    if (widestCharWidth < charWidth) {
                        widestCharWidth = charWidth;
                        this.mWidestChar = ch;
                    }
                }
                updateTextSize();
            }

            private void updateTextSize() {
                if (this.mCCLayout != null) {
                    StringBuilder widestTextBuilder = new StringBuilder();
                    int screenColumnCount = getScreenColumnCount();
                    for (int i = 0; i < screenColumnCount; i++) {
                        widestTextBuilder.append(this.mWidestChar);
                    }
                    String widestText = widestTextBuilder.toString();
                    Paint paint = new Paint();
                    paint.setTypeface(this.mCaptionStyle.getTypeface());
                    float startFontSize = 0.0f;
                    float endFontSize = 255.0f;
                    while (startFontSize < endFontSize) {
                        float endFontSize2 = (startFontSize + endFontSize) / 2.0f;
                        paint.setTextSize(endFontSize2);
                        if (((float) this.mCCLayout.getWidth()) * 0.8f > paint.measureText(widestText)) {
                            startFontSize = 0.01f + endFontSize2;
                        } else {
                            endFontSize = endFontSize2 - 0.01f;
                        }
                    }
                    this.mTextSize = this.mFontScale * endFontSize;
                    this.mCCView.setTextSize(this.mTextSize);
                }
            }

            private int getScreenColumnCount() {
                return 42;
            }

            public void removeFromCaptionView() {
                if (this.mCCLayout != null) {
                    this.mCCLayout.removeViewFromSafeTitleArea(this);
                    this.mCCLayout.removeOnLayoutChangeListener(this);
                    this.mCCLayout = null;
                }
            }

            public void setText(String text) {
                updateText(text, false);
            }

            public void appendText(String text) {
                updateText(text, true);
            }

            public void clearText() {
                this.mBuilder.clear();
                this.mCCView.setText((CharSequence) "");
            }

            private void updateText(String text, boolean appended) {
                if (!appended) {
                    this.mBuilder.clear();
                }
                if (text != null && text.length() > 0) {
                    int length = this.mBuilder.length();
                    this.mBuilder.append(text);
                    for (CharacterStyle characterStyle : this.mCharacterStyles) {
                        this.mBuilder.setSpan(characterStyle, length, this.mBuilder.length(), 33);
                    }
                }
                String[] lines = TextUtils.split(this.mBuilder.toString(), "\n");
                this.mBuilder.delete(0, this.mBuilder.length() - TextUtils.join("\n", Arrays.copyOfRange(lines, Math.max(0, lines.length - (this.mRowLimit + 1)), lines.length)).length());
                int last = this.mBuilder.length() - 1;
                int start = 0;
                int end = last;
                while (start <= end && this.mBuilder.charAt(start) <= ' ') {
                    start++;
                }
                while (end >= start && this.mBuilder.charAt(end) <= ' ') {
                    end--;
                }
                if (start == 0 && end == last) {
                    this.mCCView.setText((CharSequence) this.mBuilder);
                    return;
                }
                SpannableStringBuilder trim = new SpannableStringBuilder();
                trim.append(this.mBuilder);
                if (end < last) {
                    trim.delete(end + 1, last + 1);
                }
                if (start > 0) {
                    trim.delete(0, start);
                }
                this.mCCView.setText((CharSequence) trim);
            }

            public void setRowLimit(int rowLimit) {
                if (rowLimit >= 0) {
                    this.mRowLimit = rowLimit;
                    return;
                }
                throw new IllegalArgumentException("A rowLimit should have a positive number");
            }
        }

        class ScaledLayout extends ViewGroup {
            private static final boolean DEBUG = false;
            private static final String TAG = "ScaledLayout";
            private Rect[] mRectArray;
            private final Comparator<Rect> mRectTopLeftSorter = new Comparator<Rect>() {
                public int compare(Rect lhs, Rect rhs) {
                    if (lhs.top != rhs.top) {
                        return lhs.top - rhs.top;
                    }
                    return lhs.left - rhs.left;
                }
            };

            class ScaledLayoutParams extends ViewGroup.LayoutParams {
                public static final float SCALE_UNSPECIFIED = -1.0f;
                public float scaleEndCol;
                public float scaleEndRow;
                public float scaleStartCol;
                public float scaleStartRow;

                ScaledLayoutParams(float scaleStartRow, float scaleEndRow, float scaleStartCol, float scaleEndCol) {
                    super(-1, -1);
                    this.scaleStartRow = scaleStartRow;
                    this.scaleEndRow = scaleEndRow;
                    this.scaleStartCol = scaleStartCol;
                    this.scaleEndCol = scaleEndCol;
                }

                ScaledLayoutParams(Context context, AttributeSet attrs) {
                    super(-1, -1);
                }
            }

            ScaledLayout(Context context) {
                super(context);
            }

            public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
                return new ScaledLayoutParams(getContext(), attrs);
            }

            protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
                return p instanceof ScaledLayoutParams;
            }

            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int i;
                int widthSpecSize;
                int j;
                int widthSpecSize2 = MeasureSpec.getSize(widthMeasureSpec);
                int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
                int width = (widthSpecSize2 - getPaddingLeft()) - getPaddingRight();
                int height = (heightSpecSize - getPaddingTop()) - getPaddingBottom();
                int count = getChildCount();
                this.mRectArray = new Rect[count];
                int i2 = 0;
                while (i2 < count) {
                    View child = getChildAt(i2);
                    ViewGroup.LayoutParams params = child.getLayoutParams();
                    ViewGroup.LayoutParams layoutParams;
                    if (params instanceof ScaledLayoutParams) {
                        float scaleStartRow = ((ScaledLayoutParams) params).scaleStartRow;
                        float scaleEndRow = ((ScaledLayoutParams) params).scaleEndRow;
                        float scaleStartCol = ((ScaledLayoutParams) params).scaleStartCol;
                        float scaleEndCol = ((ScaledLayoutParams) params).scaleEndCol;
                        if (scaleStartRow < 0.0f || scaleStartRow > 1.0f) {
                            i = heightSpecSize;
                            layoutParams = params;
                            throw new RuntimeException("A child of ScaledLayout should have a range of scaleStartRow between 0 and 1");
                        } else if (scaleEndRow < scaleStartRow || scaleStartRow > 1.0f) {
                            i = heightSpecSize;
                            layoutParams = params;
                            throw new RuntimeException("A child of ScaledLayout should have a range of scaleEndRow between scaleStartRow and 1");
                        } else if (scaleEndCol < 0.0f || scaleEndCol > 1.0f) {
                            i = heightSpecSize;
                            layoutParams = params;
                            throw new RuntimeException("A child of ScaledLayout should have a range of scaleStartCol between 0 and 1");
                        } else if (scaleEndCol < scaleStartCol || scaleEndCol > 1.0f) {
                            i = heightSpecSize;
                            layoutParams = params;
                            throw new RuntimeException("A child of ScaledLayout should have a range of scaleEndCol between scaleStartCol and 1");
                        } else {
                            widthSpecSize = widthSpecSize2;
                            i = heightSpecSize;
                            this.mRectArray[i2] = new Rect((int) (((float) width) * scaleStartCol), (int) (((float) height) * scaleStartRow), (int) (((float) width) * scaleEndCol), (int) (((float) height) * scaleEndRow));
                            widthSpecSize2 = MeasureSpec.makeMeasureSpec((int) (((float) width) * (scaleEndCol - scaleStartCol)), 1073741824);
                            child.measure(widthSpecSize2, MeasureSpec.makeMeasureSpec(0, 0));
                            if (child.getMeasuredHeight() > this.mRectArray[i2].height()) {
                                int overflowedHeight = ((child.getMeasuredHeight() - this.mRectArray[i2].height()) + 1) / 2;
                                Rect rect = this.mRectArray[i2];
                                rect.bottom += overflowedHeight;
                                rect = this.mRectArray[i2];
                                rect.top -= overflowedHeight;
                                if (this.mRectArray[i2].top < 0) {
                                    rect = this.mRectArray[i2];
                                    rect.bottom -= this.mRectArray[i2].top;
                                    this.mRectArray[i2].top = 0;
                                }
                                if (this.mRectArray[i2].bottom > height) {
                                    Rect rect2 = this.mRectArray[i2];
                                    rect2.top -= this.mRectArray[i2].bottom - height;
                                    this.mRectArray[i2].bottom = height;
                                }
                            }
                            child.measure(widthSpecSize2, MeasureSpec.makeMeasureSpec((int) (((float) height) * (scaleEndRow - scaleStartRow)), 1073741824));
                            i2++;
                            widthSpecSize2 = widthSpecSize;
                            heightSpecSize = i;
                        }
                    } else {
                        i = heightSpecSize;
                        layoutParams = params;
                        throw new RuntimeException("A child of ScaledLayout cannot have the UNSPECIFIED scale factors");
                    }
                }
                widthSpecSize = widthSpecSize2;
                i = heightSpecSize;
                int[] visibleRectGroup = new int[count];
                Rect[] visibleRectArray = new Rect[count];
                i2 = 0;
                for (widthSpecSize2 = 0; widthSpecSize2 < count; widthSpecSize2++) {
                    if (getChildAt(widthSpecSize2).getVisibility() == 0) {
                        visibleRectGroup[i2] = i2;
                        visibleRectArray[i2] = this.mRectArray[widthSpecSize2];
                        i2++;
                    }
                }
                Arrays.sort(visibleRectArray, 0, i2, this.mRectTopLeftSorter);
                for (widthSpecSize2 = 0; widthSpecSize2 < i2 - 1; widthSpecSize2++) {
                    for (j = widthSpecSize2 + 1; j < i2; j++) {
                        if (Rect.intersects(visibleRectArray[widthSpecSize2], visibleRectArray[j])) {
                            visibleRectGroup[j] = visibleRectGroup[widthSpecSize2];
                            visibleRectArray[j].set(visibleRectArray[j].left, visibleRectArray[widthSpecSize2].bottom, visibleRectArray[j].right, visibleRectArray[widthSpecSize2].bottom + visibleRectArray[j].height());
                        }
                    }
                }
                for (widthSpecSize2 = i2 - 1; widthSpecSize2 >= 0; widthSpecSize2--) {
                    if (visibleRectArray[widthSpecSize2].bottom > height) {
                        j = visibleRectArray[widthSpecSize2].bottom - height;
                        for (int j2 = 0; j2 <= widthSpecSize2; j2++) {
                            if (visibleRectGroup[widthSpecSize2] == visibleRectGroup[j2]) {
                                visibleRectArray[j2].set(visibleRectArray[j2].left, visibleRectArray[j2].top - j, visibleRectArray[j2].right, visibleRectArray[j2].bottom - j);
                            }
                        }
                    }
                }
                setMeasuredDimension(widthSpecSize, i);
            }

            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                int paddingLeft = getPaddingLeft();
                int paddingTop = getPaddingTop();
                int count = getChildCount();
                for (int i = 0; i < count; i++) {
                    View child = getChildAt(i);
                    if (child.getVisibility() != 8) {
                        child.layout(this.mRectArray[i].left + paddingLeft, this.mRectArray[i].top + paddingTop, this.mRectArray[i].right + paddingTop, this.mRectArray[i].bottom + paddingLeft);
                    }
                }
            }

            public void dispatchDraw(Canvas canvas) {
                int paddingLeft = getPaddingLeft();
                int paddingTop = getPaddingTop();
                int count = getChildCount();
                for (int i = 0; i < count; i++) {
                    View child = getChildAt(i);
                    if (child.getVisibility() != 8) {
                        if (i < this.mRectArray.length) {
                            int childLeft = this.mRectArray[i].left + paddingLeft;
                            int childTop = this.mRectArray[i].top + paddingTop;
                            int saveCount = canvas.save();
                            canvas.translate((float) childLeft, (float) childTop);
                            child.draw(canvas);
                            canvas.restoreToCount(saveCount);
                        } else {
                            return;
                        }
                    }
                }
            }
        }

        class CCLayout extends ScaledLayout implements ClosedCaptionLayout {
            private static final float SAFE_TITLE_AREA_SCALE_END_X = 0.9f;
            private static final float SAFE_TITLE_AREA_SCALE_END_Y = 0.9f;
            private static final float SAFE_TITLE_AREA_SCALE_START_X = 0.1f;
            private static final float SAFE_TITLE_AREA_SCALE_START_Y = 0.1f;
            private final ScaledLayout mSafeTitleAreaLayout;

            CCLayout(Context context) {
                super(context);
                this.mSafeTitleAreaLayout = new ScaledLayout(context);
                addView(this.mSafeTitleAreaLayout, new ScaledLayoutParams(0.1f, 0.9f, 0.1f, 0.9f));
            }

            public void addOrUpdateViewToSafeTitleArea(CCWindowLayout captionWindowLayout, ScaledLayoutParams scaledLayoutParams) {
                if (this.mSafeTitleAreaLayout.indexOfChild(captionWindowLayout) < 0) {
                    this.mSafeTitleAreaLayout.addView(captionWindowLayout, scaledLayoutParams);
                } else {
                    this.mSafeTitleAreaLayout.updateViewLayout(captionWindowLayout, scaledLayoutParams);
                }
            }

            public void removeViewFromSafeTitleArea(CCWindowLayout captionWindowLayout) {
                this.mSafeTitleAreaLayout.removeView(captionWindowLayout);
            }

            public void setCaptionStyle(CaptionStyle style) {
                int count = this.mSafeTitleAreaLayout.getChildCount();
                for (int i = 0; i < count; i++) {
                    ((CCWindowLayout) this.mSafeTitleAreaLayout.getChildAt(i)).setCaptionStyle(style);
                }
            }

            public void setFontScale(float fontScale) {
                int count = this.mSafeTitleAreaLayout.getChildCount();
                for (int i = 0; i < count; i++) {
                    ((CCWindowLayout) this.mSafeTitleAreaLayout.getChildAt(i)).setFontScale(fontScale);
                }
            }
        }

        class CCView extends SubtitleView {
            CCView(Cea708CCWidget this$1, Context context) {
                this(this$1, context, null);
            }

            CCView(Cea708CCWidget this$1, Context context, AttributeSet attrs) {
                this(this$1, context, attrs, 0);
            }

            CCView(Cea708CCWidget this$1, Context context, AttributeSet attrs, int defStyleAttr) {
                this(context, attrs, defStyleAttr, 0);
            }

            CCView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
                super(context, attrs, defStyleAttr, defStyleRes);
            }

            void setCaptionStyle(CaptionStyle style) {
                if (style.hasForegroundColor()) {
                    setForegroundColor(style.foregroundColor);
                }
                if (style.hasBackgroundColor()) {
                    setBackgroundColor(style.backgroundColor);
                }
                if (style.hasEdgeType()) {
                    setEdgeType(style.edgeType);
                }
                if (style.hasEdgeColor()) {
                    setEdgeColor(style.edgeColor);
                }
                setTypeface(style.getTypeface());
            }
        }

        Cea708CCWidget(Cea708CaptionRenderer this$0, Context context) {
            this(this$0, context, null);
        }

        Cea708CCWidget(Cea708CaptionRenderer this$0, Context context, AttributeSet attrs) {
            this(this$0, context, attrs, 0);
        }

        Cea708CCWidget(Cea708CaptionRenderer this$0, Context context, AttributeSet attrs, int defStyleAttr) {
            this(context, attrs, defStyleAttr, 0);
        }

        Cea708CCWidget(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
            this.mCCHandler = new CCHandler((CCLayout) this.mClosedCaptionLayout);
        }

        public ClosedCaptionLayout createCaptionLayout(Context context) {
            return new CCLayout(context);
        }

        public void emitEvent(CaptionEvent event) {
            this.mCCHandler.processCaptionEvent(event);
            setSize(getWidth(), getHeight());
            if (this.mListener != null) {
                this.mListener.onChanged(this);
            }
        }

        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            ((ViewGroup) this.mClosedCaptionLayout).draw(canvas);
        }
    }

    static class Cea708CaptionTrack extends SubtitleTrack {
        private final Cea708CCParser mCCParser = new Cea708CCParser(this.mRenderingWidget);
        private final Cea708CCWidget mRenderingWidget;

        Cea708CaptionTrack(Cea708CCWidget renderingWidget, MediaFormat format) {
            super(format);
            this.mRenderingWidget = renderingWidget;
        }

        public void onData(byte[] data, boolean eos, long runID) {
            this.mCCParser.parse(data);
        }

        public RenderingWidget getRenderingWidget() {
            return this.mRenderingWidget;
        }

        public void updateView(ArrayList<Cue> arrayList) {
        }
    }

    public Cea708CaptionRenderer(Context context) {
        this.mContext = context;
    }

    public boolean supports(MediaFormat format) {
        if (!format.containsKey("mime")) {
            return false;
        }
        return SubtitleData2.MIMETYPE_TEXT_CEA_708.equals(format.getString("mime"));
    }

    public SubtitleTrack createTrack(MediaFormat format) {
        if (SubtitleData2.MIMETYPE_TEXT_CEA_708.equals(format.getString("mime"))) {
            if (this.mCCWidget == null) {
                this.mCCWidget = new Cea708CCWidget(this, this.mContext);
            }
            return new Cea708CaptionTrack(this.mCCWidget, format);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No matching format: ");
        stringBuilder.append(format.toString());
        throw new RuntimeException(stringBuilder.toString());
    }
}
