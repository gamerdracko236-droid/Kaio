Pasta para modelos 3D externos (.obj + .png) do jogo "A Lenda dos Caçadores do Sertão".
O GameRenderer carrega automaticamente estes arquivos (com fallback seguro se ainda não existirem):

- kauan.obj / kauan.png
- severino.obj / severino.png
- espreitador.obj / espreitador.png
- boss.obj / boss.png
- arvore.obj / arvore.png
- casa.obj / casa.png

Use o script /docs/colab_gerar_modelos_3d.py no Google Colab (GPU T4 gratuita) para converter imagens em .obj otimizados (~6.000 faces) + textura .png 512x512.
