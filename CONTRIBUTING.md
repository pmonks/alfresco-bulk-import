<img align="right" width="96px" height="96px" src="https://raw.github.com/pmonks/alfresco-bulk-import/master/icon.png">
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

## Prerequisites

* [ ] Have you reviewed the [troubleshooting guide](https://github.com/pmonks/alfresco-bulk-import/wiki/Troubleshooting)?
* [ ]  Have you [searched for duplicates](https://github.com/pmonks/alfresco-bulk-import/issues?utf8=%E2%9C%93&q=)?  A simple search for exception error messages or a summary of the unexpected behaviour should suffice.
* [ ] Have you confirmed you're running the Bulk Import Tool, and *not* the embedded fork?  See the [troubleshooting guide](https://github.com/pmonks/alfresco-bulk-import/wiki/Troubleshooting#embedded-fork) for details on validating this.
* [ ] Are you running the [latest release of the Bulk Import Tool](https://github.com/pmonks/alfresco-bulk-import/releases)?
* [ ] Are you sure this is a bug or missing capability?  If unsure, please ask on the [mailing list](https://groups.google.com/forum/#!forum/alfresco-bulk-filesystem-import) first.

## Raising an Issue
* Create your issue [here](https://github.com/pmonks/alfresco-bulk-import/issues/new).
* New issues contain two templates in the description - bug report and enhancement request.
** Please pick the most appropriate to fill out, and delete the other
* Please use [Markdown formatting](https://help.github.com/articles/github-flavored-markdown/)
liberally to assist in readability - [code fences](https://help.github.com/articles/github-flavored-markdown/#fenced-code-blocks)
for exception stack traces and log entries, for example, massively improve readability.
* Where known, please tag new issues with either "Bug" (tool explicitly supports the capability, but it doesn't work as described) or "Enhancement" (tool is missing a useful capability).

# Contributing Pull Requests (Code & Docs)
To make review of PRs easier, please:

 * please make sure your PRs will merge cleanly - PRs that don't are unlikely to be accepted.
 * for code contributions, follow [Alfresco's coding standards](https://wiki.alfresco.com/wiki/Coding_Standards).
 * for documentation contributions, follow the general structure, language, and
   tone of the [existing docs](https://github.com/pmonks/alfresco-bulk-import/wiki)
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
        * :green_heart: `:green_heart:` when fixing the CI build
    * Unlikely to ever be used in this project (but listed, just in case):
        * :non-potable_water: `:non-potable_water:` when plugging memory leaks
