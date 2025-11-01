package com.dmba;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {

    }
}

class UrlShorterService {

    public static final String ALPHABET = "qwertyuiopQWERTYUIOP123456789";

    public static final Integer ALPHABET_SIZE = ALPHABET.length();

    public ConcurrentMap<String, String> shorterMap = new ConcurrentHashMap<>();
}