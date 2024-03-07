package rest;

import io.quarkiverse.renarde.router.Router;
import io.quarkiverse.renarde.security.ControllerWithUser;
import io.quarkiverse.renarde.security.RenardeSecurity;
import io.quarkus.logging.Log;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdToken;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import model.User;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.RestForm;
import client.apple.RenardeAppleClient;
import util.DixUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Authenticated
@Blocking
public class Users extends ControllerWithUser<User> {

    @Inject
    RenardeSecurity security;

    @Inject
    @IdToken
    JsonWebToken jwt;

    @Inject
    AccessTokenCredential accessToken;

    @ConfigProperty(name = "quarkus.oidc.apple.client-id")
    String appleClientId;

    @ConfigProperty(name = "quarkus.oidc.apple.credentials.jwt.issuer")
    String appleOidcIssuer;

    @ConfigProperty(name = "quarkus.oidc.apple.credentials.jwt.token-key-id")
    String appleOidcKeyId;

    @ConfigProperty(name = "quarkus.oidc.apple.credentials.jwt.key-file")
    String appleKeyFile;


    @RestClient
    RenardeAppleClient renardeAppleClient;




    @CheckedTemplate
    public static class Templates{

        public static native TemplateInstance user(User user);

        public static native TemplateInstance users(List<User> users);

    }


    @Path("/users")
    @RolesAllowed("admin")
    public TemplateInstance users(){
        return Templates.users(User.listAll());
    }


    @Path(("/account"))
    public TemplateInstance user(){
        return Templates.user(getUser());
    }


    @Path("/user")
    @POST
    public void updateUser(@RestForm String firstName,@RestForm String lastName){
        Log.info("Incomming User update p = " + firstName + " | " + lastName);
        User user = getUser();
        user.firstName = firstName;
        user.lastName = lastName;
        user.persist();
        user();
    }

    @Path("/revokeApple")
    @Transactional
    public Response revokeApple(){
        User user = getUser();
        Log.info(String.format("Revoke apple for [%s] [%s] : [%s]", user.userName, appleClientId, accessToken.getToken()));

        try {
            // generate Apple client secret
            String clientSecret = Jwt.audience("https://appleid.apple.com")
                    .subject(appleClientId)
                    .issuer(appleOidcIssuer)
                    .issuedAt(Instant.now().getEpochSecond())
                    .expiresIn(Duration.ofDays(1))
                    .jws()
                    .keyId(appleOidcKeyId)
                    .algorithm(SignatureAlgorithm.ES256)
                    .sign(getPrivateKey(String.format("src/main/resources/%s", appleKeyFile), "EC"));
            // Revoke token access for apple user
            //see https://developer.apple.com/documentation/accountorganizationaldatasharing/revoke-tokens
            renardeAppleClient.revokeAppleUser(appleClientId, clientSecret, accessToken.getToken(), "access_token");
        } catch (IOException e) {
            Log.error("Error I/O while reading apple private key : " + e.getMessage());
            flash("backendError", "Error apple key not found");
            return Response.seeOther(Router.getURI(Users::user)).build();
        } catch (ClientWebApplicationException e) {
            Log.error("Error while calling Apple /revoke resource status: " + e.getResponse().getStatus());
            flash("backendError", "Error on /revoke : " + e.getMessage());
            return Response.seeOther(Router.getURI(Users::user)).build();
        }

        // rm user
        user.delete();
        // logout
        return Response.seeOther(Router.getURI(Login::logout)).build();
    }


    private static PrivateKey getPrivateKey(String filename, String algorithm) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filename)), "utf-8");
        try {
            String privateKey = content.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            KeyFactory kf = KeyFactory.getInstance(algorithm);
            return kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Java did not support the algorithm:" + algorithm, e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Invalid key format");
        }
    }

}
