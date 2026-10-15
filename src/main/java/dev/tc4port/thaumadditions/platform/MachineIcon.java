package dev.tc4port.thaumadditions.platform;

/**
 * The five machines that draw their own inventory icon.
 *
 * <p>1.12 rendered their item forms through the same TESR as the placed block. 1.20.1 needs a
 * {@code BlockEntityWithoutLevelRenderer}, and the two loaders disagree on how one is attached:
 * NeoForge has a client extension event, Forge 1.20.1 does not and puts the hook on the
 * {@code Item} itself. The name here lets the shared registry ask for an item without naming any
 * client class, and each node wires the icon the way its loader allows.</p>
 */
public enum MachineIcon {
    CRYSTAL_BORE,
    ASPECT_COMBINER,
    AURA_CHARGER,
    CRYSTAL_CRUSHER,
    FLUX_CONCENTRATOR
}
