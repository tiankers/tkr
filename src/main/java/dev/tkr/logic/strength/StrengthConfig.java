package dev.tkr.logic.strength;

/**
 * TKR 力量机制的全局开关与默认值。
 *
 * <p>属于<b>逻辑层</b>。
 *
 * <h2>p 与 T 为什么不在这里</h2>
 * <p>「伤害系数 p」与「伤害额值 T」是<b>每件武器各自的数据</b>，存放在武器的
 * {@code tkr:strength_damage} 组件上（见 {@code dev.tkr.data.component.StrengthDamage}）。
 * 因此这里不再有全局阈值/系数 —— 没有该组件的武器完全不受本机制影响。
 *
 * <p>当前以常量形式提供，尚未接入 config 文件；接入后这些会变成可配置项。
 */
public final class StrengthConfig {

    /**
     * 属性上限的放宽目标值。
     *
     * <p>取 {@code 1.0E20}。<b>为什么不用 {@code Double.MAX_VALUE}（约 1.8E308）</b>：
     * 属性修饰符有加法与乘法，若上限取 {@code Double.MAX_VALUE}，
     * 两次相乘就会溢出成 {@code Infinity}，生命值等变为无限并破坏存档与实体逻辑。
     * {@code 1.0E20} 的平方是 {@code 1.0E40}，距离 double 溢出上限还有极大约量，
     * 因此即使多层乘法叠加也不会溢出。
     *
     * <p>本值同时用于：① 原版全部属性的范围；② 本模组 {@code tkr:strength} 的范围；
     * ③ {@code tkr:min_strength} 与 {@code tkr:strength_damage} 组件的取值校验上界。
     * 改这一个常量即可整体调整。
     */
    public static final double ATTRIBUTE_MAX = 1.0e20;

    /** 是否同时放宽原版全部属性的范围（设为 false 则只放宽 TKR 自己的属性）。 */
    public static final boolean EXPAND_VANILLA_ATTRIBUTE_RANGES = true;

    /**
     * {@code /tkr strength_damage <p>} 省略 T 时使用的伤害额值默认值。
     *
     * <p>提供一个默认值是为了避免「必须同时手输两个数才能试一下效果」。
     * 与测试物品 {@code tkr:strength_blade} 的默认参数保持一致。
     */
    public static final double DEFAULT_STRENGTH_DAMAGE_THRESHOLD = 10.0;

    /** 全体生物力量属性的默认值。 */
    public static final double DEFAULT_STRENGTH = 0.0;

    /** 机制总开关，便于排查。 */
    public static final boolean ENABLED = true;

    private StrengthConfig() {}

    /** @return 属性上限的目标值 */
    public static double attributeMax() {
        return ATTRIBUTE_MAX;
    }

    /** @return 是否放宽原版属性范围 */
    public static boolean expandVanillaAttributeRanges() {
        return EXPAND_VANILLA_ATTRIBUTE_RANGES;
    }

    /** @return {@code /tkr strength_damage} 省略 T 时的默认额值 */
    public static double defaultStrengthDamageThreshold() {
        return DEFAULT_STRENGTH_DAMAGE_THRESHOLD;
    }

    /** @return 生物力量属性默认值 */
    public static double defaultStrength() {
        return DEFAULT_STRENGTH;
    }

    /** @return 机制是否启用 */
    public static boolean enabled() {
        return ENABLED;
    }

}
