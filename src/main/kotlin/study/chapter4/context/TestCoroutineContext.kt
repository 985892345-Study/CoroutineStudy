package study.chapter4.context

import kotlinx.coroutines.delay
import java.lang.Thread.sleep
import kotlin.concurrent.thread
import kotlin.coroutines.*

/**
 * .
 *
 * @author 985892345 (Guo Xiangrui)
 * @date 2022/10/20 14:25
 */
class TestCoroutineContext : AbstractCoroutineContextElement(Key) {
  companion object Key : CoroutineContext.Key<TestCoroutineContext>
}

/*
* startCoroutine 可以看成是启动了一个协程作用域
*
* 而对于 suspend fun test1() 就是普通的一个挂起函数
*
* test2 虽然也是一个挂起函数，但它使用了suspendCoroutine()，生成了一个挂起点
* */
fun main() {
  suspend {
    coroutineContext
    test1()
    test2()
    1
  }.startCoroutine(
    object : Continuation<Int> {
      override val context: CoroutineContext
        get() = TestCoroutineContext()

      override fun resumeWith(result: Result<Int>) {
        context[TestCoroutineContext]?.key
      }
    }
  )
}

private suspend fun test1() {
  delay(100)
}

private suspend fun test2() = suspendCoroutine {
  thread {
    sleep(100)
    it.resume(Unit)
  }
}