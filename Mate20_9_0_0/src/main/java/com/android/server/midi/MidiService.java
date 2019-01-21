package com.android.server.midi;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.XmlResourceParser;
import android.media.midi.IBluetoothMidiService;
import android.media.midi.IMidiDeviceListener;
import android.media.midi.IMidiDeviceOpenCallback;
import android.media.midi.IMidiDeviceServer;
import android.media.midi.IMidiManager.Stub;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiDeviceStatus;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.SystemService;
import com.android.server.pm.Settings;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MidiService extends Stub {
    private static final MidiDeviceInfo[] EMPTY_DEVICE_INFO_ARRAY = new MidiDeviceInfo[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final String TAG = "MidiService";
    private final HashMap<BluetoothDevice, Device> mBluetoothDevices = new HashMap();
    private int mBluetoothServiceUid;
    private final HashMap<IBinder, Client> mClients = new HashMap();
    private final Context mContext;
    private final HashMap<MidiDeviceInfo, Device> mDevicesByInfo = new HashMap();
    private final HashMap<IBinder, Device> mDevicesByServer = new HashMap();
    private int mNextDeviceId = 1;
    private final PackageManager mPackageManager;
    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        public void onPackageAdded(String packageName, int uid) {
            MidiService.this.addPackageDeviceServers(packageName);
        }

        public void onPackageModified(String packageName) {
            MidiService.this.removePackageDeviceServers(packageName);
            MidiService.this.addPackageDeviceServers(packageName);
        }

        public void onPackageRemoved(String packageName, int uid) {
            MidiService.this.removePackageDeviceServers(packageName);
        }
    };

    private final class Client implements DeathRecipient {
        private final HashMap<IBinder, DeviceConnection> mDeviceConnections = new HashMap();
        private final HashMap<IBinder, IMidiDeviceListener> mListeners = new HashMap();
        private final int mPid;
        private final IBinder mToken;
        private final int mUid;

        public Client(IBinder token) {
            this.mToken = token;
            this.mUid = Binder.getCallingUid();
            this.mPid = Binder.getCallingPid();
        }

        public int getUid() {
            return this.mUid;
        }

        public void addListener(IMidiDeviceListener listener) {
            this.mListeners.put(listener.asBinder(), listener);
        }

        public void removeListener(IMidiDeviceListener listener) {
            this.mListeners.remove(listener.asBinder());
            if (this.mListeners.size() == 0 && this.mDeviceConnections.size() == 0) {
                close();
            }
        }

        public void addDeviceConnection(Device device, IMidiDeviceOpenCallback callback) {
            DeviceConnection connection = new DeviceConnection(device, this, callback);
            this.mDeviceConnections.put(connection.getToken(), connection);
            device.addDeviceConnection(connection);
        }

        public void removeDeviceConnection(IBinder token) {
            DeviceConnection connection = (DeviceConnection) this.mDeviceConnections.remove(token);
            if (connection != null) {
                connection.getDevice().removeDeviceConnection(connection);
            }
            if (this.mListeners.size() == 0 && this.mDeviceConnections.size() == 0) {
                close();
            }
        }

        public void removeDeviceConnection(DeviceConnection connection) {
            this.mDeviceConnections.remove(connection.getToken());
            if (this.mListeners.size() == 0 && this.mDeviceConnections.size() == 0) {
                close();
            }
        }

        public void deviceAdded(Device device) {
            if (device.isUidAllowed(this.mUid)) {
                MidiDeviceInfo deviceInfo = device.getDeviceInfo();
                try {
                    for (IMidiDeviceListener listener : this.mListeners.values()) {
                        listener.onDeviceAdded(deviceInfo);
                    }
                } catch (RemoteException e) {
                    Log.e(MidiService.TAG, "remote exception", e);
                }
            }
        }

        public void deviceRemoved(Device device) {
            if (device.isUidAllowed(this.mUid)) {
                MidiDeviceInfo deviceInfo = device.getDeviceInfo();
                try {
                    for (IMidiDeviceListener listener : this.mListeners.values()) {
                        listener.onDeviceRemoved(deviceInfo);
                    }
                } catch (RemoteException e) {
                    Log.e(MidiService.TAG, "remote exception", e);
                }
            }
        }

        public void deviceStatusChanged(Device device, MidiDeviceStatus status) {
            if (device.isUidAllowed(this.mUid)) {
                try {
                    for (IMidiDeviceListener listener : this.mListeners.values()) {
                        listener.onDeviceStatusChanged(status);
                    }
                } catch (RemoteException e) {
                    Log.e(MidiService.TAG, "remote exception", e);
                }
            }
        }

        private void close() {
            synchronized (MidiService.this.mClients) {
                MidiService.this.mClients.remove(this.mToken);
                this.mToken.unlinkToDeath(this, 0);
            }
            for (DeviceConnection connection : this.mDeviceConnections.values()) {
                connection.getDevice().removeDeviceConnection(connection);
            }
        }

        public void binderDied() {
            String str = MidiService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Client died: ");
            stringBuilder.append(this);
            Log.d(str, stringBuilder.toString());
            close();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("Client: UID: ");
            sb.append(this.mUid);
            sb.append(" PID: ");
            sb.append(this.mPid);
            sb.append(" listener count: ");
            sb.append(this.mListeners.size());
            sb.append(" Device Connections:");
            for (DeviceConnection connection : this.mDeviceConnections.values()) {
                sb.append(" <device ");
                sb.append(connection.getDevice().getDeviceInfo().getId());
                sb.append(">");
            }
            return sb.toString();
        }
    }

    private final class Device implements DeathRecipient {
        private final BluetoothDevice mBluetoothDevice;
        private final ArrayList<DeviceConnection> mDeviceConnections = new ArrayList();
        private MidiDeviceInfo mDeviceInfo;
        private MidiDeviceStatus mDeviceStatus;
        private IMidiDeviceServer mServer;
        private ServiceConnection mServiceConnection;
        private final ServiceInfo mServiceInfo;
        private final int mUid;

        public Device(IMidiDeviceServer server, MidiDeviceInfo deviceInfo, ServiceInfo serviceInfo, int uid) {
            this.mDeviceInfo = deviceInfo;
            this.mServiceInfo = serviceInfo;
            this.mUid = uid;
            this.mBluetoothDevice = (BluetoothDevice) deviceInfo.getProperties().getParcelable("bluetooth_device");
            setDeviceServer(server);
        }

        public Device(BluetoothDevice bluetoothDevice) {
            this.mBluetoothDevice = bluetoothDevice;
            this.mServiceInfo = null;
            this.mUid = MidiService.this.mBluetoothServiceUid;
        }

        private void setDeviceServer(IMidiDeviceServer server) {
            if (server != null) {
                if (this.mServer != null) {
                    Log.e(MidiService.TAG, "mServer already set in setDeviceServer");
                    return;
                }
                IBinder binder = server.asBinder();
                try {
                    binder.linkToDeath(this, 0);
                    this.mServer = server;
                    MidiService.this.mDevicesByServer.put(binder, this);
                } catch (RemoteException e) {
                    this.mServer = null;
                    return;
                }
            } else if (this.mServer != null) {
                server = this.mServer;
                this.mServer = null;
                IBinder binder2 = server.asBinder();
                MidiService.this.mDevicesByServer.remove(binder2);
                try {
                    server.closeDevice();
                    binder2.unlinkToDeath(this, 0);
                } catch (RemoteException e2) {
                }
            }
            if (this.mDeviceConnections != null) {
                Iterator it = this.mDeviceConnections.iterator();
                while (it.hasNext()) {
                    ((DeviceConnection) it.next()).notifyClient(server);
                }
            }
        }

        public MidiDeviceInfo getDeviceInfo() {
            return this.mDeviceInfo;
        }

        public void setDeviceInfo(MidiDeviceInfo deviceInfo) {
            this.mDeviceInfo = deviceInfo;
        }

        public MidiDeviceStatus getDeviceStatus() {
            return this.mDeviceStatus;
        }

        public void setDeviceStatus(MidiDeviceStatus status) {
            this.mDeviceStatus = status;
        }

        public IMidiDeviceServer getDeviceServer() {
            return this.mServer;
        }

        public ServiceInfo getServiceInfo() {
            return this.mServiceInfo;
        }

        public String getPackageName() {
            return this.mServiceInfo == null ? null : this.mServiceInfo.packageName;
        }

        public int getUid() {
            return this.mUid;
        }

        public boolean isUidAllowed(int uid) {
            return !this.mDeviceInfo.isPrivate() || this.mUid == uid;
        }

        public void addDeviceConnection(DeviceConnection connection) {
            synchronized (this.mDeviceConnections) {
                if (this.mServer != null) {
                    this.mDeviceConnections.add(connection);
                    connection.notifyClient(this.mServer);
                } else if (this.mServiceConnection != null || (this.mServiceInfo == null && this.mBluetoothDevice == null)) {
                    Log.e(MidiService.TAG, "No way to connect to device in addDeviceConnection");
                    connection.notifyClient(null);
                } else {
                    Intent intent;
                    this.mDeviceConnections.add(connection);
                    this.mServiceConnection = new ServiceConnection() {
                        public void onServiceConnected(ComponentName name, IBinder service) {
                            IMidiDeviceServer server = null;
                            if (Device.this.mBluetoothDevice != null) {
                                try {
                                    server = IMidiDeviceServer.Stub.asInterface(IBluetoothMidiService.Stub.asInterface(service).addBluetoothDevice(Device.this.mBluetoothDevice));
                                } catch (RemoteException e) {
                                    Log.e(MidiService.TAG, "Could not call addBluetoothDevice()", e);
                                }
                            } else {
                                server = IMidiDeviceServer.Stub.asInterface(service);
                            }
                            Device.this.setDeviceServer(server);
                        }

                        public void onServiceDisconnected(ComponentName name) {
                            Device.this.setDeviceServer(null);
                            Device.this.mServiceConnection = null;
                        }
                    };
                    if (this.mBluetoothDevice != null) {
                        intent = new Intent("android.media.midi.BluetoothMidiService");
                        intent.setComponent(new ComponentName("com.android.bluetoothmidiservice", "com.android.bluetoothmidiservice.BluetoothMidiService"));
                    } else {
                        intent = new Intent("android.media.midi.MidiDeviceService");
                        intent.setComponent(new ComponentName(this.mServiceInfo.packageName, this.mServiceInfo.name));
                    }
                    if (!MidiService.this.mContext.bindService(intent, this.mServiceConnection, 1)) {
                        String str = MidiService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unable to bind service: ");
                        stringBuilder.append(intent);
                        Log.e(str, stringBuilder.toString());
                        setDeviceServer(null);
                        this.mServiceConnection = null;
                    }
                }
            }
        }

        public void removeDeviceConnection(DeviceConnection connection) {
            synchronized (this.mDeviceConnections) {
                this.mDeviceConnections.remove(connection);
                if (this.mDeviceConnections.size() == 0 && this.mServiceConnection != null) {
                    MidiService.this.mContext.unbindService(this.mServiceConnection);
                    this.mServiceConnection = null;
                    if (this.mBluetoothDevice != null) {
                        synchronized (MidiService.this.mDevicesByInfo) {
                            closeLocked();
                        }
                    } else {
                        setDeviceServer(null);
                    }
                }
            }
        }

        public void closeLocked() {
            synchronized (this.mDeviceConnections) {
                Iterator it = this.mDeviceConnections.iterator();
                while (it.hasNext()) {
                    DeviceConnection connection = (DeviceConnection) it.next();
                    connection.getClient().removeDeviceConnection(connection);
                }
                this.mDeviceConnections.clear();
            }
            setDeviceServer(null);
            if (this.mServiceInfo == null) {
                MidiService.this.removeDeviceLocked(this);
            } else {
                this.mDeviceStatus = new MidiDeviceStatus(this.mDeviceInfo);
            }
            if (this.mBluetoothDevice != null) {
                MidiService.this.mBluetoothDevices.remove(this.mBluetoothDevice);
            }
        }

        public void binderDied() {
            String str = MidiService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Device died: ");
            stringBuilder.append(this);
            Log.d(str, stringBuilder.toString());
            synchronized (MidiService.this.mDevicesByInfo) {
                closeLocked();
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("Device Info: ");
            sb.append(this.mDeviceInfo);
            sb.append(" Status: ");
            sb.append(this.mDeviceStatus);
            sb.append(" UID: ");
            sb.append(this.mUid);
            sb.append(" DeviceConnection count: ");
            sb.append(this.mDeviceConnections.size());
            sb.append(" mServiceConnection: ");
            sb.append(this.mServiceConnection);
            return sb.toString();
        }
    }

    private final class DeviceConnection {
        private IMidiDeviceOpenCallback mCallback;
        private final Client mClient;
        private final Device mDevice;
        private final IBinder mToken = new Binder();

        public DeviceConnection(Device device, Client client, IMidiDeviceOpenCallback callback) {
            this.mDevice = device;
            this.mClient = client;
            this.mCallback = callback;
        }

        public Device getDevice() {
            return this.mDevice;
        }

        public Client getClient() {
            return this.mClient;
        }

        public IBinder getToken() {
            return this.mToken;
        }

        public void notifyClient(IMidiDeviceServer deviceServer) {
            if (this.mCallback != null) {
                try {
                    this.mCallback.onDeviceOpened(deviceServer, deviceServer == null ? null : this.mToken);
                } catch (RemoteException e) {
                }
                this.mCallback = null;
            }
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DeviceConnection Device ID: ");
            stringBuilder.append(this.mDevice.getDeviceInfo().getId());
            return stringBuilder.toString();
        }
    }

    public static class Lifecycle extends SystemService {
        private MidiService mMidiService;

        public Lifecycle(Context context) {
            super(context);
        }

        public void onStart() {
            this.mMidiService = new MidiService(getContext());
            publishBinderService("midi", this.mMidiService);
        }

        public void onUnlockUser(int userHandle) {
            if (userHandle == 0) {
                this.mMidiService.onUnlockUser();
            }
        }
    }

    private Client getClient(IBinder token) {
        Client client;
        synchronized (this.mClients) {
            client = (Client) this.mClients.get(token);
            if (client == null) {
                client = new Client(token);
                try {
                    token.linkToDeath(client, 0);
                    this.mClients.put(token, client);
                } catch (RemoteException e) {
                    return null;
                }
            }
        }
        return client;
    }

    public MidiService(Context context) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mBluetoothServiceUid = -1;
    }

    private void onUnlockUser() {
        PackageInfo info = null;
        this.mPackageMonitor.register(this.mContext, null, true);
        List<ResolveInfo> resolveInfos = this.mPackageManager.queryIntentServices(new Intent("android.media.midi.MidiDeviceService"), 128);
        if (resolveInfos != null) {
            int count = resolveInfos.size();
            for (int i = 0; i < count; i++) {
                ServiceInfo serviceInfo = ((ResolveInfo) resolveInfos.get(i)).serviceInfo;
                if (serviceInfo != null) {
                    addPackageDeviceServer(serviceInfo);
                }
            }
        }
        try {
            info = this.mPackageManager.getPackageInfo("com.android.bluetoothmidiservice", 0);
        } catch (NameNotFoundException e) {
        }
        PackageInfo info2 = info;
        if (info2 == null || info2.applicationInfo == null) {
            this.mBluetoothServiceUid = -1;
        } else {
            this.mBluetoothServiceUid = info2.applicationInfo.uid;
        }
    }

    public void registerListener(IBinder token, IMidiDeviceListener listener) {
        Client client = getClient(token);
        if (client != null) {
            client.addListener(listener);
            updateStickyDeviceStatus(client.mUid, listener);
        }
    }

    public void unregisterListener(IBinder token, IMidiDeviceListener listener) {
        Client client = getClient(token);
        if (client != null) {
            client.removeListener(listener);
        }
    }

    private void updateStickyDeviceStatus(int uid, IMidiDeviceListener listener) {
        synchronized (this.mDevicesByInfo) {
            for (Device device : this.mDevicesByInfo.values()) {
                if (device.isUidAllowed(uid)) {
                    try {
                        MidiDeviceStatus status = device.getDeviceStatus();
                        if (status != null) {
                            listener.onDeviceStatusChanged(status);
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "remote exception", e);
                    }
                }
            }
        }
    }

    public MidiDeviceInfo[] getDevices() {
        ArrayList<MidiDeviceInfo> deviceInfos = new ArrayList();
        int uid = Binder.getCallingUid();
        synchronized (this.mDevicesByInfo) {
            for (Device device : this.mDevicesByInfo.values()) {
                if (device.isUidAllowed(uid)) {
                    deviceInfos.add(device.getDeviceInfo());
                }
            }
        }
        return (MidiDeviceInfo[]) deviceInfos.toArray(EMPTY_DEVICE_INFO_ARRAY);
    }

    public void openDevice(IBinder token, MidiDeviceInfo deviceInfo, IMidiDeviceOpenCallback callback) {
        Client client = getClient(token);
        if (client != null) {
            Device device;
            synchronized (this.mDevicesByInfo) {
                device = (Device) this.mDevicesByInfo.get(deviceInfo);
                if (device == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("device does not exist: ");
                    stringBuilder.append(deviceInfo);
                    throw new IllegalArgumentException(stringBuilder.toString());
                } else if (device.isUidAllowed(Binder.getCallingUid())) {
                } else {
                    throw new SecurityException("Attempt to open private device with wrong UID");
                }
            }
            long identity = Binder.clearCallingIdentity();
            try {
                client.addDeviceConnection(device, callback);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public void openBluetoothDevice(IBinder token, BluetoothDevice bluetoothDevice, IMidiDeviceOpenCallback callback) {
        Client client = getClient(token);
        if (client != null) {
            Device device;
            synchronized (this.mDevicesByInfo) {
                device = (Device) this.mBluetoothDevices.get(bluetoothDevice);
                if (device == null) {
                    device = new Device(bluetoothDevice);
                    this.mBluetoothDevices.put(bluetoothDevice, device);
                }
            }
            Device device2 = device;
            long identity = Binder.clearCallingIdentity();
            try {
                client.addDeviceConnection(device2, callback);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public void closeDevice(IBinder clientToken, IBinder deviceToken) {
        Client client = getClient(clientToken);
        if (client != null) {
            client.removeDeviceConnection(deviceToken);
        }
    }

    public MidiDeviceInfo registerDeviceServer(IMidiDeviceServer server, int numInputPorts, int numOutputPorts, String[] inputPortNames, String[] outputPortNames, Bundle properties, int type) {
        int i = type;
        int uid = Binder.getCallingUid();
        if (i == 1 && uid != 1000) {
            throw new SecurityException("only system can create USB devices");
        } else if (i != 3 || uid == this.mBluetoothServiceUid) {
            MidiDeviceInfo addDeviceLocked;
            synchronized (this.mDevicesByInfo) {
                addDeviceLocked = addDeviceLocked(i, numInputPorts, numOutputPorts, inputPortNames, outputPortNames, properties, server, null, false, uid);
            }
            return addDeviceLocked;
        } else {
            throw new SecurityException("only MidiBluetoothService can create Bluetooth devices");
        }
    }

    public void unregisterDeviceServer(IMidiDeviceServer server) {
        synchronized (this.mDevicesByInfo) {
            Device device = (Device) this.mDevicesByServer.get(server.asBinder());
            if (device != null) {
                device.closeLocked();
            }
        }
    }

    public MidiDeviceInfo getServiceDeviceInfo(String packageName, String className) {
        synchronized (this.mDevicesByInfo) {
            for (Device device : this.mDevicesByInfo.values()) {
                ServiceInfo serviceInfo = device.getServiceInfo();
                if (serviceInfo != null && packageName.equals(serviceInfo.packageName) && className.equals(serviceInfo.name)) {
                    MidiDeviceInfo deviceInfo = device.getDeviceInfo();
                    return deviceInfo;
                }
            }
            return null;
        }
    }

    public MidiDeviceStatus getDeviceStatus(MidiDeviceInfo deviceInfo) {
        Device device = (Device) this.mDevicesByInfo.get(deviceInfo);
        if (device != null) {
            return device.getDeviceStatus();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("no such device for ");
        stringBuilder.append(deviceInfo);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void setDeviceStatus(IMidiDeviceServer server, MidiDeviceStatus status) {
        Device device = (Device) this.mDevicesByServer.get(server.asBinder());
        if (device == null) {
            return;
        }
        if (Binder.getCallingUid() == device.getUid()) {
            device.setDeviceStatus(status);
            notifyDeviceStatusChanged(device, status);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setDeviceStatus() caller UID ");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(" does not match device's UID ");
        stringBuilder.append(device.getUid());
        throw new SecurityException(stringBuilder.toString());
    }

    private void notifyDeviceStatusChanged(Device device, MidiDeviceStatus status) {
        synchronized (this.mClients) {
            for (Client c : this.mClients.values()) {
                c.deviceStatusChanged(device, status);
            }
        }
    }

    private MidiDeviceInfo addDeviceLocked(int type, int numInputPorts, int numOutputPorts, String[] inputPortNames, String[] outputPortNames, Bundle properties, IMidiDeviceServer server, ServiceInfo serviceInfo, boolean isPrivate, int uid) {
        IMidiDeviceServer iMidiDeviceServer = server;
        int id = this.mNextDeviceId;
        this.mNextDeviceId = id + 1;
        MidiDeviceInfo deviceInfo = new MidiDeviceInfo(type, id, numInputPorts, numOutputPorts, inputPortNames, outputPortNames, properties, isPrivate);
        if (iMidiDeviceServer != null) {
            try {
                iMidiDeviceServer.setDeviceInfo(deviceInfo);
            } catch (RemoteException e) {
                RemoteException remoteException = e;
                Log.e(TAG, "RemoteException in setDeviceInfo()");
                return null;
            }
        }
        Device device = null;
        BluetoothDevice bluetoothDevice = null;
        if (type == 3) {
            bluetoothDevice = (BluetoothDevice) properties.getParcelable("bluetooth_device");
            device = (Device) this.mBluetoothDevices.get(bluetoothDevice);
            if (device != null) {
                device.setDeviceInfo(deviceInfo);
            }
        } else {
            Bundle bundle = properties;
        }
        BluetoothDevice bluetoothDevice2 = bluetoothDevice;
        if (device == null) {
            device = new Device(iMidiDeviceServer, deviceInfo, serviceInfo, uid);
        }
        Device device2 = device;
        this.mDevicesByInfo.put(deviceInfo, device2);
        if (bluetoothDevice2 != null) {
            this.mBluetoothDevices.put(bluetoothDevice2, device2);
        }
        synchronized (this.mClients) {
            for (Client c : this.mClients.values()) {
                c.deviceAdded(device2);
            }
        }
        return deviceInfo;
    }

    private void removeDeviceLocked(Device device) {
        IMidiDeviceServer server = device.getDeviceServer();
        if (server != null) {
            this.mDevicesByServer.remove(server.asBinder());
        }
        this.mDevicesByInfo.remove(device.getDeviceInfo());
        synchronized (this.mClients) {
            for (Client c : this.mClients.values()) {
                c.deviceRemoved(device);
            }
        }
    }

    private void addPackageDeviceServers(String packageName) {
        try {
            ServiceInfo[] services = this.mPackageManager.getPackageInfo(packageName, 132).services;
            if (services != null) {
                for (ServiceInfo addPackageDeviceServer : services) {
                    addPackageDeviceServer(addPackageDeviceServer);
                }
            }
        } catch (NameNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handlePackageUpdate could not find package ");
            stringBuilder.append(packageName);
            Log.e(str, stringBuilder.toString(), e);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:111:0x0247  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void addPackageDeviceServer(ServiceInfo serviceInfo) {
        Throwable th;
        String str;
        int i;
        Exception e;
        MidiService outputPortNames = this;
        ServiceInfo serviceInfo2 = serviceInfo;
        XmlResourceParser parser = null;
        XmlResourceParser parser2;
        String portName;
        try {
            parser2 = serviceInfo2.loadXmlMetaData(outputPortNames.mPackageManager, "android.media.midi.MidiDeviceService");
            if (parser2 == null) {
                if (parser2 != null) {
                    parser2.close();
                }
                return;
            }
            try {
                String tagName;
                StringBuilder stringBuilder;
                if ("android.permission.BIND_MIDI_DEVICE_SERVICE".equals(serviceInfo2.permission)) {
                    ArrayList<String> inputPortNames = new ArrayList();
                    ArrayList<String> outputPortNames2 = new ArrayList();
                    Bundle properties = null;
                    int numInputPorts = 0;
                    int numOutputPorts = 0;
                    boolean isPrivate = false;
                    while (true) {
                        ArrayList<String> outputPortNames3 = outputPortNames2;
                        int eventType = parser2.next();
                        if (eventType == 1) {
                            break;
                        }
                        ArrayList<String> outputPortNames4;
                        int i2 = 0;
                        String str2;
                        StringBuilder stringBuilder2;
                        String value;
                        if (eventType == 2) {
                            tagName = parser2.getName();
                            int count;
                            String name;
                            String value2;
                            if ("device".equals(tagName)) {
                                if (properties != null) {
                                    str2 = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("nested <device> elements in metadata for ");
                                    stringBuilder2.append(serviceInfo2.packageName);
                                    Log.w(str2, stringBuilder2.toString());
                                } else {
                                    Bundle properties2 = new Bundle();
                                    properties2.putParcelable("service_info", serviceInfo2);
                                    boolean isPrivate2 = false;
                                    int count2 = parser2.getAttributeCount();
                                    while (i2 < count2) {
                                        String name2 = parser2.getAttributeName(i2);
                                        value = parser2.getAttributeValue(i2);
                                        if ("private".equals(name2)) {
                                            isPrivate2 = "true".equals(value);
                                        } else {
                                            properties2.putString(name2, value);
                                        }
                                        i2++;
                                    }
                                    properties = properties2;
                                    numInputPorts = 0;
                                    numOutputPorts = 0;
                                    isPrivate = isPrivate2;
                                }
                            } else if ("input-port".equals(tagName)) {
                                if (properties == null) {
                                    str2 = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("<input-port> outside of <device> in metadata for ");
                                    stringBuilder2.append(serviceInfo2.packageName);
                                    Log.w(str2, stringBuilder2.toString());
                                } else {
                                    numInputPorts++;
                                    portName = null;
                                    count = parser2.getAttributeCount();
                                    while (i2 < count) {
                                        name = parser2.getAttributeName(i2);
                                        value2 = parser2.getAttributeValue(i2);
                                        if (Settings.ATTR_NAME.equals(name)) {
                                            portName = value2;
                                            break;
                                        }
                                        i2++;
                                    }
                                    inputPortNames.add(portName);
                                }
                            } else if ("output-port".equals(tagName)) {
                                if (properties == null) {
                                    str2 = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("<output-port> outside of <device> in metadata for ");
                                    stringBuilder2.append(serviceInfo2.packageName);
                                    Log.w(str2, stringBuilder2.toString());
                                } else {
                                    numOutputPorts++;
                                    portName = null;
                                    count = parser2.getAttributeCount();
                                    while (i2 < count) {
                                        name = parser2.getAttributeName(i2);
                                        value2 = parser2.getAttributeValue(i2);
                                        if (Settings.ATTR_NAME.equals(name)) {
                                            portName = value2;
                                            break;
                                        }
                                        i2++;
                                    }
                                    outputPortNames3.add(portName);
                                }
                            }
                            outputPortNames4 = outputPortNames3;
                            outputPortNames2 = outputPortNames4;
                            outputPortNames = this;
                        } else {
                            if (eventType == 3) {
                                value = parser2.getName();
                                if ("device".equals(value) && properties != null) {
                                    if (numInputPorts == 0 && numOutputPorts == 0) {
                                        tagName = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("<device> with no ports in metadata for ");
                                        stringBuilder.append(serviceInfo2.packageName);
                                        Log.w(tagName, stringBuilder.toString());
                                    } else {
                                        try {
                                            int uid = outputPortNames.mPackageManager.getApplicationInfo(serviceInfo2.packageName, 0).uid;
                                            HashMap hashMap = outputPortNames.mDevicesByInfo;
                                            synchronized (hashMap) {
                                                HashMap hashMap2;
                                                try {
                                                    MidiService midiService = outputPortNames;
                                                    hashMap2 = hashMap;
                                                    outputPortNames4 = outputPortNames3;
                                                    try {
                                                        midiService.addDeviceLocked(2, numInputPorts, numOutputPorts, (String[]) inputPortNames.toArray(EMPTY_STRING_ARRAY), (String[]) outputPortNames3.toArray(EMPTY_STRING_ARRAY), properties, null, serviceInfo2, isPrivate, uid);
                                                        properties = null;
                                                        inputPortNames.clear();
                                                        outputPortNames4.clear();
                                                        outputPortNames2 = outputPortNames4;
                                                        outputPortNames = this;
                                                    } catch (Throwable th2) {
                                                        th = th2;
                                                        throw th;
                                                    }
                                                } catch (Throwable th3) {
                                                    th = th3;
                                                    hashMap2 = hashMap;
                                                    str = value;
                                                    i = eventType;
                                                    outputPortNames4 = outputPortNames3;
                                                    throw th;
                                                }
                                            }
                                        } catch (NameNotFoundException e2) {
                                            str = value;
                                            i = eventType;
                                            outputPortNames4 = outputPortNames3;
                                            str2 = TAG;
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("could not fetch ApplicationInfo for ");
                                            stringBuilder2.append(serviceInfo2.packageName);
                                            Log.e(str2, stringBuilder2.toString());
                                        }
                                    }
                                }
                            }
                            outputPortNames4 = outputPortNames3;
                            outputPortNames2 = outputPortNames4;
                            outputPortNames = this;
                        }
                        outputPortNames4 = outputPortNames3;
                        outputPortNames2 = outputPortNames4;
                        outputPortNames = this;
                    }
                    if (parser2 != null) {
                        parser2.close();
                    }
                }
                tagName = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Skipping MIDI device service ");
                stringBuilder.append(serviceInfo2.packageName);
                stringBuilder.append(": it does not require the permission ");
                stringBuilder.append("android.permission.BIND_MIDI_DEVICE_SERVICE");
                Log.w(tagName, stringBuilder.toString());
                if (parser2 != null) {
                    parser2.close();
                }
            } catch (Exception e3) {
                e = e3;
                parser = parser2;
            } catch (Throwable th4) {
                th = th4;
                if (parser2 != null) {
                }
                throw th;
            }
        } catch (Exception e4) {
            e = e4;
            try {
                portName = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Unable to load component info ");
                stringBuilder3.append(serviceInfo.toString());
                Log.w(portName, stringBuilder3.toString(), e);
                if (parser != null) {
                    parser.close();
                }
                parser2 = parser;
            } catch (Throwable th5) {
                th = th5;
                parser2 = parser;
                if (parser2 != null) {
                    parser2.close();
                }
                throw th;
            }
        }
    }

    private void removePackageDeviceServers(String packageName) {
        synchronized (this.mDevicesByInfo) {
            Iterator<Device> iterator = this.mDevicesByInfo.values().iterator();
            while (iterator.hasNext()) {
                Device device = (Device) iterator.next();
                if (packageName.equals(device.getPackageName())) {
                    iterator.remove();
                    removeDeviceLocked(device);
                }
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, writer)) {
            IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
            pw.println("MIDI Manager State:");
            pw.increaseIndent();
            pw.println("Devices:");
            pw.increaseIndent();
            synchronized (this.mDevicesByInfo) {
                for (Device device : this.mDevicesByInfo.values()) {
                    pw.println(device.toString());
                }
            }
            pw.decreaseIndent();
            pw.println("Clients:");
            pw.increaseIndent();
            synchronized (this.mClients) {
                for (Client client : this.mClients.values()) {
                    pw.println(client.toString());
                }
            }
            pw.decreaseIndent();
        }
    }
}
