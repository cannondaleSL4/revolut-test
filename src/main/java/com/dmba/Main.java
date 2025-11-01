package com.dmba;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {

    }
}

class MyOwnEncoder {
    public static final String ALPHABET = "qwertyuiopQWERTYUIOP123456789";

    public static final Integer ALPHABET_SIZE = ALPHABET.length();

    public static final Integer LENGTH_OF_SHORT = 6;

    public String getShort(String longAddress) {
        for(String s: longAddress) {

        }
    }

    public String getLong(String shortAddress) {

    }
}

class UrlShorterService {

    public MyOwnEncoder encoder;

    public ConcurrentMap<String, String> shorterMap = new ConcurrentHashMap<>();

    public UrlShorterService(MyOwnEncoder encoder) {
        this.encoder = encoder;
    }
}