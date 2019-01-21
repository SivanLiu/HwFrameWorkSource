package com.android.internal.telephony.cat;

import android.telephony.Rlog;
import java.util.ArrayList;
import java.util.List;

public class ComprehensionTlv {
    private static final String LOG_TAG = "ComprehensionTlv";
    private boolean mCr;
    private int mLength;
    private byte[] mRawValue;
    private int mTag;
    private int mValueIndex;

    protected ComprehensionTlv(int tag, boolean cr, int length, byte[] data, int valueIndex) {
        this.mTag = tag;
        this.mCr = cr;
        this.mLength = length;
        this.mValueIndex = valueIndex;
        this.mRawValue = data;
    }

    public int getTag() {
        return this.mTag;
    }

    public boolean isComprehensionRequired() {
        return this.mCr;
    }

    public int getLength() {
        return this.mLength;
    }

    public int getValueIndex() {
        return this.mValueIndex;
    }

    public byte[] getRawValue() {
        return this.mRawValue;
    }

    public static List<ComprehensionTlv> decodeMany(byte[] data, int startIndex) throws ResultException {
        ArrayList<ComprehensionTlv> items = new ArrayList();
        int endIndex = data.length;
        while (startIndex < endIndex) {
            ComprehensionTlv ctlv = decode(data, startIndex);
            if (ctlv == null) {
                CatLog.d(LOG_TAG, "decodeMany: ctlv is null, stop decoding");
                break;
            }
            items.add(ctlv);
            startIndex = ctlv.mValueIndex + ctlv.mLength;
        }
        return items;
    }

    /* JADX WARNING: Missing block: B:11:0x001b, code skipped:
            r11 = r4;
            r10 = r5;
     */
    /* JADX WARNING: Missing block: B:17:0x0038, code skipped:
            r4 = r2 + 1;
     */
    /* JADX WARNING: Missing block: B:20:0x003c, code skipped:
            r0 = r12[r2] & 255;
     */
    /* JADX WARNING: Missing block: B:21:0x0040, code skipped:
            if (r0 >= 128) goto L_0x0047;
     */
    /* JADX WARNING: Missing block: B:22:0x0042, code skipped:
            r7 = r0;
            r3 = r4;
     */
    /* JADX WARNING: Missing block: B:24:0x0049, code skipped:
            if (r0 != 129) goto L_0x008f;
     */
    /* JADX WARNING: Missing block: B:25:0x004b, code skipped:
            r5 = r4 + 1;
     */
    /* JADX WARNING: Missing block: B:27:?, code skipped:
            r3 = 255 & r12[r4];
     */
    /* JADX WARNING: Missing block: B:28:0x0050, code skipped:
            if (r3 < 128) goto L_0x0056;
     */
    /* JADX WARNING: Missing block: B:29:0x0052, code skipped:
            r7 = r3;
            r3 = r5;
     */
    /* JADX WARNING: Missing block: B:30:0x0056, code skipped:
            r4 = com.android.internal.telephony.cat.ResultCode.CMD_DATA_NOT_UNDERSTOOD;
            r6 = new java.lang.StringBuilder();
            r6.append("length < 0x80 length=");
            r6.append(java.lang.Integer.toHexString(r3));
            r6.append(" startIndex=");
            r6.append(r13);
            r6.append(" curIndex=");
            r6.append(r5);
            r6.append(" endIndex=");
            r6.append(r1);
     */
    /* JADX WARNING: Missing block: B:31:0x008a, code skipped:
            throw new com.android.internal.telephony.cat.ResultException(r4, r6.toString());
     */
    /* JADX WARNING: Missing block: B:33:0x008c, code skipped:
            r3 = r5;
     */
    /* JADX WARNING: Missing block: B:35:0x0091, code skipped:
            if (r0 != 130) goto L_0x00db;
     */
    /* JADX WARNING: Missing block: B:38:0x009c, code skipped:
            r2 = ((r12[r4] & 255) << 8) | (255 & r12[r4 + 1]);
            r3 = r4 + 2;
     */
    /* JADX WARNING: Missing block: B:39:0x00a2, code skipped:
            if (r2 < 256) goto L_0x00a6;
     */
    /* JADX WARNING: Missing block: B:40:0x00a4, code skipped:
            r7 = r2;
     */
    /* JADX WARNING: Missing block: B:42:?, code skipped:
            r5 = com.android.internal.telephony.cat.ResultCode.CMD_DATA_NOT_UNDERSTOOD;
            r6 = new java.lang.StringBuilder();
            r6.append("two byte length < 0x100 length=");
            r6.append(java.lang.Integer.toHexString(r2));
            r6.append(" startIndex=");
            r6.append(r13);
            r6.append(" curIndex=");
            r6.append(r3);
            r6.append(" endIndex=");
            r6.append(r1);
     */
    /* JADX WARNING: Missing block: B:43:0x00da, code skipped:
            throw new com.android.internal.telephony.cat.ResultException(r5, r6.toString());
     */
    /* JADX WARNING: Missing block: B:45:0x00dd, code skipped:
            if (r0 != 131) goto L_0x013d;
     */
    /* JADX WARNING: Missing block: B:48:0x00f0, code skipped:
            r2 = (((r12[r4] & 255) << 16) | ((r12[r4 + 1] & 255) << 8)) | (255 & r12[r4 + 2]);
            r3 = r4 + 3;
     */
    /* JADX WARNING: Missing block: B:49:0x00f6, code skipped:
            if (r2 < 65536) goto L_0x0108;
     */
    /* JADX WARNING: Missing block: B:52:0x0104, code skipped:
            return new com.android.internal.telephony.cat.ComprehensionTlv(r10, r11, r7, r12, r3);
     */
    /* JADX WARNING: Missing block: B:55:0x0108, code skipped:
            r5 = com.android.internal.telephony.cat.ResultCode.CMD_DATA_NOT_UNDERSTOOD;
            r6 = new java.lang.StringBuilder();
            r6.append("three byte length < 0x10000 length=0x");
            r6.append(java.lang.Integer.toHexString(r2));
            r6.append(" startIndex=");
            r6.append(r13);
            r6.append(" curIndex=");
            r6.append(r3);
            r6.append(" endIndex=");
            r6.append(r1);
     */
    /* JADX WARNING: Missing block: B:56:0x013c, code skipped:
            throw new com.android.internal.telephony.cat.ResultException(r5, r6.toString());
     */
    /* JADX WARNING: Missing block: B:58:?, code skipped:
            r3 = com.android.internal.telephony.cat.ResultCode.CMD_DATA_NOT_UNDERSTOOD;
            r5 = new java.lang.StringBuilder();
            r5.append("Bad length modifer=");
            r5.append(r0);
            r5.append(" startIndex=");
            r5.append(r13);
            r5.append(" curIndex=");
            r5.append(r4);
            r5.append(" endIndex=");
            r5.append(r1);
     */
    /* JADX WARNING: Missing block: B:59:0x016d, code skipped:
            throw new com.android.internal.telephony.cat.ResultException(r3, r5.toString());
     */
    /* JADX WARNING: Missing block: B:61:0x016f, code skipped:
            r3 = r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static ComprehensionTlv decode(byte[] data, int startIndex) throws ResultException {
        int curIndex = startIndex;
        int endIndex = data.length;
        int curIndex2 = curIndex + 1;
        try {
            curIndex = data[curIndex] & 255;
            if (!(curIndex == 0 || curIndex == 255)) {
                boolean cr = false;
                int tag;
                int tag2;
                switch (curIndex) {
                    case 127:
                        tag = ((data[curIndex2] & 255) << 8) | (data[curIndex2 + 1] & 255);
                        if ((32768 & tag) != 0) {
                            cr = true;
                        }
                        tag2 = -32769 & tag;
                        curIndex2 += 2;
                        break;
                    case 128:
                        break;
                    default:
                        tag = curIndex;
                        if ((tag & 128) != 0) {
                            cr = true;
                        }
                        tag2 = tag & -129;
                        break;
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("decode: unexpected first tag byte=");
            stringBuilder.append(Integer.toHexString(curIndex));
            stringBuilder.append(", startIndex=");
            stringBuilder.append(startIndex);
            stringBuilder.append(" curIndex=");
            stringBuilder.append(curIndex2);
            stringBuilder.append(" endIndex=");
            stringBuilder.append(endIndex);
            Rlog.d("CAT     ", stringBuilder.toString());
            return null;
        } catch (IndexOutOfBoundsException e) {
            int curIndex3 = curIndex2;
            ResultCode resultCode = ResultCode.CMD_DATA_NOT_UNDERSTOOD;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("IndexOutOfBoundsException startIndex=");
            stringBuilder2.append(startIndex);
            stringBuilder2.append(" curIndex=");
            stringBuilder2.append(curIndex3);
            stringBuilder2.append(" endIndex=");
            stringBuilder2.append(endIndex);
            throw new ResultException(resultCode, stringBuilder2.toString());
        }
    }
}
