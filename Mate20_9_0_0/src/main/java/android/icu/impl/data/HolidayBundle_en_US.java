package android.icu.impl.data;

import android.icu.util.EasterHoliday;
import android.icu.util.Holiday;
import android.icu.util.SimpleHoliday;
import java.util.ListResourceBundle;

public class HolidayBundle_en_US extends ListResourceBundle {
    private static final Object[][] fContents;
    private static final Holiday[] fHolidays = new Holiday[]{SimpleHoliday.NEW_YEARS_DAY, new SimpleHoliday(0, 15, 2, "Martin Luther King Day", 1986), new SimpleHoliday(1, 15, 2, "Presidents' Day", 1976), new SimpleHoliday(1, 22, "Washington's Birthday", 1776, 1975), EasterHoliday.GOOD_FRIDAY, EasterHoliday.EASTER_SUNDAY, new SimpleHoliday(4, 8, 1, "Mother's Day", 1914), new SimpleHoliday(4, 31, -2, "Memorial Day", 1971), new SimpleHoliday(4, 30, "Memorial Day", 1868, 1970), new SimpleHoliday(5, 15, 1, "Father's Day", 1956), new SimpleHoliday(6, 4, "Independence Day", 1776), new SimpleHoliday(8, 1, 2, "Labor Day", 1894), new SimpleHoliday(10, 2, 3, "Election Day"), new SimpleHoliday(9, 8, 2, "Columbus Day", 1971), new SimpleHoliday(9, 31, "Halloween"), new SimpleHoliday(10, 11, "Veterans' Day", 1918), new SimpleHoliday(10, 22, 5, "Thanksgiving", 1863), SimpleHoliday.CHRISTMAS};

    static {
        Object[][] objArr = new Object[1][];
        objArr[0] = new Object[]{"holidays", fHolidays};
        fContents = objArr;
    }

    public synchronized Object[][] getContents() {
        return fContents;
    }
}
