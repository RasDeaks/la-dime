package client.insee.dto.parts;

import java.util.List;

public class Etablissement {

    public String siren;
    public String siret;
    private boolean etablissementSiege;
    public UniteLegaleEtablissement uniteLegale;
    public Adresse adresseEtablissement;
    public List<PeriodeEtablissement> periodesEtablissement;

}
