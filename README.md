# Document Manager

Semesterprojekt für SWEN3: Eine Webanwendung zur Verwaltung von Dokumenten

## Team

- Haider Elias
- Sineva Valeriia

## Technologien

| Bereich | Technologie |
|---|---|
| Backend | Java, Spring Boot 4.1.1 |
| Java-Umgebung | JDK 25 |
| Persistenz | PostgreSQL 17, JPA, Spring Data JPA |
| Mapping | MapStruct |
| Frontend | Angular 21, Angular Material |
| Webserver | nginx |
| Messaging | RabbitMQ mit AMQP-1.0-Java-Client |
| Deployment | Docker Compose |

## Zusätzlicher Use Case: Labels

Ein Label besitzt eine ID, einen eindeutigen Namen und eine
optionale Farbe als Hex-Wert.

Dokumente können mehrere Labels besitzen. Ein Label kann mehreren
Dokumenten zugeordnet sein.

Die Many-to-Many-Beziehung wird über die Tabelle `document_label`
gespeichert. Beim Löschen eines Labels werden dessen Zuordnungen
entfernt; die Dokumente bleiben bestehen.

## Erststart
Voraussetzung: Docker Desktop mit laufender Docker-Engine.

Die Datei `.exmple.env` kopieren und in `.env` umbenennen. Zugangsdaten in `.env` für die lokale Umgebung festlegen.
Anschließend:

```
docker compose up -d --build
```

| Dienst | Adresse |
|---|---|
| Weboberfläche | http://localhost |
| REST-API | http://localhost:8080/api/documents |
| RabbitMQ Management | http://localhost:15672 |

RabbitMQ verwendet derzeit lokal den Benutzer `guest` mit dem Passwort `guest`.

## REST-Endpunkte

| Methode | Pfad | Funktion |
|---|---|---|
| POST | `/api/documents` | Multipart-Upload mit Feld `file` |
| GET | `/api/documents` | Dokumente auflisten |
| GET | `/api/documents/{id}` | Dokument laden |
| PUT | `/api/documents/{id}` | Dateinamen ändern |
| DELETE | `/api/documents/{id}` | Dokument löschen |
| POST | `/api/documents/{id}/labels/{labelId}` | Label zuordnen |
| DELETE | `/api/documents/{id}/labels/{labelId}` | Zuordnung entfernen |
| POST | `/api/labels` | Label erstellen |
| GET | `/api/labels` | Labels auflisten |
| PUT | `/api/labels/{id}` | Label bearbeiten |
| DELETE | `/api/labels/{id}` | Label löschen |
| POST | `/api/messaging/hello` | Hello-World-Nachricht senden |

## Validierung und Fehlerbehandlung

- Dateinamen dürfen höchstens 255 Zeichen enthalten.
- Labelnamen dürfen höchstens 20 Zeichen enthalten.
- Nicht vorhandene Ressourcen liefern HTTP 404.
- Doppelte Labelnamen liefern HTTP 409.
- Ungültige Eingaben liefern HTTP 400.
- Das Upload-Dateilimit beträgt 50 MB.
- Der gesamte Multipart-Request darf bis zu 51 MB groß sein.