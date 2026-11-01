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

# Section HOWTO

<br/>

### Comment supprimer une instance "fantôme" de CodeLab ?

Dans certains cas de plantage de CodeLab (exceptionnels) il peut être nécessaire de "killer" l'application avec l'une des commandes suivantes :

Sous Linux / MacOS :

```bash
pkill -9 -fi codelab
```

Sous Windows :

```
taskkill /IM codelab.exe /F
```

![](./img/sep.png?style=center+margin)

<center>
![](./img/fin.png?style=center)
Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; 2022 Jérôme Lehuen, Le Mans Université, France
<br/>![](./img/licence.png?style=center)
Version 20-09-2023