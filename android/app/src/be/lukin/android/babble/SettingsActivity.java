package be.lukin.android.babble;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceManager;
import android.speech.RecognitionService;

public class SettingsActivity extends SubActivity implements OnSharedPreferenceChangeListener {

	private SettingsFragment mSettingsFragment;
	private SharedPreferences mPrefs;
	private String mKeyService;
	private String mKeyLanguages;

	// TODO: we support one service per package, this might
	// be a limitation...
	private final Map<String, String> mPkgToCls = new HashMap<String, String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSettingsFragment = new SettingsFragment();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mKeyService = getString(R.string.keyService);
		mKeyLanguages = getString(R.string.keyLanguages);

		// Display the fragment as the main content.
		getFragmentManager().beginTransaction().replace(android.R.id.content, mSettingsFragment).commit();
	}


	@Override
	protected void onResume() {
		super.onResume();
		mPrefs.registerOnSharedPreferenceChangeListener(this);

		populateServices();
	}


	@Override
	protected void onPause() {
		super.onPause();
		mPrefs.unregisterOnSharedPreferenceChangeListener(this);

		// Save the selected service class name, otherwise we cannot construct the
		//recognizer.
		String pkg = mPrefs.getString(getString(R.string.keyService), null);
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString(getString(R.string.prefRecognizerServiceCls), mPkgToCls.get(pkg));
		editor.commit();
	}


	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (key.equals(mKeyService)) {
			ListPreference pref = (ListPreference) mSettingsFragment.findPreference(key);
			pref.setSummary(pref.getEntry());
		} else if (key.equals(mKeyLanguages)) {
			// TODO: show the number of selected languages
			// MultiSelectListPreference pref = (MultiSelectListPreference) mSettingsFragment.findPreference(key);
			// pref.setSummary(pref.getEntryValues().length + " languages");
		}
	}


	private void populateServices() {
		PackageManager pm = getPackageManager();
		List<ResolveInfo> services = pm.queryIntentServices(
				new Intent(RecognitionService.SERVICE_INTERFACE), 0);

		String selectedService = mPrefs.getString(mKeyService, null);
		int selectedIndex = 0;

		CharSequence[] entries = new CharSequence[services.size()];
		CharSequence[] entryValues = new CharSequence[services.size()];

		int index = 0;
		for (ResolveInfo ri : services) {
			ServiceInfo si = ri.serviceInfo;
			if (si == null) {
				Log.i("serviceInfo == null");
				continue;
			}
			String pkg = si.packageName;
			String cls = si.name;
			CharSequence label = si.loadLabel(pm);
			mPkgToCls.put(pkg, cls);
			Log.i(pkg + " :: " + label + " :: " + mPkgToCls.get(pkg));
			entries[index] = label;
			entryValues[index] = pkg;
			if (pkg.equals(selectedService)) {
				selectedIndex = index;
			}
			index++;
		}

		ListPreference list = (ListPreference) mSettingsFragment.findPreference(mKeyService);
		list.setEntries(entries);
		list.setEntryValues(entryValues);
		list.setValueIndex(selectedIndex);
		list.setSummary(list.getEntry());
	}
}