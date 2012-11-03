package be.lukin.android.babble;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

public abstract class SubActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			//Intent intent = new Intent(this, BabbleActivity.class);
			//intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			//startActivity(intent);
			// In our simple app, "home" always behaves like BACK, so we use finish() here.
			// Launching the home-activity with startActivity() would cause onCreate,
			// which we want to avoid because the activity state would get lost.
			// TODO: this is a temporary solution
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	protected void toast(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
	}

}