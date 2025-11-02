package com.dmba;

import java.util.Optional;

public interface ShorterUrl {
    String getShortUrl(String fullUrl);
    Optional<String> getFullUrl(String shortUrl);
}
