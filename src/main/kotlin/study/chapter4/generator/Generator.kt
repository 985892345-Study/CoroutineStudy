package study.chapter4.generator

import kotlin.coroutines.*

/**
 * .
 *
 * @author 985892345 (Guo Xiangrui)
 * @date 2022/10/20 15:24
 */
interface Generator<T> {
  operator fun iterator(): Iterator<T>
}

class GeneratorImpl<T>(
  private val block: suspend GeneratorScope<T>.(T) -> Unit,
  private val parameter: T
) : Generator<T> {
  override fun iterator(): Iterator<T> {
    return GeneratorIterator(block, parameter)
  }
}

sealed class State {
  class NotReady(val continuation: Continuation<Unit>) : State()
  class Ready<T>(val continuation: Continuation<Unit>, val nextValue: T) : State()
  object Done : State()
}

class GeneratorIterator<T>(
  private val block: suspend GeneratorScope<T>.(T) -> Unit,
  private val parameter: T
) : GeneratorScope<T>, Iterator<T>, Continuation<Any?> {
  override val context: CoroutineContext = EmptyCoroutineContext

  private var state: State

  init {
    val coroutineBlock: suspend GeneratorScope<T>.() -> Unit = { block(parameter) }
    val start = coroutineBlock.createCoroutine(this, this)
    state = State.NotReady(start)
  }

  override suspend fun yield(value: T) =
    suspendCoroutine { continuation ->
      state = when (state) {
        // Ready 保存了当前 continuation 对象，用于恢复 for 循环的继续调用
        is State.NotReady -> State.Ready(continuation, value)
        is State.Ready<*> -> throw IllegalStateException("Cannot yield a value while ready.")
        State.Done -> throw IllegalStateException("Cannot yield a value while done.")
      }
    }

  private fun resume() {
    val currentState = state
    if (currentState is State.NotReady) {
      // 如果没有初始化的话就开始初始化
      // 这里最终会调用到 for (i in 0..5) 开始进行循环，然后遇到第一个 yield() 挂起
      currentState.continuation.resume(Unit)
    }
  }

  override fun hasNext(): Boolean {
    resume()
    return state != State.Done
  }

  override fun next(): T {
    return when (val currentState = state) {
      is State.NotReady -> {
        resume()
        return next()
      }

      is State.Ready<*> -> {
        // 把 continuation 保存给下一次使用
        state = State.NotReady(currentState.continuation)
        // 这里发送之前调用 yield() 时准备好了的值
        (currentState as State.Ready<T>).nextValue
      }

      State.Done -> throw IndexOutOfBoundsException("No value left.")
    }
  }

  override fun resumeWith(result: Result<Any?>) {
    state = State.Done
    result.getOrThrow()
  }

}

interface GeneratorScope<T> {
  suspend fun yield(value: T)
}

fun <T> generator(block: suspend GeneratorScope<T>.(T) -> Unit): (T) -> Generator<T> {
  return { parameter: T ->
    GeneratorImpl(block, parameter)
  }
}

fun main() {
  val nums = generator { start: Int ->
    for (i in 0..5) {
      yield(start + i)
    }
  }

  val seq = nums(10)

  for (j in seq) {
    println(j)
  }

  val sequence = sequence {
    yield(1)
    yield(2)
    yield(3)
    yield(4)
    yieldAll(listOf(1, 2, 3, 4))
  }

  for (element in sequence) {
    println(element)
  }

  val fibonacci = sequence {
    yield(1L) // first Fibonacci number
    var current = 1L
    var next = 1L
    while (true) {
      yield(next) // next Fibonacci number
      next += current
      current = next - current
    }
  }

  fibonacci.take(10).forEach(::println)
}