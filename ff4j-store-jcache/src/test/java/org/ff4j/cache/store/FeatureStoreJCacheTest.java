package org.ff4j.cache.store;

/*
 * #%L
 * ff4j-store-redis
 * %%
 * Copyright (C) 2013 - 2014 Ff4J
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.Map;

import org.ehcache.jsr107.EhcacheCachingProvider;
import org.ff4j.feature.Feature;
import org.ff4j.store.FeatureStore;
import org.ff4j.store.FeatureStoreJCache;
import org.ff4j.test.store.FeatureStoreTestSupport;
import org.junit.After;

/**
 * Test to work with Redis as a store.
 * 
 * @author <a href="mailto:cedrick.lunven@gmail.com">Cedrick LUNVEN</a>
 * 
 * TODO : FeatureTestSupport ?
 */
public class FeatureStoreJCacheTest extends FeatureStoreTestSupport {
    
    /** {@inheritDoc} */
    protected FeatureStore initStore() {
        FeatureStoreJCache ehcacheStore = new FeatureStoreJCache(EhcacheCachingProvider.class.getName());
        ehcacheStore.importFeaturesFromXmlFile("ff4j.xml");
        ehcacheStore.setCacheManager(ehcacheStore.getCacheManager());
        return ehcacheStore;
    }
    
    /**
     * Clean store after each test (avoid duplication)
     */
    @After
    public void cleanStore() {
        Map < String, Feature > f = testedStore.findAll();
        for (String key : f.keySet()) {
            testedStore.delete(key);
        }
    }

}
