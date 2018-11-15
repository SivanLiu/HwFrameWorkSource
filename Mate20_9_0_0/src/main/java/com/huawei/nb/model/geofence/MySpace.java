package com.huawei.nb.model.geofence;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class MySpace extends AManagedObject {
    public static final Creator<MySpace> CREATOR = new Creator<MySpace>() {
        public MySpace createFromParcel(Parcel in) {
            return new MySpace(in);
        }

        public MySpace[] newArray(int size) {
            return new MySpace[size];
        }
    };
    private String allAOI;
    private String aoi1;
    private Float aoi1Confi;
    private String aoi2;
    private Float aoi2Confi;
    private String aoi3;
    private Float aoi3Confi;
    private String aoi4;
    private Float aoi4Confi;
    private String aoi5;
    private Float aoi5Confi;
    private String birthCity;
    private String childrenSchool;
    private String currentCity;
    private String familiarCities;
    private String friends;
    private String fun;
    private String gym;
    private String home;
    private String homeCity;
    private Float homeCityConfi;
    private Float homeConfi;
    private Long mID;
    private String meal;
    private String work;
    private String workCity;
    private Float workCityConfi;
    private Float workConfi;

    public MySpace(Cursor cursor) {
        Float f = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mID = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.home = cursor.getString(2);
        this.work = cursor.getString(3);
        this.aoi1 = cursor.getString(4);
        this.aoi2 = cursor.getString(5);
        this.aoi3 = cursor.getString(6);
        this.aoi4 = cursor.getString(7);
        this.aoi5 = cursor.getString(8);
        this.homeConfi = cursor.isNull(9) ? null : Float.valueOf(cursor.getFloat(9));
        this.workConfi = cursor.isNull(10) ? null : Float.valueOf(cursor.getFloat(10));
        this.aoi1Confi = cursor.isNull(11) ? null : Float.valueOf(cursor.getFloat(11));
        this.aoi2Confi = cursor.isNull(12) ? null : Float.valueOf(cursor.getFloat(12));
        this.aoi3Confi = cursor.isNull(13) ? null : Float.valueOf(cursor.getFloat(13));
        this.aoi4Confi = cursor.isNull(14) ? null : Float.valueOf(cursor.getFloat(14));
        this.aoi5Confi = cursor.isNull(15) ? null : Float.valueOf(cursor.getFloat(15));
        this.meal = cursor.getString(16);
        this.fun = cursor.getString(17);
        this.childrenSchool = cursor.getString(18);
        this.friends = cursor.getString(19);
        this.gym = cursor.getString(20);
        this.allAOI = cursor.getString(21);
        this.familiarCities = cursor.getString(22);
        this.homeCity = cursor.getString(23);
        this.homeCityConfi = cursor.isNull(24) ? null : Float.valueOf(cursor.getFloat(24));
        this.workCity = cursor.getString(25);
        if (!cursor.isNull(26)) {
            f = Float.valueOf(cursor.getFloat(26));
        }
        this.workCityConfi = f;
        this.birthCity = cursor.getString(27);
        this.currentCity = cursor.getString(28);
    }

    public MySpace(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mID = null;
            in.readLong();
        } else {
            this.mID = Long.valueOf(in.readLong());
        }
        this.home = in.readByte() == (byte) 0 ? null : in.readString();
        this.work = in.readByte() == (byte) 0 ? null : in.readString();
        this.aoi1 = in.readByte() == (byte) 0 ? null : in.readString();
        this.aoi2 = in.readByte() == (byte) 0 ? null : in.readString();
        this.aoi3 = in.readByte() == (byte) 0 ? null : in.readString();
        this.aoi4 = in.readByte() == (byte) 0 ? null : in.readString();
        this.aoi5 = in.readByte() == (byte) 0 ? null : in.readString();
        this.homeConfi = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.workConfi = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.aoi1Confi = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.aoi2Confi = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.aoi3Confi = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.aoi4Confi = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.aoi5Confi = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.meal = in.readByte() == (byte) 0 ? null : in.readString();
        this.fun = in.readByte() == (byte) 0 ? null : in.readString();
        this.childrenSchool = in.readByte() == (byte) 0 ? null : in.readString();
        this.friends = in.readByte() == (byte) 0 ? null : in.readString();
        this.gym = in.readByte() == (byte) 0 ? null : in.readString();
        this.allAOI = in.readByte() == (byte) 0 ? null : in.readString();
        this.familiarCities = in.readByte() == (byte) 0 ? null : in.readString();
        this.homeCity = in.readByte() == (byte) 0 ? null : in.readString();
        this.homeCityConfi = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.workCity = in.readByte() == (byte) 0 ? null : in.readString();
        this.workCityConfi = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.birthCity = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.currentCity = str;
    }

    private MySpace(Long mID, String home, String work, String aoi1, String aoi2, String aoi3, String aoi4, String aoi5, Float homeConfi, Float workConfi, Float aoi1Confi, Float aoi2Confi, Float aoi3Confi, Float aoi4Confi, Float aoi5Confi, String meal, String fun, String childrenSchool, String friends, String gym, String allAOI, String familiarCities, String homeCity, Float homeCityConfi, String workCity, Float workCityConfi, String birthCity, String currentCity) {
        this.mID = mID;
        this.home = home;
        this.work = work;
        this.aoi1 = aoi1;
        this.aoi2 = aoi2;
        this.aoi3 = aoi3;
        this.aoi4 = aoi4;
        this.aoi5 = aoi5;
        this.homeConfi = homeConfi;
        this.workConfi = workConfi;
        this.aoi1Confi = aoi1Confi;
        this.aoi2Confi = aoi2Confi;
        this.aoi3Confi = aoi3Confi;
        this.aoi4Confi = aoi4Confi;
        this.aoi5Confi = aoi5Confi;
        this.meal = meal;
        this.fun = fun;
        this.childrenSchool = childrenSchool;
        this.friends = friends;
        this.gym = gym;
        this.allAOI = allAOI;
        this.familiarCities = familiarCities;
        this.homeCity = homeCity;
        this.homeCityConfi = homeCityConfi;
        this.workCity = workCity;
        this.workCityConfi = workCityConfi;
        this.birthCity = birthCity;
        this.currentCity = currentCity;
    }

    public int describeContents() {
        return 0;
    }

    public Long getMID() {
        return this.mID;
    }

    public void setMID(Long mID) {
        this.mID = mID;
        setValue();
    }

    public String getHome() {
        return this.home;
    }

    public void setHome(String home) {
        this.home = home;
        setValue();
    }

    public String getWork() {
        return this.work;
    }

    public void setWork(String work) {
        this.work = work;
        setValue();
    }

    public String getAoi1() {
        return this.aoi1;
    }

    public void setAoi1(String aoi1) {
        this.aoi1 = aoi1;
        setValue();
    }

    public String getAoi2() {
        return this.aoi2;
    }

    public void setAoi2(String aoi2) {
        this.aoi2 = aoi2;
        setValue();
    }

    public String getAoi3() {
        return this.aoi3;
    }

    public void setAoi3(String aoi3) {
        this.aoi3 = aoi3;
        setValue();
    }

    public String getAoi4() {
        return this.aoi4;
    }

    public void setAoi4(String aoi4) {
        this.aoi4 = aoi4;
        setValue();
    }

    public String getAoi5() {
        return this.aoi5;
    }

    public void setAoi5(String aoi5) {
        this.aoi5 = aoi5;
        setValue();
    }

    public Float getHomeConfi() {
        return this.homeConfi;
    }

    public void setHomeConfi(Float homeConfi) {
        this.homeConfi = homeConfi;
        setValue();
    }

    public Float getWorkConfi() {
        return this.workConfi;
    }

    public void setWorkConfi(Float workConfi) {
        this.workConfi = workConfi;
        setValue();
    }

    public Float getAoi1Confi() {
        return this.aoi1Confi;
    }

    public void setAoi1Confi(Float aoi1Confi) {
        this.aoi1Confi = aoi1Confi;
        setValue();
    }

    public Float getAoi2Confi() {
        return this.aoi2Confi;
    }

    public void setAoi2Confi(Float aoi2Confi) {
        this.aoi2Confi = aoi2Confi;
        setValue();
    }

    public Float getAoi3Confi() {
        return this.aoi3Confi;
    }

    public void setAoi3Confi(Float aoi3Confi) {
        this.aoi3Confi = aoi3Confi;
        setValue();
    }

    public Float getAoi4Confi() {
        return this.aoi4Confi;
    }

    public void setAoi4Confi(Float aoi4Confi) {
        this.aoi4Confi = aoi4Confi;
        setValue();
    }

    public Float getAoi5Confi() {
        return this.aoi5Confi;
    }

    public void setAoi5Confi(Float aoi5Confi) {
        this.aoi5Confi = aoi5Confi;
        setValue();
    }

    public String getMeal() {
        return this.meal;
    }

    public void setMeal(String meal) {
        this.meal = meal;
        setValue();
    }

    public String getFun() {
        return this.fun;
    }

    public void setFun(String fun) {
        this.fun = fun;
        setValue();
    }

    public String getChildrenSchool() {
        return this.childrenSchool;
    }

    public void setChildrenSchool(String childrenSchool) {
        this.childrenSchool = childrenSchool;
        setValue();
    }

    public String getFriends() {
        return this.friends;
    }

    public void setFriends(String friends) {
        this.friends = friends;
        setValue();
    }

    public String getGym() {
        return this.gym;
    }

    public void setGym(String gym) {
        this.gym = gym;
        setValue();
    }

    public String getAllAOI() {
        return this.allAOI;
    }

    public void setAllAOI(String allAOI) {
        this.allAOI = allAOI;
        setValue();
    }

    public String getFamiliarCities() {
        return this.familiarCities;
    }

    public void setFamiliarCities(String familiarCities) {
        this.familiarCities = familiarCities;
        setValue();
    }

    public String getHomeCity() {
        return this.homeCity;
    }

    public void setHomeCity(String homeCity) {
        this.homeCity = homeCity;
        setValue();
    }

    public Float getHomeCityConfi() {
        return this.homeCityConfi;
    }

    public void setHomeCityConfi(Float homeCityConfi) {
        this.homeCityConfi = homeCityConfi;
        setValue();
    }

    public String getWorkCity() {
        return this.workCity;
    }

    public void setWorkCity(String workCity) {
        this.workCity = workCity;
        setValue();
    }

    public Float getWorkCityConfi() {
        return this.workCityConfi;
    }

    public void setWorkCityConfi(Float workCityConfi) {
        this.workCityConfi = workCityConfi;
        setValue();
    }

    public String getBirthCity() {
        return this.birthCity;
    }

    public void setBirthCity(String birthCity) {
        this.birthCity = birthCity;
        setValue();
    }

    public String getCurrentCity() {
        return this.currentCity;
    }

    public void setCurrentCity(String currentCity) {
        this.currentCity = currentCity;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.mID != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mID.longValue());
        } else {
            out.writeByte((byte) 0);
            out.writeLong(1);
        }
        if (this.home != null) {
            out.writeByte((byte) 1);
            out.writeString(this.home);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.work != null) {
            out.writeByte((byte) 1);
            out.writeString(this.work);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.aoi1 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.aoi1);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.aoi2 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.aoi2);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.aoi3 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.aoi3);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.aoi4 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.aoi4);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.aoi5 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.aoi5);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.homeConfi != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.homeConfi.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.workConfi != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.workConfi.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.aoi1Confi != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.aoi1Confi.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.aoi2Confi != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.aoi2Confi.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.aoi3Confi != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.aoi3Confi.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.aoi4Confi != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.aoi4Confi.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.aoi5Confi != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.aoi5Confi.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.meal != null) {
            out.writeByte((byte) 1);
            out.writeString(this.meal);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.fun != null) {
            out.writeByte((byte) 1);
            out.writeString(this.fun);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.childrenSchool != null) {
            out.writeByte((byte) 1);
            out.writeString(this.childrenSchool);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.friends != null) {
            out.writeByte((byte) 1);
            out.writeString(this.friends);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.gym != null) {
            out.writeByte((byte) 1);
            out.writeString(this.gym);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.allAOI != null) {
            out.writeByte((byte) 1);
            out.writeString(this.allAOI);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.familiarCities != null) {
            out.writeByte((byte) 1);
            out.writeString(this.familiarCities);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.homeCity != null) {
            out.writeByte((byte) 1);
            out.writeString(this.homeCity);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.homeCityConfi != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.homeCityConfi.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.workCity != null) {
            out.writeByte((byte) 1);
            out.writeString(this.workCity);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.workCityConfi != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.workCityConfi.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.birthCity != null) {
            out.writeByte((byte) 1);
            out.writeString(this.birthCity);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.currentCity != null) {
            out.writeByte((byte) 1);
            out.writeString(this.currentCity);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<MySpace> getHelper() {
        return MySpaceHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.geofence.MySpace";
    }

    public String getDatabaseName() {
        return "dsServiceMetaData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("MySpace { mID: ").append(this.mID);
        sb.append(", home: ").append(this.home);
        sb.append(", work: ").append(this.work);
        sb.append(", aoi1: ").append(this.aoi1);
        sb.append(", aoi2: ").append(this.aoi2);
        sb.append(", aoi3: ").append(this.aoi3);
        sb.append(", aoi4: ").append(this.aoi4);
        sb.append(", aoi5: ").append(this.aoi5);
        sb.append(", homeConfi: ").append(this.homeConfi);
        sb.append(", workConfi: ").append(this.workConfi);
        sb.append(", aoi1Confi: ").append(this.aoi1Confi);
        sb.append(", aoi2Confi: ").append(this.aoi2Confi);
        sb.append(", aoi3Confi: ").append(this.aoi3Confi);
        sb.append(", aoi4Confi: ").append(this.aoi4Confi);
        sb.append(", aoi5Confi: ").append(this.aoi5Confi);
        sb.append(", meal: ").append(this.meal);
        sb.append(", fun: ").append(this.fun);
        sb.append(", childrenSchool: ").append(this.childrenSchool);
        sb.append(", friends: ").append(this.friends);
        sb.append(", gym: ").append(this.gym);
        sb.append(", allAOI: ").append(this.allAOI);
        sb.append(", familiarCities: ").append(this.familiarCities);
        sb.append(", homeCity: ").append(this.homeCity);
        sb.append(", homeCityConfi: ").append(this.homeCityConfi);
        sb.append(", workCity: ").append(this.workCity);
        sb.append(", workCityConfi: ").append(this.workCityConfi);
        sb.append(", birthCity: ").append(this.birthCity);
        sb.append(", currentCity: ").append(this.currentCity);
        sb.append(" }");
        return sb.toString();
    }

    public boolean equals(Object o) {
        return super.equals(o);
    }

    public int hashCode() {
        return super.hashCode();
    }

    public String getDatabaseVersion() {
        return "0.0.11";
    }

    public int getDatabaseVersionCode() {
        return 11;
    }

    public String getEntityVersion() {
        return "0.0.2";
    }

    public int getEntityVersionCode() {
        return 2;
    }
}
