package android.hardware.radio.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class CdmaInformationRecord {
    public final ArrayList<CdmaT53AudioControlInfoRecord> audioCtrl = new ArrayList();
    public final ArrayList<CdmaT53ClirInfoRecord> clir = new ArrayList();
    public final ArrayList<CdmaDisplayInfoRecord> display = new ArrayList();
    public final ArrayList<CdmaLineControlInfoRecord> lineCtrl = new ArrayList();
    public int name;
    public final ArrayList<CdmaNumberInfoRecord> number = new ArrayList();
    public final ArrayList<CdmaRedirectingNumberInfoRecord> redir = new ArrayList();
    public final ArrayList<CdmaSignalInfoRecord> signal = new ArrayList();

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != CdmaInformationRecord.class) {
            return false;
        }
        CdmaInformationRecord other = (CdmaInformationRecord) otherObject;
        if (this.name == other.name && HidlSupport.deepEquals(this.display, other.display) && HidlSupport.deepEquals(this.number, other.number) && HidlSupport.deepEquals(this.signal, other.signal) && HidlSupport.deepEquals(this.redir, other.redir) && HidlSupport.deepEquals(this.lineCtrl, other.lineCtrl) && HidlSupport.deepEquals(this.clir, other.clir) && HidlSupport.deepEquals(this.audioCtrl, other.audioCtrl)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.name))), Integer.valueOf(HidlSupport.deepHashCode(this.display)), Integer.valueOf(HidlSupport.deepHashCode(this.number)), Integer.valueOf(HidlSupport.deepHashCode(this.signal)), Integer.valueOf(HidlSupport.deepHashCode(this.redir)), Integer.valueOf(HidlSupport.deepHashCode(this.lineCtrl)), Integer.valueOf(HidlSupport.deepHashCode(this.clir)), Integer.valueOf(HidlSupport.deepHashCode(this.audioCtrl))});
    }

    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".name = ");
        builder.append(CdmaInfoRecName.toString(this.name));
        builder.append(", .display = ");
        builder.append(this.display);
        builder.append(", .number = ");
        builder.append(this.number);
        builder.append(", .signal = ");
        builder.append(this.signal);
        builder.append(", .redir = ");
        builder.append(this.redir);
        builder.append(", .lineCtrl = ");
        builder.append(this.lineCtrl);
        builder.append(", .clir = ");
        builder.append(this.clir);
        builder.append(", .audioCtrl = ");
        builder.append(this.audioCtrl);
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(120), 0);
    }

    public static final ArrayList<CdmaInformationRecord> readVectorFromParcel(HwParcel parcel) {
        ArrayList<CdmaInformationRecord> _hidl_vec = new ArrayList();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 120), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            CdmaInformationRecord _hidl_vec_element = new CdmaInformationRecord();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 120));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_index_0;
        HwParcel hwParcel = parcel;
        HwBlob hwBlob = _hidl_blob;
        this.name = hwBlob.getInt32(_hidl_offset + 0);
        int _hidl_vec_size = hwBlob.getInt32((_hidl_offset + 8) + 8);
        int _hidl_vec_size2 = _hidl_vec_size;
        HwBlob childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size * 16), _hidl_blob.handle(), (_hidl_offset + 8) + 0, 1);
        this.display.clear();
        int _hidl_index_02 = 0;
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size2; _hidl_index_0++) {
            CdmaDisplayInfoRecord _hidl_vec_element = new CdmaDisplayInfoRecord();
            _hidl_vec_element.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 16));
            this.display.add(_hidl_vec_element);
        }
        _hidl_vec_size2 = hwBlob.getInt32((_hidl_offset + 24) + 8);
        childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * 24), _hidl_blob.handle(), (_hidl_offset + 24) + 0, true);
        this.number.clear();
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size2; _hidl_index_0++) {
            CdmaNumberInfoRecord _hidl_vec_element2 = new CdmaNumberInfoRecord();
            _hidl_vec_element2.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 24));
            this.number.add(_hidl_vec_element2);
        }
        _hidl_vec_size2 = hwBlob.getInt32((_hidl_offset + 40) + 8);
        childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * 4), _hidl_blob.handle(), (_hidl_offset + 40) + 0, true);
        this.signal.clear();
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size2; _hidl_index_0++) {
            CdmaSignalInfoRecord _hidl_vec_element3 = new CdmaSignalInfoRecord();
            _hidl_vec_element3.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 4));
            this.signal.add(_hidl_vec_element3);
        }
        _hidl_vec_size2 = hwBlob.getInt32((_hidl_offset + 56) + 8);
        childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * 32), _hidl_blob.handle(), (_hidl_offset + 56) + 0, true);
        this.redir.clear();
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size2; _hidl_index_0++) {
            CdmaRedirectingNumberInfoRecord _hidl_vec_element4 = new CdmaRedirectingNumberInfoRecord();
            _hidl_vec_element4.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 32));
            this.redir.add(_hidl_vec_element4);
        }
        _hidl_vec_size2 = hwBlob.getInt32((_hidl_offset + 72) + 8);
        childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * 4), _hidl_blob.handle(), (_hidl_offset + 72) + 0, true);
        this.lineCtrl.clear();
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size2; _hidl_index_0++) {
            CdmaLineControlInfoRecord _hidl_vec_element5 = new CdmaLineControlInfoRecord();
            _hidl_vec_element5.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 4));
            this.lineCtrl.add(_hidl_vec_element5);
        }
        _hidl_vec_size2 = hwBlob.getInt32((_hidl_offset + 88) + 8);
        childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * 1), _hidl_blob.handle(), (_hidl_offset + 88) + 0, true);
        this.clir.clear();
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size2; _hidl_index_0++) {
            CdmaT53ClirInfoRecord _hidl_vec_element6 = new CdmaT53ClirInfoRecord();
            _hidl_vec_element6.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 1));
            this.clir.add(_hidl_vec_element6);
        }
        _hidl_vec_size2 = hwBlob.getInt32((_hidl_offset + 104) + 8);
        childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * 2), _hidl_blob.handle(), 0 + (_hidl_offset + 104), true);
        this.audioCtrl.clear();
        while (true) {
            _hidl_index_0 = _hidl_index_02;
            if (_hidl_index_0 < _hidl_vec_size2) {
                CdmaT53AudioControlInfoRecord _hidl_vec_element7 = new CdmaT53AudioControlInfoRecord();
                _hidl_vec_element7.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 2));
                this.audioCtrl.add(_hidl_vec_element7);
                _hidl_index_02 = _hidl_index_0 + 1;
            } else {
                return;
            }
        }
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(120);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<CdmaInformationRecord> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        int _hidl_index_0 = 0;
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 120);
        while (_hidl_index_0 < _hidl_vec_size) {
            ((CdmaInformationRecord) _hidl_vec.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 120));
            _hidl_index_0++;
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_index_0;
        HwBlob hwBlob = _hidl_blob;
        hwBlob.putInt32(_hidl_offset + 0, this.name);
        int _hidl_vec_size = this.display.size();
        hwBlob.putInt32((_hidl_offset + 8) + 8, _hidl_vec_size);
        int _hidl_index_02 = 0;
        hwBlob.putBool((_hidl_offset + 8) + 12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 16);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((CdmaDisplayInfoRecord) this.display.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 16));
        }
        hwBlob.putBlob((_hidl_offset + 8) + 0, childBlob);
        _hidl_vec_size = this.number.size();
        hwBlob.putInt32((_hidl_offset + 24) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 24) + 12, false);
        HwBlob childBlob2 = new HwBlob(_hidl_vec_size * 24);
        for (int _hidl_index_03 = 0; _hidl_index_03 < _hidl_vec_size; _hidl_index_03++) {
            ((CdmaNumberInfoRecord) this.number.get(_hidl_index_03)).writeEmbeddedToBlob(childBlob2, (long) (_hidl_index_03 * 24));
        }
        hwBlob.putBlob((_hidl_offset + 24) + 0, childBlob2);
        _hidl_vec_size = this.signal.size();
        hwBlob.putInt32((_hidl_offset + 40) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 40) + 12, false);
        childBlob = new HwBlob(_hidl_vec_size * 4);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((CdmaSignalInfoRecord) this.signal.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 4));
        }
        hwBlob.putBlob((_hidl_offset + 40) + 0, childBlob);
        _hidl_vec_size = this.redir.size();
        hwBlob.putInt32((_hidl_offset + 56) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 56) + 12, false);
        childBlob = new HwBlob(_hidl_vec_size * 32);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((CdmaRedirectingNumberInfoRecord) this.redir.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 32));
        }
        hwBlob.putBlob((_hidl_offset + 56) + 0, childBlob);
        _hidl_vec_size = this.lineCtrl.size();
        hwBlob.putInt32((_hidl_offset + 72) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 72) + 12, false);
        childBlob = new HwBlob(_hidl_vec_size * 4);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((CdmaLineControlInfoRecord) this.lineCtrl.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 4));
        }
        hwBlob.putBlob((_hidl_offset + 72) + 0, childBlob);
        _hidl_vec_size = this.clir.size();
        hwBlob.putInt32((_hidl_offset + 88) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 88) + 12, false);
        childBlob = new HwBlob(_hidl_vec_size * 1);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((CdmaT53ClirInfoRecord) this.clir.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 1));
        }
        hwBlob.putBlob((_hidl_offset + 88) + 0, childBlob);
        _hidl_vec_size = this.audioCtrl.size();
        hwBlob.putInt32((_hidl_offset + 104) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 104) + 12, false);
        childBlob = new HwBlob(_hidl_vec_size * 2);
        while (true) {
            _hidl_index_0 = _hidl_index_02;
            if (_hidl_index_0 < _hidl_vec_size) {
                ((CdmaT53AudioControlInfoRecord) this.audioCtrl.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 2));
                _hidl_index_02 = _hidl_index_0 + 1;
            } else {
                hwBlob.putBlob((_hidl_offset + 104) + 0, childBlob);
                return;
            }
        }
    }
}
