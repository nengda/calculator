package rpn;

import io.vavr.control.Either;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/*
 * Main class for RPN calculator, responsible of maintaining
 * and executing calculation stack, with two public methods:
 * Push: to add one or many numbers or operators to the calculation stack.
 * Abstract syntax tree is constructed on the fly.
 * Evaluate: to execute the commands in the stack
 */
public final class Calculator<E extends Operatable<E>> {
    private Stack<Command<E>> storage;
    private CommandRegistry<E> commandRegistry;
    private Supplier<Stack<Command<E>>> stackProvider;

    private Calculator() {};

    public static<O extends Operatable<O>> Calculator<O> getInstance(
            Function<String, Optional<O>> operatableProvider, Supplier<Stack<Command<O>>> stackProvider) {
        Calculator<O> c = new Calculator<>();
        c.storage = stackProvider.get();
        c.stackProvider = stackProvider;
        c.commandRegistry = CommandRegistry.getInstance(operatableProvider);
        return c;
    }

    /*
     * Sequential evaluation.
     * Possible to use parallel stream for potential performance optimization
     */
    public List<Either<Exception, E>> evaluate() {
        return storage.stream().map(c -> c.apply()).collect(Collectors.toList());
    }

    /*
     * Pushing a list of numbers or operators.
     */
    public Optional<Exception> push(String ... elementOrOperators) {
        Iterator<String> iter = Arrays.asList(elementOrOperators).iterator();
        Optional<Exception> maybeException = Optional.empty();
        while (iter.hasNext() && !maybeException.isPresent()) {
            maybeException = push(iter.next());
        }
        return maybeException;
    }

    /*
    * Not thread safe, assuming push is called sequentially.
    * Possible to refactor it into a thread-safe version depending
    * on thread safty requirement.
    */
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
                storage.push(commands.stream().map(c -> cacheableCommand(c)).collect(Collectors.toList()));
            } catch (Exception e) {
                return Optional.of(e);
            }
            return Optional.empty();
        }
    }

    /*
    * Decorator to provide caching capability.
    * Implementation mimics the double locking mechanism of a singleton
    * */
    private Command<E> cacheableCommand(Command<E> uncached) {
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

    public static void main(String[] args) {
        Calculator<Number> rpn = Calculator.getInstance(Number::of, new Supplier<Stack<Command<Number>>>() {
            @Override
            public Stack<Command<Number>> get() {
                return Stack.newInstance();
            }
        });
        Scanner in = new Scanner(System.in);

        while(true) {
            System.out.println("Enter list of numbers and operators. Supported Operators are: + - * / undo clear.");
            String s = in.nextLine();
            if (s.equals("quit")) return;
            Optional<Exception> maybeException = rpn.push(s.split(" "));
            if (maybeException.isPresent())
                System.out.println(maybeException.get().getMessage());
            String result = rpn.evaluate().stream().map(r -> {
                if (r.isLeft())
                    return r.getLeft().getMessage();
                else
                    return r.get().toString();}).collect(Collectors.joining(" "));
            System.out.println("Stack: " + result);
        }

    }
}
