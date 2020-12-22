package rpn;

import io.vavr.control.Either;

import java.util.List;

public interface Command<R> extends Measurable {
    Either<Exception, R> apply();
    List<Command<R>> undo();
}
