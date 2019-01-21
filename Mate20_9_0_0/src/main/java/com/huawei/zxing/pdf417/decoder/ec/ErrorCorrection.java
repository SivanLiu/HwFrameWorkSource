package com.huawei.zxing.pdf417.decoder.ec;

import com.huawei.zxing.ChecksumException;

public final class ErrorCorrection {
    private final ModulusGF field = ModulusGF.PDF417_GF;

    public int decode(int[] received, int numECCodewords, int[] erasures) throws ChecksumException {
        int i;
        int eval;
        ErrorCorrection errorCorrection = this;
        int[] iArr = received;
        int i2 = numECCodewords;
        int[] iArr2 = erasures;
        ModulusPoly poly = new ModulusPoly(errorCorrection.field, iArr);
        int[] S = new int[i2];
        boolean error = false;
        for (i = i2; i > 0; i--) {
            eval = poly.evaluateAt(errorCorrection.field.exp(i));
            S[i2 - i] = eval;
            if (eval != 0) {
                error = true;
            }
        }
        i = 0;
        if (!error) {
            return 0;
        }
        ModulusPoly knownErrors = errorCorrection.field.getOne();
        int length = iArr2.length;
        ModulusPoly knownErrors2 = knownErrors;
        eval = 0;
        while (eval < length) {
            int b = errorCorrection.field.exp((iArr.length - 1) - iArr2[eval]);
            knownErrors2 = knownErrors2.multiply(new ModulusPoly(errorCorrection.field, new int[]{errorCorrection.field.subtract(0, b), 1}));
            eval++;
            iArr2 = erasures;
        }
        ModulusPoly[] sigmaOmega = errorCorrection.runEuclideanAlgorithm(errorCorrection.field.buildMonomial(i2, 1), new ModulusPoly(errorCorrection.field, S), i2);
        ModulusPoly sigma = sigmaOmega[0];
        ModulusPoly omega = sigmaOmega[1];
        int[] errorLocations = errorCorrection.findErrorLocations(sigma);
        int[] errorMagnitudes = errorCorrection.findErrorMagnitudes(omega, sigma, errorLocations);
        while (i < errorLocations.length) {
            int position = (iArr.length - 1) - errorCorrection.field.log(errorLocations[i]);
            if (position >= 0) {
                iArr[position] = errorCorrection.field.subtract(iArr[position], errorMagnitudes[i]);
                i++;
                errorCorrection = this;
                i2 = numECCodewords;
            } else {
                throw ChecksumException.getChecksumInstance();
            }
        }
        return errorLocations.length;
    }

    private ModulusPoly[] runEuclideanAlgorithm(ModulusPoly a, ModulusPoly b, int R) throws ChecksumException {
        ModulusPoly temp;
        if (a.getDegree() < b.getDegree()) {
            temp = a;
            a = b;
            b = temp;
        }
        temp = a;
        ModulusPoly r = b;
        ModulusPoly tLast = this.field.getZero();
        ModulusPoly t = this.field.getOne();
        while (r.getDegree() >= R / 2) {
            ModulusPoly rLastLast = temp;
            ModulusPoly tLastLast = tLast;
            temp = r;
            tLast = t;
            if (temp.isZero()) {
                throw ChecksumException.getChecksumInstance();
            }
            r = rLastLast;
            ModulusPoly q = this.field.getZero();
            int dltInverse = this.field.inverse(temp.getCoefficient(temp.getDegree()));
            while (r.getDegree() >= temp.getDegree() && !r.isZero()) {
                int degreeDiff = r.getDegree() - temp.getDegree();
                int scale = this.field.multiply(r.getCoefficient(r.getDegree()), dltInverse);
                q = q.add(this.field.buildMonomial(degreeDiff, scale));
                r = r.subtract(temp.multiplyByMonomial(degreeDiff, scale));
            }
            t = q.multiply(tLast).subtract(tLastLast).negative();
        }
        int sigmaTildeAtZero = t.getCoefficient(0);
        if (sigmaTildeAtZero != 0) {
            int inverse = this.field.inverse(sigmaTildeAtZero);
            ModulusPoly sigma = t.multiply(inverse);
            ModulusPoly omega = r.multiply(inverse);
            return new ModulusPoly[]{sigma, omega};
        }
        throw ChecksumException.getChecksumInstance();
    }

    private int[] findErrorLocations(ModulusPoly errorLocator) throws ChecksumException {
        int numErrors = errorLocator.getDegree();
        int[] result = new int[numErrors];
        int e = 0;
        for (int i = 1; i < this.field.getSize() && e < numErrors; i++) {
            if (errorLocator.evaluateAt(i) == 0) {
                result[e] = this.field.inverse(i);
                e++;
            }
        }
        if (e == numErrors) {
            return result;
        }
        throw ChecksumException.getChecksumInstance();
    }

    private int[] findErrorMagnitudes(ModulusPoly errorEvaluator, ModulusPoly errorLocator, int[] errorLocations) {
        int errorLocatorDegree = errorLocator.getDegree();
        int[] formalDerivativeCoefficients = new int[errorLocatorDegree];
        for (int i = 1; i <= errorLocatorDegree; i++) {
            formalDerivativeCoefficients[errorLocatorDegree - i] = this.field.multiply(i, errorLocator.getCoefficient(i));
        }
        ModulusPoly formalDerivative = new ModulusPoly(this.field, formalDerivativeCoefficients);
        int s = errorLocations.length;
        int[] result = new int[s];
        for (int i2 = 0; i2 < s; i2++) {
            int xiInverse = this.field.inverse(errorLocations[i2]);
            result[i2] = this.field.multiply(this.field.subtract(0, errorEvaluator.evaluateAt(xiInverse)), this.field.inverse(formalDerivative.evaluateAt(xiInverse)));
        }
        return result;
    }
}
