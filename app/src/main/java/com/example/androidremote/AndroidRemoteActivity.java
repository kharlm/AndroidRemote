/*
 * Android application for remote control of Arduino Robot
 * By Matthieu Varagnat, 2013
 * 
 * This application connects over bluetooth to an Arduino, and sends commands
 * It also receives confirmation messages and displays them in a log
 * 
 * Shared under Creative Common Attribution licence * 
 */

package com.example.androidremote;

import android.app.Activity;

import com.example.androidremote.JoystickView.OnJoystickMoveListener;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.webkit.WebView;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;


import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;



import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;
import static android.graphics.Color.WHITE;
import static com.example.androidremote.R.id.webView;

//import static com.example.androidremote.R.id.seekBar;

public class AndroidRemoteActivity extends Activity implements OnTouchListener {
    private JoystickView m = null;
    int mode;
    public Handler mHandler;
	private TextView logview;
    private TextView mProgressText;
	private Button connect, deconnect;
    private SeekBar seek;
	private ImageView forwardArrow, backArrow, rightArrow, leftArrow, stop;

	private BluetoothAdapter mBluetoothAdapter = null;
    private TextView directionTextView;
	private String[] logArray = null;
    private TextView angleTextView;
    private TextView powerTextView;
    private TextView status;
    static WebView webView;
    int reverse;
    boolean Forwardtouch;
    boolean Backtouch;
    boolean touch;



    private JoystickView joystick;


	private BtInterface bt = null;

	static final String TAG = "Chihuahua";
	static final int REQUEST_ENABLE_BT = 3;

	//This handler listens to messages from the bluetooth interface and adds them to the log
	final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            String data = msg.getData().getString("receivedData");
            addToLog(data);
        }
    };

    //this handler is dedicated to the status of the bluetooth connection
    final Handler handlerStatus = new Handler() {
        public void handleMessage(Message msg) {
            int status = msg.arg1;
            if(status == BtInterface.CONNECTED) {
            	addToLog("Connected");
            } else if(status == BtInterface.DISCONNECTED) {
            	addToLog("Disconnected");
            }
        }
    };

    //handles the log view modification
    //only the most recent messages are shown
    private void addToLog(String message){
    	for (int i = 1; i < logArray.length; i++){
        	logArray[i-1] = logArray[i];
        }
        logArray[logArray.length - 1] = message;

        logview.setText("");
        for (int i = 0; i < logArray.length; i++){
        	if (logArray[i] != null){
        		logview.append(logArray[i] + "\n");
        	}
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_android_remote);



        //first, inflate all layout objects, and set click listeners

        logview = (TextView)findViewById(R.id.logview);
        //I chose to display only the last 3 messages
        logArray = new String[3];

        webView = (WebView)findViewById(R.id.webView);
        connect = (Button)findViewById(R.id.connect);
        connect.setOnTouchListener(this);
        mHandler=new Handler();
        Switch onOff = (Switch)  findViewById(R.id.auto);

        deconnect = (Button)findViewById(R.id.deconnect);
        deconnect.setOnTouchListener(this);
        directionTextView = (TextView) findViewById(R.id.directionTextView);
        joystick = (JoystickView) findViewById(R.id.joystickView);
        angleTextView = (TextView) findViewById(R.id.angleTextView);
        powerTextView = (TextView) findViewById(R.id.powerTextView);
        //status = (TextView) findViewById(R.id.status);



        //seek = (SeekBar)findViewById(R.id.seekBar); // make seekbar object
        View.OnClickListener listener;

        //mProgressText = (TextView)findViewById(R.id.progress);

        //forwardArrow = (ImageView)findViewById(R.id.forward_arrow);
       // forwardArrow.setOnTouchListener(this);
        backArrow = (ImageView)findViewById(R.id.back_arrow);
        backArrow.setOnTouchListener(this);
       /* */
        /*
        rightArrow = (ImageView)findViewById(R.id.right_arrow);
        rightArrow.setOnTouchListener(this);
        leftArrow = (ImageView)findViewById(R.id.left_arrow);
        leftArrow.setOnTouchListener(this);
        stop = (ImageView)findViewById(R.id.stop);
        stop
.setOnTouchListener(this);

*/

        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        //String ssid = wifiInfo.getSSID();
        String ssid;
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
//
        if (mWifi.isConnected()) {
            //ssid="Hello";
           ssid = wifiInfo.getSSID();
            Toast.makeText(this,ssid, Toast.LENGTH_SHORT).show();
            if (ssid == "Hello") {
                //Toast.makeText(this, "CONNECTED: ", Toast.LENGTH_SHORT).show();
            }

            //webView.loadUrl("https://www.google.com/");
            webView.loadUrl("http://192.168.42.1:5000");
        }


        if (mWifi.isConnected()!=true) {
            Toast.makeText( this, "Please connect to MYPI WIFI " , Toast.LENGTH_LONG ).show();
        }
        onOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mode=1;
                    bt.sendData("<mode="+String.valueOf(mode)+","+"<angle="+String.valueOf(0)+","+"velo="+String.valueOf(0)+">");
                }

                else {
                    mode =0;
                    bt.sendData("<mode="+String.valueOf(mode)+","+"<angle="+String.valueOf(0)+","+"velo="+String.valueOf(0)+">");
                }

            }
        });
        joystick.setOnJoystickMoveListener(new OnJoystickMoveListener() {

            @Override
            public void onValueChanged(int angle, int power, int direction) {
                // TODO Auto-generated method stub

                touch=true;

               // angleTextView.setText(" " + String.valueOf(angle) + "°");


                if(angle>0){


                    if(angle>90){


                        //addToLog("<mode="+String.valueOf(mode)+","+"<angle="+String.valueOf(180-angle)+","+"velo="+String.valueOf(power)+">");
                         bt.sendData("<mode="+String.valueOf(mode)+","+"<angle="+String.valueOf(180-angle)+","+"velo="+String.valueOf(power)+">");
                    }
                    else{

                        //addToLog("<mode="+String.valueOf(mode)+","+"<angle="+String.valueOf(angle)+","+"velo="+String.valueOf(power)+">");
                         bt.sendData("<mode="+String.valueOf(mode)+","+"<angle="+String.valueOf(angle)+","+"velo="+String.valueOf(power)+">");
                    }


                }

                else {

                    if(angle<-90){

                        //addToLog("<mode="+String.valueOf(mode)+","+"<angle=" + String.valueOf(-180-angle) + "," + "velo=" + String.valueOf(power) + ">");
                         bt.sendData("<mode="+String.valueOf(mode)+","+"<angle=" + String.valueOf(-180-angle) + "," + "velo=" + String.valueOf(power) + ">");
                    }
                    else{

                        //addToLog("<mode="+String.valueOf(mode)+","+"<angle=" + String.valueOf(angle) + "," + "velo=" + String.valueOf(power) + ">");
                        bt.sendData("<mode="+String.valueOf(mode)+","+"<angle=" + String.valueOf(angle) + "," + "velo=" + String.valueOf(power) + ">");
                    }

                }

//                powerTextView.setText(" " + String.valueOf(power) + "%");
                /*switch (direction) {
                    case JoystickView.FRONT:
                        directionTextView.setText("Front");
                        break;
                    case JoystickView.FRONT_RIGHT:
                        directionTextView.setText("Front Right");
                        break;
                    case JoystickView.RIGHT:
                        directionTextView.setText("Right");
                        break;
                    case JoystickView.RIGHT_BOTTOM:
                        directionTextView.setText("Bottom Right");
                        break;
                    case JoystickView.BOTTOM:
                        directionTextView.setText("Bottom");
                        break;
                    case JoystickView.BOTTOM_LEFT:
                        directionTextView.setText("Bottom Left");
                        break;
                    case JoystickView.LEFT:
                        directionTextView.setText("Left");
                        break;
                    case JoystickView.LEFT_FRONT:
                        directionTextView.setText("Front Left");
                        break;
                    default:
                        directionTextView.setText("Center");
                }
                */

            }

        }, JoystickView.DEFAULT_LOOP_INTERVAL);






       // seek.setOnSeekBarChangeListener(this);
/*        seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                bt.sendData("#");
                // TODO Auto-generated method stub

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {

                mProgressText.setText(String.valueOf(seek.getProgress()));

                if(progress>=0 && progress<=50) {

                    bt.sendData("<dir=B,"+"vel="+seek.getProgress()+">");

                    addToLog("Works");

                }

                if(progress>=51 && progress<=100) {

                    bt.sendData("<dir=F,"+"vel="+seek.getProgress()+">");

                    addToLog("Works");

                }
                // TODO Auto-generated method stub



            }
        });
*/


    }










    //it is better to handle bluetooth connection in onResume (ie able to reset when changing screens)
    @Override
    public void onResume() {
        super.onResume();


        webView = (WebView)findViewById(R.id.webView);
        webView.clearCache(true);
        webView.getSettings().setJavaScriptEnabled(true);

        //webView.loadUrl("http://192.168.42.1:5000");
        //webView.loadUrl("http://192.168.42.1:5000");
        View v = webView;
        v.setDrawingCacheEnabled(true);



//cheat to force webview to draw itself
        /*v.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
        v.buildDrawingCache(true);
        Bitmap b = Bitmap.createBitmap(v.getDrawingCache());
        v.setDrawingCacheEnabled(false);
        Log.i(TAG,"Exiting onCreate");

        */

        //webView.loadUrl("https://www.google.com/");
        //first of all, we check if there is bluetooth on the phone
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        	Log.v(TAG, "Device does not support Bluetooth");
        }
		else{
        	//Device supports BT
        	if (!mBluetoothAdapter.isEnabled()){
        		//if Bluetooth not activated, then request it
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        	}
        	else{
            	//BT activated, then initiate the BtInterface object to handle all BT communication
        		bt = new BtInterface(handlerStatus, handler);
            }
        }
    }

    //called only if the BT is not already activated, in order to activate it
	protected void onActivityResult(int requestCode, int resultCode, Intent moreData){
    	if (requestCode == REQUEST_ENABLE_BT){
    		if (resultCode == Activity.RESULT_OK){
    			//BT activated, then initiate the BtInterface object to handle all BT communication
    			bt = new BtInterface(handlerStatus, handler);
    		}
    		else if (resultCode == Activity.RESULT_CANCELED)
    			Log.v(TAG, "BT not activated");
    		else
    			Log.v(TAG, "result code not known");
    	}
    	else{
    		Log.v(TAG, "request code not known");
    	}
     }

	//handles the clicks on various parts of the screen
	//all buttons launch a function from the BtInterface object



    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            switch (v.getId()) {
               /* case R.id.forward_arrow:
                    if (mHandler != null) return true;
                    mHandler = new Handler();
                    mHandler.postDelayed(forward, 500);
                    break;
                    */
                case R.id.connect:
                    addToLog("Trying to connect");
                    bt.connect();
                    break;
                case R.id.deconnect:
                    addToLog("Closing connection");
                    bt.close();
                    break;
                case R.id.back_arrow:
                    bt.sendData("<mode="+String.valueOf(mode)+","+"<angle=" + String.valueOf(100) + "," + "velo=" + String.valueOf(0) + ">");
                    /*if (mHandler != null) return true;
                    mHandler = new Handler();
                    mHandler.postDelayed(back, 500);
                    */
                    break;
                /*
                case R.id.left_arrow:
                    if (mHandler != null) return true;
                    mHandler = new Handler();
                    mHandler.postDelayed(left, 500);
                    break;
                case R.id.right_arrow:
                    if (mHandler != null) return true;
                    mHandler = new Handler();
                    mHandler.postDelayed(right, 500);
                    break;
                case R.id.stop:
                    bt.sendData("S");
                    break;
                    */
            }


        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            switch (v.getId()) {
               /* case R.id.forward_arrow:
                    if (mHandler == null) return true;
                    mHandler.removeCallbacks(forward);
                    mHandler = null;
                    break;
                    */
                case R.id.back_arrow:
                    bt.sendData("<mode="+String.valueOf(mode)+","+"<angle=" + String.valueOf(0) + "," + "velo=" + String.valueOf(0) + ">");
                    /*if (mHandler == null) return true;
                    mHandler.removeCallbacks(back);
                    mHandler = null;
                    */
                    break;
                /*
                case R.id.left_arrow:
                    if (mHandler == null) return true;
                    mHandler.removeCallbacks(left);
                    mHandler = null;
                    break;
                case R.id.right_arrow:
                    if (mHandler == null) return true;
                    mHandler.removeCallbacks(right);
                    mHandler = null;
                    break;
                    */
            }


        }
        return true;
    }



    Runnable forward = new Runnable() {
        @Override
        public void run() {
            bt.sendData("F");
            mHandler.postDelayed(this, 500);
        }
    };

    Runnable back = new Runnable() {
        @Override
        public void run() {

           // bt.sendData("B");
           // bt.sendData("<mode="+String.valueOf(mode)+","+"<angle=" + String.valueOf(100) + "," + "velo=" + String.valueOf(101) + ">");

            mHandler.postDelayed(this, 500);
        }
    };

    Runnable right = new Runnable() {
        @Override
        public void run() {
            bt.sendData("R");
            mHandler.postDelayed(this, 500);
        }
    };

    Runnable left = new Runnable() {
        @Override
        public void run() {
            bt.sendData("L");
            mHandler.postDelayed(this, 500);
        }
    };

    static public class ConnectionChangeReceiver extends BroadcastReceiver
    {

        @Override
        public void onReceive(Context context, Intent intent )
        {

            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

            if(info.isConnected()){
               // webView.loadUrl("https://www.google.com/");

                webView.loadUrl("http://192.168.42.1:5000");

            }
                else{
                    Toast.makeText( context, "Please connect to the MYPI WIFI", Toast.LENGTH_SHORT ).show();
                }

                //To check the Network Name or other info:
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ssid = wifiInfo.getSSID();


        }


    }



}

