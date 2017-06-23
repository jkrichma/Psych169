package abr.auto;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;

import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.IOIOLooperProvider;
import ioio.lib.util.android.IOIOAndroidApplicationHelper;

public class Main_activity extends Activity implements IOIOLooperProvider, SensorEventListener        // implements IOIOLooperProvider: from IOIOActivity
{
	private final IOIOAndroidApplicationHelper helper_ = new IOIOAndroidApplicationHelper(this, this);			// from IOIOActivity
	private TextView irLeftText;
	private TextView irCenterText;
	private TextView irRightText;
	private ToggleButton btnStartStop;

	//variables for compass
	private SensorManager mSensorManager;
	private Sensor mCompass, mAccelerometer;
	float[] mAcc;
	//variables for logging
	private Sensor mGyroscope;
	private Sensor mGravityS;
	float[] mGravity;
	float[] mGyro;
	float[] mGeomagnetic;

	IOIO_thread_rover_tank m_ioio_thread;
	private TextView resultTEXT;

	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		resultTEXT = (TextView) findViewById(R.id.command);

		irLeftText = (TextView) findViewById(R.id.irLeft);
		irCenterText = (TextView) findViewById(R.id.irCenter);
		irRightText = (TextView) findViewById(R.id.irRight);
		btnStartStop = (ToggleButton) findViewById(R.id.buttonStartStop);

		//set up compass
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mCompass= mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mAccelerometer= mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		mGravityS = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

		helper_.create();		// from IOIOActivity

		// enableUi(false);

	}

	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do something here if sensor accuracy changes.
	}

	// Called whenever the value of a sensor changes
	// Should be called whenever a sensor changes.
	// Good time to get IR values and send move commands to the robot

	public static boolean on = false;
	public static boolean popup = false;
	public static boolean returnBool(){
		return popup;
	}
	public static boolean returnONorOff(){
		return on;
	}

	@Override
	public final void onSensorChanged(SensorEvent event) {
		popup = false;
		if(m_ioio_thread != null){
			setText(String.format("%.3f", m_ioio_thread.getIrLeftReading()), irLeftText);
			setText(String.format("%.3f", m_ioio_thread.getIrCenterReading()), irCenterText);
			setText(String.format("%.3f", m_ioio_thread.getIrRightReading()), irRightText);

			System.out.println("IS IT CHECKED"+btnStartStop.isChecked());

			if(btnStartStop.isChecked()) {
				on = true;
				m_ioio_thread.move(0.2f, 0.2f, true, true);

				if (resultTEXT.getText().toString().equals("stay") ) {
					System.out.println("STAYING");
					m_ioio_thread.move(0.0f, 0.0f, true, true);
				}
				 if (resultTEXT.getText().toString().equals("attack") ) {
					System.out.println("ATTACKKKK");
					m_ioio_thread.move(1f, 1f, true, true);
				}
				 if (resultTEXT.getText().toString().equals("slow down") ) {
					System.out.println("slow");
					m_ioio_thread.move(0.2f, 0.2f, true, true);
				}
				 if (resultTEXT.getText().toString().equals("turn right") ) {
					System.out.println("TURNNNNNN");
					m_ioio_thread.turn(1600);
				}
				 if (resultTEXT.getText().toString().equals("turn left") ) {
					System.out.println("TURNNNNNN");
					m_ioio_thread.turn(1000);
				}
				else{
					System.out.println("NOT HEARING OR PROMPTED ANYTHING SO SENSORS CAN DETECT");
				}

			}
			else {
				popup = true;
				on = false;
				System.out.println("FORCE STOP");
				m_ioio_thread.move(0.0f,0.0f,false,false);
			}

		}

	}

	private void enableUi(final boolean enable) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				btnStartStop.setEnabled(enable);
			}
		});
	}

	//set the text of any text view in this application
	public void setText(final String str, final TextView tv)
	{
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				tv.setText(str);
			}
		});
	}

	/*****************************************************Voice Recognition*************************************************************************/

	public void onButtonClick(View v){
		if(v.getId() == R.id.button) {
			//popup = true;
			promptSpeechInput();
		}
	}

	public void promptSpeechInput(){
		Intent i = new Intent(RecognizerIntent. ACTION_RECOGNIZE_SPEECH);
		i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		i.putExtra(RecognizerIntent.EXTRA_PROMPT, "SAY SOMETHING");

		try{
			startActivityForResult(i, 100);
		}
		catch(ActivityNotFoundException a) {
			Toast.makeText(Main_activity.this, "Sorry!", Toast.LENGTH_LONG).show();
		}

	}

	public void onActivityResult(int request_code, int result_code, Intent i) {
		super.onActivityResult(request_code , result_code , i);
		switch (request_code) {

			case 100:
				if (result_code == RESULT_OK && i != null) {
					ArrayList<String> result = i.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
					resultTEXT.setText(result.get(0));
				}
				break;
		}
	}


	/****************************************************** functions from IOIOActivity *********************************************************************************/


	@Override
	public IOIOLooper createIOIOLooper(String connectionType, Object extra) 
	{
		if(m_ioio_thread == null && connectionType.matches("ioio.lib.android.bluetooth.BluetoothIOIOConnection"))
		{
			// enableUi(true);
			m_ioio_thread = new IOIO_thread_rover_tank();
			return m_ioio_thread;
		}
		else
		{
			return null;
		}
	}

	//Called whenever activity resumes from pause
	@Override
	public void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, mCompass, SensorManager.SENSOR_DELAY_NORMAL);
		helper_.start();		// from IOIOActivity

	}

	//Called when activity pauses
	@Override
	public void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
		// Log.d("robo", "onPause");
	}

	//Called when activity restarts. onCreate() will then be called
	@Override
	public void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onDestroy()
	{
		helper_.destroy();
		super.onDestroy();
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		helper_.start();
	}

	@Override
	protected void onStop()
	{
		helper_.stop();
		super.onStop();
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
		{
			helper_.restart();
		}
	}
}
