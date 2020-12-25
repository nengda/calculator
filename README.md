### RPN Calculator

````
# Run the command line Calculator:
mvn compile exec:java

# Run the test
mvn test
````

### Assumptions
1. The operators enter to the calculator sequentially. Though it can be extended based on future multi-threading requirements, thread safety is not guaranteed in this implementation.
1. The sqrt of 2 in the second example is 1.4142135623, which suggests `ROUND_DOWN` as the default rounding method.
1. There's a bit ambiguity in the definition of position for stage 3, or in the error message of example 8 (the input size is 10 but warning suggests error occurs at position 15). This implementation assumes position to be the position of operator at current stack.
1. Clear is not a reversible action, i.e. once it's applied, undo cannot reverse it.

### Design
The design of RPN calculator closely follows Single Responsibility Principle. It decouples command definition (business logic, what to execute) from its runtime behavior (how it's executed at runtime). The benefit is new math operator can be added to registry easily (one liner with a set of pre-defined helper functions), without worrying about any execution problems (concurrent execution, thread-safty or caching). On the other hand, any enhancement on runtime execution (e.g. enable caching, concurrent evaluation) can benefit all commands without touching the business logic. In this implementation, a naive caching strategy is applied to all commands but imagine in the future, we could potentially implement a dynamic caching strategy based on operator's historical execution profile (only caching the expensive operators)!

Following Open-Closed Principle, interfaces of Operatable and Stack is abstracted away from the coupling of JDK implementation (BigDecimal or choice of Stack/ConcurrentLinkedDeque/ArrayDeque). Benefit is obvious: different stack implementation (including our own implementation) could be picked without worrying about other parts of the program.

Type safety is another principal followed closely in the design of Canvas commands. In a nutshell, the goal of type safety is to eliminate as many programming bugs as possible at compile time, where the cost of mistake is lowest. For example, ArgSize is chosen over raw int to capture unintentional use of negative int at compile time. For another example, the return type of Either<Exception, E> suggests the result of the evaluation can either be number or exception. The caller is obliged to build in the exception handling together with result processing.

### Q/A
1. Why not throw an Exception?
 Either<Exception, E> is chosen to be Command's return type over throwing an Exception. For several reasons:
 - Exception throwing and catching increases the complexity of our program. Client code needs to prepare catching both checked and unchecked exceptions. The program enters
 - Exception disrupts our program's execution flow, jumping from the point at which the exception is thrown to whatever point, with whatever state, our program defines the catch point. And in our specific case, all following commands stops to execute (even they are still good to execute) when one command fails to execute.
 - Exception is more complicated to test. JUnit 5 has made it simpler to test exceptions, but it would be just as easy as testing other return values if exception is returned than thrown.

1. Why not reference BigDecimal directly, but create an Operatable interface? Seems to make things more complicated.
It allows a different implementation of math operators if BigDecimal no longer fits our need in the future. It also helps prepare the extension needed to bring it online.

1. Why not use bottom-up approach like the following?

```java
public class Add implements Operator {
    @Override
    public BigDecimal apply(NumberProvider provider) {
        return provider.nextNumber().add(provider.nextNumber());
    }
}

public class Caculator implements NumberProvider {
    private Stack<BigDecimal> numbers;

    @Override
    public BigDecimal nextNumber() {
        return numbers.pop();
    }

    public void process(String[] cmds) {
        for (String cmd: cmds) {
            Operator operator = OperatorFactory.get(cmd);
            if (operator != null) {
                BigDecimal result = operator.apply(this);
                ...
            }
        }
    }
}
```

The bottom-up approach has benifits like open for extension. Creating a new operator is just another implementation of the Operator interface. But one big drawback is it mixes the stack minipulation (provider.nextNumber) with the business logic (.add), which makes it harder to refactor if we want to consider thread safty in the future (each operator needs to care about thread safty in their own apply() method). It's also hard to achieve concurrent evaluation as each operator is evaluated eagerly and sequentially. For example "1 2 + 3 4 * -", ideally the addition and multiplication should be executed concurrently but bottom-up doesn't seem to be the best approach for the task. 

