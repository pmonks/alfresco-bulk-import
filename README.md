<img align="right" width="96px" height="96px" src="https://raw.github.com/pmonks/alfresco-bulk-import/master/core/icon.png">
# Alfresco Bulk Import Tool

**Please note that this is a work-in-progress repository supporting the development of version 2 of the Alfresco Bulk Import Tool (formerly known as the Alfresco Bulk Filesystem Import Tool).  If you're looking for documentation, binaries etc. for the shipping 1.x versions of the tool, please see the [v1.x project](https://github.com/pmonks/alfresco-bulk-filesystem-import).**

## What Is It?
A high performance bulk import tool for the open source [Alfresco Document Management System](http://www.alfresco.org/).

This module provides a bulk import process that will load content into the repository from a variety of pluggable sources (a directory on the server's local filesystem being the default source).  It will (optionally) replace existing content items if they already exist in the repository, but does _not_ perform deletes (ie. this module is _not_ designed to fully synchronise the repository with the source).

The module provides facilities for loading both metadata and version histories for imported content, although note that Alfresco itself doesn't support version histories for folders.

## What's New?
To find out the latest changes and enhancements to the GA version of the tool, check out the [Change Log](http://code.google.com/p/alfresco-bulk-filesystem-import/wiki/ChangeLog) page in the Google Code wiki.

## For More Information
 * Documentation
 * [Mailing List](https://groups.google.com/forum/#!forum/alfresco-bulk-filesystem-import)
 * [Design overview](http://blogs.alfresco.com/wp/pmonks/2009/10/22/bulk-import-from-a-filesystem/) (somewhat out of date)
 * [DevCon 2011 presentation](http://www.slideshare.net/alfresco/taking-your-bulk-content-ingestions-to-the-next-level) (analysis of performance factors in the tool)

## Commercial Support
This extension is not supported by [Alfresco Software Inc.](http://www.alfresco.com/), although a fork of an early, pre-release version of the tool has been included in Alfresco Enterprise since v4.0, and is supported by [Alfresco support](http://support.alfresco.com).

## License
The tool is Copyright © [Peter Monks](mailto:pmonks@gmail.com), and is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).

## Attributions
 * org.java.util.concurrent.NotifyingBlockingThreadPoolExecutor obtained from [java.net](https://today.java.net/pub/a/today/2008/10/23/creating-a-notifying-blocking-thread-pool-executor.html).  Unclear licensing.<br/>
 * Icon adapted from [Appzgear](http://www.flaticon.com/free-icon/arrow-pointing-down-a-container_26007) on [www.flaticon.com](http://www.flaticon.com/).
 * [Contributing](CONTRIBUTING.md) file heavily inspired by the [Atom](https://github.com/atom/atom/blob/master/CONTRIBUTING.md) project.

## Contributing
Please see [Contributing](CONTRIBUTING.md).
