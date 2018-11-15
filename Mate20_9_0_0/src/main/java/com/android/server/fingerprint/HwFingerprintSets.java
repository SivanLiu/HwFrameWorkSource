package com.android.server.fingerprint;

import android.hardware.fingerprint.Fingerprint;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Slog;
import java.util.ArrayList;

public final class HwFingerprintSets implements Parcelable {
    public static final Creator<HwFingerprintSets> CREATOR = new Creator<HwFingerprintSets>() {
        public HwFingerprintSets createFromParcel(Parcel in) {
            return new HwFingerprintSets(in, null);
        }

        public HwFingerprintSets[] newArray(int size) {
            return new HwFingerprintSets[size];
        }
    };
    private static final String TAG = "HwFingerprintSets";
    public ArrayList<HwFingerprintGroup> mFingerprintGroups;

    public static class HwFingerprintGroup {
        public static final int DESCRIPTION_LEN = 256;
        public ArrayList<Fingerprint> mFingerprints;
        public int mGroupId;

        /* synthetic */ HwFingerprintGroup(Parcel x0, AnonymousClass1 x1) {
            this(x0);
        }

        public HwFingerprintGroup() {
            this.mFingerprints = new ArrayList();
        }

        private HwFingerprintGroup(Parcel in) {
            this.mFingerprints = new ArrayList();
            this.mGroupId = in.readInt();
            String str = HwFingerprintSets.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HwFingerprintGroup, mGroupId=");
            stringBuilder.append(this.mGroupId);
            Slog.i(str, stringBuilder.toString());
            int fpCount = in.readInt();
            String str2 = HwFingerprintSets.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("HwFingerprintGroup, fpCount=");
            stringBuilder2.append(fpCount);
            Slog.i(str2, stringBuilder2.toString());
            for (int i = 0; i < fpCount; i++) {
                int fingerid = in.readInt();
                String str3 = HwFingerprintSets.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("HwFingerprintGroup, fingerid=");
                stringBuilder3.append(fingerid);
                Slog.i(str3, stringBuilder3.toString());
                this.mFingerprints.add(new Fingerprint("", this.mGroupId, fingerid, 0));
            }
        }
    }

    /* synthetic */ HwFingerprintSets(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public HwFingerprintSets() {
        this.mFingerprintGroups = new ArrayList();
    }

    private HwFingerprintSets(Parcel in) {
        this.mFingerprintGroups = new ArrayList();
        int groupCount = in.readInt();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwFingerprintSets, groupCount=");
        stringBuilder.append(groupCount);
        Slog.i(str, stringBuilder.toString());
        for (int i = 0; i < groupCount; i++) {
            this.mFingerprintGroups.add(new HwFingerprintGroup(in, null));
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
    }
}
