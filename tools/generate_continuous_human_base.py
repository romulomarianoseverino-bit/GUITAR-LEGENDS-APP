#!/usr/bin/env python3
"""Gera uma malha-base humana contínua para a prova SceneView.

Dependências gratuitas: numpy, scikit-image e trimesh.
A malha é um bloqueio anatômico neutro; não substitui um personagem final,
topologia facial, roupa detalhada nem rig esquelético.
"""
from pathlib import Path
import numpy as np
import trimesh
from skimage.measure import marching_cubes

STEP = 0.018
OUT = Path("app/src/main/assets/models/continuous_human_base.glb")

def main():
    x = np.arange(-0.50, 0.501, STEP)
    y = np.arange(-1.12, 1.121, STEP)
    z = np.arange(-0.30, 0.301, STEP)
    X, Y, Z = np.meshgrid(x, y, z, indexing="ij")

    def ellipsoid(cx, cy, cz, rx, ry, rz):
        q = np.sqrt(((X-cx)/rx)**2 + ((Y-cy)/ry)**2 + ((Z-cz)/rz)**2)
        return (q - 1.0) * min(rx, ry, rz)

    # Massas anatômicas suavemente fundidas: cabeça, pescoço, tronco e membros.
    fields = [
        ellipsoid(0, .27, 0, .255, .34, .145),
        ellipsoid(0, -.015, 0, .205, .30, .125),
        ellipsoid(0, -.285, 0, .225, .19, .14),
        ellipsoid(0, .70, 0, .115, .145, .105),
        ellipsoid(0, .875, 0, .145, .185, .13),
    ]
    for side in (-1, 1):
        fields.extend([
            ellipsoid(side*.245, .42, 0, .135, .145, .13),
            ellipsoid(side*.325, .22, 0, .105, .235, .10),
            ellipsoid(side*.355, -.005, 0, .088, .22, .085),
            ellipsoid(side*.365, -.205, -.005, .075, .105, .065),
            ellipsoid(side*.125, -.39, 0, .125, .265, .12),
            ellipsoid(side*.13, -.61, .005, .105, .13, .105),
            ellipsoid(side*.135, -.79, .008, .092, .22, .09),
            ellipsoid(side*.135, -.94, -.045, .105, .075, .17),
        ])

    # União suave (smooth minimum), para evitar juntas entre volumes.
    blend = .075
    distance = fields[0]
    for field in fields[1:]:
        h = np.maximum(blend - np.abs(distance-field), 0.0) / blend
        distance = np.minimum(distance, field) - h*h*blend*.25

    vertices, faces, _, _ = marching_cubes(
        distance, 0.0, spacing=(STEP, STEP, STEP), allow_degenerate=False
    )
    vertices += np.array([x[0], y[0], z[0]])
    mesh = trimesh.Trimesh(vertices=vertices, faces=faces, process=True)
    mesh.remove_unreferenced_vertices()
    mesh.fix_normals()
    if len(mesh.split(only_watertight=False)) != 1:
        raise RuntimeError("A malha-base precisa formar uma única superfície conectada.")
    if not mesh.is_watertight:
        raise RuntimeError("A malha-base não está fechada; revisar os limites do volume.")

    mesh.visual = trimesh.visual.TextureVisuals(
        material=trimesh.visual.material.PBRMaterial(
            name="HumanBase_Matte",
            baseColorFactor=[190, 158, 137, 255],
            metallicFactor=0.0,
            roughnessFactor=.82,
        )
    )
    OUT.parent.mkdir(parents=True, exist_ok=True)
    scene = trimesh.Scene()
    scene.add_geometry(mesh, geom_name="ContinuousHumanBase")
    scene.export(OUT)
    print(
        f"GLB gerado: {OUT}; vértices={len(mesh.vertices)}; "
        f"faces={len(mesh.faces)}; fechado={mesh.is_watertight}; "
        f"componentes={len(mesh.split(only_watertight=False))}"
    )

if __name__ == "__main__":
    main()
