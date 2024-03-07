package client.apple;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(baseUri = "https://appleid.apple.com")
public interface RenardeAppleClient {

    @POST
    @Path("/auth/revoke")
    @Produces(MediaType.APPLICATION_FORM_URLENCODED)
    void revokeAppleUser(@FormParam("client_id") @ConfigProperty(name = "quarkus.oidc.apple.client-id") String clientID,
                             @FormParam("client_secret") String clientSecret,
                             @FormParam("token") String token,
                             @FormParam("token_type_hint") String tokenTypeHint
                                );

}
