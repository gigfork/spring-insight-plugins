/**
 * Copyright (c) 2009-2011 VMware, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.springsource.insight.plugin.jdbc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.springsource.insight.intercept.operation.Operation;
import com.springsource.insight.intercept.operation.OperationType;
import com.springsource.insight.intercept.topology.ExternalResourceDescriptor;
import com.springsource.insight.intercept.topology.ExternalResourceType;
import com.springsource.insight.intercept.topology.MD5NameGenerator;
import com.springsource.insight.intercept.trace.Frame;
import com.springsource.insight.intercept.trace.Trace;
import com.springsource.insight.util.ListUtil;
import com.springsource.insight.util.StringUtil;

/**
 * 
 */
public class JdbcOperationExternalResourceAnalyzer extends DatabaseJDBCURIAnalyzer {
    public static final OperationType   TYPE=OperationType.valueOf("jdbc");
    private static final JdbcOperationExternalResourceAnalyzer	INSTANCE=new JdbcOperationExternalResourceAnalyzer();

    private JdbcOperationExternalResourceAnalyzer() {
        super(TYPE);
    }

    public static final JdbcOperationExternalResourceAnalyzer getInstance() {
    	return INSTANCE;
    }
    
    @Override
    public Collection<ExternalResourceDescriptor> locateExternalResourceName(Trace trace, Collection<Frame> dbFrames) {
    	return createAndAddQueryExternalResourceDescriptors(super.locateExternalResourceName(trace, dbFrames));
    }
    
    static final Collection<ExternalResourceDescriptor> createAndAddQueryExternalResourceDescriptors(Collection<ExternalResourceDescriptor> dbDescriptors) {
    	
    	if (!shouldGenerateQueryExternalResources(dbDescriptors)) {
    		return dbDescriptors;
    	}
    	
    	Collection<ExternalResourceDescriptor> newCollection = new ArrayList<ExternalResourceDescriptor>(dbDescriptors);
    	
    	for(ExternalResourceDescriptor dbDescriptor : dbDescriptors) {
    		Frame frame = dbDescriptor.getFrame();
    		
    		if (frame == null) {
    			continue;
    		}
    		
    		Operation op = frame.getOperation();
    		
    		if (op == null) {
    			continue;
    		}
    		
    		String sql = op.get("sql", String.class);
    		
    		if (!StringUtil.isEmpty(sql)) {
    			String jdbcHash = MD5NameGenerator.getName(sql);
                
    			ExternalResourceDescriptor queryDescriptor=new ExternalResourceDescriptor(frame, 
    																				 dbDescriptor.getName() + ":" + jdbcHash,
    																				 sql,
    																				 ExternalResourceType.QUERY.name(),
    																				 dbDescriptor.getVendor(),
    																				 dbDescriptor.getHost(),
    																				 dbDescriptor.getPort(),
    																				 dbDescriptor.getColor(), 
    																				 dbDescriptor.isIncoming(),
    																				 dbDescriptor);
    			dbDescriptor.setChildren(Collections.singletonList(queryDescriptor));
    			newCollection.add(queryDescriptor);
    		}
    		
    	}
    	
    	return newCollection;
    }
    
    static final boolean shouldGenerateQueryExternalResources(Collection<ExternalResourceDescriptor> dbDescriptors) {
    	return ListUtil.size(dbDescriptors) > 0;
     }
}
