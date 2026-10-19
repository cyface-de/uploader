# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

The README is the main documentation. It describes the integration, the API, the architecture, the development commands, the workflows and the release:

@README.md

## Rules for Changes

- Keep the README current. When you change the public API, an exception, a command or a workflow, update the README in the same commit (also the code example).
- The Android SDK (`android-backend`) and the phone simulator (`tools`) use this library. Keep the public API compatible, or update them.
- Detekt allows no issues. Run `./gradlew detekt` before each commit.
- Follow the rules of the workflows in the README section "GitHub Workflows" for each change of `.github/workflows/`.

## Commit Style

`[TICKET-ID] Short imperative summary`.
