# Work Protocol for Builder Project

**Purpose**: Prevent confusion between local Termux builds (ARM64) and GitHub Actions builds (x86_64)

---

## 🚨 Critical Context

**FACT**: Local builds in Termux will ALWAYS fail due to ARM64 architecture incompatibility with Android Build Tools (aapt2, zipalign are x86_64 only).

**SOLUTION**: GitHub Actions is the ONLY viable build environment for this project.

---

## 📋 Protocol for All Development Work

### **Before ANY Action**

**CHECKPOINT**: Always state current context before performing actions:

```
✓ Context: Working on [branch name]
✓ Environment: GitHub Actions (x86_64) - NOT local Termux
✓ Next action: [what I'm about to do]
✓ Testing method: Push → Monitor with gh run watch
```

---

### **When Investigating Errors**

✅ **DO**:
- Read files using Read tool
- Search codebase using Grep/Glob tools
- Analyze error logs from GitHub Actions
- Ask clarifying questions before making changes

❌ **DON'T**:
- Suggest running `./install.sh` or `./gradlew` commands locally
- Assume local builds will work
- Start fixing without understanding the problem

---

### **When Making Fixes**

✅ **DO**:
- Use Edit tool to modify existing files
- Use Write tool only for new files
- Follow the todo list phases sequentially
- Test one group of fixes at a time

❌ **DON'T**:
- Make multiple unrelated changes in one commit
- Skip investigation phase
- Fix everything at once without testing

---

### **Before Committing**

**CHECKPOINT**: Review changes before commit:

```
✓ Changes made: [list modified files]
✓ Purpose: [what was fixed]
✓ Expected outcome: [what should work now]
✓ Testing plan: GitHub Actions will run [which tasks]
```

---

### **Before Pushing**

**CHECKPOINT**: Confirm push details:

```
✓ Branch: claude/check-branch-health-8MfTd
✓ Commits: [list commit messages]
✓ GitHub Actions: Will trigger automatically
✓ Monitoring: User will run gh run watch <RUN_ID>
```

---

## 🔄 Correct Workflow

1. **Investigate** → Use Read/Grep/Glob tools (no local commands)
2. **Fix** → Use Edit/Write tools
3. **Commit** → git add + git commit with clear message
4. **Push** → git push -u origin claude/check-branch-health-8MfTd
5. **Monitor** → User runs `gh run watch` to see GitHub Actions results
6. **Iterate** → If build fails, repeat from step 1

---

## ❌ Anti-Patterns to Avoid

### **Never Say**:
- "Let me test this locally with `./gradlew build`"
- "Run `./install.sh` to verify"
- "Let's build it in Termux first"
- "Try compiling locally to check"

### **Always Say**:
- "I'll push this and we'll monitor the GitHub Actions build"
- "Let's trigger GitHub Actions to test these changes"
- "After pushing, use `gh run watch` to see the results"
- "GitHub Actions will validate these fixes"

---

## 🎯 Command Reference

### **Commands I Will Use** (in responses):
```bash
# Git operations
git status
git add [files]
git commit -m "message"
git push -u origin claude/check-branch-health-8MfTd
```

### **Commands User Will Use** (in Termux):
```bash
# Monitor GitHub Actions
gh run list --repo RachEma-ux/Builder --workflow=android-ci.yml --branch=claude/check-branch-health-8MfTd --limit 5
gh run watch <RUN_ID> --repo RachEma-ux/Builder
gh run view <RUN_ID> --repo RachEma-ux/Builder
gh run download <RUN_ID> --repo RachEma-ux/Builder
```

---

## 📝 Communication Template

**When starting a phase:**
```
Context: Branch claude/check-branch-health-8MfTd
Action: Beginning Phase [X] - [Phase name]
Tasks: [list of tasks in this phase]
Testing: Will push after [task numbers] and monitor GitHub Actions
```

**After completing work:**
```
Completed: [list of changes]
Committed: [commit message]
Ready to push: Branch claude/check-branch-health-8MfTd
Next: Push and monitor with gh run watch
```

---

## ✅ Success Criteria

- [ ] Never suggest local build commands
- [ ] Always checkpoint before commits/pushes
- [ ] State context clearly before actions
- [ ] Follow todo list phases sequentially
- [ ] Test via GitHub Actions only
- [ ] Clear communication of what will happen next

---

**Last Updated**: 2026-01-12
**Status**: Active protocol for all Builder development work
