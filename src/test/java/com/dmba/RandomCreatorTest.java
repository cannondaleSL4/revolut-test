package com.dmba;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomCreatorTest {


    @Test
    void getShortCode() {
        RandomCreator randomCreator = new RandomCreator(6);
        String shortCode =  randomCreator.getShortCode();

        assertTrue(shortCode.length() == 6);
        assertTrue(randomCreator.validateShortCode(shortCode));
    }

    @Test
    void validateShortCode() {
        RandomCreator randomCreator = new RandomCreator(6);
        assertFalse(randomCreator.validateShortCode("+-+-+-"));
        assertTrue(randomCreator.validateShortCode("123456"));
    }
}