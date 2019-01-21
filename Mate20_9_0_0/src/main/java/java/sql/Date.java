package java.sql;

public class Date extends java.util.Date {
    static final long serialVersionUID = 1511598038487230103L;

    @Deprecated
    public Date(int year, int month, int day) {
        super(year, month, day);
    }

    public Date(long date) {
        super(date);
    }

    public void setTime(long date) {
        super.setTime(date);
    }

    /* JADX WARNING: Removed duplicated region for block: B:29:0x0080  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x007f A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static Date valueOf(String s) {
        String str = s;
        Date d = null;
        int YEAR_LENGTH;
        if (str != null) {
            int firstDash = str.indexOf(45);
            int secondDash = str.indexOf(45, firstDash + 1);
            if (firstDash > 0 && secondDash > 0 && secondDash < s.length() - 1) {
                String yyyy = str.substring(null, firstDash);
                String mm = str.substring(firstDash + 1, secondDash);
                String dd = str.substring(secondDash + 1);
                if (yyyy.length() == 4 && mm.length() >= 1 && mm.length() <= 2 && dd.length() >= 1 && dd.length() <= 2) {
                    int year = Integer.parseInt(yyyy);
                    int month = Integer.parseInt(mm);
                    int day = Integer.parseInt(dd);
                    if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                        YEAR_LENGTH = 4;
                        d = new Date(year - 1900, month - 1, day);
                        if (d == null) {
                            return d;
                        }
                        throw new IllegalArgumentException();
                    }
                }
            }
            YEAR_LENGTH = 4;
            if (d == null) {
            }
        } else {
            YEAR_LENGTH = 4;
            throw new IllegalArgumentException();
        }
    }

    public String toString() {
        int year = super.getYear() + 1900;
        int month = super.getMonth() + 1;
        int day = super.getDate();
        char[] buf = "2000-00-00".toCharArray();
        buf[0] = Character.forDigit(year / 1000, 10);
        buf[1] = Character.forDigit((year / 100) % 10, 10);
        buf[2] = Character.forDigit((year / 10) % 10, 10);
        buf[3] = Character.forDigit(year % 10, 10);
        buf[5] = Character.forDigit(month / 10, 10);
        buf[6] = Character.forDigit(month % 10, 10);
        buf[8] = Character.forDigit(day / 10, 10);
        buf[9] = Character.forDigit(day % 10, 10);
        return new String(buf);
    }

    @Deprecated
    public int getHours() {
        throw new IllegalArgumentException();
    }

    @Deprecated
    public int getMinutes() {
        throw new IllegalArgumentException();
    }

    @Deprecated
    public int getSeconds() {
        throw new IllegalArgumentException();
    }

    @Deprecated
    public void setHours(int i) {
        throw new IllegalArgumentException();
    }

    @Deprecated
    public void setMinutes(int i) {
        throw new IllegalArgumentException();
    }

    @Deprecated
    public void setSeconds(int i) {
        throw new IllegalArgumentException();
    }
}
