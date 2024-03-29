package com.barobot.hardware;

import java.io.IOException;

import android.app.Activity;

import com.barobot.AppInvoker;
import com.barobot.android.AndroidBarobotState;
import com.barobot.android.BarobotAndroidConnector;
import com.barobot.common.Initiator;
import com.barobot.common.constant.Constant;
import com.barobot.common.interfaces.HardwareState;
import com.barobot.common.interfaces.serial.SerialEventListener;
import com.barobot.common.interfaces.serial.SerialInputListener;
import com.barobot.common.interfaces.serial.Wire;
import com.barobot.hardware.devices.BarobotConnector;
import com.barobot.hardware.devices.MyRetReader;
import com.barobot.hardware.serial.Serial_wire2;
import com.barobot.parser.utils.Interval;

public class Arduino{
	private static Arduino instance			= null;
	private Wire connection					= null;
	private Wire debugConnection			= null;
	public boolean stop_autoconnect			= false;
	public SerialInputListener listener;
	public static boolean firmwareUpload	= false;
	public BarobotConnector barobot;
	private HardwareState state;
	private Activity activity;
	public static Arduino getInstance(){
		return instance;
	}
	public Arduino(Activity main) {
		this.activity	= main;
		this.state		= new AndroidBarobotState(main);
		this.barobot	= new BarobotAndroidConnector( state );
		instance		= this;

		Interval ii1 = new Interval(new Runnable(){
			public void run() {
				// if is connected AND is not busy AND not uploading firmware
				if( Arduino.getInstance().getConnection().isConnected() && !barobot.main_queue.isBusy() && !firmwareUpload ){
					barobot.mb.send("AA\n");
				//	Arduino.getInstance().low_send("A2\n");
				//	barobot.main_queue.add("A2", true);
				}
			}});
		AppInvoker.getInstance().inters.add(ii1);
		ii1.run( 3000, 3000 );
	//	ii1.run( 3000, 500 );
	}

	public Wire getConnection(){
		return connection;
	}

	public void connect() {
		if( connection != null ){
			connection.close();
			connection = null;
		}
		connection		= new Serial_wire2( this.activity );
		connection.init();
		connection.setSerialEventListener( new SerialEventListener() {
			@Override
			public void onConnect() {
				barobot.state.set( "STAT2", barobot.state.getInt( "STAT2", 0 ) + 1 );				// serial start
				barobot.onConnected(barobot.main_queue, false );
			}
			@Override
			public void onClose() {
				AppInvoker.getInstance().onDisconnect();
			}
			@Override
			public void connectedWith(String bt_connected_device, String address) {
			}
		});
		listener				= barobot.willReadFrom( connection );
		barobot.willWriteThrough( connection );
		MyRetReader mrr			= new MyRetReader( barobot );
		barobot.mb.setRetReader( mrr );
	}
	/*
    private void prepareDebugConnection() {
		SerialInputListener btl = new SerialInputListener() {
		    @Override
		    public void onRunError(Exception e) {
		    }
		    @Override
		    public void onNewData(final byte[] data, int length) {
		    	String message = new String(data, 0, length);
		  //  	Log.e("Serial input", message);
		    	barobot.mb.read(message);
				try {
					Arduino.getInstance().low_send(message);
				} catch (IOException e) {
					Initiator.logger.appendError(e);
				}
		    }
		};
    	if(debugConnection !=null){
    		debugConnection.close();
    	}
		debugConnection = new BT_wire(this.mainView);
		debugConnection.setSerialEventListener( new SerialEventListener() {
			@Override
			public void onConnect() {
			}
			@Override
			public void onClose() {
			}
			@Override
			public void connectedWith(String bt_connected_device, String address) {
                state.set( "LAST_BT_DEVICE", address );    	// remember device ID
			}
		});	
		debugConnection.setOnReceive(btl);	
		debugConnection.init();
       	if( debugConnection.implementAutoConnect()){
      //  	this.runTimer(debugConnection);
        }
    //   	this.sendSomething();
	}*/
	public void destroy() {
		Initiator.logger.i("Arduino.destroy", "--- ON DESTROY1 ---");
		Initiator.logger.i("Arduino.destroy", "--- ON DESTROY2 ---");
		new Thread( new Runnable(){
			@Override
			public void run() {
				Initiator.logger.i("Arduino.destroy", "--- ON DESTROY3 ---");
		//		ahc					= null;
				Initiator.logger.i("Arduino.destroy", "--- ON DESTROY4 ---");
				barobot.destroy();
				Initiator.logger.i("Arduino.destroy", "--- ON DESTROY5 ---");
				instance			= null;
				if(connection!=null){
					connection.destroy();
				}
				Initiator.logger.i("Arduino.destroy", "--- ON DESTROY6 ---");
			}}).start();
		Initiator.logger.i("Arduino.destroy", "--- ON DESTROY7 ---");
		if(debugConnection!=null){
			debugConnection.destroy();
		}
		Initiator.logger.i("Arduino.destroy", "--- ON DESTROY8 ---");
	}

	public void resume() {
		if(connection!=null){
			connection.resume();
		}
		if(debugConnection!=null){
			debugConnection.resume();
		}
	}

	public boolean allowAutoconnect() {
		if( debugConnection == null ){
			//	Initiator.logger.i(Constant.no, "nie autoconnect bo juz połączony");
				return false;
		}
		if( debugConnection.isConnected() ){
		//	Initiator.logger.i(Constant.TAG, "no autoconnect bo juz połączony");
			return false;
		}
		if( !debugConnection.implementAutoConnect() ){
			Initiator.logger.i(Constant.TAG, "no autoconnect bo !canAutoConnect");
			return false;
		}
		if( !debugConnection.canConnect() ){
			Initiator.logger.i(Constant.TAG, "no autoconnect bo !canConnect");
			return false;
		}
		if (stop_autoconnect == true ) {
			Initiator.logger.i(Constant.TAG, "no autoconnect bo STOP");
			return false;
		}
		return true;
	}

    public boolean checkBT() {
    	if(debugConnection!= null){
    		return debugConnection.canConnect();
    	}
    	return false;
    }
	public void setupBT() {
		/*
		if(connection!=null){
			connection.setup();
			if(this.allowAutoconnect()){
				connection.setAutoConnect( true ); 
			}
		}*/
		if(debugConnection!=null){
			if(this.allowAutoconnect()){
				debugConnection.setAutoConnect( true ); 
			}
		}
	}
    public synchronized void low_send( String command ){		// wyslij bez interpretacji
		if(connection == null){
			return;
		}
    	try {
			connection.send(command);
		} catch (IOException e) {
			Initiator.logger.e("Arduino.low_send", command, e );
			e.printStackTrace();
		}
    }
    public synchronized void debug( String command ){		// wyslij bez interpretacji
		if(debugConnection!=null ){
			try {
				debugConnection.send(command);
			} catch (IOException e) {
				Initiator.logger.appendError(e);
			}
		}
    }
	public void connectId(String address) {
		Initiator.logger.i("Arduino.connectId", "autoconnect with: " +address);
		if(debugConnection!=null){
			debugConnection.connectToId(address);
		}
	}
	public void resetSerial() {
		if( connection != null ){
			connection.reset();
		}
	}
	public void renewSerial() {
		if( connection != null ){
			connection.destroy();
			connection = null;
		}
		this.connect();
	}
}
