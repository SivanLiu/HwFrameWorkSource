package android.hardware.usb;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public final class UsbPortStatus implements Parcelable {
    public static final Creator<UsbPortStatus> CREATOR = new Creator<UsbPortStatus>() {
        public UsbPortStatus createFromParcel(Parcel in) {
            return new UsbPortStatus(in.readInt(), in.readInt(), in.readInt(), in.readInt());
        }

        public UsbPortStatus[] newArray(int size) {
            return new UsbPortStatus[size];
        }
    };
    private final int mCurrentDataRole;
    private final int mCurrentMode;
    private final int mCurrentPowerRole;
    private final int mSupportedRoleCombinations;

    public UsbPortStatus(int currentMode, int currentPowerRole, int currentDataRole, int supportedRoleCombinations) {
        this.mCurrentMode = currentMode;
        this.mCurrentPowerRole = currentPowerRole;
        this.mCurrentDataRole = currentDataRole;
        this.mSupportedRoleCombinations = supportedRoleCombinations;
    }

    public boolean isConnected() {
        return this.mCurrentMode != 0;
    }

    public int getCurrentMode() {
        return this.mCurrentMode;
    }

    public int getCurrentPowerRole() {
        return this.mCurrentPowerRole;
    }

    public int getCurrentDataRole() {
        return this.mCurrentDataRole;
    }

    public boolean isRoleCombinationSupported(int powerRole, int dataRole) {
        return (this.mSupportedRoleCombinations & UsbPort.combineRolesAsBit(powerRole, dataRole)) != 0;
    }

    public int getSupportedRoleCombinations() {
        return this.mSupportedRoleCombinations;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UsbPortStatus{connected=");
        stringBuilder.append(isConnected());
        stringBuilder.append(", currentMode=");
        stringBuilder.append(UsbPort.modeToString(this.mCurrentMode));
        stringBuilder.append(", currentPowerRole=");
        stringBuilder.append(UsbPort.powerRoleToString(this.mCurrentPowerRole));
        stringBuilder.append(", currentDataRole=");
        stringBuilder.append(UsbPort.dataRoleToString(this.mCurrentDataRole));
        stringBuilder.append(", supportedRoleCombinations=");
        stringBuilder.append(UsbPort.roleCombinationsToString(this.mSupportedRoleCombinations));
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mCurrentMode);
        dest.writeInt(this.mCurrentPowerRole);
        dest.writeInt(this.mCurrentDataRole);
        dest.writeInt(this.mSupportedRoleCombinations);
    }
}
