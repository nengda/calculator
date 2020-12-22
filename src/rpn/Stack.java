package rpn;

import java.util.stream.Stream;

public interface Stack<E> {
    void push(E e);
    E pop();
    int size();
    int allSize();
    Stream<E> stream();
}
