package rpn;

import io.vavr.control.Either;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Calculator<E extends Operatable<E>> {
    private Stack<Command<E>> storage;
    private Function<String, Optional<E>> operatableProvider;
    private Supplier<Stack<Command<E>>> stackProvider;

    private Calculator() {};

    public static<O extends Operatable<O>> Calculator<O> getInstance(
            Function<String, Optional<O>> operatableProvider, Supplier<Stack<Command<O>>> stackProvider) {
        Calculator<O> c = new Calculator<>();
        c.storage = stackProvider.get();
        c.operatableProvider = operatableProvider;
        c.stackProvider = stackProvider;
        return c;
    }

    // parallel stream for potential performance optimization
    public List<Either<Exception, E>> evaluate() {
        return storage.stream().map(c -> c.apply()).collect(Collectors.toList());
    }

    public Optional<Exception> offer(String ... elementOrOperators) {
        Iterator<String> iter = Arrays.asList(elementOrOperators).iterator();
        Optional<Exception> maybeException = Optional.empty();
        while (iter.hasNext() && !maybeException.isPresent()) {
            maybeException = offer(iter.next());
        }
        return maybeException;
    }

    // sequential offering
    // offer is not thread safe
    public Optional<Exception> offer(String elementOrOperator) {
        Optional<Builder> maybeBuilder = getBuilder(elementOrOperator);
        if (!maybeBuilder.isPresent())
            return Optional.of(new IllegalArgumentException("Unknown element or operator: " + elementOrOperator));
        else {
            Builder builder = maybeBuilder.get();
            Optional maybeException = builder.validator().flatMap(v -> v.apply(storage));
            if (maybeException.isPresent()) return maybeException;

            if (builder instanceof ElementBuilder) {
                storage.push(((ElementBuilder) builder).build());
            } else if (builder instanceof UnaryBuilder) {
                Command<E> top = storage.pop();
                storage.push(((UnaryBuilder) builder).build(top));
            } else if (builder instanceof BinaryBuilder) {
                Command<E> right = storage.pop();
                Command<E> left = storage.pop();
                storage.push(((BinaryBuilder) builder).build(left, right));
            } else if (builder instanceof StackManipulator) {
                StackManipulator manipulator = (StackManipulator) builder;
                List<Command<E>> in = new LinkedList<>();
                int target = storage.size() - manipulator.getSize();
                while(storage.size() > target) {
                    in.add(storage.pop());
                }
                Iterator<Command<E>> out = manipulator.build(in).iterator();
                while (out.hasNext()) {
                    storage.push(out.next());
                }
            }
            return Optional.empty();
        }
    }

    private Optional<Builder> getBuilder(String elementOrOperator) {
        switch (elementOrOperator) {
            case "*":
                return Optional.of(new BinaryBuilder<E>(new BiFunction<E, E, E>() {
                    @Override
                    public E apply(E e, E e2) {
                        return e.multiply(e2);
                    }
                }, elementOrOperator));
            case "+":
                return Optional.of(new BinaryBuilder<E>(new BiFunction<E, E, E>() {
                    @Override
                    public E apply(E e, E e2) {
                        return e.add(e2);
                    }
                }, elementOrOperator));
            case "-":
                return Optional.of(new BinaryBuilder<E>(new BiFunction<E, E, E>() {
                    @Override
                    public E apply(E e, E e2) {
                        return e.substract(e2);
                    }
                }, elementOrOperator));
            case "/":
                return Optional.of(new BinaryBuilder<E>(new BiFunction<E, E, E>() {
                    @Override
                    public E apply(E e, E e2) {
                        return e.divide(e2);
                    }
                }, elementOrOperator));
            case "sqrt":
                return Optional.of(new UnaryBuilder<E>(new Function<E, E>() {
                    @Override
                    public E apply(E e) {
                        return e.sqrt();
                    }
                }, elementOrOperator));
            case "clear":
                return Optional.of(new StackManipulator(storage.size(), new Function<List<Command>, List<Command>>() {
                    @Override
                    public List<Command> apply(List<Command> commands) {
                        return new LinkedList<>();
                    }
                }, elementOrOperator));
            case "undo":
                return Optional.of(new StackManipulator(1, new Function<List<Command>, List<Command>>() {
                    @Override
                    public List<Command> apply(List<Command> commands) {
                        return commands.get(0).undo();
                    }
                }, elementOrOperator));
            default:
                return operatableProvider.apply(elementOrOperator).map( e -> new ElementBuilder(e));
        }
    }

    // internal command builders
    interface Builder {
        default Optional<Function<Stack, Optional<Exception>>> validator() {
            return Optional.empty();
        }
    }

    private class StackManipulator<Elem> implements Builder {
        private Function<List<Command<Elem>>, List<Command<Elem>>> manipulator;
        private int size;
        private String name;

        StackManipulator(int size, Function<List<Command<Elem>>, List<Command<Elem>>> manipulator, String name) {
            this.manipulator = manipulator;
            this.size = size;
            this.name = name;
        }

        @Override
        public Optional<Function<Stack, Optional<Exception>>> validator() {
            return Optional.of(Validators.sizeValidator(size, name));
        }

        public List<Command<Elem>> build(List<Command<Elem>> commands) {
            return manipulator.apply(commands);
        }

        public int getSize() {
            return size;
        }

    }

    private class ElementBuilder<Elem> implements Builder {
        private Elem elem;

        ElementBuilder(Elem elem) {
            this.elem = elem;
        }

        public Command<Elem> build() {
            return new Command<Elem>() {
                @Override
                public Either<Exception, Elem> apply() { return Either.right(elem); }

                @Override
                public List<Command<Elem>> undo() {
                    return Arrays.asList();
                }

                @Override
                public int size() { return 1; }
            };
        }
    }


    private class UnaryBuilder<Elem> implements Builder {
        private Function<Elem, Elem> operator;
        private String name;

        UnaryBuilder(Function<Elem, Elem> operator, String name) {
            this.operator = operator;
            this.name = name;
        }

        @Override
        public Optional<Function<Stack, Optional<Exception>>> validator() {
            return Optional.of(Validators.sizeValidator(1, name));
        }

        public Command<Elem> build(Command<Elem> command) {
            return new Command<Elem>() {
                @Override
                public Either<Exception, Elem> apply() {
                    Either<Exception, Elem> that = command.apply();
                    if (that.isLeft()) return that;
                    try {
                        Elem result = operator.apply(that.get());
                        return Either.right(result);
                    } catch (Exception e) {
                        return Either.left(e);
                    }
                }

                @Override
                public List<Command<Elem>> undo() {
                    return Arrays.asList(command);
                }

                @Override
                public int size() { return 1 + command.size(); }
            };
        }
    }

    private class BinaryBuilder<Elem> implements Builder {
        private BiFunction<Elem, Elem, Elem> biOperator;
        private String name;

        BinaryBuilder(BiFunction<Elem, Elem, Elem> biOperator, String name) {
            this.biOperator = biOperator;
            this.name = name;
        }

        @Override
        public Optional<Function<Stack, Optional<Exception>>> validator() {
            return Optional.of(Validators.sizeValidator(2, name));
        }

        public Command<Elem> build(Command<Elem> left, Command<Elem> right) {
            return new Command<Elem>() {
                @Override
                public Either<Exception, Elem> apply() {
                    Either<Exception, Elem> leftEither = left.apply();
                    if (leftEither.isLeft()) return leftEither;
                    Either<Exception, Elem> rightEither = right.apply();
                    if (rightEither.isLeft()) return rightEither;
                    try {
                        Elem result = biOperator.apply(leftEither.get(), rightEither.get());
                        return Either.right(result);
                    } catch (Exception e) {
                        return Either.left(e);
                    }
                }

                @Override
                public List<Command<Elem>> undo() { return Arrays.asList(left, right); }

                @Override
                public int size() { return 1 + left.size() + right.size(); }
            };
        }
    }

    private static class Validators {
        private Validators() {};

        public static Function<Stack, Optional<Exception>> sizeValidator(int limit, String operator) {
            return new Function<Stack, Optional<Exception>>() {
                @Override
                public Optional<Exception> apply(Stack stack) {
                    return stack.size() < limit
                            ? Optional.of(new IllegalArgumentException(
                                    "Operator '" + operator + "' (position " + (stack.allSize() + 1) + "), insufficient parameter"))
                            : Optional.empty();
                }
            };
        }
    }

}
