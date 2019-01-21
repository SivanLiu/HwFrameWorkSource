package sun.security.pkcs;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Hashtable;
import sun.security.util.DerEncoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public class PKCS9Attributes {
    private final Hashtable<ObjectIdentifier, PKCS9Attribute> attributes;
    private final byte[] derEncoding;
    private boolean ignoreUnsupportedAttributes;
    private final Hashtable<ObjectIdentifier, ObjectIdentifier> permittedAttributes;

    public PKCS9Attributes(ObjectIdentifier[] permittedAttributes, DerInputStream in) throws IOException {
        this.attributes = new Hashtable(3);
        int i = 0;
        this.ignoreUnsupportedAttributes = false;
        if (permittedAttributes != null) {
            this.permittedAttributes = new Hashtable(permittedAttributes.length);
            while (i < permittedAttributes.length) {
                this.permittedAttributes.put(permittedAttributes[i], permittedAttributes[i]);
                i++;
            }
        } else {
            this.permittedAttributes = null;
        }
        this.derEncoding = decode(in);
    }

    public PKCS9Attributes(DerInputStream in) throws IOException {
        this(in, false);
    }

    public PKCS9Attributes(DerInputStream in, boolean ignoreUnsupportedAttributes) throws IOException {
        this.attributes = new Hashtable(3);
        this.ignoreUnsupportedAttributes = false;
        this.ignoreUnsupportedAttributes = ignoreUnsupportedAttributes;
        this.derEncoding = decode(in);
        this.permittedAttributes = null;
    }

    public PKCS9Attributes(PKCS9Attribute[] attribs) throws IllegalArgumentException, IOException {
        this.attributes = new Hashtable(3);
        int i = 0;
        this.ignoreUnsupportedAttributes = false;
        while (i < attribs.length) {
            ObjectIdentifier oid = attribs[i].getOID();
            if (this.attributes.containsKey(oid)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("PKCSAttribute ");
                stringBuilder.append(attribs[i].getOID());
                stringBuilder.append(" duplicated while constructing PKCS9Attributes.");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            this.attributes.put(oid, attribs[i]);
            i++;
        }
        this.derEncoding = generateDerEncoding();
        this.permittedAttributes = null;
    }

    private byte[] decode(DerInputStream in) throws IOException {
        byte[] derEncoding = in.getDerValue().toByteArray();
        int i = 0;
        derEncoding[0] = (byte) 49;
        DerValue[] derVals = new DerInputStream(derEncoding).getSet(3, 1);
        boolean reuseEncoding = true;
        while (i < derVals.length) {
            try {
                PKCS9Attribute attrib = new PKCS9Attribute(derVals[i]);
                Object oid = attrib.getOID();
                StringBuilder stringBuilder;
                if (this.attributes.get(oid) != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Duplicate PKCS9 attribute: ");
                    stringBuilder.append(oid);
                    throw new IOException(stringBuilder.toString());
                } else if (this.permittedAttributes == null || this.permittedAttributes.containsKey(oid)) {
                    this.attributes.put(oid, attrib);
                    i++;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Attribute ");
                    stringBuilder.append(oid);
                    stringBuilder.append(" not permitted in this attribute set");
                    throw new IOException(stringBuilder.toString());
                }
            } catch (ParsingException e) {
                if (this.ignoreUnsupportedAttributes) {
                    reuseEncoding = false;
                } else {
                    throw e;
                }
            }
        }
        return reuseEncoding ? derEncoding : generateDerEncoding();
    }

    public void encode(byte tag, OutputStream out) throws IOException {
        out.write((int) tag);
        out.write(this.derEncoding, 1, this.derEncoding.length - 1);
    }

    private byte[] generateDerEncoding() throws IOException {
        DerOutputStream out = new DerOutputStream();
        out.putOrderedSetOf((byte) 49, castToDerEncoder(this.attributes.values().toArray()));
        return out.toByteArray();
    }

    public byte[] getDerEncoding() throws IOException {
        return (byte[]) this.derEncoding.clone();
    }

    public PKCS9Attribute getAttribute(ObjectIdentifier oid) {
        return (PKCS9Attribute) this.attributes.get(oid);
    }

    public PKCS9Attribute getAttribute(String name) {
        return (PKCS9Attribute) this.attributes.get(PKCS9Attribute.getOID(name));
    }

    public PKCS9Attribute[] getAttributes() {
        PKCS9Attribute[] attribs = new PKCS9Attribute[this.attributes.size()];
        int j = 0;
        for (int i = 1; i < PKCS9Attribute.PKCS9_OIDS.length && j < attribs.length; i++) {
            attribs[j] = getAttribute(PKCS9Attribute.PKCS9_OIDS[i]);
            if (attribs[j] != null) {
                j++;
            }
        }
        return attribs;
    }

    public Object getAttributeValue(ObjectIdentifier oid) throws IOException {
        try {
            return getAttribute(oid).getValue();
        } catch (NullPointerException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No value found for attribute ");
            stringBuilder.append((Object) oid);
            throw new IOException(stringBuilder.toString());
        }
    }

    public Object getAttributeValue(String name) throws IOException {
        ObjectIdentifier oid = PKCS9Attribute.getOID(name);
        if (oid != null) {
            return getAttributeValue(oid);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Attribute name ");
        stringBuilder.append(name);
        stringBuilder.append(" not recognized or not supported.");
        throw new IOException(stringBuilder.toString());
    }

    public String toString() {
        StringBuffer buf = new StringBuffer((int) HttpURLConnection.HTTP_OK);
        buf.append("PKCS9 Attributes: [\n\t");
        boolean first = true;
        for (int i = 1; i < PKCS9Attribute.PKCS9_OIDS.length; i++) {
            PKCS9Attribute value = getAttribute(PKCS9Attribute.PKCS9_OIDS[i]);
            if (value != null) {
                if (first) {
                    first = false;
                } else {
                    buf.append(";\n\t");
                }
                buf.append(value.toString());
            }
        }
        buf.append("\n\t] (end PKCS9 Attributes)");
        return buf.toString();
    }

    static DerEncoder[] castToDerEncoder(Object[] objs) {
        DerEncoder[] encoders = new DerEncoder[objs.length];
        for (int i = 0; i < encoders.length; i++) {
            encoders[i] = (DerEncoder) objs[i];
        }
        return encoders;
    }
}
