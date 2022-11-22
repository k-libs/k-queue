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
 *   require(stack.next() == i)
 * ```
 *
 * @param T Type of items that will be appended to this queue.
 *
 * @author Elizabeth Paige Harper - https://github.com/foxcapades
 * @since 1.0.0
 */
@Suppress("UNCHECKED_CAST", "ReplaceSizeZeroCheckWithIsEmpty")
class Queue<T> : Collection<T>, Iterator<T> {
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
  override var size: Int
    private set

  /**
   * Current capacity of this queue.
   */
  var capacity: Int
    private set

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
   * Returns the value at the head of the queue without removing it.
   *
   * @return The value currently at the head of the queue.
   *
   * @throws NoSuchElementException If the queue has no next element.
   */
  fun peekNext(): T {
    if (size > 0)
      return buffer[head] as T
    else
      throw NoSuchElementException()
  }

  /**
   * Tests whether this queue contains at least one more element.
   *
   * @return Whether this queue contains at least one more element.
   */
  override fun hasNext() = size > 0

  /**
   * Removes and returns the value from the head of the queue.
   *
   * @return The value that was at the head of the queue when this method was
   * called.
   *
   * @throws NoSuchElementException If the queue has no next element.
   */
  override fun next(): T {
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

  override fun iterator() = this

  override fun isEmpty() = size == 0

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
  operator fun get(i: Int): T {
    if (i < 0 || i >= size)
      throw IndexOutOfBoundsException("attempted to access item $i in a queue with a size of $size")
    else
      return buffer[index(i)] as T
  }

  override fun contains(element: T): Boolean {
    for (i in indices)
      if (buffer[index(i)] == element)
        return true
    return false
  }

  override fun containsAll(elements: Collection<T>) = all { contains(it) }

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

inline fun min(a: Int, b: Int) = if (a < b) a else b
inline fun max(a: Int, b: Int) = if (a > b) a else b