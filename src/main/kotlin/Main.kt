import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

suspend fun main() {
  flow {
    emit(1)
    withContext(Dispatchers.Default) {
      emit(2)
    }
  }

  channelFlow {
    send(1)
  }
}