/*
 * Program _fileName_
 * Created by _author_ on _date_
 */

package main

import (graph "codelab/moduleGraphics/graph2D")
import "fmt"

func main() {
	for i := 0; i < 4; i++ {
		fmt.Println("go forward...")
		graph.TurtleForward(100);
		fmt.Println("turn right...")
		graph.TurtleTurnRight(90);
	}
}
