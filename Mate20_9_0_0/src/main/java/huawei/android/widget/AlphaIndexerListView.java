package huawei.android.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hwcontrol.HwWidgetFactory;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Adapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.huawei.android.os.VibratorEx;
import huawei.android.hwutil.SectionLocaleUtils;
import huawei.android.widget.loader.ResLoader;
import huawei.android.widget.loader.ResLoaderUtil;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AlphaIndexerListView extends View {
    public static final String BULLET_CHAR = "•";
    public static final String DIGIT_LABEL = "#";
    private static final int LANDSCAPE_ALPHA_COUNT_HH = 10;
    private static final int LANDSCAPE_ALPHA_COUNT_MAX = 18;
    private static final int LANDSCAPE_ALPHA_COUNT_MIN = 6;
    private static final int LANDSCAPE_ALPHA_COUNT_REDUCE = 14;
    private static final int LENGTH = 2;
    private static final int PORTRAIT_ALPHA_COUNT_MAX = 26;
    private static final String TAG = "AlphaIndexerListView";
    public static final int TOO_FEW_ALPHA_COUNT = 6;
    private static boolean debug = false;
    private String[] ALPHABETS;
    private String[] EN_ALPHABETS;
    private int choose = -1;
    Runnable dismissRunnable = new Runnable() {
        public void run() {
            if (AlphaIndexerListView.this.mPopupWindow != null) {
                AlphaIndexerListView.this.mPopupWindow.dismiss();
            }
        }
    };
    private int height;
    private float mAlphaTextSize;
    private List<String> mAlphabet = new ArrayList();
    private int mBottomGap;
    private Context mContext;
    private String mFirstEnglishAlpha = "A";
    private int mFirstNativeIndex = -1;
    private List<String> mFullAlphabet = new ArrayList();
    private float mGapBetweenAlpha;
    private Handler mHandler = new Handler();
    private boolean mHasNativeIndexer;
    private int mInactiveAlphaColor;
    private boolean mIsDigitLast = false;
    private boolean mIsInit = false;
    private boolean mIsLandscape = false;
    private boolean mIsNativeIndexerShown;
    private boolean mIsSupportVibrator = false;
    private float mLastY = 0.0f;
    private int mLessBottomGap;
    private ListView mListView;
    private String mLstEnglishAlpha = "Z";
    private OnItemClickListener mOnItemClickListener;
    private Paint mPaint = new Paint();
    private Drawable mPopupBgDrawable;
    private TextView mPopupText;
    private int mPopupTextColor = -1;
    private PopupWindow mPopupWindow;
    private String mSectionText;
    private int mSelectedAlphaColor;
    private boolean mShowPopup = true;
    private int mTopGap;
    private VibratorEx mVibratorEx = new VibratorEx();

    public interface OnItemClickListener {
        void onItemClick(String str, int i);
    }

    public AlphaIndexerListView(Context context) {
        super(context);
        init(context);
    }

    public AlphaIndexerListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AlphaIndexerListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public boolean ifShowPopup() {
        return this.mShowPopup;
    }

    public void setShowPopup(boolean mShowPopup) {
        this.mShowPopup = mShowPopup;
    }

    private void init(Context context) {
        Drawable drawable;
        int color;
        this.mContext = context;
        Resources res = ResLoader.getInstance().getResources(context);
        if (HwWidgetFactory.isHwDarkTheme(context)) {
            drawable = res.getDrawable(ResLoaderUtil.getDrawableId(context, "alphaindexer_pop_bg_emui_dark"), null);
        } else {
            drawable = res.getDrawable(ResLoaderUtil.getDrawableId(context, "alphaindexer_pop_bg_emui"), null);
        }
        this.mPopupBgDrawable = drawable;
        if (HwWidgetFactory.isHwDarkTheme(context)) {
            color = ResLoaderUtil.getColor(context, "alphaindexer_selected_alpha_color_dark");
        } else {
            color = ResLoaderUtil.getColor(context, "emui_color_gray_10");
        }
        this.mPopupTextColor = color;
        boolean z = false;
        this.mHasNativeIndexer = SectionLocaleUtils.getInstance().getBucketIndex(this.mFirstEnglishAlpha) != 1;
        if (res.getConfiguration().orientation == 2) {
            z = true;
        }
        this.mIsLandscape = z;
        this.mLessBottomGap = ResLoaderUtil.getDimensionPixelSize(context, "alphaindexer_listview_bottom_gap");
        this.mBottomGap = ResLoaderUtil.getDimensionPixelSize(context, "alphaindexer_bottom_margin");
        this.mAlphaTextSize = (float) ResLoaderUtil.getDimensionPixelSize(context, "emui_master_body_1");
        if (HwWidgetFactory.isHwDarkTheme(context)) {
            color = ResLoaderUtil.getColor(context, "alphaindexer_unselected_alpha_color_dark");
        } else {
            color = ResLoaderUtil.getColor(context, "emui_color_gray_7");
        }
        this.mInactiveAlphaColor = color;
        if (HwWidgetFactory.isHwDarkTheme(context)) {
            color = ResLoaderUtil.getColor(context, "alphaindexer_selected_alpha_color_dark");
        } else {
            color = ResLoaderUtil.getColor(context, "emui_accent");
        }
        this.mSelectedAlphaColor = color;
        this.mIsSupportVibrator = this.mVibratorEx.isSupportHwVibrator("haptic.control.letters_scroll");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("support HwVibrator type HW_VIBRATOR_TPYE_CONTROL_LETTERS_SCROLL: ");
        stringBuilder.append(this.mIsSupportVibrator);
        Log.d(str, stringBuilder.toString());
    }

    public void buildIndexer(boolean isLand, boolean isDigitLast) {
        int sizeNum = getSizeNum();
        String[] tempAlphaArrPortrait = (String[]) HwAlphaIndexResourceManager.getInstance().getPortraitDisplayAlphaIndex().toArray(new String[0]);
        String[] enTempAlphaArrPortrait = (String[]) HwAlphaIndexResourceManager.getRootPortraitDisplayAlphaIndex().toArray(new String[0]);
        String[] tempAlphaArrLand = (String[]) HwAlphaIndexResourceManager.getInstance().getLandscapeDisplayAlphaIndex().toArray(new String[0]);
        String[] enTempAlphaArrLand = (String[]) HwAlphaIndexResourceManager.getRootLandscapeDisplayAlphaIndex().toArray(new String[0]);
        String[] tempAlphaArr;
        if (this.mHasNativeIndexer) {
            String[] enTempAlphaArr;
            if (isLand) {
                if (sizeNum >= 18) {
                    tempAlphaArr = tempAlphaArrLand;
                    enTempAlphaArr = enTempAlphaArrLand;
                } else {
                    HwAlphaIndexResourceManager.getInstance();
                    List<String> list = HwAlphaIndexResourceManager.populateBulletAlphaIndex(sizeNum, Arrays.asList(tempAlphaArrPortrait));
                    HwAlphaIndexResourceManager.getInstance();
                    tempAlphaArr = (String[]) getDisplayFromComplete(list).toArray(new String[0]);
                    enTempAlphaArr = (String[]) getDisplayFromComplete(HwAlphaIndexResourceManager.populateBulletAlphaIndex(sizeNum, Arrays.asList(enTempAlphaArrPortrait))).toArray(new String[0]);
                }
            } else if (sizeNum == PORTRAIT_ALPHA_COUNT_MAX) {
                tempAlphaArr = tempAlphaArrPortrait;
                enTempAlphaArr = enTempAlphaArrPortrait;
            } else {
                tempAlphaArr = tempAlphaArrLand;
                enTempAlphaArr = enTempAlphaArrLand;
            }
            this.ALPHABETS = new String[(tempAlphaArr.length + 2)];
            this.EN_ALPHABETS = new String[(enTempAlphaArr.length + 2)];
            if (isDigitLast) {
                this.ALPHABETS[this.ALPHABETS.length - 1] = DIGIT_LABEL;
                this.EN_ALPHABETS[this.EN_ALPHABETS.length - 1] = DIGIT_LABEL;
                System.arraycopy(tempAlphaArr, 0, this.ALPHABETS, 0, tempAlphaArr.length);
                this.mFirstNativeIndex = 0;
                this.EN_ALPHABETS[0] = this.ALPHABETS[this.mFirstNativeIndex];
                System.arraycopy(enTempAlphaArr, 0, this.EN_ALPHABETS, 1, enTempAlphaArr.length);
                this.ALPHABETS[this.ALPHABETS.length - 2] = this.EN_ALPHABETS[this.EN_ALPHABETS.length - 2];
            } else {
                this.ALPHABETS[0] = DIGIT_LABEL;
                this.EN_ALPHABETS[0] = DIGIT_LABEL;
                System.arraycopy(tempAlphaArr, 0, this.ALPHABETS, 1, tempAlphaArr.length);
                this.mFirstNativeIndex = 1;
                this.EN_ALPHABETS[1] = this.ALPHABETS[this.mFirstNativeIndex];
                System.arraycopy(enTempAlphaArr, 0, this.EN_ALPHABETS, 2, enTempAlphaArr.length);
                this.ALPHABETS[this.ALPHABETS.length - 1] = this.EN_ALPHABETS[this.EN_ALPHABETS.length - 1];
            }
        } else {
            if (isLand) {
                if (sizeNum >= 18) {
                    tempAlphaArr = tempAlphaArrLand;
                } else {
                    HwAlphaIndexResourceManager.getInstance();
                    tempAlphaArr = (String[]) getDisplayFromComplete(HwAlphaIndexResourceManager.populateBulletAlphaIndex(sizeNum, Arrays.asList(tempAlphaArrPortrait))).toArray(new String[0]);
                }
            } else if (sizeNum == PORTRAIT_ALPHA_COUNT_MAX) {
                tempAlphaArr = tempAlphaArrPortrait;
            } else {
                tempAlphaArr = tempAlphaArrLand;
            }
            this.ALPHABETS = new String[(tempAlphaArr.length + 1)];
            if (isDigitLast) {
                this.ALPHABETS[this.ALPHABETS.length - 1] = DIGIT_LABEL;
                System.arraycopy(tempAlphaArr, 0, this.ALPHABETS, 0, tempAlphaArr.length);
            } else {
                this.ALPHABETS[0] = DIGIT_LABEL;
                System.arraycopy(tempAlphaArr, 0, this.ALPHABETS, 1, tempAlphaArr.length);
            }
        }
        if (!this.mHasNativeIndexer || this.mIsNativeIndexerShown) {
            this.mAlphabet = new ArrayList(Arrays.asList(this.ALPHABETS));
        } else {
            this.mAlphabet = new ArrayList(Arrays.asList(this.EN_ALPHABETS));
        }
        this.mIsLandscape = isLand;
        invalidate();
    }

    private int getSizeNum() {
        int sizeNum = (int) (((float) (((this.height - (this.mBottomGap * 2)) - getPaddingBottom()) - getPaddingTop())) / this.mAlphaTextSize);
        if (sizeNum > PORTRAIT_ALPHA_COUNT_MAX) {
            return PORTRAIT_ALPHA_COUNT_MAX;
        }
        if (sizeNum <= PORTRAIT_ALPHA_COUNT_MAX && sizeNum > 12) {
            return 18;
        }
        if (sizeNum <= 12 && sizeNum > LANDSCAPE_ALPHA_COUNT_HH) {
            return LANDSCAPE_ALPHA_COUNT_REDUCE;
        }
        if (sizeNum > LANDSCAPE_ALPHA_COUNT_HH || sizeNum <= 8) {
            return 6;
        }
        return LANDSCAPE_ALPHA_COUNT_HH;
    }

    private List<String> getDisplayFromComplete(List<String> completeList) {
        List<String> ret = new ArrayList();
        for (String item : completeList) {
            if (item.split(" ").length > 1) {
                ret.add(BULLET_CHAR);
            } else {
                ret.add(item);
            }
        }
        return ret;
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.height = getMeasuredHeight();
        buildIndexer(this.mIsLandscape, this.mIsDigitLast);
    }

    public void setListViewAttachTo(ListView listView) {
        this.mListView = listView;
        if (!this.mIsInit) {
            Adapter adapter = this.mListView.getAdapter();
            if (adapter instanceof HwSortedTextListAdapter) {
                this.mIsDigitLast = ((HwSortedTextListAdapter) adapter).isDigitLast();
            }
            buildIndexer(this.mIsLandscape, this.mIsDigitLast);
            this.mIsInit = true;
        }
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        calculateVariables();
        int specWidth = ResLoaderUtil.getDimensionPixelSize(getContext(), "alphaindexer_view_width");
        int right = getRight();
        int left = getLeft();
        int viewWidth = right - left;
        if (viewWidth > specWidth) {
            right -= (viewWidth - specWidth) / 2;
            left += (viewWidth - specWidth) / 2;
        }
        setRight(right);
        setLeft(left);
        int width = getWidth();
        int size = this.mAlphabet.size();
        int highlightPos = this.choose == -1 ? getHighlightPos() : this.choose;
        for (int i = 0; i < size; i++) {
            float yPos;
            this.mPaint.setColor(this.mInactiveAlphaColor);
            this.mPaint.setAntiAlias(true);
            this.mPaint.setTextSize(this.mAlphaTextSize);
            this.mPaint.setTypeface(Typeface.create("sans-serif", 0));
            if (i == highlightPos) {
                this.mPaint.setColor(this.mSelectedAlphaColor);
                this.mPaint.setTypeface(Typeface.create("sans-serif-medium", 0));
            }
            String displayChar = ((String) this.mAlphabet.get(i)).replace("劃", "");
            float xPos = (((float) width) / 2.0f) - (this.mPaint.measureText(displayChar) / 2.0f);
            if (this.mTopGap == this.mBottomGap) {
                yPos = ((((float) (i + 1)) * (this.mAlphaTextSize + this.mGapBetweenAlpha)) + ((float) this.mTopGap)) - this.mGapBetweenAlpha;
            } else {
                yPos = (((float) (i + 1)) * (this.mAlphaTextSize + this.mGapBetweenAlpha)) + ((float) this.mTopGap);
            }
            canvas.drawText(displayChar, xPos, yPos, this.mPaint);
            this.mPaint.reset();
        }
    }

    private int getHighlightPos() {
        if (this.mSectionText != null) {
            int len = this.mAlphabet.size();
            for (int i = 0; i < len; i++) {
                if (equalsChar((String) this.mAlphabet.get(i), this.mSectionText, i)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void calculateVariables() {
        int size = this.mAlphabet.size();
        float textSize = this.mAlphaTextSize;
        if (size < 6 && this.mLessBottomGap > 0) {
            this.mBottomGap = ((6 - size) * 4) * this.mLessBottomGap;
        }
        int bottom = this.mBottomGap;
        this.mGapBetweenAlpha = ((((float) (this.mListView.getHeight() - bottom)) - (((float) size) * textSize)) - ((float) bottom)) / ((float) (size - 1));
        this.mTopGap = ((float) bottom) > this.mGapBetweenAlpha ? (int) (((float) bottom) - this.mGapBetweenAlpha) : bottom;
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float y = event.getY();
        if ((action == 0 || action == 2) && (y < ((float) this.mTopGap) || y > ((float) (getHeight() - this.mTopGap)))) {
            return true;
        }
        int c = (int) (((y - ((float) this.mTopGap)) / ((float) (getHeight() - (this.mTopGap * 2)))) * ((float) this.mAlphabet.size()));
        if (this.mAlphabet.size() == 0) {
            return true;
        }
        float gridHeigtht = ((float) (getHeight() - (2 * this.mTopGap))) / ((float) this.mAlphabet.size());
        switch (action) {
            case 0:
                if (c >= 0 && c < this.mAlphabet.size()) {
                    performItemClicked(c);
                    invalidate();
                }
                if (this.mIsSupportVibrator) {
                    this.mLastY = event.getY();
                    break;
                }
                break;
            case 1:
                dismissPopup();
                this.choose = -1;
                invalidate();
                if (this.mIsSupportVibrator) {
                    this.mVibratorEx.stopHwVibrator("haptic.control.letters_scroll");
                    break;
                }
                break;
            case 2:
                if (c >= 0 && c < this.mAlphabet.size()) {
                    performItemClicked(c);
                    invalidate();
                    if (this.mIsSupportVibrator && Math.abs(event.getY() - this.mLastY) >= gridHeigtht) {
                        this.mVibratorEx.setHwVibrator("haptic.control.letters_scroll");
                        this.mLastY = event.getY();
                        break;
                    }
                }
            case 3:
                dismissPopup();
                return false;
        }
        return true;
    }

    private void performItemClicked(int item) {
        if (debug) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("item: ");
            stringBuilder.append(item);
            stringBuilder.append(", ALPHABETS[");
            stringBuilder.append(item);
            stringBuilder.append("]: ");
            stringBuilder.append((String) this.mAlphabet.get(item));
            Log.e(str, stringBuilder.toString());
        }
        if (this.mOnItemClickListener != null) {
            this.mOnItemClickListener.onItemClick((String) this.mAlphabet.get(item), item);
        }
    }

    private void toggleIndexer(boolean isNative) {
        List arrayList;
        Animation animation;
        this.mAlphabet.clear();
        if (isNative) {
            arrayList = new ArrayList(Arrays.asList(this.EN_ALPHABETS));
        } else {
            arrayList = new ArrayList(Arrays.asList(this.ALPHABETS));
        }
        this.mAlphabet = arrayList;
        this.mIsNativeIndexerShown = isNative ^ 1;
        int bottom2topAnimId = ResLoaderUtil.getInstance().getIdentifier(getContext(), "anim", "translate_bottom2top");
        int top2bottomAnimId = ResLoaderUtil.getInstance().getIdentifier(getContext(), "anim", "translate_top2bottom");
        if (isNative) {
            animation = AnimationUtils.loadAnimation(this.mContext, bottom2topAnimId);
        } else {
            animation = AnimationUtils.loadAnimation(this.mContext, top2bottomAnimId);
        }
        setAnimation(animation);
        calculateVariables();
        startAnimation(animation);
    }

    public Object[] getSections() {
        if (this.mListView != null) {
            Adapter adapter = this.mListView.getAdapter();
            if (adapter instanceof HwSortedTextListAdapter) {
                return ((HwSortedTextListAdapter) adapter).getSections();
            }
        }
        return new String[0];
    }

    private boolean isInNativeSection() {
        boolean z = false;
        if (this.mSectionText == null) {
            return false;
        }
        Object[] sections = getSections();
        String strDetect = this.mSectionText;
        if (sections != null && DIGIT_LABEL.equals(strDetect)) {
            if (this.mIsDigitLast) {
                return false;
            }
            if (0 + 1 < sections.length) {
                strDetect = sections[0 + 1];
            }
        }
        if (Collator.getInstance().compare(strDetect, this.mFirstEnglishAlpha) < 0) {
            z = true;
        }
        return z;
    }

    private void updateIndexer() {
        if (!this.mHasNativeIndexer) {
            return;
        }
        if (isInNativeSection()) {
            if (!this.mIsNativeIndexerShown) {
                toggleIndexer(false);
            }
        } else if (this.mIsNativeIndexerShown) {
            toggleIndexer(true);
        }
    }

    public boolean needSwitchIndexer(int pos) {
        if (this.mHasNativeIndexer) {
            if (pos == this.mFirstNativeIndex && !this.mIsNativeIndexerShown) {
                return true;
            }
            if (pos == (this.mIsDigitLast ? this.mAlphabet.size() - 2 : this.mAlphabet.size() - 1) && this.mIsNativeIndexerShown) {
                return true;
            }
        }
        return false;
    }

    public void showPopup(String text) {
        if (this.mShowPopup) {
            int height;
            this.mHandler.removeCallbacks(this.dismissRunnable);
            if (this.mPopupWindow == null) {
                this.mPopupText = new TextView(getContext());
                this.mPopupText.setTextSize(0, (float) ResLoaderUtil.getDimensionPixelSize(getContext(), "emui_master_title_2"));
                if (this.mPopupBgDrawable != null) {
                    this.mPopupText.setBackground(this.mPopupBgDrawable);
                }
                this.mPopupText.setTextColor(this.mPopupTextColor);
                this.mPopupText.setTypeface(Typeface.DEFAULT_BOLD);
                this.mPopupText.setGravity(17);
                height = ResLoaderUtil.getDimensionPixelSize(getContext(), "alphaindexer_popup_height");
                this.mPopupWindow = new PopupWindow(this.mPopupText, height, height);
                this.mPopupWindow.setAnimationStyle(ResLoaderUtil.getInstance().getIdentifier(getContext(), "style", "Animation_PopupWindow_Emui"));
            }
            if ((this.choose == -1 && text != null) || (this.choose != -1 && this.choose < this.mAlphabet.size() && equalsChar((String) this.mAlphabet.get(this.choose), text, this.choose))) {
                int popupHorizontalPos;
                this.mPopupText.setText(text);
                height = ResLoaderUtil.getDimensionPixelSize(getContext(), "alphaindexer_popup_horizontal_offset");
                int[] location = new int[2];
                getLocationOnScreen(location);
                int specWidth = ResLoaderUtil.getDimensionPixelSize(getContext(), "alphaindexer_view_width");
                int viewLeft = location[0];
                int viewRight = location[0] + specWidth;
                if (getParent().getLayoutDirection() == 1) {
                    popupHorizontalPos = viewRight + height;
                } else {
                    popupHorizontalPos = (viewLeft - height) - ResLoaderUtil.getDimensionPixelSize(getContext(), "alphaindexer_popup_height");
                }
                this.mPopupWindow.showAtLocation(getRootView(), 19, popupHorizontalPos, 0);
            }
        }
    }

    public void showPopup() {
        showPopup(this.mSectionText);
    }

    public void dismissPopup() {
        if (this.mShowPopup) {
            this.mHandler.postDelayed(this.dismissRunnable, 800);
        }
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mPopupWindow != null) {
            this.mPopupWindow.dismiss();
        }
    }

    public boolean hasNativeIndexer() {
        return this.mHasNativeIndexer;
    }

    public boolean isNativeIndexerShow() {
        return this.mIsNativeIndexerShown;
    }

    public Drawable getPopupWindowBgDrawable() {
        return this.mPopupBgDrawable;
    }

    public void setPopupWindowBgDrawable(Drawable drawable) {
        this.mPopupBgDrawable = drawable;
    }

    public void setInactiveAlphaColor(int color) {
        this.mInactiveAlphaColor = color;
    }

    public void setSelectedAlphaColor(int color) {
        this.mSelectedAlphaColor = color;
    }

    public void setPopupTextColor(int color) {
        this.mPopupTextColor = color;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }

    public void setOverLayInfo(String sectionName) {
        if ("".equals(sectionName)) {
            this.mSectionText = "@";
            return;
        }
        this.mSectionText = sectionName;
        updateIndexer();
    }

    public void setOverLayInfo(int sectionPos, String sectionName) {
        this.choose = sectionPos;
        setOverLayInfo(sectionName);
    }

    public boolean equalsChar(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        Collator coll = Collator.getInstance();
        coll.setStrength(0);
        return coll.equals(a, b);
    }

    public boolean equalsChar(String a, String b, int i) {
        if (a == null || b == null || i < 0 || i >= this.mAlphabet.size()) {
            return false;
        }
        if (!a.equals(BULLET_CHAR)) {
            return equalsChar(a, b);
        }
        this.mFullAlphabet.clear();
        if (!this.mIsDigitLast) {
            this.mFullAlphabet.add(DIGIT_LABEL);
        }
        int sizeNum = getSizeNum();
        List<String> alphaPortrait = HwAlphaIndexResourceManager.getInstance().getPortraitCompleteAlphaIndex();
        HwAlphaIndexResourceManager.getInstance();
        List<String> rootAlphaPortrait = HwAlphaIndexResourceManager.getRootPortraitDisplayAlphaIndex();
        List<String> alphaLand = HwAlphaIndexResourceManager.getInstance().getLandscapeCompleteAlphaIndex();
        List list;
        if (this.mHasNativeIndexer) {
            if (this.mIsNativeIndexerShown) {
                if (this.mIsLandscape) {
                    if (sizeNum >= 18) {
                        this.mFullAlphabet.addAll(alphaLand);
                    } else {
                        list = this.mFullAlphabet;
                        HwAlphaIndexResourceManager.getInstance();
                        list.addAll(HwAlphaIndexResourceManager.populateBulletAlphaIndex(sizeNum, alphaPortrait));
                    }
                } else if (sizeNum == PORTRAIT_ALPHA_COUNT_MAX) {
                    this.mFullAlphabet.addAll(alphaPortrait);
                } else {
                    this.mFullAlphabet.addAll(alphaLand);
                }
                this.mFullAlphabet.add(this.mLstEnglishAlpha);
            } else {
                this.mFullAlphabet.add(this.ALPHABETS[this.mFirstNativeIndex]);
                if (this.mIsLandscape) {
                    if (sizeNum >= 18) {
                        this.mFullAlphabet.addAll(HwAlphaIndexResourceManager.getRootLandscapeCompleteAlphaIndex());
                    } else {
                        list = this.mFullAlphabet;
                        HwAlphaIndexResourceManager.getInstance();
                        list.addAll(HwAlphaIndexResourceManager.populateBulletAlphaIndex(sizeNum, rootAlphaPortrait));
                    }
                } else if (sizeNum == PORTRAIT_ALPHA_COUNT_MAX) {
                    this.mFullAlphabet.addAll(rootAlphaPortrait);
                } else {
                    this.mFullAlphabet.addAll(HwAlphaIndexResourceManager.getRootLandscapeCompleteAlphaIndex());
                }
            }
        } else if (this.mIsLandscape) {
            if (sizeNum >= 18) {
                this.mFullAlphabet.addAll(alphaLand);
            } else {
                list = this.mFullAlphabet;
                HwAlphaIndexResourceManager.getInstance();
                list.addAll(HwAlphaIndexResourceManager.populateBulletAlphaIndex(sizeNum, alphaPortrait));
            }
        } else if (sizeNum == PORTRAIT_ALPHA_COUNT_MAX) {
            this.mFullAlphabet.addAll(alphaPortrait);
        } else {
            this.mFullAlphabet.addAll(alphaLand);
        }
        if (this.mIsDigitLast) {
            this.mFullAlphabet.add(DIGIT_LABEL);
        }
        String[] bullet_list = ((String) this.mFullAlphabet.get(i)).split(" ");
        for (String equalsChar : bullet_list) {
            if (equalsChar(equalsChar, b)) {
                return true;
            }
        }
        return false;
    }

    public int getPositionBySection(String sectionName) {
        if (sectionName == null) {
            return -1;
        }
        int len = this.mAlphabet.size();
        for (int i = 0; i < len; i++) {
            if (equalsChar((String) this.mAlphabet.get(i), sectionName, i)) {
                return i;
            }
        }
        return -1;
    }
}
