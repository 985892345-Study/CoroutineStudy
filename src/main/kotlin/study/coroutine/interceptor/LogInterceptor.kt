package study.coroutine.interceptor

import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

/**
 * .
 *
 * @author 985892345 (Guo Xiangrui)
 * @date 2022/10/20 14:40
 */
class LogInterceptor : ContinuationInterceptor {
  override val key: CoroutineContext.Key<*>
    get() = ContinuationInterceptor

  override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
    return LoginContinuation(continuation)
  }
}

class LoginContinuation<T>(
  private val continuation: Continuation<T>
) : Continuation<T> by continuation {
  override fun resumeWith(result: Result<T>) {
    println("before")
    continuation.resumeWith(result)
    println("after")
  }
}