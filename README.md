# MesaTech Cloud – EP2 (DSY1107 Desarrollo Cloud Native I)

Evolucion de MesaTech Cloud: **Catalogo** (publicador) y **Solicitudes** (consumidor) en EC2 distintas,
cada uno con su **propia instancia Amazon RDS MySQL**, integrados por **RabbitMQ** instalado desde cero
con Docker Compose en una tercera EC2. Se conservan React + MSAL, Entra ID, API Gateway y el BFF de la EP1.

> Version entregada: commit `<COMPLETAR hash>` · rama `main`

## Estructura
| Carpeta | Contenido |
|---|---|
| `frontend-react/` | React 19 + MSAL (Entra ID). Solo llama al API Gateway |
| `bff/` | BFF Spring Boot: valida JWT, autoriza por rol y delega en los microservicios |
| `ms-catalogo/` | Microservicio Catalogo + publicador RabbitMQ (RDS Catalogo) |
| `ms-solicitudes/` | Microservicio Solicitudes + consumidor RabbitMQ (RDS Solicitudes) |
| `rabbitmq/` | `compose.yaml`, `.env.example`, definiciones y script de usuarios |
| `sql/` | Scripts de creacion de tablas y usuarios de aplicacion de cada RDS |
| `docs/` | Contrato del evento y endpoints |

## Versiones
| Componente | Version |
|---|---|
| Java (EC2 A y B) | OpenJDK 21 (`openjdk-21-jre-headless`, Ubuntu 24.04) |
| Spring Boot / Spring AMQP | 4.1.1 / 4.x (Jackson 3, `JacksonJsonMessageConverter`) |
| RabbitMQ / Erlang | 4.1.8 / 27.3.4.19 (`rabbitmq:4.1-management`) |
| Docker Engine / Compose | 29.9.2 / v5.6.1 |
| SO de las EC2 | Ubuntu 24.04 LTS |
| Amazon RDS | MySQL `<COMPLETAR version>` · driver `mysql-connector-j` · puerto 3306 |
| React / Node (compilacion) | React 19 · Node 22+ |

## Inventario
| Componente | Ubicacion | IP privada | Proceso |
|---|---|---|---|
| EC2 A `mesatech-ec2-a-catalogo` | `i-0de9da7062aed8b2d` · IP elastica 44.196.115.142 | 172.31.17.120 | `ms-catalogo` (8082) + `bff` (8083) |
| EC2 B `mesatech-ec2-b-solicitudes` | `i-009f77527fedf8667` | 172.31.29.173 | `ms-solicitudes` (8081) |
| EC2 C `mesatech-ec2-c-rabbitmq` | `i-06db865fec6a32fb5` | 172.31.25.87 | contenedor `mesatech-rabbitmq` (5672/15672) |
| RDS Catalogo | `mesatech-rds-catalogo` · base `catalogo_db` | — | usuario de aplicacion `catalogo_app` |
| RDS Solicitudes | `mesatech-rds-solicitudes` · base `solicitudes_db` | — | usuario de aplicacion `solicitudes_app` |

## Puertos y origenes autorizados (security groups)
| Destino | Puerto | Origen | Uso |
|---|---|---|---|
| EC2 C (`mesatech-rabbitmq-sg`) | 5672 | `mesatech-catalogo-sg`, `mesatech-solicitudes-sg` | AMQP por red privada |
| EC2 C | 15672 | IP administrativa /32 | Panel de RabbitMQ |
| RDS Catalogo (`mesatech-rds-catalogo-sg`) | 3306 | `mesatech-catalogo-sg` | Solo Catalogo |
| RDS Solicitudes (`mesatech-rds-solicitudes-sg`) | 3306 | `mesatech-solicitudes-sg` | Solo Solicitudes |
| EC2 B (`mesatech-solicitudes-sg`) | 8081 | `mesatech-catalogo-sg` | BFF → Solicitudes |
| EC2 A (`mesatech-catalogo-sg`) | 8083 | `0.0.0.0/0` | Integracion HTTP del API Gateway (sin IP de origen fija); el BFF valida JWT y rol |
| EC2 A, B, C | 22 | IP administrativa /32 | SSH |

RDS sin acceso publico. Catalogo (8082) no se expone: el BFF lo llama por `localhost`. UFW inactivo en las tres EC2; el control de acceso lo hacen los security groups.

## Configuracion (variables de entorno)
Las credenciales se leen de archivos `.env` en cada EC2 (`chmod 600`), cargados por systemd (`EnvironmentFile=`). En el repositorio solo hay `*.env.example` con valores ficticios.

| Archivo en la EC2 | Plantilla |
|---|---|
| EC2 A `~/mesatech/catalogo.env` | `ms-catalogo/catalogo.env.example` |
| EC2 A `~/mesatech/bff.env` | `bff/bff.env.example` |
| EC2 B `~/mesatech/solicitudes.env` | `ms-solicitudes/solicitudes.env.example` |
| EC2 C `~/mesatech-rabbitmq/.env` | `rabbitmq/.env.example` |
| PC `frontend-react/.env` | `frontend-react/.env.example` (`REACT_APP_API_URL`) |

## Compilacion
```bash
# en cada proyecto Java
./mvnw clean package -DskipTests        # Windows: .\mvnw.cmd clean package -DskipTests
# React
cd frontend-react && npm install && npm start      # o: npm run build
```

## Despliegue (resumen)
1. **RDS**: crear las dos instancias (sin acceso publico) y ejecutar `sql/01_rds_catalogo.sql` desde la EC2 A y `sql/02_rds_solicitudes.sql` desde la EC2 B.
2. **RabbitMQ**: ver `rabbitmq/README.md`.
3. **EC2 A y B**: `sudo apt-get install -y openjdk-21-jre-headless`; copiar el JAR a `~/mesatech/`, crear el `.env` y el servicio:
   ```bash
   sudo cp <servicio>.service /etc/systemd/system/      # deploy/ de cada proyecto
   sudo systemctl daemon-reload && sudo systemctl enable --now <servicio>
   journalctl -u <servicio> -f
   ```
   Servicios: `ms-catalogo` y `bff` (EC2 A), `ms-solicitudes` (EC2 B).
4. **API Gateway**: integracion `http://<IP elastica EC2 A>:8083/{proxy}` para `ANY /{proxy+}` y `OPTIONS /{proxy+}`.
5. **React**: `REACT_APP_API_URL=https://<api-gateway>/api`; redirect URI registrada en Entra ID.

## Orden de arranque y recuperacion
1. RDS Catalogo y RDS Solicitudes en estado *Disponible*.
2. RabbitMQ (EC2 C): `docker compose up -d` → `(healthy)`.
3. Solicitudes (EC2 B): `sudo systemctl start ms-solicitudes` – consume lo pendiente en la cola.
4. Catalogo y BFF (EC2 A): `sudo systemctl start ms-catalogo bff`.
5. Verificar la integracion del API Gateway y abrir React.

**Detencion**: en orden inverso (React → BFF/Catalogo → Solicitudes → RabbitMQ con `docker compose stop` → RDS).
Los servicios estan habilitados en systemd y el contenedor tiene `restart: unless-stopped`, por lo que vuelven solos tras reiniciar una EC2.
Si el broker se detiene, Catalogo responde 502 al crear categorias (quedan guardadas sin evento); si Solicitudes se detiene, los mensajes esperan en la cola durable.

## Pruebas
P1–P8 documentadas en el informe tecnico (preparacion, datos, esperado, obtenido y evidencia).
