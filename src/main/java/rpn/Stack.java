package rpn;

import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstracting jdk stack for future improvements
 * for example, CAS implementation of a stack.
 * The preliminary implementation assumes thread safty
 * requirement that it's ok for one thread to operate on
 * a snapshot of stack with other threads operating in the same time.
 *
 * For example, size/stream/allElements all operate on
 * a snapshot of the stack. It could be further optimized
 * with clarification of the thread safty requirement.
**/
public interface Stack<E> {
    void push(List<E> e);
    List<E> pop(int n);
    List<E> allElements();
    int size();
    int allSize();
    Stream<E> stream();

    static<M extends Measurable> Stack<M> newInstance() {
        return new Stack.PreliminaryStack<>();
    }

    // preliminary implementation of rpn stack
    final class PreliminaryStack<E extends Measurable> implements Stack<E> {
        private ConcurrentLinkedDeque<E> internal;
        private final Lock lock = new ReentrantLock();

        private PreliminaryStack() {
            internal = new ConcurrentLinkedDeque<>();
        }

        @Override
        public int size() {
            return internal.size();
        }

        @Override
        public int allSize() {
            return stream().mapToInt(e -> e.size()).sum();
        }

        @Override
        public Stream<E> stream() {
            return internal.stream();
        }

        @Override
        public void push(List<E> e) {
            Iterator<E> iter = e.iterator();
            try {
                lock.lock();
                while(iter.hasNext())
                    internal.offerLast(iter.next());
            } finally {
                lock.unlock();
            }
        }

        @Override
        public List<E> pop(int n) {
            if (n < 1 || n > size()) return Arrays.asList();
            int count = 0;
            List<E> result = new ArrayList<>(n);
            try {
                lock.lock();
                while(count < n) {
                    result.add(internal.pollLast());
                    count++;
                }
            } finally {
                lock.unlock();
            }
            Collections.reverse(result);
            return result;
        }

        @Override
        public List<E> allElements() {
            return stream().collect(Collectors.toList());
        }
    }
}
