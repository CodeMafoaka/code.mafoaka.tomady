# AI Rules & Guidelines

Welcome, AI Agent! Please adhere strictly to the following rules and guidelines when working on this repository:

## Commit and Pull Request (PR) Conventions

1. **Always Use Conventional Commits**
   - Commit messages must follow the [Conventional Commits](https://www.conventionalcommits.org/) specification (e.g., `feat: ...`, `fix: ...`, `docs: ...`, `chore: ...`, `test: ...`, `refactor: ...`).
   - The subject line must be concise (max 50 chars) and written in imperative present tense.

2. **Separate Commits for Separate Features**
   - Do not group unrelated features or multiple bug fixes into a single massive commit.
   - Keep commits granular and logical to maintain clean, easy-to-read, and revertible git histories.

3. **PR Names Must Follow Conventional Commits**
   - Ensure that the Pull Request title matches the Conventional Commits specification.

## Core Architectural Rules

- Read and follow the instructions in `doc/SOUSPAPE.md` to understand the architecture, database separations (Diet vs FooDB Full/Local), and compilation guidelines.
- Prioritize compile-time correctness and robustness.
- Stubs are currently used for Android-specific APIs (e.g., Room, React Native, WorkManager) to enable seamless local compilation using standard JDK offline. Do not break this lightweight stub mechanism unless explicitly instructed to integrate the full Android Gradle Plugin (AGP).
- Always verify your work by running `./gradlew clean test` and `./gradlew compileJava` before submitting changes.
