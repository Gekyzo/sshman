# Suggested Claude Code Skills for sshman

This document outlines project-specific Claude Code skills that would streamline common development workflows for the sshman SSH management CLI tool.

## Priority Ranking Key
- **High**: Frequently used workflows that provide significant time savings
- **Medium**: Useful workflows for common but less frequent tasks
- **Low**: Nice-to-have automations for occasional tasks

---

## High Priority Skills

### 1. sshman-release

**Purpose**: Automate the complete release process for sshman, including version bumping, changelog updates, git tagging, and triggering GitHub Actions.

**Trigger Scenarios**:
- User says: "Create a new release"
- User says: "Publish version X.X.X"
- User says: "Cut a release"
- User mentions: "release", "publish", "new version"

**Implementation Approach**:

1. **Ask user for release type**:
   - Major (X.0.0) - Breaking changes
   - Minor (0.X.0) - New features, backward compatible
   - Patch (0.0.X) - Bug fixes

2. **Version bump in pom.xml**:
   - Read current version from `pom.xml` line 10
   - Calculate new version based on release type
   - Update `<version>` tag in pom.xml

3. **Update CHANGELOG.md**:
   - Move items from `## [Unreleased]` section to new version section
   - Format: `## [X.X.X] - YYYY-MM-DD`
   - Add link at bottom: `[X.X.X]: https://github.com/Gekyzo/sshman/releases/tag/vX.X.X`
   - Prompt user to review/edit changelog entries

4. **Commit and tag**:
   - `git add pom.xml CHANGELOG.md`
   - `git commit -m "chore: bump version to X.X.X"`
   - `git tag -a vX.X.X -m "Release vX.X.X"`

5. **Ask for confirmation before pushing**:
   - Show git log and tag
   - Confirm: "Ready to push tag vX.X.X to trigger release?"

6. **Push to trigger GitHub Actions**:
   - `git push origin main`
   - `git push origin vX.X.X`
   - GitHub Actions workflow (.github/workflows/release.yml) will build and create release

**Tools Needed**: Read, Edit, Bash, AskUserQuestion

**Example Usage**:
```
User: "Create a patch release"
Skill: [Analyzes current version 0.1.1, suggests 0.1.2, updates files, creates tag]
```

**Priority**: **High** - Core workflow for publishing new versions

---

### 2. sshman-profile-manager

**Purpose**: Interactive management of SSH connection profiles stored in `~/.sshman/profiles.json`, including list, edit, and delete operations.

**Trigger Scenarios**:
- User says: "Show me my SSH profiles"
- User says: "Delete the profile named X"
- User says: "Edit profile X"
- User mentions: "connection profiles", "manage profiles"

**Implementation Approach**:

1. **List profiles**:
   - Read `~/.sshman/profiles.json`
   - Parse JSON (Gson format from ProfileStorage.java)
   - Display in table format:
     ```
     ALIAS          HOSTNAME              USERNAME    PORT    SSH KEY
     ----------------------------------------------------------------------
     production     prod.example.com      deploy      22      ~/.ssh/work/id_work_ed25519
     staging        staging.example.com   ubuntu      2222    ~/.ssh/personal/id_ed25519
     ```

2. **Edit profile**:
   - Load profiles from JSON
   - Find profile by alias
   - Present current values and prompt for changes
   - Update JSON file with new values
   - Validate hostname, port, SSH key path

3. **Delete profile**:
   - Load profiles from JSON
   - Find profile by alias
   - Confirm deletion
   - Remove from array and save JSON

4. **Export/Import profiles**:
   - Export to backup file
   - Import from backup file

**Tools Needed**: Read, Write, Bash, AskUserQuestion

**Example Usage**:
```
User: "List my connection profiles"
Skill: [Reads ~/.sshman/profiles.json, displays formatted table]

User: "Delete the production profile"
Skill: [Finds profile, confirms, removes from JSON]
```

**Priority**: **High** - Profile management is currently missing edit/delete/list functionality

---

### 3. sshman-version-bump

**Purpose**: Semantic version bumping across pom.xml and CHANGELOG.md without doing a full release.

**Trigger Scenarios**:
- User says: "Bump version to X.X.X"
- User says: "Update version number"
- User mentions: "version bump", "increment version"

**Implementation Approach**:

1. **Parse current version**:
   - Read pom.xml line 10: `<version>X.X.X</version>`
   - Extract version number

2. **Calculate new version**:
   - Ask user: Major, Minor, or Patch?
   - Or accept specific version number

3. **Update pom.xml**:
   - Replace version in pom.xml

4. **Update CHANGELOG.md**:
   - Add new unreleased section or version section
   - Update version links at bottom

5. **Commit changes**:
   - `git add pom.xml CHANGELOG.md`
   - `git commit -m "chore: bump version to X.X.X"`

**Tools Needed**: Read, Edit, Bash, AskUserQuestion

**Example Usage**:
```
User: "Bump to version 0.2.0"
Skill: [Updates pom.xml, CHANGELOG.md, commits]
```

**Priority**: **High** - Commonly needed for development versioning

---

## Medium Priority Skills

### 4. sshman-new-command

**Purpose**: Generate boilerplate code for a new sshman command, including Java class, test file, and documentation stubs.

**Trigger Scenarios**:
- User says: "Create a new command called X"
- User says: "Add a command for X"
- User mentions: "new command", "add subcommand"

**Implementation Approach**:

1. **Prompt for command details**:
   - Command name (e.g., "list-profiles")
   - Description
   - Required/optional parameters

2. **Generate Command class** (src/main/java/com/sshman/XCommand.java):
   ```java
   package com.sshman;

   import picocli.CommandLine.Command;
   import picocli.CommandLine.Model.CommandSpec;
   import picocli.CommandLine.Spec;
   import java.util.concurrent.Callable;

   @Command(
       name = "command-name",
       description = "Description here",
       mixinStandardHelpOptions = true
   )
   public class XCommand implements Callable<Integer> {
       @Spec
       private CommandSpec spec;

       @Override
       public Integer call() {
           // Implementation here
           return 0;
       }
   }
   ```

3. **Generate Test class** (src/test/java/com/sshman/XCommandTest.java):
   ```java
   package com.sshman;

   import org.junit.jupiter.api.Test;
   import picocli.CommandLine;
   import java.io.PrintWriter;
   import java.io.StringWriter;
   import static org.junit.jupiter.api.Assertions.*;

   class XCommandTest {
       @Test
       void testCommandHelp() {
           // Boilerplate test
       }
   }
   ```

4. **Register command in SshMan.java**:
   - Add XCommand.class to subcommands array

5. **Add documentation stub to README.md**:
   - Add command to usage section

6. **Suggest next steps**:
   - Implement the `call()` method
   - Add integration tests
   - Regenerate completion scripts

**Tools Needed**: Read, Write, Edit, Glob

**Example Usage**:
```
User: "Create a new command to list all profiles"
Skill: [Generates ListProfilesCommand.java, test, updates SshMan.java]
```

**Priority**: **Medium** - Useful for adding new features, not used daily

---

### 5. sshman-test-runner

**Purpose**: Run specific test suites or all tests with coverage reporting and result formatting.

**Trigger Scenarios**:
- User says: "Run tests"
- User says: "Test the GenerateCommand"
- User says: "Run all tests with coverage"
- User mentions: "mvn test", "junit"

**Implementation Approach**:

1. **Determine test scope**:
   - All tests: `mvn test`
   - Specific test: `mvn test -Dtest=GenerateCommandTest`
   - With coverage: `mvn clean test jacoco:report`

2. **Execute tests**:
   - Run appropriate Maven command
   - Capture output

3. **Parse results**:
   - Extract test counts (passed, failed, skipped)
   - Identify failed tests
   - Show coverage percentage if coverage was run

4. **Format output**:
   ```
   Test Results:
   ✓ GenerateCommandTest: 5 passed
   ✓ ListCommandTest: 3 passed
   ✗ InfoCommandTest: 1 failed

   Total: 8/9 passed (88.9%)

   Failed tests:
   - InfoCommandTest.testInvalidKeyPath: AssertionError at line 42
   ```

5. **Show coverage report location**:
   - If coverage was run: `target/site/jacoco/index.html`

**Tools Needed**: Bash, Read

**Example Usage**:
```
User: "Run tests for GenerateCommand"
Skill: [Executes mvn test -Dtest=GenerateCommandTest, formats results]
```

**Priority**: **Medium** - Useful but developers often run tests directly

---

### 6. sshman-completion-regen

**Purpose**: Regenerate shell completion scripts after modifying commands or adding new options.

**Trigger Scenarios**:
- User says: "Regenerate completion scripts"
- User says: "Update shell completions"
- After modifying a command, suggest: "Should I regenerate completions?"

**Implementation Approach**:

1. **Check if JAR is built**:
   - Look for `target/sshman-*-jar-with-dependencies.jar`
   - If not found, run `mvn clean package`

2. **Extract version from pom.xml**:
   - Read version for JAR filename

3. **Regenerate bash completion** (if picocli AutoComplete is available):
   ```bash
   java -cp target/sshman-0.1.1-jar-with-dependencies.jar \
     picocli.AutoComplete com.sshman.SshMan \
     -n sshman -o completions/sshman_completion.bash -f
   ```

4. **Update zsh completion**:
   - Read command list from SshMan.java
   - Update completions/_sshman with new commands/options
   - Or prompt user to update manually

5. **Test completions**:
   - Suggest: "Test with: source completions/sshman_completion.bash"

**Tools Needed**: Bash, Read, Edit

**Example Usage**:
```
User: "Regenerate completions"
Skill: [Builds JAR, runs picocli AutoComplete, updates completion files]
```

**Priority**: **Medium** - Needed after command changes, not frequently

---

### 7. sshman-docs-sync

**Purpose**: Synchronize documentation in docs/ directory with README.md and command changes.

**Trigger Scenarios**:
- User says: "Update the documentation site"
- User says: "Sync docs with README"
- After updating README, suggest: "Should I update the docs site?"

**Implementation Approach**:

1. **Identify changes**:
   - Compare README.md with docs/index.html
   - Check for new commands in SshMan.java

2. **Update docs/index.html**:
   - Convert relevant README sections to HTML
   - Update feature lists
   - Update examples

3. **Update docs/usage.html**:
   - Extract usage examples from README
   - Format as HTML

4. **Update docs/examples.html**:
   - Extract examples from README
   - Add new command examples

5. **Commit changes**:
   - `git add docs/`
   - `git commit -m "docs: sync documentation with README"`

**Tools Needed**: Read, Write, Edit, Bash

**Example Usage**:
```
User: "Update the docs site with latest changes"
Skill: [Syncs README content to docs/, commits changes]
```

**Priority**: **Medium** - Important for maintaining docs, but not daily use

---

## Low Priority Skills

### 8. sshman-build-test

**Purpose**: Quick build and smoke test to verify the project builds and runs.

**Trigger Scenarios**:
- User says: "Build and test"
- User says: "Make sure everything works"
- User mentions: "smoke test", "sanity check"

**Implementation Approach**:

1. **Build project**:
   ```bash
   mvn clean package -q
   ```

2. **Verify JAR exists**:
   - Check `target/sshman-*-jar-with-dependencies.jar`

3. **Run smoke tests**:
   ```bash
   ./sshman --version
   ./sshman --help
   ./sshman list --help
   ```

4. **Report results**:
   ```
   ✓ Build successful
   ✓ JAR created: target/sshman-0.1.1-jar-with-dependencies.jar
   ✓ Version command works: sshman 0.1.1
   ✓ Help command works

   Build and smoke test passed!
   ```

**Tools Needed**: Bash

**Example Usage**:
```
User: "Make sure the build works"
Skill: [Runs mvn package, tests basic commands]
```

**Priority**: **Low** - Developers often run this manually

---

### 9. sshman-changelog-entry

**Purpose**: Add a properly formatted entry to CHANGELOG.md.

**Trigger Scenarios**:
- User says: "Add a changelog entry"
- User says: "Document this change in the changelog"
- After implementing a feature, suggest: "Should I add this to the changelog?"

**Implementation Approach**:

1. **Identify change type**:
   - Ask user: Added, Changed, Deprecated, Removed, Fixed, Security?

2. **Prompt for description**:
   - Ask: "Describe the change"

3. **Insert under [Unreleased]**:
   - Find `## [Unreleased]` section
   - Add entry under appropriate subsection
   - Format: `- Description here`

4. **Example**:
   ```markdown
   ## [Unreleased]

   ### Added
   - New command `list-profiles` to display all saved connection profiles

   ### Fixed
   - Fixed issue with profile deletion not saving changes
   ```

5. **Show preview and commit**:
   - Display the change
   - Offer to commit: `git add CHANGELOG.md && git commit -m "docs: add changelog entry"`

**Tools Needed**: Read, Edit, Bash, AskUserQuestion

**Example Usage**:
```
User: "Add a changelog entry for the new list-profiles command"
Skill: [Prompts for type, adds entry under ## [Unreleased] / ### Added]
```

**Priority**: **Low** - Useful but not time-consuming to do manually

---

### 10. sshman-config-analyzer

**Purpose**: Analyze sshman usage patterns by examining user's SSH keys, profiles, and .sshman files to suggest improvements.

**Trigger Scenarios**:
- User says: "Analyze my SSH setup"
- User says: "How can I improve my key organization?"
- User mentions: "audit", "analyze configuration"

**Implementation Approach**:

1. **Scan ~/.ssh directory**:
   - Count total SSH keys
   - Identify key types (ED25519, RSA, ECDSA)
   - Check for insecure permissions
   - Find keys without .pub files

2. **Analyze profiles** (~/.sshman/profiles.json):
   - Count total profiles
   - Check for profiles with missing SSH keys
   - Identify unused keys

3. **Find .sshman files**:
   - Search project directories for .sshman files
   - Verify referenced keys exist

4. **Generate report**:
   ```
   SSH Configuration Analysis:

   Keys:
   - Total: 12 keys
   - ED25519: 8 (recommended)
   - RSA: 4 (consider upgrading to ED25519)
   - Warning: 2 keys have insecure permissions

   Profiles:
   - Total: 5 connection profiles
   - 1 profile references missing key: production-old

   Auto-switching:
   - 3 projects use .sshman files
   - All referenced keys exist

   Recommendations:
   1. Fix permissions on: ~/.ssh/old-key, ~/.ssh/test-key
   2. Remove unused profile: production-old
   3. Consider upgrading RSA keys to ED25519
   ```

**Tools Needed**: Bash, Read

**Example Usage**:
```
User: "Analyze my SSH configuration"
Skill: [Scans keys, profiles, .sshman files, generates report]
```

**Priority**: **Low** - Nice to have for maintenance, not frequently used

---

## Implementation Notes

### Common Patterns for sshman Skills

1. **Version extraction from pom.xml**:
   ```
   Read pom.xml line 10: <version>X.X.X</version>
   Extract with regex or simple string parsing
   ```

2. **Profile JSON handling**:
   ```
   Read ~/.sshman/profiles.json
   Parse as JSON array of Profile objects
   Structure: [{alias, hostname, username, port, sshKey}, ...]
   ```

3. **Command registration**:
   ```
   Edit SshMan.java subcommands array to add new command
   ```

4. **Testing pattern**:
   ```java
   CommandLine cmd = new CommandLine(new SshMan());
   int exitCode = cmd.execute("command", "--option", "value");
   ```

### Skill Naming Convention

All skills use the prefix `sshman-` followed by a descriptive kebab-case name.

### Tools Typically Required

- **Read**: For reading pom.xml, CHANGELOG.md, Java files, profiles.json
- **Edit**: For modifying existing files
- **Write**: For creating new files
- **Bash**: For running Maven, git, sshman commands
- **AskUserQuestion**: For interactive prompts and confirmations
- **Glob**: For finding files (*.java, target/*.jar)
- **Grep**: For searching code patterns

---

## Usage Frequency Estimates

Based on typical development workflows:

| Skill | Frequency | Time Saved |
|-------|-----------|------------|
| sshman-release | Weekly | 15-20 min |
| sshman-version-bump | Daily | 2-3 min |
| sshman-profile-manager | Weekly | 5-10 min |
| sshman-test-runner | Daily | 1-2 min |
| sshman-new-command | Monthly | 10-15 min |
| sshman-completion-regen | Monthly | 5 min |
| sshman-docs-sync | Weekly | 10 min |
| sshman-changelog-entry | Daily | 1-2 min |
| sshman-build-test | Daily | 1 min |
| sshman-config-analyzer | Monthly | 3-5 min |

---

## Next Steps

1. **Implement High Priority skills first**: sshman-release, sshman-profile-manager, sshman-version-bump
2. **Test each skill thoroughly** with actual sshman development workflows
3. **Iterate based on usage**: Adjust priorities based on actual developer feedback
4. **Consider combining skills**: Some skills could be combined (e.g., build-test + test-runner)
5. **Add to project memory**: Once implemented, document in .ai/ for future reference

---

## Skill Template

When implementing a skill, use this template:

```typescript
name: "sshman-skill-name"
description: "Brief description for skill selection"
trigger: [
  "user mentions X",
  "user says Y"
]
prompt: `
You are working on the sshman project, an SSH key management CLI tool.

Task: [Specific task description]

Steps:
1. [Step 1]
2. [Step 2]
...

Context:
- Project uses Java 21, Maven, picocli
- Version is stored in pom.xml line 10
- Profiles are in ~/.sshman/profiles.json
- Commands are in src/main/java/com/sshman/*Command.java

Expected output:
[What should be produced]
`
```

---

*Generated: 2025-12-31*
*Project: sshman v0.1.1*
*For: Claude Code skill development*
