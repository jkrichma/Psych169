// this file is using code by Foxdog Studios Ltd licensed under the Apache License, Version 2.0

package abr.auto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v7.widget.ActionBarOverlayLayout;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.apache.http.conn.util.InetAddressUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import ioio.lib.util.IOIOLooper;
import ioio.lib.util.IOIOLooperProvider;
import ioio.lib.util.android.IOIOAndroidApplicationHelper;
import android.hardware.Camera;

import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import static android.R.attr.left;
import static android.R.attr.right;
import static android.os.Build.VERSION_CODES.M;
import static android.support.test.espresso.core.deps.guava.primitives.Floats.max;

public class Main_activity extends Activity implements IOIOLooperProvider, SensorEventListener, SurfaceHolder.Callback        // implements IOIOLooperProvider: from IOIOActivity
{
	private final IOIOAndroidApplicationHelper helper_ = new IOIOAndroidApplicationHelper(this, this);			// from IOIOActivity
	private TextView irLeftText, irCenterText, irRightText, irBackText;
	private ToggleButton btnStartStop, btnTurn;

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
	//camera:
	private static final String TAG = Main_activity.class.getSimpleName();

	private static final String WAKE_LOCK_TAG = "peepers";

	private static final String PREF_CAMERA = "camera";
	private static final int PREF_CAMERA_INDEX_DEF = 0;
	private static final String PREF_FLASH_LIGHT = "flash_light";
	private static final boolean PREF_FLASH_LIGHT_DEF = false;
	private static final String PREF_PORT = "port";
	private static final int PREF_PORT_DEF = 8080;
	private static final String PREF_JPEG_SIZE = "size";
	private static final String PREF_JPEG_QUALITY = "jpeg_quality";
	private static final int PREF_JPEG_QUALITY_DEF = 40;
	// preview sizes will always have at least one element, so this is safe
	private static final int PREF_PREVIEW_SIZE_INDEX_DEF = 0;

	private boolean mRunning = false;
	private boolean mPreviewDisplayCreated = false;
	private SurfaceHolder mPreviewDisplay = null;
	private CameraStreamer mCameraStreamer = null;
	private String mIpAddress = "";
	private int mCameraIndex = PREF_CAMERA_INDEX_DEF;
	private boolean mUseFlashLight = PREF_FLASH_LIGHT_DEF;
	private int mPort = PREF_PORT_DEF;
	private int mJpegQuality = PREF_JPEG_QUALITY_DEF;
	private int mPrevieSizeIndex = PREF_PREVIEW_SIZE_INDEX_DEF;
	private TextView mIpAddressView = null;
	private LoadPreferencesTask mLoadPreferencesTask = null;
	private SharedPreferences mPrefs = null;
	private MenuItem mSettingsMenuItem = null;
	private WakeLock mWakeLock = null;

	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		irLeftText = (TextView) findViewById(R.id.irLeft);
		irCenterText = (TextView) findViewById(R.id.irCenter);
		irRightText = (TextView) findViewById(R.id.irRight);
		irBackText = (TextView) findViewById(R.id.irBack);
		btnStartStop = (ToggleButton) findViewById(R.id.buttonStartStop);

		//set up compass
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mCompass= mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mAccelerometer= mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		mGravityS = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

		helper_.create();		// from IOIOActivity

		//camera:
		mPreviewDisplay = ((SurfaceView) findViewById(R.id.camera)).getHolder();
		mPreviewDisplay.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mPreviewDisplay.addCallback(this);

		mIpAddress = tryGetIpV4Address();
		mIpAddressView = (TextView) findViewById(R.id.ip_address);
		updatePrefCacheAndUi();

		final PowerManager powerManager =
				(PowerManager) getSystemService(POWER_SERVICE);
		mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				WAKE_LOCK_TAG);
		//end camera

		// enableUi(false);

	}

	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do something here if sensor accuracy changes.
	}

	// Called whenever the value of a sensor changes
	// Should be called whenever a sensor changes.
	// Good time to get IR values and send move commands to the robot
	@Override
	public final void onSensorChanged(SensorEvent event) {
		if(m_ioio_thread != null){
			float left = m_ioio_thread.getIrLeftReading();
			float right = m_ioio_thread.getIrRightReading();
			setText(String.format("%.3f", left), irLeftText);
			setText(String.format("%.3f", m_ioio_thread.getIrCenterReading()), irCenterText);
			setText(String.format("%.3f", right), irRightText);
			setText(String.format("%.3f", m_ioio_thread.getIrBackReading()), irBackText);
			if (btnStartStop.isChecked()) {
				if (m_ioio_thread.getIrCenterReading()>= 0.6){
					//turn if object is in front
					m_ioio_thread.turn(1600);

				} else if (m_ioio_thread.getIrBackReading()>=0.7) {
					//stop moving is object is behind (e.g. to turn it off)
					m_ioio_thread.move(0.0f,0.0f,false, false);
				}
				else if (left>=0.6){
					//turn right if there is an object is on the right
					m_ioio_thread.turn(1600);
				}
				else if (right>=0.6){
					//turn left if there is an object on the left
					m_ioio_thread.turn(1400);
				}
				else {
					//slightly avoid objects
					m_ioio_thread.move(max(0.5f - right, 0.0f),max(0.5f - left, 0.0f),true,true);
				}
			}
			else {
				m_ioio_thread.move(0.0f,0.0f,false,false);
			}



		}

	}

	private void turn(){

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
		// Log.d("robo", "setText");
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				tv.setText(str);
			}
		});
	}

	/****************************************************** functions from IOIOActivity *********************************************************************************/

	/**
	 * Create the  {@link IOIO_thread_pwm}. Called by the {@link IOIOAndroidApplicationHelper}. <br>
	 * Function copied from original IOIOActivity.
	 * */
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
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mGravityS, SensorManager.SENSOR_DELAY_NORMAL);
		helper_.start();		// from IOIOActivity
		// Log.d("robo", "onResume");

		//camera:
		mRunning = true;
		if (mPrefs != null)
		{
			mPrefs.registerOnSharedPreferenceChangeListener(
					mSharedPreferenceListener);
		} // if
		updatePrefCacheAndUi();
		tryStartCameraStreamer();
		mWakeLock.acquire();
	}

	//Called when activity pauses
	@Override
	public void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
		// Log.d("robo", "onPause");

		//camera:
		mWakeLock.release();
		mRunning = false;
		if (mPrefs != null)
		{
			mPrefs.unregisterOnSharedPreferenceChangeListener(
					mSharedPreferenceListener);
		} // if
		ensureCameraStreamerStopped();
		//end camera
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

	//camera:
	@Override
	public void surfaceChanged(final SurfaceHolder holder, final int format,
							   final int width, final int height)
	{
		// Ingored
	} // surfaceChanged(SurfaceHolder, int, int, int)
	@Override
	public void surfaceCreated(final SurfaceHolder holder)
	{
		mPreviewDisplayCreated = true;
		tryStartCameraStreamer();
	} // surfaceCreated(SurfaceHolder)

	@Override
	public void surfaceDestroyed(final SurfaceHolder holder)
	{
		mPreviewDisplayCreated = false;
		ensureCameraStreamerStopped();
	} // surfaceDestroyed(SurfaceHolder)

	private void tryStartCameraStreamer()
	{
		if (mRunning && mPreviewDisplayCreated)
		{
			mCameraStreamer = new CameraStreamer(mCameraIndex, mUseFlashLight, mPort,
					mPrevieSizeIndex, mJpegQuality, mPreviewDisplay);
			mCameraStreamer.start();
		} // if
	} // tryStartCameraStreamer()

	private void ensureCameraStreamerStopped()
	{
		if (mCameraStreamer != null)
		{
			mCameraStreamer.stop();
			mCameraStreamer = null;
		} // if
	} // stopCameraStreamer()
	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		mSettingsMenuItem = menu.add(R.string.settings);
		mSettingsMenuItem.setIcon(android.R.drawable.ic_menu_manage);
		return true;
	} // onCreateOptionsMenu(Menu)

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		if (item != mSettingsMenuItem)
		{
			return super.onOptionsItemSelected(item);
		} // if
		startActivity(new Intent(this, PeepersPreferenceActivity.class));
		return true;
	} // onOptionsItemSelected(MenuItem)

	private final class LoadPreferencesTask
			extends AsyncTask<Void, Void, SharedPreferences>
	{
		private LoadPreferencesTask()
		{
			super();
		} // constructor()

		@Override
		protected SharedPreferences doInBackground(final Void... noParams)
		{
			return PreferenceManager.getDefaultSharedPreferences(
					Main_activity.this);
		} // doInBackground()

		@Override
		protected void onPostExecute(final SharedPreferences prefs)
		{
			Main_activity.this.mPrefs = prefs;
			prefs.registerOnSharedPreferenceChangeListener(
					mSharedPreferenceListener);
			updatePrefCacheAndUi();
			tryStartCameraStreamer();
		} // onPostExecute(SharedPreferences)


	} // class LoadPreferencesTask

	private final OnSharedPreferenceChangeListener mSharedPreferenceListener =
			new OnSharedPreferenceChangeListener()
			{
				@Override
				public void onSharedPreferenceChanged(final SharedPreferences prefs,
													  final String key)
				{
					updatePrefCacheAndUi();
				} // onSharedPreferenceChanged(SharedPreferences, String)

			}; // mSharedPreferencesListener

	private final int getPrefInt(final String key, final int defValue)
	{
		// We can't just call getInt because the preference activity
		// saves everything as a string.
		try
		{
			return Integer.parseInt(mPrefs.getString(key, null /* defValue */));
		} // try
		catch (final NullPointerException e)
		{
			return defValue;
		} // catch
		catch (final NumberFormatException e)
		{
			return defValue;
		} // catch
	} // getPrefInt(String, int)

	private final void updatePrefCacheAndUi()
	{
		mCameraIndex = getPrefInt(PREF_CAMERA, PREF_CAMERA_INDEX_DEF);
		if (hasFlashLight())
		{
			if (mPrefs != null)
			{
				mUseFlashLight = mPrefs.getBoolean(PREF_FLASH_LIGHT,
						PREF_FLASH_LIGHT_DEF);
			} // if
			else
			{
				mUseFlashLight = PREF_FLASH_LIGHT_DEF;
			} // else
		} //if
		else
		{
			mUseFlashLight = false;
		} // else

		// XXX: This validation should really be in the preferences activity.
		mPort = getPrefInt(PREF_PORT, PREF_PORT_DEF);
		// The port must be in the range [1024 65535]
		if (mPort < 1024)
		{
			mPort = 1024;
		} // if
		else if (mPort > 65535)
		{
			mPort = 65535;
		} // else if

		mPrevieSizeIndex = getPrefInt(PREF_JPEG_SIZE, PREF_PREVIEW_SIZE_INDEX_DEF);
		mJpegQuality = getPrefInt(PREF_JPEG_QUALITY, PREF_JPEG_QUALITY_DEF);
		// The JPEG quality must be in the range [0 100]
		if (mJpegQuality < 0)
		{
			mJpegQuality = 0;
		} // if
		else if (mJpegQuality > 100)
		{
			mJpegQuality = 100;
		} // else if
		mIpAddressView.setText("http://" + mIpAddress + ":" + mPort + "/");
	} // updatePrefCacheAndUi()

	private boolean hasFlashLight()
	{
		return getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA_FLASH);
	} // hasFlashLight()

	private static String tryGetIpV4Address()
	{
		try
		{
			final Enumeration<NetworkInterface> en =
					NetworkInterface.getNetworkInterfaces();
			while (en.hasMoreElements())
			{
				final NetworkInterface intf = en.nextElement();
				final Enumeration<InetAddress> enumIpAddr =
						intf.getInetAddresses();
				while (enumIpAddr.hasMoreElements())
				{
					final InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress())
					{
						final String addr = inetAddress.getHostAddress().toUpperCase();
						if (InetAddressUtils.isIPv4Address(addr))
						{
							return addr;
						}
					} // if
				} // while
			} // for
		} // try
		catch (final Exception e)
		{
			// Ignore
		} // catch
		return null;
	} // tryGetIpV4Address()
	//end camera

}
