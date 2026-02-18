# Git Workflow & Commit Conventions

## Branch Strategy

| Branch | Purpose | Merge Target |
|--------|---------|-------------|
| `main` | Production-ready code | — |
| `develop` | Integration branch | `main` |
| `feature/<ticket>-<desc>` | New features | `develop` |
| `fix/<ticket>-<desc>` | Bug fixes | `develop` |
| `hotfix/<desc>` | Urgent production fixes | `main` + `develop` |

## Commit Message Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `refactor`: Code restructure without behavior change
- `docs`: Documentation only
- `test`: Adding or updating tests
- `chore`: Build, CI, dependency updates
- `perf`: Performance improvement

### Scope Examples

- `web-ide`, `mcp`, `skills`, `baselines`, `cli`, `docker`

### Examples

```
feat(web-ide): add workspace file write tool for AI delivery

fix(mcp): handle timeout in knowledge search tool

refactor(skills): extract common validation into SkillValidator

docs(adr): add ADR-004 web IDE architecture decision
```

## Pull Request Guidelines

1. PR title follows commit message format
2. Description includes: what changed, why, how to test
3. All baseline checks must pass before merge
4. At least one reviewer approval required
5. Squash merge to keep history clean

## Branch Protection Rules

- `main`: Require PR, 1 approval, passing CI, no force push
- `develop`: Require PR, passing CI
