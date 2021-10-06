package com.enterprise.my;


import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.kernel.api.procedure.GlobalProcedures;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;



public class DbmsContext extends LifecycleAdapter {
    private DatabaseManagementService databaseManagementService = null;

    public DbmsContext(final GlobalProcedures globalProceduresRegistry, final DatabaseManagementService databaseManagementService){
        this.databaseManagementService = databaseManagementService;
        globalProceduresRegistry.registerComponent((Class<DbmsContext>) getClass(), ctx -> this, true);
    }

    public DatabaseManagementService databaseManagementService() {
        return databaseManagementService;
    }

    @Override
    public void init() {

    }

    @Override
    public void shutdown() {

    }
}
