package dev.tc4port.thaumadditions.platform;

/**
 * The five tool shapes ThaumicAdditions registers. Kept as an enum so the
 * per-node factory can switch on it and construct the matching native class.
 */
public enum ToolKind {
    PICKAXE,
    AXE,
    SHOVEL,
    HOE,
    SWORD
}
