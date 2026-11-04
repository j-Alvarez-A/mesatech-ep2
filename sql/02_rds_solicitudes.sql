-- =====================================================================
--  RDS SOLICITUDES  (OTRA instancia RDS; ejecutar desde la EC2 B:
--  mysql -h <endpoint-rds-solicitudes> -u admin -p)
-- =====================================================================
CREATE DATABASE IF NOT EXISTS mesatech_solicitudes
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'solicitudes_app'@'%' IDENTIFIED BY 'CAMBIAR_password_solicitudes';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON mesatech_solicitudes.* TO 'solicitudes_app'@'%';
FLUSH PRIVILEGES;

USE mesatech_solicitudes;

-- Copia local de categorías recibidas por RabbitMQ.
-- categoria_id = id ORIGINAL en RDS Catálogo (referencia de origen, única).
CREATE TABLE IF NOT EXISTS categoria_copia (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  categoria_id     BIGINT        NOT NULL,
  nombre           VARCHAR(100)  NOT NULL,
  event_id         VARCHAR(36)   NOT NULL,
  fecha_evento     DATETIME(6)   NULL,
  fecha_recepcion  DATETIME(6)   NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_categoria_copia_origen UNIQUE (categoria_id)
) ENGINE=InnoDB;

-- Solicitudes de soporte (continuidad EP1) asociadas a la copia local.
CREATE TABLE IF NOT EXISTS solicitudes (
  id                   BIGINT        NOT NULL AUTO_INCREMENT,
  titulo               VARCHAR(150)  NOT NULL,
  descripcion          TEXT          NULL,
  categoria_id         BIGINT        NOT NULL,
  categoria_nombre     VARCHAR(100)  NOT NULL,
  prioridad            VARCHAR(30)   NULL,
  usuario_solicitante  VARCHAR(150)  NULL,
  estado               VARCHAR(20)   NOT NULL,
  fecha_creacion       DATETIME(6)   NOT NULL,
  PRIMARY KEY (id),
  KEY idx_solicitudes_categoria (categoria_id)
) ENGINE=InnoDB;

-- Consultas de evidencia
-- SELECT id, categoria_id, nombre, event_id, fecha_recepcion FROM categoria_copia ORDER BY id DESC;
-- SELECT id, titulo, categoria_id, categoria_nombre, estado FROM solicitudes ORDER BY id DESC;
