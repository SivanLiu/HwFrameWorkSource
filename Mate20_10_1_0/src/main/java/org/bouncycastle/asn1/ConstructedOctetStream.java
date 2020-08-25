package org.bouncycastle.asn1;

import java.io.IOException;
import java.io.InputStream;

class ConstructedOctetStream extends InputStream {
    private InputStream _currentStream;
    private boolean _first = true;
    private final ASN1StreamParser _parser;

    ConstructedOctetStream(ASN1StreamParser aSN1StreamParser) {
        this._parser = aSN1StreamParser;
    }

    /* JADX WARNING: Removed duplicated region for block: B:16:0x0041  */
    @Override // java.io.InputStream
    public int read() throws IOException {
        ASN1OctetStringParser aSN1OctetStringParser;
        ASN1Encodable readObject;
        if (this._currentStream == null) {
            if (!this._first || (readObject = this._parser.readObject()) == null) {
                return -1;
            }
            if (readObject instanceof ASN1OctetStringParser) {
                aSN1OctetStringParser = (ASN1OctetStringParser) readObject;
                this._first = false;
                this._currentStream = aSN1OctetStringParser.getOctetStream();
            } else {
                throw new IOException("unknown object encountered: " + readObject.getClass());
            }
        }
        int read = this._currentStream.read();
        if (read < 0) {
            ASN1Encodable readObject2 = this._parser.readObject();
            if (readObject2 != null) {
                if (readObject2 instanceof ASN1OctetStringParser) {
                    aSN1OctetStringParser = (ASN1OctetStringParser) readObject2;
                    this._currentStream = aSN1OctetStringParser.getOctetStream();
                    int read2 = this._currentStream.read();
                    if (read2 < 0) {
                    }
                }
                throw new IOException("unknown object encountered: " + readObject2.getClass());
            }
            this._currentStream = null;
            return -1;
        }
        return read2;
    }

    /*  JADX ERROR: JadxOverflowException in pass: RegionMakerVisitor
        jadx.core.utils.exceptions.JadxOverflowException: Regions count limit reached
        	at jadx.core.utils.ErrorsCounter.addError(ErrorsCounter.java:57)
        	at jadx.core.utils.ErrorsCounter.error(ErrorsCounter.java:31)
        	at jadx.core.dex.attributes.nodes.NotificationAttrNode.addError(NotificationAttrNode.java:15)
        */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x004e A[EDGE_INSN: B:29:0x004e->B:19:0x004e ?: BREAK  , SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x004d A[SYNTHETIC] */
    @Override // java.io.InputStream
    public int read(byte[] r7, int r8, int r9) throws IOException {
        /*
            r6 = this;
            java.io.InputStream r0 = r6._currentStream
            java.lang.String r1 = "unknown object encountered: "
            r2 = 0
            r3 = -1
            if (r0 != 0) goto L_0x003e
            boolean r0 = r6._first
            if (r0 != 0) goto L_0x000d
            return r3
        L_0x000d:
            org.bouncycastle.asn1.ASN1StreamParser r0 = r6._parser
            org.bouncycastle.asn1.ASN1Encodable r0 = r0.readObject()
            if (r0 != 0) goto L_0x0016
            return r3
        L_0x0016:
            boolean r4 = r0 instanceof org.bouncycastle.asn1.ASN1OctetStringParser
            if (r4 == 0) goto L_0x0025
            org.bouncycastle.asn1.ASN1OctetStringParser r0 = (org.bouncycastle.asn1.ASN1OctetStringParser) r0
            r6._first = r2
        L_0x001e:
            java.io.InputStream r0 = r0.getOctetStream()
            r6._currentStream = r0
            goto L_0x003e
        L_0x0025:
            java.io.IOException r7 = new java.io.IOException
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            r8.append(r1)
            java.lang.Class r9 = r0.getClass()
            r8.append(r9)
            java.lang.String r8 = r8.toString()
            r7.<init>(r8)
            throw r7
        L_0x003e:
            java.io.InputStream r0 = r6._currentStream
            int r4 = r8 + r2
            int r5 = r9 - r2
            int r0 = r0.read(r7, r4, r5)
            if (r0 < 0) goto L_0x004e
            int r2 = r2 + r0
            if (r2 != r9) goto L_0x003e
            return r2
        L_0x004e:
            org.bouncycastle.asn1.ASN1StreamParser r0 = r6._parser
            org.bouncycastle.asn1.ASN1Encodable r0 = r0.readObject()
            if (r0 != 0) goto L_0x005e
            r7 = 0
            r6._currentStream = r7
            r7 = 1
            if (r2 >= r7) goto L_0x005d
            r2 = r3
        L_0x005d:
            return r2
        L_0x005e:
            boolean r4 = r0 instanceof org.bouncycastle.asn1.ASN1OctetStringParser
            if (r4 == 0) goto L_0x0065
            org.bouncycastle.asn1.ASN1OctetStringParser r0 = (org.bouncycastle.asn1.ASN1OctetStringParser) r0
            goto L_0x001e
        L_0x0065:
            java.io.IOException r7 = new java.io.IOException
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            r8.append(r1)
            java.lang.Class r9 = r0.getClass()
            r8.append(r9)
            java.lang.String r8 = r8.toString()
            r7.<init>(r8)
            throw r7
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.ConstructedOctetStream.read(byte[], int, int):int");
    }
}
