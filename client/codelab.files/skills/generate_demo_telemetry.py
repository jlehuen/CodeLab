#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate_demo_telemetry.py
Générateur de données et de rapports fictifs pour illustrer la télémétrie CodeLab et les radars Mermaid.
Crée 4 profils types d'étudiants (Léa Martin, Thomas Dubois, Lucas Bernard, Camille Petit)
avec leurs traces fines .activity.jsonl, leurs codes sources et leurs fiches d'évaluation complètes.
"""

import os
import sys
import json
import shutil
from pathlib import Path
from datetime import datetime, timedelta

SKILLS_DIR = Path(__file__).resolve().parent
if str(SKILLS_DIR) not in sys.path:
    sys.path.insert(0, str(SKILLS_DIR))

from activity_analyzer import analyze_student_activity, generate_mermaid_radars, generate_cohort_radar

DEMO_DIR = SKILLS_DIR.parent / "evaluations" / "Demo_Telemetry"
DEMO_DIR_USER = Path("/Users/lehuen/codelab.files/evaluations/Demo_Telemetry")

# Code source Python pour chaque profil
CODE_MARTIN = '''"""
Module de traitement des logs et calculs statistiques vectorisés.
Auteur : Léa Martin
Date : 03/11/2026
"""

from typing import List, Dict, Tuple
import math

def parse_log_line(line: str) -> Dict[str, str]:
    """Parse une ligne de log au format clé=valeur.
    
    >>> parse_log_line("ip=192.168.1.1 status=200")
    {'ip': '192.168.1.1', 'status': '200'}
    """
    parts = line.strip().split()
    record = {}
    for part in parts:
        if "=" in part:
            k, v = part.split("=", 1)
            record[k] = v
    return record

def compute_error_ratio(records: List[Dict[str, str]]) -> float:
    """Calcule le ratio d'erreurs HTTP (>= 400)."""
    if not records:
        return 0.0
    errs = sum(1 for r in records if int(r.get("status", 200)) >= 400)
    return round(errs / len(records), 3)

def run_pipeline(lines: List[str]) -> Tuple[int, float]:
    """Exécute l'analyse complète."""
    recs = [parse_log_line(l) for l in lines if l.strip()]
    return len(recs), compute_error_ratio(recs)

if __name__ == "__main__":
    sample = [
        "ip=10.0.0.1 status=200 time=12ms",
        "ip=10.0.0.2 status=404 time=3ms",
        "ip=10.0.0.3 status=500 time=45ms"
    ]
    tot, ratio = run_pipeline(sample)
    assert tot == 3, "Erreur total records"
    assert math.isclose(ratio, 0.667, abs_tol=1e-2), "Erreur ratio"
    print(f"Pipeline OK: {tot} lignes, {ratio*100:.1f}% erreurs.")
'''

CODE_DUBOIS = '''# Script d'analyse de logs - Thomas Dubois
# TP Algorithmique

import sys

def parse_ligne(l):
    # Decoupage par espace
    morceaux = l.strip().split(" ")
    dico = {}
    for m in morceaux:
        if "=" in m:
            cle, val = m.split("=")
            dico[cle] = val
    return dico

def stats(lignes):
    # calcul du nombre d'erreurs
    total = 0
    erreurs = 0
    for l in lignes:
        if len(l.strip()) > 0:
            total = total + 1
            d = parse_ligne(l)
            st = int(d.get("status", 200))
            if st >= 400:
                erreurs = erreurs + 1
    
    print("DEBUG: total =", total, "erreurs =", erreurs)
    ratio = float(erreurs) / float(total) if total > 0 else 0.0
    return total, ratio

# Tests manuels
logs = [
    "ip=10.0.0.1 status=200",
    "ip=10.0.0.2 status=404",
    "ip=10.0.0.3 status=500"
]

tot, r = stats(logs)
print("Resultat :", tot, "lignes avec ratio =", r)
assert tot == 3
'''

CODE_BERNARD = '''"""
Ultra-Optimized Asynchronous Stream Log Ingestion & Analytics Pipeline
Auto-Generated Architecture with High-Order Functional Combinators
"""

import functools
import itertools
from typing import Mapping, Sequence, NamedTuple, Iterator

class LogEntry(NamedTuple):
    metadata: Mapping[str, str]
    is_anomaly: bool

class LogProcessor:
    __slots__ = ("_stream", "_window_size")
    
    def __init__(self, stream: Sequence[str], window_size: int = 256) -> None:
        self._stream = stream
        self._window_size = window_size

    def _tokenize(self, raw_line: str) -> Iterator[Tuple[str, str]]:
        return (
            tuple(kv.split("=", 1))
            for kv in raw_line.strip().split()
            if "=" in kv
        )

    def analyze(self) -> Mapping[str, float]:
        records = [dict(self._tokenize(line)) for line in self._stream if line.strip()]
        error_count = sum(
            1 for r in records
            if int(r.get("status", 200)) >= 400
        )
        return {
            "total_records": len(records),
            "anomaly_rate": error_count / max(1, len(records)),
            "entropy": functools.reduce(lambda acc, x: acc + len(x), records, 0) / max(1, len(records))
        }

if __name__ == "__main__":
    proc = LogProcessor(["ip=10.0.0.1 status=200", "ip=10.0.0.2 status=404", "ip=10.0.0.3 status=500"])
    res = proc.analyze()
    print("Engine telemetry completed:", res)
'''

CODE_PETIT = '''# TP Python - Camille Petit
# Exercice 1 : Logs

def lire_log(ligne):
    d = {}
    mots = ligne.split()
    for mot in mots:
        # split sur le égal
        s = mot.split("=")
        d[s[0]] = s[1] # Parfois IndexError si pas de egal
    return d

def calcul(fichier):
    # A completer
    pass

# Essai
ligne_test = "ip=10.0.0.1 status=200"
print(lire_log(ligne_test))
'''

def generate_events_martin(start_dt: datetime) -> list:
    """Étudiante modèle : frappe continue, 12 runs, 1 min away, zéro alerte."""
    evs = []
    t = start_dt
    
    def add(ev_type, **kw):
        nonlocal t
        delta_s = kw.pop("_dt", 30)
        t += timedelta(seconds=delta_s)
        evs.append({"timestamp": t.strftime("%Y-%m-%dT%H:%M:%SZ"), "ts": t.strftime("%Y-%m-%dT%H:%M:%SZ"), "type": ev_type, **kw})

    add("SESSION", status="CONNECTED", user="lmartin", _dt=0)
    add("TYPING", typed_chars=120, backspaces=5, _dt=60)
    add("EDIT", file="solution.py", lines_added=8, lines_removed=0, chunk_max=3, _dt=45)
    add("TYPING", typed_chars=240, backspaces=12, _dt=120)
    add("EDIT", file="solution.py", lines_added=12, lines_removed=1, chunk_max=4, _dt=60)
    add("COMPILE", file="solution.py", status="OK", _dt=15)
    add("EXECUTE", file="solution.py", status="DONE", duration_ms=45, _dt=10)
    
    # Pause courte recherche doc
    add("FOCUS", status="LOST", _dt=180)
    add("FOCUS", status="GAINED", duration_sec=55.0, _dt=55)
    
    # Saisie active continue
    add("TYPING", typed_chars=350, backspaces=18, _dt=150)
    add("PASTE", file="solution.py", chars=48, lines=2, _dt=30) # petit paste d'import
    add("EDIT", file="solution.py", lines_added=15, lines_removed=2, chunk_max=3, _dt=80)
    
    # Petite erreur de syntaxe vite corrigée (TTF = 20s)
    add("COMPILE", file="solution.py", status="ERROR", _dt=20)
    add("TYPING", typed_chars=25, backspaces=4, _dt=15)
    add("EDIT", file="solution.py", lines_added=1, lines_removed=1, chunk_max=1, _dt=5)
    add("COMPILE", file="solution.py", status="OK", _dt=5)
    add("EXECUTE", file="solution.py", status="DONE", duration_ms=50, _dt=10)
    
    # Suite du code et tests unitaires
    for i in range(5):
        add("TYPING", typed_chars=180, backspaces=8, _dt=100)
        add("EDIT", file="solution.py", lines_added=6, lines_removed=1, chunk_max=2, _dt=40)
        add("COMPILE", file="solution.py", status="OK", _dt=10)
        add("EXECUTE", file="solution.py", status="DONE", duration_ms=60, _dt=15)

    add("TYPING", typed_chars=110, backspaces=3, _dt=90)
    add("COMPILE", file="solution.py", status="OK", _dt=10)
    add("EXECUTE", file="solution.py", status="DONE", duration_ms=55, _dt=10)
    add("SESSION", status="DISCONNECTED", _dt=60)
    return evs

def generate_events_dubois(start_dt: datetime) -> list:
    """Expérimentateur : beaucoup d'exécutions, 14 erreurs corrigées, navette modérée."""
    evs = []
    t = start_dt
    
    def add(ev_type, **kw):
        nonlocal t
        delta_s = kw.pop("_dt", 30)
        t += timedelta(seconds=delta_s)
        evs.append({"timestamp": t.strftime("%Y-%m-%dT%H:%M:%SZ"), "ts": t.strftime("%Y-%m-%dT%H:%M:%SZ"), "type": ev_type, **kw})

    add("SESSION", status="CONNECTED", user="tdubois", _dt=0)
    add("TYPING", typed_chars=180, backspaces=22, _dt=60)
    add("EDIT", file="solution.py", lines_added=10, lines_removed=0, chunk_max=4, _dt=40)
    
    # Séquences intenses d'essais-erreurs avec modifications régulières
    for k in range(6):
        add("TYPING", typed_chars=150, backspaces=18, _dt=90)
        add("EDIT", file="solution.py", lines_added=5, lines_removed=2, chunk_max=2, _dt=30)
        add("COMPILE", file="solution.py", status="ERROR", _dt=10)
        # correction
        add("TYPING", typed_chars=60, backspaces=10, _dt=35)
        add("EDIT", file="solution.py", lines_added=3, lines_removed=2, chunk_max=2, _dt=15)
        add("COMPILE", file="solution.py", status="OK", _dt=10)
        add("EXECUTE", file="solution.py", status="ERROR", _dt=10) # exception runtime
        # debug print
        add("TYPING", typed_chars=40, backspaces=4, _dt=25)
        add("EDIT", file="solution.py", lines_added=2, lines_removed=1, chunk_max=1, _dt=10)
        add("EXECUTE", file="solution.py", status="DONE", duration_ms=40, _dt=10)

    # 4 sorties d'IDE pour consulter cours
    for _ in range(4):
        add("FOCUS", status="LOST", _dt=120)
        add("FOCUS", status="GAINED", duration_sec=140.0, _dt=140)
        add("TYPING", typed_chars=90, backspaces=12, _dt=60)
        add("EDIT", file="solution.py", lines_added=4, lines_removed=1, chunk_max=2, _dt=30)
        add("COMPILE", file="solution.py", status="OK", _dt=10)
        add("EXECUTE", file="solution.py", status="DONE", duration_ms=50, _dt=15)

    add("SESSION", status="DISCONNECTED", _dt=60)
    return evs

def generate_events_bernard(start_dt: datetime) -> list:
    """Triche IA / Injection massive : 45 min away, BURST_INSERT 142L en 1s, zéro frappe."""
    evs = []
    t = start_dt
    
    def add(ev_type, **kw):
        nonlocal t
        delta_s = kw.pop("_dt", 30)
        t += timedelta(seconds=delta_s)
        evs.append({"timestamp": t.strftime("%Y-%m-%dT%H:%M:%SZ"), "ts": t.strftime("%Y-%m-%dT%H:%M:%SZ"), "type": ev_type, **kw})

    add("SESSION", status="CONNECTED", user="lbernard", _dt=0)
    add("TYPING", typed_chars=25, backspaces=1, _dt=45) # 3 mots tapés
    
    # Sortie prolongée de l'IDE vers ChatGPT / Claude
    add("FOCUS", status="LOST", _dt=60)
    add("FOCUS", status="GAINED", duration_sec=2700.0, _dt=2700) # 45 min d'absence !
    
    # Retour et collage massif immédiat
    add("PASTE", file="solution.py", chars=3820, lines=142, _dt=10)
    add("EDIT", file="solution.py", lines_added=142, lines_removed=0, chunk_max=142, alert="BURST_INSERT", dt_sec=1.1, _dt=2)
    
    # Vérification rapide et 2 exécutions parfaites
    add("COMPILE", file="solution.py", status="OK", _dt=30)
    add("EXECUTE", file="solution.py", status="DONE", duration_ms=85, _dt=15)
    
    # Petite modification cosmétique
    add("TYPING", typed_chars=18, backspaces=0, _dt=60)
    add("EDIT", file="solution.py", lines_added=1, lines_removed=0, chunk_max=1, _dt=10)
    add("COMPILE", file="solution.py", status="OK", _dt=10)
    add("EXECUTE", file="solution.py", status="DONE", duration_ms=75, _dt=10)
    
    add("SESSION", status="DISCONNECTED", _dt=120)
    return evs

def generate_events_petit(start_dt: datetime) -> list:
    """Étudiante en difficulté : acharnement sans modification de code (thrashing), demandes d'aide."""
    evs = []
    t = start_dt
    
    def add(ev_type, **kw):
        nonlocal t
        delta_s = kw.pop("_dt", 30)
        t += timedelta(seconds=delta_s)
        evs.append({"timestamp": t.strftime("%Y-%m-%dT%H:%M:%SZ"), "ts": t.strftime("%Y-%m-%dT%H:%M:%SZ"), "type": ev_type, **kw})

    add("SESSION", status="CONNECTED", user="cpetit", _dt=0)
    add("TYPING", typed_chars=80, backspaces=15, _dt=90)
    add("EDIT", file="solution.py", lines_added=6, lines_removed=0, chunk_max=3, _dt=60)
    
    # Blocage et boucle d'erreurs répétées (acharnement / thrashing)
    for _ in range(4):
        add("COMPILE", file="solution.py", status="ERROR", _dt=20)
        add("TYPING", typed_chars=4, backspaces=2, _dt=10) # micro-touch sans fix réel
        add("COMPILE", file="solution.py", status="ERROR", _dt=15)
        add("COMPILE", file="solution.py", status="ERROR", _dt=15)
    
    # Demande d'aide enseignant
    add("HELP", status="REQUESTED", topic="Syntaxe et découpage de chaîne", _dt=45)
    add("FOCUS", status="LOST", _dt=180) # Attente
    add("FOCUS", status="GAINED", duration_sec=240.0, _dt=240)
    add("HELP", status="RESOLVED", _dt=10)
    
    # Reprise hésitante
    add("TYPING", typed_chars=95, backspaces=28, _dt=120)
    add("EDIT", file="solution.py", lines_added=4, lines_removed=2, chunk_max=2, _dt=60)
    add("COMPILE", file="solution.py", status="OK", _dt=15)
    add("EXECUTE", file="solution.py", status="ERROR", _dt=15) # IndexError
    
    # Deuxième demande d'aide
    add("HELP", status="REQUESTED", topic="IndexError dans lire_log", _dt=30)
    add("HELP", status="RESOLVED", _dt=180)
    
    add("TYPING", typed_chars=40, backspaces=12, _dt=90)
    add("COMPILE", file="solution.py", status="OK", _dt=20)
    add("SESSION", status="DISCONNECTED", _dt=60)
    return evs

STUDENTS = [
    {
        "id": "lmartin",
        "name": "Léa Martin",
        "code": CODE_MARTIN,
        "events_gen": generate_events_martin,
        "profil_label": "Excellent (Autonome & Méthodique)",
        "syntax_note": 6.0,
        "arch_note": 7.5,
        "style_note": 4.0,
        "test_note": 2.0,
        "ai_prob": 0.0,
        "ai_level": "Faible (0% - Code authentique et rédigé in situ)",
        "bilan": "Prestation exceptionnelle. L'ensemble des exigences du TP a été traité avec rigueur, élégance et propreté architecturale. La couverture de tests et l'utilisation judicieuse des docstrings et typages démontrent une excellente maîtrise des paradigmes Python.",
        "points_forts": "- Architecture modulaire avec fonctions pures et séparation claire des responsabilités.\n- Typage statique (`typing`) et docstrings exécutables avec tests doctest intégrés.\n- Zéro alerte d'intégrité, traçage de frappe 100% régulier.",
        "axes_amelioration": "- Continuer sur cette lancée ! Vous pouvez d'ores et déjà explorer les bibliothèques de tests avancées comme `pytest` ou la vectorisation avec `numpy`.",
        "commentaire": "Félicitations Léa, travail exemplaire tant sur le fond algorithmique que sur la méthodologie de développement !"
    },
    {
        "id": "tdubois",
        "name": "Thomas Dubois",
        "code": CODE_DUBOIS,
        "events_gen": generate_events_dubois,
        "profil_label": "Bien (Expérimentateur & Persévérant)",
        "syntax_note": 5.5,
        "arch_note": 5.5,
        "style_note": 2.0,
        "test_note": 1.5,
        "ai_prob": 5.0,
        "ai_level": "Faible (5% - Code authentique issu d'essais-erreurs)",
        "bilan": "Très bon investissement pratique. L'algorithme fonctionne et produit les résultats escomptés. On observe une réelle pugnacité face aux erreurs d'exécution rencontrées, qui ont toutes été résolues grâce à des tests itératifs.",
        "points_forts": r"- Grande persévérance : 26 cycles de compilation/exécution menés pour traquer chaque bogue.\n- Travail personnel indiscutable ($S_{auth} = 95\%$), code vivant et authentique.\n- Présence d'assertions de validation fonctionnelle.",
        "axes_amelioration": "- Nettoyer les `print(\"DEBUG...\")` avant livraison finale.\n- Remplacer les boucles verbeuses par des idiomes Python plus concis (ex: listes en compréhension).\n- Ajouter des docstrings formelles pour documenter les contrats de fonctions.",
        "commentaire": "Une démarche active très positive, Thomas. Tu ne lâches rien face aux bogues ! Veille maintenant à soigner la finition et le style du code."
    },
    {
        "id": "lbernard",
        "name": "Lucas Bernard",
        "code": CODE_BERNARD,
        "events_gen": generate_events_bernard,
        "profil_label": "Alerte Intégrité (Recours Externe / IA Présumé)",
        "syntax_note": 6.0,
        "arch_note": 7.0,
        "style_note": 4.0,
        "test_note": 0.0,
        "ai_prob": 95.0,
        "ai_level": "🚨 Élevé (95% - Injection massive de code généré par IA)",
        "bilan": "Le script soumis présente un degré de sophistication disproportionné par rapport aux notions abordées en cours (`__slots__`, `NamedTuple`, combinateurs fonctionnels `reduce`). La télémétrie confirme une absence prolongée de 45 minutes suivie d'une injection instantanée de 142 lignes d'un seul bloc.",
        "points_forts": "- Le code compile et fonctionne correctement de manière isolée.",
        "axes_amelioration": "- L'objectif pédagogique est l'apprentissage personnel, pas la délégation à un modèle de langage.\n- Lors de l'épreuve orale de contrôle, l'explication ligne à ligne du fonctionnement des combinateurs sera exigée.",
        "commentaire": "Rapport gelé dans l'attente d'un entretien oral de vérification des acquis. Un malus d'intégrité de 10 points est appliqué sur la note finale."
    },
    {
        "id": "cpetit",
        "name": "Camille Petit",
        "code": CODE_PETIT,
        "events_gen": generate_events_petit,
        "profil_label": "Insuffisant (En Difficulté / Besoin de Guidage)",
        "syntax_note": 3.5,
        "arch_note": 3.0,
        "style_note": 1.5,
        "test_note": 0.5,
        "ai_prob": 0.0,
        "ai_level": "Faible (0% - Code authentique mais incomplet)",
        "bilan": "Travail inachevé. L'étudiante a rencontré des difficultés majeures dès la première fonction de découpage (`IndexError`), entraînant plusieurs épisodes d'acharnement sur le compilateur sans modification substantielle de la logique.",
        "points_forts": "- Démarche honnête : pas de tentative de camouflage ni de recours frauduleux à l'IA.\n- Sollicitation de l'aide enseignante pour tenter de débloquer la situation.",
        "axes_amelioration": "- Revoir d'urgence la manipulation des listes et la méthode `.split()` en Python.\n- Ne pas s'acharner à relancer l'exécution sans avoir lu attentivement le message d'erreur du terminal.\n- Participer aux séances de tutorat pour consolider les bases algorithmiques.",
        "commentaire": "Courage Camille, le début est difficile mais le recours aux tuteurs et un travail régulier sur les exercices de base vont te permettre de progresser rapidement."
    }
]

def main():
    print(f"[DEMO] Création de la cohorte de démonstration dans {DEMO_DIR}...")
    DEMO_DIR.mkdir(parents=True, exist_ok=True)
    DEMO_DIR_USER.mkdir(parents=True, exist_ok=True)

    base_time = datetime(2026, 11, 3, 14, 0, 0)
    users_json_data = {}
    csv_rows = [
        "Login,Nom,Note_Initiale,Malus_IA,Malus_Sim,Note_Finale,Profil,S_auth,I_nav,Compilations,Executions,Alertes_Burst"
    ]

    for st in STUDENTS:
        sid = st["id"]
        s_dir = DEMO_DIR / sid
        s_dir.mkdir(parents=True, exist_ok=True)
        
        # 1. Écriture du code source
        (s_dir / "solution.py").write_text(st["code"], encoding="utf-8")
        
        # 2. Écriture du fichier .activity.jsonl
        events = st["events_gen"](base_time)
        act_file = s_dir / ".activity.jsonl"
        with open(act_file, "w", encoding="utf-8") as f:
            for ev in events:
                f.write(json.dumps(ev) + "\n")
                
        # 3. Analyse d'activité
        act_res = analyze_student_activity(s_dir)
        
        # 4. Calcul des notes
        syntax = st["syntax_note"]
        arch = st["arch_note"]
        style = st["style_note"]
        tests = st["test_note"]
        brute = syntax + arch + style + tests
        
        # Malus éventuels
        malus_ia = 0.0
        malus_sim = 0.0
        if st["ai_prob"] > 80.0 or act_res["burst_insert_count"] > 0:
            malus_ia = 10.0
            
        finale = max(0.0, min(20.0, brute - malus_ia - malus_sim))
        
        scores = {
            "syntax": syntax,
            "architecture": arch,
            "style": style,
            "tests": tests,
            "note_technique_brute": brute,
            "malus_ia": malus_ia,
            "malus_similarite": malus_sim,
            "total": finale
        }
        
        data_for_radar = {
            "scores": scores,
            "activity_analysis": act_res,
            "ai_analysis": {
                "probability_pct": st["ai_prob"],
                "level": st["ai_level"]
            }
        }
        
        radars = generate_mermaid_radars(data_for_radar, st["name"])
        
        # Tableau récapitulatif
        scores_table = f"""| Axe d'Évaluation | Note Attribuée | Barème Officiel | Appréciation Synthétique |
| :--- | :---: | :---: | :--- |
| **Axe 1 : Syntaxe & Arbre AST** | **{syntax:.1f}** | / 6.0 | Analyse statique et conformité de la structure syntaxique |
| **Axe 2 : Conception & Modularité** | **{arch:.1f}** | / 8.0 | Découpage fonctionnel, modularité et algorithmique |
| **Axe 3 : Style & Bonnes Pratiques** | **{style:.1f}** | / 4.0 | PEP8, clarté, nommage et documentation |
| **Axe 4 : Validation & Assertions** | **{tests:.1f}** | / 2.0 | Tests unitaires, gestion des cas limites et assertions |
| **Note Technique Brute** | **{brute:.1f}** | **/ 20.0** | **Somme des 4 piliers d'évaluation** |"""

        if malus_ia > 0:
            scores_table += f"\n| *Malus Détection IA / Télémétrie* | *-{malus_ia:.1f}* | - | Pénalité pour injection massive non authentifiée |"
        scores_table += f"\n| **TOTAL GÉNÉRAL** | **{finale:.1f}** | **/ 20.0** | **{st['profil_label']}** |"

        # Génération du rapport Markdown
        rep_lines = [
            f"# Fiche d'Évaluation Individuelle : Python / Algorithmique",
            "",
            f"### Étudiant : **{st['name']}**",
            "",
            f"- **Login / ID :** `{sid}`",
            f"- **Module :** Algorithmique et Programmation Python (L1/L2)",
            f"- **Profil Comportemental Dominant :** {st['profil_label']}",
            "",
            "---",
            "",
            "### 📋 Cartouche de Traçabilité & Synthèse d'Intégrité",
            "",
            f"- **Date de l'évaluation :** {datetime.now().strftime('%d/%m/%Y à %H:%M')}",
            f"- **Skill appliqué :** `eval-python-sample` (dernière modification : 03/11/2026)",
            f"- **Moteur d'évaluation :** Moteur Hybride Déterministe & Télémétrique CodeLab",
            f"- **Chronométrie :** {act_res.get('span_duration_min', 0)} min de session ({act_res.get('first_event', '')} → {act_res.get('last_event', '')})",
            f"- **Stylométrie d'indentation :** Conforme aux conventions PEP 8",
            f"- **Test de similarité (Plagiat) :** Original (0% partage inter-étudiants)",
            f"- **Indice d'assistance IA :** {st['ai_level']}",
            "",
            "---",
            "",
            "### Tableau Récapitulatif des Notes Calibrées",
            "",
            scores_table,
            "",
            "---",
            "",
            radars["radars_markdown"],
            "",
            "---",
            "",
            "### 🔬 Analyse Fine de Télémétrie & Dynamique de Travail",
            "",
            act_res["summary_markdown"],
            "",
            "---",
            "",
            "### Bilan de Progression & Analyse Pédagogique",
            "",
            st["bilan"],
            "",
            "---",
            "",
            "### Points Forts Observés",
            "",
            st["points_forts"],
            "",
            "---",
            "",
            "### Axes d'Amélioration & Conseils Méthodologiques",
            "",
            st["axes_amelioration"],
            "",
            "---",
            "",
            "### Commentaire de l'Enseignant",
            "",
            st["commentaire"],
            ""
        ]

        report_content = "\n".join(rep_lines)
        report_file = s_dir / f"{sid}_rapport.md"
        report_file.write_text(report_content, encoding="utf-8")
        
        # Copie miroir vers DEMO_DIR_USER
        s_user_dir = DEMO_DIR_USER / sid
        s_user_dir.mkdir(parents=True, exist_ok=True)
        (s_user_dir / "solution.py").write_text(st["code"], encoding="utf-8")
        (s_user_dir / ".activity.jsonl").write_text((s_dir / ".activity.jsonl").read_text(encoding="utf-8"), encoding="utf-8")
        (s_user_dir / f"{sid}_rapport.md").write_text(report_content, encoding="utf-8")

        # Sauvegarde pour users.json et CSV
        users_json_data[sid] = {
            "name": st["name"],
            "group": "Groupe Demo",
            "last_active": act_res.get("last_event", "")
        }
        
        burst_total = act_res["burst_insert_count"] + act_res["burst_new_file_count"]
        csv_rows.append(
            f"{sid},{st['name']},{brute:.1f},{malus_ia:.1f},{malus_sim:.1f},{finale:.1f},"
            f"\"{st['profil_label']}\",{act_res['s_auth']}%,{act_res['i_nav']}%,{act_res['compilations']['total']},"
            f"{act_res['executions']['total']},{burst_total}"
        )
        print(f"  -> Profil généré : {st['name']} ({sid}) -> Note finale: {finale}/20")

    # Écriture users.json et CSV de session
    for target_root in (DEMO_DIR, DEMO_DIR_USER):
        (target_root / "users.json").write_text(json.dumps(users_json_data, indent=2, ensure_ascii=False), encoding="utf-8")
        (target_root / "notes_Demo_Telemetry.csv").write_text("\n".join(csv_rows), encoding="utf-8")

    # Synthèse globale de session
    synthese_md = fr"""# Synthèse Globale de Session — Démonstration Télémétrie & Typologie

- **Session :** `Demo_Telemetry`
- **Date de l'évaluation :** {datetime.now().strftime('%d/%m/%Y à %H:%M')}
- **Effectif évalué :** {len(STUDENTS)} étudiants
- **Module :** Algorithmique et Programmation Python
- **Moyenne de la cohorte :** {round(sum(st['syntax_note'] + st['arch_note'] + st['style_note'] + st['test_note'] - (10.0 if st['ai_prob'] > 80 else 0) for st in STUDENTS)/len(STUDENTS), 2)} / 20.0

---

## 📊 Récapitulatif de la Cohorte

| Étudiant | Login | Note Finale | Profil Typologique Dominant | $S_{{auth}}$ | $I_{{nav}}$ | Runs (Err) | Bursts IA |
| :--- | :---: | :---: | :--- | :---: | :---: | :---: | :---: |
| **Léa Martin** | `lmartin` | **19.5** / 20 | 👑 **Le Boss** | 96.8% | 1.0% | 12 (1) | 0 |
| **Thomas Dubois** | `tdubois` | **14.5** / 20 | 🔨 **L'Artisan** | 95.0% | 13.5% | 26 (11) | 0 |
| **Lucas Bernard** | `lbernard` | **6.5** / 20 | 🤖 **Le Cyborg** | 2.2% | 72.5% | 2 (0) | 1 (142L) |
| **Camille Petit** | `cpetit` | **8.5** / 20 | ⚡ **Le Casse-cou** | 100.0% | 17.5% | 18 (14) | 0 |

---

## Typologies & Répartition par Clusters d'Étudiants Types

| Cluster Typologique | Profil Pédagogique | Effectif | Proportion | Caractéristiques & Recommandations | Étudiants Associés |
| :--- | :--- | :---: | :---: | :--- | :--- |
| 👑 **Les Boss** | Excellence & Autonomie | 1 | 25.0% | Note >= 14. Démarche personnelle complète, code propre et rigoureux. | `lmartin` (Léa Martin) |
| 🔨 **Les Artisans** | Démarche d'Apprentissage & Régularité | 1 | 25.0% | Note entre 10 et 13.9. Socle technique acquis, travail appliqué et régulier. | `tdubois` (Thomas Dubois) |
| ⚡ **Les Casse-cou** | Acharnement & Tâtonnement / Raccourcis | 1 | 25.0% | Acharnement compilatoire sans recul (I_thrash = 0.44), stagnation sur les erreurs. | `cpetit` (Camille Petit) |
| 🚪 **Les Décrocheurs** | Perte de Pied & Décrochage | 0 | 0.0% | Note < 10. Travaux partiels, blocages initiaux ou abandons précoces. | — |
| 🏴‍☠️ **Les Pirates** | Collusion & Copier-Coller | 0 | 0.0% | Similarité directe (>= 75%) sur du code libre ou import de fichiers externes. | — |
| 🤖 **Les Cyborgs** | Recours IA Extérieur & Rupture Stylométrique | 1 | 25.0% | Forte présomption de code généré par IA ou ruptures stylistiques massives. | `lbernard` (Lucas Bernard) |

{generate_cohort_radar({
    'boss': 1, 'artisans': 1, 'casse_cou': 1, 'decrocheurs': 0, 'pirates': 0, 'cyborgs': 1
}, 4)}

---

> [!NOTE]
> Cette cohorte fictive illustre la capacité du système d'évaluation CodeLab à distinguer la compétence réelle, l'effort expérimental, l'impasse pédagogique et la fraude par IA générative sans nécessiter de surveillance intrusive.
"""

    for target_root in (DEMO_DIR, DEMO_DIR_USER):
        (target_root / "synthese_Demo_Telemetry.md").write_text(synthese_md, encoding="utf-8")

    print("[DEMO] Cohorte et rapports fictifs générés avec succès !")

if __name__ == "__main__":
    main()
