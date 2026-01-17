# 001: Upgrade Sandestin to v0.4.0

## Status

Complete

## Summary

Upgrade the sandestin peer dependency from v0.3.0 to v0.4.0.

## Current State

Sandestin is declared in two aliases in `deps.edn`:

- `:dev` alias: `io.github.brianium/sandestin {:git/tag "v0.3.0" :git/sha "2be6acc"}`
- `:test` alias: `io.github.brianium/sandestin {:git/tag "v0.3.0" :git/sha "2be6acc"}`

## Target State

Update both locations to:

```clojure
io.github.brianium/sandestin {:git/tag "v0.4.0" :git/sha "7d29c81"}
```

## Tasks

- [x] Update sandestin in `:dev` alias
- [x] Update sandestin in `:test` alias
- [x] Run tests to verify compatibility
