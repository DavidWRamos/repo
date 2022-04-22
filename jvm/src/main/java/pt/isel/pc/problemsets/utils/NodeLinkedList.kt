package pt.isel.pc.problemsets.utils

class NodeLinkedList<T> {
    // The linked list node type
    class Node<T> internal constructor(val value: T) {
        var next: Node<T>? = null
        var prev: Node<T>? = null
    }

    private val head: Node<T?>
    var count = 0
        private set

    init {
        head = Node(null)
        head.next = head
        head.prev = head
    }

    fun enqueue(value: T): Node<T?> {
        val node = Node<T?>(value)
        val tail = head.prev
        node.prev = tail
        node.next = head
        head.prev = node
        tail!!.next = node
        count += 1
        return node
    }

    val isEmpty: Boolean
        get() = head === head.prev
    val isNotEmpty: Boolean
        get() = !isEmpty
    val headValue: T?
        get() {
            if (isEmpty) {
                throw IllegalStateException("cannot get head of an empty list")
            }
            return head.next!!.value
        }

    fun isHeadNode(node: Node<T>): Boolean {
        return head.next === node
    }

    fun pull(): Node<T?>? {
        if (isEmpty) {
            throw IllegalStateException("cannot pull from an empty list")
        }
        val node = head.next
        head.next = node!!.next
        node.next!!.prev = head
        count -= 1
        return node
    }

    fun remove(node: Node<T>) {
        node.prev!!.next = node.next
        node.next!!.prev = node.prev
        count -= 1
    }
}