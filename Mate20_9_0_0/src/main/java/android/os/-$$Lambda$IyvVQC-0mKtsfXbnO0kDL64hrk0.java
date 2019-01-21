package android.os;

import android.os.BatteryStats.IntToString;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IyvVQC-0mKtsfXbnO0kDL64hrk0 implements IntToString {
    public static final /* synthetic */ -$$Lambda$IyvVQC-0mKtsfXbnO0kDL64hrk0 INSTANCE = new -$$Lambda$IyvVQC-0mKtsfXbnO0kDL64hrk0();

    private /* synthetic */ -$$Lambda$IyvVQC-0mKtsfXbnO0kDL64hrk0() {
    }

    public final String applyAsString(int i) {
        return UserHandle.formatUid(i);
    }
}
