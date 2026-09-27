/*
 * Program hello.go
 */

package main

import (
	"fmt"
	"runtime"
)

func main() {
	fmt.Printf("Hello World from Go version %s\n", runtime.Version())
}
