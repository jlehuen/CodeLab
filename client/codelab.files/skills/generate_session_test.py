#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate_session_test.py
Générateur de la cohorte complète 'Session_test' à partir de Config_test.xml.
Crée 10 profils d'étudiants distincts illustrant toute la palette didactique :
 1. test01 - James Griffin : 👑 Le Boss Pur (Excellence, rapidité, maîtrise, zéro friction)
 2. test02 - Anthony Ford : 🔨 L'Artisan Modèle (Méthodique, régulier, saisie personnelle)
 3. test03 - Winnie Brown : 🧪 L'Expérimentateur (Empirique, pugnace, 25+ exécutions, zéro IA)
 4. test04 - Katherine Scott : 🤖 Le Cyborg Intégral (Injections massives IA, navettes hors IDE)
 5. test05 - Dana Huggins : 🤖/🔨 Le Cyborg Hybride (Début artisan puis bascule IA sur blocage)
 6. test06 - Piper Dempsey : ⚡ Le Casse-cou (Acharnement stérile, runs compulsifs, I_thrash élevé)
 7. test07 - Donald Craig : 🚪 Le Décrocheur (Perte de pied, inactivité prolongée 35 min, abandon)
 8. test08 - Joshua Darnell : 🆘 L'Élève en Difficulté Solliciteur (Besoin de Guidage, 3 appels tuteur)
 9. test09 - Judith Heard : 🏴‍☠️ La Pirate (Collusion, import fichier FILE_DROP, similarité 88%)
10. test10 - Michael Moore : 📐 Le Théoricien Posé (Longue réflexion papier, code parfait concis)
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

from activity_analyzer import generate_mermaid_radars, generate_cohort_radar

# Dossiers cibles (dans le dépôt et dans le dossier utilisateur CodeLab actif)
SESSION_NAME = "Session_test"
TARGET_DIRS = [
    SKILLS_DIR.parent / "evaluations" / SESSION_NAME,
    Path(os.path.expanduser("~/codelab.files/evaluations")) / SESSION_NAME
]

# -------------------------------------------------------------
# 1. CODES SOURCES PYTHON POUR LES 10 ÉTUDIANTS
# -------------------------------------------------------------

CODES = {
    "test01": '''"""
Module d'analyse vectorisée de flux de logs.
Auteur : James Griffin (test01)
"""
from typing import List, Dict, Tuple
import math

def parse_log_line(line: str) -> Dict[str, str]:
    """Parse une ligne au format cle=valeur."""
    return dict(kv.split("=", 1) for kv in line.strip().split() if "=" in kv)

def compute_error_ratio(records: List[Dict[str, str]]) -> float:
    """Calcule le taux de requêtes en erreur (HTTP >= 400)."""
    if not records:
        return 0.0
    errors = sum(1 for r in records if int(r.get("status", 200)) >= 400)
    return round(errors / len(records), 3)

def run_pipeline(lines: List[str]) -> Tuple[int, float]:
    """Pipeline principal d'ingestion et d'analyse."""
    recs = [parse_log_line(l) for l in lines if l.strip()]
    return len(recs), compute_error_ratio(recs)

if __name__ == "__main__":
    sample = ["ip=10.0.0.1 status=200", "ip=10.0.0.2 status=500"]
    tot, ratio = run_pipeline(sample)
    assert tot == 2 and ratio == 0.5
    print("James Griffin: pipeline validé avec succès.")
''',

    "test02": '''# Analyse de logs - Anthony Ford (test02)
# TP Programmation

def decouper_ligne(ligne):
    """Extrait les cles et valeurs d'une ligne."""
    dico = {}
    elements = ligne.strip().split()
    for el in elements:
        if "=" in el:
            cle, val = el.split("=", 1)
            dico[cle] = val
    return dico

def analyser_logs(liste_lignes):
    """Parcourt les lignes et compte les erreurs."""
    total = 0
    erreurs = 0
    for l in liste_lignes:
        if l.strip():
            total += 1
            info = decouper_ligne(l)
            code = int(info.get("status", 200))
            if code >= 400:
                erreurs += 1
    taux = (erreurs / total) if total > 0 else 0.0
    return total, round(taux, 3)

if __name__ == "__main__":
    data = ["ip=192.168.1.1 status=200", "ip=192.168.1.2 status=404"]
    nb, err = analyser_logs(data)
    print("Anthony Ford: Total =", nb, "Taux erreur =", err)
    assert nb == 2
''',

    "test03": '''# Winnie Brown (test03) - Exercice Logs
# Version finale apres de nombreux tests empiriques

def parse(s):
    res = {}
    for morceau in s.split():
        if "=" in morceau:
            t = morceau.split("=")
            res[t[0]] = t[1]
    return res

def stats(lignes):
    c = 0
    e = 0
    for lig in lignes:
        if len(lig.strip()) > 0:
            c = c + 1
            d = parse(lig)
            val = int(d.get("status", 0))
            if val >= 400:
                e = e + 1
    # print("DEBUG final: c=", c, "e=", e)
    return c, (e / c if c > 0 else 0.0)

# Tests
l = ["status=200 ip=1.1.1.1", "status=500 ip=2.2.2.2"]
tot, r = stats(l)
assert tot == 2
print("Winnie Brown: tests reussis !")
''',

    "test04": '''"""
Ultra-Scalable Distributed Log Ingestion Pipeline
Architecture générée par Assistant IA Externe
"""
import functools
from typing import Mapping, Sequence, Iterator, Tuple

class DistributedLogParser:
    __slots__ = ("_records",)

    def __init__(self, raw_stream: Sequence[str]) -> None:
        self._records = list(self._tokenize_stream(raw_stream))

    def _tokenize_stream(self, stream: Sequence[str]) -> Iterator[Mapping[str, str]]:
        for row in stream:
            if row.strip():
                yield dict(tuple(item.split("=", 1)) for item in row.strip().split() if "=" in item)

    def compute_anomaly_metrics(self) -> Mapping[str, float]:
        total = len(self._records)
        anomalies = sum(1 for r in self._records if int(r.get("status", 200)) >= 400)
        return {"total_records": float(total), "anomaly_ratio": anomalies / max(1, total)}

if __name__ == "__main__":
    p = DistributedLogParser(["status=200", "status=404"])
    print(p.compute_anomaly_metrics())
''',

    "test05": '''# Dana Huggins (test05)
# Debut de code personnel puis fonction avancee

def lire_ligne(ligne):
    d = {}
    for mot in ligne.strip().split():
        if "=" in mot:
            p = mot.split("=")
            d[p[0]] = p[1]
    return d

# Code importe pour resoudre le probleme de calcul :
def advanced_aggregate_logs(stream):
    import collections
    c = collections.Counter()
    valid_entries = [lire_ligne(x) for x in stream if x.strip()]
    errors = [x for x in valid_entries if int(x.get("status", 200)) >= 400]
    return len(valid_entries), len(errors) / max(1, len(valid_entries))

if __name__ == "__main__":
    t, e = advanced_aggregate_logs(["status=200", "status=500"])
    print("Resultat :", t, e)
''',

    "test06": '''# Piper Dempsey (test06) - TP Logs
# Bloque sur le split et le type int

def parse(ligne):
    mots = ligne.split()
    d = {}
    for m in mots:
        # erreur potentielle sur split
        c, v = m.split("=")
        d[c] = v
    return d

def calcul(liste):
    n = 0
    for l in liste:
        d = parse(l)
        # bug recurrente corrigee in extremis
        n += 1
    return n, 0.0

print(calcul(["status=200 ip=1.1.1.1"]))
''',

    "test07": '''# Donald Craig (test07)
# TP commence mais pas termine

def parse_ligne(l):
    # TODO: split
    pass

# Erreur non resolue
# print(parse_ligne("status=200"))
''',

    "test08": '''# Joshua Darnell (test08)
# Realise avec l'aide du tuteur sur les fonctions et dictionnaires

def decouper(ligne):
    # Conseil tuteur : verifier la presence du '='
    d = {}
    for bout in ligne.strip().split():
        if "=" in bout:
            k, v = bout.split("=", 1)
            d[k] = v
    return d

def total_erreurs(lignes):
    # Conseil tuteur : convertir en int
    nb_err = 0
    nb_tot = 0
    for lig in lignes:
        if lig.strip():
            nb_tot += 1
            infos = decouper(lig)
            if int(infos.get("status", 200)) >= 400:
                nb_err += 1
    return nb_tot, (nb_err / nb_tot if nb_tot > 0 else 0.0)

t, r = total_erreurs(["status=200", "status=404"])
print("Joshua Darnell: reussi avec le tuteur :", t, r)
''',

    "test09": '''"""
Module d'analyse vectorisée de flux de logs.
Auteur : Copie directe
"""
from typing import List, Dict, Tuple
import math

def parse_log_line(line: str) -> Dict[str, str]:
    """Parse une ligne au format cle=valeur."""
    return dict(kv.split("=", 1) for kv in line.strip().split() if "=" in kv)

def compute_error_ratio(records: List[Dict[str, str]]) -> float:
    """Calcule le taux de requêtes en erreur (HTTP >= 400)."""
    if not records:
        return 0.0
    errors = sum(1 for r in records if int(r.get("status", 200)) >= 400)
    return round(errors / len(records), 3)

def run_pipeline(lines: List[str]) -> Tuple[int, float]:
    """Pipeline principal d'ingestion et d'analyse."""
    recs = [parse_log_line(l) for l in lines if l.strip()]
    return len(recs), compute_error_ratio(recs)
''',

    "test10": '''"""
Log parser concis et epure.
Michael Moore (test10) - Reflexion prealable sur papier
"""

def parse_and_stats(lines):
    records = [dict(kv.split("=", 1) for kv in l.split() if "=" in kv) for l in lines if l.strip()]
    errs = sum(1 for r in records if int(r.get("status", 200)) >= 400)
    return len(records), round(errs / len(records), 3) if records else 0.0

if __name__ == "__main__":
    n, ratio = parse_and_stats(["status=200", "status=404"])
    assert n == 2 and ratio == 0.5
    print("Michael Moore: OK", n, ratio)
'''
}

# -------------------------------------------------------------
# 2. GÉNÉRATEURS DE TRACES ÉVÉNEMENTIELLES .activity.jsonl
# -------------------------------------------------------------

def make_events_for_student(login: str, start_dt: datetime) -> list:
    evs = []
    t = start_dt

    def add(ev_type, **kw):
        nonlocal t
        delta_s = kw.pop("_dt", 30)
        t += timedelta(seconds=delta_s)
        evs.append({"timestamp": t.strftime("%Y-%m-%dT%H:%M:%SZ"), "ts": t.strftime("%Y-%m-%dT%H:%M:%SZ"), "type": ev_type, **kw})

    add("SESSION", status="CONNECTED", user=login, _dt=0)

    if login == "test01":
        # 👑 James Griffin : Le Boss (frappe continue rapide, 10 runs OK, 0 aide, TTF=15s)
        add("TYPING", typed_chars=220, backspaces=4, _dt=45)
        add("EDIT", file="solution.py", lines_added=15, lines_removed=0, chunk_max=4, _dt=30)
        add("COMPILE", file="solution.py", status="OK", _dt=10)
        add("EXECUTE", file="solution.py", status="DONE", duration_ms=40, _dt=5)
        add("TYPING", typed_chars=340, backspaces=8, _dt=60)
        add("EDIT", file="solution.py", lines_added=18, lines_removed=1, chunk_max=3, _dt=40)
        add("COMPILE", file="solution.py", status="OK", _dt=10)
        add("EXECUTE", file="solution.py", status="DONE", duration_ms=45, _dt=5)
        for _ in range(4):
            add("TYPING", typed_chars=180, backspaces=5, _dt=50)
            add("EDIT", file="solution.py", lines_added=8, lines_removed=1, chunk_max=2, _dt=25)
            add("COMPILE", file="solution.py", status="OK", _dt=8)
            add("EXECUTE", file="solution.py", status="DONE", duration_ms=50, _dt=10)

    elif login == "test02":
        # 🔨 Anthony Ford : L'Artisan (saisie appliquée, 26 backspaces, cycles courts, régulier)
        add("TYPING", typed_chars=140, backspaces=14, _dt=60)
        add("EDIT", file="solution.py", lines_added=8, lines_removed=0, chunk_max=3, _dt=40)
        add("COMPILE", file="solution.py", status="OK", _dt=10)
        add("FOCUS", status="LOST", _dt=120)
        add("FOCUS", status="GAINED", duration_sec=40.0, _dt=40)
        for i in range(5):
            add("TYPING", typed_chars=160, backspaces=12, _dt=90)
            add("EDIT", file="solution.py", lines_added=7, lines_removed=2, chunk_max=2, _dt=35)
            if i == 2:
                add("COMPILE", file="solution.py", status="ERROR", _dt=15)
                add("TYPING", typed_chars=30, backspaces=6, _dt=20) # correction rapide TTF=20s
            add("COMPILE", file="solution.py", status="OK", _dt=10)
            add("EXECUTE", file="solution.py", status="DONE", duration_ms=45, _dt=10)

    elif login == "test03":
        # 🧪 Winnie Brown : L'Expérimentateur (28 exécutions, 11 erreurs surmontées, zéro IA)
        for k in range(8):
            add("TYPING", typed_chars=110, backspaces=16, _dt=50)
            add("EDIT", file="solution.py", lines_added=5, lines_removed=2, chunk_max=2, _dt=25)
            status = "ERROR" if k in (1, 3, 5) else "OK"
            add("COMPILE", file="solution.py", status=status, _dt=10)
            add("EXECUTE", file="solution.py", status="DONE" if status == "OK" else "ERROR", duration_ms=30, _dt=10)

    elif login == "test04":
        # 🤖 Katherine Scott : Le Cyborg (2 bursts massifs 55 et 68 lignes, I_nav=65%)
        add("TYPING", typed_chars=25, backspaces=1, _dt=30)
        add("FOCUS", status="LOST", _dt=60)
        add("FOCUS", status="GAINED", duration_sec=240.0, _dt=240) # 4 min sur ChatGPT
        add("PASTE", file="solution.py", chars=1420, lines=55, _dt=10)
        add("BURST_INSERT", file="solution.py", lines_inserted=55, duration_ms=950, _dt=5)
        add("COMPILE", file="solution.py", status="OK", _dt=15)
        add("EXECUTE", file="solution.py", status="DONE", duration_ms=60, _dt=10)
        add("FOCUS", status="LOST", _dt=180)
        add("FOCUS", status="GAINED", duration_sec=310.0, _dt=310) # 5 min sur Claude/ChatGPT
        add("PASTE", file="solution.py", chars=1850, lines=68, _dt=10)
        add("BURST_INSERT", file="solution.py", lines_inserted=68, duration_ms=1100, _dt=5)
        add("COMPILE", file="solution.py", status="OK", _dt=15)
        add("EXECUTE", file="solution.py", status="DONE", duration_ms=65, _dt=10)

    elif login == "test05":
        # 🤖/🔨 Dana Huggins : Cyborg Hybride (début manuel puis burst 45 lignes sur blocage)
        add("TYPING", typed_chars=190, backspaces=15, _dt=80)
        add("EDIT", file="solution.py", lines_added=12, lines_removed=1, chunk_max=3, _dt=45)
        add("COMPILE", file="solution.py", status="OK", _dt=10)
        add("COMPILE", file="solution.py", status="ERROR", _dt=30)
        add("FOCUS", status="LOST", _dt=60)
        add("FOCUS", status="GAINED", duration_sec=280.0, _dt=280) # recours IA
        add("PASTE", file="solution.py", chars=890, lines=45, _dt=15)
        add("BURST_INSERT", file="solution.py", lines_inserted=45, duration_ms=850, _dt=5)
        add("TYPING", typed_chars=80, backspaces=10, _dt=60)
        add("COMPILE", file="solution.py", status="OK", _dt=10)
        add("EXECUTE", file="solution.py", status="DONE", duration_ms=50, _dt=10)

    elif login == "test06":
        # ⚡ Piper Dempsey : Le Casse-cou (Acharnement I_thrash=0.45, 14 runs compulsifs sans pause)
        add("TYPING", typed_chars=120, backspaces=8, _dt=40)
        add("EDIT", file="solution.py", lines_added=8, lines_removed=0, chunk_max=3, _dt=20)
        for _ in range(7):
            add("COMPILE", file="solution.py", status="ERROR", _dt=8) # 8s entre runs !
            add("TYPING", typed_chars=4, backspaces=2, _dt=6)
            add("EDIT", file="solution.py", lines_added=1, lines_removed=1, chunk_max=1, _dt=4)
        add("ALERT", code="THRASH", message="Boucle d'acharnement sur erreurs (7 runs < 15s)", level="WARN", _dt=5)

    elif login == "test07":
        # 🚪 Donald Craig : Le Décrocheur (inactivité 38 min, 2 runs, abandon silencieux)
        add("TYPING", typed_chars=45, backspaces=3, _dt=30)
        add("EDIT", file="solution.py", lines_added=4, lines_removed=0, chunk_max=2, _dt=20)
        add("COMPILE", file="solution.py", status="ERROR", _dt=10)
        # Blocage et inactivité prolongée (idle 38 min)
        add("FOCUS", status="LOST", _dt=2280)
        add("FOCUS", status="GAINED", duration_sec=2280.0, _dt=10)
        add("SESSION", status="DISCONNECTED", _dt=60)

    elif login == "test08":
        # 🆘 Joshua Darnell : Élève en Difficulté Solliciteur (3 appels tuteur résolus)
        add("TYPING", typed_chars=90, backspaces=12, _dt=60)
        add("COMPILE", file="solution.py", status="ERROR", _dt=15)
        add("HELP", status="REQUESTED", _dt=20) # Appel 1
        add("HELP", status="RESOLVED", duration_sec=180.0, _dt=180) # Tuteur intervient
        add("TYPING", typed_chars=110, backspaces=10, _dt=70)
        add("COMPILE", file="solution.py", status="OK", _dt=10)
        add("HELP", status="REQUESTED", _dt=25) # Appel 2
        add("HELP", status="RESOLVED", duration_sec=140.0, _dt=140)
        add("TYPING", typed_chars=130, backspaces=14, _dt=80)
        add("COMPILE", file="solution.py", status="OK", _dt=10)
        add("EXECUTE", file="solution.py", status="DONE", duration_ms=45, _dt=10)

    elif login == "test09":
        # 🏴‍☠️ Judith Heard : La Pirate (Import FILE_DROP d'un fichier externe)
        add("TYPING", typed_chars=15, backspaces=0, _dt=20)
        add("FILE_DROP", file="solution.py", size_bytes=1045, source="external", _dt=15)
        add("ALERT", code="FILE_DROP", message="Fichier externe importé par glisser-déposer", level="CRIT", _dt=5)
        add("COMPILE", file="solution.py", status="OK", _dt=10)
        add("EXECUTE", file="solution.py", status="DONE", duration_ms=50, _dt=10)

    elif login == "test10":
        # 📐 Michael Moore : Le Théoricien Posé (Longue réflexion, 3 runs seulement, code parfait)
        add("FOCUS", status="LOST", _dt=300) # lecture doc / sujet 5 min
        add("FOCUS", status="GAINED", duration_sec=300.0, _dt=10)
        add("TYPING", typed_chars=190, backspaces=4, _dt=120)
        add("EDIT", file="solution.py", lines_added=12, lines_removed=0, chunk_max=3, _dt=40)
        add("COMPILE", file="solution.py", status="OK", _dt=10)
        add("EXECUTE", file="solution.py", status="DONE", duration_ms=35, _dt=10)

    add("SESSION", status="DISCONNECTED", _dt=30)
    return evs

# -------------------------------------------------------------
# 3. DÉFINITION DES 10 ÉTUDIANTS ET SCORES D'ÉVALUATION
# -------------------------------------------------------------

STUDENTS_CONFIG = [
    {
        "login": "test01", "name": "James Griffin", "ip": "192.168.1.101",
        "syntax": 6.0, "arch": 7.8, "style": 3.9, "tests": 2.0, "total": 19.7,
        "s_auth": 98.2, "i_nav": 1.5, "i_thrash": 0.0, "bursts": 0, "file_drops": 0, "helps": 0,
        "runs": 12, "backspaces": 31, "ttf": 15.0, "idle_max": 90.0, "q_read": 18.0, "q_edit": 72.0,
        "is_suspicious_copy": False, "ai_prob": 2.0,
        "bilan": "Prestation exceptionnelle. L'étudiant a abordé le sujet avec une parfaite compréhension des structures de données (dictionnaires, compréhension de liste) et a conçu d'emblée une architecture fonctionnelle propre et réutilisable. Démarche fluide sans aucune friction ni bogue bloquant.",
        "points_forts": "- Code élégant, concis et parfaitement conforme aux conventions PEP 8.\n- Gestion rigoureuse des cas limites (lignes vides, erreurs HTTP >= 400).\n- Autonomie totale : 100% de frappe personnelle au clavier (S_auth = 98.2%) et zéro demande d'aide.",
        "axes_amelioration": "- Pousser plus loin la robustesse en intégrant le module de logging standard plutôt que de simples print.\n- Peut être sollicité pour du tutorat par les pairs ou sur les défis bonus d'algorithmique avancée.",
        "commentaire": "Travail remarquable James. Tu maîtrises parfaitement les concepts du cours et ta démarche est d'une grande rigueur. Continue ainsi !"
    },
    {
        "login": "test02", "name": "Anthony Ford", "ip": "192.168.1.102",
        "syntax": 5.5, "arch": 6.5, "style": 3.5, "tests": 1.8, "total": 17.3,
        "s_auth": 94.5, "i_nav": 8.0, "i_thrash": 0.05, "bursts": 0, "file_drops": 0, "helps": 0,
        "runs": 8, "backspaces": 78, "ttf": 25.0, "idle_max": 140.0, "q_read": 25.0, "q_edit": 65.0,
        "is_suspicious_copy": False, "ai_prob": 5.0,
        "bilan": "Démarche d'apprentissage exemplaire. Anthony a progressé de manière incrémentale par jalons successifs, testant chaque fonction au fur et à mesure. Les traces révèlent de nombreuses réécritures et ajustements manuels au clavier témoignant d'une réelle appropriation conceptuelle.",
        "points_forts": "- Écriture progressive et soignée du code avec un découpage modulaire clair.\n- Prise en compte active des remarques du compilateur et correction rapide des erreurs (TTF = 25s).\n- Forte concentration locale tout au long de la séance (I_nav = 8.0%).",
        "axes_amelioration": "- Optimiser la conversion des types en intégrant une clause try/except ValueError pour parer à des logs corrompus.\n- Enrichir les jeux de tests unitaires avec des cas d'assertions supplémentaires.",
        "commentaire": "Très bon travail Anthony. Ta régularité et ta méthode de travail pas à pas sont exactement ce qui est attendu. C'est très encourageant !"
    },
    {
        "login": "test03", "name": "Winnie Brown", "ip": "192.168.1.103",
        "syntax": 4.8, "arch": 5.5, "style": 2.8, "tests": 1.7, "total": 14.8,
        "s_auth": 91.0, "i_nav": 11.5, "i_thrash": 0.09, "bursts": 0, "file_drops": 0, "helps": 0,
        "runs": 28, "backspaces": 94, "ttf": 40.0, "idle_max": 110.0, "q_read": 15.0, "q_edit": 75.0,
        "is_suspicious_copy": False, "ai_prob": 4.0,
        "bilan": "Belle pugnacité face aux difficultés. Confrontée à plusieurs erreurs de conversion et de découpage de chaînes, l'étudiante n'a jamais baissé les bras : 28 exécutions au total pour tester, comprendre et affiner sa logique empiriquement. L'authenticité reste irréprochable (S_auth = 91.0%).",
        "points_forts": "- Persévérance exemplaire : 11 erreurs d'exécution surmontées par elle-même sans abandonner.\n- Utilisation active des sorties console pour inspecter l'état des variables.\n- Saisie artisanale intégrale sans chercher de raccourci artificiel.",
        "axes_amelioration": "- Prendre davantage de recul sur papier avant de lancer compulsivement les runs pour diviser par deux les tâtonnements.\n- Nettoyer les instructions print de débogage temporaires avant le rendu final.",
        "commentaire": "Bravo pour ta persévérance Winnie ! Tu as su résoudre tes bogues par toi-même. Pense à poser ta démarche sur brouillon avant de coder pour gagner en temps et en sérénité."
    },
    {
        "login": "test04", "name": "Katherine Scott", "ip": "192.168.1.104",
        "syntax": 5.8, "arch": 7.5, "style": 3.8, "tests": 2.0, "total": 7.0,
        "s_auth": 8.5, "i_nav": 65.0, "i_thrash": 0.0, "bursts": 2, "file_drops": 0, "helps": 0,
        "runs": 4, "backspaces": 3, "ttf": None, "idle_max": 310.0, "q_read": 5.0, "q_edit": 25.0,
        "is_suspicious_copy": False, "ai_prob": 96.0,
        "bilan": "Forte présomption d'assistance externe non maîtrisée. L'étudiante a passé 65% de la séance hors de l'IDE, entrecoupée de deux injections brutales de blocs de code complexes (55 et 68 lignes insérées en 1 seconde chacune). Le code utilise des idiomes (__slots__, NamedTuple) totalement déconnectés des objectifs pédagogiques du niveau.",
        "points_forts": "- Le code fourni est syntaxiquement correct et s'exécute sans erreur.",
        "axes_amelioration": "- Revenir impérativement à une démarche de programmation personnelle : l'évaluation mesure la compréhension et non la capacité à copier-coller.\n- Une soutenance orale individuelle est requise pour vérifier l'aptitude à expliquer le code.",
        "commentaire": "Katherine, l'injection de code sur-ingénié via un modèle externe est immédiatement détectée par la télémétrie. Nous ferons un point en tête-à-tête pour évaluer ce que tu as réellement assimilé."
    },
    {
        "login": "test05", "name": "Dana Huggins", "ip": "192.168.1.105",
        "syntax": 4.5, "arch": 4.5, "style": 2.5, "tests": 1.5, "total": 11.5,
        "s_auth": 38.0, "i_nav": 36.0, "i_thrash": 0.04, "bursts": 1, "file_drops": 0, "helps": 0,
        "runs": 6, "backspaces": 28, "ttf": 55.0, "idle_max": 280.0, "q_read": 20.0, "q_edit": 50.0,
        "is_suspicious_copy": False, "ai_prob": 55.0,
        "bilan": "Profil hybride. Dana a débuté la séance de manière très appliquée en écrivant elle-même ses fonctions de base. Face à un blocage conceptuel sur le comptage, elle a basculé sur un outil IA externe pour générer une fonction avancée (burst de 45 lignes), avant de tenter de l'intégrer à son propre flux.",
        "points_forts": "- Première moitié de séance encourageante avec une démarche personnelle et du code clair.\n- Volonté manifeste d'arriver au bout du problème.",
        "axes_amelioration": "- Préférer appeler le tuteur plutôt que de se tourner vers une solution toute faite qui crée une rupture stylistique évidente.\n- Savoir réécrire et s'approprier les blocs de code empruntés.",
        "commentaire": "Dana, le début de ton travail montre que tu as le potentiel pour réussir seule. Ne cède pas à la facilité de l'IA dès que tu rencontres un obstacle : c'est précisément dans le blocage que se construit l'apprentissage."
    },
    {
        "login": "test06", "name": "Piper Dempsey", "ip": "192.168.1.106",
        "syntax": 3.5, "arch": 3.0, "style": 2.0, "tests": 0.8, "total": 9.3,
        "s_auth": 88.0, "i_nav": 9.0, "i_thrash": 0.44, "bursts": 0, "file_drops": 0, "helps": 0,
        "runs": 16, "backspaces": 22, "ttf": 210.0, "idle_max": 80.0, "q_read": 8.0, "q_edit": 82.0,
        "is_suspicious_copy": False, "ai_prob": 6.0,
        "bilan": "Séance marquée par un fort acharnement stérile. Bloquée sur une erreur de découpage, l'étudiante a enchaîné 16 lancements de compilateur espacés de moins de 10 secondes chacun, en modifiant des caractères au hasard sans lire les messages d'erreur de la console (I_thrash = 0.44).",
        "points_forts": "- Énergie et persévérance continue pour chercher à faire fonctionner son programme.\n- Frappe manuelle honnête et authentique (S_auth = 88.0%).",
        "axes_amelioration": "- Impératif : lire attentivement le traceback et le numéro de ligne dans le terminal avant toute modification.\n- Réduire la cadence d'exécution : s'interdire de relancer dans les 30 secondes suivant une erreur sans avoir posé une hypothèse.",
        "commentaire": "Piper, coder ce n'est pas jouer à la loterie avec le compilateur. Prends le temps de respirer, lis le message d'erreur et utilise des print ciblés pour inspecter tes données."
    },
    {
        "login": "test07", "name": "Donald Craig", "ip": "192.168.1.107",
        "syntax": 2.0, "arch": 1.5, "style": 1.0, "tests": 0.0, "total": 4.5,
        "s_auth": 85.0, "i_nav": 75.0, "i_thrash": 0.0, "bursts": 0, "file_drops": 0, "helps": 0,
        "runs": 2, "backspaces": 5, "ttf": None, "idle_max": 2280.0, "q_read": 60.0, "q_edit": 15.0,
        "is_suspicious_copy": False, "ai_prob": 3.0,
        "bilan": "Séance avortée. Confronté à un message d'erreur dès les premières minutes, Donald s'est résigné : 38 minutes consécutives d'inactivité totale, aucune modification apportée et aucune sollicitation de l'équipe pédagogique.",
        "points_forts": "- Début de syntaxe correct pour l'en-tête de fonction initiale.",
        "axes_amelioration": "- Oser lever la main ou utiliser le bouton d'aide (HELP) : les tuteurs sont présents spécifiquement pour débloquer ces impasses.\n- Participer activement aux séances de soutien de début de semestre.",
        "commentaire": "Donald, rester bloqué en silence pendant 40 minutes est dommage et contre-productif. L'apprentissage de la programmation est fait d'erreurs normales : la prochaine fois, sollicite-nous dès les 5 premières minutes !"
    },
    {
        "login": "test08", "name": "Joshua Darnell", "ip": "192.168.1.108",
        "syntax": 4.0, "arch": 3.5, "style": 2.5, "tests": 1.2, "total": 11.2,
        "s_auth": 92.0, "i_nav": 12.0, "i_thrash": 0.08, "bursts": 0, "file_drops": 0, "helps": 3,
        "runs": 6, "backspaces": 44, "ttf": 75.0, "idle_max": 180.0, "q_read": 35.0, "q_edit": 55.0,
        "is_suspicious_copy": False, "ai_prob": 5.0,
        "bilan": "Attitude exemplaire malgré des lacunes initiales. Joshua a su reconnaître ses limites et a fait appel au tuteur à 3 reprises de manière constructive. Grâce aux indications reçues, il a débloqué chaque étape et est parvenu à un script fonctionnel tout en assurant 100% de la frappe lui-même.",
        "points_forts": "- Excellente écoute et réactivité face aux conseils des enseignants.\n- Progression constante tout au long de la séance.\n- Zéro triche, travail honnête et méritant (S_auth = 92.0%).",
        "axes_amelioration": "- Revoir les manipulations fondamentales sur les chaînes de caractères et les dictionnaires pour gagner en autonomie.\n- Consolider les mécanismes de conversion de types en Python (int, float).",
        "commentaire": "Bravo Joshua pour ton investissement ! Tu as su poser les bonnes questions au bon moment et appliquer les explications avec méthode. Continue avec cette belle énergie."
    },
    {
        "login": "test09", "name": "Judith Heard", "ip": "192.168.1.109",
        "syntax": 5.8, "arch": 7.5, "style": 3.8, "tests": 2.0, "total": 5.0,
        "s_auth": 5.0, "i_nav": 10.0, "i_thrash": 0.0, "bursts": 0, "file_drops": 1, "helps": 0,
        "runs": 3, "backspaces": 0, "ttf": 10.0, "idle_max": 120.0, "q_read": 10.0, "q_edit": 10.0,
        "is_suspicious_copy": True, "ai_prob": 10.0,
        "bilan": "Incident d'intégrité caractérisé. L'étudiante a glissé-déposé un fichier externe (FILE_DROP) contenant l'intégralité de la solution 20 minutes après le début de la séance, sans aucune frappe préalable (S_auth = 5.0%). L'analyse stylométrique confirme une similarité textuelle de 88% avec le code d'un poste voisin.",
        "points_forts": "- Le script déposé est fonctionnel.",
        "axes_amelioration": "- Respecter impérativement la charte d'intégrité académique : le partage de code entre étudiants lors d'une séance évaluée est strictement proscrit.\n- Une note de 5/20 conservatoire est appliquée en attente d'un entretien avec le responsable pédagogique.",
        "commentaire": "Judith, l'import direct d'un fichier externe a déclenché une alerte critique immédiate sur le tableau de bord enseignant. Nous te recevrons en entretien individuel dès la semaine prochaine."
    },
    {
        "login": "test10", "name": "Michael Moore", "ip": "192.168.1.110",
        "syntax": 5.5, "arch": 7.0, "style": 3.8, "tests": 2.0, "total": 18.3,
        "s_auth": 97.5, "i_nav": 6.0, "i_thrash": 0.0, "bursts": 0, "file_drops": 0, "helps": 0,
        "runs": 3, "backspaces": 8, "ttf": 20.0, "idle_max": 300.0, "q_read": 55.0, "q_edit": 40.0,
        "is_suspicious_copy": False, "ai_prob": 3.0,
        "bilan": "Démarche analytique remarquable. Michael a consacré plus de la moitié de son temps à analyser le problème, concevoir l'algorithme sur papier et anticiper les structures de données. Résultat : une saisie d'un seul jet, 3 exécutions seulement et un code d'une concision et d'une clarté exemplaires.",
        "points_forts": "- Très grande maturité algorithmique : analyse préalable poussée évitant les tâtonnements.\n- Code élégant et concis en très peu de lignes.\n- Zéro alerte, authenticité parfaite (S_auth = 97.5%).",
        "axes_amelioration": "- Penser à ajouter des docstrings pour formaliser les contrats d'interface de ses fonctions.\n- Peut accélérer son passage au code pour aborder les questions subsidiaires.",
        "commentaire": "Excellente démarche Michael. Prendre le temps de concevoir sur papier avant de toucher au clavier est la marque des bons développeurs. Félicitations pour ce travail soigné."
    }
]

# -------------------------------------------------------------
# 4. GÉNÉRATION GLOBALE DES RAPPORTS INDIVIDUELS ET DE COHORTE
# -------------------------------------------------------------

def format_bullet_list(text: str) -> str:
    """Garantit que chaque point d'énumération forme un élément de liste Markdown '- ' sur sa propre ligne."""
    if not text:
        return ""
    lines = []
    for raw_line in str(text).strip().splitlines():
        line = raw_line.strip()
        if not line:
            continue
        if line.startswith("•"):
            line = "- " + line[1:].strip()
        elif not line.startswith("- ") and not line.startswith("* "):
            line = "- " + line
        lines.append(line)
    return "\n".join(lines)

def build_student_report(st: dict, radars: dict) -> str:
    """Assemble la fiche d'évaluation individuelle en Markdown avec commentaires pédagogiques complets."""
    now_str = datetime.now().strftime("%d/%m/%Y à %H:%M")
    points_forts = format_bullet_list(st.get("points_forts", ""))
    axes_amelioration = format_bullet_list(st.get("axes_amelioration", ""))
    
    return f"""# Fiche d'Évaluation & Traçabilité Télémétrique : {st['name']}

- **Étudiant :** {st['name']} (`{st['login']}`)
- **Session / Groupe :** Session_test / Group_test
- **Date de l'évaluation :** {now_str}
- **Fichier évalué :** `solution.py`

---

## 🎯 Bilan Chiffré & Notation Technique

| Pilier d'Évaluation | Note Obtenue | Barème | Critères & Observations Didactiques |
| :--- | :---: | :---: | :--- |
| **Axe 1 : Syntaxe & Structure** | **{st['syntax']:.1f}** | / 6.0 | Analyse statique et conformité de la grammaire syntaxique |
| **Axe 2 : Conception & Modularité** | **{st['arch']:.1f}** | / 8.0 | Découpage fonctionnel, modularité et algorithmique |
| **Axe 3 : Style & Bonnes Pratiques** | **{st['style']:.1f}** | / 4.0 | PEP8, clarté, lisibilité et conventions de nommage |
| **Axe 4 : Validation & Assertions** | **{st['tests']:.1f}** | / 2.0 | Tests unitaires, gestion des cas limites et assertions |
| **Note Technique Brute** | **{(st['syntax'] + st['arch'] + st['style'] + st['tests']):.1f}** | **/ 20.0** | Somme des piliers d'évaluation technique |
| **NOTE FINALE** | **{st['total']:.1f}** | **/ 20.0** | **Profil : {radars.get('profil_dominant', 'Évalué')}** |

---

{radars['radars_markdown']}

---

### 🔬 Données Clés de Télémétrie d'Activité

- **Authenticité de frappe ($S_{{auth}}$) :** **{st['s_auth']:.1f}%**
- **Temps hors de l'IDE ($I_{{nav}}$) :** **{st['i_nav']:.1f}%**
- **Indice d'acharnement ($I_{{thrash}}$) :** **{st['i_thrash']:.2f}**
- **Exécutions & Compilations :** **{st['runs']}** runs
- **Injections massives (Bursts) :** **{st['bursts']}** alerte(s)
- **Imports de fichiers externes :** **{st['file_drops']}** événement(s)
- **Demandes d'aide au tuteur :** **{st['helps']}** résolue(s)

---

### Bilan de Progression & Analyse Pédagogique

{st['bilan']}

---

### Points Forts Observés

{points_forts}

---

### Axes d'Amélioration & Conseils Méthodologiques

{axes_amelioration}

---

### Commentaire de l'Enseignant

> *« {st['commentaire']} »*
"""

def generate_cohort_summary(students_results: list) -> str:
    """Génère la synthèse de cohorte Session_test avec les 6 clusters."""
    now_str = datetime.now().strftime("%d/%m/%Y à %H:%M")
    avg_note = sum(s["total"] for s in students_results) / len(students_results)
    
    # Répartition dans les clusters
    clusters_def = [
        {"id": "boss", "label": "👑 **Les Boss**", "sub": "Excellence & Autonomie", "members": []},
        {"id": "artisans", "label": "🔨 **Les Artisans**", "sub": "Démarche d'Apprentissage & Régularité", "members": []},
        {"id": "casse_cou", "label": "⚡ **Les Casse-cou**", "sub": "Acharnement & Tâtonnement", "members": []},
        {"id": "decrocheurs", "label": "🚪 **Les Décrocheurs**", "sub": "Perte de Pied & Décrochage", "members": []},
        {"id": "pirates", "label": "🏴‍☠️ **Les Pirates**", "sub": "Collusion & Copier-Coller", "members": []},
        {"id": "cyborgs", "label": "🤖 **Les Cyborgs**", "sub": "Recours IA Extérieur", "members": []}
    ]
    c_map = {c["id"]: c for c in clusters_def}

    for s in students_results:
        dom = s["radars"]["profil_dominant"]
        entry = f"`{s['login']}` ({s['name']})"
        if "Boss" in dom:
            c_map["boss"]["members"].append(entry)
        elif "Artisan" in dom:
            c_map["artisans"]["members"].append(entry)
        elif "Casse-cou" in dom:
            c_map["casse_cou"]["members"].append(entry)
        elif "Décrocheur" in dom:
            c_map["decrocheurs"]["members"].append(entry)
        elif "Pirate" in dom:
            c_map["pirates"]["members"].append(entry)
        elif "Cyborg" in dom:
            c_map["cyborgs"]["members"].append(entry)
        else:
            c_map["artisans"]["members"].append(entry)

    lines = [
        f"# Synthèse d'Évaluation & Audit Télémétrique : Session_test\n",
        f"- **Date de l'audit :** {now_str}",
        f"- **Session :** `Session_test` (Groupe : `Group_test`)",
        f"- **Effectif évalué :** {len(students_results)} étudiants",
        f"- **Moyenne générale de la cohorte :** **{avg_note:.2f} / 20.0**\n",
        "---\n",
        "## 📊 Récapitulatif Global de la Cohorte\n",
        "| Étudiant | Login | Note Finale | Profil Dominant | $S_{auth}$ | $I_{nav}$ | Runs | Alertes |",
        "| :--- | :---: | :---: | :--- | :---: | :---: | :---: | :--- |"
    ]

    for s in students_results:
        dom = s["radars"]["profil_dominant"]
        alt_str = "Aucune"
        if s["bursts"] > 0:
            alt_str = f"🚨 {s['bursts']} Burst(s)"
        elif s["file_drops"] > 0:
            alt_str = "🚨 FILE_DROP"
        elif s["i_thrash"] > 0.3:
            alt_str = "⚠️ THRASH"

        lines.append(f"| **{s['name']}** | `{s['login']}` | **{s['total']:.1f}** / 20 | {dom} | {s['s_auth']:.1f}% | {s['i_nav']:.1f}% | {s['runs']} | {alt_str} |")

    lines.append("\n---\n")
    lines.append("## 🧭 Typologies & Répartition par Clusters d'Étudiants Types\n")
    lines.append("| Cluster Typologique | Intitulé Didactique | Effectif | Proportion | Étudiants Associés |")
    lines.append("| :--- | :--- | :---: | :---: | :--- |")

    for c in clusters_def:
        cnt = len(c["members"])
        pct = (cnt / len(students_results)) * 100.0
        m_str = ", ".join(c["members"]) if c["members"] else "—"
        lines.append(f"| {c['label']} | {c['sub']} | **{cnt}** | {pct:.1f}% | {m_str} |")

    lines.append("")
    lines.append(generate_cohort_radar(clusters_def, len(students_results)))
    lines.append("\n---\n")
    lines.append("> 💡 **Exploitation didactique** : Cette cohorte d'évaluation montre comment l'analyse conjointe des indicateurs de code et de la télémétrie permet d'adapter l'accompagnement pédagogique à chaque profil d'apprenant.")
    return "\n".join(lines)

def main():
    print(f"[{SESSION_NAME}] Génération de 10 profils d'étudiants distincts...")
    start_dt = datetime.now() - timedelta(hours=2)

    students_results = []

    for st in STUDENTS_CONFIG:
        login = st["login"]
        name = st["name"]

        # 1. Traces fines .activity.jsonl
        events = make_events_for_student(login, start_dt)

        # 2. Préparation du dictionnaire pour le calcul des radars
        act_analysis = {
            "active": True,
            "s_auth": st["s_auth"],
            "i_nav": st["i_nav"],
            "i_thrash": st["i_thrash"],
            "burst_insert_count": st["bursts"],
            "burst_new_file_count": 0,
            "file_drop_count": st["file_drops"],
            "help_requests": st["helps"],
            "typing_backspaces": st["backspaces"],
            "ttf_avg_sec": st["ttf"],
            "idle_max_sec": st["idle_max"],
            "time_quadrants": {
                "q_read_pct": st["q_read"],
                "q_edit_pct": st["q_edit"],
                "q_test_pct": 20.0,
                "q_away_pct": st["i_nav"]
            },
            "compilations": {"total": st["runs"]},
            "executions": {"total": max(1, st["runs"] - 2)}
        }

        data_for_radar = {
            "scores": {
                "syntax": st["syntax"],
                "architecture": st["arch"],
                "style": st["style"],
                "tests": st["tests"],
                "note_technique_brute": st["total"],
                "total": st["total"]
            },
            "activity_analysis": act_analysis,
            "ai_analysis": {"probability_pct": st["ai_prob"]},
            "is_suspicious_copy": st["is_suspicious_copy"]
        }

        # 3. Calcul des 2 radars et du tableau diagnostique
        radars = generate_mermaid_radars(data_for_radar, name)

        # 4. Fiche Markdown individuelle
        report_md = build_student_report(st, radars)
        code_py = CODES[login]

        st_entry = dict(st)
        st_entry["radars"] = radars
        students_results.append(st_entry)

        # 5. Déploiement des fichiers pour chaque dossier cible
        for target_root in TARGET_DIRS:
            st_dir = target_root / login
            st_dir.mkdir(parents=True, exist_ok=True)

            (st_dir / "solution.py").write_text(code_py, encoding="utf-8")
            (st_dir / f"{login}_rapport.md").write_text(report_md, encoding="utf-8")

            with open(st_dir / ".activity.jsonl", "w", encoding="utf-8") as f:
                for ev in events:
                    f.write(json.dumps(ev, ensure_ascii=False) + "\n")

        print(f"  -> Profil généré : {name} ({login}) => {radars['profil_dominant']} (Note: {st['total']}/20)")

    # 6. Synthèse globale de cohorte
    cohort_md = generate_cohort_summary(students_results)
    for target_root in TARGET_DIRS:
        (target_root / f"synthese_{SESSION_NAME}.md").write_text(cohort_md, encoding="utf-8")

        # Fichier CSV des notes
        csv_lines = ["login;nom;note_finale;profil_dominant;s_auth;i_nav"]
        for s in students_results:
            csv_lines.append(f"{s['login']};{s['name']};{s['total']};{s['radars']['profil_dominant']};{s['s_auth']};{s['i_nav']}")
        (target_root / f"notes_{SESSION_NAME}.csv").write_text("\n".join(csv_lines), encoding="utf-8")

        # users.json
        users_json = [
            {"login": s["login"], "name": s["name"], "group": "Group_test", "status": "STUDENT", "ip": s["ip"]}
            for s in students_results
        ]
        (target_root / "users.json").write_text(json.dumps(users_json, indent=2, ensure_ascii=False), encoding="utf-8")

    print(f"\n[{SESSION_NAME}] Cohorte de 10 étudiants générée avec succès dans :")
    for d in TARGET_DIRS:
        print(f"  - {d}")

if __name__ == "__main__":
    main()
