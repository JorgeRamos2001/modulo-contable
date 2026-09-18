-- schema.sql
-- Modulo Contable - Universidad Catolica de El Salvador - Actividad III
-- Adaptado para MySQL (motor InnoDB, requerido para llaves foraneas)

DROP TABLE IF EXISTS detalle_asiento;
DROP TABLE IF EXISTS asiento;
DROP TABLE IF EXISTS cuenta;

-- Catalogo de Cuentas (jerarquia auto-referenciada)
CREATE TABLE cuenta
(
    id              BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo          VARCHAR(10)    NOT NULL UNIQUE,
    nombre          VARCHAR(150)   NOT NULL,
    nivel           INTEGER        NOT NULL, -- 1=General 2=Rubro 3=Mayor 4=Subcuenta
    cuenta_padre_id BIGINT,
    tipo_cuenta     CHAR(1)        NOT NULL, -- primer digito del codigo: 1..7
    saldo_actual    NUMERIC(15, 2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_cuenta_padre FOREIGN KEY (cuenta_padre_id) REFERENCES cuenta (id),
    CONSTRAINT chk_cuenta_tipo CHECK (tipo_cuenta IN ('1', '2', '3', '4', '5', '6', '7')),
    CONSTRAINT chk_cuenta_nivel CHECK (nivel BETWEEN 1 AND 4)
) ENGINE=InnoDB;

CREATE INDEX idx_cuenta_padre ON cuenta (cuenta_padre_id);
CREATE INDEX idx_cuenta_tipo ON cuenta (tipo_cuenta);

-- Libro Diario: cabecera de cada transaccion
CREATE TABLE asiento
(
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero_asiento INTEGER      NOT NULL UNIQUE,
    fecha          DATE         NOT NULL,
    concepto       VARCHAR(255) NOT NULL,
    fecha_creacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Libro Diario: lineas Debe/Haber de cada asiento
CREATE TABLE detalle_asiento
(
    id         BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    asiento_id BIGINT         NOT NULL,
    cuenta_id  BIGINT         NOT NULL,
    concepto   VARCHAR(255),
    debe       NUMERIC(15, 2) NOT NULL DEFAULT 0,
    haber      NUMERIC(15, 2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_detalle_asiento FOREIGN KEY (asiento_id) REFERENCES asiento (id) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuenta (id),
    CONSTRAINT chk_detalle_montos_no_negativos CHECK (debe >= 0 AND haber >= 0),
    CONSTRAINT chk_detalle_una_sola_columna CHECK (NOT (debe > 0 AND haber > 0))
) ENGINE=InnoDB;

CREATE INDEX idx_detalle_asiento_asiento ON detalle_asiento (asiento_id);
CREATE INDEX idx_detalle_asiento_cuenta ON detalle_asiento (cuenta_id);