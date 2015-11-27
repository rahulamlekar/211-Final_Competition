import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import pollers.UltrasonicPoller;
import lejos.hardware.Sound;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.robotics.SampleProvider;


/**
 * @author AsherW:
 * Given that the robot is in a corner block, uses the Ultrasonic sensor to scan the area around it
 * and determine its orientation. Then sets the orientation on the odometer accurately. 
 */
public class USLocalizer {

	public enum LocalizationType { FALLING_EDGE, RISING_EDGE, FULL_CIRCLE};
	/**
	 * The speed at which the robot rotates when performing the localization
	 */
	public static int ROTATION_SPEED = 160;
	/**
	 * The acceleration of the motors (lower acceleration = less slip)
	 */
	public static int ACCELERATION = 800;
	/**
	 * The distance that the robot reads to consider it a wall
	 */
	private static int WALL_DIST = 35;
	/**
	 * The buffer distance value to ensure the robot reads a wall accurately
	 */
	private static int WALL_GAP = 3;
	/**
	 * The amount of max value readings the sensor must read to consider it a real value (and not an error)
	 */
	private static int FILTER_OUT = 3;
	/**
	 * The robot's odometer
	 */
	private Odometer odo;

	/**
	 * The type of localization that the robot performs
	 */
	private LocalizationType locType;
	/**
	 * The amount of times the robot has read a max-distance value
	 */
	private int filterControl;
	/**
	 * The value of the last distance the robot has read
	 */
	private float lastDistance;
	
	private Navigation navi;
	//Motors (we will get these from the odometer)
	private EV3LargeRegulatedMotor leftMotor, rightMotor;
	private UltrasonicPoller frontPoller;
	/**
	 * Constructor for the Localizer
	 * @param odo The odometer being used by the robot
	 * @param usSensor The ultrasonic sensor that the robot is using
	 * @param usData The float array containing the ultrasonic data of the sensor.
	 * @param locType The type of localization. FALLING_EDGE Vs RISING_EDGE
	 */
	public USLocalizer(Odometer odo, Navigation navi, UltrasonicPoller frontPoller, LocalizationType locType) {
		//get incoming values
		this.odo = odo;
		this.navi = navi;
		this.frontPoller = frontPoller;
		this.locType = locType;
		//get the motors from the odometer object.
		EV3LargeRegulatedMotor[] motors = this.odo.getMotors();
		this.leftMotor = motors[0];
		this.rightMotor = motors[1];
		leftMotor.setAcceleration(ACCELERATION);
		rightMotor.setAcceleration(ACCELERATION);
		filterControl = 0;
		lastDistance = 100;
	}

	/**
	 * Uses the Ultrasonic sensor to perform a "localization", where it figures out its initial
	 * starting angle and turns to face along the x-axis. This method actually performs the localization
	 */
	public void doLocalization() {
		//double [] pos = new double [3];
		double angleA, angleB;
		
		//set the rotational speed of the motors
		leftMotor.setSpeed(ROTATION_SPEED);
		rightMotor.setSpeed(ROTATION_SPEED);
		
		if (locType == LocalizationType.FALLING_EDGE) {
					
			// rotate the robot until it sees no wall
			while(frontPoller.getUsData() < WALL_DIST + WALL_GAP){
				leftMotor.forward();
				rightMotor.backward();
			}
			//Sound.beep();
			//System.out.println(getFilteredData());
			// keep rotating until the robot sees a wall, then latch the angle
			while(frontPoller.getUsData()  > WALL_DIST){
				leftMotor.forward();
				rightMotor.backward();
			}
			//Sound.beep();
			//get the angle from the odometer
			angleA = odo.getAng();
			
			// switch direction and wait until it sees no wall
			while(frontPoller.getUsData()  < WALL_DIST + WALL_GAP){
				leftMotor.backward();
				rightMotor.forward();
			}
			//Sound.beep();
			
			// keep rotating until the robot sees a wall, then latch the angle
			while(frontPoller.getUsData()  > WALL_DIST){
				leftMotor.backward();
				rightMotor.forward();
			}
			rightMotor.stop(true);
			leftMotor.stop(true);
			//get the angle from the odometer
			angleB = odo.getAng();
			//if our angle A is larger than B, it means we passed the 0 point, and that angle A is "negative".
			if(angleA > angleB){
				angleA = angleA - 360;
			}
			// angleA is clockwise from angleB, so assume the average of the
			// angles to the right of angleB is 45 degrees past 'north'
			double averageAngle = (angleA + angleB)/2;
			double ZeroPoint =  angleB - averageAngle + 45;

			//System.out.println("A" + angleA);
			//System.out.println("B:" + angleB);
			//System.out.println("Average" + averageAngle);
			//System.out.println("To Turn" + (FortyFiveDegPastNorth + 45));
			//rotate to the diagonal + 45 (to the horizontal x axis)
			leftMotor.rotate(convertAngle(Controller.WHEEL_RADIUS, Controller.TRACK, ZeroPoint), true);
			rightMotor.rotate(-convertAngle(Controller.WHEEL_RADIUS, Controller.TRACK, ZeroPoint), false);
			
			// update the odometer position to 0 0 0 (that's how we are facing. Position (x and y) will
			//be wrong but that will be fixed by the LightLocalizer
			odo.setPosition(new double [] {0.0, 0.0, 0}, new boolean [] {true, true, true});
		} else if(locType == LocalizationType.RISING_EDGE) {
			/*
			 * The robot should turn until it sees the wall, then look for the
			 * "rising edges:" the points where it no longer sees the wall.
			 * This is very similar to the FALLING_EDGE routine, but the robot
			 * will face toward the wall for most of it.
			 */
			// rotate the robot until it sees a wall
			while(frontPoller.getUsData()  > WALL_DIST - WALL_GAP){
				leftMotor.backward();
				rightMotor.forward();
			}
			// keep rotating until the robot no longer sees the wall, then latch the angle
			while(frontPoller.getUsData()  < WALL_DIST){
				leftMotor.backward();
				rightMotor.forward();
			}

			angleA = odo.getAng();
			
			//switch directions and rotate until the robot sees the wall.
			while(frontPoller.getUsData()  > WALL_DIST - WALL_GAP){
				leftMotor.forward();
				rightMotor.backward();
			}

			// rotate until the robot no longer sees the wall and latch the angle.
			while(frontPoller.getUsData()  < WALL_DIST){
				leftMotor.forward();
				rightMotor.backward();
			}
			leftMotor.stop(true);
			rightMotor.stop(true);
			angleB = odo.getAng();
			
			//if our angle A is bigger than B, subtract 360.
			if(angleA > angleB){
				angleA = angleA - 360;
			}
			//calculate the average angle andd the zero point (zeropoint is x axis)
			double averageAngle = (angleA + angleB)/2;
			double ZeroPoint =  angleB - averageAngle - 45;

			// angleA is clockwise from angleB, so assume the average of the
			// angles to the right of angleB is 45 degrees past 'north'
			//double averageAngle = (angleA + angleB)/2;
			//double FortyFiveDegPastNorth =  angleB - averageAngle;
			//Sound.beep();
			//rotate to the diagonal + 45 (to the x axis).
			leftMotor.rotate(convertAngle(Controller.WHEEL_RADIUS, Controller.TRACK, ZeroPoint), true);
			rightMotor.rotate(-convertAngle(Controller.WHEEL_RADIUS, Controller.TRACK, ZeroPoint), false);
			
			// update the odometer position to 0 0 0. The x and y will be wrong
			// but that will be fixed by the LightLocalizer
			odo.setPosition(new double [] {0.0, 0.0, 90}, new boolean [] {true, true, true});
		}else{ //neither falling or rising, do 360 spin
			
			//limits the amount of points we can take. This needs to be chosen experimentally.
			int upperPointAmount = 250; 
			int pointIndex = 0;
			double[] angles = new double[upperPointAmount];
			double[] distances = new double[upperPointAmount];
			
			leftMotor.setSpeed(ROTATION_SPEED);
			rightMotor.setSpeed(ROTATION_SPEED);
			
			leftMotor.backward();
			rightMotor.forward();
			frontPoller.setPollRate(25);
			boolean stopTurning = false;
			boolean passedHalfTurn = false;
			//spin until we are bigger than 359 degrees (full circle almost)
			while(!stopTurning){
				double angle = odo.getAng();
				if(pointIndex < angles.length){
					angles[pointIndex] = angle;
					distances[pointIndex] = (double) frontPoller.getUsData();
					pointIndex++;
				}else{
					Sound.beep();
					break;
				}
				if(angle > 180 && angle < 300){
					passedHalfTurn = true;
				}
				if(passedHalfTurn && (angle> 358 || (angle > 0 && angle< 100))){
					stopTurning = true;
				}
				//Now we sleep. If it takes 10s to go around, pinging at 50ms will mean 200 datapoints.
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			leftMotor.stop(true);
			rightMotor.stop(true);
//			for(int i = 0; i < angles.length; i++){
//				System.out.println(angles[i] + ", " + distances[i]);
//			}
			//now we must analyze the data we just got
			//Method 1: Get two minimums that are at least X theta apart.
			//Method 2: Remove all large values and then analyze data...
			double firstWallDist = 1000;
			int firstWallIndex = 0;
			//go through the array. We start at 4 so that we ignore first values (sometimes 0)
			for(int i = 4; i < angles.length; i++){
				//this means we've reached the empty part of our array, so leave.
				if(distances[i] == 0){
					break;
				}else{
					if(firstWallDist > distances[i]){
						firstWallDist = distances[i];
						firstWallIndex = i;
					}
				}
			}
			//we now have our minimum value. This corresponds to ONE of the walls...
			//to get the other wall, we will do the same thing, but ignore all values 
			//that are + or - 10% from the last min...
			int lowerIgnore = firstWallIndex - angles.length/10;
			int upperIgnore = firstWallIndex + angles.length/10;
			double otherWallDist = 1000;
			int otherWallIndex = 0;
			int endIndex = angles.length-1;
			for(int i = 0; i < angles.length; i++){
				//this means we've reached the empty part of our array, so leave.
				if(distances[i] == 0){
					endIndex = i;
					break;
				}else{
					//only check if outside of last wall region.
					if(i > upperIgnore || i < lowerIgnore){
						if(otherWallDist > distances[i]){
							otherWallDist = distances[i];
							otherWallIndex = i;
						}
					}
				}
			}
			firstWallIndex += upperPointAmount/25;
			otherWallIndex += otherWallIndex/25;
			if(firstWallIndex >= endIndex){
				firstWallIndex = firstWallIndex-endIndex;
			}
			if(otherWallIndex >= endIndex){
				otherWallIndex = otherWallIndex -endIndex;
			}

			//System.out.println("min dist wall: " + firstWallIndex);
			//System.out.println("second wall: " + otherWallIndex);
			double angleToTurnTo = 0;
			if(otherWallIndex > firstWallIndex){
				//check to see if we are wrapping around the array.
				if(otherWallIndex - firstWallIndex > endIndex - otherWallIndex + firstWallIndex){
					//no wrapping, so just go to the firstWallIndex
					angleToTurnTo = angles[otherWallIndex];
				}else{
					angleToTurnTo = angles[firstWallIndex];
				}
			}else{
				//check to see if we are wrapping around the array.
				if(firstWallIndex - otherWallIndex > endIndex - firstWallIndex + otherWallIndex){
					//no wrapping, so just go to the firstWallIndex
					angleToTurnTo = angles[firstWallIndex];
				}else{
					angleToTurnTo = angles[otherWallIndex];
				}
			}
			//System.out.println("My wall is at angle: " + angleToTurnTo);
			//navi.turnTo(angleToTurnTo,true);
			//Sound.beep();
//			try {
//				Thread.sleep(2000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			//we are now facing +180 degrees (along negative x axis).
			//we want to face 45 degrees, so subtract 135 + 10 since we are rotating clockwise and have offset.
			angleToTurnTo -=145;
			if(angleToTurnTo < 0){
				angleToTurnTo +=360;
			}
			navi.turnTo(angleToTurnTo, true);
			odo.setTheta(45);
			navi.travelTo(Math.max(30.4-10-firstWallDist,0), Math.max(30.4-10-firstWallDist,0));
			//done US localization.
		}
	}

	//Conversion methods.
	/**
	 * Converts a distance to be traveled by a wheel to the amount of degrees to turn the wheel.
	 * @param radius The radius of the wheel being spun
	 * @param distance The distance that we want to spin
	 * @return The amount of degrees to spin the wheel by to travel the distance
	 */
	private static int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}

	/**
	 * Converts an angle we want the robot to rotate in place to a degree amount to spin
	 * the wheel by (to reach the angle)
	 * @param radius The radius of the wheel
	 * @param width The track of the robot (distance between two wheels)
	 * @param angle The angle by which the robot should rotate
	 * @return The amount to rotate the wheel
	 */
	private static int convertAngle(double radius, double width, double angle) {
		return convertDistance(radius, Math.PI * width * angle / 360.0);
	}




}
