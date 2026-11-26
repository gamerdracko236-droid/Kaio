# Guia Local de Produção 3D: Blender + VRM + CharacterStudio + Godot 4

Este guia consolida apenas ferramentas compatíveis e livres de conflito para criar ou evoluir personagens externos do projeto **A Lenda dos Caçadores do Sertão** no computador (para exportação opcional PC/Web em paralelo ao aplicativo Android nativo).

## 1. Stack Aprovada (Sem Sobreposição)
- **Criação / Otimização Anime:** VRoid Studio ou `M3-org/CharacterStudio` (MIT — otimizador de 1 clique reduzindo malhas e texturas para 1 draw call).
- **Edição e Acessórios:** Blender 4.2+ / 4.5 LTS (`blender.org/download`) + `saturday06/VRM-Addon-for-Blender` (via *Edit → Preferences → Get Extensions → VRM format*).
- **Rigging e Animações CC0:** `Mesh2Motion/mesh2motion-app`.
- **Skills de Agente IA (Opcional):** `npx skills add gamedev-skills/awesome-gamedev-agent-skills` (Apache-2.0) + apenas 1 pack de Blender conforme sua versão (`EL4CTEO/blender-skills` para 5.2+ ou `arjun988/blender-skills` para 3.x–4.x).
- **Godot 4 (Versão PC/Web paralela):** Godot 4.3+ (`Compatibility` / WebGL 2.0) + Asset `#2031` (`V-Sekai/godot-vrm`, pastas `addons/vrm` e `addons/Godot-MToon-Shader` sem renomear).

## 2. O que NÃO Misturar
- Não instalar `makehuman.git` junto com `mpfb2` (o MPFB2 já substitui o MakeHuman legado; além disso, MakeHuman tem foco realista, não anime).
- Não clonar `godotengine/godot.git` (use o binário oficial pronto).
- Não misturar `virtual-puppet-project/vpuppr` (app de VTuber com webcam, não RPG).
- Não instalar `zeveck/audiogen` sem créditos pagos da ElevenLabs.
- Não incluir pesos `OmniVoice` (CC-BY-NC) ou `XTTS-v2` (CPML) em builds comerciais.

## 3. Regras Críticas no Blender para Modelos VRM
1. **Shape Keys (`Face`):** Nunca adicione, exclua ou dê Merge (`M`) em vértices da malha `Face` (destrói expressões e LipSync).
2. **Ossos Humanoides (`J_Bip_C_*`):** Nunca renomeie ossos principais; chapéu de couro e peixeira vão como filhos via `Ctrl + P → Bone` (`J_Bip_C_Head` / `J_Bip_R_Hand`). Roupas usam `Weight Transfer` / `Data Transfer`.
3. **MToon e Spring Bones:** Ajuste sombras e contorno em `Material Properties → VRM MToon 1.0`, e física de cabelo/fitas em `N → VRM → Spring Bone`.
4. **Validação:** Teste sempre no `Pose Mode` e rode `Validate VRM` antes de exportar.

## 4. Pipeline Hugging Face (PC / Google Colab → Android `assets/models/`)
- **`image-to-3d` / `text-to-3d`:** Use **`stabilityai/TripoSR`** (MIT, script pronto em `docs/colab_gerar_modelos_3d.py` gerando `.obj` + `.png` 512×512 reduzidos para ~6.000 faces), ou **`microsoft/TRELLIS`** / **`tencent/Hunyuan3D-2.1`** para chefes e edifícios (exporte como `.obj` + `.png` no Blender).
- **`text-to-image` / `image-to-image`:** Gere referências em fundo branco liso com estilo anime cel-shaded antes de converter para 3D.
- **`text-to-speech` / `text-to-audio`:** Gere falas PT-BR e efeitos sonoros offline no PC (ex.: `Kokoro-82M` / áudio aberto) para salvar em `res/raw/`.
- **Fora do APK Android:** `text-to-video`, `image-to-video`, `depth-estimation`, `image-segmentation`, `object-detection`, `automatic-speech-recognition` e `text-generation` não rodam dentro do jogo Android para manter o APK leve, offline e a 60 FPS.

