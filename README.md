# Garuda

Garuda is a lightweight tool to collect tweets in real-time from Twitter API v2. 
It proposes modules to handle collected tweets. 

It is developed in Scala, using the [Play framework](https://www.playframework.com/).

## Launch the application

Download the zip in the release section, execute the bash (Linux) or bat (Windows) script in the `bin` directory. 

The application should now be listening on port 9000, visit [locahost:9000]() to access the application.

### Configuration

In the conf directory, the `application.conf` file can be updated for several parameters:
- `garuda.directory`: gives the directory in which the files containing the tweets in JSON format will be written;
- `slick.dbs.default.db.url`: the first part of the line (`jdbc:h2:./data/db:play`) contains the directory in which the data of the H2 database are stored (`./data/db:play`);
- `play.http.secret.key`: see [the Play documentation](https://www.playframework.com/documentation/2.8.x/ApplicationSecret).

## Set up a collect

To set up a collect, it is first necessary to add on the accounts page an account that possess a [developer access on the Twitter plateform](https://developer.twitter.com/en). 
Once it is done, the account can be used on the collects page to define a collect. 

When accessing the collect page, [rules](https://developer.twitter.com/en/docs/twitter-api/tweets/filtered-stream/integrate/build-a-rule) can be defined. They will specify which tweets are collected among those published. 
When the collect is started, the collected tweets will be saved in a file in their JSON format. 

### Manage the rules

The `Add a rule` at the bottom of the page allows to add a rule with a given tag (a tweet is annotated with the tag which rule triggered its collect).

Once a rule is added, the can be selected by clicking on it (they appear in red when doing so). 
The `<<` button switches selected non-active rules to the active section, and the `>>` button does the opposite. 
When all rules are in the desired category, click on the `Affect rules` button to send the active rules to the Twitter account, and to disable the non-active rules. 

### Start the collect

Once the rules are defined, click on the `Start collect` button to retrieve tweets corresponding to the rules. 

## Use modules

Once a collect is defined, it is possible to access modules on the page of the collect. 
For now, there is a PostgreSQL module that allows to store tweets of a collect into a PostgreSQL database. 

## Roadmap

Several improvements are on the roadmap:
- add a module to monitor a collect and see how many tweets are collected;
- add a module to extract tweets of a collect into a CSV file;
- add a helper to define rules.

## Additional Documentation

See the following page:
* [DEVELOPMENT.md]() to start developpement (compile and run from source)
