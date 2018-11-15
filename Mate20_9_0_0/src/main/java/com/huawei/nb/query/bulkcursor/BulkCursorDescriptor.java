package com.huawei.nb.query.bulkcursor;

import android.database.CursorWindow;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public final class BulkCursorDescriptor implements Parcelable {
    public static final Creator<BulkCursorDescriptor> CREATOR = new Creator<BulkCursorDescriptor>() {
        public BulkCursorDescriptor createFromParcel(Parcel in) {
            return new BulkCursorDescriptor(in);
        }

        public BulkCursorDescriptor[] newArray(int size) {
            return new BulkCursorDescriptor[size];
        }
    };
    private String[] columnNames;
    private int count;
    private IBulkCursor cursor;
    private boolean wantsAllOnMoveCalls;
    private CursorWindow window;

    public BulkCursorDescriptor(IBulkCursor cursor, String[] columnNames, boolean wantsAllOnMoveCalls, int count, CursorWindow window) {
        this.cursor = cursor;
        this.columnNames = columnNames == null ? null : (String[]) columnNames.clone();
        this.wantsAllOnMoveCalls = wantsAllOnMoveCalls;
        this.count = count;
        this.window = window;
    }

    public BulkCursorDescriptor(Parcel in) {
        this.cursor = BulkCursorNative.asInterface(in.readStrongBinder());
        this.columnNames = in.createStringArray();
        this.wantsAllOnMoveCalls = in.readInt() != 0;
        this.count = in.readInt();
        if (in.readInt() != 0) {
            this.window = (CursorWindow) CursorWindow.CREATOR.createFromParcel(in);
        }
    }

    public IBulkCursor getCursor() {
        return this.cursor;
    }

    public String[] getColumnNames() {
        return (String[]) this.columnNames.clone();
    }

    public boolean isWantsAllOnMoveCalls() {
        return this.wantsAllOnMoveCalls;
    }

    public int getCount() {
        return this.count;
    }

    public CursorWindow getWindow() {
        return this.window;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeStrongBinder(this.cursor.asBinder());
        out.writeStringArray(this.columnNames);
        out.writeInt(this.wantsAllOnMoveCalls ? 1 : 0);
        out.writeInt(this.count);
        if (this.window != null) {
            out.writeInt(1);
            this.window.writeToParcel(out, flags);
            return;
        }
        out.writeInt(0);
    }
}
