package com.android.commands.content;

import android.app.ActivityManager;
import android.app.ContentProviderHolder;
import android.app.IActivityManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.IContentProvider;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.text.TextUtils;
import java.io.FileDescriptor;
import java.io.PrintStream;

public class Content {
    private static final String USAGE = "usage: adb shell content [subcommand] [options]\n\nusage: adb shell content insert --uri <URI> [--user <USER_ID>] --bind <BINDING> [--bind <BINDING>...]\n  <URI> a content provider URI.\n  <BINDING> binds a typed value to a column and is formatted:\n  <COLUMN_NAME>:<TYPE>:<COLUMN_VALUE> where:\n  <TYPE> specifies data type such as:\n  b - boolean, s - string, i - integer, l - long, f - float, d - double\n  Note: Omit the value for passing an empty string, e.g column:s:\n  Example:\n  # Add \"new_setting\" secure setting with value \"new_value\".\n  adb shell content insert --uri content://settings/secure --bind name:s:new_setting --bind value:s:new_value\n\nusage: adb shell content update --uri <URI> [--user <USER_ID>] [--where <WHERE>]\n  <WHERE> is a SQL style where clause in quotes (You have to escape single quotes - see example below).\n  Example:\n  # Change \"new_setting\" secure setting to \"newer_value\".\n  adb shell content update --uri content://settings/secure --bind value:s:newer_value --where \"name='new_setting'\"\n\nusage: adb shell content delete --uri <URI> [--user <USER_ID>] --bind <BINDING> [--bind <BINDING>...] [--where <WHERE>]\n  Example:\n  # Remove \"new_setting\" secure setting.\n  adb shell content delete --uri content://settings/secure --where \"name='new_setting'\"\n\nusage: adb shell content query --uri <URI> [--user <USER_ID>] [--projection <PROJECTION>] [--where <WHERE>] [--sort <SORT_ORDER>]\n  <PROJECTION> is a list of colon separated column names and is formatted:\n  <COLUMN_NAME>[:<COLUMN_NAME>...]\n  <SORT_ORDER> is the order in which rows in the result should be sorted.\n  Example:\n  # Select \"name\" and \"value\" columns from secure settings where \"name\" is equal to \"new_setting\" and sort the result by name in ascending order.\n  adb shell content query --uri content://settings/secure --projection name:value --where \"name='new_setting'\" --sort \"name ASC\"\n\nusage: adb shell content call --uri <URI> --method <METHOD> [--arg <ARG>]\n       [--extra <BINDING> ...]\n  <METHOD> is the name of a provider-defined method\n  <ARG> is an optional string argument\n  <BINDING> is like --bind above, typed data of the form <KEY>:{b,s,i,l,f,d}:<VAL>\n\nusage: adb shell content read --uri <URI> [--user <USER_ID>]\n  Example:\n  adb shell 'content read --uri content://settings/system/ringtone_cache' > host.ogg\n\nusage: adb shell content write --uri <URI> [--user <USER_ID>]\n  Example:\n  adb shell 'content write --uri content://settings/system/ringtone_cache' < host.ogg\n\nusage: adb shell content gettype --uri <URI> [--user <USER_ID>]\n  Example:\n  adb shell content gettype --uri content://media/internal/audio/media/\n\n";

    private static abstract class Command {
        final Uri mUri;
        final int mUserId;

        protected abstract void onExecute(IContentProvider iContentProvider) throws Exception;

        public Command(Uri uri, int userId) {
            this.mUri = uri;
            this.mUserId = userId;
        }

        public final void execute() {
            String providerName = this.mUri.getAuthority();
            IActivityManager activityManager;
            IBinder token;
            try {
                activityManager = ActivityManager.getService();
                token = new Binder();
                ContentProviderHolder holder = activityManager.getContentProviderExternal(providerName, this.mUserId, token);
                if (holder != null) {
                    IContentProvider provider = holder.provider;
                    onExecute(provider);
                    if (provider != null) {
                        activityManager.removeContentProviderExternal(providerName, token);
                        return;
                    }
                    return;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Could not find provider: ");
                stringBuilder.append(providerName);
                throw new IllegalStateException(stringBuilder.toString());
            } catch (Exception e) {
                PrintStream printStream = System.err;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error while accessing provider:");
                stringBuilder2.append(providerName);
                printStream.println(stringBuilder2.toString());
                e.printStackTrace();
            } catch (Throwable th) {
                if (null != null) {
                    activityManager.removeContentProviderExternal(providerName, token);
                }
            }
        }

        public static String resolveCallingPackage() {
            int myUid = Process.myUid();
            if (myUid == 0) {
                return "root";
            }
            if (myUid != 2000) {
                return null;
            }
            return "com.android.shell";
        }
    }

    private static class Parser {
        private static final String ARGUMENT_ARG = "--arg";
        private static final String ARGUMENT_BIND = "--bind";
        private static final String ARGUMENT_CALL = "call";
        private static final String ARGUMENT_DELETE = "delete";
        private static final String ARGUMENT_EXTRA = "--extra";
        private static final String ARGUMENT_GET_TYPE = "gettype";
        private static final String ARGUMENT_INSERT = "insert";
        private static final String ARGUMENT_METHOD = "--method";
        private static final String ARGUMENT_PREFIX = "--";
        private static final String ARGUMENT_PROJECTION = "--projection";
        private static final String ARGUMENT_QUERY = "query";
        private static final String ARGUMENT_READ = "read";
        private static final String ARGUMENT_SORT = "--sort";
        private static final String ARGUMENT_UPDATE = "update";
        private static final String ARGUMENT_URI = "--uri";
        private static final String ARGUMENT_USER = "--user";
        private static final String ARGUMENT_WHERE = "--where";
        private static final String ARGUMENT_WRITE = "write";
        private static final String COLON = ":";
        private static final String TYPE_BOOLEAN = "b";
        private static final String TYPE_DOUBLE = "d";
        private static final String TYPE_FLOAT = "f";
        private static final String TYPE_INTEGER = "i";
        private static final String TYPE_LONG = "l";
        private static final String TYPE_STRING = "s";
        private final Tokenizer mTokenizer;

        public Parser(String[] args) {
            this.mTokenizer = new Tokenizer(args);
        }

        public Command parseCommand() {
            StringBuilder stringBuilder;
            try {
                String operation = this.mTokenizer.nextArg();
                if (ARGUMENT_INSERT.equals(operation)) {
                    return parseInsertCommand();
                }
                if (ARGUMENT_DELETE.equals(operation)) {
                    return parseDeleteCommand();
                }
                if (ARGUMENT_UPDATE.equals(operation)) {
                    return parseUpdateCommand();
                }
                if (ARGUMENT_QUERY.equals(operation)) {
                    return parseQueryCommand();
                }
                if (ARGUMENT_CALL.equals(operation)) {
                    return parseCallCommand();
                }
                if (ARGUMENT_READ.equals(operation)) {
                    return parseReadCommand();
                }
                if (ARGUMENT_WRITE.equals(operation)) {
                    return parseWriteCommand();
                }
                if (ARGUMENT_GET_TYPE.equals(operation)) {
                    return parseGetTypeCommand();
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported operation: ");
                stringBuilder.append(operation);
                throw new IllegalArgumentException(stringBuilder.toString());
            } catch (IllegalArgumentException iae) {
                System.out.println(Content.USAGE);
                PrintStream printStream = System.out;
                stringBuilder = new StringBuilder();
                stringBuilder.append("[ERROR] ");
                stringBuilder.append(iae.getMessage());
                printStream.println(stringBuilder.toString());
                return null;
            }
        }

        private InsertCommand parseInsertCommand() {
            Uri uri = null;
            int userId = 0;
            ContentValues values = new ContentValues();
            while (true) {
                String access$000 = this.mTokenizer.nextArg();
                String argument = access$000;
                if (access$000 != null) {
                    if (ARGUMENT_URI.equals(argument)) {
                        uri = Uri.parse(argumentValueRequired(argument));
                    } else if (ARGUMENT_USER.equals(argument)) {
                        userId = Integer.parseInt(argumentValueRequired(argument));
                    } else if (ARGUMENT_BIND.equals(argument)) {
                        parseBindValue(values);
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unsupported argument: ");
                        stringBuilder.append(argument);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                } else if (uri == null) {
                    throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                } else if (values.size() != 0) {
                    return new InsertCommand(uri, userId, values);
                } else {
                    throw new IllegalArgumentException("Bindings not specified. Did you specify --bind argument(s)?");
                }
            }
        }

        private DeleteCommand parseDeleteCommand() {
            Uri uri = null;
            int userId = 0;
            String where = null;
            while (true) {
                String access$000 = this.mTokenizer.nextArg();
                String argument = access$000;
                if (access$000 != null) {
                    if (ARGUMENT_URI.equals(argument)) {
                        uri = Uri.parse(argumentValueRequired(argument));
                    } else if (ARGUMENT_USER.equals(argument)) {
                        userId = Integer.parseInt(argumentValueRequired(argument));
                    } else if (ARGUMENT_WHERE.equals(argument)) {
                        where = argumentValueRequired(argument);
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unsupported argument: ");
                        stringBuilder.append(argument);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                } else if (uri != null) {
                    return new DeleteCommand(uri, userId, where);
                } else {
                    throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                }
            }
        }

        private UpdateCommand parseUpdateCommand() {
            Uri uri = null;
            int userId = 0;
            String where = null;
            ContentValues values = new ContentValues();
            while (true) {
                String access$000 = this.mTokenizer.nextArg();
                String argument = access$000;
                if (access$000 != null) {
                    if (ARGUMENT_URI.equals(argument)) {
                        uri = Uri.parse(argumentValueRequired(argument));
                    } else if (ARGUMENT_USER.equals(argument)) {
                        userId = Integer.parseInt(argumentValueRequired(argument));
                    } else if (ARGUMENT_WHERE.equals(argument)) {
                        where = argumentValueRequired(argument);
                    } else if (ARGUMENT_BIND.equals(argument)) {
                        parseBindValue(values);
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unsupported argument: ");
                        stringBuilder.append(argument);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                } else if (uri == null) {
                    throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                } else if (values.size() != 0) {
                    return new UpdateCommand(uri, userId, values, where);
                } else {
                    throw new IllegalArgumentException("Bindings not specified. Did you specify --bind argument(s)?");
                }
            }
        }

        public CallCommand parseCallCommand() {
            String method = null;
            int userId = 0;
            String arg = null;
            Uri uri = null;
            ContentValues values = new ContentValues();
            while (true) {
                String access$000 = this.mTokenizer.nextArg();
                String argument = access$000;
                if (access$000 != null) {
                    if (ARGUMENT_URI.equals(argument)) {
                        uri = Uri.parse(argumentValueRequired(argument));
                    } else if (ARGUMENT_USER.equals(argument)) {
                        userId = Integer.parseInt(argumentValueRequired(argument));
                    } else if (ARGUMENT_METHOD.equals(argument)) {
                        method = argumentValueRequired(argument);
                    } else if (ARGUMENT_ARG.equals(argument)) {
                        arg = argumentValueRequired(argument);
                    } else if (ARGUMENT_EXTRA.equals(argument)) {
                        parseBindValue(values);
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unsupported argument: ");
                        stringBuilder.append(argument);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                } else if (uri == null) {
                    throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                } else if (method != null) {
                    return new CallCommand(uri, userId, method, arg, values);
                } else {
                    throw new IllegalArgumentException("Content provider method not specified.");
                }
            }
        }

        private GetTypeCommand parseGetTypeCommand() {
            Uri uri = null;
            int userId = 0;
            while (true) {
                String access$000 = this.mTokenizer.nextArg();
                String argument = access$000;
                if (access$000 != null) {
                    if (ARGUMENT_URI.equals(argument)) {
                        uri = Uri.parse(argumentValueRequired(argument));
                    } else if (ARGUMENT_USER.equals(argument)) {
                        userId = Integer.parseInt(argumentValueRequired(argument));
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unsupported argument: ");
                        stringBuilder.append(argument);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                } else if (uri != null) {
                    return new GetTypeCommand(uri, userId);
                } else {
                    throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                }
            }
        }

        private ReadCommand parseReadCommand() {
            Uri uri = null;
            int userId = 0;
            while (true) {
                String access$000 = this.mTokenizer.nextArg();
                String argument = access$000;
                if (access$000 != null) {
                    if (ARGUMENT_URI.equals(argument)) {
                        uri = Uri.parse(argumentValueRequired(argument));
                    } else if (ARGUMENT_USER.equals(argument)) {
                        userId = Integer.parseInt(argumentValueRequired(argument));
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unsupported argument: ");
                        stringBuilder.append(argument);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                } else if (uri != null) {
                    return new ReadCommand(uri, userId);
                } else {
                    throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                }
            }
        }

        private WriteCommand parseWriteCommand() {
            Uri uri = null;
            int userId = 0;
            while (true) {
                String access$000 = this.mTokenizer.nextArg();
                String argument = access$000;
                if (access$000 != null) {
                    if (ARGUMENT_URI.equals(argument)) {
                        uri = Uri.parse(argumentValueRequired(argument));
                    } else if (ARGUMENT_USER.equals(argument)) {
                        userId = Integer.parseInt(argumentValueRequired(argument));
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unsupported argument: ");
                        stringBuilder.append(argument);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                } else if (uri != null) {
                    return new WriteCommand(uri, userId);
                } else {
                    throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                }
            }
        }

        public QueryCommand parseQueryCommand() {
            Uri uri = null;
            int userId = 0;
            String[] projection = null;
            String sort = null;
            String where = null;
            while (true) {
                String access$000 = this.mTokenizer.nextArg();
                String argument = access$000;
                if (access$000 != null) {
                    if (ARGUMENT_URI.equals(argument)) {
                        uri = Uri.parse(argumentValueRequired(argument));
                    } else if (ARGUMENT_USER.equals(argument)) {
                        userId = Integer.parseInt(argumentValueRequired(argument));
                    } else if (ARGUMENT_WHERE.equals(argument)) {
                        where = argumentValueRequired(argument);
                    } else if (ARGUMENT_SORT.equals(argument)) {
                        sort = argumentValueRequired(argument);
                    } else if (ARGUMENT_PROJECTION.equals(argument)) {
                        projection = argumentValueRequired(argument).split("[\\s]*:[\\s]*");
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unsupported argument: ");
                        stringBuilder.append(argument);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                } else if (uri != null) {
                    return new QueryCommand(uri, userId, projection, where, sort);
                } else {
                    throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                }
            }
        }

        private void parseBindValue(ContentValues values) {
            String argument = this.mTokenizer.nextArg();
            if (TextUtils.isEmpty(argument)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Binding not well formed: ");
                stringBuilder.append(argument);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            int firstColonIndex = argument.indexOf(COLON);
            if (firstColonIndex >= 0) {
                int secondColonIndex = argument.indexOf(COLON, firstColonIndex + 1);
                if (secondColonIndex >= 0) {
                    String column = argument.substring(null, firstColonIndex);
                    String type = argument.substring(firstColonIndex + 1, secondColonIndex);
                    String value = argument.substring(secondColonIndex + 1);
                    if (TYPE_STRING.equals(type)) {
                        values.put(column, value);
                        return;
                    } else if (TYPE_BOOLEAN.equalsIgnoreCase(type)) {
                        values.put(column, Boolean.valueOf(Boolean.parseBoolean(value)));
                        return;
                    } else if (TYPE_INTEGER.equalsIgnoreCase(type) || TYPE_LONG.equalsIgnoreCase(type)) {
                        values.put(column, Long.valueOf(Long.parseLong(value)));
                        return;
                    } else if (TYPE_FLOAT.equalsIgnoreCase(type) || TYPE_DOUBLE.equalsIgnoreCase(type)) {
                        values.put(column, Double.valueOf(Double.parseDouble(value)));
                        return;
                    } else {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unsupported type: ");
                        stringBuilder2.append(type);
                        throw new IllegalArgumentException(stringBuilder2.toString());
                    }
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Binding not well formed: ");
                stringBuilder3.append(argument);
                throw new IllegalArgumentException(stringBuilder3.toString());
            }
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("Binding not well formed: ");
            stringBuilder4.append(argument);
            throw new IllegalArgumentException(stringBuilder4.toString());
        }

        private String argumentValueRequired(String argument) {
            String value = this.mTokenizer.nextArg();
            if (!TextUtils.isEmpty(value) && !value.startsWith(ARGUMENT_PREFIX)) {
                return value;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No value for argument: ");
            stringBuilder.append(argument);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static class Tokenizer {
        private final String[] mArgs;
        private int mNextArg;

        public Tokenizer(String[] args) {
            this.mArgs = args;
        }

        private String nextArg() {
            if (this.mNextArg >= this.mArgs.length) {
                return null;
            }
            String[] strArr = this.mArgs;
            int i = this.mNextArg;
            this.mNextArg = i + 1;
            return strArr[i];
        }
    }

    private static class CallCommand extends Command {
        final String mArg;
        Bundle mExtras = null;
        final String mMethod;

        public CallCommand(Uri uri, int userId, String method, String arg, ContentValues values) {
            super(uri, userId);
            this.mMethod = method;
            this.mArg = arg;
            if (values != null) {
                this.mExtras = new Bundle();
                for (String key : values.keySet()) {
                    Object val = values.get(key);
                    if (val instanceof String) {
                        this.mExtras.putString(key, (String) val);
                    } else if (val instanceof Float) {
                        this.mExtras.putFloat(key, ((Float) val).floatValue());
                    } else if (val instanceof Double) {
                        this.mExtras.putDouble(key, ((Double) val).doubleValue());
                    } else if (val instanceof Boolean) {
                        this.mExtras.putBoolean(key, ((Boolean) val).booleanValue());
                    } else if (val instanceof Integer) {
                        this.mExtras.putInt(key, ((Integer) val).intValue());
                    } else if (val instanceof Long) {
                        this.mExtras.putLong(key, ((Long) val).longValue());
                    }
                }
            }
        }

        public void onExecute(IContentProvider provider) throws Exception {
            Bundle result = provider.call(null, this.mMethod, this.mArg, this.mExtras);
            if (result != null) {
                result.size();
            }
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Result: ");
            stringBuilder.append(result);
            printStream.println(stringBuilder.toString());
        }
    }

    private static class DeleteCommand extends Command {
        final String mWhere;

        public DeleteCommand(Uri uri, int userId, String where) {
            super(uri, userId);
            this.mWhere = where;
        }

        public void onExecute(IContentProvider provider) throws Exception {
            provider.delete(Command.resolveCallingPackage(), this.mUri, this.mWhere, null);
        }
    }

    private static class GetTypeCommand extends Command {
        public GetTypeCommand(Uri uri, int userId) {
            super(uri, userId);
        }

        public void onExecute(IContentProvider provider) throws Exception {
            String type = provider.getType(this.mUri);
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Result: ");
            stringBuilder.append(type);
            printStream.println(stringBuilder.toString());
        }
    }

    private static class InsertCommand extends Command {
        final ContentValues mContentValues;

        public InsertCommand(Uri uri, int userId, ContentValues contentValues) {
            super(uri, userId);
            this.mContentValues = contentValues;
        }

        public void onExecute(IContentProvider provider) throws Exception {
            provider.insert(Command.resolveCallingPackage(), this.mUri, this.mContentValues);
        }
    }

    private static class ReadCommand extends Command {
        public ReadCommand(Uri uri, int userId) {
            super(uri, userId);
        }

        /* JADX WARNING: Missing block: B:9:0x0020, code:
            if (r0 != null) goto L_0x0022;
     */
        /* JADX WARNING: Missing block: B:10:0x0022, code:
            if (r1 != null) goto L_0x0024;
     */
        /* JADX WARNING: Missing block: B:12:?, code:
            r0.close();
     */
        /* JADX WARNING: Missing block: B:13:0x0028, code:
            r3 = move-exception;
     */
        /* JADX WARNING: Missing block: B:14:0x0029, code:
            r1.addSuppressed(r3);
     */
        /* JADX WARNING: Missing block: B:15:0x002d, code:
            r0.close();
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onExecute(IContentProvider provider) throws Exception {
            ParcelFileDescriptor fd = provider.openFile(null, this.mUri, "r", null, null);
            FileUtils.copy(fd.getFileDescriptor(), FileDescriptor.out);
            if (fd != null) {
                fd.close();
            }
        }
    }

    private static class WriteCommand extends Command {
        public WriteCommand(Uri uri, int userId) {
            super(uri, userId);
        }

        /* JADX WARNING: Missing block: B:9:0x0020, code:
            if (r0 != null) goto L_0x0022;
     */
        /* JADX WARNING: Missing block: B:10:0x0022, code:
            if (r1 != null) goto L_0x0024;
     */
        /* JADX WARNING: Missing block: B:12:?, code:
            r0.close();
     */
        /* JADX WARNING: Missing block: B:13:0x0028, code:
            r3 = move-exception;
     */
        /* JADX WARNING: Missing block: B:14:0x0029, code:
            r1.addSuppressed(r3);
     */
        /* JADX WARNING: Missing block: B:15:0x002d, code:
            r0.close();
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onExecute(IContentProvider provider) throws Exception {
            ParcelFileDescriptor fd = provider.openFile(null, this.mUri, "w", null, null);
            FileUtils.copy(FileDescriptor.in, fd.getFileDescriptor());
            if (fd != null) {
                fd.close();
            }
        }
    }

    private static class QueryCommand extends DeleteCommand {
        final String[] mProjection;
        final String mSortOrder;

        public QueryCommand(Uri uri, int userId, String[] projection, String where, String sortOrder) {
            super(uri, userId, where);
            this.mProjection = projection;
            this.mSortOrder = sortOrder;
        }

        public void onExecute(IContentProvider provider) throws Exception {
            Cursor cursor = provider.query(Command.resolveCallingPackage(), this.mUri, this.mProjection, ContentResolver.createSqlQueryBundle(this.mWhere, null, this.mSortOrder), null);
            if (cursor == null) {
                System.out.println("No result found.");
                return;
            }
            try {
                if (cursor.moveToFirst()) {
                    int rowIndex = 0;
                    StringBuilder builder = new StringBuilder();
                    do {
                        int i = 0;
                        builder.setLength(0);
                        builder.append("Row: ");
                        builder.append(rowIndex);
                        builder.append(" ");
                        rowIndex++;
                        int columnCount = cursor.getColumnCount();
                        while (i < columnCount) {
                            if (i > 0) {
                                builder.append(", ");
                            }
                            String columnName = cursor.getColumnName(i);
                            String columnValue = null;
                            int columnIndex = cursor.getColumnIndex(columnName);
                            switch (cursor.getType(columnIndex)) {
                                case 0:
                                    columnValue = "NULL";
                                    break;
                                case 1:
                                    columnValue = String.valueOf(cursor.getLong(columnIndex));
                                    break;
                                case 2:
                                    columnValue = String.valueOf(cursor.getFloat(columnIndex));
                                    break;
                                case 3:
                                    columnValue = cursor.getString(columnIndex);
                                    break;
                                case 4:
                                    columnValue = "BLOB";
                                    break;
                                default:
                                    break;
                            }
                            builder.append(columnName);
                            builder.append("=");
                            builder.append(columnValue);
                            i++;
                        }
                        System.out.println(builder);
                    } while (cursor.moveToNext());
                } else {
                    System.out.println("No result found.");
                }
                cursor.close();
            } catch (Throwable th) {
                cursor.close();
            }
        }
    }

    private static class UpdateCommand extends InsertCommand {
        final String mWhere;

        public UpdateCommand(Uri uri, int userId, ContentValues contentValues, String where) {
            super(uri, userId, contentValues);
            this.mWhere = where;
        }

        public void onExecute(IContentProvider provider) throws Exception {
            provider.update(Command.resolveCallingPackage(), this.mUri, this.mContentValues, this.mWhere, null);
        }
    }

    public static void main(String[] args) {
        Command command = new Parser(args).parseCommand();
        if (command != null) {
            command.execute();
        }
    }
}
