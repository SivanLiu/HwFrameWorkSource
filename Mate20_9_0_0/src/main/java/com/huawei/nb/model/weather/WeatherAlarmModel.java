package com.huawei.nb.model.weather;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class WeatherAlarmModel extends AManagedObject {
    public static final Creator<WeatherAlarmModel> CREATOR = new Creator<WeatherAlarmModel>() {
        public WeatherAlarmModel createFromParcel(Parcel in) {
            return new WeatherAlarmModel(in);
        }

        public WeatherAlarmModel[] newArray(int size) {
            return new WeatherAlarmModel[size];
        }
    };
    private Long _id;
    private String alarm_content;
    private String alarm_id;
    private int alarm_type;
    private String alarm_type_name;
    private String city_name;
    private String county_name;
    private int level;
    private String level_name;
    private long observationtime;
    private String province_name;
    private long weather_id;

    public WeatherAlarmModel(Cursor cursor) {
        setRowId(Long.valueOf(cursor.getLong(0)));
        this._id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.weather_id = cursor.getLong(2);
        this.alarm_id = cursor.getString(3);
        this.province_name = cursor.getString(4);
        this.city_name = cursor.getString(5);
        this.county_name = cursor.getString(6);
        this.alarm_type = cursor.getInt(7);
        this.alarm_type_name = cursor.getString(8);
        this.level = cursor.getInt(9);
        this.level_name = cursor.getString(10);
        this.observationtime = cursor.getLong(11);
        this.alarm_content = cursor.getString(12);
    }

    public WeatherAlarmModel(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this._id = null;
            in.readLong();
        } else {
            this._id = Long.valueOf(in.readLong());
        }
        this.weather_id = in.readLong();
        this.alarm_id = in.readByte() == (byte) 0 ? null : in.readString();
        this.province_name = in.readByte() == (byte) 0 ? null : in.readString();
        this.city_name = in.readByte() == (byte) 0 ? null : in.readString();
        this.county_name = in.readByte() == (byte) 0 ? null : in.readString();
        this.alarm_type = in.readInt();
        this.alarm_type_name = in.readByte() == (byte) 0 ? null : in.readString();
        this.level = in.readInt();
        this.level_name = in.readByte() == (byte) 0 ? null : in.readString();
        this.observationtime = in.readLong();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.alarm_content = str;
    }

    private WeatherAlarmModel(Long _id, long weather_id, String alarm_id, String province_name, String city_name, String county_name, int alarm_type, String alarm_type_name, int level, String level_name, long observationtime, String alarm_content) {
        this._id = _id;
        this.weather_id = weather_id;
        this.alarm_id = alarm_id;
        this.province_name = province_name;
        this.city_name = city_name;
        this.county_name = county_name;
        this.alarm_type = alarm_type;
        this.alarm_type_name = alarm_type_name;
        this.level = level;
        this.level_name = level_name;
        this.observationtime = observationtime;
        this.alarm_content = alarm_content;
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

    public String getAlarm_id() {
        return this.alarm_id;
    }

    public void setAlarm_id(String alarm_id) {
        this.alarm_id = alarm_id;
        setValue();
    }

    public String getProvince_name() {
        return this.province_name;
    }

    public void setProvince_name(String province_name) {
        this.province_name = province_name;
        setValue();
    }

    public String getCity_name() {
        return this.city_name;
    }

    public void setCity_name(String city_name) {
        this.city_name = city_name;
        setValue();
    }

    public String getCounty_name() {
        return this.county_name;
    }

    public void setCounty_name(String county_name) {
        this.county_name = county_name;
        setValue();
    }

    public int getAlarm_type() {
        return this.alarm_type;
    }

    public void setAlarm_type(int alarm_type) {
        this.alarm_type = alarm_type;
        setValue();
    }

    public String getAlarm_type_name() {
        return this.alarm_type_name;
    }

    public void setAlarm_type_name(String alarm_type_name) {
        this.alarm_type_name = alarm_type_name;
        setValue();
    }

    public int getLevel() {
        return this.level;
    }

    public void setLevel(int level) {
        this.level = level;
        setValue();
    }

    public String getLevel_name() {
        return this.level_name;
    }

    public void setLevel_name(String level_name) {
        this.level_name = level_name;
        setValue();
    }

    public long getObservationtime() {
        return this.observationtime;
    }

    public void setObservationtime(long observationtime) {
        this.observationtime = observationtime;
        setValue();
    }

    public String getAlarm_content() {
        return this.alarm_content;
    }

    public void setAlarm_content(String alarm_content) {
        this.alarm_content = alarm_content;
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
        if (this.alarm_id != null) {
            out.writeByte((byte) 1);
            out.writeString(this.alarm_id);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.province_name != null) {
            out.writeByte((byte) 1);
            out.writeString(this.province_name);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.city_name != null) {
            out.writeByte((byte) 1);
            out.writeString(this.city_name);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.county_name != null) {
            out.writeByte((byte) 1);
            out.writeString(this.county_name);
        } else {
            out.writeByte((byte) 0);
        }
        out.writeInt(this.alarm_type);
        if (this.alarm_type_name != null) {
            out.writeByte((byte) 1);
            out.writeString(this.alarm_type_name);
        } else {
            out.writeByte((byte) 0);
        }
        out.writeInt(this.level);
        if (this.level_name != null) {
            out.writeByte((byte) 1);
            out.writeString(this.level_name);
        } else {
            out.writeByte((byte) 0);
        }
        out.writeLong(this.observationtime);
        if (this.alarm_content != null) {
            out.writeByte((byte) 1);
            out.writeString(this.alarm_content);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<WeatherAlarmModel> getHelper() {
        return WeatherAlarmModelHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.weather.WeatherAlarmModel";
    }

    public String getDatabaseName() {
        return "dsWeather";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("WeatherAlarmModel { _id: ").append(this._id);
        sb.append(", weather_id: ").append(this.weather_id);
        sb.append(", alarm_id: ").append(this.alarm_id);
        sb.append(", province_name: ").append(this.province_name);
        sb.append(", city_name: ").append(this.city_name);
        sb.append(", county_name: ").append(this.county_name);
        sb.append(", alarm_type: ").append(this.alarm_type);
        sb.append(", alarm_type_name: ").append(this.alarm_type_name);
        sb.append(", level: ").append(this.level);
        sb.append(", level_name: ").append(this.level_name);
        sb.append(", observationtime: ").append(this.observationtime);
        sb.append(", alarm_content: ").append(this.alarm_content);
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
