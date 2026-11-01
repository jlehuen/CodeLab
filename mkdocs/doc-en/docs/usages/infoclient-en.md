<style>
	html {
		scroll-behavior: smooth;
	}
	img[src$="noborder"] {
		border-style: none;
		background-color: none;
	}
	img[src$="center"] {
		display: block;
		margin: 0 auto;
		border-style: none;
		background-color: none;
	}
	img[src$="center+margin"] {
		display: block;
		margin: 0 auto;
		margin-top: 40px;
		margin-bottom: 40px;
		border-style: none;
		background-color: none;
	}
	pre { 
		margin-top: 20px;
		margin-bottom: 20px;
		padding: 20px;
		margin: 10px;
	}
	code {
		background: yellow;
	}
	code[class=yellow] {
		background: yellow;
	}
</style>

![](./img/logo-codelab.png?style=noborder)

<H1>CodeLab IDE & Simulators</H1>

# Using CodeLab in Connected Mode

<br/>

## What is a CodeLab Server?

A key feature of CodeLab is the ability to run collaborative virtual classrooms (in-person or remote) powered by a dedicated client-server architecture. The user / group / session model allows teachers to organize lab groups of varying sizes, supervised by one or more tutors. Tutors can follow students' progress in real time, run and test code, and interact via built-in instant messaging.

Another major benefit of connected mode is centralized cloud-like storage: student programs are saved automatically on the server, allowing students to access their workspaces from any computer (in class, at home, or in other labs).

![](./img/sep.png?style=center+margin)

## Connecting to a CodeLab Server

To connect to a CodeLab server, you need:

- Hostname or IP address and port of the server
- User login and password

The server address and port must be specified in `user.properties` in your userdata directory (open via CodeLab menu > "Configuration folder"). Restart CodeLab to apply changes.

Your login will be provided by your instructor. Your password (initially identical to your login) must be configured on first connection:

![](./img/connexion1-en.png?style=center)

![](./img/connexion2-en.png?style=center)

Once connected, choose your active session from the drop-down list:

![](./img/connexion3-en.png?style=center)

![](./img/sep.png?style=center+margin)

## Connected Mode Features

All code written in connected mode is synchronized with the server in real time. **Note:** programs started in connected mode remain associated with that server profile.

In connected mode, CodeLab displays an extra [Help] button on the right side of the toolbar. Clicking it raises your hand to notify online tutors:

![](./img/help-en.png?style=center)

Tutors can reply directly through CodeLab's integrated instant chat:

![](./img/chat-en.png?style=center)

<center>
![](./img/fin.png?style=center)
Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; Jérôme Lehuen, Le Mans Université, France
<br/>![](./img/licence.png?style=center)
</center>
