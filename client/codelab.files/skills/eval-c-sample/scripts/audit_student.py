#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
audit_student.py - Moteur d'audit automatique d'un dossier C (Exemple Générique).
Vérifie la syntaxe, la compilation, la modularité et l'authenticité sans dépendre d'un cursus particulier.
Dernière modification : 14/10/2026
"""

import os
import sys
import re
import json
import subprocess
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


def strip_c_comments(content):
    """
    Supprime les commentaires C (/* ... */ et // ...) tout en préservant les numéros
    de lignes (sauts de ligne conservés) et en neutralisant les littéraux de chaînes.
    """
    pattern = re.compile(
        r'("(?:\\.|[^"\\])*")|(\'(?:\\.|[^\'\\])*\')|(/\*[\s\S]*?\*/)|(//[^\n]*)',
        re.MULTILINE
    )
    def _repl(match):
        if match.group(1) is not None:
            return '""'
        if match.group(2) is not None:
            return "''"
        comment = match.group(3) or match.group(4) or ''
        return '\n' * comment.count('\n')
    return pattern.sub(_repl, content)


def run_clang_check(c_files):
    """Vérifie la syntaxe C des fichiers via Clang ou GCC (tolérance void main())."""
    errors = {}
    if not c_files:
        return errors

    compiler = None
    for cand in ["clang", "gcc"]:
        try:
            res = subprocess.run([cand, "--version"], capture_output=True, text=True)
            if res.returncode == 0:
                compiler = cand
                break
        except Exception:
            continue

    if not compiler:
        for f in c_files:
            try:
                code = strip_c_comments(f.read_text(encoding='utf-8', errors='ignore'))
                if code.count('{') != code.count('}'):
                    errors[f.name] = "Accolades ouvrantes et fermantes déséquilibrées"
            except Exception as e:
                errors[f.name] = str(e)
        return errors

    inc_dir = SKILL_DIR / "includes"
    for f in c_files:
        if f.suffix.lower() == '.h':
            continue
        cmd = [
            compiler, "-fsyntax-only",
            "-Wno-main-return-type", "-Wno-main",
            "-Wno-implicit-function-declaration",
            "-Wno-int-conversion", "-Wno-incompatible-pointer-types",
            "-I", str(f.parent),
            "-I", str(f.parent.parent),
            "-I", str(f.parent.parent.parent),
        ]
        if inc_dir.is_dir():
            cmd.extend(["-I", str(inc_dir)])
        cmd.append(str(f))
        try:
            res = subprocess.run(cmd, capture_output=True, text=True, timeout=8)
            if res.returncode != 0:
                clean_err = []
                all_nested = True
                for line in res.stderr.splitlines():
                    if "error:" in line:
                        clean_err.append(line.split("error:")[-1].strip())
                        if "function definition is not allowed here" not in line:
                            all_nested = False
                # Tolérer les fonctions imbriquées (extension GNU C supportée dans CodeLab)
                if all_nested and clean_err and any("function definition is not allowed here" in l for l in clean_err):
                    continue
                first_err = clean_err[0] if clean_err else "Erreur de syntaxe de compilation"
                # Si l'erreur est uniquement un header externe introuvable, vérifier la syntaxe C pure sans les #include locaux manquants
                if "file not found" in first_err:
                    try:
                        raw_code = f.read_text(encoding='utf-8', errors='ignore')
                        stripped_includes = re.sub(r'^\s*#\s*include\s+["<][^">]+[">]', '// include neutralisé', raw_code, flags=re.MULTILINE)
                        fallback_cmd = [
                            compiler, "-fsyntax-only",
                            "-Wno-main-return-type", "-Wno-main",
                            "-Wno-implicit-function-declaration",
                            "-Wno-int-conversion", "-Wno-incompatible-pointer-types",
                            "-x", "c", "-"
                        ]
                        res2 = subprocess.run(fallback_cmd, input=stripped_includes, capture_output=True, text=True, timeout=8)
                        if res2.returncode == 0:
                            continue
                        clean_err2 = [l.split("error:")[-1].strip() for l in res2.stderr.splitlines() if "error:" in l]
                        if clean_err2:
                            first_err = clean_err2[0]
                    except Exception:
                        pass
                errors[f.name] = first_err
        except Exception as e:
            errors[f.name] = f"Échec analyse : {e}"

    return errors


def format_lines(lines, start_idx, end_idx, highlight_line=None, note=None):
    """Formate une portion de lignes avec numérotation 'ligne | code' et annotation optionnelle."""
    start = max(0, start_idx)
    end = min(len(lines), end_idx)
    res = []
    for i in range(start, end):
        l_num = i + 1
        line_str = lines[i].rstrip('\r\n')
        if l_num == highlight_line and note:
            res.append(f"{l_num:3d} | {line_str}  // <-- {note}")
        else:
            res.append(f"{l_num:3d} | {line_str}")
    return '\n'.join(res)


def detect_ai_markers(c_files):
    """
    Analyse statique et stylométrique multi-dimensionnelle des signatures d'assistance IA en C :
    1. Syntaxe anachronique ou sur-ingénierie (pointeurs de fonctions, structures anonymes imbriquées).
    2. Stylométrie didactique Chatbot (commentaires verbeux de prompt, docstrings Doxygen artificielles, étapes numérotées).
    3. Chronométrie CodeLab : fichiers complexes créés en moins de 60s (copier-coller de prompt).
    Applique des plafonds par catégorie et requiert au moins 2 catégories distinctes pour le seuil Élevé.
    """
    markers = []
    category_weights = {}
    subtype_counts = {}
    CATEGORY_CAPS = {
        'syntaxe_avancee': 0.35,
        'stylometrie_chatbot': 0.35,
        'sur_documentation': 0.25,
        'langue_incoherente': 0.25,
        'variance_stylometrique': 0.35,
        'chronometrie_anormale': 0.35,
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

    for fpath in c_files:
        rel_path = fpath.name
        try:
            content = fpath.read_text(encoding='utf-8', errors='ignore')
        except Exception:
            continue

        code_nc = strip_c_comments(content)
        lines = content.splitlines()
        loc = len([l for l in lines if l.strip() and not l.strip().startswith('//')])

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

        # 1. Syntaxe avancée (vérifiée hors commentaires)
        if re.search(r'\(\s*\*\s*[a-zA-Z_]\w*\s*\)\s*\(', code_nc):
            add_marker(rel_path, 'syntaxe_avancee', 'fn_ptr', "Pointeurs de fonctions avancés (inhabituel pour un travail d'initiation)", 0.25)

        if re.search(r'struct\s*\{[^}]*\}\s*[a-zA-Z_]\w*\s*;', code_nc):
            add_marker(rel_path, 'syntaxe_avancee', 'anon_struct', "Structures anonymes imbriquées", 0.18)

        # 2. Stylométrie Chatbot
        chatbot_phrases = [
            r'Cette fonction permet de',
            r'Fonction principale qui',
            r'Point d\'entrée du programme',
            r'Étape \d+\s*:',
            r'Step \d+\s*:',
            r'Retourne \d+ en cas de succès',
            r'Returns 0 on success',
            r'Time complexity',
            r'Complexité temporelle'
        ]
        found_phrases = [pat for pat in chatbot_phrases if re.search(pat, content, re.IGNORECASE)]
        if len(found_phrases) >= 2:
            add_marker(rel_path, 'stylometrie_chatbot', 'chatbot_phrases', f"Phrasé explicatif typique de prompt LLM ({len(found_phrases)} occurrences)", 0.25)

        # Doxygen formel artificiel
        doxygen_tags = len(re.findall(r'@(?:param|return|brief|author)\b', content))
        if doxygen_tags >= 4:
            add_marker(rel_path, 'sur_documentation', 'doxygen', f"Documentation formelle Doxygen (@param, @return) sur-générée ({doxygen_tags} tags)", 0.20)

        # Commentaires en anglais dans du code étudiant francophone
        english_comments = len(re.findall(r'//\s*(?:Initialize|Calculate|Compute|Check if|Loop through|Display|Return)\b', content, re.IGNORECASE))
        if english_comments >= 2:
            add_marker(rel_path, 'langue_incoherente', 'english_comments', f"Commentaires techniques rédigés en anglais ({english_comments} détectés)", 0.20)

        # Aveu explicite de copier-coller ou d'aide externe dans les commentaires
        if re.search(r'//\s*(?:copier\s*coller|copi[eé]\s*coll[eé]|aide\s+(?:avec|sur)\s+(?:forum|site|internet))', content, re.I):
            add_marker(rel_path, 'stylometrie_chatbot', 'copy_paste_admission', "Aveu explicite dans les commentaires de copier-coller ou d'aide externe (forum/site)", 0.35)

        if re.search(r'(?://|/\*)\s*(?:ChatGPT|Copilot|Claude|OpenAI|DeepSeek|AI generated|Prompt)', content, re.I):
            add_marker(rel_path, 'mention_explicite', 'explicit_ai', "Mention textuelle explicite d'outil IA dans les commentaires", 1.0)

    # 3. Rupture d'indentation intra-copie Tab ➔ 4 Espaces
    for f_rel, ind in file_indent.items():
        if 'primitive' in f_rel.lower():
            continue
        other_tabs = total_tabs - ind['tabs']
        other_spaces = total_spaces - ind['spaces']
        if ind['spaces'] >= 25 and (ind['spaces'] > ind['tabs'] * 3 or ind['tabs'] <= 3):
            if other_tabs >= 30 and other_tabs > other_spaces * 2:
                add_marker(f_rel, 'variance_stylometrique', 'indentation_spaces_rupture',
                           f"Rupture d'indentation intra-copie : {ind['spaces']} lignes en 4 espaces contre {ind['tabs']} tabulation(s), en contraste avec le style natif CodeLab ({other_tabs} tabulations dans le reste de la copie)", 0.35)

    # 4. Chronométrie anormale (< 300s pour projets complexes, < 60s pour fichiers standards)
    if len(created_timestamps) >= 2:
        created_timestamps.sort(key=lambda x: x[0])
        for i in range(len(created_timestamps) - 1):
            t1, f1, loc1 = created_timestamps[i]
            t2, f2, loc2 = created_timestamps[i+1]
            diff_sec = (t2 - t1).total_seconds()
            if 'primitive' in f2.lower():
                continue
            if 0 < diff_sec <= 300 and loc2 >= 60:
                add_marker(f2, 'chronometrie_anormale', 'fast_complex_creation', f"Programme complexe ({loc2} lignes) produit en seulement {int(diff_sec)}s (< 5 min) après {f1} (copier-coller externe / IA probable)", 0.35)
            elif 0 < diff_sec <= 60 and loc2 >= 25 and loc1 >= 15:
                add_marker(f2, 'chronometrie_anormale', 'fast_creation', f"Programme volumineux ({loc2} lignes) produit en seulement {int(diff_sec)}s après {f1} (copier-coller externe probable)", 0.30)

    total_weight = sum(category_weights.values())
    active_categories = [cat for cat, w in category_weights.items() if w >= 0.15]
    if len(active_categories) < 2 and 'mention_explicite' not in category_weights:
        total_weight = min(total_weight, 0.45)

    score_pct = min(100, int(round(total_weight * 100)))
    ai_prob = float(score_pct)
    if score_pct >= 60:
        level = f"Élevé ({score_pct}% - Forte présomption d'assistance IA)"
        verdict = f"⚠️ Usage IA très probable ({score_pct}% de marqueurs)"
        confidence = "Élevée"
    elif score_pct >= 25:
        level = f"Modéré ({score_pct}% - Idiomes atypiques détectés)"
        verdict = f"⚠️ Signaux d'assistance IA modérés ({score_pct}% de marqueurs)"
        confidence = "Moyenne"
    else:
        level = f"Faible ({score_pct}% - Code étudiant authentique)"
        verdict = "✅ Code authentique d'apprenant" if score_pct == 0 else f"ℹ️ Faible présence de marqueurs ({score_pct}%)"
        confidence = "Très élevée" if score_pct == 0 else "Faible"

    descriptions = [f"{m['file']}: {m['description']}" for m in markers[:8]]
    return {
        "level": level,
        "score": round(total_weight, 2),
        "probability_pct": score_pct,
        "count": len(markers),
        "details": "; ".join(descriptions) if descriptions else "Aucune signature d'IA détectée",
        "ai_probability": ai_prob,
        "confidence": confidence,
        "verdict": verdict,
        "markers_count": len(markers),
        "markers": markers
    }


def audit_student(folder_path, sessions_xml_path=None):
    """Point d'entrée standard de l'audit unitaire pour eval-c-sample."""
    p = Path(folder_path).resolve()
    student_id = p.name

    def _is_ign(f):
        try:
            rel_parts = f.relative_to(p).parts
            if any(part.startswith('.') or part in ('evaluations', '__pycache__', '.hidden', 'codelab', 'recovery') for part in rel_parts):
                return True
        except Exception:
            pass
        low = f.name.lower()
        return low.startswith('.') or '__' in low or low.endswith('.bak') or low.endswith('_bak.c') or 'copy' in low

    c_files = sorted([f for f in list(p.glob('**/*.c')) + list(p.glob('**/*.h')) if not _is_ign(f)])

    weights = load_weights()
    axes_w = weights.get("axes", {})
    syntax_max = float(axes_w.get("axe1_compilation_max", 6.0))
    arch_max = float(axes_w.get("axe2_conception_max", 8.0))
    style_max = float(axes_w.get("axe3_style_max", 4.0))
    tests_max = float(axes_w.get("axe4_validation_max", 2.0))

    results = {
        "student_id": student_id,
        "student_name": student_id,
        "assignment": p.name,
        "scores": {
            "syntax": 0.0,
            "architecture": 0.0,
            "style": 0.0,
            "tests": 0.0,
            "total": 0.0
        },
        "c_files": [f.name for f in c_files],
        "total_files": len(c_files),
        "valid_c_count": 0,
        "functions": [],
        "comments_count": 0,
        "asserts_count": 0,
        "syntax_errors": {},
        "snippets": {},
        "ai_detection": {},
        "ai_analysis": {}
    }

    if not c_files:
        empty_ai = {
            "level": "Faible (0% - Aucun fichier C)",
            "score": 0.0,
            "probability_pct": 0,
            "count": 0,
            "details": "Aucun fichier C trouvé",
            "ai_probability": 0.0,
            "confidence": "Très élevée",
            "verdict": "Aucun fichier C trouvé",
            "markers": []
        }
        results["ai_detection"] = empty_ai
        results["ai_analysis"] = empty_ai
        return results

    # 1. Vérification compilation & syntaxe
    syntax_errs = run_clang_check(c_files)
    results["syntax_errors"] = syntax_errs
    results["valid_c_count"] = len(c_files) - len(syntax_errs)

    # 2. Analyse des fonctions et de la modularité (hors commentaires)
    all_functions = []
    total_comments = 0
    total_asserts = 0
    total_code_lines = 0
    total_indented_lines = 0
    has_goto = False
    snippets = {"highlights": [], "compilation_errors": [], "anti_patterns": []}

    func_pattern = re.compile(
        r'\b((?:unsigned\s+|signed\s+|const\s+|static\s+|struct\s+)*(?:void|int|float|double|char|bool|long|short|size_t|[A-Za-z_]\w*)\s*\**)\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(([^)]*)\)\s*\{',
        re.MULTILINE | re.DOTALL
    )

    for f in c_files:
        try:
            content = f.read_text(encoding='utf-8', errors='ignore')
        except Exception:
            continue

        code_nc = strip_c_comments(content)
        lines = content.splitlines()
        nc_lines = [l for l in code_nc.splitlines() if l.strip()]
        total_code_lines += len(nc_lines)
        total_indented_lines += sum(1 for l in nc_lines if l.startswith((' ', '\t')))
        if re.search(r'\bgoto\s+[a-zA-Z_]\w*', code_nc):
            has_goto = True

        total_comments += len(re.findall(r'//.*', content))
        total_comments += len(re.findall(r'/\*.*?\*/', content, flags=re.DOTALL))
        total_asserts += len(re.findall(r'\bassert\s*\(', code_nc))

        for m in func_pattern.finditer(code_nc):
            ret_type, fname, args = m.groups()
            if fname not in ('main', 'if', 'while', 'for', 'switch', 'return', 'sizeof'):
                all_functions.append({
                    "file": f.name,
                    "name": fname,
                    "return_type": ret_type.strip(),
                    "args": args.strip()
                })
                start_pos = m.start()
                line_idx = code_nc[:start_pos].count('\n')
                snippet_txt = format_lines(lines, line_idx, line_idx + 8)
                if len(snippets["highlights"]) < 2:
                    snippets["highlights"].append({
                        "file": f.name,
                        "function": fname,
                        "code": snippet_txt
                    })

        if f.name in syntax_errs:
            err_msg = syntax_errs[f.name]
            snippets["compilation_errors"].append({
                "file": f.name,
                "error": err_msg,
                "code": format_lines(lines, 0, min(12, len(lines)))
            })

    results["snippets"] = snippets
    results["functions"] = [fn["name"] for fn in all_functions]
    results["comments_count"] = total_comments
    results["asserts_count"] = total_asserts

    # Calcul des notes exigeantes selon weights.json (par défaut 6 + 8 + 4 + 2 = 20 pts)
    num_errors = len(c_files) - results["valid_c_count"]
    ratio_valid = results["valid_c_count"] / float(len(c_files)) if c_files else 0.0
    results["scores"]["syntax"] = max(0.0, round(ratio_valid * syntax_max - 0.6 * num_errors, 1))

    unique_funcs = set(fn["name"] for fn in all_functions)
    param_funcs = sum(1 for fn in all_functions if fn["args"] and fn["args"] != "void")
    u_count = len(unique_funcs)
    if u_count >= 6 and param_funcs >= 2:
        arch_ratio = 1.0
    elif u_count >= 4:
        arch_ratio = 0.75 if param_funcs >= 1 else 0.6
    elif u_count >= 2:
        arch_ratio = 0.5
    elif u_count == 1:
        arch_ratio = 0.3
    else:
        arch_ratio = 0.15 if total_code_lines >= 10 else 0.0
    if num_errors > 0 and c_files:
        arch_ratio *= max(0.5, ratio_valid)
    results["scores"]["architecture"] = round(arch_ratio * arch_max, 1)

    style_pts = 0.0
    min_expected_comments = max(3, len(c_files))
    if total_comments >= min_expected_comments:
        style_pts += 1.5
    elif total_comments >= max(1, min_expected_comments // 2):
        style_pts += 0.7
    # Indentation effective
    indent_ratio = total_indented_lines / float(max(1, total_code_lines))
    if indent_ratio >= 0.45:
        style_pts += 1.5
    elif indent_ratio >= 0.25:
        style_pts += 0.8
    # Hygiène structurelle (absence de goto et zéro erreur de compilation)
    if not has_goto and total_code_lines > 0 and num_errors == 0:
        style_pts += 1.0
    elif not has_goto and total_code_lines > 0:
        style_pts += max(0.0, 1.0 - 0.4 * num_errors)
    results["scores"]["style"] = min(style_max, round((style_pts / 4.0) * style_max, 1))

    test_pts = 0.0
    has_test_fn = any(fn.lower().startswith('test') for fn in unique_funcs)
    has_test_file = any('test' in f.name.lower() and f.name not in ('test_primitives.c', 'test_primitive.c') for f in c_files)
    if total_asserts >= 3 or (has_test_file and total_asserts >= 1):
        test_pts = 2.0
    elif total_asserts == 2 or has_test_file or has_test_fn:
        test_pts = 1.2
    elif total_asserts == 1:
        test_pts = 0.6
    elif any('test_primitive' in f.name.lower() for f in c_files):
        test_pts = 0.4
    results["scores"]["tests"] = min(tests_max, round((test_pts / 2.0) * tests_max, 1))

    ai_info = detect_ai_markers(c_files)
    results["ai_detection"] = ai_info
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

analyze_student = audit_student

def main():
    if len(sys.argv) < 2:
        print("Usage: audit_student.py <student_folder>")
        sys.exit(1)
    res = audit_student(sys.argv[1])
    print(json.dumps(res, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()
