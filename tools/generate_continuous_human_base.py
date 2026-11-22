#!/usr/bin/env python3
"""Gera um personagem-base contínuo com volumes de roupa e detalhes visuais.

Dependências gratuitas: numpy, scikit-image e trimesh.
Esta é uma base procedural de prova: ainda não substitui escultura artística,
topologia facial profissional, rig esquelético ou animações.
"""
from pathlib import Path
import numpy as np
import trimesh
from skimage.measure import marching_cubes

STEP = 0.018
OUT = Path("app/src/main/assets/models/continuous_human_base.glb")

def grid_field(bounds, step=STEP):
    x = np.arange(bounds[0], bounds[1] + step * 0.5, step)
    y = np.arange(bounds[2], bounds[3] + step * 0.5, step)
    z = np.arange(bounds[4], bounds[5] + step * 0.5, step)
    return x, y, z, np.meshgrid(x, y, z, indexing="ij")

def ellipsoid(grid, cx, cy, cz, rx, ry, rz):
    X, Y, Z = grid
    q = np.sqrt(((X-cx)/rx)**2 + ((Y-cy)/ry)**2 + ((Z-cz)/rz)**2)
    return (q - 1.0) * min(rx, ry, rz)

def smooth_union(fields, blend=0.065):
    distance = fields[0]
    for field in fields[1:]:
        h = np.maximum(blend - np.abs(distance-field), 0.0) / blend
        distance = np.minimum(distance, field) - h*h*blend*0.25
    return distance

def make_mesh(distance, axes, name, color):
    x, y, z = axes
    vertices, faces, _, _ = marching_cubes(
        distance, 0.0, spacing=(STEP, STEP, STEP), allow_degenerate=False
    )
    vertices += np.array([x[0], y[0], z[0]])
    mesh = trimesh.Trimesh(vertices=vertices, faces=faces, process=True)
    mesh.remove_unreferenced_vertices()
    mesh.fix_normals()
    if not mesh.is_watertight:
        raise RuntimeError(f"A superfície {name} não está fechada.")
    mesh.visual = trimesh.visual.TextureVisuals(
        material=trimesh.visual.material.PBRMaterial(
            name=name,
            baseColorFactor=color,
            metallicFactor=0.0,
            roughnessFactor=0.78,
        )
    )
    return mesh

def main():
    bounds = (-0.56, 0.56, -1.12, 1.12, -0.36, 0.36)
    x, y, z, grid = grid_field(bounds)
    axes = (x, y, z)
    X, Y, Z = grid

    # Anatomia-base: volumes unidos em uma única superfície, sem juntas de peças.
    body = [
        ellipsoid(grid, 0, .27, 0, .255, .34, .145),
        ellipsoid(grid, 0, -.015, 0, .205, .30, .125),
        ellipsoid(grid, 0, -.285, 0, .225, .19, .14),
        ellipsoid(grid, 0, .70, 0, .115, .145, .105),
        ellipsoid(grid, 0, .875, 0, .145, .185, .13),
    ]
    for side in (-1, 1):
        body.extend([
            ellipsoid(grid, side*.245, .42, 0, .135, .145, .13),
            ellipsoid(grid, side*.325, .22, 0, .105, .235, .10),
            ellipsoid(grid, side*.355, -.005, 0, .088, .22, .085),
            ellipsoid(grid, side*.365, -.205, -.005, .075, .105, .065),
            ellipsoid(grid, side*.125, -.39, 0, .125, .265, .12),
            ellipsoid(grid, side*.13, -.61, .005, .105, .13, .105),
            ellipsoid(grid, side*.135, -.79, .008, .092, .22, .09),
            ellipsoid(grid, side*.135, -.94, -.045, .105, .075, .17),
        ])
    meshes = [make_mesh(smooth_union(body, .075), axes, "Skin_Base", [190, 158, 137, 255])]

    # Jaqueta: tórax, barra inferior, ombros e mangas sobrepostas aos braços.
    jacket = [
        ellipsoid(grid, 0, .245, 0, .278, .34, .166),
        ellipsoid(grid, 0, -.005, 0, .246, .22, .155),
    ]
    for side in (-1, 1):
        jacket.extend([
            ellipsoid(grid, side*.245, .405, 0, .145, .15, .145),
            ellipsoid(grid, side*.315, .205, 0, .118, .235, .118),
            ellipsoid(grid, side*.35, -.015, 0, .098, .205, .10),
        ])
    meshes.append(make_mesh(smooth_union(jacket, .055), axes, "Leather_Jacket", [38, 35, 34, 255]))

    # Camiseta central visível sob a abertura da jaqueta.
    shirt = [
        ellipsoid(grid, 0, .235, .164, .105, .255, .028),
        ellipsoid(grid, 0, .015, .151, .092, .105, .025),
    ]
    meshes.append(make_mesh(smooth_union(shirt, .025), axes, "T_Shirt", [125, 126, 128, 255]))

    # Calça jeans: cintura, quadril e pernas com transições suaves.
    pants = [
        ellipsoid(grid, 0, -.285, 0, .239, .17, .153),
    ]
    for side in (-1, 1):
        pants.extend([
            ellipsoid(grid, side*.125, -.405, 0, .137, .27, .13),
            ellipsoid(grid, side*.13, -.625, .002, .116, .135, .112),
            ellipsoid(grid, side*.135, -.805, .005, .103, .22, .10),
        ])
    meshes.append(make_mesh(smooth_union(pants, .045), axes, "Dark_Denim", [38, 48, 65, 255]))

    # Botas escuras com volume de cano e bico.
    boots = []
    for side in (-1, 1):
        boots.extend([
            ellipsoid(grid, side*.135, -.925, -.005, .112, .105, .11),
            ellipsoid(grid, side*.135, -.99, .055, .12, .055, .17),
        ])
    meshes.append(make_mesh(smooth_union(boots, .035), axes, "Leather_Boots", [28, 27, 27, 255]))

    # Cabelo e detalhes faciais discretos voltados para a frente (+Z).
    hair = [ellipsoid(grid, 0, .985, -.005, .151, .105, .137)]
    meshes.append(make_mesh(smooth_union(hair, .02), axes, "Hair", [35, 29, 27, 255]))

    face_skin = []
    eye_whites = []
    pupils = []
    brows = []
    for side in (-1, 1):
        face_skin.append(ellipsoid(grid, side*.158, .875, 0, .035, .065, .045))
        eye_whites.append(ellipsoid(grid, side*.058, .91, .128, .023, .016, .012))
        pupils.append(ellipsoid(grid, side*.058, .91, .139, .008, .010, .006))
        brows.append(ellipsoid(grid, side*.058, .94, .126, .029, .006, .008))
    face_skin.extend([
        ellipsoid(grid, 0, .865, .145, .025, .045, .025),
    ])
    meshes.append(make_mesh(smooth_union(face_skin, .008), axes, "Face_Skin_Details", [190, 158, 137, 255]))
    meshes.append(make_mesh(smooth_union(eye_whites, .004), axes, "Eye_Whites", [238, 231, 218, 255]))
    meshes.append(make_mesh(smooth_union(pupils, .003), axes, "Eye_Pupils", [35, 30, 28, 255]))
    meshes.append(make_mesh(smooth_union(brows, .003), axes, "Eyebrows", [48, 38, 34, 255]))
    mouth = [ellipsoid(grid, 0, .815, .142, .035, .008, .006)]
    meshes.append(make_mesh(smooth_union(mouth, .002), axes, "Mouth", [105, 58, 53, 255]))

    scene = trimesh.Scene()
    for mesh in meshes:
        scene.add_geometry(mesh, geom_name=mesh.visual.material.name)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    scene.export(OUT)
    print(
        f"GLB gerado: {OUT}; partes={len(meshes)}; "
        f"malhas_fechadas={all(m.is_watertight for m in meshes)}; "
        f"vértices={sum(len(m.vertices) for m in meshes)}"
    )

if __name__ == "__main__":
    main()
