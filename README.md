# Face API - Detección y Análisis Facial con Spring Boot
Servicios de Rapid API, Face Detection y Face Analyzer. Face Detection detecta todos los rostros de una imagen y determina automáticamente la posición de cada cara, sus bounding boxes y landmarks faciales (ojos, nariz, boca). Face Analyzer analiza los atributos faciales de cada rostro detectado: género, edad, expresión, belleza, gafas, pose y máscara.

##  1. APIs utilizadas

- **Rapid API - Face Detection**  
  (api4ai - face-detection14.p.rapidapi.com)

- **Rapid API - Face Analyzer**  
  (faceanalyzer-ai.p.rapidapi.com)

##  2. Spring Boot

-  **Java:** JDK 17  
-  **IDE:** Visual Studio Code  
-  **Maven:** Apache Maven  
-  **Framework:** Spring Boot 3.5.x + WebFlux
  
##  3. Base de datos - PostgreSQL (Neon)

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://ep-soft-cell-acm9jqsu-pooler.sa-east-1.aws.neon.tech:5432/AplicacionesEmpresarialesEnJava
    username: neondb_owner
    password: npg_XRcY3MmzP2La
    properties:
      ssl: true
      sslMode: require
```

   
##  4. Tablas PostgreSQL

```sql
CREATE TABLE face_detection (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    image_url   TEXT,
    image_name  TEXT,
    md5         VARCHAR(32),
    width       INTEGER,
    height      INTEGER,
    status_code VARCHAR(20),
    status_msg  VARCHAR(100),
    entities    TEXT,
    created_at  TIMESTAMP DEFAULT NOW(),
    status      VARCHAR(1) DEFAULT 'A'
);

CREATE TABLE face_analysis (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    image_url   TEXT,
    request_id  VARCHAR(100),
    error_code  INTEGER,
    error_msg   TEXT,
    face_list   TEXT,
    created_at  TIMESTAMP DEFAULT NOW(),
    status      VARCHAR(1) DEFAULT 'A'
);
```

##  5. Maven Dependencias:

```xml
<dependencies>

    <!-- WebFlux reactivo -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- R2DBC PostgreSQL reactivo -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-r2dbc</artifactId>
    </dependency>

    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>r2dbc-postgresql</artifactId>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Test -->
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Swagger para WebFlux -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
        <version>2.6.0</version>
    </dependency>

</dependencies> 
