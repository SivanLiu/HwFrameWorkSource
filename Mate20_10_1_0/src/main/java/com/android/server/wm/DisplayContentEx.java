package com.android.server.wm;

public class DisplayContentEx {
    private DisplayContent mDisplayContent;

    public void setDisplayContent(DisplayContent displayContent) {
        this.mDisplayContent = displayContent;
    }

    public DisplayContent getDisplayContent() {
        return this.mDisplayContent;
    }

    public DisplayPolicyEx getDisplayPolicyEx() {
        DisplayContent displayContent = this.mDisplayContent;
        if (displayContent == null || displayContent.getDisplayPolicy() == null) {
            return null;
        }
        DisplayPolicyEx displayPolicyEx = new DisplayPolicyEx();
        displayPolicyEx.setDisplayPolicy(this.mDisplayContent.getDisplayPolicy());
        return displayPolicyEx;
    }
}
