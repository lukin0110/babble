package be.lukin.android.babble;

import java.util.HashMap;
import java.util.Locale;

import be.lukin.android.babble.provider.Phrase;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SimpleCursorAdapter;

public class CursorLoaderListFragment extends ListFragment implements OnQueryTextListener, LoaderManager.LoaderCallbacks<Cursor> {

	// This is the Adapter being used to display the list's data.
	SimpleCursorAdapter mAdapter;

	// If non-null, this is the current filter the user has provided.
	String mCurFilter;

	private String mCurrentSortOrder;

	private static final String UTT_COMPLETED_FEEDBACK = "UTT_COMPLETED_FEEDBACK";

	private static final String[] mColumns = new String[] {
		Phrase.Columns._ID,
		Phrase.Columns.TIMESTAMP,
		Phrase.Columns.TEXT,
		Phrase.Columns.LANG,
		Phrase.Columns.DIST,
		Phrase.Columns.RESULT
	};

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		int[] to = new int[] {
				R.id.list_item_id,
				R.id.list_item_timestamp,
				R.id.list_item_text,
				R.id.list_item_lang,
				R.id.list_item_dist,
				R.id.list_item_result
		};

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mCurrentSortOrder = prefs.getString(getString(R.string.prefCurrentSortOrder), Phrase.Columns.TIMESTAMP + " DESC");

		// Give some text to display if there is no data.
		setEmptyText(getString(R.string.emptylistPhrases));

		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		// Create an empty adapter we will use to display the loaded data.
		mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.list_item, null, mColumns, to, 0);
		setListAdapter(mAdapter);

		// Start out with a progress indicator.
		setListShown(false);

		getListView().setFastScrollEnabled(true);

		// Prepare the loader.  Either re-connect with an existing one,
		// or start a new one.
		getLoaderManager().initLoader(0, null, this);
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Place an action bar item for searching.
		MenuItem item = menu.add("Search");
		item.setIcon(android.R.drawable.ic_menu_search);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		SearchView sv = new SearchView(getActivity());
		sv.setOnQueryTextListener(this);
		item.setActionView(sv);
	}

	public boolean onQueryTextChange(String newText) {
		// Called when the action bar search text has changed.  Update
		// the search filter, and restart the loader to do a new query
		// with this filter.
		String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
		// Don't do anything if the filter hasn't actually changed.
		// Prevents restarting the loader when restoring state.
		if (mCurFilter == null && newFilter == null) {
			return true;
		}
		if (mCurFilter != null && mCurFilter.equals(newFilter)) {
			return true;
		}
		mCurFilter = newFilter;
		getLoaderManager().restartLoader(0, null, this);
		return true;
	}


	public boolean changeSortOrder(String sortOrder) {
		mCurrentSortOrder = sortOrder;
		getLoaderManager().restartLoader(0, null, this);
		return true;
	}

	@Override public boolean onQueryTextSubmit(String query) {
		// Don't care about this.
		return true;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// TODO: launch the home activity to practice this phrase
		// or read it with speech synthesizer
		Cursor cursor = (Cursor) getListView().getItemAtPosition(position);
		String text = cursor.getString(cursor.getColumnIndex(Phrase.Columns.TEXT));
		String lang = cursor.getString(cursor.getColumnIndex(Phrase.Columns.LANG));
		Log.i("Item clicked: " + text);
		say(text, lang);
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		// This is called when a new Loader needs to be created.  This
		// sample only has one Loader, so we don't care about the ID.
		// First, pick the base URI to use depending on whether we are
		// currently filtering.
		Uri baseUri;
		if (mCurFilter != null) {
			//baseUri = Uri.withAppendedPath(Phrase.Columns.CONTENT_FILTER_URI, Uri.encode(mCurFilter));
			baseUri = Phrase.Columns.CONTENT_URI;
		} else {
			baseUri = Phrase.Columns.CONTENT_URI;
		}

		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.

		return new CursorLoader(getActivity(), baseUri, mColumns, null, null, mCurrentSortOrder);
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// Swap the new cursor in.  (The framework will take care of closing the
		// old cursor once we return.)
		mAdapter.swapCursor(data);

		// The list should now be shown.
		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		// This is called when the last Cursor provided to onLoadFinished()
		// above is about to be closed.  We need to make sure we are no
		// longer using it.
		mAdapter.swapCursor(null);
	}


	@SuppressWarnings("deprecation")
	private void say(String text, String lang) {
		PhrasesActivity activity = (PhrasesActivity) getActivity();
		TextToSpeech tts = activity.getTts();
		if (tts != null) {
			// TODO: maybe skip this step if the language has not changed
			boolean success = setTtsLang(tts, lang);
			if (success) {
				tts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
					@Override
					public void onUtteranceCompleted(String utteranceId) {
						Log.i("onUtteranceCompleted: " + utteranceId);
						if (utteranceId.equals(UTT_COMPLETED_FEEDBACK)) {
							// TODO: maybe do something
						}
					}
				});
				HashMap<String, String> params = new HashMap<String, String>();
				params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTT_COMPLETED_FEEDBACK);
				tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);
			} else {
				Toast.makeText(getActivity(), String.format(getString(R.string.errorTtsLangNotAvailable), lang),
						Toast.LENGTH_SHORT).show();
			}
		}
	}


	private static boolean setTtsLang(TextToSpeech tts, String localeAsStr) {
		Log.i("Default TTS engine:" + tts.getDefaultEngine());
		Locale locale = new Locale(localeAsStr);
		if (tts.isLanguageAvailable(locale) >= 0) {
			tts.setLanguage(locale);
			Log.i("Set TTS to locale: " + locale);
			return true;
		}
		return false;
	}
}