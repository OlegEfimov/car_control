package com.example.user.car_control2;

import java.lang.ref.WeakReference;
import java.nio.MappedByteBuffer;
import java.util.Locale;

//import com.example.user.car_control2.cBluetooth;
import com.example.user.car_control2.Classifier;

import com.example.user.car_control2.R;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
//import android.bluetooth.BluetoothAdapter;
import android.os.Build;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

// import org.tensorflow.lite.examples.classification.tflite.Classifier.Device;
// import org.tensorflow.lite.examples.classification.tflite.Classifier.Model;
// import org.tensorflow.lite.examples.classification.tflite.Classifier.Recognition;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
// import org.tensorflow.lite.examples.classification.env.Logger;
// import org.tensorflow.lite.examples.classification.tflite.Classifier.Device;
// import org.tensorflow.lite.gpu.GpuDelegate;
// import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.model.Model;

import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;



public class ActivityAccelerometer extends Activity implements SensorEventListener  {
	
	private SensorManager mSensorManager;
    private Sensor mAccel;
//	private cBluetooth bl = null;
    private WebSocketClient mWebSocketClient;
    private ToggleButton LightButton;
	
	private int xAxis = 0;
    private int yAxis = 0;
    private int motorLeft = 0;
    private int motorRight = 0;
    private String address;			// MAC-address from settings
    private boolean show_Debug;		// show debug information (from settings)
    private boolean BT_is_connect;	// bluetooth is connected Bluetooth)
    private int xMax;		    	// limit on the X axis from settings X, (0-10)
    private int yMax;		    	// limit on the Y axis from settings Y(0-10))
    private int yThreshold;  		// minimum value of PWM from settings
    private int pwmMax;	   			// maximum value of PWM from settings
    private int xR;					// pivot point from settings
    private String commandLeft;		// command symbol for left motor from settings
    private String commandRight;	// command symbol for right motor from settings
    private String commandHorn;		// command symbol for optional command from settings (for example - horn)
    private boolean enableControl;
    private Classifier classifier;
    public String modelName;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accelerometer);
                
        address = (String) getResources().getText(R.string.default_MAC);
        xMax = Integer.parseInt((String) getResources().getText(R.string.default_xMax));
        xR = Integer.parseInt((String) getResources().getText(R.string.default_xR));
        yMax = Integer.parseInt((String) getResources().getText(R.string.default_yMax));
        yThreshold = Integer.parseInt((String) getResources().getText(R.string.default_yThreshold));
        pwmMax = Integer.parseInt((String) getResources().getText(R.string.default_pwmMax));
        commandLeft = (String) getResources().getText(R.string.default_commandLeft);
        commandRight = (String) getResources().getText(R.string.default_commandRight);
        commandHorn = (String) getResources().getText(R.string.default_commandHorn);

        Intent intent = getIntent();
        modelName = intent.getStringExtra("model_name");

        loadPref();
        
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); 
        
        connectWebSocket();
//        bl = new cBluetooth(this, mHandler);
//        bl.checkBTState();
        
        LightButton = (ToggleButton) findViewById(R.id.LightButton);   
	
        LightButton.setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			if(LightButton.isChecked()){
                    enableControl = true;
//    				if(BT_is_connect) bl.sendData(String.valueOf(commandHorn+"1\r"));
    			}else{
                    enableControl = false;
//    				if(BT_is_connect) bl.sendData(String.valueOf(commandHorn+"0\r"));
    			}
    		}
    	});
        
        mHandler.postDelayed(sRunnable, 600000);
        //finish();

        createClassifier(modelName);

    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebSocketClient != null) {
            mWebSocketClient.close();
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<ActivityAccelerometer> mActivity;
     
        public MyHandler(ActivityAccelerometer activity) {
          mActivity = new WeakReference<ActivityAccelerometer>(activity);
        }
     
        @Override
        public void handleMessage(Message msg) {
        	ActivityAccelerometer activity = mActivity.get();
          if (activity != null) {
          	switch (msg.what) {
//            case cBluetooth.BL_NOT_AVAILABLE:
//               	Log.d(cBluetooth.TAG, "Bluetooth is not available. Exit");
//            	Toast.makeText(activity.getBaseContext(), "Bluetooth is not available", Toast.LENGTH_SHORT).show();
//                activity.finish();
//                break;
//            case cBluetooth.BL_INCORRECT_ADDRESS:
//            	Log.d(cBluetooth.TAG, "Incorrect MAC address");
//            	Toast.makeText(activity.getBaseContext(), "Incorrect Bluetooth address", Toast.LENGTH_SHORT).show();
//                break;
//            case cBluetooth.BL_REQUEST_ENABLE:
//            	Log.d(cBluetooth.TAG, "Request Bluetooth Enable");
//            	BluetoothAdapter.getDefaultAdapter();
//            	Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            	activity.startActivityForResult(enableBtIntent, 1);
//                break;
//            case cBluetooth.BL_SOCKET_FAILED:
//            	Toast.makeText(activity.getBaseContext(), "Socket failed", Toast.LENGTH_SHORT).show();
//            	//activity.finish();
//                break;
            }
          }
        }
      }
     
    private final MyHandler mHandler = new MyHandler(this);

    private final static Runnable sRunnable = new Runnable() {
    	public void run() { }
    };
          
      
    public void onSensorChanged(SensorEvent e) {
    	String directionL = "";
    	String directionR = "";
    	String cmdSendL,cmdSendR;
        float xRaw, yRaw;		// RAW-value from Accelerometer sensor (RAW-)

        WindowManager windowMgr = (WindowManager)this.getSystemService(WINDOW_SERVICE);
        int rotationIndex = windowMgr.getDefaultDisplay().getRotation();
        if (rotationIndex == 1 || rotationIndex == 3){			// detect 90 or 270 degree rotation ( 90 - 270)
        	xRaw = -e.values[1];
        	yRaw = e.values[0];
        }
        else{
        	xRaw = e.values[0];
        	yRaw = e.values[1];      	
        }
    	    	
    	xAxis = Math.round(xRaw*pwmMax/xR);
        yAxis = Math.round(yRaw*pwmMax/yMax);
        
        if(xAxis > pwmMax) xAxis = pwmMax;
        else if(xAxis < -pwmMax) xAxis = -pwmMax;		// negative - tilt right
        
        if(yAxis > pwmMax) yAxis = pwmMax;
        else if(yAxis < -pwmMax) yAxis = -pwmMax;		// negative - tilt forward
        else if(yAxis >= 0 && yAxis < yThreshold) yAxis = 0;
        else if(yAxis < 0 && yAxis > -yThreshold) yAxis = 0;
        
        if(xAxis > 0) {		// if tilt to left, slow down the left engine
        	motorRight = yAxis;
        	if(Math.abs(Math.round(xRaw)) > xR){
        		motorLeft = Math.round((xRaw-xR)*pwmMax/(xMax-xR));
        		motorLeft = Math.round(-motorLeft * yAxis/pwmMax);
        		//if(motorLeft < -pwmMax) motorLeft = -pwmMax;
        	}
        	else motorLeft = yAxis - yAxis*xAxis/pwmMax;
        }
        else if(xAxis < 0) {		// tilt to right
        	motorLeft = yAxis;
        	if(Math.abs(Math.round(xRaw)) > xR){
        		motorRight = Math.round((Math.abs(xRaw)-xR)*pwmMax/(xMax-xR));
        		motorRight = Math.round(-motorRight * yAxis/pwmMax);
        		//if(motorRight > -pwmMax) motorRight = -pwmMax;
        	}
        	else motorRight = yAxis - yAxis*Math.abs(xAxis)/pwmMax;
        }
        else if(xAxis == 0) {
        	motorLeft = yAxis;
        	motorRight = yAxis;
        }
        
        if(motorLeft > 0) {			// tilt to backward
        	directionL = "-";
        }      
        if(motorRight > 0) {		// tilt to backward
        	directionR = "-";
        }
        motorLeft = Math.abs(motorLeft);
        motorRight = Math.abs(motorRight);
               
        if(motorLeft > pwmMax) motorLeft = pwmMax;
        if(motorRight > pwmMax) motorRight = pwmMax;
                
        cmdSendL = String.valueOf(commandLeft+directionL+motorLeft+"\r");
        cmdSendR = String.valueOf(commandRight+directionR+motorRight+"\r");
        
//        if(BT_is_connect) bl.sendData(cmdSendL+cmdSendR);

        TextView textX = (TextView) findViewById(R.id.textViewX);
        TextView textY = (TextView) findViewById(R.id.textViewY);
        TextView mLeft = (TextView) findViewById(R.id.mLeft);
        TextView mRight = (TextView) findViewById(R.id.mRight);
        TextView textCmdSend = (TextView) findViewById(R.id.textViewCmdSend);


        if(show_Debug){
        	textX.setText(String.valueOf("X:" + String.format("%.1f",xRaw) + "; xPWM:"+xAxis));
	        textY.setText(String.valueOf("Y:" + String.format("%.1f",yRaw) + "; yPWM:"+yAxis));
	        mLeft.setText(String.valueOf("MotorL:" + directionL + "." + motorLeft));
	        mRight.setText(String.valueOf("MotorR:" + directionR + "." + motorRight));
	        textCmdSend.setText(String.valueOf("Send:" + cmdSendL.toUpperCase(Locale.getDefault()) + cmdSendR.toUpperCase(Locale.getDefault())));
        }
        else{
        	textX.setText("");
        	textY.setText("");
        	mLeft.setText("");
        	mRight.setText("");
        	textCmdSend.setText("");
        }

        String actionL,actionR;
        if (enableControl) {
            actionL = String.valueOf(directionL+motorLeft+"\r");
            actionR = String.valueOf(directionR+motorRight+"\r");
        } else {
            actionL = String.valueOf(directionL + "0\r");
            actionR = String.valueOf(directionR + "0\r");
        }
        // sendCommand(String.valueOf(actionL.toUpperCase(Locale.getDefault()) + "=" + actionR.toUpperCase(Locale.getDefault()) + "=;\n"));

    }
   
    private void loadPref(){
    	SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);  
    	
    	address = mySharedPreferences.getString("pref_MAC_address", address);			// the first time we load the default values
    	xMax = Integer.parseInt(mySharedPreferences.getString("pref_xMax", String.valueOf(xMax)));
    	xR = Integer.parseInt(mySharedPreferences.getString("pref_xR", String.valueOf(xR)));
    	yMax = Integer.parseInt(mySharedPreferences.getString("pref_yMax", String.valueOf(yMax)));
    	yThreshold = Integer.parseInt(mySharedPreferences.getString("pref_yThreshold", String.valueOf(yThreshold)));
    	pwmMax = Integer.parseInt(mySharedPreferences.getString("pref_pwmMax", String.valueOf(pwmMax)));
    	show_Debug = mySharedPreferences.getBoolean("pref_Debug", true);
    	commandLeft = mySharedPreferences.getString("pref_commandLeft", commandLeft);
    	commandRight = mySharedPreferences.getString("pref_commandRight", commandRight);
    	commandHorn = mySharedPreferences.getString("pref_commandHorn", commandHorn);
	}
    
    @Override
    protected void onResume() {
    	super.onResume();
//    	BT_is_connect = bl.BT_Connect(address, false);
    	mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL);    	
    }

    @Override
    protected void onPause() {
    	super.onPause();
//    	bl.BT_onPause();
    	mSensorManager.unregisterListener(this);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	loadPref();
    }
    
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // TODO Auto-generated method stub
    }


    private void connectWebSocket() {
        URI uri;
        try {
            uri = new URI("ws://192.168.4.1:81/");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                mWebSocketClient.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);
            }

            @Override
            public void onMessage(String s) {
                final String message = s;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textView = (TextView)findViewById(R.id.messages);
                        textView.setText(textView.getText() + "\n" + message);
                        textView.setText(message);
                    }
                });

                //TBD convert message to model input
                // ByteBuffer modelInput = convertMessage(message);
                // processInput(modelInput);
                // ByteBuffer modelInput = convertMessage(message);
                processInput(message);

            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };
        if (mWebSocketClient != null) {
            Log.i("Websocket", "connect start");
            mWebSocketClient.connect();
            Log.i("Websocket", "connect end");
        }
    }

    public void sendMessageLF(View view) {
        String str1 = "200=0=;";
        int action1 = 200;
        int action2 = 0;
        mWebSocketClient.send(String.format("%s\n", str1));
    }

    public void sendMessageRF(View view) {
        String str1 = "0=200=;";
        int action1 = 0;
        int action2 = 200;
        mWebSocketClient.send(String.format("%s\n", str1));
    }
    public void sendMessageLB(View view) {
        String str1 = "-200=0=;";
        int action1 = -200;
        int action2 = 0;
        mWebSocketClient.send(String.format("%s\n", str1));
    }
    public void sendMessageRB(View view) {
        String str1 = "0=-200=;";
        int action1 = 0;
        int action2 = -200;
        mWebSocketClient.send(String.format("%s\n", str1));
    }

    public void sendCommand(String cmd) {
        Log.i("Websocket", "sendCommand start1");

        if (mWebSocketClient != null) {
            if (mWebSocketClient.isOpen()) {
                Log.i("Websocket", "sendCommand start2");
                mWebSocketClient.send(cmd);
                Log.i("Websocket", "sendCommand end2");
//            } else {
//                Log.i("Websocket sendCommand", "reconnect start");
//                mWebSocketClient.reconnect();
//                Log.i("Websocket sendCommand", "reconnect end");
            }
        } else {
            Log.i("Websocket sendCommand", "mWebSocketClient == null");
        }
        Log.i("Websocket", "sendCommand end1");
    }

      private void createClassifier(String modelName) {
        if (classifier != null) {
          classifier.close();
          classifier = null;
        }
        try {
          classifier = Classifier.create(this, modelName);
        } catch (IOException e) {
        }
      }

  private static final int ZERO_ENGINE = 10;
  private static final int ZERO_ENGINE_SHIFT = 70;
  private static final int MAX_ENGINE_FORCE = 800;
  private static final int MAX_ENGINE_FORCE_HW = 230 - ZERO_ENGINE_SHIFT;
  private int maxEngineForceHW = MAX_ENGINE_FORCE_HW;

    protected void processInput(String input) {
        Log.i("processInput", "input= " + input);


        // if (classifier != null && enableControl) {
        if (classifier != null) {
            final float[][] results =
            classifier.getAction(input);

            // float tmpActFloat = results[0][0];

            // float tmpAction_0 = (float)(tmpActFloat * 0.5F) + 0.7F;
            // float tmpAction_1 = (float)(-tmpActFloat * 0.5F) +0.7F;
            // float tmpAction_0 = (float)results[0][0] + 0.7F;
            // float tmpAction_1 = (float)results[0][1] +0.7F;
            float tmpAction_0 = (float)results[0][0];
            float tmpAction_1 = (float)results[0][1];

            String strAction_0 =String.valueOf(tmpAction_0); 
            String strAction_1 =String.valueOf(tmpAction_1); 
            Log.i("processInput", "Action= " + strAction_0 + " | " + strAction_1);

            int forceLeftHW = Math.round(tmpAction_0 * maxEngineForceHW);
            int forceRightHW = Math.round(tmpAction_1 * maxEngineForceHW);
            int left = (int) (forceLeftHW * 0.3F);
            int right = (int) (forceRightHW * 0.3F);

            String strLeft =String.valueOf(left); 
            String strRight =String.valueOf(right); 
            Log.i("processInput", "left= " + strLeft + " right= " + strRight);

            if (left > ZERO_ENGINE) {
                left = left + ZERO_ENGINE_SHIFT;
            } else if ((left + ZERO_ENGINE) < 0) {
                left = left - ZERO_ENGINE_SHIFT;
            } else {
                left = 0;
            }

            if (right > ZERO_ENGINE) {
                right = right + ZERO_ENGINE_SHIFT;
            } else if ((right + ZERO_ENGINE) < 0) {
                right = right - ZERO_ENGINE_SHIFT;
            } else {
                right = 0;
            }

            motorLeft = left;
            motorRight = right;


            String directionL = "";
            String directionR = "";

            String actionL,actionR;
            if (enableControl) {
                actionL = String.valueOf(directionL+motorLeft+"\r");
                actionR = String.valueOf(directionR+motorRight+"\r");
            } else {
                actionL = String.valueOf(directionL + "0\r");
                actionR = String.valueOf(directionR + "0\r");
            }
            sendCommand(String.valueOf(actionL.toUpperCase(Locale.getDefault()) + "=" + actionR.toUpperCase(Locale.getDefault()) + "=;\n"));

            runOnUiThread(
                new Runnable() {
                @Override
                public void run() {
                // TBD show result from model
                }
            });
        }
    }

  }

