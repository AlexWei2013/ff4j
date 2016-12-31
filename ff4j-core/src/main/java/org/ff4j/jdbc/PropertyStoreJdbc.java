package org.ff4j.jdbc;

/*
 * #%L
 * ff4j-core
 * %%
 * Copyright (C) 2013 - 2016 FF4J
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


import static org.ff4j.utils.JdbcUtils.buildStatement;
import static org.ff4j.utils.JdbcUtils.executeUpdate;
import static org.ff4j.utils.JdbcUtils.isTableExist;
import static org.ff4j.utils.Util.requireHasLength;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.ff4j.exception.PropertyAccessException;
import org.ff4j.jdbc.JdbcConstants.PropertyColumns;
import org.ff4j.property.PropertyStoreSupport;
import org.ff4j.property.Property;
import org.ff4j.utils.Util;

/**
 * Access information related to properties within database.
 *
 * @author Cedrick Lunven (@clunven)
 */
public class PropertyStoreJdbc extends PropertyStoreSupport {

    /** serialVersionUID. */
    private static final long serialVersionUID = -1746222910983624609L;

    /** Access to storage. */
    private DataSource dataSource;
    
    /** Query builder. */
    private JdbcQueryBuilder queryBuilder;
    
    /** Default Constructor. */
    public PropertyStoreJdbc() {}
    
    /**
     * Constructor from DataSource.
     * 
     * @param jdbcDS
     *            native jdbc datasource
     */
    public PropertyStoreJdbc(DataSource jdbcDS) {
        this.dataSource = jdbcDS;
    }
    
    /**s
     * Constructor from DataSource.
     * 
     * @param jdbcDS
     *            native jdbc datasource
     */
    public PropertyStoreJdbc(DataSource jdbcDS, String xmlConfFile) {
        this(jdbcDS);
        importPropertiesFromXmlFile(xmlConfFile);
    }
    

    /** {@inheritDoc} */
    @Override
    public void createSchema() {
        DataSource       ds = getDataSource();
        JdbcQueryBuilder qb = getQueryBuilder();
        if (!isTableExist(ds, qb.getTableNameProperties())) {
            executeUpdate(ds, qb.sqlCreateTableProperties());
        }
    }
     
    /** {@inheritDoc} */
    @Override
    public boolean exists(String name) {
        requireHasLength(name);
        try (Connection sqlConn = getDataSource().getConnection()) {
            try(PreparedStatement ps1 = sqlConn.prepareStatement(getQueryBuilder().sqlExistProperty())) {
                ps1.setString(1, name);
                try (ResultSet rs1 = ps1.executeQuery()) {
                    rs1.next();
                    return 1 == rs1.getInt(1);
                }
            }
        } catch (SQLException sqlEX) {
           throw new PropertyAccessException("Cannot check feature existence, error related to database", sqlEX);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void create(Property<?> ap) {
        Util.requireNotNull(ap);
        try (Connection sqlConn = getDataSource().getConnection()) {
            JdbcPropertyMapper pmapper = new JdbcPropertyMapper(sqlConn, getQueryBuilder());
            try(PreparedStatement ps1 = pmapper.toStore(ap)) {
                ps1.executeUpdate();
            }
        } catch (SQLException sqlEX) {
            throw new PropertyAccessException("Cannot update properties database, SQL ERROR", sqlEX);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public long count() {
        return listPropertyNames().count();
    }

    /** {@inheritDoc} */
    @Override
    public Optional < Property<?> > findById(String name) {
        Util.requireHasLength(name);
        try (Connection sqlConn = getDataSource().getConnection()) {
            JdbcPropertyMapper pmapper = new JdbcPropertyMapper(sqlConn, getQueryBuilder());
            try(PreparedStatement ps1 = sqlConn.prepareStatement(getQueryBuilder().sqlSelectPropertyById())) {
                ps1.setString(1, name);
                try (ResultSet rs1 = ps1.executeQuery()) {
                    return (rs1.next()) ? 
                            Optional.of(pmapper.fromStore(rs1)) : Optional.empty();
                }
            }
        } catch (SQLException sqlEX) {
            throw new PropertyAccessException("Cannot check property existence, error related to database", sqlEX);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void update(String name, String newValue) {
        Util.requireHasLength(name);
        try (Connection sqlConn = getDataSource().getConnection()) {
            // Check existence
            Property<?> ab = read(name);
            // Check new value validity
            ab.fromString(newValue);
            try(PreparedStatement ps = buildStatement(sqlConn, getQueryBuilder().updateProperty(), newValue, name)) {
                ps.executeUpdate();
            }
        } catch (SQLException sqlEX) {
            throw new PropertyAccessException("Cannot update property database, SQL ERROR", sqlEX);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void update(Property<?> prop) {
        Util.requireNotNull(prop);
        Util.requireHasLength(prop.getUid());
        delete(prop.getUid());
        create(prop);
    }
   
    /** {@inheritDoc} */
    @Override
    public void delete(String name) {
        Util.requireHasLength(name);
        assertPropertyExist(name);
        try (Connection sqlConn = getDataSource().getConnection()) {
            try (PreparedStatement ps = buildStatement(sqlConn, getQueryBuilder().sqlDeleteProperty(), name)) {
                ps.executeUpdate();
            }
        } catch (SQLException sqlEX) {
            throw new PropertyAccessException("Cannot delete property database, SQL ERROR", sqlEX);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public Stream < Property<?> > findAll() {
        Collection <Property<?>> properties = new ArrayList<>();
        try (Connection sqlConn = getDataSource().getConnection()) {
            JdbcPropertyMapper  pmapper = new JdbcPropertyMapper(sqlConn, getQueryBuilder());
            try(PreparedStatement ps1 = sqlConn.prepareStatement(getQueryBuilder().sqlSelectAllProperties())) {
                try (ResultSet rs1 = ps1.executeQuery()) {
                    while (rs1.next()) {
                        properties.add(pmapper.fromStore(rs1));
                    }
                }
            }
            return properties.stream();
        } catch (SQLException sqlEX) {
            throw new PropertyAccessException("Cannot read properties within database, SQL ERROR", sqlEX);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public Stream<String> listPropertyNames() {
        Collection < String > propertyNames = new ArrayList<>();
        try (Connection sqlConn = getDataSource().getConnection()) {
            try(PreparedStatement ps1 = sqlConn.prepareStatement(getQueryBuilder().sqlSelectAllPropertyNames())) {
                try (ResultSet rs1 = ps1.executeQuery()) {
                    while (rs1.next()) {
                        propertyNames.add(rs1.getString(PropertyColumns.UID.colname()));
                    }
                }
            }
            return propertyNames.stream();
        } catch (SQLException sqlEX) {
            throw new PropertyAccessException("Cannot read properties within database, SQL ERROR", sqlEX);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void deleteAll() {
        try (Connection   sqlConn = getDataSource().getConnection()) {
            try(PreparedStatement ps = buildStatement(sqlConn, getQueryBuilder().sqlDeleteAllProperties())) {
                ps.executeUpdate();
            }
        } catch (SQLException sqlEX) {
            throw new PropertyAccessException("Cannot clear properties table, SQL ERROR", sqlEX);
        }
    }

    /**
     * Getter accessor for attribute 'dataSource'.
     *
     * @return
     *       current value of 'dataSource'
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Setter accessor for attribute 'dataSource'.
     * @param dataSource
     * 		new value for 'dataSource '
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
        
}
