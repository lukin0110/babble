package be.lukin.android.babble;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public class BabbleActivity extends AbstractRecognizerActivity {

	// Stop listening the user input after this period of milliseconds.
	// The input sentences are short and using the app should be snappy so
	// we don't want to spend too much on a single utterance.
	// TODO: maybe allow it to be configured in the settings
	public static final int LISTENING_TIMEOUT = 4000;

	private State mState = State.INIT;

	private Resources mRes;
	private SharedPreferences mPrefs;

	private MicButton mButtonMicrophone;

	private AudioCue mAudioCue;

	private SpeechRecognizer mSr;

	private LinearLayout mLlPhrase;
	private LinearLayout mLlMicrophone;
	private TextView mTvPhrase;
	private TextView mTvResult;
	private TextView mTvFeedback;
	private TextView mTvLang;
	private List<Sentence> mSentences;
	private Sentence mCurrentSentence;

	private static final Uri CONTENT_URI = Phrase.Columns.CONTENT_URI;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i("onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mRes = getResources();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		mLlPhrase = (LinearLayout) findViewById(R.id.llPhrase);
		mTvPhrase = (TextView) findViewById(R.id.tvPhrase);
		mTvResult = (TextView) findViewById(R.id.tvResult);
		mTvFeedback = (TextView) findViewById(R.id.tvFeedback);
		mTvLang = (TextView) findViewById(R.id.tvLang);
		mLlMicrophone = (LinearLayout) findViewById(R.id.llMicrophone);
		mButtonMicrophone = (MicButton) findViewById(R.id.buttonMicrophone);

		//mActionBar = getActionBar();
		//mActionBar.setHomeButtonEnabled(false);

		// When the activity is started nothing is displayed
		mLlPhrase.setVisibility(View.INVISIBLE);
		mLlMicrophone.setVisibility(View.INVISIBLE);
		mTvResult.setText("");
		mTvFeedback.setVisibility(View.INVISIBLE);

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


	private Intent createRecognizerIntent(String phrase, String lang) {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplicationContext().getPackageName());
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
		// We let the recognizer know what the user was instructed to say.
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, phrase);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
		if (mPrefs.getBoolean(getString(R.string.keyMaxOneResult), mRes.getBoolean(R.bool.defaultMaxOneResult))) { 
			intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
		}
		return intent;
	}


	private void setUpRecognizerGui(final SpeechRecognizer sr) {
		mButtonMicrophone.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mState == State.INIT || mState == State.ERROR) {
					mCurrentSentence = getSentence();
					listenSentence(sr, mCurrentSentence, true);
				} else if (mState == State.LISTENING) {
					sr.stopListening();
				}
			}
		});

		mLlPhrase.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mState == State.INIT || mState == State.ERROR) {
					if (mCurrentSentence != null) {
						listenSentence(sr, mCurrentSentence, false);
					}
				} else if (mState == State.LISTENING) {
					sr.stopListening();
				}
			}
		});
	}


	private void listenSentence(SpeechRecognizer sr, Sentence sent, boolean isStorePhrase) {
		setUiInput(sent);
		if (mAudioCue != null) {
			mAudioCue.playStartSoundAndSleep();
		}
		startListening(sr, sent.getValue(), sent.getLocale(), isStorePhrase);
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
		if (mSentences == null || mSentences.isEmpty()) {
			return new Sentence(1, "en", getString(R.string.examplePhrase), 2);
		}
		return mSentences.get(getRandom(mSentences.size()-1));
	}


	private int getRandom(int max) {
		return (int)(Math.random() * (max + 1));
	}


	private void setUiInput(Sentence sent) {
		mTvPhrase.setText(sent.getValue());
		mTvLang.setText(langLabel(sent.getLocale()));
		mLlPhrase.setVisibility(View.VISIBLE);
		mTvResult.setText("");
		mTvFeedback.setVisibility(View.INVISIBLE);
	}


	private void setUiResult(String langCode, String resultText, int dist) {
		mTvResult.setText(resultText);
		mTvFeedback.setText(getDistText(dist, langCode));
		mTvFeedback.setVisibility(View.VISIBLE);
	}


	private void setErrorMessage(int res) {
		mTvFeedback.setText(getString(res));
		mTvFeedback.setVisibility(View.VISIBLE);
	}


	private void setPartialResult(String[] results) {
		mTvResult.setText(TextUtils.join(" · ", results));
	}


	private String getDistText(int dist, String langCode) {
		if (dist == 0) {
			return getString(R.string.msgDist0);
		}
		if (dist < 10) {
			return getString(R.string.msgDist10);
		}
		return "Sorry, no speaker of " + langLabel(langCode) + " would understand you!";
	}


	private String langLabel(String langCode) {
		Locale l = new Locale(langCode);
		return l.getDisplayName(l) + " (" + langCode + ")";
	}


	private void startListening(final SpeechRecognizer sr, String phrase, String lang, final boolean isStorePhrase) {

		final String mPhrase = phrase;
		final String mLang = lang;
		Intent intentRecognizer = createRecognizerIntent(phrase, lang);

		final Runnable stopListening = new Runnable() {
			@Override
			public void run() {
				sr.stopListening();
			}
		};
		final Handler handler = new Handler();

		sr.setRecognitionListener(new RecognitionListener() {

			@Override
			public void onBeginningOfSpeech() {
				Log.i("onBeginningOfSpeech");
				mState = State.LISTENING;
			}

			@Override
			public void onBufferReceived(byte[] buffer) {
				//Log.i("onBufferReceived: " + buffer.length);
				// TODO maybe show buffer waveform
			}

			@Override
			public void onEndOfSpeech() {
				mState = State.TRANSCRIBING;
				handler.removeCallbacks(stopListening);
				mButtonMicrophone.setState(mState);
				if (mAudioCue != null) {
					mAudioCue.playStopSound();
				}
			}

			@Override
			public void onError(int error) {
				mState = State.ERROR;
				handler.removeCallbacks(stopListening);
				mButtonMicrophone.setState(mState);
				if (mAudioCue != null) {
					mAudioCue.playErrorSound();
				}
				switch (error) {
				case SpeechRecognizer.ERROR_AUDIO:
					setErrorMessage(R.string.errorResultAudioError);
					break;
				case SpeechRecognizer.ERROR_CLIENT:
					setErrorMessage(R.string.errorResultClientError);
					break;
				case SpeechRecognizer.ERROR_NETWORK:
					setErrorMessage(R.string.errorResultNetworkError);
					break;
				case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
					setErrorMessage(R.string.errorResultNetworkError);
					break;
				case SpeechRecognizer.ERROR_SERVER:
					setErrorMessage(R.string.errorResultServerError);
					break;
				case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
					setErrorMessage(R.string.errorResultServerError);
					break;
				case SpeechRecognizer.ERROR_NO_MATCH:
					setErrorMessage(R.string.errorResultNoMatch);
					break;
				case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
					setErrorMessage(R.string.errorResultNoMatch);
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
				Log.i("onEvent: " + eventType + " " + params);
				// TODO: no recognizer service seems to call this
			}

			@Override
			public void onPartialResults(Bundle partialResults) {
				// This is supported (only?) by Google Voice Search.
				// The following is Google-specific.
				Log.i("onPartialResults: keySet: " + partialResults.keySet());
				String[] results = partialResults.getStringArray("com.google.android.voicesearch.UNSUPPORTED_PARTIAL_RESULTS");
				//double[] resultsConfidence = partialResults.getDoubleArray("com.google.android.voicesearch.UNSUPPORTED_PARTIAL_RESULTS_CONFIDENCE");
				if (results != null) {
					setPartialResult(results);
				}
			}

			@Override
			public void onReadyForSpeech(Bundle params) {
				mState = State.RECORDING;
				mButtonMicrophone.setState(mState);
				handler.postDelayed(stopListening, LISTENING_TIMEOUT);
			}

			@Override
			public void onResults(Bundle results) {
				handler.removeCallbacks(stopListening);
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
					if (isStorePhrase) {
						addEntry(mPhrase, mLang, dist, result);
					}
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
			if (mPrefs.getBoolean(getString(R.string.keyDemoMode), mRes.getBoolean(R.bool.defaultDemoMode))) {

				Set<String> locales = mPrefs.getStringSet(
						getString(R.string.keyLanguages),
						new HashSet<String>(Arrays.asList(mRes.getStringArray(R.array.valuesLanguages))));
				return LangService.getDemoSentences(getApplicationContext(), locales);
			}
			return LangService.getSentences();
		}

		protected void onProgressUpdate(Integer... progress) {
			//setProgressPercent(progress[0]);
		}

		protected void onPostExecute(List<Sentence> result) {
			Log.i("DownloadSentencesTask: onPostExecute");
			mSentences = result;
			mLlMicrophone.setVisibility(View.VISIBLE);
			mLlMicrophone.setEnabled(true);
			mTvResult.setText(getString(R.string.stateInit));
		}
	}

}
