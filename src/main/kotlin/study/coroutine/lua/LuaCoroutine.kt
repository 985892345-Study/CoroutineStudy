package study.coroutine.lua

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.*

/**
 * .
 *
 * @author 985892345 (Guo Xiangrui)
 * @date 2022/10/20 16:40
 */
sealed class Status {
  class Created(val continuation: Continuation<Unit>): Status()
  class Yielded<P>(val continuation: Continuation<P>): Status()
  class Resumed<R>(val continuation: Continuation<R>): Status()
  object Dead: Status()
}

interface CoroutineScope<P, R> {
  val parameter: P?

  suspend fun yield(value: R): P
}

class Coroutine<P, R> (
  override val context: CoroutineContext = EmptyCoroutineContext,
  private val block: suspend CoroutineScope<P, R>.(P) -> R
): Continuation<R> {

  companion object {
    fun <P, R> create(
      context: CoroutineContext = EmptyCoroutineContext,
      block: suspend CoroutineScope<P, R>.(P) -> R
    ): Coroutine<P, R> {
      return Coroutine(context, block)
    }
  }

  private val scope = object : CoroutineScope<P, R> {
    override var parameter: P? = null

    override suspend fun yield(value: R): P = suspendCoroutine { continuation ->
      val previousStatus = status.getAndUpdate {
        when(it) {
          is Status.Created -> throw IllegalStateException("Never started!")
          is Status.Yielded<*> -> throw IllegalStateException("Already yielded!")
          // 这里收到了 for 循环中发送的新值，挂起 for 循环，
          // 并保存当前挂起点，用于后面恢复
          is Status.Resumed<*> -> Status.Yielded(continuation)
          Status.Dead -> throw IllegalStateException("Already dead!")
        }
      }

      println("第二个挂起点：${System.identityHashCode(continuation)}")
      // 协程挂起时返回当前值，这里会给 第一个挂起点 return 值，即 resume() 返回当前 value
      (previousStatus as? Status.Resumed<R>)?.continuation?.resume(value)
    }
  }

  private val status: AtomicReference<Status>

  val isActive: Boolean
    get() = status.get() != Status.Dead

  init {
    val coroutineBlock: suspend CoroutineScope<P, R>.() -> R = { block(parameter!!) }
    /*
    * 注意：这里是创建了一个新的协程，但这个协程持有当前 Coroutine<P, R> 类的引用
    * */
    val start = coroutineBlock.createCoroutine(scope, this)
    status = AtomicReference(Status.Created(start))
  }

  /**
   * 协程作用域完全结束时回调
   *
   * 因为自身实现了 Continuation<R>，并且在 init 中传递给了 coroutineBlock 使用，
   * 所以会在 coroutineBlock 完全执行完时才会回调
   * 即 for 循环完全执行完时
   *
   * 因为 for 循环完全执行完，此时 state 为调用 resume() 后所修改的 Resumed 状态（因为已经没有 yield() 可以调用了），
   * 所以 result 就为最后返回的 200
   */
  override fun resumeWith(result: Result<R>) {
    val previousStatus = status.getAndUpdate {
      when(it) {
        is Status.Created -> throw IllegalStateException("Never started!")
        is Status.Yielded<*> -> throw IllegalStateException("Already yielded!")
        is Status.Resumed<*> -> {
          Status.Dead
        }
        Status.Dead -> throw IllegalStateException("Already dead!")
      }
    }
    (previousStatus as? Status.Resumed<R>)?.continuation?.resumeWith(result)
  }

  /**
   * 经过 log 证明，resume() 和 yield() 的 continuation 并不是同一个对象
   * 意味着每一次调用 suspendCoroutine() 都会生成一个新的 continuation 来记录挂起点
   */
  suspend fun resume(value: P): R = suspendCoroutine { continuation ->
    val previousStatus = status.getAndUpdate {
      when(it) {
        is Status.Created -> {
          scope.parameter = value // 这个值没有使用
          Status.Resumed(continuation)
        }
        is Status.Yielded<*> -> {
          Status.Resumed(continuation)
        }
        is Status.Resumed<*> -> throw IllegalStateException("Already resumed!")
        Status.Dead -> throw IllegalStateException("Already dead!")
      }
    }

    // 第一个挂起点
    println("第一个挂起点：${System.identityHashCode(continuation)}")
    // previousStatus 为上一次协程状态
    when(previousStatus){
      // 如果上一次状态是 Created，则会执行 init 中的 coroutineBlock
      // 然后就开始初始化 for 循环，最后遇到第一次 yield() 调用时挂起 for 循环
      is Status.Created -> previousStatus.continuation.resume(Unit)
      // 这里 Yielded 表明已经初始化了 for 循环，会给 第二个挂起点 return 值，用于恢复 第二个挂起点
      is Status.Yielded<*> -> (previousStatus as Status.Yielded<P>).continuation.resume(value)
      else -> {}
    }
  }

  suspend fun <SymT> SymCoroutine<SymT>.yield(value: R): P {
    return scope.yield(value)
  }
}

class Dispatcher: ContinuationInterceptor {
  override val key = ContinuationInterceptor

  private val executor = Executors.newSingleThreadExecutor()

  override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
    return DispatcherContinuation(continuation, executor)
  }
}

class DispatcherContinuation<T>(val continuation: Continuation<T>, val executor: Executor): Continuation<T> by continuation {

  override fun resumeWith(result: Result<T>) {
    executor.execute {
      continuation.resumeWith(result)
    }
  }
}

suspend fun main() {
  val producer = Coroutine.create<Unit, Int>(Dispatcher()) {
    for (i in 0..3) {
      println("send $i")
      yield(i)
    }
    200
  }

  val consumer = Coroutine.create<Int, Unit>(Dispatcher()) { param: Int ->
    println("start $param")
    for (i in 0..3) {
      val value = yield(Unit)
      println("receive $value")
    }
  }

  while (producer.isActive && consumer.isActive){
    val result = producer.resume(Unit)
    consumer.resume(result)
  }
}