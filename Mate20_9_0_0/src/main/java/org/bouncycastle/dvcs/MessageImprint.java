package org.bouncycastle.dvcs;

import org.bouncycastle.asn1.x509.DigestInfo;

public class MessageImprint {
    private final DigestInfo messageImprint;

    public MessageImprint(DigestInfo digestInfo) {
        this.messageImprint = digestInfo;
    }

    public boolean equals(Object obj) {
        return obj == this ? true : obj instanceof MessageImprint ? this.messageImprint.equals(((MessageImprint) obj).messageImprint) : false;
    }

    public int hashCode() {
        return this.messageImprint.hashCode();
    }

    public DigestInfo toASN1Structure() {
        return this.messageImprint;
    }
}
