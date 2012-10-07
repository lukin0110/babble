package be.lukin.android.babble;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import be.lukin.android.babble.Constants.State;
import be.lukin.android.babble.backend.LangService;
import be.lukin.android.babble.backend.Sentence;
import be.lukin.android.babble.provider.Phrase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class BabbleActivity extends AbstractRecognizerActivity {

	// Stop listening the user input after this period of milliseconds.
	// The input sentences are short and using the app should be snappy so
	// we don't want to spend too much on a single utterance.
	// TODO: maybe allow it to be configured in the settings
	public static final int LISTENING_TIMEOUT = 3000;

	private State mState = State.INIT;

	private Resources mRes;
	private SharedPreferences mPrefs;

	private MicButton mButtonMicrophone;

	private AudioCue mAudioCue;

	//private ActionBar mActionBar;

	private SpeechRecognizer mSr;

	private TextView mTvPhrase;
	private TextView mTvResult;
	private TextView mTvScore;
	private List<Sentence> mSentences;

	private static final Uri CONTENT_URI = Phrase.Columns.CONTENT_URI;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mRes = getResources();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		mTvPhrase = (TextView) findViewById(R.id.tvPhrase);
		mTvResult = (TextView) findViewById(R.id.tvResult);
		mTvScore = (TextView) findViewById(R.id.tvScore);
		mButtonMicrophone = (MicButton) findViewById(R.id.buttonMicrophone);

		//mActionBar = getActionBar();
		//mActionBar.setHomeButtonEnabled(false);

		new DownloadSentencesTask().execute();
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
				setUpRecognizerGui(mSr);
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
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuMainPhrases:
			startActivity(new Intent(this, PhrasesActivity.class));
			return true;
		case R.id.menuLanguagesPlot:
			LanguagesBarChart lbc = new LanguagesBarChart();
			Intent intent = lbc.execute(this);
			startActivity(intent);
			return true;
		case R.id.menuMainSettings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
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


	private void setUpRecognizerGui(final SpeechRecognizer sr) {
		mButtonMicrophone.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mState == State.INIT || mState == State.ERROR) {
					Sentence sent = getSentence();
					setUiInput(sent.getValue());
					if (mAudioCue != null) {
						mAudioCue.playStartSoundAndSleep();
					}
					startListening(sr, sent.getValue(), sent.getLocale());
				}
				else if (mState == State.LISTENING) {
					sr.stopListening();
				} else {
					// TODO: bad state to press the button
				}
			}
		});

		setUiReady();
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


	private void addEntry(String text, String lang, int dist, String result) {
		Time now = new Time();
		now.setToNow();
		long timestamp = now.toMillis(false);
		ContentValues values = new ContentValues();
		values.put(Phrase.Columns.TIMESTAMP, timestamp);
		values.put(Phrase.Columns.TEXT, text);
		values.put(Phrase.Columns.LANG, lang);
		values.put(Phrase.Columns.DIST, dist);
		values.put(Phrase.Columns.RESULT, result);
		insert(CONTENT_URI, values);
	}


	private Sentence getSentence() {
		if (mSentences == null) {
			return new Sentence(1, "en", "I like Android", 2);
		}
		return mSentences.get(getRandom(mSentences.size()-1));
	}


	private int getRandom(int max) {
		return (int)(Math.random() * (max + 1));
	}


	private void setUiReady() {
		LinearLayout llMicrophone = (LinearLayout) findViewById(R.id.llMicrophone);
		llMicrophone.setVisibility(View.VISIBLE);
		llMicrophone.setEnabled(true);
		mTvPhrase.setText("");
		mTvResult.setText(getString(R.string.stateInit));
		mTvScore.setVisibility(View.GONE);
	}


	private void setUiInput(String text) {
		mTvPhrase.setText(text);
		mTvResult.setText("");
		mTvScore.setVisibility(View.GONE);
	}


	private void setUiResult(String langCode, String resultText, int dist) {
		mTvResult.setText(resultText);
		Locale l = new Locale(langCode);
		String distText = "Sorry, but no native speaker of " + l.getDisplayName(l) + " (" + langCode + ") would understand you!";
		if (dist == 0) {
			distText = "Perfect!";
		} else if (dist < 10) {
			distText = "Pretty good";
		}
		mTvScore.setText(distText);
		mTvScore.setVisibility(View.VISIBLE);
	}


	private void startListening(final SpeechRecognizer sr, String phrase, String lang) {

		final String mPhrase = phrase;
		final String mLang = lang;
		Intent intentRecognizer = createRecognizerIntent(lang);

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
				setUiReady();
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
				Runnable stopListening = new Runnable() {
					@Override
					public void run() {
						sr.stopListening();
					}
				};
				new Handler().postDelayed(stopListening, LISTENING_TIMEOUT);
			}

			@Override
			public void onResults(Bundle results) {
				ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
				mState = State.INIT;
				mButtonMicrophone.setState(mState);
				if (matches.isEmpty()) {
					toast("ERROR: No results"); // TODO
				} else {
					// TODO: we just take the first result for the time being
					// TODO: confidence scores support is in API 14
					String result = matches.iterator().next();
					int dist = Utils.phraseDistance(mPhrase, result);
					setUiResult(mLang, result, dist);
					addEntry(mPhrase, mLang, dist, result);
				}
			}

			@Override
			public void onRmsChanged(float rmsdB) {
				mButtonMicrophone.setVolumeLevel(rmsdB);
			}
		});
		sr.startListening(intentRecognizer);
	}


	private class DownloadSentencesTask extends AsyncTask<Void, Integer, List<Sentence>> {
		protected List<Sentence> doInBackground(Void... arg0) {
			return LangService.getDemoSentences();
		}

		protected void onProgressUpdate(Integer... progress) {
			//setProgressPercent(progress[0]);
		}

		protected void onPostExecute(List<Sentence> result) {
			mSentences = result;
			setUiReady();
		}
	}

}
