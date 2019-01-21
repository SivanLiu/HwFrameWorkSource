package android.os;

import android.annotation.SystemApi;
import android.content.Context;
import android.os.IBinder.DeathRecipient;
import android.os.IIncidentManager.Stub;
import android.util.Slog;

@SystemApi
public class IncidentManager {
    private static final String TAG = "IncidentManager";
    private final Context mContext;
    private IIncidentManager mService;

    private class IncidentdDeathRecipient implements DeathRecipient {
        private IncidentdDeathRecipient() {
        }

        public void binderDied() {
            synchronized (this) {
                IncidentManager.this.mService = null;
            }
        }
    }

    public IncidentManager(Context context) {
        this.mContext = context;
    }

    public void reportIncident(IncidentReportArgs args) {
        reportIncidentInternal(args);
    }

    private void reportIncidentInternal(IncidentReportArgs args) {
        try {
            IIncidentManager service = getIIncidentManagerLocked();
            if (service == null) {
                Slog.e(TAG, "reportIncident can't find incident binder service");
            } else {
                service.reportIncident(args);
            }
        } catch (RemoteException ex) {
            Slog.e(TAG, "reportIncident failed", ex);
        }
    }

    private IIncidentManager getIIncidentManagerLocked() throws RemoteException {
        if (this.mService != null) {
            return this.mService;
        }
        synchronized (this) {
            IIncidentManager iIncidentManager;
            if (this.mService != null) {
                iIncidentManager = this.mService;
                return iIncidentManager;
            }
            this.mService = Stub.asInterface(ServiceManager.getService("incident"));
            if (this.mService != null) {
                this.mService.asBinder().linkToDeath(new IncidentdDeathRecipient(), 0);
            }
            iIncidentManager = this.mService;
            return iIncidentManager;
        }
    }
}
