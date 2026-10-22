#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
eval_codelab.py - Moteur unifié d'évaluation pédagogique pour CodeLab.
Supporte :
  1. Évaluation d'un dossier unique d'étudiant (via explorateur ou ligne de commande)
  2. Évaluation d'un étudiant au sein d'une session CodeLab
  3. Évaluation globale d'une session complète (cohorte, CSV Moodle/Apogée, synthèse)
Intègre la détection automatique du langage (C / CodeLab ou Python/Numpy),
la lecture des clés dans userdata/ai.properties et la génération par LLM (Gemini) avec fallback déterministe.
"""

import os
import sys
import argparse
import json
import re
import urllib.request
import urllib.error
import hashlib
import threading
import time
from datetime import datetime
from pathlib import Path

# Répertoire racine du présent script (dans codelab.files/skills)
SCRIPT_DIR = Path(__file__).resolve().parent
USER_SKILLS_DIR = Path.home() / "codelab.files" / "skills"
HAL_SKILLS_DIR = Path.home() / "HAL" / "skills"
DESKTOP_SKILLS_DIR = Path.home() / "Desktop" / "skills"

def get_skill_dir(name):
    """Localise le dossier d'un skill dans le projet ou chez l'utilisateur."""
    if not name:
        return None
    for base in [USER_SKILLS_DIR, HAL_SKILLS_DIR, DESKTOP_SKILLS_DIR, SCRIPT_DIR]:
        candidate = base / name
        if candidate.is_dir():
            return candidate.resolve()
    return None

def get_skill_metadata(skill_dir_or_name):
    """Extrait les métadonnées (nom, version, date de dernière modification) d'un skill."""
    p = None
    if isinstance(skill_dir_or_name, (str, Path)):
        cand = Path(skill_dir_or_name)
        if cand.is_dir():
            p = cand.resolve()
        else:
            sdir = get_skill_dir(str(skill_dir_or_name))
            if sdir:
                p = sdir.resolve()

    meta = {
        "name": p.name if p else (str(skill_dir_or_name) if skill_dir_or_name else "skill-inconnu"),
        "version": "1.1.0",
        "last_modified": "10/10/2026"
    }

    if not p or not p.is_dir():
        return meta

    meta["name"] = p.name
    skill_md = p / "SKILL.md"
    if skill_md.is_file():
        try:
            content = skill_md.read_text(encoding="utf-8", errors="ignore")
            m_ver = re.search(r"^version:\s*([^\r\n]+)", content, re.M | re.I)
            if m_ver:
                meta["version"] = m_ver.group(1).strip().strip("'\"")

            m_date = re.search(r"^last_modified:\s*([^\r\n]+)", content, re.M | re.I)
            if m_date:
                raw = m_date.group(1).strip().strip("'\"")
                m_iso = re.match(r"^(\d{4})-(\d{2})-(\d{2})$", raw)
                if m_iso:
                    meta["last_modified"] = f"{m_iso.group(3)}/{m_iso.group(2)}/{m_iso.group(1)}"
                else:
                    meta["last_modified"] = raw
            else:
                m_vis = re.search(r"Dernière modification\s*:\s*\*?\*?([^\r\n*|]+)", content, re.I)
                if m_vis:
                    meta["last_modified"] = m_vis.group(1).strip()
                else:
                    mtime = skill_md.stat().st_mtime
                    meta["last_modified"] = datetime.fromtimestamp(mtime).strftime("%d/%m/%Y")
        except Exception:
            pass

    return meta

INTROPROG_DIR = get_skill_dir("eval-introprog") or (SCRIPT_DIR / "eval-introprog")
INTROYTHON_DIR = get_skill_dir("eval-intropython") or (SCRIPT_DIR / "eval-intropython")

def resolve_skill(skill_name=None, target_dir=None):
    """Résout le dossier de skill demandé ou auto-détecté."""
    if skill_name and skill_name.lower() not in ("auto", "none", ""):
        sdir = get_skill_dir(skill_name)
        if sdir:
            return sdir
        print(f"[WARN] Skill '{skill_name}' introuvable, tentative d'auto-détection...", file=sys.stderr)

    lang = detect_language(target_dir) if target_dir else "c"
    if lang == "c":
        candidates = ["eval-introprog", "eval-c-sample"]
    elif lang == "python":
        candidates = ["eval-intropython", "eval-python-sample"]
    elif lang == "java":
        candidates = ["eval-java-sample"]
    else:
        candidates = ["eval-c-sample", "eval-python-sample", "eval-java-sample", "eval-introprog", "eval-intropython"]

    for name in candidates:
        sdir = get_skill_dir(name)
        if sdir:
            return sdir
    return None

def load_sessions_users(sessions_xml_path=None, search_dirs=None):
    """Charge la base des utilisateurs {login: {'name': ..., 'mail': ..., 'groups': ...}} depuis sessions.xml."""
    candidates = []
    if sessions_xml_path and Path(sessions_xml_path).is_file():
        candidates.append(Path(sessions_xml_path))
    if search_dirs:
        for d in search_dirs:
            if d:
                candidates.append(Path(d) / "sessions.xml")

    import xml.etree.ElementTree as ET
    users = {}
    for c in candidates:
        if c.is_file():
            try:
                tree = ET.parse(c)
                for u in tree.findall(".//user"):
                    login = u.get("login")
                    name = u.get("name")
                    mail = u.get("mail", "")
                    groups = u.get("groups", "")
                    role = u.get("role", "")
                    if login:
                        users[login] = {"name": name or login, "mail": mail, "groups": groups, "role": role}
                if users:
                    return users
            except Exception:
                pass
    return users

def is_tutor_user(login, user_info=None):
    """Détermine si un compte correspond à un tuteur ou enseignant à exclure des évaluations étudiantes."""
    if not login:
        return False
    l = str(login).strip().lower()
    # 1. Logins connus d'enseignants ou tuteurs
    if l in ("lehuen", "despres", "tuteur", "tutor", "admin", "enseignant", "prof"):
        return True
    if user_info:
        role = (user_info.get("role") or "").lower()
        if role in ("tutor", "admin", "tuteur"):
            return True
        groups = (user_info.get("groups") or "").lower()
        if groups in ("tutor", "admin", "tuteurs", "enseignants"):
            return True
        mail = (user_info.get("mail") or "").lower()
        # Si groupe vide et courriel non-étudiant ou absent, et login non-numérique
        if not groups and (mail in ("--", "") or "etu" not in mail) and not l.isdigit():
            return True
    return False

def extract_author_from_files(folder_path):
    """Fallback universel : extrait le nom de l'étudiant depuis les en-têtes CodeLab ('Created by ...')."""
    p = Path(folder_path)
    if not p.is_dir():
        return None
    patterns = [
        r"Created by\s+([^0-9\r\n\t]+?)\s+on\s+[0-9]{2}/[0-9]{2}/[0-9]{2}",
        r"(?:Auteur|Author)\s*[:=]\s*([^\r\n0-9]+)",
    ]
    files = []
    for ext in ("*.c", "*.h", "*.cpp", "*.py", "*.java", "*.ipynb"):
        files.extend(p.glob(f"**/{ext}"))

    for f in files:
        try:
            content = f.read_text(encoding="utf-8", errors="ignore")[:3000]
            for pat in patterns:
                m = re.search(pat, content)
                if m:
                    cand = m.group(1).strip()
                    if cand and len(cand) >= 3 and cand.lower() not in ("john doe", "none", "inconnu", "anonymous", "_author_"):
                        return cand
        except Exception:
            pass
    return None

def resolve_student_identity(target_dir, student_login=None, student_name=None, sessions_xml_path=None):
    """
    Résout de façon déterministe et exhaustive l'identité de l'étudiant :
      1. Nom en clair (student_name)
      2. Identifiant / Login (student_id)
      3. Dossier / Travail (assignment)
    """
    target = Path(target_dir).resolve()

    # 1. Déduction de student_id et assignment
    student_id = student_login
    assignment = target.name

    if not student_id:
        curr = target
        while curr.parent and curr.parent.name and curr.parent.name != curr.root:
            if re.match(r"^(tp\d*|td\d*|projet.*|partie.*|src|exercices?)$", curr.name, re.I):
                curr = curr.parent
            else:
                break
        student_id = curr.name
        if curr != target:
            assignment = f"{curr.name}/{target.name}"

    # 2. Déduction du nom en clair
    resolved_name = student_name
    if not resolved_name or resolved_name.lower() in ("unknown", "inconnu", "none", ""):
        # A. Vérification dans sessions.xml
        users_db = load_sessions_users(sessions_xml_path, search_dirs=[target, target.parent, target.parent.parent])
        if student_id in users_db and users_db[student_id].get("name"):
            resolved_name = users_db[student_id]["name"]

        # B. Extraction depuis les en-têtes des fichiers sources
        if not resolved_name:
            resolved_name = extract_author_from_files(target)
            if not resolved_name and target.parent and target.parent.name == student_id:
                resolved_name = extract_author_from_files(target.parent)

    if not resolved_name:
        resolved_name = student_id

    return {
        "student_id": student_id,
        "student_name": resolved_name,
        "assignment": assignment
    }

def normalize_markdown_newlines(text: str) -> str:
    """Garantit des lignes physiques nettes entre les éléments Markdown, dans les tableaux et protège les blocs de code."""
    if not text:
        return text

    # 0. Remplacer les séquences littérales '\n' accidentellement écrites telles quelles par le modèle
    text = re.sub(r'\\n\\n+', '\n\n', text)
    text = re.sub(r'\\n', '\n', text)

    # 1. Isoler et assainir tous les blocs de code clôturés (``` ou ~~~) AVANT toute manipulation du texte Markdown.
    # Ceci évite que les barres verticales (|), les dièses (#) ou les tirets (---) contenus dans le code
    # ne soient interprétés à tort comme des tableaux, titres ou séparateurs horizontaux.
    code_blocks = []

    def _save_code_block(m):
        open_indent = m.group(1) or ""
        fence_chars = m.group(2)
        lang = m.group(3) or ""
        body = m.group(4)
        close_indent = m.group(5) or ""

        # Compacter les lignes vides excessives insérées entre numéros de lignes (ex: " 15 | ...\n\n 16 | ...")
        clean_body = re.sub(r'(\d+\s*\|[^\n]*)\n\n+(?=[ \t]*\d+\s*\|)', r'\1\n', body)

        # Supprimer les lignes vides au début et à la fin du corps tout en préservant l'indentation du code
        body_lines = clean_body.splitlines()
        while body_lines and not body_lines[0].strip():
            body_lines.pop(0)
        while body_lines and not body_lines[-1].strip():
            body_lines.pop()
        clean_body = "\n".join(body_lines)

        # Règle cruciale d'indentation CommonMark / Flexmark :
        # Si la balise ouvrante est indentée (ex: item de liste ordonnée '1. ...'),
        # la balise fermante DOIT impérativement conserver la même indentation.
        # Une balise fermante à colonne 0 romprait la liste et ferait avaler le texte suivant comme bloc de code.
        final_close_indent = open_indent if open_indent else close_indent

        rebuilt = f"{open_indent}{fence_chars}{lang}\n{clean_body}\n{final_close_indent}{fence_chars}"
        idx = len(code_blocks)
        code_blocks.append(rebuilt)
        return f"@@@CODELAB_CODE_BLOCK_{idx}@@@"

    code_fence_pattern = re.compile(
        r'(?m)^([ \t]*)(`{3,}|~{3,})([a-zA-Z0-9_-]*)[ \t]*\r?\n([\s\S]*?)\r?\n([ \t]*)\2[ \t]*$'
    )
    text = code_fence_pattern.sub(_save_code_block, text)

    # 2. Insérer une ligne vide avant et après chaque bloc de code s'il est collé à un texte
    text = re.sub(r'([^\n])\n([ \t]*@@@CODELAB_CODE_BLOCK_\d+@@@)', r'\1\n\n\2', text)
    text = re.sub(r'(@@@CODELAB_CODE_BLOCK_\d+@@@)\n([^\n])', r'\1\n\n\2', text)

    # 3. Remplacer les délimitations de lignes de tableau collées (| | ou |   |) par un vrai retour à la ligne
    text = re.sub(r'\|\s*\|', '|\n|', text)

    # 4. Insérer une ligne vide avant les titres (#, ##, ###) s'ils sont collés à la ligne précédente
    text = re.sub(r'([^\n])\n(#{1,6}\s+)', r'\1\n\n\2', text)

    # 5. Insérer une ligne vide après les titres (#, ##, ###) s'ils sont collés à la ligne suivante
    text = re.sub(r'(#{1,6}\s+[^\n]+)\n([^\n#])', r'\1\n\n\2', text)

    # 6. Insérer une ligne vide avant les items de liste numérotée (1., 2.) s'ils sont collés au texte précédent
    text = re.sub(r'([^\n])\n([0-9]+\.\s+)', r'\1\n\n\2', text)

    # 6b. Insérer une ligne vide et une indentation après les titres d'items numérotés (ex: 1. **Titre :**)
    # afin que le premier paragraphe explicatif soit bien un paragraphe distinct imbriqué dans l'item.
    # Cas A : sur la même ligne (ex: "1. **Titre :** Texte...")
    text = re.sub(r'(?m)^([ \t]*[0-9]+\.\s+\*\*[^*\n]+?\*\*:?)[ \t]+([A-Za-zÀ-ÿ0-9"«\'(\[].+)$', r'\1\n\n   \2', text)
    # Cas B : sur la ligne suivante sans ligne vide (ex: "1. **Titre :**\nTexte...")
    text = re.sub(r'(?m)^([ \t]*[0-9]+\.\s+\*\*[^*\n]+?\*\*:?)[ \t]*\r?\n[ \t]*([^\n\r#\s`].*)$', r'\1\n\n   \2', text)
    # Cas C : avec ligne vide mais sans indentation
    text = re.sub(r'(?m)^([ \t]*[0-9]+\.\s+\*\*[^*\n]+?\*\*:?)[ \t]*\r?\n[ \t]*\r?\n([ \t]{0,2})([^\n\r#\s`].*)$', r'\1\n\n   \3', text)

    # 7. Insérer une ligne vide avant et après les séparateurs horizontaux (---)
    text = re.sub(r'([^\n])\n(---)', r'\1\n\n\2', text)
    text = re.sub(r'(---)\n([^\n])', r'\1\n\n\2', text)

    # 8. Tables Markdown : Insérer une ligne vide avant et après le tableau
    text = re.sub(r'([^\n])\n([ \t]*\|[^\n]+\|[ \t]*\n[ \t]*\|[\s\-:]+\|)', r'\1\n\n\2', text)
    text = re.sub(r'(^[ \t]*\|[^\n]+\|[ \t]*)\n([^\n|])', r'\1\n\n\2', text, flags=re.MULTILINE)

    # 9. Restaurer les blocs de code préservés
    for idx, block in enumerate(code_blocks):
        text = text.replace(f"@@@CODELAB_CODE_BLOCK_{idx}@@@", block)

    return text

def load_ai_properties(custom_path=None):
    """Charge les propriétés IA depuis user.properties (avec rétro-compatibilité ai.properties)."""
    candidates = [
        SCRIPT_DIR.parent / "userdata" / "user.properties",
        Path.home() / "codelab.files" / "userdata" / "user.properties",
        # Rétrocompatibilité si un ancien ai.properties existe encore
        SCRIPT_DIR.parent / "userdata" / "ai.properties",
        Path.home() / "codelab.files" / "userdata" / "ai.properties"
    ]
    if custom_path:
        candidates.append(Path(custom_path))
    
    props = {
        "GEMINI_API_KEY": os.environ.get("GEMINI_API_KEY", ""),
        "GEMINI_MODEL": os.environ.get("GEMINI_MODEL", ""),
        "PROXY_ENABLED": os.environ.get("PROXY_ENABLED", ""),
        "PROXY_HOST": os.environ.get("PROXY_HOST", ""),
        "PROXY_PORT": os.environ.get("PROXY_PORT", ""),
        "HTTPS_PROXY": os.environ.get("HTTPS_PROXY", os.environ.get("https_proxy", "")),
        "HTTP_PROXY": os.environ.get("HTTP_PROXY", os.environ.get("http_proxy", "")),
        "AI_CACHE": os.environ.get("AI_CACHE", ""),
        "AI_CACHE_DIR": os.environ.get("AI_CACHE_DIR", ""),
    }
    
    found_file = None
    for c in candidates:
        if c.is_file():
            if not found_file:
                found_file = c
            try:
                with open(c, "r", encoding="utf-8") as f:
                    for line in f:
                        line = line.strip()
                        if not line or line.startswith("#"):
                            continue
                        if "=" in line:
                            k, v = line.split("=", 1)
                            k = k.strip()
                            v = v.strip()
                            if v:
                                if k == "PROXY_ENABLED":
                                    if not props.get("PROXY_ENABLED"):
                                        props[k] = v
                                elif k == "PROXY_HOST" and v.upper() in ("NO", "NO_PROXY", "NONE"):
                                    props[k] = ""
                                elif not props.get(k):  # Conserver la première valeur trouvée (priorité user.properties)
                                    props[k] = v
            except Exception as e:
                print(f"[WARN] Erreur lecture {c}: {e}", file=sys.stderr)

    # Valeurs par défaut si non spécifiées
    if not props.get("GEMINI_MODEL"):
        props["GEMINI_MODEL"] = "gemini-3.5-flash-lite"
    if not props.get("AI_CACHE"):
        props["AI_CACHE"] = "yes"
    props["AI_CACHE"] = props["AI_CACHE"].lower()

    # Migration automatique si modèle retiré par Google ou ancien défaut
    if props.get("GEMINI_MODEL") in ("gemini-2.5-flash", "gemini-2.5-pro", ""):
        props["GEMINI_MODEL"] = "gemini-3.5-flash-lite"

    # Déterminer le dossier de cache
    if not props.get("AI_CACHE_DIR"):
        if found_file:
            props["AI_CACHE_DIR"] = str(found_file.parent / "ai_cache")
        else:
            props["AI_CACHE_DIR"] = str(Path.home() / "codelab.files" / "userdata" / "ai_cache")

    return props

def setup_proxy(ai_props):
    """Configure le proxy réseau pour urllib si spécifié."""
    proxy_enabled = ai_props.get("PROXY_ENABLED", "").strip().lower() if ai_props else ""
    if proxy_enabled in ("no", "false", "0"):
        return None

    proxy_host = ai_props.get("PROXY_HOST", "").strip() if ai_props else ""
    if proxy_host.upper() in ("NO", "NO_PROXY", "NONE"):
        return None

    proxy_port = ai_props.get("PROXY_PORT", "").strip() if ai_props else ""
    https_proxy = ai_props.get("HTTPS_PROXY", "").strip() if ai_props else ""

    proxy_url = None
    if https_proxy:
        proxy_url = https_proxy
    elif proxy_host and proxy_host.upper() not in ("NO", "NO_PROXY", "NONE"):
        if proxy_port:
            proxy_url = f"http://{proxy_host}:{proxy_port}"
        else:
            proxy_url = f"http://{proxy_host}"
    elif os.environ.get("HTTPS_PROXY") or os.environ.get("https_proxy"):
        proxy_url = os.environ.get("HTTPS_PROXY") or os.environ.get("https_proxy")

    if proxy_url:
        try:
            proxy_handler = urllib.request.ProxyHandler({
                "http": proxy_url,
                "https": proxy_url,
            })
            opener = urllib.request.build_opener(proxy_handler)
            urllib.request.install_opener(opener)
            print(f"[INFO] Configuration du proxy réseau : {proxy_url}", flush=True)
            return proxy_url
        except Exception as e:
            print(f"[WARN] Erreur lors de la configuration du proxy : {e}", flush=True)
    return None

def get_cache_key(model, prompt):
    """Calcule une empreinte SHA-256 unique pour le modèle et le prompt."""
    h = hashlib.sha256()
    h.update(model.encode("utf-8"))
    h.update(b":::")
    h.update(prompt.encode("utf-8"))
    return h.hexdigest()

def detect_language(folder_path):
    """Détecte si le dossier concerne le cours C (L1) ou Python (L2)."""
    p = Path(folder_path)
    if not p.is_dir():
        return "c"
    
    c_count = len(list(p.rglob("*.c")))
    py_count = len(list(p.rglob("*.py")))
    
    # Présence de sous-dossiers typiques
    has_c_tps = any((p / f"TP{i}").is_dir() for i in range(1, 7))
    has_py_data = any(p.rglob("*.npy")) or any(p.rglob("openSSH*.log"))
    
    if py_count > c_count or has_py_data:
        return "python"
    return "c"

def call_gemini_api(api_key, model, prompt, ai_props=None):
    """Appelle l'API REST Google Gemini avec cache local, proxy et ticker en temps réel."""
    if not api_key:
        print("[INFO] Aucune clé API Gemini configurée. Bascule sur le moteur déterministe.", flush=True)
        return None

    # 1. Vérification du cache local
    use_cache = True
    cache_file = None
    cache_dir = None
    if ai_props:
        use_cache = str(ai_props.get("AI_CACHE", "true")).lower() in ("true", "1", "yes", "oui")
        cache_dir_str = ai_props.get("AI_CACHE_DIR")
        if cache_dir_str and use_cache:
            cache_dir = Path(cache_dir_str)
            cache_key = get_cache_key(model, prompt)
            cache_file = cache_dir / f"{cache_key}.md"
            if cache_file.is_file():
                try:
                    with open(cache_file, "r", encoding="utf-8") as f:
                        cached_text = f.read()
                    if cached_text.strip():
                        print("[DONE] Fiche pédagogique chargée instantanément depuis le cache local (ai_cache).", flush=True)
                        return normalize_markdown_newlines(cached_text)
                except Exception as e:
                    print(f"[WARN] Impossible de lire le cache ({e}), contact de l'API...", flush=True)

    # 2. Configuration du proxy réseau
    proxy_url = setup_proxy(ai_props)

    # 3. Chaîne de modèles (modèle configuré + modèle de secours haute disponibilité)
    models_to_try = [model]
    if model != "gemini-3.5-flash-lite":
        models_to_try.append("gemini-3.5-flash-lite")
    elif "gemini-3.5-flash" not in models_to_try:
        models_to_try.append("gemini-3.5-flash")

    payload = {
        "contents": [{
            "parts": [{"text": prompt}]
        }],
        "generationConfig": {
            "temperature": 0.3,
            "maxOutputTokens": 4096
        }
    }
    data = json.dumps(payload).encode("utf-8")

    for current_model in models_to_try:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{current_model}:generateContent?key={api_key}"
        req = urllib.request.Request(
            url,
            data=data,
            headers={"Content-Type": "application/json"}
        )

        # 2 tentatives par modèle pour surmonter les erreurs transitoires (503 / 429 / timeout)
        for attempt in range(1, 3):
            if current_model != model:
                print(f"[INFO] Bascule sur le modèle de secours {current_model}...", flush=True)
            elif attempt > 1:
                print(f"[INFO] Nouvelle tentative ({attempt}/2) avec {current_model}...", flush=True)
            else:
                if proxy_url:
                    print(f"[INFO] Contact de {current_model} sur Google AI Studio via proxy...", flush=True)
                else:
                    print(f"[INFO] Contact du modèle IA {current_model} sur Google AI Studio...", flush=True)

            result = []
            error = []

            def _worker():
                try:
                    with urllib.request.urlopen(req, timeout=45) as resp:
                        res_json = json.loads(resp.read().decode("utf-8"))
                        candidates = res_json.get("candidates", [])
                        if candidates:
                            parts = candidates[0].get("content", {}).get("parts", [])
                            if parts:
                                result.append(parts[0].get("text", ""))
                except Exception as e:
                    error.append(e)

            worker_thread = threading.Thread(target=_worker, daemon=True)
            start_time = time.time()
            worker_thread.start()

            # Heartbeat toutes les 2 secondes pour informer le terminal et le dialogue Swing
            while worker_thread.is_alive():
                worker_thread.join(timeout=2.0)
                if worker_thread.is_alive():
                    elapsed = int(time.time() - start_time)
                    print(f"[INFO] Rédaction IA en cours... ({elapsed}s)", flush=True)

            if result and result[0]:
                total_time = int(time.time() - start_time)
                print(f"[DONE] Fiche rédigée par {current_model} en {total_time}s.", flush=True)
                text = normalize_markdown_newlines(result[0])
                # Sauvegarde en cache si activé
                if use_cache and cache_file and cache_dir:
                    try:
                        cache_dir.mkdir(parents=True, exist_ok=True)
                        with open(cache_file, "w", encoding="utf-8") as f:
                            f.write(text)
                    except Exception as e:
                        print(f"[WARN] Impossible d'écrire dans le cache: {e}", flush=True)
                return text

            if error:
                err_str = str(error[0])
                is_transient = "503" in err_str or "429" in err_str or "timed out" in err_str
                if is_transient and attempt == 1:
                    print(f"[WARN] {current_model} temporairement indisponible ({err_str}), nouvelle tentative dans 1.5s...", flush=True)
                    time.sleep(1.5)
                    continue
                else:
                    print(f"[WARN] Appel API avec {current_model} échoué ({err_str}).", flush=True)
                    break  # Passer au modèle de secours

    print("[WARN] Tous les modèles IA ont échoué ou sont temporairement saturés (503).", flush=True)
    print("[INFO] Bascule automatique sur la génération déterministe.", flush=True)
    return None

def load_activities_context(skill_dir, active_tps=None, max_chars_per_subject=6000, max_corriges_per_tp=7):
    """
    Charge les sujets Markdown et les corrigés C de référence depuis le sous-dossier 'activities/' du skill.
    Permet au LLM de comparer le code de l'élève aux attentes précises et aux implémentations types.
    """
    if not skill_dir:
        return {}
    act_dir = Path(skill_dir) / "activities"
    if not act_dir.is_dir():
        return {}

    context = {}
    for tp_folder in sorted(act_dir.glob("TP*")):
        tp_name = tp_folder.name
        if active_tps and tp_name not in active_tps:
            continue
        
        tp_info = {"sujet": "", "corriges": {}}
        
        # 1. Charger le sujet Markdown
        sujet_files = list((tp_folder / "sujet").glob("*.md")) if (tp_folder / "sujet").is_dir() else []
        if not sujet_files:
            sujet_files = list(tp_folder.glob("*.md"))
        if sujet_files:
            try:
                raw_sujet = sujet_files[0].read_text(encoding="utf-8", errors="ignore")
                clean_sujet = re.sub(r'<style>.*?</style>', '', raw_sujet, flags=re.DOTALL).strip()
                if len(clean_sujet) > max_chars_per_subject:
                    clean_sujet = clean_sujet[:max_chars_per_subject] + "\n... [Sujet tronqué pour concision]"
                tp_info["sujet"] = clean_sujet
            except Exception:
                pass

        # 2. Charger les corrigés de référence C (supporte 'corriges' ou 'corrigés' NFC/NFD)
        corriges_dirs = [d for d in tp_folder.iterdir() if d.is_dir() and 'corrig' in d.name.lower()]
        if corriges_dirs:
            c_dir = corriges_dirs[0]
            c_files = sorted(c_dir.glob("*.c"))[:max_corriges_per_tp]
            for cf in c_files:
                try:
                    c_code = cf.read_text(encoding="utf-8", errors="ignore")
                    tp_info["corriges"][cf.name] = c_code
                except Exception:
                    pass

        context[tp_name] = tp_info

    return context

def collect_student_c_code(student_dir, max_chars_per_file=4000):
    """
    Collecte les programmes C effectifs de l'étudiant classés par TP,
    en éliminant les fichiers vides ou squelettes inactifs.
    Retourne (dict {rel_path: code}, list [active_tps]).
    """
    student_p = Path(student_dir)
    student_code = {}
    active_tps = set()
    
    for f in sorted(student_p.rglob("*.c")):
        parts = f.relative_to(student_p).parts
        if any(p.startswith(".") or p in ("__pycache__", "codelab", "evaluations") for p in parts):
            continue
        try:
            content = f.read_text(encoding="utf-8", errors="ignore")
            clean = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)
            clean = re.sub(r'//.*', '', clean)
            if ';' not in clean:
                continue
            
            rel_str = str(f.relative_to(student_p))
            tp_match = re.search(r'(TP[1-9])', rel_str)
            if tp_match:
                active_tps.add(tp_match.group(1))
                
            if len(content) > max_chars_per_file:
                content = content[:max_chars_per_file] + "\n... [Code tronqué pour concision]"
            student_code[rel_str] = content
        except Exception:
            pass
            
    return student_code, sorted(list(active_tps))

def extract_eval_scores(llm_text):
    """Extrait le bloc JSON eval_scores produit par Gemini et retourne (scores_dict, clean_markdown)."""
    m = re.search(r'```eval_scores\s*([\s\S]*?)\s*```', llm_text)
    if not m:
        m = re.search(r'```(?:json)?\s*(\{[\s\S]*?"total_note"[\s\S]*?\})\s*```', llm_text)
    
    if m:
        json_str = m.group(1).strip()
        try:
            scores_data = json.loads(json_str)
            clean_text = llm_text[:m.start()] + llm_text[m.end():]
            clean_text = clean_text.strip()
            return scores_data, clean_text
        except Exception as e:
            print(f"[WARN] Erreur lors du parsing JSON de eval_scores : {e}", flush=True)
            
    return None, llm_text

def extract_integrity_indicators(res, plagiarism_info=None):
    """
    Extrait et structure les 3 piliers d'intégrité académique :
    1. Analyse Chronométrique (séances, étalement, bursts de création instantanée, inversions)
    2. Ruptures Stylométriques (tabulations CodeLab vs 4 espaces ChatGPT, void main vs int main, Doxygen)
    3. Test de Similarité inter-étudiants (Winnowing/Jaccard, détection de plagiat)
    """
    ai_data = res.get("ai_analysis", {})
    markers = ai_data.get("markers", [])
    dates_info = res.get("dates_info", {})
    ind_stats = ai_data.get("indentation_stats", {})
    
    # 1. Analyse Chronométrique
    chrono_markers = [m for m in markers if m.get("category") in ("chronometrie_instantanee", "chronometrie_anormale")]
    distinct_days = dates_info.get("distinct_days", 0)
    span_days = dates_info.get("span_days", 0)
    first_d = dates_info.get("first_date", "")
    last_d = dates_info.get("last_date", "")
    
    if distinct_days > 0:
        chrono_base = f"{distinct_days} séance(s) de TP tracée(s) sur {span_days} jour(s) (du {first_d} au {last_d})"
    else:
        chrono_base = "Aucun horodatage significatif tracé sur CodeLab"
        
    if chrono_markers:
        chrono_courte = f"⚠️ {len(chrono_markers)} anomalie(s) temporelle(s)"
        chrono_lines = [
            "> [!WARNING]",
            f"> **Anomalies chronométriques détectées ({len(chrono_markers)} événement(s))** :",
        ]
        for cm in chrono_markers:
            chrono_lines.append(f"> - `{cm.get('file', '')}` : {cm.get('description', '')}")
        chrono_lines.append(">\n> *Rappel : Un programme complexe produit en quelques secondes ou hors-séance trahit une injection ou un copier-coller de code externe.*")
        chrono_md = "\n".join(chrono_lines)
    else:
        chrono_courte = f"Régulière ({distinct_days} séances / {span_days}j)" if distinct_days > 0 else "Neutre (sans trace)"
        chrono_md = f"✅ **Rythme régulier et authentique** : {chrono_base}. Aucun pic de création instantané suspect (< 60s ou < 5 min)."

    # 2. Stylométrie & Analyse d'Indentation
    tot_tabs = ind_stats.get("total_tabs", 0)
    tot_spaces = ind_stats.get("total_spaces", 0)
    style_markers = [m for m in markers if m.get("category") in ("variance_stylometrique", "stylometrie_ia", "stylometrie_chatbot", "sur_documentation", "langue_incoherente")]
    indent_markers = [m for m in style_markers if m.get("subtype") == "indentation_spaces_rupture"]
    
    if indent_markers:
        style_courte = f"⚠️ Rupture 4 espaces ({tot_spaces} espaces vs {tot_tabs} tabs)"
    elif style_markers:
        style_courte = f"⚠️ Idiomes IA ({len(style_markers)} marqueur(s))"
    else:
        style_courte = f"Conforme CodeLab ({tot_tabs} tabs)" if tot_tabs > 0 else "Standard"

    if style_markers:
        style_lines = [
            "> [!WARNING]",
            f"> **Ruptures stylométriques & d'indentation détectées ({len(style_markers)} marqueur(s))** :"
        ]
        for sm in style_markers:
            style_lines.append(f"> - `{sm.get('file', '')}` : {sm.get('description', '')}")
        style_lines.append(">\n> *Rappel : L'éditeur CodeLab insère nativement des tabulations. L'apparition soudaine de blocs indentés en 4 espaces ou de commentaires Doxygen signale un recours à des assistants IA ou du code externe.*")
        style_md = "\n".join(style_lines)
    else:
        tab_str = f" ({tot_tabs} tabulations, 0 rupture en 4 espaces)" if tot_tabs > 0 else ""
        style_md = f"✅ **Style et indentation homogènes** : Utilisation exclusive des tabulations natives de l'éditeur CodeLab{tab_str}. Conventions de fonctions et de casse conformes aux exigences du module."

    # 3. Contrôle de Similarité Inter-Étudiants (Plagiat)
    plag = res.get("plagiarism_alert") or plagiarism_info
    if plag and plag.get("max_sim", 0) >= 75.0:
        p_name = plag.get("partner") or plag.get("partner_id") or "Camarade"
        p_id = plag.get("partner_id", "")
        max_s = plag.get("max_sim", 0)
        sev = plag.get("severity") or "⚠️ Suspicion de similarité"
        files = plag.get("files", [])
        files_str = ", ".join(f"`{f}` ({sim}%)" for f, sim in files) if files else "Exercices libres"
        
        similarite_courte = f"🚨 Partage avec {p_id or p_name} ({max_s}%)"
        sim_lines = [
            "> [!CAUTION]",
            f"> **{sev} de partage de code inter-étudiants** :",
            f"> - **Binôme ou tiers identifié** : **{p_name}**" + (f" (`{p_id}`)" if p_id else ""),
            f"> - **Taux maximal de similarité syntaxique (Jaccard)** : **{max_s}%**",
            f"> - **Fichiers suspects en doublon** : {files_str}",
            ">\n> *Sanction pédagogique : Note plafonnée conformément aux consignes d'intégrité académique. L'étudiant doit justifier personnellement de la réalisation de ces programmes.*"
        ]
        similarite_md = "\n".join(sim_lines)
    else:
        similarite_courte = "✅ Original (0% partage)"
        similarite_md = "✅ **Travail original authentique** : Analyse croisée de cohorte par n-grams syntaxiques (Jaccard) réalisée sans aucune similarité suspecte détectée sur les exercices à code libre."

    # Bloc combiné complet
    analyse_integrite_block = f"""### 🔬 Analyses d'Intégrité Pédagogique

#### ⏱️ Analyse Chronométrique & Dynamique de Travail
{chrono_md}

#### 📐 Stylométrie & Respect des Conventions d'Indentation
{style_md}

#### 👥 Contrôle de Similarité Inter-Étudiants (Détection de Plagiat)
{similarite_md}"""

    return {
        "chronometrie": chrono_md,
        "chronometrie_courte": chrono_courte,
        "stylometrie": style_md,
        "stylometrie_courte": style_courte,
        "similarite": similarite_md,
        "similarite_courte": similarite_courte,
        "analyse_integrite": analyse_integrite_block
    }

def render_template_if_exists(template_candidates, template_vars):
    """Cherche et applique un gabarit Markdown (report_template.md) s'il existe."""
    for cand in template_candidates:
        if cand and Path(cand).is_file():
            try:
                with open(cand, "r", encoding="utf-8") as f:
                    template_str = f.read()
                for key, val in template_vars.items():
                    template_str = template_str.replace(f"{{{key}}}", str(val))
                return template_str
            except Exception as e:
                print(f"[WARN] Erreur lors de la lecture du gabarit {cand}: {e}", flush=True)
    return None

def extract_pedagogical_sections(markdown_text):
    """Extrait les 4 sections pédagogiques standards depuis un texte Markdown brut."""
    sec_bilan = ""
    sec_points_forts = ""
    sec_ameliorations = ""
    sec_commentaire = ""
    
    if not markdown_text:
        return sec_bilan, sec_points_forts, sec_ameliorations, sec_commentaire

    m_bilan = re.search(r'###?\s*(?:Bilan|Synthèse|Progression|Résultats|Diagnostic)[\s\S]*?(?=###?\s*Points?\s*Forts?|$)', markdown_text, re.IGNORECASE)
    m_forts = re.search(r'###?\s*Points?\s*Forts?[\s\S]*?(?=###?\s*(?:Axes|Conseils|Pistes|Amélioration|Remédiation|Anti-Pattern)|$)', markdown_text, re.IGNORECASE)
    m_amel = re.search(r'###?\s*(?:Axes|Conseils|Pistes|Amélioration|Remédiation|Anti-Pattern)[\s\S]*?(?=###?\s*Commentaire|###?\s*Appréciation|$)', markdown_text, re.IGNORECASE)
    m_comm = re.search(r'###?\s*(?:Commentaire|Appréciation)[\s\S]*?$', markdown_text, re.IGNORECASE)

    if m_bilan: sec_bilan = re.sub(r'^###?[^\n]*\n+', '', m_bilan.group(0)).strip()
    if m_forts: sec_points_forts = re.sub(r'^###?[^\n]*\n+', '', m_forts.group(0)).strip()
    if m_amel: sec_ameliorations = re.sub(r'^###?[^\n]*\n+', '', m_amel.group(0)).strip()
    if m_comm: sec_commentaire = re.sub(r'^###?[^\n]*\n+', '', m_comm.group(0)).strip()

    return sec_bilan, sec_points_forts, sec_ameliorations, sec_commentaire

def find_single_student_plagiarism(student_dir, student_id, audit_cohort_mod=None):
    """Détecte automatiquement si un étudiant présente une similarité anormale avec un voisin de promo."""
    try:
        parent_p = Path(student_dir).resolve().parent
        siblings = [
            d for d in parent_p.iterdir()
            if d.is_dir() and not d.name.startswith('.')
            and d.name not in ('evaluations', 'codelab', '.hidden', 'recovery', '__pycache__')
        ]
        if len(siblings) >= 2:
            if audit_cohort_mod is None:
                import audit_cohort
                audit_cohort_mod = audit_cohort
            if hasattr(audit_cohort_mod, "detect_cohort_plagiarism"):
                suspicions = audit_cohort_mod.detect_cohort_plagiarism(str(parent_p))
                if isinstance(suspicions, tuple):
                    if len(suspicions) == 2 and isinstance(suspicions[1], dict) and student_id in suspicions[1]:
                        return suspicions[1][student_id]
                    suspicions = suspicions[0]
                if isinstance(suspicions, list):
                    agg = None
                    existing_files = set()
                    for sp in suspicions:
                        partner = None
                        partner_id = None
                        if sp.get('s1') == student_id:
                            partner = sp.get('name2', sp.get('s2'))
                            partner_id = sp.get('s2')
                        elif sp.get('s2') == student_id:
                            partner = sp.get('name1', sp.get('s1'))
                            partner_id = sp.get('s1')
                        if partner_id:
                            if agg is None:
                                agg = {
                                    'partner': partner,
                                    'partner_id': partner_id,
                                    'max_sim': sp.get('max_sim', 0),
                                    'files': [],
                                    'severity': sp.get('severity', '')
                                }
                            for sf in sp.get('similar_files', []):
                                if sf not in existing_files:
                                    agg['files'].append(sf)
                                    existing_files.add(sf)
                            if sp.get('max_sim', 0) > agg['max_sim']:
                                agg['max_sim'] = sp.get('max_sim', 0)
                                agg['partner'] = partner
                                agg['partner_id'] = partner_id
                                agg['severity'] = sp.get('severity', '')
                    if agg is not None:
                        return agg
    except Exception:
        pass
    return None

def build_generic_scores_table(scores, data, profil):
    """Génère un tableau Markdown dynamique et soigné pour n'importe quel skill."""
    total = scores.get("total", data.get("total", "N/A"))
    axis_labels = {
        "syntax": ("Axe 1 : Compilation & Syntaxe", "/ 6.0", "Conformité syntaxique et compilation"),
        "compilation": ("Axe 1 : Compilation du Code", "/ 6.0", "Conformité syntaxique et compilation"),
        "architecture": ("Axe 2 : Architecture & Conception", "/ 8.0", "Structuration modulaire et logique"),
        "modelisation": ("Axe 2 : Modélisation Objet", "/ 8.0", "Conception orientée objet et encapsulation"),
        "conception": ("Axe 2 : Conception Algorithmique", "/ 8.0", "Découpage et structuration"),
        "style": ("Axe 3 : Style & Lisibilité", "/ 4.0", "Conventions de code, indentation et documentation"),
        "tests": ("Axe 4 : Démarche de Test", "/ 2.0", "Assertions et jeux de tests unitaires"),
        "axe1_progression": ("Axe 1 : Progression & Couverture", "/ 10.0", "Jalons et progression des TP"),
        "axe2_architecture": ("Axe 2 : Architecture Algorithmique", "/ 8.0", "Modularité et qualité"),
        "axe3_style": ("Axe 3 : Style & Rigueur", "/ 2.0", "Documentation et conventions"),
        "score_p1": ("Partie 1 : Flux & Regex", "/ 8.0", "Traitement de fichiers et regex"),
        "score_p2": ("Partie 2 : Numpy & Matplotlib", "/ 10.0", "Calcul matriciel et visualisations"),
        "score_transversal": ("Transversal : Style Pythonique", "/ 2.0", "Idiomes et vectorisation"),
    }
    rows = [
        "| Axe d'Évaluation | Note Attribuée | Barème Officiel | Appréciation Synthétique |",
        "| :--- | :---: | :---: | :--- |"
    ]
    has_custom_axes = False
    raw_sum = 0.0
    for k, v in scores.items():
        if k in ("total", "total_note", "note_technique_brute", "note_apres_ia", "malus_similarite", "malus_saut", "axe2_details"):
            continue
        try:
            val_f = float(v)
            val_str = f"{val_f:.1f}"
            raw_sum += val_f
        except (ValueError, TypeError):
            val_str = str(v)
        lbl, bareme, desc = axis_labels.get(k, (f"Axe : {k.replace('_', ' ').capitalize()}", "", "Évaluation spécifique"))
        rows.append(f"| **{lbl}** | **{val_str}** | {bareme} | {desc} |")
        has_custom_axes = True
    
    try:
        tot_f = float(total)
        tot_str = f"{tot_f:.1f}"
    except (ValueError, TypeError):
        tot_str = str(total)

    note_sans_malus = scores.get("note_technique_brute", data.get("note_technique_brute", round(raw_sum, 1) if has_custom_axes else total))
    try:
        nsm_str = f"{float(note_sans_malus):.1f}"
    except (ValueError, TypeError):
        nsm_str = str(note_sans_malus)

    if not has_custom_axes:
        rows.append(f"| **Note sans malus (IA / Similarité)** | **{nsm_str}** | / 20.0 | Note technique brute |")
        rows.append(f"| **Note Globale (Avec malus)** | **{tot_str}** | / 20.0 | Bilan global de l'évaluation |")
    else:
        rows.append(f"| **Note sans malus (IA / Similarité)** | **{nsm_str}** | **/ 20.0** | **Somme technique brute des axes** |")
        rows.append(f"| **TOTAL GÉNÉRAL (Avec malus)** | **{tot_str}** | **/ 20.0** | **{profil}** |")
    return "\n".join(rows)

def build_deterministic_pedagogical_sections(skill_name, data, identity):
    """Construit les 4 sections pédagogiques de repli en mode déterministe."""
    s_name = identity.get("student_name") or data.get("student_name") or "Étudiant"
    total = data.get("scores", {}).get("total", data.get("total", 0.0))
    snippets = data.get("snippets", {})
    antipatterns = data.get("antipatterns", [])

    # 1. Bilan
    if "python" in skill_name.lower():
        sujet = data.get("sujet", 1)
        p1 = data.get("score_p1", 0.0)
        p2 = data.get("score_p2", 0.0)
        bilan = f"L'évaluation porte sur le **TP n°4 (Sujet {sujet})**. Vous obtenez **{p1:.2f} / 8.0** sur la Partie 1 (Flux & Regex) et **{p2:.2f} / 10.0** sur la Partie 2 (Numpy & Matplotlib), complétés par **{data.get('score_transversal', 0.0):.2f} / 2.0** sur l'axe transversal."
    else:
        bilan = f"L'analyse statique et le contrôle de compilation de vos sources aboutissent à une note globale estimée de **{total} / 20.0**. Les critères couvrent la syntaxe, l'architecture modulaire, le style et les tests."

    # 2. Points Forts
    forts_parts = []
    if snippets.get("highlights"):
        forts_parts.append("Plusieurs initiatives et constructions pertinentes ont été relevées dans vos fichiers :")
        for hl in snippets["highlights"][:3]:
            h_title = hl.get("title", hl.get("function", "Fonction"))
            h_file = hl.get("file", "source")
            h_line = hl.get("line", "")
            h_code = hl.get("code", "")
            loc = f" (`{h_file}`, ligne {h_line})" if h_line else f" (`{h_file}`)"
            forts_parts.append(f"- **{h_title}**{loc} :\n```\n{h_code}\n```")
    else:
        forts_parts.append("Votre travail témoigne d'un investissement régulier sur les exercices fondamentaux du module.")
    points_forts = "\n\n".join(forts_parts)

    # 3. Axes d'Amélioration
    amel_parts = []
    if antipatterns:
        amel_parts.append("Certains points de méthode et anti-patterns méritent une attention particulière :")
        for ap in antipatterns[:3]:
            amel_parts.append(f"- ⚠️ **{ap}**")
    if snippets.get("compilation_errors"):
        amel_parts.append("Erreurs relevées lors du contrôle syntaxique :")
        for ce in snippets["compilation_errors"][:2]:
            c_file = ce.get("file", "source")
            c_err = ce.get("error", "Erreur")
            c_code = ce.get("code", "")
            amel_parts.append(f"- Fichier `{c_file}` : `{c_err}`\n```\n{c_code}\n```")
    elif snippets.get("anti_patterns"):
        amel_parts.append("Extraits à retravailler méthodologiquement :")
        for ap in snippets["anti_patterns"][:2]:
            ap_title = ap.get("title", "Alerte")
            ap_file = ap.get("file", "source")
            ap_code = ap.get("code", "")
            amel_parts.append(f"- **{ap_title}** (`{ap_file}`) :\n```\n{ap_code}\n```")
    if not amel_parts:
        amel_parts.append("1. **Approfondissement algorithmique :** Continuez à décomposer vos fonctions en sous-traitements réutilisables.\n\n2. **Rigueur des tests :** Intégrez systématiquement des jeux d'assertions pour valider les cas limites.")
    axes_amelioration = "\n\n".join(amel_parts)

    # 4. Commentaire de l'enseignant
    try:
        tot_num = float(total)
    except Exception:
        tot_num = 10.0
    if tot_num >= 15.0:
        commentaire = f"Très bon travail {s_name} ! Vos bases sont solides et votre démarche est rigoureuse. Poursuivez dans cette voie pour les modules suivants."
    elif tot_num >= 10.0:
        commentaire = f"Travail sérieux et satisfaisant {s_name}. En consolidant la décomposition modulaire et les tests unitaires, vous progresserez encore plus efficacement."
    else:
        commentaire = f"{s_name}, un investissement soutenu est nécessaire pour assimiler pleinement les concepts fondamentaux. N'hésitez pas à solliciter l'équipe pédagogique lors des séances de TP."

    return bilan, points_forts, axes_amelioration, commentaire

def build_canonical_introprog_report(res, identity, eval_date_str, skill_meta, sections=None, raw_llm_text=None, plagiarism_info=None):
    """
    Construit la fiche d'évaluation individuelle IntroProg selon un gabarit 100% strict et uniforme.
    Garantit que TOUS les étudiants ont exactement le même en-tête, le même cartouche,
    les mêmes noms d'axes, et les mêmes notes que le CSV, sans aucune dépendance envers des regex.
    """
    sc = res.get("scores", {})
    a1 = float(sc.get("axe1_progression", 0.0))
    a2 = float(sc.get("axe2_architecture", 0.0))
    a3 = float(sc.get("axe3_style", 0.0))
    note_sans_malus = float(sc.get("note_technique_brute", round(a1 + a2 + a3, 1)))
    tot = float(sc.get("total_note", 0.0))
    profil = res.get("profil", "Standard")

    s_name = identity.get("student_name") or res.get("student_name") or "Étudiant"
    s_id = identity.get("student_id") or res.get("student_id") or ""
    s_group = identity.get("student_group") or res.get("student_group") or ""
    group_line = f"\n- **Groupe :** {s_group}" if s_group else ""
    
    ai_data = res.get("ai_analysis", {})
    ai_level = ai_data.get("level", "Faible (0% - Code étudiant authentique)")
    ai_details = ai_data.get("details", "Code étudiant authentique")

    integ = extract_integrity_indicators(res, plagiarism_info=plagiarism_info)

    # Appréciations synthétiques du tableau
    if res.get("valid_files", 0) == 0 or (tot == 0.0 and note_sans_malus == 0.0):
        app_a1 = "Aucun travail rendu sur CodeLab."
        app_a2 = "Aucun programme soumis."
        app_a3 = "Aucun code source à analyser."
    else:
        malus_s = float(sc.get("malus_saut", 0.0))
        app_a1 = "Parcours séquentiel complet sans saut." if malus_s == 0 else f"Pénalité pour saut d'activité majeure (-{malus_s:.1f} pt)."
        app_a2 = "Modularité et qualité algorithmique."
        doc_r = float(res.get("doc_ratio", 0.0))
        app_a3 = f"Documentation {doc_r:.0f}% des fonctions. Conventions respectées." if a3 >= 1.6 else "Effort à accentuer sur la documentation formalisée ou les conventions de casse."

    sec_bilan = ""
    sec_points_forts = ""
    sec_ameliorations = ""
    sec_commentaire = ""

    if sections:
        sec_bilan = sections.get("bilan", "")
        sec_points_forts = sections.get("points_forts", "")
        sec_ameliorations = sections.get("axes_amelioration", "")
        sec_commentaire = sections.get("commentaire", "")
    elif raw_llm_text:
        text = raw_llm_text
        text = re.sub(r'```(?:eval_scores|json)[\s\S]*?```', '', text).strip()
        
        # Extraction robuste des 4 sections pédagogiques
        m_bilan = re.search(r'###\s*(?:Bilan|Progression|Analyse)[\s\S]*?(?=###\s*Points?\s*Forts?|$)', text, re.IGNORECASE)
        m_forts = re.search(r'###\s*Points?\s*Forts?[\s\S]*?(?=###\s*(?:Axes|Conseils|Pistes|Amélioration)|$)', text, re.IGNORECASE)
        m_amel = re.search(r'###\s*(?:Axes|Conseils|Pistes|Amélioration)[\s\S]*?(?=###\s*Commentaire|$)', text, re.IGNORECASE)
        m_comm = re.search(r'###\s*Commentaire[\s\S]*?$', text, re.IGNORECASE)

        if m_bilan:
            sec_bilan = re.sub(r'^###[^\n]*\n+', '', m_bilan.group(0)).strip()
        if m_forts:
            sec_points_forts = re.sub(r'^###[^\n]*\n+', '', m_forts.group(0)).strip()
        if m_amel:
            sec_ameliorations = re.sub(r'^###[^\n]*\n+', '', m_amel.group(0)).strip()
        if m_comm:
            sec_commentaire = re.sub(r'^###[^\n]*\n+', '', m_comm.group(0)).strip()

    # Construction déterministe riche et pédagogique si l'évaluation est exécutée sans IA (--no-ai)
    snippets = res.get("snippets", {})
    dinfo = res.get("dates_info", {})
    milestones_details = res.get("milestones_details", [])
    if not sec_bilan:
        bilan_lines = [
            f"- **Volume réalisé** : **{res.get('milestones_present', 0)} / {res.get('milestones_total', 31)}** jalons validés ({res.get('total_files', 0)} fichiers C dont {res.get('valid_files', 0)} actifs, {res.get('total_lines', 0)} lignes de code).",
            f"- **Contrôle de compilation Clang** : **{res.get('compiled_files', 0)} / {res.get('valid_files', 0)}** fichiers valides compilés avec succès ({res.get('compilation_rate', 0.0)}%) *(forme `void main()` et bibliothèques standards C tolérées sans malus)*."
        ]
        if dinfo.get("distinct_days"):
            bilan_lines.append(f"- **Régularité du travail** : {dinfo['distinct_days']} jours d'activité sur CodeLab étalés sur {dinfo.get('span_days', 0)} jours (du {dinfo.get('first_date', '')} au {dinfo.get('last_date', '')}).")
        bilan_lines.append(f"- **Fonctions écrites** : {res.get('total_functions', 0)} fonctions (dont {res.get('documented_functions', 0)} formellement documentées, soit {res.get('doc_ratio', 0.0)}%).")
        if res.get("compilation_errors"):
            bilan_lines.append(f"\n> [!WARNING]\n> **Erreurs de syntaxe / compilation C détectées ({len(res['compilation_errors'])} fichier(s))** :")
            for err_info in res["compilation_errors"]:
                bilan_lines.append(f"> - `{err_info['file']}` : `{err_info['error']}`")
            if snippets.get("compilation_errors"):
                bilan_lines.append(">\n> **Extrait(s) du code concerné :**")
                for c_err in snippets["compilation_errors"]:
                    code_quoted = "\n".join(f"> {l}" for l in c_err["code"].splitlines())
                    bilan_lines.append(f"> ```c\n> // {c_err['file']} (ligne {c_err['line']})\n{code_quoted}\n> ```")
        if res.get("critical_skips"):
            bilan_lines.append("\n> [!CAUTION]\n> **Activités majeures sautées détectées** :")
            for sk in res["critical_skips"]:
                bilan_lines.append(f"> - `{sk['tp']}` : **{sk['id']}** ({sk['description']})")
        if milestones_details:
            bilan_lines.append("\n#### Détail par Activité :")
            tp_status = {}
            for m in milestones_details:
                tp_status.setdefault(m["tp"], []).append(m)
            for tp, m_list in tp_status.items():
                done_cnt = sum(1 for m in m_list if m.get("present"))
                tot_cnt = len(m_list)
                symb = "✅" if done_cnt == tot_cnt else ("🟡" if done_cnt > 0 else "⚪")
                bilan_lines.append(f"- **{tp}** [{done_cnt}/{tot_cnt}] {symb} : " + ", ".join(f"`{m['id']}` {'✓' if m.get('present') else '✗'}" for m in m_list))
        sec_bilan = "\n".join(bilan_lines)

    if not sec_points_forts:
        forts_lines = []
        if a2 >= 7.0:
            forts_lines.append("- **Conception architecturale solide** : décomposition modulaire pertinente, fonctions ciblées et bonne logique d'automate.")
        elif a2 >= 5.0:
            forts_lines.append("- **Bonne structuration modulaire** : utilisation pertinente des fonctions et des primitives de déplacement.")
        if any(m.get("id") == "fangio" and m.get("present") for m in milestones_details):
            forts_lines.append("- **Projet Fangio validé** : intégration réussie du suivi de piste, du contournement d'obstacle et du chronométrage.")
        if any(m.get("id") == "knossos1" and m.get("present") for m in milestones_details):
            forts_lines.append("- **Projet Knossos 1 validé** : lecture d'un plan d'ordres et navigation autonome dans le labyrinthe (`B09` / `B09bis`).")
        if any(m.get("id") == "knossos2" and m.get("present") for m in milestones_details):
            forts_lines.append("- **Projet Knossos 2 validé** : mémorisation des déplacements par tableaux de compteurs et trajet retour optimisé.")
        if res.get("has_div_zero_check"):
            forts_lines.append("- **Programmation défensive** : protection explicite contre la division par zéro dans le calcul de moyenne pondérée (`TP6E2`).")
        if res.get("doc_ratio", 0.0) >= 50.0:
            forts_lines.append(f"- **Documentation soignée** : {res.get('doc_ratio', 0.0)}% des fonctions disposent d'un cartouche descriptif.")
        if snippets.get("highlights"):
            forts_lines.append("\n#### Extraits remarquables observés dans votre code :")
            for hl in snippets["highlights"]:
                forts_lines.append(f"- **{hl['title']}** (`{hl['file']}`, ligne {hl['line']}) :\n```c\n{hl['code']}\n```")
        if not forts_lines:
            forts_lines.append("- Votre démarche montre un investissement sur les exercices fondamentaux abordés durant les séances.")
        sec_points_forts = "\n".join(forts_lines)

    if not sec_ameliorations:
        amel_lines = []
        if not res.get("has_div_zero_check") and res.get("milestones_present", 0) >= 24:
            amel_lines.append("- **Programmation défensive (division par zéro)** : dans `TP6E2` (`moyennePosition`), si tous les capteurs lisent du blanc, la somme des pondérations vaut `0`. Pensez à vérifier `if (somme > 0)` avant de diviser.")
        if res.get("doc_ratio", 0.0) < 40.0:
            amel_lines.append("- **Documenter systématiquement chaque fonction** : indiquez en commentaire le rôle de la fonction, ses paramètres et sa valeur de retour.")
        if res.get("const_lowercase_count", 0) > 0:
            amel_lines.append(f"- **Conventions de nommage** : {res['const_lowercase_count']} constante(s) sont en minuscules (`const int`). Utilisez `MAJUSCULES_AVEC_UNDERSCORE` pour les constantes et `camelCase` pour les variables/fonctions.")
        if res.get("compilation_errors"):
            amel_lines.append(f"- **Corriger les erreurs de compilation ({len(res['compilation_errors'])} fichier(s))** : testez chaque programme dans CodeLab avant de passer à l'exercice suivant.")
        if res.get("critical_skips"):
            amel_lines.append("- **Compléter les jalons intermédiaires sautés** : chaque activité prépare les concepts nécessaires aux projets intégrateurs (*Fangio*, *Knossos*).")
        if snippets.get("anti_patterns"):
            amel_lines.append("\n#### Extraits à retravailler méthodologiquement :")
            for ap in snippets["anti_patterns"]:
                amel_lines.append(f"- **{ap['title']}** (`{ap['file']}`, ligne {ap['line']}) :\n```c\n{ap['code']}\n```")
        if not amel_lines:
            amel_lines.append("- Poursuivez vos efforts en veillant à tester systématiquement vos programmes aux cas limites et à maintenir une documentation régulière.")
        sec_ameliorations = "\n".join(amel_lines)

    if not sec_commentaire:
        if note_sans_malus >= 17.5:
            sec_commentaire = f"Excellent parcours {s_name} (note technique hors malus : {note_sans_malus:.1f}/20). L'architecture du code et les projets intégrateurs jusqu'à Knossos 2 sont très bien maîtrisés."
        elif note_sans_malus >= 15.0:
            sec_commentaire = f"Très bon travail {s_name} (note technique hors malus : {note_sans_malus:.1f}/20). Les compétences de programmation impérative et les projets jusqu'à Knossos 1 sont solidement acquis."
        elif note_sans_malus >= 13.0:
            sec_commentaire = f"Bon travail {s_name} (note technique hors malus : {note_sans_malus:.1f}/20). Le parcours jusqu'au projet Fangio est bien réalisé ; poursuivez vers la maîtrise complète des tableaux (TP6)."
        elif note_sans_malus >= 10.0:
            sec_commentaire = f"Ensemble correct {s_name} ({note_sans_malus:.1f}/20 hors malus), mais certains jalons ou projets de fin de semestre demandent encore à être consolidés."
        else:
            sec_commentaire = f"Des notions fondamentales restent fragiles ou incomplètes {s_name} ({note_sans_malus:.1f}/20). Une reprise méthodique des exercices de base est recommandée."
        if tot < note_sans_malus:
            sec_commentaire += f" Attention : la note finale avec malus est ajustée à **{tot:.1f}/20** en raison des alertes d'intégrité (similarité inter-étudiants et/ou suspicion d'aide IA)."

    scores_table = f"""| Axe d'Évaluation | Note Attribuée | Barème Officiel | Appréciation Synthétique |
| :--- | :---: | :---: | :--- |
| **Axe 1 : Progression & Couverture des TP** | **{a1:.1f}** | / 10.0 | {app_a1} |
| **Axe 2 : Architecture & Qualité Algorithmique** | **{a2:.1f}** | / 8.0 | {app_a2} |
| **Axe 3 : Style, Rigueur & Bonnes Pratiques** | **{a3:.1f}** | / 2.0 | {app_a3} |
| **Note sans malus (IA / Similarité)** | **{note_sans_malus:.1f}** | **/ 20.0** | **Somme technique brute des 3 axes** |
| **TOTAL GÉNÉRAL (Avec malus)** | **{tot:.1f}** | **/ 20.0** | **{profil}** |"""

    template_vars = {
        "nom": s_name,
        "student_name": s_name,
        "login": s_id,
        "student_id": s_id,
        "groupe": s_group,
        "student_group": s_group,
        "group_line": group_line,
        "profil": profil,
        "eval_date": eval_date_str,
        "skill_name": skill_meta.get("name", "eval-introprog"),
        "skill_last_modified": skill_meta.get("last_modified", ""),
        "eval_engine": res.get("eval_engine", "LLM Gemini"),
        "ai_level": ai_level,
        "ai_details": ai_details,
        "scores_table": scores_table,
        "axe1_note": f"{a1:.1f}",
        "axe1_max": "10.0",
        "axe1_app": app_a1,
        "axe2_note": f"{a2:.1f}",
        "axe2_max": "8.0",
        "axe2_app": app_a2,
        "axe3_note": f"{a3:.1f}",
        "axe3_max": "2.0",
        "axe3_app": app_a3,
        "note_sans_malus": f"{note_sans_malus:.1f}",
        "total_note": f"{tot:.1f}",
        "note_totale": f"{tot:.1f}",
        "chronometrie": integ["chronometrie"],
        "chronometrie_courte": integ["chronometrie_courte"],
        "stylometrie": integ["stylometrie"],
        "stylometrie_courte": integ["stylometrie_courte"],
        "similarite": integ["similarite"],
        "similarite_courte": integ["similarite_courte"],
        "analyse_integrite": integ["analyse_integrite"],
        "bilan": sec_bilan,
        "points_forts": sec_points_forts,
        "axes_amelioration": sec_ameliorations,
        "commentaire": sec_commentaire,
    }

    # Recherche d'un gabarit Markdown personnalisé (report_template.md)
    intro_dir = get_skill_dir("eval-introprog") or INTROPROG_DIR
    template_candidates = [
        intro_dir / "report_template.md" if intro_dir else None,
        Path("/Users/lehuen/codelab.files/skills/eval-introprog/report_template.md"),
        INTROPROG_DIR / "report_template.md"
    ]
    rendered = render_template_if_exists(template_candidates, template_vars)
    if rendered:
        return rendered

    return f"""# Fiche d'Évaluation Individuelle — Travaux Pratiques

### Étudiant : **{s_name}**

- **Login / ID :** `{s_id}`
- **Module :** Introduction à la programmation (L1 Informatique){group_line}
- **Profil :** {profil}

---

### 📋 Cartouche de Traçabilité & Synthèse d'Intégrité

- **Date de l'évaluation :** {eval_date_str}
- **Skill appliqué :** `{skill_meta['name']}` (dernière modification : {skill_meta['last_modified']})
- **Moteur d'évaluation :** {res.get('eval_engine', 'LLM Gemini')}
- **Chronométrie :** {integ['chronometrie_courte']}
- **Stylométrie d'indentation :** {integ['stylometrie_courte']}
- **Test de similarité (Plagiat) :** {integ['similarite_courte']}
- **Indice d'assistance IA :** {ai_level}
- **Environnement :** CodeLab v1.5.0

---

### Tableau Récapitulatif des Notes Calibrées

{scores_table}

---

{integ['analyse_integrite']}

---

### Bilan de Progression & Analyse Pédagogique

{sec_bilan}

---

### Points Forts Observés

{sec_points_forts}

---

### Axes d'Amélioration & Conseils Méthodologiques

{sec_ameliorations}

---

### Commentaire de l'Enseignant

{sec_commentaire}
"""

def evaluate_single_c_folder(student_dir, output_file, sessions_xml=None, ai_props=None, student_login=None, student_name=None, evaluated_results=None, plagiarism_info=None):
    """Évalue un dossier unique en C (eval-introprog) via l'approche hybride comptable + compilateur + Gemini IA (3 axes : 10 + 8 + 2 = 20)."""
    identity = resolve_student_identity(student_dir, student_login=student_login, student_name=student_name, sessions_xml_path=sessions_xml)
    print(f"[INFO] Analyse du code C : {identity['student_name']} ({identity['student_id']})", flush=True)
    introprog_dir = get_skill_dir("eval-introprog") or INTROPROG_DIR
    sys.path.insert(0, str(introprog_dir / "scripts"))
    try:
        from audit_student import analyze_student
        import generate_report
        generate_individual_report = getattr(generate_report, "generate_individual_report", None)
        if not generate_individual_report:
            raise ImportError("generate_individual_report introuvable dans generate_report.py")
    except ImportError as e:
        print(f"[FAIL] Impossible d'importer le skill C: {e}", file=sys.stderr, flush=True)
        return None

    # 1. Analyse déterministe comptable et compilation Clang
    if evaluated_results is None:
        print("[PROGRESS] 1/3 Contrôle Clang et jalons...", flush=True)
    print("[INFO] Contrôle syntaxique Clang (-fsyntax-only) et vérification des 31 jalons...", flush=True)
    res = analyze_student(str(student_dir), sessions_xml_path=sessions_xml)

    res["student_id"] = identity["student_id"]
    res["student_name"] = identity["student_name"]
    res["assignment"] = identity["assignment"]

    # 1.1 Recherche de similarité inter-étudiants si non fournie
    if plagiarism_info is None:
        plagiarism_info = find_single_student_plagiarism(student_dir, identity['student_id'])

    if plagiarism_info:
        res["plagiarism_alert"] = plagiarism_info
        sc_p = res.get("scores", {})
        if "note_apres_ia" not in sc_p:
            sc_p["note_apres_ia"] = float(sc_p.get("total_note", 0.0))
        if "note_technique_brute" not in sc_p:
            sc_p["note_technique_brute"] = round(
                float(sc_p.get("axe1_progression", 0.0))
                + float(sc_p.get("axe2_architecture", 0.0))
                + float(sc_p.get("axe3_style", 0.0)), 1
            )
        nb_sim = len(plagiarism_info.get("files", []))
        max_sim = float(plagiarism_info.get("max_sim", 0.0))
        sim_malus = 0.0
        if nb_sim >= 3 or max_sim >= 95.0:
            sim_malus = 2.0
        elif nb_sim >= 2 or max_sim >= 85.0:
            sim_malus = 1.0
        elif nb_sim >= 1 and max_sim >= 75.0:
            sim_malus = 0.5
        if sim_malus > 0.0:
            sc_p["malus_similarite"] = sim_malus
            tot = float(sc_p["note_apres_ia"]) - sim_malus
            if "note_technique_brute" in sc_p:
                tot = max(tot, float(sc_p["note_technique_brute"]) - 3.5)
            sc_p["total_note"] = max(0.0, round(tot, 1))
            if "Suspicion de partage" not in res.get("profil", ""):
                res["profil"] = f"{res.get('profil', 'Standard')} (Malus Similarité -{sim_malus:.1f}pt)"

    sc = res.get('scores', {})
    note_sans_malus = sc.get('note_technique_brute', sc.get('total_note', 'N/A'))
    note = sc.get('total_note', 'N/A')
    jalons = f"{res.get('milestones_present', 0)}/{res.get('milestones_total', 31)}"
    print(f"[DONE] Audit statique comptable : {jalons} jalons validés | Note sans malus : {note_sans_malus}/20 | Note avec malus : {note}/20", flush=True)

    # 1.2 Affichage des analyses d'intégrité à la console
    integ = extract_integrity_indicators(res, plagiarism_info=plagiarism_info)
    print(f"[INFO] Chronométrie : {integ['chronometrie_courte']}", flush=True)
    print(f"[INFO] Stylométrie : {integ['stylometrie_courte']}", flush=True)
    if "🚨" in integ['similarite_courte'] or "⚠️" in integ['similarite_courte']:
        print(f"[WARN] Similarité / Plagiat : {integ['similarite_courte']}", flush=True)
    else:
        print(f"[DONE] Similarité / Plagiat : {integ['similarite_courte']}", flush=True)

    skill_meta = get_skill_metadata(introprog_dir or "eval-introprog")
    eval_date_str = datetime.now().strftime("%d/%m/%Y à %H:%M")
    res["eval_date"] = eval_date_str
    res["skill_last_modified"] = skill_meta["last_modified"]
    res["skill_name"] = skill_meta["name"]
    res["skill_version"] = skill_meta["version"]

    # 2. Collecte du code source C de l'élève et des contextes de TP (sujets & corrigés)
    student_code, active_tps = collect_student_c_code(student_dir)
    activities_ctx = load_activities_context(introprog_dir, active_tps=active_tps)

    # Si l'étudiant n'a rendu AUCUN fichier valide ou si le code est vide (ex: Gaspard Cotte)
    if not student_code or res.get("valid_files", 0) == 0 or res.get("total_lines", 0) == 0:
        print(f"[INFO] Aucun travail rendu pour {identity['student_id']}. Fiche 0.0/20 générée directement sans appel LLM.", flush=True)
        res["scores"]["axe1_progression"] = 0.0
        res["scores"]["axe2_architecture"] = 0.0
        res["scores"]["axe3_style"] = 0.0
        res["scores"]["total_note"] = 0.0
        if "axe4_tests" in res["scores"]:
            del res["scores"]["axe4_tests"]
        res["profil"] = "Aucun travail rendu (Non évalué)"
        res["eval_engine"] = "Moteur Déterministe CodeLab"
        report_text = build_canonical_introprog_report(res, identity, eval_date_str, skill_meta, sections={
            "bilan": f"Le diagnostic comptable effectué sur l'espace de travail de l'étudiant indique qu'**aucun fichier source C n'a été trouvé ni soumis** sur le serveur CodeLab (`total_files : 0`, `valid_files : 0`, `compiled_files : 0`).\n\nPar conséquent, aucun jalon du semestre (du TP1 au TP6, incluant les projets intégrateurs *Fangio* et *Knossos*) n'a pu être validé.",
            "points_forts": "Aucun code n'ayant été versé au dossier de TP, il n'est pas possible d'évaluer des réalisations techniques pour cette session.",
            "axes_amelioration": "1. **Assiduité et dépôt des travaux :**\n\n   Veillez à bien vous connecter régulièrement sur l'environnement CodeLab lors des séances encadrées et à créer vos fichiers dans les répertoires correspondants (`TP1`, `TP2`, etc.).\n\n2. **Rattrapage du retard :**\n\n   Les TPs d'introduction à la programmation constituent un socle cumulatif indispensable. Ne pas réaliser les premiers travaux pénalise fortement la compréhension des concepts avancés abordés dans la seconde moitié du semestre.\n\n3. **Accompagnement :**\n\n   N'hésitez pas à solliciter les enseignants lors des permanences ou des séances de travaux pratiques en cas de blocage technique ou méthodologique.",
            "commentaire": f"Monsieur/Madame {identity['student_name']}, votre espace de travail ne comporte actuellement aucun code pour le module d'introduction à la programmation. Ce travail est donc évalué à 0/20 par absence de rendu. Nous vous invitons vivement à vous manifester auprès de l'équipe pédagogique afin de régulariser votre situation dès la prochaine séance."
        }, plagiarism_info=plagiarism_info)
        clean_markdown = normalize_markdown_newlines(report_text)
        os.makedirs(os.path.dirname(os.path.abspath(output_file)), exist_ok=True)
        with open(output_file, "w", encoding="utf-8") as f:
            f.write(clean_markdown)
        print(f"[DONE] Fiche individuelle sauvegardée (0/20) : {os.path.basename(output_file)}", flush=True)
        if evaluated_results is not None:
            evaluated_results.append(res)
        return output_file

    # Tenter l'évaluation et calibration par LLM si clé présente
    api_key = ai_props.get("GEMINI_API_KEY") if ai_props else None
    model = ai_props.get("GEMINI_MODEL", "gemini-3.5-flash-lite") if ai_props else "gemini-3.5-flash-lite"

    if api_key:
        res["eval_engine"] = f"LLM Gemini ({model}) [Hybride Sujets & Corrigés]"
        if evaluated_results is None:
            print(f"[PROGRESS] 2/3 Analyse approfondie ({model})...", flush=True)
        print(f"[INFO] Analyse approfondie et calibration des notes par LLM ({model})...", flush=True)

        # Formatage des sujets et corrigés de référence
        activities_parts = []
        if activities_ctx:
            for tp_name, tp_data in activities_ctx.items():
                activities_parts.append(f"### 🎯 Sujet Officiel {tp_name}\n{tp_data.get('sujet', '(Non fourni)')}")
                corriges = tp_data.get("corriges", {})
                if corriges:
                    activities_parts.append(f"#### 💡 Corrigés Types de Référence C pour {tp_name} :")
                    for c_name, c_src in corriges.items():
                        activities_parts.append(f"**Corrigé `{c_name}`** :\n```c\n{c_src}\n```")
        activities_text = "\n\n".join(activities_parts) if activities_parts else "(Aucun sujet/corrigé d'activité disponible)"

        # Formatage du code étudiant avec statut Clang
        comp_err_files = {e['file']: e['error'] for e in res.get('compilation_errors', [])}
        student_code_parts = []
        for rel_path, code_str in student_code.items():
            if rel_path in comp_err_files:
                status_str = f"❌ Erreur Clang : {comp_err_files[rel_path]}"
            else:
                status_str = "✅ Compilation PASSED"
            student_code_parts.append(f"#### Programme Étudiant `{rel_path}` ({status_str}) :\n```c\n{code_str}\n```")
        student_code_text = "\n\n".join(student_code_parts) if student_code_parts else "(Aucun code source C significatif trouvé)"

        prompt = f"""Tu es Jérôme Lehuen, enseignant-chercheur en informatique à Le Mans Université et concepteur de CodeLab.
Tu évalues les travaux pratiques d'un étudiant pour le module 'Introduction à la programmation' (L1 Informatique).

Ton rôle est d'effectuer une ÉVALUATION PÉDAGOGIQUE HYBRIDE APPROFONDIE :
Tu ne te reposes PAS uniquement sur un comptage rigide de regex ou de patterns python.
Tu disposes :
1. Du diagnostic comptable factuel et du contrôle de compilation Clang (-fsyntax-only).
2. Des sujets officiels et des corrigés C types de référence des TPs traités par l'étudiant.
3. De l'intégralité du code C réel écrit par l'étudiant.

==================================================
1. DIAGNOSTIC FACTUEL COMPTABLE & CLANG
==================================================
```json
{json.dumps(res, indent=2, ensure_ascii=False)}
```

==================================================
2. SUJETS OFFICIELS & CORRIGÉS DE RÉFÉRENCE
==================================================
{activities_text}

==================================================
3. CODE SOURCE C EFFECTIF DE L'ÉTUDIANT
==================================================
{student_code_text}

==================================================
MISSION DE CALIBRATION ET DIRECTIVES D'ÉVALUATION (3 AXES : 10 + 8 + 2 = 20)
==================================================
En tant qu'enseignant-chercheur universitaire rigoureux, exigeant et juste, analyse le travail réel, la compréhension conceptuelle et l'authenticité de l'étudiant.
NE SOIS PAS COMPLAISANT : face aux risques majeurs de plagiat et d'usage non assimilé de l'IA générative, ton rôle est d'évaluer avec lucidité.
Ne sur-note JAMAIS un simple empilement de fichiers d'exercices guidés sans compréhension ni initiatives réelles.
Calibre la note finale /20 sur les 3 axes officiels en appliquant des règles d'exigence académique et de discernement :

- Axe 1 : Progression & Couverture des TP (sur 10.0 pts)
  BARÈME D'ACHÈVEMENT DU SEMESTRE CALIBRÉ :
  • Repères cibles pour un étudiant ayant bien travaillé :
    - Jusqu'à Fangio inclus (TP1 à TP5 complets) ≈ 14 / 20 au total (Axe 1 ≈ 6.8 à 7.2 / 10).
    - Jusqu'à Knossos 1 inclus (TP1 à TP6E4, carte "B09" ou "B09bis") ≈ 16 / 20 au total (Axe 1 ≈ 8.5 à 8.8 / 10).
    - Jusqu'à Knossos 2 inclus (TP1 à TP6E5, carte "B09" ou "B09bis") ≈ 18+ / 20 au total (Axe 1 ≈ 9.6 à 10.0 / 10).
  • "B09" et "B09bis" sont acceptés indifféremment pour Knossos 1 et Knossos 2.

- Axe 2 : Architecture & Qualité Algorithmique (sur 8.0 pts)
  Examine la compréhension réelle plutôt que l'alignement de syntaxe :
  • Découpage modulaire : fonctions pertinentes, paramètres, valeur de retour utile.
  • Logique algorithmique et compréhension réelle des concepts :
    - Vérifie la maîtrise réelle du temps (ex: dans TP5E1, calcul d'une vraie durée écoulée `t2 - t1`).
    - Vérifie la gestion de l'aléa (`initRandom()` appelé une seule fois au début).
    - Vérifie l'usage des tableaux et de la moyenne pondérée (`float`) dans TP6.

- Axe 3 : Style, Rigueur & Bonnes Pratiques (sur 2.0 pts)
  Lisibilité, nommage clair des variables et constantes en MAJUSCULES, documentation des fonctions, indentation propre.
  Vérifie l'absence d'anti-patterns : waitFor bloquant moteur allumé, waitNumpad sans relâchement, troncature de float.
  Note : 'void main()' ainsi que les bibliothèques standards C (<stdio.h>, printf, <stdlib.h>, <stdbool.h>, <math.h>) sont TOLÉRÉS SANS AUCUNE PÉNALITÉ.

- Note sans malus /20 = Axe 1 + Axe 2 + Axe 3 (évaluation purement technique hors malus IA / similarité).
  La Note finale avec malus sera calculée automatiquement à partir des pénalités d'intégrité.

FORMAT DE SORTIE IMPÉRATIF :
1. Au tout début de ta réponse (avant tout autre texte ou titre), insère OBLIGATOIREMENT le bloc JSON suivant délimité exactement par ```eval_scores ... ``` :
```eval_scores
{{
  "axe1_progression": <float sur 10.0>,
  "axe2_architecture": <float sur 8.0>,
  "axe3_style": <float sur 2.0>,
  "total_note": <float sur 20.0, somme technique des 3 axes>,
  "ai_assistance": {{
    "level": "<Faible / Modéré / Élevé>",
    "justification": "<Explication factuelle basée sur le code de l'élève>"
  }},
  "profil": "<Profil pédagogique, ex: Consolidation maîtrisée / Investi>",
  "appreciation": "<1 ou 2 phrases résumant l'évaluation globale>"
}}
```

2. Rédige ensuite les 4 SECTIONS PÉDAGOGIQUES en Markdown avec les titres suivants EXACTEMENT :
### Bilan de Progression & Analyse Pédagogique
(analyse de l'assiduité, jalons validés, régularité des dates, et compilation Clang en rappelant que void main() est autorisé sans malus)

### Points Forts Observés
(valoriser l'effort, la modularité et les initiatives réelles en citant des extraits réels du code de l'élève dans des blocs ```c ... ```)

### Axes d'Amélioration & Conseils Méthodologiques
(expliquer de façon socratique sans donner la solution brute, cibler les erreurs de compilation ou anti-patterns avec blocs ```c ... ```, et rappeler l'importance du travail personnel si IA ou similarité détectée)

### Commentaire de l'Enseignant
(mot personnalisé et encourageant de l'enseignant pour la progression de l'étudiant)
"""

        llm_text = call_gemini_api(api_key, model, prompt, ai_props=ai_props)
        if llm_text:
            scores_data, _ = extract_eval_scores(llm_text)
            if scores_data:
                try:
                    det_a1 = float(res["scores"]["axe1_progression"])
                    det_a2 = float(res["scores"]["axe2_architecture"])
                    det_a3 = float(res["scores"]["axe3_style"])

                    a1 = det_a1
                    llm_a2 = float(scores_data.get("axe2_architecture", det_a2))
                    llm_a3 = float(scores_data.get("axe3_style", det_a3))

                    a2 = max(0.0, min(8.0, max(det_a2 - 1.0, min(det_a2 + 0.4, llm_a2))))
                    a3 = max(0.0, min(2.0, max(det_a3 - 0.4, min(det_a3 + 0.2, llm_a3))))

                    note_sans_malus = round(a1 + a2 + a3, 1)
                    t_note = note_sans_malus

                    # Application des malus d'intégrité (IA + Similarité) uniquement sur la note avec malus (total_note)
                    ai_prob = res.get("ai_analysis", {}).get("probability_pct", 0)
                    if ai_prob >= 60:
                        t_note = min(12.0, max(0.0, round(t_note - 2.0, 1)))
                    elif ai_prob >= 30:
                        t_note = min(14.0, max(0.0, round(t_note - 1.0, 1)))

                    res["scores"]["note_apres_ia"] = round(t_note, 1)
                    sim_malus = float(res["scores"].get("malus_similarite", 0.0))
                    if sim_malus > 0.0:
                        t_note = max(0.0, round(t_note - sim_malus, 1))
                    t_note = max(t_note, round(note_sans_malus - 3.5, 1))

                    res["scores"]["note_technique_brute"] = note_sans_malus
                    res["scores"]["total_note"] = round(t_note, 1)
                    res["scores"]["axe1_progression"] = round(a1, 1)
                    res["scores"]["axe2_architecture"] = round(a2, 1)
                    res["scores"]["axe3_style"] = round(a3, 1)
                    if "axe4_tests" in res["scores"]:
                        del res["scores"]["axe4_tests"]
                    if "profil" in scores_data:
                        res["profil"] = scores_data["profil"]
                    if "ai_assistance" in scores_data and isinstance(scores_data["ai_assistance"], dict):
                        ai_obj = scores_data["ai_assistance"]
                        res.setdefault("ai_analysis", {})["level"] = ai_obj.get("level", res.get("ai_analysis", {}).get("level", "Faible"))
                        res["ai_analysis"]["justification"] = ai_obj.get("justification", "")
                    print(f"[DONE] Note calibrée par Gemini : Sans malus={note_sans_malus}/20 | Avec malus={res['scores']['total_note']}/20 (Axe1={res['scores']['axe1_progression']}/10, Axe2={res['scores']['axe2_architecture']}/8, Axe3={res['scores']['axe3_style']}/2)", flush=True)
                except Exception as e:
                    print(f"[WARN] Impossible de parser les scores calibrés : {e}", flush=True)

            # Assemblage canonique strict
            clean_markdown = build_canonical_introprog_report(res, identity, eval_date_str, skill_meta, raw_llm_text=llm_text, plagiarism_info=plagiarism_info)
            clean_markdown = normalize_markdown_newlines(clean_markdown)
            os.makedirs(os.path.dirname(os.path.abspath(output_file)), exist_ok=True)
            if evaluated_results is None:
                print("[PROGRESS] 3/3 Rédaction de la fiche pédagogique...", flush=True)
            with open(output_file, "w", encoding="utf-8") as f:
                f.write(clean_markdown)
            print(f"[DONE] Fiche pédagogique enregistrée : {os.path.basename(output_file)}", flush=True)
            if evaluated_results is not None:
                evaluated_results.append(res)
            return output_file

    # Fallback déterministe
    if evaluated_results is None:
        print("[PROGRESS] 2/2 Génération fiche déterministe...", flush=True)
    print("[INFO] Génération de la fiche via le moteur déterministe...", flush=True)
    os.makedirs(os.path.dirname(os.path.abspath(output_file)), exist_ok=True)
    res["eval_engine"] = "Moteur Déterministe CodeLab"
    clean_markdown = build_canonical_introprog_report(res, identity, eval_date_str, skill_meta, plagiarism_info=plagiarism_info)
    clean_markdown = normalize_markdown_newlines(clean_markdown)
    with open(output_file, "w", encoding="utf-8") as f:
        f.write(clean_markdown)
    print(f"[DONE] Fiche enregistrée : {os.path.basename(output_file)}", flush=True)
    if evaluated_results is not None:
        evaluated_results.append(res)
    return output_file

def evaluate_single_python_folder(student_dir, output_file, ai_props=None, student_login=None, student_name=None, sessions_xml=None, evaluated_results=None, plagiarism_info=None):
    """Évalue un dossier unique en Python (eval-intropython) avec gabarit strict et analyses d'intégrité."""
    identity = resolve_student_identity(student_dir, student_login=student_login, student_name=student_name, sessions_xml_path=sessions_xml)
    print(f"[INFO] Analyse du code Python & Numpy : {identity['student_name']} ({identity['student_id']})", flush=True)
    intropython_dir = get_skill_dir("eval-intropython") or INTROYTHON_DIR
    sys.path.insert(0, str(intropython_dir / "scripts"))
    try:
        from audit_student import audit_student
    except ImportError as e:
        print(f"[FAIL] Impossible d'importer le skill Python: {e}", file=sys.stderr, flush=True)
        return None

    print("[INFO] Audit des scripts TP4, expressions régulières et calculs Numpy...", flush=True)
    data = audit_student(str(student_dir))

    data["student_id"] = identity["student_id"]
    data["student_name"] = identity["student_name"]
    data["assignment"] = identity["assignment"]

    # Détection automatique de similarité/plagiat avec les voisins de promo si non fournie
    if plagiarism_info is None:
        plagiarism_info = find_single_student_plagiarism(student_dir, identity['student_id'])
    if plagiarism_info:
        data["plagiarism_alert"] = plagiarism_info
        if "note_technique_brute" not in data:
            data["note_technique_brute"] = round(float(data.get("score_p1", 0.0)) + float(data.get("score_p2", 0.0)) + float(data.get("score_transversal", 0.0)), 2)
        if "note_apres_ia" not in data:
            data["note_apres_ia"] = float(data.get("total", data["note_technique_brute"]))
        nb_sim = len(plagiarism_info.get("files", []))
        max_sim = float(plagiarism_info.get("max_sim", 0.0))
        sim_malus = 0.0
        if nb_sim >= 2 or max_sim >= 95.0:
            sim_malus = 2.0
        elif max_sim >= 85.0:
            sim_malus = 1.0
        elif max_sim >= 75.0:
            sim_malus = 0.5
        if sim_malus > 0.0:
            data["malus_similarite"] = sim_malus
            tot = float(data["note_apres_ia"]) - sim_malus
            if "note_technique_brute" in data:
                tot = max(tot, float(data["note_technique_brute"]) - 3.5)
            data["total"] = max(0.0, round(tot, 2))

    note = data.get('total', 'N/A')
    note_sans_malus = data.get('note_technique_brute', note)
    p1 = data.get('score_p1', 0)
    p2 = data.get('score_p2', 0)
    print(f"[DONE] Audit statique achevé : P1={p1}/8 | P2={p2}/10 | Sans malus={note_sans_malus}/20 | Avec malus={note}/20", flush=True)

    # Affichage des 3 piliers d'intégrité académique à la console
    integ = extract_integrity_indicators(data, plagiarism_info=plagiarism_info)
    print(f"[INFO] Chronométrie : {integ['chronometrie_courte']}", flush=True)
    print(f"[INFO] Stylométrie : {integ['stylometrie_courte']}", flush=True)
    if "🚨" in integ['similarite_courte'] or "⚠️" in integ['similarite_courte']:
        print(f"[WARN] Similarité / Plagiat : {integ['similarite_courte']}", flush=True)
    else:
        print(f"[DONE] Similarité / Plagiat : {integ['similarite_courte']}", flush=True)

    skill_meta = get_skill_metadata(intropython_dir or "eval-intropython")
    eval_date_str = datetime.now().strftime("%d/%m/%Y à %H:%M")
    data["eval_date"] = eval_date_str
    data["skill_last_modified"] = skill_meta["last_modified"]
    data["skill_name"] = skill_meta["name"]
    data["skill_version"] = skill_meta["version"]

    s_name = identity["student_name"]
    s_id = identity["student_id"]
    s_group = identity.get("student_group", "")
    group_line = f"\n- **Groupe :** {s_group}" if s_group else ""
    try:
        note_f = float(note)
        nsm_f = float(note_sans_malus)
        profil = "Remarquable / Excellent" if note_f >= 16 else ("Bien" if note_f >= 13 else ("Passable" if note_f >= 10 else "Insuffisant"))
    except Exception:
        note_f = 0.0
        nsm_f = 0.0
        profil = "Standard"

    scores_table = f"""| Axe d'Évaluation | Note Attribuée | Barème Officiel | Appréciation Synthétique |
| :--- | :---: | :---: | :--- |
| **Partie 1 : Flux & Regex** | **{p1:.2f}** | / 8.0 | Traitement des flux de données et expressions régulières |
| **Partie 2 : Numpy & Matplotlib** | **{p2:.2f}** | / 10.0 | Calculs matriciels vectorisés et visualisations |
| **Transversal : Style Pythonique** | **{data.get('score_transversal', 0.0):.2f}** | / 2.0 | Idiomes Python, propreté et bonnes pratiques |
| **Note sans malus (IA / Similarité)** | **{nsm_f:.2f}** | **/ 20.0** | **Somme technique brute des 3 axes** |
| **TOTAL GÉNÉRAL (Avec malus)** | **{note_f:.2f}** | **/ 20.0** | **{profil}** |"""

    api_key = ai_props.get("GEMINI_API_KEY") if ai_props else None
    model = ai_props.get("GEMINI_MODEL", "gemini-3.5-flash-lite") if ai_props else "gemini-3.5-flash-lite"

    sec_bilan = ""
    sec_points_forts = ""
    sec_ameliorations = ""
    sec_commentaire = ""

    if api_key:
        data["eval_engine"] = f"LLM Gemini ({model})"
        print(f"[INFO] Rédaction socratique personnalisée par LLM ({model})...", flush=True)
        prompt = f"""Tu es Jérôme Lehuen, enseignant-chercheur à Le Mans Université.
Tu évalues les travaux pratiques du TP4 sommatif pour le module 'Introduction à Python et à Numpy' (L2 Informatique).

Voici le diagnostic factuel extrait par l'analyseur automatique :
```json
{json.dumps(data, indent=2, ensure_ascii=False)}
```

Rédige une fiche d'évaluation individuelle bienveillante, pédagogique et rigoureuse en Markdown avec les 4 sections obligatoires suivantes :
### Bilan de Progression & Analyse Pédagogique
(analyse de l'assiduité, de la Partie 1 flux/regex et Partie 2 numpy/matplotlib, et respect du style pythonique)

### Points Forts Observés
(valoriser l'effort et la vectorisation en citant des extraits réels du code de l'élève dans des blocs ```python ... ```)

### Axes d'Amélioration & Conseils Méthodologiques
(chasse aux anti-patterns comme les boucles for Numpy inutiles ou re.compile dans une boucle, questions socratiques avec blocs ```python ... ```)

### Commentaire de l'Enseignant
(mot personnalisé et encourageant de l'enseignant pour la poursuite d'études en L3)
"""
        llm_text = call_gemini_api(api_key, model, prompt, ai_props=ai_props)
        if llm_text:
            sec_bilan, sec_points_forts, sec_ameliorations, sec_commentaire = extract_pedagogical_sections(llm_text)

    if not sec_bilan:
        data["eval_engine"] = "Moteur Déterministe CodeLab"
        sec_bilan, sec_points_forts, sec_ameliorations, sec_commentaire = build_deterministic_pedagogical_sections(
            "eval-intropython", data, identity
        )

    template_vars = {
        "sujet": data.get("sujet", 1),
        "nom": s_name,
        "student_name": s_name,
        "login": s_id,
        "student_id": s_id,
        "groupe": s_group,
        "student_group": s_group,
        "group_line": group_line,
        "profil": profil,
        "eval_date": eval_date_str,
        "skill_name": skill_meta.get("name", "eval-intropython"),
        "skill_last_modified": skill_meta.get("last_modified", ""),
        "eval_engine": data.get("eval_engine", "Moteur Déterministe CodeLab"),
        "ai_level": data.get("ai_analysis", {}).get("level", "Faible (0% - Code authentique)"),
        "scores_table": scores_table,
        "chronometrie": integ["chronometrie"],
        "chronometrie_courte": integ["chronometrie_courte"],
        "stylometrie": integ["stylometrie"],
        "stylometrie_courte": integ["stylometrie_courte"],
        "similarite": integ["similarite"],
        "similarite_courte": integ["similarite_courte"],
        "analyse_integrite": integ["analyse_integrite"],
        "bilan": sec_bilan,
        "points_forts": sec_points_forts,
        "axes_amelioration": sec_ameliorations,
        "commentaire": sec_commentaire,
    }

    template_candidates = [
        intropython_dir / "report_template.md" if intropython_dir else None,
        Path("/Users/lehuen/codelab.files/skills/eval-intropython/report_template.md")
    ]
    report_text = render_template_if_exists(template_candidates, template_vars)
    if not report_text:
        report_text = f"""# Fiche d'Évaluation Individuelle — TP n°4 (Sujet {data.get('sujet', 1)})

### Étudiant : **{s_name}**

- **Login / ID :** `{s_id}`
- **Module :** Introduction à Python et à Numpy (L2 Informatique){group_line}
- **Profil :** {profil}

---

### 📋 Cartouche de Traçabilité & Synthèse d'Intégrité

- **Date de l'évaluation :** {eval_date_str}
- **Skill appliqué :** `{skill_meta['name']}` (dernière modification : {skill_meta['last_modified']})
- **Moteur d'évaluation :** {data.get('eval_engine', 'Moteur Déterministe CodeLab')}
- **Chronométrie :** {integ['chronometrie_courte']}
- **Stylométrie d'indentation :** {integ['stylometrie_courte']}
- **Test de similarité (Plagiat) :** {integ['similarite_courte']}
- **Indice d'assistance IA :** {data.get('ai_analysis', {}).get('level', 'Faible (0% - Code authentique)')}
- **Environnement :** CodeLab v1.5.0

---

### Tableau Récapitulatif des Notes Calibrées

{scores_table}

---

{integ['analyse_integrite']}

---

### Bilan de Progression & Analyse Pédagogique

{sec_bilan}

---

### Points Forts Observés

{sec_points_forts}

---

### Axes d'Amélioration & Conseils Méthodologiques

{sec_ameliorations}

---

### Commentaire de l'Enseignant

{sec_commentaire}
"""

    report_text = normalize_markdown_newlines(report_text)
    os.makedirs(os.path.dirname(os.path.abspath(output_file)), exist_ok=True)
    with open(output_file, "w", encoding="utf-8") as f:
        f.write(report_text)
    print(f"[DONE] Fiche enregistrée : {os.path.basename(output_file)}", flush=True)
    if evaluated_results is not None:
        evaluated_results.append(data)
    return output_file

def evaluate_generic_folder(student_dir, output_file, sessions_xml=None, ai_props=None, skill_dir=None, student_login=None, student_name=None, evaluated_results=None, plagiarism_info=None):
    """Évalue un dossier unique en utilisant un skill dynamique (ex: eval-c-sample, eval-python-sample, eval-java-sample)."""
    if not skill_dir:
        return None
    skill_name = skill_dir.name
    identity = resolve_student_identity(student_dir, student_login=student_login, student_name=student_name, sessions_xml_path=sessions_xml)
    print(f"[INFO] Analyse avec le skill '{skill_name}' : {identity['student_name']} ({identity['student_id']})", flush=True)
    scripts_dir = skill_dir / "scripts"
    if not scripts_dir.is_dir():
        print(f"[FAIL] Dossier scripts/ introuvable pour le skill {skill_name}", file=sys.stderr)
        return None

    sys.path.insert(0, str(scripts_dir))
    try:
        import audit_student
    except ImportError as e:
        print(f"[FAIL] Erreur d'importation dans {scripts_dir}: {e}", file=sys.stderr)
        return None

    audit_func = getattr(audit_student, "audit_student", getattr(audit_student, "analyze_student", None))
    if not audit_func:
        print(f"[FAIL] Fonction d'audit introuvable dans {skill_name}/scripts/audit_student.py", file=sys.stderr)
        return None

    try:
        if "sessions_xml" in audit_func.__code__.co_varnames:
            data = audit_func(str(student_dir), sessions_xml)
        else:
            data = audit_func(str(student_dir))
    except Exception as e:
        print(f"[FAIL] Exception pendant l'audit: {e}", file=sys.stderr)
        return None

    data["student_id"] = identity["student_id"]
    data["student_name"] = identity["student_name"]
    data["assignment"] = identity["assignment"]

    # Détection automatique de similarité/plagiat avec les voisins de promo si non fournie
    if plagiarism_info is None:
        plagiarism_info = find_single_student_plagiarism(student_dir, identity['student_id'])
    if plagiarism_info:
        data["plagiarism_alert"] = plagiarism_info
        sc_g = data.get("scores", {})
        if "note_apres_ia" not in sc_g:
            sc_g["note_apres_ia"] = float(sc_g.get("total", data.get("total", 0.0)))
        nb_sim = len(plagiarism_info.get("files", []))
        max_sim = float(plagiarism_info.get("max_sim", 0.0))
        sim_malus = 0.0
        if nb_sim >= 3 or max_sim >= 95.0:
            sim_malus = 2.0
        elif nb_sim >= 2 or max_sim >= 85.0:
            sim_malus = 1.0
        elif nb_sim >= 1 and max_sim >= 75.0:
            sim_malus = 0.5
        if sim_malus > 0.0:
            sc_g["malus_similarite"] = sim_malus
            tot = float(sc_g["note_apres_ia"]) - sim_malus
            if "note_technique_brute" in sc_g:
                tot = max(tot, float(sc_g["note_technique_brute"]) - 3.5)
            sc_g["total"] = max(0.0, round(tot, 1))

    scores = data.get("scores", {})
    total = scores.get("total", data.get("total", "N/A"))
    note_sans_malus = scores.get("note_technique_brute", total)
    print(f"[DONE] Audit achevé | Note sans malus : {note_sans_malus}/20 | Note avec malus : {total}/20", flush=True)

    # Affichage des 3 piliers d'intégrité académique à la console
    integ = extract_integrity_indicators(data, plagiarism_info=plagiarism_info)
    print(f"[INFO] Chronométrie : {integ['chronometrie_courte']}", flush=True)
    print(f"[INFO] Stylométrie : {integ['stylometrie_courte']}", flush=True)
    if "🚨" in integ['similarite_courte'] or "⚠️" in integ['similarite_courte']:
        print(f"[WARN] Similarité / Plagiat : {integ['similarite_courte']}", flush=True)
    else:
        print(f"[DONE] Similarité / Plagiat : {integ['similarite_courte']}", flush=True)

    skill_meta = get_skill_metadata(skill_dir)
    eval_date_str = datetime.now().strftime("%d/%m/%Y à %H:%M")
    data["eval_date"] = eval_date_str
    data["skill_last_modified"] = skill_meta["last_modified"]
    data["skill_name"] = skill_meta["name"]
    data["skill_version"] = skill_meta["version"]

    s_name = identity["student_name"]
    s_id = identity["student_id"]
    s_group = identity.get("student_group", "")
    group_line = f"\n- **Groupe :** {s_group}" if s_group else ""
    try:
        t_val = float(total)
        profil = "Remarquable / Excellent" if t_val >= 16 else ("Bien" if t_val >= 13 else ("Passable" if t_val >= 10 else "Insuffisant"))
    except Exception:
        profil = "Standard"

    scores_table = build_generic_scores_table(scores, data, profil)

    api_key = ai_props.get("GEMINI_API_KEY") if ai_props else None
    model = ai_props.get("GEMINI_MODEL", "gemini-3.5-flash-lite") if ai_props else "gemini-3.5-flash-lite"

    sec_bilan = ""
    sec_points_forts = ""
    sec_ameliorations = ""
    sec_commentaire = ""

    if api_key:
        data["eval_engine"] = f"LLM Gemini ({model})"
        print(f"[INFO] Rédaction pédagogique par LLM ({model})...", flush=True)
        skill_md = skill_dir / "SKILL.md"
        instructions = ""
        if skill_md.is_file():
            try:
                instructions = skill_md.read_text(encoding="utf-8")
            except Exception:
                pass

        prompt = f"""Tu es un enseignant bienveillant et rigoureux évaluant les travaux d'un étudiant dans CodeLab.
Voici la spécification du skill d'évaluation utilisé :
{instructions}

Voici le diagnostic factuel extrait par l'analyseur automatique :
```json
{json.dumps(data, indent=2, ensure_ascii=False)}
```

Rédige une fiche d'évaluation individuelle bienveillante, pédagogique et structurée en Markdown avec les 4 sections obligatoires suivantes :
### Bilan de Progression & Analyse Pédagogique
(analyse synthétique du travail, de la compilation et des réalisations)

### Points Forts Observés
(valoriser concrètement les réussites en citant des extraits réels du code de l'élève si disponibles dans snippets.highlights)

### Axes d'Amélioration & Conseils Méthodologiques
(conseils socratiques sans donner directement le code solution, ciblant les erreurs de compilation ou anti-patterns de snippets)

### Commentaire de l'Enseignant
(mot personnalisé et encourageant de l'enseignant pour la progression de l'étudiant)
"""
        llm_text = call_gemini_api(api_key, model, prompt, ai_props=ai_props)
        if llm_text:
            sec_bilan, sec_points_forts, sec_ameliorations, sec_commentaire = extract_pedagogical_sections(llm_text)

    if not sec_bilan:
        data["eval_engine"] = "Moteur Déterministe CodeLab"
        sec_bilan, sec_points_forts, sec_ameliorations, sec_commentaire = build_deterministic_pedagogical_sections(
            skill_name, data, identity
        )

    template_vars = {
        "nom": s_name,
        "student_name": s_name,
        "login": s_id,
        "student_id": s_id,
        "groupe": s_group,
        "student_group": s_group,
        "group_line": group_line,
        "profil": profil,
        "eval_date": eval_date_str,
        "skill_name": skill_meta.get("name", skill_name),
        "skill_last_modified": skill_meta.get("last_modified", ""),
        "eval_engine": data.get("eval_engine", "Moteur Déterministe CodeLab"),
        "ai_level": data.get("ai_analysis", {}).get("level", "Faible (0% - Code authentique)"),
        "scores_table": scores_table,
        "chronometrie": integ["chronometrie"],
        "chronometrie_courte": integ["chronometrie_courte"],
        "stylometrie": integ["stylometrie"],
        "stylometrie_courte": integ["stylometrie_courte"],
        "similarite": integ["similarite"],
        "similarite_courte": integ["similarite_courte"],
        "analyse_integrite": integ["analyse_integrite"],
        "bilan": sec_bilan,
        "points_forts": sec_points_forts,
        "axes_amelioration": sec_ameliorations,
        "commentaire": sec_commentaire,
    }

    template_candidates = [
        skill_dir / "report_template.md" if skill_dir else None,
        Path(f"/Users/lehuen/codelab.files/skills/{skill_name}/report_template.md")
    ]
    report_text = render_template_if_exists(template_candidates, template_vars)
    if not report_text:
        report_text = f"""# Fiche d'Évaluation : {skill_meta.get('title', skill_name)}

### Étudiant : **{s_name}**

- **Login / ID :** `{s_id}`
- **Module :** {skill_meta.get('title', skill_name)}{group_line}
- **Profil :** {profil}

---

### 📋 Cartouche de Traçabilité & Synthèse d'Intégrité

- **Date de l'évaluation :** {eval_date_str}
- **Skill appliqué :** `{skill_meta['name']}` (dernière modification : {skill_meta['last_modified']})
- **Moteur d'évaluation :** {data.get('eval_engine', 'Moteur Déterministe CodeLab')}
- **Chronométrie :** {integ['chronometrie_courte']}
- **Stylométrie d'indentation :** {integ['stylometrie_courte']}
- **Test de similarité (Plagiat) :** {integ['similarite_courte']}
- **Indice d'assistance IA :** {data.get('ai_analysis', {}).get('level', 'Faible (0% - Code authentique)')}
- **Environnement :** CodeLab v1.5.0

---

### Tableau Récapitulatif des Notes Calibrées

{scores_table}

---

{integ['analyse_integrite']}

---

### Bilan de Progression & Analyse Pédagogique

{sec_bilan}

---

### Points Forts Observés

{sec_points_forts}

---

### Axes d'Amélioration & Conseils Méthodologiques

{sec_ameliorations}

---

### Commentaire de l'Enseignant

{sec_commentaire}
"""

    report_text = normalize_markdown_newlines(report_text)
    os.makedirs(os.path.dirname(os.path.abspath(output_file)), exist_ok=True)
    with open(output_file, "w", encoding="utf-8") as f:
        f.write(report_text)
    print(f"[DONE] Fiche enregistrée : {os.path.basename(output_file)}", flush=True)
    if evaluated_results is not None:
        evaluated_results.append(data)
    return output_file

def evaluate_folder(target_dir, out_file, sessions_xml=None, ai_props=None, skill_name=None, student_login=None, student_name=None, plagiarism_info=None):
    """Point d'entrée universel pour évaluer un dossier selon un skill donné ou auto-détecté."""
    skill_dir = resolve_skill(skill_name, target_dir)
    if not skill_dir:
        print(f"[FAIL] Impossible de résoudre un skill pour le dossier {target_dir}", file=sys.stderr)
        return None
    print(f"[INFO] Skill sélectionné : {skill_dir.name}", flush=True)
    if skill_dir.name == "eval-introprog":
        return evaluate_single_c_folder(target_dir, out_file, sessions_xml, ai_props, student_login=student_login, student_name=student_name, plagiarism_info=plagiarism_info)
    elif skill_dir.name == "eval-intropython":
        return evaluate_single_python_folder(target_dir, out_file, ai_props, student_login=student_login, student_name=student_name, sessions_xml=sessions_xml, plagiarism_info=plagiarism_info)
    else:
        return evaluate_generic_folder(target_dir, out_file, sessions_xml, ai_props, skill_dir, student_login=student_login, student_name=student_name, plagiarism_info=plagiarism_info)

def evaluate_session(session_id, session_dir, eval_output_dir=None, sessions_xml=None, ai_props=None, skill_name=None):
    """Évalue l'ensemble d'une session d'étudiants (cohorte)."""
    skill_dir = resolve_skill(skill_name, session_dir)
    if not skill_dir:
        print(f"[FAIL] Impossible de résoudre un skill pour la session {session_dir}", file=sys.stderr)
        return None
    print(f"[INFO] Évaluation de session avec le skill : {skill_dir.name}", flush=True)

    session_p = Path(session_dir)
    eval_dir = Path(eval_output_dir) if eval_output_dir else (session_p / "evaluations")
    eval_dir.mkdir(parents=True, exist_ok=True)
    
    csv_file = eval_dir / f"notes_{session_id}.csv"
    synthese_file = eval_dir / f"synthese_{session_id}.md"

    users_db = load_sessions_users(sessions_xml, search_dirs=[session_p])

    if skill_dir.name == "eval-introprog":
        introprog_dir = get_skill_dir("eval-introprog") or INTROPROG_DIR
        sys.path.insert(0, str(introprog_dir / "scripts"))
        plag_map = {}
        try:
            import audit_cohort
            if hasattr(audit_cohort, "detect_cohort_plagiarism"):
                plag_res = audit_cohort.detect_cohort_plagiarism(str(session_p), users_db=users_db)
                if isinstance(plag_res, tuple):
                    if len(plag_res) == 2 and isinstance(plag_res[1], dict):
                        plag_map = plag_res[1]
                    else:
                        plag_res = plag_res[0]
                if not plag_map and isinstance(plag_res, list):
                    for sp in plag_res:
                        for sid, partner in ((sp.get("s1"), sp.get("s2")), (sp.get("s2"), sp.get("s1"))):
                            if not sid:
                                continue
                            if sid not in plag_map:
                                plag_map[sid] = {
                                    "partner": partner,
                                    "max_sim": sp.get("max_sim", 0),
                                    "files": list(sp.get("similar_files", [])),
                                    "severity": sp.get("severity", "")
                                }
                            else:
                                existing_files = set(plag_map[sid].get("files", []))
                                for sf in sp.get("similar_files", []):
                                    if sf not in existing_files:
                                        plag_map[sid]["files"].append(sf)
                                        existing_files.add(sf)
                                if sp.get("max_sim", 0) > plag_map[sid].get("max_sim", 0):
                                    plag_map[sid]["max_sim"] = sp.get("max_sim", 0)
                                    plag_map[sid]["partner"] = partner
                                    plag_map[sid]["severity"] = sp.get("severity", "")
        except Exception:
            pass
        evaluated_results = []
        eligible_items = [
            item for item in sorted(session_p.iterdir())
            if item.is_dir() and item.name not in ("evaluations", "codelab", ".hidden", "recovery", "__pycache__")
            and not is_tutor_user(item.name, users_db.get(item.name))
        ]
        total_students = len(eligible_items)
        for idx, item in enumerate(eligible_items, start=1):
            u_info = users_db.get(item.name)
            student_eval_dir = eval_dir / item.name
            student_eval_dir.mkdir(parents=True, exist_ok=True)
            fiche_path = student_eval_dir / f"{item.name}_rapport.md"
            s_name = u_info.get("name") if u_info else None
            display_name = f"{s_name} ({item.name})" if s_name and s_name != item.name else item.name
            print(f"[PROGRESS] {idx}/{total_students} {display_name}", flush=True)
            evaluate_single_c_folder(
                item, fiche_path, sessions_xml, ai_props,
                student_login=item.name, student_name=s_name,
                evaluated_results=evaluated_results,
                plagiarism_info=plag_map.get(item.name)
            )
        if evaluated_results:
            try:
                import audit_cohort
                export_csv = getattr(audit_cohort, "export_csv", None)
                export_summary_markdown = getattr(audit_cohort, "export_summary_markdown", None)
                use_ai_flag = bool(ai_props and ai_props.get("GEMINI_API_KEY"))
                if export_csv and export_summary_markdown:
                    export_csv(evaluated_results, str(csv_file), cohort_dir=str(session_p))
                    try:
                        export_summary_markdown(evaluated_results, str(synthese_file), cohort_dir=str(session_p), use_ai=use_ai_flag, session_id=session_id)
                    except TypeError:
                        export_summary_markdown(evaluated_results, str(synthese_file), str(session_p))
                    print(f"[DONE] Synthèse de session et export CSV générés avec les notes calibrées et détection de similarités !", flush=True)
                else:
                    try:
                        audit_cohort.audit_cohort(str(session_dir), output_csv=str(csv_file), output_md=str(synthese_file), sessions_xml=sessions_xml, use_ai=use_ai_flag)
                    except TypeError:
                        audit_cohort.audit_cohort(str(session_dir), output_csv=str(csv_file), output_md=str(synthese_file), sessions_xml=sessions_xml)
            except Exception as e:
                print(f"[WARN] Erreur lors de la génération de synthèse : {e}", flush=True)
    elif skill_dir.name == "eval-intropython":
        intropython_dir = get_skill_dir("eval-intropython") or INTROYTHON_DIR
        sys.path.insert(0, str(intropython_dir / "scripts"))
        plag_map = {}
        try:
            import audit_cohort
            if hasattr(audit_cohort, "detect_cohort_plagiarism"):
                plag_res = audit_cohort.detect_cohort_plagiarism(str(session_p))
                if isinstance(plag_res, tuple):
                    if len(plag_res) == 2 and isinstance(plag_res[1], dict):
                        plag_map = plag_res[1]
                    else:
                        plag_res = plag_res[0]
                if not plag_map and isinstance(plag_res, list):
                    for sp in plag_res:
                        for sid, partner in ((sp.get("s1"), sp.get("s2")), (sp.get("s2"), sp.get("s1"))):
                            if not sid:
                                continue
                            if sid not in plag_map:
                                plag_map[sid] = {
                                    "partner": partner,
                                    "max_sim": sp.get("max_sim", 0),
                                    "files": list(sp.get("similar_files", [])),
                                    "severity": sp.get("severity", "")
                                }
                            else:
                                existing_files = set(plag_map[sid].get("files", []))
                                for sf in sp.get("similar_files", []):
                                    if sf not in existing_files:
                                        plag_map[sid]["files"].append(sf)
                                        existing_files.add(sf)
                                if sp.get("max_sim", 0) > plag_map[sid].get("max_sim", 0):
                                    plag_map[sid]["max_sim"] = sp.get("max_sim", 0)
                                    plag_map[sid]["partner"] = partner
                                    plag_map[sid]["severity"] = sp.get("severity", "")
        except Exception:
            pass
        from audit_cohort import analyze_cohort, export_csv, export_summary_markdown
        results = analyze_cohort(str(session_dir))
        export_csv(results, str(csv_file), cohort_dir=str(session_p))
        use_ai_flag = bool(ai_props and ai_props.get("GEMINI_API_KEY"))
        try:
            export_summary_markdown(results, str(synthese_file), cohort_dir=str(session_p), use_ai=use_ai_flag, session_id=session_id)
        except TypeError:
            export_summary_markdown(results, str(synthese_file), cohort_dir=str(session_p))
        eligible_items = [
            item for item in sorted(session_p.iterdir())
            if item.is_dir() and item.name not in ("evaluations", ".hidden", "recovery", "codelab")
            and not is_tutor_user(item.name, users_db.get(item.name))
        ]
        total_students = len(eligible_items)
        for idx, item in enumerate(eligible_items, start=1):
            u_info = users_db.get(item.name)
            student_eval_dir = eval_dir / item.name
            student_eval_dir.mkdir(parents=True, exist_ok=True)
            fiche_path = student_eval_dir / f"{item.name}_rapport.md"
            s_name = u_info.get("name") if u_info else None
            display_name = f"{s_name} ({item.name})" if s_name and s_name != item.name else item.name
            print(f"[PROGRESS] {idx}/{total_students} {display_name}", flush=True)
            evaluate_single_python_folder(item, fiche_path, ai_props, student_login=item.name, student_name=s_name, sessions_xml=sessions_xml, plagiarism_info=plag_map.get(item.name))
    else:
        sys.path.insert(0, str(skill_dir / "scripts"))
        plag_map = {}
        try:
            import audit_cohort
            if hasattr(audit_cohort, "detect_cohort_plagiarism"):
                plag_res = audit_cohort.detect_cohort_plagiarism(str(session_p))
                if isinstance(plag_res, tuple):
                    if len(plag_res) == 2 and isinstance(plag_res[1], dict):
                        plag_map = plag_res[1]
                    else:
                        plag_res = plag_res[0]
                if not plag_map and isinstance(plag_res, list):
                    for sp in plag_res:
                        for sid, partner in ((sp.get("s1"), sp.get("s2")), (sp.get("s2"), sp.get("s1"))):
                            if not sid:
                                continue
                            if sid not in plag_map:
                                plag_map[sid] = {
                                    "partner": partner,
                                    "max_sim": sp.get("max_sim", 0),
                                    "files": list(sp.get("similar_files", [])),
                                    "severity": sp.get("severity", "")
                                }
                            else:
                                existing_files = set(plag_map[sid].get("files", []))
                                for sf in sp.get("similar_files", []):
                                    if sf not in existing_files:
                                        plag_map[sid]["files"].append(sf)
                                        existing_files.add(sf)
                                if sp.get("max_sim", 0) > plag_map[sid].get("max_sim", 0):
                                    plag_map[sid]["max_sim"] = sp.get("max_sim", 0)
                                    plag_map[sid]["partner"] = partner
                                    plag_map[sid]["severity"] = sp.get("severity", "")
        except Exception:
            pass
        import audit_cohort
        if hasattr(audit_cohort, "analyze_cohort"):
            results = audit_cohort.analyze_cohort(str(session_dir))
            if hasattr(audit_cohort, "export_csv"):
                try:
                    audit_cohort.export_csv(results, str(csv_file), cohort_dir=str(session_p))
                except TypeError:
                    audit_cohort.export_csv(results, str(csv_file))
            if hasattr(audit_cohort, "export_summary_markdown"):
                use_ai_flag = bool(ai_props and ai_props.get("GEMINI_API_KEY"))
                try:
                    audit_cohort.export_summary_markdown(results, str(synthese_file), cohort_dir=str(session_p), use_ai=use_ai_flag, session_id=session_id)
                except TypeError:
                    try:
                        audit_cohort.export_summary_markdown(results, str(synthese_file), cohort_dir=str(session_p))
                    except TypeError:
                        audit_cohort.export_summary_markdown(results, str(synthese_file))
        eligible_items = [
            item for item in sorted(session_p.iterdir())
            if item.is_dir() and item.name not in ("evaluations", ".hidden", "recovery", "codelab")
            and not is_tutor_user(item.name, users_db.get(item.name))
        ]
        total_students = len(eligible_items)
        for idx, item in enumerate(eligible_items, start=1):
            u_info = users_db.get(item.name)
            student_eval_dir = eval_dir / item.name
            student_eval_dir.mkdir(parents=True, exist_ok=True)
            fiche_path = student_eval_dir / f"{item.name}_rapport.md"
            s_name = u_info.get("name") if u_info else None
            display_name = f"{s_name} ({item.name})" if s_name and s_name != item.name else item.name
            print(f"[PROGRESS] {idx}/{total_students} {display_name}", flush=True)
            evaluate_generic_folder(item, fiche_path, sessions_xml, ai_props, skill_dir, student_login=item.name, student_name=s_name, plagiarism_info=plag_map.get(item.name))

    return synthese_file

def main():
    parser = argparse.ArgumentParser(description="CodeLab AI & Deterministic Evaluator")
    parser.add_argument("--folder", help="Chemin vers un dossier unique d'étudiant à évaluer")
    parser.add_argument("--student", help="Login de l'étudiant à évaluer dans la session")
    parser.add_argument("--name", "--student-name", dest="student_name", help="Nom en clair de l'étudiant")
    parser.add_argument("--session", help="ID de la session pour audit complet de cohorte")
    parser.add_argument("--dir", help="Répertoire racine de la session")
    parser.add_argument("--eval-dir", help="Dossier de destination permanent pour les évaluations")
    parser.add_argument("--sessions", help="Chemin vers sessions.xml (optionnel)")
    parser.add_argument("--out", help="Fichier de sortie spécifique (.md)")
    parser.add_argument("--props", help="Chemin vers user.properties (optionnel)")
    parser.add_argument("--skill", help="ID du skill à appliquer (ex: eval-introprog, eval-c-sample, eval-python-sample, eval-java-sample ou auto)")
    parser.add_argument("--no-ai", "--deterministic", action="store_true", dest="no_ai", help="Exécuter l'évaluation en mode 100%% déterministe (0 crédit IA)")
    
    args = parser.parse_args()
    ai_props = load_ai_properties(args.props)
    if args.no_ai:
        ai_props["GEMINI_API_KEY"] = ""
        print("[INFO] Mode d'évaluation : Déterministe local (Sans IA — 0 crédit consommé)", flush=True)
    
    # 1. Mode dossier unique (Explorateur FileManager)
    if args.folder:
        target_dir = Path(args.folder).resolve()
        if not target_dir.is_dir():
            print(f"[FAIL] Le dossier {target_dir} n'existe pas.", file=sys.stderr)
            sys.exit(1)
            
        student_login = args.student
        out_file = args.out
        if not out_file:
            if args.eval_dir:
                eval_target_dir = Path(args.eval_dir)
                eval_target_dir.mkdir(parents=True, exist_ok=True)
                out_file = eval_target_dir / f"{target_dir.name}_rapport.md"
            elif args.dir:
                session_dir = Path(args.dir).resolve()
                try:
                    rel = target_dir.relative_to(session_dir)
                    student_login = student_login or rel.parts[0]
                    eval_student_dir = session_dir / "evaluations" / student_login
                    eval_student_dir.mkdir(parents=True, exist_ok=True)
                    out_file = eval_student_dir / f"{target_dir.name}_rapport.md"
                except ValueError:
                    out_file = target_dir / f"{target_dir.name}_rapport.md"
            else:
                out_file = target_dir / f"{target_dir.name}_rapport.md"
                
        res_path = evaluate_folder(target_dir, out_file, args.sessions, ai_props, skill_name=args.skill,
                                   student_login=student_login, student_name=args.student_name)
            
        if res_path:
            print(f"[DONE] Évaluation terminée avec succès !", flush=True)
            print(f"REPORT_FILE: {os.path.abspath(res_path)}", flush=True)
            sys.exit(0)
        else:
            sys.exit(1)

    # 2. Mode étudiant individuel dans une session
    if args.student and args.dir:
        session_dir = Path(args.dir).resolve()
        student_dir = session_dir / args.student
        if not student_dir.is_dir():
            print(f"[FAIL] Dossier étudiant {student_dir} introuvable.", file=sys.stderr, flush=True)
            sys.exit(1)
            
        users_db = load_sessions_users(args.sessions, [session_dir, session_dir.parent])
        u_info = users_db.get(args.student)
        if is_tutor_user(args.student, u_info):
            print(f"[INFO] Le compte {args.student} correspond à un tuteur/enseignant. Aucune évaluation étudiante requise.", flush=True)
            sys.exit(0)
            eval_student_dir = Path(args.eval_dir) / args.student
        else:
            eval_student_dir = session_dir / "evaluations" / args.student
        eval_student_dir.mkdir(parents=True, exist_ok=True)
        out_file = args.out or (eval_student_dir / f"{args.student}_rapport.md")
        res_path = evaluate_folder(student_dir, out_file, args.sessions, ai_props, skill_name=args.skill,
                                   student_login=args.student, student_name=args.student_name)
            
        if res_path:
            print(f"[DONE] Évaluation terminée avec succès !", flush=True)
            print(f"REPORT_FILE: {os.path.abspath(res_path)}", flush=True)
            sys.exit(0)
        else:
            sys.exit(1)

    # 3. Mode session complète
    if args.session and args.dir:
        session_dir = Path(args.dir).resolve()
        res_path = evaluate_session(args.session, session_dir, eval_output_dir=args.eval_dir, sessions_xml=args.sessions, ai_props=ai_props, skill_name=args.skill)
        if res_path:
            print(f"REPORT_FILE: {os.path.abspath(res_path)}")
            sys.exit(0)
        else:
            sys.exit(1)

    parser.print_help()
    sys.exit(1)

if __name__ == "__main__":
    main()

