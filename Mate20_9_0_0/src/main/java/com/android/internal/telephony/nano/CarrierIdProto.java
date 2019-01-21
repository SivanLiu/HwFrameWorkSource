package com.android.internal.telephony.nano;

import com.android.internal.telephony.protobuf.nano.CodedInputByteBufferNano;
import com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano;
import com.android.internal.telephony.protobuf.nano.ExtendableMessageNano;
import com.android.internal.telephony.protobuf.nano.InternalNano;
import com.android.internal.telephony.protobuf.nano.InvalidProtocolBufferNanoException;
import com.android.internal.telephony.protobuf.nano.MessageNano;
import com.android.internal.telephony.protobuf.nano.WireFormatNano;
import java.io.IOException;

public interface CarrierIdProto {

    public static final class CarrierAttribute extends ExtendableMessageNano<CarrierAttribute> {
        private static volatile CarrierAttribute[] _emptyArray;
        public String[] gid1;
        public String[] gid2;
        public String[] iccidPrefix;
        public String[] imsiPrefixXpattern;
        public String[] mccmncTuple;
        public String[] plmn;
        public String[] preferredApn;
        public String[] spn;

        public static CarrierAttribute[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new CarrierAttribute[0];
                    }
                }
            }
            return _emptyArray;
        }

        public CarrierAttribute() {
            clear();
        }

        public CarrierAttribute clear() {
            this.mccmncTuple = WireFormatNano.EMPTY_STRING_ARRAY;
            this.imsiPrefixXpattern = WireFormatNano.EMPTY_STRING_ARRAY;
            this.spn = WireFormatNano.EMPTY_STRING_ARRAY;
            this.plmn = WireFormatNano.EMPTY_STRING_ARRAY;
            this.gid1 = WireFormatNano.EMPTY_STRING_ARRAY;
            this.gid2 = WireFormatNano.EMPTY_STRING_ARRAY;
            this.preferredApn = WireFormatNano.EMPTY_STRING_ARRAY;
            this.iccidPrefix = WireFormatNano.EMPTY_STRING_ARRAY;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            int i;
            int i2 = 0;
            if (this.mccmncTuple != null && this.mccmncTuple.length > 0) {
                for (String element : this.mccmncTuple) {
                    if (element != null) {
                        output.writeString(1, element);
                    }
                }
            }
            if (this.imsiPrefixXpattern != null && this.imsiPrefixXpattern.length > 0) {
                for (String element2 : this.imsiPrefixXpattern) {
                    if (element2 != null) {
                        output.writeString(2, element2);
                    }
                }
            }
            if (this.spn != null && this.spn.length > 0) {
                for (String element22 : this.spn) {
                    if (element22 != null) {
                        output.writeString(3, element22);
                    }
                }
            }
            if (this.plmn != null && this.plmn.length > 0) {
                for (String element222 : this.plmn) {
                    if (element222 != null) {
                        output.writeString(4, element222);
                    }
                }
            }
            if (this.gid1 != null && this.gid1.length > 0) {
                for (String element2222 : this.gid1) {
                    if (element2222 != null) {
                        output.writeString(5, element2222);
                    }
                }
            }
            if (this.gid2 != null && this.gid2.length > 0) {
                for (String element22222 : this.gid2) {
                    if (element22222 != null) {
                        output.writeString(6, element22222);
                    }
                }
            }
            if (this.preferredApn != null && this.preferredApn.length > 0) {
                for (String element222222 : this.preferredApn) {
                    if (element222222 != null) {
                        output.writeString(7, element222222);
                    }
                }
            }
            if (this.iccidPrefix != null && this.iccidPrefix.length > 0) {
                while (true) {
                    i = i2;
                    if (i >= this.iccidPrefix.length) {
                        break;
                    }
                    String element3 = this.iccidPrefix[i];
                    if (element3 != null) {
                        output.writeString(8, element3);
                    }
                    i2 = i + 1;
                }
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int dataSize;
            int dataCount;
            int i;
            int size = super.computeSerializedSize();
            int i2 = 0;
            if (this.mccmncTuple != null && this.mccmncTuple.length > 0) {
                dataSize = 0;
                dataCount = 0;
                for (String element : this.mccmncTuple) {
                    if (element != null) {
                        dataCount++;
                        dataSize += CodedOutputByteBufferNano.computeStringSizeNoTag(element);
                    }
                }
                size = (size + dataSize) + (1 * dataCount);
            }
            if (this.imsiPrefixXpattern != null && this.imsiPrefixXpattern.length > 0) {
                dataSize = 0;
                dataCount = 0;
                for (String element2 : this.imsiPrefixXpattern) {
                    if (element2 != null) {
                        dataCount++;
                        dataSize += CodedOutputByteBufferNano.computeStringSizeNoTag(element2);
                    }
                }
                size = (size + dataSize) + (1 * dataCount);
            }
            if (this.spn != null && this.spn.length > 0) {
                dataSize = 0;
                dataCount = 0;
                for (String element22 : this.spn) {
                    if (element22 != null) {
                        dataCount++;
                        dataSize += CodedOutputByteBufferNano.computeStringSizeNoTag(element22);
                    }
                }
                size = (size + dataSize) + (1 * dataCount);
            }
            if (this.plmn != null && this.plmn.length > 0) {
                dataSize = 0;
                dataCount = 0;
                for (String element222 : this.plmn) {
                    if (element222 != null) {
                        dataCount++;
                        dataSize += CodedOutputByteBufferNano.computeStringSizeNoTag(element222);
                    }
                }
                size = (size + dataSize) + (1 * dataCount);
            }
            if (this.gid1 != null && this.gid1.length > 0) {
                dataSize = 0;
                dataCount = 0;
                for (String element2222 : this.gid1) {
                    if (element2222 != null) {
                        dataCount++;
                        dataSize += CodedOutputByteBufferNano.computeStringSizeNoTag(element2222);
                    }
                }
                size = (size + dataSize) + (1 * dataCount);
            }
            if (this.gid2 != null && this.gid2.length > 0) {
                dataSize = 0;
                dataCount = 0;
                for (String element22222 : this.gid2) {
                    if (element22222 != null) {
                        dataCount++;
                        dataSize += CodedOutputByteBufferNano.computeStringSizeNoTag(element22222);
                    }
                }
                size = (size + dataSize) + (1 * dataCount);
            }
            if (this.preferredApn != null && this.preferredApn.length > 0) {
                dataSize = 0;
                dataCount = 0;
                for (String element222222 : this.preferredApn) {
                    if (element222222 != null) {
                        dataCount++;
                        dataSize += CodedOutputByteBufferNano.computeStringSizeNoTag(element222222);
                    }
                }
                size = (size + dataSize) + (1 * dataCount);
            }
            if (this.iccidPrefix == null || this.iccidPrefix.length <= 0) {
                return size;
            }
            i = 0;
            dataSize = 0;
            while (i2 < this.iccidPrefix.length) {
                String element3 = this.iccidPrefix[i2];
                if (element3 != null) {
                    i++;
                    dataSize += CodedOutputByteBufferNano.computeStringSizeNoTag(element3);
                }
                i2++;
            }
            return (size + dataSize) + (1 * i);
        }

        public CarrierAttribute mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                if (tag == 0) {
                    return this;
                }
                int arrayLength;
                int i;
                String[] newArray;
                if (tag == 10) {
                    arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 10);
                    i = this.mccmncTuple == null ? 0 : this.mccmncTuple.length;
                    newArray = new String[(i + arrayLength)];
                    if (i != 0) {
                        System.arraycopy(this.mccmncTuple, 0, newArray, 0, i);
                    }
                    while (i < newArray.length - 1) {
                        newArray[i] = input.readString();
                        input.readTag();
                        i++;
                    }
                    newArray[i] = input.readString();
                    this.mccmncTuple = newArray;
                } else if (tag == 18) {
                    arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 18);
                    i = this.imsiPrefixXpattern == null ? 0 : this.imsiPrefixXpattern.length;
                    newArray = new String[(i + arrayLength)];
                    if (i != 0) {
                        System.arraycopy(this.imsiPrefixXpattern, 0, newArray, 0, i);
                    }
                    while (i < newArray.length - 1) {
                        newArray[i] = input.readString();
                        input.readTag();
                        i++;
                    }
                    newArray[i] = input.readString();
                    this.imsiPrefixXpattern = newArray;
                } else if (tag == 26) {
                    arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 26);
                    i = this.spn == null ? 0 : this.spn.length;
                    newArray = new String[(i + arrayLength)];
                    if (i != 0) {
                        System.arraycopy(this.spn, 0, newArray, 0, i);
                    }
                    while (i < newArray.length - 1) {
                        newArray[i] = input.readString();
                        input.readTag();
                        i++;
                    }
                    newArray[i] = input.readString();
                    this.spn = newArray;
                } else if (tag == 34) {
                    arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 34);
                    i = this.plmn == null ? 0 : this.plmn.length;
                    newArray = new String[(i + arrayLength)];
                    if (i != 0) {
                        System.arraycopy(this.plmn, 0, newArray, 0, i);
                    }
                    while (i < newArray.length - 1) {
                        newArray[i] = input.readString();
                        input.readTag();
                        i++;
                    }
                    newArray[i] = input.readString();
                    this.plmn = newArray;
                } else if (tag == 42) {
                    arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 42);
                    i = this.gid1 == null ? 0 : this.gid1.length;
                    newArray = new String[(i + arrayLength)];
                    if (i != 0) {
                        System.arraycopy(this.gid1, 0, newArray, 0, i);
                    }
                    while (i < newArray.length - 1) {
                        newArray[i] = input.readString();
                        input.readTag();
                        i++;
                    }
                    newArray[i] = input.readString();
                    this.gid1 = newArray;
                } else if (tag == 50) {
                    arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 50);
                    i = this.gid2 == null ? 0 : this.gid2.length;
                    newArray = new String[(i + arrayLength)];
                    if (i != 0) {
                        System.arraycopy(this.gid2, 0, newArray, 0, i);
                    }
                    while (i < newArray.length - 1) {
                        newArray[i] = input.readString();
                        input.readTag();
                        i++;
                    }
                    newArray[i] = input.readString();
                    this.gid2 = newArray;
                } else if (tag == 58) {
                    arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 58);
                    i = this.preferredApn == null ? 0 : this.preferredApn.length;
                    newArray = new String[(i + arrayLength)];
                    if (i != 0) {
                        System.arraycopy(this.preferredApn, 0, newArray, 0, i);
                    }
                    while (i < newArray.length - 1) {
                        newArray[i] = input.readString();
                        input.readTag();
                        i++;
                    }
                    newArray[i] = input.readString();
                    this.preferredApn = newArray;
                } else if (tag == 66) {
                    arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 66);
                    i = this.iccidPrefix == null ? 0 : this.iccidPrefix.length;
                    newArray = new String[(i + arrayLength)];
                    if (i != 0) {
                        System.arraycopy(this.iccidPrefix, 0, newArray, 0, i);
                    }
                    while (i < newArray.length - 1) {
                        newArray[i] = input.readString();
                        input.readTag();
                        i++;
                    }
                    newArray[i] = input.readString();
                    this.iccidPrefix = newArray;
                } else if (!storeUnknownField(input, tag)) {
                    return this;
                }
            }
        }

        public static CarrierAttribute parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (CarrierAttribute) MessageNano.mergeFrom(new CarrierAttribute(), data);
        }

        public static CarrierAttribute parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new CarrierAttribute().mergeFrom(input);
        }
    }

    public static final class CarrierId extends ExtendableMessageNano<CarrierId> {
        private static volatile CarrierId[] _emptyArray;
        public int canonicalId;
        public CarrierAttribute[] carrierAttribute;
        public String carrierName;

        public static CarrierId[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new CarrierId[0];
                    }
                }
            }
            return _emptyArray;
        }

        public CarrierId() {
            clear();
        }

        public CarrierId clear() {
            this.canonicalId = 0;
            this.carrierName = "";
            this.carrierAttribute = CarrierAttribute.emptyArray();
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.canonicalId != 0) {
                output.writeInt32(1, this.canonicalId);
            }
            if (!this.carrierName.equals("")) {
                output.writeString(2, this.carrierName);
            }
            if (this.carrierAttribute != null && this.carrierAttribute.length > 0) {
                for (CarrierAttribute element : this.carrierAttribute) {
                    if (element != null) {
                        output.writeMessage(3, element);
                    }
                }
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.canonicalId != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(1, this.canonicalId);
            }
            if (!this.carrierName.equals("")) {
                size += CodedOutputByteBufferNano.computeStringSize(2, this.carrierName);
            }
            if (this.carrierAttribute != null && this.carrierAttribute.length > 0) {
                for (CarrierAttribute element : this.carrierAttribute) {
                    if (element != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(3, element);
                    }
                }
            }
            return size;
        }

        public CarrierId mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    this.canonicalId = input.readInt32();
                } else if (tag == 18) {
                    this.carrierName = input.readString();
                } else if (tag == 26) {
                    int arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 26);
                    int i = this.carrierAttribute == null ? 0 : this.carrierAttribute.length;
                    CarrierAttribute[] newArray = new CarrierAttribute[(i + arrayLength)];
                    if (i != 0) {
                        System.arraycopy(this.carrierAttribute, 0, newArray, 0, i);
                    }
                    while (i < newArray.length - 1) {
                        newArray[i] = new CarrierAttribute();
                        input.readMessage(newArray[i]);
                        input.readTag();
                        i++;
                    }
                    newArray[i] = new CarrierAttribute();
                    input.readMessage(newArray[i]);
                    this.carrierAttribute = newArray;
                } else if (!storeUnknownField(input, tag)) {
                    return this;
                }
            }
        }

        public static CarrierId parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (CarrierId) MessageNano.mergeFrom(new CarrierId(), data);
        }

        public static CarrierId parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new CarrierId().mergeFrom(input);
        }
    }

    public static final class CarrierList extends ExtendableMessageNano<CarrierList> {
        private static volatile CarrierList[] _emptyArray;
        public CarrierId[] carrierId;
        public int version;

        public static CarrierList[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new CarrierList[0];
                    }
                }
            }
            return _emptyArray;
        }

        public CarrierList() {
            clear();
        }

        public CarrierList clear() {
            this.carrierId = CarrierId.emptyArray();
            this.version = 0;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.carrierId != null && this.carrierId.length > 0) {
                for (CarrierId element : this.carrierId) {
                    if (element != null) {
                        output.writeMessage(1, element);
                    }
                }
            }
            if (this.version != 0) {
                output.writeInt32(2, this.version);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.carrierId != null && this.carrierId.length > 0) {
                for (CarrierId element : this.carrierId) {
                    if (element != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(1, element);
                    }
                }
            }
            if (this.version != 0) {
                return size + CodedOutputByteBufferNano.computeInt32Size(2, this.version);
            }
            return size;
        }

        public CarrierList mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 10) {
                    int arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 10);
                    int i = this.carrierId == null ? 0 : this.carrierId.length;
                    CarrierId[] newArray = new CarrierId[(i + arrayLength)];
                    if (i != 0) {
                        System.arraycopy(this.carrierId, 0, newArray, 0, i);
                    }
                    while (i < newArray.length - 1) {
                        newArray[i] = new CarrierId();
                        input.readMessage(newArray[i]);
                        input.readTag();
                        i++;
                    }
                    newArray[i] = new CarrierId();
                    input.readMessage(newArray[i]);
                    this.carrierId = newArray;
                } else if (tag == 16) {
                    this.version = input.readInt32();
                } else if (!storeUnknownField(input, tag)) {
                    return this;
                }
            }
        }

        public static CarrierList parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (CarrierList) MessageNano.mergeFrom(new CarrierList(), data);
        }

        public static CarrierList parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new CarrierList().mergeFrom(input);
        }
    }
}
