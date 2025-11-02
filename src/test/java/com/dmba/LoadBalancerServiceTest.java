package com.dmba;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoadBalancerServiceTest {

    @Test
    void getRequest() {
        List<Backend> backendList = new ArrayList<>();
        backendList.add(new Backend("1"));
        backendList.add(new Backend("2"));
        backendList.add(new Backend("2"));
        backendList.add(new Backend("3"));
        backendList.add(new Backend("3"));
        LoadBalancerService loadBalancerService =  new LoadBalancerService(new RandomStrategy(), backendList);

        var result = loadBalancerService.getRequest();

        assertEquals(result.getId(), "1");
    }

    @Test
    void releaseRequest() {
    }
}