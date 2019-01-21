package com.android.internal.widget;

import android.app.Person;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Pools.SimplePool;
import android.util.Pools.SynchronizedPool;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RemoteViews.RemoteView;
import com.android.internal.widget.MessagingLinearLayout.LayoutParams;
import com.android.internal.widget.MessagingLinearLayout.MessagingChild;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RemoteView
public class MessagingGroup extends LinearLayout implements MessagingChild {
    private static SimplePool<MessagingGroup> sInstancePool = new SynchronizedPool(10);
    private ArrayList<MessagingMessage> mAddedMessages = new ArrayList();
    private Icon mAvatarIcon;
    private CharSequence mAvatarName = "";
    private String mAvatarSymbol = "";
    private ImageView mAvatarView;
    private Point mDisplaySize = new Point();
    private boolean mFirstLayout;
    private ViewGroup mImageContainer;
    private boolean mImagesAtEnd;
    private boolean mIsHidingAnimated;
    private MessagingImageMessage mIsolatedMessage;
    private int mLayoutColor;
    private MessagingLinearLayout mMessageContainer;
    private List<MessagingMessage> mMessages;
    private boolean mNeedsGeneratedAvatar;
    private Person mSender;
    private ImageFloatingTextView mSenderName;
    private ProgressBar mSendingSpinner;
    private View mSendingSpinnerContainer;
    private int mSendingTextColor;
    private int mTextColor;
    private boolean mTransformingImages;

    public MessagingGroup(Context context) {
        super(context);
    }

    public MessagingGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessagingGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MessagingGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mMessageContainer = (MessagingLinearLayout) findViewById(16908946);
        this.mSenderName = (ImageFloatingTextView) findViewById(16909078);
        this.mAvatarView = (ImageView) findViewById(16909077);
        this.mImageContainer = (ViewGroup) findViewById(16909080);
        this.mSendingSpinner = (ProgressBar) findViewById(16909081);
        this.mSendingSpinnerContainer = findViewById(16909082);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        this.mDisplaySize.x = displayMetrics.widthPixels;
        this.mDisplaySize.y = displayMetrics.heightPixels;
    }

    public void updateClipRect() {
        Rect clipRect;
        if (this.mSenderName.getVisibility() == 8 || this.mTransformingImages) {
            clipRect = null;
        } else {
            ViewGroup parent = (ViewGroup) this.mSenderName.getParent();
            int top = (getDistanceFromParent(this.mSenderName, parent) - getDistanceFromParent(this.mMessageContainer, parent)) + this.mSenderName.getHeight();
            int size = Math.max(this.mDisplaySize.x, this.mDisplaySize.y);
            clipRect = new Rect(0, top, size, size);
        }
        this.mMessageContainer.setClipBounds(clipRect);
    }

    private int getDistanceFromParent(View searchedView, ViewGroup parent) {
        int position = 0;
        for (View view = searchedView; view != parent; view = (View) view.getParent()) {
            position = (int) (((float) position) + (((float) view.getTop()) + view.getTranslationY()));
        }
        return position;
    }

    public void setSender(Person sender, CharSequence nameOverride) {
        this.mSender = sender;
        if (nameOverride == null) {
            nameOverride = sender.getName();
        }
        this.mSenderName.setText(nameOverride);
        int i = 0;
        this.mNeedsGeneratedAvatar = sender.getIcon() == null;
        if (!this.mNeedsGeneratedAvatar) {
            setAvatar(sender.getIcon());
        }
        this.mAvatarView.setVisibility(0);
        ImageFloatingTextView imageFloatingTextView = this.mSenderName;
        if (TextUtils.isEmpty(nameOverride)) {
            i = 8;
        }
        imageFloatingTextView.setVisibility(i);
    }

    public void setSending(boolean sending) {
        int visibility = sending ? 0 : 8;
        if (this.mSendingSpinnerContainer.getVisibility() != visibility) {
            this.mSendingSpinnerContainer.setVisibility(visibility);
            updateMessageColor();
        }
    }

    private int calculateSendingTextColor() {
        TypedValue alphaValue = new TypedValue();
        this.mContext.getResources().getValue(17105244, alphaValue, true);
        return Color.valueOf((float) Color.red(this.mTextColor), (float) Color.green(this.mTextColor), (float) Color.blue(this.mTextColor), alphaValue.getFloat()).toArgb();
    }

    public void setAvatar(Icon icon) {
        this.mAvatarIcon = icon;
        this.mAvatarView.setImageIcon(icon);
        this.mAvatarSymbol = "";
        this.mAvatarName = "";
    }

    static MessagingGroup createGroup(MessagingLinearLayout layout) {
        MessagingGroup createdGroup = (MessagingGroup) sInstancePool.acquire();
        if (createdGroup == null) {
            createdGroup = (MessagingGroup) LayoutInflater.from(layout.getContext()).inflate(17367198, layout, false);
            createdGroup.addOnLayoutChangeListener(MessagingLayout.MESSAGING_PROPERTY_ANIMATOR);
        }
        layout.addView(createdGroup);
        return createdGroup;
    }

    public void removeMessage(MessagingMessage messagingMessage) {
        View view = messagingMessage.getView();
        boolean wasShown = view.isShown();
        ViewGroup messageParent = (ViewGroup) view.getParent();
        if (messageParent != null) {
            messageParent.removeView(view);
            Runnable recycleRunnable = new -$$Lambda$MessagingGroup$uEKViIlAuE6AYNmbbTgLGe5mU7I(messageParent, view, messagingMessage);
            if (!wasShown || MessagingLinearLayout.isGone(view)) {
                recycleRunnable.run();
            } else {
                messageParent.addTransientView(view, 0);
                performRemoveAnimation(view, recycleRunnable);
            }
        }
    }

    static /* synthetic */ void lambda$removeMessage$0(ViewGroup messageParent, View view, MessagingMessage messagingMessage) {
        messageParent.removeTransientView(view);
        messagingMessage.recycle();
    }

    public void recycle() {
        if (this.mIsolatedMessage != null) {
            this.mImageContainer.removeView(this.mIsolatedMessage);
        }
        for (int i = 0; i < this.mMessages.size(); i++) {
            MessagingMessage message = (MessagingMessage) this.mMessages.get(i);
            this.mMessageContainer.removeView(message.getView());
            message.recycle();
        }
        setAvatar(null);
        this.mAvatarView.setAlpha(1.0f);
        this.mAvatarView.setTranslationY(0.0f);
        this.mSenderName.setAlpha(1.0f);
        this.mSenderName.setTranslationY(0.0f);
        setAlpha(1.0f);
        this.mIsolatedMessage = null;
        this.mMessages = null;
        this.mAddedMessages.clear();
        this.mFirstLayout = true;
        MessagingPropertyAnimator.recycle(this);
        sInstancePool.release(this);
    }

    public void removeGroupAnimated(Runnable endAction) {
        performRemoveAnimation(this, new -$$Lambda$MessagingGroup$QKnXYzCylYJqF8wEQG98SXlcu2M(this, endAction));
    }

    public static /* synthetic */ void lambda$removeGroupAnimated$1(MessagingGroup messagingGroup, Runnable endAction) {
        messagingGroup.setAlpha(1.0f);
        MessagingPropertyAnimator.setToLaidOutPosition(messagingGroup);
        if (endAction != null) {
            endAction.run();
        }
    }

    public void performRemoveAnimation(View message, Runnable endAction) {
        performRemoveAnimation(message, -message.getHeight(), endAction);
    }

    private void performRemoveAnimation(View view, int disappearTranslation, Runnable endAction) {
        MessagingPropertyAnimator.startLocalTranslationTo(view, disappearTranslation, MessagingLayout.FAST_OUT_LINEAR_IN);
        MessagingPropertyAnimator.fadeOut(view, endAction);
    }

    public CharSequence getSenderName() {
        return this.mSenderName.getText();
    }

    public static void dropCache() {
        sInstancePool = new SynchronizedPool(10);
    }

    public int getMeasuredType() {
        if (this.mIsolatedMessage != null) {
            return 1;
        }
        boolean hasNormal = false;
        int measureHeight = getMeasuredHeight();
        int i = this.mMessageContainer.getChildCount() - 1;
        while (true) {
            boolean tooSmall = false;
            if (i >= 0) {
                View child = this.mMessageContainer.getChildAt(i);
                if (child.getVisibility() != 8 && (child instanceof MessagingChild)) {
                    int type = ((MessagingChild) child).getMeasuredType();
                    if (type != 2 && measureHeight < child.getMeasuredHeight()) {
                        type = 2;
                    }
                    if (type == 2) {
                        tooSmall = true;
                    }
                    if (tooSmall | ((LayoutParams) child.getLayoutParams()).hide) {
                        if (hasNormal) {
                            return 1;
                        }
                        return 2;
                    } else if (type == 1) {
                        return 1;
                    } else {
                        hasNormal = true;
                    }
                }
                i--;
            } else if (this.mMessageContainer.getChildCount() != 0 || this.mIsolatedMessage == null) {
                return 0;
            } else {
                int type2 = this.mIsolatedMessage.getMeasuredType();
                if (type2 != 2 && measureHeight < this.mIsolatedMessage.getMeasuredHeight()) {
                    type2 = 2;
                }
                return type2;
            }
        }
    }

    public int getConsumedLines() {
        int result = 0;
        for (int i = 0; i < this.mMessageContainer.getChildCount(); i++) {
            View child = this.mMessageContainer.getChildAt(i);
            if (child instanceof MessagingChild) {
                result += ((MessagingChild) child).getConsumedLines();
            }
        }
        return (this.mIsolatedMessage != null ? Math.max(result, 1) : result) + 1;
    }

    public void setMaxDisplayedLines(int lines) {
        this.mMessageContainer.setMaxDisplayedLines(lines);
    }

    public void hideAnimated() {
        setIsHidingAnimated(true);
        removeGroupAnimated(new -$$Lambda$MessagingGroup$buM2CBWR7uz4neT0lee-MKMDx5M(this));
    }

    public boolean isHidingAnimated() {
        return this.mIsHidingAnimated;
    }

    private void setIsHidingAnimated(boolean isHiding) {
        ViewParent parent = getParent();
        this.mIsHidingAnimated = isHiding;
        invalidate();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).invalidate();
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public Icon getAvatarSymbolIfMatching(CharSequence avatarName, String avatarSymbol, int layoutColor) {
        if (this.mAvatarName.equals(avatarName) && this.mAvatarSymbol.equals(avatarSymbol) && layoutColor == this.mLayoutColor) {
            return this.mAvatarIcon;
        }
        return null;
    }

    public void setCreatedAvatar(Icon cachedIcon, CharSequence avatarName, String avatarSymbol, int layoutColor) {
        if (!this.mAvatarName.equals(avatarName) || !this.mAvatarSymbol.equals(avatarSymbol) || layoutColor != this.mLayoutColor) {
            setAvatar(cachedIcon);
            this.mAvatarSymbol = avatarSymbol;
            setLayoutColor(layoutColor);
            this.mAvatarName = avatarName;
        }
    }

    public void setTextColors(int senderTextColor, int messageTextColor) {
        this.mTextColor = messageTextColor;
        this.mSendingTextColor = calculateSendingTextColor();
        updateMessageColor();
        this.mSenderName.setTextColor(senderTextColor);
    }

    public void setLayoutColor(int layoutColor) {
        if (layoutColor != this.mLayoutColor) {
            this.mLayoutColor = layoutColor;
            this.mSendingSpinner.setIndeterminateTintList(ColorStateList.valueOf(this.mLayoutColor));
        }
    }

    private void updateMessageColor() {
        if (this.mMessages != null) {
            int color = this.mSendingSpinnerContainer.getVisibility() == 0 ? this.mSendingTextColor : this.mTextColor;
            for (MessagingMessage message : this.mMessages) {
                message.setColor(message.getMessage().isRemoteInputHistory() ? color : this.mTextColor);
            }
        }
    }

    public void setMessages(List<MessagingMessage> group) {
        MessagingImageMessage isolatedMessage = null;
        int textMessageIndex = 0;
        for (int messageIndex = 0; messageIndex < group.size(); messageIndex++) {
            MessagingMessage message = (MessagingMessage) group.get(messageIndex);
            if (message.getGroup() != this) {
                message.setMessagingGroup(this);
                this.mAddedMessages.add(message);
            }
            boolean isImage = message instanceof MessagingImageMessage;
            if (this.mImagesAtEnd && isImage) {
                isolatedMessage = (MessagingImageMessage) message;
            } else {
                if (removeFromParentIfDifferent(message, this.mMessageContainer)) {
                    ViewGroup.LayoutParams layoutParams = message.getView().getLayoutParams();
                    if (!(layoutParams == null || (layoutParams instanceof LayoutParams))) {
                        message.getView().setLayoutParams(this.mMessageContainer.generateDefaultLayoutParams());
                    }
                    this.mMessageContainer.addView(message.getView(), textMessageIndex);
                }
                if (isImage) {
                    ((MessagingImageMessage) message).setIsolated(false);
                }
                if (textMessageIndex != this.mMessageContainer.indexOfChild(message.getView())) {
                    this.mMessageContainer.removeView(message.getView());
                    this.mMessageContainer.addView(message.getView(), textMessageIndex);
                }
                textMessageIndex++;
            }
        }
        if (isolatedMessage != null) {
            if (removeFromParentIfDifferent(isolatedMessage, this.mImageContainer)) {
                this.mImageContainer.removeAllViews();
                this.mImageContainer.addView(isolatedMessage.getView());
            }
            isolatedMessage.setIsolated(true);
        } else if (this.mIsolatedMessage != null) {
            this.mImageContainer.removeAllViews();
        }
        this.mIsolatedMessage = isolatedMessage;
        updateImageContainerVisibility();
        this.mMessages = group;
        updateMessageColor();
    }

    private void updateImageContainerVisibility() {
        ViewGroup viewGroup = this.mImageContainer;
        int i = (this.mIsolatedMessage == null || !this.mImagesAtEnd) ? 8 : 0;
        viewGroup.setVisibility(i);
    }

    private boolean removeFromParentIfDifferent(MessagingMessage message, ViewGroup newParent) {
        ViewGroup parent = message.getView().getParent();
        if (parent == newParent) {
            return false;
        }
        if (parent instanceof ViewGroup) {
            parent.removeView(message.getView());
        }
        return true;
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!this.mAddedMessages.isEmpty()) {
            final boolean firstLayout = this.mFirstLayout;
            getViewTreeObserver().addOnPreDrawListener(new OnPreDrawListener() {
                public boolean onPreDraw() {
                    Iterator it = MessagingGroup.this.mAddedMessages.iterator();
                    while (it.hasNext()) {
                        MessagingMessage message = (MessagingMessage) it.next();
                        if (message.getView().isShown()) {
                            MessagingPropertyAnimator.fadeIn(message.getView());
                            if (!firstLayout) {
                                MessagingPropertyAnimator.startLocalTranslationFrom(message.getView(), message.getView().getHeight(), MessagingLayout.LINEAR_OUT_SLOW_IN);
                            }
                        }
                    }
                    MessagingGroup.this.mAddedMessages.clear();
                    MessagingGroup.this.getViewTreeObserver().removeOnPreDrawListener(this);
                    return true;
                }
            });
        }
        this.mFirstLayout = false;
        updateClipRect();
    }

    /* JADX WARNING: Missing block: B:11:0x004f, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int calculateGroupCompatibility(MessagingGroup otherGroup) {
        int i = 0;
        if (!TextUtils.equals(getSenderName(), otherGroup.getSenderName())) {
            return 0;
        }
        int result = 1;
        while (i < this.mMessages.size() && i < otherGroup.mMessages.size() && ((MessagingMessage) this.mMessages.get((this.mMessages.size() - 1) - i)).sameAs((MessagingMessage) otherGroup.mMessages.get((otherGroup.mMessages.size() - 1) - i))) {
            result++;
            i++;
        }
        return result;
    }

    public View getSenderView() {
        return this.mSenderName;
    }

    public View getAvatar() {
        return this.mAvatarView;
    }

    public MessagingLinearLayout getMessageContainer() {
        return this.mMessageContainer;
    }

    public MessagingImageMessage getIsolatedMessage() {
        return this.mIsolatedMessage;
    }

    public boolean needsGeneratedAvatar() {
        return this.mNeedsGeneratedAvatar;
    }

    public Person getSender() {
        return this.mSender;
    }

    public void setTransformingImages(boolean transformingImages) {
        this.mTransformingImages = transformingImages;
    }

    public void setDisplayImagesAtEnd(boolean atEnd) {
        if (this.mImagesAtEnd != atEnd) {
            this.mImagesAtEnd = atEnd;
            updateImageContainerVisibility();
        }
    }

    public List<MessagingMessage> getMessages() {
        return this.mMessages;
    }
}
