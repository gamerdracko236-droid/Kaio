#!/usr/bin/env bash
set -euo pipefail

# Script para instalar ferramentas e skills 3D compativeis no PC local (Windows Git Bash / Linux / macOS)
# Nao execute dentro do APK Android; use no computador para gerar modelos VRM / OBJ / audio offline.

mkdir -p ferramentas_3d && cd ferramentas_3d

echo "=== 1. Skills de Desenvolvimento de Jogos (Apache-2.0) ==="
if command -v npx >/dev/null 2>&1; then
  npx skills add gamedev-skills/awesome-gamedev-agent-skills || true
fi

echo "=== 2. Repositorios 3D Selecionados (Sem Conflito) ==="
[ -d CharacterStudio ] || git clone --depth 1 https://github.com/M3-org/CharacterStudio.git
[ -d VRM-Addon-for-Blender ] || git clone --depth 1 https://github.com/saturday06/VRM-Addon-for-Blender.git
[ -d TripoSR ] || git clone --depth 1 https://github.com/VAST-AI-Research/TripoSR.git
[ -d Hunyuan3D-2 ] || git clone --depth 1 https://github.com/Tencent/Hunyuan3D-2.git

echo "Concluido! Use Blender 4.2+ com VRM-Addon-for-Blender ou TripoSR (ou Google Colab com docs/colab_gerar_modelos_3d.py)."
