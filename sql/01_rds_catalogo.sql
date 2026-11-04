-- =====================================================================
--  RDS CATÁLOGO  (ejecutar conectado a la instancia RDS de Catálogo,
--  desde la EC2 A:  mysql -h <endpoint-rds-catalogo> -u admin -p)
-- =====================================================================
CREATE DATABASE IF NOT EXISTS mesatech_catalogo
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Usuario de aplicación (no usar el usuario maestro de RDS en la app).
-- '%' porque RDS solo es alcanzable desde el security group de EC2 A.
CREATE USER IF NOT EXISTS 'catalogo_app'@'%' IDENTIFIED BY 'CAMBIAR_password_catalogo';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON mesatech_catalogo.* TO 'catalogo_app'@'%';
FLUSH PRIVILEGES;

USE mesatech_catalogo;

-- Equivale a lo que genera JPA (ddl-auto=update) para la entidad Categoria.
CREATE TABLE IF NOT EXISTS categoria (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  nombre          VARCHAR(100)  NOT NULL,
  descripcion     VARCHAR(255)  NULL,
  activa          BIT(1)        NOT NULL,
  fecha_creacion  DATETIME(6)   NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_categoria_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

-- Consultas de evidencia (P2, P3, P6, P7)
-- SELECT id AS categoriaId, nombre, fecha_creacion FROM categoria ORDER BY id DESC;
