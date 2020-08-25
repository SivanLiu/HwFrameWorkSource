package com.android.server.wifi;

import android.os.Build;

class SystemBuildProperties implements BuildProperties {
    SystemBuildProperties() {
    }

    @Override // com.android.server.wifi.BuildProperties
    public boolean isEngBuild() {
        return Build.TYPE.equals("eng");
    }

    @Override // com.android.server.wifi.BuildProperties
    public boolean isUserdebugBuild() {
        return Build.TYPE.equals("userdebug");
    }

    @Override // com.android.server.wifi.BuildProperties
    public boolean isUserBuild() {
        return Build.TYPE.equals("user");
    }
}
