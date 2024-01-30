# la-dime

Ce projet use Quarkus, le cadriciel Supersonique Subatomique Java.

Quarkus ? -> website: https://quarkus.io/ .

## WTF ?

La dime c'est pour demat' de la Facture

1. Je créé des factures
   * html FORM
   * ~~UBL/CII file upload~~
2. Je les valide
   * Structure (cardinalité, enum)
   * ~~Fonc (xslt, chematorn, siren/siret/routage)~~
3. Je les range secure pour mes utilisateurs enregistrés
   * OIDC (in progress)
   * ~~Gestion par entreprise~~
4. ~~Je les affiche en PDF, xml, HTML~~
   * ~~Nice UX lol!~~
   * ~~Polymorphe UBL/CII -> Entité -> Form/HTML~~
5. ~~(Je sais les adresser..)~~
   * ~~Vérification des SIREN~~
   * ~~Appel Annuaire~~




#
#
#

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.


## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/la-dime-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Related Guides

- Renarde ([guide](https://quarkiverse.github.io/quarkiverse-docs/quarkus-renarde/dev/index.html)): Renarde is a server-side Web Framework based on Quarkus, Qute, Hibernate and RESTEasy Reactive.

## Provided Code

### Renarde

This is a small Renarde webapp

[Related guide section...](https://quarkiverse.github.io/quarkiverse-docs/quarkus-renarde/dev/index.html)

