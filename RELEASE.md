# Release Notes — Langa Spring Agent v1.0.0

**Date** : 15 mars 2026
**Artifact** : `com.capricedumardi:langa-spring-agent:1.0.0`
**Licence** : Apache 2.0

> Première release du Langa Spring Agent — un agent d'observabilité léger pour les applications Spring Boot. Cette version pose les fondations de la collecte automatique de logs et de métriques avec envoi vers un backend Langa.

---

## Quoi de neuf

### Collecte de logs

- Support **Logback** (défaut Spring Boot) et **Log4j2** via des appenders dédiés
- Détection automatique du framework de logging depuis le classpath
- Binding automatique de l'appender au démarrage de l'agent — aucune configuration XML requise pour Logback

### Collecte de métriques

- Annotation `@Monitored(name = "...")` pour instrumenter les méthodes Spring
- Aspect Spring AOP avec capture automatique du temps d'exécution, statut et contexte HTTP
- `MonitoringStatusFilter` pour résoudre le statut HTTP final en fin de requête (gestion des snapshots différés)
- Auto-configuration Spring Boot activée par défaut (`langa.monitoring.enabled=true`)

### Envoi des données

- **HTTP Sender** — Apache HttpClient 4.5, compression GZIP automatique au-delà d'un seuil configurable, retry avec exponential backoff
- **Kafka Sender** — Kafka Client 3.9.1, mode async avec callback tracking, compression Snappy, idempotence activée
- Sélection automatique HTTP/Kafka basée sur le format de l'URL d'ingestion
- Fallback automatique vers un `NoOpSenderService` en cas de configuration invalide (fail-safe)

### Buffers et résilience

- Buffer à double file (principale + retry) avec batching configurable
- Flush périodique et flush sur seuil de batch
- Retry automatique avec exponential backoff et jitter
- **Circuit Breaker** : états CLOSED → OPEN → HALF_OPEN avec seuils configurables

### Configuration

- Multi-source avec priorité : System Properties > Variables d'environnement > Fichier `langa-agent.properties` > Défauts
- Configuration dynamique à chaud via **JMX MBeans** et **Spring Boot Actuator**
- Endpoints Actuator auto-configurés : `GET /actuator/langaMetrics`, `GET|POST /actuator/langaConfig`

### Sécurité

- Authentification HMAC-SHA256 sur chaque requête d'envoi
- Credentials encodés en Base64 dans l'URL d'ingestion (format `accountKey-lga-appKey`)
- Résolution de la CVE-2025-66566 (lz4-java) via remplacement par `at.yawk.lz4:lz4-java:1.10.4`

---

## Prérequis

| Composant | Version minimale |
|-----------|-----------------|
| Java | 17+ |
| Spring Boot | 3.x |
| Maven | 3.8+ |

---

## Installation

```xml
<dependency>
    <groupId>com.capricedumardi</groupId>
    <artifactId>langa-spring-agent</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## Configuration minimale

```bash
export LANGA_INGESTION_URL=https://<host>/api/ingestion/h/<base64(accountKey-lga-appKey)>
export LANGA_INGESTION_SECRET=your-secret-key
```

Le framework de logging est détecté automatiquement. Pour le forcer : `export LOGGING_FRAMEWORK=logback`

---

## Dépendances principales

| Dépendance | Version |
|------------|---------|
| Spring Boot (parent) | 3.5.5 |
| Logback | 1.5.32 |
| Log4j2 | 2.25.3 |
| Apache HttpClient | 4.5.14 |
| Kafka Client | 3.9.1 |
| Gson | 2.13.1 |
| AspectJ | 1.9.24 |

---

## Limitations connues

- Le `KafkaSenderService` nécessite un broker Kafka accessible au démarrage pour créer le `KafkaProducer`
- Le mode Java Agent (`-javaagent`) est fonctionnel mais l'utilisation comme dépendance Spring Boot est recommandée pour bénéficier de l'auto-configuration

---

## Liens

- [README](README.md)
- [Guide de configuration complet](src/main/resources/configuration-guide.md)
- [Code source](https://github.com/langa-org/langa-spring)
- [Issues](https://github.com/langa-org/langa-spring/issues)
