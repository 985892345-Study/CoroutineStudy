package study.father_son

import kotlinx.coroutines.*
import kotlin.concurrent.thread

/**
 * ...
 * @author 985892345 (Guo Xiangrui)
 * @email guo985892345@foxmail.com
 * @date 2022/10/30 0:44
 */

/**
 * 该 Test 为认识 SupervisorJob 与普通 Job 的 区别
 *
 * SupervisorJob
 * - 需要配合 CoroutineScope() 和 coroutineContext[Job] 一起使用
 * - SupervisorJob 的作用域创建出来的协程不会向父协程传播，从而保护其他兄弟协程和父协程
 *
 *
 * 可以看看这篇文章：https://zhuanlan.zhihu.com/p/269228572
 */
fun main() {
  test1()
//  test2()
  thread {
    while (true);
  }
}

private fun test1() {
  GlobalScope.launch {
    val coroutineScope = CoroutineScope(SupervisorJob(coroutineContext[Job]))
    coroutineScope.launch {
      launch {
        launch {
          delay(1000)
          println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
                    "launch111   throw")
          throw RuntimeException()
        }
        observeCancel("launch11")
      }
      observeCancel("launch1")
    }
    observeCancel("launch")
  }
}

private fun test2() {
  GlobalScope.launch {
    launch {
      launch {
        launch {
          delay(1000)
          println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
                    "launch111   throw")
          throw RuntimeException()
        }
        observeCancel("launch11")
      }
      observeCancel("launch1")
    }
    observeCancel("launch")
  }
}