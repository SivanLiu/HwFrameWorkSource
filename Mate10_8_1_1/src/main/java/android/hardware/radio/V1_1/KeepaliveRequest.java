package android.hardware.radio.V1_1;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class KeepaliveRequest {
    public int cid;
    public final ArrayList<Byte> destinationAddress = new ArrayList();
    public int destinationPort;
    public int maxKeepaliveIntervalMillis;
    public final ArrayList<Byte> sourceAddress = new ArrayList();
    public int sourcePort;
    public int type;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != KeepaliveRequest.class) {
            return false;
        }
        KeepaliveRequest other = (KeepaliveRequest) otherObject;
        return this.type == other.type && HidlSupport.deepEquals(this.sourceAddress, other.sourceAddress) && this.sourcePort == other.sourcePort && HidlSupport.deepEquals(this.destinationAddress, other.destinationAddress) && this.destinationPort == other.destinationPort && this.maxKeepaliveIntervalMillis == other.maxKeepaliveIntervalMillis && this.cid == other.cid;
    }

    public final int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.type))), Integer.valueOf(HidlSupport.deepHashCode(this.sourceAddress)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.sourcePort))), Integer.valueOf(HidlSupport.deepHashCode(this.destinationAddress)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.destinationPort))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxKeepaliveIntervalMillis))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.cid)))});
    }

    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".type = ");
        builder.append(KeepaliveType.toString(this.type));
        builder.append(", .sourceAddress = ");
        builder.append(this.sourceAddress);
        builder.append(", .sourcePort = ");
        builder.append(this.sourcePort);
        builder.append(", .destinationAddress = ");
        builder.append(this.destinationAddress);
        builder.append(", .destinationPort = ");
        builder.append(this.destinationPort);
        builder.append(", .maxKeepaliveIntervalMillis = ");
        builder.append(this.maxKeepaliveIntervalMillis);
        builder.append(", .cid = ");
        builder.append(this.cid);
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(64), 0);
    }

    public static final ArrayList<KeepaliveRequest> readVectorFromParcel(HwParcel parcel) {
        ArrayList<KeepaliveRequest> _hidl_vec = new ArrayList();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 64), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            KeepaliveRequest _hidl_vec_element = new KeepaliveRequest();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 64));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_index_0;
        this.type = _hidl_blob.getInt32(0 + _hidl_offset);
        int _hidl_vec_size = _hidl_blob.getInt32((8 + _hidl_offset) + 8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 1), _hidl_blob.handle(), (8 + _hidl_offset) + 0, true);
        this.sourceAddress.clear();
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            this.sourceAddress.add(Byte.valueOf(childBlob.getInt8((long) (_hidl_index_0 * 1))));
        }
        this.sourcePort = _hidl_blob.getInt32(24 + _hidl_offset);
        _hidl_vec_size = _hidl_blob.getInt32((32 + _hidl_offset) + 8);
        childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 1), _hidl_blob.handle(), (32 + _hidl_offset) + 0, true);
        this.destinationAddress.clear();
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            this.destinationAddress.add(Byte.valueOf(childBlob.getInt8((long) (_hidl_index_0 * 1))));
        }
        this.destinationPort = _hidl_blob.getInt32(48 + _hidl_offset);
        this.maxKeepaliveIntervalMillis = _hidl_blob.getInt32(52 + _hidl_offset);
        this.cid = _hidl_blob.getInt32(56 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(64);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<KeepaliveRequest> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 64);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((KeepaliveRequest) _hidl_vec.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 64));
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_index_0;
        _hidl_blob.putInt32(0 + _hidl_offset, this.type);
        int _hidl_vec_size = this.sourceAddress.size();
        _hidl_blob.putInt32((8 + _hidl_offset) + 8, _hidl_vec_size);
        _hidl_blob.putBool((8 + _hidl_offset) + 12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 1);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            childBlob.putInt8((long) (_hidl_index_0 * 1), ((Byte) this.sourceAddress.get(_hidl_index_0)).byteValue());
        }
        _hidl_blob.putBlob((8 + _hidl_offset) + 0, childBlob);
        _hidl_blob.putInt32(24 + _hidl_offset, this.sourcePort);
        _hidl_vec_size = this.destinationAddress.size();
        _hidl_blob.putInt32((32 + _hidl_offset) + 8, _hidl_vec_size);
        _hidl_blob.putBool((32 + _hidl_offset) + 12, false);
        childBlob = new HwBlob(_hidl_vec_size * 1);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            childBlob.putInt8((long) (_hidl_index_0 * 1), ((Byte) this.destinationAddress.get(_hidl_index_0)).byteValue());
        }
        _hidl_blob.putBlob((32 + _hidl_offset) + 0, childBlob);
        _hidl_blob.putInt32(48 + _hidl_offset, this.destinationPort);
        _hidl_blob.putInt32(52 + _hidl_offset, this.maxKeepaliveIntervalMillis);
        _hidl_blob.putInt32(56 + _hidl_offset, this.cid);
    }
}
