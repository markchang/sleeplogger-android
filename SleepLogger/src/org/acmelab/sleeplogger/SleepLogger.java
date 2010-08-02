/**
 * SleepLogger.java
 * 
 * SleepLogger logs the sleep habits of a single individual to a local 
 * Android database.
 * 
 * Features (some implemented):
 *  - export to csv
 *  - graphing
 */

package org.acmelab.sleeplogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.sql.Date;
import java.util.Calendar;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SimpleCursorAdapter;

public class SleepLogger extends ListActivity {
	public static final String TAG = "SleepLogger";

	private static final String FILENAME_CSV = "sleeplogger.csv";
	private static final int MENU_RESET = 0;
	private static final int MENU_QUIT = 1;
	private static final int MENU_EMAIL = 2;
	private static final int MENU_GRAPH = 3;

	Button btnSleep, btnWake;
	SleepDatabaseHelper dbHelper;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);

		btnSleep = (Button) findViewById(R.id.btnSleep);
		btnWake = (Button) findViewById(R.id.btnWake);

		btnSleep.setOnClickListener(mSleepClick);
		btnWake.setOnClickListener(mWakeClick);

		dbHelper = new SleepDatabaseHelper(this);

		openDatabase();
		fillData();
	}

	@Override
	public void onPause() {
		super.onPause();
		closeDatabase();
	}

	@Override
	public void onResume() {
		super.onResume();
		openDatabase();
		fillData();
	}

	/* sleep listener */
	private OnClickListener mSleepClick = new OnClickListener() {
		public void onClick(View v) {
			Calendar rightNow = Calendar.getInstance();
			openDatabase();
			logAction(rightNow, getString(R.string.db_sleep));
			fillData();
		}
	};

	/* wake listener */
	private OnClickListener mWakeClick = new OnClickListener() {
		public void onClick(View v) {
			Calendar rightNow = Calendar.getInstance();
			openDatabase();
			logAction(rightNow, getString(R.string.db_wake));
			fillData();
		}
	};

	/* menus */
	/* Creates the menu items */
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_EMAIL, 0, "Send Email").setIcon(
				android.R.drawable.ic_menu_send);
		menu.add(0, MENU_RESET, 0, "Reset Database").setIcon(
				android.R.drawable.ic_menu_delete);
		menu.add(0, MENU_QUIT, 0, "Quit").setIcon(
				android.R.drawable.ic_menu_close_clear_cancel);
		menu.add(0, MENU_GRAPH, 0, "Graph");
		return true;
	}

	/* Handles item selections */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_RESET:
			resetDatabase();
			return true;
		case MENU_QUIT:
			finish();
			return true;
		case MENU_EMAIL:
			openDatabase();
			writeCsvFile();
			sendEmail();
			fillData();
			return true;
		case MENU_GRAPH:
			graphStuff();
			return true;
		}
		return false;
	}

	private void graphStuff() {
		Intent i = new Intent(this, GraphViewDemo.class);
		startActivity(i);
	}

	/* -- utility functions -- */

	/*
	 * write the database to a CSV file on the sdcard error alert and return if
	 * the sdcard failed
	 */
	private boolean writeCsvFile() {
		if( sdCardPresent() ) {
			try {
				File fp = new File(android.os.Environment.getExternalStorageDirectory() + "/" + FILENAME_CSV);
				FileOutputStream fos = new FileOutputStream(fp);
				OutputStreamWriter osw = new OutputStreamWriter(fos);
				
				Cursor rows = dbHelper.fetchAllEntries();
				int dateIdx = rows.getColumnIndex("date");
				int timeIdx = rows.getColumnIndex("time");
				int actionIdx = rows.getColumnIndex("action");
				
				rows.moveToFirst();
				while(!rows.isAfterLast()) {
					String dateStr = rows.getString(dateIdx);
					String timeStr = rows.getString(timeIdx);
					String actionStr = rows.getString(actionIdx);
					
					osw.write(dateStr + "," + timeStr + "," + actionStr + "\n");
					
					rows.moveToNext();
				}
				
				osw.close();
				
				return true;
			} catch (Exception e) {
				new AlertDialog.Builder(this)
				.setCancelable(false)
				.setTitle("Error!")
				.setMessage("Problem writing to SD Card!")
				.setNeutralButton("Oh Well", null)
				.show();
				return false;
			}
		} else {
			new AlertDialog.Builder(this)
				.setCancelable(false)
				.setTitle("Error!")
				.setMessage("Need SD Card to email logs.")
				.setNeutralButton("I'll find one", null)
				.show();
			return false;
		}
	}

	private boolean sdCardPresent() {
		String Sdcard_status = Environment.getExternalStorageState();
		if(Sdcard_status.equals(Environment.MEDIA_REMOVED)) {
			return false;
		}
		else {
			return true;
		} 
	}
	/* send the database as csv via email intent */
	private void sendEmail() {
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT,
				"Attached is your Sleep Logger log. Enjoy.");
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Sleep Logger Output");
		sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///sdcard/" + FILENAME_CSV));
		sendIntent.setType("text/csv");
		startActivity(Intent.createChooser(sendIntent,
				"Please pick your preferred email application"));
		fillData();
	}

	/* reset the database contents */
	private void resetDatabase() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to erase the database?")
				.setCancelable(false).setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								openDatabase();
								dbHelper.resetDatabase();
								fillData();
							}
						}).setNegativeButton("No",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		builder.show();
	}

	/* fills the listview with all data from database */
	private void fillData() {
		Cursor c = dbHelper.fetchAllEntries();
		startManagingCursor(c);

		String[] from = new String[] { SleepDatabaseHelper.KEY_DATE,
				SleepDatabaseHelper.KEY_TIME, SleepDatabaseHelper.KEY_ACTION };
		int[] to = new int[] { R.id.date, R.id.time, R.id.action };

		// Now create an array adapter and set it to display using our row
		SimpleCursorAdapter sleep_rows = new SimpleCursorAdapter(this,
				R.layout.sleep_row, c, from, to);
		setListAdapter(sleep_rows);

	}

	/* adds the action to the database and listadapter */
	private void logAction(Calendar rightNow, String action) {
		dbHelper.createRow(getDateString(rightNow), getTimeString(rightNow),
				action);
	}

	private void addRow(String text) {
		/* FIXME: add to the row */
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		adapter.add(text);
		setListAdapter(adapter);
	}

	/* gets a handle to the database, creating if necessary */
	private void openDatabase() {
		try {
			dbHelper.open();
		} catch (SQLException e) {
			new AlertDialog.Builder(this)
				.setCancelable(false)
				.setTitle("Error!")
				.setMessage("Couldn't open database. Quitting.")
				.setNeutralButton("Quit", null).show();
			this.finish();
		}

	}

	private void closeDatabase() {
		dbHelper.close();
	}

	/* returns date in SQL string format */
	public String getDateString(Calendar now) {
		Date sqlDate = new Date(now.getTimeInMillis());
		return sqlDate.toString();
	}

	/* returns time in string format */
	public String getTimeString(Calendar now) {
		String strTime = new StringBuilder().append(
				pad(now.get(Calendar.HOUR_OF_DAY))).append(":").append(
				pad(now.get(Calendar.MINUTE))).toString();

		return strTime;
	}

	/* pads time results to two digits */
	private static String pad(int c) {
		if (c >= 10)
			return String.valueOf(c);
		else
			return "0" + String.valueOf(c);
	}

}