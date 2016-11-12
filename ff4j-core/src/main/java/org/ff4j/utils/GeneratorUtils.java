package org.ff4j.utils;

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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.ff4j.FF4j;

/**
 * Generation of Java Interface.
 *
 * @author Cedrick LUNVEN (@clunven)
 */
public class GeneratorUtils {

    /** XML Generation constants. */
    private static final String ENCODING = "UTF-8";
    
    /**
     * Hide constructor.
     */
    private GeneratorUtils() {
    }
    
    /**
     * Generate target Java Interface.
     *
     * @param ff4j
     *      current ff4J bean
     * @return 
     *      data as inputstream
     * @throws IOException
     *             error occu
     */
    public static String generateInterfaceConstantsSource(FF4j ff4j) {
        Util.assertNotNull(ff4j);
        StringBuilder sb = new StringBuilder();
        sb.append("/**\r\n * Constants for ff4j features and properties.");
        sb.append("\r\n * Generated on : " + new SimpleDateFormat("yyyy-MM-DD HH:mm").format(new Date()));
        sb.append("\r\n *");
        sb.append("\r\n * @author FF4J Generator Engine");
        sb.append("\r\n */");
        sb.append("\r\npublic interface FF4jConstants {");
        sb.append("\r\n");
        sb.append("\r\n   // -------------------------");
        sb.append("\r\n   //  Features ");
        sb.append("\r\n   // -------------------------");
        sb.append("\r\n");
        ff4j.getFeatureStore().findAll().forEach(f -> {
            sb.append("\r\n   /* Feature '" + f.getUid() + "' : '" + f.getDescription().orElse("") + "' */");
            sb.append("\r\n   String FEATURE_" + f.getUid().replaceAll(" ", "_").toUpperCase() + " = \"" + f.getUid() + "\";");
            sb.append("\r\n");
        });
        sb.append("\r\n   // -------------------------");
        sb.append("\r\n   //  Groups ");
        sb.append("\r\n   // -------------------------");
        sb.append("\r\n");
        ff4j.getFeatureStore().readAllGroups().forEach(g -> {
            sb.append("\r\n   /* Group '" + g + "' */");
            sb.append("\r\n   String FEATURE_GROUP_" + g.replaceAll(" ", "_").toUpperCase() + " = \"" + g + "\";");
            sb.append("\r\n");
        });
        sb.append("\r\n   // -------------------------");
        sb.append("\r\n   //  Properties ");
        sb.append("\r\n   // -------------------------");
        sb.append("\r\n");
        ff4j.getPropertiesStore().findAll().forEach(p -> {
            sb.append("\r\n   /* Property '" + p.getUid() + "' : '" + p.getDescription().orElse("") + "' */");
            sb.append("\r\n   String PROPERTY_" + p.getUid().replaceAll(" ", "_").toUpperCase() + " = \"" + p.getUid() + "\";");
            sb.append("\r\n");
        });
        sb.append("\r\n}");
        return sb.toString();      
    }
    
    public static InputStream exportInterfaceConstants(FF4j ff4j)  throws IOException {   
       return new ByteArrayInputStream(generateInterfaceConstantsSource(ff4j).getBytes(ENCODING));
    }
    
    public static void generateInterfaceConstantFile(FF4j ff4j, File folder) throws IOException {
        Util.assertNotNull(folder);
        File outFile = new File(folder.getAbsolutePath() + File.separator + "FF4jConstants.java");
        FileWriter out = new FileWriter(outFile);
        out.write(generateInterfaceConstantsSource(ff4j));
        out.close();
    }

}
