# Dicoogle Classification Database

This plugin integrates a database for indexing and querying classification data
with Dicoogle.

## Building

This project depends on the
[Dicoogle Classification API](https://github.com/Enet4/dicoogle-classification-api).
First fetch and install the project there, so that Maven can resolve this dependency
locally. Then, build this project:

```sh
mvn install
```

The plugin jar "class-db-1.0.0-plugin.jar" will be in the target folder.

## Configuring

A configuration is required for the classification database to work. Specify as many
`classifier` elements as there are classifiers and as many `criterion` elements as
desired. Criteria can also include a variable number of criteria that the specific
classification method depends on, often used for conditional classification.
The `index-path` element is optional and is used for specifying the classification
index' directory path. These classifiers are expected to be compliant with the
[Dicoogle Classification API](https://github.com/Enet4/dicoogle-classification-api).

Example:

```xml
<configuration>
    <index-path>/opt/dicoogle/my-predictions</index-path>
    <classifiers>
        <classifier name="my-classifier">
          <criterion id="liver" />
          <criterion id="lesionType" depends="head-neck" />
        </classifier>
        <classifier name="my-classifier-2">
          <criterion id="head-neck" />
        </classifier>
    </classifiers>
</configuration>
```

## Usage

This plugin will register a new indexer and query provider. When an indexing procedure
is issued, this indexer will take all registered classifiers, classify the files and store
the results in its own embedded database.

### Query Provider API

```java
public Iterable<SearchResult> query(String query);
public Iterable<SearchResult> query(String query, Map<String, Object> options);
```

The query provider accepts standard Lucene syntax queries. The available fields are "classifier",
"criterion", as well as the actual criteria available, or prepended by the classifier's name
with a forward slash. Free keyword queries are also supported.

 - `mammo/microcalcifications:true` -- all predictions where the classifier `mammo` predicted `microcalcifications` as true.
 - `liver:true` or just `liver` -- all predictions where `liver` was predicted true
 - `bodyPart:torso` -- all files where criterion `bodyPart` was predicted `torso`
 - `uri:file:/001.dcm` -- all predictions of the given file

### Web Services

#### `GET /classification/classify?classifier={}&criterion={}`

Request for a classification procedure and return the predictions.
The classification database is not involved.

#### `GET /classification/query?query={}`

Query for predicted data.

## License

Copyright (C) 2017 UA.PT Bioinformatics - http://bioinformatics.ua.pt

dicoogle-class-db is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

dicoogle-class-db is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
