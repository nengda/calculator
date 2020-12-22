package rpn;

import rpn.operatable.Multipliable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Optional;

public class Number implements Operatable<Number> {
    private static int DEFUALT_PRECISION = 15;
    private static int DEFUALT_DISPLAY_PRECISION = 10;
    private static int DEFUALT_ROUNDING = BigDecimal.ROUND_DOWN;
    private int precision;
    private DecimalFormat format;
    private BigDecimal value;

    private Number(){}

    public static Optional<Number> of(String value) {
        return of(value, DEFUALT_PRECISION, DEFUALT_DISPLAY_PRECISION);
    }

    public static Optional<Number> of(String value, int precision, int displayPrecision) {
        try {
            DecimalFormat format = new DecimalFormat("#." + String.join("", Collections.nCopies(displayPrecision, "#")));
            format.setRoundingMode(RoundingMode.valueOf(DEFUALT_ROUNDING));
            return Optional.of(of(new BigDecimal(value), precision, format));
        } catch (NumberFormatException|NullPointerException e) {
            return Optional.empty();
        }
    }

    public static Number of(BigDecimal value, int precision, DecimalFormat format) {
        Number n = new Number();
        n.value = value.setScale(precision, DEFUALT_ROUNDING);
        n.precision = precision;
        n.format = format;
        return n;
    }

    public BigDecimal getValue() {
        return value;
    }

    @Override
    public Number multiply(Number that) {
        return copy(this.value.multiply(that.getValue()));
    }

    @Override
    public Number sqrt() {
        return copy(BigDecimal.valueOf(Math.sqrt(this.value.doubleValue())));
    }

    @Override
    public Number add(Number that) {
        return copy(this.value.add(that.value));
    }

    @Override
    public Number divide(Number that) {
        return copy(this.value.divide(that.value, precision, DEFUALT_ROUNDING));
    }

    @Override
    public Number substract(Number that) {
        return copy(this.value.subtract(that.value));
    }

    private Number copy(BigDecimal that) {
        return of(that, this.precision, this.format);
    }

    @Override
    public String toString() {
        return format.format(value);
    }
}
