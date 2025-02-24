package edu.iastate.cs2280.hw3;

import java.util.AbstractSequentialList;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * 
 * @author Bavly Fayed
 * 
 * Implementation of the list interface based on linked nodes
 * that store multiple items per node.  Rules for adding and removing
 * elements ensure that each node (except possibly the last one)
 * is at least half full.
 */
public class StoutList<E extends Comparable<? super E>> extends AbstractSequentialList<E> {
    /**
     * Default number of elements that may be stored in each node.
     */
    private static final int DEFAULT_NODESIZE = 4;

    /**
     * Number of elements that can be stored in each node.
     */
    private final int nodeSize;

    /**
     * Dummy node for head. It should be private but set to public here only for
     * grading purpose. In practice, you should always make the head of a linked
     * list a private instance variable.
     */
    public Node head;

    /**
     * Dummy node for tail.
     */
    private Node tail;

    /**
     * Number of elements in the list.
     */
    private int size;

    /**
     * Constructs an empty list with the default node size.
     */
    public StoutList() {
        this(DEFAULT_NODESIZE);
    }

    /**
     * Constructs an empty list with the given node size.
     *
     * @param nodeSize number of elements that may be stored in each node, must be
     *                 an even number
     */
    public StoutList(int nodeSize) {
        if (nodeSize <= 0 || nodeSize % 2 != 0)
            throw new IllegalArgumentException();

        // dummy nodes
        head = new Node();

        tail = new Node();

        head.next = tail;

        tail.previous = head;

        this.nodeSize = nodeSize;
    }

    /**
     * Constructor for grading only. Fully implemented.
     *
     * @param head
     * @param tail
     * @param nodeSize
     * @param size
     */
    public StoutList(Node head, Node tail, int nodeSize, int size) {
        this.head = head;
        this.tail = tail;
        this.nodeSize = nodeSize;
        this.size = size;
    }

    /**
     * @return the size of elements in the list
     */
    @Override
    public int size() {
        // TODO Auto-generated method stub
        return size;
    }

    /**
     * Appends the item to the list.
     *
     * @param item The item to append to the list.
     * @return True if the item was successfully appended, false if the item is
     *         already present in the list.
     */
    @Override
    public boolean add(E item) {
        if (item == null) {
            throw new NullPointerException();
        }

        if (contains(item)) {
            return false;
        }

        Node n;
        if (size == 0) {
            n = new Node();
            head.next = n;
            n.previous = head;
            n.next = tail;
            tail.previous = n;
            n.addItem(item);
        } else {
            n = tail.previous;
            if (n.count < nodeSize) {
                n.addItem(item);
            } else {
                Node newNode = new Node();
                newNode.addItem(item);
                newNode.next = tail;
                newNode.previous = n;
                n.next = newNode;
                tail.previous = newNode;
            }
        }

        size++;
        return true;
    }


    @Override
    public void add(int pos, E item) {
        if (pos < 0 || pos > size) {
            throw new IndexOutOfBoundsException();
        }

        // If the list is empty, add item at the end
        if (head.next == tail) {
            add(item);
            return;
        }

        NodeInfo nodeInfo = find(pos);
        Node temp = nodeInfo.node;
        int offset = nodeInfo.offset;

        // Case when the insertion point is at the start of a node
        if (offset == 0) {
            if (temp.previous != head && temp.previous.count < nodeSize) {
                temp.previous.addItem(item);
            } else if (temp == tail) {
                add(item);
            } else {
                temp.addItem(offset, item);
            }
            size++;
            return;
        }

        // Regular case for insertion within a node with enough space
        if (temp.count < nodeSize) {
            temp.addItem(offset, item);
            size++;
            return;
        }

        // Case where the node is full and needs to be split
        Node newNode = new Node();
        int mid = nodeSize / 2;
        for (int count = 0; count < mid; count++) {
            newNode.addItem(temp.data[mid]);
            temp.removeItem(mid);
        }

        // Re-linking the new node into the list
        newNode.next = temp.next;
        if (newNode.next != null) {
            newNode.next.previous = newNode;
        }
        temp.next = newNode;
        newNode.previous = temp;

        // Decide which node to add based on the offset
        if (offset <= mid) {
            temp.addItem(offset, item);
        } else {
            newNode.addItem(offset - mid, item);
        }
        size++;
    }


    /**
     *
     * Checks if there is a duplicate
     *
     * @param item
     * @return whether the argument is present or not
     *
     *
     * */

    public boolean contains(E item) {
        if(size < 1)
            return false;
        Node temp = head.next;
        while(temp != tail) {
            for(int i=0;i<temp.count;i++) {
                if(temp.data[i].equals(item))
                    return true;
                temp = temp.next;
            }
        }
        return false;
    }

    @Override
    public E remove(int pos) {
        if (pos < 0 || pos > size) {
            throw new IndexOutOfBoundsException("Position " + pos + " is out of bounds.");
        }

        NodeInfo nodeInfo = find(pos);
        Node tempNode = nodeInfo.node;
        int offset = nodeInfo.offset;
        E value = tempNode.data[offset];  // Store the value to return

        // Simple case: Remove the last element of the list
        if (tempNode.next == tail && tempNode.count == 1) {
            unlinkNode(tempNode);
        } else {
            tempNode.removeItem(offset);
            adjustAfterRemoval(tempNode);
        }

        size--;
        return value;
    }


    /**
     * Unlinks a given node from the list  by adjusting its previous and next nodes to point to each other,
     * effectively bypassing the specified node.
     *
     * @param node node to be unlinked from the list.
     */
    private void unlinkNode(Node node) {
        Node predecessor = node.previous;
        Node successor = node.next;
        predecessor.next = successor;
        successor.previous = predecessor;
    }


    /**
     * Adjusts the list after an item has been removed from a node and ensures
     * that the list remains balanced according to the rules
     *
     * @param node node from which an item was removed
     */
    private void adjustAfterRemoval(Node node) {
        // No adjustment needed if the node after removal still has more than half of its capacity filled
        if (node.count >= nodeSize / 2) return;

        Node nextNode = node.next;
        if (nextNode == tail) return;  // Nothing to do if the next node is the tail

        if (nextNode.count > nodeSize / 2) {
            // Perform a mini-merge: Transfer one element from nextNode to node
            node.addItem(nextNode.data[0]);
            nextNode.removeItem(0);
        } else {
            // Perform a full merge: Transfer all elements from nextNode to node and remove nextNode
            mergeNodes(node, nextNode);
        }
    }

    /**
     * Merges two adjacent nodes into one, transfering all elements from the second
     * node (nextNode) to the first node (node) and then unlinks the second node from the list.
     *
     *
     * @param node     node into which elements will be merged
     * @param nextNode the node whose elements are to be transferred to node.
     *
     */
    private void mergeNodes(Node node, Node nextNode) {
        for (int i = 0; i < nextNode.count; i++) {
            node.addItem(nextNode.data[i]);  // Transfer all elements
        }
        unlinkNode(nextNode);  // Remove nextNode from the list
    }


    /**
     * Sort all elements in the stout list in the NON-DECREASING order. You may do
     * the following. Traverse the list and copy its elements into an array,
     * deleting every visited node along the way. Then, sort the array by calling
     * the insertionSort() method. (Note that sorting efficiency is not a concern
     * for this project.) Finally, copy all elements from the array back to the
     * stout list, creating new nodes for storage. After sorting, all nodes but
     * (possibly) the last one must be full of elements.
     *
     * Comparator<E> must have been implemented for calling insertionSort().
     */
    public void sort() {
        E[] sortDataList = (E[]) new Comparable[size];
        int pos = 0;
        Node temp = head.next;
        while (temp != tail) {
            for (int i = 0; i < temp.count; i++) {
                sortDataList[pos] = temp.data[i];
                pos++;
            }
            temp = temp.next;
        }
        head.next = tail;
        tail.previous = head;
        Arrays.sort(sortDataList);
        size = 0;
        for (int i = 0; i < sortDataList.length; i++) {
            add(sortDataList[i]);
        }
    }

    /**
     * Sort all elements in the stout list in the NON-INCREASING order. Call the
     * bubbleSort() method. After sorting, all but (possibly) the last nodes must be
     * filled with elements.
     *
     * Comparable<? super E> must be implemented for calling bubbleSort().
     */
    public void sortReverse() {
        E[] revSortDataList = (E[]) new Comparable[size];

        int pos = 0;
        Node tempNode = head.next;
        while (tempNode != null) {
            for (int i = 0; i < tempNode.count; i++) {
                revSortDataList[pos] = tempNode.data[i];
                pos++;
            }
            tempNode = tempNode.next;
        }
        head.next = tail;
        tail.previous = head;
        bubbleSort(revSortDataList);
        size = 0;
        for (int i = 0; i < revSortDataList.length; i++) {
            add(revSortDataList[i]);
        }

    }

    @Override
    public Iterator<E> iterator() {
        return new StoutListIterator();
    }

    @Override
    public ListIterator<E> listIterator() {
        return new StoutListIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return new StoutListIterator(index);
    }

    /**
     * Returns a string representation of this list showing the internal structure
     * of the nodes.
     */
    public String toStringInternal() {
        return toStringInternal(null);
    }

    /**
     * Returns a string representation of this list showing the internal structure
     * of the nodes and the position of the iterator.
     *
     * @param iter an iterator for this list
     */
    public String toStringInternal(ListIterator<E> iter) {
        int count = 0;
        int position = -1;
        if (iter != null) {
            position = iter.nextIndex();
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        Node current = head.next;
        while (current != tail) {
            sb.append('(');
            E data = current.data[0];
            if (data == null) {
                sb.append("-");
            } else {
                if (position == count) {
                    sb.append("| ");
                    position = -1;
                }
                sb.append(data.toString());
                ++count;
            }

            for (int i = 1; i < nodeSize; ++i) {
                sb.append(", ");
                data = current.data[i];
                if (data == null) {
                    sb.append("-");
                } else {
                    if (position == count) {
                        sb.append("| ");
                        position = -1;
                    }
                    sb.append(data.toString());
                    ++count;

                    // iterator at end
                    if (position == size && count == size) {
                        sb.append(" |");
                        position = -1;
                    }
                }
            }
            sb.append(')');
            current = current.next;
            if (current != tail)
                sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Node type for this list. Each node holds a maximum of nodeSize elements in an
     * array. Empty slots are null.
     */
    private class Node {
        /**
         * Array of actual data elements.
         */

        public E[] data = (E[]) new Comparable[nodeSize];

        /**
         * Link to next node.
         */
        public Node next;

        /**
         * Link to previous node;
         */
        public Node previous;

        /**
         * Index of the next available offset in this node, also equal to the number of
         * elements in this node.
         */
        public int count;

        /**
         * Adds an item to this node at the first available offset. Precondition: count
         * < nodeSize
         *
         * @param item element to be added
         */
        void addItem(E item) {
            if (count >= nodeSize) {
                return;
            }
            data[count++] = item;

        }


        /**
         * Adds an item to this node at the indicated offset, shifting elements to the
         * right as necessary.
         *
         * Precondition: count < nodeSize
         *
         * @param offset array index at which to put the new element
         * @param item   element to be added
         */
        void addItem(int offset, E item) {
            if (count >= nodeSize) {
                return;
            }
            for (int i = count - 1; i >= offset; --i) {
                data[i + 1] = data[i];
            }
            ++count;
            data[offset] = item;

        }

        /**
         * Deletes an element from this node at the indicated offset, shifting elements
         * left as necessary. Precondition: 0 <= offset < count
         *
         * @param offset
         */
        void removeItem(int offset) {
            E item = data[offset];
            for (int i = offset + 1; i < nodeSize; ++i) {
                data[i - 1] = data[i];
            }
            data[count - 1] = null;
            --count;
        }
    }

    private class StoutListIterator implements ListIterator<E> {
        final int prevAction = 0;
        final int nextAction = 1;
        /**
         * pointer cursor to track where the iterator is
         */
        int currPosition;

        /**
         * array list of data in a node
         */
        public E[] dataList;

        /**
         * int variable to track the action taken by the program used to determine
         * whether an item should be set or removed in set() or remove()
         */
        int action;

        /**
         * Default constructor
         */
        public StoutListIterator() {
            currPosition = 0;
            action = -1;

            dataList = (E[]) new Comparable[size];
            int tempPos = 0;
            Node temp = head.next;
            while (temp != null) {
                for (int i = 0; i < temp.count; i++) {
                    dataList[tempPos] = temp.data[i];
                    tempPos++;
                }
                temp = temp.next;
            }
            ;
        }

        /**
         * Constructor finds node at a given position.
         *
         * @param pos
         */
        public StoutListIterator(int pos) {
            currPosition = pos;
            action = -1;

            dataList = (E[]) new Comparable[size];
            int tempPos = 0;
            Node temp = head.next;
            while (temp != null) {
                for (int i = 0; i < temp.count; i++) {
                    dataList[tempPos] = temp.data[i];
                    tempPos++;
                }
                temp = temp.next;
            }
            ;

        }

        @Override
        public boolean hasNext() {
            // Check if the current position is within the bounds of the list size
            if (currPosition < size) {
                return true; // there is a next element
            } else {
                return false; // Otherwise, there is no next element
            }
        }


        @Override
        public void add(E item) {
            if (item == null)
                throw new NullPointerException();

            // Add the item at the current position in the list
            StoutList.this.add(currPosition, item);
            currPosition++;

            // Rebuild the dataList after the addition of a new element
            dataList = (E[]) new Comparable[size];
            int tempPosition = 0;
            Node temp = head.next;
            while (temp != null) {
                for (int i = 0; i < temp.count; i++) {
                    dataList[tempPosition] = temp.data[i];
                    tempPosition++;
                }
                temp = temp.next;
            }
            action = -1; // Reset action
        }


        @Override
        public E next() {

            if (!hasNext())
                throw new IndexOutOfBoundsException();

            action = nextAction; // Set to next

            return dataList[currPosition++]; // Return the next element and increment the position
        }


        @Override
        public int nextIndex() {
            return currPosition;
        }


        @Override
        public void remove() {
            if (action == nextAction) {
                // Remove the element at the current position
                StoutList.this.remove(currPosition - 1);
                // Rebuild the dataList after removal
                dataList = (E[]) new Comparable[size];
                int tempPosition = 0;
                Node temp = head.next;
                while (temp != null) {
                    for (int i = 0; i < temp.count; i++) {
                        dataList[tempPosition] = temp.data[i];
                        tempPosition++;
                    }
                    temp = temp.next;
                }
                currPosition--;

                action = -1; // Reset action

                if (currPosition < 0)
                    currPosition = 0;

            } else if (action == prevAction) {
                // Remove the element at the current position (for previous action)
                StoutList.this.remove(currPosition);

                // Rebuild the dataList after removal
                dataList = (E[]) new Comparable[size];

                int tPos = 0;
                Node temp = head.next;
                while (temp != null) {

                    for (int i = 0; i < temp.count; i++) {
                        dataList[tPos] = temp.data[i];
                        tPos++;
                    }

                    temp = temp.next;
                }
                action = -1; // Reset action
            }
        }

        @Override
        public boolean hasPrevious() {
            return currPosition > 0;
        }

        @Override
        public int previousIndex() {
            return currPosition - 1;
        }



        @Override
        public void set(E item) {
            if (action == nextAction) {

                Node temp = head.next;

                int tempPosition = 0;

                while (temp != tail) {
                    if (tempPosition + temp.count <= currPosition - 1) {
                        tempPosition += temp.count;
                        temp = temp.next;
                        continue;
                    }

                    int offset = currPosition - tempPosition - 1;
                    temp.data[offset] = item; // Set the new value at the specified position
                    dataList[currPosition - 1] = item;
                    return;
                }
            } else if (action == prevAction) {
                Node temp = head.next;
                int tempPosition = 0;

                while (temp != null) {
                    if (tempPosition + temp.count <= currPosition) {
                        tempPosition = tempPosition + temp.count;

                        temp = temp.next;

                        continue;
                    }
                    int offset = currPosition - tempPosition;

                    temp.data[offset] = item; // Set the new value at the specified position

                    dataList[currPosition] = item;

                    return;
                }
            }
        }


        @Override
        public E previous() {
            if (!hasPrevious())
                throw new NoSuchElementException();

            action = prevAction; // Set the action to 'previous'

            currPosition--;

            return dataList[currPosition];
            // Return the previous element
        }


    }

    /**
     * Helper method to locate an specific item
     *
     * @param pos position of item that needs a info
     * @return NodeInfo of specific point of the list
     */
    private NodeInfo find(int pos) {
        Node temp = head.next;
        int currPos = 0; // the current position
        while (temp != tail) {
            if (currPos + temp.count <= pos) { // If current position + node count is less than or equal to the target position
                currPos += temp.count; // Move the current position by the count
                temp = temp.next;
                continue;
            }
            // We have found the node containing the target position
            NodeInfo nodeInfo = new NodeInfo(temp, pos - currPos); // Create a NodeInfo object with the node and offset
            return nodeInfo; // Return the NodeInfo
        }
        return null; // If nothing is found
    }


    /**
     * Sort an array arr[] using the insertion sort algorithm in the NON-DECREASING
     * order.
     *
     * @param arr  array storing elements from the list
     * @param comp comparator used in sorting
     */
    private void insertionSort(E[] arr, Comparator<? super E> comp) {
        for (int i = 1; i < arr.length; i++) {
            E key = arr[i]; // Select the key element for comparison
            int j = i - 1; // Start from the element before the key
            while (j >= 0 && comp.compare(arr[j], key) > 0) { // Compare and shift elements until the correct position is found
                arr[j + 1] = arr[j]; // Shift element to the right
                j--; // Move to the previous element
            }
            arr[j + 1] = key; // Insert the key in the correct position
        }
    }


    /**
     * Comparator class for elements of type E.
     */
    class ElementComparator<E extends Comparable<E>> implements Comparator<E> {
        @Override
        public int compare(E arg0, E arg1) {
            return arg0.compareTo(arg1); // Compare elements using their natural ordering
        }
    }



    /**
     * Sort arr[] using the bubble sort algorithm in the NON-INCREASING order. For a
     * description of bubble sort please refer to Section 6.1 in the project
     * description. You must use the compareTo() method from an implementation of
     * the Comparable interface by the class E or ? super E.
     *
     * @param arr array holding elements from the list
     */
    private void bubbleSort(E[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) { // Compare adjacent elements and swap if necessary
                if (arr[j].compareTo(arr[j + 1]) < 0) { // Compare elements in non-increasing order
                    E temp = arr[j]; // Swap elements
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                }
            }
        }
    }

    /**
     * Helper class to represent a specific point of the list
     */
    private class NodeInfo {
        public Node node;
        public int offset;

        public NodeInfo(Node node, int offset) {
            this.node = node;
            this.offset = offset;
        }
    }



}