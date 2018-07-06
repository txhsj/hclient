/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hive.metastore.tools;

import org.apache.thrift.TException;
import picocli.CommandLine;

import java.net.URISyntaxException;

import static org.apache.hadoop.hive.metastore.tools.Constants.HMS_DEFAULT_PORT;

/**
 * Command-line access to Hive Metastore.
 */
@SuppressWarnings("squid:S106") // Using System.out
@CommandLine.Command(
        name = "MetastoreTool",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = {CommandLine.HelpCommand.class,
          MetastoreTool.DbCommand.class,
          MetastoreTool.TableCommand.class},
        showDefaultValues = true)
public class MetastoreTool implements Runnable {
  @CommandLine.Option(names = {"-H", "--host"},
      description = "HMS Host",
      paramLabel = "URI")
  private String host;
  @CommandLine.Option(names = {"-P", "--port"}, description = "HMS Server port")
  private Integer port = HMS_DEFAULT_PORT;

  public static void main(String[] args) {
    CommandLine.run(new MetastoreTool(), args);
  }

  @Override
  public void run() {
    try (HMSClient client = new HMSClient(Util.getServerUri(host, String.valueOf(port)))) {
      TableCommand.printTableList(client, null, null);
    } catch (URISyntaxException e) {
      System.out.println("invalid host " + host);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @CommandLine.Command(name = "db",
      subcommands = {CommandLine.HelpCommand.class, DbCommand.DbListCommand.class})
  static class DbCommand implements Runnable {

    @CommandLine.ParentCommand
    private MetastoreTool parent;

    @CommandLine.Option(names = {"-d", "--db"}, description = "Database name pattern")
    private String dbName;

    static void PrintDbList(HMSClient client, String dbName) throws TException {
      client.getAllDatabases(dbName).forEach(System.out::println);
    }

    @Override
    public void run() {
      String host = parent.host;
      int port = parent.port;
      try (HMSClient client = new HMSClient(Util.getServerUri(host, String.valueOf(port)))) {
        PrintDbList(client, dbName);
      } catch (URISyntaxException e) {
        System.out.println("invalid host " + host);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    /**
     * List databases.
     */
    @CommandLine.Command(name = "list",
      description = "List all databases, optionally matching pattern",
      subcommands = {CommandLine.HelpCommand.class})
    static class DbListCommand implements Runnable {

      @CommandLine.ParentCommand
      private DbCommand parent;

      @Override
      public void run() {
        String host = parent.parent.host;
        int port = parent.parent.port;
        String dbName = parent.dbName;
        try (HMSClient client = new HMSClient(Util.getServerUri(host, String.valueOf(port)))) {
          PrintDbList(client, dbName);
        } catch (URISyntaxException e) {
          System.out.println("invalid host " + host);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  @CommandLine.Command(name = "table",
      subcommands = {CommandLine.HelpCommand.class, TableCommand.TablePrintCommand.class},
      description = "HMS Table operations")
  static class TableCommand implements Runnable {

    @CommandLine.ParentCommand
    private MetastoreTool parent;

    @CommandLine.Option(names = {"-d", "--db"}, description = "Database name pattern")
    private String dbName;

    static void printTableList(HMSClient client, String dbName, String tableName) throws TException {
      for (String database : client.getAllDatabases(dbName)) {
        client.getAllTables(database, tableName).stream()
            .sorted()
            .map(t -> database + "." + t)
            .forEach(System.out::println);
      }
    }

    /**
     * List all tables
     */
    @CommandLine.Command(name = "list", description = "List all tables")
    static class TablePrintCommand implements Runnable {

      @CommandLine.ParentCommand
      private TableCommand parent;

      @CommandLine.Option(names = {"-t", "--table"}, description = "Table name pattern")
      private String tableName;

      @Override
      public void run() {
        String host = parent.parent.host;
        String dbName = parent.dbName;
        int port = parent.parent.port;
        try (HMSClient client = new HMSClient(Util.getServerUri(host, String.valueOf(port)))) {
          printTableList(client, dbName, tableName);
        } catch (URISyntaxException e) {
          System.out.println("invalid host " + host);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    @Override
    public void run() {
    }
  }
}
