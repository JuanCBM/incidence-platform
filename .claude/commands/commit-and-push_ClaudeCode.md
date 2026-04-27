# Commit & Push — Convención ECI

## Comportamiento esperado del agente

Este prompt define un flujo **totalmente automatizado**. El agente debe:

- Ejecutar todos los pasos sin pedir confirmación, verificación ni autorización, salvo en los puntos marcados explícitamente con **[INTERACCIÓN]**.
- No añadir pasos propios no descritos en este prompt.
- No verificar, revisar ni validar resultados intermedios salvo que un comando falle con error.
- No preguntar al usuario nada que no esté marcado como **[INTERACCIÓN]**.
- Comunicarse con el usuario únicamente en lenguaje natural y en español. No exponer llamadas a herramientas, bloques JSON, nombres de tools ni logs internos en los mensajes al usuario.
- Esperar la respuesta de cada comando antes de ejecutar el siguiente.

## Objetivo

Analizar los cambios del repositorio actual, generar mensajes de commit siguiendo la convención ECI, ejecutar los commits y hacer push al remoto — creando la rama remota si todavía no existe.

---

## Paso 1 — Recopilar contexto

Ejecuta los siguientes comandos en orden, esperando la respuesta de cada uno antes de continuar:

```bash
git status --porcelain
git diff --cached
git branch --show-current
```

Si `git diff --cached` no devuelve nada (no hay cambios staged), ejecuta también:

```bash
git diff origin/$(git branch --show-current)...HEAD
git diff --stat origin/$(git branch --show-current)...HEAD
```

---

## Paso 2 — Analizar el diff

Con la salida anterior:

1. Identifica los cambios: archivos nuevos, modificados, eliminados.
2. Determina si los cambios son **cohesivos** (1 commit) o **separables** (N commits).
   - Separa siempre que haya cambios de naturaleza claramente distinta (ej. docs + código + config).
3. Para cada commit elige el prefijo correcto:

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

**Formato obligatorio del mensaje:**
```
<prefijo>: <descripción breve, clara y específica en infinitivo>
```

---

## Paso 3 — Mostrar el plan antes de ejecutar

Antes de ejecutar, muestra el plan completo en lenguaje natural:

```
CommitPlan:
  1. <prefijo>: <mensaje>
     Archivos: <lista de archivos>
     Motivo: <motivo en una línea>
  2. ...

¿Procedo con los commits y el push? (s/n)
```

**[INTERACCIÓN]** Espera confirmación del usuario antes de continuar. Este es el único punto de interacción de este prompt.

---

## Paso 4 — Ejecutar commits

Por cada commit del plan, en orden:

```bash
git add <archivos del commit>
git commit -m "<prefijo>: <mensaje>"
```

---

## Paso 5 — Push al remoto

Obtén la rama local actual:

```bash
git branch --show-current
```

Comprueba si la rama ya existe en el remoto:

```bash
git ls-remote --exit-code origin <rama>
```

- **Si la rama ya existe en remoto** (exit code 0):
  ```bash
  git push origin <rama>
  ```

- **Si la rama NO existe en remoto** (exit code distinto de 0):
  ```bash
  git push --set-upstream origin <rama>
  ```

---

## Criterios de calidad

- Mensajes cortos, sin verbos en pasado ni relleno ("updated", "changed", "some fixes"...).
- Usa el imperativo en la descripción: "add", "fix", "remove", "update".
- Un commit = una responsabilidad.
- Si hay dudas sobre el alcance, prefiere varios commits pequeños a uno grande.
