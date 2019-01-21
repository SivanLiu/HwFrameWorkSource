package java.time.chrono;

import java.time.DateTimeException;

public enum IsoEra implements Era {
    BCE,
    CE;

    public static IsoEra of(int isoEra) {
        switch (isoEra) {
            case 0:
                return BCE;
            case 1:
                return CE;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid era: ");
                stringBuilder.append(isoEra);
                throw new DateTimeException(stringBuilder.toString());
        }
    }

    public int getValue() {
        return ordinal();
    }
}
