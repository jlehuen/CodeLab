# Scripts de gestion du serveur CodeLab

Ce dossier regroupe les scripts d'administration et d'exploitation du serveur CodeLab, conçus pour être exécutés directement dans le répertoire d'installation du serveur (ex: `~/codelab-server/`).

## Contenu

| Script | Description |
| :--- | :--- |
| `compile-server.sh` | Compile le serveur Rust (`server/`) avec Cargo (mode dev par défaut, ou arguments personnalisés comme `--release`). |
| `install-server.sh` | Décompresse l'archive `server.tgz` (si présente) et lance la compilation initiale. |
| `run-server.sh` | Démarre le serveur en arrière-plan avec `nohup` (persistant à la déconnexion SSH). Vérifie les conflits de port (9988). |
| `kill-server.sh` | Arrête proprement le processus du serveur CodeLab à l'écoute sur le port configuré. |
| `get-log.sh` | Affiche ou suit en temps réel (`-f`) les journaux d'exécution du serveur (`logs/server.log`). Supporte également un mode distant via SSH/SCP si configuré. |

## Utilisation

```bash
# Compilation
./compile-server.sh

# Lancement en arrière-plan
./run-server.sh

# Suivi des journaux en direct
./get-log.sh -f

# Arrêt du service
./kill-server.sh
```
