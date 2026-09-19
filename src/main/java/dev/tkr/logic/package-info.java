/**
 * 逻辑层 —— 「怎么运作」全部集中在这里，且必须服务端可独立运行。
 *
 * <h2>职责</h2>
 * <ul>
 *   <li>{@code strength} —— 力量机制：属性注入、伤害结算、使用门槛、指令。</li>
 *   <li>{@code effect} —— 状态效果、能力加成、数值结算。</li>
 *   <li>{@code machine} —— 方块实体、容器、Tick 逻辑等持续运转的系统。</li>
 *   <li>{@code event} —— 游戏总线事件订阅（各系统的接线点）。</li>
 *   <li>{@code network} —— 网络包定义、注册与双向处理。</li>
 * </ul>
 *
 * <h2>依赖方向（严禁违反）</h2>
 * <p>可依赖 {@code dev.tkr.api} 与 {@code dev.tkr.data}；
 * <b>禁止</b> import 任何 {@code dev.tkr.client} 或 {@code net.minecraft.client.*} 下的类型。
 * 需要表现层配合时，通过 {@code event} 发布事件或走 {@code network}，
 * 让 {@code client} 主动订阅，而不是由本层反向调用。</p>
 *
 * <h2>为什么这条最硬</h2>
 * <p>服务端专用环境的崩溃绝大多数来自「逻辑里偷偷 import 了客户端类」。
 * 本层的纯净性使该问题在编译期即可发现，而不是等到联机测试。</p>
 */
package dev.tkr.logic;
