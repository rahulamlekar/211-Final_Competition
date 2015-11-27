/*
 * File: Navigation.java
 * Written by: Sean Lawlor
 * ECSE 211 - Design Principles and Methods, Head TA
 * Fall 2011
 * Ported to EV3 by: Francois Ouellet Delorme
 * Fall 2015
 * 
 * Movement control class (turnTo, travelTo, flt, localize)
 */
import pollers.UltrasonicPoller;
import lejos.hardware.Sound;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class Navigation {

	final static int ACCELERATION = 1000;
	final int ROTATION_SPEED_FOR_LIGHTLOCALIZATION = 180;
	
	private int fast;
	private int slow;
	private Odometer odometer;
	private EV3LargeRegulatedMotor leftMotor, rightMotor;
	private double wheelRadius;
	private double wheelBase;
	private WallAvoider avoider;
	private UltrasonicPoller frontPoller;
	private double degreeError;
	private double cmError;
	
	public Navigation(Odometer odo, WallAvoider avoider,  UltrasonicPoller frontPoller, double wheelRadius, double wheelBase) {
		this.wheelRadius = wheelRadius;
		this.wheelBase = wheelBase;
		this.odometer = odo;
		this.avoider = avoider;
		this.frontPoller = frontPoller;

		EV3LargeRegulatedMotor[] motors = this.odometer.getMotors();
		this.leftMotor = motors[0];
		this.rightMotor = motors[1];

		// set acceleration
		this.leftMotor.setAcceleration(ACCELERATION);
		this.rightMotor.setAcceleration(ACCELERATION);
		degreeError = 2.0;
		cmError = 0.5;
		fast = 120;
		slow = 90;
	}
	public void setFastSpeed(int speed){
		this.fast = speed;
	}
	public void setSlowSpeed(int speed){
		this.slow = speed;
	}
	public void setCmError(double cmErr){
		cmError = cmErr;
	}
	public void setDegreeError(double deg){
		degreeError = deg;
	}

	/*
	 * Functions to set the motor speeds jointly
	 */
	public void setSpeeds(float lSpd, float rSpd) {
		this.leftMotor.setSpeed(lSpd);
		this.rightMotor.setSpeed(rSpd);
		if (lSpd < 0)
			this.leftMotor.backward();
		else
			this.leftMotor.forward();
		if (rSpd < 0)
			this.rightMotor.backward();
		else
			this.rightMotor.forward();
		
		if(lSpd == 0 && rSpd == 0){
			this.leftMotor.stop(true);
			this.rightMotor.stop(true);
		}
	}

	public void setSpeeds(int lSpd, int rSpd) {
		this.leftMotor.setSpeed(lSpd);
		this.rightMotor.setSpeed(rSpd);
		if (lSpd < 0)
			this.leftMotor.backward();
		else
			this.leftMotor.forward();
		if (rSpd < 0)
			this.rightMotor.backward();
		else
			this.rightMotor.forward();
		
		if(lSpd == 0 && rSpd == 0){
			this.leftMotor.stop(true);
			this.rightMotor.stop(true);
		}
	}

	/*
	 * Float the two motors jointly
	 */
	public void setFloat() {
		this.leftMotor.stop();
		this.rightMotor.stop();
		this.leftMotor.flt(true);
		this.rightMotor.flt(true);
	}

	/*
	 * TravelTo function which takes as arguments the x and y position in cm Will travel to designated position, while
	 * constantly updating it's heading
	 */
	public void travelTo(double x, double y) {
		double minAng;
		while (Math.abs(x - odometer.getX()) > cmError || Math.abs(y - odometer.getY()) > cmError) {
			
			
			minAng = (Math.atan2(y - odometer.getY(), x - odometer.getX())) * (180.0 / Math.PI);
			if (minAng < 0)
				minAng += 360.0;
			this.turnTo(minAng, false);
			this.setSpeeds(fast, fast);
			
		}
		this.setSpeeds(0, 0);
	}

	/*
	 * TravelToAndAvoid function which takes as arguments the x and y position in cm Will travel to designated position, while
	 * constantly updating it's heading. It also avoids blocks in the way
	 */
	public void travelToAndAvoid(double x, double y) {
		double minAng;
		while (Math.abs(x - odometer.getX()) > cmError || Math.abs(y - odometer.getY()) > cmError) {
			
			
			minAng = (Math.atan2(y - odometer.getY(), x - odometer.getX())) * (180.0 / Math.PI);
			if (minAng < 0)
				minAng += 360.0;
			this.turnTo(minAng, false);
			this.setSpeeds(fast, fast);
			
			//if we see a block coming up, RUN wallFollower.avoidWall();
			if(frontPoller.getUsData() < 13){
				Sound.beep();
				Sound.beep();
				this.setSpeeds(0, 0);

				avoider.avoidWall(odometer.getX()+10.0*Math.cos(odometer.getAng()), odometer.getY()+10.0*Math.sin(odometer.getAng()),x, y);	
				
				this.setSpeeds(fast,fast);

			}
			
		}
		this.setSpeeds(0, 0);
	}
	
	/*
	 * TurnTo function which takes an angle and boolean as arguments The boolean controls whether or not to stop the
	 * motors when the turn is completed
	 */
	public void turnTo(double angle, boolean stop) {

		double error = angle - this.odometer.getAng();

		while (Math.abs(error) > degreeError) {

			error = angle - this.odometer.getAng();

			if (error < -180.0) {
				this.setSpeeds(-slow, slow);
			} else if (error < 0.0) {
				this.setSpeeds(slow, -slow);
			} else if (error > 180.0) {
				this.setSpeeds(slow, -slow);
			} else {
				this.setSpeeds(-slow, slow);
			}
		}

		if (stop) {
			this.setSpeeds(0, 0);
		}
	}
	
	/*
	 * Go foward a set distance in cm
	 */
	public void goForward(double distance) {
		this.travelTo(Math.cos(Math.toRadians(this.odometer.getAng())) * distance, Math.cos(Math.toRadians(this.odometer.getAng())) * distance);

	}
	//from Ming
	public void rotate(int leftspeed, int rightspeed) {

		leftMotor.setAcceleration(ACCELERATION);
		rightMotor.setAcceleration(ACCELERATION);
		leftMotor.setSpeed(Math.abs(leftspeed));
		rightMotor.setSpeed(Math.abs(rightspeed));
		if (leftspeed < 0)
			leftMotor.backward();
		else
			leftMotor.forward();
		if (rightspeed < 0)
			rightMotor.backward();
		else
			rightMotor.forward();
	}
	/**
	 * Stops both motors immediately
	 */
	public void stopMotor() {

		leftMotor.setSpeed(0);
		rightMotor.setSpeed(0);
		try {	Thread.sleep(100);	} catch (InterruptedException e) {}
	}
	/*
	 * This method returns if the wheels are rotating
	 */
	public boolean isRotating(){
		return rightMotor.isMoving() || leftMotor.isMoving();
	}
	
	private static int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}

	private static int convertAngle(double radius, double width, double angle) {
		return convertDistance(radius, Math.PI * width * angle / 360.0);
	}
	/*
	 * rotateForLocalization method is for rotating 360 degree, being used in light localization
	 */
	public void rotateForLightLocalization(){
		leftMotor.setSpeed(ROTATION_SPEED_FOR_LIGHTLOCALIZATION);
		rightMotor.setSpeed(ROTATION_SPEED_FOR_LIGHTLOCALIZATION);
		leftMotor.rotate(-convertAngle(wheelRadius, wheelBase, 360.0), true);
		rightMotor.rotate(convertAngle(wheelRadius, wheelBase, 360.0), true);
	}
}
