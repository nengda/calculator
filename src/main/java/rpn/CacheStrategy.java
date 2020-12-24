package rpn;

import io.vavr.control.Either;

import java.util.List;
import java.util.function.Function;

/**
 * Factory for caching strategies.
 * An naive strategy is implemented to cache all command execution
 **/
public interface CacheStrategy<E> extends Function<Command<E>, Command<E>>{

    static<E> CacheStrategy<E> get() {
        return new CacheStrategy<E>() {
            /*
             * Decorator to provide caching capability.
             * Implementation mimics the double locking mechanism of a singleton
             * */
            @Override
            public Command<E> apply(Command<E> uncached) {
                return new Command<E>() {
                    private volatile Either<Exception, E> result;
                    private Object lock = new Object();

                    @Override
                    public Either<Exception, E> apply() {
                        if (result == null) {
                            synchronized (lock) {
                                if (result == null)
                                    result = uncached.apply();
                            }
                        }
                        return result;
                    }

                    @Override
                    public List<Command<E>> undo() {
                        return uncached.undo();
                    }

                    @Override
                    public int size() {
                        return uncached.size();
                    }
                };
            }
        };
    }
}
