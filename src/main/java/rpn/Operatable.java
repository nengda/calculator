package rpn;

/**
 * interface for all number operations.
 **/
public interface Operatable<T> {
    T add(T arg);
    T substract(T arg);
    T divide(T arg);
    T multiply(T arg);
    T sqrt();
}
