package com.dmba;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoadBalancerServiceTest {


    LoadBalancer loadBalancer;
    List<Backend> backendLis;
    SelectionStrategy selectionStrategy;

//    @BeforeEach
//    void setUp() {
//        backendLis = new ArrayList<>();
//        backendLis.add(new Backend("1"));
//        backendLis.add(new Backend("2"));
//        backendLis.add(new Backend("2"));
//        backendLis.add(new Backend("3"));
//        backendLis.add(new Backend("3"));
//        selectionStrategy = new LeastConnections();
//        loadBalancer = new LoadBalancerService(backendLis, selectionStrategy);
//    }



    @Test
    void getRequest() {
        backendLis = new ArrayList<>();
        backendLis.add(new Backend("1"));
        backendLis.add(new Backend("2"));
        backendLis.add(new Backend("2"));
        backendLis.add(new Backend("3"));
        backendLis.add(new Backend("3"));
        selectionStrategy = new LeastConnections();
        loadBalancer = new LoadBalancerService(backendLis, selectionStrategy);


        Backend backend = loadBalancer.getRequest();
        assertEquals(backend.getId(), "1");
    }

    @Test
    void releaseRequest() {
    }
}