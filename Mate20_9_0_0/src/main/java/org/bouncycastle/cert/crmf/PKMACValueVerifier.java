package org.bouncycastle.cert.crmf;

import java.io.OutputStream;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.cmp.PBMParameter;
import org.bouncycastle.asn1.crmf.PKMACValue;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.operator.MacCalculator;
import org.bouncycastle.util.Arrays;

class PKMACValueVerifier {
    private final PKMACBuilder builder;

    public PKMACValueVerifier(PKMACBuilder pKMACBuilder) {
        this.builder = pKMACBuilder;
    }

    public boolean isValid(PKMACValue pKMACValue, char[] cArr, SubjectPublicKeyInfo subjectPublicKeyInfo) throws CRMFException {
        this.builder.setParameters(PBMParameter.getInstance(pKMACValue.getAlgId().getParameters()));
        MacCalculator build = this.builder.build(cArr);
        OutputStream outputStream = build.getOutputStream();
        try {
            outputStream.write(subjectPublicKeyInfo.getEncoded(ASN1Encoding.DER));
            outputStream.close();
            return Arrays.areEqual(build.getMac(), pKMACValue.getValue().getBytes());
        } catch (Throwable e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exception encoding mac input: ");
            stringBuilder.append(e.getMessage());
            throw new CRMFException(stringBuilder.toString(), e);
        }
    }
}
