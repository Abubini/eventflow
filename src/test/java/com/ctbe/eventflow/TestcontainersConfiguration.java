package com.ctbe.eventflow;

import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
    // Using H2 in-memory for unit tests (see application-test.properties)
}
