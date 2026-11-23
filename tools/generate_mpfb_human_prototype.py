#!/usr/bin/env python3
"""Generate a first clothed MPFB2 human GLB and visual evidence.

This is an isolated prototype. It does not modify Android production sources.
"""
from __future__ import annotations
import importlib, json, os, sys
from pathlib import Path
import bpy
from mathutils import Vector

def dynamic_import(package_suffix, symbol):
    for module_name in list(sys.modules):
        if module_name.endswith(package_suffix):
            module = importlib.import_module(module_name)
            if hasattr(module, symbol):
                return getattr(module, symbol)
    raise RuntimeError(f"MPFB2 service not found: {package_suffix}.{symbol}")

HumanService = dynamic_import("mpfb.services.humanservice", "HumanService")
AssetService = dynamic_import("mpfb.services.assetservice", "AssetService")
ExportService = dynamic_import("mpfb.services.exportservice", "ExportService")
ObjectService = dynamic_import("mpfb.services.objectservice", "ObjectService")
TargetService = dynamic_import("mpfb.services.targetservice", "TargetService")
LocationService = dynamic_import("mpfb.services.locationservice", "LocationService")

out = Path("artifacts/character-prototype")
out.mkdir(parents=True, exist_ok=True)

# Blender starts with a default cube, camera and light in a new scene.
# Remove them before creating the human; otherwise the cube is exported and
# also dominates the world-space bounds used to frame the preview camera.
bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)
for datablocks in (bpy.data.meshes, bpy.data.cameras, bpy.data.lights):
    for datablock in (list(datablocks)):
        if datablock.users == 0:
            datablocks.remove(datablock)
report = {"stage": "first_clothed_human_glb", "production_integrated": False,
          "animation_exported": False, "assets": {}, "mesh_count": 0,
          "material_count": 0, "armature_count": 0}

# Asset packs are extracted by the workflow into MPFB's user-data directory.
AssetService.rescan_pack_metadata()
report["installed_packs"] = AssetService.get_pack_names()
if not report["installed_packs"]:
    raise RuntimeError("Nenhum pacote MPFB2 foi instalado no diretório de dados efetivo da extensão.")
def catalog_asset_names(subdir):
    catalog = AssetService.get_asset_list(subdir, "mhclo")
    return sorted({Path(item.get("full_path", "")).name for item in catalog.values()
                   if item.get("full_path")})
report["available_hair_assets"] = catalog_asset_names("hair")
report["available_clothing_assets"] = catalog_asset_names("clothes")

def find(filename, subdir):
    path = AssetService.find_asset_absolute_path(filename, asset_subdir=subdir)
    if not path:
        raise RuntimeError(f"Required asset missing: {subdir}/{filename}")
    return path

# Build an original stage avatar with a natural, moderately lean phenotype.
# These macro values are broad art-direction controls, not an exact likeness claim.
macro = TargetService.get_default_macro_info_dict()
macro.update({
    "gender": 0.0,  # MPFB2 oficial: 0.0 = masculino; 1.0 = feminino.
    "cupsize": 0.0,  # reduzir o volume mamário para a anatomia masculina natural.
    "age": 0.32,
    "muscle": 0.52,
    "weight": 0.43,
    "proportions": 0.52,
    "height": 0.52,
    "firmness": 0.32,
})
macro["race"] = {"caucasian": 0.72, "african": 0.12, "asian": 0.16}
if float(macro.get("gender", 1.0)) != 0.0 or float(macro.get("cupsize", 1.0)) != 0.0:
    raise RuntimeError(f"Perfil corporal masculino não aplicado: gender={macro.get('gender')}, cupsize={macro.get('cupsize')}")
human = HumanService.create_human(macro_detail_dict=macro)
report["art_direction"] = {
    "inspiration": "modern Brazilian pop/sertanejo stage aesthetic",
    "hair": "dark chestnut, textured short stage cut with controlled sides and visible top volume; avoid light/auburn tones",
    "wardrobe": "coordinated midnight-black contemporary stage tailoring, black footwear and restrained smoke-silver accents; no cowboy styling",
    "identity": "original game avatar, not a photorealistic replica",
    "macro_phenotype": macro,
    "body_validation": "MPFB2 gender=0.0 (male), cupsize=0.0; generated preview must be inspected before approval",
}
skin = find("young_caucasian_male.mhmat", "skins")
HumanService.set_character_skin(skin, human, skin_type="GAMEENGINE")
HumanService.add_builtin_rig(human, "game_engine")
report["assets"]["skin"] = Path(skin).name

# Use a contemporary tailored stage look, not the cowboy/vaqueiro
# stereotype: no cowboy boots, hat, oversized buckle or leg-hung accessory.
# The system elegant male suit and neutral shoes are CC0 and fit the shared MPFB human rig.
wardrobe = [
    ("eyes", "low-poly.mhclo", "Eyes"),
    ("eyebrows", "eyebrow001.mhclo", "Eyebrows"),
    ("hair", "short03.mhclo", "Hair"),
    ("clothes", "male_elegantsuit01.mhclo", "Clothes"),
    ("clothes", "shoes01.mhclo", "Clothes"),
]
for subdir, filename, kind in wardrobe:
    asset = find(filename, subdir)
    HumanService.add_mhclo_asset(asset, human, asset_type=kind, material_type="GAMEENGINE")
    report["assets"][filename] = str(Path(asset).name)

# Stage styling pass: replace the generic office palette with a coordinated,
# contemporary black/graphite performance look. Keep material slots separate
# where the asset provides separate jacket/shirt/tie/shoe surfaces.
def set_material_color(material, rgba, roughness=None, metallic=None):
    if not material:
        return
    material.diffuse_color = rgba
    if not material.use_nodes:
        return
    for node in material.node_tree.nodes:
        if node.type != "BSDF_PRINCIPLED":
            continue
        base = node.inputs.get("Base Color")
        if base:
            for link in list(material.node_tree.links):
                if link.to_socket == base:
                    material.node_tree.links.remove(link)
            base.default_value = rgba
        if roughness is not None and node.inputs.get("Roughness"):
            node.inputs["Roughness"].default_value = roughness
        if metallic is not None and node.inputs.get("Metallic"):
            node.inputs["Metallic"].default_value = metallic

report["material_style_changes"] = []
for obj in bpy.data.objects:
    if obj.type != "MESH":
        continue
    object_name = obj.name.lower()
    for material in obj.data.materials:
        if not material:
            continue
        material_name = material.name.lower()
        if "short03" in object_name or "hair" in object_name or "hair" in material_name:
            set_material_color(material, (0.035, 0.012, 0.006, 1.0), roughness=0.72)
            report["material_style_changes"].append({"material": material.name, "style": "dark_chestnut_hair"})
        elif "male_elegantsuit01" in object_name or any(k in material_name for k in ("suit", "jacket", "trouser", "pants")):
            set_material_color(material, (0.012, 0.014, 0.020, 1.0), roughness=0.82)
            report["material_style_changes"].append({"material": material.name, "style": "midnight_black_stage_tailoring"})
        elif "shoes01" in object_name or "shoe" in material_name:
            set_material_color(material, (0.018, 0.016, 0.015, 1.0), roughness=0.58)
            report["material_style_changes"].append({"material": material.name, "style": "black_stage_footwear"})
        elif "tie" in material_name:
            set_material_color(material, (0.11, 0.10, 0.09, 1.0), roughness=0.55, metallic=0.12)
            report["material_style_changes"].append({"material": material.name, "style": "smoke_silver_detail"})

# Duplicate the full character hierarchy for export, leaving the generated
# working character untouched.
root = ExportService.create_character_copy(human, name_suffix="_glb")
base = ObjectService.find_object_of_type_amongst_nearest_relatives(root, "Basemesh")
ExportService.bake_modifiers_remove_helpers(base, bake_masks=True,
    bake_subdiv=True, remove_helpers=True, also_proxy=True)

# Render evidence from the dressed prototype.
bpy.context.scene.render.engine = "BLENDER_EEVEE_NEXT"
bpy.context.scene.render.resolution_x = 1200
bpy.context.scene.render.resolution_y = 1200
bpy.context.scene.render.resolution_percentage = 100
bpy.context.scene.render.image_settings.file_format = "PNG"
bpy.context.scene.render.film_transparent = False
world = bpy.context.scene.world
if world:
    world.color = (0.055, 0.055, 0.055)
# Studio lights.
for name, loc, power, size in [
    ("Key", (4.0, -5.0, 6.0), 900, 5.0),
    ("Fill", (-4.0, -3.0, 3.5), 500, 4.0),
    ("Rim", (0.0, 3.0, 5.0), 1100, 3.0),
]:
    data = bpy.data.lights.new(name, "AREA")
    data.energy = power
    data.shape = "DISK"
    data.size = size
    obj = bpy.data.objects.new(name, data)
    bpy.context.collection.objects.link(obj)
    obj.location = loc
    obj.rotation_euler = (0.0, 0.0, 0.0)
    direction = -obj.location
    obj.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()

cam_data = bpy.data.cameras.new("PrototypeCamera")
cam = bpy.data.objects.new("PrototypeCamera", cam_data)
bpy.context.collection.objects.link(cam)
cam.location = (4.2, -8.5, 3.3)
target = (0.0, 0.0, 0.95)
direction = (Vector(target) - cam.location)
cam.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()
cam.data.lens = 55
bpy.context.scene.camera = cam

# Frame the complete character based on world-space bounds.
depsgraph = bpy.context.evaluated_depsgraph_get()
points = []
for obj in bpy.context.scene.objects:
    if obj.type == "MESH":
        evaluated = obj.evaluated_get(depsgraph)
        points.extend([evaluated.matrix_world @ Vector(corner)
                       for corner in evaluated.bound_box])
if not points:
    raise RuntimeError("No character meshes found for preview framing")
min_z = min(p.z for p in points)
max_z = max(p.z for p in points)
height = max_z - min_z
if height <= 0.1:
    raise RuntimeError(f"Invalid character bounds: height={height}")
# Aim at the center of the complete human and pull the camera back far enough
# to keep head, feet and footwear inside the square preview.
target = (0.0, 0.0, (min_z + max_z) * 0.5)
cam.location = (4.2, -8.5, target[2] + height * 0.08)
direction = Vector(target) - cam.location
cam.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()
cam.data.lens = 125
bpy.context.scene.render.filepath = str(out / "preview-front-3q.png")
bpy.ops.render.render(write_still=True)

# GLB export of the scene with the skinned armature and wardrobe.
glb_path = out / "guitar-legends-human-prototype.glb"
bpy.ops.export_scene.gltf(filepath=str(glb_path), export_format="GLB",
                          use_selection=False, export_apply=True)
if not glb_path.exists() or glb_path.stat().st_size < 100_000:
    raise RuntimeError("GLB export missing or unexpectedly small")

report["glb"] = glb_path.name
report["glb_bytes"] = glb_path.stat().st_size
report["mesh_count"] = sum(1 for o in bpy.data.objects if o.type == "MESH")
report["armature_count"] = sum(1 for o in bpy.data.objects if o.type == "ARMATURE")
report["material_count"] = len(bpy.data.materials)
report["body_validation"] = {"gender": macro["gender"], "cupsize": macro["cupsize"], "expected_gender": "male"}
if macro["gender"] != 0.0 or macro["cupsize"] != 0.0:
    raise RuntimeError("A geração não está configurada para anatomia masculina.")
report["mesh_names"] = [o.name for o in bpy.data.objects if o.type == "MESH"]
report["material_names"] = [m.name for m in bpy.data.materials]
report["armature_names"] = [o.name for o in bpy.data.objects if o.type == "ARMATURE"]
(out / "prototype-report.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
print("CHARACTER_PROTOTYPE_REPORT=" + json.dumps(report, ensure_ascii=False))
