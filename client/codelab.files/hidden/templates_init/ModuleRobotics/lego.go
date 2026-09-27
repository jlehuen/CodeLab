/*
 * Program _fileName_
 * Created by _author_ on _date_
 */

package main

import (robot "codelab/moduleRobotics/robotLego")
import "time"

func main() {
	robot.SetBackground("B01")
	robot.SetRobotConfiguration("NXT01")

	robot.MotorOn(robot.OUT_BC, 50)
	time.Sleep(1 * time.Second)
	robot.MotorOff(robot.OUT_BC)
}
