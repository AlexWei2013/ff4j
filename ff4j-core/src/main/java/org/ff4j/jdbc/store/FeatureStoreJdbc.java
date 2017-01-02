package org.ff4j.jdbc.store;

import static org.ff4j.utils.JdbcUtils.buildStatement;

import static org.ff4j.utils.Util.requireNotNull;

/*
 * #%L
 * ff4j-core
 * %%
 * Copyright (C) 2013 - 2015 FF4J
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

import static org.ff4j.utils.JdbcUtils.closeConnection;
import static org.ff4j.utils.JdbcUtils.executeUpdate;
import static org.ff4j.utils.JdbcUtils.isTableExist;
import static org.ff4j.utils.Util.requireHasLength;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.ff4j.exception.FeatureAccessException;
import org.ff4j.feature.Feature;
import org.ff4j.feature.FeatureStore;
import org.ff4j.feature.FeatureStoreSupport;
import org.ff4j.jdbc.JdbcQueryBuilder;
import org.ff4j.jdbc.JdbcConstants.FeaturePropertyColumns;
import org.ff4j.jdbc.JdbcConstants.FeaturesColumns;
import org.ff4j.jdbc.mapper.JdbcFeatureMapper;
import org.ff4j.jdbc.mapper.JdbcPropertyMapper;
import org.ff4j.property.Property;
import org.ff4j.utils.JdbcUtils;
import org.ff4j.utils.Util;

/**
 * Implementation of {@link FeatureStore} to work with RDBMS through JDBC.
 *
 * @author Cedrick Lunven (@clunven)
 */
public class FeatureStoreJdbc extends FeatureStoreSupport {

	/** serialVersionUID. */
    private static final long serialVersionUID = 7144391802850457781L;

    /** Error message 1. */
    public static final String CANNOT_CHECK_FEATURE_EXISTENCE_ERROR_RELATED_TO_DATABASE =
    		"Cannot check feature existence, error related to database";

    /** Error message 2. */
    public static final String CANNOT_UPDATE_FEATURES_DATABASE_SQL_ERROR =
    		"Cannot update features database, SQL ERROR";

    /** Access to storage. */
    private DataSource dataSource;

    /** Query builder. */
    private JdbcQueryBuilder queryBuilder;

    /** Default Constructor. */
    public FeatureStoreJdbc() {}

    /**
     * Constructor from DataSource.
     *
     * @param jdbcDS
     *            native jdbc datasource
     */
    public FeatureStoreJdbc(DataSource jdbcDS) {
        this.dataSource = jdbcDS;
    }

    /**
     * Constructor from DataSource.
     *
     * @param jdbcDS
     *            native jdbc datasource
     */
    public FeatureStoreJdbc(DataSource jdbcDS, String xmlConfFile) {
        this(jdbcDS);
        importFeaturesFromXmlFile(xmlConfFile);
    }
    
    /** {@inheritDoc} */
    @Override
    public void createSchema() {
        DataSource       ds = getDataSource();
        JdbcQueryBuilder qb = getQueryBuilder();
        if (!isTableExist(ds, qb.getTableNameFeatures())) {
            executeUpdate(ds, qb.sqlCreateTableFeature());
        }
        executeUpdate(ds, qb.sqlCreateTableFeatureProperties());
        executeUpdate(ds, qb.sqlCreateTableFeaturePermission());
        executeUpdate(ds, qb.sqlCreateTableFeatureStrategy());
        executeUpdate(ds, qb.sqlCreateTablePermission());
    }

    /** {@inheritDoc} */
    @Override
    public void toggleOn(String uid) {
        assertFeatureExist(uid);
        update(getQueryBuilder().sqlEditFeatureStatus(), 1, uid);
    }

    /** {@inheritDoc} */
    @Override
    public void toggleOff(String uid) {
        assertFeatureExist(uid);
        update(getQueryBuilder().sqlEditFeatureStatus(), 0, uid);
    }

    /** {@inheritDoc} */
    @Override
    public void toggleOnGroup(String groupName) {
        assertGroupExist(groupName);
        update(getQueryBuilder().sqlEditGroupStatus(), 1, groupName);
    }

    /** {@inheritDoc} */
    @Override
    public void toggleOffGroup(String groupName) {
        assertGroupExist(groupName);
        update(getQueryBuilder().sqlEditGroupStatus(), 0, groupName);
    }

    /** {@inheritDoc} */
    @Override
    public void addToGroup(String uid, String groupName) {
        assertFeatureExist(uid);
        requireHasLength(groupName);
        update(getQueryBuilder().sqlEditFeatureToGroup(), groupName, uid);
    }

    /** {@inheritDoc} */
    @Override
    public void removeFromGroup(String uid, String groupName) {
        assertFeatureExist(uid);
        assertGroupExist(groupName);
        Feature feat = read(uid);
        if (feat.getGroup().isPresent() && !feat.getGroup().get().equals(groupName)) {
            throw new IllegalArgumentException("'" + uid + "' is not in group '" + groupName + "'");
        }
        update(getQueryBuilder().sqlEditFeatureToGroup(), "", uid);
    }
    
    /** {@inheritDoc} */
    @Override
    public long count() {
        try (Connection sqlConn = getDataSource().getConnection()) {
            try (PreparedStatement ps = JdbcUtils.buildStatement(sqlConn, getQueryBuilder().sqlCountFeatures())) {
                try (ResultSet rs = ps.executeQuery()) {
                    //Query count always have return
                    rs.next();
                    return rs.getInt(1);
                }
            }
        } catch (SQLException sqlEX) {
            throw new FeatureAccessException(CANNOT_CHECK_FEATURE_EXISTENCE_ERROR_RELATED_TO_DATABASE, sqlEX);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean exists(String uid) {
    	requireHasLength(uid);
    	try (Connection sqlConn = getDataSource().getConnection()) {
    	    try(PreparedStatement ps1 = JdbcUtils.buildStatement(sqlConn, getQueryBuilder().sqlExistFeature(), uid)) {
    	        try (ResultSet rs1 = ps1.executeQuery()) {
    	            // Query count always have return a value
                    rs1.next();
    	            return 1 == rs1.getInt(1);
    	        }
    	    }
    	} catch (SQLException sqlEX) {
            throw new FeatureAccessException(CANNOT_CHECK_FEATURE_EXISTENCE_ERROR_RELATED_TO_DATABASE, sqlEX);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Optional < Feature > findById(String uid) {
        requireNotNull(uid);
        // Closeable sql connection
        Feature f = null;
        try (Connection sqlConn = getDataSource().getConnection()) {
            JdbcFeatureMapper  fmapper = new JdbcFeatureMapper(sqlConn, getQueryBuilder());
            JdbcPropertyMapper pmapper = new JdbcPropertyMapper(sqlConn, getQueryBuilder());
            // Get core feature
            try(PreparedStatement ps1 = sqlConn.prepareStatement(
                    getQueryBuilder().sqlSelectFeatureById())) {
                ps1.setString(1, uid);
                try (ResultSet rs1 = ps1.executeQuery()) {
                    if (!rs1.next()) {
                        return Optional.empty();
                    } else {
                        f = fmapper.fromStore(rs1);
                    }
                }
            }
            
            
            // Get strategies related to features
            try(PreparedStatement ps2 = sqlConn.prepareStatement(getQueryBuilder().sqlStrategyOfFeature())) {
                
            }
            
            /* Get AccessControlList related to features
            try(PreparedStatement ps2 = sqlConn.prepareStatement(getQueryBuilder().sqlSelectFeatureAccessControlList())) {
                ps2.setString(1, uid);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    while (rs2.next()) {
                        f.addPermissions(rs2.getString(RolesColumns.ROLE.colname()));
                    }   
                }
            }*/
            
            // Get Custom properties related to features
            try(PreparedStatement ps3 = sqlConn.prepareStatement(getQueryBuilder().sqlSelectCustomPropertiesOfFeature())) {
                ps3.setString(1, uid);
                try (ResultSet rs3 = ps3.executeQuery()) {
                    while (rs3.next()) {
                        f.addCustomProperty(pmapper.fromStore(rs3));
                    }   
                }
            } 
        } catch (SQLException sqlEX) {
            throw new FeatureAccessException(CANNOT_CHECK_FEATURE_EXISTENCE_ERROR_RELATED_TO_DATABASE, sqlEX);
        }
        return Optional.ofNullable(f);
    }
    
    /** {@inheritDoc} */
    @Override
    public void create(Feature feature) {
    	assertFeatureNotNull(feature);
    	assertFeatureNotExist(feature.getUid());
    	Connection sqlConn = null;
    	try {
            // Create connection
            sqlConn = getDataSource().getConnection();
    	    sqlConn.setAutoCommit(false);
    	    // Create core Feature
            JdbcFeatureMapper mapper = new JdbcFeatureMapper(sqlConn, getQueryBuilder());
            try (PreparedStatement ps1 = mapper.toStore(feature)) {
                ps1.executeUpdate();
            }
            /* Create roles
            if (feature.getPermissions().isPresent()) {
                // Do not use Lambda/Streams for exceptions
                for(String role : feature.getPermissions().get()) {
                    // Preparestament is closable
                    try(PreparedStatement ps2 = sqlConn.prepareStatement(getQueryBuilder().sqlInsertRoles())) {
                        ps2.setString(1, feature.getUid());
                        ps2.setString(2, role);
                        ps2.executeUpdate();  
                    }
                }
            }*/
            // Create customproperties
            if (feature.getCustomProperties().isPresent()) {
                JdbcPropertyMapper pmapper = new JdbcPropertyMapper(sqlConn, getQueryBuilder());
                for(Property<?> property : feature.getCustomProperties().get().values()) {
                    try(PreparedStatement ps = pmapper.customPropertytoStore(property, feature.getUid())) {
                        ps.executeUpdate();
                    }
                }
            }
            // Commit
            sqlConn.commit();
            sqlConn.setAutoCommit(true);
            
        } catch (SQLException sqlEX) {
            throw new FeatureAccessException(CANNOT_UPDATE_FEATURES_DATABASE_SQL_ERROR, sqlEX);
        } finally {
            closeConnection(sqlConn);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void delete(String uid) {
    	assertFeatureExist(uid);
        try (Connection sqlConn = getDataSource().getConnection()) {
            sqlConn.setAutoCommit(false);
            Feature fp = read(uid);
            if (fp.getCustomProperties().isPresent()) {
                try (PreparedStatement ps1 = 
                        sqlConn.prepareStatement(getQueryBuilder().sqlDeleteAllCustomPropertiesOfFeature())) {
                    ps1.setString(1, fp.getUid());
                    ps1.executeUpdate();
                }
            }
            /* Delete Roles
            if (fp.getPermissions().isPresent()) {
                try (PreparedStatement ps1 = 
                        sqlConn.prepareStatement(getQueryBuilder().sqlDeleteAllRolesOfFeature())) {
                    ps1.setString(1, fp.getUid());
                    ps1.executeUpdate();
                }
            }*/
            // Delete Feature
            try (PreparedStatement ps1 = sqlConn.prepareStatement(getQueryBuilder().sqlDeleteFeature())) {
                ps1.setString(1, fp.getUid());
                ps1.executeUpdate();
            }
            // Commit
            sqlConn.commit();
            sqlConn.setAutoCommit(true);
        } catch (SQLException sqlEX) {
            throw new FeatureAccessException(CANNOT_UPDATE_FEATURES_DATABASE_SQL_ERROR, sqlEX);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Stream < Feature > findAll() {
        LinkedHashMap<String, Feature> mapFP = new LinkedHashMap<String, Feature>();
        // Closeable sql connection
        try (Connection sqlConn = getDataSource().getConnection()) {
            // Get core feature
            JdbcFeatureMapper  fmapper = new JdbcFeatureMapper(sqlConn, getQueryBuilder());
            try(PreparedStatement ps1 = sqlConn.prepareStatement(getQueryBuilder().sqlSelectAllFeatures())) {
                try (ResultSet rs1 = ps1.executeQuery()) {
                    while (rs1.next()) {
                        Feature f = fmapper.fromStore(rs1);
                        mapFP.put(f.getUid(), f);
                    }
                }
            }
            /* Get Roles related to features
            try(PreparedStatement ps2 = sqlConn.prepareStatement(getQueryBuilder().sqlSelectAllRoles())) {
                try (ResultSet rs2 = ps2.executeQuery()) {
                    while (rs2.next()) {
                        mapFP.get(rs2.getString(RolesColumns.FEATURE_UID.colname()))
                            .addPermission(rs2.getString(RolesColumns.ROLE.colname()));
                    }   
                }
            }*/
            // Get Custom properties related to features
            JdbcPropertyMapper pmapper = new JdbcPropertyMapper(sqlConn, getQueryBuilder());
            try(PreparedStatement ps3 = sqlConn.prepareStatement(getQueryBuilder().sqlSelectAllCustomProperties())) {
                try (ResultSet rs3 = ps3.executeQuery()) {
                    while (rs3.next()) {
                        mapFP.get(rs3.getString(FeaturePropertyColumns.UID.colname()))
                             .addCustomProperty(pmapper.fromStore(rs3));
                    }   
                }
            }
            return mapFP.values().stream();
        } catch (SQLException sqlEX) {
            throw new FeatureAccessException(CANNOT_CHECK_FEATURE_EXISTENCE_ERROR_RELATED_TO_DATABASE, sqlEX);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Stream<String> readAllGroups() {
        Set<String> setOFGroup = new HashSet<String>();
        try (Connection sqlConn = getDataSource().getConnection()) {
            try(PreparedStatement ps1 = sqlConn.prepareStatement(getQueryBuilder().sqlSelectAllGroups())) {
                try (ResultSet rs1 = ps1.executeQuery()) {
                    while (rs1.next()) {
                        String groupName = rs1.getString(FeaturesColumns.GROUPNAME.colname());
                        if (Util.hasLength(groupName)) {
                            setOFGroup.add(groupName);
                        }  
                    }
                }
            }
            return setOFGroup.stream();
        } catch (SQLException sqlEX) {
            throw new FeatureAccessException("Cannot list groups, error related to database", sqlEX);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void deleteAll() {
        try (Connection sqlConn = getDataSource().getConnection()) {
            try(PreparedStatement ps1 = sqlConn.prepareStatement(getQueryBuilder().sqlDeleteAllCustomProperties())) {
                ps1.executeUpdate();
            }
            try(PreparedStatement ps3 = sqlConn.prepareStatement(getQueryBuilder().sqlDeleteAllFeatures())) {
                ps3.executeUpdate();
            }
        } catch (SQLException sqlEX) {
            throw new FeatureAccessException(CANNOT_CHECK_FEATURE_EXISTENCE_ERROR_RELATED_TO_DATABASE, sqlEX);
        }
    }

    /**
     * Ease creation of properties in Database.
     *
     * @param uid
     *      target unique identifier
     * @param props
     *      target properties.
     */
    public void createCustomProperties(String uid, Collection <Property<?> > props) {
        Util.requireNotNull(uid);
        if (props == null) return;
        try (Connection sqlConn = getDataSource().getConnection()) {
            // Begin TX
            sqlConn.setAutoCommit(false);
            // Queries
            for (Property<?> pp : props) {
                JdbcPropertyMapper mapper = new JdbcPropertyMapper(sqlConn, getQueryBuilder());
                try(PreparedStatement ps = mapper.customPropertytoStore(pp, uid)) {
                    ps.executeUpdate();
                }
            }
            // End TX
            sqlConn.commit();
            sqlConn.setAutoCommit(true);
        } catch (SQLException sqlEX) {
            throw new FeatureAccessException(CANNOT_CHECK_FEATURE_EXISTENCE_ERROR_RELATED_TO_DATABASE, sqlEX);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean existGroup(String groupName) {
    	requireHasLength(groupName);
        try (Connection sqlConn = dataSource.getConnection()) {
            try(PreparedStatement ps = sqlConn.prepareStatement(getQueryBuilder().sqlExistGroup())) {
                ps.setString(1, groupName);
                try(ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1) > 0;
                }
            }   
        } catch (SQLException sqlEX) {
            throw new FeatureAccessException(CANNOT_CHECK_FEATURE_EXISTENCE_ERROR_RELATED_TO_DATABASE, sqlEX);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Stream <Feature> readGroup(String groupName) {
    	assertGroupExist(groupName);
    	LinkedHashMap<String, Feature> mapFP = new LinkedHashMap<String, Feature>();
    	try (Connection sqlConn = dataSource.getConnection()) {
            // Feature Core
    	    JdbcFeatureMapper mapper = new JdbcFeatureMapper(sqlConn, getQueryBuilder());
    	    try(PreparedStatement ps = sqlConn.prepareStatement(getQueryBuilder().sqlSelectFeaturesOfGroup())) {
                ps.setString(1, groupName);
                try(ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Feature f = mapper.fromStore(rs);
                        mapFP.put(f.getUid(), f);
                    }
                }
            }
    	    /* Get Roles related to features
            try(PreparedStatement ps2 = sqlConn.prepareStatement(getQueryBuilder().sqlSelectAllRoles())) {
                try (ResultSet rs2 = ps2.executeQuery()) {
                    while (rs2.next()) {
                        String featureId = rs2.getString(RolesColumns.FEATURE_UID.colname());
                        if (mapFP.containsKey(featureId)) {
                            mapFP.get(featureId).addPermission(rs2.getString(RolesColumns.ROLE.colname()));
                        }
                    }   
                }
            }*/
            // Get Custom properties related to features
            JdbcPropertyMapper pmapper = new JdbcPropertyMapper(sqlConn, getQueryBuilder());
            try(PreparedStatement ps3 = sqlConn.prepareStatement(getQueryBuilder().sqlSelectAllCustomProperties())) {
                try (ResultSet rs3 = ps3.executeQuery()) {
                    while (rs3.next()) {
                        String featureId = rs3.getString(FeaturePropertyColumns.UID.colname());
                        if (mapFP.containsKey(featureId)) {
                            mapFP.get(featureId).addCustomProperty(pmapper.fromStore(rs3));
                        }
                    }   
                }
            }
            return mapFP.values().stream();
    	} catch (SQLException sqlEX) {
            throw new FeatureAccessException(CANNOT_CHECK_FEATURE_EXISTENCE_ERROR_RELATED_TO_DATABASE, sqlEX);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void update(Feature entity) {
        requireNotNull(entity);
        requireHasLength(entity.getUid());
        assertItemExist(entity.getUid());
        
        entity.setLastModified(LocalDateTime.now());
        entity.setCreationDate(entity.getCreationDate().orElse(entity.getLastModifiedDate().get()));
        delete(entity);
        assertFeatureNotExist(entity.getUid());
        create(entity);
        
    }
    
    /**
     * Utility method to perform UPDATE and DELETE operations.
     *
     * @param query
     *            target query
     * @param params
     *            sql query params
     */
    public void update(String query, Object... params) {
        try (Connection sqlConn = dataSource.getConnection()) {
            try (PreparedStatement ps = buildStatement(sqlConn, query, params)) {
                ps.executeUpdate();
            }
        } catch (SQLException sqlEX) {
            throw new FeatureAccessException(CANNOT_UPDATE_FEATURES_DATABASE_SQL_ERROR, sqlEX);
        }
    }

    /**
     * Getter accessor for attribute 'dataSource'.
     *
     * @return current value of 'dataSource'
     */
    public DataSource getDataSource() {
    	if (dataSource == null) {
    		throw new IllegalStateException("DataSource has not been initialized");
    	}
        return dataSource;
    }

    /**
     * Setter accessor for attribute 'dataSource'.
     *
     * @param dataSource
     *            new value for 'dataSource '
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

	/**
	 * @return the queryBuilder
	 */
	public JdbcQueryBuilder getQueryBuilder() {
		if (queryBuilder == null) {
			queryBuilder = new JdbcQueryBuilder();
		}
		return queryBuilder;
	}

	/**
	 * @param queryBuilder the queryBuilder to set
	 */
	public void setQueryBuilder(JdbcQueryBuilder queryBuilder) {
		this.queryBuilder = queryBuilder;
	}

    @Override
    protected void createFeature(Feature feature) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void updateFeature(Feature feature) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void deleteFeature(String uid) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void deleteAllFeatures() {
        // TODO Auto-generated method stub
        
    }

    

}
