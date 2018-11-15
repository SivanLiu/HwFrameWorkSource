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

    /* JADX WARNING: Removed duplicated region for block: B:13:0x0027  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int read() throws IOException {
        ASN1OctetStringParser aSN1OctetStringParser;
        if (this._currentStream == null) {
            if (!this._first) {
                return -1;
            }
            aSN1OctetStringParser = (ASN1OctetStringParser) this._parser.readObject();
            if (aSN1OctetStringParser == null) {
                return -1;
            }
            this._first = false;
            this._currentStream = aSN1OctetStringParser.getOctetStream();
        }
        int read = this._currentStream.read();
        if (read < 0) {
            return read;
        }
        aSN1OctetStringParser = (ASN1OctetStringParser) this._parser.readObject();
        if (aSN1OctetStringParser == null) {
            this._currentStream = null;
            return -1;
        }
        this._currentStream = aSN1OctetStringParser.getOctetStream();
        int read2 = this._currentStream.read();
        if (read2 < 0) {
        }
        return read2;
    }

    /* JADX WARNING: Removed duplicated region for block: B:21:0x002e A:{SYNTHETIC} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int read(byte[] bArr, int i, int i2) throws IOException {
        ASN1OctetStringParser aSN1OctetStringParser;
        int i3 = 0;
        if (this._currentStream == null) {
            if (!this._first) {
                return -1;
            }
            aSN1OctetStringParser = (ASN1OctetStringParser) this._parser.readObject();
            if (aSN1OctetStringParser == null) {
                return -1;
            }
            this._first = false;
            this._currentStream = aSN1OctetStringParser.getOctetStream();
        }
        do {
            int read = this._currentStream.read(bArr, i + i3, i2 - i3);
            if (read < 0) {
                i3 += read;
            } else {
                aSN1OctetStringParser = (ASN1OctetStringParser) this._parser.readObject();
                if (aSN1OctetStringParser == null) {
                    this._currentStream = null;
                    if (i3 < 1) {
                        i3 = -1;
                    }
                    return i3;
                }
                this._currentStream = aSN1OctetStringParser.getOctetStream();
                int read2 = this._currentStream.read(bArr, i + i3, i2 - i3);
                if (read2 < 0) {
                }
            }
            i3 += read2;
        } while (i3 != i2);
        return i3;
    }
}
