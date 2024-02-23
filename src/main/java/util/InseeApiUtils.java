package util;

import client.insee.dto.ReponseEtablissement;
import client.insee.dto.ReponseUnitesLegales;
import client.insee.dto.parts.PeriodeUniteLegale;
import model.Etablissement;

import java.util.Collections;
import java.util.List;

public class InseeApiUtils {

    // Utils!
    private InseeApiUtils() {}

    private static final String ACTIVE = "A";


    public static List<PeriodeUniteLegale> filterActiveLegalUnit(ReponseUnitesLegales legalUnit) {
        // null-check
        if (null == legalUnit.uniteLegale ||
                null == legalUnit.uniteLegale.periodesUniteLegale ||
                legalUnit.uniteLegale.periodesUniteLegale.isEmpty()){
            return Collections.emptyList();
        }
        // filter Active establishment
        return legalUnit.uniteLegale.periodesUniteLegale.stream()
                .filter(periode -> null == periode.dateFin) // NO END DATE
                .filter(periode -> ACTIVE.equalsIgnoreCase(periode.etatAdministratifUniteLegale)) // STILL ACTIVE
                .filter(periode -> null != periode.nicSiegeUniteLegale) // WITH AN IDENTIFIER
                .toList();
    }


    public static List<ReponseEtablissement> filterActiveEstablishment(List<ReponseEtablissement> establishments) {
        return establishments.stream()
                .filter(etab -> 200 == etab.header.statut)
                .filter(etab -> null != etab.etablissement)
                .filter(etab -> null != etab.etablissement.uniteLegale)
                .filter(etab -> ACTIVE.equalsIgnoreCase(etab.etablissement.uniteLegale.etatAdministratifUniteLegale)) // must belong to an active Legal Entity
                .filter(etab -> null != etab.etablissement.periodesEtablissement)
                .filter(etab -> !etab.etablissement.periodesEtablissement.isEmpty())
                .filter(etab -> etab.etablissement.periodesEtablissement.stream()
                        .anyMatch(period -> null == period.dateFin &&
                                ACTIVE.equalsIgnoreCase(period.etatAdministratifEtablissement))) //active = At least 1 period with not end date and 'active'
                .toList();
    }

    public static String prettyPrintEstablishment(Etablissement establishement) {
        return String.format("[%s - %s %s %s, %s %s]",
                establishement.siret,
                establishement.numVoie,
                establishement.typeVoie,
                establishement.libelleVoie,
                establishement.codePostal,
                establishement.libelleCommune);
    }

}
