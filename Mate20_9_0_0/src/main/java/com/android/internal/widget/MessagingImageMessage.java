package com.android.internal.widget;

import android.app.Notification.MessagingStyle.Message;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pools.SimplePool;
import android.util.Pools.SynchronizedPool;
import android.view.LayoutInflater;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.RemoteViews.RemoteView;
import java.io.IOException;

@RemoteView
public class MessagingImageMessage extends ImageView implements MessagingMessage {
    private static final String TAG = "MessagingImageMessage";
    private static SimplePool<MessagingImageMessage> sInstancePool = new SynchronizedPool(10);
    private int mActualHeight;
    private int mActualWidth;
    private float mAspectRatio;
    private Drawable mDrawable;
    private final int mExtraSpacing;
    private final int mImageRounding;
    private boolean mIsIsolated;
    private final int mIsolatedSize;
    private final int mMaxImageHeight;
    private final int mMinImageHeight;
    private final Path mPath;
    private final MessagingMessageState mState;

    public MessagingImageMessage(Context context) {
        this(context, null);
    }

    public MessagingImageMessage(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MessagingImageMessage(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MessagingImageMessage(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mState = new MessagingMessageState(this);
        this.mPath = new Path();
        this.mMinImageHeight = context.getResources().getDimensionPixelSize(17105183);
        this.mMaxImageHeight = context.getResources().getDimensionPixelSize(17105182);
        this.mImageRounding = context.getResources().getDimensionPixelSize(17105184);
        this.mExtraSpacing = context.getResources().getDimensionPixelSize(17105181);
        setMaxHeight(this.mMaxImageHeight);
        this.mIsolatedSize = getResources().getDimensionPixelSize(17105179);
    }

    public MessagingMessageState getState() {
        return this.mState;
    }

    public boolean setMessage(Message message) {
        super.setMessage(message);
        try {
            Drawable drawable = LocalImageResolver.resolveImage(message.getDataUri(), getContext());
            int intrinsicHeight = drawable.getIntrinsicHeight();
            if (intrinsicHeight == 0) {
                Log.w(TAG, "Drawable with 0 intrinsic height was returned");
                return false;
            }
            this.mDrawable = drawable;
            this.mAspectRatio = ((float) this.mDrawable.getIntrinsicWidth()) / ((float) intrinsicHeight);
            setImageDrawable(drawable);
            setContentDescription(message.getText());
            return true;
        } catch (IOException | SecurityException e) {
            e.printStackTrace();
            return false;
        }
    }

    static MessagingMessage createMessage(MessagingLayout layout, Message m) {
        MessagingLinearLayout messagingLinearLayout = layout.getMessagingLinearLayout();
        MessagingImageMessage createdMessage = (MessagingImageMessage) sInstancePool.acquire();
        if (createdMessage == null) {
            createdMessage = (MessagingImageMessage) LayoutInflater.from(layout.getContext()).inflate(17367199, messagingLinearLayout, false);
            createdMessage.addOnLayoutChangeListener(MessagingLayout.MESSAGING_PROPERTY_ANIMATOR);
        }
        if (createdMessage.setMessage(m)) {
            return createdMessage;
        }
        createdMessage.recycle();
        return MessagingTextMessage.createMessage(layout, m);
    }

    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.clipPath(getRoundedRectPath());
        int width = (int) Math.max((float) getActualWidth(), ((float) getActualHeight()) * this.mAspectRatio);
        int left = (int) (((float) (getActualWidth() - width)) / 1073741824);
        this.mDrawable.setBounds(left, 0, left + width, (int) (((float) width) / this.mAspectRatio));
        this.mDrawable.draw(canvas);
        canvas.restore();
    }

    public Path getRoundedRectPath() {
        int right = getActualWidth();
        int bottom = getActualHeight();
        this.mPath.reset();
        float roundnessX = (float) this.mImageRounding;
        float roundnessY = (float) this.mImageRounding;
        roundnessX = Math.min((float) ((right - 0) / 2), roundnessX);
        roundnessY = Math.min((float) ((bottom - 0) / 2), roundnessY);
        this.mPath.moveTo((float) null, ((float) null) + roundnessY);
        this.mPath.quadTo((float) null, (float) null, ((float) null) + roundnessX, (float) null);
        this.mPath.lineTo(((float) right) - roundnessX, (float) null);
        this.mPath.quadTo((float) right, (float) null, (float) right, ((float) null) + roundnessY);
        this.mPath.lineTo((float) right, ((float) bottom) - roundnessY);
        this.mPath.quadTo((float) right, (float) bottom, ((float) right) - roundnessX, (float) bottom);
        this.mPath.lineTo(((float) null) + roundnessX, (float) bottom);
        this.mPath.quadTo((float) null, (float) bottom, (float) null, ((float) bottom) - roundnessY);
        this.mPath.close();
        return this.mPath;
    }

    public void recycle() {
        super.recycle();
        setImageBitmap(null);
        this.mDrawable = null;
        sInstancePool.release(this);
    }

    public static void dropCache() {
        sInstancePool = new SynchronizedPool(10);
    }

    public int getMeasuredType() {
        int minImageHeight;
        int measuredHeight = getMeasuredHeight();
        if (this.mIsIsolated) {
            minImageHeight = this.mIsolatedSize;
        } else {
            minImageHeight = this.mMinImageHeight;
        }
        boolean measuredTooSmall = measuredHeight < minImageHeight && measuredHeight != this.mDrawable.getIntrinsicHeight();
        if (measuredTooSmall) {
            return 2;
        }
        return (this.mIsIsolated || measuredHeight == this.mDrawable.getIntrinsicHeight()) ? 0 : 1;
    }

    public void setMaxDisplayedLines(int lines) {
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.mIsIsolated) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        setActualWidth(getStaticWidth());
        setActualHeight(getHeight());
    }

    public int getConsumedLines() {
        return 3;
    }

    public void setActualWidth(int actualWidth) {
        this.mActualWidth = actualWidth;
        invalidate();
    }

    public int getActualWidth() {
        return this.mActualWidth;
    }

    public void setActualHeight(int actualHeight) {
        this.mActualHeight = actualHeight;
        invalidate();
    }

    public int getActualHeight() {
        return this.mActualHeight;
    }

    public int getStaticWidth() {
        if (this.mIsIsolated) {
            return getWidth();
        }
        return (int) (((float) getHeight()) * this.mAspectRatio);
    }

    public void setIsolated(boolean isolated) {
        if (this.mIsIsolated != isolated) {
            this.mIsIsolated = isolated;
            MarginLayoutParams layoutParams = (MarginLayoutParams) getLayoutParams();
            layoutParams.topMargin = isolated ? 0 : this.mExtraSpacing;
            setLayoutParams(layoutParams);
        }
    }

    public int getExtraSpacing() {
        return this.mExtraSpacing;
    }
}
