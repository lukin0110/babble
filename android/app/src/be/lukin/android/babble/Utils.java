package be.lukin.android.babble;

import java.util.HashMap;
import java.util.Map;

import be.lukin.android.babble.provider.Phrase;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.text.SpannableString;
import android.text.util.Linkify;

public class Utils {

	private Utils() {}

	private static final String PUNCT = "\\p{Punct}";
	private static final String SPACE = "\\p{Space}";

	// TODO: scale by string lengths
	public static int phraseDistance(String s1, String s2) {
		s1 = s1.toLowerCase().replaceAll(SPACE, "").replaceAll(PUNCT, "");
		s2 = s2.toLowerCase().replaceAll(SPACE, "").replaceAll(PUNCT, "");
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


	// TODO: add restriction by timestamp
	// TODO: SELECT Lang,AVG(Dist) FROM Babble GROUP BY Lang
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
			double avg = langToDist.get(lang) / langToCount.get(lang);
			langToDist.put(lang, avg);
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
}