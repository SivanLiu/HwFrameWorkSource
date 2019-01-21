package com.android.internal.widget;

import android.app.ActivityManager;
import android.app.Notification.MessagingStyle.Message;
import android.view.View;
import com.android.internal.widget.MessagingLinearLayout.MessagingChild;
import java.util.Objects;

public interface MessagingMessage extends MessagingChild {
    public static final String IMAGE_MIME_TYPE_PREFIX = "image/";

    MessagingMessageState getState();

    int getVisibility();

    void setVisibility(int i);

    static MessagingMessage createMessage(MessagingLayout layout, Message m) {
        if (!hasImage(m) || ActivityManager.isLowRamDeviceStatic()) {
            return MessagingTextMessage.createMessage(layout, m);
        }
        return MessagingImageMessage.createMessage(layout, m);
    }

    static void dropCache() {
        MessagingTextMessage.dropCache();
        MessagingImageMessage.dropCache();
    }

    static boolean hasImage(Message m) {
        return (m.getDataUri() == null || m.getDataMimeType() == null || !m.getDataMimeType().startsWith(IMAGE_MIME_TYPE_PREFIX)) ? false : true;
    }

    boolean setMessage(Message message) {
        getState().setMessage(message);
        return true;
    }

    Message getMessage() {
        return getState().getMessage();
    }

    boolean sameAs(Message message) {
        Message ownMessage = getMessage();
        if (!Objects.equals(message.getText(), ownMessage.getText()) || !Objects.equals(message.getSender(), ownMessage.getSender())) {
            return false;
        }
        if (((message.isRemoteInputHistory() != ownMessage.isRemoteInputHistory()) || Objects.equals(Long.valueOf(message.getTimestamp()), Long.valueOf(ownMessage.getTimestamp()))) && Objects.equals(message.getDataMimeType(), ownMessage.getDataMimeType()) && Objects.equals(message.getDataUri(), ownMessage.getDataUri())) {
            return true;
        }
        return false;
    }

    boolean sameAs(MessagingMessage message) {
        return sameAs(message.getMessage());
    }

    void removeMessage() {
        getGroup().removeMessage(this);
    }

    void setMessagingGroup(MessagingGroup group) {
        getState().setGroup(group);
    }

    void setIsHistoric(boolean isHistoric) {
        getState().setIsHistoric(isHistoric);
    }

    MessagingGroup getGroup() {
        return getState().getGroup();
    }

    void setIsHidingAnimated(boolean isHiding) {
        getState().setIsHidingAnimated(isHiding);
    }

    boolean isHidingAnimated() {
        return getState().isHidingAnimated();
    }

    void hideAnimated() {
        setIsHidingAnimated(true);
        getGroup().performRemoveAnimation(getView(), new -$$Lambda$MessagingMessage$goi5oiwdlMBbUvfJzNl7fGbZ-K0(this));
    }

    boolean hasOverlappingRendering() {
        return false;
    }

    void recycle() {
        getState().recycle();
    }

    View getView() {
        return (View) this;
    }

    void setColor(int textColor) {
    }
}
