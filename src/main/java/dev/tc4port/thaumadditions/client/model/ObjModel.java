package dev.tc4port.thaumadditions.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A Wavefront OBJ, read at runtime.
 *
 * <p>Several of the 1.12 models were OBJ rather than box models — the void thaumometer, and the
 * whole blue wolf suit — and neither modern loader can load one. The first of those was baked into
 * source to get it working; this reads them instead, which is the better answer for the rest: any
 * mesh the mod shipped can be dropped in beside its texture and drawn, with no build step and no
 * generated code to keep in step.</p>
 *
 * <p>Only what those models use is read: vertices, texture coordinates, faces and the material
 * names that separate one texture from another. Normals are ignored because the meshes are drawn
 * flat-lit, and groups are ignored because nothing here is animated by them.</p>
 *
 * <p>Reading happens once per mesh and the result is kept, so a renderer may call
 * {@link #get(ResourceLocation)} every frame.</p>
 */
public final class ObjModel {
    /** One face: four corners, each with a position and a texture coordinate. */
    public record Face(String material, float[] corners) {
    }

    private static final Map<ResourceLocation, ObjModel> CACHE = new LinkedHashMap<>();

    private final List<Face> faces;
    private final float minX;
    private final float minY;
    private final float minZ;
    private final float maxX;
    private final float maxY;
    private final float maxZ;

    private ObjModel(List<Face> faces, float minX, float minY, float minZ,
                     float maxX, float maxY, float maxZ) {
        this.faces = faces;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    /** The mesh at this resource path, or {@code null} when it is missing or unreadable. */
    public static ObjModel get(ResourceLocation location) {
        if (CACHE.containsKey(location)) {
            return CACHE.get(location);
        }
        ObjModel parsed = parse(location);
        CACHE.put(location, parsed);
        return parsed;
    }

    private static ObjModel parse(ResourceLocation location) {
        try {
            Resource resource = net.minecraft.client.Minecraft.getInstance().getResourceManager()
                    .getResource(location).orElse(null);
            if (resource == null) {
                return null;
            }
            List<float[]> positions = new ArrayList<>();
            List<float[]> textures = new ArrayList<>();
            List<Face> faces = new ArrayList<>();
            String material = "default";
            float[] bounds = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                    -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};

            try (BufferedReader reader = resource.openAsReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.charAt(0) == '#') {
                        continue;
                    }
                    String[] parts = line.split("\\s+");
                    switch (parts[0]) {
                        case "v" -> positions.add(new float[]{
                                Float.parseFloat(parts[1]), Float.parseFloat(parts[2]),
                                Float.parseFloat(parts[3])});
                        case "vt" -> textures.add(new float[]{
                                Float.parseFloat(parts[1]), Float.parseFloat(parts[2])});
                        case "usemtl" -> material = parts.length > 1 ? parts[1] : "default";
                        case "f" -> {
                            Face face = readFace(parts, positions, textures, material, bounds);
                            if (face != null) {
                                faces.add(face);
                            }
                        }
                        default -> {
                            // Normals, groups and the rest are not used.
                        }
                    }
                }
            }
            return new ObjModel(faces, bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        } catch (Exception unreadable) {
            return null;
        }
    }

    /** Reads one face, padding a triangle into a quad so a caller has one case to handle. */
    private static Face readFace(String[] parts, List<float[]> positions, List<float[]> textures,
                                 String material, float[] bounds) {
        int corners = parts.length - 1;
        if (corners < 3) {
            return null;
        }
        float[] packed = new float[20];
        int[] index = new int[]{0, 1, 2, 2};
        for (int corner = 0; corner < 4; corner++) {
            String[] bits = parts[1 + index[Math.min(corner, corners - 1)]].split("/");
            int position = Integer.parseInt(bits[0]) - 1;
            float[] xyz = positions.get(position);
            float u = 0.0F;
            float v = 0.0F;
            if (bits.length > 1 && !bits[1].isEmpty()) {
                float[] uv = textures.get(Integer.parseInt(bits[1]) - 1);
                u = uv[0];
                v = 1.0F - uv[1];       // OBJ's texture origin is the opposite corner to OpenGL's
            }
            int base = corner * 5;
            packed[base] = xyz[0];
            packed[base + 1] = xyz[1];
            packed[base + 2] = xyz[2];
            packed[base + 3] = u;
            packed[base + 4] = v;
            bounds[0] = Math.min(bounds[0], xyz[0]);
            bounds[1] = Math.min(bounds[1], xyz[1]);
            bounds[2] = Math.min(bounds[2], xyz[2]);
            bounds[3] = Math.max(bounds[3], xyz[0]);
            bounds[4] = Math.max(bounds[4], xyz[1]);
            bounds[5] = Math.max(bounds[5], xyz[2]);
        }
        return new Face(material, packed);
    }

    public List<Face> faces() {
        return faces;
    }

    /** The mesh's width, which a renderer needs to scale it to the block it stands for. */
    public float width() {
        return maxX - minX;
    }

    public float height() {
        return maxY - minY;
    }

    /** The centre of the mesh, so a renderer can put its origin in the middle. */
    public float centreX() {
        return (minX + maxX) / 2.0F;
    }

    public float centreY() {
        return (minY + maxY) / 2.0F;
    }

    public float centreZ() {
        return (minZ + maxZ) / 2.0F;
    }

    /** Which material names the mesh uses, in the order they first appear. */
    public List<String> materials() {
        List<String> out = new ArrayList<>();
        for (Face face : faces) {
            if (!out.contains(face.material())) {
                out.add(face.material());
            }
        }
        return out;
    }

}
