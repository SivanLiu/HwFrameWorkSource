package android.os;

import android.annotation.SystemApi;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcelable.Creator;
import android.util.IntArray;
import java.util.ArrayList;

@SystemApi
public final class IncidentReportArgs implements Parcelable {
    public static final Creator<IncidentReportArgs> CREATOR = new Creator<IncidentReportArgs>() {
        public IncidentReportArgs createFromParcel(Parcel in) {
            return new IncidentReportArgs(in);
        }

        public IncidentReportArgs[] newArray(int size) {
            return new IncidentReportArgs[size];
        }
    };
    private static final int DEST_AUTO = 200;
    private static final int DEST_EXPLICIT = 100;
    private boolean mAll;
    private int mDest;
    private final ArrayList<byte[]> mHeaders;
    private final IntArray mSections;

    public IncidentReportArgs() {
        this.mSections = new IntArray();
        this.mHeaders = new ArrayList();
        this.mDest = 200;
    }

    public IncidentReportArgs(Parcel in) {
        this.mSections = new IntArray();
        this.mHeaders = new ArrayList();
        readFromParcel(in);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.mAll);
        int N = this.mSections.size();
        out.writeInt(N);
        int i = 0;
        for (int i2 = 0; i2 < N; i2++) {
            out.writeInt(this.mSections.get(i2));
        }
        N = this.mHeaders.size();
        out.writeInt(N);
        while (i < N) {
            out.writeByteArray((byte[]) this.mHeaders.get(i));
            i++;
        }
        out.writeInt(this.mDest);
    }

    public void readFromParcel(Parcel in) {
        int i = 0;
        this.mAll = in.readInt() != 0;
        this.mSections.clear();
        int N = in.readInt();
        for (int i2 = 0; i2 < N; i2++) {
            this.mSections.add(in.readInt());
        }
        this.mHeaders.clear();
        N = in.readInt();
        while (i < N) {
            this.mHeaders.add(in.createByteArray());
            i++;
        }
        this.mDest = in.readInt();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Incident(");
        if (this.mAll) {
            sb.append("all");
        } else {
            int N = this.mSections.size();
            if (N > 0) {
                sb.append(this.mSections.get(0));
            }
            for (int i = 1; i < N; i++) {
                sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                sb.append(this.mSections.get(i));
            }
        }
        sb.append(", ");
        sb.append(this.mHeaders.size());
        sb.append(" headers), ");
        sb.append("Dest enum value: ");
        sb.append(this.mDest);
        return sb.toString();
    }

    public void setAll(boolean all) {
        this.mAll = all;
        if (all) {
            this.mSections.clear();
        }
    }

    public void setPrivacyPolicy(int dest) {
        if (dest == 100 || dest == 200) {
            this.mDest = dest;
        } else {
            this.mDest = 200;
        }
    }

    public void addSection(int section) {
        if (!this.mAll && section > 1) {
            this.mSections.add(section);
        }
    }

    public boolean isAll() {
        return this.mAll;
    }

    public boolean containsSection(int section) {
        return this.mAll || this.mSections.indexOf(section) >= 0;
    }

    public int sectionCount() {
        return this.mSections.size();
    }

    public void addHeader(byte[] header) {
        this.mHeaders.add(header);
    }
}
