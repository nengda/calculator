package rpn;

import io.vavr.control.Either;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/*
 * Main interface for RPN calculator, responsible of maintaining
 * and executing calculation stack, with two public methods:
 * Push: to add one or many numbers or operators to the calculation stack.
 * Abstract syntax tree is constructed on the fly.
 * Evaluate: to execute the commands in the stack
 */
public interface Calculator<E extends Operatable<E>> {
    List<Either<Exception, E>> evaluate();

    Optional<Exception> push(String elementOrOperator);

    /*
     * Pushing a list of numbers or operators.
     */
    default Optional<Exception> push(String ... elementOrOperators) {
        Iterator<String> iter = Arrays.asList(elementOrOperators).iterator();
        Optional<Exception> maybeException = Optional.empty();
        while (iter.hasNext() && !maybeException.isPresent()) {
            maybeException = push(iter.next());
        }
        return maybeException;
    }

    static<O extends Operatable<O>> Calculator<O> getInstance(
            Function<String, Optional<O>> operatableProvider, Supplier<Stack<Command<O>>> stackProvider) {
        EagerBuildLazyEvalCalculator<O> c = new EagerBuildLazyEvalCalculator<>(operatableProvider, stackProvider);
        return c;
    }


    /*
     * This implementation of RPN calculator builds the calculation stack
     * eagerly (and sequentially), and evaluates the stack lazily (also sequentially).
     * Depending on the future multi-threading requirement, another algorithm
     * can be supported such that the calculation stack is built and evaluated
     * concurrently and lazily. For example, when there's a large number of commands
     * (say millions) coming in, they can be first added into an event queue, and before
     * evaluation starts, the event queue is divided into smaller chunks and one calculation
     * stack is built for each of these chunks in parallel. Then these stacks are combined
     * into a single stack and evaluated in parallel, based on the dependency graph of the
     * stack.
     */
    final class EagerBuildLazyEvalCalculator<E extends Operatable<E>> implements Calculator<E> {
        private Stack<Command<E>> storage;
        private CommandRegistry<E> commandRegistry;
        private Supplier<Stack<Command<E>>> stackProvider;
        private CacheStrategy<E> cachingStrategy = CacheStrategy.get();

        private EagerBuildLazyEvalCalculator() {};

        private EagerBuildLazyEvalCalculator(
                Function<String, Optional<E>> operatableProvider, Supplier<Stack<Command<E>>> stackProvider) {
            this.storage = stackProvider.get();
            this.stackProvider = stackProvider;
            this.commandRegistry = CommandRegistry.getInstance(operatableProvider);
        }

        /*
         * Sequential evaluation.
         * Possible to use parallel stream for potential performance optimization
         */
        @Override
        public List<Either<Exception, E>> evaluate() {
            return storage.stream().map(c -> c.apply()).collect(Collectors.toList());
        }

        /*
         * Not thread safe, assuming push is called sequentially.
         * Possible to refactor it into a thread-safe version depending
         * on thread safty requirement.
         */
        @Override
        public Optional<Exception> push(String elementOrOperator) {
            Optional<CommandRegistry<E>.CommandDefinition> maybeDefinition = commandRegistry.get(elementOrOperator);
            if (!maybeDefinition.isPresent())
                return Optional.of(new IllegalArgumentException("Unknown element or operator: " + elementOrOperator));
            else {
                CommandRegistry<E>.CommandDefinition definition = maybeDefinition.get();
                CommandRegistry.ArgSize size = definition.getArgSize();
                try {
                    List<Command<E>> commands;
                    if (size.equals(CommandRegistry.ArgSize.All)) {
                        List<Command<E>> all = storage.allElements();
                        storage = stackProvider.get();
                        commands = definition.getTransformer().apply(all);
                    } else {
                        if (storage.size() < size.value())
                            throw new IllegalArgumentException(
                                    "Operator '" + elementOrOperator + "' (position " + (storage.allSize() + 1) + "), insufficient parameter");
                        commands = definition.getTransformer().apply(storage.pop(size.value()));
                    }
                    storage.push(commands.stream().map(c -> cachingStrategy.apply(c)).collect(Collectors.toList()));
                } catch (Exception e) {
                    return Optional.of(e);
                }
                return Optional.empty();
            }
        }
    }
}
