package com.android.org.conscrypt;

import java.io.IOException;
import java.security.AlgorithmParametersSpi;
import java.security.InvalidAlgorithmParameterException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidParameterSpecException;

public class ECParameters extends AlgorithmParametersSpi {
    private OpenSSLECGroupContext curve;

    protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidParameterSpecException {
        OpenSSLECGroupContext newCurve;
        StringBuilder stringBuilder;
        if (algorithmParameterSpec instanceof ECGenParameterSpec) {
            String newCurveName = ((ECGenParameterSpec) algorithmParameterSpec).getName();
            newCurve = OpenSSLECGroupContext.getCurveByName(newCurveName);
            if (newCurve != null) {
                this.curve = newCurve;
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown EC curve name: ");
            stringBuilder.append(newCurveName);
            throw new InvalidParameterSpecException(stringBuilder.toString());
        } else if (algorithmParameterSpec instanceof ECParameterSpec) {
            ECParameterSpec ecParamSpec = (ECParameterSpec) algorithmParameterSpec;
            try {
                newCurve = OpenSSLECGroupContext.getInstance(ecParamSpec);
                if (newCurve != null) {
                    this.curve = newCurve;
                    return;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown EC curve: ");
                stringBuilder.append(ecParamSpec);
                throw new InvalidParameterSpecException(stringBuilder.toString());
            } catch (InvalidAlgorithmParameterException e) {
                throw new InvalidParameterSpecException(e.getMessage());
            }
        } else {
            throw new InvalidParameterSpecException("Only ECParameterSpec and ECGenParameterSpec are supported");
        }
    }

    protected void engineInit(byte[] bytes) throws IOException {
        long ref = NativeCrypto.EC_KEY_parse_curve_name(bytes);
        if (ref != 0) {
            this.curve = new OpenSSLECGroupContext(new EC_GROUP(ref));
            return;
        }
        throw new IOException("Error reading ASN.1 encoding");
    }

    protected void engineInit(byte[] bytes, String format) throws IOException {
        if (format == null || format.equals("ASN.1")) {
            engineInit(bytes);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported format: ");
        stringBuilder.append(format);
        throw new IOException(stringBuilder.toString());
    }

    protected <T extends AlgorithmParameterSpec> T engineGetParameterSpec(Class<T> aClass) throws InvalidParameterSpecException {
        if (aClass == ECParameterSpec.class) {
            return this.curve.getECParameterSpec();
        }
        if (aClass == ECGenParameterSpec.class) {
            return new ECGenParameterSpec(Platform.getCurveName(this.curve.getECParameterSpec()));
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported class: ");
        stringBuilder.append(aClass);
        throw new InvalidParameterSpecException(stringBuilder.toString());
    }

    protected byte[] engineGetEncoded() throws IOException {
        return NativeCrypto.EC_KEY_marshal_curve_name(this.curve.getNativeRef());
    }

    protected byte[] engineGetEncoded(String format) throws IOException {
        if (format == null || format.equals("ASN.1")) {
            return engineGetEncoded();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported format: ");
        stringBuilder.append(format);
        throw new IOException(stringBuilder.toString());
    }

    protected String engineToString() {
        return "Conscrypt EC AlgorithmParameters";
    }
}
