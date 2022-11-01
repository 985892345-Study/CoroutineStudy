package study.father_son

import kotlinx.coroutines.*
import kotlin.concurrent.thread

/**
 * ...
 * @author 985892345 (Guo Xiangrui)
 * @email guo985892345@foxmail.com
 * @date 2022/10/30 0:10
 */

/**
 * 通过下面这个测试，可以看到：
 * - launch 默认创建 StandaloneCoroutine 协程
 * - launch 里面开 launch，则它们之间是协同关系，子协程的异常会取消父协程，父协程取消又会取消所有子协程，
 *   如果父协程与父父协程也是协同关系，那么就会递归取消所有有协同关系的协程，就像病毒一样
 */
fun main() {
  GlobalScope.launch {
    println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
              "launch: $coroutineContext")
    val launch1 = launch {
      println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
                "launch1: $coroutineContext")
      launch {
        println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
                  "launch11: $coroutineContext")
        observeCancel("launch11")
      }
      launch {
        println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
                  "launch12: $coroutineContext")
        observeCancel("launch12")
      }
      launch {
        println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
                  "launch13: $coroutineContext")
        launch {
          println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
                    "launch131: $coroutineContext")
        }
        /**
         * 可以发现，在这里设置的异常处理器并不会被调用
         */
        launch(
          CoroutineExceptionHandler { coroutineContext, throwable ->
            println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
              "CoroutineExceptionHandler")
          }
        ) {
          println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
                    "launch132: $coroutineContext")
          delay(200)
          println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
                    "launch132 throw")
          throw RuntimeException()
        }
        observeCancel("launch13")
      }
      observeCancel("launch1")
    }
    launch {
      println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
                "launch2: $coroutineContext")
      launch {
        println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
                  "launch21: $coroutineContext")
        observeCancel("launch21")
      }
      observeCancel("launch2")
    }
    delay(3000)
    launch1.cancel()
  }
  thread {
    while (true);
  }
}

suspend fun observeCancel(tag: String) = suspendCancellableCoroutine<Unit> {
  it.invokeOnCancellation {
    println(".(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
              "tag = $tag   Cancel")
  }
}