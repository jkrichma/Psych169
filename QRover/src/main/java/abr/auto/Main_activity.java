package abr.auto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import ioio.lib.util.IOIOLooper;
import ioio.lib.util.IOIOLooperProvider;
import ioio.lib.util.android.IOIOAndroidApplicationHelper;

// implements IOIOLooperProvider: from IOIOActivity
public class Main_activity extends Activity implements IOIOLooperProvider, SensorEventListener, CameraBridgeViewBase.CvCameraViewListener2 {

    private final IOIOAndroidApplicationHelper helper_ = new IOIOAndroidApplicationHelper(this, this);            // from IOIOActivity
    private TextView irLeftText;
    private TextView irCenterText;
    private TextView irRightText;
    private ToggleButton btnStartStop;

    float irLeftReading;
    float irCenterReading;
    float irRightReading;

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

    // variables for OpenCV
    private static final String TAG = "----- OpenCV -----";
    private CameraBridgeViewBase mOpenCvCameraView;
    // These variables are used (at the moment) to fix camera orientation from 270degree to 0degree
    Mat mRgba;
    Mat mRgbaF;
    Mat mRgbaT;

    private static boolean isZxingThreadRunning;

    IOIO_thread_rover_tank m_ioio_thread;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);

        irLeftText = (TextView) findViewById(R.id.irLeft);
        irCenterText = (TextView) findViewById(R.id.irCenter);
        irRightText = (TextView) findViewById(R.id.irRight);
        btnStartStop = (ToggleButton) findViewById(R.id.buttonStartStop);


        //set up compass
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mCompass = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mGravityS = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        helper_.create();        // from IOIOActivity

        // TEST OPENCV
        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        enableUi(true);
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

        if (m_ioio_thread != null) {

            irLeftReading = m_ioio_thread.getIrLeftReading();
            irCenterReading = m_ioio_thread.getIrCenterReading();
            irRightReading = m_ioio_thread.getIrRightReading();

            setText(String.format("%.3f", irLeftReading), irLeftText);
            setText(String.format("%.3f", irCenterReading), irCenterText);
            setText(String.format("%.3f", irRightReading), irRightText);

            if (btnStartStop.isChecked()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnStartStop.setBackgroundColor(Color.RED);
                    }
                });

                if (irCenterReading < 0.6f) {
                    m_ioio_thread.move(0.2f, 0.2f, true, true);
                } else {
                    m_ioio_thread.turn(1200);
                }

                if (irLeftReading > 0.6f) {
                    m_ioio_thread.turn(1800);
                }

                if (irRightReading > 0.6f) {
                    m_ioio_thread.turn(1200);
                }

            } else {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnStartStop.setBackgroundColor(Color.GREEN);
                    }
                });

                m_ioio_thread.move(0.0f, 0.0f, false, false);
            }
            
        }

        // sensors unused for the moment.  may want to implement later
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            mGravity = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
            mGyro = event.values;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mAcc = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
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
    public void setText(final String str, final TextView tv) {
        //Log.i("robo", "setText");
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
     */
    @Override
    public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
        if (m_ioio_thread == null && connectionType.matches("ioio.lib.android.bluetooth.BluetoothIOIOConnection")) {
            // enableUi(true);
            m_ioio_thread = new IOIO_thread_rover_tank();
            return m_ioio_thread;
        } else {
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
        helper_.start();        // from IOIOActivity

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        // Log.d("robo", "onResume");
    }

    //Called when activity pauses
    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        // Log.d("robo", "onPause");
    }

    //Called when activity restarts. onCreate() will then be called
    @Override
    public void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        helper_.destroy();
        super.onDestroy();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        helper_.start();
    }

    @Override
    protected void onStop() {
        helper_.stop();
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
            helper_.restart();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "STARTED CAMERA VIEW!!! " + width + "x" + height);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        //Log.i(TAG, "Input frame is " + inputFrame.rgba().width() + " by " + inputFrame.rgba().height());
        mRgba = inputFrame.rgba();

        Thread zxingThread = new Thread() {
            @Override
            public void run() {
                if (isZxingThreadRunning) {
                    return;
                }

                isZxingThreadRunning = true;
                try {
                    zxing();
                } catch (ChecksumException e) {
                    e.printStackTrace();
                } catch (FormatException e) {
                    e.printStackTrace();
                }
                isZxingThreadRunning = false;
            }
        };

        zxingThread.start();

        return mRgba; // This function must return
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public void zxing() throws ChecksumException, FormatException {

        Bitmap bMap = Bitmap.createBitmap(mRgba.width(), mRgba.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, bMap);
        int[] intArray = new int[bMap.getWidth() * bMap.getHeight()];
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Reader reader = new QRCodeMultiReader();

        try {
            Result result = reader.decode(bitmap);
            Log.i(TAG, "CODE FOUND!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            Log.i(TAG, result.getText());
            if (result.getText().equals("START")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnStartStop.setChecked(true);
                        btnStartStop.setBackgroundColor(Color.RED);
                    }
                });
            } else if (result.getText().equals("STOP")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnStartStop.setChecked(false);
                        btnStartStop.setBackgroundColor(Color.GREEN);
                    }
                });
            }
        } catch (NotFoundException e) {
            Log.i(TAG, "Code Not Found");
            e.printStackTrace();
        }
    }
}
