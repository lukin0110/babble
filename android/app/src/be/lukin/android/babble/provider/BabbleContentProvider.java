package be.lukin.android.babble.provider;

import java.util.HashMap;

import be.lukin.android.babble.Log;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class BabbleContentProvider extends ContentProvider {

	public static final String PHRASE_TABLE_NAME = "phrase";

	public static final String AUTHORITY = "be.lukin.android.babble.provider.BabbleContentProvider";

	private static final String DATABASE_NAME = "babble.db";

	private static final int DATABASE_VERSION = 1;

	private static final UriMatcher sUriMatcher;

	private static final int PHRASE = 1;
	private static final int PHRASE_ID = 2;

	private static HashMap<String, String> appsProjectionMap;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		private final Context mContext;

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mContext = context;
		}


		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + PHRASE_TABLE_NAME + " ("
					+ Phrase.Columns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ Phrase.Columns.TIMESTAMP + " TIMESTAMP,"
					+ Phrase.Columns.TEXT + " TEXT NOT NULL,"
					+ Phrase.Columns.LANG + " TEXT NOT NULL," // TODO: should be short string
					+ Phrase.Columns.DIST + " INTEGER,"
					+ Phrase.Columns.RESULT + " TEXT NOT NULL"
					+ ");");
		}


		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i("Upgrading database v" + oldVersion + " -> v" + newVersion + ", which will destroy all old data.");
			db.execSQL("DROP TABLE IF EXISTS " + PHRASE_TABLE_NAME);
			onCreate(db);
		}
	}

	private DatabaseHelper dbHelper;

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case PHRASE:
			count = db.delete(PHRASE_TABLE_NAME, where, whereArgs);
			break;

		case PHRASE_ID:
			String appId = uri.getPathSegments().get(1);
			count = db.delete(
					PHRASE_TABLE_NAME,
					Phrase.Columns._ID + "=" + appId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
					whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}


	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case PHRASE:
			return Phrase.Columns.CONTENT_TYPE;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}


	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = 0;
		Uri returnUri = null;

		switch (sUriMatcher.match(uri)) {
		case PHRASE:
			rowId = db.insert(PHRASE_TABLE_NAME, Phrase.Columns.TEXT, values);
			if (rowId <= 0) {
				throw new SQLException("Failed to insert row into " + uri);
			}
			returnUri = ContentUris.withAppendedId(Phrase.Columns.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(returnUri, null);
			return returnUri;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}


	@Override
	public boolean onCreate() {
		dbHelper = new DatabaseHelper(getContext());
		return true;
	}


	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch (sUriMatcher.match(uri)) {
		case PHRASE:
			qb.setTables(PHRASE_TABLE_NAME);
			qb.setProjectionMap(appsProjectionMap);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}


	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case PHRASE:
			count = db.update(PHRASE_TABLE_NAME, values, where, whereArgs);
			break;

		case PHRASE_ID:
			String appId = uri.getPathSegments().get(1);
			count = db.update(
					PHRASE_TABLE_NAME,
					values,
					Phrase.Columns._ID + "=" + appId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
					whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, PHRASE_TABLE_NAME, PHRASE);
		sUriMatcher.addURI(AUTHORITY, PHRASE_TABLE_NAME + "/#", PHRASE_ID);

		appsProjectionMap = new HashMap<String, String>();
		appsProjectionMap.put(Phrase.Columns._ID, Phrase.Columns._ID);
		appsProjectionMap.put(Phrase.Columns.TIMESTAMP, Phrase.Columns.TIMESTAMP);
		appsProjectionMap.put(Phrase.Columns.TEXT, Phrase.Columns.TEXT);
		appsProjectionMap.put(Phrase.Columns.LANG, Phrase.Columns.LANG);
		appsProjectionMap.put(Phrase.Columns.DIST, Phrase.Columns.DIST);
		appsProjectionMap.put(Phrase.Columns.RESULT, Phrase.Columns.RESULT);
	}
}