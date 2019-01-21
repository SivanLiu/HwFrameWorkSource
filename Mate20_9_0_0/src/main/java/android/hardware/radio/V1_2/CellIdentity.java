package android.hardware.radio.V1_2;

import android.hardware.radio.V1_0.CellInfoType;
import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class CellIdentity {
    public final ArrayList<CellIdentityCdma> cellIdentityCdma = new ArrayList();
    public final ArrayList<CellIdentityGsm> cellIdentityGsm = new ArrayList();
    public final ArrayList<CellIdentityLte> cellIdentityLte = new ArrayList();
    public final ArrayList<CellIdentityTdscdma> cellIdentityTdscdma = new ArrayList();
    public final ArrayList<CellIdentityWcdma> cellIdentityWcdma = new ArrayList();
    public int cellInfoType;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != CellIdentity.class) {
            return false;
        }
        CellIdentity other = (CellIdentity) otherObject;
        if (this.cellInfoType == other.cellInfoType && HidlSupport.deepEquals(this.cellIdentityGsm, other.cellIdentityGsm) && HidlSupport.deepEquals(this.cellIdentityWcdma, other.cellIdentityWcdma) && HidlSupport.deepEquals(this.cellIdentityCdma, other.cellIdentityCdma) && HidlSupport.deepEquals(this.cellIdentityLte, other.cellIdentityLte) && HidlSupport.deepEquals(this.cellIdentityTdscdma, other.cellIdentityTdscdma)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.cellInfoType))), Integer.valueOf(HidlSupport.deepHashCode(this.cellIdentityGsm)), Integer.valueOf(HidlSupport.deepHashCode(this.cellIdentityWcdma)), Integer.valueOf(HidlSupport.deepHashCode(this.cellIdentityCdma)), Integer.valueOf(HidlSupport.deepHashCode(this.cellIdentityLte)), Integer.valueOf(HidlSupport.deepHashCode(this.cellIdentityTdscdma))});
    }

    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".cellInfoType = ");
        builder.append(CellInfoType.toString(this.cellInfoType));
        builder.append(", .cellIdentityGsm = ");
        builder.append(this.cellIdentityGsm);
        builder.append(", .cellIdentityWcdma = ");
        builder.append(this.cellIdentityWcdma);
        builder.append(", .cellIdentityCdma = ");
        builder.append(this.cellIdentityCdma);
        builder.append(", .cellIdentityLte = ");
        builder.append(this.cellIdentityLte);
        builder.append(", .cellIdentityTdscdma = ");
        builder.append(this.cellIdentityTdscdma);
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(88), 0);
    }

    public static final ArrayList<CellIdentity> readVectorFromParcel(HwParcel parcel) {
        ArrayList<CellIdentity> _hidl_vec = new ArrayList();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 88), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            CellIdentity _hidl_vec_element = new CellIdentity();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 88));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_index_0;
        HwParcel hwParcel = parcel;
        HwBlob hwBlob = _hidl_blob;
        this.cellInfoType = hwBlob.getInt32(_hidl_offset + 0);
        int _hidl_vec_size = hwBlob.getInt32((_hidl_offset + 8) + 8);
        int _hidl_vec_size2 = _hidl_vec_size;
        HwBlob childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size * 80), _hidl_blob.handle(), (_hidl_offset + 8) + 0, 1);
        this.cellIdentityGsm.clear();
        int _hidl_index_02 = 0;
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size2; _hidl_index_0++) {
            CellIdentityGsm _hidl_vec_element = new CellIdentityGsm();
            _hidl_vec_element.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 80));
            this.cellIdentityGsm.add(_hidl_vec_element);
        }
        _hidl_vec_size2 = hwBlob.getInt32((_hidl_offset + 24) + 8);
        childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * 80), _hidl_blob.handle(), (_hidl_offset + 24) + 0, true);
        this.cellIdentityWcdma.clear();
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size2; _hidl_index_0++) {
            CellIdentityWcdma _hidl_vec_element2 = new CellIdentityWcdma();
            _hidl_vec_element2.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 80));
            this.cellIdentityWcdma.add(_hidl_vec_element2);
        }
        _hidl_vec_size2 = hwBlob.getInt32((_hidl_offset + 40) + 8);
        childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * 56), _hidl_blob.handle(), (_hidl_offset + 40) + 0, true);
        this.cellIdentityCdma.clear();
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size2; _hidl_index_0++) {
            CellIdentityCdma _hidl_vec_element3 = new CellIdentityCdma();
            _hidl_vec_element3.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 56));
            this.cellIdentityCdma.add(_hidl_vec_element3);
        }
        _hidl_vec_size2 = hwBlob.getInt32((_hidl_offset + 56) + 8);
        childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * 88), _hidl_blob.handle(), (_hidl_offset + 56) + 0, true);
        this.cellIdentityLte.clear();
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size2; _hidl_index_0++) {
            CellIdentityLte _hidl_vec_element4 = new CellIdentityLte();
            _hidl_vec_element4.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 88));
            this.cellIdentityLte.add(_hidl_vec_element4);
        }
        _hidl_vec_size2 = hwBlob.getInt32((_hidl_offset + 72) + 8);
        childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * 88), _hidl_blob.handle(), 0 + (_hidl_offset + 72), true);
        this.cellIdentityTdscdma.clear();
        while (true) {
            _hidl_index_0 = _hidl_index_02;
            if (_hidl_index_0 < _hidl_vec_size2) {
                CellIdentityTdscdma _hidl_vec_element5 = new CellIdentityTdscdma();
                _hidl_vec_element5.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 88));
                this.cellIdentityTdscdma.add(_hidl_vec_element5);
                _hidl_index_02 = _hidl_index_0 + 1;
            } else {
                return;
            }
        }
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(88);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<CellIdentity> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        int _hidl_index_0 = 0;
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 88);
        while (_hidl_index_0 < _hidl_vec_size) {
            ((CellIdentity) _hidl_vec.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 88));
            _hidl_index_0++;
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_index_0;
        HwBlob hwBlob = _hidl_blob;
        hwBlob.putInt32(_hidl_offset + 0, this.cellInfoType);
        int _hidl_vec_size = this.cellIdentityGsm.size();
        hwBlob.putInt32((_hidl_offset + 8) + 8, _hidl_vec_size);
        int _hidl_index_02 = 0;
        hwBlob.putBool((_hidl_offset + 8) + 12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 80);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((CellIdentityGsm) this.cellIdentityGsm.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 80));
        }
        hwBlob.putBlob((_hidl_offset + 8) + 0, childBlob);
        _hidl_vec_size = this.cellIdentityWcdma.size();
        hwBlob.putInt32((_hidl_offset + 24) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 24) + 12, false);
        HwBlob childBlob2 = new HwBlob(_hidl_vec_size * 80);
        for (int _hidl_index_03 = 0; _hidl_index_03 < _hidl_vec_size; _hidl_index_03++) {
            ((CellIdentityWcdma) this.cellIdentityWcdma.get(_hidl_index_03)).writeEmbeddedToBlob(childBlob2, (long) (_hidl_index_03 * 80));
        }
        hwBlob.putBlob((_hidl_offset + 24) + 0, childBlob2);
        _hidl_vec_size = this.cellIdentityCdma.size();
        hwBlob.putInt32((_hidl_offset + 40) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 40) + 12, false);
        childBlob = new HwBlob(_hidl_vec_size * 56);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((CellIdentityCdma) this.cellIdentityCdma.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 56));
        }
        hwBlob.putBlob((_hidl_offset + 40) + 0, childBlob);
        _hidl_vec_size = this.cellIdentityLte.size();
        hwBlob.putInt32((_hidl_offset + 56) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 56) + 12, false);
        childBlob = new HwBlob(_hidl_vec_size * 88);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((CellIdentityLte) this.cellIdentityLte.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 88));
        }
        hwBlob.putBlob((_hidl_offset + 56) + 0, childBlob);
        _hidl_vec_size = this.cellIdentityTdscdma.size();
        hwBlob.putInt32((_hidl_offset + 72) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 72) + 12, false);
        childBlob = new HwBlob(_hidl_vec_size * 88);
        while (true) {
            _hidl_index_0 = _hidl_index_02;
            if (_hidl_index_0 < _hidl_vec_size) {
                ((CellIdentityTdscdma) this.cellIdentityTdscdma.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 88));
                _hidl_index_02 = _hidl_index_0 + 1;
            } else {
                hwBlob.putBlob((_hidl_offset + 72) + 0, childBlob);
                return;
            }
        }
    }
}
