/**
 * Database helper class for SleepLogger
 */
package org.acmelab.sleeplogger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * @author mchang
 * 
 */
public class SleepDatabaseHelper extends SQLiteOpenHelper {

	/* log tag */
	private static final String TAG = "SleepDBHelper";

	/* database keys */
	public static final String KEY_TIME = "time";
	public static final String KEY_DATE = "date";
	public static final String KEY_ACTION = "action";
	public static final String KEY_ROWID = "_id";

	String[] KEYS = { KEY_ROWID, KEY_DATE, KEY_TIME, KEY_ACTION };

	private static final String DATABASE_NAME = "sleepdb";
	private static final String DATABASE_TABLE = "sleeplog";
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_CREATE = "CREATE TABLE "
			+ DATABASE_TABLE + " (_id integer primary key autoincrement, "
			+ "date VARCHAR, time VARCHAR, action VARCHAR);";

	/* database handle */
	private SQLiteDatabase mDb;

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_CREATE);
	}

	public SleepDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading database from " + oldVersion + " to "
				+ newVersion);
		db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
		onCreate(db);
	}

	/* db handlers */
	public SleepDatabaseHelper open() throws SQLException {
		mDb = this.getWritableDatabase();

		return this;
	}

	/* wipes the database */
	public void resetDatabase() {
		mDb.delete(DATABASE_TABLE, null, null);
	}
	
	/* create a row */
	public long createRow(String date, String time, String action) {
		ContentValues cv = new ContentValues();
		cv.put(KEY_DATE, date);
		cv.put(KEY_TIME, time);
		cv.put(KEY_ACTION, action);

		return mDb.insert(DATABASE_TABLE, null, cv);
	}

	/* return a cursor over all entries */
	public Cursor fetchAllEntries() {
		return mDb.query(DATABASE_TABLE, KEYS, null, null, null, null, KEY_ROWID + " DESC");
	}

}
