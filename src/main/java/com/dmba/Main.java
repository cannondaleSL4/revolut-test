package com.dmba;

import java.util.Optional;

public class Main {
    public static void main(String[] args) {

        ShortUrlService shortUrlService = new ShortUrlService();


        String longUrl = "http://revolut.com/my-very-very-long-url";
        String shortOne = shortUrlService.getShortUrl(longUrl);
        String longUrlFromTheShortUrl = shortUrlService.getLongUrl(shortOne).get();

        System.out.println("Look here is my long URL:" + longUrl);
        System.out.println("Look here is my short URL:" + shortOne);
        System.out.println("Look here is my longUrlFrom URL:" + longUrlFromTheShortUrl);


    }
}

class CustomShortUrlException extends RuntimeException {
    public CustomShortUrlException(String message) {
        super(message);
    }
}

interface ShortUrlInterface {
    String getShortUrl(String longUrl) throws CustomShortUrlException;
    Optional<String> getLongUrl(String shortUrl);
}

class ShortUrlService implements ShortUrlInterface {


    @Override
    public String getShortUrl(String longUrl) throws CustomShortUrlException {
        return "";
    }

    @Override
    public Optional<String> getLongUrl(String shortUrl) {
        return Optional.empty();
    }
}
