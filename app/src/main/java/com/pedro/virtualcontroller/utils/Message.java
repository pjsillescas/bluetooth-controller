package com.pedro.virtualcontroller.utils;

public class Message {
	private int angle;
	private int strength;
	private String data;

	public Message(String str)
	{
		data = str;
		String[] chunks = str.split(",");

		strength = Integer.parseInt(chunks[0]);
		angle = Integer.parseInt(chunks[1]);
	}

	public Message(int strength,int angle)
	{
		this.strength = strength;
		this.angle = angle;

		this.data = String.format("%d,%d",strength,angle);
	}

	public int getAngle() {
		return angle;
	}

	public void setAngle(int angle) {
		this.angle = angle;
	}

	public int getStrength() {
		return strength;
	}

	public void setStrength(int strength) {
		this.strength = strength;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}
