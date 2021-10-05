import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class Counts {

    @Context
    public GraphDatabaseService db;

    @Context
    public DbmsContext dbmsContext;


    @Procedure(name = "multidb.counts", mode = Mode.READ)
    @Description("counts([databases])")
    public Stream<Count> counts(@Name("databases") List<String> databases) {
        return databases.stream().map(database -> {
            try (Transaction tx = dbmsContext.databaseManagementService().database(database).beginTx()) {
                Long nodes = (Long) tx.execute("MATCH () return count(*) as count").next().get("count");
                return new Count(database, nodes);
            }
        });
    }


    public static class Count {

        public Count(String database, Long nodes) {
            this.database = database;
            this.nodes = nodes;
        }

        public String database;
        public Long nodes;
    }

}