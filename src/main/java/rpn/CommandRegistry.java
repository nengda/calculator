package rpn;

import io.vavr.control.Either;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Registry for all available stack and number operators.
 * Add new operator to register() method.
 *
 * Static registering is not used because Java doesn't have
 * good support for generics on static variables.
 **/
public final class CommandRegistry<E extends Operatable<E>> {
    private HashMap<String, CommandDefinition> operatorRegistry;
    private Function<String, Optional<E>> operatableProvider;

    private CommandRegistry() {
        operatorRegistry = new HashMap<>();
        register();
    }

    /**
     * Registers all available stack and number operators.
     **/
    private void register() {
        operatorRegistry.put("*", binaryCommandDefinition(new BiFunction<E, E, E>() {
            @Override
            public E apply(E e, E e2) {
                return e.multiply(e2);
            }
        }));
        operatorRegistry.put("+", binaryCommandDefinition(new BiFunction<E, E, E>() {
            @Override
            public E apply(E e, E e2) {
                return e.add(e2);
            }
        }));
        operatorRegistry.put("/", binaryCommandDefinition(new BiFunction<E, E, E>() {
            @Override
            public E apply(E e, E e2) {
                return e.divide(e2);
            }
        }));
        operatorRegistry.put("-", binaryCommandDefinition(new BiFunction<E, E, E>() {
            @Override
            public E apply(E e, E e2) {
                return e.substract(e2);
            }
        }));
        operatorRegistry.put("sqrt", unaryCommandDefinition(new Function<E, E>() {
            @Override
            public E apply(E e) {
                return e.sqrt();
            }
        }));
        operatorRegistry.put("clear", new CommandDefinition(ArgSize.All, new Function<List<Command<E>>, List<Command<E>>>() {
            @Override
            public List<Command<E>> apply(List<Command<E>> commands) {
                return Arrays.asList();
            }
        }));
        operatorRegistry.put("undo", new CommandDefinition(ArgSize.One, new Function<List<Command<E>>, List<Command<E>>>() {
            @Override
            public List<Command<E>> apply(List<Command<E>> commands) {
                return commands.get(0).undo();
            }
        }));
    }

    /**
     * Factory method for new instance of registry
     **/
    public static<O extends Operatable<O>> CommandRegistry<O> getInstance(Function<String, Optional<O>> operatableProvider) {
        CommandRegistry<O> registry = new CommandRegistry<>();
        registry.operatableProvider = operatableProvider;
        return registry;
    }

    /**
     * Get a registered command definition.
     *
     * Returns Optional.Empty if input is neither a number or
     * a registered operator.
     **/
    public Optional<CommandDefinition> get(String elementOrOperator) {
        if (operatorRegistry.containsKey(elementOrOperator))
            return Optional.of(operatorRegistry.get(elementOrOperator));
        else
            return operatableProvider.apply(elementOrOperator).map( e -> elementCommandDefinition(e));
    }

    /**
     * Helper function to build a command definition
     * for binary operator, e.g. *, /, +, -.
     **/
    private CommandDefinition binaryCommandDefinition(BiFunction<E, E, E> biOperator) {
        return new CommandDefinition(
                ArgSize.Two,
//                enableCache ? ExecutionStratagy.Cached : ExecutionStratagy.None,
                new Function<List<Command<E>>, List<Command<E>>>() {
            @Override
            public List<Command<E>> apply(List<Command<E>> commands) {
                Command<E> left = commands.get(0);
                Command<E> right = commands.get(1);
                return Arrays.asList(new Command<E>() {
                    @Override
                    public Either<Exception, E> apply() {
                        Either<Exception, E> leftEither = left.apply();
                        if (leftEither.isLeft()) return leftEither;
                        Either<Exception, E> rightEither = right.apply();
                        if (rightEither.isLeft()) return rightEither;
                        try {
                            E result = biOperator.apply(leftEither.get(), rightEither.get());
                            return Either.right(result);
                        } catch (Exception e) {
                            return Either.left(e);
                        }
                    }

                    @Override
                    public List<Command<E>> undo() {
                        return Arrays.asList(left, right);
                    }

                    @Override
                    public int size() {
                        return 1 + left.size() + right.size();
                    }
                });
            }
        });
    }

    /**
     * Helper function to build a command definition
     * for unary operator, e.g. n!, sqrt, cos, atan.
     **/
    private CommandDefinition unaryCommandDefinition(Function<E, E> operator) {
        return new CommandDefinition(
                ArgSize.One,
//                enableCache ? ExecutionStratagy.Cached : ExecutionStratagy.None,
                new Function<List<Command<E>>, List<Command<E>>>() {
            @Override
            public List<Command<E>> apply(List<Command<E>> commands) {
                return Arrays.asList(new Command<E>() {
                    @Override
                    public Either<Exception, E> apply() {
                        Either<Exception, E> that = commands.get(0).apply();
                        if (that.isLeft()) return that;
                        try {
                            E result = operator.apply(that.get());
                            return Either.right(result);
                        } catch (Exception e) {
                            return Either.left(e);
                        }
                    }

                    @Override
                    public List<Command<E>> undo() {
                        return commands;
                    }

                    @Override
                    public int size() {
                        return 1 + commands.get(0).size();
                    }
                });
            }
        });
    }

    /**
     * Helper function to build a command definition
     * for numbers.
     **/
    private CommandDefinition elementCommandDefinition(E elem) {
        return new CommandDefinition(ArgSize.Zero, new Function<List<Command<E>>, List<Command<E>>>() {
            @Override
            public List<Command<E>> apply(List<Command<E>> commands) {
                return Arrays.asList(new Command<E>() {
                    @Override
                    public Either<Exception, E> apply() {
                        return Either.right(elem);
                    }

                    @Override
                    public List<Command<E>> undo() {
                        return Arrays.asList();
                    }

                    @Override
                    public int size() {
                        return 1;
                    }
                });
            }
        });
    }
    /*
     * A command definition defines the profile of a command,
     * i.e. what to do about a command, including the command function,
     * or number of input commands needed to transform into a new command.
     */
    public class CommandDefinition {
        private ArgSize argSize;
        private Function<List<Command<E>>, List<Command<E>>> transformer;

        private CommandDefinition(
                ArgSize argSize,
                Function<List<Command<E>>, List<Command<E>>> transformer) {
            this.argSize = argSize;
            this.transformer = transformer;
        }

        public ArgSize getArgSize() {
            return argSize;
        }


        public Function<List<Command<E>>, List<Command<E>>> getTransformer() {
            return new Function<List<Command<E>>, List<Command<E>>>() {
                @Override
                public List<Command<E>> apply(List<Command<E>> commands) {
                    if (!argSize.equals(ArgSize.All) && commands.size() != argSize.value())
                        throw new IllegalStateException("Unexpected state: expects " + argSize.value() + " arguments but receives " + commands.size() + " arguments");
                    return transformer.apply(commands);
                }
            };
        }
    }

    /*
     * Number of input commands needed to transform into a new command.
     * To ensure type safty, Enum is used instead of raw int for
     * 1. The variation of argument size is limited.
     * 2. To capture unintentional use of negative int at compile time.
     */
    public enum ArgSize {
        Zero(0), One(1), Two(2), All(-1);

        private final int size;
        ArgSize(int size) {
            this.size = size;
        }

        public int value() { return size; }
    }

}
