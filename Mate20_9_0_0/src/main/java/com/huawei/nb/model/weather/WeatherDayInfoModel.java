package com.huawei.nb.model.weather;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class WeatherDayInfoModel extends AManagedObject {
    public static final Creator<WeatherDayInfoModel> CREATOR = new Creator<WeatherDayInfoModel>() {
        public WeatherDayInfoModel createFromParcel(Parcel in) {
            return new WeatherDayInfoModel(in);
        }

        public WeatherDayInfoModel[] newArray(int size) {
            return new WeatherDayInfoModel[size];
        }
    };
    private Long _id;
    private String day_code;
    private int day_index;
    private float high_temp;
    private float low_temp;
    private String mobile_link;
    private int moon_type = -1;
    private float night_high_temp;
    private float night_low_temp;
    private String night_text_long;
    private String night_text_short;
    private int night_weather_icon;
    private String night_wind_direction;
    private int night_wind_speed;
    private long obs_date;
    private long sun_rise_time;
    private long sun_set_time;
    private String text_long;
    private String text_short;
    private int weather_icon;
    private long weather_info_id;
    private String wind_direction;
    private int wind_speed;

    public WeatherDayInfoModel(Cursor cursor) {
        setRowId(Long.valueOf(cursor.getLong(0)));
        this._id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.weather_info_id = cursor.getLong(2);
        this.day_index = cursor.getInt(3);
        this.obs_date = cursor.getLong(4);
        this.day_code = cursor.getString(5);
        this.sun_rise_time = cursor.getLong(6);
        this.sun_set_time = cursor.getLong(7);
        this.high_temp = cursor.getFloat(8);
        this.low_temp = cursor.getFloat(9);
        this.weather_icon = cursor.getInt(10);
        this.wind_speed = cursor.getInt(11);
        this.wind_direction = cursor.getString(12);
        this.text_short = cursor.getString(13);
        this.text_long = cursor.getString(14);
        this.night_high_temp = cursor.getFloat(15);
        this.night_low_temp = cursor.getFloat(16);
        this.night_weather_icon = cursor.getInt(17);
        this.night_wind_speed = cursor.getInt(18);
        this.night_wind_direction = cursor.getString(19);
        this.night_text_short = cursor.getString(20);
        this.night_text_long = cursor.getString(21);
        this.moon_type = cursor.getInt(22);
        this.mobile_link = cursor.getString(23);
    }

    public WeatherDayInfoModel(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this._id = null;
            in.readLong();
        } else {
            this._id = Long.valueOf(in.readLong());
        }
        this.weather_info_id = in.readLong();
        this.day_index = in.readInt();
        this.obs_date = in.readLong();
        this.day_code = in.readByte() == (byte) 0 ? null : in.readString();
        this.sun_rise_time = in.readLong();
        this.sun_set_time = in.readLong();
        this.high_temp = in.readFloat();
        this.low_temp = in.readFloat();
        this.weather_icon = in.readInt();
        this.wind_speed = in.readInt();
        this.wind_direction = in.readByte() == (byte) 0 ? null : in.readString();
        this.text_short = in.readByte() == (byte) 0 ? null : in.readString();
        this.text_long = in.readByte() == (byte) 0 ? null : in.readString();
        this.night_high_temp = in.readFloat();
        this.night_low_temp = in.readFloat();
        this.night_weather_icon = in.readInt();
        this.night_wind_speed = in.readInt();
        this.night_wind_direction = in.readByte() == (byte) 0 ? null : in.readString();
        this.night_text_short = in.readByte() == (byte) 0 ? null : in.readString();
        this.night_text_long = in.readByte() == (byte) 0 ? null : in.readString();
        this.moon_type = in.readInt();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mobile_link = str;
    }

    private WeatherDayInfoModel(Long _id, long weather_info_id, int day_index, long obs_date, String day_code, long sun_rise_time, long sun_set_time, float high_temp, float low_temp, int weather_icon, int wind_speed, String wind_direction, String text_short, String text_long, float night_high_temp, float night_low_temp, int night_weather_icon, int night_wind_speed, String night_wind_direction, String night_text_short, String night_text_long, int moon_type, String mobile_link) {
        this._id = _id;
        this.weather_info_id = weather_info_id;
        this.day_index = day_index;
        this.obs_date = obs_date;
        this.day_code = day_code;
        this.sun_rise_time = sun_rise_time;
        this.sun_set_time = sun_set_time;
        this.high_temp = high_temp;
        this.low_temp = low_temp;
        this.weather_icon = weather_icon;
        this.wind_speed = wind_speed;
        this.wind_direction = wind_direction;
        this.text_short = text_short;
        this.text_long = text_long;
        this.night_high_temp = night_high_temp;
        this.night_low_temp = night_low_temp;
        this.night_weather_icon = night_weather_icon;
        this.night_wind_speed = night_wind_speed;
        this.night_wind_direction = night_wind_direction;
        this.night_text_short = night_text_short;
        this.night_text_long = night_text_long;
        this.moon_type = moon_type;
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

    public long getWeather_info_id() {
        return this.weather_info_id;
    }

    public void setWeather_info_id(long weather_info_id) {
        this.weather_info_id = weather_info_id;
        setValue();
    }

    public int getDay_index() {
        return this.day_index;
    }

    public void setDay_index(int day_index) {
        this.day_index = day_index;
        setValue();
    }

    public long getObs_date() {
        return this.obs_date;
    }

    public void setObs_date(long obs_date) {
        this.obs_date = obs_date;
        setValue();
    }

    public String getDay_code() {
        return this.day_code;
    }

    public void setDay_code(String day_code) {
        this.day_code = day_code;
        setValue();
    }

    public long getSun_rise_time() {
        return this.sun_rise_time;
    }

    public void setSun_rise_time(long sun_rise_time) {
        this.sun_rise_time = sun_rise_time;
        setValue();
    }

    public long getSun_set_time() {
        return this.sun_set_time;
    }

    public void setSun_set_time(long sun_set_time) {
        this.sun_set_time = sun_set_time;
        setValue();
    }

    public float getHigh_temp() {
        return this.high_temp;
    }

    public void setHigh_temp(float high_temp) {
        this.high_temp = high_temp;
        setValue();
    }

    public float getLow_temp() {
        return this.low_temp;
    }

    public void setLow_temp(float low_temp) {
        this.low_temp = low_temp;
        setValue();
    }

    public int getWeather_icon() {
        return this.weather_icon;
    }

    public void setWeather_icon(int weather_icon) {
        this.weather_icon = weather_icon;
        setValue();
    }

    public int getWind_speed() {
        return this.wind_speed;
    }

    public void setWind_speed(int wind_speed) {
        this.wind_speed = wind_speed;
        setValue();
    }

    public String getWind_direction() {
        return this.wind_direction;
    }

    public void setWind_direction(String wind_direction) {
        this.wind_direction = wind_direction;
        setValue();
    }

    public String getText_short() {
        return this.text_short;
    }

    public void setText_short(String text_short) {
        this.text_short = text_short;
        setValue();
    }

    public String getText_long() {
        return this.text_long;
    }

    public void setText_long(String text_long) {
        this.text_long = text_long;
        setValue();
    }

    public float getNight_high_temp() {
        return this.night_high_temp;
    }

    public void setNight_high_temp(float night_high_temp) {
        this.night_high_temp = night_high_temp;
        setValue();
    }

    public float getNight_low_temp() {
        return this.night_low_temp;
    }

    public void setNight_low_temp(float night_low_temp) {
        this.night_low_temp = night_low_temp;
        setValue();
    }

    public int getNight_weather_icon() {
        return this.night_weather_icon;
    }

    public void setNight_weather_icon(int night_weather_icon) {
        this.night_weather_icon = night_weather_icon;
        setValue();
    }

    public int getNight_wind_speed() {
        return this.night_wind_speed;
    }

    public void setNight_wind_speed(int night_wind_speed) {
        this.night_wind_speed = night_wind_speed;
        setValue();
    }

    public String getNight_wind_direction() {
        return this.night_wind_direction;
    }

    public void setNight_wind_direction(String night_wind_direction) {
        this.night_wind_direction = night_wind_direction;
        setValue();
    }

    public String getNight_text_short() {
        return this.night_text_short;
    }

    public void setNight_text_short(String night_text_short) {
        this.night_text_short = night_text_short;
        setValue();
    }

    public String getNight_text_long() {
        return this.night_text_long;
    }

    public void setNight_text_long(String night_text_long) {
        this.night_text_long = night_text_long;
        setValue();
    }

    public int getMoon_type() {
        return this.moon_type;
    }

    public void setMoon_type(int moon_type) {
        this.moon_type = moon_type;
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
        out.writeLong(this.weather_info_id);
        out.writeInt(this.day_index);
        out.writeLong(this.obs_date);
        if (this.day_code != null) {
            out.writeByte((byte) 1);
            out.writeString(this.day_code);
        } else {
            out.writeByte((byte) 0);
        }
        out.writeLong(this.sun_rise_time);
        out.writeLong(this.sun_set_time);
        out.writeFloat(this.high_temp);
        out.writeFloat(this.low_temp);
        out.writeInt(this.weather_icon);
        out.writeInt(this.wind_speed);
        if (this.wind_direction != null) {
            out.writeByte((byte) 1);
            out.writeString(this.wind_direction);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.text_short != null) {
            out.writeByte((byte) 1);
            out.writeString(this.text_short);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.text_long != null) {
            out.writeByte((byte) 1);
            out.writeString(this.text_long);
        } else {
            out.writeByte((byte) 0);
        }
        out.writeFloat(this.night_high_temp);
        out.writeFloat(this.night_low_temp);
        out.writeInt(this.night_weather_icon);
        out.writeInt(this.night_wind_speed);
        if (this.night_wind_direction != null) {
            out.writeByte((byte) 1);
            out.writeString(this.night_wind_direction);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.night_text_short != null) {
            out.writeByte((byte) 1);
            out.writeString(this.night_text_short);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.night_text_long != null) {
            out.writeByte((byte) 1);
            out.writeString(this.night_text_long);
        } else {
            out.writeByte((byte) 0);
        }
        out.writeInt(this.moon_type);
        if (this.mobile_link != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mobile_link);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<WeatherDayInfoModel> getHelper() {
        return WeatherDayInfoModelHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.weather.WeatherDayInfoModel";
    }

    public String getDatabaseName() {
        return "dsWeather";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("WeatherDayInfoModel { _id: ").append(this._id);
        sb.append(", weather_info_id: ").append(this.weather_info_id);
        sb.append(", day_index: ").append(this.day_index);
        sb.append(", obs_date: ").append(this.obs_date);
        sb.append(", day_code: ").append(this.day_code);
        sb.append(", sun_rise_time: ").append(this.sun_rise_time);
        sb.append(", sun_set_time: ").append(this.sun_set_time);
        sb.append(", high_temp: ").append(this.high_temp);
        sb.append(", low_temp: ").append(this.low_temp);
        sb.append(", weather_icon: ").append(this.weather_icon);
        sb.append(", wind_speed: ").append(this.wind_speed);
        sb.append(", wind_direction: ").append(this.wind_direction);
        sb.append(", text_short: ").append(this.text_short);
        sb.append(", text_long: ").append(this.text_long);
        sb.append(", night_high_temp: ").append(this.night_high_temp);
        sb.append(", night_low_temp: ").append(this.night_low_temp);
        sb.append(", night_weather_icon: ").append(this.night_weather_icon);
        sb.append(", night_wind_speed: ").append(this.night_wind_speed);
        sb.append(", night_wind_direction: ").append(this.night_wind_direction);
        sb.append(", night_text_short: ").append(this.night_text_short);
        sb.append(", night_text_long: ").append(this.night_text_long);
        sb.append(", moon_type: ").append(this.moon_type);
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
