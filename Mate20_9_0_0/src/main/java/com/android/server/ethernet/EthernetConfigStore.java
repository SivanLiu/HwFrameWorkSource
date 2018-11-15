package com.android.server.ethernet;

import android.net.IpConfiguration;
import android.os.Environment;
import android.util.ArrayMap;
import com.android.server.net.IpConfigStore;

public class EthernetConfigStore {
    private static final String ipConfigFile;
    private IpConfiguration mIpConfigurationForDefaultInterface;
    private ArrayMap<String, IpConfiguration> mIpConfigurations = new ArrayMap(0);
    private IpConfigStore mStore = new IpConfigStore();
    private final Object mSync = new Object();

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Environment.getDataDirectory());
        stringBuilder.append("/misc/ethernet/ipconfig.txt");
        ipConfigFile = stringBuilder.toString();
    }

    public void read() {
        synchronized (this.mSync) {
            ArrayMap<String, IpConfiguration> configs = IpConfigStore.readIpConfigurations(ipConfigFile);
            if (configs.containsKey("0")) {
                this.mIpConfigurationForDefaultInterface = (IpConfiguration) configs.remove("0");
            }
            this.mIpConfigurations = configs;
        }
    }

    public void write(String iface, IpConfiguration config) {
        synchronized (this.mSync) {
            boolean modified = true;
            if (config != null) {
                modified = true ^ config.equals((IpConfiguration) this.mIpConfigurations.put(iface, config));
            } else if (this.mIpConfigurations.remove(iface) == null) {
                modified = false;
            }
            if (modified) {
                this.mStore.writeIpConfigurations(ipConfigFile, this.mIpConfigurations);
            }
        }
    }

    public ArrayMap<String, IpConfiguration> getIpConfigurations() {
        ArrayMap<String, IpConfiguration> arrayMap;
        synchronized (this.mSync) {
            arrayMap = new ArrayMap(this.mIpConfigurations);
        }
        return arrayMap;
    }

    public IpConfiguration getIpConfigurationForDefaultInterface() {
        IpConfiguration ipConfiguration;
        synchronized (this.mSync) {
            ipConfiguration = this.mIpConfigurationForDefaultInterface == null ? null : new IpConfiguration(this.mIpConfigurationForDefaultInterface);
        }
        return ipConfiguration;
    }
}
