/**
 * 自定义属性注册（定义层）。
 *
 * <p>只声明「存在哪些属性」，不实现玩法。属性如何影响伤害由
 * {@code dev.tkr.logic.strength} 决定，如何注入生物由
 * {@code dev.tkr.logic.strength.StrengthAttributes} 决定。
 *
 * <p>约束：本包<b>禁止</b> import {@code dev.tkr.client}；
 * 也不应包含事件订阅（那属于 {@code logic}）。
 */
package dev.tkr.data.attribute;
