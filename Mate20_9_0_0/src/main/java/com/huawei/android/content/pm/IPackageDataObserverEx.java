package com.huawei.android.content.pm;

import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageDataObserver.Stub;
import android.os.RemoteException;

public class IPackageDataObserverEx {
    public IPackageDataObserver mIPackageDataObserver = new Stub() {
        public void onRemoveCompleted(String packageName, boolean succeeded) throws RemoteException {
            IPackageDataObserverEx.this.onRemoveCompleted(packageName, succeeded);
        }
    };

    public void onRemoveCompleted(String packageName, boolean succeeded) throws RemoteException {
    }

    public IPackageDataObserver getIPackageDataObserver() {
        return this.mIPackageDataObserver;
    }
}
