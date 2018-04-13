package com.pedro.virtualcontroller;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.ResourceBundle;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class ControllerMainActivity extends AppCompatActivity {

	private ControllerFragment joystickControllerFragment;
	private TextView textViewAngle;
	private TextView textViewStrength;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_controller_main);


		joystickControllerFragment = (ControllerFragment) getSupportFragmentManager().findFragmentById(R.id.joystick_fragment);
		textViewAngle = findViewById(R.id.textViewAngleRemote);
		textViewStrength = findViewById(R.id.textViewStrengthRemote);

		JoystickView.OnMoveListener listener = new JoystickView.OnMoveListener() {
			@Override
			public void onMove(int angle, int strength) {
				// do whatever you want
				joystickControllerFragment.setAngleLabel(angle);
				joystickControllerFragment.setStrengthLabel(strength);

				setStrengthLabel(strength);
				setAngleLabel(angle);
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

}
