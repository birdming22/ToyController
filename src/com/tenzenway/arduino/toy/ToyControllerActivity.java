package com.tenzenway.arduino.toy;

import java.util.LinkedList;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.LineAndPointRenderer;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.tenzenway.arduino.toy.MainView;
import com.tenzenway.arduino.toy.R;
import com.tenzenway.arduino.toy.MainView.ColValues;
import com.tenzenway.arduino.toy.MainView.JoystickListener;
import com.tenzenway.arduino.toy.SerialAdapter;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ToyControllerActivity extends Activity {
	private static final String TAG = "SpokaLightBluetooth_MainActivity";
	
	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	private TextView _title;
	private BluetoothAdapter _bluetoothAdapter;
	private SerialAdapter _serialAdapter;
	private boolean _connectDevice = false;
	private SimpleXYSeries rollHistorySeries = null;
	private LinkedList<Number> rollHistory;
	private XYPlot aprHistoryPlot = null;
	private int dataCount = 0;

	public boolean updateColours(int redPerc, int bluePerc, int orangePerc) {
		byte[] bytes = new byte[5];
		bytes[0] = (byte)0x81;
		bytes[1] = (byte)3;
		int R = redPerc*256/100;
		int G = orangePerc*256/100;
		int B = bluePerc*256/100;
		if (R < 0) R = 0;
		if (G < 0) G = 0;
		if (B < 0) B = 0;
		if (R > 255) R = 255;
		if (G > 255) G = 255;
		if (B > 255) B = 255;
		bytes[2] = (byte)R;
		bytes[3] = (byte)G;
		bytes[4] = (byte)B;

		if (_serialAdapter != null) {
			_serialAdapter.sendBytes(bytes);
			return true;
		} else {
			return false;
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_title);

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

		// Set up the custom title
		_title = (TextView) findViewById(R.id.title_left_text);
		_title.setText(R.string.app_name);
		_title = (TextView) findViewById(R.id.title_right_text);

		// Get local Bluetooth adapter
		_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (_bluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		rollHistory = new LinkedList<Number>();
		rollHistorySeries = new SimpleXYSeries("Time");
		// setup the APR History plot:
		aprHistoryPlot = (XYPlot) findViewById(R.id.aprHistoryPlot);
		aprHistoryPlot.setRangeBoundaries(0, Constant.SENSING_LEVEL,
				BoundaryMode.FIXED);
		aprHistoryPlot.setDomainBoundaries(0, Constant.DOMAIN_BOUNDARY,
				BoundaryMode.FIXED);
		aprHistoryPlot.addSeries(rollHistorySeries, LineAndPointRenderer.class,
				new LineAndPointFormatter(Color.rgb(200, 100, 100),
						Color.BLACK, null));
		aprHistoryPlot.setDomainStepValue(5);
		aprHistoryPlot.setTicksPerRangeLabel(3);
		aprHistoryPlot.setDomainLabel("Sample Index");
		aprHistoryPlot.getDomainLabelWidget().pack();
		aprHistoryPlot.setRangeLabel("Angle (Degs)");
		aprHistoryPlot.getRangeLabelWidget().pack();
		aprHistoryPlot.disableAllMarkup();
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "++ ON START ++");

		if (!_bluetoothAdapter.isEnabled()) {
			Log.d(TAG, "BT is not on");

			// If BT is not on, request that it be enabled.
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			// Asynchronous, the onActivityResult will be called back when
			// finished
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		} else {
			Log.d(TAG, "BT is on");
			// Bluetooth is already enabled
			// Launch the BTDeviceListActivity to see devices and do scan
			if (!_connectDevice) {
				Intent serverIntent = new Intent(this,
						BTDeviceListActivity.class);
				startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "++ ON DESTROY ++");
		if (_serialAdapter != null)
			_serialAdapter.disconnect();
	}
	public static final double[] forwardMagnitude(double[] input) {
		int N = input.length;
		double[] mag = new double[N];
		double[] c = new double[N];
		double[] s = new double[N];
		double twoPi = 2*Math.PI;
		
		for(int i=0; i<N; i++) {
			for(int j=0; j<N; j++) {
				c[i] += input[j]*Math.cos(i*j*twoPi/N);
				s[i] -= input[j]*Math.sin(i*j*twoPi/N);
			}
			c[i]/=N;
			s[i]/=N;
			
			mag[i]=Math.sqrt(c[i]*c[i]+s[i]*s[i]);
		}
		
		return mag;
	}
	private final Handler _handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				int[] sensorData = (int[]) msg.obj;
				
				if (rollHistory.size() > Constant.DOMAIN_BOUNDARY) {
					for (int i = 0; i < 8; i++)
						rollHistory.removeFirst();
				}
				
				for (int i=0; i< Constant.DATA_SIZE; i++) {
					int value = sensorData[i];
					rollHistory.addLast(value);
				}
				
				dataCount += 8;
				rollHistorySeries.setModel(rollHistory,
						SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
				aprHistoryPlot.redraw();
				break;
			default:
				System.out.println("msg.what not 0!");
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult " + resultCode);

		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When BTDeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(
						BTDeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Create a business object which attempts to create to the
				// Spoka !
				// ensureDiscoverable(_bluetoothAdapter);
				try {
					Log.d(TAG, "address:" + address);

					_serialAdapter = new SerialAdapter(_bluetoothAdapter, address, _handler);
					_connectDevice = true;
				} catch (Exception e) {
					Toast.makeText(this, "Can't connect to the SPOKA",
							Toast.LENGTH_SHORT).show();
					finish();
				}
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled
				// Launch the BTDeviceListActivity to see devices and do scan
				Intent serverIntent = new Intent(this,
						BTDeviceListActivity.class);
				startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this,
						"User did not enable Bluetooth or an error occured",
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	private void ensureDiscoverable(BluetoothAdapter bluetoothAdapter) {
		if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Log.d(TAG, "Force discoverable");
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}
}