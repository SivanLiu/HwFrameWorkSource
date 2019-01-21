package com.android.internal.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.hardware.input.InputManager;
import android.hardware.input.InputManager.InputDeviceListener;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.VelocityTracker;
import android.view.VelocityTracker.Estimator;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManagerPolicyConstants.PointerEventListener;
import java.util.ArrayList;

public class PointerLocationView extends View implements InputDeviceListener, PointerEventListener {
    private static final String ALT_STRATEGY_PROPERY_KEY = "debug.velocitytracker.alt";
    protected static final boolean HWFLOW;
    private static final String TAG = "Pointer";
    private final int ESTIMATE_FUTURE_POINTS = 2;
    private final float ESTIMATE_INTERVAL = 0.02f;
    private final int ESTIMATE_PAST_POINTS = 4;
    private int mActivePointerId;
    private final VelocityTracker mAltVelocity;
    private boolean mCurDown;
    private int mCurNumPointers;
    private final Paint mCurrentPointPaint;
    private int mHeaderBottom;
    private final InputManager mIm;
    private int mMaxNumPointers;
    private final Paint mPaint;
    private final Paint mPathPaint;
    private final ArrayList<PointerState> mPointers = new ArrayList();
    private boolean mPrintCoords = true;
    private RectF mReusableOvalRect = new RectF();
    private final Paint mTargetPaint;
    private final PointerCoords mTempCoords = new PointerCoords();
    private final FasterStringBuilder mText = new FasterStringBuilder();
    private final Paint mTextBackgroundPaint;
    private final Paint mTextLevelPaint;
    private final FontMetricsInt mTextMetrics = new FontMetricsInt();
    private final Paint mTextPaint;
    private final ViewConfiguration mVC;
    private final VelocityTracker mVelocity;

    private static final class FasterStringBuilder {
        private char[] mChars = new char[64];
        private int mLength;

        public FasterStringBuilder clear() {
            this.mLength = 0;
            return this;
        }

        public FasterStringBuilder append(String value) {
            int valueLength = value.length();
            value.getChars(0, valueLength, this.mChars, reserve(valueLength));
            this.mLength += valueLength;
            return this;
        }

        public FasterStringBuilder append(int value) {
            return append(value, 0);
        }

        public FasterStringBuilder append(int value, int zeroPadWidth) {
            boolean negative = value < 0;
            if (negative) {
                value = -value;
                if (value < 0) {
                    append("-2147483648");
                    return this;
                }
            }
            int index = reserve(11);
            char[] chars = this.mChars;
            if (value == 0) {
                int index2 = index + 1;
                chars[index] = '0';
                this.mLength++;
                return this;
            }
            int index3;
            int index4;
            if (negative) {
                index3 = index + 1;
                chars[index] = '-';
            } else {
                index3 = index;
            }
            index = 1000000000;
            int index5 = index3;
            index3 = 10;
            while (value < index) {
                index /= 10;
                index3--;
                if (index3 < zeroPadWidth) {
                    index4 = index5 + 1;
                    chars[index5] = '0';
                    index5 = index4;
                }
            }
            while (true) {
                int digit = value / index;
                value -= digit * index;
                index /= 10;
                index4 = index5 + 1;
                chars[index5] = (char) (digit + 48);
                if (index == 0) {
                    this.mLength = index4;
                    return this;
                }
                index5 = index4;
            }
        }

        public FasterStringBuilder append(float value, int precision) {
            int scale = 1;
            for (int i = 0; i < precision; i++) {
                scale *= 10;
            }
            value = (float) (Math.rint((double) (((float) scale) * value)) / ((double) scale));
            append((int) value);
            if (precision != 0) {
                append(".");
                value = Math.abs(value);
                append((int) (((float) scale) * ((float) (((double) value) - Math.floor((double) value)))), precision);
            }
            return this;
        }

        public String toString() {
            return new String(this.mChars, 0, this.mLength);
        }

        private int reserve(int length) {
            int oldLength = this.mLength;
            int newLength = this.mLength + length;
            char[] oldChars = this.mChars;
            int oldCapacity = oldChars.length;
            if (newLength > oldCapacity) {
                char[] newChars = new char[(oldCapacity * 2)];
                System.arraycopy(oldChars, 0, newChars, 0, oldLength);
                this.mChars = newChars;
            }
            return oldLength;
        }
    }

    public static class PointerState {
        private Estimator mAltEstimator = new Estimator();
        private float mAltXVelocity;
        private float mAltYVelocity;
        private float mBoundingBottom;
        private float mBoundingLeft;
        private float mBoundingRight;
        private float mBoundingTop;
        private PointerCoords mCoords = new PointerCoords();
        private boolean mCurDown;
        private Estimator mEstimator = new Estimator();
        private boolean mHasBoundingBox;
        private int mToolType;
        private int mTraceCount;
        private boolean[] mTraceCurrent = new boolean[32];
        private float[] mTraceX = new float[32];
        private float[] mTraceY = new float[32];
        private float mXVelocity;
        private float mYVelocity;

        public void clearTrace() {
            this.mTraceCount = 0;
        }

        public void addTrace(float x, float y, boolean current) {
            int traceCapacity = this.mTraceX.length;
            if (this.mTraceCount == traceCapacity) {
                traceCapacity *= 2;
                float[] newTraceX = new float[traceCapacity];
                System.arraycopy(this.mTraceX, 0, newTraceX, 0, this.mTraceCount);
                this.mTraceX = newTraceX;
                float[] newTraceY = new float[traceCapacity];
                System.arraycopy(this.mTraceY, 0, newTraceY, 0, this.mTraceCount);
                this.mTraceY = newTraceY;
                boolean[] newTraceCurrent = new boolean[traceCapacity];
                System.arraycopy(this.mTraceCurrent, 0, newTraceCurrent, 0, this.mTraceCount);
                this.mTraceCurrent = newTraceCurrent;
            }
            this.mTraceX[this.mTraceCount] = x;
            this.mTraceY[this.mTraceCount] = y;
            this.mTraceCurrent[this.mTraceCount] = current;
            this.mTraceCount++;
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z;
    }

    public PointerLocationView(Context c) {
        super(c);
        setFocusableInTouchMode(true);
        this.mIm = (InputManager) c.getSystemService(InputManager.class);
        this.mVC = ViewConfiguration.get(c);
        this.mTextPaint = new Paint();
        this.mTextPaint.setAntiAlias(true);
        this.mTextPaint.setTextSize(10.0f * getResources().getDisplayMetrics().density);
        this.mTextPaint.setARGB(255, 0, 0, 0);
        this.mTextBackgroundPaint = new Paint();
        this.mTextBackgroundPaint.setAntiAlias(false);
        this.mTextBackgroundPaint.setARGB(128, 255, 255, 255);
        this.mTextLevelPaint = new Paint();
        this.mTextLevelPaint.setAntiAlias(false);
        this.mTextLevelPaint.setARGB(192, 255, 0, 0);
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setARGB(255, 255, 255, 255);
        this.mPaint.setStyle(Style.STROKE);
        this.mPaint.setStrokeWidth(2.0f);
        this.mCurrentPointPaint = new Paint();
        this.mCurrentPointPaint.setAntiAlias(true);
        this.mCurrentPointPaint.setARGB(255, 255, 0, 0);
        this.mCurrentPointPaint.setStyle(Style.STROKE);
        this.mCurrentPointPaint.setStrokeWidth(2.0f);
        this.mTargetPaint = new Paint();
        this.mTargetPaint.setAntiAlias(false);
        this.mTargetPaint.setARGB(255, 0, 0, 192);
        this.mPathPaint = new Paint();
        this.mPathPaint.setAntiAlias(false);
        this.mPathPaint.setARGB(255, 0, 96, 255);
        this.mPaint.setStyle(Style.STROKE);
        this.mPaint.setStrokeWidth(1.0f);
        this.mPointers.add(new PointerState());
        this.mActivePointerId = 0;
        this.mVelocity = VelocityTracker.obtain();
        String altStrategy = SystemProperties.get(ALT_STRATEGY_PROPERY_KEY);
        if (altStrategy.length() != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Comparing default velocity tracker strategy with ");
            stringBuilder.append(altStrategy);
            Log.d(str, stringBuilder.toString());
            this.mAltVelocity = VelocityTracker.obtain(altStrategy);
            return;
        }
        this.mAltVelocity = null;
    }

    public void setPrintCoords(boolean state) {
        this.mPrintCoords = state;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.mTextPaint.getFontMetricsInt(this.mTextMetrics);
        this.mHeaderBottom = ((-this.mTextMetrics.ascent) + this.mTextMetrics.descent) + 2;
    }

    private void drawOval(Canvas canvas, float x, float y, float major, float minor, float angle, Paint paint) {
        canvas.save(1);
        canvas.rotate((float) (((double) (180.0f * angle)) / 3.141592653589793d), x, y);
        this.mReusableOvalRect.left = x - (minor / 2.0f);
        this.mReusableOvalRect.right = (minor / 2.0f) + x;
        this.mReusableOvalRect.top = y - (major / 2.0f);
        this.mReusableOvalRect.bottom = (major / 2.0f) + y;
        canvas.drawOval(this.mReusableOvalRect, paint);
        canvas.restore();
    }

    /* JADX WARNING: Removed duplicated region for block: B:81:0x0643 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x0621  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void onDraw(Canvas canvas) {
        PointerState ps;
        float f;
        float dx;
        float dy;
        float dx2;
        Canvas canvas2 = canvas;
        int w = getWidth();
        int itemW = w / 8;
        int base = (-this.mTextMetrics.ascent) + 1;
        int bottom = this.mHeaderBottom;
        int NP = this.mPointers.size();
        if (this.mActivePointerId >= 0) {
            if (this.mActivePointerId >= NP) {
                if (HWFLOW) {
                    Log.i(TAG, "exception occur: mActivePointerId >= NP");
                }
                return;
            }
            ps = (PointerState) this.mPointers.get(this.mActivePointerId);
            canvas2.drawRect(0.0f, 0.0f, (float) (itemW - 1), (float) bottom, this.mTextBackgroundPaint);
            canvas2.drawText(this.mText.clear().append("P: ").append(this.mCurNumPointers).append(" / ").append(this.mMaxNumPointers).toString(), 1.0f, (float) base, this.mTextPaint);
            int N = ps.mTraceCount;
            if ((this.mCurDown && ps.mCurDown) || N == 0) {
                f = 1.0f;
                canvas2.drawRect((float) itemW, 0.0f, (float) ((itemW * 2) - 1), (float) bottom, this.mTextBackgroundPaint);
                canvas2.drawText(this.mText.clear().append("X: ").append(ps.mCoords.x, 1).toString(), (float) (1 + itemW), (float) base, this.mTextPaint);
                canvas2.drawRect((float) (itemW * 2), 0.0f, (float) ((itemW * 3) - 1), (float) bottom, this.mTextBackgroundPaint);
                canvas2.drawText(this.mText.clear().append("Y: ").append(ps.mCoords.y, 1).toString(), (float) ((itemW * 2) + 1), (float) base, this.mTextPaint);
            } else {
                f = 1.0f;
                dx = ps.mTraceX[N - 1] - ps.mTraceX[0];
                dy = ps.mTraceY[N - 1] - ps.mTraceY[0];
                dx2 = dx;
                canvas2.drawRect((float) itemW, 0.0f, (float) ((itemW * 2) - 1), (float) bottom, Math.abs(dx) < ((float) this.mVC.getScaledTouchSlop()) ? this.mTextBackgroundPaint : this.mTextLevelPaint);
                canvas2.drawText(this.mText.clear().append("dX: ").append(dx2, 1).toString(), (float) (1 + itemW), (float) base, this.mTextPaint);
                float dy2 = dy;
                canvas2.drawRect((float) (itemW * 2), 0.0f, (float) ((itemW * 3) - 1), (float) bottom, Math.abs(dy2) < ((float) this.mVC.getScaledTouchSlop()) ? this.mTextBackgroundPaint : this.mTextLevelPaint);
                canvas2.drawText(this.mText.clear().append("dY: ").append(dy2, 1).toString(), (float) ((itemW * 2) + 1), (float) base, this.mTextPaint);
            }
            canvas2.drawRect((float) (itemW * 3), 0.0f, (float) ((itemW * 4) - 1), (float) bottom, this.mTextBackgroundPaint);
            canvas2.drawText(this.mText.clear().append("Xv: ").append(ps.mXVelocity, 3).toString(), (float) ((itemW * 3) + 1), (float) base, this.mTextPaint);
            canvas2.drawRect((float) (itemW * 4), 0.0f, (float) ((itemW * 5) - 1), (float) bottom, this.mTextBackgroundPaint);
            canvas2.drawText(this.mText.clear().append("Yv: ").append(ps.mYVelocity, 3).toString(), (float) (1 + (itemW * 4)), (float) base, this.mTextPaint);
            canvas2.drawRect((float) (itemW * 5), 0.0f, (float) ((itemW * 6) - 1), (float) bottom, this.mTextBackgroundPaint);
            canvas2.drawRect((float) (itemW * 5), 0.0f, (((float) (itemW * 5)) + (ps.mCoords.pressure * ((float) itemW))) - f, (float) bottom, this.mTextLevelPaint);
            canvas2.drawText(this.mText.clear().append("Prs: ").append(ps.mCoords.pressure, 2).toString(), (float) (1 + (itemW * 5)), (float) base, this.mTextPaint);
            canvas2.drawRect((float) (itemW * 6), 0.0f, (float) ((itemW * 7) - 1), (float) bottom, this.mTextBackgroundPaint);
            canvas2.drawRect((float) (itemW * 6), 0.0f, (((float) (itemW * 6)) + (ps.mCoords.size * ((float) itemW))) - f, (float) bottom, this.mTextLevelPaint);
            canvas2.drawText(this.mText.clear().append("Size: ").append(ps.mCoords.size, 2).toString(), (float) (1 + (itemW * 6)), (float) base, this.mTextPaint);
            canvas2.drawRect((float) (itemW * 7), 0.0f, (float) w, (float) bottom, this.mTextBackgroundPaint);
            canvas2.drawRect((float) (itemW * 7), 0.0f, (((float) (itemW * 7)) + (ps.mCoords.size * ((float) itemW))) - f, (float) bottom, this.mTextLevelPaint);
            canvas2.drawText(this.mText.clear().append("T: ").append((float) SystemClock.uptimeMillis(), 6).toString(), (float) (1 + (itemW * 7)), (float) base, this.mTextPaint);
        }
        int p = 0;
        while (true) {
            int p2 = p;
            int itemW2;
            int base2;
            int bottom2;
            if (p2 < NP) {
                int y;
                float x;
                int i;
                ps = (PointerState) this.mPointers.get(p2);
                int N2 = ps.mTraceCount;
                int i2 = 128;
                int w2 = w;
                this.mPaint.setARGB(255, 128, 255, 255);
                float lastX = 0.0f;
                boolean haveLast = false;
                boolean drawn = false;
                p = 0;
                int lastY = 0;
                while (true) {
                    int i3 = p;
                    if (i3 >= N2) {
                        break;
                    }
                    int i4;
                    float x2 = ps.mTraceX[i3];
                    y = ps.mTraceY[i3];
                    if (Float.isNaN(x2)) {
                        haveLast = false;
                        i4 = i3;
                        itemW2 = itemW;
                        base2 = base;
                        base = i2;
                    } else {
                        if (haveLast) {
                            x = x2;
                            i4 = i3;
                            w = lastY;
                            itemW2 = itemW;
                            itemW = lastX;
                            base2 = base;
                            base = i2;
                            canvas2.drawLine(lastX, lastY, x, y, this.mPathPaint);
                            canvas2.drawPoint(itemW, w, ps.mTraceCurrent[i4] ? this.mCurrentPointPaint : this.mPaint);
                            drawn = true;
                        } else {
                            x = x2;
                            i4 = i3;
                            w = lastY;
                            itemW2 = itemW;
                            base2 = base;
                            base = i2;
                        }
                        lastX = x;
                        lastY = y;
                        haveLast = true;
                    }
                    p = i4 + 1;
                    i2 = base;
                    itemW = itemW2;
                    base = base2;
                }
                w = lastY;
                itemW2 = itemW;
                base2 = base;
                float lastX2 = lastX;
                base = i2;
                if (drawn) {
                    this.mPaint.setARGB(base, base, 0, base);
                    y = -3;
                    dy = ps.mEstimator.estimateX(-0.08f);
                    x = ps.mEstimator.estimateY(-0.08f);
                    p = -3;
                    while (true) {
                        int i5 = p;
                        if (i5 > 2) {
                            break;
                        }
                        float x3 = ps.mEstimator.estimateX(((float) i5) * 0.02f);
                        float y2 = ps.mEstimator.estimateY(((float) i5) * 0.02f);
                        int i6 = i5;
                        canvas2.drawLine(dy, x, x3, y2, this.mPaint);
                        dy = x3;
                        x = y2;
                        p = i6 + 1;
                    }
                    this.mPaint.setARGB(255, 255, 64, base);
                    canvas2.drawLine(lastX2, w, lastX2 + (ps.mXVelocity * 16.0f), w + (ps.mYVelocity * 16.0f), this.mPaint);
                    if (this.mAltVelocity != null) {
                        this.mPaint.setARGB(base, 0, base, base);
                        dy = ps.mAltEstimator.estimateX(-0.08f);
                        x = ps.mAltEstimator.estimateY(-0.08f);
                        while (true) {
                            i2 = y;
                            if (i2 > 2) {
                                break;
                            }
                            float x4 = ps.mAltEstimator.estimateX(((float) i2) * 0.02f);
                            f = ps.mAltEstimator.estimateY(((float) i2) * 0.02f);
                            i = 2;
                            int i7 = i2;
                            canvas2.drawLine(dy, x, x4, f, this.mPaint);
                            dy = x4;
                            x = f;
                            y = i7 + 1;
                        }
                        i = 2;
                        this.mPaint.setARGB(255, 64, 255, base);
                        canvas2.drawLine(lastX2, w, lastX2 + (ps.mAltXVelocity * 16.0f), w + (ps.mAltYVelocity * 16.0f), this.mPaint);
                        if (this.mCurDown || !ps.mCurDown) {
                            bottom2 = bottom;
                            bottom = i;
                        } else {
                            float orientationVectorY;
                            canvas2.drawLine(0.0f, ps.mCoords.y, (float) getWidth(), ps.mCoords.y, this.mTargetPaint);
                            canvas2.drawLine(ps.mCoords.x, 0.0f, ps.mCoords.x, (float) getHeight(), this.mTargetPaint);
                            i2 = (int) (ps.mCoords.pressure * 255.0f);
                            this.mPaint.setARGB(255, i2, 255, 255 - i2);
                            canvas2.drawPoint(ps.mCoords.x, ps.mCoords.y, this.mPaint);
                            this.mPaint.setARGB(255, i2, 255 - i2, base);
                            int lastY2 = w;
                            w = i2;
                            bottom2 = bottom;
                            bottom = i;
                            PointerState ps2 = ps;
                            drawOval(canvas2, ps.mCoords.x, ps.mCoords.y, ps.mCoords.touchMajor, ps.mCoords.touchMinor, ps.mCoords.orientation, this.mPaint);
                            this.mPaint.setARGB(255, w, 128, 255 - w);
                            drawOval(canvas2, ps2.mCoords.x, ps2.mCoords.y, ps2.mCoords.toolMajor, ps2.mCoords.toolMinor, ps2.mCoords.orientation, this.mPaint);
                            float arrowSize = ps2.mCoords.toolMajor * 0.7f;
                            if (arrowSize < 20.0f) {
                                arrowSize = 20.0f;
                            }
                            dx2 = arrowSize;
                            this.mPaint.setARGB(255, w, 255, 0);
                            float orientationVectorX = (float) (Math.sin((double) ps2.mCoords.orientation) * ((double) dx2));
                            dx = (float) ((-Math.cos((double) ps2.mCoords.orientation)) * ((double) dx2));
                            if (ps2.mToolType == bottom) {
                                orientationVectorY = dx;
                            } else if (ps2.mToolType == 4) {
                                orientationVectorY = dx;
                            } else {
                                orientationVectorY = dx;
                                canvas2.drawLine(ps2.mCoords.x - orientationVectorX, ps2.mCoords.y - dx, ps2.mCoords.x + orientationVectorX, ps2.mCoords.y + dx, this.mPaint);
                                dx = (float) Math.sin((double) ps2.mCoords.getAxisValue(25));
                                canvas2.drawCircle(ps2.mCoords.x + (orientationVectorX * dx), ps2.mCoords.y + (orientationVectorY * dx), 3.0f, this.mPaint);
                                if (!ps2.mHasBoundingBox) {
                                    canvas2.drawRect(ps2.mBoundingLeft, ps2.mBoundingTop, ps2.mBoundingRight, ps2.mBoundingBottom, this.mPaint);
                                }
                            }
                            canvas2.drawLine(ps2.mCoords.x, ps2.mCoords.y, ps2.mCoords.x + orientationVectorX, ps2.mCoords.y + orientationVectorY, this.mPaint);
                            dx = (float) Math.sin((double) ps2.mCoords.getAxisValue(25));
                            canvas2.drawCircle(ps2.mCoords.x + (orientationVectorX * dx), ps2.mCoords.y + (orientationVectorY * dx), 3.0f, this.mPaint);
                            if (!ps2.mHasBoundingBox) {
                            }
                        }
                        p = p2 + 1;
                        w = w2;
                        itemW = itemW2;
                        base = base2;
                        bottom = bottom2;
                    }
                }
                i = 2;
                if (this.mCurDown) {
                }
                bottom2 = bottom;
                bottom = i;
                p = p2 + 1;
                w = w2;
                itemW = itemW2;
                base = base2;
                bottom = bottom2;
            } else {
                itemW2 = itemW;
                base2 = base;
                bottom2 = bottom;
                return;
            }
        }
    }

    private void logMotionEvent(String type, MotionEvent event) {
        int historyPos;
        MotionEvent motionEvent = event;
        int action = event.getAction();
        int N = event.getHistorySize();
        int NI = event.getPointerCount();
        int i = 0;
        int historyPos2 = 0;
        while (true) {
            historyPos = historyPos2;
            if (historyPos >= N) {
                break;
            }
            historyPos2 = 0;
            while (true) {
                int i2 = historyPos2;
                if (i2 >= NI) {
                    break;
                }
                int id = motionEvent.getPointerId(i2);
                motionEvent.getHistoricalPointerCoords(i2, historyPos, this.mTempCoords);
                logCoords(type, action, i2, this.mTempCoords, id, motionEvent);
                historyPos2 = i2 + 1;
            }
            historyPos2 = historyPos + 1;
        }
        while (i < NI) {
            historyPos = motionEvent.getPointerId(i);
            motionEvent.getPointerCoords(i, this.mTempCoords);
            logCoords(type, action, i, this.mTempCoords, historyPos, motionEvent);
            i++;
        }
    }

    private void logCoords(String type, int action, int index, PointerCoords coords, int id, MotionEvent event) {
        String prefix;
        int i = action;
        int i2 = index;
        PointerCoords pointerCoords = coords;
        MotionEvent motionEvent = event;
        int toolType = motionEvent.getToolType(i2);
        int buttonState = event.getButtonState();
        switch (i & 255) {
            case 0:
                prefix = "DOWN";
                break;
            case 1:
                prefix = "UP";
                break;
            case 2:
                prefix = "MOVE";
                break;
            case 3:
                prefix = "CANCEL";
                break;
            case 4:
                prefix = "OUTSIDE";
                break;
            case 5:
                if (i2 != ((i & 65280) >> 8)) {
                    prefix = "MOVE";
                    break;
                } else {
                    prefix = "DOWN";
                    break;
                }
            case 6:
                if (i2 != ((i & 65280) >> 8)) {
                    prefix = "MOVE";
                    break;
                } else {
                    prefix = "UP";
                    break;
                }
            case 7:
                prefix = "HOVER MOVE";
                break;
            case 8:
                prefix = "SCROLL";
                break;
            case 9:
                prefix = "HOVER ENTER";
                break;
            case 10:
                prefix = "HOVER EXIT";
                break;
            default:
                prefix = Integer.toString(action);
                break;
        }
        Log.i(TAG, this.mText.clear().append(type).append(" id ").append(id + 1).append(": ").append(prefix).append(" (").append(pointerCoords.x, 3).append(", ").append(pointerCoords.y, 3).append(") Pressure=").append(pointerCoords.pressure, 3).append(" Size=").append(pointerCoords.size, 3).append(" TouchMajor=").append(pointerCoords.touchMajor, 3).append(" TouchMinor=").append(pointerCoords.touchMinor, 3).append(" ToolMajor=").append(pointerCoords.toolMajor, 3).append(" ToolMinor=").append(pointerCoords.toolMinor, 3).append(" Orientation=").append((float) (((double) (pointerCoords.orientation * 180.0f)) / 3.141592653589793d), 1).append("deg").append(" Tilt=").append((float) (((double) (pointerCoords.getAxisValue(25) * 180.0f)) / 3.141592653589793d), 1).append("deg").append(" Distance=").append(pointerCoords.getAxisValue(24), 1).append(" VScroll=").append(pointerCoords.getAxisValue(9), 1).append(" HScroll=").append(pointerCoords.getAxisValue(10), 1).append(" BoundingBox=[(").append(motionEvent.getAxisValue(32), 3).append(", ").append(motionEvent.getAxisValue(33), 3).append(")").append(", (").append(motionEvent.getAxisValue(34), 3).append(", ").append(motionEvent.getAxisValue(35), 3).append(")]").append(" ToolType=").append(MotionEvent.toolTypeToString(toolType)).append(" ButtonState=").append(MotionEvent.buttonStateToString(buttonState)).toString());
    }

    public void onPointerEvent(MotionEvent event) {
        int index;
        int p;
        PointerState ps;
        PointerState ps2;
        PointerCoords coords;
        MotionEvent motionEvent = event;
        int action = event.getAction();
        int NP = this.mPointers.size();
        int i = 1;
        if (action == 0 || (action & 255) == 5) {
            index = (action & 65280) >> 8;
            if (action == 0) {
                for (p = 0; p < NP; p++) {
                    ps = (PointerState) this.mPointers.get(p);
                    ps.clearTrace();
                    ps.mCurDown = false;
                }
                this.mCurDown = true;
                this.mCurNumPointers = 0;
                this.mMaxNumPointers = 0;
                this.mVelocity.clear();
                if (this.mAltVelocity != null) {
                    this.mAltVelocity.clear();
                }
            }
            this.mCurNumPointers++;
            if (this.mMaxNumPointers < this.mCurNumPointers) {
                this.mMaxNumPointers = this.mCurNumPointers;
            }
            p = motionEvent.getPointerId(index);
            while (NP <= p) {
                this.mPointers.add(new PointerState());
                NP++;
            }
            if (this.mActivePointerId < this.mPointers.size() && (this.mActivePointerId < 0 || !((PointerState) this.mPointers.get(this.mActivePointerId)).mCurDown)) {
                this.mActivePointerId = p;
            }
            PointerState ps3 = (PointerState) this.mPointers.get(p);
            ps3.mCurDown = true;
            InputDevice device = InputDevice.getDevice(event.getDeviceId());
            boolean z = (device == null || device.getMotionRange(32) == null) ? false : true;
            ps3.mHasBoundingBox = z;
        }
        int NP2 = NP;
        int NI = event.getPointerCount();
        this.mVelocity.addMovement(motionEvent);
        this.mVelocity.computeCurrentVelocity(1);
        if (this.mAltVelocity != null) {
            this.mAltVelocity.addMovement(motionEvent);
            this.mAltVelocity.computeCurrentVelocity(1);
        }
        int N = event.getHistorySize();
        NP = 0;
        while (true) {
            int historyPos = NP;
            if (historyPos >= N) {
                break;
            }
            int N2;
            NP = 0;
            while (true) {
                int i2 = NP;
                if (i2 >= NI) {
                    break;
                }
                PointerCoords coords2;
                PointerState ps4;
                int i3;
                int historyPos2;
                int id = motionEvent.getPointerId(i2);
                ps2 = null;
                if (id < this.mPointers.size()) {
                    ps2 = this.mCurDown ? (PointerState) this.mPointers.get(id) : null;
                }
                PointerState ps5 = ps2;
                PointerCoords coords3 = ps5 != null ? ps5.mCoords : this.mTempCoords;
                motionEvent.getHistoricalPointerCoords(i2, historyPos, coords3);
                if (this.mPrintCoords) {
                    coords2 = coords3;
                    ps4 = ps5;
                    i3 = i2;
                    historyPos2 = historyPos;
                    N2 = N;
                    logCoords(TAG, action, i2, coords2, id, motionEvent);
                } else {
                    coords2 = coords3;
                    ps4 = ps5;
                    int i4 = id;
                    i3 = i2;
                    historyPos2 = historyPos;
                    N2 = N;
                }
                if (ps4 != null) {
                    coords = coords2;
                    ps4.addTrace(coords.x, coords.y, false);
                }
                NP = i3 + 1;
                historyPos = historyPos2;
                N = N2;
            }
            N2 = N;
            NP = historyPos + 1;
        }
        NP = 0;
        while (true) {
            int i5 = NP;
            if (i5 >= NI) {
                break;
            }
            PointerCoords coords4;
            PointerState ps6;
            int id2;
            N = motionEvent.getPointerId(i5);
            ps2 = null;
            if (N < this.mPointers.size()) {
                ps2 = this.mCurDown ? (PointerState) this.mPointers.get(N) : null;
            }
            PointerState ps7 = ps2;
            PointerCoords coords5 = ps7 != null ? ps7.mCoords : this.mTempCoords;
            motionEvent.getPointerCoords(i5, coords5);
            if (this.mPrintCoords) {
                coords4 = coords5;
                ps6 = ps7;
                id2 = N;
                logCoords(TAG, action, i5, coords5, N, motionEvent);
            } else {
                coords4 = coords5;
                ps6 = ps7;
                id2 = N;
            }
            if (ps6 != null) {
                coords = coords4;
                ps6.addTrace(coords.x, coords.y, true);
                ps6.mXVelocity = this.mVelocity.getXVelocity(id2);
                ps6.mYVelocity = this.mVelocity.getYVelocity(id2);
                this.mVelocity.getEstimator(id2, ps6.mEstimator);
                if (this.mAltVelocity != null) {
                    ps6.mAltXVelocity = this.mAltVelocity.getXVelocity(id2);
                    ps6.mAltYVelocity = this.mAltVelocity.getYVelocity(id2);
                    this.mAltVelocity.getEstimator(id2, ps6.mAltEstimator);
                }
                ps6.mToolType = motionEvent.getToolType(i5);
                if (ps6.mHasBoundingBox) {
                    index = 32;
                    ps6.mBoundingLeft = motionEvent.getAxisValue(32, i5);
                    ps6.mBoundingTop = motionEvent.getAxisValue(33, i5);
                    ps6.mBoundingRight = motionEvent.getAxisValue(34, i5);
                    ps6.mBoundingBottom = motionEvent.getAxisValue(35, i5);
                    NP = i5 + 1;
                    id2 = index;
                }
            }
            index = 32;
            NP = i5 + 1;
            id2 = index;
        }
        if (action == 1 || action == 3 || (action & 255) == 6) {
            index = (65280 & action) >> 8;
            p = motionEvent.getPointerId(index);
            if (p >= NP2) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Got pointer ID out of bounds: id=");
                stringBuilder.append(p);
                stringBuilder.append(" arraysize=");
                stringBuilder.append(NP2);
                stringBuilder.append(" pointerindex=");
                stringBuilder.append(index);
                stringBuilder.append(" action=0x");
                stringBuilder.append(Integer.toHexString(action));
                Slog.wtf(str, stringBuilder.toString());
                return;
            } else if (p < this.mPointers.size()) {
                ps = (PointerState) this.mPointers.get(p);
                ps.mCurDown = false;
                if (action == 1 || action == 3) {
                    this.mCurDown = false;
                    this.mCurNumPointers = 0;
                } else {
                    this.mCurNumPointers--;
                    if (this.mActivePointerId == p) {
                        if (index != 0) {
                            i = 0;
                        }
                        this.mActivePointerId = motionEvent.getPointerId(i);
                    }
                    ps.addTrace(Float.NaN, Float.NaN, false);
                }
            } else {
                return;
            }
        }
        invalidate();
    }

    public boolean onTouchEvent(MotionEvent event) {
        onPointerEvent(event);
        if (event.getAction() == 0 && !isFocused()) {
            requestFocus();
        }
        return true;
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        int source = event.getSource();
        if ((source & 2) != 0) {
            onPointerEvent(event);
        } else if ((source & 16) != 0) {
            logMotionEvent("Joystick", event);
        } else if ((source & 8) != 0) {
            logMotionEvent("Position", event);
        } else {
            logMotionEvent("Generic", event);
        }
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!shouldLogKey(keyCode)) {
            return super.onKeyDown(keyCode, event);
        }
        int repeatCount = event.getRepeatCount();
        String str;
        StringBuilder stringBuilder;
        if (repeatCount == 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Key Down: ");
            stringBuilder.append(event);
            Log.i(str, stringBuilder.toString());
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Key Repeat #");
            stringBuilder.append(repeatCount);
            stringBuilder.append(": ");
            stringBuilder.append(event);
            Log.i(str, stringBuilder.toString());
        }
        return true;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!shouldLogKey(keyCode)) {
            return super.onKeyUp(keyCode, event);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Key Up: ");
        stringBuilder.append(event);
        Log.i(str, stringBuilder.toString());
        return true;
    }

    private static boolean shouldLogKey(int keyCode) {
        boolean z = true;
        switch (keyCode) {
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
                return true;
            default:
                if (!(KeyEvent.isGamepadButton(keyCode) || KeyEvent.isModifierKey(keyCode))) {
                    z = false;
                }
                return z;
        }
    }

    public boolean onTrackballEvent(MotionEvent event) {
        logMotionEvent("Trackball", event);
        return true;
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mIm.registerInputDeviceListener(this, getHandler());
        logInputDevices();
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mIm.unregisterInputDeviceListener(this);
    }

    public void onInputDeviceAdded(int deviceId) {
        logInputDeviceState(deviceId, "Device Added");
    }

    public void onInputDeviceChanged(int deviceId) {
        logInputDeviceState(deviceId, "Device Changed");
    }

    public void onInputDeviceRemoved(int deviceId) {
        logInputDeviceState(deviceId, "Device Removed");
    }

    private void logInputDevices() {
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int logInputDeviceState : deviceIds) {
            logInputDeviceState(logInputDeviceState, "Device Enumerated");
        }
    }

    private void logInputDeviceState(int deviceId, String state) {
        InputDevice device = this.mIm.getInputDevice(deviceId);
        String str;
        StringBuilder stringBuilder;
        if (device != null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(state);
            stringBuilder.append(": ");
            stringBuilder.append(device);
            Log.i(str, stringBuilder.toString());
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append(state);
        stringBuilder.append(": ");
        stringBuilder.append(deviceId);
        Log.i(str, stringBuilder.toString());
    }
}
