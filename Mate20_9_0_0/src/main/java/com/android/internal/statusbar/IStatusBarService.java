package com.android.internal.statusbar;

import android.content.ComponentName;
import android.graphics.Rect;
import android.hardware.biometrics.IBiometricPromptReceiver;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;

public interface IStatusBarService extends IInterface {

    public static abstract class Stub extends Binder implements IStatusBarService {
        private static final String DESCRIPTOR = "com.android.internal.statusbar.IStatusBarService";
        static final int TRANSACTION_addTile = 33;
        static final int TRANSACTION_clearNotificationEffects = 16;
        static final int TRANSACTION_clickTile = 35;
        static final int TRANSACTION_collapsePanels = 2;
        static final int TRANSACTION_disable = 4;
        static final int TRANSACTION_disable2 = 6;
        static final int TRANSACTION_disable2ForUser = 7;
        static final int TRANSACTION_disableForUser = 5;
        static final int TRANSACTION_expandNotificationsPanel = 1;
        static final int TRANSACTION_expandSettingsPanel = 12;
        static final int TRANSACTION_handleSystemKey = 36;
        static final int TRANSACTION_hideFingerprintDialog = 43;
        static final int TRANSACTION_isNotificationsPanelExpand = 44;
        static final int TRANSACTION_onClearAllNotifications = 20;
        static final int TRANSACTION_onFingerprintAuthenticated = 40;
        static final int TRANSACTION_onFingerprintError = 42;
        static final int TRANSACTION_onFingerprintHelp = 41;
        static final int TRANSACTION_onGlobalActionsHidden = 30;
        static final int TRANSACTION_onGlobalActionsShown = 29;
        static final int TRANSACTION_onNotificationActionClick = 18;
        static final int TRANSACTION_onNotificationClear = 21;
        static final int TRANSACTION_onNotificationClick = 17;
        static final int TRANSACTION_onNotificationDirectReplied = 24;
        static final int TRANSACTION_onNotificationError = 19;
        static final int TRANSACTION_onNotificationExpansionChanged = 23;
        static final int TRANSACTION_onNotificationSettingsViewed = 27;
        static final int TRANSACTION_onNotificationSmartRepliesAdded = 25;
        static final int TRANSACTION_onNotificationSmartReplySent = 26;
        static final int TRANSACTION_onNotificationVisibilityChanged = 22;
        static final int TRANSACTION_onPanelHidden = 15;
        static final int TRANSACTION_onPanelRevealed = 14;
        static final int TRANSACTION_reboot = 32;
        static final int TRANSACTION_registerStatusBar = 13;
        static final int TRANSACTION_remTile = 34;
        static final int TRANSACTION_removeIcon = 10;
        static final int TRANSACTION_setIcon = 8;
        static final int TRANSACTION_setIconVisibility = 9;
        static final int TRANSACTION_setImeWindowStatus = 11;
        static final int TRANSACTION_setSystemUiVisibility = 28;
        static final int TRANSACTION_showFingerprintDialog = 39;
        static final int TRANSACTION_showPinningEnterExitToast = 37;
        static final int TRANSACTION_showPinningEscapeToast = 38;
        static final int TRANSACTION_shutdown = 31;
        static final int TRANSACTION_togglePanel = 3;

        private static class Proxy implements IStatusBarService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            public void expandNotificationsPanel() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void collapsePanels() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void togglePanel() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void disable(int what, IBinder token, String pkg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(what);
                    _data.writeStrongBinder(token);
                    _data.writeString(pkg);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void disableForUser(int what, IBinder token, String pkg, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(what);
                    _data.writeStrongBinder(token);
                    _data.writeString(pkg);
                    _data.writeInt(userId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void disable2(int what, IBinder token, String pkg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(what);
                    _data.writeStrongBinder(token);
                    _data.writeString(pkg);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void disable2ForUser(int what, IBinder token, String pkg, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(what);
                    _data.writeStrongBinder(token);
                    _data.writeString(pkg);
                    _data.writeInt(userId);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setIcon(String slot, String iconPackage, int iconId, int iconLevel, String contentDescription) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(slot);
                    _data.writeString(iconPackage);
                    _data.writeInt(iconId);
                    _data.writeInt(iconLevel);
                    _data.writeString(contentDescription);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setIconVisibility(String slot, boolean visible) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(slot);
                    _data.writeInt(visible);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeIcon(String slot) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(slot);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setImeWindowStatus(IBinder token, int vis, int backDisposition, boolean showImeSwitcher) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(vis);
                    _data.writeInt(backDisposition);
                    _data.writeInt(showImeSwitcher);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void expandSettingsPanel(String subPanel) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(subPanel);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void registerStatusBar(IStatusBar callbacks, List<String> iconSlots, List<StatusBarIcon> iconList, int[] switches, List<IBinder> binders, Rect fullscreenStackBounds, Rect dockedStackBounds) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callbacks != null ? callbacks.asBinder() : null);
                    if (switches == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(switches.length);
                    }
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    _reply.readStringList(iconSlots);
                    _reply.readTypedList(iconList, StatusBarIcon.CREATOR);
                    _reply.readIntArray(switches);
                    _reply.readBinderList(binders);
                    if (_reply.readInt() != 0) {
                        fullscreenStackBounds.readFromParcel(_reply);
                    }
                    if (_reply.readInt() != 0) {
                        dockedStackBounds.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onPanelRevealed(boolean clearNotificationEffects, int numItems) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(clearNotificationEffects);
                    _data.writeInt(numItems);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onPanelHidden() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearNotificationEffects() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onNotificationClick(String key, NotificationVisibility nv) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(key);
                    if (nv != null) {
                        _data.writeInt(1);
                        nv.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onNotificationActionClick(String key, int actionIndex, NotificationVisibility nv) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(key);
                    _data.writeInt(actionIndex);
                    if (nv != null) {
                        _data.writeInt(1);
                        nv.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onNotificationError(String pkg, String tag, int id, int uid, int initialPid, String message, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeString(tag);
                    _data.writeInt(id);
                    _data.writeInt(uid);
                    _data.writeInt(initialPid);
                    _data.writeString(message);
                    _data.writeInt(userId);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onClearAllNotifications(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onNotificationClear(String pkg, String tag, int id, int userId, String key, int dismissalSurface, NotificationVisibility nv) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeString(tag);
                    _data.writeInt(id);
                    _data.writeInt(userId);
                    _data.writeString(key);
                    _data.writeInt(dismissalSurface);
                    if (nv != null) {
                        _data.writeInt(1);
                        nv.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onNotificationVisibilityChanged(NotificationVisibility[] newlyVisibleKeys, NotificationVisibility[] noLongerVisibleKeys) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedArray(newlyVisibleKeys, 0);
                    _data.writeTypedArray(noLongerVisibleKeys, 0);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onNotificationExpansionChanged(String key, boolean userAction, boolean expanded) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(key);
                    _data.writeInt(userAction);
                    _data.writeInt(expanded);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onNotificationDirectReplied(String key) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(key);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onNotificationSmartRepliesAdded(String key, int replyCount) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(key);
                    _data.writeInt(replyCount);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onNotificationSmartReplySent(String key, int replyIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(key);
                    _data.writeInt(replyIndex);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onNotificationSettingsViewed(String key) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(key);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setSystemUiVisibility(int vis, int mask, String cause) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(vis);
                    _data.writeInt(mask);
                    _data.writeString(cause);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onGlobalActionsShown() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onGlobalActionsHidden() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void shutdown() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reboot(boolean safeMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(safeMode);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void addTile(ComponentName tile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (tile != null) {
                        _data.writeInt(1);
                        tile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void remTile(ComponentName tile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (tile != null) {
                        _data.writeInt(1);
                        tile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clickTile(ComponentName tile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (tile != null) {
                        _data.writeInt(1);
                        tile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void handleSystemKey(int key) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(key);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void showPinningEnterExitToast(boolean entering) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(entering);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void showPinningEscapeToast() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void showFingerprintDialog(Bundle bundle, IBiometricPromptReceiver receiver) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (bundle != null) {
                        _data.writeInt(1);
                        bundle.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(receiver != null ? receiver.asBinder() : null);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onFingerprintAuthenticated() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onFingerprintHelp(String message) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(message);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onFingerprintError(String error) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(error);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void hideFingerprintDialog() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isNotificationsPanelExpand() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IStatusBarService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IStatusBarService)) {
                return new Proxy(obj);
            }
            return (IStatusBarService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            String descriptor = DESCRIPTOR;
            boolean z;
            if (i != 1598968902) {
                boolean _arg3 = false;
                NotificationVisibility _arg2 = null;
                String _arg0;
                int _arg1;
                String _arg02;
                ComponentName _arg03;
                switch (i) {
                    case 1:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        expandNotificationsPanel();
                        reply.writeNoException();
                        return z;
                    case 2:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        collapsePanels();
                        reply.writeNoException();
                        return z;
                    case 3:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        togglePanel();
                        reply.writeNoException();
                        return z;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        disable(data.readInt(), data.readStrongBinder(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        disableForUser(data.readInt(), data.readStrongBinder(), data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        disable2(data.readInt(), data.readStrongBinder(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        disable2ForUser(data.readInt(), data.readStrongBinder(), data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        setIcon(data.readString(), data.readString(), data.readInt(), data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readString();
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        setIconVisibility(_arg0, _arg3);
                        reply.writeNoException();
                        return true;
                    case 10:
                        boolean z2 = true;
                        parcel.enforceInterface(descriptor);
                        removeIcon(data.readString());
                        reply.writeNoException();
                        return z2;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        IBinder _arg04 = data.readStrongBinder();
                        _arg1 = data.readInt();
                        int _arg22 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        setImeWindowStatus(_arg04, _arg1, _arg22, _arg3);
                        reply.writeNoException();
                        return true;
                    case 12:
                        boolean z3 = true;
                        parcel.enforceInterface(descriptor);
                        expandSettingsPanel(data.readString());
                        reply.writeNoException();
                        return z3;
                    case 13:
                        int[] _arg32;
                        parcel.enforceInterface(descriptor);
                        IStatusBar _arg05 = com.android.internal.statusbar.IStatusBar.Stub.asInterface(data.readStrongBinder());
                        List _arg12 = new ArrayList();
                        List<StatusBarIcon> _arg23 = new ArrayList();
                        int _arg3_length = data.readInt();
                        if (_arg3_length < 0) {
                            _arg32 = null;
                        } else {
                            _arg32 = new int[_arg3_length];
                        }
                        int[] _arg33 = _arg32;
                        List<IBinder> _arg4 = new ArrayList();
                        Rect _arg5 = new Rect();
                        Rect _arg6 = new Rect();
                        Rect _arg52 = _arg5;
                        List<IBinder> _arg42 = _arg4;
                        int[] _arg34 = _arg33;
                        List<StatusBarIcon> _arg24 = _arg23;
                        registerStatusBar(_arg05, _arg12, _arg23, _arg33, _arg42, _arg52, _arg6);
                        reply.writeNoException();
                        parcel2.writeStringList(_arg12);
                        parcel2.writeTypedList(_arg24);
                        parcel2.writeIntArray(_arg34);
                        parcel2.writeBinderList(_arg42);
                        parcel2.writeInt(1);
                        _arg52.writeToParcel(parcel2, 1);
                        parcel2.writeInt(1);
                        _arg6.writeToParcel(parcel2, 1);
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        onPanelRevealed(_arg3, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        onPanelHidden();
                        reply.writeNoException();
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        clearNotificationEffects();
                        reply.writeNoException();
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readString();
                        if (data.readInt() != 0) {
                            _arg2 = (NotificationVisibility) NotificationVisibility.CREATOR.createFromParcel(parcel);
                        }
                        onNotificationClick(_arg02, _arg2);
                        reply.writeNoException();
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readString();
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg2 = (NotificationVisibility) NotificationVisibility.CREATOR.createFromParcel(parcel);
                        }
                        onNotificationActionClick(_arg02, _arg1, _arg2);
                        reply.writeNoException();
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        onNotificationError(data.readString(), data.readString(), data.readInt(), data.readInt(), data.readInt(), data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        onClearAllNotifications(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 21:
                        NotificationVisibility _arg62;
                        parcel.enforceInterface(descriptor);
                        String _arg06 = data.readString();
                        String _arg13 = data.readString();
                        int _arg25 = data.readInt();
                        int _arg35 = data.readInt();
                        String _arg43 = data.readString();
                        int _arg53 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg62 = (NotificationVisibility) NotificationVisibility.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg62 = null;
                        }
                        onNotificationClear(_arg06, _arg13, _arg25, _arg35, _arg43, _arg53, _arg62);
                        reply.writeNoException();
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        onNotificationVisibilityChanged((NotificationVisibility[]) parcel.createTypedArray(NotificationVisibility.CREATOR), (NotificationVisibility[]) parcel.createTypedArray(NotificationVisibility.CREATOR));
                        reply.writeNoException();
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readString();
                        boolean _arg14 = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        onNotificationExpansionChanged(_arg0, _arg14, _arg3);
                        reply.writeNoException();
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        onNotificationDirectReplied(data.readString());
                        reply.writeNoException();
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        onNotificationSmartRepliesAdded(data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 26:
                        parcel.enforceInterface(descriptor);
                        onNotificationSmartReplySent(data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 27:
                        parcel.enforceInterface(descriptor);
                        onNotificationSettingsViewed(data.readString());
                        reply.writeNoException();
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        setSystemUiVisibility(data.readInt(), data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        onGlobalActionsShown();
                        reply.writeNoException();
                        return true;
                    case 30:
                        parcel.enforceInterface(descriptor);
                        onGlobalActionsHidden();
                        reply.writeNoException();
                        return true;
                    case 31:
                        parcel.enforceInterface(descriptor);
                        shutdown();
                        reply.writeNoException();
                        return true;
                    case 32:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        reboot(_arg3);
                        reply.writeNoException();
                        return true;
                    case 33:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        addTile(_arg03);
                        reply.writeNoException();
                        return true;
                    case 34:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        remTile(_arg03);
                        reply.writeNoException();
                        return true;
                    case 35:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        clickTile(_arg03);
                        reply.writeNoException();
                        return true;
                    case 36:
                        parcel.enforceInterface(descriptor);
                        handleSystemKey(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 37:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        showPinningEnterExitToast(_arg3);
                        reply.writeNoException();
                        return true;
                    case 38:
                        parcel.enforceInterface(descriptor);
                        showPinningEscapeToast();
                        reply.writeNoException();
                        return true;
                    case 39:
                        Bundle _arg07;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg07 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        showFingerprintDialog(_arg07, android.hardware.biometrics.IBiometricPromptReceiver.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 40:
                        parcel.enforceInterface(descriptor);
                        onFingerprintAuthenticated();
                        reply.writeNoException();
                        return true;
                    case 41:
                        parcel.enforceInterface(descriptor);
                        onFingerprintHelp(data.readString());
                        reply.writeNoException();
                        return true;
                    case 42:
                        parcel.enforceInterface(descriptor);
                        onFingerprintError(data.readString());
                        reply.writeNoException();
                        return true;
                    case 43:
                        parcel.enforceInterface(descriptor);
                        hideFingerprintDialog();
                        reply.writeNoException();
                        return true;
                    case 44:
                        parcel.enforceInterface(descriptor);
                        _arg3 = isNotificationsPanelExpand();
                        reply.writeNoException();
                        parcel2.writeInt(_arg3);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            z = true;
            parcel2.writeString(descriptor);
            return z;
        }
    }

    void addTile(ComponentName componentName) throws RemoteException;

    void clearNotificationEffects() throws RemoteException;

    void clickTile(ComponentName componentName) throws RemoteException;

    void collapsePanels() throws RemoteException;

    void disable(int i, IBinder iBinder, String str) throws RemoteException;

    void disable2(int i, IBinder iBinder, String str) throws RemoteException;

    void disable2ForUser(int i, IBinder iBinder, String str, int i2) throws RemoteException;

    void disableForUser(int i, IBinder iBinder, String str, int i2) throws RemoteException;

    void expandNotificationsPanel() throws RemoteException;

    void expandSettingsPanel(String str) throws RemoteException;

    void handleSystemKey(int i) throws RemoteException;

    void hideFingerprintDialog() throws RemoteException;

    boolean isNotificationsPanelExpand() throws RemoteException;

    void onClearAllNotifications(int i) throws RemoteException;

    void onFingerprintAuthenticated() throws RemoteException;

    void onFingerprintError(String str) throws RemoteException;

    void onFingerprintHelp(String str) throws RemoteException;

    void onGlobalActionsHidden() throws RemoteException;

    void onGlobalActionsShown() throws RemoteException;

    void onNotificationActionClick(String str, int i, NotificationVisibility notificationVisibility) throws RemoteException;

    void onNotificationClear(String str, String str2, int i, int i2, String str3, int i3, NotificationVisibility notificationVisibility) throws RemoteException;

    void onNotificationClick(String str, NotificationVisibility notificationVisibility) throws RemoteException;

    void onNotificationDirectReplied(String str) throws RemoteException;

    void onNotificationError(String str, String str2, int i, int i2, int i3, String str3, int i4) throws RemoteException;

    void onNotificationExpansionChanged(String str, boolean z, boolean z2) throws RemoteException;

    void onNotificationSettingsViewed(String str) throws RemoteException;

    void onNotificationSmartRepliesAdded(String str, int i) throws RemoteException;

    void onNotificationSmartReplySent(String str, int i) throws RemoteException;

    void onNotificationVisibilityChanged(NotificationVisibility[] notificationVisibilityArr, NotificationVisibility[] notificationVisibilityArr2) throws RemoteException;

    void onPanelHidden() throws RemoteException;

    void onPanelRevealed(boolean z, int i) throws RemoteException;

    void reboot(boolean z) throws RemoteException;

    void registerStatusBar(IStatusBar iStatusBar, List<String> list, List<StatusBarIcon> list2, int[] iArr, List<IBinder> list3, Rect rect, Rect rect2) throws RemoteException;

    void remTile(ComponentName componentName) throws RemoteException;

    void removeIcon(String str) throws RemoteException;

    void setIcon(String str, String str2, int i, int i2, String str3) throws RemoteException;

    void setIconVisibility(String str, boolean z) throws RemoteException;

    void setImeWindowStatus(IBinder iBinder, int i, int i2, boolean z) throws RemoteException;

    void setSystemUiVisibility(int i, int i2, String str) throws RemoteException;

    void showFingerprintDialog(Bundle bundle, IBiometricPromptReceiver iBiometricPromptReceiver) throws RemoteException;

    void showPinningEnterExitToast(boolean z) throws RemoteException;

    void showPinningEscapeToast() throws RemoteException;

    void shutdown() throws RemoteException;

    void togglePanel() throws RemoteException;
}
