package java.time.chrono;

import java.time.DateTimeException;

public enum ThaiBuddhistEra implements Era {
    BEFORE_BE,
    BE;

    public static ThaiBuddhistEra of(int thaiBuddhistEra) {
        switch (thaiBuddhistEra) {
            case 0:
                return BEFORE_BE;
            case 1:
                return BE;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid era: ");
                stringBuilder.append(thaiBuddhistEra);
                throw new DateTimeException(stringBuilder.toString());
        }
    }

    public int getValue() {
        return ordinal();
    }
}
