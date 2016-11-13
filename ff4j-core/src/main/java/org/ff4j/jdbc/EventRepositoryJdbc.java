package org.ff4j.jdbc;


import static org.ff4j.jdbc.JdbcStoreConstants.COL_EVENT_HOSTNAME;
import static org.ff4j.jdbc.JdbcStoreConstants.COL_EVENT_NAME;
import static org.ff4j.jdbc.JdbcStoreConstants.COL_EVENT_SOURCE;
import static org.ff4j.jdbc.JdbcStoreConstants.COL_EVENT_USER;

/*
 * #%L ff4j-core %% Copyright (C) 2013 - 2015 Ff4J %% Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License. #L%
 */

import static org.ff4j.utils.JdbcUtils.closeConnection;
import static org.ff4j.utils.JdbcUtils.closeResultSet;
import static org.ff4j.utils.JdbcUtils.closeStatement;
import static org.ff4j.utils.JdbcUtils.executeUpdate;
import static org.ff4j.utils.JdbcUtils.isTableExist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.ff4j.audit.Event;
import org.ff4j.audit.EventQueryDefinition;
import org.ff4j.audit.EventSeries;
import org.ff4j.audit.MutableHitCount;
import org.ff4j.audit.TimeSeriesChart;
import org.ff4j.exception.AuditAccessException;
import org.ff4j.exception.FeatureAccessException;
import org.ff4j.store.AbstractEventRepository;
import org.ff4j.store.EventRepository;
import org.ff4j.utils.Util;

/**
 * Implementation of in memory {@link EventRepository} with limited events.
 * 
 * @author Cedrick Lunven (@clunven)
 */
public class EventRepositoryJdbc extends AbstractEventRepository {
    
    /** Error message 1. */
    public static final String CANNOT_READ_AUDITTABLE =  "Cannot read audit table from DB";

	/** error message. */
    public static final String CANNOT_BUILD_PIE_CHART_FROM_REPOSITORY = "Cannot build PieChart from repository, ";
    
    /** Access to storage. */
    private DataSource dataSource;
    
    /** Query builder. */
    private JdbcQueryBuilder queryBuilder;
    
    /**
     * Constructor from DataSource.
     * 
     * @param jdbcDS
     *            native jdbc datasource
     */
    public EventRepositoryJdbc(DataSource jdbcDS) {
        this.dataSource = jdbcDS;
    }
    
    /** {@inheritDoc} */
    @Override
    public void createSchema() {
        DataSource       ds = getDataSource();
        JdbcQueryBuilder qb = getQueryBuilder();
        if (!isTableExist(ds, qb.getTableNameAudit())) {
            executeUpdate(ds, qb.sqlCreateTableAudit());
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean saveEvent(Event evt) {
        Util.assertEvent(evt);
        Connection sqlConn = null;
        PreparedStatement stmt = null;
        try {
            sqlConn = dataSource.getConnection();
            stmt = new JdbcEventMapper(sqlConn, getQueryBuilder()).toStore(evt);
            stmt.executeUpdate();
        } catch(Exception exc) {
            throw new AuditAccessException("Cannot insert event into DB (" + exc.getClass() + ") "+ exc.getCause(), exc);
        } finally {
           closeStatement(stmt);
           closeConnection(sqlConn);
        }
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    public Optional < Event > findById(String uuid, Long timestamp) {
        Util.assertHasLength(uuid);
        Connection          sqlConn = null;
        PreparedStatement   ps = null;
        ResultSet           rs = null;
        try {
            sqlConn = getDataSource().getConnection();
            ps = sqlConn.prepareStatement(getQueryBuilder().getEventByUuidQuery());
            ps.setString(1, uuid);
            rs = ps.executeQuery();
            if (rs.next()) {
               return Optional.of(new JdbcEventMapper(sqlConn, getQueryBuilder()).fromStore(rs));
            }
            return Optional.empty();
        } catch (SQLException sqlEX) {
            throw new IllegalStateException("CANNOT_READ_AUDITTABLE", sqlEX);
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
            closeConnection(sqlConn);
        }
    }
     
    /** {@inheritDoc} */
    @Override
    public void purgeAuditTrail(EventQueryDefinition qDef) {
        Util.assertNotNull(qDef);
        Connection          sqlConn = null;
        PreparedStatement   ps = null;
        ResultSet           rs = null;
        try {
           sqlConn = getDataSource().getConnection();
           ps = sqlConn.prepareStatement(getQueryBuilder().getPurgeAuditTrailQuery(qDef));
           ps.setTimestamp(1, new java.sql.Timestamp(qDef.getFrom()));
           ps.setTimestamp(2, new java.sql.Timestamp(qDef.getTo()));
           ps.executeUpdate();
        } catch (SQLException sqlEX) {
            throw new IllegalStateException("CANNOT_READ_AUDITTABLE", sqlEX);
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
            closeConnection(sqlConn);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public void purgeFeatureUsage(EventQueryDefinition qDef) {
        Util.assertNotNull(qDef);
        Connection          sqlConn = null;
        PreparedStatement   ps = null;
        ResultSet           rs = null;
        try {
           sqlConn = getDataSource().getConnection();
            ps = sqlConn.prepareStatement(getQueryBuilder().getPurgeFeatureUsageQuery(qDef));
            ps.setTimestamp(1, new java.sql.Timestamp(qDef.getFrom()));
            ps.setTimestamp(2, new java.sql.Timestamp(qDef.getTo()));
            ps.executeUpdate();
        } catch (SQLException sqlEX) {
            throw new IllegalStateException("CANNOT_READ_AUDITTABLE", sqlEX);
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
            closeConnection(sqlConn);
        }
    }
    
    /** {@inheritDoc} */
    private EventSeries searchEvents(String sqlQuery, long from, long to) {
        Connection          sqlConn = null;
        PreparedStatement   ps = null;
        ResultSet           rs = null;
        EventSeries         es = new EventSeries();
        try {
            sqlConn = getDataSource().getConnection();
            ps = sqlConn.prepareStatement(sqlQuery);
            ps.setTimestamp(1, new Timestamp(from));
            ps.setTimestamp(2, new Timestamp(to));
            rs = ps.executeQuery();
            while (rs.next()) {
                es.add(new JdbcEventMapper(sqlConn, getQueryBuilder()).fromStore(rs));
            }
        } catch (SQLException sqlEX) {
            throw new IllegalStateException("CANNOT_READ_AUDITTABLE", sqlEX);
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
            closeConnection(sqlConn);
        }
        return es;
    }
    
    /** {@inheritDoc} */
    private  Map<String, MutableHitCount> computeHitCount(String sqlQuery, String columnName, long from, long to) {
        Connection          sqlConn = null;
        PreparedStatement   ps = null;
        ResultSet           rs = null;
        Map<String, MutableHitCount>  hitCount = new HashMap<String, MutableHitCount>();
        try {
            // Returns features
            sqlConn = dataSource.getConnection();
            ps = sqlConn.prepareStatement(sqlQuery);
            ps.setTimestamp(1, new Timestamp(from));
            ps.setTimestamp(2, new Timestamp(to));
            rs = ps.executeQuery();
            while (rs.next()) {
                hitCount.put(rs.getString(columnName), new MutableHitCount(rs.getInt("NB")));
            } 
        } catch (SQLException sqlEX) {
            throw new FeatureAccessException(CANNOT_BUILD_PIE_CHART_FROM_REPOSITORY, sqlEX);
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
            closeConnection(sqlConn);
        }
        return hitCount;
    }
    
    /** {@inheritDoc} */
    @Override
    public EventSeries getAuditTrail(EventQueryDefinition qDef) {
        return searchEvents(getQueryBuilder().getSelectAuditTrailQuery(qDef), qDef.getFrom(), qDef.getTo());
    }

    /** {@inheritDoc} */
    @Override
    public EventSeries searchFeatureUsageEvents(EventQueryDefinition qDef) {
        return searchEvents(getQueryBuilder().getSelectFeatureUsageQuery(qDef), qDef.getFrom(), qDef.getTo());
    }
        
    /** {@inheritDoc} */
    @Override
    public Map<String, MutableHitCount> getFeatureUsageHitCount(EventQueryDefinition query) {
        return computeHitCount(getQueryBuilder().getFeaturesHitCount(), COL_EVENT_NAME, query.getFrom(), query.getTo());
    }
    
    /** {@inheritDoc} */
    @Override
    public Map<String, MutableHitCount> getHostHitCount(EventQueryDefinition query) {
        return computeHitCount(getQueryBuilder().getHostHitCount(), COL_EVENT_HOSTNAME, query.getFrom(), query.getTo());
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, MutableHitCount> getUserHitCount(EventQueryDefinition query) {
        return computeHitCount(getQueryBuilder().getUserHitCount(), COL_EVENT_USER, query.getFrom(), query.getTo());
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, MutableHitCount> getSourceHitCount(EventQueryDefinition query) {
        return computeHitCount(getQueryBuilder().getSourceHitCount(), COL_EVENT_SOURCE, query.getFrom(), query.getTo());
    }

    /** {@inheritDoc} */
    @Override
    public TimeSeriesChart getFeatureUsageHistory(EventQueryDefinition query, TimeUnit units) {
        // Create the interval depending on units
        TimeSeriesChart tsc = new TimeSeriesChart(query.getFrom(), query.getTo(), units);
        // Search All events
        Iterator<Event> iterEvent = searchFeatureUsageEvents(query).iterator();
        // Dispatch events into time slots
        while (iterEvent.hasNext()) {
            tsc.addEvent(iterEvent.next());
        }
        return tsc;
    }
   
    /**
     * Getter accessor for attribute 'dataSource'.
     *
     * @return current value of 'dataSource'
     */
    public DataSource getDataSource() {
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
    public long count() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void delete(String entityId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void delete(Iterable<? extends Event> entities) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void delete(Event entity) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deleteAll() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean exists(String id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Stream<Event> findAll() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Stream<Event> findAll(Iterable<String> ids) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<Event> findById(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Event read(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void create(Event entity) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void update(Event entity) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void save(Collection<Event> entities) {
        // TODO Auto-generated method stub
        
    }
}
