package study.channel

import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.supervisorScope

/**
 * .
 *
 * @author 985892345 (Guo Xiangrui)
 * @date 2022/10/26 16:43
 */
suspend fun main() {
  val channel = Channel<Int>()
  channel.iterator()
}