# ascolais/manse

## Project Overview

This is a Clojure project using deps.edn for dependency management.

## Technology Stack

- **Clojure** with deps.edn
- **clj-reload** for namespace reloading during development
- **Portal** for data inspection (tap> integration)
- **Cognitect test-runner** for running tests

## Development Setup

### Starting the REPL

```bash
clj -M:dev
```

This starts a REPL with development dependencies loaded.

### Development Workflow

1. Start REPL with `clj -M:dev`
2. Load dev namespace: `(dev)`
3. Start the system: `(start)`
4. Make changes to source files
5. Reload: `(reload)`

The `dev` namespace provides:
- `(start)` - Start the development system
- `(stop)` - Stop the system
- `(reload)` - Reload changed namespaces via clj-reload
- `(restart)` - Stop, reload, and start

### Portal

Portal opens automatically when the dev namespace loads. Any `(tap> data)` calls will appear in the Portal UI.

## Project Structure

```
src/clj/          # Clojure source files
dev/src/clj/      # Development-only source (user.clj, dev.clj)
test/src/clj/     # Test files
resources/        # Resource files
```

## REPL Evaluation

Use the clojure-eval skill to evaluate code via nREPL:

```bash
clj-nrepl-eval --discover-ports          # Find running REPLs
clj-nrepl-eval -p <PORT> "(+ 1 2 3)"     # Evaluate expression
```

To reload changed namespaces, use `(dev/reload)` from the dev namespace:

```bash
clj-nrepl-eval -p <PORT> "(dev/reload)"
```

## Running Tests

```bash
clj -X:test
```

Or from the REPL:

```clojure
(require '[clojure.test :refer [run-tests]])
(require '[ascolais.manse-test] :reload)
(run-tests 'ascolais.manse-test)
```

## Adding Dependencies

When adding new dependencies in a REPL-connected environment:

1. **Add to the running REPL first** using `clojure.repl.deps/add-lib`:
   ```clojure
   (clojure.repl.deps/add-lib 'metosin/malli {:mvn/version "0.16.4"})
   ```
   Note: The library name must be quoted.

2. **Confirm the dependency works** by requiring and testing it in the REPL.

3. **Only then add to deps.edn** once confirmed working.

This ensures dependencies are immediately available without restarting the REPL.

## Code Style

- Follow standard Clojure conventions
- Use `cljfmt` formatting (applied automatically via hooks)
- Prefer pure functions where possible
- Use `tap>` for debugging output (appears in Portal)

## Git Commits

Use conventional commits format:

```
<type>: <description>

[optional body]
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

Examples:
- `feat: add user authentication`
- `fix: resolve nil pointer in data parser`
- `refactor: simplify database connection logic`

## Sandestin Usage

Sandestin is the effect dispatch library used by Manse. Key concepts:

### Creating a Dispatch Function

`create-dispatch` takes a **sequence** of registries (not a single registry):

```clojure
(require '[ascolais.sandestin :as s])
(require '[ascolais.manse :as manse])

;; Create a registry
(def reg (manse/registry {:datasource ds}))

;; Create dispatch - note the vector wrapping the registry
(def dispatch (s/create-dispatch [reg]))
```

### Discoverability Functions

These functions operate on the **dispatch function**, not the registry:

```clojure
;; Describe all registered items
(s/describe dispatch)

;; Describe a specific effect/placeholder
(s/describe dispatch ::manse/execute)

;; Describe by type
(s/describe dispatch :effects)
(s/describe dispatch :placeholders)

;; Search by text in descriptions
(s/grep dispatch "transaction")

;; Generate sample data from schema
(s/sample dispatch ::manse/execute)
```

### Registry Structure

Registries are maps with `::s/effects` and `::s/placeholders` keys:

```clojure
{::s/effects      {::key effect-definition ...}
 ::s/placeholders {::key placeholder-definition ...}}
```

Each effect/placeholder definition includes:
- `::s/description` - human-readable description
- `::s/schema` - Malli schema for validation
- `::s/handler` - the handler function
