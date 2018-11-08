package android.media.hwmnote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;

class HwMnoteOutputStream {
    private static final boolean DEBUG = false;
    protected static final byte[] HW_MNOTE_HEADER = new byte[]{(byte) 72, (byte) 85, (byte) 65, (byte) 87, (byte) 69, (byte) 73, (byte) 0, (byte) 0};
    private static final int MAX_HW_MNOTE_SIZE = 65535;
    private static final int STATE_FRAME_HEADER = 1;
    private static final int STATE_SOI = 0;
    private static final int STREAMBUFFER_SIZE = 65536;
    private static final String TAG = "HwMnoteOutputStream";
    private static final short TAG_SIZE = (short) 12;
    private static final short TIFF_BIG_ENDIAN = (short) 19789;
    private static final short TIFF_HEADER = (short) 42;
    private static final short TIFF_HEADER_SIZE = (short) 8;
    private static final short TIFF_LITTLE_ENDIAN = (short) 18761;
    private HwMnoteData mHwMnoteData;

    protected HwMnoteOutputStream() {
    }

    protected void setData(HwMnoteData hwMnoteData) {
        this.mHwMnoteData = hwMnoteData;
    }

    protected HwMnoteData getHwMnoteData() {
        return this.mHwMnoteData;
    }

    public byte[] getHwMnoteBuffer() throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        if (outStream == null) {
            throw new IOException("Can't create ByteArrayOutputStream");
        }
        writeHwMnoteData(outStream);
        byte[] mnoteBuffer = outStream.toByteArray();
        outStream.close();
        return mnoteBuffer;
    }

    private void writeHwMnoteData(OutputStream outStream) throws IOException {
        if (this.mHwMnoteData != null) {
            ArrayList<HwMnoteTag> nullTags = stripNullValueTags(this.mHwMnoteData);
            createRequiredIfdAndTag();
            if (calculateAllOffset() + 8 > MAX_HW_MNOTE_SIZE) {
                throw new IOException("Mnote HW header is too large (>64Kb)");
            }
            OrderedDataOutputStream dataOutputStream = new OrderedDataOutputStream(outStream);
            dataOutputStream.setByteOrder(ByteOrder.BIG_ENDIAN);
            dataOutputStream.write(HW_MNOTE_HEADER);
            if (this.mHwMnoteData.getByteOrder() == ByteOrder.BIG_ENDIAN) {
                dataOutputStream.writeShort(TIFF_BIG_ENDIAN);
            } else {
                dataOutputStream.writeShort(TIFF_LITTLE_ENDIAN);
            }
            dataOutputStream.setByteOrder(this.mHwMnoteData.getByteOrder());
            dataOutputStream.writeShort((short) 42);
            dataOutputStream.writeInt(8);
            writeAllTags(dataOutputStream);
            int size = nullTags.size();
            for (int i = 0; i < size; i++) {
                this.mHwMnoteData.addTag((HwMnoteTag) nullTags.get(i));
            }
        }
    }

    private ArrayList<HwMnoteTag> stripNullValueTags(HwMnoteData data) {
        ArrayList<HwMnoteTag> nullTags = new ArrayList();
        if (data == null || data.getAllTags() == null) {
            return nullTags;
        }
        for (HwMnoteTag t : data.getAllTags()) {
            if (t.getValue() == null && (HwMnoteInterfaceImpl.isOffsetTag(t.getTagId()) ^ 1) != 0) {
                data.removeTag(t.getTagId(), t.getIfd());
                nullTags.add(t);
            }
        }
        return nullTags;
    }

    private void writeAllTags(OrderedDataOutputStream dataOutputStream) throws IOException {
        writeIfd(this.mHwMnoteData.getIfdData(0), dataOutputStream);
        HwMnoteIfdData sceneIfd = this.mHwMnoteData.getIfdData(1);
        if (sceneIfd != null) {
            writeIfd(sceneIfd, dataOutputStream);
        }
        HwMnoteIfdData faceIfd = this.mHwMnoteData.getIfdData(2);
        if (faceIfd != null) {
            writeIfd(faceIfd, dataOutputStream);
        }
    }

    private void writeIfd(HwMnoteIfdData ifd, OrderedDataOutputStream dataOutputStream) throws IOException {
        int i = 0;
        HwMnoteTag[] tags = ifd.getAllTags();
        dataOutputStream.writeShort((short) tags.length);
        for (HwMnoteTag tag : tags) {
            HwMnoteTag tag2;
            dataOutputStream.writeShort(tag2.getTagId());
            dataOutputStream.writeShort(tag2.getDataType());
            dataOutputStream.writeInt(tag2.getComponentCount());
            if (tag2.getDataSize() > 4) {
                dataOutputStream.writeInt(tag2.getOffset());
            } else {
                writeTagValue(tag2, dataOutputStream);
                int n = 4 - tag2.getDataSize();
                for (int i2 = 0; i2 < n; i2++) {
                    dataOutputStream.write(0);
                }
            }
        }
        dataOutputStream.writeInt(ifd.getOffsetToNextIfd());
        int length = tags.length;
        while (i < length) {
            tag2 = tags[i];
            if (tag2.getDataSize() > 4) {
                writeTagValue(tag2, dataOutputStream);
            }
            i++;
        }
    }

    private int calculateOffsetOfIfd(HwMnoteIfdData ifd, int offset) {
        offset += ((ifd.getTagCount() * 12) + 2) + 4;
        for (HwMnoteTag tag : ifd.getAllTags()) {
            if (tag.getDataSize() > 4) {
                tag.setOffset(offset);
                offset += tag.getDataSize();
            }
        }
        return offset;
    }

    private void createRequiredIfdAndTag() {
        HwMnoteIfdData ifd0 = this.mHwMnoteData.getIfdData(0);
        if (ifd0 == null) {
            ifd0 = new HwMnoteIfdData(0);
            this.mHwMnoteData.addIfdData(ifd0);
        }
        if (this.mHwMnoteData.getIfdData(1) != null) {
            ifd0.setTag(new HwMnoteTag(HwMnoteInterfaceImpl.getTrueTagKey(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_IFD), (short) 4, 1, 0, true));
        }
        if (this.mHwMnoteData.getIfdData(2) != null) {
            ifd0.setTag(new HwMnoteTag(HwMnoteInterfaceImpl.getTrueTagKey(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_IFD), (short) 4, 1, 0, true));
        }
    }

    private int calculateAllOffset() {
        HwMnoteIfdData ifd0 = this.mHwMnoteData.getIfdData(0);
        int offset = calculateOffsetOfIfd(ifd0, 8);
        HwMnoteIfdData sceneIfd = this.mHwMnoteData.getIfdData(1);
        if (sceneIfd != null) {
            ifd0.getTag(HwMnoteInterfaceImpl.getTrueTagKey(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_IFD)).setValue(offset);
            offset = calculateOffsetOfIfd(sceneIfd, offset);
        }
        HwMnoteIfdData faceIfd = this.mHwMnoteData.getIfdData(2);
        if (faceIfd == null) {
            return offset;
        }
        ifd0.getTag(HwMnoteInterfaceImpl.getTrueTagKey(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_IFD)).setValue(offset);
        return calculateOffsetOfIfd(faceIfd, offset);
    }

    static void writeTagValue(HwMnoteTag tag, OrderedDataOutputStream dataOutputStream) throws IOException {
        switch (tag.getDataType()) {
            case (short) 4:
                int n = tag.getComponentCount();
                for (int i = 0; i < n; i++) {
                    dataOutputStream.writeInt((int) tag.getValueAt(i));
                }
                return;
            case (short) 7:
                byte[] buf = new byte[tag.getComponentCount()];
                tag.getBytes(buf);
                dataOutputStream.write(buf);
                return;
            default:
                return;
        }
    }
}
