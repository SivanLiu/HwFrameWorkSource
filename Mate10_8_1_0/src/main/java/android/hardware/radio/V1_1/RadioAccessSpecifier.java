package android.hardware.radio.V1_1;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class RadioAccessSpecifier {
    public final ArrayList<Integer> channels = new ArrayList();
    public final ArrayList<Integer> eutranBands = new ArrayList();
    public final ArrayList<Integer> geranBands = new ArrayList();
    public int radioAccessNetwork;
    public final ArrayList<Integer> utranBands = new ArrayList();

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != RadioAccessSpecifier.class) {
            return false;
        }
        RadioAccessSpecifier other = (RadioAccessSpecifier) otherObject;
        return this.radioAccessNetwork == other.radioAccessNetwork && HidlSupport.deepEquals(this.geranBands, other.geranBands) && HidlSupport.deepEquals(this.utranBands, other.utranBands) && HidlSupport.deepEquals(this.eutranBands, other.eutranBands) && HidlSupport.deepEquals(this.channels, other.channels);
    }

    public final int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.radioAccessNetwork))), Integer.valueOf(HidlSupport.deepHashCode(this.geranBands)), Integer.valueOf(HidlSupport.deepHashCode(this.utranBands)), Integer.valueOf(HidlSupport.deepHashCode(this.eutranBands)), Integer.valueOf(HidlSupport.deepHashCode(this.channels))});
    }

    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".radioAccessNetwork = ");
        builder.append(RadioAccessNetworks.toString(this.radioAccessNetwork));
        builder.append(", .geranBands = ");
        builder.append(this.geranBands);
        builder.append(", .utranBands = ");
        builder.append(this.utranBands);
        builder.append(", .eutranBands = ");
        builder.append(this.eutranBands);
        builder.append(", .channels = ");
        builder.append(this.channels);
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(72), 0);
    }

    public static final ArrayList<RadioAccessSpecifier> readVectorFromParcel(HwParcel parcel) {
        ArrayList<RadioAccessSpecifier> _hidl_vec = new ArrayList();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 72), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            RadioAccessSpecifier _hidl_vec_element = new RadioAccessSpecifier();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 72));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_index_0;
        this.radioAccessNetwork = _hidl_blob.getInt32(0 + _hidl_offset);
        int _hidl_vec_size = _hidl_blob.getInt32((8 + _hidl_offset) + 8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 4), _hidl_blob.handle(), (8 + _hidl_offset) + 0, true);
        this.geranBands.clear();
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            this.geranBands.add(Integer.valueOf(childBlob.getInt32((long) (_hidl_index_0 * 4))));
        }
        _hidl_vec_size = _hidl_blob.getInt32((24 + _hidl_offset) + 8);
        childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 4), _hidl_blob.handle(), (24 + _hidl_offset) + 0, true);
        this.utranBands.clear();
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            this.utranBands.add(Integer.valueOf(childBlob.getInt32((long) (_hidl_index_0 * 4))));
        }
        _hidl_vec_size = _hidl_blob.getInt32((40 + _hidl_offset) + 8);
        childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 4), _hidl_blob.handle(), (40 + _hidl_offset) + 0, true);
        this.eutranBands.clear();
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            this.eutranBands.add(Integer.valueOf(childBlob.getInt32((long) (_hidl_index_0 * 4))));
        }
        _hidl_vec_size = _hidl_blob.getInt32((56 + _hidl_offset) + 8);
        childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 4), _hidl_blob.handle(), (56 + _hidl_offset) + 0, true);
        this.channels.clear();
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            this.channels.add(Integer.valueOf(childBlob.getInt32((long) (_hidl_index_0 * 4))));
        }
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(72);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<RadioAccessSpecifier> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 72);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((RadioAccessSpecifier) _hidl_vec.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 72));
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_index_0;
        _hidl_blob.putInt32(0 + _hidl_offset, this.radioAccessNetwork);
        int _hidl_vec_size = this.geranBands.size();
        _hidl_blob.putInt32((8 + _hidl_offset) + 8, _hidl_vec_size);
        _hidl_blob.putBool((8 + _hidl_offset) + 12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 4);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            childBlob.putInt32((long) (_hidl_index_0 * 4), ((Integer) this.geranBands.get(_hidl_index_0)).intValue());
        }
        _hidl_blob.putBlob((8 + _hidl_offset) + 0, childBlob);
        _hidl_vec_size = this.utranBands.size();
        _hidl_blob.putInt32((24 + _hidl_offset) + 8, _hidl_vec_size);
        _hidl_blob.putBool((24 + _hidl_offset) + 12, false);
        childBlob = new HwBlob(_hidl_vec_size * 4);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            childBlob.putInt32((long) (_hidl_index_0 * 4), ((Integer) this.utranBands.get(_hidl_index_0)).intValue());
        }
        _hidl_blob.putBlob((24 + _hidl_offset) + 0, childBlob);
        _hidl_vec_size = this.eutranBands.size();
        _hidl_blob.putInt32((40 + _hidl_offset) + 8, _hidl_vec_size);
        _hidl_blob.putBool((40 + _hidl_offset) + 12, false);
        childBlob = new HwBlob(_hidl_vec_size * 4);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            childBlob.putInt32((long) (_hidl_index_0 * 4), ((Integer) this.eutranBands.get(_hidl_index_0)).intValue());
        }
        _hidl_blob.putBlob((40 + _hidl_offset) + 0, childBlob);
        _hidl_vec_size = this.channels.size();
        _hidl_blob.putInt32((56 + _hidl_offset) + 8, _hidl_vec_size);
        _hidl_blob.putBool((56 + _hidl_offset) + 12, false);
        childBlob = new HwBlob(_hidl_vec_size * 4);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            childBlob.putInt32((long) (_hidl_index_0 * 4), ((Integer) this.channels.get(_hidl_index_0)).intValue());
        }
        _hidl_blob.putBlob((56 + _hidl_offset) + 0, childBlob);
    }
}
