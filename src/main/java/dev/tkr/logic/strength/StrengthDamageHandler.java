package dev.tkr.logic.strength;

import dev.tkr.TkrMod;
import dev.tkr.data.attribute.TkrAttributes;
import dev.tkr.data.component.StrengthDamage;
import dev.tkr.data.component.TkrComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 力量 → 伤害结算，以及武器「使用门槛」拦截。
 *
 * <p>属于<b>逻辑层</b>，服务端执行。
 *
 * <h2>挂载点</h2>
 * <p>{@code LivingIncomingDamageEvent} 的 javadoc 原文：<i>"fired when a LivingEntity is
 * about to receive damage... after invulnerability checks but <b>before any damage
 * processing/mitigation</b>"</i>，并提供可变 {@code DamageContainer}。因此在护甲/附魔减免
 * <b>之前</b>改伤害。
 *
 * <h2>结算模型</h2>
 * <pre>
 *   最终伤害 = 原始伤害 + D
 *   D 由 StrengthMath.bonusDamage(力量, 额值 T, 系数 p) 给出
 * </pre>
 * <b>p 与 T 来自武器上的 {@code tkr:strength_damage} 组件</b>，因此每件武器可以不同；
 * 没有该组件的武器<b>完全不受影响</b>（这是模组内容外的物品保持原版行为的关键）。
 *
 * <p><b>支持近战与远程</b>：近战取攻击者手持武器；远程取射出弹丸的武器（弓弩由箭矢记录），
 * 投掷类投射物则回退到投掷者的主手物品。判定细节见 {@link #resolveWeapon}。
 *
 * <h2>耐久（整数规则，比较 D）</h2>
 * <pre>
 *   D &lt; 额值 → 扣 1 点
 *   D ≥ 额值 → 扣 2 点
 * </pre>
 *
 * <h2>为什么用 {@code @EventBusSubscriber}</h2>
 * <p>实测教训：{@code LivingIncomingDamageEvent} 属于<b>游戏总线</b>，注册到模组总线会抛
 * {@code IModBusEvent} 校验异常并导致模组加载失败。该注解 {@code bus()} 默认即 {@code GAME}。
 * 详见 {@code PITFALLS.md} P-019。
 */
@EventBusSubscriber(modid = TkrMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class StrengthDamageHandler {

    private StrengthDamageHandler() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!StrengthConfig.enabled()) return;

        var source = event.getSource();

        // 攻击者限定为玩家（生物也有力量属性，但本机制只对玩家结算）
        if (!(source.getEntity() instanceof LivingEntity attacker)) return;
        if (!(attacker instanceof Player)) return;

        // 解析「造成这次伤害的武器」——近战与远程取法不同，见 resolveWeapon
        ItemStack weapon = resolveWeapon(source, attacker);
        if (weapon.isEmpty()) return;

        // 该武器必须声明了力量伤害组件，否则完全不动这次伤害
        StrengthDamage component = weapon.get(TkrComponents.STRENGTH_DAMAGE);
        if (component == null) return;

        // 武器「使用门槛」：力量不足则本次攻击不产生伤害
        if (!MinStrengthGate.meets(attacker, weapon)) {
            event.setCanceled(true);
            return;
        }

        double strength = attacker.getAttributeValue(TkrAttributes.STRENGTH);
        if (strength <= 0.0) return;

        double bonus = StrengthMath.bonusDamage(strength, component.threshold(), component.bonusRatio());
        if (bonus <= 0.0 || !Double.isFinite(bonus)) return;

        // 防御：原始伤害 + 增量在 float 下可能溢出成 Infinity。
        // Infinity 是「合法」的 float，会被写进存档并污染后续计算，因此在这里夹住。
        double sum = (double) event.getAmount() + bonus;
        event.setAmount(sum >= Float.MAX_VALUE ? Float.MAX_VALUE : (float) sum);

        // 耐久比较的是这个伤害增量 D（不是力量，也不是总伤害）
        applyDurability(weapon, bonus, component.threshold());
    }

    /**
     * 解析造成本次伤害的武器，同时支持近战与远程。
     *
     * <p><b>为什么不能只用 {@code DamageSource.getWeaponItem()}</b>：其实现是
     * <pre>
     *   return this.directEntity != null ? this.directEntity.getWeaponItem() : null;
     * </pre>
     * 它问的是<b>直接伤害来源实体</b>。近战时那是攻击者本身，返回手持武器；
     * 但远程时那是<b>箭矢</b>——取到的是 {@code AbstractArrow.getWeaponItem()}（弓或弩），
     * 而雪球这类普通投射物的 {@code getWeaponItem()} 返回空，于是远程伤害会被整体漏掉。
     *
     * <p>因此分两种情形：
     * <ol>
     *   <li><b>投射物伤害</b>（{@code DamageTypeTags.IS_PROJECTILE}）：优先取
     *       {@code directEntity.getWeaponItem()}（箭矢记录了射出它的弓弩）；
     *       若为空（如投掷的三叉戟、雪球），则回退到射击者<b>主手</b>的物品。</li>
     *   <li><b>其余（近战等）</b>：直接用 {@code source.getWeaponItem()}。</li>
     * </ol>
     *
     * <p>回退到主手是必须的：投掷类投射物不会记录武器，而玩家投掷时主手拿的正是那件投掷物，
     * 因此主手就是正确的「武器」。
     *
     * @param source   伤害来源
     * @param attacker 伤害的造成者
     * @return 武器物品栈；无法确定时返回 {@link ItemStack#EMPTY}
     */
    private static ItemStack resolveWeapon(net.minecraft.world.damagesource.DamageSource source,
                                           LivingEntity attacker) {
        boolean isProjectile = source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE);

        if (isProjectile) {
            Entity direct = source.getDirectEntity();
            if (direct != null) {
                ItemStack fromProjectile = direct.getWeaponItem();
                if (fromProjectile != null && !fromProjectile.isEmpty()) {
                    return fromProjectile;
                }
            }
            // 投掷类投射物不记录武器，回退到射击者主手
            return attacker.getMainHandItem();
        }

        ItemStack weapon = source.getWeaponItem();
        return weapon == null ? ItemStack.EMPTY : weapon;
    }

    /**
     * 按整数规则扣耐久。
     *
     * <p>{@code UNBREAKABLE} 组件类型是 {@code record Unbreakable(boolean showInTooltip)}，
     * <b>不是</b> Boolean，故用 {@code has(...)} 判存在。
     *
     * @param weapon        武器
     * @param bonus         本次造成的伤害增量 D
     * @param threshold     伤害额值 T（来自该武器的组件）
     */
    private static void applyDurability(ItemStack weapon, double bonus, double threshold) {
        if (!weapon.isDamageableItem()) return;
        if (weapon.has(DataComponents.UNBREAKABLE)) return;

        int cost = StrengthMath.durabilityCost(bonus, threshold);
        int current = weapon.getDamageValue();
        int max = weapon.getMaxDamage();
        weapon.setDamageValue(Math.min(max, current + cost));
    }
}
