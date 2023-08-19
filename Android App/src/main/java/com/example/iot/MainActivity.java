package com.example.iot;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private Handler handler=new Handler();

    TextView tv, tv1, tv2;
    ImageView iv;
    Button bt;
    boolean rtt_flag=false;

    //private static final String TAG="MainActivity";
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magnetometer;

    private float[] acc_reading = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];

    private float[] mag_reading = new float[3];
    private float[] gyroMatrix = new float[9];
    private float[] gyroOrientation = new float[3];

    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private float timestamp;
    public static final float EPSILON = 0.000000001f;

    private int count=0, step=0;

    private float[] prev_mean_pose = new float[3];
    private float[] control = new float[2];
    boolean flag=true;

    Python py;
    PyObject pyObj;

    Bitmap bitmap, drawBitmap;
    Canvas canvas;
    Paint paint, p1;

    private Sensor gravity;
    private float[] grav_reading = new float[3];

    private float rc = 0.002f;

    float vel_x=0.0f, vel_y=0.0f, vel_z=0.0f;
    ArrayList<Float> acc_m = new ArrayList<>();
    private float[] vel = new float[3];
    ArrayList<float[]> avg_vel = new ArrayList<float[]>();

    long lastCheckTime = 0;

    boolean highLineState = true;
    boolean lowLineState = true;
    boolean passageState = false;

    double highLine = 0.01 ;
    double highBoundaryLine = 0;
    double highBoundaryLineAlpha = 1.0;

    double highLineMin = 0.005;
    double highLineMax = 0.02;
    double highLineAlpha = 0.0005;

    double lowLine = -0.02;
    double lowBoundaryLine = 0;
    double lowBoundaryLineAlpha = -1.0;

    double lowLineMax = -0.015;
    double lowLineMin = -0.03;
    double lowLineAlpha = 0.0005;

    private float[] bel_pos = new float[3];
    private float[] bel_cov = new float[3];
    boolean first = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = (TextView) findViewById(R.id.tv);
        //tv1 = (TextView) findViewById(R.id.tv1);
        tv2 = (TextView) findViewById(R.id.tv2);
        iv = (ImageView)findViewById(R.id.iv);
        bt = (Button)findViewById(R.id.bt);

        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rtt_flag=true;
            }
        });

        sensorManager =  (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        sensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);

        // initialise gyroMatrix with identity matrix
        gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;

        py = Python.getInstance();
        //specify the name of the python file
        pyObj = py.getModule("kalman");

        bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.line);
        drawBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas iniCanvas = new Canvas(drawBitmap);
        iniCanvas.drawBitmap(bitmap, new Matrix(), null);
        p1 = new Paint();
        p1.setColor(Color.MAGENTA);
        p1.setStrokeWidth(7);


        iv.setImageBitmap(bitmap);
        iv.setImageBitmap(drawBitmap);
        canvas = new Canvas(drawBitmap);
        iv.draw(canvas);

        paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(5);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null) {
            return;
        }

        if (event.sensor.getType()== Sensor.TYPE_GRAVITY){
            //Log.d(TAG, "On sensor change gravity: x"+event.values[0]+"y:"+event.values[1]+"z:"+event.values[2]);
            System.arraycopy(event.values,0, grav_reading, 0, grav_reading.length);
        }else if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            //Log.d(TAG, "accelerometer: x"+event.values[0]+"y:"+event.values[1]+"z:"+event.values[2]);
            System.arraycopy(event.values,0, acc_reading, 0, acc_reading.length);

            if(timestamp != 0) {
                final float dt = (event.timestamp - timestamp) * NS2S;
                final float alpha = rc / (rc + dt);

                //removing gravity with low-pass filter
                for(int i=0; i<3; i++){
                    acc_reading[i] -= (alpha * grav_reading[i] + (1 - alpha) * acc_reading[i]);
                }

                //y axis acceleration changing with each step
                acc_m.add(acc_reading[1]);
                vel[0] = acc_reading[0] * dt;
                vel[1] = acc_reading[1] * dt;
                vel[2] = acc_reading[2] * dt;
                avg_vel.add(vel);

                if (acc_reading != null) {
                    //this function check if a step is detected by fixing thresholds
                    readStepDetection(acc_reading);
                }
            }
            timestamp = event.timestamp;

        } else if(event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){
            //Log.d(TAG, "magnetometer: azi:"+event.values[0]+"pitch:"+event.values[1]+"roll:"+event.values[2]);
            System.arraycopy(event.values, 0, mag_reading, 0, mag_reading.length);
        } else if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE){
            //gives angular speed in x,y,z direction
            if (timestamp != 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;
                // Axis of the rotation sample, not normalized yet.
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];

                // Calculate the angular speed of the sample
                float omegaMagnitude = (float) Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

                // Normalize the rotation vector if it's big enough to get the axis
                if (omegaMagnitude > EPSILON) {
                    axisX /= omegaMagnitude;
                    axisY /= omegaMagnitude;
                    axisZ /= omegaMagnitude;
                }

                // Integrate around this axis with the angular speed by the time step
                // in order to get a delta rotation from this sample over the time step
                // We will convert this axis-angle representation of the delta rotation
                // into a quaternion before turning it into the rotation matrix.
                float thetaOverTwo = omegaMagnitude * dT / 2.0f;
                float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
                float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
                deltaRotationVector[0] = sinThetaOverTwo * axisX;
                deltaRotationVector[1] = sinThetaOverTwo * axisY;
                deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                deltaRotationVector[3] = cosThetaOverTwo;
            }
            timestamp = event.timestamp;
            float[] deltaRotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
            gyroMatrix = matrixMultiplication(gyroMatrix, deltaRotationMatrix);

            // gyroOrientation contains gyroscope based orientation
            SensorManager.getOrientation(gyroMatrix, gyroOrientation);
            //Log.d(TAG, "gyroOrientation: orientation x:"+Math.toDegrees(gyroOrientation[0])+"y:"+Math.toDegrees(gyroOrientation[1])+"z:"+Math.toDegrees(gyroOrientation[2]));
        }
        if(SensorManager.getRotationMatrix(rotationMatrix, null, acc_reading, mag_reading)){
            //orientation contains magnetometer and acceleration based orientation
            SensorManager.getOrientation(rotationMatrix, orientation);
            //Log.d(TAG, "magOrientation: orientation x:"+Math.toDegrees(orientation[0])+"y:"+Math.toDegrees(orientation[1])+"z:"+Math.toDegrees(orientation[2]));
        }

    }

    private void readStepDetection(float[] acc_reading) {
        long currentTime = System.currentTimeMillis();
        long gapTime1 = (currentTime - lastCheckTime);

        //All the thresholds are determined by observing the acceleration plot

        if (highLineState && highLine > highLineMin) {
            highLine = highLine - highLineAlpha;
            highBoundaryLine = highLine * highBoundaryLineAlpha;
        }
        if (lowLineState && lowLine < lowLineMax) {
            lowLine = lowLine + lowLineAlpha;
            lowBoundaryLine = lowLine * lowBoundaryLineAlpha;
        }

        double yValue=acc_reading[1];
        if (highLineState && gapTime1 > 100 && yValue > highBoundaryLine){
            highLineState = false;
        }
        if (lowLineState && yValue < lowBoundaryLine && passageState) {
            lowLineState = false;
        }
        if (!highLineState) {
            if (yValue > highLine) {
                highLine = yValue;
                highBoundaryLine = highLine * highBoundaryLineAlpha;

                if (highLine > highLineMax) {
                    highLine = highLineMax;
                    highBoundaryLine = highLine * highBoundaryLineAlpha;
                }
            } else {
                if (highBoundaryLine > yValue) {
                    highLineState = true;
                    passageState = true;
                }
            }
        }
        if (!lowLineState && passageState) {
            if (yValue < lowLine) {
                lowLine = yValue;
                lowBoundaryLine = lowLine * lowBoundaryLineAlpha;

                if (lowLine < lowLineMin) {
                    lowLine = lowLineMin;
                    lowBoundaryLine = lowLine * lowBoundaryLineAlpha;
                }
            } else {
                if (lowBoundaryLine < yValue) {
                    lowLineState = true;
                    passageState = false;

                    //step is detected here
                    //step length, heading are calculated and send to python script in this thread
                    //also path is ploted in real-time
                    TestRunnable runnable=new TestRunnable();
                    new Thread(runnable).start();

                    lastCheckTime = currentTime;
                }
            }
        }
    }

    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    class TestRunnable implements Runnable{

        @Override
        public void run() {
            try {
                Thread.sleep(1000);

                if(orientation!=null && gyroOrientation!=null){
                    //use x-axis value
                    double compAngle1 = 0.02 * orientation[0] + 0.98 * gyroOrientation[0];
                    //double compAngle2 = 0.02 * orientation[1] + 0.98 * gyroOrientation[1];
                    //double compAngle3 = 0.02 * orientation[2] + 0.98 * gyroOrientation[2];
                    //tv.setText("magx:"+(int) Math.toDegrees(orientation[0])+"y:"+(int) Math.toDegrees(orientation[1])+"z:"+(int) Math.toDegrees(orientation[2]));
                    //tv1.setText("gyrox:"+(int) Math.toDegrees(gyroOrientation[0])+"y:"+(int) Math.toDegrees(gyroOrientation[1])+"z:"+(int) Math.toDegrees(gyroOrientation[2]));
                    //tv2.setText("x:"+(int) Math.toDegrees(compAngle1)+"y:"+(int) Math.toDegrees(compAngle2)+"z:"+(int) Math.toDegrees(compAngle3));

                    if(flag){
                        prev_mean_pose[2]= (int) Math.toDegrees(compAngle1);
                        flag=false;
                    }
                    control[1]= (int) Math.toDegrees(compAngle1);
                }

                float acc_max = Collections.max(acc_m);
                float acc_min = Collections.min(acc_m);

                for(int i=0; i<avg_vel.size(); i++){
                    vel_x += avg_vel.get(i)[0];
                    vel_y += avg_vel.get(i)[1];
                    vel_z += avg_vel.get(i)[2];
                }
                vel_x /= avg_vel.size();
                vel_y /= avg_vel.size();
                vel_z /= avg_vel.size();

                float v_step = (float) Math.sqrt(vel_x*vel_x + vel_y*vel_y + vel_z*vel_z);
                float sub = acc_max - acc_min;

                //convert to cm for ploting.... i pixel equal 1 cm......(*100)
                control[0] = (float) ((0.68 - 0.37 * v_step + 0.15 * v_step * v_step) * Math.pow(sub,0.25))*100;

                avg_vel.clear();

                if(first) {
                    //functions defined in python file are called and there returned values are used for ploting path
                    List<PyObject> obj = pyObj.callAttr("kalman_init_line").asList();
                    prev_mean_pose = obj.get(0).toJava(float[].class);
                    bel_cov = obj.get(1).toJava(float[].class);
                    first=false;
                }else if(rtt_flag){
                    List<PyObject> obj = pyObj.callAttr("pdr_with_rtt",control[0], control[1]).asList();
                    bel_pos = obj.get(0).toJava(float[].class);
                    bel_cov = obj.get(1).toJava(float[].class);
                    rtt_flag=false;
                }else {
                    List<PyObject> obj = pyObj.callAttr("pdr",control[0], control[1]).asList();
                    bel_pos = obj.get(0).toJava(float[].class);
                    bel_cov = obj.get(1).toJava(float[].class);
                }

                //x-axis 1cm = 0.8 pixel and initial position (0,0) is at pixel value (190,(height*0.5)-54)
                canvas.drawLine(prev_mean_pose[0]+(prev_mean_pose[0]*0.8f)+190, (drawBitmap.getHeight()*0.5f-54)+prev_mean_pose[1], bel_pos[0]+(bel_pos[0]*0.8f)+190, (drawBitmap.getHeight()*0.5f-54)+bel_pos[1], paint);
                step++;
                handler.post(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        tv2.setText("vx: "+bel_cov[0]+"vy: "+bel_cov[1]+"vz: "+bel_cov[2]);
                        tv.setText("step: "+step+"x: "+bel_pos[0]+"y: "+bel_pos[1]+" theta: "+bel_pos[2]);
                        iv.setImageBitmap(drawBitmap);
                    }
                });

                prev_mean_pose[0]=bel_pos[0];
                prev_mean_pose[1]=bel_pos[1];
                prev_mean_pose[2]=bel_pos[2];

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}