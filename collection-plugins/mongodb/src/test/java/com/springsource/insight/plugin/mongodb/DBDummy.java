/**
 * Copyright 2009-2011 the original author or authors.
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

package com.springsource.insight.plugin.mongodb;

import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;

/**
 */
public class DBDummy extends DB {

    public DBDummy(Mongo mongo, String name) {
        super(mongo, name);
    }

    @Override
	public CommandResult command(com.mongodb.DBObject cmd)
            throws com.mongodb.MongoException {
        return null;
    }

    @Override
	public CommandResult command(com.mongodb.DBObject cmd, int options)
            throws com.mongodb.MongoException {
        return null;
    }

    @Override
	public CommandResult command(java.lang.String cmd)
            throws com.mongodb.MongoException {
        return null;
    }


    @Override
    public void requestStart() {
    	// do nothing
    }

    @Override
    public void requestDone() {
    	// do nothing
    }

    @Override
    public void requestEnsureConnection() {
    	// do nothing
    }

    @Override
    protected DBCollection doGetCollection(String s) {
        return null;
    }
}
