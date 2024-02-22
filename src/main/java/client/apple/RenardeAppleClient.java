package client.apple;

import io.quarkus.logging.Log;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(baseUri = "https://appleid.apple.com")
public interface RenardeAppleClient {


    @POST
    @Path("/auth/revoke")
    @Produces(MediaType.APPLICATION_FORM_URLENCODED)
    void revokeAppleUser(@FormParam("client_id") String clientID,
                             @FormParam("client_secret") String clientSecret,
                             @FormParam("token") String token,
                             @FormParam("token_type_hint") String tokenTypeHint
                                );



    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        if (response.getStatus() == 400) {
            Log.error("BAD REQUEST !! body below :");
            Log.error(response.readEntity(String.class));
            //return new RuntimeException("The remote service responded with HTTP 400");
        }
        return null;
    }
}
