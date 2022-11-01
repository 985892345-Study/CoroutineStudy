package study.father_son

import kotlinx.coroutines.*
import kotlin.concurrent.thread

/**
 * ...
 * @author 985892345 (Guo Xiangrui)
 * @email guo985892345@foxmail.com
 * @date 2022/10/30 0:37
 */

/**
 * supervisorScope 作用：
 * - 创建一个 SupervisorCoroutine 的作用域
 * - SupervisorCoroutine 的作用域可以拦截子协程的异常传播，从而保护其他子协程，也保护了父协程
 * - 但是 supervisorScope 是一个挂起函数，会导致父协程挂起，一直直到 supervisorScope 运行结束或者出异常（异常并不会向外传播）
 * - 想不挂起，可以使用 CoroutineScope(SupervisorJob(coroutineContext[Job]))
 * - test1() 中使用的 supervisorScope 等同于 test2() 中创建 SupervisorJob() 并手动 join()，也与 test3() 一样
 */
fun main() {
//  test1()
//  test2()
  test3()
  thread {
    while (true);
  }
}

private fun test1() {
  GlobalScope.launch {
    supervisorScope {
      val launch1 = launch {
        val launch11 = launch {
          val launch111 = launch {
            delay(1000)
            println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
                      "launch111 throw")
            throw RuntimeException()
          }
          observeCancel("launch11") // 会打印
        }
        observeCancel("launch1") // 会打印
      }
      observeCancel("supervisorScope") // 不会打印，所以 supervisorScope 并不会被取消
    }
    println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
      "join?") // 不会打印，因为被 supervisorScope 堵塞了。虽然 launch111 抛出了异常，但并不会导致 supervisorScope 终止
    observeCancel("launch") // 不会打印
  }
}

private fun test2() {
  GlobalScope.launch {
    val supervisorJob = SupervisorJob(coroutineContext[Job])
    /**
     * SupervisorJob() 被放在了 launch() 里面
     */
    val launch1 = launch(supervisorJob) {
      val launch11 = launch {
        val launch111 = launch {
          delay(1000)
          println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
                    "launch111 throw")
          throw RuntimeException()
        }
        observeCancel("launch11") // 会打印
      }
      observeCancel("launch1") // 会打印
    }
    supervisorJob.join()
    println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
              "join?") // 不会打印，因为被 supervisorJob 堵塞了。虽然 launch111 抛出了异常，但并不会导致 supervisorScope 终止
    observeCancel("launch1")  // 不会打印
  }
}

private fun test3() {
  GlobalScope.launch {
    val coroutineScope = CoroutineScope(SupervisorJob(coroutineContext[Job]))
    val launch1 = coroutineScope.launch {
      val launch11 = launch {
        val launch111 = launch {
          delay(1000)
          println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
                    "launch111 throw")
          throw RuntimeException()
        }
        observeCancel("launch11") // 会打印
      }
      observeCancel("launch1") // 会打印
    }
    coroutineScope.coroutineContext[Job]?.apply {
      println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
        "Job = $this") // 这里打印会是 SupervisorJobImpl
      join()
    }
    println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
              "join?") // 不会打印，因为被 coroutineScope 中的 Job 堵塞了。虽然 launch111 抛出了异常，但并不会导致 coroutineScope 中的 Job 终止
    observeCancel("launch1")  // 不会打印
  }
}