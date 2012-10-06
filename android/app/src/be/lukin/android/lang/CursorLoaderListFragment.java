package be.lukin.android.lang;

import be.lukin.android.lang.provider.Phrase;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SimpleCursorAdapter;

public class CursorLoaderListFragment extends ListFragment implements OnQueryTextListener, LoaderManager.LoaderCallbacks<Cursor> {

	// This is the Adapter being used to display the list's data.
	SimpleCursorAdapter mAdapter;

	// If non-null, this is the current filter the user has provided.
	String mCurFilter;

	private static final String SORT_ORDER_TEXT = Phrase.Columns.TEXT + " ASC";
	//private static final String SORT_ORDER_TIMESTAMP = Phrase.Columns.TEXT + " ASC";

	private static final String[] mColumns = new String[] {
		Phrase.Columns._ID,
		Phrase.Columns.TEXT,
		Phrase.Columns.LANG,
		Phrase.Columns.DIST,
		Phrase.Columns.RESULT
	};

	@Override public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		int[] to = new int[] {
				R.id.list_item_id,
				R.id.list_item_text,
				R.id.list_item_lang,
				R.id.list_item_dist,
				R.id.list_item_result
		};

		// Give some text to display if there is no data.
		setEmptyText(getString(R.string.emptylistPhrases));

		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		// Create an empty adapter we will use to display the loaded data.
		mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.list_item, null, mColumns, to, 0);
		setListAdapter(mAdapter);

		// Start out with a progress indicator.
		setListShown(false);

		// Prepare the loader.  Either re-connect with an existing one,
		// or start a new one.
		getLoaderManager().initLoader(0, null, this);
	}

	@Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Place an action bar item for searching.
		MenuItem item = menu.add("Search");
		item.setIcon(android.R.drawable.ic_menu_search);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
				| MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
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

	@Override public boolean onQueryTextSubmit(String query) {
		// Don't care about this.
		return true;
	}

	@Override public void onListItemClick(ListView l, View v, int position, long id) {
		// Insert desired behavior here.
		Log.i("FragmentComplexList", "Item clicked: " + id);
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

		return new CursorLoader(getActivity(), baseUri, mColumns, null, null, SORT_ORDER_TEXT);
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

		// TODO: display the number of phrases on the action bar
		/*
		mActionBar.setSubtitle(
				mRes.getQuantityString(R.plurals.numberOfInputs, count, count));
		 */
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		// This is called when the last Cursor provided to onLoadFinished()
		// above is about to be closed.  We need to make sure we are no
		// longer using it.
		mAdapter.swapCursor(null);
	}
}