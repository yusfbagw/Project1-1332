//Author: Yusuf Bagwan
//GT User: ybagwan3
//GTID: 903891335

package refactor;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class StaticEndlessLinkedListNew<T> extends StaticEndlessLinkedList<T> {
    
    //Constructors must state the name of the class. 
    public StaticEndlessLinkedListNew() {
        head = null;
        size = 0;
        
    }

    /**
     * Adds the element to the front of the list.
     *
     * @param data the data to add to the front of the list
     * @throws IllegalArgumentException if data is null
     * @implSpec {@code O(1)} runtime
     */
    @Override
    public void addFirst(T data) {
        // TODO Auto-generated method stub
        //head = head.getNext();
        //Error checking
        if (data == null) {
            throw new IllegalArgumentException("Data can't be null");
        }
        if (size == 0) {
            Node<T> newNode = new Node<T>(data);
            head = newNode;
            size++;
            head.setNext(head);
        } 
        else {
            //Creating the new node.
            Node<T> newNode = new Node<T>(head.getData());
            //The heads data is set to the data inputted.
            //head.data = data;
            head.setData(data);
            //the newNode.next = head.next;
            newNode.setNext(head.getNext());
            //head.next = newNode;
            head.setNext(newNode);
            //size++;
            size++;
        }

    }
    /**
     * Adds the element to the back of the list.
     *
     * @param data the data to add to the back of the list
     * @throws IllegalArgumentException if data is null
     * @implSpec {@code O(1)} runtime
     */
    @Override
    public void addLast(T data) {
        // TODO Auto-generated method stub
        if (data == null) {
            throw new IllegalArgumentException("Data can't be null");
        }
        if (size == 0) {
            Node<T> newNode = new Node<T>(data);
            head = newNode;
            size++;
            head.setNext(head);
        } 
        else {
            Node<T> newNode = new Node<T>(head.getData());
            head.setData(data);
            newNode.setNext(head.getNext());
            head.setNext(newNode);
            head = head.getNext();
            size++;
        }
    }
    /**
     * Removes and returns the first element of the list.
     *
     * @return the data formerly located at the front of the list
     * @throws NoSuchElementException if the list is empty
     * @implSpec {@code O(1)} runtime
     */
    @Override
    public T removeFirst() {
        // TODO Auto-generated method stub
        if (size == 0) {
            throw new NoSuchElementException("There are no elements in the linked list.");
        }
        if (size == 1) {
            T data = head.getData();
            head = null;
            size--;
            return data;
        }
        else {
            T data = head.getData();
            head.setData(head.getNext().getData());
            head.setNext(head.getNext().getNext());
            size--;
            return data;
        }
    }
    /**
     * Removes and returns the last element of the list.
     *
     * @return the data formerly located at the back of the list
     * @throws NoSuchElementException if the list is empty
     * @implSpec {@code O(n)} runtime
     */
    @Override
    public T removeLast() {
        // TODO Auto-generated method stub
        if (size == 0) {
            throw new NoSuchElementException("There isn't anything in the list.");
        }
        if (size == 1) {
            T data = head.getData();
            head = null;
            size--;
            return data;
        }
        else {
            Node<T> curr = head;
            while (curr.getNext().getNext() != head ) {
                curr = curr.getNext();
            }
            T data = curr.getNext().getData();
            curr.setNext(curr.getNext().getNext());
            size--;
            return data;
        }
    }
    /**
     * Locates the first occurrence of the specified data and removes it.
     *
     * @param data the data to remove
     * @return the index of the data that was removed
     * @throws IllegalArgumentException if {@code data} is null
     * @throws NoSuchElementException if {@code data} is not in the list
     * @implSpec {@code O(n)} runtime
     */
    @Override
    public int removeValue(T data) {
        // TODO Auto-generated method stub
        if (data == null) {
            throw new IllegalArgumentException("data can't be null.");
        }
        if (size == 0) {
            throw new NoSuchElementException("The data isn't in the list.");
        }
        Node<T> curr = head;
        
        int found = 0;
        int i = 0;
        
        if (head.getData().equals(data)) {
            head = head.getNext();
            size--;
            return 0;
        }

        while (curr.getNext() != head) {
            if (curr.getNext().getData().equals(data)) {
                found = 1;
                i++;
                curr.setNext(curr.getNext().getNext());
                size--;
                break;
            }

            i++;
            curr = curr.getNext();
        }
        if (found == 0) {
            throw new NoSuchElementException("The data isn't in the list.");
        }
        return i;
    }
    /**
     * Returns the element at the specified index.
     *
     * @param index the index of the element to get
     * @return the data stored at the index in the list
     * @throws IndexOutOfBoundsException if index < 0 or index >= size
     * @implSpec
     * <p> {@code O(1)} runtime for index 0
     * <p> {@code O(n)} runtime for all other cases
     */
    @Override
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index is out of bounds, please pick a new index > 0 or make it < size.");
        }
        T data = null;
        if (index == 0) {
            data = head.getData();
        }
        Node<T> curr = head;
        for (int i = 0; i < size; i++) {
            if (i == index) {
                data = curr.getData();
                break;
            }
            curr = curr.getNext();
        }
        return data;
    }
    /**
     * Reverses the linked list.
     * <p>
     * This should modify the linked list in place, such that iterating
     * through it from the head would be traversed in reversed.
     * @implSpec
     * <p> {@code O(n)} runtime
     * <p> {@code O(1)} auxiliary space
     */
    @Override
    public void reverse() {
        // TODO Auto-generated method stub
    }
    /**
     * Retrieves your implementation of an iterator for
     * {@link StaticEndlessLinkedList}.
     * <p>
     * Unless the list is empty, your implementation should iterate
     * through the list in the order of front to back, then wrap
     * around to the beginning of the list. Follow {@link Iterator}
     * documentation for exception handling.
     *
     * @return an implementation of iterator
     * @implSpec
     * <p> All methods – {@code O(1)} auxiliary space
     * <p> {@link Iterator#next() next()} – {@code O(1)} runtime
     * <p> {@link Iterator#hasNext() hasNext()} – {@code O(1)} runtime
     * <p> Default methods – do not override
     */
    @Override
    public Iterator<T> iterator() {
        // TODO Auto-generated method stub
        return null;
    }
}
