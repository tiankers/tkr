/**
 * 力量机制的数值与结算（逻辑层）。
 *
 * <p>本包集中实现 `tkr:strength` 属性的全部玩法语义。最终规格见
 * {@code .dsh/skills/tkr-context/references/strength-spec.md} —— <b>改动前必读</b>。
 *
 * <h2>包内职责划分</h2>
 * <ul>
 *   <li>{@code StrengthMath} —— 纯数学，<b>不依赖任何 Minecraft 类型</b>，可独立测试</li>
 *   <li>{@code StrengthConfig} —— 数值集中管理</li>
 *   <li>{@code StrengthAttributes} —— 把力量注入全体生物</li>
 *   <li>{@code StrengthDamageHandler} —— 伤害结算 + 武器使用门槛 + 耐久</li>
 *   <li>{@code MinStrengthGate} —— 力量下限判定</li>
 * </ul>
 *
 * <p>约束：本包<b>禁止</b> import 任何 {@code dev.tkr.client} 或
 * {@code net.minecraft.client.*}。需要表现层配合时通过事件让 {@code client} 主动订阅。
 */
package dev.tkr.logic.strength;
