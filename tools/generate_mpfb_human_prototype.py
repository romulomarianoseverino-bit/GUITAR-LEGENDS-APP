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

def find(filename, subdir):
    path = AssetService.find_asset_absolute_path(filename, asset_subdir=subdir)
    if not path:
        raise RuntimeError(f"Required asset missing: {subdir}/{filename}")
    return path

# Build the human base, apply a game-engine skin and a compatible game rig.
human = HumanService.create_human()
skin = find("young_caucasian_male.mhmat", "skins")
HumanService.set_character_skin(skin, human, skin_type="GAMEENGINE")
HumanService.add_builtin_rig(human, "game_engine")
report["assets"]["skin"] = Path(skin).name

# Use specific, documented assets: natural hair, a basic tee, classic jeans,
# and male boots. The jeans pack is CC-BY and attribution is recorded in-repo.
wardrobe = [
    ("eyes", "low-poly.mhclo", "Eyes"),
    ("eyebrows", "eyebrow001.mhclo", "Eyebrows"),
    ("hair", "short01.mhclo", "Hair"),
    ("clothes", "toigo_basic_tucked_t-shirt.mhclo", "Clothes"),
    ("clothes", "punkduck_male_classic_jeans.mhclo", "Clothes"),
    ("clothes", "culturalibre_male_boots.mhclo", "Clothes"),
]
for subdir, filename, kind in wardrobe:
    asset = find(filename, subdir)
    HumanService.add_mhclo_asset(asset, human, asset_type=kind, material_type="GAMEENGINE")
    report["assets"][filename] = str(Path(asset).name)

# Duplicate the full character hierarchy for export, leaving the generated
# working character untouched.
root = ExportService.create_character_copy(human, name_suffix="_glb")
base = ObjectService.find_object_of_type_amongst_nearest_relatives(root, "Basemesh")
ExportService.bake_modifiers_remove_helpers(base, bake_masks=True,
    bake_subdiv=True, remove_helpers=True, also_proxy=True)

# Keep only the duplicated export hierarchy visible. Rendering both the
# editable source and its export copy causes overlapping meshes/z-fighting.
export_objects = {root, *list(root.children_recursive)}
for obj in bpy.context.scene.objects:
    if obj.type in {"MESH", "ARMATURE"} and obj not in export_objects:
        obj.hide_render = True

# MPFB2 may leave Blender's startup cube in the scene. It is not part of
# the human and must never be included in the preview or exported GLB.
for obj in list(bpy.data.objects):
    if obj.name == "Cube" and obj.type == "MESH":
        bpy.data.objects.remove(obj, do_unlink=True)

# Render evidence from the export hierarchy only. MPFB humanoids face +Z;
# place the camera on that side so the face is visible instead of the back.
# Use an orthographic front three-quarter camera to avoid perspective distortion.
bpy.context.scene.render.engine = "BLENDER_EEVEE_NEXT"
bpy.context.scene.render.resolution_x = 1200
bpy.context.scene.render.resolution_y = 1200
bpy.context.scene.render.resolution_percentage = 100
bpy.context.scene.render.image_settings.file_format = "PNG"
bpy.context.scene.render.film_transparent = False
world = bpy.context.scene.world
if world:
    world.color = (0.055, 0.055, 0.055)

for name, loc, power, size in [
    ("Key", (3.5, 3.5, -4.0), 900, 4.0),
    ("Fill", (-3.5, 2.5, -3.0), 500, 4.5),
    ("Rim", (0.0, 3.0, 3.5), 1000, 3.0),
]:
    data = bpy.data.lights.new(name, "AREA")
    data.energy = power
    data.shape = "DISK"
    data.size = size
    obj = bpy.data.objects.new(name, data)
    bpy.context.collection.objects.link(obj)
    obj.location = loc
    obj.rotation_euler = (-obj.location).to_track_quat("-Z", "Y").to_euler()

cam_data = bpy.data.cameras.new("PrototypeCamera")
cam = bpy.data.objects.new("PrototypeCamera", cam_data)
bpy.context.collection.objects.link(cam)
bpy.context.scene.camera = cam

# Calculate bounds from visible export meshes only; MPFB humans are Y-up.
depsgraph = bpy.context.evaluated_depsgraph_get()
points = []
for obj in export_objects:
    if obj.type == "MESH" and not obj.hide_render:
        evaluated = obj.evaluated_get(depsgraph)
        points.extend([evaluated.matrix_world @ Vector(corner)
                       for corner in evaluated.bound_box])
if not points:
    raise RuntimeError("No visible export meshes found for preview framing")
mins = [min(p[i] for p in points) for i in range(3)]
maxs = [max(p[i] for p in points) for i in range(3)]
center = Vector(tuple((mins[i] + maxs[i]) * 0.5 for i in range(3)))
height = maxs[1] - mins[1]
if height <= 0.1:
    raise RuntimeError(f"Invalid character bounds: height={height}")
cam.location = center + Vector((height * 0.10, height * 0.06, height * 4.0))
cam.rotation_euler = (center - cam.location).to_track_quat("-Z", "Y").to_euler()
cam.data.type = "ORTHO"
cam.data.ortho_scale = height * 1.42
bpy.context.scene.render.filepath = str(out / "preview-front-3q.png")
bpy.ops.render.render(write_still=True)

# Export only the dressed, rigged copy; do not duplicate the editable source.
bpy.ops.object.select_all(action="DESELECT")
for obj in export_objects:
    if obj.type in {"MESH", "ARMATURE"}:
        obj.select_set(True)
bpy.context.view_layer.objects.active = base
glb_path = out / "guitar-legends-human-prototype.glb"
bpy.ops.export_scene.gltf(filepath=str(glb_path), export_format="GLB",
                          use_selection=True, export_apply=True)
if not glb_path.exists() or glb_path.stat().st_size < 100_000:
    raise RuntimeError("GLB export missing or unexpectedly small")

report["glb"] = glb_path.name
report["glb_bytes"] = glb_path.stat().st_size
report["mesh_count"] = sum(1 for o in bpy.data.objects if o.type == "MESH")
report["startup_cube_removed"] = not any(o.name == "Cube" for o in bpy.data.objects)
report["vertical_axis"] = "Y"
report["armature_count"] = sum(1 for o in bpy.data.objects if o.type == "ARMATURE")
report["material_count"] = len(bpy.data.materials)
report["mesh_names"] = [o.name for o in bpy.data.objects if o.type == "MESH"]
report["armature_names"] = [o.name for o in bpy.data.objects if o.type == "ARMATURE"]
(out / "prototype-report.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
print("CHARACTER_PROTOTYPE_REPORT=" + json.dumps(report, ensure_ascii=False))
