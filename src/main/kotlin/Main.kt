import study.coroutine.context.TestCoroutineContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.startCoroutine

fun main() {
  suspend {
    coroutineContext
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