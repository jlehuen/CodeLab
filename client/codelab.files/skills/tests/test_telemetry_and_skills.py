#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Suite de tests unitaires et d'invariants pour la télémétrie CodeLab,
l'analyseur d'activité, l'orchestrateur eval_codelab.py et les 4 skills génériques.
Exécutable avec : python3 -m unittest discover -s client/codelab.files/skills/tests -v
"""

import csv
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SKILLS_ROOT = Path(__file__).resolve().parent.parent
if str(SKILLS_ROOT) not in sys.path:
    sys.path.insert(0, str(SKILLS_ROOT))

import activity_analyzer as aa
import eval_codelab as ec
import skills_common as sc


def _write_jsonl(student_dir: Path, events: list[dict]):
    student_dir.mkdir(parents=True, exist_ok=True)
    log_path = student_dir / ".activity.jsonl"
    with open(log_path, "w", encoding="utf-8") as f:
        for ev in events:
            f.write(json.dumps(ev) + "\n")


def _make_ipynb(path: Path, lang: str, code_lines: list[str]):
    kernels = {
        "python": ({"name": "python3", "language": "python"}, {"name": "python"}),
        "c": ({"name": "c", "language": "c"}, {"name": "c"}),
        "haskell": ({"name": "haskell", "language": "haskell"}, {"name": "haskell"}),
    }
    ks, li = kernels[lang]
    nb = {
        "cells": [
            {
                "cell_type": "code",
                "execution_count": 1,
                "metadata": {},
                "outputs": [],
                "source": [ln if ln.endswith("\n") else ln + "\n" for ln in code_lines],
            }
        ],
        "metadata": {"kernelspec": ks, "language_info": li},
        "nbformat": 4,
        "nbformat_minor": 5,
    }
    path.write_text(json.dumps(nb, indent=2), encoding="utf-8")


class TestTelemetryAndSkills(unittest.TestCase):
    def test_pre_155_and_view_only_no_fake_authenticity(self):
        """Client pré-1.5.5 / VIEW-only avec BURST_NEW_FILE serveur -> has_rich_telemetry=False, ni 100% ni Cyborg 0%."""
        with tempfile.TemporaryDirectory() as tmp:
            s_dir = Path(tmp) / "etu_old"
            _write_jsonl(s_dir, [
                {"type": "SESSION", "timestamp": "2026-11-10T09:00:00Z", "status": "CONNECTED"},
                {"type": "VIEW", "timestamp": "2026-11-10T09:05:00Z", "file": "sujet.md"},
                {"type": "VIEW", "timestamp": "2026-11-10T09:25:00Z", "file": "exo1.py"},
                {
                    "type": "EDIT",
                    "timestamp": "2026-11-10T09:26:00Z",
                    "file": "exo1.py",
                    "lines_added": 30,
                    "lines_removed": 0,
                    "chunk_max": 30,
                    "alert": "BURST_NEW_FILE",
                },
                {"type": "SESSION", "timestamp": "2026-11-10T09:30:00Z", "status": "DISCONNECTED", "duration_sec": 1800},
            ])

            act = aa.analyze_student_activity(s_dir)
            self.assertFalse(act["has_rich_telemetry"])
            self.assertIsNone(act["s_auth"])
            self.assertEqual(act["sliding_windows"], [])
            self.assertNotIn("100.0%", act["summary_markdown"])

            # Vérifie que extract_integrity_indicators n'affiche pas "Authentique (S_auth: 100%)"
            integ = ec.extract_integrity_indicators({"activity_analysis": act, "ai_analysis": {}, "dates_info": {}})
            self.assertNotIn("S_auth: 100%", integ["chronometrie_courte"])

            # Vérifie que le radar ne catalogue pas en Cyborg à cause du BURST_NEW_FILE serveur seul
            rad = aa.generate_mermaid_radars({
                "scores": {"syntax": 5.5, "architecture": 7.0, "style": 3.5, "tests": 1.5, "note_technique_brute": 17.5, "total": 17.5},
                "ai_analysis": {"probability_pct": 0.0},
                "activity_analysis": act,
            }, "Ancien Client")
            self.assertNotEqual(rad["primary_profile_id"], "cyborg")

    def test_multi_day_sessions_sliding_windows_and_ttf(self):
        """2 séances d'1h espacées de 7 jours -> 8 fenêtres (pas 672) et TTF borné intra-séance."""
        with tempfile.TemporaryDirectory() as tmp:
            s_dir = Path(tmp) / "etu_multiday"
            events = [
                # Séance 1 : 03/11 09:00 -> 10:00 (4 tranches de 15 min)
                {"type": "SESSION", "timestamp": "2026-11-03T09:00:00Z", "status": "CONNECTED"},
                {"type": "TYPING", "timestamp": "2026-11-03T09:05:00Z", "file": "tp.py", "typed_chars": 120, "backspaces": 5},
                {"type": "COMPILE", "timestamp": "2026-11-03T09:20:00Z", "file": "tp.py", "status": "ERROR"},
                {"type": "COMPILE", "timestamp": "2026-11-03T09:21:00Z", "file": "tp.py", "status": "OK"},  # TTF = 60s
                {"type": "TYPING", "timestamp": "2026-11-03T09:40:00Z", "file": "tp.py", "typed_chars": 150, "backspaces": 10},
                # Erreur laissée non résolue en fin de séance 1
                {"type": "COMPILE", "timestamp": "2026-11-03T09:58:00Z", "file": "tp.py", "status": "ERROR"},
                {"type": "SESSION", "timestamp": "2026-11-03T10:00:00Z", "status": "DISCONNECTED", "duration_sec": 3600},
                # Séance 2 : 10/11 09:00 -> 10:00 (7 jours plus tard, 4 tranches de 15 min)
                {"type": "SESSION", "timestamp": "2026-11-10T09:00:00Z", "status": "CONNECTED"},
                {"type": "COMPILE", "timestamp": "2026-11-10T09:02:00Z", "file": "tp.py", "status": "OK"},  # Ne doit PAS créer un TTF de 7 jours
                {"type": "TYPING", "timestamp": "2026-11-10T09:20:00Z", "file": "tp.py", "typed_chars": 200, "backspaces": 12},
                {"type": "TYPING", "timestamp": "2026-11-10T09:40:00Z", "file": "tp.py", "typed_chars": 180, "backspaces": 8},
                {"type": "EXECUTE", "timestamp": "2026-11-10T09:59:00Z", "file": "tp.py", "status": "DONE"},
                {"type": "SESSION", "timestamp": "2026-11-10T10:00:00Z", "status": "DISCONNECTED", "duration_sec": 3600},
            ]
            _write_jsonl(s_dir, events)

            act = aa.analyze_student_activity(s_dir)
            self.assertTrue(act["has_rich_telemetry"])
            self.assertEqual(len(act["sliding_windows"]), 8)
            # Préfixe date %d/%m présent en multi-jours
            self.assertIn("03/11", act["sliding_windows"][0]["t0_str"])
            self.assertIn("10/11", act["sliding_windows"][-1]["t0_str"])
            # TTF strictement intra-séance (= 60.0s, pas 600000s)
            self.assertEqual(act["ttf_avg_sec"], 60.0)
            # Le texte I_nav affiche la durée active, pas 10080 min
            self.assertNotIn("10080", act["summary_markdown"])

    def test_rust_python_parity_on_canonical_personas(self):
        """6 personas canoniques + Architecte Méthodique -> classification dominante cohérente."""
        personas = [
            (
                "boss_artisan",
                {"syntax": 6.0, "architecture": 8.0, "style": 4.0, "tests": 2.0, "note_technique_brute": 20.0, "total": 20.0},
                [
                    {"type": "SESSION", "timestamp": "2026-11-10T09:00:00Z", "status": "CONNECTED"},
                    {"type": "TYPING", "timestamp": "2026-11-10T09:05:00Z", "file": "a.py", "typed_chars": 1200, "backspaces": 30},
                    *[{"type": "EXECUTE", "timestamp": f"2026-11-10T09:{10+i:02d}:00Z", "file": "a.py", "status": "DONE"} for i in range(6)],
                    {"type": "SESSION", "timestamp": "2026-11-10T09:30:00Z", "status": "DISCONNECTED", "duration_sec": 1800},
                ],
                {"boss", "artisan"},
            ),
            (
                "cyborg",
                {"syntax": 5.0, "architecture": 6.0, "style": 3.0, "tests": 1.0, "note_technique_brute": 15.0, "total": 12.0},
                [
                    {"type": "SESSION", "timestamp": "2026-11-10T09:00:00Z", "status": "CONNECTED"},
                    {"type": "FOCUS", "timestamp": "2026-11-10T09:15:00Z", "status": "GAINED", "duration_sec": 800},
                    {"type": "PASTE", "timestamp": "2026-11-10T09:16:00Z", "file": "a.py", "chars": 1500, "lines": 50},
                    {"type": "EDIT", "timestamp": "2026-11-10T09:16:05Z", "file": "a.py", "lines_added": 50, "lines_removed": 0, "chunk_max": 50, "alert": "BURST_NEW_FILE"},
                    {"type": "TYPING", "timestamp": "2026-11-10T09:17:00Z", "file": "a.py", "typed_chars": 20, "backspaces": 1},
                    {"type": "SESSION", "timestamp": "2026-11-10T09:20:00Z", "status": "DISCONNECTED", "duration_sec": 1200},
                ],
                {"cyborg"},
            ),
            (
                "casse_cou",
                {"syntax": 2.0, "architecture": 3.0, "style": 2.0, "tests": 0.0, "note_technique_brute": 7.0, "total": 7.0},
                [
                    {"type": "SESSION", "timestamp": "2026-11-10T09:00:00Z", "status": "CONNECTED"},
                    {"type": "TYPING", "timestamp": "2026-11-10T09:02:00Z", "file": "a.py", "typed_chars": 200, "backspaces": 5},
                    {"type": "COMPILE", "timestamp": "2026-11-10T09:03:00Z", "file": "a.py", "status": "ERROR"},
                    {"type": "COMPILE", "timestamp": "2026-11-10T09:03:10Z", "file": "a.py", "status": "ERROR"},
                    {"type": "COMPILE", "timestamp": "2026-11-10T09:03:20Z", "file": "a.py", "status": "ERROR"},
                    {"type": "SESSION", "timestamp": "2026-11-10T09:10:00Z", "status": "DISCONNECTED", "duration_sec": 600},
                ],
                {"casse_cou"},
            ),
            (
                "decrocheur",
                {"syntax": 1.0, "architecture": 1.0, "style": 0.5, "tests": 0.0, "note_technique_brute": 2.5, "total": 2.5},
                [
                    {"type": "SESSION", "timestamp": "2026-11-10T09:00:00Z", "status": "CONNECTED"},
                    {"type": "TYPING", "timestamp": "2026-11-10T09:01:00Z", "file": "a.py", "typed_chars": 15, "backspaces": 0},
                    {"type": "VIEW", "timestamp": "2026-11-10T09:25:00Z", "file": "sujet.md"},
                    {"type": "SESSION", "timestamp": "2026-11-10T09:30:00Z", "status": "DISCONNECTED", "duration_sec": 1800},
                ],
                {"decrocheur"},
            ),
            (
                "pirate",
                {"syntax": 5.0, "architecture": 6.0, "style": 3.0, "tests": 1.0, "note_technique_brute": 15.0, "total": 13.0},
                [
                    {"type": "SESSION", "timestamp": "2026-11-10T09:00:00Z", "status": "CONNECTED"},
                    {"type": "FILE_DROP", "timestamp": "2026-11-10T09:02:00Z", "file": "exo1.py", "size_bytes": 1024},
                    {"type": "FILE_DROP", "timestamp": "2026-11-10T09:03:00Z", "file": "exo2.py", "size_bytes": 2048},
                    {"type": "FILE_DROP", "timestamp": "2026-11-10T09:04:00Z", "file": "exo3.py", "size_bytes": 2048},
                    {"type": "SESSION", "timestamp": "2026-11-10T09:10:00Z", "status": "DISCONNECTED", "duration_sec": 600},
                ],
                {"pirate"},
            ),
        ]

        with tempfile.TemporaryDirectory() as tmp:
            for name, scores, events, expected_ids in personas:
                s_dir = Path(tmp) / name
                _write_jsonl(s_dir, events)
                act = aa.analyze_student_activity(s_dir)
                rad = aa.generate_mermaid_radars({
                    "scores": scores,
                    "ai_analysis": {"probability_pct": 0.0},
                    "activity_analysis": act,
                }, name)
                self.assertIn(
                    rad["primary_profile_id"],
                    expected_ids,
                    f"Persona {name}: expected one of {expected_ids}, got {rad['primary_profile_id']} ({rad['valeurs_profils']})",
                )
                if name == "boss_artisan":
                    self.assertTrue(rad["is_double_profil"])
                    self.assertEqual(rad["double_profil_surnom"], "L'Architecte Méthodique")

    def test_plagiarism_triggers_pirate_radar_and_excludes_tutors(self):
        """2 étudiants identiques + 1 tuteur corrigé -> tuteur exclu, malus cohérent CSV/synthèse/fiche et Pirate >= 16."""
        py_solution = (
            "def calculer_somme(liste):\n"
            "    \"\"\"Calcule la somme des elements pairs.\"\"\"\n"
            "    total = 0\n"
            "    for element in liste:\n"
            "        if element % 2 == 0:\n"
            "            total += element\n"
            "    return total\n\n"
            "def calculer_moyenne(liste):\n"
            "    \"\"\"Calcule la moyenne des elements.\"\"\"\n"
            "    if not liste:\n"
            "        return 0\n"
            "    return sum(liste) / len(liste)\n\n"
            "assert calculer_somme([2, 4, 5]) == 6\n"
            "assert calculer_moyenne([2, 4]) == 3\n"
        )
        hs_solution = (
            "module Exo1 where\n\n"
            "sommePairs :: [Int] -> Int\n"
            "sommePairs [] = 0\n"
            "sommePairs (x:xs)\n"
            "  | even x    = x + sommePairs xs\n"
            "  | otherwise = sommePairs xs\n\n"
            "produitListe :: [Int] -> Int\n"
            "produitListe [] = 1\n"
            "produitListe (x:xs) = x * produitListe xs\n\n"
            "main :: IO ()\n"
            "main = print (sommePairs [1,2,3,4,5,6] + produitListe [1,2,3])\n"
        )

        for skill_name, fname, code in [
            ("eval-python-sample", "exo1.py", py_solution),
            ("eval-haskell-sample", "Exo1.hs", hs_solution),
        ]:
            with tempfile.TemporaryDirectory() as tmp:
                sess_dir = Path(tmp) / "session_plag"
                eval_dir = Path(tmp) / "eval_out"
                for uid in ("etu1", "etu2", "tuteur1"):
                    d = sess_dir / uid
                    d.mkdir(parents=True)
                    (d / fname).write_text(code, encoding="utf-8")

                users_json = Path(tmp) / "users.json"
                users_json.write_text(json.dumps([
                    {"login": "etu1", "name": "Alice", "role": "student"},
                    {"login": "etu2", "name": "Bob", "role": "student"},
                    {"login": "tuteur1", "name": "Prof", "role": "tutor"},
                ]), encoding="utf-8")

                ec.evaluate_session(
                    "session_plag",
                    str(sess_dir),
                    eval_output_dir=str(eval_dir),
                    sessions_xml=str(users_json),
                    ai_props={"GEMINI_API_KEY": ""},
                    skill_name=skill_name,
                )

                csv_path = eval_dir / "notes_session_plag.csv"
                synth_path = eval_dir / "synthese_session_plag.md"
                self.assertTrue(csv_path.is_file(), f"CSV manquant pour {skill_name}")
                self.assertTrue(synth_path.is_file(), f"Synthèse manquante pour {skill_name}")

                with open(csv_path, encoding="utf-8") as f:
                    rows = list(csv.DictReader(f, delimiter=";"))
                logins = [r["login"] for r in rows]
                self.assertEqual(sorted(logins), ["etu1", "etu2"], f"Tuteur non exclu dans {skill_name}")
                self.assertNotIn("tuteur1", synth_path.read_text(encoding="utf-8"))

                # Politique zéro faux positif : note technique conservée (aucun malus auto), mais alerte rouge + Pirate dans synthèse et fiche
                for r in rows:
                    self.assertEqual(float(r["note_finale"]), float(r["note_sans_malus"]), f"Aucun malus auto attendu dans {skill_name}")
                    self.assertIn("Pirate", r["profil_didactique"], f"Profil Pirate absent pour {r['login']} ({skill_name})")
                fiche1 = (eval_dir / "etu1" / "etu1_rapport.md").read_text(encoding="utf-8")
                self.assertIn("Le Pirate", fiche1)
                self.assertIn("ALERTE INTÉGRITÉ", fiche1)
                self.assertIn("#dc2626", fiche1)
                synth_txt = synth_path.read_text(encoding="utf-8")
                self.assertIn("ALERTES D'INTÉGRITÉ", synth_txt)
                self.assertIn("#dc2626", synth_txt)
                self.assertIn("| Note brute (/20) | Malus |", synth_txt)

    def test_ipynb_all_languages_no_json_indent_false_positive(self):
        """Carnets .ipynb Python, C et Haskell : 0 faux positif indentation_spaces_rupture, compilation vérifiée, inclus en cohorte."""
        with tempfile.TemporaryDirectory() as tmp:
            # 1. Python .ipynb valide (4 espaces standards Jupyter, pas de .py à côté)
            py_sess = Path(tmp) / "sess_py"
            py_etu = py_sess / "etu_nb_py"
            py_etu.mkdir(parents=True)
            _make_ipynb(py_etu / "tp1.ipynb", "python", [
                "def carre(x):",
                "    \"\"\"Retourne le carre de x.\"\"\"",
                "    return x * x",
                "assert carre(3) == 9",
            ])
            py_mod = ec._load_skill_module(SKILLS_ROOT / "eval-python-sample" / "scripts" / "audit_student.py", "eval-python-sample")
            py_coh = ec._load_skill_module(SKILLS_ROOT / "eval-python-sample" / "scripts" / "audit_cohort.py", "eval-python-sample")
            res_py = py_mod.audit_student(str(py_etu))
            self.assertEqual(res_py["valid_ast_count"], 1)
            indent_markers = [m for m in res_py["ai_analysis"]["markers"] if m.get("subtype") == "indentation_spaces_rupture"]
            self.assertEqual(indent_markers, [], "Faux positif indentation_spaces_rupture sur .ipynb Python")
            self.assertEqual(len(py_coh.analyze_cohort(str(py_sess))), 1, "Cohorte Python doit inclure dossier .ipynb-only")

            # 2. C .ipynb invalide : doit échouer la compilation (pas 6/6) et être inclus en cohorte
            c_sess = Path(tmp) / "sess_c"
            c_etu = c_sess / "etu_nb_c"
            c_etu.mkdir(parents=True)
            _make_ipynb(c_etu / "broken.ipynb", "c", [
                "#include <stdio.h>",
                "int main() {",
                "    int x = ;",
                "    return 0;",
                "}",
            ])
            c_mod = ec._load_skill_module(SKILLS_ROOT / "eval-c-sample" / "scripts" / "audit_student.py", "eval-c-sample")
            c_coh = ec._load_skill_module(SKILLS_ROOT / "eval-c-sample" / "scripts" / "audit_cohort.py", "eval-c-sample")
            res_c = c_mod.audit_student(str(c_etu))
            self.assertIn("broken.ipynb", res_c["syntax_errors"], "Un .ipynb C cassé doit échouer la compilation")
            self.assertEqual(res_c["valid_c_count"], 0)
            self.assertEqual(len(c_coh.analyze_cohort(str(c_sess))), 1, "Cohorte C doit inclure dossier .ipynb-only")

            # 3. Haskell .ipynb valide
            hs_etu = Path(tmp) / "etu_nb_hs"
            hs_etu.mkdir(parents=True)
            _make_ipynb(hs_etu / "tp1.ipynb", "haskell", [
                "double :: Int -> Int",
                "double x = x * 2",
            ])
            hs_mod = ec._load_skill_module(SKILLS_ROOT / "eval-haskell-sample" / "scripts" / "audit_student.py", "eval-haskell-sample")
            res_hs = hs_mod.audit_student(str(hs_etu))
            self.assertEqual(res_hs["total_files"], 1)
            self.assertEqual(res_hs["valid_hs_count"], 1)

    def test_custom_weights_json_propagates_to_radars_and_penalties(self):
        """Barème personnalisé (tests sur 6.0 pts, note 2.0/6.0) -> radar affiche 6.7/20 (et non 20.0/20)."""
        data = {
            "scores": {
                "syntax": 4.0,
                "architecture": 6.0,
                "style": 2.0,
                "tests": 2.0,
                "note_technique_brute": 14.0,
                "total": 14.0,
            },
            "axes_max": {
                "syntax": 4.0,
                "architecture": 6.0,
                "style": 4.0,
                "tests": 6.0,
            },
            "ai_analysis": {"probability_pct": 0.0},
            "activity_analysis": {},
        }
        rad = aa.generate_mermaid_radars(data, "Test Barème")
        self.assertAlmostEqual(rad["valeurs_indicateurs"]["test"], 6.7, places=1)
        tbl = ec.build_generic_scores_table(data["scores"], data, "Bien")
        self.assertIn("/ 6.0", tbl)

        # Politique zéro faux positif : compute_similarity_malus renvoie toujours 0.0
        custom_weights = {"anti_triche": {"malus_max_cumule_global": 5.0}}
        malus, max_c = sc.compute_similarity_malus({"files": [("a.py", 82.0)], "max_sim": 82.0}, custom_weights)
        self.assertEqual(malus, 0.0)
        self.assertEqual(max_c, 5.0)
        malus_below, _ = sc.compute_similarity_malus({"files": [("a.py", 78.0)], "max_sim": 78.0}, custom_weights)
        self.assertEqual(malus_below, 0.0)

    def test_cli_modes_eval_dir_isolation(self):
        """eval_codelab.py --student avec --eval-dir -> rapport dans eval_dir uniquement, rien dans session_dir/evaluations."""
        with tempfile.TemporaryDirectory() as tmp:
            sess_dir = Path(tmp) / "ma_session"
            etu_dir = sess_dir / "jdupont"
            etu_dir.mkdir(parents=True)
            (etu_dir / "exo.py").write_text("def f(x):\n    return x + 1\nassert f(1) == 2\n", encoding="utf-8")
            eval_dir = Path(tmp) / "isolated_evals"

            cmd = [
                sys.executable,
                str(SKILLS_ROOT / "eval_codelab.py"),
                "--student", "jdupont",
                "--dir", str(sess_dir),
                "--eval-dir", str(eval_dir),
                "--skill", "eval-python-sample",
                "--no-ai",
            ]
            proc = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
            self.assertEqual(proc.returncode, 0, f"CLI failed:\nSTDOUT:\n{proc.stdout}\nSTDERR:\n{proc.stderr}")

            expected_report = eval_dir / "jdupont" / "jdupont_rapport.md"
            self.assertTrue(expected_report.is_file(), "Rapport absent de --eval-dir")
            self.assertFalse((sess_dir / "evaluations").exists(), "Aucun dossier evaluations/ ne doit être créé dans le dossier synchronisé")

    def test_c1_ai_and_plagiarism_no_auto_penalty_but_red_banner(self):
        """Politique zéro faux positif : aucun malus automatique IA/plagiat, mais bandeau rouge bien visible."""
        note_ia_seule, m1 = sc.apply_combined_penalties(19.0, 80.0, None)
        note_ia_plus_plag, m2 = sc.apply_combined_penalties(
            19.0, 80.0, {"files": [("exo1.c", 92.0)], "max_sim": 92.0}
        )
        self.assertEqual(note_ia_seule, 19.0)
        self.assertEqual(note_ia_plus_plag, 19.0)
        self.assertEqual(m1, 0.0)
        self.assertEqual(m2, 0.0)
        banner = sc.build_integrity_red_banner(
            {"ai_analysis": {"probability_pct": 80.0, "level": "Élevé (80%)"}},
            plagiarism_info={"partner": "etu2", "max_sim": 92.0, "files": [("exo1.c", 92.0)], "severity": "Forte suspicion"},
        )
        self.assertIn("#dc2626", banner)
        self.assertIn("ALERTE INTÉGRITÉ", banner)
        self.assertIn("Aucun malus automatique", banner)

    def test_c2_without_telemetry_profile_calibration(self):
        """C2 : Sans télémétrie, 5/20 -> decrocheur (pas boss), 10/20 -> pas Architecte Méthodique, casse_cou atteignable."""
        rad_low = aa.generate_mermaid_radars({
            "scores": {"syntax": 1.5, "architecture": 2.0, "style": 1.0, "tests": 0.5, "note_technique_brute": 5.0, "total": 5.0},
            "ai_analysis": {"probability_pct": 0.0},
            "activity_analysis": {},
        }, "Faible Sans Tel")
        self.assertEqual(rad_low["primary_profile_id"], "decrocheur")

        rad_mid = aa.generate_mermaid_radars({
            "scores": {"syntax": 3.0, "architecture": 4.0, "style": 2.0, "tests": 1.0, "note_technique_brute": 10.0, "total": 10.0},
            "ai_analysis": {"probability_pct": 0.0},
            "activity_analysis": {},
        }, "Moyen Sans Tel")
        self.assertFalse(rad_mid["is_double_profil"], "10/20 ne doit pas déclencher le double profil Architecte Méthodique")

        rad_casse = aa.generate_mermaid_radars({
            "scores": {"syntax": 5.0, "architecture": 2.5, "style": 1.0, "tests": 0.0, "note_technique_brute": 8.5, "total": 8.5},
            "ai_analysis": {"probability_pct": 0.0},
            "activity_analysis": {},
            "critical_skips": ["TP2E1", "TP3E2"],
            "snippets": {"anti_patterns": [{"title": "ap1"}, {"title": "ap2"}, {"title": "ap3"}]},
        }, "Casse Cou Sans Tel")
        self.assertEqual(rad_casse["primary_profile_id"], "casse_cou")

    def test_c3_paste_burst_subdir_dedup(self):
        """C3 : PASTE sur 'exo1.c' + EDIT BURST sur 'TP1/exo1.c' en <30s ne doivent pas être comptés deux fois."""
        with tempfile.TemporaryDirectory() as tmp:
            s_dir = Path(tmp) / "etu_dedup"
            _write_jsonl(s_dir, [
                {"type": "SESSION", "timestamp": "2026-11-10T09:00:00Z", "status": "CONNECTED"},
                {"type": "TYPING", "timestamp": "2026-11-10T09:05:00Z", "file": "exo1.c", "typed_chars": 900, "backspaces": 10},
                {"type": "PASTE", "timestamp": "2026-11-10T09:10:00Z", "file": "exo1.c", "chars": 900, "lines": 30},
                {"type": "EDIT", "timestamp": "2026-11-10T09:10:05Z", "file": "TP1/exo1.c", "lines_added": 30, "lines_removed": 0, "chunk_max": 30, "alert": "BURST_NEW_FILE"},
                {"type": "SESSION", "timestamp": "2026-11-10T09:15:00Z", "status": "DISCONNECTED", "duration_sec": 900},
            ])
            act = aa.analyze_student_activity(s_dir)
            self.assertEqual(act["s_auth"], 50.0, f"Double comptage détecté : s_auth={act['s_auth']}")

    def test_c4_ithrash_monotonicity(self):
        """C4 : I_thrash doit être strictement supérieur pour 20 erreurs consécutives que pour 3."""
        with tempfile.TemporaryDirectory() as tmp:
            def _make_err_log(name, n_errs):
                d = Path(tmp) / name
                evs = [{"type": "SESSION", "timestamp": "2026-11-10T09:00:00Z", "status": "CONNECTED"}]
                evs.append({"type": "TYPING", "timestamp": "2026-11-10T09:01:00Z", "file": "a.c", "typed_chars": 150, "backspaces": 5})
                for i in range(n_errs):
                    evs.append({"type": "COMPILE", "timestamp": f"2026-11-10T09:{2 + i // 6:02d}:{(i % 6) * 10:02d}Z", "file": "a.c", "status": "ERROR"})
                evs.append({"type": "SESSION", "timestamp": "2026-11-10T09:20:00Z", "status": "DISCONNECTED", "duration_sec": 1200})
                _write_jsonl(d, evs)
                return aa.analyze_student_activity(d)

            act_3 = _make_err_log("err3", 3)
            act_20 = _make_err_log("err20", 20)
            self.assertGreater(act_20["i_thrash"], act_3["i_thrash"])

    def test_c5_ai_prob_merges_with_rich_telemetry_and_drop_no_boss(self):
        """C5 : ai_prob=100% avec télémétrie riche déclenche quand même cyborg ; 1 FILE_DROP sans frappe != Boss/Artisan."""
        with tempfile.TemporaryDirectory() as tmp:
            s_dir = Path(tmp) / "etu_ai_tel"
            _write_jsonl(s_dir, [
                {"type": "SESSION", "timestamp": "2026-11-10T09:00:00Z", "status": "CONNECTED"},
                {"type": "TYPING", "timestamp": "2026-11-10T09:05:00Z", "file": "a.py", "typed_chars": 800, "backspaces": 20},
                {"type": "EXECUTE", "timestamp": "2026-11-10T09:10:00Z", "file": "a.py", "status": "DONE"},
                {"type": "SESSION", "timestamp": "2026-11-10T09:20:00Z", "status": "DISCONNECTED", "duration_sec": 1200},
            ])
            act = aa.analyze_student_activity(s_dir)
            rad = aa.generate_mermaid_radars({
                "scores": {"syntax": 5.5, "architecture": 7.0, "style": 3.5, "tests": 1.5, "note_technique_brute": 17.5, "total": 12.0},
                "ai_analysis": {"probability_pct": 100.0},
                "activity_analysis": act,
            }, "Cyborg Stylo")
            self.assertEqual(rad["primary_profile_id"], "cyborg")

            drop_dir = Path(tmp) / "etu_drop1"
            _write_jsonl(drop_dir, [
                {"type": "SESSION", "timestamp": "2026-11-10T09:00:00Z", "status": "CONNECTED"},
                {"type": "FILE_DROP", "timestamp": "2026-11-10T09:02:00Z", "file": "exo1.py", "size_bytes": 2048},
                {"type": "SESSION", "timestamp": "2026-11-10T09:10:00Z", "status": "DISCONNECTED", "duration_sec": 600},
            ])
            act_drop = aa.analyze_student_activity(drop_dir)
            rad_drop = aa.generate_mermaid_radars({
                "scores": {"syntax": 5.5, "architecture": 7.0, "style": 3.5, "tests": 1.5, "note_technique_brute": 17.5, "total": 17.5},
                "ai_analysis": {"probability_pct": 0.0},
                "activity_analysis": act_drop,
            }, "Drop Unique")
            self.assertNotIn(rad_drop["primary_profile_id"], ("boss", "artisan"))

            # S_auth < 50% sans burst : ne doit pas être Boss ni Artisan
            low_auth_dir = Path(tmp) / "etu_low_auth"
            _write_jsonl(low_auth_dir, [
                {"type": "SESSION", "timestamp": "2026-11-10T09:00:00Z", "status": "CONNECTED"},
                {"type": "TYPING", "timestamp": "2026-11-10T09:05:00Z", "file": "a.py", "typed_chars": 450, "backspaces": 10},
                {"type": "PASTE", "timestamp": "2026-11-10T09:06:00Z", "file": "a.py", "chars": 550, "lines": 12},
                {"type": "EXECUTE", "timestamp": "2026-11-10T09:10:00Z", "file": "a.py", "status": "DONE"},
                {"type": "SESSION", "timestamp": "2026-11-10T09:20:00Z", "status": "DISCONNECTED", "duration_sec": 1200},
            ])
            act_la = aa.analyze_student_activity(low_auth_dir)
            rad_la = aa.generate_mermaid_radars({
                "scores": {"syntax": 5.0, "architecture": 6.5, "style": 3.0, "tests": 1.5, "note_technique_brute": 16.0, "total": 16.0},
                "ai_analysis": {"probability_pct": 0.0},
                "activity_analysis": act_la,
            }, "Low Auth")
            self.assertLessEqual(rad_la["valeurs_profils"]["boss"], 9.5)
            self.assertLessEqual(rad_la["valeurs_profils"]["artisan"], 9.5)

    def test_c6_pure_pep8_python_no_indentation_rupture(self):
        """C6 : Un script Python purement PEP 8 (4 espaces, >=40 lignes) ne doit déclencher aucun marqueur indentation_spaces_rupture."""
        with tempfile.TemporaryDirectory() as tmp:
            etu_dir = Path(tmp) / "etu_pep8"
            etu_dir.mkdir(parents=True)
            lines = ["def somme_plage(n):", '    """Somme des entiers jusqu a n."""', "    total = 0"]
            for i in range(45):
                lines.append(f"    total += {i}")
            lines.append("    return total")
            (etu_dir / "pep8.py").write_text("\n".join(lines) + "\n", encoding="utf-8")

            py_mod = ec._load_skill_module(SKILLS_ROOT / "eval-python-sample" / "scripts" / "audit_student.py", "eval-python-sample")
            res = py_mod.audit_student(str(etu_dir))
            indent_markers = [m for m in res["ai_analysis"]["markers"] if m.get("subtype") == "indentation_spaces_rupture"]
            self.assertEqual(indent_markers, [], f"Faux positif PEP 8 : {indent_markers}")


if __name__ == "__main__":
    unittest.main()
