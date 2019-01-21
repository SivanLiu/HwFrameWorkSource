package android.bluetooth.le;

import android.bluetooth.BluetoothUuid;
import android.os.ParcelUuid;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class ScanRecord {
    private static final int DATA_TYPE_FLAGS = 1;
    private static final int DATA_TYPE_LOCAL_NAME_COMPLETE = 9;
    private static final int DATA_TYPE_LOCAL_NAME_SHORT = 8;
    private static final int DATA_TYPE_MANUFACTURER_SPECIFIC_DATA = 255;
    private static final int DATA_TYPE_SERVICE_DATA_128_BIT = 33;
    private static final int DATA_TYPE_SERVICE_DATA_16_BIT = 22;
    private static final int DATA_TYPE_SERVICE_DATA_32_BIT = 32;
    private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE = 7;
    private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL = 6;
    private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE = 3;
    private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL = 2;
    private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE = 5;
    private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL = 4;
    private static final int DATA_TYPE_TX_POWER_LEVEL = 10;
    private static final String TAG = "ScanRecord";
    private final int mAdvertiseFlags;
    private final byte[] mBytes;
    private final String mDeviceName;
    private final SparseArray<byte[]> mManufacturerSpecificData;
    private final Map<ParcelUuid, byte[]> mServiceData;
    private final List<ParcelUuid> mServiceUuids;
    private final int mTxPowerLevel;

    public int getAdvertiseFlags() {
        return this.mAdvertiseFlags;
    }

    public List<ParcelUuid> getServiceUuids() {
        return this.mServiceUuids;
    }

    public SparseArray<byte[]> getManufacturerSpecificData() {
        return this.mManufacturerSpecificData;
    }

    public byte[] getManufacturerSpecificData(int manufacturerId) {
        if (this.mManufacturerSpecificData == null) {
            return null;
        }
        return (byte[]) this.mManufacturerSpecificData.get(manufacturerId);
    }

    public Map<ParcelUuid, byte[]> getServiceData() {
        return this.mServiceData;
    }

    public byte[] getServiceData(ParcelUuid serviceDataUuid) {
        if (serviceDataUuid == null || this.mServiceData == null) {
            return null;
        }
        return (byte[]) this.mServiceData.get(serviceDataUuid);
    }

    public int getTxPowerLevel() {
        return this.mTxPowerLevel;
    }

    public String getDeviceName() {
        return this.mDeviceName;
    }

    public byte[] getBytes() {
        return this.mBytes;
    }

    private ScanRecord(List<ParcelUuid> serviceUuids, SparseArray<byte[]> manufacturerData, Map<ParcelUuid, byte[]> serviceData, int advertiseFlags, int txPowerLevel, String localName, byte[] bytes) {
        this.mServiceUuids = serviceUuids;
        this.mManufacturerSpecificData = manufacturerData;
        this.mServiceData = serviceData;
        this.mDeviceName = localName;
        this.mAdvertiseFlags = advertiseFlags;
        this.mTxPowerLevel = txPowerLevel;
        this.mBytes = bytes;
    }

    public static ScanRecord parseFromBytes(byte[] scanRecord) {
        List<ParcelUuid> list;
        String str;
        StringBuilder stringBuilder;
        byte[] bArr = scanRecord;
        if (bArr == null) {
            return null;
        }
        Map<ParcelUuid, byte[]> serviceData;
        List<ParcelUuid> serviceUuids = new ArrayList();
        SparseArray<byte[]> manufacturerData = new SparseArray();
        Map<ParcelUuid, byte[]> serviceData2 = new ArrayMap();
        int advertiseFlag = -1;
        String localName = null;
        int txPowerLevel = Integer.MIN_VALUE;
        int currentPos = 0;
        while (true) {
            serviceData = serviceData2;
            int i;
            try {
                if (currentPos < bArr.length) {
                    int currentPos2 = currentPos + 1;
                    try {
                        int length = bArr[currentPos] & 255;
                        if (length == 0) {
                            i = currentPos2;
                        } else {
                            int dataLength = length - 1;
                            int currentPos3 = currentPos2 + 1;
                            try {
                                currentPos2 = bArr[currentPos2] & 255;
                                if (currentPos2 != 22) {
                                    if (currentPos2 != 255) {
                                        switch (currentPos2) {
                                            case 1:
                                                advertiseFlag = bArr[currentPos3] & 255;
                                                continue;
                                            case 2:
                                            case 3:
                                                parseServiceUuid(bArr, currentPos3, dataLength, 2, serviceUuids);
                                                continue;
                                            case 4:
                                            case 5:
                                                parseServiceUuid(bArr, currentPos3, dataLength, 4, serviceUuids);
                                                continue;
                                            case 6:
                                            case 7:
                                                parseServiceUuid(bArr, currentPos3, dataLength, 16, serviceUuids);
                                                continue;
                                            case 8:
                                            case 9:
                                                localName = new String(extractBytes(bArr, currentPos3, dataLength));
                                                continue;
                                            case 10:
                                                txPowerLevel = bArr[currentPos3];
                                                continue;
                                            default:
                                                switch (currentPos2) {
                                                    case 32:
                                                    case 33:
                                                        break;
                                                    default:
                                                        continue;
                                                        continue;
                                                }
                                        }
                                    } else {
                                        manufacturerData.put(((bArr[currentPos3 + 1] & 255) << 8) + (255 & bArr[currentPos3]), extractBytes(bArr, currentPos3 + 2, dataLength - 2));
                                    }
                                    currentPos = currentPos3 + dataLength;
                                    serviceData2 = serviceData;
                                }
                                currentPos = 2;
                                if (currentPos2 == 32) {
                                    currentPos = 4;
                                } else if (currentPos2 == 33) {
                                    currentPos = 16;
                                }
                                serviceData.put(BluetoothUuid.parseUuidFrom(extractBytes(bArr, currentPos3, currentPos)), extractBytes(bArr, currentPos3 + currentPos, dataLength - currentPos));
                                currentPos = currentPos3 + dataLength;
                                serviceData2 = serviceData;
                            } catch (Exception e) {
                                list = serviceUuids;
                                i = currentPos3;
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("unable to parse scan record: ");
                                stringBuilder.append(Arrays.toString(scanRecord));
                                Log.e(str, stringBuilder.toString());
                                return new ScanRecord(null, null, null, -1, Integer.MIN_VALUE, null, bArr);
                            }
                        }
                    } catch (Exception e2) {
                        list = serviceUuids;
                        i = currentPos2;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("unable to parse scan record: ");
                        stringBuilder.append(Arrays.toString(scanRecord));
                        Log.e(str, stringBuilder.toString());
                        return new ScanRecord(null, null, null, -1, Integer.MIN_VALUE, null, bArr);
                    }
                }
            } catch (Exception e3) {
                i = currentPos;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("unable to parse scan record: ");
                stringBuilder.append(Arrays.toString(scanRecord));
                Log.e(str, stringBuilder.toString());
                return new ScanRecord(null, null, null, -1, Integer.MIN_VALUE, null, bArr);
            }
        }
        try {
            try {
                return new ScanRecord(serviceUuids.isEmpty() ? null : serviceUuids, manufacturerData, serviceData, advertiseFlag, txPowerLevel, localName, bArr);
            } catch (Exception e4) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("unable to parse scan record: ");
                stringBuilder.append(Arrays.toString(scanRecord));
                Log.e(str, stringBuilder.toString());
                return new ScanRecord(null, null, null, -1, Integer.MIN_VALUE, null, bArr);
            }
        } catch (Exception e5) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("unable to parse scan record: ");
            stringBuilder.append(Arrays.toString(scanRecord));
            Log.e(str, stringBuilder.toString());
            return new ScanRecord(null, null, null, -1, Integer.MIN_VALUE, null, bArr);
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ScanRecord [mAdvertiseFlags=");
        stringBuilder.append(this.mAdvertiseFlags);
        stringBuilder.append(", mServiceUuids=");
        stringBuilder.append(this.mServiceUuids);
        stringBuilder.append(", mManufacturerSpecificData=");
        stringBuilder.append(BluetoothLeUtils.toString(this.mManufacturerSpecificData));
        stringBuilder.append(", mServiceData=");
        stringBuilder.append(BluetoothLeUtils.toString(this.mServiceData));
        stringBuilder.append(", mTxPowerLevel=");
        stringBuilder.append(this.mTxPowerLevel);
        stringBuilder.append(", mDeviceName=");
        stringBuilder.append(this.mDeviceName);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    private static int parseServiceUuid(byte[] scanRecord, int currentPos, int dataLength, int uuidLength, List<ParcelUuid> serviceUuids) {
        while (dataLength > 0) {
            serviceUuids.add(BluetoothUuid.parseUuidFrom(extractBytes(scanRecord, currentPos, uuidLength)));
            dataLength -= uuidLength;
            currentPos += uuidLength;
        }
        return currentPos;
    }

    private static byte[] extractBytes(byte[] scanRecord, int start, int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(scanRecord, start, bytes, 0, length);
        return bytes;
    }
}
