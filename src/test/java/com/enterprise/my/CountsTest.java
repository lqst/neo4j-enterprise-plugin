package com.enterprise.my;

import com.neo4j.harness.EnterpriseNeo4jBuilders;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.*;
import org.neo4j.harness.Neo4j;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class CountsTest {

    private static final Config driverConfig = Config.builder().withoutEncryption().build();
    private static Driver driver;
    private static Neo4j embeddedDatabaseServer;

    @BeforeAll
    static void initializeNeo4j() {
        embeddedDatabaseServer = EnterpriseNeo4jBuilders.newInProcessBuilder()
                .withDisabledServer()
                .withExtensionFactories(Collections.singletonList(new PluginExtensionFactory()))
                .withProcedure(Counts.class)
                .build();

        driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI(), driverConfig);

        try (Session session = driver.session(SessionConfig.forDatabase("system"))) {
            session.writeTransaction(tx -> {
                tx.run("create database db1");
                tx.run("create database db2");
                return true;
            });
        }
        try (Session session = driver.session(SessionConfig.forDatabase("db1"))) {
            session.writeTransaction(tx -> {
                tx.run("create ()");
                return true;
            });
        }

        try (Session session = driver.session(SessionConfig.forDatabase("db2"))) {
            session.writeTransaction(tx -> {
                tx.run("create ()");
                tx.run("create ()");
                return true;
            });
        }

    }

    @AfterAll
    static void closeDriver(){
        driver.close();
        embeddedDatabaseServer.close();
    }

    @Test
    void counts() {
        try (Session session = driver.session(SessionConfig.forDatabase("db1"))) {
            Result result = session.run("CALL multidb.counts(['db1','db2']) yield database, nodes return database, nodes");
            assertThat(result.stream())
                    .hasSize(2)
                    .extracting(r -> {
                        String database = r.get("database").asString();
                        Long nodes = r.get("nodes").asLong();
                        return String.format("%s:%s", database, nodes);
                    })
                    .containsExactlyInAnyOrder("db1:1", "db2:2");;
        }


    }
}