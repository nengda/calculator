package rpn;

import io.vavr.control.Either;

import java.util.List;

/*
 * Interface for all calculation commands.
 * apply: execute the command.
 * undo: revert the execution.
 * size: measure the element count in command execution tree
 */
public interface Command<R> extends Measurable {
    Either<Exception, R> apply();
    List<Command<R>> undo();
}
