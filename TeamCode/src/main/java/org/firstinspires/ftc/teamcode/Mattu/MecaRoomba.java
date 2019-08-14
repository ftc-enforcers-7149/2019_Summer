package org.firstinspires.ftc.teamcode.Mattu;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.DistanceSensor;

import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.Velocity;

@Autonomous(name = "MecaRoomba")
public class MecaRoomba extends OpMode {
    //Motors and sensors
    DcMotor fLeft, fRight, bLeft, bRight;
    DistanceSensor distanceL, distanceR, distanceC;
    BNO055IMU imu;

    //left = left distance sensor return value
    //right = right distance sensor return value
    //mid = center distance sensor return value
    //distance = valu eto determine if sensors detect obstacle
    double left, right, mid;
    double distance;

    // State used for updating telemetry
    //sensorState = array to hold boolean values of whether or not each sensor detects an object {left, mid, right}
    //movingState = string to hold direction of motion intended for the switch statement in loop
    boolean[] sensorState = {false, false, false};
    String movingState = "Forward";

    //IMU variables
    //gravity holds acceleration values in x, y, and z
    Acceleration gravity; //Acceleration
    AngularVelocity angular; //Angular Velocity
    double lastAngularZ;
    double speedX, speedY, dTime; //Speed

    int loops;

    public void init() {
        fLeft = hardwareMap.dcMotor.get("fLeft");
        fRight = hardwareMap.dcMotor.get("fRight");
        bLeft = hardwareMap.dcMotor.get("bLeft");
        bRight = hardwareMap.dcMotor.get("bRight");

        fLeft.setDirection(DcMotorSimple.Direction.FORWARD);
        fRight.setDirection(DcMotorSimple.Direction.REVERSE);
        bRight.setDirection(DcMotor.Direction.REVERSE);
        bLeft.setDirection(DcMotor.Direction.FORWARD);

        fLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        fRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        bLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        bRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        fLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        fRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        bLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        bRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        //Set up imu parameters
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.calibrationDataFile = "BNO055IMUCalibration.json"; // see the calibration sample opmode
        parameters.loggingEnabled = true;
        parameters.loggingTag = "IMU";
        parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();

        // Retrieve and initialize the IMU. We expect the IMU to be attached to an I2C port
        // on a Core Device Interface Module, configured to be a sensor of type "AdaFruit IMU",
        // and named "imu".
        imu = hardwareMap.get(BNO055IMU.class, "imu");
        imu.initialize(parameters);

        // Start the logging of measured acceleration
        imu.startAccelerationIntegration(new Position(), new Velocity(), 1000);

        //Set up the distance sensors
        distanceL = hardwareMap.get(DistanceSensor.class, "distanceL");
        distanceR = hardwareMap.get(DistanceSensor.class, "distanceR");
        distanceC = hardwareMap.get(DistanceSensor.class, "distanceC");

        //Setup variables
        distance = 24;
        lastAngularZ = 0;
        dTime = 0;
        speedX = 0;
        speedY = 0;
        loops = 0;
    }

    public void loop() {
        //Acceleration and angular velocity variables, respectably
        gravity = imu.getAcceleration();
        angular = imu.getAngularVelocity();

        //Set variables for distance values
        left = distanceL.getDistance(DistanceUnit.CM);
        mid = distanceC.getDistance(DistanceUnit.CM);
        right = distanceR.getDistance(DistanceUnit.CM);

        //Setup sensorState with boolean values
        sensorState[0] = left <= distance;
        sensorState[1] = mid <= distance;
        sensorState[2] = right <= distance;

        //Get speed values from acceleration values
        speedX += gravity.xAccel * (System.currentTimeMillis() - dTime) / 1000;
        speedY += gravity.yAccel * (System.currentTimeMillis() - dTime) / 1000;
        dTime = System.currentTimeMillis();

        //Telemetry of states
        telemetry.addData("Moving State: ", movingState);
        telemetry.addData("Sensor State: ", sensorState);

        //Telemetry of distances
        telemetry.addData("Left Distance (cm): ", left);
        telemetry.addData("Center Distance (cm): ", mid);
        telemetry.addData("Right Distance (cm): ", right);

        //Telemetry of speed, acceleration, and angular velocity
        telemetry.addData("X Speed (m/s): ", speedX);
        telemetry.addData("Y Speed (m/s): ", speedY);
        telemetry.addData("X Acceleration (m/s/s): ", gravity.xAccel);
        telemetry.addData("Y Acceleration (m/s/s): ", gravity.yAccel);
        telemetry.addData("Z Acceleration (m/s/s): ", imu.getGravity().zAccel);
        telemetry.addData("Angular Velocity", angular.zRotationRate);

        //Logic for moving
        if (sensorState[2]) {
            movingState = "Back Left";
        }
        if (sensorState[0] || sensorState[1]) {
            movingState = "Back Right";
        }
        else {
            movingState = "Forward";
        }

        switch (movingState) {
            //Move forward
            case "Forward":
                fLeft.setPower(0.3);
                fRight.setPower(0.3);
                bLeft.setPower(0.3);
                bRight.setPower(0.3);
                break;
            //Turn right and move back
            case "Back Right":
                if (loops % 3 == 0) {
                    //Collision detection
                    if (angular.zRotationRate < lastAngularZ - 1) {
                        movingState = "Up Right";
                        break;
                    } else {
                        fLeft.setPower(0.2);
                        fRight.setPower(-0.4);
                        bLeft.setPower(0.2);
                        bRight.setPower(-0.4);
                    }
                    lastAngularZ = angular.zRotationRate;
                }
                break;
            //Turn left and move back
            case "Back Left":
                if (loops % 3 == 0) {
                    //Collision detection
                    if (angular.zRotationRate > lastAngularZ + 1) {
                        movingState = "Up Left";
                        break;
                    } else {
                        fLeft.setPower(-0.4);
                        fRight.setPower(0.2);
                        bLeft.setPower(-0.4);
                        bRight.setPower(0.2);
                    }
                    lastAngularZ = angular.zRotationRate;
                }
                break;
            //Turn right and move up
            case "Up Right":
                if (loops % 3 == 0) {
                    //Collision detection
                    if (angular.zRotationRate < lastAngularZ - 1) {
                        movingState = "Back Right";
                        break;
                    } else {
                        fLeft.setPower(0.4);
                        fRight.setPower(-0.2);
                        bLeft.setPower(0.4);
                        bRight.setPower(-0.2);
                    }
                    lastAngularZ = angular.zRotationRate;
                }
                break;
            //Turn left and move up
            case "Up Left":
                if (loops % 3 == 0) {
                    //Collision detection
                    if (angular.zRotationRate < lastAngularZ - 1) {
                        movingState = "Back Left";
                        break;
                    } else {
                        fLeft.setPower(-0.2);
                        fRight.setPower(0.4);
                        bLeft.setPower(-0.2);
                        bRight.setPower(0.4);
                    }
                    lastAngularZ = angular.zRotationRate;
                }
                break;
        }

        loops++;
    }

    public void stop() {
        fLeft.setPower(0);
        fRight.setPower(0);
        bLeft.setPower(0);
        bRight.setPower(0);
    }
}
