package com.android.server.pm.permission;

import java.util.ArrayList;

public class DefaultAppPermission {
    ArrayList<DefaultPermissionGroup> mGrantedGroups = new ArrayList();
    ArrayList<DefaultPermissionSingle> mGrantedSingles = new ArrayList();
    String mPackageName;
    boolean mTrust;

    public static class DefaultPermissionGroup {
        boolean mGrant;
        String mName;
        boolean mSystemFixed;

        public DefaultPermissionGroup(String name) {
            this(name, true, true);
        }

        public DefaultPermissionGroup(String name, boolean grant, boolean fixed) {
            this.mName = name;
            this.mGrant = grant;
            this.mSystemFixed = fixed;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Group ");
            stringBuilder.append(this.mName);
            stringBuilder.append(" grant:");
            stringBuilder.append(this.mGrant);
            stringBuilder.append(" fixed:");
            stringBuilder.append(this.mSystemFixed);
            return stringBuilder.toString();
        }
    }

    public static class DefaultPermissionSingle {
        boolean mGrant;
        String mName;
        boolean mSystemFixed;

        public DefaultPermissionSingle(String name) {
            this(name, true, true);
        }

        public DefaultPermissionSingle(String name, boolean grant, boolean fixed) {
            this.mName = name;
            this.mGrant = grant;
            this.mSystemFixed = fixed;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Single Permission ");
            stringBuilder.append(this.mName);
            stringBuilder.append(" grant:");
            stringBuilder.append(this.mGrant);
            stringBuilder.append(" fixed:");
            stringBuilder.append(this.mSystemFixed);
            return stringBuilder.toString();
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Permission for ");
        stringBuilder.append(this.mPackageName);
        stringBuilder.append(", mTrust:");
        stringBuilder.append(this.mTrust);
        return stringBuilder.toString();
    }
}
