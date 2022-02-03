package shcm.shsupercm.fabric.shutupdrasil.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class ShutTheFuckUpDrasil {
    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "createUserApiService", cancellable = true, at = @At(value = "HEAD"))
    public void ohMyFuckingGlobShutTheFuckUpImRunningInADevelopmentEnvironment(YggdrasilAuthenticationService authService, RunArgs runArgs, CallbackInfoReturnable<UserApiService> cir) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            LOGGER.error("Failed to verify authentication");
            cir.setReturnValue(UserApiService.OFFLINE);
        }
    }
}
