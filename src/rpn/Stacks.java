package rpn;

import java.util.stream.Stream;

public class Stacks {
    private Stacks() {}

    public static<M extends Command> Stack<M> newInstance() {
        return new PreliminaryStack<>();
    }

    // preliminary implementation of rpn stack
    private static class PreliminaryStack<E extends Measurable> implements Stack<E> {
        private java.util.Stack<E> internal;

        public PreliminaryStack() {
            internal = new java.util.Stack<>();
        }

        @Override
        public void push(E e) {
            internal.push(e);
        }

        @Override
        public E pop() {
            return internal.pop();
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
    }
}
