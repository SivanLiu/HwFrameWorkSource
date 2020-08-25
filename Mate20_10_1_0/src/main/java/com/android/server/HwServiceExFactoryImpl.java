package com.android.server;

import android.app.job.JobService;
import android.content.Context;
import android.os.Handler;
import com.android.server.HwServiceExFactory;
import com.android.server.am.BroadcastDispatcher;
import com.android.server.am.HwActivityManagerServiceEx;
import com.android.server.am.HwBroadcastQueue;
import com.android.server.am.HwBroadcastQueueEx;
import com.android.server.am.IHwActivityManagerInner;
import com.android.server.am.IHwActivityManagerServiceEx;
import com.android.server.am.IHwBroadcastQueueEx;
import com.android.server.audio.HwAudioServiceEx;
import com.android.server.audio.IHwAudioServiceEx;
import com.android.server.audio.IHwAudioServiceInner;
import com.android.server.connectivity.IHwConnectivityServiceInner;
import com.android.server.display.HwDisplayManagerServiceEx;
import com.android.server.display.HwDisplayPowerControllerEx;
import com.android.server.display.IHwDisplayManagerInner;
import com.android.server.display.IHwDisplayManagerServiceEx;
import com.android.server.display.IHwDisplayPowerControllerEx;
import com.android.server.imm.HwInputMethodManagerServiceEx;
import com.android.server.imm.IHwInputMethodManagerInner;
import com.android.server.imm.IHwInputMethodManagerServiceEx;
import com.android.server.input.HwInputManagerServiceEx;
import com.android.server.input.IHwInputManagerInner;
import com.android.server.input.IHwInputManagerServiceEx;
import com.android.server.media.projection.HwMediaProjectionManagerServiceEx;
import com.android.server.media.projection.IHwMediaProjectionManagerServiceInner;
import com.android.server.net.HwNetworkStatsServiceEx;
import com.android.server.net.IHwNetworkStatsInner;
import com.android.server.net.IHwNetworkStatsServiceEx;
import com.android.server.notification.HwNotificationManagerServiceEx;
import com.android.server.notification.IHwNotificationManagerServiceEx;
import com.android.server.pm.HwBackgroundDexOptServiceEx;
import com.android.server.pm.HwPackageManagerServiceEx;
import com.android.server.pm.HwPluginPackage;
import com.android.server.pm.IHwBackgroundDexOptInner;
import com.android.server.pm.IHwBackgroundDexOptServiceEx;
import com.android.server.pm.IHwPackageManagerInner;
import com.android.server.pm.IHwPackageManagerServiceEx;
import com.android.server.pm.IHwPluginPackage;
import com.android.server.policy.HwPhoneWindowManagerEx;
import com.android.server.policy.IHwPhoneWindowManagerEx;
import com.android.server.policy.IHwPhoneWindowManagerInner;
import com.android.server.power.HwPowerManagerServiceEx;
import com.android.server.power.IHwPowerManagerInner;
import com.android.server.power.IHwPowerManagerServiceEx;
import com.android.server.usb.HwUsbDeviceManagerEx;
import com.android.server.usb.HwUsbUserSettingsManagerEx;
import com.android.server.usb.IHwUsbDeviceManagerEx;
import com.android.server.usb.IHwUsbDeviceManagerInner;
import com.android.server.usb.IHwUsbUserSettingsManagerEx;
import com.android.server.usb.IHwUsbUserSettingsManagerInner;
import com.android.server.vr.HwVrManagerServiceEx;
import com.android.server.vr.IHwVrManagerServiceEx;
import com.android.server.wallpaper.IHwWallpaperManagerInner;
import com.android.server.wallpaper.IHwWallpaperManagerServiceEx;
import com.android.server.wm.ActivityStack;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.DisplayContent;
import com.android.server.wm.HwActivityDisplayEx;
import com.android.server.wm.HwActivityStackEx;
import com.android.server.wm.HwActivityStackSupervisorEx;
import com.android.server.wm.HwActivityStarterEx;
import com.android.server.wm.HwActivityTaskManagerServiceEx;
import com.android.server.wm.HwDisplayContentEx;
import com.android.server.wm.HwDisplayPolicyEx;
import com.android.server.wm.HwDisplayRotationEx;
import com.android.server.wm.HwRootActivityContainerEx;
import com.android.server.wm.HwTaskLaunchParamsModifierEx;
import com.android.server.wm.HwTaskPositionerEx;
import com.android.server.wm.HwTaskRecordEx;
import com.android.server.wm.HwTaskSnapshotCacheEx;
import com.android.server.wm.HwTaskStackEx;
import com.android.server.wm.HwWindowManagerServiceEx;
import com.android.server.wm.HwWindowStateEx;
import com.android.server.wm.IHwActivityDisplayEx;
import com.android.server.wm.IHwActivityTaskManagerInner;
import com.android.server.wm.IHwActivityTaskManagerServiceEx;
import com.android.server.wm.IHwDisplayContentEx;
import com.android.server.wm.IHwRootActivityContainerInner;
import com.android.server.wm.IHwTaskPositionerEx;
import com.android.server.wm.IHwTaskSnapshotCacheEx;
import com.android.server.wm.IHwTaskStackEx;
import com.android.server.wm.IHwWindowManagerInner;
import com.android.server.wm.IHwWindowManagerServiceEx;
import com.android.server.wm.IHwWindowStateEx;
import com.android.server.wm.TaskStack;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowState;
import com.huawei.server.connectivity.IHwConnectivityServiceEx;
import com.huawei.server.media.projection.IHwMediaProjectionManagerServiceEx;
import com.huawei.server.wm.IHwActivityStackEx;
import com.huawei.server.wm.IHwActivityStackSupervisorEx;
import com.huawei.server.wm.IHwActivityStarterEx;
import com.huawei.server.wm.IHwDisplayPolicyEx;
import com.huawei.server.wm.IHwDisplayPolicyInner;
import com.huawei.server.wm.IHwDisplayRotationEx;
import com.huawei.server.wm.IHwRootActivityContainerEx;
import com.huawei.server.wm.IHwTaskLaunchParamsModifierEx;
import com.huawei.server.wm.IHwTaskRecordEx;
import huawei.com.android.server.connectivity.HwConnectivityServiceEx;
import huawei.com.android.server.wallpaper.HwWallpaperManagerServiceEx;

public class HwServiceExFactoryImpl implements HwServiceExFactory.Factory {
    private static final String TAG = "HwServiceExFactoryImpl";

    public IHwActivityManagerServiceEx getHwActivityManagerServiceEx(IHwActivityManagerInner ams, Context context) {
        return new HwActivityManagerServiceEx(ams, context);
    }

    public IHwWallpaperManagerServiceEx getHwWallpaperManagerServiceEx(IHwWallpaperManagerInner wms, Context context) {
        return new HwWallpaperManagerServiceEx(wms, context);
    }

    public IHwActivityTaskManagerServiceEx getHwActivityTaskManagerServiceEx(IHwActivityTaskManagerInner atms, Context context) {
        return new HwActivityTaskManagerServiceEx(atms, context);
    }

    public IHwNotificationManagerServiceEx getHwNotificationManagerServiceEx() {
        return new HwNotificationManagerServiceEx();
    }

    public IHwWindowManagerServiceEx getHwWindowManagerServiceEx(IHwWindowManagerInner wms, Context context) {
        return new HwWindowManagerServiceEx(wms, context);
    }

    public IHwPackageManagerServiceEx getHwPackageManagerServiceEx(IHwPackageManagerInner pms, Context context) {
        return new HwPackageManagerServiceEx(pms, context);
    }

    public IHwPluginPackage getHwPluginPackage(IHwPackageManagerInner pms, String packageName) {
        return new HwPluginPackage(pms, packageName);
    }

    public IHwInputMethodManagerServiceEx getHwInputMethodManagerServiceEx(IHwInputMethodManagerInner ims, Context context) {
        return new HwInputMethodManagerServiceEx(ims, context);
    }

    public IHwBackgroundDexOptServiceEx getHwBackgroundDexOptServiceEx(IHwBackgroundDexOptInner bdos, JobService service, Context context) {
        return new HwBackgroundDexOptServiceEx(bdos, service, context);
    }

    public IHwActivityStarterEx getHwActivityStarterEx(ActivityTaskManagerService ams) {
        return new HwActivityStarterEx(ams);
    }

    public IHwAudioServiceEx getHwAudioServiceEx(IHwAudioServiceInner ias, Context context) {
        return new HwAudioServiceEx(ias, context);
    }

    public IHwPowerManagerServiceEx getHwPowerManagerServiceEx(IHwPowerManagerInner pms, Context context) {
        return new HwPowerManagerServiceEx(pms, context);
    }

    public IHwInputManagerServiceEx getHwInputManagerServiceEx(IHwInputManagerInner ims, Context context) {
        return new HwInputManagerServiceEx(ims, context);
    }

    public IHwDisplayManagerServiceEx getHwDisplayManagerServiceEx(IHwDisplayManagerInner dms, Context context) {
        return new HwDisplayManagerServiceEx(dms, context);
    }

    public IHwTaskPositionerEx getHwTaskPositionerEx(WindowManagerService wms) {
        return new HwTaskPositionerEx(wms);
    }

    public IHwWindowStateEx getHwWindowStateEx(WindowManagerService wms, WindowState windowState) {
        return new HwWindowStateEx(wms, windowState);
    }

    public IHwPhoneWindowManagerEx getHwPhoneWindowManagerEx(IHwPhoneWindowManagerInner pws, Context context) {
        return new HwPhoneWindowManagerEx(pws, context);
    }

    public IHwTaskStackEx getHwTaskStackEx(TaskStack taskStack, WindowManagerService wms) {
        return new HwTaskStackEx(taskStack, wms);
    }

    public IHwActivityStackSupervisorEx getHwActivityStackSupervisorEx(ActivityTaskManagerService ams) {
        return new HwActivityStackSupervisorEx(ams);
    }

    public IHwTaskLaunchParamsModifierEx getHwTaskLaunchParamsModifierEx() {
        return new HwTaskLaunchParamsModifierEx();
    }

    public IHwTaskRecordEx getHwTaskRecordEx() {
        return new HwTaskRecordEx();
    }

    public IHwVrManagerServiceEx getHwVrManagerServiceEx() {
        return new HwVrManagerServiceEx();
    }

    public IHwDisplayPowerControllerEx getHwDisplayPowerControllerEx(Context context, IHwDisplayPowerControllerEx.Callbacks callbacks) {
        return new HwDisplayPowerControllerEx(context, callbacks);
    }

    public IHwDisplayPolicyEx getHwDisplayPolicyEx(WindowManagerService service, IHwDisplayPolicyInner displayPolicy, DisplayContent displayContent, Context context, boolean isDefaultDisplay) {
        return new HwDisplayPolicyEx(service, displayPolicy, displayContent, context, isDefaultDisplay);
    }

    public IHwDisplayRotationEx getHwDisplayRotationEx(WindowManagerService service, DisplayContent displayContent, boolean isDefaultDisplay) {
        return new HwDisplayRotationEx(service, displayContent, isDefaultDisplay);
    }

    public IHwConnectivityServiceEx getHwConnectivityServiceEx(IHwConnectivityServiceInner csi, Context context) {
        return new HwConnectivityServiceEx(csi, context);
    }

    public IHwMediaProjectionManagerServiceEx getHwMediaProjectionManagerServiceEx(IHwMediaProjectionManagerServiceInner mpms, Context context) {
        return new HwMediaProjectionManagerServiceEx(mpms, context);
    }

    public IHwActivityStackEx getHwActivityStackEx(ActivityStack stack, ActivityTaskManagerService service) {
        return new HwActivityStackEx(stack, service);
    }

    public IHwRootActivityContainerEx getHwRootActivityContainerEx(IHwRootActivityContainerInner rac, ActivityTaskManagerService service) {
        return new HwRootActivityContainerEx(rac, service);
    }

    public IHwDisplayContentEx getHwDisplayContentEx() {
        return new HwDisplayContentEx();
    }

    public IHwBroadcastQueueEx getHwBroadcastQueueEx(HwBroadcastQueue bq, BroadcastDispatcher bd, String queueName) {
        return new HwBroadcastQueueEx(bq, bd, queueName);
    }

    public IHwBluetoothManagerServiceEx getHwBluetoothManagerServiceEx(IHwBluetoothManagerInner bms, Context context, Handler handler) {
        return new HwBluetoothManagerServiceEx(bms, context, handler);
    }

    public IHwNetworkStatsServiceEx getHwNetworkStatsServiceEx(IHwNetworkStatsInner hwNetworkStatsInner, Context context) {
        return new HwNetworkStatsServiceEx(hwNetworkStatsInner, context);
    }

    public IHwTaskSnapshotCacheEx getHwTaskSnapshotCacheEx() {
        return new HwTaskSnapshotCacheEx();
    }

    public IHwStorageManagerServiceEx getHwStorageManagerServiceEx(IHwStorageManagerInner sms, Context context) {
        return new HwStorageManagerServiceEx(sms, context);
    }

    public IHwUsbDeviceManagerEx getHwUsbDeviceManagerEx(IHwUsbDeviceManagerInner ums, Context context) {
        return new HwUsbDeviceManagerEx(ums, context);
    }

    public IHwActivityDisplayEx getHwActivityDisplayEx() {
        return new HwActivityDisplayEx();
    }

    public IHwUsbUserSettingsManagerEx getHwUsbUserSettingsManagerEx(IHwUsbUserSettingsManagerInner hwUsbUserSettingsManagerInner, Context context) {
        return new HwUsbUserSettingsManagerEx(hwUsbUserSettingsManagerInner, context);
    }
}
