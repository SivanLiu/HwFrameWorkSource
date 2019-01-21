package huawei.android.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.android.internal.R;
import huawei.android.widget.loader.ResLoaderUtil;
import java.util.Locale;

public class SeekBar extends android.widget.SeekBar {
    private static final int OFFSET_LABELLING = 16;
    private static final int OFFSET_TRACK = 4;
    private static final int SEEKBAR_HEIGHT_LABELLING = 48;
    private int mBubbleTipBgId;
    private Drawable mCircleDr;
    private Context mContext;
    private boolean mIsShowPopWindow;
    private Paint mPaintCircle;
    private Paint mPaintText;
    private int mProgress;
    private PopupWindow mPw;
    private Resources mRes;
    private boolean mSetLabelling;
    private boolean mSetTip;
    private int mSingleTipBgId;
    private int mStepNum;
    private int mStepTextColor;
    private int mStepTextSize;
    private float mStepValue;
    private Rect mTempRect;
    private int mTipBgId;
    private String mTipText;
    private int mTipTextColor;
    private int mTipTextSize;
    private TextView mTv;
    private int mTvHeight;
    private int mTvWidth;
    private int mXmlProgress;

    public SeekBar(Context context) {
        this(context, null);
    }

    public SeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 16842875);
    }

    public SeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mSetTip = false;
        this.mSetLabelling = false;
        this.mIsShowPopWindow = false;
        this.mTipText = null;
        this.mProgress = 0;
        this.mTempRect = new Rect();
        this.mContext = context;
        TypedArray progresssTypedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressBar, defStyleAttr, defStyleRes);
        this.mXmlProgress = progresssTypedArray.getInt(3, this.mProgress);
        progresssTypedArray.recycle();
        TypedArray showTextTypedArray = context.obtainStyledAttributes(attrs, R.styleable.Switch, defStyleAttr, defStyleRes);
        this.mIsShowPopWindow = showTextTypedArray.getBoolean(11, false);
        showTextTypedArray.recycle();
        this.mRes = ResLoaderUtil.getResources(context);
        this.mStepTextSize = ResLoaderUtil.getDimensionPixelSize(context, "emui_master_caption_2");
        this.mTipTextSize = ResLoaderUtil.getDimensionPixelSize(context, "emui_master_body_2");
        if (HwWidgetUtils.isHwLightTheme(context)) {
            this.mBubbleTipBgId = ResLoaderUtil.getDrawableId(context, "seekbar_info_bubble_emui");
            this.mSingleTipBgId = ResLoaderUtil.getDrawableId(context, "seekbar_info_single_emui");
            this.mTipTextColor = ResLoaderUtil.getColor(context, "emui_white");
            this.mStepTextColor = ResLoaderUtil.getColor(context, "emui_color_gray_7");
        } else if (HwWidgetUtils.isHwDarkTheme(context)) {
            this.mBubbleTipBgId = ResLoaderUtil.getDrawableId(context, "seekbar_info_bubble_emui_dark");
            this.mSingleTipBgId = ResLoaderUtil.getDrawableId(context, "seekbar_info_single_emui_dark");
            this.mTipTextColor = -16777216;
            this.mStepTextColor = -1;
        } else if (HwWidgetUtils.isHwEmphasizeTheme(context)) {
            this.mBubbleTipBgId = ResLoaderUtil.getDrawableId(context, "seekbar_info_bubble_emui_emphasize");
            this.mSingleTipBgId = ResLoaderUtil.getDrawableId(context, "seekbar_info_single_emui_emphasize");
            this.mTipTextColor = -1;
            this.mStepTextColor = -1;
        }
        if (this.mIsShowPopWindow) {
            initTip();
            this.mTipBgId = this.mBubbleTipBgId;
        }
    }

    protected void drawableStateChanged() {
        super.drawableStateChanged();
        Drawable progressDrawable = getProgressDrawable();
        if (progressDrawable != null) {
            progressDrawable.setAlpha(isEnabled() ? 255 : 76);
        }
        if (this.mCircleDr != null) {
            this.mCircleDr.setState(getDrawableState());
        }
    }

    public void setTip(boolean setLabelling, int stepNum, boolean isBubbleTip) {
        if (stepNum != 0) {
            this.mSetTip = true;
            this.mSetLabelling = setLabelling;
            this.mStepNum = stepNum;
            this.mStepValue = (((float) (getMax() - getMin())) + 0.0f) / ((float) this.mStepNum);
            this.mTipBgId = isBubbleTip ? this.mBubbleTipBgId : this.mSingleTipBgId;
            this.mCircleDr = this.mRes.getDrawable(ResLoaderUtil.getDrawableId(getContext(), "seekbar_circle_emui"));
            initTip();
            this.mPaintCircle = new Paint();
            this.mPaintCircle.setAntiAlias(true);
            this.mPaintText = new Paint();
            this.mPaintText.setAntiAlias(true);
            this.mPaintText.setColor(this.mStepTextColor);
            this.mPaintText.setTextSize((float) this.mStepTextSize);
            this.mPaintText.setTypeface(Typeface.SANS_SERIF);
            if (this.mSetLabelling) {
                getLayoutParams().height = dip2px(SEEKBAR_HEIGHT_LABELLING);
            }
            setProgress(this.mXmlProgress);
            invalidate();
        }
    }

    public void setTipText(String tipText) {
        if (this.mTipBgId == this.mBubbleTipBgId && this.mIsShowPopWindow && tipText != null) {
            this.mTipText = tipText;
            this.mTv.setText(this.mTipText);
        }
    }

    private void initTip() {
        this.mTv = new TextView(this.mContext);
        this.mTv.setTextColor(this.mTipTextColor);
        this.mTv.setTextSize(0, (float) this.mTipTextSize);
        this.mTv.setTypeface(Typeface.SANS_SERIF);
        if (this.mTipBgId == this.mSingleTipBgId) {
            Drawable tipBgDra = this.mRes.getDrawable(this.mTipBgId);
            if (tipBgDra != null) {
                this.mTv.setLayoutParams(new LayoutParams(tipBgDra.getIntrinsicWidth(), tipBgDra.getIntrinsicHeight()));
            } else {
                this.mTv.setLayoutParams(new LayoutParams(-2, -2));
            }
            this.mTv.setGravity(17);
        } else {
            this.mTv.setLayoutParams(new LayoutParams(-2, -2));
            this.mTv.setGravity(17);
        }
        this.mTv.setSingleLine(true);
        this.mPw = new PopupWindow(this.mTv, -2, -2, false);
    }

    protected synchronized void onDraw(Canvas canvas) {
        if (this.mSetTip && this.mSetLabelling) {
            int saveCount = canvas.save();
            canvas.translate(0.0f, (float) (0 - dip2px(4)));
            super.onDraw(canvas);
            canvas.restoreToCount(saveCount);
        } else {
            super.onDraw(canvas);
        }
    }

    protected void drawTrackEx(Canvas canvas) {
        super.drawTrackEx(canvas);
        if (this.mSetTip) {
            drawCircles(canvas);
        }
    }

    protected void onHwStartTrackingTouch() {
        if (this.mIsShowPopWindow) {
            this.mTv.setBackgroundResource(this.mTipBgId);
            this.mPw.showAsDropDown(this);
            onProgressRefreshEx(getScaleEx(), true, getProgress());
        }
    }

    protected void onHwStopTrackingTouch() {
        if (this.mIsShowPopWindow) {
            this.mPw.dismiss();
        }
    }

    protected void onProgressRefreshEx(float scale, boolean fromUser, int progress) {
        if (this.mSetTip) {
            progress = Math.round(this.mStepValue * ((float) ((int) ((((float) progress) / this.mStepValue) + 0.5f))));
            setProgress(progress);
        }
        if (this.mIsShowPopWindow) {
            this.mTv.setText(String.valueOf(progress));
            updateTip();
        }
    }

    public synchronized void setProgress(int progress) {
        if (this.mSetTip) {
            progress = Math.round(this.mStepValue * ((float) ((int) ((((float) progress) / this.mStepValue) + 0.5f))));
        }
        this.mProgress = progress;
        super.setProgress(this.mProgress);
    }

    private void updateTip() {
        updatePopWidth();
        int available = (getWidth() - this.mPaddingLeft) - this.mPaddingRight;
        int paddingLeft = getPaddingLeft();
        float f = (float) available;
        float scaleEx = (!isLayoutRtl() || "ur".equals(Locale.getDefault().getLanguage())) ? getScaleEx() : 1.0f - getScaleEx();
        paddingLeft = (paddingLeft + ((int) (((double) (f * scaleEx)) + 0.5d))) - (this.mTvWidth / 2);
        int yoff = (0 - getMeasuredHeight()) - this.mTvHeight;
        if (this.mPw.isShowing()) {
            this.mPw.update(this, paddingLeft, yoff, this.mTvWidth, this.mTvHeight);
        }
    }

    private void updatePopWidth() {
        this.mTv.measure(MeasureSpec.makeMeasureSpec(0, 0), MeasureSpec.makeMeasureSpec(0, 0));
        this.mTvWidth = this.mTv.getMeasuredWidth();
        this.mTvHeight = this.mTv.getMeasuredHeight();
    }

    private void drawCircles(Canvas canvas) {
        Canvas canvas2 = canvas;
        if (this.mCircleDr != null) {
            Bitmap bmp = drawableToBitmap(this.mCircleDr);
            int circleWidth = this.mCircleDr.getIntrinsicWidth();
            int circleHeight = this.mCircleDr.getIntrinsicHeight();
            int width = getWidth();
            float trackLength = (float) ((width - getPaddingLeft()) - getPaddingRight());
            int stepNum = this.mStepNum;
            int circleTop = (((getHeight() - this.mPaddingTop) - this.mPaddingBottom) / 2) - (circleHeight / 2);
            int circleLeft = getPaddingLeft();
            int i = 1;
            if (stepNum > 1) {
                float stepLength = trackLength / ((float) stepNum);
                int width2;
                float trackLength2;
                if (this.mSetLabelling) {
                    int i2 = 0;
                    while (i2 <= stepNum) {
                        if (!isLayoutRtl() || "ur".equals(Locale.getDefault().getLanguage())) {
                            i = Math.round((this.mStepValue * ((float) i2)) + ((float) getMin()));
                        } else {
                            i = Math.round((this.mStepValue * ((float) (stepNum - i2))) + ((float) getMin()));
                        }
                        width2 = width;
                        int circleHeight2 = circleHeight;
                        trackLength2 = trackLength;
                        canvas2.drawText(String.valueOf(i), (((float) circleLeft) + (((float) i2) * stepLength)) - ((float) (getTextWidth(String.valueOf(i)) / 2)), (float) (((circleTop + circleHeight) + dip2px(OFFSET_LABELLING)) + getTextHeight(String.valueOf(i))), this.mPaintText);
                        i2++;
                        width = width2;
                        circleHeight = circleHeight2;
                        trackLength = trackLength2;
                    }
                    width2 = width;
                    trackLength2 = trackLength;
                    return;
                }
                width2 = width;
                trackLength2 = trackLength;
                while (true) {
                    circleHeight = i;
                    if (circleHeight < stepNum) {
                        canvas2.drawBitmap(bmp, (((float) circleLeft) + (((float) circleHeight) * stepLength)) - ((float) (circleWidth / 2)), (float) circleTop, this.mPaintCircle);
                        i = circleHeight + 1;
                    } else {
                        return;
                    }
                }
            }
        }
    }

    private static int dip2px(int dp) {
        return (int) TypedValue.applyDimension(1, (float) dp, Resources.getSystem().getDisplayMetrics());
    }

    private int getTextWidth(String str) {
        if (TextUtils.isEmpty(str)) {
            return -1;
        }
        this.mPaintText.getTextBounds(str, 0, str.length(), this.mTempRect);
        return this.mTempRect.width();
    }

    private int getTextHeight(String str) {
        if (TextUtils.isEmpty(str)) {
            return -1;
        }
        this.mPaintText.getTextBounds(str, 0, str.length(), this.mTempRect);
        return this.mTempRect.height();
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        try {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }
}
