package android.provider;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriPermission;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.hwgallerycache.HwGalleryCacheManager;
import android.media.MiniThumbFile;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.Contacts.GroupMembership;
import android.util.Log;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import libcore.io.IoUtils;

public final class MediaStore {
    public static final String ACTION_IMAGE_CAPTURE = "android.media.action.IMAGE_CAPTURE";
    public static final String ACTION_IMAGE_CAPTURE_SECURE = "android.media.action.IMAGE_CAPTURE_SECURE";
    public static final String ACTION_VIDEO_CAPTURE = "android.media.action.VIDEO_CAPTURE";
    public static final String AUTHORITY = "media";
    private static final String CONTENT_AUTHORITY_SLASH = "content://media/";
    public static final String EXTRA_DURATION_LIMIT = "android.intent.extra.durationLimit";
    public static final String EXTRA_FINISH_ON_COMPLETION = "android.intent.extra.finishOnCompletion";
    public static final String EXTRA_FULL_SCREEN = "android.intent.extra.fullScreen";
    public static final String EXTRA_MEDIA_ALBUM = "android.intent.extra.album";
    public static final String EXTRA_MEDIA_ARTIST = "android.intent.extra.artist";
    public static final String EXTRA_MEDIA_FOCUS = "android.intent.extra.focus";
    public static final String EXTRA_MEDIA_GENRE = "android.intent.extra.genre";
    public static final String EXTRA_MEDIA_PLAYLIST = "android.intent.extra.playlist";
    public static final String EXTRA_MEDIA_RADIO_CHANNEL = "android.intent.extra.radio_channel";
    public static final String EXTRA_MEDIA_TITLE = "android.intent.extra.title";
    public static final String EXTRA_OUTPUT = "output";
    public static final String EXTRA_SCREEN_ORIENTATION = "android.intent.extra.screenOrientation";
    public static final String EXTRA_SHOW_ACTION_ICONS = "android.intent.extra.showActionIcons";
    public static final String EXTRA_SIZE_LIMIT = "android.intent.extra.sizeLimit";
    public static final String EXTRA_VIDEO_QUALITY = "android.intent.extra.videoQuality";
    public static final String INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH = "android.media.action.MEDIA_PLAY_FROM_SEARCH";
    public static final String INTENT_ACTION_MEDIA_SEARCH = "android.intent.action.MEDIA_SEARCH";
    @Deprecated
    public static final String INTENT_ACTION_MUSIC_PLAYER = "android.intent.action.MUSIC_PLAYER";
    public static final String INTENT_ACTION_STILL_IMAGE_CAMERA = "android.media.action.STILL_IMAGE_CAMERA";
    public static final String INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE = "android.media.action.STILL_IMAGE_CAMERA_SECURE";
    public static final String INTENT_ACTION_TEXT_OPEN_FROM_SEARCH = "android.media.action.TEXT_OPEN_FROM_SEARCH";
    public static final String INTENT_ACTION_VIDEO_CAMERA = "android.media.action.VIDEO_CAMERA";
    public static final String INTENT_ACTION_VIDEO_PLAY_FROM_SEARCH = "android.media.action.VIDEO_PLAY_FROM_SEARCH";
    public static final String MEDIA_IGNORE_FILENAME = ".nomedia";
    public static final String MEDIA_SCANNER_VOLUME = "volume";
    public static final String META_DATA_STILL_IMAGE_CAMERA_PREWARM_SERVICE = "android.media.still_image_camera_preview_service";
    public static final String PARAM_DELETE_DATA = "deletedata";
    public static final String RETRANSLATE_CALL = "update_titles";
    private static final String TAG = "MediaStore";
    public static final String UNHIDE_CALL = "unhide";
    public static final String UNKNOWN_STRING = "<unknown>";

    public static final class Audio {

        public interface AlbumColumns {
            public static final String ALBUM = "album";
            public static final String ALBUM_ART = "album_art";
            public static final String ALBUM_ID = "album_id";
            public static final String ALBUM_KEY = "album_key";
            public static final String ARTIST = "artist";
            public static final String FIRST_YEAR = "minyear";
            public static final String LAST_YEAR = "maxyear";
            public static final String NUMBER_OF_SONGS = "numsongs";
            public static final String NUMBER_OF_SONGS_FOR_ARTIST = "numsongs_by_artist";
        }

        public interface ArtistColumns {
            public static final String ARTIST = "artist";
            public static final String ARTIST_KEY = "artist_key";
            public static final String NUMBER_OF_ALBUMS = "number_of_albums";
            public static final String NUMBER_OF_TRACKS = "number_of_tracks";
        }

        public interface GenresColumns {
            public static final String NAME = "name";
        }

        public interface PlaylistsColumns {
            public static final String DATA = "_data";
            public static final String DATE_ADDED = "date_added";
            public static final String DATE_MODIFIED = "date_modified";
            public static final String NAME = "name";
        }

        public static final class Radio {
            public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/radio";

            private Radio() {
            }
        }

        public static final class Albums implements BaseColumns, AlbumColumns {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/albums";
            public static final String DEFAULT_SORT_ORDER = "album_key";
            public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/album";
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");
            public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");

            public static Uri getContentUri(String volumeName) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(MediaStore.CONTENT_AUTHORITY_SLASH);
                stringBuilder.append(volumeName);
                stringBuilder.append("/audio/albums");
                return Uri.parse(stringBuilder.toString());
            }
        }

        public static final class Artists implements BaseColumns, ArtistColumns {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/artists";
            public static final String DEFAULT_SORT_ORDER = "artist_key";
            public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/artist";
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");
            public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");

            public static final class Albums implements AlbumColumns {
                public static final Uri getContentUri(String volumeName, long artistId) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(MediaStore.CONTENT_AUTHORITY_SLASH);
                    stringBuilder.append(volumeName);
                    stringBuilder.append("/audio/artists/");
                    stringBuilder.append(artistId);
                    stringBuilder.append("/albums");
                    return Uri.parse(stringBuilder.toString());
                }
            }

            public static Uri getContentUri(String volumeName) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(MediaStore.CONTENT_AUTHORITY_SLASH);
                stringBuilder.append(volumeName);
                stringBuilder.append("/audio/artists");
                return Uri.parse(stringBuilder.toString());
            }
        }

        public static final class Genres implements BaseColumns, GenresColumns {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/genre";
            public static final String DEFAULT_SORT_ORDER = "name";
            public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/genre";
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");
            public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");

            public static final class Members implements AudioColumns {
                public static final String AUDIO_ID = "audio_id";
                public static final String CONTENT_DIRECTORY = "members";
                public static final String DEFAULT_SORT_ORDER = "title_key";
                public static final String GENRE_ID = "genre_id";

                public static final Uri getContentUri(String volumeName, long genreId) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(MediaStore.CONTENT_AUTHORITY_SLASH);
                    stringBuilder.append(volumeName);
                    stringBuilder.append("/audio/genres/");
                    stringBuilder.append(genreId);
                    stringBuilder.append("/members");
                    return Uri.parse(stringBuilder.toString());
                }
            }

            public static Uri getContentUri(String volumeName) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(MediaStore.CONTENT_AUTHORITY_SLASH);
                stringBuilder.append(volumeName);
                stringBuilder.append("/audio/genres");
                return Uri.parse(stringBuilder.toString());
            }

            public static Uri getContentUriForAudioId(String volumeName, int audioId) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(MediaStore.CONTENT_AUTHORITY_SLASH);
                stringBuilder.append(volumeName);
                stringBuilder.append("/audio/media/");
                stringBuilder.append(audioId);
                stringBuilder.append("/genres");
                return Uri.parse(stringBuilder.toString());
            }
        }

        public static final class Playlists implements BaseColumns, PlaylistsColumns {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/playlist";
            public static final String DEFAULT_SORT_ORDER = "name";
            public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/playlist";
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");
            public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");

            public static final class Members implements AudioColumns {
                public static final String AUDIO_ID = "audio_id";
                public static final String CONTENT_DIRECTORY = "members";
                public static final String DEFAULT_SORT_ORDER = "play_order";
                public static final String PLAYLIST_ID = "playlist_id";
                public static final String PLAY_ORDER = "play_order";
                public static final String _ID = "_id";

                public static final Uri getContentUri(String volumeName, long playlistId) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(MediaStore.CONTENT_AUTHORITY_SLASH);
                    stringBuilder.append(volumeName);
                    stringBuilder.append("/audio/playlists/");
                    stringBuilder.append(playlistId);
                    stringBuilder.append("/members");
                    return Uri.parse(stringBuilder.toString());
                }

                public static final boolean moveItem(ContentResolver res, long playlistId, int from, int to) {
                    Uri uri = getContentUri("external", playlistId).buildUpon().appendEncodedPath(String.valueOf(from)).appendQueryParameter("move", "true").build();
                    ContentValues values = new ContentValues();
                    values.put("play_order", Integer.valueOf(to));
                    return res.update(uri, values, null, null) != 0;
                }
            }

            public static Uri getContentUri(String volumeName) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(MediaStore.CONTENT_AUTHORITY_SLASH);
                stringBuilder.append(volumeName);
                stringBuilder.append("/audio/playlists");
                return Uri.parse(stringBuilder.toString());
            }
        }

        public interface AudioColumns extends MediaColumns {
            public static final String ALBUM = "album";
            public static final String ALBUM_ARTIST = "album_artist";
            public static final String ALBUM_ID = "album_id";
            public static final String ALBUM_KEY = "album_key";
            public static final String ARTIST = "artist";
            public static final String ARTIST_ID = "artist_id";
            public static final String ARTIST_KEY = "artist_key";
            public static final String BOOKMARK = "bookmark";
            public static final String COMPILATION = "compilation";
            public static final String COMPOSER = "composer";
            public static final String DURATION = "duration";
            public static final String GENRE = "genre";
            public static final String IS_ALARM = "is_alarm";
            public static final String IS_MUSIC = "is_music";
            public static final String IS_NOTIFICATION = "is_notification";
            public static final String IS_PODCAST = "is_podcast";
            public static final String IS_RINGTONE = "is_ringtone";
            public static final String TITLE_KEY = "title_key";
            public static final String TITLE_RESOURCE_URI = "title_resource_uri";
            public static final String TRACK = "track";
            public static final String YEAR = "year";
        }

        public static final class Media implements AudioColumns {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/audio";
            public static final String DEFAULT_SORT_ORDER = "title_key";
            public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/audio";
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");
            private static final String[] EXTERNAL_PATHS;
            public static final String EXTRA_MAX_BYTES = "android.provider.MediaStore.extra.MAX_BYTES";
            public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");
            public static final String RECORD_SOUND_ACTION = "android.provider.MediaStore.RECORD_SOUND";

            static {
                String secondary_storage = System.getenv("SECONDARY_STORAGE");
                if (secondary_storage != null) {
                    EXTERNAL_PATHS = secondary_storage.split(SettingsStringUtil.DELIMITER);
                } else {
                    EXTERNAL_PATHS = new String[0];
                }
            }

            public static Uri getContentUri(String volumeName) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(MediaStore.CONTENT_AUTHORITY_SLASH);
                stringBuilder.append(volumeName);
                stringBuilder.append("/audio/media");
                return Uri.parse(stringBuilder.toString());
            }

            public static Uri getContentUriForPath(String path) {
                for (String ep : EXTERNAL_PATHS) {
                    if (path.startsWith(ep)) {
                        return EXTERNAL_CONTENT_URI;
                    }
                }
                return path.startsWith(Environment.getExternalStorageDirectory().getPath()) ? EXTERNAL_CONTENT_URI : INTERNAL_CONTENT_URI;
            }
        }

        public static String keyFor(String name) {
            if (name == null) {
                return null;
            }
            boolean sortfirst = false;
            if (name.equals(MediaStore.UNKNOWN_STRING)) {
                return "\u0001";
            }
            if (name.startsWith("\u0001")) {
                sortfirst = true;
            }
            name = name.trim().toLowerCase();
            if (name.startsWith("the ")) {
                name = name.substring(4);
            }
            if (name.startsWith("an ")) {
                name = name.substring(3);
            }
            if (name.startsWith("a ")) {
                name = name.substring(2);
            }
            int i = 0;
            if (name.endsWith(", the") || name.endsWith(",the") || name.endsWith(", an") || name.endsWith(",an") || name.endsWith(", a") || name.endsWith(",a")) {
                name = name.substring(0, name.lastIndexOf(44));
            }
            name = name.replaceAll("[\\[\\]\\(\\)\"'.,?!]", "").trim();
            if (name.length() <= 0) {
                return "";
            }
            StringBuilder b = new StringBuilder();
            b.append('.');
            int nl = name.length();
            while (i < nl) {
                b.append(name.charAt(i));
                b.append('.');
                i++;
            }
            String key = DatabaseUtils.getCollationKey(b.toString());
            if (sortfirst) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("\u0001");
                stringBuilder.append(key);
                key = stringBuilder.toString();
            }
            return key;
        }
    }

    public static final class Files {

        public interface FileColumns extends MediaColumns {
            public static final String FORMAT = "format";
            public static final String MEDIA_TYPE = "media_type";
            public static final int MEDIA_TYPE_AUDIO = 2;
            public static final int MEDIA_TYPE_IMAGE = 1;
            public static final int MEDIA_TYPE_NONE = 0;
            public static final int MEDIA_TYPE_PLAYLIST = 4;
            public static final int MEDIA_TYPE_VIDEO = 3;
            public static final String MIME_TYPE = "mime_type";
            public static final String PARENT = "parent";
            public static final String STORAGE_ID = "storage_id";
            public static final String TITLE = "title";
        }

        public static Uri getContentUri(String volumeName) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(MediaStore.CONTENT_AUTHORITY_SLASH);
            stringBuilder.append(volumeName);
            stringBuilder.append("/file");
            return Uri.parse(stringBuilder.toString());
        }

        public static final Uri getContentUri(String volumeName, long rowId) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(MediaStore.CONTENT_AUTHORITY_SLASH);
            stringBuilder.append(volumeName);
            stringBuilder.append("/file/");
            stringBuilder.append(rowId);
            return Uri.parse(stringBuilder.toString());
        }

        public static Uri getMtpObjectsUri(String volumeName) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(MediaStore.CONTENT_AUTHORITY_SLASH);
            stringBuilder.append(volumeName);
            stringBuilder.append("/object");
            return Uri.parse(stringBuilder.toString());
        }

        public static final Uri getMtpObjectsUri(String volumeName, long fileId) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(MediaStore.CONTENT_AUTHORITY_SLASH);
            stringBuilder.append(volumeName);
            stringBuilder.append("/object/");
            stringBuilder.append(fileId);
            return Uri.parse(stringBuilder.toString());
        }

        public static final Uri getMtpReferencesUri(String volumeName, long fileId) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(MediaStore.CONTENT_AUTHORITY_SLASH);
            stringBuilder.append(volumeName);
            stringBuilder.append("/object/");
            stringBuilder.append(fileId);
            stringBuilder.append("/references");
            return Uri.parse(stringBuilder.toString());
        }

        public static final Uri getDirectoryUri(String volumeName) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(MediaStore.CONTENT_AUTHORITY_SLASH);
            stringBuilder.append(volumeName);
            stringBuilder.append("/dir");
            return Uri.parse(stringBuilder.toString());
        }
    }

    public static final class Images {

        public static class Thumbnails implements BaseColumns {
            public static final String DATA = "_data";
            public static final String DEFAULT_SORT_ORDER = "image_id ASC";
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");
            public static final int FULL_SCREEN_KIND = 2;
            public static final String HEIGHT = "height";
            public static final String IMAGE_ID = "image_id";
            public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");
            public static final String KIND = "kind";
            public static final int MICRO_KIND = 3;
            public static final int MINI_KIND = 1;
            public static final String THUMB_DATA = "thumb_data";
            public static final String WIDTH = "width";

            public static final Cursor query(ContentResolver cr, Uri uri, String[] projection) {
                return cr.query(uri, projection, null, null, DEFAULT_SORT_ORDER);
            }

            public static final Cursor queryMiniThumbnails(ContentResolver cr, Uri uri, int kind, String[] projection) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("kind = ");
                stringBuilder.append(kind);
                return cr.query(uri, projection, stringBuilder.toString(), null, DEFAULT_SORT_ORDER);
            }

            public static final Cursor queryMiniThumbnail(ContentResolver cr, long origId, int kind, String[] projection) {
                Uri uri = EXTERNAL_CONTENT_URI;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("image_id = ");
                stringBuilder.append(origId);
                stringBuilder.append(" AND ");
                stringBuilder.append("kind");
                stringBuilder.append(" = ");
                stringBuilder.append(kind);
                return cr.query(uri, projection, stringBuilder.toString(), null, null);
            }

            public static void cancelThumbnailRequest(ContentResolver cr, long origId) {
                InternalThumbnails.cancelThumbnailRequest(cr, origId, EXTERNAL_CONTENT_URI, 0);
            }

            public static Bitmap getThumbnail(ContentResolver cr, long origId, int kind, Options options) {
                return InternalThumbnails.getThumbnail(cr, origId, 0, kind, options, EXTERNAL_CONTENT_URI, false);
            }

            public static void cancelThumbnailRequest(ContentResolver cr, long origId, long groupId) {
                InternalThumbnails.cancelThumbnailRequest(cr, origId, EXTERNAL_CONTENT_URI, groupId);
            }

            public static Bitmap getThumbnail(ContentResolver cr, long origId, long groupId, int kind, Options options) {
                return InternalThumbnails.getThumbnail(cr, origId, groupId, kind, options, EXTERNAL_CONTENT_URI, false);
            }

            public static Uri getContentUri(String volumeName) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(MediaStore.CONTENT_AUTHORITY_SLASH);
                stringBuilder.append(volumeName);
                stringBuilder.append("/images/thumbnails");
                return Uri.parse(stringBuilder.toString());
            }
        }

        public interface ImageColumns extends MediaColumns {
            public static final String BUCKET_DISPLAY_NAME = "bucket_display_name";
            public static final String BUCKET_ID = "bucket_id";
            public static final String DATE_TAKEN = "datetaken";
            public static final String DESCRIPTION = "description";
            public static final String IS_HDR = "is_hdr";
            public static final String IS_PRIVATE = "isprivate";
            public static final String LATITUDE = "latitude";
            public static final String LONGITUDE = "longitude";
            public static final String MINI_THUMB_MAGIC = "mini_thumb_magic";
            public static final String ORIENTATION = "orientation";
            public static final String PICASA_ID = "picasa_id";
        }

        public static final class Media implements ImageColumns {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/image";
            public static final String DEFAULT_SORT_ORDER = "bucket_display_name";
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");
            public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");

            public static final Cursor query(ContentResolver cr, Uri uri, String[] projection) {
                return cr.query(uri, projection, null, null, "bucket_display_name");
            }

            public static final Cursor query(ContentResolver cr, Uri uri, String[] projection, String where, String orderBy) {
                String str;
                if (orderBy == null) {
                    str = "bucket_display_name";
                } else {
                    str = orderBy;
                }
                return cr.query(uri, projection, where, null, str);
            }

            public static final Cursor query(ContentResolver cr, Uri uri, String[] projection, String selection, String[] selectionArgs, String orderBy) {
                String str;
                if (orderBy == null) {
                    str = "bucket_display_name";
                } else {
                    str = orderBy;
                }
                return cr.query(uri, projection, selection, selectionArgs, str);
            }

            public static final Bitmap getBitmap(ContentResolver cr, Uri url) throws FileNotFoundException, IOException {
                InputStream input = cr.openInputStream(url);
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();
                return bitmap;
            }

            public static final String insertImage(ContentResolver cr, String imagePath, String name, String description) throws FileNotFoundException {
                FileInputStream stream = new FileInputStream(imagePath);
                String ret;
                try {
                    Bitmap bm = BitmapFactory.decodeFile(imagePath);
                    ret = insertImage(cr, bm, name, description);
                    if (bm != null) {
                        bm.recycle();
                    }
                    return ret;
                } finally {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        ret = e;
                    }
                }
            }

            private static final Bitmap StoreThumbnail(ContentResolver cr, Bitmap source, long id, float width, float height, int kind) {
                ContentResolver contentResolver = cr;
                Matrix matrix = new Matrix();
                matrix.setScale(width / ((float) source.getWidth()), height / ((float) source.getHeight()));
                Bitmap thumb = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
                ContentValues values = new ContentValues(4);
                values.put("kind", Integer.valueOf(kind));
                values.put("image_id", Integer.valueOf((int) id));
                values.put("height", Integer.valueOf(thumb.getHeight()));
                values.put("width", Integer.valueOf(thumb.getWidth()));
                try {
                    OutputStream thumbOut = contentResolver.openOutputStream(contentResolver.insert(Thumbnails.EXTERNAL_CONTENT_URI, values));
                    thumb.compress(CompressFormat.JPEG, 100, thumbOut);
                    thumbOut.close();
                    return thumb;
                } catch (FileNotFoundException e) {
                    return null;
                } catch (IOException e2) {
                    return null;
                }
            }

            /* JADX WARNING: Removed duplicated region for block: B:27:? A:{SYNTHETIC, RETURN} */
            /* JADX WARNING: Removed duplicated region for block: B:25:0x0082  */
            /* JADX WARNING: Removed duplicated region for block: B:23:0x007b  */
            /* JADX WARNING: Removed duplicated region for block: B:25:0x0082  */
            /* JADX WARNING: Removed duplicated region for block: B:27:? A:{SYNTHETIC, RETURN} */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public static final String insertImage(ContentResolver cr, Bitmap source, String title, String description) {
                Uri url;
                Exception e;
                ContentResolver contentResolver = cr;
                Bitmap bitmap = source;
                ContentValues values = new ContentValues();
                values.put("title", title);
                values.put("description", description);
                values.put("mime_type", "image/jpeg");
                String stringUrl = null;
                try {
                    url = contentResolver.insert(EXTERNAL_CONTENT_URI, values);
                    if (bitmap != null) {
                        OutputStream imageOut;
                        try {
                            imageOut = contentResolver.openOutputStream(url);
                            bitmap.compress(CompressFormat.JPEG, 50, imageOut);
                            imageOut.close();
                            long id = ContentUris.parseId(url);
                            StoreThumbnail(contentResolver, Thumbnails.getThumbnail(contentResolver, id, 1, null), id, 50.0f, 50.0f, 3);
                        } catch (Exception e2) {
                            e = e2;
                            Log.e(MediaStore.TAG, "Failed to insert image", e);
                            if (url != null) {
                            }
                            if (url != null) {
                            }
                        } catch (Throwable th) {
                            imageOut.close();
                        }
                    } else {
                        Log.e(MediaStore.TAG, "Failed to create thumbnail, removing original");
                        contentResolver.delete(url, null, null);
                        url = null;
                    }
                } catch (Exception e3) {
                    e = e3;
                    url = null;
                    Log.e(MediaStore.TAG, "Failed to insert image", e);
                    if (url != null) {
                        contentResolver.delete(url, null, null);
                        url = null;
                    }
                    if (url != null) {
                    }
                }
                if (url != null) {
                    return url.toString();
                }
                return stringUrl;
            }

            public static Uri getContentUri(String volumeName) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(MediaStore.CONTENT_AUTHORITY_SLASH);
                stringBuilder.append(volumeName);
                stringBuilder.append("/images/media");
                return Uri.parse(stringBuilder.toString());
            }
        }
    }

    public static final class Video {
        public static final String DEFAULT_SORT_ORDER = "_display_name";

        public static class Thumbnails implements BaseColumns {
            public static final String DATA = "_data";
            public static final String DEFAULT_SORT_ORDER = "video_id ASC";
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");
            public static final int FULL_SCREEN_KIND = 2;
            public static final String HEIGHT = "height";
            public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");
            public static final String KIND = "kind";
            public static final int MICRO_KIND = 3;
            public static final int MINI_KIND = 1;
            public static final String VIDEO_ID = "video_id";
            public static final String WIDTH = "width";

            public static void cancelThumbnailRequest(ContentResolver cr, long origId) {
                InternalThumbnails.cancelThumbnailRequest(cr, origId, EXTERNAL_CONTENT_URI, 0);
            }

            public static Bitmap getThumbnail(ContentResolver cr, long origId, int kind, Options options) {
                return InternalThumbnails.getThumbnail(cr, origId, 0, kind, options, EXTERNAL_CONTENT_URI, true);
            }

            public static Bitmap getThumbnail(ContentResolver cr, long origId, long groupId, int kind, Options options) {
                return InternalThumbnails.getThumbnail(cr, origId, groupId, kind, options, EXTERNAL_CONTENT_URI, true);
            }

            public static void cancelThumbnailRequest(ContentResolver cr, long origId, long groupId) {
                InternalThumbnails.cancelThumbnailRequest(cr, origId, EXTERNAL_CONTENT_URI, groupId);
            }

            public static Uri getContentUri(String volumeName) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(MediaStore.CONTENT_AUTHORITY_SLASH);
                stringBuilder.append(volumeName);
                stringBuilder.append("/video/thumbnails");
                return Uri.parse(stringBuilder.toString());
            }
        }

        public interface VideoColumns extends MediaColumns {
            public static final String ALBUM = "album";
            public static final String ARTIST = "artist";
            public static final String BOOKMARK = "bookmark";
            public static final String BUCKET_DISPLAY_NAME = "bucket_display_name";
            public static final String BUCKET_ID = "bucket_id";
            public static final String CATEGORY = "category";
            public static final String DATE_TAKEN = "datetaken";
            public static final String DESCRIPTION = "description";
            public static final String DURATION = "duration";
            public static final String IS_PRIVATE = "isprivate";
            public static final String LANGUAGE = "language";
            public static final String LATITUDE = "latitude";
            public static final String LONGITUDE = "longitude";
            public static final String MINI_THUMB_MAGIC = "mini_thumb_magic";
            public static final String RESOLUTION = "resolution";
            public static final String TAGS = "tags";
        }

        public static final class Media implements VideoColumns {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/video";
            public static final String DEFAULT_SORT_ORDER = "title";
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");
            public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");

            public static Uri getContentUri(String volumeName) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(MediaStore.CONTENT_AUTHORITY_SLASH);
                stringBuilder.append(volumeName);
                stringBuilder.append("/video/media");
                return Uri.parse(stringBuilder.toString());
            }
        }

        public static final Cursor query(ContentResolver cr, Uri uri, String[] projection) {
            return cr.query(uri, projection, null, null, "_display_name");
        }
    }

    private static class InternalThumbnails implements BaseColumns {
        static final int DEFAULT_GROUP_ID = 0;
        private static final int FULL_SCREEN_KIND = 2;
        private static final int MICRO_KIND = 3;
        private static final int MINI_KIND = 1;
        private static final String[] PROJECTION = new String[]{"_id", "_data"};
        private static byte[] sThumbBuf;
        private static final Object sThumbBufLock = new Object();

        private InternalThumbnails() {
        }

        private static Bitmap getMiniThumbFromFile(Cursor c, Uri baseUri, ContentResolver cr, Options options) {
            String str;
            StringBuilder stringBuilder;
            Bitmap bitmap = null;
            Uri thumbUri = null;
            try {
                long thumbId = c.getLong(0);
                String filePath = c.getString(1);
                thumbUri = ContentUris.withAppendedId(baseUri, thumbId);
                ParcelFileDescriptor pfdInput = cr.openFileDescriptor(thumbUri, "r");
                bitmap = BitmapFactory.decodeFileDescriptor(pfdInput.getFileDescriptor(), null, options);
                pfdInput.close();
                return bitmap;
            } catch (FileNotFoundException ex) {
                str = MediaStore.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("couldn't open thumbnail ");
                stringBuilder.append(thumbUri);
                stringBuilder.append("; ");
                stringBuilder.append(ex);
                Log.e(str, stringBuilder.toString());
                return bitmap;
            } catch (IOException ex2) {
                str = MediaStore.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("couldn't open thumbnail ");
                stringBuilder.append(thumbUri);
                stringBuilder.append("; ");
                stringBuilder.append(ex2);
                Log.e(str, stringBuilder.toString());
                return bitmap;
            } catch (OutOfMemoryError ex3) {
                str = MediaStore.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("failed to allocate memory for thumbnail ");
                stringBuilder.append(thumbUri);
                stringBuilder.append("; ");
                stringBuilder.append(ex3);
                Log.e(str, stringBuilder.toString());
                return bitmap;
            }
        }

        static void cancelThumbnailRequest(ContentResolver cr, long origId, Uri baseUri, long groupId) {
            Cursor cursor = null;
            Cursor c = null;
            try {
                cursor = cr.query(baseUri.buildUpon().appendQueryParameter("cancel", "1").appendQueryParameter("orig_id", String.valueOf(origId)).appendQueryParameter(GroupMembership.GROUP_ID, String.valueOf(groupId)).build(), PROJECTION, null, null, null);
            } finally {
                c = 
/*
Method generation error in method: android.provider.MediaStore.InternalThumbnails.cancelThumbnailRequest(android.content.ContentResolver, long, android.net.Uri, long):void, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: ?: MERGE  (r7_1 'c' android.database.Cursor) = (r7_0 'c' android.database.Cursor), (r1_7 'cursor' android.database.Cursor) in method: android.provider.MediaStore.InternalThumbnails.cancelThumbnailRequest(android.content.ContentResolver, long, android.net.Uri, long):void, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:205)
	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:102)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:52)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeTryCatch(RegionGen.java:300)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:65)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:183)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:321)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:259)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:221)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.addInnerClasses(ClassGen.java:234)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:220)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:77)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:10)
	at jadx.core.ProcessClass.process(ProcessClass.java:38)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.CodegenException: MERGE can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:222)
	... 24 more

*/

        /* JADX WARNING: Removed duplicated region for block: B:109:0x01a4 A:{SYNTHETIC, Splitter:B:109:0x01a4} */
        /* JADX WARNING: Removed duplicated region for block: B:126:0x01c8  */
        /* JADX WARNING: Removed duplicated region for block: B:122:0x01be  */
        /* JADX WARNING: Removed duplicated region for block: B:230:0x0347  */
        /* JADX WARNING: Removed duplicated region for block: B:224:0x0339  */
        /* JADX WARNING: Removed duplicated region for block: B:230:0x0347  */
        /* JADX WARNING: Removed duplicated region for block: B:224:0x0339  */
        /* JADX WARNING: Removed duplicated region for block: B:230:0x0347  */
        /* JADX WARNING: Removed duplicated region for block: B:230:0x0347  */
        /* JADX WARNING: Removed duplicated region for block: B:224:0x0339  */
        /* JADX WARNING: Removed duplicated region for block: B:230:0x0347  */
        /* JADX WARNING: Removed duplicated region for block: B:224:0x0339  */
        /* JADX WARNING: Removed duplicated region for block: B:230:0x0347  */
        /* JADX WARNING: Removed duplicated region for block: B:230:0x0347  */
        /* JADX WARNING: Removed duplicated region for block: B:224:0x0339  */
        /* JADX WARNING: Removed duplicated region for block: B:230:0x0347  */
        /* JADX WARNING: Removed duplicated region for block: B:33:0x009d  */
        /* JADX WARNING: Removed duplicated region for block: B:224:0x0339  */
        /* JADX WARNING: Removed duplicated region for block: B:230:0x0347  */
        /* JADX WARNING: Removed duplicated region for block: B:224:0x0339  */
        /* JADX WARNING: Removed duplicated region for block: B:230:0x0347  */
        /* JADX WARNING: Removed duplicated region for block: B:224:0x0339  */
        /* JADX WARNING: Removed duplicated region for block: B:230:0x0347  */
        /* JADX WARNING: Removed duplicated region for block: B:224:0x0339  */
        /* JADX WARNING: Removed duplicated region for block: B:230:0x0347  */
        /* JADX WARNING: Removed duplicated region for block: B:224:0x0339  */
        /* JADX WARNING: Removed duplicated region for block: B:230:0x0347  */
        /* JADX WARNING: Missing block: B:21:0x007a, code skipped:
            if (r1 != null) goto L_0x007c;
     */
        /* JADX WARNING: Missing block: B:22:0x007c, code skipped:
            r1.close();
     */
        /* JADX WARNING: Missing block: B:29:0x008f, code skipped:
            if (r1 != null) goto L_0x007c;
     */
        /* JADX WARNING: Missing block: B:30:0x0092, code skipped:
            android.util.Log.w(android.provider.MediaStore.TAG, "getThumbnail for video from kvdb faild!");
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        static Bitmap getThumbnail(ContentResolver cr, long origId, long groupId, int kind, Options options, Uri baseUri, boolean isVideo) {
            Cursor c;
            Throwable th;
            MiniThumbFile thumbFile;
            Cursor cursor;
            Bitmap bitmap;
            Object c2;
            Cursor cursor2;
            ContentResolver contentResolver = cr;
            long j = origId;
            int i = kind;
            Options options2 = options;
            Uri uri = baseUri;
            boolean z = isVideo;
            Bitmap bitmap2 = null;
            if (HwGalleryCacheManager.isGalleryCacheEffect() && 1 == i && z && j <= 2147483647L) {
                Cursor c3 = null;
                try {
                    c = contentResolver.query(Uri.parse(baseUri.buildUpon().appendPath(String.valueOf(origId)).toString().replaceFirst("thumbnails", MediaStore.AUTHORITY)), new String[]{"_id", "_data", "date_modified"}, null, null, null);
                    if (c != null) {
                        try {
                            if (c.moveToFirst()) {
                                bitmap2 = HwGalleryCacheManager.getGalleryCachedVideo((int) j, c.getLong(c.getColumnIndexOrThrow("date_modified")), options2);
                                if (bitmap2 != null) {
                                    if (c != null) {
                                        c.close();
                                    }
                                    return bitmap2;
                                }
                            }
                        } catch (SQLiteException e) {
                            try {
                                Log.w(MediaStore.TAG, "sqlite exception");
                            } catch (Throwable th2) {
                                th = th2;
                                if (c != null) {
                                }
                                throw th;
                            }
                        }
                    }
                } catch (SQLiteException e2) {
                    c = c3;
                    Log.w(MediaStore.TAG, "sqlite exception");
                } catch (Throwable th3) {
                    th = th3;
                    c = c3;
                    if (c != null) {
                        c.close();
                    }
                    throw th;
                }
            }
            MiniThumbFile thumbFile2 = MiniThumbFile.instance(z ? Media.EXTERNAL_CONTENT_URI : Media.EXTERNAL_CONTENT_URI);
            Cursor c4 = null;
            try {
                int i2;
                Uri blockingUri;
                if (thumbFile2.getMagic(j) != 0) {
                    if (i == 3) {
                        try {
                            synchronized (sThumbBufLock) {
                                if (sThumbBuf == null) {
                                    sThumbBuf = new byte[10000];
                                }
                                if (thumbFile2.getMiniThumbFromFile(j, sThumbBuf) != null) {
                                    bitmap2 = BitmapFactory.decodeByteArray(sThumbBuf, 0, sThumbBuf.length);
                                    if (bitmap2 == null) {
                                        Log.w(MediaStore.TAG, "couldn't decode byte array.");
                                    }
                                }
                            }
                            if (c4 != null) {
                                c4.close();
                            }
                            thumbFile2.deactivate();
                            return bitmap2;
                        } catch (SQLiteException e3) {
                            th = e3;
                            thumbFile = thumbFile2;
                            try {
                                Log.w(MediaStore.TAG, th);
                                if (c4 != null) {
                                }
                                thumbFile.deactivate();
                                return bitmap2;
                            } catch (Throwable th4) {
                                th = th4;
                                if (c4 != null) {
                                    c4.close();
                                }
                                thumbFile.deactivate();
                                throw th;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            thumbFile = thumbFile2;
                            if (c4 != null) {
                            }
                            thumbFile.deactivate();
                            throw th;
                        }
                    } else if (i == 1) {
                        String column;
                        if (z) {
                            column = "video_id=";
                        } else {
                            try {
                                column = "image_id=";
                            } catch (SQLiteException e4) {
                                th = e4;
                                cursor = c4;
                                bitmap = bitmap2;
                                thumbFile = thumbFile2;
                                Log.w(MediaStore.TAG, th);
                                if (c4 != null) {
                                }
                                thumbFile.deactivate();
                                return bitmap2;
                            } catch (Throwable th6) {
                                th = th6;
                                cursor = c4;
                                bitmap = bitmap2;
                                thumbFile = thumbFile2;
                                if (c4 != null) {
                                }
                                thumbFile.deactivate();
                                throw th;
                            }
                        }
                        String[] strArr = PROJECTION;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(column);
                        stringBuilder.append(j);
                        i2 = 0;
                        i2 = 3;
                        String[] strArr2 = strArr;
                        cursor = c4;
                        bitmap = bitmap2;
                        bitmap2 = thumbFile2;
                        try {
                            c4 = contentResolver.query(uri, strArr2, stringBuilder.toString(), null, null);
                            if (c4 != null) {
                                try {
                                    if (c4.moveToFirst()) {
                                        Bitmap bitmap3 = getMiniThumbFromFile(c4, uri, contentResolver, options2);
                                        if (bitmap3 != null) {
                                            if (c4 != null) {
                                                c4.close();
                                            }
                                            bitmap2.deactivate();
                                            return bitmap3;
                                        }
                                        bitmap = bitmap3;
                                    }
                                } catch (SQLiteException e5) {
                                    th = e5;
                                    thumbFile = bitmap2;
                                    bitmap2 = bitmap;
                                    Log.w(MediaStore.TAG, th);
                                    if (c4 != null) {
                                    }
                                    thumbFile.deactivate();
                                    return bitmap2;
                                } catch (Throwable th7) {
                                    th = th7;
                                    thumbFile = bitmap2;
                                    if (c4 != null) {
                                    }
                                    thumbFile.deactivate();
                                    throw th;
                                }
                            }
                            thumbFile2 = c4;
                            blockingUri = baseUri.buildUpon().appendQueryParameter("blocking", "1").appendQueryParameter("orig_id", String.valueOf(origId)).appendQueryParameter(GroupMembership.GROUP_ID, String.valueOf(groupId)).build();
                            if (thumbFile2 != null) {
                                try {
                                    thumbFile2.close();
                                } catch (SQLiteException e6) {
                                    th = e6;
                                    c2 = thumbFile2;
                                } catch (Throwable th8) {
                                    th = th8;
                                    c2 = thumbFile2;
                                    thumbFile = bitmap2;
                                    if (c4 != null) {
                                    }
                                    thumbFile.deactivate();
                                    throw th;
                                }
                            }
                            cursor = thumbFile2;
                            c = contentResolver.query(blockingUri, PROJECTION, null, null, null);
                            if (c != null) {
                                if (c != null) {
                                    c.close();
                                }
                                bitmap2.deactivate();
                                return null;
                            }
                            Cursor cursor3;
                            StringBuilder stringBuilder2;
                            if (i == i2) {
                                try {
                                    synchronized (sThumbBufLock) {
                                        if (sThumbBuf == null) {
                                            sThumbBuf = new byte[10000];
                                        }
                                        Arrays.fill(sThumbBuf, (byte) 0);
                                        if (bitmap2.getMiniThumbFromFile(j, sThumbBuf) != null) {
                                            bitmap = BitmapFactory.decodeByteArray(sThumbBuf, 0, sThumbBuf.length);
                                            if (bitmap == null) {
                                                Log.w(MediaStore.TAG, "couldn't decode byte array.");
                                            }
                                        }
                                    }
                                    cursor3 = true;
                                } catch (SQLiteException e7) {
                                    th = e7;
                                    c4 = c;
                                } catch (Throwable th9) {
                                    th = th9;
                                    c4 = c;
                                    thumbFile = bitmap2;
                                    if (c4 != null) {
                                    }
                                    thumbFile.deactivate();
                                    throw th;
                                }
                            }
                            cursor3 = true;
                            if (i == 1) {
                                try {
                                    if (c.moveToFirst()) {
                                        bitmap = getMiniThumbFromFile(c, uri, contentResolver, options2);
                                    }
                                } catch (SQLiteException e8) {
                                    th = e8;
                                    cursor2 = c;
                                    thumbFile = bitmap2;
                                    bitmap2 = bitmap;
                                    c4 = cursor2;
                                    Log.w(MediaStore.TAG, th);
                                    if (c4 != null) {
                                    }
                                    thumbFile.deactivate();
                                    return bitmap2;
                                } catch (Throwable th10) {
                                    th = th10;
                                    cursor2 = c;
                                    thumbFile = bitmap2;
                                    c4 = cursor2;
                                    if (c4 != null) {
                                    }
                                    thumbFile.deactivate();
                                    throw th;
                                }
                            }
                            cursor2 = c;
                            thumbFile = bitmap2;
                            try {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Unsupported kind: ");
                                stringBuilder2.append(i);
                                throw new IllegalArgumentException(stringBuilder2.toString());
                            } catch (SQLiteException e9) {
                                th = e9;
                                bitmap2 = bitmap;
                                c4 = cursor2;
                                Log.w(MediaStore.TAG, th);
                                if (c4 != null) {
                                }
                                thumbFile.deactivate();
                                return bitmap2;
                            } catch (Throwable th11) {
                                th = th11;
                                c4 = cursor2;
                                if (c4 != null) {
                                }
                                thumbFile.deactivate();
                                throw th;
                            }
                            if (bitmap == null) {
                                column = MediaStore.TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Create the thumbnail in memory: origId=");
                                stringBuilder2.append(j);
                                stringBuilder2.append(", kind=");
                                stringBuilder2.append(i);
                                stringBuilder2.append(", isVideo=");
                                stringBuilder2.append(z);
                                Log.v(column, stringBuilder2.toString());
                                Uri uri2 = Uri.parse(baseUri.buildUpon().appendPath(String.valueOf(origId)).toString().replaceFirst("thumbnails", MediaStore.AUTHORITY));
                                if (c != null) {
                                    c.close();
                                }
                                Cursor thumbFile3 = bitmap2;
                                cursor2 = c;
                                c = cursor3;
                                try {
                                    c4 = contentResolver.query(uri2, PROJECTION, null, null, null);
                                    if (c4 != null) {
                                        try {
                                            if (c4.moveToFirst()) {
                                                column = c4.getString(c);
                                                if (column != null) {
                                                    if (z) {
                                                        bitmap = ThumbnailUtils.createVideoThumbnail(column, i);
                                                    } else {
                                                        bitmap = ThumbnailUtils.createImageThumbnail(column, i);
                                                    }
                                                }
                                                c = thumbFile3;
                                            }
                                        } catch (SQLiteException e10) {
                                            th = e10;
                                            bitmap2 = bitmap;
                                            thumbFile = thumbFile3;
                                            Log.w(MediaStore.TAG, th);
                                            if (c4 != null) {
                                            }
                                            thumbFile.deactivate();
                                            return bitmap2;
                                        } catch (Throwable th12) {
                                            th = th12;
                                            thumbFile = thumbFile3;
                                            if (c4 != null) {
                                            }
                                            thumbFile.deactivate();
                                            throw th;
                                        }
                                    }
                                    if (c4 != null) {
                                        c4.close();
                                    }
                                    thumbFile3.deactivate();
                                    return null;
                                } catch (SQLiteException e11) {
                                    th = e11;
                                    thumbFile = thumbFile3;
                                    bitmap2 = bitmap;
                                    c4 = cursor2;
                                    Log.w(MediaStore.TAG, th);
                                    if (c4 != null) {
                                    }
                                    thumbFile.deactivate();
                                    return bitmap2;
                                } catch (Throwable th13) {
                                    th = th13;
                                    thumbFile = thumbFile3;
                                    c4 = cursor2;
                                    if (c4 != null) {
                                    }
                                    thumbFile.deactivate();
                                    throw th;
                                }
                            }
                            cursor2 = c;
                            c = bitmap2;
                            c4 = cursor2;
                            if (c4 != null) {
                                c4.close();
                            }
                            c.deactivate();
                            bitmap2 = bitmap;
                            return bitmap2;
                        } catch (SQLiteException e12) {
                            th = e12;
                            thumbFile = bitmap2;
                            c4 = cursor;
                            bitmap2 = bitmap;
                            Log.w(MediaStore.TAG, th);
                            if (c4 != null) {
                            }
                            thumbFile.deactivate();
                            return bitmap2;
                        } catch (Throwable th14) {
                            th = th14;
                            thumbFile = bitmap2;
                            c4 = cursor;
                            if (c4 != null) {
                            }
                            thumbFile.deactivate();
                            throw th;
                        }
                    }
                }
                i2 = 3;
                bitmap = bitmap2;
                bitmap2 = thumbFile2;
                thumbFile2 = c4;
                try {
                    blockingUri = baseUri.buildUpon().appendQueryParameter("blocking", "1").appendQueryParameter("orig_id", String.valueOf(origId)).appendQueryParameter(GroupMembership.GROUP_ID, String.valueOf(groupId)).build();
                    if (thumbFile2 != null) {
                    }
                    cursor = thumbFile2;
                } catch (SQLiteException e13) {
                    th = e13;
                    thumbFile = bitmap2;
                    c4 = thumbFile2;
                    bitmap2 = bitmap;
                    Log.w(MediaStore.TAG, th);
                    if (c4 != null) {
                    }
                    thumbFile.deactivate();
                    return bitmap2;
                } catch (Throwable th15) {
                    th = th15;
                    thumbFile = bitmap2;
                    c4 = thumbFile2;
                    if (c4 != null) {
                    }
                    thumbFile.deactivate();
                    throw th;
                }
                try {
                    c = contentResolver.query(blockingUri, PROJECTION, null, null, null);
                    if (c != null) {
                    }
                } catch (SQLiteException e14) {
                    th = e14;
                    thumbFile = bitmap2;
                    c4 = cursor;
                    bitmap2 = bitmap;
                    Log.w(MediaStore.TAG, th);
                    if (c4 != null) {
                        c4.close();
                    }
                    thumbFile.deactivate();
                    return bitmap2;
                } catch (Throwable th16) {
                    th = th16;
                    thumbFile = bitmap2;
                    c4 = cursor;
                    if (c4 != null) {
                    }
                    thumbFile.deactivate();
                    throw th;
                }
            } catch (SQLiteException e15) {
                th = e15;
                cursor = c4;
                thumbFile = thumbFile2;
                bitmap = bitmap2;
                Log.w(MediaStore.TAG, th);
                if (c4 != null) {
                }
                thumbFile.deactivate();
                return bitmap2;
            } catch (Throwable th17) {
                th = th17;
                cursor = c4;
                thumbFile = thumbFile2;
                bitmap = bitmap2;
                if (c4 != null) {
                }
                thumbFile.deactivate();
                throw th;
            }
        }
    }

    public interface MediaColumns extends BaseColumns {
        public static final String DATA = "_data";
        public static final String DATE_ADDED = "date_added";
        public static final String DATE_MODIFIED = "date_modified";
        public static final String DISPLAY_NAME = "_display_name";
        public static final String HEIGHT = "height";
        public static final String IS_DRM = "is_drm";
        public static final String MEDIA_SCANNER_NEW_OBJECT_ID = "media_scanner_new_object_id";
        public static final String MIME_TYPE = "mime_type";
        public static final String SIZE = "_size";
        public static final String TITLE = "title";
        public static final String WIDTH = "width";
    }

    public static Uri getMediaScannerUri() {
        return Uri.parse("content://media/none/media_scanner");
    }

    public static String getVersion(Context context) {
        Cursor c = context.getContentResolver().query(Uri.parse("content://media/none/version"), null, null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    String string = c.getString(0);
                    return string;
                }
                c.close();
            } finally {
                c.close();
            }
        }
        return null;
    }

    public static Uri getDocumentUri(Context context, Uri mediaUri) {
        try {
            ContentResolver resolver = context.getContentResolver();
            return getDocumentUri(resolver, getFilePath(resolver, mediaUri), resolver.getPersistedUriPermissions());
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    private static String getFilePath(ContentResolver resolver, Uri mediaUri) throws RemoteException {
        ContentProviderClient client = resolver.acquireUnstableContentProviderClient(AUTHORITY);
        Cursor c;
        try {
            c = client.query(mediaUri, new String[]{"_data"}, null, null, null);
            if (c.getCount() == 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Not found media file under URI: ");
                stringBuilder.append(mediaUri);
                throw new IllegalStateException(stringBuilder.toString());
            } else if (c.moveToFirst()) {
                String path = c.getString(null);
                IoUtils.closeQuietly(c);
                if (client != null) {
                    $closeResource(null, client);
                }
                return path;
            } else {
                throw new IllegalStateException("Failed to move cursor to the first item.");
            }
        } catch (Throwable th) {
            Throwable th2 = th;
            try {
            } catch (Throwable th3) {
                if (client != null) {
                    $closeResource(th2, client);
                }
            }
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    /* JADX WARNING: Missing block: B:10:0x002a, code skipped:
            if (r0 != null) goto L_0x002c;
     */
    /* JADX WARNING: Missing block: B:11:0x002c, code skipped:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static Uri getDocumentUri(ContentResolver resolver, String path, List<UriPermission> uriPermissions) throws RemoteException {
        ContentProviderClient client = resolver.acquireUnstableContentProviderClient(DocumentsContract.EXTERNAL_STORAGE_PROVIDER_AUTHORITY);
        Bundle in = new Bundle();
        in.putParcelableList("com.android.externalstorage.documents.extra.uriPermissions", uriPermissions);
        Uri uri = (Uri) client.call("getDocumentId", path, in).getParcelable("uri");
        if (client != null) {
            $closeResource(null, client);
        }
        return uri;
    }

    public static String getPath(Context context, Uri uri) {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                String[] split = DocumentsContract.getDocumentId(uri).split(SettingsStringUtil.DELIMITER);
                if ("primary".equalsIgnoreCase(split[0])) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(Environment.getExternalStorageDirectory());
                    stringBuilder.append("/");
                    stringBuilder.append(split[1]);
                    return stringBuilder.toString();
                }
            } else if (isDownloadsDocument(uri)) {
                try {
                    return getDataColumn(context, ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(DocumentsContract.getDocumentId(uri))), null, null);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "exception get contenturi.");
                    return null;
                }
            } else if (isMediaDocument(uri)) {
                String type = DocumentsContract.getDocumentId(uri).split(SettingsStringUtil.DELIMITER)[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = Media.EXTERNAL_CONTENT_URI;
                }
                String selection = "_id=?";
                return getDataColumn(context, contentUri, "_id=?", new String[]{split[1]});
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        } else {
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        }
        return null;
    }

    /* JADX WARNING: Missing block: B:10:0x002e, code skipped:
            if (r0 != null) goto L_0x0030;
     */
    /* JADX WARNING: Missing block: B:20:0x0049, code skipped:
            if (r0 == null) goto L_0x004c;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String column = "_data";
        try {
            cursor = context.getContentResolver().query(uri, new String[]{"_data"}, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                String string = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
                if (cursor != null) {
                    cursor.close();
                }
                return string;
            }
        } catch (SQLiteException e) {
            Log.w(TAG, "SQLiteException when getDataColumn");
        } catch (IllegalArgumentException e2) {
            Log.w(TAG, "IllegalArgumentException when getDataColumn");
            if (cursor != null) {
                cursor.close();
            }
            return null;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return DocumentsContract.EXTERNAL_STORAGE_PROVIDER_AUTHORITY.equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
