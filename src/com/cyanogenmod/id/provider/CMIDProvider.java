package com.cyanogenmod.id.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

public class CMIDProvider extends ContentProvider {

    private static String TAG = CMIDProvider.class.getSimpleName();
    public static final String AUTHORITY = "com.cyanogenmod.id.store";
    private static final String HANDSHAKE_TOKEN_PATH = "token";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY).buildUpon().appendPath(HANDSHAKE_TOKEN_PATH).build();

    private static final String TABLE_HANDSHAKE_TOKEN = "handshake_token";
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    private static final int HANDSHAKE_TOKEN = 1;
    private static final int HANDSHAKE_TOKEN_ID = 2;

    private static HashMap<String, String> sSecretProjectionMap;

    static {
        URI_MATCHER.addURI(AUTHORITY, HANDSHAKE_TOKEN_PATH, HANDSHAKE_TOKEN);
        URI_MATCHER.addURI(AUTHORITY, HANDSHAKE_TOKEN_PATH + "/#", HANDSHAKE_TOKEN_ID);

        sSecretProjectionMap = new HashMap<String, String>();
        sSecretProjectionMap.put(HandshakeStoreColumns._ID, HandshakeStoreColumns._ID);
        sSecretProjectionMap.put(HandshakeStoreColumns.SECRET, HandshakeStoreColumns.SECRET);
        sSecretProjectionMap.put(HandshakeStoreColumns.EXPIRATION, HandshakeStoreColumns.EXPIRATION);
        sSecretProjectionMap.put(HandshakeStoreColumns.METHOD, HandshakeStoreColumns.METHOD);
    }
    private SQLiteOpenHelper mOpenHelper;


    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        cleanUpExpiredTokens();
        return true;
    }

    private void cleanUpExpiredTokens() {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.execSQL("delete from " + TABLE_HANDSHAKE_TOKEN + " where " + HandshakeStoreColumns.EXPIRATION + " < datetime('now', 'localtime')");
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (Binder.getCallingPid() != android.os.Process.myPid()) {
            throw new SecurityException("Cannot read from this provider");
        }
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (URI_MATCHER.match(uri)) {
            case HANDSHAKE_TOKEN:
                qb.setTables(TABLE_HANDSHAKE_TOKEN);
                qb.setProjectionMap(sSecretProjectionMap);
                break;
            case HANDSHAKE_TOKEN_ID:
                qb.setTables(TABLE_HANDSHAKE_TOKEN);
                qb.setProjectionMap(sSecretProjectionMap);
                qb.appendWhere(HandshakeStoreColumns._ID + "=" + uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        int type = URI_MATCHER.match(uri);
        switch (type) {
            case HANDSHAKE_TOKEN:
                return HandshakeStoreColumns.CONTENT_TYPE;
            case HANDSHAKE_TOKEN_ID:
                return HandshakeStoreColumns.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (Binder.getCallingPid() != android.os.Process.myPid()) {
            throw new SecurityException("Cannot insert into this provider");
        }
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (URI_MATCHER.match(uri)) {
            case HANDSHAKE_TOKEN:
                long rowId = db.insert(TABLE_HANDSHAKE_TOKEN, null, values);
                if (rowId != -1) {
                    Uri newUri = ContentUris.withAppendedId(uri, rowId);
                    getContext().getContentResolver().notifyChange(newUri, null);
                    return newUri;
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (Binder.getCallingPid() != android.os.Process.myPid()) {
            throw new SecurityException("Cannot delete from this provider");
        }
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;
        switch (URI_MATCHER.match(uri)) {
            case HANDSHAKE_TOKEN:
                count = db.delete(TABLE_HANDSHAKE_TOKEN, selection, selectionArgs);
                break;
            case HANDSHAKE_TOKEN_ID:
                count = db.delete(TABLE_HANDSHAKE_TOKEN, HandshakeStoreColumns._ID + "=" + ContentUris.parseId(uri)
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (Binder.getCallingPid() != android.os.Process.myPid()) {
            throw new SecurityException("Cannot insert into this provider");
        }
        switch (URI_MATCHER.match(uri)) {
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "cmid.db";
        private static final int DATABASE_VERSION = 4;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_HANDSHAKE_TOKEN
                    + " ("
                    + HandshakeStoreColumns._ID + " INTEGER PRIMARY KEY, "
                    + HandshakeStoreColumns.SECRET + " TEXT NOT NULL UNIQUE, "
                    + HandshakeStoreColumns.EXPIRATION + " DATETIME DEFAULT 0, "
                    + HandshakeStoreColumns.METHOD + " TEXT NOT NULL);");

            db.execSQL("create trigger update_expiration after insert on " + TABLE_HANDSHAKE_TOKEN +
                    " begin update " + TABLE_HANDSHAKE_TOKEN + " set " + HandshakeStoreColumns.EXPIRATION +
                    "= datetime('now', '+10 minutes', 'localtime') where expiration = 0" +
                    "; end");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_HANDSHAKE_TOKEN);
            onCreate(db);
        }
    }

    public static interface HandshakeStoreColumns {
        public static final String _ID = "_id";
        public static final String SECRET = "secret";
        public static final String EXPIRATION = "expiration";
        public static final String METHOD = "method";
        public static final String CONTENT_TYPE = "vnd.cyanogenmod.cursor.dir/secret";
        public static final String CONTENT_ITEM_TYPE = "vnd.cyanogenmod.cursor.item/secret";
    }
}
