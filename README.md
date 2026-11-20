# 📦 Order Service

Microservicio encargado de la gestión del ciclo de vida de órdenes (`Order`), diseñado bajo principios de **alta resiliencia, consistencia eventual y tolerancia a fallos** utilizando **Spring Boot** y **RabbitMQ**.

---

## 🏗️ Arquitectura y Patrones Implementados

Este servicio implementa patrones clave para sistemas distribuidos:

* **Idempotencia en Creación de Órdenes:** Garantiza que múltiples reintentos de una misma petición `POST` (por fallos de red o doble clic) no generen órdenes ni eventos duplicados.
  * Utiliza la cabecera HTTP `Idempotency-Key` (UUID) junto con un hash `SHA-256` del payload (`sku` + `quantity`) para validar la integridad de la petición.
  * Emplea fronteras transaccionales separadas (`OrderService` vs. `OrderCreationService`) y persistencia inmediata (`saveAndFlush`) para detectar condiciones de carrera a nivel de base de datos y recuperar de forma segura el resultado de la petición ganadora.
* **Transactional Outbox Pattern:** Asegura la atomicidad entre el guardado de la orden en la base de datos y la publicación de eventos (`OrderCreated`, `OrderCancelled`) hacia **RabbitMQ**, evitando inconsistencias por caídas del broker.
* **Paginación Estandarizada:** Implementa `Pageable` con serialización limpia (`PageSerializationMode.VIA_DTO`) y límites máximos de tamaño de página (`max-page-size=100`) para proteger el rendimiento de la API.

---

## 🛠️ Stack Tecnológico

* **Java 21+** / **Spring Boot 3.x**
* **Spring Data JPA** (Persistencia y control transaccional)
* **RabbitMQ** (Mensajería asíncrona basada en eventos)
* **Base de Datos Relacional** (H2)
* **SpringDoc OpenAPI (Swagger UI)** (Documentación interactiva de la API)

---

## 🚀 Endpoints Principales

| Método | Ruta | Descripción | Cabeceras requeridas |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/orders` | Crea una nueva orden de forma idempotente. | `Idempotency-Key` (UUID) |
| `GET` | `/api/orders` | Lista órdenes de forma paginada (`page`, `size`, `sort`). | Ninguna |
| `GET` | `/api/orders/{id}` | Obtiene el detalle de una orden por su UUID. | Ninguna |
| `PATCH` / `POST` | `/api/orders/{id}/cancel` | Cancela una orden existente y emite evento `OrderCancelled`. | Ninguna |

### Ejemplo de Creación de Orden (`POST /api/orders`)

**Headers:**
```http
Content-Type: application/json
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

**Request Body:**
```json
{
  "sku": "SKU-001",
  "quantity": 5
}
```

**Response (`201 Created`):**
```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "sku": "SKU-001",
  "quantity": 5,
  "status": "CREATED",
  "failureReason": null,
  "createdAt": "2026-11-19T16:30:00Z",
  "updatedAt": "2026-11-19T16:30:00Z"
}
```

---

## 📡 Eventos Publicados (RabbitMQ)

El servicio registra los siguientes eventos de dominio en la tabla **Outbox** para su posterior despacho al broker:

* **`OrderCreated`**: Emitido al crear exitosamente una nueva orden.
* **`OrderCancelled`**: Emitido cuando una orden cambia su estado a cancelada.

---

## 📖 Documentación de la API (Swagger)

Una vez iniciada la aplicación, puedes explorar y probar los endpoints desde:

```text
http://localhost:8081/swagger-ui.html
```
