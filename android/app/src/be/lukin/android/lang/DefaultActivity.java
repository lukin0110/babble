package be.lukin.android.lang;

import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CursorTreeAdapter;
import android.widget.LinearLayout;

import android.app.ActionBar;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.preference.PreferenceManager;
import be.lukin.android.lang.Constants.State;

import java.util.ArrayList;
import java.util.List;


public class DefaultActivity extends AbstractRecognizerActivity {

	private State mState = State.INIT;

	private Resources mRes;
	private SharedPreferences mPrefs;

	private static String mCurrentSortOrder;

	private MicButton mButtonMicrophone;

	private AudioCue mAudioCue;

	private ActionBar mActionBar;

	private SpeechRecognizer mSr;

	// TODO
	private static final String SORT_ORDER_TIMESTAMP = "";

	private final class QueryHandler extends AsyncQueryHandler {
		private CursorTreeAdapter mAdapter;

		public QueryHandler(Context context, CursorTreeAdapter adapter) {
			super(context.getContentResolver());
			this.mAdapter = adapter;
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			updateUi();
		}

		protected void onDeleteComplete(int token, Object cookie, int result) {
			updateUi();
		}

		protected void onInsertComplete(int token, Object cookie, Uri uri) {
			updateUi();
		}

		public void insert(Uri contentUri, ContentValues values) {
			startInsert(1, null, contentUri, values);
		}

		public void delete(Uri contentUri, long key) {
			Uri uri = ContentUris.withAppendedId(contentUri, key);
			startDelete(1, null, uri, null, null);
		}

		private void updateUi() {
			if (mActionBar != null) {
				int count = mAdapter.getGroupCount();
				mActionBar.setSubtitle(
						mRes.getQuantityString(R.plurals.numberOfInputs, count, count));
			}
		}
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mRes = getResources();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		mButtonMicrophone = (MicButton) findViewById(R.id.buttonMicrophone);

		//registerForContextMenu(mListView);

		mActionBar = getActionBar();
		mActionBar.setHomeButtonEnabled(false);

	}


	/**
	 * We initialize the speech recognizer here, assuming that the configuration
	 * changed after onStop. That is why onStop destroys the recognizer.
	 */
	@Override
	public void onStart() {
		super.onStart();

		if (mPrefs.getBoolean(getString(R.string.keyAudioCues), mRes.getBoolean(R.bool.defaultAudioCues))) {
			mAudioCue = new AudioCue(this);
		} else {
			mAudioCue = null;
		}

		ComponentName serviceComponent = getServiceComponent();

		if (serviceComponent == null) {
			toast(getString(R.string.errorNoDefaultRecognizer));
			//TODO: goToStore();
		} else {
			Log.i("Starting service: " + serviceComponent);
			mSr = SpeechRecognizer.createSpeechRecognizer(this, serviceComponent);
			if (mSr == null) {
				toast(getString(R.string.errorNoDefaultRecognizer));
			} else {
				Intent intentRecognizer = createRecognizerIntent(
						mPrefs.getString(getString(R.string.keyLanguage), getString(R.string.defaultLanguage)));
				setUpRecognizerGui(mSr, intentRecognizer);
			}
		}
	}


	@Override
	public void onResume() {
		super.onResume();
	}


	@Override
	public void onStop() {
		super.onStop();

		if (mSr != null) {
			mSr.cancel(); // TODO: do we need this, we do destroy anyway?
			mSr.destroy();
		}

		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString(getString(R.string.prefCurrentSortOrder), mCurrentSortOrder);
		editor.commit();
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mSr != null) {
			mSr.destroy();
			mSr = null;
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		// Indicate the current sort order by checking the corresponding radio button
		int id = mPrefs.getInt(getString(R.string.prefCurrentSortOrderMenu), R.id.menuMainSortByTimestamp);
		MenuItem menuItem = menu.findItem(id);
		menuItem.setChecked(true);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuMainSortByTimestamp:
			sort(item, SORT_ORDER_TIMESTAMP);
			return true;
		case R.id.menuMainSettings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	private void sort(MenuItem item, String sortOrder) {
		//startQuery(sortOrder);
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


	private Intent createRecognizerIntent(String langSource) {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplicationContext().getPackageName());
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, langSource);
		if (mPrefs.getBoolean(getString(R.string.keyMaxOneResult), mRes.getBoolean(R.bool.defaultMaxOneResult))) { 
			intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
		}
		return intent;
	}


	private void setUpRecognizerGui(final SpeechRecognizer sr, final Intent intentRecognizer) {
		mButtonMicrophone.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mState == State.INIT || mState == State.ERROR) {
					if (mAudioCue != null) {
						mAudioCue.playStartSoundAndSleep();
					}
					startListening(sr, intentRecognizer);
				}
				else if (mState == State.LISTENING) {
					sr.stopListening();
				} else {
					// TODO: bad state to press the button
				}
			}
		});

		LinearLayout llMicrophone = (LinearLayout) findViewById(R.id.llMicrophone);
		llMicrophone.setVisibility(View.VISIBLE);
		llMicrophone.setEnabled(true);
	}


	/**
	 * Look up the default recognizer service in the preferences.
	 * If the default have not been set then set the first available
	 * recognizer as the default. If no recognizer is installed then
	 * return null.
	 */
	private ComponentName getServiceComponent() {
		String pkg = mPrefs.getString(getString(R.string.keyService), null);
		String cls = mPrefs.getString(getString(R.string.prefRecognizerServiceCls), null);
		if (pkg == null || cls == null) {
			List<ResolveInfo> services = getPackageManager().queryIntentServices(
					new Intent(RecognitionService.SERVICE_INTERFACE), 0);
			if (services.isEmpty()) {
				return null;
			}
			ResolveInfo ri = services.iterator().next();
			pkg = ri.serviceInfo.packageName;
			cls = ri.serviceInfo.name;
			SharedPreferences.Editor editor = mPrefs.edit();
			editor.putString(getString(R.string.keyService), pkg);
			editor.putString(getString(R.string.prefRecognizerServiceCls), cls);
			editor.commit();
		}
		return new ComponentName(pkg, cls);
	}


	private void startListening(final SpeechRecognizer sr, Intent intentRecognizer) {

		sr.setRecognitionListener(new RecognitionListener() {

			@Override
			public void onBeginningOfSpeech() {
				mState = State.LISTENING;
			}

			@Override
			public void onBufferReceived(byte[] buffer) {
				// TODO maybe show buffer waveform
			}

			@Override
			public void onEndOfSpeech() {
				mState = State.TRANSCRIBING;
				mButtonMicrophone.setState(mState);
				if (mAudioCue != null) {
					mAudioCue.playStopSound();
				}
			}

			@Override
			public void onError(int error) {
				mState = State.ERROR;
				mButtonMicrophone.setState(mState);
				if (mAudioCue != null) {
					mAudioCue.playErrorSound();
				}
				switch (error) {
				case SpeechRecognizer.ERROR_AUDIO:
					showErrorDialog(R.string.errorResultAudioError);
					break;
				case SpeechRecognizer.ERROR_CLIENT:
					showErrorDialog(R.string.errorResultClientError);
					break;
				case SpeechRecognizer.ERROR_NETWORK:
					showErrorDialog(R.string.errorResultNetworkError);
					break;
				case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
					showErrorDialog(R.string.errorResultNetworkError);
					break;
				case SpeechRecognizer.ERROR_SERVER:
					showErrorDialog(R.string.errorResultServerError);
					break;
				case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
					showErrorDialog(R.string.errorResultServerError);
					break;
				case SpeechRecognizer.ERROR_NO_MATCH:
					showErrorDialog(R.string.errorResultNoMatch);
					break;
				case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
					showErrorDialog(R.string.errorResultNoMatch);
					break;
				case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
					// This is programmer error.
					break;
				default:
					break;
				}
			}

			@Override
			public void onEvent(int eventType, Bundle params) {
				// TODO ???
			}

			@Override
			public void onPartialResults(Bundle partialResults) {
				// ignore
			}

			@Override
			public void onReadyForSpeech(Bundle params) {
				mState = State.RECORDING;
				mButtonMicrophone.setState(mState);
			}

			@Override
			public void onResults(Bundle results) {
				ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
				// TODO: confidence scores support is only in API 14
				mState = State.INIT;
				mButtonMicrophone.setState(mState);
				toast(matches.toString()); // TODO: populate the list instead
			}

			@Override
			public void onRmsChanged(float rmsdB) {
				mButtonMicrophone.setVolumeLevel(rmsdB);
			}
		});
		sr.startListening(intentRecognizer);
	}

}
