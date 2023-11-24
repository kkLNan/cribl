# Log Management System

## How I implemented
This application runs on Java 17.

So, we are effectively doing a string search on a large file and there could be some immediate issues such as
* How to load the content to memory.
* How to search faster

The way I did it is to create a simple index which is based off new line.
Upon application starts, it will read all the log files, saves the new line positions into a file.
Later on, the algorithm will be able to leverage the newline positions, and RandomFileAccess class to quick locate a line of content, and then compare if something is there.

Also, we only load partial content into the memory, so that, it won't get exploded, but I am still trying to find the sweet spot.
So far, the search speed can take 10ms to 4000ms on a 1.2GB file with 15000000 lines in it.
Also, you can append new content to it, and it supposed to pick up the changes, and refresh the indexes on the fly.

The indexes are refreshed based on the WatchService provided by Java itself, and it might not be as reliable...
An alternative solution would be use a local cron to check the file content, which could be a bit more reliable, and also less events are generated.

 

### Things I didn't work on
* Sanity checks
* Auth

## How to Run 
To run the application, assume you have maven installed locally, as well as JRE 17.
Before you run, please create a tmp folder, as the application will create the indexes during start up.

```shell
mvn spring-boot:run -Dspring-boot.run.arguments="--cribl.log.directory=/Users/han/Documents"
```

The above -D variable will allow you to dedicate a directory to monitor, so don't point to any of your working directory, seriously.


## Endpoints
### Create a log file
This will create a file at that directory, and it will be 100 rows.
```shell
curl --location 'http://localhost:8080/api/manage/generate?filename=test.log&lines=100'
```

### Look up a value
This will lookup the matching lines.
```shell
curl --location 'http://localhost:8080/api/keyword?keyword=delicious&lastN=10&filename=test.log'
```

### Append new lines
This will append 100 rows
```shell
curl --location 'http://localhost:8080/api/manage/append?filename=test.log&lines=100'
```

And feel free to create a file around 15000000 lines, which should be around 1.2GB.
```shell
curl --location 'http://localhost:8080/api/manage/generate?filename=huge.log&lines=15000000'
```

The file content is generated via a class internally for testing purpose.
`RandomWordsFileGenerator.java`


The content it creates will look like this.
```text
1700758909005:[3401596]: I am eating a(n) elderberry and that's very delicious!!!!
1700758909005:[3401597]: I am eating a(n) cherry and that's very delicious!!!!
1700758909005:[3401598]: I am eating a(n) banana and that's very delicious!!!!
1700758909005:[3401599]: I am eating a(n) nectarine and that's very delicious!!!!
1700758909005:[3401600]: I am eating a(n) strawberry and that's very delicious!!!!
1700758909005:[3401601]: I am eating a(n) mango and that's very delicious!!!!
1700758909005:[3401602]: I am eating a(n) apple and that's very delicious!!!!
1700758909005:[3401603]: I am eating a(n) elderberry and that's very delicious!!!!
1700758909005:[3401604]: I am eating a(n) grape and that's very delicious!!!!
1700758909005:[3401605]: I am eating a(n) lemon and that's very delicious!!!!
1700758909005:[3401606]: I am eating a(n) quince and that's very delicious!!!!
1700758909005:[3401607]: I am eating a(n) tangerine and that's very delicious!!!!
1700758909005:[3401608]: I am eating a(n) strawberry and that's very delicious!!!!
1700758909005:[3401609]: I am eating a(n) watermelon and that's very delicious!!!!
```
the format is

`<unix-timestamp>:[line-number]: I am eating a(n) <a random fruit> and that's very delicious!!!!`

If you want to search 
* the latest lines, you can search by keyword `delicious`, and it should pull the last N rows of the log.
* only 1 result, you can search by line number, i.e `[3401609]`, which effectively should return only 1.
* the first row, you can search by `[1]`
* a serious or event in the mid section of the file, you can search by timestamp, i.e `1700758909005`

## About UI
There's a poor man's version of the UI you can visit at http://localhost:8080/ which is React based.


# The better design
Apparently this is not the practical design and would only demonstrate how it works locally.

My initial design was to include
* NoSQL DB
* Redis Cache
And they will be used as a distributed data store for storing the index information, and should multiple instances spin up, we should be able to read or update the indexes properly.
* Also, with NoSQL, we should be able to leverage the index capability of it, and provide more flexible searches.
But I didn't go for it, as I think it is getting too messy.


If we are talking about bigger cloud based design, one ideal solution I can think of is:

Any service which requires log collection should be able to run a job agent (a jar, or a separate process), and monitor the change of the log file.
Upon a new line is added, a message is published to a MQ system, which include the server metadata, and line of log, and the metadata of the log, like trace_ids, etc
The central log server system, who listens on the topic will be able to 
* persist the message to the right file
* manage all the search facets
* index the message based on the fields (DB, or other search stack, i.e, lucene ELK)
* grooms the old data depends on the retention policy

If you are talking about a primary and secondary setup, like one main primary servers or service pods, to ingest the app.
And secondary servers can be used to serve the searching results.

![system_design.png](system_design.png)
