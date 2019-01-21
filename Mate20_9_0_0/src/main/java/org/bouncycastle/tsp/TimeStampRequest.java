package org.bouncycastle.tsp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.tsp.TimeStampReq;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;

public class TimeStampRequest {
    private static Set EMPTY_SET = Collections.unmodifiableSet(new HashSet());
    private Extensions extensions;
    private TimeStampReq req;

    public TimeStampRequest(InputStream inputStream) throws IOException {
        this(loadRequest(inputStream));
    }

    public TimeStampRequest(TimeStampReq timeStampReq) {
        this.req = timeStampReq;
        this.extensions = timeStampReq.getExtensions();
    }

    public TimeStampRequest(byte[] bArr) throws IOException {
        this(new ByteArrayInputStream(bArr));
    }

    private Set convert(Set set) {
        if (set == null) {
            return set;
        }
        HashSet hashSet = new HashSet(set.size());
        for (Object next : set) {
            if (next instanceof String) {
                hashSet.add(new ASN1ObjectIdentifier((String) next));
            } else {
                hashSet.add(next);
            }
        }
        return hashSet;
    }

    private static TimeStampReq loadRequest(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder;
        try {
            return TimeStampReq.getInstance(new ASN1InputStream(inputStream).readObject());
        } catch (ClassCastException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("malformed request: ");
            stringBuilder.append(e);
            throw new IOException(stringBuilder.toString());
        } catch (IllegalArgumentException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("malformed request: ");
            stringBuilder.append(e2);
            throw new IOException(stringBuilder.toString());
        }
    }

    public boolean getCertReq() {
        return this.req.getCertReq() != null ? this.req.getCertReq().isTrue() : false;
    }

    public Set getCriticalExtensionOIDs() {
        return this.extensions == null ? EMPTY_SET : Collections.unmodifiableSet(new HashSet(Arrays.asList(this.extensions.getCriticalExtensionOIDs())));
    }

    public byte[] getEncoded() throws IOException {
        return this.req.getEncoded();
    }

    public Extension getExtension(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        return this.extensions != null ? this.extensions.getExtension(aSN1ObjectIdentifier) : null;
    }

    public List getExtensionOIDs() {
        return TSPUtil.getExtensionOIDs(this.extensions);
    }

    Extensions getExtensions() {
        return this.extensions;
    }

    public ASN1ObjectIdentifier getMessageImprintAlgOID() {
        return this.req.getMessageImprint().getHashAlgorithm().getAlgorithm();
    }

    public byte[] getMessageImprintDigest() {
        return this.req.getMessageImprint().getHashedMessage();
    }

    public Set getNonCriticalExtensionOIDs() {
        return this.extensions == null ? EMPTY_SET : Collections.unmodifiableSet(new HashSet(Arrays.asList(this.extensions.getNonCriticalExtensionOIDs())));
    }

    public BigInteger getNonce() {
        return this.req.getNonce() != null ? this.req.getNonce().getValue() : null;
    }

    public ASN1ObjectIdentifier getReqPolicy() {
        return this.req.getReqPolicy() != null ? this.req.getReqPolicy() : null;
    }

    public int getVersion() {
        return this.req.getVersion().getValue().intValue();
    }

    public boolean hasExtensions() {
        return this.extensions != null;
    }

    public void validate(Set set, Set set2, Set set3) throws TSPException {
        set = convert(set);
        set2 = convert(set2);
        set3 = convert(set3);
        if (!set.contains(getMessageImprintAlgOID())) {
            throw new TSPValidationException("request contains unknown algorithm", 128);
        } else if (set2 == null || getReqPolicy() == null || set2.contains(getReqPolicy())) {
            if (!(getExtensions() == null || set3 == null)) {
                Enumeration oids = getExtensions().oids();
                while (oids.hasMoreElements()) {
                    if (!set3.contains((ASN1ObjectIdentifier) oids.nextElement())) {
                        throw new TSPValidationException("request contains unknown extension", 8388608);
                    }
                }
            }
            if (TSPUtil.getDigestLength(getMessageImprintAlgOID().getId()) != getMessageImprintDigest().length) {
                throw new TSPValidationException("imprint digest the wrong length", 4);
            }
        } else {
            throw new TSPValidationException("request contains unknown policy", 256);
        }
    }
}
