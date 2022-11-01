package study.father_son

import kotlinx.coroutines.*
import kotlin.concurrent.thread

/**
 * ...
 * @author 985892345 (Guo Xiangrui)
 * @email guo985892345@foxmail.com
 * @date 2022/10/30 1:08
 */

/**
 * 该 Test 为 SupervisorJob 的不同创建和使用方式
 *
 * 这个与 SupervisorJob1Test 有些区别，我把 SupervisorJob() 放在了 launch() 里面
 *
 * 经过实践得出一下结论
 * - launch111 抛出异常，launch 11 被取消了，所以 launch111 与 launch11 是协同关系
 * - 接上，launch11 被取消，但 launch1 却没有被取消，说明 launch11 与 launch1 是主从关系
 * - SupervisorJob() 写在 launch() 里面等同于 [test2] 写法
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
    val launch1 = launch {
      val supervisorJob = SupervisorJob(coroutineContext[Job])
      /**
       * SupervisorJob() 被放在了 launch() 里面
       */
      val launch11 = launch(supervisorJob) {
        val launch111 = launch {
          delay(1000)
          println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
                    "launch111 throw")
          throw RuntimeException()
        }
        observeCancel("launch11")
      }
      val launch12 = launch(supervisorJob) {
        observeCancel("launch12") // 不会打印
      }
      supervisorJob.join()
      println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
        "join?")
      observeCancel("launch1")  // 不会打印
    }
  }
}

private fun test2() {
  GlobalScope.launch {
    val launch1 = launch {
      val coroutineScope = CoroutineScope(SupervisorJob(coroutineContext[Job]))
      val launch11 = coroutineScope.launch {
        val launch111 = launch {
          delay(1000)
          println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
                    "launch111 throw")
          throw RuntimeException()
        }
        observeCancel("launch11")
      }
      val launch12 = launch {
        observeCancel("launch12") // 不会打印
      }
      observeCancel("launch1") // 不会打印
    }
  }
}