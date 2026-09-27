# ============================================================================
# NOTES TECHNIQUES
# ============================================================================
#
# 1. ARCHITECTURE ASYNCHRONE
#    - Utilisation de Tokio pour gérer de multiples connexions simultanées
#    - Pattern actor avec des structures Client clonables
#    - Gestion des signaux de shutdown gracieux
# 
# 2. COMMUNICATION
#    - Protocol TCP custom avec handshake de login/password
#    - MessagePack pour la communication binaire efficace avec les clients Java
#    - Interface web d'administration avec authentification HTTP Basic
# 
# 3. STOCKAGE
#    - Base de données XML pour les utilisateurs et sessions
#    - Système de fichiers pour le stockage des projets étudiants
#    - Compression TAR+ZStandard pour l'envoi des sessions complètes
# 
# 4. SÉCURITÉ
#    - Hachage SHA-256 des mots de passe
#    - Gestion des sessions avec vérification des permissions
#    - Isolation des données par session/utilisateur
# 
# 5. ADMINISTRATION
#    - Interface web pour monitoring en temps réel
#    - Statistiques de connexion et d'utilisation
#    - Contrôle à distance (arrêt, déconnexion forcée)
# 
# 6. MAINTENANCE
#    - Système de backup automatique avec rotation
#    - Nettoyage intelligent des fichiers de sauvegarde
#    - Logging complet avec rotation des fichiers

-------------------------------------------------------
Configuration du proxy à l'université
-------------------------------------------------------

% export HTTP_PROXY="proxy.univ-lemans.fr:3128"
% export HTTPS_PROXY="proxy.univ-lemans.fr:3128"

-------------------------------------------------------
Installation du compilateur Rust
-------------------------------------------------------

% curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

  To get started you may need to restart your current shell.
  This would reload your PATH environment variable to include
  Cargo's bin directory ($HOME /.cargo/bin).

-------------------------------------------------------
Configurer un accès sans mot de passe
-------------------------------------------------------
https://tutox.fr/2017/04/08/se-connecter-ssh-de-passe/

% ssh-keygen -t rsa
% ssh-copy-id codelab@codelab-serv.univ-lemans.fr

(entrez le mot de passe du compte - idem Mac)

Pour supprimer une ancienne clé:

% ssh-keygen -R codelab-serv.univ-lemans.fr

-------------------------------------------------------
Ouvrir une connection SSH
-------------------------------------------------------

% ssh -t codelab@codelab-serv.univ-lemans.fr 'cd codelab-server ; bash --login'

-------------------------------------------------------
Interface d'administration HTTP
-------------------------------------------------------

Identifiants : admin / idem ordi ML

% echo -n "nouveau_password" | sha256sum

-------------------------------------------------------


# 1. Tester la résolution DNS
nslookup codelab-serv.univ-lemans.fr

# 2. Tester la connectivité réseau
ping codelab-serv.univ-lemans.fr
