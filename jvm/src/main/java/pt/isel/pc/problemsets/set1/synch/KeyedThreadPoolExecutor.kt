package pt.isel.pc.problemsets.set1.synch

import pt.isel.pc.problemsets.utils.Timeouts

/**
 * KeyedThreadPoolExecutor thread synchronizer, implemented kernel style.
 */
class KeyedThreadPoolExecutor(private val maxPoolSize: Int, private val keepAliveTime: Int) {
    /**
     * Static class Work, that is the representation of the work to
     * be done by the worker threads.
     */
    private class Work private constructor(work: Runnable, key: Object) {
        val work: Runnable
        val key: Object

        init {
            this.work = work
            this.key = key
        }
    }

    private val lock: Lock = ReentrantLock()
    private val waitShutdown: Condition = lock.newCondition()
    private val waitNewWork: Condition = lock.newCondition()
    private val workList: LinkedList<Work> = LinkedList()
    private val runningKeys: Set<Object> = HashSet()
    private var numWorkerThreads = 0
    private var currentlyWorking = 0
    private var shutdown = false
    fun execute(runnable: Runnable, key: Object) {
        lock.lock()
        try {
            if (shutdown) {
                throw RejectedExecutionException("Shutdown Mode")
            }
            workList.addLast(Work(runnable, key))
            if (currentlyWorking == numWorkerThreads && numWorkerThreads < maxPoolSize) {
                val deadline: Long = Timeouts.deadlineFor(keepAliveTime)
                val th: `var` = Thread { threadLoop(deadline) }
                th.start()
                numWorkerThreads += 1
            }
            waitNewWork.signal()
        } finally {
            lock.unlock()
        }
    }

    private fun getWork(deadline: Long, prevKey: Object?): Optional<Work> {
        lock.lock()
        try {
            if (prevKey != null) --currentlyWorking
            runningKeys.remove(prevKey)
            for (i in 0 until workList.size()) {
                val work: `var` = workList.get(i)
                if (!runningKeys.contains(work.key)) {
                    runningKeys.add(work.key)
                    workList.remove(i)
                    ++currentlyWorking
                    return Optional.of(work)
                }
            }
            if (shutdown && workList.isEmpty()) {
                --numWorkerThreads
                if (numWorkerThreads == 0) {
                    waitShutdown.signalAll()
                }
                return Optional.empty()
            }
            var remaining: Long = Timeouts.remainingUntil(deadline)
            while (true) {
                try {
                    waitNewWork.await(remaining, TimeUnit.MILLISECONDS)
                } catch (e: InterruptedException) {
                    --numWorkerThreads
                    if (numWorkerThreads == 0) {
                        waitShutdown.signalAll()
                    }
                    return Optional.empty()
                }
                for (i in 0 until workList.size()) {
                    val work: `var` = workList.get(i)
                    if (!runningKeys.contains(work.key)) {
                        runningKeys.add(work.key)
                        workList.remove(i)
                        ++currentlyWorking
                        return Optional.of(work)
                    }
                }
                remaining = Timeouts.remainingUntil(deadline)
                if (Timeouts.isTimeout(remaining)) {
                    --numWorkerThreads
                    return Optional.empty()
                }
            }
        } finally {
            lock.unlock()
        }
    }

    private fun threadLoop(deadline: Long) {
        val currKey: AtomicReference<Object> = AtomicReference()
        while (true) {
            val maybeWork: `var` = getWork(deadline, currKey.get())
            maybeWork.ifPresent { obj ->
                obj.work.run()
                currKey.set(obj.key)
            }
            if (maybeWork.isEmpty()) {
                return
            }
        }
    }

    fun shutdown() {
        lock.lock()
        shutdown = try {
            true
        } finally {
            lock.unlock()
        }
    }

    @Throws(InterruptedException::class)
    fun awaitTermination(timeout: Int): Boolean {
        lock.lock()
        try {
            if (Timeouts.noWait(timeout)) {
                return false
            }
            if (numWorkerThreads == 0 && workList.isEmpty()) {
                return true
            }
            val deadline: Long = Timeouts.deadlineFor(timeout)
            var remaining: Long = Timeouts.remainingUntil(deadline)
            while (true) {
                waitShutdown.await(remaining, TimeUnit.MILLISECONDS)
                if (numWorkerThreads == 0 && workList.isEmpty()) {
                    return true
                }
                remaining = Timeouts.remainingUntil(deadline)
                if (Timeouts.isTimeout(remaining)) {
                    return false
                }
            }
        } finally {
            lock.unlock()
        }
    }
}