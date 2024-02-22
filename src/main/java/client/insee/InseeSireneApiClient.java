package client.insee;

import client.insee.dto.ReponseEtablissement;
import client.insee.dto.ReponseUnitesLegales;
import io.quarkus.logging.Log;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
public interface InseeSireneApiClient {

    @Path("/siren/{siren}")
    @GET
    ReponseUnitesLegales getBySiren(@HeaderParam("Authorization") String authorization, String siren);


    @Path("/siret/{siret}")
    @GET
    ReponseEtablissement getBySiret(@HeaderParam("Authorization") String authorization, String siret);


    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        if (response.getStatus() == 200){
            // never happen I guess
            Log.info("200 response from INSEE Sirene");
        } else if (response.getStatus() == 500){
            Log.error("500 response from INSEE Sirene - they might be down, it happen");
            return new RuntimeException("Failed to call INSEE");
        } else {
            String s = response.readEntity(String.class);
            Log.error("BAD REQUEST:\n"  + s);
        }
        return null;
    }
}
