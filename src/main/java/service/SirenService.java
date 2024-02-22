package service;

import client.insee.InseeSireneApiClient;
import client.insee.dto.ReponseEtablissement;
import client.insee.dto.ReponseUnitesLegales;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import model.Entreprise;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class SirenService {

    @RestClient
    InseeSireneApiClient sireneApiClient;

    @ConfigProperty(name = "quarkus.rest-client.\"client.insee.InseeSireneApiClient\".auth")
    String authHeader;

    public String verifySirenByApi(Entreprise entreprise){
        Log.info(String.format("start callBySiren %n\tauth=%s%n\tsiren=%s", authHeader, entreprise.siren));

        // call INSEE api /siren
        ReponseUnitesLegales bySiren = null;
        try {
            bySiren = sireneApiClient.getBySiren(authHeader, entreprise.siren);
        } catch (ClientWebApplicationException e) {
            if (404 == e.getResponse().getStatus()){
                return String.format("Siren [%s] NOT FOUND !", entreprise.siren);
            }
            // dirty dirty
            throw new RuntimeException(e);
        }
        //DixUtils.printThisShit(bySiren);
        if (null == bySiren.uniteLegale || null == bySiren.uniteLegale.periodesUniteLegale || bySiren.uniteLegale.periodesUniteLegale.isEmpty()){
            return String.format("Siren [%s] UNKNOWN !", entreprise.siren);
        }

        // extract active SIRETs
        List<String> nicCodes = bySiren.uniteLegale.periodesUniteLegale.stream()
                .filter(periode -> null == periode.dateFin) // NO END DATE
                .filter(periode -> "A".equalsIgnoreCase(periode.etatAdministratifUniteLegale)) // STILL ACTIVE
                .map(periode -> periode.nicSiegeUniteLegale)
                .toList();
        Log.info(String.format("%s 'Etablissement(s)' found for Siren [%s]", nicCodes.size(), entreprise.siren));
        if (nicCodes.isEmpty()){
            return String.format("Siren [%s] not ACTIVE !", entreprise.siren);
        }

        // call INSEE api /siret
        List<ReponseEtablissement> etablissements = nicCodes.stream().map(nic -> sireneApiClient.getBySiret(authHeader, entreprise.siren + nic)).toList();
        // filter only active SIRET
        List<ReponseEtablissement> etabFiltered = etablissements.stream()
                .filter(etab -> 200 == etab.header.statut)
                .filter(etab -> null != etab.etablissement)
                .filter(etab -> null != etab.etablissement.uniteLegale)
                .filter(etab -> "A".equalsIgnoreCase(etab.etablissement.uniteLegale.etatAdministratifUniteLegale)) // must belong to an active Legal Entity
                .filter(etab -> null != etab.etablissement.periodesEtablissement)
                .filter(etab -> !etab.etablissement.periodesEtablissement.isEmpty())
                .filter(etab -> etab.etablissement.periodesEtablissement.stream()
                        .anyMatch(period -> null == period.dateFin &&
                                "A".equalsIgnoreCase(period.etatAdministratifEtablissement))) //active = At least 1 period with not end date and 'active'
                .toList();
        Log.info(String.format("%s ACTIVE 'Etablissement(s)' found for Siren [%s]", etabFiltered.size(), entreprise.siren));
        //DixUtils.printThisShit(etabFiltered);
        if (etabFiltered.isEmpty()){
            return String.format("Siren [%s] has no ACTIVE 'etablissement' !", entreprise.siren);
        }

        // Save Company info in DB
        persistCompanyInfo(etabFiltered.get(0));


        // return list of "etablissement"
        String joinActiveSirets = etabFiltered.stream()
                .map(etablissement ->
                        String.format("[%s - %s %s %s, %s %s]",
                                etablissement.etablissement.siret,
                                etablissement.etablissement.adresseEtablissement.numeroVoieEtablissement,
                                etablissement.etablissement.adresseEtablissement.typeVoieEtablissement,
                                etablissement.etablissement.adresseEtablissement.libelleVoieEtablissement,
                                etablissement.etablissement.adresseEtablissement.codePostalEtablissement,
                                etablissement.etablissement.adresseEtablissement.libelleCommuneEtablissement))
                .collect(Collectors.joining(" - "));
        return bySiren.uniteLegale.periodesUniteLegale.stream()
                .map(periode -> String.format("[%s] FOUND : %s", periode.denominationUniteLegale, joinActiveSirets))
                .findFirst().orElse(String.format("Siren [%s] NOT  FOUND !", entreprise.siren));
    }


    private void persistCompanyInfo(ReponseEtablissement reponseEtablissement){
        Entreprise bySiren = (Entreprise) Entreprise.find("siren", reponseEtablissement.etablissement.siren).firstResult();
        if (null == bySiren){
            Entreprise newCompany = new Entreprise();
            newCompany.siren = reponseEtablissement.etablissement.siren;
            newCompany.raisonSocial = reponseEtablissement.etablissement.uniteLegale.denominationUniteLegale;
            newCompany.persist();
        } // else update existing ?
    }
}
