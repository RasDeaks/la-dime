package service;

import client.insee.InseeSireneApiClient;
import client.insee.dto.ReponseEtablissement;
import client.insee.dto.ReponseUnitesLegales;
import client.insee.dto.parts.PeriodeUniteLegale;
import error.DimeError;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import model.Entreprise;
import model.Etablissement;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import util.InseeApiUtils;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class SirenService {

    @RestClient
    InseeSireneApiClient sireneApiClient;

    @ConfigProperty(name = "quarkus.rest-client.\"client.insee.InseeSireneApiClient\".auth")
    String authHeader;

    public String verifyAndSaveCompany(Entreprise entreprise) throws DimeError {
        Log.info(String.format("START callBySiren, siren=%s", entreprise.siren));

        // call INSEE api /siren
        ReponseUnitesLegales sirenResp = sireneApiClient.getBySiren(authHeader, entreprise.siren);
        Log.info(String.format("END status=%s, message= [%s]", sirenResp.header.statut, sirenResp.header.message));

        // verify activity & extract SIRETs or exit
        List<PeriodeUniteLegale> activeNicCodes = InseeApiUtils.filterActiveLegalUnit(sirenResp);
        Log.info(String.format("%s 'Etablissement(s)' found for Siren [%s]", activeNicCodes.size(), entreprise.siren));
        if (activeNicCodes.isEmpty()){
            throw new DimeError(String.format("Siren [%s] not ACTIVE !", entreprise.siren));
        }

        // call INSEE api /siret for additional infos
        List<ReponseEtablissement> siretsResp = activeNicCodes.stream().map(nic -> {
            Log.info(String.format("START callBySiret, siret=%s", entreprise.siren + nic.nicSiegeUniteLegale));
            ReponseEtablissement siretResp = sireneApiClient.getBySiret(authHeader, entreprise.siren + nic.nicSiegeUniteLegale);
            Log.info(String.format("END status=%s, message= [%s]", siretResp.header.statut, siretResp.header.message));
            return siretResp;
        }).toList();

        // filter only active SIRET or exit
        List<ReponseEtablissement> siretFiltered = InseeApiUtils.filterActiveEstablishment(siretsResp);
        Log.info(String.format("%s ACTIVE 'Etablissement(s)' checked for Siren [%s]", siretFiltered.size(), entreprise.siren));
        if (siretFiltered.isEmpty()){
            throw new DimeError(String.format("Siren [%s] has no ACTIVE 'etablissement' !", entreprise.siren));
        }

        // Save Company info in DB
        List<Etablissement> etablissements = persistCompanyInfo(siretFiltered);
        Log.info(String.format("Company updated, %s Establishement(s) updated", etablissements.size()));

        // return pretty print of "etablissement(s)"
        return etablissements.stream()
                .map(InseeApiUtils::prettyPrintEstablishment)
                .collect(Collectors.joining(" - "));
    }


    private List<Etablissement> persistCompanyInfo(List<ReponseEtablissement> etablissementActifs){
        // get UniteLegale from first result
        ReponseEtablissement firstEtab = etablissementActifs.get(0);
        // upsert Entreprise
        Entreprise bySiren = (Entreprise) Entreprise.find("siren", firstEtab.etablissement.siren)
                .firstResultOptional().orElse(new Entreprise());
        bySiren.siren = firstEtab.etablissement.siren;
        bySiren.raisonSocial = firstEtab.etablissement.uniteLegale.denominationUniteLegale;
        // upsert Etablissement
        List<Etablissement> etablissements = etablissementActifs.stream().map(etab -> {
            Etablissement etablissement = (Etablissement) Etablissement.find("siret", etab.etablissement.siret)
                    .firstResultOptional()
                    .orElse(new Etablissement());
            etablissement.entreprise = bySiren;
            etablissement.siret = etab.etablissement.siret;
            etablissement.denominationUniteLegale = etab.etablissement.uniteLegale.denominationUniteLegale;
            if (null != etab.etablissement.adresseEtablissement) {
                etablissement.numVoie = etab.etablissement.adresseEtablissement.numeroVoieEtablissement;
                etablissement.typeVoie = etab.etablissement.adresseEtablissement.typeVoieEtablissement;
                etablissement.libelleVoie = etab.etablissement.adresseEtablissement.libelleVoieEtablissement;
                etablissement.codePostal = etab.etablissement.adresseEtablissement.codePostalEtablissement;
                etablissement.libelleCommune = etab.etablissement.adresseEtablissement.libelleCommuneEtablissement;
            }
            return etablissement;
        }).toList();

        bySiren.persist();
        etablissements.forEach(etablissement -> etablissement.persist());
        return etablissements;
    }
}
