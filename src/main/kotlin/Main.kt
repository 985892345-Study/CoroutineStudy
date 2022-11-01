import kotlinx.coroutines.*
import kotlin.concurrent.thread

fun main() {
  GlobalScope.launch {
    println(".(Main.kt:${Exception().stackTrace[0].lineNumber}) " +
        "launch: $coroutineContext")
    launch {
      println(".(Main.kt:${Exception().stackTrace[0].lineNumber}) " +
                "launch1: $coroutineContext")
      launch(SupervisorJob(coroutineContext[Job])) {
        println(".(Main.kt:${Exception().stackTrace[0].lineNumber}) " +
                  "launch11: $coroutineContext")
        launch {
          println(".(Main.kt:${Exception().stackTrace[0].lineNumber}) " +
                    "launch111: $coroutineContext")
          observeCancel("launch111")
        }
        launch {
          println(".(Main.kt:${Exception().stackTrace[0].lineNumber}) " +
                    "launch112: $coroutineContext")
          delay(1000)
          println(".(Main.kt:${Exception().stackTrace[0].lineNumber}) " +
            "launch112 throw")
          throw RuntimeException()
        }
        observeCancel("launch11")
      }
      launch {
        println(".(Main.kt:${Exception().stackTrace[0].lineNumber}) " +
                  "launch12: $coroutineContext")
        observeCancel("launch12")
      }
    }
    println(".(Main.kt:${Exception().stackTrace[0].lineNumber}) " +
      "launch ...")
    observeCancel("launch")
  }
  thread {
    while (true);
  }
}

suspend fun observeCancel(tag: String) = suspendCancellableCoroutine<Unit> {
  it.invokeOnCancellation {
    println(
      ".(Main.kt:${Exception().stackTrace[0].lineNumber}) " +
        "tag = $tag   Cancel"
    )
  }
}