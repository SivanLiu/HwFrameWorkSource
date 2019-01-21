package android.hardware.radio.V1_2;

import android.hardware.radio.V1_0.CellInfoType;
import android.hardware.radio.V1_0.TimeStampType;
import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import com.android.internal.telephony.AbstractPhoneBase;
import java.util.ArrayList;
import java.util.Objects;

public final class CellInfo {
    public final ArrayList<CellInfoCdma> cdma = new ArrayList();
    public int cellInfoType;
    public int connectionStatus;
    public final ArrayList<CellInfoGsm> gsm = new ArrayList();
    public final ArrayList<CellInfoLte> lte = new ArrayList();
    public boolean registered;
    public final ArrayList<CellInfoTdscdma> tdscdma = new ArrayList();
    public long timeStamp;
    public int timeStampType;
    public final ArrayList<CellInfoWcdma> wcdma = new ArrayList();

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != CellInfo.class) {
            return false;
        }
        CellInfo other = (CellInfo) otherObject;
        if (this.cellInfoType == other.cellInfoType && this.registered == other.registered && this.timeStampType == other.timeStampType && this.timeStamp == other.timeStamp && HidlSupport.deepEquals(this.gsm, other.gsm) && HidlSupport.deepEquals(this.cdma, other.cdma) && HidlSupport.deepEquals(this.lte, other.lte) && HidlSupport.deepEquals(this.wcdma, other.wcdma) && HidlSupport.deepEquals(this.tdscdma, other.tdscdma) && this.connectionStatus == other.connectionStatus) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.cellInfoType))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.registered))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.timeStampType))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.timeStamp))), Integer.valueOf(HidlSupport.deepHashCode(this.gsm)), Integer.valueOf(HidlSupport.deepHashCode(this.cdma)), Integer.valueOf(HidlSupport.deepHashCode(this.lte)), Integer.valueOf(HidlSupport.deepHashCode(this.wcdma)), Integer.valueOf(HidlSupport.deepHashCode(this.tdscdma)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.connectionStatus)))});
    }

    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".cellInfoType = ");
        builder.append(CellInfoType.toString(this.cellInfoType));
        builder.append(", .registered = ");
        builder.append(this.registered);
        builder.append(", .timeStampType = ");
        builder.append(TimeStampType.toString(this.timeStampType));
        builder.append(", .timeStamp = ");
        builder.append(this.timeStamp);
        builder.append(", .gsm = ");
        builder.append(this.gsm);
        builder.append(", .cdma = ");
        builder.append(this.cdma);
        builder.append(", .lte = ");
        builder.append(this.lte);
        builder.append(", .wcdma = ");
        builder.append(this.wcdma);
        builder.append(", .tdscdma = ");
        builder.append(this.tdscdma);
        builder.append(", .connectionStatus = ");
        builder.append(CellConnectionStatus.toString(this.connectionStatus));
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(112), 0);
    }

    public static final ArrayList<CellInfo> readVectorFromParcel(HwParcel parcel) {
        ArrayList<CellInfo> _hidl_vec = new ArrayList();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 112), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            CellInfo _hidl_vec_element = new CellInfo();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 112));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_index_0;
        HwParcel hwParcel = parcel;
        HwBlob hwBlob = _hidl_blob;
        this.cellInfoType = hwBlob.getInt32(_hidl_offset + 0);
        this.registered = hwBlob.getBool(_hidl_offset + 4);
        this.timeStampType = hwBlob.getInt32(_hidl_offset + 8);
        this.timeStamp = hwBlob.getInt64(_hidl_offset + 16);
        int _hidl_vec_size = hwBlob.getInt32((_hidl_offset + 24) + 8);
        int _hidl_vec_size2 = _hidl_vec_size;
        HwBlob childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size * 96), _hidl_blob.handle(), (_hidl_offset + 24) + 0, 1);
        this.gsm.clear();
        int _hidl_index_02 = 0;
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size2; _hidl_index_0++) {
            CellInfoGsm _hidl_vec_element = new CellInfoGsm();
            _hidl_vec_element.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 96));
            this.gsm.add(_hidl_vec_element);
        }
        _hidl_vec_size2 = hwBlob.getInt32((_hidl_offset + 40) + 8);
        childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * 80), _hidl_blob.handle(), (_hidl_offset + 40) + 0, true);
        this.cdma.clear();
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size2; _hidl_index_0++) {
            CellInfoCdma _hidl_vec_element2 = new CellInfoCdma();
            _hidl_vec_element2.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 80));
            this.cdma.add(_hidl_vec_element2);
        }
        _hidl_vec_size2 = hwBlob.getInt32((_hidl_offset + 56) + 8);
        childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * 112), _hidl_blob.handle(), (_hidl_offset + 56) + 0, true);
        this.lte.clear();
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size2; _hidl_index_0++) {
            CellInfoLte _hidl_vec_element3 = new CellInfoLte();
            _hidl_vec_element3.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 112));
            this.lte.add(_hidl_vec_element3);
        }
        _hidl_vec_size2 = hwBlob.getInt32((_hidl_offset + 72) + 8);
        childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * 96), _hidl_blob.handle(), (_hidl_offset + 72) + 0, true);
        this.wcdma.clear();
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size2; _hidl_index_0++) {
            CellInfoWcdma _hidl_vec_element4 = new CellInfoWcdma();
            _hidl_vec_element4.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 96));
            this.wcdma.add(_hidl_vec_element4);
        }
        _hidl_vec_size2 = hwBlob.getInt32((_hidl_offset + 88) + 8);
        childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * AbstractPhoneBase.EVENT_ECC_NUM), _hidl_blob.handle(), 0 + (_hidl_offset + 88), true);
        this.tdscdma.clear();
        while (true) {
            _hidl_index_0 = _hidl_index_02;
            if (_hidl_index_0 < _hidl_vec_size2) {
                CellInfoTdscdma _hidl_vec_element5 = new CellInfoTdscdma();
                _hidl_vec_element5.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * AbstractPhoneBase.EVENT_ECC_NUM));
                this.tdscdma.add(_hidl_vec_element5);
                _hidl_index_02 = _hidl_index_0 + 1;
            } else {
                this.connectionStatus = hwBlob.getInt32(_hidl_offset + 104);
                return;
            }
        }
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(112);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<CellInfo> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        int _hidl_index_0 = 0;
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 112);
        while (_hidl_index_0 < _hidl_vec_size) {
            ((CellInfo) _hidl_vec.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 112));
            _hidl_index_0++;
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_index_0;
        HwBlob hwBlob = _hidl_blob;
        hwBlob.putInt32(_hidl_offset + 0, this.cellInfoType);
        hwBlob.putBool(_hidl_offset + 4, this.registered);
        hwBlob.putInt32(_hidl_offset + 8, this.timeStampType);
        hwBlob.putInt64(_hidl_offset + 16, this.timeStamp);
        int _hidl_vec_size = this.gsm.size();
        hwBlob.putInt32((_hidl_offset + 24) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 24) + 12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 96);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((CellInfoGsm) this.gsm.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 96));
        }
        hwBlob.putBlob((_hidl_offset + 24) + 0, childBlob);
        _hidl_vec_size = this.cdma.size();
        hwBlob.putInt32((_hidl_offset + 40) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 40) + 12, false);
        childBlob = new HwBlob(_hidl_vec_size * 80);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((CellInfoCdma) this.cdma.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 80));
        }
        hwBlob.putBlob((_hidl_offset + 40) + 0, childBlob);
        _hidl_vec_size = this.lte.size();
        hwBlob.putInt32((_hidl_offset + 56) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 56) + 12, false);
        childBlob = new HwBlob(_hidl_vec_size * 112);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((CellInfoLte) this.lte.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 112));
        }
        hwBlob.putBlob((_hidl_offset + 56) + 0, childBlob);
        _hidl_vec_size = this.wcdma.size();
        hwBlob.putInt32((_hidl_offset + 72) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 72) + 12, false);
        childBlob = new HwBlob(_hidl_vec_size * 96);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((CellInfoWcdma) this.wcdma.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 96));
        }
        hwBlob.putBlob((_hidl_offset + 72) + 0, childBlob);
        _hidl_vec_size = this.tdscdma.size();
        hwBlob.putInt32((_hidl_offset + 88) + 8, _hidl_vec_size);
        int _hidl_index_02 = 0;
        hwBlob.putBool((_hidl_offset + 88) + 12, false);
        HwBlob childBlob2 = new HwBlob(_hidl_vec_size * AbstractPhoneBase.EVENT_ECC_NUM);
        while (true) {
            int _hidl_index_03 = _hidl_index_02;
            if (_hidl_index_03 < _hidl_vec_size) {
                ((CellInfoTdscdma) this.tdscdma.get(_hidl_index_03)).writeEmbeddedToBlob(childBlob2, (long) (_hidl_index_03 * AbstractPhoneBase.EVENT_ECC_NUM));
                _hidl_index_02 = _hidl_index_03 + 1;
            } else {
                hwBlob.putBlob((_hidl_offset + 88) + 0, childBlob2);
                hwBlob.putInt32(_hidl_offset + 104, this.connectionStatus);
                return;
            }
        }
    }
}
