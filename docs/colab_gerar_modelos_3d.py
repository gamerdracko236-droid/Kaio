# %% [markdown]
# Gerador 3D Gratuito para Android (TripoSR - Licença MIT)
# Execute no Google Colab com Runtime -> GPU T4 (gratuita)

# %% Cell 1 - Instalar TripoSR + Otimizador de Malha (~2 min)
# !git clone https://github.com/VAST-AI-Research/TripoSR.git
# %cd /content/TripoSR
# !pip install -q -r requirements.txt
# !pip install -q git+https://github.com/tatsy/torchmcubes.git
# !pip install -q xatlas trimesh onnxruntime pymeshlab

# %% Cell 2 - Enviar imagens em fundo branco liso (ex: kauan.png, arvore.png, casa.png, espreitador.png)
import os

def prepare_workspace():
    os.makedirs("/content/input_images", exist_ok=True)
    os.makedirs("/content/output_raw", exist_ok=True)
    os.makedirs("/content/android_models", exist_ok=True)

# %% Cell 3 - Converter imagens em .obj + .png (512x512) e reduzir para ~6.000 faces
def generate_android_models():
    import glob
    import shutil
    import subprocess
    import pymeshlab

    input_imgs = sorted(glob.glob("/content/input_images/*.*"))
    if not input_imgs:
        raise RuntimeError("Nenhuma imagem encontrada em /content/input_images")

    subprocess.run([
        "python", "/content/TripoSR/run.py",
        *input_imgs,
        "--output-dir", "/content/output_raw",
        "--bake-texture",
        "--texture-resolution", "512",
        "--mc-resolution", "256",
    ], check=True)

    for idx, img_path in enumerate(input_imgs):
        base_name = os.path.splitext(os.path.basename(img_path))[0].lower().replace(" ", "_")
        sub_dir = os.path.join("/content/output_raw", str(idx))
        raw_obj = os.path.join(sub_dir, "mesh.obj")
        raw_tex = os.path.join(sub_dir, "texture.png")

        dest_obj = os.path.join("/content/android_models", f"{base_name}.obj")
        dest_png = os.path.join("/content/android_models", f"{base_name}.png")

        if os.path.exists(raw_tex):
            shutil.copyfile(raw_tex, dest_png)

        if os.path.exists(raw_obj):
            ms = pymeshlab.MeshSet()
            ms.load_new_mesh(raw_obj)
            ms.meshing_decimation_quadric_edge_collapse_with_texture(
                targetfacenum=6000,
                preserveboundary=True,
                preservenormal=True,
            )
            ms.save_current_mesh(
                dest_obj,
                save_vertex_color=False,
                save_wedge_texcoord=True,
                save_wedge_normal=True,
            )
            print(f"Pronto para app/src/main/assets/models/: {dest_obj} + {dest_png}")
