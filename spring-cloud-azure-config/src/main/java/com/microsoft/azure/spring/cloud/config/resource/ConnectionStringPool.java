/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.cloud.config.resource;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Container for connection string of stores
 */
public class ConnectionStringPool {
    private Map<String, ConnectionString> connectionStringMap = new ConcurrentHashMap<>();

    public void put(String endpoint, ConnectionString connectionString) {
        Assert.hasText(endpoint, "Config store endpoint cannot be null or empty.");
        Assert.notNull(connectionString, "Connection string should not be null.");
        this.connectionStringMap.put(endpoint, connectionString);
    }

    public void put(String endpoint, String connectionString) {
        this.put(endpoint, ConnectionString.of(connectionString));
    }

    @Nullable
    public ConnectionString get(String endpoint) {
        return this.connectionStringMap.get(endpoint);
    }

    public Map<String, ConnectionString> getAll() {
        return this.connectionStringMap;
    }
}

