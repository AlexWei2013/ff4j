package org.ff4j.feature;

import static org.ff4j.utils.JsonUtils.attributeAsJson;
import static org.ff4j.utils.JsonUtils.cacheJson;
import static org.ff4j.utils.JsonUtils.collectionAsJson;
import static org.ff4j.utils.JsonUtils.objectAsJson;
import static org.ff4j.utils.Util.requireHasLength;
import static org.ff4j.utils.Util.requireNotNull;
import static org.ff4j.utils.Util.setOf;

/*
 * #%L
 * ff4j-core
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

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ff4j.audit.AuditTrail;
import org.ff4j.conf.XmlParser;
import org.ff4j.exception.FeatureAlreadyExistException;
import org.ff4j.exception.FeatureNotFoundException;
import org.ff4j.exception.GroupNotFoundException;
import org.ff4j.exception.ItemAlreadyExistException;
import org.ff4j.exception.ItemNotFoundException;
import org.ff4j.store.FF4jRepositorySupport;
import org.ff4j.store.FF4jRepository;
import org.ff4j.store.FF4jRepositoryListener;
import org.ff4j.utils.Util;

/**
 * Specialization of the {@link FF4jRepository} to work with {@link Feature}. Implementation
 * of dedicated operation related to feature through {@link FeatureStore}.
 *
 * @author Cedrick Lunven (@clunven)
 */
public abstract class FeatureStoreSupport extends FF4jRepositorySupport<Feature, FeatureStoreListener > implements FeatureStore {

    /** serialVersionUID. */
    private static final long serialVersionUID = -7450698535116107530L;

    /** Json Attribute. */
    public static final String JSON_ATTRIBUTE_FEATURECOUNT = "featuresCount";
    
    /** Json Attribute. */
    public static final String JSON_ATTRIBUTE_FEATURENAMES = "featuresNames";
    
    /** Json Attribute. */
    public static final String JSON_ATTRIBUTE_GROUPCOUNT   = "groupsCount";
    
    /** Json Attribute. */
    public static final String JSON_ATTRIBUTE_GROUPNAMES   = "groupNames";
    
    /** Json Attribute. */
    public static final String JSON_ATTRIBUTE_CLASSNAME    = "className";
    
    /** Listener Name. */
    private static final String LISTENERNAME_AUDIT = "FeatureStoreAuditListener";
    
    /**
     * Validate feature uid.
     *
     * @param uid
     *      target uid
     */
    protected void assertFeatureExist(String uid) {
        try {
            assertItemExist(uid);
        } catch(ItemNotFoundException infEx) {
            throw new FeatureNotFoundException(uid, infEx);
        }
    }
    
    /**
     * Check that current feature does not exist.
     *
     * @param uid
     *      current feature identifier.s
     */
    protected void assertFeatureNotExist(String uid) {
        try {
            assertItemNotExist(uid);
        } catch(ItemAlreadyExistException infEx) {
            throw new FeatureAlreadyExistException(uid, infEx);
        }
    }
    
    /**
     * Validate feature uid.
     *
     * @param uid
     *      target uid
     */
    protected void assertGroupExist(String groupName) {
        Util.requireHasLength(groupName);
        if (!existGroup(groupName)) {
            throw new GroupNotFoundException(groupName);
        }
    }
    
    /**
     * Validate feature uid.
     *
     * @param uid
     *      target uid
     */
    protected void assertFeatureNotNull(Feature feature) {
        if (feature == null) {
            throw new IllegalArgumentException("Feature cannot be null nor empty");
        }
    }
    
    /** {@inheritDoc} */
    protected String toJson() {
        StringBuilder sb = new StringBuilder("{");
        sb.append(attributeAsJson(JSON_ATTRIBUTE_CLASSNAME, this.getClass().getCanonicalName()));
        // Features
        Set<String> myFeatures = setOf(findAll().map(Feature::getUid));
        sb.append(attributeAsJson(JSON_ATTRIBUTE_FEATURECOUNT, myFeatures.size()));
        sb.append(objectAsJson(JSON_ATTRIBUTE_FEATURENAMES, collectionAsJson(myFeatures)));
        // Groups
        Set<String> groups = readAllGroups().collect(Collectors.toSet());
        sb.append(attributeAsJson(JSON_ATTRIBUTE_GROUPCOUNT, groups.size()));
        sb.append(objectAsJson(JSON_ATTRIBUTE_GROUPNAMES, collectionAsJson(groups)));
        // Cache
        sb.append(cacheJson(this));
        sb.append("}");
        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return toJson();
    }
    
    /**
     * Initialize store from XML Configuration File.
     *
     * @param xmlConfFile
     *      xml configuration file
     */
    protected Stream < Feature > importFeaturesFromXmlFile(String xmlConfFile) {
        // Load as Inputstream
        requireHasLength(xmlConfFile);
        InputStream xmlIS = getClass().getClassLoader().getResourceAsStream(xmlConfFile);
        requireNotNull(xmlIS);
        // Use the Feature Parser
        Map < String, Feature > features = new XmlParser().parseConfigurationFile(xmlIS).getFeatures();
        save(features.values());
        return features.values().stream();
    }
    
    /** {@inheritDoc} */
    @Override
    public void update(Feature feature) {
        preUpdate(feature);
        updateFeature(feature);
        // Notify all listeners registered (like AuditTrail)
        this.notify(l -> l.onUpdate(feature));
    }
    
    /** {@inheritDoc} */
    @Override
    public void toggleOn(String uid) {
        assertFeatureExist(uid);
        updateFeature(read(uid).toggleOn());
        // AuditTrail or any subscriber
        notify(l -> l.onToggleOnFeature(uid));
    }
    
    /** {@inheritDoc} */
    @Override
    public void toggleOff(String uid) {
        assertFeatureExist(uid);
        updateFeature(read(uid).toggleOff());
        // AuditTrail or any subscriber
        notify(l -> l.onToggleOffFeature(uid));
    }
    
    /** {@inheritDoc} */
    @Override
    public Feature read(String uid) {
        try {
            return super.read(uid);
        } catch(ItemNotFoundException ie) {
            // Specialize exception
            throw new FeatureNotFoundException(ie.getEntityUid(), ie);
        }
    }
    
    /** {@inheritDoc} */    
    @Override
    public void create(Feature fp) {
        assertFeatureNotNull(fp);
        assertFeatureNotExist(fp.getUid());
        createFeature(fp);
        this.notify(l -> l.onCreate(fp));
    }
    
    /** {@inheritDoc} */
    @Override
    public void delete(String uid) {
        assertFeatureExist(uid);
        deleteFeature(uid);
        this.notify(l -> l.onDelete(uid));
    }
    
    /** {@inheritDoc} */
    @Override
    public void deleteAll() {
        deleteAllFeatures();
        this.notify(l -> l.onDeleteAll());
    }
    
    /** {@inheritDoc} */
    @Override
    public void toggleOnGroup(String groupName) {
        this.notify(l -> l.onToggleOnGroup(groupName));
        readGroup(groupName).map(Feature::getUid).forEach(this::toggleOn);
    }

    /** {@inheritDoc} */
    @Override
    public void toggleOffGroup(String groupName) {
        this.notify(l -> l.onToggleOffGroup(groupName));
        readGroup(groupName).map(Feature::getUid).forEach(this::toggleOff);
    }

    /** {@inheritDoc} */
    @Override
    public void addToGroup(String uid, String groupName) {
        requireHasLength(uid);
        requireHasLength(groupName);
        this.notify(l -> l.onAddFeatureToGroup(uid, groupName));
        updateFeature(read(uid).setGroup(groupName));
    }
    
    /** {@inheritDoc} */
    @Override
    public void removeFromGroup(String uid, String groupName) {
        assertFeatureExist(uid);
        assertGroupExist(groupName);
        this.notify(l -> l.onRemoveFeatureFromGroup(uid, groupName));
        updateFeature(findById(uid).get().setGroup(""));
    }
    
    /**
     * Specialization of 'create' without any concern about audit nor cache.
     *
     * @param fp
     *      target feature
     */
    protected abstract void createFeature(Feature feature);
    
    /**
     * Specialization of 'update' without any concern about audit nor cache.
     *
     * @param fp
     *      target feature
     */
    protected abstract void updateFeature(Feature feature);
    
    /**
     * Specialization of 'delete' without any concern about audit nor cache.
     *
     * @param fp
     *      target feature
     */
    protected abstract void deleteFeature(String uid);
    
    /**
     * Specialization of 'delete' without any concern about audit nor cache.
     *
     * @param fp
     *      target feature
     */
    protected abstract void deleteAllFeatures();
    
    /** {@inheritDoc} */
    @Override
    public void registerListener(String name, FF4jRepositoryListener<Feature> listener) {
        // Enforce subclass to reach AbstractObservable.registerListener(..)
        registerListener(name, (FeatureStoreListener) listener);
    }
    
    /** {@inheritDoc} */
    @Override
    public void registerAuditListener(AuditTrail auditTrail) {
        this.registerListener(LISTENERNAME_AUDIT, new FeatureStoreListenerAudit(auditTrail));
    }
    
    /** {@inheritDoc} */
    @Override
    public void unRegisterAuditListener() {
        this.unregisterListener(LISTENERNAME_AUDIT);
    }
    
}
