# archive-generator

This project presents a design for generating a TAR archive from two streams: One stream provides *documents*, and the second stream provides document *attachments*, also referred to throughout the project variously as *binaries* or *binary objects*.

The underlying assumption for this design is that the document stream exists in one store, and the document attachments are housed in a separate store.

For example, the documents could be in ElasticSearch, and obtained from ElasticSearch using the [Search Scroll API](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high-search-scroll.html). In this scenario, one would obtain document metadata from ElasticSearch such as document title, date modified, etc. Included in that metadata might be a key that represents a document attachment which is an object in an S3 bucket.

The goal would be to generate a TAR archive in which each TAR entry gets the document name from ElasticSearch, and the TAR entry content consists of the binary object from S3.

The goal for this project was to experiment with multi-threaded archive generation using small (1K) objects obtained from an S3 bucket. At the end of the day, I was never able to crack the code on how to generate an archive with 1K objects from S3 at a download rate that exceeded about 500K bytes/second.

According to my cable provider, my max download throughput is about 57 megabytes/second. And, in fact, when I generate an archive with S3 binaries sized 10K+, I achieved that throughput. But with objects in the 1K range in S3, nothing I tried performed above 500K bytes per second which is obviously a thousandth of what is possible! I experimented with one-level keys like `0123/0123` and with two-level keys like `00/12/01234` to overcome the S3 per-prefix rate limiting.

Candidates for the bottleneck are: 1) the S3 prefix rate limit, 2) an AWS Java SDK limit, 3) a JDK limit, or 4) some/all of the above. Since I tested with thousands of prefixes in my bucket, I don't think S3 rate-limiting was the issue.

So that leaves me with: an AWS Java SDK limit, or a JDK limit, or perhaps a defect in my approach. I might get motivated and research further using a profiling tool. But at this time, I'm declaring failure.

## Veni, vidi, victus sum

"I came, I saw, I was conquered"