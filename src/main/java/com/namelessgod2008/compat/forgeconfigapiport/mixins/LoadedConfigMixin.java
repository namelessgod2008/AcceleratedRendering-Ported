package com.namelessgod2008.compat.forgeconfigapiport.mixins;

import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.file.Path;

/**
 * Workaround for C2ME bundling night-config 3.6.5 which lacks
 * {@link WritingMode#REPLACE_ATOMIC}. forgeconfigapiport's ConfigTracker
 * references REPLACE_ATOMIC in its bytecode, causing a NoSuchFieldError
 * when the old night-config is loaded.
 * <p>
 * This mixin wraps the writeConfig call from LoadedConfig.save() and
 * catches the NoSuchFieldError, falling back to the older REPLACE mode.
 */
@Pseudo
@Mixin(targets = "net.neoforged.fml.config.LoadedConfig", remap = false)
public class LoadedConfigMixin {

	@WrapOperation(
		method = "save",
		at = @At(
			value = "INVOKE",
			target = "Lnet/neoforged/fml/config/ConfigTracker;writeConfig(Ljava/nio/file/Path;Lcom/electronwill/nightconfig/core/UnmodifiableCommentedConfig;)V"
		),
		require = 0
	)
	public void wrapWriteConfig(Path path, UnmodifiableCommentedConfig config, Operation<Void> original) {
		try {
			original.call(path, config);
		} catch (NoSuchFieldError e) {
			// C2ME's night-config 3.6.5 is missing WritingMode.REPLACE_ATOMIC.
			// Fall back to the non-atomic REPLACE mode.
			try {
				new TomlWriter().write(config, path, WritingMode.valueOf("REPLACE"));
			} catch (Exception suppressed) {
				suppressed.printStackTrace();
			}
		}
	}
}
