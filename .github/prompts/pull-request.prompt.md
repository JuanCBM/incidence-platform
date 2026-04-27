---
mode: agent
description: Genera el título y la descripción completa de una Pull Request siguiendo el estándar ECI y la crea automáticamente en GitHub, a partir del diff de la rama actual contra main.
tools:
  - terminal
---

# Pull Request — Estándar ECI

## Objetivo

Analizar los cambios de la rama actual respecto a `main`, extraer el contexto necesario, generar el contenido completo de una Pull Request (título + descripción en Markdown) siguiendo el estándar ECI y crearla automáticamente en GitHub con `gh pr create`. No inventar funcionalidades ni afirmar nada que no sea visible en el diff.

---

---
mode: agent
description: Genera el título y la descripción completa de una Pull Request siguiendo el estándar ECI y la crea automáticamente en GitHub, a partir del diff de la rama actual contra main.
tools:
  - terminal
---

# Pull Request — Estándar ECI

## Comportamiento esperado del agente

Este prompt define un flujo **totalmente automatizado**. El agente debe:

- Ejecutar todos los pasos sin pedir confirmación, verificación ni autorización, salvo en los puntos marcados explícitamente con **[INTERACCIÓN]**.
- No añadir pasos propios no descritos en este prompt.
- No verificar, revisar ni validar resultados intermedios salvo que un comando falle con error.
- No preguntar al usuario nada que no esté marcado como **[INTERACCIÓN]**.
- Comunicarse con el usuario únicamente en lenguaje natural y en español. No exponer llamadas a herramientas, bloques JSON, nombres de tools ni logs internos en los mensajes al usuario.
- Esperar la respuesta de cada comando antes de ejecutar el siguiente.

## Objetivo

Analizar los cambios de la rama actual respecto a `main`, extraer el contexto necesario, generar el contenido completo de una Pull Request (título + descripción en Markdown) siguiendo el estándar ECI y crearla automáticamente en GitHub con `gh pr create`. No inventar funcionalidades ni afirmar nada que no sea visible en el diff.

---

## Restricciones de ejecución

- Ejecuta únicamente los comandos git y gh especificados en este prompt, en el orden indicado.
- No leas ni inspecciones archivos adicionales más allá del diff.
- No verifiques ni valides el resultado de los comandos salvo que fallen con error.
- No solicites confirmación para ningún paso salvo los indicados explícitamente.

---

## Paso 1 — Recopilar contexto

Ejecuta los siguientes comandos:

```bash
git branch --show-current
git log origin/main...HEAD --oneline
git diff --stat origin/main...HEAD
git diff origin/main...HEAD
```

---

## Paso 2 — Detectar JIRA-ID y prefijo

1. Busca el **JIRA-ID** (formato `ABC-123`) en:
   - El nombre de la rama actual.
   - Los mensajes de los commits.
   - El contenido del diff (comentarios, nombres de variables, etc.).
2. **[INTERACCIÓN]** Si **no encuentras el JIRA-ID**, pregunta al usuario **solo por el JIRA-ID** antes de continuar. Este es el único punto de interacción posible de este prompt.
3. Detecta el **prefijo** predominante entre los commits:

| Prefijo    | Cuándo usarlo |
|------------|---------------|
| `feat`     | Nueva funcionalidad |
| `fix`      | Corrección de bug |
| `docs`     | Solo documentación |
| `chore`    | Tareas de mantenimiento, dependencias, configs sin impacto en producción |
| `refactor` | Refactorización sin cambio de comportamiento |
| `style`    | Formato, espacios, punto y coma — sin cambio de lógica |
| `test`     | Añadir o corregir tests |
| `perf`     | Mejoras de rendimiento |
| `ci`       | Cambios en CI/CD |
| `build`    | Sistema de build o dependencias externas |
| `revert`   | Revertir un commit anterior |
| `init`     | Commit inicial del proyecto o módulo |
| `wip`      | Trabajo en progreso, no listo para revisión |

---

## Paso 3 — Generar el contenido de la PR

### Título

```
<prefijo>: [<JIRA-ID>] <resumen corto en imperativo>
```

Ejemplo: `feat: [ABC-123] add validation for X in Y`

### Descripción (Markdown)

Genera las siguientes secciones. Sé conciso y profesional. No incluyas secciones vacías; si un apartado no aplica, omítelo.

---

#### 📋 Contexto y objetivo
_2-4 líneas explicando qué problema resuelve este cambio y por qué es necesario._

#### 🔑 Cambios clave
- Bullet por cada cambio relevante para el reviewer.
- Agrupa por áreas si hay muchos cambios.

#### 📦 Alcance / Impacto
- Módulos, servicios o áreas del sistema afectadas.
- Cambios de contrato (API, modelos de datos, configuración) si los hay.

#### 🧪 Cómo probar
Incluye pasos concretos reproducibles:
1. Paso 1
2. Paso 2
3. (comandos de terminal si aplica)

#### ⚠️ Riesgos y mitigaciones
| Riesgo | Mitigación / Rollback |
|--------|-----------------------|
| ...    | ...                   |

_(Omite esta sección si no hay riesgos identificables.)_

#### 🔍 Puntos de atención para el reviewer
- Dónde mirar con más foco.
- Decisiones de diseño que requieran validación explícita.

#### ✅ Checklist de calidad
- [ ] Tests añadidos o actualizados
- [ ] Sin warnings de lint/format
- [ ] CI en verde
- [ ] Documentación actualizada (si aplica)
- [ ] Sin secrets ni datos sensibles en el diff
- [ ] Feature flag o rollback disponible (si aplica)

---

## Paso 4 — Crear la PR en GitHub

Con el título y la descripción generados, ejecuta:

```bash
gh pr create --title "<título>" --body "<descripción en Markdown>" --base main
```

Si el comando falla por cualquier motivo, muestra el título y la descripción generados para que el usuario pueda crear la PR manualmente.
