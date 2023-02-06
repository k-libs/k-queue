package io.klibs.collections

/**
 * Creates a new [Queue] instance wrapping the given values.
 *
 * The created queue will have an initial capacity equal to the number of args
 * passed.
 *
 * Example:
 * ```
 * val queue = queueOf(1, 2, 3)
 *
 * queue.next() // 1
 * queue.next() // 2
 * queue.next() // 3
 * ```
 *
 * @param values Values to append to the newly created queue.
 *
 * @return A new queue wrapping the given values.
 */
fun <T> queueOf(vararg values: T) = Queue<T>(values.size).apply { values.forEach { append(it) } }

/**
 * Creates a new [Queue] instance wrapping the given values.
 *
 * Example:
 * ```
 * val queue = queueOf(listOf(1, 2, 3))
 *
 * queue.next() // 1
 * queue.next() // 2
 * queue.next() // 3
 * ```
 *
 * @param values Values to append to the newly created queue.
 *
 * @return A new queue wrapping the given values.
 */
fun <T> queueOf(values: Iterable<T>) =
  if (values is Collection)
    Queue<T>(values.size).apply { values.forEach { append(it) } }
  else
    Queue<T>().apply { values.forEach { append(it) } }

/**
 * # FIFO Queue
 *
 * Example:
 * ```
 * // Create a new queue
 * val queue = Queue<Int>()
 *
 * for (i in 1 .. 3)
 *   queue += i
 *
 * for (i in 1 .. 3)
 *   require(queue.next() == i)
 * ```
 *
 * @param T Type of items that will be appended to this queue.
 *
 * @author Elizabeth Paige Harper - https://github.com/foxcapades
 * @since 1.0.0
 */
class Queue<T> {
  private var buffer: Array<Any?>

  private var head: Int

  private var scale: Float

  private inline val tail
    get() = index(size)

  /**
   * The maximum size this queue may grow to.
   *
   * Attempting to append items to a queue that is already at it's max size will
   * result in an exception being thrown.
   */
  val maxSize: Int

  /**
   * Current size of this queue.
   */
  var size: Int
    private set

  /**
   * Current capacity of this queue.
   */
  var capacity: Int
    private set

  /**
   * `true` if this [Queue] contains zero items.
   */
  inline val isEmpty get() = size == 0

  /**
   * `true` if this [Queue] contains one or more items.
   */
  inline val isNotEmpty get() = size > 0

  /**
   * Last valid index in this [Queue].
   *
   * If this [Queue] is empty, [lastIndex] will be `-1`.
   */
  inline val lastIndex get() = size - 1

  /**
   * Returns the range of valid indices in this [Queue].
   */
  inline val indices get() = 0 .. lastIndex

  /**
   * Constructs a new [Queue] instance.
   *
   * @param initialCapacity Initial capacity for the queue.
   *
   * Default: `8`
   *
   * @param scaleFactor Factor by which the queue's capacity will be increased
   * at a time when required to contain appended values.
   *
   * When a value is appended to the queue that would increase the queue's
   * [size] to be greater than the queue's [capacity], the data container
   * underlying this queue will be resized to increase the capacity at a rate of
   * `capacity * scaleFactor`.
   *
   * Default: `1.5`
   *
   * @param maxSize Size cap for the stack.  If an attempt is made to append a
   * number of items into this queue that is greater than the [maxSize] value,
   * an exception will be thrown.
   *
   * Default: `2147483647`
   */
  constructor(
    initialCapacity: Int = 8,
    scaleFactor: Float = 1.5F,
    maxSize: Int = Int.MAX_VALUE,
  ) {
    this.buffer   = arrayOfNulls(initialCapacity)
    this.head     = 0
    this.scale    = scaleFactor
    this.maxSize  = maxSize
    this.size     = 0
    this.capacity = initialCapacity
  }

  /**
   * Appends the given value to the tail end of the queue.
   *
   * @param value Value to append to the queue.
   *
   * @throws IllegalStateException If appending this value would increase the
   * queue's [size] to be greater than [maxSize].
   */
  fun append(value: T) {
    if (size == capacity)
      increaseBufferSize()

    buffer[tail] = value
    size++
  }

  /**
   * Removes and returns the value from the head of the queue.
   *
   * @return The value that was at the head of the queue when this method was
   * called.
   *
   * @throws NoSuchElementException If the queue has no next element.
   */
  @Suppress("UNCHECKED_CAST")
  fun next(): T {
    if (size == 0)
      throw NoSuchElementException()

    val out = buffer[head] as T
    buffer[head] = null
    head++
    if (head == capacity)
      head = 0
    size--

    return out
  }

  /**
   * Clears all elements from this queue.
   *
   * The capacity of the queue will not change as a result of this action.
   *
   * This operation is `O(n)` where `n` is the current [size] of the queue.
   */
  fun clear() {
    for (i in indices)
      buffer[index(i)] = null

    head = 0
    size = 0
  }

  /**
   * Returns the value at the head of the queue without removing it.
   *
   * @return The value currently at the head of the queue.
   *
   * @throws NoSuchElementException If the queue has no next element.
   */
  @Suppress("UNCHECKED_CAST")
  fun peek(): T {
    if (size > 0)
      return buffer[head] as T
    else
      throw NoSuchElementException()
  }

  /**
   * Alias for [append].
   */
  operator fun plusAssign(value: T) = append(value)

  /**
   * Gets the target queue element without removing it from the queue.
   *
   * @param i Index of the queue element to get.
   *
   * @return The queue element at index [i].
   *
   * @throws IndexOutOfBoundsException If [i] is less than zero or is greater
   * than or equal to [size].
   */
  @Suppress("UNCHECKED_CAST")
  operator fun get(i: Int): T {
    if (i < 0 || i >= size)
      throw IndexOutOfBoundsException("attempted to access item $i in a queue with a size of $size")
    else
      return buffer[index(i)] as T
  }

  /**
   * Tests whether this [Queue] contains the given item.
   *
   * This function iterates through the items in the `Queue` and tests each one
   * until it either finds an entry matching the given [element] or it reaches
   * the end of the `Queue` items.
   *
   * @param element Item to test for.
   *
   * @return `true` if this [Queue] contains an item that is equal to the given
   * [element], otherwise `false`.
   */
  operator fun contains(element: T): Boolean {
    for (i in indices)
      if (buffer[index(i)] == element)
        return true
    return false
  }

  /**
   * Tests whether this [Queue] contains all the given [elements].
   *
   * This function calls [contains] for each given element in [elements] until
   * it either finds an element that does not exist in this [Queue] (at which
   * point it returns `false`), or until it reaches the end of the array of
   * given [elements].
   *
   * @param elements Elements to test for.
   *
   * @return `true` if this [Queue] contains all the given [elements], otherwise
   * `false`.
   */
  fun containsAll(vararg elements: T) = elements.all { contains(it) }

  /**
   * Tests whether this [Queue] contains all the given [elements].
   *
   * This function calls [contains] for each given element in [elements] until
   * it either finds an element that does not exist in this [Queue] (at which
   * point it returns `false`), or until it reaches the end of the collection of
   * given [elements].
   *
   * @param elements Elements to test for.
   *
   * @return `true` if this [Queue] contains all the given [elements], otherwise
   * `false`.
   */
  fun containsAll(elements: Collection<T>) = elements.all { contains(it) }

  inline fun destructiveForEach(fn: (T) -> Unit) {
    while (isNotEmpty)
      fn(next())
  }

  inline fun nonDestructiveForEach(fn: (T) -> Unit) {
    var i = 0
    val l = lastIndex
    while (i < l)
      fn(get(i++))
  }

  fun destructiveIterator() = object {
    operator fun hasNext() = isNotEmpty
    operator fun next() = this@Queue.next()
  }

  fun nonDestructiveIterator() = object {
    private var i = 0;
    operator fun hasNext() = i < lastIndex
    operator fun next() = get(i++)
  }

  private inline fun index(i: Int): Int {
    return (head + i).let { if (it >= capacity) it - capacity else it }
  }

  private fun increaseBufferSize() {
    if (capacity >= maxSize)
      throw IllegalStateException("attempted to increase a queue's capacity past it's maxSize value $maxSize")

    val new = arrayOfNulls<Any?>(
      min(
        maxSize,
        max(
          capacity + 1,
          (capacity * scale).toInt()
        )
      )
    )

    if (head <= tail) {
      buffer.copyInto(new, 0, head, tail)
    } else {
      buffer.copyInto(new, 0, head, buffer.size)
      buffer.copyInto(new, buffer.size - head, 0, tail)
    }

    head = 0
    buffer = new
  }
}

private inline fun min(a: Int, b: Int) = if (a < b) a else b
private inline fun max(a: Int, b: Int) = if (a > b) a else b