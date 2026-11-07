#!/usr/bin/env python3
"""Check the handoff documents; this does not build or test the Minecraft mod."""

import argparse
import re
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
REQUIRED = (
    "AGENTS.md", "README.md", "CHANGELOG.md",
    "docs/PROJECT_STATUS.md", "docs/NEXT_STEPS.md",
    "docs/API_COMPATIBILITY.md", "docs/PROJECT_SPECIFICATION.md",
    "docs/MASTER_CHECKLIST.md",
)
HEADINGS = {
    "docs/PROJECT_STATUS.md": (
        "Fase atual", "Concluído", "Em desenvolvimento", "Pendente",
        "Testes e verificações executados", "Erros e limitações conhecidos",
        "Última alteração relevante e sincronização",
    ),
    "docs/NEXT_STEPS.md": (
        "Última tarefa realizada", "Próxima tarefa exata", "Arquivos importantes",
        "Dependências e problemas ainda não resolvidos", "Testes pendentes e cuidados",
    ),
}


def check(base=None):
    errors = []
    for name in REQUIRED:
        file = ROOT / name
        if not file.is_file() or not file.read_text(encoding="utf-8").strip():
            errors.append(f"Documento ausente ou vazio: {name}")

    for name, headings in HEADINGS.items():
        file = ROOT / name
        if not file.is_file():
            continue
        content = file.read_text(encoding="utf-8")
        for heading in headings:
            if f"## {heading}" not in content.splitlines():
                errors.append(f"Seção obrigatória ausente: {name}: {heading}")

    for file in [ROOT / "README.md", ROOT / "AGENTS.md", *sorted((ROOT / "docs").glob("*.md"))]:
        if not file.is_file():
            continue
        for target in re.findall(r"\[[^\]]+\]\(([^)]+)\)", file.read_text(encoding="utf-8")):
            if "://" in target or target.startswith(("#", "mailto:")):
                continue
            path = target.split("#", 1)[0]
            if path and not (file.parent / path).exists():
                errors.append(f"Link local inválido: {file.relative_to(ROOT)} -> {target}")

    if base:
        changed = set(subprocess.check_output(
            ["git", "diff", "--name-only", f"{base}...HEAD"], cwd=ROOT, text=True,
        ).splitlines())
        technical = any(
            name.startswith(("src/", "scripts/", ".github/workflows/", "gradle/"))
            or name in {"build.gradle", "settings.gradle", "gradle.properties", "gradlew", "gradlew.bat"}
            for name in changed
        )
        if technical:
            for name in ("docs/PROJECT_STATUS.md", "docs/NEXT_STEPS.md", "docs/MASTER_CHECKLIST.md"):
                if name not in changed:
                    errors.append(f"Alteração técnica sem atualização de passagem: {name}")
    return errors


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", help="Commit base para conferir atualizações de progresso")
    args = parser.parse_args()
    failures = check(args.base)
    for failure in failures:
        print(f"ERRO: {failure}")
    if failures:
        raise SystemExit(1)
    print("Continuidade documental validada. Build e testes do mod não estão incluídos.")
