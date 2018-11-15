package com.android.server.hidata.wavemapping.entity;

import com.android.server.hidata.wavemapping.cons.Constant;

public class ClusterResult {
    private int cluster_num = -1;
    private int mainAp_cluster_num = -1;
    private String place;
    private String result;

    public String getResult() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.cluster_num);
        stringBuilder.append(Constant.RESULT_SEPERATE);
        stringBuilder.append(this.mainAp_cluster_num);
        this.result = stringBuilder.toString();
        return this.result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public ClusterResult(String place) {
        this.place = place;
    }

    public int getCluster_num() {
        return this.cluster_num;
    }

    public void setCluster_num(int cluster_num) {
        this.cluster_num = cluster_num;
    }

    public String getPlace() {
        return this.place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public int getMainAp_cluster_num() {
        return this.mainAp_cluster_num;
    }

    public void setMainAp_cluster_num(int mainAp_cluster_num) {
        this.mainAp_cluster_num = mainAp_cluster_num;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ClusterResult{cluster_num=");
        stringBuilder.append(this.cluster_num);
        stringBuilder.append(", place='");
        stringBuilder.append(this.place);
        stringBuilder.append('\'');
        stringBuilder.append(", mainAp_cluster_num=");
        stringBuilder.append(this.mainAp_cluster_num);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
