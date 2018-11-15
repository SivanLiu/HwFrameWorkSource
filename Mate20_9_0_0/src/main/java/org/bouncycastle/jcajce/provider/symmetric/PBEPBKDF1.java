package org.bouncycastle.jcajce.provider.symmetric;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.spec.PBEParameterSpec;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.pkcs.PBEParameter;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseAlgorithmParameters;
import org.bouncycastle.jcajce.provider.util.AlgorithmProvider;

public class PBEPBKDF1 {

    public static class AlgParams extends BaseAlgorithmParameters {
        PBEParameter params;

        protected byte[] engineGetEncoded() {
            try {
                return this.params.getEncoded(ASN1Encoding.DER);
            } catch (IOException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Oooops! ");
                stringBuilder.append(e.toString());
                throw new RuntimeException(stringBuilder.toString());
            }
        }

        protected byte[] engineGetEncoded(String str) {
            return isASN1FormatString(str) ? engineGetEncoded() : null;
        }

        protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidParameterSpecException {
            if (algorithmParameterSpec instanceof PBEParameterSpec) {
                PBEParameterSpec pBEParameterSpec = (PBEParameterSpec) algorithmParameterSpec;
                this.params = new PBEParameter(pBEParameterSpec.getSalt(), pBEParameterSpec.getIterationCount());
                return;
            }
            throw new InvalidParameterSpecException("PBEParameterSpec required to initialise a PBKDF1 PBE parameters algorithm parameters object");
        }

        protected void engineInit(byte[] bArr) throws IOException {
            this.params = PBEParameter.getInstance(bArr);
        }

        protected void engineInit(byte[] bArr, String str) throws IOException {
            if (isASN1FormatString(str)) {
                engineInit(bArr);
                return;
            }
            throw new IOException("Unknown parameters format in PBKDF2 parameters object");
        }

        protected String engineToString() {
            return "PBKDF1 Parameters";
        }

        protected AlgorithmParameterSpec localEngineGetParameterSpec(Class cls) throws InvalidParameterSpecException {
            if (cls == PBEParameterSpec.class) {
                return new PBEParameterSpec(this.params.getSalt(), this.params.getIterationCount().intValue());
            }
            throw new InvalidParameterSpecException("unknown parameter spec passed to PBKDF1 PBE parameters object.");
        }
    }

    public static class Mappings extends AlgorithmProvider {
        private static final String PREFIX = PBEPBKDF1.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$AlgParams");
            configurableProvider.addAlgorithm("AlgorithmParameters.PBKDF1", stringBuilder.toString());
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.AlgorithmParameters.");
            stringBuilder2.append(PKCSObjectIdentifiers.pbeWithMD2AndDES_CBC);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "PBKDF1");
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.AlgorithmParameters.");
            stringBuilder2.append(PKCSObjectIdentifiers.pbeWithMD5AndDES_CBC);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "PBKDF1");
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.AlgorithmParameters.");
            stringBuilder2.append(PKCSObjectIdentifiers.pbeWithMD5AndRC2_CBC);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "PBKDF1");
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.AlgorithmParameters.");
            stringBuilder2.append(PKCSObjectIdentifiers.pbeWithSHA1AndDES_CBC);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "PBKDF1");
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.AlgorithmParameters.");
            stringBuilder2.append(PKCSObjectIdentifiers.pbeWithSHA1AndRC2_CBC);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "PBKDF1");
        }
    }

    private PBEPBKDF1() {
    }
}
