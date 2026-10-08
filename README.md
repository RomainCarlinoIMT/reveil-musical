# Réveil musical

Socle initial du projet décrit dans `TP_reveil_musical.pdf`. Le projet utilise
Java et Spring Boot pour préparer l'injection de dépendances (IoC), avec une
séparation entre le domaine et les ports d'accès aux fournisseurs.

Les sources musicales iTunes et MusicBrainz sont intégrées derrière
`TrackProvider`. Le fournisseur composite tente d'abord la source préférée par
l'utilisateur, puis les autres sources distantes configurées, avant de choisir
un morceau local en secours. L'ordonnancement du réveil reste à
faire. Les notifications email, SMS et push disposent chacune d'un sender
mock qui journalise le canal, le destinataire simulé et le message ; aucun
message n'est envoyé réellement.
La création de compte accepte un pseudonyme et génère un `UserId` basé sur un
UUID. Les comptes sont conservés en mémoire uniquement ; les préférences
musicales sont stockées séparément avec une source distante préférée, un morceau
de secours et une liste de morceaux par combinaison jour de semaine / météo.
Les réglages de réveil
(canal de notification et heure locale) sont également séparés. Lors d'un
appel de réveil, un morceau est choisi aléatoirement dans la liste de la
condition reçue, puis envoyé par l'adaptateur du canal choisi. L'heure est
enregistrée comme préférence, mais l'ordonnanceur reste hors périmètre.

## API REST

Les comptes et les préférences musicales ont chacun leur contrôleur. Tous les
endpoints sont sous `/api` :

| Méthode | Endpoint | Résultat |
|---|---|---|
| `POST` | `/api/users` | Crée un compte et renvoie son UUID et son pseudonyme (`201 Created`). |
| `POST` | `/api/users/{userId}/music-preferences` | Remplace les préférences musicales de l'utilisateur (`204 No Content`). |

Exemple de création :

```json
{"pseudonym":"Camille"}
```

Réponse :

```json
{"userId":"a78df734-6964-4774-a079-e67915fa01af","pseudonym":"Camille"}
```

Exemple de préférences musicales :

```json
{
  "preferredSource": "MUSICBRAINZ",
  "fallbackTrack": "Morceau de secours",
  "conditions": [
    {
      "day": "MONDAY",
      "weather": "SOLEIL",
      "tracks": ["Morceau A", "Morceau B"]
    },
    {
      "day": "MONDAY",
      "weather": "PLUIE",
      "tracks": ["Morceau C"]
    }
  ]
}
```

Chaque condition combine les noms enum Java `DayOfWeek` et `WeatherType`. Les
sources disponibles sont `ITUNES` et `MUSICBRAINZ`. La source préférée est
essayée en premier, puis les autres sources configurées, avant le fallback
local. Les conditions dupliquées, les morceaux vides et les champs requis
manquants renvoient `400 Bad Request` ; un `userId` inexistant renvoie
`404 Not Found`.
Le stockage en mémoire est perdu au redémarrage. Ce endpoint ne configure pas
le canal de notification ou l'heure, et l'ordonnanceur n'est pas encore exposé.

## Prérequis et commandes

- JDK 25 ou supérieur
- Maven 3.9+

```sh
mvn test
mvn spring-boot:run
```

Le démarrage lance le contexte Spring Boot. Aucun service externe n'est appelé
avant une recherche de morceau. Pour activer MusicBrainz, définir un User-Agent
conforme aux consignes de l'API, avec un contact permettant de joindre
l'application :

```sh
MUSICBRAINZ_USER_AGENT="ReveilMusical/1.0 (contact: votre-adresse@example.org)" mvn spring-boot:run
```

iTunes est appelé sans clé API. Les deux fournisseurs utilisent un cache en
mémoire (maximum 1 000 recherches, 24 h pour un morceau trouvé, 5 min pour
aucun résultat) et limitent les appels non cachés à un toutes les 3 s pour
iTunes et un par seconde pour MusicBrainz. En cas d'échec technique ou
d'absence de résultat, le fournisseur composite journalise le mode dégradé et
choisit un morceau local.

## Choix technique

L'ensembre des choix techniques et leur justifications se trouve dans `IA.md`

## Structure initiale

```text
src/main/java/fr/reveil/musical/
├── ReveilMusicalApplication.java
├── api/
│   ├── ApiExceptionHandler.java
│   ├── music/
│   │   ├── MusicConditionRequest.java
│   │   ├── MusicPreferencesController.java
│   │   └── SaveMusicPreferencesRequest.java
│   └── user/
│       ├── CreateUserRequest.java
│       ├── UserController.java
│       └── UserResponse.java
├── application/
│   ├── port/
│   │   ├── NotificationAdapter.java
│   │   ├── PreferredTrackProvider.java
│   │   ├── SourceTrackProvider.java
│   │   ├── TrackProvider.java
│   │   ├── TrackProviderException.java
│   │   ├── UserAccountRepository.java
│   │   ├── UserMusicPreferencesProvider.java
│   │   ├── UserPreferencesProvider.java
│   │   └── WakeUpService.java
│   └── service/
│       ├── UserAccountService.java
│       ├── UserMusicPreferencesService.java
│       ├── UserPreferencesService.java
│       └── WakeUpApplicationService.java
├── infrastructure/
│   ├── notification/
│   │   ├── email/
│   │   │   ├── EmailNotificationAdapter.java
│   │   │   ├── EmailSender.java
│   │   │   └── MockEmailSender.java
│   │   ├── push/
│   │   │   ├── PushNotificationAdapter.java
│   │   │   ├── PushSender.java
│   │   │   └── MockPushSender.java
│   │   └── sms/
│   │       ├── MockSmsSender.java
│   │       ├── SmsNotificationAdapter.java
│   │       └── SmsSender.java
│   ├── user/
│   │   ├── InMemoryUserAccountRepository.java
│   │   ├── InMemoryUserMusicPreferencesProvider.java
│   │   └── InMemoryUserPreferencesProvider.java
│   └── track/
│       ├── FallbackTrackProvider.java
│       ├── ItunesTrackProvider.java
│       ├── MusicBrainzTrackProvider.java
│       ├── ResilientTrackProvider.java
│       ├── TrackHttpConfiguration.java
│       ├── TrackSearchCache.java
│       └── TrackSearchRateLimiter.java
└── domain/
    ├── NotificationChannel.java
    ├── MusicCondition.java
    ├── MusicSource.java
    ├── Track.java
    ├── UserAccount.java
    ├── UserId.java
    ├── UserMusicPreferences.java
    ├── UserWakeUpPreferences.java
    ├── WakeUpRequest.java
    └── WeatherType.java
```

- `domain` contient uniquement des types métier indépendants de Spring et des
  fournisseurs externes.
- `application.port` décrit les contrats à implémenter pour lire les
  préférences, rechercher un morceau et envoyer une notification.
- `infrastructure.track` contient les adaptateurs iTunes et MusicBrainz, le
  catalogue de secours, ainsi que le fournisseur composite Spring-injecté.
- `infrastructure.notification` contient un mock et un adaptateur par canal.
  Les méthodes des senders sont volontairement différentes ; chaque adaptateur
  les ramène à l'interface commune `NotificationAdapter`.
- `UserAccountService` inscrit un compte avec un pseudonyme non vide et un ID
  UUID. `UserAccountRepository` isole le stockage, fourni ici par une
  implémentation concurrente en mémoire, perdue au redémarrage.
- `api.user.UserController` crée les comptes ; `api.music.MusicPreferencesController`
  enregistre séparément les préférences musicales d'un compte existant.
  `UserMusicPreferencesService` et `UserMusicPreferencesProvider` gardent le
  stockage de ces préférences indépendant des réglages de notification et de
  l'heure de réveil.
- `UserPreferencesService` stocke séparément le canal et l'heure souhaitée ;
  `LocalTime` ne définit pas encore de fuseau horaire ni de déclenchement.
- `WakeUpApplicationService` lit séparément les préférences musicales et les
  réglages de réveil, puis relie le fournisseur de morceaux et l'adaptateur de
  notification. Une condition absente utilise le morceau de secours.
- Les comptes et préférences restent indépendants en stockage : les
  préférences ne sont pas incorporées au modèle `UserAccount`.
- Les adaptateurs sont injectés par constructeur ; le domaine ne dépend ni de
  Spring ni des formats spécifiques des API.
- `Track` ne transporte pas l'URL spécifique à iTunes : ce détail reste dans
  l'adaptateur correspondant.
- En l'absence de coordonnées utilisateur dans le modèle actuel, l'identifiant
  utilisateur sert uniquement de destinataire simulé dans les logs.

## Dépendances, licences et fraîcheur

Vérification effectuée le 8 octobre 2026 à partir des métadonnées Maven Central,
de Spring Initializr, des pages officielles des projets et des versions
installées dans l'environnement de développement.

| Composant(s) Maven résolu(s) | Version | Licence | Fraîcheur / remarque |
|---|---:|---|---|
| OpenJDK | 25 (25.0.4 installé) | GPLv2 avec Classpath Exception | Java 25 est la version LTS ciblée. JDK 27 est la version GA la plus récente au moment de la vérification ; le projet reste sur la LTS pour privilégier la stabilité. |
| Apache Maven | 3.9.11 installé | Apache-2.0 | Apache Maven 3.10.0 est la dernière version stable annoncée ; Maven est un outil de build local, pas une dépendance livrée par l'application. |
| `spring-boot-maven-plugin` | 4.1.1 | Apache-2.0 | Plugin d'exécution empaqueté avec Spring Boot ; version gérée par le parent Spring Boot. |
| `maven-compiler-plugin`, `maven-resources-plugin`, `maven-surefire-plugin` | 3.15.0, 3.5.0, 3.5.6 | Apache-2.0 | Plugins de compilation, ressources et tests gérés par le parent Spring Boot ; outils de build uniquement. |
| `spring-boot-starter-webmvc`, `spring-boot-starter-jackson`, `spring-boot-jackson`, `spring-boot-webmvc`, `spring-boot-web-server`, `spring-boot-servlet`, `spring-boot-http-converter` | 4.1.1 | Apache-2.0 | Starter REST MVC et infrastructure JSON/HTTP gérés par le BOM stable Spring Boot. |
| `spring-web`, `spring-webmvc` | 7.0.9 | Apache-2.0 | Framework REST ; versions gérées par le BOM Spring Boot. |
| `spring-boot-starter-tomcat`, `spring-boot-starter-tomcat-runtime`, `spring-boot-tomcat`, `tomcat-embed-core`, `tomcat-embed-el`, `tomcat-embed-websocket` | 4.1.1, 11.0.24 | Apache-2.0 | Serveur HTTP embarqué pour l'API ; versions gérées par le BOM Spring Boot. |
| `jackson-databind`, `jackson-core` (Jackson 3) | 3.1.5 | Apache-2.0 | Utilisés pour lire les réponses JSON ; version gérée par le BOM de Spring Boot 4.1.1. Jackson 3.2.3 est plus récent ; on conserve la version du BOM pour éviter une surcharge qui pourrait introduire une incompatibilité. |
| `jackson-annotations` (Jackson 2) | 2.21 | Apache-2.0 | Dépendance transitive du module Jackson 3, version gérée par le BOM Spring Boot ; 2.21.5 est plus récente mais reste non surchargée pour préserver l'alignement du BOM. |
| `spring-boot-starter`, `spring-boot-starter-logging`, `spring-boot-autoconfigure`, `spring-boot`, `spring-boot-test` | 4.1.1 | Apache-2.0 | Version stable proposée par Spring Initializr. Gérée par le parent/BOM Spring Boot ; aucune surcharge de version. |
| `spring-context`, `spring-aop`, `spring-beans`, `spring-expression`, `spring-core`, `spring-test` | 7.0.9 | Apache-2.0 | Versions choisies et gérées par le BOM stable Spring Boot 4.1.1. |
| `micrometer-observation`, `micrometer-commons` | 1.17.1 | Apache-2.0 | Versions choisies et gérées par le BOM stable Spring Boot 4.1.1. |
| `logback-classic`, `logback-core` | 1.5.38 | EPL-1.0 ou LGPL-2.1 | Licence au choix du redistributeur ; versions gérées par le BOM stable Spring Boot 4.1.1. |
| `log4j-to-slf4j`, `log4j-api` | 2.25.5 | Apache-2.0 | Versions gérées par le BOM stable Spring Boot 4.1.1. |
| `jul-to-slf4j`, `slf4j-api` | 2.0.18 | MIT | Versions gérées par le BOM stable Spring Boot 4.1.1. |
| `jakarta.annotation-api` | 3.0.0 | EPL-2.0 ou GPL-2.0 avec Classpath Exception | Double licence ; versions gérées par le BOM stable Spring Boot 4.1.1. |
| `snakeyaml` | 2.6 | Apache-2.0 | Version gérée par le BOM stable Spring Boot 4.1.1. |
| `commons-logging` | 1.3.6 | Apache-2.0 | Version gérée par le BOM stable Spring Boot 4.1.1. |
| `jspecify` | 1.0.1 | Apache-2.0 | Version gérée par le BOM stable Spring Boot 4.1.1. |
| `junit-jupiter`, `junit-jupiter-api`, `junit-jupiter-params`, `junit-jupiter-engine`, `junit-platform-commons`, `junit-platform-engine` | 6.0.3 | EPL-2.0 | Framework de tests ; versions gérées par le BOM stable Spring Boot 4.1.1. |
| `opentest4j` | 1.3.0 | Apache-2.0 | Version gérée par le BOM stable Spring Boot 4.1.1. |
| `apiguardian-api` | 1.1.2 | Apache-2.0 | Version gérée par le BOM stable Spring Boot 4.1.1. |

Les versions Maven effectives des dépendances de production et de test ont été
relevées dans l'arbre `mvn dependency:tree`. Les versions transitives sont
alignées sur le BOM stable Spring Boot 4.1.1 (aucune version n'est surchargée
individuellement). Le socle de test est volontairement limité à Spring Test et
JUnit ; il n'ajoute pas de bibliothèques de mock ou d'assertions tierces.
Les mocks de notification utilisent SLF4J fourni transitivement par Spring
Boot ; aucune nouvelle dépendance n'a été ajoutée pour eux.
Le serveur REST s'appuie sur Spring MVC et Tomcat embarqué, tous deux sous
licence Apache-2.0, sans composant propriétaire.
Vérifier à nouveau licence et fraîcheur avant toute mise à jour du BOM ou
ajout de dépendance.

Sources de vérification :

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Initializr (versions prises en charge)](https://start.spring.io/metadata/client)
- [Licence Spring Boot](https://github.com/spring-projects/spring-boot/blob/main/LICENSE.txt)
- [Maven Central - parent Spring Boot 4.1.1](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-starter-parent/4.1.1/spring-boot-starter-parent-4.1.1.pom)
- [Maven Central - Jackson Databind 3.1.5](https://repo.maven.apache.org/maven2/tools/jackson/core/jackson-databind/3.1.5/jackson-databind-3.1.5.pom)
- [MusicBrainz - politique de limitation](https://musicbrainz.org/doc/MusicBrainz_API/Rate_Limiting)
- [OpenJDK - GPLv2 avec Classpath Exception](https://openjdk.org/legal/gplv2+ce.html)
- [Versions Apache Maven](https://maven.apache.org/download.cgi)
- [Versions OpenJDK disponibles](https://jdk.java.net/)


## Trouble shooting

# Case 1
If test are ends with errors like `can't find adapter for xxx`, you might need to use `mvn clean` and then do the tests again `mvn test`.
Occurred when I made major changes in interfaces.
