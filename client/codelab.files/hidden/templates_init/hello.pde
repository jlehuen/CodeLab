/**
 * Program _className_
 * Created by _author_ on _date_
 */

float alpha = 0;
float theta = 0;
float gamma = 0;

void setup() {
	size(500, 200, P3D);
	fill(237, 255, 12);
	textAlign(CENTER, CENTER);
	textSize(30);
}

void draw() {
	background(0);
	translate(width/2, height/2, -60);
	rotateX(alpha);
	rotateY(theta);
	rotateZ(gamma);
	text("Hello World from Processing", 0, 0);
	alpha += 0;
	theta += (width/2 - mouseX) * 0.0001;
	gamma += (height/2 - mouseY) * 0.0001;
}
