package android.os;

import android.content.ContentValues;
import android.util.Log;
import java.util.ArrayList;

public class JankBdItem {
    private static boolean HWFLOW = false;
    private static final int SECTIONNUM_MAX = 100;
    private static final String TAG = "JankShield";
    public String appname;
    public String casename;
    public int id = 0;
    public String marks;
    public ArrayList<Integer> sectionCnts = new ArrayList();
    public String timestamp;
    public int totaltime;

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z;
    }

    public int getId() {
        return this.id;
    }

    public boolean readFromParcel(Parcel src, String time, int ttime) {
        this.sectionCnts.clear();
        this.timestamp = time;
        this.totaltime = ttime;
        this.casename = src.readString();
        int i = 0;
        if (this.casename == null) {
            return false;
        }
        this.appname = src.readString();
        if (this.appname == null) {
            return false;
        }
        this.marks = src.readString();
        if (this.marks == null) {
            return false;
        }
        int nsectionnum = src.readInt();
        if (nsectionnum < 0 || nsectionnum > 100) {
            return false;
        }
        while (i < nsectionnum) {
            this.sectionCnts.add(Integer.valueOf(src.readInt()));
            i++;
        }
        return true;
    }

    public boolean isEmpty() {
        int nsectionnum = this.sectionCnts.size();
        for (int i = 0; i < nsectionnum; i++) {
            if (((Integer) this.sectionCnts.get(i)).intValue() != 0) {
                return false;
            }
        }
        return true;
    }

    public boolean add(JankBdItem b) {
        int i = 0;
        if (b.sectionCnts.size() > this.sectionCnts.size()) {
            if (HWFLOW) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("a.size");
                stringBuilder.append(this.sectionCnts.size());
                stringBuilder.append(", b.size");
                stringBuilder.append(b.sectionCnts.size());
                Log.i(str, stringBuilder.toString());
            }
            return false;
        }
        this.totaltime += b.totaltime;
        int nsectionnum = b.sectionCnts.size();
        while (true) {
            int i2 = i;
            if (i2 >= nsectionnum) {
                return true;
            }
            this.sectionCnts.set(i2, Integer.valueOf(((Integer) this.sectionCnts.get(i2)).intValue() + ((Integer) b.sectionCnts.get(i2)).intValue()));
            i = i2 + 1;
        }
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.casename);
        dest.writeString(this.appname);
        dest.writeString(this.marks);
        int nsectionnum = this.sectionCnts.size();
        dest.writeInt(nsectionnum);
        for (int i = 0; i < nsectionnum; i++) {
            dest.writeInt(((Integer) this.sectionCnts.get(i)).intValue());
        }
    }

    public ContentValues getContentValues(String[] fieldnames) {
        if (fieldnames == null || fieldnames.length < 4) {
            return null;
        }
        ContentValues values = new ContentValues();
        int index = 0 + 1;
        values.put(fieldnames[0], this.casename);
        int index2 = index + 1;
        values.put(fieldnames[index], this.timestamp);
        index = index2 + 1;
        values.put(fieldnames[index2], this.appname);
        index2 = index + 1;
        values.put(fieldnames[index], Integer.valueOf(this.totaltime));
        index = index2 + 1;
        values.put(fieldnames[index2], this.marks);
        index2 = this.sectionCnts.size();
        int i = 0;
        while (i < index2 && index < fieldnames.length) {
            int index3 = index + 1;
            values.put(fieldnames[index], (Integer) this.sectionCnts.get(i));
            i++;
            index = index3;
        }
        return values;
    }
}
