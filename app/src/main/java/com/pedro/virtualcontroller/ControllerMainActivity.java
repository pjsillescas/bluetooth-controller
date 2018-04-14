package com.pedro.virtualcontroller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.pedro.virtualcontroller.activities.DeviceListActivity;
import com.pedro.virtualcontroller.fragments.ControllerFragment;
import com.pedro.virtualcontroller.services.BluetoothControllerService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class ControllerMainActivity extends AppCompatActivity {

	// Message types sent from the BluetoothControllerService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothControllerService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	private ControllerFragment joystickControllerFragment;
	private TextView textViewAngle;
	private TextView textViewStrength;
	private BluetoothControllerService controllerService;
	private String connectedDeviceName;
	private TextView textViewConnectedDevice;

	private BluetoothAdapter bluetoothAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_controller_main);

		setupJoystick();

		setupBluetooth();
	}


	private boolean setupBluetooth()
	{
		boolean output = true;
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// If the adapter is null, then Bluetooth is not supported

		if (bluetoothAdapter == null) {
			Toast.makeText(this, R.string.bluetooth_is_not_available, Toast.LENGTH_LONG).show();
			finish();
			output = false;
		}

		return output;
	}

	private void setupJoystick()
	{
		joystickControllerFragment = (ControllerFragment) getSupportFragmentManager().findFragmentById(R.id.joystick_fragment);
		textViewAngle = findViewById(R.id.textViewAngleRemote);
		textViewStrength = findViewById(R.id.textViewStrengthRemote);
		textViewConnectedDevice = findViewById(R.id.textViewConnectedDevice);

		JoystickView.OnMoveListener listener = new JoystickView.OnMoveListener() {
			@Override
			public void onMove(int angle, int strength) {
				// do whatever you want
				joystickControllerFragment.setAngleLabel(angle);
				joystickControllerFragment.setStrengthLabel(strength);

				sendMessage("" + strength + "," + angle);

				//setStrengthLabel(strength);
				//setAngleLabel(angle);
			}
		};

		joystickControllerFragment.setOnMoveListener(listener);
	}

	public void setAngleLabel(int angle)
	{
		String angleString = String.format("%s %d deg.",getString(R.string.remote_angle_string),angle);
		textViewAngle.setText(angleString);
	}

	public void setStrengthLabel(int strength)
	{
		String strengthString = String.format("%s %d %%",getString(R.string.remote_strength_string),strength);
		textViewStrength.setText(strengthString);
	}

	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	@Override
	protected void onStart() {
		super.onStart();

		if (!bluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		} else {
			if (controllerService == null) setupController();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (controllerService != null) {
			if (controllerService.getState() == BluetoothControllerService.STATE_NONE) {
				controllerService.start();
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if(controllerService != null) controllerService.stop();
	}

	private void ensureDiscoverable() {
		if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	private void setupController()
	{
		controllerService = new BluetoothControllerService(this,handler);
	}

	private void sendMessage(String message) {

		// Check that we're actually connected before trying anything
		if (controllerService.getState() != BluetoothControllerService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
			return;
		}
		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothChatService to write
			byte[] send = message.getBytes();
			controllerService.write(send);
		}
	}

	// The Handler that gets information back from the BluetoothChatService
	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			com.pedro.virtualcontroller.utils.Message message;

			switch (msg.what) {
				case MESSAGE_WRITE:
					byte[] writeBuf = (byte[]) msg.obj;
					// construct a string from the buffer
					String writeMessage = new String(writeBuf);
					message = new com.pedro.virtualcontroller.utils.Message(writeMessage);

					joystickControllerFragment.setAngleLabel(message.getAngle());
					joystickControllerFragment.setStrengthLabel(message.getStrength());

					break;
				case MESSAGE_READ:
					byte[] readBuf = (byte[]) msg.obj;
					// construct a string from the valid bytes in the buffer
					String readMessage = new String(readBuf, 0, msg.arg1);
					message = new com.pedro.virtualcontroller.utils.Message(readMessage);

					setAngleLabel(message.getAngle());
					setStrengthLabel(message.getStrength());

					break;
				case MESSAGE_DEVICE_NAME:
					// save the connected device's name
					connectedDeviceName = msg.getData().getString(DEVICE_NAME);
					Toast.makeText(getApplicationContext(), getString(R.string.connected_to)
							+ " " + connectedDeviceName, Toast.LENGTH_SHORT).show();
					textViewConnectedDevice.setText(connectedDeviceName);
					break;
				case MESSAGE_TOAST:
					Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
							Toast.LENGTH_SHORT).show();
					break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_CONNECT_DEVICE:
				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK) {
					// Get the device MAC address
					String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
					// Get the BLuetoothDevice object
					BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
					// Attempt to connect to the device
					controllerService.connect(device);
				}
				break;
			case REQUEST_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if (resultCode == Activity.RESULT_OK) {
					// Bluetooth is now enabled, so set up a chat session
					Toast.makeText(this,"pozi",Toast.LENGTH_LONG).show();
					setupController();
				} else {
					// User did not enable Bluetooth or an error occured
					Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
					finish();
				}
		}
	}

	public void connect(View v) {
		Intent serverIntent = new Intent(this, DeviceListActivity.class);
		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	}

	public void discoverable(View v) {
		ensureDiscoverable();
	}
}
