package wraith.smithee.mixin;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wraith.smithee.items.tools.BaseSmitheePickaxe;
import wraith.smithee.items.tools.BaseSmitheeSword;
import wraith.smithee.items.tools.BaseSmitheeTool;
import wraith.smithee.properties.Properties;
import wraith.smithee.properties.ToolPartRecipe;
import wraith.smithee.registry.ItemRegistry;
import wraith.smithee.utils.Utils;

import java.util.HashMap;
import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow
    public abstract Item getItem();

    @Shadow
    @Nullable
    public abstract CompoundTag getSubTag(String key);

    @Shadow
    private CompoundTag tag;

    @Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
    public void getMaxDamage(CallbackInfoReturnable<Integer> cir) {
        if (Utils.isSmitheeTool(((ItemStack) (Object) this)) && tag != null && tag.contains("SmitheeProperties")) {
            CompoundTag tag = getSubTag("SmitheeProperties");
            cir.setReturnValue(tag.getInt("Durability"));
            cir.cancel();
        }
    }

    @Inject(method = "getTranslationKey", at = @At("HEAD"), cancellable = true)
    public void getTranslationKey(CallbackInfoReturnable<String> cir) {
        if (getItem() instanceof BaseSmitheeTool && tag != null && tag.contains("Parts")) {
            CompoundTag tag = getSubTag("Parts");
            String head = tag.getString("HeadPart");
            cir.setReturnValue("items.smithee.tools." + head + "_" + Utils.getToolType(getItem()));
        }
    }

    @Inject(method = "getMiningSpeedMultiplier", at = @At("HEAD"), cancellable = true)
    public void getMiningSpeedMultiplier(BlockState state, CallbackInfoReturnable<Float> cir) {
        if (getItem() instanceof BaseSmitheeTool && !(getItem() instanceof BaseSmitheeSword) && tag != null && tag.contains("SmitheeProperties")) {
            CompoundTag tag = getSubTag("SmitheeProperties");
            float mineSpeed = ((MiningToolItemAccessor) getItem()).getEffectiveBlocks().contains(state.getBlock()) ? tag.getFloat("MiningSpeed") : 1.0F;
            if (getItem() instanceof AxeItem) {
                mineSpeed = ((AxeItemAccessor) getItem()).getEffectiveMaterials().contains(state.getMaterial()) ? tag.getFloat("MiningSpeed") : mineSpeed;
            } else if (getItem() instanceof SwordItem) {
                mineSpeed = (getItem().isEffectiveOn(state)) ? tag.getFloat("MiningSpeed") : mineSpeed;
            }
            cir.setReturnValue(mineSpeed);
            cir.cancel();
        }
    }

    @Inject(method = "isEffectiveOn", at = @At("HEAD"), cancellable = true)
    public void isEffectiveOn(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (getItem() instanceof BaseSmitheeTool) {
            if (getItem() instanceof BaseSmitheePickaxe && tag != null && tag.contains("SmitheeProperties")) {
                CompoundTag tag = getSubTag("SmitheeProperties");
                int mineLevel = tag.getInt("MiningLevel");
                if (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.CRYING_OBSIDIAN) && !state.isOf(Blocks.NETHERITE_BLOCK) && !state.isOf(Blocks.RESPAWN_ANCHOR) && !state.isOf(Blocks.ANCIENT_DEBRIS)) {
                    if (!state.isOf(Blocks.DIAMOND_BLOCK) && !state.isOf(Blocks.DIAMOND_ORE) && !state.isOf(Blocks.EMERALD_ORE) && !state.isOf(Blocks.EMERALD_BLOCK) && !state.isOf(Blocks.GOLD_BLOCK) && !state.isOf(Blocks.GOLD_ORE) && !state.isOf(Blocks.REDSTONE_ORE)) {
                        if (!state.isOf(Blocks.IRON_BLOCK) && !state.isOf(Blocks.IRON_ORE) && !state.isOf(Blocks.LAPIS_BLOCK) && !state.isOf(Blocks.LAPIS_ORE)) {
                            Material material = state.getMaterial();
                            cir.setReturnValue(material == Material.STONE || material == Material.METAL || material == Material.REPAIR_STATION || state.isOf(Blocks.NETHER_GOLD_ORE));
                        } else {
                            cir.setReturnValue(mineLevel >= 1);
                        }
                    } else {
                        cir.setReturnValue(mineLevel >= 2);
                    }
                } else {
                    cir.setReturnValue(mineLevel >= 3);
                }
                cir.cancel();
            } else if (getItem() instanceof MiningToolItem) {
                cir.setReturnValue(((MiningToolItemAccessor) getItem()).getEffectiveBlocks().contains(state.getBlock()));
                cir.cancel();
            }
        }
    }

    @Inject(method = "getAttributeModifiers", at = @At("HEAD"), cancellable = true)
    public void getAttributeModifiers(EquipmentSlot equipmentSlot, CallbackInfoReturnable<Multimap<EntityAttribute, EntityAttributeModifier>> cir) {
        if (getItem() instanceof BaseSmitheeTool && tag != null && tag.contains("SmitheeProperties") && equipmentSlot == EquipmentSlot.MAINHAND) {
            CompoundTag tag = getSubTag("SmitheeProperties");
            ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
            builder.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(((ItemAccessor) getItem()).getAttackDamageModifierId(), getItem() instanceof BaseSmitheeSword ? "Tool modifier" : "Weapon modifier", tag.getFloat("AttackDamage") + Properties.getExtraDamage(Utils.getToolType(getItem())), EntityAttributeModifier.Operation.ADDITION));
            builder.put(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(((ItemAccessor) getItem()).getAttackSpeedModifierId(), getItem() instanceof BaseSmitheeSword ? "Tool modifier" : "Weapon modifier", -4 + tag.getFloat("AttackSpeed") + Properties.getExtraAttackSpeed(Utils.getToolType(getItem())), EntityAttributeModifier.Operation.ADDITION));
            cir.setReturnValue(builder.build());
            cir.cancel();
        }
    }

    @Inject(method = "hasCustomName", at = @At("HEAD"), cancellable = true)
    public void hasCustomName(CallbackInfoReturnable<Boolean> cir) {
        if (getItem() instanceof BaseSmitheeTool) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    public void getName(CallbackInfoReturnable<Text> cir) {
        if (getItem() instanceof BaseSmitheeTool) {
            if (tag != null && tag.contains("SmitheeProperties") && tag.getCompound("SmitheeProperties").contains("CustomName")) {
                cir.setReturnValue(new LiteralText(tag.getCompound("SmitheeProperties").getString("CustomName")));
            } else if (tag != null && tag.contains("Parts")) {
                cir.setReturnValue(new LiteralText(Utils.capitalize(tag.getCompound("Parts").getString("HeadPart")) + " " + Utils.capitalize(Utils.getToolType(getItem()))));
            } else {
                cir.setReturnValue(new LiteralText("Base Smithee " + Utils.capitalize(Utils.getToolType(getItem()))));
            }
        }
    }

    @ModifyVariable(method = "getTooltip", at = @At("RETURN"))
    public List<Text> getTooltip(List<Text> list, PlayerEntity player, TooltipContext context) {
        if (ItemRegistry.TOOL_PART_RECIPES.containsKey(getItem())) {
            HashMap<String, ToolPartRecipe> recipes = ItemRegistry.TOOL_PART_RECIPES.get(getItem());
            if (Screen.hasShiftDown()) {
                list.add(new LiteralText("§dTool material:"));
                list.add(new LiteralText("§9Head - Takes " + recipes.get("head").requiredAmount));
                list.add(new LiteralText("§9Binding - Takes " + recipes.get("binding").requiredAmount));
                list.add(new LiteralText("§9Handle - Takes " + recipes.get("handle").requiredAmount));
            } else {
                list.add(new LiteralText("§5Tool material - Hold §1[§dSHIFT§1] §5for info."));
            }
            return list;
        }
        return list;
    }
}