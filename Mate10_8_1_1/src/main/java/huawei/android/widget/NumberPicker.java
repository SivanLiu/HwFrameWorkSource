package huawei.android.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hwcontrol.HwWidgetFactory;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.NumberPicker.Formatter;
import dalvik.system.DexClassLoader;
import huawei.android.widget.DecouplingUtil.ReflectUtil;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class NumberPicker extends android.widget.NumberPicker {
    private static final String GOOGLE_NP_CLASSNAME = "android.widget.NumberPicker";
    private static final String HW_CHINESE_MEDIUM_TYPEFACE = "HwChinese-medium";
    private static final String TAG = "NumberPicker";
    private static String apkPath = "/system/framework/immersion.jar";
    private static String dexOutputDir = "/data/data/com.immersion/";
    private static DexClassLoader mClassLoader = null;
    private int FLING_BACKWARD;
    private int FLING_FOWARD;
    private int FLING_STOP;
    private boolean isVibrateImplemented;
    private Context mContext_Vibrate;
    private final Typeface mDefaultTypeface;
    private final int mEdgeOffset;
    private List<android.widget.NumberPicker> mFireList;
    private int mFlingDirection;
    private Class mGNumberPickerClass;
    private final Typeface mHwChineseMediumTypeface;
    private boolean mIsDarkHwTheme;
    private boolean mIsLongPress;
    private int mNormalTextColor;
    private float mNormalTextSize;
    private int mSelectorOffset;
    private int mSelectorTextColor;
    private float mSelectorTextSize;
    private int mSmallTextColor;

    public NumberPicker(Context context) {
        this(context, null);
    }

    public NumberPicker(Context context, AttributeSet attrs) {
        this(context, attrs, context.getResources().getIdentifier("numberPickerStyle", "attr", "android"));
    }

    public NumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NumberPicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mSelectorOffset = 0;
        this.mSelectorTextSize = 0.0f;
        this.mNormalTextSize = 0.0f;
        this.mSelectorTextColor = 0;
        this.mSmallTextColor = 0;
        this.mNormalTextColor = 0;
        this.mIsDarkHwTheme = false;
        this.mFireList = new ArrayList();
        this.isVibrateImplemented = SystemProperties.getBoolean("ro.config.touch_vibrate", false);
        this.FLING_FOWARD = 0;
        this.FLING_BACKWARD = 1;
        this.FLING_STOP = 2;
        this.mFlingDirection = this.FLING_STOP;
        this.mIsLongPress = false;
        initClass();
        OnClickListener onClickListener = new OnClickListener() {
            public void onClick(View v) {
                ReflectUtil.callMethod(this, "hideSoftInput", null, null, NumberPicker.this.mGNumberPickerClass);
                EditText inputText = (EditText) ReflectUtil.getObject(this, "mInputText", NumberPicker.this.mGNumberPickerClass);
                if (inputText != null) {
                    inputText.clearFocus();
                    Class<?>[] changeValueByOneArgsClass = new Class[]{Boolean.TYPE};
                    if (v.getId() == 16908969) {
                        null[0] = Boolean.valueOf(true);
                    } else {
                        null[0] = Boolean.valueOf(false);
                    }
                    ReflectUtil.callMethod(this, "changeValueByOne", changeValueByOneArgsClass, null, NumberPicker.this.mGNumberPickerClass);
                    NumberPicker.this.setLongPressState(false);
                }
            }
        };
        OnLongClickListener onLongClickListener = new OnLongClickListener() {
            public boolean onLongClick(View v) {
                NumberPicker.this.setLongPressState(true);
                ReflectUtil.callMethod(this, "hideSoftInput", null, null, NumberPicker.this.mGNumberPickerClass);
                EditText inputText = (EditText) ReflectUtil.getObject(this, "mInputText", NumberPicker.this.mGNumberPickerClass);
                if (inputText != null) {
                    inputText.clearFocus();
                    Class<?>[] postChangeCurrentByOneFromLongPressArgsClass = new Class[]{Boolean.TYPE, Long.TYPE};
                    if (v.getId() == 16908969) {
                        null[0] = Boolean.valueOf(true);
                    } else {
                        null[0] = Boolean.valueOf(false);
                    }
                    null[1] = Integer.valueOf(0);
                    ReflectUtil.callMethod(this, "postChangeCurrentByOneFromLongPress", postChangeCurrentByOneFromLongPressArgsClass, null, NumberPicker.this.mGNumberPickerClass);
                }
                return true;
            }
        };
        boolean hasSelectorWheel = ((Boolean) ReflectUtil.getObject(this, "mHasSelectorWheel", this.mGNumberPickerClass)).booleanValue();
        ImageButton incrementButton = (ImageButton) ReflectUtil.getObject(this, "mIncrementButton", this.mGNumberPickerClass);
        ImageButton decrementButton = (ImageButton) ReflectUtil.getObject(this, "mDecrementButton", this.mGNumberPickerClass);
        if (!(incrementButton == null || decrementButton == null || (hasSelectorWheel ^ 1) == 0)) {
            incrementButton.setOnClickListener(onClickListener);
            incrementButton.setOnLongClickListener(onLongClickListener);
            decrementButton.setOnClickListener(onClickListener);
            decrementButton.setOnLongClickListener(onLongClickListener);
        }
        initialNumberPicker(context, attrs);
        getSelectorWheelPaint().setColor(this.mNormalTextColor);
        this.mContext_Vibrate = context;
        this.mSelectorWheelItemCount = 5;
        this.SELECTOR_MIDDLE_ITEM_INDEX = this.mSelectorWheelItemCount / 2;
        this.mSelectorIndices = new int[this.mSelectorWheelItemCount];
        this.mDefaultTypeface = Typeface.create((String) null, 0);
        this.mHwChineseMediumTypeface = Typeface.create(HW_CHINESE_MEDIUM_TYPEFACE, 0);
        this.mEdgeOffset = context.getResources().getDimensionPixelSize(34472050);
        getInputText().setTypeface(this.mHwChineseMediumTypeface);
        getInputText().setTextColor(this.mSelectorTextColor);
        try {
            mClassLoader = new DexClassLoader(apkPath, dexOutputDir, null, ClassLoader.getSystemClassLoader());
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "fail get mClassLoader");
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!getHasSelectorWheel() || (isEnabled() ^ 1) != 0) {
            return false;
        }
        switch (event.getActionMasked()) {
            case 0:
                handleFireList();
                break;
        }
        return super.onInterceptTouchEvent(event);
    }

    protected void initializeFadingEdges() {
    }

    protected int getNormalTextColor(int color) {
        return this.mNormalTextColor;
    }

    protected void setSelectorColor(int i, int currentOffset, int initOffset, int index, int height, Paint paint) {
        int offset = currentOffset;
        offset = currentOffset + ((i - index) * height);
        if (offset <= initOffset - this.mSelectorOffset || offset >= this.mSelectorOffset + initOffset) {
            paint.setTextSize(this.mNormalTextSize);
            paint.setColor(this.mSmallTextColor);
            paint.setTypeface(this.mDefaultTypeface);
        } else {
            paint.setTextSize(this.mSelectorTextSize);
            paint.setColor(getAlphaGradient(initOffset, offset, this.mSelectorTextColor));
            paint.setTypeface(this.mHwChineseMediumTypeface);
        }
        if (i == 0 || this.mSelectorIndices.length - 1 == i) {
            paint.setAlpha(102);
        }
    }

    protected float adjustYPosition(int i, float y) {
        float ret = y;
        if (i == this.SELECTOR_MIDDLE_ITEM_INDEX) {
            return y - (this.mSelectorTextSize - this.mNormalTextSize);
        }
        return ret;
    }

    public void addFireList(android.widget.NumberPicker np) {
        this.mFireList.add(np);
    }

    protected int initializeSelectorElementHeight(int textSize, int selectorTextGapHeight) {
        return ((textSize * 5) / 3) + selectorTextGapHeight;
    }

    private void initialNumberPicker(Context context, AttributeSet attrs) {
        Resources res = context.getResources();
        this.mSelectorOffset = res.getDimensionPixelSize(34472047);
        this.mSelectorTextSize = (float) res.getDimensionPixelSize(34472048);
        this.mNormalTextSize = (float) res.getDimensionPixelSize(34472049);
        this.mSelectorTextColor = res.getColor(33882282);
        this.mIsDarkHwTheme = HwWidgetFactory.isHwDarkTheme(context);
        if (this.mIsDarkHwTheme) {
            this.mSmallTextColor = res.getColor(33882199);
            this.mNormalTextColor = res.getColor(33882200);
            return;
        }
        this.mSmallTextColor = res.getColor(33882194);
        this.mNormalTextColor = res.getColor(33882195);
    }

    private void handleFireList() {
        getInputText().setTextSize(0, this.mSelectorTextSize);
        int size = this.mFireList.size();
        for (int i = 0; i < size; i++) {
            android.widget.NumberPicker np = (android.widget.NumberPicker) this.mFireList.get(i);
            np.getInputText().setVisibility(0);
            np.invalidate();
        }
    }

    private int getAlphaGradient(int initOffset, int offset, int color) {
        float rate = 1.0f - (((float) Math.abs(initOffset - offset)) / ((float) this.mSelectorOffset));
        if (rate < 0.4f) {
            rate = 0.4f;
        }
        return (16777215 & color) | (((int) (((float) Color.alpha(color)) * rate)) << 24);
    }

    protected void playIvtEffect() {
        if (this.isVibrateImplemented && 1 == System.getInt(this.mContext_Vibrate.getContentResolver(), "touch_vibrate_mode", 1)) {
            if (this.mContext_Vibrate.checkCallingOrSelfPermission("android.permission.VIBRATE") != 0) {
                Log.e(TAG, "playIvtEffect Method requires android.Manifest.permission.VIBRATE permission");
                return;
            }
            try {
                Class<?> mClazz_vibetonzImpl = mClassLoader.loadClass("com.immersion.VibetonzImpl");
                Object object_vibetonzImpl = mClazz_vibetonzImpl.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
                mClazz_vibetonzImpl.getMethod("playIvtEffect", new Class[]{String.class}).invoke(object_vibetonzImpl, new Object[]{"NUMBERPICKER_ITEMSCROLL"});
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "ClassNotFoundException in reflect playIvtEffect in set object");
            } catch (NoSuchMethodException e2) {
                Log.e(TAG, "no field in reflect playIvtEffect in set object");
            } catch (IllegalAccessException e3) {
                e3.printStackTrace();
            } catch (IllegalArgumentException e4) {
                Log.e(TAG, "IllegalArgumentException in reflect playIvtEffect in set object");
            } catch (InvocationTargetException e5) {
                Log.e(TAG, "InvocationTargetException in reflect playIvtEffect in set object");
            } catch (RuntimeException e6) {
                Log.e(TAG, "RuntimeException in reflect playIvtEffect in set object");
            } catch (Exception e7) {
                Log.e(TAG, "Exception in reflect playIvtEffect in set object");
            }
        }
    }

    private void setLongPressState(boolean state) {
        this.mIsLongPress = state;
    }

    protected boolean needToPlayIvtEffectWhenScrolling(int scrollByY) {
        int mScrollState = ((Integer) ReflectUtil.getObject(this, "mScrollState", this.mGNumberPickerClass)).intValue();
        if (this.mIsLongPress || mScrollState != 1) {
            return false;
        }
        if (Math.abs(scrollByY) > 10) {
            return true;
        }
        return false;
    }

    protected void playIvtEffectWhenFling(int previous, int current) {
        int mScrollState = ((Integer) ReflectUtil.getObject(this, "mScrollState", this.mGNumberPickerClass)).intValue();
        if (!this.mIsLongPress && mScrollState == 2) {
            if (this.mFlingDirection == this.FLING_FOWARD) {
                if (current > previous) {
                    playIvtEffect();
                } else {
                    this.mFlingDirection = this.FLING_STOP;
                }
            } else if (this.mFlingDirection != this.FLING_BACKWARD) {
            } else {
                if (current < previous) {
                    playIvtEffect();
                } else {
                    this.mFlingDirection = this.FLING_STOP;
                }
            }
        }
    }

    protected void setFlingDirection(int velocityY) {
        if (velocityY > 0) {
            this.mFlingDirection = this.FLING_BACKWARD;
        } else {
            this.mFlingDirection = this.FLING_FOWARD;
        }
    }

    protected float adjustYCoordinate(int i, float y) {
        if (i == 0) {
            return y + ((float) this.mEdgeOffset);
        }
        if (this.mSelectorIndices.length - 1 == i) {
            return y - ((float) this.mEdgeOffset);
        }
        return y;
    }

    private void initClass() {
        if (this.mGNumberPickerClass == null) {
            try {
                this.mGNumberPickerClass = Class.forName(GOOGLE_NP_CLASSNAME);
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "mGNumberPickerClass not found");
            }
        }
    }

    public void setFormatter(Formatter formatter) {
        super.setFormatter(formatter);
    }
}
