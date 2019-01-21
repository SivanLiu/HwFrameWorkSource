package android.widget;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.graphics.drawable.Drawable;
import android.hwcontrol.HwWidgetFactory.PermissionInformation;
import android.os.Parcel;
import android.os.UserHandle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppSecurityPermissions {
    private static final String TAG = "AppSecurityPermissions";
    public static final int WHICH_ALL = 65535;
    public static final int WHICH_NEW = 4;
    private static final boolean localLOGV = false;
    private final Context mContext;
    private final LayoutInflater mInflater;
    private final CharSequence mNewPermPrefix;
    private String mPackageName;
    private final PermissionInfoComparator mPermComparator;
    private final PermissionGroupInfoComparator mPermGroupComparator;
    private final Map<String, MyPermissionGroupInfo> mPermGroups;
    private final List<MyPermissionGroupInfo> mPermGroupsList;
    private final List<MyPermissionInfo> mPermsList;
    private final PackageManager mPm;

    private static class PermissionGroupInfoComparator implements Comparator<MyPermissionGroupInfo> {
        private final Collator sCollator;

        private PermissionGroupInfoComparator() {
            this.sCollator = Collator.getInstance();
        }

        public final int compare(MyPermissionGroupInfo a, MyPermissionGroupInfo b) {
            return this.sCollator.compare(a.mLabel, b.mLabel);
        }
    }

    private static class PermissionInfoComparator implements Comparator<MyPermissionInfo> {
        private final Collator sCollator = Collator.getInstance();

        PermissionInfoComparator() {
        }

        public final int compare(MyPermissionInfo a, MyPermissionInfo b) {
            return this.sCollator.compare(a.mLabel, b.mLabel);
        }
    }

    static class MyPermissionGroupInfo extends PermissionGroupInfo {
        final ArrayList<MyPermissionInfo> mAllPermissions = new ArrayList();
        CharSequence mLabel;
        final ArrayList<MyPermissionInfo> mNewPermissions = new ArrayList();

        MyPermissionGroupInfo(PermissionInfo perm) {
            this.name = perm.packageName;
            this.packageName = perm.packageName;
        }

        MyPermissionGroupInfo(PermissionGroupInfo info) {
            super(info);
        }

        public Drawable loadGroupIcon(Context context, PackageManager pm) {
            if (this.icon != 0) {
                return loadUnbadgedIcon(pm);
            }
            return context.getDrawable(17302704);
        }
    }

    private static class MyPermissionInfo extends PermissionInfo {
        int mExistingReqFlags;
        CharSequence mLabel;
        boolean mNew;
        int mNewReqFlags;

        MyPermissionInfo(PermissionInfo info) {
            super(info);
        }
    }

    public static class PermissionItemView extends LinearLayout implements OnClickListener {
        AlertDialog mDialog;
        MyPermissionGroupInfo mGroup;
        private String mPackageName;
        MyPermissionInfo mPerm;
        private boolean mShowRevokeUI = false;

        public PermissionItemView(Context context, AttributeSet attrs) {
            super(context, attrs);
            setClickable(true);
        }

        public void setPermission(MyPermissionGroupInfo grp, MyPermissionInfo perm, boolean first, CharSequence newPermPrefix, String packageName, boolean showRevokeUI) {
            this.mGroup = grp;
            this.mPerm = perm;
            this.mShowRevokeUI = showRevokeUI;
            this.mPackageName = packageName;
            findViewById(16909184);
            findViewById(16909187);
            ImageView permGrpIcon = (ImageView) PermissionInformation.getPermissionImageView(this);
            TextView permNameView = (TextView) PermissionInformation.getPermissionTextView(this);
            PackageManager pm = getContext().getPackageManager();
            Drawable icon = null;
            if (first) {
                icon = this.mContext.getResources().getDrawable(17302776);
            }
            CharSequence label = perm.mLabel;
            if (perm.mNew && newPermPrefix != null) {
                SpannableStringBuilder builder = new SpannableStringBuilder();
                Parcel parcel = Parcel.obtain();
                TextUtils.writeToParcel(newPermPrefix, parcel, 0);
                parcel.setDataPosition(0);
                CharSequence newStr = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
                parcel.recycle();
                builder.append(newStr);
                builder.append(label);
                label = builder;
            }
            permGrpIcon.setImageDrawable(icon);
            permNameView.setText(label);
            setOnClickListener(this);
        }

        public void onClick(View v) {
            if (this.mGroup != null && this.mPerm != null) {
                if (this.mDialog != null) {
                    this.mDialog.dismiss();
                }
                PackageManager pm = getContext().getPackageManager();
                Builder builder = new Builder(getContext());
                builder.setTitle(this.mGroup.mLabel);
                if (this.mPerm.descriptionRes != 0) {
                    builder.setMessage(this.mPerm.loadDescription(pm));
                } else {
                    CharSequence appName;
                    try {
                        appName = pm.getApplicationInfo(this.mPerm.packageName, 0).loadLabel(pm);
                    } catch (NameNotFoundException e) {
                        appName = this.mPerm.packageName;
                    }
                    StringBuilder sbuilder = new StringBuilder(128);
                    sbuilder.append(getContext().getString(17040892, new Object[]{appName}));
                    sbuilder.append("\n\n");
                    sbuilder.append(this.mPerm.name);
                    builder.setMessage(sbuilder.toString());
                }
                PermissionInformation.setPositiveButton(builder, this.mDialog);
                builder.setCancelable(true);
                builder.setIcon(this.mGroup.loadGroupIcon(getContext(), pm));
                addRevokeUIIfNecessary(builder);
                this.mDialog = builder.show();
                this.mDialog.setCanceledOnTouchOutside(true);
            }
        }

        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (this.mDialog != null) {
                this.mDialog.dismiss();
            }
        }

        private void addRevokeUIIfNecessary(Builder builder) {
            if (this.mShowRevokeUI) {
                boolean z = true;
                if ((this.mPerm.mExistingReqFlags & 1) == 0) {
                    z = false;
                }
                if (!z) {
                    builder.setNegativeButton(17041012, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            PermissionItemView.this.getContext().getPackageManager().revokeRuntimePermission(PermissionItemView.this.mPackageName, PermissionItemView.this.mPerm.name, new UserHandle(PermissionItemView.this.mContext.getUserId()));
                            PermissionItemView.this.setVisibility(8);
                        }
                    });
                    builder.setPositiveButton(17039370, null);
                }
            }
        }
    }

    private AppSecurityPermissions(Context context) {
        this.mPermGroups = new HashMap();
        this.mPermGroupsList = new ArrayList();
        this.mPermGroupComparator = new PermissionGroupInfoComparator();
        this.mPermComparator = new PermissionInfoComparator();
        this.mPermsList = new ArrayList();
        this.mContext = context;
        this.mInflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        this.mPm = this.mContext.getPackageManager();
        this.mNewPermPrefix = this.mContext.getText(17040893);
    }

    public AppSecurityPermissions(Context context, String packageName) {
        this(context);
        this.mPackageName = packageName;
        Set<MyPermissionInfo> permSet = new HashSet();
        try {
            PackageInfo pkgInfo = this.mPm.getPackageInfo(packageName, 4096);
            if (!(pkgInfo.applicationInfo == null || pkgInfo.applicationInfo.uid == -1)) {
                getAllUsedPermissions(pkgInfo.applicationInfo.uid, permSet);
            }
            this.mPermsList.addAll(permSet);
            setPermissions(this.mPermsList);
        } catch (NameNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't retrieve permissions for package:");
            stringBuilder.append(packageName);
            Log.w(str, stringBuilder.toString());
        }
    }

    public AppSecurityPermissions(Context context, PackageInfo info) {
        this(context);
        Set<MyPermissionInfo> permSet = new HashSet();
        if (info != null) {
            this.mPackageName = info.packageName;
            PackageInfo installedPkgInfo = null;
            if (info.requestedPermissions != null) {
                try {
                    installedPkgInfo = this.mPm.getPackageInfo(info.packageName, 4096);
                } catch (NameNotFoundException e) {
                }
                extractPerms(info, permSet, installedPkgInfo);
            }
            if (info.sharedUserId != null) {
                try {
                    getAllUsedPermissions(this.mPm.getUidForSharedUser(info.sharedUserId), permSet);
                } catch (NameNotFoundException e2) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Couldn't retrieve shared user id for: ");
                    stringBuilder.append(info.packageName);
                    Log.w(str, stringBuilder.toString());
                }
            }
            this.mPermsList.addAll(permSet);
            setPermissions(this.mPermsList);
        }
    }

    public static View getPermissionItemView(Context context, CharSequence grpName, CharSequence description, boolean dangerous) {
        return getPermissionItemViewOld(context, (LayoutInflater) context.getSystemService("layout_inflater"), grpName, description, dangerous, context.getDrawable(dangerous ? 17302299 : 17302776));
    }

    private void getAllUsedPermissions(int sharedUid, Set<MyPermissionInfo> permSet) {
        String[] sharedPkgList = this.mPm.getPackagesForUid(sharedUid);
        if (sharedPkgList != null && sharedPkgList.length != 0) {
            for (String sharedPkg : sharedPkgList) {
                getPermissionsForPackage(sharedPkg, permSet);
            }
        }
    }

    private void getPermissionsForPackage(String packageName, Set<MyPermissionInfo> permSet) {
        try {
            PackageInfo pkgInfo = this.mPm.getPackageInfo(packageName, 4096);
            extractPerms(pkgInfo, permSet, pkgInfo);
        } catch (NameNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't retrieve permissions for package: ");
            stringBuilder.append(packageName);
            Log.w(str, stringBuilder.toString());
        }
    }

    private void extractPerms(PackageInfo info, Set<MyPermissionInfo> permSet, PackageInfo installedPkgInfo) {
        PackageInfo packageInfo = info;
        PackageInfo packageInfo2 = installedPkgInfo;
        String[] strList = packageInfo.requestedPermissions;
        int[] flagsList = packageInfo.requestedPermissionsFlags;
        Set<MyPermissionInfo> set;
        if (strList == null || strList.length == 0) {
            set = permSet;
            return;
        }
        int i = 0;
        int i2 = 0;
        while (true) {
            int i3 = i2;
            if (i3 < strList.length) {
                String permName = strList[i3];
                try {
                    PermissionInfo tmpPermInfo = this.mPm.getPermissionInfo(permName, i);
                    if (tmpPermInfo != null) {
                        int j;
                        int existingIndex = -1;
                        if (packageInfo2 != null && packageInfo2.requestedPermissions != null) {
                            for (j = i; j < packageInfo2.requestedPermissions.length; j++) {
                                if (permName.equals(packageInfo2.requestedPermissions[j])) {
                                    existingIndex = j;
                                    break;
                                }
                            }
                        }
                        j = existingIndex >= 0 ? packageInfo2.requestedPermissionsFlags[existingIndex] : i;
                        if (isDisplayablePermission(tmpPermInfo, flagsList[i3], j)) {
                            String origGroupName = tmpPermInfo.group;
                            String groupName = origGroupName;
                            if (groupName == null) {
                                groupName = tmpPermInfo.packageName;
                                tmpPermInfo.group = groupName;
                            }
                            if (((MyPermissionGroupInfo) this.mPermGroups.get(groupName)) == null) {
                                MyPermissionGroupInfo group;
                                PermissionGroupInfo grp = null;
                                if (origGroupName != null) {
                                    grp = this.mPm.getPermissionGroupInfo(origGroupName, i);
                                }
                                if (grp != null) {
                                    group = new MyPermissionGroupInfo(grp);
                                } else {
                                    tmpPermInfo.group = tmpPermInfo.packageName;
                                    if (((MyPermissionGroupInfo) this.mPermGroups.get(tmpPermInfo.group)) == null) {
                                        MyPermissionGroupInfo group2 = new MyPermissionGroupInfo(tmpPermInfo);
                                    }
                                    group = new MyPermissionGroupInfo(tmpPermInfo);
                                }
                                this.mPermGroups.put(tmpPermInfo.group, group);
                            }
                            boolean newPerm = packageInfo2 != null && (j & 2) == 0;
                            MyPermissionInfo myPerm = new MyPermissionInfo(tmpPermInfo);
                            myPerm.mNewReqFlags = flagsList[i3];
                            myPerm.mExistingReqFlags = j;
                            myPerm.mNew = newPerm;
                            try {
                                permSet.add(myPerm);
                            } catch (NameNotFoundException e) {
                            }
                            i2 = i3 + 1;
                            i = 0;
                        }
                    }
                    set = permSet;
                } catch (NameNotFoundException e2) {
                    set = permSet;
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Ignoring unknown permission:");
                    stringBuilder.append(permName);
                    Log.i(str, stringBuilder.toString());
                    i2 = i3 + 1;
                    i = 0;
                }
                i2 = i3 + 1;
                i = 0;
            } else {
                set = permSet;
                return;
            }
        }
    }

    public int getPermissionCount() {
        return getPermissionCount(65535);
    }

    private List<MyPermissionInfo> getPermissionList(MyPermissionGroupInfo grp, int which) {
        if (which == 4) {
            return grp.mNewPermissions;
        }
        return grp.mAllPermissions;
    }

    public int getPermissionCount(int which) {
        int N = 0;
        for (int i = 0; i < this.mPermGroupsList.size(); i++) {
            N += getPermissionList((MyPermissionGroupInfo) this.mPermGroupsList.get(i), which).size();
        }
        return N;
    }

    public View getPermissionsView() {
        return getPermissionsView(65535, false);
    }

    public View getPermissionsViewWithRevokeButtons() {
        return getPermissionsView(65535, true);
    }

    public View getPermissionsView(int which) {
        return getPermissionsView(which, false);
    }

    private View getPermissionsView(int which, boolean showRevokeUI) {
        LinearLayout permsView = (LinearLayout) this.mInflater.inflate(17367098, null);
        LinearLayout displayList = (LinearLayout) permsView.findViewById(16909191);
        View noPermsView = permsView.findViewById(16909124);
        displayPermissions(this.mPermGroupsList, displayList, which, showRevokeUI);
        if (displayList.getChildCount() <= 0) {
            noPermsView.setVisibility(0);
        }
        return permsView;
    }

    private void displayPermissions(List<MyPermissionGroupInfo> groups, LinearLayout permListView, int which, boolean showRevokeUI) {
        int i = which;
        permListView.removeAllViews();
        int spacing = (int) (8.0f * this.mContext.getResources().getDisplayMetrics().density);
        int i2 = 0;
        while (true) {
            int i3 = i2;
            LinearLayout linearLayout;
            if (i3 < groups.size()) {
                MyPermissionGroupInfo grp = (MyPermissionGroupInfo) groups.get(i3);
                List<MyPermissionInfo> perms = getPermissionList(grp, i);
                i2 = 0;
                while (true) {
                    int j = i2;
                    if (j >= perms.size()) {
                        break;
                    }
                    View view = getPermissionItemView(grp, (MyPermissionInfo) perms.get(j), j == 0, i != 4 ? this.mNewPermPrefix : null, showRevokeUI);
                    LayoutParams lp = new LayoutParams(-1, -2);
                    if (j == 0) {
                        lp.topMargin = spacing;
                    }
                    if (j == grp.mAllPermissions.size() - 1) {
                        lp.bottomMargin = spacing;
                    }
                    if (permListView.getChildCount() == 0) {
                        lp.topMargin *= 2;
                    }
                    permListView.addView(view, (ViewGroup.LayoutParams) lp);
                    i2 = j + 1;
                }
                linearLayout = permListView;
                i2 = i3 + 1;
            } else {
                List<MyPermissionGroupInfo> list = groups;
                linearLayout = permListView;
                return;
            }
        }
    }

    private PermissionItemView getPermissionItemView(MyPermissionGroupInfo grp, MyPermissionInfo perm, boolean first, CharSequence newPermPrefix, boolean showRevokeUI) {
        return getPermissionItemView(this.mContext, this.mInflater, grp, perm, first, newPermPrefix, this.mPackageName, showRevokeUI);
    }

    private static PermissionItemView getPermissionItemView(Context context, LayoutInflater inflater, MyPermissionGroupInfo grp, MyPermissionInfo perm, boolean first, CharSequence newPermPrefix, String packageName, boolean showRevokeUI) {
        LayoutInflater layoutInflater = inflater;
        MyPermissionInfo myPermissionInfo = perm;
        boolean z = true;
        layoutInflater.inflate((myPermissionInfo.flags & 1) != 0 ? 17367096 : 17367095, null);
        if ((myPermissionInfo.flags & 1) == 0) {
            z = false;
        }
        PermissionItemView permView = PermissionInformation.getHwPermItemView(z, layoutInflater);
        permView.setPermission(grp, myPermissionInfo, first, newPermPrefix, packageName, showRevokeUI);
        return permView;
    }

    private static View getPermissionItemViewOld(Context context, LayoutInflater inflater, CharSequence grpName, CharSequence permList, boolean dangerous, Drawable icon) {
        View inflate = inflater.inflate(17367097, null);
        inflate = PermissionInformation.getPermissionItemViewOld(inflater);
        inflate.findViewById(16909188);
        inflate.findViewById(16909190);
        inflate.findViewById(16909184);
        TextView permGrpView = PermissionInformation.getPermissionItemViewOldPermGrpView(inflate);
        TextView permDescView = PermissionInformation.getPermissionItemViewOldPermDescView(inflate);
        PermissionInformation.getPermissionItemViewOldImgView(inflate).setImageDrawable(icon);
        if (grpName != null) {
            permGrpView.setText(grpName);
            permDescView.setText(permList);
        } else {
            permGrpView.setText(permList);
            permDescView.setVisibility(8);
        }
        return inflate;
    }

    private boolean isDisplayablePermission(PermissionInfo pInfo, int newReqFlags, int existingReqFlags) {
        int base = pInfo.protectionLevel & 15;
        if (base == 0) {
            return false;
        }
        boolean isDangerous = base == 1 || (pInfo.protectionLevel & 128) != 0;
        boolean isRequired = (newReqFlags & 1) != 0;
        boolean isDevelopment = (pInfo.protectionLevel & 32) != 0;
        boolean wasGranted = (existingReqFlags & 2) != 0;
        boolean isGranted = (newReqFlags & 2) != 0;
        if (isDangerous && (isRequired || wasGranted || isGranted)) {
            return true;
        }
        return isDevelopment && wasGranted;
    }

    private void addPermToList(List<MyPermissionInfo> permList, MyPermissionInfo pInfo) {
        if (pInfo.mLabel == null) {
            pInfo.mLabel = pInfo.loadSafeLabel(this.mPm, 20000.0f, 5);
        }
        int idx = Collections.binarySearch(permList, pInfo, this.mPermComparator);
        if (idx < 0) {
            permList.add((-idx) - 1, pInfo);
        }
    }

    private void setPermissions(List<MyPermissionInfo> permList) {
        if (permList != null) {
            for (MyPermissionInfo pInfo : permList) {
                if (isDisplayablePermission(pInfo, pInfo.mNewReqFlags, pInfo.mExistingReqFlags)) {
                    MyPermissionGroupInfo group = (MyPermissionGroupInfo) this.mPermGroups.get(pInfo.group);
                    if (group != null) {
                        pInfo.mLabel = pInfo.loadSafeLabel(this.mPm, 20000.0f, 5);
                        addPermToList(group.mAllPermissions, pInfo);
                        if (pInfo.mNew) {
                            addPermToList(group.mNewPermissions, pInfo);
                        }
                    }
                }
            }
        }
        for (MyPermissionGroupInfo pgrp : this.mPermGroups.values()) {
            if (pgrp.labelRes == 0 && pgrp.nonLocalizedLabel == null) {
                try {
                    pgrp.mLabel = this.mPm.getApplicationInfo(pgrp.packageName, 0).loadSafeLabel(this.mPm, 20000.0f, 5);
                } catch (NameNotFoundException e) {
                    pgrp.mLabel = pgrp.loadSafeLabel(this.mPm, 20000.0f, 5);
                }
            } else {
                pgrp.mLabel = pgrp.loadSafeLabel(this.mPm, 20000.0f, 5);
            }
            this.mPermGroupsList.add(pgrp);
        }
        Collections.sort(this.mPermGroupsList, this.mPermGroupComparator);
    }
}
