package com.huawei.nearbysdk.closeRange;

import android.os.ParcelUuid;
import android.util.SparseArray;
import com.huawei.nearbysdk.HwLog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public enum CloseRangeProtocol {
    CloseRangeBusinessId(1, 2, true),
    ReferenceRssi(2, 1, false),
    ModelId(3, 3, true),
    SubModelId(4, 1, false),
    DeviceId(5, 2, false),
    ConnectedDevice(6, 1, false),
    PairedDevice(7, 1, false),
    MaxConnectNumber(8, 1, false),
    MaxPairNumber(9, 1, false),
    DualModeDeviceKey(10, 2, false),
    TotalBattery(11, 1, false),
    LeftEarBattery(12, 1, false),
    RightEarBattery(13, 1, false),
    ChargerBattery(14, 1, false);
    
    private static final int BUSINESS_CLOSERANGE_BIT = 0;
    private static final int BUSINESS_RECONNECT_BIT = 1;
    private static final int INVALID_VALUE = -255;
    public static final ParcelUuid PARCEL_UUID_CLOSERANGE = null;
    public static final int RSSI_LENGTH = 1;
    private static final String STRING_UUID_NEARBY_CLOSERANGE = "0000FDEE-0000-1000-8000-00805F9B34FB";
    private static final String TAG = "CloseRangeProtocol";
    private static final UUID UUID_NEARBY_CLOSERANGE = null;
    private static final SparseArray<CloseRangeProtocol> lookupMap = null;
    private static final LinkedList<CloseRangeProtocol> mustList = null;
    private boolean isMust;
    private int type;
    private int valueLength;

    public static class BatteryStatus {
        private int batteryLevel;
        private boolean isCharge;

        public static BatteryStatus build(byte[] data) {
            if (data.length == 0) {
                return null;
            }
            BatteryStatus result = new BatteryStatus();
            result.isCharge = (data[0] & 128) != 0;
            result.batteryLevel = (data[0] & 127) & 255;
            return result;
        }

        public int getBatteryLevel() {
            return this.batteryLevel;
        }

        public boolean isCharge() {
            return this.isCharge;
        }
    }

    static {
        UUID_NEARBY_CLOSERANGE = UUID.fromString(STRING_UUID_NEARBY_CLOSERANGE);
        PARCEL_UUID_CLOSERANGE = new ParcelUuid(UUID_NEARBY_CLOSERANGE);
        lookupMap = new SparseArray();
        mustList = new LinkedList();
        CloseRangeProtocol[] values = values();
        int length = values.length;
        int i;
        while (i < length) {
            CloseRangeProtocol value = values[i];
            lookupMap.put(value.type, value);
            if (value.isMust) {
                mustList.add(value);
            }
            i++;
        }
    }

    private CloseRangeProtocol(int type, int valueLength, boolean isMust) {
        this.type = type;
        this.valueLength = valueLength;
        this.isMust = isMust;
    }

    public static SparseArray<byte[]> parseCloseRangeServiceData(byte[] serviceData) {
        if (serviceData == null) {
            return new SparseArray();
        }
        SparseArray<byte[]> resultMap = new SparseArray();
        int length = serviceData.length;
        int currentIndex = 0;
        while (currentIndex < length) {
            CloseRangeProtocol protocol = (CloseRangeProtocol) lookupMap.get(serviceData[currentIndex]);
            if (protocol == null) {
                break;
            }
            int startIndex = currentIndex + 1;
            int endIndex = protocol.getValueLength() + startIndex;
            if (endIndex > length) {
                return new SparseArray();
            }
            int type = protocol.getType();
            byte[] curData = Arrays.copyOfRange(serviceData, startIndex, endIndex);
            if (resultMap.get(type) == null) {
                resultMap.put(type, curData);
            } else if (protocol != DeviceId) {
                return new SparseArray();
            } else {
                byte[] orgData = (byte[]) resultMap.get(protocol.getType());
                byte[] fullData = new byte[(orgData.length + curData.length)];
                System.arraycopy(orgData, 0, fullData, 0, orgData.length);
                System.arraycopy(curData, 0, fullData, orgData.length, curData.length);
                resultMap.put(type, fullData);
            }
            currentIndex = endIndex;
        }
        Iterator it = mustList.iterator();
        while (it.hasNext()) {
            if (resultMap.get(((CloseRangeProtocol) it.next()).getType()) == null) {
                return new SparseArray();
            }
        }
        return resultMap;
    }

    public int getType() {
        return this.type;
    }

    public int getValueLength() {
        return this.valueLength;
    }

    public static boolean isCloseRangeEnabled(SparseArray<byte[]> nearbyArray) {
        byte[] data = getBusinessData(nearbyArray);
        boolean z = false;
        if (data == null) {
            return false;
        }
        if (CloseRangeBusinessType.fromTag(data[0]) != CloseRangeBusinessType.iConnect) {
            HwLog.i(TAG, "not iConnect, isCloseRangeEnabled return false");
            return false;
        }
        if (getBit(data[1], 0) > 0) {
            z = true;
        }
        return z;
    }

    private static boolean isInvalidData(int type, byte[] data) {
        return data == null || data.length != ((CloseRangeProtocol) lookupMap.get(type)).getValueLength();
    }

    private static byte[] getBusinessData(SparseArray<byte[]> nearbyArray) {
        if (nearbyArray == null) {
            HwLog.i(TAG, "empty array");
            return null;
        }
        int type = CloseRangeBusinessId.getType();
        byte[] data = (byte[]) nearbyArray.get(type);
        if (!isInvalidData(type, data)) {
            return data;
        }
        HwLog.i(TAG, "invalid business data");
        return null;
    }

    private static int getBit(byte byteData, int position) {
        return (byteData >> position) & 1;
    }

    public static boolean isReconnectEnabled(SparseArray<byte[]> nearbyArray) {
        byte[] data = getBusinessData(nearbyArray);
        boolean z = false;
        if (data == null) {
            return false;
        }
        if (CloseRangeBusinessType.fromTag(data[0]) != CloseRangeBusinessType.iConnect) {
            HwLog.i(TAG, "reconnect=false");
            return false;
        }
        if (getBit(data[1], 1) > 0) {
            z = true;
        }
        return z;
    }

    public static String getModelId(SparseArray<byte[]> nearbyArray) {
        if (nearbyArray == null) {
            HwLog.i(TAG, "empty array");
            return null;
        }
        int type = ModelId.getType();
        byte[] data = (byte[]) nearbyArray.get(type);
        if (!isInvalidData(type, data)) {
            return byteArrayToHexStr(data);
        }
        HwLog.i(TAG, "invalid model id");
        return null;
    }

    public static String getSubModelId(SparseArray<byte[]> nearbyArray) {
        if (nearbyArray == null) {
            HwLog.i(TAG, "empty array");
            return null;
        }
        int type = SubModelId.getType();
        byte[] data = (byte[]) nearbyArray.get(type);
        if (!isInvalidData(type, data)) {
            return byteArrayToHexStr(data);
        }
        HwLog.i(TAG, "invalid sub model id");
        return null;
    }

    private static String byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int length = byteArray.length;
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X", new Object[]{Byte.valueOf(byteArray[i])}));
        }
        return sb.toString();
    }

    public static int getReferenceRssi(SparseArray<byte[]> nearbyArray) {
        if (nearbyArray == null) {
            HwLog.i(TAG, "empty array");
            return INVALID_VALUE;
        }
        int type = ReferenceRssi.getType();
        byte[] data = (byte[]) nearbyArray.get(type);
        if (isInvalidData(type, data)) {
            return INVALID_VALUE;
        }
        return data[0];
    }

    public static String getDualModeDeviceKey(SparseArray<byte[]> nearbyArray) {
        if (nearbyArray == null) {
            HwLog.i(TAG, "empty array");
            return null;
        }
        int type = DualModeDeviceKey.getType();
        byte[] data = (byte[]) nearbyArray.get(type);
        if (!isInvalidData(type, data)) {
            return byteArrayToHexStr(data);
        }
        HwLog.i(TAG, "invalid dual model key");
        return null;
    }

    public static List<String> getDeviceId(SparseArray<byte[]> nearbyArray) {
        if (nearbyArray == null) {
            HwLog.i(TAG, "empty array");
            return null;
        }
        byte[] data = (byte[]) nearbyArray.get(DeviceId.getType());
        int length = DeviceId.getValueLength();
        if (data == null || data.length == 0 || data.length % length != 0) {
            HwLog.d(TAG, "invalid paired device id");
            return null;
        }
        String s = byteArrayToHexStr(data);
        if (s == null) {
            HwLog.d(TAG, "invalid paired device id");
            return null;
        }
        int strLength = length * 2;
        return getStrList(s, strLength, s.length() / strLength);
    }

    private static List<String> getStrList(String inputString, int length, int size) {
        List<String> list = new ArrayList();
        for (int index = 0; index < size; index++) {
            list.add(substring(inputString, index * length, (index + 1) * length));
        }
        return list;
    }

    private static String substring(String str, int f, int t) {
        if (f > str.length()) {
            return null;
        }
        if (t > str.length()) {
            return str.substring(f, str.length());
        }
        return str.substring(f, t);
    }

    public static int getConnectedDevice(SparseArray<byte[]> nearbyArray) {
        if (nearbyArray == null) {
            HwLog.i(TAG, "empty array");
            return INVALID_VALUE;
        }
        byte[] data = (byte[]) nearbyArray.get(ConnectedDevice.getType());
        if (data == null) {
            return INVALID_VALUE;
        }
        return toInt(data);
    }

    public static int getPairedDevice(SparseArray<byte[]> nearbyArray) {
        if (nearbyArray == null) {
            HwLog.i(TAG, "empty array");
            return INVALID_VALUE;
        }
        byte[] data = (byte[]) nearbyArray.get(PairedDevice.getType());
        if (data == null) {
            return INVALID_VALUE;
        }
        return toInt(data);
    }

    public static int getMaxConnectNumber(SparseArray<byte[]> nearbyArray) {
        if (nearbyArray == null) {
            HwLog.i(TAG, "empty array");
            return INVALID_VALUE;
        }
        byte[] data = (byte[]) nearbyArray.get(MaxConnectNumber.getType());
        if (data == null) {
            return INVALID_VALUE;
        }
        return toInt(data);
    }

    public static int getMaxPairNumber(SparseArray<byte[]> nearbyArray) {
        if (nearbyArray == null) {
            HwLog.i(TAG, "empty array");
            return INVALID_VALUE;
        }
        byte[] data = (byte[]) nearbyArray.get(MaxPairNumber.getType());
        if (data == null) {
            return INVALID_VALUE;
        }
        return toInt(data);
    }

    public static BatteryStatus getTotalBattery(SparseArray<byte[]> nearbyArray) {
        if (nearbyArray == null) {
            HwLog.i(TAG, "empty array");
            return null;
        }
        byte[] data = (byte[]) nearbyArray.get(TotalBattery.getType());
        if (data == null) {
            return null;
        }
        return BatteryStatus.build(data);
    }

    public static BatteryStatus getLeftBattery(SparseArray<byte[]> nearbyArray) {
        if (nearbyArray == null) {
            HwLog.i(TAG, "empty array");
            return null;
        }
        byte[] data = (byte[]) nearbyArray.get(LeftEarBattery.getType());
        if (data == null) {
            return null;
        }
        return BatteryStatus.build(data);
    }

    public static BatteryStatus getRightBattery(SparseArray<byte[]> nearbyArray) {
        if (nearbyArray == null) {
            HwLog.i(TAG, "empty array");
            return null;
        }
        byte[] data = (byte[]) nearbyArray.get(RightEarBattery.getType());
        if (data == null) {
            return null;
        }
        return BatteryStatus.build(data);
    }

    public static BatteryStatus getChargerBattery(SparseArray<byte[]> nearbyArray) {
        if (nearbyArray == null) {
            HwLog.i(TAG, "empty array");
            return null;
        }
        byte[] data = (byte[]) nearbyArray.get(ChargerBattery.getType());
        if (data == null) {
            return null;
        }
        return BatteryStatus.build(data);
    }

    private static int toInt(byte[] bRefArr) {
        int res = 0;
        for (int i = 0; i < bRefArr.length; i++) {
            res += (bRefArr[i] & 255) << (8 * i);
        }
        return res;
    }
}
