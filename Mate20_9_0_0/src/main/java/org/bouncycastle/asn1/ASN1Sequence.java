package org.bouncycastle.asn1;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Iterable;

public abstract class ASN1Sequence extends ASN1Primitive implements Iterable<ASN1Encodable> {
    protected Vector seq = new Vector();

    protected ASN1Sequence() {
    }

    protected ASN1Sequence(ASN1Encodable aSN1Encodable) {
        this.seq.addElement(aSN1Encodable);
    }

    protected ASN1Sequence(ASN1EncodableVector aSN1EncodableVector) {
        for (int i = 0; i != aSN1EncodableVector.size(); i++) {
            this.seq.addElement(aSN1EncodableVector.get(i));
        }
    }

    protected ASN1Sequence(ASN1Encodable[] aSN1EncodableArr) {
        for (int i = 0; i != aSN1EncodableArr.length; i++) {
            this.seq.addElement(aSN1EncodableArr[i]);
        }
    }

    public static ASN1Sequence getInstance(Object obj) {
        StringBuilder stringBuilder;
        if (obj == null || (obj instanceof ASN1Sequence)) {
            return (ASN1Sequence) obj;
        }
        if (obj instanceof ASN1SequenceParser) {
            return getInstance(((ASN1SequenceParser) obj).toASN1Primitive());
        }
        if (obj instanceof byte[]) {
            try {
                return getInstance(ASN1Primitive.fromByteArray((byte[]) obj));
            } catch (IOException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("failed to construct sequence from byte[]: ");
                stringBuilder.append(e.getMessage());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        if (obj instanceof ASN1Encodable) {
            ASN1Primitive toASN1Primitive = ((ASN1Encodable) obj).toASN1Primitive();
            if (toASN1Primitive instanceof ASN1Sequence) {
                return (ASN1Sequence) toASN1Primitive;
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("unknown object in getInstance: ");
        stringBuilder.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static ASN1Sequence getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        if (!z) {
            ASN1Encodable object = aSN1TaggedObject.getObject();
            if (aSN1TaggedObject.isExplicit()) {
                return aSN1TaggedObject instanceof BERTaggedObject ? new BERSequence(object) : new DLSequence(object);
            } else {
                if (object instanceof ASN1Sequence) {
                    return (ASN1Sequence) object;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unknown object in getInstance: ");
                stringBuilder.append(aSN1TaggedObject.getClass().getName());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        } else if (aSN1TaggedObject.isExplicit()) {
            return getInstance(aSN1TaggedObject.getObject().toASN1Primitive());
        } else {
            throw new IllegalArgumentException("object implicit - explicit expected.");
        }
    }

    private ASN1Encodable getNext(Enumeration enumeration) {
        return (ASN1Encodable) enumeration.nextElement();
    }

    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (!(aSN1Primitive instanceof ASN1Sequence)) {
            return false;
        }
        ASN1Sequence aSN1Sequence = (ASN1Sequence) aSN1Primitive;
        if (size() != aSN1Sequence.size()) {
            return false;
        }
        Enumeration objects = getObjects();
        Enumeration objects2 = aSN1Sequence.getObjects();
        while (objects.hasMoreElements()) {
            ASN1Encodable next = getNext(objects);
            ASN1Encodable next2 = getNext(objects2);
            ASN1Primitive toASN1Primitive = next.toASN1Primitive();
            ASN1Primitive toASN1Primitive2 = next2.toASN1Primitive();
            if (toASN1Primitive != toASN1Primitive2) {
                if (!toASN1Primitive.equals(toASN1Primitive2)) {
                    return false;
                }
            }
        }
        return true;
    }

    abstract void encode(ASN1OutputStream aSN1OutputStream) throws IOException;

    public ASN1Encodable getObjectAt(int i) {
        return (ASN1Encodable) this.seq.elementAt(i);
    }

    public Enumeration getObjects() {
        return this.seq.elements();
    }

    public int hashCode() {
        Enumeration objects = getObjects();
        int size = size();
        while (objects.hasMoreElements()) {
            size = (size * 17) ^ getNext(objects).hashCode();
        }
        return size;
    }

    boolean isConstructed() {
        return true;
    }

    public Iterator<ASN1Encodable> iterator() {
        return new Arrays.Iterator(toArray());
    }

    public ASN1SequenceParser parser() {
        return new ASN1SequenceParser() {
            private int index;
            private final int max = ASN1Sequence.this.size();

            public ASN1Primitive getLoadedObject() {
                return this;
            }

            public ASN1Encodable readObject() throws IOException {
                if (this.index == this.max) {
                    return null;
                }
                ASN1Sequence aSN1Sequence = ASN1Sequence.this;
                int i = this.index;
                this.index = i + 1;
                ASN1Encodable objectAt = aSN1Sequence.getObjectAt(i);
                if (objectAt instanceof ASN1Sequence) {
                    return ((ASN1Sequence) objectAt).parser();
                }
                if (objectAt instanceof ASN1Set) {
                    objectAt = ((ASN1Set) objectAt).parser();
                }
                return objectAt;
            }

            public ASN1Primitive toASN1Primitive() {
                return this;
            }
        };
    }

    public int size() {
        return this.seq.size();
    }

    public ASN1Encodable[] toArray() {
        ASN1Encodable[] aSN1EncodableArr = new ASN1Encodable[size()];
        for (int i = 0; i != size(); i++) {
            aSN1EncodableArr[i] = getObjectAt(i);
        }
        return aSN1EncodableArr;
    }

    ASN1Primitive toDERObject() {
        ASN1Primitive dERSequence = new DERSequence();
        dERSequence.seq = this.seq;
        return dERSequence;
    }

    ASN1Primitive toDLObject() {
        ASN1Primitive dLSequence = new DLSequence();
        dLSequence.seq = this.seq;
        return dLSequence;
    }

    public String toString() {
        return this.seq.toString();
    }
}
