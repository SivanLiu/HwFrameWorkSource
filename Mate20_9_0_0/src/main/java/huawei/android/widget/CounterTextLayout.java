package huawei.android.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import huawei.android.widget.loader.ResLoader;
import huawei.android.widget.loader.ResLoaderUtil;
import java.util.ArrayList;

public class CounterTextLayout extends LinearLayout {
    private static final int ANIMATION_DURATION = 50;
    private int mAnimShakeId;
    private int mCounterResBgId;
    private int mCounterTextAppearance;
    private TextView mCounterView;
    private int mCounterWarningColor;
    private EditText mEditText;
    private int mEditTextBgResId;
    private int mErrorResBgId;
    private int mLinearEditBgResId;
    private final ArrayList<View> mMatchParentChildren;
    private int mMaxLength;
    private ResLoader mResLoader;
    private ShapeMode mShapeMode;
    private boolean mSpaceOccupied;
    private int mStartShownPos;

    public enum ShapeMode {
        Bubble,
        Linear
    }

    private class TextInputAccessibilityDelegate extends AccessibilityDelegate {
        private TextInputAccessibilityDelegate() {
        }

        /* synthetic */ TextInputAccessibilityDelegate(CounterTextLayout x0, AnonymousClass1 x1) {
            this();
        }

        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(host, event);
            event.setClassName(CounterTextLayout.class.getSimpleName());
        }

        public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
            super.onPopulateAccessibilityEvent(host, event);
        }

        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setClassName(CounterTextLayout.class.getSimpleName());
            if (CounterTextLayout.this.mEditText != null) {
                info.setLabelFor(CounterTextLayout.this.mEditText);
            }
            CharSequence error = CounterTextLayout.this.mCounterView != null ? CounterTextLayout.this.mCounterView.getText() : null;
            if (!TextUtils.isEmpty(error)) {
                info.setContentInvalid(true);
                info.setError(error);
            }
        }
    }

    public CounterTextLayout(Context context) {
        this(context, null);
    }

    public CounterTextLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CounterTextLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);
        this.mMatchParentChildren = new ArrayList(1);
        this.mMaxLength = -1;
        this.mStartShownPos = -1;
        this.mCounterResBgId = -1;
        this.mErrorResBgId = -1;
        this.mEditTextBgResId = -1;
        this.mLinearEditBgResId = -1;
        setOrientation(1);
        this.mResLoader = ResLoader.getInstance();
        int textInputErrorColor = this.mResLoader.getIdentifier(context, ResLoaderUtil.COLOR, "design_textinput_error_color");
        this.mAnimShakeId = this.mResLoader.getIdentifier(context, "anim", "shake");
        this.mCounterWarningColor = context.getResources().getColor(textInputErrorColor);
        Theme theme = this.mResLoader.getTheme(context);
        if (theme != null) {
            TypedArray a = theme.obtainStyledAttributes(attrs, this.mResLoader.getIdentifierArray(context, ResLoaderUtil.STAYLEABLE, "CounterTextLayout"), this.mResLoader.getIdentifier(context, "attr", "counterTextLayoutStyle"), this.mResLoader.getIdentifier(context, "style", "Widget.Emui.CounterTextLayout"));
            this.mCounterTextAppearance = a.getResourceId(ResLoaderUtil.getStyleableId(context, "CounterTextLayout_counterTextAppearance"), 0);
            this.mSpaceOccupied = a.getBoolean(ResLoaderUtil.getStyleableId(context, "CounterTextLayout_spaceOccupied"), false);
            this.mShapeMode = ShapeMode.values()[a.getInt(ResLoaderUtil.getStyleableId(context, "CounterTextLayout_shape_mode"), ShapeMode.Bubble.ordinal())];
            this.mLinearEditBgResId = a.getResourceId(ResLoaderUtil.getStyleableId(context, "CounterTextLayout_linearEditBg"), 0);
            this.mCounterResBgId = a.getResourceId(ResLoaderUtil.getStyleableId(context, "CounterTextLayout_counterResBg"), 0);
            this.mErrorResBgId = a.getResourceId(ResLoaderUtil.getStyleableId(context, "CounterTextLayout_errorResBg"), 0);
            this.mEditTextBgResId = a.getResourceId(ResLoaderUtil.getStyleableId(context, "CounterTextLayout_editTextBg"), 0);
            a.recycle();
        }
        setupCounterView();
        if (getImportantForAccessibility() == 0) {
            setImportantForAccessibility(1);
        }
        setAccessibilityDelegate(new TextInputAccessibilityDelegate(this, null));
    }

    public void addView(View child, int index, LayoutParams params) {
        if (child instanceof EditText) {
            setEditText((EditText) child);
            super.addView(child, 0, updateEditTextMargin(params));
            return;
        }
        super.addView(child, index, params);
    }

    private void setEditText(EditText editText) {
        if (this.mEditText == null) {
            this.mEditText = editText;
            this.mEditText.setImeOptions(33554432 | this.mEditText.getImeOptions());
            if (this.mShapeMode == ShapeMode.Bubble) {
                this.mEditText.setBackgroundResource(this.mEditTextBgResId);
            } else if (this.mShapeMode == ShapeMode.Linear) {
                this.mEditText.setBackgroundResource(this.mLinearEditBgResId);
            }
            this.mEditText.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (-1 == CounterTextLayout.this.mMaxLength || -1 == CounterTextLayout.this.mStartShownPos) {
                        CounterTextLayout.this.setError(null);
                        return;
                    }
                    Editable editable = CounterTextLayout.this.mEditText.getText();
                    int len = editable.length();
                    if (len > CounterTextLayout.this.mMaxLength) {
                        int selEndIndex = Selection.getSelectionEnd(editable);
                        CounterTextLayout.this.mEditText.setText(editable.toString().substring(null, CounterTextLayout.this.mMaxLength));
                        editable = CounterTextLayout.this.mEditText.getText();
                        if (selEndIndex > editable.length()) {
                            selEndIndex = editable.length();
                        }
                        Selection.setSelection(editable, selEndIndex);
                        Animation shake = AnimationUtils.loadAnimation(CounterTextLayout.this.getContext(), CounterTextLayout.this.mAnimShakeId);
                        shake.setAnimationListener(new AnimationListener() {
                            public void onAnimationStart(Animation animation) {
                                CounterTextLayout.this.mCounterView.setTextColor(CounterTextLayout.this.mCounterWarningColor);
                                CounterTextLayout.this.updateTextLayoutBackground(CounterTextLayout.this.mErrorResBgId, ColorStateList.valueOf(CounterTextLayout.this.mCounterView.getCurrentTextColor()));
                            }

                            public void onAnimationRepeat(Animation animation) {
                            }

                            public void onAnimationEnd(Animation animation) {
                                CounterTextLayout.this.updateTextLayoutBackground(CounterTextLayout.this.mCounterResBgId, null);
                                CounterTextLayout.this.mCounterView.setTextAppearance(CounterTextLayout.this.getContext(), CounterTextLayout.this.mCounterTextAppearance);
                            }
                        });
                        CounterTextLayout.this.mEditText.startAnimation(shake);
                    } else if (len > CounterTextLayout.this.mStartShownPos) {
                        CounterTextLayout counterTextLayout = CounterTextLayout.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(len);
                        stringBuilder.append(" / ");
                        stringBuilder.append(CounterTextLayout.this.mMaxLength);
                        counterTextLayout.setError(stringBuilder.toString());
                    } else {
                        CounterTextLayout.this.setError(null);
                    }
                }
            });
            if (this.mCounterView != null) {
                this.mCounterView.setPaddingRelative(this.mEditText.getPaddingStart(), 0, this.mEditText.getPaddingEnd(), 0);
                return;
            }
            return;
        }
        throw new IllegalArgumentException("We already have an EditText, can only have one");
    }

    private LinearLayout.LayoutParams updateEditTextMargin(LayoutParams lp) {
        if (lp instanceof LinearLayout.LayoutParams) {
            return (LinearLayout.LayoutParams) lp;
        }
        return new LinearLayout.LayoutParams(lp);
    }

    public EditText getEditText() {
        return this.mEditText;
    }

    public void setHint(CharSequence hint) {
        this.mEditText.setHint(hint);
        sendAccessibilityEvent(2048);
    }

    public CharSequence getHint() {
        return this.mEditText.getHint();
    }

    public void setMaxLength(int maxLength) {
        this.mMaxLength = maxLength;
        this.mStartShownPos = (this.mMaxLength * 9) / 10;
    }

    public int getMaxLength() {
        return this.mMaxLength;
    }

    private void setupCounterView() {
        this.mCounterView = new TextView(getContext());
        this.mCounterView.setTextAppearance(getContext(), this.mCounterTextAppearance);
        this.mCounterView.setVisibility(this.mSpaceOccupied ? 4 : 8);
        addCounterView();
        if (this.mEditText != null) {
            this.mCounterView.setPaddingRelative(this.mEditText.getPaddingStart(), 0, this.mEditText.getPaddingEnd(), 0);
        }
    }

    private void addCounterView() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.gravity = 8388693;
        addView(this.mCounterView, params);
    }

    private void setError(CharSequence error) {
        if (!TextUtils.isEmpty(error)) {
            this.mCounterView.setAlpha(0.0f);
            this.mCounterView.setText(error);
            this.mCounterView.animate().alpha(1.0f).setDuration(50).setInterpolator(new LinearInterpolator()).setListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    CounterTextLayout.this.mCounterView.setVisibility(0);
                }
            }).start();
            if (this.mShapeMode == ShapeMode.Bubble) {
                this.mEditText.setBackgroundResource(this.mCounterResBgId);
            } else {
                ShapeMode shapeMode = this.mShapeMode;
                ShapeMode shapeMode2 = ShapeMode.Linear;
            }
        } else if (this.mCounterView.getVisibility() == 0) {
            this.mCounterView.animate().alpha(0.0f).setDuration(50).setInterpolator(new LinearInterpolator()).setListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    int i;
                    TextView access$700 = CounterTextLayout.this.mCounterView;
                    if (CounterTextLayout.this.mSpaceOccupied) {
                        i = 4;
                    } else {
                        i = 8;
                    }
                    access$700.setVisibility(i);
                }
            }).start();
            updateTextLayoutBackground(this.mEditTextBgResId, null);
        }
        sendAccessibilityEvent(2048);
    }

    public CharSequence getError() {
        if (this.mCounterView == null || this.mCounterView.getVisibility() != 0) {
            return null;
        }
        return this.mCounterView.getText();
    }

    private void updateTextLayoutBackground(int bubbleBackgroundID, ColorStateList linearTint) {
        if (this.mShapeMode == ShapeMode.Bubble) {
            this.mEditText.setBackgroundResource(bubbleBackgroundID);
        } else if (this.mShapeMode == ShapeMode.Linear) {
            this.mEditText.setBackgroundTintList(linearTint);
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mShapeMode == ShapeMode.Bubble) {
            measureBubble(widthMeasureSpec, heightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private void measureBubble(int widthMeasureSpec, int heightMeasureSpec) {
        int i;
        int i2;
        int childState;
        int widthWithMargin;
        int i3 = widthMeasureSpec;
        int i4 = heightMeasureSpec;
        int count = getChildCount();
        boolean z = (MeasureSpec.getMode(widthMeasureSpec) == 1073741824 && MeasureSpec.getMode(heightMeasureSpec) == 1073741824) ? false : true;
        boolean measureMatchParentChildren = z;
        this.mMatchParentChildren.clear();
        int maxHeight = 0;
        int maxWidth = 0;
        int childState2 = 0;
        int i5 = 0;
        while (true) {
            i = i5;
            if (i >= count) {
                break;
            }
            int i6;
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                View child2 = child;
                i2 = -1;
                i6 = i;
                childState = childState2;
                measureChildWithMargins(child, i3, 0, i4, 0);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child2.getLayoutParams();
                widthWithMargin = (child2.getMeasuredWidth() + lp.leftMargin) + lp.rightMargin;
                int maxWidth2 = widthWithMargin > maxWidth ? widthWithMargin : maxWidth;
                int heightWithMargin = (child2.getMeasuredHeight() + lp.topMargin) + lp.bottomMargin;
                i = heightWithMargin > maxHeight ? heightWithMargin : maxHeight;
                childState2 = combineMeasuredStates(childState, child2.getMeasuredState());
                if (measureMatchParentChildren && (lp.width == i2 || lp.height == i2)) {
                    this.mMatchParentChildren.add(child2);
                }
                maxWidth = maxWidth2;
                maxHeight = i;
            } else {
                i6 = i;
                childState = childState2;
            }
            i5 = i6 + 1;
        }
        i2 = -1;
        childState = childState2;
        i5 = getSuggestedMinimumHeight();
        widthWithMargin = getSuggestedMinimumWidth();
        setMeasuredDimension(resolveSizeAndState(maxWidth > widthWithMargin ? maxWidth : widthWithMargin, i3, childState), resolveSizeAndState(maxHeight > i5 ? maxHeight : i5, i4, childState << 16));
        i = this.mMatchParentChildren.size();
        if (i > 1) {
            childState2 = 0;
            while (childState2 < i) {
                int minHeight;
                View child3 = (View) this.mMatchParentChildren.get(childState2);
                MarginLayoutParams lp2 = (MarginLayoutParams) child3.getLayoutParams();
                if (lp2.width == i2) {
                    maxHeight = (getMeasuredWidth() - lp2.leftMargin) - lp2.rightMargin;
                    maxHeight = MeasureSpec.makeMeasureSpec(maxHeight > 0 ? maxHeight : 0, 1073741824);
                } else {
                    maxHeight = getChildMeasureSpec(i3, lp2.leftMargin + lp2.rightMargin, lp2.width);
                }
                i2 = maxHeight;
                if (lp2.height == -1) {
                    maxHeight = (getMeasuredHeight() - lp2.topMargin) - lp2.bottomMargin;
                    minHeight = i5;
                    maxHeight = MeasureSpec.makeMeasureSpec(maxHeight > 0 ? maxHeight : 0, 1073741824);
                } else {
                    minHeight = i5;
                    maxHeight = getChildMeasureSpec(i4, lp2.topMargin + lp2.bottomMargin, lp2.height);
                }
                child3.measure(i2, maxHeight);
                childState2++;
                i5 = minHeight;
                i2 = -1;
            }
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mShapeMode == ShapeMode.Bubble) {
            layoutBubble(left, top, right, bottom);
            this.mCounterView.offsetTopAndBottom(-9);
            return;
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    private void layoutBubble(int left, int top, int right, int bottom) {
        int count;
        int parentLeft;
        int count2 = getChildCount();
        int parentLeft2 = 0;
        int parentRight = right - left;
        int parentBottom = bottom - top;
        int i = 0;
        while (i < count2) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                int childTop;
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
                int width = child.getMeasuredWidth();
                int height = child.getMeasuredHeight();
                int gravity = lp.gravity;
                count = count2;
                if (gravity == -1) {
                    gravity = 8388693;
                }
                count2 = getLayoutDirection();
                int absoluteGravity = Gravity.getAbsoluteGravity(gravity, count2);
                int layoutDirection = count2;
                count2 = gravity & 112;
                parentLeft = parentLeft2;
                parentLeft2 = absoluteGravity & 7;
                if (parentLeft2 == 1) {
                    parentLeft2 = (((((parentRight + 0) - width) / 2) + 0) + lp.leftMargin) - lp.rightMargin;
                } else if (parentLeft2 != 5) {
                    parentLeft2 = lp.leftMargin + 0;
                } else {
                    parentLeft2 = (parentRight - width) - lp.rightMargin;
                }
                int verticalGravity;
                if (count2 == 16) {
                    verticalGravity = count2;
                    childTop = (((((parentBottom + 0) - height) / 2) + 0) + lp.topMargin) - lp.bottomMargin;
                } else if (count2 == 48) {
                    verticalGravity = count2;
                    childTop = 0 + lp.topMargin;
                } else if (count2 != 80) {
                    childTop = lp.topMargin + 0;
                    verticalGravity = count2;
                } else {
                    verticalGravity = count2;
                    childTop = (parentBottom - height) - lp.bottomMargin;
                }
                count2 = childTop;
                child.layout(parentLeft2, count2, parentLeft2 + width, count2 + height);
            } else {
                count = count2;
                parentLeft = parentLeft2;
            }
            i++;
            count2 = count;
            parentLeft2 = parentLeft;
        }
        count = count2;
        parentLeft = parentLeft2;
    }
}
