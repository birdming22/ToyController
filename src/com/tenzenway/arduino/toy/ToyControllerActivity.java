package com.tenzenway.arduino.toy;

import com.tenzenway.arduino.toy.MainView;
import com.tenzenway.arduino.toy.R;
import com.tenzenway.arduino.toy.MainView.ColValues;
import com.tenzenway.arduino.toy.MainView.JoystickListener;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class ToyControllerActivity extends Activity {
	private static final String TAG = "SpokaLightBluetooth_MainActivity";

	public boolean updateColours(int redPerc, int bluePerc, int orangePerc) {
		byte[] bytes = new byte[1];

		if (redPerc > bluePerc) {
			bytes[0] = 'I';
		} else {
			bytes[0] = 'O';
		}

		return false;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// register this activity with the View, to listen to Joystick events
		((MainView) findViewById(R.id.myMainView))
				.addJoystickListener(new JoystickListener() {
					@Override
					public void onMove(ColValues newColours) {
						final String result = updateColours(newColours.red,
								newColours.blue, newColours.orange) ? "OK"
								: "NOK";

						Log.d(TAG, "Update Colours " + result + " ("
								+ newColours + ")");
					}
				});
	}
}