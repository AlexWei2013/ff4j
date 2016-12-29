package org.ff4j.test.store;

import org.ff4j.feature.FeatureStore;

/*
 * #%L ff4j-core $Id:$ $HeadURL:$ %% Copyright (C) 2013 Ff4J %% Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License. #L%
 */

import org.ff4j.inmemory.FeatureStoreInMemory;
import org.junit.Test;

/**
 * All TEST LOGIC is in super class to be processed on EACH STORE.
 * 
 * @author clunven
 */
public class InMemoryFeatureStoreTest extends FeatureStoreTestSupport {

    /** {@inheritDoc} */
    @Override
    public FeatureStore initStore() {
        return  new FeatureStoreInMemory("test-ff4j-features.xml");
    }

    /**
     * TDD.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUnitFeatureInitialization3() {
        // Given
        // 'invalid.xml' file does not exist.
        new FeatureStoreInMemory("invalid.xml");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testWithInvalidFileFailed() {
        new FeatureStoreInMemory("");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testWithInvalidFileFailed2() {
        new FeatureStoreInMemory((String) null);
    }

}
