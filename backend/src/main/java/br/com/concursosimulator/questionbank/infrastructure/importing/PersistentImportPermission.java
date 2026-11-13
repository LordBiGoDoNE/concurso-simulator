package br.com.concursosimulator.questionbank.infrastructure.importing;

import br.com.concursosimulator.identity.application.AdministrativeAccessUseCases;
import br.com.concursosimulator.identity.application.port.AdministrativeCapabilities;
import br.com.concursosimulator.questionbank.application.port.AdministrativeImportPermission;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PersistentImportPermission implements AdministrativeImportPermission,AdministrativeCapabilities {
    private final AdministrativeAccessUseCases access;private final ImportSettings settings;
    public PersistentImportPermission(AdministrativeAccessUseCases access,ImportSettings settings){this.access=access;this.settings=settings;}
    public boolean permits(UUID actor,long version) {
        try{access.current(actor,version,false);return true;}
        catch(AdministrativeAccessUseCases.Failure denied){return false;}
    }
    public boolean questionImportAvailable(){return settings.configured();}
}
