package study.context

import kotlin.coroutines.CoroutineContext

/**
 * CoroutineContext 内部方法的学习
 *
 * 文章推荐：https://juejin.cn/post/6978613779252641799
 *
 * 总结：整个 CoroutineContext 是一个偏心洋葱的结构，
 *
 * 比如：
 * a + b          -> (a,b)
 * a + b + c      -> ((a,b),c)
 * a + b + c + d  -> (((a,b),c),d)
 *
 * 上面这个是定义好了的结构，Element 和 CombinedContext 作为了这种数据结构的核心实现类，他们都有一个共同的父接口：CoroutineContext
 *
 * @author 985892345 (Guo Xiangrui)
 * @email guo985892345@foxmail.com
 * @date 2022/11/1 18:10
 */
interface CoroutineContextTest : CoroutineContext {
  
  /**
   * 得到里面 key 值对应的元素，你可以把这个当成一个 Map 键值对，但只不过底层实现其实是一个深度遍历
   */
  override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E?
  
  /**
   * 方法名为：折叠
   * 顾名思义，该方法就是用来构成偏心洋葱结构的核心方法
   *
   * 你可以简单的理解为：(先假设：a、b 都是 Element)
   * a.fold(b) {
   *   CombinedContext(b, a) // CombinedContext 是一个 Pair，用于保存洋葱中的左右结构
   * }
   *
   * 上述使用可以概括为：a 折叠到 b 上，按照偏心洋葱的结构，所以 a 在 b 的右边
   *
   * 该方法在 Element 和 CombinedContext 上有不同的实现
   * Element：因为是单个元素，所以直接放到右边即可
   * CombinedContext：因为是多个元素，所以需要先遍历找到最左边的 Element(取名为 e)，然后调用 e.fold(initial, operation)，
   * 这样才能使 e 在 initial 的后面，即 initial 在最开始的位置
   *
   * 例子：
   * val a = Element()
   * val b = Element()
   * val c = Element()
   * val d = Element()
   *
   * val context1 = a + b           -> (a,b)
   * val context2 = c + d           -> (c,d)
   *
   * val context12 = context1 + context2   由定义我们直接可得答案：(((a,b),c),d)
   *
   * context12 的计算步骤：
   * 1、context2.fold(context1, operation) 形象的来看就是：(c,d).fold((a,b), operation)
   * 2、context2 先深度遍历到最左边的 c，然后调用：c.fold((a,b), operation)
   * 3、此时生成一个新的 CombinedContext，洋葱结构为：((a,b),c)
   * 4、然后再调用：d.fold(((a,b),c), operation)
   * 5、最后结构就为：(((a,b),c),d)
   *
   * 那个深度遍历其实这是一个递归，至于例子中出现的 operation，我们暂时可以不用管它，简单理解为合并即可
   */
  override fun <R> fold(initial: R, operation: (R, CoroutineContext.Element) -> R): R
  
  /**
   * 按照方法名我们就可以猜出这个方法等同于 Map.remove(Key)
   *
   * 其实在 CombinedContext 也是一个深度遍历，实现代码很简单，这里就不说明了
   * 比如：
   * (((a,b),c),d) 中去掉 b，则结果为：((a,c),d)，其中包裹 a b 的 CombinedContext 被废弃掉了，new 了一个新的 CombinedContext 来包裹 a c
   */
  override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext
  
  /**
   * 用于实现 a + b 的运算符重载方法
   *
   * 里面的实现就是直接调用了 b.fold(a, operation)
   *
   * 对于 operation，除了直接合并成洋葱结构往外，还有两个其他作用：
   * - 去掉重复的元素（这也就是为什么是等同于 Map 键值对的原因）（但去掉后旧的重复元素后并不会把新的元素放在原位置，而是继续贴在最右边）
   * - 永远将 Interceptor 放在偏心洋葱的最外层
   *
   * 整个等同于 Map 键值对，但内部实现是采取深度遍历来实现的，应该是考虑到结构顺序的问题所以采取了偏心洋葱这种结构
   */
  override fun plus(context: CoroutineContext): CoroutineContext
}