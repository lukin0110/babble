package be.lukin.android.babble;

import be.lukin.android.babble.provider.Phrase;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;

public class PhrasesActivity extends SubActivity {

	private static final String SORT_ORDER_TEXT = Phrase.Columns.TEXT + " ASC";
	private static final String SORT_ORDER_LANG = Phrase.Columns.LANG + " ASC";
	private static final String SORT_ORDER_DIST = Phrase.Columns.DIST + " DESC";
	private static final String SORT_ORDER_TIMESTAMP = Phrase.Columns.TIMESTAMP + " DESC";

	private static final int TTS_DATA_CHECK_CODE = 1;
	private TextToSpeech mTts;

	private SharedPreferences mPrefs;
	private static String mCurrentSortOrder;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.phrases);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		Intent checkIntent = new Intent();
		checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkIntent, TTS_DATA_CHECK_CODE);
	}


	@Override
	public void onStop() {
		super.onStop();
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString(getString(R.string.prefCurrentSortOrder), mCurrentSortOrder);
		editor.commit();
	}


	@Override
	public void onDestroy() {
		super.onDestroy();

		// Stop TTS
		if (mTts != null) {
			mTts.shutdown();
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.phrases, menu);

		// Indicate the current sort order by checking the corresponding radio button
		int id = mPrefs.getInt(getString(R.string.prefCurrentSortOrderMenu), R.id.menuMainSortByTimestamp);
		MenuItem menuItem = menu.findItem(id);
		// TODO: check why null can happen
		if (menuItem != null) {
			menuItem.setChecked(true);
		}
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuMainSortByTimestamp:
			sort(item, SORT_ORDER_TIMESTAMP);
			return true;
		case R.id.menuMainSortByText:
			sort(item, SORT_ORDER_TEXT);
			return true;
		case R.id.menuMainSortByLang:
			sort(item, SORT_ORDER_LANG);
			return true;
		case R.id.menuMainSortByDist:
			sort(item, SORT_ORDER_DIST);
			return true;
		case R.id.menuLanguagesPlot:
			LanguagesBarChart lbc = new LanguagesBarChart();
			Intent intent = lbc.execute(this);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == TTS_DATA_CHECK_CODE) {
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				mTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
					@Override
					public void onInit(int status) {
						if (status == TextToSpeech.SUCCESS) {
						} else {
							// TODO: inform the user about the TTS problem
							Log.e(getString(R.string.errorTtsInitError));
						}
					}
				});
			} else {
				Intent installIntent = new Intent();
				installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installIntent);
			}
		}
	}


	public TextToSpeech getTts() {
		return mTts;
	}


	private void sort(MenuItem item, String sortOrder) {
		mCurrentSortOrder = sortOrder;
		CursorLoaderListFragment fragment = (CursorLoaderListFragment) getFragmentManager().findFragmentById(R.id.list);
		fragment.changeSortOrder(sortOrder);
		item.setChecked(true);
		// Save the ID of the selected item.
		// TODO: ideally this should be done in onDestory
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putInt(getString(R.string.prefCurrentSortOrderMenu), item.getItemId());
		editor.commit();
	}


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.cm_main, menu);
	}

}