package com.huawei.nb.model.profile;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class MyProfile extends AManagedObject {
    public static final Creator<MyProfile> CREATOR = new Creator<MyProfile>() {
        public MyProfile createFromParcel(Parcel in) {
            return new MyProfile(in);
        }

        public MyProfile[] newArray(int size) {
            return new MyProfile[size];
        }
    };
    private Integer accomCellId;
    private Integer accomCellLac;
    private Float accomConfidence;
    private String accomGPS;
    private Double age;
    private Float ageConfidence;
    private String arriveHomeTimeWeekend;
    private Float arriveHomeTimeWeekendConfidence;
    private String arriveHomeTimeWorkday;
    private Float arriveHomeTimeWorkdayConfidence;
    private String arriveWorkplaceTimeWorkday;
    private Float arriveWorkplaceTimeWorkdayConfidence;
    private String baseCity;
    private Float baseCityConfidence;
    private Float buziTripPrefer;
    private Float buziTripPreferConfidence;
    private Integer contactNumber;
    private Float contactNumberConfidence;
    private String deviceID;
    private String hwId;
    private Float hwIdConfidence;
    private Integer id;
    private Float imeiConfidence;
    private String leaveHomeTimeWeekend;
    private Float leaveHomeTimeWeekendConfidence;
    private String leaveHomeTimeWorkday;
    private Float leaveHomeTimeWorkdayConfidence;
    private String leaveWorkplaceTimeWorkday;
    private Float leaveWorkplaceTimeWorkdayConfidence;
    private String offTimeWeekend;
    private Float offTimeWeekendConfidence;
    private String offTimeWorkday;
    private Float offTimeWorkdayConfidence;
    private String onTimeWeekend;
    private Float onTimeWeekendConfidence;
    private String onTimeWorkday;
    private Float onTimeWorkdayConfidence;
    private Float photoPrefer;
    private Float photoPreferConfidence;
    private Float photoPreferTurism;
    private Float photoPreferTurismConfidence;
    private Integer poi1CellId;
    private Integer poi1CellLac;
    private Float poi1Confidence;
    private String poi1GPS;
    private Integer poi2CellId;
    private Integer poi2CellLac;
    private Float poi2Confidence;
    private String poi2GPS;
    private Integer poi3CellId;
    private Integer poi3CellLac;
    private Float poi3Confidence;
    private String poi3GPS;
    private Integer poi4CellId;
    private Integer poi4CellLac;
    private Float poi4Confidence;
    private String poi4GPS;
    private Integer poi5CellId;
    private Integer poi5CellLac;
    private Float poi5Confidence;
    private String poi5GPS;
    private Double roamingRadius;
    private Float roamingRadiusConfidence;
    private Double sexuality;
    private Float sexualityConfidence;
    private String top10Tag;
    private Float top10TagConfidence;
    private String top1Tag;
    private Float top1TagConfidence;
    private String top2Tag;
    private Float top2TagConfidence;
    private String top3Tag;
    private Float top3TagConfidence;
    private String top4Tag;
    private Float top4TagConfidence;
    private String top5Tag;
    private Float top5TagConfidence;
    private String top6Tag;
    private Float top6TagConfidence;
    private String top7Tag;
    private Float top7TagConfidence;
    private String top8Tag;
    private Float top8TagConfidence;
    private String top9Tag;
    private Float top9TagConfidence;
    private String topMode;
    private Float topModeConfidence;
    private Integer workPlaceCellId;
    private Integer workPlaceCellLac;
    private String workPlaceGPS;
    private Float workplaceConfidence;

    public MyProfile(Cursor cursor) {
        Float f;
        Float f2 = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.deviceID = cursor.getString(2);
        this.hwId = cursor.getString(3);
        this.age = cursor.isNull(4) ? null : Double.valueOf(cursor.getDouble(4));
        this.sexuality = cursor.isNull(5) ? null : Double.valueOf(cursor.getDouble(5));
        this.contactNumber = cursor.isNull(6) ? null : Integer.valueOf(cursor.getInt(6));
        this.accomGPS = cursor.getString(7);
        this.accomCellId = cursor.isNull(8) ? null : Integer.valueOf(cursor.getInt(8));
        this.accomCellLac = cursor.isNull(9) ? null : Integer.valueOf(cursor.getInt(9));
        this.workPlaceGPS = cursor.getString(10);
        this.workPlaceCellId = cursor.isNull(11) ? null : Integer.valueOf(cursor.getInt(11));
        this.workPlaceCellLac = cursor.isNull(12) ? null : Integer.valueOf(cursor.getInt(12));
        this.poi1GPS = cursor.getString(13);
        this.poi1CellId = cursor.isNull(14) ? null : Integer.valueOf(cursor.getInt(14));
        this.poi1CellLac = cursor.isNull(15) ? null : Integer.valueOf(cursor.getInt(15));
        this.poi2GPS = cursor.getString(16);
        this.poi2CellId = cursor.isNull(17) ? null : Integer.valueOf(cursor.getInt(17));
        this.poi2CellLac = cursor.isNull(18) ? null : Integer.valueOf(cursor.getInt(18));
        this.poi3GPS = cursor.getString(19);
        this.poi3CellId = cursor.isNull(20) ? null : Integer.valueOf(cursor.getInt(20));
        this.poi3CellLac = cursor.isNull(21) ? null : Integer.valueOf(cursor.getInt(21));
        this.poi4GPS = cursor.getString(22);
        this.poi4CellId = cursor.isNull(23) ? null : Integer.valueOf(cursor.getInt(23));
        this.poi4CellLac = cursor.isNull(24) ? null : Integer.valueOf(cursor.getInt(24));
        this.poi5GPS = cursor.getString(25);
        this.poi5CellId = cursor.isNull(26) ? null : Integer.valueOf(cursor.getInt(26));
        this.poi5CellLac = cursor.isNull(27) ? null : Integer.valueOf(cursor.getInt(27));
        this.baseCity = cursor.getString(28);
        this.roamingRadius = cursor.isNull(29) ? null : Double.valueOf(cursor.getDouble(29));
        this.buziTripPrefer = cursor.isNull(30) ? null : Float.valueOf(cursor.getFloat(30));
        this.photoPrefer = cursor.isNull(31) ? null : Float.valueOf(cursor.getFloat(31));
        if (cursor.isNull(32)) {
            f = null;
        } else {
            f = Float.valueOf(cursor.getFloat(32));
        }
        this.photoPreferTurism = f;
        this.topMode = cursor.getString(33);
        this.top1Tag = cursor.getString(34);
        this.top2Tag = cursor.getString(35);
        this.top3Tag = cursor.getString(36);
        this.top4Tag = cursor.getString(37);
        this.top5Tag = cursor.getString(38);
        this.top6Tag = cursor.getString(39);
        this.top7Tag = cursor.getString(40);
        this.top8Tag = cursor.getString(41);
        this.top9Tag = cursor.getString(42);
        this.top10Tag = cursor.getString(43);
        this.onTimeWorkday = cursor.getString(44);
        this.leaveHomeTimeWorkday = cursor.getString(45);
        this.arriveWorkplaceTimeWorkday = cursor.getString(46);
        this.leaveWorkplaceTimeWorkday = cursor.getString(47);
        this.arriveHomeTimeWorkday = cursor.getString(48);
        this.offTimeWorkday = cursor.getString(49);
        this.onTimeWeekend = cursor.getString(50);
        this.leaveHomeTimeWeekend = cursor.getString(51);
        this.arriveHomeTimeWeekend = cursor.getString(52);
        this.offTimeWeekend = cursor.getString(53);
        this.imeiConfidence = cursor.isNull(54) ? null : Float.valueOf(cursor.getFloat(54));
        this.hwIdConfidence = cursor.isNull(55) ? null : Float.valueOf(cursor.getFloat(55));
        this.ageConfidence = cursor.isNull(56) ? null : Float.valueOf(cursor.getFloat(56));
        this.sexualityConfidence = cursor.isNull(57) ? null : Float.valueOf(cursor.getFloat(57));
        this.contactNumberConfidence = cursor.isNull(58) ? null : Float.valueOf(cursor.getFloat(58));
        this.accomConfidence = cursor.isNull(59) ? null : Float.valueOf(cursor.getFloat(59));
        this.workplaceConfidence = cursor.isNull(60) ? null : Float.valueOf(cursor.getFloat(60));
        this.poi1Confidence = cursor.isNull(61) ? null : Float.valueOf(cursor.getFloat(61));
        this.poi2Confidence = cursor.isNull(62) ? null : Float.valueOf(cursor.getFloat(62));
        this.poi3Confidence = cursor.isNull(63) ? null : Float.valueOf(cursor.getFloat(63));
        this.poi4Confidence = cursor.isNull(64) ? null : Float.valueOf(cursor.getFloat(64));
        this.poi5Confidence = cursor.isNull(65) ? null : Float.valueOf(cursor.getFloat(65));
        this.baseCityConfidence = cursor.isNull(66) ? null : Float.valueOf(cursor.getFloat(66));
        this.roamingRadiusConfidence = cursor.isNull(67) ? null : Float.valueOf(cursor.getFloat(67));
        this.buziTripPreferConfidence = cursor.isNull(68) ? null : Float.valueOf(cursor.getFloat(68));
        this.photoPreferConfidence = cursor.isNull(69) ? null : Float.valueOf(cursor.getFloat(69));
        this.photoPreferTurismConfidence = cursor.isNull(70) ? null : Float.valueOf(cursor.getFloat(70));
        this.topModeConfidence = cursor.isNull(71) ? null : Float.valueOf(cursor.getFloat(71));
        this.top1TagConfidence = cursor.isNull(72) ? null : Float.valueOf(cursor.getFloat(72));
        this.top2TagConfidence = cursor.isNull(73) ? null : Float.valueOf(cursor.getFloat(73));
        this.top3TagConfidence = cursor.isNull(74) ? null : Float.valueOf(cursor.getFloat(74));
        this.top4TagConfidence = cursor.isNull(75) ? null : Float.valueOf(cursor.getFloat(75));
        this.top5TagConfidence = cursor.isNull(76) ? null : Float.valueOf(cursor.getFloat(76));
        this.top6TagConfidence = cursor.isNull(77) ? null : Float.valueOf(cursor.getFloat(77));
        this.top7TagConfidence = cursor.isNull(78) ? null : Float.valueOf(cursor.getFloat(78));
        this.top8TagConfidence = cursor.isNull(79) ? null : Float.valueOf(cursor.getFloat(79));
        this.top9TagConfidence = cursor.isNull(80) ? null : Float.valueOf(cursor.getFloat(80));
        this.top10TagConfidence = cursor.isNull(81) ? null : Float.valueOf(cursor.getFloat(81));
        this.onTimeWorkdayConfidence = cursor.isNull(82) ? null : Float.valueOf(cursor.getFloat(82));
        this.leaveHomeTimeWorkdayConfidence = cursor.isNull(83) ? null : Float.valueOf(cursor.getFloat(83));
        this.arriveWorkplaceTimeWorkdayConfidence = cursor.isNull(84) ? null : Float.valueOf(cursor.getFloat(84));
        this.leaveWorkplaceTimeWorkdayConfidence = cursor.isNull(85) ? null : Float.valueOf(cursor.getFloat(85));
        this.arriveHomeTimeWorkdayConfidence = cursor.isNull(86) ? null : Float.valueOf(cursor.getFloat(86));
        this.offTimeWorkdayConfidence = cursor.isNull(87) ? null : Float.valueOf(cursor.getFloat(87));
        this.onTimeWeekendConfidence = cursor.isNull(88) ? null : Float.valueOf(cursor.getFloat(88));
        this.leaveHomeTimeWeekendConfidence = cursor.isNull(89) ? null : Float.valueOf(cursor.getFloat(89));
        this.arriveHomeTimeWeekendConfidence = cursor.isNull(90) ? null : Float.valueOf(cursor.getFloat(90));
        if (!cursor.isNull(91)) {
            f2 = Float.valueOf(cursor.getFloat(91));
        }
        this.offTimeWeekendConfidence = f2;
    }

    public MyProfile(Parcel in) {
        Float f = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.deviceID = in.readByte() == (byte) 0 ? null : in.readString();
        this.hwId = in.readByte() == (byte) 0 ? null : in.readString();
        this.age = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.sexuality = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.contactNumber = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.accomGPS = in.readByte() == (byte) 0 ? null : in.readString();
        this.accomCellId = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.accomCellLac = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.workPlaceGPS = in.readByte() == (byte) 0 ? null : in.readString();
        this.workPlaceCellId = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.workPlaceCellLac = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.poi1GPS = in.readByte() == (byte) 0 ? null : in.readString();
        this.poi1CellId = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.poi1CellLac = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.poi2GPS = in.readByte() == (byte) 0 ? null : in.readString();
        this.poi2CellId = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.poi2CellLac = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.poi3GPS = in.readByte() == (byte) 0 ? null : in.readString();
        this.poi3CellId = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.poi3CellLac = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.poi4GPS = in.readByte() == (byte) 0 ? null : in.readString();
        this.poi4CellId = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.poi4CellLac = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.poi5GPS = in.readByte() == (byte) 0 ? null : in.readString();
        this.poi5CellId = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.poi5CellLac = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.baseCity = in.readByte() == (byte) 0 ? null : in.readString();
        this.roamingRadius = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.buziTripPrefer = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.photoPrefer = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.photoPreferTurism = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.topMode = in.readByte() == (byte) 0 ? null : in.readString();
        this.top1Tag = in.readByte() == (byte) 0 ? null : in.readString();
        this.top2Tag = in.readByte() == (byte) 0 ? null : in.readString();
        this.top3Tag = in.readByte() == (byte) 0 ? null : in.readString();
        this.top4Tag = in.readByte() == (byte) 0 ? null : in.readString();
        this.top5Tag = in.readByte() == (byte) 0 ? null : in.readString();
        this.top6Tag = in.readByte() == (byte) 0 ? null : in.readString();
        this.top7Tag = in.readByte() == (byte) 0 ? null : in.readString();
        this.top8Tag = in.readByte() == (byte) 0 ? null : in.readString();
        this.top9Tag = in.readByte() == (byte) 0 ? null : in.readString();
        this.top10Tag = in.readByte() == (byte) 0 ? null : in.readString();
        this.onTimeWorkday = in.readByte() == (byte) 0 ? null : in.readString();
        this.leaveHomeTimeWorkday = in.readByte() == (byte) 0 ? null : in.readString();
        this.arriveWorkplaceTimeWorkday = in.readByte() == (byte) 0 ? null : in.readString();
        this.leaveWorkplaceTimeWorkday = in.readByte() == (byte) 0 ? null : in.readString();
        this.arriveHomeTimeWorkday = in.readByte() == (byte) 0 ? null : in.readString();
        this.offTimeWorkday = in.readByte() == (byte) 0 ? null : in.readString();
        this.onTimeWeekend = in.readByte() == (byte) 0 ? null : in.readString();
        this.leaveHomeTimeWeekend = in.readByte() == (byte) 0 ? null : in.readString();
        this.arriveHomeTimeWeekend = in.readByte() == (byte) 0 ? null : in.readString();
        this.offTimeWeekend = in.readByte() == (byte) 0 ? null : in.readString();
        this.imeiConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.hwIdConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.ageConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.sexualityConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.contactNumberConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.accomConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.workplaceConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.poi1Confidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.poi2Confidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.poi3Confidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.poi4Confidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.poi5Confidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.baseCityConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.roamingRadiusConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.buziTripPreferConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.photoPreferConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.photoPreferTurismConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.topModeConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.top1TagConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.top2TagConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.top3TagConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.top4TagConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.top5TagConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.top6TagConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.top7TagConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.top8TagConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.top9TagConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.top10TagConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.onTimeWorkdayConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.leaveHomeTimeWorkdayConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.arriveWorkplaceTimeWorkdayConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.leaveWorkplaceTimeWorkdayConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.arriveHomeTimeWorkdayConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.offTimeWorkdayConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.onTimeWeekendConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.leaveHomeTimeWeekendConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.arriveHomeTimeWeekendConfidence = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        if (in.readByte() != (byte) 0) {
            f = Float.valueOf(in.readFloat());
        }
        this.offTimeWeekendConfidence = f;
    }

    private MyProfile(Integer id, String deviceID, String hwId, Double age, Double sexuality, Integer contactNumber, String accomGPS, Integer accomCellId, Integer accomCellLac, String workPlaceGPS, Integer workPlaceCellId, Integer workPlaceCellLac, String poi1GPS, Integer poi1CellId, Integer poi1CellLac, String poi2GPS, Integer poi2CellId, Integer poi2CellLac, String poi3GPS, Integer poi3CellId, Integer poi3CellLac, String poi4GPS, Integer poi4CellId, Integer poi4CellLac, String poi5GPS, Integer poi5CellId, Integer poi5CellLac, String baseCity, Double roamingRadius, Float buziTripPrefer, Float photoPrefer, Float photoPreferTurism, String topMode, String top1Tag, String top2Tag, String top3Tag, String top4Tag, String top5Tag, String top6Tag, String top7Tag, String top8Tag, String top9Tag, String top10Tag, String onTimeWorkday, String leaveHomeTimeWorkday, String arriveWorkplaceTimeWorkday, String leaveWorkplaceTimeWorkday, String arriveHomeTimeWorkday, String offTimeWorkday, String onTimeWeekend, String leaveHomeTimeWeekend, String arriveHomeTimeWeekend, String offTimeWeekend, Float imeiConfidence, Float hwIdConfidence, Float ageConfidence, Float sexualityConfidence, Float contactNumberConfidence, Float accomConfidence, Float workplaceConfidence, Float poi1Confidence, Float poi2Confidence, Float poi3Confidence, Float poi4Confidence, Float poi5Confidence, Float baseCityConfidence, Float roamingRadiusConfidence, Float buziTripPreferConfidence, Float photoPreferConfidence, Float photoPreferTurismConfidence, Float topModeConfidence, Float top1TagConfidence, Float top2TagConfidence, Float top3TagConfidence, Float top4TagConfidence, Float top5TagConfidence, Float top6TagConfidence, Float top7TagConfidence, Float top8TagConfidence, Float top9TagConfidence, Float top10TagConfidence, Float onTimeWorkdayConfidence, Float leaveHomeTimeWorkdayConfidence, Float arriveWorkplaceTimeWorkdayConfidence, Float leaveWorkplaceTimeWorkdayConfidence, Float arriveHomeTimeWorkdayConfidence, Float offTimeWorkdayConfidence, Float onTimeWeekendConfidence, Float leaveHomeTimeWeekendConfidence, Float arriveHomeTimeWeekendConfidence, Float offTimeWeekendConfidence) {
        this.id = id;
        this.deviceID = deviceID;
        this.hwId = hwId;
        this.age = age;
        this.sexuality = sexuality;
        this.contactNumber = contactNumber;
        this.accomGPS = accomGPS;
        this.accomCellId = accomCellId;
        this.accomCellLac = accomCellLac;
        this.workPlaceGPS = workPlaceGPS;
        this.workPlaceCellId = workPlaceCellId;
        this.workPlaceCellLac = workPlaceCellLac;
        this.poi1GPS = poi1GPS;
        this.poi1CellId = poi1CellId;
        this.poi1CellLac = poi1CellLac;
        this.poi2GPS = poi2GPS;
        this.poi2CellId = poi2CellId;
        this.poi2CellLac = poi2CellLac;
        this.poi3GPS = poi3GPS;
        this.poi3CellId = poi3CellId;
        this.poi3CellLac = poi3CellLac;
        this.poi4GPS = poi4GPS;
        this.poi4CellId = poi4CellId;
        this.poi4CellLac = poi4CellLac;
        this.poi5GPS = poi5GPS;
        this.poi5CellId = poi5CellId;
        this.poi5CellLac = poi5CellLac;
        this.baseCity = baseCity;
        this.roamingRadius = roamingRadius;
        this.buziTripPrefer = buziTripPrefer;
        this.photoPrefer = photoPrefer;
        this.photoPreferTurism = photoPreferTurism;
        this.topMode = topMode;
        this.top1Tag = top1Tag;
        this.top2Tag = top2Tag;
        this.top3Tag = top3Tag;
        this.top4Tag = top4Tag;
        this.top5Tag = top5Tag;
        this.top6Tag = top6Tag;
        this.top7Tag = top7Tag;
        this.top8Tag = top8Tag;
        this.top9Tag = top9Tag;
        this.top10Tag = top10Tag;
        this.onTimeWorkday = onTimeWorkday;
        this.leaveHomeTimeWorkday = leaveHomeTimeWorkday;
        this.arriveWorkplaceTimeWorkday = arriveWorkplaceTimeWorkday;
        this.leaveWorkplaceTimeWorkday = leaveWorkplaceTimeWorkday;
        this.arriveHomeTimeWorkday = arriveHomeTimeWorkday;
        this.offTimeWorkday = offTimeWorkday;
        this.onTimeWeekend = onTimeWeekend;
        this.leaveHomeTimeWeekend = leaveHomeTimeWeekend;
        this.arriveHomeTimeWeekend = arriveHomeTimeWeekend;
        this.offTimeWeekend = offTimeWeekend;
        this.imeiConfidence = imeiConfidence;
        this.hwIdConfidence = hwIdConfidence;
        this.ageConfidence = ageConfidence;
        this.sexualityConfidence = sexualityConfidence;
        this.contactNumberConfidence = contactNumberConfidence;
        this.accomConfidence = accomConfidence;
        this.workplaceConfidence = workplaceConfidence;
        this.poi1Confidence = poi1Confidence;
        this.poi2Confidence = poi2Confidence;
        this.poi3Confidence = poi3Confidence;
        this.poi4Confidence = poi4Confidence;
        this.poi5Confidence = poi5Confidence;
        this.baseCityConfidence = baseCityConfidence;
        this.roamingRadiusConfidence = roamingRadiusConfidence;
        this.buziTripPreferConfidence = buziTripPreferConfidence;
        this.photoPreferConfidence = photoPreferConfidence;
        this.photoPreferTurismConfidence = photoPreferTurismConfidence;
        this.topModeConfidence = topModeConfidence;
        this.top1TagConfidence = top1TagConfidence;
        this.top2TagConfidence = top2TagConfidence;
        this.top3TagConfidence = top3TagConfidence;
        this.top4TagConfidence = top4TagConfidence;
        this.top5TagConfidence = top5TagConfidence;
        this.top6TagConfidence = top6TagConfidence;
        this.top7TagConfidence = top7TagConfidence;
        this.top8TagConfidence = top8TagConfidence;
        this.top9TagConfidence = top9TagConfidence;
        this.top10TagConfidence = top10TagConfidence;
        this.onTimeWorkdayConfidence = onTimeWorkdayConfidence;
        this.leaveHomeTimeWorkdayConfidence = leaveHomeTimeWorkdayConfidence;
        this.arriveWorkplaceTimeWorkdayConfidence = arriveWorkplaceTimeWorkdayConfidence;
        this.leaveWorkplaceTimeWorkdayConfidence = leaveWorkplaceTimeWorkdayConfidence;
        this.arriveHomeTimeWorkdayConfidence = arriveHomeTimeWorkdayConfidence;
        this.offTimeWorkdayConfidence = offTimeWorkdayConfidence;
        this.onTimeWeekendConfidence = onTimeWeekendConfidence;
        this.leaveHomeTimeWeekendConfidence = leaveHomeTimeWeekendConfidence;
        this.arriveHomeTimeWeekendConfidence = arriveHomeTimeWeekendConfidence;
        this.offTimeWeekendConfidence = offTimeWeekendConfidence;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
        setValue();
    }

    public String getDeviceID() {
        return this.deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
        setValue();
    }

    public String getHwId() {
        return this.hwId;
    }

    public void setHwId(String hwId) {
        this.hwId = hwId;
        setValue();
    }

    public Double getAge() {
        return this.age;
    }

    public void setAge(Double age) {
        this.age = age;
        setValue();
    }

    public Double getSexuality() {
        return this.sexuality;
    }

    public void setSexuality(Double sexuality) {
        this.sexuality = sexuality;
        setValue();
    }

    public Integer getContactNumber() {
        return this.contactNumber;
    }

    public void setContactNumber(Integer contactNumber) {
        this.contactNumber = contactNumber;
        setValue();
    }

    public String getAccomGPS() {
        return this.accomGPS;
    }

    public void setAccomGPS(String accomGPS) {
        this.accomGPS = accomGPS;
        setValue();
    }

    public Integer getAccomCellId() {
        return this.accomCellId;
    }

    public void setAccomCellId(Integer accomCellId) {
        this.accomCellId = accomCellId;
        setValue();
    }

    public Integer getAccomCellLac() {
        return this.accomCellLac;
    }

    public void setAccomCellLac(Integer accomCellLac) {
        this.accomCellLac = accomCellLac;
        setValue();
    }

    public String getWorkPlaceGPS() {
        return this.workPlaceGPS;
    }

    public void setWorkPlaceGPS(String workPlaceGPS) {
        this.workPlaceGPS = workPlaceGPS;
        setValue();
    }

    public Integer getWorkPlaceCellId() {
        return this.workPlaceCellId;
    }

    public void setWorkPlaceCellId(Integer workPlaceCellId) {
        this.workPlaceCellId = workPlaceCellId;
        setValue();
    }

    public Integer getWorkPlaceCellLac() {
        return this.workPlaceCellLac;
    }

    public void setWorkPlaceCellLac(Integer workPlaceCellLac) {
        this.workPlaceCellLac = workPlaceCellLac;
        setValue();
    }

    public String getPoi1GPS() {
        return this.poi1GPS;
    }

    public void setPoi1GPS(String poi1GPS) {
        this.poi1GPS = poi1GPS;
        setValue();
    }

    public Integer getPoi1CellId() {
        return this.poi1CellId;
    }

    public void setPoi1CellId(Integer poi1CellId) {
        this.poi1CellId = poi1CellId;
        setValue();
    }

    public Integer getPoi1CellLac() {
        return this.poi1CellLac;
    }

    public void setPoi1CellLac(Integer poi1CellLac) {
        this.poi1CellLac = poi1CellLac;
        setValue();
    }

    public String getPoi2GPS() {
        return this.poi2GPS;
    }

    public void setPoi2GPS(String poi2GPS) {
        this.poi2GPS = poi2GPS;
        setValue();
    }

    public Integer getPoi2CellId() {
        return this.poi2CellId;
    }

    public void setPoi2CellId(Integer poi2CellId) {
        this.poi2CellId = poi2CellId;
        setValue();
    }

    public Integer getPoi2CellLac() {
        return this.poi2CellLac;
    }

    public void setPoi2CellLac(Integer poi2CellLac) {
        this.poi2CellLac = poi2CellLac;
        setValue();
    }

    public String getPoi3GPS() {
        return this.poi3GPS;
    }

    public void setPoi3GPS(String poi3GPS) {
        this.poi3GPS = poi3GPS;
        setValue();
    }

    public Integer getPoi3CellId() {
        return this.poi3CellId;
    }

    public void setPoi3CellId(Integer poi3CellId) {
        this.poi3CellId = poi3CellId;
        setValue();
    }

    public Integer getPoi3CellLac() {
        return this.poi3CellLac;
    }

    public void setPoi3CellLac(Integer poi3CellLac) {
        this.poi3CellLac = poi3CellLac;
        setValue();
    }

    public String getPoi4GPS() {
        return this.poi4GPS;
    }

    public void setPoi4GPS(String poi4GPS) {
        this.poi4GPS = poi4GPS;
        setValue();
    }

    public Integer getPoi4CellId() {
        return this.poi4CellId;
    }

    public void setPoi4CellId(Integer poi4CellId) {
        this.poi4CellId = poi4CellId;
        setValue();
    }

    public Integer getPoi4CellLac() {
        return this.poi4CellLac;
    }

    public void setPoi4CellLac(Integer poi4CellLac) {
        this.poi4CellLac = poi4CellLac;
        setValue();
    }

    public String getPoi5GPS() {
        return this.poi5GPS;
    }

    public void setPoi5GPS(String poi5GPS) {
        this.poi5GPS = poi5GPS;
        setValue();
    }

    public Integer getPoi5CellId() {
        return this.poi5CellId;
    }

    public void setPoi5CellId(Integer poi5CellId) {
        this.poi5CellId = poi5CellId;
        setValue();
    }

    public Integer getPoi5CellLac() {
        return this.poi5CellLac;
    }

    public void setPoi5CellLac(Integer poi5CellLac) {
        this.poi5CellLac = poi5CellLac;
        setValue();
    }

    public String getBaseCity() {
        return this.baseCity;
    }

    public void setBaseCity(String baseCity) {
        this.baseCity = baseCity;
        setValue();
    }

    public Double getRoamingRadius() {
        return this.roamingRadius;
    }

    public void setRoamingRadius(Double roamingRadius) {
        this.roamingRadius = roamingRadius;
        setValue();
    }

    public Float getBuziTripPrefer() {
        return this.buziTripPrefer;
    }

    public void setBuziTripPrefer(Float buziTripPrefer) {
        this.buziTripPrefer = buziTripPrefer;
        setValue();
    }

    public Float getPhotoPrefer() {
        return this.photoPrefer;
    }

    public void setPhotoPrefer(Float photoPrefer) {
        this.photoPrefer = photoPrefer;
        setValue();
    }

    public Float getPhotoPreferTurism() {
        return this.photoPreferTurism;
    }

    public void setPhotoPreferTurism(Float photoPreferTurism) {
        this.photoPreferTurism = photoPreferTurism;
        setValue();
    }

    public String getTopMode() {
        return this.topMode;
    }

    public void setTopMode(String topMode) {
        this.topMode = topMode;
        setValue();
    }

    public String getTop1Tag() {
        return this.top1Tag;
    }

    public void setTop1Tag(String top1Tag) {
        this.top1Tag = top1Tag;
        setValue();
    }

    public String getTop2Tag() {
        return this.top2Tag;
    }

    public void setTop2Tag(String top2Tag) {
        this.top2Tag = top2Tag;
        setValue();
    }

    public String getTop3Tag() {
        return this.top3Tag;
    }

    public void setTop3Tag(String top3Tag) {
        this.top3Tag = top3Tag;
        setValue();
    }

    public String getTop4Tag() {
        return this.top4Tag;
    }

    public void setTop4Tag(String top4Tag) {
        this.top4Tag = top4Tag;
        setValue();
    }

    public String getTop5Tag() {
        return this.top5Tag;
    }

    public void setTop5Tag(String top5Tag) {
        this.top5Tag = top5Tag;
        setValue();
    }

    public String getTop6Tag() {
        return this.top6Tag;
    }

    public void setTop6Tag(String top6Tag) {
        this.top6Tag = top6Tag;
        setValue();
    }

    public String getTop7Tag() {
        return this.top7Tag;
    }

    public void setTop7Tag(String top7Tag) {
        this.top7Tag = top7Tag;
        setValue();
    }

    public String getTop8Tag() {
        return this.top8Tag;
    }

    public void setTop8Tag(String top8Tag) {
        this.top8Tag = top8Tag;
        setValue();
    }

    public String getTop9Tag() {
        return this.top9Tag;
    }

    public void setTop9Tag(String top9Tag) {
        this.top9Tag = top9Tag;
        setValue();
    }

    public String getTop10Tag() {
        return this.top10Tag;
    }

    public void setTop10Tag(String top10Tag) {
        this.top10Tag = top10Tag;
        setValue();
    }

    public String getOnTimeWorkday() {
        return this.onTimeWorkday;
    }

    public void setOnTimeWorkday(String onTimeWorkday) {
        this.onTimeWorkday = onTimeWorkday;
        setValue();
    }

    public String getLeaveHomeTimeWorkday() {
        return this.leaveHomeTimeWorkday;
    }

    public void setLeaveHomeTimeWorkday(String leaveHomeTimeWorkday) {
        this.leaveHomeTimeWorkday = leaveHomeTimeWorkday;
        setValue();
    }

    public String getArriveWorkplaceTimeWorkday() {
        return this.arriveWorkplaceTimeWorkday;
    }

    public void setArriveWorkplaceTimeWorkday(String arriveWorkplaceTimeWorkday) {
        this.arriveWorkplaceTimeWorkday = arriveWorkplaceTimeWorkday;
        setValue();
    }

    public String getLeaveWorkplaceTimeWorkday() {
        return this.leaveWorkplaceTimeWorkday;
    }

    public void setLeaveWorkplaceTimeWorkday(String leaveWorkplaceTimeWorkday) {
        this.leaveWorkplaceTimeWorkday = leaveWorkplaceTimeWorkday;
        setValue();
    }

    public String getArriveHomeTimeWorkday() {
        return this.arriveHomeTimeWorkday;
    }

    public void setArriveHomeTimeWorkday(String arriveHomeTimeWorkday) {
        this.arriveHomeTimeWorkday = arriveHomeTimeWorkday;
        setValue();
    }

    public String getOffTimeWorkday() {
        return this.offTimeWorkday;
    }

    public void setOffTimeWorkday(String offTimeWorkday) {
        this.offTimeWorkday = offTimeWorkday;
        setValue();
    }

    public String getOnTimeWeekend() {
        return this.onTimeWeekend;
    }

    public void setOnTimeWeekend(String onTimeWeekend) {
        this.onTimeWeekend = onTimeWeekend;
        setValue();
    }

    public String getLeaveHomeTimeWeekend() {
        return this.leaveHomeTimeWeekend;
    }

    public void setLeaveHomeTimeWeekend(String leaveHomeTimeWeekend) {
        this.leaveHomeTimeWeekend = leaveHomeTimeWeekend;
        setValue();
    }

    public String getArriveHomeTimeWeekend() {
        return this.arriveHomeTimeWeekend;
    }

    public void setArriveHomeTimeWeekend(String arriveHomeTimeWeekend) {
        this.arriveHomeTimeWeekend = arriveHomeTimeWeekend;
        setValue();
    }

    public String getOffTimeWeekend() {
        return this.offTimeWeekend;
    }

    public void setOffTimeWeekend(String offTimeWeekend) {
        this.offTimeWeekend = offTimeWeekend;
        setValue();
    }

    public Float getImeiConfidence() {
        return this.imeiConfidence;
    }

    public void setImeiConfidence(Float imeiConfidence) {
        this.imeiConfidence = imeiConfidence;
        setValue();
    }

    public Float getHwIdConfidence() {
        return this.hwIdConfidence;
    }

    public void setHwIdConfidence(Float hwIdConfidence) {
        this.hwIdConfidence = hwIdConfidence;
        setValue();
    }

    public Float getAgeConfidence() {
        return this.ageConfidence;
    }

    public void setAgeConfidence(Float ageConfidence) {
        this.ageConfidence = ageConfidence;
        setValue();
    }

    public Float getSexualityConfidence() {
        return this.sexualityConfidence;
    }

    public void setSexualityConfidence(Float sexualityConfidence) {
        this.sexualityConfidence = sexualityConfidence;
        setValue();
    }

    public Float getContactNumberConfidence() {
        return this.contactNumberConfidence;
    }

    public void setContactNumberConfidence(Float contactNumberConfidence) {
        this.contactNumberConfidence = contactNumberConfidence;
        setValue();
    }

    public Float getAccomConfidence() {
        return this.accomConfidence;
    }

    public void setAccomConfidence(Float accomConfidence) {
        this.accomConfidence = accomConfidence;
        setValue();
    }

    public Float getWorkplaceConfidence() {
        return this.workplaceConfidence;
    }

    public void setWorkplaceConfidence(Float workplaceConfidence) {
        this.workplaceConfidence = workplaceConfidence;
        setValue();
    }

    public Float getPoi1Confidence() {
        return this.poi1Confidence;
    }

    public void setPoi1Confidence(Float poi1Confidence) {
        this.poi1Confidence = poi1Confidence;
        setValue();
    }

    public Float getPoi2Confidence() {
        return this.poi2Confidence;
    }

    public void setPoi2Confidence(Float poi2Confidence) {
        this.poi2Confidence = poi2Confidence;
        setValue();
    }

    public Float getPoi3Confidence() {
        return this.poi3Confidence;
    }

    public void setPoi3Confidence(Float poi3Confidence) {
        this.poi3Confidence = poi3Confidence;
        setValue();
    }

    public Float getPoi4Confidence() {
        return this.poi4Confidence;
    }

    public void setPoi4Confidence(Float poi4Confidence) {
        this.poi4Confidence = poi4Confidence;
        setValue();
    }

    public Float getPoi5Confidence() {
        return this.poi5Confidence;
    }

    public void setPoi5Confidence(Float poi5Confidence) {
        this.poi5Confidence = poi5Confidence;
        setValue();
    }

    public Float getBaseCityConfidence() {
        return this.baseCityConfidence;
    }

    public void setBaseCityConfidence(Float baseCityConfidence) {
        this.baseCityConfidence = baseCityConfidence;
        setValue();
    }

    public Float getRoamingRadiusConfidence() {
        return this.roamingRadiusConfidence;
    }

    public void setRoamingRadiusConfidence(Float roamingRadiusConfidence) {
        this.roamingRadiusConfidence = roamingRadiusConfidence;
        setValue();
    }

    public Float getBuziTripPreferConfidence() {
        return this.buziTripPreferConfidence;
    }

    public void setBuziTripPreferConfidence(Float buziTripPreferConfidence) {
        this.buziTripPreferConfidence = buziTripPreferConfidence;
        setValue();
    }

    public Float getPhotoPreferConfidence() {
        return this.photoPreferConfidence;
    }

    public void setPhotoPreferConfidence(Float photoPreferConfidence) {
        this.photoPreferConfidence = photoPreferConfidence;
        setValue();
    }

    public Float getPhotoPreferTurismConfidence() {
        return this.photoPreferTurismConfidence;
    }

    public void setPhotoPreferTurismConfidence(Float photoPreferTurismConfidence) {
        this.photoPreferTurismConfidence = photoPreferTurismConfidence;
        setValue();
    }

    public Float getTopModeConfidence() {
        return this.topModeConfidence;
    }

    public void setTopModeConfidence(Float topModeConfidence) {
        this.topModeConfidence = topModeConfidence;
        setValue();
    }

    public Float getTop1TagConfidence() {
        return this.top1TagConfidence;
    }

    public void setTop1TagConfidence(Float top1TagConfidence) {
        this.top1TagConfidence = top1TagConfidence;
        setValue();
    }

    public Float getTop2TagConfidence() {
        return this.top2TagConfidence;
    }

    public void setTop2TagConfidence(Float top2TagConfidence) {
        this.top2TagConfidence = top2TagConfidence;
        setValue();
    }

    public Float getTop3TagConfidence() {
        return this.top3TagConfidence;
    }

    public void setTop3TagConfidence(Float top3TagConfidence) {
        this.top3TagConfidence = top3TagConfidence;
        setValue();
    }

    public Float getTop4TagConfidence() {
        return this.top4TagConfidence;
    }

    public void setTop4TagConfidence(Float top4TagConfidence) {
        this.top4TagConfidence = top4TagConfidence;
        setValue();
    }

    public Float getTop5TagConfidence() {
        return this.top5TagConfidence;
    }

    public void setTop5TagConfidence(Float top5TagConfidence) {
        this.top5TagConfidence = top5TagConfidence;
        setValue();
    }

    public Float getTop6TagConfidence() {
        return this.top6TagConfidence;
    }

    public void setTop6TagConfidence(Float top6TagConfidence) {
        this.top6TagConfidence = top6TagConfidence;
        setValue();
    }

    public Float getTop7TagConfidence() {
        return this.top7TagConfidence;
    }

    public void setTop7TagConfidence(Float top7TagConfidence) {
        this.top7TagConfidence = top7TagConfidence;
        setValue();
    }

    public Float getTop8TagConfidence() {
        return this.top8TagConfidence;
    }

    public void setTop8TagConfidence(Float top8TagConfidence) {
        this.top8TagConfidence = top8TagConfidence;
        setValue();
    }

    public Float getTop9TagConfidence() {
        return this.top9TagConfidence;
    }

    public void setTop9TagConfidence(Float top9TagConfidence) {
        this.top9TagConfidence = top9TagConfidence;
        setValue();
    }

    public Float getTop10TagConfidence() {
        return this.top10TagConfidence;
    }

    public void setTop10TagConfidence(Float top10TagConfidence) {
        this.top10TagConfidence = top10TagConfidence;
        setValue();
    }

    public Float getOnTimeWorkdayConfidence() {
        return this.onTimeWorkdayConfidence;
    }

    public void setOnTimeWorkdayConfidence(Float onTimeWorkdayConfidence) {
        this.onTimeWorkdayConfidence = onTimeWorkdayConfidence;
        setValue();
    }

    public Float getLeaveHomeTimeWorkdayConfidence() {
        return this.leaveHomeTimeWorkdayConfidence;
    }

    public void setLeaveHomeTimeWorkdayConfidence(Float leaveHomeTimeWorkdayConfidence) {
        this.leaveHomeTimeWorkdayConfidence = leaveHomeTimeWorkdayConfidence;
        setValue();
    }

    public Float getArriveWorkplaceTimeWorkdayConfidence() {
        return this.arriveWorkplaceTimeWorkdayConfidence;
    }

    public void setArriveWorkplaceTimeWorkdayConfidence(Float arriveWorkplaceTimeWorkdayConfidence) {
        this.arriveWorkplaceTimeWorkdayConfidence = arriveWorkplaceTimeWorkdayConfidence;
        setValue();
    }

    public Float getLeaveWorkplaceTimeWorkdayConfidence() {
        return this.leaveWorkplaceTimeWorkdayConfidence;
    }

    public void setLeaveWorkplaceTimeWorkdayConfidence(Float leaveWorkplaceTimeWorkdayConfidence) {
        this.leaveWorkplaceTimeWorkdayConfidence = leaveWorkplaceTimeWorkdayConfidence;
        setValue();
    }

    public Float getArriveHomeTimeWorkdayConfidence() {
        return this.arriveHomeTimeWorkdayConfidence;
    }

    public void setArriveHomeTimeWorkdayConfidence(Float arriveHomeTimeWorkdayConfidence) {
        this.arriveHomeTimeWorkdayConfidence = arriveHomeTimeWorkdayConfidence;
        setValue();
    }

    public Float getOffTimeWorkdayConfidence() {
        return this.offTimeWorkdayConfidence;
    }

    public void setOffTimeWorkdayConfidence(Float offTimeWorkdayConfidence) {
        this.offTimeWorkdayConfidence = offTimeWorkdayConfidence;
        setValue();
    }

    public Float getOnTimeWeekendConfidence() {
        return this.onTimeWeekendConfidence;
    }

    public void setOnTimeWeekendConfidence(Float onTimeWeekendConfidence) {
        this.onTimeWeekendConfidence = onTimeWeekendConfidence;
        setValue();
    }

    public Float getLeaveHomeTimeWeekendConfidence() {
        return this.leaveHomeTimeWeekendConfidence;
    }

    public void setLeaveHomeTimeWeekendConfidence(Float leaveHomeTimeWeekendConfidence) {
        this.leaveHomeTimeWeekendConfidence = leaveHomeTimeWeekendConfidence;
        setValue();
    }

    public Float getArriveHomeTimeWeekendConfidence() {
        return this.arriveHomeTimeWeekendConfidence;
    }

    public void setArriveHomeTimeWeekendConfidence(Float arriveHomeTimeWeekendConfidence) {
        this.arriveHomeTimeWeekendConfidence = arriveHomeTimeWeekendConfidence;
        setValue();
    }

    public Float getOffTimeWeekendConfidence() {
        return this.offTimeWeekendConfidence;
    }

    public void setOffTimeWeekendConfidence(Float offTimeWeekendConfidence) {
        this.offTimeWeekendConfidence = offTimeWeekendConfidence;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.id.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.deviceID != null) {
            out.writeByte((byte) 1);
            out.writeString(this.deviceID);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.hwId != null) {
            out.writeByte((byte) 1);
            out.writeString(this.hwId);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.age != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.age.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.sexuality != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.sexuality.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.contactNumber != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.contactNumber.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.accomGPS != null) {
            out.writeByte((byte) 1);
            out.writeString(this.accomGPS);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.accomCellId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.accomCellId.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.accomCellLac != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.accomCellLac.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.workPlaceGPS != null) {
            out.writeByte((byte) 1);
            out.writeString(this.workPlaceGPS);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.workPlaceCellId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.workPlaceCellId.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.workPlaceCellLac != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.workPlaceCellLac.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi1GPS != null) {
            out.writeByte((byte) 1);
            out.writeString(this.poi1GPS);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi1CellId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.poi1CellId.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi1CellLac != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.poi1CellLac.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi2GPS != null) {
            out.writeByte((byte) 1);
            out.writeString(this.poi2GPS);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi2CellId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.poi2CellId.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi2CellLac != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.poi2CellLac.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi3GPS != null) {
            out.writeByte((byte) 1);
            out.writeString(this.poi3GPS);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi3CellId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.poi3CellId.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi3CellLac != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.poi3CellLac.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi4GPS != null) {
            out.writeByte((byte) 1);
            out.writeString(this.poi4GPS);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi4CellId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.poi4CellId.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi4CellLac != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.poi4CellLac.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi5GPS != null) {
            out.writeByte((byte) 1);
            out.writeString(this.poi5GPS);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi5CellId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.poi5CellId.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi5CellLac != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.poi5CellLac.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.baseCity != null) {
            out.writeByte((byte) 1);
            out.writeString(this.baseCity);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.roamingRadius != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.roamingRadius.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.buziTripPrefer != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.buziTripPrefer.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.photoPrefer != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.photoPrefer.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.photoPreferTurism != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.photoPreferTurism.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.topMode != null) {
            out.writeByte((byte) 1);
            out.writeString(this.topMode);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top1Tag != null) {
            out.writeByte((byte) 1);
            out.writeString(this.top1Tag);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top2Tag != null) {
            out.writeByte((byte) 1);
            out.writeString(this.top2Tag);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top3Tag != null) {
            out.writeByte((byte) 1);
            out.writeString(this.top3Tag);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top4Tag != null) {
            out.writeByte((byte) 1);
            out.writeString(this.top4Tag);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top5Tag != null) {
            out.writeByte((byte) 1);
            out.writeString(this.top5Tag);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top6Tag != null) {
            out.writeByte((byte) 1);
            out.writeString(this.top6Tag);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top7Tag != null) {
            out.writeByte((byte) 1);
            out.writeString(this.top7Tag);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top8Tag != null) {
            out.writeByte((byte) 1);
            out.writeString(this.top8Tag);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top9Tag != null) {
            out.writeByte((byte) 1);
            out.writeString(this.top9Tag);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top10Tag != null) {
            out.writeByte((byte) 1);
            out.writeString(this.top10Tag);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.onTimeWorkday != null) {
            out.writeByte((byte) 1);
            out.writeString(this.onTimeWorkday);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.leaveHomeTimeWorkday != null) {
            out.writeByte((byte) 1);
            out.writeString(this.leaveHomeTimeWorkday);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.arriveWorkplaceTimeWorkday != null) {
            out.writeByte((byte) 1);
            out.writeString(this.arriveWorkplaceTimeWorkday);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.leaveWorkplaceTimeWorkday != null) {
            out.writeByte((byte) 1);
            out.writeString(this.leaveWorkplaceTimeWorkday);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.arriveHomeTimeWorkday != null) {
            out.writeByte((byte) 1);
            out.writeString(this.arriveHomeTimeWorkday);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.offTimeWorkday != null) {
            out.writeByte((byte) 1);
            out.writeString(this.offTimeWorkday);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.onTimeWeekend != null) {
            out.writeByte((byte) 1);
            out.writeString(this.onTimeWeekend);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.leaveHomeTimeWeekend != null) {
            out.writeByte((byte) 1);
            out.writeString(this.leaveHomeTimeWeekend);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.arriveHomeTimeWeekend != null) {
            out.writeByte((byte) 1);
            out.writeString(this.arriveHomeTimeWeekend);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.offTimeWeekend != null) {
            out.writeByte((byte) 1);
            out.writeString(this.offTimeWeekend);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.imeiConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.imeiConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.hwIdConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.hwIdConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.ageConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.ageConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.sexualityConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.sexualityConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.contactNumberConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.contactNumberConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.accomConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.accomConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.workplaceConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.workplaceConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi1Confidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.poi1Confidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi2Confidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.poi2Confidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi3Confidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.poi3Confidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi4Confidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.poi4Confidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi5Confidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.poi5Confidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.baseCityConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.baseCityConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.roamingRadiusConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.roamingRadiusConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.buziTripPreferConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.buziTripPreferConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.photoPreferConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.photoPreferConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.photoPreferTurismConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.photoPreferTurismConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.topModeConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.topModeConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top1TagConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.top1TagConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top2TagConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.top2TagConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top3TagConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.top3TagConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top4TagConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.top4TagConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top5TagConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.top5TagConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top6TagConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.top6TagConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top7TagConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.top7TagConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top8TagConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.top8TagConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top9TagConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.top9TagConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top10TagConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.top10TagConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.onTimeWorkdayConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.onTimeWorkdayConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.leaveHomeTimeWorkdayConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.leaveHomeTimeWorkdayConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.arriveWorkplaceTimeWorkdayConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.arriveWorkplaceTimeWorkdayConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.leaveWorkplaceTimeWorkdayConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.leaveWorkplaceTimeWorkdayConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.arriveHomeTimeWorkdayConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.arriveHomeTimeWorkdayConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.offTimeWorkdayConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.offTimeWorkdayConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.onTimeWeekendConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.onTimeWeekendConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.leaveHomeTimeWeekendConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.leaveHomeTimeWeekendConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.arriveHomeTimeWeekendConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.arriveHomeTimeWeekendConfidence.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.offTimeWeekendConfidence != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.offTimeWeekendConfidence.floatValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<MyProfile> getHelper() {
        return MyProfileHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.profile.MyProfile";
    }

    public String getDatabaseName() {
        return "dsServiceMetaData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("MyProfile { id: ").append(this.id);
        sb.append(", deviceID: ").append(this.deviceID);
        sb.append(", hwId: ").append(this.hwId);
        sb.append(", age: ").append(this.age);
        sb.append(", sexuality: ").append(this.sexuality);
        sb.append(", contactNumber: ").append(this.contactNumber);
        sb.append(", accomGPS: ").append(this.accomGPS);
        sb.append(", accomCellId: ").append(this.accomCellId);
        sb.append(", accomCellLac: ").append(this.accomCellLac);
        sb.append(", workPlaceGPS: ").append(this.workPlaceGPS);
        sb.append(", workPlaceCellId: ").append(this.workPlaceCellId);
        sb.append(", workPlaceCellLac: ").append(this.workPlaceCellLac);
        sb.append(", poi1GPS: ").append(this.poi1GPS);
        sb.append(", poi1CellId: ").append(this.poi1CellId);
        sb.append(", poi1CellLac: ").append(this.poi1CellLac);
        sb.append(", poi2GPS: ").append(this.poi2GPS);
        sb.append(", poi2CellId: ").append(this.poi2CellId);
        sb.append(", poi2CellLac: ").append(this.poi2CellLac);
        sb.append(", poi3GPS: ").append(this.poi3GPS);
        sb.append(", poi3CellId: ").append(this.poi3CellId);
        sb.append(", poi3CellLac: ").append(this.poi3CellLac);
        sb.append(", poi4GPS: ").append(this.poi4GPS);
        sb.append(", poi4CellId: ").append(this.poi4CellId);
        sb.append(", poi4CellLac: ").append(this.poi4CellLac);
        sb.append(", poi5GPS: ").append(this.poi5GPS);
        sb.append(", poi5CellId: ").append(this.poi5CellId);
        sb.append(", poi5CellLac: ").append(this.poi5CellLac);
        sb.append(", baseCity: ").append(this.baseCity);
        sb.append(", roamingRadius: ").append(this.roamingRadius);
        sb.append(", buziTripPrefer: ").append(this.buziTripPrefer);
        sb.append(", photoPrefer: ").append(this.photoPrefer);
        sb.append(", photoPreferTurism: ").append(this.photoPreferTurism);
        sb.append(", topMode: ").append(this.topMode);
        sb.append(", top1Tag: ").append(this.top1Tag);
        sb.append(", top2Tag: ").append(this.top2Tag);
        sb.append(", top3Tag: ").append(this.top3Tag);
        sb.append(", top4Tag: ").append(this.top4Tag);
        sb.append(", top5Tag: ").append(this.top5Tag);
        sb.append(", top6Tag: ").append(this.top6Tag);
        sb.append(", top7Tag: ").append(this.top7Tag);
        sb.append(", top8Tag: ").append(this.top8Tag);
        sb.append(", top9Tag: ").append(this.top9Tag);
        sb.append(", top10Tag: ").append(this.top10Tag);
        sb.append(", onTimeWorkday: ").append(this.onTimeWorkday);
        sb.append(", leaveHomeTimeWorkday: ").append(this.leaveHomeTimeWorkday);
        sb.append(", arriveWorkplaceTimeWorkday: ").append(this.arriveWorkplaceTimeWorkday);
        sb.append(", leaveWorkplaceTimeWorkday: ").append(this.leaveWorkplaceTimeWorkday);
        sb.append(", arriveHomeTimeWorkday: ").append(this.arriveHomeTimeWorkday);
        sb.append(", offTimeWorkday: ").append(this.offTimeWorkday);
        sb.append(", onTimeWeekend: ").append(this.onTimeWeekend);
        sb.append(", leaveHomeTimeWeekend: ").append(this.leaveHomeTimeWeekend);
        sb.append(", arriveHomeTimeWeekend: ").append(this.arriveHomeTimeWeekend);
        sb.append(", offTimeWeekend: ").append(this.offTimeWeekend);
        sb.append(", imeiConfidence: ").append(this.imeiConfidence);
        sb.append(", hwIdConfidence: ").append(this.hwIdConfidence);
        sb.append(", ageConfidence: ").append(this.ageConfidence);
        sb.append(", sexualityConfidence: ").append(this.sexualityConfidence);
        sb.append(", contactNumberConfidence: ").append(this.contactNumberConfidence);
        sb.append(", accomConfidence: ").append(this.accomConfidence);
        sb.append(", workplaceConfidence: ").append(this.workplaceConfidence);
        sb.append(", poi1Confidence: ").append(this.poi1Confidence);
        sb.append(", poi2Confidence: ").append(this.poi2Confidence);
        sb.append(", poi3Confidence: ").append(this.poi3Confidence);
        sb.append(", poi4Confidence: ").append(this.poi4Confidence);
        sb.append(", poi5Confidence: ").append(this.poi5Confidence);
        sb.append(", baseCityConfidence: ").append(this.baseCityConfidence);
        sb.append(", roamingRadiusConfidence: ").append(this.roamingRadiusConfidence);
        sb.append(", buziTripPreferConfidence: ").append(this.buziTripPreferConfidence);
        sb.append(", photoPreferConfidence: ").append(this.photoPreferConfidence);
        sb.append(", photoPreferTurismConfidence: ").append(this.photoPreferTurismConfidence);
        sb.append(", topModeConfidence: ").append(this.topModeConfidence);
        sb.append(", top1TagConfidence: ").append(this.top1TagConfidence);
        sb.append(", top2TagConfidence: ").append(this.top2TagConfidence);
        sb.append(", top3TagConfidence: ").append(this.top3TagConfidence);
        sb.append(", top4TagConfidence: ").append(this.top4TagConfidence);
        sb.append(", top5TagConfidence: ").append(this.top5TagConfidence);
        sb.append(", top6TagConfidence: ").append(this.top6TagConfidence);
        sb.append(", top7TagConfidence: ").append(this.top7TagConfidence);
        sb.append(", top8TagConfidence: ").append(this.top8TagConfidence);
        sb.append(", top9TagConfidence: ").append(this.top9TagConfidence);
        sb.append(", top10TagConfidence: ").append(this.top10TagConfidence);
        sb.append(", onTimeWorkdayConfidence: ").append(this.onTimeWorkdayConfidence);
        sb.append(", leaveHomeTimeWorkdayConfidence: ").append(this.leaveHomeTimeWorkdayConfidence);
        sb.append(", arriveWorkplaceTimeWorkdayConfidence: ").append(this.arriveWorkplaceTimeWorkdayConfidence);
        sb.append(", leaveWorkplaceTimeWorkdayConfidence: ").append(this.leaveWorkplaceTimeWorkdayConfidence);
        sb.append(", arriveHomeTimeWorkdayConfidence: ").append(this.arriveHomeTimeWorkdayConfidence);
        sb.append(", offTimeWorkdayConfidence: ").append(this.offTimeWorkdayConfidence);
        sb.append(", onTimeWeekendConfidence: ").append(this.onTimeWeekendConfidence);
        sb.append(", leaveHomeTimeWeekendConfidence: ").append(this.leaveHomeTimeWeekendConfidence);
        sb.append(", arriveHomeTimeWeekendConfidence: ").append(this.arriveHomeTimeWeekendConfidence);
        sb.append(", offTimeWeekendConfidence: ").append(this.offTimeWeekendConfidence);
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
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
