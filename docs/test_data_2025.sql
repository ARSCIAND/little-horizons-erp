-- =====================================================================
-- DATOS DE PRUEBA 2025 - EXAMEN: BUSCADOR AVANZADO DE ESTUDIANTES
-- Base: erp_little_horizons (ejecutar DESPUÉS de docs/database.sql)
--
-- Adaptación del script de datos de prueba entregado para el examen:
-- mismos 3 estudiantes de Décimo y mismas notas, con estos ajustes:
--   1. Sin IDs fijos: el script original inserta id_estudiante 1..3,
--      id_materia 1..7, id_profesor 1 e id_matricula 1..3, que en esta base
--      ya existen (datos de docs/database.sql) y generarían errores de
--      llave duplicada. Aquí los IDs los asigna AUTO_INCREMENT y se buscan
--      por documento o por código.
--   2. Grado correcto: el original usa id_grado = 10, que en el script
--      oficial corresponde a 'Noveno'. Aquí se busca el grado por nombre
--      ('Décimo'), que es el id_grado 11.
--   3. Materias: Décimo ya tiene MAT-10, FIS-10, QUI-10, ING-10 y ESP-10;
--      solo se agregan las que faltan (SOC-10 y BIO-10).
--   4. Casos adicionales para probar el buscador y el reporte:
--      - 4 estudiantes en Noveno 2025 (matrícula Activa).
--      - 1 estudiante de Décimo con matrícula 2025 'Retirada'
--        (no debe aparecer en el buscador: caso especial 1).
--      - Once no tiene matrículas activas en 2025 (caso especial 2).
--
-- El script se puede ejecutar varias veces: al inicio borra los datos de
-- prueba anteriores (por número de documento) y los vuelve a crear.
-- =====================================================================

SET NAMES utf8mb4;
USE erp_little_horizons;

-- ---------------------------------------------------------------------
-- 0. Limpieza de una ejecución anterior (ON DELETE CASCADE borra
--    matrículas y notas asociadas)
-- ---------------------------------------------------------------------
DELETE FROM estudiantes WHERE numero_documento IN
    ('1023456789', '1023456790', '1023456791', '1023456792',
     '1023456793', '1023456794', '1023456795', '1023456796');

-- ---------------------------------------------------------------------
-- 1. Grados (búsqueda por nombre, no por ID)
-- ---------------------------------------------------------------------
SET @grado_noveno = (SELECT id_grado FROM grados WHERE nombre_grado = 'Noveno');
SET @grado_decimo = (SELECT id_grado FROM grados WHERE nombre_grado = 'Décimo');

-- ---------------------------------------------------------------------
-- 2. Materias de Décimo que faltan (Ciencias Sociales y Biología)
-- ---------------------------------------------------------------------
INSERT INTO materias (codigo_materia, nombre_materia, descripcion, id_grado, horas_semanales, esta_activa)
SELECT 'SOC-10', 'Ciencias Sociales Décimo', 'Ciencias sociales para grado décimo', @grado_decimo, 3, TRUE
WHERE NOT EXISTS (SELECT 1 FROM materias WHERE codigo_materia = 'SOC-10');

INSERT INTO materias (codigo_materia, nombre_materia, descripcion, id_grado, horas_semanales, esta_activa)
SELECT 'BIO-10', 'Biología Décimo', 'Biología para grado décimo', @grado_decimo, 4, TRUE
WHERE NOT EXISTS (SELECT 1 FROM materias WHERE codigo_materia = 'BIO-10');

-- ---------------------------------------------------------------------
-- 3. Profesor del script de prueba (solo si no existe)
-- ---------------------------------------------------------------------
INSERT INTO profesores (tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido,
       segundo_apellido, correo_electronico, telefono, especializacion, fecha_contratacion, estado)
SELECT 'CC', '80123456', 'Roberto', 'Andrés', 'Martínez', 'Torres', 'rmartinez@littlehorizons.edu',
       '3001234567', 'Matemáticas', '2020-01-15', 'Activo'
WHERE NOT EXISTS (SELECT 1 FROM profesores WHERE numero_documento = '80123456');

SET @profesor_prueba = (SELECT id_profesor FROM profesores WHERE numero_documento = '80123456');

-- ---------------------------------------------------------------------
-- 4. Estudiantes de prueba
-- ---------------------------------------------------------------------
INSERT INTO estudiantes (tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido,
       segundo_apellido, fecha_nacimiento, genero, fecha_ingreso, estado) VALUES
('TI', '1023456789', 'Juan', 'Carlos', 'Pérez', 'González', '2010-03-15', 'M', '2022-01-15', 'Activo'),
('TI', '1023456790', 'María', 'Elena', 'López', 'Ramírez', '2010-07-22', 'F', '2022-01-15', 'Activo'),
('TI', '1023456791', 'Carlos', NULL, 'Gómez', 'Silva', '2010-11-05', 'M', '2022-01-15', 'Activo'),
('TI', '1023456792', 'Laura', 'Camila', 'Suárez', 'Prada', '2010-05-30', 'F', '2022-01-15', 'Retirado'),
('TI', '1023456793', 'Valeria', 'Sofía', 'Castro', 'Ortiz', '2011-02-11', 'F', '2022-01-15', 'Activo'),
('TI', '1023456794', 'Tomás', NULL, 'Rincón', 'Duarte', '2011-08-19', 'M', '2022-01-15', 'Activo'),
('TI', '1023456795', 'Isabella', NULL, 'Quintero', NULL, '2011-04-02', 'F', '2022-01-15', 'Activo'),
('TI', '1023456796', 'Samuel', 'David', 'Peñaranda', 'Vargas', '2011-12-07', 'M', '2022-01-15', 'Activo');

-- ---------------------------------------------------------------------
-- 5. Matrículas 2025
-- ---------------------------------------------------------------------
INSERT INTO matriculas (id_estudiante, id_grado, año_academico, estado_matricula, fecha_matricula, fecha_retiro, observaciones)
SELECT id_estudiante, @grado_decimo, 2025, 'Activa', '2025-01-15', NULL, 'Datos de prueba del examen' FROM estudiantes WHERE numero_documento = '1023456789';
INSERT INTO matriculas (id_estudiante, id_grado, año_academico, estado_matricula, fecha_matricula, fecha_retiro, observaciones)
SELECT id_estudiante, @grado_decimo, 2025, 'Activa', '2025-01-15', NULL, 'Datos de prueba del examen' FROM estudiantes WHERE numero_documento = '1023456790';
INSERT INTO matriculas (id_estudiante, id_grado, año_academico, estado_matricula, fecha_matricula, fecha_retiro, observaciones)
SELECT id_estudiante, @grado_decimo, 2025, 'Activa', '2025-01-15', NULL, 'Datos de prueba del examen' FROM estudiantes WHERE numero_documento = '1023456791';
INSERT INTO matriculas (id_estudiante, id_grado, año_academico, estado_matricula, fecha_matricula, fecha_retiro, observaciones)
SELECT id_estudiante, @grado_decimo, 2025, 'Retirada', '2025-01-15', '2025-06-20', 'Retiro por traslado de ciudad' FROM estudiantes WHERE numero_documento = '1023456792';
INSERT INTO matriculas (id_estudiante, id_grado, año_academico, estado_matricula, fecha_matricula, fecha_retiro, observaciones)
SELECT id_estudiante, @grado_noveno, 2025, 'Activa', '2025-01-15', NULL, 'Datos de prueba del examen' FROM estudiantes WHERE numero_documento = '1023456793';
INSERT INTO matriculas (id_estudiante, id_grado, año_academico, estado_matricula, fecha_matricula, fecha_retiro, observaciones)
SELECT id_estudiante, @grado_noveno, 2025, 'Activa', '2025-01-15', NULL, 'Datos de prueba del examen' FROM estudiantes WHERE numero_documento = '1023456794';
INSERT INTO matriculas (id_estudiante, id_grado, año_academico, estado_matricula, fecha_matricula, fecha_retiro, observaciones)
SELECT id_estudiante, @grado_noveno, 2025, 'Activa', '2025-01-15', NULL, 'Datos de prueba del examen' FROM estudiantes WHERE numero_documento = '1023456795';
INSERT INTO matriculas (id_estudiante, id_grado, año_academico, estado_matricula, fecha_matricula, fecha_retiro, observaciones)
SELECT id_estudiante, @grado_noveno, 2025, 'Activa', '2025-01-15', NULL, 'Datos de prueba del examen' FROM estudiantes WHERE numero_documento = '1023456796';

-- ---------------------------------------------------------------------
-- 6. Notas 2025 (3 períodos cursados: I, II y III)
--    Décimo: mismas notas del script de prueba del examen.
--    Noveno: casos con promedio alto (beca), medio y bajo (refuerzo).
-- ---------------------------------------------------------------------
-- Juan Carlos Pérez González (Décimo)
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 4.8, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'MAT-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 4.5, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'MAT-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 4.7, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'MAT-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 3.5, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'FIS-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 3.8, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'FIS-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 4.0, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'FIS-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 4.2, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'QUI-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 4.0, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'QUI-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 4.5, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'QUI-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 5.0, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'ING-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 4.8, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'ING-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 4.9, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'ING-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 4.3, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'ESP-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 4.5, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'ESP-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 4.4, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'ESP-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 4.0, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'SOC-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 4.2, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'SOC-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 4.1, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'SOC-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 3.8, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'BIO-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 4.0, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'BIO-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 4.2, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456789' AND m.codigo_materia = 'BIO-10';

-- María Elena López Ramírez (Décimo)
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 3.5, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'MAT-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 3.8, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'MAT-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 4.0, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'MAT-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 3.2, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'FIS-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 3.5, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'FIS-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 3.7, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'FIS-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 4.5, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'QUI-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 4.7, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'QUI-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 4.8, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'QUI-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 4.8, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'ING-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 4.9, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'ING-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 5.0, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'ING-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 4.6, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'ESP-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 4.7, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'ESP-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 4.5, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'ESP-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 4.2, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'SOC-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 4.3, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'SOC-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 4.4, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'SOC-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 4.0, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'BIO-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 4.1, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'BIO-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 4.3, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456790' AND m.codigo_materia = 'BIO-10';

-- Carlos Gómez Silva (Décimo, sin segundo nombre)
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 4.0, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'MAT-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 4.2, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'MAT-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 4.3, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'MAT-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 4.5, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'FIS-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 4.6, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'FIS-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 4.7, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'FIS-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 3.8, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'QUI-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 3.9, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'QUI-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 4.0, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'QUI-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 3.5, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'ING-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 3.7, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'ING-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 3.8, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'ING-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 4.1, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'ESP-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 4.3, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'ESP-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 4.2, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'ESP-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 3.9, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'SOC-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 4.0, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'SOC-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 4.1, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'SOC-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 4.4, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'BIO-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 4.5, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'BIO-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'III', 4.6, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456791' AND m.codigo_materia = 'BIO-10';

-- Laura Camila Suárez Prada (Décimo, matrícula RETIRADA: no debe aparecer)
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 3.0, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456792' AND m.codigo_materia = 'MAT-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 2.8, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456792' AND m.codigo_materia = 'MAT-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'I', 3.4, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456792' AND m.codigo_materia = 'ING-10';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, @profesor_prueba, 2025, 'II', 3.1, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456792' AND m.codigo_materia = 'ING-10';

-- Valeria Sofía Castro Ortiz (Noveno)
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'MAT-09' LIMIT 1), 2025, 'I', 4.9, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456793' AND m.codigo_materia = 'MAT-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'MAT-09' LIMIT 1), 2025, 'II', 4.8, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456793' AND m.codigo_materia = 'MAT-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'MAT-09' LIMIT 1), 2025, 'III', 4.7, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456793' AND m.codigo_materia = 'MAT-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ESP-09' LIMIT 1), 2025, 'I', 4.6, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456793' AND m.codigo_materia = 'ESP-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ESP-09' LIMIT 1), 2025, 'II', 4.8, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456793' AND m.codigo_materia = 'ESP-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ESP-09' LIMIT 1), 2025, 'III', 4.9, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456793' AND m.codigo_materia = 'ESP-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'CNA-09' LIMIT 1), 2025, 'I', 4.7, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456793' AND m.codigo_materia = 'CNA-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'CNA-09' LIMIT 1), 2025, 'II', 4.6, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456793' AND m.codigo_materia = 'CNA-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'CNA-09' LIMIT 1), 2025, 'III', 4.8, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456793' AND m.codigo_materia = 'CNA-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'SOC-09' LIMIT 1), 2025, 'I', 4.5, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456793' AND m.codigo_materia = 'SOC-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'SOC-09' LIMIT 1), 2025, 'II', 4.7, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456793' AND m.codigo_materia = 'SOC-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'SOC-09' LIMIT 1), 2025, 'III', 4.6, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456793' AND m.codigo_materia = 'SOC-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ING-09' LIMIT 1), 2025, 'I', 5.0, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456793' AND m.codigo_materia = 'ING-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ING-09' LIMIT 1), 2025, 'II', 4.9, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456793' AND m.codigo_materia = 'ING-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ING-09' LIMIT 1), 2025, 'III', 4.8, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456793' AND m.codigo_materia = 'ING-09';

-- Tomás Rincón Duarte (Noveno, sin segundo nombre)
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'MAT-09' LIMIT 1), 2025, 'I', 2.1, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456794' AND m.codigo_materia = 'MAT-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'MAT-09' LIMIT 1), 2025, 'II', 2.4, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456794' AND m.codigo_materia = 'MAT-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'MAT-09' LIMIT 1), 2025, 'III', 2.6, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456794' AND m.codigo_materia = 'MAT-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ESP-09' LIMIT 1), 2025, 'I', 3.0, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456794' AND m.codigo_materia = 'ESP-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ESP-09' LIMIT 1), 2025, 'II', 2.8, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456794' AND m.codigo_materia = 'ESP-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ESP-09' LIMIT 1), 2025, 'III', 3.1, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456794' AND m.codigo_materia = 'ESP-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'CNA-09' LIMIT 1), 2025, 'I', 2.5, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456794' AND m.codigo_materia = 'CNA-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'CNA-09' LIMIT 1), 2025, 'II', 2.9, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456794' AND m.codigo_materia = 'CNA-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'CNA-09' LIMIT 1), 2025, 'III', 2.7, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456794' AND m.codigo_materia = 'CNA-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'SOC-09' LIMIT 1), 2025, 'I', 3.2, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456794' AND m.codigo_materia = 'SOC-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'SOC-09' LIMIT 1), 2025, 'II', 3.0, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456794' AND m.codigo_materia = 'SOC-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'SOC-09' LIMIT 1), 2025, 'III', 2.9, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456794' AND m.codigo_materia = 'SOC-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ING-09' LIMIT 1), 2025, 'I', 2.8, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456794' AND m.codigo_materia = 'ING-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ING-09' LIMIT 1), 2025, 'II', 2.6, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456794' AND m.codigo_materia = 'ING-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ING-09' LIMIT 1), 2025, 'III', 3.0, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456794' AND m.codigo_materia = 'ING-09';

-- Isabella Quintero (Noveno, sin segundo nombre ni segundo apellido)
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'MAT-09' LIMIT 1), 2025, 'I', 3.6, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456795' AND m.codigo_materia = 'MAT-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'MAT-09' LIMIT 1), 2025, 'II', 3.9, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456795' AND m.codigo_materia = 'MAT-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'MAT-09' LIMIT 1), 2025, 'III', 4.0, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456795' AND m.codigo_materia = 'MAT-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ESP-09' LIMIT 1), 2025, 'I', 4.1, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456795' AND m.codigo_materia = 'ESP-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ESP-09' LIMIT 1), 2025, 'II', 3.8, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456795' AND m.codigo_materia = 'ESP-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ESP-09' LIMIT 1), 2025, 'III', 3.9, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456795' AND m.codigo_materia = 'ESP-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'CNA-09' LIMIT 1), 2025, 'I', 3.7, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456795' AND m.codigo_materia = 'CNA-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'CNA-09' LIMIT 1), 2025, 'II', 3.9, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456795' AND m.codigo_materia = 'CNA-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'CNA-09' LIMIT 1), 2025, 'III', 4.1, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456795' AND m.codigo_materia = 'CNA-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'SOC-09' LIMIT 1), 2025, 'I', 3.8, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456795' AND m.codigo_materia = 'SOC-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'SOC-09' LIMIT 1), 2025, 'II', 3.6, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456795' AND m.codigo_materia = 'SOC-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'SOC-09' LIMIT 1), 2025, 'III', 3.9, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456795' AND m.codigo_materia = 'SOC-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ING-09' LIMIT 1), 2025, 'I', 3.9, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456795' AND m.codigo_materia = 'ING-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ING-09' LIMIT 1), 2025, 'II', 4.0, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456795' AND m.codigo_materia = 'ING-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ING-09' LIMIT 1), 2025, 'III', 3.8, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456795' AND m.codigo_materia = 'ING-09';

-- Samuel David Peñaranda Vargas (Noveno)
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'MAT-09' LIMIT 1), 2025, 'I', 2.6, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456796' AND m.codigo_materia = 'MAT-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'MAT-09' LIMIT 1), 2025, 'II', 2.9, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456796' AND m.codigo_materia = 'MAT-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'MAT-09' LIMIT 1), 2025, 'III', 2.8, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456796' AND m.codigo_materia = 'MAT-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ESP-09' LIMIT 1), 2025, 'I', 3.5, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456796' AND m.codigo_materia = 'ESP-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ESP-09' LIMIT 1), 2025, 'II', 3.6, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456796' AND m.codigo_materia = 'ESP-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ESP-09' LIMIT 1), 2025, 'III', 3.4, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456796' AND m.codigo_materia = 'ESP-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'CNA-09' LIMIT 1), 2025, 'I', 3.3, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456796' AND m.codigo_materia = 'CNA-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'CNA-09' LIMIT 1), 2025, 'II', 3.1, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456796' AND m.codigo_materia = 'CNA-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'CNA-09' LIMIT 1), 2025, 'III', 3.4, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456796' AND m.codigo_materia = 'CNA-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'SOC-09' LIMIT 1), 2025, 'I', 3.6, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456796' AND m.codigo_materia = 'SOC-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'SOC-09' LIMIT 1), 2025, 'II', 3.8, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456796' AND m.codigo_materia = 'SOC-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'SOC-09' LIMIT 1), 2025, 'III', 3.5, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456796' AND m.codigo_materia = 'SOC-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ING-09' LIMIT 1), 2025, 'I', 3.2, '2025-03-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456796' AND m.codigo_materia = 'ING-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ING-09' LIMIT 1), 2025, 'II', 3.4, '2025-06-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456796' AND m.codigo_materia = 'ING-09';
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT e.id_estudiante, m.id_materia, (SELECT a.id_profesor FROM asignaciones_profesor a JOIN materias m2 ON m2.id_materia = a.id_materia WHERE m2.codigo_materia = 'ING-09' LIMIT 1), 2025, 'III', 3.3, '2025-09-15'
  FROM estudiantes e, materias m WHERE e.numero_documento = '1023456796' AND m.codigo_materia = 'ING-09';

-- ---------------------------------------------------------------------
-- 7. Verificación: estudiantes con matrícula ACTIVA en 2025 por grado
--    Resultado esperado: Noveno = 4, Décimo = 3 (Laura está Retirada)
-- ---------------------------------------------------------------------
SELECT g.nombre_grado AS grado, COUNT(*) AS estudiantes_activos_2025
  FROM matriculas m JOIN grados g ON g.id_grado = m.id_grado
 WHERE m.año_academico = 2025 AND m.estado_matricula = 'Activa'
 GROUP BY g.nombre_grado, g.nivel_numerico
 ORDER BY g.nivel_numerico;
