package org.ff4j.test.property;

import static org.ff4j.utils.Util.setOf;

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


import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;

import org.ff4j.exception.InvalidPropertyTypeException;
import org.ff4j.property.domain.PropertyBigDecimal;
import org.ff4j.property.domain.PropertyBigInteger;
import org.ff4j.property.domain.PropertyBoolean;
import org.ff4j.property.domain.PropertyByte;
import org.ff4j.property.domain.PropertyCalendar;
import org.ff4j.property.domain.PropertyClass;
import org.ff4j.property.domain.PropertyDate;
import org.ff4j.property.domain.PropertyDouble;
import org.ff4j.property.domain.PropertyFloat;
import org.ff4j.property.domain.PropertyInt;
import org.ff4j.property.domain.PropertyLogLevel;
import org.ff4j.property.domain.PropertyLong;
import org.ff4j.property.domain.PropertyShort;
import org.ff4j.property.domain.PropertyString;
import org.junit.Assert;
import org.junit.Test;

/**
 * Basic testing of {@link PropertyString}.
 *
 * @author Cedrick Lunven (@clunven)</a>
 */
public class PropertyTest {
    
    @Test
    public void tesInitPropertyString() {
        PropertyString p1 = new PropertyString("p1");
        p1.setReadOnly(p1.isReadOnly());
        PropertyString p2 = new PropertyString("p2", "EAST", setOf("EAST","WEST","SOUTH","NORTH"));
        Assert.assertNotNull(p1.getUid());
        Assert.assertNotNull(p2.getFixedValues());
    }
    
    @Test
    public void tesInitPropertyDouble() {
        PropertyDouble d1 = new PropertyDouble("d1");
        PropertyDouble d2 = new PropertyDouble("d2", 1.2);
        PropertyDouble d3 = new PropertyDouble("d3", "1.3");
        Assert.assertNotNull(d1.getUid());
        Assert.assertNotNull(d2.getFixedValues());
        Assert.assertNotNull(d3.getUid());
    }
    
    @Test(expected = InvalidPropertyTypeException.class)
    public void testInitPropertyDoubleInvalid() {
        new PropertyDouble("d3", "invalid value");
    }
    
    @Test(expected = InvalidPropertyTypeException.class)
    public void testInitPropertyBigDecimalInvalid() {
        new PropertyBigDecimal("d3", "invalid value");
    }
    
    @Test(expected = InvalidPropertyTypeException.class)
    public void testInitPropertyBigIntegerInvalid() {
        new PropertyBigInteger("d3", "invalid value");
    }
    
    @Test(expected = InvalidPropertyTypeException.class)
    public void testInitPropertyBooleanInvalid() {
        new PropertyBoolean("d3", "invalid value");
    }
    
    @Test(expected = InvalidPropertyTypeException.class)
    public void testInitPropertyFloatInvalid() {
        new PropertyFloat("d3", "invalid value");
    }
    
    @Test(expected = InvalidPropertyTypeException.class)
    public void testInitPropertyIntInvalid() {
        new PropertyInt("d3", "invalid value");
    }
    
    @Test(expected = InvalidPropertyTypeException.class)
    public void testInitPropertyLongInvalid() {
        new PropertyLong("d3", "invalid value");
    }
    
    @Test(expected = InvalidPropertyTypeException.class)
    public void testInitPropertyShortInvalid() {
        new PropertyShort("d3", "invalid value");
    }
    
    @Test
    public void tesInitPropertyInt() {
        PropertyInt d1 = new PropertyInt("d1");
        PropertyInt d2 = new PropertyInt("d2", 1);
        PropertyInt d3 = new PropertyInt("d3", "2");
        PropertyInt d4 = new PropertyInt("d4", 2, setOf(0,1,2));
        Assert.assertNotNull(d1.getUid());
        Assert.assertNotNull(d2.getFixedValues());
        Assert.assertNotNull(d3.getUid());
        Assert.assertNotNull(d4.getUid());
        d4.toString();
    }
    
    @Test
    public void tesInitPropertyBoolean() {
        PropertyBoolean d1 = new PropertyBoolean("d1");
        PropertyBoolean d2 = new PropertyBoolean("d2", true);
        PropertyBoolean d3 = new PropertyBoolean("d3", "false");
        Assert.assertNotNull(d1.getUid());
        Assert.assertNotNull(d2.getFixedValues());
        Assert.assertNotNull(d3.getUid());
    }
    
    @Test
    public void tesInitPropertyFloat() {
        PropertyFloat d1 = new PropertyFloat("d1");
        PropertyFloat d2 = new PropertyFloat("d2", 1.1F);
        PropertyFloat d3 = new PropertyFloat("d3", "1.0");
        Assert.assertNotNull(d1.getUid());
        Assert.assertNotNull(d2.getUid());
        Assert.assertNotNull(d3.getUid());
        d1.fromString("1.1");
    }
    
    @Test
    public void tesInitPropertyLong() {
        PropertyLong d1 = new PropertyLong("d1");
        PropertyLong d2 = new PropertyLong("d2", 1L);
        PropertyLong d3 = new PropertyLong("d3", "1");
        Assert.assertNotNull(d1.getUid());
        Assert.assertNotNull(d2.getUid());
        Assert.assertNotNull(d3.getUid());
        d1.fromString("1");
    }
    
    @Test
    public void tesInitPropertyShort() {
        PropertyShort d1 = new PropertyShort("d1");
        PropertyShort d2 = new PropertyShort("d2", new Short("1"));
        PropertyShort d3 = new PropertyShort("d3", "2");
        Assert.assertNotNull(d1.getUid());
        Assert.assertNotNull(d2.getFixedValues());
        Assert.assertNotNull(d3.getUid());
        d1.fromString("1");
    }
    
    @Test
    public void tesInitPropertyBigInteger() {
        PropertyBigInteger d1 = new PropertyBigInteger("d1");
        PropertyBigInteger d2 = new PropertyBigInteger("d2", new BigInteger("1"));
        PropertyBigInteger d3 = new PropertyBigInteger("d3", "2");
        Assert.assertNotNull(d1.getUid());
        Assert.assertNotNull(d2.getFixedValues());
        Assert.assertNotNull(d3.getUid());
        d1.fromString("1");
    }
    
    @Test
    public void tesInitPropertyByte() {
        PropertyByte d1 = new PropertyByte("d1");
        PropertyByte d2 = new PropertyByte("d2", "1");
        PropertyByte d3 = new PropertyByte("d3", new Byte((byte) 100));
        Assert.assertNotNull(d1.getUid());
        Assert.assertNotNull(d2.getUid());
        Assert.assertNotNull(d3.getUid());
        d1.fromString("1");
        d1.fromString(null);
    }
    
    @Test(expected = InvalidPropertyTypeException.class)
    public void tesInitPropertyByteInvalidType() {
        PropertyByte d1 = new PropertyByte("d1");
        d1.fromString("Invalide");
    }
    
    @Test
    public void tesInitPropertyLogLevel() {
        PropertyLogLevel d1 = new PropertyLogLevel("DEBUG");
        d1.error();d1.fatal();
        d1.warn();d1.debug();
        d1.info();d1.trace();
    }
    
    @Test
    public void tesInitPropertyBigDecimal() {
        PropertyBigDecimal d1 = new PropertyBigDecimal("d1");
        PropertyBigDecimal d2 = new PropertyBigDecimal("d2", new BigDecimal("1"));
        PropertyBigDecimal d3 = new PropertyBigDecimal("d3", "2");
        Assert.assertNotNull(d1.getUid());
        Assert.assertNotNull(d2.getFixedValues());
        Assert.assertNotNull(d3.getUid());
        d1.fromString("1");
    }
    
    @Test
    public void tesInitNull() {
        PropertyBigDecimal bd = new PropertyBigDecimal("b1");
        bd.setValue(null);
        bd.asString();
        Assert.assertNotNull(bd.parameterizedType());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testPropertyString() {
        new PropertyString("p1", "v1", setOf("v0", "v2"));
    }
    
    @Test
    public void tesInitPropertyDate() {
        PropertyDate d1 = new PropertyDate("d1");
        d1.fromString("2015-01-02 13:00:00");
        Assert.assertNotNull(d1.getUid());
        PropertyDate d2 = new PropertyDate("d2", "2015-01-02 13:00:00");
        Assert.assertNotNull(d2.getUid());
        Date dd = null;
        PropertyDate d3 = new PropertyDate("d3", dd);
        Assert.assertNull(d3.asString());
    }
    
    @Test
    public void tesInitPropertyClass() {
        PropertyClass c0 = new PropertyClass("c0");
        c0.fromString(String.class.getName());
        new PropertyClass("c1");
        new PropertyClass("c2", String.class.getName());
        PropertyClass c3 = new PropertyClass("c3", String.class);
        Assert.assertEquals(String.class.getName(), c3.asString());
        
        Class<?> cc = null;
        PropertyClass c4 = new PropertyClass("c3", cc);
        Assert.assertNull(c4.asString());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void tesInitPropertyClass2() {
       new PropertyClass("c3", "existPas");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void tesInitPropertyDate2() {
        PropertyDate d0 = new PropertyDate("d0");
        d0.fromString("invalid");
    }
    
    @Test
    public void tesInitPropertyCalendar() {
        PropertyCalendar d0 = new PropertyCalendar("d0");
        PropertyCalendar d1 = new PropertyCalendar("d1");
        PropertyCalendar d2 = new PropertyCalendar("d2", "2015-01-02 13:00");
        PropertyCalendar d3 = new PropertyCalendar("d3", Calendar.getInstance());
        Assert.assertNotNull(d1.getUid());
        Assert.assertNotNull(d2.getUid());
        Assert.assertNotNull(d3.getUid());
        d3.asString();
        d0.fromString("2015-01-02 13:00");
        d0.setDescription("OK");
        d0.setType(PropertyCalendar.class.getName());
        Assert.assertNotNull(d0.toJson());
        
        Calendar cc = null;
        PropertyCalendar d4 = new PropertyCalendar("d4", cc);
        Assert.assertNull(d4.asString());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void tesInitPropertyCalendar2() {
        PropertyCalendar d0 = new PropertyCalendar("d0");
        d0.fromString("invalid");
    }
    
}

