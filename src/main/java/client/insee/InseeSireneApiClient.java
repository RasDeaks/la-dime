package client.insee;

import client.insee.dto.BaseInseeResponse;
import client.insee.dto.ReponseEtablissement;
import client.insee.dto.ReponseUnitesLegales;
import error.DimeWsException;
import io.quarkus.logging.Log;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ProcessingException;
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
        // ClientExceptionMapper: only called for 400 and higher responses
        // map to custom Exception
        if (response.getStatus() == 500){
            Log.error("INSEE Sirene - SERVER ERROR, they might be down, it happen");
            return new DimeWsException(500, "INSEE server error, try again later");
        } else {
            try {
                BaseInseeResponse s = response.readEntity(BaseInseeResponse.class);
                Log.warn("INSEE Sirene - BAD REQUEST:"  + s.header.message);
                return new DimeWsException(response.getStatus(), String.format("INSEE client error : %s", s.header.message));
            } catch (ProcessingException e) {
                return new DimeWsException(response.getStatus(), "INSEE client error : UNKNOWN");
            }
        }
    }
}
