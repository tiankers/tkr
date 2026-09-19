/**
 * 定义层 —— 只描述「有什么」，不描述「怎么运作」。
 *
 * <h2>职责</h2>
 * <ul>
 *   <li>{@code registry} —— {@code DeferredRegister}/{@code DeferredHolder} 汇总与注册入口。
 *       注册动作在 {@code RegisterEvent} 中触发，本层不主动调用注册。</li>
 *   <li>{@code item} / {@code block} —— 物品与方块的类型声明、属性、创造模式标签页归属。
 *       行为逻辑一律委托给 {@code logic}，本层不写玩法判定。</li>
 *   <li>{@code component} —— 数据组件（DataComponent）的声明与默认值。</li>
 *   <li>{@code codec} —— {@code Codec}/{@code StreamCodec} 与网络编解码定义。</li>
 * </ul>
 *
 * <h2>依赖方向（严禁违反）</h2>
 * <p>可依赖 {@code dev.tkr.api}；<b>禁止</b> import {@code dev.tkr.client}；
 * 尽量避免 import {@code dev.tkr.logic} —— 若确需回调，改用 {@code logic} 注册的处理器。</p>
 *
 * <h2>服务端安全</h2>
 * <p>本层在专用服务端同样加载，任何客户端类引用都会导致服务端启动即崩，务必保持纯净。</p>
 */
package dev.tkr.data;
