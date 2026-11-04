# Contrato del evento CATEGORIA_CREADA

Publicado por **Catalogo** (EC2 A) en `catalogo.exchange` con routing key `categoria.creada`,
consumido por **Solicitudes** (EC2 B) desde `solicitudes.catalogo.queue`.
Se publica solo al crear un item de tipo `CATEGORIA` (las prioridades y las actualizaciones/bajas no generan evento).

## Cuerpo (JSON, `content_type: application/json`)

| Campo | Tipo | Formato / regla | Ejemplo |
|---|---|---|---|
| `eventId` | String | UUID v4; tambien es el `message_id` AMQP | `9398bb76-f4ad-4c5e-8a1e-308dcb5bc9d7` |
| `tipoEvento` | String | Siempre `CATEGORIA_CREADA`; tambien va en la propiedad `type` | `CATEGORIA_CREADA` |
| `fechaEvento` | String | Instante UTC en ISO-8601 (`yyyy-MM-ddTHH:mm:ss.SSSSSSSSSZ`) | `2026-11-02T15:02:34.812603712Z` |
| `categoriaId` | Long | Id **original** de la fila en RDS Catalogo; en Solicitudes es la clave de `categoria_local` | `9` |
| `nombre` | String | 1–100 caracteres | `Software` |

## Propiedades AMQP
| Propiedad | Valor |
|---|---|
| `delivery_mode` | `2` (persistente) |
| `message_id` | = `eventId` |
| `type` | `CATEGORIA_CREADA` |
| `content_type` | `application/json` |

## Ejemplo real (mensaje leido con "Nack message requeue true" durante P3)
```json
{"eventId":"090b5223-5dc3-494a-b80a-04bb8a4d8165","tipoEvento":"CATEGORIA_CREADA","fechaEvento":"2026-11-02T15:02:34.812603712Z","categoriaId":9,"nombre":"Software"}
```

## Garantias y limites
- El publicador espera el **ack** del broker (`publisher-confirm-type=correlated`) y detecta el **retorno**
  si no hay ruta a una cola (`mandatory=true`). Si no hay confirmacion responde **502** y avisa que la
  categoria **si quedo guardada** en RDS Catalogo (no se revierte).
- El ack del broker solo indica que RabbitMQ acepto el mensaje; **no** acredita que Solicitudes lo haya guardado.
- El consumidor (modo AUTO, prefetch 1) confirma **despues** de guardar en RDS Solicitudes; si falla, el mensaje vuelve a la cola.
- Una reentrega no duplica la fila (clave `categoria_id`), pero **no** hay garantia de procesamiento exactamente una vez.
- Fuera de alcance: outbox, DLQ, reintentos avanzados, propagacion de actualizaciones/bajas.

## Trazabilidad
HTTP `POST /api/catalogo` → respuesta con `id` y `eventId` → log `[RDS CATALOGO] guardado id=…` →
log `[PUBLICADO] ack del broker … messageId=… categoriaId=…` → log `[RECIBIDO] messageId=…` (EC2 B) →
log `[RDS SOLICITUDES] copia guardada … -> ack` → fila `categoria_local(categoria_id, event_id, fecha_evento, fecha_recepcion)`.
