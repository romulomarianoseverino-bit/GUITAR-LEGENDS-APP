#!/usr/bin/env python3
"""Gera uma base humana 3D de superfícies orgânicas contínuas.

A anatomia usa perfis longitudinais e segmentos afunilados unidos por campos
suaves. Continua sendo uma base procedural de validação, não uma escultura
artística final nem um personagem rigado.
"""
from pathlib import Path
import numpy as np
import trimesh
from skimage.measure import marching_cubes

STEP = 0.014
FACE_STEP = 0.005
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

def loft(grid, profile):
    """Campo aproximado de uma superfície orgânica definida por anéis (y, rx, rz)."""
    X, Y, Z = grid
    profile = np.asarray(profile, dtype=float)
    ys, rxs, rzs = profile[:, 0], profile[:, 1], profile[:, 2]
    rx = np.interp(Y, ys, rxs)
    rz = np.interp(Y, ys, rzs)
    radial = (np.sqrt((X / np.maximum(rx, 1e-5))**2 +
                      (Z / np.maximum(rz, 1e-5))**2) - 1.0) * np.minimum(rx, rz)
    vertical = np.maximum(np.maximum(ys[0] - Y, Y - ys[-1]), 0.0)
    return (np.sqrt(np.maximum(radial, 0.0)**2 + vertical**2) +
            np.minimum(np.maximum(radial, vertical), 0.0))

def tapered_segment(grid, a, b, radius_a, radius_b, depth_scale=1.0):
    """Segmento afunilado com seção elíptica, útil para braços e pernas."""
    X, Y, Z = grid
    a = np.asarray(a, dtype=float)
    b = np.asarray(b, dtype=float)
    v = b - a
    denom = float(np.dot(v, v))
    t = np.clip(((X-a[0])*v[0] + (Y-a[1])*v[1] +
                 (Z-a[2])*v[2]) / max(denom, 1e-9), 0.0, 1.0)
    cx, cy, cz = a[0]+t*v[0], a[1]+t*v[1], a[2]+t*v[2]
    radius = radius_a + t*(radius_b-radius_a)
    radial = np.sqrt((X-cx)**2 + (Y-cy)**2 +
                     ((Z-cz)/depth_scale)**2) - radius
    cap_a = np.sqrt((X-a[0])**2 + (Y-a[1])**2 +
                    ((Z-a[2])/depth_scale)**2) - radius_a
    cap_b = np.sqrt((X-b[0])**2 + (Y-b[1])**2 +
                    ((Z-b[2])/depth_scale)**2) - radius_b
    return np.minimum(np.maximum(radial, np.minimum(cap_a, cap_b)),
                      np.maximum(cap_a, cap_b))

def smooth_union(fields, blend=0.055):
    distance = fields[0]
    for field in fields[1:]:
        h = np.maximum(blend - np.abs(distance-field), 0.0) / blend
        distance = np.minimum(distance, field) - h*h*blend*0.25
    return distance

def make_mesh(distance, axes, name, color, step=STEP):
    x, y, z = axes
    vertices, faces, _, _ = marching_cubes(
        distance, 0.0, spacing=(step, step, step), allow_degenerate=False
    )
    lower = np.array([x[0], y[0], z[0]])
    upper = np.array([x[-1], y[-1], z[-1]])
    margin = step * 0.5
    if np.any(vertices.min(axis=0) <= margin) or np.any(vertices.max(axis=0) >= (upper-lower)-margin):
        raise RuntimeError(f"A superfície {name} encosta no limite da grade.")
    vertices += lower
    mesh = trimesh.Trimesh(vertices=vertices, faces=faces, process=True)
    mesh.remove_unreferenced_vertices()
    mesh.fix_normals()
    if not mesh.is_watertight:
        raise RuntimeError(f"A superfície {name} não está fechada.")
    mesh.visual = trimesh.visual.TextureVisuals(
        material=trimesh.visual.material.PBRMaterial(
            name=name, baseColorFactor=color, metallicFactor=0.0, roughnessFactor=0.72
        )
    )
    return mesh

def main():
    bounds = (-0.58, 0.58, -1.12, 1.12, -0.38, 0.38)
    x, y, z, grid = grid_field(bounds)
    axes = (x, y, z)
    meshes = []

    # Corpo: tórax, cintura e quadril formam um único perfil; braços e pernas
    # são segmentos afunilados, fundidos ao tronco sem juntas esféricas.
    body = [loft(grid, [
        (-0.52, .205, .125), (-0.40, .235, .145), (-0.29, .225, .145),
        (-0.16, .185, .118), (-0.02, .190, .122), (.12, .222, .142),
        (.28, .255, .158), (.40, .265, .155), (.49, .205, .125)
    ])]
    body.append(tapered_segment(grid, (0,.43,0), (0,.70,0), .105,.085,.92))
    body.append(ellipsoid(grid, 0, .865, 0, .142, .185, .125))
    for side in (-1, 1):
        body.extend([
            ellipsoid(grid, side*.245, .405, 0, .125, .145, .125),
            tapered_segment(grid, (side*.25,.40,0), (side*.335,.18,0), .105,.082,.92),
            tapered_segment(grid, (side*.335,.18,0), (side*.355,-.055,0), .082,.067,.90),
            ellipsoid(grid, side*.36, -.16, -.005, .065, .075, .065),
            tapered_segment(grid, (side*.125,-.37,0), (side*.132,-.62,.005), .112,.088,.94),
            tapered_segment(grid, (side*.132,-.62,.005), (side*.135,-.91,.008), .088,.067,.92),
            ellipsoid(grid, side*.135, -.955, -.035, .095, .065, .145),
        ])
    meshes.append(make_mesh(smooth_union(body, .075), axes, "Skin_Base",
                            [190,158,137,255]))

    # Jaqueta: perfil próprio e mangas afuniladas, com transições contínuas.
    jacket = [loft(grid, [
        (-.075,.242,.155), (.02,.252,.16), (.18,.278,.18),
        (.32,.292,.18), (.43,.27,.16), (.49,.205,.13)
    ])]
    for side in (-1,1):
        jacket.extend([
            ellipsoid(grid, side*.245,.405,0,.145,.145,.145),
            tapered_segment(grid,(side*.25,.40,0),(side*.335,.18,0),.122,.098,1.0),
            tapered_segment(grid,(side*.335,.18,0),(side*.355,-.055,0),.098,.078,1.0),
        ])
    meshes.append(make_mesh(smooth_union(jacket,.065),axes,"Leather_Jacket",[38,35,34,255]))

    shirt = [loft(grid, [
        (-.06,.105,.025), (.08,.108,.03), (.25,.115,.03), (.39,.105,.025)
    ])]
    meshes.append(make_mesh(smooth_union(shirt,.025),axes,"T_Shirt",[125,126,128,255]))

    pants = [loft(grid, [
        (-.40,.235,.15),(-.32,.245,.16),(-.23,.22,.145),(-.14,.19,.125)
    ])]
    for side in (-1,1):
        pants.extend([
            tapered_segment(grid,(side*.12,-.34,0),(side*.132,-.62,.005),.132,.105,.98),
            tapered_segment(grid,(side*.132,-.62,.005),(side*.135,-.91,.008),.105,.078,.96),
        ])
    meshes.append(make_mesh(smooth_union(pants,.055),axes,"Dark_Denim",[38,48,65,255]))

    boots=[]
    for side in (-1,1):
        boots.extend([
            tapered_segment(grid,(side*.135,-.86,.005),(side*.135,-.985,.015),.082,.09,1.05),
            ellipsoid(grid,side*.135,-.99,.075,.112,.052,.17),
        ])
    meshes.append(make_mesh(smooth_union(boots,.035),axes,"Leather_Boots",[28,27,27,255]))

    hair=[ellipsoid(grid,0,.982,-.008,.151,.105,.137)]
    meshes.append(make_mesh(smooth_union(hair,.02),axes,"Hair",[35,29,27,255]))

    fx,fy,fz,face_grid=grid_field((-.23,.23,.75,1.04,-.06,.22),FACE_STEP)
    face_axes=(fx,fy,fz)
    eye_whites=[]; pupils=[]; brows=[]; ears=[]
    for side in (-1,1):
        ears.append(ellipsoid(face_grid,side*.158,.875,0,.035,.065,.045))
        eye_whites.append(ellipsoid(face_grid,side*.058,.91,.128,.023,.016,.012))
        pupils.append(ellipsoid(face_grid,side*.058,.91,.141,.009,.011,.008))
        brows.append(ellipsoid(face_grid,side*.058,.944,.126,.032,.009,.009))
    face_skin=[
        ellipsoid(face_grid,0,.865,.145,.025,.045,.025),
        ellipsoid(face_grid,0,.852,.155,.018,.025,.025),
    ]
    meshes.append(make_mesh(smooth_union(face_skin,.006),face_axes,"Face_Skin_Details",
                            [190,158,137,255],FACE_STEP))
    meshes.append(make_mesh(smooth_union(ears,.006),face_axes,"Ears",
                            [190,158,137,255],FACE_STEP))
    meshes.append(make_mesh(smooth_union(eye_whites,.004),face_axes,"Eye_Whites",
                            [238,231,218,255],FACE_STEP))
    meshes.append(make_mesh(smooth_union(pupils,.003),face_axes,"Eye_Pupils",
                            [35,30,28,255],FACE_STEP))
    meshes.append(make_mesh(smooth_union(brows,.003),face_axes,"Eyebrows",
                            [48,38,34,255],FACE_STEP))
    mouth=[ellipsoid(face_grid,0,.815,.15,.035,.009,.008)]
    meshes.append(make_mesh(smooth_union(mouth,.002),face_axes,"Mouth",
                            [105,58,53,255],FACE_STEP))

    scene=trimesh.Scene()
    for mesh in meshes:
        scene.add_geometry(mesh,geom_name=mesh.visual.material.name)
    OUT.parent.mkdir(parents=True,exist_ok=True)
    scene.export(OUT)
    print(f"GLB gerado: {OUT}; partes={len(meshes)}; "
          f"malhas_fechadas={all(m.is_watertight for m in meshes)}; "
          f"vértices={sum(len(m.vertices) for m in meshes)}")

if __name__=="__main__":
    main()
