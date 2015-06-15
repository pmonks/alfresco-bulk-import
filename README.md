<img align="right" width="96px" height="96px" src="https://raw.github.com/pmonks/alfresco-bulk-import/master/icon.png">
# Alfresco Bulk Import Tool

## What Is It?
A high performance bulk import tool for the open source [Alfresco Document
Management System](http://www.alfresco.org/).

This module provides a bulk import process that will load content into the
repository from a variety of pluggable sources (a directory on the server's
local filesystem being the default source).  It will (optionally) replace
existing content items if they already exist in the repository, but does _not_
perform deletes (ie. this module is _not_ designed to fully synchronise the
repository with the source).

The module provides facilities for loading both metadata and version histories
for imported content, although note that Alfresco itself doesn't support version
histories for folders.

## What's New?
 * [Commit log](https://github.com/pmonks/alfresco-bulk-import/commits/master)

## For More Information
 * [Wiki](https://github.com/pmonks/alfresco-bulk-import/wiki/Home) (documentation etc.)
 * [Mailing List](https://groups.google.com/forum/#!forum/alfresco-bulk-filesystem-import)
 * [Design overview](http://blogs.alfresco.com/wp/pmonks/2009/10/22/bulk-import-from-a-filesystem/) (less relevant for v2.0+)
 * [DevCon 2011 presentation](http://www.slideshare.net/alfresco/taking-your-bulk-content-ingestions-to-the-next-level) (less relevant for v2.0+, but some good general findings on tuning Alfresco for I/O bound workloads such as bulk imports)

## Commercial Support
**This extension is not supported by [Alfresco Software Inc.](http://www.alfresco.com/)**,
although a fork of an early, pre-release version of the tool has been included in Alfresco
Enterprise since v4.0, and is supported by [Alfresco support](http://support.alfresco.com).

Please note that the embedded fork has never been rebased against upstream, meaning that it
is functionally equivalent to the 1.0-RC1 (ancient, circa mid-2010) version of the tool.
its use is therefore discouraged.

## License
Copyright © [Peter Monks](mailto:pmonks@gmail.com). Licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).

## Attributions
 * Icon adapted from [Appzgear](http://www.flaticon.com/free-icon/arrow-pointing-down-a-container_26007) on [www.flaticon.com](http://www.flaticon.com/).
 * [Contributing](CONTRIBUTING.md) file heavily inspired by the [Atom](https://github.com/atom/atom/blob/master/CONTRIBUTING.md) project.

## Contributing
Please see [Contributing](CONTRIBUTING.md).
