package be.lukin.android.lang.provider;

import android.net.Uri;

public class Phrase {

	public Phrase() {
	}

	public static final class Columns extends BaseColumnsImpl {
		private Columns() {
		}

		public static final Uri CONTENT_URI = makeContentUri(LangContentProvider.PHRASE_TABLE_NAME);

		// Text of the phrase
		public static final String TEXT = "TEXT";
		// Language of the phrase
		public static final String LANG = "LANG";
		// Distance from the ASR result
		public static final String DIST = "DIST";
		// ASR result
		public static final String RESULT = "RESULT";
	}
}