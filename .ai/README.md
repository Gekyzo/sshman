# sshman AI Context

This directory contains AI-specific documentation and context for Claude Code to better understand and assist with the sshman project.

## 📁 Directory Contents

### `suggested-skills.md`
Comprehensive analysis and suggestions for project-specific Claude Code skills. This document was created by analyzing the sshman codebase to identify common workflows and pain points.

**Contains:**
- 10 suggested skills with detailed implementation approaches
- Priority rankings (High/Medium/Low)
- Trigger scenarios and usage examples
- Tool requirements and code templates
- Common patterns and conventions
- Usage frequency estimates

### Skills Implementation
All suggested skills have been implemented and are available in `.claude/skills/`. See `.claude/skills/README.md` for details.

## 🎯 Purpose

This directory serves as a knowledge base for AI assistants working on sshman. It helps Claude Code:

1. **Understand project structure** - How the codebase is organized
2. **Follow conventions** - Coding patterns, naming, and standards
3. **Automate workflows** - Common development tasks
4. **Provide context** - Project-specific knowledge

## 📚 Project Context

### Technology Stack
- **Language:** Java 21
- **Build Tool:** Maven
- **CLI Framework:** picocli 4.7.5
- **JSON:** GSON 2.10.1
- **Testing:** JUnit 5
- **CI/CD:** GitHub Actions

### Key Files
- `pom.xml` - Maven configuration, version at line 10
- `CHANGELOG.md` - Keep a Changelog format
- `ROADMAP.md` - Planned features
- `README.md` - Main documentation
- `src/main/java/com/sshman/` - Command implementations
- `src/test/java/com/sshman/` - Test files
- `.github/workflows/release.yml` - Release automation

### Project Structure
```
sshman/
├── .ai/                    # AI context (this directory)
├── .claude/                # Claude Code configuration
│   └── skills/            # Custom skills
├── .github/               # GitHub Actions workflows
├── completions/           # Shell completion scripts
├── docs/                  # GitHub Pages documentation
├── src/
│   ├── main/java/com/sshman/  # Command classes
│   └── test/java/com/sshman/  # Test classes
├── pom.xml                # Maven build file
├── CHANGELOG.md           # Release notes
├── ROADMAP.md            # Future plans
└── README.md             # Documentation
```

### Development Workflow

**Typical workflow:**
1. Create feature branch
2. Implement command (use `new-command` skill)
3. Write tests (JUnit 5 pattern)
4. Run tests (use `test-runner` skill)
5. Update documentation
6. Add changelog entry (use `changelog-entry` skill)
7. Regenerate completions (use `completion-regen` skill)
8. Commit and push
9. Create release (use `release` skill)

### Conventions

**Java Classes:**
- Command classes: `*Command.java` in `src/main/java/com/sshman/`
- Implement `Callable<Integer>`
- Use picocli annotations: `@Command`, `@Option`, `@Parameters`
- Return 0 for success, 1 for errors
- Use `@Spec private CommandSpec spec` for output streams

**Tests:**
- Test classes: `*CommandTest.java` in `src/test/java/com/sshman/`
- Use JUnit 5 (`@Test` annotations)
- Test with `CommandLine cmd = new CommandLine(new SshMan())`
- Capture output with `StringWriter`

**Versioning:**
- Semantic versioning (MAJOR.MINOR.PATCH)
- Version stored in `pom.xml` line 10
- Update CHANGELOG.md with each release
- Git tags: `vX.X.X` (e.g., `v0.1.1`)

**Git Commits:**
- Conventional commits format
- Prefixes: `feat:`, `fix:`, `docs:`, `chore:`, `test:`
- Keep commits atomic and focused

**Documentation:**
- Keep README.md updated
- Document all commands in README
- Update docs/ for GitHub Pages
- Add examples for new features

## 🔄 Keeping Context Updated

When making significant changes to the project:

1. **Update this README** if structure changes
2. **Update suggested-skills.md** if new workflows emerge
3. **Add new skills** to `.claude/skills/` as needed
4. **Document patterns** that should be followed

## 🎓 Learning from the Codebase

When Claude Code needs to understand how to implement features:

### Command Implementation Pattern
See `src/main/java/com/sshman/GenerateCommand.java` for a complete example of:
- picocli annotations
- Option handling
- Error handling
- Process execution (ssh-keygen)
- Output formatting

### Test Pattern
See `src/test/java/com/sshman/GenerateCommandTest.java` for:
- Testing commands with picocli
- Capturing stdout/stderr
- Testing help output
- Testing error cases

### Profile Storage Pattern
See `src/main/java/com/sshman/ProfileStorage.java` for:
- JSON file handling with GSON
- CRUD operations
- Config directory management

## 🚀 Quick Start for AI

When starting work on sshman:

1. **Check version:** Read pom.xml line 10
2. **Check recent changes:** Read CHANGELOG.md [Unreleased] section
3. **Check current work:** Read ROADMAP.md for planned features
4. **Review tests:** Run `mvn test` to ensure baseline

## 📊 Skill Usage Recommendations

**For daily development:**
- `version-bump` - Quick version updates
- `test-runner` - Running tests
- `changelog-entry` - Documenting changes
- `build-test` - Quick verification

**For feature development:**
- `new-command` - Generating boilerplate
- `test-runner` - Testing implementation
- `completion-regen` - Updating completions
- `docs-sync` - Updating documentation

**For releases:**
- `release` - Complete release process
- `build-test` - Final verification

**For maintenance:**
- `profile-manager` - Managing user profiles
- `config-analyzer` - Analyzing configurations

## 🔗 External Resources

- [picocli Documentation](https://picocli.info/)
- [Keep a Changelog](https://keepachangelog.com/)
- [Semantic Versioning](https://semver.org/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Maven Documentation](https://maven.apache.org/guides/)

## 📝 Notes for AI

**Important project-specific knowledge:**

1. **Version location:** Always read from pom.xml line 10, not from hardcoded values
2. **SSH directory:** User's SSH keys are in `~/.ssh/`, profiles in `~/.sshman/`
3. **Auto-switching:** `.sshman` files in project directories enable automatic key loading
4. **Completions:** Both bash and zsh, bash is auto-generated, zsh is manual
5. **Release process:** Tag-based, GitHub Actions handles the build and release
6. **Profile format:** JSON with alias, hostname, username, port, sshKey fields

**Common pitfalls to avoid:**

1. Don't hardcode version numbers - read from pom.xml
2. Don't skip updating CHANGELOG.md
3. Don't forget to regenerate completions after command changes
4. Don't break the Keep a Changelog format in CHANGELOG.md
5. Don't create releases without updating documentation

**Quality checks before release:**

- [ ] All tests pass (`mvn test`)
- [ ] Version updated in pom.xml
- [ ] CHANGELOG.md updated with changes
- [ ] README.md reflects new features
- [ ] Completions regenerated if needed
- [ ] Build succeeds (`mvn clean package`)
- [ ] Smoke tests pass (--version, --help work)

## 🤝 Contributing to AI Context

If you discover new patterns or workflows that should be documented:

1. Add them to this README
2. Consider creating a new skill in `.claude/skills/`
3. Update `suggested-skills.md` if the skill was anticipated
4. Keep documentation concise but complete

---

**Last Updated:** 2024-12-31
**Project Version:** 0.1.1
**Skills Implemented:** 10/10
**Purpose:** AI context and knowledge base for sshman development
