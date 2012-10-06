package be.lukin.android.lang;

import java.util.HashMap;
import java.util.Map;

import be.lukin.android.lang.provider.Phrase;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.text.SpannableString;
import android.text.util.Linkify;

public class Utils {

	private Utils() {}


	// TODO: delete punctuation and spaces
	// TODO: scale by string lengths
	public static int phraseDistance(String s1, String s2) {
		s1 = s1.toLowerCase();
		s2 = s2.toLowerCase();
		return computeDistance(s1, s2);
	}


	/**
	 * Originates from: http://rosettacode.org/wiki/Levenshtein_distance#Java
	 */
	private static int computeDistance(String s1, String s2) {
		int[] costs = new int[s2.length() + 1];
		for (int i = 0; i <= s1.length(); i++) {
			int lastValue = i;
			for (int j = 0; j <= s2.length(); j++) {
				if (i == 0)
					costs[j] = j;
				else {
					if (j > 0) {
						int newValue = costs[j - 1];
						if (s1.charAt(i - 1) != s2.charAt(j - 1))
							newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
						costs[j - 1] = lastValue;
						lastValue = newValue;
					}
				}
			}
			if (i > 0)
				costs[s2.length()] = lastValue;
		}
		return costs[s2.length()];
	}


	/**
	 * TODO: should we immediately return null if id = 0?
	 */
	public static String idToValue(Context context, Uri contentUri, String columnId, String columnUrl, long id) {
		String value = null;
		Cursor c = context.getContentResolver().query(
				contentUri,
				new String[] { columnUrl },
				columnId + "= ?",
				new String[] { String.valueOf(id) },
				null);

		if (c.moveToFirst()) {
			value = c.getString(0);
		}
		c.close();
		return value;
	}


	// TODO: add restriction by timestamp
	public static Map<String, Double> getLangToDist(Context context) {
		Map<String, Double> langToDist = new HashMap<String, Double>();
		Map<String, Integer> langToCount = new HashMap<String, Integer>();
		Cursor c = context.getContentResolver().query(
				Phrase.Columns.CONTENT_URI,
				new String[] { Phrase.Columns.LANG, Phrase.Columns.DIST },
				null,
				null,
				null);
		while (c.moveToNext()) {
			String lang = c.getString(0);
			Integer dist = c.getInt(1);
			Double val = langToDist.get(lang);
			if (val == null) {
				val = (double) dist;
				langToCount.put(lang, 1);
			} else {
				val += dist;
				langToCount.put(lang, 1 + langToCount.get(lang));
			}
			langToDist.put(lang, val);
		}
		c.close();

		for (String lang : langToDist.keySet()) {
			langToDist.put(lang, langToDist.get(lang) / langToCount.get(lang));
		}
		return langToDist;
	}


	public static AlertDialog getOkDialog(final Context context, String msg) {
		final SpannableString s = new SpannableString(msg);
		Linkify.addLinks(s, Linkify.ALL);
		return new AlertDialog.Builder(context)
		.setPositiveButton(context.getString(R.string.buttonOk), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		})
		.setMessage(s)
		.create();
	}


	public static AlertDialog getYesNoDialog(Context context, String confirmationMessage, final Executable ex) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder
		.setMessage(confirmationMessage)
		.setCancelable(false)
		.setPositiveButton(context.getString(R.string.buttonYes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				ex.execute();
			}
		})
		.setNegativeButton(context.getString(R.string.buttonNo), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		return builder.create();
	}


	public static AlertDialog getGoToStoreDialog(final Context context, String msg, final Uri uri) {
		final SpannableString s = new SpannableString(msg);
		Linkify.addLinks(s, Linkify.ALL);
		return new AlertDialog.Builder(context)
		.setPositiveButton(context.getString(R.string.buttonGoToStore), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
			}
		})
		.setNegativeButton(context.getString(R.string.buttonCancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		})
		.setMessage(s)
		.create();
	}


	public static String getVersionName(Context c) {
		PackageInfo info = getPackageInfo(c);
		if (info == null) {
			return "?.?.?";
		}
		return info.versionName;
	}


	private static PackageInfo getPackageInfo(Context c) {
		PackageManager manager = c.getPackageManager();
		try {
			return manager.getPackageInfo(c.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			Log.e("Couldn't find package information in PackageManager: " + e);
		}
		return null;
	}
}