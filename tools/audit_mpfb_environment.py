#!/usr/bin/env python3
"""Auditoria não destrutiva do ambiente Blender/MPFB2 no runner do GitHub Actions.

Esta etapa NÃO gera nem substitui o personagem do jogo. Ela confirma a versão
do Blender, detecta a extensão MPFB2 e registra os diretórios de recursos
disponíveis para preparar a geração do primeiro personagem GLB.
"""
from __future__ import annotations

import json
import os
import platform
import sys
from pathlib import Path

REPORT_DIR = Path("artifacts")
REPORT_DIR.mkdir(parents=True, exist_ok=True)

report = {
    "purpose": "environment_audit_only",
    "model_generated": False,
    "production_files_modified": False,
    "python_version": sys.version,
    "platform": platform.platform(),
    "home": str(Path.home()),
    "checks": {},
    "candidate_asset_directories": [],
}

try:
    import bpy  # type: ignore

    report["checks"]["blender_available"] = True
    report["blender_version"] = bpy.app.version_string
    report["blender_version_tuple"] = list(bpy.app.version)
    config_root = Path(bpy.utils.user_resource("CONFIG"))
    extension_roots = [
        config_root / "extensions",
        Path.home() / ".config" / "blender" / "4.2" / "extensions",
    ]
    candidates = []
    for root in extension_roots:
        if root.exists():
            candidates.extend(str(p) for p in root.rglob("*mpfb*") if p.is_dir())
    report["mpfb_extension_candidates"] = sorted(set(candidates))
    report["checks"]["mpfb_extension_directory_found"] = bool(candidates)

    # MPFB data packs may live outside the extension directory. Record only
    # likely locations; do not assume that a directory means assets are valid.
    asset_roots = [
        Path.home() / "Documents" / "makehuman",
        Path.home() / ".config" / "makehuman",
        Path.home() / ".local" / "share" / "makehuman",
        Path.home() / ".local" / "share" / "blender",
    ]
    for root in asset_roots:
        if root.exists():
            report["candidate_asset_directories"].append(str(root))
    report["checks"]["candidate_asset_directory_found"] = bool(
        report["candidate_asset_directories"]
    )
except Exception as exc:
    report["checks"]["blender_available"] = False
    report["error"] = repr(exc)

text = json.dumps(report, ensure_ascii=False, indent=2)
(REPORT_DIR / "mpfb-environment-report.json").write_text(text, encoding="utf-8")
(REPORT_DIR / "mpfb-environment-report.txt").write_text(
    "RELATÓRIO DE AUDITORIA DO AMBIENTE 3D\n\n" + text + "\n",
    encoding="utf-8",
)
print(text)

if not report["checks"].get("blender_available"):
    raise SystemExit("Blender não pôde ser carregado no runner.")
if not report["checks"].get("mpfb_extension_directory_found"):
    raise SystemExit("A extensão MPFB2 não foi localizada após a instalação.")
