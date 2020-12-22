package rpn;

import rpn.operatable.*;

public interface Operatable<T> extends
        Multipliable<T>,
        SequareRootable<T>,
        Addable<T>, Substractable<T>, Dividable<T> {
}
