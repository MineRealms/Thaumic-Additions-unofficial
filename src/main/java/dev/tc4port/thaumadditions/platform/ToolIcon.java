package dev.tc4port.thaumadditions.platform;

/**
 * The items that draw themselves rather than being a flat picture.
 *
 * <p>1.12 could point an item at a model file and let the model system transform it per context.
 * The modern loaders cannot, for the two things here: the void thaumometer's model was a Wavefront
 * OBJ, which neither loader reads any more, and an OBJ needs per-context placement that only a
 * renderer can do. Naming the item here lets the shared registry ask for one without naming any
 * client class, and each node wires the renderer the way its loader allows.</p>
 */
public enum ToolIcon {
    VOID_THAUMOMETER
}
