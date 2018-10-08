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

**By submitting a contribution to the Alfresco Bulk Import project you agree to
the above statements.**

# Contributing Issues

## Prerequisites

* [ ] Have you reviewed the [troubleshooting guide](https://github.com/pmonks/alfresco-bulk-import/wiki/Troubleshooting)?
* [ ] Have you [searched the issue tracker for duplicates](https://github.com/pmonks/alfresco-bulk-import/issues?utf8=%E2%9C%93&q=)?  A simple search for exception error messages or a summary of the unexpected behaviour should suffice.
* [ ] Have you [searched the mailing list for similar situations](https://groups.google.com/forum/#!searchin/alfresco-bulk-filesystem-import/%3Center$20your$20search$20here%3E%7Csort:date)?
* [ ] Have you confirmed you're running the Bulk Import Tool, and *not* the embedded fork?  See the [troubleshooting guide](https://github.com/pmonks/alfresco-bulk-import/wiki/Troubleshooting#embedded-fork) for details on validating this.
* [ ] Are you running the [latest release of the Bulk Import Tool](https://github.com/pmonks/alfresco-bulk-import/releases)?
* [ ] Are you 100% certain this really is a bug or desirable enhancement?  **If unsure, please ask on the [mailing list](https://groups.google.com/forum/#!forum/alfresco-bulk-filesystem-import) first!**

## Raising an Issue

* Create your issue [here](https://github.com/pmonks/alfresco-bulk-import/issues/new).
* Please choose the correct issue template for your new issue.
* Please use [Markdown formatting](https://help.github.com/categories/writing-on-github/) liberally to assist in readability.
  * [Code fences](https://help.github.com/articles/creating-and-highlighting-code-blocks/) for exception stack traces and log entries, for example, massively improve readability.

# Contributing Pull Requests (Code & Docs)

To make review of PRs easier, please:

 * Reference an issue from your PR.  If there isn't an existing issue for your PR, please create an issue first before submitting the PR.
   * This helps expedite review by keeping the problem statement (the issue) explicitly separate from one of potentially many solutions (the PR).
 * Make sure your PRs will merge cleanly - PRs that don't are unlikely to be accepted.
 * For code contributions, follow [Alfresco's coding standards](https://wiki.alfresco.com/wiki/Coding_Standards).
 * For documentation contributions, follow the general structure, language, and tone of the [existing docs](https://github.com/pmonks/alfresco-bulk-import/wiki).
 * Keep PRs small and cohesive - if you have multiple contributions, please submit them as independent PRs.
 * Minimise "spurious" changes (e.g. whitespace shenanigans).
 * Ensure all new files include a header comment block containing the [Apache License v2.0 and your copyright information](http://www.apache.org/licenses/LICENSE-2.0#apply).
 * Add yourself to the top of the [CONTRIBUTORS file](https://github.com/pmonks/alfresco-bulk-import/blob/master/CONTRIBUTORS.md)
 * If necessary (e.g. due to 3rd party dependency licensing requirements), update the [NOTICE file](https://github.com/pmonks/alfresco-bulk-import/blob/master/NOTICE) with any new attribution notices

## Commit and PR Messages

* **Reference issues, wiki pages, and pull requests liberally!**
* Use the present tense ("Add feature" not "Added feature")
* Use the imperative mood ("Move button left..." not "Moves button left...")
* Limit the first line to 72 characters or less
* Please start the commit message with one or more applicable emoji:

| Emoji | Raw Emoji Code | Description |
|:---:|:---:|---|
| :tada: | `:tada:` | **initial** commit |
| :construction: | `:construction:` | **WIP** (Work In Progress) commits |
| :ambulance: | `:ambulance:` | when fixing a **bug** |
| :bug: | `:bug:` | when **identifying a bug**, via an inline comment (please use the `@FIXME` tag in the comment) |
| :new: | `:new:` | when introducing **new** features |
| :art: | `:art:` | when improving the **format** / structure of the code |
| :pencil: | `:pencil:` | when **performing minor changes / fixing** the code or language |
| :ballot_box_with_check: | `:ballot_box_with_check:` | when completing a task |
| :arrow_up: | `:arrow_up:` | when upgrading **dependencies** |
| :arrow_down: | `:arrow_down:` | when downgrading **dependencies** |
| :racehorse: | `:racehorse:` | when improving **performance** |
| :fire: | `:fire:` | when **removing code** or files |
| :speaker: | `:speaker:` | when adding **logging** |
| :mute: | `:mute:` | when reducing **logging** |
| :books: | `:books:` | when writing **docs** |
| :bookmark: | `:bookmark:` | when adding a **tag** |
| :gem: | `:gem:` | new **release** |
| :zap: | `:zap:` | when introducing **backward incompatible** changes or **removing functionality** |
| :bulb: | `:bulb:` | new **idea** identified in the code, via an inline comment (please use the `@IDEA` tag in the comment) |
| :snowflake: | `:snowflake:` | changing **configuration** |
| :lipstick: | `:lipstick:` | when improving **UI** / cosmetic |
| :umbrella: | `:umbrella:` | when adding **tests** |
| :green_heart: | `:green_heart:` | when fixing the **CI** build |
| :lock: | `:lock:` | when dealing with **security** |
| :shirt: | `:shirt:` | when removing **linter** / strict / deprecation / reflection warnings |
| :fast_forward: | `:fast_forward:` | when **forward-porting features** from an older version/branch |
| :rewind: | `:rewind:` | when **backporting features** from a newer version/branch |
| :wheelchair: | `:wheelchair:` | when improving **accessibility** |
| :globe_with_meridians: | `:globe_with_meridians:` | when dealing with **globalisation** / internationalisation |
| :rocket: | `:rocket:` | anything related to deployments / **DevOps** |
| :non-potable_water: | `:non-potable_water:` | when plugging memory leaks
| :penguin: | `:penguin:` | when fixing something on **Linux** |
| :apple: | `:apple:` | when fixing something on **Mac OS** |
| :checkered_flag: | `:checkered_flag:` | when fixing something on **Windows** |
| :handbag: | `:handbag:` | when a commit contains multiple unrelated changes that don't fit into any one category (but please try not to do this!) |
