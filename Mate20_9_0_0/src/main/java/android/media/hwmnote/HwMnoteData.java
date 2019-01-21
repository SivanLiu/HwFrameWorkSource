package android.media.hwmnote;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

class HwMnoteData {
    private static final String TAG = "HwMnoteData";
    private final ByteOrder mByteOrder;
    private final HwMnoteIfdData[] mIfdDatas = new HwMnoteIfdData[3];

    HwMnoteData(ByteOrder order) {
        this.mByteOrder = order;
    }

    protected ByteOrder getByteOrder() {
        return this.mByteOrder;
    }

    protected HwMnoteIfdData getIfdData(int ifdId) {
        if (HwMnoteTag.isValidIfd(ifdId)) {
            return this.mIfdDatas[ifdId];
        }
        return null;
    }

    protected void addIfdData(HwMnoteIfdData data) {
        this.mIfdDatas[data.getId()] = data;
    }

    protected HwMnoteIfdData getOrCreateIfdData(int ifdId) {
        HwMnoteIfdData ifdData = this.mIfdDatas[ifdId];
        if (ifdData != null) {
            return ifdData;
        }
        ifdData = new HwMnoteIfdData(ifdId);
        this.mIfdDatas[ifdId] = ifdData;
        return ifdData;
    }

    protected HwMnoteTag getTag(short tag, int ifd) {
        HwMnoteIfdData ifdData = this.mIfdDatas[ifd];
        return ifdData == null ? null : ifdData.getTag(tag);
    }

    protected HwMnoteTag addTag(HwMnoteTag tag) {
        if (tag != null) {
            return addTag(tag, tag.getIfd());
        }
        return null;
    }

    protected HwMnoteTag addTag(HwMnoteTag tag, int ifdId) {
        if (tag == null || !HwMnoteTag.isValidIfd(ifdId)) {
            return null;
        }
        return getOrCreateIfdData(ifdId).setTag(tag);
    }

    protected void removeTag(short tagId, int ifdId) {
        HwMnoteIfdData ifdData = this.mIfdDatas[ifdId];
        if (ifdData != null) {
            ifdData.removeTag(tagId);
        }
    }

    protected List<HwMnoteTag> getAllTags() {
        ArrayList<HwMnoteTag> ret = new ArrayList();
        for (HwMnoteIfdData d : this.mIfdDatas) {
            if (d != null) {
                HwMnoteTag[] tags = d.getAllTags();
                if (tags != null) {
                    for (HwMnoteTag t : tags) {
                        ret.add(t);
                    }
                }
            }
        }
        if (ret.size() == 0) {
            return null;
        }
        return ret;
    }
}
