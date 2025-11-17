package com.enterprise.my;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.internal.kernel.api.security.SecurityContext;
import org.neo4j.procedure.*;

import java.util.stream.Stream;

public class Counts {

    @Context
    public GraphDatabaseService db;

    @Context
    public Transaction tx;

    @Context
    public SecurityContext sc;

    @Procedure(name = "my.counts", mode = Mode.READ)
    @Description("counts(count_rels)")
    public Stream<Count> myCounts(@Name("databases") Boolean count_rels) {
        Long nodes = (Long) tx.execute("MATCH () return count(*) as count").next().get("count");
        return Stream.of(new Count(nodes));
    }



    public static class Count {

        public Count(Long nodes) {
            this.nodes = nodes;
        }
        public Long nodes;
    }

}