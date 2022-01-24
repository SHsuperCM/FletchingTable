package io.shcm.shsupercm.fabric.fletchingtable.api;

import java.lang.annotation.*;

/**
 * Automatically adds the annotated element to the requested entrypoint in fabric.mod.json
 */
@Repeatable(Entrypoint.Repeated.class)
@Retention(RetentionPolicy.SOURCE)
@Target( { ElementType.TYPE, ElementType.METHOD, ElementType.FIELD } )
public @interface Entrypoint {
    /**
     * Used on elements implementing ModInitializer to run code when the game starts.
     */
    String MAIN = "main";
    /**
     * Used on elements implementing ClientModInitializer to run code when the physical client starts.
     */
    String CLIENT = "client";
    /**
     * Used on elements implementing DedicatedServerModInitializer to run code when the physical server starts.
     */
    String SERVER = "server";
    /**
     * Used on elements implementing PreLaunchEntryPoint to run code as soon as possible(right before the game starts).<br>
     * Use with caution!
     */
    String PRE_LAUNCH = "preLaunch";

    /**
     * @return entrypoint identifier to register the annotated element to
     */
    String value();

    /**
     * @deprecated Do not use!
     */
    @Deprecated
    @Retention(RetentionPolicy.SOURCE)
    @Target( { ElementType.TYPE, ElementType.METHOD, ElementType.FIELD } )
    @interface Repeated {
        Entrypoint[] value();
    }
}
