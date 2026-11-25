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
| `restore-trash.sh` | Consulte (`--list`), restaure interactivement un fichier supprimé vers `sessions/`, ou purge les anciens fichiers (`--purge <jours>`) dans `server/trash/`. |
| `purge-trash.sh` | Purge les fichiers de la corbeille de plus de 90 jours par défaut (`<jours>`, `--dry-run`, `--force` pour cron) et nettoie les dossiers vides. |

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

# Consulter ou restaurer la corbeille serveur (server/trash/)
./restore-trash.sh --list
./restore-trash.sh
./restore-trash.sh <session> <login>

# Purger les anciens fichiers supprimés (90 jours par défaut)
./purge-trash.sh --dry-run
./purge-trash.sh 90
./purge-trash.sh 90 --force   # mode cron (sans confirmation)
```
