package com.enterprise.my;

import com.neo4j.fabric.auth.FabricAuthManagerWrapper;
import com.neo4j.fabric.auth.FabricAuthSubject;
import com.neo4j.fabric.driver.DriverPool;
import com.neo4j.fabric.driver.FabricDriverTransaction;
import com.neo4j.fabric.driver.PooledDriver;
import com.neo4j.fabric.shaded.driver.AuthToken;
import com.neo4j.fabric.shaded.driver.internal.security.InternalAuthToken;
import org.neo4j.fabric.FabricDatabaseManager;
import org.neo4j.fabric.executor.Location;
import org.neo4j.fabric.transaction.FabricTransaction;
import org.neo4j.fabric.transaction.FabricTransactionInfo;
import org.neo4j.fabric.transaction.TransactionManager;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.internal.kernel.api.security.AbstractSecurityLog;
import org.neo4j.internal.kernel.api.security.AuthSubject;
import org.neo4j.internal.kernel.api.security.LoginContext;
import org.neo4j.internal.kernel.api.security.SecurityContext;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.availability.UnavailableException;
import org.neo4j.kernel.database.NamedDatabaseId;
import org.neo4j.kernel.impl.coreapi.InternalTransaction;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Counts {

    @Context
    public GraphDatabaseService db;

    @Context
    public DbmsContext dbmsContext;

    @Context
    public Transaction tx;

    @Context
    public SecurityContext sc;

    @Procedure(name = "multidb.counts", mode = Mode.READ)
    @Description("counts([databases])")
    public Stream<Count> multidbCounts(@Name("databases") List<String> databases) {
        return databases.stream().map(database -> {
            try (Transaction tx = dbmsContext.databaseManagementService().database(database).beginTx()) {
                Long nodes = (Long) tx.execute("MATCH () return count(*) as count").next().get("count");
                return new Count(database, nodes);
            }
        });
    }

    @Procedure(name = "fabric.counts", mode = Mode.READ)
    @Description("counts([databases])")
    public Stream<Count> fabricCounts(@Name("databases") List<String> databases) throws Exception {

        if (!(sc.subject() instanceof FabricAuthSubject))  {
            throw new Exception("This procedure must be executed in a Fabric database");
        }

        TransactionManager transactionManager = ((GraphDatabaseFacade) db).getDependencyResolver().resolveDependency(TransactionManager.class);
        FabricDatabaseManager fabricDatabaseManager = ((GraphDatabaseFacade) db).getDependencyResolver().resolveDependency(FabricDatabaseManager.class);
        DriverPool driverPool = ((GraphDatabaseFacade) db).getDependencyResolver().resolveDependency(DriverPool.class);

        return databases.stream()
                .map(database -> {
                    Long nodes = 0L;
                    try {
                        GraphDatabaseFacade databaseFacade = fabricDatabaseManager.getDatabase(database);
                        // This does not work (no result)
                        // SecurityContext remoteDbSecurityContext = new SecurityContext(sc.subject(), sc.mode(), sc.connectionInfo(), database);
                        // InternalTransaction internalTransaction = databaseFacade.beginTransaction(KernelTransaction.Type.IMPLICIT, remoteDbSecurityContext);

                        // This does not work (no result)
                        //InternalTransaction internalTransaction = databaseFacade.beginTransaction(KernelTransaction.Type.IMPLICIT, sc);

                        // This works (both in unit test and deployed to db with basic auth)
                        InternalTransaction internalTransaction = databaseFacade.beginTransaction(KernelTransaction.Type.IMPLICIT, SecurityContext.AUTH_DISABLED);
                        String databaseName = internalTransaction.getDatabaseName();
                        nodes = (Long) internalTransaction.execute("MATCH () return count(*) as count").next().get("count");
                        internalTransaction.commit();
                    } catch (UnavailableException e) {
                        e.printStackTrace();
                    }
                    return new Count(database, nodes);
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