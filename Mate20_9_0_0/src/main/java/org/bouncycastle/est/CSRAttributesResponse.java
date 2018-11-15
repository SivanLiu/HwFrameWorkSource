package org.bouncycastle.est;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.est.AttrOrOID;
import org.bouncycastle.asn1.est.CsrAttrs;
import org.bouncycastle.util.Encodable;

public class CSRAttributesResponse implements Encodable {
    private final CsrAttrs csrAttrs;
    private final HashMap<ASN1ObjectIdentifier, AttrOrOID> index;

    public CSRAttributesResponse(CsrAttrs csrAttrs) throws ESTException {
        this.csrAttrs = csrAttrs;
        this.index = new HashMap(csrAttrs.size());
        AttrOrOID[] attrOrOIDs = csrAttrs.getAttrOrOIDs();
        for (int i = 0; i != attrOrOIDs.length; i++) {
            HashMap hashMap;
            Object oid;
            AttrOrOID attrOrOID = attrOrOIDs[i];
            if (attrOrOID.isOid()) {
                hashMap = this.index;
                oid = attrOrOID.getOid();
            } else {
                hashMap = this.index;
                oid = attrOrOID.getAttribute().getAttrType();
            }
            hashMap.put(oid, attrOrOID);
        }
    }

    public CSRAttributesResponse(byte[] bArr) throws ESTException {
        this(parseBytes(bArr));
    }

    private static CsrAttrs parseBytes(byte[] bArr) throws ESTException {
        try {
            return CsrAttrs.getInstance(ASN1Primitive.fromByteArray(bArr));
        } catch (Throwable e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("malformed data: ");
            stringBuilder.append(e.getMessage());
            throw new ESTException(stringBuilder.toString(), e);
        }
    }

    public byte[] getEncoded() throws IOException {
        return this.csrAttrs.getEncoded();
    }

    public Collection<ASN1ObjectIdentifier> getRequirements() {
        return this.index.keySet();
    }

    public boolean hasRequirement(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        return this.index.containsKey(aSN1ObjectIdentifier);
    }

    public boolean isAttribute(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        return this.index.containsKey(aSN1ObjectIdentifier) ? ((AttrOrOID) this.index.get(aSN1ObjectIdentifier)).isOid() ^ 1 : false;
    }

    public boolean isEmpty() {
        return this.csrAttrs.size() == 0;
    }
}
