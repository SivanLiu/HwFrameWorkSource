package org.bouncycastle.math.ec.tools;

import java.io.PrintStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.TreeSet;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.util.Integers;

public class TraceOptimizer {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static final SecureRandom R = new SecureRandom();

    private static int calculateTrace(ECFieldElement eCFieldElement) {
        int fieldSize = eCFieldElement.getFieldSize();
        ECFieldElement eCFieldElement2 = eCFieldElement;
        ECFieldElement eCFieldElement3 = eCFieldElement2;
        for (int i = 1; i < fieldSize; i++) {
            eCFieldElement2 = eCFieldElement2.square();
            eCFieldElement3 = eCFieldElement3.add(eCFieldElement2);
        }
        BigInteger toBigInteger = eCFieldElement3.toBigInteger();
        if (toBigInteger.bitLength() <= 1) {
            return toBigInteger.intValue();
        }
        throw new IllegalStateException();
    }

    private static ArrayList enumToList(Enumeration enumeration) {
        ArrayList arrayList = new ArrayList();
        while (enumeration.hasMoreElements()) {
            arrayList.add(enumeration.nextElement());
        }
        return arrayList;
    }

    public static void implPrintNonZeroTraceBits(X9ECParameters x9ECParameters) {
        int i;
        ECCurve curve = x9ECParameters.getCurve();
        int fieldSize = curve.getFieldSize();
        ArrayList arrayList = new ArrayList();
        for (i = 0; i < fieldSize; i++) {
            if (calculateTrace(curve.fromBigInteger(ONE.shiftLeft(i))) != 0) {
                arrayList.add(Integers.valueOf(i));
                PrintStream printStream = System.out;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" ");
                stringBuilder.append(i);
                printStream.print(stringBuilder.toString());
            }
        }
        System.out.println();
        i = 0;
        while (i < 1000) {
            BigInteger bigInteger = new BigInteger(fieldSize, R);
            int calculateTrace = calculateTrace(curve.fromBigInteger(bigInteger));
            int i2 = 0;
            int i3 = i2;
            while (i2 < arrayList.size()) {
                if (bigInteger.testBit(((Integer) arrayList.get(i2)).intValue())) {
                    i3 ^= 1;
                }
                i2++;
            }
            if (calculateTrace == i3) {
                i++;
            } else {
                throw new IllegalStateException("Optimized-trace sanity check failed");
            }
        }
    }

    public static void main(String[] strArr) {
        TreeSet<String> treeSet = new TreeSet(enumToList(ECNamedCurveTable.getNames()));
        treeSet.addAll(enumToList(CustomNamedCurves.getNames()));
        for (String str : treeSet) {
            X9ECParameters byName = CustomNamedCurves.getByName(str);
            if (byName == null) {
                byName = ECNamedCurveTable.getByName(str);
            }
            if (byName != null && ECAlgorithms.isF2mCurve(byName.getCurve())) {
                PrintStream printStream = System.out;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(":");
                printStream.print(stringBuilder.toString());
                implPrintNonZeroTraceBits(byName);
            }
        }
    }

    public static void printNonZeroTraceBits(X9ECParameters x9ECParameters) {
        if (ECAlgorithms.isF2mCurve(x9ECParameters.getCurve())) {
            implPrintNonZeroTraceBits(x9ECParameters);
            return;
        }
        throw new IllegalArgumentException("Trace only defined over characteristic-2 fields");
    }
}
