package android.media.hwmnote;

import android.util.SparseIntArray;
import com.huawei.hsm.permission.StubController;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.HashSet;

public class HwMnoteInterfaceImpl implements IHwMnoteInterface {
    public static final ByteOrder DEFAULT_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
    public static final int DEFINITION_NULL = 0;
    public static final int IFD_NULL = -1;
    private static final String NULL_ARGUMENT_STRING = "Argument is null";
    public static final int TAG_NULL = -1;
    private static HashSet<Short> sOffsetTags = new HashSet();
    protected HwMnoteData mData = new HwMnoteData(DEFAULT_BYTE_ORDER);
    private SparseIntArray mHwMnoteTagInfo = null;

    static {
        sOffsetTags.add(Short.valueOf(getTrueTagKey(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_IFD)));
        sOffsetTags.add(Short.valueOf(getTrueTagKey(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_IFD)));
    }

    public static short getTrueTagKey(int tag) {
        return (short) tag;
    }

    public static int getTrueIfd(int tag) {
        return tag >>> 16;
    }

    public void readHwMnote(byte[] mnote) throws IOException {
        InputStream inStream = new ByteArrayInputStream(mnote);
        if (inStream == null) {
            throw new IllegalArgumentException(NULL_ARGUMENT_STRING);
        }
        try {
            this.mData = new HwMnoteReader(this).read(inStream);
            inStream.close();
        } catch (Exception e) {
            inStream.close();
            throw new IOException("Invalid HwMnote format : " + e);
        }
    }

    public byte[] getHwMnote() throws IOException {
        HwMnoteOutputStream mos = new HwMnoteOutputStream();
        mos.setData(this.mData);
        return mos.getHwMnoteBuffer();
    }

    public void clearHwMnote() {
        this.mData = new HwMnoteData(DEFAULT_BYTE_ORDER);
    }

    public HwMnoteTag getTag(int tagId, int ifdId) {
        if (HwMnoteTag.isValidIfd(ifdId)) {
            return this.mData.getTag(getTrueTagKey(tagId), ifdId);
        }
        return null;
    }

    public Object getTagValue(int tagId) {
        HwMnoteTag t = getTag(tagId, getDefinedTagDefaultIfd(tagId));
        if (t == null) {
            return null;
        }
        return t.getValue();
    }

    public Long getTagLongValue(int tagId) {
        long[] l = getTagLongValues(tagId);
        if (l.length <= 0) {
            return null;
        }
        return Long.valueOf(l[0]);
    }

    public Integer getTagIntValue(int tagId) {
        int[] l = getTagIntValues(tagId);
        if (l == null || l.length <= 0) {
            return null;
        }
        return Integer.valueOf(l[0]);
    }

    public long[] getTagLongValues(int tagId) {
        HwMnoteTag t = getTag(tagId, getDefinedTagDefaultIfd(tagId));
        if (t == null) {
            return new long[0];
        }
        return t.getValueAsLongs();
    }

    public int[] getTagIntValues(int tagId) {
        HwMnoteTag t = getTag(tagId, getDefinedTagDefaultIfd(tagId));
        if (t == null) {
            return new int[0];
        }
        return t.getValueAsInts();
    }

    public byte[] getTagByteValues(int tagId) {
        HwMnoteTag t = getTag(tagId, getDefinedTagDefaultIfd(tagId));
        if (t == null) {
            return new byte[0];
        }
        return t.getValueAsBytes();
    }

    public int getDefinedTagDefaultIfd(int tagId) {
        if (getTagInfo().get(tagId) == 0) {
            return -1;
        }
        return getTrueIfd(tagId);
    }

    protected static boolean isOffsetTag(short tag) {
        return sOffsetTags.contains(Short.valueOf(tag));
    }

    private HwMnoteTag buildTag(int tagId, Object val) {
        int ifdId = getTrueIfd(tagId);
        int info = getTagInfo().get(tagId);
        if (info == 0 || val == null) {
            return null;
        }
        short type = getTypeFromInfo(info);
        int definedCount = getComponentCountFromInfo(info);
        boolean hasDefinedCount = definedCount != 0;
        if (!isIfdAllowed(info, ifdId)) {
            return null;
        }
        HwMnoteTag t = new HwMnoteTag(getTrueTagKey(tagId), type, definedCount, ifdId, hasDefinedCount);
        if (t.setValue(val)) {
            return t;
        }
        return null;
    }

    public boolean setTagValue(int tagId, Object val) {
        HwMnoteTag t = getTag(tagId, getDefinedTagDefaultIfd(tagId));
        if (t != null) {
            return t.setValue(val);
        }
        t = buildTag(tagId, val);
        if (t == null) {
            return false;
        }
        this.mData.addTag(t);
        return true;
    }

    public void deleteTag(int tagId) {
        this.mData.removeTag(getTrueTagKey(tagId), getDefinedTagDefaultIfd(tagId));
    }

    protected SparseIntArray getTagInfo() {
        if (this.mHwMnoteTagInfo == null) {
            this.mHwMnoteTagInfo = new SparseIntArray();
            initTagInfo();
        }
        return this.mHwMnoteTagInfo;
    }

    private void initTagInfo() {
        int ifdFlags = getFlagsFromAllowedIfds(new int[]{0}) << 24;
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_IFD, (ifdFlags | StubController.PERMISSION_CALLLOG_DELETE) | 1);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_IFD, (ifdFlags | StubController.PERMISSION_CALLLOG_DELETE) | 1);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_CAPTURE_MODE, (ifdFlags | StubController.PERMISSION_CALLLOG_DELETE) | 1);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_BURST_NUMBER, (ifdFlags | StubController.PERMISSION_CALLLOG_DELETE) | 1);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FRONT_CAMERA, (ifdFlags | StubController.PERMISSION_CALLLOG_DELETE) | 1);
        int sceneFlags = getFlagsFromAllowedIfds(new int[]{1}) << 24;
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_VERSION, (sceneFlags | StubController.PERMISSION_CALLLOG_DELETE) | 1);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_FOOD_CONF, (sceneFlags | StubController.PERMISSION_CALLLOG_DELETE) | 1);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_STAGE_CONF, (sceneFlags | StubController.PERMISSION_CALLLOG_DELETE) | 1);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_BLUESKY_CONF, (sceneFlags | StubController.PERMISSION_CALLLOG_DELETE) | 1);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_GREENPLANT_CONF, (sceneFlags | StubController.PERMISSION_CALLLOG_DELETE) | 1);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_BEACH_CONF, (sceneFlags | StubController.PERMISSION_CALLLOG_DELETE) | 1);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_SNOW_CONF, (sceneFlags | StubController.PERMISSION_CALLLOG_DELETE) | 1);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_SUNSET_CONF, (sceneFlags | StubController.PERMISSION_CALLLOG_DELETE) | 1);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_FLOWERS_CONF, (sceneFlags | StubController.PERMISSION_CALLLOG_DELETE) | 1);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_NIGHT_CONF, (sceneFlags | StubController.PERMISSION_CALLLOG_DELETE) | 1);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_TEXT_CONF, (sceneFlags | StubController.PERMISSION_CALLLOG_DELETE) | 1);
        int faceFlags = getFlagsFromAllowedIfds(new int[]{2}) << 24;
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_VERSION, (faceFlags | StubController.PERMISSION_CALLLOG_DELETE) | 1);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_COUNT, (faceFlags | StubController.PERMISSION_CALLLOG_DELETE) | 1);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_CONF, faceFlags | 458752);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_SMILE_SCORE, faceFlags | 458752);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_RECT, faceFlags | 458752);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_LEYE_CENTER, faceFlags | 458752);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_REYE_CENTER, faceFlags | 458752);
        this.mHwMnoteTagInfo.put(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_MOUTH_CENTER, faceFlags | 458752);
    }

    protected static int getAllowedIfdFlagsFromInfo(int info) {
        return info >>> 24;
    }

    protected static boolean isIfdAllowed(int info, int ifd) {
        int[] ifds = HwMnoteIfdData.getIfds();
        int ifdFlags = getAllowedIfdFlagsFromInfo(info);
        int i = 0;
        while (i < ifds.length) {
            if (ifd == ifds[i] && ((ifdFlags >> i) & 1) == 1) {
                return true;
            }
            i++;
        }
        return false;
    }

    protected static int getFlagsFromAllowedIfds(int[] allowedIfds) {
        if (allowedIfds == null || allowedIfds.length == 0) {
            return 0;
        }
        int flags = 0;
        int[] ifds = HwMnoteIfdData.getIfds();
        for (int i = 0; i < 3; i++) {
            for (int j : allowedIfds) {
                if (ifds[i] == j) {
                    flags |= 1 << i;
                    break;
                }
            }
        }
        return flags;
    }

    protected static short getTypeFromInfo(int info) {
        return (short) ((info >> 16) & 255);
    }

    protected static int getComponentCountFromInfo(int info) {
        return 65535 & info;
    }
}
