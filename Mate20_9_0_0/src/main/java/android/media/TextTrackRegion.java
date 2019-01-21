package android.media;

/* compiled from: WebVttRenderer */
class TextTrackRegion {
    static final int SCROLL_VALUE_NONE = 300;
    static final int SCROLL_VALUE_SCROLL_UP = 301;
    float mAnchorPointX = 0.0f;
    float mAnchorPointY = 100.0f;
    String mId = "";
    int mLines = 3;
    int mScrollValue = 300;
    float mViewportAnchorPointX = 0.0f;
    float mViewportAnchorPointY = 100.0f;
    float mWidth = 100.0f;

    TextTrackRegion() {
    }

    public String toString() {
        String str;
        StringBuilder res = new StringBuilder(" {id:\"");
        res.append(this.mId);
        res.append("\", width:");
        res.append(this.mWidth);
        res.append(", lines:");
        res.append(this.mLines);
        res.append(", anchorPoint:(");
        res.append(this.mAnchorPointX);
        res.append(", ");
        res.append(this.mAnchorPointY);
        res.append("), viewportAnchorPoints:");
        res.append(this.mViewportAnchorPointX);
        res.append(", ");
        res.append(this.mViewportAnchorPointY);
        res.append("), scrollValue:");
        if (this.mScrollValue == 300) {
            str = "none";
        } else if (this.mScrollValue == 301) {
            str = "scroll_up";
        } else {
            str = "INVALID";
        }
        res.append(str);
        return res.append("}").toString();
    }
}
