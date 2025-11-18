package com.enterprise.my;

import com.neo4j.harness.EnterpriseNeo4jBuilders;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.*;
import org.neo4j.harness.Neo4j;

import static org.assertj.core.api.Assertions.assertThat;

class CountsTest {

    private static final Config driverConfig = Config.builder().withoutEncryption().build();
    private static Driver driver;
    private static Neo4j embeddedDatabaseServer;

    @BeforeAll
    static void initializeNeo4j() {
        embeddedDatabaseServer = EnterpriseNeo4jBuilders.newInProcessBuilder()
                .withDisabledServer()
                .withProcedure(Counts.class)
                .build();

        driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI(), driverConfig);

        try (Session session = driver.session(SessionConfig.forDatabase("system"))) {
            session.executeWriteWithoutResult(tx -> {
                tx.run("create database db1");
            });
        }
        try (Session session = driver.session(SessionConfig.forDatabase("db1"))) {
            session.executeWriteWithoutResult(tx -> {
                tx.run("create ()");
            });
        }
    }

    @AfterAll
    static void closeDriver(){
        driver.close();
        embeddedDatabaseServer.close();
    }

    @Test
    void mySillyCounts() {
        try (Session session = driver.session(SessionConfig.forDatabase("db1"))) {
            Result result = session.run("CALL my.counts(false) yield nodes return nodes");
            assertThat(result.stream())
                    .hasSize(1)
                    .extracting(r -> {
                        Long nodes = (Long) r.get("nodes").asLong();
                        return String.format("%s", nodes);
                    })
                    .containsExactlyInAnyOrder("1");;
        }
    }

}