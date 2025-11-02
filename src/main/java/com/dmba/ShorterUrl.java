package com.dmba;

import java.util.Optional;

public interface ShorterUrl {
    String getShortUrl(String fullUrl) throws ShortUrlCustomException;
    Optional<String> getFullUrl(String shortUrl);
}
