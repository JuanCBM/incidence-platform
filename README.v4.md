# ✅ Tarea 1 – SPEC‑Driven Development (SDD) con AI‑SPEC externo, OpenSpec y generación de SPECS para IA

## Objetivo
Evaluar e integrar el enfoque **SPEC‑Driven Development (SDD)** utilizando un repositorio de especificaciones **AI‑SPEC** independiente del proyecto, e investigar cómo generar y estructurar dichas specs para que sean **consumibles por GitHub Copilot y Claude**.

Se tomarán como referencias:
- AI‑Specs (LIDR Academy):  
  https://github.com/LIDR-academy/ai-specs
- OpenSpec y su inicialización (`openSpec init`).

## Contexto
- Las especificaciones **no deben vivir dentro del repo del proyecto**.
- `AI-SPEC` será un **repositorio o carpeta externa**, enlazada o referenciada.
- Las specs deben convertirse en la **fuente de verdad funcional y técnica**.
- El objetivo es que tanto **Claude** como **Copilot** usen explícitamente estas specs como contexto.

## Alcance

### 1. SPEC‑Driven Development
- Analizar el enfoque SDD:
    - Qué problema resuelve.
    - Flujo spec‑first frente a code‑first o prompt‑first.
- Estudiar la estructura y principios de `ai-specs`:
    - Tipos de specs.
    - Separación funcional / técnica.
    - Nivel de detalle recomendado para IA.

### 2. OpenSpec
- Investigar **OpenSpec** como herramienta de generación de specs.
- Analizar el uso de:
    - `openSpec init` para generar una spec inicial.
    - Personalización y extensión de la spec generada.
Evaluar:
- Qué estructura de archivos genera.
- Qué tipo de información captura.
- Cómo se adapta a un proyecto existente.
- Probar la integración de OpenSpec en un proyecto real:
- Sin acoplar los ficheros generados al repo principal.
- Generando los archivos en `AI‑SPEC`.

### 3. Generación de SPECS para Claude y Copilot
- Analizar cómo deben estructurarse las specs para:
- **Claude / Claude Code** (ej. `CLAUDE.md`, specs como contexto base).
- **GitHub Copilot** (ej. `copilot-instructions`, specs como documentación de referencia).
- Evaluar:
- Si las specs generadas por OpenSpec son suficientes.
- Qué adaptaciones son necesarias para cada IA.
- Probar casos prácticos:
- Generación de código basada en specs.
- Resolución de dudas funcionales desde IA.
- Consistencia con las definiciones de `AI‑SPEC`.

## Entregables
- Documento Markdown con:
- Explicación del enfoque SDD.
- Estructura propuesta del repositorio `AI‑SPEC`.
- Resultado del uso de `openSpec init`.
- Ejemplos de specs orientadas a Claude y Copilot.
- Evidencias de uso real por cada herramienta de IA.
- Limitaciones y problemas detectados.
- Recomendaciones de adopción.

## Criterios de éxito
- `AI‑SPEC` permanece fuera del proyecto.
- OpenSpec permite generar una base de specs reutilizable.
- Claude y Copilot usan las specs como contexto real.
- El enfoque es repetible y escalable a otros proyectos.