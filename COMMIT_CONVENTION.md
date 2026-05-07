# Commit Convention

## Branch policy

- `main`: stable production branch
- `baseline/*`: active baseline branch for ongoing verified work
- `pre-release-*` / `release/*`: historical release snapshots
- temporary branches such as `codex/*`, `actions-test-*`, `backup/*`: delete after validation or migration

## Commit message format

Use:

```text
<type>: <summary>
```

Examples:

```text
feat: add backend container delivery workflow
fix: correct flyway migration compatibility on empty databases
chore: remove obsolete test branches from remote
docs: add commit convention for baseline workflow
```

## Allowed types

- `feat`: new feature or user-visible capability
- `fix`: bug fix or regression repair
- `refactor`: structural code change without intended behavior change
- `chore`: tooling, workflow, cleanup, branch governance
- `docs`: documentation only
- `test`: test-only change
- `perf`: performance improvement
- `ci`: CI/CD workflow change

## Rules

- Keep the summary on one line.
- Use the imperative mood.
- Describe the actual change, not the intention.
- Split unrelated changes into separate commits.
- Infrastructure and deployment changes must be committed separately from business logic when possible.

## Baseline workflow

- New ongoing validated work should target the retained `baseline/*` branch instead of creating long-lived ad hoc branches.
- Before deleting a temporary remote branch, make sure any required commits are already preserved in `baseline/*`, `release/*`, or `main`.
