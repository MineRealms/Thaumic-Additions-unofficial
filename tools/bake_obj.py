# Bakes an OBJ into a Java mesh literal.
#
# The modern loaders have no OBJ model support left, so a model that shipped as one has to be drawn
# by hand. Parsing it at runtime would add a reader and a resource that can fail at load; the mesh
# is small and never changes, so it is baked into the source instead and can be read and checked.
#
# Usage: python tools/bake_obj.py <in.obj> <out.java> <Class> <material1Material2...>
import io
import sys

OBJ, OUT, CLASS, MATERIALS = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4].split(",")


def parse(path):
    verts, uvs = [], []
    quads = []          # (material, [(vIdx, vtIdx) x4])
    material = MATERIALS[0]
    for line in io.open(path, encoding="utf-8", errors="replace"):
        parts = line.split()
        if not parts:
            continue
        if parts[0] == "v":
            verts.append(tuple(float(x) for x in parts[1:4]))
        elif parts[0] == "vt":
            uvs.append(tuple(float(x) for x in parts[1:3]))
        elif parts[0] == "usemtl":
            material = parts[1]
        elif parts[0] == "f":
            face = []
            for token in parts[1:]:
                bits = token.split("/")
                vi = int(bits[0]) - 1
                ti = int(bits[1]) - 1 if len(bits) > 1 and bits[1] else -1
                face.append((vi, ti))
            # Quads as quads, triangles as triangles; the file only has quads.
            if len(face) == 3:
                face.append(face[2])
            quads.append((material, face))
    return verts, uvs, quads


verts, uvs, quads = parse(OBJ)

lines = []
lines.append("package dev.tc4port.thaumadditions.client.model;")
lines.append("")
lines.append("/**")
lines.append(" * The %s mesh, baked from its OBJ." % CLASS)
lines.append(" *")
lines.append(" * <p>Its model shipped as a Wavefront OBJ, which the modern loaders cannot load: Forge and")
lines.append(" * NeoForge both dropped OBJ support. The mesh is small and fixed, so rather than write a")
lines.append(" * reader that can fail at load, it is baked into this file — one quad per four vertices,")
lines.append(" * each vertex five floats: x, y, z, u, v. Quads carry the material they belong to, because")
lines.append(" * the two halves of the model are textured from different sheets.</p>")
lines.append(" */")
lines.append("public final class %s {" % CLASS)
lines.append("    private %s() {" % CLASS)
lines.append("    }")
lines.append("")
for index, mat in enumerate(MATERIALS):
    lines.append("    /** Material %d: {@code %s}. */" % (index, mat))
    lines.append("    public static final int MATERIAL_%s = %d;" % (mat.upper(), index))
lines.append("")
lines.append("    /** One quad: the material, then four vertices of five floats each. */")
lines.append("    public record Quad(int material, float[] vertices) {")
lines.append("    }")
lines.append("")
lines.append("    /** Every quad, in the file's own order. */")
lines.append("    public static final Quad[] QUADS = {")
for material, face in quads:
    floats = []
    for vi, ti in face:
        x, y, z = verts[vi]
        u, v = uvs[ti] if ti >= 0 else (0.0, 0.0)
        floats += [x, y, z, u, v]
    body = ", ".join("%.5fF" % f for f in floats)
    lines.append("            new Quad(MATERIAL_%s, new float[]{%s})," % (material.upper(), body))
lines.append("    };")
lines.append("}")
io.open(OUT, "w", encoding="utf-8", newline="\n").write("\n".join(lines) + "\n")

# The mesh's bounds, which is what a renderer needs to centre and scale it.
xs = [v[0] for v in verts]
ys = [v[1] for v in verts]
zs = [v[2] for v in verts]
print("baked %d quads, %d verts; bounds x[%.3f %.3f] y[%.3f %.3f] z[%.3f %.3f]"
      % (len(quads), len(verts), min(xs), max(xs), min(ys), max(ys), min(zs), max(zs)))
