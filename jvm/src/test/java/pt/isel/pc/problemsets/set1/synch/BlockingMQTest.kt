package pt.isel.pc.problemsets.set1.synch

import org.junit.Assert
import org.junit.Test
import java.util.concurrent.ExecutionException

class BlockingMQTest {
    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun test_waiting_request() {
        val messageQueue: `var` = BlockingMessageQueue<Integer>(3)
        val future: `var` = messageQueue.dequeue()
        messageQueue.enqueue(1, 500)
        assertEquals(1, future.get().intValue())
        assertEquals("DequeueRequest", future.getClass().getSimpleName())
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun test_waiting_message() {
        val messageQueue: `var` = BlockingMessageQueue<Integer>(3)
        messageQueue.enqueue(1, 500)
        val future: `var` = messageQueue.dequeue()
        assertEquals(1, future.get().intValue())
        assertEquals("CompleteRequest", future.getClass().getSimpleName())
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun test_ensure_fifo_order() {
        val messageQueue: `var` = BlockingMessageQueue<Integer>(3)
        val future1: `var` = messageQueue.dequeue()
        messageQueue.enqueue(1, 500)
        messageQueue.enqueue(2, 500)
        val future2: `var` = messageQueue.dequeue()
        val future3: `var` = messageQueue.dequeue()
        messageQueue.enqueue(3, 500)
        assertEquals(1, future1.get().intValue())
        assertEquals(2, future2.get().intValue())
        assertEquals(3, future3.get().intValue())
        assertEquals("DequeueRequest", future1.getClass().getSimpleName())
        assertEquals("CompleteRequest", future2.getClass().getSimpleName())
        assertEquals("DequeueRequest", future3.getClass().getSimpleName())
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun test_ensure_message_not_lost() {
        val messageQueue: `var` = BlockingMessageQueue<Integer>(1) // 1 message max capacity
        messageQueue.enqueue(1, 500)
        val t = Thread {  // 5 sec awake
            try {
                messageQueue.enqueue(5, 5000)
            } catch (e: InterruptedException) {
                // ignore
            }
        }
        t.start()
        messageQueue.dequeue()
        Thread.sleep(100)
        val future1: `var` = messageQueue.dequeue()
        assertEquals(5, future1.get().intValue())
        assertEquals("CompleteRequest", future1.getClass().getSimpleName())
        t.join()
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun test_ensure_message_not_lost_with_interrupt() {
        val messageQueue: `var` = BlockingMessageQueue<Integer>(1) // 1 message max capacity
        messageQueue.enqueue(1, 500)
        val t = Thread {  // 5 sec awake
            try {
                messageQueue.enqueue(5, 5000)
            } catch (e: InterruptedException) {
                // ignore
            }
        }
        t.start()
        messageQueue.dequeue()
        t.interrupt() // Attempts to interrupt thread waiting to enqueue, but fails because there already is a slot
        Thread.sleep(100)
        val future1: `var` = messageQueue.dequeue()
        assertEquals(5, future1.get().intValue())
        assertEquals("CompleteRequest", future1.getClass().getSimpleName())
        t.join()
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun test_capacity_0() {
        val messageQueue: `var` = BlockingMessageQueue<Integer>(0)
        val t = Thread {
            try {
                messageQueue.enqueue(1, 5000)
            } catch (e: InterruptedException) {
                // ignore
            }
        }
        t.start()
        Thread.sleep(10)
        val future: `var` = messageQueue.dequeue()
        assertEquals(1, future.get().intValue())
        assertEquals("DequeueRequest", future.getClass().getSimpleName())
        t.join()
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun test_request_cancellation() {
        val messageQueue: `var` = BlockingMessageQueue<Integer>(0) // 0 message capacity
        val future: `var` = messageQueue.dequeue()
        future.cancel(false)
        Assert.assertTrue(future.isCancelled())
        Assert.assertFalse(messageQueue.enqueue(1, 1000)) // returns false from timeout due to no requests
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun test_request_failed_cancellation() {
        val messageQueue: `var` = BlockingMessageQueue<Integer>(0) // 0 message capacity
        val future: `var` = messageQueue.dequeue()
        Assert.assertTrue(messageQueue.enqueue(1, 1000)) // returns true due to 1 request in line
        Assert.assertFalse(future.cancel(false)) // fails to cancel because value already returned
        assertEquals(1, future.get().intValue())
    }
}