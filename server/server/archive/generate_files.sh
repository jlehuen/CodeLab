#!/bin/bash

# Nombre total de fichiers à générer
total_files=1000

# Répertoire cible pour les fichiers (modifiez si nécessaire)
output_dir="test/lehuen"

# Créer le répertoire de sortie
mkdir -p "$output_dir"

# Temps actuel
start_time=$(date "+%s")

# Intervalle : 10 mn (600 secondes) entre chaque fichier
interval_seconds=600

# Génération des fichiers
echo "Génération de $total_files fichiers dans $output_dir ..."

for i in $(seq 1 $total_files); do
    # Calculer le timestamp pour chaque fichier (en reculant)
    current_time=$((start_time - (i - 1) * interval_seconds))

    # Formater le timestamp en YYMMDD_HHMMSS
    formatted_time=$(date -r "$current_time" "+%y%m%d_%H%M%S")

    # Générer le nom du fichier
    file_name="hello_${formatted_time}_bak.c.bak"

    # Créer le fichier vide
    touch "$output_dir/$file_name"
done

echo "Fichiers générés avec succès."
