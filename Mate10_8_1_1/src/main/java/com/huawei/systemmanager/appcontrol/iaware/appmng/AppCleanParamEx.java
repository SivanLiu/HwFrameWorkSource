package com.huawei.systemmanager.appcontrol.iaware.appmng;

import android.app.mtm.iaware.appmng.AppCleanParam;
import android.app.mtm.iaware.appmng.AppMngConstant.AppCleanSource;
import java.util.ArrayList;
import java.util.List;

public class AppCleanParamEx {
    private AppCleanParam mInnerAppCleanParam;

    public static class AppCleanInfo {
        private android.app.mtm.iaware.appmng.AppCleanParam.AppCleanInfo mInnerInfo;

        public AppCleanInfo(String pkgName, Integer userid, Integer cleanType) {
            this.mInnerInfo = new android.app.mtm.iaware.appmng.AppCleanParam.AppCleanInfo(pkgName, userid, cleanType);
        }

        public void setTaskId(Integer taskId) {
            this.mInnerInfo.setTaskId(taskId);
        }

        public android.app.mtm.iaware.appmng.AppCleanParam.AppCleanInfo getInnerInfo() {
            return this.mInnerInfo;
        }
    }

    public static class Builder {
        android.app.mtm.iaware.appmng.AppCleanParam.Builder mInnerBuilder;

        public Builder(int source) {
            this.mInnerBuilder = new android.app.mtm.iaware.appmng.AppCleanParam.Builder(source);
        }

        public AppCleanParamEx build() {
            AppCleanParam appCleanParam = this.mInnerBuilder.build();
            if (appCleanParam == null) {
                return null;
            }
            return new AppCleanParamEx(appCleanParam);
        }

        public Builder action(int action) {
            this.mInnerBuilder.action(action);
            return this;
        }

        public Builder appCleanInfoList(List<AppCleanInfo> appCleanInfoList) {
            List<android.app.mtm.iaware.appmng.AppCleanParam.AppCleanInfo> innerList = new ArrayList();
            for (AppCleanInfo appCleanInfo : appCleanInfoList) {
                innerList.add(appCleanInfo.getInnerInfo());
            }
            this.mInnerBuilder.appCleanInfoList(innerList);
            return this;
        }
    }

    AppCleanParamEx(AppCleanParam appCleanParam) {
        this.mInnerAppCleanParam = appCleanParam;
    }

    public List<String> getStringList() {
        return this.mInnerAppCleanParam.getStringList();
    }

    public AppCleanParam getInnerAppCleanParam() {
        return this.mInnerAppCleanParam;
    }

    public static AppCleanParamEx getCleanParam(List<AppCleanInfo> appCleanInfos) {
        return new Builder(AppCleanSource.SYSTEM_MANAGER.ordinal()).action(0).appCleanInfoList(appCleanInfos).build();
    }

    public static AppCleanParamEx getAppListParm() {
        return new Builder(AppCleanSource.SYSTEM_MANAGER.ordinal()).action(1).build();
    }
}
