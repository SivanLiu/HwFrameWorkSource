package com.gsma.services.nfc;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.nfc.cardemulation.NxpAidGroup;
import android.util.Log;
import com.gsma.services.utils.InsufficientResourcesException;
import com.nxp.nfc.gsma.internal.NxpNfcController;
import com.nxp.nfc.gsma.internal.NxpOffHostService;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class OffHostService {
    private static final String TAG = "OffHostService";
    List<AidGroup> mAidGroupList;
    Drawable mBanner;
    int mBannerResId;
    Context mContext;
    String mDescription;
    boolean mModifiable;
    NxpNfcController mNxpNfcController;
    String mPackageName;
    String mSEName;
    String mServiceName;
    int mUserId;

    protected OffHostService(int userId, String description, String SELocation, String packageName, String serviceName, boolean modifiable) {
        this.mDescription = null;
        this.mSEName = null;
        this.mPackageName = null;
        this.mServiceName = null;
        this.mModifiable = true;
        this.mAidGroupList = new ArrayList();
        this.mNxpNfcController = null;
        this.mContext = null;
        this.mBannerResId = 0;
        this.mUserId = userId;
        this.mDescription = description;
        this.mSEName = SELocation;
        this.mPackageName = packageName;
        this.mServiceName = serviceName;
        this.mModifiable = modifiable;
    }

    protected OffHostService(NxpOffHostService service) {
        this.mDescription = null;
        this.mSEName = null;
        this.mPackageName = null;
        this.mServiceName = null;
        this.mModifiable = true;
        this.mAidGroupList = new ArrayList();
        this.mNxpNfcController = null;
        this.mContext = null;
        this.mUserId = service.mUserId;
        this.mDescription = service.mDescription;
        this.mSEName = service.mSEName;
        this.mPackageName = service.mPackageName;
        this.mServiceName = service.mServiceName;
        this.mModifiable = service.mModifiable;
        this.mAidGroupList = convertToOffHostAidGroupList(service.mNxpAidGroupList);
        this.mBanner = service.mBanner;
        this.mBannerResId = service.getBannerId();
        this.mContext = service.getContext();
        this.mNxpNfcController = service.mNxpNfcController;
        PackageManager pManager = this.mContext.getPackageManager();
        if (this.mBannerResId > 0) {
            String str;
            StringBuilder stringBuilder;
            try {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("setBannerResId(): getDrawable() with mBannerResId=");
                stringBuilder.append(String.valueOf(this.mBannerResId));
                Log.d(str, stringBuilder.toString());
                this.mBanner = pManager.getResourcesForApplication(this.mPackageName).getDrawable(this.mBannerResId, null);
            } catch (Exception e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception : ");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    public String getLocation() {
        return this.mSEName;
    }

    public String getDescription() {
        return this.mDescription;
    }

    protected String getServiceName() {
        return this.mServiceName;
    }

    public void setBanner(Drawable banner) {
        String packName = this.mPackageName;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setBanner() Resources packName: ");
        stringBuilder.append(packName);
        Log.d(str, stringBuilder.toString());
        boolean z = false;
        int i = 0;
        while (true) {
            try {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(packName);
                stringBuilder2.append(".R");
                if (i >= Class.forName(stringBuilder2.toString()).getClasses().length) {
                    break;
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(packName);
                stringBuilder2.append(".R");
                if (Class.forName(stringBuilder2.toString()).getClasses()[i].getName().split("\\$")[1].equals("drawable")) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(packName);
                    stringBuilder2.append(".R");
                    if (Class.forName(stringBuilder2.toString()).getClasses()[i] != null) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(packName);
                        stringBuilder2.append(".R");
                        Field[] f = Class.forName(stringBuilder2.toString()).getClasses()[i].getDeclaredFields();
                        int counter = 0;
                        int max = f.length;
                        while (counter < max) {
                            int resId = f[counter].getInt(null);
                            Drawable d = this.mContext.getDrawable(resId);
                            if (areDrawablesEqual(banner, d)) {
                                this.mBannerResId = resId;
                                this.mBanner = d;
                                String str2 = TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("setBanner() Resources GOT THE DRAWABLE On loop ");
                                stringBuilder3.append(String.valueOf(counter));
                                stringBuilder3.append("got resId : ");
                                stringBuilder3.append(String.valueOf(resId));
                                Log.d(str2, stringBuilder3.toString());
                                str2 = TAG;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("setBanner() is banner null?");
                                if (this.mBanner == null) {
                                    z = true;
                                }
                                stringBuilder3.append(z);
                                Log.d(str2, stringBuilder3.toString());
                            } else {
                                counter++;
                            }
                        }
                    }
                }
                i++;
            } catch (Exception e) {
                Log.d(TAG, "setBanner() Resources exception ...", e);
            }
        }
        if (this.mBannerResId == 0) {
            Log.d(TAG, "bannerId  set to 0");
            this.mBannerResId = -1;
            this.mBanner = banner;
        }
    }

    public void setBanner(int bannerResId) throws NotFoundException {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setBannerResId() with ");
        stringBuilder.append(String.valueOf(bannerResId));
        Log.d(str, stringBuilder.toString());
        this.mBannerResId = bannerResId;
        PackageManager pManager = this.mContext.getPackageManager();
        String packName = this.mContext.getPackageName();
        if (this.mBannerResId > 0) {
            try {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setBannerResId(): getDrawable() with mBannerResId=");
                stringBuilder2.append(String.valueOf(this.mBannerResId));
                Log.d(str2, stringBuilder2.toString());
                this.mBanner = pManager.getResourcesForApplication(packName).getDrawable(this.mBannerResId, null);
            } catch (Exception e) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Exception : ");
                stringBuilder3.append(e.getMessage());
                Log.e(str3, stringBuilder3.toString());
            }
        }
    }

    protected boolean getModifiable() {
        return this.mModifiable;
    }

    public Drawable getBanner() {
        return this.mBanner;
    }

    public AidGroup defineAidGroup(String description, String category) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("defineAidGroup description=");
        stringBuilder.append(description);
        stringBuilder.append(",  category=");
        stringBuilder.append(category);
        Log.d(str, stringBuilder.toString());
        if (description == null) {
            throw new IllegalArgumentException("Invalid description provided");
        } else if ("payment".equals(category) || "other".equals(category)) {
            AidGroup aidGroup = new AidGroup(description, category);
            this.mAidGroupList.add(aidGroup);
            return aidGroup;
        } else {
            throw new IllegalArgumentException("Invalid category provided");
        }
    }

    public void deleteAidGroup(AidGroup group) {
        this.mAidGroupList.remove(group);
    }

    public AidGroup[] getAidGroups() {
        if (this.mAidGroupList.size() == 0) {
            return null;
        }
        return (AidGroup[]) this.mAidGroupList.toArray(new AidGroup[this.mAidGroupList.size()]);
    }

    private ArrayList<NxpAidGroup> convertToCeAidGroupList(List<AidGroup> mAidGroups) {
        ArrayList<NxpAidGroup> mApduAidGroupList = new ArrayList();
        List<String> aidList = new ArrayList();
        for (AidGroup mGroup : mAidGroups) {
            NxpAidGroup nxpAidGroup;
            for (String aid : mGroup.getAidList()) {
                aidList.add(aid);
            }
            if (aidList.size() == 0) {
                nxpAidGroup = new NxpAidGroup(mGroup.getCategory(), mGroup.getDescription());
            } else {
                nxpAidGroup = new NxpAidGroup(aidList, mGroup.getCategory(), mGroup.getDescription());
            }
            mApduAidGroupList.add(nxpAidGroup);
        }
        return mApduAidGroupList;
    }

    private NxpOffHostService convertToNxpOffhostService(OffHostService service) {
        ArrayList<NxpAidGroup> mAidGroupList = convertToCeAidGroupList(service.mAidGroupList);
        NxpOffHostService mNxpOffHostService = new NxpOffHostService(service.mUserId, service.mDescription, service.mSEName, service.mPackageName, service.mServiceName, service.mModifiable);
        if (service.mBannerResId <= 0) {
            mNxpOffHostService.setBanner(service.mBanner);
        }
        mNxpOffHostService.setBannerId(service.mBannerResId);
        mNxpOffHostService.mNxpAidGroupList.addAll(mAidGroupList);
        return mNxpOffHostService;
    }

    public void commit() throws InsufficientResourcesException {
        Log.d(TAG, "commit() begin");
        boolean status = this.mNxpNfcController.commitOffHostService(this.mUserId, this.mPackageName, convertToNxpOffhostService(this));
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" commit status value");
        stringBuilder.append(status);
        Log.d(str, stringBuilder.toString());
        if (!status) {
            throw new InsufficientResourcesException("Routing Table is Full, Cannot Commit");
        }
    }

    private void setNxpNfcController(NxpNfcController nxpNfcController) {
        this.mNxpNfcController = nxpNfcController;
    }

    private ArrayList<AidGroup> convertToOffHostAidGroupList(List<NxpAidGroup> mAidGroups) {
        ArrayList<AidGroup> mOffHostAidGroups = new ArrayList();
        String aidGroupDescription = "";
        for (NxpAidGroup mCeAidGroup : mAidGroups) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(mCeAidGroup.getDescription());
            stringBuilder.append(",");
            stringBuilder.append(mCeAidGroup.getCategory());
            Log.d(str, stringBuilder.toString());
            if (mCeAidGroup.getDescription() == null) {
                aidGroupDescription = "";
            } else {
                aidGroupDescription = mCeAidGroup.getDescription();
            }
            AidGroup mAidGroup = defineAidGroup(aidGroupDescription, mCeAidGroup.getCategory());
            for (String aid : mCeAidGroup.getAids()) {
                mAidGroup.addNewAid(aid);
            }
            mOffHostAidGroups.add(mAidGroup);
        }
        return mOffHostAidGroups;
    }

    private boolean areDrawablesEqual(Drawable drawableA, Drawable drawableB) {
        ConstantState stateA = drawableA.getConstantState();
        ConstantState stateB = drawableB.getConstantState();
        if ((stateA == null || stateB == null || !stateA.equals(stateB)) && !areDrawableBitmapsEqual(drawableA, drawableB)) {
            return false;
        }
        return true;
    }

    private boolean areDrawableBitmapsEqual(Drawable drawableA, Drawable drawableB) {
        if (!(drawableA instanceof BitmapDrawable) || !(drawableB instanceof BitmapDrawable)) {
            return false;
        }
        Bitmap bitmapA = ((BitmapDrawable) drawableA).getBitmap();
        Bitmap bitmapB = ((BitmapDrawable) drawableB).getBitmap();
        if (bitmapA.getWidth() == bitmapB.getWidth() && bitmapA.getHeight() == bitmapB.getHeight() && bitmapA.sameAs(bitmapB)) {
            return true;
        }
        return false;
    }

    public String toString() {
        StringBuffer out = new StringBuffer("OffHostService: ");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mPackageName: ");
        stringBuilder.append(this.mPackageName);
        out.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", mServiceName: ");
        stringBuilder.append(this.mServiceName);
        out.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", description: ");
        stringBuilder.append(this.mDescription);
        out.append(stringBuilder.toString());
        for (AidGroup aidGroup : this.mAidGroupList) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(", aidGroup: ");
            stringBuilder2.append(aidGroup);
            out.append(stringBuilder2.toString());
        }
        return out.toString();
    }
}
