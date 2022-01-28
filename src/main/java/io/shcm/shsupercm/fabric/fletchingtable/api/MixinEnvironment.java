package io.shcm.shsupercm.fabric.fletchingtable.api;

import java.lang.annotation.*;

/**
 * Overrides the environment of the annotated mixin to be registered automatically by Fletching Table.<br>
 * The default value is applied to any class annotated with @Mixin regardless of this annotation.<br>
 * The default environment can be set in FletchingTable's configuration.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface MixinEnvironment {
	/**
	 * Overrides the default environment for the annotated mixin.<br>
	 * The default environment can be set in FletchingTable's configuration.
	 * @return physical environment to inject this mixin on
	 */
	Env value() default Env.NONE;
	
	/**
	 * Physical environment to inject this mixin on.
	 */
	enum Env {
		/**
		 * Completely skips automatic registry.
		 */
		NONE("none"),
		/**
		 * Automatically determines which environment the annotated mixin belongs to based on its target's package.<br>
		 * Targets in `net.minecraft.client` are set to CLIENT and the rest are set to MIXINS.
		 */
		AUTO("auto"),
		/**
		 * The annotated mixin will be automatically registered to the "common" mixins config which applies on both sides.
		 */
		MIXINS("mixins"),
		/**
		 * The annotated mixin will be automatically registered to the client mixins config which applies only on clients.
		 */
		CLIENT("client"),
		/**
		 * The annotated mixin will be automatically registered to the server mixins config which applies only on dedicated servers.
		 */
		SERVER("server");

		public final String configName;

		Env(String configName) {
			this.configName = configName;
		}
	}
}