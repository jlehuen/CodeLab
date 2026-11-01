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

# Utilisation en mode connecté

<br/>

## Qu'est-ce qu'un serveur CodeLab ?

Une fonctionnalité de CodeLab est de permettre la constitution de classes virtuelles (en présence ou à distance) grâce à une architecture client-serveur dédiée. Le modèle utilisateur / groupe / session permet la constitution de groupes de TP à géométrie variable, encadrés par un ou plusieurs tuteurs. Ces derniers peuvent suivre en temps réel votre travail, tester vos programmes, communiquer avec vous par l’intermédiaire de la messagerie instantanée intégrée à CodeLab, etc.

Un autre avantage d'utiliser CodeLab en mode "connecté" est que vos programmes sont sauvegardés de façon centralisée au niveau d'un serveur (mécanisme de type Cloud). Vous pouvez ainsi retrouver vos programmes quelque-soit la machine que vous utilisez. Une activité commencée en classe peut donc être poursuivie à domicile ou dans une autre salle de TP.

![](./img/sep.png?style=center+margin)

## Connexion à un serveur CodeLab

Pour utiliser CodeLab en mode connecté, vous devez connaître les paramètres de connexion d'un serveur CodeLab, c'est-à-dire :

- Le nom (ou l'adresse IP) de la machine qui l'héberge, ainsi que son port
- Un login et un mot de passe permettant de vous y connecter

Le nom ou l'adresse IP du serveur ainsi que son port sont à renseigner dans le fichier `user.properties` du dossier userdata. Cliquez sur l'entrée "Dossier de configuration" du menu CodeLab pour y accéder, et ouvrez-le dans un éditeur. Il faudra relancer CodeLab pour prendre en compte les modifications.

Votre login vous sera donné le cas échéant par un enseignant, et votre mot de passe, initialement identique à votre login, sera à renseigner dans une fenêtre de dialogue dès votre première connexion :

![](./img/connexion1-fr.png?style=center)

![](./img/connexion2-fr.png?style=center)

Une fois la connexion établie, le serveur vous demandera de sélectionner dans une liste déroulante une session parmi celles qui vous sont attribuées :

![](./img/connexion3-fr.png?style=center)

![](./img/sep.png?style=center+margin)

## Fonctionnalités en mode connecté

En mode connecté, vos programmes sont systématiquement sauvegardés au niveau du serveur sur lequel vous êtes connecté. Vous pouvez ainsi les retrouver et continuer votre travail quelque-soit l'ordinateur que vous utilisez. **Attention :** tout programme commencé en mode connecté n'est accessible qu'en mode connecté, et avec les mêmes paramètres de connexion.

En mode connecté, la barre de menu de CodeLab comporte dans sa partie droite un bouton supplémentaire [Aide] qui permet d'appeler un tuteur. Lorsque vous cliquez dessus, la main de l'élève se lève et la demande est transmise au niveau des tuteurs actuellement en ligne :

![](./img/help-fr.png?style=center)

Un tuteur pourra vous répondre en utilisant la messagerie instantanée intégrée à CodeLab :

![](./img/chat-fr.png?style=center)

<center>
![](./img/fin.png?style=center)
Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; 2022 Jérôme Lehuen, Le Mans Université, France
<br/>![](./img/licence.png?style=center)
Version 04-05-2022