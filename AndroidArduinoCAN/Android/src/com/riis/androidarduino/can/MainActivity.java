package com.riis.androidarduino.can;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.riis.androidarduino.lib.BluetoothComm;

public class MainActivity extends Activity {
	private static String DEVICE_NAME = "AndroidArduinoCANBT";
	private static final int MAX_ARRAY_SIZE = 1000;
	
	private Button btConnectButton;
	private Button startTrackingButton;
	private Button stopTrackingButton;
	private Button pauseTrackingButton;
	private boolean enableTracking;
	
	private double timeSinceTrackStart;
	private ArrayList<Double> avgRPM;
	private ArrayList<Double> avgSpeed;
	
	private GuageView speedometer;
	private GuageView tachometer;
	
	private TextView engineRunTime;
	private TextView airTemp;
	private TextView hybridBatteryPack;
	private TextView VIN;
	
	private ProgressBar oilTempBar;
	private TextView oilTempTxt;
	private ProgressBar coolantTempBar;
	private TextView coolantTempTxt;
	private ProgressBar throttlePosBar;
	private TextView throttlePosTxt;
	private ProgressBar absThrottleBBar;
	private TextView absThrottleBTxt;
	private ProgressBar absThrottleCBar;
	private TextView absThrottleCTxt;
	private ProgressBar absAccPosDBar;
	private TextView absAccPosDTxt;
	private ProgressBar absAccPosEBar;
	private TextView absAccPosETxt;
	private ProgressBar absAccPosFBar;
	private TextView absAccPosFTxt;
	
	private volatile boolean keepRunning;
	private boolean lastStatus;
	private Thread msgThread;
	
	private static Handler handler;
	private Context context;

	private BluetoothComm btComm;
	//private ArrayList<double> readings;
	
	private Runnable msgUpdateThread = new Runnable() { 
		public void run() {
			while(keepRunning) {
				if(btComm.isConnected()) {
					if(!lastStatus) {
						lastStatus = true;
						changeConnectButtonText();
					}
					
					if(btComm.isMessageReady()) {
						String data = btComm.readMessage();
						parseCANData(data);
					}
		        	
		        } else {
		        	if(lastStatus) {
						lastStatus = false;
						changeConnectButtonText();
					}
		        }
				
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        keepRunning = true;
        lastStatus = false;
        
        context = this;
        avgRPM = new ArrayList<Double>(MAX_ARRAY_SIZE);
        avgSpeed = new ArrayList<Double>(MAX_ARRAY_SIZE);
        enableTracking = false;
        
        setUpGUI();
    }
    
    private void setUpGUI() {
    	setUpConnectButton();
    	setUpStartTrackButton();
    	setUpPauseTrackButton();
    	setUpStopTrackButton();
    	setUpTempMonitors();
    	setUpThrottleMonitors();
    	setUpAcceleratorMonitors();
    	setUpBatteryMonitor();
    	setUpRuntimeMonitor();
    	setUpVIN();
    	setUpSpeedometer();
    	setupHandler();
    }

	private void setUpConnectButton() {
    	btConnectButton = (Button)findViewById(R.id.connectButton);
    	btConnectButton.setOnClickListener(
    		new OnClickListener() {
    			public void onClick(View v) {
    				if(btComm.isConnected())
    					btComm.disconnect();
    				else
    					btComm.connect();
    			}
    		}
    	);
    }
	
	private void changeConnectButtonText() {
		if(btComm.isConnected())
			btConnectButton.setText("Disconnect");
		else
			btConnectButton.setText("Connect");
	}
    
    private void setUpStartTrackButton() {
    	startTrackingButton = (Button)findViewById(R.id.startTrackButton);
    	startTrackingButton.setOnClickListener(
    		new OnClickListener() {
    			public void onClick(View v) {
    				enableTracking = true;
    			}
    		}
    	);
	}

	private void setUpPauseTrackButton() {
		pauseTrackingButton = (Button)findViewById(R.id.pauseTrackButton);
    	pauseTrackingButton.setOnClickListener(
    		new OnClickListener() {
    			public void onClick(View v) {
    				enableTracking = false;
    			}
    		}
    	);
	}

	private void setUpStopTrackButton() {
		stopTrackingButton = (Button)findViewById(R.id.stopTrackButton);
    	stopTrackingButton.setOnClickListener(
    		new OnClickListener() {
    			public void onClick(View v) {
    				enableTracking = false;
    				avgRPM.clear();
    				avgSpeed.clear();
    			}
    		}
    	);
	}

	private void setUpTempMonitors() {
		oilTempBar = (ProgressBar)findViewById(R.id.oilTempBar);
		oilTempBar.setMax(255);
		oilTempBar.setProgress(40);
		oilTempTxt = (TextView)findViewById(R.id.oilTempTxt);
		oilTempTxt.append("0 C");
		
		coolantTempBar = (ProgressBar)findViewById(R.id.coolantTempBar);
		coolantTempBar.setMax(255);
		coolantTempBar.setProgress(40);
		coolantTempTxt = (TextView)findViewById(R.id.coolantTempTxt);
		coolantTempTxt.append("0 C");
		
		airTemp = (TextView)findViewById(R.id.airTemp);
		airTemp.append("0 C");
	}

	private void setUpThrottleMonitors() {
		throttlePosBar = (ProgressBar)findViewById(R.id.throttlePosBar);
		throttlePosBar.setMax(100);
		throttlePosTxt = (TextView)findViewById(R.id.throttlePosTxt);
		throttlePosTxt.append("0%");
		
		absThrottleBBar = (ProgressBar)findViewById(R.id.absoluteThrottleBBar);
		absThrottleBBar.setMax(100);
		absThrottleBTxt = (TextView)findViewById(R.id.absoluteThrottleBTxt);
		absThrottleBTxt.append("0%");
		
		absThrottleCBar = (ProgressBar)findViewById(R.id.absoluteThrottleCBar);
		absThrottleCBar.setMax(100);
		absThrottleCTxt = (TextView)findViewById(R.id.absoluteThrottleCTxt);
		absThrottleCTxt.append("0%");
	}

	private void setUpAcceleratorMonitors() {
		absAccPosDBar = (ProgressBar)findViewById(R.id.absAccPosDBar);
		absAccPosDBar.setMax(100);
		absAccPosDTxt = (TextView)findViewById(R.id.absAccPosDTxt);
		absAccPosDTxt.append("0%");
		
		absAccPosEBar = (ProgressBar)findViewById(R.id.absAccPosEBar);
		absAccPosEBar.setMax(100);
		absAccPosETxt = (TextView)findViewById(R.id.absAccPosETxt);
		absAccPosETxt.append("0%");
		
		absAccPosFBar = (ProgressBar)findViewById(R.id.absAccPosFBar);
		absAccPosFBar.setMax(100);
		absAccPosFTxt = (TextView)findViewById(R.id.absAccPosFTxt);
		absAccPosFTxt.append("0%");
	}

	private void setUpBatteryMonitor() {
		hybridBatteryPack = (TextView)findViewById(R.id.hybridBatteryPack);
	}
	
	private void setUpVIN() {
		VIN = (TextView)findViewById(R.id.VIN);
	}
	
	private void setUpRuntimeMonitor() {
		engineRunTime = (TextView)findViewById(R.id.engineRunTime);
		engineRunTime.append("0s");
	}
	
	private void setUpSpeedometer() {
		speedometer = (GuageView) findViewById(R.id.speedometer);
	}
    
    private void setupHandler() {
		handler = new Handler() {
			public void handleMessage(Message msg) {
				String taggedMessage = (String) msg.obj;
				String[] tokens = taggedMessage.split("~");
				
				String message = tokens[1];
				if(tokens[0].equals("LOG")) {
					
				} else if(tokens[0].equals("DATA")) {
			    	if(Integer.parseInt(tokens[1]) == 0x02)
			    		setVIN(tokens[2]);
			    	else if(Integer.parseInt(tokens[1]) == 0x04)
			    		setEngineLoadVal(tokens[2]);
			    	else if(Integer.parseInt(tokens[1]) == 0x05)
			    		setEngineCoolantTemp(tokens[2]);
			    	else if(Integer.parseInt(tokens[1]) == 0x0C)
			    		setEngineRPM(tokens[2]);
			    	else if(Integer.parseInt(tokens[1]) == 0x0D)
			    		setVehicleSpeed(tokens[2]);
			    	else if(Integer.parseInt(tokens[1]) == 0x11)
			    		setThrottlePosition(tokens[2]);
			    	else if(Integer.parseInt(tokens[1]) == 0x1F)
			    		setEngineRunTime(tokens[2]);
			    	else if(Integer.parseInt(tokens[1]) == 0x2F)
			    		setFuelLevelInput(tokens[2]);
			    	else if(Integer.parseInt(tokens[1]) == 0x46)
			    		setAmbiantAirTemp(tokens[2]);
			    	else if(Integer.parseInt(tokens[1]) == 0x47)
			    		setAbsoluteThrottleB(tokens[2]);
			    	else if(Integer.parseInt(tokens[1]) == 0x48)
			    		setAbsoluteThrottleC(tokens[2]);
			    	else if(Integer.parseInt(tokens[1]) == 0x49)
			    		setAccPedalPosD(tokens[2]);
			    	else if(Integer.parseInt(tokens[1]) == 0x4A)
			    		setAccPedalPosE(tokens[2]);
			    	else if(Integer.parseInt(tokens[1]) == 0x4B)
			    		setAbsAccPedalPosF(tokens[2]);
			    	else if(Integer.parseInt(tokens[1]) == 0x5B)
			    		setHybridBatteryPackLife(tokens[2]);
			    	else if(Integer.parseInt(tokens[1]) == 0x5C)
			    		setEngineOilTemp(tokens[2]);
				}
			}
		};
	}
    
    private void setVIN(String vin) {
    	VIN.setText(getString(R.string.VINPreface) + vin);
	}

	private void setEngineLoadVal(String substring) {
		// TODO Auto-generated method stub
		
	}

	private void setEngineCoolantTemp(String coolantTempStr) {
    	double coolantTemp = Double.parseDouble(coolantTempStr);
		coolantTempTxt.setText(getString(R.string.engineCoolantTempPreface) + coolantTemp + " C");
		coolantTempBar.setProgress((int)coolantTemp+40);
	}

	private void setEngineRPM(String rpmStr) {
		double rpm = Double.parseDouble(rpmStr);
		
		if(enableTracking) {
			if(avgRPM.size() == MAX_ARRAY_SIZE)
				avgRPM.remove(0);
			avgRPM.add(rpm);
		}
		
		double newAvg = getAvg(avgRPM);
		
		tachometer.setValue(Float.parseFloat(rpmStr));
		
		// calculate angle from rpm
		// -turn into a %- rpm/maxVal
		// zeroAngle + (rpm*maxValAngle)
		// if(result < 0)
		//    result = 360+result;
		// calculate angle from newAvg
	}

	private void setVehicleSpeed(String speedStr) {
		double speed = Double.parseDouble(speedStr);
		
		if(enableTracking) {
			if(avgSpeed.size() == MAX_ARRAY_SIZE)
				avgSpeed.remove(0);
			avgSpeed.add(speed);
		}
		
		double newAvg = getAvg(avgSpeed);
		
		speedometer.setValue(Float.parseFloat(speedStr));
		
		// calculate angle from speed
		// calculate angle from newAvg
	}

	private double getAvg(ArrayList<Double> dataList) {
		double avg = 0;
		for(double datum : dataList) {
			avg += datum;
		}
		avg /= dataList.size();
		return avg;
	}

	private void setThrottlePosition(String throttlePosStr) {
		double throttlePos = Double.parseDouble(throttlePosStr);
		throttlePosTxt.setText(getString(R.string.throttlePosPreface) + throttlePos + "%");
		throttlePosBar.setProgress((int)throttlePos);
	}

	private void setEngineRunTime(String timeStr) {
		engineRunTime.setText(getString(R.string.engineRunTimePreface) + timeStr + "s");
	}

	private void setFuelLevelInput(String substring) {
		// TODO Auto-generated method stub
		
	}

	private void setAmbiantAirTemp(String tempStr) {
		airTemp.setText(getString(R.string.airTempPreface) + Double.parseDouble(tempStr) + " C");
	}

	private void setAbsoluteThrottleB(String absThrottleBStr) {
		double absThrottleB = Double.parseDouble(absThrottleBStr);
		absThrottleBTxt.setText(getString(R.string.absThrottleBPreface) + absThrottleB + "%");
		absThrottleBBar.setProgress((int)absThrottleB);
	}

	private void setAbsoluteThrottleC(String absThrottleCStr) {
		double absThrottleC = Double.parseDouble(absThrottleCStr);
		absThrottleCTxt.setText(getString(R.string.absThrottleCPreface) + absThrottleC + "%");
		absThrottleCBar.setProgress((int)absThrottleC);
	}

	private void setAccPedalPosD(String absAccPosDStr) {
		double absAccPosD = Double.parseDouble(absAccPosDStr);
		absAccPosDTxt.setText(getString(R.string.accPedalPosDPreface) + absAccPosD + "%");
		absAccPosDBar.setProgress((int)absAccPosD);
	}

	private void setAccPedalPosE(String absAccPosEStr) {
		double absAccPosE = Double.parseDouble(absAccPosEStr);
		absAccPosETxt.setText(getString(R.string.accPedalPosEPreface) + absAccPosE + "%");
		absAccPosEBar.setProgress((int)absAccPosE);
	}

	private void setAbsAccPedalPosF(String absAccPosFStr) {
		double absAccPosF = Double.parseDouble(absAccPosFStr);
		absAccPosFTxt.setText(getString(R.string.accPedalPosFPreface) + absAccPosF + "%");
		absAccPosFBar.setProgress((int)absAccPosF);
	}

	private void setHybridBatteryPackLife(String batteryLifeStr) {
		double batteryLife = Double.parseDouble(batteryLifeStr);
		hybridBatteryPack.setText(getString(R.string.hybridBatteryPackPreface) + batteryLife + "%");
	}

	private void setEngineOilTemp(String oilTempStr) {
    	double oilTemp = Double.parseDouble(oilTempStr);
		oilTempTxt.setText(getString(R.string.engineOilTempPreface) + oilTemp + " C");
		oilTempBar.setProgress((int)oilTemp+40);
    }
    
    private void parseCANData(String data) {
    	Message msg = Message.obtain(handler);
		msg.obj = "DATA~" + data;
		handler.sendMessage(msg);
    }
    
    @Override
	public void onResume() {
		super.onResume();
		
		if(btComm == null) {
			btComm = new BluetoothComm(this, DEVICE_NAME);
		} else {
			btComm.resumeConnection();
		}
		
		btComm.shouldPrintLogMsgs(false);
		msgThread = new Thread(msgUpdateThread);
		msgThread.start();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		keepRunning = false;
		btComm.pauseConnection();
	}
    
    public BluetoothComm getBlueToothComm() {
    	return btComm;
    }
}