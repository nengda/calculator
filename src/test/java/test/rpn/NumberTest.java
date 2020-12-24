package test.rpn;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import rpn.Number;

import java.math.BigDecimal;

public class NumberTest {
    @Test
    public void testCanCreateNumber() {
        assertFalse(Number.of("fail").isPresent());
        assertFalse(Number.of("").isPresent());
        assertFalse(Number.of(null).isPresent());
        assertEquals(Number.of("4").get().toString(), "4");
        assertEquals(Number.of("4.0").get().toString(), "4");
        assertEquals(Number.of("4.5").get().toString(), "4.5");
        assertEquals(Number.of("-4.5").get().toString(), "-4.5");
        assertEquals(Number.of("0").get().toString(), "0");
        assertEquals(Number.of("0.0000").get().toString(), "0");
        assertEquals(Number.of("500000.1234567899").get().toString(), "500000.1234567899");
        assertEquals(Number.of("500000.12345678993").get().toString(), "500000.1234567899");
        assertEquals(Number.of("500000.12345678996").get().toString(), "500000.1234567899");
        assertEquals(Number.of("500000.1234567899355").get().toString(), "500000.1234567899");
        assertEquals(Number.of("-500000.1234567899355").get().toString(), "-500000.1234567899");
        assertEquals(Number.of("500000.1234567899355").get().getValue().toString(), "500000.123456789935500");
        assertEquals(Number.of("500000.12345678").get().getValue().toString(), "500000.123456780000000");
    }

    @Test
    public void testCanOperateNumber() {
        // integer
        assertEquals(Number.of("4").get().multiply(Number.of("5").get()).toString(), "20");
        assertEquals(Number.of("4").get().add(Number.of("5").get()).toString(), "9");
        assertEquals(Number.of("4").get().substract(Number.of("5").get()).toString(), "-1");
        assertEquals(Number.of("4").get().divide(Number.of("5").get()).toString(), "0.8");
        assertEquals(Number.of("4").get().sqrt().toString(), "2");

        // double
        assertEquals(Number.of("4.545").get().multiply(Number.of("5.43").get()).toString(), "24.67935");
        assertEquals(Number.of("4.545").get().add(Number.of("5.43").get()).toString(), "9.975");
        assertEquals(Number.of("4.545").get().substract(Number.of("5.43").get()).toString(), "-0.885");
        assertEquals(Number.of("4.545").get().divide(Number.of("5.43").get()).toString(), "0.8370165745");
        assertEquals(Number.of("4.545").get().divide(Number.of("5.43").get()).getValue().toString(), "0.837016574585635");
        assertEquals(Number.of("4.545").get().sqrt().toString(), "2.1319005605");
        assertEquals(Number.of("4.545").get().sqrt().getValue().toString(), "2.131900560532784");
        assertEquals(Number.of("4.545").get().sqrt().sqrt().toString(), "1.460102928");
        assertEquals(Number.of("4.545").get().sqrt().sqrt().getValue().toString(), "1.460102928061163");

        // negative
        assertEquals(Number.of("-4.545").get().multiply(Number.of("5.43").get()).toString(), "-24.67935");
        assertEquals(Number.of("-4.545").get().add(Number.of("5.43").get()).toString(), "0.885");
        assertEquals(Number.of("-4.545").get().substract(Number.of("5.43").get()).toString(), "-9.975");
        assertEquals(Number.of("-4.545").get().divide(Number.of("5.43").get()).toString(), "-0.8370165745");
        assertEquals(Number.of("-4.545").get().divide(Number.of("5.43").get()).getValue().toString(), "-0.837016574585635");

        // exceptions
        assertThrows(ArithmeticException.class, () -> {
            Number.of("-4.545").get().divide(Number.of("0").get());
        });
        assertThrows(NumberFormatException.class, () -> {
            Number.of("-4.545").get().sqrt();
        });
        assertThrows(NullPointerException.class, () -> {
            Number.of("-4.545").get().divide(null);
        });

    }

}