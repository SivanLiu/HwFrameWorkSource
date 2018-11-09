package com.android.server.audio;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioFormat.Builder;
import android.media.AudioRecordingConfiguration;
import android.media.AudioSystem;
import android.media.AudioSystem.AudioRecordingCallback;
import android.media.IRecordingConfigDispatcher;
import android.media.MediaRecorder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.util.Log;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public final class RecordingActivityMonitor implements AudioRecordingCallback {
    public static final String TAG = "AudioService.RecordingActivityMonitor";
    private ArrayList<RecMonitorClient> mClients = new ArrayList();
    private boolean mHasPublicClients = false;
    private final PackageManager mPackMan;
    private HashMap<Integer, AudioRecordingConfiguration> mRecordConfigs = new HashMap();

    private static final class RecMonitorClient implements DeathRecipient {
        static RecordingActivityMonitor sMonitor;
        final IRecordingConfigDispatcher mDispatcherCb;
        final boolean mIsPrivileged;

        RecMonitorClient(IRecordingConfigDispatcher rcdb, boolean isPrivileged) {
            this.mDispatcherCb = rcdb;
            this.mIsPrivileged = isPrivileged;
        }

        public void binderDied() {
            Log.w(RecordingActivityMonitor.TAG, "client died");
            sMonitor.unregisterRecordingCallback(this.mDispatcherCb);
        }

        boolean init() {
            try {
                this.mDispatcherCb.asBinder().linkToDeath(this, 0);
                return true;
            } catch (RemoteException e) {
                Log.w(RecordingActivityMonitor.TAG, "Could not link to client death", e);
                return false;
            }
        }

        void release() {
            this.mDispatcherCb.asBinder().unlinkToDeath(this, 0);
        }
    }

    RecordingActivityMonitor(Context ctxt) {
        RecMonitorClient.sMonitor = this;
        this.mPackMan = ctxt.getPackageManager();
    }

    public void onRecordingConfigurationChanged(int event, int uid, int session, int source, int[] recordingInfo, String packName) {
        if (!MediaRecorder.isSystemOnlyAudioSource(source)) {
            List<AudioRecordingConfiguration> configsSystem = updateSnapshot(event, uid, session, source, recordingInfo);
            if (configsSystem != null) {
                synchronized (this.mClients) {
                    List<AudioRecordingConfiguration> configsPublic;
                    if (this.mHasPublicClients) {
                        configsPublic = anonymizeForPublicConsumption(configsSystem);
                    } else {
                        configsPublic = new ArrayList();
                    }
                    Iterator<RecMonitorClient> clientIterator = this.mClients.iterator();
                    while (clientIterator.hasNext()) {
                        RecMonitorClient rmc = (RecMonitorClient) clientIterator.next();
                        try {
                            if (rmc.mIsPrivileged) {
                                rmc.mDispatcherCb.dispatchRecordingConfigChange(configsSystem);
                            } else {
                                rmc.mDispatcherCb.dispatchRecordingConfigChange(configsPublic);
                            }
                        } catch (RemoteException e) {
                            Log.w(TAG, "Could not call dispatchRecordingConfigChange() on client", e);
                        }
                    }
                }
            }
        }
    }

    protected void dump(PrintWriter pw) {
        pw.println("\nRecordActivityMonitor dump time: " + DateFormat.getTimeInstance().format(new Date()));
        synchronized (this.mRecordConfigs) {
            for (AudioRecordingConfiguration conf : this.mRecordConfigs.values()) {
                conf.dump(pw);
            }
        }
    }

    private ArrayList<AudioRecordingConfiguration> anonymizeForPublicConsumption(List<AudioRecordingConfiguration> sysConfigs) {
        ArrayList<AudioRecordingConfiguration> publicConfigs = new ArrayList();
        for (AudioRecordingConfiguration config : sysConfigs) {
            publicConfigs.add(AudioRecordingConfiguration.anonymizedCopy(config));
        }
        return publicConfigs;
    }

    void initMonitor() {
        AudioSystem.setRecordingCallback(this);
    }

    void registerRecordingCallback(IRecordingConfigDispatcher rcdb, boolean isPrivileged) {
        if (rcdb != null) {
            synchronized (this.mClients) {
                RecMonitorClient rmc = new RecMonitorClient(rcdb, isPrivileged);
                if (rmc.init()) {
                    if (!isPrivileged) {
                        this.mHasPublicClients = true;
                    }
                    this.mClients.add(rmc);
                }
            }
        }
    }

    void unregisterRecordingCallback(IRecordingConfigDispatcher rcdb) {
        if (rcdb != null) {
            synchronized (this.mClients) {
                Iterator<RecMonitorClient> clientIterator = this.mClients.iterator();
                boolean hasPublicClients = false;
                while (clientIterator.hasNext()) {
                    RecMonitorClient rmc = (RecMonitorClient) clientIterator.next();
                    if (rcdb.equals(rmc.mDispatcherCb)) {
                        rmc.release();
                        clientIterator.remove();
                    } else if (!rmc.mIsPrivileged) {
                        hasPublicClients = true;
                    }
                }
                this.mHasPublicClients = hasPublicClients;
            }
        }
    }

    List<AudioRecordingConfiguration> getActiveRecordingConfigurations(boolean isPrivileged) {
        synchronized (this.mRecordConfigs) {
            if (isPrivileged) {
                List arrayList = new ArrayList(this.mRecordConfigs.values());
                return arrayList;
            }
            List<AudioRecordingConfiguration> configsPublic = anonymizeForPublicConsumption(new ArrayList(this.mRecordConfigs.values()));
            return configsPublic;
        }
    }

    private List<AudioRecordingConfiguration> updateSnapshot(int event, int uid, int session, int source, int[] recordingInfo) {
        List<AudioRecordingConfiguration> arrayList;
        synchronized (this.mRecordConfigs) {
            boolean configChanged;
            switch (event) {
                case 0:
                    if (this.mRecordConfigs.remove(new Integer(session)) == null) {
                        configChanged = false;
                        break;
                    }
                    configChanged = true;
                    break;
                case 1:
                    String packageName;
                    AudioFormat clientFormat = new Builder().setEncoding(recordingInfo[0]).setChannelMask(recordingInfo[1]).setSampleRate(recordingInfo[2]).build();
                    AudioFormat deviceFormat = new Builder().setEncoding(recordingInfo[3]).setChannelMask(recordingInfo[4]).setSampleRate(recordingInfo[5]).build();
                    int patchHandle = recordingInfo[6];
                    Integer sessionKey = new Integer(session);
                    String[] packages = this.mPackMan.getPackagesForUid(uid);
                    if (packages == null || packages.length <= 0) {
                        packageName = "";
                    } else {
                        packageName = packages[0];
                    }
                    AudioRecordingConfiguration updatedConfig = new AudioRecordingConfiguration(uid, session, source, clientFormat, deviceFormat, patchHandle, packageName);
                    if (this.mRecordConfigs.containsKey(sessionKey)) {
                        if (!updatedConfig.equals(this.mRecordConfigs.get(sessionKey))) {
                            this.mRecordConfigs.remove(sessionKey);
                            this.mRecordConfigs.put(sessionKey, updatedConfig);
                            configChanged = true;
                            break;
                        }
                        configChanged = false;
                        break;
                    }
                    this.mRecordConfigs.put(sessionKey, updatedConfig);
                    configChanged = true;
                    break;
                    break;
                default:
                    Log.e(TAG, String.format("Unknown event %d for session %d, source %d", new Object[]{Integer.valueOf(event), Integer.valueOf(session), Integer.valueOf(source)}));
                    configChanged = false;
                    break;
            }
            if (configChanged) {
                arrayList = new ArrayList(this.mRecordConfigs.values());
            } else {
                arrayList = null;
            }
        }
        return arrayList;
    }
}
