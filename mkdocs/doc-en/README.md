# CodeLab Documentation (English)

This directory contains the Markdown source files for the CodeLab documentation in English.

## Documentation Overview

### 1. User Guides
- [Connected Mode (Client)](docs/usages/infoclient-en.md): Joining classroom sessions, built-in instant messaging, and real-time code monitoring.
- [Server Installation and Administration](docs/usages/infoserver-en.md): Network configuration, user management, and session management.
- [HOWTO Section (FAQ)](docs/usages/howto-en.md): Practical tips, common troubleshooting, and useful commands.

### 2. Language API References
- [C Language API](docs/lang-c/doc-c-en.md): 2D graphics functions, Lego NXT robotics, motors, and system utilities.
- [Java Language API](docs/lang-java/doc-java-en.md): `Drawing`, `Turtle`, `RobotLego`, `NXTMotor`, `EventManager`, `ToneGenerator`, etc.
- [Python Language API](docs/lang-python/doc-py-en.md): `graph2D`, `turtle`, `robotLego`, `robotRovio`, `nxtMotor`, `sound`, and `system` libraries.
- [CLIPS Language API](docs/lang-clips/doc-clips-en.md): Rule-based graphics and robotics primitives.

### 3. Developer Tutorials (Application Module Development)
- [Tutorial #1: Designing a First CodeLab Module](docs/tuto1/tuto1-en.md): PAC (Presentation-Abstraction-Control) architecture, lifecycle, and communication protocol.
- [Tutorial #2: Designing a 2D Plotter Module](docs/tuto2/tuto2-en.md): Building an interactive GUI with vector drawing surface.
- [Tutorial #3: Designing a Motor Simulator](docs/tuto3/tuto3-en.md): Real-time simulator for a servo motor with tachometer support.

---

### Building the Static Website (Optional)
To build the documentation locally as a static HTML website using [MkDocs](https://www.mkdocs.org/):
```bash
./compile.sh
```
The site will be generated locally in the `site/` directory.
