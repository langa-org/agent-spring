# Langa Spring Agent

[![Maven Central](https://img.shields.io/maven-central/v/com.capricedumardi/langa-spring-agent)](https://central.sonatype.com/artifact/com.capricedumardi/langa-spring-agent)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue)](https://openjdk.org/)
[![Spring Boot 3.x](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Agent Java léger pour la collecte automatique de **logs** et de **métriques** dans les applications Spring Boot, avec envoi vers le backend [Langa](https://github.com/langa-org).

## Fonctionnalités

- **Collecte de logs** — Intégration automatique avec Logback (défaut Spring Boot) et Log4j2
- **Collecte de métriques** — Monitoring des méthodes via `@Monitored` et Spring AOP
- **Envoi HTTP ou Kafka** — Compression GZIP automatique, retry avec exponential backoff
- **Circuit Breaker** — Protection contre les backends défaillants
- **Configuration dynamique** — Modifiable à chaud via JMX ou Spring Boot Actuator
- **Auto-configuration Spring Boot** — Monitoring et Actuator activés automatiquement

---

## Installation

### Maven

```xml
<dependency>
    <groupId>com.capricedumardi</groupId>
    <artifactId>langa-spring-agent</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.capricedumardi:langa-spring-agent:1.0.0'
```

---

## Démarrage rapide

### 1. Configurer les variables d'environnement requises

```bash
# URL d'ingestion Langa (contient le type de sender + les credentials encodés en base64)
export LANGA_INGESTION_URL=https://api.langa.io/api/ingestion/h/<base64_credentials>

# Clé secrète pour l'authentification HMAC
export LANGA_INGESTION_SECRET=your-secret-key

# Framework de logging (optionnel — détecté automatiquement depuis le classpath)
export LOGGING_FRAMEWORK=logback
```

> **Format de l'URL d'ingestion** :
> `https://<host>/api/ingestion/<type>/<base64(accountKey-lga-appKey)>`
>
> - `type` : `h` pour HTTP, ou un nom de topic Kafka
> - `base64_credentials` : encodage Base64 de `{accountKey}-lga-{appKey}`
>
> Pour Kafka : `kafka://<broker>:<port>/api/ingestion/<topic>/<base64_credentials>`

### 2. Lancer l'application

**Comme Java Agent (standalone) :**

```bash
java -javaagent:langa-spring-agent-1.0.0.jar -jar your-application.jar
```

**Comme dépendance Spring Boot (recommandé) :**

Ajoutez la dépendance Maven/Gradle ci-dessus. L'auto-configuration Spring Boot active automatiquement :
- Le monitoring des méthodes annotées `@Monitored`
- Les endpoints Actuator (`/actuator/langaMetrics`, `/actuator/langaConfig`)

---

## Collecte de logs

Le framework de logging est **détecté automatiquement** depuis le classpath. Pour les applications Spring Boot, Logback est utilisé par défaut.

Vous pouvez forcer le choix via `LOGGING_FRAMEWORK` :

| Valeur | Framework |
|--------|-----------|
| `logback` | Logback (défaut Spring Boot) |
| `log4j2` | Log4j2 |
| `none` | Collecte de logs désactivée |

### Logback (défaut)

L'agent s'attache automatiquement au `LoggerContext`. Aucune configuration XML supplémentaire n'est nécessaire.

Configuration manuelle optionnelle dans `logback.xml` :

```xml
<configuration>
    <appender name="LANGA" class="com.capricedumardi.agent.core.appenders.LangaLogbackAppender"/>
    <root level="INFO">
        <appender-ref ref="LANGA"/>
    </root>
</configuration>
```

### Log4j2

```xml
<Configuration>
    <Appenders>
        <Langa name="LangaAppender"/>
    </Appenders>
    <Loggers>
        <Root level="info">
            <AppenderRef ref="LangaAppender"/>
        </Root>
    </Loggers>
</Configuration>
```

---

## Collecte de métriques

Annotez vos méthodes avec `@Monitored` pour mesurer automatiquement leur temps d'exécution :

```java
import com.capricedumardi.agent.core.metrics.Monitored;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    @Monitored(name = "processOrder")
    public Order processOrder(Order order) {
        // Le temps d'exécution, le statut et les infos HTTP sont collectés automatiquement
        return orderRepository.save(order);
    }
}
```

Le monitoring est activé par défaut. Pour le désactiver :

```properties
# application.properties
langa.monitoring.enabled=false
```

---

## Configuration

L'agent utilise une configuration multi-source (par ordre de priorité) :

1. **System Properties** — `-Dlanga.buffer.batch.size=100`
2. **Variables d'environnement** — `LANGA_BUFFER_BATCH_SIZE=100`
3. **Fichier** — `langa-agent.properties` (racine du projet)
4. **Valeurs par défaut**

### Variables requises

| Variable | Description |
|----------|-------------|
| `LANGA_INGESTION_URL` | URL d'ingestion (HTTP ou Kafka) |
| `LANGA_INGESTION_SECRET` | Clé secrète HMAC |

### Configuration principale

| Propriété | Env Variable | Défaut | Description |
|-----------|-------------|--------|-------------|
| `langa.buffer.batch.size` | `LANGA_BUFFER_BATCH_SIZE` | `50` | Taille du batch avant envoi |
| `langa.buffer.flush.interval.seconds` | `LANGA_BUFFER_FLUSH_INTERVAL_SECONDS` | `5` | Intervalle de flush automatique |
| `langa.buffer.main.queue.capacity` | `LANGA_BUFFER_MAIN_QUEUE_CAPACITY` | `10000` | Capacité de la file principale |
| `langa.http.compression.threshold.bytes` | `LANGA_HTTP_COMPRESSION_THRESHOLD_BYTES` | `1024` | Seuil de compression GZIP (octets) |
| `langa.http.max.retry.attempts` | `LANGA_HTTP_MAX_RETRY_ATTEMPTS` | `3` | Tentatives de retry HTTP |
| `langa.circuit.breaker.failure.threshold` | `LANGA_CIRCUIT_BREAKER_FAILURE_THRESHOLD` | `5` | Échecs avant ouverture du circuit |
| `langa.circuit.breaker.open.duration.millis` | `LANGA_CIRCUIT_BREAKER_OPEN_DURATION_MILLIS` | `30000` | Durée du circuit ouvert (ms) |
| `langa.monitoring.enabled` | `LANGA_MONITORING_ENABLED` | `true` | Activer/désactiver le monitoring AOP |
| `langa.debug.mode` | `LANGA_DEBUG_MODE` | `false` | Mode debug (logs verbeux de l'agent) |

> Voir le [guide de configuration complet](src/main/resources/configuration-guide.md) pour toutes les options (HTTP, Kafka, scheduler, retry).

---

## Monitoring en production

### Spring Boot Actuator

Si `spring-boot-actuator` est sur le classpath, deux endpoints sont exposés automatiquement :

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/langaMetrics` | Statistiques des buffers, sender, erreurs, uptime |
| `GET /actuator/langaConfig` | Configuration courante de l'agent |
| `POST /actuator/langaConfig` | Modifier la config à chaud (`batchSize`, `flushIntervalSeconds`, `debugMode`, `compressionThreshold`) |

### JMX

Les MBeans `LangaAgent:*` exposent les mêmes métriques et contrôles pour les outils JMX (JConsole, VisualVM).

---

## Architecture

```
┌─────────────────────────────────────┐
│        Application Spring Boot      │
│                                     │
│   Logback/Log4j2 ──► LangaAppender │
│   @Monitored ──► SpringAspect      │
│              │             │        │
│              └──────┬──────┘        │
│              ┌──────▼──────┐        │
│              │   Buffers   │        │
│              │ (batch+retry)│       │
│              └──────┬──────┘        │
│              ┌──────▼──────┐        │
│              │ SenderService│       │
│              │ (HTTP/Kafka) │       │
│              └──────┬──────┘        │
└─────────────────────┼───────────────┘
                      │
               ┌──────▼──────┐
               │ Langa Backend│
               └─────────────┘
```

---

## Développement

### Prérequis

- Java 17+
- Maven 3.8+

### Build

```bash
mvn clean install
```

### Tests

```bash
mvn test
```

Le rapport de couverture JaCoCo est généré dans `target/site/jacoco/index.html`.

---

## Licence

[Apache License 2.0](LICENSE)

---

Développé par [Caprice du Mardi](https://github.com/langa-org)
