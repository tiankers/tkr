package dev.tkr.logic.strength;

import dev.tkr.data.component.TkrComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 「力量下限」判定。
 *
 * <p>属于<b>逻辑层</b>，是纯判定逻辑，不订阅事件、不碰客户端。
 *
 * <h2>语义切分（重要）</h2>
 * <p>用户原话是「让物品有<b>使用或穿戴</b>的力量属性下限」，这两者在本项目里分开实现：
 * <ul>
 *   <li><b>武器 → 使用门槛</b>：由 {@link StrengthDamageHandler} 在伤害事件中拦截。
 *       力量不足时可以拿在手里，但<b>挥不动</b>（攻击不产生伤害）。</li>
 *   <li><b>护具 → 穿戴门槛</b>：<b>未实现</b>。原版
 *       {@code LivingEquipmentChangeEvent} 的 javadoc 明确写着
 *       <i>"This event is not ICancellableEvent"</i>，<b>不能</b>用它阻止穿戴。</li>
 * </ul>
 *
 * <p>组件缺失时一律视为无要求（下限 0），保证没有该组件的物品行为完全不变。
 */
public final class MinStrengthGate {

    private MinStrengthGate() {}

    /**
     * 读取物品要求的力量下限。
     *
     * @param stack 待检查物品
     * @return 要求的最低力量；组件缺失或非正时返回 {@code 0.0}
     */
    public static double requiredStrength(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;
        Double required = stack.get(TkrComponents.MIN_STRENGTH);
        if (required == null || required <= 0.0) return 0.0;
        return required;
    }

    /**
     * 判断实体当前力量是否达到物品要求。
     *
     * @param entity 使用者（可为 {@code null}，此时视为不满足）
     * @param stack  待使用物品
     * @return 满足要求返回 {@code true}；无要求时恒为 {@code true}
     */
    public static boolean meets( LivingEntity entity, ItemStack stack) {
        double required = requiredStrength(stack);
        if (required <= 0.0) return true;
        if (entity == null) return false;
        double strength = entity.getAttributeValue(dev.tkr.data.attribute.TkrAttributes.STRENGTH);
        return strength >= required;
    }

    /**
     * 供提示使用的「缺口」值。
     *
     * @return 还差多少力量；已满足或无要求时返回 {@code 0.0}，便于直接判空显示
     */
    public static double shortfall(LivingEntity entity, ItemStack stack) {
        double required = requiredStrength(stack);
        if (required <= 0.0) return 0.0;
        double strength = entity == null
                ? 0.0
                : entity.getAttributeValue(dev.tkr.data.attribute.TkrAttributes.STRENGTH);
        return Math.max(0.0, required - strength);
    }
}
