package android.media.hwmnote;

import android.util.ArrayMap;

class HwMnoteIfdData {
    private static final short TAG_ID_MAKER_NOTE = (short) -28036;
    private static final int[] sIfds = new int[]{0, 1, 2};
    private final ArrayMap<Short, HwMnoteTag> mHwMnoteTags = new ArrayMap();
    private final int mIfdId;
    private int mOffsetToNextIfd = 0;

    HwMnoteIfdData(int ifdId) {
        this.mIfdId = ifdId;
    }

    protected static int[] getIfds() {
        return sIfds;
    }

    protected HwMnoteTag[] getAllTags() {
        return (HwMnoteTag[]) this.mHwMnoteTags.values().toArray(new HwMnoteTag[this.mHwMnoteTags.size()]);
    }

    protected int getId() {
        return this.mIfdId;
    }

    protected HwMnoteTag getTag(short tagId) {
        return (HwMnoteTag) this.mHwMnoteTags.get(Short.valueOf(tagId));
    }

    protected HwMnoteTag setTag(HwMnoteTag tag) {
        tag.setIfd(this.mIfdId);
        return (HwMnoteTag) this.mHwMnoteTags.put(Short.valueOf(tag.getTagId()), tag);
    }

    protected boolean checkCollision(short tagId) {
        return this.mHwMnoteTags.get(Short.valueOf(tagId)) != null;
    }

    protected void removeTag(short tagId) {
        this.mHwMnoteTags.remove(Short.valueOf(tagId));
    }

    protected int getTagCount() {
        return this.mHwMnoteTags.size();
    }

    protected void setOffsetToNextIfd(int offset) {
        this.mOffsetToNextIfd = offset;
    }

    protected int getOffsetToNextIfd() {
        return this.mOffsetToNextIfd;
    }
}
