package wraith.smithee.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import wraith.smithee.properties.Trait;
import wraith.smithee.properties.TraitType;
import wraith.smithee.registry.StatusEffectRegistry;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    public abstract boolean addStatusEffect(StatusEffectInstance effect);

    @ModifyArgs(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;applyDamage(Lnet/minecraft/entity/damage/DamageSource;F)V"))
    private void applyDamage(Args args, DamageSource source, float amount) {
        if (source.getAttacker() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) source.getAttacker();
            boolean reduceDamage = false; //TODO add a trait for this
            if (reduceDamage) {
                args.set(1, 0); // REMEMBER TO DO THIS
            }
            if (Trait.hasTrait(player.getMainHandStack(), TraitType.CHILLING)) {
                addStatusEffect(new StatusEffectInstance(StatusEffectRegistry.STATUS_EFFECTS.get("frostbite"), Trait.getFrostbiteEffectDuration(player.getMainHandStack())));
                //TODO just a review, this get seem not great (hardcoded string)... any ideas?
            }
        }
    }

}
