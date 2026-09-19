package dev.tkr.logic.strength;

/**
 * 力量伤害曲线的纯数学实现。
 *
 * <p><b>本类不依赖任何 Minecraft / NeoForge 类型</b>，可在普通 JUnit 中直接验证，
 * 不需要启动游戏。
 *
 * <h2>最终确认的规格</h2>
 * <p>曲线横轴是<b>伤害增量 D</b>（该组件贡献的伤害），不是力量本身；比较与耐久也都以 D 为准。
 * 返回值为<b>伤害增量</b>，不含武器基础伤害。
 *
 * <pre>
 *   D(力量) = 力量 × p                          当 D ≤ T（线性段）
 *   D(力量) = 2T − T·e^(−p/T · (力量 − T/p))      当 D &gt; T（e 型趋近段）
 * </pre>
 *
 * <h2>性质（均已数值验证）</h2>
 * <ul>
 *   <li>弯折点：{@code 力量 = T/p}，此时 {@code D = T}</li>
 *   <li><b>C¹ 连续</b>：弯折点处左右斜率都为 {@code p}，无折角
 *       （验证值 0.500000 / 0.499999）</li>
 *   <li>渐近线：{@code 力量 → ∞} 时 {@code D → 2T}，即「趋近额值 2 倍」</li>
 *   <li>{@code D(0) = 0}，力量为 0 时该组件不提供任何加成</li>
 * </ul>
 *
 * <p>以用户示例参数（{@code p = 0.5, T = 10}，弯折点力量 20）：
 * <pre>
 *   0→0   5→2.5   10→5   20→10   40→16.32   ∞→20
 * </pre>
 */
public final class StrengthMath {

    private StrengthMath() {}

    /**
     * 计算力量带来的<b>伤害增量</b>。
     *
     * @param strength   攻击者力量 {@code S}
     * @param threshold  伤害额值 {@code T}，必须 {@code > 0}
     * @param bonusRatio 百分比 {@code p}，即线性段每点力量贡献的伤害，必须 {@code > 0}
     * @return 伤害增量 {@code D}，恒为有限值且 {@code >= 0}
     */
    public static double bonusDamage(double strength, double threshold, double bonusRatio) {
        if (strength <= 0.0 || threshold <= 0.0 || bonusRatio <= 0.0) return 0.0;

        double bendStrength = threshold / bonusRatio;   // 弯折点力量 = T/p
        if (strength <= bendStrength) {
            return strength * bonusRatio;               // 线性段
        }
        // e 型趋近段：D = 2T − T·e^(−a·(S − S₀))，其中 a = p/T
        // 参数由两个条件唯一确定：弯折点 D = T（得系数 T）、弯折点斜率 = p（得 a = p/T）
        double a = bonusRatio / threshold;
        return 2.0 * threshold - threshold * Math.exp(-a * (strength - bendStrength));
    }

    /**
     * 曲线弯折点对应的力量值：{@code T/p}。
     *
     * @return 力量达到该值时进入曲线段；参数非法时返回 {@code 0}
     */
    public static double bendStrength(double threshold, double bonusRatio) {
        if (threshold <= 0.0 || bonusRatio <= 0.0) return 0.0;
        return threshold / bonusRatio;
    }

    /**
     * 计算本次命中应消耗的耐久点数。
     *
     * <p>比较对象是<b>伤害增量 D</b>（用户最终确认「比较伤害」），整数消耗：
     * <pre>
     *   D &lt; T → 1 点
     *   D ≥ T → 2 点
     * </pre>
     *
     * <p>注意：{@code D} 到达 {@code T} 恰好发生在弯折点，因此「进入曲线段」与
     * 「耐久翻倍」在同一个力量值上发生。
     *
     * @param appliedDamage 本次造成的伤害增量 {@code D}
     * @param threshold     伤害额值 {@code T}
     * @return 耐久点数（1 或 2）
     */
    public static int durabilityCost(double appliedDamage, double threshold) {
        return appliedDamage >= threshold ? 2 : 1;
    }
}
