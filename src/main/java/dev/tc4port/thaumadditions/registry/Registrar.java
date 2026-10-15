package dev.tc4port.thaumadditions.registry;

import java.util.function.Supplier;

/**
 * Platform-neutral registration port. Implemented per node on top of DeferredRegister; each
 * call returns a holder that resolves once registries fire.
 */
public interface Registrar {
    <T> Supplier<T> register(String name, Supplier<T> factory);
}
