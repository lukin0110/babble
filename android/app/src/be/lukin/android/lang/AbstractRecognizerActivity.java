package be.lukin.android.lang;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Toast;

public abstract class AbstractRecognizerActivity extends Activity {

	public String getVersionName() {
		return Utils.getVersionName(this);
	}


	protected List<ResolveInfo> getIntentActivities(Intent intent) {
		PackageManager pm = getPackageManager();
		List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
		return activities;
	}


	protected void toast(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
	}


	public void showErrorDialog(int msg) {
		new AlertDialog.Builder(this)
		.setPositiveButton(getString(R.string.buttonOk), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		})
		.setTitle(R.string.error)
		.setMessage(msg)
		.create()
		.show();
	}
}