package dev.tc4port.thaumadditions.platform;

/**
 * Marks an armor piece as belonging to a named set, so set-wide effects
 * (mithminite's harmful-effect cleanse) can be evaluated without comparing
 * version-specific armor material types.
 */
public interface TaArmorSetMember {
    /** The logical set id, e.g. {@code "mithminite"}. */
    String armorSetId();
}
