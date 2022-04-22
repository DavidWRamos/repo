package pt.isel.pc.problemsets.set1.synch

import pt.isel.pc.problemsets.utils.NodeLinkedList

/**
 * BlockingMessageQueue thread synchronizer, implemented monitor style.
 */
class BlockingMessageQueue<E>(private val capacity: Int) {
    /**
     * Static class Request, that is the representation
     * request made in dequeue, with a condition to be notified
     * of when its request is fulfilled, and booleans isDone and
     * isCanceled to keep track of the state of the request.
     */
    private class Request<E> private constructor(lock: Lock) {
        var message: E? = null
        var isDone = false
        var isCanceled = false
        val condition: Condition

        init {
            condition = lock.newCondition()
        }
    }

    private val monitor: Lock = ReentrantLock()
    private val messages: NodeLinkedList<E> = NodeLinkedList()
    private val requests: NodeLinkedList<Request<E>> = NodeLinkedList()
    private val awaitCapacity: Condition = monitor.newCondition()
    @Throws(InterruptedException::class)
    fun enqueue(message: E, timeout: Long): Boolean {
        monitor.lock()
        try {

            // fast path
            if (requests.isNotEmpty()) {
                val request: `var` = requests.pull().value
                request.message = message
                request.isDone = true
                request.condition.signal()
                return true
            }
            if (messages.getCount() < capacity) {
                messages.enqueue(message)
                return true
            }

            // wait path
            val deadline: Long = Timeouts.deadlineFor(timeout)
            var remaining: Long = Timeouts.remainingUntil(deadline)
            while (true) {
                try {
                    awaitCapacity.await(remaining, TimeUnit.MILLISECONDS)
                } catch (e: InterruptedException) {
                    if (messages.getCount() < capacity) {
                        messages.enqueue(message)
                        Thread.currentThread().interrupt()
                        return true
                    }
                    throw e
                }
                if (requests.isNotEmpty()) {
                    val request: `var` = requests.pull().value
                    request.message = message
                    request.isDone = true
                    request.condition.signal()
                    return true
                }
                if (messages.getCount() < capacity) {
                    messages.enqueue(message)
                    return true
                }
                remaining = Timeouts.remainingUntil(deadline)
                if (Timeouts.isTimeout(remaining)) {
                    return false
                }
            }
        } finally {
            monitor.unlock()
        }
    }

    fun dequeue(): Future<E> {
        monitor.lock()
        return try {
            if (messages.isNotEmpty()) {
                awaitCapacity.signal()
                return CompleteRequest(messages.pull().value)
            }
            val node: `var` =
                requests.enqueue(Request<Any>(monitor))
            // if the message queue has 0 capacity, then all threads in enqueue are waiting for requests
            if (capacity == 0) {
                awaitCapacity.signal()
            }
            DequeueRequest(node)
        } finally {
            monitor.unlock()
        }
    }

    private inner class CompleteRequest(private val message: E) : Future<E> {
        @Override
        fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            return false
        }

        @get:Override
        val isCancelled: Boolean
            get() = false

        @get:Override
        val isDone: Boolean
            get() = true

        @Override
        @Throws(InterruptedException::class, ExecutionException::class)
        fun get(): E {
            return message
        }

        @Override
        @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
        operator fun get(timeout: Long, unit: TimeUnit?): E {
            return message
        }
    }

    private inner class DequeueRequest(node: NodeLinkedList.Node<Request<E>?>) : Future<E> {
        private val node: NodeLinkedList.Node<Request<E>>

        init {
            this.node = node
        }

        @Override
        fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            monitor.lock()
            return try {
                if (node.value.isDone) {
                    return false
                }
                if (node.value.isCanceled) {
                    return true
                }
                requests.remove(node)
                node.value.isCanceled = true
                true
            } finally {
                monitor.unlock()
            }
        }

        @get:Override
        val isCancelled: Boolean
            get() {
                monitor.lock()
                return try {
                    node.value.isCanceled
                } finally {
                    monitor.unlock()
                }
            }

        @get:Override
        val isDone: Boolean
            get() {
                monitor.lock()
                return try {
                    node.value.isDone
                } finally {
                    monitor.unlock()
                }
            }

        @Override
        @Throws(InterruptedException::class, ExecutionException::class)
        fun get(): E {
            monitor.lock()
            try {
                while (true) {
                    if (node.value.isDone) {
                        return node.value.message
                    }
                    if (node.value.isCanceled) {
                        throw CancellationException()
                    }
                    node.value.condition.await()
                }
            } finally {
                monitor.unlock()
            }
        }

        @Override
        @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
        operator fun get(timeout: Long, unit: TimeUnit?): E? {
            monitor.lock()
            try {
                val deadline: Long = Timeouts.deadlineFor(timeout)
                var remaining: Long = Timeouts.remainingUntil(deadline)
                while (true) {
                    if (node.value.isDone) {
                        return node.value.message
                    }
                    if (node.value.isCanceled) {
                        throw CancellationException()
                    }
                    node.value.condition.await(remaining, TimeUnit.MILLISECONDS)
                    remaining = Timeouts.remainingUntil(deadline)
                    if (Timeouts.isTimeout(remaining)) {
                        return null
                    }
                }
            } finally {
                monitor.unlock()
            }
        }
    }
}