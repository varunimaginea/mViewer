/*
 * Copyright (c) 2011 Imaginea Technologies Private Ltd.
 * Hyderabad, India
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.imaginea.mongodb.services.impl;

import com.imaginea.mongodb.services.CollectionService;
import com.imaginea.mongodb.utils.MongoInstanceProvider;
import com.imaginea.mongodb.utils.SessionMongoInstanceProvider;
import com.imaginea.mongodb.exceptions.CollectionException;
import com.imaginea.mongodb.exceptions.DatabaseException;
import com.imaginea.mongodb.exceptions.ErrorCodes;
import com.imaginea.mongodb.exceptions.ValidationException;
import com.mongodb.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Defines services definitions for performing operations like create/drop on
 * collections inside a database present in mongo to which we are connected to.
 * Also provides service to get list of all collections present and Statistics
 * of a particular collection.
 *
* @author Srinath Anantha
 */
public class CollectionServiceImpl implements CollectionService {

    /**
     * Instance variable used to get a mongo instance after binding to an
     * implementation.
     */
    private MongoInstanceProvider mongoInstanceProvider;
    /**
     * Mongo Instance to communicate with mongo
     */
    private Mongo mongoInstance;

    /**
     * Creates an instance of MongoInstanceProvider which is used to get a mongo
     * instance to perform operations on collections. The instance is created
     * based on a userMappingKey which is recieved from the collection request
     * dispatcher and is obtained from tokenId of user.
     *
     * @param dbInfo A combination of username,mongoHost and mongoPort
     */
    public CollectionServiceImpl(String dbInfo) {
        mongoInstanceProvider = new SessionMongoInstanceProvider(dbInfo);
    }

    /**
     * Gets the list of collections present in a database in mongo to which user
     * is connected to.
     *
     * @param dbName Name of database
     * @return List of All Collections present in MongoDb
     * @throws DatabaseException   throw super type of UndefinedDatabaseException
     * @throws ValidationException throw super type of EmptyDatabaseNameException
     * @throws CollectionException exception while performing get list operation on
     *                             collection
     */
    public Set<String> getCollList(String dbName) throws ValidationException, DatabaseException, CollectionException {

        mongoInstance = mongoInstanceProvider.getMongoInstance();

        if (dbName == null) {
            throw new DatabaseException(ErrorCodes.DB_NAME_EMPTY, "Database Name Is Null");
        }
        if (dbName.equals("")) {
            throw new DatabaseException(ErrorCodes.DB_NAME_EMPTY, "Database Name Empty");
        }

        Set<String> collList = new HashSet<String>();

        try {
            if (!mongoInstance.getDatabaseNames().contains(dbName)) {
                throw new DatabaseException(ErrorCodes.DB_DOES_NOT_EXISTS, "Database with dbName [ " + dbName + "] does not exist");
            }

            Set<String> collectionList = new HashSet<String>();
            collectionList = mongoInstance.getDB(dbName).getCollectionNames();
            Iterator<String> it = collectionList.iterator();

            while (it.hasNext()) {
                String coll = it.next();
                if (!coll.contains("system.")) {
                    collList.add(coll);
                }
            }

        } catch (MongoException m) {
            throw new CollectionException(ErrorCodes.GET_COLLECTION_LIST_EXCEPTION, m.getMessage());
        }
        return collList;

    }

    /**
     * Creates a collection inside a database in mongo to which user is
     * connected to.
     *
     * @param dbName         Name of Database in which to insert a collection
     * @param collectionName Name of Collection to be inserted
     * @param capped         Specify if the collection is capped
     * @param size           Specify the size of collection
     * @param maxDocs        specify maximum no of documents in the collection
     * @return Success if Insertion is successful else throw exception
     * @throws DatabaseException   throw super type of UndefinedDatabaseException
     * @throws ValidationException throw super type of
     *                             EmptyDatabaseNameException,EmptyCollectionNameException
     * @throws CollectionException throw super type of
     *                             DuplicateCollectionException,InsertCollectionException
     */
    public String insertCollection(String dbName, String collectionName, boolean capped, int size, int maxDocs) throws DatabaseException, CollectionException, ValidationException {

        mongoInstance = mongoInstanceProvider.getMongoInstance();
        if (dbName == null) {
            throw new DatabaseException(ErrorCodes.DB_NAME_EMPTY, "Database name is null");

        }
        if (dbName.equals("")) {
            throw new DatabaseException(ErrorCodes.DB_NAME_EMPTY, "Database Name Empty");
        }

        if (collectionName == null) {
            throw new CollectionException(ErrorCodes.COLLECTION_NAME_EMPTY, "Collection name is null");
        }
        if (collectionName.equals("")) {
            throw new CollectionException(ErrorCodes.COLLECTION_NAME_EMPTY, "Collection Name Empty");
        }
        try {
            if (!mongoInstance.getDatabaseNames().contains(dbName)) {
                throw new DatabaseException(ErrorCodes.DB_DOES_NOT_EXISTS, "Db with name [" + dbName + "] doesn't exist.");
            }
            if (mongoInstance.getDB(dbName).getCollectionNames().contains(collectionName)) {
                throw new CollectionException(ErrorCodes.COLLECTION_ALREADY_EXISTS, "Collection [" + collectionName + "] Already exists in Database [" + dbName + "]");
            }

            DBObject options = new BasicDBObject();
            options.put("capped", capped);
            if (capped) {
                options.put("size", size);
                options.put("max", maxDocs);
            }
            mongoInstance.getDB(dbName).createCollection(collectionName, options);
        } catch (MongoException m) {
            throw new CollectionException(ErrorCodes.COLLECTION_CREATION_EXCEPTION, m.getMessage());
        }
        String result = "Collection [" + collectionName + "] added to Database [" + dbName + "].";
        return result;
    }

    /**
     * Deletes a collection inside a database in mongo to which user is
     * connected to.
     *
     * @param dbName         Name of Database in which to insert a collection
     * @param collectionName Name of Collection to be inserted
     * @return Success if deletion is successful else throw exception
     * @throws DatabaseException   throw super type of UndefinedDatabaseException
     * @throws ValidationException throw super type of
     *                             EmptyDatabaseNameException,EmptyCollectionNameException
     * @throws CollectionException throw super type of
     *                             UndefinedCollectionException,DeleteCollectionException
     */

    public String deleteCollection(String dbName, String collectionName) throws DatabaseException, CollectionException, ValidationException {

        mongoInstance = mongoInstanceProvider.getMongoInstance();
        if (dbName == null) {
            throw new DatabaseException(ErrorCodes.DB_NAME_EMPTY, "Database name is null");

        }
        if (dbName.equals("")) {
            throw new DatabaseException(ErrorCodes.DB_NAME_EMPTY, "Database Name Empty");
        }

        if (collectionName == null) {
            throw new CollectionException(ErrorCodes.COLLECTION_NAME_EMPTY, "Collection name is null");
        }
        if (collectionName.equals("")) {
            throw new CollectionException(ErrorCodes.COLLECTION_NAME_EMPTY, "Collection Name Empty");
        }
        try {
            if (!mongoInstance.getDatabaseNames().contains(dbName)) {
                throw new DatabaseException(ErrorCodes.DB_DOES_NOT_EXISTS, "DB with name [" + dbName + "]DOES_NOT_EXIST");
            }
            if (!mongoInstance.getDB(dbName).getCollectionNames().contains(collectionName)) {
                throw new CollectionException(ErrorCodes.COLLECTION_DOES_NOT_EXIST, "Collection with name [" + collectionName + "] DOES NOT EXIST in Database [" + dbName + "]");
            }
            mongoInstance.getDB(dbName).getCollection(collectionName).drop();
        } catch (MongoException m) {
            throw new CollectionException(ErrorCodes.COLLECTION_DELETION_EXCEPTION, m.getMessage());
        }
        String result = "Collection [" + collectionName + "] has been deleted from Database [" + dbName + "].";

        return result;
    }

    /**
     * Get Statistics of a collection inside a database in mongo to which user
     * is connected to.
     *
     * @param dbName         Name of Database in which to insert a collection
     * @param collectionName Name of Collection to be inserted
     * @return Array of JSON Objects each containing a key value pair in
     *         Collection Stats.
     * @throws DatabaseException   throw super type of UndefinedDatabaseException
     * @throws ValidationException throw super type of
     *                             EmptyDatabaseNameException,EmptyCollectionNameException
     * @throws CollectionException throw super type of UndefinedCollectionException
     * @throws JSONException       JSON Exception
     */

    public JSONArray getCollStats(String dbName, String collectionName) throws DatabaseException, CollectionException, ValidationException, JSONException {
        mongoInstance = mongoInstanceProvider.getMongoInstance();
        if (dbName == null) {
            throw new DatabaseException(ErrorCodes.DB_NAME_EMPTY, "Database name is null");

        }
        if (dbName.equals("")) {
            throw new DatabaseException(ErrorCodes.DB_NAME_EMPTY, "Database Name Empty");
        }

        if (collectionName == null) {
            throw new CollectionException(ErrorCodes.COLLECTION_NAME_EMPTY, "Collection name is null");
        }
        if (collectionName.equals("")) {
            throw new CollectionException(ErrorCodes.COLLECTION_NAME_EMPTY, "Collection Name Empty");
        }

        JSONArray collStats = new JSONArray();

        try {
            if (!mongoInstance.getDatabaseNames().contains(dbName)) {
                throw new DatabaseException(ErrorCodes.DB_DOES_NOT_EXISTS, "DB with name [" + dbName + "]DOES_NOT_EXIST");
            }
            if (!mongoInstance.getDB(dbName).getCollectionNames().contains(collectionName)) {
                throw new CollectionException(ErrorCodes.COLLECTION_DOES_NOT_EXIST,
                    "Collection with name [" + collectionName + "] DOES NOT EXIST in Database [" + dbName + "]");
            }
            CommandResult stats = mongoInstance.getDB(dbName).getCollection(collectionName).getStats();

            Set<String> keys = stats.keySet();
            Iterator<String> keyIterator = keys.iterator();
            JSONObject temp = new JSONObject();

            while (keyIterator.hasNext()) {
                temp = new JSONObject();
                String key = keyIterator.next().toString();
                temp.put("Key", key);
                String value = stats.get(key).toString();
                temp.put("Value", value);
                String type = stats.get(key).getClass().toString();
                temp.put("Type", type.substring(type.lastIndexOf('.') + 1));
                collStats.put(temp);
            }
        } catch (JSONException e) {
            throw e;
        } catch (MongoException m) {
            throw new CollectionException(ErrorCodes.GET_COLL_STATS_EXCEPTION, m.getMessage());
        }
        return collStats;
    }
}
