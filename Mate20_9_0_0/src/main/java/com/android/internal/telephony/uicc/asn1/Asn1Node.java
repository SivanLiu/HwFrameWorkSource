package com.android.internal.telephony.uicc.asn1;

import com.android.internal.telephony.uicc.IccUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Asn1Node {
    private static final List<Asn1Node> EMPTY_NODE_LIST = Collections.emptyList();
    private static final byte[] FALSE_BYTES = new byte[]{(byte) 0};
    private static final int INT_BYTES = 4;
    private static final byte[] TRUE_BYTES = new byte[]{(byte) -1};
    private final List<Asn1Node> mChildren;
    private final boolean mConstructed;
    private byte[] mDataBytes;
    private int mDataLength;
    private int mDataOffset;
    private int mEncodedLength;
    private final int mTag;

    public static final class Builder {
        private final List<Asn1Node> mChildren;
        private final int mTag;

        private Builder(int tag) {
            if (Asn1Node.isConstructedTag(tag)) {
                this.mTag = tag;
                this.mChildren = new ArrayList();
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Builder should be created for a constructed tag: ");
            stringBuilder.append(tag);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public Builder addChild(Asn1Node child) {
            this.mChildren.add(child);
            return this;
        }

        public Builder addChild(Builder child) {
            this.mChildren.add(child.build());
            return this;
        }

        public Builder addChildren(byte[] encodedBytes) throws InvalidAsn1DataException {
            Asn1Decoder subDecoder = new Asn1Decoder(encodedBytes, 0, encodedBytes.length);
            while (subDecoder.hasNextNode()) {
                this.mChildren.add(subDecoder.nextNode());
            }
            return this;
        }

        public Builder addChildAsInteger(int tag, int value) {
            if (Asn1Node.isConstructedTag(tag)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot set value of a constructed tag: ");
                stringBuilder.append(tag);
                throw new IllegalStateException(stringBuilder.toString());
            }
            byte[] dataBytes = IccUtils.signedIntToBytes(value);
            addChild(new Asn1Node(tag, dataBytes, 0, dataBytes.length));
            return this;
        }

        public Builder addChildAsString(int tag, String value) {
            if (Asn1Node.isConstructedTag(tag)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot set value of a constructed tag: ");
                stringBuilder.append(tag);
                throw new IllegalStateException(stringBuilder.toString());
            }
            byte[] dataBytes = value.getBytes(StandardCharsets.UTF_8);
            addChild(new Asn1Node(tag, dataBytes, 0, dataBytes.length));
            return this;
        }

        public Builder addChildAsBytes(int tag, byte[] value) {
            if (Asn1Node.isConstructedTag(tag)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot set value of a constructed tag: ");
                stringBuilder.append(tag);
                throw new IllegalStateException(stringBuilder.toString());
            }
            addChild(new Asn1Node(tag, value, 0, value.length));
            return this;
        }

        public Builder addChildAsBytesFromHex(int tag, String hex) {
            return addChildAsBytes(tag, IccUtils.hexStringToBytes(hex));
        }

        public Builder addChildAsBits(int tag, int value) {
            if (Asn1Node.isConstructedTag(tag)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot set value of a constructed tag: ");
                stringBuilder.append(tag);
                throw new IllegalStateException(stringBuilder.toString());
            }
            byte[] dataBytes = new byte[5];
            value = Integer.reverse(value);
            int dataLength = 0;
            for (int i = 1; i < dataBytes.length; i++) {
                dataBytes[i] = (byte) (value >> ((4 - i) * 8));
                if (dataBytes[i] != (byte) 0) {
                    dataLength = i;
                }
            }
            dataLength++;
            dataBytes[0] = IccUtils.countTrailingZeros(dataBytes[dataLength - 1]);
            addChild(new Asn1Node(tag, dataBytes, 0, dataLength));
            return this;
        }

        public Builder addChildAsBoolean(int tag, boolean value) {
            if (Asn1Node.isConstructedTag(tag)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot set value of a constructed tag: ");
                stringBuilder.append(tag);
                throw new IllegalStateException(stringBuilder.toString());
            }
            addChild(new Asn1Node(tag, value ? Asn1Node.TRUE_BYTES : Asn1Node.FALSE_BYTES, 0, 1));
            return this;
        }

        public Asn1Node build() {
            return new Asn1Node(this.mTag, this.mChildren);
        }
    }

    public static Builder newBuilder(int tag) {
        return new Builder(tag);
    }

    private static boolean isConstructedTag(int tag) {
        return (IccUtils.unsignedIntToBytes(tag)[0] & 32) != 0;
    }

    private static int calculateEncodedBytesNumForLength(int length) {
        if (length > 127) {
            return 1 + IccUtils.byteNumForUnsignedInt(length);
        }
        return 1;
    }

    Asn1Node(int tag, byte[] src, int offset, int length) {
        this.mTag = tag;
        this.mConstructed = isConstructedTag(tag);
        this.mDataBytes = src;
        this.mDataOffset = offset;
        this.mDataLength = length;
        this.mChildren = this.mConstructed ? new ArrayList() : EMPTY_NODE_LIST;
        this.mEncodedLength = (IccUtils.byteNumForUnsignedInt(this.mTag) + calculateEncodedBytesNumForLength(this.mDataLength)) + this.mDataLength;
    }

    private Asn1Node(int tag, List<Asn1Node> children) {
        this.mTag = tag;
        this.mConstructed = true;
        this.mChildren = children;
        int i = 0;
        this.mDataLength = 0;
        int size = children.size();
        while (i < size) {
            this.mDataLength += ((Asn1Node) children.get(i)).mEncodedLength;
            i++;
        }
        this.mEncodedLength = (IccUtils.byteNumForUnsignedInt(this.mTag) + calculateEncodedBytesNumForLength(this.mDataLength)) + this.mDataLength;
    }

    public int getTag() {
        return this.mTag;
    }

    public boolean isConstructed() {
        return this.mConstructed;
    }

    public boolean hasChild(int tag, int... tags) throws InvalidAsn1DataException {
        try {
            getChild(tag, tags);
            return true;
        } catch (TagNotFoundException e) {
            return false;
        }
    }

    public Asn1Node getChild(int tag, int... tags) throws TagNotFoundException, InvalidAsn1DataException {
        if (this.mConstructed) {
            int index = 0;
            int tag2 = tag;
            tag = this;
            while (tag != 0) {
                int i;
                List<Asn1Node> children = tag.getChildren();
                int size = children.size();
                Asn1Node foundChild = null;
                for (i = 0; i < size; i++) {
                    Asn1Node child = (Asn1Node) children.get(i);
                    if (child.getTag() == tag2) {
                        foundChild = child;
                        break;
                    }
                }
                tag = foundChild;
                if (index >= tags.length) {
                    break;
                }
                i = index + 1;
                tag2 = tags[index];
                index = i;
            }
            if (tag != 0) {
                return tag;
            }
            throw new TagNotFoundException(tag2);
        }
        throw new TagNotFoundException(tag);
    }

    public List<Asn1Node> getChildren(int tag) throws TagNotFoundException, InvalidAsn1DataException {
        if (!this.mConstructed) {
            return EMPTY_NODE_LIST;
        }
        List<Asn1Node> children = getChildren();
        if (children.isEmpty()) {
            return EMPTY_NODE_LIST;
        }
        List<Asn1Node> output = new ArrayList();
        int size = children.size();
        for (int i = 0; i < size; i++) {
            Asn1Node child = (Asn1Node) children.get(i);
            if (child.getTag() == tag) {
                output.add(child);
            }
        }
        return output.isEmpty() ? EMPTY_NODE_LIST : output;
    }

    public List<Asn1Node> getChildren() throws InvalidAsn1DataException {
        if (!this.mConstructed) {
            return EMPTY_NODE_LIST;
        }
        if (this.mDataBytes != null) {
            Asn1Decoder subDecoder = new Asn1Decoder(this.mDataBytes, this.mDataOffset, this.mDataLength);
            while (subDecoder.hasNextNode()) {
                this.mChildren.add(subDecoder.nextNode());
            }
            this.mDataBytes = null;
            this.mDataOffset = 0;
        }
        return this.mChildren;
    }

    public boolean hasValue() {
        return (this.mConstructed || this.mDataBytes == null) ? false : true;
    }

    public int asInteger() throws InvalidAsn1DataException {
        if (this.mConstructed) {
            throw new IllegalStateException("Cannot get value of a constructed node.");
        } else if (this.mDataBytes != null) {
            try {
                return IccUtils.bytesToInt(this.mDataBytes, this.mDataOffset, this.mDataLength);
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                throw new InvalidAsn1DataException(this.mTag, "Cannot parse data bytes.", e);
            }
        } else {
            throw new InvalidAsn1DataException(this.mTag, "Data bytes cannot be null.");
        }
    }

    public long asRawLong() throws InvalidAsn1DataException {
        if (this.mConstructed) {
            throw new IllegalStateException("Cannot get value of a constructed node.");
        } else if (this.mDataBytes != null) {
            try {
                return IccUtils.bytesToRawLong(this.mDataBytes, this.mDataOffset, this.mDataLength);
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                throw new InvalidAsn1DataException(this.mTag, "Cannot parse data bytes.", e);
            }
        } else {
            throw new InvalidAsn1DataException(this.mTag, "Data bytes cannot be null.");
        }
    }

    public String asString() throws InvalidAsn1DataException {
        if (this.mConstructed) {
            throw new IllegalStateException("Cannot get value of a constructed node.");
        } else if (this.mDataBytes != null) {
            try {
                return new String(this.mDataBytes, this.mDataOffset, this.mDataLength, StandardCharsets.UTF_8);
            } catch (IndexOutOfBoundsException e) {
                throw new InvalidAsn1DataException(this.mTag, "Cannot parse data bytes.", e);
            }
        } else {
            throw new InvalidAsn1DataException(this.mTag, "Data bytes cannot be null.");
        }
    }

    public byte[] asBytes() throws InvalidAsn1DataException {
        if (this.mConstructed) {
            throw new IllegalStateException("Cannot get value of a constructed node.");
        } else if (this.mDataBytes != null) {
            byte[] output = new byte[this.mDataLength];
            try {
                System.arraycopy(this.mDataBytes, this.mDataOffset, output, 0, this.mDataLength);
                return output;
            } catch (IndexOutOfBoundsException e) {
                throw new InvalidAsn1DataException(this.mTag, "Cannot parse data bytes.", e);
            }
        } else {
            throw new InvalidAsn1DataException(this.mTag, "Data bytes cannot be null.");
        }
    }

    public int asBits() throws InvalidAsn1DataException {
        if (this.mConstructed) {
            throw new IllegalStateException("Cannot get value of a constructed node.");
        } else if (this.mDataBytes != null) {
            try {
                int bits = IccUtils.bytesToInt(this.mDataBytes, this.mDataOffset + 1, this.mDataLength - 1);
                for (int i = this.mDataLength - 1; i < 4; i++) {
                    bits <<= 8;
                }
                return Integer.reverse(bits);
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                throw new InvalidAsn1DataException(this.mTag, "Cannot parse data bytes.", e);
            }
        } else {
            throw new InvalidAsn1DataException(this.mTag, "Data bytes cannot be null.");
        }
    }

    public boolean asBoolean() throws InvalidAsn1DataException {
        int i;
        StringBuilder stringBuilder;
        if (this.mConstructed) {
            throw new IllegalStateException("Cannot get value of a constructed node.");
        } else if (this.mDataBytes == null) {
            throw new InvalidAsn1DataException(this.mTag, "Data bytes cannot be null.");
        } else if (this.mDataLength != 1) {
            i = this.mTag;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot parse data bytes as boolean: length=");
            stringBuilder.append(this.mDataLength);
            throw new InvalidAsn1DataException(i, stringBuilder.toString());
        } else if (this.mDataOffset < 0 || this.mDataOffset >= this.mDataBytes.length) {
            throw new InvalidAsn1DataException(this.mTag, "Cannot parse data bytes.", new ArrayIndexOutOfBoundsException(this.mDataOffset));
        } else if (this.mDataBytes[this.mDataOffset] == (byte) -1) {
            return Boolean.TRUE.booleanValue();
        } else {
            if (this.mDataBytes[this.mDataOffset] == (byte) 0) {
                return Boolean.FALSE.booleanValue();
            }
            i = this.mTag;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot parse data bytes as boolean: ");
            stringBuilder.append(this.mDataBytes[this.mDataOffset]);
            throw new InvalidAsn1DataException(i, stringBuilder.toString());
        }
    }

    public int getEncodedLength() {
        return this.mEncodedLength;
    }

    public int getDataLength() {
        return this.mDataLength;
    }

    public void writeToBytes(byte[] dest, int offset) {
        if (offset < 0 || this.mEncodedLength + offset > dest.length) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Not enough space to write. Required bytes: ");
            stringBuilder.append(this.mEncodedLength);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        }
        write(dest, offset);
    }

    public byte[] toBytes() {
        byte[] dest = new byte[this.mEncodedLength];
        write(dest, 0);
        return dest;
    }

    public String toHex() {
        return IccUtils.bytesToHexString(toBytes());
    }

    public String getHeadAsHex() {
        String headHex = IccUtils.bytesToHexString(IccUtils.unsignedIntToBytes(this.mTag));
        if (this.mDataLength <= 127) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(headHex);
            stringBuilder.append(IccUtils.byteToHex((byte) this.mDataLength));
            return stringBuilder.toString();
        }
        byte[] lenBytes = IccUtils.unsignedIntToBytes(this.mDataLength);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(headHex);
        stringBuilder2.append(IccUtils.byteToHex((byte) (lenBytes.length | 128)));
        headHex = stringBuilder2.toString();
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(headHex);
        stringBuilder2.append(IccUtils.bytesToHexString(lenBytes));
        return stringBuilder2.toString();
    }

    private int write(byte[] dest, int offset) {
        int offset2;
        offset += IccUtils.unsignedIntToBytes(this.mTag, dest, offset);
        if (this.mDataLength <= 127) {
            offset2 = offset + 1;
            dest[offset] = (byte) this.mDataLength;
        } else {
            offset++;
            offset2 = IccUtils.unsignedIntToBytes(this.mDataLength, dest, offset);
            dest[offset - 1] = (byte) (offset2 | 128);
            offset2 += offset;
        }
        if (this.mConstructed && this.mDataBytes == null) {
            offset = this.mChildren.size();
            for (int i = 0; i < offset; i++) {
                offset2 = ((Asn1Node) this.mChildren.get(i)).write(dest, offset2);
            }
            return offset2;
        } else if (this.mDataBytes == null) {
            return offset2;
        } else {
            System.arraycopy(this.mDataBytes, this.mDataOffset, dest, offset2, this.mDataLength);
            return offset2 + this.mDataLength;
        }
    }
}
