#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
audit_student.py - Moteur d'audit automatique d'un dossier Python (Exemple).
Analyse l'AST standard des fichiers .py et extrait des métriques de conformité.
Dernière modification : 10/10/2026
"""

import os
import sys
import ast
import re
import json
from pathlib import Path
from datetime import datetime

SKILL_DIR = Path(__file__).resolve().parent.parent


def load_weights():
    """Charge dynamiquement les pondérations depuis weights.json."""
    for p in (SKILL_DIR / "weights.json", Path(__file__).resolve().parent / "weights.json"):
        if p.is_file():
            try:
                return json.loads(p.read_text(encoding="utf-8"))
            except Exception:
                pass
    return {}


def strip_python_comments_and_strings(content):
    """
    Supprime les commentaires (# ...) et neutralise le contenu des littéraux de chaînes
    (y compris les docstrings '''...''' et \"\"\"...\"\"\") tout en conservant les sauts de ligne.
    """
    pattern = re.compile(
        r'("""[\s\S]*?"""|\'\'\'[\s\S]*?\'\'\'|"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])*\'|#[^\n]*)',
        re.DOTALL
    )
    def _repl(match):
        s = match.group(0)
        if s.startswith('#'):
            return ''
        newlines = '\n' * s.count('\n')
        if s.startswith('"""') or s.startswith("'''"):
            return f'"""{newlines}"""'
        return f'""{newlines}'
    return pattern.sub(_repl, content)


class CodeVisitor(ast.NodeVisitor):
    def __init__(self):
        self.functions = []
        self.classes = []
        self.docstrings_count = 0
        self.asserts_count = 0
        self.comprehensions_count = 0
        self.for_loops = 0
        self.while_loops = 0
        self.bare_except_lines = []

    def visit_FunctionDef(self, node):
        self.functions.append(node.name)
        if ast.get_docstring(node):
            self.docstrings_count += 1
        self.generic_visit(node)

    def visit_AsyncFunctionDef(self, node):
        self.functions.append(node.name)
        if ast.get_docstring(node):
            self.docstrings_count += 1
        self.generic_visit(node)

    def visit_ClassDef(self, node):
        self.classes.append(node.name)
        if ast.get_docstring(node):
            self.docstrings_count += 1
        self.generic_visit(node)

    def visit_Assert(self, node):
        self.asserts_count += 1
        self.generic_visit(node)

    def visit_ListComp(self, node):
        self.comprehensions_count += 1
        self.generic_visit(node)

    def visit_DictComp(self, node):
        self.comprehensions_count += 1
        self.generic_visit(node)

    def visit_SetComp(self, node):
        self.comprehensions_count += 1
        self.generic_visit(node)

    def visit_GeneratorExp(self, node):
        self.comprehensions_count += 1
        self.generic_visit(node)

    def visit_ExceptHandler(self, node):
        if node.type is None and hasattr(node, 'lineno'):
            self.bare_except_lines.append(node.lineno)
        self.generic_visit(node)

    def visit_For(self, node):
        self.for_loops += 1
        self.generic_visit(node)

    def visit_While(self, node):
        self.while_loops += 1
        self.generic_visit(node)

def format_lines(lines, start_idx, end_idx, highlight_line=None, note=None):
    """Formate une portion de lignes avec numérotation 'ligne | code' et annotation optionnelle."""
    start = max(0, start_idx)
    end = min(len(lines), end_idx)
    res = []
    for i in range(start, end):
        l_num = i + 1
        line_str = lines[i].rstrip('\r\n')
        if l_num == highlight_line and note:
            res.append(f"{l_num:3d} | {line_str}  # <-- {note}")
        else:
            res.append(f"{l_num:3d} | {line_str}")
    return '\n'.join(res)

def detect_ai_markers(py_files):
    """
    Analyse statique et stylométrique multi-dimensionnelle des signatures d'assistance IA en Python :
    1. Typage statique et bibliothèques hors-programme (typing, dataclass, pandas, scipy, sklearn, collections avancées).
    2. Syntaxe avancée Python 3.10+ (match/case, walrus :=).
    3. Stylométrie didactique Chatbot (docstrings Google/Sphinx formelles, étapes didactiques # Étape 1 :, commentaires anglais).
    4. Chronométrie CodeLab : fichiers complexes créés en moins de 60s (copier-coller de prompt).
    Applique des plafonds par catégorie et requiert au moins 2 catégories distinctes pour le seuil Élevé.
    """
    markers = []
    category_weights = {}
    subtype_counts = {}
    CATEGORY_CAPS = {
        'typage_statique': 0.30,
        'bibliotheque_hors_programme': 0.40,
        'syntaxe_avancee': 0.30,
        'stylometrie_ia': 0.35,
        'variance_stylometrique': 0.35,
        'chronometrie_instantanee': 0.35,
        'mention_explicite': 1.0,
    }

    def add_marker(rel_path, category, subtype, description, base_weight):
        markers.append({
            'file': rel_path,
            'category': category,
            'description': description
        })
        cnt = subtype_counts.get(subtype, 0)
        subtype_counts[subtype] = cnt + 1
        factor = 1.0 if cnt == 0 else (0.35 if cnt == 1 else 0.15)
        cur = category_weights.get(category, 0.0)
        cap = CATEGORY_CAPS.get(category, 0.35)
        category_weights[category] = min(cap, cur + base_weight * factor)

    created_timestamps = []
    file_indent = {}
    total_tabs = 0
    total_spaces = 0

    for fpath in py_files:
        rel_path = fpath.name
        try:
            content = fpath.read_text(encoding='utf-8', errors='ignore')
        except Exception:
            continue

        code_nc = strip_python_comments_and_strings(content)
        lines = content.splitlines()
        loc = len([l for l in lines if l.strip() and not l.strip().startswith('#')])

        # Analyse d'indentation (Tabulations natives CodeLab vs 4 Espaces LLM Web)
        t_count = sum(1 for l in lines if l.startswith('\t'))
        s_count = sum(1 for l in lines if l.startswith('    '))
        total_tabs += t_count
        total_spaces += s_count
        file_indent[rel_path] = {'tabs': t_count, 'spaces': s_count, 'loc': loc}

        # Horodatage CodeLab
        m_dt = re.search(r'Created by .* on (\d{2}/\d{2}/\d{2}) \((\d{2}:\d{2}:\d{2})\)', content)
        if m_dt:
            try:
                dt = datetime.strptime(f"{m_dt.group(1)} {m_dt.group(2)}", "%d/%m/%y %H:%M:%S")
                created_timestamps.append((dt, rel_path, loc))
            except Exception:
                pass

        # 1. Typage statique & typing (vérifié hors commentaires et chaînes)
        if re.search(r'\bfrom\s+typing\s+import\b|\bimport\s+typing\b', code_nc) or re.search(r'def\s+[a-zA-Z0-9_]+\([^)]*:\s*(?:int|str|float|bool|List|Dict|Tuple|Optional|Any)\b', code_nc):
            add_marker(rel_path, 'typage_statique', 'typing', "Annotations de type statique (typing / Type Hints) hors-programme", 0.25)

        # 2. Bibliothèques avancées (pandas, scipy, sklearn, dataclass)
        ext_libs = re.findall(r'\b(?:import|from)\s+(pandas|scipy|sklearn|dataclasses|itertools|functools)\b', code_nc)
        if ext_libs:
            uniq = sorted(list(set(ext_libs)))
            add_marker(rel_path, 'bibliotheque_hors_programme', 'ext_libs', f"Bibliothèque avancée hors-programme : {', '.join(uniq)}", 0.30)

        # 3. Syntaxe Python 3.10+ (match/case, walrus) hors commentaires
        if re.search(r'^\s*match\s+[^:]+:\s*$', code_nc, re.MULTILINE):
            add_marker(rel_path, 'syntaxe_avancee', 'match_case', "Syntaxe moderne 'match / case' (Python 3.10+)", 0.25)

        if ':=' in code_nc:
            add_marker(rel_path, 'syntaxe_avancee', 'walrus', "Opérateur walrus (:=) non enseigné", 0.22)

        # 4. Docstrings industrielles formelles (Google / Sphinx)
        if re.search(r'"""[\s\S]*?\b(?:Args|Parameters|Returns|Raises)\s*:\s*\n', content):
            add_marker(rel_path, 'stylometrie_ia', 'docstring_google', "Docstrings formelles industrielles (Args: / Returns:)", 0.25)

        # 5. Découpage didactique et étapes scolaires de Chatbot
        didactic_steps = re.findall(r'#\s*(?:(?:[ÉE]tape|Phase|Step)\s+\d+|Cas de base|Gestion des erreurs|Boucle principale|Chargement des données)\b', content, re.I)
        if len(didactic_steps) >= 2:
            add_marker(rel_path, 'stylometrie_ia', 'didactic_steps', f"Découpage didactique typique de LLM : '{didactic_steps[0].strip()}'", 0.25)

        # Commentaires en anglais
        english_comments = re.findall(r'#\s*(?:Main loop|Load data|Parse file|Calculate (?:mean|sum|average)|Plot (?:graph|results)|Helper function)\b', content, re.I)
        if english_comments:
            add_marker(rel_path, 'stylometrie_ia', 'english_comments', f"Commentaires en anglais typiques d'une invite LLM : '{english_comments[0].strip()}'", 0.25)

        # Aveu explicite de copier-coller ou d'aide externe dans les commentaires
        if re.search(r'#\s*(?:copier\s*coller|copi[eé]\s*coll[eé]|aide\s+(?:avec|sur)\s+(?:forum|site|internet))', content, re.I):
            add_marker(rel_path, 'stylometrie_ia', 'copy_paste_admission', "Aveu explicite dans les commentaires de copier-coller ou d'aide externe (forum/site)", 0.35)

        # Mentions directes d'outils IA
        if re.search(r'#\s*(?:ChatGPT|Copilot|Claude|OpenAI|DeepSeek|AI generated|Prompt)', content, re.I):
            add_marker(rel_path, 'mention_explicite', 'explicit_ai', "Mention textuelle d'outil IA dans les commentaires", 1.0)

    # Rupture d'indentation intra-copie Tab ➔ 4 Espaces
    for f_rel, ind in file_indent.items():
        other_tabs = total_tabs - ind['tabs']
        other_spaces = total_spaces - ind['spaces']
        if ind['spaces'] >= 25 and (ind['spaces'] > ind['tabs'] * 3 or ind['tabs'] <= 3):
            if other_tabs >= 30 and other_tabs > other_spaces * 2:
                add_marker(f_rel, 'variance_stylometrique', 'indentation_spaces_rupture',
                           f"Rupture d'indentation intra-copie : {ind['spaces']} lignes en 4 espaces contre {ind['tabs']} tabulation(s), en contraste avec le style natif CodeLab ({other_tabs} tabulations dans le reste de la copie)", 0.35)

    # Chronométrie (< 300s pour scripts complexes, < 60s pour fichiers standards)
    created_timestamps.sort(key=lambda x: x[0])
    for i in range(1, len(created_timestamps)):
        dt_prev, f_prev, loc_prev = created_timestamps[i - 1]
        dt_curr, f_curr, loc_curr = created_timestamps[i]
        diff_sec = (dt_curr - dt_prev).total_seconds()
        if 0 < diff_sec <= 300 and loc_curr >= 60:
            add_marker(f_curr, 'chronometrie_instantanee', 'fast_complex_creation', f"Script complexe ({loc_curr} lignes) produit en seulement {int(diff_sec)}s (< 5 min) après {f_prev} (copier-coller externe / IA probable)", 0.35)
        elif 0 < diff_sec < 60 and loc_curr >= 25 and loc_prev >= 15:
            add_marker(f_curr, 'chronometrie_instantanee', 'fast_creation', f"Création quasi-instantanée ({int(diff_sec)}s) d'un script complexe ({loc_curr} lignes)", 0.30)

    total_weight = sum(category_weights.values())
    active_categories = [cat for cat, w in category_weights.items() if w >= 0.15]
    if len(active_categories) < 2 and 'mention_explicite' not in category_weights:
        total_weight = min(total_weight, 0.45)

    score_pct = min(100, int(round(total_weight * 100)))
    score = min(1.0, round(total_weight, 2))
    if score_pct >= 60:
        level = f"Élevé ({score_pct}% - Forte présomption d'assistance IA)"
    elif score_pct >= 25:
        level = f"Modéré ({score_pct}% - Idiomes atypiques détectés)"
    else:
        level = f"Faible ({score_pct}% - Code étudiant authentique)"

    descriptions = [f"{m['file']}: {m['description']}" for m in markers[:8]]
    return {
        'level': level,
        'score': score,
        'probability_pct': score_pct,
        'count': len(markers),
        'markers': markers,
        'details': "; ".join(descriptions) if descriptions else "Aucune signature d'IA détectée"
    }

def audit_student(folder_path, sessions_xml_path=None):
    folder = Path(folder_path).resolve()
    py_files = [f for f in folder.glob("*.py") if not f.name.startswith(".") and not f.name.endswith(".bak")]
    if not py_files:
        def _is_ign(f):
            try:
                return any(p.startswith(".") or p in ("__pycache__", "evaluations", "codelab", ".venv", "env", "build") for p in f.relative_to(folder).parts[:-1]) or f.name.endswith(".bak")
            except Exception:
                return False
        py_files = [f for f in folder.rglob("*.py") if not _is_ign(f)]

    # Détection de l'identité et du travail
    student_id = folder.name
    assignment = folder.name
    if bool(re.match(r"^(tp\d*|td\d*|projet.*|partie.*|src|exercices?)$", folder.name, re.I)) and folder.parent.name:
        student_id = folder.parent.name
        assignment = f"{folder.parent.name}/{folder.name}"

    student_name = None
    if sessions_xml_path and os.path.isfile(sessions_xml_path):
        try:
            import xml.etree.ElementTree as ET
            tree = ET.parse(sessions_xml_path)
            for u in tree.findall(".//user"):
                if u.get("login") == student_id and u.get("name"):
                    student_name = u.get("name")
                    break
        except Exception:
            pass

    if not student_name:
        all_py = py_files if py_files else list(folder.glob("**/*.py"))
        for f in all_py:
            try:
                cnt = f.read_text(encoding="utf-8", errors="ignore")[:3000]
                m = re.search(r"Created by\s+([^0-9\r\n\t]+?)\s+on\s+[0-9]{2}/[0-9]{2}/[0-9]{2}", cnt)
                if m:
                    cand = m.group(1).strip()
                    if cand and len(cand) >= 3 and cand.lower() not in ("john doe", "none", "inconnu", "anonymous", "_author_"):
                        student_name = cand
                        break
            except Exception:
                pass

    if not student_name and folder.parent and folder.parent.name == student_id:
        for f in list(folder.parent.glob("*.py")) + list(folder.parent.glob("**/*.py")):
            try:
                cnt = f.read_text(encoding="utf-8", errors="ignore")[:3000]
                m = re.search(r"Created by\s+([^0-9\r\n\t]+?)\s+on\s+[0-9]{2}/[0-9]{2}/[0-9]{2}", cnt)
                if m:
                    cand = m.group(1).strip()
                    if cand and len(cand) >= 3 and cand.lower() not in ("john doe", "none", "inconnu", "anonymous", "_author_"):
                        student_name = cand
                        break
            except Exception:
                pass

    results = {
        "student_id": student_id,
        "student_name": student_name or student_id,
        "assignment": assignment,
        "folder": str(folder),
        "files_count": len(py_files),
        "py_files": [f.name for f in py_files],
        "valid_ast_count": 0,
        "syntax_errors": {},
        "functions": [],
        "classes": [],
        "docstrings_count": 0,
        "asserts_count": 0,
        "comprehensions_count": 0,
        "scores": {
            "syntax": 0.0,
            "architecture": 0.0,
            "style": 0.0,
            "tests": 0.0,
            "total": 0.0
        },
        "feedback": []
    }

    if not py_files:
        results["feedback"].append("Aucun fichier source Python (.py) trouvé dans le dossier.")
        return results

    all_functions = []
    all_classes = []
    total_docstrings = 0
    total_asserts = 0
    total_comprehensions = 0
    snippets = {
        "compilation_errors": [],
        "anti_patterns": [],
        "highlights": []
    }

    for py_file in py_files:
        content = ""
        lines = []
        try:
            content = py_file.read_text(encoding="utf-8", errors="replace")
            lines = content.splitlines()
            tree = ast.parse(content, filename=py_file.name)
            results["valid_ast_count"] += 1

            visitor = CodeVisitor()
            visitor.visit(tree)

            all_functions.extend(visitor.functions)
            all_classes.extend(visitor.classes)
            total_docstrings += visitor.docstrings_count
            total_asserts += visitor.asserts_count
            total_comprehensions += visitor.comprehensions_count

            # Highlights : fonction documentée avec docstring (max 1)
            if len(snippets["highlights"]) < 1:
                doc_m = re.search(r'((?:def|class)\s+[a-zA-Z_][a-zA-Z0-9_]*\s*\(.*?\)\s*:\s*\n\s*(?:"""|\'\'\')[\s\S]*?(?:"""|\'\'\'))', content)
                if doc_m:
                    start_l = content[:doc_m.start()].count('\n')
                    match_lines = lines[start_l : min(len(lines), start_l + 7)]
                    formatted = '\n'.join(f"{start_l + idx + 1:3d} | {l.rstrip()}" for idx, l in enumerate(match_lines))
                    snippets["highlights"].append({
                        "title": "Fonction/Classe documentée avec docstring PEP 257",
                        "file": py_file.name,
                        "line": start_l + 1,
                        "code": formatted
                    })

            # Highlights : tests unitaires / assertions (max 1)
            if ("test" in py_file.name.lower() or "assert" in content) and not any("test" in h["title"].lower() for h in snippets["highlights"]):
                ass_m = re.search(r'^(.*?\bassert\s+.*)$', content, re.MULTILINE)
                if ass_m:
                    l_num = content[:ass_m.start()].count('\n') + 1
                    snip = format_lines(lines, l_num - 2, l_num + 2, highlight_line=l_num, note="Assertion de test")
                    snippets["highlights"].append({
                        "title": "Vérification unitaire par assertion",
                        "file": py_file.name,
                        "line": l_num,
                        "code": snip
                    })

            # Anti-pattern : except nu (bare except détecté par l'AST hors chaînes/commentaires)
            if len(snippets["anti_patterns"]) < 1 and visitor.bare_except_lines:
                l_num = visitor.bare_except_lines[0]
                snip = format_lines(lines, l_num - 1, l_num + 2, highlight_line=l_num, note="Anti-pattern : bare except (attrape aussi KeyboardInterrupt/SystemExit)")
                snippets["anti_patterns"].append({
                    "title": "Exception nue (bare except: au lieu de except Exception:)",
                    "file": py_file.name,
                    "line": l_num,
                    "code": snip
                })

        except SyntaxError as se:
            results["syntax_errors"][py_file.name] = f"Ligne {se.lineno}: {se.msg}"
            if len(snippets["compilation_errors"]) < 2:
                code_snip = format_lines(lines, (se.lineno or 1) - 3, (se.lineno or 1) + 2, highlight_line=se.lineno, note=se.msg)
                snippets["compilation_errors"].append({
                    "file": py_file.name,
                    "line": se.lineno or 1,
                    "error": se.msg,
                    "code": code_snip
                })
        except Exception as e:
            results["syntax_errors"][py_file.name] = str(e)

    results["snippets"] = snippets
    results["functions"] = all_functions
    results["classes"] = all_classes
    results["docstrings_count"] = total_docstrings
    results["asserts_count"] = total_asserts
    results["comprehensions_count"] = total_comprehensions

    weights = load_weights()
    axes_w = weights.get("axes", {})
    syntax_max = float(axes_w.get("axe1_compilation_max", 6.0))
    arch_max = float(axes_w.get("axe2_conception_max", 8.0))
    style_max = float(axes_w.get("axe3_style_max", 4.0))
    tests_max = float(axes_w.get("axe4_validation_max", 2.0))

    # 1. Syntaxe & AST (/6)
    num_errors = len(py_files) - results["valid_ast_count"]
    if py_files:
        ratio = results["valid_ast_count"] / len(py_files)
        results["scores"]["syntax"] = max(0.0, round(ratio * syntax_max - 1.0 * num_errors, 1))

    # 2. Conception & Modularité exigeante (/8)
    unique_funcs = set(all_functions)
    arch_pts = 0.0
    if len(unique_funcs) >= 5:
        arch_pts += 6.0
    elif len(unique_funcs) >= 3:
        arch_pts += 4.5
    elif len(unique_funcs) >= 1:
        arch_pts += 2.5
    if len(all_classes) >= 1 and total_comprehensions >= 1:
        arch_pts += 2.0
    elif len(all_classes) >= 1 or total_comprehensions >= 2:
        arch_pts += 1.5
    elif total_comprehensions == 1:
        arch_pts += 0.8
    if num_errors > 0 and py_files:
        arch_pts *= max(0.5, ratio)
    results["scores"]["architecture"] = min(arch_max, round((arch_pts / 8.0) * arch_max, 1))

    # 3. Qualité & Style Pythonique (/4)
    style_pts = 0.0
    expected_docs = max(2, len(unique_funcs) + len(all_classes))
    doc_cov = total_docstrings / float(expected_docs) if expected_docs > 0 else 0.0
    if doc_cov >= 0.75 and total_docstrings >= 2:
        style_pts += 2.0
    elif doc_cov >= 0.40 or total_docstrings >= 2:
        style_pts += 1.2
    elif total_docstrings >= 1:
        style_pts += 0.6
    if total_comprehensions >= 2:
        style_pts += 1.0
    elif total_comprehensions == 1:
        style_pts += 0.5
    if len(py_files) > 0 and not snippets["anti_patterns"] and num_errors == 0:
        style_pts += 1.0
    elif len(py_files) > 0 and not snippets["anti_patterns"]:
        style_pts += 0.4
    results["scores"]["style"] = min(style_max, round((style_pts / 4.0) * style_max, 1))

    # 4. Tests (/2)
    has_test_file = any("test" in f.name.lower() for f in py_files)
    if total_asserts >= 4 or (has_test_file and total_asserts >= 2):
        test_pts = 2.0
    elif total_asserts >= 2 or has_test_file:
        test_pts = 1.2
    elif total_asserts == 1:
        test_pts = 0.5
    else:
        test_pts = 0.0
    results["scores"]["tests"] = min(tests_max, round((test_pts / 2.0) * tests_max, 1))

    ai_info = detect_ai_markers(py_files)
    results["ai_analysis"] = ai_info
    ai_prob = ai_info.get("probability_pct", 0)

    if ai_prob >= 60:
        results["scores"]["tests"] = min(results["scores"]["tests"], 0.2)
        results["scores"]["architecture"] = max(0.0, round(results["scores"]["architecture"] - 1.5, 1))
        results["scores"]["style"] = max(0.2, round(results["scores"]["style"] - 0.5, 1))
        cur_tot = results["scores"]["syntax"] + results["scores"]["architecture"] + results["scores"]["style"] + results["scores"]["tests"]
        if cur_tot > 13.5:
            results["scores"]["architecture"] = max(0.0, round(13.5 - results["scores"]["syntax"] - results["scores"]["style"] - results["scores"]["tests"], 1))
    elif ai_prob >= 30:
        results["scores"]["tests"] = min(results["scores"]["tests"], 0.6)
        results["scores"]["architecture"] = max(0.0, round(results["scores"]["architecture"] - 0.5, 1))
        cur_tot = results["scores"]["syntax"] + results["scores"]["architecture"] + results["scores"]["style"] + results["scores"]["tests"]
        if cur_tot > 16.0:
            results["scores"]["architecture"] = max(0.0, round(16.0 - results["scores"]["syntax"] - results["scores"]["style"] - results["scores"]["tests"], 1))

    results["scores"]["total"] = round(
        results["scores"]["syntax"] + results["scores"]["architecture"] + results["scores"]["style"] + results["scores"]["tests"],
        1
    )
    return results

def main():
    if len(sys.argv) < 2:
        print("Usage: audit_student.py <student_folder>")
        sys.exit(1)
    import json
    res = audit_student(sys.argv[1])
    print(json.dumps(res, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()
