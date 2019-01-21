package android.mtp;

import android.app.DownloadManager;
import android.content.ContentProviderClient;
import android.database.Cursor;
import android.media.midi.MidiDeviceInfo;
import android.mtp.MtpStorageManager.MtpObject;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.MediaStore.Audio.Genres;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Files;
import android.util.Log;
import java.util.ArrayList;

class MtpPropertyGroup {
    private static final int FILE_SIZE_MAGNIFICATION = 3;
    private static final String PATH_WHERE = "_data=?";
    private static final String SUFFIX_HEIC_FILE = ".HEIC";
    private static final String SUFFIX_HEIF_FILE = ".HEIF";
    private static final String SUFFIX_JPG_FILE = ".jpg";
    private static final String TAG = MtpPropertyGroup.class.getSimpleName();
    private String[] mColumns;
    private final Property[] mProperties;
    private final ContentProviderClient mProvider;
    private final Uri mUri;
    private final String mVolumeName;

    private class Property {
        int code;
        int column;
        int type;

        Property(int code, int type, int column) {
            this.code = code;
            this.type = type;
            this.column = column;
        }
    }

    public static native String format_date_time(long j);

    public MtpPropertyGroup(ContentProviderClient provider, String volumeName, int[] properties) {
        this.mProvider = provider;
        this.mVolumeName = volumeName;
        this.mUri = Files.getMtpObjectsUri(volumeName).buildUpon().appendQueryParameter("nonotify", "1").build();
        int count = properties.length;
        ArrayList<String> columns = new ArrayList(count);
        columns.add(DownloadManager.COLUMN_ID);
        columns.add(DownloadManager.COLUMN_MEDIA_TYPE);
        this.mProperties = new Property[count];
        int i = 0;
        for (int i2 = 0; i2 < count; i2++) {
            this.mProperties[i2] = createProperty(properties[i2], columns);
        }
        count = columns.size();
        this.mColumns = new String[count];
        while (i < count) {
            this.mColumns[i] = (String) columns.get(i);
            i++;
        }
    }

    private Property createProperty(int code, ArrayList<String> columns) {
        int type;
        String column = null;
        switch (code) {
            case MtpConstants.PROPERTY_STORAGE_ID /*56321*/:
                type = 6;
                break;
            case MtpConstants.PROPERTY_OBJECT_FORMAT /*56322*/:
                type = 4;
                break;
            case MtpConstants.PROPERTY_PROTECTION_STATUS /*56323*/:
                type = 4;
                break;
            case MtpConstants.PROPERTY_OBJECT_SIZE /*56324*/:
                type = 8;
                break;
            case MtpConstants.PROPERTY_OBJECT_FILE_NAME /*56327*/:
                type = 65535;
                break;
            case MtpConstants.PROPERTY_DATE_MODIFIED /*56329*/:
                type = 65535;
                break;
            case MtpConstants.PROPERTY_PARENT_OBJECT /*56331*/:
                type = 6;
                break;
            case MtpConstants.PROPERTY_PERSISTENT_UID /*56385*/:
                type = 10;
                break;
            case MtpConstants.PROPERTY_NAME /*56388*/:
                type = 65535;
                break;
            case MtpConstants.PROPERTY_ARTIST /*56390*/:
                type = 65535;
                break;
            case MtpConstants.PROPERTY_DESCRIPTION /*56392*/:
                column = "description";
                type = 65535;
                break;
            case MtpConstants.PROPERTY_DATE_ADDED /*56398*/:
                type = 65535;
                break;
            case MtpConstants.PROPERTY_DURATION /*56457*/:
                column = "duration";
                type = 6;
                break;
            case MtpConstants.PROPERTY_TRACK /*56459*/:
                column = "track";
                type = 4;
                break;
            case MtpConstants.PROPERTY_GENRE /*56460*/:
                type = 65535;
                break;
            case MtpConstants.PROPERTY_COMPOSER /*56470*/:
                column = "composer";
                type = 65535;
                break;
            case MtpConstants.PROPERTY_ORIGINAL_RELEASE_DATE /*56473*/:
                column = "year";
                type = 65535;
                break;
            case MtpConstants.PROPERTY_ALBUM_NAME /*56474*/:
                type = 65535;
                break;
            case MtpConstants.PROPERTY_ALBUM_ARTIST /*56475*/:
                column = "album_artist";
                type = 65535;
                break;
            case MtpConstants.PROPERTY_DISPLAY_NAME /*56544*/:
                type = 65535;
                break;
            case MtpConstants.PROPERTY_BITRATE_TYPE /*56978*/:
            case MtpConstants.PROPERTY_NUMBER_OF_CHANNELS /*56980*/:
                type = 4;
                break;
            case MtpConstants.PROPERTY_SAMPLE_RATE /*56979*/:
            case MtpConstants.PROPERTY_AUDIO_WAVE_CODEC /*56985*/:
            case MtpConstants.PROPERTY_AUDIO_BITRATE /*56986*/:
                type = 6;
                break;
            default:
                type = 0;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unsupported property ");
                stringBuilder.append(code);
                Log.e(str, stringBuilder.toString());
                break;
        }
        if (column == null) {
            return new Property(code, type, -1);
        }
        columns.add(column);
        return new Property(code, type, columns.size() - 1);
    }

    private String queryAudio(String path, String column) {
        Cursor c = null;
        String str;
        try {
            Uri audioUri = Media.getContentUri(this.mVolumeName).buildUpon().appendQueryParameter("nonotify", "1").build();
            c = this.mProvider.query(Media.getContentUri(this.mVolumeName), new String[]{column}, PATH_WHERE, new String[]{path}, null, null);
            if (c == null || !c.moveToNext()) {
                str = "";
                if (c != null) {
                    c.close();
                }
                return str;
            }
            str = c.getString(0);
            if (c != null) {
                c.close();
            }
            return str;
        } catch (Exception e) {
            str = "";
            if (c != null) {
                c.close();
            }
            return str;
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }

    private String queryGenre(String path) {
        Cursor c = null;
        try {
            Uri uri = Genres.getContentUri(this.mVolumeName).buildUpon().appendQueryParameter("nonotify", "1").build();
            c = this.mProvider.query(uri, new String[]{MidiDeviceInfo.PROPERTY_NAME}, PATH_WHERE, new String[]{path}, null, null);
            String str;
            if (c == null || !c.moveToNext()) {
                str = "";
                if (c != null) {
                    c.close();
                }
                return str;
            }
            str = c.getString(0);
            if (c != null) {
                c.close();
            }
            return str;
        } catch (Exception e) {
            String str2 = "";
            if (c != null) {
                c.close();
            }
            return str2;
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }

    public int getPropertyList(MtpObject object, MtpPropertyList list) {
        MtpPropertyGroup mtpPropertyGroup = this;
        MtpPropertyList mtpPropertyList = list;
        int id = object.getId();
        String path = object.getPath().toString();
        Property[] propertyArr = mtpPropertyGroup.mProperties;
        int length = propertyArr.length;
        Cursor c = null;
        int i = 0;
        while (i < length) {
            Property property = propertyArr[i];
            boolean isHeifFile = true;
            if (property.column != -1 && c == null) {
                try {
                    c = mtpPropertyGroup.mProvider.query(mtpPropertyGroup.mUri, mtpPropertyGroup.mColumns, PATH_WHERE, new String[]{path}, null, null);
                    if (!(c == null || c.moveToNext())) {
                        c.close();
                        c = null;
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Mediaprovider lookup failed");
                }
            }
            Cursor c2 = c;
            int suffixIndex;
            switch (property.code) {
                case MtpConstants.PROPERTY_STORAGE_ID /*56321*/:
                    mtpPropertyList.append(id, property.code, property.type, (long) object.getStorageId());
                    break;
                case MtpConstants.PROPERTY_OBJECT_FORMAT /*56322*/:
                    mtpPropertyList.append(id, property.code, property.type, (long) object.getFormat());
                    break;
                case MtpConstants.PROPERTY_PROTECTION_STATUS /*56323*/:
                    mtpPropertyList.append(id, property.code, property.type, 0);
                    break;
                case MtpConstants.PROPERTY_OBJECT_SIZE /*56324*/:
                    boolean isAutomaticMode = MtpDatabase.issIsHeifSettingAutomaticMode();
                    if (object.getFormat() != 14354) {
                        isHeifFile = false;
                    }
                    boolean isHeifFile2 = isHeifFile;
                    if (!isAutomaticMode || !isHeifFile2) {
                        mtpPropertyList.append(id, property.code, property.type, object.getSize());
                        break;
                    }
                    mtpPropertyList.append(id, property.code, property.type, 3 * object.getSize());
                    break;
                    break;
                case MtpConstants.PROPERTY_OBJECT_FILE_NAME /*56327*/:
                case MtpConstants.PROPERTY_NAME /*56388*/:
                case MtpConstants.PROPERTY_DISPLAY_NAME /*56544*/:
                    c = object.getName();
                    Cursor filePath = c;
                    boolean isAutomaticMode2 = MtpDatabase.issIsHeifSettingAutomaticMode();
                    if (!(filePath.toUpperCase().endsWith(SUFFIX_HEIC_FILE) || filePath.toUpperCase().endsWith(SUFFIX_HEIF_FILE))) {
                        isHeifFile = false;
                    }
                    if (!isAutomaticMode2 || !isHeifFile) {
                        mtpPropertyList.append(id, property.code, c);
                        break;
                    }
                    suffixIndex = filePath.lastIndexOf(46);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(filePath.substring(0, suffixIndex));
                    stringBuilder.append(SUFFIX_JPG_FILE);
                    mtpPropertyList.append(id, property.code, stringBuilder.toString());
                    break;
                    break;
                case MtpConstants.PROPERTY_DATE_MODIFIED /*56329*/:
                case MtpConstants.PROPERTY_DATE_ADDED /*56398*/:
                    mtpPropertyList.append(id, property.code, format_date_time(object.getModifiedTime()));
                    break;
                case MtpConstants.PROPERTY_PARENT_OBJECT /*56331*/:
                    mtpPropertyList.append(id, property.code, property.type, object.getParent().isRoot() != null ? 0 : (long) object.getParent().getId());
                    break;
                case MtpConstants.PROPERTY_PERSISTENT_UID /*56385*/:
                    mtpPropertyList.append(id, property.code, property.type, ((long) (object.getPath().toString().hashCode() << 32)) + object.getModifiedTime());
                    break;
                case MtpConstants.PROPERTY_ARTIST /*56390*/:
                    mtpPropertyList.append(id, property.code, mtpPropertyGroup.queryAudio(path, "artist"));
                    break;
                case MtpConstants.PROPERTY_TRACK /*56459*/:
                    c = null;
                    if (c2 != null) {
                        c = c2.getInt(property.column);
                    }
                    suffixIndex = c;
                    mtpPropertyList.append(id, property.code, 4, (long) (suffixIndex % 1000));
                    break;
                case MtpConstants.PROPERTY_GENRE /*56460*/:
                    c = mtpPropertyGroup.queryGenre(path);
                    if (c == null) {
                        break;
                    }
                    mtpPropertyList.append(id, property.code, c);
                    break;
                case MtpConstants.PROPERTY_ORIGINAL_RELEASE_DATE /*56473*/:
                    c = null;
                    if (c2 != null) {
                        c = c2.getInt(property.column);
                    }
                    String dateTime = new StringBuilder();
                    dateTime.append(Integer.toString(c));
                    dateTime.append("0101T000000");
                    mtpPropertyList.append(id, property.code, dateTime.toString());
                    break;
                case MtpConstants.PROPERTY_ALBUM_NAME /*56474*/:
                    mtpPropertyList.append(id, property.code, mtpPropertyGroup.queryAudio(path, "album"));
                    break;
                case MtpConstants.PROPERTY_BITRATE_TYPE /*56978*/:
                case MtpConstants.PROPERTY_NUMBER_OF_CHANNELS /*56980*/:
                    mtpPropertyList.append(id, property.code, 4, 0);
                    break;
                case MtpConstants.PROPERTY_SAMPLE_RATE /*56979*/:
                case MtpConstants.PROPERTY_AUDIO_WAVE_CODEC /*56985*/:
                case MtpConstants.PROPERTY_AUDIO_BITRATE /*56986*/:
                    mtpPropertyList.append(id, property.code, 6, 0);
                    break;
                default:
                    int i2 = property.type;
                    if (i2 != 0) {
                        if (i2 == 65535) {
                            String value = "";
                            if (c2 != null) {
                                value = c2.getString(property.column);
                            }
                            mtpPropertyList.append(id, property.code, value);
                            break;
                        }
                        long longValue = 0;
                        if (c2 != null) {
                            longValue = c2.getLong(property.column);
                        }
                        mtpPropertyList.append(id, property.code, property.type, longValue);
                        break;
                    }
                    mtpPropertyList.append(id, property.code, property.type, 0);
                    break;
            }
            i++;
            c = c2;
            mtpPropertyGroup = this;
        }
        if (c != null) {
            c.close();
        }
        return MtpConstants.RESPONSE_OK;
    }
}
