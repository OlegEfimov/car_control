package com.example.user.car_control2;

import java.lang.ref.WeakReference;

//import com.example.user.car_control2.cBluetooth;
import com.example.user.car_control2.R;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

import android.app.Activity;
//import android.bluetooth.BluetoothAdapter;
import android.os.Build;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.TextView;

public class ActivityButtons extends Activity {
	
//	private cBluetooth bl = null;
    private WebSocketClient mWebSocketClient;
	private ToggleButton LightButton;
	
	private Button btn_forward, btn_backward, btn_left, btn_right;
	
    private int motorLeft = 0;
    private int motorRight = 0;
    private String address;			// MAC-address from settings
    private boolean BT_is_connect;	// bluetooh is connected
    private int pwmBtnMotorLeft;	// left PWM constant value from settings
    private int pwmBtnMotorRight;	// right PWM constant value from settings
    private String commandLeft;		// command symbol for left motor from settings
    private String commandRight;	// command symbol for right motor from settings
    private String commandHorn;		// command symbol for optional command (for example - horn) from settings
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_buttons);
		
		address = (String) getResources().getText(R.string.default_MAC);
		pwmBtnMotorLeft = Integer.parseInt((String) getResources().getText(R.string.default_pwmBtnMotorLeft));
		pwmBtnMotorRight = Integer.parseInt((String) getResources().getText(R.string.default_pwmBtnMotorRight));
        commandLeft = (String) getResources().getText(R.string.default_commandLeft);
        commandRight = (String) getResources().getText(R.string.default_commandRight);
        commandHorn = (String) getResources().getText(R.string.default_commandHorn);
		
		loadPref();
		
        connectWebSocket();
//	    bl = new cBluetooth(this, mHandler);
//	    bl.checkBTState();
		
		btn_forward = (Button) findViewById(R.id.forward);
		btn_backward = (Button) findViewById(R.id.backward);
		btn_left = (Button) findViewById(R.id.left);
		btn_right = (Button) findViewById(R.id.right);
		       
		btn_forward.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
		        if(event.getAction() == MotionEvent.ACTION_MOVE) {
		        	motorLeft = pwmBtnMotorLeft;
		        	motorRight = pwmBtnMotorRight;
					sendMessageLF(v);
//		        	if(BT_is_connect) bl.sendData(String.valueOf(commandLeft+motorLeft+"\r"+commandRight+motorRight+"\r"));
		        } else if (event.getAction() == MotionEvent.ACTION_UP) {
		        	motorLeft = 0;
		        	motorRight = 0;
//		        	if(BT_is_connect) bl.sendData(String.valueOf(commandLeft+motorLeft+"\r"+commandRight+motorRight+"\r"));
		        }
				return false;
		    }
		});
		
		btn_left.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
		        if(event.getAction() == MotionEvent.ACTION_MOVE) {
		        	motorLeft = -pwmBtnMotorLeft;
		        	motorRight = pwmBtnMotorRight;
					sendMessageRF(v);
//		        	if(BT_is_connect) bl.sendData(String.valueOf(commandLeft+motorLeft+"\r"+commandRight+motorRight+"\r"));
		        } else if (event.getAction() == MotionEvent.ACTION_UP) {
		        	motorLeft = 0;
		        	motorRight = 0;
//		        	if(BT_is_connect) bl.sendData(String.valueOf(commandLeft+motorLeft+"\r"+commandRight+motorRight+"\r"));
		        }
				return false;
		    }
		});
		
		btn_right.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
		        if(event.getAction() == MotionEvent.ACTION_MOVE) {
		        	motorLeft = pwmBtnMotorLeft;
		        	motorRight = -pwmBtnMotorRight;
//		        	if(BT_is_connect) bl.sendData(String.valueOf(commandLeft+motorLeft+"\r"+commandRight+motorRight+"\r"));
		        } else if (event.getAction() == MotionEvent.ACTION_UP) {
		        	motorLeft = 0;
		        	motorRight = 0;
//		        	if(BT_is_connect) bl.sendData(String.valueOf(commandLeft+motorLeft+"\r"+commandRight+motorRight+"\r"));
		        }
				return false;
		    }
		});
		
		btn_backward.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
		        if(event.getAction() == MotionEvent.ACTION_MOVE) {
		        	motorLeft = -pwmBtnMotorLeft;
		        	motorRight = -pwmBtnMotorRight;
//		        	if(BT_is_connect) bl.sendData(String.valueOf(commandLeft+motorLeft+"\r"+commandRight+motorRight+"\r"));
		        } else if (event.getAction() == MotionEvent.ACTION_UP) {
		        	motorLeft = 0;
		        	motorRight = 0;
//		        	if(BT_is_connect) bl.sendData(String.valueOf(commandLeft+motorLeft+"\r"+commandRight+motorRight+"\r"));
		        }
				return false;
		    }
		});
		
		LightButton = (ToggleButton) findViewById(R.id.LightButton);   
//		LightButton.setOnClickListener(new OnClickListener() {
//			public void onClick(View v) {
//				if(LightButton.isChecked()){
//					if(BT_is_connect) bl.sendData(String.valueOf(commandHorn+"1\r"));
//	    		}else{
//	    			if(BT_is_connect) bl.sendData(String.valueOf(commandHorn+"0\r"));
//	    		}
//	    	}
//	    });
		
		mHandler.postDelayed(sRunnable, 600000);
		
	}
		
    private static class MyHandler extends Handler {
        private final WeakReference<ActivityButtons> mActivity;
     
        public MyHandler(ActivityButtons activity) {
          mActivity = new WeakReference<ActivityButtons>(activity);
        }
     
        @Override
        public void handleMessage(Message msg) {
        	ActivityButtons activity = mActivity.get();
        	if (activity != null) {
        		switch (msg.what) {
	            case cBluetooth.BL_NOT_AVAILABLE:
	               	Log.d(cBluetooth.TAG, "Bluetooth is not available. Exit");
	            	Toast.makeText(activity.getBaseContext(), "Bluetooth is not available", Toast.LENGTH_SHORT).show();
	                activity.finish();
	                break;
	            case cBluetooth.BL_INCORRECT_ADDRESS:
	            	Log.d(cBluetooth.TAG, "Incorrect MAC address");
	            	Toast.makeText(activity.getBaseContext(), "Incorrect Bluetooth address", Toast.LENGTH_SHORT).show();
	                break;
//	            case cBluetooth.BL_REQUEST_ENABLE:
//	            	Log.d(cBluetooth.TAG, "Request Bluetooth Enable");
//	            	BluetoothAdapter.getDefaultAdapter();
//	            	Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//	            	activity.startActivityForResult(enableBtIntent, 1);
//	                break;
	            case cBluetooth.BL_SOCKET_FAILED:
	            	Toast.makeText(activity.getBaseContext(), "Socket failed", Toast.LENGTH_SHORT).show();
	            	//activity.finish();
	                break;
	          	}
          	}
        }
	}
     
	private final MyHandler mHandler = new MyHandler(this);
     
	private final static Runnable sRunnable = new Runnable() {
		public void run() { }
	};
	
    private void loadPref(){
    	SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);  
    	address = mySharedPreferences.getString("pref_MAC_address", address);			// the first time we load the default values
    	pwmBtnMotorLeft = Integer.parseInt(mySharedPreferences.getString("pref_pwmBtnMotorLeft", String.valueOf(pwmBtnMotorLeft)));
    	pwmBtnMotorRight = Integer.parseInt(mySharedPreferences.getString("pref_pwmBtnMotorRight", String.valueOf(pwmBtnMotorRight)));
    	commandLeft = mySharedPreferences.getString("pref_commandLeft", commandLeft);
    	commandRight = mySharedPreferences.getString("pref_commandRight", commandRight);
    	commandHorn = mySharedPreferences.getString("pref_commandHorn", commandHorn);
	}
    
    @Override
    protected void onResume() {
    	super.onResume();
//    	BT_is_connect = bl.BT_Connect(address, false);
    }

    @Override
    protected void onPause() {
    	super.onPause();
//    	bl.BT_onPause();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	loadPref();
    }


    private void connectWebSocket() {
        URI uri;
        try {
            uri = new URI("ws://192.168.0.37:81/");
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
                    }
                });
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
			mWebSocketClient.connect();
		}
    }

    public void sendMessageLF(View view) {
//        EditText editText = (EditText)findViewById(R.id.message);
//        mWebSocketClient.send(editText.getText().toString());
        String str1 = "200=0=;";
        int action1 = 200;
        int action2 = 0;
        mWebSocketClient.send(String.format("%s\n", str1));
//        mWebSocketClient.send(String.format("%d=%d=\n", action1, action2));
//        editText.setText("");
    }

    public void sendMessageRF(View view) {
        String str1 = "0=200=;";
        int action1 = 0;
        int action2 = 200;
        mWebSocketClient.send(String.format("%s\n", str1));
//        mWebSocketClient.send(String.format("%d=%d=\n", action1, action2));
    }
    public void sendMessageLB(View view) {
        String str1 = "-200=0=;";
        int action1 = -200;
        int action2 = 0;
        mWebSocketClient.send(String.format("%s\n", str1));
//        mWebSocketClient.send(String.format("%d=%d=\n", action1, action2));
    }
    public void sendMessageRB(View view) {
        String str1 = "0=-200=;";
        int action1 = 0;
        int action2 = -200;
        mWebSocketClient.send(String.format("%s\n", str1));
//        mWebSocketClient.send(String.format("%d=%d=\n", action1, action2));
    }


}
