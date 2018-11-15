package com.android.server.location;

import android.location.ILocationManager;
import android.location.Location;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.Log;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import com.android.internal.location.ProviderProperties;
import com.android.internal.location.ProviderRequest;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class MockProvider implements LocationProviderInterface {
    private static final String TAG = "MockProvider";
    private boolean mEnabled;
    private final Bundle mExtras = new Bundle();
    private boolean mHasLocation;
    private boolean mHasStatus;
    private final Location mLocation;
    private final ILocationManager mLocationManager;
    private final String mName;
    private final ProviderProperties mProperties;
    private int mStatus;
    private long mStatusUpdateTime;

    public MockProvider(String name, ILocationManager locationManager, ProviderProperties properties) {
        if (properties != null) {
            this.mName = name;
            this.mLocationManager = locationManager;
            this.mProperties = properties;
            this.mLocation = new Location(name);
            return;
        }
        throw new NullPointerException("properties is null");
    }

    public String getName() {
        return this.mName;
    }

    public ProviderProperties getProperties() {
        return this.mProperties;
    }

    public void disable() {
        this.mEnabled = false;
    }

    public void enable() {
        this.mEnabled = true;
    }

    public boolean isEnabled() {
        return this.mEnabled;
    }

    public int getStatus(Bundle extras) {
        if (!this.mHasStatus) {
            return 2;
        }
        extras.clear();
        extras.putAll(this.mExtras);
        return this.mStatus;
    }

    public long getStatusUpdateTime() {
        return this.mStatusUpdateTime;
    }

    public void setLocation(Location l) {
        this.mLocation.set(l);
        this.mHasLocation = true;
        if (this.mEnabled) {
            try {
                this.mLocationManager.reportLocation(this.mLocation, false);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException calling reportLocation");
            }
        }
    }

    public void clearLocation() {
        this.mHasLocation = false;
    }

    public void setStatus(int status, Bundle extras, long updateTime) {
        this.mStatus = status;
        this.mStatusUpdateTime = updateTime;
        this.mExtras.clear();
        if (extras != null) {
            this.mExtras.putAll(extras);
        }
        this.mHasStatus = true;
    }

    public void clearStatus() {
        this.mHasStatus = false;
        this.mStatusUpdateTime = 0;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        dump(pw, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
    }

    public void dump(PrintWriter pw, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append(this.mName);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mHasLocation=");
        stringBuilder.append(this.mHasLocation);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mLocation:");
        pw.println(stringBuilder.toString());
        Location location = this.mLocation;
        Printer printWriterPrinter = new PrintWriterPrinter(pw);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(prefix);
        stringBuilder2.append("  ");
        location.dump(printWriterPrinter, stringBuilder2.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mHasStatus=");
        stringBuilder.append(this.mHasStatus);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mStatus=");
        stringBuilder.append(this.mStatus);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mStatusUpdateTime=");
        stringBuilder.append(this.mStatusUpdateTime);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mExtras=");
        stringBuilder.append(this.mExtras);
        pw.println(stringBuilder.toString());
    }

    public void setRequest(ProviderRequest request, WorkSource source) {
    }

    public boolean sendExtraCommand(String command, Bundle extras) {
        return false;
    }
}
