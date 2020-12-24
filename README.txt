RPN Calculator

# Run the command line Calculator:
mvn compile exec:java

# test
mvn test

Assumptions
1. The operators enter to the calculator sequentially. Though it can be extended based on future multi-threading requirements, thread safety is not guaranteed in this implementation.
2. The sqrt of 2 in the second example is 1.4142135623, which suggests ROUND_DOWN as the default rounding method.
3. There's a bit ambiguity in the definition of position for stage 3, or in the error message of example 8 (the input size is 10 but warning suggests error occurs at position 15). This implementation assumes position to be the position of operator at current stack.
4. Clear is not a reversible action, i.e. once it's applied, undo cannot reverse it.

Design
The design of RPN calculator closely follows Single Responsibility Principle. It decouples command definition (business logic, what to execute) from its runtime behavior (how it's executed at runtime). The benefit is new math operator can be added to registry easily (one liner with a set of pre-defined helper functions), without worrying about any execution problems (concurrent execution, thread-safty or caching). On the other hand, any enhancement on runtime execution (e.g. enable caching, concurrent evaluation) can benefit all commands without touching the business logic.

Following Open-Closed Principle, interfaces of Operatable and Stack is abstracted away from the coupling of JDK implementation (BigDecimal or choice of Stack/ConcurrentLinkedDeque/ArrayDeque). Benefit is obvious: different stack implementation (including our own implementation) could be picked without worrying about other parts of the program.

Type safety is another principal followed closely in the design of Canvas commands. In a nutshell, the goal of type safety is to eliminate as many programming bugs as possible at compile time, where the cost of mistake is lowest. For example, ArgSize is chosen over raw int to capture unintentional use of negative int at compile time. For another example, the return type of Either<Exception, E> suggests the result of the evaluation can either be number or exception. The caller is obliged to build in the exception handling together with result processing.