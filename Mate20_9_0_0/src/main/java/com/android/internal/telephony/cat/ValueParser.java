package com.android.internal.telephony.cat;

import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.cat.BearerDescription.BearerType;
import com.android.internal.telephony.cat.Duration.TimeUnit;
import com.android.internal.telephony.cat.InterfaceTransportLevel.TransportProtocol;
import com.android.internal.telephony.uicc.IccUtils;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

abstract class ValueParser {
    ValueParser() {
    }

    static CommandDetails retrieveCommandDetails(ComprehensionTlv ctlv) throws ResultException {
        CommandDetails cmdDet = new CommandDetails();
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            cmdDet.compRequired = ctlv.isComprehensionRequired();
            cmdDet.commandNumber = rawValue[valueIndex] & 255;
            cmdDet.typeOfCommand = rawValue[valueIndex + 1] & 255;
            cmdDet.commandQualifier = rawValue[valueIndex + 2] & 255;
            return cmdDet;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static DeviceIdentities retrieveDeviceIdentities(ComprehensionTlv ctlv) throws ResultException {
        DeviceIdentities devIds = new DeviceIdentities();
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            devIds.sourceId = rawValue[valueIndex] & 255;
            devIds.destinationId = rawValue[valueIndex + 1] & 255;
            return devIds;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
    }

    static Duration retrieveDuration(ComprehensionTlv ctlv) throws ResultException {
        TimeUnit timeUnit = TimeUnit.SECOND;
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            return new Duration(rawValue[valueIndex + 1] & 255, TimeUnit.values()[rawValue[valueIndex] & 255]);
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static Item retrieveItem(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();
        if (length == 0) {
            return null;
        }
        try {
            return new Item(rawValue[valueIndex] & 255, HwTelephonyFactory.getHwUiccManager().adnStringFieldToStringForSTK(rawValue, valueIndex + 1, length - 1));
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static int retrieveItemId(ComprehensionTlv ctlv) throws ResultException {
        try {
            return ctlv.getRawValue()[ctlv.getValueIndex()] & 255;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static IconId retrieveIconId(ComprehensionTlv ctlv) throws ResultException {
        IconId id = new IconId();
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int valueIndex2 = valueIndex + 1;
        try {
            id.selfExplanatory = (rawValue[valueIndex] & 255) == 0;
            id.recordNumber = rawValue[valueIndex2] & 255;
            return id;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static ItemsIconId retrieveItemsIconId(ComprehensionTlv ctlv) throws ResultException {
        CatLog.d("ValueParser", "retrieveItemsIconId:");
        ItemsIconId id = new ItemsIconId();
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        boolean z = true;
        int numOfItems = ctlv.getLength() - 1;
        id.recordNumbers = new int[numOfItems];
        int valueIndex2 = valueIndex + 1;
        try {
            int index = 0;
            if ((rawValue[valueIndex] & 255) != 0) {
                z = false;
            }
            id.selfExplanatory = z;
            while (true) {
                valueIndex = index;
                if (valueIndex >= numOfItems) {
                    return id;
                }
                index = valueIndex + 1;
                int valueIndex3 = valueIndex2 + 1;
                try {
                    id.recordNumbers[valueIndex] = rawValue[valueIndex2];
                    valueIndex2 = valueIndex3;
                } catch (IndexOutOfBoundsException e) {
                    valueIndex2 = valueIndex3;
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
            }
        } catch (IndexOutOfBoundsException e2) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static List<TextAttribute> retrieveTextAttribute(ComprehensionTlv ctlv) throws ResultException {
        ArrayList<TextAttribute> lst = new ArrayList();
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();
        if (length == 0) {
            return null;
        }
        int itemCount = length / 4;
        int valueIndex2 = valueIndex;
        valueIndex = 0;
        while (valueIndex < itemCount) {
            try {
                int start = rawValue[valueIndex2] & 255;
                int textLength = rawValue[valueIndex2 + 1] & 255;
                int format = rawValue[valueIndex2 + 2] & 255;
                int colorValue = rawValue[valueIndex2 + 3] & 255;
                int alignValue = format & 3;
                TextAlignment align = TextAlignment.fromInt(alignValue);
                int sizeValue = (format >> 2) & 3;
                FontSize size = FontSize.fromInt(sizeValue);
                if (size == null) {
                    size = FontSize.NORMAL;
                }
                FontSize size2 = size;
                boolean strikeThrough = true;
                boolean bold = (format & 16) != 0;
                boolean italic = (format & 32) != 0;
                boolean underlined = (format & 64) != 0;
                if ((format & 128) == 0) {
                    strikeThrough = false;
                }
                lst.add(new TextAttribute(start, textLength, align, size2, bold, italic, underlined, strikeThrough, TextColor.fromInt(colorValue)));
                valueIndex++;
                valueIndex2 += 4;
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        return lst;
    }

    static String retrieveAlphaId(ComprehensionTlv ctlv) throws ResultException {
        String str = null;
        if (ctlv != null) {
            byte[] rawValue = ctlv.getRawValue();
            int valueIndex = ctlv.getValueIndex();
            int length = ctlv.getLength();
            if (length != 0) {
                try {
                    return HwTelephonyFactory.getHwUiccManager().adnStringFieldToStringForSTK(rawValue, valueIndex, length);
                } catch (IndexOutOfBoundsException e) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Alpha Id length=");
            stringBuilder.append(length);
            CatLog.d("ValueParser", stringBuilder.toString());
            return null;
        }
        boolean noAlphaUsrCnf;
        try {
            noAlphaUsrCnf = Resources.getSystem().getBoolean(17957031);
        } catch (NotFoundException e2) {
            noAlphaUsrCnf = false;
        }
        if (!noAlphaUsrCnf) {
            str = "Default Message";
        }
        return str;
    }

    static String retrieveTextString(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int textLen = ctlv.getLength();
        if (textLen == 0) {
            return null;
        }
        textLen--;
        try {
            String text;
            byte codingScheme = (byte) (rawValue[valueIndex] & 12);
            if (codingScheme != (byte) 0) {
                if (codingScheme != (byte) 12) {
                    if (codingScheme == (byte) 4) {
                        text = GsmAlphabet.gsm8BitUnpackedToString(rawValue, valueIndex + 1, textLen);
                    } else if (codingScheme == (byte) 8) {
                        text = new String(rawValue, valueIndex + 1, textLen, "UTF-16");
                    } else {
                        throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                    }
                    return text;
                }
            }
            text = GsmAlphabet.gsm7BitPackedToString(rawValue, valueIndex + 1, (textLen * 8) / 7);
            return text;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        } catch (UnsupportedEncodingException e2) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static int retrieveBufferSize(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            return ((rawValue[valueIndex] & 255) << 8) | (rawValue[valueIndex + 1] & 255);
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static InterfaceTransportLevel retrieveInterfaceTransportLevel(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            return new InterfaceTransportLevel(((rawValue[valueIndex + 1] & 255) << 8) | (rawValue[valueIndex + 2] & 255), TransportProtocol.values()[rawValue[valueIndex] & 255]);
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static int retrieveChannelDataLength(ComprehensionTlv ctlv) throws ResultException {
        try {
            return ctlv.getRawValue()[ctlv.getValueIndex()] & 255;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static byte[] retrieveChannelData(ComprehensionTlv ctlv) throws ResultException {
        byte[] data = new byte[ctlv.getLength()];
        System.arraycopy(ctlv.getRawValue(), ctlv.getValueIndex(), data, 0, data.length);
        return data;
    }

    static byte[] retrieveOtherAddress(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        if (ctlv.getLength() == 0) {
            return new byte[0];
        }
        try {
            int addrType = rawValue[valueIndex] & 255;
            if (addrType != 33 && addrType != 87) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            } else if ((addrType != 33 || ctlv.getLength() == 5) && (addrType != 87 || ctlv.getLength() == 17)) {
                byte[] addr = new byte[(ctlv.getLength() - 1)];
                System.arraycopy(rawValue, valueIndex + 1, addr, 0, addr.length);
                return addr;
            } else {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static String retrieveBIPAlphaId(ComprehensionTlv ctlv) throws ResultException {
        if (ctlv == null) {
            return null;
        }
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();
        if (length == 0) {
            return null;
        }
        try {
            return IccUtils.adnStringFieldToString(rawValue, valueIndex, length);
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static String retrieveNetworkAccessName(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        String networkAccessName = null;
        int len = ctlv.getLength() + valueIndex;
        while (valueIndex < len) {
            try {
                byte labelLen = rawValue[valueIndex];
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("labelLen:");
                stringBuilder.append(labelLen);
                CatLog.d("ValueParser", stringBuilder.toString());
                if (labelLen <= (byte) 0) {
                    return networkAccessName;
                }
                String label = GsmAlphabet.gsm8BitUnpackedToString(rawValue, valueIndex + 1, labelLen);
                valueIndex += labelLen + 1;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("valueIndex:");
                stringBuilder2.append(valueIndex);
                stringBuilder2.append("label:");
                stringBuilder2.append(label);
                stringBuilder2.append("labelLen:");
                stringBuilder2.append(labelLen);
                CatLog.d("ValueParser", stringBuilder2.toString());
                if (networkAccessName == null) {
                    networkAccessName = label;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(networkAccessName);
                    stringBuilder.append(".");
                    stringBuilder.append(label);
                    networkAccessName = stringBuilder.toString();
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("valueIndex:");
                stringBuilder2.append(valueIndex);
                stringBuilder2.append("label:");
                stringBuilder2.append(label);
                stringBuilder2.append("labelLen:");
                stringBuilder2.append(labelLen);
                stringBuilder2.append("networkAccessName:");
                stringBuilder2.append(networkAccessName);
                CatLog.d("ValueParser", stringBuilder2.toString());
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        return networkAccessName;
    }

    static BearerDescription retrieveBearerDescription(ComprehensionTlv ctlv) throws ResultException {
        BearerType type = null;
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            for (BearerType bt : BearerType.values()) {
                if (bt.value() == rawValue[valueIndex]) {
                    type = bt;
                    break;
                }
            }
            if (type != null) {
                byte[] parameters = new byte[(ctlv.getLength() - 1)];
                System.arraycopy(rawValue, valueIndex + 1, parameters, 0, parameters.length);
                return new BearerDescription(type, parameters);
            }
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }
}
