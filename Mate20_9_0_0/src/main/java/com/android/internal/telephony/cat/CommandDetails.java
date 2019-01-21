package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

class CommandDetails extends ValueObject implements Parcelable {
    public static final Creator<CommandDetails> CREATOR = new Creator<CommandDetails>() {
        public CommandDetails createFromParcel(Parcel in) {
            return new CommandDetails(in);
        }

        public CommandDetails[] newArray(int size) {
            return new CommandDetails[size];
        }
    };
    public int commandNumber;
    public int commandQualifier;
    public boolean compRequired;
    public int typeOfCommand;

    public ComprehensionTlvTag getTag() {
        return ComprehensionTlvTag.COMMAND_DETAILS;
    }

    CommandDetails() {
    }

    public boolean compareTo(CommandDetails other) {
        return this.compRequired == other.compRequired && this.commandNumber == other.commandNumber && this.commandQualifier == other.commandQualifier && this.typeOfCommand == other.typeOfCommand;
    }

    public CommandDetails(Parcel in) {
        this.compRequired = in.readInt() != 0;
        this.commandNumber = in.readInt();
        this.typeOfCommand = in.readInt();
        this.commandQualifier = in.readInt();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.compRequired);
        dest.writeInt(this.commandNumber);
        dest.writeInt(this.typeOfCommand);
        dest.writeInt(this.commandQualifier);
    }

    public int describeContents() {
        return 0;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CmdDetails: compRequired=");
        stringBuilder.append(this.compRequired);
        stringBuilder.append(" commandNumber=");
        stringBuilder.append(this.commandNumber);
        stringBuilder.append(" typeOfCommand=");
        stringBuilder.append(this.typeOfCommand);
        stringBuilder.append(" commandQualifier=");
        stringBuilder.append(this.commandQualifier);
        return stringBuilder.toString();
    }
}
