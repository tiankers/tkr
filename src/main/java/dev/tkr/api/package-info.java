/**
 * 公开契约层 —— 对外稳定、可被附属 mod 依赖的 API。
 *
 * <p>TKR 以 LGPL-3.0-only 发布：他人可以依赖本包（含闭源项目），
 * 也可以修改并重新分发，但修改后的衍生作品必须同样以 LGPL/GPL 开源。
 * 因此本层是<b>兼容性承诺</b>，不是内部实现的堆放处。
 *
 * <h2>职责</h2>
 * <ul>
 *   <li>{@code slot} —— 饰品槽位契约：槽位标识、槽位类型与数量上限。</li>
 *   <li>{@code capability} —— 能力契约：其他 mod 可实现的接口（如饰品行为、自定义效果）。</li>
 *   <li>{@code event} —— 对外事件：以 NeoForge 事件总线发布的可监听扩展点。</li>
 * </ul>
 *
 * <h2>依赖方向（严禁违反）</h2>
 * <p>本层 <b>禁止</b> import {@code dev.tkr.data}、{@code dev.tkr.logic}、{@code dev.tkr.client}，
 * 也禁止 import 任何客户端专用类。它只允许依赖 JDK、原版 Minecraft/NeoForge 的公共 API，
 * 以及必要的第三方公共 API。</p>
 *
 * <h2>修改纪律</h2>
 * <p>已发布的公开类型只能新增，不得改签名或删字段；破坏性变更必须走新的类型名或版本化。
 * 内部实现如需暴露给附属，先在 {@code logic} 中做实现，确认契约稳定后再提升到本层。</p>
 */
package dev.tkr.api;
