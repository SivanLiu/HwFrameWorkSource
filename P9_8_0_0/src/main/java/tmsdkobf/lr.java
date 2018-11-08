package tmsdkobf;

import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.CheckVersionField;
import java.util.Date;
import java.util.GregorianCalendar;

public class lr {
    public static GregorianCalendar a(GregorianCalendar gregorianCalendar) {
        gregorianCalendar.clear(11);
        gregorianCalendar.clear(10);
        gregorianCalendar.clear(12);
        gregorianCalendar.clear(13);
        gregorianCalendar.clear(14);
        return gregorianCalendar;
    }

    public static GregorianCalendar a(GregorianCalendar gregorianCalendar, int i) {
        Object -l_3_R = new GregorianCalendar();
        -l_3_R.setTimeInMillis(gregorianCalendar.getTimeInMillis());
        if (-l_3_R.get(5) != i) {
            if (-l_3_R.get(5) <= i) {
                int -l_4_I = -l_3_R.getActualMaximum(5);
                if (-l_4_I >= i || -l_3_R.get(5) != -l_4_I) {
                    -l_3_R.add(2, -1);
                }
                -l_3_R = b(-l_3_R, i);
            } else {
                -l_3_R.set(5, i);
            }
        }
        return a(-l_3_R);
    }

    public static boolean a(long j, long j2, int i) {
        return (((j - j2) > ((long) ((i * 60) * CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY)) ? 1 : ((j - j2) == ((long) ((i * 60) * CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY)) ? 0 : -1)) <= 0 ? 1 : 0) == 0 ? true : 0;
    }

    public static boolean a(Date date, Date date2) {
        Object -l_2_R = new GregorianCalendar();
        Object -l_3_R = new GregorianCalendar();
        -l_2_R.setTime(date);
        -l_2_R = a(-l_2_R);
        -l_3_R.setTime(date2);
        return -l_2_R.getTimeInMillis() == a(-l_3_R).getTimeInMillis();
    }

    private static GregorianCalendar b(GregorianCalendar gregorianCalendar, int i) {
        int -l_2_I = gregorianCalendar.getActualMaximum(5);
        if (-l_2_I < i) {
            gregorianCalendar.set(5, -l_2_I);
        } else {
            gregorianCalendar.set(5, i);
        }
        return gregorianCalendar;
    }
}
