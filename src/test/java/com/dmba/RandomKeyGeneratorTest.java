package com.dmba;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomKeyGeneratorTest {

    private RandomKeyGenerator randomKeyGenerator;

//    @BeforeEach
//    void setUp() {
//    }
//
//    @AfterEach
//    void tearDown() {
//    }

    @Test
    void randomGenerator() {
        randomKeyGenerator = new RandomKeyGenerator(6);
        assertTrue(randomKeyGenerator.randomGenerator().matches("[a-zA-Z0-9]{6}"));

    }
}