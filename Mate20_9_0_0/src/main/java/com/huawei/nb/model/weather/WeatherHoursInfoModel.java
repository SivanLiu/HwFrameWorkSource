package com.huawei.nb.model.weather;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class WeatherHoursInfoModel extends AManagedObject {
    public static final Creator<WeatherHoursInfoModel> CREATOR = new Creator<WeatherHoursInfoModel>() {
        public WeatherHoursInfoModel createFromParcel(Parcel in) {
            return new WeatherHoursInfoModel(in);
        }

        public WeatherHoursInfoModel[] newArray(int size) {
            return new WeatherHoursInfoModel[size];
        }
    };
    private Long _id;
    private long forcase_date_time;
    private float hour_temprature;
    private int is_day_light;
    private String mobile_link;
    private float rain_probability;
    private int weather_icon;
    private long weather_id;

    public WeatherHoursInfoModel(Cursor cursor) {
        setRowId(Long.valueOf(cursor.getLong(0)));
        this._id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.weather_id = cursor.getLong(2);
        this.forcase_date_time = cursor.getLong(3);
        this.weather_icon = cursor.getInt(4);
        this.hour_temprature = cursor.getFloat(5);
        this.is_day_light = cursor.getInt(6);
        this.rain_probability = cursor.getFloat(7);
        this.mobile_link = cursor.getString(8);
    }

    public WeatherHoursInfoModel(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this._id = null;
            in.readLong();
        } else {
            this._id = Long.valueOf(in.readLong());
        }
        this.weather_id = in.readLong();
        this.forcase_date_time = in.readLong();
        this.weather_icon = in.readInt();
        this.hour_temprature = in.readFloat();
        this.is_day_light = in.readInt();
        this.rain_probability = in.readFloat();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mobile_link = str;
    }

    private WeatherHoursInfoModel(Long _id, long weather_id, long forcase_date_time, int weather_icon, float hour_temprature, int is_day_light, float rain_probability, String mobile_link) {
        this._id = _id;
        this.weather_id = weather_id;
        this.forcase_date_time = forcase_date_time;
        this.weather_icon = weather_icon;
        this.hour_temprature = hour_temprature;
        this.is_day_light = is_day_light;
        this.rain_probability = rain_probability;
        this.mobile_link = mobile_link;
    }

    public int describeContents() {
        return 0;
    }

    public Long get_id() {
        return this._id;
    }

    public void set_id(Long _id) {
        this._id = _id;
        setValue();
    }

    public long getWeather_id() {
        return this.weather_id;
    }

    public void setWeather_id(long weather_id) {
        this.weather_id = weather_id;
        setValue();
    }

    public long getForcase_date_time() {
        return this.forcase_date_time;
    }

    public void setForcase_date_time(long forcase_date_time) {
        this.forcase_date_time = forcase_date_time;
        setValue();
    }

    public int getWeather_icon() {
        return this.weather_icon;
    }

    public void setWeather_icon(int weather_icon) {
        this.weather_icon = weather_icon;
        setValue();
    }

    public float getHour_temprature() {
        return this.hour_temprature;
    }

    public void setHour_temprature(float hour_temprature) {
        this.hour_temprature = hour_temprature;
        setValue();
    }

    public int getIs_day_light() {
        return this.is_day_light;
    }

    public void setIs_day_light(int is_day_light) {
        this.is_day_light = is_day_light;
        setValue();
    }

    public float getRain_probability() {
        return this.rain_probability;
    }

    public void setRain_probability(float rain_probability) {
        this.rain_probability = rain_probability;
        setValue();
    }

    public String getMobile_link() {
        return this.mobile_link;
    }

    public void setMobile_link(String mobile_link) {
        this.mobile_link = mobile_link;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this._id != null) {
            out.writeByte((byte) 1);
            out.writeLong(this._id.longValue());
        } else {
            out.writeByte((byte) 0);
            out.writeLong(1);
        }
        out.writeLong(this.weather_id);
        out.writeLong(this.forcase_date_time);
        out.writeInt(this.weather_icon);
        out.writeFloat(this.hour_temprature);
        out.writeInt(this.is_day_light);
        out.writeFloat(this.rain_probability);
        if (this.mobile_link != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mobile_link);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<WeatherHoursInfoModel> getHelper() {
        return WeatherHoursInfoModelHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.weather.WeatherHoursInfoModel";
    }

    public String getDatabaseName() {
        return "dsWeather";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("WeatherHoursInfoModel { _id: ").append(this._id);
        sb.append(", weather_id: ").append(this.weather_id);
        sb.append(", forcase_date_time: ").append(this.forcase_date_time);
        sb.append(", weather_icon: ").append(this.weather_icon);
        sb.append(", hour_temprature: ").append(this.hour_temprature);
        sb.append(", is_day_light: ").append(this.is_day_light);
        sb.append(", rain_probability: ").append(this.rain_probability);
        sb.append(", mobile_link: ").append(this.mobile_link);
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
        return "0.0.16";
    }

    public int getDatabaseVersionCode() {
        return 16;
    }

    public String getEntityVersion() {
        return "0.0.14";
    }

    public int getEntityVersionCode() {
        return 14;
    }
}
