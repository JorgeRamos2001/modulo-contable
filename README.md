# Módulo Contable

Universidad Católica de El Salvador — Actividad III del III Periodo (25% + 5% parcial).

Sistema web que automatiza el ciclo contable básico:
- **Libro Diario**: registro de transacciones con validación obligatoria de Partida Doble.
- **Libro Mayor**: mayorización automática en tiempo real (saldos por cuenta sin cálculo manual).
- **Estados Financieros dinámicos**: Balance General (1 = 2 + 3) y Estado de Resultados (5 − 4 = utilidad), generados por clasificación automática del código de cuenta.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 25 + Spring Boot 4.1.1 |
| Frontend | Vue.js 3 |
| Base de datos | PostgreSQL |
| Documentación API | springdoc-openapi (Swagger UI) |

## Requisitos previos

- JDK 25
- Maven 3.9+
- PostgreSQL corriendo localmente, con una base de datos llamada `modulo_contable`

## Cómo correr el backend

1. Crea la base de datos en Postgres:
   ```sql
   CREATE DATABASE modulo_contable;
   ```
2. Ajusta usuario/contraseña en `src/main/resources/application.yml` si no usas las credenciales por defecto.
3. Desde la raíz del proyecto:
   ```bash
   mvn spring-boot:run
   ```
4. El backend queda disponible en `http://localhost:8080`.
   Documentación interactiva de la API: `http://localhost:8080/swagger-ui.html`.

> `schema.sql` y `data.sql` (en `src/main/resources`) se ejecutan automáticamente en cada arranque (`spring.sql.init.mode: always`), recreando las tablas y cargando el Catálogo de Cuentas. Esto es intencional durante el desarrollo; antes de la entrega final se debe desactivar para no perder datos de la demo en cada reinicio.

## Estructura del proyecto

```
src/main/java/com/modulocontable/
├── controller/
├── service/
├── repository/
├── model/
├── dto/
└── exception/
```

## Equipo

_(completar con los integrantes)_

## Flujo de trabajo (Git)

Ramas por tipo de cambio (`feature/...`, `fix/...`), fusionadas a `main` vía pull request.
