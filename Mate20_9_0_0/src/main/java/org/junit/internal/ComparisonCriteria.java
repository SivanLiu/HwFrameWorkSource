package org.junit.internal;

import java.lang.reflect.Array;
import java.util.Arrays;
import org.junit.Assert;

public abstract class ComparisonCriteria {
    protected abstract void assertElementsEqual(Object obj, Object obj2);

    public void arrayEquals(String message, Object expecteds, Object actuals) throws ArrayComparisonFailure {
        if (expecteds != actuals) {
            Object[] objArr = new Object[1];
            int i = 0;
            objArr[0] = expecteds;
            if (!Arrays.deepEquals(objArr, new Object[]{actuals})) {
                String header;
                if (message == null) {
                    header = "";
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(message);
                    stringBuilder.append(": ");
                    header = stringBuilder.toString();
                }
                int expectedsLength = assertArraysAreSameLength(expecteds, actuals, header);
                while (i < expectedsLength) {
                    Object expected = Array.get(expecteds, i);
                    Object actual = Array.get(actuals, i);
                    if (isArray(expected) && isArray(actual)) {
                        try {
                            arrayEquals(message, expected, actual);
                        } catch (ArrayComparisonFailure e) {
                            e.addDimension(i);
                            throw e;
                        }
                    }
                    try {
                        assertElementsEqual(expected, actual);
                    } catch (AssertionError e2) {
                        throw new ArrayComparisonFailure(header, e2, i);
                    }
                    i++;
                }
            }
        }
    }

    private boolean isArray(Object expected) {
        return expected != null && expected.getClass().isArray();
    }

    private int assertArraysAreSameLength(Object expecteds, Object actuals, String header) {
        StringBuilder stringBuilder;
        if (expecteds == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(header);
            stringBuilder.append("expected array was null");
            Assert.fail(stringBuilder.toString());
        }
        if (actuals == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(header);
            stringBuilder.append("actual array was null");
            Assert.fail(stringBuilder.toString());
        }
        int actualsLength = Array.getLength(actuals);
        int expectedsLength = Array.getLength(expecteds);
        if (actualsLength != expectedsLength) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(header);
            stringBuilder2.append("array lengths differed, expected.length=");
            stringBuilder2.append(expectedsLength);
            stringBuilder2.append(" actual.length=");
            stringBuilder2.append(actualsLength);
            Assert.fail(stringBuilder2.toString());
        }
        return expectedsLength;
    }
}
