package be.lukin.android.lang.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class BaseColumnsImpl implements BaseColumns {

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.be.lukin.android.lang";

	public static Uri makeContentUri(String name) {
		return Uri.parse("content://" + LangContentProvider.AUTHORITY + "/" + name);
	}

}