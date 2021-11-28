/**
 * The <code>binary</code> package has the classes that are associated with "binaries". In this app, a binary
 * is a document attachment that is stored in a lower-latency store like S3. The package contains providers
 * to get objects from an S3 bucket, and it also contains a "fake" binary that can be used for concurrency
 * testing and performance testing. The fake binary doesn't do any S3 I/O so it enables the concurrency aspect
 * of the design to be tested independent of any I/O-related affects.
 */
package org.ericace.binary;