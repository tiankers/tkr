package dev.tkr.logic.strength;

import dev.tkr.data.attribute.TkrAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

/**
 * 把力量属性注入<b>全体生物</b>。
 *
 * <p>属于<b>逻辑层</b>。这里的「全体生物」= 所有在 {@code DefaultAttributes} 中拥有
 * 属性表的实体类型，也就是原版可正常生成的全部 {@link LivingEntity}（含玩家、怪物、
 * 动物、盔甲架等）。
 *
 * <h2>为什么能一次覆盖全部</h2>
 * <p>NeoForge 提供 {@link EntityAttributeModificationEvent}，其 javadoc 原文：
 * <i>"Use this event to add attributes to existing entity types. This event is fired after
 * registration and before common setup, and after EntityAttributeCreationEvent. Fired on the
 * Mod bus."</i>
 *
 * <p>该事件的 {@code getTypes()} 已经预过滤为 {@code DefaultAttributes::hasSupplier} 的实体，
 * 所以直接遍历即可，不需要逐个实体硬编码，也不会误改没有属性表的实体。
 *
 * <p>与 {@link dev.tkr.data.attribute.TkrAttributes#STRENGTH} 的分工：那边只声明属性，
 * 这边只负责注入，玩法计算在 {@link StrengthMath} 与 {@link StrengthDamageHandler}。
 */
public final class StrengthAttributes {

    private StrengthAttributes() {}

    /**
     * 事件回调：为每个拥有属性表的实体类型添加力量属性。
     *
     * <p>使用三参数 {@code add(type, attribute, value)} 并显式传默认值，
     * 而不是依赖属性的默认值，使「注入值」在代码里直接可见、便于日后调整。
     */
    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            event.add(type, TkrAttributes.STRENGTH, StrengthConfig.defaultStrength());
        }
    }
}
