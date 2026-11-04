# Endpoints

React solo llama al **API Gateway** (`https://qifufilp2j.execute-api.us-east-1.amazonaws.com`),
ruta `ANY /{proxy+}` con JWT Authorizer de Entra ID y `OPTIONS /{proxy+}` sin autorizador (preflight CORS).
Ambas rutas usan la integracion HTTP `http://<IP elastica EC2 A>:8083/{proxy}` (BFF).

## BFF (EC2 A, puerto 8083) – JWT de Entra ID + autorizacion por rol
| Metodo y ruta | Roles | Delegado a |
|---|---|---|
| GET `/api/usuario` | autenticado | — (datos del token) |
| GET `/api/catalogo` (v1) | autenticado | Catalogo `GET /v1/catalogo` |
| GET `/api/v2/catalogo` (v2) | autenticado | Catalogo `GET /v1/catalogo/v2/catalogo` |
| POST `/api/catalogo` | ADMINISTRADOR | Catalogo `POST /v1/catalogo` (guarda y publica) |
| GET `/api/categorias-locales` | autenticado | Solicitudes `GET /v1/categorias` |
| GET `/api/categorias-locales/{id}` | autenticado | Solicitudes `GET /v1/categorias/{id}` (404 = aun no llega) |
| POST `/api/solicitudes` | CLIENTE, ADMINISTRADOR | Solicitudes `POST /v1/solicitudes` (el BFF inyecta el correo del token) |
| GET `/api/solicitudes/mias` | CLIENTE, ADMINISTRADOR | Solicitudes `GET /v1/solicitudes/mias?usuario=` |
| GET `/api/solicitudes` | OPERADOR, ADMINISTRADOR | Solicitudes `GET /v1/solicitudes` |
| PUT `/api/solicitudes/{id}/estado` | OPERADOR, ADMINISTRADOR | Solicitudes `PUT /v1/solicitudes/{id}/estado` |

Sin token: **401**. Token valido sin el rol: **403**.

## Catalogo (EC2 A, 8082 – solo accesible desde la misma EC2)
| Metodo y ruta | Respuesta |
|---|---|
| GET `/v1/catalogo`, GET `/v1/catalogo/tipo/{tipo}` | Lista de items |
| POST `/v1/catalogo` `{"nombre","tipo"}` | 201 guardado y publicado · 502 guardado sin confirmacion · 400 invalido · 409 duplicado |
| GET `/v1/catalogo/v2/catalogo` | v2: items + `version` y `fechaConsulta` |

## Solicitudes (EC2 B, 8081 – solo accesible desde el SG de la EC2 A)
| Metodo y ruta | Respuesta |
|---|---|
| GET `/v1/categorias` | Copia local de categorias |
| GET `/v1/categorias/{categoriaId}` | 200 disponible · 404 `{"disponible":false}` |
| POST `/v1/solicitudes` `{"titulo","descripcion","categoriaId","prioridad"}` | 201 · 400 si la categoria no esta en la copia local |
| GET `/v1/solicitudes`, GET `/v1/solicitudes/mias?usuario=` | Listas |
| PUT `/v1/solicitudes/{id}/estado` `{"estado"}` | 200 · 400 si se intenta RESUELTA sin EN_PROCESO · 404 |
