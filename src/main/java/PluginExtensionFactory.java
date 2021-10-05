import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.kernel.api.procedure.GlobalProcedures;
import org.neo4j.kernel.extension.ExtensionFactory;
import org.neo4j.kernel.extension.ExtensionType;
import org.neo4j.kernel.extension.context.ExtensionContext;
import org.neo4j.kernel.lifecycle.Lifecycle;

public class PluginExtensionFactory extends ExtensionFactory<PluginExtensionFactory.Dependencies> {


    protected PluginExtensionFactory() {
        super(ExtensionType.GLOBAL, "PluginExtensionFactory");
    }

    public interface Dependencies {
        GlobalProcedures globalProceduresRegistry();
        DatabaseManagementService databaseManagementService();
    }

    @Override
    public Lifecycle newInstance(ExtensionContext extensionContext, Dependencies dependencies) {
        return new DbmsContext(dependencies.globalProceduresRegistry(), dependencies.databaseManagementService());
    }
}
