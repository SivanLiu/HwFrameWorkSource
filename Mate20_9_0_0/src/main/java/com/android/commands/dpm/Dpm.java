package com.android.commands.dpm;

import android.app.ActivityManager;
import android.app.admin.IDevicePolicyManager;
import android.app.admin.IDevicePolicyManager.Stub;
import android.content.ComponentName;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.os.BaseCommand;
import java.io.PrintStream;

public final class Dpm extends BaseCommand {
    private static final String COMMAND_CLEAR_FREEZE_PERIOD_RECORD = "clear-freeze-period-record";
    private static final String COMMAND_FORCE_SECURITY_LOGS = "force-security-logs";
    private static final String COMMAND_REMOVE_ACTIVE_ADMIN = "remove-active-admin";
    private static final String COMMAND_SET_ACTIVE_ADMIN = "set-active-admin";
    private static final String COMMAND_SET_DEVICE_OWNER = "set-device-owner";
    private static final String COMMAND_SET_PROFILE_OWNER = "set-profile-owner";
    private ComponentName mComponent = null;
    private IDevicePolicyManager mDevicePolicyManager;
    private String mName = "";
    private int mUserId = 0;

    public static void main(String[] args) {
        new Dpm().run(args);
    }

    public void onShowUsage(PrintStream out) {
        out.println("usage: dpm [subcommand] [options]\nusage: dpm set-active-admin [ --user <USER_ID> | current ] <COMPONENT>\nusage: dpm set-device-owner [ --user <USER_ID> | current *EXPERIMENTAL* ] [ --name <NAME> ] <COMPONENT>\nusage: dpm set-profile-owner [ --user <USER_ID> | current ] [ --name <NAME> ] <COMPONENT>\nusage: dpm remove-active-admin [ --user <USER_ID> | current ] [ --name <NAME> ] <COMPONENT>\n\ndpm set-active-admin: Sets the given component as active admin for an existing user.\n\ndpm set-device-owner: Sets the given component as active admin, and its package as device owner.\n\ndpm set-profile-owner: Sets the given component as active admin and profile owner for an existing user.\n\ndpm remove-active-admin: Disables an active admin, the admin must have declared android:testOnly in the application in its manifest. This will also remove device and profile owners.\n\ndpm clear-freeze-period-record: clears framework-maintained record of past freeze periods that the device went through. For use during feature development to prevent triggering restriction on setting freeze periods.\n\ndpm force-security-logs: makes all security logs available to the DPC and triggers DeviceAdminReceiver.onSecurityLogsAvailable() if needed.");
    }

    public void onRun() throws Exception {
        this.mDevicePolicyManager = Stub.asInterface(ServiceManager.getService("device_policy"));
        if (this.mDevicePolicyManager == null) {
            showError("Error: Could not access the Device Policy Manager. Is the system running?");
            return;
        }
        String command = nextArgRequired();
        Object obj = -1;
        switch (command.hashCode()) {
            case -1791908857:
                if (command.equals(COMMAND_SET_DEVICE_OWNER)) {
                    obj = 1;
                    break;
                }
                break;
            case -776610703:
                if (command.equals(COMMAND_REMOVE_ACTIVE_ADMIN)) {
                    obj = 3;
                    break;
                }
                break;
            case -536624985:
                if (command.equals(COMMAND_CLEAR_FREEZE_PERIOD_RECORD)) {
                    obj = 4;
                    break;
                }
                break;
            case 547934547:
                if (command.equals(COMMAND_SET_ACTIVE_ADMIN)) {
                    obj = null;
                    break;
                }
                break;
            case 639813476:
                if (command.equals(COMMAND_SET_PROFILE_OWNER)) {
                    obj = 2;
                    break;
                }
                break;
            case 1325530298:
                if (command.equals(COMMAND_FORCE_SECURITY_LOGS)) {
                    obj = 5;
                    break;
                }
                break;
        }
        switch (obj) {
            case null:
                runSetActiveAdmin();
                break;
            case 1:
                runSetDeviceOwner();
                break;
            case 2:
                runSetProfileOwner();
                break;
            case 3:
                runRemoveActiveAdmin();
                break;
            case 4:
                runClearFreezePeriodRecord();
                break;
            case 5:
                runForceSecurityLogs();
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unknown command '");
                stringBuilder.append(command);
                stringBuilder.append("'");
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private void runForceSecurityLogs() throws RemoteException, InterruptedException {
        while (true) {
            long toWait = this.mDevicePolicyManager.forceSecurityLogs();
            if (toWait == 0) {
                System.out.println("Success");
                return;
            }
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("We have to wait for ");
            stringBuilder.append(toWait);
            stringBuilder.append(" milliseconds...");
            printStream.println(stringBuilder.toString());
            Thread.sleep(toWait);
        }
    }

    private void parseArgs(boolean canHaveName) {
        String opt;
        StringBuilder stringBuilder;
        while (true) {
            String nextOption = nextOption();
            opt = nextOption;
            if (nextOption == null) {
                this.mComponent = parseComponentName(nextArgRequired());
                return;
            } else if ("--user".equals(opt)) {
                nextOption = nextArgRequired();
                if ("current".equals(nextOption) || "cur".equals(nextOption)) {
                    this.mUserId = -2;
                } else {
                    this.mUserId = parseInt(nextOption);
                }
                if (this.mUserId == -2) {
                    try {
                        this.mUserId = ActivityManager.getService().getCurrentUser().id;
                    } catch (RemoteException e) {
                        e.rethrowAsRuntimeException();
                    }
                }
            } else if (canHaveName && "--name".equals(opt)) {
                this.mName = nextArgRequired();
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown option: ");
                stringBuilder.append(opt);
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown option: ");
        stringBuilder.append(opt);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private void runSetActiveAdmin() throws RemoteException {
        parseArgs(false);
        this.mDevicePolicyManager.setActiveAdmin(this.mComponent, true, this.mUserId);
        PrintStream printStream = System.out;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Success: Active admin set to component ");
        stringBuilder.append(this.mComponent.toShortString());
        printStream.println(stringBuilder.toString());
    }

    private void runSetDeviceOwner() throws RemoteException {
        parseArgs(true);
        this.mDevicePolicyManager.setActiveAdmin(this.mComponent, true, this.mUserId);
        try {
            StringBuilder stringBuilder;
            if (this.mDevicePolicyManager.setDeviceOwner(this.mComponent, this.mName, this.mUserId)) {
                this.mDevicePolicyManager.setUserProvisioningState(3, this.mUserId);
                PrintStream printStream = System.out;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Success: Device owner set to package ");
                stringBuilder.append(this.mComponent);
                printStream.println(stringBuilder.toString());
                printStream = System.out;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Active admin set to component ");
                stringBuilder.append(this.mComponent.toShortString());
                printStream.println(stringBuilder.toString());
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Can't set package ");
            stringBuilder.append(this.mComponent);
            stringBuilder.append(" as device owner.");
            throw new RuntimeException(stringBuilder.toString());
        } catch (Exception e) {
            this.mDevicePolicyManager.removeActiveAdmin(this.mComponent, 0);
            throw e;
        }
    }

    private void runRemoveActiveAdmin() throws RemoteException {
        parseArgs(false);
        this.mDevicePolicyManager.forceRemoveActiveAdmin(this.mComponent, this.mUserId);
        PrintStream printStream = System.out;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Success: Admin removed ");
        stringBuilder.append(this.mComponent);
        printStream.println(stringBuilder.toString());
    }

    private void runSetProfileOwner() throws RemoteException {
        parseArgs(true);
        this.mDevicePolicyManager.setActiveAdmin(this.mComponent, true, this.mUserId);
        try {
            StringBuilder stringBuilder;
            if (this.mDevicePolicyManager.setProfileOwner(this.mComponent, this.mName, this.mUserId)) {
                this.mDevicePolicyManager.setUserProvisioningState(3, this.mUserId);
                PrintStream printStream = System.out;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Success: Active admin and profile owner set to ");
                stringBuilder.append(this.mComponent.toShortString());
                stringBuilder.append(" for user ");
                stringBuilder.append(this.mUserId);
                printStream.println(stringBuilder.toString());
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Can't set component ");
            stringBuilder.append(this.mComponent.toShortString());
            stringBuilder.append(" as profile owner for user ");
            stringBuilder.append(this.mUserId);
            throw new RuntimeException(stringBuilder.toString());
        } catch (Exception e) {
            this.mDevicePolicyManager.removeActiveAdmin(this.mComponent, this.mUserId);
            throw e;
        }
    }

    private void runClearFreezePeriodRecord() throws RemoteException {
        this.mDevicePolicyManager.clearSystemUpdatePolicyFreezePeriodRecord();
        System.out.println("Success");
    }

    private ComponentName parseComponentName(String component) {
        ComponentName cn = ComponentName.unflattenFromString(component);
        if (cn != null) {
            return cn;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid component ");
        stringBuilder.append(component);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private int parseInt(String argument) {
        try {
            return Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid integer argument '");
            stringBuilder.append(argument);
            stringBuilder.append("'");
            throw new IllegalArgumentException(stringBuilder.toString(), e);
        }
    }
}
