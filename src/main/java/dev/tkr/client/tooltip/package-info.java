/**
 * 物品提示渲染（表现层）。
 *
 * <p>只负责把逻辑层算好的数据渲染成文字，判定逻辑在
 * {@code dev.tkr.logic.strength.MinStrengthGate}。这体现了四层结构的要求：
 * 逻辑层发布数据，表现层消费，<b>反向依赖被禁止</b>。
 */
package dev.tkr.client.tooltip;
