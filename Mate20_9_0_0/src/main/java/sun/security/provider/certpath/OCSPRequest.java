package sun.security.provider.certpath;

import java.io.IOException;
import java.security.cert.Extension;
import java.util.Collections;
import java.util.List;
import sun.misc.HexDumpEncoder;
import sun.security.util.Debug;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

class OCSPRequest {
    private static final Debug debug = Debug.getInstance("certpath");
    private static final boolean dump;
    private final List<CertId> certIds;
    private final List<Extension> extensions;
    private byte[] nonce;

    static {
        boolean z = debug != null && Debug.isOn("ocsp");
        dump = z;
    }

    OCSPRequest(CertId certId) {
        this(Collections.singletonList(certId));
    }

    OCSPRequest(List<CertId> certIds) {
        this.certIds = certIds;
        this.extensions = Collections.emptyList();
    }

    OCSPRequest(List<CertId> certIds, List<Extension> extensions) {
        this.certIds = certIds;
        this.extensions = extensions;
    }

    byte[] encodeBytes() throws IOException {
        DerOutputStream extOut;
        DerOutputStream extsOut;
        DerOutputStream tmp = new DerOutputStream();
        DerOutputStream requestsOut = new DerOutputStream();
        for (CertId certId : this.certIds) {
            DerOutputStream certIdOut = new DerOutputStream();
            certId.encode(certIdOut);
            requestsOut.write((byte) 48, certIdOut);
        }
        tmp.write((byte) 48, requestsOut);
        if (!this.extensions.isEmpty()) {
            extOut = new DerOutputStream();
            for (Extension ext : this.extensions) {
                ext.encode(extOut);
                if (ext.getId().equals(OCSP.NONCE_EXTENSION_OID.toString())) {
                    this.nonce = ext.getValue();
                }
            }
            extsOut = new DerOutputStream();
            extsOut.write((byte) 48, extOut);
            tmp.write(DerValue.createTag(Byte.MIN_VALUE, true, (byte) 2), extsOut);
        }
        extOut = new DerOutputStream();
        extOut.write((byte) 48, tmp);
        extsOut = new DerOutputStream();
        extsOut.write((byte) 48, extOut);
        byte[] bytes = extsOut.toByteArray();
        if (dump) {
            HexDumpEncoder hexEnc = new HexDumpEncoder();
            Debug debug = debug;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("OCSPRequest bytes...\n\n");
            stringBuilder.append(hexEnc.encode(bytes));
            stringBuilder.append("\n");
            debug.println(stringBuilder.toString());
        }
        return bytes;
    }

    List<CertId> getCertIds() {
        return this.certIds;
    }

    byte[] getNonce() {
        return this.nonce;
    }
}
