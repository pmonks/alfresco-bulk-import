# Contributing to the Alfresco Bulk Import Tool
:+1: First off, thanks for taking the time to contribute! :+1:

# Contributor License Agreement (CLA)
A CLA is a document that specifies how a project is allowed to use your
contribution.  We want a CLA that is simple and as clear as possible so that it
doesn't impede contributions to the Alfresco Bulk Import project.

When you make a contribution to the Alfresco Bulk Import project, you agree:

1. Your contribution is your original work (you own the copyright) or you
otherwise have the right to submit the work.
2. You grant the Alfresco Bulk Import project a nonexclusive, irrevocable
license to use your submitted contribution in any way.
3. You are capable of granting these rights for the contribution.

By submitting a contribution to the Alfresco Bulk Import project you agree to
the above statements.

# Contributing Issues

You can create an issue [here](https://github.com/pmonks/alfresco-bulk-import/issues),
but before doing so please read the notes below on submitting issues.  If you're
not 100% sure that what you're seeing is a bug, it's worth asking on the
[mailing list](https://groups.google.com/forum/#!forum/alfresco-bulk-filesystem-import)
first.

Before raising a new issue, please check for duplicates.  A simple search for
exception error messages or a summary of the unexpected behaviour should
suffice.  If in doubt, go ahead and raise a new issue and I'll mark it as a
duplicate if it turns out to be one.

When creating a new issue, please use the following format for the body /
description:

```text
# Steps to Reproduce:
 1. ...step 1 description...
 2. ...step 2 description...

# Expected Result:
...description of what you expected to see...

# Actual Result:
...what actually happened, including full exceptions (please include the entire
stack trace), log entries, screen shots where appropriate, etc....

# Environment:
...your operating system, database, app server, JVM version, Alfresco edition
and version, etc....
```

Please use [Markdown formatting](https://help.github.com/articles/github-flavored-markdown/)
liberally to assist in readability - code fences for exception stack traces and
log entries, for example, massively improve readability.

Where known, please tag new issues with either "Bug" (tool explicitly supports
the capability, but it doesn't work as described) or "Enhancement" (tool is
missing a useful capability).

# Contributing Pull Requests (Code & Docs)
To make review of PRs easier, please:

 * please make sure your PRs will merge cleanly - PRs that don't are unlikely to be accepted.
 * for code contributions, follow [Alfresco's coding standards](https://wiki.alfresco.com/wiki/Coding_Standards).
 * for documentation contributions, follow the general structure, language, and
   tone of the existing docs (####TODO: link this once issue #17 is addressed).
 * keep PRs small and cohesive - if you have multiple contributions, please
   submit them as independent PRs.
 * reference issue #s if your PR has anything to do with an issue (even if it
   doesn't address it).
 * minimise "spurious" changes (e.g. whitespace shenanigans).
 * ensure all new files include an appropriate header comment block, containing
   at least the license (Apache v2.0, preferably) and copyright information.

## Commit and PR Messages

* **Reference issues, wiki pages, and pull requests liberally!**
* Use the present tense ("Add feature" not "Added feature")
* Use the imperative mood ("Move button left..." not "Moves button left...")
* Limit the first line to 72 characters or less
* Consider starting the commit message with one or more applicable emoji:
    * Frequently used:
        * :bug: `:bug:` when fixing a bug
        * :new: `:new:` when implementing an enhancement
        * :ballot_box_with_check: `:ballot_box_with_check:` when completing a task
        * :memo: `:memo:` when writing docs
        * :racehorse: `:racehorse:` when improving performance
        * :art: `:art:` when improving the format/structure of the code
    * Infrequently used:
        * :lock: `:lock:` when dealing with security
        * :fire: `:fire:` when removing code or files
        * :arrow_up: `:arrow_up:` when upgrading dependencies
        * :arrow_down: `:arrow_down:` when downgrading dependencies
        * :penguin: `:penguin:` when fixing something on Linux
        * :apple: `:apple:` when fixing something on Mac OS
        * :checkered_flag: `:checkered_flag:` when fixing something on Windows
        * :white_check_mark: `:white_check_mark:` when adding tests
    * Unlikely to ever be used in this project (but listed, just in case):
        * :green_heart: `:green_heart:` when fixing the CI build
        * :non-potable_water: `:non-potable_water:` when plugging memory leaks
