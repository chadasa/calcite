/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.adapter.geode.rel;

import org.apache.calcite.adapter.geode.util.GeodeUtils;

import org.apache.geode.cache.GemFireCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Runner class to run examples of RelationalJdbcExample.java
 * This class is created to add unit tests
 */
public class RelationalJdbcExampleRunner {

  public RelationalJdbcExampleRunner() {

  }

  private static final Logger LOGGER = LoggerFactory.getLogger(
      RelationalJdbcExampleRunner.class.getName());

  public Connection getCalciteConnection(Properties info) throws Exception {
    return DriverManager.getConnection("jdbc:calcite:", info);
  }

  public void closeAll(List<AutoCloseable> closeableList) throws Exception {
    GeodeUtils.closeClientCache();
    for (AutoCloseable closeable : closeableList) {
      closeable.close();
    }
  }

  public Context getContext(Hashtable env) throws NamingException {
    return new InitialContext(env);
  }

  public GemFireCache createClientCache() {
    return GeodeUtils.createClientCache("localhost", 10334,
        "org.apache.calcite.adapter.geode.domain.*", true);
  }

  private void runExampleForGivenGeodeModelJson(String geodeModelJson) throws Exception {
    Class.forName("org.apache.calcite.jdbc.Driver");

    Properties info = new Properties();
    info.put("model", geodeModelJson);

    Connection connection = getCalciteConnection(info);

    Statement statement = connection.createStatement();
    ResultSet resultSet = statement.executeQuery(
        "SELECT \"b\".\"author\", \"b\".\"retailCost\", \"i\".\"quantityInStock\"\n"
            + "FROM \"TEST\".\"BookMaster\" AS \"b\" "
            + " INNER JOIN \"TEST\".\"BookInventory\" AS \"i\""
            + "  ON \"b\".\"itemNumber\" = \"i\".\"itemNumber\"\n "
            + "WHERE  \"b\".\"retailCost\" > 0");

    final StringBuilder buf = new StringBuilder();
    while (resultSet.next()) {
      ResultSetMetaData metaData = resultSet.getMetaData();
      for (int i = 1; i <= metaData.getColumnCount(); i++) {
        buf.append(i > 1 ? "; " : "")
            .append(metaData.getColumnLabel(i)).append("=").append(resultSet.getObject(i));
      }
      LOGGER.info("Result entry: " + buf.toString());
      buf.setLength(0);
    }

    closeAll(Arrays.asList(resultSet, statement, connection));
  }

  private void runExampleWithLocatorHostPort() throws Exception {
    final String geodeModelJson =
        "inline:"
            + "{\n"
            + "  version: '1.0',\n"
            + "  schemas: [\n"
            + "     {\n"
            + "       type: 'custom',\n"
            + "       name: 'TEST',\n"
            + "       factory: 'org.apache.calcite.adapter.geode.rel.GeodeSchemaFactory',\n"
            + "       operand: {\n"
            + "         locatorHost: 'localhost', \n"
            + "         locatorPort: '10334', \n"
            + "         regions: 'BookMaster,BookCustomer,BookInventory,BookOrder', \n"
            + "         pdxSerializablePackagePath: 'org.apache.calcite.adapter.geode.domain.*' \n"
            + "       }\n"
            + "     }\n"
            + "   ]\n"
            + "}";

    runExampleForGivenGeodeModelJson(geodeModelJson);
  }

  private void runExampleWithJndiClientCacheObject() throws Exception {
    String jndiInitialContextFactory = "org.apache.geode.internal.jndi.InitialContextFactoryImpl";
    Hashtable env = new Hashtable();
    env.put(Context.INITIAL_CONTEXT_FACTORY, jndiInitialContextFactory);
    Context context = getContext(env);
    GemFireCache gemFireCache = createClientCache();

    context.bind("testClientCacheObject", gemFireCache);
    final String geodeModelJson =
        "inline:"
            + "{\n"
            + "  version: '1.0',\n"
            + "  schemas: [\n"
            + "     {\n"
            + "       type: 'custom',\n"
            + "       name: 'TEST',\n"
            + "       factory: 'org.apache.calcite.adapter.geode.rel.GeodeSchemaFactory',\n"
            + "       operand: {\n"
            + "         jndiInitialContextFactory: 'org.apache.geode.internal.jndi.InitialContextFactoryImpl',\n"
            + "         jndiClientCacheObjectKey: 'testClientCacheObject',\n"
            + "         regions: 'BookMaster,BookCustomer,BookInventory,BookOrder'\n"
            + "       }\n"
            + "     }\n"
            + "   ]\n"
            + "}";

    runExampleForGivenGeodeModelJson(geodeModelJson);
  }

  public void doExample(int i) throws Exception {
    switch (i) {
    case 0:
      runExampleWithLocatorHostPort();
      break;
    case 1:
      runExampleWithJndiClientCacheObject();
      break;
    default:
      throw new AssertionError("unknown example " + i);
    }
  }
}

// End RelationalJdbcExampleRunner.java
