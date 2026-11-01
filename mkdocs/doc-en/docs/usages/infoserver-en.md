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

# Server Installation

<br/>

## 1. Why a CodeLab Server?

Une fonctionnalité de CodeLab est de permettre la constitution de classes virtuelles (en présence ou à distance) grâce à une architecture client-serveur dédiée. Le modèle utilisateur / groupe / session permet la constitution de groupes de TP à géométrie variable, encadrés par un ou plusieurs tuteurs. Ces derniers peuvent suivre en temps réel le travail des apprenants, tester leurs programmes, communiquer avec eux par l’intermédiaire d’une messagerie instantanée intégrée, les déconnecter en fin de séance, etc. L’interface tuteur de CodeLab présente la liste des utilisateurs (connectés ou non) pour une session donnée, permet d’accéder à leur espace de travail, ainsi qu’à diverses informations les concernant (dernière heure de connexion, adresse IP, demande d’aide, etc.).

Un autre avantage d'utiliser CodeLab en mode "connecté" est que les productions des utilisateurs sont sauvegardées de façon centralisée, au niveau d'un serveur (mécanisme de type Cloud). Ils peuvent ainsi retrouver leurs programmes quelque-soit la machine qu'ils utilisent. Une activité commencée en classe peut donc être poursuivie à domicile ou dans une autre salle de TP.

<font color=red>**Côté apprenant :**</font> il suffit de connaître le nom ou l'adresse IP d'un serveur CodeLab, ainsi que le port ouvert (ces informations sont placées dans le fichier `user.properties` du dossier `userdata` = entrée "Dossier de configuration" du menu CodeLab) puis de se connecter au serveur via l'entrée "Connexion à un serveur" du menu Serveur.

<font color=red>**Côté enseignant :**</font> cette fonctionnalité nécessite de télécharger, d'installer, de configurer et de démarrer le logiciel "CodeLab Server" sur une machine serveur (de préférence sous Linux) toujours disponible et accessible depuis Internet. La mise en oeuvre d'un serveur CodeLab est décrite ci-dessous.

![](./img/sep.png?style=center+margin)

## 2. Downloading and Installing the Server

Ce qui suit concerne les utilisateurs qui souhaitent installer un serveur CodeLab.

1. Allez sur la [page de téléchargement](http://codelab.univ-lemans.fr/downloads/server/) du serveur CodeLab
2. Cliquez sur un lien en fonction de votre système (Linux ou MacOS, version Win64 à venir)
3. Décompressez le fichier sur votre machine serveur connectée à Internet
4. Exemple de l'arborescence du serveur CodeLab pour MacOS :

<pre>
codelabserver/
├── backups/                &larr; Dossier des sauvegardes
├── config/                 &larr; Dossier des configurations
│   ├── server.properties   &larr; Configuration du serveur (à renseigner)
│   ├── quickserver.xml     &larr; Configuration du serveur (ne pas modifier)
│   ├── quickserver.dtd     &larr; Grammaire du fichier quickserver.xml
│   ├── test.xml            &larr; Définition des utilisateurs et des sessions
│   ├── sessions.dtd        &larr; Grammaire du fichier XML test.xml
│   ├── example.xlsx        &larr; Exemple de fichier Excel d'utilisateurs
│   ├── example.csv         &larr; Exemple de fichier CSV d'utilisateurs
│   ├── example.xml         &larr; Fichier XML généré à partir de example.csv
│   └── build-xml.sh        &larr; Script pour générer example.xml
├── jre/                    &larr; Dossier de la machine virtuelle Java
├── lib/                    &larr; Dossier des librairies Java du serveur
├── log/                    &larr; Dossier des logs (serveur et utilisateurs)
├── temp/                   &larr; Dossier des fichiers temporaires
├── CodelabServer.jar       &larr; Exécutable du serveur CodeLab
├── kill-server.command     &larr; Script pour tuer le serveur
├── run-server.command      &larr; Script pour lancer le serveur
├── stop-server.command     &larr; Script pour stopper le serveur
├── first-time.command      &larr; Script pour autoriser CodeLab Server
└── file-recovery.command   &larr; Script pour récupérer le backup d'un programme
</pre>


### Overriding Apple Protections on macOS

Les systèmes MacOS intégrent un mécanisme qui bloque au moment de sa première exécution tout logiciel téléchargé ne possèdant pas de certificat Apple. D'autre part, CodeLab Server comprend la distribution libre Java AdoptOpenJDK qui n'est également pas signée par Apple. Pour lever les protections Apple, commencez par exécuter le script `first-time.command` (avec le bouton droit de la souris et avec un compte administrateur) puis cliquez sur le bouton `Ouvrir` de la fenêtre suivante :

![](./img/first-time.png?style=center)

Un terminal s'ouvre et vous demande d'entrer le mot de passe administrateur. Cette opération active l'option "Autoriser les applications téléchargées de n'importe où" du panel "Sécurité et confidentialité" des préférences système. Vous pourrez par la suite sans problème revenir à vos anciens réglages.

![](./img/sep.png?style=center+margin)

## 3. Server Configuration

La configuration du serveur CodeLab repose sur deux fichiers : `server.properties` et `quickserver.xml`. Vous n'avez a priori pas à modifier ce dernier. Consultez le fichier `server.properties` :

~~~txt
# Server settings

SERVER_PORT=9988
SERVER_NAME=Test server
OUT_ENCODING=UTF-8
BACKUP_INTERVAL=5
CHECK_VERSION=yes
SESSION_FILE=test.xml

# Notification settings

SMTP_SERVER=<BASE64 coded server name>
SMTP_USERNAME=<BASE64 coded login>
SMTP_PASSWORD=<BASE64 coded passwd>
EMAIL_FROM=<your_name@your_server.fr>
EMAIL_TO=<your_name@your_server.fr>
~~~

Le serveur CodeLab nécessite qu'un port de la machine soit ouvert afin que les clients puissent s'y connecter. Le port est défini par la propriété `SERVER_PORT` (valeur 9988 par défaut). Vous pouvez également changer le nom du serveur (Test server par défaut). Vous pouvez d'ailleurs installer sur la même machine plusieurs serveurs, avec des noms et des ports différents. <font color=red>Attention : le nom ou l'adresse IP de la machine serveur, ainsi que le port ouvert doit être renseigné dans les fichiers `user.properties` des clients CodeLab pour que la connexion puisse se faire.</font> La propriété `SESSION_FILE` permet de pointer vers le fichier XML décrivant les utilisateurs et les sessions.

### Periodic Backup of Work

Les programmes des utilisateurs sont actualisés en temps réel dans un dossier `sessions` créé lors de la première exécution du serveur. Parallèlement à ces sauvegardes systématiques, un mécanisme de backup périodique s'exécute en tâche de fond. Ce mécanisme sauvegarde dans le dossier `backups` la totalité des sessions (et donc des programmes) avec une période définie en minutes dans `user.properties` par la propriété `BACKUP_INTERVAL`.

### Email Notifications for Events

Le serveur CodeLab peut envoyer des notifications par e-mail suite à des évènements importants (démarrage, arrêt ou encore exception Java). Vous pouvez configurer l'expédition en renseignant les propriétés de notification dans le fichier `server.properties`. Attention : pour des raisons de confidentialité, le nom du serveur SMTP ainsi que les login et mot de passe de l'expéditeur doivent être encodés en BASE64. Pour ce faire, vous pouvez exécuter la commande shell suivante dans un terminal : `base64 <<< chaine-à-encoder`.

### Automatic Version Checks

Si la valeur de la propriété `CHECK_VERSION` est `yes` le serveur vérifiera régulièrement si une nouvelle version est disponible. Si c'est le cas, il la téléchargera et enverra une notification. La nouvelle version sera effectivement installée au prochain redémarrage du serveur.

![](./img/sep.png?style=center+margin)

## 4. Defining Users and Sessions

Pour que votre serveur soit utilisable, il vous faut définir des utilisateurs, des groupes et des sessions. Ces données sont à renseigner dans un fichier XML placé dans dle dossier `config`. Le modèle utilisateur / groupe / session peut être représenté de la façon suivante :

![](./img/model.png?style=center)

- Un <font color=red>**utilisateur**</font> est un apprenant ou un tuteur. Il possède un nom, un statut (STUDENT ou TUTOR), un login et un mot de passe. Il est éventuellement rattaché à un ou plusieurs groupes. Le mot de passe d'un utilisateur est initialement vide (considéré identique à son login). Il lui sera demandé de le changer dès sa première connexion.
- Un <font color=red>**groupe**</font> est un ensemble d'utilisateurs associés par une raison quelconque (une classe, un groupe de TP, un groupe de niveau, etc.). Un groupe est juste défini par un identifiant, comme par exemples : `Groupe_TP1` ou `Niveau_1`. La définition de groupes n'est pas obligatoire.
- Une <font color=red>**session**</font> est caractérisée par un ensemble d'utilisateurs (ajoutés individuellement ou par leurs groupes) et par un identifiant. Un utilisateur peut être rattaché à plusieurs sessions. Lorsqu'un utilisateur se connecte à un serveur, il devra choisir une session parmi celles auxquelles il est rattaché. Un tuteur a la possibilité d'ouvrir ou de fermer les sessions auxquelles il est rattaché.

Exemple de fichier XML :

~~~xml
<?xml version='1.0' encoding='utf-8'?>
<!DOCTYPE config SYSTEM 'sessions.dtd'>
<users>

	<!-- Groups -->

	<group id='Group_1'/>
	<group id='Group_2'/>

	<!-- Sessions -->

	<session id='Session_1A' open='TRUE'>
		<include group='Group_1'/>
		<include login='tutor1'/>
	</session>

	<session id='Session_1B' open='TRUE'>
		<include group='Group_1'/>
		<include login='tutor1'/>
	</session>

	<session id='Session_2A' open='TRUE'>
		<include group='Group_2'/>
		<include login='tutor2'/>
	</session>

	<session id='Big_session' open='FALSE'>
		<include group='Group_1'/>
		<include group='Group_2'/>
		<include login='tutor1'/>
		<include login='tutor2'/>
	</session>

	<!-- Tutors -->

	<user login='tutor1' passwd='' statut='TUTOR' name='Gandalf the grey'/>
	<user login='tutor2' passwd='' statut='TUTOR' name='Saruman the white'/>

	<!-- Students -->

	<user login='21000786' passwd='' statut='STUDENT' groups='Group_1' name='Griffin James'/>
	<user login='21001823' passwd='' statut='STUDENT' groups='Group_1' name='Ford Anthony'/>
	<user login='20206330' passwd='' statut='STUDENT' groups='Group_1' name='Brown Winnie'/>
	<user login='20208467' passwd='' statut='STUDENT' groups='Group_1 Group_2' name='Scott Katherine'/>
	<user login='21001807' passwd='' statut='STUDENT' groups='Group_1 Group_2' name='Huggins Dana'/>
	<user login='21004339' passwd='' statut='STUDENT' groups='Group_2' name='Dempsey Piper'/>
	<user login='21001106' passwd='' statut='STUDENT' groups='Group_2' name='Craig Donald'/>
	<user login='20190655' passwd='' statut='STUDENT' groups='Group_2' name='Darnell Joshua'/>
	<user login='21000461' passwd='' statut='STUDENT' groups='Group_2' name='Heard Judith'/>
	<user login='20182321' passwd='' statut='STUDENT' groups='Group_2' name='Moore Michael'/>

</users>
~~~

Pour constituer la liste des utilisateurs, vous pouvez partir d'un fichier de tableur (type Excel) que vous exporterez au format CSV (séparateur point-virgule) et que vous transformerez à l'aide du script `build-xml.sh` exécuté dans un terminal :

![](./img/excel.png?style=center)

Fichier `example.csv` produit par l'exportation au format CSV :

~~~txt
21000786;Griffin;James;Group_1
21001823;Ford;Anthony;Group_1
20206330;Brown;Winnie;Group_1
20208467;Scott;Katherine;Group_1 Group_2
21001807;Huggins;Dana;Group_1 Group_2
21004339;Dempsey;Piper;Group_2
21001106;Craig;Donald;Group_2
20190655;Darnell;Joshua;Group_2
21000461;Heard;Judith;Group_2
20182321;Moore;Michael;Group_2
~~~

Fichier `example.xml` produit par la commande `build-xml.sh example.csv` :

~~~xml
<user login='21000786' passwd='' statut='STUDENT' groups='Group_1' name='Griffin James'/>
<user login='21001823' passwd='' statut='STUDENT' groups='Group_1' name='Ford Anthony'/>
<user login='20206330' passwd='' statut='STUDENT' groups='Group_1' name='Brown Winnie'/>
<user login='20208467' passwd='' statut='STUDENT' groups='Group_1 Group_2' name='Scott Katherine'/>
<user login='21001807' passwd='' statut='STUDENT' groups='Group_1 Group_2' name='Huggins Dana'/>
<user login='21004339' passwd='' statut='STUDENT' groups='Group_2' name='Dempsey Piper'/>
<user login='21001106' passwd='' statut='STUDENT' groups='Group_2' name='Craig Donald'/>
<user login='20190655' passwd='' statut='STUDENT' groups='Group_2' name='Darnell Joshua'/>
<user login='21000461' passwd='' statut='STUDENT' groups='Group_2' name='Heard Judith'/>
~~~

Il ne vous reste plus qu'à copier-coller ces lignes dans le fichier XML et le tour est joué.

![](./img/sep.png?style=center+margin)

## 5. Server Management

La gestion du serveur est facilitée par les scripts suivants :

- `run-server.sh` lance le serveur en tâche de fond (vous pouvez fermer le terminal)
- `stop-server.sh` stoppe le serveur après 1 minute (et notifie les utilisateurs connectés)
- `kill-server.sh` stoppe immédiatement le serveur sans préavis

En cas de problème, vous pouvez consulter les fichiers de log (serveur et utilisateurs) qui sont actualisés dans les dossiers `log` et `log/users`.

![](./img/sep.png?style=center+margin)

## 6. Connected Mode Features

En mode connecté, l'interface tuteur de CodeLab présente la liste des utilisateurs (connectés ou non).

- Si le fond de la liste est de couleur **rouge** cela signifie que la session est fermée.
- Un utilisateur noté en **gris** indique que celui-ci n'est pas en ligne.
- Un utilisateur noté en **rouge** indique que celui-ci vous demande de l'aide (vous pouvez utiliser la messagerie).
- Une ligne de couleur **jaune** indique que vous avez pris le contrôle d'un espace de travail (vous pouvez le rendre).
- Un clic droit sur un utilisateur affiche le menu suivant :

![](./img/menu-en.png?style=center)

### Restoring an Older Version of a Program

Les programmes des utilisateurs sont actualisés en temps réel dans un dossier `sessions` créé lors de la première exécution du serveur. Parallèlement à ces sauvegardes systématiques, un mécanisme de backup périodique s'exécute en tâche de fond. Ce mécanisme sauvegarde dans le dossier `backups` la totalité des sessions (et donc des programmes) dans des fichiers compressés dont le nom indique la date de la sauvegarde. Par exemple, le fichier `sessions-220305-114813-612.zip` correspond à la sauvegarde du 5 mars 2022 à 11:48:13 :

~~~txt
sessions-220305-114813-612.zip
sessions-220305-115323-841.zip
sessions-220305-115330-516.zip
etc.
~~~

Grâce à ce mécanisme de type "time machine", un tuteur est en mesure de retrouver l'état de toutes les productions à une date donnée, ou de faire face à une suppression accidentelle (cf. fenêtres ci-dessous). Vous pouvez bien entendu "faire le ménage" dans le dossier `backups` quand bon vous semble.

**Attention:** Toute récupération d'une ancienne version d'un programe est répercutée vers l'auteur du programme.

<center>
![](./img/time1-en.png?style=noborder)
![](./img/time2-en.png?style=noborder)
![](./img/time3-en.png?style=noborder)
</center>

<center>
![](./img/fin.png?style=center)
Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; 2022 Jérôme Lehuen, Le Mans Université, France
<br/>![](./img/licence.png?style=center)
Version 04-05-2022