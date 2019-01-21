package android.hardware.radio.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class ImsSmsMessage {
    public final ArrayList<CdmaSmsMessage> cdmaMessage = new ArrayList();
    public final ArrayList<GsmSmsMessage> gsmMessage = new ArrayList();
    public int messageRef;
    public boolean retry;
    public int tech;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != ImsSmsMessage.class) {
            return false;
        }
        ImsSmsMessage other = (ImsSmsMessage) otherObject;
        if (this.tech == other.tech && this.retry == other.retry && this.messageRef == other.messageRef && HidlSupport.deepEquals(this.cdmaMessage, other.cdmaMessage) && HidlSupport.deepEquals(this.gsmMessage, other.gsmMessage)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.tech))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.retry))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.messageRef))), Integer.valueOf(HidlSupport.deepHashCode(this.cdmaMessage)), Integer.valueOf(HidlSupport.deepHashCode(this.gsmMessage))});
    }

    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".tech = ");
        builder.append(RadioTechnologyFamily.toString(this.tech));
        builder.append(", .retry = ");
        builder.append(this.retry);
        builder.append(", .messageRef = ");
        builder.append(this.messageRef);
        builder.append(", .cdmaMessage = ");
        builder.append(this.cdmaMessage);
        builder.append(", .gsmMessage = ");
        builder.append(this.gsmMessage);
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(48), 0);
    }

    public static final ArrayList<ImsSmsMessage> readVectorFromParcel(HwParcel parcel) {
        ArrayList<ImsSmsMessage> _hidl_vec = new ArrayList();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 48), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ImsSmsMessage _hidl_vec_element = new ImsSmsMessage();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 48));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_index_0;
        HwParcel hwParcel = parcel;
        HwBlob hwBlob = _hidl_blob;
        this.tech = hwBlob.getInt32(_hidl_offset + 0);
        this.retry = hwBlob.getBool(_hidl_offset + 4);
        this.messageRef = hwBlob.getInt32(_hidl_offset + 8);
        int _hidl_vec_size = hwBlob.getInt32((_hidl_offset + 16) + 8);
        int _hidl_vec_size2 = _hidl_vec_size;
        HwBlob childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size * 88), _hidl_blob.handle(), (_hidl_offset + 16) + 0, 1);
        this.cdmaMessage.clear();
        int _hidl_index_02 = 0;
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size2; _hidl_index_0++) {
            CdmaSmsMessage _hidl_vec_element = new CdmaSmsMessage();
            _hidl_vec_element.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 88));
            this.cdmaMessage.add(_hidl_vec_element);
        }
        _hidl_vec_size2 = hwBlob.getInt32((_hidl_offset + 32) + 8);
        childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * 32), _hidl_blob.handle(), 0 + (_hidl_offset + 32), true);
        this.gsmMessage.clear();
        while (true) {
            _hidl_index_0 = _hidl_index_02;
            if (_hidl_index_0 < _hidl_vec_size2) {
                GsmSmsMessage _hidl_vec_element2 = new GsmSmsMessage();
                _hidl_vec_element2.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 32));
                this.gsmMessage.add(_hidl_vec_element2);
                _hidl_index_02 = _hidl_index_0 + 1;
            } else {
                return;
            }
        }
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(48);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<ImsSmsMessage> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        int _hidl_index_0 = 0;
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 48);
        while (_hidl_index_0 < _hidl_vec_size) {
            ((ImsSmsMessage) _hidl_vec.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 48));
            _hidl_index_0++;
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        HwBlob hwBlob = _hidl_blob;
        hwBlob.putInt32(_hidl_offset + 0, this.tech);
        hwBlob.putBool(_hidl_offset + 4, this.retry);
        hwBlob.putInt32(_hidl_offset + 8, this.messageRef);
        int _hidl_vec_size = this.cdmaMessage.size();
        hwBlob.putInt32((_hidl_offset + 16) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 16) + 12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 88);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((CdmaSmsMessage) this.cdmaMessage.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 88));
        }
        hwBlob.putBlob((_hidl_offset + 16) + 0, childBlob);
        _hidl_vec_size = this.gsmMessage.size();
        hwBlob.putInt32((_hidl_offset + 32) + 8, _hidl_vec_size);
        int _hidl_index_02 = 0;
        hwBlob.putBool((_hidl_offset + 32) + 12, false);
        HwBlob childBlob2 = new HwBlob(_hidl_vec_size * 32);
        while (true) {
            int _hidl_index_03 = _hidl_index_02;
            if (_hidl_index_03 < _hidl_vec_size) {
                ((GsmSmsMessage) this.gsmMessage.get(_hidl_index_03)).writeEmbeddedToBlob(childBlob2, (long) (_hidl_index_03 * 32));
                _hidl_index_02 = _hidl_index_03 + 1;
            } else {
                hwBlob.putBlob((_hidl_offset + 32) + 0, childBlob2);
                return;
            }
        }
    }
}
