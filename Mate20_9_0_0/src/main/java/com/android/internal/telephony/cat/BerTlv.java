package com.android.internal.telephony.cat;

import com.android.internal.telephony.HwTelephonyFactory;
import java.util.List;

class BerTlv {
    public static final int BER_EVENT_DOWNLOAD_TAG = 214;
    public static final int BER_MENU_SELECTION_TAG = 211;
    public static final int BER_PROACTIVE_COMMAND_TAG = 208;
    public static final int BER_UNKNOWN_TAG = 0;
    private List<ComprehensionTlv> mCompTlvs = null;
    private boolean mLengthValid = true;
    private int mTag = 0;

    private BerTlv(int tag, List<ComprehensionTlv> ctlvs, boolean lengthValid) {
        this.mTag = tag;
        this.mCompTlvs = ctlvs;
        this.mLengthValid = lengthValid;
    }

    public List<ComprehensionTlv> getComprehensionTlvs() {
        return this.mCompTlvs;
    }

    public int getTag() {
        return this.mTag;
    }

    public boolean isLengthValid() {
        return this.mLengthValid;
    }

    public static BerTlv decode(byte[] data) throws ResultException {
        ResultCode resultCode;
        StringBuilder stringBuilder;
        ResultException e;
        int endIndex = data.length;
        int length = 0;
        boolean isLengthValid = true;
        int curIndex = 0 + 1;
        int curIndex2;
        try {
            ResultCode resultCode2;
            StringBuilder stringBuilder2;
            int tag = data[0] & 255;
            if (tag == BER_PROACTIVE_COMMAND_TAG) {
                curIndex2 = curIndex + 1;
                try {
                    curIndex = data[curIndex] & 255;
                    if (curIndex < 128) {
                        length = curIndex;
                        curIndex = curIndex2;
                    } else if (curIndex == 129) {
                        int curIndex3 = curIndex2 + 1;
                        try {
                            curIndex = data[curIndex2] & 255;
                            if (curIndex >= 128) {
                                length = curIndex;
                                curIndex = curIndex3;
                            } else {
                                resultCode2 = ResultCode.CMD_DATA_NOT_UNDERSTOOD;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("length < 0x80 length=");
                                stringBuilder2.append(Integer.toHexString(0));
                                stringBuilder2.append(" curIndex=");
                                stringBuilder2.append(curIndex3);
                                stringBuilder2.append(" endIndex=");
                                stringBuilder2.append(endIndex);
                                throw new ResultException(resultCode2, stringBuilder2.toString());
                            }
                        } catch (IndexOutOfBoundsException e2) {
                            curIndex2 = curIndex3;
                            resultCode = ResultCode.REQUIRED_VALUES_MISSING;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("IndexOutOfBoundsException  curIndex=");
                            stringBuilder.append(curIndex2);
                            stringBuilder.append(" endIndex=");
                            stringBuilder.append(endIndex);
                            throw new ResultException(resultCode, stringBuilder.toString());
                        } catch (ResultException e3) {
                            e = e3;
                            curIndex2 = curIndex3;
                            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, e.explanation());
                        }
                    } else {
                        resultCode2 = ResultCode.CMD_DATA_NOT_UNDERSTOOD;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Expected first byte to be length or a length tag and < 0x81 byte= ");
                        stringBuilder2.append(Integer.toHexString(curIndex));
                        stringBuilder2.append(" curIndex=");
                        stringBuilder2.append(curIndex2);
                        stringBuilder2.append(" endIndex=");
                        stringBuilder2.append(endIndex);
                        throw new ResultException(resultCode2, stringBuilder2.toString());
                    }
                } catch (IndexOutOfBoundsException e4) {
                    resultCode = ResultCode.REQUIRED_VALUES_MISSING;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("IndexOutOfBoundsException  curIndex=");
                    stringBuilder.append(curIndex2);
                    stringBuilder.append(" endIndex=");
                    stringBuilder.append(endIndex);
                    throw new ResultException(resultCode, stringBuilder.toString());
                } catch (ResultException e5) {
                    e = e5;
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, e.explanation());
                }
            } else if (ComprehensionTlvTag.COMMAND_DETAILS.value() == (tag & -129)) {
                tag = 0;
                curIndex = 0;
            }
            if (endIndex - curIndex >= length) {
                List<ComprehensionTlv> ctlvs = ComprehensionTlv.decodeMany(data, curIndex);
                if (tag == BER_PROACTIVE_COMMAND_TAG) {
                    int totalLength = 0;
                    for (ComprehensionTlv item : ctlvs) {
                        int i;
                        int itemLength = item.getLength();
                        if (itemLength >= 128 && itemLength <= 255) {
                            i = itemLength + 3;
                        } else if (itemLength < 0 || itemLength >= 128) {
                            isLengthValid = false;
                            break;
                        } else {
                            i = itemLength + 2;
                        }
                        totalLength += i;
                    }
                    if (length != totalLength) {
                        if (HwTelephonyFactory.getHwUiccManager().isContainZeros(data, length, totalLength, curIndex)) {
                            isLengthValid = true;
                        } else {
                            isLengthValid = false;
                        }
                    }
                }
                return new BerTlv(tag, ctlvs, isLengthValid);
            }
            resultCode2 = ResultCode.CMD_DATA_NOT_UNDERSTOOD;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Command had extra data endIndex=");
            stringBuilder2.append(endIndex);
            stringBuilder2.append(" curIndex=");
            stringBuilder2.append(curIndex);
            stringBuilder2.append(" length=");
            stringBuilder2.append(length);
            throw new ResultException(resultCode2, stringBuilder2.toString());
        } catch (IndexOutOfBoundsException e6) {
            curIndex2 = curIndex;
            resultCode = ResultCode.REQUIRED_VALUES_MISSING;
            stringBuilder = new StringBuilder();
            stringBuilder.append("IndexOutOfBoundsException  curIndex=");
            stringBuilder.append(curIndex2);
            stringBuilder.append(" endIndex=");
            stringBuilder.append(endIndex);
            throw new ResultException(resultCode, stringBuilder.toString());
        } catch (ResultException e7) {
            e = e7;
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, e.explanation());
        }
    }
}
