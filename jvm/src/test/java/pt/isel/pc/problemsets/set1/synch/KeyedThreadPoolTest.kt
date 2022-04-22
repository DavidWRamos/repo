package pt.isel.pc.problemsets.set1.synch

import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class KeyedThreadPoolTest {
    @Test
    @Throws(InterruptedException::class)
    fun test_all_work_done_with_dif_keys() {
        val monitor: `var` = KeyedThreadPoolExecutor(3, 5000)
        val integer = AtomicInteger()
        for (i in 0..4) {
            monitor.execute({ integer.set(integer.intValue() + 1) }, i)
            Thread.sleep(500)
        }
        monitor.shutdown()
        while (!monitor.awaitTermination(5000));
        assertEquals(5, integer.intValue())
    }

    @Test
    @Throws(InterruptedException::class)
    fun test_all_work_done_with_equal_keys() {
        val monitor: `var` = KeyedThreadPoolExecutor(3, 5000)
        val integer = AtomicInteger()
        for (i in 0..4) {
            monitor.execute({ integer.set(integer.intValue() + 1) }, 1)
            Thread.sleep(500)
        }
        monitor.shutdown()
        while (!monitor.awaitTermination(5000));
        assertEquals(5, integer.intValue())
    }
}